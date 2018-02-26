/*
 * Copyright (c) 2015-2018 by k3b.
 *
 * This file is part of AndroFotoFinder / #APhotoManager.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>
 */

package de.k3b.media;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

import de.k3b.FotoLibGlobal;
import de.k3b.io.FileProcessor;
import de.k3b.io.VISIBILITY;
import de.k3b.transactionlog.TransactionLoggerBase;

/**
 * apply meta data changes to jpg and/or xmp file and log.
 *
 * Created by k3b on 25.08.2015.
 */
public class JpgMetaWorkflow {
    private static final Logger logger = LoggerFactory.getLogger(FotoLibGlobal.LOG_TAG);
    private final TransactionLoggerBase transactionLogger;

    private StringBuilder debugExif(StringBuilder sb, String context, MetaWriterExifXml exif, File filePath) {
        if (sb != null) {
            sb.append("\n\t").append(context).append("\t: ");

            if (exif != null) {
                if (exif.getExif() != null) {
                    sb.append(exif.getExif().getDebugString(" "));
                } else {
                    sb.append(MediaUtil.toString(exif, false, null, MediaUtil.FieldID.path));
                }
            }
        }
        return sb;
    }

    /** overwrite to create a android specific Workflow */
    public JpgMetaWorkflow(TransactionLoggerBase transactionLogger) {

        this.transactionLogger = transactionLogger;
    }
    public MetaWriterExifXml saveLatLon(File filePath, Double latitude, Double longitude) {
        IMetaApi changedData = new MediaDTO().setLatitudeLongitude(latitude, longitude);
        MediaDiffCopy metaDiffCopy = new MediaDiffCopy(true, true)
                .setDiff(changedData, MediaUtil.FieldID.latitude_longitude);
        MetaWriterExifXml exif = applyChanges(filePath, null, 0, false, metaDiffCopy);
        metaDiffCopy.close();
        return exif;
    }

    /** writes either (changes + _affectedFields) or metaDiffCopy to jpg/xmp-filePath.
     * Returns new values or null if no change. */
    public MetaWriterExifXml applyChanges(File inFilePath, String outFilePath,
                                          long id, boolean deleteOriginalWhenFinished, MediaDiffCopy metaDiffCopy) {
        StringBuilder sb = (FotoLibGlobal.debugEnabled)
                ? createDebugStringBuilder(inFilePath)
                : null;
        File outFile = (outFilePath != null) ? new File(outFilePath) : inFilePath;
        if ((inFilePath != null) && outFile.getParentFile().canWrite()) {
            MetaWriterExifXml exif = null;
            try {
                long lastModified = inFilePath.lastModified();
                exif = MetaWriterExifXml.create (inFilePath.getAbsolutePath(), outFilePath, false, "MetaWriterExifXml:");
                debugExif(sb, "old", exif, inFilePath);
                List<String> oldTags = exif.getTags();

                boolean sameFile = (outFile.equals(inFilePath));

                File newOutFile = handleVisibility(metaDiffCopy.getVisibility(), outFile, exif);
                if (newOutFile != null) {
                    outFile = newOutFile;
                    outFilePath = outFile.getAbsolutePath();
                    if (sameFile) {
                        sameFile = false;
                        deleteOriginalWhenFinished = true;
                    }
                }

                List<MediaUtil.FieldID> changed = metaDiffCopy.applyChanges(exif);

                if (!sameFile || (changed != null)) {
                    debugExif(sb, "assign ", exif, inFilePath);

                    exif.save("MetaWriterExifXml save");

                    if (FotoLibGlobal.preserveJpgFileModificationDate) {
                        // preseve file modification date
                        inFilePath.setLastModified(lastModified);
                    }

                    id = updateMediaDB(id, inFilePath.getAbsolutePath(), outFile);

                    if (sb != null) {
                        MetaWriterExifXml exifVerify = MetaWriterExifXml.create (inFilePath.getAbsolutePath(),
                                null, false, "dbg in MetaWriterExifXml", true, true, false);
                        debugExif(sb, "new ", exifVerify, inFilePath);
                    }

                    // add applied meta changes to transactionlog
                    if(transactionLogger != null) {
                        if (!sameFile) {
                            // first log copy/move. copy  may change databaseID
                            transactionLogger.addChangesCopyMove(deleteOriginalWhenFinished, outFilePath, "applyChanges");
                        }
                        transactionLogger.set(id, outFilePath);
                        if ((changed != null) && (changed.size() > 0)) {
                            transactionLogger.addChanges(exif, EnumSet.copyOf(changed), oldTags);
                        }
                    }

                    if (!sameFile && deleteOriginalWhenFinished) {
                        File delete = FileProcessor.getSidecar(inFilePath, false);
                        deleteFile(delete);

                        delete = FileProcessor.getSidecar(inFilePath, true);
                        deleteFile(delete);

                        delete = inFilePath;
                        deleteFile(delete);
                    }
                } else {
                    if (sb != null) sb.append("no changes ");
                    exif = null;
                }

                if (sb != null) {
                    JpgMetaWorkflow.logger.info(sb.toString());
                }
                return exif;
            } catch (IOException e) {
                if (sb == null) {
                    sb = createDebugStringBuilder(inFilePath);
                    debugExif(sb, "err content", exif, inFilePath);
                }

                sb.append("error='").append(e.getMessage()).append("' ");
                JpgMetaWorkflow.logger.error(sb.toString(), e);
                return null;
            }
        } else {
            if (sb == null) {
                sb = createDebugStringBuilder(inFilePath);
            }

            sb.append("error='file is write protected' ");
            JpgMetaWorkflow.logger.error(sb.toString());
            return null;
        }
    }

    protected void deleteFile(File delete) {
        if ((delete != null) && delete.exists()) {
            delete.delete();
            if (FotoLibGlobal.debugEnabledJpg) {
                logger.info("JpgMetaWorkflow deleteFile " + delete);
            }
        }
    }

    /** return modified out file or null if filename must not change due to visibility rule */
    protected File handleVisibility(VISIBILITY newVisibility, File outFile, MetaWriterExifXml exif) {
        if (FotoLibGlobal.renamePrivateJpg) {
            final String oldAbsoluteOutPath = (outFile == null) ? null : outFile.getAbsolutePath();
            String newAbsoluteOutPath = MediaUtil.getModifiedPath(oldAbsoluteOutPath, newVisibility);

            if (newAbsoluteOutPath != null) {
                String sourcePath = exif.getPath();
                if ((sourcePath != null) && (sourcePath.compareTo(oldAbsoluteOutPath) == 0)) {
                    // original intend was "change in same file" so add to log that filename has changed (rename/move)
                    transactionLogger.addChangesCopyMove(true, newAbsoluteOutPath, "handleVisibility");
                }
                exif.setAbsoluteJpgOutPath(newAbsoluteOutPath);
                return new File(newAbsoluteOutPath);
            }
        }
        return null;
    }

    /** todo overwrite in android class to implement update media db */
    protected long updateMediaDB(long id, String oldJpgAbsolutePath, File newJpgFile) {
        return id;
    }

    private StringBuilder createDebugStringBuilder(File filePath) {
        if(filePath != null) {
            return new StringBuilder("Set Exif to file='").append(filePath.getAbsolutePath()).append("'\n\t");
        }
        return new StringBuilder();
    }


    // Translate exif-orientation code (0..8) to exifOrientationCode2RotationDegrees (clockwise)
    // http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/EXIF.html
    private static final short[] exifOrientationCode2RotationDegrees = {
            0,     // EXIF Orientation constants:
            0,     // 1 = Horizontal (normal)
            0,     // 2 = (!) Mirror horizontal
            180,   // 3 = Rotate 180
            180,   // 4 = (!) Mirror vertical
            90,    // 5 = (!) Mirror horizontal and rotate 270 CW
            90,    // 6 = Rotate 90 CW
            270,   // 7 = (!) Mirror horizontal and rotate 90 CW
            270};  // 8 = Rotate 270 CW

    /**
     * Get necessary rotation for image file from exif.
     *
     * @param fullPathToImageFile The filename.
     * @return right-rotate (in degrees) image according to exifdata.
     */
    public static int getRotationFromExifOrientation(String fullPathToImageFile) {
        try {
            ExifInterfaceEx exif = new ExifInterfaceEx(fullPathToImageFile, null, null, "getRotationFromExifOrientation");
            if (exif.isValidJpgExifFormat()) {
                int orientation = exif.getAttributeInt(ExifInterfaceEx.TAG_ORIENTATION, 0);
                if ((orientation >= 0) && (orientation < exifOrientationCode2RotationDegrees.length))
                    return exifOrientationCode2RotationDegrees[orientation];
            }
        }
        catch (Exception e) {
        }
        return 0;
    }


}

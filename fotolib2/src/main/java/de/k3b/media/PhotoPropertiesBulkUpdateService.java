/*
 * Copyright (c) 2015-2019 by k3b.
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
import java.io.InputStream;
import java.util.EnumSet;
import java.util.List;

import de.k3b.LibGlobal;
import de.k3b.io.FileFacade;
import de.k3b.io.FileProcessor;
import de.k3b.io.IFile;
import de.k3b.io.VISIBILITY;
import de.k3b.transactionlog.TransactionLoggerBase;

/**
 * apply meta data changes to one ore more jpg and/or xmp file and log.
 *
 * Created by k3b on 25.08.2015.
 */
public class PhotoPropertiesBulkUpdateService {
    private static final Logger logger = LoggerFactory.getLogger(LibGlobal.LOG_TAG);
    private final TransactionLoggerBase transactionLogger;

    private StringBuilder debugExif(StringBuilder sb, String context, PhotoPropertiesUpdateHandler exif, IFile filePath) {
        if (sb != null) {
            sb.append("\n\t").append(context).append("\t: ");

            if (exif != null) {
                if (exif.getExif() != null) {
                    sb.append(exif.getExif().getDebugString(" "));
                } else {
                    sb.append(PhotoPropertiesFormatter.format(exif, false, null, PhotoPropertiesFormatter.FieldID.path));
                }
            }
        }
        return sb;
    }

    /** overwrite to create a android specific Workflow */
    public PhotoPropertiesBulkUpdateService(TransactionLoggerBase transactionLogger) {

        this.transactionLogger = transactionLogger;
    }

    @Deprecated
    public PhotoPropertiesUpdateHandler saveLatLon(File filePath, Double latitude, Double longitude) {
        return saveLatLon(FileFacade.convert(filePath), latitude, longitude);
    }

    public PhotoPropertiesUpdateHandler saveLatLon(IFile filePath, Double latitude, Double longitude) {
        IPhotoProperties changedData = new PhotoPropertiesDTO().setLatitudeLongitude(latitude, longitude);
        PhotoPropertiesDiffCopy metaDiffCopy = new PhotoPropertiesDiffCopy(true, true)
                .setDiff(changedData, PhotoPropertiesFormatter.FieldID.latitude_longitude);
        PhotoPropertiesUpdateHandler exif = applyChanges(filePath, null, 0, false, metaDiffCopy);
        metaDiffCopy.close();
        return exif;
    }

    @Deprecated
    public PhotoPropertiesUpdateHandler applyChanges(File inFilePath, String outFilePath,
                                                     long id, boolean deleteOriginalWhenFinished, PhotoPropertiesDiffCopy metaDiffCopy) {
        return applyChanges(FileFacade.convert(inFilePath), outFilePath, id, deleteOriginalWhenFinished, metaDiffCopy);
    }

    /**
     * writes either (changes + _affectedFields) or metaDiffCopy to jpg/xmp-filePath.
     * Returns new values or null if no change.
     */
    public PhotoPropertiesUpdateHandler applyChanges(IFile inFilePath, String outFilePath,
                                                     long id, boolean deleteOriginalWhenFinished, PhotoPropertiesDiffCopy metaDiffCopy) {
        StringBuilder sb = (LibGlobal.debugEnabled)
                ? createDebugStringBuilder(inFilePath)
                : null;
        IFile outFile = (outFilePath != null) ? FileFacade.convert(new File(outFilePath)) : inFilePath;
        if ((inFilePath != null) && outFile.getParentFile().canWrite()) {
            PhotoPropertiesUpdateHandler exifHandler = null;
            try {
                long lastModified = inFilePath.lastModified();
                exifHandler = PhotoPropertiesUpdateHandler.create(inFilePath.getAbsolutePath(), outFilePath, false, "PhotoPropertiesUpdateHandler:");
                debugExif(sb, "old", exifHandler, inFilePath);
                List<String> oldTags = exifHandler.getTags();

                boolean sameFile = (outFile.equals(inFilePath));

                IFile newOutFile = handleVisibility(metaDiffCopy.getVisibility(), outFile, exifHandler);
                if (newOutFile != null) {
                    outFile = newOutFile;
                    outFilePath = outFile.getAbsolutePath();
                    if (sameFile) {
                        sameFile = false;
                        deleteOriginalWhenFinished = true;
                    }
                }

                List<PhotoPropertiesFormatter.FieldID> changed = metaDiffCopy.applyChanges(exifHandler);

                if (!sameFile || (changed != null)) {
                    debugExif(sb, "assign ", exifHandler, inFilePath);

                    exifHandler.save("PhotoPropertiesUpdateHandler save");

                    if (LibGlobal.preserveJpgFileModificationDate) {
                        // preseve file modification date
                        inFilePath.setLastModified(lastModified);
                    }

                    id = updateMediaDB(id, inFilePath.getAbsolutePath(), outFile);

                    if (sb != null) {
                        PhotoPropertiesUpdateHandler exifVerify = PhotoPropertiesUpdateHandler.create (inFilePath.getAbsolutePath(),
                                null, false, "dbg in PhotoPropertiesUpdateHandler", true, true, false);
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
                            transactionLogger.addChanges(exifHandler, EnumSet.copyOf(changed), oldTags);
                        }
                    }

                    if (!sameFile && deleteOriginalWhenFinished) {
                        IFile delete = FileProcessor.getSidecar(inFilePath, false);
                        deleteFile(delete);

                        delete = FileProcessor.getSidecar(inFilePath, true);
                        deleteFile(delete);

                        delete = inFilePath;
                        deleteFile(delete);
                    }
                } else {
                    if (sb != null) sb.append("no changes ");
                    exifHandler = null;
                }

                if (sb != null) {
                    PhotoPropertiesBulkUpdateService.logger.info(sb.toString());
                }
                return exifHandler;
            } catch (IOException e) {
                if (sb == null) {
                    sb = createDebugStringBuilder(inFilePath);
                    debugExif(sb, "err content", exifHandler, inFilePath);
                }

                sb.append("error='").append(e.getMessage()).append("' ");
                PhotoPropertiesBulkUpdateService.logger.error(sb.toString(), e);
                return null;
            }
        } else {
            if (sb == null) {
                sb = createDebugStringBuilder(inFilePath);
            }

            sb.append("error='file is write protected' ");
            PhotoPropertiesBulkUpdateService.logger.error(sb.toString());
            return null;
        }
    }

    @Deprecated
    protected void deleteFile(File delete) {
        deleteFile(FileFacade.convert(delete));
    }

    protected void deleteFile(IFile delete) {
        if ((delete != null) && delete.exists()) {
            delete.delete();
            if (LibGlobal.debugEnabledJpg) {
                logger.info("PhotoPropertiesBulkUpdateService deleteFile " + delete);
            }
        }
    }

    protected File handleVisibility(VISIBILITY newVisibility, File outFile, PhotoPropertiesUpdateHandler exif) {
        return handleVisibility(newVisibility, FileFacade.convert(outFile), exif).getFile();

    }
    /** return modified out file or null if filename must not change due to visibility rule */
    protected IFile handleVisibility(VISIBILITY newVisibility, IFile outFile, PhotoPropertiesUpdateHandler exif) {
        if (LibGlobal.renamePrivateJpg) {
            final String oldAbsoluteOutPath = (outFile == null) ? null : outFile.getAbsolutePath();
            String newAbsoluteOutPath = PhotoPropertiesUtil.getModifiedPath(oldAbsoluteOutPath, newVisibility);

            if (newAbsoluteOutPath != null) {
                String sourcePath = exif.getPath();
                if ((sourcePath != null) && (sourcePath.compareTo(oldAbsoluteOutPath) == 0)) {
                    // original intend was "change in same file" so add to log that filename has changed (rename/move)
                    transactionLogger.addChangesCopyMove(true, newAbsoluteOutPath, "handleVisibility");
                }
                exif.setAbsoluteJpgOutPath(newAbsoluteOutPath);
                return FileFacade.convert(new File(newAbsoluteOutPath));
            }
        }
        return null;
    }

    @Deprecated
    protected long updateMediaDB(long id, String oldJpgAbsolutePath, File newJpgFile) {
        return updateMediaDB(id, oldJpgAbsolutePath, FileFacade.convert(newJpgFile));
    }
    /** todo overwrite in android class to implement update media db */
    protected long updateMediaDB(long id, String oldJpgAbsolutePath, IFile newJpgFile) {
        return id;
    }

    private StringBuilder createDebugStringBuilder(IFile filePath) {
        if(filePath != null) {
            return new StringBuilder("Set Exif to file='").append(filePath.getAbsolutePath()).append("'\n\t");
        }
        return new StringBuilder();
    }

    /**
     * Get necessary rotation for image file from exif.
     *
     * @param fullPathToImageFile The filename.
     * @param inputStream
     * @return right-rotate (in degrees) image according to exifdata.
     */
    public static int getRotationFromExifOrientation(String fullPathToImageFile, InputStream inputStream) {
        try {
            ExifInterfaceEx exif = ExifInterfaceEx.create(fullPathToImageFile, inputStream, null, "getRotationFromExifOrientation");
            if (exif.isValidJpgExifFormat()) {

                return PhotoPropertiesUtil.exifOrientationCode2RotationDegrees(exif.getAttributeInt(ExifInterfaceEx.TAG_ORIENTATION, 0), 0);
            }
        }
        catch (Exception e) {
        }
        return 0;
    }
}

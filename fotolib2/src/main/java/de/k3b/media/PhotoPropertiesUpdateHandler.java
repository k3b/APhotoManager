/*
 * Copyright (c) 2017-2018 by k3b.
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

import java.io.IOException;
import java.util.Date;

import de.k3b.LibGlobal;
import de.k3b.io.FileCommands;
import de.k3b.io.FileFacade;
import de.k3b.io.FileProcessor;
import de.k3b.io.FileUtils;
import de.k3b.io.IFile;

/**
 * Represents content of exactly one jpg-exif-file with corresponding xmp-file that can be modified
 * via {@link IPhotoProperties} and {@link #save(String)}.
 *
 * Depending on the global settings handles updating/creating jpg-exif and/or xmp-file.
 * Also handles jpg/xmp file move/copy.
 * Transactionlog and database update is handled by caller
 *
 * Android free implementation.
 *
 * Created by k3b on 21.04.2017.
 */

public class PhotoPropertiesUpdateHandler extends PhotoPropertiesWrapper implements IPhotoProperties {
    private static final Logger logger = LoggerFactory.getLogger(LibGlobal.LOG_TAG);

    private ExifInterfaceEx exif;   // not null if exif changes are written to jpg file
    private PhotoPropertiesXmpSegment xmp;    // not null if exif changes are written to xmp sidecar file.
    private String absoluteJpgInPath; // where changes are read from.
    private String absoluteJpgOutPath; // where changes are written to. Null meanst same as input
    private boolean deleteOriginalAfterFinish; // true: after save original jpg/mxp are deleted (move instead of copy)
    private long    dbgLoadEndTimestamp;

    private PhotoPropertiesUpdateHandler(
            IPhotoProperties readChild, IPhotoProperties writeChild,
            ExifInterfaceEx exif, PhotoPropertiesXmpSegment xmp) {
        super(readChild, writeChild);
        this.exif = exif;
        this.xmp = xmp;
    }

    /**
     * public api: Factory to create PhotoPropertiesUpdateHandler. Settings/ Internal state determine
     * configuration for PhotoPropertiesUpdateHandler.
     *
     *
     * @param absoluteJpgInPath     where data is read from
     * @param absoluteJpgOutPath    where data changes are written to. Null means same as absoluteJpgInPath.
     * @param deleteOriginalAfterFinish true: after save original jpg/mxp are deleted (move instead of copy)
     * @param dbg_context           for debug log: who called this
     * @return                      new loaded instance
     * @throws IOException
     */
    public static PhotoPropertiesUpdateHandler create(
            String absoluteJpgInPath, String absoluteJpgOutPath,
            boolean deleteOriginalAfterFinish, String dbg_context) throws IOException {
        return create(absoluteJpgInPath, absoluteJpgOutPath, deleteOriginalAfterFinish, dbg_context,
                LibGlobal.mediaUpdateStrategy.contains("J"),    // write jpg file
                LibGlobal.mediaUpdateStrategy.contains("X"),    // write xmp file
                LibGlobal.mediaUpdateStrategy.contains("C")    // create xmp if it does not exist
        );
    }

    /**
     * Used by junit tests with no dependency to internal state: factory to create PhotoPropertiesUpdateHandler.
     *
     *
     * @param absoluteJpgInPath     where data is read from
     * @param absoluteJpgOutPath    where data changes are written to. Null means same as absoluteJpgInPath.
     * @param deleteOriginalAfterFinish true: after save original jpg/mxp are deleted (move instead of copy)
     * @param dbg_context           for debug log: who called this
     * @param writeJpg              true: exif changes go into jpg
     * @param writeXmp              true: exif changes go into xmp
     * @param createXmpIfNotExist   true: create xmp sidecar file if it does not exist yet
     * @return                      new loaded instance
     * @throws IOException
     */
    protected static PhotoPropertiesUpdateHandler create(
            String absoluteJpgInPath, String absoluteJpgOutPath,
            boolean deleteOriginalAfterFinish, String dbg_context,
            boolean writeJpg, boolean writeXmp, boolean createXmpIfNotExist)
            throws IOException {
        long    startTimestamp = 0;
        if (LibGlobal.debugEnabledJpgMetaIo) {
            startTimestamp = new Date().getTime();
        }
        PhotoPropertiesXmpSegment xmp = PhotoPropertiesXmpSegment.loadXmpSidecarContentOrNull(absoluteJpgInPath, dbg_context);
        if ((xmp == null) && (createXmpIfNotExist || PhotoPropertiesUtil.isImage(absoluteJpgInPath,
                PhotoPropertiesUtil.IMG_TYPE_COMPRESSED_NON_JPG | PhotoPropertiesUtil.IMG_TYPE_UNCOMPRESSED_NON_JPG))) {
            PhotoPropertiesImageReader jpg = new PhotoPropertiesImageReader().load(absoluteJpgInPath,null,null,
                    dbg_context + " xmp-file not found. create/extract from jpg ");

            // #124: fix can be null for gif/png
            xmp = (jpg == null) ? null : jpg.getImternalXmp();

            // jpg has no embedded xmp create
            if (xmp == null) {
                xmp = new PhotoPropertiesXmpSegment();
            }

            // xmp should have the same data as exif/iptc
            PhotoPropertiesUtil.copyNonEmpty(xmp, jpg);
            if ((absoluteJpgInPath != null) && (xmp.getDateTimeTaken() == null)) {
                IFile in = FileFacade.convert("PhotoPropertiesUpdateHandler.create", absoluteJpgInPath);
                if (in.exists() && in.isFile()) {
                    long lastModified = in.lastModified();
                    if (lastModified != 0) {
                        final Date newDate = new Date(lastModified);
                        xmp.setDateTimeTaken(newDate);
                        if (xmp.getFilelastModified() == 0) xmp.setFilelastModified(in);
                    }
                }

            }
        }
        ExifInterfaceEx exif = ExifInterfaceEx.create(absoluteJpgInPath, null, xmp, dbg_context);
        if (exif.isValidJpgExifFormat()) {
            exif.setPath(absoluteJpgInPath);
        } else {
            exif = null;

            // if no exif (i.e. png file) always use xmp instead. create xmp if neccessary
            if (xmp == null) xmp = new PhotoPropertiesXmpSegment();
        }

        PhotoPropertiesUpdateHandler result;

        if (exif != null) {
            result = new PhotoPropertiesUpdateHandler(
                    (writeJpg) ? exif : new PhotoPropertiesChainReader(xmp, exif), //  (!writeJpg) prefer read from xmp value before exif value
                    (writeJpg) ? exif : xmp,  //  (!writeJpg) modify xmp value only
                    (writeJpg) ? exif : null,  //  (!writeJpg) do not safe changes to jpg exif file
                    (writeXmp) ? xmp : null);   //  (!writeXmp) do not safe changes to xmp-sidecar file
        } else {
            // if no exif (i.e. png file) always use xmp instead
            result = new PhotoPropertiesUpdateHandler(xmp, xmp, null, xmp);

        }

        result.absoluteJpgOutPath = (absoluteJpgOutPath != null) ? absoluteJpgOutPath : absoluteJpgInPath;
        result.absoluteJpgInPath = absoluteJpgInPath;
        result.deleteOriginalAfterFinish = deleteOriginalAfterFinish;
        if (LibGlobal.debugEnabledJpgMetaIo) {
            result.dbgLoadEndTimestamp = new Date().getTime();
            logger.debug(dbg_context + " load[msec]:" + (result.dbgLoadEndTimestamp - startTimestamp));
        }

        return result;
    }

    public void setAbsoluteJpgOutPath(String absoluteJpgOutPath) {
        this.absoluteJpgOutPath = absoluteJpgOutPath;
    }

    public String getAbsoluteJpgOutPath() {
        return this.absoluteJpgOutPath;
    }

    public int save(String dbg_context)  throws IOException {
        long    startTimestamp = 0;
        if (LibGlobal.debugEnabledJpgMetaIo) {
            startTimestamp = new Date().getTime();
        }

        final int result = transferExif(dbg_context)
                + transferXmp(dbg_context);

        if (LibGlobal.debugEnabledJpgMetaIo) {
            long    endTimestamp = new Date().getTime();
            logger.debug(dbg_context
                    + " process[msec]:" + (startTimestamp - this.dbgLoadEndTimestamp)
                    + ",save[msec]:" + (endTimestamp - startTimestamp)
            );
        }

        return result;
    }

    private int transferXmp(String dbg_context) throws IOException {
        int changedFiles = 0;

        String inJpgFullPath = this.getPath();
        if (inJpgFullPath == null) inJpgFullPath = this.absoluteJpgInPath;
        String outJpgFullPath = (this.absoluteJpgOutPath == null) ? inJpgFullPath : this.absoluteJpgOutPath;
        boolean isSameFile = (outJpgFullPath.compareTo(inJpgFullPath) == 0);

        if (xmp != null) {
            // copy/move via modifying xmp(s)
            Boolean xmpLongFormat = xmp.isLongFormat();
            boolean isLongFormat = (xmpLongFormat != null)
                    ? xmpLongFormat.booleanValue()
                    : LibGlobal.preferLongXmpFormat;

            changedFiles += saveXmp(xmp, outJpgFullPath, isLongFormat, dbg_context);

            if (xmp.isHasAlsoOtherFormat()) {
                changedFiles += saveXmp(xmp, outJpgFullPath, !isLongFormat, dbg_context);
                if (!isSameFile && this.deleteOriginalAfterFinish) FileProcessor.getSidecar(inJpgFullPath, !isLongFormat).delete();
            }

            if (this.deleteOriginalAfterFinish && !isSameFile) {
                // if xmp-file does not exist, nothing happens :-)
                FileProcessor.getSidecar(inJpgFullPath, isLongFormat).delete();
            }
        } else {
            if (!isSameFile) {
                // copy/move via file copy and remove original if neccessary
                changedFiles += copyReplaceIfExist(inJpgFullPath, outJpgFullPath, true, dbg_context);
                changedFiles += copyReplaceIfExist(inJpgFullPath, outJpgFullPath, false, dbg_context);
            }
        }
        return changedFiles;
    }

    private int copyReplaceIfExist(
            String absoluteJpgInPath, String outJpgFullPath,
            boolean longFormat, String dbg_context) throws IOException {
        int changedFiles = 0;
        IFile xmpInFile = FileCommands.getExistingSidecarOrNull(absoluteJpgInPath, longFormat);
        if (xmpInFile != null) {
            FileUtils.copyReplace(
                    xmpInFile, FileCommands.getSidecar(outJpgFullPath, longFormat),
                    this.deleteOriginalAfterFinish, dbg_context);
            changedFiles++;
        }
        return changedFiles;
    }

    private int saveXmp(
            PhotoPropertiesXmpSegment xmp,
            String outFullJpgPath,
            boolean isLongFileName, String dbg_context) throws IOException {
        IFile xmpOutFile = FileCommands.getSidecar(outFullJpgPath, isLongFileName);
        xmp.save(xmpOutFile, LibGlobal.debugEnabledJpgMetaIo, dbg_context);
        return 1;
    }

    private int transferExif(String dbg_context) throws IOException {
        String inJpgFullPath = this.getPath();
        if (inJpgFullPath == null) inJpgFullPath = this.absoluteJpgInPath;
        String outJpgFullPath = (this.absoluteJpgOutPath == null) ? inJpgFullPath : this.absoluteJpgOutPath;
        boolean isSameFile = (outJpgFullPath.compareTo(inJpgFullPath) == 0);

        if (exif != null) {
            if (!isSameFile) {
                exif.saveAttributes(
                        FileFacade.convert("PhotoPropertiesUpdateHandler.transferExif in", inJpgFullPath),
                        FileFacade.convert("PhotoPropertiesUpdateHandler.transferExif out", outJpgFullPath),
                        this.deleteOriginalAfterFinish);
            } else {
                exif.saveAttributes();
            }
        } else if (!isSameFile) {
            // changes are NOT written to exif. Do File copy instead.
            FileUtils.copyReplace(inJpgFullPath, outJpgFullPath, this.deleteOriginalAfterFinish, dbg_context + "-transferExif");
        } else {// same file, no exif changes: nothing to do
            return 0;
        }
        return 1;
    }

    public ExifInterfaceEx getExif() {
        return exif;
    }

    public PhotoPropertiesXmpSegment getXmp() {
        return xmp;
    }
}

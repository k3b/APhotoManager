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

import java.io.File;
import java.io.IOException;
import java.util.Date;

import de.k3b.FotoLibGlobal;
import de.k3b.io.FileCommands;
import de.k3b.io.FileProcessor;
import de.k3b.io.FileUtils;

/**
 * Represents content of exactly one jpg-exif-file with corresponding xmp-file that can be modified
 * via {@link IMetaApi} and {@link #save(String)}.
 *
 * Depending on the global settings handles updating/creating jpg-exif and/or xmp-file.
 * Also handles jpg/xmp file move/copy.
 * Transactionlog and database update is handled by caller
 *
 * Android free implementation.
 *
 * Created by k3b on 21.04.2017.
 */

public class MetaWriterExifXml extends MetaApiWrapper  implements IMetaApi {
    private static final Logger logger = LoggerFactory.getLogger(FotoLibGlobal.LOG_TAG);

    private ExifInterfaceEx exif;   // not null if exif changes are written to jpg file
    private MediaXmpSegment xmp;    // not null if exif changes are written to xmp sidecar file.
    private String absoluteJpgInPath; // where changes are read from.
    private String absoluteJpgOutPath; // where changes are written to. Null meanst same as input
    private boolean deleteOriginalAfterFinish; // true: after save original jpg/mxp are deleted (move instead of copy)
    private long    dbgLoadEndTimestamp;

    /**
     * public api: Factory to create MetaWriterExifXml. Settings/ Internal state determine
     * configuration for MetaWriterExifXml.
     *
     *
     * @param absoluteJpgInPath     where data is read from
     * @param absoluteJpgOutPath    where data changes are written to. Null means same as absoluteJpgInPath.
     * @param deleteOriginalAfterFinish true: after save original jpg/mxp are deleted (move instead of copy)
     * @param dbg_context           for debug log: who called this
     * @return                      new loaded instance
     * @throws IOException
     */
    public static MetaWriterExifXml create(String absoluteJpgInPath, String absoluteJpgOutPath,
                                           boolean deleteOriginalAfterFinish, String dbg_context)
            throws IOException {
        return create(absoluteJpgInPath, absoluteJpgOutPath, deleteOriginalAfterFinish, dbg_context,
                FotoLibGlobal.mediaUpdateStrategy.contains("J"),    // write jpg file
                FotoLibGlobal.mediaUpdateStrategy.contains("X"),    // write xmp file
                FotoLibGlobal.mediaUpdateStrategy.contains("C")    // create xmp if it does not exist
        );
    }

    /**
     * Used by junit tests with no dependency to internal state: factory to create MetaWriterExifXml.
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
    public static MetaWriterExifXml create(String absoluteJpgInPath, String absoluteJpgOutPath,
                                           boolean deleteOriginalAfterFinish, String dbg_context,
                                           boolean writeJpg, boolean writeXmp, boolean createXmpIfNotExist)
            throws IOException {
        long    startTimestamp = 0;
        if (FotoLibGlobal.debugEnabledJpgMetaIo) {
            startTimestamp = new Date().getTime();
        }
        MediaXmpSegment xmp = MediaXmpSegment.loadXmpSidecarContentOrNull(absoluteJpgInPath, dbg_context);
        if ((createXmpIfNotExist) && (xmp == null)) {
            ImageMetaReader jpg = new ImageMetaReader().load(absoluteJpgInPath,null,null,
                    dbg_context + " xmp-file not found. create/extract from jpg ");

            xmp = jpg.getImternalXmp();

            // jpg has no embedded xmp create
            if (xmp == null) xmp = new MediaXmpSegment();

            // xmp should have the same data as exif/iptc
            MediaUtil.copyNonEmpty(xmp, jpg);
        }
        ExifInterfaceEx exif = new ExifInterfaceEx(absoluteJpgInPath, null, xmp, dbg_context);
        if (exif.isValidJpgExifFormat()) {
            exif.setPath(absoluteJpgInPath);
        } else {
            exif = null;

            // if no exif (i.e. png file) always use xmp instead. create xmp if neccessary
            if (xmp == null) xmp = new MediaXmpSegment();
        }

        MetaWriterExifXml result;

        if (exif != null) {
            result = new MetaWriterExifXml(
                    (writeJpg) ? exif : new MetaApiChainReader(xmp, exif), //  (!writeJpg) prefer read from xmp value before exif value
                    (writeJpg) ? exif : xmp,  //  (!writeJpg) modify xmp value only
                    (writeJpg) ? exif : null,  //  (!writeJpg) do not safe changes to jpg exif file
                    (writeXmp) ? xmp : null);   //  (!writeXmp) do not safe changes to xmp-sidecar file
        } else {
            // if no exif (i.e. png file) always use xmp instead
            result = new MetaWriterExifXml(xmp, xmp, null, xmp);

        }

        result.absoluteJpgOutPath = (absoluteJpgOutPath != null) ? absoluteJpgOutPath : absoluteJpgInPath;
        result.absoluteJpgInPath = absoluteJpgInPath;
        result.deleteOriginalAfterFinish = deleteOriginalAfterFinish;
        if (FotoLibGlobal.debugEnabledJpgMetaIo) {
            result.dbgLoadEndTimestamp = new Date().getTime();
            logger.debug(dbg_context + " load[msec]:" + (result.dbgLoadEndTimestamp - startTimestamp));
        }

        return result;
    }

    private MetaWriterExifXml(IMetaApi readChild, IMetaApi writeChild, ExifInterfaceEx exif, MediaXmpSegment xmp)
    {
        super(readChild, writeChild);
        this.exif = exif;
        this.xmp = xmp;
    }

    public void setAbsoluteJpgOutPath(String absoluteJpgOutPath) {
        this.absoluteJpgOutPath = absoluteJpgOutPath;
    }

    public String getAbsoluteJpgOutPath() {
        return this.absoluteJpgOutPath;
    }

    public int save(String dbg_context)  throws IOException {
        long    startTimestamp = 0;
        if (FotoLibGlobal.debugEnabledJpgMetaIo) {
            startTimestamp = new Date().getTime();
        }

        final int result = transferExif(dbg_context)
                + transferXmp(dbg_context);

        if (FotoLibGlobal.debugEnabledJpgMetaIo) {
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
                    : FotoLibGlobal.preferLongXmpFormat;

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

    private int copyReplaceIfExist(String absoluteJpgInPath, String outJpgFullPath, boolean longFormat, String dbg_context) throws IOException {
        int changedFiles = 0;
        File xmpInFile = FileCommands.getExistingSidecarOrNull(absoluteJpgInPath, longFormat);
        if (xmpInFile != null) {
            FileUtils.copyReplace(
                    xmpInFile, FileCommands.getSidecar(outJpgFullPath, longFormat),
                    this.deleteOriginalAfterFinish, dbg_context);
            changedFiles++;
        }
        return changedFiles;
    }

    private int saveXmp(MediaXmpSegment xmp,
                        String outFullJpgPath,
                        boolean isLongFileName, String dbg_context) throws IOException {
        File xmpOutFile = FileCommands.getSidecar(outFullJpgPath, isLongFileName);
        xmp.save(xmpOutFile, FotoLibGlobal.debugEnabledJpgMetaIo, dbg_context);
        return 1;
    }

    private int transferExif(String dbg_context) throws IOException {
        String inJpgFullPath = this.getPath();
        if (inJpgFullPath == null) inJpgFullPath = this.absoluteJpgInPath;
        String outJpgFullPath = (this.absoluteJpgOutPath == null) ? inJpgFullPath : this.absoluteJpgOutPath;
        boolean isSameFile = (outJpgFullPath.compareTo(inJpgFullPath) == 0);

        if (exif != null) {
            if (!isSameFile) {
                exif.saveAttributes(new File(inJpgFullPath), new File(outJpgFullPath),
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

    public MediaXmpSegment getXmp() {
        return xmp;
    }
}

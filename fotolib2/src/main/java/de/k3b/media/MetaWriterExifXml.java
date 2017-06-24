/*
 * Copyright (c) 2017 by k3b.
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

import java.io.File;
import java.io.IOException;

import de.k3b.FotoLibGlobal;
import de.k3b.io.FileCommands;
import de.k3b.io.FileUtils;

/**
 * Depending on the global settings write MetaApi data to exif or xmp or both
 *
 * Created by k3b on 21.04.2017.
 */

public class MetaWriterExifXml extends MetaApiWrapper {
    private ExifInterfaceEx exif;
    private MediaXmpSegment xmp;

    public static MetaWriterExifXml create(String absoluteJpgPath, String dbg_context) throws IOException {
        return create(absoluteJpgPath, dbg_context,
                FotoLibGlobal.mediaUpdateStrategy.contains("J"),    // write jpg file
                FotoLibGlobal.mediaUpdateStrategy.contains("X"),    // write xmp file
                FotoLibGlobal.mediaUpdateStrategy.contains("C"));   // create xmp if it does not exist
    }

    public static MetaWriterExifXml create(String absoluteJpgPath, String dbg_context
                    ,boolean writeJpg, boolean writeXmp, boolean createXmpIfNotExist) throws IOException {
        MediaXmpSegment xmp = MediaXmpSegment.loadXmpSidecarContentOrNull(absoluteJpgPath, dbg_context);
        if ((createXmpIfNotExist) && (xmp == null)) {
            ImageMetaReader jpg = new ImageMetaReader().load(absoluteJpgPath,null,null,
                    dbg_context + " xmp-file not found. create/extract from jpg ");

            xmp = jpg.getImternalXmp();

            // jpg has no embedded xmp create
            if (xmp == null) xmp = new MediaXmpSegment();

            // xmp should have the same data as exif/iptc
            MediaUtil.copyNonEmpty(xmp, jpg);
        }
        ExifInterfaceEx exif = new ExifInterfaceEx(absoluteJpgPath, null, xmp, dbg_context);
        exif.setPath(absoluteJpgPath);

        MetaWriterExifXml result = new MetaWriterExifXml(
                (writeJpg) ? exif : new MetaApiChainReader(xmp, exif), //  (!writeJpg) prefer read from xmp value before exif value
                (writeJpg) ? exif : xmp ,  //  (!writeJpg) modify xmp value only
                (writeJpg) ? exif : null,  //  (!writeJpg) do not safe changes to jpg exif file
                (writeXmp) ? xmp : null);   //  (!writeXmp) do not safe changes to xmp-sidecar file

        return result;
    }

    private MetaWriterExifXml(IMetaApi readChild, IMetaApi writeChild, ExifInterfaceEx exif, MediaXmpSegment xmp)
    {
        super(readChild, writeChild);
        this.exif = exif;
        this.xmp = xmp;
    }

    public void save(String dbg_context)  throws IOException {
        if (exif != null) exif.saveAttributes();
        File xmpFile = FileCommands.getExistingSidecarOrNull(this.getPath());
        if (xmpFile == null) xmpFile = FileCommands.getSidecar(this.getPath(), FotoLibGlobal.preferLongXmpFormat);
        if (xmp != null) xmp.save(xmpFile, true , dbg_context);
    }

    public ExifInterfaceEx getExif() {
        return exif;
    }

    public MediaXmpSegment getXmp() {
        return xmp;
    }
}

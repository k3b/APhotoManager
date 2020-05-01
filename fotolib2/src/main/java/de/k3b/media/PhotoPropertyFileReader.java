/*
 * Copyright (c) 2020 by k3b.
 *
 * This file is part of #APhotoManager (https://github.com/k3b/APhotoManager/)
 *              and #toGoZip (https://github.com/k3b/ToGoZip/).
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

import de.k3b.LibGlobal;
import de.k3b.io.IFile;

/**
 * Facade to combine-loading jpg + xmp
 */
public class PhotoPropertyFileReader implements IPhotoPropertyFileReader {
    private static final Logger logger = LoggerFactory.getLogger(LibGlobal.LOG_TAG);

    // private final IPhotoPropertyFileReader exifFactory = new ExifInterfaceEx();
    private final IPhotoPropertyFileReader exifFactory = new PhotoPropertiesImageReader();

    private IPhotoProperties xmp = null;
    private IPhotoProperties jpg = null;

    public PhotoPropertyFileReader() {
    }

    @Override
    public IPhotoProperties load(IFile jpgFile, IPhotoProperties childProperties, String dbg_context) {
        // PhotoPropertiesXmpSegment xmp = PhotoPropertiesXmpSegment.loadXmpSidecarContentOrNull(jpgFile, dbg_context);
        // PhotoPropertiesImageReader jpg = new PhotoPropertiesImageReader().load(jpgFile, jpgFile.openInputStream(), xmp, dbg_context);
        this.xmp = PhotoPropertiesXmpSegment.loadXmpSidecarContentOrNull(jpgFile, dbg_context);
        this.jpg = exifFactory.load(jpgFile, xmp, dbg_context);
        // new PhotoPropertiesImageReader().load()

        return jpg;
    }

    public IPhotoProperties getXmp() {
        return xmp;
    }

    public IPhotoProperties getJpg() {
        return jpg;
    }
}

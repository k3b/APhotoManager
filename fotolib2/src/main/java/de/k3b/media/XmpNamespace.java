/*
 * Copyright (c) 2016-2017 by k3b.
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

import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMetaFactory;
import com.adobe.xmp.XMPSchemaRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.k3b.FotoLibGlobal;

/**
 * Created by k3b on 10.10.2016.
 */

public enum XmpNamespace {
    NONE("",""),
    EXIF("XMP-exif","http://ns.adobe.com/exif/1.0/"),
    XAP("XMP-xmp","http://ns.adobe.com/xap/1.0/"),
    DC("XMP-dc", "http://purl.org/dc/elements/1.1/"),
    APM("apm", "https://github.com/k3b/APhotoManager/wiki/spec"),
    PHOTOSHOP("photoshop","http://ns.adobe.com/photoshop/1.0/"),
    MICROSOFT_PHOTO("MicrosoftPhoto", "http://ns.microsoft.com/photo/1.0");

    private static final String dbg_context = XmpNamespace.class.getSimpleName();
    private static final Logger logger = LoggerFactory.getLogger(FotoLibGlobal.LOG_TAG);

    private final String prefix;
    private final String uri;

    private XmpNamespace(String prefix, String uri) {
        this.prefix = prefix;

        this.uri = uri;
    }

    public String getUriAsString() {
        return uri;
    }

    @Override
    public String toString() {
        return prefix+"="+uri;
    }

    public String getPrefix() {
        return prefix;
    }

    static {
        try {
            XMPSchemaRegistry schemaRegistry = XMPMetaFactory.getSchemaRegistry();
            schemaRegistry.registerNamespace(APM.getUriAsString(), APM.getPrefix());
            schemaRegistry.registerNamespace(MICROSOFT_PHOTO.getUriAsString(), MICROSOFT_PHOTO.getPrefix());
            schemaRegistry.registerNamespace(PHOTOSHOP.getUriAsString(), PHOTOSHOP.getPrefix());
        } catch (XMPException e) {
            logger.error(dbg_context, "registerNamespace", e);
        }
    }

}

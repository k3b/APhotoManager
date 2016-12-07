/*
 * Copyright (c) 2016 by k3b.
 *
 * This file is part of AndroFotoFinder.
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

/**
 * Created by k3b on 10.10.2016.
 */

public enum XmpNamespace {
    NONE("",""),
    EXIF("XMP-exif","http://ns.adobe.com/exif/1.0/"),
    XAP("XMP-xmp","http://ns.adobe.com/xap/1.0/"),
    DC("XMP-dc", "http://purl.org/dc/elements/1.1/"),
    PHOTOSHOP("photoshop","http://ns.adobe.com/photoshop/1.0/");


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
}

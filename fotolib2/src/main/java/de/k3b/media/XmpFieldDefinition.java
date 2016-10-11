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

import de.k3b.media.XmpNamespace;

/**
 * Created by k3b on 10.10.2016.
 */

public enum XmpFieldDefinition {
    PATH(XmpNamespace.NONE,"SourceFile"), // used by exiftool-csv

    DateTimeOriginal(XmpNamespace.EXIF,"DateTimeOriginal"),
    GPSLatitude(XmpNamespace.EXIF,"GPSLatitude"),
    GPSLongitude(XmpNamespace.EXIF,"GPSLongitude"),

    TITLE(XmpNamespace.DC,"Title"),
    TAGS(XmpNamespace.DC,"Subject"),
    DESCRIPTION(XmpNamespace.DC,"Description");

    private final XmpNamespace xmpNamespace;
    private final String name;

    private XmpFieldDefinition(XmpNamespace xmpNamespace, String name) {
        this.xmpNamespace = xmpNamespace;
        this.name = name;
    }

    public XmpNamespace getXmpNamespace() {
        return xmpNamespace;
    }

    public String getShortName() {
        return name;
    }
}

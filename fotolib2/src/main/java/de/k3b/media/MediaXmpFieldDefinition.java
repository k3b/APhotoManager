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

import com.adobe.xmp.options.PropertyOptions;

/**
 * Supported predefined Xmp-Fileds for {@MediaXmpSegment}.
 *
 * Created by k3b on 10.10.2016.
 */

public enum MediaXmpFieldDefinition {
    SourceFile(XmpNamespace.NONE), // used by exiftool-csv
    FileModifyDate(XmpNamespace.NONE), // used by exiftool-csv

    CreateDate(XmpNamespace.XAP),          // JPhotoTagger default

    DateCreated(XmpNamespace.PHOTOSHOP),  // exiftool default

    DateAcquired(XmpNamespace.MICROSOFT_PHOTO),

    OriginalFileName(XmpNamespace.APM), // apm specific: the original file name of the image file
    AppVersion(XmpNamespace.APM),       // apm specific: creating app/version
    Visibility(XmpNamespace.APM),       // apm specific: Private/Public/....

    DateTimeOriginal(XmpNamespace.EXIF),
    GPSLatitude(XmpNamespace.EXIF),
    GPSLongitude(XmpNamespace.EXIF),

    creator(XmpNamespace.DC,PropertyOptions.ARRAY | PropertyOptions.ARRAY_ORDERED),
    rights(XmpNamespace.DC, PropertyOptions.ARRAY_ALT_TEXT | PropertyOptions.ARRAY_ALTERNATE | PropertyOptions.ARRAY_ALT_TEXT),
    title(XmpNamespace.DC, PropertyOptions.ARRAY | PropertyOptions.ARRAY_ORDERED | PropertyOptions.ARRAY_ALTERNATE | PropertyOptions.ARRAY_ALT_TEXT),
    description(XmpNamespace.DC,
            PropertyOptions.ARRAY_ALT_TEXT | PropertyOptions.ARRAY |
                    PropertyOptions.ARRAY_ORDERED | PropertyOptions.ARRAY_ALTERNATE),
    Rating(XmpNamespace.XAP),

    subject(XmpNamespace.DC, PropertyOptions.ARRAY),        // xmp dc standard for keywords
    LastKeywordIPTC(XmpNamespace.MICROSOFT_PHOTO, PropertyOptions.ARRAY),   // used by microsoft for subject
    LastKeywordXMP(XmpNamespace.MICROSOFT_PHOTO, PropertyOptions.ARRAY)     // used by microsoft for subject

    ;

    private final XmpNamespace xmpNamespace;
    private final int arrayOption;

    private MediaXmpFieldDefinition(XmpNamespace xmpNamespace) {
        this(xmpNamespace, PropertyOptions.NO_OPTIONS);
    }

    private MediaXmpFieldDefinition(XmpNamespace xmpNamespace, int arrayOption) {
        this.xmpNamespace = xmpNamespace;
        this.arrayOption = arrayOption;
    }

    public XmpNamespace getXmpNamespace() {
        return xmpNamespace;
    }

    public String getShortName() {
        return super.toString();
    }

    @Override
    public String toString() {
        return getShortName() + "@" + getXmpNamespace();
    }

    public boolean isArray() {
        return getArrayOption() != PropertyOptions.NO_OPTIONS;
    }

    public int getArrayOption() {
        return arrayOption;
    }
}

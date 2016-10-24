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

import com.adobe.xmp.XMPDateTime;
import com.adobe.xmp.XMPUtils;
import com.adobe.xmp.impl.XMPDateTimeImpl;

import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import de.k3b.io.DateUtil;
import de.k3b.io.GeoUtil;

/**
 * Hides Implementation details of xmp lib
 * Created by k3b on 20.10.2016.
 */

public class MediaXmpItem extends XmpSegment implements IMetaApi {
    /** the full path of the image where this xmp-file belongs to */
    private String path = null;

    /** the full path of the image where this xmp-file belongs to */
    @Override
    public String getPath() {
        return path;
    }

    /** the full path of the image where this xmp-file belongs to */
    @Override
    public IMetaApi setPath(String filePath) {
        this.path = filePath;
        return this;
    }

    @Override
    public Date getDateTimeTaken() {
        return getPropertyAsDate(
                XmpFieldDefinition.CreateDate,   // JPhotoTagger default
                XmpFieldDefinition.DateCreated,  // exiftool default
                XmpFieldDefinition.DateTimeOriginal);
    }

    @Override
    public IMetaApi setDateTimeTaken(Date value) {
        setProperty(XMPUtils.convertFromDate(new XMPDateTimeImpl(value, DateUtil.UTC)), // DateUtil.toIsoDateString(value),
                XmpFieldDefinition.CreateDate,   // JPhotoTagger default
                XmpFieldDefinition.DateCreated,  // exiftool default
                XmpFieldDefinition.DateTimeOriginal); // EXIF
        return this;
    }

    /**
     * latitude, in degrees north.
     *
     * @param latitude
     */
    @Override
    public IMetaApi setLatitude(Double latitude) {
        setProperty(GeoUtil.toXmpStringLatNorth(latitude),
                XmpFieldDefinition.GPSLatitude);
        return this;
    }

    /**
     * longitude, in degrees east.
     *
     * @param longitude
     */
    @Override
    public IMetaApi setLongitude(Double longitude) {
        setProperty(GeoUtil.toXmpStringLonEast(longitude),
                XmpFieldDefinition.GPSLongitude);
        return this;
    }

    @Override
    public Double getLatitude() {
        return GeoUtil.parse(getPropertyAsString(XmpFieldDefinition.GPSLatitude),"NS");
    }

    @Override
    public Double getLongitude() {
        return GeoUtil.parse(getPropertyAsString(XmpFieldDefinition.GPSLongitude),"EW");
    }

    @Override
    public String getTitle() {
        return getPropertyAsString(XmpFieldDefinition.TITLE);
    }

    @Override
    public IMetaApi setTitle(String title) {
        setProperty(title,
                XmpFieldDefinition.TITLE);
        return this;
    }

    @Override
    public String getDescription() {
        return getPropertyAsString(XmpFieldDefinition.DESCRIPTION);
    }

    @Override
    public IMetaApi setDescription(String description) {
        setProperty(description,
                XmpFieldDefinition.DESCRIPTION);
        return this;
    }

    @Override
    public List<String> getTags() {
        return getPropertyArray(XmpFieldDefinition.TAGS);
    }

    @Override
    public IMetaApi setTags(List<String> tags) {
        replacePropertyArray(XmpFieldDefinition.TAGS, tags);
        return this;
    }
}

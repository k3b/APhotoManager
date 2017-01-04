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

import com.adobe.xmp.XMPMetaFactory;
import com.adobe.xmp.XMPUtils;
import com.adobe.xmp.impl.XMPDateTimeImpl;

import java.util.Date;
import java.util.List;

import de.k3b.io.DateUtil;
import de.k3b.io.GeoUtil;

/**
 * {@link XmpSegment} that implements {@link IMetaApi} to read/write xmp.
 *
 * Created by k3b on 20.10.2016.
 */

public class MediaXmpSegment extends XmpSegment implements IMetaApi {
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

    public IMetaApi setOriginalFileName(String filePath) {
        setProperty(filePath, MediaXmpFieldDefinition.OriginalFileName);
        return this;
    }

    @Override
    public Date getDateTimeTaken() {
        return getPropertyAsDate(
                MediaXmpFieldDefinition.CreateDate,   // JPhotoTagger default
                MediaXmpFieldDefinition.DateCreated,  // exiftool default
                MediaXmpFieldDefinition.DateTimeOriginal);
    }

    @Override
    public IMetaApi setDateTimeTaken(Date value) {
        setProperty(XMPUtils.convertFromDate(new XMPDateTimeImpl(value, DateUtil.UTC)), // DateUtil.toIsoDateString(value),
                MediaXmpFieldDefinition.CreateDate,   // JPhotoTagger default
                MediaXmpFieldDefinition.DateCreated,  // exiftool default
                MediaXmpFieldDefinition.DateTimeOriginal); // EXIF
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
                MediaXmpFieldDefinition.GPSLatitude);
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
                MediaXmpFieldDefinition.GPSLongitude);
        return this;
    }

    @Override
    public Double getLatitude() {
        return GeoUtil.parse(getPropertyAsString(MediaXmpFieldDefinition.GPSLatitude),"NS");
    }

    @Override
    public Double getLongitude() {
        return GeoUtil.parse(getPropertyAsString(MediaXmpFieldDefinition.GPSLongitude),"EW");
    }

    @Override
    public String getTitle() {
        return getPropertyAsString(MediaXmpFieldDefinition.title);
    }

    @Override
    public IMetaApi setTitle(String title) {
        setProperty(title,
                MediaXmpFieldDefinition.title);
        return this;
    }

    @Override
    public String getDescription() {
        return getPropertyAsString(MediaXmpFieldDefinition.description);
    }

    @Override
    public IMetaApi setDescription(String description) {
        setProperty(description,
                MediaXmpFieldDefinition.description);
        return this;
    }

    @Override
    public List<String> getTags() {
        return getPropertyArray(MediaXmpFieldDefinition.subject);
    }

    @Override
    public IMetaApi setTags(List<String> tags) {
        replacePropertyArray(tags, MediaXmpFieldDefinition.subject);
        return this;
    }

    /**
     * 5=best .. 1=worst or 0/null unknown
     */
    @Override
    public Integer getRating() {
        String result = getPropertyAsString(MediaXmpFieldDefinition.Rating);
        if ((result != null) && (result.length() > 0)){
            try {
                return Integer.parseInt(result);
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public IMetaApi setRating(Integer value) {
        setProperty(value,
                MediaXmpFieldDefinition.Rating);
        return this;
    }

}

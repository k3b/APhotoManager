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

import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPUtils;
import com.adobe.xmp.impl.XMPDateTimeImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import de.k3b.FotoLibGlobal;
import de.k3b.io.DateUtil;
import de.k3b.io.FileCommands;
import de.k3b.io.GeoUtil;
import de.k3b.io.VISIBILITY;

/**
 * {@link XmpSegment} that implements {@link IMetaApi} to read/write xmp.
 *
 * Created by k3b on 20.10.2016.
 */

public class MediaXmpSegment extends XmpSegment implements IMetaApi {
    private static final String dbg_context = "MediaXmpSegment: ";
    private static final Logger logger = LoggerFactory.getLogger(FotoLibGlobal.LOG_TAG);

    /** the full path of the image where this xmp-file belongs to */
    private String path = null;

    /** true: file.jpg.xmp; false: file.xmp; null: unknown */
    private Boolean longFormat = false;
    private boolean hasAlsoOtherFormat;

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
                "getDateTimeTaken", MediaXmpFieldDefinition.CreateDate,   // JPhotoTagger default
                MediaXmpFieldDefinition.DateCreated,  // exiftool default
                MediaXmpFieldDefinition.DateTimeOriginal,
                MediaXmpFieldDefinition.DateAcquired);
    }

    @Override
    public IMetaApi setDateTimeTaken(Date value) {
        String dateValue = (value == null) ? null : XMPUtils.convertFromDate(new XMPDateTimeImpl(value, DateUtil.UTC));
        setProperty(dateValue, // DateUtil.toIsoDateTimeString(value),
                MediaXmpFieldDefinition.CreateDate,   // JPhotoTagger default
                MediaXmpFieldDefinition.DateCreated,  // exiftool default
                MediaXmpFieldDefinition.DateTimeOriginal, // EXIF
                MediaXmpFieldDefinition.DateAcquired);
        return this;
    }

    /** latitude, in degrees north. (-90 .. +90); longitude, in degrees east.  (-180 .. + 180)    */
    @Override public IMetaApi setLatitudeLongitude(Double latitude, Double longitude) {
        setProperty(GeoUtil.toXmpStringLatNorth(latitude),
                MediaXmpFieldDefinition.GPSLatitude);
        setProperty(GeoUtil.toXmpStringLonEast(longitude),
                MediaXmpFieldDefinition.GPSLongitude);
        return this;
    }

    @Override
    public Double getLatitude() {
        return GeoUtil.parse(getPropertyAsString("getLatitude", MediaXmpFieldDefinition.GPSLatitude),"NS");
    }

    @Override
    public Double getLongitude() {
        return GeoUtil.parse(getPropertyAsString("getLongitude", MediaXmpFieldDefinition.GPSLongitude),"EW");
    }

    @Override
    public String getTitle() {
        return getPropertyAsString("getTitle", MediaXmpFieldDefinition.title);
    }

    @Override
    public IMetaApi setTitle(String title) {
        setProperty(title,
                MediaXmpFieldDefinition.title);
        return this;
    }

    @Override
    public String getDescription() {
        return getPropertyAsString("getDescription", MediaXmpFieldDefinition.description);
    }

    @Override
    public IMetaApi setDescription(String description) {
        setProperty(description,
                MediaXmpFieldDefinition.description);
        return this;
    }

    @Override
    public List<String> getTags() {
        return getPropertyArray("getTags",
                MediaXmpFieldDefinition.subject,
                MediaXmpFieldDefinition.LastKeywordXMP,
                MediaXmpFieldDefinition.LastKeywordIPTC);
    }

    @Override
    public IMetaApi setTags(List<String> tags) {
        replacePropertyArray(tags, MediaXmpFieldDefinition.subject);
        replacePropertyArray(tags, MediaXmpFieldDefinition.LastKeywordXMP);
        replacePropertyArray(tags, MediaXmpFieldDefinition.LastKeywordIPTC);

        return this;
    }

    /**
     * 5=best .. 1=worst or 0/null unknown
     */
    @Override
    public Integer getRating() {
        String result = getPropertyAsString("getRating", MediaXmpFieldDefinition.Rating);
        if ((result != null) && (result.length() > 0)){
            try {
                return Integer.parseInt(result);
            } catch (NumberFormatException ex) {
                logger.error(dbg_context, "getRating", ex);
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

    @Override
    public VISIBILITY getVisibility() {
        String sValue = getPropertyAsString("getVisibility", MediaXmpFieldDefinition.Visibility);
        return VISIBILITY.fromString(sValue);
    }

    @Override
    public IMetaApi setVisibility(VISIBILITY value) {
        String sValue = VISIBILITY.isChangingValue(value) ? value.toString() : null;
        setProperty(value,
                MediaXmpFieldDefinition.Visibility);
        return this;
    }

    // Override adds logging
    @Override
    public XmpSegment setXmpMeta(XMPMeta xmpMeta, String dbg_context) {
        super.setXmpMeta(xmpMeta, dbg_context);
        if (FotoLibGlobal.debugEnabledJpgMetaIo) {
            logger.info(dbg_context + " setXmpMeta " +  MediaUtil.toString(this, false, null, MediaUtil.FieldID.path, MediaUtil.FieldID.clasz));
        }

        return this;
    }

    @Override
    public XmpSegment save(File file, boolean humanReadable, String dbg_context) throws FileNotFoundException {
        fixAttributes(file);
        return super.save(file, humanReadable, dbg_context);
    }

    private void fixAttributes(File file) {
        if (getPropertyAsString("   fixAttributes OriginalFileName", MediaXmpFieldDefinition.OriginalFileName) == null) {
            setProperty(file.getName(), MediaXmpFieldDefinition.OriginalFileName);
        }
        if (getPropertyAsString("   fixAttributes AppVersion", MediaXmpFieldDefinition.AppVersion) == null) {
            setProperty(FotoLibGlobal.appName + "-" + FotoLibGlobal.appVersion, MediaXmpFieldDefinition.AppVersion);
        }
    }

    // Override adds logging
    @Override
    public XmpSegment save(OutputStream os, boolean humanReadable, String dbg_context) {
        super.save(os, humanReadable, dbg_context);
        if (FotoLibGlobal.debugEnabledJpgMetaIo) {
            logger.info(dbg_context + " save " + MediaUtil.toString(this, false, null, MediaUtil.FieldID.path, MediaUtil.FieldID.clasz));
        }

        return this;
    }

    public static MediaXmpSegment loadXmpSidecarContentOrNull(String absoluteJpgPath, String _dbg_context) {
        MediaXmpSegment xmpContent = null;
        FileCommands.XmpFile xmpFile = FileCommands.getExistingSidecarOrNull(absoluteJpgPath);
        String dbg_context = _dbg_context + " loadXmpSidecarContent(" + xmpFile + "): ";
        if ((xmpFile != null) && xmpFile.isFile() && xmpFile.exists() && xmpFile.canRead()) {
            xmpContent = new MediaXmpSegment();
            try {
                xmpContent.load(xmpFile, dbg_context);
                xmpContent.setLongFormat(xmpFile.isLongFormat());
                xmpContent.setHasAlsoOtherFormat(xmpFile.isHasAlsoOtherFormat());
            } catch (FileNotFoundException e) {
                logger.error(dbg_context + "failed " + e.getMessage(), e);
                xmpContent = null;
            }

        } else if (FotoLibGlobal.debugEnabledJpgMetaIo) {
            logger.error(dbg_context + "file not found");
        }

        return xmpContent;
    }


    @Override
    public String toString() {
        return MediaUtil.toString(this);
    }

    /** true: file.jpg.xmp; false: file.xmp */
    public void setLongFormat(Boolean longFormat) {
        this.longFormat = longFormat;
    }

    /** true: file.jpg.xmp; false: file.xmp */
    public Boolean isLongFormat() {
        return longFormat;
    }

    public void setHasAlsoOtherFormat(boolean hasAlsoOtherFormat) {
        this.hasAlsoOtherFormat = hasAlsoOtherFormat;
    }

    public boolean isHasAlsoOtherFormat() {
        return hasAlsoOtherFormat;
    }
}

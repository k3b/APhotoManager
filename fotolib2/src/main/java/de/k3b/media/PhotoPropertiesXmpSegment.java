/*
 * Copyright (c) 2016-2020 by k3b.
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

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import de.k3b.LibGlobal;
import de.k3b.io.DateUtil;
import de.k3b.io.GeoUtil;
import de.k3b.io.VISIBILITY;
import de.k3b.io.XmpFile;
import de.k3b.io.filefacade.FileFacade;
import de.k3b.io.filefacade.IFile;
import de.k3b.media.MediaFormatter.FieldID;
/**
 * {@link XmpSegment} that implements {@link IPhotoProperties} to read/write xmp.
 *
 * Created by k3b on 20.10.2016.
 */

public class PhotoPropertiesXmpSegment extends XmpSegment
        implements IPhotoProperties, IPhotoPropertyFileWriter, IPhotoPropertyFileReader {
    private static final String dbg_context = "PhotoPropertiesXmpSegment: ";
    private static final Logger logger = LoggerFactory.getLogger(LibGlobal.LOG_TAG);

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
    public IPhotoProperties setPath(String filePath) {
        this.path = filePath;
        return this;
    }

    @Override
    public Date getDateTimeTaken() {
        return getPropertyAsDate(
                "getDateTimeTaken", PhotoPropertiesXmpFieldDefinition.CreateDate,   // JPhotoTagger default
                PhotoPropertiesXmpFieldDefinition.DateCreated,  // exiftool default
                PhotoPropertiesXmpFieldDefinition.DateTimeOriginal,
                PhotoPropertiesXmpFieldDefinition.DateAcquired,
                PhotoPropertiesXmpFieldDefinition.DateCreatedIptcXmp);
    }

    @Override
    public IPhotoProperties setDateTimeTaken(Date value) {
        String dateValue = (value == null) ? null : XMPUtils.convertFromDate(new XMPDateTimeImpl(value, DateUtil.UTC));
        setProperty(dateValue, // DateUtil.toIsoDateTimeString(value),
                PhotoPropertiesXmpFieldDefinition.CreateDate,   // JPhotoTagger default
                PhotoPropertiesXmpFieldDefinition.DateCreated,  // exiftool default
                PhotoPropertiesXmpFieldDefinition.DateTimeOriginal, // EXIF
                PhotoPropertiesXmpFieldDefinition.DateAcquired);
        return this;
    }

    /** latitude, in degrees north. (-90 .. +90); longitude, in degrees east.  (-180 .. + 180)    */
    @Override public IPhotoProperties setLatitudeLongitude(Double latitude, Double longitude) {
        setProperty(GeoUtil.toXmpStringLatNorth(latitude),
                PhotoPropertiesXmpFieldDefinition.GPSLatitude);
        setProperty(GeoUtil.toXmpStringLonEast(longitude),
                PhotoPropertiesXmpFieldDefinition.GPSLongitude);
        return this;
    }

    @Override
    public Double getLatitude() {
        return GeoUtil.parse(getPropertyAsString("getLatitude", PhotoPropertiesXmpFieldDefinition.GPSLatitude),"NS");
    }

    @Override
    public Double getLongitude() {
        return GeoUtil.parse(getPropertyAsString("getLongitude", PhotoPropertiesXmpFieldDefinition.GPSLongitude),"EW");
    }

    @Override
    public String getTitle() {
        return getPropertyAsString("getTitle", PhotoPropertiesXmpFieldDefinition.title);
    }

    @Override
    public IPhotoProperties setTitle(String title) {
        setProperty(title,
                PhotoPropertiesXmpFieldDefinition.title);
        return this;
    }

    @Override
    public String getDescription() {
        return getPropertyAsString("getDescription", PhotoPropertiesXmpFieldDefinition.description);
    }

    @Override
    public IPhotoProperties setDescription(String description) {
        setProperty(description,
                PhotoPropertiesXmpFieldDefinition.description);
        return this;
    }

    @Override
    public List<String> getTags() {
        return getPropertyArray("getTags",
                PhotoPropertiesXmpFieldDefinition.subject,
                PhotoPropertiesXmpFieldDefinition.LastKeywordXMP,
                PhotoPropertiesXmpFieldDefinition.LastKeywordIPTC);
    }

    @Override
    public IPhotoProperties setTags(List<String> tags) {
        replacePropertyArray(tags, PhotoPropertiesXmpFieldDefinition.subject);
        replacePropertyArray(tags, PhotoPropertiesXmpFieldDefinition.LastKeywordXMP);
        replacePropertyArray(tags, PhotoPropertiesXmpFieldDefinition.LastKeywordIPTC);

        return this;
    }

    /**
     * 5=best .. 1=worst or 0/null unknown
     */
    @Override
    public Integer getRating() {
        String result = getPropertyAsString("getRating", PhotoPropertiesXmpFieldDefinition.Rating);
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
    public IPhotoProperties setRating(Integer value) {
        setProperty(value,
                PhotoPropertiesXmpFieldDefinition.Rating);
        return this;
    }

    @Override
    public VISIBILITY getVisibility() {
        String sValue = getPropertyAsString("getVisibility", PhotoPropertiesXmpFieldDefinition.Visibility);
        return VISIBILITY.fromString(sValue);
    }

    @Override
    public IPhotoProperties setVisibility(VISIBILITY value) {
        String sValue = VISIBILITY.isChangingValue(value) ? value.toString() : null;
        setProperty(value,
                PhotoPropertiesXmpFieldDefinition.Visibility);
        return this;
    }

    // Override adds logging
    @Override
    public XmpSegment setXmpMeta(XMPMeta xmpMeta, String dbg_context) {
        super.setXmpMeta(xmpMeta, dbg_context);
        if (LibGlobal.debugEnabledJpgMetaIo) {
            logger.info(dbg_context + " setXmpMeta " +  PhotoPropertiesFormatter.format(this, false, null, FieldID.path, FieldID.clasz));
        }

        return this;
    }

    @Override
    public XmpSegment save(IFile file, boolean humanReadable, String dbg_context) throws FileNotFoundException {
        fixAttributes(file);
        return super.save(file, humanReadable, dbg_context);
    }

    private void fixAttributes(IFile file) {
        if (getPropertyAsString("   fixAttributes OriginalFileName", PhotoPropertiesXmpFieldDefinition.OriginalFileName) == null) {
            setProperty(file.getName(), PhotoPropertiesXmpFieldDefinition.OriginalFileName);
        }
        if (getPropertyAsString("   fixAttributes AppVersion", PhotoPropertiesXmpFieldDefinition.AppVersion) == null) {
            setProperty(LibGlobal.appName + "-" + LibGlobal.appVersion, PhotoPropertiesXmpFieldDefinition.AppVersion);
        }
    }

    // Override adds logging
    @Override
    public XmpSegment save(OutputStream os, boolean humanReadable, String dbg_context) {
        super.save(os, humanReadable, dbg_context);
        if (LibGlobal.debugEnabledJpgMetaIo) {
            logger.info(dbg_context + " save " + PhotoPropertiesFormatter.format(this, false, null, FieldID.path, FieldID.clasz));
        }

        return this;
    }

    @Deprecated
    public static PhotoPropertiesXmpSegment loadXmpSidecarContentOrNull(String absoluteJpgPath, String _dbg_context) {
        return loadXmpSidecarContentOrNull(FileFacade.convert(dbg_context, absoluteJpgPath), _dbg_context);
    }

    public static PhotoPropertiesXmpSegment loadXmpSidecarContentOrNull(IFile jpgFile, String _dbg_context) {
        PhotoPropertiesXmpSegment xmpContent = null;
        XmpFile xmpFile = XmpFile.getExistingSidecarOrNull(jpgFile);
        String dbg_context = _dbg_context + " loadXmpSidecarContent(" + xmpFile + "): ";
        if ((xmpFile != null) && xmpFile.isFile() && xmpFile.exists() && xmpFile.canRead()) {
            xmpContent = new PhotoPropertiesXmpSegment();
            try {
                xmpContent.load(xmpFile.openInputStream(), dbg_context);
                xmpContent.setLongFormat(xmpFile.isLongFormat());
                xmpContent.setHasAlsoOtherFormat(xmpFile.isHasAlsoOtherFormat());
            } catch (FileNotFoundException e) {
                logger.error(dbg_context + "failed " + e.getMessage(), e);
                xmpContent = null;
            }

        } else if (LibGlobal.debugEnabledJpgMetaIo) {
            logger.error(dbg_context + "file not found");
        }

        return xmpContent;
    }

    @Override
    public IPhotoProperties load(IFile jpgFile, IPhotoProperties ignore, String dbg_context) {
        return loadXmpSidecarContentOrNull(jpgFile, dbg_context);
    }


    @Override
    public String toString() {
        return PhotoPropertiesFormatter.format(this).toString();
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

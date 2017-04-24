/*
 * Copyright (c) 2016 - 2017 by k3b.
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

// import android.media.ExifInterface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import de.k3b.FotoLibGlobal;
import de.k3b.io.ListUtils;

/**
 * Thin Wrapper around Android-s ExifInterface to read/write exif data from jpg file
 * and also implements IMetaApi
 *
 * Created by k3b on 08.10.2016.
 */

public class ExifInterfaceEx extends ExifInterface implements IMetaApi {
    private static final Logger logger = LoggerFactory.getLogger(LOG_TAG);

    private static final SimpleDateFormat sExifDateTimeFormatter;
    private static final String LIST_DELIMITER = ";";

    static {
        sExifDateTimeFormatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        sExifDateTimeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }


    /** when xmp sidecar file was last modified or 0 */
    private long filelastModified = 0;

    // false for unittests because UserComment = null is not implemented for COM - Marker
    protected static boolean useUserComment = true;

    private final String mDbg_context;
	/** if not null content of xmp sidecar file */
    private final IMetaApi xmpExtern;

    /**
     * Reads Exif tags from the specified JPEG file.
     *  @param absoluteJpgPath
     * @param xmpExtern if not null content of xmp sidecar file
     * @param dbg_context
     */
    public ExifInterfaceEx(String absoluteJpgPath, InputStream in, IMetaApi xmpExtern, String dbg_context) throws IOException {
        super(absoluteJpgPath, in);
        setFilelastModified(mExifFile);

        this.xmpExtern = xmpExtern;
        this.mDbg_context = dbg_context + "->ExifInterfaceEx(" + absoluteJpgPath+ ") ";
        if (FotoLibGlobal.debugEnabledJpgMetaIo) {
            logger.debug(this.mDbg_context +
                    " load: " + MediaUtil.toString(this));
        }
        // Log.d(LOG_TAG, msg);

    }

    @Override
    public void saveAttributes() throws IOException {
        fixAttributes();
        super.saveAttributes();
        setFilelastModified(mExifFile);
        if (FotoLibGlobal.debugEnabledJpgMetaIo) {
            logger.debug(mDbg_context +
                    " saved: " + MediaUtil.toString(this));
        }
    }

    protected void fixAttributes() {
        /*
        if ((mExifFile != null) && (getDateTimeTaken() == null) && (getFilelastModified() != 0)) {
           setDateTimeTaken(new Date(getFilelastModified()));
        }
        */
    }

    @Override
    public String getPath() {
        return (mExifFile != null) ? mExifFile.getAbsolutePath() : null;
    }

    @Override
    public IMetaApi setPath(String filePath) {
        mExifFile = (filePath != null) ? new File(filePath) : null;
        if (xmpExtern != null) xmpExtern.setPath(filePath);
        return this;
    }

    @Override
    public Date getDateTimeTaken(){
        Date result = null;
        if (result == null) result = getAttributeDate(ExifInterfaceEx.TAG_DATETIME_ORIGINAL);
        if (result == null) getAttributeDate(ExifInterfaceEx.TAG_DATETIME);
        if ((result == null) && (xmpExtern != null)) result = xmpExtern.getDateTimeTaken();
        return result;
    }

    @Override
    public ExifInterfaceEx setDateTimeTaken(Date value) {
        String dateInExifFormat = toExifDateTimeString(value);
        setAttribute(ExifInterfaceEx.TAG_DATETIME, dateInExifFormat);
        setAttribute(ExifInterfaceEx.TAG_DATETIME_ORIGINAL, dateInExifFormat);
        if (xmpExtern != null) xmpExtern.setDateTimeTaken(value);
        return this;
    }

    /**
     * returns ref for latitude which is S or N.
     * @return S or N
     */
    private String latitudeRef(Double latitude) {
        if (latitude == null) return null;
        return latitude<0.0d?"S":"N";
    }

    /**
     * returns ref for latitude which is S or N.
     * @return W or E
     */
    private String longitudeRef(Double longitude) {
        if (longitude == null) return null;
        return longitude<0.0d?"W":"E";
    }

    /**
     * convert latitude into DMS (degree minute second) format. For instance<br/>
     * -79.948862 becomes<br/>
     * -79 degrees, 56 minutes, 55903 millisecs (equals 55.903 seconds)
     *  79/1,56/1,55903/1000<br/>
     * It works for latitude and longitude<br/>
     * @param _latitude could be longitude.
     */
    private final String convert(Double _latitude) {
        if (_latitude == null) return null;

        double latitude=Math.abs(_latitude);
        int degree = (int) latitude;
        latitude *= 60;
        latitude -= (degree * 60.0d);
        int minute = (int) latitude;
        latitude *= 60;
        latitude -= (minute * 60.0d);
        int milliSecond = (int) (latitude*1000.0d);

        String sb = String.valueOf(degree) +
                "/1," +
                minute +
                "/1," +
                milliSecond +
                "/1000,";
        return sb;
    }

    private Double mLatitude = null;
    private Double mLongitude = null;
    @Override
    public ExifInterfaceEx setLatitude(Double value) {
        setAttribute(ExifInterfaceEx.TAG_GPS_LATITUDE, convert(value));
        setAttribute(ExifInterfaceEx.TAG_GPS_LATITUDE_REF, latitudeRef(value));
        mLatitude = value;

        if (xmpExtern != null) xmpExtern.setLatitude(value);

        return this;
    }

    @Override
    public ExifInterfaceEx setLongitude(Double value) {
        setAttribute(ExifInterfaceEx.TAG_GPS_LONGITUDE, convert(value));
        setAttribute(ExifInterfaceEx.TAG_GPS_LONGITUDE_REF, longitudeRef(value));
        mLongitude = value;

        if (xmpExtern != null) xmpExtern.setLongitude(value);

        return this;
    }

    @Override
    public Double getLatitude() {
        if (mLatitude == null) {
            loadLatLon();
        }

        if ((mLatitude == null) && (xmpExtern != null)) return xmpExtern.getLatitude();

        return mLatitude;
    }

    @Override
    public Double getLongitude() {
        if (mLongitude == null) {
            loadLatLon();
        }

        if ((mLongitude == null) && (xmpExtern != null)) return xmpExtern.getLongitude();

        return mLongitude;
    }

    @Override
    public String getTitle() {
        String result = null;
        if ((isEmpty(result)) && (xmpExtern != null)) result = xmpExtern.getTitle();
        if (isEmpty(result)) result = getAttribute(TAG_WIN_TITLE);
        // iptc:Headline
        return result;
    }

    @Override
    public IMetaApi setTitle(String value) {
        setAttribute(TAG_WIN_TITLE, value);
        if (xmpExtern != null) xmpExtern.setTitle(value);
        return this;
    }

    /** not implemented in {@link ExifInterface} */
    @Override
    public String getDescription() {
        String result = null;
        if (isEmpty(result)) result = getAttribute(TAG_IMAGE_DESCRIPTION);

        if (isEmpty(result)) result = getAttribute(TAG_WIN_SUBJECT);

		// XMP-dc:Description
        if ((isEmpty(result)) && (xmpExtern != null)) result = xmpExtern.getDescription();

        if (isEmpty(result)) result = getAttribute(TAG_WIN_COMMENT);
		// iptc:Caption-Abstract

        // NOTE: write not fully supported for TAG_USER_COMMENT in tiff-com-segment
        if (useUserComment && isEmpty(result)) result = getAttribute(TAG_USER_COMMENT);

        return result;
    }

    private static boolean isEmpty(String result) {
        return (result == null); // || (result.length() == 0);
    }

    /** not implemented in {@link ExifInterface} */
    @Override
    public IMetaApi setDescription(String value) {
        setAttribute(TAG_IMAGE_DESCRIPTION, value);
        setAttribute(TAG_WIN_SUBJECT, value);

        if (xmpExtern != null) xmpExtern.setDescription(value);
        setAttribute(TAG_WIN_COMMENT, value);

        // NOTE: write not fully supported for TAG_USER_COMMENT in tiff-com-segment
        if (useUserComment) setAttribute(TAG_USER_COMMENT, value);
        return this;
    }

    /** not implemented in {@link ExifInterface} */
    @Override
    public List<String> getTags() {
        List<String> result = null;
        if ((result == null) && (xmpExtern != null)) result = xmpExtern.getTags();
        if ((result == null) || (result.size() == 0)) {
            String s = getAttribute(TAG_WIN_KEYWORDS);
            if (s != null) result = ListUtils.fromString(s, LIST_DELIMITER);
        }
        return result;
    }

    /** not implemented in {@link ExifInterface} */
    @Override
    public IMetaApi setTags(List<String> value) {
        setAttribute(TAG_WIN_KEYWORDS, (value == null) ? null : ListUtils.toString(value, LIST_DELIMITER));
        if (xmpExtern != null) xmpExtern.setTags(value);
        return this;
    }

    /** not implemented in {@link ExifInterface} */
    @Override
    public Integer getRating() {
        Integer result = null;
        if ((result == null) && (xmpExtern != null)) result = xmpExtern.getRating();
        if (result == null) {
            int r = getAttributeInt(TAG_WIN_RATING, -1);
            if (r != -1) result = Integer.valueOf(r);
        }
        return result;
    }

    /** not implemented in {@link ExifInterface} */
    @Override
    public IMetaApi setRating(Integer value) {
        setAttribute(TAG_WIN_RATING, (value != null) ? value.toString() : null);
        if (xmpExtern != null) xmpExtern.setRating(value);
        return this;
    }

    /** return the image orinentation as id (one of the ORIENTATION_ROTATE_XXX constants) */
    private int getOrientationId() {
        return getAttributeInt(
                ExifInterfaceEx.TAG_ORIENTATION, -1);
    }

    private static final int ORIENTATION_ROTATE_180 = 3;
    private static final int ORIENTATION_ROTATE_90 = 6;  // rotate 90 cw to right it
    private static final int ORIENTATION_ROTATE_270 = 8;  // rotate 270 to right it

    /** return image orinentation in degrees (0, 90,180,270) or 0 if inknown */
    public int getOrientationInDegrees() {
        int orientation = getOrientationId();
        if (orientation != -1) {
            // We only recognize a subset of orientation tag values.
            int degree;
            switch (orientation) {
                case ExifInterfaceEx.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterfaceEx.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterfaceEx.ORIENTATION_ROTATE_270:
                    return 270;
                default:
            }
        }
        return 0;
    }

    protected Date getAttributeDate(String tag) {
        String dateTimeString =  this.getAttribute(tag);

        if (isEmpty(dateTimeString)) return null;

        ParsePosition pos = new ParsePosition(0);
        try {
            return sExifDateTimeFormatter.parse(dateTimeString, pos);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    protected static String toExifDateTimeString(Date value) {
        final String exifDate = (value != null) ? sExifDateTimeFormatter.format(value) : null;
        return exifDate;
    }

    private void loadLatLon() {
        float[] latlng = new float[2];
        if (getLatLong(latlng)) {
            mLatitude = Double.valueOf(latlng[0]);
            mLongitude = Double.valueOf(latlng[1]);
        }
    }


    /** when xmp sidecar file was last modified or 0 */
    public void setFilelastModified(File file) {
        if (file != null) this.filelastModified = file.lastModified();
    }

    /** when xmp sidecar file was last modified or 0 */
    public long getFilelastModified() {
        return filelastModified;
    }

}

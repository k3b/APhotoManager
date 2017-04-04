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

import java.io.IOException;
import java.io.InputStream;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import de.k3b.io.ListUtils;

/**
 * Thin Wrapper around Android-s ExifInterface to read/write exif data from jpg file
 * Created by k3b on 08.10.2016.
 */

public class ExifInterfaceEx extends ExifInterface implements IMetaApi {
    private static final SimpleDateFormat sExifDateTimeFormatter;
    private static final String LIST_DELIMITER = ";";

    static {
        sExifDateTimeFormatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        sExifDateTimeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

	/** if not null content of xmp sidecar file */
    private final IMetaApi xmpExtern;

    /**
     * Reads Exif tags from the specified JPEG file.
     *
     * @param filename
     * @param xmpExtern if not null content of xmp sidecar file
     */
    public ExifInterfaceEx(String filename, InputStream in, IMetaApi xmpExtern) throws IOException {
        super(filename, in);
        this.xmpExtern = xmpExtern;
    }
    public ExifInterfaceEx(String filename, IMetaApi xmpExtern) throws IOException {
        this(filename, null, xmpExtern);
    }

    @Override
    public String getPath() {
        return mFilename;
    }

    @Override
    public IMetaApi setPath(String filePath) {
        mFilename = filePath;
        return this;
    }

    @Override
    public Date getDateTimeTaken(){
        Date result = getAttributeDate(ExifInterfaceEx.TAG_DATETIME);
        if ((result == null) && (xmpExtern != null)) result = xmpExtern.getDateTimeTaken();
        return result;
    }

    @Override
    public ExifInterfaceEx setDateTimeTaken(Date value) {
        setAttribute(ExifInterfaceEx.TAG_DATETIME, toExifDateTimeString(value));
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
        if ((result == null) && (xmpExtern != null)) result = xmpExtern.getTitle();
        if (result == null) result = getAttribute(TAG_WIN_TITLE);
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
        if (result == null) result = getAttribute(TAG_IMAGE_DESCRIPTION);
        if (result == null) result = getAttribute(TAG_USER_COMMENT);

		// XMP-dc:Description
        if ((result == null) && (xmpExtern != null)) result = xmpExtern.getDescription(); 

        if (result == null) result = getAttribute(TAG_WIN_COMMENT);
		// iptc:Caption-Abstract
		
        return result;
    }

    /** not implemented in {@link ExifInterface} */
    @Override
    public IMetaApi setDescription(String value) {
        setAttribute(TAG_IMAGE_DESCRIPTION, value);
        setAttribute(TAG_USER_COMMENT, value);
        setAttribute(TAG_WIN_COMMENT, value);
        if (xmpExtern != null) xmpExtern.setDescription(value);
        return this;
    }

    /** not implemented in {@link ExifInterface} */
    @Override
    public List<String> getTags() {
        List<String> result = null;
        if ((result == null) && (xmpExtern != null)) result = xmpExtern.getTags();
        if (result == null) {
            String s = getAttribute(TAG_WIN_KEYWORDS);
            if (s != null) result = ListUtils.fromString(s, LIST_DELIMITER);
        }
        return result;
    }

    /** not implemented in {@link ExifInterface} */
    @Override
    public IMetaApi setTags(List<String> value) {
        setAttribute(TAG_WIN_TITLE, (value == null) ? null : ListUtils.toString(value, LIST_DELIMITER));
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

    protected Date getAttributeDate(String tag) {
        String dateTimeString =  this.getAttribute(tag);

        if (dateTimeString == null) return null;

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
}

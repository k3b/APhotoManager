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

package de.k3b.android.util;

import android.media.ExifInterface;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import de.k3b.media.IMetaApi;

/**
 * Thin Wrapper around Android-s ExifInterface to read/write exif data from jpg file
 * Created by k3b on 08.10.2016.
 */

public class ExifInterfaceEx extends ExifInterface implements IMetaApi {
    private static final String NL = "\n";

    private static final SimpleDateFormat sExifDateTimeFormatter;

    static {
        sExifDateTimeFormatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        sExifDateTimeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    protected String mFilename;

    /**
     * Reads Exif tags from the specified JPEG file.
     *
     * @param filename
     */
    public ExifInterfaceEx(String filename) throws IOException {
        super(filename);
        mFilename = filename;
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
    public Date getDateTimeTaken() {
        String dateTimeString =  this.getAttribute(ExifInterfaceEx.TAG_DATETIME);
        if (dateTimeString == null) return null;

        ParsePosition pos = new ParsePosition(0);
        try {
            return sExifDateTimeFormatter.parse(dateTimeString, pos);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    public ExifInterfaceEx setDateTimeTaken(Date value) {
        setAttribute(ExifInterfaceEx.TAG_DATETIME, toExifDateTimeString(value));
        return this;
    }

    public static String toExifDateTimeString(Date value) {
        final String exifDate = (value != null) ? sExifDateTimeFormatter.format(value) : null;
        return exifDate;
    }

    @Nullable
    public Map<String, String> getAttributes() {
        // access private attribute ExifInterface.mAttributes via reflection
        // http://stackoverflow.com/questions/1196192/how-do-i-read-a-private-field-in-java
        try {

             /*---  [GETING VALUE FROM PRIVATE FIELD]  ---*/
            Field f = ExifInterface.class.getDeclaredField("mAttributes");
            f.setAccessible(true);//Abracadabra
            Object field = f.get((ExifInterface) this);
            Map<String, String> exifAttributes = (field instanceof Map) ? (Map<String, String>) field : null;
            if  ((exifAttributes != null) && (exifAttributes.size() > 0)) return exifAttributes;

        } catch (Exception ex) {
            // ??? add logging to find out what goes wrong
        }
        return null;
    }

    public String getDebugString(String seperator) {
        StringBuilder builder = new StringBuilder();
        Map<String, String> exifAttributes = this.getAttributes();

        if ((exifAttributes != null) && (exifAttributes.size() > 0)) {
            // TreeMap to sort by key
            JpgMetaWorkflow.addAttributes(builder, new TreeMap<String, String>(exifAttributes));
            Double latitude = getLatitude();
            if (latitude != null) {
                builder.append("GPS Latitude: ").append(latitude).append(seperator);
                builder.append("GPS Longitude: ").append(getLongitude()).append(seperator);
            }
        } else {
            builder.append("Date & Time: ").append(getExifTag(toExifDateTimeString(getDateTimeTaken()))).append(seperator +
                    seperator);

            Double latitude = getLatitude();
            if (latitude != null) {
                builder.append("GPS Latitude: ").append(latitude).append(seperator);
                builder.append("GPS Longitude: ").append(getLongitude()).append(seperator);
                builder.append("GPS Altitude: ").append(getAltitude(0)).append(seperator);
                builder.append("GPS Date & Time: ").append(getExifTag(ExifInterfaceEx.TAG_GPS_DATESTAMP)).append(" "
                ).append(getExifTag(ExifInterfaceEx.TAG_GPS_TIMESTAMP)).append(seperator +
                        seperator);
                builder.append("GPS Processing Method: ").append(getExifTag(ExifInterfaceEx.TAG_GPS_PROCESSING_METHOD)).append(seperator);
            }

            builder.append("Camera Make: ").append(getExifTag(ExifInterfaceEx.TAG_MAKE)).append(seperator);
            builder.append("Camera Model: ").append(getExifTag(ExifInterfaceEx.TAG_MODEL)).append(seperator);
            builder.append("Flash: ").append(getExifTag(ExifInterfaceEx.TAG_FLASH)).append(seperator);
            builder.append("Focal Length: ").append(getExifTag(ExifInterfaceEx.TAG_FOCAL_LENGTH)).append(seperator +
                    seperator);

            builder.append("Image Length: ").append(getExifTag(ExifInterfaceEx.TAG_IMAGE_LENGTH)).append(seperator);
            builder.append("Image Width: ").append(getExifTag(ExifInterfaceEx.TAG_IMAGE_WIDTH)).append(seperator +
                    seperator);
            builder.append("Camera Orientation: ").append(getExifTag(ExifInterfaceEx.TAG_ORIENTATION)).append(seperator);
            builder.append("Camera White Balance: ").append(getExifTag(ExifInterfaceEx.TAG_WHITE_BALANCE)).append(seperator);
        }
        if (mFilename != null) {
            File filePath = new File(mFilename);
            Date date = new Date(filePath.lastModified());
            builder.append(seperator).append("filedate: ").append(ExifInterfaceEx.toExifDateTimeString(date)).append(seperator);;
            builder.append("filemode: ").append( "" + (filePath.canRead() ? "r":"-") + (filePath.canWrite() ? "w":"-") + (filePath.canExecute() ? "x":"-")).append(seperator);;
        }

        return builder.toString();
    }

    private String getExifTag(String tag){
        String attribute = getAttribute(tag);

        return (null != attribute ? attribute : "");
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
    public ExifInterfaceEx setLatitude(Double latitude) {
        setAttribute(ExifInterfaceEx.TAG_GPS_LATITUDE, convert(latitude));
        setAttribute(ExifInterfaceEx.TAG_GPS_LATITUDE_REF, latitudeRef(latitude));
        mLatitude = latitude;
        return this;
    }

    @Override
    public ExifInterfaceEx setLongitude(Double longitude) {
        setAttribute(ExifInterfaceEx.TAG_GPS_LONGITUDE, convert(longitude));
        setAttribute(ExifInterfaceEx.TAG_GPS_LONGITUDE_REF, longitudeRef(longitude));
        mLongitude = longitude;
        return this;
    }

    @Override
    public Double getLatitude() {
        if (mLatitude == null) {
            loadLatLon();
        }
        return mLatitude;
    }

    @Override
    public Double getLongitude() {
        if (mLongitude == null) {
            loadLatLon();
        }
        return mLongitude;
    }

    /** not implemented in {@link ExifInterface} */
    @Override
    public String getTitle() {
        return null;
    }

    /** not implemented in {@link ExifInterface} */
    @Override
    public IMetaApi setTitle(String title) {
        return this;
    }

    /** not implemented in {@link ExifInterface} */
    @Override
    public String getDescription() {
        return null;
    }

    /** not implemented in {@link ExifInterface} */
    @Override
    public IMetaApi setDescription(String description) {
        return this;
    }

    /** not implemented in {@link ExifInterface} */
    @Override
    public List<String> getTags() {
        return null;
    }

    /** not implemented in {@link ExifInterface} */
    @Override
    public IMetaApi setTags(List<String> tags) {
        return this;
    }

    /** not implemented in {@link ExifInterface} */
    @Override
    public Integer getRating() {
        return null;
    }

    /** not implemented in {@link ExifInterface} */
    @Override
    public IMetaApi setRating(Integer value) {
        return this;
    }

    private void loadLatLon() {
        float[] latlng = new float[2];
        if (getLatLong(latlng)) {
            mLatitude = Double.valueOf(latlng[0]);
            mLongitude = Double.valueOf(latlng[1]);
        }
    }
}

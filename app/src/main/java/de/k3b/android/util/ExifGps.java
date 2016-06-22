/*
 * Copyright (c) 2015 by k3b.
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
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import de.k3b.android.androFotoFinder.Global;

/**
 * Write geo data (lat/lon) to photo
 * Based on http://stackoverflow.com/questions/5280479/how-to-save-gps-coordinates-in-exif-data-on-android
 *
 * Created by k3b on 25.08.2015.
 */
public class ExifGps {
    private static SimpleDateFormat sFormatter;

    static {
        sFormatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        sFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static StringBuilder debugExif(StringBuilder sb, String context, ExifInterface exif, File filePath) {
        if (sb != null) {
            sb.append("\n\t").append(context).append("\t: ");

            if (exif != null) {
                float[] latLong = new float[2];
                if (exif.getLatLong(latLong)) {
                    sb.append(ExifInterface.TAG_GPS_LATITUDE).append("=").append(latLong[0]).append(" ")
                            .append(ExifInterface.TAG_GPS_LONGITUDE).append("='").append(latLong[1]).append("' ");
                }
                sb.append(ExifInterface.TAG_DATETIME).append("='").append(exif.getAttribute(ExifInterface.TAG_DATETIME)).append("' ");
                sb.append(ExifInterface.TAG_MAKE).append("='").append(exif.getAttribute(ExifInterface.TAG_MAKE)).append("' ");
                sb.append(ExifInterface.TAG_MODEL).append("='").append(exif.getAttribute(ExifInterface.TAG_MODEL)).append("' ");
            }

            if (filePath != null) {
                sb.append("filedate='").append(sFormatter.format(new Date(filePath.lastModified()))).append("' ")
                .append(filePath.canRead() ? "r":"-").append(filePath.canWrite() ? "w":"-").append(filePath.canExecute() ? "x":"-");
            }
        }
        return sb;
    }

    public static boolean saveLatLon(File filePath, double latitude, double longitude, String appName, String appVersion) {
        StringBuilder sb = (Global.debugEnabled)
                ? sb = createDebugStringBuilder(filePath)
                : null;
        if (filePath.canWrite()) {
            ExifInterface exif = null;
            try {
                long lastModified = filePath.lastModified();
                exif = new ExifInterface(filePath.getAbsolutePath());
                debugExif(sb, "old", exif, filePath);

                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, convert(latitude));
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, latitudeRef(latitude));
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, convert(longitude));
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, longitudeRef(longitude));
                // exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, convert(0));
                // exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, longitudeRef(0));

                // #29 set data if not in exif: date, make model
                if ((lastModified != 0) && (null == exif.getAttribute(ExifInterface.TAG_DATETIME))) {
                    final String exifDate = sFormatter.format(new Date(lastModified));
                    exif.setAttribute(ExifInterface.TAG_DATETIME, exifDate);
                }

                if ((appName != null) && (null == exif.getAttribute(ExifInterface.TAG_MAKE))) {
                    exif.setAttribute(ExifInterface.TAG_MAKE, appName);
                }

                if ((appVersion != null) && (null == exif.getAttribute(ExifInterface.TAG_MODEL))) {
                    exif.setAttribute(ExifInterface.TAG_MODEL, appVersion);
                }
                debugExif(sb, "assign ", exif, filePath);

                exif.saveAttributes();

                // preseve file modification date
                filePath.setLastModified(lastModified);

                if (sb != null) {
                    exif = new ExifInterface(filePath.getAbsolutePath());
                    debugExif(sb, "new ", exif, filePath);

                    Log.d(Global.LOG_CONTEXT, sb.toString());
                }
                return true;
            } catch (IOException e) {
                if (sb == null) {
                    sb = createDebugStringBuilder(filePath);
                    debugExif(sb, "err content", exif, filePath);
                }

                sb.append("error='").append(e.getMessage()).append("' ");
                Log.e(Global.LOG_CONTEXT, sb.toString(), e);
                return false;
            }
        } else {
            if (sb == null) {
                sb = createDebugStringBuilder(filePath);
            }

            sb.append("error='file is write protected' ");
            Log.e(Global.LOG_CONTEXT, sb.toString());
            return false;
        }
    }

    private static StringBuilder createDebugStringBuilder(File filePath) {
        return new StringBuilder("Set Exif to file='").append(filePath.getAbsolutePath()).append("'\n\t");
    }

    /**
     * returns ref for latitude which is S or N.
     * @param latitude
     * @return S or N
     */
    private static String latitudeRef(double latitude) {
        return latitude<0.0d?"S":"N";
    }

    /**
     * returns ref for latitude which is S or N.
     * @param longitude
     * @return W or E
     */
    private static String longitudeRef(double longitude) {
        return longitude<0.0d?"W":"E";
    }

    /**
     * convert latitude into DMS (degree minute second) format. For instance<br/>
     * -79.948862 becomes<br/>
	 * -79 degrees, 56 minutes, 55903 millisecs (equals 55.903 seconds)
     *  79/1,56/1,55903/1000<br/>
     * It works for latitude and longitude<br/>
     * @param latitude could be longitude.
     * @return
     */
    private static final String convert(double latitude) {
        latitude=Math.abs(latitude);
        int degree = (int) latitude;
        latitude *= 60;
        latitude -= (degree * 60.0d);
        int minute = (int) latitude;
        latitude *= 60;
        latitude -= (minute * 60.0d);
        int milliSecond = (int) (latitude*1000.0d);

        StringBuilder sb = new StringBuilder(20);
        sb.append(degree);
        sb.append("/1,");
        sb.append(minute);
        sb.append("/1,");
        sb.append(milliSecond);
        sb.append("/1000,");
        return sb.toString();
    }
}

/*
 * Copyright (c) 2015-2016 by k3b.
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

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.media.ExifInterfaceEx;

/**
 * Write geo data (lat/lon) to photo
 * Based on http://stackoverflow.com/questions/5280479/how-to-save-gps-coordinates-in-exif-data-on-android
 *
 * Created by k3b on 25.08.2015.
 */
public class JpgMetaWorkflow {
    private static StringBuilder debugExif(StringBuilder sb, String context, ExifInterfaceEx exif, File filePath) {
        if (sb != null) {
            sb.append("\n\t").append(context).append("\t: ");

            if (exif != null) {
                sb.append(exif.getDebugString(" "));
            }
        }
        return sb;
    }

    public static boolean saveLatLon(File filePath, Double latitude, Double longitude, String appName, String appVersion) {
        StringBuilder sb = (Global.debugEnabled)
                ? sb = createDebugStringBuilder(filePath)
                : null;
        if (filePath.canWrite()) {
            ExifInterfaceEx exif = null;
            try {
                long lastModified = filePath.lastModified();
                exif = new ExifInterfaceEx(filePath.getAbsolutePath(), null, null, "saveLatLon");
                debugExif(sb, "old", exif, filePath);

                exif.setLatitude(latitude);
                exif.setLongitude(longitude);
                // exif.setAttribute(ExifInterfaceEx.TAG_GPS_ALTITUDE, convert(0));
                // exif.setAttribute(ExifInterfaceEx.TAG_GPS_ALTITUDE_REF, longitudeRef(0));

                // #29 set data if not in exif: date, make model
                if ((lastModified != 0) && (null == exif.getDateTimeTaken())) {
                    exif.setDateTimeTaken(new Date(lastModified));
                }

                if ((appName != null) && (null == exif.getAttribute(ExifInterfaceEx.TAG_MAKE))) {
                    exif.setAttribute(ExifInterfaceEx.TAG_MAKE, appName);
                }

                if ((appVersion != null) && (null == exif.getAttribute(ExifInterfaceEx.TAG_MODEL))) {
                    exif.setAttribute(ExifInterfaceEx.TAG_MODEL, appVersion);
                }
                debugExif(sb, "assign ", exif, filePath);

                exif.saveAttributes();

                // preseve file modification date
                filePath.setLastModified(lastModified);

                if (sb != null) {
                    exif = new ExifInterfaceEx(filePath.getAbsolutePath(), null, null, "dbg in saveLatLon");
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


    // Translate exif-orientation code (0..8) to exifOrientationCode2RotationDegrees (clockwise)
    // http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/EXIF.html
    private static final short[] exifOrientationCode2RotationDegrees = {
            0,     // EXIF Orientation constants:
            0,     // 1 = Horizontal (normal)
            0,     // 2 = (!) Mirror horizontal
            180,   // 3 = Rotate 180
            180,   // 4 = (!) Mirror vertical
            90,    // 5 = (!) Mirror horizontal and rotate 270 CW
            90,    // 6 = Rotate 90 CW
            270,   // 7 = (!) Mirror horizontal and rotate 90 CW
            270};  // 8 = Rotate 270 CW

    /**
     * Get necessary rotation for image file from exif.
     *
     * @param fullPathToImageFile The filename.
     * @return right-rotate (in degrees) image according to exifdata.
     */
    public static int getRotationFromExifOrientation(String fullPathToImageFile) {
        try {
            ExifInterfaceEx exif = new ExifInterfaceEx(fullPathToImageFile, null, null, "getRotationFromExifOrientation");
            int orientation = exif.getAttributeInt(ExifInterfaceEx.TAG_ORIENTATION, 0);
            if ((orientation >= 0) && (orientation < exifOrientationCode2RotationDegrees.length))
                return exifOrientationCode2RotationDegrees[orientation];
        }
        catch (Exception e) {
        }
        return 0;
    }


}

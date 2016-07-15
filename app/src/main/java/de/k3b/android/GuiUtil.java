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
 
package de.k3b.android;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.ExifInterface;

/**
 * gui utils
 */
public class GuiUtil {
    public static String getAppVersionName(final Context context) {
        try {

            final String versionName = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
            return versionName;
        } catch (final NameNotFoundException e) {
        }
        return null;
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
            ExifInterface exif = new ExifInterface(fullPathToImageFile);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
            if ((orientation >= 0) && (orientation < exifOrientationCode2RotationDegrees.length))
                return exifOrientationCode2RotationDegrees[orientation];
        }
        catch (Exception e) {
        }
        return 0;
    }

}

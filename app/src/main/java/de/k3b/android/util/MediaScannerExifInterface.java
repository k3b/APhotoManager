/*
 * Copyright (c) 2017 by k3b.
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

package de.k3b.android.util;


import android.content.ContentValues;
import android.content.Context;

import java.io.IOException;

import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.api.IGeoPointInfo;
import de.k3b.media.ExifInterfaceEx;
import de.k3b.media.IMetaApi;

/**
 * Created by k3b on 11.04.2017.
 */

public class MediaScannerExifInterface extends MediaScanner {
    public MediaScannerExifInterface(Context context) {
        super(context);
    }

    @Override
    protected IMetaApi loadNonMediaValues(ContentValues destinationValues, String absoluteJpgPath) {
        ExifInterfaceEx exif = null;
        try {
            exif = new ExifInterfaceEx(absoluteJpgPath, null);
        } catch (IOException ex) {
            // exif is null
        }

        if (exif != null) {
            int orientation = exif.getAttributeInt(
                    ExifInterfaceEx.TAG_ORIENTATION, -1);
            if (orientation != -1) {
                // We only recognize a subset of orientation tag values.
                int degree;
                switch(orientation) {
                    case ExifInterfaceEx.ORIENTATION_ROTATE_90:
                        degree = 90;
                        break;
                    case ExifInterfaceEx.ORIENTATION_ROTATE_180:
                        degree = 180;
                        break;
                    case ExifInterfaceEx.ORIENTATION_ROTATE_270:
                        degree = 270;
                        break;
                    default:
                        degree = 0;
                        break;
                }
                destinationValues.put(DB_ORIENTATION, degree);
            }
        }
        return exif;
    }

    @Override
    public IGeoPointInfo getPositionFromFile(String absolutePath, String id) {
        ExifInterfaceEx exif = null;
        try {
            exif = new ExifInterfaceEx(absolutePath, null);
        } catch (IOException ex) {
            // exif is null
        }

        if (exif != null) {
            Double latitude = exif.getLatitude();
            if (latitude != null) {
                return new GeoPointDto(latitude, exif.getLongitude(), GeoPointDto.NO_ZOOM).setId(id);
            }
        }

        return null;
    }

}

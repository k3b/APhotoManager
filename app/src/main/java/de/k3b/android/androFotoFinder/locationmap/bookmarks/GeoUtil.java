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

package de.k3b.android.androFotoFinder.locationmap.bookmarks;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.File;
import java.io.FileOutputStream;

import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.api.IGeoPointInfo;

/**
 * Created by k3b on 18.03.2015.
 */
public class GeoUtil {
    /** convert from osm {@link org.osmdroid.api.IGeoPoint} to k3b - {@link de.k3b.geo.api.GeoPointDto} */
    public static GeoPointDto createBookmark(IGeoPoint center, int zoomLevel, String name) {
        return createBookmark(center, zoomLevel, name, new GeoPointDto());
    }

    /** convert from osm {@link org.osmdroid.api.IGeoPoint} to k3b - {@link de.k3b.geo.api.GeoPointDto} */
    public static GeoPointDto createBookmark(IGeoPoint center, int zoomLevel, String name, GeoPointDto destination) {
        return destination
                .setLatitude(center.getLatitude())
                .setLongitude(center.getLongitude())
                .setZoomMin(zoomLevel)
                .setName(name);
    }

    /** convert to osm {@link org.osmdroid.api.IGeoPoint} from k3b - {@link de.k3b.geo.api.GeoPointDto} */
    public static IGeoPoint createOsmPoint(IGeoPointInfo geoInfo) {
        return  new GeoPoint(geoInfo.getLatitude(), geoInfo.getLongitude());
    }

    // see http://stackoverflow.com/questions/19694642/outofmemory-exception-when-creating-bitmap-from-osm-mapview
    /** create {@link android.graphics.Bitmap} from {@link org.osmdroid.views.MapView} */
    public static Bitmap createBitmapFromMapView(final MapView mapview, final int newWidth, final int newHeight) {
        mapview.setDrawingCacheEnabled(true);
        Bitmap result = scaleBitmap(mapview.getDrawingCache(), newWidth, newHeight);
        mapview.setDrawingCacheEnabled(false);
        return result;
    }

    // see http://stackoverflow.com/questions/4821488/bad-image-quality-after-resizing-scaling-bitmap
    /** create a scaled-down {@link android.graphics.Bitmap}  */
    public static Bitmap scaleBitmap(final Bitmap source, final int newWidth, final int newHeight) {
        Bitmap scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);

        float ratioX = newWidth / (float) source.getWidth();
        float ratioY = newHeight / (float) source.getHeight();

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, 0, 0);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(source, 0, 0, new Paint(Paint.FILTER_BITMAP_FLAG));
        return scaledBitmap;
    }

    public static void saveBitmapAsFile(final Bitmap bmp, File imageFile) {
        File dir = imageFile.getParentFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            final FileOutputStream out = new FileOutputStream(imageFile);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}

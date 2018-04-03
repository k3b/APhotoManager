/*
 * Copyright (c) 2015-2016 by k3b.
 *
 * This file is part of LocationMapViewer.
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

package de.k3b.android.osmdroid;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

/**
 * Helper for OsmDroid lib.
 *
 * Created by k3b on 16.03.2015.
 */
public class OsmdroidUtil {
    // osmdroid supports 0..29.0 but this app only 0..22
    private static int MAX_ZOOM_LEVEL = 22;

    public static final int NO_ZOOM = -1; // GeoPointDto.NO_ZOOM;
    public static final int RECALCULATE_ZOOM = NO_ZOOM; // GeoPointDto.NO_ZOOM;

    /**
     * Similar to MapView.zoomToBoundingBox that seems to be to inexact.
     * @param zoom if NO_ZOOM (-1) zoom is calculated from min and max
     */
    public static int zoomTo(MapView mapView, int zoom, IGeoPoint min, IGeoPoint max) {
        int calculatedZoom = zoom;
        MapTileProviderBase tileProvider = mapView.getTileProvider();
        IMapController controller = mapView.getController();
        IGeoPoint center = min;

        if (max != null) {
            center = new GeoPoint((max.getLatitude() + min.getLatitude()) / 2, (max.getLongitude() + min.getLongitude()) / 2);

            if (calculatedZoom == NO_ZOOM) {
                // int pixels = Math.min(mapView.getWidth(), mapView.getHeight());
                double pixels = Math.sqrt((mapView.getWidth() * mapView.getWidth()) + (mapView.getHeight() * mapView.getHeight()));
                final double requiredMinimalGroundResolutionInMetersPerPixel
                        = ((double) new GeoPoint(min.getLatitude(), min.getLongitude()).distanceToAsDouble(max)) / pixels;
                calculatedZoom = calculateZoom(center.getLatitude(), requiredMinimalGroundResolutionInMetersPerPixel, getMaximumZoomLevel(tileProvider), tileProvider.getMinimumZoomLevel());
            }
        }
        if (calculatedZoom != NO_ZOOM) {
            controller.setZoom((double) calculatedZoom);
        }

        if (center != null) {
            controller.setCenter(center);
        }

        return calculatedZoom;
    }

    public static double getMaximumZoomLevel(MapView map) {
        final double maxZoomLevel = map.getMaxZoomLevel();
        return (maxZoomLevel > MAX_ZOOM_LEVEL) ? MAX_ZOOM_LEVEL : maxZoomLevel;
    }

    public static int getMaximumZoomLevel(MapTileProviderBase tileProvider) {
        final int maxZoomLevel = tileProvider.getMaximumZoomLevel();
        return (maxZoomLevel > MAX_ZOOM_LEVEL) ? MAX_ZOOM_LEVEL : maxZoomLevel;
    }

    private static int calculateZoom(double latitude, double requiredMinimalGroundResolutionInMetersPerPixel,
                                        int maximumZoomLevel, int minimumZoomLevel) {
        double groundResolution;
        for (int zoom = Math.min(maximumZoomLevel, 15); zoom >= minimumZoomLevel; zoom--) {
            groundResolution = TileSystemFix_978.GroundResolution(latitude, zoom);
            if (groundResolution > requiredMinimalGroundResolutionInMetersPerPixel)
                return zoom;
        }

        return NO_ZOOM;
    }

    private static class TileSystemFix_978 {
        protected static long mTileSize = 256;
        private static final double EarthRadius = 6378137;
        private static final double MinLatitude = -85.05112878;
        private static final double MaxLatitude = 85.05112878;

        public static double GroundResolution(double latitude, final int levelOfDetail) {
            latitude = Clip(latitude, MinLatitude, MaxLatitude);
            return Math.cos(latitude * Math.PI / 180) * 2 * Math.PI * EarthRadius
                    / MapSize(levelOfDetail);
        }

        public static long MapSize(final int levelOfDetail) {
            return mTileSize << (levelOfDetail < 29 ? levelOfDetail
                    : 29);
        }

        private static double Clip(final double n, final double minValue, final double maxValue) {
            return Math.min(Math.max(n, minValue), maxValue);
        }

    }

    /**
     * Creates view on top of mapView at the specified geoPosition + offset where pop-windows can be attached to.
     *
     * Every view opend by {@link #openMapPopupView(MapView, int, IGeoPoint)} must be
     * closed by {@link #closeMapPopupView(MapView, View)}.
     * This code was inspired by org.osmdroid.bonuspack.overlays.InfoWindow.
     * @param layoutResId layout resource id or 0 if for a 1x1 pix view that can host a popupmenu.
     * @param geoPosition to place the window on the map
     */
    @NonNull
    public static View openMapPopupView(MapView mapView, int layoutResId, IGeoPoint geoPosition) {
        return openMapPopupView(mapView, layoutResId, geoPosition, MapView.LayoutParams.CENTER, 0, 0);
    }

    /**
     * Creates view on top of mapView at the specified geoPosition + offset where pop-windows can be attached to.
     *
     * Every view opend by {@link #openMapPopupView(MapView, int, IGeoPoint)} must be
     * closed by {@link #closeMapPopupView(MapView, View)}.
     * This code was inspired by org.osmdroid.bonuspack.overlays.InfoWindow.
     * @param layoutResId layout resource id or 0 if for a 1x1 pix view that can host a popupmenu.
     * @param geoPosition to place the window on the map
     * @param layoutposition one of the {@link MapView.LayoutParams} constants
     * @param offsetX (&offsetY) the offset of the view to the position, in pixels.
     */
    @NonNull
    public static View openMapPopupView(MapView mapView, int layoutResId, IGeoPoint geoPosition, int layoutposition, int offsetX, int offsetY) {
        View popupView = null;
        int layoutSize = MapView.LayoutParams.WRAP_CONTENT;
        if (layoutResId == 0) {
            layoutSize = 1;
            popupView = new View(mapView.getContext());
        } else {
            ViewGroup parent=(ViewGroup)mapView.getParent();
            Context context = mapView.getContext();
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            popupView = inflater.inflate(layoutResId, parent, false);

        }
        popupView.setVisibility(View.VISIBLE);

        MapView.LayoutParams lp = new MapView.LayoutParams(
                layoutSize,
                layoutSize,
                geoPosition, layoutposition,
                offsetX, offsetY);

        mapView.addView(popupView, lp);
        return popupView;
    }

    /**
     * Every view opend by {@link #openMapPopupView(MapView, int, IGeoPoint)} must be
     * closed by {@link #closeMapPopupView(MapView, View)}.
     *
     * @param mapView owner of the popup.
     * @param popupView popup to be closed.
     */
    public static void closeMapPopupView(MapView mapView, View popupView) {
        if ((mapView != null) && (popupView != null)) {
            mapView.removeView(popupView);
            ((ViewGroup) mapView.getParent()).removeView(popupView);
        }
    }
}

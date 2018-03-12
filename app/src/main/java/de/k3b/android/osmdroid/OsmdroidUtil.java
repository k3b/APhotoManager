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

import microsoft.mappoint.TileSystem;

/**
 * Helper for OsmDroid lib.
 *
 * Created by k3b on 16.03.2015.
 */
public class OsmdroidUtil {

    public static final int NO_ZOOM = -1; // GeoPointDto.NO_ZOOM;

    /**
     * Similar to MapView.zoomToBoundingBox that seems to be to inexact.
     * @param zoom if NO_ZOOM (-1) zoom is calculated from min and max
     */
    public static void zoomTo(MapView mapView, int zoom, IGeoPoint min, IGeoPoint max) {
        int calculatedZoom = zoom;
        MapTileProviderBase tileProvider = mapView.getTileProvider();
        IMapController controller = mapView.getController();
        IGeoPoint center = min;

        if (max != null) {
            center = new GeoPoint((max.getLatitudeE6() + min.getLatitudeE6()) / 2, (max.getLongitudeE6() + min.getLongitudeE6()) / 2);

            if (calculatedZoom == NO_ZOOM) {
                // int pixels = Math.min(mapView.getWidth(), mapView.getHeight());
                double pixels = Math.sqrt((mapView.getWidth() * mapView.getWidth()) + (mapView.getHeight() * mapView.getHeight()));
                final double requiredMinimalGroundResolutionInMetersPerPixel
                        = ((double) new GeoPoint(min.getLatitude(), min.getLongitude()).distanceToAsDouble(max)) / pixels;
                calculatedZoom = calculateZoom(center.getLatitude(), requiredMinimalGroundResolutionInMetersPerPixel, tileProvider.getMaximumZoomLevel(), tileProvider.getMinimumZoomLevel());
            }
        }
        if (calculatedZoom != NO_ZOOM) {
            controller.setZoom(calculatedZoom);
        }

        if (center != null) {
            controller.setCenter(center);
        }
/*
            if (logger.isDebugEnabled()) {
                logger.debug("DelayedSetCenterZoom.execute({}: ({}) .. ({}),z={}) => ({}), z={} => {}",
                        debugContext,
                        min, max, mZoomLevel, center, zoom, getStatusForDebug());
            }
*/
    }

    private static int calculateZoom(double latitude, double requiredMinimalGroundResolutionInMetersPerPixel, int maximumZoomLevel, int minimumZoomLevel) {
        for (int zoom = maximumZoomLevel; zoom >= minimumZoomLevel; zoom--) {
            if (TileSystem.GroundResolution(latitude, zoom) > requiredMinimalGroundResolutionInMetersPerPixel)
                return zoom;
        }

        return NO_ZOOM;
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

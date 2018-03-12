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
 
package de.k3b.android.androFotoFinder.locationmap;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.osmdroid.IconOverlay;
import de.k3b.android.util.ResourceUtils;
import de.k3b.io.collections.SelectedItems;
import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.api.IGeoPointInfo;
import de.k3b.io.GalleryFilterParameter;
import de.k3b.io.GeoRectangle;

/**
 * LocationMapFragment working as Picker. Current result is the red marker.
 *
 * Created by k3b on 30.08.2015.
 */
public class PickerLocationMapFragment extends LocationMapFragment {
    private static final String STATE_LAST_SELECTED_POINT = "last_geo_pick";

    private final String mDebugPrefix;
    private boolean mUsePicker;

    public PickerLocationMapFragment() {
        mDebugPrefix = "PickerLocationMapFragment ";
        STATE_LAST_VIEWPORT = "LAST_PICKER_VIEWPORT";
    }
    /**
     * non tap-able marker that moves to last tap position
     */
    private class CurrentSelectionMarker extends IconOverlay {
        public CurrentSelectionMarker(IGeoPoint position, Drawable icon) {
            super(position, icon);
        }

        /**
         * remember last tap position in this marker
         */
        @Override
        public boolean onSingleTapConfirmed(final MotionEvent e, final MapView mapView) {
            if (isEnabled()) {
                mMarkerId = NO_MARKER_ID;
                moveTo(e, mapView);
                updateMarker(null, NO_MARKER_ID, getPosition(), null);
                hideImage();
            }
            return super.onSingleTapConfirmed(e, mapView);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Intent intent = this.getActivity().getIntent();
        String action = (intent != null) ? intent.getAction() : null;
        mUsePicker = ((action != null) && ((Intent.ACTION_PICK.equals(action)) || (Intent.ACTION_GET_CONTENT.equals(action))));

        View result = super.onCreateView(inflater, container, savedInstanceState);

        if (mUsePicker) {
            defineButtons(result);
        }
        return result;
    }

    protected IconOverlay createSelectedItemOverlay() {
        if (mUsePicker) {
            Drawable currrentSelectionIcon = ResourceUtils.getDrawable(getActivity(), R.drawable.marker_red);
            return new CurrentSelectionMarker(null, currrentSelectionIcon);
        } else {
            Drawable currrentSelectionIcon = ResourceUtils.getDrawable(getActivity(), R.drawable.marker_red);
            // fixed positon, not updated on pick
            return new IconOverlay(null, currrentSelectionIcon);
        }

    }
    @Override
    protected void definteOverlays(MapView mapView) {
        super.definteOverlays(mapView);

        /// TODO
        /*
        OverlayManager items = new OverlayManager(null);
        // form  intent and Intent.EXTRA_STREAM as array[uri]


        !!!!!!


        onLoadFinishedBlueMarker(items);
        */
    }

    /** get all important parameters for displaying the map */
    public void defineNavigation(GalleryFilterParameter rootFilter, IGeoPointInfo selectedItem,
                                 GeoRectangle rectangle, int zoomlevel,
                                 SelectedItems selectedItems, Uri additionalPointsContentUri) {
        IGeoPointInfo currentSelection = selectedItem;
        if ((currentSelection == null) && (getCurrentSelectionPosition() == null)) {
            // first call with no geo: take last use from config
            // save as value if picker is called again with no geo-coordinate
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String lastValue = sharedPref.getString(STATE_LAST_SELECTED_POINT, null);
            currentSelection = (lastValue == null) ? null : mGeoUriEngine.fromUri(lastValue);
        }

        super.defineNavigation(rootFilter, rectangle, zoomlevel, selectedItems, additionalPointsContentUri);
        if (currentSelection != null) {
            updateMarker(null, NO_MARKER_ID, new GeoPoint(currentSelection.getLatitude(), currentSelection.getLongitude()), null);
        }
    }

    protected IGeoPoint getCurrentSelectionPosition() {
        if (this.mCurrrentSelectionRedMarker == null) return null;
        IGeoPoint pos = this.mCurrrentSelectionRedMarker.getPosition();
        if ((pos == null) || GeoPointDto.isEmpty(pos.getLatitude(), pos.getLongitude())) return null;
        return pos;
    }

    @Override
    protected void onOk() {
        IGeoPoint result = null;

        Activity activity = getActivity();
        if (mMarkerId != NO_MARKER_ID) {
            result = FotoSql.execGetPosition(activity, null, mMarkerId);
        }

        if (result == null) {
            result = getCurrentSelectionPosition();
        }

        if (result != null) {
            double currentZoomLevel = this.mMapView.getZoomLevelDouble();
            String uriCurrentPoint = mGeoUriEngine.toUriString(
                    new GeoPointDto(result.getLatitude(), result.getLongitude(), (int) currentZoomLevel));

            // save as value if picker is called again with no geo-coordinate
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            SharedPreferences.Editor edit = sharedPref.edit();

            edit.putString(STATE_LAST_SELECTED_POINT, uriCurrentPoint);

            edit.apply();

            if (Global.debugEnabled) {
                Log.i(Global.LOG_CONTEXT, mDebugPrefix + "onOk: " + uriCurrentPoint);
            }
            Intent resultIntent = new Intent(activity.getIntent());
            resultIntent.setData(Uri.parse(uriCurrentPoint));
            activity.setResult(1, resultIntent);
            activity.finish();
        }
    }

    @Override
    protected void onCancel() {
        this.getActivity().finish();
    }

    // warum ist roter marker nicht sichtbar wenn auf gruenen marker gecklickt wurde ???
}

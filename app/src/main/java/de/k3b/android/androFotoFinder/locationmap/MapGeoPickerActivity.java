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

package de.k3b.android.androFotoFinder.locationmap;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import de.k3b.android.androFotoFinder.Common;
import de.k3b.android.androFotoFinder.GalleryFilterActivity;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.osmdroid.ZoomUtil;
import de.k3b.android.widget.AboutDialogPreference;
import de.k3b.database.SelectedItems;
import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.api.IGeoPointInfo;
import de.k3b.geo.io.GeoUri;
import de.k3b.io.GalleryFilterParameter;
import de.k3b.io.GeoRectangle;

public class MapGeoPickerActivity extends Activity implements Common {
    private static final String debugPrefix = "GalM-";
    private static final String STATE_Filter = "filterMap";
    private static final String STATE_LAST_GEO = "geoLastView";

    private PickerLocationMapFragment mMap;

    /** true: if activity started without special intent-parameters, the last mFilter is saved/loaded for next use */
    private boolean mSaveLastUsedFilterToSharedPrefs = true;
    private boolean mSaveLastUsedGeoToSharedPrefs = true;

    private GalleryFilterParameter mFilter;
    private GeoUri mGeoUriParser = new GeoUri(GeoUri.OPT_PARSE_INFER_MISSING);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Intent intent = this.getIntent();

        GeoPointDto geoPointFromIntent = getGeoPointDtoFromIntent(intent);
        // no geo: from intent: use last used value
        mSaveLastUsedGeoToSharedPrefs = (geoPointFromIntent == null);

        String lastGeoUri = sharedPref.getString(STATE_LAST_GEO, "geo:53,8?z=6");
        IGeoPointInfo lastGeo = mGeoUriParser.fromUri(lastGeoUri);
        if ((geoPointFromIntent != null) && (lastGeo != null)) {
            // apply default values if part is missing
            if (Double.isNaN(geoPointFromIntent.getLongitude())) geoPointFromIntent.setLongitude(lastGeo.getLongitude());
            if (Double.isNaN(geoPointFromIntent.getLatitude())) geoPointFromIntent.setLatitude(lastGeo.getLatitude());
            if (geoPointFromIntent.getZoomMin() == IGeoPointInfo.NO_ZOOM) geoPointFromIntent.setZoomMin(lastGeo.getZoomMin());
        }

        IGeoPointInfo initalZoom = (mSaveLastUsedGeoToSharedPrefs) ? lastGeo : geoPointFromIntent;

        String extraTitle = intent.getStringExtra(EXTRA_TITLE);
        if (extraTitle == null && (geoPointFromIntent == null)) {
            extraTitle = getString(R.string.app_map_name);
        }

        if (extraTitle == null) {
            this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        }

        setContentView(R.layout.activity_map_geo_picker);

        if (extraTitle != null) {
            this.setTitle(extraTitle);
        } else {
            setNoTitle();
        }

        mMap = (PickerLocationMapFragment) getFragmentManager().findFragmentById(R.id.fragment_map);
        mMap.STATE_LAST_VIEWPORT = "ignore"; // do not use last viewport in settings

        GeoRectangle rectangle = null;
        int zoom = ZoomUtil.NO_ZOOM;
        if ((savedInstanceState == null) && (initalZoom != null)) {
            rectangle = new GeoRectangle();
            zoom = initalZoom.getZoomMin();
            rectangle.setLogituedMin(initalZoom.getLongitude()).setLatitudeMin(initalZoom.getLatitude());
            rectangle.setLogituedMax(initalZoom.getLongitude()).setLatitudeMax(initalZoom.getLatitude());
        } // else (savedInstanceState != null) restore after rotation. fragment takes care of restoring map pos

        String selectedItemsString = intent.getStringExtra(EXTRA_SELECTED_ITEMS);
        SelectedItems selectedItems = (selectedItemsString != null) ? new SelectedItems().parse(selectedItemsString) : null;

        String filter = null;
        // for debugging: where does the filter come from
        String dbgFilter = null;
        if (intent != null) {
            filter = intent.getStringExtra(EXTRA_FILTER);
        }
        this.mSaveLastUsedFilterToSharedPrefs = (filter == null); // false if controlled via intent
        if (savedInstanceState != null) {
            filter = savedInstanceState.getString(STATE_Filter);
            if (filter != null) dbgFilter = "filter from savedInstanceState=" + filter;
        }

        if (this.mSaveLastUsedFilterToSharedPrefs) {
            filter = sharedPref.getString(STATE_Filter, null);
            if (filter != null) dbgFilter = "filter from sharedPref=" + filter;
        }

        this.mFilter = new GalleryFilterParameter();

        if (filter != null) {
            GalleryFilterParameter.parse(filter, this.mFilter);
        }

        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, debugPrefix + dbgFilter + " => " + this.mFilter);
        }

        mMap.defineNavigation(this.mFilter, geoPointFromIntent, rectangle, zoom, selectedItems);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_map_geo_picker, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        saveSettings(this);
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        saveSettings(this);

        if ((savedInstanceState != null) && (this.mFilter != null)) {
            savedInstanceState.putString(STATE_Filter, this.mFilter.toString());
        }
    }

    private void saveSettings(Context context) {
        if (mSaveLastUsedFilterToSharedPrefs || mSaveLastUsedGeoToSharedPrefs) {
            // save settings
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor edit = sharedPref.edit();

            if (mSaveLastUsedFilterToSharedPrefs && (mFilter != null)) {
                edit.putString(STATE_Filter, mFilter.toString());
            }

            if (mSaveLastUsedGeoToSharedPrefs) {
                String currentGeoUri = mMap.getCurrentGeoUri();
                edit.putString(STATE_LAST_GEO, currentGeoUri);
            }

            edit.commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.cmd_filter:
                openFilter();
                return true;
            case R.id.cmd_about:
                AboutDialogPreference.createAboutDialog(this).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    /**
     * Call back from sub-activities.<br/>
     * Process Change StartTime (longpress start), Select StopTime before stop
     * (longpress stop) or filter change for detailReport
     */
    @Override
    protected void onActivityResult(final int requestCode,
                                    final int resultCode, final Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        switch (requestCode) {
            case GalleryFilterActivity.resultID :
                onFilterChanged(GalleryFilterActivity.getFilter(intent));
                break;
        }
    }

    private void onFilterChanged(GalleryFilterParameter filter) {
        if (filter != null) {
            this.mFilter = filter;
            mMap.defineNavigation(this.mFilter, null, ZoomUtil.NO_ZOOM, null);
        }
    }

    private void openFilter() {
        GalleryFilterActivity.showActivity(this, this.mFilter, null);
    }

    private GeoPointDto getGeoPointDtoFromIntent(Intent intent) {
        final Uri uri = (intent != null) ? intent.getData() : null;
        String uriAsString = (uri != null) ? uri.toString() : null;
        GeoPointDto pointFromIntent = null;
        if (uriAsString != null) {
            Toast.makeText(this, getString(R.string.app_name) + ": received  " + uriAsString, Toast.LENGTH_LONG).show();

            pointFromIntent = (GeoPointDto) mGeoUriParser.fromUri(uriAsString, new GeoPointDto());
            if (GeoPointDto.isEmpty(pointFromIntent)) pointFromIntent = null;
        }
        return pointFromIntent;
    }

    private void setNoTitle() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }


}

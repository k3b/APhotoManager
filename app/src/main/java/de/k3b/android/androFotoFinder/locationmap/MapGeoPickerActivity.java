/*
 * Copyright (c) 2015-2018 by k3b.
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

import org.osmdroid.api.IGeoPoint;

import de.k3b.android.androFotoFinder.AffUtils;
import de.k3b.android.androFotoFinder.BookmarkController;
import de.k3b.android.androFotoFinder.Common;
import de.k3b.android.androFotoFinder.FotoGalleryActivity;
import de.k3b.android.androFotoFinder.GalleryFilterActivity;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.LockScreen;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.SettingsActivity;
import de.k3b.android.androFotoFinder.imagedetail.ImageDetailActivityViewPager;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.osmdroid.OsmdroidUtil;
import de.k3b.android.widget.AboutDialogPreference;
import de.k3b.android.widget.LocalizedActivity;
import de.k3b.database.QueryParameter;
import de.k3b.io.collections.SelectedFiles;
import de.k3b.io.collections.SelectedItems;
import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.api.IGeoPointInfo;
import de.k3b.geo.io.GeoUri;
import de.k3b.io.GalleryFilterParameter;
import de.k3b.io.GeoRectangle;
import de.k3b.io.IGeoRectangle;

public class MapGeoPickerActivity extends LocalizedActivity implements Common {
    private static final String mDebugPrefix = "GalM-";
    private static final String STATE_Filter = "filterMap";
    private static final String STATE_LAST_GEO = "geoLastView";

    private PickerLocationMapFragment mMap;

    /** true: if activity started without special intent-parameters, the last mFilter is saved/loaded for next use */
    private boolean mSaveLastUsedFilterToSharedPrefs = true;
    private boolean mSaveLastUsedGeoToSharedPrefs = true;

    private GalleryFilterParameter mFilter;
    private GeoUri mGeoUriParser = new GeoUri(GeoUri.OPT_PARSE_INFER_MISSING);

    private BookmarkController mBookmarkController = null;

    public static void showActivity(Activity context, SelectedFiles selectedItems, GalleryFilterParameter filter) {
        Uri initalUri = null;
        final Intent intent = new Intent().setClass(context,
                MapGeoPickerActivity.class);

        GalleryFilterParameter localFilter = new GalleryFilterParameter();

        if (filter != null) {
            localFilter.get(filter);
        }

        if (AffUtils.putSelectedFiles(intent, selectedItems)) {
            IGeoPoint initialPoint = FotoSql.execGetPosition(context, null, selectedItems.getId(0));
            if (initialPoint != null) {
                GeoUri PARSER = new GeoUri(GeoUri.OPT_PARSE_INFER_MISSING);

                initalUri = Uri.parse(PARSER.toUriString(new GeoPointDto(initialPoint.getLatitude(),initialPoint.getLongitude(), IGeoPointInfo.NO_ZOOM)));
                intent.setData(initalUri);
            }
        }

        localFilter.setNonGeoOnly(false);
        intent.putExtra(EXTRA_FILTER, localFilter.toString());

        intent.setAction(Intent.ACTION_VIEW);
        if (Global.debugEnabled) {
            Log.d(Global.LOG_CONTEXT, context.getClass().getSimpleName()
                    + " > MapGeoPickerActivity.showActivity@" + initalUri);
        }
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Intent intent = this.getIntent();
        if (Global.debugEnabled && (intent != null)){
            Log.d(Global.LOG_CONTEXT, mDebugPrefix + "onCreate " + intent.toUri(Intent.URI_INTENT_SCHEME));
        }

        mBookmarkController = new BookmarkController(this);
        mBookmarkController.loadState(intent,savedInstanceState);

        GeoPointDto geoPointFromIntent = getGeoPointDtoFromIntent(intent);
        // no geo: from intent: use last used value
        mSaveLastUsedGeoToSharedPrefs = (geoPointFromIntent == null);

        Uri additionalPointsContentUri = ((intent != null) && (geoPointFromIntent == null)) ? intent.getData() : null;

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
            extraTitle = getString(R.string.app_map_title);
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

        GeoRectangle rectangle = null;
        int zoom = OsmdroidUtil.NO_ZOOM;
        if ((savedInstanceState == null) && (initalZoom != null) && (additionalPointsContentUri == null)) {
            rectangle = new GeoRectangle();
            zoom = initalZoom.getZoomMin();
            rectangle.setLogituedMin(initalZoom.getLongitude()).setLatitudeMin(initalZoom.getLatitude());
            rectangle.setLogituedMax(initalZoom.getLongitude()).setLatitudeMax(initalZoom.getLatitude());
        } // else (savedInstanceState != null) restore after rotation. fragment takes care of restoring map pos

        SelectedItems selectedItems = AffUtils.getSelectedItems(intent);

        // TODO !!! #62 gpx/kml files: wie an LocatonMapFragment übergeben??
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
            Log.i(Global.LOG_CONTEXT, mDebugPrefix + dbgFilter + " => " + this.mFilter);
        }

        mMap.defineNavigation(this.mFilter, geoPointFromIntent, rectangle, zoom, selectedItems, additionalPointsContentUri);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(LockScreen.isLocked(this) ? R.menu.menu_map_context_locked :  R.menu.menu_map_geo_picker, menu);
        AboutDialogPreference.onPrepareOptionsMenu(this, menu);

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
        mBookmarkController.saveState(null, savedInstanceState);

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

            edit.apply();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (LockScreen.onOptionsItemSelected(this, item))
            return true;
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.cmd_filter:
                openFilter();
                return true;
            //cmd_lock
            case R.id.cmd_about:
                AboutDialogPreference.createAboutDialog(this).show();
                return true;
            case R.id.cmd_settings:
                SettingsActivity.show(this);
                return true;
			case R.id.cmd_photo:
				return showPhoto(mMap.getCurrentGeoRectangle());
			case R.id.cmd_gallery:
				return showGallery(mMap.getCurrentGeoRectangle());
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private boolean showPhoto(IGeoRectangle geoArea) {
        QueryParameter query = new QueryParameter();
        FotoSql.setSort(query, FotoSql.SORT_BY_DATE, false);
        FotoSql.addWhereFilterLatLon(query, geoArea);

        ImageDetailActivityViewPager.showActivity(this, null, 0, query, 0);
        return true;
    }

    private boolean showGallery(IGeoRectangle geoArea) {
        GalleryFilterParameter filter = new GalleryFilterParameter();
        filter.get(geoArea);
        FotoGalleryActivity.showActivity(this, filter, null, 0);
        return true;
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
                mBookmarkController.loadState(intent, null);
                onFilterChanged(GalleryFilterActivity.getFilter(intent));
                break;
            default:
                throw new IllegalStateException();
        }
    }

    private void onFilterChanged(GalleryFilterParameter filter) {
        if ((mMap != null) && (filter != null)) {
            this.mFilter = filter;
            mMap.defineNavigation(this.mFilter, null, OsmdroidUtil.NO_ZOOM, null, null);
        }
    }

    private void openFilter() {
        GalleryFilterActivity.showActivity(this, this.mFilter, null,
                mBookmarkController.getlastBookmarkFileName(), GalleryFilterActivity.resultID);
    }

    private GeoPointDto getGeoPointDtoFromIntent(Intent intent) {
        final Uri uri = (intent != null) ? intent.getData() : null;
        String uriAsString = (uri != null) ? uri.toString() : null;
        GeoPointDto pointFromIntent = null;
        if (uriAsString != null) {
            Toast.makeText(this, getString(R.string.app_name) + ": received  " + uriAsString, Toast.LENGTH_LONG).show();

            pointFromIntent = mGeoUriParser.fromUri(uriAsString, new GeoPointDto());
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

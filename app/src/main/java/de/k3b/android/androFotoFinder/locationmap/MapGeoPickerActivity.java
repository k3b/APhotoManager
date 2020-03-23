/*
 * Copyright (c) 2015-2020 by k3b.
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
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.views.MapView;

import de.k3b.android.androFotoFinder.AffUtils;
import de.k3b.android.androFotoFinder.Common;
import de.k3b.android.androFotoFinder.FotoGalleryActivity;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.LockScreen;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.SettingsActivity;
import de.k3b.android.androFotoFinder.imagedetail.ImageDetailActivityViewPager;
import de.k3b.android.androFotoFinder.imagedetail.ImageDetailMetaDialogBuilder;
import de.k3b.android.androFotoFinder.queries.AndroidAlbumUtils;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.tagDB.TagSql;
import de.k3b.android.osmdroid.OsmdroidUtil;
import de.k3b.android.util.IntentUtil;
import de.k3b.android.widget.AboutDialogPreference;
import de.k3b.android.widget.BaseQueryActivity;
import de.k3b.database.QueryParameter;
import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.api.IGeoPointInfo;
import de.k3b.geo.io.GeoUri;
import de.k3b.io.GalleryFilterParameter;
import de.k3b.io.GeoRectangle;
import de.k3b.io.IDirectory;
import de.k3b.io.IGeoRectangle;
import de.k3b.io.collections.SelectedFiles;
import de.k3b.io.collections.SelectedItems;

// BaseQueryActivity LocalizedActivity
public class MapGeoPickerActivity extends BaseQueryActivity implements Common {
    private static final String mDebugPrefix = "GalM-";
    private static final String STATE_Filter = "filterMap";
    private static final String STATE_LAST_GEO = "geoLastView";

    private PickerLocationMapFragment mMap;

    /** true: if activity started without special intent-parameters, the last mFilter is saved/loaded for next use */
    private boolean mSaveLastUsedFilterToSharedPrefs = true;
    private boolean mSaveLastUsedGeoToSharedPrefs = true;

    private GeoUri mGeoUriParser = new GeoUri(GeoUri.OPT_PARSE_INFER_MISSING);

    // lockscreen support
    private boolean locked = false; // if != Global.locked : must update menu
    private boolean mMustReplaceMenue = false;

    public static void showActivity(String debugContext, Activity context, SelectedFiles selectedItems,
                                    QueryParameter query, IGeoRectangle zoomTo, int requestCode) {
        Uri initalUri = null;
        Intent intent = new Intent(context, MapGeoPickerActivity.class);

        AndroidAlbumUtils.saveFilterAndQuery(context, null, intent, null, null, query);

        if (zoomTo != null) {
            intent.putExtra(EXTRA_ZOOM_TO, new GeoRectangle().get(zoomTo).toString());
        }

        if (AffUtils.putSelectedFiles(intent, selectedItems)) {
            IGeoPoint initialPoint = FotoSql.execGetPosition(null,
                    null, selectedItems.getId(0), context, mDebugPrefix, "showActivity");
            if (initialPoint != null) {
                GeoUri PARSER = new GeoUri(GeoUri.OPT_PARSE_INFER_MISSING);

                initalUri = Uri.parse(PARSER.toUriString(new GeoPointDto(initialPoint.getLatitude(),initialPoint.getLongitude(), IGeoPointInfo.NO_ZOOM)));
                intent.setData(initalUri);
            }
        }

        intent.setAction(Intent.ACTION_VIEW);
        if (Global.debugEnabled) {
            Log.d(Global.LOG_CONTEXT, context.getClass().getSimpleName()
                    + " > MapGeoPickerActivity.showActivity@" + initalUri);
        }
        IntentUtil.startActivity(debugContext, context, requestCode, intent);
    }

    @Override
    protected void onCreateEx(Bundle savedInstanceState) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Intent intent = this.getIntent();
        if (Global.debugEnabled && (intent != null)){
            Log.d(Global.LOG_CONTEXT, mDebugPrefix + "onCreate " + intent.toUri(Intent.URI_INTENT_SCHEME));
        }

        final GeoPointDto geoPointFromIntent = getGeoPointDtoFromIntent(intent);
        // no geo: from intent: use last used value
        mSaveLastUsedGeoToSharedPrefs = (geoPointFromIntent == null);

        final Uri uriFromIntent = intent.getData();
        final Uri additionalPointsContentUri = ((intent != null) && (geoPointFromIntent == null)) ? uriFromIntent : null;

        String lastGeoUri = sharedPref.getString(STATE_LAST_GEO, "geo:53,8?z=6");
        IGeoPointInfo lastGeo = mGeoUriParser.fromUri(lastGeoUri);
        if ((geoPointFromIntent != null) && (lastGeo != null)) {
            // apply default values if part is missing
            if (Double.isNaN(geoPointFromIntent.getLongitude())) geoPointFromIntent.setLongitude(lastGeo.getLongitude());
            if (Double.isNaN(geoPointFromIntent.getLatitude())) geoPointFromIntent.setLatitude(lastGeo.getLatitude());
            if (geoPointFromIntent.getZoomMin() == IGeoPointInfo.NO_ZOOM) geoPointFromIntent.setZoomMin(lastGeo.getZoomMin());
        }

        IGeoPointInfo initalZoom = (mSaveLastUsedGeoToSharedPrefs) ? lastGeo : geoPointFromIntent;

        setContentView(R.layout.activity_map_geo_picker);

        mMap = (PickerLocationMapFragment) getFragmentManager().findFragmentById(R.id.fragment_map);

        GeoRectangle zoomToRectangle = null;

        int zoomToZoomlevel = OsmdroidUtil.NO_ZOOM;

        if (savedInstanceState == null) {
            String zoomToArea = intent.getStringExtra(Common.EXTRA_ZOOM_TO);
            if (zoomToArea != null) {
                zoomToRectangle = new GalleryFilterParameter();
                GalleryFilterParameter.parse(zoomToArea, (GalleryFilterParameter) zoomToRectangle);
            } else if ((initalZoom != null) && (additionalPointsContentUri == null)) {
                zoomToRectangle = new GeoRectangle();
                zoomToZoomlevel = initalZoom.getZoomMin();
                zoomToRectangle.setLogituedMin(initalZoom.getLongitude()).setLatitudeMin(initalZoom.getLatitude());
                zoomToRectangle.setLogituedMax(initalZoom.getLongitude()).setLatitudeMax(initalZoom.getLatitude());
            }
        } // else (savedInstanceState != null) restore after rotation. fragment takes care of restoring map pos

        final SelectedItems selectedItems = AffUtils.getSelectedItems(intent);

        // TODO !!! #62 gpx/kml files: wie an LocatonMapFragment übergeben??
        String filter = null;
        // for debugging: where does the filter come from
        String dbgFilter = null;

        onCreateData(savedInstanceState);

        {
            // bugfix: first defineNavigation will not work until map is created completely
            // so wait until then
            // note: delayed params must be final
            final GeoRectangle _zoomToRectangle = zoomToRectangle;
            final int _zoomToZoomlevel = zoomToZoomlevel;
            final boolean _zoom2fit = false;
            mMap.mMapView.addOnFirstLayoutListener(new MapView.OnFirstLayoutListener() {
                @Override
                public void onFirstLayout(View v, int left, int top, int right, int bottom) {
                    mMap.defineNavigation(mGalleryQueryParameter.calculateEffectiveGalleryContentQuery(),
                            null, geoPointFromIntent,
                            _zoomToRectangle, _zoomToZoomlevel,
                            selectedItems, additionalPointsContentUri, _zoom2fit);
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // if lock mode has changed redefine menu
        final boolean locked = LockScreen.isLocked(this);
        if (this.locked != locked) {
            this.locked = locked;
            mMustReplaceMenue = true;
            invalidateOptionsMenu();
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        mMustReplaceMenue = true;
        fixOptionsMenu(menu);
        return true;
    }

        @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // if lock mode has changed redefine menu
        fixOptionsMenu(menu);
        return true;
    }

    private void fixOptionsMenu(Menu menu) {
        final boolean locked = LockScreen.isLocked(this);
        if (mMustReplaceMenue || (locked != this.locked)) {
            mMustReplaceMenue = false;
            this.locked = locked;
            menu.clear();
            MenuInflater inflater = getMenuInflater();

            if (locked) {
                inflater.inflate(R.menu.menu_locked, menu);
                LockScreen.removeDangerousCommandsFromMenu(menu);
            } else {
                inflater.inflate(R.menu.menu_map_geo_picker, menu);

            }
            AboutDialogPreference.onPrepareOptionsMenu(this, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (LockScreen.onOptionsItemSelected(this, menuItem)) {
            return true;
        }
        // Handle presses on the action bar items
        switch (menuItem.getItemId()) {
            //cmd_lock
            case R.id.cmd_about:
                AboutDialogPreference.createAboutDialog(this).show();
                return true;
            case R.id.cmd_settings:
                SettingsActivity.showActivity(this);
                return true;
			case R.id.cmd_photo:
                return showPhoto(menuItem, mMap.getCurrentGeoRectangle());
			case R.id.cmd_gallery:
                return showGallery(menuItem, mMap.getCurrentGeoRectangle());
            case R.id.action_details:
                cmdShowDetails();
                return true;
            case R.id.cmd_show_geo_as: {
                String uri = mMap.getCurrentGeoUri();
                IntentUtil.cmdStartIntent("show_geo_as", this,
                        null, uri, null, Intent.ACTION_VIEW,
                        R.string.geo_show_as_menu_title, R.string.geo_picker_err_not_found, 0);

                return true;
            }
            default:
                return onOptionsItemSelected(menuItem, AffUtils.getSelectedItems(getIntent()));
        }

    }

    private boolean showPhoto(MenuItem menuItem, IGeoRectangle geoArea) {
        QueryParameter query = new QueryParameter();
        FotoSql.setSort(query, FotoSql.SORT_BY_DATE, false);
        FotoSql.addWhereFilterLatLon(query, geoArea);

        ImageDetailActivityViewPager.showActivity(" menu " + menuItem.getTitle() + "[19]-" + geoArea, this, null, 0, query, 0);
        return true;
    }

    private boolean showGallery(MenuItem menuItem, IGeoRectangle geoArea) {
        QueryParameter query = getAsMergedQuery(geoArea);

        FotoGalleryActivity.showActivity(" menu " + menuItem.getTitle() + "[20]-" + geoArea, this, query, 0);
        return true;
    }

    private QueryParameter getAsMergedQuery(IGeoRectangle geoArea) {
        GalleryFilterParameter filter = new GalleryFilterParameter();
        filter.get(geoArea);
        return TagSql.filter2NewQuery(filter);
    }

    private void cmdShowDetails() {
        final QueryParameter asMergedQuery = mMap.getCurrentAreaQuery();

        CharSequence subQuerymTitle = getValueAsTitle(true);
        final Dialog dlg = ImageDetailMetaDialogBuilder.createImageDetailDialog(
                this,
                getTitle().toString(),
                asMergedQuery.toSqlString(),
                TagSql.getStatisticsMessage(this, R.string.show_photo,
                        asMergedQuery),
                mMap.getCurrentGeoRectangle() + " ==> " + mMap.getCurrentGeoUri(),
                subQuerymTitle
        );
        dlg.show();
        setAutoClose(null, dlg, null);
    }

    @Override
    protected void reloadGui(String why) {
        if (mMap != null) {
            QueryParameter query = this.mGalleryQueryParameter.calculateEffectiveGalleryContentQuery();
            mMap.defineNavigation(query,
                    null,null, IGeoPointInfo.NO_ZOOM, null, null, false);
        }

    }

    @Override
    protected void defineDirectoryNavigation(IDirectory directoryRoot) {
    }

    /** allows childclass to have their own sharedPreference names */
    @Override
    protected String fixSharedPrefSuffix(String statSuffix) {
        return super.fixSharedPrefSuffix(statSuffix) + "-map";
    }

    private GeoPointDto getGeoPointDtoFromIntent(Intent intent) {
        String uriAsString =  (intent != null) ? intent.getDataString() : null;
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

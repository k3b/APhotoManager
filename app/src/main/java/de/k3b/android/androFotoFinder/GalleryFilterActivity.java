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
 
package de.k3b.android.androFotoFinder;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

// import com.squareup.leakcanary.RefWatcher;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import de.k3b.android.androFotoFinder.directory.DirectoryLoaderTask;
import de.k3b.android.androFotoFinder.directory.DirectoryPickerFragment;
import de.k3b.android.androFotoFinder.locationmap.LocationMapFragment;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.osmdroid.OsmdroidUtil;
import de.k3b.android.widget.AboutDialogPreference;
import de.k3b.android.widget.HistoryEditText;
import de.k3b.android.widget.LocalizedActivity;
import de.k3b.database.QueryParameter;
import de.k3b.io.DirectoryFormatter;
import de.k3b.io.GalleryFilterParameter;
import de.k3b.io.IDirectory;
import de.k3b.io.IGalleryFilter;
import de.k3b.io.IGeoRectangle;

/**
 * Defines a gui for global foto filter: only fotos from certain filepath, date and/or lat/lon will be visible.
 */
public class GalleryFilterActivity extends LocalizedActivity
        implements Common, DirectoryPickerFragment.OnDirectoryInteractionListener,
        LocationMapFragment.OnDirectoryInteractionListener {
    private static final String mDebugPrefix = "GalF-";

    public static final int resultID = 522;
    private static final String DLG_NAVIGATOR_TAG = "GalleryFilterActivity";
    private static final String SETTINGS_KEY = "GalleryFilterActivity-";
    private static final String FILTER_VALUE = "CURRENT_FILTER";
    private static final String WILDCARD = "%";
    private static QueryParameter mRootQuery;

    private GalleryFilterParameter mFilter = new GalleryFilterParameter();

    private FilterValue mFilterValue = null;
    private HistoryEditText mHistory;
    private BookmarkController bookmarkController = null;

    public static void showActivity(Activity context, IGalleryFilter filter, QueryParameter rootQuery) {
        mRootQuery = rootQuery;
        if (Global.debugEnabled) {
            Log.d(Global.LOG_CONTEXT, context.getClass().getSimpleName()
                    + " > GalleryFilterActivity.showActivity");
        }

        final Intent intent = new Intent().setClass(context,
                GalleryFilterActivity.class);

        if (!GalleryFilterParameter.isEmpty(filter)) {
            intent.putExtra(EXTRA_FILTER, filter.toString());
        }

        context.startActivityForResult(intent, resultID);
    }

    public static GalleryFilterParameter getFilter(Intent intent) {
        if (intent == null) return null;
        String filter = intent.getStringExtra(EXTRA_FILTER);
        if (filter == null) return null;
        return GalleryFilterParameter.parse(filter, new GalleryFilterParameter());
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        fromGui(mFilter);
        savedInstanceState.putString(FILTER_VALUE, mFilter.toString());
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Global.debugMemory(mDebugPrefix, "onCreate");
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();
        if (Global.debugEnabled && (intent != null)){
            Log.d(Global.LOG_CONTEXT, mDebugPrefix + "onCreate " + intent.toUri(Intent.URI_INTENT_SCHEME));
        }
        setContentView(R.layout.activity_gallery_filter);
        this.mFilterValue = new FilterValue();
        onCreateButtos();

        GalleryFilterParameter filter = (savedInstanceState == null)
                ? getFilter(this.getIntent())
                : GalleryFilterParameter.parse(savedInstanceState.getString(FILTER_VALUE, ""),  new GalleryFilterParameter()) ;

        if (filter != null) {
            mFilter = filter;
            toGui(mFilter);
            mFilterValue.showLatLon(filter.isNonGeoOnly());
        }

        bookmarkController = new BookmarkController(this);
    }

    private void onCreateButtos() {
        Button cmd = (Button) findViewById(R.id.cmd_path);
        cmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDirectoryPicker(FotoSql.queryGroupByDir);
            }
        });
        cmd = (Button) findViewById(R.id.cmd_date);
        cmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDirectoryPicker(FotoSql.queryGroupByDate);
            }
        });
        cmd = (Button) findViewById(R.id.cmd_select_lat_lon);
        cmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//              showDirectoryPicker(FotoSql.queryGroupByPlace);
                showLatLonPicker();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_gallery_filter, menu);
        AboutDialogPreference.onPrepareOptionsMenu(this, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.cmd_cancel:
                finish();
                return true;
            case R.id.cmd_clear:
                clearFilter();
                return true;
            case R.id.cmd_ok:
                onOk();
                return true;
            case R.id.cmd_about:
                AboutDialogPreference.createAboutDialog(this).show();
                return true;
            case R.id.cmd_settings:
                SettingsActivity.show(this);
                return true;

            case R.id.action_save_as:
                bookmarkController.onSaveAsQuestion("", getAsQuery());
                return true;
            case R.id.action_load_from:
                bookmarkController.onLoadFromQuestion(new BookmarkController.IQueryConsumer() {
                    @Override
                    public void setQuery(QueryParameter newQuery) {
                        IGalleryFilter filter = FotoSql.getWhereFilter(newQuery, false);
                        toGui(filter);
                    }
                }, getAsQuery());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private QueryParameter getAsQuery() {
        IGalleryFilter filter = new GalleryFilterParameter();
        fromGui(filter);
        QueryParameter query = new QueryParameter(mRootQuery);
        FotoSql.setWhereFilter(query, filter, true);
        return query;
    }

    @Override
    protected void onPause () {
        Global.debugMemory(mDebugPrefix, "onPause");
        saveLastFilter();
        super.onPause();
    }

    @Override
    protected void onResume () {
        Global.debugMemory(mDebugPrefix, "onResume");
        loadLastFilter();
        super.onResume();
    }

    private void loadLastFilter() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        loadLastFilter(sharedPref, FotoSql.QUERY_TYPE_GROUP_ALBUM);
        loadLastFilter(sharedPref, FotoSql.QUERY_TYPE_GROUP_DATE);
        loadLastFilter(sharedPref, FotoSql.QUERY_TYPE_GROUP_PLACE);
    }

    private void loadLastFilter(SharedPreferences sharedPref, int queryTypeID) {
        getOrCreateDirInfo(queryTypeID).currentPath = sharedPref.getString(SETTINGS_KEY + queryTypeID, null);
    }

    private void saveLastFilter() {
        if (dirInfos != null)
        {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor edit = sharedPref.edit();
            
            for(Integer id : dirInfos.keySet()) {
                DirInfo dir = dirInfos.get(id);
                if ((dir != null) && (dir.currentPath != null) && (dir.currentPath.length() > 0)) {
                    edit.putString(SETTINGS_KEY + id, dir.currentPath);
                }
            }
            edit.apply();
        }
    }

    @Override
    protected void onDestroy() {
        Global.debugMemory(mDebugPrefix, "onDestroy start");
        super.onDestroy();

        if (dirInfos != null)
        {
            for(Integer id : dirInfos.keySet()) {
                DirInfo dir = dirInfos.get(id);
                if (dir.directoryRoot != null) {
                    dir.directoryRoot.destroy();
                }
            }
            dirInfos = null;
        }

        System.gc();
        Global.debugMemory(mDebugPrefix, "onDestroy end");
        // RefWatcher refWatcher = AndroFotoFinderApp.getRefWatcher(this);
        // refWatcher.watch(this);
    }

    /** gui content seen as IGalleryFilter */
    private class FilterValue implements IGalleryFilter {
        final private java.text.DateFormat isoDateformatter = new SimpleDateFormat(
                "yyyy-MM-dd", Locale.US);

        private EditText mPath;

        private EditText mDateFrom;
        private EditText mDateTo;
        private EditText mLongitudeFrom;
        private EditText mLongitudeTo;
        private EditText mLatitudeTo;
        private EditText mLatitudeFrom;
        private CheckBox mWithNoGeoInfo;

        FilterValue() {
            this.mPath = (EditText) findViewById(R.id.edit_path);
            this.mDateFrom = (EditText) findViewById(R.id.edit_date_from);
            this.mDateTo = (EditText) findViewById(R.id.edit_date_to);
            this.mLatitudeFrom = (EditText) findViewById(R.id.edit_latitude_from);
            this.mLatitudeTo = (EditText) findViewById(R.id.edit_latitude_to);
            this.mLongitudeFrom = (EditText) findViewById(R.id.edit_longitude_from);
            this.mLongitudeTo = (EditText) findViewById(R.id.edit_longitude_to);
            this.mWithNoGeoInfo = (CheckBox) findViewById(R.id.chk_with_no_geo);

            mWithNoGeoInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showLatLon(mWithNoGeoInfo.isChecked());
                }

            });

            mHistory = new HistoryEditText(GalleryFilterActivity.this, new int[] {
                    R.id.cmd_path_history, R.id.cmd_date_from_history, R.id.cmd_date_to_history,
                    R.id.cmd_lat_from_history, R.id.cmd_lat_to_history, R.id.cmd_lon_from_history, R.id.cmd_lon_to_history} ,
                    mPath ,mDateFrom ,mDateTo, mLatitudeFrom, mLatitudeTo, mLongitudeFrom, mLongitudeTo);

        }

       protected void showLatLon(boolean noGeoInfo) {
            show(noGeoInfo, R.id.cmd_select_lat_lon, R.id.lbl_latitude, R.id.cmd_lat_from_history, R.id.edit_latitude_from,
                    R.id.cmd_lat_to_history, R.id.edit_latitude_to, R.id.lbl_longitude, R.id.cmd_lon_from_history,
                    R.id.edit_longitude_from,R.id.cmd_lon_to_history, R.id.edit_longitude_to);
        }

        private void show(boolean checked, int... ids) {
            for(int id:ids)
                findViewById(id).setVisibility((!checked) ? View.VISIBLE : View.INVISIBLE );
        }

        /** minimum latitude, in degrees north. -90..+90 */
        @Override
        public double getLatitudeMin() {
            return convertLL(mLatitudeFrom.getText().toString());
        }

        /** maximum latitude, in degrees north. -90..+90 */
        @Override
        public double getLatitudeMax() {
            return convertLL(mLatitudeTo.getText().toString());
        }

        /** minimum longitude, in degrees east. -180..+180 */
        @Override
        public double getLogituedMin() {
            return convertLL(mLongitudeFrom.getText().toString());
        }

        /** maximum longitude, in degrees east. -180..+180 */
        @Override
        public double getLogituedMax() {
            return convertLL(mLongitudeTo.getText().toString());
        }

        @Override
        public String getPath() {
            // smart filter path edit:  if the field does not contain a path element "/" then surround
            // the value with sql wildcard "%"
            String result = mPath.getText().toString().trim().replace('\\','/');
            if ((result.length() > 0) && !result.contains("/") && !result.contains(WILDCARD)) result = WILDCARD + result + WILDCARD;
            return result;
        }

        @Override
        public long getDateMin() {
            return convertDate(mDateFrom.getText().toString());
        }

        @Override
        public long getDateMax() {
            return convertDate(mDateTo.getText().toString());
        }

        @Override
        public boolean isNonGeoOnly() {
            return mWithNoGeoInfo.isChecked();
        }

        /**
         * number defining current sorting
         */
        @Override
        public int getSortID() {
            return (mFilter != null) ? mFilter.getSortID() :  SORT_BY_NONE;
        }

        /**
         * false: sort descending
         */
        @Override
        public boolean isSortAscending() {
            return (mFilter != null) ? mFilter.isSortAscending() :  false;
        }

        @Override
        public IGalleryFilter get(IGalleryFilter src) {
            if (src != null) {
                get((IGeoRectangle) src);
                mPath.setText(src.getPath());
                mDateFrom.setText(convertDate(src.getDateMin()));
                mDateTo.setText(convertDate(src.getDateMax()));
                mWithNoGeoInfo.setChecked(src.isNonGeoOnly());
                showLatLon(src.isNonGeoOnly());
            }
            return this;
        }

        @Override
        public IGalleryFilter get(IGeoRectangle src) {
            mLongitudeFrom  .setText(convertLL(src.getLogituedMin()));
            mLongitudeTo    .setText(convertLL(src.getLogituedMax()));
            mLatitudeFrom   .setText(convertLL(src.getLatitudeMin()));
            mLatitudeTo     .setText(convertLL(src.getLatitudeMax()));
            return this;
        }
        /************* local helper *****************/
        private String convertLL(double latLon) {
            if (Double.isNaN(latLon)) return "";
            return DirectoryFormatter.parseLatLon(latLon);
        }

        private String convertDate(long dateMin) {
            if (dateMin == 0) return "";
            return isoDateformatter.format(new Date(dateMin));
        }

        private double convertLL(String string) throws RuntimeException {
            if ((string == null) || (string.length() == 0)) {
                return Double.NaN;
            }

            try {
                return Double.parseDouble(string);
            } catch (Exception ex) {
                throw new RuntimeException(getString(R.string.filter_err_invalid_location_format, string), ex);
            }
        }

        private long convertDate(String string) throws RuntimeException {
            if ((string == null) || (string.length() == 0)) {
                return 0;
            }
            try {
                return this.isoDateformatter.parse(string).getTime();
            } catch (Exception ex) {
                throw new RuntimeException(getString(R.string.filter_err_invalid_date_format, string), ex);
            }
        }
    };

    private void toGui(IGalleryFilter gf) {
        mFilterValue.get(gf);
    }

    private boolean fromGui(IGalleryFilter dest) {
        try {
            if (dest != null) {
                dest.get(mFilterValue);
            }
            return true;
        } catch (RuntimeException ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void clearFilter() {
        GalleryFilterParameter filter = new GalleryFilterParameter();

        if (mFilter != null) {
            filter.setSort(mFilter.getSortID(), mFilter.isSortAscending());
        }

        this.mFilter = filter;
        toGui(mFilter);
    }

    private void onOk() {
        if (fromGui(mFilter)) {
            mHistory.saveHistory();

            final Intent intent = new Intent();
            if (this.mFilter != null) {
                intent.putExtra(EXTRA_FILTER, this.mFilter.toString());
            }
            this.setResult(resultID, intent);
            finish();
        }
    }

    /**************** DirectoryPicker *****************/
    private static class DirInfo {
        public int queryId = 0;
        public IDirectory directoryRoot = null;
        public String currentPath = null;
    }

    private HashMap<Integer, DirInfo> dirInfos = new HashMap<Integer, DirInfo>();
    private DirInfo getOrCreateDirInfo(int queryId) {
        DirInfo result = dirInfos.get(queryId);
        if (result == null) {
            result = new DirInfo();
            result.queryId = queryId;
            dirInfos.put(queryId, result);
        }
        return result;
    }

    private void showLatLonPicker() {
        if (fromGui(mFilter)) {
            final FragmentManager manager = getFragmentManager();
            LocationMapFragment dirDialog = new LocationMapFragment();
            dirDialog.defineNavigation(null, mFilter, OsmdroidUtil.NO_ZOOM, null, null);

            dirDialog.show(manager, DLG_NAVIGATOR_TAG);
        }
    }

    private void showDirectoryPicker(final QueryParameter currentDirContentQuery) {
        if (fromGui(mFilter)) {
            IDirectory directoryRoot = getOrCreateDirInfo(currentDirContentQuery.getID()).directoryRoot;
            if (directoryRoot == null) {
                DirectoryLoaderTask loader = new DirectoryLoaderTask(this, mDebugPrefix) {
                    @Override
                    protected void onPostExecute(IDirectory directoryRoot) {
                        onDirectoryDataLoadComplete(directoryRoot, currentDirContentQuery.getID());
                    }
                };
                loader.execute(currentDirContentQuery);
            } else {
                onDirectoryDataLoadComplete(directoryRoot, currentDirContentQuery.getID());
            }
        }
    }

    private void onDirectoryDataLoadComplete(IDirectory directoryRoot, int queryId) {
        if (directoryRoot != null) {
            Global.debugMemory(mDebugPrefix, "onDirectoryDataLoadComplete");

            DirInfo dirInfo = getOrCreateDirInfo(queryId);
            dirInfo.directoryRoot = directoryRoot;
            final FragmentManager manager = getFragmentManager();
            DirectoryPickerFragment dirDialog = new DirectoryPickerFragment();
            dirDialog.setContextMenuId(R.menu.menu_context_dirpicker);

            dirDialog.defineDirectoryNavigation(dirInfo.directoryRoot, dirInfo.queryId, dirInfo.currentPath);

            dirDialog.show(manager, DLG_NAVIGATOR_TAG);
        }
    }

    /**
     * called when user picks a new directory
     */
    @Override
    public void onDirectoryPick(String selectedAbsolutePath, int queryTypeId) {
        DirInfo dirInfo = getOrCreateDirInfo(queryTypeId);
        dirInfo.currentPath=selectedAbsolutePath;

        FotoSql.set(mFilter,selectedAbsolutePath, queryTypeId);
        toGui(mFilter);
    }

    /** interface DirectoryPickerFragment.invalidateDirectories not used */
    @Override
    public void invalidateDirectories(String why) {
    }

    /** interface DirectoryPickerFragment.OnDirectoryInteractionListener not used */
    @Override
    public void onDirectoryCancel(int queryTypeId) {}

    /** interface DirectoryPickerFragment.OnDirectoryInteractionListener not used */
    @Override
    public void onDirectorySelectionChanged(String selectedChild, int queryTypeId) {}

}

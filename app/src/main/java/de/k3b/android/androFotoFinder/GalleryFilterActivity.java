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
import de.k3b.android.androFotoFinder.queries.QueryParameterParcelable;
import de.k3b.android.osmdroid.ZoomUtil;
import de.k3b.android.widget.AboutDialogPreference;
import de.k3b.android.widget.HistoryEditText;
import de.k3b.io.DirectoryFormatter;
import de.k3b.io.GalleryFilterParameter;
import de.k3b.io.IDirectory;
import de.k3b.io.IGalleryFilter;
import de.k3b.io.IGeoRectangle;

/**
 * Defines a gui for global foto filter: only fotos from certain filepath, date and/or lat/lon will be visible.
 */
public class GalleryFilterActivity extends Activity implements Common, DirectoryPickerFragment.OnDirectoryInteractionListener, LocationMapFragment.OnDirectoryInteractionListener {
    private static final String debugPrefix = "GalF-";

    public static final int resultID = 522;
    private static final String DLG_NAVIGATOR_TAG = "GalleryFilterActivity";
    private static final String SETTINGS_KEY = "GalleryFilterActivity-";

    GalleryFilterParameter mFilter = new GalleryFilterParameter();

    private AsFilter mAsFilter = null;
    private HistoryEditText mHistory;

    public static void showActivity(Activity context, GalleryFilterParameter filter) {
        if (Global.debugEnabled) {
            Log.d(Global.LOG_CONTEXT, context.getClass().getSimpleName()
                    + " > GalleryFilterActivity.showActivity");
        }

        final Intent intent = new Intent().setClass(context,
                GalleryFilterActivity.class);

        if (filter != null) {
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
    protected void onCreate(Bundle savedInstanceState) {
        Global.debugMemory(debugPrefix, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_filter);
        this.mAsFilter = new AsFilter();
        onCreateButtos();

        GalleryFilterParameter filter = getFilter(this.getIntent());

        if (filter != null) {
            mFilter = filter;
            toGui(mFilter);
        }
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
                showLatLonPicker(FotoSql.queryGroupByPlace);
            }
        });

        cmd = (Button) findViewById(R.id.cmd_ok);
        cmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOk();
            }
        });
        cmd = (Button) findViewById(R.id.cmd_cancel);
        cmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        cmd = (Button) findViewById(R.id.cmd_clear);
        cmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearFilter();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_gallery_filter, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (item.getItemId()) {
            case R.id.cmd_about:
                AboutDialogPreference.createAboutDialog(this).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPause () {
        Global.debugMemory(debugPrefix, "onPause");
        saveLastFilter();
        super.onPause();
    }

    @Override
    protected void onResume () {
        Global.debugMemory(debugPrefix, "onResume");
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
            edit.commit();
        }
    }

    @Override
    protected void onDestroy() {
        Global.debugMemory(debugPrefix, "onDestroy start");
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
        Global.debugMemory(debugPrefix, "onDestroy end");
        // RefWatcher refWatcher = AndroFotoFinderApp.getRefWatcher(this);
        // refWatcher.watch(this);
    }

    /** gui content seen as IGalleryFilter */
    private class AsFilter implements IGalleryFilter {
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

        AsFilter() {
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
                    showLatLon();
                }

            });

            mHistory = new HistoryEditText(GalleryFilterActivity.this, new int[] {
                    R.id.cmd_path_history, R.id.cmd_date_from_history, R.id.cmd_date_to_history,
                    R.id.cmd_lat_from_history, R.id.cmd_lat_to_history, R.id.cmd_lon_from_history, R.id.cmd_lon_to_history} ,
                    mPath ,mDateFrom ,mDateTo, mLatitudeFrom, mLatitudeTo, mLongitudeFrom, mLongitudeTo);

        }

        private void showLatLon() {
            show(mWithNoGeoInfo.isChecked(), R.id.cmd_select_lat_lon, R.id.lbl_latitude, R.id.cmd_lat_from_history, R.id.edit_latitude_from,
                    R.id.cmd_lat_to_history, R.id.edit_latitude_to, R.id.lbl_longitude, R.id.cmd_lon_from_history,
                    R.id.edit_longitude_from,R.id.cmd_lon_to_history, R.id.edit_longitude_to);
        }

        private void show(boolean checked, int... ids) {
            for(int id:ids)
                findViewById(id).setVisibility((!checked) ? View.VISIBLE : View.INVISIBLE );
        }

        @Override
        public double getLatitudeMin() {
            return convertLL(mLatitudeFrom.getText().toString());
        }

        @Override
        public double getLatitudeMax() {
            return convertLL(mLatitudeTo.getText().toString());
        }

        @Override
        public double getLogituedMin() {
            return convertLL(mLongitudeFrom.getText().toString());
        }

        @Override
        public double getLogituedMax() {
            return convertLL(mLongitudeTo.getText().toString());
        }

        @Override
        public String getPath() {
            return mPath.getText().toString();
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

        @Override
        public IGalleryFilter get(IGalleryFilter src) {
            get((IGeoRectangle) src);
            mPath           .setText(src.getPath());
            mDateFrom       .setText(convertDate(src.getDateMin()));
            mDateTo         .setText(convertDate(src.getDateMax()));
            mWithNoGeoInfo.setChecked(src.isNonGeoOnly());
            showLatLon();
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
                throw new RuntimeException(getString(R.string.invalid_location, string), ex);
            }
        }

        private long convertDate(String string) throws RuntimeException {
            if ((string == null) || (string.length() == 0)) {
                return 0;
            }
            try {
                return this.isoDateformatter.parse(string).getTime();
            } catch (Exception ex) {
                throw new RuntimeException(getString(R.string.invalid_date, string), ex);
            }
        }
    };

    private void toGui(IGalleryFilter gf) {
        mAsFilter.get(gf);
    }

    private boolean fromGui(IGalleryFilter src) {
        try {
            src.get(mAsFilter);
            return true;
        } catch (RuntimeException ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void clearFilter() {
        mFilter = new GalleryFilterParameter();
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
        int queryId = 0;
        IDirectory directoryRoot = null;
        String currentPath = null;
    }

    private HashMap<Integer, DirInfo> dirInfos = new HashMap<Integer, DirInfo>();
    DirInfo getOrCreateDirInfo(int queryId) {
        DirInfo result = dirInfos.get(queryId);
        if (result == null) {
            result = new DirInfo();
            result.queryId = queryId;
            dirInfos.put(queryId, result);
        }
        return result;
    }

    private void showLatLonPicker(final QueryParameterParcelable currentDirContentQuery) {
        if (fromGui(mFilter)) {
            final FragmentManager manager = getFragmentManager();
            LocationMapFragment dirDialog = new LocationMapFragment();
            dirDialog.defineNavigation(null, mFilter, ZoomUtil.NO_ZOOM, null);

            dirDialog.show(manager, DLG_NAVIGATOR_TAG);
        }
    }

    private void showDirectoryPicker(final QueryParameterParcelable currentDirContentQuery) {
        if (fromGui(mFilter)) {
            IDirectory directoryRoot = getOrCreateDirInfo(currentDirContentQuery.getID()).directoryRoot;
            if (directoryRoot == null) {
                DirectoryLoaderTask loader = new DirectoryLoaderTask(this, debugPrefix) {
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
            Global.debugMemory(debugPrefix, "onDirectoryDataLoadComplete");

            DirInfo dirInfo = getOrCreateDirInfo(queryId);
            dirInfo.directoryRoot = directoryRoot;
            final FragmentManager manager = getFragmentManager();
            DirectoryPickerFragment dirDialog = new DirectoryPickerFragment();
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
    public void invalidateDirectories() {
    }

    /** interface DirectoryPickerFragment.OnDirectoryInteractionListener not used */
    @Override
    public void onDirectoryCancel(int queryTypeId) {}

    /** interface DirectoryPickerFragment.OnDirectoryInteractionListener not used */
    @Override
    public void onDirectorySelectionChanged(String selectedChild, int queryTypeId) {}

}

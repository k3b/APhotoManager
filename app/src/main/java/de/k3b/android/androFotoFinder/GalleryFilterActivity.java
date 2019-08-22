/*
 * Copyright (c) 2015-2019 by k3b.
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
 
package de.k3b.android.androFotoFinder;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import de.k3b.LibGlobal;
import de.k3b.android.androFotoFinder.directory.DirectoryLoaderTask;
import de.k3b.android.androFotoFinder.directory.DirectoryPickerFragment;
import de.k3b.android.androFotoFinder.imagedetail.ImageDetailMetaDialogBuilder;
import de.k3b.android.androFotoFinder.locationmap.LocationMapFragment;
import de.k3b.android.androFotoFinder.locationmap.MapGeoPickerActivity;
import de.k3b.android.androFotoFinder.queries.AndroidAlbumUtils;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.tagDB.TagSql;
import de.k3b.android.androFotoFinder.tagDB.TagsPickerFragment;
import de.k3b.android.osmdroid.OsmdroidUtil;
import de.k3b.android.util.IntentUtil;
import de.k3b.android.widget.AboutDialogPreference;
import de.k3b.android.widget.ActivityWithAutoCloseDialogs;
import de.k3b.android.widget.BaseQueryActivity;
import de.k3b.android.widget.HistoryEditText;
import de.k3b.database.QueryParameter;
import de.k3b.io.AlbumFile;
import de.k3b.io.GalleryFilterParameter;
import de.k3b.io.IDirectory;
import de.k3b.io.IGalleryFilter;
import de.k3b.io.IGeoRectangle;
import de.k3b.io.StringUtils;
import de.k3b.io.VISIBILITY;
import de.k3b.media.MediaFormatter;
import de.k3b.tagDB.Tag;

/**
 * Editor for GalleryFilterParameter and for parsed *.query and *.album QueryParameter-files.
 *
 * Defines a gui for global foto filter: only fotos from certain filepath, date and/or lat/lon will be visible.
 */
public class GalleryFilterActivity extends ActivityWithAutoCloseDialogs
        implements Common, DirectoryPickerFragment.OnDirectoryInteractionListener,
        LocationMapFragment.OnDirectoryInteractionListener,
        TagsPickerFragment.ITagsPicker
{
    private static final String mDebugPrefix = "GalF-";

    private static final String DLG_NAVIGATOR_TAG = "GalleryFilterActivity";
    private static final String DLG_SAVE_AS_TAG = "GalleryFilterActivitySaveAs";
    private static final String SETTINGS_KEY = "GalleryFilterActivity-";
    private static final String LAST_SELECTED_DIR_KEY = "mLastSelectedAlbumDir";

    private static final String WILDCARD = "%";
    private static final int SAVE_AS_VALBUM_PICK = 9921;

    private GalleryFilterParameter mFilter = new GalleryFilterParameter();

    // where to navigate to when folder picker is opend: last path or last picked album file
    private String mLastSelectedAlbumDir = null;

    // parsed filter part of query
    private FilterValue mFilterValue = null;
    // contains the non parsebale part of query
    private QueryParameter mQueryWithoutFilter;

    private HistoryEditText mHistory;

    private GalleryFilterPathState mGalleryFilterPathState = null;
    private VirtualAlbumController mBookmarkController = null;

    /** set while dir picker is active */
    private DirectoryPickerFragment mDirPicker = null;

    public static void showActivity(String debugContext, Activity context,
                                    IGalleryFilter filter, QueryParameter query,
                                    String lastBookmarkFileName, int requestCode) {
        if (Global.debugEnabled) {
            Log.d(Global.LOG_CONTEXT, context.getClass().getSimpleName()
                    + " > GalleryFilterActivity.showActivity");
        }

        final Intent intent = new Intent().setClass(context,
                GalleryFilterActivity.class);

        AndroidAlbumUtils.saveFilterAndQuery(context, null, intent, null, filter, query);
        IntentUtil.startActivity(debugContext, context, requestCode, intent);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        fromGui(mFilter);
        AndroidAlbumUtils.saveFilterAndQuery(this, null, null, savedInstanceState, mFilter, mQueryWithoutFilter);

        savedInstanceState.putString(LAST_SELECTED_DIR_KEY, mLastSelectedAlbumDir);

        mGalleryFilterPathState.save(this, savedInstanceState);

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Global.debugMemory(mDebugPrefix, "onCreate");
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();

        mGalleryFilterPathState = new GalleryFilterPathState().load(this, intent,
                savedInstanceState);
        // for debugging: where does the filter come from
        StringBuilder dbgMessageResult = (Global.debugEnabled) ? new StringBuilder() : null;

        if ((dbgMessageResult != null) && (intent != null)){
            dbgMessageResult.append(StringUtils.appendMessage(dbgMessageResult,
                    mDebugPrefix , "onCreate", intent.toUri(Intent.URI_INTENT_SCHEME)));
        }
        setContentView(R.layout.activity_gallery_filter);
        this.mFilterValue = new FilterValue();
        onCreateButtos();

        final QueryParameter query = AndroidAlbumUtils.getQuery(this, "",
                savedInstanceState, intent, null, null, dbgMessageResult);
        setQueryAndFilter(query);

        if (dbgMessageResult != null) {
            Log.i(Global.LOG_CONTEXT, dbgMessageResult.toString());
        }

        mBookmarkController = new VirtualAlbumController(this);

        if (savedInstanceState != null) {
            mLastSelectedAlbumDir = savedInstanceState.getString(LAST_SELECTED_DIR_KEY);
        }
    }

    private void setQueryAndFilter(QueryParameter query) {
        this.mQueryWithoutFilter = query;
        if (this.mQueryWithoutFilter == null) {
            this.mQueryWithoutFilter = new QueryParameter();
            this.mFilter = new GalleryFilterParameter();
        } else {
            this.mFilter.get(TagSql.parseQueryEx(this.mQueryWithoutFilter, true));
        }

        mFilterValue.showLatLon(this.mFilter.isNonGeoOnly());
        toGui(this.mFilter);
    }

    private void onCreateButtos() {
        Button cmd = (Button) findViewById(R.id.cmd_path);
        cmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String path = (!StringUtils.isNullOrEmpty(mLastSelectedAlbumDir)) ? mLastSelectedAlbumDir : getAsGalleryFilter().getPath();

                if (StringUtils.isNullOrEmpty(path)) {
                    path = mGalleryFilterPathState.load(GalleryFilterActivity.this,
                                null, null)
                            .getPathDefault(null);
                }
                if (path != null) path = path.replaceAll("%","");
                showDirectoryPickerForFilterParamValue(
                        mDebugPrefix + " path picker " + path,
                        FotoSql.queryGroupByDir,
                        true,
                        false, path);
            }
        });
        cmd = (Button) findViewById(R.id.cmd_date);
        cmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String path = getAsGalleryFilter().getDatePath();
                showDirectoryPickerForFilterParamValue(
                        mDebugPrefix + " date picker " + path,
                        FotoSql.queryGroupByDate, false,
                        LibGlobal.datePickerUseDecade, path);
            }
        });
        cmd = (Button) findViewById(R.id.cmd_date_modified);
        cmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String path = getAsGalleryFilter().getDateModifiedPath();
                showDirectoryPickerForFilterParamValue(
                        mDebugPrefix + " date modified picker " + path,
                        FotoSql.queryGroupByDateModified, false,
                        LibGlobal.datePickerUseDecade, path);
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
        cmd = (Button) findViewById(R.id.cmd_tags_include);
        cmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//              showDirectoryPicker(FotoSql.queryGroupByPlace);
                showTagPicker(R.string.tags_activity_title);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_edit_common, menu);
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
                SettingsActivity.showActivity(this);
                return true;

            case R.id.cmd_gallery:
                FotoGalleryActivity.showActivity("[2]", this, getAsMergedQuery(), 0);
                        // TagSql.filter2NewQuery(getAsGalleryFilter()), 0);
                return true;
            case R.id.cmd_show_geo: {
                MapGeoPickerActivity.showActivity("[3]", this, null, getAsMergedQuery(), 0);
                return true;
            }
            case R.id.action_details:
                cmdShowDetails();
                return true;


            case R.id.action_save_as:
                mGalleryFilterPathState.load(this,null,null);
                // mBookmarkController.onSaveAsQuestion(mBookmarkController.getlastBookmarkFileName(), getAsQuery());
                File valbum = mGalleryFilterPathState.getSaveAlbumAs(getString(R.string.mk_dir_default), AlbumFile.SUFFIX_VALBUM);
                DialogFragment dlg = mBookmarkController.onSaveAsVirutalAlbumQuestion(valbum, getAsMergedQuery());
                setAutoClose(dlg, null, null);
                invalidatePathData();

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

        IDirectory lastPopUpSelection = (mDirPicker == null) ? null : mDirPicker.getLastPopUpSelection();
        if (lastPopUpSelection != null) lastPopUpSelection.refresh();
    }

    private GalleryFilterParameter getAsGalleryFilter() {
        GalleryFilterParameter filter = new GalleryFilterParameter();
        fromGui(filter);
        return filter;
    }

    private QueryParameter getAsMergedQuery() {
        return AndroidAlbumUtils.getAsMergedNewQueryParameter(mQueryWithoutFilter, getAsGalleryFilter());
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
        if (LockScreen.isLocked(this)) {
            // filter is forbidden when locked. Might occur via
            //    gallery -> filter -> dir-pick -> openInNew Gallery -> exit
            finish();
        }
    }

    private void loadLastFilter() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        loadLastFilter(sharedPref, FotoSql.QUERY_TYPE_GROUP_ALBUM);
        loadLastFilter(sharedPref, FotoSql.QUERY_TYPE_GALLERY);
        loadLastFilter(sharedPref, FotoSql.QUERY_TYPE_GROUP_DATE);
        loadLastFilter(sharedPref, FotoSql.QUERY_TYPE_GROUP_PLACE);
        loadLastFilter(sharedPref, FotoSql.QUERY_TYPE_GROUP_DATE_MODIFIED);
    }

    private void loadLastFilter(SharedPreferences sharedPref, int queryTypeID) {
        getOrCreateDirInfo(queryTypeID).currentPath = sharedPref.getString(SETTINGS_KEY + queryTypeID, null);
    }

    private void saveLastFilter() {
        if (mDirInfos != null)
        {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor edit = sharedPref.edit();

            for(Integer id : mDirInfos.keySet()) {
                DirInfo dir = mDirInfos.get(id);
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

        if (mDirInfos != null)
        {
            for(Integer id : mDirInfos.keySet()) {
                DirInfo dir = mDirInfos.get(id);
                if (dir.directoryRoot != null) {
                    dir.directoryRoot.destroy();
                }
            }
            mDirInfos = null;
        }

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
        private EditText mDateModifiedFrom;
        private EditText mDateModifiedTo;
        private EditText mLongitudeFrom;
        private EditText mLongitudeTo;
        private EditText mLatitudeTo;
        private EditText mLatitudeFrom;
        private CheckBox mWithNoGeoInfo;
        private CheckBox mWithNoTags;
        private RatingBar mRatingBar;
        private CheckBox mPublic        ;
        private CheckBox mPrivate       ;
        private EditText mAny            ;
        private EditText mTagsInclude    ;
        private EditText mTagsExclude    ;
        private VISIBILITY mVisibility = VISIBILITY.DEFAULT;

        FilterValue() {
            this.mPath = (EditText) findViewById(R.id.edit_path);
            this.mAny             = (EditText) findViewById(R.id.edit_any);
            this.mTagsInclude     = (EditText) findViewById(R.id.edit_tags_include);
            this.mTagsExclude     = (EditText) findViewById(R.id.edit_tags_exclude);
            this.mDateFrom = (EditText) findViewById(R.id.edit_date_from);
            this.mDateTo = (EditText) findViewById(R.id.edit_date_to);
            this.mDateModifiedFrom = (EditText) findViewById(R.id.edit_date_modified_from);
            this.mDateModifiedTo = (EditText) findViewById(R.id.edit_date_modified_to);
            this.mLatitudeFrom = (EditText) findViewById(R.id.edit_latitude_from);
            this.mLatitudeTo = (EditText) findViewById(R.id.edit_latitude_to);
            this.mLongitudeFrom = (EditText) findViewById(R.id.edit_longitude_from);
            this.mLongitudeTo = (EditText) findViewById(R.id.edit_longitude_to);
            this.mWithNoGeoInfo = (CheckBox) findViewById(R.id.chk_with_no_geo);
            this.mWithNoTags = (CheckBox) findViewById(R.id.chk_with_no_tags);
            this.mRatingBar = (RatingBar ) findViewById(R.id.ratingBar);
            this.mPublic        = (CheckBox) findViewById(R.id.chk_public);
            this.mPrivate       = (CheckBox) findViewById(R.id.chk_private);

            mWithNoGeoInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showLatLon(mWithNoGeoInfo.isChecked());
                }

            });
            mPublic.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showVisibility(mPublic, mPrivate);
                }

            });
            mPrivate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showVisibility(mPrivate, mPublic);
                }

            });

            mHistory = new HistoryEditText(GalleryFilterActivity.this, new int[] {
                    R.id.cmd_path_history,
                    R.id.cmd_date_from_history,
                    R.id.cmd_date_to_history,
                    R.id.cmd_date_modified_from_history,
                    R.id.cmd_date_modified_to_history,
                    R.id.cmd_lat_from_history, R.id.cmd_lat_to_history, R.id.cmd_lon_from_history, R.id.cmd_lon_to_history ,
                    R.id.cmd_any_history,
                    R.id.cmd_tags_include_history,
                    R.id.cmd_tags_exclude_history} ,
                    mPath ,
                    mDateFrom ,
                    mDateTo,
                    mDateModifiedFrom ,
                    mDateModifiedTo,
                    mLatitudeFrom, mLatitudeTo, mLongitudeFrom, mLongitudeTo,
                    mAny             ,
                    mTagsInclude     ,
                    mTagsExclude).setIncludeEmpty(true);
        }

        protected void showVisibility(VISIBILITY visibility) {
            VISIBILITY actualVisibility = visibility;
            if (actualVisibility == VISIBILITY.DEFAULT) {
                actualVisibility = (LibGlobal.visibilityShowPrivateByDefault) ? VISIBILITY.PRIVATE_PUBLIC : VISIBILITY.PUBLIC;
            }

            switch (actualVisibility) {
                case PRIVATE:
                    mPrivate.setChecked(true);
                    mPublic.setChecked(false);
                    break;
                case PRIVATE_PUBLIC:
                    mPrivate.setChecked(true);
                    mPublic.setChecked(true);
                    break;
                case PUBLIC:
                default:
                    mPublic.setChecked(true);
                    mPrivate.setChecked(false);
                    break;

            }
        }

        private void showVisibility(CheckBox chk1, CheckBox chk2) {
            if ((!chk1.isChecked()) && (!chk2.isChecked())) chk2.setChecked(true);
            if (mPrivate.isChecked()) {
                if (mPublic.isChecked()) {
                    mVisibility = VISIBILITY.PRIVATE_PUBLIC;
                } else {
                    mVisibility = VISIBILITY.PRIVATE;
                }
            } else {
                mVisibility = VISIBILITY.PUBLIC;
            }
        }

        protected void showLatLon(boolean noGeoInfo) {
            show(noGeoInfo, R.id.cmd_select_lat_lon, R.id.lbl_latitude, R.id.cmd_lat_from_history, R.id.edit_latitude_from,
                    R.id.cmd_lat_to_history, R.id.edit_latitude_to, R.id.lbl_longitude, R.id.cmd_lon_from_history,
                    R.id.edit_longitude_from,R.id.cmd_lon_to_history, R.id.edit_longitude_to);
        }

        private void show(boolean checked, int... ids) {
            for(int id:ids)
                findViewById(id).setVisibility((!checked) ? View.VISIBLE : View.GONE );
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
        public String getInAnyField() {
            return mAny.getText().toString();
        }

        @Override
        public List<String> getTagsAllIncluded() {
            return GalleryFilterParameter.convertList(mTagsInclude.getText().toString());
        }

        @Override
        public List<String>  getTagsAllExcluded() {
            return GalleryFilterParameter.convertList(mTagsExclude.getText().toString());
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
        public long getDateModifiedMin() {
            return convertDate(mDateModifiedFrom.getText().toString());
        }

        @Override
        public long getDateModifiedMax() {
            return convertDate(mDateModifiedTo.getText().toString());
        }

        @Override
        public boolean isNonGeoOnly() {
            return mWithNoGeoInfo.isChecked();
        }

        @Override
        public boolean isWithNoTags() {
            return mWithNoTags.isChecked();
        }

        @Override
        public int getRatingMin() {
            return (int) mRatingBar.getRating();
        }

        @Override
        public VISIBILITY getVisibility() {
            return mVisibility;
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
            return (mFilter != null) && mFilter.isSortAscending();
        }

        @Override
        public IGalleryFilter get(IGalleryFilter src) {
            if (src != null) {
                get((IGeoRectangle) src);
                mPath.setText(src.getPath());
                mAny            .setText(src.getInAnyField());
                mTagsInclude    .setText(GalleryFilterParameter.convertList(src.getTagsAllIncluded()));
                mTagsExclude    .setText(GalleryFilterParameter.convertList(src.getTagsAllExcluded()));
                mDateFrom.setText(convertDate(src.getDateMin()));
                mDateTo.setText(convertDate(src.getDateMax()));
                mDateModifiedFrom.setText(convertDate(src.getDateModifiedMin()));
                mDateModifiedTo.setText(convertDate(src.getDateModifiedMax()));
                mWithNoGeoInfo.setChecked(src.isNonGeoOnly());
                mWithNoTags.setChecked(src.isWithNoTags());
                mRatingBar.setRating(src.getRatingMin());
                mVisibility = src.getVisibility();

                showVisibility(mVisibility);

                showLatLon(src.isNonGeoOnly());
            }
            return this;
        }

        @Override
        public IGalleryFilter get(IGeoRectangle src) {
            mLongitudeFrom.setText(MediaFormatter.convertLL(src.getLogituedMin()));
            mLongitudeTo.setText(MediaFormatter.convertLL(src.getLogituedMax()));
            mLatitudeFrom.setText(MediaFormatter.convertLL(src.getLatitudeMin()));
            mLatitudeTo.setText(MediaFormatter.convertLL(src.getLatitudeMax()));
            return this;
        }
        /************* local helper *****************/

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

        @Override public String toString() {
            return new GalleryFilterParameter().get(this).toString();
        }

    }

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
            Log.e(Global.LOG_CONTEXT, mDebugPrefix + ex.getMessage(), ex);
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void cmdShowDetails() {
        final QueryParameter asMergedQuery = getAsMergedQuery();

        final Dialog dlg = ImageDetailMetaDialogBuilder.createImageDetailDialog(
                this,
                getTitle().toString(),
                asMergedQuery.toSqlString(),
                TagSql.getStatisticsMessage(this, R.string.show_photo, asMergedQuery)
        );
        dlg.show();
        setAutoClose(null, dlg, null);
    }

    private void clearFilter() {
        GalleryFilterParameter filter = new GalleryFilterParameter();

        if (mFilter != null) {
            filter.setSort(mFilter.getSortID(), mFilter.isSortAscending());
        }

        mFilter = filter;
        mQueryWithoutFilter = new QueryParameter();
        toGui(mFilter);
    }

    private void onOk() {
        if (fromGui(mFilter)) {
            mHistory.saveHistory();

            final Intent resultIntent = new Intent();
            final Intent originalIntent = getIntent();

            // if result must be saved to file
            Uri originalUri = (originalIntent == null) ? null : originalIntent.getData();

            AndroidAlbumUtils.saveFilterAndQuery(this, originalUri, resultIntent, null, mFilter, mQueryWithoutFilter);

            this.setResult(BaseQueryActivity.resultID, resultIntent);

            finish();
        }
    }

    /**************** DirectoryPicker *****************/
    private static class DirInfo {
        public int queryId = 0;
        public IDirectory directoryRoot = null;
        public String currentPath = null;
    }

    /** one DirInfo per possible queyType */
    private HashMap<Integer, DirInfo> mDirInfos = new HashMap<Integer, DirInfo>();
    private DirInfo getOrCreateDirInfo(int queryId) {
        DirInfo result = mDirInfos.get(queryId);
        if (result == null) {
            result = new DirInfo();
            result.queryId = queryId;
            mDirInfos.put(queryId, result);
        }
        return result;
    }

    private void showTagPicker(int idTitle) {
        if (fromGui(mFilter)) {
            final FragmentManager manager = getFragmentManager();
            TagsPickerFragment dlg = new TagsPickerFragment();
            dlg.setFragmentOnwner(this);
            dlg.setTitleId(idTitle);
            dlg.setAddNames(mFilter.getTagsAllIncluded());
            dlg.setRemoveNames(mFilter.getTagsAllExcluded());
            dlg.show(manager, DLG_NAVIGATOR_TAG);
            setAutoClose(dlg, null, null);
        }
    }

    /** called by {@link TagsPickerFragment} */
    @Override
    public boolean onCancel(String msg) {
        setAutoClose(null, null, null);
        return true;
    }

    /** called by {@link TagsPickerFragment} */
    @Override
    public boolean onOk(List<String> addNames, List<String> removeNames) {
        mFilter.setTagsAllIncluded(addNames);
        mFilter.setTagsAllExcluded(removeNames);
        toGui(mFilter);
        setAutoClose(null, null, null);
        return true;
    }

    /** called by {@link TagsPickerFragment} */
    @Override
    public boolean onTagPopUpClick(int menuItemItemId, Tag selectedTag) {
        return TagsPickerFragment.handleMenuShow(menuItemItemId, selectedTag, this, getAsMergedQuery());
    }

    private void showLatLonPicker() {
        if (fromGui(mFilter)) {
            final FragmentManager manager = getFragmentManager();
            LocationMapFragment dlg = new LocationMapFragment();
            dlg.defineNavigation(null, null, mFilter, OsmdroidUtil.NO_ZOOM, null, null, false);

            dlg.show(manager, DLG_NAVIGATOR_TAG);
            setAutoClose(dlg, null, null);
        }
    }

    private void showDirectoryPickerForFilterParamValue(
                            final String debugContext, final QueryParameter currentDirContentQuery,
                            boolean addVAlbums, boolean datePickerUseDecade,
                            final String initialSelection) {
        if (fromGui(mFilter)) {
            final DirInfo dirInfo = getOrCreateDirInfo(currentDirContentQuery.getID());
            IDirectory directoryRoot = dirInfo.directoryRoot;
            dirInfo.currentPath = initialSelection;
            if (directoryRoot == null) {
                DirectoryLoaderTask loader = new DirectoryLoaderTask(this, datePickerUseDecade, debugContext) {
                    @Override
                    protected void onPostExecute(IDirectory directoryRoot) {
                        onDirectoryDataLoadCompleteForFilterParamValue(directoryRoot, currentDirContentQuery.getID());
                    }
                };

                if (addVAlbums) {
                    // load dir-s + "*.album"
                    loader.execute(currentDirContentQuery, FotoSql.queryVAlbum);
                } else {
                    loader.execute(currentDirContentQuery);
                }
            } else {
                onDirectoryDataLoadCompleteForFilterParamValue(directoryRoot, currentDirContentQuery.getID());
            }
        }
    }

    private void onDirectoryDataLoadCompleteForFilterParamValue(IDirectory directoryRoot, int queryId) {
        if (directoryRoot != null) {
            Global.debugMemory(mDebugPrefix, "onDirectoryDataLoadComplete");

            DirInfo dirInfo = getOrCreateDirInfo(queryId);
            dirInfo.directoryRoot = directoryRoot;
            final FragmentManager manager = getFragmentManager();
            DirectoryPickerFragment dlg = new DirectoryPickerFragment();

            int menuResId = 0; // no menu in app lock mode
            if (!LockScreen.isLocked(this)) {
                menuResId = R.menu.menu_context_dirpicker;
                if ((queryId == FotoSql.QUERY_TYPE_GROUP_DATE) || (queryId == FotoSql.QUERY_TYPE_GROUP_DATE)) {
                    menuResId = R.menu.menu_context_datepicker;
                }
            }
            dlg.setContextMenuId(menuResId);

            dlg.defineDirectoryNavigation(dirInfo.directoryRoot, dirInfo.queryId, dirInfo.currentPath);
            dlg.show(manager, DLG_NAVIGATOR_TAG);
            mDirPicker = dlg;

            setAutoClose(dlg, null, null);
        }
    }

    /**
     * called when user picks a new directory.
     * Sources: Filter-Path, Filter-Date or SaveAs distinguished via queryTypeId
     */
    @Override
    public void onDirectoryPick(final String selectedAbsolutePath, int queryTypeId) {
        closeDialogIfNeeded();
        mGalleryFilterPathState.load(this, null, null);

        File selectedAlbumFile = AlbumFile.getExistingQueryFileOrNull(selectedAbsolutePath);
        if (selectedAlbumFile != null) {
            // selection was an album file
            final String selectedAlbumPath = selectedAlbumFile.getPath();
            setQueryAndFilter(AndroidAlbumUtils.getQueryFromUri(mDebugPrefix + ".onDirectoryPick loading album "
                    + selectedAlbumPath, this, Uri.fromFile(selectedAlbumFile), null));
            mGalleryFilterPathState.setAlbum(Uri.fromFile(selectedAlbumFile));
            mGalleryFilterPathState.setLastPath(selectedAlbumFile.getParent());
            mLastSelectedAlbumDir = selectedAlbumPath; //??electedAlbumFile.getParent();
        } else if (AlbumFile.isQueryFile(selectedAbsolutePath)) {
            // album does not exist (any more) rescan
            AndroidAlbumUtils.albumMediaScan(mDebugPrefix + " onAlbumPick not found in filesystem => ",
                    this, new File(selectedAbsolutePath), 1);

            mGalleryFilterPathState.setLastPath(selectedAlbumFile.getParent());
            invalidatePathData();
        } else {
            // selection was a path (os-dir or date)
            mLastSelectedAlbumDir = null;
            FotoSql.set(mFilter, selectedAbsolutePath, queryTypeId);
            if (queryTypeId == FotoSql.QUERY_TYPE_GROUP_ALBUM) {
                mGalleryFilterPathState.setLastPath(selectedAbsolutePath);
            }
            toGui(mFilter);
        }
    }

    private void invalidatePathData() {
        getOrCreateDirInfo(FotoSql.QUERY_TYPE_GROUP_ALBUM).directoryRoot = null;
        getOrCreateDirInfo(FotoSql.QUERY_TYPE_GALLERY).directoryRoot = null;
    }

    /** interface DirectoryPickerFragment.invalidateDirectories not used */
    @Override
    public void invalidateDirectories(String why) {
    }

    /** interface DirectoryPickerFragment.OnDirectoryInteractionListener not used */
    @Override
    public void onDirectoryCancel(int queryTypeId) {closeDialogIfNeeded();}

    @Override
    protected void closeDialogIfNeeded() {
        super.closeDialogIfNeeded();
        mDirPicker = null;
    }

    /** interface DirectoryPickerFragment.OnDirectoryInteractionListener not used */
    @Override
    public void onDirectorySelectionChanged(String selectedChild, int queryTypeId) {}

}

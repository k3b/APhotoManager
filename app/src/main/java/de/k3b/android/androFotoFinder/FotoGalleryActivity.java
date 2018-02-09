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
 
package de.k3b.android.androFotoFinder;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

// import com.squareup.leakcanary.RefWatcher;

import java.util.ArrayList;
import java.util.List;

import de.k3b.FotoLibGlobal;
import de.k3b.android.androFotoFinder.directory.DirectoryGui;
import de.k3b.android.androFotoFinder.directory.DirectoryLoaderTask;
import de.k3b.android.androFotoFinder.gallery.cursor.GalleryCursorFragment;
import de.k3b.android.androFotoFinder.imagedetail.ImageDetailActivityViewPager;
import de.k3b.android.androFotoFinder.locationmap.GeoEditActivity;
import de.k3b.android.androFotoFinder.locationmap.LocationMapFragment;
import de.k3b.android.androFotoFinder.directory.DirectoryPickerFragment;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.queries.FotoViewerParameter;
import de.k3b.android.androFotoFinder.queries.Queryable;
import de.k3b.android.androFotoFinder.tagDB.TagSql;
import de.k3b.android.androFotoFinder.tagDB.TagsPickerFragment;
import de.k3b.android.osmdroid.OsmdroidUtil;
import de.k3b.android.util.GarbageCollector;
import de.k3b.android.util.IntentUtil;
import de.k3b.android.util.MediaScanner;
import de.k3b.android.widget.AboutDialogPreference;
import de.k3b.android.widget.LocalizedActivity;
import de.k3b.database.QueryParameter;
import de.k3b.io.collections.SelectedItems;
import de.k3b.io.Directory;
import de.k3b.io.DirectoryFormatter;
import de.k3b.io.GalleryFilterParameter;
import de.k3b.io.GeoRectangle;
import de.k3b.io.IDirectory;
import de.k3b.io.IGalleryFilter;
import de.k3b.io.ListUtils;
import de.k3b.tagDB.Tag;

public class FotoGalleryActivity extends LocalizedActivity implements Common,
        OnGalleryInteractionListener, DirectoryPickerFragment.OnDirectoryInteractionListener,
        LocationMapFragment.OnDirectoryInteractionListener,
        TagsPickerFragment.ITagsPicker
{
    private static final String mDebugPrefix = "GalleryA-";

    private static final String DEFAULT_BOOKMARKNAME_PICK_GEO = "pickGeoFromPhoto";
    /** intent parameters supported by FotoGalleryActivity: EXTRA_... */

    private static final String DLG_NAVIGATOR_TAG = "navigator";

    /** after media db change cached Directories must be recalculated */
    private final ContentObserver mMediaObserverDirectory = new ContentObserver(null) {
        @Override
        public void onChange(boolean selfChange) {
            invalidateDirectories(mDebugPrefix + "#onChange from mMediaObserverDirectory");
        }
    };
    private final BookmarkController.IQueryConsumer mLoadBookmarkResultConsumer = new BookmarkController.IQueryConsumer() {
        @Override
        public void setQuery(String fileName, QueryParameter newQuery) {
            final IGalleryFilter whereFilter = TagSql.parseQueryEx(newQuery, true);
            mGalleryQueryParameter.mGalleryContentQuery = newQuery;
            mBookmarkController.setlastBookmarkFileName(fileName);
            String why = "#loadedBookmark " + fileName;
            onFilterChanged(whereFilter, why);
            invalidateDirectories(mDebugPrefix + why);
            mGalleryQueryParameter.setHasUserDefinedQuery(true);
            reloadGui(why);
        }
    };

    /** set while dir picker is active */
    private DialogFragment mDirPicker = null;

    private GalleryQueryParameter mGalleryQueryParameter = new GalleryQueryParameter();
    // multi selection support
    private SelectedItems mSelectedItems = null;

    private Queryable mGalleryGui;

    private boolean mHasEmbeddedDirPicker = false;
    private DirectoryGui mDirGui;
    private IDirectory mPopUpSelection = null;

    private String mTitleResultCount = "";

    private IDirectory mDirectoryRoot = null;

    /** true if activity should show navigator dialog after loading mDirectoryRoot is complete */
    private boolean mMustShowNavigator = false;

    private static class GalleryQueryParameter {
        private static final String STATE_SUB_FILTER_CurrentPath = "CurrentPath";
        private static final String STATE_DirQueryID = "DirQueryID";
        private static final String STATE_SortID = "SortID";
        private static final String STATE_SortAscending = "SortAscending";
        private static final String STATE_Filter = "filter";
        private static final String STATE_SUB_FILTER_LAT_LON = "currentLatLon";
        private static final String STATE_SUB_FILTER_TAGS = "currentTags";
        private static final String STATE_SUB_FILTR_MODE = "currentSubFilterMode";
        private static final String PICK_GEO_SUFFIX = "-pick-geo";

        /** true use latLonPicker; false use directoryPicker */
        private static final int SUB_FILTER_MODE_PATH = 0;
        private static final int SUB_FILTER_MODE_GEO = 1;
        private static final int SUB_FILTER_MODE_TAG = 2;
        private int mCurrentSubFilterMode = SUB_FILTER_MODE_PATH;

        private GeoRectangle mCurrentLatLonFromGeoAreaPicker = new GeoRectangle();
        private List<String> mCurrentTagsFromPicker = new ArrayList<String>();

        /** one of the FotoSql.QUERY_TYPE_xxx values */
        protected int mDirQueryID = FotoSql.QUERY_TYPE_GROUP_DEFAULT;

        private boolean mHasUserDefinedQuery = false;

        /** current sort order */
        private int mCurrentSortID = FotoSql.SORT_BY_DEFAULT;
        /** current sort order */
        private boolean mCurrentSortAscending = false;

        private String mCurrentPathFromFolderPicker = "/";

        /** Filter parameter defining current visible items */
        private IGalleryFilter mCurrentFilterSettings;

        /** sql defines current visible items with optional sort order */
        protected QueryParameter mGalleryContentQuery = null;

        /** true: if activity started without special intent-parameters,
         *  the last mCurrentFilterSettings is saved/loaded for next use */
        private boolean mSaveToSharedPrefs = true;

        /** view/pick-image/pick-geo have different state persistence.
         * naem=STATE_XXXXX + mStatSuffix
         * ""==view; "-pick-image"; "-pick-geo" */
        private String mStatSuffix = "";

        /** one of the FotoSql.QUERY_TYPE_xxx values. if undefined use default */
        private int getDirQueryID() {
            if (this.mDirQueryID == FotoSql.QUERY_TYPE_UNDEFINED)
                return FotoSql.QUERY_TYPE_GROUP_DEFAULT;

            return this.mDirQueryID;
        }

        public int getSortID() {
            return mCurrentSortID;
        }
        public void setSortID(int sortID) {
            if (sortID == mCurrentSortID) {
                mCurrentSortAscending = !mCurrentSortAscending;
            } else {
                mCurrentSortAscending = true;
                mCurrentSortID = sortID;
            }
        }

        public String getSortDisplayName(Context context) {
            return  FotoSql.getName(context, this.mCurrentSortID) + " " + ((mCurrentSortAscending) ? IGalleryFilter.SORT_DIRECTION_ASCENDING : IGalleryFilter.SORT_DIRECTION_DESCENDING);
        }

        public boolean clearPathIfActive() {
            if ((mCurrentSubFilterMode == SUB_FILTER_MODE_PATH) && (mCurrentPathFromFolderPicker != null)) {
                mCurrentPathFromFolderPicker = null;
                return true;
            }
            return false;
        }

        /** combine root-query plus current selected directoryRoot/geo/tags */
        private QueryParameter calculateEffectiveGalleryContentQuery() {
            return calculateEffectiveGalleryContentQuery(mGalleryContentQuery);
        }

        /** combine root-query plus current selected directoryRoot */
        private QueryParameter calculateEffectiveGalleryContentQuery(QueryParameter rootQuery) {
            if (rootQuery == null) return null;

            // .nomedia folder has no current sql
            if ((this.getCurrentFilterSettings() != null) && MediaScanner.isNoMedia(this.getCurrentFilterSettings().getPath(), MediaScanner.DEFAULT_SCAN_DEPTH)) {
                return null;
            }

            QueryParameter result = new QueryParameter(rootQuery);

            TagSql.filter2QueryEx(result, this.getCurrentFilterSettings(), !hasUserDefinedQuery());
            if (result == null) return null;

            if (mCurrentSubFilterMode == SUB_FILTER_MODE_GEO) {
                FotoSql.addWhereFilterLatLon(result, mCurrentLatLonFromGeoAreaPicker);
            } else if (mCurrentSubFilterMode == SUB_FILTER_MODE_TAG) {
                TagSql.addWhereTagsIncluded(result, mCurrentTagsFromPicker,false);
            } else if (this.mCurrentPathFromFolderPicker != null) {
                FotoSql.addPathWhere(result, this.mCurrentPathFromFolderPicker, this.getDirQueryID());
            }

            if (mCurrentSortID != IGalleryFilter.SORT_BY_NONE) {
                FotoSql.setSort(result, mCurrentSortID, mCurrentSortAscending);
            }
            return result;
        }

        private void saveInstanceState(Context context, Bundle savedInstanceState) {
            saveSettings(context);

            // save InstanceState
            savedInstanceState.putInt(STATE_DirQueryID , this.getDirQueryID());
            if (mStatSuffix.length() == 0) {
                savedInstanceState.putString(STATE_SUB_FILTER_LAT_LON, this.mCurrentLatLonFromGeoAreaPicker.toString());
                savedInstanceState.putString(STATE_SUB_FILTER_CurrentPath, this.mCurrentPathFromFolderPicker);
                savedInstanceState.putString(STATE_SUB_FILTER_TAGS, ListUtils.toString(mCurrentTagsFromPicker));
            }
            savedInstanceState.putInt(STATE_SortID, this.mCurrentSortID);
            savedInstanceState.putBoolean(STATE_SortAscending, this.mCurrentSortAscending);
            if (this.getCurrentFilterSettings() != null) {
                savedInstanceState.putString(STATE_Filter, this.getCurrentFilterSettings().toString());
            }
            savedInstanceState.putInt(STATE_SUB_FILTR_MODE, this.mCurrentSubFilterMode);
        }

        private void saveSettings(Context context) {
            if (mSaveToSharedPrefs) {
                // save settings
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor edit = sharedPref.edit();

                edit.putInt(STATE_DirQueryID + mStatSuffix, this.getDirQueryID());
                edit.putInt(STATE_SortID + mStatSuffix, this.mCurrentSortID);
                edit.putBoolean(STATE_SortAscending + mStatSuffix, this.mCurrentSortAscending);

                if (mStatSuffix.length() == 0) {
                    edit.putString(STATE_SUB_FILTER_CurrentPath, this.mCurrentPathFromFolderPicker);
                    edit.putString(STATE_SUB_FILTER_TAGS, ListUtils.toString(mCurrentTagsFromPicker));
                    edit.putString(STATE_SUB_FILTER_LAT_LON, this.mCurrentLatLonFromGeoAreaPicker.toString());
                }

                if (getCurrentFilterSettings() != null) {
                    edit.putString(STATE_Filter + mStatSuffix, getCurrentFilterSettings().toString());
                }
                edit.apply();
            }
        }

        public boolean isGeoPick() {
            return (mStatSuffix != null) && mStatSuffix.equals(PICK_GEO_SUFFIX);
        }

        // load from settings/instanceState
        private void loadSettingsAndInstanceState(Activity context, Bundle savedInstanceState) {

            Intent intent = context.getIntent();

            // for debugging: where does the filter come from
            StringBuilder dbgFilter = (Global.debugEnabled) ? new StringBuilder() : null;
            String filter = null;
            String pathFilter = null;

            if (intent != null) {
                filter = intent.getStringExtra(EXTRA_FILTER);

                if ((filter != null) && (dbgFilter != null)) dbgFilter.append("filter from ").append(EXTRA_FILTER).append("=").append(filter).append("\n");

                Uri uri = IntentUtil.getUri(intent);
                boolean fileUri = IntentUtil.isFileUri(uri);
                if (filter == null) {

                    if (fileUri) {
                        pathFilter = uri.getSchemeSpecificPart();
                        if (pathFilter != null) pathFilter = pathFilter.replace('*', '%');
                        if (dbgFilter != null) dbgFilter.append("path from uri=").append(pathFilter).append("\n");
                    } else {
                        String action = (intent != null) ? intent.getAction() : null;

                        if ((action != null) && ((Intent.ACTION_PICK.compareTo(action) == 0) || (Intent.ACTION_GET_CONTENT.compareTo(action) == 0))) {
                            mStatSuffix = "-pick-image";
                            String schema = intent.getScheme();
                            if ((schema != null) && ("geo".compareTo(schema) == 0)) {
                                mStatSuffix = PICK_GEO_SUFFIX;
                            }
                        }
                    }
                }
                this.mSaveToSharedPrefs = ((filter == null) && (pathFilter == null) && (!fileUri) ); // false if controlled via intent
            } else {
                this.mSaveToSharedPrefs = true;
            }

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            if (this.mSaveToSharedPrefs) {
                if (mStatSuffix.length() == 0) {
                    this.mCurrentLatLonFromGeoAreaPicker.get(DirectoryFormatter.parseLatLon(sharedPref.getString(STATE_SUB_FILTER_LAT_LON, null)));
                    this.mCurrentPathFromFolderPicker = sharedPref.getString(STATE_SUB_FILTER_CurrentPath, this.mCurrentPathFromFolderPicker);
                    this.mCurrentTagsFromPicker = new ArrayList<>(ListUtils.fromString(sharedPref.getString(STATE_SUB_FILTER_TAGS, ListUtils.toString(mCurrentTagsFromPicker))));
                }
                this.mDirQueryID = sharedPref.getInt(STATE_DirQueryID + mStatSuffix, this.getDirQueryID());
                this.mCurrentSortID = sharedPref.getInt(STATE_SortID + mStatSuffix, this.mCurrentSortID);
                this.mCurrentSortAscending = sharedPref.getBoolean(STATE_SortAscending + mStatSuffix, this.mCurrentSortAscending);
            }

            // instance state overrides settings
            if (savedInstanceState != null) {
                if (mStatSuffix.length() == 0) {
                    this.mCurrentLatLonFromGeoAreaPicker.get(DirectoryFormatter.parseLatLon(savedInstanceState.getString(STATE_SUB_FILTER_LAT_LON)));
                    this.mCurrentPathFromFolderPicker = savedInstanceState.getString(STATE_SUB_FILTER_CurrentPath, this.mCurrentPathFromFolderPicker);
                    this.mCurrentTagsFromPicker = new ArrayList<>(ListUtils.fromString(savedInstanceState.getString(STATE_SUB_FILTER_TAGS, ListUtils.toString(mCurrentTagsFromPicker))));
                }
                this.mDirQueryID = savedInstanceState.getInt(STATE_DirQueryID, this.getDirQueryID());
                this.mCurrentSortID = savedInstanceState.getInt(STATE_SortID, this.mCurrentSortID);
                this.mCurrentSortAscending = savedInstanceState.getBoolean(STATE_SortAscending, this.mCurrentSortAscending);
                filter = savedInstanceState.getString(STATE_Filter);
                if ((filter != null) && (dbgFilter != null)) dbgFilter.append("filter from savedInstanceState=").append(filter).append("\n");

                this.mCurrentSubFilterMode = savedInstanceState.getInt(STATE_SUB_FILTR_MODE, this.mCurrentSubFilterMode);
            }

            if ((pathFilter == null) && (filter == null) && (this.getCurrentFilterSettings() == null)) {
                filter = sharedPref.getString(STATE_Filter + mStatSuffix, null);
                if ((filter != null) && (dbgFilter != null)) dbgFilter.append("filter from sharedPref=").append(filter).append("\n");
            }

            if (filter != null) {
                this.setCurrentFilterSettings(GalleryFilterParameter.parse(filter, new GalleryFilterParameter()));
            } else if (pathFilter != null) {
                if (!pathFilter.endsWith("%")) pathFilter += "%";
                this.setCurrentFilterSettings(new GalleryFilterParameter().setPath(pathFilter));
            }

            // extra parameter
            final String sqlString = intent.getStringExtra(EXTRA_QUERY);
            if (sqlString != null) {
                if (dbgFilter != null) dbgFilter.append("query from ").append(EXTRA_QUERY).append("\n\t").append(sqlString).append("\n");
                this.mGalleryContentQuery = QueryParameter.parse(sqlString);
                setHasUserDefinedQuery(true);
            }

            if (this.mGalleryContentQuery == null) this.mGalleryContentQuery = FotoSql.getQuery(FotoSql.QUERY_TYPE_DEFAULT);

            if (dbgFilter != null)  {
                Log.i(Global.LOG_CONTEXT, mDebugPrefix + dbgFilter.toString());
            }
        }

        public boolean hasUserDefinedQuery() {
            return mHasUserDefinedQuery;
        }

        public void setHasUserDefinedQuery(boolean mHasUserDefinedQuery) {
            this.mHasUserDefinedQuery = mHasUserDefinedQuery;
        }

        public IGalleryFilter getCurrentFilterSettings() {
            return mCurrentFilterSettings;
        }

        public void setCurrentFilterSettings(IGalleryFilter newFilterSettings) {
            if ((newFilterSettings != null) && (PICK_GEO_SUFFIX.compareTo(mStatSuffix) == 0)) {
                // geopick: only photos that have lat/lon
                GalleryFilterParameter parameter = new GalleryFilterParameter().get(newFilterSettings);
                parameter.setNonGeoOnly(false);
                if (parameter.isEmpty()) {
                    parameter.setLatitude(-90.0,90.0).setLogitude(-180.0,180.0);
                }
                this.mCurrentFilterSettings = parameter;
            } else {
                this.mCurrentFilterSettings = newFilterSettings;
            }
        }
    }

    private BookmarkController mBookmarkController = null;

    /**
     * shows a new instance of FotoGalleryActivity.
     *
     * @param context calling activity
     * @param filter if != null set initial filter to new FotoGalleryActivity
     * @param query if != null set initial filter to new FotoGalleryActivity
     * @param requestCode if != 0 start for result. else start without result
     */
    public static void showActivity(Activity context, GalleryFilterParameter filter, QueryParameter query, int requestCode) {
        Intent intent = new Intent(context, FotoGalleryActivity.class);

        if (filter != null) {
            intent.putExtra(EXTRA_FILTER, filter.toString());
        }

        if (query != null) {
            intent.putExtra(EXTRA_QUERY, query.toReParseableString());
        }

        if (requestCode != 0) {
            context.startActivityForResult(intent, requestCode);
        } else {
            context.startActivity(intent);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        this.mGalleryQueryParameter.saveInstanceState(this, savedInstanceState);
        mBookmarkController.saveState(null, savedInstanceState);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        Global.debugMemory(mDebugPrefix, "onCreate");
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();

        if (BuildConfig.DEBUG) {
            // not implemented yet
            FotoLibGlobal.itpcWriteSupport = false;
        }
        if (Global.debugEnabled && (intent != null)){
            Log.d(Global.LOG_CONTEXT, mDebugPrefix + "onCreate " + intent.toUri(Intent.URI_INTENT_SCHEME));
        }

        this.getContentResolver().registerContentObserver(FotoSql.SQL_TABLE_EXTERNAL_CONTENT_URI, true, mMediaObserverDirectory);
        this.getContentResolver().registerContentObserver(FotoSql.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE, true, mMediaObserverDirectory);
        setContentView(R.layout.activity_gallery); // .gallery_activity);

        this.mGalleryQueryParameter.loadSettingsAndInstanceState(this, savedInstanceState);

        mBookmarkController = new BookmarkController(this);
        mBookmarkController.loadState(intent,savedInstanceState);

        if (this.mGalleryQueryParameter.isGeoPick()) {
            // #76: load predefined bookmark file
            this.mBookmarkController.onLoadFromAnswer(DEFAULT_BOOKMARKNAME_PICK_GEO, this.mLoadBookmarkResultConsumer);
        }

        FragmentManager fragmentManager = getFragmentManager();
        mGalleryGui = (Queryable) fragmentManager.findFragmentById(R.id.galleryCursor);

        if (mGalleryGui instanceof GalleryCursorFragment) {
            this.mSelectedItems = ((GalleryCursorFragment) mGalleryGui).getSelectedItems();
        }

        // on tablet seperate dir navigator fragment
        mDirGui = (DirectoryGui) fragmentManager.findFragmentById(R.id.directoryFragment);

        if (FotoViewerParameter.galleryHasEmbeddedDirPicker) {
            if (mDirGui == null) {
                // on small screen/cellphone DirectoryGui is part of gallery
                mDirGui = (DirectoryGui) fragmentManager.findFragmentById(R.id.galleryCursor);
            } else {
                mHasEmbeddedDirPicker = true;
            }
        } else {
            if (mDirGui != null) {
                fragmentManager.beginTransaction().remove((Fragment) mDirGui).commit();
                mDirGui = null;
            }
        }

        setTitle();
        reloadGui("onCreate");
    }

    @Override
    protected void onPause () {
        Global.debugMemory(mDebugPrefix, "onPause");
        this.mGalleryQueryParameter.saveSettings(this);
        super.onPause();
    }

    @Override
    protected void onResume () {
        Global.debugMemory(mDebugPrefix, "onResume");
        super.onResume();
    }

    @Override public void onLowMemory() {
        super.onLowMemory();
        invalidateDirectories(mDebugPrefix + "#onLowMemory");
    }

    @Override
    protected void onDestroy() {
        Global.debugMemory(mDebugPrefix, "onDestroy start");
        super.onDestroy();
        this.getContentResolver().unregisterContentObserver(mMediaObserverDirectory);
        mPopUpSelection = null;
        // to avoid memory leaks
        GarbageCollector.freeMemory(findViewById(R.id.root_view));

        this.mGalleryQueryParameter.mGalleryContentQuery = null;
        mGalleryGui = null;
        mDirGui = null;
        invalidateDirectories(mDebugPrefix + "#onDestroy");

        System.gc();
        Global.debugMemory(mDebugPrefix, "onDestroy end");
        // RefWatcher refWatcher = AndroFotoFinderApp.getRefWatcher(this);
        // refWatcher.watch(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Intent intent = this.getIntent();
        String action = (intent != null) ? intent.getAction() : null;
        if ((action == null) || (((Intent.ACTION_PICK.compareTo(action) != 0) && (Intent.ACTION_GET_CONTENT.compareTo(action) != 0)))) {
            MenuInflater inflater = getMenuInflater();

            inflater.inflate(R.menu.menu_gallery_non_selected_only, menu);
            inflater.inflate(R.menu.menu_gallery_non_multiselect, menu);
            /*
            getActionBar().setListNavigationCallbacks();
            MenuItem sorter = menu.getItem(R.id.cmd_sort);
            sorter.getSubMenu().
            */
            Global.fixMenu(this, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem sorter = menu.findItem(R.id.cmd_sort);

        if (sorter != null) {
            String sortTitle = getString(R.string.sort_menu_title) +
                    ": " +
                    mGalleryQueryParameter.getSortDisplayName(this);
            sorter.setTitle(sortTitle);
        }
        AboutDialogPreference.onPrepareOptionsMenu(this, menu);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.cmd_select_folder:
                openFolderPicker();
                return true;

            case R.id.cmd_select_lat_lon:
                openLatLonPicker();
                return true;
            case R.id.cmd_select_tag:
                openTagPicker();
                return true;
            case R.id.cmd_filter:
                openFilter();
                return true;
            case R.id.cmd_load_bookmark:
                loadBookmark();
                return true;
            case R.id.cmd_sort_date:
                this.mGalleryQueryParameter.setSortID(FotoSql.SORT_BY_DATE);
                reloadGui("sort date");
                return true;
            case R.id.cmd_sort_directory:
                this.mGalleryQueryParameter.setSortID(FotoSql.SORT_BY_NAME);
                reloadGui("sort dir");
                return true;
            case R.id.cmd_sort_path_len:
                this.mGalleryQueryParameter.setSortID(FotoSql.SORT_BY_NAME_LEN);
                reloadGui("sort len");
                return true;
            case R.id.cmd_sort_file_len:
                this.mGalleryQueryParameter.setSortID(FotoSql.SORT_BY_FILE_LEN);
                reloadGui("sort size");
                return true;

            case R.id.cmd_sort_width:
                this.mGalleryQueryParameter.setSortID(FotoSql.SORT_BY_WIDTH);
                reloadGui("sort width");
                return true;

            case R.id.cmd_sort_location:
                this.mGalleryQueryParameter.setSortID(FotoSql.SORT_BY_LOCATION);
                reloadGui("sort geo");
                return true;

            case R.id.cmd_sort_rating:
                this.mGalleryQueryParameter.setSortID(FotoSql.SORT_BY_RATING);
                reloadGui("sort rating");
                return true;

            case R.id.cmd_sort_modification:
                this.mGalleryQueryParameter.setSortID(FotoSql.SORT_BY_MODIFICATION);
                reloadGui("sort modification");
                return true;


            case R.id.cmd_settings:
                SettingsActivity.show(this);
                return true;
            case R.id.cmd_about:
                AboutDialogPreference.createAboutDialog(this).show();
                return true;
            case R.id.cmd_more:
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        // reopen after some delay
                        openOptionsMenu();
                    }
                }, 200);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void loadBookmark() {
        mBookmarkController.onLoadFromQuestion(mLoadBookmarkResultConsumer, this.mGalleryQueryParameter.calculateEffectiveGalleryContentQuery());
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
        if (mGalleryGui instanceof Fragment) {
            ((Fragment) mGalleryGui).onActivityResult(requestCode, resultCode, intent);
        }

        if (mDirPicker instanceof Fragment) {
            ((Fragment) mDirPicker).onActivityResult(requestCode, resultCode, intent);
        }

        if (mPopUpSelection != null) mPopUpSelection.refresh();

        switch (requestCode) {
            case GalleryFilterActivity.resultID :
                if (BookmarkController.isReset(intent)) {
                    mGalleryQueryParameter.mGalleryContentQuery = new QueryParameter(FotoSql.queryDetail);
                }
                mBookmarkController.loadState(intent, null);
                onFilterChanged(GalleryFilterActivity.getFilter(intent), mDebugPrefix + "#onActivityResult from GalleryFilterActivity");
                break;
            case ImageDetailActivityViewPager.ACTIVITY_ID:
                if (resultCode == ImageDetailActivityViewPager.RESULT_CHANGE) {
                    invalidateDirectories(mDebugPrefix + "#onActivityResult from ImageDetailActivityViewPager");
                }
                break;
            case GeoEditActivity.RESULT_ID:
                if (resultCode == ImageDetailActivityViewPager.RESULT_CHANGE) {
                    invalidateDirectories(mDebugPrefix + "#onActivityResult from GeoEditActivity");
                }
                break;
            default:break;
        }
    }

    private void onFilterChanged(IGalleryFilter filter, String why) {
        if (filter != null) {
            this.mGalleryQueryParameter.setCurrentFilterSettings(filter);
            this.mGalleryQueryParameter.setHasUserDefinedQuery(false);

            invalidateDirectories(mDebugPrefix + "#filter changed " + why);

            reloadGui("filter changed");
        }
    }

    private void openLatLonPicker() {
        mGalleryQueryParameter.mCurrentSubFilterMode = GalleryQueryParameter.SUB_FILTER_MODE_GEO;

        final FragmentManager manager = getFragmentManager();
        LocationMapFragment dialog = new LocationMapFragment();
        dialog.defineNavigation(this.mGalleryQueryParameter.getCurrentFilterSettings(),
                this.mGalleryQueryParameter.mCurrentLatLonFromGeoAreaPicker, OsmdroidUtil.NO_ZOOM, mSelectedItems, null);

        dialog.show(manager, DLG_NAVIGATOR_TAG);
    }

    private void openTagPicker() {
        mGalleryQueryParameter.mCurrentSubFilterMode = GalleryQueryParameter.SUB_FILTER_MODE_TAG;

        final FragmentManager manager = getFragmentManager();
        TagsPickerFragment dlg = new TagsPickerFragment();
        dlg.setFragmentOnwner(this);
        dlg.setTitleId(R.string.tags_activity_title);
        dlg.setAddNames(mGalleryQueryParameter.mCurrentTagsFromPicker);
        dlg.show(manager, DLG_NAVIGATOR_TAG);
    }

    /** called by {@link TagsPickerFragment} */
    @Override
    public boolean onCancel(String msg) {
        return true;
    }

    /** called by {@link TagsPickerFragment} */
    @Override
    public boolean onOk(List<String> addNames, List<String> removeNames) {
        Log.d(Global.LOG_CONTEXT, "FotoGalleryActivity.navigateTo " + ListUtils.toString(addNames) + " from "
                + ListUtils.toString(mGalleryQueryParameter.mCurrentTagsFromPicker));
        mGalleryQueryParameter.mCurrentTagsFromPicker = new ArrayList<String>(addNames);
        reloadGui("navigate to tags");
        return true;
    }

    /** called by {@link TagsPickerFragment} */
    @Override
    public boolean onTagPopUpClick(int menuItemItemId, Tag selectedTag) {
        return TagsPickerFragment.handleMenuShow(menuItemItemId, selectedTag, this, this.mGalleryQueryParameter.getCurrentFilterSettings());
    }

    private void openFolderPicker() {
        mGalleryQueryParameter.mCurrentSubFilterMode = GalleryQueryParameter.SUB_FILTER_MODE_PATH;

        int dirQueryID = this.mGalleryQueryParameter.getDirQueryID();

        /** if wrong datatype was saved: gallery is not allowed for dirPicker */
        if (FotoSql.QUERY_TYPE_GALLERY == dirQueryID) {
            dirQueryID = FotoSql.QUERY_TYPE_GROUP_ALBUM;
        }

        if (mDirectoryRoot == null) {
            // not loaded yet. load directoryRoot in background
            final QueryParameter currentDirContentQuery = new QueryParameter(FotoSql.getQuery(dirQueryID));
            TagSql.filter2QueryEx(currentDirContentQuery, this.mGalleryQueryParameter.getCurrentFilterSettings(),
                    this.mGalleryQueryParameter.getSortID() != IGalleryFilter.SORT_BY_NONE);

            this.mGalleryQueryParameter.mDirQueryID = (currentDirContentQuery != null) ? currentDirContentQuery.getID() : FotoSql.QUERY_TYPE_UNDEFINED;

            if (currentDirContentQuery != null) {
                this.mMustShowNavigator = true;
                DirectoryLoaderTask loader = new DirectoryLoaderTask(this, mDebugPrefix) {
                    @Override
                    protected void onPostExecute(IDirectory directoryRoot) {
                        onDirectoryDataLoadComplete(directoryRoot);
                    }
                };
                loader.execute(currentDirContentQuery);
            } else {
                Log.e(Global.LOG_CONTEXT, mDebugPrefix + " this.mDirQueryID undefined "
                        + this.mGalleryQueryParameter.mDirQueryID);
            }
        } else {
            this.mMustShowNavigator = false;
            final FragmentManager manager = getFragmentManager();
            DirectoryPickerFragment dirDialog =new DirectoryPickerFragment() {
                protected boolean onPopUpClick(MenuItem menuItem, IDirectory popUpSelection) {
                    mPopUpSelection = popUpSelection;
                    return super.onPopUpClick(menuItem, popUpSelection);
                }
            };

            // (DirectoryPickerFragment) manager.findFragmentByTag(DLG_NAVIGATOR_TAG);
            dirDialog.setContextMenuId(LockScreen.isLocked(this) ? 0 :  R.menu.menu_context_dirpicker);

            dirDialog.defineDirectoryNavigation(mDirectoryRoot, dirQueryID,
                    this.mGalleryQueryParameter.mCurrentPathFromFolderPicker);

            mDirPicker = dirDialog;
            dirDialog.show(manager, DLG_NAVIGATOR_TAG);
        }
    }

    private void openFilter() {
        GalleryFilterActivity.showActivity(this,
                this.mGalleryQueryParameter.getCurrentFilterSettings(),
                this.mGalleryQueryParameter.mGalleryContentQuery,
                mBookmarkController.getlastBookmarkFileName(), GalleryFilterActivity.resultID);
    }

    /** called by Fragment: a fragment Item was clicked */
    @Override
    public void onGalleryImageClick(long imageId, Uri imageUri, int position) {
        Global.debugMemory(mDebugPrefix, "onGalleryImageClick");
        QueryParameter imageDetailQuery = this.mGalleryQueryParameter.calculateEffectiveGalleryContentQuery();
        ImageDetailActivityViewPager.showActivity(this, imageUri, position, imageDetailQuery, ImageDetailActivityViewPager.ACTIVITY_ID);
    }

    /** GalleryFragment tells the Owning Activity that querying data has finisched */
    @Override
    public void setResultCount(int count) {
        this.mTitleResultCount = (count > 0) ? ("(" + count + ")") : "";
        setTitle();

        // current path does not contain photo => refreshLocal witout current path
        if ((count == 0) &&(mGalleryQueryParameter.clearPathIfActive())) {
            setTitle();
            reloadGui("query changed");
        }
    }

    /**
     * called when user selects a new directoryRoot
     */
    @Override
    public void onDirectoryPick(String selectedAbsolutePath, int queryTypeId) {
        if (!this.mHasEmbeddedDirPicker) {
            navigateTo(selectedAbsolutePath, queryTypeId);
        }
        mDirPicker = null;
    }

    @Override
    public void invalidateDirectories(String why) {

        if (mDirectoryRoot != null) {
            if (Global.debugEnabled) {
                StringBuilder name = new StringBuilder(mDirectoryRoot.getAbsolute());
                Directory.appendCount(name, mDirectoryRoot, Directory.OPT_DIR | Directory.OPT_SUB_DIR);
                Log.i(Global.LOG_CONTEXT, mDebugPrefix + "invalidateDirectories(" + name + ") because of " + why);
            }
            if (mDirPicker == null) {
                mDirectoryRoot.destroy();
                mDirectoryRoot = null; // must refreshLocal next time
            }
        }
    }

    /**
     * called when user cancels selection of a new directoryRoot
     */
    @Override
    public void onDirectoryCancel(int queryTypeId) {
        mDirPicker = null;
    }

    /** called after the selection in tree has changed */
    @Override
    public void onDirectorySelectionChanged(String selectedAbsolutePath, int queryTypeId) {
        if (this.mHasEmbeddedDirPicker) {
            navigateTo(selectedAbsolutePath, queryTypeId);
        }
    }

    private void navigateTo(String selectedAbsolutePath, int queryTypeId) {

        if (selectedAbsolutePath != null) {
            if (mGalleryQueryParameter.mCurrentSubFilterMode == GalleryQueryParameter.SUB_FILTER_MODE_GEO) {
                Log.d(Global.LOG_CONTEXT, "FotoGalleryActivity.navigateTo " + selectedAbsolutePath + " from " + mGalleryQueryParameter.mCurrentLatLonFromGeoAreaPicker);
                this.mGalleryQueryParameter.mCurrentLatLonFromGeoAreaPicker.get(DirectoryFormatter.parseLatLon(selectedAbsolutePath));

                reloadGui("navigate to geo");

            } else if (mGalleryQueryParameter.mCurrentSubFilterMode == GalleryQueryParameter.SUB_FILTER_MODE_TAG) {
                Log.d(Global.LOG_CONTEXT, "FotoGalleryActivity.navigateTo " + selectedAbsolutePath + " from "
                        + ListUtils.toString(this.mGalleryQueryParameter.mCurrentTagsFromPicker));
                this.mGalleryQueryParameter.mCurrentTagsFromPicker = new ArrayList<>(ListUtils.fromString(selectedAbsolutePath));
                reloadGui("navigate to tags");
            } else if (mGalleryQueryParameter.mCurrentSubFilterMode == GalleryQueryParameter.SUB_FILTER_MODE_PATH) {
                Log.d(Global.LOG_CONTEXT, "FotoGalleryActivity.navigateTo " + selectedAbsolutePath + " from " + this.mGalleryQueryParameter.mCurrentPathFromFolderPicker);
                this.mGalleryQueryParameter.mCurrentPathFromFolderPicker = selectedAbsolutePath;
                this.mGalleryQueryParameter.mDirQueryID = queryTypeId;
                setTitle();

                reloadGui("navigate to dir");
            }
        }
    }

    private void reloadGui(String why) {
        if (mGalleryGui != null) {
            QueryParameter query = this.mGalleryQueryParameter.calculateEffectiveGalleryContentQuery();
            if (query != null) {
                this.mGalleryGui.requery(this, query, mDebugPrefix + why);
            }
        }

        if (mDirGui != null) {
            String currentPath = this.mGalleryQueryParameter.mCurrentPathFromFolderPicker;
            if (currentPath != null) {
                mDirGui.navigateTo(currentPath);
            }
        }
    }

    private void onDirectoryDataLoadComplete(IDirectory directoryRoot) {
        if (directoryRoot == null) {
            final String message = getString(R.string.folder_err_load_failed_format, FotoSql.getName(this, this.mGalleryQueryParameter.getDirQueryID()));
            Toast.makeText(this, message,Toast.LENGTH_LONG).show();
        } else {
            mDirectoryRoot = directoryRoot;
            final boolean mustDefineNavigation = (mDirGui != null) && (this.mGalleryQueryParameter.mCurrentPathFromFolderPicker != null);
            final boolean mustShowFolderPicker = (mDirectoryRoot != null) && (this.mMustShowNavigator);

            if (Global.debugEnabled) {
                StringBuilder name = new StringBuilder(mDirectoryRoot.getAbsolute());
                Directory.appendCount(name, mDirectoryRoot, Directory.OPT_DIR | Directory.OPT_SUB_DIR);
                Log.i(Global.LOG_CONTEXT, mDebugPrefix + "onDirectoryDataLoadComplete(" +
                        "mustDefineNavigation=" + mustDefineNavigation +
                        ", mustShowFolderPicker=" + mustShowFolderPicker +
                        ", content=" + name + ")");
            }

            if (mustDefineNavigation) {
                mDirGui.defineDirectoryNavigation(directoryRoot, this.mGalleryQueryParameter.getDirQueryID(), this.mGalleryQueryParameter.mCurrentPathFromFolderPicker);
            }
            Global.debugMemory(mDebugPrefix, "onDirectoryDataLoadComplete");

            if (mustShowFolderPicker) {
                openFolderPicker();
            }
        }
    }

    private void setTitle() {
        Intent intent = getIntent();
        String title = (intent == null) ? null : intent.getStringExtra(EXTRA_TITLE);

        if (title == null) {
            if (mGalleryQueryParameter.mCurrentSubFilterMode == GalleryQueryParameter.SUB_FILTER_MODE_GEO) {
                title = getString(R.string.gallery_title);
            } else if (this.mGalleryQueryParameter.mCurrentPathFromFolderPicker != null) {
                title = FotoSql.getName(this, this.mGalleryQueryParameter.getDirQueryID())
                        + " - " + this.mGalleryQueryParameter.mCurrentPathFromFolderPicker;
            } else {
                title = FotoSql.getName(this, this.mGalleryQueryParameter.getDirQueryID());
            }
        }
        if (title != null) {
            this.setTitle(title + mTitleResultCount);
        }
    }

    @Override
    public String toString() {
        return mDebugPrefix + "->" + this.mGalleryGui;
    }
}

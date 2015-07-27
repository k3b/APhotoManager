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
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

// import com.squareup.leakcanary.RefWatcher;

import de.k3b.android.androFotoFinder.directory.DirectoryGui;
import de.k3b.android.androFotoFinder.directory.DirectoryLoaderTask;
import de.k3b.android.androFotoFinder.imagedetail.ImageDetailActivityViewPager;
import de.k3b.android.androFotoFinder.locationmap.LocationMapFragment;
import de.k3b.android.androFotoFinder.queries.GalleryFilterParameterParcelable;
import de.k3b.android.androFotoFinder.queries.QueryParameterParcelable;
import de.k3b.android.androFotoFinder.directory.DirectoryPickerFragment;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.queries.FotoViewerParameter;
import de.k3b.android.androFotoFinder.queries.Queryable;
import de.k3b.android.util.GarbageCollector;
import de.k3b.io.Directory;
import de.k3b.io.DirectoryFormatter;
import de.k3b.io.GeoRectangle;

public class FotoGalleryActivity extends Activity implements
        OnGalleryInteractionListener, DirectoryPickerFragment.OnDirectoryInteractionListener,
        LocationMapFragment.OnDirectoryInteractionListener
{
    private static final String debugPrefix = "GalA-";

    /** intent parameters supported by FotoGalleryActivity: EXTRA_... */
    public static final String EXTRA_QUERY = "gallery";
    public static final String EXTRA_TITLE = Intent.EXTRA_TITLE;

    private static final String DLG_NAVIGATOR_TAG = "navigator";

    private static class GalleryQueryParameter {
        private static final String STATE_CurrentPath = "CurrentPath";
        private static final String STATE_DirQueryID = "DirQueryID";
        private static final String STATE_SortID = "SortID";
        private static final String STATE_SortAscending = "SortAscending";
        private static final String STATE_Filter = "mFilter";
        private static final String STATE_LAT_LON = "currentLatLon";
        private static final String STATE_LAT_LON_ACTIVE = "currentLatLonActive";

        /** true use latLonPicker; false use directoryPicker */
        private boolean mUseLatLon = false;
        private GeoRectangle mCurrentLatLon = new GeoRectangle();

        /** one of the FotoSql.QUERY_TYPE_xxx values */
        int mDirQueryID = FotoSql.QUERY_TYPE_GROUP_DEFAULT;

        private int mSortID = FotoSql.SORT_BY_DEFAULT;
        private boolean mSortAscending = true;

        private String mCurrentPath = "/";

        QueryParameterParcelable mGalleryContentQuery = null;

        GalleryFilterParameterParcelable mFilter;

        /** one of the FotoSql.QUERY_TYPE_xxx values. if undefined use default */
        private int getDirQueryID() {
            if (this.mDirQueryID == FotoSql.QUERY_TYPE_UNDEFINED)
                return FotoSql.QUERY_TYPE_GROUP_DEFAULT;

            return this.mDirQueryID;
        }

        public void setSortID(int sortID) {
            if (sortID == mSortID) {
                mSortAscending = !mSortAscending;
            } else {
                mSortAscending = true;
                mSortID = sortID;
            }
        }

        public String getSortDisplayName(Context context) {
            return  FotoSql.getName(context, this.mSortID) + ((mSortAscending) ? " ^" : " V");
        }

        /** combine root-query plus current selected directoryRoot */
        private QueryParameterParcelable calculateEffectiveGalleryContentQuery() {
            if (this.mGalleryContentQuery == null) return null;
            QueryParameterParcelable result = new QueryParameterParcelable(this.mGalleryContentQuery);

            FotoSql.setWhereFilter(result, this.mFilter);
            if (result == null) return null;

            if (mUseLatLon) {
                FotoSql.addWhereFilteLatLon(result, mCurrentLatLon);
            } else if (this.mCurrentPath != null) {
                FotoSql.addPathWhere(result, this.mCurrentPath, this.getDirQueryID());
            }

            FotoSql.setSort(result, mSortID, mSortAscending);
            return result;
        }

        private void saveInstanceState(Context context, Bundle savedInstanceState) {
            saveSettings(context);

            // save InstanceState
            savedInstanceState.putInt(STATE_DirQueryID, this.getDirQueryID());
            savedInstanceState.putString(STATE_CurrentPath, this.mCurrentPath);
            savedInstanceState.putInt(STATE_SortID, this.mSortID);
            savedInstanceState.putBoolean(STATE_SortAscending, this.mSortAscending);
            savedInstanceState.putParcelable(STATE_Filter, this.mFilter);
            savedInstanceState.putString(STATE_LAT_LON, this.mCurrentLatLon.toString());
            savedInstanceState.putBoolean(STATE_LAT_LON_ACTIVE, this.mUseLatLon);

        }

        private void saveSettings(Context context) {
            // save settings
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor edit = sharedPref.edit();

            edit.putInt(STATE_DirQueryID, this.getDirQueryID());
            edit.putString(STATE_CurrentPath, this.mCurrentPath);
            edit.putInt(STATE_SortID, this.mSortID);
            edit.putBoolean(STATE_SortAscending, this.mSortAscending);
            edit.putString(STATE_LAT_LON, this.mCurrentLatLon.toString());


            mFilter.saveSettings(edit);

            // edit.putParcelable(STATE_Filter, this.mFilter);
            edit.commit();
        }

        // load from settings/instanceState
        private void loadSettingsAndInstanceState(Activity context, Bundle savedInstanceState) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            this.mCurrentPath = sharedPref.getString(STATE_CurrentPath, this.mCurrentPath);
            this.mDirQueryID = sharedPref.getInt(STATE_DirQueryID, this.getDirQueryID());
            this.mSortID = sharedPref.getInt(STATE_SortID, this.mSortID);
            this.mSortAscending = sharedPref.getBoolean(STATE_SortAscending, this.mSortAscending);
            this.mCurrentLatLon.get(DirectoryFormatter.parseLatLon(sharedPref.getString(STATE_LAT_LON, null)));

            // instance state overrides settings
            if (savedInstanceState != null) {
                this.mCurrentPath = savedInstanceState.getString(STATE_CurrentPath, this.mCurrentPath);
                this.mDirQueryID = savedInstanceState.getInt(STATE_DirQueryID, this.getDirQueryID());
                this.mSortID = savedInstanceState.getInt(STATE_SortID, this.mSortID);
                this.mSortAscending = savedInstanceState.getBoolean(STATE_SortAscending, this.mSortAscending);
                this.mFilter = savedInstanceState.getParcelable(STATE_Filter);
                this.mCurrentLatLon.get(DirectoryFormatter.parseLatLon(savedInstanceState.getString(STATE_LAT_LON)));

                this.mUseLatLon = savedInstanceState.getBoolean(STATE_LAT_LON_ACTIVE, this.mUseLatLon);
            }

            if (this.mFilter == null) {
                this.mFilter = new GalleryFilterParameterParcelable();
                mFilter.loadSettings(sharedPref);
            }
            // extra parameter
            this.mGalleryContentQuery = context.getIntent().getParcelableExtra(EXTRA_QUERY);
            if (this.mGalleryContentQuery == null) this.mGalleryContentQuery = FotoSql.getQuery(FotoSql.QUERY_TYPE_DEFAULT);
        }
    }

    private GalleryQueryParameter galleryQueryParameter = new GalleryQueryParameter();

    private Queryable mGalleryGui;

    private boolean mHasEmbeddedDirPicker = false;
    private DirectoryGui mDirGui;

    private String mTitleResultCount = "";

    private Directory mDirectoryRoot = null;

    /** true if activity should show navigator dialog after loading mDirectoryRoot is complete */
    private boolean mMustShowNavigator = false;

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        this.galleryQueryParameter.saveInstanceState(this, savedInstanceState);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Global.debugMemory(debugPrefix, "onCreate");
        super.onCreate(savedInstanceState);
		
        setContentView(R.layout.activity_gallery); // .gallery_activity);

        this.galleryQueryParameter.loadSettingsAndInstanceState(this, savedInstanceState);

        FragmentManager fragmentManager = getFragmentManager();
        mGalleryGui = (Queryable) fragmentManager.findFragmentById(R.id.galleryCursor);

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
        reloadGui();
    }

    @Override
    protected void onPause () {
        Global.debugMemory(debugPrefix, "onPause");
        this.galleryQueryParameter.saveSettings(this);
        super.onPause();
    }

    @Override
    protected void onResume () {
        Global.debugMemory(debugPrefix, "onResume");
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        Global.debugMemory(debugPrefix, "onDestroy start");
        super.onDestroy();

        // to avoid memory leaks
        GarbageCollector.freeMemory(findViewById(R.id.root_view));

        this.galleryQueryParameter.mGalleryContentQuery = null;
        mGalleryGui = null;
        mDirGui = null;

        if (mDirectoryRoot != null)
        {
            mDirectoryRoot.destroy();
            mDirectoryRoot = null;
        }
        System.gc();
        Global.debugMemory(debugPrefix, "onDestroy end");
        // RefWatcher refWatcher = AndroFotoFinderApp.getRefWatcher(this);
        // refWatcher.watch(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_gallery, menu);
        /*
        getActionBar().setListNavigationCallbacks();
        MenuItem sorter = menu.getItem(R.id.cmd_sort);
        sorter.getSubMenu().
        */
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem sorter = menu.findItem(R.id.cmd_sort);

        if (sorter != null) {
            StringBuilder sortTitle = new StringBuilder();
            sortTitle.append(getString(R.string.action_sort_title))
                    .append(": ")
                    .append(galleryQueryParameter.getSortDisplayName(this));
            sorter.setTitle(sortTitle.toString());
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.cmd_navigator:
                openNavigator();
                return true;

            case R.id.cmd_lat_lon:
                openLatLonPicker();
                return true;
            case R.id.cmd_filter:
                openFilter();
                return true;
            case R.id.cmd_sort:
                openSort();
                return true;
            case R.id.cmd_sort_date:
                this.galleryQueryParameter.setSortID(FotoSql.SORT_BY_DATE);
                reloadGui();
                return true;
            case R.id.cmd_sort_directory:
                this.galleryQueryParameter.setSortID(FotoSql.SORT_BY_NAME);
                reloadGui();
                return true;
            case R.id.cmd_sort_len:
                this.galleryQueryParameter.setSortID(FotoSql.SORT_BY_NAME_LEN);
                reloadGui();
                return true;
            case R.id.cmd_sort_location:
                this.galleryQueryParameter.setSortID(FotoSql.SORT_BY_LOCATION);
                reloadGui();
                return true;
            case R.id.action_settings:
                openSettings();
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

    private void onFilterChanged(GalleryFilterParameterParcelable filter) {
        if (filter != null) {
            this.galleryQueryParameter.mFilter = filter;

            if (mDirectoryRoot != null) mDirectoryRoot.destroy();
            mDirectoryRoot = null; // must reload next time

            reloadGui();
        }
    }

    private void openLatLonPicker() {
        galleryQueryParameter.mUseLatLon = true;

        final FragmentManager manager = getFragmentManager();
        LocationMapFragment dirDialog = new LocationMapFragment();
        dirDialog.defineNavigation(this.galleryQueryParameter.mFilter, this.galleryQueryParameter.mCurrentLatLon, FotoSql.QUERY_TYPE_GROUP_PLACE_MAP);

        dirDialog.show(manager, DLG_NAVIGATOR_TAG);
    }


    private void openNavigator() {
        galleryQueryParameter.mUseLatLon = false;

        int dirQueryID = this.galleryQueryParameter.getDirQueryID();

        /** if wrong datatype was saved: gallery is not allowed for dirPicker */
        if (FotoSql.QUERY_TYPE_GALLERY == dirQueryID) {
            dirQueryID = FotoSql.QUERY_TYPE_GROUP_ALBUM;
        }

        if (mDirectoryRoot == null) {
            // not loaded yet. load directoryRoot in background
            final QueryParameterParcelable currentDirContentQuery = new QueryParameterParcelable(FotoSql.getQuery(dirQueryID));
            FotoSql.setWhereFilter(currentDirContentQuery, this.galleryQueryParameter.mFilter);

            this.galleryQueryParameter.mDirQueryID = (currentDirContentQuery != null) ? currentDirContentQuery.getID() : FotoSql.QUERY_TYPE_UNDEFINED;

            if (currentDirContentQuery != null) {
                this.mMustShowNavigator = true;
                DirectoryLoaderTask loader = new DirectoryLoaderTask(this, debugPrefix) {
                    protected void onPostExecute(Directory directoryRoot) {
                        onDirectoryDataLoadComplete(directoryRoot);
                    }
                };
                loader.execute(currentDirContentQuery);
            } else {
                Log.e(Global.LOG_CONTEXT, debugPrefix + " this.mDirQueryID undefined " + this.galleryQueryParameter.mDirQueryID);
            }
        } else {
            this.mMustShowNavigator = false;
            final FragmentManager manager = getFragmentManager();
            DirectoryPickerFragment dirDialog = new DirectoryPickerFragment(); // (DirectoryPickerFragment) manager.findFragmentByTag(DLG_NAVIGATOR_TAG);
            dirDialog.defineDirectoryNavigation(mDirectoryRoot, dirQueryID, this.galleryQueryParameter.mCurrentPath);

            dirDialog.show(manager, DLG_NAVIGATOR_TAG);
        }
    }

    private void openFilter() {
        GalleryFilterActivity.showActivity(this, this.galleryQueryParameter.mFilter);
    }

    private void openSort() {
    }

    private void openSettings() {
//        Intent intent = new Intent(this, SettingsActivity.class);;
//        startActivity(intent);
    }

    /** called by Fragment: a fragment Item was clicked */
    @Override
    public void onGalleryImageClick(long imageId, Uri imageUri, int position) {
        Global.debugMemory(debugPrefix, "onGalleryImageClick");
        Intent intent;
        //Create intent
        intent = new Intent(this, ImageDetailActivityViewPager.class);

        intent.putExtra(ImageDetailActivityViewPager.EXTRA_QUERY, this.galleryQueryParameter.calculateEffectiveGalleryContentQuery() );
        intent.putExtra(ImageDetailActivityViewPager.EXTRA_POSITION, position);
        intent.setData(imageUri);

        startActivity(intent);
    }

    /** GalleryFragment tells the Owning Activity that querying data has finisched */
    @Override
    public void setResultCount(int count) {
        this.mTitleResultCount = (count > 0) ? ("(" + count + ")") : "";
        setTitle();
    }

    /**
     * called when user selects a new directoryRoot
     *
     * @param selectedAbsolutePath
     * @param queryTypeId
     */
    @Override
    public void onDirectoryPick(String selectedAbsolutePath, int queryTypeId) {
        if (!this.mHasEmbeddedDirPicker) {
            navigateTo(selectedAbsolutePath, queryTypeId);
        }
    }

    /**
     * called when user cancels selection of a new directoryRoot
     * @param queryTypeId
     */
    @Override
    public void onDirectoryCancel(int queryTypeId) {
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
            if (galleryQueryParameter.mUseLatLon) {
                Log.d(Global.LOG_CONTEXT, "FotoGalleryActivity.navigateTo " + selectedAbsolutePath + " from " + galleryQueryParameter.mCurrentLatLon);
                this.galleryQueryParameter.mCurrentLatLon.get(DirectoryFormatter.parseLatLon(selectedAbsolutePath));

                reloadGui();
            } else { //  if (this.galleryQueryParameter.mCurrentPath.compareTo(selectedAbsolutePath) != 0) {
                Log.d(Global.LOG_CONTEXT, "FotoGalleryActivity.navigateTo " + selectedAbsolutePath + " from " + this.galleryQueryParameter.mCurrentPath);
                this.galleryQueryParameter.mCurrentPath = selectedAbsolutePath;
                this.galleryQueryParameter.mDirQueryID = queryTypeId;
                setTitle();

                reloadGui();
            }
        }
    }

    private void reloadGui() {
        if (mGalleryGui != null) {
            QueryParameterParcelable query = this.galleryQueryParameter.calculateEffectiveGalleryContentQuery();
            if (query != null) {
                this.mGalleryGui.requery(this, query);
            }
        }

        if (mDirGui != null) {
            String currentPath = this.galleryQueryParameter.mCurrentPath;
            if (currentPath != null) {
                mDirGui.navigateTo(currentPath);
            }
        }
    }

    private void onDirectoryDataLoadComplete(Directory directoryRoot) {
        if (directoryRoot == null) {
            final String message = getString(R.string.err_load_dir_failed, FotoSql.getName(this, this.galleryQueryParameter.getDirQueryID()));
            Toast.makeText(this, message,Toast.LENGTH_LONG).show();
        } else {
            mDirectoryRoot = directoryRoot;
            if ((mDirGui != null) && (this.galleryQueryParameter.mCurrentPath != null)) {
                mDirGui.defineDirectoryNavigation(directoryRoot, this.galleryQueryParameter.getDirQueryID(), this.galleryQueryParameter.mCurrentPath);
            }
            Global.debugMemory(debugPrefix, "onDirectoryDataLoadComplete");

            if ((mDirectoryRoot != null) && (this.mMustShowNavigator)) {
                openNavigator();
            }
        }
    }

    private void setTitle() {
        String title = getIntent().getStringExtra(EXTRA_TITLE);

        if (title == null) {
            if (galleryQueryParameter.mUseLatLon) {
                title = getString(R.string.gallery_foto);
            } else if (this.galleryQueryParameter.mCurrentPath != null) {
                title = FotoSql.getName(this, this.galleryQueryParameter.getDirQueryID()) + " - " + this.galleryQueryParameter.mCurrentPath;
            }
        }
        if (title != null) {
            this.setTitle(title + mTitleResultCount);
        }
    }
}

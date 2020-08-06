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

package de.k3b.android.widget;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.k3b.LibGlobal;
import de.k3b.android.androFotoFinder.BaseActivity;
import de.k3b.android.androFotoFinder.Common;
import de.k3b.android.androFotoFinder.GalleryFilterActivity;
import de.k3b.android.androFotoFinder.GalleryFilterPathState;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.LockScreen;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.directory.DirectoryLoaderTask;
import de.k3b.android.androFotoFinder.directory.DirectoryPickerFragment;
import de.k3b.android.androFotoFinder.locationmap.LocationMapFragment;
import de.k3b.android.androFotoFinder.queries.AndroidAlbumUtils;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.tagDB.TagSql;
import de.k3b.android.androFotoFinder.tagDB.TagsPickerFragment;
import de.k3b.android.osmdroid.OsmdroidUtil;
import de.k3b.android.util.PhotoChangeNotifyer;
import de.k3b.android.util.PhotoPropertiesMediaFilesScanner;
import de.k3b.database.QueryParameter;
import de.k3b.io.AlbumFile;
import de.k3b.io.Directory;
import de.k3b.io.DirectoryFormatter;
import de.k3b.io.GalleryFilterParameter;
import de.k3b.io.IDirectory;
import de.k3b.io.IGalleryFilter;
import de.k3b.io.ListUtils;
import de.k3b.io.StringUtils;
import de.k3b.io.collections.SelectedItemIds;
import de.k3b.tagDB.Tag;

/**
 * An activity that can have a filtered collection of photos based on
 * a base-filter plus a sub-filter
 *
 * Created by k3b on 10.07.2018.
 */
public abstract class BaseQueryActivity extends BaseActivity implements Common, DirectoryPickerFragment.OnDirectoryInteractionListener,
        LocationMapFragment.OnDirectoryInteractionListener,
        TagsPickerFragment.ITagsPicker {
    protected static final String mDebugPrefix = "GalleryA-";

    private static final String DLG_NAVIGATOR_TAG = "navigator";
    private static final String DEFAULT_BOOKMARKNAME_PICK_GEO = "pickGeoFromPhoto";
    public static final int resultID = 522;

    protected GalleryQueryParameter mGalleryQueryParameter = new GalleryQueryParameter();

    protected String mTitleResultCount = "";

    protected boolean mHasEmbeddedDirPicker = false;

    private void navigateTo(String selectedAbsolutePath, int queryTypeId) {

        if (selectedAbsolutePath != null) {
            final GalleryFilterParameter currentSubFilterSettings = this.mGalleryQueryParameter.getCurrentSubFilterSettings();
            if (mGalleryQueryParameter.mCurrentSubFilterMode == GalleryQueryParameter.SUB_FILTER_MODE_GEO) {
                final String why = "FotoGalleryActivity.navigateTo tags geo ";
                Log.d(Global.LOG_CONTEXT, why + selectedAbsolutePath + " from "
                        + DirectoryFormatter.formatLatLon(currentSubFilterSettings.getLatitudeMin()
                        , currentSubFilterSettings.getLogituedMin()
                        , currentSubFilterSettings.getLatitudeMax()
                        , currentSubFilterSettings.getLogituedMax()));
                currentSubFilterSettings.get(DirectoryFormatter.parseLatLon(selectedAbsolutePath));

                reloadGui(why);

            } else if (mGalleryQueryParameter.mCurrentSubFilterMode == GalleryQueryParameter.SUB_FILTER_MODE_TAG) {
                final String why = "FotoGalleryActivity.navigateTo tags ";
                Log.d(Global.LOG_CONTEXT, why + selectedAbsolutePath + " from "
                        + ListUtils.toString(this.mGalleryQueryParameter.getCurrentSubFilterSettings().getTagsAllIncluded()));
                currentSubFilterSettings.setTagsAllIncluded(new ArrayList<>(ListUtils.fromString(selectedAbsolutePath)));

                reloadGui(why);
            } else if (mGalleryQueryParameter.mCurrentSubFilterMode == GalleryQueryParameter.SUB_FILTER_MODE_DATE) {
                final String why = "FotoGalleryActivity.navigateTo date ";
                Log.d(Global.LOG_CONTEXT, why + selectedAbsolutePath + " from " + currentSubFilterSettings.getDatePath());

                Date from = new Date();
                Date to = new Date();
                DirectoryFormatter.parseDatesPath(selectedAbsolutePath, from, to);

                currentSubFilterSettings.setDate(from.getTime(), to.getTime());
                this.mGalleryQueryParameter.mCurrentSubFilterMode = GalleryQueryParameter.SUB_FILTER_MODE_DATE;
                this.mGalleryQueryParameter.mDirQueryID = queryTypeId;
                setTitle();

                reloadGui(why);
            } else if (mGalleryQueryParameter.mCurrentSubFilterMode == GalleryQueryParameter.SUB_FILTER_MODE_DATE_MODIFIED) {
                final String why = "FotoGalleryActivity.navigateTo date modified";
                Log.d(Global.LOG_CONTEXT, why + selectedAbsolutePath + " from " + currentSubFilterSettings.getDateModifiedPath());

                Date from = new Date();
                Date to = new Date();
                DirectoryFormatter.parseDatesPath(selectedAbsolutePath, from, to);

                currentSubFilterSettings.setDateModified(from.getTime(), to.getTime());
                this.mGalleryQueryParameter.mCurrentSubFilterMode = GalleryQueryParameter.SUB_FILTER_MODE_DATE_MODIFIED;
                this.mGalleryQueryParameter.mDirQueryID = queryTypeId;
                setTitle();

                reloadGui(why);
            } else if (mGalleryQueryParameter.mCurrentSubFilterMode == GalleryQueryParameter.SUB_FILTER_MODE_PATH) {
                GalleryFilterPathState state = new GalleryFilterPathState()
                        .load(BaseQueryActivity.this,
                                null, null);
                File queryFile = AlbumFile.getExistingQueryFileOrNull(selectedAbsolutePath);
                if (queryFile != null) {
                    final String why = "FotoGalleryActivity.navigate to virtual album ";
                    Log.d(Global.LOG_CONTEXT, why + selectedAbsolutePath);

                    QueryParameter albumQuery = AndroidAlbumUtils.getQueryFromUri(mDebugPrefix + " navigateTo ", this, null, Uri.fromFile(queryFile), null);
                    if (albumQuery != null) {
                        this.mGalleryQueryParameter.mGalleryContentBaseQuery = albumQuery;
                        this.mGalleryQueryParameter.mCurrentSubFilterMode = GalleryQueryParameter.SUB_FILTER_MODE_ALBUM;
                        currentSubFilterSettings.setPath(selectedAbsolutePath);

                        state.setLastPath(queryFile.getParent());
                        state.setAlbum(Uri.fromFile(queryFile));

                        reloadGui(why);
                    }
                } else {
                    final String why = "FotoGalleryActivity.navigateTo dir ";
                    Log.d(Global.LOG_CONTEXT, why + selectedAbsolutePath + " from " + currentSubFilterSettings.getPath());

                    currentSubFilterSettings.setFolderAndBelow(selectedAbsolutePath);
                    this.mGalleryQueryParameter.mCurrentSubFilterMode = GalleryQueryParameter.SUB_FILTER_MODE_PATH;
                    this.mGalleryQueryParameter.mDirQueryID = queryTypeId;
                    state.setLastPath(selectedAbsolutePath);
                    setTitle();

                    reloadGui(why);
                }
                state.save(this, null);

            } // if SUB_FILTER_MODE_PATH
        }
    }

    // ...path, {album, +tag, ?search, #date, lat+long
    public CharSequence getValueAsTitle(boolean longName) {
        if (mGalleryQueryParameter == null) return null;
        return mGalleryQueryParameter.getValueAsTitle(longName);
    }

    /**
     * allows childclass to have their own sharedPreference names
     */
    protected String fixSharedPrefSuffix(String statSuffix) {
        return statSuffix;
    }


    private FolderApi mFolderApi = null;

    private FolderApi getFolderApi() {
        if (mFolderApi == null) {
            mFolderApi = new FolderApi();
        }
        return mFolderApi;
    }

    /**
     * every thing that belongs to search.
     * visible gallery items are mGalleryContentBaseQuery + expression(mCurrentSubFilterMode)
     */
    protected class GalleryQueryParameter {

        /** picker has different set of filter parameters as ordenary gallery view */
        private static final String PICK_NONE_SUFFIX = "";
        private static final String PICK_GEO_SUFFIX = "-pick-geo";
        private static final String PICK_IMAGE_SUFFIX = "-pick-image";

        /**
         * one of the PICK_XXXX_SUFFIX constants
         * view/pick-image/pick-geo have different state persistence.
         * naem=STATE_XXXXX + mSharedPrefKeySuffix
         * ""==view; "-pick-image"; "-pick-geo"
         */
        private String mSharedPrefKeySuffix = PICK_NONE_SUFFIX;

        /**
         * STATE_... to persist current filter
         */
        private static final String STATE_DirQueryID = "DirQueryID";
        private static final String STATE_SortID = "SortID";
        private static final String STATE_SortAscending = "SortAscending";

        private static final String STATE_SUB_FILTER = "subFilter";

        private static final String STATE_SUB_FILTR_MODE = "currentSubFilterMode";

        /**
         * mCurrentSubFilterMode = SUB_FILTER_MODE_XXX: which filter addon is currently active
         */
        private static final int SUB_FILTER_MODE_NONE = -1;
        private static final int SUB_FILTER_MODE_PATH = 0;
        private static final int SUB_FILTER_MODE_GEO = 1;
        private static final int SUB_FILTER_MODE_TAG = 2;
        private static final int SUB_FILTER_MODE_SEARCH_BAR = 3;
        private static final int SUB_FILTER_MODE_ALBUM = 4;
        private static final int SUB_FILTER_MODE_DATE = 5;

        private static final int SUB_FILTER_MODE_DATE_MODIFIED = 6;

        /**
         * mCurrentSubFilterMode = SUB_FILTER_MODE_XXX: which filter addon is currently active:
         * Filter = basefilter + mCurrentSubFilterMode
         */
        private int mCurrentSubFilterMode = SUB_FILTER_MODE_NONE;
        private String mAlbumName = null;

        /**
         * mCurrentSubFilterMode defines which of the Filter parameters define the current visible items
         */
        private GalleryFilterParameter mCurrentSubFilterSettings = new GalleryFilterParameter();

        /**
         * sql defines current visible items with optional sort order
         */
        protected QueryParameter mGalleryContentBaseQuery = null;

        /**
         * one of the FotoSql.QUERY_TYPE_xxx values
         */
        protected int mDirQueryID = FotoSql.QUERY_TYPE_GROUP_DEFAULT;

        /**
         * current sort order
         */
        private int mCurrentSortID = FotoSql.SORT_BY_DEFAULT;
        /**
         * current sort order
         */
        private boolean mCurrentSortAscending = false;

        /**
         * true: if activity started without special intent-parameters,
         * the last mCurrentSubFilterSettings is saved/loaded for next use
         */
        private boolean mSaveLastUsedFilterToSharedPrefs = true;

        /**
         * one of the FotoSql.QUERY_TYPE_xxx values. if undefined use default
         */
        public int getDirQueryID() {
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
            return FotoSql.getName(context, this.mCurrentSortID) + " " + ((mCurrentSortAscending) ? IGalleryFilter.SORT_DIRECTION_ASCENDING : IGalleryFilter.SORT_DIRECTION_DESCENDING);
        }

        public boolean clearPathIfActive() {
            if ((mCurrentSubFilterMode == SUB_FILTER_MODE_PATH) && (mCurrentSubFilterSettings.getPath() != null)) {
                mCurrentSubFilterSettings.setPath(null);
                return true;
            }
            return false;
        }

        /**
         * combine root-query plus current selected directoryRoot/geo/tags
         */
        public QueryParameter calculateEffectiveGalleryContentQuery() {
            return calculateEffectiveGalleryContentQuery(mGalleryContentBaseQuery);
        }

        /**
         * combine root-query plus current selected directoryRoot
         */
        private QueryParameter calculateEffectiveGalleryContentQuery(QueryParameter rootQuery) {
            QueryParameter result = new QueryParameter(rootQuery);

            final IGalleryFilter currentSubFilterSettings = this.getCurrentSubFilterSettings();
            if (currentSubFilterSettings != null) {

                if (result == null) return null;

                switch (mCurrentSubFilterMode) {
                    case SUB_FILTER_MODE_SEARCH_BAR:
                        TagSql.addFilterAny(result, currentSubFilterSettings.getInAnyField());
                        break;
                    case SUB_FILTER_MODE_GEO:
                        FotoSql.addWhereFilterLatLon(result, currentSubFilterSettings);
                        break;
                    case SUB_FILTER_MODE_TAG:
                        TagSql.addWhereTagsIncluded(result, currentSubFilterSettings.getTagsAllIncluded(), false);
                        break;
                    case SUB_FILTER_MODE_PATH:
                    case SUB_FILTER_MODE_ALBUM: {
                        final String path = currentSubFilterSettings.getPath();
                        if (!StringUtils.isNullOrEmpty(path)) {
                            Uri uri = Uri.fromFile(new File(path));
                            QueryParameter albumQuery = AndroidAlbumUtils.getQueryFromUri(
                                    mDebugPrefix + " calculateEffectiveGalleryContentQuery ",
                                    BaseQueryActivity.this, null, uri, null);
                            if (albumQuery != null) {
                                result.getWhereFrom(albumQuery, true);
                            } else if (PhotoPropertiesMediaFilesScanner.isNoMedia(path, PhotoPropertiesMediaFilesScanner.DEFAULT_SCAN_DEPTH)) {
                                // do not show (parent-)directories that contain ".nomedia"
                                return null;
                            } else {
                                FotoSql.addPathWhere(result, path, this.getDirQueryID());
                            }
                        }
                    }
                    break;

                    case SUB_FILTER_MODE_DATE:
                        FotoSql.addWhereDateMinMax(result, currentSubFilterSettings.getDateMin(), currentSubFilterSettings.getDateMax());
                        break;
                    case SUB_FILTER_MODE_DATE_MODIFIED:
                        FotoSql.addWhereDateModifiedMinMax(result, currentSubFilterSettings.getDateModifiedMin(), currentSubFilterSettings.getDateModifiedMax());
                        break;
                }
            }

            if (mCurrentSortID != IGalleryFilter.SORT_BY_NONE) {
                FotoSql.setSort(result, mCurrentSortID, mCurrentSortAscending);
            }
            return result;
        }

        public boolean isGeoPick() {
            return (mSharedPrefKeySuffix != null) && mSharedPrefKeySuffix.equals(PICK_GEO_SUFFIX);
        }

        // load from intent/ savedInstanceState/ SharedPrefs
        // Use cases

        /**
         * load from savedInstanceState/ intent/ SharedPrefs
         * Use cases
         * * stand alone gallery (i.e. from file manager with intent-uri of image.jpg/virtual-album/directory)
         * * sub gallery drill down from other apm activity with intent containing file-uri/query-extra and/or filter-extra
         * * as picker for
         * * * ACTION_PICK geo via image
         * * * ACTION_PICK pick image
         * * * ACTION_GET_CONTENT
         *
         * @param context
         * @param savedInstanceState
         */
        private void loadSettingsAndInstanceState(Activity context, Bundle savedInstanceState) {

            Intent intent = context.getIntent();

            // for debugging: where does the filter come from
            StringBuilder dbgMessageResult = (Global.debugEnabled) ? new StringBuilder() : null;

            // special name handling for pickers
            boolean isPick = false;
            String action = (intent != null) ? intent.getAction() : null;
            if ((action != null) && ((Intent.ACTION_PICK.compareTo(action) == 0) || (Intent.ACTION_GET_CONTENT.compareTo(action) == 0))) {
                String schema = intent.getScheme();
                if ((schema != null) && ("geo".compareTo(schema) == 0)) {
                    this.mSharedPrefKeySuffix = PICK_GEO_SUFFIX;
                    if (dbgMessageResult != null) dbgMessageResult.append("pick geo ");
                } else {
                    this.mSharedPrefKeySuffix = PICK_IMAGE_SUFFIX;
                    if (dbgMessageResult != null) dbgMessageResult.append("pick photo ");
                }
                this.mSaveLastUsedFilterToSharedPrefs = true;
                isPick = true;
            } else {
                this.mSharedPrefKeySuffix = PICK_NONE_SUFFIX;
                // save only if no intent-uri is involved
                this.mSaveLastUsedFilterToSharedPrefs = StringUtils.isNullOrEmpty(intent.getDataString());
            }
            this.mSharedPrefKeySuffix = fixSharedPrefSuffix(this.mSharedPrefKeySuffix);
            SharedPreferences sharedPref =
                    (this.mSaveLastUsedFilterToSharedPrefs)
                            ? PreferenceManager.getDefaultSharedPreferences(context)
                            : null;

            StringBuilder uriQueryFile = new StringBuilder();
            this.mGalleryContentBaseQuery = AndroidAlbumUtils.getQuery(
                    BaseQueryActivity.this, mSharedPrefKeySuffix,
                    savedInstanceState, intent, sharedPref, uriQueryFile, dbgMessageResult);

            if (dbgMessageResult != null) dbgMessageResult.append("SubFilter ");
            boolean found = false;
            if (savedInstanceState == null) {
                // onCreate (first call) : if intent is usefull use it else use sharedPref
                if (!found && (intent != null)) {

                    found = setState(dbgMessageResult, " from-Intent: ",
                                intent.getStringExtra(STATE_SUB_FILTER),
                                intent.getIntExtra(STATE_DirQueryID, this.getDirQueryID()),
                                intent.getIntExtra(STATE_SortID, this.mCurrentSortID),
                                intent.getBooleanExtra(STATE_SortAscending, this.mCurrentSortAscending),
                                intent.getIntExtra(STATE_SUB_FILTR_MODE, this.mCurrentSubFilterMode));
                    if ((!found) && (!isPick) && (AndroidAlbumUtils.getQuery(
                            BaseQueryActivity.this, null,
                            null, intent, null,
                            null, null) != null)) {
                        // has been called with  intent filter data so no more state loading
                        found = true;
                       this.mSaveLastUsedFilterToSharedPrefs = false;
                    }
                }
                if (!found && (sharedPref != null)) {
                    found = setState(dbgMessageResult, " from-SharedPrefs: ",
                                sharedPref.getString(STATE_SUB_FILTER + mSharedPrefKeySuffix, null),
                                sharedPref.getInt(STATE_DirQueryID + mSharedPrefKeySuffix, this.getDirQueryID()),
                                sharedPref.getInt(STATE_SortID + mSharedPrefKeySuffix, this.mCurrentSortID),
                                sharedPref.getBoolean(STATE_SortAscending + mSharedPrefKeySuffix, this.mCurrentSortAscending),
                                sharedPref.getInt(STATE_SUB_FILTR_MODE, this.mCurrentSubFilterMode));
                }
            } else  {
                // (savedInstanceState != null) : onCreate after screen rotation

                found = setState(dbgMessageResult, " from-InstanceState: ",
                            savedInstanceState.getString(STATE_SUB_FILTER, null),
                            savedInstanceState.getInt(STATE_DirQueryID, this.getDirQueryID()),
                            savedInstanceState.getInt(STATE_SortID, this.mCurrentSortID),
                            savedInstanceState.getBoolean(STATE_SortAscending, this.mCurrentSortAscending),
                            savedInstanceState.getInt(STATE_SUB_FILTR_MODE, this.mCurrentSubFilterMode));
            }

            // all parameters loaded: either album, filter or path
            if (this.mGalleryContentBaseQuery == null) {
                this.mGalleryContentBaseQuery = FotoSql.getQuery(FotoSql.QUERY_TYPE_DEFAULT);
                if (dbgMessageResult != null) dbgMessageResult.append(" no query in parameters-use defaults ");
            }

            if (uriQueryFile.length() > 0) {
                this.setAlbum(uriQueryFile.toString());
            }

            if (dbgMessageResult != null) {
                Log.i(Global.LOG_CONTEXT, mDebugPrefix + dbgMessageResult.toString());
            }
        }

        private void setAlbum(String uriQueryFile) {
            Uri uri = (uriQueryFile == null) ? null : Uri.parse(uriQueryFile);
            if (uri != null) {
                this.mAlbumName = uri.getLastPathSegment();
                this.mCurrentSubFilterMode = SUB_FILTER_MODE_ALBUM;
            }
        }

        private boolean setState(StringBuilder dbgMessageResult, String dbgContext,
                              String subFilterSettingsAsString, int dirQueryID, int sortID,
                              boolean sortAscending, int subFilterMode) {
            if (subFilterSettingsAsString != null) {
                // SubFilterSettings, DirQueryID, SortID, SortAscending, SubFilterMode
                GalleryFilterParameter.parse(subFilterSettingsAsString, mCurrentSubFilterSettings);
                if (dbgMessageResult != null)
                    dbgMessageResult.append(dbgContext).append(subFilterSettingsAsString);
                this.mDirQueryID = dirQueryID;
                this.mCurrentSortID = sortID;
                this.mCurrentSortAscending = sortAscending;
                this.mCurrentSubFilterMode = subFilterMode;
                return true;
            }
            return false;
        }

        private void saveToInstanceState(Context context, Bundle savedInstanceState) {
            saveToSharedPrefs(context);

            // SubFilterSettings, DirQueryID, SortID, SortAscending, SubFilterMode
            if (mCurrentSubFilterSettings != null) {
                savedInstanceState.putString(STATE_SUB_FILTER, mCurrentSubFilterSettings.toString());
            }
            savedInstanceState.putInt(STATE_DirQueryID, this.getDirQueryID());
            savedInstanceState.putInt(STATE_SortID, this.mCurrentSortID);
            savedInstanceState.putBoolean(STATE_SortAscending, this.mCurrentSortAscending);
            savedInstanceState.putInt(STATE_SUB_FILTR_MODE, this.mCurrentSubFilterMode);

            if ((mGalleryQueryParameter != null) && (mGalleryQueryParameter.mGalleryContentBaseQuery != null)) {
                savedInstanceState.putString(EXTRA_QUERY, mGalleryQueryParameter.mGalleryContentBaseQuery.toReParseableString());
            }

        }

        private void saveToSharedPrefs(Context context) {
            if (mSaveLastUsedFilterToSharedPrefs) {
                // SubFilterSettings, DirQueryID, SortID, SortAscending, SubFilterMode
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor edit = sharedPref.edit();

                if (getCurrentSubFilterSettings() != null) {
                    edit.putString(STATE_SUB_FILTER + mSharedPrefKeySuffix, getCurrentSubFilterSettings().toString());
                }

                edit.putInt(STATE_DirQueryID + mSharedPrefKeySuffix, this.getDirQueryID());
                edit.putInt(STATE_SortID + mSharedPrefKeySuffix, this.mCurrentSortID);
                edit.putBoolean(STATE_SortAscending + mSharedPrefKeySuffix, this.mCurrentSortAscending);
                edit.putInt(STATE_SUB_FILTR_MODE, this.mCurrentSubFilterMode);

                if ((mGalleryQueryParameter != null) && (mGalleryQueryParameter.mGalleryContentBaseQuery != null)) {
                    edit.putString(EXTRA_QUERY + mSharedPrefKeySuffix, mGalleryQueryParameter.mGalleryContentBaseQuery.toReParseableString());
                }

                edit.apply();
            }
        }

        public GalleryFilterParameter getCurrentSubFilterSettings() {
            return mCurrentSubFilterSettings;
        }

        // ...path, {album, +tag, ?search, #date, lat+long
        protected CharSequence getValueAsTitle(boolean longName) {
            final int subFilterMode = this.mCurrentSubFilterMode;
            GalleryFilterParameter v = mCurrentSubFilterSettings;

            if (v != null) {
                switch (subFilterMode) {
                    case SUB_FILTER_MODE_PATH: {
                        File f = v.getPathFile();
                        if (f == null) break;
                        StringBuilder result = new StringBuilder();
                        result.insert(0, f.getName());
                        f = f.getParentFile();
                        if (f != null) {
                            result.insert(0, "/");
                            result.insert(0, f.getName());
                        }
                        result.insert(0, "...");
                        if (longName) result.insert(0, getString(R.string.folder_menu_title));

                        return result;
                    }

                    case SUB_FILTER_MODE_ALBUM: {
                        if (this.mAlbumName != null) return getValueAsTitle(longName, "{", R.string.bookmark, this.mAlbumName);
                        return null;
                    }

                    case SUB_FILTER_MODE_GEO:
                        final int lat = (int) v.getLatitudeMin();
                        final int lon = (int) v.getLogituedMin();
                        return ((longName) ? getString(R.string.area_menu_title) : "") + lat + " " + lon;
                    case SUB_FILTER_MODE_TAG:
                        return getValueAsTitle(longName, "+", R.string.tags_activity_title, ListUtils.toString(v.getTagsAllIncluded()));
                    case SUB_FILTER_MODE_SEARCH_BAR:
                        return getValueAsTitle(longName, "?", R.string.searchbar_menu_title, v.getInAnyField());
                    case SUB_FILTER_MODE_DATE:
                        return getValueAsTitle(longName, "#", R.string.date_picker_menu_title, v.getDatePath());
                }
            }
            return null;
        }

        private  CharSequence getValueAsTitle(boolean longName, String prefix, int stringResourceId, String value) {
            StringBuilder result = new StringBuilder();
            if (longName && (stringResourceId != 0)) result.append(getString(stringResourceId)).append(": ");
            if (!StringUtils.isNullOrEmpty(value)) result.append(prefix);
            result.append(value);
            return result;
        }
    }

    abstract protected void defineDirectoryNavigation(IDirectory directoryRoot);

    private void openLatLonPicker(SelectedItemIds selectedItemIds) {
        this.mGalleryQueryParameter.mCurrentSubFilterMode = BaseQueryActivity.GalleryQueryParameter.SUB_FILTER_MODE_GEO;

        final FragmentManager manager = getFragmentManager();
        LocationMapFragment dialog = new LocationMapFragment();
        dialog.defineNavigation(this.mGalleryQueryParameter.mGalleryContentBaseQuery,
                null,
                this.mGalleryQueryParameter.getCurrentSubFilterSettings(), OsmdroidUtil.NO_ZOOM, selectedItemIds, null, false);

        dialog.show(manager, DLG_NAVIGATOR_TAG);
        setAutoClose(dialog, null, null);
    }

    private void openFilter(CharSequence debugContext) {
        GalleryFilterActivity.showActivity("[23] neu " + debugContext, this,
                null,
                this.mGalleryQueryParameter.mGalleryContentBaseQuery,
                null, BaseQueryActivity.resultID);
    }

    private void openTagPicker() {
        mGalleryQueryParameter.mCurrentSubFilterMode = BaseQueryActivity.GalleryQueryParameter.SUB_FILTER_MODE_TAG;

        final FragmentManager manager = getFragmentManager();
        TagsPickerFragment dlg = new TagsPickerFragment();
        dlg.setFragmentOnwner(this);
        dlg.setTitleId(R.string.tags_activity_title);
        dlg.setBaseQuery(this.mGalleryQueryParameter.calculateEffectiveGalleryContentQuery());
        List<String> included = this.mGalleryQueryParameter.getCurrentSubFilterSettings().getTagsAllIncluded();
        if (included == null) included = new ArrayList<String>();
        dlg.setAddNames(included);
        dlg.show(manager, DLG_NAVIGATOR_TAG);
        setAutoClose(dlg, null, null);
    }

    /**
     * called by {@link TagsPickerFragment}
     */
    @Override
    public boolean onCancel(String msg) {
        return true;
    }

    /**
     * called by {@link TagsPickerFragment}
     */
    @Override
    public boolean onOk(List<String> addNames, List<String> removeNames) {
        Log.d(Global.LOG_CONTEXT, "BaseQueryActivity.navigateTo " + ListUtils.toString(addNames) + " from "
                + ListUtils.toString(mGalleryQueryParameter.getCurrentSubFilterSettings().getTagsAllIncluded()));
        mGalleryQueryParameter.mCurrentSubFilterMode = BaseQueryActivity.GalleryQueryParameter.SUB_FILTER_MODE_TAG;
        mGalleryQueryParameter.getCurrentSubFilterSettings().setTagsAllIncluded(new ArrayList<String>(addNames));
        reloadGui("navigate to tags");
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Global.debugMemory(mDebugPrefix, "onCreate");
        super.onCreate(savedInstanceState);
        PhotoChangeNotifyer.registerContentObserver(this, mMediaObserverDirectory);

    }

    protected void onCreateData(Bundle savedInstanceState) {
        final Intent intent = getIntent();

        this.mGalleryQueryParameter.loadSettingsAndInstanceState(this, savedInstanceState);
    }

    /**
     * OnDirectoryInteractionListener: called when user selects a new directoryRoot
     */
    @Override
    public void onDirectoryPick(String selectedAbsolutePath, int queryTypeId) {
        if (!this.mHasEmbeddedDirPicker) {
            navigateTo(selectedAbsolutePath, queryTypeId);
        }
        closeDialogIfNeeded();

    }

    /**
     * after media db change cached Directories must be recalculated
     */
    private final ContentObserver mMediaObserverDirectory = new ContentObserver(null) {
        @Override
        public void onChange(boolean selfChange) {
            invalidateDirectories(mDebugPrefix + "#onChange from mMediaObserverDirectory");
        }
    };

    /* OnDirectoryInteractionListener */
    @Override
    public void invalidateDirectories(String why) {
        getFolderApi().invalidateDirectories(why);
    }

    /**
     * DirectoryPickerFragment#OnDirectoryInteractionListener: called when user cancels selection of a new directoryRoot
     */
    @Override
    public void onDirectoryCancel(int queryTypeId) {
        closeDialogIfNeeded();
    }

    @Override
    public void closeDialogIfNeeded() {
        super.closeDialogIfNeeded();
        getFolderApi().mDirPicker = null;
    }

    /**
     * DirectoryPickerFragment#OnDirectoryInteractionListener: called after the selection in tree has changed
     */
    @Override
    public void onDirectorySelectionChanged(String selectedAbsolutePath, int queryTypeId) {
        if (this.mHasEmbeddedDirPicker) {
            navigateTo(selectedAbsolutePath, queryTypeId);
        }
    }

    protected class FolderApi {
        // either folder picker or date picker
        private static final int QUERY_TYPE_GROUP_ALBUM = FotoSql.QUERY_TYPE_GROUP_ALBUM;
        // either folder picker or date picker
        private static final int QUERY_TYPE_GROUP_DATE = FotoSql.QUERY_TYPE_GROUP_DATE;

        private static final int QUERY_TYPE_GROUP_DATE_MODIFIED = FotoSql.QUERY_TYPE_GROUP_DATE_MODIFIED;

        private IDirectory mDirectoryRoot = null;
        private IDirectory mDateRoot = null;
        private IDirectory mDateModifiedRoot = null;

        /**
         * true if activity should show navigator dialog after loading mDirectoryRoot is complete
         */
        private boolean mMustShowNavigator = false;

        /**
         * set while dir picker is active
         */
        private DirectoryPickerFragment mDirPicker = null;

        private void openDatePicker() {
            openPicker(BaseQueryActivity.GalleryQueryParameter.SUB_FILTER_MODE_DATE, QUERY_TYPE_GROUP_DATE,
                    R.menu.menu_context_pick_show_in_new, R.menu.menu_context_pick_date);
        }

        private void openDateModifiedPicker() {
            openPicker(GalleryQueryParameter.SUB_FILTER_MODE_DATE_MODIFIED, QUERY_TYPE_GROUP_DATE_MODIFIED,
                    R.menu.menu_context_pick_show_in_new, R.menu.menu_context_pick_date);
        }

        private void openFolderPicker() {
            openPicker(BaseQueryActivity.GalleryQueryParameter.SUB_FILTER_MODE_PATH, QUERY_TYPE_GROUP_ALBUM, R.menu.menu_context_pick_dir);
        }

        private void openPicker(final int filterMode, int _dirQueryID, int... menuId) {
            mGalleryQueryParameter.mCurrentSubFilterMode = filterMode;
            final Activity context = BaseQueryActivity.this;

            /** if wrong datatype was saved: gallery is not allowed for dirPicker */
            final int dirQueryID =
                    (FotoSql.QUERY_TYPE_GALLERY == _dirQueryID)
                            ? QUERY_TYPE_GROUP_ALBUM
                            : _dirQueryID;

            mGalleryQueryParameter.mDirQueryID = dirQueryID;

            boolean loadDate = (dirQueryID == QUERY_TYPE_GROUP_DATE) || (dirQueryID == QUERY_TYPE_GROUP_DATE_MODIFIED);
            final IDirectory currentDirectoryRoot = getiDirectoryRoot(dirQueryID);
            if (currentDirectoryRoot == null) {
                // not loaded yet. load directoryRoot in background
                final QueryParameter mergedBaseQuery = FotoSql.getQuery(dirQueryID);
                mergedBaseQuery.getWhereFrom(mGalleryQueryParameter.mGalleryContentBaseQuery, false);
                if (mergedBaseQuery != null) {
                    this.mMustShowNavigator = true;
                    mergedBaseQuery.setID(dirQueryID);

                    DirectoryLoaderTask loader = new DirectoryLoaderTask(context, loadDate && LibGlobal.datePickerUseDecade,
                            mDebugPrefix + " from openPicker(dirQueryID=" +
                                    FotoSql.getName(getApplicationContext(), dirQueryID) + ")") {
                        @Override
                        protected void onPostExecute(IDirectory directoryRoot) {
                            onDirectoryDataLoadComplete(dirQueryID, directoryRoot);
                        }
                    };
                    switch (dirQueryID) {
                        case QUERY_TYPE_GROUP_DATE:
                        case QUERY_TYPE_GROUP_DATE_MODIFIED:
                            loader.execute(mergedBaseQuery);
                            break;
                        default:
                            // limit valbums to matching parent-path query
                            QueryParameter vAlbumQueryWithPathExpr = FotoSql.copyPathExpressions(FotoSql.queryVAlbum, mergedBaseQuery);
                            if (vAlbumQueryWithPathExpr == null)
                                vAlbumQueryWithPathExpr = FotoSql.queryVAlbum;

                            // load dir-s + "*.album"
                            loader.execute(mergedBaseQuery, vAlbumQueryWithPathExpr);
                    }

                } else {
                    Log.e(Global.LOG_CONTEXT, mDebugPrefix + " this.mDirQueryID undefined "
                            + FotoSql.getName(getApplicationContext(), mGalleryQueryParameter.mDirQueryID));
                }
                // if not loaded yet
            } else {
                // if loaded
                mMustShowNavigator = false;
                final FragmentManager manager = getFragmentManager();
                DirectoryPickerFragment dirDialog = new DirectoryPickerFragment();

                if (!LockScreen.isLocked(context)) {
                    dirDialog.setContextMenuId(menuId);
                }
                dirDialog.setBaseQuery(mGalleryQueryParameter.mGalleryContentBaseQuery);

                String initialPath = mGalleryQueryParameter.getCurrentSubFilterSettings().getPath();

                if ((initialPath == null) && (!loadDate)) {
                    initialPath = new GalleryFilterPathState()
                            .load(BaseQueryActivity.this,
                                    null,null)
                            .getPathDefault(null);
                }

                if ((initialPath != null) && (initialPath.endsWith("%"))) {
                    initialPath = initialPath.substring(0,initialPath.length() - 1);
                }
                dirDialog.defineDirectoryNavigation(currentDirectoryRoot, dirQueryID,
                        initialPath);

                mDirPicker = dirDialog;
                setAutoClose(mDirPicker, null, null);
                dirDialog.show(manager, DLG_NAVIGATOR_TAG);
            }
        }

        private IDirectory getiDirectoryRoot(int dirQueryID) {
            switch (dirQueryID) {
                case QUERY_TYPE_GROUP_DATE:
                    return this.mDateRoot;
                case QUERY_TYPE_GROUP_DATE_MODIFIED:
                    return this.mDateModifiedRoot;
                default:
                    return this.mDirectoryRoot;
            }
        }

        private void onDirectoryDataLoadComplete(int dirQueryID, IDirectory directoryRoot) {
            if (directoryRoot == null) {
                final String message = getString(R.string.folder_err_load_failed_format, FotoSql.getName(BaseQueryActivity.this, mGalleryQueryParameter.getDirQueryID()));
                Toast.makeText(BaseQueryActivity.this, message, Toast.LENGTH_LONG).show();
            } else {
                boolean mustDefineNavigation;
                switch (dirQueryID) {
                    case QUERY_TYPE_GROUP_DATE:
                        mustDefineNavigation= (mGalleryQueryParameter.getCurrentSubFilterSettings().getDatePath() != null);
                        this.mDateRoot = directoryRoot;
                        break;
                    case QUERY_TYPE_GROUP_DATE_MODIFIED:
                        mustDefineNavigation = (mGalleryQueryParameter.getCurrentSubFilterSettings().getDateModifiedPath() != null);
                        this.mDateModifiedRoot = directoryRoot;
                        break;
                    default:
                        mustDefineNavigation = (mGalleryQueryParameter.getCurrentSubFilterSettings().getPath() != null);
                        this.mDirectoryRoot = directoryRoot;
                }

                final boolean mustShowFolderPicker = (directoryRoot != null) && (this.mMustShowNavigator);

                if (Global.debugEnabled) {
                    StringBuilder name = new StringBuilder(directoryRoot.getAbsolute());
                    Directory.appendCount(name, directoryRoot, Directory.OPT_DIR | Directory.OPT_SUB_DIR);
                    Log.i(Global.LOG_CONTEXT, mDebugPrefix + "onDirectoryDataLoadComplete(" +
                            "mustDefineNavigation=" + mustDefineNavigation +
                            ", mustShowFolderPicker=" + mustShowFolderPicker +
                            ", content=" + name + ",dirQueryID=" +
                            FotoSql.getName(getApplicationContext(), dirQueryID) + ")");
                }

                if (mustDefineNavigation) {
                    defineDirectoryNavigation(directoryRoot);
                }
                Global.debugMemory(mDebugPrefix, "onDirectoryDataLoadComplete");

                if (mustShowFolderPicker) {
                    switch (dirQueryID) {
                        case QUERY_TYPE_GROUP_DATE:
                            openDatePicker();
                            break;
                        case QUERY_TYPE_GROUP_DATE_MODIFIED:
                            openDateModifiedPicker();
                            break;
                        default:
                            openFolderPicker();
                    }
                }
            }
        }

        private void refreshSelection() {
            IDirectory lastPopUpSelection = (mDirPicker == null) ? null : mDirPicker.getLastPopUpSelection();
            if (lastPopUpSelection != null) lastPopUpSelection.refresh();
        }

        private void invalidateDirectories(String why) {
            mDirectoryRoot = invalidateDirectories(why, mDirectoryRoot);
            mDateRoot = invalidateDirectories(why, mDateRoot);
            mDateModifiedRoot = invalidateDirectories(why, mDateModifiedRoot);
        }

        private IDirectory invalidateDirectories(String why, IDirectory directoryRoot) {
            if (directoryRoot != null) {
                if (Global.debugEnabled) {
                    StringBuilder name = new StringBuilder(directoryRoot.getAbsolute());
                    Directory.appendCount(name, directoryRoot, Directory.OPT_DIR | Directory.OPT_SUB_DIR);
                    Log.i(Global.LOG_CONTEXT, mDebugPrefix + "invalidateDirectories(" + name + ") because of " + why);
                }
                if (mDirPicker == null) {
                    directoryRoot.destroy();
                    directoryRoot = null; // must refreshLocal next time
                }
            }
            return directoryRoot;
        }
    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        this.mGalleryQueryParameter.saveToInstanceState(this, savedInstanceState);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onPause() {
        Global.debugMemory(mDebugPrefix, "onPause");
        this.mGalleryQueryParameter.saveToSharedPrefs(this);
        super.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        invalidateDirectories(mDebugPrefix + "#onLowMemory");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PhotoChangeNotifyer.unregisterContentObserver(this, mMediaObserverDirectory);
        this.mGalleryQueryParameter.mGalleryContentBaseQuery = null;
        invalidateDirectories(mDebugPrefix + "#onDestroy");
    }

    @Override
    protected void onResume() {
        Global.debugMemory(mDebugPrefix, "onResume");
        super.onResume();
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

        getFolderApi().refreshSelection();

        switch (requestCode) {
            case BaseQueryActivity.resultID:
                onBaseFilterChanged(AndroidAlbumUtils.getQuery(
                        this, "", null, intent, null, null, null)
                        , mDebugPrefix + "#onActivityResult from GalleryFilterActivity");
                break;
            default:
                break;
        }
    }


    protected abstract void reloadGui(String why);

    /**
     * called by {@link TagsPickerFragment}
     */
    @Override
    public boolean onTagPopUpClick(MenuItem menuItem, int menuItemItemId, Tag selectedTag) {
        return TagsPickerFragment.handleMenuShow(mCurrentDialogFragment, menuItem, selectedTag.getName());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final boolean result = super.onCreateOptionsMenu(menu);
        initSearchView(menu.findItem(R.id.cmd_searchbar));
        return result;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final boolean result = super.onPrepareOptionsMenu(menu);
        initSearchView(menu.findItem(R.id.cmd_searchbar));
        return result;
    }

    protected boolean onOptionsItemSelected(MenuItem menuItem, SelectedItemIds selectedItemIds) {
        // Handle presses on the action bar items
        switch (menuItem.getItemId()) {
            case R.id.cmd_date:
            case R.id.cmd_select_date:
                getFolderApi().openDatePicker();
                return true;

            case R.id.cmd_date_modified:
                getFolderApi().openDateModifiedPicker();
                return true;

            case R.id.cmd_select_folder:
            case R.id.cmd_path:
                getFolderApi().openFolderPicker();
                return true;

            case R.id.cmd_select_lat_lon:
                openLatLonPicker(selectedItemIds);
                return true;
            case R.id.cmd_select_tag:
                openTagPicker();
                return true;
            case R.id.cmd_filter:
                openFilter(" menu " + menuItem.getTitle());
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

            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    /**
     * redefine base filter and refresh gui
     */
    protected void onBaseFilterChanged(QueryParameter query, String why) {
        if (query != null) {
            this.mGalleryQueryParameter.mGalleryContentBaseQuery = query;
            this.mGalleryQueryParameter.mCurrentSubFilterMode = GalleryQueryParameter.SUB_FILTER_MODE_NONE;

            invalidateDirectories(mDebugPrefix + "#filter changed " + why);

            reloadGui("basefilter changed " + why);
            setTitle();
        }
    }

    /** GalleryFragment tells the Owning Activity that querying data has finisched */
    public void setResultCount(int count) {
        this.mTitleResultCount = (count > 0) ? ("(" + count + ")") : "";
        setTitle();

        // current path does not contain photo => refreshLocal witout current path
        if ((count == 0) &&(mGalleryQueryParameter.clearPathIfActive())) {
            setTitle();
            reloadGui("query changed");
        }
    }

    protected void setTitle() {
        Intent intent = getIntent();
        CharSequence title = (intent == null) ? null : intent.getStringExtra(EXTRA_TITLE);

        if (title == null) {
            title = mGalleryQueryParameter.getValueAsTitle(false);
            if (StringUtils.isNullOrEmpty(title)) title = getString(R.string.gallery_title);
        }
        if (title != null) {
            if (hasParentCallContext()) {
                this.setTitle("<" + mTitleResultCount + title);
            } else {
                this.setTitle(mTitleResultCount + title);
            }
        }
    }
    /*********************** search view *******************/
    private SearchViewWithHistory searchView = null;

    protected void initSearchView(MenuItem item) {
        if (item != null) {
            final SearchViewWithHistory searchView = (SearchViewWithHistory) item.getActionView();
            this.searchView = searchView;
            if (searchView != null) {
                searchView.setMenuItem(item);
                // searchView.setCursorDrawable(R.drawable.custom_cursor);
                searchView.setEllipsize(true);
                searchView.setOnQueryTextListener(new SearchViewWithHistory.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        showSearchbarResult(query, "search bar submit");
                        // Toast.makeText(FotoGalleryActivity.this, "Query: " + query, Toast.LENGTH_LONG).show();
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        sendDelayed(HANDLER_FILTER_TEXT_CHANGED, HANDLER_FILTER_TEXT_DELAY);
                        return false;
                    }
                });

                searchView.setOnSearchViewListener(new SearchViewWithHistory.SearchViewListener() {
                    @Override
                    public void onSearchViewShown() {
                        showSearchbarResult("onSearchViewShown");
                    }

                    @Override
                    public void onSearchViewClosed() {

                        showSearchbarResult("onSearchViewClosed");
                        searchView.hideKeyboard(BaseQueryActivity.this.searchView.getRootView());
                    }
                });
                if (Global.debugEnabledViewItem) {
                    Log.i(Global.LOG_CONTEXT, mDebugPrefix + " initSearchView " + searchView);
                }

            }
        }
    }

    private void showSearchbarResult(String why) {
        if ((searchView != null) && (searchView.isSearchOpen()) ) {
            showSearchbarResult(searchView.getFilterValue(), why);
        }
    }
    private void showSearchbarResult(String query, String why) {
        if (Global.debugEnabledViewItem) {
            Log.i(Global.LOG_CONTEXT, mDebugPrefix + " showSearchbarResult(" +
                    why + ") " + searchView);
        }
        final GalleryFilterParameter currentSubFilterSettings = mGalleryQueryParameter.getCurrentSubFilterSettings();
        if ((mGalleryQueryParameter.mCurrentSubFilterMode != GalleryQueryParameter.SUB_FILTER_MODE_SEARCH_BAR)
                || (0 != StringUtils.compare(query, currentSubFilterSettings.getInAnyField()))) {

            mGalleryQueryParameter.mCurrentSubFilterMode = GalleryQueryParameter.SUB_FILTER_MODE_SEARCH_BAR;
            currentSubFilterSettings.setInAnyField(query);
            if (Global.debugEnabledSql) {
                Log.i(Global.LOG_CONTEXT, why + ": search " + query);
            }

            reloadGui(why);
        } else {
            if (Global.debugEnabledSql) {
                Log.i(Global.LOG_CONTEXT, why + ": ignore " + query);
            }

        }
    }

    // char(s) typing in filter is active
    private static final int HANDLER_FILTER_TEXT_CHANGED = 0;
    private static final int HANDLER_FILTER_TEXT_DELAY = 500;

    private final Handler delayProcessor = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            clearDelayProcessor();
            switch (msg.what) {
                case HANDLER_FILTER_TEXT_CHANGED:
                    showSearchbarResult( "onQueryTextChange");
                    break;
                default:
                    // not implemented
                    throw new IllegalStateException();
            }
        }

    };

    private void clearDelayProcessor() {
        this.delayProcessor
                .removeMessages(HANDLER_FILTER_TEXT_CHANGED);
    }

    private void sendDelayed(final int messageID, final int delayInMilliSec) {
        this.clearDelayProcessor();

        final Message msg = Message
                .obtain(this.delayProcessor, messageID, null);
        delayProcessor.sendMessageDelayed(msg,
                delayInMilliSec);
    }
    @Override
    public void onBackPressed() {
        if ((searchView != null) && searchView.isSearchOpen()) {
            searchView.closeSearch();

            // ??bug?? : with back-key on my android-4.2 the soft keyboard does not close
        } else {
            super.onBackPressed();
        }
    }

}

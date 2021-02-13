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
 
package de.k3b.android.androFotoFinder.gallery.cursor;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ShareActionProvider;
import android.widget.Toast;

import org.osmdroid.api.IGeoPoint;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import de.k3b.LibGlobal;
import de.k3b.android.androFotoFinder.AffUtils;
import de.k3b.android.androFotoFinder.Common;
import de.k3b.android.androFotoFinder.FotoGalleryActivity;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.LockScreen;
import de.k3b.android.androFotoFinder.OnGalleryInteractionListener;
import de.k3b.android.androFotoFinder.PhotoAutoprocessingEditActivity;
import de.k3b.android.androFotoFinder.PhotoPropertiesEditActivity;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.backup.BackupActivity;
import de.k3b.android.androFotoFinder.directory.DirectoryGui;
import de.k3b.android.androFotoFinder.directory.DirectoryPickerFragment;
import de.k3b.android.androFotoFinder.imagedetail.ImageDetailActivityViewPager;
import de.k3b.android.androFotoFinder.imagedetail.ImageDetailMetaDialogBuilder;
import de.k3b.android.androFotoFinder.locationmap.GeoEditActivity;
import de.k3b.android.androFotoFinder.locationmap.MapGeoPickerActivity;
import de.k3b.android.androFotoFinder.queries.CursorLoaderWithException;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.queries.FotoViewerParameter;
import de.k3b.android.androFotoFinder.queries.Queryable;
import de.k3b.android.androFotoFinder.queries.SqlJobTaskBase;
import de.k3b.android.androFotoFinder.tagDB.TagSql;
import de.k3b.android.androFotoFinder.tagDB.TagTask;
import de.k3b.android.androFotoFinder.tagDB.TagWorflow;
import de.k3b.android.androFotoFinder.tagDB.TagsPickerFragment;
import de.k3b.android.io.AndroidFileCommands;
import de.k3b.android.util.DBUtils;
import de.k3b.android.util.OsUtils;
import de.k3b.android.util.PhotoChangeNotifyer;
import de.k3b.android.util.PhotoPropertiesMediaFilesScanner;
import de.k3b.android.util.ResourceUtils;
import de.k3b.android.widget.AboutDialogPreference;
import de.k3b.android.widget.ActivityWithCallContext;
import de.k3b.android.widget.Dialogs;
import de.k3b.android.widget.FilePermissionActivity;
import de.k3b.android.widget.UpdateTask;
import de.k3b.database.QueryParameter;
import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.api.IGeoPointInfo;
import de.k3b.geo.io.GeoUri;
import de.k3b.io.Directory;
import de.k3b.io.GalleryFilterParameter;
import de.k3b.io.IDirectory;
import de.k3b.io.IGalleryFilter;
import de.k3b.io.ListUtils;
import de.k3b.io.PhotoAutoprocessingDto;
import de.k3b.io.StringUtils;
import de.k3b.io.VISIBILITY;
import de.k3b.io.collections.SelectedFiles;
import de.k3b.io.collections.SelectedItemIds;
import de.k3b.io.filefacade.FileFacade;
import de.k3b.io.filefacade.IFile;
import de.k3b.tagDB.Tag;

/**
 * A {@link Fragment} to show ImageGallery content based on ContentProvider-Cursor.
 * Activities that contain this fragment must implement the
 * {@link OnGalleryInteractionListener} interface
 * to handle interaction events.
 * Use the {@link GalleryCursorFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 * States view-locked <=> view <=> view-multiselect
 *             pick-single, pick-multible, pick-locked
 *
 * Menu:
 *   if (picker-mode) menu_gallery_pick + menu_gallery_non_multiselect
 *   if (locked) menu_locked
 *   if (isMultiSelectionActive()) menu_gallery_multiselect_mode_all + menu_image_commands
 *   if (view-non-select) menu_gallery_non_selected_only + menu_gallery_non_multiselect
 */
public class GalleryCursorFragment extends Fragment implements Queryable, DirectoryGui, Common,
        TagsPickerFragment.ITagsPicker, PhotoChangeNotifyer.PhotoChangedListener {
    private static final String INSTANCE_STATE_LAST_VISIBLE_POSITION = "lastVisiblePosition";
    private static final String INSTANCE_STATE_SELECTED_ITEM_IDS = "selectedItems";
    private static final String INSTANCE_STATE_OLD_TITLE = "oldTitle";
    private static final String INSTANCE_STATE_SEL_ONLY = "selectedOnly";
    private static final String INSTANCE_STATE_LOADER_ID = "loaderID";

    private static final int MODE_VIEW_PICKER_NONE = 0;
    private static final int MODE_VIEW_PICK_SINGLE = 1;
    private static final int MODE_VIEW_PICK_MULTIBLE = 2;

    private static final boolean SUPPORT_MULTISELECTION = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN);

    private static int nextLoaderID = 100;
    private int loaderID = -1;
    private boolean locked = false; // if != Global.locked : must update menu

    private HorizontalScrollView mParentPathBarScroller;
    private LinearLayout mParentPathBar;

    // private HorizontalScrollView mChildPathBarScroller;
    private LinearLayout mChildPathBar;

    // for debugging
    private static int id = 1;
    private String mDebugPrefix;

    private GridView mGalleryView;

    private ShareActionProvider mShareActionProvider;
    long mUpdateId = FotoSql.getMediaDBApi().getCurrentUpdateId();

    private GalleryCursorAdapterFromArray mAdapter = null;

    private OnGalleryInteractionListener mGalleryListener;
    private QueryParameter mGalleryContentQuery;

    private DirectoryPickerFragment.OnDirectoryInteractionListener mDirectoryListener;
    private int mLastVisiblePosition = -1;

    private TagUpdateTask mTagWorflow = null;

    // multi selection support
    private final SelectedItemIds mSelectedItemIds = new SelectedItemIds();

    private String mOldAppTitleBeforeMultiselectionStarts = null;
    private boolean mShowSelectedOnly = false;
    private boolean isMultiSelectionMenuVisible = false;
    private final AndroidFileCommands mFileCommands = new LocalFileCommands();
    private MenuItem mShareOnlyToggle;
    private MenuItem mMenuRemoveAllSelected = null;

    /* false: prevent showing error message again */
    private boolean mNoShareError = true;

    /** true pick geo; false pick image */
    private boolean mModePickGeoElsePickImaage = false;

    /** one of the MODE_VIEW_PICKER_XXXX */
    private int mMode = MODE_VIEW_PICKER_NONE;

    /**
     * not null while mDestDirPicker is open
     */
    private MoveOrCopyDestDirPicker mDestDirPicker = null;

    /**
     * not null while tag picker is open
     */
    private WeakReference<TagsPickerFragment> mTagPickerDialog = null;

    /**
     * not null while background task is active
     */
    private static UpdateTask exifUpdate = null;

    /**************** construction ******************/
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment GalleryCursorFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static GalleryCursorFragment newInstance() {
        GalleryCursorFragment fragment = new GalleryCursorFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public SelectedItemIds getSelectedItemIds() {
        return mSelectedItemIds;
    }

    public void onNotifyPhotoChanged() {
        requeryIfDataHasChanged();
    }

    /**
     * incremented every time a new curster/query is generated
     */
    private int mRequeryInstanceCount = 0;

    protected LocalCursorLoader mCurorLoader = null;

    private void requeryIfDataHasChanged() {
        if (FotoSql.getMediaDBApi().mustRequery(mUpdateId)) {
            requery("requeryIfDataHasChanged");
        }
    }

    private void requery(String why) {
        mUpdateId = FotoSql.getMediaDBApi().getCurrentUpdateId();
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, mDebugPrefix + why + " requery\n" + ((mGalleryContentQuery != null) ? mGalleryContentQuery.toSqlString() : null));
        }

        if (mGalleryContentQuery != null) {
            // query has been initialized
            if (mCurorLoader == null) {
                mCurorLoader = new LocalCursorLoader();
                getLoaderManager().initLoader(loaderID, null, mCurorLoader);
            } else {
                getLoaderManager().restartLoader(loaderID, null, this.mCurorLoader);
            }
        }
    }

    private boolean cmdMoveOrCopyWithDestDirPicker(final boolean move, final String lastCopyToPath,
                                                   final SelectedFiles fotos, final CharSequence title) {
        final FilePermissionActivity activity = (FilePermissionActivity) getActivity();
        if (fotos != null) {
            File missingRoot = activity.getMissingRootDirFileOrNull(
                    "cmdMoveOrCopyWithDestDirPicker", fotos.getFiles());
            if (missingRoot != null) {
                // ask for needed permissions
                activity.requestRootUriDialog(missingRoot, title,
                        new FilePermissionActivity.IOnDirectoryPermissionGrantedHandler() {
                            @Override
                            public void afterGrant(FilePermissionActivity activity) {
                                // does not work in frag,emt
                                mFileCommands.setContext(activity);
                                if (FileFacade.debugLogFacade) {
                                    Log.i(FileFacade.LOG_TAG, this.getClass().getSimpleName()
                                            + ": afterGrant " + activity + "-" + this);
                                }
                                cmdMoveOrCopyWithDestDirPicker(move, lastCopyToPath, fotos, title);
                            }
                        });

                return false;
            }
        }


        if (AndroidFileCommands.canProcessFile(this.getActivity(), false)) {
            PhotoChangeNotifyer.setPhotoChangedListener(this);
            mDestDirPicker = MoveOrCopyDestDirPicker.newInstance(move, fotos);

            mDestDirPicker.defineDirectoryNavigation(OsUtils.getRootOSDirectory(null),
                    (move) ? FotoSql.QUERY_TYPE_GROUP_MOVE : FotoSql.QUERY_TYPE_GROUP_COPY,
                    lastCopyToPath);
            if (!LockScreen.isLocked(this.getActivity())) {
                mDestDirPicker.setContextMenuId(R.menu.menu_context_pick_osdir);
            }

            mDestDirPicker.setBaseQuery(getCurrentQuery());
            mDestDirPicker.show(activity.getFragmentManager(), "osdir");
        }
        return false;
    }

    public GalleryCursorFragment() {
        mDebugPrefix = "GalleryCursorFragment#" + (id++)  + " ";
        Global.debugMemory(mDebugPrefix, "ctor");

        // Required empty public constructor
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, mDebugPrefix + "()");
        }
    }

    /**************** live-cycle ******************/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Global.debugMemory(mDebugPrefix, "onCreate");
        setHasOptionsMenu(true);
        this.mShareActionProvider = new ShareActionProvider(this.getActivity());
        if (savedInstanceState != null) {
            this.mLastVisiblePosition = savedInstanceState.getInt(INSTANCE_STATE_LAST_VISIBLE_POSITION, this.mLastVisiblePosition);
            this.loaderID = savedInstanceState.getInt(INSTANCE_STATE_LOADER_ID, this.loaderID);

            String oldIds = mSelectedItemIds.toString();
            mSelectedItemIds.clear();
            mSelectedItemIds.parse(savedInstanceState.getString(INSTANCE_STATE_SELECTED_ITEM_IDS, oldIds));
            this.mOldAppTitleBeforeMultiselectionStarts = savedInstanceState.getString(INSTANCE_STATE_OLD_TITLE, this.mOldAppTitleBeforeMultiselectionStarts);
            this.mShowSelectedOnly = savedInstanceState.getBoolean(INSTANCE_STATE_SEL_ONLY, this.mShowSelectedOnly);
            if (isMultiSelectionActive()) {
                mMustReplaceMenue = true;
                getActivity().invalidateOptionsMenu();
            }
        } else {
            // first creation of new instance
            loaderID = nextLoaderID++;
        }
        if (!mDebugPrefix.contains("@")) {
            mDebugPrefix += "@" + loaderID + " ";
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mLastVisiblePosition = mGalleryView.getLastVisiblePosition();
        outState.putInt(INSTANCE_STATE_LAST_VISIBLE_POSITION, mLastVisiblePosition);
        outState.putInt(INSTANCE_STATE_LOADER_ID, loaderID);
        outState.putString(INSTANCE_STATE_SELECTED_ITEM_IDS, this.mSelectedItemIds.toString());
        outState.putString(INSTANCE_STATE_OLD_TITLE, this.mOldAppTitleBeforeMultiselectionStarts);
        outState.putBoolean(INSTANCE_STATE_SEL_ONLY, this.mShowSelectedOnly);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Global.debugMemory(mDebugPrefix, "onCreateView");

        // Inflate the layout for this fragment
        View result = inflater.inflate(R.layout.fragment_gallery, container, false);
        mGalleryView = (GridView) result.findViewById(R.id.gridView);

        Activity parent = this.getActivity();
        // mAdapter = new GalleryCursorAdapter(parent, mSelectedItems, mDebugPrefix);

        Intent intent = (parent == null) ? null : parent.getIntent();

        if (Global.debugEnabled && (intent != null)){
            Log.d(Global.LOG_CONTEXT, mDebugPrefix + "onCreateView " + intent.toUri(Intent.URI_INTENT_SCHEME));
        }

        String action = (intent != null) ? intent.getAction() : null;

        if ((action != null) && ((Intent.ACTION_PICK.compareTo(action) == 0) || (Intent.ACTION_GET_CONTENT.compareTo(action) == 0))) {
            this.mMode = (intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE,false)) ? MODE_VIEW_PICK_MULTIBLE : MODE_VIEW_PICK_SINGLE;
            mMustReplaceMenue = true;
            String schema = intent.getScheme();
            mModePickGeoElsePickImaage = ((schema != null) && ("geo".compareTo(schema) == 0));
        }

        String path = (intent == null) ? null : intent.getStringExtra(AffUtils.EXTRA_SELECTED_ITEM_PATHS);

        String filterValue = ((intent != null) && (path == null)) ? intent.getStringExtra(EXTRA_FILTER) : null;
        IGalleryFilter filter = (filterValue != null) ? GalleryFilterParameter.parse(filterValue, new GalleryFilterParameter()) : null;

        if (filter != null) {
            path = filter.getPath();
        }
        mAdapter = new GalleryCursorAdapterFromArray(parent, mSelectedItemIds, mDebugPrefix,
                FileFacade.convert(mDebugPrefix, path));
        mGalleryView.setAdapter(mAdapter);

        mGalleryView.setLongClickable(true);
        mGalleryView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                return onGalleryLongImageClick((GalleryCursorAdapter.GridCellViewHolder) v.getTag(), position);
            }
        });
        mShareActionProvider.setOnShareTargetSelectedListener(new ShareActionProvider.OnShareTargetSelectedListener() {
            @Override
            public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
                if (Global.clearSelectionAfterCommand) {
                    multiSelectionCancel();
                }
                return false;
            }
        });

        mGalleryView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                onGalleryImageClick((GalleryCursorAdapter.GridCellViewHolder) v.getTag(), position);
            }
        });

        this.mParentPathBar = (LinearLayout) result.findViewById(R.id.parent_owner);
        this.mParentPathBarScroller = (HorizontalScrollView) result.findViewById(R.id.parent_scroller);

        this.mChildPathBar = (LinearLayout) result.findViewById(R.id.child_owner);
        // this.mChildPathBarScroller = (HorizontalScrollView) result.findViewById(R.id.child_scroller);

        reloadDirGuiIfAvailable("onCreateView");

        if (!mAdapter.isInArrayMode()) {
            fixMediaDatabase();
        }

        requery("onCreateView");
        updateExifUpdateTask(this.getActivity());

        return result;
    }

    @Override
    public void onAttach(Activity activity) {
        Global.debugMemory(mDebugPrefix, "onAttach");
        super.onAttach(activity);
        mFileCommands.setContext(activity);
        mFileCommands.setLogFilePath(mFileCommands.getDefaultLogFile());
        updateExifUpdateTask(activity);


        if (Global.debugEnabledMemory) {
            Log.d(Global.LOG_CONTEXT, mDebugPrefix + " - onAttach cmd (" +
                    MoveOrCopyDestDirPicker.sFileCommands + ") => (" + mFileCommands +
                    ")");

        }

        MoveOrCopyDestDirPicker.sFileCommands = mFileCommands;
        try {
            mGalleryListener = (OnGalleryInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnGalleryInteractionListener");
        }

        try {
            mDirectoryListener = (DirectoryPickerFragment.OnDirectoryInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement DirectoryPickerFragment.OnDirectoryInteractionListener");
        }
    }

    @Override
    public void onResume() {
        Global.debugMemory(mDebugPrefix, "onResume");
        super.onResume(); // this may destroy an other instance of gallery(fragment)

        final boolean locked = LockScreen.isLocked(this.getActivity());
        if (this.locked != locked) {
            this.locked = locked;
            mMustReplaceMenue = true;
            getActivity().invalidateOptionsMenu();
        }

        if (Global.debugEnabledMemory) {
            Log.d(Global.LOG_CONTEXT, mDebugPrefix + " - onResume cmd (" +
                            MoveOrCopyDestDirPicker.sFileCommands + ") => (" + mFileCommands +
                    ")");

        }

        // workaround fragment lifecycle is newFragment.attach oldFragment.detach.
        // this makes shure that the visible fragment has commands
        MoveOrCopyDestDirPicker.sFileCommands = mFileCommands;
        requeryIfDataHasChanged();
    }

    /**
     * Call back from sub-activities.<br/>
     * Process Change StartTime (longpress start), Select StopTime before stop
     * (longpress stop) or filter change for detailReport
     */
    @Override
    public void onActivityResult(final int requestCode,
                                    final int resultCode, final Intent intent) {
        super.onActivityResult(requestCode,resultCode,intent);

        if (mDestDirPicker != null) mDestDirPicker.onActivityResult(requestCode,resultCode,intent);

        final boolean locked = LockScreen.isLocked(this.getActivity());
        if (this.locked != locked) {
            this.locked = locked;
            mMustReplaceMenue = true;
            getActivity().invalidateOptionsMenu();
        }

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case R.id.cmd_rename_multible:
                    onRenameMultible(PhotoAutoprocessingEditActivity.getAutoprocessingData(intent), AffUtils.getSelectedFiles(intent));
                    break;
            }
        }
    }

    @Override
    public void onDetach() {
        updateExifUpdateTask(null);
        Global.debugMemory(mDebugPrefix, "onDetach");
        super.onDetach();
        mGalleryListener = null;
        mDirectoryListener = null;
        mFileCommands.setContext(null);

        // kill this instance only if not an other instance is active
        if (MoveOrCopyDestDirPicker.sFileCommands == mFileCommands) {
            if (Global.debugEnabledMemory) {
                Log.d(Global.LOG_CONTEXT, mDebugPrefix + " - onDetach cmd (" +
                        MoveOrCopyDestDirPicker.sFileCommands + ") => (null) ");

            }
            MoveOrCopyDestDirPicker.sFileCommands = null;
        } else if (Global.debugEnabledMemory) {
            Log.d(Global.LOG_CONTEXT, mDebugPrefix + " - onDetach cmd [ignore] (" +
                    MoveOrCopyDestDirPicker.sFileCommands + ")");

        }
    }

    @Override
    public void onPause() {
        super.onPause();
        destroyLoaderIfFinishing("onPause");
    }

    @Override
    public void onDestroy() {
        updateExifUpdateTask(null);
        Global.debugMemory(mDebugPrefix, "before onDestroy");

        mDestDirPicker = null;
        destroyLoaderIfFinishing("onDestroy");

        mFileCommands.closeLogFile();
        mFileCommands.closeAll();
        mGalleryContentQuery = null;
        mAdapter = null;
        System.gc();
        Global.debugMemory(mDebugPrefix, "after onDestroy");
        // RefWatcher refWatcher = AndroFotoFinderApp.getRefWatcher(getActivity());
        // refWatcher.watch(this);
        super.onDestroy();
    }

    private void destroyLoaderIfFinishing(String context) {
        if ((getActivity() != null) && (getActivity().isFinishing())) {
            destroyLoader(context);
        }
    }

    //!!! TODO destroyLoader for performance reasons while scanner is active to remove
    private void destroyLoader(String context) {
        if (loaderID != -1) {
            getLoaderManager().destroyLoader(loaderID);
            if ((Global.debugEnabled) && (mCurorLoader != null)) {
                Log.d(Global.LOG_CONTEXT, mDebugPrefix + context + " - releasing mCurorLoader" +
                        mCurorLoader.getDebugContext());
            }
            mCurorLoader = null;
            if (loaderID == (nextLoaderID - 1)) nextLoaderID--;
            loaderID = -1;
        }
    }

    /**
     * interface Queryable: Owning activity tells fragment to change its content:
     * Initiates a database requery in the background
     */
    @Override
    public void requery(Activity context, QueryParameter parameters, String why) {
        this.mGalleryContentQuery = parameters;

        FotoSql.setWhereVisibility(this.mGalleryContentQuery, VISIBILITY.DEFAULT);
        requery(why);
    }

    /**
     * image entries may not have DISPLAY_NAME which is essential for calculating the item-s folder.
     */
    private void repairMissingDisplayNames() {
        SqlJobTaskBase task = new SqlJobTaskBase(this.getActivity(), "Searching media database for missing 'displayname'-s:\n", null) {
            private int mPathColNo = -2;
            private int mResultCount = 0;

            @Override
            protected void doInBackground(Long id, Cursor cursor) {
                if (mPathColNo == -2) mPathColNo = cursor.getColumnIndex(FotoSql.SQL_COL_PATH);
                mResultCount += PhotoPropertiesMediaFilesScanner.getInstance(getActivity()).updatePathRelatedFields(getActivity(), cursor, cursor.getString(mPathColNo), mColumnIndexPK, mPathColNo);
            }

            @Override
            protected void onPostExecute(SelectedItemIds selectedItemIds) {
                if (!isCancelled()) {
                    onMissingDisplayNamesComplete(mStatus);
                    onNotifyPhotoChanged();
                }
            }
        };
        QueryParameter query = FotoSql.queryGetMissingDisplayNames;
        FotoSql.setWhereVisibility(query, VISIBILITY.PRIVATE_PUBLIC);
        task.execute(query);
    }

    private QueryParameter getCurrentQuery() {
        return getCurrentQuery(mGalleryContentQuery);
    }

    private QueryParameter getCurrentQuery(QueryParameter rootQuery) {
        QueryParameter selFilter = new QueryParameter(rootQuery);
        if (mShowSelectedOnly) {
            FotoSql.setWhereSelectionPks(selFilter, mSelectedItemIds);
        }
        selFilter.replaceFrom(FotoSql.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE.toString());
        return selFilter;
    }

    @Override
    public String toString() {
        return mDebugPrefix + this.mAdapter;
    }

    /* --********************** local helper ****************************************** - */
    /** an Image in the FotoGallery was clicked */
    private void onGalleryImageClick(final GalleryCursorAdapter.GridCellViewHolder holder, int position) {
        if ((!multiSelectionHandleClick(holder)) && (mGalleryListener != null)) {
            if (holder.filter != null) {
                QueryParameter subGalleryQuery = new QueryParameter(FotoSql.queryDetail);

                subGalleryQuery.addWhere(holder.filter);
                onOpenChildGallery("Image#filter", subGalleryQuery);
                return;
            }
            long imageID = holder.imageID;
            mGalleryListener.onGalleryImageClick(imageID, getUri(imageID), position);
        }
    }

    private Uri getUri(long imageID) {
        return mAdapter.getUri(imageID);
    }

    private void onOpenChildGallery(String debugContext, QueryParameter subGalleryQuery) {
        if (Global.debugEnabledSql) {
            Log.i(Global.LOG_CONTEXT, "Exec child gallery " +
                    debugContext + "\n\t" + subGalleryQuery.toSqlString());
        }
        FotoGalleryActivity.showActivity("[8]"+debugContext, getActivity(), subGalleryQuery, 0);
    }

    /****************** path navigation *************************/

    private IDirectory mDirectoryRoot = null;
    private int mDirQueryID = 0;

    private String mCurrentPath = null;

    /** Defines Directory Navigation */
    @Override
    public void defineDirectoryNavigation(IDirectory root, int dirTypId, String initialAbsolutePath) {
        mDirectoryRoot = root;
        mDirQueryID = dirTypId;
        navigateTo(initialAbsolutePath);

    }

    /** Set curent selection to absolutePath */
    @Override
    public void navigateTo(String absolutePath) {
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, mDebugPrefix + " navigateTo : " + absolutePath);
        }

        mCurrentPath = absolutePath;
        reloadDirGuiIfAvailable("navigateTo " + absolutePath);
        // requeryGallery(); done by owning activity
    }

    /**
     * path/directory was clicked
     */
    private final View.OnClickListener onPathButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onPathButtonClick((IDirectory) v.getTag());
        }
    };

    private Button createPathButton(IDirectory currentDir) {
        Button result = new Button(getActivity());
        result.setTag(currentDir);
        result.setText(getDirectoryDisplayText(null, currentDir, (FotoViewerParameter.includeSubItems) ? Directory.OPT_SUB_ITEM : Directory.OPT_ITEM));

        result.setOnClickListener(onPathButtonClickListener);
        return result;
    }

    private void reloadDirGuiIfAvailable(String why) {
        if ((mDirectoryRoot != null) && (mCurrentPath != null) && (mParentPathBar != null)) {
            if (Global.debugEnabled) {
                Log.i(Global.LOG_CONTEXT, mDebugPrefix + " reloadDirGuiIfAvailable : " + why);
            }

            mParentPathBar.removeAllViews();
            mChildPathBar.removeAllViews();

            IDirectory selectedChild = mDirectoryRoot.find(mCurrentPath);
            if (selectedChild == null) selectedChild = mDirectoryRoot;

            Button first = null;
            IDirectory current = selectedChild;
            while (current.getParent() != null) {
                Button button = createPathButton(current);
                // add parent left to chlild
                // gui order root/../child.parent/child
                mParentPathBar.addView(button, 0);
                if (first == null) first = button;
                current = current.getParent();
            }

            // scroll to right where deepest child is
            if (first != null) mParentPathBarScroller.requestChildFocus(mParentPathBar, first);

            IDirectory[] children = selectedChild.getChildren();
            if (children != null) {
                for (IDirectory child : children) {
                    Button button = createPathButton(child);
                    mChildPathBar.addView(button);
                }
            }
        }
    }

    /** path/directory was clicked */
    private void onPathButtonClick(IDirectory newSelection) {
        if ((mDirectoryListener != null) && (newSelection != null)) {
            mCurrentPath = newSelection.getAbsolute();
            mDirectoryListener.onDirectoryPick(mCurrentPath, this.mDirQueryID);
        }
    }

    /** getFrom tree display text */
    private static String getDirectoryDisplayText(String prefix, IDirectory directory, int options) {
        StringBuilder result = new StringBuilder();
        if (prefix != null) result.append(prefix);
        result.append(directory.getRelPath()).append(" ");
        Directory.appendCount(result, directory, options);
        return result.toString();
    }


    /********************** Multi selection support ***********************************************************/
    private boolean mMustReplaceMenue = false;

    /** starts mutliselection */
    private boolean onGalleryLongImageClick(final GalleryCursorAdapter.GridCellViewHolder holder, int position) {
        if (!LockScreen.isLocked(this.getActivity())) {
            if (!isMultiSelectionActive()) {
                startMultiSelectionMode();

                mSelectedItemIds.add(holder.imageID);
                holder.icon.setVisibility(View.VISIBLE);
                multiSelectionUpdateActionbar("Start multisel");
            } else {
                // in gallery mode long click is view image
                ImageDetailActivityViewPager.showActivity("[9]", this.getActivity(), getUri(holder.imageID), position, getCurrentQuery(), ImageDetailActivityViewPager.ACTIVITY_ID);
            }
            return true;
        }
        return false; // no multi-selection in lock mode
    }

    private void startMultiSelectionMode() {
        // multi selection not active yet: start multi selection
        if (mOldAppTitleBeforeMultiselectionStarts == null) {
            mOldAppTitleBeforeMultiselectionStarts = getActivity().getTitle().toString();
        }
        mMustReplaceMenue = true;
        mShowSelectedOnly = false;
        getActivity().invalidateOptionsMenu();
    }

    /**
     * Different menu modes
     * 	* normal		name searchbar-icon folder map (tags) (filter) menu
     * 	    * R.menu.menu_gallery_non_selected_only R.menu.menu_gallery_non_multiselect
     * 	* selected	    selected cancel seleted-only share (save) menu
     * 	    * (Filter not available; this.isMultiSelectionActive();
     * 	    * R.menu.menu_gallery_multiselect_mode_all R.menu.menu_image_commands
     * 	* locked		name lock folder map menu
     * 	    * (no multiselection, no base-filters)
     * 	    * (this.locked; R.menu.menu_locked)
     * 	* searchbar	    bar cancel-searc-bar (folder) (map) (tags) (filter) menu
     * 	* picker-locked
     * 	    * R.menu.menu_gallery_pick R.menu.menu_locked
     * 	* picker-non-locked     selected ok cancel filter settings
     * 	    * R.menu.menu_gallery_pick  R.menu.menu_gallery_non_multiselect
     *
     * 	(xxxx) with "IFROOM" (in wide screen only)
     * 	searchbarmode is like "normal" where there is no "IFROOM" on no-searchbar items
     *
     * @param menu
     */
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        this.isMultiSelectionMenuVisible = false;
        super.onPrepareOptionsMenu(menu);
        final boolean locked = LockScreen.isLocked(this.getActivity());
        if (mMustReplaceMenue || (locked != this.locked)) {
            MenuInflater inflater = getActivity().getMenuInflater();

            this.locked = locked;
            mMustReplaceMenue = false;
            menu.clear();
            mMenuRemoveAllSelected = null;
            if (mMode == MODE_VIEW_PICKER_NONE) {
                //
                if (locked) { // view-locked
                    if (isMultiSelectionActive()) {
                        clearSelections();
                    }
                    inflater.inflate(R.menu.menu_locked, menu);
                    LockScreen.removeDangerousCommandsFromMenu(menu);
                    AboutDialogPreference.onPrepareOptionsMenu(getActivity(), menu);
                } else if (this.isMultiSelectionActive()) { // view-multiselect
                    inflater.inflate(R.menu.menu_gallery_multiselect_mode_all, menu);
                    this.isMultiSelectionMenuVisible = true;
                    if (Global.allowRenameMultible) {
                        inflater.inflate(R.menu.menu_gallery_multiselect_rename, menu);
                    }

                    mShareOnlyToggle = menu.findItem(R.id.cmd_selected_only);
                    if (mShowSelectedOnly && (mShareOnlyToggle != null)) {
                        mShareOnlyToggle.setIcon(android.R.drawable.checkbox_on_background);
                        mShareOnlyToggle.setChecked(true);
                    }
                    MenuItem shareItem = menu.findItem(R.id.menu_item_share);
                    shareItem.setActionProvider(mShareActionProvider);
                    // multiSelectionUpdateShareIntent();
                    inflater.inflate(R.menu.menu_image_commands, menu);
                    multiSelectionUpdateShareIntent();
                    Global.fixMenu(getActivity(), menu);

                } else { // view-non-select
                    inflater.inflate(R.menu.menu_gallery_non_selected_only, menu);
                    inflater.inflate(R.menu.menu_gallery_non_multiselect, menu);
                    Global.fixMenu(getActivity(), menu);
                }


            } else { // picker mode
                inflater.inflate(R.menu.menu_gallery_pick, menu);
                if (!locked) {
                    inflater.inflate(R.menu.menu_gallery_non_multiselect, menu);
                } else { // pick-locked
                  LockScreen.removeDangerousCommandsFromMenu(menu);
                }
                AboutDialogPreference.onPrepareOptionsMenu(getActivity(), menu);
            }
            mMenuRemoveAllSelected = menu.findItem(R.id.cmd_selection_remove_all);
        }


        updateSelectionCount();
    }

    protected void updateSelectionCount() {
        if (mMenuRemoveAllSelected != null) {
            mMenuRemoveAllSelected.setVisible(isMultiSelectionActive());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (LockScreen.onOptionsItemSelected(this.getActivity(), menuItem)) {
            this.mMustReplaceMenue = true;
            multiSelectionCancel();
            this.getActivity().invalidateOptionsMenu();
            if (mShareOnlyToggle != null) mShareOnlyToggle.setVisible(false);
            if (mMenuRemoveAllSelected != null) mMenuRemoveAllSelected.setVisible(false);

            return true;
        }

        // Handle menuItem selection
        AndroidFileCommands fileCommands = mFileCommands;

        final SelectedFiles selectedFiles = this.mAdapter.createSelectedFiles(getActivity(), this.mSelectedItemIds);
        if ((mSelectedItemIds != null)
                && (fileCommands.onOptionsItemSelected(
                (FilePermissionActivity) getActivity(), menuItem, selectedFiles, this))) {
            return true;
        }
        switch (menuItem.getItemId()) {
            case R.id.cmd_cancel_multiselect:
                return multiSelectionCancel();
            case R.id.cmd_cancel_pick:
                getActivity().finish();
                return true;
            case R.id.cmd_ok:
                return onPickOk();
            case R.id.cmd_selected_only:
                return multiSelectionToggle();
            case R.id.cmd_backup:
                BackupActivity.showActivity(" menu " + menuItem.getTitle(),
                        getActivity(), selectedFiles, null,
                        getCurrentQuery(), BackupActivity.REQUEST_BACKUP_ID);
                return true;
            case R.id.cmd_copy:
                return cmdMoveOrCopyWithDestDirPicker(false, fileCommands.getLastCopyToPath(), selectedFiles, menuItem.getTitle());
            case R.id.cmd_move:
                return cmdMoveOrCopyWithDestDirPicker(true, fileCommands.getLastCopyToPath(), selectedFiles, menuItem.getTitle());
            case R.id.cmd_rename_multible:
                return cmdRenameMultible(menuItem, selectedFiles);
            case R.id.cmd_show_geo:
                MapGeoPickerActivity.showActivity(" menu " + menuItem.getTitle(),
                        this.getActivity(), selectedFiles, null, null, 0);
                return true;
            case R.id.cmd_edit_geo:
                GeoEditActivity.showActivity(" menu " + menuItem.getTitle(),
                        this.getActivity(), selectedFiles, GeoEditActivity.RESULT_ID);
                return true;
            case R.id.cmd_edit_tags: {
                return tagsShowEditDialog(selectedFiles);
            }
            case R.id.menu_exif:
                return onEditExif(menuItem, selectedFiles);
            case R.id.cmd_selection_add_all:
                addAllToSelection();
                return true;
            case R.id.cmd_selection_remove_all:
                removeAllFromSelection();
                return true;

            case R.id.action_details:
                cmdShowDetails();
                return true;
            case R.id.cmd_scan:
                return fileCommands.cmdMediaScannerWithQuestion(this.getActivity());

            default:
                return super.onOptionsItemSelected(menuItem);
        }

    }

    private void cmdShowDetails() {
        SelectedItemIds ids = getSelectedItemIds();

        final Activity activity = this.getActivity();
        CharSequence subQueryTypName = (activity instanceof FotoGalleryActivity)
                ? ((FotoGalleryActivity)activity).getValueAsTitle(true)
                : null;

        String files = ((ids != null) && (ids.size() > 0)) ? mAdapter.createSelectedFiles(activity, ids).toString().replace(",","\n") : null;
        final Dialog dlg = ImageDetailMetaDialogBuilder.createImageDetailDialog(
                activity,
                getActivity().getTitle().toString(),
                this.toString(),
                ids,
                files,
                (mGalleryContentQuery != null) ? mGalleryContentQuery.toSqlString() : null,
                TagSql.getStatisticsMessage(this.getActivity(), R.string.show_photo, mGalleryContentQuery),
                subQueryTypName);
        dlg.show();
        // setAutoClose(null, dlg, null);
    }

    private void removeDuplicates() {
        SqlJobTaskBase task = new SqlJobTaskBase(this.getActivity(), "Searching for duplcates in media database:\n", null) {
            @Override
            protected void doInBackground(Long id, Cursor cursor) {
                this.mSelectedItemIds.add(id);
                if (mStatus != null) {
                    mStatus
                            .append("\nduplicate found ")
                            .append(id)
                            .append("#")
                            .append(DBUtils.getString(cursor, FotoSql.SQL_COL_DISPLAY_TEXT, "???"))
                    //.append("\n")
                    ;
                }
            }

            @Override
            protected void onPostExecute(SelectedItemIds selectedItemIds) {
                if (!isCancelled()) {
                    if ((selectedItemIds != null) && (selectedItemIds.size() > 0)) {
                        onDuplicatesFound(selectedItemIds, mStatus);
                    } else {
                        onDuplicatesFound(null, mStatus);
                    }
                    onNotifyPhotoChanged();
                } else {
                    if (mStatus != null) {
                        mStatus.append("\nTask canceled");
                        Log.w(Global.LOG_CONTEXT, mDebugPrefix + mStatus);
                    }

                }
            }
        };
        QueryParameter query = FotoSql.queryGetDuplicates;
        FotoSql.setWhereVisibility(query, VISIBILITY.PRIVATE_PUBLIC);
        task.execute(query);
    }

    private boolean onEditExif(MenuItem menuItem, SelectedFiles fotos) {
        PhotoPropertiesEditActivity.showActivity(" menu " + menuItem.getTitle() + "[12]",
                getActivity(), null, null, fotos, 0, true);
        return true;
    }

    private boolean tagsShowEditDialog(SelectedFiles fotos) {
        mTagWorflow = new TagUpdateTask(fotos);
        TagsPickerFragment dlg = new TagsPickerFragment();
        dlg.setFragmentOnwner(this);
        dlg.setTitleId(R.string.tags_edit_menu_title);
        dlg.setAffectedNames(mTagWorflow.getWorkflow().getAffected());
        dlg.setAddNames(new ArrayList<String>());
        dlg.setRemoveNames(new ArrayList<String>());
        dlg.show(getFragmentManager(), "editTags");
        dlg.setBaseQuery(getCurrentQuery());

        ((FotoGalleryActivity) getActivity()).setAutoClose(dlg, null, null);
        mTagPickerDialog = new WeakReference<TagsPickerFragment>(dlg);
        return true;
    }

    /** called by {@link TagsPickerFragment} */
    @Override
    public boolean onCancel(String msg) {
        if (mTagWorflow != null) mTagWorflow.destroy();
        mTagWorflow = null;
        return true;
    }

    /** called by {@link TagsPickerFragment} */
    @Override
    public boolean onOk(List<String> addNames, List<String> removeNames) {
        if (mTagWorflow != null) {
            mTagWorflow.execute(addNames, removeNames);
        }
        mTagWorflow = null;
        return true;
    }

    /** called by {@link TagsPickerFragment} */
    @Override
    public boolean onTagPopUpClick(MenuItem menuItem, int menuItemItemId, Tag selectedTag) {
        if ((mTagPickerDialog == null) || (mTagPickerDialog.get() == null)) return false;
        return TagsPickerFragment.handleMenuShow(mTagPickerDialog.get(), menuItem, selectedTag.getName());
    }


    private void tagsUpdate(TagWorflow fotos, List<String> addNames, List<String> removeNames) {
        fotos.updateTags(addNames,removeNames);
    }

    public static class MoveOrCopyDestDirPicker extends DirectoryPickerFragment {
        protected static AndroidFileCommands sFileCommands = null;

        public static MoveOrCopyDestDirPicker newInstance(boolean move, final SelectedFiles srcFotos) {
            MoveOrCopyDestDirPicker f = new MoveOrCopyDestDirPicker();

            // Supply index input as an argument.
            Bundle args = new Bundle();
            args.putBoolean("move", move);
            AffUtils.putSelectedFiles(args, srcFotos);

            f.setArguments(args);

            return f;
        }

        /** do not use activity callback */
        @Override
        protected void setDirectoryListener(Activity activity) {}

        public boolean getMove() {
            return getArguments().getBoolean("move", false);
        }

        /** overwritten by dialog host to get selected photos for edit autoprocessing mode */
        @Override public SelectedFiles getSrcFotos() {
            return AffUtils.getSelectedFiles(getArguments());
        }

        @Override
        protected void onDirectoryPick(IDirectory selection) {
            // super.onDirectoryPick(selection);
            sFileCommands.onMoveOrCopyDirectoryPick(getMove(), getSrcFotos(), selection);
            dismiss();
        }
    }

    public void notifyPhotoChanged() {
        PhotoChangeNotifyer.notifyPhotoChanged(this.getActivity(), this.mAdapter);
    }


    private boolean cmdRenameMultible(MenuItem menuItem, final SelectedFiles fotos) {
        /*
showActivity(String debugContext, Activity context,
                                    PhotoAutoprocessingDto workflow,
                                    String directoryOrApmFileUrl,
                                    SelectedFiles selectedFiles,
                                    int requestCode)        */
        PhotoAutoprocessingDto workflow = new PhotoAutoprocessingDto();
        PhotoAutoprocessingEditActivity.showActivity(
                "[5]" + " menu " + menuItem.getTitle(), this.getActivity()
                , workflow, null, fotos, menuItem.getItemId(), menuItem.getTitle().toString());
        return true;
    }

    private void onRenameMultible(PhotoAutoprocessingDto autoprocessingData, SelectedFiles selectedFiles) {
        AndroidFileCommands cmd = AndroidFileCommands.createFileCommand(this.getActivity(), true);

        exifUpdate = new UpdateTask(R.string.exif_menu_title, this.getActivity(), cmd, true, autoprocessingData);
        exifUpdate.execute(selectedFiles);

    }

    private static void updateExifUpdateTask(Activity activity) {
        if (exifUpdate != null) {
            if (exifUpdate.isNotFinishedYet()) {
                exifUpdate.setActivity(activity);
            } else {
                exifUpdate.destroy();
                exifUpdate = null;
            }
        }
    }

    private boolean onPickOk() {
        mDestDirPicker = null;
        Activity parent = getActivity();
        List<Uri> resultUri = getSelectedUri(parent, "onPickOk");

        if ((resultUri != null) && (resultUri.size() > 0)) {
            final Intent intent = new Intent();

            intent.setData(resultUri.get(0));
            addAsClip(intent, parent.getContentResolver(), resultUri);
            if (!mModePickGeoElsePickImaage) {
                // permission result.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            parent.setResult(Activity.RESULT_OK, intent);
            parent.finish();

        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void addAsClip(Intent intent, ContentResolver contentResolver, List<Uri> resultUri) {
        if (SUPPORT_MULTISELECTION && (resultUri.size() > 1)) {
            ClipData clip = null;
            for (Uri uri : resultUri) {
                ClipData clipData = ClipData.newUri(contentResolver, uri.toString(), uri);
                if (clip == null) {
                    clip = clipData;
                } else  {
                    clip.addItem(clipData.getItemAt(0));
                }
                intent.setClipData(clip);
            }
        }
    }

    @Nullable
    private List<Uri> getSelectedUri(Activity parent, Object... dbgContext) {
        StringBuilder debugMessage = StringUtils.createDebugMessage(Global.debugEnabledSql, mDebugPrefix, "getSelectedUri", dbgContext);

        List<Uri> resultUri = null;

        SelectedItemIds selectedItemIds = getSelectedItemIds();
        if ((selectedItemIds != null) && (selectedItemIds.size() > 0)) {
            long id = selectedItemIds.first();

            if (resultUri == null) {
                resultUri = new ArrayList<Uri>();
            }
            resultUri.add(getUri(parent, id));
        }
        if (debugMessage != null) {

            StringUtils.appendMessage(debugMessage, "result",
                    ListUtils.toString(" ", resultUri));
            Log.i(Global.LOG_CONTEXT, debugMessage.toString());
        }
        return resultUri;
    }

    private Uri getUri(Activity parent, long id) {
        Uri resultUri = null;
        if (mModePickGeoElsePickImaage) {
            // mode pick gep
            IGeoPoint initialPoint = FotoSql.execGetPosition(null,
                    null, id, mDebugPrefix, "getSelectedUri");

            if (initialPoint != null) {
                GeoUri PARSER = new GeoUri(GeoUri.OPT_PARSE_INFER_MISSING);

                resultUri = Uri.parse(PARSER.toUriString(new GeoPointDto(initialPoint.getLatitude(),initialPoint.getLongitude(), IGeoPointInfo.NO_ZOOM)));
            }


        } else {
            // mode pick image
            resultUri = FotoSql.getUri(id);
        }
        return resultUri;
    }

    private boolean multiSelectionToggle() {
        mShowSelectedOnly = !mShowSelectedOnly;
        mMustReplaceMenue = true;
        getActivity().invalidateOptionsMenu();

        requery("multiSelectionToggle");
        return true;
    }

    private boolean multiSelectionCancel() {
        clearSelections();
        multiSelectionUpdateActionbar("multiSelectionCancel");
        return true;
    }

    private void clearSelections() {
        mSelectedItemIds.clear();

        for (int i = mGalleryView.getChildCount() - 1; i >= 0; i--)
        {
            GalleryCursorAdapter.GridCellViewHolder holder =  (GalleryCursorAdapter.GridCellViewHolder) mGalleryView.getChildAt(i).getTag();
            if (holder != null) {
                holder.icon.setVisibility(View.GONE);
            }
        }
    }

    private void multiSelectionReplaceTitleIfNecessary() {
        if (isMultiSelectionActive()) {
            if (mOldAppTitleBeforeMultiselectionStarts == null) {
                mOldAppTitleBeforeMultiselectionStarts = getActivity().getTitle().toString();
            }
            multiSelectionUpdateActionbar("selection my have changed");
        }

        fix();
    }

    private void multiSelectionUpdateActionbar(String why) {
        String newTitle = null;
        if (!isMultiSelectionActive()) {
            // lost last selection. revert mShowSelectedOnly if neccessary
            if (mShowSelectedOnly) {
                mShowSelectedOnly = false;
                requery(why + "-lost multisel");
            }

            // lost last selection. revert title if neccessary
            if (mOldAppTitleBeforeMultiselectionStarts != null) {
                // last is deselected. Restore title and menu;
                newTitle = mOldAppTitleBeforeMultiselectionStarts;
                mOldAppTitleBeforeMultiselectionStarts = null;
                getActivity().invalidateOptionsMenu();
            }
        } else {
            if (!this.isMultiSelectionMenuVisible) {
                startMultiSelectionMode();
            }
            // multi selection is active: update title and data for share menue
            newTitle = getActivity().getString(R.string.selection_status_format, mSelectedItemIds.size());
            multiSelectionUpdateShareIntent();
        }

        if (newTitle != null) {
            getActivity().setTitle(newTitle);
        }
        updateSelectionCount();
    }

    private void multiSelectionUpdateShareIntent() {
        int selectionCount = mSelectedItemIds.size();
        if ((selectionCount > 0) && (mShareActionProvider != null)) {
            Intent sendIntent = new Intent();
            sendIntent.setType("image/*");
            if (selectionCount == 1) {
                Long imageId = mSelectedItemIds.first();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(EXTRA_STREAM, getUri(imageId));
            } else {
                sendIntent.setAction(Intent.ACTION_SEND_MULTIPLE);

                ArrayList<Uri> uris = new ArrayList<Uri>();

                for (Long itemId : mSelectedItemIds) {
                    uris.add(getUri(itemId));
                }
                sendIntent.putParcelableArrayListExtra(EXTRA_STREAM, uris);
            }

            try {
                String debugContext = "send";
                ActivityWithCallContext.additionalCallContext = debugContext;
                ActivityWithCallContext.addContext(debugContext, sendIntent, getActivity());

                mShareActionProvider.setShareIntent(sendIntent);
                this.mNoShareError = true;
            } catch (Exception e) {
                if (this.mNoShareError) {
                    Toast.makeText(this.getActivity(), R.string.share_err_to_many, Toast.LENGTH_LONG).show();
                    this.mNoShareError = false; // do not show it again
                }
            }
        }

    }
    private void removeAllFromSelection() {
        SqlJobTaskBase task = new SqlJobTaskBase(this.getActivity(), "removeAllFromSelection", this.mSelectedItemIds) {
            @Override
            protected void doInBackground(Long id, Cursor cursor) {
                this.mSelectedItemIds.remove(id);
            }

            @Override
            protected void onPostExecute(SelectedItemIds selectedItems) {
                replaceSelectedItems(selectedItems, mStatus, mDebugPrefix);
            }
        };
        QueryParameter query = getCurrentQuery();
        FotoSql.setWhereVisibility(query, VISIBILITY.DEFAULT);
        task.execute(query);
    }

    private void addAllToSelection() {
        SqlJobTaskBase task = new SqlJobTaskBase(this.getActivity(), "addAllToSelection", this.mSelectedItemIds) {
            @Override
            protected void doInBackground(Long id, Cursor cursor) {
                this.mSelectedItemIds.add(id);
            }

            @Override
            protected void onPostExecute(SelectedItemIds selectedItems) {
                replaceSelectedItems(selectedItems, mStatus, mDebugPrefix);
            }
        };
        QueryParameter query = getCurrentQuery();
        FotoSql.setWhereVisibility(query, VISIBILITY.PRIVATE_PUBLIC);
        task.execute(query);
    }

    private void replaceSelectedItems(SelectedItemIds selectedItemIds, StringBuffer debugMessage, String why) {
        int oldSize = mSelectedItemIds.size();
        this.mSelectedItemIds.clear();
        this.mSelectedItemIds.addAll(selectedItemIds);
        int newSize = mSelectedItemIds.size();

        if (debugMessage != null) {
            debugMessage.append("\nSelections ").append(oldSize).append("=>").append(newSize);
            Log.i(Global.LOG_CONTEXT, debugMessage.toString());
        }
        if ((oldSize == 0) && (newSize > 0)) {
            startMultiSelectionMode();
        }
        multiSelectionUpdateActionbar(why);
        requery(why);
    }

    //-------------------------------------------------------------

    private void fixMediaDatabase() {
        if (!PhotoPropertiesMediaFilesScanner.isScannerActive(getActivity().getContentResolver())) {
            if (Global.debugEnabled) {
                Log.d(Global.LOG_CONTEXT, "Analysing media database for potential problems");
            }
            repairMissingDisplayNames();
            removeDuplicates();
        }
    }

    /**
     * is called when removeDuplicates() found duplicates
     */
    private void onDuplicatesFound(SelectedItemIds selectedItemIds, StringBuffer debugMessage) {
        if (debugMessage != null) {
            Log.w(Global.LOG_CONTEXT, mDebugPrefix + debugMessage);
        }

        if (selectedItemIds != null) {
            QueryParameter query = new QueryParameter();
            FotoSql.setWhereSelectionPks(query, selectedItemIds);

            final Activity activity = getActivity();

            // might be null in in orientation change
            if (activity != null) {
                int delCount = 0;

                String sqlWhere = query.toAndroidWhere(); //  + " OR " + FotoSql.SQL_COL_PATH + " is null";
                try {
                    delCount = FotoSql.getMediaDBApi().deleteMedia(mDebugPrefix + "onDuplicatesFound", sqlWhere, null, true);
                } catch (Exception ex) {
                    Log.w(Global.LOG_CONTEXT, "deleteMedia via update failed for 'where " + sqlWhere +
                            "'.");
                }
                if (debugMessage != null) {
                    Log.w(Global.LOG_CONTEXT, mDebugPrefix + " deleted " + delCount +
                            " duplicates\n\tDELETE ... WHERE " + sqlWhere);
                }

                if (delCount > 0) {
                    requery("after delete duplicates"); // content has changed: must refreshLocal
                }
            }
        }
    }

    /**
     * called after MissingDisplayNamesComplete finished
     */
    private void onMissingDisplayNamesComplete(StringBuffer debugMessage) {
        if (debugMessage != null) {
            Log.w(Global.LOG_CONTEXT, mDebugPrefix + debugMessage);
        }
    }

    protected class LocalFileCommands extends AndroidFileCommands {

        @Override
        protected void onPostProcess(String what, int opCode, SelectedFiles selectedFiles, int modifyCount, int itemCount, IFile[] oldPathNames, IFile[] newPathNames) {
            if (Global.clearSelectionAfterCommand || (opCode == OP_DELETE) || (opCode == OP_MOVE)) {
                mShowSelectedOnly = true;
                multiSelectionCancel();
            }

            super.onPostProcess(what, opCode, selectedFiles, modifyCount, itemCount, oldPathNames, newPathNames);

            if ((mAdapter.isInArrayMode()) && ((opCode == OP_RENAME) || (opCode == OP_MOVE) || (opCode == OP_DELETE))) {
                mAdapter.refreshLocal();
                mGalleryView.setAdapter(mAdapter);
            }
            if ((opCode == OP_RENAME) || (opCode == OP_MOVE) || (opCode == OP_DELETE) || (opCode == OP_RENAME)) {
                requeryIfDataHasChanged();
            }
        }
    }

    class LocalCursorLoader implements LoaderManager.LoaderCallbacks<Cursor> {
        /**
         * called by LoaderManager.getLoader(ACTIVITY_ID) to (re)create loader
         * that attaches to last query/cursor if it still exist i.e. after rotation
         */
        @Override
        public Loader<Cursor> onCreateLoader(int aLoaderID, Bundle bundle) {
            if (loaderID == aLoaderID) {
                QueryParameter query = getCurrentQuery();
                mRequeryInstanceCount++;
                if (Global.debugEnabledSql) {
                    Log.i(Global.LOG_CONTEXT, mDebugPrefix + " onCreateLoader"
                            + getDebugContext() +
                            " : query = " + query);
                }
                return FotoSql.createCursorLoader(getActivity().getApplicationContext(), query);
            }

            // An invalid id was passed in
            return null;
        }

        /**
         * called after media db content has changed
         */
        @Override
        public void onLoadFinished(Loader<Cursor> _loader, Cursor data) {
            mLastVisiblePosition = mGalleryView.getLastVisiblePosition();

            final Activity context = getActivity();
            if (data == null) {
                CursorLoaderWithException loader = (CursorLoaderWithException) _loader;
                String title;
                String message = context.getString(R.string.global_err_sql_message_format, loader.getException().getMessage(), loader.getQuery().toSqlString());
                if (loader.getException() != null) {
                    if (0 != loader.getQuery().toSqlString().compareTo(getCurrentQuery(FotoSql.queryDetail).toSqlString())) {
                        // query is not default query. revert to default query
                        mGalleryContentQuery = FotoSql.queryDetail;
                        requery("requery after query-errror");
                        title = context.getString(R.string.global_err_sql_title_reload);
                    } else {
                        title = context.getString(R.string.global_err_system);
                        context.finish();
                    }
                    Dialogs.messagebox(context, title, message, null);
                    return;
                }
            }

            mUpdateId = FotoSql.getMediaDBApi().getCurrentUpdateId();
            // do change the data
            mAdapter.swapCursor(data);

            if (mLastVisiblePosition > 0) {
                mGalleryView.smoothScrollToPosition(mLastVisiblePosition);
                mLastVisiblePosition = -1;
            }

            final int resultCount = (data == null) ? 0 : data.getCount();
            if (Global.debugEnabled) {
                Log.i(Global.LOG_CONTEXT, mDebugPrefix + " onLoadFinished"
                        + getDebugContext() +
                        " fount " + resultCount + " rows");
            }

            // do change the data
            notifyPhotoChanged();

            if (mLastVisiblePosition > 0) {
                mGalleryView.smoothScrollToPosition(mLastVisiblePosition);
                mLastVisiblePosition = -1;
            }

            // show the changes

            if (context instanceof OnGalleryInteractionListener) {
                ((OnGalleryInteractionListener) context).setResultCount(resultCount);
            }
            multiSelectionReplaceTitleIfNecessary();
        }

        /**
         * called by LoaderManager. after search criteria were changed or if activity is destroyed.
         */
        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            if (Global.debugEnabled) {
                Log.i(Global.LOG_CONTEXT, mDebugPrefix + " onLoaderReset" + getDebugContext());
            }
            // rember position where we have to scroll to after refreshLocal is finished.
            mLastVisiblePosition = mGalleryView.getLastVisiblePosition();

            mAdapter.swapCursor(null);
            notifyPhotoChanged();
        }

        @NonNull
        protected String getDebugContext() {
            return "(@" + loaderID + ", #" + mRequeryInstanceCount +
                    ", LastVisiblePosition=" + mLastVisiblePosition +
//                    ",  Path='" + mInitialFilePath +
                    "')";
        }
    }

    private boolean isMultiSelectionActive() {
        if (mMode != MODE_VIEW_PICKER_NONE) return true;
        return !mSelectedItemIds.isEmpty();
    }

    /** return true if multiselection is active */
    private boolean multiSelectionHandleClick(GalleryCursorAdapter.GridCellViewHolder holder) {
        if (isMultiSelectionActive()) {
            long imageID = holder.imageID;
            holder.icon.setVisibility(toggleSelection(imageID) ? View.VISIBLE : View.GONE);
            multiSelectionUpdateActionbar("changed mutli sel");
            return true;
        }
        multiSelectionUpdateActionbar("lost multi sel");
        return false;
    }

    private void fix() {
        if (((mRequeryInstanceCount > 2) && (LibGlobal.itpcWriteSupport))) {
            View iptc = ResourceUtils.findLast(this.mGalleryView.getRootView(), "ads");
            if (iptc != null) {
                ((ViewGroup) iptc.getParent()).removeView(iptc);
            }
        }
    }

    /** return true if included; false if excluded */
    private boolean toggleSelection(long imageID) {
        boolean contains = mSelectedItemIds.contains(imageID);
        if (mMode == MODE_VIEW_PICK_SINGLE) {
            clearSelections();
        }
        if (contains) {
            mSelectedItemIds.remove(imageID);
            return false;
        } else {
            mSelectedItemIds.add(imageID);
            return true;
        }
    }

    private class TagUpdateTask extends TagTask<List<String>> {

        TagUpdateTask(SelectedFiles fotos) {
            super(GalleryCursorFragment.this.getActivity(), R.string.tags_activity_title);
            this.getWorkflow().init(GalleryCursorFragment.this.getActivity(), fotos, null);

        }

        @Override
        protected Integer doInBackground(List<String>... params) {
            return getWorkflow().updateTags(params[0], params[1]);
        }

        @Override
        protected void onPostExecute(Integer itemCount) {
            super.onPostExecute(itemCount);
            onNotifyPhotoChanged();
        }

    }

}

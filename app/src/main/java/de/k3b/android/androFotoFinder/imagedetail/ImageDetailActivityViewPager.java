/*******************************************************************************
 * Copyright 2011, 2012 Chris Banes.
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
package de.k3b.android.androFotoFinder.imagedetail;

import android.app.ActionBar;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.PopupMenu;
import android.widget.Toast;

// import com.squareup.leakcanary.RefWatcher;

import org.osmdroid.api.IGeoPoint;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.k3b.android.androFotoFinder.AffUtils;
import de.k3b.android.androFotoFinder.Common;
import de.k3b.android.androFotoFinder.ExifEditActivity;
import de.k3b.android.androFotoFinder.FotoGalleryActivity;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.LockScreen;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.SettingsActivity;
import de.k3b.android.androFotoFinder.directory.DirectoryPickerFragment;
import de.k3b.android.androFotoFinder.locationmap.GeoEditActivity;
import de.k3b.android.androFotoFinder.locationmap.MapGeoPickerActivity;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.tagDB.TagSql;
import de.k3b.android.androFotoFinder.tagDB.TagTask;
import de.k3b.android.androFotoFinder.tagDB.TagsPickerFragment;
import de.k3b.android.util.AndroidFileCommands;
import de.k3b.android.util.FileManagerUtil;
import de.k3b.android.util.IntentUtil;
import de.k3b.android.util.MediaScanner;
import de.k3b.android.util.MediaScannerAsyncTask;
import de.k3b.android.util.OsUtils;
import de.k3b.android.widget.AboutDialogPreference;
import de.k3b.android.widget.Dialogs;
import de.k3b.android.widget.LocalizedActivity;
import de.k3b.database.QueryParameter;
import de.k3b.io.collections.SelectedFiles;
import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.io.GeoUri;
import de.k3b.io.FileUtils;
import de.k3b.io.GalleryFilterParameter;
import de.k3b.io.IDirectory;
import de.k3b.io.StringUtils;
import de.k3b.media.MediaUtil;
import de.k3b.tagDB.Tag;

/**
 * Shows a zoomable imagee.<br>
 * Swipe left/right to show previous/next image.
 */

public class ImageDetailActivityViewPager extends LocalizedActivity implements Common, TagsPickerFragment.ITagsPicker {
    private static final String INSTANCE_STATE_MODIFY_COUNT = "mModifyCount";
    private static final String INSTANCE_STATE_LAST_SCROLL_POSITION = "lastScrollPosition";
    /** #70: remember on config change (screen rotation) */
    private static final String INSTANCE_STATE_ContextName = "ContextName";
    private static final String INSTANCE_STATE_ContextMenuId = "ContextMenuId";

    /** #70: part of sql column expression for ExtraDetailExpression used to find old defintion ot delete */
    private static final String CONTEXT_COLUMN_ALIAS = " as " + ImagePagerAdapterFromCursor.CONTEXT_COLUMN_FIELD;

    public static final int ACTIVITY_ID = 76621;

    /** activityRequestCode: in forward mode: intent is forwarded to gallery */
    private static final int ACTION_RESULT_FORWARD = 471102;

    // #64: after image edit rescan file
    private static final int ACTION_RESULT_MUST_MEDIA_SCAN = 47103;

    private static final int DEFAULT_SORT = FotoSql.SORT_BY_NAME_LEN;
    private static final QueryParameter DEFAULT_QUERY = FotoSql.queryDetail;
    private static final int NO_INITIAL_SCROLL_POSITION = -1;

    /** if smaller that these millisecs then the actionbar autohide is disabled */
    private static final int DISABLE_HIDE_ACTIONBAR = 700;
    private static final int NOMEDIA_GALLERY = 8227;

    // how many changes have been made. if != 0 parent activity must invalidate cached data
    private static int mModifyCount = 0;
    private MenuItem mMenuSlideshow = null;

    private boolean mWaitingForMediaScannerResult = false;

	MoveOrCopyDestDirPicker mDestDirPicker = null;
	
    private LocalCursorLoader mCurorLoader;

    // private static final String ISLOCKED_ARG = "isLocked";

    private LockableViewPager mViewPager = null;
    private ImagePagerAdapterFromCursorArray mAdapter = null;
    private TagUpdateTask mTagWorflow = null;

    private final AndroidFileCommands mFileCommands = new LocalFileCommands();

    // for debugging
    private static int id = 1;
    private String mDebugPrefix;

    /** where data comes from */
    private QueryParameter mGalleryContentQuery = null;
    private GalleryFilterParameter mFilter = null;

    /** if >= 0 after load cursor scroll to this offset */
    private int mInitialScrollPosition = NO_INITIAL_SCROLL_POSITION;

    /** if not null: after load cursor scroll to this path */
    private String mInitialFilePath = null;

    /** after 2 secs of user inactive the actionbar is hidden until the screen is touched */
    private Handler mActionBarHideTimer = null;

    /** #70: handles ExtraDetail definition */
    private ImageContextController mImageContextController = null;

    /** #70: optinal sql expression to be shown in detailview */
    private String mContextColumnExpression = null; // sql field expression. Result will be displayed in ImageView Context area
    private String mContextName;                    // name of current ImageView Context persisted in bundle
    private boolean mMustReplaceMenue       = false;
    private boolean locked = false; // if != Global.locked : must update menu

    /** executes sql to load image detail data in a background task that may survive
     * conriguration change (i.e. device rotation) */
    class LocalCursorLoader implements LoaderManager.LoaderCallbacks<Cursor> {
        /** incremented every time a new curster/query is generated */
        private int mRequeryInstanceCount = 0;

        /** called by LoaderManager.getLoader(ACTIVITY_ID) to (re)create loader
         * that attaches to last query/cursor if it still exist i.e. after rotation */
        @Override
        public Loader<Cursor> onCreateLoader(int loaderID, Bundle bundle) {
            switch (loaderID) {
                case ACTIVITY_ID:
                    mRequeryInstanceCount++;
                    mWaitingForMediaScannerResult = false;
                    if (Global.debugEnabledSql) {
                        Log.i(Global.LOG_CONTEXT, mDebugPrefix + " onCreateLoader" +
                                getDebugContext() +
                                " : query = " + mGalleryContentQuery);
                    }
                    return FotoSql.createCursorLoader(getApplicationContext(), mGalleryContentQuery);
                default:
                    // An invalid id was passed in
                    return null;
            }
        }

        /** called after media db content has changed */
        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            // to be restored after refreshLocal if there is no mInitialFilePath
            if ((mInitialScrollPosition == NO_INITIAL_SCROLL_POSITION) && (mViewPager != null)) {
                mInitialScrollPosition = mViewPager.getCurrentItem();
            }
            // do change the data
            mAdapter.swapCursor(data);

            // restore position is invalid
            final int newItemCount = mAdapter.getCount();

            if (((newItemCount == 0)) || (mInitialScrollPosition >= newItemCount)) mInitialScrollPosition = NO_INITIAL_SCROLL_POSITION;

            if (Global.debugEnabledSql) {
                Log.i(Global.LOG_CONTEXT, mDebugPrefix + " onLoadFinished" +
                        getDebugContext() +
                        " found " + ((data == null) ? 0 : newItemCount) + " rows");
            }

            // do change the data
            mAdapter.notifyDataSetChanged();
            mViewPager.setAdapter(mAdapter);

            // show the changes
            onLoadCompleted();
        }

        /** called by LoaderManager. after search criteria were changed or if activity is destroyed. */
        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            // rember position where we have to scroll to after refreshLocal is finished.
            mInitialScrollPosition = mViewPager.getCurrentItem();
            mAdapter.swapCursor(null);
            if (Global.debugEnabledSql) {
                Log.i(Global.LOG_CONTEXT, mDebugPrefix + " onLoaderReset" +
                        getDebugContext());
            }
            mAdapter.notifyDataSetChanged();
        }

        @NonNull
        private String getDebugContext() {
            return "(#" + mRequeryInstanceCount
                    + ", mScrollPosition=" + mInitialScrollPosition +
                    ",  Path='" + mInitialFilePath +
                    "')";
        }
    }

    class LocalFileCommands extends AndroidFileCommands {
        @Override
        protected void onPostProcess(String what, int opCode, SelectedFiles selectedFiles, int modifyCount, int itemCount, String[] oldPathNames, String[] newPathNames) {
            mInitialFilePath = null;
            switch (opCode) {
                case OP_MOVE:
                case OP_RENAME:
                    if ((newPathNames!= null) && (newPathNames.length > 0)) {
                        // so selection will be restored to this after load complete
                        mInitialFilePath = newPathNames[0];
                    }
                    break;
                case OP_COPY:
                    if ((oldPathNames!= null) && (oldPathNames.length > 0)) {
                        // so selection will be restored to this after load complete
                        mInitialFilePath = oldPathNames[0];
                    }
                    break;
                default:break;
            }

            super.onPostProcess(what, opCode, selectedFiles, modifyCount, itemCount, oldPathNames, newPathNames);

            if ((opCode == OP_RENAME) || (opCode == OP_MOVE) || (opCode == OP_DELETE)) {
                refreshIfNecessary();
            }
        }

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

        /* do not use activity callback */
        @Override
        protected void setDirectoryListener(Activity activity) {/* do not use activity callback */}

        public boolean getMove() {
            return getArguments().getBoolean("move", false);
        }

        public SelectedFiles getSrcFotos() {
            return AffUtils.getSelectedFiles(getArguments());
        }

        /**
         * To be overwritten to check if a path can be picked.
         *
         * @param path to be checked if it cannot be handled
         * @return null if no error else error message with the reason why it cannot be selected
         */
        @Override
        protected String getStatusErrorMessage(String path) {
            String errorMessage = (sFileCommands == null) ? null : sFileCommands.checkWriteProtected(0, new File(path));
            if (errorMessage != null) {
                int pos = errorMessage.indexOf('\n');
                return (pos > 0) ? errorMessage.substring(0,pos) : errorMessage;
            }
            return super.getStatusErrorMessage(path);
        }

        @Override
        protected void onDirectoryPick(IDirectory selection) {
            // super.onDirectoryPick(selection);
            mModifyCount++; // copy or move initiated
            getActivity().setResult((mModifyCount == 0) ? RESULT_NOCHANGE : RESULT_CHANGE);

            sFileCommands.onMoveOrCopyDirectoryPick(getMove(), getSrcFotos(), selection);
            dismiss();
        }
    };

    private class TagUpdateTask extends TagTask<List<String>> {

        TagUpdateTask(SelectedFiles fotos) {
            super(ImageDetailActivityViewPager.this,R.string.tags_activity_title);
            this.getWorkflow().init(ImageDetailActivityViewPager.this, fotos, null);

        }

        @Override
        protected Integer doInBackground(List<String>... params) {
            return getWorkflow().updateTags(params[0], params[1]);
        }

    }

    /**
     * shows a new instance of ImageDetailActivityViewPager.
     *
     * @param context calling activity
     * @param imageUri if != null initial image to show
     * @param position offset of image to display in query or current file directory
     * @param imageDetailQuery if != null set initial filter to new FotoGalleryActivity
     * @param requestCode if != 0 start for result. else start without result
     */
    public static void showActivity(Activity context, Uri imageUri, int position, QueryParameter imageDetailQuery, int requestCode) {
        Intent intent;
        //Create intent
        intent = new Intent(context, ImageDetailActivityViewPager.class);

        if (imageDetailQuery != null) {
            intent.putExtra(ImageDetailActivityViewPager.EXTRA_QUERY, imageDetailQuery.toReParseableString());
        }
        intent.putExtra(ImageDetailActivityViewPager.EXTRA_POSITION, position);
        intent.setData(imageUri);

        if (requestCode != 0) {
            context.startActivityForResult(intent, requestCode);
        } else {
            context.startActivity(intent);
        }
    }

    @Override
	public void onCreate(Bundle savedInstanceState) {
        mDebugPrefix = "ImageDetailActivityViewPager#" + (id++)  + " ";
        Global.debugMemory(mDebugPrefix, "onCreate");
        this.mWaitingForMediaScannerResult = false;

        // #17: let actionbar overlap image so there is no need to resize main view item
        // http://stackoverflow.com/questions/6749261/custom-translucent-android-actionbar
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        super.onCreate(savedInstanceState);
        SettingsActivity.prefs2Global(this);
        Intent intent = getIntent();

        if (Global.debugEnabled && (intent != null)){
            Log.d(Global.LOG_CONTEXT, mDebugPrefix + "onCreate " + intent.toUri(Intent.URI_INTENT_SCHEME));
        }

        if (mustForward(intent)) {
            // cannot handle myself. Forward to FotoGalleryActivity
            Intent childIntent = new Intent();
            childIntent.setClass(this, FotoGalleryActivity.class);
            childIntent.setAction(intent.getAction());
            IntentUtil.setDataAndTypeAndNormalize(childIntent, intent.getData(), intent.getType());
            copyExtras(childIntent, intent.getExtras(),
                    EXTRA_FILTER, EXTRA_POSITION, EXTRA_QUERY, AffUtils.EXTRA_SELECTED_ITEM_IDS, AffUtils.EXTRA_SELECTED_ITEM_DATES,
                    AffUtils.EXTRA_SELECTED_ITEM_PATHS, EXTRA_STREAM, EXTRA_TITLE);
            startActivityForResult(childIntent, ACTION_RESULT_FORWARD);
        } else { // not in forward mode
            setContentView(R.layout.activity_image_view_pager);

            mViewPager = (LockableViewPager) findViewById(R.id.view_pager);
            setContentView(mViewPager);

            // extra parameter
            getParameter(intent);

            mAdapter = new ImagePagerAdapterFromCursorArray(this, mDebugPrefix, mInitialFilePath);
            if (savedInstanceState != null) {
                String querySql = savedInstanceState.getString(EXTRA_QUERY);
                if (querySql != null) {
                    this.mGalleryContentQuery = QueryParameter.parse(querySql);
                }

                mContextName = savedInstanceState.getString(INSTANCE_STATE_ContextName, mContextName);

                int lastMenuId = savedInstanceState.getInt(INSTANCE_STATE_ContextMenuId, 0);
                if (lastMenuId != 0) {
                    PopupMenu menu = new PopupMenu(this, mViewPager);
                    onCreateOptionsMenu(menu.getMenu());
                    MenuItem menuItem = menu.getMenu().findItem(lastMenuId);
                    if (menuItem != null) {
                        mContextName = menuItem.getTitle().toString();
                        mAdapter.setContext(menuItem);
                    }
                }

                setContextMode(mContextName);
            }

            mViewPager.setAdapter(mAdapter);

            mViewPager.setOnInterceptTouchEvent(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onGuiTouched();
                }
            });

            if (savedInstanceState != null) {
                mInitialScrollPosition = savedInstanceState.getInt(INSTANCE_STATE_LAST_SCROLL_POSITION, this.mInitialScrollPosition);
                mModifyCount = savedInstanceState.getInt(INSTANCE_STATE_ContextMenuId, this.mModifyCount);
            } else {
                mModifyCount = 0;
            }

            if ((mInitialFilePath != null) && (mInitialScrollPosition ==-1)) {
                mInitialScrollPosition = mAdapter.getPositionFromPath(mInitialFilePath);
            }
            if ((mInitialScrollPosition >= 0) && (mInitialScrollPosition < mAdapter.getCount())) {
                mViewPager.setCurrentItem(mInitialScrollPosition);
            }

            setResult((mModifyCount == 0) ? RESULT_NOCHANGE : RESULT_CHANGE);

            mFileCommands.setContext(this);
            mFileCommands.setLogFilePath(mFileCommands.getDefaultLogFile());

            if (Global.debugEnabledMemory) {
                Log.d(Global.LOG_CONTEXT, mDebugPrefix + " - onCreate cmd (" +
                        MoveOrCopyDestDirPicker.sFileCommands + ") => (" + mFileCommands +
                        ")");

            }

            MoveOrCopyDestDirPicker.sFileCommands = mFileCommands;

            if (mAdapter.getCount() == 0) {
                // if cursor does not contain data via file
                mCurorLoader = new LocalCursorLoader();
                getLoaderManager().initLoader(ACTIVITY_ID, null, mCurorLoader);
            }
            // #17: make actionbar background nearly transparent
            // http://stackoverflow.com/questions/6749261/custom-translucent-android-actionbar
            getActionBar().setBackgroundDrawable(new ColorDrawable(Color.argb(128, 0, 0, 0)));
        }
        unhideActionBar(Global.actionBarHideTimeInMilliSecs, "onCreate");
    }


    private void onGuiTouched() {
        // stop slideshow if active
        if (mSlideShowStarted) startStopSlideShow(false);
        unhideActionBar(Global.actionBarHideTimeInMilliSecs, "onGuiTouched");
    }

    private void unhideActionBar(int milliSecsUntilHide, String why) {
        final int ACTIONBAR_HIDE_HANDLER_ID = 3;
        boolean timer = milliSecsUntilHide >= DISABLE_HIDE_ACTIONBAR;

        ActionBar bar = getActionBar();

        if ((bar != null) && (!bar.isShowing())) {
            if (Global.debugEnabled) {
                Log.d(Global.LOG_CONTEXT, mDebugPrefix + "unhiding ActionBar(timer=" + timer +
                        ") " + why);
            }
            bar.show();
        }

        if (timer) {
            if (mActionBarHideTimer == null) {
                mActionBarHideTimer = new Handler() {
                    public void handleMessage(Message m) {
                        ActionBar bar = getActionBar();

                        if ((bar != null) && (bar.isShowing())) {
                            if (Global.debugEnabled) {
                                Log.d(Global.LOG_CONTEXT, mDebugPrefix + "hiding ActionBar");
                            }
                            bar.hide();
                        }
                    }
                };

            }
            mActionBarHideTimer.removeMessages(ACTIONBAR_HIDE_HANDLER_ID);
            mActionBarHideTimer.sendMessageDelayed(Message.obtain(mActionBarHideTimer, ACTIONBAR_HIDE_HANDLER_ID), milliSecsUntilHide);
        } else {
            if (mActionBarHideTimer != null) {
                mActionBarHideTimer.removeMessages(ACTIONBAR_HIDE_HANDLER_ID);
                mActionBarHideTimer = null;
            }
        }
    }

    private void copyExtras(Intent dest, Bundle source, String... keys) {
        if ((dest != null) && (source != null) && (keys != null)) {
            for (String key : keys) {
                Object item = source.get(key);
                if (item != null) {
                    dest.putExtra(key, item.toString());
                }
            }
        }
    }

    @Override
    protected void onActivityResult(final int requestCode,
                                    final int resultCode, final Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (mDestDirPicker != null) mDestDirPicker.onActivityResult(requestCode,resultCode,intent);

        final boolean locked = LockScreen.isLocked(this);
        if (this.locked != locked) {
            this.locked = locked;
            mMustReplaceMenue = true;
            invalidateOptionsMenu();
        }

        if (requestCode == ACTION_RESULT_FORWARD) {
            // forward result from child-activity to parent-activity
            setResult(resultCode, intent);
            finish();
        } else if (requestCode == ACTION_RESULT_MUST_MEDIA_SCAN) {
            // #64 after edit the content might have been changed. update media DB.
            String orgiginalFileToScan = getCurrentFilePath();

            if (orgiginalFileToScan != null) {
                MediaScanner.getInstance(this).updateMediaDatabase_Android42(this, null, orgiginalFileToScan);
            }
        }

        refreshIfNecessary();
    }

    private static boolean mustForward(Intent intent) {
        Uri uri = IntentUtil.getUri(intent);
        File file = IntentUtil.getFile(uri);

        // probably content: url
        if (file == null) return IntentUtil.isFileUri(uri); // true if file:// with wildcards "%" or "*" goto gallery;

        // file with wildcard, directory or no read permissions
        return (!file.exists() || !file.isFile() || !file.canRead());
    }

    private void setFilter(GalleryFilterParameter value) {
        if (value != null) {
            QueryParameter query = new QueryParameter(DEFAULT_QUERY);
            FotoSql.setSort(query, DEFAULT_SORT, true);
            TagSql.filter2QueryEx(query, value, true);
            addContextColumn(query, mContextColumnExpression);
            mGalleryContentQuery = query;
        }
        this.mFilter = value; // #34

    }

    /** query from EXTRA_QUERY, EXTRA_FILTER, fileParentDir , defaultQuery */
    private void getParameter(Intent intent) {
        this.mInitialScrollPosition = intent.getIntExtra(EXTRA_POSITION, this.mInitialScrollPosition);
        this.mGalleryContentQuery = QueryParameter.parse(intent.getStringExtra(EXTRA_QUERY));

        this.mFilter = null;
        if (mGalleryContentQuery == null) {
            String filterValue = intent.getStringExtra(EXTRA_FILTER);
            if (filterValue != null) {
                setFilter(GalleryFilterParameter.parse(filterValue, new GalleryFilterParameter()));
            }
        }

        if (mGalleryContentQuery == null) {
            this.mInitialScrollPosition = NO_INITIAL_SCROLL_POSITION;
            String path = IntentUtil.getFilePath(this, IntentUtil.getUri(intent));
            if (path != null) {
                setFilter(getParameterFromPath(path));
            }
        }

        if ((mGalleryContentQuery == null) && (mFilter == null)) {
            Log.e(Global.LOG_CONTEXT, mDebugPrefix + " onCreate() : parameter Not found " + EXTRA_QUERY +
                    ", " + EXTRA_FILTER + ", ] not found. Using default.");
            mGalleryContentQuery = new QueryParameter(DEFAULT_QUERY);
            this.mInitialScrollPosition = NO_INITIAL_SCROLL_POSITION;
            addContextColumn(mGalleryContentQuery, mContextColumnExpression);
            //mInitialFilePath = path;
            //this.mInitialScrollPosition = NO_INITIAL_SCROLL_POSITION;
        } else if (Global.debugEnabledSql) {
            Log.i(Global.LOG_CONTEXT, mDebugPrefix + " onCreate() : query = " + mGalleryContentQuery + "; filter = " + mFilter);
        }
    }

    private GalleryFilterParameter getParameterFromPath(String path) {
        if ((path == null) || (path.length() == 0)) return null;

        File selectedPhoto = new File(path);
        this.mInitialFilePath = path;
        this.mInitialScrollPosition = NO_INITIAL_SCROLL_POSITION;
        GalleryFilterParameter filter = new GalleryFilterParameter().setPath(selectedPhoto.getParent() + "/%");
        return filter;
    }

/* these doe not work yet (tested with for android 4.0)
    manifest         <activity ... android:ellipsize="middle" />

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        // http://stackoverflow.com/questions/10779037/set-activity-title-ellipse-to-middle
        final int ActionBarTitle = android.R.id.title; //  Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
        final TextView titleView = (TextView)  this.getWindow().findViewById(ActionBarTitle);
        if ( titleView != null ) {
            titleView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        }
    }
*/

    @Override
    protected void onPause () {
        unhideActionBar(DISABLE_HIDE_ACTIONBAR, "onPause");
        Global.debugMemory(mDebugPrefix, "onPause");
        startStopSlideShow(false);
        super.onPause();
    }

    @Override
    protected void onResume () {
        unhideActionBar(Global.actionBarHideTimeInMilliSecs, "onResume");
        Global.debugMemory(mDebugPrefix, "onResume");
        super.onResume();

        final boolean locked = LockScreen.isLocked(this);
        if (this.locked != locked) {
            this.locked = locked;
            mMustReplaceMenue = true;
            invalidateOptionsMenu();
        }

        if (Global.debugEnabledMemory) {
            Log.d(Global.LOG_CONTEXT, mDebugPrefix + " - onResume cmd (" +
                    MoveOrCopyDestDirPicker.sFileCommands + ") => (" + mFileCommands +
                    ")");

        }

        // workaround fragment lifecycle is newFragment.attach oldFragment.detach.
        // this makes shure that the visible fragment has commands
        MoveOrCopyDestDirPicker.sFileCommands = mFileCommands;
    }

    @Override
    protected void onDestroy() {
        Global.debugMemory(mDebugPrefix, "onDestroy");
		mDestDirPicker = null;

        unhideActionBar(DISABLE_HIDE_ACTIONBAR, "onDestroy");

        if (mImageContextController != null) mImageContextController.close();
        mImageContextController = null;
        // getLoaderManager().destroyLoader(ACTIVITY_ID);
        if (mAdapter != null) {
            mViewPager.setAdapter(null);
            mFileCommands.closeLogFile();
            mFileCommands.closeAll();
            mFileCommands.setContext(null);
        }

        // kill this instance only if not an other instance is active
        if (MoveOrCopyDestDirPicker.sFileCommands == mFileCommands) {
            if (Global.debugEnabledMemory) {
                Log.d(Global.LOG_CONTEXT, mDebugPrefix + " - onDestroy cmd (" +
                        MoveOrCopyDestDirPicker.sFileCommands + ") => (null) ");

            }
            MoveOrCopyDestDirPicker.sFileCommands = null;
        } else if (Global.debugEnabledMemory) {
            Log.d(Global.LOG_CONTEXT, mDebugPrefix + " - onDestroy cmd [ignore] (" +
                    MoveOrCopyDestDirPicker.sFileCommands + ")");

        }

        if (mSlideShowTimer != null) {
            startStopSlideShow(false);
            mSlideShowTimer = null;
        }
        super.onDestroy();
        // RefWatcher refWatcher = AndroFotoFinderApp.getRefWatcher(this);
        // refWatcher.watch(this);
    }

    private void onLoadCompleted() {
        if (mAdapter.getCount() == 0) {
            // image not found in media database

            if (checkForIncompleteMediaDatabase(mInitialFilePath, "onLoadCompleted().count=0")) {
                // this.finish();
            } else {

                // close activity if last image of current selection has been deleted
                String message = getString(R.string.image_err_not_found_format, mInitialFilePath);
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                this.finish();
            }
        } else if (mInitialFilePath != null) {
            // try to find selection by text
            mViewPager.invalidate();
            int positionFound = mAdapter.getPositionFromPath(mInitialFilePath);
            if (positionFound < 0) {
                // not found
                checkForIncompleteMediaDatabase(mInitialFilePath, "onLoadCompleted(Selection='" +
                        mInitialFilePath + "' not found)");
                if (mInitialScrollPosition >= 0) mViewPager.setCurrentItem(mInitialScrollPosition);
                // mInitialFilePath = null; keep path so next requery/rotatate the selected image will be displayed
            } else {
                mViewPager.setCurrentItem(positionFound);
                mInitialFilePath = null;
            }
            mInitialScrollPosition = NO_INITIAL_SCROLL_POSITION;
        } else if (mInitialScrollPosition >= 0) {
            // after initial load select correct image
            mViewPager.invalidate();
            mViewPager.setCurrentItem(mInitialScrollPosition);
            mInitialScrollPosition = NO_INITIAL_SCROLL_POSITION;
            mInitialFilePath = null;
        }
    }

    private void refreshIfNecessary() {
        if ((mAdapter != null) && (mViewPager != null) && (mAdapter.isInArrayMode())) {
            mAdapter.refreshLocal();
            mViewPager.setAdapter(mAdapter);

            // show the changes
            onLoadCompleted();
        }
    }

    /**
     * gets called if no file is found by a db-query or if jpgFullFilePath is not found in media db
     * return false; activity must me closed
     */
    private boolean checkForIncompleteMediaDatabase(String jpgFullFilePath, String why) {
        if (!MediaScanner.isNoMedia(jpgFullFilePath,MediaScanner.DEFAULT_SCAN_DEPTH)) {
            File fileToLoad = (jpgFullFilePath != null) ? new File(jpgFullFilePath) : null;

            if ((!this.mWaitingForMediaScannerResult) && (fileToLoad != null) && (fileToLoad.exists()) && (fileToLoad.canRead())) {
                // file exists => must update media database
                this.mWaitingForMediaScannerResult = true;
                int numberOfNewItems = updateIncompleteMediaDatabase(mDebugPrefix, this,
                        mDebugPrefix + "checkForIncompleteMediaDatabase-" + why,
                        fileToLoad.getParentFile());

                String message = getString(R.string.image_err_not_in_db_format, jpgFullFilePath, numberOfNewItems);
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                return true;
            }
            this.mWaitingForMediaScannerResult = false;
        }
        return false;
    }

    private static int updateIncompleteMediaDatabase(String debugPrefix, Context context, String why, File dirToScan) {
        if (dirToScan == null) return 0;

        String dbPathSearch = null;
        ArrayList<String> missing = new ArrayList<String>();
        dbPathSearch = dirToScan.getPath() + "/%";
        List<String> known = FotoSql.execGetFotoPaths(context, dbPathSearch);
        File[] existing = dirToScan.listFiles();

        if (existing != null) {
            for (File file : existing) {
                String found = file.getAbsolutePath();
                if (MediaUtil.isImage(found, MediaUtil.IMG_TYPE_ALL) && !known.contains(found)) {
                    missing.add(found);
                }
            }
        }

        if (Global.debugEnabled) {
            StringBuilder message = new StringBuilder();
            message.append(debugPrefix).append("updateIncompleteMediaDatabase('")
                    .append(dbPathSearch).append("') : \n\t");

            for(String s : missing) {
                message.append(s).append("; ");
            }
            Log.d(Global.LOG_CONTEXT, message.toString());
        }

        MediaScannerAsyncTask scanner = new MediaScannerAsyncTask(MediaScanner.getInstance(context), context, why);
        scanner.execute(null, missing.toArray(new String[missing.size()]));
        return missing.size();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        defineMenu(menu);

        boolean result = super.onCreateOptionsMenu(menu);
        return result;
    }

    private void defineMenu(Menu menu) {
        if (LockScreen.isLocked(this)) {
            getMenuInflater().inflate(R.menu.menu_image_detail_locked, menu);
            LockScreen.fixMenu(menu);

        } else {
            getMenuInflater().inflate(R.menu.menu_image_detail, menu);
            getMenuInflater().inflate(R.menu.menu_image_commands, menu);
            MenuItem item = menu.findItem(R.id.cmd_filemanager);
            final boolean hasShowInFilemanager = FileManagerUtil.hasShowInFilemanager(this, "/a/b/");
            if ((item != null) && !hasShowInFilemanager) {
                // no filemanager installed
                item.setVisible(false);
            }

            Global.fixMenu(this, menu);
        }
        mMenuSlideshow = menu.findItem(R.id.action_slideshow);
        if (mAdapter != null) mAdapter.setMenu(menu);
        getOrCreateContextTextController().setMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final boolean locked = LockScreen.isLocked(this);
        if (mMustReplaceMenue || (locked != this.locked)) {
            this.locked = locked;

            mMustReplaceMenue = false;
            menu.clear();
            defineMenu(menu);
        }

        // have more time to find and press the menu
        unhideActionBar(Global.actionBarHideTimeInMilliSecs * 3, "onPrepareOptionsMenu");
        AboutDialogPreference.onPrepareOptionsMenu(this, menu);

        MenuItem item = menu.findItem(R.id.cmd_show_geo);

        if (item != null) {
            item.setVisible(hasCurrentGeo());
        }

        item = menu.findItem(R.id.cmd_show_geo_as);

        if (item != null) {
            item.setVisible(hasCurrentGeo());
        }

        if (mAdapter != null) mAdapter.setMenu(menu);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean reloadContext = true;
        boolean result = true;
        boolean slideShowStarted = mSlideShowStarted;

        onGuiTouched();
        if (LockScreen.onOptionsItemSelected(this, item)) {
            mMustReplaceMenue       = true;
            this.invalidateOptionsMenu();
            return true;
        }
        if (mFileCommands.onOptionsItemSelected(item, getCurrentFoto())) {
            mModifyCount++;
        } else {
            // Handle presses on the action bar items
            switch (item.getItemId()) {
                case R.id.action_details:
                    cmdShowDetails(getCurrentFilePath(), getCurrentImageId());
                    break;

                case R.id.action_view_context_mode:
                    pickContextDefinition();
                    break;
                case R.id.cmd_filemanager:
                    FileManagerUtil.showInFilemanager(this, getCurrentDir());
                    break;


                case R.id.action_slideshow:
                    reloadContext = false;
                    // only if not started
                    if (!slideShowStarted) startStopSlideShow(true);
                    break;

                case R.id.action_edit:
                    // #64: (not) open editor via chooser
                    IntentUtil.cmdStartIntent(this, getCurrentFilePath(), null, null,
                            Intent.ACTION_EDIT,
                            (Global.showEditChooser) ? R.string.edit_chooser_title : 0,
                            R.string.edit_err_editor_not_found, ACTION_RESULT_MUST_MEDIA_SCAN);
                    break;

                case R.id.menu_item_share:
                    reloadContext = false;
                    IntentUtil.cmdStartIntent(this, null, null, getCurrentFilePath(), Intent.ACTION_SEND, R.string.share_menu_title, R.string.share_err_not_found, 0);
                    break;

                case R.id.cmd_copy:
                    result = cmdMoveOrCopyWithDestDirPicker(false, mFileCommands.getLastCopyToPath(), getCurrentFoto());
                    break;
                case R.id.cmd_move:
                    result =  cmdMoveOrCopyWithDestDirPicker(true, mFileCommands.getLastCopyToPath(), getCurrentFoto());
                    break;
                case R.id.menu_item_rename:
                    result =  onRenameDirQueston(getCurrentFoto(), getCurrentImageId(), getCurrentFilePath(), null);
                    break;
                case R.id.menu_exif:
                    result =  onEditExif(getCurrentFoto(), getCurrentImageId(), getCurrentFilePath());
                    break;

                case R.id.cmd_gallery: {
                    reloadContext = false;
                    String dirPath = getCurrentFilePath(); // MediaScanner.getDir().getAbsolutePath();
                    if (dirPath != null) {
                        dirPath = FileUtils.getDir(dirPath).getAbsolutePath();
                        GalleryFilterParameter newFilter = new GalleryFilterParameter();
                        newFilter.setPath(dirPath);
                        // int callBackId = (MediaScanner.isNoMedia(dirPath,MediaScanner.DEFAULT_SCAN_DEPTH)) ? NOMEDIA_GALLERY : 0;

                        FotoGalleryActivity.showActivity(this, this.mFilter, null, 0);
                    }
                    break;
                }

                case R.id.cmd_show_geo:
                    MapGeoPickerActivity.showActivity(this, getCurrentFoto(), null);
                    break;

                case R.id.cmd_show_geo_as: {
                    final long imageId = getCurrentImageId();
                    IGeoPoint _geo = FotoSql.execGetPosition(this, null, imageId);
                    final String currentFilePath = getCurrentFilePath();
                    GeoPointDto geo = new GeoPointDto(_geo.getLatitude(), _geo.getLongitude(), GeoPointDto.NO_ZOOM);

                    geo.setDescription(currentFilePath);
                    geo.setId(""+imageId);
                    geo.setName("#"+imageId);
                    GeoUri PARSER = new GeoUri(GeoUri.OPT_PARSE_INFER_MISSING);
                    String uri = PARSER.toUriString(geo);

                    IntentUtil.cmdStartIntent(this, null, uri, null, Intent.ACTION_VIEW, R.string.geo_show_as_menu_title, R.string.geo_picker_err_not_found, 0);
                    break;
                }

                case R.id.cmd_edit_geo: {
                    SelectedFiles selectedItem = getCurrentFoto();
                    GeoEditActivity.showActivity(this, selectedItem, GeoEditActivity.RESULT_ID);
                    break;
                }
                case R.id.cmd_edit_tags: {
                    SelectedFiles selectedItem = getCurrentFoto();
                    tagsShowEditDialog(selectedItem);
                    break;
                }

                case R.id.cmd_about:
                    reloadContext = false;
                    AboutDialogPreference.createAboutDialog(this).show();
                    break;
                case R.id.cmd_settings:
                    reloadContext = false;
                    SettingsActivity.show(this);
                    break;
                case R.id.cmd_more:
                    reloadContext = false;
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            // reopen after some delay
                            openOptionsMenu();
                        }
                    }, 200);
                    break;

                default:
                    result =  super.onOptionsItemSelected(item);
            }
        }

        if (reloadContext) {
            setContextMode(item.getTitle());
        }

        return result;

    }

    private static final int SLIDESHOW_HANDLER_ID = 2;
    private boolean mSlideShowStarted = false;
    private Handler mSlideShowTimer = new Handler() {
        public void handleMessage(Message m) {
            if (mSlideShowStarted) {
                onSlideShowNext();
                sendMessageDelayed(Message.obtain(this, SLIDESHOW_HANDLER_ID), Global.slideshowIntervalInMilliSecs);
            }
        }
    };

    private void startStopSlideShow(boolean start) {
        if (mViewPager != null) {
            mViewPager.setLocked(start);
            if (start != mSlideShowStarted) {
                if (start) {
                    onSlideShowNext();
                    mSlideShowTimer.sendMessageDelayed(Message.obtain(mSlideShowTimer, SLIDESHOW_HANDLER_ID), Global.slideshowIntervalInMilliSecs);
                } else {
                    mSlideShowTimer.removeMessages(SLIDESHOW_HANDLER_ID);
                }
                mSlideShowStarted = start;

                // #24 Prevent sleepmode while slideshow is active
                if (this.mViewPager != null) this.mViewPager.setKeepScreenOn(start);

                if (mMenuSlideshow != null) mMenuSlideshow.setChecked(start);
            }
        }
    }

    private void onSlideShowNext() {
        int pos = mViewPager.getCurrentItem() + 1;
        if (pos >= mAdapter.getCount()) pos = 0;
        mViewPager.setCurrentItem(pos);
    }

    private void cmdShowDetails(String fullFilePath, long currentImageId) {

        ImageDetailMetaDialogBuilder.createImageDetailDialog(this, fullFilePath, currentImageId, mGalleryContentQuery, mViewPager.getCurrentItem()).show();
    }

    private boolean cmdMoveOrCopyWithDestDirPicker(final boolean move, String lastCopyToPath, final SelectedFiles fotos) {
        if (AndroidFileCommands.canProcessFile(this, false)) {
            mDestDirPicker = MoveOrCopyDestDirPicker.newInstance(move, fotos);

            mDestDirPicker.defineDirectoryNavigation(OsUtils.getRootOSDirectory(),
                    (move) ? FotoSql.QUERY_TYPE_GROUP_MOVE : FotoSql.QUERY_TYPE_GROUP_COPY,
                    lastCopyToPath);
            mDestDirPicker.setContextMenuId(LockScreen.isLocked(this) ? 0 :  R.menu.menu_context_osdir);
            mDestDirPicker.show(this.getFragmentManager(), "osdirimage");
        }
        return false;
    }

    private boolean onEditExif(SelectedFiles currentFoto, final long fotoId, final String fotoPath) {
        ExifEditActivity.showActivity(this, null, fotoPath, currentFoto, 0, true);
        return true;
    }
    private boolean onRenameDirQueston(final SelectedFiles currentFoto, final long fotoId, final String fotoPath, final String _newName) {
        if (AndroidFileCommands.canProcessFile(this, false)) {
            final String newName = (_newName == null)
                    ? new File(getCurrentFilePath()).getName()
                    : _newName;


            Dialogs dialog = new Dialogs() {
                @Override
                protected void onDialogResult(String newFileName, Object... parameters) {
                    if (newFileName != null) {
                        onRenameSubDirAnswer(currentFoto, (Long) parameters[0], (String) parameters[1], newFileName);
                    }
                }
            };
            dialog.editFileName(this, getString(R.string.rename_menu_title), newName, fotoId, fotoPath);
        }
        return true;
    }

    private void onRenameSubDirAnswer(SelectedFiles currentFoto, final long fotoId, final String fotoSourcePath, String newFileName) {
        File src = new File(fotoSourcePath);
        File dest = new File(src.getParentFile(), newFileName);

        File srcXmpShort = mFileCommands.getSidecar(src, false);
        boolean hasSideCarShort = ((srcXmpShort != null) && (mFileCommands.osFileExists(srcXmpShort)));
        File srcXmpLong = mFileCommands.getSidecar(src, true);
        boolean hasSideCarLong = ((srcXmpLong != null) && (mFileCommands.osFileExists(srcXmpLong)));

        File destXmpShort = mFileCommands.getSidecar(dest, false);
        File destXmpLong = mFileCommands.getSidecar(dest, true);

        if (src.equals(dest)) return; // new name == old name ==> nothing to do

        String errorMessage = null;
        if (hasSideCarShort && mFileCommands.osFileExists(destXmpShort)) {
            errorMessage = getString(R.string.image_err_file_exists_format, destXmpShort.getAbsoluteFile());
        }
        if (hasSideCarLong && mFileCommands.osFileExists(destXmpLong)) {
            errorMessage = getString(R.string.image_err_file_exists_format, destXmpLong.getAbsoluteFile());
        }
        if (mFileCommands.osFileExists(dest)) {
            errorMessage = getString(R.string.image_err_file_exists_format, dest.getAbsoluteFile());
        }

        if (errorMessage != null) {
            // dest-file already exists
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            onRenameDirQueston(currentFoto, fotoId, fotoSourcePath, newFileName);
        } else if (mFileCommands.rename(currentFoto, dest, null)) {
            mModifyCount++;
        } else {
            // rename failed
            errorMessage = getString(R.string.image_err_file_rename_format, src.getAbsoluteFile());
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
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
    public boolean onTagPopUpClick(int menuItemItemId, Tag selectedTag) {
        return TagsPickerFragment.handleMenuShow(menuItemItemId, selectedTag, this, this.mFilter);
    }

    protected SelectedFiles getCurrentFoto() {
        long imageId = getCurrentImageId();
        Date imageDatePhotoTaken = getCurrentDatePhotoTaken();
        SelectedFiles result = new SelectedFiles(
                new String[] {getCurrentFilePath()},
                new Long[] {Long.valueOf(imageId)},
                new Date[] {imageDatePhotoTaken});
        return  result;
    }

    private long getCurrentImageId() {
        if ((mViewPager != null) && (mAdapter != null)) {
            int itemPosition = mViewPager.getCurrentItem();
            return this.mAdapter.getImageId(itemPosition);
        }
        return -1;
    }

    private Date getCurrentDatePhotoTaken() {
        if ((mViewPager != null) && (mAdapter != null)) {
            int itemPosition = mViewPager.getCurrentItem();
            return this.mAdapter.getDatePhotoTaken(itemPosition);
        }
        return null;
    }

    protected String getCurrentDir() {
        try {
            return new File(getCurrentFilePath()).getParent();
        } catch (Exception ignore) {}
        return null;
    }

    protected String getCurrentFilePath() {
        if ((mViewPager != null) && (mAdapter != null)) {
            int itemPosition = mViewPager.getCurrentItem();
            return this.mAdapter.getFullFilePath(itemPosition);
        }
        return "";
    }

    protected boolean hasCurrentGeo() {
        if ((mViewPager != null) && (mAdapter != null)) {
            int itemPosition = mViewPager.getCurrentItem();
            return this.mAdapter.hasGeo(itemPosition);
        }
        return false;
    }

    private boolean isViewPagerActive() {
    	return (mViewPager != null && mViewPager instanceof LockableViewPager);
    }

	@Override
	protected void onSaveInstanceState(Bundle outState) {
        if (mGalleryContentQuery != null) {
            outState.putString(EXTRA_QUERY, mGalleryContentQuery.toReParseableString());
        }
        outState.putString(INSTANCE_STATE_ContextName, mContextName);
        if (mAdapter != null) {
            outState.putInt(INSTANCE_STATE_ContextMenuId, mAdapter.getMenuId());
        }

        if (isViewPagerActive()) {
            outState.putInt(INSTANCE_STATE_LAST_SCROLL_POSITION, mViewPager.getCurrentItem());
    	}
        outState.putInt(INSTANCE_STATE_MODIFY_COUNT, mModifyCount);
        // if (mImageContextController != null) mImageContextController.saveToFile();
        super.onSaveInstanceState(outState);
	}

    @Override
    public String toString() {
        return mDebugPrefix + this.mAdapter;
    }

    /** #70: Gui: Choose ExtraDetail definition */
    private void pickContextDefinition() {
        getOrCreateContextTextController().onLoadFromQuestion();
    }

    /** #70: handles ExtraDetail definition */
    private ImageContextController getOrCreateContextTextController() {
        if (mImageContextController == null) {
            mImageContextController = new ImageContextController(this) {
                @Override
                protected void onListItemClick(String name, String value) {
                    onDefineContext(name, value);
                }
            };
        }
        return mImageContextController;
    }

    private void setContextMode(Object modeName) {
        if (modeName != null) {
            String value = getOrCreateContextTextController().getPropertyValue(modeName.toString());

            onDefineContext(modeName.toString(), value);
        }
    }

    /**
     * #70: Gui has changed ContextExpression
     * @param modeName property name. if starting with "auto"  then the image detail quick-botton is redefined.
     * @param contextSqlColumnExpression if not empy the result of this expression is shown as context data in image detail view.
     */
    private void onDefineContext(String modeName, String contextSqlColumnExpression) {
        addContextColumn(mGalleryContentQuery, contextSqlColumnExpression);
        if ((mGalleryContentQuery != null)
                && (0 != StringUtils.compare(contextSqlColumnExpression, mContextColumnExpression))
                && (this.mAdapter != null)) {
            // sql detail expression has changed and initialization has completed: requery

            this.mContextColumnExpression = contextSqlColumnExpression; // prevent executing again
            if (mCurorLoader == null) {
                // query has not been initialized
                mCurorLoader = new LocalCursorLoader();
                getLoaderManager().initLoader(ACTIVITY_ID, null, mCurorLoader);
            } else {
                // query has changed
                getLoaderManager().restartLoader(ACTIVITY_ID, null, this.mCurorLoader);
            }
        }
        this.mContextColumnExpression = contextSqlColumnExpression; // prevent executing again

        if (modeName != null) {
            this.mContextName = modeName;
            if (this.mAdapter != null) {
                this.mAdapter.setIconResourceName(modeName);
            }
        }

    }
    /** #70: adds/removes/replases contextColumnExpression */
    private static void addContextColumn(QueryParameter query, String contextColumnExpression) {
        if (query != null) {
            query.removeFirstColumnThatContains(CONTEXT_COLUMN_ALIAS);
            if ((contextColumnExpression != null) && (contextColumnExpression.trim().length() > 0)) {
                query.addColumn(contextColumnExpression + CONTEXT_COLUMN_ALIAS);
            }
        }
    }
}

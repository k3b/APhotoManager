/*******************************************************************************
 * Copyright 2011, 2012 Chris Banes.
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
package de.k3b.android.androFotoFinder.imagedetail;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

// import com.squareup.leakcanary.RefWatcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.k3b.android.androFotoFinder.Common;
import de.k3b.android.androFotoFinder.FotoGalleryActivity;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.directory.DirectoryPickerFragment;
import de.k3b.android.androFotoFinder.locationmap.GeoEditActivity;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.queries.QueryParameterParcelable;
import de.k3b.android.util.AndroidFileCommands;
import de.k3b.android.util.IntentUtil;
import de.k3b.android.util.MediaScanner;
import de.k3b.android.util.SelectedFotos;
import de.k3b.android.widget.AboutDialogPreference;
import de.k3b.io.FileCommands;
import de.k3b.io.GalleryFilterParameter;
import de.k3b.io.IDirectory;
import de.k3b.io.OSDirectory;

/**
 * Shows a zoomable imagee.<br>
 * Swipe left/right to show previous/next image.
 */

public class ImageDetailActivityViewPager extends Activity implements Common {
    private static final String INSTANCE_STATE_MODIFY_COUNT = "mModifyCount";
    public static final int ACTIVITY_ID = 76621;

    /** activityRequestCode: in forward mode: intent is forwarded to gallery */
    private static final int ID_FORWARD = 471102;
    private static final int DEFAULT_SORT = FotoSql.SORT_BY_NAME_LEN;
    private static final QueryParameterParcelable DEFAULT_QUERY = FotoSql.queryDetail;

    // how many changes have been made. if != 0 parent activity must invalidate cached data
    private static int mModifyCount = 0;
    private MenuItem mMenuSlideshow = null;

    class ImageDetailFileCommands extends AndroidFileCommands {
        @Override
        protected void onPostProcess(String[] oldPathNames, String[] newPathNames, int modifyCount, int itemCount, int opCode) {
            Context context = ImageDetailActivityViewPager.this.getApplicationContext();

            switch (opCode) {
                case OP_UPDATE:
                case OP_COPY:
                    // restart scanner in background if necessary
                    super.onPostProcess(oldPathNames, newPathNames, modifyCount, itemCount, opCode);
                    break;
                case OP_MOVE:
                case OP_RENAME:
                    if ((newPathNames!= null) && (newPathNames.length > 0)) {
                        mInitialFilePath = newPathNames[0];
                    }
                    MediaScanner.updateMediaDatabase_Android42(context, oldPathNames, newPathNames);
                    break;
                case OP_DELETE:
                    // media db is already updated
                    break;
            }

            int resId = getResourceId(opCode);
            String message = getString(resId, Integer.valueOf(modifyCount), Integer.valueOf(itemCount));
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            // mDirectoryListener.invalidateDirectories();
            // reload after modification
            requery("ImageDetailFileCommands.onPostProcess()" + message);
        }

        private int getResourceId(int opCode) {
            switch (opCode) {
                case OP_COPY: return R.string.format_copy_result;
                case OP_MOVE: return R.string.format_move_result;
                case OP_DELETE: return R.string.format_delete_result;
                case OP_RENAME: return R.string.format_rename_result;
                case OP_UPDATE: return R.string.format_update_result;
            }
            return 0;
        }

    }
    public static class MoveOrCopyDestDirPicker extends DirectoryPickerFragment {
        static AndroidFileCommands sFileCommands = null;

        public static MoveOrCopyDestDirPicker newInstance(boolean move, SelectedFotos srcFotos) {
            MoveOrCopyDestDirPicker f = new MoveOrCopyDestDirPicker();

            // Supply index input as an argument.
            Bundle args = new Bundle();
            args.putBoolean("move", move);
            args.putSerializable("srcFotos", srcFotos);
            f.setArguments(args);

            return f;
        }

        /** do not use activity callback */
        @Override
        protected void setDirectoryListener(Activity activity) {}

        public boolean getMove() {
            return getArguments().getBoolean("move", false);
        }

        public SelectedFotos getSrcFotos() {
            return (SelectedFotos) getArguments().getSerializable("srcFotos");
        }

        @Override
        protected void onDirectoryPick(IDirectory selection) {
            // super.onDirectoryPick(selection);
            mModifyCount++; // copy or move initiated
            getActivity().setResult((mModifyCount == 0) ? RESULT_NOCHANGE : RESULT_CHANGE);

            dismiss();
            sFileCommands.onMoveOrCopyDirectoryPick(getMove(), selection, getSrcFotos());
        }
    };

    private static final String INSTANCE_STATE_LAST_SCROLL_POSITION = "lastScrollPosition";

    // private static final String ISLOCKED_ARG = "isLocked";
	
	private LockableViewPager mViewPager = null;
    private ImagePagerAdapterFromCursor mAdapter = null;

    private final AndroidFileCommands mFileCommands = new ImageDetailFileCommands();

    // for debugging
    private static int id = 1;
    private String mDebugPrefix;
    private DataSetObserver mLoadCompleteHandler;

    /** where data comes from */
    private QueryParameterParcelable mGalleryContentQuery = null;

    /** if >= 0 after load cursor scroll to this offset */
    private int mScrollPosition = -1;

    /** if != after load cursor scroll to this path */
    private String mInitialFilePath = null;

    public static void showActivity(Activity context, Uri imageUri, int position, QueryParameterParcelable imageDetailQuery) {
        Intent intent;
        //Create intent
        intent = new Intent(context, ImageDetailActivityViewPager.class);

        intent.putExtra(ImageDetailActivityViewPager.EXTRA_QUERY, imageDetailQuery);
        intent.putExtra(ImageDetailActivityViewPager.EXTRA_POSITION, position);
        intent.setData(imageUri);

        context.startActivityForResult(intent, ACTIVITY_ID);
    }

    @Override
	public void onCreate(Bundle savedInstanceState) {
        mDebugPrefix = "ImageDetailActivityViewPager#" + (id++)  + " ";
        Global.debugMemory(mDebugPrefix, "onCreate");

        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        if (mustForward(intent)) {
            // cannot handle myself. Forward to FotoGalleryActivity
            Intent childIntent = new Intent();
            childIntent.setClass(this, FotoGalleryActivity.class);
            childIntent.setAction(intent.getAction());
            childIntent.setDataAndType(intent.getData(), intent.getType());
            copyExtras(childIntent, intent.getExtras(),
                    EXTRA_FILTER, EXTRA_POSITION, EXTRA_QUERY, EXTRA_SELECTED_ITEMS, EXTRA_STREAM, EXTRA_TITLE);
            startActivityForResult(childIntent, ID_FORWARD);
        } else { // not in forward mode
            setContentView(R.layout.activity_image_view_pager);

            mViewPager = (LockableViewPager) findViewById(R.id.view_pager);
            setContentView(mViewPager);

            // extra parameter
            getParameter(intent);

            mLoadCompleteHandler = new DataSetObserver() {
                @Override
                public void onChanged() {
                    super.onChanged();
                    // #13 Swiping: Sometimes the app jumps back
                    int currentViewItem = mViewPager.getCurrentItem();
                    if (currentViewItem > 0) {
                        mScrollPosition = currentViewItem;
                    }
                    onLoadCompleted();
                }
            };

            mAdapter = new ImagePagerAdapterFromCursor(this, mGalleryContentQuery, mDebugPrefix);
            mAdapter.registerDataSetObserver(mLoadCompleteHandler);
            mViewPager.setAdapter(mAdapter);
            mViewPager.setOnInterceptTouchEvent(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mSlideShowStarted) startStopSlideShow(false);
                }
            });

            if (savedInstanceState != null) {
                mScrollPosition = savedInstanceState.getInt(INSTANCE_STATE_LAST_SCROLL_POSITION, this.mScrollPosition);
                mModifyCount = savedInstanceState.getInt(INSTANCE_STATE_MODIFY_COUNT, this.mModifyCount);
            } else {
                mModifyCount = 0;
            }

            setResult((mModifyCount == 0) ? RESULT_NOCHANGE : RESULT_CHANGE);

            mFileCommands.setContext(this);
            mFileCommands.setLogFilePath(mFileCommands.getDefaultLogFile());
            MoveOrCopyDestDirPicker.sFileCommands = mFileCommands;
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

        if (requestCode == ID_FORWARD) {
            // forward result from child-activity to parent-activity
            setResult(resultCode, intent);
            finish();
        }
    }

    private static boolean mustForward(Intent intent) {
        Uri uri = IntentUtil.getUri(intent);
        File file = IntentUtil.getFile(uri);

        // probably content: url
        if (file == null) return IntentUtil.isFileUri(uri); // true if file:// with wildcards "%" or "*" goto gallery;

        // file with wildcard, directory or no read permissions
        return (!file.exists() || !file.isFile() || !file.canRead());
    }

    /** query from EXTRA_QUERY, EXTRA_FILTER, fileParentDir , defaultQuery */
    private void getParameter(Intent intent) {
        this.mScrollPosition = intent.getIntExtra(EXTRA_POSITION, this.mScrollPosition);
        this.mGalleryContentQuery = intent.getParcelableExtra(EXTRA_QUERY);
        if (mGalleryContentQuery == null) {
            String filterValue = intent.getStringExtra(EXTRA_FILTER);
            if (filterValue != null) {
                GalleryFilterParameter filter = GalleryFilterParameter.parse(filterValue, new GalleryFilterParameter());
                QueryParameterParcelable query = new QueryParameterParcelable(DEFAULT_QUERY);
                FotoSql.setSort(query, DEFAULT_SORT, true);
                FotoSql.setWhereFilter(query, filter);
            }
        }
        if (mGalleryContentQuery == null) {
            Uri uri = IntentUtil.getUri(intent);
            if (uri != null) {
                String scheme = uri.getScheme();
                if ((scheme == null) || ("file".equals(scheme))) {
                    getParameterFromPath(uri.getPath(), true);
                } else if ("content".equals(scheme)) {
                    String path = FotoSql.execGetFotoPath(this, uri);
                    if (path != null) {
                        getParameterFromPath(path, false);
                    }
                }
            }
        }

        if (mGalleryContentQuery == null) {
            Log.e(Global.LOG_CONTEXT, mDebugPrefix + " onCreate() : intent.extras[" + EXTRA_QUERY +
                    "] not found. data=" + intent.getData() +
                    ". Using default.");
            mGalleryContentQuery = new QueryParameterParcelable(DEFAULT_QUERY);
        } else if (Global.debugEnabledSql) {
            Log.i(Global.LOG_CONTEXT, mDebugPrefix + " onCreate() : query = " + mGalleryContentQuery);
        }
    }

    private void getParameterFromPath(String path, boolean isFileUri) {
        mInitialFilePath = path;
        File selectedPhoto = new File(mInitialFilePath);
        this.mScrollPosition = -1;

        QueryParameterParcelable query = new QueryParameterParcelable(DEFAULT_QUERY);
        FotoSql.addPathWhere(query, selectedPhoto.getParent(), FotoSql.QUERY_TYPE_GALLERY);
        FotoSql.setSort(query, DEFAULT_SORT, true);
        mGalleryContentQuery = query;
    }

/* these doe not work yet (tested with for android 4.0)
    manifest         <activity ... android:ellipsize="middle" />

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        // http://stackoverflow.com/questions/10779037/set-activity-title-ellipse-to-middle
        final int actionBarTitle = android.R.id.title; //  Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
        final TextView titleView = (TextView)  this.getWindow().findViewById(actionBarTitle);
        if ( titleView != null ) {
            titleView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        }
    }
*/

    @Override
    protected void onPause () {
        Global.debugMemory(mDebugPrefix, "onPause");
        startStopSlideShow(false);
        super.onPause();
    }

    @Override
    protected void onResume () {
        Global.debugMemory(mDebugPrefix, "onResume");
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        Global.debugMemory(mDebugPrefix, "onDestroy");

        if (mAdapter != null) {
            // not in forward-mode
            mAdapter.unregisterDataSetObserver(mLoadCompleteHandler);
            mLoadCompleteHandler = null;
            mViewPager.setAdapter(null);
            mFileCommands.closeLogFile();
            mFileCommands.closeAll();
            mFileCommands.setContext(null);
            MoveOrCopyDestDirPicker.sFileCommands = null;
        }

        super.onDestroy();
        // RefWatcher refWatcher = AndroFotoFinderApp.getRefWatcher(this);
        // refWatcher.watch(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_image_detail, menu);
        getMenuInflater().inflate(R.menu.menu_image_commands, menu);
        mMenuSlideshow = menu.findItem(R.id.action_slideshow);
        return super.onCreateOptionsMenu(menu);
    }

    private void onLoadCompleted() {
        if (mAdapter.getCount() == 0) {
            // image not found in media database

            if (checkForIncompleteMediaDatabase(mInitialFilePath)) {
                // this.finish();
            } else {
                // close activity if last image of current selection has been deleted
                String message = getString(R.string.err_no_fotos_found, mInitialFilePath);
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                this.finish();
            }
        } else if (mScrollPosition >= 0) {
            // after initial load select correct image
            mViewPager.invalidate();
            mViewPager.setCurrentItem(mScrollPosition);
            mScrollPosition = -1;
            mInitialFilePath = null;
        } else if (mInitialFilePath != null) {
            mViewPager.invalidate();
            int positionFound = mAdapter.getCursorFromPath(mInitialFilePath);
            if (positionFound < 0) {
                checkForIncompleteMediaDatabase(mInitialFilePath);
            }
            mViewPager.setCurrentItem(positionFound);
            mScrollPosition = -1;
            // mInitialFilePath = null; keep path so next requery/rotatate the selected image will be displayed
        } else {
            // update mViewPager so that deleted image will not be the current any more
            mScrollPosition = mViewPager.getCurrentItem();
            mViewPager.setAdapter(mAdapter); // reload
            mViewPager.setCurrentItem(mScrollPosition);
            mScrollPosition = -1;
        }
    }

    private boolean checkForIncompleteMediaDatabase(String filePath) {
        File fileToLoad = (filePath != null) ? new File(filePath) : null;
        if ((fileToLoad != null) && (fileToLoad.exists()) && (fileToLoad.canRead())) {
            // file exists => must update media database
            int numberOfNewItems = updateIncompleteMediaDatabase(fileToLoad);

            String message = getString(R.string.err_fotos_not_in_db, filePath, numberOfNewItems);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            return true;
        }
        return false;
    }

    private int updateIncompleteMediaDatabase(File fileToLoad) {
        String fullPath = fileToLoad.toString();
        String dbPathSearch = null;
        ArrayList<String> missing = new ArrayList<String>();
        File parentDir = fileToLoad.getParentFile();
        dbPathSearch = parentDir.getPath() + "%";
        List<String> known = FotoSql.execGetFotoPaths(this, dbPathSearch);
        File[] existing = parentDir.listFiles();

        if (existing != null) {
            for (File file : existing) {
                String found = file.getAbsolutePath();
                if (MediaScanner.isJpeg(found) && !known.contains(found)) {
                    missing.add(found);
                }
            }
        }

        if (missing.size() == 0) {
            missing.add(fullPath);
        }

        if (Global.debugEnabled) {
            StringBuilder message = new StringBuilder();
            message.append(mDebugPrefix).append("updateIncompleteMediaDatabase.updateIncompleteMediaDatabase('")
                    .append(fullPath).append("', '").append(dbPathSearch).append("') : \n\t");

            for(String s : missing) {
                message.append(s).append("; ");
            }
            Log.d(Global.LOG_CONTEXT, message.toString());
        }

        MediaScanner scanner = new MediaScanner(this) {
            @Override
            protected void onPostExecute(Integer resultCount) {
                super.onPostExecute(resultCount);
                if (resultCount > 0) {
                    requery("updateIncompleteMediaDatabase-MediaScanner.onPostExecute()");
                } else {
                    finish();
                }
                if (Global.debugEnabled) {
                    Log.d(Global.LOG_CONTEXT, "Items updated by Media scanner : " + resultCount);
                }
            }
        };
        scanner.execute(null, missing.toArray(new String[missing.size()]));
        return missing.size();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean slideShowStarted = mSlideShowStarted;

        // every command will stop the slideshow
        if (slideShowStarted) {
            startStopSlideShow(false);
        }
        if (mFileCommands.onOptionsItemSelected(item, getCurrentFoto())) {
            mModifyCount++;
            return true; // case R.id.cmd_delete:
        }

        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_details:
                cmdShowDetails(getCurrentFilePath(), getCurrentImageId());
                return true;

            case R.id.action_slideshow:
                // only if not started
                if (!slideShowStarted) startStopSlideShow(true);
                return true;

            case R.id.action_edit:
                cmdStartIntent(getCurrentFilePath(), Intent.ACTION_EDIT, R.string.title_chooser_edit, R.string.error_edit);
                return true;

            case R.id.menu_item_share:
                cmdStartIntent(getCurrentFilePath(), Intent.ACTION_SEND, R.string.title_chooser_share, R.string.error_share);
                return true;

            case R.id.cmd_copy:
                return cmdMoveOrCopyWithDestDirPicker(false, mFileCommands.getLastCopyToPath(), getCurrentFoto());
            case R.id.cmd_move:
                return cmdMoveOrCopyWithDestDirPicker(true, mFileCommands.getLastCopyToPath(), getCurrentFoto());
            case R.id.menu_item_rename:
                return onRenameDirQueston(getCurrentImageId(), getCurrentFilePath(), null);
            case R.id.cmd_edit_geo:
                GeoEditActivity.showActivity(this, getCurrentFoto());
                return true;

            case R.id.cmd_about:
                AboutDialogPreference.createAboutDialog(this).show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private static final int SLIDESHOW_WHAT = 2;
    private boolean mSlideShowStarted = false;
    private Handler mSlideShowTimer = new Handler() {
        public void handleMessage(Message m) {
            if (mSlideShowStarted) {
                onSlideShowNext();
                sendMessageDelayed(Message.obtain(this, SLIDESHOW_WHAT), Global.slideshowIntervallInMilliSecs);
            }
        }
    };

    private void startStopSlideShow(boolean start) {
        mViewPager.setLocked(start);
        if (start != mSlideShowStarted) {
            if (start) {
                onSlideShowNext();
                mSlideShowTimer.sendMessageDelayed(Message.obtain(mSlideShowTimer, SLIDESHOW_WHAT), Global.slideshowIntervallInMilliSecs);
            } else {
                mSlideShowTimer.removeMessages(SLIDESHOW_WHAT);
            }
            mSlideShowStarted = start;
            if (mMenuSlideshow != null) mMenuSlideshow.setChecked(start);
        }
    }

    private void onSlideShowNext() {
        int pos = mViewPager.getCurrentItem() + 1;
        if (pos >= mAdapter.getCount()) pos = 0;
        mViewPager.setCurrentItem(pos);
    }

    private void cmdStartIntent(String currentFilePath, String action, int idChooserCaption, int idEditError) {
        File file = new File(currentFilePath);
        final Uri uri = Uri.fromFile(file);

        final Intent outIntent = new Intent()
                .setAction(action)
                .setDataAndType(uri, getMime(currentFilePath))
                // .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (Global.debugEnabled) {
            Log.d(Global.LOG_CONTEXT, "cmdStartIntent(" +
                    action +
                    ":'" + outIntent.getData() + "',  mime:'" + outIntent.getType() + "')");
        }

        try {
            this.startActivity(Intent.createChooser(outIntent, getText(idChooserCaption)));
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, idEditError,Toast.LENGTH_LONG).show();
        }
    }

    private void cmdShowDetails(String fullFilePath, long currentImageId) {

        ImageDetailDialogBuilder.createImageDetailDialog(this, fullFilePath, currentImageId).show();
    }

    private boolean cmdMoveOrCopyWithDestDirPicker(final boolean move, String lastCopyToPath, final SelectedFotos fotos) {
        if (AndroidFileCommands.canProcessFile(this)) {
            MoveOrCopyDestDirPicker destDir = MoveOrCopyDestDirPicker.newInstance(move, fotos);

            destDir.defineDirectoryNavigation(new OSDirectory("/", null),
                    (move) ? FotoSql.QUERY_TYPE_GROUP_MOVE : FotoSql.QUERY_TYPE_GROUP_COPY,
                    lastCopyToPath);
            destDir.setContextMenuId(R.menu.menu_context_osdir);
            destDir.show(this.getFragmentManager(), "osdirimage");
        }
        return false;
    }

    private boolean onRenameDirQueston(final long fotoId, final String fotoPath, String newName) {
        if (AndroidFileCommands.canProcessFile(this)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.cmd_rename);
            View content = this.getLayoutInflater().inflate(R.layout.dialog_edit_name, null);

            final EditText edit = (EditText) content.findViewById(R.id.edName);

            if (newName == null) {
                newName = new File(getCurrentFilePath()).getName();
            }
            edit.setText(newName);

            // select text without extension
            int selectLen = newName.lastIndexOf(".");
            if (selectLen == -1) selectLen = newName.length();
            edit.setSelection(0, selectLen);

            builder.setView(content);
            builder.setNegativeButton(R.string.cancel, null);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                //@Override
                public void onClick(DialogInterface dialog, int which) {
                    onRenameSubDirAnswer(fotoId, fotoPath, edit.getText().toString());
                }
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();

            int width = (int) (8 * edit.getTextSize());
            // DisplayMetrics metrics = getResources().getDisplayMetrics();
            // int width = metrics.widthPixels;
            alertDialog.getWindow().setLayout(width * 2, LinearLayout.LayoutParams.WRAP_CONTENT);
            edit.requestFocus();

            // request keyboard. See http://stackoverflow.com/questions/2403632/android-show-soft-keyboard-automatically-when-focus-is-on-an-edittext
            alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        return true;
    }

    private void onRenameSubDirAnswer(final long fotoId, final String fotoSourcePath, String newFileName) {
        File src = new File(fotoSourcePath);
        File srcXmp = mFileCommands.getSidecar(src);
        boolean hasSideCar = ((srcXmp != null) && (mFileCommands.osFileExists(srcXmp)));

        File dest = new File(src.getParentFile(), newFileName);
        File destXmp = mFileCommands.getSidecar(dest);

        if (src == dest) return; // new name == old name ==> nothing to do

        String errorMessage = null;
        if (hasSideCar && mFileCommands.osFileExists(destXmp)) {
            errorMessage = getString(R.string.err_file_exists, destXmp.getAbsoluteFile());
        }
        if (mFileCommands.osFileExists(dest)) {
            errorMessage = getString(R.string.err_file_exists, dest.getAbsoluteFile());
        }

        if (errorMessage != null) {
            // dest-file already exists
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            onRenameDirQueston(fotoId, fotoSourcePath, newFileName);
        } else if (mFileCommands.rename(fotoId, dest, src)) {
            mModifyCount++;
        } else {
            // rename failed
            errorMessage = getString(R.string.err_file_rename, src.getAbsoluteFile());
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void requery(String why) {
        mAdapter.requery(this, mGalleryContentQuery, mDebugPrefix + why);
    }

    private String getMime(String path) {
        MimeTypeMap map = MimeTypeMap.getSingleton();
        return map.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(path));
    }

    protected SelectedFotos getCurrentFoto() {
        long imageId = getCurrentImageId();
        SelectedFotos result = new SelectedFotos();
        result.add(imageId);
        return  result;
    }

    private long getCurrentImageId() {
        int itemPosition = mViewPager.getCurrentItem();
        return this.mAdapter.getImageId(itemPosition);
    }

    protected String getCurrentFilePath() {
        int itemPosition = mViewPager.getCurrentItem();
        return this.mAdapter.getFullFilePath(itemPosition);
    }

    private void toggleViewPagerScrolling() {
    	if (isViewPagerActive()) {
    		((LockableViewPager) mViewPager).toggleLock();
    	}
    }
    
    private boolean isViewPagerActive() {
    	return (mViewPager != null && mViewPager instanceof LockableViewPager);
    }

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (isViewPagerActive()) {
            outState.putInt(INSTANCE_STATE_LAST_SCROLL_POSITION, mViewPager.getCurrentItem());
    	}
        outState.putInt(INSTANCE_STATE_MODIFY_COUNT, mModifyCount);
		super.onSaveInstanceState(outState);
	}

}

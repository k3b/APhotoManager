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
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
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

import de.k3b.android.androFotoFinder.Common;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.directory.DirectoryPickerFragment;
import de.k3b.android.androFotoFinder.locationmap.GeoEditActivity;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.queries.QueryParameterParcelable;
import de.k3b.android.util.AndroidFileCommands;
import de.k3b.android.util.SelectedFotos;
import de.k3b.android.widget.AboutDialogPreference;
import de.k3b.io.IDirectory;
import de.k3b.io.OSDirectory;

/**
 * Shows a zoomable imagee.<br>
 * Swipe left/right to show previous/next image.
 */

public class ImageDetailActivityViewPager extends Activity implements Common {
    private static final String INSTANCE_STATE_MODIFY_COUNT = "mModifyCount";
    public static final int ACTIVITY_ID = 76621;

    // how many changes have been made. if != 0 parent activity must invalidate cached data
    private static int mModifyCount = 0;

    class ImageDetailFileCommands extends AndroidFileCommands {
        @Override
        protected void onPostProcess(String[] paths, int modifyCount, int itemCount, int opCode) {
            super.onPostProcess(paths, modifyCount, itemCount, opCode);
            // reload after modification
            requery();
            if (Global.clearSelectionAfterCommand || (opCode == OP_DELETE) || (opCode == OP_MOVE)) {
            }

            Activity context = ImageDetailActivityViewPager.this;
            int resId = getResourceId(opCode);
            String message = getString(resId, Integer.valueOf(modifyCount), Integer.valueOf(itemCount));
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            // mDirectoryListener.invalidateDirectories();
        }

        private int getResourceId(int opCode) {
            switch (opCode) {
                case OP_COPY: return R.string.format_copy_result;
                case OP_MOVE: return R.string.format_move_result;
                case OP_DELETE: return R.string.format_delete_result;
            }
            return 0;
        }
/*        */
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
    public static final String EXTRA_QUERY = "de.k3b.extras.sql";
    public static final String EXTRA_POSITION = "de.k3b.extras.position";

    // private static final String ISLOCKED_ARG = "isLocked";
	
	private ViewPager mViewPager;
    private ImagePagerAdapterFromCursor mAdapter;

    private QueryParameterParcelable mGalleryContentQuery = null;
    private final AndroidFileCommands mFileCommands = new ImageDetailFileCommands();

    // for debugging
    private static int id = 1;
    private String debugPrefix;
    private DataSetObserver loadCompleteHandler;
    private int mInitialPosition = -1;
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
        debugPrefix = "ImageDetailActivityViewPager#" + (id++)  + " ";
        Global.debugMemory(debugPrefix, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view_pager);

        mViewPager = (LockableViewPager) findViewById(R.id.view_pager);
		setContentView(mViewPager);

        // extra parameter
        Intent intent = getIntent();
        getParameter(intent);

        mAdapter = new ImagePagerAdapterFromCursor(this, mGalleryContentQuery, debugPrefix);
        loadCompleteHandler = new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                onLoadCompleted();
            }
        };
        mAdapter.registerDataSetObserver(loadCompleteHandler);
        mViewPager.setAdapter(mAdapter);

        if (savedInstanceState != null) {
            mInitialPosition = savedInstanceState.getInt(INSTANCE_STATE_LAST_SCROLL_POSITION, this.mInitialPosition);
            mModifyCount = savedInstanceState.getInt(INSTANCE_STATE_MODIFY_COUNT, this.mModifyCount);
        } else {
            mModifyCount = 0;
        }

        setResult((mModifyCount == 0) ? RESULT_NOCHANGE : RESULT_CHANGE);

        mFileCommands.setContext(this);
        mFileCommands.setLogFilePath(mFileCommands.getDefaultLogFile());
        MoveOrCopyDestDirPicker.sFileCommands = mFileCommands;
    }

    private void getParameter(Intent intent) {
        this.mInitialPosition = intent.getIntExtra(EXTRA_POSITION, this.mInitialPosition);
        this.mGalleryContentQuery = intent.getParcelableExtra(EXTRA_QUERY);
        if (mGalleryContentQuery == null) {
            Uri uri = intent.getData();
            if (uri == null) {
                Object stream = intent.getExtras().get(Intent.EXTRA_STREAM);
                if (stream != null) {
                    uri = Uri.parse(stream.toString());
                }
            }
            if (uri != null) {
                String scheme = uri.getScheme();
                if ((scheme == null) || ("file".equals(scheme))) {
                    getParameterFromPath(uri.getPath());
                } else if ("content".equals(scheme)) {
                    String path = FotoSql.execGetFotoPath(this, uri);
                    if (path != null) {
                        getParameterFromPath(path);
                    }
                }
            }
        }

        if (mGalleryContentQuery == null) {
            Log.e(Global.LOG_CONTEXT, debugPrefix + " onCreate() : intent.extras[" + EXTRA_QUERY +
                    "] not found. data=" + intent.getData() +
                    ". Using default.");
            mGalleryContentQuery = FotoSql.getQuery(FotoSql.QUERY_TYPE_DEFAULT);
        } else if (Global.debugEnabledSql) {
            Log.i(Global.LOG_CONTEXT, debugPrefix + " onCreate() : query = " + mGalleryContentQuery);
        }
    }

    private void getParameterFromPath(String path) {
        mInitialFilePath = path;
        File selectedPhoto = new File(mInitialFilePath);
        this.mInitialPosition = -1;

        QueryParameterParcelable query = new QueryParameterParcelable(FotoSql.queryDetail);
        FotoSql.addPathWhere(query, selectedPhoto.getParent(), FotoSql.QUERY_TYPE_GALLERY);
        FotoSql.setSort(query, FotoSql.SORT_BY_NAME_LEN, true);
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
        Global.debugMemory(debugPrefix, "onPause");

        super.onPause();
    }

    @Override
    protected void onResume () {
        Global.debugMemory(debugPrefix, "onResume");
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        Global.debugMemory(debugPrefix, "onDestroy");
        mAdapter.unregisterDataSetObserver(loadCompleteHandler);
        loadCompleteHandler = null;
        mViewPager.setAdapter(null);
        mFileCommands.closeLogFile();
        mFileCommands.closeAll();
        mFileCommands.setContext(null);
        MoveOrCopyDestDirPicker.sFileCommands = null;

        super.onDestroy();
        // RefWatcher refWatcher = AndroFotoFinderApp.getRefWatcher(this);
        // refWatcher.watch(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_image_detail, menu);
        getMenuInflater().inflate(R.menu.menu_image_commands, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void onLoadCompleted() {
        if (mAdapter.getCount() == 0) {
            // close activity if last image of current selection has been deleted
            Toast.makeText(this, R.string.delete_empty, Toast.LENGTH_LONG).show();
            this.finish();
        } else if (mInitialPosition >= 0) {
            // after initial load select correct image
            mViewPager.invalidate();
            mViewPager.setCurrentItem(mInitialPosition);
            mInitialPosition = -1;
            mInitialFilePath = null;
        } else if (mInitialFilePath != null) {
            mViewPager.invalidate();
            mViewPager.setCurrentItem(mAdapter.getCursorFromPath(mInitialFilePath));
            mInitialPosition = -1;
            mInitialFilePath = null;
        } else {
            // update mViewPager so that deleted image will not be the current any more
            mInitialPosition = mViewPager.getCurrentItem();
            mViewPager.setAdapter(mAdapter); // reload
            mViewPager.setCurrentItem(mInitialPosition);
            mInitialPosition = -1;
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mFileCommands.onOptionsItemSelected(item, getCurrentFoto())) {
            mModifyCount++;
            return true; // case R.id.cmd_delete:
        }

        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_details:
                cmdShowDetails(getCurrentFilePath());
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

    private void cmdShowDetails(String fullFilePath) {

        ImageDetailDialogBuilder.createImageDetailDialog(this, fullFilePath).show();
    }

    private boolean cmdMoveOrCopyWithDestDirPicker(final boolean move, String lastCopyToPath, final SelectedFotos fotos) {
        MoveOrCopyDestDirPicker destDir = MoveOrCopyDestDirPicker.newInstance(move, fotos);

        destDir.defineDirectoryNavigation(new OSDirectory("/", null), FotoSql.QUERY_TYPE_GROUP_COPY, lastCopyToPath);
        destDir.setContextMenuId(R.menu.menu_context_osdir);
        destDir.show(this.getFragmentManager(), "osdirimage");
        return false;
    }

    private boolean onRenameDirQueston(final long fotoId, final String fotoPath, String newName) {
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

        int width = (int ) (8 * edit.getTextSize());
        // DisplayMetrics metrics = getResources().getDisplayMetrics();
        // int width = metrics.widthPixels;
        alertDialog.getWindow().setLayout(width * 2, LinearLayout.LayoutParams.WRAP_CONTENT);
		edit.requestFocus();
		
		// request keyboard. See http://stackoverflow.com/questions/2403632/android-show-soft-keyboard-automatically-when-focus-is-on-an-edittext
		alertDialog.getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
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
            // rename success: update media database and gui
            requery();
            errorMessage = getString(R.string.success_file_rename, src.getAbsoluteFile(), newFileName);
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            mModifyCount++;
        } else {
            // rename failed
            errorMessage = getString(R.string.err_file_rename, src.getAbsoluteFile());
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void requery() {
        mAdapter.requery(this, mGalleryContentQuery);
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

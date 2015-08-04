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
 
package de.k3b.android.androFotoFinder.gallery.cursor;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.provider.MediaStore;
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

// import com.squareup.leakcanary.RefWatcher;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.directory.DirectoryGui;
import de.k3b.android.androFotoFinder.directory.DirectoryPickerFragment;
import de.k3b.android.androFotoFinder.imagedetail.ImageDetailActivityViewPager;
import de.k3b.android.androFotoFinder.queries.FotoViewerParameter;
import de.k3b.android.androFotoFinder.queries.QueryParameterParcelable;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.OnGalleryInteractionListener;
import de.k3b.android.androFotoFinder.queries.Queryable;
import de.k3b.android.util.AndroidFileCommands;
import de.k3b.android.util.SelectedFotos;
import de.k3b.io.Directory;
import de.k3b.io.IDirectory;

/**
 * A {@link Fragment} to show ImageGallery content based on ContentProvider-Cursor.
 * Activities that contain this fragment must implement the
 * {@link OnGalleryInteractionListener} interface
 * to handle interaction events.
 * Use the {@link GalleryCursorFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GalleryCursorFragment extends Fragment  implements Queryable, DirectoryGui {
    private static final String INSTANCE_STATE_LAST_VISIBLE_POSITION = "lastVisiblePosition";
    private static final String INSTANCE_STATE_SELECTED_ITEM_IDS = "selectedItems";
    private static final String INSTANCE_STATE_OLD_TITLE = "oldTitle";
    private static final String INSTANCE_STATE_SEL_ONLY = "selectedOnly";

    private HorizontalScrollView parentPathBarScroller;
    private LinearLayout parentPathBar;

    private HorizontalScrollView childPathBarScroller;
    private LinearLayout childPathBar;

    // for debugging
    private static int id = 1;
    private final String debugPrefix;

    private GridView galleryView;

    private ShareActionProvider mShareActionProvider;
    private MenuItem mShareOnlyToggle;
    private GalleryCursorAdapter galleryAdapter = null;

    private OnGalleryInteractionListener mGalleryListener;
    private QueryParameterParcelable mGalleryContentQuery;

    private DirectoryPickerFragment.OnDirectoryInteractionListener mDirectoryListener;
    private int mLastVisiblePosition = -1;
    private int mInitialPositionY = 0;

    // multi selection support
    private final SelectedFotos mSelectedItems = new SelectedFotos();
    private String mOldTitle = null;
    private boolean mShowSelectedOnly = false;
    private AndroidFileCommands mFileCommands;

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

    public GalleryCursorFragment() {
        debugPrefix = "GalleryCursorFragment#" + (id++)  + " ";
        Global.debugMemory(debugPrefix, "ctor");

        // Required empty public constructor
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, debugPrefix + "()");
        }

    }

    /**************** live-cycle ******************/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Global.debugMemory(debugPrefix, "onCreate");
        setHasOptionsMenu(true);
        this.mShareActionProvider = new ShareActionProvider(this.getActivity());
        if (savedInstanceState != null) {
            this.mLastVisiblePosition = savedInstanceState.getInt(INSTANCE_STATE_LAST_VISIBLE_POSITION, this.mLastVisiblePosition);
            String old = mSelectedItems.toString();
            mSelectedItems.clear();
            mSelectedItems.parse(savedInstanceState.getString(INSTANCE_STATE_SELECTED_ITEM_IDS, old));
            this.mOldTitle = savedInstanceState.getString(INSTANCE_STATE_OLD_TITLE, this.mOldTitle);
            this.mShowSelectedOnly = savedInstanceState.getBoolean(INSTANCE_STATE_SEL_ONLY, this.mShowSelectedOnly);
            if (!mSelectedItems.isEmpty()) {
                mMustReplaceMenue = true;
                getActivity().invalidateOptionsMenu();
            }
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mLastVisiblePosition = galleryView.getLastVisiblePosition();
        outState.putInt(INSTANCE_STATE_LAST_VISIBLE_POSITION, mLastVisiblePosition);
        outState.putString(INSTANCE_STATE_SELECTED_ITEM_IDS, this.mSelectedItems.toString());
        outState.putString(INSTANCE_STATE_OLD_TITLE, this.mOldTitle);
        outState.putBoolean(INSTANCE_STATE_SEL_ONLY, this.mShowSelectedOnly);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Global.debugMemory(debugPrefix, "onCreateView");

        // Inflate the layout for this fragment
        View result = inflater.inflate(R.layout.fragment_gallery, container, false);
        galleryView = (GridView) result.findViewById(R.id.gridView);

        galleryAdapter = new GalleryCursorAdapter(this.getActivity(), mGalleryContentQuery, mSelectedItems, debugPrefix) {
            protected void onLoadFinished(Cursor cursor, StringBuffer debugMessage) {
                super.onLoadFinished(cursor, debugMessage);
                if (cursor != null) {
                    // do not update on destroy
                    multiSelectionReplaceTitleIfNecessary();
                }
            }
        };
        galleryAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (mLastVisiblePosition > 0) {
                    galleryView.smoothScrollToPosition(mLastVisiblePosition);
                    mLastVisiblePosition = -1;
                }
            }
        });
        galleryView.setAdapter(galleryAdapter);

        galleryView.setLongClickable(true);
        galleryView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
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

        galleryView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                onGalleryImageClick((GalleryCursorAdapter.GridCellViewHolder) v.getTag(), position);
            }
        });

        this.parentPathBar = (LinearLayout) result.findViewById(R.id.parent_owner);
        this.parentPathBarScroller = (HorizontalScrollView) result.findViewById(R.id.parent_scroller);

        this.childPathBar = (LinearLayout) result.findViewById(R.id.child_owner);
        this.childPathBarScroller = (HorizontalScrollView) result.findViewById(R.id.child_scroller);

        reloadDirGuiIfAvailable();

        return result;
    }

    @Override
    public void onAttach(Activity activity) {
        Global.debugMemory(debugPrefix, "onAttach");
        super.onAttach(activity);
        mSelectedItems.setContext(activity);
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
    public void onDetach() {
        Global.debugMemory(debugPrefix, "onDetach");
        super.onDetach();
        mGalleryListener = null;
        mDirectoryListener = null;
        mFileCommands = null;
    }

    @Override
    public void onDestroy() {
        Global.debugMemory(debugPrefix, "onDestroy before");
        mGalleryContentQuery = null;
        galleryAdapter.changeCursor(null);
        galleryAdapter = null;
        mFileCommands = null;
        super.onDestroy();
        System.gc();
        Global.debugMemory(debugPrefix, "onDestroy after");
        // RefWatcher refWatcher = AndroFotoFinderApp.getRefWatcher(getActivity());
        // refWatcher.watch(this);
    }


    /**
     * interface Queryable: Owning activity tells fragment to change its content:
     * Initiates a database requery in the background
     *
     * @param context
     * @param parameters
     */
    @Override
    public void requery(Activity context, QueryParameterParcelable parameters) {
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, debugPrefix + "requery " + ((parameters != null) ? parameters.toSqlString() : null));
        }

        this.mGalleryContentQuery = parameters;

        requery();
    }

    private void requery() {
        QueryParameterParcelable selFilter = getCurrentQuery();
        galleryAdapter.requery(this.getActivity(), selFilter);
    }

    private QueryParameterParcelable getCurrentQuery() {
        QueryParameterParcelable selFilter = null;
        if (mShowSelectedOnly) {
            selFilter = new QueryParameterParcelable(mGalleryContentQuery);
            FotoSql.addWhereSelection(selFilter, mSelectedItems);
        } else {
            selFilter = mGalleryContentQuery;
        }
        return selFilter;
    }

    @Override
    public String toString() {
        return debugPrefix + this.galleryAdapter;
    }

    /*********************** local helper *******************************************/
    /** an Image in the FotoGallery was clicked */
    private void onGalleryImageClick(final GalleryCursorAdapter.GridCellViewHolder holder, int position) {
        if ((!multiSelectionHandleClick(holder)) && (mGalleryListener != null) && (mGalleryContentQuery != null)) {
            QueryParameterParcelable imageQuery = new QueryParameterParcelable(mGalleryContentQuery);

            if (holder.filter != null) {
                FotoSql.addWhereFilter(imageQuery, holder.filter);
            }
            long imageID = holder.imageID;
            mGalleryListener.onGalleryImageClick(imageID, getUri(imageID), position);
        }
    }

    /** converts imageID to content-uri */
    private Uri getUri(long imageID) {
        return Uri.parse(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString() + "/" + imageID);
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
            Log.i(Global.LOG_CONTEXT, debugPrefix + " navigateTo : " + absolutePath);
        }

        mCurrentPath = absolutePath;
        reloadDirGuiIfAvailable();
        // requeryGallery(); done by owning activity
    }

    private void reloadDirGuiIfAvailable() {
        if ((mDirectoryRoot != null) && (mCurrentPath != null) && (parentPathBar != null)) {

            parentPathBar.removeAllViews();
            childPathBar.removeAllViews();

            IDirectory selectedChild = mDirectoryRoot.find(mCurrentPath);
            if (selectedChild == null) selectedChild = mDirectoryRoot;

            Button first = null;
            IDirectory current = selectedChild;
            while (current.getParent() != null) {
                Button button = createPathButton(current);
                // add parent left to chlild
                // gui order root/../child.parent/child
                parentPathBar.addView(button, 0);
                if (first == null) first = button;
                current = current.getParent();
            }

            // scroll to right where deepest child is
            if (first != null) parentPathBarScroller.requestChildFocus(parentPathBar, first);

            List<IDirectory> children = selectedChild.getChildren();
            if (children != null) {
                for (IDirectory child : children) {
                    Button button = createPathButton(child);
                    childPathBar.addView(button);
                }
            }
        }
    }

    private Button createPathButton(IDirectory currentDir) {
        Button result = new Button(getActivity());
        result.setTag(currentDir);
        result.setText(getDirectoryDisplayText(null, currentDir, (FotoViewerParameter.includeSubItems) ? Directory.OPT_SUB_ITEM : Directory.OPT_ITEM));

        result.setOnClickListener(onPathButtonClickListener);
        return result;
    }

    /** path/directory was clicked */
    private View.OnClickListener onPathButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onPathButtonClick((IDirectory) v.getTag());
        }
    };

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
        if (mSelectedItems.isEmpty()) {
            // multi selection not active yet: start multi selection
            mOldTitle = getActivity().getTitle().toString();
            mMustReplaceMenue = true;
            mShowSelectedOnly = false;
            getActivity().invalidateOptionsMenu();
            mSelectedItems.add(holder.imageID);
            holder.icon.setVisibility(View.VISIBLE);
            multiSelectionUpdateActionbar();
        } else {
            // in gallery mode long click is view image
            ImageDetailActivityViewPager.showActivity(this.getActivity(), getUri(holder.imageID) , position, getCurrentQuery());
        }
        return true;
    }

    /** return true if multiselection is active */
    private boolean multiSelectionHandleClick(GalleryCursorAdapter.GridCellViewHolder holder) {
        if (!mSelectedItems.isEmpty()) {
            long imageID = holder.imageID;
            holder.icon.setVisibility((mSelectedItems.toggle(imageID)) ? View.VISIBLE : View.GONE);
            multiSelectionUpdateActionbar();
            return true;
        }
        multiSelectionUpdateActionbar();
        return false;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mMustReplaceMenue) {
            mMustReplaceMenue = false;
            menu.clear();
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.menu_gallery_multiselect, menu);
            mShareOnlyToggle = menu.findItem(R.id.cmd_selected_only);
            if (mShowSelectedOnly) {
                mShareOnlyToggle.setIcon(android.R.drawable.checkbox_on_background);
                mShareOnlyToggle.setChecked(true);
            } else {
                inflater.inflate(R.menu.menu_gallery_select_current, menu);
            }

            MenuItem shareItem = menu.findItem(R.id.menu_item_share);
            shareItem.setActionProvider(mShareActionProvider);
            inflater.inflate(R.menu.menu_image_commands, menu);

            multiSelectionUpdateShareIntent();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.cmd_cancel:
                return multiSelectionCancel();
            case R.id.cmd_delete:
                return onDelete();
            case R.id.cmd_selected_only:
                return multiSelectionToggle();

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private boolean onDelete() {
        String fileNames[] = mSelectedItems.getFileNames();
        if (fileNames != null) {
            getFileCommands().deleteFileWithQuestion(fileNames);
        }
        return true;
    }

    private AndroidFileCommands getFileCommands() {
        if (mFileCommands == null) {
            mFileCommands = new AndroidFileCommands(this.getActivity()) {
                @Override
                public void deleteFile(String... paths) {
                    super.deleteFile(paths);
                    mShowSelectedOnly = true;
                    multiSelectionCancel();
                }
            };
        }
        return mFileCommands;
    }

    private boolean multiSelectionToggle() {
        mShowSelectedOnly = !mShowSelectedOnly;
        mMustReplaceMenue = true;
        getActivity().invalidateOptionsMenu();

        requery();
        return true;
    }

    private boolean multiSelectionCancel() {
        mSelectedItems.clear();

        for (int i = galleryView.getChildCount() - 1; i >= 0; i--)
        {
            GalleryCursorAdapter.GridCellViewHolder holder =  (GalleryCursorAdapter.GridCellViewHolder) galleryView.getChildAt(i).getTag();
            if (holder != null) {
                holder.icon.setVisibility(View.GONE);
            }
        }
        multiSelectionUpdateActionbar();
        return true;
    }

    void multiSelectionReplaceTitleIfNecessary() {
        if (!mSelectedItems.isEmpty()) {
            mOldTitle = getActivity().getTitle().toString();
            multiSelectionUpdateActionbar();
        }
    }

    private void multiSelectionUpdateActionbar() {
        String newTitle = null;
        if (mSelectedItems.isEmpty()) {

            // lost last selection. revert mShowSelectedOnly if neccessary
            if (mShowSelectedOnly) {
                mShowSelectedOnly = false;
                requery();
            }

            // lost last selection. revert title if neccessary
            if (mOldTitle != null) {
                // last is deselected. Restore title and menu;
                newTitle = mOldTitle;
                mOldTitle = null;
                getActivity().invalidateOptionsMenu();
            }
        } else {
            // multi selection is active: update title and data for share menue
            newTitle = getActivity().getString(R.string.title_multiselection, mSelectedItems.size());
            multiSelectionUpdateShareIntent();
        }

        if (newTitle != null) {
            getActivity().setTitle(newTitle);
        }
    }

    private void multiSelectionUpdateShareIntent() {
        int selectionCount = mSelectedItems.size();
        if ((selectionCount > 0) && (mShareActionProvider != null)) {
            Intent sendIntent = new Intent();
            sendIntent.setType("image/*");
            if (selectionCount == 1) {
                Long imageId = mSelectedItems.first();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_STREAM, getUri(imageId));
            } else {
                sendIntent.setAction(Intent.ACTION_SEND_MULTIPLE);

                ArrayList<Uri> uris = new ArrayList<Uri>();

                Iterator<Long> iter = mSelectedItems.iterator();
                while (iter.hasNext()) {
                    uris.add(getUri(iter.next()));
                }
                sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            }
            mShareActionProvider.setShareIntent(sendIntent);
        }

    }


}

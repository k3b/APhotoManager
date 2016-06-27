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
 
package de.k3b.android.androFotoFinder.directory;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

// import com.squareup.leakcanary.RefWatcher;

import de.k3b.IBackgroundProcess;
import de.k3b.android.androFotoFinder.FotoGalleryActivity;
import de.k3b.android.androFotoFinder.imagedetail.ImageDetailDialogBuilder;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.queries.FotoThumbSql;
import de.k3b.android.androFotoFinder.queries.FotoViewerParameter;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.util.AndroidFileCommands;
import de.k3b.io.Directory;
import de.k3b.io.DirectoryNavigator;
import de.k3b.io.GalleryFilterParameter;
import de.k3b.io.IDirectory;
import de.k3b.io.OSDirectory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A fragment with a Listing of Directories to be picked.
 *
 * [parentPathBar]
 * [treeView]
 *
 * Activities that contain this fragment must implement the
 * {@link OnDirectoryInteractionListener} interface
 * to handle interaction events.
 */
public class DirectoryPickerFragment extends DialogFragment implements DirectoryGui {

    private static final int FIRST_RADIO = 5000;

    /** executer for background task, that updates status-message and stops if cancel is pressed */
    private abstract class AsyncTaskEx<Params> extends AsyncTask<Params, Integer,Integer> implements IBackgroundProcess<Integer> {
        private final int mProgressMessageResourceId;

        AsyncTaskEx(int progressMessageResourceId) {

            mProgressMessageResourceId = progressMessageResourceId;
        }

        @Override
        public void publishProgress_(Integer... values) {
            publishProgress(values);
        }

        @Override
        public boolean isCancelled_() {
            return isCancelled();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            // values: progressPos,progressCount
            super.onProgressUpdate(values);
            StringBuilder message = new StringBuilder();
            message
                    .append(getActivity().getString(mProgressMessageResourceId))
                    .append(": ").append(values[0])
                    .append(" / ").append(values[1]);
            setStatusMessage(message.toString());
            if (Global.debugEnabled) {
                Log.d(Global.LOG_CONTEXT, message.toString());
            }
        }

        @Override protected void onCancelled() {
            onDirectoryCancel();
        }

        @Override protected void onPostExecute(Integer result) {
            StringBuilder message = new StringBuilder();
            message
                    .append(getActivity().getString(mProgressMessageResourceId))
                    .append(": ").append(result);
            Toast.makeText(getActivity(), message.toString(),
                    Toast.LENGTH_LONG).show();
            onDirectoryCancel();
        }

    }

    private class ThumbRepairTask extends AsyncTaskEx<File> {
        ThumbRepairTask() {
            super(R.string.thumbnails_repair_title);
        }
        @Override
        protected Integer doInBackground(File... params) {
            File tbumbNailDir = params[0];

            Integer step = 1;
            final Context context = getActivity().getApplicationContext();
            int thumbRecordsWithoutParentImage = FotoThumbSql.thumbRecordsDeleteOrphans(context, this, step);
            step += 2;
            ArrayList<Long> dbIds4Delete = new ArrayList<Long>();
            ArrayList<String> files4Delete = new ArrayList<String>();

            // do not delete orphan files because they may belong to video thumbnails
            FotoThumbSql.scanOrphans(context, tbumbNailDir, dbIds4Delete, files4Delete, this, step);
            step += 3;
            int thumbRecordsWithoutFile = FotoThumbSql.deleteThumbRecords(context, dbIds4Delete, this, step++);

            // do not delete orphan files because they may belong to video thumbnails
            int thumbFileWithoutRecord = 0; // FotoThumbSql.deleteThumbFiles(tbumbNailDir, files4Delete, this, step++);
            return Integer.valueOf(thumbRecordsWithoutParentImage + thumbRecordsWithoutFile + thumbFileWithoutRecord);
        }
    }

    private class ThumbDeleteTask extends AsyncTaskEx<String> {
        ThumbDeleteTask() {
            super(R.string.thumbnails_delete_title);
        }

        @Override
        protected Integer doInBackground(String... params) {
            String imageDir = params[0];
            int result = FotoThumbSql.thumbRecordsDeleteByPath(getActivity().getApplicationContext(),
                    imageDir, this, 1);

            return Integer.valueOf(result);
        }
    }

    private class ThumbMoveTask extends AsyncTaskEx<String> {
        ThumbMoveTask() {
            super(R.string.thumbnails_move_title);
        }

        @Override
        protected Integer doInBackground(String... params) {
            String imageDir = params[0];
            File destThumbDir = new File(params[1]);
            int result = FotoThumbSql.thumbRecordsMoveByPath(getActivity().getApplicationContext(),
                    imageDir, destThumbDir, this, 1);

            return Integer.valueOf(result);
        }
    }

    private static final String TAG = "DirFragment";
    private static final java.lang.String INSTANCE_STATE_CONTEXT_MENU = "contextmenu";

    // public state
    private IDirectory mCurrentSelection = null;

    // Layout
    private HorizontalScrollView mParentPathBarScroller;
    private LinearLayout mParentPathBar;
    private ExpandableListView mTreeView;
    private TextView mStatus = null;
    private Button mCmdOk = null;
    private Button mCmdCancel = null;
    private Button mCmdPopup = null;

    private View.OnClickListener mPathButtonClickHandler;
    private View.OnLongClickListener mPathButtonLongClickHandler = null;
    // local data
    protected Activity mContext;
    private DirectoryListAdapter mAdapter;
    private DirectoryNavigator mNavigation;
    private int mDirTypId = 0;
    protected int mTitleId = 0;

    // api to fragment owner or null
    private OnDirectoryInteractionListener mDirectoryListener = null;

    // for debugging
    private static int id = 1;
    private final String debugPrefix;
    private ImageView mImage;
    private int mLastIconID = -1;
    private int mContextMenue = 0;

    // not null when renumber dialog is open.
    private AsyncTask mSubTask = null;
    private AlertDialog mSubDialog = null;

    // onsorted generated by ide-s autocomplete


    public DirectoryPickerFragment() {
        // Required empty public constructor
        debugPrefix = "DirectoryPickerFragment#" + (id++)  + " ";
        Global.debugMemory(debugPrefix, "ctor");
        // Required empty public constructor
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, debugPrefix + "()");
        }
    }

    public DirectoryPickerFragment setTitleId(int titleId) {
        mTitleId = titleId;
        return this;
    }

    public DirectoryPickerFragment setContextMenuId(int contextMenuId) {
        mContextMenue = contextMenuId;
        return this;
    }

    /****** live cycle ********/
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (savedInstanceState != null) {
            this.mContextMenue = savedInstanceState.getInt(INSTANCE_STATE_CONTEXT_MENU, this.mContextMenue);
        }

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_directory, container, false);

        mContext = this.getActivity();
        if (Global.debugEnabled && (mContext != null) && (mContext.getIntent() != null)){
            Log.d(Global.LOG_CONTEXT, "DirectoryPickerFragment onCreateView " + mContext.getIntent().toUri(Intent.URI_INTENT_SCHEME));
        }


        mPathButtonClickHandler = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onParentPathBarButtonClick((IDirectory) v.getTag());
            }
        };

        if (this.mContextMenue != 0) {
            mPathButtonLongClickHandler = new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onShowPopUp(v, (IDirectory) v.getTag());
                    return true;
                }
            };
        }

        this.mParentPathBar = (LinearLayout) view.findViewById(R.id.parent_owner);
        this.mParentPathBarScroller = (HorizontalScrollView) view.findViewById(R.id.parent_scroller);

        mTreeView = (ExpandableListView)view.findViewById(R.id.directory_tree);
        mTreeView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                return DirectoryPickerFragment.this.onChildDirectoryClick(childPosition, mNavigation.getChild(groupPosition, childPosition));
            }
        });

        mTreeView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                return DirectoryPickerFragment.this.onParentDirectoryClick(mNavigation.getGroup(groupPosition));
            }
        });

        if (mContextMenue != 0) {
            mTreeView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int flatPosition, long id) {
                    long packedPos = mTreeView.getExpandableListPosition(flatPosition);
                    int group = mTreeView.getPackedPositionGroup(packedPos);
                    int child = mTreeView.getPackedPositionChild(packedPos);
                    IDirectory directory = (child != -1) ? mNavigation.getChild(group, child) : mNavigation.getGroup(group);
                    onShowPopUp(view, directory);
                    return false;
                }
            });
        }

        this.mImage = (ImageView) view.findViewById(R.id.image);

        if (getShowsDialog()) {
            onCreateViewDialog(view);
        }

        // does nothing if defineDirectoryNavigation() has not been called yet
        reloadTreeViewIfAvailable();
        super.onCreate(savedInstanceState);
        return view;
    }

    /** handle init for dialog-only controlls: cmdOk, cmdCancel, status */
    private void onCreateViewDialog(View view) {
        this.mStatus = (TextView) view.findViewById(R.id.status);
        this.mStatus.setVisibility(View.VISIBLE);
        
        this.mCmdOk = (Button) view.findViewById(R.id.cmd_ok);
        this.mCmdOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDirectoryPick(mCurrentSelection);
            }
        });
        mCmdOk.setVisibility(View.VISIBLE);
        
        mCmdCancel = (Button) view.findViewById(R.id.cmd_cancel);
        if (mCmdCancel != null) {
            mCmdCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onDirectoryCancel();
                }
            });
            mCmdCancel.setVisibility(View.VISIBLE);
        }

        mCmdPopup = null;
        if (mContextMenue != 0) {
            mCmdPopup = (Button) view.findViewById(R.id.cmd_popup);
            if (mCmdPopup != null) {
                mCmdPopup.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onShowPopUp(mCmdPopup, mCurrentSelection);
                    }
                });
                mCmdPopup.setVisibility(View.VISIBLE);
            }
        }

        if (mDirTypId != 0) {
            String title = mContext.getString(
                    R.string.folder_dialog_title_format,
                    FotoSql.getName(mContext,mDirTypId));
            getDialog().setTitle(title);
            // no api for setIcon ????
        } else if (mTitleId != 0) {
            getDialog().setTitle(mTitleId);
        }
    }

    /** called via pathBar-Button-LongClick, tree-item-LongClick, popUp-button */
    private void onShowPopUp(View anchor, IDirectory selection) {
        PopupMenu popup = new PopupMenu(getActivity(), anchor);
        popup.setOnMenuItemClickListener(popUpListener);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(this.mContextMenue, popup.getMenu());
        mPopUpSelection = selection;

        if (Global.useThumbApi) {
            if (getTumbDir(selection) != null) {
                MenuItem thumbMenue = popup.getMenu().findItem(R.id.action_thumbnails_repair);
                if (thumbMenue != null) thumbMenue.setVisible(true);
            }
            MenuItem thumbMenue = popup.getMenu().findItem(R.id.action_thumbnails_delete);
            if (thumbMenue != null) thumbMenue.setVisible(true);

            thumbMenue = popup.getMenu().findItem(R.id.action_thumbnails_move);
            if (thumbMenue != null) thumbMenue.setVisible(true);
        }
        popup.show();
    }

    private static File getTumbDir(IDirectory selection) {
        if (selection != null) {
            File thumbsDir = new File(selection.getAbsolute(),FotoThumbSql.THUMBNAIL_DIR_NAME);
            if (thumbsDir.exists()) return thumbsDir;
        }
        return null;
    }
    private IDirectory mPopUpSelection = null;
    private final PopupMenu.OnMenuItemClickListener popUpListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            return onPopUpClick(menuItem);
        }
    };

    private boolean onPopUpClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.cmd_mk_dir:
                return onCreateSubDirQuestion(mPopUpSelection);
            case R.id.cmd_gallery:
                return showGallery(mPopUpSelection);
            case R.id.action_details:
                return showDirInfo(mPopUpSelection);
            case R.id.action_thumbnails_repair:
                return onThumbRepair(mPopUpSelection);
            case R.id.action_thumbnails_delete:
                return onThumbDeleteQuestion(mPopUpSelection);
            case R.id.action_thumbnails_move:
                return onThumbMoveQuestion(mPopUpSelection);
        }
        return false;
    }

    private boolean onCreateSubDirQuestion(final IDirectory parentDir) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.mk_dir_menu_title);
        View content = getActivity().getLayoutInflater().inflate(R.layout.dialog_edit_name, null);

        final EditText edit = (EditText) content.findViewById(R.id.edName);
        String defaultName = getString(R.string.mk_dir_default);
        edit.setText(defaultName);
        edit.setSelection(0, defaultName.length());

        builder.setView(content);
        builder.setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();mSubDialog=null;
            }
        });
        builder.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
            //@Override
            public void onClick(DialogInterface dialog, int which) {
                onCreateSubDirAnswer(parentDir, edit.getText().toString());
                mSubDialog=null;
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        int width = (int ) (8 * edit.getTextSize());
        // DisplayMetrics metrics = getResources().getDisplayMetrics();
        // int width = metrics.widthPixels;
        alertDialog.getWindow().setLayout(width*2, LinearLayout.LayoutParams.WRAP_CONTENT);
        mSubDialog = alertDialog;
        return true;
    }

    private void onCreateSubDirAnswer(final IDirectory parentDir, String newCildFolderName) {
        OSDirectory newChild = ((OSDirectory) parentDir).addChildFolder(newCildFolderName);

        if (newChild != null) {
            String newPathAbsolute = newChild.getAbsolute();

            int msgId;
            if (newChild.osMkDirs()) {
                AndroidFileCommands.log(getActivity(), "mkdirs \"", newPathAbsolute, "\"").closeLogFile();
                msgId = R.string.mk_success_format;
                reloadTreeViewIfAvailable();
                onParentPathBarButtonClick(newChild);
            } else {
                msgId = R.string.mk_err_failed_format;
                parentDir.getChildren().remove(newChild);
                newChild.destroy();
            }
            Toast.makeText(getActivity(), getActivity().getString(msgId, newPathAbsolute),
                    Toast.LENGTH_LONG).show();
        }
    }

    public boolean onThumbDeleteQuestion(final IDirectory selectedDir) {
        String pathFilter = (selectedDir != null) ? selectedDir.getAbsolute() : null;
        if (pathFilter != null) {

            final String message = mContext
                    .getString(R.string.delete_question_message_format, 
                            FotoThumbSql.formatThumbStatistic(getActivity(),pathFilter,new StringBuilder()));

            final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            final String title = mContext.getText(R.string.delete_question_title)
                    .toString();

            builder.setTitle(title);
            builder.setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.btn_yes,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(
                                        final DialogInterface dialog,
                                        final int id) {
                                    mSubDialog = null;
                                    onThumbDeleteAnswer(selectedDir);
                                }
                            }
                    )
                    .setNegativeButton(R.string.btn_no,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(
                                        final DialogInterface dialog,
                                        final int id) {
                                    mSubDialog = null;
                                    dialog.cancel();
                                }
                            }
                    );

            final AlertDialog alert = builder.create();
            mSubDialog = alert;
            alert.show();
        }
        return true;
    }


    private void onThumbDeleteAnswer(IDirectory selectedDir) {
        String pathFilter = (selectedDir != null) ? selectedDir.getAbsolute() : null;
        if (pathFilter != null) {
            new ThumbDeleteTask().execute(pathFilter);
        }
    }

    public boolean onThumbMoveQuestion(final IDirectory selectedDir) {
        String pathFilter = (selectedDir != null) ? selectedDir.getAbsolute() : null;
        if (pathFilter != null) {
            File[] thumbDirs = FotoThumbSql.getThumbRootFiles();
            if ((thumbDirs == null) || thumbDirs.length < 2) {
                Toast.makeText(getActivity(), R.string.thumbnails_dir_not_found,
                        Toast.LENGTH_LONG).show();
                return false;
            }

            final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            final String title = mContext.getText(R.string.thumbnails_move_title)
                    .toString();

            final RadioGroup group = new RadioGroup(getActivity());

            int id = FIRST_RADIO;
            RadioButton radioButton = null;
            for (File thumbDir : thumbDirs) {
                if (thumbDir != null) {
                    radioButton = new RadioButton(mContext);
                    radioButton.setText(thumbDir.toString());
                    radioButton.setId(id++);
                    group.addView(radioButton);
                }
            }

            // select the last radio button
            if (radioButton != null) radioButton.setChecked(true);

            builder.setTitle(title);
            builder.setView(group)
                    .setCancelable(false)
                    .setPositiveButton(R.string.btn_yes,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(
                                        final DialogInterface dialog,
                                        final int id) {
                                    RadioButton checked = (RadioButton) group.findViewById(group.getCheckedRadioButtonId());
                                    if (checked != null) {
                                        mSubDialog = null;
                                        onThumbMoveAnswer(selectedDir, checked.getText().toString());
                                    }
                                }
                            }
                    )
                    .setNegativeButton(R.string.btn_no,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(
                                        final DialogInterface dialog,
                                        final int id) {
                                    mSubDialog = null;
                                    dialog.cancel();
                                }
                            }
                    );

            final AlertDialog alert = builder.create();
            mSubDialog = alert;
            alert.show();
        }
        return true;
    }

    private void onThumbMoveAnswer(IDirectory selectedDir, String trgetDir) {
        String pathFilter = (selectedDir != null) ? selectedDir.getAbsolute() : null;
        if (pathFilter != null) {
            new ThumbMoveTask().execute(pathFilter, trgetDir);
        }
    }

    private boolean onThumbRepair(IDirectory selectedDir) {
        File tbumbNailDir = getTumbDir(selectedDir);
        if (tbumbNailDir != null) {
            this.mSubTask = new ThumbRepairTask();
            mSubTask.execute(tbumbNailDir);
            return true;
        }
        return false;
    }

    private boolean showDirInfo(IDirectory selectedDir) {
        String pathFilter = (selectedDir != null) ? selectedDir.getAbsolute() : null;
        if (pathFilter != null) {

            ImageDetailDialogBuilder.createImageDetailDialog(
                    this.getActivity(),
                    pathFilter,
                    FotoThumbSql.formatDirStatistic(this.getActivity(), pathFilter)
            ).show();
            return true;
        }
        return false;
    }

    private boolean showGallery(IDirectory selectedDir) {
        String pathFilter = (selectedDir != null) ? selectedDir.getAbsolute() : null;
        if (pathFilter != null) {
            GalleryFilterParameter filter = new GalleryFilterParameter(); //.setPath(pathFilter);
            if (!FotoSql.set(filter, pathFilter, mDirTypId))
            {
                filter.setPath(pathFilter + "/%");
            }

            FotoGalleryActivity.showActivity(this.getActivity(), filter, null, 0);
            return true;
        }
        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(INSTANCE_STATE_CONTEXT_MENU, mContextMenue);
    }

    private void onDirectoryCancel() {
        closeAll();
        Log.d(Global.LOG_CONTEXT, debugPrefix + "onCancel: " + mCurrentSelection);
        if (mDirectoryListener != null) mDirectoryListener.onDirectoryCancel(mDirTypId);
        dismiss();
    }

    protected void onDirectoryPick(IDirectory selection) {
        closeAll();
        Log.d(Global.LOG_CONTEXT, debugPrefix + "onDirectoryPick: " + selection);
        if ((mDirectoryListener != null) && (selection != null)) {
            mDirectoryListener.onDirectoryPick(selection.getAbsolute()
                    , mDirTypId);
            dismiss();
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog result = super.onCreateDialog(savedInstanceState);

        return result;
    };

    public void onResume() {
        super.onResume();

        // after rotation dlg has no more data: close it
        if (this.getShowsDialog() && (this.mNavigation == null)) {
            dismiss();
        }
    }

    public void closeAll() {
        if (mSubDialog != null) mSubDialog.dismiss();
        mSubDialog = null;
        if (mSubTask != null) mSubTask.cancel(false);
        mSubTask = null;
    }

    @Override public void onDestroy() {
        closeAll();
        super.onDestroy();
        // RefWatcher refWatcher = AndroFotoFinderApp.getRefWatcher(getActivity());
        // refWatcher.watch(this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        setDirectoryListener(activity);
    }

    protected void setDirectoryListener(Activity activity) {
        try {
            mDirectoryListener = (OnDirectoryInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnDirectoryInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mDirectoryListener = null;
    }

    /*********************** gui interaction *******************************************/
    private boolean onParentDirectoryClick(IDirectory dir) {
        updateParentPathBar(dir);
        notifySelectionChanged(dir);
        return false;
    }

    private boolean onChildDirectoryClick(int childPosition, IDirectory selectedChild) {
        Log.d(TAG, debugPrefix + "onChildDirectoryClick(" +
                selectedChild.getAbsolute() + ")");

        // naviationchange only if there are children below child
        IDirectory newGrandParent = ((selectedChild != null) && (Directory.getChildCount(selectedChild) > 0)) ? selectedChild.getParent() : null;

        navigateTo(childPosition, newGrandParent);
        updateParentPathBar(selectedChild);
        notifySelectionChanged(selectedChild);
        return false;
    }

    private void onParentPathBarButtonClick(IDirectory selectedChild) {
        Log.d(TAG, debugPrefix + "onParentPathBarButtonClick(" +
                selectedChild.getAbsolute() + ")");

        // naviationchange only if there are children below child
        IDirectory newGrandParent = ((selectedChild != null) && (Directory.getChildCount(selectedChild) > 0)) ? selectedChild.getParent() : null;
        List<IDirectory> siblings = (newGrandParent != null) ? newGrandParent.getChildren() : null;

        if (siblings != null) {
            int childPosition = siblings.indexOf(selectedChild);
            navigateTo(childPosition, newGrandParent);
        }
        updateParentPathBar(selectedChild);
        notifySelectionChanged(selectedChild);
    }

    protected void notifySelectionChanged(IDirectory selectedChild) {
        if ((mDirectoryListener != null) && (selectedChild != null)) mDirectoryListener.onDirectorySelectionChanged(selectedChild.getAbsolute(), mDirTypId);
    }

    /**
     * To be overwritten to check if a path can be picked.
     *
     * @param path to be checked if it cannot be handled
     * @return null if no error else error message with the reason why it cannot be selected
     */
    protected String getStatusErrorMessage(String path) {
        return null;
    }

    private void updateStatus() {
        int itemCount = getItemCount(mCurrentSelection);

        String selectedPath = (this.mCurrentSelection != null) ? this.mCurrentSelection.getAbsolute() : null;

        String statusMessage = (itemCount == 0) ? mContext.getString(R.string.selection_none_hint)  : getStatusErrorMessage(selectedPath);
        boolean canPressOk = (statusMessage == null);

        if (mCmdOk != null) mCmdOk.setEnabled(canPressOk);
        if (mCmdPopup != null) mCmdPopup.setEnabled(canPressOk);

        setStatusMessage(statusMessage);
    }

    private void setStatusMessage(String statusMessage) {
        if (mStatus != null) {
            if (statusMessage == null) {
                mStatus.setText(this.mCurrentSelection.getAbsolute());
            } else {
                mStatus.setText(statusMessage);
            }
        }
    }

    private int getItemCount(IDirectory _directory) {
        if ((_directory == null) || (!(_directory instanceof Directory))) return 1;

        Directory directory = (Directory) _directory;
        return (FotoViewerParameter.includeSubItems)
                        ? directory.getNonDirSubItemCount()
                        : directory.getNonDirItemCount();
    }

    /*********************** local helper *******************************************/
    private void updateParentPathBar(String currentSelection) {
        if (this.mNavigation != null) {
            updateParentPathBar(this.mNavigation.getRoot().find(currentSelection));
        }
    }

    private void updateParentPathBar(IDirectory selectedChild) {
        mParentPathBar.removeAllViews();

        if (selectedChild != null) {

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
            mParentPathBarScroller.requestChildFocus(mParentPathBar, first);
        }

        if (mImage != null) {
            updateBitmap(selectedChild.getIconID());
        }

        this.mCurrentSelection = selectedChild;

        updateStatus();
    }

    private void updateBitmap(int iconID) {
        if (Global.debugEnabledViewItem) {
            Log.d(Global.LOG_CONTEXT, debugPrefix + "updateBitmap#" + iconID);
        }
        if (mLastIconID != iconID) {
            mLastIconID = iconID;
            if (mLastIconID == 0) {
                this.mImage.setVisibility(View.GONE);
            } else {
                this.mImage.setVisibility(View.VISIBLE);
                this.mImage.setImageBitmap(getBitmap(mLastIconID));
            }
        }
    }

    private Bitmap getBitmap(int id) {
        final Bitmap thumbnail = MediaStore.Images.Thumbnails.getThumbnail(
                getActivity().getContentResolver(),
                id,
                MediaStore.Images.Thumbnails.MICRO_KIND,
                new BitmapFactory.Options());

        return thumbnail;
    }


    private Button createPathButton(IDirectory currentDir) {
        Button result = new Button(getActivity());
        result.setTag(currentDir);
        result.setText(DirectoryListAdapter.getDirectoryDisplayText(null, currentDir, (FotoViewerParameter.includeSubItems) ? Directory.OPT_SUB_ITEM : Directory.OPT_ITEM));

        result.setOnClickListener(mPathButtonClickHandler);
        if (mPathButtonLongClickHandler != null) {
            result.setOnLongClickListener(mPathButtonLongClickHandler);
        }
        return result;
    }

    /**
     * DirectoryGui-Public api for embedding activity
     * (Re)-Defines base parameters for Directory Navigation
     * @param root
     * @param dirTypId
     * @param initialAbsolutePath
     */
    @Override
    public void defineDirectoryNavigation(IDirectory root, int dirTypId, String initialAbsolutePath) {
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, debugPrefix + " defineDirectoryNavigation : " + initialAbsolutePath);
        }

        mDirTypId = dirTypId;
        if (root != null)
            mNavigation = new DirectoryNavigator(root);

        navigateTo(initialAbsolutePath);
    }

    /** refreshLocal tree to new newGrandParent by preserving selection */
    private void navigateTo(int newGroupSelection, IDirectory newGrandParent) {
        if (newGrandParent != null) {
            Log.d(TAG, debugPrefix + "navigateTo(" +
                    newGrandParent.getAbsolute() + ")");
            mNavigation.setCurrentGrandFather(newGrandParent);
            this.mTreeView.setAdapter(mAdapter);
            if (newGroupSelection >= 0) {
                /// find selectedChild as new selectedGroup and expand it
                mTreeView.expandGroup(newGroupSelection, true);
            }
        }
    }

    /**
     * Set curent selection to absolutePath
     *
     * @param absolutePath
     */
    @Override
    public void navigateTo(String absolutePath) {
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, debugPrefix + " navigateTo : " + absolutePath);
        }

        if ((mNavigation != null) && (absolutePath != null)) {
            mCurrentSelection = mNavigation.getRoot().find(absolutePath);
            mNavigation.navigateTo(mCurrentSelection);
        }

        // does nothing if OnCreate() has not been called yet

        reloadTreeViewIfAvailable();
    }

    /** Does nothing if either OnCreate() or defineDirectoryNavigation() has NOT been called yet */
    private boolean reloadTreeViewIfAvailable() {
        if ((mTreeView != null) && (mNavigation != null)) {
            if (mAdapter == null) {
                mAdapter = new DirectoryListAdapter(this.mContext,
                        mNavigation, mTreeView, debugPrefix);
            }
            mTreeView.setAdapter(mAdapter);

            int g = mNavigation.getLastNavigateToGroupPosition();
            if (g != DirectoryNavigator.UNDEFINED) {
                mTreeView.expandGroup(g, true);
                updateParentPathBar(mNavigation.getCurrentSelection());
            }

            return true;
        }
        return false;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnDirectoryInteractionListener {
        /** called when user picks a new directory */
        void onDirectoryPick(String selectedAbsolutePath, int queryTypeId);

        /** called when user cancels picking of a new directory
         * @param queryTypeId*/
        void onDirectoryCancel(int queryTypeId);

        /** called after the selection in tree has changed */
        void onDirectorySelectionChanged(String selectedChild, int queryTypeId);

        /** remove cached directories
         * @param why*/
        void invalidateDirectories(String why);
    }
}

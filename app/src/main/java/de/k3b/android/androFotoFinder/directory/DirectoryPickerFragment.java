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
 
package de.k3b.android.androFotoFinder.directory;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import de.k3b.FotoLibGlobal;
import de.k3b.android.androFotoFinder.FotoGalleryActivity;
import de.k3b.android.androFotoFinder.PhotoAutoprocessingEditActivity;
import de.k3b.android.androFotoFinder.ThumbNailUtils;
import de.k3b.android.androFotoFinder.imagedetail.ImageDetailActivityViewPager;
import de.k3b.android.androFotoFinder.imagedetail.ImageDetailMetaDialogBuilder;
import de.k3b.android.androFotoFinder.queries.AndroidAlbumUtils;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.queries.FotoThumbSql;
import de.k3b.android.androFotoFinder.queries.FotoViewerParameter;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.util.AndroidFileCommands;
import de.k3b.android.util.ClipboardUtil;
import de.k3b.android.util.FileManagerUtil;
import de.k3b.android.util.IntentUtil;
import de.k3b.android.util.MediaScanner;
import de.k3b.android.widget.Dialogs;
import de.k3b.database.QueryParameter;
import de.k3b.io.AlbumFile;
import de.k3b.io.GalleryFilterParameter;
import de.k3b.io.ListUtils;
import de.k3b.io.VISIBILITY;
import de.k3b.io.collections.SelectedFiles;
import de.k3b.io.Directory;
import de.k3b.io.DirectoryNavigator;
import de.k3b.io.FileUtils;
import de.k3b.io.IDirectory;
import de.k3b.io.OSDirectory;
import de.k3b.io.StringUtils;

import java.io.File;
import java.util.HashMap;
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
    /** executer for background task, that updates status-message and stops if cancel is pressed */
    private abstract class AsyncTaskEx<Params> extends AsyncTask<Params, Integer,Integer> {
        private final int mProgressMessageResourceId;

        AsyncTaskEx(int progressMessageResourceId) {

            mProgressMessageResourceId = progressMessageResourceId;
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
            String message = getActivity().getString(mProgressMessageResourceId) +
                    ": " + result;
            Toast.makeText(getActivity(), message,
                    Toast.LENGTH_LONG).show();
            onDirectoryCancel();
        }

    }

    private static final String TAG = "DirFragment";
    private static final java.lang.String INSTANCE_STATE_CONTEXT_MENU = "contextmenu";

    // public state
    protected IDirectory mCurrentSelection = null;

    private IDirectory mLastPopUpSelection = null;

    // Layout
    private HorizontalScrollView mParentPathBarScroller;
    private LinearLayout mParentPathBar;
    private ExpandableListView mTreeView;
    private TextView mStatus = null;
    private Button mCmdOk = null;
    private Button mCmdPopup = null;

    private View.OnClickListener mPathButtonClickHandler;
    private View.OnLongClickListener mPathButtonLongClickHandler = null;
    // local data
    protected Activity mContext;
    private DirectoryListAdapter mAdapter;
    protected DirectoryNavigator mNavigation;
    private int mDirTypId = 0;
    protected int mTitleId = 0;

    // api to fragment owner or null
    private OnDirectoryInteractionListener mDirectoryListener = null;
    private OnDirectoryPickListener mDirectoryPickListener = null;

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

    public IDirectory getLastPopUpSelection() {
        return mLastPopUpSelection;
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

        Button cmdCancel = (Button) view.findViewById(R.id.cmd_cancel);
        if (cmdCancel != null) {
            cmdCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onDirectoryCancel();
                }
            });
            cmdCancel.setVisibility(View.VISIBLE);
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
        PopupMenu popup = onCreatePopupMenu(anchor, selection);

        if (popup != null) {
            popup.show();
        }
    }

    @NonNull
    private PopupMenu onCreatePopupMenu(View anchor, IDirectory selection) {
        PopupMenu popup = new PopupMenu(getActivity(), anchor);
        popup.setOnMenuItemClickListener(popUpListener);
        MenuInflater inflater = popup.getMenuInflater();
        Menu menu = popup.getMenu();
        inflater.inflate(this.mContextMenue, menu);
        if (selection != null) {
            String absoluteSelectedPath = selection.getAbsolute();
            boolean isAlbumFile = AlbumFile.isQueryFile(absoluteSelectedPath);
            mPopUpSelection = selection;

            setMenuVisibility(menu, R.id.cmd_fix_link, FileUtils.isSymlinkDir(new File(absoluteSelectedPath), false));
            setMenuVisibility(menu, R.id.cmd_folder_hide_images, !isAlbumFile && MediaScanner.canHideFolderMedia(absoluteSelectedPath));

            setMenuVisibility(menu, R.id.cmd_apm_edit, !isAlbumFile && FotoLibGlobal.apmEnabled);

            setMenuVisibility(menu, R.id.cmd_filemanager, !isAlbumFile && FileManagerUtil.hasShowInFilemanager(getActivity(), absoluteSelectedPath));
            setMenuVisibility(menu, R.id.cmd_delete, isAlbumFile);
            setMenuVisibility(menu, R.id.action_edit, isAlbumFile);

            setMenuVisibility(menu, android.R.id.copy, !isAlbumFile);

        }
        return popup;
    }

    private void setMenuVisibility(Menu menu, int menuId, boolean visibility) {
        MenuItem menuItem = menu.findItem(menuId);
        if (menuItem != null) {
            menuItem.setVisible(visibility);
        }

    }

    private IDirectory mPopUpSelection = null;
    private final PopupMenu.OnMenuItemClickListener popUpListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            return onPopUpClick(menuItem, mPopUpSelection);
        }
    };

    protected boolean onPopUpClick(MenuItem menuItem, IDirectory popUpSelection) {
        this.mLastPopUpSelection = popUpSelection;
        switch (menuItem.getItemId()) {
            case R.id.cmd_mk_dir:
                return onCreateSubDirQuestion(popUpSelection);
            case R.id.cmd_apm_edit:
                return onEditApm(popUpSelection);
            case android.R.id.copy:
                return onCopy(popUpSelection);

            case R.id.menu_item_rename:
                return onRenameDirQuestion(popUpSelection);
            case R.id.cmd_delete:
                return onDeleteDirQuestion(popUpSelection);

            case R.id.action_edit:
                return onEdit(popUpSelection);

            case R.id.cmd_photo:
                return showPhoto(popUpSelection, mDirTypId);
            case R.id.cmd_gallery:
                return showGallery(popUpSelection, mDirTypId);
            case R.id.cmd_filemanager:
                return FileManagerUtil.showInFilemanager(getActivity(), popUpSelection.getAbsolute());
            case R.id.action_details:
                return showDirInfo(popUpSelection);
            case R.id.cmd_fix_link:
                return fixLinks(popUpSelection);
            case R.id.cmd_folder_hide_images:
                onHideFolderMediaQuestion(popUpSelection.getAbsolute());
                return true;
            default:break;
        }
        return false;
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == R.id.cmd_apm_edit) && (resultCode == Activity.RESULT_OK) && (mPopUpSelection != null)) {
            // autoprocessing status may have changed: refresh data and gui
            mPopUpSelection.refresh();
            // notifyDataSetChanged();
            mAdapter = null;
            reloadTreeViewIfAvailable();
        }
    }

    public void notifyDataSetChanged() {
        if (this.mAdapter != null) this.mAdapter.notifyDataSetChanged();
    }

    private boolean onCopy(IDirectory selection) {
        String path = (selection == null) ? null : selection.getAbsolute();
        return ClipboardUtil.addDirToClipboard(this.getActivity(), path);
    }

    private boolean onEditApm(IDirectory selection) {
        String path = (selection == null) ? null : selection.getAbsolute();
        if (!StringUtils.isNullOrEmpty(path)) {
            PhotoAutoprocessingEditActivity.showActivity("onEditApm(" + selection +
                    ")", getActivity(), null, path, getSrcFotos(), R.id.cmd_apm_edit);
            return true;
        }
        return false;
    }

    private boolean onHideFolderMediaQuestion(final String path) {
        if (AndroidFileCommands.canProcessFile(mContext, false)) {
            Dialogs dlg = new Dialogs() {
                @Override
                protected void onDialogResult(String result, Object[] parameters) {
                    if (result != null) {
                        MediaScanner.hideFolderMedia(mContext, path);
                        onDirectoryCancel();
                        if (mDirectoryListener != null) mDirectoryListener.invalidateDirectories("hide folder " + path);
                    }
                }
            };

            dlg.yesNoQuestion(mContext, mContext.getString(R.string.folder_hide_images_menu_title),
                    mContext.getString(R.string.folder_hide_images_question_message_format, path));
        } // else toast "cannot process because scanner is active"
        return true;
    }

    private boolean onEdit(IDirectory dir) {
        if (dir != null) {
            File file = FileUtils.tryGetCanonicalFile(dir.getAbsolute());
            Intent sendIntent = new Intent();
            IntentUtil.setDataAndTypeAndNormalize(sendIntent, Uri.fromFile(file), "text/plain");
            sendIntent.setAction(Intent.ACTION_EDIT);
            mContext.startActivity(sendIntent);
            return true;
        }
        return false;
    }

    private boolean onDeleteDirQuestion(final IDirectory parentDir) {
        Dialogs dlg = new Dialogs() {
            @Override protected void onDialogResult(String result, Object[] parameters) {
                if (result != null) {
                    if (parentDir != null) {
                        File parentFile = FileUtils.tryGetCanonicalFile(parentDir.getAbsolute());
                        if (parentFile != null) {
                            onDeleteAnswer(parentFile, parentDir);
                        }
                    }
                }
            }
        };

        dlg.yesNoQuestion(mContext, parentDir.getRelPath(), mContext.getString(R.string.bookmark_delete_question));
        return true;
    }

    private void onDeleteAnswer(File file, IDirectory dir) {
        boolean deleteSuccess = false;

        // delete from filesystem
        if (file.exists() && file.delete()) {
            deleteSuccess = true;
        }

        // delete from database
        if (FotoSql.deleteMedia("delete album", getActivity(),
                ListUtils.toStringList(file.getAbsolutePath()),false) > 0) {
            deleteSuccess = true;
        }

        // delete from dir tree
        IDirectory parent = (dir != null) ? dir.getParent() :null;
        if (parent != null) {
            parent.getChildren().remove(dir);
            dir.destroy();
        }

        String message = (deleteSuccess)
                ? mContext.getString(R.string.bookmark_delete_answer_format, file.getAbsoluteFile() )
                : mContext.getString(R.string.bookmark_delete_error_format, file.getAbsoluteFile() );
        Toast.makeText(mContext,
                message,
                Toast.LENGTH_LONG).show();
        Log.d(Global.LOG_CONTEXT, message);

        // notifyDataSetChanged();
        mAdapter = null;
        reloadTreeViewIfAvailable();
    }
    private boolean onRenameDirQuestion(final IDirectory parentDir) {
        if (parentDir != null) {
			File parentFile = FileUtils.tryGetCanonicalFile(parentDir.getAbsolute());
			if (parentFile != null) {
				String defaultName = parentFile.getName();
				Dialogs dialog = new Dialogs() {
					@Override
					protected void onDialogResult(String newFileName, Object... parameters) {
						if (newFileName != null) {
							onRenameDirAnswer(parentDir, newFileName);
						}
						mSubDialog = null;
					}
				};
				mSubDialog = dialog.editFileName(getActivity(), getString(R.string.rename_menu_title), defaultName);
				return true;
			}
        }
        return false;
    }

    private void onRenameDirAnswer(final IDirectory srcDir, String newFolderName) {
        int modified = -1;
        File srcDirFile = (srcDir != null) ? FileUtils.tryGetCanonicalFile(srcDir.getAbsolute()) : null;
        if (srcDirFile != null) {
            AndroidFileCommands cmd
                    = new AndroidFileCommands()
                    .setContext(getActivity())
                    .openDefaultLogFile();
            modified = cmd.execRename(srcDirFile, newFolderName);
            cmd.closeAll();
        }

        if (modified <= 0) {
            Toast.makeText(getActivity(), getActivity().getString(R.string.image_err_file_rename_format, srcDirFile.getAbsolutePath()),
                    Toast.LENGTH_LONG).show();
        } else {
            // update dirpicker
            srcDir.rename(srcDirFile.getName(), newFolderName);
            notifyDataSetChanged();
        }
    }

    private boolean onCreateSubDirQuestion(final IDirectory parentDir) {
        String defaultName = getString(R.string.mk_dir_default);
        Dialogs dialog = new Dialogs() {
            @Override
            protected void onDialogResult(String newFileName, Object... parameters) {
                if (newFileName != null) {
                    onCreateSubDirAnswer(parentDir, newFileName);
                }
                mSubDialog=null;
            }
        };
        mSubDialog = dialog.editFileName(getActivity(), getString(R.string.mk_dir_menu_title), defaultName);
        return true;
    }

    private void onCreateSubDirAnswer(final IDirectory parentDir, String newCildFolderName) {
        OSDirectory newChild = ((OSDirectory) parentDir).addChildFolder(newCildFolderName);

        if (newChild != null) {
            String newPathAbsolute = newChild.getAbsolute();

            int msgId;
            if (newChild.osMkDirs()) {
                // apmMove.cmd and apmCopy.cmd create dir on demand
                AndroidFileCommands logger = AndroidFileCommands.createFileCommand(getActivity(), false);
                logger.log("rem mkdir \"", newPathAbsolute, "\"");
                logger.closeLogFile();
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

    private boolean fixLinks(IDirectory linkDir) {
        Context context = getActivity();
        String linkPath = (linkDir != null) ? linkDir.getAbsolute() : null;
        if (linkPath != null) {
            File linkFile = new File(linkPath);
            if (FileUtils.isSymlinkDir(linkFile, false)) {
                String canonicalPath = FileUtils.tryGetCanonicalPath(linkFile, null);
                if ((canonicalPath != null) && linkFile.exists() && linkFile.isDirectory()) {
                    if (!linkPath.endsWith("/")) linkPath+="/";
                    if (!canonicalPath.endsWith("/")) canonicalPath+="/";

                    String sqlWhereLink = FotoSql.SQL_COL_PATH + " like '" + linkPath + "%'";
                    SelectedFiles linkFiles = FotoSql.getSelectedfiles(context, sqlWhereLink, VISIBILITY.PRIVATE_PUBLIC);

                    String sqlWhereCanonical = FotoSql.SQL_COL_PATH + " in (" + linkFiles.toString() + ")";
                    sqlWhereCanonical = sqlWhereCanonical.replace(linkPath,canonicalPath);
                    SelectedFiles canonicalFiles = FotoSql.getSelectedfiles(context, sqlWhereCanonical, VISIBILITY.PRIVATE_PUBLIC);
                    HashMap<String, String> link2canonical = new HashMap<String, String>();
                    for(String cann : canonicalFiles.getFileNames()) {
                        link2canonical.put(linkPath + cann.substring(canonicalPath.length()), cann);
                    }

                    if (Global.debugEnabled) {
                        Log.d(Global.LOG_CONTEXT, "\tlinkFiles      " + linkFiles.toString());
                        Log.d(Global.LOG_CONTEXT, "\tcanonicalFiles " + canonicalFiles.toString());
                    }

                    ContentValues updateValues = new ContentValues(2);

                    String[] linkFileNames = linkFiles.getFileNames();
                    Long[] linkIds = linkFiles.getIds();
                    for(int i = linkFileNames.length -1; i >= 0; i--) {
                        String lin = linkFileNames[i];
                        String cann = link2canonical.get(lin);
                        if (Global.debugEnabled) {
                            Log.d(Global.LOG_CONTEXT, "\t\tmap " + lin + "\t-> " + cann);
                        }

                        if (cann == null) {
                            // rename linkFile to canonicalFile
                            updateValues.put(FotoSql.SQL_COL_PATH, canonicalPath + lin.substring(linkPath.length()));
                            FotoSql.execUpdate("fixLinks", context, linkIds[i].intValue() ,updateValues);
                        } else {
                            FotoSql.deleteMedia("DirectoryPickerFragment.fixLinks", context, FotoSql.FILTER_COL_PK, new String[] {linkIds[i].toString()}, true);
                        }
                    }
                    MediaScanner.notifyChanges(context, "Fixed link/canonical duplicates");


                }
                return true;
            }
        }
        return false;
    }

    private boolean showDirInfo(IDirectory selectedDir) {
        String pathFilter = (selectedDir != null) ? selectedDir.getAbsolute() : null;
        if (pathFilter != null) {

            ImageDetailMetaDialogBuilder.createImageDetailDialog(
                    this.getActivity(),
                    pathFilter,
                    FotoThumbSql.formatDirStatistic(this.getActivity(), pathFilter)
            ).show();
            return true;
        }
        return false;
    }

    private QueryParameter getSelectionQuery(String dbgContext, IDirectory selectedDir, int dirTypId) {
        String pathFilter = (selectedDir != null) ? selectedDir.getAbsolute() : null;
        QueryParameter query = null;
        if (pathFilter != null) {

            if(dirTypId == FotoSql.QUERY_TYPE_GROUP_ALBUM) {
                String pathWithWildcard = pathFilter + "/%";
                query = AndroidAlbumUtils.getQueryFromUri(dbgContext, getActivity(), Uri.fromFile(new File(pathWithWildcard)), null);
            } else {
                GalleryFilterParameter filterParameter = new GalleryFilterParameter();
                FotoSql.set(filterParameter, pathFilter, dirTypId);
                return AndroidAlbumUtils.getAsMergedNewQueryParameter(null, filterParameter);
            }
        }
        return query;

    }
    private boolean showPhoto(IDirectory selectedDir, int dirTypId) {
        QueryParameter query = getSelectionQuery("showPhoto", selectedDir, dirTypId);
        if (query != null) {
            String dbgContext = "showPhoto(" + FotoSql.getName(mContext,dirTypId) + ":" + selectedDir + ")";

            FotoSql.setSort(query, FotoSql.SORT_BY_DATE, false);
            ImageDetailActivityViewPager.showActivity(dbgContext, this.getActivity(), null, 0, query, 0);
            return true;
        }
        return false;
    }

    private boolean showGallery(IDirectory selectedDir, int dirTypId) {
        String dbgContext = "showGallery(" + FotoSql.getName(mContext,dirTypId) + ":" + selectedDir + ")";
        QueryParameter query = getSelectionQuery(dbgContext, selectedDir, dirTypId);
        if (query != null) {
            FotoGalleryActivity.showActivity(dbgContext, this.getActivity(), query, 0);
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
        if ((mDirectoryPickListener != null) && (selection != null)) {
            mDirectoryPickListener.onDirectoryPick(selection.getAbsolute()
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
        if (this.getShowsDialog() && (this.mNavigation == null) && isResumed()) {
            // isResumed() else Can not perform this action after onSaveInstanceState
            // http://stackoverflow.com/a/23034285/519334
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
        mLastPopUpSelection = null;
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
            if ((activity == null) || activity instanceof OnDirectoryInteractionListener) {
                mDirectoryListener = (OnDirectoryInteractionListener) activity;
            }

            if ((activity == null) || activity instanceof OnDirectoryPickListener) {
                mDirectoryPickListener = (OnDirectoryPickListener) activity;
            }
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
        String selectedPath = (this.mCurrentSelection != null) ? this.mCurrentSelection.getAbsolute() : null;

        String statusMessage = (!isPickable(mCurrentSelection)) ? mContext.getString(R.string.selection_none_hint)  : getStatusErrorMessage(selectedPath);
        boolean canPressOk = (statusMessage == null);

        if (mCmdOk != null) mCmdOk.setEnabled(canPressOk);
        if (mCmdPopup != null) mCmdPopup.setEnabled(canPressOk);

        setStatusMessage(statusMessage);
    }

    /** decides if an item can be picked */
    protected boolean isPickable(IDirectory selection) {
        if (selection == null) return false;
        if (AlbumFile.isQueryFile (selection.getRelPath())) return true;
        int itemCount = getItemCount(selection);
        return (itemCount > 0);
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
            updateParentPathBar(getSelectedDir(currentSelection));
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
            updateBitmap(selectedChild.getSelectionIconID());
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
                ThumbNailUtils.getThumb(iconID, mImage);
            }
        }
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

    public IDirectory getRoot() {
        if (mNavigation != null) return mNavigation.getRoot();
        return null;
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
     */
    @Override
    public void navigateTo(String absolutePath) {
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, debugPrefix + " navigateTo : " + absolutePath);
        }

        if ((mNavigation != null) && (absolutePath != null)) {
            mCurrentSelection = getSelectedDir(absolutePath);
            mNavigation.navigateTo(mCurrentSelection);
        }

        // does nothing if OnCreate() has not been called yet

        reloadTreeViewIfAvailable();
    }

    protected IDirectory getSelectedDir(String absolutePath) {
        return mNavigation.getRoot().find(absolutePath);
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

    /** overwritten by dialog host to get selected photos for edit autoprocessing mode */
    public SelectedFiles getSrcFotos() {
        return null;
    }

    public interface OnDirectoryPickListener {
        /** called when user picks a new directory */
        void onDirectoryPick(String selectedAbsolutePath, int queryTypeId);
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
    public interface OnDirectoryInteractionListener extends OnDirectoryPickListener {
        /** called when user cancels picking of a new directory */
        void onDirectoryCancel(int queryTypeId);

        /** called after the selection in tree has changed */
        void onDirectorySelectionChanged(String selectedChild, int queryTypeId);

        /** remove cached directories */
        void invalidateDirectories(String why);
    }
}

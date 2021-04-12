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
 
package de.k3b.android.androFotoFinder.directory;

import android.animation.ValueAnimator;
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
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
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

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;

import de.k3b.LibGlobal;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.PhotoAutoprocessingEditActivity;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.ThumbNailUtils;
import de.k3b.android.androFotoFinder.backup.BackupActivity;
import de.k3b.android.androFotoFinder.imagedetail.ImageDetailMetaDialogBuilder;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.queries.FotoThumbSql;
import de.k3b.android.androFotoFinder.queries.FotoViewerParameter;
import de.k3b.android.androFotoFinder.tagDB.TagSql;
import de.k3b.android.io.AndroidFileCommands;
import de.k3b.android.util.ClipboardUtil;
import de.k3b.android.util.FileManagerUtil;
import de.k3b.android.util.IntentUtil;
import de.k3b.android.util.PhotoChangeNotifyer;
import de.k3b.android.util.PhotoPropertiesMediaFilesScanner;
import de.k3b.android.widget.Dialogs;
import de.k3b.database.QueryParameter;
import de.k3b.io.AlbumFile;
import de.k3b.io.Directory;
import de.k3b.io.DirectoryNavigator;
import de.k3b.io.FileUtils;
import de.k3b.io.GalleryFilterParameter;
import de.k3b.io.IDirectory;
import de.k3b.io.IGalleryFilter;
import de.k3b.io.ListUtils;
import de.k3b.io.OSDirectory;
import de.k3b.io.StringUtils;
import de.k3b.io.VISIBILITY;
import de.k3b.io.collections.SelectedFiles;

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
public class DirectoryPickerFragment extends DialogFragment
        implements DirectoryGui, ShowInMenuHandler.PickerContext {
    private static final String TAG = "DirFragment";
    private static final java.lang.String INSTANCE_STATE_CONTEXT_MENU = "contextmenuIds";

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

    // #127: Folderpicker: moving pathbar makes it discoverable. stop animation once the pathbar is touched
    private boolean showPathBarAnimation = Global.showPathBarAnimation;
    private ValueAnimator mPathbarAnimation = null;

    private View.OnClickListener mPathButtonClickHandler;
    private View.OnLongClickListener mPathButtonLongClickHandler = null;
    // local data
    protected Activity mContext;
    private DirectoryListAdapter mAdapter;
    protected DirectoryNavigator mNavigation;
    private int mDirTypId = 0;
    protected int mTitleId = 0;

    protected QueryParameter baseQuery = null;
    // api to fragment owner or null
    private OnDirectoryInteractionListener mDirectoryListener = null;
    private OnDirectoryPickListener mDirectoryPickListener = null;

    // for debugging
    private static int id = 1;
    private final String debugPrefix;
    private ImageView mImage;
    private int mLastIconID = -1;
    private int[] mContextMenue = null;

    // not null when renumber dialog is open.
    private AsyncTask mSubTask = null;
    private AlertDialog mSubDialog = null;
    private WeakReference<View> anchor;
    private ShowInMenuHandler showInMenuHandler = null;

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

    public void setBaseQuery(QueryParameter baseQuery) {
        this.baseQuery = baseQuery;
    }

    public DirectoryPickerFragment setContextMenuId(int... contextMenuId) {
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
            int[] ids = savedInstanceState.getIntArray(INSTANCE_STATE_CONTEXT_MENU);
            if ((ids != null) && (ids.length > 0)) {
                this.mContextMenue = ids;
            }
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

        if (this.mContextMenue != null) {
            mPathButtonLongClickHandler = new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showPathBarAnimation = false;
                    final IDirectory directory = (IDirectory) v.getTag();
                    String selectionPath = (directory == null) ? null : directory.getAbsolute();
                    onShowPopUp(v, v, selectionPath, directory, mContextMenue);
                    return true;
                }
            };
        }

        this.mParentPathBar = (LinearLayout) view.findViewById(R.id.parent_owner);
        this.mParentPathBarScroller = (HorizontalScrollView) view.findViewById(R.id.parent_scroller);
        this.mParentPathBarScroller.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                animationPathBarStop();
                // #127: stop animation once the pathbar is touched
                showPathBarAnimation = false;
                return false; // assume that the event is not handled yet so button can do it-s function
            }
        });

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

        if (mContextMenue != null) {
            mTreeView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int flatPosition, long id) {
                    long packedPos = mTreeView.getExpandableListPosition(flatPosition);
                    int group = ExpandableListView.getPackedPositionGroup(packedPos);
                    int child = ExpandableListView.getPackedPositionChild(packedPos);
                    IDirectory directory = (child != -1) ? mNavigation.getChild(group, child) : mNavigation.getGroup(group);
                    mTreeView.setSelectedChild(group, child, false);
                    String selectionPath = (directory == null) ? null : directory.getAbsolute();
                    onShowPopUp(view, mTreeView, selectionPath, directory, mContextMenue);
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
        if (mContextMenue != null) {
            mCmdPopup = (Button) view.findViewById(R.id.cmd_popup);
            if (mCmdPopup != null) {
                mCmdPopup.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String selectionPath = (mCurrentSelection == null) ? null : mCurrentSelection.getAbsolute();

                        onShowPopUp(mCmdPopup, mCmdPopup, selectionPath, mCurrentSelection, mContextMenue);
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

    // interface PickerContext
    /** called via pathBar-Button-LongClick, tree-item-LongClick, popUp-button */
    @Override
    public boolean onShowPopUp(View anchor, View owner, String selectionPath, Object selection, int... idContextMenue) {
        return onShowPopUp(anchor, owner, selectionPath, (IDirectory) selection, idContextMenue);
    }

    private boolean onShowPopUp(View anchor, View owner, String selectionPath, IDirectory selection, int... idContextMenue) {

        if (idContextMenue != null) {
            if (owner != null) {
                this.anchor = new WeakReference<View>(owner);
            }

            if ((anchor == null) && (this.anchor != null) && (this.anchor.get() != null)) {
                anchor = this.anchor.get();
            }
            if (mTreeView == anchor) {
                if (mTreeView.getSelectedView() != null) {
                    anchor = mTreeView.getSelectedView();
                }
            }

            PopupMenu popup = onCreatePopupMenu(anchor, selection, idContextMenue);

            if (popup != null) {
                popup.show();
            }
        }
        return true;
    }

    @NonNull
    private PopupMenu onCreatePopupMenu(View anchor, IDirectory selection, int... idContextMenue) {
        PopupMenu popup = new PopupMenu(getActivity(), anchor);
        popup.setOnMenuItemClickListener(popUpListener);
        MenuInflater inflater = popup.getMenuInflater();
        Menu menu = popup.getMenu();
        for (int id : idContextMenue) {
            if (id != 0) {
                inflater.inflate(id, menu);
            }
        }
        if (selection != null) {
            String absoluteSelectedPath = selection.getAbsolute();
            boolean isAlbumFile = AlbumFile.isQueryFile(absoluteSelectedPath);
            mPopUpSelection = selection;

            setMenuVisibility(menu, R.id.cmd_fix_link, FileUtils.isSymlinkDir(new File(absoluteSelectedPath), false));
            setMenuVisibility(menu, R.id.cmd_folder_hide_images, !isAlbumFile && PhotoPropertiesMediaFilesScanner.canHideFolderMedia(absoluteSelectedPath));

            setMenuVisibility(menu, R.id.cmd_apm_edit, !isAlbumFile && LibGlobal.apmEnabled);

            setMenuVisibility(menu, R.id.cmd_filemanager, !isAlbumFile && FileManagerUtil.hasShowInFilemanager(getActivity(), absoluteSelectedPath));
            setMenuVisibility(menu, R.id.cmd_delete, isAlbumFile);
            setMenuVisibility(menu, R.id.action_edit, isAlbumFile);

            setMenuVisibility(menu, android.R.id.copy, !isAlbumFile);

            IGalleryFilter currentSelectionAsFilter = getCurrentSelectionAsFilter(selection, this.mDirTypId);
            this.showInMenuHandler = new ShowInMenuHandler(mContext, this, baseQuery,
                    currentSelectionAsFilter, mDirTypId);
            if (selection != null) {
                showInMenuHandler.fixMenuOpenIn(selection.getAbsolute(), menu);
            }
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
        if ((showInMenuHandler != null) && (popUpSelection != null)
                && showInMenuHandler.onPopUpClick(menuItem, popUpSelection, popUpSelection.getAbsolute())) {
            return true;
        }
        switch (menuItem.getItemId()) {
            case R.id.cmd_mk_dir:
                return onCreateSubDirQuestion(popUpSelection);
            case R.id.cmd_apm_edit:
                return onEditApm(menuItem, popUpSelection);
            case android.R.id.copy:
                return onCopy(popUpSelection);

            case R.id.menu_item_rename:
                return onRenameDirQuestion(popUpSelection);
            case R.id.cmd_delete:
                return onDeleteDirQuestion(popUpSelection);

            case R.id.action_edit:
                return onEdit(popUpSelection);

            case R.id.cmd_backup:
                return onBackup(menuItem, popUpSelection);
            case R.id.cmd_filemanager:
                return FileManagerUtil.showInFilemanager(getActivity(), popUpSelection.getAbsolute());
            case R.id.action_details:
                return showDirInfo(baseQuery, popUpSelection);
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

    public void notifyPhotoChanged() {
        PhotoChangeNotifyer.notifyPhotoChanged(this.getActivity(), this.mAdapter);
    }

    private boolean onCopy(IDirectory selection) {
        String path = (selection == null) ? null : selection.getAbsolute();
        return ClipboardUtil.addDirToClipboard(this.getActivity(), path, true);
    }

    private boolean onEditApm(MenuItem menuItem, IDirectory selection) {
        String path = (selection == null) ? null : selection.getAbsolute();
        if (!StringUtils.isNullOrEmpty(path)) {
            PhotoAutoprocessingEditActivity.showActivity("[5]" + " menu " + menuItem.getTitle(), getActivity(),
                    null, path, getSrcFotos(), R.id.cmd_apm_edit, null);
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
                        PhotoPropertiesMediaFilesScanner.hideFolderMedia(mContext, path);
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

    private boolean onBackup(MenuItem menuItem, IDirectory dir) {
        if ((menuItem != null) && (dir != null)) {
            IGalleryFilter currentSelectionAsFilter = getCurrentSelectionAsFilter(dir, this.mDirTypId);
            String dbgContext = " menu " + menuItem.getTitle();
            BackupActivity.showActivity(dbgContext,
                    mContext, null, currentSelectionAsFilter,
                    baseQuery, BackupActivity.REQUEST_BACKUP_ID);
        }
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
        if (FotoSql.deleteMedia("delete album",
                ListUtils.toStringList(file.getAbsolutePath()),false) > 0) {
            deleteSuccess = true;
        }

        // delete from dir tree
        IDirectory parent = (dir != null) ? dir.getParent() :null;
        if (parent != null) {
            parent.removeChild(dir);
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
            notifyPhotoChanged();
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
                parentDir.removeChild(newChild);
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
                    SelectedFiles linkFiles = FotoSql.getSelectedfiles(sqlWhereLink, VISIBILITY.PRIVATE_PUBLIC);

                    String sqlWhereCanonical = FotoSql.SQL_COL_PATH + " in (" + linkFiles.toString() + ")";
                    sqlWhereCanonical = sqlWhereCanonical.replace(linkPath,canonicalPath);
                    SelectedFiles canonicalFiles = FotoSql.getSelectedfiles(sqlWhereCanonical, VISIBILITY.PRIVATE_PUBLIC);
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
                            FotoSql.getMediaDBApi().execUpdate("fixLinks", linkIds[i].intValue(), updateValues);
                        } else {
                            FotoSql.getMediaDBApi().deleteMedia("DirectoryPickerFragment.fixLinks", FotoSql.FILTER_COL_PK, new String[]{linkIds[i].toString()}, true);
                        }
                    }
                    PhotoPropertiesMediaFilesScanner.notifyChanges(context, "Fixed link/canonical duplicates");


                }
                return true;
            }
        }
        return false;
    }

    private boolean showDirInfo(QueryParameter baseQuery, IDirectory selectedDir) {
        String pathFilter = (selectedDir != null) ? selectedDir.getAbsolute() : null;
        String sql = (baseQuery != null) ? baseQuery.toSqlString() : null;
        CharSequence statistics = TagSql.getStatisticsMessage(this.getActivity(), R.string.show_photo, baseQuery);
        if (pathFilter != null) {


            final Dialog dlg = ImageDetailMetaDialogBuilder.createImageDetailDialog(
                    this.getActivity(),
                    getActivity().getTitle().toString(),
                    pathFilter,
                    sql,
                    statistics,
                    FotoThumbSql.formatDirStatistic(this.getActivity(), pathFilter)
            );
            dlg.show();
            // setAutoClose(null, dlg, null);
            return true;
        }
        return false;
    }

    private IGalleryFilter getCurrentSelectionAsFilter(IDirectory selection,
                                                       int dirTypId) {
        String selectionPath = selection.getAbsolute();
        GalleryFilterParameter filterParameter = new GalleryFilterParameter();
        FotoSql.set(filterParameter, selectionPath, dirTypId);
        return filterParameter;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mContextMenue != null) {
            outState.putIntArray(INSTANCE_STATE_CONTEXT_MENU, mContextMenue);
        }
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
    }

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
        showInMenuHandler = null;
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
        showInMenuHandler = null;
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

        showPathBarAnimation = false;
        animationPathBarStop();

        // naviationchange only if there are children below child
        IDirectory newGrandParent = ((selectedChild != null) && (Directory.getChildCount(selectedChild) > 0)) ? selectedChild.getParent() : null;

        int childPosition = (newGrandParent != null) ? newGrandParent.childIndexOf(selectedChild) : -1;
        if (childPosition >= 0) {
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

        if (showPathBarAnimation) {
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    // start animation after some delay
                    animationPathBarInit();
                }
            }, 800);
        }
    }

    // #127: Folderpicker: moving pathbar makes it discoverable. stop animation once the pathbar is touched
    private void animationPathBarInit() {
        animationPathBarStop();

        if (showPathBarAnimation) {
            int delta = mParentPathBar.getWidth() - mParentPathBarScroller.getWidth();
            if (delta > 0) {
                mPathbarAnimation = ValueAnimator.ofInt(delta, 0);
                mPathbarAnimation.setDuration(5000); // 5 from right to left seconds
                mPathbarAnimation.setRepeatCount(ValueAnimator.INFINITE);
                mPathbarAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator updatedAnimation) {
                        int x = (int) updatedAnimation.getAnimatedValue();
                        mParentPathBarScroller.smoothScrollTo(x, 0);
                    }
                });
                mPathbarAnimation.setRepeatMode(ValueAnimator.REVERSE);
                mPathbarAnimation.start();
            }
        }
    }

    // #127: Folderpicker: moving pathbar makes it discoverable. stop animation once the pathbar is touched
    private void animationPathBarStop() {
        if (mPathbarAnimation != null) {
            mPathbarAnimation.end();
            mPathbarAnimation.removeAllUpdateListeners();
            mPathbarAnimation = null;
        }
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

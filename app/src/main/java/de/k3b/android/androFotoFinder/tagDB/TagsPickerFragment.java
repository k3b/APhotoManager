/*
 * Copyright (c) 2017 by k3b.
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

package de.k3b.android.androFotoFinder.tagDB;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.k3b.android.androFotoFinder.FotoGalleryActivity;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.imagedetail.ImageDetailActivityViewPager;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.util.ResourceUtils;
import de.k3b.android.widget.Dialogs;
import de.k3b.database.QueryParameter;
import de.k3b.io.GalleryFilterParameter;
import de.k3b.io.IGalleryFilter;
import de.k3b.io.ListUtils;
import de.k3b.io.StringUtils;
import de.k3b.tagDB.Tag;
import de.k3b.tagDB.TagExpression;
import de.k3b.tagDB.TagRepository;

/**
 * A DialogFragment to select Tags to be added/removed.
 * Closes on screen rotation.
 * Needs additional Parameters that are currently not backed up by settings/preferences.
 *
 * Created by k3b on 04.01.2017.
 */

public class TagsPickerFragment  extends DialogFragment  {
    public interface ITagsSelector {
        void onSelect(CharSequence tag, List<String> addNames);
    }
    /** Owning Activity must implement this if it wants to handle the result of ok and cancel */
    public interface ITagsPicker {
        /** tag-dialog cancel pressed */
        boolean onCancel(String msg);

        /** tag-dialog ok pressed */
        boolean onOk(List<String> addNames,
                     List<String> removeNames);

        boolean onTagPopUpClick(int menuItemItemId, Tag selectedTag);
    };

    private ITagsPicker mFragmentOnwner = null;
    private boolean mIsFilterMode = true;
    private ImageView mFilterMode;
    private ImageView mBookmarkMode;
    private CharSequence mFilterValue = null;
    private int mFilterSelection = -1;
    private ITagsSelector mSelector = null;

    public static final int ACTIVITY_ID = 78921;

    private static final String TAG = "TagsPicker";
    private static final java.lang.String INSTANCE_STATE_CONTEXT_MENU = "contextmenu";

    private static final java.lang.String INSTANCE_STATE_FILTER = "filter";
    private static final java.lang.String INSTANCE_STATE_BOOKMARKS = "filter";
    private static final String PREFS_LAST_TAG_FILTER = "LAST_TAG_FILTER";
    private static final String PREFS_LAST_TAG_BOOKMARKS = "LAST_TAG_BOOKBARKS";

    private TagListArrayAdapter mDataAdapter;
    private EditText mFilterEdit;

    // not null when dialog is open that must be closed.
    private AlertDialog mSubDialog = null;


    private int mContextMenueId = R.menu.menu_tags_context;
    private int mTitleId = 0;

    // local data
    protected Activity mContext;
    private Tag mCurrentMenuSelection;
    private Tag mClipboardItem = null;
    private int mClipboardType = 0;

    private final List<String> mBookMarkNames = new ArrayList<String>();
    private List<String> mAddNames = null;
    private List<String> mRemoveNames = null;
    private List<String> mAffectedNames = TagListArrayAdapter.ALL_REMOVEABLE; // new ArrayList<>();

    // for debugging
    private static int id = 1;
    private final String debugPrefix;

    public TagsPickerFragment() {
        // Required empty public constructor
        debugPrefix = this.getClass().getSimpleName() + "#" + (id++)  + " ";
        Global.debugMemory(debugPrefix, "ctor");
        // Required empty public constructor
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, debugPrefix + "()");
        }
    }

    public TagsPickerFragment setTitleId(int titleId) {
        mTitleId = titleId;
        return this;
    }

    public TagsPickerFragment setContextMenuId(int contextMenuId) {
        mContextMenueId = contextMenuId;
        return this;
    }


    public TagsPickerFragment setAddNames(List<String> mAddNames) {
        this.mAddNames = mAddNames;
        return this;
    }

    public TagsPickerFragment setRemoveNames(List<String> mRemoveNames) {
        this.mRemoveNames = mRemoveNames;
        return this;
    }

    public TagsPickerFragment setAffectedNames(List<String> mAffectedNames) {
        this.mAffectedNames = mAffectedNames;
        return this;
    }

    public void setFragmentOnwner(ITagsPicker fragmentOnwner) {
        this.mFragmentOnwner = fragmentOnwner;
    }

    public void onStart() {
        super.onStart();
        if (mFragmentOnwner == null) {
            Log.d(Global.LOG_CONTEXT, debugPrefix + "onStart: mFragmentOnwner == null (after screen rotate). Closing dialog.");
            dismiss();
        }

    }

    /****** live cycle ********/
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (mFragmentOnwner == null) {
            super.onCreate(savedInstanceState);
            Log.d(Global.LOG_CONTEXT, debugPrefix + "onCreateView: mFragmentOnwner == null (after screen rotate). Closing dialog.");
            return null;
        }

        if (savedInstanceState != null) {
            this.mContextMenueId = savedInstanceState.getInt(INSTANCE_STATE_CONTEXT_MENU, this.mContextMenueId);
        }
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this.getActivity());

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_tags, container, false);

        mContext = this.getActivity();
        if (Global.debugEnabled && (mContext != null) && (mContext.getIntent() != null)){
            Log.d(Global.LOG_CONTEXT, "TagsPickerFragment onCreateView " + mContext.getIntent().toUri(Intent.URI_INTENT_SCHEME));
        }

        mBookMarkNames.clear();
        String lastBookMarkNames = prefs.getString(PREFS_LAST_TAG_BOOKMARKS, null);
        if (lastBookMarkNames != null) {
            mBookMarkNames.addAll(ListUtils.fromString(lastBookMarkNames));
        }

        List<Tag> existingTags = loadTagRepositoryItems(true);
        TagRepository tagRepository = TagRepository.getInstance();
        if (tagRepository.includeTagNamesIfNotFound(mAddNames, mRemoveNames, mAffectedNames, mBookMarkNames) > 0) {
            tagRepository.save();
        }
        this.mDataAdapter = new TagListArrayAdapter(this.getActivity(),
                existingTags,
                mAddNames, mRemoveNames, mAffectedNames, mBookMarkNames
        );

        final ListView list = (ListView)view.findViewById(R.id.list);
        list.setAdapter(mDataAdapter);
        if (mSelector != null) {
            list.setFocusable(true);
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                /** list item click. In tag pick mode: return filter-value and close */
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // mImageButtonLongClicked workaround imagebutton-long-click prevent list-itemclick.
                    if (!mDataAdapter.isImageButtonLongClicked()) {
                        Object item = mDataAdapter.getItem(position);
                        if ((item != null) && (mSelector != null)) {
                            saveSettings();
                            mSelector.onSelect(item.toString(), mAddNames);
                            dismiss();
                        }
                    }
                    mDataAdapter.setImageButtonLongClicked(false); // next listitem-click is allowed
                }
            });
        }

        if (mContextMenueId != 0)

        {
            list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    mCurrentMenuSelection = (position >= 0) ? mDataAdapter.getItem(position) : null;
                    Log.d(Global.LOG_CONTEXT,"tag-OnItemLongClick-" + ((mCurrentMenuSelection != null) ? mCurrentMenuSelection.getPath() : ""));
                    onShowPopUp(mContextMenueId, view, mCurrentMenuSelection);
                    return true;
                }
            });
        }
        this.mFilterMode = (ImageView) view.findViewById(R.id.cmd_find_mode);
        View.OnClickListener click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFilterMode(!mIsFilterMode);
            }
        };
        this.mFilterMode.setOnClickListener(click);
        this.mBookmarkMode = (ImageView) view.findViewById(R.id.cmd_bookmark_mode);
        this.mBookmarkMode.setOnClickListener(click);

        this.mFilterEdit = (EditText) view.findViewById(R.id.myFilter);
        this.mFilterEdit.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(final Editable s) {
            }

            @Override
            public void beforeTextChanged(final CharSequence s,
                                          final int start, final int count, final int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, final int start,
                                      final int before, final int count) {
                TagsPickerFragment.this.sendDelayed(
                        TagsPickerFragment.HANDLER_FILTER_TEXT_CHANGED,
                        TagsPickerFragment.HANDLER_FILTER_TEXT_DELAY);
            }

        });

        // load/save filter/bookmarks
        CharSequence lastTagFilter = (this.mFilterValue != null) ? this.mFilterValue : prefs.getString(PREFS_LAST_TAG_FILTER, null);
        if (lastTagFilter != null) {
            mFilterEdit.setText(lastTagFilter);
            if ((mFilterSelection >= 0) && (mFilterSelection <= mFilterValue.length())) {
                mFilterEdit.setSelection(mFilterSelection, mFilterSelection);
                ResourceUtils.setFocusWithKeyboard(mFilterEdit);
            }
            refershResultList();
        }

        if (getShowsDialog()) {
            onCreateViewDialog(view);
        }

        // does nothing if defineDirectoryNavigation() has not been called yet
        // reloadTreeViewIfAvailable();
        super.onCreate(savedInstanceState);

        return view;
        
    }

    private void setFilterMode(boolean newValue) {
        mIsFilterMode = newValue;
        this.mBookmarkMode.setImageDrawable(ResourceUtils.getDrawable(getActivity(),
                !mIsFilterMode ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off));

        this.mFilterMode.setImageDrawable(ResourceUtils.getDrawable(getActivity(),
                mIsFilterMode ? R.drawable.ic_btn_search_blue : R.drawable.ic_btn_search));

        // drawable.setColorFilter(0x00ff0000, PorterDuff.Mode.ADD);
        this.mFilterEdit.setVisibility(mIsFilterMode ? View.VISIBLE : View.INVISIBLE);

        refershResultList();
    }

    private void saveSettings() {
        if (mFragmentOnwner != null) {
            // not recreate after rotation
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
            SharedPreferences.Editor edit = sharedPref.edit();

            if (mFilterValue == null) {
                edit.putString(PREFS_LAST_TAG_FILTER, mFilterEdit.getText().toString());
            }

            edit.putString(PREFS_LAST_TAG_BOOKMARKS, ListUtils.toString(mBookMarkNames));
            edit.apply();
        }
    }

    @Override
    public void onDetach() {
        saveSettings();
        super.onDetach();
    }

    @Override public void onDestroy() {
        if (mSubDialog != null) mSubDialog.dismiss();
        mSubDialog = null;
        super.onDestroy();
    }


    /** handle init for dialog-only controlls: cmdOk, cmdCancel, status */
    private void onCreateViewDialog(View view) {
        ImageButton cmdOk = (ImageButton) view.findViewById(R.id.cmd_ok);
        cmdOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOk(mAddNames, mRemoveNames);
            }
        });

        ImageButton cmdCancel = (ImageButton) view.findViewById(R.id.cmd_cancel);
        if (cmdCancel != null) {
            cmdCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onCancel(debugPrefix + "onCancel: " + mCurrentMenuSelection);
                }
            });
        }

        this.getDialog().setTitle(mTitleId);
    }

    public boolean onCancel(String msg) {
        Log.d(Global.LOG_CONTEXT, debugPrefix + msg);

        if ((mFragmentOnwner != null) && (!mFragmentOnwner.onCancel(msg))) return false;

        saveSettings();
        dismiss();
        return true;
    }

    /** ok button clicked to transfer changes and close dlg */
    public boolean onOk(List<String> addNames,
                        List<String> removeNames) {
        Log.d(Global.LOG_CONTEXT, debugPrefix + "onOk: " + mCurrentMenuSelection);

        if (mSelector != null) {
            String newTagName = mFilterEdit.getText().toString();
            Tag existingTag = TagRepository.getInstance().findFirstByName(newTagName);
            if (existingTag != null) {
                mSelector.onSelect(existingTag.getName(), mAddNames);
            } else {
                mSelector.onSelect(newTagName, mAddNames);
            }
        } else {
            if ((mFragmentOnwner != null) && (!mFragmentOnwner.onOk(addNames, removeNames))) return false;
        }

        saveSettings();
        dismiss();

        return true;
    }

    /** called via pathBar-Button-LongClick, tree-item-LongClick, popUp-button */
    private void onShowPopUp(int contextMenueId, View anchor, Tag selection) {
        PopupMenu popup = onCreatePopupMenu(contextMenueId, anchor, selection);

        // without mClipboardItem paste is not possible
        if (mClipboardItem == null) {
            MenuItem menuItem = popup.getMenu().findItem(android.R.id.paste);
            if (menuItem != null) menuItem.setVisible(false);
        }

        if (popup != null) {
            popup.show();
        }
    }


    @NonNull
    private PopupMenu onCreatePopupMenu(int contextMenueId, View anchor, Tag selection) {
        PopupMenu popup = new PopupMenu(getActivity(), anchor);
        popup.setOnMenuItemClickListener(popUpListener);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(contextMenueId, popup.getMenu());
        if (selection != null) {
            mCurrentMenuSelection = selection;
        }
        return popup;
    }

    private final PopupMenu.OnMenuItemClickListener popUpListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            return onPopUpClick(menuItem);
        }
    };

    private boolean onPopUpClick(MenuItem menuItem) {
        if (mFragmentOnwner.onTagPopUpClick(menuItem.getItemId(), mCurrentMenuSelection)) return true;
        switch (menuItem.getItemId()) {
                    /*!!!!!
                    mCurrentMenuSelection = mDataAdapter.getItem(position);
                    */
            /*
            case R.id.cmd_photo:
                return showPhoto(mCurrentMenuSelection);
            case R.id.cmd_gallery:
                */
            case android.R.id.cut:
            case android.R.id.copy:
                mClipboardItem = mCurrentMenuSelection;
                mClipboardType = menuItem.getItemId();
                break;
            case android.R.id.paste:
                return onPaste(mCurrentMenuSelection);
            case R.id.cmd_tags_add:
                return showTagAddDialog(mCurrentMenuSelection);
            case R.id.menu_item_rename:
                return showTagRenameDialog(mCurrentMenuSelection);
            case R.id.cmd_delete:
                return showTagDeleteDialog(mCurrentMenuSelection);
/*
    implemented by owner
            case R.id.cmd_photo:
            case R.id.cmd_gallery:
                return showGallery(mCurrentMenuSelection);
            case R.id.cmd_show_geo:
*/
            default:break;
        }
        return false;
    }


    public static boolean handleMenuShow(int menuItemItemId, Tag selectedTag, Activity context, IGalleryFilter parentFilter) {
        switch (menuItemItemId) {
            case R.id.cmd_photo:
                ImageDetailActivityViewPager.showActivity(context, null, 0, createSubQueryByTag(parentFilter, selectedTag), 0);
                return true;
            case R.id.cmd_gallery:
                FotoGalleryActivity.showActivity(context, createSubFilterByTag(parentFilter, selectedTag), null, 0);
                return true;
            default:break;
        }
        return false;
    }

    @NonNull
    private static QueryParameter createSubQueryByTag(IGalleryFilter parentFilter, Tag selectedTag) {
        GalleryFilterParameter filter = createSubFilterByTag(parentFilter, selectedTag);
        QueryParameter query = new QueryParameter();
        TagSql.filter2QueryEx(query, filter, false);
        FotoSql.setSort(query, FotoSql.SORT_BY_DATE, false);
        return query;
    }

    @NonNull
    private static GalleryFilterParameter createSubFilterByTag(IGalleryFilter parentFilter, Tag selectedTag) {
        GalleryFilterParameter filter = new GalleryFilterParameter().get(parentFilter);
        filter.setTagsAllIncluded(ListUtils.toStringList(selectedTag));
        return filter;
    }

    public boolean showTagDeleteDialog(final Tag item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.delete_menu_title);
        View content = getActivity().getLayoutInflater().inflate(R.layout.dialog_tag_delete, null);

        ((TextView) content.findViewById(R.id.lblTag)).setText(item.getName());

        final CheckBox chkUpdateAffectedPhotos = (CheckBox) content.findViewById(R.id.chkUpdateAffectedPhotos);

        final CheckBox chkDeleteChildren = (CheckBox) content.findViewById(R.id.chkDeleteChildren);
        TextView childTags = (TextView) content.findViewById(R.id.lblTagChildren);

        List<Tag> rootList = new ArrayList<Tag>();
        rootList.add(item);
        final int rootTagReferenceCount = TagSql.getTagRefCount(getActivity(), rootList);

        List<Tag> children = item.getChildren(loadTagRepositoryItems(false), true, false);

        if (children != null) rootList.addAll(children);
        final int allTagReferenceCount = (children == null)
                ? rootTagReferenceCount
                : TagSql.getTagRefCount(getActivity(), rootList);

        if (children == null) {
            chkDeleteChildren.setVisibility(View.GONE);
            childTags.setVisibility(View.GONE);
        } else {
            childTags.setText(ListUtils.toString(children));
            chkDeleteChildren.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    chkUpdateAffectedPhotos.setText(getString(R.string.tags_update_photos) + " (" +
                            ((chkDeleteChildren.isChecked())
                                    ? allTagReferenceCount
                                    : rootTagReferenceCount )  + ")");
                }
            });
        }

        chkUpdateAffectedPhotos.setText(getString(R.string.tags_update_photos) + " (" +
                rootTagReferenceCount + ")");
        builder.setView(content);
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            //@Override
            public void onClick(DialogInterface dialog, int which) {
                tagDelete(item, chkDeleteChildren.isChecked(), chkUpdateAffectedPhotos.isChecked());
                dialog.dismiss();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        return true;
    }

    private List<Tag> loadTagRepositoryItems(boolean reload) {
        List<Tag> result = reload ? TagRepository.getInstance().reload() : TagRepository.getInstance().load();
        if (result.size() == 0) {
            TagRepository.includePaths(result,null,null,getString(R.string.tags_defaults));
        }
        return result;
    }

    private class TagDeleteWithDbUpdateTask extends TagTask<List<Tag>> {
        TagDeleteWithDbUpdateTask() {
            super(getActivity(), R.string.delete_menu_title);
        }

        @Override
        protected Integer doInBackground(List<Tag>... params) {
            publishProgress("...");
            getWorkflow().init(getActivity(), null, params[0]);
            return getWorkflow().updateTags(null, ListUtils.toStringList(params[0]));
        }
    }


    private boolean tagDelete(Tag item, boolean recursive, boolean deleteFromPhotos) {
        if (item != null) {
            List<Tag> existingItems = loadTagRepositoryItems(false);
            List<Tag> children = (recursive) ? item.getChildren(existingItems, true, false) : null;

            if (deleteFromPhotos) {
                List<Tag> affectedTags = new ArrayList<Tag>();
                affectedTags.add(item);
                if (children != null) affectedTags.addAll(children);

                new TagDeleteWithDbUpdateTask().execute(affectedTags);
            }

            if ((existingItems != null) && (item.delete(existingItems, recursive) > 0)) {
                updateKnownLists(null, item.getName(), true);
                mDataAdapter.remove(item);
                if (children != null) {
                    for (Tag child : children) {
                        mDataAdapter.remove(child);
                    }
                }
                TagRepository.getInstance().save();
                mDataAdapter.reloadList();
            }

            return true;
        }
        return false;
    }

    private boolean showTagRenameDialog(final Tag tag) {
        Dialogs dialog = new Dialogs() {
            CheckBox chkUpdatePhotos;

            @Override
            protected View onCreateContentView(Activity parent) {
                View result = parent.getLayoutInflater().inflate(R.layout.dialog_tag_rename, null);
                chkUpdatePhotos = (CheckBox) result.findViewById(R.id.chkUpdatePhotos);

                List<Tag> rootList = new ArrayList<Tag>();
                rootList.add(tag);
                final int rootTagReferenceCount = TagSql.getTagRefCount(getActivity(), rootList);

                chkUpdatePhotos.setText(getString(R.string.tags_update_photos) + " (" +
                        rootTagReferenceCount + ")");

                return result;
            }

            @Override
            protected void onDialogResult(String newFileName, Object... parameters) {
                if (newFileName != null) {
                    tagRename(tag, newFileName, chkUpdatePhotos.isChecked(), false);
                }
                mSubDialog=null;

            }
        };
        mSubDialog=dialog.editFileName(getActivity(), getString(R.string.rename_menu_title), tag.getName(), tag);

        return true;
    }

    private boolean showTagAddDialog(final Tag tagParent) {
        String defaultName = getString(R.string.tags_add_default);
        Dialogs dialog = new Dialogs() {
            @Override
            protected void onDialogResult(String newFileName, Object... parameters) {
                if (newFileName != null) {
                    tagAdd(tagParent, newFileName);
                }
                mSubDialog=null;

            }
        };
        mSubDialog=dialog.editFileName(getActivity(), getString(R.string.tags_add_menu_title), defaultName, tagParent);

        return true;
    }

    private class TagRenameWithDbUpdateTask extends TagTask<Object> {
        private final Tag mOldTag;
        private final String mNewName;

        TagRenameWithDbUpdateTask(String oldName, String newName) {
            super(getActivity(), R.string.rename_menu_title);
            this.mOldTag = new Tag().setName(oldName);
            this.mNewName = newName;
        }

        @Override
        protected Integer doInBackground(Object... params) {
            List<Tag> removedTags = new ArrayList<Tag>();
            removedTags.add(mOldTag);
            publishProgress("...");
            getWorkflow().init(getActivity(), null, removedTags);

            TagRepository.getInstance().renameTags(mOldTag.getName(), mNewName);

            return getWorkflow().updateTags(ListUtils.toStringList(mNewName), ListUtils.toStringList(removedTags));
        }

        @Override
        protected void onPostExecute(Integer result) {
            tagRename(mOldTag, mNewName, false, true);
            super.onPostExecute(result);
        }
    }

    private void tagRename(Tag oldTag, String newPath, boolean updateDatabase, boolean updateAffected) {
        String[] pathElements = TagExpression.getPathElemensFromLastExpr(newPath);

        if (pathElements != null) {
            boolean mustReload = false;
            String oldName = oldTag.getName();
            String newName = pathElements[pathElements.length - 1];

            if ((!StringUtils.isNullOrEmpty(newName)) && (newName.compareTo(oldName) != 0)) {
                if (updateDatabase) {
                    new TagRenameWithDbUpdateTask(oldName, newName).execute();
                    return;
                }
                updateKnownLists(newName, oldName, updateAffected);
                mDataAdapter.remove(oldTag);
                // mDataAdapter.add();
                mustReload = true;
            }

            if (pathElements.length > 1) {
                // move to different path
                mDataAdapter.remove(oldTag);
                if (tagAdd(oldTag.getParent(), newPath) > 0) {
                    mustReload = true;
                }
            }

            if (mustReload) {
                mDataAdapter.reloadList();
            }
        }
    }

    private void updateKnownLists(String newName, String oldName, boolean updateAffected) {
        int modified = updateLists(oldName, newName, mBookMarkNames, mAddNames);
        if (updateAffected) modified += updateLists(oldName, newName, mRemoveNames, mAffectedNames);
        if (modified > 0) {
            saveSettings();
        }
        TagRepository.getInstance().save();
    }

    private int updateLists(String oldName, String newName, List<String>... lists) {
        int changes = 0;
        for (List<String> list :lists) {
            if ((list != null) && list.remove(oldName)) {
                changes ++;
                if (newName != null) {
                    list.add(newName);
                }
            }
        }
        return changes;
    }

    public void setFilter(CharSequence filterValue, int selection) {
        this.mFilterValue = filterValue;
        this.mFilterSelection = selection;
    }

    public void setTagSelector(ITagsSelector selector) {
        mSelector = selector;
    }

    private int tagAdd(Tag parent, String itemExpression) {
        List<Tag> existingItems = loadTagRepositoryItems(false);
        int changeCount = TagRepository.includePaths(existingItems, parent, null, itemExpression);

        if (changeCount > 0) {

            int len = existingItems.size();

            // assume that new tags are appended.
            // iteraterate over all appended tags
            for (int i = len - changeCount; i < len; i++) {
                Tag t = existingItems.get(i);
                // existingItems.add(t);
                mDataAdapter.add(t);

                // new added tags will be automatically bookmarked
                if ((mBookMarkNames != null) && !mBookMarkNames.contains(t.getName())) {
                    mBookMarkNames.add(t.getName());
                }
            }
            mDataAdapter.notifyDataSetInvalidated();
            mDataAdapter.notifyDataSetChanged();
            mDataAdapter.reloadList();
            TagRepository.getInstance().save();
            mDataAdapter.notifyDataSetChanged();
        }
        return changeCount;
    }

    private void tagChange(Tag tag, Tag parent) {
        List<Tag> existingItems = loadTagRepositoryItems(false);
        tag.setParent(parent);
        if (!existingItems.contains(tag)) {
            existingItems.add(tag);
            mDataAdapter.add(tag);
            mDataAdapter.reloadList();
        }
        TagRepository.getInstance().save();
        mDataAdapter.notifyDataSetChanged();
    }

    private boolean onPaste(Tag currentMenuSelection) {
        if (mClipboardItem == null) return false;

        if (mClipboardType == android.R.id.cut) {
            tagChange(mClipboardItem, currentMenuSelection);
            mClipboardItem = null;
            return true;
        }
        if (mClipboardType == android.R.id.copy) {
            tagChange(new Tag().setName(mClipboardItem.getName()), currentMenuSelection);
            mClipboardItem = null;
            return true;
        }

        return false;
    }

	/*----------------------------
	 * Delayed Processing: <br/>
	 * textchange->HANDLER_FILTER_TEXT_CHANGED(reload list)->HANDLER_FILTER_COUNT_UPDATE(update itemcount)
	 -----------------------------*/

    // char(s) typing in filter is active
    private static final int HANDLER_FILTER_TEXT_CHANGED = 0;
    private static final int HANDLER_FILTER_TEXT_DELAY = 1000;

    // list is reloading
    private static final int HANDLER_FILTER_COUNT_UPDATE = 1;
    private static final int HANDLER_FILTER_COUNT_DELAY = 500;

    private final Handler delayProcessor = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
        TagsPickerFragment.this.clearDelayProcessor();
        switch (msg.what) {
            case TagsPickerFragment.HANDLER_FILTER_TEXT_CHANGED:
                TagsPickerFragment.this.refershResultList();
                TagsPickerFragment.this.sendDelayed(
                        TagsPickerFragment.HANDLER_FILTER_COUNT_UPDATE,
                        TagsPickerFragment.HANDLER_FILTER_COUNT_DELAY);
                break;
            case TagsPickerFragment.HANDLER_FILTER_COUNT_UPDATE:
                TagsPickerFragment.this.refreshCounter();
                break;
            default:
                // not implemented
                throw new IllegalStateException();
        }
        }

    };

    private void clearDelayProcessor() {
        this.delayProcessor
                .removeMessages(TagsPickerFragment.HANDLER_FILTER_TEXT_CHANGED);
        this.delayProcessor
                .removeMessages(TagsPickerFragment.HANDLER_FILTER_COUNT_UPDATE);
    }

    private void sendDelayed(final int messageID, final int delayInMilliSec) {
        this.clearDelayProcessor();

        final Message msg = Message
                .obtain(this.delayProcessor, messageID, null);
        TagsPickerFragment.this.delayProcessor.sendMessageDelayed(msg,
                delayInMilliSec);
    }
    
    private void refershResultList() {
        final String filter = (mIsFilterMode) ? TagsPickerFragment.this.mFilterEdit.getText()
                .toString() : "@@@@";
        mDataAdapter.setFilterParam(filter);
    }

    private void refreshCounter() {
        TagsPickerFragment.this.mDataAdapter.getCount();
    }
}

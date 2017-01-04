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
import android.app.DialogFragment;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.io.ListUtils;
import de.k3b.tagDB.Tag;
import de.k3b.tagDB.TagRepository;

/**
 * Created by k3b on 04.01.2017.
 */

public class TagsPickerFragment  extends DialogFragment  {

    private ITagsPicker mFragmentOnwner = null;

    /** Owning Activity must implement this if it wants to handle the result of ok and cancel */
    public interface ITagsPicker {
        boolean onCancel(String msg);

        boolean onOk(List<String> addNames,
                     List<String> removeNames);
    };

    public static final int ACTIVITY_ID = 78921;

    private static final String TAG = "TagsPicker";
    private static final java.lang.String INSTANCE_STATE_CONTEXT_MENU = "contextmenu";

    private static final java.lang.String INSTANCE_STATE_FILTER = "filter";
    private static final java.lang.String INSTANCE_STATE_BOOKMARKS = "filter";
    private static final String PREFS_LAST_TAG_FILTER = "LAST_TAG_FILTER";
    private static final String PREFS_LAST_TAG_BOOKMARKS = "LAST_TAG_BOOKBARKS";

    private TagListArrayAdapter mDataAdapter;
    private EditText mFilterEdit;
    private ListView mList;
    private ImageView mImage;

    private TextView mStatus = null;
    private Button mCmdPopup = null;
    
    private int mContextMenue = 0;
    private int mTitleId = 0;

    // local data
    protected Activity mContext;
    private Tag mCurrentSelection;
    private List<Tag> mSelectedItems = new ArrayList<Tag>();
    private final List<String> mBookMarkNames = new ArrayList<String>();
    private List<String> mAddNames = null;
    private List<String> mRemoveNames = null;
    private List<String> mAffectedNames = TagListArrayAdapter.ALL_REMOVEABLE; // new ArrayList<>();

    // for debugging
    private static int id = 1;
    private final String debugPrefix;

    View.OnLongClickListener onLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            onShowPopUp(mCmdPopup, mCurrentSelection);
            return false;
        }
    };

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
        mContextMenue = contextMenuId;
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
            this.mContextMenue = savedInstanceState.getInt(INSTANCE_STATE_CONTEXT_MENU, this.mContextMenue);
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

        this.mDataAdapter = new TagListArrayAdapter(this.getActivity(),
                TagRepository.getInstance().load(),
                mAddNames, mRemoveNames, mAffectedNames, mBookMarkNames,
                onLongClickListener);

        mList = (ListView)view.findViewById(R.id.list);
        mList.setAdapter(mDataAdapter);

        if (mContextMenue != 0) {
            mList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    return false;
                }
            });
        }

        this.mImage = (ImageView) view.findViewById(R.id.image);

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
        String lastTagFilter = prefs.getString(PREFS_LAST_TAG_FILTER, null);
        if (lastTagFilter != null) {
            mFilterEdit.setText(lastTagFilter);
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

    private void saveSettings() {
        if (mFragmentOnwner != null) {
            // not recreate after rotation
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
            SharedPreferences.Editor edit = sharedPref.edit();

            edit.putString(PREFS_LAST_TAG_FILTER, mFilterEdit.getText().toString());
            edit.putString(PREFS_LAST_TAG_BOOKMARKS, ListUtils.toString(mBookMarkNames));
            edit.apply();
        }
    }

    @Override
    public void onDetach() {
        saveSettings();
        super.onDetach();
    }


    /** handle init for dialog-only controlls: cmdOk, cmdCancel, status */
    private void onCreateViewDialog(View view) {
        this.mStatus = (TextView) view.findViewById(R.id.status);
        this.mStatus.setVisibility(View.VISIBLE);

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
                    onCancel(debugPrefix + "onCancel: " + mCurrentSelection);
                }
            });
        }

        mCmdPopup = null;
        if (mContextMenue != 0) {
            mCmdPopup = (Button) view.findViewById(R.id.cmd_popup);
            if (mCmdPopup != null) {
                mCmdPopup.setOnLongClickListener(onLongClickListener);
                mCmdPopup.setVisibility(View.VISIBLE);
            }
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

    public boolean onOk(List<String> addNames,
                        List<String> removeNames) {
        Log.d(Global.LOG_CONTEXT, debugPrefix + "onOk: " + mCurrentSelection);

        if ((mFragmentOnwner != null) && (!mFragmentOnwner.onOk(addNames, removeNames))) return false;

        saveSettings();
        dismiss();

        return true;
    }

    /** called via pathBar-Button-LongClick, tree-item-LongClick, popUp-button */
    private void onShowPopUp(View anchor, Tag selection) {
        PopupMenu popup = onCreatePopupMenu(anchor, selection);

        if (popup != null) {
            popup.show();
        }
    }


    @NonNull
    private PopupMenu onCreatePopupMenu(View anchor, Tag selection) {
        PopupMenu popup = new PopupMenu(getActivity(), anchor);
        popup.setOnMenuItemClickListener(popUpListener);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(this.mContextMenue, popup.getMenu());
        if (selection != null) {
            mCurrentSelection = selection;
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
        switch (menuItem.getItemId()) {
            /*
            case R.id.cmd_mk_dir:
                return onCreateSubDirQuestion(mCurrentSelection);
            case R.id.cmd_photo:
                return showPhoto(mCurrentSelection);
            case R.id.cmd_gallery:
                return showGallery(mCurrentSelection);
            case R.id.action_details:
                return showDirInfo(mCurrentSelection);
            case R.id.cmd_fix_link:
                return fixLinks(mCurrentSelection);
            case R.id.cmd_folder_hide_images:
                onHideFolderMediaQuestion(mCurrentSelection.getAbsolute());
                return true;
                */
            default:break;
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
        final String filter = TagsPickerFragment.this.mFilterEdit.getText()
                .toString();
        mDataAdapter.setFilterParam(filter);
        boolean empty = TagsPickerFragment.this.mDataAdapter.getCount() == 0;
        mList.setVisibility(empty ? View.INVISIBLE : View.VISIBLE);
    }

    private void refreshCounter() {
        final int itemCcount = TagsPickerFragment.this.mDataAdapter.getCount();
        // TagsPickerFragment.this.setCount(itemCcount);
    }
}

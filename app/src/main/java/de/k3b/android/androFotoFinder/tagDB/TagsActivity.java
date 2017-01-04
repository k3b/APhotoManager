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
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ListView;

import de.k3b.android.androFotoFinder.R;
import de.k3b.tagDB.TagRepository;

/** listview-activity for tags with incremental search.
 *  See also https://github.com/codepath/android_guides/wiki/Using-an-ArrayAdapter-with-ListView  */
public class TagsActivity extends ListActivity {
    public static final int ACTIVITY_ID = 78921;

    private TagListArrayAdapter mDataAdapter;
    private EditText mFilterEdit;

    public static void showActivity(Activity context) {
        Intent intent;
        //Create intent
        intent = new Intent(context, TagsActivity.class);

        /*
        if (imageDetailQuery != null) {
            intent.putExtra(TagsActivity.EXTRA_QUERY, imageDetailQuery.toReParseableString());
        }
        intent.putExtra(TagsActivity.EXTRA_POSITION, position);
        intent.setData(imageUri);
        */

        context.startActivityForResult(intent, ACTIVITY_ID);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tags);

        this.mDataAdapter = new TagListArrayAdapter(this, TagRepository.getInstance().load());

        final ListView listView = this.getListView();
        // Assign adapter to ListView
        listView.setAdapter(this.mDataAdapter);

        this.mFilterEdit = (EditText) this.findViewById(R.id.myFilter);
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
                TagsActivity.this.sendDelayed(
                        TagsActivity.HANDLER_FILTER_TEXT_CHANGED,
                        TagsActivity.HANDLER_FILTER_TEXT_DELAY);
            }

        });

    }

    private void refershResultList() {
        final String filter = TagsActivity.this.mFilterEdit.getText()
                .toString();
        TagsActivity.this.mDataAdapter.getFilter().filter(filter);
    }

    private void refreshCounter() {
        final int itemCcount = TagsActivity.this.mDataAdapter.getCount();
        TagsActivity.this.setCount(itemCcount);
    }

    void setCount(final int count) {
        /*
        final String title = this.getString(R.string.app_name) + ": " + count
                + this.getString(R.string.message_found);
        this.setTitle(title);
        */
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
            TagsActivity.this.clearDelayProcessor();
            switch (msg.what) {
                case TagsActivity.HANDLER_FILTER_TEXT_CHANGED:
                    TagsActivity.this.refershResultList();
                    TagsActivity.this.sendDelayed(
                            TagsActivity.HANDLER_FILTER_COUNT_UPDATE,
                            TagsActivity.HANDLER_FILTER_COUNT_DELAY);
                    break;
                case TagsActivity.HANDLER_FILTER_COUNT_UPDATE:
                    TagsActivity.this.refreshCounter();
                    break;
            }
        }

    };

    private void clearDelayProcessor() {
        this.delayProcessor
                .removeMessages(TagsActivity.HANDLER_FILTER_TEXT_CHANGED);
        this.delayProcessor
                .removeMessages(TagsActivity.HANDLER_FILTER_COUNT_UPDATE);
    }

    private void sendDelayed(final int messageID, final int delayInMilliSec) {
        this.clearDelayProcessor();

        final Message msg = Message
                .obtain(this.delayProcessor, messageID, null);
        TagsActivity.this.delayProcessor.sendMessageDelayed(msg,
                delayInMilliSec);
    }

}

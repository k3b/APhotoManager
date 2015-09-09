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

package de.k3b.android.androFotoFinder.testgui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import de.k3b.android.widget.HistoryEditText;

public class TestGuiActivity extends Activity {
    private static final int ACTION_ID = 4711;
    private String appName;
    private EditText editMime;
    private EditText editUri;
    private EditText editTitle;
    private EditText editFilter;
    private CheckBox chk_EXTRA_ALLOW_MULTIPLE;
    private CheckBox chk_CATEGORY_OPENABLE;
    private CheckBox chk_EXTRA_LOCAL_ONLY;
    private HistoryEditText mHistory;

    /** Greate the gui to enter the parameters for testing intent-api. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_gui);
        appName = getString(R.string.app_name) + ":";

        Button rundDemoView = (Button) findViewById(R.id.run_view);
        Button rundDemoPick = (Button) findViewById(R.id.run_demo_pick);
        Button rundDemoGetContent = (Button) findViewById(R.id.run_demo_get_content);
        editMime = (EditText) findViewById(R.id.edit_mime);
        editUri = (EditText) findViewById(R.id.edit_Uri);
        editTitle = (EditText) findViewById(R.id.edit_title);
        editFilter = (EditText) findViewById(R.id.edit_filter);

        chk_CATEGORY_OPENABLE = (CheckBox) findViewById(R.id.chk_CATEGORY_OPENABLE);
        chk_EXTRA_ALLOW_MULTIPLE = (CheckBox) findViewById(R.id.chk_EXTRA_ALLOW_MULTIPLE);
        chk_EXTRA_LOCAL_ONLY = (CheckBox) findViewById(R.id.chk_EXTRA_LOCAL_ONLY);

        rundDemoPick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDemo(Intent.ACTION_PICK);
            }
        });
        rundDemoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDemo(Intent.ACTION_VIEW);
            }
        });
        rundDemoGetContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {startDemo(Intent.ACTION_GET_CONTENT);
            }
        });

        mHistory = new HistoryEditText(this, new int[] {R.id.cmd_mime, R.id.cmd_filter, R.id.cmd_title, R.id.cmd_uri} , editMime, editFilter, editTitle, editUri);
    }

    /** Gui dependant code */
    protected void startDemo(String action) {
        String uriString = editUri.getText().toString(); // "geo:53.2,8.8?q=(name)&z=1";
        String mimeString = editMime.getText().toString(); // null or */*
        if ((mimeString != null) && (mimeString.length() == 0)) mimeString = null;
        String title = editTitle.getText().toString(); // Example "where did you take the photo"
        String filter = editFilter.getText().toString().trim();

        mHistory.saveHistory();

        startDemo(uriString, mimeString, action, title, filter,
                chk_CATEGORY_OPENABLE.isChecked(), chk_EXTRA_ALLOW_MULTIPLE.isChecked(), chk_EXTRA_LOCAL_ONLY.isChecked());
    }

    /**
     * Gui independant code: Shows open an app using the intent-api.
     * @param uriString i.e. "geo:54.0,8.0?q=(Hello)"
     * @param mimeString i.e. null
     * @param action i.e. "android.intent.action.VIEW" or "android.intent.action.PICK"
     * @param title i.e. "where did you take the photo" or null
     */
    private void startDemo(String uriString, String mimeString, String action, String title,
                           String filter, boolean openable, boolean multiselect, boolean localOnly) {
        Uri uri = Uri.parse(uriString);
        Intent intent = new Intent();
        if (action != null) {
            intent.setAction(action);
        }

        if ((title != null) && (title.length() > 0)) {
            intent.putExtra(Intent.EXTRA_TITLE, title);
        }

        if ((filter != null) && (filter.length() > 0)) {
            intent.putExtra("de.k3b.extra.FILTER", filter);
        }

        if (openable) intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (multiselect) intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        if (localOnly) intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);

        intent.setDataAndType(uri, mimeString);
        Toast.makeText(this, appName + "Starting " + uriString + "-" + intent.getType(), Toast.LENGTH_SHORT).show();
        try {
            startActivityForResult(Intent.createChooser(intent,"Choose app to test"), ACTION_ID);
        } catch (Exception e) {
            Toast.makeText(this, appName + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /** Optional: Process the result location of the geo-picker  */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String resultIntent = getUri(data);
        super.onActivityResult(requestCode, resultCode, data);
        final String result = "got result " + resultIntent;
        Toast.makeText(this, appName + result, Toast.LENGTH_LONG).show();
        TextView lastResult = (TextView) findViewById(R.id.label_last_result);
        lastResult.setText(result);
    }

    private static String getUri(Intent src) {
        final Uri uri = (src != null) ? src.getData() : null;
        if (uri != null) return uri.toString();

        String intentUri = (src != null) ? src.toUri(Intent.URI_INTENT_SCHEME) : null;
        return intentUri;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_test_gui, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

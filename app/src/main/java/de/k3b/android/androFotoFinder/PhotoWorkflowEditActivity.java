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
 
package de.k3b.android.androFotoFinder;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import de.k3b.FotoLibGlobal;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.util.IntentUtil;
import de.k3b.android.util.MediaScanner;
import de.k3b.android.widget.AboutDialogPreference;
import de.k3b.android.widget.ActivityWithAutoCloseDialogs;
import de.k3b.android.widget.HistoryEditText;
import de.k3b.database.SelectedFiles;
import de.k3b.io.FileUtils;
import de.k3b.io.PhotoWorkFlowDto;
import de.k3b.media.IMetaApi;
import de.k3b.media.MediaAsString;
import de.k3b.media.MediaDiffCopy;

/**
 * Editor for a WorkflowItem ".apm" files that define
 * rules to process one or more photos
 * * filename schemata for copied/moved files
 * * default properties that every photot should receive.
 */
public class PhotoWorkflowEditActivity extends ActivityWithAutoCloseDialogs implements Common {
    private static final String mDebugPrefix = "WorkflowEdit-";
    private static final String SETTINGS_KEY = "WorkflowEditCurrent-";
    private static final int EXIF_EDIT_RESULT_ID = 86441;

    /** current modified value of the first selected file */
    private PhotoWorkFlowDto mCurrentData;

    private TextView preview;
    private Spinner spinnerPrefix;
    private EditText editName;
    private Spinner spinnerSuffix;
    private TextView exifChanges;

    public static void showActivity(Activity context, PhotoWorkFlowDto workflow,
                                    String directoryOrApmFileUrl
            , SelectedFiles selectedFiles, int requestCode) {
        if (Global.debugEnabled) {
            Log.d(Global.LOG_CONTEXT, context.getClass().getSimpleName()
                    + " > PhotoWorkflowEditActivity.showActivity");
        }

        final Intent intent = new Intent().setClass(context,
                PhotoWorkflowEditActivity.class);

        intent.setAction(Intent.ACTION_EDIT);

        if (workflow != null) {
            intent.putExtra(SETTINGS_KEY, workflow.toSerializable());
        }

        if (directoryOrApmFileUrl != null) {
            intent.setData(Uri.parse(directoryOrApmFileUrl));
        }

        if ((selectedFiles != null) && (selectedFiles.size() > 0)) {
            intent.putExtra(EXTRA_SELECTED_ITEM_IDS, selectedFiles.toIdString());
            intent.putExtra(EXTRA_SELECTED_ITEM_PATHS, selectedFiles.toString());
        }

        if (requestCode != 0) {
            context.startActivityForResult(intent, requestCode);
        } else {
            context.startActivity(intent);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putSerializable(SETTINGS_KEY, mCurrentData.toSerializable());

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Global.debugMemory(mDebugPrefix, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_workflow_edit);

        preview		 	= (TextView ) findViewById(R.id.lbl_preview);
        spinnerPrefix	= (Spinner  ) findViewById(R.id.spinner_prefix);
        editName		= (EditText ) findViewById(R.id.edit_name);
        spinnerSuffix	= (Spinner  ) findViewById(R.id.spinner_suffix);
        exifChanges	 	= (TextView ) findViewById(R.id.lbl_exif_changes);

        Button cmd = (Button ) findViewById(R.id.cmd_pick_exif);
        cmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPickExif();
            }
        });

        if (savedInstanceState != null) {
            mCurrentData = PhotoWorkFlowDto.load(savedInstanceState.getSerializable(SETTINGS_KEY));
        } else {
            Intent intent = getIntent();

            // Edit dir or edit ".apm"
            File apm = IntentUtil.getFile(intent.getData());
            if (apm != null) {
                if (apm.isFile()) apm = apm.getParentFile();
                try {
                    mCurrentData = new PhotoWorkFlowDto().load(apm);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            SelectedFiles items = getSelectedFiles("onCreate ", intent, false);
            File first = (items != null) ? items.getFile(0) : null;

            if ((first != null) && (first.exists())) {
                IMetaApi currentData = MediaScanner.getInstance(this).getExifFromFile(first);
                mCurrentData.setMediaDefaults(currentData);
            }
        }
    }

    private void onPickExif() {
    }

    private SelectedFiles getSelectedFiles(String dbgContext, Intent intent, boolean mustLoadIDs) {
        if (intent == null) return null;

        SelectedFiles result = null;

        String selectedIDs = intent.getStringExtra(EXTRA_SELECTED_ITEM_IDS);
        String selectedFiles = intent.getStringExtra(EXTRA_SELECTED_ITEM_PATHS);

        if ((selectedIDs != null) && (selectedFiles != null)) {
            result = new SelectedFiles(selectedFiles, selectedIDs);
        } else {
            String path = IntentUtil.getFilePath(this, IntentUtil.getUri(intent));
            String fileNames[] = SelectedFiles.getFileNameList(path);
            Long[] ids = null;
            int itemCount = (fileNames != null) ? fileNames.length : 0;

            if (itemCount > 0) {
                if ((mustLoadIDs) && (ids == null)) {
                    ids = new Long[itemCount];
                    Map<String, Long> idMap = FotoSql.execGetPathIdMap(this, fileNames);

                    for (int i = 0; i < itemCount; i++) {
                        ids[i] = idMap.get(fileNames[i]);
                    }
                }

                result = new SelectedFiles(fileNames, ids);
            }
        }

        if (Global.debugEnabled && (intent != null)){
            Log.d(Global.LOG_CONTEXT, mDebugPrefix + dbgContext + intent.toUri(Intent.URI_INTENT_SCHEME));
        }

        return result;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_edit_common, menu);
        AboutDialogPreference.onPrepareOptionsMenu(this, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.cmd_cancel:
                finish();
                return true;
            case R.id.cmd_ok:
                onOk();
                return true;
            case R.id.cmd_about:
                AboutDialogPreference.createAboutDialog(this).show();
                return true;
            case R.id.cmd_settings:
                SettingsActivity.show(this);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Call back from sub-activities.<br/>
     * Process Change StartTime (longpress start), Select StopTime before stop
     * (longpress stop) or filter change for detailReport
     */
    @Override
    protected void onActivityResult(final int requestCode,
                                    final int resultCode, final Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        switch (requestCode) {
            case EXIF_EDIT_RESULT_ID:
                onExifChanged((intent != null) ? intent.getData() : null);
                break;
            default:break;
        }
    }

    @Override
    protected void onResume () {
        Global.debugMemory(mDebugPrefix, "onResume");
        super.onResume();
    }

    private void onExifChanged(Uri data) {
        //!!!
    }

    /** save exif changes back to image and database */
    private void onOk() {
        Activity ctx = this;
        Intent intent = getIntent();
        SelectedFiles items = getSelectedFiles("onOk ", getIntent(), true);
        //!!!
    }

}

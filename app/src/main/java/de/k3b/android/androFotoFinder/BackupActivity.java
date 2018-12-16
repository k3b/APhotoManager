/*
 * Copyright (c) 2018-2019 by k3b.
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
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;

import de.k3b.android.androFotoFinder.queries.AndroidAlbumUtils;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.util.IntentUtil;
import de.k3b.android.widget.AboutDialogPreference;
import de.k3b.android.widget.ActivityWithAutoCloseDialogs;
import de.k3b.database.QueryParameter;
import de.k3b.io.IGalleryFilter;
import de.k3b.io.collections.SelectedFiles;
import de.k3b.io.DateUtil;
import de.k3b.media.MediaUtil;
import de.k3b.zip.IZipConfig;
import de.k3b.zip.ZipConfigDto;
import de.k3b.zip.ZipConfigRepository;

/**
 * #108: Zip-file support: backup-or-copy filtered-or-selected photos to Zip-file
 */
public class BackupActivity extends ActivityWithAutoCloseDialogs implements Common {

    public static final int REQUEST_BACKUP_ID = 99289;
    private static final String STATE_ZIP_CONFIG = "zip_config";
    private static String mDebugPrefix = "BackupActivity";

    private Gui gui = null;
    private SelectedFiles mSelectedFiles;

    /**
     * Shows Activity to edit and execute backup/copy to zip file
     *
     * Data priority
     * uri = *.zip.apm.cfg
     * else uri = *.album
     * else pk-s of selectedFiles
     * else filter+query
     */
    public static void showActivity(String debugContext, Activity context,
                                    Uri uri, SelectedFiles selectedFiles,
                                    IGalleryFilter filter, QueryParameter query,
                                    int requestCode) {
        final Intent intent = new Intent().setClass(context,
                BackupActivity.class);

        intent.setAction(Intent.ACTION_EDIT);

        IZipConfig config = loadZipConfig(uri, context);
        QueryParameter mergedQuery = null;
        // intent.putExtra()
        if (null == config) {
            config = new ZipConfigDto(null);
            mergedQuery = getQuery(debugContext, context,
                    uri, selectedFiles, filter, query);
        }

        if (mergedQuery != null) {
            config.setFilter(mergedQuery.toReParseableString());
        }

        if (Global.debugEnabled) {
            Log.d(Global.LOG_CONTEXT, mDebugPrefix + context.getClass().getSimpleName()
                    + " > BackupActivity.showActivity " + intent.toUri(Intent.URI_INTENT_SCHEME));
        }

        IntentUtil.startActivity(debugContext, context, requestCode, intent);
    }

    /**
     *
     * Data priority
     *
     * uri = *.album
     * else pk-s of selectedFiles
     * else filter+query
     *
     * @return query or null if not found
     */
    private static QueryParameter getQuery(String debugContext, Activity context,
                                          Uri uri, SelectedFiles selectedFiles,
                                          IGalleryFilter filter, QueryParameter query) {
        QueryParameter mergedQuery = AndroidAlbumUtils.getQueryFromUri(debugContext, context, uri, null);

        if (mergedQuery == null) {
            final int idCount = (selectedFiles == null) ? 0 : selectedFiles.size();
            if ((query != null) || (filter != null) || (idCount > 0)) {

                if (idCount > 0) {
                    mergedQuery = FotoSql.setWhereSelectionPks(new QueryParameter(), selectedFiles.toIdString());
                } else {
                    mergedQuery = AndroidAlbumUtils.getAsMergedNewQueryParameter(query, filter);
                }
            }
        }
        return mergedQuery;
    }

    private static ZipConfigRepository loadZipConfig(Uri uri, Activity context) {
        if ((uri != null) && ZipConfigRepository.isZipConfig(uri.toString())) {
            try {
                InputStream inputsteam = context.getContentResolver().openInputStream(uri);
                return new ZipConfigRepository().load(inputsteam, uri);
            } catch (Exception ex) {
                // file not found or no permission
                if (Global.debugEnabled) {
                    Log.d(Global.LOG_CONTEXT, mDebugPrefix + context.getClass().getSimpleName()
                            + "-loadZipConfig(" + uri + ") failed " + ex.getClass().getSimpleName(), ex);
                }
            }
        }
        return null;
    }

    private class Gui implements IZipConfig {
        private final EditText editDateModifiedFrom;
        private final EditText editZipRelPath;
        private final EditText editZipName;
        private final EditText editZipDir;
        private final EditText editFilter;

        private Gui() {
            editDateModifiedFrom = (EditText) findViewById(R.id.edit_date_modified_from);
            editFilter = (EditText) findViewById(R.id.edit_filter);
            editZipDir = (EditText) findViewById(R.id.edit_zip_dir);
            editZipName = (EditText) findViewById(R.id.edit_zip_name);
            editZipRelPath = (EditText) findViewById(R.id.edit_zip_rel_path);
        }

        private void toGui(IZipConfig src) {
            ZipConfigDto.copy(this, src);
        }

        private boolean fromGui(IZipConfig dest) {
            try {
                ZipConfigDto.copy(dest, this);
                return true;
            } catch (RuntimeException ex) {
                Log.e(Global.LOG_CONTEXT, mDebugPrefix + ex.getMessage(), ex);
                Toast.makeText(BackupActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
                return false;
            }
        }

        @Override
        public Date getDateModifiedFrom() {return DateUtil.parseIsoDate(editDateModifiedFrom.getText().toString());}
        @Override
        public String getZipRelPath() {return editZipRelPath.getText().toString();}
        @Override
        public String getZipName() {return editZipName.getText().toString();}
        @Override
        public String getZipDir() {return editZipDir.getText().toString();}
        @Override
        public String getFilter() {return editFilter.getText().toString();}

        @Override
        public void setDateModifiedFrom(Date value) {editDateModifiedFrom.setText(DateUtil.toIsoDateString(value));}
        @Override
        public void setZipRelPath(String value) {editZipRelPath.setText(value);}
        @Override
        public void setZipName(String value) {editZipName.setText(value);}
        @Override
        public void setZipDir(String value) {editZipDir.setText(value);}
        @Override
        public void setFilter(String value) {editFilter.setText(value);}
    }

    private ZipConfigDto mFilter = new ZipConfigDto(null);


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        gui.fromGui(mFilter);
        savedInstanceState.putSerializable(STATE_ZIP_CONFIG, mFilter);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Global.debugMemory(mDebugPrefix, "onCreate");
        super.onCreate(savedInstanceState);

        gui = new Gui();
        Intent intent = getIntent();
        mSelectedFiles = getSelectedFiles("onCreate ", intent, false);

        if (savedInstanceState != null) {
            mFilter.loadFrom((IZipConfig) savedInstanceState.getSerializable(STATE_ZIP_CONFIG));
        } else {
            // intent.

        }
        defineGui();
        gui.toGui(mFilter);
    }

    /**
     * load layout and bind layout members
     */
    private void defineGui() {
        setContentView(R.layout.activity_backup);


    }

    @NonNull
    private SelectedFiles getSelectedFiles(String dbgContext, Intent intent, boolean mustLoadIDs) {
        if (intent == null) return null;

        SelectedFiles result = AffUtils.getSelectedFiles(intent);

        if (result == null) {
            String path = IntentUtil.getFilePath(this, IntentUtil.getUri(intent));
            File rootDirFile = new File(path);
            String[] fileNames = rootDirFile.list(MediaUtil.JPG_FILENAME_FILTER);

            int itemCount = (fileNames != null) ? fileNames.length : 0;

            // convert to absolute paths
            String parentDirString = rootDirFile.getAbsolutePath();
            for (int i = 0; i < itemCount; i++) {
                fileNames[i] = parentDirString + "/" + fileNames[i];
            }

            Long[] ids = null;

            if (itemCount > 0) {
                if ((mustLoadIDs) && (ids == null)) {
                    ids = new Long[itemCount];
                    Map<String, Long> idMap = FotoSql.execGetPathIdMap(this, fileNames);

                    for (int i = 0; i < itemCount; i++) {
                        ids[i] = idMap.get(fileNames[i]);
                    }
                }

            }
            result = new SelectedFiles(fileNames,
                    ids, null);
        }

        if (Global.debugEnabled && (intent != null)) {
            Log.d(Global.LOG_CONTEXT, mDebugPrefix + dbgContext + intent.toUri(Intent.URI_INTENT_SCHEME));
        }

        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_edit_common, menu);
        getMenuInflater().inflate(R.menu.menu_autoprocessing, menu);

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
                setResult(Activity.RESULT_CANCELED, null);
                finish();
                return true;
            case R.id.cmd_about:
                AboutDialogPreference.createAboutDialog(this).show();
                return true;
            case R.id.cmd_settings:
                SettingsActivity.showActivity(this);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        Global.debugMemory(mDebugPrefix, "onResume");
        super.onResume();
    }

}


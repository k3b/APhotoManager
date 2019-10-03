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
 
package de.k3b.android.androFotoFinder.backup;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import de.k3b.android.androFotoFinder.AffUtils;
import de.k3b.android.androFotoFinder.Common;
import de.k3b.android.androFotoFinder.GalleryFilterActivity;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.SettingsActivity;
import de.k3b.android.androFotoFinder.directory.DirectoryPickerFragment;
import de.k3b.android.androFotoFinder.imagedetail.ImageDetailMetaDialogBuilder;
import de.k3b.android.androFotoFinder.media.AndroidLabelGenerator;
import de.k3b.android.androFotoFinder.queries.AndroidAlbumUtils;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.tagDB.TagSql;
import de.k3b.android.util.AndroidFileCommands;
import de.k3b.android.util.IntentUtil;
import de.k3b.android.util.OsUtils;
import de.k3b.android.widget.AboutDialogPreference;
import de.k3b.android.widget.ActivityWithAutoCloseDialogs;
import de.k3b.android.widget.HistoryEditText;
import de.k3b.database.QueryParameter;
import de.k3b.io.DateUtil;
import de.k3b.io.FileUtils;
import de.k3b.io.GalleryFilterFormatter;
import de.k3b.io.IDirectory;
import de.k3b.io.IGalleryFilter;
import de.k3b.io.ListUtils;
import de.k3b.io.StringUtils;
import de.k3b.io.collections.SelectedFiles;
import de.k3b.media.MediaFormatter;
import de.k3b.media.PhotoPropertiesUtil;
import de.k3b.zip.IZipConfig;
import de.k3b.zip.LibZipGlobal;
import de.k3b.zip.ZipConfigDto;
import de.k3b.zip.ZipConfigRepository;
import de.k3b.zip.ZipStorage;

/**
 * #108: Zip-file support: backup-or-copy filtered-or-selected photos to Zip-file.
 * Gui to edit the backup parameters
 *
 * API
 *  * uri = intent.getData() load file via file-uri
 *  * else intent.Extra[STATE_ZIP_CONFIG]
 */
public class BackupActivity extends ActivityWithAutoCloseDialogs implements Common {
    private static final int REQUEST_ID_PICK_ZIP_OUT_DIR = 1234;

    public static final int REQUEST_BACKUP_ID = 99289;
    public static final int REQUEST_ID_PICK_EXIF = 99293;

    /**
     * shows progress dialog while backup is running
     */
    private static final int REQUEST_ID_BACKUP_PROGRESS = 99294; //  0==deactivated

    private static final String STATE_ZIP_CONFIG = "zip_config";
    private static String mDebugPrefix = "BackupActivity: ";

    // != null while async backup is running
    private static BackupAsyncTask backupAsyncTask = null;

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
                                    String selectedRelDir, int requestCode) {
        final Intent intent = new Intent().setClass(context,
                BackupActivity.class);

        intent.setAction(Intent.ACTION_EDIT);

        IZipConfig config = Backup2ZipService.loadZipConfig(uri, context);
        File selectedRelDirFile = FileUtils.createFile(selectedRelDir);
        final String zipName = (selectedRelDirFile == null) ? null : selectedRelDirFile.getName();
        if (null == config) {
            config = getPreviousZipConfig(null, zipName);
        }
        if (null == config) {
            config = new ZipConfigDto(null);
            QueryParameter mergedQuery = getQuery(debugContext, context,
                    uri, selectedFiles, filter, query);
            if (mergedQuery != null) {
                config.setFilter(mergedQuery.toReParseableString(null));
            }

            if (selectedRelDirFile != null) {
                config.setZipRelPath(selectedRelDirFile.getPath());
                config.setZipName(zipName);
            }
        }


        if (config != null) {
            intent.putExtra(STATE_ZIP_CONFIG, (Serializable) config);
        }
        if (LibZipGlobal.debugEnabled) {
            Log.d(LibZipGlobal.LOG_TAG, mDebugPrefix + context.getClass().getSimpleName()
                    + " > BackupActivity.showActivity " + intent.toUri(Intent.URI_INTENT_SCHEME));
        }

        IntentUtil.startActivity(debugContext, context, requestCode, intent);
    }

    private static IZipConfig getPreviousZipConfig(Activity context, String zipName) {
        if (zipName != null) {
            File repositoryFile = ZipConfigRepository.getZipConfigFile(zipName);
            if ((repositoryFile != null) && (repositoryFile.exists())) {
                try {
                    IZipConfig repo = new ZipConfigRepository(null).load(new FileInputStream(repositoryFile), repositoryFile);
                    if (repo != null) {
                        if (context != null) {
                            showStatistics(context, repo, zipName);
                        }
                        // ZipConfigDto is serializable and ZipConfigRepository is not.
                        return new ZipConfigDto(repo);
                    }
                } catch (IOException ignore) {
                }
            }
        }
        return null;
    }

    private static void showStatistics(Activity context, IZipConfig config, String messagePrefix) {
        final QueryParameter asMergedQuery
                = Backup2ZipService.getEffectiveQueryParameter(config);
        CharSequence statistics = TagSql.getStatisticsMessage(context, 0, asMergedQuery);

        if (messagePrefix != null) {
            Toast.makeText(context, messagePrefix + statistics, Toast.LENGTH_LONG).show();
        }

        final TextView status = (TextView) context.findViewById(R.id.lbl_status);
        if (status != null) {
            status.setText(statistics);
        }
    }

    private void showCurrentStatistics() {
        showStatistics(this, gui, null);
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
        QueryParameter mergedQuery = AndroidAlbumUtils.getQueryFromUri(debugContext, context, null, uri, null);

        if (mergedQuery == null) {
            final int idCount = (selectedFiles == null) ? 0 : selectedFiles.size();
            if ((query != null) || (filter != null) || (idCount > 0)) {

                if (idCount > 0) {
                    mergedQuery = FotoSql.setWhereSelectionPks(new QueryParameter(), selectedFiles.toIdString());
                } else {
                    mergedQuery = AndroidAlbumUtils.getAsMergedNewQuery(query, filter);
                }
            }
        }
        return mergedQuery;
    }

    private void cmdShowDetails() {
        final QueryParameter asMergedQuery
                = Backup2ZipService.getEffectiveQueryParameter(this.gui);
        String sql = (asMergedQuery != null) ? asMergedQuery.toSqlString() : null;

        final Dialog dlg = ImageDetailMetaDialogBuilder.createImageDetailDialog(
                this,
                getTitle().toString(),
                sql,
                TagSql.getStatisticsMessage(this, R.string.backup_title, asMergedQuery)
        );
        dlg.show();
        setAutoClose(null, dlg, null);
    }

    private ZipConfigDto mZipConfigData = new ZipConfigDto(null);


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        saveGuiToData();
        savedInstanceState.putSerializable(STATE_ZIP_CONFIG, mZipConfigData);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void saveGuiToData() {
        gui.fromGui(mZipConfigData);
    }

    private void loadGuiFromData() {
        gui.toGui(mZipConfigData);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Global.debugMemory(mDebugPrefix, "onCreate");
        super.onCreate(savedInstanceState);
        defineGui();

        Intent intent = getIntent();

        mSelectedFiles = getSelectedFiles("onCreate ", intent, false);

        if (savedInstanceState != null) {
            mZipConfigData.loadFrom((IZipConfig) savedInstanceState.getSerializable(STATE_ZIP_CONFIG));
        } else {
            Uri uri = intent.getData();
            IZipConfig config = Backup2ZipService.loadZipConfig(uri, this);
            if (config != null) {
                mZipConfigData.loadFrom(config);
                showStatistics(this, config, null);
            } else {
                mZipConfigData.loadFrom((IZipConfig) intent.getSerializableExtra(STATE_ZIP_CONFIG));
            }
        }
        loadGuiFromData();
        gui.updateHistory();
    }

    @Override
    protected void onDestroy() {
        setBackupAsyncTaskProgessReceiver(null);
        super.onDestroy();
    }

    /**
     * load layout and bind layout members
     */
    private void defineGui() {
        setContentView(R.layout.activity_backup);

        this.gui = new Gui();

        Button cmd = (Button) findViewById(R.id.cmd_zip_dir);
        cmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String path = gui.getZipDir();
                pickDir(true, path,R.string.lbl_zip_dir);
            }
        });

        cmd = (Button) findViewById(R.id.cmd_date);
        cmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDateTimeApi.showDatePicker();
            }
        });
        cmd = (Button) findViewById(R.id.cmd_time);
        cmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDateTimeApi.showTimePicker();
            }
        });

        final Button cmd2 = (Button) findViewById(R.id.cmd_filter);
        cmd2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickPickExif(" cmd " + cmd2.getText());
            }
        });
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
            case REQUEST_ID_PICK_EXIF:
                if (resultCode != 0) {
                    QueryParameter q = AndroidAlbumUtils.getQuery(this, "",
                            null, intent, null,
                            null, null);
                    onExifChanged((q != null) ? q.toReParseableString(null) : null);
                }
                break;

            case REQUEST_ID_PICK_ZIP_OUT_DIR:
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = intent.getData();
                    if (uri != null) {
                        final String folderLocation = uri.toString();
                        onZipDirPick(folderLocation);
                    }
                }
                break;

            case REQUEST_ID_BACKUP_PROGRESS:
                //!!!
                if (resultCode == Activity.RESULT_OK) {
                }
                break;

            default:
                break;
        }
    }

    private void onClickPickExif(String debugContext) {
        saveGuiToData();
        QueryParameter filter = QueryParameter.parse(mZipConfigData.getFilter());
        GalleryFilterActivity.showActivity("[20] " + debugContext, BackupActivity.this,
                null, filter, null, REQUEST_ID_PICK_EXIF);
    }

    /**
     * exif editor result
     */
    private void onExifChanged(String modifiedQuery) {
        mZipConfigData.setFilter((modifiedQuery != null) ? modifiedQuery : "");
        loadGuiFromData();
        gui.updateHistory();
        showCurrentStatistics();
    }

    private final DateTimeApi mDateTimeApi = new DateTimeApi();

    private class DateTimeApi
            implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {
        public void showTimePicker() {
            final Calendar c = getDateTimeTakenAsCalendar();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            Dialog dlg1 = new TimePickerDialog(BackupActivity.this, mDateTimeApi, hour, minute,
                    DateFormat.is24HourFormat(BackupActivity.this));
            dlg1.show();
            setAutoClose(null, dlg1, null);
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            final Calendar c = getDateTimeTakenAsCalendar();
            c.set(Calendar.HOUR_OF_DAY, hourOfDay);
            c.set(Calendar.MINUTE, minute);
            gui.setDateModifiedFrom(c.getTime());
        }

        public void showDatePicker() {
            final Calendar c = getDateTimeTakenAsCalendar();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog dlg1 = new DatePickerDialog(BackupActivity.this, mDateTimeApi,
                    year, month, day);
            dlg1.show();
            setAutoClose(null, dlg1, null);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            final Calendar c = getDateTimeTakenAsCalendar();
            c.set(Calendar.YEAR, year);
            c.set(Calendar.MONTH, month);
            c.set(Calendar.DAY_OF_MONTH, day);
            gui.setDateModifiedFrom(c.getTime());
        }

        private Calendar getDateTimeTakenAsCalendar() {
            final Calendar c = Calendar.getInstance();
            c.setTimeZone(TimeZone.getTimeZone("gmt"));
            Date dateTimeTaken = gui.getDateModifiedFrom();
            if (dateTimeTaken != null) {
                c.setTimeInMillis(dateTimeTaken.getTime());
            }
            return c;
        }
    }

    public static class DestZipDirPicker extends DirectoryPickerFragment {
        protected static AndroidFileCommands sFileCommands = null;

        public static DestZipDirPicker newInstance(boolean outDir/*, final SelectedFiles srcFotos*/) {
            DestZipDirPicker f = new DestZipDirPicker();

            // Supply index input as an argument.
            Bundle args = new Bundle();
            args.putBoolean("outDir", outDir);
            // AffUtils.putSelectedFiles(args, srcFotos);

            f.setArguments(args);

            return f;
        }

        public boolean isOutDir() {
            return getArguments().getBoolean("outDir", false);
        }

        /** do not use activity callback */
        @Override
        protected void setDirectoryListener(Activity activity) {}

        @Override
        protected void onDirectoryPick(IDirectory selection) {
            final BackupActivity activity = (BackupActivity) getActivity();
            if ((selection != null) && (activity != null)) {
                if (isOutDir()) {
                    activity.onZipDirPick(selection.getAbsolute());
                } else {
                    activity.onRelPathPick(selection.getAbsolute());
                }
            }
            dismiss();
        }

    }

    private boolean pickDir(boolean outDir, String lastCopyToPath, int titleId) {
        if (AndroidFileCommands.canProcessFile(this, false)) {
            if (BackupProgressActivity.USE_DOCUMENT_PROVIDER) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

                IntentUtil.startActivity("pickDir",
                        this, REQUEST_ID_PICK_ZIP_OUT_DIR, intent);
            } else {
                DestZipDirPicker dlg = DestZipDirPicker.newInstance(outDir/*, fotos*/);

                dlg.defineDirectoryNavigation(OsUtils.getRootOSDirectory(null),
                        0,
                        lastCopyToPath);
                dlg.setContextMenuId(R.menu.menu_context_pick_osdir);
                dlg.setTitleId(titleId);
                dlg.show(this.getFragmentManager(), "osdir");
                dlg.setBaseQuery(Backup2ZipService.getEffectiveQueryParameter(gui));
                setAutoClose(dlg, null, null);
            }
        }
        return false;
    }

    private void onZipDirPick(String absolute) {
        if (LibZipGlobal.debugEnabled || Global.debugEnabled) {
            Log.d(LibZipGlobal.LOG_TAG, mDebugPrefix + "onZipDirPick " + absolute);
        }
        this.gui.setZipDir(absolute);
    }

    private void onRelPathPick(String absolute) {
        this.gui.setZipRelPath(absolute);
    }

    @NonNull
    private SelectedFiles getSelectedFiles(String dbgContext, Intent intent, boolean mustLoadIDs) {
        if (intent == null) return null;

        SelectedFiles result = AffUtils.getSelectedFiles(intent);

        if (result == null) {
            String path = IntentUtil.getFilePath(this, IntentUtil.getUri(intent));
            if (!StringUtils.isNullOrEmpty(path)) {
                File rootDirFile = new File(path);
                String[] fileNames = rootDirFile.list(PhotoPropertiesUtil.JPG_FILENAME_FILTER);

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
        }

        if (LibZipGlobal.debugEnabled && (intent != null)) {
            Log.d(LibZipGlobal.LOG_TAG, mDebugPrefix + dbgContext + intent.toUri(Intent.URI_INTENT_SCHEME));
        }

        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_edit_common, menu);
        getMenuInflater().inflate(R.menu.menu_copy_paste, menu);
        getMenuInflater().inflate(R.menu.menu_backup, menu);

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
            case R.id.cmd_ok:
                onBakupOk();
                return true;
            case R.id.cmd_clear:
                clearFilter();
                return true;
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
            case R.id.action_details:
                cmdShowDetails();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPause() {
        setBackupAsyncTaskProgessReceiver(null);
        super.onPause();
    }

    @Override
    protected void onResume() {
        setBackupAsyncTaskProgessReceiver(this);
        Global.debugMemory(mDebugPrefix, "onResume");
        super.onResume();

    }

    private void setBackupAsyncTaskProgessReceiver(Activity progressReceiver) {
        if (backupAsyncTask != null) {
            final ProgressBar progressBar = (ProgressBar) this.findViewById(R.id.progressBar);
            final TextView status = (TextView) this.findViewById(R.id.lbl_status);
            final Button cancel = (Button) this.findViewById(R.id.cmd_cancel);

            final boolean isActive = BackupAsyncTask.isActive(backupAsyncTask);
            final boolean running = (progressReceiver != null) && isActive;
            setVisibility(running, progressBar, cancel);

            if (running) {
                backupAsyncTask.setContext(this, progressBar, status);
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        backupAsyncTask.cancel(false);
                    }
                });

            } else {
                backupAsyncTask.setContext(null, null, null);
                cancel.setOnClickListener(null);
                if (!isActive) {
                    backupAsyncTask = null;
                }
            }
        }
    }

    /**
     * save exif changes back to image and database
     */
    private boolean onBakupOk() {
        saveGuiToData();
        gui.mHistory.saveHistory();

        if (REQUEST_ID_BACKUP_PROGRESS != 0) {
            BackupProgressActivity.showActivity(this, mZipConfigData, REQUEST_ID_BACKUP_PROGRESS);
        } else {
            Date backupDate = new Date();
            final String zipDir = mZipConfigData.getZipDir();
            final String zipName = ZipConfigDto.getZipFileName(mZipConfigData, backupDate);
            ZipStorage zipStorage = BackupProgressActivity.getCurrentStorage(this, zipDir, zipName);

            backupAsyncTask = new BackupAsyncTask(this, mZipConfigData, zipStorage,
                    backupDate);
            setBackupAsyncTaskProgessReceiver(this);
            backupAsyncTask.execute();
        }
        return true;
    }

    private void setVisibility(boolean visible, View... views) {
        for (View v : views) {
            if (v != null) v.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void clearFilter() {
        mZipConfigData = new ZipConfigDto(null);
        loadGuiFromData();
    }

    private class Gui implements IZipConfig {
        private static final String FILTER_DELIMITER = "\n\n\n\n@--!@--!@--!\n\n\n\n";
        private final EditText editDateModifiedFrom;
        private final EditText editZipRelPath;
        private final EditText editZipName;
        private final EditText editZipDir;
        private final EditText editFilter;
        private final TextView exifFilterDetails;
        private final GalleryFilterFormatter formatter;

        private HistoryEditText mHistory;

        private Gui() {
            formatter = new GalleryFilterFormatter(false,
                    new AndroidLabelGenerator(getApplicationContext(), "\n"),
                    MediaFormatter.FieldID.clasz, MediaFormatter.FieldID.visibility,
                    MediaFormatter.FieldID.lastModified);

            editDateModifiedFrom = (EditText) findViewById(R.id.edit_date_modified_from);
            editFilter = (EditText) findViewById(R.id.edit_filter);
            exifFilterDetails = (TextView) findViewById(R.id.lbl_exif_filter_details);

            editZipDir = (EditText) findViewById(R.id.edit_zip_dir);
            editZipName = (EditText) findViewById(R.id.edit_zip_name);
            editZipRelPath = (EditText) findViewById(R.id.edit_zip_rel_path);

            mHistory = new HistoryEditText(BackupActivity.this, new int[]{
                    R.id.cmd_zip_name_history,
                    R.id.cmd_date_modified_from_history,
                    R.id.cmd_zip_rel_path_history,
                    R.id.cmd_zip_dir_history,
                    R.id.cmd_filter_history},
                    editZipName,
                    editDateModifiedFrom,
                    editZipRelPath,
                    editZipDir,
                    editFilter) {
                @Override
                protected boolean onHistoryPick(EditorHandler editorHandler, EditText editText, String text) {
                    boolean chagend = ((text != null) && (editText != null)
                            && (!text.equalsIgnoreCase(editText.getText().toString())));

                    final boolean result = super.onHistoryPick(editorHandler, editText, text);

                    if (chagend) {
                        if (editText.equals(editFilter)) {
                            showExifFilterDetails(gui);
                            updateHistory();
                            showCurrentStatistics();
                        }
                        if (editText.equals(editDateModifiedFrom)) {
                            showCurrentStatistics();
                        }
                        if (editText.equals(editZipName)) {
                            IZipConfig config = getPreviousZipConfig(BackupActivity.this, text);
                            if (config != null) {
                                toGui(config);
                            }
                        }
                    }

                    return result;
                }

            }.setIncludeEmpty(true);

        }

        private void toGui(IZipConfig src) {
            ZipConfigDto.copy(this, src);
            showExifFilterDetails(src);
        }

        public void updateHistory() {
            updateHistory(getEffectiveQueryParameter(this));
        }

        private void updateHistory(QueryParameter query) {
            List<String> paths = new ArrayList<String>();
            List<String> filenames = new ArrayList<String>();

            String minFolder = FotoSql.getMinFolder(getApplicationContext(), query, true);
            updateHistory(FileUtils.getDir(minFolder), filenames, paths, 1);

            String queryFolder = FotoSql.getFilePath(query, false);
            if ((queryFolder != null) && (queryFolder.startsWith("/"))) {
                updateHistory(FileUtils.getDir(queryFolder), filenames, paths, 1);
            }

            mHistory.addHistory(0, ListUtils.asStringArray(filenames));
            mHistory.addHistory(2, ListUtils.asStringArray(paths));
        }

        private void updateHistory(File dir, List<String> filenames, List<String> paths, int recursion) {
            if (dir != null) {
                if (recursion > 0) {
                    updateHistory(dir.getParentFile(), filenames, paths, recursion - 1);
                }
                include(filenames, dir.getName());
                include(paths, dir.getAbsolutePath());
            }
        }

        private void include(List<String> names, String name) {
            if (!StringUtils.isNullOrEmpty(name) && !names.contains(name)) names.add(name);
        }

        public void showExifFilterDetails(IZipConfig src) {
            QueryParameter query = getEffectiveQueryParameter(src);

            StringBuilder result = new StringBuilder();
            CharSequence details = formatter.format(TagSql.parseQueryEx(query, true));
            if (details != null) {
                result.append(details);
            }
            if (query.hasWhere()) {
                query.toParsableWhere(result);
            }

            exifFilterDetails.setText(result.toString());
        }

        public QueryParameter getEffectiveQueryParameter(IZipConfig src) {
            return Backup2ZipService.getEffectiveQueryParameter(src);
        }

        private boolean fromGui(IZipConfig dest) {
            try {
                ZipConfigDto.copy(dest, this);
                return true;
            } catch (RuntimeException ex) {
                Log.e(LibZipGlobal.LOG_TAG, mDebugPrefix + ex.getMessage(), ex);
                Toast.makeText(BackupActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
                return false;
            }
        }

        @Override
        public Date getDateModifiedFrom() {
            return DateUtil.parseIsoDate(editDateModifiedFrom.getText().toString());
        }

        @Override
        public String getZipRelPath() {
            return editZipRelPath.getText().toString();
        }

        @Override
        public String getZipName() {
            return editZipName.getText().toString();
        }

        @Override
        public String getZipDir() {
            return editZipDir.getText().toString();
        }

        @Override
        public String getFilter() {
            // display-text = filter-display-Text + FILTER_DELIMITER + sql
            // return sql-part only
            final String fullText = editFilter.getText().toString();
            int delim = fullText.indexOf(FILTER_DELIMITER);
            if (delim >= 0) return fullText.substring(delim + FILTER_DELIMITER.length());
            return fullText;
        }

        @Override
        public void setDateModifiedFrom(Date value) {
            editDateModifiedFrom.setText(DateUtil.toIsoDateTimeString(value));
        }

        @Override
        public void setZipRelPath(String value) {
            editZipRelPath.setText(value);
        }

        @Override
        public void setZipName(String value) {
            editZipName.setText(value);
        }

        @Override
        public void setZipDir(String value) {
            editZipDir.setText(value);
        }

        @Override
        public void setFilter(String value) {
            editFilter.setText(getFilterDisplayText(value));
        }

        private String getFilterDisplayText(String value) {
            QueryParameter q = (value == null) ? null : QueryParameter.parse(value);
            if (q != null) {
                IGalleryFilter filter = TagSql.parseQueryEx(q, true);

                // display-text = filter-display-Text + FILTER_DELIMITER + sql
                return filter + FILTER_DELIMITER + value;
            }
            return "";
        }
    }

}


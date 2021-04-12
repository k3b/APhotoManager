/*
 * Copyright (c) 2017-2020 by k3b.
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
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import de.k3b.LibGlobal;
import de.k3b.android.androFotoFinder.media.AndroidLabelGenerator;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.util.ClipboardUtil;
import de.k3b.android.util.IntentUtil;
import de.k3b.android.widget.AboutDialogPreference;
import de.k3b.android.widget.HistoryEditText;
import de.k3b.io.DateUtil;
import de.k3b.io.ListUtils;
import de.k3b.io.PhotoAutoprocessingDto;
import de.k3b.io.RuleFileNameProcessor;
import de.k3b.io.StringUtils;
import de.k3b.io.collections.SelectedFiles;
import de.k3b.io.filefacade.FileFacade;
import de.k3b.io.filefacade.IFile;
import de.k3b.media.IPhotoProperties;
import de.k3b.media.PhotoPropertiesAsString;
import de.k3b.media.PhotoPropertiesDTO;
import de.k3b.media.PhotoPropertiesFormatter;
import de.k3b.media.PhotoPropertiesUtil;

/**
 * #93: Editor for a WorkflowItem ".apm" files that define
 * rules to process one or more photos
 * * filename schemata for copied/moved files
 * * default properties that every photot should receive.
 */
public class PhotoAutoprocessingEditActivity extends BaseActivity implements Common {
    private static final String DEBUG_PREFIX = "AutoProcEdit-";
    private static final String SETTINGS_KEY = "AutoProcEditCurrent-";
    private static final int EXIF_EDIT_RESULT_ID = 86441;
    private static final String PREF_LAST_RENAME_DATE_PATTERN = "LastRenameDatePattern";
    private static final String PREF_LAST_RENAME_NUMBER_PATTERN = "LastRenameNumberPattern";
    private static final String EXTRA_RENAME_MULTIBLE = "ExtraRenameMultible";

    /**
     * current modified value of the first selected file
     */
    private IFile mCurrentOutDir = null;
    private PhotoAutoprocessingDto mCurrentAutoprocessingData;
    private SelectedFiles mSelectedFiles;

    private RuleFileNameProcessor mProcessor;

    private TextView mPreview;
    private Spinner mSpinnerDatePattern;
    private EditText mEditName;
    private Spinner mSpinnerNumberPattern;
    private TextView mExifChanges;
    private IFile exampleSrcfile;
    private Date exampleDate;

    private PhotoPropertiesFormatter.ILabelGenerator mLabelGenerator;
    private HistoryEditText mHistory;


    public static void showActivity(String debugContext, Activity context,
                                    PhotoAutoprocessingDto workflow,
                                    String directoryOrApmFileUrl,
                                    SelectedFiles selectedFiles,
                                    int requestCode, String dialogTitle) {
        final Intent intent = new Intent().setClass(context,
                PhotoAutoprocessingEditActivity.class);

        intent.setAction(Intent.ACTION_EDIT);

        putAutoprocessing(intent, workflow);

        if (directoryOrApmFileUrl != null) {
            intent.setData(Uri.parse(directoryOrApmFileUrl));
        }

        if (dialogTitle != null) {
            intent.putExtra(EXTRA_TITLE, dialogTitle);
        }

        if (requestCode == R.id.cmd_rename_multible) {
            intent.putExtra(EXTRA_RENAME_MULTIBLE, true);
        }

        AffUtils.putSelectedFiles(intent, selectedFiles);

        if (Global.debugEnabled) {
            Log.d(Global.LOG_CONTEXT, DEBUG_PREFIX + context.getClass().getSimpleName()
                    + " > PhotoAutoprocessingEditActivity.showActivity " + intent.toUri(Intent.URI_INTENT_SCHEME));
        }

        IntentUtil.startActivity(debugContext, context, requestCode, intent);
    }

    public static void putAutoprocessing(Intent intent, PhotoAutoprocessingDto workflow) {
        if (workflow != null) {
            intent.putExtra(SETTINGS_KEY, workflow);
        }
    }

    public static PhotoAutoprocessingDto getAutoprocessingData(Intent intent) {
        if (intent != null) {
            return (PhotoAutoprocessingDto) intent.getSerializableExtra(SETTINGS_KEY);
        }
        return null;
    }

    @Override
    protected void onCreateEx(Bundle savedInstanceState) {
        this.mLabelGenerator = new AndroidLabelGenerator(getApplicationContext(), "\n");
        Intent intent = getIntent();
        CharSequence title = (intent == null) ? null : intent.getStringExtra(EXTRA_TITLE);

        if (title != null) {
            this.setTitle(title);
        }
        mSelectedFiles = getSelectedFiles("onCreate ", intent, false);

        // Edit dir or edit ".apm"
        mCurrentOutDir = FileFacade.convert(DEBUG_PREFIX + " mCurrentOutDir",
                IntentUtil.getFile(intent.getData()));
        if (mCurrentOutDir != null) {
            if (mCurrentOutDir.isFile()) mCurrentOutDir = mCurrentOutDir.getParentFile();
        }

        mCurrentAutoprocessingData = null;
        if (savedInstanceState != null) {
            mCurrentAutoprocessingData = (PhotoAutoprocessingDto) savedInstanceState.getSerializable(SETTINGS_KEY);
        } else {
            mCurrentAutoprocessingData = getAutoprocessingData(intent);
        }

        if ((mCurrentAutoprocessingData == null) && (mCurrentOutDir != null)) {
            try {
                mCurrentAutoprocessingData = new PhotoAutoprocessingDto();
                mCurrentAutoprocessingData.load(mCurrentOutDir);
            } catch (IOException e) {
                onFatalError(DEBUG_PREFIX + "Cannot load .apm from " + mCurrentAutoprocessingData, e);
                return;
            }
        }

        if ((mCurrentOutDir == null) && (mSelectedFiles != null) && (mSelectedFiles.size() > 0)) {
            mCurrentOutDir = mSelectedFiles.getIFile(0).getParentFile();
        }

        if (Global.debugEnabled) {
            final String nl = "\n\t.";
            Log.d(Global.LOG_CONTEXT, ListUtils.toString(" ", DEBUG_PREFIX,
                    "onCreate", intent.toUri(Intent.URI_INTENT_SCHEME),
                    nl, mCurrentOutDir,
                    nl, "savedInstanceState", savedInstanceState,
                    nl, mCurrentAutoprocessingData));
        }

        if ((mCurrentAutoprocessingData == null) || (mCurrentOutDir == null)) {
            onFatalError(DEBUG_PREFIX + "Missing Intent.data parameter. intent="
                    + intent.toUri(Intent.URI_INTENT_SCHEME), null);
            return;
        }

        mCurrentAutoprocessingData.setOutDir(mCurrentOutDir);
        mProcessor = new RuleFileNameProcessor(mCurrentOutDir);

        // if there are no MediaDefaults yet infer them from selected files
        if (mCurrentAutoprocessingData.getMediaDefaults() == null) {
            PhotoPropertiesAsString exampleExif = PhotoPropertiesUtil.inferAutoprocessingExifDefaults(new PhotoPropertiesAsString(), mSelectedFiles.getIFiles());
            mCurrentAutoprocessingData.setMediaDefaults(exampleExif);
        }
        this.exampleSrcfile = RuleFileNameProcessor.getFile(mSelectedFiles.getIFile(0));

        this.exampleDate = getExampleDate(mSelectedFiles.getDatesPhotoTaken(), this.exampleSrcfile);
        defineGui(intent.getBooleanExtra(EXTRA_RENAME_MULTIBLE, false));
        toGui();
    }

    private void onFatalError(String msg, Exception exception) {
        Log.e(Global.LOG_CONTEXT, msg, exception);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        setResult(Activity.RESULT_CANCELED, null);
        finish();
    }

    /**
     * simple dto for spinner with pattern and example
     */
    private static class Pattern {
        private final String pattern;
        private final String value;

        Pattern(String pattern, String value, boolean longString) {
            this.pattern = pattern;
            this.value = value + ((longString && (value.length() > 0)) ? (" (" + pattern + ")") : "");
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private void fromGui() {
        mCurrentAutoprocessingData.setName(mEditName.getText().toString());
        mCurrentAutoprocessingData.setDateFormat(getSelectedPattern(mSpinnerDatePattern));
        mCurrentAutoprocessingData.setNumberFormat(getSelectedPattern(mSpinnerNumberPattern));
    }

    /** to avoid endless recursion toGui() ... TextView.setText ... afterTextChanged ... toGui() */
    private int inToGuiCount = 0;

    private void toGui() {
        String numberFormat = mCurrentAutoprocessingData.getNumberFormat();
        final String namePart = mCurrentAutoprocessingData.getName();
        final String dateFormat = mCurrentAutoprocessingData.getDateFormat();

        inToGuiCount++;
        try {
            mEditName.setText(namePart);
            select(mSpinnerDatePattern, dateFormat);
            select(mSpinnerNumberPattern, numberFormat);

            if (mPreview != null) {
                String exampleResultFileName = createExampleResultFileName(dateFormat, namePart, numberFormat);

                // !!! where to get "copy"/"move" from?
                String photoOperation = ""; // getString(R.string.move_menu_title);

                // %1$s %2$d Photos\n\t%3$s (%4$s), ...\nTo %5$s\n\t%6$s
        /*
            Copy 5 Photos
				hello.jpg (2017-08-02), ...
			To .../path/toFile/
				1708NewName0001.jpg
        */
                mPreview.setText(getString(R.string.preview_message_format,
                        photoOperation,
                        mSelectedFiles.size(),
                        (exampleSrcfile == null) ? null : exampleSrcfile.getName(),
                        DateUtil.toIsoDateString(exampleDate),
                        mCurrentAutoprocessingData.getOutDir(), exampleResultFileName));
            }

            if (mExifChanges != null) {
                IPhotoProperties mediaChanges = mCurrentAutoprocessingData.getMediaDefaults();
                CharSequence exifChange = null;
                if (mediaChanges != null) {
                    exifChange = PhotoPropertiesFormatter.format(mediaChanges,
                            false, mLabelGenerator, PhotoPropertiesFormatter.FieldID.clasz);
                }
                mExifChanges.setText(exifChange);
            }
        } finally {
            inToGuiCount--;
        }
    }

    private String createExampleResultFileName(String dateFormat, String baseName, String numberFormat) {
        mProcessor.set(dateFormat, baseName, numberFormat);

        IFile file = mProcessor.getNextFile(null, exampleDate, StringUtils.isNullOrEmpty(numberFormat) ? 0 : 1);
        if(file != null) return file.getName();
        return null;
    }

    private void select(Spinner spinner, String selectedValueOrNull) {
        ArrayAdapter<Pattern> adapter = (ArrayAdapter<Pattern>) spinner.getAdapter();
        int count = adapter.getCount();
        String selectedValue = selectedValueOrNull == null ? "" : selectedValueOrNull;

        for (int i = 0; i < count; i++) {
            Pattern p = adapter.getItem(i);
            if (StringUtils.compare(p.pattern, selectedValue) == 0) {
                spinner.setSelection(i);
            }
        }
    }

    private String getSelectedPattern(Spinner spinner) {
        Pattern sel = (Pattern) spinner.getSelectedItem();
        return (sel == null) ? null : sel.pattern;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        fromGui();
        savedInstanceState.putSerializable(SETTINGS_KEY, mCurrentAutoprocessingData);
        if (Global.debugEnabled) {
            Log.d(Global.LOG_CONTEXT, DEBUG_PREFIX
                    + " onSaveInstanceState " + savedInstanceState);
        }

        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * load layout and bind layout members
     */
    private void defineGui(boolean rename) {
        @LayoutRes int layoutID = rename ? R.layout.activity_rename_multible : R.layout.activity_photo_workflow_edit;
        if (LibGlobal.debugEnabledJpg && Global.debugEnabled) {
            layoutID = R.layout.activity_photo_workflow_edit_dbg;
        }
        setContentView(layoutID);

        mPreview = (TextView) findViewById(R.id.lbl_preview);
        mSpinnerDatePattern = (Spinner) findViewById(R.id.sp_date_pattern);
        mSpinnerDatePattern.setAdapter(new ArrayAdapter<Pattern>(this, android.R.layout.simple_list_item_1, createDatePatterns()));
        mSpinnerDatePattern.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCurrentAutoprocessingData.setDateFormat(getSelectedPattern(mSpinnerDatePattern));
                toGui();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mEditName = (EditText) findViewById(R.id.edit_name);
        mEditName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                // to avoid endless recursion toGui() ... TextView.setText ... afterTextChanged ... toGui()
                if (inToGuiCount == 0) {
                    String newValue = s.toString();
                    if (0 != StringUtils.compare(newValue, mCurrentAutoprocessingData.getName())) {
                        int start = mEditName.getSelectionStart();
                        int end = mEditName.getSelectionEnd();
                        // prevent stackoverflow
                        mCurrentAutoprocessingData.setName(newValue);
                        toGui();
                        mEditName.setSelection(start, end); // prevent cursor from jumping
                    }
                }
            }
        });

        mSpinnerNumberPattern = (Spinner) findViewById(R.id.sp_number_pattern);
        mSpinnerNumberPattern.setAdapter(new ArrayAdapter<Pattern>(this, android.R.layout.simple_list_item_1, createNumberPatterns()));
        mSpinnerNumberPattern.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCurrentAutoprocessingData.setNumberFormat(getSelectedPattern(mSpinnerNumberPattern));
                toGui();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mExifChanges = (TextView) findViewById(R.id.lbl_exif_changes);

        final Button cmd = (Button) findViewById(R.id.cmd_pick_exif);
        if (cmd != null) {
            cmd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onPickExif(" cmd " + cmd.getText());
                }
            });
            cmd.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return onReInferExifAndPick(" cmd " + cmd.getText());
                }
            });
        }

        findViewById(R.id.cmd_file_name_pattern_history).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOpenRenamerPopupMenu(v);
            }
        });

        mHistory = new HistoryEditText(this, new int[]{
                R.id.cmd_file_name_base_history},
                mEditName).setIncludeEmpty(true);
    }

    private List<Pattern> createDatePatterns() {
        String[] patternValues = getResources().getStringArray(R.array.date_patterns);
        ArrayList<Pattern> result = new ArrayList<>();
        for (String patternValue : patternValues) {
            String formattedExample = (!StringUtils.isNullOrEmpty(patternValue))
                    ? new SimpleDateFormat(patternValue).format(this.exampleDate)
                    : "";
            result.add(new Pattern(patternValue, formattedExample, true));
        }
        return result;
    }

    private Date getExampleDate(Date[] datesPhotoTaken, IFile exampleSrcfile) {
        Date exampleDate = (datesPhotoTaken != null && datesPhotoTaken.length > 0) ? datesPhotoTaken[0] : null;
        if (exampleDate == null) {
            exampleDate = getExampleDate(RuleFileNameProcessor.getFile(exampleSrcfile));
        }
        return exampleDate;
    }

    private Date getExampleDate(IFile exampleSrcfile) {
        Date exampleValue = null;
        if ((mCurrentAutoprocessingData != null) && (mCurrentAutoprocessingData.getMediaDefaults() != null)) {
            exampleValue = mCurrentAutoprocessingData.getMediaDefaults().getDateTimeTaken();
        }
        if (exampleValue == null) {
            long lastModified = (exampleSrcfile == null) ? 0 : exampleSrcfile.lastModified();
            exampleValue = (lastModified == 0) ? new Date() : new Date(lastModified);
        }
        return exampleValue;
    }

    private List<Pattern> createNumberPatterns() {
        long exampleValue = 1;

        /*
        if ((mCurrentAutoprocessingData != null) && (mCurrentAutoprocessingData.getOutDir() != null)) {
            exampleValue = mCurrentAutoprocessingData.getMediaDefaults().getDateTimeTaken();
        }
        if (exampleValue == null) exampleValue = new Date();
        */

        String[] patternValues = getResources().getStringArray(R.array.number_patterns);
        ArrayList<Pattern> result = new ArrayList<Pattern>();
        for (String patternValue : patternValues) {
            String formattedExample = (!StringUtils.isNullOrEmpty(patternValue))
                    ? new DecimalFormat(patternValue).format(exampleValue)
                    : "";
            result.add(new Pattern(patternValue, formattedExample, false));
        }
        return result;
    }

    private void onOpenRenamerPopupMenu(View anchor) {
        PopupMenu popup = createRenamerPopupMenu(anchor);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onRenamerPopupMenuClick(item);
            }
        });

        if (popup != null) {
            popup.show();
        }
    }

    private PopupMenu createRenamerPopupMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.menu_renamer_context, popup.getMenu());

        String name = this.mProcessor.getDirBaseName();
        setMenuItem(popup, R.id.cmd_dir, name);
        name = getParentDirBaseName();
        setMenuItem(popup, R.id.cmd_subdir_dir, name);
        return popup;
    }

    private String getParentDirBaseName() {
        return this.mProcessor.getParentDirBaseName() + "-" + this.mProcessor.getDirBaseName();
    }

    private void setMenuItem(PopupMenu popup, int menuId, String dirBaseName) {
        MenuItem menuitem = popup.getMenu().findItem(menuId);
        if ((dirBaseName != null) && (menuitem != null)) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

            String dateFormat = sharedPref.getString(PREF_LAST_RENAME_DATE_PATTERN, null);
            String numberFormat = sharedPref.getString(PREF_LAST_RENAME_NUMBER_PATTERN, null);
            menuitem.setTitle(createExampleResultFileName(dateFormat, dirBaseName, numberFormat));
        }
    }

    private boolean onRenamerPopupMenuClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.cmd_clear:
                return setFileRule(null);
            case R.id.cmd_dir:
                return setFileRule(this.mProcessor.getDirBaseName());
            case R.id.cmd_subdir_dir:
                return setFileRule(this.getParentDirBaseName());
        }
        return false;
    }

    private boolean setFileRule(String namePart) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        mCurrentAutoprocessingData.setNumberFormat((namePart == null) ? "" : sharedPref.getString(PREF_LAST_RENAME_NUMBER_PATTERN, null));
        mCurrentAutoprocessingData.setName((namePart == null) ? "" : namePart);
        mCurrentAutoprocessingData.setDateFormat((namePart == null) ? "" : sharedPref.getString(PREF_LAST_RENAME_DATE_PATTERN, null));

        toGui();

        return true;
    }

    private void saveLastFilePattern(String dateFormat, String numberFormat) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = sharedPref.edit();

        edit.putString(PREF_LAST_RENAME_DATE_PATTERN, dateFormat);
        edit.putString(PREF_LAST_RENAME_NUMBER_PATTERN, numberFormat);

        edit.apply();

    }

    /**
     * cmd exif press: schow exif editor
     */
    private void onPickExif(String debugContext) {
        fromGui();
        PhotoPropertiesEditActivity.showActivity(debugContext, this, mCurrentAutoprocessingData.getMediaDefaults(),
                null, getSelectedFiles(DEBUG_PREFIX + "EditExif-", getIntent(),
                        false),
                EXIF_EDIT_RESULT_ID, false);
    }

    /**
     * #132: cmd exif long-press: infer exif from selected and schow exif editor
     */
    private boolean onReInferExifAndPick(String debugContext) {
        fromGui();
        IPhotoProperties currentMediaDefaults = mCurrentAutoprocessingData.getMediaDefaults();

        SelectedFiles selectedFiles = getSelectedFiles(DEBUG_PREFIX + "EditExif-", getIntent(),
                false);
        IPhotoProperties inferedMediaDefaults = PhotoPropertiesUtil.inferAutoprocessingExifDefaults(new PhotoPropertiesDTO(), selectedFiles.getFiles());

        if (inferedMediaDefaults.getDescription() != null) currentMediaDefaults.setDescription(inferedMediaDefaults.getDescription());
        if (inferedMediaDefaults.getLatitude() != null) currentMediaDefaults.setLatitudeLongitude(inferedMediaDefaults.getLatitude(), inferedMediaDefaults.getLongitude());

        if (inferedMediaDefaults.getTags() != null) {
            List<String> tags = currentMediaDefaults.getTags();
            ListUtils.include(tags, inferedMediaDefaults.getTags());
            inferedMediaDefaults.setTags(tags);
        }

        PhotoPropertiesEditActivity.showActivity(debugContext, this, currentMediaDefaults,
                null, selectedFiles,
                EXIF_EDIT_RESULT_ID, false);
        return true;
    }
    /**
     * exif editor result
     */
    private void onExifChanged(IPhotoProperties modifiedExif) {
        if (modifiedExif != null) {
            mCurrentAutoprocessingData.setMediaDefaults(modifiedExif);
            toGui();
        }
    }

    @NonNull
    private SelectedFiles getSelectedFiles(String dbgContext, Intent intent, boolean mustLoadIDs) {
        if (intent == null) return null;

        SelectedFiles result = AffUtils.getSelectedFiles(intent);

        if (result == null) {
            String path = IntentUtil.getFilePath(this, IntentUtil.getUri(intent));
            IFile rootDirFile = FileFacade.convert(DEBUG_PREFIX + ".getSelectedFiles", path);

            IFile[] files = rootDirFile.listFiles();
            if (files != null) {
                IFile[] fileNames = new IFile[files.length];
                int itemCount = 0;
                for (int i = 0; i < files.length; i++) {
                    if (PhotoPropertiesUtil.isImage(files[i], PhotoPropertiesUtil.IMG_TYPE_ALL)) {
                        fileNames[itemCount++] = files[i];
                    }
                }

                Long[] ids = null;

                if (itemCount > 0) {
                    if ((mustLoadIDs) && (ids == null)) {
                        ids = new Long[itemCount];
                        Map<String, Long> idMap = FotoSql.execGetPathIdMap(fileNames);

                        for (int i = 0; i < itemCount; i++) {
                            ids[i] = idMap.get(fileNames[i]);
                        }
                    }

                }
                result = new SelectedFiles(fileNames,
                        ids, null);
            }

            if (Global.debugEnabled && (intent != null)) {
                Log.d(Global.LOG_CONTEXT, DEBUG_PREFIX + dbgContext + intent.toUri(Intent.URI_INTENT_SCHEME));
            }

        }
        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_edit_common, menu);
        getMenuInflater().inflate(R.menu.menu_copy_paste, menu);

        MenuItem item = menu.findItem(android.R.id.paste);
        final IFile apmFile = getiApmFileFromClipboard();
        if ((item != null) && (apmFile != null) && apmFile.exists()) {
            item.setVisible(true);
        }

        item = menu.findItem(android.R.id.copy);
        if ((item != null) && (mCurrentOutDir == null)) {
            item.setVisible(false);
        }
        AboutDialogPreference.onPrepareOptionsMenu(this, menu);

        return true;
    }

    private IFile getiApmFileFromClipboard() {
        final IFile clipboardDir = ClipboardUtil.getClipboardDir(this);
        return (clipboardDir == null) ? null : PhotoAutoprocessingDto.getApmFile(clipboardDir);
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
            case R.id.cmd_ok:
                onOk();
                return true;
            case R.id.cmd_clear:
                clearFilter();
                return true;
            case R.id.cmd_about:
                AboutDialogPreference.createAboutDialog(this).show();
                return true;
            case R.id.cmd_settings:
                SettingsActivity.showActivity(this);
                return true;

            case android.R.id.copy:
                return ClipboardUtil.addDirToClipboard(this, mCurrentOutDir.getAbsolutePath(), true);
            case android.R.id.paste:
                return onPaste();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean onPaste() {
        IFile dir = ClipboardUtil.getClipboardDir(this);
        if (dir != null) {
            try {
                PhotoAutoprocessingDto srcApm = new PhotoAutoprocessingDto().load(dir);
                if (srcApm != null) {
                    mCurrentAutoprocessingData.paste(srcApm);
                    toGui();
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
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
                if (resultCode != 0) {
                    onExifChanged(PhotoPropertiesEditActivity.getExifParam(intent));
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onResume() {
        Global.debugMemory(DEBUG_PREFIX, "onResume");
        super.onResume();
    }

    /**
     * save exif changes back to image and database
     */
    private void onOk() {
        fromGui();
        try {
            mHistory.saveHistory();
            Intent resultIntent = null;
            if (getAutoprocessingData(getIntent()) != null) {
                resultIntent = new Intent();
                AffUtils.putSelectedFiles(resultIntent, this.mSelectedFiles);
                putAutoprocessing(resultIntent, mCurrentAutoprocessingData);
            } else {
                mCurrentAutoprocessingData.save();
                saveLastFilePattern(mCurrentAutoprocessingData.getDateFormat(), mCurrentAutoprocessingData.getNumberFormat());
            }
            setResult(Activity.RESULT_OK, resultIntent);
        } catch (IOException e) {
            onFatalError("onOk()-save()", e);
        }
        finish();
    }

    private void clearFilter() {
        mCurrentAutoprocessingData.clear();
        toGui();
    }

}


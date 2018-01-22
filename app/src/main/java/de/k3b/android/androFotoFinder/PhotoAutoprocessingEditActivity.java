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
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.util.IntentUtil;
import de.k3b.android.util.MediaScanner;
import de.k3b.android.widget.AboutDialogPreference;
import de.k3b.android.widget.ActivityWithAutoCloseDialogs;
import de.k3b.io.ListUtils;
import de.k3b.io.VISIBILITY;
import de.k3b.io.collections.SelectedFiles;
import de.k3b.io.DateUtil;
import de.k3b.io.RuleFileNameProcessor;
import de.k3b.io.PhotoWorkFlowDto;
import de.k3b.io.StringUtils;
import de.k3b.media.IMetaApi;
import de.k3b.media.MediaAsString;
import de.k3b.media.MediaUtil;

/**
 * Editor for a WorkflowItem ".apm" files that define
 * rules to process one or more photos
 * * filename schemata for copied/moved files
 * * default properties that every photot should receive.
 */
public class PhotoAutoprocessingEditActivity extends ActivityWithAutoCloseDialogs implements Common {
    private static final String mDebugPrefix = "AutoProcEdit-";
    private static final String SETTINGS_KEY = "AutoProcEditCurrent-";
    private static final int EXIF_EDIT_RESULT_ID = 86441;

    /**
     * current modified value of the first selected file
     */
    private PhotoWorkFlowDto mCurrentData;
    private SelectedFiles mSelectedFiles;

    private RuleFileNameProcessor mProcessor;

    private TextView mPreview;
    private Spinner mSpinnerDatePattern;
    private EditText mEditName;
    private Spinner mSpinnerNumberPattern;
    private TextView mExifChanges;

    public static void showActivity(Activity context, PhotoWorkFlowDto workflow,
                                    String directoryOrApmFileUrl
            , SelectedFiles selectedFiles, int requestCode) {
        final Intent intent = new Intent().setClass(context,
                PhotoAutoprocessingEditActivity.class);

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

        if (Global.debugEnabled) {
            Log.d(Global.LOG_CONTEXT, mDebugPrefix + context.getClass().getSimpleName()
                    + " > PhotoAutoprocessingEditActivity.showActivity " + intent.toUri(Intent.URI_INTENT_SCHEME));
        }

        if (requestCode != 0) {
            context.startActivityForResult(intent, requestCode);
        } else {
            context.startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Global.debugMemory(mDebugPrefix, "onCreate");
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mSelectedFiles = getSelectedFiles("onCreate ", intent, false);

        // Edit dir or edit ".apm"
        File currentOutDir = IntentUtil.getFile(intent.getData());
        if (currentOutDir != null) {
            if (currentOutDir.isFile()) currentOutDir = currentOutDir.getParentFile();
        }

        if (savedInstanceState != null) {
            mCurrentData = PhotoWorkFlowDto.load(savedInstanceState.getSerializable(SETTINGS_KEY));
        } else if (currentOutDir != null) {
            try {
                mCurrentData = new PhotoWorkFlowDto();
                mCurrentData.load(currentOutDir);
            } catch (IOException e) {
                onFatalError(mDebugPrefix + "Cannot load .apm from " + mCurrentData, e);
                return;
            }
        }

        if (Global.debugEnabled) {
            final String nl = "\n\t.";
            Log.d(Global.LOG_CONTEXT, ListUtils.toString(" ", mDebugPrefix,
                    "onCreate",intent.toUri(Intent.URI_INTENT_SCHEME),
                    nl,currentOutDir,
                    nl,"savedInstanceState",savedInstanceState,
                    nl,mCurrentData));
        }

        if ((currentOutDir == null) || (mCurrentData == null)) {
            onFatalError(mDebugPrefix + "Missing Intent.data parameter. intent="
                    + intent.toUri(Intent.URI_INTENT_SCHEME), null);
            return;
        }

        mCurrentData.setOutDir(currentOutDir);
        mProcessor = new RuleFileNameProcessor(currentOutDir);

        if (mCurrentData.getMediaDefaults() == null) {
            File first = mSelectedFiles.getFile(0);

            MediaAsString exampleExif = new MediaAsString();
            if ((first != null) && (first.exists())) {
                IMetaApi example = MediaScanner.getInstance(this).getExifFromFile(first);
                if (example != null) {
                    exampleExif.setData(example);
                }
            }
            exampleExif.setDateTimeTaken(null);
            exampleExif.setPath(null);
            mCurrentData.setMediaDefaults(exampleExif);
        }
        defineGui();
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
        mCurrentData.setName(mEditName.getText().toString());
        mCurrentData.setDateFormat(getSelectedPattern(mSpinnerDatePattern));
        mCurrentData.setNumberFormat(getSelectedPattern(mSpinnerNumberPattern));
    }

    /** to avoid endless recursion toGui() ... TextView.setText ... afterTextChanged ... toGui() */
    private int inToGuiCount = 0;
    private void toGui() {
        inToGuiCount++;
        try {
            mEditName.setText(mCurrentData.getName());
            select(mSpinnerDatePattern, mCurrentData.getDateFormat());
            String numberFormat = mCurrentData.getNumberFormat();
            select(mSpinnerNumberPattern, numberFormat);
            mProcessor.set(mCurrentData.getDateFormat(), mCurrentData.getName(), numberFormat);

            File exampleSrcfile = mProcessor.getFile(mSelectedFiles.getFile(0));
            Date exampleDate = getExampleDate(exampleSrcfile);
            File exampleResultFile = mProcessor.getNextFile(exampleSrcfile, exampleDate, StringUtils.isNullOrEmpty(numberFormat) ? 0 : 1);

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
                    mCurrentData.getOutDir(), exampleResultFile.getName()));

            IMetaApi mediaChanges = mCurrentData.getMediaDefaults();
            String exifChange = null;
            if (mediaChanges != null) {
                exifChange = MediaUtil.toString(mediaChanges, false, mLabelGenerator, MediaUtil.FieldID.clasz);
            }
            mExifChanges.setText(exifChange);
        } finally {
            inToGuiCount--;
        }
    }

    private void select(Spinner spinner, String selectedFormat) {
        ArrayAdapter<Pattern> adapter = (ArrayAdapter<Pattern>) spinner.getAdapter();
        int count = adapter.getCount();

        for (int i = 0; i < count; i++) {
            Pattern p = adapter.getItem(i);
            if (StringUtils.compare(p.pattern, selectedFormat) == 0) {
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
        savedInstanceState.putSerializable(SETTINGS_KEY, mCurrentData.toSerializable());
        if (Global.debugEnabled) {
            Log.d(Global.LOG_CONTEXT, mDebugPrefix
                    + " onSaveInstanceState " + savedInstanceState);
        }

        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * load layout and bind layout members
     */
    private void defineGui() {
        setContentView(R.layout.activity_photo_workflow_edit);

        mPreview = (TextView) findViewById(R.id.lbl_preview);
        mSpinnerDatePattern = (Spinner) findViewById(R.id.sp_date_pattern);
        mSpinnerDatePattern.setAdapter(new ArrayAdapter<Pattern>(this, android.R.layout.simple_list_item_1, createDatePatterns()));
        mSpinnerDatePattern.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCurrentData.setDateFormat(getSelectedPattern(mSpinnerDatePattern));
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
                    if (0 != StringUtils.compare(newValue, mCurrentData.getName())) {
                        int start = mEditName.getSelectionStart();
                        int end = mEditName.getSelectionEnd();
                        // prevent stackoverflow
                        mCurrentData.setName(newValue);
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
                mCurrentData.setNumberFormat(getSelectedPattern(mSpinnerNumberPattern));
                toGui();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mExifChanges = (TextView) findViewById(R.id.lbl_exif_changes);

        Button cmd = (Button) findViewById(R.id.cmd_pick_exif);
        cmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPickExif();
            }
        });

    }

    private List<Pattern> createDatePatterns() {
        Date exampleValue = getExampleDate(mProcessor.getFile(mSelectedFiles.getFile(0)));

        String[] patternValues = getResources().getStringArray(R.array.date_patterns);
        ArrayList<Pattern> result = new ArrayList<Pattern>();
        for (String patternValue : patternValues) {
            String formattedExample = (!StringUtils.isNullOrEmpty(patternValue))
                    ? new SimpleDateFormat(patternValue).format(exampleValue)
                    : "";
            result.add(new Pattern(patternValue, formattedExample, true));
        }
        return result;
    }

    private Date getExampleDate(File exampleSrcfile) {
        Date exampleValue = null;
        if ((mCurrentData != null) && (mCurrentData.getMediaDefaults() != null)) {
            exampleValue = mCurrentData.getMediaDefaults().getDateTimeTaken();
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
        if ((mCurrentData != null) && (mCurrentData.getOutDir() != null)) {
            exampleValue = mCurrentData.getMediaDefaults().getDateTimeTaken();
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

    /**
     * cmd exif press: schow exif editor
     */
    private void onPickExif() {
        fromGui();
        ExifEditActivity.showActivity(this, mCurrentData.getMediaDefaults(), null, null, EXIF_EDIT_RESULT_ID);
    }

    /**
     * exif editor result
     */
    private void onExifChanged(IMetaApi modifiedExif) {
        if (modifiedExif != null) {
            mCurrentData.setMediaDefaults(modifiedExif);
            toGui();
        }
    }


    private File getExample(String dbgContext, Intent intent) {
        SelectedFiles selectedFiles = getSelectedFiles(dbgContext, intent, false);
        File file = (selectedFiles != null)
                ? selectedFiles.getFile(0) : null;
        if (file == null) {
            file = new File("/path/to/source.jpg");
        }
        return file;
    }

    @NonNull
    private SelectedFiles getSelectedFiles(String dbgContext, Intent intent, boolean mustLoadIDs) {
        if (intent == null) return null;

        SelectedFiles result = null;

        String selectedIDs = intent.getStringExtra(EXTRA_SELECTED_ITEM_IDS);
        String selectedFiles = intent.getStringExtra(EXTRA_SELECTED_ITEM_PATHS);

        if ((selectedIDs != null) && (selectedFiles != null)) {
            result = new SelectedFiles(selectedFiles, selectedIDs);
        } else {
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
            result = new SelectedFiles(fileNames, ids);
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
                if (resultCode != 0) {
                    onExifChanged(ExifEditActivity.getExifParam(intent));
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onResume() {
        Global.debugMemory(mDebugPrefix, "onResume");
        super.onResume();
    }

    /**
     * save exif changes back to image and database
     */
    private void onOk() {
        fromGui();
        try {
            mCurrentData.save();
            setResult(Activity.RESULT_OK, null);
        } catch (IOException e) {
            onFatalError("onOk()-save()", e);
        }
        finish();
    }

    /**
     * implement resource based labels for MediaUtil.toString(...)
     */
    private MediaUtil.ILabelGenerator mLabelGenerator = new MediaUtil.ILabelGenerator() {
        @Override
        public String get(MediaUtil.FieldID id) {
            switch (id) {
                case dateTimeTaken:
                    return getString2(R.string.lbl_date);
                case title:
                    return getString2(R.string.lbl_title);
                case description:
                    return getString2(R.string.lbl_description);
                case latitude_longitude:
                    return getString2(R.string.lbl_latitude_short) + "/" + getString(R.string.lbl_longitude_short) + " ";
                case rating:
                    return getString2(R.string.lbl_rating);
                case visibility:
                    return getString2(R.string.lbl_image_visibility);
                case tags:
                    return getString2(R.string.lbl_tag);
                case path:
                case clasz:
                    return null;
            }
            return null;
        }
    };

    public final String getString2(int resId) {
        return "\n" + getString(resId) + " ";
    }
}


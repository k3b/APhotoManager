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
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import de.k3b.FotoLibGlobal;
import de.k3b.android.androFotoFinder.media.MediaContentValues;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.tagDB.TagsPickerFragment;
import de.k3b.android.util.IntentUtil;
import de.k3b.android.util.MediaScanner;
import de.k3b.android.util.ResourceUtils;
import de.k3b.android.widget.AboutDialogPreference;
import de.k3b.android.widget.ActivityWithAutoCloseDialogs;
import de.k3b.android.widget.AsyncTaskWithProgressDialog;
import de.k3b.android.widget.HistoryEditText;
import de.k3b.database.SelectedFiles;
import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.api.IGeoPointInfo;
import de.k3b.geo.io.GeoUri;
import de.k3b.io.DateUtil;
import de.k3b.io.FileUtils;
import de.k3b.io.GeoUtil;
import de.k3b.io.IGalleryFilter;
import de.k3b.io.StringUtils;
import de.k3b.media.IMetaApi;
import de.k3b.media.JpgMetaWorkflow;
import de.k3b.media.MediaAsString;
import de.k3b.media.MediaDiffCopy;
import de.k3b.media.MediaUtil;
import de.k3b.media.MetaWriterExifXml;
import de.k3b.tagDB.Tag;
import de.k3b.tagDB.TagConverter;
import de.k3b.transactionlog.TransactionLoggerBase;

/**
 * Defines a gui for global foto filter: only fotos from certain filepath, date and/or lat/lon will be visible.
 */
public class ExifEditActivity extends ActivityWithAutoCloseDialogs implements Common {
    private static final boolean SYNC_UPDATE_EXIF = false; // for sync debugging. false: asynch task
    private static final String mDebugPrefix = "ExifEdit-";
    private static final String DLG_NAVIGATOR_TAG = mDebugPrefix;

    public static final int resultID = 522;
    private static final int GEO_RESULT_ID = 572;

    private static final String SETTINGS_KEY = "ExifEditActivity-";
    private static final String SETTINGS_KEY_INITIAL = "ExifEditActivityInitial-";
    private static final GeoUri PARSER = new GeoUri(GeoUri.OPT_PARSE_INFER_MISSING);

    /** current modified value of the first selected file */
    private MediaAsString mCurrentData;
    /** originial unmodified value of the first selected file */
    private MediaAsString mInitialData;

    private EditText edTitle;
    private EditText edDescription;
    private EditText edDate;
    private EditText edTagsInclude;
    private EditText edLatitude;
    private EditText edLongitude;
    private RatingBar   rating;

    private HistoryEditText mHistory;
    private UpdateTask exifUpdate;

    public static void showActivity(Activity context, String url,
                                    SelectedFiles selectedFiles, int requestCode) {
        Uri initalUri = null;

        if (Global.debugEnabled) {
            Log.d(Global.LOG_CONTEXT, context.getClass().getSimpleName()
                    + " > ExifEditActivity.showActivity");
        }

        final Intent intent = new Intent().setClass(context,
                ExifEditActivity.class);

        intent.setAction(Intent.ACTION_EDIT);
        if (url != null) {
            intent.setData(Uri.parse(url));
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
        saveGuiToExif("onSaveInstanceState");

        savedInstanceState.putString(SETTINGS_KEY, mCurrentData.toString());
        savedInstanceState.putString(SETTINGS_KEY_INITIAL, mInitialData.toString());

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Global.debugMemory(mDebugPrefix, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exif_edit);

        edTitle = (EditText) findViewById(R.id.edit_title);
        edDescription = (EditText) findViewById(R.id.edit_description);

        edDate          = (EditText) findViewById(R.id.edit_date);
        edTagsInclude   = (EditText) findViewById(R.id.edit_tags_include);
        edLatitude      = (EditText) findViewById(R.id.edit_latitude);
        edLongitude     = (EditText) findViewById(R.id.edit_longitude);
        rating = (RatingBar)  findViewById(R.id.ratingBar);

        onCreateButtos();

        mCurrentData = new MediaAsString();
        mInitialData = new MediaAsString();

        if (savedInstanceState != null) {
            String data = savedInstanceState.getString(SETTINGS_KEY, null);
            if (data != null) {
                mCurrentData.fromString(data);
            }
            data = savedInstanceState.getString(SETTINGS_KEY_INITIAL, null);
            if (data != null) {
                mInitialData.fromString(data);
            }
        } else {
            SelectedFiles items = getSelectedFiles("onCreate ", getIntent(), false);
            File first = (items != null) ? items.getFile(0) : null;

            if ((first != null) && (first.exists())) {
                IMetaApi currentData = MediaScanner.getInstance(this).getExifFromFile(first);
                mCurrentData.setData(currentData);
                mInitialData.setData(currentData);
            }
        }
        loadGuiFromExif(((savedInstanceState != null) ? "on(re)Create" : "onCreate"));

        mHistory = new HistoryEditText(this, new int[] {
                R.id.cmd_title_history                     ,
                R.id.cmd_description_history               ,
                R.id.cmd_date_history                      ,
                R.id.cmd_tags_include_history              ,
                R.id.cmd_lat_history                  ,
                R.id.cmd_lon_history                  },
                edTitle                     ,
                edDescription               ,
                edDate                      ,
                edTagsInclude               ,
                edLatitude                  ,
                edLongitude                 );

        new HashTagEditWatcher(this, edTitle);
        new HashTagEditWatcher(this, edDescription);
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

    private void loadGuiFromExif(String function) {
        MediaUtil.copy(mActivityData, mCurrentData, true, true);
        debugChanges(function);
    }

    private void saveGuiToExif(String function) {
        MediaUtil.copy(mCurrentData, mActivityData, true, true);
        debugChanges(function);
    }

    // used to analyse error #91:.
    private void debugChanges(String function) {
        if (FotoLibGlobal.debugEnabledJpgMetaIo){
            MediaDiffCopy diff = new MediaDiffCopy();
            diff.setDiff(mInitialData, mCurrentData);
            Log.d(FotoLibGlobal.LOG_TAG, mDebugPrefix + " "
                    + function + "\n\t"
                    + diff + "\n\t"
                    + SETTINGS_KEY_INITIAL + "=" + mInitialData.toString() + ";\n\t"
                    + SETTINGS_KEY + "=" + mCurrentData.toString())
            ;
        }
    }

    private void onCreateButtos() {
        Button cmd;
        cmd = (Button) findViewById(R.id.cmd_select_lat_lon);
        cmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showGeoPicker();

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
        cmd = (Button) findViewById(R.id.cmd_tags_include);
        cmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTagsApi.showTagPicker(null, -1, null);
            }
        });
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
            case GEO_RESULT_ID:
                onGeoChanged((intent != null) ? intent.getData() : null);
                break;
            default:break;
        }
    }

    @Override
    protected void onResume () {
        Global.debugMemory(mDebugPrefix, "onResume");
        super.onResume();
    }

    public void showGeoPicker() {
        saveGuiToExif("showGeoPicker"); // #92:bugfix
        GeoPointDto geo = new GeoPointDto();

        Double latitude = mCurrentData.getLatitude();
        Double longitude = mCurrentData.getLongitude();
        if ((latitude != null) && (longitude != null)) {
            if ((latitude.doubleValue() != 0) || (latitude.doubleValue() != 0)) {
                geo.setLatitude(latitude);
                geo.setLongitude(longitude);
            } // ignore (0,0)
        }
        String geoUri = PARSER.toUriString(geo);
        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        final Uri parseUri = Uri.parse(geoUri);
        intent.setData(parseUri);
        intent.putExtra(EXTRA_TITLE, getString(R.string.geo_picker_title));

        try {
            // #7: allow choosing geo pick from map or from "photo with geo"
            startActivityForResult(Intent.createChooser(intent, getString(R.string.geo_edit_menu_title)), GEO_RESULT_ID);
            // this.startActivityForResult(intent, RESULT_ID);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(ExifEditActivity.this, R.string.geo_picker_err_not_found,Toast.LENGTH_LONG).show();
        }
    }

    private void onGeoChanged(Uri data) {
        IGeoPointInfo geo = (data == null) ? null : PARSER.fromUri(data.toString());
        if ((geo != null) && (!GeoPointDto.isEmpty(geo))) {
            if ((geo.getLatitude() != 0) || (geo.getLongitude() != 0)) {
                // bugfix #92:
                mCurrentData.setLatitudeLongitude(geo.getLatitude(), geo.getLongitude());
                loadGuiFromExif("onGeoChanged");
            } // ignore (0,0)
        }
    }

    private final DateTimeApi mDateTimeApi = new DateTimeApi();

    private class DateTimeApi
            implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {
        public void showTimePicker() {
            saveGuiToExif("showTimePicker");
            final Calendar c = getDateTimeTakenAsCalendar();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            Dialog dlg1 = new TimePickerDialog(ExifEditActivity.this, mDateTimeApi, hour, minute,
                    DateFormat.is24HourFormat(ExifEditActivity.this));
            dlg1.show();
            setAutoClose(null, dlg1, null);
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            final Calendar c = getDateTimeTakenAsCalendar();
            c.set(Calendar.HOUR_OF_DAY, hourOfDay);
            c.set(Calendar.MINUTE, minute);
            mCurrentData.setDateTimeTaken(c.getTime());
            loadGuiFromExif("onTimeSet");
        }

        public void showDatePicker() {
            saveGuiToExif("showDatePicker");
            final Calendar c = getDateTimeTakenAsCalendar();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog dlg1 = new DatePickerDialog(ExifEditActivity.this, mDateTimeApi,
                    year, month, day);
            dlg1.show();
            setAutoClose(null, dlg1, null);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            final Calendar c = getDateTimeTakenAsCalendar();
            c.set(Calendar.YEAR, year);
            c.set(Calendar.MONTH, month);
            c.set(Calendar.DAY_OF_MONTH, day);
            mCurrentData.setDateTimeTaken(c.getTime());
            loadGuiFromExif("onDateSet");
        }

        private Calendar getDateTimeTakenAsCalendar() {
            final Calendar c = Calendar.getInstance();
            c.setTimeZone(TimeZone.getTimeZone("gmt"));
            Date dateTimeTaken = mCurrentData.getDateTimeTaken();
            if (dateTimeTaken != null) {
                c.setTimeInMillis(dateTimeTaken.getTime());
            }
            return c;
        }
    }

    private final TagsApi mTagsApi = new TagsApi();
    private class TagsApi implements TagsPickerFragment.ITagsPicker {
        private void showTagPicker(CharSequence filterValue, int selection, TagsPickerFragment.ITagsSelector selector) {
            saveGuiToExif("showTagPicker");
            final FragmentManager manager = getFragmentManager();
            TagsPickerFragment dlg = new TagsPickerFragment();
            dlg.setFragmentOnwner(mTagsApi);
            dlg.setTitleId(R.string.tags_activity_title);
            dlg.setTagSelector(selector);
            // dlg.setAddNames(mFilter.getTagsAllIncluded());
            List<String> tags = mCurrentData.getTags();
            if (tags == null) tags = new ArrayList<String>();
            dlg.setAddNames(tags);
            dlg.setAffectedNames(tags);
            if (filterValue != null) {
                dlg.setFilter(filterValue, selection);
            }
            dlg.show(manager, DLG_NAVIGATOR_TAG);
            setAutoClose(dlg, null, null);
        }

        /**
         * called by {@link TagsPickerFragment}
         */
        @Override
        public boolean onCancel(String msg) {
            setAutoClose(null, null, null);
            return true;
        }

        /**
         * called by {@link TagsPickerFragment}
         */
        @Override
        public boolean onOk(List<String> addNames, List<String> removeNames) {
            mCurrentData.setTags(addNames);
            loadGuiFromExif("Tags onOk");
            setAutoClose(null, null, null);
            return true;
        }

        /**
         * called by {@link TagsPickerFragment}
         */
        @Override
        public boolean onTagPopUpClick(int menuItemItemId, Tag selectedTag) {
            return TagsPickerFragment.handleMenuShow(menuItemItemId, selectedTag, ExifEditActivity.this, null);
        }
    };

    private static class HashTagEditWatcher implements TextWatcher {
        private boolean enabled = true;
        private EditText mEditView = null;
        private ExifEditActivity mActivity  = null;

        HashTagEditWatcher(ExifEditActivity activity, EditText editView) {
            this.mEditView = editView;
            this.mActivity = activity;
            editView.setTag(this);
            editView.addTextChangedListener(this);
        }

        public static void close(EditText editView) {
            HashTagEditWatcher watcher = getWatcher(editView);
            if (watcher != null) {
                editView.setTag(null);
                editView.removeTextChangedListener(watcher);
                watcher.mEditView = null;
                watcher.mActivity = null;
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if ((enabled) && (before == 0) && (count == 1)) {
                int after = start - before + count;
                int tagStart = StringUtils.getTagStart(s, after);
                final int tagEnd = StringUtils.getTagEnd(s, after);
                if ((tagStart >= 0) && (tagEnd > tagStart)) {
                    final int contentStart = tagStart + 1;
                    mActivity.mTagsApi.showTagPicker(s.subSequence(contentStart, tagEnd), start - tagStart, new TagsPickerFragment.ITagsSelector() {
                        @Override
                        public void onSelect(CharSequence tag, List<String> addNames) {
                            StringBuilder content = new StringBuilder(mEditView.getText());
                            content.delete(contentStart, tagEnd);
                            content.insert(contentStart, tag + " ");
                            enabled = false;
                            mEditView.setText(content);
                            int newSelPos = contentStart + tag.length() + 1;
                            mEditView.setSelection(newSelPos, newSelPos);
                            enabled = true;
                            ResourceUtils.setFocusWithKeyboard(mEditView);

                            // content of edit title/description => model
                            mActivity.saveGuiToExif("onTextChanged saveGuiToExif");

                            List<String> tags = addNames;
                            if (tags == null) tags = new ArrayList<String>();
                            if (!tags.contains(tag)) {
                                tags.add(tag.toString());
                            }
                            mActivity.mCurrentData.setTags(tags);
                            mActivity.loadGuiFromExif("onTextChanged loadGuiFromExif");
                            mActivity.setAutoClose(null, null, null);
                        }
                    });
                }
            }

        }

        @Override
        public void afterTextChanged(Editable s) {
        }

        public static HashTagEditWatcher getWatcher(EditText editView) {
            if (editView != null) {
                return (HashTagEditWatcher) editView.getTag();
            }
            return null;
        }
    }

    private final IMetaApi mActivityData = new ActivityData();
    private class ActivityData implements IMetaApi {

        /**
         * Normalized absolute path to file (jpg or xmp)
         */
        @Override
        public String getPath() {
            return null;
        }

        @Override
        public IMetaApi setPath(String filePath) {
            return this;
        }

        /**
         * When the photo was taken (not file create/modify date) in local time or utc
         */
        @Override
        public Date getDateTimeTaken() {
            return DateUtil.parseIsoDate(edDate.getText().toString());
        }

        @Override
        public IMetaApi setDateTimeTaken(Date value) {
            edDate.setText(DateUtil.toIsoDateString(value));
            return this;
        }

        /** latitude, in degrees north. (-90 .. +90); longitude, in degrees east.  (-180 .. + 180)    */
        @Override public IMetaApi setLatitudeLongitude(Double latitude, Double longitude) {
            edLatitude.setText(GeoUtil.toCsvStringLatLon(latitude));
            edLongitude.setText(GeoUtil.toCsvStringLatLon(longitude));
            return this;
        }

        @Override
        public Double getLatitude() {
            return GeoUtil.parse(edLatitude.getText().toString(), "NS");
        }

        @Override
        public Double getLongitude() {
            return GeoUtil.parse(edLongitude.getText().toString(), "EW");
        }

        @Override
        public IMetaApi setTitle(String title) {
            edTitle.setText(title);
            return this;
        }

        @Override
        public String getTitle() {
            return edTitle.getText().toString().trim();
        }

        /**
         * Longer description = comment. may have more than one line
         */
        @Override
        public String getDescription() {
            return edDescription.getText().toString().trim();
        }

        @Override
        public IMetaApi setDescription(String description) {
            edDescription.setText(description);
            return this;
        }

        /**
         * Tags/Keywords/Categories/VirtualAlbum used to find images
         */
        @Override
        public List<String> getTags() {
            return TagConverter.fromString(edTagsInclude.getText().toString());
        }

        @Override
        public IMetaApi setTags(List<String> tags) {
            edTagsInclude.setText(TagConverter.asDbString(null, tags));
            return this;
        }

        /**
         * 5=best .. 1=worst or 0/null unknown
         */
        @Override
        public Integer getRating() {
            int numStars = (int) rating.getRating();
            return (numStars == 0) ? null : numStars;
        }

        @Override
        public IMetaApi setRating(Integer value) {
            rating.setRating((value == null) ? 0 : value.intValue());
            return this;
        }

        @Override
        public String toString() {
            return MediaUtil.toString(this);
        }
    };

    /** save exif changes back to image and database */
    private void onOk() {
        Activity ctx = this;
        Intent intent = getIntent();
        saveGuiToExif("onOk (finish)");
        mHistory.saveHistory();
        SelectedFiles items = getSelectedFiles("onOk ", getIntent(), true);
        long now = new Date().getTime();
        TransactionLogger logger = new TransactionLogger(ctx, now, null);

        if (!SYNC_UPDATE_EXIF) {
            this.exifUpdate = new UpdateTask(ctx,logger, mInitialData, mCurrentData, now);
            if (exifUpdate.isEmpty()) {
                finish();
                return;
            }
            exifUpdate.execute(items);
        } else {
            // for debugging: sync debugging is easier
            applyChangesSynchrounus(ctx, items, logger, mInitialData, mCurrentData, now);

            this.setResult(resultID, intent);
            finish();
        }
    }

    /** update exif changes in asynch task mit chow dialog */
    private static class UpdateTask extends AsyncTaskWithProgressDialog<SelectedFiles> {
        private MediaDiffCopy mediaDiffCopy;
        private TransactionLogger logger;
        private final long now;

        UpdateTask(Activity ctx, TransactionLogger logger, MediaAsString unmodifiedData, MediaAsString modifiedData, long now) {
            super(ctx, R.string.exif_menu_title);
            this.mediaDiffCopy = new MediaDiffCopy();
            this.logger = logger;
            this.now = now;
            if (this.mediaDiffCopy.setDiff(unmodifiedData, modifiedData) == null) {
                destroy();
            }
        }

        @Override
        protected Integer doInBackground(SelectedFiles... params) {
            publishProgress("...");
            if (mediaDiffCopy != null) {
                SelectedFiles items = params[0];
                int total = (items != null) ? items.size() : 0;
                int progressCountDown = 0;

                File file = null;
                for(int itemCount=0; itemCount < total; itemCount++) {
                    file = items.getFile(itemCount);
                    applyChanges(parent, file, items.getId(itemCount), mediaDiffCopy , logger);
                    progressCountDown--;
                    if (isCancelled()) {
                        break;
                    }
                    if (progressCountDown < 0) {
                        progressCountDown = 10;
                        publishProgress(itemCount, total, file);
                    }
                }
                publishProgress(total, total, file);
                mediaDiffCopy.fixTagRepository();

                return total;
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer itemCount) {
            if (Global.debugEnabled) {
                Log.d(Global.LOG_CONTEXT, mDebugPrefix + " onPostExecute " + itemCount);
            }
            Activity parent = this.parent;
            super.onPostExecute(itemCount);
            parent.setResult(resultID, parent.getIntent());
            parent.finish();
        }

        @Override
        public void destroy() {
            if (mediaDiffCopy != null) mediaDiffCopy.close();
            mediaDiffCopy = null;
            FileUtils.close(logger, mDebugPrefix);
            logger = null;
            super.destroy();
        }

        public boolean isEmpty() {
            return (mediaDiffCopy == null);
        }
    }


    private static void applyChangesSynchrounus(Activity ctx, SelectedFiles items,
                           TransactionLogger logger,
                           MediaAsString unmodifiedData, MediaAsString modifiedData, long now) {
        MediaDiffCopy mediaDiffCopy = new MediaDiffCopy().setDiff(unmodifiedData, modifiedData);

        if (mediaDiffCopy != null) {

            int size = (items != null) ? items.size() : 0;

            for(int i=0; i < size; i++) {
                applyChanges(ctx, items.getFile(i), items.getId(i), mediaDiffCopy , logger);
            }
            mediaDiffCopy.fixTagRepository();
            mediaDiffCopy.close();
        }

        FileUtils.close(logger, mDebugPrefix);
    }

    private static void applyChanges(Activity ctx,
                              File file, long id,
                              MediaDiffCopy mediaDiffCopy, TransactionLoggerBase logger) {
        if ((file != null) && file.exists()) {
            // save changes to jpg/xmp
            logger.set(id, file.getAbsolutePath());

            // change jpg/xmp + log changes
            MetaWriterExifXml exifFile = JpgMetaWorkflow.applyChanges(file, mediaDiffCopy, logger);

            // trigge jpg/xmp rescan to database
            if (exifFile != null) {
                MediaContentValues oldData = MediaScanner.getInstance(ctx).getExifFromFile(file);

                ContentValues dbValues = oldData.getContentValues();
                FotoSql.execUpdate(mDebugPrefix, ctx, file.getAbsolutePath(), dbValues, IGalleryFilter.VISIBILITY_PRIVATE_PUBLIC);

            }

        }
    }

    @Override
    protected void onDestroy() {
        // avoid memory leaks through cyclic dependencies
        HashTagEditWatcher.close(edTitle);
        HashTagEditWatcher.close(edDescription);

        if (exifUpdate != null)
            exifUpdate.destroy();
        exifUpdate = null;
        super.onDestroy();
    }
}

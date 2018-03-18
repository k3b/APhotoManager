/*
 * Copyright (c) 2017-2018 by k3b.
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
import android.content.Context;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
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
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.tagDB.TagsPickerFragment;
import de.k3b.android.util.AndroidFileCommands;
import de.k3b.android.util.IntentUtil;
import de.k3b.android.util.MediaScanner;
import de.k3b.android.util.ResourceUtils;
import de.k3b.android.widget.AboutDialogPreference;
import de.k3b.android.widget.ActivityWithAutoCloseDialogs;
import de.k3b.android.widget.UpdateTask;
import de.k3b.android.widget.HistoryEditText;
import de.k3b.io.VISIBILITY;
import de.k3b.io.collections.SelectedFiles;
import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.api.IGeoPointInfo;
import de.k3b.geo.io.GeoUri;
import de.k3b.io.DateUtil;
import de.k3b.io.GeoUtil;
import de.k3b.io.StringUtils;
import de.k3b.media.IMetaApi;
import de.k3b.media.MediaAsString;
import de.k3b.media.MediaDiffCopy;
import de.k3b.media.MediaUtil;
import de.k3b.tagDB.Tag;
import de.k3b.tagDB.TagConverter;

/**
 * Defines a gui to edit Exif content.
 * Invoke: see {@link #showActivity(Activity, IMetaApi, String, SelectedFiles, int, boolean)}.
 * Modes: if IMetaApi is not null edit exif data witout modifying any jpg file.
 * Modes: else if data-url/SelectedFiles is not null: modify the referenced jpg files.
 */
public class ExifEditActivity extends ActivityWithAutoCloseDialogs implements Common {
    private static final boolean SYNC_UPDATE_EXIF = false; // for sync debugging. false: asynch task
    private static final String mDebugPrefix = "ExifEdit-";
    private static final String DLG_NAVIGATOR_TAG = mDebugPrefix;

    public static final int EXIF_RESULT_ID = UpdateTask.EXIF_RESULT_ID;
    private static final int GEO_RESULT_ID = 572;

    public static final String EXTRA_EXIF_DATA = "de.k3b.extra.EXIF";

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

    // prevents event handling while programatically changes checkboxes
    private boolean checkActive = false;
    private CheckBox    chkPrivate;
    private CheckBox    chkPublic;

    private HistoryEditText mHistory;
    private UpdateTask exifUpdate;

    /**
     * @param context owhner activity starting this activity
     * @param exifDataToEdit if not null: edit value only via intent. donot update any files.
     *                       the content of exif that is edited.
     * @param url if not null url of jpg who-s exif will be modified or analysed.
     * @param selectedFiles if not null url of jpg who-s exif will be modified or analysed.
     * @param requestCode if not 0 request code for onActivityResult of calling activity
     *                    that receives the edit result.
     * @param saveChangesToUri false: edit value only via intent. donot update any files.
     *                         Neccessary to suport inital empy value of exifDataToEdit.
     */
    public static void showActivity(Activity context, IMetaApi exifDataToEdit, String url,
                                    SelectedFiles selectedFiles, int requestCode, boolean saveChangesToUri) {
        if (Global.debugEnabled) {
            Log.d(Global.LOG_CONTEXT, context.getClass().getSimpleName()
                    + " > ExifEditActivity.showActivity");
        }

        final Intent intent = new Intent().setClass(context,
                ExifEditActivity.class);

        intent.setAction(Intent.ACTION_EDIT);

        if (exifDataToEdit != null) {
            final String exifAsString = (exifDataToEdit instanceof MediaAsString)
                    ? exifDataToEdit.toString()
                    : new MediaAsString().setData(exifDataToEdit).toString();
            intent.putExtra(EXTRA_EXIF_DATA, exifAsString);
        } else if (!saveChangesToUri) {
            // special edit exif via intent without initial value
            intent.putExtra(EXTRA_EXIF_DATA, "");
        }
        if (url != null) {
            intent.setData(Uri.parse(url));
        }

        if (selectedFiles != null) {
            AffUtils.putSelectedFiles(intent, selectedFiles);
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

        chkPrivate = (CheckBox) findViewById(R.id.chk_private);
        chkPrivate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onCheckPrivateChanged(chkPrivate);
            }
        });
        chkPublic = (CheckBox) findViewById(R.id.chk_public);
        chkPublic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onCheckPrivateChanged(chkPublic);
            }
        });

        onCreateButtos();

        mCurrentData = new MediaAsString();
        mInitialData = new MediaAsString();

        if (savedInstanceState != null) {
            VISIBILITY priv = null;
            String data;
            data = savedInstanceState.getString(SETTINGS_KEY_INITIAL, null);
            if (data != null) {
                mInitialData.fromString(data);
                priv = mInitialData.getVisibility();
            }
            data = savedInstanceState.getString(SETTINGS_KEY, null);
            if (data != null) {
                mCurrentData.fromString(data);
                priv = mCurrentData.getVisibility();
            }
            mActivityData.setVisibility(priv);
        } else {
            Intent intent = getIntent();
            IMetaApi currentData = getExifParam(intent);

            if (null == currentData) {
                // there is no ExifParam : infer exif from jpg file
                SelectedFiles items = getSelectedFiles("onCreate ", this, intent, false);
                File first = (items != null) ? items.getFile(0) : null;

                if ((first != null) && (first.exists())) {
                    currentData = MediaScanner.getInstance(this).getExifFromFile(first);
                }
            }
            mCurrentData.setData(currentData);
            mInitialData.setData(currentData);

            final VISIBILITY priv = mActivityData.getVisibility();
            mCurrentData.setVisibility(priv);
            mInitialData.setVisibility(priv);
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

    public static IMetaApi getExifParam(Intent intent) {
        String exifAsString = intent.getStringExtra(EXTRA_EXIF_DATA);
        if (!StringUtils.isNullOrEmpty(exifAsString)) {
            return new MediaAsString().fromString(exifAsString);
        }
        return null;
    }

    private static SelectedFiles getSelectedFiles(String dbgContext, Context ctx, Intent intent, boolean mustLoadIDs) {
        if (intent == null) return null;

        SelectedFiles result = AffUtils.getSelectedFiles(intent);

        if (result == null) {
            String path = IntentUtil.getFilePath(ctx, IntentUtil.getUri(intent));
            String fileNames[] = SelectedFiles.getFileNameList(path);
            Long[] ids = null;
            int itemCount = (fileNames != null) ? fileNames.length : 0;

            if (itemCount > 0) {
                if ((mustLoadIDs) && (ids == null)) {
                    ids = new Long[itemCount];
                    Map<String, Long> idMap = FotoSql.execGetPathIdMap(ctx, fileNames);

                    for (int i = 0; i < itemCount; i++) {
                        ids[i] = idMap.get(fileNames[i]);
                    }
                }

                result = new SelectedFiles(fileNames, ids, null);
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
            MediaDiffCopy diff = new MediaDiffCopy(true, true);
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
            case R.id.cmd_clear:
                clearFilter();
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
            edDate.setText(DateUtil.toIsoDateTimeString(value));
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
        public VISIBILITY getVisibility() {
            if (chkPrivate.isChecked()) return VISIBILITY.PRIVATE;
            if (chkPublic.isChecked()) return VISIBILITY.PUBLIC;
            return null;
        }

        @Override
        public IMetaApi setVisibility(VISIBILITY priv) {
            checkActive = true;
            chkPrivate.setChecked(false);
            chkPublic.setChecked(false);
            if ((priv != null) && (priv != VISIBILITY.DEFAULT) && (priv != VISIBILITY.PRIVATE_PUBLIC)) {
                if (priv == VISIBILITY.PRIVATE) {
                    chkPrivate.setChecked(true);
                } else {
                    chkPublic.setChecked(true);
                }
            }
            checkActive = false;
            return this;
        }

        @Override
        public String toString() {
            return MediaUtil.toString(this);
        }
    };

    private void onCheckPrivateChanged(CheckBox chk) {
        if (!checkActive && (chk != null)) {
            VISIBILITY priv;
            if (!chk.isChecked()) {
                priv = null;
            } else if (chk == chkPrivate) {
                priv = VISIBILITY.PRIVATE;
            } else {
                priv = VISIBILITY.PUBLIC;
            }
            mActivityData.setVisibility(priv);
            mCurrentData.setVisibility(priv);
        }
    }

    private void clearFilter() {
        mCurrentData.clear();
        loadGuiFromExif("clearFilter");
    }

    /** save exif changes back to image and database */
    private void onOk() {
        Activity ctx = this;
        saveGuiToExif("onOk (finish)");
        mHistory.saveHistory();
        boolean finish = true;

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.getStringExtra(EXTRA_EXIF_DATA) != null) {
                Intent resultIntent = new Intent(Intent.ACTION_EDIT);
                resultIntent.putExtra(EXTRA_EXIF_DATA, mCurrentData.toString());
                setResult(EXIF_RESULT_ID, resultIntent);
            } else {
                // modify jpg files and return
                SelectedFiles items = getSelectedFiles("onOk ", this, getIntent(), true);

                if ((items != null) && (items.size() > 0)) {

                    //!!! todo #93: this code also in
                    AndroidFileCommands cmd = AndroidFileCommands.createFileCommand(this, true);

                    MediaDiffCopy exifChanges = new MediaDiffCopy(true, true).setDiff(mInitialData, mCurrentData);

                    if (exifChanges != null) {
                        if (!SYNC_UPDATE_EXIF) {
                            this.exifUpdate = new UpdateTask(ctx, cmd, exifChanges);
                            exifUpdate.execute(items);
                            finish = false;
                        } else {
                            // for debugging: sync debugging is easier
                            cmd.applyExifChanges(true, exifChanges, items, null);

                            this.setResult(EXIF_RESULT_ID, intent);
                        }
                    } // else  no changes, nothing to do
                } // if there are selected items
            } // if save mode
        } // if intent exists

        if (finish) {
            finish();
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

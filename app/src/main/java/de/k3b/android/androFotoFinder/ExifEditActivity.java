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
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import de.k3b.android.androFotoFinder.media.MediaContentValues;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.tagDB.TagsPickerFragment;
import de.k3b.android.util.IntentUtil;
import de.k3b.android.util.MediaScanner;
import de.k3b.android.widget.AboutDialogPreference;
import de.k3b.android.widget.ActivityWithAutoCloseDialogs;
import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.api.IGeoPointInfo;
import de.k3b.geo.io.GeoUri;
import de.k3b.io.DateUtil;
import de.k3b.io.FileUtils;
import de.k3b.io.GeoUtil;
import de.k3b.io.IGalleryFilter;
import de.k3b.media.IMetaApi;
import de.k3b.media.JpgMetaWorkflow;
import de.k3b.media.MediaAsString;
import de.k3b.media.MediaUtil;
import de.k3b.tagDB.Tag;
import de.k3b.tagDB.TagConverter;

/**
 * Defines a gui for global foto filter: only fotos from certain filepath, date and/or lat/lon will be visible.
 */
public class ExifEditActivity extends ActivityWithAutoCloseDialogs implements Common {
    private static final String mDebugPrefix = "ExifEdit-";
    private static final String DLG_NAVIGATOR_TAG = mDebugPrefix;

    public static final int resultID = 522;
    private static final int GEO_RESULT_ID = 572;

    private static final String SETTINGS_KEY = "ExifEditActivity-";
    private static final GeoUri PARSER = new GeoUri(GeoUri.OPT_PARSE_INFER_MISSING);

    private MediaAsString  mData;
    private EditText edTitle;
    private EditText edDescription;
    private EditText edDate;
    private EditText edTagsInclude;
    private EditText edTagsExclude;
    private EditText edLatitude;
    private EditText edLongitude;
    private RatingBar   rating;


    public static void showActivity(Activity context, String url, int requestCode) {
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

        if (requestCode != 0) {
            context.startActivityForResult(intent, requestCode);
        } else {
            context.startActivity(intent);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        saveGuiToExif();
        String guiDataAsString = mData.toString();
        if (Global.debugEnabled){
            Log.d(Global.LOG_CONTEXT, mDebugPrefix + "onSaveInstanceState " +
                    SETTINGS_KEY + "=" + guiDataAsString);
        }
        savedInstanceState.putString(SETTINGS_KEY, guiDataAsString);
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
        edTagsExclude   = (EditText) findViewById(R.id.edit_tags_exclude);
        edLatitude      = (EditText) findViewById(R.id.edit_latitude);
        edLongitude     = (EditText) findViewById(R.id.edit_longitude);
        rating = (RatingBar)  findViewById(R.id.ratingBar);

        onCreateButtos();

        mData = new MediaAsString();

        if (savedInstanceState != null) {
            String data = savedInstanceState.getString(SETTINGS_KEY, null);
            if (data != null) {
                mData.setData(data);
                if (Global.debugEnabled){
                    Log.d(Global.LOG_CONTEXT, mDebugPrefix + "onCreate " +
                            SETTINGS_KEY + "=" + data);
                }
            }

        } else {
            Intent intent = getIntent();
            String path = IntentUtil.getFilePath(this, IntentUtil.getUri(intent));
            if (path != null) {
                File file = new File(path);
                if (file.exists()) {
                    IMetaApi currentData = MediaScanner.getInstance(this).getExifFromFile(file);
                    mData.setData(currentData);
                }
            }

            if (Global.debugEnabled && (intent != null)){
                Log.d(Global.LOG_CONTEXT, mDebugPrefix + "onCreate " + intent.toUri(Intent.URI_INTENT_SCHEME));
            }
        }

        // load gui from exif
        loadGuiFromExif();
    }

    private void loadGuiFromExif() {
        MediaUtil.copy(mActivityData, mData, true, true);
    }

    private void saveGuiToExif() {
        MediaUtil.copy(mData, mActivityData, true, true);
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
                mTagsApi.showTagPicker(R.string.tags_activity_title);
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
        GeoPointDto geo = new GeoPointDto();

        geo.setLatitude(mData.getLatitude());
        geo.setLongitude(mData.getLongitude());

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
        if (geo != null) {
            mData.setLatitude(geo.getLatitude()).setLongitude(geo.getLongitude());
            loadGuiFromExif();
        }
    }

    private final DateTimeApi mDateTimeApi = new DateTimeApi();

    private class DateTimeApi
            implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {
        public void showTimePicker() {
            saveGuiToExif();
            final Calendar c = getDateTimeTakenAsCalendar();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            Dialog dlg1 = new TimePickerDialog(ExifEditActivity.this, mDateTimeApi, hour, minute,
                    DateFormat.is24HourFormat(ExifEditActivity.this));
            dlg1.show();
            setAutoClose(null, dlg1);
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            final Calendar c = getDateTimeTakenAsCalendar();
            c.set(Calendar.HOUR_OF_DAY, hourOfDay);
            c.set(Calendar.MINUTE, minute);
            mData.setDateTimeTaken(c.getTime());
            loadGuiFromExif();
        }

        public void showDatePicker() {
            saveGuiToExif();
            final Calendar c = getDateTimeTakenAsCalendar();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog dlg1 = new DatePickerDialog(ExifEditActivity.this, mDateTimeApi,
                    year, month, day);
            dlg1.show();
            setAutoClose(null, dlg1);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            final Calendar c = getDateTimeTakenAsCalendar();
            c.set(Calendar.YEAR, year);
            c.set(Calendar.MONTH, month);
            c.set(Calendar.DAY_OF_MONTH, day);
            mData.setDateTimeTaken(c.getTime());
            loadGuiFromExif();
        }

        private Calendar getDateTimeTakenAsCalendar() {
            final Calendar c = Calendar.getInstance();
            c.setTimeZone(TimeZone.getTimeZone("gmt"));
            Date dateTimeTaken = mData.getDateTimeTaken();
            if (dateTimeTaken != null) {
                c.setTimeInMillis(dateTimeTaken.getTime());
            }
            return c;
        }
    }

    private final TagsApi mTagsApi = new TagsApi();
    private class TagsApi implements TagsPickerFragment.ITagsPicker {
        private void showTagPicker(int idTitle) {
            saveGuiToExif();
            final FragmentManager manager = getFragmentManager();
            TagsPickerFragment dlg = new TagsPickerFragment();
            dlg.setFragmentOnwner(mTagsApi);
            dlg.setTitleId(idTitle);
            // dlg.setAddNames(mFilter.getTagsAllIncluded());
            dlg.setAddNames(mData.getTags());
            dlg.setAffectedNames(mData.getTags());
            dlg.show(manager, DLG_NAVIGATOR_TAG);
            setAutoClose(dlg, null);
        }

        /**
         * called by {@link TagsPickerFragment}
         */
        @Override
        public boolean onCancel(String msg) {
            setAutoClose(null, null);
            return true;
        }

        /**
         * called by {@link TagsPickerFragment}
         */
        @Override
        public boolean onOk(List<String> addNames, List<String> removeNames) {
            mData.setTags(addNames);
            loadGuiFromExif();
            setAutoClose(null, null);
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

        /**
         * Latitude, in degrees north. (-90 .. +90)
         *
         * @param latitude
         */
        @Override
        public IMetaApi setLatitude(Double latitude) {
            edLatitude.setText(GeoUtil.toCsvStringLatLon(latitude));
            return this;
        }

        /**
         * Longitude, in degrees east.  (-180 .. + 180)
         *
         * @param longitude
         */
        @Override
        public IMetaApi setLongitude(Double longitude) {
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
            return edTitle.getText().toString();
        }

        /**
         * Longer description = comment. may have more than one line
         */
        @Override
        public String getDescription() {
            return edDescription.getText().toString();
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
            int numStars = rating.getNumStars();
            return (numStars == 0) ? null : numStars;
        }

        @Override
        public IMetaApi setRating(Integer value) {
            rating.setNumStars((value == null) ? 0 : value.intValue());
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
        saveGuiToExif();
        Intent intent = getIntent();
        String path = IntentUtil.getFilePath(this, IntentUtil.getUri(intent));
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                MediaContentValues oldData = MediaScanner.getInstance(ctx).getExifFromFile(file);
                List<MediaUtil.FieldID> differences = MediaUtil.getChanges(oldData, mData);

                if (differences != null) {
                    List<String> oldTags = oldData.getTags();
                    EnumSet<MediaUtil.FieldID> diffSet = EnumSet.copyOf(differences);
                    diffSet.remove(MediaUtil.FieldID.path);

                    // save changes to jpg/xmp
                    JpgMetaWorkflow.applyChanges(file, mData, diffSet);

                    // save changes to database
                    int changes = MediaUtil.copySpecificProperties(oldData, mData, diffSet);
                    if (changes > 0) {
                        ContentValues dbValues = oldData.getContentValues();
                        FotoSql.execUpdate(mDebugPrefix, ctx, path, dbValues, IGalleryFilter.VISIBILITY_PRIVATE_PUBLIC);
                    }

                    Map<String, Long> idMap = FotoSql.execGetPathIdMap(ctx, path);
                    long now = new Date().getTime();
                    long id = (idMap != null) ? idMap.get(path) : 0;

                    TransactionLogger logger = new TransactionLogger(ctx, id, path, now, null);
                    logger.addChanges(mData,diffSet, oldTags);
                    FileUtils.close(logger,mDebugPrefix);
                    // if (diffSet.contains(MediaUtil.FieldID.dateTimeTaken))
                    // save changes to log
                }
            }
        }

        this.setResult(resultID, intent);
        finish();
    }


}

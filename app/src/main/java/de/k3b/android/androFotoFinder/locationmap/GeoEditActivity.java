/*
 * Copyright (c) 2015-2018 by k3b.
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
 
package de.k3b.android.androFotoFinder.locationmap;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

import de.k3b.android.androFotoFinder.AffUtils;
import de.k3b.android.androFotoFinder.Common;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.util.AndroidFileCommands;
import de.k3b.android.util.MediaScanner;
import de.k3b.android.widget.HistoryEditText;
import de.k3b.android.widget.LocalizedActivity;
import de.k3b.io.collections.SelectedFiles;
import de.k3b.io.collections.SelectedItems;
import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.api.IGeoPointInfo;
import de.k3b.geo.io.GeoPickHistory;
import de.k3b.geo.io.GeoUri;
import de.k3b.io.DirectoryFormatter;

/**
 * Defines a gui for global foto filter: only fotos from certain filepath, date and/or lat/lon will be visible.
 */
public class GeoEditActivity extends LocalizedActivity implements Common  {
    private static final String mDebugPrefix = "GeoEdit-";

    public static final int RESULT_ID = 524;
    private static final String SETTINGS_KEY_LAST_URI = "GeoEditActivity-";
    private static final GeoUri PARSER = new GeoUri(GeoUri.OPT_PARSE_INFER_MISSING);

    private EditText mLatitudeFrom;
    private EditText mLongitudeFrom;
    private String mCurrentId = null; // belonging to current lat/long

    private HistoryEditText mHistory;
    private SelectedFiles mSelectedItems;

    private GeoPointDto mCurrentPoint = new GeoPointDto();
    private Button cmdOk;
    private Button cmdCancel;

    /** != -null will setGeo-asynctask is running. null if activity is destroyed so async-task does not update gui any more */
    private ProgressBar mProgressBar = null;
    private TextView mLblStatusMessage;

    public static void showActivity(Activity context, SelectedFiles selectedFiles, int requestCode) {
        Uri initalUri = null;
        final Intent intent = new Intent().setClass(context,
                GeoEditActivity.class);

        if (AffUtils.putSelectedFiles(intent, selectedFiles)) {
            Long id = selectedFiles.getId(0);
            IGeoPointInfo initialPoint = MediaScanner.getInstance(context).getPositionFromFile(selectedFiles.getFileNames()[0], (id != null) ? id.toString() : null);
            if (initialPoint != null) {
                initalUri = Uri.parse(PARSER.toUriString(initialPoint));
                intent.setData(initalUri);
            }
        }

        if (Global.debugEnabled) {
            Log.d(Global.LOG_CONTEXT, context.getClass().getSimpleName()
                    + " > GeoEditActivity.showActivity@" + initalUri);
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
        setContentView(R.layout.activity_geo_edit);
        onCreateButtos();

        final Intent intent = this.getIntent();
        if (Global.debugEnabled && (intent != null)){
            Log.d(Global.LOG_CONTEXT, mDebugPrefix + "onCreate " + intent.toUri(Intent.URI_INTENT_SCHEME));
        }
        SelectedFiles selectedItems = AffUtils.getSelectedFiles(intent);
        if (selectedItems != null) {
            mSelectedItems = selectedItems;

            String title = getString(R.string.geo_edit_menu_title) + " (" + selectedItems.size() + ")";
            setTitle(title);
        }

        GeoPointDto currentPoint = getLastGeo();
        if (currentPoint != null) {
            toGui(currentPoint);
        }

        if (Global.geoNoEdit) {
            showLatLonPicker(fromGui());
        }
    }

    private void onCreateButtos() {
        this.mLatitudeFrom = (EditText) findViewById(R.id.edit_latitude_from);
        this.mLongitudeFrom = (EditText) findViewById(R.id.edit_longitude_from);
        mHistory = new HistoryEditText(GeoEditActivity.this, new int[] {
                R.id.cmd_lat_from_history, R.id.cmd_lon_from_history} ,
                mLatitudeFrom, mLongitudeFrom);

        Button cmd = (Button) findViewById(R.id.cmd_select_lat_lon);
        cmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//              showDirectoryPicker(FotoSql.queryGroupByPlace);
                showLatLonPicker(fromGui());
            }
        });

        this.cmdOk = (Button) findViewById(R.id.cmd_ok);
        cmdOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOk();
            }
        });
        this.cmdCancel = (Button) findViewById(R.id.cmd_cancel);
        cmdCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_NOCHANGE);
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        String geoUri = fromGui();
        saveLastGeo(geoUri);
        mHistory.saveHistory();
        /** indicate that activity is destroyed so async-task does not update gui any more */
        mProgressBar = null;

        super.onDestroy();
    }

    private String fromGui() {
        try {
            mCurrentPoint.setLatitude(getLatitude());
            mCurrentPoint.setLongitude(getLogitued());
            mCurrentPoint.setId(mCurrentId);
            return PARSER.toUriString(mCurrentPoint);
        } catch (RuntimeException ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
            return null;
        }
    }

    public double getLatitude() {
        return convertLL(mLatitudeFrom.getText().toString());
    }

    public double getLogitued() {
        return convertLL(mLongitudeFrom.getText().toString());
    }


    @Nullable
    private GeoPointDto getLastGeo() {
        Uri initalUri = getIntent().getData();
        if (initalUri != null) {
            return parseGeo(initalUri.toString());
        }

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPref != null) {
            String uriAsString = sharedPref.getString(SETTINGS_KEY_LAST_URI, "geo:51,9?z=4");
            return parseGeo(uriAsString);
        }
        return null;
    }

    @Nullable
    private void saveLastGeo(String geoUri) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = sharedPref.edit();

        edit.putString(SETTINGS_KEY_LAST_URI, geoUri);

        edit.apply();
    }

    private GeoPointDto parseGeo(String uriAsString) {
        if (uriAsString != null) {
            mCurrentPoint.clear();
            GeoUri parser = new GeoUri(GeoUri.OPT_PARSE_INFER_MISSING);
            return parser.fromUri(uriAsString, mCurrentPoint);
        }
        return null;
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_gallery_filter, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (item.getItemId()) {
            case R.id.cmd_about:
                AboutDialogPreference.createAboutDialog(this).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
*/

    private void toGui(GeoPointDto src) {
        mLongitudeFrom  .setText(convertLL(src.getLongitude()));
        mLatitudeFrom   .setText(convertLL(src.getLatitude()));
        mCurrentId  = src.getId();
    }

    /************* local helper *****************/
    private String convertLL(double latLon) {
        if (Double.isNaN(latLon)) return "";
        return DirectoryFormatter.formatLatLon(latLon);
    }

    private double convertLL(String string) throws RuntimeException {
        if ((string == null) || (string.length() == 0)) {
            return Double.NaN;
        }

        try {
            return Double.parseDouble(string);
        } catch (Exception ex) {
            throw new RuntimeException(getString(R.string.filter_err_invalid_location_format, string), ex);
        }
    }

    /** opens lat/lon picker */
    private void showLatLonPicker(String geoUri) {
        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        final Uri parseUri = Uri.parse(geoUri);
        intent.setData(parseUri);
        intent.putExtra(EXTRA_TITLE, getString(R.string.geo_picker_title));

        SelectedItems calculatedSelectedFotosIds = new SelectedItems();

        GeoPickHistory history = getHistory();
        if (history != null) {
            history.addKeysTo(calculatedSelectedFotosIds);
        }

        if (mSelectedItems != null) {
            calculatedSelectedFotosIds.addAll(Arrays.asList(mSelectedItems.getIds()));
        }

        AffUtils.putSelectedFiles(intent, AffUtils.querySelectedFiles(this, calculatedSelectedFotosIds));
        try {
            // #7: allow choosing geo pick from map or from "photo with geo"
            this.startActivityForResult(Intent.createChooser(intent, this.getString(R.string.geo_edit_menu_title)), GeoEditActivity.RESULT_ID);
            // this.startActivityForResult(intent, RESULT_ID);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, R.string.geo_picker_err_not_found,Toast.LENGTH_LONG).show();
        }
    }

    private static GeoPickHistory getHistory() {
        if (Global.pickHistoryFile != null) {
            GeoPickHistory history = new GeoPickHistory(Global.pickHistoryFile, Global.pickHistoryMax);
            history.load();
            return history;
        }
        // null if disabled
        return null;
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
            case RESULT_ID:
                onGeoChanged((intent != null) ? intent.getData() : null);
                break;
            default:break;
        }
    }

    private void onGeoChanged(Uri data) {
        if (data != null) {
            toGui(parseGeo(data.toString()));
        }
        if (Global.geoNoEdit) {
            if (data != null) {
                onOk();
            } else {
                finish();
            }
        }
    }

    private void onOk() {
        double latitude = getLatitude();
        double longitude = getLogitued();
        mHistory.saveHistory();
        if (!Double.isNaN(latitude) && !Double.isNaN(longitude)) {
            setGeo(latitude, longitude, mCurrentId, mSelectedItems);
        }
    }

    private void setGeo(final double latitude, final double longitude, String id, final SelectedFiles selectedItems) {
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        cmdOk.setVisibility(View.INVISIBLE);
        cmdCancel.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressBar.setMax(selectedItems.size() + 1);

        mLblStatusMessage = ((TextView) findViewById(R.id.lbl_status));
        mLblStatusMessage.setText(R.string.geo_edit_update_in_progress);

        GeoPickHistory history = getHistory();
        if (history != null) {
            history.add((id != null) ? Long.parseLong(id) : 0, latitude, longitude);
            history.save();
        }

        /** encapsulate geo-job into async background task */
        AsyncTask<Object, Integer, Integer> task = new AsyncTask<Object, Integer, Integer>() {
            private AndroidFileCommands engine;
            @Override protected void onPreExecute() {
                engine = new AndroidFileCommands() {
                    /** map AndroidFileCommands-progress to AsyncTask-progress */
                    @Override public boolean onProgress(int itemcount, int size, String message) {
                        publishProgress(itemcount, size);
                        return !isCancelled();
                    }
                };
                engine.setContext(GeoEditActivity.this);
            }

            @Override protected Integer doInBackground(Object... params) {
                engine.setLogFilePath(engine.getDefaultLogFile());
                int itemcount = engine.setGeo(latitude, longitude, selectedItems, 10);
                engine.setLogFilePath(null);
                return itemcount;
            }

            @Override protected void onProgressUpdate(Integer... values) {
                if (mProgressBar != null) mProgressBar.setProgress(values[0]);
                super.onProgressUpdate(values);
            }

            @Override protected void onPostExecute(Integer result) {
                if (mProgressBar != null) {
                    // gui is not destoyed yet
                    if ((result != null) && (result.intValue() > 0)) {
                        String message = getString(R.string.image_success_update_format, result.intValue());
                        Toast.makeText(GeoEditActivity.this, message, Toast.LENGTH_LONG).show();
                        setResult(RESULT_CHANGE);
                        finish();
                    }
                    cmdOk.setVisibility(View.VISIBLE);
                    cmdCancel.setVisibility(View.VISIBLE);
                    mProgressBar.setVisibility(View.INVISIBLE);
                    mLblStatusMessage.setText("");
                }
            }
        };

        task.execute();
    }
}

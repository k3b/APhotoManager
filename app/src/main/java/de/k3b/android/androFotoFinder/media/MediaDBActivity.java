/*
 * Copyright (c) 2016-2018 by k3b.
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

package de.k3b.android.androFotoFinder.media;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Date;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.tagDB.TagSql;
import de.k3b.android.util.AndroidFileCommands;
import de.k3b.android.util.IntentUtil;
import de.k3b.csv2db.csv.CsvLoader;
import de.k3b.io.FileUtils;
import de.k3b.io.VISIBILITY;
import de.k3b.media.MediaCsvItem;
import de.k3b.media.MediaUtil;
import de.k3b.tagDB.Tag;
import de.k3b.tagDB.TagRepository;

public class MediaDBActivity extends Activity {
    public static final String DBG_CONTEXT = "MediaDB-Import(Csv): ";

    /** Does the import into MediaDB in background task */
    private AsyncTask<Uri,String,String> mTask = null;
    private TextView mStatus;
    private TextView mFolder;

    @Override protected void onDestroy() {
        if (mTask != null) {
            // cancel background import if active
            mTask.cancel(false);
            mTask = null;
        }
        super.onDestroy();
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Uri uri = getUri();
        if (uri == null) {
            finish();
        } else {
            setContentView(R.layout.activity_media_db);
            setTitle(R.string.scanner_menu_title);
            mFolder = (TextView) findViewById(R.id.folder);
            mStatus = (TextView) findViewById(R.id.status);

            mFolder.setText(uri.toString());
            mStatus.setText("");
            ((Button) findViewById(R.id.cmd_cancel)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mTask != null) mTask.cancel(false);
                    finish();
                }
            });

            ((Button) findViewById(R.id.cmd_ok)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startScanner(uri);
                }
            });
        }
    }

    private void startScanner(Uri uri) {
        if ((AndroidFileCommands.canProcessFile(this, false)) && (uri != null)) {
            mTask = new TaskLoadMediaDB();
            mTask.execute(uri);

        }
    }

    private Uri getUri() {
        final String action = getIntent().getAction();
        if(Intent.ACTION_VIEW.equals(action)) {
            return IntentUtil.getUri(getIntent());
        }
        return null;
    }

    /** Does the import into MediaDB in background task */
    private class TaskLoadMediaDB extends AsyncTask<Uri,String,String> {
        private int mItemCount = 0;
        private int mUpdateCount = 0;
        private int mProgressCountDown = 0;
        protected MediaCsvLoader mLoader = null;

        /**
         * Override this method to perform a computation on a background thread. The
         * specified parameters are the parameters passed to {@link #execute}
         * by the caller of this task.
         * <p>
         * This method can call {@link #publishProgress} to publish updates
         * on the UI thread.
         *
         * @param params The parameters of the task.
         * @return A result, defined by the subclass of this task.
         * @see #onPreExecute()
         * @see #onPostExecute
         * @see #publishProgress
         */
        @Override
        protected String doInBackground(Uri... params) {
            return processUri(params[0]);
        }

        protected String processUri(Uri uri) {
            File file = IntentUtil.getFile(uri);
            if (file != null) {
                Reader reader = null;
                try {
                    File csvRootDir = FileUtils.tryGetCanonicalFile(file.getParentFile(), null);
                    if ((csvRootDir != null) && file.getName().endsWith(".csv")) {
                        Log.i(Global.LOG_CONTEXT, DBG_CONTEXT + "start form " + uri.toString());

                        reader = new InputStreamReader(getContentResolver().openInputStream(uri));
                        mLoader = new MediaCsvLoader(csvRootDir);

                        mLoader.load(reader, new MediaCsvItem());

                        if (Global.Media.enableXmpNone) {
                            // set all xmp-file-dates to EXT_LAST_EXT_SCAN_NO_XMP_IN_CSV if null
                            mProgressCountDown = 10;
                            publishProgress(getString(R.string.scanner_menu_title) + " (" + mItemCount + ", +" + mUpdateCount + ") " + uri.toString());

                            updateDB("set all xmp-file-dates to EXT_LAST_EXT_SCAN_NO_XMP_IN_CSV", csvRootDir.getAbsolutePath() + "/%", TagSql.EXT_LAST_EXT_SCAN_NO_XMP_IN_CSV, new ContentValues());
                        }
                    }
                    mLoader = null;
                    if (reader != null) reader.close();
                } catch (Exception e) {
                    String msg = DBG_CONTEXT +
                            "[" + e.getClass().getSimpleName() +
                            "]: Error processing " + uri + ": " + e.getMessage();
                    Log.e(Global.LOG_CONTEXT, msg, e);
                    mLoader = null;
                    mTask = null;
                    return msg;
                }
            }
            mTask = null;
            String message = getString(R.string.image_success_update_format, mUpdateCount);
            Log.i(Global.LOG_CONTEXT, DBG_CONTEXT + uri.toString() + ": " + message);

            return message;
        }

        private void updateDB(String dbgContext, String _path, long xmlLastFileModifyDate, ContentValues dbValues) {
            String path = _path;
            if (path != null) {
                if (!path.contains("%")) {
                    if (MediaUtil.isImage(path, MediaUtil.IMG_TYPE_ALL)) {
                        // non xmp-file: do not update file modify date
                        xmlLastFileModifyDate = (Global.Media.enableXmpNone)
                                ? TagSql.EXT_LAST_EXT_SCAN_NO_XMP_IN_CSV
                                : TagSql.EXT_LAST_EXT_SCAN_UNKNOWN;
                    } else {
                        // xmp-file: find path without extension
                        path = FileUtils.replaceExtension(path, ".%");
                    }
                }
                if (--mProgressCountDown <= 0) {
                    mProgressCountDown = 10;
                    publishProgress("(" + mItemCount + ", +" + mUpdateCount + ") " + path);
                    if (isCancelled() && (mLoader != null)) {
                        mLoader.cancel();
                    }
                }

                if (xmlLastFileModifyDate != TagSql.EXT_LAST_EXT_SCAN_UNKNOWN) {
                    TagSql.setXmpFileModifyDate(dbValues, xmlLastFileModifyDate);
                }

                TagSql.setFileModifyDate(dbValues, new Date().getTime() / 1000);

                mUpdateCount += TagSql.execUpdate(dbgContext, MediaDBActivity.this, path, xmlLastFileModifyDate, dbValues, VISIBILITY.PRIVATE_PUBLIC);
                mItemCount++;
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            mStatus.setText(values[0]);
        }

        @Override
        protected void onPostExecute(String errorMessage) {
            if (Global.Media.enableIptcMediaScanner) {
                TagRepository.getInstance().save();
            }

            if (!isCancelled()) {
                if (errorMessage != null) {
                    Toast.makeText(MediaDBActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
                finish();
            }
        }

        private class MediaCsvLoader extends CsvLoader<MediaCsvItem> {
            /**
             * path-s in csv are relative to mCsvRootDir
             */
            private final File mCsvRootDir;

            private ContentValues mDbValues = new ContentValues();
            private MediaContentValues mMediaValueAdapter = new MediaContentValues();
            private Tag mImportRoot = null;

            public MediaCsvLoader(File csvRootDir) {
                mCsvRootDir = csvRootDir;
            }

            @Override
            protected void onNextItem(MediaCsvItem next, int lineNumber, int recordNumber) {
                if (next != null) {
                    String path = next.getPath();
                    next.setPath(null);
                    mDbValues.clear();
                    if ((path != null) && (MediaUtil.copy(mMediaValueAdapter.set(mDbValues, null), next, false, false) >= 1)) {
                        // >= 1 means at least one extra attribute (excluding path that has been set to null)
                        Date fileModifyDate = next.getFileModifyDate();
                        long fileModifyDateMilliSecs = (fileModifyDate != null) ? fileModifyDate.getTime() : TagSql.EXT_LAST_EXT_SCAN_NO_XMP_IN_CSV;

                        String canonicalPath = FileUtils.tryGetCanonicalPath(new File(mCsvRootDir, path), null);
                        updateDB("MediaCsvLoader.onNextItem", canonicalPath, fileModifyDateMilliSecs, mDbValues);
                        TagRepository.getInstance().includeTagNamesIfNotFound(mMediaValueAdapter.getTags());
                    }
                }
            }

            /** get or create parent-tag where alle imports are appendend as children */
            public Tag getImportRoot() {
                if (mImportRoot == null) {
                    mImportRoot = TagRepository.getInstance().getImportRoot();
                }
                return mImportRoot;
            }
        }
    }


}

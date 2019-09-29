/*
 * Copyright (c) 2019 by k3b.
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
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.io.IProgessListener;
import de.k3b.zip.IZipConfig;
import de.k3b.zip.LibZipGlobal;
import de.k3b.zip.ProgressFormatter;
import de.k3b.zip.ZipConfigDto;
import de.k3b.zip.ZipStorage;

class ProgressData {
    final int itemcount;
    final int size;
    final String message;

    ProgressData(int itemcount, int size, String message) {
        this.itemcount = itemcount;
        this.size = size;
        this.message = message;
    }
}

public class BackupAsyncTask extends AsyncTask<Object, ProgressData, IZipConfig> implements IProgessListener {

    private final Backup2ZipService service;
    private Activity activity;
    private ProgressBar mProgressBar = null;
    private TextView status;
    private AtomicBoolean isActive = new AtomicBoolean(true);

    private final ProgressFormatter formatter;
    // last known number of items to be processed
    private int lastSize = 0;

    public BackupAsyncTask(Context context, ZipConfigDto mZipConfigData, ZipStorage zipStorage,
                           Date backupDate) {
        this.service = new Backup2ZipService(context.getApplicationContext(),
                mZipConfigData, zipStorage, backupDate);

        formatter = new ProgressFormatter();
    }

    public void setContext(Activity activity, ProgressBar progressBar, TextView status) {
        this.activity = activity;
        mProgressBar = progressBar;
        this.status = status;
        service.setProgessListener((progressBar != null) ? this : null);
    }

    @Override
    protected IZipConfig doInBackground(Object... voids) {
        try {
            return this.service.execute();
        } finally {
            this.isActive.set(false);
        }
    }

    /** called on success */
    @Override
    protected void onPostExecute(IZipConfig iZipConfig) {
        super.onPostExecute(iZipConfig);
        if (activity != null) {
            activity.setResult(Activity.RESULT_OK);
            activity.finish();

            Toast.makeText(activity, iZipConfig.toString(), Toast.LENGTH_LONG).show();

            if (LibZipGlobal.debugEnabled || Global.debugEnabled) {
                Log.d(LibZipGlobal.LOG_TAG, activity.getClass().getSimpleName() + ": " +
                        formatter.format(lastSize, lastSize)
                );
                Log.d(LibZipGlobal.LOG_TAG, activity.getClass().getSimpleName() + ": " +
                        activity.getText(R.string.backup_title) + " " +
                        iZipConfig.toString()
                );
            }

            setContext(null, null, null);
        }
    }

    /** called on error */
    @Override
    protected void onCancelled() {
        if (activity != null) {
            Toast.makeText(activity, activity.getText(android.R.string.cancel), Toast.LENGTH_LONG).show();

            if (LibZipGlobal.debugEnabled || Global.debugEnabled) {
                Log.d(LibZipGlobal.LOG_TAG, activity.getClass().getSimpleName() + ": " + activity.getText(android.R.string.cancel));
            }

            setContext(null, null, null);
        }
    }

    public static boolean isActive(BackupAsyncTask backupAsyncTask) {
        return (backupAsyncTask != null) && (backupAsyncTask.isActive.get());
    }

    /**
     * de.k3b.io.IProgessListener:
     *
     * called every time when command makes some little progress in non gui thread.
     * return true to continue
     */
    @Override
    public boolean onProgress(int itemcount, int size, String message) {
        publishProgress(new ProgressData(itemcount, size, message));
        return !this.isCancelled();
    }

    /** called from {@link AsyncTask} in gui task */
    @Override
    protected void onProgressUpdate(ProgressData... values) {
        final ProgressData progressData = values[0];
        if (mProgressBar != null) {
            int size = progressData.size;
            if ((size != 0) && (size > lastSize)) {
                mProgressBar.setMax(size);
                lastSize = size;
            }
            mProgressBar.setProgress(progressData.itemcount);
        }
        if (this.status != null) {
            this.status.setText(formatter.format(progressData.itemcount, progressData.size));
            //  values[0].message);
        }
    }
}

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
package de.k3b.android.widget;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.concurrent.atomic.AtomicBoolean;

import de.k3b.android.androFotoFinder.Common;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.io.IProgessListener;
import de.k3b.zip.LibZipGlobal;
import de.k3b.zip.ProgressFormatter;

public abstract class ProgressableAsyncTask<RESULT> extends AsyncTask<Object, ProgressData, RESULT> implements IProgessListener {
    protected final ProgressFormatter formatter = new ProgressFormatter();
    private final String mDebugPrefix = "ProgressableAsyncTask ";
    protected Activity activity;
    protected AtomicBoolean isActive = new AtomicBoolean(true);
    // last known number of items to be processed
    protected int lastSize = 0;
    private ProgressBar mProgressBar = null;
    private TextView status;

    public static boolean isActive(ProgressableAsyncTask backupAsyncTask) {
        return (backupAsyncTask != null) && (backupAsyncTask.isActive.get());
    }

    /**
     * (Re-)Attach owning Activity to BackupAsyncTask
     * (i.e. after Device rotation
     *
     * @param why
     * @param activity    new owner
     * @param progressBar To be updated while compression task is running
     * @param status      To be updated while compression task is running
     */
    public void setContext(String why, Activity activity, ProgressBar progressBar, TextView status) {
        if (LibZipGlobal.debugEnabled) {
            Log.d(LibZipGlobal.LOG_TAG, mDebugPrefix + why + " setContext " + activity);
        }
        this.activity = activity;
        mProgressBar = progressBar;
        this.status = status;
    }

    /**
     * called on error
     */
    @Override
    protected void onCancelled() {
        if (activity != null) {
            if (LibZipGlobal.debugEnabled || Global.debugEnabled) {
                Log.d(LibZipGlobal.LOG_TAG, activity.getClass().getSimpleName() + ": " + activity.getText(android.R.string.cancel));
            }

            finish(mDebugPrefix + " onCancelled ", Activity.RESULT_CANCELED, activity.getText(android.R.string.cancel));
        }
    }

    protected void finish(String why, int resultCode, CharSequence message) {
        if (message != null) {
            Intent intent = new Intent();
            intent.putExtra(Common.EXTRA_TITLE, message);
            activity.setResult(resultCode, intent);
        } else {
            activity.setResult(resultCode);
        }
        activity.finish();

        setContext(why + mDebugPrefix + " finish ", null, null, null);

    }

    /**
     * de.k3b.io.IProgessListener:
     * <p>
     * called every time when command makes some little progress in non gui thread.
     * return true to continue
     */
    @Override
    public boolean onProgress(int itemcount, int size, String message) {
        publishProgress(new ProgressData(itemcount, size, message));

        final boolean cancelled = this.isCancelled();
        if (cancelled) {
            if (LibZipGlobal.debugEnabled) {
                Log.d(LibZipGlobal.LOG_TAG, mDebugPrefix + " cancel backup pressed ");
            }
        }
        return !cancelled;
    }


    /**
     * called from {@link AsyncTask} in gui task
     */
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

/**
 * ProgressData: Text that can be displayed as progress message
 * in owning Activity. Translated from android independant {@link de.k3b.io.IProgessListener}
 */
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


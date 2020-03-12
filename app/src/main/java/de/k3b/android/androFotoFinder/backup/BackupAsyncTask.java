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
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Date;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.widget.ProgressableAsyncTask;
import de.k3b.io.IProgessListener;
import de.k3b.zip.IZipConfig;
import de.k3b.zip.LibZipGlobal;
import de.k3b.zip.ZipConfigDto;
import de.k3b.zip.ZipStorage;

/**
 * Async ancapsulation of
 * {@link de.k3b.android.androFotoFinder.backup.Backup2ZipService}
 */
public class BackupAsyncTask extends ProgressableAsyncTask<IZipConfig> implements IProgessListener {

    private final String mDebugPrefix = "BackupAsyncTask ";
    private final Backup2ZipService service;

    public BackupAsyncTask(Context context, ZipConfigDto mZipConfigData, ZipStorage zipStorage,
                           Date backupDate) {
        this.service = new Backup2ZipService(context.getApplicationContext(),
                mZipConfigData, zipStorage, backupDate, BackupOptions.ALL);
    }

    @Override
    public void setContext(String why, Activity activity, ProgressBar progressBar, TextView status) {
        super.setContext(why, activity, progressBar, status);
        service.setProgessListener((progressBar != null) ? this : null);
    }

    @Override
    protected IZipConfig doInBackground(Object... voids) {
        try {
            return this.service.execute();
        } catch (Exception ex) {
            return null;
        } finally {
            this.isActive.set(false);
        }
    }

    /** called on success */
    @Override
    protected void onPostExecute(IZipConfig zipConfig) {
        super.onPostExecute(zipConfig);
        if (activity != null) {
            if (LibZipGlobal.debugEnabled || Global.debugEnabled) {
                Log.d(LibZipGlobal.LOG_TAG, activity.getClass().getSimpleName() + ": " +
                        formatter.format(lastSize, lastSize)
                );
                Log.d(LibZipGlobal.LOG_TAG, activity.getClass().getSimpleName() + ": " +
                        activity.getText(R.string.backup_title) + " " +
                        zipConfig
                );
            }

            if (zipConfig != null) {
                finish(" onPostExecute ok ", Activity.RESULT_OK, null);
            } else {
                CharSequence lastError = (this.service != null) ? this.service.getLastError(LibZipGlobal.debugEnabled) : null;
                if (lastError == null) lastError = activity.getText(android.R.string.cancel);
                finish(" onPostExecute not ok ", Activity.RESULT_CANCELED, lastError);
            }
        }
    }

    @Override
    public boolean onProgress(int itemcount, int size, String message) {
        boolean result = super.onProgress(itemcount, size, message);
        if (isCancelled()) {
            if (this.service != null) this.service.cancel();
        }
        return result;
    }
}

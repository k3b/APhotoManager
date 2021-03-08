/*
 * Copyright (c) 2016-2021 by k3b.
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
package de.k3b.android.util;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.io.IProgessListener;
import de.k3b.io.filefacade.IFile;

/**
 * Run mScanner as Background AsyncTask
 * Created by k3b on 04.10.2016.
 */

public class PhotoPropertiesMediaFilesScannerAsyncTask extends AsyncTask<IFile[], Object, Integer> implements IProgessListener {
    private static final String CONTEXT = "PhotoPropertiesMediaFilesScannerAsyncTask.";

    protected final PhotoPropertiesMediaFilesScanner mScanner;
    protected final Context mContext;
    protected final String mWhy;
    protected final IProgessListener progessListener;

    public PhotoPropertiesMediaFilesScannerAsyncTask(
            PhotoPropertiesMediaFilesScanner scanner, Context context, String why,
            IProgessListener progessListener) {
        mWhy = why;
        mContext = context.getApplicationContext();
        mScanner = scanner;
        this.progessListener = progessListener;
    }

    @Override
    protected Integer doInBackground(IFile[]... pathNames) {
        if (pathNames.length != 2)
            throw new IllegalArgumentException(CONTEXT + ".execute(oldFileNames, newFileNames)");
        return mScanner.updateMediaDatabaseAndroid42(mContext, pathNames[0], pathNames[1]);
    }

    private static void notifyIfThereAreChanges(Integer modifyCount, Context context, String why) {
        if (modifyCount > 0) {
            PhotoPropertiesMediaFilesScanner.notifyChanges(context, why);
            if (PhotoChangeNotifyer.photoChangedListener != null) {
                PhotoChangeNotifyer.photoChangedListener.onNotifyPhotoChanged();
            }

        }
    }

    @Override
    protected void onPostExecute(Integer modifyCount) {
        super.onPostExecute(modifyCount);
        String message = this.mContext.getString(R.string.scanner_update_result_format, modifyCount);
        Toast.makeText(this.mContext, message, Toast.LENGTH_LONG).show();
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, CONTEXT + "A42 scanner finished: " + message);
        }

        notifyIfThereAreChanges(modifyCount, mContext, mWhy);
    }

    /**
     * return true if this is executed in the gui thread
     */
    private static boolean isGuiThread() {
        return (Looper.myLooper() == Looper.getMainLooper());
    }

    @Override
    public boolean onProgress(int itemcount, int size, String message) {
        if (progessListener != null) {
            return progessListener.onProgress(itemcount, size, message);
        }
        return true;
    }
}

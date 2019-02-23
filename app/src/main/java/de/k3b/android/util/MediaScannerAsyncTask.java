/*
 * Copyright (c) 2016-2019 by k3b.
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

/**
 * Created by k3b on 04.10.2016.
 */

public class MediaScannerAsyncTask  extends AsyncTask<String[],Object,Integer> {
    private static final String CONTEXT = "MediaScannerAsyncTask.";

    protected final MediaScanner mScanner;
    protected final Context mContext;
    protected final String mWhy;

    public MediaScannerAsyncTask(MediaScanner scanner, Context context, String why) {
        mWhy = why;
        mContext = context.getApplicationContext();
        mScanner = scanner;
    }

    @Override
    protected Integer doInBackground(String[]... pathNames) {
        if (pathNames.length != 2) throw new IllegalArgumentException(CONTEXT + ".execute(oldFileNames, newFileNames)");
        return mScanner.updateMediaDatabase_Android42(mContext, pathNames[0], pathNames[1]);
    }

    @Override
    protected void onPostExecute(Integer modifyCount) {
        super.onPostExecute(modifyCount);
        String message = this.mContext.getString(R.string.scanner_update_result_format, modifyCount);
        Toast.makeText(this.mContext, message, Toast.LENGTH_LONG).show();
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, CONTEXT + "A42 scanner finished: " + message);
        }

        if (modifyCount > 0) {
            MediaScanner.notifyChanges(mContext, mWhy);
        }
    }

    /** do not wait for result. */
    public static void updateMediaDBInBackground(MediaScanner scanner, Context context, String why, String[] oldPathNames, String[] newPathNames) {
        if (isGuiThread()) {
            // update_Android42 scanner in seperate background task
            MediaScannerAsyncTask scanTask = new MediaScannerAsyncTask(scanner, context.getApplicationContext(), why + " from completed new AsycTask");
            scanTask.execute(oldPathNames, newPathNames);
        } else {
            // Continute in background task
            int modifyCount = scanner.updateMediaDatabase_Android42(context.getApplicationContext(), oldPathNames, newPathNames);
            if (modifyCount > 0) {
                MediaScanner.notifyChanges(context, why + " within current non-gui-task");
            }
        }
    }

    /** return true if this is executed in the gui thread */
    private static boolean isGuiThread() {
        return (Looper.myLooper() == Looper.getMainLooper());
    }

}

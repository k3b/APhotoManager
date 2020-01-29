/*
 * Copyright (c) 2020 by k3b.
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
package de.k3b.android.androFotoFinder.queries;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import de.k3b.android.util.PhotoChangeNotifyer;

/**
 * collect notifications that media content has changed
 */
public class GlobalMediaContentObserver extends ContentObserver {
    private static GlobalMediaContentObserver instance = null;
    private static Handler delayedChangeNotifiyHandler = null;
    private static Runnable delayedRunner = null;
    private static Context appContext;
    private static PhotoChangeNotifyer.PhotoChangedListener photoChangedListener = null;

    private GlobalMediaContentObserver() {
        super(null);
    }

    public static GlobalMediaContentObserver getInstance(final Context appContext) {
        if (instance == null) {
            GlobalMediaContentObserver.appContext = appContext;

            delayedRunner = new Runnable() {
                public void run() {
                    onExternalDataChangeCompleted(appContext);

                }
            };
            delayedChangeNotifiyHandler = new Handler();
            instance = new GlobalMediaContentObserver();
        }
        return instance;
    }

    /**
     * called in gui thread after external media-content changes are completed
     *
     * @param appContext
     */
    private static void onExternalDataChangeCompleted(Context appContext) {
        Log.d(MediaDBRepository.LOG_TAG, "Media content changed ");
        // todo fix database

        if (photoChangedListener != null) {
            photoChangedListener.onNotifyPhotoChanged();
        }
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);
        if (!selfChange) {
            Log.d(MediaDBRepository.LOG_TAG, "Media content changing " + uri);

            delayedChangeNotifiyHandler.removeCallbacks(delayedRunner);
            delayedChangeNotifiyHandler.postDelayed(delayedRunner, 500);
        }
    }
}
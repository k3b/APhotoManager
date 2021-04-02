/*
 * Copyright (c) 2019-2020 by k3b.
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

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.provider.MediaStore;

import de.k3b.android.androFotoFinder.queries.FotoSql;

/**
 * implements hiding Android specific Data change notifikation
 **/
public class PhotoChangeNotifyer {
    public static PhotoChangedListener photoChangedListener = null;

    public static void setPhotoChangedListener(PhotoChangedListener photoChangedListener) {
        PhotoChangeNotifyer.photoChangedListener = photoChangedListener;
    }

    public static void onNotifyPhotoChanged() {
        if (photoChangedListener != null) photoChangedListener.onNotifyPhotoChanged();
    }

    public static void notifyPhotoChanged(Context context, PhotoChangedListener adapter) {
        if (adapter != null) adapter.onNotifyPhotoChanged();

        if (false) {
            context.getApplicationContext().getContentResolver().notifyChange(FotoSql.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE, null, false);
        }
    }

    public static final void registerContentObserver(Context context, ContentObserver observer) {
        final ContentResolver contentResolver = context.getApplicationContext().getContentResolver();
        contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer);
        contentResolver.registerContentObserver(MediaStore.Files.getContentUri("external"), true, observer);
    }

    public static void unregisterContentObserver(Context context, ContentObserver instance) {
        context.getApplicationContext().getContentResolver().unregisterContentObserver(instance);
    }

    public interface PhotoChangedListener {
        void onNotifyPhotoChanged();
    }

}
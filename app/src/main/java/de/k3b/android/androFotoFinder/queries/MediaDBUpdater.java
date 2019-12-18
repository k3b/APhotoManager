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
package de.k3b.android.androFotoFinder.queries;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import java.util.Date;

import de.k3b.io.IProgessListener;

public class MediaDBUpdater {
    private final Context context;
    private final SQLiteDatabase writableDatabase;

    public MediaDBUpdater(Context context, SQLiteDatabase writableDatabase) {
        this.context = context;
        this.writableDatabase = writableDatabase;
    }

    public void rebuild(Context context, IProgessListener progessListener) {
        long start = new Date().getTime();
        clearMediaCopy();
        MediaImageDbReplacement.Impl.updateMedaiCopy(context, writableDatabase, null, progessListener);
        start = (new Date().getTime() - start) / 1000;
        final String text = "load db " + start + " secs";
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
        if (progessListener != null) progessListener.onProgress(0, 0, text);
    }

    public void clearMediaCopy() {
        DatabaseHelper.version2Upgrade_RecreateMediDbCopy(writableDatabase);
    }
}

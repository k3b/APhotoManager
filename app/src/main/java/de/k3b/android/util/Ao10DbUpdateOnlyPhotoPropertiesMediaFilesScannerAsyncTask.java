/*
 * Copyright (c) 2021 by k3b.
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

package de.k3b.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.queries.IMediaRepositoryApi;
import de.k3b.android.androFotoFinder.tagDB.TagSql;
import de.k3b.database.QueryParameter;
import de.k3b.io.IProgessListener;
import de.k3b.io.filefacade.FileFacade;
import de.k3b.io.filefacade.IFile;

public class Ao10DbUpdateOnlyPhotoPropertiesMediaFilesScannerAsyncTask extends RecursivePhotoPropertiesMediaFilesScannerAsyncTask {
    private static final String AO_10_NEW_SCANN_LAST_DATE_ADDED = "ao10NewScannLastDateAdded";
    private final IMediaRepositoryApi mediaDBApi;
    private final Date dateLastAdded;

    public Ao10DbUpdateOnlyPhotoPropertiesMediaFilesScannerAsyncTask(
            IMediaRepositoryApi mediaDBApi,
            PhotoPropertiesMediaFilesScanner scanner,
            Context context, String why,
            Date dateLastAdded,
            IProgessListener progessListener) {
        super(scanner, context, why, false, false, false, progessListener);
        this.mediaDBApi = mediaDBApi;
        this.dateLastAdded = dateLastAdded;
    }

    public static void saveDateLastAdded(Context context, Date date) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        if (date != null) {
            editor.putLong(AO_10_NEW_SCANN_LAST_DATE_ADDED, date.getTime());
        } else {
            editor.remove(AO_10_NEW_SCANN_LAST_DATE_ADDED);
        }
        editor.commit();
    }

    public static Date loadDateLastAdded(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        long result = sharedPref.getLong(AO_10_NEW_SCANN_LAST_DATE_ADDED, 0);
        if (result == 0) return null;
        return new Date(result);
    }

    @Override
    protected Integer doInBackground(IFile[]... pathNames) {
        // do not call super.doInBackground here because logic is different
        String dbgContext = "APM-Re-Scan Photos without tags/gps/rating since " + dateLastAdded + "\n";
        int resultCount = 0;
        boolean oldValue = this.mScanner.setIgnoreNoMediaCheck(true);
        List<Long> notFound = new ArrayList<>();
        long dateAddedInSecs = 0;
        try {
            onProgress(0, 0, "#");
            QueryParameter query = TagSql.createQueryIdPathDateForMediaScan(dateLastAdded);
            Cursor c = mediaDBApi.createCursorForQuery(null, dbgContext, query, null, null);
            int size = c.getCount();
            if (c.moveToFirst()) {
                do {
                    long id = c.getLong(0);
                    String path = c.getString(1);
                    dateAddedInSecs = (!c.isNull(2)) ? c.getLong(2) : 0;
                    IFile file = FileFacade.convert(dbgContext, path);
                    int modifyCount = mScanner.updateAndroid42(mediaDBApi, dbgContext, id, file);
                    if (modifyCount == 0) notFound.add(id);
                    this.mFileCount++;
                    this.mCurrentFolder = path;
                    if (mFileCount % 10 == 1) {
                        onProgress(mFileCount, size, path);
                    }
                } while (c.moveToNext());

            }
        } finally {
            this.mScanner.setIgnoreNoMediaCheck(oldValue);
            if (notFound.size() > 0) {
                FotoSql.deleteMedia(mediaDBApi, dbgContext + " not found", notFound, false);
            }
            if (dateAddedInSecs != 0) {
                saveDateLastAdded(mContext, new Date(dateAddedInSecs * 1000));
            }
        }
        return resultCount;
    }

// PhotoPropertiesMediaFilesScanner
}

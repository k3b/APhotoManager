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

import android.content.ContentValues;
import android.net.Uri;

import de.k3b.io.VISIBILITY;

/**
 * #155: All reads are done through database while writes are
 * applied to database and contentProvider.
 * <p>
 * Since Android-10 (api 29) using sqLite functions as content-provider-columns is not possible anymore.
 * Therefore apm uses a copy of contentprovider MediaStore.Images with same column names and same pk.
 */
public class MergedMediaDB extends MediaDBApiWrapper {
    private final IMediaDBApi database;
    private final IMediaDBApi contentProvider;

    public MergedMediaDB(IMediaDBApi database, IMediaDBApi contentProvider) {
        super(database, contentProvider);
        this.database = database;
        this.contentProvider = contentProvider;
    }

    @Override
    public int execUpdate(String dbgContext, long id, ContentValues values) {
        int result = super.execUpdate(dbgContext, id, values);
        database.execUpdate(dbgContext, id, values);
        return result;
    }

    @Override
    public int execUpdate(String dbgContext, String path, ContentValues values, VISIBILITY visibility) {
        int result = super.execUpdate(dbgContext, path, values, visibility);
        database.execUpdate(dbgContext, path, values, visibility);
        return result;
    }

    @Override
    public int exexUpdateImpl(String dbgContext, ContentValues values, String sqlWhere, String[] selectionArgs) {
        int result = super.exexUpdateImpl(dbgContext, values, sqlWhere, selectionArgs);
        database.exexUpdateImpl(dbgContext, values, sqlWhere, selectionArgs);
        return result;
    }

    /**
     * return id of inserted item
     *
     * @param dbgContext
     * @param dbUpdateFilterJpgFullPathName
     * @param values
     * @param visibility
     * @param updateSuccessValue
     */
    @Override
    public Long insertOrUpdateMediaDatabase(String dbgContext, String dbUpdateFilterJpgFullPathName,
                                            ContentValues values, VISIBILITY visibility, Long updateSuccessValue) {
        Long result = updateSuccessValue;

        int modifyCount = contentProvider.execUpdate(dbgContext, dbUpdateFilterJpgFullPathName,
                values, visibility);

        if (modifyCount == 0) {
            // update failed (probably becauce oldFullPathName not found. try insert it.
            FotoSql.addDateAdded(values);

            // insert into contentProvider and database
            Uri uriWithId = execInsert(dbgContext, values);
            result = FotoSql.getId(uriWithId);
        } else {
            // update into contentprovider successfull. also add to database
            database.execUpdate(dbgContext, dbUpdateFilterJpgFullPathName,
                    values, visibility);
        }
        return result;
    }

    /**
     * every database insert should go through this. adds logging if enabled
     *
     * @param dbgContext
     * @param values
     */
    @Override
    public Uri execInsert(String dbgContext, ContentValues values) {
        Uri result = super.execInsert(dbgContext, values);

        // insert with same pk as contentprovider does
        values.put(FotoSql.SQL_COL_PK, FotoSql.getId(result));
        database.execInsert(dbgContext, values);
        values.remove(FotoSql.SQL_COL_PK);
        return result;
    }

    /**
     * Deletes media items specified by where with the option to prevent cascade delete of the image.
     *
     * @param dbgContext
     * @param where
     * @param selectionArgs
     * @param preventDeleteImageFile
     */
    @Override
    public int deleteMedia(String dbgContext, String where, String[] selectionArgs, boolean preventDeleteImageFile) {
        int result = super.deleteMedia(dbgContext, where, selectionArgs, preventDeleteImageFile);
        database.deleteMedia(dbgContext, where, selectionArgs, preventDeleteImageFile);
        return result;
    }
}

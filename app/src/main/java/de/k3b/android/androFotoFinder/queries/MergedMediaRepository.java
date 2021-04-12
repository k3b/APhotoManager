/*
 * Copyright (c) 2019-2021 by k3b.
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
import android.util.Log;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.io.VISIBILITY;

/**
 * #155: All reads are done through localDatabase while writes are
 * applied to localDatabase and contentProvider.
 * <p>
 * Since Android-10 (api 29) is not possible anymore:
 * * using sqLite functions as content-provider-columns
 * * read gps data (latitude,longitude.
 *
 * Therefore apm uses a copy of contentprovider MediaStore.Images with same column names and same pk.
 *
 * The copy is called "localDatabase" the other "contentProvider".
 */
public class MergedMediaRepository extends MediaRepositoryApiWrapper {
    private static final String LOG_TAG = MediaDBRepository.LOG_TAG;

    private final IMediaRepositoryApi localDatabase;
    private final IMediaRepositoryApi contentProvider;

    public MergedMediaRepository(IMediaRepositoryApi localDatabase, IMediaRepositoryApi contentProvider) {
        super(localDatabase, contentProvider, localDatabase);
        this.localDatabase = localDatabase;
        this.contentProvider = contentProvider;
    }

    @Override
    public int execUpdate(String dbgContext, long id, ContentValues values) {
        int result = super.execUpdate(dbgContext, id, values);
        if (result == 0) {
            dbgContext += " " + id + " not found or no change in contentprovider.";

            String path = values.getAsString(FotoSql.SQL_COL_PATH);
            Long changedId = FotoSql.getId(dbgContext, contentProvider, path);
            if (changedId != null && id != changedId.longValue()) {
                // pk in contentprovidser has changed
                dbgContext += " unsing " + path + "(" + changedId + ") instead";
                result = super.execUpdate(dbgContext, changedId, values);
                if (result > 0) {
                    // also correcting pk in local db
                    values.put(FotoSql.SQL_COL_PK, changedId);
                }
            }
        }
        return localDatabase.execUpdate(dbgContext, id, values);
    }

    @Override
    public int execUpdate(String dbgContext, String path, ContentValues values, VISIBILITY visibility) {
        int result = super.execUpdate(
                dbgContext, path, values, visibility);
        result = localDatabase.execUpdate(dbgContext, path, values, visibility);
        return result;
    }

    @Override
    public int exexUpdateImpl(String dbgContext, ContentValues values, String sqlWhere, String[] selectionArgs) {
        int result = super.exexUpdateImpl(dbgContext, values, sqlWhere, selectionArgs);
        localDatabase.exexUpdateImpl(dbgContext, values, sqlWhere, selectionArgs);
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
        Uri uriWithId = null;
        Long idInContentprovider = FotoSql.getId(dbgContext, contentProvider, dbUpdateFilterJpgFullPathName);

        if (idInContentprovider != null) {
            // already existing
            int modifyCount = contentProvider.execUpdate(
                    dbgContext, idInContentprovider,values);
            if (modifyCount != 1 || Global.debugEnabledSql) {
                Log.i(LOG_TAG, dbgContext + " merge-execUpdate(existContentProvider," + idInContentprovider +
                        "[" + dbUpdateFilterJpgFullPathName + "]) = " + modifyCount +" items modified");
            }
        } else {
            // update failed (probably becauce oldFullPathName not found. try insert it.
            FotoSql.addDateAdded(values);

            // insert into contentProvider and database
            uriWithId =  contentProvider.execInsert(dbgContext, values);

            if (uriWithId != null) {
                idInContentprovider = FotoSql.getId(uriWithId);
            }
        }

        Long idInDb = FotoSql.getId(dbgContext, localDatabase, dbUpdateFilterJpgFullPathName);
        if (idInContentprovider != null && (idInDb == null || !idInContentprovider.equals(idInDb))) {
            // must set or change id in local database
            values.put(FotoSql.SQL_COL_PK, idInContentprovider);
        }

        if (idInDb != null) {
            int modifyCount = localDatabase.execUpdate(dbgContext, dbUpdateFilterJpgFullPathName,
                    values, visibility);
            if (modifyCount != 1 || Global.debugEnabledSql) {
                Log.i(LOG_TAG, dbgContext + " merge-execUpdate(existLocalDb," + idInDb +
                        "[" + dbUpdateFilterJpgFullPathName + "]) = " + modifyCount +" items modified");
            }
        } else {
            // update failed (probably becauce oldFullPathName not found. try insert it.
            FotoSql.addDateAdded(values);

            // insert into contentProvider and database
            uriWithId = localDatabase.execInsert(dbgContext, values);
            if (uriWithId != null) {
                idInDb = FotoSql.getId(uriWithId);
            }
        }

        Long result;
        if (idInDb != null) {
            result = idInDb;
        } else if (idInContentprovider != null) {
            result = idInContentprovider;
        } else {
            result = updateSuccessValue;
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
        // insert into android-contentprovider
        Uri result = super.execInsert(dbgContext, values);
        if (result == null) {
            dbgContext = dbgContext + "-insert into Contentprovider failed";
            // not inserted because in android-contentprovider may already exist.
            // get id from android-contentprovider
            String path = values.getAsString(FotoSql.SQL_COL_PATH);
            Long id = FotoSql.getId(dbgContext, contentProvider, path);

            if (id == null) {
                Log.i(LOG_TAG, dbgContext + " Path '" + path + "' not found. Aborted.");
                return null;
            }
            values.put(FotoSql.SQL_COL_PK, id);
            super.execUpdate(dbgContext +
                    "- Updating id=" + id +" instead ",id, values);
            result = FotoSql.getUri(id);
        } else {
            // insert with same pk as contentprovider does
            values.put(FotoSql.SQL_COL_PK, FotoSql.getId(result));
        }
        result = localDatabase.execInsert(dbgContext, values);
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
        localDatabase.deleteMedia(dbgContext, where, selectionArgs, preventDeleteImageFile);
        return result;
    }

    @Override
    public ContentValues getDbContent(Long idOrNull, String filePathOrNull) {
        return this.contentProvider.getDbContent(idOrNull, filePathOrNull);
    }

}

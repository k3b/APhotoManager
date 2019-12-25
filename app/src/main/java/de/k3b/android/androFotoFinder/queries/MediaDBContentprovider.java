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
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.CancellationSignal;

import de.k3b.database.QueryParameter;
import de.k3b.io.VISIBILITY;

/**
 * Implementation of Context.getContentResolver()-ContentProvider based media api
 */
public class MediaDBContentprovider implements IMediaDBApi {
    private final Context context;

    public MediaDBContentprovider(final Context context) {
        this.context = context;
    }

    @Override
    public Cursor createCursorForQuery(
            StringBuilder out_debugMessage, String dbgContext,
            QueryParameter parameters, VISIBILITY visibility, CancellationSignal cancellationSignal) {
        return ContentProviderMediaImpl.createCursorForQuery(
                out_debugMessage, dbgContext, context, parameters, visibility, cancellationSignal);
    }

    @Override
    public Cursor createCursorForQuery(StringBuilder out_debugMessage, String dbgContext, final String from, final String sqlWhereStatement,
                                       final String[] sqlWhereParameters, final String sqlSortOrder,
                                       CancellationSignal cancellationSignal, final String... sqlSelectColums) {
        return ContentProviderMediaImpl.createCursorForQuery(
                out_debugMessage, dbgContext, context, from, sqlWhereStatement,
                sqlWhereParameters, sqlSortOrder, null, sqlSelectColums);
    }

    @Override
    public int execUpdate(String dbgContext, long id, ContentValues values) {
        return exexUpdateImpl(dbgContext, values, FotoSql.FILTER_COL_PK, new String[]{Long.toString(id)});
    }

    @Override
    public int execUpdate(String dbgContext, String path, ContentValues values, VISIBILITY visibility) {
        return exexUpdateImpl(dbgContext, values, FotoSql.getFilterExprPathLikeWithVisibility(visibility), new String[]{path});
    }

    @Override
    public int exexUpdateImpl(String dbgContext, ContentValues values, String sqlWhere, String[] selectionArgs) {
        return ContentProviderMediaImpl.exexUpdateImpl(dbgContext, context, values, sqlWhere, selectionArgs);
    }

    /**
     * return id of inserted item
     */
    @Override
    public Long insertOrUpdateMediaDatabase(String dbgContext,
                                            String dbUpdateFilterJpgFullPathName,
                                            ContentValues values, VISIBILITY visibility,
                                            Long updateSuccessValue) {
        return ContentProviderMediaImpl.insertOrUpdateMediaDatabase(dbgContext, context,
                dbUpdateFilterJpgFullPathName,
                values, visibility,
                updateSuccessValue);
    }

    /**
     * every database insert should go through this. adds logging if enabled
     */
    @Override
    public Uri execInsert(String dbgContext, ContentValues values) {
        return ContentProviderMediaImpl.execInsert(dbgContext, context, values);
    }

    /**
     * Deletes media items specified by where with the option to prevent cascade delete of the image.
     */
    @Override
    public int deleteMedia(String dbgContext, String where, String[] selectionArgs, boolean preventDeleteImageFile) {
        return ContentProviderMediaImpl.deleteMedia(dbgContext, context, where, selectionArgs, preventDeleteImageFile);
    }

    @Override
    public ContentValues getDbContent(final long id) {
        return ContentProviderMediaImpl.getDbContent(context, id);
    }

    @Override
    public long getCurrentUpdateId() {
        return 0;
    }

    @Override
    public boolean mustRequery(long updateId) {
        return false;
    }

    @Override
    public void beginTransaction() {

    }

    @Override
    public void setTransactionSuccessful() {

    }

    @Override
    public void endTransaction() {

    }

}

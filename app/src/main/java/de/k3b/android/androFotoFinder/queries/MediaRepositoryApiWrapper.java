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
package de.k3b.android.androFotoFinder.queries;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.CancellationSignal;

import de.k3b.database.QueryParameter;
import de.k3b.io.VISIBILITY;
import de.k3b.media.IPhotoProperties;

/**
 * (Default) Implementation of {@link IMediaRepositoryApi} to forward all methods to an inner child {@link IPhotoProperties}.
 * <p>
 * Created by k3b on 30.11.2019.
 */
public class MediaRepositoryApiWrapper implements IMediaRepositoryApi {
    protected final IMediaRepositoryApi readChild;
    protected final IMediaRepositoryApi writeChild;
    protected final IMediaRepositoryApi transactionChild;

    /**
     * count the non path write calls
     */
    private int modifyCount = 0;

    public MediaRepositoryApiWrapper(IMediaRepositoryApi child) {
        this(child, child, child);
    }

    public MediaRepositoryApiWrapper(IMediaRepositoryApi readChild, IMediaRepositoryApi writeChild, IMediaRepositoryApi transactionChild) {
        this.readChild = readChild;
        this.writeChild = writeChild;
        this.transactionChild = transactionChild;
    }

    @Override
    public Cursor createCursorForQuery(StringBuilder out_debugMessage, String dbgContext, QueryParameter parameters, VISIBILITY visibility, CancellationSignal cancellationSignal) {
        return readChild.createCursorForQuery(out_debugMessage, dbgContext, parameters, visibility, cancellationSignal);
    }

    @Override
    public Cursor createCursorForQuery(StringBuilder out_debugMessage, String dbgContext, String from, String sqlWhereStatement, String[] sqlWhereParameters, String sqlSortOrder, CancellationSignal cancellationSignal, String... sqlSelectColums) {
        return readChild.createCursorForQuery(out_debugMessage, dbgContext, from, sqlWhereStatement, sqlWhereParameters, sqlSortOrder, cancellationSignal, sqlSelectColums);
    }

    @Override
    public int execUpdate(String dbgContext, long id, ContentValues values) {
        return writeChild.execUpdate(dbgContext, id, values);
    }

    @Override
    public int execUpdate(String dbgContext, String path, ContentValues values, VISIBILITY visibility) {
        return writeChild.execUpdate(dbgContext, path, values, visibility);
    }

    @Override
    public int exexUpdateImpl(String dbgContext, ContentValues values, String sqlWhere, String[] selectionArgs) {
        return writeChild.exexUpdateImpl(dbgContext, values, sqlWhere, selectionArgs);
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
    public Long insertOrUpdateMediaDatabase(String dbgContext, String dbUpdateFilterJpgFullPathName, ContentValues values, VISIBILITY visibility, Long updateSuccessValue) {
        return writeChild.insertOrUpdateMediaDatabase(dbgContext, dbUpdateFilterJpgFullPathName, values, visibility, updateSuccessValue);
    }

    /**
     * every database insert should go through this. adds logging if enabled
     *
     * @param dbgContext
     * @param values
     */
    @Override
    public Uri execInsert(String dbgContext, ContentValues values) {
        return writeChild.execInsert(dbgContext, values);
    }

    /**
     * Deletes media items specified by where with the option to prevent cascade delete of the image.
     */
    @Override
    public int deleteMedia(String dbgContext, String where, String[] selectionArgs, boolean preventDeleteImageFile) {
        return writeChild.deleteMedia(dbgContext, where, selectionArgs, preventDeleteImageFile);
    }

    @Override
    public ContentValues getDbContent(long id) {
        return readChild.getDbContent(id);
    }

    @Override
    public long getCurrentUpdateId() {
        return transactionChild.getCurrentUpdateId();
    }

    @Override
    public boolean mustRequery(long updateId) {
        return transactionChild.mustRequery(updateId);
    }

    @Override
    public void beginTransaction() {
        transactionChild.beginTransaction();
    }

    @Override
    public void setTransactionSuccessful() {
        transactionChild.setTransactionSuccessful();
    }

    @Override
    public void endTransaction() {
        transactionChild.endTransaction();
    }
}

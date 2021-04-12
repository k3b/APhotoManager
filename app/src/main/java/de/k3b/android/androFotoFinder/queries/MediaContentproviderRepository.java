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

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.util.Log;

import java.util.ArrayList;

import de.k3b.LibGlobal;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.database.QueryParameter;
import de.k3b.io.VISIBILITY;

/**
 * Access Media Data through Android media contentprovider.
 * <p>
 * Implementation of Context.getContentResolver()-ContentProvider based media api
 */
public class MediaContentproviderRepository implements IMediaRepositoryApi {
    public static final String LOG_TAG = MediaContentproviderRepositoryImpl.LOG_TAG;
    private static final String MODUL_NAME = MediaContentproviderRepository.class.getSimpleName() + "-Transaction ";

    private final Context context;

    private ArrayList<ContentProviderOperation> transaction = null;

    public MediaContentproviderRepository(final Context context) {
        this.context = context;
    }

    @Override
    public Cursor createCursorForQuery(
            StringBuilder out_debugMessage, String dbgContext,
            QueryParameter parameters, VISIBILITY visibility, CancellationSignal cancellationSignal) {
        return MediaContentproviderRepositoryImpl.createCursorForQuery(
                out_debugMessage, dbgContext, context, parameters, visibility, cancellationSignal);
    }

    @Override
    public Cursor createCursorForQuery(StringBuilder out_debugMessage, String dbgContext, final String from, final String sqlWhereStatement,
                                       final String[] sqlWhereParameters, final String sqlSortOrder,
                                       CancellationSignal cancellationSignal, final String... sqlSelectColums) {
        return MediaContentproviderRepositoryImpl.createCursorForQuery(
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
        if (transaction != null) {
            if (((dbgContext != null) && (Global.debugEnabledSql || LibGlobal.debugEnabledJpg))) {
                Log.i(LOG_TAG, dbgContext + ":" +
                        MODUL_NAME +
                        ".update\n" +
                        QueryParameter.toString(null, values.toString(), FotoSqlBase.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE_NAME,
                                sqlWhere, selectionArgs, null, -1));
            }

            transaction.add(ContentProviderOperation
                    .newUpdate(FotoSqlBase.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE)
                    .withValues(values)
                    .withSelection(sqlWhere, selectionArgs)
                    .build());
            return 1;
        } else {
            return MediaContentproviderRepositoryImpl.exexUpdateImpl(dbgContext, context, values, sqlWhere, selectionArgs);
        }
    }

    /**
     * return id of inserted item
     */
    @Override
    public Long insertOrUpdateMediaDatabase(String dbgContext,
                                            String dbUpdateFilterJpgFullPathName,
                                            ContentValues values, VISIBILITY visibility,
                                            Long updateSuccessValue) {
        if (transaction != null) {
            String msg = dbgContext + ":" +
                    MODUL_NAME +
                    ".insertOrUpdateMediaDatabase not implemented\n" +
                    QueryParameter.toString(null, values.toString(), FotoSqlBase.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE_NAME,
                            dbUpdateFilterJpgFullPathName, null, null, -1);
            throw new SQLException(msg);
        }
        return MediaContentproviderRepositoryImpl.insertOrUpdateMediaDatabase(dbgContext, context,
                dbUpdateFilterJpgFullPathName,
                values, visibility,
                updateSuccessValue);
    }

    /**
     * every database insert should go through this. adds logging if enabled
     */
    @Override
    public Uri execInsert(String dbgContext, ContentValues values) {
        if (transaction != null) {
            if (((dbgContext != null) && (Global.debugEnabledSql || LibGlobal.debugEnabledJpg))) {
                Log.i(LOG_TAG, dbgContext + ":" +
                        MODUL_NAME +
                        ".insert\n" +
                        QueryParameter.toString(null, values.toString(), FotoSqlBase.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE_NAME,
                                null, null, null, -1));
            }

            transaction.add(ContentProviderOperation
                    .newInsert(FotoSqlBase.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE)
                    .withValues(values)
                    .build());
            return Uri.EMPTY;
        } else {
            return MediaContentproviderRepositoryImpl.execInsert(dbgContext, context, values);
        }
    }

    /**
     * Deletes media items specified by where with the option to prevent cascade delete of the image.
     */
    @Override
    public int deleteMedia(String dbgContext, String sqlWhere, String[] selectionArgs, boolean preventDeleteImageFile) {
        if (transaction != null) {
            if (((dbgContext != null) && (Global.debugEnabledSql || LibGlobal.debugEnabledJpg))) {
                Log.i(LOG_TAG, dbgContext + ":" +
                        MODUL_NAME +
                        ".update\n" +
                        QueryParameter.toString(null, null, FotoSqlBase.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE_NAME,
                                sqlWhere, selectionArgs, null, -1));
            }

            if (preventDeleteImageFile) {
                // set SQL_COL_PATH empty so sql-delete cannot cascade delete the referenced image-file via delete trigger
                ContentValues values = MediaContentproviderRepositoryImpl.getContentValuesDeleteStep1();
                exexUpdateImpl(dbgContext + "-a: " +
                                MODUL_NAME +
                                ".deleteMedia: ",
                        values, sqlWhere, selectionArgs);
                deleteMedia(dbgContext, FotoSql.SQL_COL_PATH + " is null", null, false);
            } else {
                transaction.add(ContentProviderOperation
                        .newDelete(FotoSqlBase.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE)
                        .withSelection(sqlWhere, selectionArgs)
                        .build());
            }
            return 1;
        } else {
            return MediaContentproviderRepositoryImpl.deleteMedia(dbgContext, context, sqlWhere, selectionArgs, preventDeleteImageFile);
        }
    }

    @Override
    public ContentValues getDbContent(final Long idOrNull, String filePathOrNull) {
        return MediaContentproviderRepositoryImpl.getDbContent(context, idOrNull, filePathOrNull);
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
        if (transaction != null) {
            String msg =
                    MODUL_NAME +
                            ".beginTransaction : already pending Transaction";
            throw new SQLException(msg);
        } else {
            if ((Global.debugEnabledSql || LibGlobal.debugEnabledJpg)) {
                Log.i(LOG_TAG,
                        MODUL_NAME +
                                ".beginTransaction");
            }
            this.transaction = new ArrayList<>();
        }
    }

    @Override
    public void setTransactionSuccessful() {
        if (transaction != null) {
            if ((Global.debugEnabledSql || LibGlobal.debugEnabledJpg)) {
                Log.i(LOG_TAG, MODUL_NAME + ".setTransactionSuccessful " + transaction.size());
            }
            try {
                context.getContentResolver().applyBatch(
                        FotoSqlBase.AUTHORITY, transaction);
            } catch (Exception e) {
                final String msg = MODUL_NAME + ".applyBatch " + e.getMessage();
                Log.e(LOG_TAG, msg, e);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    throw new SQLException(msg, e);
                }
                throw new SQLException(msg);
            }
        } else {
            String msg =
                    MODUL_NAME +
                            ".setTransactionSuccessful : no pending Transaction";
            throw new SQLException(msg);
        }
    }

    @Override
    public void endTransaction() {
        if (transaction != null) {
            if ((Global.debugEnabledSql || LibGlobal.debugEnabledJpg)) {
                Log.i(LOG_TAG, MODUL_NAME + ".endTransaction");
            }
            transaction = null;

        } else {
            String msg =
                    MODUL_NAME +
                            ".endTransaction : no pending Transaction";
            throw new SQLException(msg);
        }
    }

}

/*
 * Copyright (c) 2015-2020 by k3b.
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
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.util.Log;

import java.util.ArrayList;

import de.k3b.LibGlobal;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.database.QueryParameter;
import de.k3b.io.StringUtils;
import de.k3b.io.VISIBILITY;

/**
 * Static Implementation of Context.getContentResolver()-ContentProvider based media api
 */
public class MediaContentproviderRepositoryImpl {
    public static final String LOG_TAG = FotoSql.LOG_TAG + "Content";
    private static final String MODUL_NAME = MediaContentproviderRepositoryImpl.class.getName();

    public static Cursor createCursorForQuery(
            StringBuilder out_debugMessage, String dbgContext, final Context context,
            QueryParameter parameters, VISIBILITY visibility, CancellationSignal cancellationSignal) {
        if (visibility != null) FotoSql.setWhereVisibility(parameters, visibility);
        return createCursorForQuery(out_debugMessage, dbgContext, context, parameters.toFrom(),
                parameters.toAndroidWhere(),
                parameters.toAndroidParameters(), parameters.toOrderBy(),
                cancellationSignal, parameters.toColumns()
        );
    }

    /**
     * every cursor query should go through this. adds logging if enabled
     */
    static Cursor createCursorForQuery(
            StringBuilder out_debugMessage, String dbgContext, final Context context,
            final String from, final String sqlWhereStatement,
            final String[] sqlWhereParameters, final String sqlSortOrder,
            CancellationSignal cancellationSignal, final String... sqlSelectColums) {
        ContentResolver resolver = context.getContentResolver();
        Cursor query = null;

        Exception excpetion = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                query = resolver.query(Uri.parse(from), sqlSelectColums, sqlWhereStatement, sqlWhereParameters, sqlSortOrder, cancellationSignal);
            } else {
                query = resolver.query(Uri.parse(from), sqlSelectColums, sqlWhereStatement, sqlWhereParameters, sqlSortOrder);
            }
        } catch (Exception ex) {
            excpetion = ex;
        } finally {
            if ((excpetion != null) || Global.debugEnabledSql || (out_debugMessage != null)) {
                StringBuilder message = StringUtils.appendMessage(out_debugMessage, excpetion,
                        dbgContext, MODUL_NAME +
                                ".createCursorForQuery:\n",
                        QueryParameter.toString(sqlSelectColums, null, from, sqlWhereStatement,
                                sqlWhereParameters, sqlSortOrder, query.getCount()));
                if (out_debugMessage == null) {
                    Log.i(LOG_TAG, message.toString(), excpetion);
                } // else logging is done by caller
            }
        }

        return query;
    }

    public static int execUpdate(String dbgContext, Context context, String path, ContentValues values, VISIBILITY visibility) {
        return exexUpdateImpl(dbgContext, context, values, FotoSql.getFilterExprPathLikeWithVisibility(visibility), new String[]{path});
    }

    /**
     * every database update should go through this. adds logging if enabled
     */
    public static int exexUpdateImpl(String dbgContext, Context context, ContentValues values, String sqlWhere, String[] selectionArgs) {
        int result = -1;
        Exception excpetion = null;
        try {
            result = context.getContentResolver().update(FotoSqlBase.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE,
                    values, sqlWhere,
                    selectionArgs);
        } catch (Exception ex) {
            excpetion = ex;
        } finally {
            if ((excpetion != null) || ((dbgContext != null) && (Global.debugEnabledSql || LibGlobal.debugEnabledJpg))) {
                Log.i(LOG_TAG, dbgContext + ":" +
                        MODUL_NAME +
                        ".exexUpdate " + excpetion + "\n" +
                        QueryParameter.toString(null, values.toString(), FotoSqlBase.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE_NAME,
                                sqlWhere, selectionArgs, null, result), excpetion);
            }
        }
        return result;
    }

    /**
     * return id of inserted item
     */
    public static Long insertOrUpdateMediaDatabase(String dbgContext, Context context,
                                                   String dbUpdateFilterJpgFullPathName,
                                                   ContentValues values, VISIBILITY visibility,
                                                   Long updateSuccessValue) {
        Long result = updateSuccessValue;

        int modifyCount = execUpdate(dbgContext, context, dbUpdateFilterJpgFullPathName,
                values, visibility);

        if (modifyCount == 0) {
            // update failed (probably becauce oldFullPathName not found. try insert it.
            FotoSql.addDateAdded(values);

            Uri uriWithId = execInsert(dbgContext, context, values);
            result = FotoSql.getId(uriWithId);
        }
        return result;
    }

    /**
     * every database insert should go through this. adds logging if enabled
     */
    public static Uri execInsert(String dbgContext, Context context, ContentValues values) {
        Uri providerUri = (null != values.get(FotoSql.SQL_COL_EXT_MEDIA_TYPE)) ? FotoSqlBase.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE : FotoSqlBase.SQL_TABLE_EXTERNAL_CONTENT_URI;

        Uri result = null;
        Exception excpetion = null;
        try {
            // on my android-4.4 insert with media_type=1001 (private) does insert with media_type=1 (image)
            result = context.getContentResolver().insert(providerUri, values);
        } catch (Exception ex) {
            excpetion = ex;
        } finally {
            if ((excpetion != null) || Global.debugEnabledSql || LibGlobal.debugEnabledJpg) {
                Log.i(LOG_TAG, dbgContext + ":" +
                        MODUL_NAME +
                        ".execInsert " + excpetion + " " +
                        values.toString() + " => " + result + " " + excpetion, excpetion);
            }
        }
        return result;
    }

    /**
     * Deletes media items specified by where with the option to prevent cascade delete of the image.
     */
    public static int deleteMedia(String dbgContext, Context context, String where, String[] selectionArgs, boolean preventDeleteImageFile) {
        String[] lastSelectionArgs = selectionArgs;
        String lastUsedWhereClause = where;
        int delCount = 0;
        try {
            if (preventDeleteImageFile) {
                // set SQL_COL_PATH empty so sql-delete cannot cascade delete the referenced image-file via delete trigger
                ContentValues values = new ContentValues();
                values.put(FotoSql.SQL_COL_PATH, FotoSql.DELETED_FILE_MARKER);
                values.put(FotoSql.SQL_COL_EXT_MEDIA_TYPE, 0); // so it will not be shown as image any more
                exexUpdateImpl(dbgContext + "-a: " +
                                MODUL_NAME +
                                ".deleteMedia: ",
                        context, values, lastUsedWhereClause, lastSelectionArgs);

                lastUsedWhereClause = FotoSql.SQL_COL_PATH + " is null";
                lastSelectionArgs = null;
                delCount = context.getContentResolver()
                        .delete(FotoSqlBase.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE, lastUsedWhereClause, lastSelectionArgs);
                if (Global.debugEnabledSql || LibGlobal.debugEnabledJpg) {
                    Log.i(LOG_TAG, dbgContext + "-b: " +
                            MODUL_NAME +
                            ".deleteMedia delete\n" +
                            QueryParameter.toString(null, null, FotoSqlBase.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE_NAME,
                                    lastUsedWhereClause, lastSelectionArgs, null, delCount));
                }
            } else {
                delCount = context.getContentResolver()
                        .delete(FotoSqlBase.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE, lastUsedWhereClause, lastSelectionArgs);
                if (Global.debugEnabledSql || LibGlobal.debugEnabledJpg) {
                    Log.i(LOG_TAG, dbgContext + ": " +
                            MODUL_NAME +
                            ".deleteMedia\ndelete " +
                            QueryParameter.toString(null, null,
                                    FotoSqlBase.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE_NAME,
                                    lastUsedWhereClause, lastSelectionArgs, null, delCount));
                }
            }
        } catch (Exception ex) {
            // null pointer exception when delete matches not items??
            final String msg = dbgContext + ": Exception in " +
                    MODUL_NAME +
                    ".deleteMedia:\n" +
                    QueryParameter.toString(null, null, FotoSqlBase.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE_NAME,
                            lastUsedWhereClause, lastSelectionArgs, null, -1)
                    + " : " + ex.getMessage();
            Log.e(LOG_TAG, msg, ex);

        }
        return delCount;
    }

    /**
     * execRenameFolder(getActivity(),"/storage/sdcard0/testFolder/", "/storage/sdcard0/renamedFolder/")
     * "/storage/sdcard0/testFolder/image.jpg" becomes "/storage/sdcard0/renamedFolder/image.jpg"
     *
     * @return number of updated items
     */
    private static int _del_execRenameFolder_batch_not_working(Context context, String pathOld, String pathNew) {
        final String dbgContext = MODUL_NAME +
                ".execRenameFolder('" +
                pathOld + "' => '" + pathNew + "')";
        // sql update file set path = newBegin + substing(path, begin+len) where path like newBegin+'%'
        // public static final String SQL_EXPR_FOLDER = "substr(" + SQL_COL_PATH + ",1,length(" + SQL_COL_PATH + ") - length(" + MediaStore.Images.Media.DISPLAY_NAME + "))";

        final String sqlColNewPathAlias = "new_path";
        final String sql_col_pathnew = "'" + pathNew + "' || substr(" + FotoSql.SQL_COL_PATH +
                "," + (pathOld.length() + 1) + ",255) AS " + sqlColNewPathAlias;

        QueryParameter queryAffectedFiles = new QueryParameter()
                .setID(FotoSql.QUERY_TYPE_DEFAULT)
                .addColumn(FotoSql.SQL_COL_PK,
                        sql_col_pathnew)
                .addFrom(FotoSqlBase.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE_NAME)
                .addWhere(FotoSql.SQL_COL_PATH + " like '" + pathOld + "%'")
                // SQL_COL_EXT_MEDIA_TYPE IS NOT NULL enshures that all media types (mp3, mp4, txt,...) are updated
                .addWhere(FotoSql.SQL_COL_EXT_MEDIA_TYPE + " IS NOT NULL");

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

        Cursor c = null;
        try {
            c = createCursorForQuery(null, dbgContext, context, queryAffectedFiles, null, null);
            int pkColNo = c.getColumnIndex(FotoSql.SQL_COL_PK);
            int pathColNo = c.getColumnIndex(sqlColNewPathAlias);

            while (c.moveToNext()) {
                // paths[row] = c.getString(pathColNo);
                // ids[row] = c.getLong(pkColNo);
                ops.add(ContentProviderOperation.newUpdate(FotoSqlBase.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE)
                        .withSelection(FotoSql.FILTER_COL_PK, new String[]{c.getString(pkColNo)})
                        .withValue(FotoSql.SQL_COL_PATH, c.getString(pathColNo))
                        .build());
            }
        } catch (Exception ex) {
            Log.e(LOG_TAG, dbgContext + "-getAffected error :", ex);
            return -1;
        } finally {
            if (c != null) c.close();
        }

        try {
            context.getContentResolver().applyBatch(FotoSqlBase.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE_NAME, ops);
        } catch (Exception ex) {
            // java.lang.IllegalArgumentException: Unknown authority content://media/external/file
            // i assume not batch support for file
            Log.e(LOG_TAG, dbgContext + "-updateAffected error :", ex);
            return -1;
        }
        return ops.size();
    }

    public static ContentValues getDbContent(Context context, final long id) {
        ContentResolver resolver = context.getContentResolver();

        Cursor c = null;
        try {
            c = resolver.query(FotoSqlBase.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE, new String[]{"*"}, FotoSql.FILTER_COL_PK, new String[]{"" + id}, null);
            if (c.moveToNext()) {
                ContentValues values = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(c, values);
                return values;
            }
        } catch (Exception ex) {
            Log.e(LOG_TAG, MODUL_NAME +
                    ".getDbContent(id=" + id + ") failed", ex);
        } finally {
            if (c != null) c.close();
        }
        return null;
    }
}

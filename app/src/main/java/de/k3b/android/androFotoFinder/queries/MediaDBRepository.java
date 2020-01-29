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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.provider.MediaStore;
import android.util.Log;

import java.sql.Date;

import de.k3b.LibGlobal;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.database.QueryParameter;
import de.k3b.io.AlbumFile;
import de.k3b.io.IProgessListener;
import de.k3b.io.StringUtils;
import de.k3b.io.VISIBILITY;

import static de.k3b.android.androFotoFinder.queries.FotoSql.QUERY_TYPE_UNDEFINED;
import static de.k3b.android.androFotoFinder.queries.FotoSql.SQL_COL_DATE_ADDED;
import static de.k3b.android.androFotoFinder.queries.FotoSql.SQL_COL_DATE_TAKEN;
import static de.k3b.android.androFotoFinder.queries.FotoSql.SQL_COL_EXT_MEDIA_TYPE;
import static de.k3b.android.androFotoFinder.queries.FotoSql.SQL_COL_EXT_RATING;
import static de.k3b.android.androFotoFinder.queries.FotoSql.SQL_COL_EXT_TITLE;
import static de.k3b.android.androFotoFinder.queries.FotoSql.SQL_COL_LAST_MODIFIED;
import static de.k3b.android.androFotoFinder.queries.FotoSql.SQL_COL_LAT;
import static de.k3b.android.androFotoFinder.queries.FotoSql.SQL_COL_LON;
import static de.k3b.android.androFotoFinder.queries.FotoSql.SQL_COL_ORIENTATION;
import static de.k3b.android.androFotoFinder.queries.FotoSql.SQL_COL_PATH;
import static de.k3b.android.androFotoFinder.queries.FotoSql.SQL_COL_PK;
import static de.k3b.android.androFotoFinder.queries.FotoSql.SQL_COL_SIZE;
import static de.k3b.android.androFotoFinder.queries.FotoSql.SQL_COL__IMPL_DISPLAY_NAME;
import static de.k3b.android.androFotoFinder.queries.FotoSql.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE_NAME;
import static de.k3b.android.androFotoFinder.tagDB.TagSql.SQL_COL_EXT_DESCRIPTION;
import static de.k3b.android.androFotoFinder.tagDB.TagSql.SQL_COL_EXT_TAGS;
import static de.k3b.android.androFotoFinder.tagDB.TagSql.SQL_COL_EXT_XMP_LAST_MODIFIED_DATE;

/**
 * Access Media Data through stand alone database-table.
 * <p>
 * Since Android-10 (api 29) using sqLite functions as content-provider-columns is not possible anymore.
 * Therefore apm uses a copy of contentprovider MediaStore.Images with same column names.
 */
public class MediaDBRepository implements IMediaRepositoryApi {
    public static final String LOG_TAG = FotoSql.LOG_TAG + "DB";

    // #155
    public static final boolean debugEnabledSqlRefresh = true;

    private static final String MODUL_NAME = MediaContentproviderRepositoryImpl.class.getName();
    private static String currentUpdateReason = null;
    private static long currentUpdateId = 1;
    private static int transactionNumber = 0;
    private final SQLiteDatabase db;

    public MediaDBRepository(SQLiteDatabase db) {
        this.db = db;
    }

    @Override
    public Cursor createCursorForQuery(StringBuilder out_debugMessage, String dbgContext,
                                       QueryParameter parameters, VISIBILITY visibility,
                                       CancellationSignal cancellationSignal) {
        if (visibility != null) FotoSql.setWhereVisibility(parameters, visibility);
        return createCursorForQuery(out_debugMessage, dbgContext,
                parameters.toWhere(), parameters.toAndroidParameters(),
                parameters.toGroupBy(), parameters.toHaving(),
                parameters.toOrderBy(),
                cancellationSignal, parameters.toColumns()
        );
    }

    @Override
    public Cursor createCursorForQuery(StringBuilder out_debugMessage, String dbgContext, String from,
                                       String sqlWhereStatement, String[] sqlWhereParameters,
                                       String sqlSortOrder, CancellationSignal cancellationSignal,
                                       String... sqlSelectColums) {
        return createCursorForQuery(out_debugMessage, dbgContext,
                sqlWhereStatement, sqlWhereParameters,
                null, null,
                sqlSortOrder, cancellationSignal, sqlSelectColums);
    }

    /**
     * every cursor query should go through this. adds logging if enabled
     */
    private Cursor createCursorForQuery(StringBuilder out_debugMessage, String dbgContext,
                                        String sqlWhereStatement, String[] selectionArgs, String groupBy,
                                        String having, String sqlSortOrder,
                                        CancellationSignal cancellationSignal, final String... sqlSelectColums) {
        Cursor query = null;

        Exception excpetion = null;
        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                query = db.query(false, Impl.table, sqlSelectColums, sqlWhereStatement, selectionArgs,
                        groupBy, having, sqlSortOrder, null, cancellationSignal);
            } else {
                query = db.query(false, Impl.table, sqlSelectColums, sqlWhereStatement, selectionArgs,
                        groupBy, having, sqlSortOrder, null);
            }

        } catch (Exception ex) {
            excpetion = ex;
        } finally {
            if ((excpetion != null) || Global.debugEnabledSql || (out_debugMessage != null)) {
                final int count = (query == null) ? 0 : query.getCount();
                StringBuilder message = StringUtils.appendMessage(out_debugMessage, excpetion,
                        dbgContext, MODUL_NAME +
                                ".createCursorForQuery:\n",
                        QueryParameter.toString(sqlSelectColums, null, Impl.table, sqlWhereStatement,
                                selectionArgs, sqlSortOrder, count));
                if (out_debugMessage == null) {
                    Log.i(LOG_TAG, message.toString(), excpetion);
                } // else logging is done by caller
            }
        }

        return query;
    }

    @Override
    public int execUpdate(String dbgContext, long id, ContentValues values) {
        return exexUpdateImpl(dbgContext, values, FotoSql.FILTER_COL_PK, new String[]{Long.toString(id)});
    }

    @Override
    public int execUpdate(String dbgContext, String path, ContentValues values, VISIBILITY visibility) {
        return exexUpdateImpl(dbgContext, values, FotoSql.getFilterExprPathLikeWithVisibility(visibility), new String[]{path});
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

        int modifyCount = execUpdate(dbgContext, dbUpdateFilterJpgFullPathName,
                values, visibility);

        if (modifyCount == 0) {
            // update failed (probably becauce oldFullPathName not found. try insert it.
            FotoSql.addDateAdded(values);

            Uri uriWithId = execInsert(dbgContext, values);
            result = FotoSql.getId(uriWithId);
        }
        return result;
    }

    @Override
    public int exexUpdateImpl(String dbgContext, ContentValues values, String sqlWhere, String[] selectionArgs) {
        int result = -1;
        Exception excpetion = null;
        try {
            result = db.update(Impl.table, values, sqlWhere, selectionArgs);
            if (result != 0) {
                currentUpdateId++;
                currentUpdateReason = dbgContext;
            }
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

    @Override
    public ContentValues getDbContent(long id) {
        Cursor c = null;
        try {
            c = this.createCursorForQuery(null, "getDbContent",
                    Impl.table, FotoSql.FILTER_COL_PK, new String[]{"" + id}, null, null, "*");
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

    /**
     * every database insert should go through this. adds logging if enabled
     *
     * @param dbgContext
     * @param values
     */
    @Override
    public Uri execInsert(String dbgContext, ContentValues values) {
        long result = 0;
        Exception excpetion = null;
        try {
            // on my android-4.4 insert with media_type=1001 (private) does insert with media_type=1 (image)
            result = db.insert(Impl.table, null, values);
            if (result > 0) {
                currentUpdateId++;
                currentUpdateReason = dbgContext;
            }

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
        return Uri.parse("content://apm/photo/" + result);
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
                        values, lastUsedWhereClause, lastSelectionArgs);

                lastUsedWhereClause = FotoSql.SQL_COL_PATH + " is null";
                lastSelectionArgs = null;
                delCount = db.delete(Impl.table, lastUsedWhereClause, lastSelectionArgs);
                if (Global.debugEnabledSql || LibGlobal.debugEnabledJpg) {
                    Log.i(LOG_TAG, dbgContext + "-b: " +
                            MODUL_NAME +
                            ".deleteMedia delete\n" +
                            QueryParameter.toString(null, null, FotoSqlBase.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE_NAME,
                                    lastUsedWhereClause, lastSelectionArgs, null, delCount));
                }
            } else {
                delCount = db.delete(Impl.table, lastUsedWhereClause, lastSelectionArgs);
                if (Global.debugEnabledSql || LibGlobal.debugEnabledJpg) {
                    Log.i(LOG_TAG, dbgContext + ": " +
                            MODUL_NAME +
                            ".deleteMedia\ndelete " +
                            QueryParameter.toString(null, null,
                                    FotoSqlBase.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE_NAME,
                                    lastUsedWhereClause, lastSelectionArgs, null, delCount));
                }
            }
            if (delCount > 0) {
                currentUpdateId++;
                currentUpdateReason = dbgContext;
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

    @Override
    public long getCurrentUpdateId() {
        return currentUpdateId;
    }

    @Override
    public boolean mustRequery(long updateId) {
        final boolean modified = currentUpdateId != updateId;
        if (modified && MediaDBRepository.debugEnabledSqlRefresh) {
            Log.i(MediaDBRepository.LOG_TAG, "mustRequery: true because of " + currentUpdateReason);
        }
        return modified;
    }

    @Override
    public void beginTransaction() {
        if (Global.debugEnabledSql) {
            Log.i(LOG_TAG, "beginTransaction #" + (++transactionNumber));
        }

        db.beginTransaction();
    }

    @Override
    public void setTransactionSuccessful() {
        if (Global.debugEnabledSql) {
            Log.i(LOG_TAG, "setTransactionSuccessful #" + transactionNumber);
        }
        db.setTransactionSuccessful();
    }

    @Override
    public void endTransaction() {
        if (Global.debugEnabledSql) {
            Log.i(LOG_TAG, "endTransaction #" + transactionNumber);
        }
        db.endTransaction();
    }

    public static class Impl {
        /**
         * SQL to create copy of contentprovider MediaStore.Images.
         * copied from android-4.4 android database. Removed columns not used
         */
        public static final String[] DDL = new String[]{
                "DROP TABLE IF EXISTS \"files\"",
                "CREATE TABLE \"files\" (\n" +
                        "\t_id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                        "\t_size INTEGER,\n" +
                        "\tdate_added INTEGER,\n" +
                        "\tdate_modified INTEGER,\n" +
                        "\tdatetaken INTEGER,\n" +
                        "\torientation INTEGER,\n" +
                        "\tduration INTEGER,\n" +
                        "\tbookmark INTEGER,\n" +
                        "\tmedia_type INTEGER,\n" +
                        "\twidth INTEGER,\n" +
                        "\theight INTEGER,\n" +

                        "\t_data TEXT UNIQUE COLLATE NOCASE,\n" +
                        "\ttitle TEXT,\n" +
                        "\tdescription TEXT,\n" +
                        "\t_display_name TEXT,\n" +
                        "\tmime_type TEXT,\n" +
                        "\ttags TEXT,\n" +

                        "\tlatitude DOUBLE,\n" +
                        "\tlongitude DOUBLE\n" +
                        "\t )",
                "CREATE INDEX media_type_index ON files(media_type)",
                "CREATE INDEX path_index ON files(_data)",
                "CREATE INDEX sort_index ON files(datetaken ASC, _id ASC)",
                "CREATE INDEX title_idx ON files(title)",
        };

        public static final String table = "files";
        // same colum order as in DDL
        private static final String[] USED_MEDIA_COLUMNS = new String[]{
                // INTEGER 0 .. 10
                SQL_COL_PK,
                SQL_COL_DATE_ADDED,
                SQL_COL_LAST_MODIFIED,
                SQL_COL_SIZE,
                SQL_COL_DATE_TAKEN,
                SQL_COL_ORIENTATION,
                SQL_COL_EXT_XMP_LAST_MODIFIED_DATE, // duration
                SQL_COL_EXT_RATING, // bookmark
                SQL_COL_EXT_MEDIA_TYPE,
                MediaStore.MediaColumns.WIDTH,
                MediaStore.MediaColumns.HEIGHT,

                // TEXT 11 .. 16
                SQL_COL_PATH, // _data
                SQL_COL_EXT_TITLE,
                SQL_COL_EXT_DESCRIPTION,
                SQL_COL__IMPL_DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
                SQL_COL_EXT_TAGS,

                // DOUBLE 17..18
                SQL_COL_LAT,
                SQL_COL_LON,
        };

        private static final int intMin = 0;
        private static final int intMax = 10;
        private static final int txtMin = 11;
        private static final int txtMax = 16;
        private static final int dblMin = 17;
        private static final int dblMax = 18;

        private static final int colID = 0;
        private static final int colDATE_ADDED = 1;
        private static final int colLAST_MODIFIED = 2;
        private static final String FILTER_EXPR_AFFECTED_FILES
                = "(" + FotoSql.FILTER_EXPR_PRIVATE_PUBLIC
                + " OR " + SQL_COL_PATH + " like '%" + AlbumFile.SUFFIX_VALBUM + "' "
                + " OR " + SQL_COL_PATH + " like '%" + AlbumFile.SUFFIX_QUERY + "' "
                + ")";
        private static final QueryParameter queryGetAllColumns = new QueryParameter()
                .setID(QUERY_TYPE_UNDEFINED)
                .addColumn(USED_MEDIA_COLUMNS)
                .addFrom(SQL_TABLE_EXTERNAL_CONTENT_URI_FILE_NAME)
                .addWhere(FILTER_EXPR_AFFECTED_FILES);

        private static boolean isLomg(int index) {
            return index >= intMin && index <= intMax;
        }

        // private Object get(Cursor cursor, columIndex)

        private static boolean isString(int index) {
            return index >= txtMin && index <= txtMax;
        }

        private static boolean isDouble(int index) {
            return index >= dblMin && index <= dblMax;
        }

        private static String getSqlInsertWithParams() {
            StringBuilder sql = new StringBuilder();

            sql.append("INSERT INTO ").append(table).append("(").append(USED_MEDIA_COLUMNS[0]);
            for (int i = 1; i < USED_MEDIA_COLUMNS.length; i++) {
                sql.append(", ").append(USED_MEDIA_COLUMNS[i]);
            }
            sql.append(") VALUES (?");
            for (int i = 1; i < USED_MEDIA_COLUMNS.length; i++) {
                sql.append(", ?");
            }
            sql.append(")");
            return sql.toString();
        }

        private static String getSqlUpdateWithParams() {
            StringBuilder sql = new StringBuilder();

            sql.append("UPDATE ").append(table).append(" SET ");
            for (int i = 1; i < USED_MEDIA_COLUMNS.length; i++) {
                if (i > 1) sql.append(", ");
                sql.append(USED_MEDIA_COLUMNS[i]).append("=?");
            }
            sql.append(" WHERE ").append(USED_MEDIA_COLUMNS[0]).append("=?");
            return sql.toString();
        }

        private static int bindAndExecUpdate(Cursor c, SQLiteStatement sql) {
            sql.clearBindings();

            // sql where
            sql.bindLong(dblMax + 1, c.getLong(intMin));

            for (int i = intMin + 1; i <= intMax; i++) {
                if (!c.isNull(i)) {
                    sql.bindLong(i, c.getLong(i));
                }
            }
            for (int i = txtMin; i <= txtMax; i++) {
                if (!c.isNull(i)) {
                    sql.bindString(i, c.getString(i));
                }
            }
            for (int i = dblMin; i <= dblMax; i++) {
                if (!c.isNull(i)) {
                    sql.bindDouble(i, c.getDouble(i));
                }
            }
            return sql.executeUpdateDelete();
        }

        private static void bindAndExecInsert(Cursor c, SQLiteStatement sql) {
            sql.clearBindings();

            for (int i = intMin; i <= intMax; i++) {
                if (!c.isNull(i)) {
                    sql.bindLong(i + 1, c.getLong(i));
                }
            }
            for (int i = txtMin; i <= txtMax; i++) {
                if (!c.isNull(i)) {
                    sql.bindString(i + 1, c.getString(i));
                }
            }
            for (int i = dblMin; i <= dblMax; i++) {
                if (!c.isNull(i)) {
                    sql.bindDouble(i + 1, c.getDouble(i));
                }
            }
            sql.executeInsert();
        }

        private static ContentValues getContentValues(Cursor cursor, ContentValues destination) {
            destination.clear();
            int colCount = cursor.getColumnCount();
            String columnName;
            for (int i = 0; i < colCount; i++) {
                columnName = cursor.getColumnName(i);
                if (cursor.isNull(i)) {
                    destination.putNull(columnName);
                } else if (isLomg(i)) {
                    destination.put(columnName, cursor.getLong(i));
                } else if (isString(i)) {
                    destination.put(columnName, cursor.getString(i));
                } else if (isDouble(i)) {
                    destination.put(columnName, cursor.getDouble(i));
                }
            }
            return destination;
        }

        public static void clearMedaiCopy(SQLiteDatabase db) {
            try {
                db.execSQL("DROP TABLE " + table);
            } catch (Exception ex) {
                // Log.e(LOG_TAG, "FotoSql.execGetFotoPaths() Cannot get path from: " + FotoSql.SQL_COL_PATH + " like '" + pathFilter +"'", ex);
            } finally {
            }
        }


        public static int updateMedaiCopy(Context context, SQLiteDatabase db, Date lastUpdate, IProgessListener progessListener) {
            int progress = 0;
            java.util.Date startTime = new java.util.Date();

            QueryParameter query = queryGetAllColumns;
            long _lastUpdate = (lastUpdate != null) ? (lastUpdate.getTime() / 1000L) : 0L;

            if (_lastUpdate != 0) {
                query = new QueryParameter().getFrom(queryGetAllColumns);
                FotoSql.addWhereDateModifiedMinMax(query, _lastUpdate, 0);
                // FotoSql.createCursorForQuery()
            }
            Cursor c = null;
            SQLiteStatement sqlInsert = null;
            SQLiteStatement sqlUpdate = null;
            SQLiteStatement lastSql = null;
            boolean isUpdate = false;
            int itemCount = 0;
            int insertCout = 0;
            int updateCount = 0;
            // ContentValues contentValues = new ContentValues();
            try {
                db.beginTransaction(); // Performance boost: all db-inserts/updates in one transaction

                if (progessListener != null) progessListener.onProgress(progress, 0,
                        context.getString(R.string.load_db_menu_title));

                c = MediaContentproviderRepositoryImpl.createCursorForQuery(null, "updateMedaiCopy-source", context,
                        query, null, null);
                itemCount = c.getCount();

                sqlInsert = db.compileStatement(getSqlInsertWithParams());
                sqlUpdate = db.compileStatement(getSqlUpdateWithParams());
                while (c.moveToNext()) {
                    // getContentValues(c, contentValues);

                    isUpdate = (c.getLong(colDATE_ADDED) <= _lastUpdate);

                    if (isUpdate) {
                        updateCount++;
                        lastSql = sqlUpdate;
                        isUpdate = bindAndExecUpdate(c, sqlUpdate) > 0;
                        // 0 affected update rows: must insert
                    }

                    if (!isUpdate) {
                        insertCout++;
                        lastSql = sqlInsert;
                        bindAndExecInsert(c, sqlInsert);
                    }

                    lastSql = null;
                    // save(db, c, contentValues, _lastUpdate);
                    if ((progessListener != null) && (progress % 100) == 0) {
                        if (!progessListener.onProgress(progress, itemCount, context.getString(R.string.scanner_update_result_format, progress))) {
                            // canceled in gui thread
                            return -progress;
                        }
                    }
                    progress++;
                }
                db.setTransactionSuccessful(); // This commits the transaction if there were no exceptions
                if (Global.debugEnabledSql) {
                    java.util.Date endTime = new java.util.Date();
                    final String message = "MediaDBRepository.updateMedaiCopy(inserted:" + insertCout +
                            ", updated:" + updateCount +
                            ", toal:" + progress +
                            " / " + itemCount +
                            ") in " + ((endTime.getTime() - startTime.getTime()) / 1000) +
                            " Secs";
                    Log.i(LOG_TAG, message);
                }
            } catch (Exception ex) {
                java.util.Date endTime = new java.util.Date();
                final String message = "MediaDBRepository.updateMedaiCopy(inserted:" + insertCout +
                        ", updated:" + updateCount +
                        ", toal:" + progress +
                        " / " + itemCount +
                        ") in " + ((endTime.getTime() - startTime.getTime()) / 1000) +
                        " Secs";
                Log.e(LOG_TAG, "Cannot insert/update: " + lastSql + " from " + c + " in " + message, ex);
            } finally {
                sqlInsert.close();
                sqlUpdate.close();
                db.endTransaction();
                if (c != null) c.close();
            }

            if (Global.debugEnabled) {
                // Log.d(LOG_TAG, "FotoSql.execGetFotoPaths() result count=" + result.size());
            }
            return progress;
        }

        private static void save(SQLiteDatabase db, Cursor c, ContentValues contentValues, long lastUpdate) {
            boolean isNew = (c.getLong(colDATE_ADDED) > lastUpdate);

            if (isNew) {
                db.insert(table, null, contentValues);
            } else {
                String[] params = new String[]{"" + c.getLong(colID)};
                contentValues.remove(SQL_COL_PK);
                db.update(table, contentValues, FotoSql.FILTER_COL_PK, params);
            }
        }
    }
}

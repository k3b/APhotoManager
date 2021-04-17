/*
 * Copyright (c) 2015-2021 by k3b.
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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;

import java.sql.Date;
import java.util.Calendar;

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
    public static final boolean DEBUG_ENABLED_SQL_REFRESH = true;

    private static final String MODUL_NAME = MediaDBRepository.class.getSimpleName();
    private static String currentUpdateReason = null;
    private static long currentUpdateId = 1;
    private static int transactionNumber = 0;
    private final SQLiteDatabase db;

    public MediaDBRepository(SQLiteDatabase db) {
        this.db = db;
    }

    @Override
    public Cursor createCursorForQuery(StringBuilder outDebugMessage, String dbgContext,
                                       QueryParameter parameters, VISIBILITY visibility,
                                       CancellationSignal cancellationSignal) {
        if (visibility != null) {
            FotoSql.setWhereVisibility(parameters, visibility);
        }
        return createCursorForQuery(outDebugMessage, dbgContext,
                parameters.toWhere(), parameters.toAndroidParameters(),
                parameters.toGroupBy(), parameters.toHaving(),
                parameters.toOrderBy(),
                cancellationSignal, parameters.toColumns()
        );
    }

    @Override
    public Cursor createCursorForQuery(StringBuilder outDebugMessage, String dbgContext, String from,
                                       String sqlWhereStatement, String[] sqlWhereParameters,
                                       String sqlSortOrder, CancellationSignal cancellationSignal,
                                       String... sqlSelectColums) {
        return createCursorForQuery(outDebugMessage, dbgContext,
                sqlWhereStatement, sqlWhereParameters,
                null, null,
                sqlSortOrder, cancellationSignal, sqlSelectColums);
    }

    /**
     * every cursor query should go through this. adds logging if enabled
     */
    private Cursor createCursorForQuery(StringBuilder outDebugMessage, String dbgContext,
                                        String sqlWhereStatement, String[] selectionArgs, String groupBy,
                                        String having, String sqlSortOrder,
                                        CancellationSignal cancellationSignal, final String... sqlSelectColums) {
        Cursor query = null;

        Exception excpetion = null;
        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                query = db.query(false, Impl.DATABASE_TABLE_NAME, sqlSelectColums, sqlWhereStatement, selectionArgs,
                        groupBy, having, sqlSortOrder, null, cancellationSignal);
            } else {
                query = db.query(false, Impl.DATABASE_TABLE_NAME, sqlSelectColums, sqlWhereStatement, selectionArgs,
                        groupBy, having, sqlSortOrder, null);
            }

        } catch (Exception ex) {
            excpetion = ex;
        } finally {
            if ((excpetion != null) || Global.debugEnabledSql || (outDebugMessage != null)) {
                final int count = (query == null) ? 0 : query.getCount();
                StringBuilder message = StringUtils.appendMessage(outDebugMessage, excpetion,
                        dbgContext, MODUL_NAME +
                                ".createCursorForQuery:\n",
                        QueryParameter.toString(sqlSelectColums, null, Impl.DATABASE_TABLE_NAME, sqlWhereStatement,
                                selectionArgs, sqlSortOrder, count));
                if (outDebugMessage == null) {
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
     * return id of inserted item or updateSuccessValue if update
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
            result = db.update(Impl.DATABASE_TABLE_NAME, values, sqlWhere, selectionArgs);
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
    public ContentValues getDbContent(Long idOrNull, String filePathOrNull) {
        if (idOrNull != null || filePathOrNull != null) {
            Cursor c = null;
            try {
                String selection;
                String[] selectionArgs;
                if (filePathOrNull == null) {
                    selection = FotoSql.FILTER_COL_PK;
                    selectionArgs = new String[]{"" + idOrNull};
                } else if (idOrNull == null) {
                    selection = FotoSql.FILTER_EXPR_PATH_LIKE;
                    selectionArgs = new String[]{filePathOrNull};
                } else {
                    selection = FotoSql.FILTER_COL_PK + " or " + FotoSql.FILTER_EXPR_PATH_LIKE;
                    selectionArgs = new String[]{"" + idOrNull, filePathOrNull};
                }
                c = this.createCursorForQuery(null, "getDbContent",
                        Impl.DATABASE_TABLE_NAME, selection, selectionArgs, null, null, "*");
                if (c.moveToNext()) {
                    ContentValues values = new ContentValues();
                    DatabaseUtils.cursorRowToContentValues(c, values);
                    return values;
                }
            } catch (Exception ex) {
                Log.e(LOG_TAG, MODUL_NAME +
                        ".getDbContent(id=" + idOrNull + ", path='" + filePathOrNull + "') failed", ex);
            } finally {
                if (c != null) c.close();
            }
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
            result = db.insertWithOnConflict(Impl.DATABASE_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            if (result == -1) {
                return null;
            }

            currentUpdateId++;
            currentUpdateReason = dbgContext;
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
                delCount = db.delete(Impl.DATABASE_TABLE_NAME, lastUsedWhereClause, lastSelectionArgs);
                if (Global.debugEnabledSql || LibGlobal.debugEnabledJpg) {
                    Log.i(LOG_TAG, dbgContext + "-b: " +
                            MODUL_NAME +
                            ".deleteMedia delete\n" +
                            QueryParameter.toString(null, null, FotoSqlBase.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE_NAME,
                                    lastUsedWhereClause, lastSelectionArgs, null, delCount));
                }
            } else {
                delCount = db.delete(Impl.DATABASE_TABLE_NAME, lastUsedWhereClause, lastSelectionArgs);
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
        if (modified && MediaDBRepository.DEBUG_ENABLED_SQL_REFRESH) {
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

    /**
     * generic method to get values from current MediaDBApi-Implementation
     *
     * @param fullFilePathFilter
     * @param destination
     * @param dbgContext
     * @return
     */
    public static ContentValues getContentValues(String fullFilePathFilter, ContentValues destination, String dbgContext) {
        final String meldung = MODUL_NAME + ".getContentValues(" + dbgContext + "," + fullFilePathFilter + ")";
        QueryParameter query = new QueryParameter().addColumn(MediaDBRepository.Impl.USED_MEDIA_COLUMNS);
        query.removeFirstColumnThatContains(FotoSql.SQL_COL_PK);
        FotoSql.setWhereFileNames(query, fullFilePathFilter);

        Cursor c = null;

        try {
            c = FotoSql.getMediaDBApi().createCursorForQuery(null, meldung, query, null, null);
            if (c.moveToNext()) {
                if (destination == null) {
                    destination = new ContentValues();
                }
                return Impl.getContentValues(c, destination);
            }
        } catch (Exception ex) {
            Log.e(LOG_TAG, meldung +
                    " error :", ex);
        } finally {
            if (c != null) c.close();
        }

        return null;
    }


    public static void saveSyncStats(Context context, long maxDateAddedSecs, long maxDateUpdatedSecs) {
        SharedPreferences prefsInstance = PreferenceManager
                .getDefaultSharedPreferences(context.getApplicationContext());

        SharedPreferences.Editor prefs = prefsInstance.edit();
        if (maxDateUpdatedSecs > 0) prefs.putLong("maxDateUpdatedSecs", maxDateUpdatedSecs + 1);
        if (maxDateAddedSecs > 0) prefs.putLong("maxDateAddedSecs", maxDateAddedSecs + 1);
        prefs.apply();
    }

    public static class Impl {
        public static final String DATABASE_TABLE_NAME = "files";
        public static final String DATABASE_TABLE_NAME_BACKUP = "backup";
        /**
         * SQL to create copy of contentprovider MediaStore.Images.
         * copied from android-4.4 android database. Removed columns not used
         */
        protected static final String[] DDL = new String[]{
                "DROP TABLE IF EXISTS \"" + DATABASE_TABLE_NAME + "\"",
                "CREATE TABLE \"" + DATABASE_TABLE_NAME + "\" (\n" +
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
                "CREATE INDEX media_type_index ON " + DATABASE_TABLE_NAME + "(media_type)",
                "CREATE INDEX path_index ON " + DATABASE_TABLE_NAME + "(_data)",
                "CREATE INDEX sort_index ON " + DATABASE_TABLE_NAME + "(datetaken ASC, _id ASC)",
                "CREATE INDEX title_idx ON " + DATABASE_TABLE_NAME + "(title)",
        };
        protected static final String[] RESTORE_FROM_BACKUP = new String[]{
                "UPDATE " + DATABASE_TABLE_NAME + "\n" +
                        "SET latitude = (SELECT latitude FROM " + DATABASE_TABLE_NAME_BACKUP + "  WHERE _data = " + DATABASE_TABLE_NAME + "._data)\n" +
                        ", latitude = (SELECT latitude FROM " + DATABASE_TABLE_NAME_BACKUP + "  WHERE _data = " + DATABASE_TABLE_NAME + "._data)\n" +
                        ", longitude = (SELECT longitude FROM " + DATABASE_TABLE_NAME_BACKUP + "  WHERE _data = " + DATABASE_TABLE_NAME + "._data)\n" +
                        ", tags = (SELECT tags FROM " + DATABASE_TABLE_NAME_BACKUP + "  WHERE _data = " + DATABASE_TABLE_NAME + "._data)\n" +
                        ", duration = (SELECT duration FROM " + DATABASE_TABLE_NAME_BACKUP + "  WHERE _data = " + DATABASE_TABLE_NAME + "._data)\n" +
                        ", title = (SELECT title FROM " + DATABASE_TABLE_NAME_BACKUP + "  WHERE _data = " + DATABASE_TABLE_NAME + "._data)\n" +
                        ", description = (SELECT description FROM " + DATABASE_TABLE_NAME_BACKUP + "  WHERE _data = " + DATABASE_TABLE_NAME + "._data)\n" +
                        ", bookmark = (SELECT bookmark FROM " + DATABASE_TABLE_NAME_BACKUP + "  WHERE _data = " + DATABASE_TABLE_NAME + "._data)",
        };
        private static final String HAS_DATA = "latitude is not null or tags is not null or bookmark is not null";
        private static final String MY_COLUMNS = " _id, _data, latitude, longitude, tags, duration, bookmark, title, description ";
        private static final String SELECT_FROM_FILES = " SELECT" + MY_COLUMNS + " from " + DATABASE_TABLE_NAME + "" + " where " + HAS_DATA;
        protected static final String[] CREATE_BACKUP = new String[]{
                "DROP TABLE IF EXISTS \"" + DATABASE_TABLE_NAME_BACKUP + "\"",
                "CREATE TABLE \"" + DATABASE_TABLE_NAME_BACKUP + "\" AS" + SELECT_FROM_FILES,
                "CREATE INDEX bu_path_index ON " + DATABASE_TABLE_NAME_BACKUP + "(_data)",
        };
        protected static final String[] UPDATE_BACKUP = new String[]{
                "DELETE FROM " + DATABASE_TABLE_NAME_BACKUP + " where exists " +
                        "(select _data from " + DATABASE_TABLE_NAME +
                        " WHERE _data = " + DATABASE_TABLE_NAME_BACKUP + "._data" + " AND (" + HAS_DATA + "))",
                "INSERT INTO " + DATABASE_TABLE_NAME_BACKUP + "(" + MY_COLUMNS + ")" + SELECT_FROM_FILES,
        };

        private static final int COL_INT_MIN = 0;
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
        private static final int COL_INT_MAX = 10;
        private static final int COL_TXT_MIN = 11;
        private static final int COL_TXT_MAX = 16;
        private static final int COL_DBL_MIN = 17;
        private static final int COL_DBL_MAX = 18;
        // COL_ID must be col#0 to macke update where id=? work
        private static final int COL_ID = COL_INT_MIN; // = 0
        private static final int COL_DATE_ADDED = 1;
        private static final int COL_LAST_MODIFIED = 2;
        // do not copy 6==SQL_COL_EXT_XMP_LAST_MODIFIED_DATE
        // on android-10 and above wich is used as rescan marker
        private static final int COL_INT_COPY_IGNORE = (Global.useAo10MediaImageDbReplacement) ? 6 : -1;

        private Impl() {
        }

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
        private static long nextMonthTimeInSecs;

        private static boolean isLomg(int index) {
            return index >= COL_INT_MIN && index <= COL_INT_MAX;
        }

        // private Object get(Cursor cursor, columIndex)

        private static boolean isString(int index) {
            return index >= COL_TXT_MIN && index <= COL_TXT_MAX;
        }

        private static boolean isDouble(int index) {
            return index >= COL_DBL_MIN && index <= COL_DBL_MAX;
        }

        private static String getSqlInsertWithParams() {
            StringBuilder sql = new StringBuilder();

            sql.append("INSERT INTO ").append(DATABASE_TABLE_NAME).append("(").append(USED_MEDIA_COLUMNS[0]);
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

            sql.append("UPDATE ").append(DATABASE_TABLE_NAME).append(" SET ");
            for (int i = 1; i < USED_MEDIA_COLUMNS.length; i++) {
                if (i > 1) sql.append(", ");
                sql.append(USED_MEDIA_COLUMNS[i]).append("=?");
            }
            sql.append(" WHERE ").append(USED_MEDIA_COLUMNS[0]).append("=?");
            return sql.toString();
        }

        private static int bindAndExecUpdate(Cursor c, SQLiteStatement sql, long dateAdded, long dateUpdated) {
            sql.clearBindings();

            // sql where
            sql.bindLong(COL_DBL_MAX + 1, c.getLong(COL_ID));

            for (int i = COL_INT_MIN + 1; i <= COL_INT_MAX; i++) {
                if (COL_INT_COPY_IGNORE != i && !c.isNull(i)) {
                    sql.bindLong(i, c.getLong(i));
                }
            }
            for (int i = COL_TXT_MIN; i <= COL_TXT_MAX; i++) {
                if (!c.isNull(i)) {
                    sql.bindString(i, c.getString(i));
                }
            }
            for (int i = COL_DBL_MIN; i <= COL_DBL_MAX; i++) {
                if (!c.isNull(i)) {
                    sql.bindDouble(i, c.getDouble(i));
                }
            }
            if (dateAdded != 0) {
                sql.bindLong(COL_DATE_ADDED, dateAdded);
            }
            if (dateUpdated != 0) {
                sql.bindLong(COL_LAST_MODIFIED, dateUpdated);
            }
            return sql.executeUpdateDelete();
        }

        private static void bindAndExecInsert(Cursor c, SQLiteStatement sql, long dateAdded, long dateUpdated) {
            sql.clearBindings();

            for (int i = COL_INT_MIN; i <= COL_INT_MAX; i++) {
                if (COL_INT_COPY_IGNORE != i && !c.isNull(i)) {
                    sql.bindLong(i + 1, c.getLong(i));
                }
            }
            for (int i = COL_TXT_MIN; i <= COL_TXT_MAX; i++) {
                if (!c.isNull(i)) {
                    sql.bindString(i + 1, c.getString(i));
                }
            }
            for (int i = COL_DBL_MIN; i <= COL_DBL_MAX; i++) {
                if (!c.isNull(i)) {
                    sql.bindDouble(i + 1, c.getDouble(i));
                }
            }
            if (dateAdded != 0) {
                sql.bindLong(COL_DATE_ADDED + 1, dateAdded);
            }
            if (dateUpdated != 0) {
                sql.bindLong(COL_LAST_MODIFIED + 1, dateUpdated);
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
                db.execSQL("DROP TABLE " + DATABASE_TABLE_NAME);
            } catch (Exception ex) {
                // Log.e(LOG_TAG, "FotoSql.execGetFotoPaths() Cannot get path from: " + FotoSql.SQL_COL_PATH + " like '" + pathFilter +"'", ex);
            }
        }

        public static int updateMediaCopy(
                Context context, SQLiteDatabase db,
                IProgessListener progessListener) {
            SharedPreferences prefsInstance = PreferenceManager
                    .getDefaultSharedPreferences(context.getApplicationContext());
            long maxDateAddedSecs = prefsInstance.getLong("maxDateAddedSecs", 0l);
            return updateMediaCopy(context, db, null, new Date(maxDateAddedSecs * FotoSql.LAST_MODIFIED_FACTOR), progessListener);
        }

        public static int updateMediaCopy(
                Context context, SQLiteDatabase db,
                Date filterLastUpdateMin, Date filterLastAddedMin, IProgessListener progessListener) {
            int progress = 0;
            java.util.Date startTime = new java.util.Date();

            QueryParameter query = new QueryParameter().getFrom(queryGetAllColumns);

            Calendar nextMonth = Calendar.getInstance();
            nextMonth.add(Calendar.MONTH, 1);
            nextMonthTimeInSecs = nextMonth.getTimeInMillis() / FotoSql.LAST_MODIFIED_FACTOR;

            long filterLastUpdateMinInMillis = (filterLastUpdateMin != null) ? (filterLastUpdateMin.getTime()) : 0L;
            if (filterLastUpdateMinInMillis != 0) {
                FotoSql.addWhereDateModifiedMinMax(query, filterLastUpdateMinInMillis, 0);
            }

            long filterLastAddedMinInMillis = (filterLastAddedMin != null) ? (filterLastAddedMin.getTime()) : 0L;
            if (filterLastAddedMinInMillis != 0) {
                FotoSql.addWhereDateAddedMinMax(query, filterLastAddedMinInMillis, nextMonth.getTimeInMillis());
            }

            Cursor c = null;
            SQLiteStatement sqlInsert = null;
            SQLiteStatement sqlUpdate = null;
            SQLiteStatement lastSql = null;
            boolean isUpdate = false;
            int itemCount = 0;
            int insertCout = 0;
            int updateCount = 0;

            try {
                db.beginTransaction(); // Performance boost: all db-inserts/updates in one transaction

                if (progessListener != null) progessListener.onProgress(progress, 0,
                        context.getString(R.string.load_db_menu_title));

                c = MediaContentproviderRepositoryImpl.createCursorForQuery(null, "updateMedaiCopy-source", context,
                        query, null, null);
                itemCount = c.getCount();

                sqlInsert = db.compileStatement(getSqlInsertWithParams());
                sqlUpdate = db.compileStatement(getSqlUpdateWithParams());
                long maxDateAddedSecs = 0;
                long maxDateUpdatedSecs = 0;
                while (c.moveToNext()) {

                    long curDateAddedSecs = getDateInSecs(c, COL_DATE_ADDED);
                    if (curDateAddedSecs > maxDateAddedSecs) {
                        maxDateAddedSecs = curDateAddedSecs;
                    }
                    isUpdate = (curDateAddedSecs <= filterLastUpdateMinInMillis / FotoSql.LAST_MODIFIED_FACTOR);

                    long curDateUpdatedSecs = getDateInSecs(c, COL_LAST_MODIFIED);
                    if (curDateUpdatedSecs > maxDateUpdatedSecs) {
                        maxDateUpdatedSecs = curDateUpdatedSecs;
                    }

                    if (isUpdate) {
                        lastSql = sqlUpdate;
                        isUpdate = bindAndExecUpdate(c, sqlUpdate, curDateAddedSecs, curDateUpdatedSecs) > 0;
                        // 0 affected update rows: must insert

                        if (isUpdate) {
                            updateCount++;
                        }
                    }

                    if (!isUpdate) {
                        lastSql = sqlInsert;
                        try {
                            bindAndExecInsert(c, sqlInsert, curDateAddedSecs, curDateUpdatedSecs);//!!!
                            insertCout++;
                        } catch (SQLiteConstraintException ignore) {
                            // already in local database, ignore
                        }
                    }

                    lastSql = null;

                    if ((progessListener != null) && (progress % 100) == 0 &&
                            !progessListener.onProgress(progress, itemCount, context.getString(R.string.scanner_update_result_format, progress))) {
                        // canceled in gui thread
                        return -progress;
                    }
                    progress++;
                } // while over all old items
                db.setTransactionSuccessful(); // This commits the transaction if there were no exceptions

                saveSyncStats(context, maxDateAddedSecs, maxDateUpdatedSecs);

                if (Global.debugEnabledSql) {
                    java.util.Date endTime = new java.util.Date();
                    final String message = "MediaDBRepository.updateMedaiCopy(inserted:" + insertCout +
                            ", updated:" + updateCount +
                            ", toal:" + progress +
                            " / " + itemCount +
                            ") in " + ((endTime.getTime() - startTime.getTime()) / FotoSql.LAST_MODIFIED_FACTOR) +
                            " Secs";
                    Log.i(LOG_TAG, message);
                }
            } catch (Exception ex) {
                java.util.Date endTime = new java.util.Date();
                final String message = "MediaDBRepository.updateMedaiCopy(inserted:" + insertCout +
                        ", updated:" + updateCount +
                        ", toal:" + progress +
                        " / " + itemCount +
                        ") in " + ((endTime.getTime() - startTime.getTime()) / FotoSql.LAST_MODIFIED_FACTOR) +
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

        protected static long getDateInSecs(Cursor c, int colPosition) {
            long dateInSecs = (c.isNull(colPosition)) ? 0 : c.getLong(colPosition);
            if (dateInSecs > nextMonthTimeInSecs) {
                // colDATE_ADDED: some apps/libs use milliscs instead of secs. Fix this.
                dateInSecs = dateInSecs / FotoSql.LAST_MODIFIED_FACTOR;
            }
            return dateInSecs;
        }
    }

}

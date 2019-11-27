/*
 * Copyright (c) 2015-2019 by k3b.
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
import android.database.sqlite.SQLiteDatabase;
import android.provider.MediaStore;

import java.sql.Date;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.database.QueryParameter;
import de.k3b.io.AlbumFile;

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
 * Since Android-10 (api 29) using sqLite functions as content-provider-columns is not possible anymore.
 * Therefore apm uses a copy of contentprovider MediaStore.Images with same column names.
 */
public class MediaImageDbReplacement {
    /**
     * SQL to create copy of contentprovider MediaStore.Images.
     * copied from android-4.4 android database. Removed columns not used
     */
    public static final String[] DDL = new String[]{
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
            "CREATE INDEX titlekey_index ON files(title_key)",
            "CREATE INDEX media_type_index ON files(media_type)",
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

    public static int updateMedaiCopy(Context context, SQLiteDatabase db, Date lastUpdate) {
        int changeCount = 0;

        QueryParameter query = queryGetAllColumns;
        long _lastUpdate = (lastUpdate != null) ? (lastUpdate.getTime() / 1000L) : 0L;

        if (_lastUpdate != 0) {
            query = new QueryParameter().getFrom(queryGetAllColumns);
            FotoSql.addWhereDateModifiedMinMax(query, _lastUpdate, 0);
            // FotoSql.createCursorForQuery()
        }
        Cursor c = null;
        ContentValues contentValues = new ContentValues();
        try {
            c = ContentProviderMediaExecuter.createCursorForQuery(null, "execGetFotoPaths(pathFilter)", context,
                    query, null);
            while (c.moveToNext()) {
                getContentValues(c, contentValues);
                save(db, c, contentValues, _lastUpdate);
            }
        } catch (Exception ex) {
            // Log.e(Global.LOG_CONTEXT, "FotoSql.execGetFotoPaths() Cannot get path from: " + FotoSql.SQL_COL_PATH + " like '" + pathFilter +"'", ex);
        } finally {
            if (c != null) c.close();
        }

        if (Global.debugEnabled) {
            // Log.d(Global.LOG_CONTEXT, "FotoSql.execGetFotoPaths() result count=" + result.size());
        }
        return 0;
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

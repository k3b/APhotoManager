/*
 * Copyright (c) 2015-2016 by k3b.
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

import android.database.Cursor;

/**
 * Created by k3b on 15.08.2016.
 */
public class DBUtils {
    /** debug support for logging current cursor content */
    public static String debugCursor(Cursor cursor, int maxRows, String delim, String... colmnNames) {
        StringBuilder result = new StringBuilder();
        if ((cursor != null) && (!cursor.isClosed())) {
            int last = Math.min(maxRows - 1, cursor.getCount() - 1);
            for (int position = 0; position <= last; position ++) {
                result.append("#").append(position);
                cursor.moveToPosition(position);
                for (String col : colmnNames) {
                    result.append(";").append(DBUtils.getString(cursor,col,"???"));
                }
                result.append(delim);
            }
        }
        return result.toString();
    }

    public static boolean isNull(Cursor cursor, String colId, boolean notFoundValue) {
        int columnIndex = (cursor == null) ? -1 : cursor.getColumnIndex(colId);
        return (columnIndex == -1)  ? notFoundValue : cursor.isNull(columnIndex);
    }

    public static String getString(Cursor cursor, String colId, String notFoundValue) {
        int columnIndex = (cursor == null) ? -1 : cursor.getColumnIndex(colId);
        return (columnIndex == -1)  ? notFoundValue : cursor.getString(columnIndex);
    }

    public static long getLong(Cursor cursor, String colId, long notFoundValue) {
        int columnIndex = (cursor == null) ? -1 : cursor.getColumnIndex(colId);
        return (columnIndex == -1)  ? notFoundValue : cursor.getLong(columnIndex);
    }
}

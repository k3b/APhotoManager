/*
 * Copyright (c) 2015-2017 by k3b.
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

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.database.QueryParameter;
import de.k3b.io.IGalleryFilter;
import de.k3b.io.VISIBILITY;

/**
 * Created by k3b on 21.06.2016.
 */
public class FotoThumbSql {
    private static String mDebugPrefix = "FotoThumbSql ";

    // columns that must be avaulable in the Cursor
    public static final String SQL_COL_COUNT = "count";
    public static final String SQL_COL_SIZE = "size";

    public static QueryParameter getQueryImageSizeByPath(String imagePath) {

        return FotoSql.setWhereVisibility(new QueryParameter()
                // .setID(QUERY_TYPE_GROUP_DATE)
                .addColumn(
                        "count(*) as " + SQL_COL_COUNT,
                        "sum(" + FotoSql.SQL_COL_SIZE + ") AS " + SQL_COL_SIZE)
                .addFrom(FotoSql.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE.toString())
                .addWhere(FotoSql.SQL_COL_PATH +
                        " like ?", imagePath + "%"),
                VISIBILITY.PRIVATE_PUBLIC)
                ;
    }

    /** creats statistics row */
    private static String getStatistic(Context context, QueryParameter query, String type, String path, double factor) {
        StringBuilder result = new StringBuilder();
		if (path != null) {
			// http://docs.oracle.com/javase/1.5.0/docs/api/java/util/Formatter.html#syntax
			// ie "Thumbnail[/mnt/sdcard0/] 122 (13.5 MB)"
			String format = "%1$s[%2$s %5$d] #%3$d (%4$01.1f MB)\n";
			// java.text.MessageFormat("The disk \"{1}\" contains {0} file(s).").format(testArgs);
			
			long countThumbInternal = 0;
			double sizeKBThumbInternal  = 0.0;
            long kind = 0;

            Cursor c = null;
            try {
                c = FotoSql.createCursorForQuery(mDebugPrefix + "getStatistic", context, query, VISIBILITY.PRIVATE_PUBLIC);
                if (Global.debugEnabledSql) {
                    Log.i(Global.LOG_CONTEXT, mDebugPrefix + "getStatistic " + c.getCount() +
                            "\n\t" + query.toSqlString());
                }

                if (c.getCount() > 0) {
                    boolean hasKind = c.getColumnCount() > 2;
                    while (c.moveToNext()) {
                        countThumbInternal = c.getLong(0);
                        sizeKBThumbInternal = (factor * c.getLong(1)) / 1048576; // 1MB= 1048576 Bytes. https://en.wikipedia.org/wiki/Megabyte
                        kind = (hasKind) ? c.getLong(2) : 0;

                        result.append(String.format(format, type, path, countThumbInternal, sizeKBThumbInternal, kind));

                    }
                } else {
                    result.append(String.format(format, type, path, countThumbInternal, sizeKBThumbInternal, kind));

                }
            } catch (Exception ex) {
                Log.e(Global.LOG_CONTEXT, mDebugPrefix + "getStatistic() : error executing " + query, ex);
            } finally {
                if (c != null) c.close();
            }

		}
		return result.toString();
	}

    public static String formatDirStatistic(Context context, String imagePath) {
		StringBuilder result = new StringBuilder();

		result
            .append(getStatistic(context, getQueryImageSizeByPath(imagePath), "Image", imagePath, 1.0))
        ;

        return result.toString();
    }

}

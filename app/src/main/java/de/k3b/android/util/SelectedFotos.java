/*
 * Copyright (c) 2015 by k3b.
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

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.queries.QueryParameterParcelable;
import de.k3b.database.SelectedItems;

/**
 * SelectedItems with media support
 * Created by k3b on 03.08.2015.
 */
public class SelectedFotos extends SelectedItems implements Serializable {
    public static File[] getFiles(String[] fileNames) {
         // getFileNames();
        if ((fileNames == null) || (fileNames.length == 0)) return null;

        File[] result = new File[fileNames.length];
        int i = 0;
        for (String name : fileNames) {
            result[i++] = new File(name);
        }

        return result;
    }

    public String[] getFileNames(Activity context) {
        if (!isEmpty()) {
            ArrayList<String> result = new ArrayList<>();

            QueryParameterParcelable parameters = new QueryParameterParcelable(FotoSql.queryDetail);
            FotoSql.setWhereSelection(parameters, this);

            Cursor cursor = null;

            try {
                cursor = requery(context, parameters.toColumns(), parameters.toFrom(), parameters.toAndroidWhere(), parameters.toOrderBy(), parameters.toAndroidParameters());

                int colPath = cursor.getColumnIndex(FotoSql.SQL_COL_DISPLAY_TEXT);
                while (cursor.moveToNext()) {
                    String path = cursor.getString(colPath);
                    result.add(path);
                    int ext = result.lastIndexOf(".");
                    String xmpPath = ((ext >= 0) ? path.substring(0, ext) : path) + ".xmp";
                    if (new File(xmpPath).exists()) {
                        result.add(xmpPath);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            int size = result.size();

            if (size > 0) {
                return result.toArray(new String[size]);
            }
        }
        return null;

    }

    private Cursor requery(final Activity context, final String[] sqlProjection, final String from, final String sqlWhereStatement, final String sqlSortOrder, final String... sqlWhereParameters) {
        Cursor result = context.getContentResolver().query(Uri.parse(from), // Table to query
                sqlProjection,             // Projection to return
                sqlWhereStatement,        // No selection clause
                sqlWhereParameters,       // No selection arguments
                sqlSortOrder              // Default sort order
        );

        return result;

    }

    /** converts imageID to content-uri */
    public static Uri getUri(long imageID) {
        return Uri.parse(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString() + "/" + imageID);
    }



}

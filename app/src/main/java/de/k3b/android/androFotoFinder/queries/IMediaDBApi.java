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
import android.database.Cursor;
import android.net.Uri;

import de.k3b.database.QueryParameter;
import de.k3b.io.VISIBILITY;

/**
 * media database api
 */
public interface IMediaDBApi {
    Cursor createCursorForQuery(
            StringBuilder out_debugMessage, String dbgContext,
            QueryParameter parameters, VISIBILITY visibility);

    Cursor createCursorForQuery(StringBuilder out_debugMessage, String dbgContext, final String from, final String sqlWhereStatement,
                                final String[] sqlWhereParameters, final String sqlSortOrder,
                                final String... sqlSelectColums);

    int execUpdate(String dbgContext, long id, ContentValues values);

    int execUpdate(String dbgContext, String path, ContentValues values, VISIBILITY visibility);

    int exexUpdateImpl(String dbgContext, ContentValues values, String sqlWhere, String[] selectionArgs);

    /**
     * return id of inserted item
     */
    Long insertOrUpdateMediaDatabase(String dbgContext,
                                     String dbUpdateFilterJpgFullPathName,
                                     ContentValues values, VISIBILITY visibility,
                                     Long updateSuccessValue);

    /**
     * every database insert should go through this. adds logging if enabled
     */
    Uri execInsert(String dbgContext, ContentValues values);

    /**
     * Deletes media items specified by where with the option to prevent cascade delete of the image.
     */
    int deleteMedia(String dbgContext, String where, String[] selectionArgs, boolean preventDeleteImageFile);

    ContentValues getDbContent(long id);
}

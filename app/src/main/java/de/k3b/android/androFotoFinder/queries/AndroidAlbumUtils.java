/*
 * Copyright (c) 2018 by k3b.
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
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import de.k3b.android.androFotoFinder.Common;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.tagDB.TagSql;
import de.k3b.android.util.IntentUtil;
import de.k3b.android.util.MediaScanner;
import de.k3b.database.QueryParameter;
import de.k3b.io.AlbumFile;
import de.k3b.io.FileUtils;
import de.k3b.io.GalleryFilterParameter;

/**
 * Handles file io of QueryParameter and GalleryFilterParameter via intent.
 *
 * Created by k3b on 28.03.2018.
 */
public class AndroidAlbumUtils implements Common {
    private static final String mDebugPrefix = AndroidAlbumUtils.class.getSimpleName();

    public static QueryParameter getQueryFromUri(Context context, Uri uri) {
        if ((uri != null) && (context != null) && AlbumFile.isQueryFile(uri.getPath())) {
            try {
                QueryParameter query = QueryParameter.load(context.getContentResolver().openInputStream(uri));
                if (query != null) {
                    insertToMediaDB(context,uri,mDebugPrefix + ".load");
                    return query;
                }
            } catch (IOException e) {
                Log.e(Global.LOG_CONTEXT, mDebugPrefix + ".loadFrom(" + uri +
                        ") failed: " + e.getMessage(), e);
            }
        }
        return null;
    }

    public static GalleryFilterParameter getGalleryFilterParameterFromQueryUri(Context context, Uri uri) {
        QueryParameter query = getQueryFromUri(context, uri);
        if (query != null) {
            return (GalleryFilterParameter) TagSql.parseQueryEx(query, true);
        }
        return null;
    }

    /** get from file-uri via intent.data else from EXTRA_FILTER */
    public static GalleryFilterParameter getFilter(Context context, Intent intent) {
        if (intent == null) return null;

        Uri uri = intent.getData();
        if (uri != null) {
            return getGalleryFilterParameterFromQueryUri(context, uri);
        }
        String filter = intent.getStringExtra(EXTRA_FILTER);
        if (filter != null) {
            return GalleryFilterParameter.parse(filter, new GalleryFilterParameter());
        }
        return null;
    }

    public static void saveGalleryFilterParameterAsQuery(Context context, GalleryFilterParameter filter, Uri uri) {
        final String dbgContext = mDebugPrefix + ".saveTo(" + uri + ")";
        PrintWriter out = null;
        try {
            final QueryParameter query = new QueryParameter();
            TagSql.filter2QueryEx(query, filter, true);

            query.save(context.getContentResolver().openOutputStream(uri, "w"));
            insertToMediaDB(context, uri, dbgContext);
        } catch (IOException e) {
            Log.e(Global.LOG_CONTEXT, dbgContext +
                    " failed: " + e.getMessage(), e);
        } finally {
            FileUtils.close(out, "");
        }
    }

    public static void insertToMediaDB(Context context, Uri uri, String dbgContext) {
        if (IntentUtil.isFileUri(uri)) {
            File f = FileUtils.tryGetCanonicalFile(IntentUtil.getFile(uri));
            if (f != null) {
                ContentValues values = new ContentValues();
                String newAbsolutePath = MediaScanner.setFileFields(values, f);
                values.put(FotoSql.SQL_COL_EXT_MEDIA_TYPE,0);
                FotoSql.insertOrUpdateMediaDatabase(dbgContext, context, newAbsolutePath, values, 1l);
            }
        }
    }
}

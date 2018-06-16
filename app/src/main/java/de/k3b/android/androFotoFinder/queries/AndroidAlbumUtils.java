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
import android.os.Bundle;
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
import de.k3b.io.IGalleryFilter;

/**
 * Handles file io of QueryParameter and GalleryFilterParameter via intent.
 *
 * Created by k3b on 28.03.2018.
 */
public class AndroidAlbumUtils implements Common {
    private static final String mDebugPrefix = AndroidAlbumUtils.class.getSimpleName();

    /** get from savedInstanceState else intent: inten.data=file-uri else EXTRA_QUERY else EXTRA_FILTER */
    public static GalleryFilterParameter getFilterAndRestQuery(
            Context context, Bundle savedInstanceState, Intent intent, QueryParameter resultQueryWithoutFilter,
            boolean ignoreFilter, StringBuilder dbgFilter) {
        if (savedInstanceState != null) {
            return getFilterAndRestQuery(
                    context,
                    null,
                    savedInstanceState.getString(EXTRA_QUERY),
                    savedInstanceState.getString(EXTRA_FILTER),
                    resultQueryWithoutFilter, ignoreFilter, dbgFilter);
        } else if (intent != null) {
            return getFilterAndRestQuery(
                    context,
                    IntentUtil.getUri(intent),
                    intent.getStringExtra(EXTRA_QUERY),
                    intent.getStringExtra(EXTRA_FILTER),
                    resultQueryWithoutFilter, ignoreFilter, dbgFilter);
        }
        return null;
    }

    /** used by folder picker if album-file is picked instead of folder */
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

    public static GalleryFilterParameter getGalleryFilterAndRestQueryFromQueryUri(
            Context context, Uri uri, QueryParameter resultQueryWithoutFilter,
            boolean ignoreFilter, StringBuilder dbgFilter) {
        QueryParameter query = getQueryFromUri(context, uri);
        if (query != null) {
            if ((uri != null) && (dbgFilter != null)) {
                dbgFilter.append("query from uri ").append(uri).append("\n");
            }
            return getFilterAndRestQuery(query, resultQueryWithoutFilter, ignoreFilter, dbgFilter);
        }
        return null;
    }

    private static GalleryFilterParameter getFilterAndRestQuery(
            Context context, Uri uri, String sql, String filter, QueryParameter resultQueryWithoutFilter,
            boolean ignoreFilter, StringBuilder dbgFilter) {
        if (uri != null) {
            return getGalleryFilterAndRestQueryFromQueryUri(context, uri, resultQueryWithoutFilter, ignoreFilter, dbgFilter);
        }

        if ((sql != null) && (dbgFilter != null)) {
            dbgFilter.append("query from ").append(EXTRA_QUERY).append("\n\t").append(sql).append("\n");
        }
        QueryParameter query = QueryParameter.parse(sql);
        final GalleryFilterParameter result = getFilterAndRestQuery(query, resultQueryWithoutFilter, ignoreFilter, dbgFilter);

        if (filter == null) {
            // no own value for filter return parsed filter
            return result;
        }

        if (filter != null) {
            if (dbgFilter != null) {
                dbgFilter.append("filter from ").append(EXTRA_FILTER).append("=").append(filter).append("\n");
            }
            return GalleryFilterParameter.parse(filter, new GalleryFilterParameter());
        }
        return null;
    }

    private static GalleryFilterParameter getFilterAndRestQuery(
            QueryParameter query, QueryParameter resultQueryWithoutFilter,
            boolean ignoreFilter, StringBuilder dbgFilter) {

        final GalleryFilterParameter result = (ignoreFilter) ? null : (GalleryFilterParameter) TagSql.parseQueryEx(query, true);

        if (resultQueryWithoutFilter != null) {
            resultQueryWithoutFilter.clear();
            resultQueryWithoutFilter.getFrom(query);
        }
        return result;
    }

    public static void saveFilterAndQuery(
            Context context, Uri destUri, Intent destIntent, Bundle destBundle,
            IGalleryFilter filter, QueryParameter queryParameter) {
        final String dbgContext = mDebugPrefix + ".saveFilterAndQuery(" + destUri + ")";
        final QueryParameter query = new QueryParameter(queryParameter);
        TagSql.filter2QueryEx(query, filter, false);

        if (destUri != null) {
            PrintWriter out = null;
            try {
                query.save(context.getContentResolver().openOutputStream(destUri, "w"));
                insertToMediaDB(context, destUri, dbgContext);
            } catch (IOException e) {
                Log.e(Global.LOG_CONTEXT, dbgContext +
                        " failed: " + e.getMessage(), e);
            } finally {
                FileUtils.close(out, "");
            }
        }

        if (destBundle != null) {
            if (queryParameter != null) destBundle.putString(EXTRA_QUERY, query.toReParseableString());
            if (filter != null) destBundle.putString(EXTRA_FILTER, filter.toString());
        }
        if (destIntent != null) {
            if (queryParameter != null) destIntent.putExtra(EXTRA_QUERY, query.toReParseableString());
            if (filter != null) destIntent.putExtra(EXTRA_FILTER, filter.toString());
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

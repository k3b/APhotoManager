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
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import de.k3b.io.StringUtils;

/**
 * Handles file io of QueryParameter and GalleryFilterParameter via intent.
 *
 * Created by k3b on 28.03.2018.
 */
public class AndroidAlbumUtils implements Common {
    private static final String mDebugPrefix = AndroidAlbumUtils.class.getSimpleName();

    /** get from savedInstanceState else intent: inten.data=file-uri else EXTRA_QUERY else EXTRA_FILTER */
    @Deprecated
    public static GalleryFilterParameter getFilterAndRestQuery(
            @NonNull Context context, @Nullable Bundle savedInstanceState,
            @Nullable Intent intent, @Nullable QueryParameter resultQueryWithoutFilter,
            boolean ignoreFilter, @Nullable StringBuilder dbgFilter) {
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

    /** get from savedInstanceState else intent else sharedPref: inten.data=file-uri else EXTRA_QUERY else EXTRA_FILTER */
    public static QueryParameter getQuery(
            @NonNull Context context, @NonNull String paramNameSuffix,
            @Nullable Bundle savedInstanceState,
            @Nullable Intent intent, @Nullable SharedPreferences sharedPref,
            @Nullable StringBuilder dbgMessageResult) {
        QueryParameter result = null;

        if (savedInstanceState == null) {
            // onCreate (first call) : if intent is usefull use it else use sharedPref
            if (intent != null) {
                result = getQueryFromFirstMatching(
                        context,
                        IntentUtil.getUri(intent),
                        intent.getStringExtra(EXTRA_QUERY),
                        intent.getStringExtra(EXTRA_FILTER),
                        "Intent", dbgMessageResult);
            }

            if ((result == null) && (sharedPref != null)) {
                result = getQueryFromFirstMatching(
                        context,
                        null,
                        sharedPref.getString(EXTRA_QUERY + paramNameSuffix, null),
                        sharedPref.getString(EXTRA_FILTER + paramNameSuffix, null),
                        "SharedPreferences", dbgMessageResult);
            }
        } else  {
            // (savedInstanceState != null) : onCreate after screen rotation
            result = getQueryFromFirstMatching(
                    context,
                    null,
                    savedInstanceState.getString(EXTRA_QUERY + paramNameSuffix),
                    savedInstanceState.getString(EXTRA_FILTER + paramNameSuffix),
                    "InstanceState", dbgMessageResult);
        }
        return result;
    }

    /** used by folder picker if album-file is picked instead of folder */
    public static QueryParameter getQueryFromUri(@NonNull Context context, Uri uri, @Nullable StringBuilder dbgFilter) {
        // ignore geo: or area: uri-s
        String path = (IntentUtil.isFileOrContentUri(uri, true)) ? uri.getPath() : null;
        if (!StringUtils.isNullOrEmpty(path)) {
            // #118 app specific content uri convert
            // from {content://approvider}//storage/emulated/0/DCIM/... to /storage/emulated/0/DCIM/
            while (path.startsWith("//")) {
                path = path.substring(1);
            }

            if ((context != null) && AlbumFile.isQueryFile(path)) {
                try {
                    QueryParameter query = QueryParameter.load(context.getContentResolver().openInputStream(uri));
                    if (query != null) {
                        insertToMediaDB(context,
                                Uri.fromFile(new File(path)), mDebugPrefix + ".load");
                        if (dbgFilter != null) {
                            dbgFilter.append("query album from uri ").append(uri).append("\n")
                                    .append(query).append("\n");
                        }
                        return query;
                    }
                } catch (IOException e) {
                    Log.e(Global.LOG_CONTEXT, mDebugPrefix + ".loadFrom(" + uri +
                            ") failed: " + e.getMessage(), e);
                    if (dbgFilter != null) {
                        dbgFilter.append("query album from uri '").append(uri).append("' failed:")
                                .append(e.getMessage()).append("\n");
                    }
                }
            }

            // replace file wildcards with sql wildcards
            path = path.replace("*","%");

            // non full-path-query becomes contains-path-query
            if (!path.startsWith("/")) {
                path = "%" + path;
                if (!path.endsWith("%")) {
                    path += "%";
                }
            }
            if (path.length() > 2) {
                if (dbgFilter != null) {
                    dbgFilter.append("path query from uri '").append(uri).append("' : '")
                            .append(path).append("'\n");
                }
                GalleryFilterParameter filter = new GalleryFilterParameter().setPath(path);
                return TagSql.filter2NewQuery(filter);
            }
        }
        return null;
    }

    @Deprecated
    public static GalleryFilterParameter getGalleryFilterAndRestQueryFromQueryUri(
            @NonNull Context context, Uri uri, QueryParameter resultQueryWithoutFilter,
            boolean ignoreFilter, @Nullable StringBuilder dbgFilter) {
        QueryParameter query = getQueryFromUri(context, uri, null);
        if (query != null) {
            if ((uri != null) && (dbgFilter != null)) {
                dbgFilter.append("query from uri ").append(uri).append("\n");
            }
            return getFilterAndRestQuery(query, resultQueryWithoutFilter, ignoreFilter, dbgFilter);
        }
        return null;
    }

    @Deprecated
    private static GalleryFilterParameter getFilterAndRestQuery(
            @NonNull Context context, Uri uri, String sql, String filter, QueryParameter resultQueryWithoutFilter,
            boolean ignoreFilter, @Nullable StringBuilder dbgFilter) {
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

    /**
     * @param context
     * @param uri if not null and exist: load from uri
     * @param sql else if not null: parse query from this sql
     * @param filter else if not null parse query from filter
     * @param dbgMessagePrefix
     */
    private static QueryParameter getQueryFromFirstMatching(
            @NonNull Context context, @Nullable Uri uri, @Nullable String sql, @Nullable String filter,
            String dbgMessagePrefix, @Nullable StringBuilder dbgMessageResult) {
        if (uri != null) {
            QueryParameter query = getQueryFromUri(context, uri, dbgMessageResult);
            if (query != null) {
                return query;
            }
        }

        if (sql != null) {
            if (dbgMessageResult != null) {
                dbgMessageResult.append(dbgMessagePrefix).append(" query from ").append(EXTRA_QUERY).append("\n\t").append(sql).append("\n");
            }
            return QueryParameter.parse(sql);
        }

        if (filter != null) {
            if (dbgMessageResult != null) {
                dbgMessageResult.append(dbgMessagePrefix).append(" filter from ").append(EXTRA_FILTER).append("=").append(filter).append("\n");
            }
            return TagSql.filter2NewQuery(GalleryFilterParameter.parse(filter, new GalleryFilterParameter()));
        }
        return null;
    }

    @Deprecated
    private static GalleryFilterParameter getFilterAndRestQuery(
            QueryParameter query, QueryParameter resultQueryWithoutFilter,
            boolean ignoreFilter, @Nullable StringBuilder dbgFilter) {

        final GalleryFilterParameter result = (ignoreFilter) ? null : (GalleryFilterParameter) TagSql.parseQueryEx(query, true);

        if (resultQueryWithoutFilter != null) {
            resultQueryWithoutFilter.clear();
            resultQueryWithoutFilter.getFrom(query);
        }
        return result;
    }

    /**
     *
     * @param context
     * @param destUri if not null: merged-query-filter will be saved to this uri/file with update mediaDB
     * @param destIntent if not null: merged-query-filter will be saved to this intent
     * @param destBundle if not null: merged-query-filter will be saved to this intent
     * @param filter if not null filter will be appended to query parameter
     * @param queryParameter
     */
    public static void saveFilterAndQuery(
            @NonNull Context context, Uri destUri, Intent destIntent, Bundle destBundle,
            final IGalleryFilter filter, final QueryParameter queryParameter) {
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
            if (queryParameter != null)
                destBundle.putString(EXTRA_QUERY, query.toReParseableString());
            else if (filter != null)
                destBundle.putString(EXTRA_FILTER, filter.toString());
        }
        if (destIntent != null) {
            if (queryParameter != null)
                destIntent.putExtra(EXTRA_QUERY, query.toReParseableString());
            else if (filter != null)
                destIntent.putExtra(EXTRA_FILTER, filter.toString());
        }
    }

    public static void insertToMediaDB(@NonNull Context context, Uri uri, String dbgContext) {
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

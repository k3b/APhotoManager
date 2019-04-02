/*
 * Copyright (c) 2018-2019 by k3b.
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
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import de.k3b.android.androFotoFinder.Common;
import de.k3b.android.androFotoFinder.GalleryFilterPathState;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.tagDB.TagSql;
import de.k3b.android.util.IntentUtil;
import de.k3b.android.util.PhotoPropertiesMediaFilesScanner;
import de.k3b.database.QueryParameter;
import de.k3b.io.AlbumFile;
import de.k3b.io.FileUtils;
import de.k3b.io.GalleryFilterParameter;
import de.k3b.io.IGalleryFilter;
import de.k3b.io.StringUtils;
import de.k3b.tagDB.TagProcessor;

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

    public static QueryParameter getQuery(@NonNull String paramNameSuffix,
            Properties sourceProperties, StringBuilder outQueryFileUri,
            @Nullable StringBuilder dbgMessageResult) {
        QueryParameter result = null;

        if (sourceProperties != null) {
            result = getQueryFromFirstMatching(
                    "Properties", null,
                    null,
                    sourceProperties.getProperty(EXTRA_QUERY),
                    sourceProperties.getProperty(EXTRA_FILTER),
                    dbgMessageResult);
        }
        return result;
    }

    /** get from savedInstanceState else intent else sharedPref: inten.data=file-uri else EXTRA_QUERY else EXTRA_FILTER */
    public static QueryParameter getQuery(
            @NonNull Context context, @NonNull String paramNameSuffix,
            @Nullable Bundle sourceInstanceState,
            @Nullable Intent sourceIntent, @Nullable SharedPreferences sourceSharedPref,
            StringBuilder outQueryFileUri,
            @Nullable StringBuilder dbgMessageResult) {
        QueryParameter result = null;

        if (sourceInstanceState == null) {
            // onCreate (first call) : if intent is usefull use it else use sharedPref
            if (sourceIntent != null) {
                Uri uri = IntentUtil.getUri(sourceIntent);
                result = getQueryFromFirstMatching(
                        "Intent-uri", context,
                        uri,
                        null,
                        null,
                        dbgMessageResult);
                if (result == null) {
                    result = getQueryFromFirstMatching(
                            "Intent", context,
                            uri,
                            sourceIntent.getStringExtra(EXTRA_QUERY),
                            sourceIntent.getStringExtra(EXTRA_FILTER),
                            dbgMessageResult);
                } else if (outQueryFileUri !=  null) {
                    outQueryFileUri.append(uri.toString());
                }
            }

            if ((result == null) && (sourceSharedPref != null)) {
                result = getQueryFromFirstMatching(
                        "SharedPreferences", context,
                        null,
                        sourceSharedPref.getString(EXTRA_QUERY + paramNameSuffix, null),
                        sourceSharedPref.getString(EXTRA_FILTER + paramNameSuffix, null),
                        dbgMessageResult);
            }
        } else  {
            // (savedInstanceState != null) : onCreate after screen rotation
            result = getQueryFromFirstMatching(
                    "InstanceState", context,
                    null,
                    sourceInstanceState.getString(EXTRA_QUERY + paramNameSuffix),
                    sourceInstanceState.getString(EXTRA_FILTER + paramNameSuffix),
                    dbgMessageResult);
        }
        return result;
    }

    /** used by folder picker if album-file is picked instead of folder */
    public static QueryParameter getQueryFromUri(String dbgContext, @NonNull Context context, Uri uri, @Nullable StringBuilder dbgFilter) {
        // ignore geo: or area: uri-s
        String path = (IntentUtil.isFileOrContentUri(uri, true)) ? uri.getPath() : null;
        if (!StringUtils.isNullOrEmpty(path)) {
            // #118 app specific content uri convert
            // from {content://approvider}//storage/emulated/0/DCIM/... to /storage/emulated/0/DCIM/
            path = FileUtils.fixPath(path);
            if (dbgFilter != null) {
                dbgFilter.append(dbgContext).append("\n");
            } else if (Global.debugEnabled) {
                Log.d(Global.LOG_CONTEXT, dbgContext);
            }

            if ((context != null) && AlbumFile.isQueryFile(path)) {
                try {
                    QueryParameter query = QueryParameter.load(context.getContentResolver().openInputStream(uri));
                    if (query != null) {
                        Map<String, Long> found = FotoSql.execGetPathIdMap(context, path);
                        if ((found == null) || (found.size() == 0)) {
                            AndroidAlbumUtils.albumMediaScan(dbgContext + " not found mediadb => ", context, new File(path), 1);
                        }

                        if (dbgFilter != null) {
                            dbgFilter.append("query album from uri ").append(uri).append("\n")
                                    .append(query).append("\n");
                        }
                        return query;
                    }

                    // not found in filesystem.
                    AndroidAlbumUtils.albumMediaScan(dbgContext + " not found in filesystem => ", context, new File(path), 1);
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
        QueryParameter query = getQueryFromUri(mDebugPrefix, context, uri, null);
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
     * @param dbgMessagePrefix
     * @param context
     * @param uri if not null and exist: load from uri
     * @param sql else if not null: parse query from this sql
     * @param filter else if not null parse query from filter
     */
    private static QueryParameter getQueryFromFirstMatching(
            String dbgMessagePrefix, @NonNull Context context, @Nullable Uri uri, @Nullable String sql, @Nullable String filter,
            @Nullable StringBuilder dbgMessageResult) {
        if (uri != null) {
            QueryParameter query = getQueryFromUri(dbgMessagePrefix, context, uri, dbgMessageResult);
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
     * @param context
     * @param destUri if not null: merged-query-filter will be saved to this uri/file with update mediaDB
     * @param destIntent if not null: merged-query-filter will be saved to this intent
     * @param destBundle if not null: merged-query-filter will be saved to this intent
     * @param srcFilter if not null filter will be appended to query parameter
     * @param srcQueryParameter
     */
    public static void saveFilterAndQuery(
            @NonNull Context context, Uri destUri, Intent destIntent, Bundle destBundle,
            final IGalleryFilter srcFilter, final QueryParameter srcQueryParameter) {
        final String dbgContext = mDebugPrefix + ".saveFilterAndQuery(" + destUri + ")";
        final QueryParameter mergedQuery = getAsMergedNewQueryParameter(srcQueryParameter, srcFilter);

        if (destUri != null) {
            PrintWriter out = null;
            try {
                mergedQuery.save(context.getContentResolver().openOutputStream(destUri, "w"));
                insertToMediaDB(dbgContext, context, destUri);
            } catch (IOException e) {
                Log.e(Global.LOG_CONTEXT, dbgContext +
                        " failed: " + e.getMessage(), e);
            } finally {
                FileUtils.close(out, "");
            }
            if (destIntent != null) destIntent.setData(destUri);
        }

        if (destBundle != null) {
            if (srcQueryParameter != null)
                destBundle.putString(EXTRA_QUERY, mergedQuery.toReParseableString());
            else if (srcFilter != null)
                destBundle.putString(EXTRA_FILTER, srcFilter.toString());
        }
        if (destIntent != null) {
            if (srcQueryParameter != null)
                destIntent.putExtra(EXTRA_QUERY, mergedQuery.toReParseableString());
            else if (srcFilter != null)
                destIntent.putExtra(EXTRA_FILTER, srcFilter.toString());
        }
        /*
        if (destProperties != null) {
            if (srcQueryParameter != null)
                destProperties.put(EXTRA_QUERY, mergedQuery.toReParseableString());
            else if (srcFilter != null)
                destProperties.put(EXTRA_FILTER, srcFilter.toString());
        }
        */
    }

    /** create a copy of srcQueryParameter and add srcFilter */
    public static QueryParameter getAsMergedNewQueryParameter(QueryParameter srcQueryParameter, IGalleryFilter srcFilter) {
        final QueryParameter mergedQuery = new QueryParameter(srcQueryParameter);
        TagSql.filter2QueryEx(mergedQuery, srcFilter, false);
        return mergedQuery;
    }

    public static void insertToMediaDB(String dbgContext, @NonNull Context context, Uri uri) {
        if (IntentUtil.isFileUri(uri)) {
            File f = FileUtils.tryGetCanonicalFile(IntentUtil.getFile(uri));
            insertToMediaDB(dbgContext, context, f);
        }
    }

    public static void insertToMediaDB(String dbgContext, @NonNull Context context, File fileToBeScannedAndInserted) {
        if (fileToBeScannedAndInserted != null) {
            ContentValues values = new ContentValues();
            String newAbsolutePath = PhotoPropertiesMediaFilesScanner.setFileFields(values, fileToBeScannedAndInserted);
            values.put(FotoSql.SQL_COL_EXT_MEDIA_TYPE, FotoSql.MEDIA_TYPE_ALBUM_FILE);
            FotoSql.insertOrUpdateMediaDatabase(dbgContext, context, newAbsolutePath, values, null, 1l);
        }
    }

    // !!! todo einbauen in filter-edit if item not found in db and/or filesystem und in media scanner
    /** return the number of modified(added+deleted) items */
    public static int albumMediaScan(String dbgContext, Context context, File _root, int subDirLevels) {
        String dbgMessage = dbgContext + ": albumMediaScan(" + _root + "): ";

        File root = FileUtils.getFirstExistingDir(_root);
        int result = 0;
        if (root != null) {
            List<String> currentFiles = AlbumFile.getFilePaths(null, root, subDirLevels);
            List<String> databaseFiles = FotoSql.getAlbumFiles(context, FileUtils.tryGetCanonicalPath(root, null), subDirLevels);

            List<String> added = new ArrayList<String>();
            List<String> removed = new ArrayList<String>();
            TagProcessor.getDiff(databaseFiles, currentFiles, added, removed);

            for (String insert : added) {
                insertToMediaDB(dbgMessage + "add-new", context, new File(insert));
                result++;
            }

            result += FotoSql.deleteMedia(dbgMessage + "delete-obsolete", context, removed, false);
        }
        return result;
    }

    public static void saveAs(Context context, File outFile, final QueryParameter currentFilter) {
        if (Global.debugEnabled) {
            Log.d(Global.LOG_CONTEXT, "onSaveAs(" + outFile.getAbsolutePath() + ")");
        }
        PrintWriter out = null;
        try {
            out = new PrintWriter(outFile);
            out.println(currentFilter.toReParseableString());
            out.close();
            out = null;

            AndroidAlbumUtils.insertToMediaDB(
                    ".saveAlbumAs",
                    context,
                    outFile);
            GalleryFilterPathState.saveAsPreference(context, Uri.fromFile(outFile), null);
        } catch (IOException err) {
            String errorMessage = context.getString(R.string.mk_err_failed_format, outFile.getAbsoluteFile());
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
            Log.e(Global.LOG_CONTEXT, errorMessage, err);
        }
    }

}

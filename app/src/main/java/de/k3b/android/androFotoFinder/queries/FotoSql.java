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
 
package de.k3b.android.androFotoFinder.queries;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.database.QueryParameter;
import de.k3b.database.SelectedFiles;
import de.k3b.database.SelectedItems;
import de.k3b.io.DirectoryFormatter;
import de.k3b.io.FileCommands;
import de.k3b.io.GalleryFilterParameter;
import de.k3b.io.GeoRectangle;
import de.k3b.io.IGalleryFilter;
import de.k3b.io.IGeoRectangle;

/**
 * contains all SQL needed to query the android gallery
 *
 * Created by k3b on 04.06.2015.
 */
public class FotoSql {
//    private static final int PER_DAY = 1000 * 60 * 60 * 24;
//    public static final String SQL_EXPR_DAY = "(ROUND("
//            + MediaStore.Images.Media.SQL_COL_DATE_TAKEN + "/" + PER_DAY + ") * " + PER_DAY + ")";

    public static final int SORT_BY_NONE = 0;
    public static final int SORT_BY_DATE = 1;
    public static final int SORT_BY_NAME = 2;
    public static final int SORT_BY_LOCATION = 3;
    public static final int SORT_BY_NAME_LEN = 4;
    public static final int SORT_BY_DEFAULT = SORT_BY_NAME;

    public static final int QUERY_TYPE_UNDEFINED = 0;
    public static final int QUERY_TYPE_GALLERY = 11;
    public static final int QUERY_TYPE_GROUP_DATE = 12;
    public static final int QUERY_TYPE_GROUP_ALBUM = 13;
    public static final int QUERY_TYPE_GROUP_PLACE = 14;
    public static final int QUERY_TYPE_GROUP_PLACE_MAP = 141;

    public static final int QUERY_TYPE_GROUP_COPY = 20;
    public static final int QUERY_TYPE_GROUP_MOVE = 21;

    public static final int QUERY_TYPE_GROUP_DEFAULT = QUERY_TYPE_GROUP_ALBUM;
    public static final int QUERY_TYPE_DEFAULT = QUERY_TYPE_GALLERY;

    public static final Uri SQL_TABLE_EXTERNAL_CONTENT_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

    // columns that must be avaulable in the Cursor
    public static final String SQL_COL_PK = MediaStore.Images.Media._ID;
    public static final String SQL_COL_DISPLAY_TEXT = "disp_txt";
    public static final String SQL_COL_LAT = MediaStore.Images.Media.LATITUDE;
    public static final String SQL_COL_LON = MediaStore.Images.Media.LONGITUDE;
    public static final String SQL_COL_SIZE = MediaStore.Images.Media.SIZE;

    // only works with api >= 16
    public static final String SQL_COL_MAX_WITH =
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                ? "max(" + MediaStore.Images.Media.WIDTH + "," +
                                                MediaStore.Images.Media.HEIGHT +")"
                : "1024";

    private static final String FILTER_EXPR_LAT_MAX = SQL_COL_LAT + " < ?";
    private static final String FILTER_EXPR_LAT_MIN = SQL_COL_LAT + " >= ?";
    private static final String FILTER_EXPR_NO_GPS = SQL_COL_LAT + " is null AND " + SQL_COL_LON + " is null";
    private static final String FILTER_EXPR_LON_MAX = SQL_COL_LON + " < ?";
    private static final String FILTER_EXPR_LON_MIN = SQL_COL_LON + " >= ?";
    public static final String SQL_COL_GPS = MediaStore.Images.Media.LONGITUDE;
    public static final String SQL_COL_COUNT = "count";
    public static final String SQL_COL_WHERE_PARAM = "where_param";

    public static final String SQL_COL_DATE_TAKEN = MediaStore.Images.Media.DATE_TAKEN;
    private static final String FILTER_EXPR_DATE_MAX = SQL_COL_DATE_TAKEN + " < ?";
    private static final String FILTER_EXPR_DATE_MIN = SQL_COL_DATE_TAKEN + " >= ?";
    public static final String SQL_COL_PATH = MediaStore.Images.Media.DATA;
    private static final String FILTER_EXPR_PATH_LIKE = SQL_COL_PATH + " like ?";

    // same format as dir. i.e. description='/2014/12/24/' or '/mnt/sdcard/pictures/'
    public static final String SQL_EXPR_DAY = "strftime('/%Y/%m/%d/', " + SQL_COL_DATE_TAKEN + " /1000, 'unixepoch', 'localtime')";

    public static final QueryParameter queryGroupByDate = new QueryParameter()
            .setID(QUERY_TYPE_GROUP_DATE)
            .addColumn(
                    "max(" + SQL_COL_PK + ") AS " + SQL_COL_PK,
                    SQL_EXPR_DAY + " AS " + SQL_COL_DISPLAY_TEXT,
                    "count(*) AS " + SQL_COL_COUNT,
                    "max(" + SQL_COL_GPS + ") AS " + SQL_COL_GPS,
                    "max(" + SQL_COL_PATH + ") AS " + SQL_COL_PATH)
            .addFrom(SQL_TABLE_EXTERNAL_CONTENT_URI.toString())
            .addGroupBy(SQL_EXPR_DAY)
            .addOrderBy(SQL_EXPR_DAY);


    public static final String SQL_EXPR_FOLDER = "substr(" + SQL_COL_PATH + ",1,length(" + SQL_COL_PATH + ") - length(" + MediaStore.Images.Media.DISPLAY_NAME + "))";
    public static final QueryParameter queryGroupByDir = new QueryParameter()
            .setID(QUERY_TYPE_GROUP_ALBUM)
            .addColumn(
                    "max(" + SQL_COL_PK + ") AS " + SQL_COL_PK,
                    SQL_EXPR_FOLDER + " AS " + SQL_COL_DISPLAY_TEXT,
                    "count(*) AS " + SQL_COL_COUNT,
                    // (Substr(_data,1, length(_data) -  length(_display_Name)) = '/storage/sdcard0/DCIM/onw7b/2013/')
                    // "'(" + SQL_EXPR_FOLDER + " = ''' || " + SQL_EXPR_FOLDER + " || ''')'"
                    "max(" + SQL_COL_GPS + ") AS " + SQL_COL_GPS)
            .addFrom(SQL_TABLE_EXTERNAL_CONTENT_URI.toString())
            .addGroupBy(SQL_EXPR_FOLDER)
            .addOrderBy(SQL_EXPR_FOLDER);

    /* image entries may become duplicated if media scanner finds new images that have not been inserted into media database yet
     * and aFotoSql tries to show the new image and triggers a filescan. */
    public static final QueryParameter queryGetDuplicates = new QueryParameter()
            .setID(QUERY_TYPE_UNDEFINED)
            .addColumn(
                    "min(" + SQL_COL_PK + ") AS " + SQL_COL_PK,
                    SQL_COL_PATH + " AS " + SQL_COL_DISPLAY_TEXT,
                    "count(*) AS " + SQL_COL_COUNT)
            .addFrom(SQL_TABLE_EXTERNAL_CONTENT_URI.toString())
            .addWhere(SQL_COL_PATH + " IS NOT NULL ")
            .addGroupBy(SQL_COL_PATH)
            .addHaving("count(*) > 1")
            .addOrderBy(SQL_COL_PATH);

    /* image entries may not have DISPLAY_NAME which is essential for calculating the item-s folder. */
    public static final QueryParameter queryChangePath = new QueryParameter()
            .setID(QUERY_TYPE_UNDEFINED)
            .addColumn(
                    SQL_COL_PK,
                    SQL_COL_PATH,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.TITLE)
            .addFrom(SQL_TABLE_EXTERNAL_CONTENT_URI.toString());

    /* image entries may not have DISPLAY_NAME which is essential for calculating the item-s folder. */
    public static final QueryParameter queryGetMissingDisplayNames = queryChangePath
            .addWhere(MediaStore.MediaColumns.DISPLAY_NAME + " is null");

    // the bigger the smaller the area
    private static final double GROUPFACTOR_FOR_Z0 = 0.025;

    /** to avoid cascade delete of linked file when mediaDB-item is deleted
     *  the links are first set to null before delete. */
    private static final String DELETED_FILE_MARKER = null;

    public static final double getGroupFactor(final int _zoomLevel) {
        int zoomLevel = _zoomLevel;
        double result = GROUPFACTOR_FOR_Z0;
        while (zoomLevel > 0) {
            // result <<= 2; //
            result = result * 2;
            zoomLevel--;
        }

        if (Global.debugEnabled) {
            Log.e(Global.LOG_CONTEXT, "FotoSql.getGroupFactor(" + _zoomLevel + ") => " + result);
        }

        return result;
    }

    public static final QueryParameter queryGroupByPlace = getQueryGroupByPlace(100);

    public static QueryParameter getQueryGroupByPlace(double groupingFactor) {
        //String SQL_EXPR_LAT = "(round(" + SQL_COL_LAT + " - 0.00499, 2))";
        //String SQL_EXPR_LON = "(round(" + SQL_COL_LON + " - 0.00499, 2))";

        // "- 0.5" else rounding "10.6" becomes 11.0
        // + (1/groupingFactor/2) in the middle of grouping area
        String SQL_EXPR_LAT = "((round((" + SQL_COL_LAT + " * " + groupingFactor + ") - 0.5) /"
                + groupingFactor + ") + " + (1/groupingFactor/2) + ")";
        String SQL_EXPR_LON = "((round((" + SQL_COL_LON + " * " + groupingFactor + ") - 0.5) /"
                + groupingFactor + ") + " + (1/groupingFactor/2) + ")";

        QueryParameter result = new QueryParameter();

        result.setID(QUERY_TYPE_GROUP_PLACE)
                .addColumn(
                        "max(" + SQL_COL_PK + ") AS " + SQL_COL_PK,
                        SQL_EXPR_LAT + " AS " + SQL_COL_LAT,
                        SQL_EXPR_LON + " AS " + SQL_COL_LON,
                        "count(*) AS " + SQL_COL_COUNT)
                .addFrom(SQL_TABLE_EXTERNAL_CONTENT_URI.toString())
                .addGroupBy(SQL_EXPR_LAT, SQL_EXPR_LON)
                .addOrderBy(SQL_EXPR_LAT, SQL_EXPR_LON);

        return result;
    }

    public static final String[] DEFAULT_GALLERY_COLUMNS = new String[]{SQL_COL_PK,
            SQL_COL_PATH + " AS " + SQL_COL_DISPLAY_TEXT,
            "0 AS " + SQL_COL_COUNT,
            SQL_COL_MAX_WITH + " AS " + SQL_COL_SIZE,
            SQL_COL_GPS,
            SQL_COL_PATH};

    public static final QueryParameter queryDetail = new QueryParameter()
            .setID(QUERY_TYPE_GALLERY)
            .addColumn(
                    DEFAULT_GALLERY_COLUMNS)
            .addFrom(SQL_TABLE_EXTERNAL_CONTENT_URI.toString());

    public static final QueryParameter queryGps = new QueryParameter()
            .setID(QUERY_TYPE_UNDEFINED)
            .addColumn(
                    SQL_COL_PK,
                    // SQL_COL_PATH + " AS " + SQL_COL_DISPLAY_TEXT,
                    "0 AS " + SQL_COL_COUNT,
                    SQL_COL_LAT, SQL_COL_LON)
            .addFrom(SQL_TABLE_EXTERNAL_CONTENT_URI.toString());

    public static void setWhereFilter(QueryParameter parameters, IGalleryFilter filter, boolean clearWhereBefore) {
        if ((parameters != null) && (filter != null)) {
            if (clearWhereBefore) {
                parameters.clearWhere();
            }

            if (filter.isNonGeoOnly()) {
                parameters.addWhere(FILTER_EXPR_NO_GPS);
            } else {
                addWhereFilterLatLon(parameters, filter);
            }

            if (filter.getDateMin() != 0) parameters.addWhere(FILTER_EXPR_DATE_MIN, Long.toString(filter.getDateMin()));
            if (filter.getDateMax() != 0) parameters.addWhere(FILTER_EXPR_DATE_MAX, Long.toString(filter.getDateMax()));

            String path = filter.getPath();
            if ((path != null) && (path.length() > 0)) parameters.addWhere(FILTER_EXPR_PATH_LIKE, path);
        }
    }

    public static IGalleryFilter getWhereFilter(QueryParameter parameters, boolean remove) {
        if (parameters != null) {
            GalleryFilterParameter filter = new GalleryFilterParameter();
            if (null != parameters.getWhereParameter(FILTER_EXPR_NO_GPS, remove)) {
                filter.setNonGeoOnly(true);
            } else {
                filter.setLogitude(getParam(parameters, FILTER_EXPR_LON_MIN, remove), getParam(parameters, FILTER_EXPR_LON_MAX, remove));
                filter.setLatitude(getParam(parameters, FILTER_EXPR_LAT_MIN, remove), getParam(parameters, FILTER_EXPR_LAT_MAX, remove));
            }

	        filter.setDate(getParam(parameters, FILTER_EXPR_DATE_MIN, remove), getParam(parameters, FILTER_EXPR_DATE_MAX, remove));
            filter.setPath(getParam(parameters, FILTER_EXPR_PATH_LIKE, remove));
            return filter;
        }
        return null;
    }

    private static String getParam(QueryParameter query, String expresion, boolean remove) {
        final String[] result = query.getWhereParameter(expresion, remove);
        return ((result != null) && (result.length > 0)) ? result[0] : null;
    }

    public static QueryParameter setWhereSelectionPks(QueryParameter query, SelectedItems selectedItems) {
        if ((query != null) && (selectedItems != null) && (!selectedItems.isEmpty())) {
            String pksAsListString = selectedItems.toString();
            setWhereSelectionPks(query, pksAsListString);
        }
        return query;
    }

    public static QueryParameter setWhereSelectionPks(QueryParameter query, String pksAsListString) {
        if ((pksAsListString != null) && (pksAsListString.length() > 0)) {
            query.clearWhere()
                    .addWhere(FotoSql.SQL_COL_PK + " in (" + pksAsListString + ")")
            ;
        }
        return query;
    }

    public static void setWhereSelectionPaths(QueryParameter query, SelectedFiles selectedItems) {
        if ((query != null) && (selectedItems != null) && (selectedItems.size() > 0)) {
            query.clearWhere()
                    .addWhere(FotoSql.SQL_COL_PATH + " in (" + selectedItems.toString() + ")")
            ;
        }
    }

    public static void setWhereFileNames(QueryParameter query, String... fileNames) {
        if ((query != null) && (fileNames != null) && (fileNames.length > 0)) {
            query.clearWhere()
                    .addWhere(getWhereInFileNames(fileNames))
            ;
        }
    }

    public static void addWhereLatLonNotNull(QueryParameter query) {
        query.addWhere(FotoSql.SQL_COL_LAT + " is not null and " + FotoSql.SQL_COL_LON + " is not null")
        ;
    }

    public static void addWhereFilterLatLon(QueryParameter parameters, IGeoRectangle filter) {
        if ((parameters != null) && (filter != null)) {
            addWhereFilterLatLon(parameters, filter.getLatitudeMin(),
                    filter.getLatitudeMax(), filter.getLogituedMin(), filter.getLogituedMax());
        }
    }

    public static void addWhereFilterLatLon(QueryParameter query, double latitudeMin, double latitudeMax, double logituedMin, double logituedMax) {
        if (!Double.isNaN(latitudeMin)) query.addWhere(FILTER_EXPR_LAT_MIN, DirectoryFormatter.parseLatLon(latitudeMin));
        if (!Double.isNaN(latitudeMax)) query.addWhere(FILTER_EXPR_LAT_MAX, DirectoryFormatter.parseLatLon(latitudeMax));
        if (!Double.isNaN(logituedMin)) query.addWhere(FILTER_EXPR_LON_MIN, DirectoryFormatter.parseLatLon(logituedMin));
        if (!Double.isNaN(logituedMax)) query.addWhere(FILTER_EXPR_LON_MAX, DirectoryFormatter.parseLatLon(logituedMax));
    }

    public static void addPathWhere(QueryParameter newQuery, String selectedAbsolutePath, int dirQueryID) {
        if ((selectedAbsolutePath != null) && (selectedAbsolutePath.length() > 0)) {
            if (QUERY_TYPE_GROUP_DATE == dirQueryID) {
                addWhereDatePath(newQuery, selectedAbsolutePath);
            } else {
                // selectedAbsolutePath is assumed to be a file path i.e. /mnt/sdcard/pictures/
                addWhereDirectoryPath(newQuery, selectedAbsolutePath);
            }
        }
    }

    /**
     * directory path i.e. /mnt/sdcard/pictures/
     */
    private static void addWhereDirectoryPath(QueryParameter newQuery, String selectedAbsolutePath) {
        if (FotoViewerParameter.includeSubItems) {
            newQuery
                    .addWhere(FotoSql.SQL_COL_PATH + " like ?", selectedAbsolutePath + "%")
                            // .addWhere(FotoSql.SQL_COL_PATH + " like '" + selectedAbsolutePath + "%'")
                    .addOrderBy(FotoSql.SQL_COL_PATH);
        } else {
            // foldername exact match
            newQuery
                    .addWhere(SQL_EXPR_FOLDER + " =  ?", selectedAbsolutePath)
                    .addOrderBy(FotoSql.SQL_COL_PATH);
        }
    }

    /**
     * path has format /year/month/day/ or /year/month/ or /year/ or /
     */
    private static void addWhereDatePath(QueryParameter newQuery, String selectedAbsolutePath) {
        Date from = new Date();
        Date to = new Date();

        DirectoryFormatter.getDates(selectedAbsolutePath, from, to);

        if (to.getTime() == 0) {
            newQuery
                    .addWhere(SQL_COL_DATE_TAKEN + " in (0,-1, null)")
                    .addOrderBy(SQL_COL_DATE_TAKEN + " desc");
        } else {
            newQuery
                    .addWhere(FILTER_EXPR_DATE_MIN, "" + from.getTime())
                    .addWhere(FILTER_EXPR_DATE_MAX, "" + to.getTime())
                    .addOrderBy(SQL_COL_DATE_TAKEN + " desc");
        }
    }

    public static QueryParameter getQuery(int queryID) {
        switch (queryID) {
            case QUERY_TYPE_UNDEFINED:
                return null;
            case QUERY_TYPE_GALLERY:
                return queryDetail;
            case QUERY_TYPE_GROUP_DATE:
                return queryGroupByDate;
            case QUERY_TYPE_GROUP_ALBUM:
                return queryGroupByDir;
            case QUERY_TYPE_GROUP_PLACE:
            case QUERY_TYPE_GROUP_PLACE_MAP:
                return queryGroupByPlace;
            case QUERY_TYPE_GROUP_COPY:
            case QUERY_TYPE_GROUP_MOVE:
                return null;
            default:
                Log.e(Global.LOG_CONTEXT, "FotoSql.getQuery(" + queryID + "): unknown ID");
                return null;
        }
    }

    public static String getName(Context context, int id) {
        switch (id) {
            case SORT_BY_NONE:
                return context.getString(R.string.sort_by_none);
            case SORT_BY_DATE:
                return context.getString(R.string.sort_by_date);
            case SORT_BY_NAME:
                return context.getString(R.string.sort_by_name);
            case SORT_BY_LOCATION:
                return context.getString(R.string.sort_by_place);
            case SORT_BY_NAME_LEN:
                return context.getString(R.string.sort_by_name_len);

            case QUERY_TYPE_GALLERY:
                return context.getString(R.string.gallery_title);
            case QUERY_TYPE_GROUP_DATE:
                return context.getString(R.string.sort_by_date);
            case QUERY_TYPE_GROUP_ALBUM:
                return context.getString(R.string.sort_by_folder);
            case QUERY_TYPE_GROUP_PLACE:
            case QUERY_TYPE_GROUP_PLACE_MAP:
                return context.getString(R.string.sort_by_place);
            case QUERY_TYPE_GROUP_COPY:
                return context.getString(R.string.destination_copy);
            case QUERY_TYPE_GROUP_MOVE:
                return context.getString(R.string.destination_move);
            default:
                return "???";
        }

    }

    public static QueryParameter setSort(QueryParameter result, int sortID, boolean ascending) {
        String asc = (ascending) ? " asc" : " desc";
        result.replaceOrderBy();
        switch (sortID) {
            case SORT_BY_DATE:
                return result.replaceOrderBy(SQL_COL_DATE_TAKEN + asc);
            case SORT_BY_NAME:
                return result.replaceOrderBy(SQL_COL_PATH + asc);
            case SORT_BY_LOCATION:
                return result.replaceOrderBy(SQL_COL_GPS + asc, MediaStore.Images.Media.LATITUDE + asc);
            case SORT_BY_NAME_LEN:
                return result.replaceOrderBy("length(" + SQL_COL_PATH + ")"+asc);
            default: return  result;
        }
    }

    public static boolean set(GalleryFilterParameter dest, String selectedAbsolutePath, int queryTypeId) {
        switch (queryTypeId) {
            case FotoSql.QUERY_TYPE_GROUP_ALBUM:
                dest.setPath(selectedAbsolutePath + "%");
                return true;
            case FotoSql.QUERY_TYPE_GROUP_DATE:
                Date from = new Date();
                Date to = new Date();

                DirectoryFormatter.getDates(selectedAbsolutePath, from, to);
                dest.setDateMin(from.getTime());
                dest.setDateMax(to.getTime());
                return true;
            case FotoSql.QUERY_TYPE_GROUP_PLACE_MAP:
            case FotoSql.QUERY_TYPE_GROUP_PLACE:
                IGeoRectangle geo = DirectoryFormatter.parseLatLon(selectedAbsolutePath);
                if (geo != null) {
                    dest.get(geo);
                }
                return true;
            default:break;
        }
        return false;
    }

    public static String execGetFotoPath(Context context, Uri uri) {
        Cursor c = null;
        try {
            c = createCursorForQuery(context, uri.toString(), null, null, null, FotoSql.SQL_COL_PATH);
            if (c.moveToFirst()) {
                return c.getString(c.getColumnIndex(FotoSql.SQL_COL_PATH));
            }
        } catch (Exception ex) {
            Log.e(Global.LOG_CONTEXT, "FotoSql.execGetFotoPath() Cannot get path from " + uri, ex);
        } finally {
            if (c != null) c.close();
        }
        return null;
    }

    public static List<String> execGetFotoPaths(Context context, String pathFilter) {
        ArrayList<String> result = new ArrayList<String>();
        ContentResolver resolver = context.getContentResolver();

        Cursor c = null;
        try {
            c = resolver.query(SQL_TABLE_EXTERNAL_CONTENT_URI, new String[]{FotoSql.SQL_COL_PATH}, FotoSql.SQL_COL_PATH + " like ?", new String[]{pathFilter}, FotoSql.SQL_COL_PATH);
            while (c.moveToNext()) {
                result.add(c.getString(0));
            }
        } catch (Exception ex) {
            Log.e(Global.LOG_CONTEXT, "FotoSql.execGetFotoPaths() Cannot get path from: " + FotoSql.SQL_COL_PATH + " like '" + pathFilter +"'", ex);
        } finally {
            if (c != null) c.close();
        }

        if (Global.debugEnabled) {
            Log.d(Global.LOG_CONTEXT, "FotoSql.execGetFotoPaths() result count=" + result.size());
        }
        return result;
    }

    /**
     * Write geo data (lat/lon) media database.<br/>
     */
    public static int execUpdateGeo(final Context context, double latitude, double longitude, SelectedFiles selectedItems) {
        QueryParameter where = new QueryParameter();
        setWhereSelectionPaths(where, selectedItems);

        ContentValues values = new ContentValues(2);
        values.put(SQL_COL_LAT, DirectoryFormatter.parseLatLon(latitude));
        values.put(SQL_COL_LON, DirectoryFormatter.parseLatLon(longitude));
        ContentResolver resolver = context.getContentResolver();
        return resolver.update(SQL_TABLE_EXTERNAL_CONTENT_URI, values, where.toAndroidWhere(), where.toAndroidParameters());
    }

    public static Cursor createCursorForQuery(final Context context, QueryParameter parameters) {
        return createCursorForQuery(context, parameters.toFrom(), parameters.toAndroidWhere(),
                parameters.toAndroidParameters(), parameters.toOrderBy(),
                parameters.toColumns()
        );
    }

    private static Cursor createCursorForQuery(final Context context, final String from, final String sqlWhereStatement,
                                               final String[] sqlWhereParameters, final String sqlSortOrder,
                                               final String... sqlSelectColums) {
        ContentResolver resolver = context.getContentResolver();
        return resolver.query(Uri.parse(from), sqlSelectColums, sqlWhereStatement, sqlWhereParameters, sqlSortOrder);
    }

    public static IGeoRectangle execGetGeoRectangle(Context context, IGalleryFilter filter, SelectedItems selectedItems) {
        QueryParameter query = new QueryParameter()
                .setID(QUERY_TYPE_UNDEFINED)
                .addColumn(
                        "min(" + SQL_COL_LAT + ") AS LAT_MIN",
                        "max(" + SQL_COL_LAT + ") AS LAT_MAX",
                        "min(" + SQL_COL_LON + ") AS LON_MIN",
                        "max(" + SQL_COL_LON + ") AS LON_MAX",
                        "count(*)"
                )
                .addFrom(SQL_TABLE_EXTERNAL_CONTENT_URI.toString());

        if (filter != null) {
            setWhereFilter(query, filter, true);
        }

        if (selectedItems != null) {
            setWhereSelectionPks(query, selectedItems);
        }
        FotoSql.addWhereLatLonNotNull(query);

        Cursor c = null;
        try {
            c = createCursorForQuery(context, query);
            if (c.moveToFirst()) {
                GeoRectangle result = new GeoRectangle();
                result.setLatitude(c.getDouble(0), c.getDouble(1));
                result.setLogitude(c.getDouble(2), c.getDouble(3));

                if (Global.debugEnabledSql) {
                    Log.i(Global.LOG_CONTEXT, "FotoSql.execGetGeoRectangle() => " + result + " from " + c.getLong(4) + " via\n\t" + query);
                }

                return result;
            }
        } catch (Exception ex) {
            Log.e(Global.LOG_CONTEXT, "FotoSql.execGetGeoRectangle(): error executing " + query, ex);
        } finally {
            if (c != null) c.close();
        }
        return null;
    }

    /** gets IGeoPoint either from file if fullPath is not null else from db via id */
    public static IGeoPoint execGetPosition(Context context, String fullPath, long id) {
        QueryParameter query = new QueryParameter()
        .setID(QUERY_TYPE_UNDEFINED)
                .addColumn(SQL_COL_LAT, SQL_COL_LON)
                .addFrom(SQL_TABLE_EXTERNAL_CONTENT_URI.toString())
                .addWhere(SQL_COL_LAT + " IS NOT NULL")
                .addWhere(SQL_COL_LON + " IS NOT NULL");

        if (fullPath != null) {
            query.addWhere(SQL_COL_PATH + "= ?", fullPath);

        } else {
            query.addWhere(SQL_COL_PK + "= ?", "" + id);
        }

        Cursor c = null;
        try {
            c = createCursorForQuery(context, query);
            if (c.moveToFirst()) {
                GeoPoint result = new GeoPoint(c.getDouble(0),c.getDouble(1));
                return result;
            }
        } catch (Exception ex) {
            Log.e(Global.LOG_CONTEXT, "FotoSql.execGetPosition: error executing " + query, ex);
        } finally {
            if (c != null) c.close();
        }
        return null;
    }

    /**
     * @return returns a hashmap filename => mediaID
     */
    public static Map<String, Integer> execGetPathIdMap(Context context, String... fileNames) {
        Map<String, Integer> result = new HashMap<String, Integer>();

        String whereFileNames = getWhereInFileNames(fileNames);
        if (whereFileNames != null) {
            QueryParameter query = new QueryParameter()
                    .setID(QUERY_TYPE_UNDEFINED)
                    .addColumn(SQL_COL_PK, SQL_COL_PATH)
                    .addFrom(SQL_TABLE_EXTERNAL_CONTENT_URI.toString())
                    .addWhere(whereFileNames);

            Cursor c = null;
            try {
                c = createCursorForQuery(context, query);
                while (c.moveToNext()) {
                    result.put(c.getString(1),c.getInt(0));
                }
            } catch (Exception ex) {
                Log.e(Global.LOG_CONTEXT, "FotoSql.execGetPathIdMap: error executing " + query, ex);
            } finally {
                if (c != null) c.close();
            }
        }
        return result;
    }

    public static String getWhereInFileNames(String... fileNames) {
        if (fileNames != null) {
            StringBuilder filter = new StringBuilder();
            filter.append(SQL_COL_PATH).append(" in (");

            int count = 0;
            for (String fileName : fileNames) {
                if ((fileName != null) &&!FileCommands.isSidecar(fileName)) {
                    if (count > 0) filter.append(", ");
                    filter.append("'").append(fileName).append("'");
                    count++;
                }
            }

            filter.append(")");

            if (count > 0) return filter.toString();
        }
        return null;
    }

    public static ContentValues getDbContent(Context context, final long id) {
        ContentResolver resolver = context.getContentResolver();

        Cursor c = null;
        try {
            c = resolver.query(SQL_TABLE_EXTERNAL_CONTENT_URI, new String[]{"*"}, FotoSql.SQL_COL_PK + " = ?", new String[]{"" + id}, null);
            if (c.moveToNext()) {
                ContentValues values = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(c, values);
                return values;
            }
        } catch (Exception ex) {
            Log.e(Global.LOG_CONTEXT, "FotoSql.getDbContent(id=" + id + ") failed", ex);
        } finally {
            if (c != null) c.close();
        }
        return null;
    }

    public static int execUpdate(Context context, int id, ContentValues values) {
        return context.getContentResolver().update(FotoSql.SQL_TABLE_EXTERNAL_CONTENT_URI, values, SQL_COL_PK + " = ?", new String[]{Integer.toString(id)});
    }

    public static Uri execInsert(Context context, ContentValues values) {
        return context.getContentResolver().insert(FotoSql.SQL_TABLE_EXTERNAL_CONTENT_URI, values);
    }

    @NonNull
    public static CursorLoader createCursorLoader(Context context, final QueryParameter query) {
        final CursorLoader loader = new CursorLoaderWithException(context, query);
        return loader;
    }

    public static void execDeleteByPath(Activity context, String parentDirString) {
        int delCount = FotoSql.deleteMedia(context.getContentResolver(), FILTER_EXPR_PATH_LIKE, new String[] {parentDirString + "/%"}, true);
        if (Global.debugEnabledSql) {
            Log.i(Global.LOG_CONTEXT, "FotoSql.deleted(NoMedia='" + parentDirString +
                    "') : " + delCount + " db records" );
        }
    }

    /**
     * Deletes media items specified by where with the option to prevent cascade delete of the image.
     */
    public static int deleteMedia(ContentResolver contentResolver, String where, String[] selectionArgs, boolean preventDeleteImage)
    {
        int delCount = 0;
        try {
            if (preventDeleteImage) {
                // set SQL_COL_PATH empty so sql-delete cannot cascade delete the referenced image-file via delete trigger
                ContentValues values = new ContentValues();
                values.put(FotoSql.SQL_COL_PATH, DELETED_FILE_MARKER);
                contentResolver.update(FotoSql.SQL_TABLE_EXTERNAL_CONTENT_URI, values, where, selectionArgs);

                where = FotoSql.SQL_COL_PATH + " is null";
                delCount = contentResolver.delete(FotoSql.SQL_TABLE_EXTERNAL_CONTENT_URI, where, null);
            } else {
                delCount = contentResolver.delete(FotoSql.SQL_TABLE_EXTERNAL_CONTENT_URI, where, selectionArgs);
            }
        } catch (Exception ex) {
            // null pointer exception when delete matches not items??
            final String msg = "FotoSql.deleteMedia("
                    + QueryParameter.toString(FotoSql.SQL_TABLE_EXTERNAL_CONTENT_URI.toString(),null
                        , where, selectionArgs, null)
                    + " : " + ex.getMessage();
            Log.e(Global.LOG_CONTEXT, msg, ex);

        }
        return delCount;
    }

    /** converts imageID to content-uri */
    public static Uri getUri(long imageID) {
        return Uri.parse(
                getUriString(imageID));
    }

    @NonNull
    public static String getUriString(long imageID) {
        return SQL_TABLE_EXTERNAL_CONTENT_URI.toString() + "/" + imageID;
    }

    /** converts internal ID-list to string array of filenNames via media database. */
    public static String[] getFileNames(Context context, SelectedItems items) {
        if (!items.isEmpty()) {
            ArrayList<String> result = new ArrayList<>();

            QueryParameter parameters = new QueryParameter(queryDetail);
            setWhereSelectionPks(parameters, items);

            Cursor cursor = null;

            try {
                cursor = requery(context, parameters.toColumns(), parameters.toFrom(), parameters.toAndroidWhere(), parameters.toOrderBy(), parameters.toAndroidParameters());

                int colPath = cursor.getColumnIndex(SQL_COL_DISPLAY_TEXT);
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

    private static Cursor requery(final Context context, final String[] sqlProjection, final String from, final String sqlWhereStatement, final String sqlSortOrder, final String... sqlWhereParameters) {
        Cursor result = context.getContentResolver().query(Uri.parse(from), // Table to query
                sqlProjection,             // Projection to return
                sqlWhereStatement,        // No selection clause
                sqlWhereParameters,       // No selection arguments
                sqlSortOrder              // Default sort order
        );

        return result;

    }

    public static class CursorLoaderWithException extends CursorLoader {
        private final QueryParameter query;
        private Exception mException;

        public CursorLoaderWithException(Context context, QueryParameter query) {
            super(context, Uri.parse(query.toFrom()), query.toColumns(), query.toAndroidWhere(), query.toAndroidParameters(), query.toOrderBy());
            this.query = query;
        }

        @Override
        public Cursor loadInBackground() {
            mException = null;
            try {
                Cursor result = super.loadInBackground();
                return result;
            } catch (Exception ex) {
                final String msg = "FotoSql.createCursorLoader()#loadInBackground failed:\n\t" + query.toSqlString();
                Log.e(Global.LOG_CONTEXT, msg, ex);
                mException = ex;
                return null;
            }
        }

        public QueryParameter getQuery() {
            return query;
        }

        public Exception getException() {
            return mException;
        }
    }
}



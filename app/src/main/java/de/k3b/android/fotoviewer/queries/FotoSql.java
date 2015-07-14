package de.k3b.android.fotoviewer.queries;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.util.Date;

import de.k3b.android.fotoviewer.Global;
import de.k3b.android.fotoviewer.R;
import de.k3b.database.QueryParameter;
import de.k3b.io.DirectoryFormatter;
import de.k3b.io.IGalleryFilter;

/**
 * contains all SQL needed to query the android gallery
 *
 * Created by k3b on 04.06.2015.
 */
public class FotoSql {
//    private static final int PER_DAY = 1000 * 60 * 60 * 24;
//    public static final String SQL_EXPR_DAY = "(ROUND("
//            + MediaStore.Images.Media.SQL_COL_DATE_TAKEN + "/" + PER_DAY + ") * " + PER_DAY + ")";

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

    public static final int QUERY_TYPE_GROUP_DEFAULT = QUERY_TYPE_GROUP_ALBUM;
    public static final int QUERY_TYPE_DEFAULT = QUERY_TYPE_GALLERY;

    public static final Uri SQL_TABLE_EXTERNAL_CONTENT_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

    // columns that must be avaulable in the Cursor
    public static final String SQL_COL_PK = MediaStore.Images.Media._ID;
    public static final String SQL_COL_DISPLAY_TEXT = "disp_txt";
    public static final String SQL_COL_LAT = MediaStore.Images.Media.LATITUDE;
    public static final String SQL_COL_LON = MediaStore.Images.Media.LONGITUDE;
    public static final String SQL_COL_GPS = MediaStore.Images.Media.LONGITUDE;
    public static final String SQL_COL_COUNT = "count";

    public static final String SQL_COL_DATE_TAKEN = MediaStore.Images.Media.DATE_TAKEN;
    public static final String SQL_COL_PATH = MediaStore.Images.Media.DATA;

    // same format as dir. i.e. description='/2014/12/24/' or '/mnt/sdcard/pictures/'
    public static final String SQL_EXPR_DAY = "strftime('/%Y/%m/%d/', " + SQL_COL_DATE_TAKEN + " /1000, 'unixepoch', 'localtime')";

    public static final QueryParameterParcelable queryGroupByDate = (QueryParameterParcelable) new QueryParameterParcelable()
            .setID(QUERY_TYPE_GROUP_DATE)
            .addColumn(
                    "max(" + SQL_COL_PK + ") AS " + SQL_COL_PK,
                    SQL_EXPR_DAY + " AS " + SQL_COL_DISPLAY_TEXT,
                    "count(*) AS " + SQL_COL_COUNT,
                    "max(" + SQL_COL_GPS + ") AS " + SQL_COL_GPS)
            .addFrom(SQL_TABLE_EXTERNAL_CONTENT_URI.toString())
            .addGroupBy(SQL_EXPR_DAY)
            .addOrderBy(SQL_EXPR_DAY);


    public static final String SQL_EXPR_FOLDER = "substr(" + SQL_COL_PATH + ",1,length(" + SQL_COL_PATH + ") - length(" + MediaStore.Images.Media.DISPLAY_NAME + "))";
    public static final QueryParameterParcelable queryGroupByDir = (QueryParameterParcelable) new QueryParameterParcelable()
            .setID(QUERY_TYPE_GROUP_ALBUM)
            .addColumn(
                    "max(" + SQL_COL_PK + ") AS " + SQL_COL_PK,
                    SQL_EXPR_FOLDER + " AS " + SQL_COL_DISPLAY_TEXT,
                    "count(*) AS " + SQL_COL_COUNT,
                    "max(" + SQL_COL_GPS + ") AS " + SQL_COL_GPS)
            .addFrom(SQL_TABLE_EXTERNAL_CONTENT_URI.toString())
            .addGroupBy(SQL_EXPR_FOLDER)
            .addOrderBy(SQL_EXPR_FOLDER);

//    public static final int DEFAULT_NUMBER_OF_DECIMAL_DIGITS = 2;
    public static final QueryParameterParcelable queryGroupByPlace = getQueryGroupByPlace(100);

    public static QueryParameterParcelable getQueryGroupByPlace(int factor) {
//        String SQL_EXPR_LAT = "(round(" + SQL_COL_LAT + ", "+ numberOfDecimalDigits + "))";
//        String SQL_EXPR_LON = "(round(" + SQL_COL_LON + ", "+ numberOfDecimalDigits + "))";
        String SQL_EXPR_LAT = "(round(" + SQL_COL_LAT + " * " + factor + ") /" + factor + ")";
        String SQL_EXPR_LON = "(round(" + SQL_COL_LON + " * " + factor + ") /" + factor + ")";

        QueryParameterParcelable result = new QueryParameterParcelable();

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






    public static final QueryParameterParcelable queryDetail = (QueryParameterParcelable) new QueryParameterParcelable()
            .setID(QUERY_TYPE_GALLERY)
            .addColumn(
                    SQL_COL_PK,
                    SQL_COL_PATH + " AS " + SQL_COL_DISPLAY_TEXT,
                    "0 AS " + SQL_COL_COUNT,
                    SQL_COL_GPS)
            .addFrom(SQL_TABLE_EXTERNAL_CONTENT_URI.toString())
            .addOrderBy(SQL_COL_PATH);

    public static void setWhereFilter(QueryParameterParcelable parameters, IGalleryFilter filter) {
        if ((parameters != null) && (filter != null)) {
            parameters.clearWhere();

            if (filter.getLatitudeMin() != 0) parameters.addWhere(SQL_COL_LAT + " >= ?", DirectoryFormatter.getLatLon(filter.getLatitudeMin()));
            if (filter.getLatitudeMax() != 0) parameters.addWhere(SQL_COL_LAT + " < ?", DirectoryFormatter.getLatLon(filter.getLatitudeMax()));
            if (filter.getLogituedMin() != 0) parameters.addWhere(SQL_COL_LON + " >= ?", DirectoryFormatter.getLatLon((filter.getLogituedMin())));
            if (filter.getLogituedMax() != 0) parameters.addWhere(SQL_COL_LON + " < ?", DirectoryFormatter.getLatLon((filter.getLogituedMax())));

            if (filter.getDateMin() != 0) parameters.addWhere(SQL_COL_DATE_TAKEN + " >= ?", Double.toString(filter.getDateMin()));
            if (filter.getDateMax() != 0) parameters.addWhere(SQL_COL_DATE_TAKEN + " < ?", Double.toString(filter.getDateMax()));

            String path = filter.getPath();
            if ((path != null) && (path.length() > 0)) parameters.addWhere(SQL_COL_PATH + " like ?", path);
        }
    }

    public static String getFilter(Cursor cursor, QueryParameterParcelable parameters, String description) {
        if ((parameters != null) && (parameters.getID() == QUERY_TYPE_GROUP_ALBUM)) {
            return description;
        }
        return null;
    }

    public static void addWhereFilter(QueryParameterParcelable parameters, String filterParameter) {
        if ((parameters != null) && (parameters.getID() == QUERY_TYPE_GROUP_ALBUM) && (filterParameter != null)) {
            parameters.addWhere(SQL_EXPR_FOLDER + " = ?", filterParameter);
        }
    }

    public static void addPathWhere(QueryParameterParcelable newQuery, String selectedAbsolutePath, int dirQueryID) {
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
    private static void addWhereDirectoryPath(QueryParameterParcelable newQuery, String selectedAbsolutePath) {
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
    private static void addWhereDatePath(QueryParameterParcelable newQuery, String selectedAbsolutePath) {
        Date from = new Date();
        Date to = new Date();

        DirectoryFormatter.getDates(selectedAbsolutePath, from, to);

        if (to.getTime() == 0) {
            newQuery
                    .addWhere(SQL_COL_DATE_TAKEN + " in (0,-1, null)")
                    .addOrderBy(SQL_COL_DATE_TAKEN + " desc");
        } else {
            newQuery
                    .addWhere(SQL_COL_DATE_TAKEN + " >= ?", "" + from.getTime())
                    .addWhere(SQL_COL_DATE_TAKEN + " < ?", "" + to.getTime())
                    .addOrderBy(SQL_COL_DATE_TAKEN + " desc");
        }
    }

    public static QueryParameterParcelable getQuery(int queryID) {
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
                return queryGroupByPlace;
            default:
                Log.e(Global.LOG_CONTEXT, "FotoSql.getQuery(" + queryID + "): unknown ID");
                return null;
        }
    }

    public static String getName(Context context, int id) {
        switch (id) {
            case SORT_BY_DATE:
                return context.getString(R.string.gallery_date);
            case SORT_BY_NAME:
                return context.getString(R.string.gallery_file_name);
            case SORT_BY_LOCATION:
                return context.getString(R.string.gallery_location);
            case SORT_BY_NAME_LEN:
                return context.getString(R.string.sort_by_name_len);

            case QUERY_TYPE_GALLERY:
                return context.getString(R.string.gallery_foto);
            case QUERY_TYPE_GROUP_DATE:
                return context.getString(R.string.gallery_date);
            case QUERY_TYPE_GROUP_ALBUM:
                return context.getString(R.string.gallery_album);
            case QUERY_TYPE_GROUP_PLACE:
                return context.getString(R.string.gallery_location);
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
}



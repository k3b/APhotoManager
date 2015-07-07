package de.k3b.android.fotoviewer.queries;

import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.util.GregorianCalendar;

import de.k3b.android.fotoviewer.Global;
import de.k3b.android.fotoviewer.R;
import de.k3b.io.Directory;

/**
 * contains all SQL needed to query the android gallery
 *
 * Created by k3b on 04.06.2015.
 */
public class FotoSql {
//    private static final int PER_DAY = 1000 * 60 * 60 * 24;
//    public static final String SQL_EXPR_DAY = "(ROUND("
//            + MediaStore.Images.Media.DATE_TAKEN + "/" + PER_DAY + ") * " + PER_DAY + ")";

    public static final int SORT_BY_DATE = R.string.gallery_date;
    public static final int SORT_BY_ALBUM = R.string.gallery_directory;
    public static final int SORT_BY_LOCATION = R.string.gallery_location;
    public static final int SORT_BY_NAME_LEN = R.string.sort_by_name_len;

    public static final int QUERY_TYPE_UNDEFINED = 0;
    public static final int QUERY_TYPE_GALLERY = R.string.gallery_foto;
    public static final int QUERY_TYPE_GROUP_DATE = R.string.gallery_date;
    public static final int QUERY_TYPE_GROUP_ALBUM = R.string.gallery_directory;

    public static final int QUERY_TYPE_GROUP_DEFAULT = QUERY_TYPE_GROUP_ALBUM;
    public static final int QUERY_TYPE_DEFAULT = QUERY_TYPE_GALLERY;

    public static final Uri SQL_TABLE_EXTERNAL_CONTENT_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

    // columns that must be avaulable in the Cursor
    public static final String SQL_COL_PK = MediaStore.Images.Media._ID;
    public static final String SQL_COL_DISPLAY_TEXT = "disp_txt";
    public static final String SQL_COL_GPS = MediaStore.Images.Media.LONGITUDE;
    public static final String SQL_COL_COUNT = "count";

    public static final String DATE_TAKEN = MediaStore.Images.Media.DATE_TAKEN;
    public static final String SQL_COL_PATH = MediaStore.Images.Media.DATA;

    // same format as dir. i.e. description='/2014/12/24/' or '/mnt/sdcard/pictures/'
    public static final String SQL_EXPR_DAY = "strftime('/%Y/%m/%d/', " + DATE_TAKEN + " /1000, 'unixepoch', 'localtime')";

    public static final QueryParameterParcelable queryGroupByDate = (QueryParameterParcelable) new QueryParameterParcelable()
            .setID(QUERY_TYPE_GROUP_DATE)
            .addColumn(
                    "min(" + SQL_COL_PK + ") AS " + SQL_COL_PK,
                    SQL_EXPR_DAY + " AS " + SQL_COL_DISPLAY_TEXT,
                    "count(*) AS " + SQL_COL_COUNT,
                    "max(" + SQL_COL_GPS + ") AS " + SQL_COL_GPS)
            .addFrom(SQL_TABLE_EXTERNAL_CONTENT_URI.toString())
            .addGroupBy(SQL_EXPR_DAY)
            .addOrderBy(SQL_EXPR_DAY)
            ;


    public static final String SQL_EXPR_FOLDER = "substr(" + SQL_COL_PATH + ",1,length(" + SQL_COL_PATH + ") - length(" + MediaStore.Images.Media.DISPLAY_NAME + "))";
    public static final QueryParameterParcelable queryGroupByDir = (QueryParameterParcelable) new QueryParameterParcelable()
            .setID(QUERY_TYPE_GROUP_ALBUM)
            .addColumn(
                    "min(" + SQL_COL_PK + ") AS " + SQL_COL_PK,
                    SQL_EXPR_FOLDER + " AS " + SQL_COL_DISPLAY_TEXT,
                    "count(*) AS " + SQL_COL_COUNT,
                    "max(" + SQL_COL_GPS + ") AS " + SQL_COL_GPS)
            .addFrom(SQL_TABLE_EXTERNAL_CONTENT_URI.toString())
            .addGroupBy(SQL_EXPR_FOLDER)
            .addOrderBy(SQL_EXPR_FOLDER)
            ;
    public static final QueryParameterParcelable queryDetail = (QueryParameterParcelable) new QueryParameterParcelable()
            .setID(QUERY_TYPE_GALLERY)
            .addColumn(
                    SQL_COL_PK,
                    SQL_COL_PATH + " AS " + SQL_COL_DISPLAY_TEXT,
                    "0 AS " + SQL_COL_COUNT,
                    SQL_COL_GPS)
            .addFrom(SQL_TABLE_EXTERNAL_CONTENT_URI.toString())
            .addOrderBy(SQL_COL_PATH)
            ;

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

    /** directory path i.e. /mnt/sdcard/pictures/ */
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

    /** path has format /year/month/day/ or /year/month/ or /year/ or / */
    private static void addWhereDatePath(QueryParameterParcelable newQuery, String selectedAbsolutePath) {
        Integer year = null;
        Integer month = null;
        Integer day = null;

        String parts[] = selectedAbsolutePath.split(Directory.PATH_DELIMITER);

        for (String part : parts) {
            if ((part != null) && ((part.length() > 0))) {
                try {
                    Integer value = Integer.parseInt(part);
                    if (year == null) year = value;
                    else if (month == null) month = value;
                    else if (day == null) day = value;
                } catch (NumberFormatException ex) {

                }
            }
        }

        if (year != null) {
            int yearFrom = year.intValue();

            if (yearFrom == 1970) {
                newQuery
                        .addWhere(DATE_TAKEN + " in (0,-1, null)")
                        .addOrderBy(DATE_TAKEN + " desc");

            } else {
                int monthFrom = (month != null) ? month.intValue() : 1;
                int dayFrom = (day != null) ? day.intValue() : 1;

                GregorianCalendar from = new GregorianCalendar(yearFrom, monthFrom - 1, dayFrom, 0, 0, 0);

                int field = GregorianCalendar.YEAR;
                if (month != null) field = GregorianCalendar.MONTH;
                if (day != null) field = GregorianCalendar.DAY_OF_MONTH;

                GregorianCalendar to = new GregorianCalendar();
                to.setTimeInMillis(from.getTimeInMillis());
                to.add(field, 1);

                newQuery
                        .addWhere(DATE_TAKEN + " >= ?", "" + from.getTimeInMillis())
                        .addWhere(DATE_TAKEN + " < ?", "" + to.getTimeInMillis())
                        .addOrderBy(DATE_TAKEN + " desc");
            }
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
            default:
                Log.e(Global.LOG_CONTEXT,"FotoSql.getQuery(" +queryID+"): unknown ID");
                return null;
        }
    }

}

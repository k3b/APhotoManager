package de.k3b.android.fotoviewer.gallery.cursor;

import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import de.k3b.android.database.QueryParameterParcelable;
import de.k3b.android.fotoviewer.R;

/**
 * SQL to query the android gallery
 *
 * Created by k3b on 04.06.2015.
 */
public class FotoSql {
    public static final Uri SQL_TABLE_EXTERNAL_CONTENT_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    // columns that must be avaulable in the Cursor
    public static final String SQL_COL_PK = MediaStore.Images.Media._ID;
    public static final String SQL_COL_DESCRIPTION = MediaStore.Images.Media.DATA;
    public static final String SQL_COL_GPS = MediaStore.Images.Media.LONGITUDE;
    public static final String SQL_COL_COUNT = "count";

    public static final String SQL_EXPR_FOLDER = "substr(" + SQL_COL_DESCRIPTION + ",1,length(" + SQL_COL_DESCRIPTION + ") - length(" + MediaStore.Images.Media.DISPLAY_NAME + "))";
    public static final QueryParameterParcelable queryDirs = (QueryParameterParcelable) new QueryParameterParcelable()
            .setID(R.string.directory_gallery)
            .addColumn(
                    "min(" + SQL_COL_PK + ") AS " + SQL_COL_PK,
                    SQL_EXPR_FOLDER + " AS " + SQL_COL_DESCRIPTION,
                    "count(*) AS " + SQL_COL_COUNT,
                    "max(" + SQL_COL_GPS + ") AS " + SQL_COL_GPS)
            .addFrom(SQL_TABLE_EXTERNAL_CONTENT_URI.toString())
            .addGroupBy(SQL_EXPR_FOLDER)
            .addOrderBy(SQL_EXPR_FOLDER)
            ;
    public static final QueryParameterParcelable queryDetail = (QueryParameterParcelable) new QueryParameterParcelable()
            .setID(R.string.foto_gallery)
            .addColumn(
                    SQL_COL_PK,
                    SQL_COL_DESCRIPTION,
                    "0 AS " + SQL_COL_COUNT,
                    SQL_COL_GPS)
            .addFrom(SQL_TABLE_EXTERNAL_CONTENT_URI.toString())
            .addOrderBy(SQL_COL_DESCRIPTION)
            ;

    public static String getFilter(Cursor cursor, QueryParameterParcelable parameters, String description) {
        if ((parameters != null) && (parameters.getID() == R.string.directory_gallery)) {
            return description;
        }
        return null;
    }

    public static void addWhereFilter(QueryParameterParcelable parameters, String filterParameter) {
        if ((parameters != null) && (parameters.getID() == R.string.directory_gallery) && (filterParameter != null)) {
            parameters.addWhere(SQL_EXPR_FOLDER + " = ?", filterParameter);
        }
    }
}

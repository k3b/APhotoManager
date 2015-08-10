package de.k3b.android.util;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.queries.QueryParameterParcelable;
import de.k3b.database.SelectedItems;

/**
 * SelectedItems with media support
 * Created by k3b on 03.08.2015.
 */
public class SelectedFotos extends SelectedItems {
    private Activity mContext;

    public void setContext(Activity context) {
        mContext = context;
    }

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

    public String[] getFileNames() {
        if (!isEmpty()) {
            ArrayList<String> result = new ArrayList<>();

            QueryParameterParcelable parameters = new QueryParameterParcelable(FotoSql.queryDetail);
            FotoSql.addWhereSelection(parameters, this);

            Cursor cursor = null;

            try {
                cursor = requery(mContext, parameters.toColumns(), parameters.toFrom(), parameters.toAndroidWhere(), parameters.toOrderBy(), parameters.toAndroidParameters());

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

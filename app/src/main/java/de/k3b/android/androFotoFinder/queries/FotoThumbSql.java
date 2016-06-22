package de.k3b.android.androFotoFinder.queries;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.util.OsUtils;
import de.k3b.database.QueryParameter;

/**
 * Created by k3b on 21.06.2016.
 */
public class FotoThumbSql {
    public static final Uri SQL_TABLE_EXTERNAL_CONTENT_URI = MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI;

    // columns that must be avaulable in the Cursor
    public static final String SQL_COL_PK = MediaStore.Images.Thumbnails._ID;
    public static final String SQL_COL_IMAGE_ID = MediaStore.Images.Thumbnails.IMAGE_ID;
    public static final String SQL_COL_PATH = MediaStore.Images.Thumbnails.DATA;
    public static final String SQL_COL_WIDTH = MediaStore.Images.Thumbnails.WIDTH;
    public static final String SQL_COL_HEIGHT = MediaStore.Images.Thumbnails.HEIGHT;

    public static final String SQL_COL_COUNT = "count";
    public static final String SQL_COL_SIZE = "size";

    public static QueryParameter getQueryThumbSizeByPath(String imagePath, String thumpPath) {

        return new QueryParameter()
                // .setID(QUERY_TYPE_GROUP_DATE)
                .addColumn("count(*) as " + SQL_COL_COUNT,
                        "sum(" + SQL_COL_WIDTH + " * " + SQL_COL_HEIGHT + ") AS " + SQL_COL_SIZE)
                .addFrom(SQL_TABLE_EXTERNAL_CONTENT_URI.toString())
                .addWhere(SQL_COL_PATH + " like ?", thumpPath + "%")
                .addWhere(SQL_COL_IMAGE_ID + " in (SELECT " + FotoSql.SQL_COL_PK +
                        " from " + "images" + " where " + FotoSql.SQL_COL_PATH +
                                " like ?)", imagePath + "%")
                ;
    }

    public static QueryParameter getQueryImageSizeByPath(String imagePath) {

        return new QueryParameter()
                // .setID(QUERY_TYPE_GROUP_DATE)
                .addColumn("count(*) as " + SQL_COL_COUNT,
                        "sum(" + FotoSql.SQL_COL_SIZE + ") AS " + SQL_COL_SIZE)
                .addFrom(FotoSql.SQL_TABLE_EXTERNAL_CONTENT_URI.toString())
                .addWhere(FotoSql.SQL_COL_PATH +
                        " like ?", imagePath + "%")
                ;
    }

    public static String getStatistic(Context context, QueryParameter query, String type, String path, double factor) {
		if (path != null) {
			// http://docs.oracle.com/javase/1.5.0/docs/api/java/util/Formatter.html#syntax
			// ie "Thumbnail[/mnt/sdcard0/] 122 (13.5 MB)"
			String format = "%1$s[%2$s] #%3$d (%4$01.1f MB)\n";
			// java.text.MessageFormat("The disk \"{1}\" contains {0} file(s).").format(testArgs);
			
			long countThumbInternal = 0;
			double sizeKBThumbInternal  = 0.0;
			
            Cursor c = null;
            try {
				if (Global.debugEnabledSql) {
                    Log.i(Global.LOG_CONTEXT, "getStatistic\n\t" + query.toSqlString());
                }
                c = FotoSql.createCursorForQuery(context, query);
                if (c.moveToFirst()) {
                    countThumbInternal = c.getLong(0);
                    sizeKBThumbInternal = (factor * c.getLong(1)) /  1048576; // 1MB= 1048576 Bytes. https://en.wikipedia.org/wiki/Megabyte
					
					return String.format(format, type, path,countThumbInternal,sizeKBThumbInternal);
					
                }
            } catch (Exception ex) {
                Log.e(Global.LOG_CONTEXT, "getStatistic() : error executing " + query, ex);
            } finally {
                if (c != null) c.close();
            }

		}
		return "";
	}

    public static String formatDirStatistic(Context context, String imagePath) {
		StringBuilder result = new StringBuilder();

        String[] thumpPaths = OsUtils.getExternalStorageDirs();
		result.append(getStatistic(context, getQueryImageSizeByPath(imagePath), "Image", imagePath, 1.0));
		
		if (thumpPaths != null) {
			for (String thumpPath : thumpPaths) {
				if (thumpPath != null) {
					for (String path : thumpPath.split(";")) {
						// thumbnail only has width and hight but no size. Estimation jpg thumbnail size is 25% of width*height.
						result.append(getStatistic(context, getQueryThumbSizeByPath(imagePath, path), "Tumbnail", path, 0.25));
					}
				}
			}
		}

        return result.toString();
    }


}

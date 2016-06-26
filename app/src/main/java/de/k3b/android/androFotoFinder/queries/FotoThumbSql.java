package de.k3b.android.androFotoFinder.queries;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import de.k3b.IBackgroundProcess;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.util.MediaScanner;
import de.k3b.android.util.OsUtils;
import de.k3b.database.QueryParameter;
import de.k3b.database.SelectedItems;

/**
 * Created by k3b on 21.06.2016.
 */
public class FotoThumbSql {
    private static String mDebugPrefix = "FotoThumbSql ";
    public static final Uri SQL_TABLE_EXTERNAL_CONTENT_URI = MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI;

    // columns that must be avaulable in the Cursor
    public static final String SQL_COL_PK = MediaStore.Images.Thumbnails._ID;
    public static final String SQL_COL_IMAGE_ID = MediaStore.Images.Thumbnails.IMAGE_ID;
    public static final String SQL_COL_PATH = MediaStore.Images.Thumbnails.DATA;
    public static final String SQL_COL_WIDTH = MediaStore.Images.Thumbnails.WIDTH;
    public static final String SQL_COL_HEIGHT = MediaStore.Images.Thumbnails.HEIGHT;

    public static final String SQL_COL_KIND = MediaStore.Images.Thumbnails.KIND;
    public static final String SQL_COL_COUNT = "count";
    public static final String SQL_COL_SIZE = "size";
    private static final String TABLE_IMAGES = "images";
    static final String WHERE_THUMB_IS_ORPHAN = SQL_COL_IMAGE_ID + " not in (SELECT " + FotoSql.SQL_COL_PK +
            " from " + TABLE_IMAGES + ")";
    public static final String THUMBNAIL_DIR_NAME = ".thumbnails";
    private static final int CHUNK_SIZE = 100;

    public static QueryParameter getQueryThumbSizeByPath(String imagePath, String thumpPath) {

        return new QueryParameter()
                // .setID(QUERY_TYPE_GROUP_DATE)
                .addColumn(
                        "count(*) as " + SQL_COL_COUNT,
                        "sum(" + SQL_COL_WIDTH + " * " + SQL_COL_HEIGHT + ") AS " + SQL_COL_SIZE,
                        SQL_COL_KIND
                )
                .addFrom(SQL_TABLE_EXTERNAL_CONTENT_URI.toString())
                .addWhere(SQL_COL_PATH + " like ?", thumpPath + "%")
                .addWhere(SQL_COL_IMAGE_ID + " in (SELECT " + FotoSql.SQL_COL_PK +
                        " from " + TABLE_IMAGES + " where " + FotoSql.SQL_COL_PATH +
                                " like ?)", imagePath + "%")
                .addGroupBy(SQL_COL_KIND)
                .addOrderBy(SQL_COL_KIND)
                ;
    }

    public static QueryParameter getQueryThumbSizeByOrphan() {

        return new QueryParameter()
                // .setID(QUERY_TYPE_GROUP_DATE)
                .addColumn(
                        "count(*) as " + SQL_COL_COUNT,
                        "sum(" + SQL_COL_WIDTH + " * " + SQL_COL_HEIGHT + ") AS " + SQL_COL_SIZE,
                        SQL_COL_KIND
                )
                .addFrom(SQL_TABLE_EXTERNAL_CONTENT_URI.toString())
                .addWhere(WHERE_THUMB_IS_ORPHAN)
                .addGroupBy(SQL_COL_KIND)
                .addOrderBy(SQL_COL_KIND)
                ;
    }

    public static QueryParameter getQueryImageSizeByPath(String imagePath) {

        return new QueryParameter()
                // .setID(QUERY_TYPE_GROUP_DATE)
                .addColumn(
                        "count(*) as " + SQL_COL_COUNT,
                        "sum(" + FotoSql.SQL_COL_SIZE + ") AS " + SQL_COL_SIZE)
                .addFrom(FotoSql.SQL_TABLE_EXTERNAL_CONTENT_URI.toString())
                .addWhere(FotoSql.SQL_COL_PATH +
                        " like ?", imagePath + "%")
                ;
    }

    private static String getNext(Cursor c, int dbPathOffset) {
        if (!c.moveToNext()) return null;
        return c.getString(0).substring(dbPathOffset);
    }

    /** creats statistics row */
    private static String getStatistic(Context context, QueryParameter query, String type, String path, double factor) {
        StringBuilder result = new StringBuilder();
		if (path != null) {
			// http://docs.oracle.com/javase/1.5.0/docs/api/java/util/Formatter.html#syntax
			// ie "Thumbnail[/mnt/sdcard0/] 122 (13.5 MB)"
			String format = "%1$s[%2$s %5$d] #%3$d (%4$01.1f MB)\n";
			// java.text.MessageFormat("The disk \"{1}\" contains {0} file(s).").format(testArgs);
			
			long countThumbInternal = 0;
			double sizeKBThumbInternal  = 0.0;
            long kind = 0;

            Cursor c = null;
            try {
                c = FotoSql.createCursorForQuery(context, query);
                if (Global.debugEnabledSql) {
                    Log.i(Global.LOG_CONTEXT, mDebugPrefix + "getStatistic " + c.getCount() +
                            "\n\t" + query.toSqlString());
                }
                boolean hasKind = c.getColumnCount() > 2;
                while (c.moveToNext()) {
                    countThumbInternal = c.getLong(0);
                    sizeKBThumbInternal = (factor * c.getLong(1)) /  1048576; // 1MB= 1048576 Bytes. https://en.wikipedia.org/wiki/Megabyte
                    kind = (hasKind) ? c.getLong(2) : 0;

					result.append(String.format(format, type, path,countThumbInternal,sizeKBThumbInternal, kind));
					
                }
            } catch (Exception ex) {
                Log.e(Global.LOG_CONTEXT, mDebugPrefix + "getStatistic() : error executing " + query, ex);
            } finally {
                if (c != null) c.close();
            }

		}
		return result.toString();
	}

    /** get all existing getThumbRoot-Dirs */
    public static File[] getThumbRootFiles() {
        File[] mountFiles = OsUtils.getExternalStorageDirFiles();
        if (mountFiles != null) {
            for (int i = mountFiles.length -1; i >= 0; i--) {
                final File rootCandidate = getThumbDir(mountFiles[i]);
                mountFiles[i] = rootCandidate.exists() ?  rootCandidate : null;
            }

            return mountFiles;
        }
        return null;
    }

    /** getThumbDir("/mnt/sdcard") = "/mnt/sdcard/DCIM/.thumbnails" */
    public static File getThumbDir(File root) {
        // /mnt/sdcard/DCIM/.thumbnails/12345.jpg

        return OsUtils.buildPath(root, Environment.DIRECTORY_DCIM, THUMBNAIL_DIR_NAME);
    }

    public static String formatDirStatistic(Context context, String imagePath) {
		StringBuilder result = new StringBuilder();

		result
            .append(getStatistic(context, getQueryImageSizeByPath(imagePath), "Image", imagePath, 1.0))
            .append("\n")
            .append(getStatistic(context, getQueryThumbSizeByOrphan(), "Orphan Tumbnail", imagePath, 0.25))
        ;

        File[] thumpPaths = getThumbRootFiles();
        if (thumpPaths != null) {
			for (File thumpPath : thumpPaths) {
				if (thumpPath != null) {
					String path = thumpPath.getAbsolutePath() + "/";

                    // thumbnail only has width and hight but no size. Estimation jpg thumbnail size is 25% of width*height.
                    result.append(getStatistic(context, getQueryThumbSizeByPath(imagePath, path), "Tumbnail", path, 0.25));
				}
			}
		}

        return result.toString();
    }

    // /mnt/sdcard/DCIM/.thumbnails/12345.jpg
    public static void scanOrphans(Context context, File thumbDirRoot,
                                   ArrayList<Long> dbIds4Delete, ArrayList<String> files4Delete,
                                   IBackgroundProcess<Integer> taskControl, Integer step) {
        if (taskControl != null) {
            if (taskControl.isCancelled_()) return;
            taskControl.publishProgress_(0,0, step++);
        }

        int dbPathOffset = thumbDirRoot.getAbsolutePath().length() + 1;
        QueryParameter query = new QueryParameter()
                .addColumn(FotoThumbSql.SQL_COL_PATH, FotoThumbSql.SQL_COL_PK)
                .addFrom(SQL_TABLE_EXTERNAL_CONTENT_URI.toString())
                .addWhere(SQL_COL_PATH + " like ?", thumbDirRoot.getAbsolutePath() + "/%")
                .addOrderBy(SQL_COL_PATH + " ASC");

        Cursor c = null;
        try {
            String[] files = thumbDirRoot.list(MediaScanner.JPG_FILENAME_FILTER);
            Arrays.sort(files); // sorted ascending

            if (taskControl != null) {
                if (taskControl.isCancelled_()) return;
                taskControl.publishProgress_(0,0, step++);
            }

            c = FotoSql.createCursorForQuery(context, query);

            Integer progressCount = c.getCount() + files.length; // items to be processed
            int progressPos = 0; // incremented for every processed file/dbRow
            int progressCountdown = 0; // decremented by processing. if <= 0 update progress

            int curentFilePos = 0;
            String filePath = (curentFilePos < files.length) ? files[curentFilePos++] : null;
            String dbPath = getNext(c, dbPathOffset);

            // ArrayList<Long> dbIds4Delete = new ArrayList<Long>();
            // ArrayList<String> files4Delete = new ArrayList<String>();

            while ((dbPath != null) && (filePath != null)) {
                if ((progressCountdown <= 0) && (taskControl != null)) {
                    if (taskControl.isCancelled_()) return;
                    taskControl.publishProgress_(progressPos,progressCount, step);
                    progressCountdown = CHUNK_SIZE + CHUNK_SIZE;
                }

                progressPos++;progressCountdown--;
                int compareResult = dbPath.compareTo(filePath);
                if (compareResult == 0) {
                    // dbPath == filePath

                    // nothing to do
                    filePath = (curentFilePos < files.length) ? files[curentFilePos++] : null;
                    dbPath = getNext(c, dbPathOffset);
                    progressPos++;progressCountdown--;
                } else if (compareResult < 0) {
                    // dbPath < filePath
                    dbIds4Delete.add(c.getLong(1));
                    dbPath = getNext(c, dbPathOffset);
                } else {
                    // dbPath > filePath

                    files4Delete.add(filePath);
                    filePath = (curentFilePos < files.length) ? files[curentFilePos++] : null;
                }
            }

        } catch (Exception ex) {
            Log.e(Global.LOG_CONTEXT, mDebugPrefix + "scanOrphans() : error executing " + query, ex);
        } finally {
            if (c != null) c.close();
            if (Global.debugEnabledSql) {
                Log.i(Global.LOG_CONTEXT, mDebugPrefix + "scanOrphans OrphanThumb IDs : " + SelectedItems.toString(dbIds4Delete.iterator()));
                Log.i(Global.LOG_CONTEXT, mDebugPrefix + "scanOrphans OrphanThumb Files : " + SelectedItems.toString(files4Delete.iterator()));
            }
        }
    }


    private static List<Long> getOrphanThumbIds(Context context) {
        List<Long> result = new ArrayList<Long>();

        QueryParameter query = new QueryParameter()
                .addColumn(SQL_COL_PK)
                .addFrom(SQL_TABLE_EXTERNAL_CONTENT_URI.toString())
                .addWhere(WHERE_THUMB_IS_ORPHAN)
                ;

        Cursor c = null;
        try {
            c = FotoSql.createCursorForQuery(context, query);
            if (Global.debugEnabledSql) {
                Log.i(Global.LOG_CONTEXT, mDebugPrefix + "getOrphanThumbIds " + c.getCount() +
                        "\n\t" + query.toSqlString());
            }
            while (c.moveToNext()) {
                result.add(c.getLong(0));
            }
        } catch (Exception ex) {
            Log.e(Global.LOG_CONTEXT,mDebugPrefix + "getOrphanThumbIds() : error executing " + query, ex);
        } finally {
            if (c != null) c.close();
        }
        return result;
    }

    public static int deleteOrphanThumbRecords(Context context, IBackgroundProcess<Integer> taskControl, Integer step) {
        if (taskControl != null) {
            if (taskControl.isCancelled_()) return 0;
            taskControl.publishProgress_(0,0, step);
        }
        List<Long> delItems = getOrphanThumbIds(context);

        int delCount = deleteThumbRecords(context, delItems, taskControl, step + 1);
        return delCount;
    }

    public static int deleteThumbRecords(Context context, List<Long> dbIds4Delete, IBackgroundProcess<Integer> taskControl, Integer step) {
        int delCount = 0;
        final int delSize = (dbIds4Delete != null) ? dbIds4Delete.size() : 0;
        if (delSize > 0) {
            int delOffset = 0;

            final Iterator<Long> delIterator = dbIds4Delete.iterator();
            while (delOffset < delSize) {

                if (taskControl != null) {
                    if (taskControl.isCancelled_()) return delCount;
                    taskControl.publishProgress_(delOffset, delSize, step);
                }

                final String sqlWhere = SQL_COL_PK + " in (" +
                        SelectedItems.toString(delIterator, CHUNK_SIZE) + ")";

                delCount += context.getContentResolver().delete(SQL_TABLE_EXTERNAL_CONTENT_URI, sqlWhere, null);
                if (Global.debugEnabledSql) {
                    Log.i(Global.LOG_CONTEXT,mDebugPrefix + "deleteThumbRecords#" + step +
                            " = " + delOffset + "/" + delSize +
                            " : " + sqlWhere);
                }

                delOffset += CHUNK_SIZE;
            }
        } else {
            if (Global.debugEnabledSql) {
                Log.i(Global.LOG_CONTEXT, mDebugPrefix + "deleteThumbRecords#" + step +
                        " nothing found to delete");
            }

        }
        return delCount;
    }

    public static int deleteThumbFiles(File rootDir, ArrayList<String> files4Delete, IBackgroundProcess<Integer> taskControl, Integer step) {
        int delCount = 0;

        if (files4Delete != null) {
            for (String fileName : files4Delete) {
                /*
                if (new File(rootDir, fileName).delete()) {
                    delCount ++;
                }
                */
            }
        }
        return delCount;
    }
}

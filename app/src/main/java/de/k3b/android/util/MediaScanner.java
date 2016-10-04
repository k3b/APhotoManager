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
 
package de.k3b.android.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.database.QueryParameter;
import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.api.IGeoPointInfo;

/**
 * Since android.media.MediaScannerConnection does not work on my android-4.2
 * here is my own implementation.
 *
 * Created by k3b on 14.09.2015.
 */
public class MediaScanner  {
    private static final String CONTEXT = "MediaScanner.";
    private static SimpleDateFormat sFormatter;
    public static final int DEFAULT_SCAN_DEPTH = 22;

    private static MediaScanner sInstance = null;

    static {
        sFormatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        sFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static final FilenameFilter JPG_FILENAME_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            return MediaScanner.isImage(filename, false);
        }
    };

    final Context mContext;

    public MediaScanner(Context context) {
        mContext = context.getApplicationContext();
    }


    public void notifyChanges(Context context, String why) {
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, CONTEXT + "notifyChanges(" + why + ") "
                    + FotoSql.SQL_TABLE_EXTERNAL_CONTENT_URI);
        }
        context.getContentResolver().notifyChange(FotoSql.SQL_TABLE_EXTERNAL_CONTENT_URI, null);
    }

    public static boolean isNoMedia(int maxLevel, String[] pathNames) {
        if (pathNames != null) {
            for(String path : pathNames) {
                if (isNoMedia(path, maxLevel)) {
                    return true;
                }
            }

        }
        return false;
    }

    /** return true, if file is in a ".nomedia" dir */
    public static boolean isNoMedia(String path, int maxLevel) {
        if (path != null) {
            if (path.indexOf("/.") >= 0) {
                return true; // linux convention: folder names starting with "." are hidden
            }
            File file = getDir(path);
            int level = maxLevel;
            while ((--level >= 0) && (file != null)) {
                if (new File(file, ".nomedia").exists()) {
                    return true;
                }
                file = file.getParentFile();
            }
        }
        return false;
    }

    /** return parent of path if path is not a dir. else return path */
    public static File getDir(String path) {
        if ((path == null) || (path.length() == 0)) return null;
        if (path.endsWith("%")) {
            // remove sql wildcard at end of name
            return getDir(new File(path.substring(0,path.length() - 1)));
        }
        return getDir(new File(path));
    }

    /** return parent of file if path is not a dir. else return file */
    private static File getDir(File file) {
        return ((file != null) && (!file.isDirectory())) ? file.getParentFile() : file;
    }

    public int updateMediaDatabase_Android42(Context context, String[] oldPathNames, String... newPathNames) {
        final boolean hasNew = excludeNomediaFiles(newPathNames) > 0;
        final boolean hasOld = excludeNomediaFiles(oldPathNames) > 0;

        if (hasNew && hasOld) {
            return renameInMediaDatabase(context, oldPathNames, newPathNames);
        } else if (hasOld) {
            return deleteInMediaDatabase(context, oldPathNames);
        } if (hasNew) {
            return insertIntoMediaDatabase(context, newPathNames);
        }
        return 0;
    }

    /**
     * Replace all files that either non-jpg or in ".nomedia" folder with null so they wont be
     * processed by media scanner
     *
     * @return number of items left.
     */
    private int excludeNomediaFiles(String[] fullPathNames) {
        int itemsLeft = 0;
        if (fullPathNames != null) {
            // ignore non-jpeg
            for (int i = 0; i < fullPathNames.length; i++) {
                String fullPathName = fullPathNames[i];
                if (fullPathName != null) {
                    if (!isImage(fullPathName, false) || isNoMedia(fullPathName, 22)) {
                        fullPathNames[i] = null;
                    } else {
                        itemsLeft++;
                    }
                }
            }
        }

        return itemsLeft;
    }

    private int insertIntoMediaDatabase(Context context, String[] newPathNames) {
        int modifyCount = 0;

        if ((newPathNames != null) && (newPathNames.length > 0)) {
            if (Global.debugEnabled) {
                Log.i(Global.LOG_CONTEXT, CONTEXT + "A42 scanner starting with " + newPathNames.length + " files " + newPathNames[0] + "...");
            }

            Map<String, Integer> inMediaDb = FotoSql.execGetPathIdMap(context.getApplicationContext(), newPathNames);

            for (String fileName : newPathNames) {
                if (fileName != null) {
                    Integer id = inMediaDb.get(fileName);
                    if (id != null) {
                        // already exists
                        modifyCount += update_Android42(context, id, new File(fileName));
                    } else {
                        modifyCount += insert_Android42(context, new File(fileName));
                    }
                }
            }
        }
        return modifyCount;
    }

    /** delete oldPathNames from media database */
    private int deleteInMediaDatabase(Context context, String[] oldPathNames) {
        int modifyCount = 0;

        if ((oldPathNames != null) && (oldPathNames.length > 0)) {
            String sqlWhere = FotoSql.getWhereInFileNames(oldPathNames);
            try {
                modifyCount = FotoSql.deleteMedia(context.getContentResolver(), sqlWhere, null, true);
                if (Global.debugEnabled) {
                    Log.d(Global.LOG_CONTEXT, CONTEXT + "deleteInMediaDatabase(len=" + oldPathNames.length + ", files='" + oldPathNames[0] + "'...) result count=" + modifyCount);
                }
            } catch (Exception ex) {
                Log.e(Global.LOG_CONTEXT, CONTEXT + "deleteInMediaDatabase(" + sqlWhere + ") error :", ex);
            }
        }

        return modifyCount;
    }

    /** change path and path dependant fields in media database */
    private int renameInMediaDatabase(Context context, String[] oldPathNames, String... newPathNames) {
        if ((oldPathNames != null) && (oldPathNames.length > 0)) {
            if (Global.debugEnabled) {
                Log.i(Global.LOG_CONTEXT, CONTEXT + "renameInMediaDatabase to " + newPathNames.length + " files " + newPathNames[0] + "...");
            }
            Map<String, String> old2NewFileNames = new HashMap<>(oldPathNames.length);
            ArrayList<String> deleteFileNames = new ArrayList<String>();
            ArrayList<String> insertFileNames = new ArrayList<String>();

            for (int i = 0; i < oldPathNames.length; i++) {
                String oldPathName = oldPathNames[i];
                String newPathName = newPathNames[i];

                if ((oldPathName != null) && (newPathName != null)) {
                    old2NewFileNames.put(oldPathName, newPathName);
                } else if (oldPathName != null) {
                    deleteFileNames.add(oldPathName);
                } else if (newPathName != null) {
                    insertFileNames.add(newPathName);
                }
            }

            int modifyCount =
                    deleteInMediaDatabase(context, deleteFileNames.toArray(new String[deleteFileNames.size()]))
                            + renameInMediaDatabase(context, old2NewFileNames)
                            + insertIntoMediaDatabase(context, insertFileNames.toArray(new String[insertFileNames.size()]));
            return modifyCount;
        }
        return 0;
    }

    private int renameInMediaDatabase(Context context, Map<String, String> old2NewFileNames) {
        int modifyCount = 0;
        if (old2NewFileNames.size() > 0) {
            QueryParameter query = new QueryParameter(FotoSql.queryChangePath);
            FotoSql.setWhereFileNames(query, old2NewFileNames.keySet().toArray(new String[old2NewFileNames.size()]));

            Cursor c = null;
            try {
                c = FotoSql.createCursorForQuery(context, query);
                int pkColNo = c.getColumnIndex(FotoSql.SQL_COL_PK);
                int pathColNo = c.getColumnIndex(FotoSql.SQL_COL_PATH);
                while (c.moveToNext()) {
                    String oldPath = c.getString(pathColNo);
                    modifyCount += updatePathRelatedFields(context, c, old2NewFileNames.get(oldPath), pkColNo, pathColNo);
                }
            } catch (Exception ex) {
                Log.e(Global.LOG_CONTEXT, CONTEXT + "execChangePaths() error :", ex);
            } finally {
                if (c != null) c.close();
            }

            if (Global.debugEnabled) {
                Log.d(Global.LOG_CONTEXT, CONTEXT + "execChangePaths() result count=" + modifyCount);
            }
        }
        return modifyCount;
    }

    /** updates values with current values of file */
    protected void getExifFromFile(ContentValues values, File file) {
        String absolutePath = file.getAbsolutePath();
        setPathRelatedFieldsIfNeccessary(values, absolutePath, null);

        values.put(MediaStore.MediaColumns.DATE_MODIFIED, file.lastModified() / 1000);
        values.put(MediaStore.MediaColumns.SIZE, file.length());

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; // only need with/height but not content
        BitmapFactory.decodeFile(absolutePath, options);
        int mHeight = options.outHeight;
        int mWidth = options.outWidth;
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) && mWidth > 0 && mHeight > 0) {
            values.put(MediaStore.MediaColumns.WIDTH, mWidth);
            values.put(MediaStore.MediaColumns.HEIGHT, mHeight);
        }
        String imageType = options.outMimeType;
        values.put(MediaStore.MediaColumns.MIME_TYPE, imageType);

        ExifInterface exif = null;
        try {
            exif = new ExifInterface(absolutePath);
        } catch (IOException ex) {
            // exif is null
        }

        if (exif != null) {
            float[] latlng = new float[2];
            if (exif.getLatLong(latlng)) {
                values.put(MediaStore.Images.Media.LATITUDE, latlng[0]);
                values.put(MediaStore.Images.Media.LONGITUDE, latlng[1]);
            }

            long time = getDateTime(exif);
            if (time != -1) {
                values.put(MediaStore.Images.Media.DATE_TAKEN, time);
            }

            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, -1);
            if (orientation != -1) {
                // We only recognize a subset of orientation tag values.
                int degree;
                switch(orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        degree = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        degree = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        degree = 270;
                        break;
                    default:
                        degree = 0;
                        break;
                }
                values.put(MediaStore.Images.Media.ORIENTATION, degree);
            }
        }
    }

    public IGeoPointInfo getPositionFromFile(String absolutePath, String id) {
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(absolutePath);
        } catch (IOException ex) {
            // exif is null
        }

        if (exif != null) {
            float[] latlng = new float[2];
            if (exif.getLatLong(latlng)) {
                return new GeoPointDto(latlng[0], latlng[1], GeoPointDto.NO_ZOOM).setId(id);
            }
        }

        return null;
    }
    public int updatePathRelatedFields(Context context, Cursor cursor, String newAbsolutePath) {
        int columnIndexPk = cursor.getColumnIndex(FotoSql.SQL_COL_PK);
        int columnIndexPath = cursor.getColumnIndex(FotoSql.SQL_COL_PATH);
        return updatePathRelatedFields(context, cursor, newAbsolutePath, columnIndexPk, columnIndexPath);
    }

    public int updatePathRelatedFields(Context context, Cursor cursor, String newAbsolutePath, int columnIndexPk, int columnIndexPath) {
        ContentValues values = new ContentValues();
        DatabaseUtils.cursorRowToContentValues(cursor, values);
        String oldAbsolutePath = cursor.getString(columnIndexPath);
        int id = cursor.getInt(columnIndexPk);
        setPathRelatedFieldsIfNeccessary(values, newAbsolutePath, oldAbsolutePath);
        return FotoSql.execUpdate(context, id, values);
    }

    /** sets the path related fields */
    private void setPathRelatedFieldsIfNeccessary(ContentValues values, String newAbsolutePath, String oldAbsolutePath) {
        setFieldIfNeccessary(values, MediaStore.MediaColumns.TITLE, generateTitleFromFilePath(newAbsolutePath), generateTitleFromFilePath(oldAbsolutePath));
        setFieldIfNeccessary(values, MediaStore.MediaColumns.DISPLAY_NAME, generateDisplayNameFromFilePath(newAbsolutePath), generateDisplayNameFromFilePath(oldAbsolutePath));
        values.put(MediaStore.MediaColumns.DATA, newAbsolutePath);
    }

    /** values[fieldName]=newCalculatedValue if current not set or equals oldCalculatedValue */
    private void setFieldIfNeccessary(ContentValues values, String fieldName, String newCalculatedValue, String oldCalculatedValue) {
        String currentValue = values.getAsString(fieldName);
        if ((currentValue == null) || (TextUtils.isEmpty(currentValue.trim())) || (currentValue.equals(oldCalculatedValue))) {
            values.put(fieldName, newCalculatedValue);
        }
    }

    private int update_Android42(Context context, int id, File file) {
        if ((file != null) && file.exists() && file.canRead()) {
            ContentValues values = new ContentValues();
            getExifFromFile(values, file);
            return FotoSql.execUpdate(context, id, values);
        }
		return 0;
    }

    private int insert_Android42(Context context, File file) {
        if ((file != null) && file.exists() && file.canRead()) {
            ContentValues values = new ContentValues();
            long now = new Date().getTime();
            values.put(MediaStore.Images.ImageColumns.DATE_ADDED, now / 1000);//sec

            getExifFromFile(values, file);
            return (null != FotoSql.execInsert(context, values)) ? 1 : 0;
        }
		return 0;
    }

    @NonNull
    // generates a title based on file name
    protected String generateTitleFromFilePath(String _filePath) {
        String filePath = generateDisplayNameFromFilePath(_filePath);

        if (filePath != null) {
            // truncate the file extension (if any)
            int lastDot = filePath.lastIndexOf('.');
            if (lastDot > 0) {
                filePath = filePath.substring(0, lastDot);
            }
        }
        return filePath;
    }

    @NonNull
    // generates a title based on file name
    public static String generateDisplayNameFromFilePath(String filePath) {
        if (filePath != null) {
            // extract file name after last slash
            int lastSlash = filePath.lastIndexOf('/');
            if (lastSlash >= 0) {
                lastSlash++;
                if (lastSlash < filePath.length()) {
                    return filePath.substring(lastSlash);
                }
            }
        }
        return filePath;
    }

    /**
     * Returns number of milliseconds since Jan. 1, 1970, midnight.
     * Returns -1 if the date time information if not available.
     */
    public static long getDateTime(ExifInterface exif) {
        String dateTimeString =  exif.getAttribute(ExifInterface.TAG_DATETIME);
        if (dateTimeString == null) return -1;

        ParsePosition pos = new ParsePosition(0);
        try {
            Date datetime = sFormatter.parse(dateTimeString, pos);
            if (datetime == null) return -1;
            return datetime.getTime();
        } catch (IllegalArgumentException ex) {
            return -1;
        }
    }

    /** update media db via android-s native scanner.
     * Requires android-4.4 and up to support single files
     */
    public static void updateMediaDB_Androd44(Context context, String[] pathNames) {
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, CONTEXT + "updateMediaDB_Androd44(" + pathNames.length + " files " + pathNames[0] + "...");
        }

        // this only works in android-4.4 and up but not below
        MediaScannerConnection.scanFile(
                // http://stackoverflow.com/questions/5739140/mediascannerconnection-produces-android-app-serviceconnectionleaked
                context.getApplicationContext(),
                pathNames, // mPathNames.toArray(new String[mPathNames.size()]),
                null, null);
    }

    // http://stackoverflow.com/questions/12136681/detect-if-media-scanner-running-on-android
    public static boolean isScannerActive(ContentResolver cr) {
        boolean result = false;
        Cursor cursor = null;

        try {
            cursor = cr.query(MediaStore.getMediaScannerUri(),
                    new String[]{MediaStore.MEDIA_SCANNER_VOLUME},
                    null, null, null);
            if ((cursor != null) && (cursor.getCount() == 1)) {
                cursor.moveToFirst();
                result = "external".equals(cursor.getString(0));
            }
            return result;
        } finally {
            if (cursor != null) cursor.close();
        }
    }


    /** return true if path is "*.jp(e)g" */
    public static boolean isImage(File path, boolean jpgOnly) {
        if (path == null) return false;
        return isImage(path.getName(), jpgOnly);
    }

    /** return true if path is "*.jp(e)g" */
    public static boolean isImage(String path, boolean jpgOnly) {
        if (path == null) return false;
        String lcPath = path.toLowerCase();

        if ((!jpgOnly) && (lcPath.endsWith(".gif") || lcPath.endsWith(".png") || lcPath.endsWith(".tiff") || lcPath.endsWith(".bmp"))) {
            return true;
        }
        return lcPath.endsWith(".jpg") || lcPath.endsWith(".jpeg");
    }

    public static MediaScanner getInstance(Context context) {
        if (sInstance == null) sInstance = new MediaScanner(context);
        return sInstance;
    }

    public static void setInstance(MediaScanner sInstance) {
        MediaScanner.sInstance = sInstance;
    }
}

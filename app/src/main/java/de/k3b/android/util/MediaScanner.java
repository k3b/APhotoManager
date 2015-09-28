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
import java.io.IOException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.queries.QueryParameterParcelable;

/**
 * Since android.media.MediaScannerConnection does not work on my android-4.2
 * here is my own implementation.
 *
 * Created by k3b on 14.09.2015.
 */
public class MediaScanner extends AsyncTask<String[],Object,Integer> {
    private static final String CONTEXT = "MediaScanner";
    private static SimpleDateFormat sFormatter;

    static {
        sFormatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        sFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    protected final Context mContext;

    public MediaScanner(Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    protected Integer doInBackground(String[]... pathNames) {
        if (pathNames.length != 2) throw new IllegalArgumentException("MediaScanner.execute(oldFileNames, newFileNames)");
        return updateMediaDatabase_Android42(mContext, pathNames[0], pathNames[1]);
    }

    @Override
    protected void onPostExecute(Integer resultCount) {
        super.onPostExecute(resultCount);
        String message = this.mContext.getString(R.string.media_update_result, resultCount);
        Toast.makeText(this.mContext, message, Toast.LENGTH_LONG).show();
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, CONTEXT + "A42 scanner finished: " + message);
        }
    }

    /** do not wait for result. Use buildIn scanner if a4.4 else own scanner. */
    public static void updateMediaDBInBackground(Context context, String[] oldPathNames, String[] newPathNames) {
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                && (newPathNames != null) && (newPathNames.length > 0)) {
            // for android-4.4 and up use internal scanner
            MediaScanner.updateMediaDB_Androd44(context, newPathNames);
        } else if (isGuiThread()) {
            // update_Android42 scanner in seperate background task for android-4.3 and below
            MediaScanner scanTask = new MediaScanner(context.getApplicationContext());
            scanTask.execute(oldPathNames, newPathNames);
        } else {
            // Continute in background task  for android-4.3 and below
            MediaScanner.updateMediaDatabase_Android42(context.getApplicationContext(), oldPathNames, newPathNames);
        }
    }

    public static boolean isGuiThread() {
        return (Looper.myLooper() == Looper.getMainLooper());
    }


    public static int updateMediaDatabase_Android42(Context context, String[] oldPathNames, String... newPathNames) {
        if ((newPathNames != null) && (newPathNames.length > 0) && (oldPathNames != null) && (oldPathNames.length > 0)) {
            return renameInMediaDatabase(context, oldPathNames, newPathNames);
        }
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, CONTEXT + "A42 scanner starting with " + newPathNames.length + " files " + newPathNames[0] + "...");
        }

        // ignore non-jpeg
        for(int i = 0; i < newPathNames.length; i++) {
            if (!isJpeg(newPathNames[i])) {
                newPathNames[i] = null;
            }
        }

        Map<String, Integer> inMediaDb = FotoSql.execGetPathIdMap(context.getApplicationContext(), newPathNames);

        int count = 0;
        for (String fileName : newPathNames) {
            if (fileName != null) {
                Integer id = inMediaDb.get(fileName);
                if (id != null) {
                    // already exists
                    update_Android42(context, id, new File(fileName));
                } else {
                    insert_Android42(context, new File(fileName));
                }
                count++;
            }
        }

        return count;
    }

    /** change path and path dependant fields in media database */
    private static int renameInMediaDatabase(Context context, String[] oldPathNames, String... newPathNames) {
        Map<String,String> old2NewFileNames = new HashMap<>(oldPathNames.length);
        for (int i = 0; i < oldPathNames.length; i++) {
            old2NewFileNames.put(oldPathNames[i], newPathNames[i]);
        }

        QueryParameterParcelable query = new QueryParameterParcelable(FotoSql.queryChangePath);
        FotoSql.setWhereFileNames(query, oldPathNames);
        int resultCount = 0;

        Cursor c = null;
        try {
            c = FotoSql.createCursorForQuery(context, query);
            int pkColNo  = c.getColumnIndex(FotoSql.SQL_COL_PK);
            int pathColNo  = c.getColumnIndex(FotoSql.SQL_COL_PATH);
            while (c.moveToNext()) {
                String oldPath = c.getString(pathColNo);
                MediaScanner.updatePathRelatedFields(context, c, old2NewFileNames.get(oldPath), pkColNo, pathColNo);

                resultCount++;
            }
        } catch (Exception ex) {
            Log.e(Global.LOG_CONTEXT, "FotoSql.execChangePaths() error :", ex);
        } finally {
            if (c != null) c.close();
        }

        if (Global.debugEnabled) {
            Log.d(Global.LOG_CONTEXT, "FotoSql.execChangePaths() result count=" + resultCount);
        }
        return resultCount;
    }

    /** updates values with current values of file */
    private static void getExifFromFile(ContentValues values, File file) {
        String absolutePath = file.getAbsolutePath();
        setPathRelatedFieldsIfNeccessary(values, absolutePath, null);

        values.put(MediaStore.MediaColumns.DATE_MODIFIED, file.lastModified() / 1000);
        values.put(MediaStore.MediaColumns.SIZE, file.length());

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; // only need with/height but not content
        BitmapFactory.decodeFile(absolutePath, options);
        int mHeight = options.outHeight;
        int mWidth = options.outWidth;
        if (mWidth > 0 && mHeight > 0) {
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

    public static void updatePathRelatedFields(Context context, Cursor cursor, String newAbsolutePath) {
        int columnIndexPk = cursor.getColumnIndex(FotoSql.SQL_COL_PK);
        int columnIndexPath = cursor.getColumnIndex(FotoSql.SQL_COL_PATH);
        updatePathRelatedFields(context, cursor, newAbsolutePath, columnIndexPk, columnIndexPath);
    }

    public static void updatePathRelatedFields(Context context, Cursor cursor, String newAbsolutePath, int columnIndexPk, int columnIndexPath) {
        ContentValues values = new ContentValues();
        DatabaseUtils.cursorRowToContentValues(cursor, values);
        String oldAbsolutePath = cursor.getString(columnIndexPath);
        int id = cursor.getInt(columnIndexPk);
        setPathRelatedFieldsIfNeccessary(values, newAbsolutePath, oldAbsolutePath);
        FotoSql.execUpdate(context, id, values);
    }

    /** sets the path related fields */
    public static void setPathRelatedFieldsIfNeccessary(ContentValues values, String newAbsolutePath, String oldAbsolutePath) {
        setFieldIfNeccessary(values, MediaStore.MediaColumns.TITLE, generateTitleFromFilePath(newAbsolutePath), generateTitleFromFilePath(oldAbsolutePath));
        setFieldIfNeccessary(values, MediaStore.MediaColumns.DISPLAY_NAME, generateDisplayNameFromFilePath(newAbsolutePath), generateDisplayNameFromFilePath(oldAbsolutePath));
        values.put(MediaStore.MediaColumns.DATA, newAbsolutePath);
    }

    /** values[fieldName]=newCalculatedValue if current not set or equals oldCalculatedValue */
    private static void setFieldIfNeccessary(ContentValues values, String fieldName, String newCalculatedValue, String oldCalculatedValue) {
        String currentValue = values.getAsString(fieldName);
        if ((currentValue == null) || (TextUtils.isEmpty(currentValue.trim())) || (currentValue.equals(oldCalculatedValue))) {
            values.put(fieldName, newCalculatedValue);
        }
    }

    private static void update_Android42(Context context, int id, File file) {
        if ((file != null) && file.exists() && file.canRead()) {
            ContentValues values = new ContentValues();
            getExifFromFile(values, file);
            FotoSql.execUpdate(context, id, values);
        }
    }

    private static void insert_Android42(Context context, File file) {
        if ((file != null) && file.exists() && file.canRead()) {
            ContentValues values = new ContentValues();
            long now = new Date().getTime();
            values.put(MediaStore.Images.ImageColumns.DATE_ADDED, now / 1000);//sec

            getExifFromFile(values, file);
            FotoSql.execInsert(context, values);
        }
    }

    @NonNull
    // generates a title based on file name
    public static String generateTitleFromFilePath(String filePath) {
        filePath = generateDisplayNameFromFilePath(filePath);

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
                    filePath = filePath.substring(lastSlash);
                }
            }
        }
        return filePath;
    }

    /**
     * Returns number of milliseconds since Jan. 1, 1970, midnight.
     * Returns -1 if the date time information if not available.
     * @hide
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
     * @param context
     * @param pathNames
     */
    public static void updateMediaDB_Androd44(Context context, String[] pathNames) {
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, CONTEXT + "A44 scanner starting with " + pathNames.length + " files " + pathNames[0] + "...");
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
            if (cursor != null) {
                if (cursor.getCount() == 1) {
                    cursor.moveToFirst();
                    result = "external".equals(cursor.getString(0));
                }
            }
            return result;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public static boolean isJpeg(String path) {
        if (path == null) return false;
        String lcPath = path.toLowerCase();
        return lcPath.endsWith(".jpg") || lcPath.endsWith(".jpeg");
    }
}

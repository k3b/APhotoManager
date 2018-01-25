/*
 * Copyright (c) 2015-2018 by k3b.
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
 
package de.k3b.android.util;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.k3b.FotoLibGlobal;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.media.MediaContentValues;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.tagDB.TagSql;
import de.k3b.database.QueryParameter;
import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.api.IGeoPointInfo;
import de.k3b.io.FileUtils;
import de.k3b.io.VISIBILITY;
import de.k3b.media.IMetaApi;
import de.k3b.media.MediaUtil;
import de.k3b.media.MediaXmpSegment;
import de.k3b.media.MetaApiChainReader;
import de.k3b.tagDB.TagRepository;

/**
 * Android Media Scanner for images/photos/jpg compatible with android-5.0 Media scanner.
 * This Class handles standard Android-5.0 image fields.
 *
 * Since android.media.MediaScannerConnection does not work on my android-4.2
 * here is my own implementation.
 *
 * Created by k3b on 14.09.2015.
 */
abstract public class MediaScanner  {
    protected static final String CONTEXT = "MediaScanner.";

    /* the DB_XXXX fields are updated by the scanner via ExifInterfaceEx
    protected static final String DB_DATE_TAKEN = MediaStore.Images.Media.DATE_TAKEN;
    protected static final String DB_LONGITUDE = MediaStore.Images.Media.LONGITUDE;
    protected static final String DB_LATITUDE = MediaStore.Images.Media.LATITUDE;
    */

    // the DB_XXXX fields are updated directly by the scanner
    // private fields are updated by base scanner
    private static final String DB_DATE_MODIFIED = MediaStore.MediaColumns.DATE_MODIFIED;
    private static final String DB_SIZE = MediaStore.MediaColumns.SIZE;
    private static final String DB_WIDTH = MediaStore.MediaColumns.WIDTH;
    private static final String DB_HEIGHT = MediaStore.MediaColumns.HEIGHT;
    private static final String DB_MIME_TYPE = MediaStore.MediaColumns.MIME_TYPE;

    protected static final String DB_ORIENTATION = MediaStore.Images.Media.ORIENTATION;
    protected static final String DB_TITLE = MediaStore.MediaColumns.TITLE;
    protected static final String DB_DISPLAY_NAME = MediaStore.MediaColumns.DISPLAY_NAME;
    protected static final String DB_DATA = MediaStore.MediaColumns.DATA;

    public static final int DEFAULT_SCAN_DEPTH = 22;
    public static final String MEDIA_IGNORE_FILENAME = FileUtils.MEDIA_IGNORE_FILENAME; //  MediaStore.MEDIA_IGNORE_FILENAME;

    /** singelton */
    private static MediaScanner sInstance = null;

    public final Context mContext;

    public MediaScanner(Context context) {
        mContext = context.getApplicationContext();
    }


    public static void notifyChanges(Context context, String why) {
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, CONTEXT + "notifyChanges(" + why + ") "
                    + FotoSql.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE);
        }
        context.getContentResolver().notifyChange(FotoSql.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE, null);
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
        return FileUtils.isNoMedia(path,maxLevel);
    }

    public static boolean isNoMedia(String path) {
        return FileUtils.isNoMedia(path,MediaScanner.DEFAULT_SCAN_DEPTH);
    }

    public static boolean canHideFolderMedia(String absoluteSelectedPath) {
        return !isNoMedia(absoluteSelectedPath);
    }

    public static int hideFolderMedia(Activity context, String path) {
        int result = 0;
        if (canHideFolderMedia(path)) {
            File nomedia = new File(path, MEDIA_IGNORE_FILENAME);
            try {
                if (Global.debugEnabled) {
                    Log.i(Global.LOG_CONTEXT, CONTEXT + " hideFolderMedia: creating " + nomedia);
                }

                FileWriter writer = new FileWriter(nomedia, true);
                writer.close();
            } catch (IOException e) {
                Log.e(Global.LOG_CONTEXT, CONTEXT + " cannot create  " + nomedia, e);
            }
            if (nomedia.exists()) {
                if (Global.debugEnabled) {
                    Log.i(Global.LOG_CONTEXT, CONTEXT + " hideFolderMedia: delete from media db " + path + "/**");
                }
                result = FotoSql.execDeleteByPath(CONTEXT + " hideFolderMedia", context, path, VISIBILITY.PRIVATE_PUBLIC);
                if (result > 0) {
                    MediaScanner.notifyChanges(context, "hide " + path + "/**");
                }
            }
        }
        return result;
    }

    public int updateMediaDatabase_Android42(Context context, String[] oldPathNames, String... newPathNames) {
        final boolean hasNew = excludeNomediaFiles(newPathNames) > 0;
        final boolean hasOld = excludeNomediaFiles(oldPathNames) > 0;
        int result = 0;

        if (hasNew && hasOld) {
            result = renameInMediaDatabase(context, oldPathNames, newPathNames);
        } else if (hasOld) {
            result = deleteInMediaDatabase(context, oldPathNames);
        } if (hasNew) {
            result = insertIntoMediaDatabase(context, newPathNames);
        }
        TagSql.fixPrivate(context);
        return result;
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
                    if (!MediaUtil.isImage(fullPathName, MediaUtil.IMG_TYPE_ALL) || isNoMedia(fullPathName, 22)) {
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

            Map<String, Long> inMediaDb = FotoSql.execGetPathIdMap(context.getApplicationContext(), newPathNames);

            for (String fileName : newPathNames) {
                if (fileName != null) {
                    Long id = inMediaDb.get(fileName);
                    if (id != null) {
                        // already exists
                        modifyCount += update_Android42("MediaScanner.insertIntoMediaDatabase already existing "
                                , context, id, new File(fileName));
                    } else {
                        modifyCount += insert_Android42("MediaScanner.insertIntoMediaDatabase new item ", context, new File(fileName));
                    }
                }
            }
        }
        return modifyCount;
    }

    /**  */
    public Long insertOrUpdateMediaDatabase(String dbgContext, Context context,
                                            String dbUpdateFilterJpgFullPathName, File currentJpgFile,
                                            Long updateSuccessValue) {
        if ((currentJpgFile != null) && currentJpgFile.exists() && currentJpgFile.canRead()) {
            ContentValues values = createDefaultContentValues();
            getExifFromFile(values, currentJpgFile);
            Long result = FotoSql.insertOrUpdateMediaDatabase(dbgContext, context, dbUpdateFilterJpgFullPathName, values, updateSuccessValue);

            return result;
        }
        return null;
    }

    /** delete oldPathNames from media database */
    private int deleteInMediaDatabase(Context context, String[] oldPathNames) {
        int modifyCount = 0;

        if ((oldPathNames != null) && (oldPathNames.length > 0)) {
            String sqlWhere = FotoSql.getWhereInFileNames(oldPathNames);
            try {
                modifyCount = FotoSql.deleteMedia(CONTEXT + "deleteInMediaDatabase", context, sqlWhere, null, true);
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
                c = FotoSql.createCursorForQuery("renameInMediaDatabase", context, query, VISIBILITY.PRIVATE_PUBLIC);
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
    public MediaContentValues getExifFromFile(File jpgFile) {
        return getExifFromFile(createDefaultContentValues(), jpgFile);
    }

    /** updates values with current values of file. */
    protected MediaContentValues getExifFromFile(ContentValues values, File jpgFile) {
        String absoluteJpgPath = FileUtils.tryGetCanonicalPath(jpgFile, jpgFile.getAbsolutePath());

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; // only need with/height but not content
        BitmapFactory.decodeFile(absoluteJpgPath, options);
        int mHeight = options.outHeight;
        int mWidth = options.outWidth;
        String imageType = options.outMimeType;

        MediaXmpSegment xmpContent = MediaXmpSegment.loadXmpSidecarContentOrNull(absoluteJpgPath, "getExifFromFile");
        final long xmpFilelastModified = getXmpFilelastModified(xmpContent);

        values.put(DB_DATE_MODIFIED, jpgFile.lastModified() / 1000);
        values.put(DB_SIZE, jpgFile.length());

        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) && mWidth > 0 && mHeight > 0) {
            values.put(DB_WIDTH, mWidth);
            values.put(DB_HEIGHT, mHeight);
        }
        values.put(DB_MIME_TYPE, imageType);

        TagSql.setXmpFileModifyDate(values, xmpFilelastModified);

        IMetaApi exif = loadNonMediaValues(values, absoluteJpgPath, xmpContent);

        IMetaApi src = null;
        if (exif == null) {
            src = xmpContent;
        } else {
            // (!writeExif) prefer read from xmp value before exif value
            src = (FotoLibGlobal.mediaUpdateStrategy.contains("J"))
                    ? exif
                    : new MetaApiChainReader(xmpContent, exif);
        }
        MediaContentValues dest = new MediaContentValues().set(values, null);

        if (src != null) {
            // image has valid exif
            getExifValues(dest, jpgFile, src);

            updateTagRepository(src.getTags());
        }

        setPathRelatedFieldsIfNeccessary(values, absoluteJpgPath, null);

        return dest;
    }

    private void updateTagRepository(List<String> tags) {
        TagRepository tagRepository = TagRepository.getInstance();
        tagRepository.includeTagNamesIfNotFound(tags);
    }

    /** in secs since 1970 */
    protected static long getXmpFilelastModified(MediaXmpSegment xmpContent) {
        long xmpFilelastModified = 0;
        if (xmpContent != null) {
            xmpFilelastModified = xmpContent.getFilelastModified();
        }
        if (xmpFilelastModified == 0) {
            xmpFilelastModified = TagSql.EXT_LAST_EXT_SCAN_NO_XMP;
        }
        return xmpFilelastModified;
    }


    protected IGeoPointInfo getPositionFromMeta(String absoluteJpgPath, String id, IMetaApi exif) {
        if (exif != null) {
            Double latitude = exif.getLatitude();
            if (latitude != null) {
                return new GeoPointDto(latitude, exif.getLongitude(), GeoPointDto.NO_ZOOM).setId(id);
            }
            MediaXmpSegment xmpContent = MediaXmpSegment.loadXmpSidecarContentOrNull(absoluteJpgPath, "getPositionFromFile");
            if (xmpContent != null) {
                latitude = xmpContent.getLatitude();
                if (latitude != null) {
                    return new GeoPointDto(latitude, xmpContent.getLongitude(), GeoPointDto.NO_ZOOM).setId(id);
                }
            }
        }

        return null;
    }

    abstract protected IMetaApi loadNonMediaValues(ContentValues destinationValues, String absoluteJpgPath, IMetaApi xmpContent);

    /** @return number of copied properties */
    protected int getExifValues(MediaContentValues dest, File file, IMetaApi src) {
        return MediaUtil.copyNonEmpty(dest, src);
    }

    abstract public IGeoPointInfo getPositionFromFile(String absolutePath, String id);
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
        return FotoSql.execUpdate("updatePathRelatedFields", context, id, values);
    }

    /** sets the path related fields */
    private void setPathRelatedFieldsIfNeccessary(ContentValues values, String newAbsolutePath, String oldAbsolutePath) {
        setFieldIfNeccessary(values, DB_TITLE, generateTitleFromFilePath(newAbsolutePath), generateTitleFromFilePath(oldAbsolutePath));
        setFieldIfNeccessary(values, DB_DISPLAY_NAME, generateDisplayNameFromFilePath(newAbsolutePath), generateDisplayNameFromFilePath(oldAbsolutePath));
        values.put(DB_DATA, newAbsolutePath);
    }

    /** values[fieldName]=newCalculatedValue if current not set or equals oldCalculatedValue */
    private void setFieldIfNeccessary(ContentValues values, String fieldName, String newCalculatedValue, String oldCalculatedValue) {
        String currentValue = values.getAsString(fieldName);
        if ((currentValue == null) || (TextUtils.isEmpty(currentValue.trim())) || (currentValue.equals(oldCalculatedValue))) {
            values.put(fieldName, newCalculatedValue);
        }
    }

    private int update_Android42(String dbgContext, Context context, long id, File file) {
        if ((file != null) && file.exists() && file.canRead()) {
            ContentValues values = createDefaultContentValues();
            getExifFromFile(values, file);
            return FotoSql.execUpdate(dbgContext, context, id, values);
        }
		return 0;
    }

    protected ContentValues createDefaultContentValues() {
        ContentValues contentValues = new ContentValues();

        // to allow set null becyuse copy does not setNull if already has null (not found)
        contentValues.putNull(DB_TITLE);
        contentValues.putNull(FotoSql.SQL_COL_LON);
        contentValues.putNull(FotoSql.SQL_COL_LAT);
        contentValues.putNull(TagSql.SQL_COL_EXT_DESCRIPTION);
        contentValues.putNull(TagSql.SQL_COL_EXT_TAGS);
        contentValues.putNull(TagSql.SQL_COL_EXT_RATING);
        return contentValues;
    }

    private int insert_Android42(String dbgContext, Context context, File file) {
        if ((file != null) && file.exists() && file.canRead()) {
            ContentValues values = createDefaultContentValues();
            FotoSql.addDateAdded(values);

            getExifFromFile(values, file);
            return (null != FotoSql.execInsert(dbgContext, context, values)) ? 1 : 0;
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


    public static MediaScanner getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new MediaScannerExifInterface(context);
        }
        return sInstance;
    }

    public static void setInstance(MediaScanner sInstance) {
        MediaScanner.sInstance = sInstance;
    }
}

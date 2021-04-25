/*
 * Copyright (c) 2015-2021 by k3b.
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
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.k3b.LibGlobal;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.media.PhotoPropertiesMediaDBContentValues;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.queries.IMediaRepositoryApi;
import de.k3b.database.QueryParameter;
import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.api.IGeoPointInfo;
import de.k3b.io.AlbumFile;
import de.k3b.io.FileUtils;
import de.k3b.io.VISIBILITY;
import de.k3b.io.filefacade.IFile;
import de.k3b.media.IPhotoProperties;
import de.k3b.media.PhotoPropertiesChainReader;
import de.k3b.media.PhotoPropertiesUtil;
import de.k3b.media.PhotoPropertiesXmpSegment;
import de.k3b.tagDB.TagRepository;

import static de.k3b.android.androFotoFinder.tagDB.TagSql.EXT_LAST_EXT_SCAN_NO_XMP;
import static de.k3b.android.androFotoFinder.tagDB.TagSql.SQL_COL_EXT_DESCRIPTION;
import static de.k3b.android.androFotoFinder.tagDB.TagSql.SQL_COL_EXT_RATING;
import static de.k3b.android.androFotoFinder.tagDB.TagSql.SQL_COL_EXT_TAGS;
import static de.k3b.android.androFotoFinder.tagDB.TagSql.SQL_COL_EXT_TITLE;
import static de.k3b.android.androFotoFinder.tagDB.TagSql.SQL_COL_LAST_MODIFIED;
import static de.k3b.android.androFotoFinder.tagDB.TagSql.SQL_COL_LAT;
import static de.k3b.android.androFotoFinder.tagDB.TagSql.SQL_COL_LON;
import static de.k3b.android.androFotoFinder.tagDB.TagSql.SQL_COL_PATH;
import static de.k3b.android.androFotoFinder.tagDB.TagSql.SQL_COL_PK;
import static de.k3b.android.androFotoFinder.tagDB.TagSql.SQL_COL_SIZE;
import static de.k3b.android.androFotoFinder.tagDB.TagSql.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE;
import static de.k3b.android.androFotoFinder.tagDB.TagSql.addDateAdded;
import static de.k3b.android.androFotoFinder.tagDB.TagSql.execDeleteByPath;
import static de.k3b.android.androFotoFinder.tagDB.TagSql.execGetPathIdMap;
import static de.k3b.android.androFotoFinder.tagDB.TagSql.fixAllPrivate;
import static de.k3b.android.androFotoFinder.tagDB.TagSql.getMediaDBApi;
import static de.k3b.android.androFotoFinder.tagDB.TagSql.getWhereInFileNames;
import static de.k3b.android.androFotoFinder.tagDB.TagSql.queryChangePath;
import static de.k3b.android.androFotoFinder.tagDB.TagSql.setVisibility;
import static de.k3b.android.androFotoFinder.tagDB.TagSql.setWhereFileNames;
import static de.k3b.android.androFotoFinder.tagDB.TagSql.setXmpFileModifyDate;

/**
 * Android Media Scanner for images/photos/jpg compatible with android-5.0 Media scanner.
 * This Class handles standard Android-5.0 image fields.
 * <p>
 * Since android.media.MediaScannerConnection does not work on my android-4.2
 * here is my own implementation.
 * <p>
 * Created by k3b on 14.09.2015.
 */
public abstract class PhotoPropertiesMediaFilesScanner {
    protected static final String CONTEXT = "PhotoPropertiesMediaFilesScanner.";

    /* the DB_XXXX fields are updated by the scanner via ExifInterfaceEx
    protected static final String DB_DATE_TAKEN = MediaStore.Images.Media.DATE_TAKEN;
    protected static final String DB_LONGITUDE = MediaStore.Images.Media.LONGITUDE;
    protected static final String DB_LATITUDE = MediaStore.Images.Media.LATITUDE;
    */

    protected static final String DB_TITLE = SQL_COL_EXT_TITLE;
    private static final String DB_WIDTH = MediaStore.MediaColumns.WIDTH;
    private static final String DB_HEIGHT = MediaStore.MediaColumns.HEIGHT;
    private static final String DB_MIME_TYPE = MediaStore.MediaColumns.MIME_TYPE;

    protected static final String DB_ORIENTATION = MediaStore.Images.Media.ORIENTATION;
    protected static final String DB_DATA = SQL_COL_PATH; // _data
    protected static final String DB_DISPLAY_NAME = MediaStore.MediaColumns.DISPLAY_NAME;
    private static final String DB_SIZE = SQL_COL_SIZE;

    public static final int DEFAULT_SCAN_DEPTH = 22;
    public static final String MEDIA_IGNORE_FILENAME = FileUtils.MEDIA_IGNORE_FILENAME;

    /**
     * singelton
     */
    private static PhotoPropertiesMediaFilesScanner sInstance = null;

    public final Context mContext;

    private final Map<String, Boolean> noMediaCache = new HashMap<>();

    private boolean ignoreNoMediaCheck = false;

    public PhotoPropertiesMediaFilesScanner(Context context) {
        mContext = context.getApplicationContext();
    }


    public static void notifyChanges(Context context, String why) {
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, CONTEXT + "notifyChanges(" + why + ") "
                    + SQL_TABLE_EXTERNAL_CONTENT_URI_FILE);
        }
        context.getContentResolver().notifyChange(SQL_TABLE_EXTERNAL_CONTENT_URI_FILE, null);
    }

    public static boolean isNoMedia(int maxLevel, IFile[] pathNames) {
        if (pathNames != null) {
            for (IFile path : pathNames) {
                if (isNoMedia(path, maxLevel)) {
                    return true;
                }
            }

        }
        return false;
    }

    public static boolean isNoMedia(IFile path, int maxLevel) {
        return isNoMedia(path, maxLevel, null);
    }

    /**
     * return true, if file is in a ".nomedia" dir
     */
    public static boolean isNoMedia(IFile path, int maxLevel, Map<String, Boolean> noMediaCache) {
        return FileUtils.isNoMedia(path, maxLevel, noMediaCache);
    }

    /**
     * return true, if file is in a ".nomedia" dir
     */
    @Deprecated
    public static boolean isNoMedia(String path) {
        return FileUtils.isNoMedia(path, PhotoPropertiesMediaFilesScanner.DEFAULT_SCAN_DEPTH);
    }

    @Deprecated
    public static boolean canHideFolderMedia(String absoluteSelectedPath) {
        return !isNoMedia(absoluteSelectedPath);
    }

    @Deprecated
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
                result = execDeleteByPath(CONTEXT + " hideFolderMedia", path, VISIBILITY.PRIVATE_PUBLIC);
                if (result > 0) {
                    PhotoPropertiesMediaFilesScanner.notifyChanges(context, "hide " + path + "/**");
                }
            }
        }
        return result;
    }

    /**
     * in secs since 1970
     */
    protected static long getXmpFilelastModified(PhotoPropertiesXmpSegment xmpContent) {
        long xmpFilelastModified = 0;
        if (xmpContent != null) {
            xmpFilelastModified = xmpContent.getFilelastModified();
        }
        if (xmpFilelastModified == 0) {
            xmpFilelastModified = EXT_LAST_EXT_SCAN_NO_XMP;
        }
        return xmpFilelastModified;
    }

    /**
     * sets the path related fields
     */
    public static String setFileFields(ContentValues values, File file) {
        String newAbsolutePath = FileUtils.tryGetCanonicalPath(file, file.getAbsolutePath());
        setPathRelatedFieldsIfNeccessary(values, newAbsolutePath, null);
        values.put(SQL_COL_LAST_MODIFIED, file.lastModified() / 1000);
        values.put(DB_SIZE, file.length());
        return newAbsolutePath;
    }

    public int updateMediaDatabaseAndroid42(Context context, boolean withTransaction, IFile[] oldPathNames, IFile... newPathNames) {
        if (withTransaction) {
            try {
                getMediaDBApi().beginTransaction();
                int result = updateMediaDatabaseAndroid42Impl(context, oldPathNames, newPathNames);
                fixAllPrivate();
                getMediaDBApi().setTransactionSuccessful();
                return result;
            } finally {
                getMediaDBApi().endTransaction();
            }
        } else {
            return updateMediaDatabaseAndroid42Impl(context, oldPathNames, newPathNames);
        }
    }

    private int updateMediaDatabaseAndroid42Impl(Context context, IFile[] oldPathNames, IFile[] newPathNames) {
        final boolean hasNew = excludeNomediaFiles(newPathNames) > 0;
        final boolean hasOld = excludeNomediaFiles(oldPathNames) > 0;
        int result = 0;

        if (hasNew && hasOld) {
            result = renameInMediaDatabase(context, oldPathNames, newPathNames);
        } else if (hasOld) {
            result = deleteInMediaDatabase(oldPathNames);
        }
        if (hasNew) {
            result = insertIntoMediaDatabase(newPathNames);
        }
        return result;
    }

    /**
     * Replace all files that either non-jpg or in ".nomedia" folder with null so they will not be
     * processed by media scanner
     *
     * @return number of items left.
     */
    private int excludeNomediaFiles(IFile[] fullPathNames) {
        int itemsLeft = 0;
        if (fullPathNames != null) {
            // ignore non-jpeg
            for (int i = 0; i < fullPathNames.length; i++) {
                IFile fullPathName = fullPathNames[i];
                if (fullPathName != null) {
                    if (!PhotoPropertiesUtil.isImage(fullPathName, PhotoPropertiesUtil.IMG_TYPE_ALL | PhotoPropertiesUtil.IMG_TYPE_ALBUM)
                            || excludeIsNoMedia(fullPathName)) {
                        fullPathNames[i] = null;
                    } else {
                        itemsLeft++;
                    }
                }
            }
        }

        return itemsLeft;
    }

    public boolean setIgnoreNoMediaCheck(boolean ignoreNoMediaCheck) {
        boolean oldValue = this.ignoreNoMediaCheck;
        this.ignoreNoMediaCheck = ignoreNoMediaCheck;
        return oldValue;
    }

    protected boolean excludeIsNoMedia(IFile fullPathName) {
        if (ignoreNoMediaCheck) return false;
        return isNoMedia(fullPathName, 5, this.noMediaCache);
    }

    private int insertIntoMediaDatabase(IFile[] newPathNames) {
        int modifyCount = 0;

        if ((newPathNames != null) && (newPathNames.length > 0)) {
            if (Global.debugEnabled) {
                Log.i(Global.LOG_CONTEXT, CONTEXT + "A42 scanner starting with " + newPathNames.length + " files " + newPathNames[0] + "...");
            }

            Map<String, Long> inMediaDb = execGetPathIdMap(newPathNames);

            IMediaRepositoryApi mediaDBApi = getMediaDBApi();
            for (IFile fileName : newPathNames) {
                if (fileName != null) {
                    Long id = inMediaDb.get(fileName.getAbsolutePath());
                    if (id != null) {
                        // already exists
                        modifyCount += updateAndroid42(
                                mediaDBApi,
                                "PhotoPropertiesMediaFilesScanner.insertIntoMediaDatabase already existing ",
                                id, fileName);
                    } else {
                        modifyCount += insertAndroid42(
                                "PhotoPropertiesMediaFilesScanner.insertIntoMediaDatabase new item ",
                                fileName);
                    }
                }
            }
        }
        return modifyCount;
    }

    /**
     * delete oldPathNames from media database
     */
    private int deleteInMediaDatabase(IFile[] oldPathNames) {
        int modifyCount = 0;

        if ((oldPathNames != null) && (oldPathNames.length > 0)) {
            String sqlWhere = getWhereInFileNames(oldPathNames);
            try {
                modifyCount = getMediaDBApi().deleteMedia(CONTEXT + "deleteInMediaDatabase", sqlWhere, null, true);
                if (Global.debugEnabled) {
                    Log.d(Global.LOG_CONTEXT, CONTEXT + "deleteInMediaDatabase(len=" + oldPathNames.length + ", files='" + oldPathNames[0] + "'...) result count=" + modifyCount);
                }
            } catch (Exception ex) {
                Log.e(Global.LOG_CONTEXT, CONTEXT + "deleteInMediaDatabase(" + sqlWhere + ") error :", ex);
            }
        }

        return modifyCount;
    }

    /**
     * updates values with current values of file
     */
    public PhotoPropertiesMediaDBContentValues getExifFromFile(IFile jpgFile) {
        return getExifFromFile(createDefaultContentValues(), jpgFile);
    }

    /**
     * change path and path dependant fields in media database
     */
    private int renameInMediaDatabase(Context context, IFile[] oldPathNames, IFile... newPathNames) {
        if ((oldPathNames != null) && (oldPathNames.length > 0)) {
            if (Global.debugEnabled) {
                Log.i(Global.LOG_CONTEXT, CONTEXT + "renameInMediaDatabase to " + newPathNames.length + " files " + newPathNames[0] + "...");
            }
            Map<String, String> old2NewFileNames = new HashMap<>(oldPathNames.length);
            ArrayList<IFile> deleteFileNames = new ArrayList<>();
            ArrayList<IFile> insertFileNames = new ArrayList<>();

            for (int i = 0; i < oldPathNames.length; i++) {
                String oldPathName = oldPathNames[i].getAbsolutePath();
                String newPathName = newPathNames[i].getAbsolutePath();

                if ((oldPathName != null) && (newPathName != null)) {
                    //!!! ?seiteneffekt update other fields?
                    if (oldPathName.compareToIgnoreCase(newPathName) != 0) {
                        old2NewFileNames.put(oldPathName, newPathName);
                    }
                } else if (oldPathName != null) {
                    deleteFileNames.add(oldPathNames[i]);
                } else if (newPathName != null) {
                    insertFileNames.add(newPathNames[i]);
                }
            }

            int modifyCount =
                    deleteInMediaDatabase(deleteFileNames.toArray(new IFile[deleteFileNames.size()]))
                            + renameInMediaDatabase(context, old2NewFileNames)
                            + insertIntoMediaDatabase(insertFileNames.toArray(new IFile[insertFileNames.size()]));
            return modifyCount;
        }
        return 0;
    }

    private void updateTagRepository(List<String> tags) {
        TagRepository tagRepository = TagRepository.getInstance();
        tagRepository.includeTagNamesIfNotFound(tags);
    }

    private int renameInMediaDatabase(Context context, Map<String, String> old2NewFileNames) {
        int modifyCount = 0;
        if (old2NewFileNames.size() > 0) {
            QueryParameter query = new QueryParameter(queryChangePath);
            setWhereFileNames(query, old2NewFileNames.keySet().toArray(new String[old2NewFileNames.size()]));

            Cursor c = null;
            try {
                c = getMediaDBApi().createCursorForQuery(null, "renameInMediaDatabase", query, VISIBILITY.PRIVATE_PUBLIC, null);
                int pkColNo = c.getColumnIndex(SQL_COL_PK);
                int pathColNo = c.getColumnIndex(SQL_COL_PATH);
                while (c.moveToNext()) {
                    String oldPath = c.getString(pathColNo);
                    modifyCount += updatePathRelatedFields(c, old2NewFileNames.get(oldPath), pkColNo, pathColNo);
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


    protected IGeoPointInfo getPositionFromMeta(String absoluteJpgPath, String id, IPhotoProperties exif) {
        if (exif != null) {
            Double latitude = exif.getLatitude();
            if (latitude != null) {
                return new GeoPointDto(latitude, exif.getLongitude(), GeoPointDto.NO_ZOOM).setId(id);
            }
            PhotoPropertiesXmpSegment xmpContent = PhotoPropertiesXmpSegment.loadXmpSidecarContentOrNull(absoluteJpgPath, "getPositionFromFile");
            if (xmpContent != null) {
                latitude = xmpContent.getLatitude();
                if (latitude != null) {
                    return new GeoPointDto(latitude, xmpContent.getLongitude(), GeoPointDto.NO_ZOOM).setId(id);
                }
            }
        }

        return null;
    }

    protected abstract IPhotoProperties loadNonMediaValues(ContentValues destinationValues, IFile jpgFile, IPhotoProperties xmpContent);

    /**
     * @return number of copied properties
     */
    protected int getExifValues(PhotoPropertiesMediaDBContentValues dest, IPhotoProperties src) {
        return PhotoPropertiesUtil.copyNonEmpty(dest, src);
    }

    public abstract IGeoPointInfo getPositionFromFile(String absolutePath, String id);

    /**
     * updates values with current values of file.
     */
    protected PhotoPropertiesMediaDBContentValues getExifFromFile(ContentValues values, IFile jpgFile) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true; // only need with/height but not content
            BitmapFactory.decodeStream(jpgFile.openInputStream(), null, options);
            int mHeight = options.outHeight;
            int mWidth = options.outWidth;
            String imageType = options.outMimeType;

            PhotoPropertiesXmpSegment xmpContent = PhotoPropertiesXmpSegment.loadXmpSidecarContentOrNull(jpgFile, "getExifFromFile");
            final long xmpFilelastModified = getXmpFilelastModified(xmpContent);

            loadFileValues(values, jpgFile);

            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) && mWidth > 0 && mHeight > 0) {
                values.put(DB_WIDTH, mWidth);
                values.put(DB_HEIGHT, mHeight);
            }
            values.put(DB_MIME_TYPE, imageType);

            setXmpFileModifyDate(values, xmpFilelastModified);

            IPhotoProperties exif = loadNonMediaValues(values, jpgFile, xmpContent);

            IPhotoProperties src = null;
            if (exif == null) {
                src = xmpContent;
            } else {
                // (!writeExif) prefer read from xmp value before exif value
                src = (LibGlobal.mediaUpdateStrategy.contains("J"))
                        ? exif
                        : new PhotoPropertiesChainReader(xmpContent, exif);
            }
            PhotoPropertiesMediaDBContentValues dest = new PhotoPropertiesMediaDBContentValues().set(values, null);

            if (src != null) {
                // image has valid exif
                getExifValues(dest, src);

                updateTagRepository(src.getTags());

                VISIBILITY visibility = src.getVisibility();
                if (visibility == null) {
                    visibility = VISIBILITY.getVisibility(src);
                }
                setVisibility(values, visibility);
            }

            String absoluteJpgPath = jpgFile.getCanonicalPath();
            setPathRelatedFieldsIfNeccessary(values, absoluteJpgPath, null);

            return dest;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void loadFileValues(ContentValues values, IFile jpgFile) {
        values.put(SQL_COL_LAST_MODIFIED, jpgFile.lastModified() / 1000);
        values.put(DB_SIZE, jpgFile.length());
    }

    public int updatePathRelatedFields(Context context, Cursor cursor, String newAbsolutePath) {
        int columnIndexPk = cursor.getColumnIndex(SQL_COL_PK);
        int columnIndexPath = cursor.getColumnIndex(SQL_COL_PATH);
        return updatePathRelatedFields(cursor, newAbsolutePath, columnIndexPk, columnIndexPath);
    }

    /**
     * sets the path related fields
     */
    private static void setPathRelatedFieldsIfNeccessary(ContentValues values, String newAbsolutePath, String oldAbsolutePath) {
        setFieldIfNeccessary(values, DB_TITLE, generateTitleFromFilePath(newAbsolutePath), generateTitleFromFilePath(oldAbsolutePath));
        setFieldIfNeccessary(values, DB_DISPLAY_NAME, generateDisplayNameFromFilePath(newAbsolutePath), generateDisplayNameFromFilePath(oldAbsolutePath));
        values.put(DB_DATA, newAbsolutePath);
    }

    public int updatePathRelatedFields(Cursor cursor, String newAbsolutePath, int columnIndexPk, int columnIndexPath) {
        ContentValues values = new ContentValues();
        DatabaseUtils.cursorRowToContentValues(cursor, values);
        String oldAbsolutePath = cursor.getString(columnIndexPath);
        int id = cursor.getInt(columnIndexPk);
        setPathRelatedFieldsIfNeccessary(values, newAbsolutePath, oldAbsolutePath);
        return getMediaDBApi().execUpdate("updatePathRelatedFields", id, values);
    }


    /** values[fieldName]=newCalculatedValue if current not set or equals oldCalculatedValue */
    private static void setFieldIfNeccessary(ContentValues values, String fieldName, String newCalculatedValue, String oldCalculatedValue) {
        String currentValue = values.getAsString(fieldName);
        if ((currentValue == null) || (TextUtils.isEmpty(currentValue.trim())) || (currentValue.equals(oldCalculatedValue))) {
            values.put(fieldName, newCalculatedValue);
        }
    }

    protected ContentValues createDefaultContentValues() {
        ContentValues contentValues = new ContentValues();

        // to allow set null because copy does not setNull if already has null (not found)
        contentValues.putNull(DB_TITLE);
        contentValues.putNull(SQL_COL_LON);
        contentValues.putNull(SQL_COL_LAT);
        contentValues.putNull(SQL_COL_EXT_DESCRIPTION);
        contentValues.putNull(SQL_COL_EXT_TAGS);
        contentValues.putNull(SQL_COL_EXT_RATING);
        return contentValues;
    }

    private int insertAndroid42(String dbgContext, IFile file) {
        if ((file != null) && file.exists() && file.canRead()) {
            ContentValues values = createDefaultContentValues();
            addDateAdded(values);

            String canonicalPath = file.getCanonicalPath();
            if (AlbumFile.isQueryFile(canonicalPath)) {
                loadFileValues(values, file);
                setPathRelatedFieldsIfNeccessary(values, canonicalPath, "");
                values.put(FotoSql.SQL_COL_EXT_MEDIA_TYPE, FotoSql.MEDIA_TYPE_ALBUM_FILE);
                return (null != getMediaDBApi().insertOrUpdateMediaDatabase(dbgContext, canonicalPath, values, null, 1L)) ? 1 : 0;
            } else {
                IPhotoProperties exif = getExifFromFile(values, file);
                return (null != getMediaDBApi().insertOrUpdateMediaDatabase(dbgContext, canonicalPath, values, exif.getVisibility(), 1L)) ? 1 : 0;
            }
        }
        return 0;
    }

    protected int updateAndroid42(IMediaRepositoryApi mediaDBApi, String dbgContext, long id, IFile file) {
        if ((file != null) && !AlbumFile.isQueryFile(file.getName()) && file.exists() && file.canRead()) {
            ContentValues values = createDefaultContentValues();
            getExifFromFile(values, file);
            return mediaDBApi.execUpdate(dbgContext, id, values);
        }
        return 0;
    }

    // generates a title based on file name
    protected static String generateTitleFromFilePath(String filePath) {
        String currentFilePath = generateDisplayNameFromFilePath(filePath);

        if (currentFilePath != null) {
            // truncate the file extension (if any)
            int lastDot = currentFilePath.lastIndexOf('.');
            if (lastDot > 0) {
                currentFilePath = currentFilePath.substring(0, lastDot);
            }
        }
        return currentFilePath;
    }

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


    public static PhotoPropertiesMediaFilesScanner getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new PhotoPropertiesMediaFilesScannerExifInterface(context);
        }
        return sInstance;
    }

    public static void setInstance(PhotoPropertiesMediaFilesScanner sInstance) {
        PhotoPropertiesMediaFilesScanner.sInstance = sInstance;
    }
}

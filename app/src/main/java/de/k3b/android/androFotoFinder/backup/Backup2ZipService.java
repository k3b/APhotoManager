/*
 * Copyright (c) 2019 by k3b.
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

package de.k3b.android.androFotoFinder.backup;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.InputStream;
import java.util.Date;

import de.k3b.android.androFotoFinder.media.PhotoPropertiesMediaDBCursor;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.tagDB.TagSql;
import de.k3b.database.QueryParameter;
import de.k3b.io.FileUtils;
import de.k3b.io.IItemSaver;
import de.k3b.media.IPhotoProperties;
import de.k3b.media.PhotoProperties2ExistingFileSaver;
import de.k3b.zip.CompressJob;
import de.k3b.zip.IZipConfig;
import de.k3b.zip.LibZipGlobal;
import de.k3b.zip.ZipConfigRepository;
import de.k3b.zip.ZipLog;
import de.k3b.zip.ZipLogImpl;
import de.k3b.zip.ZipStorage;

import de.k3b.media.PhotoPropertiesCsvStringSaver;

/**
 * #108: Zip-file support: backup-or-copy filtered-or-selected photos to Zip-file.
 * Gui independant service to load/save/execute the backup and it-s parameters
 */
public class Backup2ZipService {
    private static String mDebugPrefix = "Backup2ZipService: ";

    public static IZipConfig loadZipConfig(Uri uri, Context context) {
        if ((uri != null) && ZipConfigRepository.isZipConfig(uri.toString())) {
            InputStream inputsteam = null;
            try {
                inputsteam = context.getContentResolver().openInputStream(uri);
                return new ZipConfigRepository(null).load(inputsteam, uri);
            } catch (Exception ex) {
                // file not found or no permission
                Log.w(LibZipGlobal.LOG_TAG, mDebugPrefix + context.getClass().getSimpleName()
                            + "-loadZipConfig(" + uri + ") failed " + ex.getClass().getSimpleName(), ex);
            } finally {
                FileUtils.close(inputsteam, uri);
            }
        }
        return null;
    }

    /** Executes add2zip for all found items of found query-result-item of zipConfig */
    public static IZipConfig execute(Context context, IZipConfig zipConfig, ZipStorage zipStorage, ContentResolver contentResolver) {
        ZipConfigRepository repo = new ZipConfigRepository(zipConfig);
        final File zipConfigFile = repo.getZipConfigFile();
        if (zipConfigFile != null) {
            QueryParameter filter = getEffectiveQueryParameter(zipConfig);

            // pipline for (IPhotoProperties item: query(filter)) : csv+=toCsv(item)
            final PhotoPropertiesCsvStringSaver csvFromQuery = new PhotoPropertiesCsvStringSaver();

            ZipLog zipLog = (LibZipGlobal.debugEnabled) ? new ZipLogImpl(true) : null;

            final CompressJob job = new ApmZipCompressJob(context, zipLog,"history.log");
            job.setDestZipFile(zipStorage);

            // pipline for (IPhotoProperties item: query(filter)) : Zip+=File(item)
            /// !!!  todo go on here
            final IItemSaver<File> file2ZipSaver = new IItemSaver<File>() {
                @Override
                public boolean save(File item) {
                    job.addToCompressQue("", item);
                    return true;
                }
            };

            final PhotoProperties2ExistingFileSaver media2fileZipSaver = new PhotoProperties2ExistingFileSaver(file2ZipSaver);

            execQuery(filter, contentResolver, csvFromQuery, media2fileZipSaver);

            job.addTextToCompressQue("changes.csv", csvFromQuery.toString());

            job.compress(false);

            if (repo.save()) {
                if (LibZipGlobal.debugEnabled) {
                    Log.d(LibZipGlobal.LOG_TAG, mDebugPrefix + " Saved as " + repo);
                }
                return repo;
            }
        }
        return null;
    }

    /**
     * @return get query without filte-DateModified-min/max and with added zipConfig.getDateModifiedFrom
     */
    private static QueryParameter getEffectiveQueryParameter(IZipConfig zipConfig) {
        QueryParameter filter = QueryParameter.parse(zipConfig.getFilter());
        FotoSql.parseDateModifiedMax(filter, true);
        FotoSql.parseDateModifiedMin(filter, true);
        final Date dateModifiedFrom = zipConfig.getDateModifiedFrom();
        if (dateModifiedFrom != null) {
            FotoSql.addWhereDateModifiedMinMax(filter, dateModifiedFrom.getTime(), 0);
        }

        filter.clearColumns().addColumn(TagSql.SQL_COL_PK
                ,TagSql.SQL_COL_PATH
                ,TagSql.SQL_COL_DATE_TAKEN
                ,TagSql.SQL_COL_EXT_TITLE
                ,TagSql.SQL_COL_EXT_DESCRIPTION
                ,TagSql.SQL_COL_EXT_TAGS
                ,TagSql.SQL_COL_LAT
                ,TagSql.SQL_COL_LON
                ,TagSql.SQL_COL_EXT_RATING
                ,TagSql.SQL_COL_EXT_MEDIA_TYPE);

        return filter;
    }

    /** calls consumers for each found query-result-item */
    private static void execQuery(QueryParameter query, ContentResolver contentResolver,
                                  IItemSaver<IPhotoProperties>... consumers) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(Uri.parse(query.toFrom()), query.toColumns(),
                    query.toAndroidWhere(), query.toAndroidParameters(), query.toOrderBy());

            int itemCount = cursor.getCount();
            final int expectedCount = itemCount + itemCount;

            PhotoPropertiesMediaDBCursor mediaItem = new PhotoPropertiesMediaDBCursor(cursor);
            while (cursor.moveToNext()) {
                for (IItemSaver<IPhotoProperties> consumer :  consumers){
                    if (consumer != null) consumer.save(mediaItem);
                }
            }
        } catch (Exception ex){
            Log.e(LibZipGlobal.LOG_TAG, mDebugPrefix + query, ex);
            throw new RuntimeException(mDebugPrefix + query + "\n" + ex.getMessage(), ex);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }


    }
}

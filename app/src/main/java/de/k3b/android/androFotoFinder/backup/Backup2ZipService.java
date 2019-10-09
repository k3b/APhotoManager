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
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.text.MessageFormat;
import java.util.Date;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.media.PhotoPropertiesMediaDBCursor;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.tagDB.TagSql;
import de.k3b.database.QueryParameter;
import de.k3b.io.IItemSaver;
import de.k3b.io.IProgessListener;
import de.k3b.io.StringUtils;
import de.k3b.io.VISIBILITY;
import de.k3b.media.IPhotoProperties;
import de.k3b.media.PhotoProperties2ExistingFileSaver;
import de.k3b.media.PhotoPropertiesCsvStringSaver;
import de.k3b.zip.CompressItem;
import de.k3b.zip.CompressJob;
import de.k3b.zip.FileCompressItem;
import de.k3b.zip.IZipConfig;
import de.k3b.zip.LibZipGlobal;
import de.k3b.zip.ZipConfigRepository;
import de.k3b.zip.ZipLog;
import de.k3b.zip.ZipLogImpl;
import de.k3b.zip.ZipStorage;

/**
 * #108: Zip-file support: backup-or-copy filtered-or-selected photos to Zip-file.
 * Gui independant service to load/save/execute the backup and it-s parameters
 */
public class Backup2ZipService implements IProgessListener, ZipLog {
    private static String mDebugPrefix = "Backup2ZipService: ";
    private final Context context;
    private final IZipConfig zipConfig;
    private final ZipStorage zipStorage;
    private final Date backupDate;
    private IProgessListener progessListener = null;
    private final ZipLog zipLog;
    private CompressJob job = null;
    // set to false (in gui thread) if operation is canceled
    protected boolean continueProcessing = true;

    // used to translate ZipLog.traceMessage() to become IProgessListener
    private int lastZipItemNumber = 0;

    public Backup2ZipService(Context context, IZipConfig zipConfig, ZipStorage zipStorage,
                             Date backupDate) {

        this.context = context;
        this.zipConfig = zipConfig;
        this.zipStorage = zipStorage;

        //!!! after success add this date into config before save
        this.backupDate = backupDate;
        this.zipLog = (LibZipGlobal.debugEnabled) ? new ZipLogImpl(true) : null;
    }

    public Backup2ZipService setProgessListener(IProgessListener progessListener) {
        this.progessListener = progessListener;
        return this;
    }

    /**
     * Executes add2zip for all found items of found query-result-item of zipConfig.
     *
     * @return config used oder null if there is an error.
     */
    public IZipConfig execute() {
        ZipConfigRepository repo = new ZipConfigRepository(zipConfig);
        final File zipConfigFile = repo.getZipConfigFile();
        if (zipConfigFile != null) {
            QueryParameter filter = getEffectiveQueryParameter(zipConfig);

            String zipRelPath = zipConfig.getZipRelPath();

            // pipline for (IPhotoProperties item: query(filter)) : csv+=toCsv(item)
            final PhotoPropertiesCsvStringSaver csvFromQuery = new PhotoPropertiesCsvStringSaver();

            if (!StringUtils.isNullOrEmpty(zipRelPath)) {
                csvFromQuery.setCompressFilePathMode(zipRelPath);
            }

            onProgress(0, 0,
                    "query images " + ((Global.debugEnabledSql) ? filter : ""));

            if (this.continueProcessing) {
                job = new ApmZipCompressJob(context, this, "history.log");
                job.setZipStorage(zipStorage);
                if (!StringUtils.isNullOrEmpty(zipRelPath)) {
                    FileCompressItem.setZipRelPath(new File(zipRelPath));
                }

                // pipline for (IPhotoProperties item: query(filter)) : Zip+=File(item)
                final IItemSaver<File> file2ZipSaver = new IItemSaver<File>() {
                    @Override
                    public boolean save(File item) {
                        CompressItem compressItem = job.addToCompressQue("", item);
                        /*
                        if (PhotoPropertiesUtil.isImage(item.getName(), PhotoPropertiesUtil.IMG_TYPE_COMPRESSED)) {
                            // performance improvement: jpg-s should not be compressed
                            compressItem.setDoCompress(false);
                        }
                        */
                        return true;
                    }
                };

                final PhotoProperties2ExistingFileSaver media2fileZipSaver = new PhotoProperties2ExistingFileSaver(file2ZipSaver);

                execQuery(filter, csvFromQuery, media2fileZipSaver);

                job.addTextToCompressQue("changes.csv", csvFromQuery.toString());

                if (this.continueProcessing) {
                    // not canceled yet in gui thread
                    if (job.compress(false) < 0) {
                        this.continueProcessing = false;
                    }
                }
                this.job = null;

                if (this.continueProcessing) {
                    repo.setDateModifiedFrom(this.backupDate);
                    if (repo.save()) {
                        if (LibZipGlobal.debugEnabled) {
                            Log.d(LibZipGlobal.LOG_TAG, mDebugPrefix + " Saved as " + repo);
                        }
                        return repo;
                    }
                }
            }
        }
        return null;
    }

    /**
     * @return get query without filte-DateModified-min/max and with added zipConfig.getDateModifiedFrom
     */
    @NonNull
    public static QueryParameter getEffectiveQueryParameter(@NonNull IZipConfig zipConfig) {
        QueryParameter filter = QueryParameter.parse(zipConfig.getFilter());
        if (filter == null) {
            filter = new QueryParameter();
            FotoSql.setWhereVisibility(filter, VISIBILITY.DEFAULT);
        }

        // remove lastModified from filter
        FotoSql.parseDateModifiedMax(filter, true);
        FotoSql.parseDateModifiedMin(filter, true);
        filter.clearColumns().addColumn(TagSql.SQL_COL_PK
                , TagSql.SQL_COL_PATH
                , TagSql.SQL_COL_DATE_TAKEN
                , TagSql.SQL_COL_EXT_TITLE
                , TagSql.SQL_COL_EXT_DESCRIPTION
                , TagSql.SQL_COL_EXT_TAGS
                , TagSql.SQL_COL_LAT
                , TagSql.SQL_COL_LON
                , TagSql.SQL_COL_EXT_RATING
                , TagSql.SQL_COL_EXT_MEDIA_TYPE);
        final Date dateModifiedFrom = zipConfig.getDateModifiedFrom();
        if (dateModifiedFrom != null) {
            FotoSql.addWhereDateModifiedMinMax(filter, dateModifiedFrom.getTime(), 0);
        }

        return filter;
    }

    /** calls consumers for each found query-result-item */
    private void execQuery(QueryParameter query,
                                  IItemSaver<IPhotoProperties>... consumers) {
        ContentResolver contentResolver = context.getContentResolver();

        Cursor cursor = null;
        try {
            this.onProgress(0,0, "Calculate");
            cursor = contentResolver.query(Uri.parse(query.toFrom()), query.toColumns(),
                    query.toAndroidWhere(), query.toAndroidParameters(), query.toOrderBy());

            int itemCount = cursor.getCount();

            onProgress(0,itemCount,
                    context.getString(R.string.view_context_menu_title) );

            PhotoPropertiesMediaDBCursor mediaItem = new PhotoPropertiesMediaDBCursor(cursor);

            int progress = 0;
            while (cursor.moveToNext()) {
                for (IItemSaver<IPhotoProperties> consumer :  consumers){
                    if (consumer != null) consumer.save(mediaItem);
                }

                if ((progress % 100) == 0) {
                    if (!onProgress(0, itemCount, context.getString(R.string.selection_status_format, progress)))
                    {
                        // canceled in gui thread
                        return;
                    }
                }
                progress++;

            }
            onProgress(progress, progress, context.getString(R.string.selection_status_format, progress));
        } catch (Exception ex){
            Log.e(LibZipGlobal.LOG_TAG, mDebugPrefix + query, ex);
            throw new RuntimeException(mDebugPrefix + query + "\n" + ex.getMessage(), ex);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Interface IProgessListener: called every time when command
     * makes some little progress. Can be mapped to async progress-bar.
     * return true to continue
     */
    @Override
    public boolean onProgress(int itemcount, int size, String message) {
        if (progessListener != null) {
            if (!progessListener.onProgress(itemcount, size, message)) {
                this.continueProcessing = false;
            }
        }
        return this.continueProcessing;
    }

    /**
     * 10 `interface ZipLog-traceMessage` become one `interface onProgress()` message
     */
    @Override
    public String traceMessage(int state, int itemNumber, int itemTotal, String format, Object... params) {
        if ((itemNumber != 0) && (itemNumber > this.lastZipItemNumber)) {
            lastZipItemNumber = itemNumber;

            if ((itemNumber % 10) == 0 ) {
                onProgress(itemNumber, itemTotal, MessageFormat.format(format, params));
            }
        }
        if (zipLog != null) return zipLog.traceMessage(state, itemNumber, itemTotal, format, params);
        return null;
    }

    /**
     * interface ZipLog: adds an errormessage to error-result
     */
    @Override
    public void addError(String errorMessage) {
        if (zipLog != null) zipLog.addError(errorMessage);
    }

    /**
     * interface ZipLog: get last error plus debugLogMessages if available
     */
    @Override
    public String getLastError(boolean detailed) {
        if (zipLog != null) return zipLog.getLastError(detailed);
        return null;
    }

    public void cancel() {
        this.continueProcessing = false;
        if (this.job != null) job.cancel();
    }
}

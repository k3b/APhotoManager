/*
 * Copyright (c) 2020-2021 by k3b.
 *
 * This file is part of #APhotoManager (https://github.com/k3b/APhotoManager/)
 *              and #toGoZip (https://github.com/k3b/ToGoZip/).
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *
 * for more details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>
 */
package de.k3b.android.androFotoFinder.media;

import android.content.ContentValues;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.util.Date;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.tagDB.TagSql;
import de.k3b.io.FileUtils;
import de.k3b.io.StringUtils;
import de.k3b.io.VISIBILITY;
import de.k3b.io.collections.SelectedFiles;
import de.k3b.io.filefacade.IFile;
import de.k3b.media.ExifInterfaceEx;
import de.k3b.media.PhotoPropertiesUtil;

/**
 * Android specific Version of {@link ExifInterfaceEx} that updates the
 * Database, when saving exif changes.
 */
public class AndroidExifInterfaceEx extends ExifInterfaceEx {
    // set to true to log what happens to database-ID when changing exif
    private static final boolean DBG_RENAME_IN_DB_ENABLED = true;

    /**
     * if not null temprary jpg rename is active while edit exif in place (src=dest-file)
     */
    private String outTempFilePath;
    private String inOriginalFilePath;
    private Boolean hasXmp;

    public static void init() {
        setFactory(new Factory() {
            @Override
            public ExifInterfaceEx create() {
                return new AndroidExifInterfaceEx();
            }
        });
    }

    @Override
    public void saveAttributes(IFile inFile, IFile outFile,
                               boolean deleteInFileOnFinish, Boolean hasXmp) throws IOException {
        if (!inFile.equals(outFile)) {
            // not change exif in place (same file)
            if (deleteInFileOnFinish) { // move
                renameInDatabase(":saveAttributes", inFile.getCanonicalPath(), outFile.getCanonicalPath(), true, Global.cancelExifChangeIfDatabaseUpdateFails);
            } else { // copy
                insertIntoDatabase(outFile, hasXmp);
            }
        } // if (!inFile.equals(outFile)) else database update is done later in renameSouraceFileBeforeReplaceOrThrow

        super.saveAttributes(inFile, outFile, deleteInFileOnFinish, hasXmp);
        this.hasXmp = hasXmp;
    }

    /**
     * Called for exif change in place (same file is modified with temporary old jpf file).
     */
    @Override
    protected IFile renameSouraceFileBeforeReplaceOrThrow(IFile oldSourcefile, String newName) throws IOException {
        debugIdPaths("renameSouraceFileBeforeReplaceOrThrow begin", oldSourcefile.getAbsolutePath(), newName);
        this.inOriginalFilePath = oldSourcefile.getAbsolutePath();
        this.outTempFilePath = this.inOriginalFilePath + TMP_FILE_SUFFIX;

        if (!renameInDatabase(":renameSouraceFileBeforeReplaceOrThrow",
                this.inOriginalFilePath, this.outTempFilePath, false, Global.cancelExifChangeIfDatabaseUpdateFails)) {
            this.outTempFilePath = null; // failed
        }

        final IFile result = super.renameSouraceFileBeforeReplaceOrThrow(oldSourcefile, newName);
        debugIdPaths("renameSouraceFileBeforeReplaceOrThrow end", oldSourcefile.getAbsolutePath(), newName);
        return result;
    }

    @Override
    protected void beforeCloseSaveOutputStream() throws IOException {
        if (this.outTempFilePath != null) {
            // rename back temp file after modify exif in place
            renameInDatabase(":beforeCloseSaveOutputStream", this.outTempFilePath, this.inOriginalFilePath, true, false);
            this.outTempFilePath = null;
        }
        super.beforeCloseSaveOutputStream();
    }

    private void insertIntoDatabase(IFile outFile, Boolean hasXmp) throws IOException {
        ContentValues values = new ContentValues();
        PhotoPropertiesMediaDBContentValues mediaValueAdapter = new PhotoPropertiesMediaDBContentValues().set(values, null);

        PhotoPropertiesUtil.copyNonEmpty(mediaValueAdapter, this);

        Date lastModified = new Date();
        TagSql.setFileModifyDate(values, lastModified);
        if (this.hasXmp != null) {
            if (this.hasXmp) {
                TagSql.setXmpFileModifyDate(values, lastModified);
            } else {
                TagSql.setXmpFileModifyDate(values, TagSql.EXT_LAST_EXT_SCAN_NO_XMP);
            }
        }

        values.put(FotoSql.SQL_COL_PATH, outFile.getCanonicalPath());
        Uri result = FotoSql.getMediaDBApi().execInsert("Copy with Autoprocessing", values);
        if (result != null) {
            String message = " copy insertIntoDatabase('" + outFile + "') failed ";
            if (DBG_RENAME_IN_DB_ENABLED) {
                Log.e(Global.LOG_CONTEXT, message);
            }
            if (Global.cancelExifChangeIfDatabaseUpdateFails) {
                throw new IOException(message);
            }
        }

    }

    // TODO additional database parameters (see scanner)
    // DateLastModified, xmpDate, ....
    private boolean renameInDatabase(
            String dbgContext, String fromPath, String toPath,
            boolean thransferExif, boolean throwOnErrorMessage) throws IOException {
        ContentValues values = new ContentValues();
        if (thransferExif) {
            PhotoPropertiesMediaDBContentValues mediaValueAdapter = new PhotoPropertiesMediaDBContentValues().set(values, null);

            PhotoPropertiesUtil.copyNonEmpty(mediaValueAdapter, this);

            Date lastModified = new Date();
            TagSql.setFileModifyDate(values, lastModified);
            if (this.hasXmp != null) {
                if (this.hasXmp) {
                    TagSql.setXmpFileModifyDate(values, lastModified);
                } else {
                    TagSql.setXmpFileModifyDate(values, TagSql.EXT_LAST_EXT_SCAN_NO_XMP);
                }
            }
        }
        values.put(FotoSql.SQL_COL_PATH, toPath);
        debugIdPaths(dbgContext + " renameInDatabase begin", fromPath, toPath);
        final int execResultCount = FotoSql.getMediaDBApi().
                execUpdate(this.getClass().getSimpleName() + dbgContext, fromPath, values, null);

        debugIdPaths(dbgContext + " renameInDatabase end " + execResultCount, fromPath, toPath);
        if (execResultCount != 1) {
            String message = dbgContext + " renameInDatabase('" + fromPath +
                    "' => '" + toPath + "') returned " + execResultCount + ". Expected 1.";
            if (DBG_RENAME_IN_DB_ENABLED) {
                Log.e(Global.LOG_CONTEXT, message);
            }
            if (throwOnErrorMessage) {
                this.outTempFilePath = null;
                throw new IOException(message);
            }
        }
        return 1 == execResultCount;
    }

    private void debugIdPaths(String dbgContext, String... paths) {
        if (DBG_RENAME_IN_DB_ENABLED) {
            StringBuilder sqlWhere = new StringBuilder();
            for (String path : paths) {
                if (sqlWhere.length() > 0) {
                    sqlWhere.append(" OR ");
                }
                sqlWhere.append("(").append(FotoSql.SQL_COL_PATH).append(" like '")
                        .append(FileUtils.replaceExtension(path, "")).append("%')");
            }

            // to prevent adding visibility
            sqlWhere.append(" and " +
                    FotoSql.SQL_COL_EXT_MEDIA_TYPE +
                    " is not null");
            final SelectedFiles selectedfiles = FotoSql.getSelectedfiles(sqlWhere.toString(), VISIBILITY.PRIVATE_PUBLIC);
            Log.d(Global.LOG_CONTEXT, dbgContext + "\n\t["
                    + StringUtils.appendMessage(paths)
                    + "] :\n\t\t"
                    + selectedfiles.toIdString() + " -> " + selectedfiles.toPathListString());
        }
    }
}

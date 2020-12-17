/*
 * Copyright (c) 2015-2020 by k3b.
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

package de.k3b.android.io;

import android.content.ContentValues;
import android.net.Uri;

import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.queries.MediaDBRepository;
import de.k3b.io.FileCommands;
import de.k3b.io.filefacade.IFile;
import de.k3b.media.PhotoPropertiesUtil;

/**
 * Api to manipulate files/photos.
 * Same as FileCommands with update media database.
 * <p>
 * Created by k3b on 03.08.2015.
 */
public class AndroidFileCommandsDbImpl extends FileCommands {
    /**
     * copies a file from the sourceFullPath path to the target path.
     * Android specific: also updates database.
     *
     * @param sourceFullPath the path of the file that shall be copied including the file name with ending
     * @param targetFullPath the path of the file  that shall be written to with filename
     */
    @Override
    protected boolean osFileCopy(IFile targetFullPath, IFile sourceFullPath) {
        final String srcPath = sourceFullPath.getAbsolutePath();
        String toPath = null;
        boolean dbSuccess = false;
        if (PhotoPropertiesUtil.isImage(srcPath, PhotoPropertiesUtil.IMG_TYPE_ALL)) {
            toPath = targetFullPath.getAbsolutePath();
            dbSuccess = (null != copyInDatabase("osFileCopy", srcPath, toPath));
        }

        if (dbSuccess) {
            dbSuccess = super.osFileCopy(targetFullPath, sourceFullPath);
        }
        return dbSuccess;
    }

    /**
     * Moves a file from the sourceFullPath path to the target path.
     * Android specific: also updates database.
     *
     * @param sourceFullPath the path of the file that shall be copied including the file name with ending
     * @param targetFullPath the path of the file  that shall be written to with filename
     */
    @Override
    protected boolean osFileMove(IFile targetFullPath, IFile sourceFullPath) {
        final String srcPath = sourceFullPath.getAbsolutePath();
        String toPath = null;
        boolean dbSuccess = false;
        // Database update must be done before super.osFileMove to avoid deleting/recreating old file entry
        if (PhotoPropertiesUtil.isImage(srcPath, PhotoPropertiesUtil.IMG_TYPE_ALL)) {
            toPath = targetFullPath.getAbsolutePath();
            dbSuccess = renameInDatabase("osFileMove", srcPath, toPath);
        }
        final boolean osSuccess = super.osFileMove(targetFullPath, sourceFullPath);
        if (!osSuccess && dbSuccess) {
            // os falled. Rollback
            renameInDatabase("osFileMove-rollback", toPath, srcPath);
        }
        return osSuccess;
    }

    @Override
    protected boolean osDeleteFile(IFile file) {
        return super.osDeleteFile(file);
    }

    private boolean renameInDatabase(String dbgContext, String fromPath, String toPath) {
        ContentValues values = new ContentValues();
        values.put(FotoSql.SQL_COL_PATH, toPath);
        final int execResultCount = FotoSql.getMediaDBApi().
                execUpdate(this.getClass().getSimpleName() + dbgContext, fromPath, values, null);

        return 1 == execResultCount;
    }

    private Uri copyInDatabase(String dbgContext, String fromPath, String toPath) {
        ContentValues values = MediaDBRepository.getContentValues(fromPath, null, dbgContext);
        if (values != null) {
            values.put(FotoSql.SQL_COL_PATH, toPath);
            return FotoSql.getMediaDBApi().
                    execInsert(this.getClass().getSimpleName() + dbgContext, values);
        }
        return null;
    }

}


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

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.InputStream;

import de.k3b.io.FileUtils;
import de.k3b.zip.IZipConfig;
import de.k3b.zip.LibZipGlobal;
import de.k3b.zip.ZipConfigRepository;

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

    public static IZipConfig execute(IZipConfig mFilter) {
        ZipConfigRepository repo = new ZipConfigRepository(mFilter);
        final File zipConfigFile = repo.getZipConfigFile();
        if (zipConfigFile != null) {
            if (repo.save()) {
                if (LibZipGlobal.debugEnabled) {
                    Log.d(LibZipGlobal.LOG_TAG, mDebugPrefix + " Saved as " + repo);
                }
                return repo;
            }
        }
        return null;
    }
}

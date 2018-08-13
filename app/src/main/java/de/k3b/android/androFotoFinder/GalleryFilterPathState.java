/*
 * Copyright (c) 2018 by k3b.
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

package de.k3b.android.androFotoFinder;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;

import java.io.File;

import de.k3b.android.util.IntentUtil;
import de.k3b.io.AlbumFile;
import de.k3b.io.FileUtils;
import de.k3b.io.StringUtils;

/**
 * Created by k3b on 09.08.2018.
 */
public class GalleryFilterPathState {
    private static final String KEY_LastAlbum = "GalleryFilterActivity-LastAlbum";
    private static final String KEY_CurrentAlbum = "GalleryFilterActivity-CurrentAlbum";
    private static final String KEY_LastFilterPath = "GalleryFilterActivity-LastFilterPath";

    /*
        Concept path currend
            - (1) settings-lastAlbum = dir for "save album as"
                    loaded-from/saved-to settings
                    default for save album as
            - (2) bundle-currentAlbum (that will be saved on exit)
                    initially set from intent-uri
                    overwrites (1)
                    modified by "save as"
                    saved/loaded from bundle
            - (3) settings-last used "filtered path-directory"
                    loaded-from/saved-to settings
                    default for get filter path
            - (4) data-current filtered path-directory
                    overwrites (3) (or 1 if album)
     */
    private Uri mLastAlbum = null;
    private Uri mCurrentAlbum = null;
    private String mLastFilterPath = null;

    public GalleryFilterPathState load(Context context, Intent intent, Bundle savedInstanceState) {
        if (context != null) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            mLastAlbum = get(sharedPref.getString(KEY_LastAlbum, null), mLastAlbum);
            mLastFilterPath = sharedPref.getString(KEY_LastFilterPath, mLastFilterPath);

        }
        if (savedInstanceState != null) {
            mCurrentAlbum = get(savedInstanceState.getString(KEY_CurrentAlbum), mCurrentAlbum);
        }
        if (intent != null) {
            if (mCurrentAlbum == null) {
                Uri uri = IntentUtil.getUri(intent);
                if ((uri != null) && AlbumFile.isQueryFile(uri.getPath())) {
                    mCurrentAlbum = uri;
                    mLastAlbum = uri;

                }
            }
        }
        return this;
    }

    public GalleryFilterPathState save(Context context, Bundle savedInstanceState) {
        saveAsPreference(context, this.mLastAlbum, this.mLastFilterPath);

        if (savedInstanceState != null) {
            if (mCurrentAlbum != null)
                savedInstanceState.putString(KEY_CurrentAlbum, mCurrentAlbum.toString());
        }
        return this;
    }

    public static void saveAsPreference(Context context, Uri lastAlbum, String lastFilterPath) {
        if (context != null) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = sharedPref.edit();
            if (lastFilterPath != null) editor.putString(KEY_LastFilterPath, lastFilterPath);
            if (lastAlbum != null) editor.putString(KEY_LastAlbum, lastAlbum.toString());
            editor.commit();
        }
    }

    private Uri get(String str, Uri defaultValue) {
        Uri uri = null;
        if (str != null) uri = Uri.parse(str);
        if (uri != null) return uri;
        return defaultValue;
    }

    public Uri getAlbumDefault() {
        return mLastAlbum;
    }

    public File getSaveAlbumAs(String newFilePrefix, String newFileSuffix) {
        if (mCurrentAlbum != null) return getFile(mCurrentAlbum);
        File parentDir = getExistingParentDirFile(this.mLastAlbum);
        if (parentDir == null) {
            parentDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        }
        return FileUtils.getFirstNonExistingFile(parentDir, newFilePrefix, 0, newFileSuffix);
    }

    public static File getFile(Uri uri) {
        if (uri != null) {
            String path = FileUtils.fixPath(uri.getPath());
            if (path != null) {
                return new File(path);
            }
        }
        return null;
    }

    public static File getExistingParentDirFile(Uri uri) {
        return FileUtils.getFirstExistingDir(getFile(uri));
    }

    public void setAlbum(Uri album) {
        this.mLastAlbum = album;
        this.mCurrentAlbum = album;
    }

    public Uri getCurrentAlbum() {
        return mCurrentAlbum;
    }

    public String getPathDefault(String currentPath) {
        if (!StringUtils.isNullOrEmpty(currentPath)) return currentPath;
        return mLastFilterPath;
    }

    public void setLastPath(String mLastFilterPath) {
        this.mLastFilterPath = mLastFilterPath;
    }
}

/*
 * Copyright (c) 2017-2020 by k3b.
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

package de.k3b.android.androFotoFinder.imagedetail;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.Date;

import de.k3b.android.androFotoFinder.AdapterArrayHelper;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.util.PhotoPropertiesMediaFilesScanner;
import de.k3b.io.filefacade.IFile;

/**
 * Purpose: allow viewing images from ".nomedia" folders where no data is available in mediadb/cursor.
 * Same as ImagePagerAdapterFromCursor but while underlaying cursor has
 * no data photos are taken from array instead.
 *
 * Created by k3b on 12.04.2016.
 */
public class ImagePagerAdapterFromCursorArray extends ImagePagerAdapterFromCursor {

    private final Uri imageUri;
    /**
     * not null data comes from array instead from base implementation
     */
    private AdapterArrayHelper mArrayImpl = null;

    public ImagePagerAdapterFromCursorArray(final Activity context, String name, IFile fullPhotoPath, Uri imageUri) {
        super(context, name);
        this.imageUri = imageUri;

        if (PhotoPropertiesMediaFilesScanner.isNoMedia(fullPhotoPath, PhotoPropertiesMediaFilesScanner.DEFAULT_SCAN_DEPTH)) {
            mArrayImpl = new AdapterArrayHelper(context, fullPhotoPath, "debugContext");
        }
    }

    /** get informed that cursordata may be available so array can be disabled */
    @Override
    public Cursor swapCursor(Cursor newCursor) {
        Cursor oldCursor = super.swapCursor(newCursor);
        if (super.getCount() > 0) {
            // cursor has data => disable aray
            this.mArrayImpl = null;
        }
        return oldCursor;
    }

    @Override
    public int getCount() {
        if (isInSingleImageMode()) return 1;
        if (mArrayImpl != null) {
            return mArrayImpl.getCount();
        }

        return super.getCount();
    }

    @Override
    public IFile getFullFilePath(int position) {
        IFile result = getLocalFullFilePath(position);
        if (result == null) result = super.getFullFilePath(position);
        return result;
    }

    private IFile getLocalFullFilePath(int position) {
        if (isInSingleImageMode()) return null;
        if (mArrayImpl != null) return mArrayImpl.getFullFilePathfromArray(position);
        return null;
    }

    public boolean isInSingleImageMode() {
        if (imageUri != null) return true;
        return super.isInSingleImageMode();
    }

    /**
     * translates offset in adapter to id of image
     */
    @Override
    public long getImageId(int position) {
        if (isInSingleImageMode()) return -1;
        if (mArrayImpl != null) return mArrayImpl.getImageId(position);
        return super.getImageId(position);
    }

    /**
     * gets uri that belongongs to current image
     */
    @Override
    public Uri getImageUri(int position) {
        Uri uri = super.getImageUri(position);

        if ((uri == null) && (isInSingleImageMode())) {
            uri = this.imageUri;
        }

        return uri;
    }

    @Override
    public Date getDatePhotoTaken(int position) {
        if (isInSingleImageMode()) return null;
        if (mArrayImpl != null) return null;
        return super.getDatePhotoTaken(position);
    }

    @Override
    public View instantiateItem(ViewGroup container, int position) {
        IFile file = getFile(position);
        if (file != null) {
            // special case image from ".nomedia" folder via absolute path not via content: uri
            return createViewWithContent(position, container, file, null, "instantiateItemFromArray(#", 32767);
        }

        if (isInSingleImageMode()) {
            // special case where uri exists and cannot be translated to file uri.
            return createViewWithContent(position, container, null, this.imageUri, "instantiateItemFromArray(" + imageUri, 32767);
        }

        // no array avaliable. Use original cursor baed implementation
        return super.instantiateItem(container, position);
    }
    /** internal helper. return -1 if position is not available */
    @Override
    public int getPositionFromPath(IFile path) {
        if (mArrayImpl != null) {
            int result = mArrayImpl.getPositionFromPath(path);

            if (Global.debugEnabledViewItem) Log.i(Global.LOG_CONTEXT, mDebugPrefix + "getPositionFromPath-Array(" + path +") => " + result);
            return result;
        }
        return super.getPositionFromPath(path);
    }

    public void refreshLocal() {
        if (mArrayImpl != null) mArrayImpl.reload(" after move delete rename ");
    }

    public boolean isInArrayMode() {
        return (mArrayImpl != null);
    }
}

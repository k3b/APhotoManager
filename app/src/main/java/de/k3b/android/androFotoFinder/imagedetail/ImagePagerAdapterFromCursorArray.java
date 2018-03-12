/*
 * Copyright (c) 2017-2018 by k3b.
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
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.Date;

import de.k3b.android.androFotoFinder.AdapterArrayHelper;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.util.MediaScanner;

/**
 * Purpose: allow viewing images from ".nomedia" folders where no data is available in mediadb/cursor.
 * Same as ImagePagerAdapterFromCursor but while underlaying cursor has
 * no data photos are taken from array instead.
 *
 * Created by k3b on 12.04.2016.
 */
public class ImagePagerAdapterFromCursorArray extends ImagePagerAdapterFromCursor {

    /** not null data comes from array instead from base implementation */
    private AdapterArrayHelper mArrayImpl = null;

    public ImagePagerAdapterFromCursorArray(final Activity context, String name, String fullPhotoPath) {
        super(context, name);

        if (MediaScanner.isNoMedia(fullPhotoPath,MediaScanner.DEFAULT_SCAN_DEPTH)) {
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
        if (mArrayImpl != null) {
            return mArrayImpl.getCount();
        }

        return super.getCount();
    }

    @Override
    public String getFullFilePath(int position) {
        if (mArrayImpl != null) return mArrayImpl.getFullFilePathfromArray(position);
        return super.getFullFilePath(position);
    }

    /** translates offset in adapter to id of image */
    @Override
    public long getImageId(int position) {
        if (mArrayImpl != null) return mArrayImpl.getImageId(position);
        return super.getImageId(position);
    }

    @Override
    public Date getDatePhotoTaken(int position) {
        if (mArrayImpl != null) return null;
        return super.getDatePhotoTaken(position);
    }

    @Override
    public View instantiateItem(ViewGroup container, int position) {
        final String fullPhotoPath = (mArrayImpl != null) ? mArrayImpl.getFullFilePathfromArray(position) : null;
        if (fullPhotoPath != null) {
            // special case image from ".nomedia" folder via absolute path not via content: uri

            return createViewWithContent(position, container, fullPhotoPath, "instantiateItemFromArray(#", 32767);
        }

        // no array avaliable. Use original cursor baed implementation
        return  super.instantiateItem(container,position);
    }
    /** internal helper. return -1 if position is not available */
    @Override
    public int getPositionFromPath(String path) {
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

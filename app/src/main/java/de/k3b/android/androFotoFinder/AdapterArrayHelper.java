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
 
package de.k3b.android.androFotoFinder;

import android.app.Activity;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.io.FileUtils;
import de.k3b.io.VISIBILITY;
import de.k3b.io.collections.SelectedItems;
import de.k3b.io.filefacade.IFile;
import de.k3b.media.PhotoPropertiesUtil;

/**
 * Implements the array sepecific stuff that hopefully can be reused in other adapters, too
 */
public class AdapterArrayHelper {
    private final String mDebugPrefix;
    /** not null data comes from array instead from base implementation */
    private IFile[] mFullPhotoPaths = null;
    private final IFile mRootDir;

    public AdapterArrayHelper(Activity context, IFile fullPhotoPath, String debugContext) {
        this.mDebugPrefix = debugContext;
        mRootDir = FileUtils.getDir(fullPhotoPath);
        reload(" ctor ");

        if (Global.mustRemoveNOMEDIAfromDB && (mRootDir != null) && (mFullPhotoPaths != null)) {
            String parentDirString = mRootDir.getAbsolutePath();
            FotoSql.execDeleteByPath(debugContext + " AdapterArrayHelper mustRemoveNOMEDIAfromDB ", parentDirString, VISIBILITY.PRIVATE_PUBLIC);
        }
    }

    /** refreshLocal files from inital path */
    public void reload(String why) {
        final List<IFile> iFiles = new ArrayList<>();
        for (IFile f : mRootDir.listFiles()) {
            if (PhotoPropertiesUtil.isImage(f, PhotoPropertiesUtil.IMG_TYPE_ALL)) {
                iFiles.add(f);
            }
        }
        if (iFiles.size() == 0) {
            mFullPhotoPaths = null;
            Log.i(Global.LOG_CONTEXT, mDebugPrefix + why + "AdapterArrayHelper.refreshLocal(" + mRootDir + ") " + 0);
        } else {
            if (mFullPhotoPaths != null && Global.debugEnabled) {
                Log.i(Global.LOG_CONTEXT, mDebugPrefix + why + "AdapterArrayHelper.refreshLocal(" + mRootDir + ") " + mFullPhotoPaths.length);
            }
            mFullPhotoPaths = iFiles.toArray(new IFile[iFiles.size()]);
        }
    }

    public int getCount() {
        return mFullPhotoPaths == null ? 0 : mFullPhotoPaths.length;
    }

    /** return null if no file array or illegal index */
    public IFile getFullFilePathfromArray(int position) {
        if ((mFullPhotoPaths != null) && (position >= 0) && (position < mFullPhotoPaths.length)) {
            return mFullPhotoPaths[position];
        }
        return null;
    }

    /** internal helper. return -1 if position is not available */
    public int getPositionFromPath(IFile path) {
        if ((mFullPhotoPaths != null) && (path != null)) {
            int result = -1;
            for (int position = 0; position < mFullPhotoPaths.length; position++) {
                if (path.equals(mFullPhotoPaths[position])) {
                    result = position;
                    break;
                }
            }
            if (Global.debugEnabledViewItem) Log.i(Global.LOG_CONTEXT, "AdapterArrayHelper.getPositionFromPath-Array(" + path +") => " + result);
            return result;
        }
        return -1;
    }

    /** helper for SelectedItems.Id2FileNameConverter: converts items.id-s to string array of filenNames. */
    public IFile[] getFileNames(SelectedItems items) {
        if (items != null) {
            ArrayList<IFile> result = new ArrayList<>();

            int size = 0;
            for(Long id : items) {
                IFile path = (id != null) ? getFullFilePathfromArray(convertBetweenPositionAndId(id.intValue())) : null;
                result.add(path);
                if (path != null) size++;
            }

            if (size > 0) {
                return result.toArray(new IFile[size]);
            }
        }
        return null;
    }

    /** translates offset in adapter to id of image */
    public long getImageId(int position) {
        return convertBetweenPositionAndId(position);
    }

    /** translates offset in adapter to id of image and vice versa */
    public int convertBetweenPositionAndId(int value) {
        return -2 -value;
    }
}

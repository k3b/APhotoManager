/*
 * Copyright (c) 2015-2017 by k3b.
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

import java.io.File;

import android.annotation.TargetApi;
import android.os.Build;

/**
 * Version for Android-4.4 (api-19) and up
 * Created by k3b on 19.09.2016.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class AndroidFileCommands44 extends AndroidFileCommands {
    private static boolean useOld = (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT);

    /*
    @Override
    protected boolean osDeleteFile(File file) {
        Uri docUri = Uri.fromFile(file);

        int permissionCode = mContext.checkCallingOrSelfUriPermission(docUri,Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        if (permissionCode == PackageManager.PERMISSION_DENIED) return false;

        return super.osDeleteFile(file);
    }

    @Override
    protected boolean osFileMove(File dest, File source) {
        if (useOld) return super.osFileMove(dest,source);

        DocumentFile docFilesource = DocumentFile.fromFile(source);
        Uri docUrisource = Uri.fromFile(source);
        mContext.grantUriPermission(mContext.getPackageName(), docUrisource, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);


//        DocumentFile docFiledest = DocumentFile.fromFile(dest);
//        Uri docUridest = Uri.fromFile(dest);
//        mContext.grantUriPermission(mContext.getPackageName(), docUridest, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);


        return docFilesource.renameTo(dest.getAbsolutePath());
    }

    @Override
    protected boolean osFileCopy(File targetFullPath, File sourceFullPath) {
        if (useOld) return super.osFileCopy(targetFullPath, sourceFullPath);

        return _osFileCopy(targetFullPath, sourceFullPath, this);
    }

*/
}

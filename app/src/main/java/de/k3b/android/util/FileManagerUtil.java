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

package de.k3b.android.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;

import java.io.File;
import java.util.List;

/**
 * Created by k3b on 27.02.2018.
 */

public class FileManagerUtil {
    private static Intent getIntentShowInFilemanager(Activity context, String path) {
        if ((path != null) && (path.length() > 0)) {
            final Uri pathUri = Uri.fromFile(new File(path));

            Intent startIntent = new Intent();
            startIntent.setAction(Intent.ACTION_VIEW);

            // https://developer.android.com/reference/android/provider/DocumentsContract.html#EXTRA_INITIAL_URI
            // DocumentsContract.EXTRA_INITIAL_URI="android.provider.extra.INITIAL_URI" since api 26
            startIntent.putExtra("android.provider.extra.INITIAL_URI", pathUri);
            startIntent.putExtra("org.openintents.extra.ABSOLUTE_PATH", pathUri);
            startIntent.setDataAndType(pathUri, "resource/folder");

            // https://github.com/SimpleMobileTools/Simple-File-Manager/issues/84 https://github.com/SimpleMobileTools/Simple-File-Manager/commit/0187fb3c036df14f4444a23477d145e4cb591978
            //         public static final String MIME_TYPE_DIR = "vnd.android.document/directory";
            if (isInstalled(context, startIntent)) {
                return startIntent;
            }

            startIntent.setDataAndType(pathUri, DocumentsContract.Document.MIME_TYPE_DIR);

            if (isInstalled(context, startIntent)) {
                return startIntent;
            }
        }
        return null;
    }

    public static boolean showInFilemanager(Activity context, String path) {
        Intent startIntent = getIntentShowInFilemanager(context, path);
        if (startIntent != null) {
            context.startActivity(startIntent);
            return true;
        }

        return false;
    }

    public static boolean hasShowInFilemanager(Activity context, String path) {
        Intent startIntent = getIntentShowInFilemanager(context, path);
        return (startIntent != null);
    }

    private static boolean isInstalled(Context context, Intent intent) {
        final List<ResolveInfo> list = ((PackageManager)context.getPackageManager()).queryIntentActivities(intent, 0);

        return ((list != null) && (list.size() > 0));
    }
}

/*
 * Copyright (c) 2015-2016 by k3b.
 *
 * This file is part of AndroFotoFinder.
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
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

import de.k3b.android.androFotoFinder.Common;
import de.k3b.android.androFotoFinder.Global;

/**
 * Created by k3b on 09.09.2015.
 */
public class IntentUtil implements Common {

    /** get uri from data. if there is no data from EXTRA_STREAM */
    @Nullable
    public static Uri getUri(Intent intent) {
        Uri uri = intent.getData();
        Bundle extras = (uri != null) ? null : intent.getExtras();
        Object stream = (extras == null) ? null : extras.get(EXTRA_STREAM);
        if (stream != null) {
            uri = Uri.parse(stream.toString());
        }
        return uri;
    }

    /** return null if uri is not a valid file scheam */
    @Nullable
    public static File getFile(Uri uri) {
        if (isFileUri(uri)) {
            try {
                return new File(uri.getPath());
            } catch (Exception ex) {
                ; // i.e. contain illegal chars
            }
        }
        return null;
    }

    public static boolean isFileUri(Uri uri) {
        return (uri != null) && ("file".equals(uri.getScheme()));
    }

    public static boolean isFileUri(String initalFileUrl) {
        return ((initalFileUrl != null) && ((initalFileUrl.indexOf(":") == -1)
                || (initalFileUrl.startsWith("file:"))));
    }

    private static String getMime(String path) {
        return "image/*";
        /*
        MimeTypeMap map = MimeTypeMap.getSingleton();
        return map.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(path));
        */
    }

    public static void cmdStartIntent(Activity parent, String currentFilePath, String currentUri, String extraPath, String action, int idChooserCaption, int idEditError) {

        final Intent outIntent = new Intent()
                .setAction(action)
                // .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (currentFilePath != null) {
            File file = new File(currentFilePath);
            final Uri uri = Uri.fromFile(file);
            outIntent.setDataAndType(uri, IntentUtil.getMime(currentFilePath));
        } else if (currentUri != null) {
            outIntent.setData(Uri.parse(currentUri));
        }

        if (extraPath != null) {
            File file = new File(extraPath);
            final Uri uri = Uri.fromFile(file);
            outIntent.setType(IntentUtil.getMime(extraPath));
            outIntent.putExtra(EXTRA_STREAM, uri);
        }
        if (Global.debugEnabled) {
            Log.d(Global.LOG_CONTEXT,
                    "cmdStartIntent(" + outIntent.toUri(Intent.URI_INTENT_SCHEME) + "')");
        }

        try {
            parent.startActivity(Intent.createChooser(outIntent, parent.getText(idChooserCaption)));
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(parent, idEditError,Toast.LENGTH_LONG).show();
        }
    }


}

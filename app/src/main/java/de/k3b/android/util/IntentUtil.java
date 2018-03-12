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
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

import de.k3b.android.androFotoFinder.Common;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.io.StringUtils;

/**
 * Created by k3b on 09.09.2015.
 */
public class IntentUtil implements Common {

    public static Intent setDataAndTypeAndNormalize(Intent intent, Uri data, String type) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return intent.setDataAndTypeAndNormalize(data, type);
        }
        return intent.setDataAndType(data, type);
    }

    /** either file: or content-uri. If content-uri translate to file uri */
    public static String getFilePath(Context context, Uri uri) {
        // Uri uri = IntentUtil.getUri(intent);
        String path = null;
        if (uri != null) {
            String scheme = uri.getScheme();
            if ((scheme == null) || ("file".equals(scheme))) {
                path = uri.getPath();
            } else if ("content".equals(scheme)) {
                path = FotoSql.execGetFotoPath(context, uri);
                if (path != null) {
                    if (Global.debugEnabled) {
                        Log.i(Global.LOG_CONTEXT, "Translate from '" + uri +
                                "' to '" + path + "'");
                    }
                } else {
                    Log.i(Global.LOG_CONTEXT, "Cannot translate from '" + uri +
                            "' to local file");
                }
            }
        }
        return path;
    }

    /** get uri from data. if there is no data from EXTRA_STREAM */
    @Nullable
    public static Uri getUri(Intent intent) {
        Uri uri = (intent == null) ? null : intent.getData();
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
        if (uri == null) return false;
        String scheme = uri.getScheme();
        return StringUtils.isNullOrEmpty(scheme) || (0 == "file".compareTo(scheme));
    }

    public static boolean isFileUri(String initalFileUrl) {
        return ((initalFileUrl != null) && ((!initalFileUrl.contains(":"))
                || (initalFileUrl.startsWith("file:"))));
    }

    private static String getMime(String path) {
        return "image/*";
        /*
        MimeTypeMap map = MimeTypeMap.getSingleton();
        return map.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(path));
        */
    }

    /**
     * Helper to Execute parent.startActivity(ForResult)()
     *
     * @param parentActivity    activity used to start and to receive actionResult
     * @param currentFilePath   for setData: null or file-path that also defines the content type.
     * @param currentUri        for setData: null or uri to be procesed (i.e. content:...)
     * @param extraPath         null or uri for EXTRA_STREAM
     * @param action            edit, send, view, ....
     * @param idChooserCaption  if != 0 string-resource-id for chooser caption. if 0 no chooser.
     * @param idEditError       string-resource-id for error message if there is no hadler for action
     * @param idActivityResultRequestCode  if != 0 execute startActivityForResult else startActivity
     */
    public static void cmdStartIntent(Activity parentActivity,
                                      String currentFilePath, String currentUri,
                                      String extraPath, String action,
                                      int idChooserCaption, int idEditError,
                                      int idActivityResultRequestCode) {

        final Intent outIntent = new Intent()
                .setAction(action)
                // .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (Intent.ACTION_EDIT.equalsIgnoreCase(action)) {
            // #64 edit allow to modify file
            outIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        }
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
            // #64: edit image (not) via chooser
            final Intent execIntent = (idChooserCaption == 0)
                    ? outIntent
                    : Intent.createChooser(outIntent, parentActivity.getText(idChooserCaption));

            if (idActivityResultRequestCode == 0) {
                parentActivity.startActivity(execIntent);
            } else {
                parentActivity.startActivityForResult(execIntent, idActivityResultRequestCode);
            }
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(parentActivity, idEditError,Toast.LENGTH_LONG).show();
        }
    }


}

/*
 * Copyright (c) 2015-2020 by k3b.
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
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.File;

import de.k3b.android.androFotoFinder.Common;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.io.DocumentFileTranslator;
import de.k3b.android.widget.ActivityWithCallContext;
import de.k3b.androidx.documentfile.DocumentFileEx;
import de.k3b.io.StringUtils;
import de.k3b.io.filefacade.IFile;

/**
 * Created by k3b on 09.09.2015.
 */
public class IntentUtil implements Common {
    public static final String DEFAULT_MIME = "image/*";
    /**
     * false means android does not allow file: uri- any more
     */
    private final static boolean URI_AS_FILE = Build.VERSION.SDK_INT < Build.VERSION_CODES.M;

    public static Intent setDataAndTypeAndNormalize(Intent intent, Uri data, String type) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return intent.setDataAndTypeAndNormalize(data, type);
        }
        return intent.setDataAndType(data, type);
    }

    /**
     * either file: or content-uri. If content-uri translate to file uri
     */
    public static String getFilePath(Context context, Uri uri) {
        File result = getExistingFileOrNull(context, uri);
        return (null == result) ? null : result.getAbsolutePath();
    }

    public static File getExistingFileOrNull(Context context, Uri uri) {
        // Uri uri = IntentUtil.getUri(intent);
        String path = null;
        if (uri != null) {
            String scheme = uri.getScheme();
            if ((scheme == null) || ("file".equals(scheme))) {
                path = uri.getPath();
            } else if (DocumentFileEx.isDocumentUri(context, uri)) {
                path = DocumentFileTranslator.pathFromUri(uri.toString());
            } else if ("content".equals(scheme)) {
                // try to translate via media db
                path = FotoSql.execGetFotoPath(uri);
                if (path != null) {
                    if (Global.debugEnabled) {
                        Log.i(Global.LOG_CONTEXT, "Translate from '" + uri +
                                "' to '" + path + "'");
                    }
                } else {
                    // #118 try to translate from app specific content uri
                    // i.e. "OI file manager" uses "content://org.openintents.filemanager/%2Fstorage%2Femulated%2F0%2FDCIM%2F%F0%9F%93%B8test%2F180122mytest001.jpg"
                    path = uri.getPath();
                }
            }

            // #118 app specific content uri convert from //storage/emulated/0/DCIM/... to /storage/emulated/0/DCIM/
            if ((path != null) && (path.startsWith("//"))) path = path.substring(1);
            return getExistingFileOrNull(path);
        }
        return null;
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
        // #118: opened from "OI file manager" with app specific content uri-s i.e.
        // "content://org.openintents.filemanager/%2Fstorage%2Femulated%2F0%2FDCIM%2F%F0%9F%93%B8test%2F180122mytest001.jpg"
        if (isFileUri(uri)) {
            final File file = getExistingFileOrNull(uri.getPath());
            return file;
        }
        return null;
    }

    private static File getExistingFileOrNull(String fullPath) {
        if (fullPath != null) {
            try {
                final File file = new File(fullPath);
                if ((file != null) && (file.exists())) {
                    return file;
                }
            } catch (Exception ex) {
                Log.d(Global.LOG_CONTEXT,
                        "Cannot open " + fullPath + " as file ");
            }
        }
        return null;
    }

    public static boolean isFileUri(Uri uri) {
        return isFileOrContentUri(uri,false);
    }

    public static boolean isFileOrContentUri(Uri uri, boolean allowContent) {
        if (uri == null) return false;
        String scheme = uri.getScheme();
        return StringUtils.isNullOrEmpty(scheme) || (0 == "file".compareTo(scheme)
                || (allowContent && (0 == "content".compareTo(scheme))));
    }

    public static boolean isFileUri(String initalFileUrl) {
        return ((initalFileUrl != null) && ((!initalFileUrl.contains(":"))
                || (initalFileUrl.startsWith("file:"))));
    }

    private static String getMime(String path, String notFoundValue) {
        final String fileExtensionFromUrl = MimeTypeMap.getFileExtensionFromUrl(path);
        if (!StringUtils.isNullOrEmpty(fileExtensionFromUrl)) {
            MimeTypeMap map = MimeTypeMap.getSingleton();
            final String result = map.getMimeTypeFromExtension(fileExtensionFromUrl);
            if (!StringUtils.isNullOrEmpty(fileExtensionFromUrl)) {
                return result;
            }
        }
        return notFoundValue;
    }

    /**
     * Helper to Execute parent.startActivity(ForResult)()
     * @param debugContext
     * @param parentActivity              activity used to start and to receive actionResult
     * @param action                      edit, send, view, ....
     * @param asExtra                     false: uri via data
     * @param uriString                   prio 1 if not null
     * @param imageId                     prio 2 (3) if > 0
     * @param file                        prio 3 (2) if not null
     * @param idChooserCaption            if != 0 string-resource-id for chooser caption. if 0 no chooser.
     * @param idEditError                 string-resource-id for error message if there is no hadler for action
     * @param idActivityResultRequestCode if != 0 execute startActivityForResult else startActivity
     */
    public static void cmdStartIntent(String debugContext, Activity parentActivity,
                                      String action,
                                      boolean asExtra, String uriString, long imageId, IFile file,


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

        String mime = (file != null) ? IntentUtil.getMime(file.getName(), null) : null;
        Uri uri = null;

        if (uriString != null) {
            uri = Uri.parse(uriString);
            mime = IntentUtil.getMime(uriString, DEFAULT_MIME);
        } else if (URI_AS_FILE && file != null) {
            uri = Uri.fromFile(file.getFile());
        } else if (imageId > 0) {
            if ((mime != null) && (mime.startsWith("image/"))) {
                final Uri baseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                uri = Uri.parse(baseUri.toString() + "/" + imageId);
            } else {
                uri = MediaStore.Files.getContentUri("external", imageId);
            }
        } else if (file != null) {
            uri = Uri.parse(file.getAsUriString());
        }

        if (uri != null) {
            if (mime == null) {
                mime = DEFAULT_MIME;
            }

            if (asExtra) {
                outIntent.setType(mime);
                outIntent.putExtra(EXTRA_STREAM, uri);
            } else {
                outIntent.setDataAndType(uri, mime);
            }

            try {
                if (Global.debugEnabled) {
                    Log.d(Global.LOG_CONTEXT,
                            "cmdStartIntent(" + outIntent.toUri(Intent.URI_INTENT_SCHEME) + "')");
                }

                // #64: edit image (not) via chooser
                final Intent execIntent = (idChooserCaption == 0)
                        ? outIntent
                        : Intent.createChooser(outIntent, parentActivity.getText(idChooserCaption));

                ActivityWithCallContext.additionalCallContext = debugContext;
                ActivityWithCallContext.addContext(debugContext, execIntent, parentActivity);

                if (idActivityResultRequestCode == 0) {
                    parentActivity.startActivity(execIntent);
                } else {
                    parentActivity.startActivityForResult(execIntent, idActivityResultRequestCode);
                }
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(parentActivity, idEditError, Toast.LENGTH_LONG).show();
            }
        }
    }

    public static void startActivity(String debugContext, Activity context, int requestCode, Intent intent) {
        ActivityWithCallContext.additionalCallContext = debugContext;
        ActivityWithCallContext.addContext(debugContext, intent, context);

        if (requestCode != 0) {
            context.startActivityForResult(intent, requestCode);
        } else {
            context.startActivity(intent);
        }
    }

}

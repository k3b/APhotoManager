/*
 * Copyright (c) 2020-2021 by k3b.
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

package de.k3b.android.widget;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.File;

import de.k3b.android.androFotoFinder.Common;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.io.AndroidFileFacade;
import de.k3b.android.io.DocumentFileTranslator;
import de.k3b.io.FileNameUtil;
import de.k3b.io.filefacade.FileFacade;

/**
 * Manage permission in lifecycle for Android-6 ff {@link android.app.Activity}
 * * write to external-storage and
 * * write to sdcard/usbstick,....
 * <p>
 * implemented in {@link #onCreate(Bundle)}
 * when done executes in {@link #onCreateEx(Bundle)} in inherited class
 * <p>
 * how to use:
 * * In all activities replace ".... extends {@link android.app.Activity} with extends {@link FilePermissionActivity}
 * * rename {@link #onCreate(Bundle)} to {@link #onCreateEx(Bundle)}
 * * make shure that in onCreateEx() that there is no call to super.onCreate()
 */
public abstract class FilePermissionActivity extends ActivityWithAsyncTaskDialog {
    private static final int REQUEST_ROOT_DIR = 2001;
    public static final String TAG = "k3b.FilePermAct";
    private static IOnDirectoryPermissionGrantedHandler currentPermissionGrantedHandler = null;

    private static final int REQUEST_ID_WRITE_EXTERNAL_STORAGE = 2000;
    private static final Uri fileRootUri = null;
    private static final String PERMISSION_WRITE_EXTERNAL_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static File currentRootFileRequest = null;
    private DocumentFileTranslator documentFileTranslator = null;

    protected abstract void onCreateEx(Bundle savedInstanceState);

    // workflow onCreate() => requestPermission(PERMISSION_WRITE_EXTERNAL_STORAGE) => onRequestPermissionsResult() => abstract onCreateEx()
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && ActivityCompat.checkSelfPermission(this, PERMISSION_WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermission(PERMISSION_WRITE_EXTERNAL_STORAGE, REQUEST_ID_WRITE_EXTERNAL_STORAGE);
        } else {
            onCreateEx(null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AndroidFileFacade.initFactory(this);
    }

    private void requestPermission(final String permission, final int requestCode) {
        ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ID_WRITE_EXTERNAL_STORAGE: {
                final boolean success = (grantResults != null)
                        && (grantResults.length > 0)
                        && (grantResults[0] == PackageManager.PERMISSION_GRANTED);
                if (success) {
                    if (FileFacade.debugLogSAFFacade) {
                        Log.i(FileFacade.LOG_TAG, this.getClass().getSimpleName()
                                + ": onRequestPermissionsResult(success) ");
                    }
                    onCreateEx(null);
                } else {
                    Log.i(FileFacade.LOG_TAG, this.getClass().getSimpleName()
                            + ": " + getText(R.string.permission_error));
                    Toast.makeText(this, R.string.permission_error, Toast.LENGTH_LONG).show();
                    setResult(Common.RESULT_NO_PERMISSIONS, null);
                    finish();
                }
                return;
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    protected static void requestRootUriDialog(
            final FilePermissionActivity parent, final File rootFile,
            final CharSequence title, final CharSequence message,
            final IOnDirectoryPermissionGrantedHandler permissionGrantedHandler) {
        if ((title != null) || (message != null)) {
            final DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    execRequestRootUri(parent, rootFile, permissionGrantedHandler);
                    dialog.dismiss();
                }
            };

            Dialog dialog;
            if (false) {
                dialog = Dialogs.messagebox(parent, title, message, onClickListener);
            } else {
                dialog = Dialogs.htmlMessagebox(parent, title, message, onClickListener);
            }
            parent.setAutoClose(null, dialog, null);
            /*
            Dialog dialog = AboutDialogPreference.createDialog(parent, title, message, android.R.string.ok);
             */
        } else {
            execRequestRootUri(parent, rootFile, permissionGrantedHandler);
        }
    }

    private static void execRequestRootUri(
            FilePermissionActivity parent,
            File rootFile, IOnDirectoryPermissionGrantedHandler permissionGrantedHandler) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        currentPermissionGrantedHandler = permissionGrantedHandler;
        currentRootFileRequest = rootFile;
        parent.startActivityForResult(intent, REQUEST_ROOT_DIR);
    }

    /**
     *
     * @param dbgContext
     * @param dirs where the permissions are needed.
     * @return null if all permissions are granted or
     * the root file that has not permissions yet.
     */
    public File getMissingRootDirFileOrNull(String dbgContext, File... dirs) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            for (int i = dirs.length - 1; i >= 0; i--) {
                final File dir = dirs[i];
                if (!getDocumentFileTranslator().isKnownRoot(dir)) {
                    final File anddroidRootDir = FileNameUtil.getAnddroidRootDir(dir);
                    if (FileFacade.debugLogSAFFacade) {
                        Log.i(TAG, dbgContext + ":" + this.documentFileTranslator
                                + ":getMissingRootDirFileOrNull(" + dir
                                + ") needs " + anddroidRootDir);
                    }
                    return anddroidRootDir;
                }
            }
        }
        return null;
    }

    public DocumentFileTranslator getDocumentFileTranslator() {
        if (this.documentFileTranslator == null) {
            this.documentFileTranslator = DocumentFileTranslator.create(this);
        }
        return this.documentFileTranslator;
    }

    // ... -> requestRootUriDialog -> execRequestRootUri
    // -> onActivityResult -> onRootUriResult -> IOnDirectoryPermissionGrantedHandler.afterGrant()
    public void requestRootUriDialog(File root, final CharSequence title, IOnDirectoryPermissionGrantedHandler permissionGrantedHandler) {
        requestRootUriDialog(this, root,
                title,
                getString(R.string.select_folder_root_rationale, root),
                permissionGrantedHandler);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ROOT_DIR) {
            if (resultCode == RESULT_OK) {
                onRootUriResult(data.getData());
            } else {
                onRootUriResult(null);
            }
        }
    }

    private void onRootUriResult(Uri documentRootUri) {
        if (FileFacade.debugLogSAFFacade) {
            Log.i(FileFacade.LOG_TAG, this.getClass().getSimpleName()
                    + ": onRootUriResult(" + documentRootUri + ") ");
        }
        IOnDirectoryPermissionGrantedHandler permissionGrantedHandler = currentPermissionGrantedHandler;
        File rootFile = currentRootFileRequest;
        currentPermissionGrantedHandler = null;
        currentRootFileRequest = null;

        if ((rootFile != null) && (documentRootUri != null)) {
            getDocumentFileTranslator().addRoot2DirCache(rootFile, documentRootUri);
            if (permissionGrantedHandler != null) {
                permissionGrantedHandler.afterGrant(this);
            }
        }
    }

    public interface IOnDirectoryPermissionGrantedHandler {
        void afterGrant(FilePermissionActivity activity);
    }
}

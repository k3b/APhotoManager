/*
 * Copyright (c) 2020 by k3b.
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
import android.support.v4.app.ActivityCompat;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

import de.k3b.android.androFotoFinder.Common;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.util.DocumentFileTranslator;
import de.k3b.io.FileNameUtil;

/**
 * Manage permission
 * * write to external-storage and
 * * write to sdcard/usbstick,....
 */
public abstract class FilePermissionActivity extends ActivityWithAutoCloseDialogs {
    private static final int REQUEST_ROOT_DIR = 2001;
    private static IOnDirectoryPermissionGrantedHandler currentPermissionGrantedHandler = null;

    private static final int REQUEST_ID_WRITE_EXTERNAL_STORAGE = 2000;
    private static Uri fileRootUri = null;
    private static final String PERMISSION_WRITE_EXTERNAL_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static File currentRootFileRequest = null;
    private DocumentFileTranslator documentFileTranslator = null;

    private static void requestRootUriDialog(
            final FilePermissionActivity parent, final File rootFile,
            final String title, final String message,
            final IOnDirectoryPermissionGrantedHandler permissionGrantedHandler) {
        if ((title != null) || (message != null)) {
            Dialog dialog = Dialogs.messagebox(parent, title, message, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    execRequestRootUri(parent, rootFile, permissionGrantedHandler);
                    dialog.dismiss();
                }
            });
            parent.setAutoClose(null, dialog, null);
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
                    onCreateEx(null);
                } else {
                    Log.i(Global.LOG_CONTEXT, this.getClass().getSimpleName()
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

    // requestDocumentDirWithPermissionsOrNull -> requestRootUriDialog -> execRequestRootUri
    // -> onActivityResult -> onRootUriResult -> IOnDirectoryPermissionGrantedHandler.afterGrant()

    protected abstract void onCreateEx(Bundle savedInstanceState);

    /**
     * @param dirs where the permissions are needed.
     * @return array of corresponding dirs or
     * null if permission is not ready yet.
     * permissionGrantedHandler will be called when permission is available
     */
    protected DocumentFile[] requestDocumentDirWithPermissionsOrNull(
            IOnDirectoryPermissionGrantedHandler permissionGrantedHandler,
            File... dirs) {
        DocumentFile[] result = new DocumentFile[dirs.length];
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            for (int i = dirs.length - 1; i >= 0; i--) {
                if (null == (result[i] = getDocumentFileTranslator().getOrCreateDirectory(dirs[i]))) {
                    File root = FileNameUtil.getAnddroidRootDir(dirs[i]);
                    requestRootUriDialog(this, root,
                            getString(R.string.permission_error),
                            getString(R.string.select_folder_root_rationale),
                            permissionGrantedHandler);
                    return null;
                }
            }
        } else { // old android does not support request permission
            for (int i = dirs.length - 1; i >= 0; i--) {
                result[i] = DocumentFile.fromFile(dirs[i]);
            }
        }
        return result;
    }

    private DocumentFileTranslator getDocumentFileTranslator() {
        if (this.documentFileTranslator == null) {
            this.documentFileTranslator = DocumentFileTranslator.create(this);
        }
        return this.documentFileTranslator;
    }

    protected void __del_askForDirectoryRoot() {
        // Not enough to access sd-card via file :-(
        if (false && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && fileRootUri == null) {

            // requestRootUri();
        } else {
            onCreateEx(null);
        }

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
        IOnDirectoryPermissionGrantedHandler permissionGrantedHandler = currentPermissionGrantedHandler;
        File rootFile = currentRootFileRequest;
        currentPermissionGrantedHandler = null;
        currentRootFileRequest = null;

        if ((rootFile == null) && (permissionGrantedHandler != null) && (documentRootUri != null)) {
            getDocumentFileTranslator().addRoot(rootFile, DocumentFile.fromTreeUri(this, documentRootUri));
            permissionGrantedHandler.afterGrant(this);
        }
    }

    public interface IOnDirectoryPermissionGrantedHandler {
        void afterGrant(FilePermissionActivity activity);
    }
}

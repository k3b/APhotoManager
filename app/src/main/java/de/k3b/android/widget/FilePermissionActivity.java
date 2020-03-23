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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import de.k3b.android.androFotoFinder.Common;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;

/**
 * Manage permission
 * * write to external-storage and
 * * write to sdcard/usbstick,....
 */
public abstract class FilePermissionActivity extends ActivityWithAutoCloseDialogs {
    private static final int REQUEST_ID_SD_ROOT_DIR = 2001;

    private static final int REQUEST_ID_WRITE_EXTERNAL_STORAGE = 2000;
    private static Uri fileRootUri = null;
    private static final String PERMISSION_WRITE_EXTERNAL_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && ActivityCompat.checkSelfPermission(this, PERMISSION_WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermission(PERMISSION_WRITE_EXTERNAL_STORAGE, REQUEST_ID_WRITE_EXTERNAL_STORAGE);
        } else {
            askForDirectoryRoot();
        }
    }

    protected void askForDirectoryRoot() {
        // Not enough to access sd-card via file :-(
        if (false && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && fileRootUri == null) {

            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_ID_SD_ROOT_DIR);
        } else {
            onCreateEx(null);
        }

    }
    protected abstract void onCreateEx(Bundle savedInstanceState);

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
                    askForDirectoryRoot();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ID_SD_ROOT_DIR && resultCode == RESULT_OK) {
            fileRootUri = data.getData();
            onCreateEx(null);
        }
    }


    protected void requestPermission(final String permission, final int requestCode) {
        ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
    }
}

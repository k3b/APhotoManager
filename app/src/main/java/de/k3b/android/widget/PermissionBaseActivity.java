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

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;

import java.util.HashMap;
import java.util.Map;

/**
 * can Ask for permission
 * <p>
 * inspired by Oleksii Shliama (https://github.com/shliama).
 * <p>
 * Workflow
 * saveAsPublicCroppedImage() {
 * requestPermission(REQUEST_SAVE_EDIT_PICTURE_PERMISSION)
 * <p>
 * onRequestPermissionsResult(REQUEST_SAVE_EDIT_PICTURE_PERMISSION)
 * openPublicOutputUriPicker:
 * startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT), REQUEST_SAVE_EDIT_PICTURE_AS);
 * <p>
 * onActivityResult(REQUEST_SAVE_EDIT_PICTURE_AS, RESULT_OK, data) :
 * edit.onSaveEditPictureAsOutputUriPickerResult(outUri=data.getData())
 */
public abstract class PermissionBaseActivity extends ActivityWithAutoCloseDialogs {
    private static final CallbackHandler permissionHandler = new CallbackHandler();

    /**
     * @param permission    i.e. Manifest.permission.READ_EXTERNAL_STORAGE
     * @param resultHanlder
     */
    protected void requestPermission(final String permission, CallbackHandler.IPermissionResult resultHanlder) {
        Integer id = permissionHandler.getOrCreateId(resultHanlder);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && ActivityCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermission(permission, id);
        } else {
            resultHanlder.onPermissionResult(this, true, null);
        }
    }

    /**
     * Requests given permission.
     * If the permission has been denied previously, a Dialog will prompt the user to grant the
     * permission, otherwise it is requested directly.
     */
    private void requestPermission(final String permission, final int requestCode) {
        ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        final boolean success = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        CallbackHandler.IPermissionResult callback = permissionHandler.id2Callback(Integer.valueOf(requestCode));
        if (callback != null) {
            permissionHandler.id2Callback.put(requestCode, null);
            permissionHandler.callback2Id.put(callback, null);
            callback.onPermissionResult(this, success, null);
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private static class CallbackHandler {
        private static Integer nextId = 1000;
        ;
        private final Map<Integer, IPermissionResult> id2Callback = new HashMap<>();
        private final Map<IPermissionResult, Integer> callback2Id = new HashMap<>();

        public Integer getOrCreateId(IPermissionResult resultHanlder) {
            Integer id = callback2Id.get(resultHanlder);
            if (id == null) {
                id = nextId++;
                callback2Id.put(resultHanlder, id);
                id2Callback.put(id, resultHanlder);
            }
            return id;
        }

        public IPermissionResult id2Callback(Integer requestId) {
            return id2Callback(requestId);
        }

        protected interface IPermissionResult<THIS extends Activity, RESULT_DATA> {
            boolean onPermissionResult(THIS activity, boolean success, RESULT_DATA resultData);
        }
    }

}

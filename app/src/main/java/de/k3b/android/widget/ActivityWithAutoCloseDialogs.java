/*
 * Copyright (c) 2017-2019 by k3b.
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

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;

import java.io.Closeable;

import de.k3b.io.FileUtils;

/**
 * An activity that automatically closes pop-dialogs
 * that where registered with {@link #setAutoClose(DialogFragment, Dialog, Closeable)}
 * on rotation/destruction
 *
 * Created by k3b on 21.06.2017.
 */

public class ActivityWithAutoCloseDialogs extends LocalizedActivity {
    protected DialogFragment mCurrentDialogFragment;
    private Closeable mCloseable;
    private DialogInterface mCurrentDialog;

    protected void closeDialogIfNeeded() {
        // close dialog. else crash in onResume
        if (mCurrentDialog != null) mCurrentDialog.dismiss();

        if (mCurrentDialogFragment != null) { // && (mCurrentDialogFragment.isVisible()) ){
            mCurrentDialogFragment.onDestroyView();
        }
        if (mCloseable != null) { // && (mCurrentDialogFragment.isVisible()) ){
            FileUtils.close(mCloseable, this.getClass().getSimpleName());
        }
        setAutoClose(null, null, null);
    }

    public void setAutoClose(DialogFragment dialogFragment, Dialog dialog, Closeable closeable) {
        mCurrentDialog = dialog;
        this.mCurrentDialogFragment = dialogFragment;
        this.mCloseable = closeable;
    }

    @Override
    protected void onPause() {
        closeDialogIfNeeded();

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        closeDialogIfNeeded();
        super.onDestroy();

        System.gc();
    }

    /**
     * Call back from sub-activities.<br/>
     */
    @Override
    protected void onActivityResult(final int requestCode,
                                    final int resultCode, final Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (mCurrentDialogFragment != null)
            mCurrentDialogFragment.onActivityResult(requestCode, resultCode, intent);
    }
}
/*
 * Copyright (c) 2017 by k3b.
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
 * An activity that automatically closes pop-dialogs on rotation/destruction
 *
 * Created by k3b on 21.06.2017.
 */

public class ActivityWithAutoCloseDialogs extends LocalizedActivity {
    private DialogFragment mDlg;
    private Closeable mCloseable;
    private DialogInterface mDlg1;

    protected void closeDialogIfNeeded() {
        // close dialog. else crash in onResume
        if (mDlg1 != null) mDlg1.dismiss();

        if (mDlg != null) { // && (mDlg.isVisible()) ){
            mDlg.onDestroyView();
        }
        if (mCloseable != null) { // && (mDlg.isVisible()) ){
            FileUtils.close(mCloseable, this.getClass().getSimpleName());
        }
        setAutoClose(null, null, null);
    }

    protected void setAutoClose(DialogFragment dlg, Dialog dlg1, Closeable closeable) {
        mDlg1 = dlg1;
        mDlg = dlg;
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
     * Process Change StartTime (longpress start), Select StopTime before stop
     * (longpress stop) or filter change for detailReport
     */
    @Override
    protected void onActivityResult(final int requestCode,
                                    final int resultCode, final Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (mDlg != null) mDlg.onActivityResult(requestCode, resultCode, intent);
    }
}
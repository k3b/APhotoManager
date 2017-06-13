package de.k3b.android.widget;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;

/**
 * Created by EVE on 21.06.2017.
 */

public class ActivityWithAutoCloseDialogs extends LocalizedActivity {
    private DialogFragment mDlg;
    private DialogInterface mDlg1;

    protected void closeDialogIfNeeded() {
        // close dialog. else crash in onResume
        if (mDlg1 != null) mDlg1.dismiss();

        if (mDlg != null) { // && (mDlg.isVisible()) ){
            mDlg.onDestroyView();
        }
        setAutoClose(null, null);
    }

    protected void setAutoClose(DialogFragment dlg, Dialog dlg1) {
        mDlg1 = dlg1;
        mDlg = dlg;

    }

    @Override
    protected void onPause () {
        closeDialogIfNeeded();

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        closeDialogIfNeeded();
        super.onDestroy();

        System.gc();
    }

}

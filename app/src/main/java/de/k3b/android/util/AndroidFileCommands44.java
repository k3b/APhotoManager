package de.k3b.android.util;

import java.io.File;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import de.k3b.android.androFotoFinder.Global;

/**
 * Version for Android-4.4 (api-19) and up
 * Created by k3b on 19.09.2016.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class AndroidFileCommands44 extends AndroidFileCommands {
    private static boolean useOld = (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT);

    /*
    @Override
    protected boolean osDeleteFile(File file) {
        Uri docUri = Uri.fromFile(file);

        int permissionCode = mContext.checkCallingOrSelfUriPermission(docUri,Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        if (permissionCode == PackageManager.PERMISSION_DENIED) return false;

        return super.osDeleteFile(file);
    }

    @Override
    protected boolean osFileMove(File dest, File source) {
        if (useOld) return super.osFileMove(dest,source);

        DocumentFile docFilesource = DocumentFile.fromFile(source);
        Uri docUrisource = Uri.fromFile(source);
        mContext.grantUriPermission(mContext.getPackageName(), docUrisource, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);


//        DocumentFile docFiledest = DocumentFile.fromFile(dest);
//        Uri docUridest = Uri.fromFile(dest);
//        mContext.grantUriPermission(mContext.getPackageName(), docUridest, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);


        return docFilesource.renameTo(dest.getAbsolutePath());
    }

    @Override
    protected boolean osFileCopy(File targetFullPath, File sourceFullPath) {
        if (useOld) return super.osFileCopy(targetFullPath, sourceFullPath);

        return _osFileCopy(targetFullPath, sourceFullPath, this);
    }

*/
}

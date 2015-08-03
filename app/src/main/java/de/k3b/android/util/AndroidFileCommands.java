package de.k3b.android.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.media.MediaScannerConnection;
import android.os.Environment;

import java.io.File;

import de.k3b.android.androFotoFinder.R;
import de.k3b.io.FileCommands;

/**
 * Api to manipulate files/photos.
 * Same as FileCommands with update media database.
 *
 * Created by k3b on 03.08.2015.
 */
public class AndroidFileCommands extends FileCommands {
    private final Activity mContext;

    public AndroidFileCommands(Activity context) {
        this(context, getDefaultLogFile(context));
    }

    public AndroidFileCommands(Activity context, String logFilePath) {
        super(logFilePath);
        mContext = context;
    }

    private static String getDefaultLogFile(Activity context) {
        Boolean isSDPresent = true; // Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);

        // since android 4.4 Environment.getDataDirectory() and .getDownloadCacheDirectory ()
        // is protected by android-os :-(
        // app will not work on devices with no external storage (sdcard)
        final File rootDir = ((isSDPresent)) ? Environment.getExternalStorageDirectory() : Environment.getRootDirectory();
        final String zipfile = rootDir.getAbsolutePath() + "/" + context.getString(R.string.log_file_path);
        return zipfile;
    }

    /** called for each modified/deleted file */
    @Override
    protected void onPostProcess(String[] paths) {
        super.onPostProcess(paths);
        updateMediaDatabase(paths);
    }

    public void updateMediaDatabase(String... pathNames) {
        MediaScannerConnection.scanFile(
                mContext,
                pathNames, // mPathNames.toArray(new String[mPathNames.size()]),
                null, null);
    }

    public void deleteFileWithQuestion(final String... pathNames) {
        StringBuffer names = new StringBuffer();
        for (String name : pathNames) {
            names.append(name).append("\n");
        }
        final String message = mContext
                .getString(R.string.format_confirm_delete, names.toString());

        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        final String title = mContext.getText(R.string.title_confirm_removal)
                .toString();

        builder.setTitle(title + pathNames.length);
        builder.setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.btn_yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(
                                    final DialogInterface dialog,
                                    final int id) {
                                deleteFile(pathNames);
                            }
                        }
                )
                .setNegativeButton(R.string.btn_no,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(
                                    final DialogInterface dialog,
                                    final int id) {
                                dialog.cancel();
                            }
                        }
                );

        final AlertDialog alert = builder.create();
        alert.show();

    }
}

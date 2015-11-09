/*
 * Copyright (c) 2015 by k3b.
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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.directory.DirectoryPickerFragment;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.io.DirectoryFormatter;
import de.k3b.io.FileCommands;
import de.k3b.io.IDirectory;
import de.k3b.io.OSDirectory;

/**
 * Api to manipulate files/photos.
 * Same as FileCommands with update media database.
 *
 * Created by k3b on 03.08.2015.
 */
public class AndroidFileCommands extends FileCommands {
    private static final String SETTINGS_KEY_LAST_COPY_TO_PATH = "last_copy_to_path";
    private static final String mDebugPrefix = "AndroidFileCommands.";
    private Activity mContext;
    private AlertDialog mActiveAlert = null;

    public AndroidFileCommands() {
        // setLogFilePath(getDefaultLogFile());
        setContext(null);
    }

    public void closeAll() {
        closeLogFile();
        if (mActiveAlert != null) {
            mActiveAlert.dismiss();
            mActiveAlert = null;
        }
    }
    public String getDefaultLogFile() {
        Boolean isSDPresent = true; // Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);

        // since android 4.4 Evnvironment.getDataDirectory() and .getDownloadCacheDirectory ()
        // is protected by android-os :-(
        // app will not work on devices with no external storage (sdcard)
        final File rootDir = (isSDPresent) ? Environment.getExternalStorageDirectory() : Environment.getRootDirectory();
        final String zipfile = rootDir.getAbsolutePath() + "/" + mContext.getString(R.string.global_log_file_path);
        return zipfile;
    }

    /** called before copy/move/rename/delete */
    @Override
    protected void onPreProcess(String what, String[] oldPathNames, String[] newPathNames, int opCode) {
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, mDebugPrefix + " onPreProcess('" + what + "')");
        }
        super.onPreProcess(what, oldPathNames, newPathNames, opCode);
    }

    /** called for each modified/deleted file */
    @Override
    protected void onPostProcess(String what, String[] oldPathNames, String[] newPathNames, int modifyCount, int itemCount, int opCode) {
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, mDebugPrefix
                    + " onPostProcess('" + what + "') => " + modifyCount + "/" + itemCount);
        }
        super.onPostProcess(what, oldPathNames, newPathNames, modifyCount, itemCount, opCode);

        int resId = getResourceId(opCode);
        String message = mContext.getString(resId, Integer.valueOf(modifyCount), Integer.valueOf(itemCount));
        if (itemCount > 0) {
            MediaScanner.updateMediaDBInBackground(mContext, message, oldPathNames, newPathNames);
        }

        Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
    }

    private int getResourceId(int opCode) {
        switch (opCode) {
            case OP_COPY: return R.string.copy_result_format;
            case OP_MOVE: return R.string.move_result_format;
            case OP_DELETE: return R.string.delete_result_format;
            case OP_RENAME: return R.string.rename_result_format;
            case OP_UPDATE: return R.string.update_result_format;
        }
        return 0;

    }

    public boolean onOptionsItemSelected(final MenuItem item, final SelectedFotos selectedFileNames) {
        if ((selectedFileNames != null) && (selectedFileNames.size() > 0)) {
            // Handle item selection
            switch (item.getItemId()) {
                case R.id.cmd_delete:
                    return cmdDeleteFileWithQuestion(selectedFileNames);
            }
        }
        return false;
    }

    public boolean rename(Long fileId, File dest, File src) {
        int result = moveOrCopyFiles(true, "rename", new File[]{dest}, new File[]{src});
        return (result != 0);
    }

    public void onMoveOrCopyDirectoryPick(boolean move, IDirectory destFolder, SelectedFotos srcFotos) {
        if (destFolder != null) {
            String copyToPath = destFolder.getAbsolute();
            setLastCopyToPath(copyToPath);
            File destDirFolder = new File(copyToPath);

            String[] selectedFileNames = srcFotos.getFileNames(mContext);
            moveOrCopyFilesTo(move, destDirFolder, SelectedFotos.getFiles(selectedFileNames));
        }
    }

    @NonNull
    public String getLastCopyToPath() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        return sharedPref.getString(SETTINGS_KEY_LAST_COPY_TO_PATH, "/");
    }

    private void setLastCopyToPath(String copyToPath) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor edit = sharedPref.edit();
        edit.putString(SETTINGS_KEY_LAST_COPY_TO_PATH, copyToPath);
        edit.commit();
    }

    public boolean cmdDeleteFileWithQuestion(final SelectedFotos fotos) {
        String[] pathNames = fotos.getFileNames(mContext);
        StringBuffer names = new StringBuffer();
        for (String name : pathNames) {
            names.append(name).append("\n");
        }
        final String message = mContext
                .getString(R.string.delete_question_message_format, names.toString());

        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        final String title = mContext.getText(R.string.delete_question_title)
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
                                mActiveAlert = null; deleteFiles(fotos);
                            }
                        }
                )
                .setNegativeButton(R.string.btn_no,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(
                                    final DialogInterface dialog,
                                    final int id) {
                                mActiveAlert = null;
                                dialog.cancel();
                            }
                        }
                );

        final AlertDialog alert = builder.create();
        mActiveAlert = alert;
        alert.show();
        return true;
    }

    private int deleteFiles(SelectedFotos fotos) {
        String[] fileNames = fotos.getFileNames(mContext);
        return super.deleteFiles(fileNames);
    }

    class MediaScannerDirectoryPickerFragment extends DirectoryPickerFragment {
        /** do not use activity callback */
        @Override protected void setDirectoryListener(Activity activity) {}

        @Override
        protected void onDirectoryPick(IDirectory selection) {
            dismiss();
            if (selection != null) {
                onMediaScannerAnswer(selection.getAbsolute());
            }
        }
    }

    public boolean cmdMediaScannerWithQuestion() {
        final RecursiveMediaScanner scanner = RecursiveMediaScanner.sScanner;

        if (scanner != null) {
            // connect gui to already running scanner if possible
            scanner.resumeIfNeccessary(); // if paused resume it.
            showMediaScannerStatus(scanner);
            return true;
        } else if (AndroidFileCommands.canProcessFile(mContext)) {
            // show dialog to get start parameter
            DirectoryPickerFragment destDir = new MediaScannerDirectoryPickerFragment() {
                /** do not use activity callback */
                @Override protected void setDirectoryListener(Activity activity) {}

                @Override
                protected void onDirectoryPick(IDirectory selection) {
                    dismiss();
                    if (selection != null) {
                        onMediaScannerAnswer(selection.getAbsolute());
                    }
                }

                @Override
                public void onPause() {
                    super.onPause();

                    // else the java.lang.InstantiationException: can't instantiate
                    // class de.k3b.android.util.AndroidFileCommands$MediaScannerDirectoryPickerFragment;
                    // no empty constructor
                    // on orientation change
                    dismiss();
                }
            };

            destDir.setTitleId(R.string.scanner_dir_question);
            destDir.defineDirectoryNavigation(new OSDirectory("/", null),
                    FotoSql.QUERY_TYPE_UNDEFINED,
                    getLastCopyToPath());
            destDir.setContextMenuId(R.menu.menu_context_osdir);
            destDir.show(mContext.getFragmentManager(), "scannerPick");

            return true;
        }
        return false;
    }

    /** answer from "which directory to start scanner from"? */
    private void onMediaScannerAnswer(String scanRootDir) {
        if  ((AndroidFileCommands.canProcessFile(mContext)) || (RecursiveMediaScanner.sScanner == null)){
            final String message = mContext.getString(R.string.scanner_menu_title);
            final RecursiveMediaScanner scanner = (RecursiveMediaScanner.sScanner != null)
                    ? RecursiveMediaScanner.sScanner :
                    new RecursiveMediaScanner(mContext, message);
            synchronized (scanner) {
                if (RecursiveMediaScanner.sScanner == null) {
                    RecursiveMediaScanner.sScanner = scanner;
                    scanner.execute(new String[]{scanRootDir});
                } // else scanner is already running
            }

            showMediaScannerStatus(RecursiveMediaScanner.sScanner);
        }
    }

    private void showMediaScannerStatus(RecursiveMediaScanner mediaScanner) {
        if (mediaScanner != null) {
            mediaScanner.showStatusDialog(mContext);
        }
    }

    /**
     * Write geo data (lat/lon) to photo, media database and log.<br/>
     *  @param latitude
     * @param longitude
     * @param selectedItems
     * @param itemsPerProgress
     */
    public int setGeo(double latitude, double longitude, SelectedFotos selectedItems, int itemsPerProgress) {
        if (!Double.isNaN(latitude) && !Double.isNaN(longitude) && (selectedItems != null) && (selectedItems.size() > 0)) {
            // in case that current activity is destroyed while running async, applicationContext will allow to finish database operation
            Context applicationContext = this.mContext.getApplicationContext();
            int itemcount = 0;
            int countdown = 0;
            String[] fileNames = selectedItems.getFileNames(this.mContext);
            if (fileNames != null) {
                File[] files = SelectedFotos.getFiles(fileNames);
                int maxCount = files.length+1;
                openLogfile();
                for (File file : files) {
                    countdown--;
                    if (countdown <= 0) {
                        countdown = itemsPerProgress;
                        onProgress(itemcount, maxCount);
                    }
                    ExifGps.saveLatLon(file, latitude, longitude);
                    log("CALL setgps  ", getFilenameForLog(file),
                            " ", DirectoryFormatter.parseLatLon(latitude), " ", DirectoryFormatter.parseLatLon(longitude));
                    itemcount++;
                }
                onProgress(itemcount, maxCount);
                int result = FotoSql.execUpdateGeo(applicationContext, latitude, longitude, selectedItems);
                closeLogFile();
                onProgress(++itemcount, maxCount);
                return result;
            }
        }
        return 0;
    }

    /** called every time when command makes some little progress. Can be mapped to async progress-bar */
    protected void onProgress(int itemcount, int size) {
    }

    public AndroidFileCommands setContext(Activity mContext) {
        this.mContext = mContext;
        if (mContext != null) {
            closeLogFile();
        }
        return this;
    }

    public static AndroidFileCommands log(Activity context, Object... params) {
        AndroidFileCommands cmd = new AndroidFileCommands().setContext(context);
        cmd.setLogFilePath(cmd.getDefaultLogFile());
        cmd.openLogfile();
        cmd.log(params);
        return cmd;
    }

    @Override
    protected boolean canProcessFile(int opCode) {
        if (opCode != OP_UPDATE) {
            return AndroidFileCommands.canProcessFile(this.mContext);
        }
        return true;
    }

    public static boolean canProcessFile(Context context) {
        if (!Global.mustCheckMediaScannerRunning) return true; // always allowed. DANGEROUS !!!

        if (MediaScanner.isScannerActive(context.getContentResolver())) {
            Toast.makeText(context, R.string.scanner_err_busy, Toast.LENGTH_LONG).show();
            return false;
        }

        if (RecursiveMediaScanner.getBusyScanner() != null) {
            return false;
        }
        return true;
    }

}

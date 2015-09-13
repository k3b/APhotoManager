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
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.MenuItem;

import java.io.File;

import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.io.DirectoryFormatter;
import de.k3b.io.FileCommands;
import de.k3b.io.IDirectory;

/**
 * Api to manipulate files/photos.
 * Same as FileCommands with update media database.
 *
 * Created by k3b on 03.08.2015.
 */
public class AndroidFileCommands extends FileCommands {
    private static final String SETTINGS_KEY_LAST_COPY_TO_PATH = "last_copy_to_path";
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
        final String zipfile = rootDir.getAbsolutePath() + "/" + mContext.getString(R.string.log_file_path);
        return zipfile;
    }

    /** called for each modified/deleted file */
    @Override
    protected void onPostProcess(String[] paths, int modifyCount, int itemCount, int opCode) {
        super.onPostProcess(paths, modifyCount, itemCount, opCode);

        if (opCode != OP_DELETE) {
            updateMediaDatabase(paths);
        }
    }

    public void updateMediaDatabase(String... pathNames) {
        SelectedFotos deletedItems = new SelectedFotos();
        MediaScannerConnection.scanFile(
                // http://stackoverflow.com/questions/5739140/mediascannerconnection-produces-android-app-serviceconnectionleaked
                mContext.getApplicationContext(),
                pathNames, // mPathNames.toArray(new String[mPathNames.size()]),
                null, null);
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
        int result = moveOrCopyFiles(true, new File[]{dest}, new File[]{src});
        if ((fileId != null) && (!osFileExists(src))) {
            onMediaDeleted(src.getAbsolutePath(), fileId);
        }

        return (result != 0);
    }

    public void onMoveOrCopyDirectoryPick(boolean move, IDirectory destFolder, SelectedFotos srcFotos) {
        if (destFolder != null) {
            String copyToPath = destFolder.getAbsolute();
            setLastCopyToPath(copyToPath);
            File destDirFolder = new File(copyToPath);

            String[] selectedFileNames = srcFotos.getFileNames(mContext);
            Long[] ids = (move) ? srcFotos.getIds() : null;
            moveOrCopyFilesTo(move, destDirFolder, SelectedFotos.getFiles(selectedFileNames));

            if (move) {
                // remove from media database after successfull move
                File[] sourceFiles = SelectedFotos.getFiles(selectedFileNames);
                for (int i = 0; i < sourceFiles.length; i++) {
                    File sourceFile = sourceFiles[i];
                    if (!osFileExists(sourceFile)) {
                        onMediaDeleted(sourceFile.getAbsolutePath(), ids[i]);
                    }
                }
            }
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
        int result = 0;
        String[] fileNames = fotos.getFileNames(mContext);
        openLogfile();
        File[] toBeDeleted = SelectedFotos.getFiles(fileNames);
        Long[] ids = fotos.getIds();

        for (int i = 0; i < ids.length; i++) {
            File file = toBeDeleted[i];
            if (deleteFileWitSidecar(file)) {
                onMediaDeleted(file.getAbsolutePath(), ids[i]);
                result++;
            }
        }

        closeLogFile();
        onPostProcess(fileNames, result, ids.length, OP_DELETE);

        return result;
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
                    log("setgeo  '" + file.getAbsolutePath() +
                            "' ", DirectoryFormatter.parseLatLon(latitude), " ", DirectoryFormatter.parseLatLon(longitude));
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

    private void onMediaDeleted(String absolutePath, Long id) {
        Uri uri = SelectedFotos.getUri(id);
        mContext.getContentResolver().delete(uri, null, null);
        log("rem deleted '" + absolutePath +
                "' as content: ", uri.toString());
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
}

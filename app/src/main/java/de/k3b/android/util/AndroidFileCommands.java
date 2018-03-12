/*
 * Copyright (c) 2015-2018 by k3b.
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

package de.k3b.android.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.util.Date;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.LockScreen;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.directory.DirectoryPickerFragment;
import de.k3b.android.androFotoFinder.media.AndroidJpgMetaWorkflow;
import de.k3b.android.androFotoFinder.queries.DatabaseHelper;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.tagDB.TagSql;
import de.k3b.android.androFotoFinder.transactionlog.TransactionLogSql;
import de.k3b.database.QueryParameter;
import de.k3b.io.FileUtils;
import de.k3b.io.IProgessListener;
import de.k3b.io.collections.SelectedFiles;
import de.k3b.io.DirectoryFormatter;
import de.k3b.io.FileCommands;
import de.k3b.io.IDirectory;
import de.k3b.media.JpgMetaWorkflow;
import de.k3b.media.MediaUtil;
import de.k3b.media.MetaWriterExifXml;
import de.k3b.transactionlog.MediaTransactionLogEntryType;
import de.k3b.transactionlog.TransactionLoggerBase;

/**
 * Api to manipulate files/photos.
 * Same as FileCommands with update media database.
 *
 * Created by k3b on 03.08.2015.
 */
public class AndroidFileCommands extends FileCommands {
    private static final String SETTINGS_KEY_LAST_COPY_TO_PATH = "last_copy_to_path";
    private static final String mDebugPrefix = "AndroidFileCommands.";
    private boolean isInBackground = false;
    protected Activity mContext; // must be activity because of fragmentManager
    private AlertDialog mActiveAlert = null;
    private boolean mHasNoMedia = false;
    private MediaScanner mScanner = null;

    public AndroidFileCommands() {
        // setLogFilePath(getDefaultLogFile());
        setContext(null);
    }

    public void closeAll() {
        super.closeAll();
        if (mActiveAlert != null) {
            mActiveAlert.dismiss();
            mActiveAlert = null;
        }
    }

    @Override
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
    protected void onPreProcess(String what, int opCode, SelectedFiles selectedFiles, String[] oldPathNames, String[] newPathNames) {
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, mDebugPrefix + "onPreProcess('" + what + "')");
        }

        // a nomedia file is affected => must update gui
        this.mHasNoMedia = mScanner.isNoMedia(22, oldPathNames) || MediaScanner.isNoMedia(22, newPathNames);
        super.onPreProcess(what, opCode, selectedFiles, oldPathNames, newPathNames);
    }

    /** called for each modified/deleted file */
    @Override
    protected void onPostProcess(String what, int opCode, SelectedFiles selectedFiles, int modifyCount, int itemCount, String[] oldPathNames, String[] newPathNames) {
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, mDebugPrefix
                    + "onPostProcess('" + what + "') => " + modifyCount + "/" + itemCount);
        }
        super.onPostProcess(what, opCode, selectedFiles, modifyCount, itemCount, oldPathNames, newPathNames);

        Context context = this.mContext;
        String message = getModifyMessage(context, opCode, modifyCount, itemCount);
        if ((itemCount > 0) && (mScanner != null)) {
            MediaScannerAsyncTask.updateMediaDBInBackground(mScanner, mContext, message, oldPathNames, newPathNames);
        }

        if (false && this.mHasNoMedia && (mContext != null)) {
            // a nomedia file is affected => must update gui
            this.mContext.getContentResolver().notifyChange(FotoSql.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE, null, false);
            this.mHasNoMedia = false;
        }

        if (!isInBackground) {
            Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
        }
    }

    public static String getModifyMessage(Context context, int opCode, int modifyCount, int itemCount) {
        int resId = getResourceId(opCode);
        return context.getString(resId, Integer.valueOf(modifyCount), Integer.valueOf(itemCount));
    }

    /** called for every cath(Exception...). Version with Android specific logging */
    @Override
    protected void onException(final Throwable e, Object... params) {
        StringBuilder message = new StringBuilder();
        message.append(mDebugPrefix).append("onException(");
        for (Object param : params) {
            if (param != null) {
                message.append(param).append(" ");
            }
        }
        message.append("): ").append(e.getMessage());

        Log.e(Global.LOG_CONTEXT, message.toString(), e);
        // e.printStackTrace();
    }

    private static int getResourceId(int opCode) {
        switch (opCode) {
            case OP_COPY: return R.string.copy_result_format;
            case OP_MOVE: return R.string.move_result_format;
            case OP_DELETE: return R.string.delete_result_format;
            case OP_RENAME: return R.string.rename_result_format;
            case OP_UPDATE: return R.string.update_result_format;
            default:break;
        }
        return 0;

    }

    public boolean onOptionsItemSelected(final MenuItem item, final SelectedFiles selectedFileNames) {
        if ((selectedFileNames != null) && (selectedFileNames.size() > 0)) {
            // Handle item selection
            switch (item.getItemId()) {
                case R.id.cmd_delete:
                    return cmdDeleteFileWithQuestion(selectedFileNames);
                default:break;
            }
        }
        return false;
    }

    /**
     * Check if all files in selectedFileNamesToBeModified are not write protected.
     *
     * @return null if no error. else formated error message.
     */
    public String checkWriteProtected(int resIdAction, final File... filesToBeModified) {
        //   <string name="file_err_writeprotected">\'%1$s\' ist schreibgeschützt. \'%2$s\' ist nicht möglich.</string>
        if (filesToBeModified != null) {
            for (File file : filesToBeModified) {
                if ((file != null) && (file.exists()) && (!file.canWrite())) {
                    String action = (resIdAction == 0) ? "" : mContext.getString(resIdAction);
                    // file_err_writeprotected="writeprotected \'%1$s\'.\n\n \'%2$s\' is not possible."
                    return mContext.getString(R.string.file_err_writeprotected, file.getAbsolutePath(), action);
                }
            }
        }
        return null;
    }

    public boolean rename(SelectedFiles selectedFiles, File dest, IProgessListener progessListener) {
        int result = moveOrCopyFiles(true, "rename", null, selectedFiles, new File[]{dest}, progessListener);
        return (result != 0);
    }

    public int execRename(File srcDirFile, String newFolderName) {
        // this will allow to be newFolderName = "../someOtherDir/newName" or even "/absolute/path/to"
        File destDirFile = FileUtils.tryGetCanonicalFile(new File(srcDirFile.getParent(), newFolderName));

        int modifyCount = -1;

        if (destDirFile != null) {
            destDirFile.getParentFile().mkdirs();
            if (srcDirFile.renameTo(destDirFile)) {
                modifyCount = FotoSql.execRenameFolder(this.mContext, srcDirFile.getAbsolutePath() + "/", destDirFile.getAbsolutePath() + "/");
                if (modifyCount < 0) {
                    destDirFile.renameTo(srcDirFile); // error: undo change
                    return -1;
                } else {
                    long now = new Date().getTime();
                    this.addTransactionLog(-1, srcDirFile.getAbsolutePath(), now,
                            MediaTransactionLogEntryType.MOVE_DIR,
                            destDirFile.getAbsolutePath());

                    MediaScanner.notifyChanges(this.mContext,"renamed dir");
                }
            }
        }
        return modifyCount;
    }

    /** implement copy/move called after dest-dir-pick  */
    public void onMoveOrCopyDirectoryPick(boolean move, SelectedFiles selectedFiles, IDirectory destFolder) {
        if (destFolder != null) {
            String copyToPath = destFolder.getAbsolute();
            File destDirFolder = new File(copyToPath);

            setLastCopyToPath(copyToPath);

            //     public int moveOrCopyFilesTo(boolean move, SelectedFiles selectedFiles, File destDirFolder, IProgessListener progessListener) {

            moveOrCopyFilesTo(move, selectedFiles, destDirFolder, null);
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
        edit.apply();
    }

    public boolean cmdDeleteFileWithQuestion(final SelectedFiles fotos) {
        String[] pathNames = fotos.getFileNames();
        String errorMessage = checkWriteProtected(R.string.delete_menu_title, SelectedFiles.getFiles(pathNames));

        if (errorMessage != null) {
            if (!isInBackground) {
                Toast.makeText(this.mContext, errorMessage, Toast.LENGTH_LONG).show();
            }
        } else {
            StringBuilder names = new StringBuilder();
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
                    .setPositiveButton(android.R.string.yes,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(
                                        final DialogInterface dialog,
                                        final int id) {
                                    mActiveAlert = null;
                                    deleteFiles(fotos, null);
                                }
                            }
                    )
                    .setNegativeButton(android.R.string.no,
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
        }
        return true;
    }

    @Override
    public int deleteFiles(SelectedFiles fotos, IProgessListener progessListener) {
        int nameCount = fotos.getNonEmptyNameCount();
        int deleteCount = super.deleteFiles(fotos, progessListener);

        if ((nameCount == 0) || (nameCount == deleteCount)) {
            // no delete file error so also delete media-items
            QueryParameter where = new QueryParameter();
            FotoSql.setWhereSelectionPks (where, fotos.toIdString());

            FotoSql.deleteMedia("AndroidFileCommands.deleteFiles", mContext, where.toAndroidWhere(), null, true);
        }
        return deleteCount;
    }

    @SuppressLint("ValidFragment")
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
        final RecursiveMediaScannerAsyncTask scanner = RecursiveMediaScannerAsyncTask.sScanner;

        if (scanner != null) {
            // connect gui to already running scanner if possible
            scanner.resumeIfNeccessary(); // if paused resume it.
            showMediaScannerStatus(scanner);
            return true;
        } else if (AndroidFileCommands.canProcessFile(mContext, this.isInBackground)) {
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
            destDir.defineDirectoryNavigation(OsUtils.getRootOSDirectory(),
                    FotoSql.QUERY_TYPE_UNDEFINED,
                    getLastCopyToPath());
            destDir.setContextMenuId(LockScreen.isLocked(mContext) ? 0 :  R.menu.menu_context_osdir);
            destDir.show(mContext.getFragmentManager(), "scannerPick");

            return true;
        }
        return false;
    }

    /** answer from "which directory to start scanner from"? */
    private void onMediaScannerAnswer(String scanRootDir) {
        if  ((AndroidFileCommands.canProcessFile(mContext, this.isInBackground)) || (RecursiveMediaScannerAsyncTask.sScanner == null)){

            // remove ".nomedia" file from scan root
            File nomedia = new File(scanRootDir, MediaScanner.MEDIA_IGNORE_FILENAME);
            if (nomedia.exists()) {
                if (Global.debugEnabled) {
                    Log.i(Global.LOG_CONTEXT, mDebugPrefix + "onMediaScannerAnswer deleting " + nomedia);
                }
                nomedia.delete();
            }
            if (Global.debugEnabled) {
                Log.i(Global.LOG_CONTEXT, mDebugPrefix + "onMediaScannerAnswer start scanning " + scanRootDir);
            }

            final String message = mContext.getString(R.string.scanner_menu_title);
            final RecursiveMediaScannerAsyncTask scanner = (RecursiveMediaScannerAsyncTask.sScanner != null)
                    ? RecursiveMediaScannerAsyncTask.sScanner :
                    new RecursiveMediaScannerAsyncTask(mScanner, mContext, message);
            synchronized (this) {
                if (RecursiveMediaScannerAsyncTask.sScanner == null) {
                    RecursiveMediaScannerAsyncTask.sScanner = scanner;
                    scanner.execute(new String[]{scanRootDir});
                } // else scanner is already running
            }

            showMediaScannerStatus(RecursiveMediaScannerAsyncTask.sScanner);
        }
    }

    private void showMediaScannerStatus(RecursiveMediaScannerAsyncTask mediaScanner) {
        if (mediaScanner != null) {
            mediaScanner.showStatusDialog(mContext);
        }
    }

    /**
     * Write geo data (lat/lon) to photo, media database and log.
     */
    public int setGeo(double latitude, double longitude, SelectedFiles selectedItems, int itemsPerProgress) {
        String dbgContext = "setGeo";
        if (!Double.isNaN(latitude) && !Double.isNaN(longitude) && (selectedItems != null) && (selectedItems.size() > 0)) {
            // in case that current activity is destroyed while running async, applicationContext will allow to finish database operation
            File[] files = SelectedFiles.getFiles(selectedItems.getFileNames());
            String errorMessage = checkWriteProtected(R.string.geo_edit_menu_title, files);

            if (errorMessage != null) {
                if (!isInBackground) {
                    Toast.makeText(this.mContext, errorMessage, Toast.LENGTH_LONG).show();
                }
            } else if (files != null) {
                Context applicationContext = this.mContext.getApplicationContext();
                int itemcount = 0;
                int countdown = 0;
                int maxCount = files.length+1;
                openLogfile();
                int resultFile = 0;
                long now = new Date().getTime();

                String latLong = DirectoryFormatter.formatLatLon(latitude) + " " + DirectoryFormatter.formatLatLon(longitude);
                for (int i=0; i < files.length;i++) {
                    countdown--;
                    if (countdown <= 0) {
                        countdown = itemsPerProgress;
                        if (!onProgress(itemcount, maxCount, null)) break;
                    }
                    File file = files[i];
                    MetaWriterExifXml jpg = createWorkflow(null, dbgContext).saveLatLon(file, latitude, longitude);
                    resultFile += TagSql.updateDB(dbgContext, applicationContext,
                            file.getAbsolutePath(), jpg, MediaUtil.FieldID.latitude_longitude);
                    itemcount++;
                    addTransactionLog(selectedItems.getId(i), file.getAbsolutePath(), now, MediaTransactionLogEntryType.GPS, latLong);
                }
                onProgress(itemcount, maxCount, null);

                closeLogFile();
                onProgress(++itemcount, maxCount, null);

                return resultFile;
            }
        }
        return 0;
    }

    public AndroidFileCommands setContext(Activity mContext) {
        this.mContext = mContext;
        if (mContext != null) {
            closeLogFile();
            mScanner = MediaScanner.getInstance(mContext);
        }

        return this;
    }

    @NonNull
    public static AndroidFileCommands createFileCommand(Activity context, boolean isInBackground) {
        AndroidFileCommands cmd = new AndroidFileCommands().setContext(context).setInBackground(isInBackground);
        cmd.createFileCommand();
        cmd.setLogFilePath(cmd.getDefaultLogFile());
        cmd.openLogfile();
        return cmd;
    }

    @NonNull
    public AndroidFileCommands openDefaultLogFile() {
        setLogFilePath(getDefaultLogFile());
        openLogfile();
        return this;
    }

    private AndroidFileCommands setInBackground(boolean isInBackground) {
        this.isInBackground = isInBackground;
        return this;
    }

    @Override
    protected boolean canProcessFile(int opCode) {
        if (opCode != OP_UPDATE) {
            return AndroidFileCommands.canProcessFile(this.mContext, this.isInBackground);
        }
        return true;
    }

    public static boolean canProcessFile(Context context, boolean isInBackground) {
        if (!Global.mustCheckMediaScannerRunning) return true; // always allowed. DANGEROUS !!!

        if (MediaScanner.isScannerActive(context.getContentResolver())) {
            if (!isInBackground) {
                Toast.makeText(context, R.string.scanner_err_busy, Toast.LENGTH_LONG).show();
            }
            return false;
        }

        return (RecursiveMediaScannerAsyncTask.getBusyScanner() == null);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        if (mContext != null) {
            result.append(mContext).append("->");
        }
        result.append(mDebugPrefix);
        return result.toString();
    }

    /** overwrite to create a android specific Workflow */
    @Override
    public JpgMetaWorkflow createWorkflow(TransactionLoggerBase logger, String dbgContext) {
        return new AndroidJpgMetaWorkflow(mContext, logger, dbgContext);
    }


    /** adds android database specific logging to base implementation */
    @Override
    public void addTransactionLog(
            long currentMediaID, String fileFullPath, long modificationDate,
            MediaTransactionLogEntryType mediaTransactionLogEntryType, String commandData) {
        if (fileFullPath != null) {
            super.addTransactionLog(currentMediaID, fileFullPath, modificationDate,
                    mediaTransactionLogEntryType, commandData);
            if (mediaTransactionLogEntryType.getId() != null) {
                // not a comment
                SQLiteDatabase db = DatabaseHelper.getWritableDatabase(mContext);
                ContentValues values = TransactionLogSql.set(null, currentMediaID, fileFullPath, modificationDate,
                        mediaTransactionLogEntryType, commandData);
                db.insert(TransactionLogSql.TABLE, null, values);
            }
        }
    }
}


package de.k3b.android.util;

import android.app.Activity;
import android.app.AlertDialog;
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
import de.k3b.android.androFotoFinder.directory.DirectoryPickerFragment;
import de.k3b.android.androFotoFinder.queries.FotoSql;
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
    protected void onPostProcess(String[] paths, int modifyCount, int itemCount, int opCode) {
        super.onPostProcess(paths, modifyCount, itemCount, opCode);

        if (opCode != OP_DELETE) {
            updateMediaDatabase(paths);
        }
    }

    public void updateMediaDatabase(String... pathNames) {
        SelectedFotos deletedItems = new SelectedFotos();
        MediaScannerConnection.scanFile(
                mContext,
                pathNames, // mPathNames.toArray(new String[mPathNames.size()]),
                null, null);
    }

    public boolean onOptionsItemSelected(final MenuItem item, final SelectedFotos selectedFileNames) {
        if ((selectedFileNames != null) && (selectedFileNames.size() > 0)) {
            // Handle item selection
            switch (item.getItemId()) {
                case R.id.cmd_delete:
                    return cmdDeleteFileWithQuestion(selectedFileNames);
                case R.id.cmd_copy:
                    return cmdMoveOrCopyWithDestDirPicker(false, selectedFileNames);
                case R.id.cmd_move:
                    return cmdMoveOrCopyWithDestDirPicker(true, selectedFileNames);
            }
        }
        return false;
    }

    private boolean cmdMoveOrCopyWithDestDirPicker(final boolean move, final SelectedFotos fotos) {
        DirectoryPickerFragment destDir = new DirectoryPickerFragment() {
            @Override
            protected void onDirectoryPick(IDirectory selection) {
                // super.onDirectoryPick(selection);
                dismiss();

                if (selection != null) {
                    String copyToPath = selection.getAbsolute();
                    setLastCopyToPath(copyToPath);
                    File destDirFolder = new File(copyToPath);

                    String[] selectedFileNames = fotos.getFileNames();
                    Long[] ids = (move) ? fotos.getIds() : null;
                    moveOrCopyFilesTo(move, destDirFolder, SelectedFotos.getFiles(selectedFileNames));

                    if (move) {
                        // remove from media database after successfull move
                        File[] sourceFiles = SelectedFotos.getFiles(selectedFileNames);
                        for (int i = 0; i < sourceFiles.length; i++) {
                            File sourceFile = sourceFiles[i];
                            if (!sourceFile.exists()) {
                                onMediaDeleted(sourceFile.getAbsolutePath(), ids[i]);
                            }
                        }
                    }
                }
            }
        };
        String copyToPath = getLastCopyToPath();

        destDir.defineDirectoryNavigation(new OSDirectory("/", null), FotoSql.QUERY_TYPE_GROUP_COPY, copyToPath);
        destDir.show(this.mContext.getFragmentManager(), "osdir");
        return false;
    }

    @NonNull
    private String getLastCopyToPath() {
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
        String[] pathNames = fotos.getFileNames();
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
                                deleteFiles(fotos);
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
        return true;
    }

    private int deleteFiles(SelectedFotos fotos) {
        int result = 0;
        String[] fileNames = fotos.getFileNames();
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

    private void onMediaDeleted(String absolutePath, Long id) {
        Uri uri = SelectedFotos.getUri(id);
        mContext.getContentResolver().delete(uri,null, null);
        log("rem deleted '" + absolutePath +
                "' as content: " , uri.toString());
    }
}

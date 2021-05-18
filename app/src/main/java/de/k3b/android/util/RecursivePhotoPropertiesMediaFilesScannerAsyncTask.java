/*
 * Copyright (c) 2015-2021 by k3b.
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
import android.os.AsyncTask;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.tagDB.TagSql;
import de.k3b.io.IProgessListener;
import de.k3b.io.filefacade.FileFacade;
import de.k3b.io.filefacade.IFile;
import de.k3b.media.PhotoPropertiesUtil;

import static de.k3b.android.androFotoFinder.queries.FotoSql.getMediaDBApi;

/**
 * Special PhotoPropertiesMediaFilesScanner that can only handle inserNew/updateExisting for directories or jp(e)g files.
 * <p>
 * Can handle pause/resume scanning after a directory was scanned before
 * continuing scanning other dirs.
 * <p>
 * Timer based update-Status-Dialog
 * <p>
 * Created by k3b on 22.10.2015.
 */
public class RecursivePhotoPropertiesMediaFilesScannerAsyncTask extends PhotoPropertiesMediaFilesScannerAsyncTask {
    /**
     * Either
     * - current running scanner instance
     * - or reumable instanc
     */
    public static RecursivePhotoPropertiesMediaFilesScannerAsyncTask sScanner = null;

    private final boolean fullScan;
    private final boolean rescanNeverScannedByAPM;
    private final boolean scanForDeleted;

    // statistics displayed in the status dialog
    protected String mCurrentFolder = "";
    protected int mFileCount = 0;
    /**
     * if not null scanner is either
     * - in resume mode (can be started without parameters to resume interrupted scan)
     * - or in pausing mode collecting all canceled scans here to be processed in resumeIfNecessary()
     */
    protected List<IFile> mPaused = null;

    private AlertDialog mStatusDialog = null;
    private Handler mTimerHandler = null;
    private Runnable mTimerRunner = null;
    private int mDirCount = 0;

    public RecursivePhotoPropertiesMediaFilesScannerAsyncTask(
            PhotoPropertiesMediaFilesScanner scanner, Context context, String why,
            boolean fullScan, boolean rescanNeverScannedByAPM, boolean scanForDeleted, IProgessListener progessListener) {
        super(scanner, context, why, progessListener);

        this.fullScan = fullScan;
        this.rescanNeverScannedByAPM = rescanNeverScannedByAPM;
        this.scanForDeleted = scanForDeleted;

    }

    @Override
    protected Integer doInBackground(IFile[]... pathNames) {
        // do not call super.doInBackground here because logic is different
        int resultCount = 0;
        this.withTransaction = false;
        try {
            getMediaDBApi().beginTransaction();

            FileDirectoryMediaCollector fileDirectoryMediaCollector = new FileDirectoryMediaCollector();
            for (IFile[] pathArray : pathNames) {
                if (pathArray != null) {
                    for (IFile pathName : pathArray) {
                        resultCount += fileDirectoryMediaCollector.scanRootDir(pathName);
                    }
                }
            }
            TagSql.fixAllPrivate();
            getMediaDBApi().setTransactionSuccessful();
        } finally {
            getMediaDBApi().endTransaction();
        }
        return resultCount;
    }

    /**
     * find media files by traversing File-(Sub-)Directories
     */
    private class FileDirectoryMediaCollector {
        private int scanRootDir(IFile rootPath) {
            int resultCount = 0;
            if ((rootPath != null) && (rootPath.length() > 0)) {
                if (rescanNeverScannedByAPM) {
                    List<String> pathsToUpdate = TagSql.getPhotosNeverScanned(rootPath.getAbsolutePath());
                    for (String pathToUpdate : pathsToUpdate) {
                        resultCount += scanDirOrFile(FileFacade.convert("scanRoot incremental paths never scanned ", pathToUpdate));
                    }
                }

                resultCount += scanDirOrFile(rootPath);
            }
            return resultCount;
        }

        private int scanDirOrFile(IFile file) {
            int resultCount = 0;
            final String fullFilePath = file.getAbsolutePath();
            if (fullFilePath != null) {
                if (!isCancelled()) {
                    if (file.isDirectory()) {
                        resultCount += scanDir(file, fullFilePath);
                    } else if (PhotoPropertiesUtil.isImage(file.getName(), PhotoPropertiesUtil.IMG_TYPE_ALL)) {
                        resultCount += runScanner(fullFilePath, file);
                    }
                } else if (mPaused != null) {
                    mPaused.add(file);
                }
            }
            return resultCount;
        }

        private int scanDir(IFile file, String fullFilePath) {
            int resultCount = 0;
            if (scanForDeleted) {
                List<String> deletedPaths = null;
                List<String> existing = FotoSql.getPathsOfFolderWithoutSubfolders(file.getAbsolutePath());
                for (String candidatePath : existing) {
                    IFile camdidateFile = FileFacade.convert(
                            "RecursivePhotoPropertiesMediaFilesScannerAsyncTask.scanDir find deleted", candidatePath);
                    // delete in db for existing but not found as file
                    if (!camdidateFile.exists()) {
                        if (deletedPaths == null) deletedPaths = new ArrayList<>();
                        deletedPaths.add(candidatePath);
                    }
                }
                if (deletedPaths != null) {
                    FotoSql.deleteMedia("del photos that did not exist any more ",
                            deletedPaths, true);
                    resultCount += deletedPaths.size();
                }
            }

            if (fullScan) {
                IFile[] childFileNames = file.listIFiles();

                if (childFileNames != null) {
                    resultCount += runScanner(fullFilePath, childFileNames);
                }
            }

            if (fullScan || scanForDeleted) {
                IFile[] subDirs = file.listIDirs();

                if (subDirs != null) {
                    // #33
                    for (IFile subDir : subDirs) {
                        if ((subDir != null) && (subDir.isDirectory()) && (!subDir.getName().startsWith("."))) {
                            resultCount += scanDirOrFile(subDir);
                        }
                    }
                }
            }
            return resultCount;
        }
    }

    /** @return true if scanner was resumable and started resume operation. */
    public boolean resumeIfNeccessary() {
        if ((getStatus() == AsyncTask.Status.PENDING) && (mPaused != null)) {
            execute(mPaused.toArray(new IFile[mPaused.size()]));
            mPaused = null;
            return true;
        }
        return false;
    }

    private static void createScannerTask(String why, PhotoPropertiesMediaFilesScanner scanner, boolean fullScan, boolean rescanNeverScannedByAPM, boolean scanForDeleted, List<IFile> mPaused, IProgessListener progessListener) {
        RecursivePhotoPropertiesMediaFilesScannerAsyncTask newScanner = ScannerTaskFactory.createScannerTask(why, scanner, fullScan, rescanNeverScannedByAPM, scanForDeleted, progessListener);
        newScanner.mPaused = mPaused;
        RecursivePhotoPropertiesMediaFilesScannerAsyncTask.sScanner = newScanner;
    }

    @Override
    protected void onPostExecute(Integer modifyCount) {
        super.onPostExecute(modifyCount);
        if (isCancelled()) {
            handleScannerCancel();
        } else {
            onStatusDialogEnd(null, false);
            if (sScanner == this) {
                sScanner = null;
            }
        }

    }

    /**
     * call the original background scanner and update the statistics
     */
    protected Integer runScanner(String parentPath, IFile... fileNames) {
        this.mCurrentFolder = parentPath;
        this.mDirCount++;
        onProgress(mFileCount, mDirCount, parentPath);
        final Integer resultCount = super.doInBackground(null, fileNames);
        if (resultCount != null) {
            this.mFileCount += resultCount.intValue();
        }
        return resultCount;
    }

    private void handleScannerCancel() {
        final boolean mustCreateResumeScanner = (mPaused != null) &&
                ((this == RecursivePhotoPropertiesMediaFilesScannerAsyncTask.sScanner)
                        || (null == RecursivePhotoPropertiesMediaFilesScannerAsyncTask.sScanner));
        onStatusDialogEnd(mPaused, false);

        if (sScanner == this) {
            sScanner = null;
        }

        if (mustCreateResumeScanner) {

            RecursivePhotoPropertiesMediaFilesScannerAsyncTask newScanner;

            createScannerTask("resume " + mWhy, mScanner, fullScan, rescanNeverScannedByAPM, scanForDeleted, mPaused, progessListener);
        }
    }

    @Override
    protected void onCancelled(Integer result) {
        // super.onCancelled();
        handleScannerCancel();
    }

    /** returns null if scanner is not busy-active */
    public static RecursivePhotoPropertiesMediaFilesScannerAsyncTask getBusyScanner() {
        if ((RecursivePhotoPropertiesMediaFilesScannerAsyncTask.sScanner != null) && (RecursivePhotoPropertiesMediaFilesScannerAsyncTask.sScanner.getStatus() == Status.RUNNING)) {
            return RecursivePhotoPropertiesMediaFilesScannerAsyncTask.sScanner;
        }
        return null;
    }

    public void showStatusDialog(final Activity parent) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(parent);
        builder.setTitle(R.string.scanner_menu_title);
        View content = parent.getLayoutInflater().inflate(R.layout.dialog_scanner_status, null);

        final TextView folder = (TextView) content.findViewById(R.id.folder);
        final TextView count = (TextView) content.findViewById(R.id.count);

        builder.setView(content);
        builder.setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onStatusDialogEnd(null, true);
            }
        });
        builder.setNegativeButton(R.string.btn_pause, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onStatusDialogEnd(new ArrayList<IFile>(), true);
            }
        });

        builder.setPositiveButton(R.string.background, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onStatusDialogEnd(null, false);
            }
        });
        mStatusDialog = builder.create();
        mStatusDialog.show();

        mTimerHandler = new Handler();
        mTimerRunner = new Runnable() {
            @Override
            public void run() {
                if (mStatusDialog != null) {
                    RecursivePhotoPropertiesMediaFilesScannerAsyncTask scanner = RecursivePhotoPropertiesMediaFilesScannerAsyncTask.this;
                    folder.setText(scanner.mCurrentFolder);
                    count.setText(parent.getString(R.string.image_loading_at_position_format, scanner.mFileCount));
                    if (scanner.mTimerRunner != null) {
                        mTimerHandler.postDelayed(scanner.mTimerRunner, 500); // e.g. 500 milliseconds
                    }
                }
            }
        };
        this.mTimerRunner.run();
    }

    private void onStatusDialogEnd(List<IFile> pauseState, boolean cancelScanner) {
        // no more timer gui updates
        if ((mTimerRunner != null) && (mTimerHandler != null)) {
            mTimerHandler.removeCallbacks(mTimerRunner);
            mTimerHandler = null;
            mTimerRunner = null;
        }

        if (mStatusDialog != null) {
            mStatusDialog.dismiss();
            mStatusDialog = null;
        }

        if (cancelScanner) {
            mPaused = pauseState;
            cancel(false);

            if (RecursivePhotoPropertiesMediaFilesScannerAsyncTask.sScanner == this) {
                RecursivePhotoPropertiesMediaFilesScannerAsyncTask.sScanner = null;
            }
        }
    }
}

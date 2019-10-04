/*
 * Copyright (c) 2018-2019 by k3b.
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

package de.k3b.android.androFotoFinder.backup;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.Serializable;
import java.net.URLDecoder;
import java.util.Date;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.util.IntentUtil;
import de.k3b.android.widget.LocalizedActivity;
import de.k3b.zip.IZipConfig;
import de.k3b.zip.LibZipGlobal;
import de.k3b.zip.ZipConfigDto;
import de.k3b.zip.ZipStorage;
import de.k3b.zip.ZipStorageFile;

/**
 * #108: Showing progress while backup/compression-to-zip is executed
 */
public class BackupProgressActivity extends LocalizedActivity {
    /**
     * document tree supported since andrid-5.0. For older devices use folder picker
     */
    public static final boolean USE_DOCUMENT_PROVIDER = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);

    private static final String EXTRA_STATE_ZIP_CONFIG = "zip_config";
    private static String mDebugPrefix = "BuProgressActivity: ";

    // != null while async backup is running
    private static BackupAsyncTask backupAsyncTask = null;
    private static Date backupDate = null;

    private IZipConfig mZipConfigData = null;

    /**
     * Shows Activity to display progress in backup processing
     */
    public static void showActivity(Activity context,
                                    IZipConfig config, int requestCode) {
        final Intent intent = new Intent().setClass(context,
                BackupProgressActivity.class);

        intent.setClass(context, BackupProgressActivity.class);
        intent.putExtra(EXTRA_STATE_ZIP_CONFIG, (Serializable) config);
        if (LibZipGlobal.debugEnabled) {
            Log.d(LibZipGlobal.LOG_TAG, mDebugPrefix + context.getClass().getSimpleName()
                    + " > BackupProgressActivity.showActivity " + intent.toUri(Intent.URI_INTENT_SCHEME));
        }

        IntentUtil.startActivity(mDebugPrefix, context, requestCode, intent);
    }

    public static ZipStorage getCurrentStorage(Context context, String zipDir, String baseFileName) {
        if (USE_DOCUMENT_PROVIDER) {
            DocumentFile docDir = getDocFile(context, zipDir);
            return new de.k3b.android.zip.ZipStorageDocumentFile(context, docDir, baseFileName);

        } else {
            File absoluteZipFile = new File(zipDir, baseFileName);
            return new ZipStorageFile(absoluteZipFile.getAbsolutePath());
        }
    }

/*
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }
*/

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static DocumentFile getDocFile(Context context, @NonNull String dir) {
        DocumentFile docDir = null;

        if (dir.indexOf(":") >= 0) {
            Uri uri = Uri.parse(dir);

            if ("file".equals(uri.getScheme())) {
                File fileDir = new File(uri.getPath());
                docDir = DocumentFile.fromFile(fileDir);
            } else {
                docDir = DocumentFile.fromTreeUri(context, uri);
            }
        } else {
            docDir = DocumentFile.fromFile(new File(dir));
        }
        return docDir;

    }

/*
    @Override
    protected void onPause() {
        setBackupAsyncTaskProgessReceiver(null);
        super.onPause();
    }
*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Global.debugMemory(mDebugPrefix, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_progress);

        Intent intent = getIntent();

        mZipConfigData = (IZipConfig) intent.getSerializableExtra(EXTRA_STATE_ZIP_CONFIG);

        if (backupAsyncTask == null) {
            backupDate = new Date();
            final String zipDir = mZipConfigData.getZipDir();
            final String zipName = ZipConfigDto.getZipFileName(mZipConfigData, backupDate);
            ZipStorage zipStorage = getCurrentStorage(this, zipDir, zipName);

            backupAsyncTask = new BackupAsyncTask(this, new ZipConfigDto(mZipConfigData), zipStorage,
                    backupDate);
            setBackupAsyncTaskProgessReceiver(mDebugPrefix + "onCreate create backupAsyncTask ", this);
            backupAsyncTask.execute();
        }

        final TextView lblContext = (TextView) findViewById(R.id.lbl_context);

        String contextMessage = URLDecoder.decode(mZipConfigData.getZipDir() + "/" + ZipConfigDto.getZipFileName(mZipConfigData, backupDate));

        /*
        final StringBuilder contextMessage = StringUtils.appendMessage(null, ZipConfigDto.getZipFileName(mZipConfigData, backupDate), "\n",
                mZipConfigData.getZipDir(), "\n\n"
        );
        */
        lblContext.setText(contextMessage);
    }

    @Override
    protected void onDestroy() {
        setBackupAsyncTaskProgessReceiver(mDebugPrefix + "onDestroy ", null);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        setBackupAsyncTaskProgessReceiver(mDebugPrefix + "onResume ", this);
        Global.debugMemory(mDebugPrefix, "onResume");
        super.onResume();

    }

    /**
     * (Re-)Connects this activity back with static backupAsyncTask
     */
    private void setBackupAsyncTaskProgessReceiver(String why, Activity progressReceiver) {
        boolean isActive = BackupAsyncTask.isActive(backupAsyncTask);
        boolean running = (progressReceiver != null) && isActive;

        String debugContext = why + mDebugPrefix + " setBackupAsyncTaskProgessReceiver isActive=" + isActive +
                ", running=" + running +
                " ";

        if (backupAsyncTask != null) {
            final ProgressBar progressBar = (ProgressBar) this.findViewById(R.id.progressBar);
            final TextView status = (TextView) this.findViewById(R.id.lbl_status);
            final Button buttonCancel = (Button) this.findViewById(R.id.cmd_cancel);

            // setVisibility(running, progressBar, buttonCancel);

            if (running) {
                backupAsyncTask.setContext(debugContext, this, progressBar, status);
                final String _debugContext = debugContext;
                buttonCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (LibZipGlobal.debugEnabled) {
                            Log.d(LibZipGlobal.LOG_TAG, mDebugPrefix + " button Cancel backup pressed initialized by " + _debugContext);
                        }
                        backupAsyncTask.cancel(false);
                        buttonCancel.setVisibility(View.INVISIBLE);
                    }
                });

            } else {
                backupAsyncTask.setContext(debugContext, null, null, null);
                buttonCancel.setOnClickListener(null);
                if (!isActive) {
                    backupAsyncTask = null;
                }
            }
        }
    }
}


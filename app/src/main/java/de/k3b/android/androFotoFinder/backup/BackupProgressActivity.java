/*
 * Copyright (c) 2018-2020 by k3b.
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
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.Serializable;
import java.net.URLDecoder;
import java.util.Date;

import de.k3b.android.androFotoFinder.R;
import de.k3b.android.util.IntentUtil;
import de.k3b.android.widget.ProgressActivity;
import de.k3b.android.widget.ProgressableAsyncTask;
import de.k3b.zip.IZipConfig;
import de.k3b.zip.LibZipGlobal;
import de.k3b.zip.ZipConfigDto;
import de.k3b.zip.ZipStorage;
import de.k3b.zip.ZipStorageFile;

/**
 * #108: Showing progress while backup/compression-to-zip is executed
 */
public class BackupProgressActivity extends ProgressActivity<IZipConfig> {
    /**
     * document tree supported since andrid-5.0. For older devices use folder picker
     */
    public static final boolean USE_DOCUMENT_PROVIDER = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);

    protected static final String EXTRA_STATE_ZIP_CONFIG = "zip_config";
    private static final String mDebugPrefix = "BuProgressActivity: ";

    // != null while async backup is running
    private static ProgressableAsyncTask<IZipConfig> asyncTask = null;
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

    @Override
    protected ProgressableAsyncTask<IZipConfig> getAsyncTask() {
        return asyncTask;
    }

    @Override
    protected void setAsyncTask(ProgressableAsyncTask<IZipConfig> asyncTask) {
        BackupProgressActivity.asyncTask = asyncTask;
    }

    /*
    @Override
    protected void onPause() {
        setBackupAsyncTaskProgessReceiver(null);
        super.onPause();
    }
*/

    @Override
    protected void onCreateEx(Bundle savedInstanceState) {
        setContentView(R.layout.activity_backup_progress);

        Intent intent = getIntent();

        mZipConfigData = (IZipConfig) intent.getSerializableExtra(EXTRA_STATE_ZIP_CONFIG);

        if (getAsyncTask() == null) {
            backupDate = new Date();
            final String zipDir = mZipConfigData.getZipDir();
            final String zipName = ZipConfigDto.getZipFileName(mZipConfigData, backupDate);
            ZipStorage zipStorage = getCurrentStorage(this, zipDir, zipName);

            setAsyncTask(new BackupAsyncTask(this, new ZipConfigDto(mZipConfigData), zipStorage,
                    backupDate));
            setAsyncTaskProgessReceiver(mDebugPrefix + "onCreate create asyncTask ", this);
            getAsyncTask().execute();
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

}


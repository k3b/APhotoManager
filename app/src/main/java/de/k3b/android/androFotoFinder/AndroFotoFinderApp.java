/*
 * Copyright (c) 2015-2017 by k3b.
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
 
package de.k3b.android.androFotoFinder;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import org.osmdroid.api.IMapView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import de.k3b.LibGlobal;
import de.k3b.android.GuiUtil;
import de.k3b.android.androFotoFinder.imagedetail.HugeImageLoader;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.queries.FotoSqlBase;
import de.k3b.android.osmdroid.forge.MapsForgeSupport;
import de.k3b.android.util.LogCat;
import de.k3b.database.QueryParameter;
import de.k3b.io.PhotoAutoprocessingDto;
import de.k3b.media.ExifInterface;
import de.k3b.media.ImageMetaReader;
import uk.co.senab.photoview.PhotoViewAttacher;
import uk.co.senab.photoview.gestures.CupcakeGestureDetector;

// import com.squareup.leakcanary.LeakCanary;
// import com.squareup.leakcanary.RefWatcher;

/**
 * Created by k3b on 14.07.2015.
 */
public class AndroFotoFinderApp extends Application {
    private static String fileNamePrefix = "androFotofinder.logcat-";

    private LogCat mCrashSaveToFile = null;


    /*
        private RefWatcher refWatcher;

        public static RefWatcher getRefWatcher(Context context) {
            AndroFotoFinderApp application = (AndroFotoFinderApp) context.getApplicationContext();
            return application.refWatcher;
        }

        @Override public void onCreate() {
            super.onCreate();
            LeakCanary.install(this);
        }
        */
    @Override public void onCreate() {
        // StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyDialog().build());
        // StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyDeath().build());
        FotoSqlBase.init();

        super.onCreate();

        LibGlobal.appName = getString(R.string.app_name);
        LibGlobal.appVersion = GuiUtil.getAppVersionName(this);

        Global.pickHistoryFile = getDatabasePath("pickHistory.geouri.txt");
        SettingsActivity.prefs2Global(this);

        // create sensible defaults for andorid-independant files from android string resources
        QueryParameter.sFileComment = getBookMarkComment(this);
        PhotoAutoprocessingDto.sFileComment = getBookMarkComment(this);

        QueryParameter.sParserDefaultFrom = FotoSql.SQL_TABLE_EXTERNAL_CONTENT_URI_FILE.toString();
        QueryParameter.sParserDefaultQueryTypeId = FotoSql.QUERY_TYPE_DEFAULT;
        QueryParameter.sParserDefaultSelect = new ArrayList<String>();
        Collections.addAll(QueryParameter.sParserDefaultSelect, FotoSql.DEFAULT_GALLERY_COLUMNS);
        mCrashSaveToFile = new LogCat(Global.LOG_CONTEXT, HugeImageLoader.LOG_TAG,
                PhotoViewAttacher.LOG_TAG, CupcakeGestureDetector.LOG_TAG,
                LibGlobal.LOG_TAG, ThumbNailUtils.LOG_TAG, IMapView.LOGTAG,
                ExifInterface.LOG_TAG, ImageMetaReader.LOG_TAG) {

            public void saveToFile() {
                final File logFile = getOutpuFile();
                String message = (logFile != null)
                        ? "saving errorlog ('LocCat') to " + logFile.getAbsolutePath()
                        : "Saving errorlog ('LocCat') is disabled. See Settings 'Diagnostics' for details";
                Log.e(Global.LOG_CONTEXT, message);
                Toast.makeText(AndroFotoFinderApp.this , message, Toast.LENGTH_LONG).show();

                saveLogCat(logFile, null, mTags);
            }

            private File getOutpuFile() {
                File logDirectory = Global.logCatDir;
                if (logDirectory == null) return null;

                // Datetime as part of the crash-log-filename
                // i.e. /mnt/sdcard/copy/log/androFotofinder.logcat-20160509-195217.txt

                File logFile = new File(logDirectory,
                        getLocalLogFileName(fileNamePrefix));

                // create log folder
                logDirectory.mkdirs();

                return logFile;
            }


        };

        ThumbNailUtils.init(this, null);

        //https://github.com/osmdroid/osmdroid/issues/366
        //super important. Many tile servers, including open street maps, will BAN applications by user
        //??? OpenStreetMapTileProviderConstants.setUserAgentValue(getAppId() + " https://github.com/k3b/APhotoManager"); // BuildConfig.APPLICATION_ID);

        // #60: configure some of the mapsforge settings first
        MapsForgeSupport.createInstance(this);

        FotoSql.deleteMediaWithNullPath(this);

        Log.i(Global.LOG_CONTEXT, getAppId() + " created");
    }

    public static String getBookMarkComment(Context context) {
        return context.getString(R.string.bookmark_file_comment_format,
                context.getString(R.string.app_name),
                GuiUtil.getAppVersionName(context),
                new Date().toString());
    }

    @NonNull
    private String getAppId() {
        return getString(R.string.app_name) + " " + GuiUtil.getAppVersionName(this);
    }

    @Override
    public void onTerminate() {
        Log.i(Global.LOG_CONTEXT, getAppId() + " terminated");
        if (mCrashSaveToFile != null) {
            mCrashSaveToFile.close();
        }
        mCrashSaveToFile = null;
        super.onTerminate();
    }

    public void saveToFile() {
        if (mCrashSaveToFile != null) {
            mCrashSaveToFile.saveToFile();
        }
    }
    public void clear() {
        if (mCrashSaveToFile != null) {
            mCrashSaveToFile.clear();
        }
    }
}

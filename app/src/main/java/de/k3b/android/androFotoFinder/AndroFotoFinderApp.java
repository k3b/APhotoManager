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
 
package de.k3b.android.androFotoFinder;

import android.app.Application;
import android.support.annotation.NonNull;
import android.util.Log;

import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import de.k3b.FotoLibGlobal;
import de.k3b.android.GuiUtil;
import de.k3b.android.androFotoFinder.imagedetail.HugeImageLoader;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.util.LogCat;
import de.k3b.database.QueryParameter;
import uk.co.senab.photoview.PhotoViewAttacher;
import uk.co.senab.photoview.gestures.CupcakeGestureDetector;

// import com.squareup.leakcanary.LeakCanary;
// import com.squareup.leakcanary.RefWatcher;

/**
 * Created by k3b on 14.07.2015.
 */
public class AndroFotoFinderApp extends Application {
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

        super.onCreate();

        Global.pickHistoryFile = getDatabasePath("pickHistory.geouri.txt");
        SettingsActivity.prefs2Global(this);

        // create sensible defaults for domain-independant QueryParameter parsing
        QueryParameter.sParserComment = getString(R.string.bookmark_file_comment_format,
                getString(R.string.app_name),
                GuiUtil.getAppVersionName(this),
                new Date().toString());

        QueryParameter.sParserDefaultFrom = FotoSql.SQL_TABLE_EXTERNAL_CONTENT_URI.toString();
        QueryParameter.sParserDefaultQueryTypeId = FotoSql.QUERY_TYPE_DEFAULT;
        QueryParameter.sParserDefaultSelect = new ArrayList<String>();
        Collections.addAll(QueryParameter.sParserDefaultSelect, FotoSql.DEFAULT_GALLERY_COLUMNS);
        mCrashSaveToFile = new LogCat(this, Global.LOG_CONTEXT, HugeImageLoader.LOG_TAG,
                PhotoViewAttacher.LOG_TAG, CupcakeGestureDetector.LOG_TAG,
                FotoLibGlobal.LOG_TAG, ThumbNailUtils.LOG_TAG);

        ThumbNailUtils.init(this, null);

        //https://github.com/osmdroid/osmdroid/issues/366
        //super important. Many tile servers, including open street maps, will BAN applications by user
        OpenStreetMapTileProviderConstants.setUserAgentValue(getAppId() + " https://github.com/k3b/APhotoManager"); // BuildConfig.APPLICATION_ID);

        Log.i(Global.LOG_CONTEXT, getAppId() + " created");
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

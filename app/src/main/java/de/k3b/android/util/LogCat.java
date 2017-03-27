/*
 * Copyright (c) 2015-2016 by k3b.
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

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import de.k3b.android.androFotoFinder.Global;

/**
 * UncaughtException writes apps own logcat content to logfile, if Global.logCatDir is defined.
 *
 * Created by k3b on 04.11.2015.
 */
public class LogCat implements Thread.UncaughtExceptionHandler {
    private Thread.UncaughtExceptionHandler mPreviousUncaughtExceptionHandler;
    private final Context mAppContext;
    private final String[] mTags;

    // Datetime as part of the crash-log-filename
    // inspired by http://stackoverflow.com/questions/36617172/android-mediascanner-in-uncaughtexceptionhandler-not-scanning-file
    private static DateFormat fmtDateTime2String = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT);

    public LogCat(Context appContext, String... tags) {
        fmtDateTime2String.setTimeZone(TimeZone.getTimeZone("UTC"));

        mAppContext = appContext;
        this.mTags = tags;
        mPreviousUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public void saveToFile() {
        saveToFile(mAppContext, mTags);
    }

    private static void saveToFile(Context context, String... tags) {
        File logDirectory = Global.logCatDir;

        // Datetime as part of the crash-log-filename
        // i.e. /mnt/sdcard/copy/log/androFotofinder.logcat-20160509-195217.txt
        File logFile = (logDirectory == null) ? null : new File(logDirectory,
                            "androFotofinder.logcat-"
                                    + fmtDateTime2String.format(new Date(System.currentTimeMillis()))
                                    + ".txt");
        String message = (logFile != null)
                ? "saving errorlog ('LocCat') to " + logFile.getAbsolutePath()
                : "Saving errorlog ('LocCat') is disabled. See Settings 'Diagnostics' for details";
        Log.e(Global.LOG_CONTEXT, message);
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }

        if (logDirectory != null) {

            // create log folder
            logDirectory.mkdirs();

            StringBuilder cmdline = new StringBuilder();

            // see http://developer.android.com/tools/debugging/debugging-log.html#filteringOutput
            cmdline.append("logcat -d -v long -f ").append(logFile).append(" -s ");
            for (String tag : tags) {
                cmdline.append(tag).append(":D ");
            }
            // clear the previous logcat and then write the new one to the file
            try {
                Runtime.getRuntime().exec(cmdline.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void clear() {
        clear(mAppContext, mTags);
    }

    private static void clear(Context context, String... tags) {
        StringBuilder cmdline = new StringBuilder();

        // see http://developer.android.com/tools/debugging/debugging-log.html#filteringOutput
        cmdline.append("logcat -c ");
        for (String tag : tags) {
            cmdline.append(tag).append(":D ");
        }
        try {
            Process // process = Runtime.getRuntime().exec( "logcat -c");
                    process = Runtime.getRuntime().exec(cmdline.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {

        try {
            // Do your stuff with the exception
            Log.e(Global.LOG_CONTEXT,"LogCat.uncaughtException " + ex, ex);
            saveToFile();
        } catch (Exception e) {
            /* Ignore */
        } finally {
            // Let Android show the default error dialog
            mPreviousUncaughtExceptionHandler.uncaughtException(thread, ex);
        }
    }

    public void close() {
        if (mPreviousUncaughtExceptionHandler != null) {
            Thread.setDefaultUncaughtExceptionHandler(mPreviousUncaughtExceptionHandler);
            mPreviousUncaughtExceptionHandler = null;
        }
    }
}

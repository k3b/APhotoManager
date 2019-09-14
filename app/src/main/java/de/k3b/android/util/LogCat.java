/*
 * Copyright (c) 2015-2019 by k3b.
 *
 * This file is part of AndroFotoFinder / #APhotoManager / ToGoZip.
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

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import de.k3b.io.FileUtils;

/**
 * UncaughtException writes apps own logcat content to logfile, if Global.logCatDir is defined.
 *
 * Created by k3b on 04.11.2015.
 */
public abstract class LogCat implements Thread.UncaughtExceptionHandler {
    private Thread.UncaughtExceptionHandler mPreviousUncaughtExceptionHandler;
    protected final String[] mTags;

    // Datetime as part of the crash-log-filename
    // inspired by http://stackoverflow.com/questions/36617172/android-mediascanner-in-uncaughtexceptionhandler-not-scanning-file
    private static DateFormat fmtDateTime2String = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT);

    public LogCat(String... tags) {
        fmtDateTime2String.setTimeZone(TimeZone.getTimeZone("UTC"));

        if ((tags == null) || (tags.length < 1)) {
            throw new IllegalArgumentException("LogCat must have at least one loggger name.");
        }
        this.mTags = tags;
        mPreviousUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public abstract void saveToFile();

    protected void saveLogCat(File logFile, OutputStream outputStream, String[] tags) {
        StringBuilder cmdline = new StringBuilder();

        // see http://developer.android.com/tools/debugging/debugging-log.html#filteringOutput
        cmdline.append("logcat -d -v long");
        if (logFile != null) {
            cmdline.append(" -f ").append(logFile);
        }
        cmdline.append(" -s ");
        for (String tag : tags) {
            cmdline.append(tag).append(":D ");
        }

        // clear the previous logcat and then write the new one to the file
        try {
            Process process = Runtime.getRuntime().exec(cmdline.toString());
            if (outputStream != null) {
                final InputStream inputStream = process.getInputStream();
                if (inputStream != null) {
                    FileUtils.copy(inputStream, outputStream);
                    inputStream.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected String getLocalLogFileName(String fileNamePrefix) {
        return fileNamePrefix
                + fmtDateTime2String.format(new Date(System.currentTimeMillis()))
                + ".txt";
    }

    public void clear() {
        clear(mTags);
    }

    private static void clear(String... tags) {
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

    /**
     * Thread.UncaughtExceptionHandler.uncaughtException is called when an app component is crashing:
     * there is an exception that is not handled by the app
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        try {
            // Do your stuff with the exception
            Log.e(mTags[0],"LogCat.uncaughtException " + ex, ex);
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

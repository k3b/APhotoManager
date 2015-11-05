package de.k3b.android.util;

import android.os.Environment;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * Created by k3b on 04.11.2015.
 */
public class LogCat implements Thread.UncaughtExceptionHandler {
    private Thread.UncaughtExceptionHandler mPreviousUncaughtExceptionHandler;
    private final String[] mTags;

    public LogCat(String... tags) {
        this.mTags = tags;
        mPreviousUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public static void saveToFile(String... tags) {
        File appDirectory = new File(Environment.getExternalStorageDirectory() + "/copy");
        File logDirectory = new File(appDirectory + "/log");
        File logFile = new File(logDirectory, "androFotofinder.logcat" + System.currentTimeMillis() + ".txt");

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
            saveToFile(mTags);
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

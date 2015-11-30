package de.k3b.android.util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

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

    public LogCat(Context appContext, String... tags) {
        mAppContext = appContext;
        this.mTags = tags;
        mPreviousUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public static void saveToFile(Context context, String... tags) {
        File logDirectory = Global.logCatDir;
        File logFile = (logDirectory == null) ? null : new File(logDirectory, "androFotofinder.logcat" + System.currentTimeMillis() + ".txt");
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
                Process // process = Runtime.getRuntime().exec( "logcat -c");
                        process = Runtime.getRuntime().exec(cmdline.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {

        try {
            // Do your stuff with the exception
            Log.e(Global.LOG_CONTEXT,"LogCat.uncaughtException " + ex, ex);
            saveToFile(mAppContext, mTags);
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

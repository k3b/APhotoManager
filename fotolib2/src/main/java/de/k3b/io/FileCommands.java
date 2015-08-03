package de.k3b.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Created by k3b on 03.08.2015.
 */
public class FileCommands implements  Cloneable {

    // private static final String LOG_FILE_ENCODING = "UTF-8";
    private PrintWriter mLogFile;

    public FileCommands(String logFilePath) {
        mLogFile = null;

        if (logFilePath != null) {
            OutputStream stream = null;
            try {
                File logFile = new File(logFilePath);
                if (logFile.exists()) {
                    // open existing in append mode
                    stream = new FileOutputStream(logFile, true);
                    mLogFile = new PrintWriter(stream, true);
                } else {
                    // create new
                    mLogFile = new PrintWriter(logFile, "UTF-8");
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    }

    public void deleteFile(String... paths) {
        for(String path : paths) {
            File file = new File(path);
            if (file.exists()) {
                log("rem file exists");
                if (!file.delete()) {
                    log("rem delete failed");
                }
            } else {
                log("rem file does not exist");
            }
            log("del \"" + path + "\"", "");
        }
        onPostProcess(paths);
    }

    /** called for each modified/deleted file */
    protected void onPostProcess(String[] paths) {
    }

    public void log(String... messages) {
        if (mLogFile != null) {
            for(String message : messages) {
                mLogFile.println(message);
            }
            mLogFile.flush();
        }
    }

    public void close() {
        if (mLogFile != null) {
            mLogFile.close();
            mLogFile = null;
        }
    }
}

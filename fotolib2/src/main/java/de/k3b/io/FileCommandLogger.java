/*
 * Copyright (c) 2017 by k3b.
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

package de.k3b.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;

/**
 * Created by k3b on 09.10.2017.
 */

public class FileCommandLogger implements IFileCommandLogger {
    protected String mLogFilePath;
    protected PrintWriter mLogFile;

    public void setLogFilePath(String logFilePath) {
        closeLogFile();
        mLogFilePath = logFilePath;
    }

    public void openLogfile() {
        closeLogFile();
        if (mLogFilePath != null) {
            OutputStream stream = null;
            try {
                File logFile = new File(mLogFilePath);
                if (logFile.exists()) {
                    // open existing in append mode
                    long ageInHours = (new Date().getTime() - logFile.lastModified()) / (1000 * 60 * 60);
                    stream = new FileOutputStream(logFile, true);
                    mLogFile = new PrintWriter(stream, true);

                    if (ageInHours > 15) {
                        log();
                        log("rem ", new Date());
                    }
                } else {
                    // create new
                    mLogFile = new PrintWriter(logFile, "UTF-8");
                    log("rem " , new Date());
                }
            } catch (Throwable e) {
                onException(e, "openLogfile", mLogFilePath);
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e1) {
                        onException(e1, "openLogfile-close", mLogFilePath);
                    }
                }
            }
        }
    }

    public void closeAll() {
        closeLogFile();
    }

    public void closeLogFile() {
        if (mLogFile != null) {
            mLogFile.close();
            mLogFile = null;
        }
    }

    /** called for every cath(Exception...) */
    protected void onException(final Throwable e, Object... context) {
        if (e != null) {
            e.printStackTrace();
        }
    }

    @Override
    public IFileCommandLogger log(Object... messages) {
        if (mLogFile != null) {
            for(Object message : messages) {
                if (message != null) mLogFile.print(message);
            }
            mLogFile.println();
            mLogFile.flush();
        }
        return this;
    }

}

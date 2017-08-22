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
 * Created by k3b on 03.08.2017.
 */

public class FileProcessor {
    private static final String EXT_SIDECAR = ".xmp";
    protected String mLogFilePath;
    // private static final String LOG_FILE_ENCODING = "UTF-8";
    protected PrintWriter mLogFile;

    /** can be replaced by mock/stub in unittests */
    public boolean osFileExists(File file) {
        return file.exists();
    }

    protected boolean fileOrSidecarExists(File file) {
        if (file == null) return false;

        return osFileExists(file) || osFileExists(FileCommands.getSidecar(file, false))  || osFileExists(FileCommands.getSidecar(file, true));
    }
    public static boolean isSidecar(File file) {
        if (file == null) return false;
        return isSidecar(file.getAbsolutePath());
    }

    public static boolean isSidecar(String name) {
        if (name == null) return false;
        return name.toLowerCase().endsWith(EXT_SIDECAR);
    }

    public static File getSidecar(File file, boolean longFormat) {
        if (file == null) return null;
        return getSidecar(file.getAbsolutePath(), longFormat);
    }

    public static File getSidecar(String absolutePath, boolean longFormat) {
        File result;
        if (longFormat) {
            result = new File(absolutePath + EXT_SIDECAR);
        } else {
            result = new File(FileUtils.replaceExtension(absolutePath, EXT_SIDECAR));
        }
        return result;
    }

    public static File getExistingSidecarOrNull(String absolutePath) {
        File result = null;
        if (absolutePath != null) {
            result = getSidecar(absolutePath, true);
            if ((result == null) || !result.exists() || !result.isFile()) result = getSidecar(absolutePath, false);
            if ((result == null) || !result.exists() || !result.isFile()) result = null;
        }
        return result;
    }
	
    /**
     * @return file if rename is not neccessary else File with new name
     */
    public File renameDuplicate(File file) {
        if (!fileOrSidecarExists(file)) {
            // rename is not neccessary
            return file;
        }


        String filename = file.getAbsolutePath();
        String extension = ")";
        int extensionPosition = filename.lastIndexOf(".");
        if (extensionPosition >= 0) {
            extension = ")" + filename.substring(extensionPosition);
            filename = filename.substring(0, extensionPosition) + "(";
        }
        int id = 0;
        while (true) {
            id++;
            String candidatePath = filename + id + extension;
            File candidate = new File(candidatePath);
            if (!fileOrSidecarExists(candidate)) {
                log("rem renamed from '", filename, "' to '", candidatePath,"'");
                return candidate;
            }

        }
    }

    /** called for every cath(Exception...) */
    protected void onException(final Throwable e, Object... context) {
        if (e != null) {
            e.printStackTrace();
        }
    }

    public void openLogfile() {
        closeLogFile();
        if (mLogFilePath != null) {
            OutputStream stream = null;
            try {
                File logFile = new File(mLogFilePath);
                if (osFileExists(logFile)) {
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

    public void closeLogFile() {
        if (mLogFile != null) {
            mLogFile.close();
            mLogFile = null;
        }
    }

    public FileProcessor log(Object... messages) {
        if (mLogFile != null) {
            for(Object message : messages) {
                mLogFile.print(message);
            }
            mLogFile.println();
            mLogFile.flush();
        }
        return this;
    }
}

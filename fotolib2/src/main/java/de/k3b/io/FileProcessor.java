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

/**
 * Created by k3b on 03.08.2017.
 */

public class FileProcessor extends FileCommandLogger implements IFileCommandLogger {
    private static final String EXT_SIDECAR = ".xmp";

    /** if not null: all logging goes through this */
    private IFileCommandLogger internalLogger = null;

    // private static final String LOG_FILE_ENCODING = "UTF-8";
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

    public static XmpFile getSidecar(String absolutePath, boolean longFormat) {
        XmpFile result;
        if (longFormat) {
            result = new XmpFile(absolutePath + EXT_SIDECAR, longFormat);
        } else {
            result = new XmpFile(FileUtils.replaceExtension(absolutePath, EXT_SIDECAR), longFormat);
        }
        return result;
    }

    public static class XmpFile extends File {
        /** true: file.jpg.xmp; false: file.xmp */
        private final boolean longFormat;
        private boolean hasAlsoOtherFormat = false;

        public XmpFile(String absolutePath, boolean longFormat) {
            super(absolutePath);
            this.longFormat = longFormat;
        }

        /** true: file.jpg.xmp; false: file.xmp */
        public boolean isLongFormat() {
            return longFormat;
        }

        public boolean isHasAlsoOtherFormat() {
            return hasAlsoOtherFormat;
        }

        public void setHasAlsoOtherFormat(boolean hasAlsoOtherFormat) {
            this.hasAlsoOtherFormat = hasAlsoOtherFormat;
        }
    }

    public static XmpFile getExistingSidecarOrNull(String absolutePath) {
        XmpFile result = null;
        if (absolutePath != null) {
            XmpFile resultLong = getExistingSidecarOrNull(absolutePath, true);
            XmpFile resultShort = getExistingSidecarOrNull(absolutePath, false);

            if (resultLong != null) {
                result = resultLong;
                result.setHasAlsoOtherFormat(resultShort != null);
            } else {
                result = resultShort;
            }
        }
        return result;
    }

    public static XmpFile getExistingSidecarOrNull(String absolutePath, boolean longFormat) {
        XmpFile result = getSidecar(absolutePath, longFormat);
        if ((result == null) || !result.exists() || !result.isFile()) return null;
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

    /** if not null: all logging goes through this */
    public IFileCommandLogger getInternalLogger() {
        return internalLogger;
    }

    public void setInternalLogger(IFileCommandLogger internalLogger) {
        this.internalLogger = internalLogger;
    }

    @Override
    public IFileCommandLogger log(Object... messages) {
        if ((internalLogger != null) && (internalLogger != this)) {
            internalLogger.log(messages);
        } else {
            super.log(messages);
        }
        return this;
    }

}

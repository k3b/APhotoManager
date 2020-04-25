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

    /// TODO what is mime for XMP
    private static final String XMP_MINE = "*/*";

    /** if not null: all logging goes through this */
    private IFileCommandLogger internalLogger = null;

    // private static final String LOG_FILE_ENCODING = "UTF-8";

    @Deprecated
    public static boolean isSidecar(File file) {
        if (file == null) return false;
        return isSidecar(file.getName());
    }

    public static boolean isSidecar(IFile file) {
        if (file == null) return false;
        return isSidecar(file.getName());
    }

    @Deprecated
    public static IFile getSidecar(File file, boolean longFormat) {
        return getSidecar(FileFacade.convert(file), longFormat);
    }

    public static IFile getSidecar(IFile file, boolean longFormat) {
        if (file == null) return null;
        String name = file.getName();
        return getSidecar(file.getParentFile(), name, longFormat);
    }

    public static XmpFile getSidecar(IFile parent, String name, boolean longFormat) {
        XmpFile result;
        if (longFormat) {
            result = new XmpFile(FileFacade.getOrCreateChild(parent, name + EXT_SIDECAR, XMP_MINE), longFormat);
        } else {
            result = new XmpFile(FileFacade.getOrCreateChild(parent, FileUtils.replaceExtension(name, EXT_SIDECAR), XMP_MINE), longFormat);
        }
        return result;

    }

    @Deprecated
    public static XmpFile getSidecar(String absolutePath, boolean longFormat) {
        XmpFile result;
        if (longFormat) {
            result = new XmpFile(absolutePath + EXT_SIDECAR, longFormat);
        } else {
            result = new XmpFile(FileUtils.replaceExtension(absolutePath, EXT_SIDECAR), longFormat);
        }
        return result;
    }

    public static boolean isSidecar(String name) {
        if (name == null) return false;
        return name.toLowerCase().endsWith(EXT_SIDECAR);
    }

    /**
     * can be replaced by mock/stub in unittests
     */
    @Deprecated
    public boolean osFileExists(File file) {
        return osFileExists(FileFacade.convert(file));
    }

    public boolean osFileExists(IFile file) {
        return file.exists();
    }

    @Deprecated
    protected boolean fileOrSidecarExists(File file) {
        return fileOrSidecarExists(FileFacade.convert(file));
    }

    protected boolean fileOrSidecarExists(IFile file) {
        if (file == null) return false;

        IFile parent = file.getParentFile();
        String name = file.getName();
        return file.exists() || FileCommands.getSidecar(parent, name, false).exists()
                || FileCommands.getSidecar(parent, name, true).exists();
    }

    @Deprecated
    public File renameDuplicate(File file) {
        return FileFacade.convert(file).getFile();
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
    public IFile renameDuplicate(IFile file) {
        if (!fileOrSidecarExists(file)) {
            // rename is not neccessary
            return file;
        }

        IFile parent = file.getParentFile();

        String mime = file.getMime();
        String filename = file.getName();
        String extension = ")";
        int extensionPosition = filename.lastIndexOf(".");
        if (extensionPosition >= 0) {
            extension = ")" + filename.substring(extensionPosition);
            filename = filename.substring(0, extensionPosition) + "(";
        }
        int id = 0;
        while (true) {
            id++;
            String candidateName = filename + id + extension;
            IFile candidate = parent.create(candidateName, mime);
            if (!fileOrSidecarExists(candidate)) {
                log("rem renamed from '", filename, "' to '", candidateName,"'");
                return candidate;
            }

        }
    }

    public static class XmpFile extends FileWrapper {
        /**
         * true: file.jpg.xmp; false: file.xmp
         */
        private final boolean longFormat;
        private boolean hasAlsoOtherFormat = false;

        public XmpFile(IFile parent, String name, String mime, boolean longFormat) {
            this(FileFacade.getOrCreateChild(parent, name, mime), longFormat);
        }

        @Deprecated
        public XmpFile(String absolutePath, boolean longFormat) {
            this(FileFacade.convert(new File(absolutePath)), longFormat);
        }

        public XmpFile(IFile file, boolean longFormat) {
            super(file);
            this.longFormat = longFormat;
        }

        /**
         * true: file.jpg.xmp; false: file.xmp
         */
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

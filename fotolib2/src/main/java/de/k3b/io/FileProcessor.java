/*
 * Copyright (c) 2017-2020 by k3b.
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

import de.k3b.io.filefacade.IFile;

/**
 * Created by k3b on 03.08.2017.
 */

public class FileProcessor extends FileCommandLogger implements IFileCommandLogger {
    /** if not null: all logging goes through this */
    private IFileCommandLogger internalLogger = null;

    // private static final String LOG_FILE_ENCODING = "UTF-8";

    /**
     * can be replaced by mock/stub in unittests
     */
    public boolean osFileExists(IFile file) {
        return file.exists();
    }

    protected boolean fileOrSidecarExists(IFile file) {
        if (file == null) return false;
        if (osFileExists(file)) return true;

        IFile parent = file.getParentFile();
        String name = file.getName();
        return osFileExists(XmpFile.getSidecar(parent, name, false))
                || osFileExists(XmpFile.getSidecar(parent, name, true));
    }

    /**
     * @return file if rename is not neccessary else IFile with new name
     */
    public IFile renameDuplicate(IFile file) {
        if (!fileOrSidecarExists(file)) {
            // rename is not neccessary
            return file;
        }

        IFile parent = file.getParentFile();

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
            IFile candidate = parent.createFile(candidateName);
            if (!fileOrSidecarExists(candidate)) {
                log("rem renamed from '", filename, "' to '", candidateName,"'");
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

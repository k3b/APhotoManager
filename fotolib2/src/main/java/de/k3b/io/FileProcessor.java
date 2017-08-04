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

public class FileProcessor {
    private static final String EXT_SIDECAR = ".xmp";

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
}

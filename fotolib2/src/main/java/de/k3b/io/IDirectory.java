/*
 * Copyright (c) 2015-2021 by k3b.
 *
 * This file is part of AndroFotoFinder.
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
 * Abstraction of Directory
 * <p>
 * Created by k3b on 04.08.2015.
 */
public interface IDirectory<T extends IDirectory> {
    int DIR_FLAG_NONE = 0;
    int DIR_FLAG_NOMEDIA = 1; // below linux hidden dir ".*" or below DIR_FLAG_NOMEDIA_ROOT
    int DIR_FLAG_NOMEDIA_ROOT = 2; // containing ".nomedia"

    int DIR_FLAG_APM_DIR = 8; // containing ".apm"
    int DIR_FLAG_VIRTUAL_DIR = 9; // containing "*.album"
    String APM_DIR_PREFIX = "§ ";

    String getRelPath();

    String getAbsolute();

    T createOsDirectory(IFile file, T parent, T[] children);

    T[] createOsDirectoryArray(int size);

    T getParent();

    T[] getChildDirs();

    void removeChild(T... child);

    void addChild(T... child);

    int childIndexOf(IDirectory child);

    T find(String path);

    void destroy();

    int getSelectionIconID();

    int getDirFlags();

    /**
     * reloads entry flags
     */
    void refresh();

    /** #114: update internal data after a folder has been renamed in the gui */
    void rename(String oldFolderName, String newFolderName);
}

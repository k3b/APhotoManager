/*
 * Copyright (c) 2015-2018 by k3b.
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

import java.util.List;

/**
 * Abstraction of Directory
 *
 * Created by k3b on 04.08.2015.
 */
public interface IDirectory {
    public static final int DIR_FLAG_NONE           = 0;
    public static final int DIR_FLAG_NOMEDIA        = 1; // below linux hidden dir ".*" or below DIR_FLAG_NOMEDIA_ROOT
    public static final int DIR_FLAG_NOMEDIA_ROOT   = 2; // containing ".nomedia"

    public static final int DIR_FLAG_APM_DIR        = 8; // containing ".apm"
    String APM_DIR_PREFIX = "§ ";

    String getRelPath();
    String getAbsolute();

    IDirectory getParent();

    List<IDirectory> getChildren();

    IDirectory find(String path);

    void destroy();

    int getSelectionIconID();

    int getDirFlags();

    /** reloads entry flags */
    void refresh();

    /** #114: update internal data after a folder has been renamed in the gui */
    void rename(String oldFolderName, String newFolderName);
}

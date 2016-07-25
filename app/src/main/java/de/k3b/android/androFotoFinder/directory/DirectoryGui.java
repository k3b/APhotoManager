/*
 * Copyright (c) 2015 by k3b.
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
 
package de.k3b.android.androFotoFinder.directory;

import de.k3b.io.IDirectory;

/**
 * Created by k3b on 02.07.2015.
 */
public interface DirectoryGui {
    /** Defines Directory Navigation */
    void defineDirectoryNavigation(IDirectory root, int dirTypId, String initialAbsolutePath);

    /** Set curent selection to absolutePath */
    void navigateTo(String absolutePath);
}

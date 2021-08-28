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
package de.k3b;

public class LibGlobalFile {
    /**
     * LOG_CONTEXT is used as logging source for filtering logging messages that belong to this
     */
    public static final String LOG_TAG = "k3bLibFileFacade";

    /**
     * Global.xxxxx. Non final values may be changed from outside (SettingsActivity or commandline parameter)
     */
    public static boolean debugEnabled = false;

    /**
     * false do not follow symlinks when scanning Directories.
     */
    public static final boolean ignoreSymLinks = false;


    // will be written into exif/xmp if not already set.
    // Overwritten by android Application.onCreate()
    public static String appName = null;
    public static String appVersion = null;
}

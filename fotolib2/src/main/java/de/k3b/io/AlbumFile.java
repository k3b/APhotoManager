/*
 * Copyright (c) 2018-2019 by k3b.
 *
 * This file is part of AndroFotoFinder / #APhotoManager
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
import java.util.ArrayList;
import java.util.List;

/**
 * Created by k3b on 17.04.2018.
 */

public class AlbumFile {
    public static final String SUFFIX_VALBUM = ".album";
    public static final String SUFFIX_QUERY = ".query";

    public static boolean isQueryFile(String uri) {
        if (uri != null) {
            return isQueryFile(uri, SUFFIX_VALBUM) || isQueryFile(uri, SUFFIX_QUERY);
        }
        return false;
    }

    private static boolean isQueryFile(String uri, String suffix) {
        int pos = uri.lastIndexOf(suffix);
        if (pos < 0) return false;
        return (uri.length() - pos - suffix.length()) <= 2; // ends with suffix or (suffix + "/%")
    }

    public static boolean isQueryFile(File uri) {
        if (uri != null) {
            return isQueryFile(uri.getName());
        }
        return false;
    }
    public static File getExistingQueryFileOrNull(String uri) {
        if (isQueryFile(uri)) {
            File result = new File(fixPath(uri));
            if ((result != null) && result.isFile() && result.exists()) return result;
        }
        return null;
    }

    public static String fixPath(String uri) {
        String result = removeAtEnd(FileUtils.fixPath(uri), "%", "/");
        return result;
    }

    private static String removeAtEnd(String result, String... chars) {
        for (String c : chars) {
            if (result.endsWith(c)) result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /** return all album files as absolute path */
    public static List<String> getFilePaths(List<String> result, File root, int subDirLevels) {
        if (result == null) result = new ArrayList<String>();

        if ((root != null) && !FileUtils.isSymlinkDir(root,false) && root.isDirectory()) {
            for (File file : root.listFiles()) {
                if (file.isDirectory() && (subDirLevels > 1)) {
                    getFilePaths(result, file, subDirLevels - 1);
                } else if (isQueryFile((file))) {
                    String path = FileUtils.tryGetCanonicalPath(file, null);
                    result.add(path);
                }
            }
        }
        return result;
    }
}

/*
 * Copyright (c) 2018 by k3b.
 *
 * This file is part of #APhotoManager (https://github.com/k3b/APhotoManager/)
 *              and #toGoZip (https://github.com/k3b/ToGoZip/).
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *
 * for more details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>
 */
package de.k3b.io;

/**
 * Created by k3b on 17.02.2015.
 */
public class FileNameUtil {

    /** converts baseName to a valid filename. If it has no file-extension then defaultExtension is added */
    public static String createFileName(String baseName, String defaultExtension) {
        StringBuilder result = new StringBuilder(baseName);

        // remove trailing "."
        int len = result.length();
        while ((len > 0) && (result.charAt(len - 1) == '.')) {
            result.deleteCharAt(len - 1);
            len--;
        }

        // remove leading "."
        while ((len > 0) && (result.charAt(0) == '.')) {
            result.deleteCharAt(0);
            len--;
        }

        // add extension if there is none
        if ((defaultExtension != null) && (result.indexOf(".") < 0)) {
            result.append(".").append(defaultExtension);
        }

        // replace illegal chars with "_"
        replace(result, "_", "/", "\\", ":", " ", "?", "*", "&", "%", ">", "<", "|", "'", "\"", "__");
        return result.toString();
    }

    public static String getWithoutWildcard(String path) {
        if ((path == null) || (path.length() == 0)) return null;
        if (path.endsWith("%")) {
            // remove sql wildcard at end of name
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    /** replaces all occurences of illegalValues in result by replacement */
    private static void replace(StringBuilder result, String replacement, String ... illegalValues) {
        for (String illegalValue : illegalValues) {
            int found = result.indexOf(illegalValue);
            while (found >= 0) {
                result.replace(found, found+illegalValue.length(), replacement);
                found = result.indexOf(illegalValue);
            }
        }
    }
}

/*
 * Copyright (c) 2021 by k3b.
 *
 * This file is part of #APhotoManager (https://github.com/k3b/APhotoManager/)
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
package de.k3b.io.filefacade;

import java.util.ArrayList;
import java.util.List;

import de.k3b.io.FileUtilsBase;

public class DirectoryFilter {
    private static final List<String> allowedFileSuffixesLowercase = new ArrayList<>();

    static {
        init();
    }

    public static void init() {
        includeFileSuffixesForListDir(FileUtilsBase.MEDIA_IGNORE_FILENAME);
    }

    public static void includeFileSuffixesForListDir(String... allowedFileSuffixes) {
        for (String suffix : allowedFileSuffixes) {
            String suffixLowerCase = suffix.toLowerCase();
            if (!allowedFileSuffixesLowercase.contains(suffixLowerCase)) {
                allowedFileSuffixesLowercase.add(suffixLowerCase);
            }
        }
    }

    public static boolean accept(String nameLowerCase) {
        for (String suffix : allowedFileSuffixesLowercase) {
            if (nameLowerCase.endsWith(suffix)) return true;
        }
        return false;
    }
}

/*
 * Copyright (c) 2019 by k3b.
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
package de.k3b.android.androFotoFinder.backup;


public class BackupOptions {
    public static final int ALL = 0xfffffff;

    public static final int PHOTOS_WITH_EXISTING_XMP = 0x1;
    public static final int PROPERTIES_AS_CSV = 0x101;

    //TODO Tag-Definitions, album files, batches from change history (copr/move/delete)

    public static boolean allOf(int flags, int selected) {
        return (flags & selected) == selected;
    }

    public static boolean anyOf(int flags, int selected) {
        return (flags & selected) != 0;
    }
}

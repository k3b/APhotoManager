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
package de.k3b.android.androFotoFinder.queries;

import android.net.Uri;
import android.provider.MediaStore;

/**
 * Created by k3b on 04.01.2017.
 */

// pseudo constants (non final with unittest-defaults to be initialized with init()
public class FotoSqlBase {
    public static Uri SQL_TABLE_EXTERNAL_CONTENT_URI = null; // = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    public static Uri SQL_TABLE_EXTERNAL_CONTENT_URI_FILE = null; // = MediaStore.Files.getContentUri("external");
    public static String SQL_TABLE_EXTERNAL_CONTENT_URI_FILE_NAME = "content:/unittest/file"; // = SQL_TABLE_EXTERNAL_CONTENT_URI_FILE.toString();

    /** if not called: run in unittests. else use calculated methods that require android */
    public static void init() {
        // required calls to android methods which is not possible in host run unittests.
        SQL_TABLE_EXTERNAL_CONTENT_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        SQL_TABLE_EXTERNAL_CONTENT_URI_FILE = MediaStore.Files.getContentUri("external");
        SQL_TABLE_EXTERNAL_CONTENT_URI_FILE_NAME = SQL_TABLE_EXTERNAL_CONTENT_URI_FILE.toString();
    }
}

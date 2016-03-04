/*
 * Copyright (c) 2015-2016 by k3b.
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
 
package de.k3b.android.util;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;

import java.io.File;

import de.k3b.android.androFotoFinder.Common;

/**
 * Created by k3b on 09.09.2015.
 */
public class IntentUtil implements Common {

    /** get uri from data. if there is no data from EXTRA_STREAM */
    @Nullable
    public static Uri getUri(Intent intent) {
        Uri uri = intent.getData();
        Bundle extras = (uri != null) ? null : intent.getExtras();
        Object stream = (extras == null) ? null : extras.get(EXTRA_STREAM);
        if (stream != null) {
            uri = Uri.parse(stream.toString());
        }
        return uri;
    }

    /** return null if uri is not a valid file scheam */
    @Nullable
    public static File getFile(Uri uri) {
        if (isFileUri(uri)) {
            try {
                return new File(uri.getPath());
            } catch (Exception ex) {
                ; // i.e. contain illegal chars
            }
        }
        return null;
    }

    public static boolean isFileUri(Uri uri) {
        return (uri != null) && ("file".equals(uri.getScheme()));
    }

}

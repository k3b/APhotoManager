/*
 * Copyright (c) 2020 by k3b.
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

package de.k3b.android.io;

import java.io.IOException;
import java.io.InputStream;

import de.k3b.android.widget.FilePermissionActivity;
import de.k3b.media.ExifInterfaceEx;
import de.k3b.media.IPhotoProperties;

/**
 * Overwrite of {@link ExifInterfaceEx} that replaces {@link java.io.File} implementation
 * with android specific {@link android.support.v4.provider.DocumentFile}
 */
public class ExifInterfaceExAndroid extends ExifInterfaceEx {
    private static final String MIMNE = "image/jpeg";
    private static DocumentFileTranslator documentFileTranslator = null;

    private ExifInterfaceExAndroid(
            String absoluteJpgPath, InputStream in,
            IPhotoProperties xmpExtern, String dbg_context) throws IOException {
        super(absoluteJpgPath, in, xmpExtern, dbg_context);
    }

    public static void initFactory() {
        factory = new Factory() {
            public ExifInterfaceEx create(
                    String absoluteJpgPath, InputStream in, IPhotoProperties xmpExtern,
                    String dbg_context) throws IOException {
                return new ExifInterfaceExAndroid(absoluteJpgPath, in, xmpExtern, dbg_context);
            }
        };
    }

    public static void setContext(FilePermissionActivity activity) {
        documentFileTranslator = activity.getDocumentFileTranslator();
    }

}

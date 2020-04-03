/*
 * Copyright (c) 2020 by k3b.
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
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>
 */
package de.k3b.android.util;

import android.support.v4.provider.DocumentFile;
import android.util.Log;

import java.io.File;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.io.FileApi;

/**
 * File api abstraction based on android dependand {@link android.support.v4.provider.DocumentFile} API
 */
public class AndroidFileApi extends FileApi {
    protected boolean osRenameTo(File dest, File source) throws PermissionException {
        final String context = this.getClass().getSimpleName() +
                ".osRenameTo(" + dest + " <== " + source + ")";
        if ((source != null) && (dest != null)) {
            if (dest.getParentFile().equals(source.getParentFile())) {
                Boolean result = null;
                try {
                    final DocumentFile documentFile = DocumentFile.fromFile(source);
                    if (!documentFile.canWrite()) throw new PermissionException(source, context);
                    result = documentFile.renameTo(dest.getName());
                    return result;
                } finally {
                    if (Global.debugEnabled) {
                        Log.d(TAG, context + " ==> " + result);
                    }
                }
            }
        }

        Log.w(TAG, context + " move between different directories is not implemented yet");
        return super.osRenameTo(dest, source);
    }

    public static class PermissionException extends RuntimeException {
        private final File sourceFile;

        public PermissionException(File source, String errorMessage) {
            super(errorMessage);
            this.sourceFile = source;
        }

        public File getSourceFile() {
            return sourceFile;
        }
    }

}

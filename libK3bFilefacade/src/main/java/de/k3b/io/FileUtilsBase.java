/*
 * Copyright (c) 2015-2021 by k3b.
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

package de.k3b.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.k3b.LibGlobalFile;

public class FileUtilsBase {
    private static final Logger logger = LoggerFactory.getLogger(LibGlobalFile.LOG_TAG);
    private static final String DBG_CONTEXT = "FileUtils:";

    /** tryGetCanonicalFile without exception */
    public static File tryGetCanonicalFile(String path) {
        if (path == null) return null;

        final File file = new File(path);
        return tryGetCanonicalFile(file, file);
    }

    /** tryGetCanonicalFile without exception */
    public static File tryGetCanonicalFile(File file, File errorValue) {
        if (file == null) return null;

        try {
            return file.getCanonicalFile();
        } catch (IOException ex) {
            logger.warn(DBG_CONTEXT + "Error tryGetCanonicalFile('" + file.getAbsolutePath() + "') => '" + errorValue + "' exception " + ex.getMessage(), ex);
            return errorValue;
        }
    }

    /** tryGetCanonicalFile without exception */
    public static File tryGetCanonicalFile(File file) {
        return tryGetCanonicalFile(file, file);
    }

    /** tryGetCanonicalFile without exception */
    public static String tryGetCanonicalPath(File file, String errorValue) {
        if (file == null) return null;

        try {
            return file.getCanonicalPath();
        } catch (IOException ex) {
            logger.warn(DBG_CONTEXT + "Error tryGetCanonicalPath('"
                    + file.getAbsolutePath() + "') => '" + errorValue + "' exception "
                    + ex.getMessage(), ex);
            return errorValue;
        }
    }
    public static void copy(InputStream in, OutputStream out) throws IOException {
        copy(in, out, "");
    }

    /**
     * copy all from is to os and closes the streams when done
     */
    public static void copy(InputStream in, OutputStream out, String dbgContext) throws IOException {
        try {
            if (LibGlobalFile.debugEnabled) {
                logger.debug(DBG_CONTEXT + dbgContext + " copy " + in + "=>" + out);
            }
            byte[] buffer = new byte[10240]; // 10k buffer
            int bytesRead = -1;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        } finally {
            FileUtilsBase.close(in, dbgContext + " copy-close src " + in);
            FileUtilsBase.close(out, dbgContext + " copy-close dest " + out);
        }
    }


    public static void close(Closeable stream, Object source) {
        if (stream != null) {
            try {
                if (LibGlobalFile.debugEnabled) {
                    logger.warn(DBG_CONTEXT + "Closing " + source);
                }

                stream.close();
            } catch (Exception e) {
                // catch IOException and in android also NullPointerException
                // java.lang.NullPointerException: Attempt to invoke virtual method
                // 'void java.io.InputStream.close()' on a null object reference
                // where stream is java.io.FilterInputStream whith child-filter "in" = null
                if (source != null && LibGlobalFile.debugEnabled) {
                    logger.warn(DBG_CONTEXT + "Error close " + source, e);
                }
            }
        }
    }

}

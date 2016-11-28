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
 
package de.k3b.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.k3b.FotoLibGlobal;


/**
 * Created by k3b on 06.10.2015.
 */
public class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FotoLibGlobal.LOG_TAG);

    public static String readFile(File file) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();

        while (line != null) {
            sb.append(line);
            sb.append("\n");
            line = br.readLine();
        }
        br.close();
        return sb.toString();
    }

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
            if (FotoLibGlobal.debugEnabled) {
                logger.warn("Error tryGetCanonicalFile('" + file.getAbsolutePath() + "') => '" + errorValue + "' exception " + ex.getMessage(), ex);
            }
            return errorValue;
        }
    }

    /** tryGetCanonicalFile without exception */
    public static String tryGetCanonicalPath(File file, String errorValue) {
        if (file == null) return null;

        try {
            return file.getCanonicalPath();
        } catch (IOException ex) {
            if (FotoLibGlobal.debugEnabled) {
                logger.warn("Error tryGetCanonicalPath('" + file.getAbsolutePath() + "') => '" + errorValue + "' exception " + ex.getMessage(), ex);
            }
            return errorValue;
        }
    }

    /** @return true if directory is an alias of an other (symlink-dir). */
	public static  boolean isSymlinkDir(File directory, boolean errorValue) {
        if (FotoLibGlobal.ignoreSymLinks || (directory == null)) {
            return false;
        }

        // from http://stackoverflow.com/questions/813710/java-1-6-determine-symbolic-links
        String canonicalPath = tryGetCanonicalPath(directory, null);
        if (canonicalPath != null) {
            boolean result = !directory.getAbsolutePath().equals(canonicalPath);
            if (result && FotoLibGlobal.debugEnabled) {
                logger.debug("isSymlinkDir('" + directory.getAbsolutePath() + "') => true because CanonicalPath='" + canonicalPath + "'");
            }
			
			return result;
        }
        return errorValue;

	}

    public static String getDebugString(String prefix, File file) {
        StringBuffer result = new StringBuffer();
        result.append(prefix)
                .append(" dir ").append(file.isDirectory())
                .append(", hid ").append(file.isHidden())
                .append(", write ").append(file.canWrite())
                .append(" syml ").append(FileUtils.isSymlinkDir(file,true))
                .append(" : ").append(file.getAbsolutePath()
        );
        return result.toString();
    }

    public static File getXmpFile(String filepath) {
        if (filepath != null) {
            // String filepath = file.getAbsolutePath();
            int ext = filepath.lastIndexOf(".");

            String xmpFilePath = (ext >= 0) ? (filepath.substring(0, ext) + ".xmp") : (filepath + ".xmp");
            File xmpFile = new File(xmpFilePath);
            return xmpFile;
        }
        return null;
    }
}

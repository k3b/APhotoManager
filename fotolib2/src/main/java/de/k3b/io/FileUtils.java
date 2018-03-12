/*
 * Copyright (c) 2015-2018 by k3b.
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
 
package de.k3b.io;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.k3b.FotoLibGlobal;


/**
 * Created by k3b on 06.10.2015.
 */
public class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FotoLibGlobal.LOG_TAG);
    private static final String DBG_CONTEXT = "FileUtils:";

    public static final String MEDIA_IGNORE_FILENAME = ".nomedia"; // MediaStore.MEDIA_IGNORE_FILENAME;

    public static InputStream streamFromStringContent(String data) {
        ByteArrayInputStream s = new ByteArrayInputStream(data.getBytes());
        return s;
    }

    public static String readFile(File file) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();

        while (line != null) {
            sb.append(line);
            sb.append("\n");
            line = br.readLine();
        }
        close(br, file);
        return sb.toString();
    }

	public static void close(Closeable stream, Object source) {
		if (stream != null) {
			try {			
				stream.close();
			} catch (IOException e) {
                logger.warn(DBG_CONTEXT + "Error close " + source, e);
			}
		}
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
            logger.warn(DBG_CONTEXT + "Error tryGetCanonicalPath('" + file.getAbsolutePath() + "') => '" + errorValue + "' exception " + ex.getMessage(), ex);
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
                logger.debug(DBG_CONTEXT + "isSymlinkDir('" + directory.getAbsolutePath() + "') => true because CanonicalPath='" + canonicalPath + "'");
            }
			
			return result;
        }
        return errorValue;

	}

    public static String getDebugString(String prefix, File file) {
        StringBuilder result = new StringBuilder();
        result.append(prefix)
                .append(" dir ").append(file.isDirectory())
                .append(", hid ").append(file.isHidden())
                .append(", write ").append(file.canWrite())
                .append(" syml ").append(FileUtils.isSymlinkDir(file,true))
                .append(" : ").append(file.getAbsolutePath()
        );
        return result.toString();
    }

    /** replaceExtension("/path/to/image.jpg", ".xmp") becomes "/path/to/image.xmp" */
    public static String replaceExtension(String path, String extension) {
        if (path == null) return null;
        int ext = path.lastIndexOf(".");
        return ((ext >= 0) ? path.substring(0, ext) : path) + extension;
    }

    public static String getExtension(String path) {
        if (path == null) return null;
        int ext = path.lastIndexOf(".");
        if (ext >= 0) return path.substring(ext);
        return null;
    }

    /** return parent of path if path is not a dir. else return path */
    public static File getDir(String path) {
        if ((path == null) || (path.length() == 0)) return null;
        if (path.endsWith("%")) {
            // remove sql wildcard at end of name
            return getDir(new File(path.substring(0,path.length() - 1)));
        }
        return getDir(new File(path));
    }

    /** return parent of file if path is not a dir. else return file */
    private static File getDir(File file) {
        return ((file != null) && (!file.isDirectory())) ? file.getParentFile() : file;
    }

    /** find cildren by regular expression */
    public static File[] listFiles(File parent, final Pattern fileOrDirThatMustBeInTheRoot) {
        return parent.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File owner, String fileName) {
                final boolean found = fileOrDirThatMustBeInTheRoot.matcher(fileName).matches();
                return found;
            }
        });
    }

    /** return true, if file is in a ".nomedia" dir */
    public static boolean isNoMedia(String path, int maxLevel) {
        if (path != null) {
            if (isHiddenFolder(path))
                return true;
            File file = getDir(path);
            int level = maxLevel;
            while ((--level >= 0) && (file != null)) {
                if (new File(file, MEDIA_IGNORE_FILENAME).exists()) {
                    return true;
                }
                file = file.getParentFile();
            }
        }
        return false;
    }

    // linux convention: folder names starting with "." are hidden
    public static boolean isHiddenFolder(String path) {
        return (path.contains("/."));
    }

    public static void delete(String fileName) {
        delete(fileName, "");
    }

    public static void delete(String fileName, String fileExt) {
        File file = new File(fileName);
        delete(file, fileExt);
    }

    // Delete the file or if it's a directory, all files in the directory
    public static void delete(File file, final String fileExt) {
        if (file.exists()) {
            //check if the file is a directory
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for(File f:files){
                    //call deletion of file individually
                    delete(f, fileExt);
                }
            } else {
                String path = file.getAbsolutePath();
                if(fileExt == null || path.endsWith(fileExt)) {
                    boolean result = file.delete();
                    if (FotoLibGlobal.debugEnabled) {
                        // test if delete of file is success or not
                        if (result) {
                            logger.info(DBG_CONTEXT + "File {} deleted", file.getAbsolutePath());
                        } else {
                            logger.info(DBG_CONTEXT + "File {} was not deleted, unknown reason", file.getAbsolutePath());
                        }
                    }
                }
            }
        } else {
            if (FotoLibGlobal.debugEnabled) logger.info(DBG_CONTEXT + "File {} doesn't exist", file.getAbsolutePath());
        }
    }

    /** overwrites existing file */
    public static void copyReplace(String sourcePath, String destinationPath,
                                   boolean deleteOriginalAfterFinish, String what) throws IOException {
        copyReplace(new File(sourcePath), new File(destinationPath), deleteOriginalAfterFinish, what);
    }

    public static void copyReplace(File inFile, File outFile, boolean deleteOriginalAfterFinish, String what) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug(DBG_CONTEXT + what + (deleteOriginalAfterFinish ? "-move" : "-copy") + ": " + inFile +
                    " ==> " + outFile);
        }
        InputStream sourceStream = null;
        try {
            sourceStream = new FileInputStream(inFile);
            copyReplace(sourceStream, outFile);

            if(deleteOriginalAfterFinish && inFile.exists()) inFile.delete();
        } finally {
            close(sourceStream, what);
        }
    }

    public static void copyReplace(InputStream sourceStream, File destinationFile) throws IOException {
        if (destinationFile.exists()) destinationFile.delete();
        destinationFile.getParentFile().mkdirs();
        FileOutputStream result = new FileOutputStream(destinationFile);
        FileUtils.copy(sourceStream, result);
        result.flush();
        FileUtils.close(result, destinationFile);
    }

    public static void copy(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[10240]; // 10k buffer
        int bytesRead = -1;

        while((bytesRead = is.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
    }


}

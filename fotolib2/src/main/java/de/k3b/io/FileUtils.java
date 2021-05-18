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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Map;
import java.util.regex.Pattern;

import de.k3b.LibGlobal;
import de.k3b.io.filefacade.FileFacade;
import de.k3b.io.filefacade.IFile;

/**
 * Created by k3b on 06.10.2015.
 */
public class FileUtils extends FileUtilsBase {
    private static final Logger logger = LoggerFactory.getLogger(LibGlobal.LOG_TAG);
    private static final String DBG_CONTEXT = "FileUtils:";

    public static InputStream streamFromStringContent(String data) {
        ByteArrayInputStream s = new ByteArrayInputStream(data.getBytes());
        return s;
    }

    public static String readFile(InputStream file) throws IOException {
        return internalReadFile(new BufferedReader(new InputStreamReader(file)), file);
    }

    @Deprecated
    public static String readFile(File file) throws IOException {
        return readFile(FileFacade.convert("FileUtils readFile via File ", file));
    }

    public static String readFile(IFile file) throws IOException {
        return internalReadFile(new BufferedReader(new InputStreamReader(file.openInputStream())), file);
    }

    public static String readFile(InputStream is, byte[] buffer) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        FileUtils.copyStream(byteArrayOutputStream, is, buffer);
        String result = new String(byteArrayOutputStream.toByteArray());
        byteArrayOutputStream.flush();
        byteArrayOutputStream.close();
        return result;
    }

    /**
     * helper to copy stream-data
     */
    public static void copyStream(OutputStream outputStream, InputStream inputStream, byte[] buffer) throws IOException {
        try {
            for (int read = inputStream.read(buffer); read > -1; read = inputStream
                    .read(buffer)) {
                outputStream.write(buffer, 0, read);
            }
        } finally {
            close(inputStream, null);
        }
    }

    public static String internalReadFile(BufferedReader br, Object source) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();

        try {
            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
        } finally {
            close(br, source);
        }
        return sb.toString();
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

    /**
     * replaceExtension("/path/to/image.jpg", ".xmp") becomes "/path/to/image.xmp"
     *
     * @param extension the new extension, including the dot. "" means no extension.
     */
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
        return getDir(createFile(path));
    }

    public static File createFile(String path) {
        if ((path == null) || (path.length() == 0)) return null;
        return new File(FileNameUtil.getWithoutWildcard(path));
    }

    /** return parent of file if path is not a dir. else return file */
    private static File getDir(File file) {
        return ((file != null) && (!file.isDirectory())) ? file.getParentFile() : file;
    }

    /** return parent of file if path is not a dir. else return file */
    public static IFile getDir(IFile file) {
        return ((file != null) && (!file.isDirectory())) ? file.getParentIFile() : file;
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

    public static boolean isNoMedia(IFile path, int maxLevel) {
        return isNoMedia(path, maxLevel, null);
    }

    /**
     * return true, if file is in a ".nomedia" dir
     */
    public static boolean isNoMedia(IFile path, int maxLevel, Map<String, Boolean> nomediaCache) {
        if (path != null) {
            if (isHiddenFolder(path))
                return true;
            IFile file = path.getParentIFile();
            String firstDir = file.getAbsolutePath();
            Boolean cacheFind;
            int level = maxLevel;
            while ((--level >= 0) && (file != null)) {
                final String absolutePath = file.getAbsolutePath();
                cacheFind = (nomediaCache == null) ? null : nomediaCache.get(absolutePath);
                if (cacheFind != null) {
                    nomediaCache.put(firstDir, cacheFind.booleanValue());
                    return cacheFind.booleanValue();
                }

                if (file.createIFile(MEDIA_IGNORE_FILENAME).exists()) {
                    if (nomediaCache != null) {
                        nomediaCache.put(absolutePath, true);
                        nomediaCache.put(firstDir, true);
                    }
                    return true;
                }
                file = file.getParentIFile();
            }
            if (nomediaCache != null) {
                nomediaCache.put(firstDir, false);
            }
        }
        return false;
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
    public static boolean isHiddenFolder(IFile path) {
        return (path != null) && isHiddenFolder(path.getCanonicalPath());
    }

    // linux convention: folder names starting with "." are hidden
    public static boolean isHiddenFolder(String path) {
        return (path != null) && (path.contains("/.") || path.contains("\\."));
    }

    public static void delete(String fileName) {
        delete(fileName, "");
    }

    public static void delete(String fileName, String fileExt) {
        File file = new File(fileName);
        delete(file, fileExt);
    }

    public static void delete(File file, final String fileExt) {
        delete(FileFacade.convert("FileUtils delete via File ", file), fileExt);
    }

    // Delete the file or if it's a directory, all files in the directory
    public static void delete(IFile file, final String fileExt) {
        if (file.exists()) {
            //check if the file is a directory
            if (file.isDirectory()) {
                IFile[] files = file.listIFiles();
                for (IFile f : files) {
                    //call deletion of file individually
                    delete(f, fileExt);
                }
            } else {
                String path = file.getAbsolutePath();
                if(fileExt == null || path.endsWith(fileExt)) {
                    boolean result = file.delete();
                    if (LibGlobal.debugEnabled) {
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
            if (LibGlobal.debugEnabled) logger.info(DBG_CONTEXT + "File {} doesn't exist", file.getAbsolutePath());
        }
    }

    /** overwrites existing file */
    public static void copyReplace(String sourcePath, String destinationPath,
                                   boolean deleteOriginalAfterFinish, String what) throws IOException {
        copyReplace(new File(sourcePath), new File(destinationPath), deleteOriginalAfterFinish, what);
    }

    @Deprecated
    public static void copyReplace(File inFile, File outFile, boolean deleteOriginalAfterFinish, String what) throws IOException {
        copyReplace(FileFacade.convert("FileUtils copyReplace via File ", inFile), FileFacade.convert(null, outFile), deleteOriginalAfterFinish, what);
    }

    public static void copyReplace(IFile inFile, IFile outFile, boolean deleteOriginalAfterFinish, String what) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug(DBG_CONTEXT + what + (deleteOriginalAfterFinish ? "-move" : "-copy") + ": " + inFile +
                    " ==> " + outFile);
        }
        InputStream sourceStream = null;
        try {
            sourceStream = inFile.openInputStream();
            copyReplace(sourceStream, outFile);

            if(deleteOriginalAfterFinish && inFile.exists()) inFile.delete();
        } finally {
            close(sourceStream, what);
        }
    }

    @Deprecated
    public static void copyReplace(InputStream sourceStream, File destinationFile) throws IOException {
        copyReplace(sourceStream, FileFacade.convert("FileUtils copyReplace via File ", destinationFile));
    }

    public static void copyReplace(InputStream sourceStream, IFile destinationFile) throws IOException {
        if (destinationFile.exists()) destinationFile.delete();
        destinationFile.getParentIFile().mkdirs();
        FileUtilsBase.copy(sourceStream, destinationFile.openOutputStream(), " copyReplace ");
    }


    // #118 app specific content uri convert
    // from {content://approvider}//storage/emulated/0/DCIM/... to /storage/emulated/0/DCIM/
    public static String fixPath(String path) {
        if (path != null) {
            while (path.startsWith("//")) {
                path = path.substring(1);
            }
        }
        return path;
    }

    @Deprecated
    public static File getFirstExistingDir(File root) {
        final IFile dir = getFirstExistingDir(FileFacade.convert("FileUtils getFirstExistingDir via File ", root));
        return (dir == null) ? null : dir.getFile();
    }

    public static IFile getFirstExistingDir(IFile root) {
        while ((root != null) && (!root.exists() || !root.isDirectory())) {
            root = root.getParentIFile();
        }
        return root;
    }

    @Deprecated
    public static File getFirstNonExistingFile(File parentDir, String newFilePrefix, int number, String newFileSuffix) {
        return getFirstNonExistingFile(FileFacade.convert("FileUtils getFirstNonExistingFile via File ", parentDir), newFilePrefix, number, newFileSuffix).getFile();
    }

    public static IFile getFirstNonExistingFile(IFile parentDir, String newFilePrefix, int number, String newFileSuffix) {
        if (parentDir == null) return null;

        parentDir.mkdirs();
        final String mime = "*/*";
        IFile candidate = parentDir.createIFile(newFilePrefix + newFileSuffix);
        while (candidate.exists()) {
            number++;
            candidate = parentDir.createIFile(newFilePrefix + number + newFileSuffix);
        }
        return candidate;
    }
}

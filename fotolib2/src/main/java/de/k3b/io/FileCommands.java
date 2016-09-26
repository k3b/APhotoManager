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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by k3b on 03.08.2015.
 */
public class FileCommands implements  Cloneable {
    public static final int OP_COPY = 1;
    public static final int OP_MOVE = 2;
    public static final int OP_DELETE = 3;
    public static final int OP_RENAME = 4;
    public static final int OP_UPDATE = 5;
    private static final String EXT_SIDECAR = ".xmp";

    protected String mLogFilePath;
    // private static final String LOG_FILE_ENCODING = "UTF-8";
    protected PrintWriter mLogFile;
    protected ArrayList<String> mModifiedDestFiles;
    protected ArrayList<String> mModifiedSrcFiles;

    public FileCommands() {
        setLogFilePath(null);
    }

    public int deleteFiles(String... paths) {
        int result = 0;
        if (canProcessFile(OP_DELETE)) {
            openLogfile();
            onPreProcess("delete", paths, null, OP_DELETE);
            for (String path : paths) {
                if (path != null) {
                    if (deleteFileWithSidecar(new File(path))) {
                        result++;
                    }
                }
            }
            onPostProcess("delete", paths, null, result, paths.length, OP_DELETE);
            closeLogFile();
        }
        return result;
    }

    protected boolean canProcessFile(int opCode) {
        return true;
    }

    /**
     * @return true if file was deleted or does not exist (any more)
     */
    protected boolean deleteFileWithSidecar(File file) {
        boolean result = false;

        if (file != null) {
            File sidecar = getSidecar(file);
            if (osFileExists(sidecar)) {
                osDeleteFile(sidecar); // dont care if delete was successfull
                log("del " , getFilenameForLog(sidecar));
            }

            if (osFileExists(file)) {
                if (!osDeleteFile(file)) {
                    log("rem file exists. delete failed : ", getFilenameForLog(file));
                } else {
                    result = true; // was deleted
                }
            } else {
                log("rem file ", getFilenameForLog(file), " does not exist");
                result = true; // it is gone
            }
        }
        log("del ", getFilenameForLog(file));
        return result;
    }

    public static String getFilenameForLog(File file) {
        if (file == null) return "";
        return getFilenameForLog(file.getAbsolutePath());
    }

    public static String getFilenameForLog(String absolutePath) {
        if (absolutePath == null) return "";
        return "\"" + absolutePath.replace("/", "\\") + "\"";
    }

    public int moveOrCopyFilesTo(boolean move, File destDirFolder, File... sourceFiles) {
        int result = 0;
        if (canProcessFile(move ? OP_MOVE : OP_COPY)) {
            if (osCreateDirIfNeccessary(destDirFolder)) {
                File[] destFiles = createDestFiles(destDirFolder, sourceFiles);

                result = moveOrCopyFiles(move, (move ? "mov" : "copy"), destFiles, sourceFiles);

            } else {
                log("rem Target dir ", getFilenameForLog(destDirFolder), " cannot be created");
            }
        }
        return result;
    }

    /** does the copying. also used by unittesting */
    protected int moveOrCopyFiles(boolean move, String what, File[] destFiles, File[] sourceFiles) {
        int opCode = (move) ? OP_MOVE : OP_COPY;

        mModifiedSrcFiles = (move) ? new ArrayList<String>() : null;
        mModifiedDestFiles = new ArrayList<String>();

        openLogfile();
        // onPreProcess(what, sourceFiles, destFiles, opCode);
        onPreProcess(what, null, null, opCode);
        int itemCount = 0;
        int pos = 0;
        int fileCount = destFiles.length;

        while (pos < fileCount) {
            File sourceFile = sourceFiles[pos];
            File destFile = destFiles[pos];

            File destRenamed = renameDuplicate(destFile);
            if (osFileMoveOrCopy(move, destRenamed, sourceFile)) itemCount++;
            log(((move) ? "MOVE /y " : "COPY /y "), getFilenameForLog(sourceFile), " " , getFilenameForLog(destRenamed));

            File sourceSidecar = getSidecar(sourceFile);
            if (osFileExists(sourceSidecar)) {
                File destSidecar = getSidecar(destRenamed);
                if (osFileMoveOrCopy(move, destSidecar, sourceSidecar)) itemCount++;
                log(((move) ? "MOVE /y " : "COPY /y "), getFilenameForLog(sourceSidecar), " " , getFilenameForLog(destSidecar));
            }
            pos++;
        }
        int modifyCount = mModifiedDestFiles.size();

        String[] modifiedSourceFiles =  ((mModifiedSrcFiles != null) && (mModifiedSrcFiles.size() > 0)) ? mModifiedSrcFiles.toArray(new String[modifyCount]) : null;

        onPostProcess(what, modifiedSourceFiles, (modifyCount > 0) ? mModifiedDestFiles.toArray(new String[modifyCount]) : null, itemCount, sourceFiles.length, opCode);

        closeLogFile();
        return itemCount;
    }

    private File[] createDestFiles(File destDirFolder, File... sourceFiles) {
        File[] result = new File[sourceFiles.length];
        int pos = 0;
        for(File srcFile : sourceFiles) {
            File destFile = new File(destDirFolder, srcFile.getName());
            result[pos++] = destFile;
        }

        return result;
    }

    protected boolean fileOrSidecarExists(File file) {
        if (file == null) return false;

        return osFileExists(file) || osFileExists(getSidecar(file));
    }

    public static boolean isSidecar(File file) {
        if (file == null) return false;
        return isSidecar(file.getAbsolutePath());
    }

    public static boolean isSidecar(String name) {
        if (name == null) return false;
        return name.toLowerCase().endsWith(EXT_SIDECAR);
    }

    public File getSidecar(File file) {
        if (file == null) return null;
        return getSidecar(file.getAbsolutePath());
    }

    public File getSidecar(String absolutePath) {
        if (absolutePath == null) return null;

        int suffix = absolutePath.lastIndexOf(".");

        String pathWitoutSuffix = (suffix >= 0) ?absolutePath.substring(0, suffix) : absolutePath ;
        return new File(pathWitoutSuffix + EXT_SIDECAR);
    }

    /**
     * @return file if rename is not neccessary else File with new name
     */
    public File renameDuplicate(File file) {
        if (!fileOrSidecarExists(file)) {
            // rename is not neccessary
            return file;
        }


        String filename = file.getAbsolutePath();
        String extension = ")";
        int extensionPosition = filename.lastIndexOf(".");
        if (extensionPosition >= 0) {
            extension = ")" + filename.substring(extensionPosition);
            filename = filename.substring(0, extensionPosition) + "(";
        }
        int id = 0;
        while (true) {
            id++;
            String candidatePath = filename + id + extension;
            File candidate = new File(candidatePath);
            if (!fileOrSidecarExists(candidate)) {
                log("rem renamed from ", getFilenameForLog(file), " to ", getFilenameForLog(candidatePath));
                return candidate;
            }

        }
    }

    protected boolean osFileMoveOrCopy(boolean move, File dest, File source) {
        boolean result = false;
        long fileTime = source.lastModified();

        if (move) {
            result = osFileMove(dest, source);
            if (result) {
                mModifiedDestFiles.add(dest.getAbsolutePath());
                mModifiedSrcFiles.add(source.getAbsolutePath());
            }
        } else {
            result = osFileCopy(dest, source);
            if (result) {
                mModifiedDestFiles.add(dest.getAbsolutePath());
            }
        }
        if (dest.lastModified() != fileTime) {
            dest.setLastModified(fileTime);
        }
        return result;
    }

    /** can be replaced by mock/stub in unittests */
    protected boolean osFileMove(File dest, File source) {
        if (source.renameTo(dest)) {
            // move within same mountpoint
            return true;
        }

        // #61 cannot move between different mountpoints/devices/partitions. do Copy+Delete instead
        if (source.exists() && source.isFile() && source.canRead()
                && source.canWrite() // to delete after success
                && !dest.exists()) {
            if (osFileCopy(dest, source)) {
                if (osDeleteFile(source)) {
                    return true; // move: copy + delete(source) : success
                } else {
                    // cannot delete souce: undo copy
                    osDeleteFile(dest);
                }
            }
        }
        return false;
    }

    /**
     *
     * @param sourceFullPath the path of the file that shall be copied including the file name with ending
     * @param targetFullPath the path of the file  that shall be written to without filename
     *
     * Copies a file from the sourceFullPath path to the target path.
     */
    protected boolean osFileCopy(File targetFullPath, File sourceFullPath) {
        return _osFileCopy(targetFullPath, sourceFullPath, this);
    }

    /**
     *
     * @param sourceFullPath the path of the file that shall be copied including the file name with ending
     * @param targetFullPath the path of the file  that shall be written to without filename
     *
     * Copies a file from the sourceFullPath path to the target path.
     */
    public static boolean _osFileCopy(File targetFullPath, File sourceFullPath, FileCommands owner) {
        boolean result = true;

        FileChannel in = null;
        FileChannel out = null;
        try {
            in = new FileInputStream(sourceFullPath).getChannel();
            out = new FileOutputStream(targetFullPath).getChannel();
            long size = in.size();
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0,	size);
            out.write(buf);
        } catch (Throwable e) {
            result = false;
            if (owner != null) {
                owner.onException(e, "_osFileCopy", sourceFullPath, targetFullPath);
            }
        } finally {
            if (in != null)
                try {
                    in.close();
                    if (out != null)
                        out.close();
                } catch (Exception e) {
                    if (owner != null) {
                        owner.onException(e, "osFileCopy-close", sourceFullPath, targetFullPath);
                    }
                }
        }
        return result;
    }

    /** called for every cath(Exception...) */
    protected void onException(final Throwable e, Object... context) {
        if (e != null) {
            e.printStackTrace();
        }
    }

    /** to be replaced by mock/stub in unittests */
    protected boolean osDeleteFile(File file) {
        return file.delete();
    }

    /** to be replaced by mock/stub in unittests */
    protected boolean osCreateDirIfNeccessary(File destDirFolder) {
        return destDirFolder.mkdirs() || destDirFolder.isDirectory();
    }

    /** can be replaced by mock/stub in unittests */
    public boolean osFileExists(File file) {
        return file.exists();
    }

    /** called before copy/move/rename/delete */
    protected void onPreProcess(String what, String[] oldPathNames, String[] newPathNames, int opCode) {
        /* can be overwritten */
    }

    /** called for each modified/deleted file */
    protected void onPostProcess(String what, String[] oldPathNames, String[] newPathNames, int modifyCount, int itemCount, int opCode) {
        /* can be overwritten */
    }

    public void openLogfile() {
        closeLogFile();
        if (mLogFilePath != null) {
            OutputStream stream = null;
            try {
                File logFile = new File(mLogFilePath);
                if (osFileExists(logFile)) {
                    // open existing in append mode
                    long ageInHours = (new Date().getTime() - logFile.lastModified()) / (1000 * 60 * 60);
                    stream = new FileOutputStream(logFile, true);
                    mLogFile = new PrintWriter(stream, true);

                    if (ageInHours > 15) {
                        log();
                        log("rem ", new Date());
                    }
                } else {
                    // create new
                    mLogFile = new PrintWriter(logFile, "UTF-8");
                    log("rem " , new Date());
                }
            } catch (Throwable e) {
                onException(e, "openLogfile", mLogFilePath);
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e1) {
                        onException(e1, "openLogfile-close", mLogFilePath);
                    }
                }
            }
        }
    }

    public void setLogFilePath(String logFilePath) {
        closeLogFile();
        mLogFilePath = logFilePath;
    }

    public void closeLogFile() {
        if (mLogFile != null) {
            mLogFile.close();
            mLogFile = null;
        }
    }

    public FileCommands log(Object... messages) {
        if (mLogFile != null) {
            for(Object message : messages) {
                mLogFile.print(message);
            }
            mLogFile.println();
            mLogFile.flush();
        }
        return this;
    }
}

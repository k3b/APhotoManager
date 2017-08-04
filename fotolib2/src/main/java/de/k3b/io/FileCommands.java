/*
 * Copyright (c) 2015-2017 by k3b.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import de.k3b.FotoLibGlobal;
import de.k3b.transactionlog.MediaTransactionLogDto;
import de.k3b.transactionlog.MediaTransactionLogEntryType;

/**
 * Created by k3b on 03.08.2015.
 */
public class FileCommands extends FileProcessor implements  Cloneable {
    private static final Logger logger = LoggerFactory.getLogger(FotoLibGlobal.LOG_TAG);

    public static final int OP_COPY = 1;
    public static final int OP_MOVE = 2;
    public static final int OP_DELETE = 3;
    public static final int OP_RENAME = 4;
    public static final int OP_UPDATE = 5;

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
                if ((path != null) && deleteFileWithSidecar(new File(path))) {
                        result++;
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
            File sidecar = getSidecar(file, false);
            if (osFileExists(sidecar)) {
                osDeleteFile(sidecar); // dont care if delete was successfull
            }
            sidecar = getSidecar(file, true);
            if (osFileExists(sidecar)) {
                osDeleteFile(sidecar); // dont care if delete was successfull
            }

            if (osFileExists(file)) {
                if (!osDeleteFile(file)) {
                    log("rem file exists. delete failed : ", file.getAbsolutePath());
                } else {
                    result = true; // was deleted
                }
            } else {
                log("rem file '", file.getAbsolutePath(), "' does not exist");
                result = true; // it is gone
            }
            log(MediaTransactionLogEntryType.DELETE.getCommand(file.getAbsolutePath(),"", false));
        }
        return result;
    }

    public int moveOrCopyFilesTo(boolean move, File destDirFolder, Long[] ids, File... sourceFiles) {
        int result = 0;
        if (canProcessFile(move ? OP_MOVE : OP_COPY)) {
            if (osCreateDirIfNeccessary(destDirFolder)) {
                File[] destFiles = createDestFiles(destDirFolder, sourceFiles);

                result = moveOrCopyFiles(move, (move ? "mov" : "copy"), ids, destFiles, sourceFiles);

            } else {
                log("rem Target dir '", destDirFolder, "' cannot be created");
            }
        }
        return result;
    }

    /** does the copying. also used by unittesting */
    protected int moveOrCopyFiles(boolean move, String what, Long[] ids, File[] destFiles, File[] sourceFiles) {
        int opCode = (move) ? OP_MOVE : OP_COPY;

        mModifiedSrcFiles = (move) ? new ArrayList<String>() : null;
        mModifiedDestFiles = new ArrayList<String>();

        openLogfile();
        // onPreProcess(what, sourceFiles, destFiles, opCode);
        onPreProcess(what, null, null, opCode);
        int itemCount = 0;
        int pos = 0;
        int fileCount = destFiles.length;
        long now = new Date().getTime();
        MediaTransactionLogEntryType command = (move) ? MediaTransactionLogEntryType.MOVE : MediaTransactionLogEntryType.COPY;
        MediaTransactionLogEntryType transaction = (move) ? MediaTransactionLogEntryType.MOVE : MediaTransactionLogEntryType.COPY;

        while (pos < fileCount) {
            File sourceFile = sourceFiles[pos];
            File destFile = destFiles[pos];
            if ((sourceFile != null) && (destFile != null)) {
                Long id = ids[pos];

                File destRenamed = renameDuplicate(destFile);
                if (osFileMoveOrCopy(move, destRenamed, sourceFile)) itemCount++;
                log(command.getCommand(sourceFile.getAbsolutePath(), destRenamed.getAbsolutePath(), true));

                File sourceSidecar = getSidecar(sourceFile, false);
                if (osFileExists(sourceSidecar)) {
                    File destSidecar = getSidecar(destRenamed, false);
                    if (osFileMoveOrCopy(move, destSidecar, sourceSidecar)) itemCount++;
                }

                sourceSidecar = getSidecar(sourceFile, true);
                if (osFileExists(sourceSidecar)) {
                    File destSidecar = getSidecar(destRenamed, true);
                    if (osFileMoveOrCopy(move, destSidecar, sourceSidecar)) itemCount++;
                }
                addTransactionLog(id, sourceFile.getPath(), now, transaction, destFile.getPath());
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
                log("rem renamed from '", filename, "' to '", candidatePath,"'");
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
        if (osFileExists(source) && source.isFile() && source.canRead()
                && source.canWrite() // to delete after success
                && !osFileExists(dest)
                && osFileCopy(dest, source)) {
            if (osDeleteFile(source)) {
                return true; // move: copy + delete(source) : success
            } else {
                // cannot delete souce: undo copy
                osDeleteFile(dest);
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
            FileUtils.close(in,"_osFileCopy-close");
            FileUtils.close(out,"_osFileCopy-close");
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

    public void addTransactionLog(
            long currentMediaID, String fileFullPath, long modificationDate,
            MediaTransactionLogEntryType mediaTransactionLogEntryType, String commandData) {

        if (FotoLibGlobal.debugEnabled) {
            MediaTransactionLogDto dto = new MediaTransactionLogDto(currentMediaID, fileFullPath, modificationDate,
                mediaTransactionLogEntryType, commandData);
            logger.info(getClass().getSimpleName() + ".addTransactionLog(" + dto.toString() + ")");
        }

    }

    public String getDefaultLogFile() {
        return "apmLog.log";
    }

    public FileCommands createFileCommand() {
        setLogFilePath(getDefaultLogFile());
        openLogfile();
        return this;
    }
}

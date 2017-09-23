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
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.RejectedExecutionException;

import de.k3b.FotoLibGlobal;
import de.k3b.media.IMetaApi;
import de.k3b.media.MetaWriterExifXml;
import de.k3b.transactionlog.MediaTransactionLogDto;
import de.k3b.transactionlog.MediaTransactionLogEntryType;

/**
 * All file processing commands.
 * This module has no android api dependencies to ease junit tests.
 * For Android-api specific code see AndroidFileCommands.
 *
 * Created by k3b on 03.08.2015.
 */
public class FileCommands extends FileProcessor implements  Cloneable {
    private static final Logger logger = LoggerFactory.getLogger(FotoLibGlobal.LOG_TAG);

    public static final int OP_COPY = 1;
    public static final int OP_MOVE = 2;
    public static final int OP_DELETE = 3;
    public static final int OP_RENAME = 4;
    public static final int OP_UPDATE = 5;

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

    /**
     * move (or copy) sourcefiles (with their xmp-sidecar-files) to destdirfolder.
     * Executes autoprocessing (#91: rename, add exif) if destdirfolder
     * contains ".apm"  (autoprocessing data file)
     *
     * @param move false: copy
     * @param destDirFolder where files are moved/copied to
     * @param ids for postprocessing android media-db-update affected content-id-s
     * @param sourceFiles files to me boved/copied
     * @return number of changes
     */
    public int moveOrCopyFilesTo(boolean move, File destDirFolder, Long[] ids, File... sourceFiles) {
        IFileNameProcessor renameProcessor = null;
        IMetaApi exifChanges = null;

        PhotoWorkFlowDto autoProccessData = getPhotoWorkFlowDto(destDirFolder);
        if (autoProccessData != null) {
            renameProcessor = autoProccessData.createFileNameProcessor();
            exifChanges = autoProccessData.getMediaDefaults();
        }

        return moveOrCopyFilesTo(move, renameProcessor, exifChanges, destDirFolder, ids, sourceFiles);
    }

    /**
     * For junit integration test: special version with explicit dependencies.
     *
     * @param move false: copy
     * @param renameProcessor
     * @param exifChanges
     * @param destDirFolder where files are moved/copied to
     * @param ids for postprocessing android media-db-update affected content-id-s
     * @param sourceFiles files to me boved/copied
     * @return number of changes
     */
    public int moveOrCopyFilesTo(boolean move,
                                 IFileNameProcessor renameProcessor, IMetaApi exifChanges,
                                 File destDirFolder, Long[] ids, File... sourceFiles) {
        int result = 0;
        if (canProcessFile(move ? OP_MOVE : OP_COPY)) {
            if (osCreateDirIfNeccessary(destDirFolder)) {
                File[] destFiles = createDestFiles(renameProcessor, destDirFolder, sourceFiles);

                result = moveOrCopyFiles(move, (move ? "mov" : "copy"), exifChanges, ids, destFiles, sourceFiles);

            } else {
                log("rem Target dir '", destDirFolder, "' cannot be created");
            }
        }
        return result;
    }

    private PhotoWorkFlowDto getPhotoWorkFlowDto(File destDirFolder) {
        PhotoWorkFlowDto autoProccessData = null;
        try {
            autoProccessData = new PhotoWorkFlowDto().load(destDirFolder);
        } catch (IOException e) {
            log("cannot load .apm file for '", destDirFolder, "'. ", e.getMessage());
            autoProccessData = null;
        }
        return autoProccessData;
    }

    /** does the copying. also used by unittesting */
    protected int moveOrCopyFiles(boolean move, String what, IMetaApi exifChanges, Long[] ids, File[] destFiles, File[] sourceFiles) {
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

                File destRenamed;
                if (FotoLibGlobal.autoExifEnabled && (exifChanges != null) && (sourceFile.equals(destFile))) {
                    // copy/move with exif changes ==> exif changes only
                    destRenamed = destFile;
                } else {
                    destRenamed = renameDuplicate(destFile);
                }

                if (!FotoLibGlobal.autoExifEnabled || (exifChanges == null)) {
                    // old style move/copy image with sidecarfile(s)
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
                } else {
                    try {
                        MetaWriterExifXml exifProcessor = MetaWriterExifXml.create(sourceFile.getAbsolutePath(), what);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    throw new RuntimeException("do exif changes while copy/move (#93;) not implemented yet");
                }
            }
            pos++;
        }
        int modifyCount = mModifiedDestFiles.size();

        String[] modifiedSourceFiles =  ((mModifiedSrcFiles != null) && (mModifiedSrcFiles.size() > 0)) ? mModifiedSrcFiles.toArray(new String[modifyCount]) : null;

        onPostProcess(what, modifiedSourceFiles, (modifyCount > 0) ? mModifiedDestFiles.toArray(new String[modifyCount]) : null, itemCount, sourceFiles.length, opCode);

        closeLogFile();
        return itemCount;
    }

    private File[] createDestFiles(IFileNameProcessor renameProcessor, File destDirFolder, File... sourceFiles) {
        File[] result = new File[sourceFiles.length];

        int pos = 0;
        File destFile;
        for(File srcFile : sourceFiles) {
            if (renameProcessor != null) {
                destFile = renameProcessor.getNextFile(srcFile, new Date(srcFile.lastModified()), -1);
            } else {
                destFile = new File(destDirFolder, srcFile.getName());
            }
            result[pos++] = destFile;
        }

        return result;
    }

    /** executes os specific move or copy operation and updates the list of modified files */
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

    public void setLogFilePath(String logFilePath) {
        closeLogFile();
        mLogFilePath = logFilePath;
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

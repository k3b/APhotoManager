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

import de.k3b.FotoLibGlobal;
import de.k3b.io.collections.DestDirFileNameProcessor;
import de.k3b.io.collections.SelectedFiles;
import de.k3b.media.IMetaApi;
import de.k3b.media.JpgMetaWorkflow;
import de.k3b.media.MediaDiffCopy;
import de.k3b.media.MetaWriterExifXml;
import de.k3b.transactionlog.MediaTransactionLogEntryDto;
import de.k3b.transactionlog.MediaTransactionLogEntryType;
import de.k3b.transactionlog.TransactionLoggerBase;

/**
 * All file processing commands:
 * * copy/move/delete + modify exif in jpg/xmp + TransactionLog.
 *
 * This module has no android api dependencies to ease junit tests.
 * For Android-api specific code is added via virtual methods in AndroidFileCommands.
 *
 * Created by k3b on 03.08.2015.
 */
public class FileCommands extends FileProcessor implements  Cloneable, IProgessListener {
    private static final Logger logger = LoggerFactory.getLogger(FotoLibGlobal.LOG_TAG);

    public static final int OP_COPY = 1;
    public static final int OP_MOVE = 2;
    public static final int OP_DELETE = 3;
    public static final int OP_RENAME = 4;
    public static final int OP_UPDATE = 5;

    protected ArrayList<String> mModifiedDestFiles;
    protected ArrayList<String> mModifiedSrcFiles;

    // may be set while looping over items to inform client over progress
    private IProgessListener progessListener;

    public FileCommands() {
        setLogFilePath(null);
    }

    public int deleteFiles(SelectedFiles fotos, IProgessListener progessListener) {
        final String dbgContext = "delete";
        int nameCount = fotos.getNonEmptyNameCount();
        int deleteCount = 0;
        if ((nameCount > 0) && canProcessFile(OP_DELETE)) {
            IProgessListener progessListenerOld = this.progessListener;
            this.progessListener = progessListener;
            try {
                long    startTimestamp = 0;
                if (FotoLibGlobal.debugEnabledJpgMetaIo) {
                    startTimestamp = new Date().getTime();
                }

                String[] fileNames = fotos.getFileNames();
                long now = new Date().getTime();

                int itemsPerProgress = FotoLibGlobal.itemsPerProgress;
                int itemcount = 0;
                int countdown = 0;
                int maxCount = fotos.size();

                openLogfile();
                onPreProcess(dbgContext, OP_DELETE, fotos, fileNames, null);
                for (int i = 0; i < maxCount; i++) {
                    File file = fotos.getFile(i);
                    countdown--;
                    if (countdown <= 0) {
                        countdown = itemsPerProgress;
                        if (!onProgress(itemcount, maxCount, (file == null) ? null : file.getAbsolutePath())) break;
                    }

                    if ((file != null) && deleteFileWithSidecar(file)) {
                        deleteCount++;
                        addTransactionLog(fotos.getId(i), file.getAbsolutePath(), now, MediaTransactionLogEntryType.DELETE, null);
                    }
                }
                onPostProcess(dbgContext, OP_DELETE, fotos, deleteCount, fileNames.length, fileNames, null);
                if (FotoLibGlobal.debugEnabledJpg || FotoLibGlobal.debugEnabledJpgMetaIo) {
                    long dbgLoadEndTimestamp = new Date().getTime();

                    FileCommands.logger.debug(dbgContext + " process items:" + deleteCount
                            + ", msecs:" + (dbgLoadEndTimestamp - startTimestamp));
                }

                onProgress(itemcount, maxCount, null);
            } finally {
                closeLogFile();
                this.progessListener = progessListenerOld;
            }
        }
        return deleteCount;
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
            log(MediaTransactionLogEntryType.DELETE.getCommand(file.getAbsolutePath(),""));
        }
        return result;
    }

    /**
     * apply changes in exifChanges to all images in selectedFiles.
     * @return number of changed files.
     */
    public int applyExifChanges(boolean move, MediaDiffCopy exifChanges, SelectedFiles selectedFiles, IProgessListener progessListener) {
        // source files are the same as dest files.
        final File[] destFiles = SelectedFiles.getFiles(selectedFiles.getFileNames());
        return moveOrCopyFiles(move, "change_exif", exifChanges, selectedFiles, destFiles, progessListener);
    }

    /**
     * move (or copy) sourcefiles (with their xmp-sidecar-files) to destdirfolder.
     * Executes autoprocessing (#93: rename, add exif) if destdirfolder
     * contains ".apm"  (autoprocessing data file)
     * @param move false: copy
     * @param selectedFiles
     * @param destDirFolder where files are moved/copied to
     * @param progessListener  */
    public int moveOrCopyFilesTo(boolean move, SelectedFiles selectedFiles, File destDirFolder, IProgessListener progessListener) {
        PhotoWorkFlowDto autoProccessData = (!FotoLibGlobal.apmEnabled) ? null : getPhotoWorkFlowDto(destDirFolder);

        return moveOrCopyFilesTo(move, selectedFiles, destDirFolder, autoProccessData, progessListener);
    }

    /**
     * For junit integration test: special internal version with explicit dependencies.
     * move (or copy) sourcefiles (with their xmp-sidecar-files) to destdirfolder.
     * Executes autoprocessing (#91: rename, add exif) if destdirfolder
     * contains ".apm"  (autoprocessing data file)
     * @param move false: copy
     * @param selectedFiles
     * @param destDirFolder where files are moved/copied to
     * @param autoProccessData null or data for auto rename/exif data
     * @param progessListener  */
    int moveOrCopyFilesTo(boolean move, SelectedFiles selectedFiles, File destDirFolder,
                          PhotoWorkFlowDto autoProccessData, IProgessListener progessListener) {
        boolean doNotRenameIfSourceInDestFolder = false;
        IFileNameProcessor renameProcessor = null;
        MediaDiffCopy exifChanges = null;

        if ((autoProccessData != null) && (!autoProccessData.isEmpty())) {
            doNotRenameIfSourceInDestFolder = true;
            if (!autoProccessData.isRenameEmpty()) renameProcessor = autoProccessData.createFileNameProcessor();
            final IMetaApi mediaDefaults = autoProccessData.getMediaDefaults();
            if (mediaDefaults != null) exifChanges = new MediaDiffCopy(mediaDefaults, false);
        }

        if (renameProcessor == null){
            renameProcessor = new DestDirFileNameProcessor(destDirFolder, doNotRenameIfSourceInDestFolder);
        }

        return moveOrCopyFilesTo(move, exifChanges, selectedFiles, renameProcessor, destDirFolder, progessListener);
    }

    /**
     * For junit integration test: special internal version with explicit dependencies.
     * @param move false: copy
     * @param exifChanges not null: update exif while copying
     * @param selectedFiles
     * @param renameProcessor not null: handles destination filename
     * @param destDirFolder where files are moved/copied to
     * @param progessListener   not null: to show in gui what is happening
     */
    int moveOrCopyFilesTo(boolean move,
                          MediaDiffCopy exifChanges, SelectedFiles selectedFiles, IFileNameProcessor renameProcessor,
                          File destDirFolder, IProgessListener progessListener) {
        int result = 0;
        if (canProcessFile(move ? OP_MOVE : OP_COPY)) {
            if (osCreateDirIfNeccessary(destDirFolder)) {
                File[] destFiles = createDestFiles(renameProcessor, destDirFolder, selectedFiles.getDatesPhotoTaken() , SelectedFiles.getFiles(selectedFiles.getFileNames()));

                result = moveOrCopyFiles(move, (move ? "mov" : "copy"), exifChanges, selectedFiles, destFiles, progessListener);

            } else {
                log("rem Target dir '", destDirFolder, "' cannot be created");
            }
        }
        return result;
    }

    /** does the copying and/or apply exif changes. also used by unittesting */
    protected int moveOrCopyFiles(final boolean move, String what, MediaDiffCopy exifChanges,
                                  SelectedFiles fotos, File[] destFiles,
                                  IProgessListener progessListener) {
        long    startTimestamp = 0;
        if (FotoLibGlobal.debugEnabledJpgMetaIo) {
            startTimestamp = new Date().getTime();
        }

        int itemCount = 0;
        int nameCount = fotos.getNonEmptyNameCount();
        int opCode = (move) ? OP_MOVE : OP_COPY;
        if ((nameCount > 0) && canProcessFile(opCode) && ((exifChanges == null) || canProcessFile(OP_UPDATE))) {
            IProgessListener progessListenerOld = this.progessListener;
            this.progessListener = progessListener;
            try {

                int fileCount = destFiles.length;

                Long[] ids = fotos.getIds();
                File[] sourceFiles = SelectedFiles.getFiles(fotos.getFileNames());

                mModifiedSrcFiles = (move) ? new ArrayList<String>() : null;
                mModifiedDestFiles = new ArrayList<String>();

                int itemsPerProgress = FotoLibGlobal.itemsPerProgress;
                int itemcount = 0;
                int countdown = 0;
                int maxCount = fotos.size();

                openLogfile();
                // onPreProcess(what, sourceFiles, destFiles, opCode);
                onPreProcess(what, opCode, fotos, null, null);
                int pos = 0;
                long now = new Date().getTime();
                MediaTransactionLogEntryType moveOrCopyCommand = (move) ? MediaTransactionLogEntryType.MOVE : MediaTransactionLogEntryType.COPY;
                TransactionLoggerBase logger = (exifChanges == null) ? null : new TransactionLoggerBase(this, now);
                boolean sameFile;

                while (pos < fileCount) {
                    File sourceFile = FileUtils.tryGetCanonicalFile(sourceFiles[pos]);
                    File destFile = FileUtils.tryGetCanonicalFile(destFiles[pos]);
                    Long id = ids[pos];

                    boolean deleteOriginalAfterFinish = move;
                    countdown--;
                    if (countdown <= 0) {
                        countdown = itemsPerProgress;
                        if (!onProgress(itemcount, maxCount, (sourceFile == null) ? null : sourceFile.toString())) break;
                    }

                    File destRenamed;
                    sameFile = (sourceFile != null) && sourceFile.equals(destFile);
                    if ((exifChanges != null) && sameFile) {
                        // copy/move with exif changes ==> exif changes only
                        destRenamed = destFile;
                    } else {
                        destRenamed = renameDuplicate(destFile);
                    }

                    final String sourcePath = FileUtils.tryGetCanonicalPath(sourceFile, null);
                    final String destPath = FileUtils.tryGetCanonicalPath(destRenamed, null);
                    if ((sourcePath != null) && (destPath != null)) {

                        if (exifChanges == null) {
                            // old style move/copy image with sidecarfile(s)
                            if (osFileMoveOrCopy(move, destRenamed, sourceFile)) itemCount++;


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
                            addTransactionLog(id, sourceFile.getPath(), now, moveOrCopyCommand, destFile.getPath());
                        } else { // else move/copy with simultanious exif changes
                            MediaDiffCopy mediaDiffCopy = exifChanges;
                            // new style move/copy image with sidecarfile(s) with exif autoprocessing

                            // for the log the file has already been copied/moved
                            logger.set(id, sourcePath);

                            MetaWriterExifXml exifProcessor = createWorkflow(logger, what).applyChanges(sourceFile, destPath, id, move, mediaDiffCopy);

                            if (exifProcessor == null) break; // error

                            itemCount++;
                            // apply exif; add changes to log and transactionlog
                            // should havebeen done by applyChanges
                            // exifProcessor.save("FileCommands-moveOrCopyFiles-with-exif");

                            String modifiedOutPath = exifProcessor.getAbsoluteJpgOutPath();
                            if (null != modifiedOutPath) {
                                // destFile might have renamed it-s extension for private images
                                destFile = new File(modifiedOutPath);
                                sameFile = (sourceFile != null) && sourceFile.equals(destFile);
                            }

                            addProcessedFiles(move, destFile, sourceFile);
                        }
                    }
                    pos++;
                } // foreach selected file
                int modifyCount = mModifiedDestFiles.size();

                String[] modifiedSourceFiles = ((mModifiedSrcFiles != null) && (mModifiedSrcFiles.size() > 0)) ? mModifiedSrcFiles.toArray(new String[modifyCount]) : null;

                String[] modifiedDestFiles = (modifyCount > 0) ? mModifiedDestFiles.toArray(new String[modifyCount]) : null;
                onPostProcess(what, opCode, fotos, itemCount, sourceFiles.length, modifiedSourceFiles, modifiedDestFiles);
                if (FotoLibGlobal.debugEnabledJpgMetaIo) {
                    long dbgLoadEndTimestamp = new Date().getTime();

                    FileCommands.logger.debug(what + " process items:" + itemCount
                            + ",msecs:" + (dbgLoadEndTimestamp - startTimestamp));
                }

            } finally {

                if (exifChanges != null) {
                    exifChanges.fixTagRepository();
                    exifChanges.close();
                }
                closeLogFile();
                this.progessListener = progessListenerOld;
            }
        }
        return itemCount;
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

    public JpgMetaWorkflow createWorkflow(TransactionLoggerBase logger, String dbgContext) {
        return new JpgMetaWorkflow(logger);
    }

    private File[] createDestFiles(IFileNameProcessor renameProcessor, File destDirFolder, Date[] datesLastModified, File... sourceFiles) {
        File[] result = new File[sourceFiles.length];

        int pos = 0;
        File destFile;
        for(File srcFile : sourceFiles) {
            if (renameProcessor != null) {
                destFile = renameProcessor.getNextFile(srcFile, getRenameSourceFileDate(srcFile, datesLastModified, pos), -1);
            } else {
                destFile = new File(destDirFolder, srcFile.getName());
            }
            result[pos++] = destFile;
        }

        return result;
    }

    private Date getRenameSourceFileDate(File srcFile, Date[] datesLastModified, int pos) {
        if ((datesLastModified != null) && (pos >= 0) && (pos < datesLastModified.length)) {
            return datesLastModified[pos];
        }
        return new Date(srcFile.lastModified());
    }

    /** executes os specific move or copy operation and updates the list of modified files */
    protected boolean osFileMoveOrCopy(boolean move, File dest, File source) {
        boolean result = false;
        long fileTime = source.lastModified();

        if (move) {
            result = osFileMove(dest, source);
        } else {
            result = osFileCopy(dest, source);
        }
        if (dest.lastModified() != fileTime) {
            dest.setLastModified(fileTime);
        }

        if (result) {
            addProcessedFiles(move, dest, source);
        }

        return result;
    }

    private void addProcessedFiles(boolean move, File dest, File source) {
        mModifiedDestFiles.add(dest.getAbsolutePath());
        if (move) {
            mModifiedSrcFiles.add(source.getAbsolutePath());
        }
    }

    /** can be replaced by mock/stub in unittests */
    protected boolean osFileMove(File dest, File source) {
        if (source.renameTo(dest)) {
            // move within same mountpoint
            if (FotoLibGlobal.debugEnabledJpg) {
                logger.info("osFileMove(rename) '" + source
                        + "' => '" + dest + "'");
            }
            return true;
        }

        // #61 cannot move between different mountpoints/devices/partitions. do Copy+Delete instead
        if (osFileExists(source) && source.isFile() && source.canRead()
                && source.canWrite() // to delete after success
                && !osFileExists(dest)
                && osFileCopy(dest, source)) {
            if (osDeleteFile(source)) {
                if (FotoLibGlobal.debugEnabledJpg) {
                    logger.info("osFileMove(copy+delete) '" + source
                            + "' => '" + dest + "'");
                }
                return true; // move: copy + delete(source) : success
            } else {
                // cannot delete souce: undo copy
                if (FotoLibGlobal.debugEnabledJpg) {
                    logger.info("osFileMove failed for  '" + source
                            + "' => '" + dest + "'");
                }
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
    private static boolean _osFileCopy(File targetFullPath, File sourceFullPath, FileCommands owner) {
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
        if (FotoLibGlobal.debugEnabledJpg) {
            logger.info("osFileCopy '" + sourceFullPath
                    + "' => '" + targetFullPath + "' success=" + result);
        }
        return result;
    }

    /** to be replaced by mock/stub in unittests */
    protected boolean osDeleteFile(File file) {
        final boolean result = file.delete();
        if (FotoLibGlobal.debugEnabledJpg) logger.info("osDeleteFile '" + file + "' success=" + result);
        return result;
    }

    /** to be replaced by mock/stub in unittests */
    protected boolean osCreateDirIfNeccessary(File destDirFolder) {
        return destDirFolder.mkdirs() || destDirFolder.isDirectory();
    }

    /** called before copy/move/rename/delete */
    protected void onPreProcess(String what, int opCode, SelectedFiles selectedFiles, String[] oldPathNames, String[] newPathNames) {
        /* can be overwritten */
    }

    /** called for each modified/deleted file */
    protected void onPostProcess(String what, int opCode, SelectedFiles selectedFiles, int modifyCount, int itemCount, String[] oldPathNames, String[] newPathNames) {
        /* can be overwritten */
    }

    /** all logging goes through this.
     * android specific database logging is handled by overwrite AndroidTransactionLogger.addTransactionLog
     * return new id */
    public void addTransactionLog(
        long currentMediaID, String fileFullPath, long modificationDate,
        MediaTransactionLogEntryType mediaTransactionLogEntryType, String commandData) {

        MediaTransactionLogEntryDto dto = new MediaTransactionLogEntryDto(currentMediaID, fileFullPath, modificationDate,
            mediaTransactionLogEntryType, commandData);
        if (FotoLibGlobal.debugEnabledJpg) {
            logger.info(getClass().getSimpleName() + ".addTransactionLog(" + dto.toString() + ")");
        }
        this.log(mediaTransactionLogEntryType.getCommand(fileFullPath, commandData));
    }

    public String getDefaultLogFile() {
        return "apmLog.log";
    }

    public FileCommands createFileCommand() {
        setLogFilePath(getDefaultLogFile());
        openLogfile();
        return this;
    }

    /** called every time when command makes some little progress. Can be mapped to async progress-bar */
    public boolean onProgress(int itemcount, int size, String message) {
        boolean result = true;
        if (progessListener != null) {
            result = progessListener.onProgress(itemcount, size, message);
        }
        return result;
    }
}

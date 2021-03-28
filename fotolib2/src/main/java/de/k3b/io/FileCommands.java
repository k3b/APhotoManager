/*
 * Copyright (c) 2015-2020 by k3b.
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import de.k3b.LibGlobal;
import de.k3b.io.collections.DestDirFileNameProcessor;
import de.k3b.io.collections.SelectedFiles;
import de.k3b.io.filefacade.FileFacade;
import de.k3b.io.filefacade.IFile;
import de.k3b.media.IPhotoProperties;
import de.k3b.media.PhotoPropertiesBulkUpdateService;
import de.k3b.media.PhotoPropertiesDiffCopy;
import de.k3b.media.PhotoPropertiesUpdateHandler;
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
    private static final Logger logger = LoggerFactory.getLogger(LibGlobal.LOG_TAG);
    private static final String mDebugPrefix = "FileCommands-";

    public static final int OP_COPY = 1;
    public static final int OP_MOVE = 2;
    public static final int OP_DELETE = 3;
    public static final int OP_RENAME = 4;
    public static final int OP_UPDATE = 5;

    protected ArrayList<IFile> mModifiedDestFiles;
    protected ArrayList<IFile> mModifiedSrcFiles;

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
                if (LibGlobal.debugEnabledJpgMetaIo) {
                    startTimestamp = new Date().getTime();
                }

                IFile[] selectedFotos = fotos.getIFiles();
                long now = new Date().getTime();

                int itemsPerProgress = LibGlobal.itemsPerProgress;
                int itemcount = 0;
                int countdown = 0;
                int maxCount = fotos.size();

                openLogfile();
                onPreProcess(dbgContext, OP_DELETE, fotos, selectedFotos, null);
                for (int i = 0; i < maxCount; i++) {
                    IFile file = selectedFotos[i];
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
                onPostProcess(dbgContext, OP_DELETE, fotos, deleteCount, selectedFotos.length, selectedFotos, null);
                if (LibGlobal.debugEnabledJpg || LibGlobal.debugEnabledJpgMetaIo) {
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
     * @param sourceFullPath the path of the file that shall be copied including the file name with ending
     * @param targetFullPath the path of the file  that shall be written to with filename
     *                       <p>
     *                       Copies a file from the sourceFullPath path to the target path.
     */
    private static boolean _osFileCopy(IFile targetFullPath, IFile sourceFullPath, FileCommands owner) {
        boolean result = true;
        try {
            result = sourceFullPath.copy(targetFullPath, false);
        } catch (Throwable e) {
            result = false;
            if (owner != null) {
                owner.onException(e, "_osFileCopy", sourceFullPath, targetFullPath);
            }
        }
        if (LibGlobal.debugEnabledJpg) {
            logger.info("osFileCopy '" + sourceFullPath
                    + "' => '" + targetFullPath + "' success=" + result);
        }
        return result;
    }

    /**
     * apply changes in exifChanges to all images in selectedFiles.
     * @return number of changed files.
     */
    public int applyExifChanges(boolean move, PhotoPropertiesDiffCopy exifChanges, SelectedFiles selectedFiles, IProgessListener progessListener) {
        // source files are the same as dest files.
        final IFile[] destFiles = selectedFiles.getIFiles();
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
    public int moveOrCopyFilesTo(boolean move, SelectedFiles selectedFiles, IFile destDirFolder, IProgessListener progessListener) {
        PhotoAutoprocessingDto autoProccessData = (!LibGlobal.apmEnabled) ? null : getPhotoAutoprocessingDto(destDirFolder);

        return moveOrCopyFilesTo(move, selectedFiles, destDirFolder, autoProccessData, progessListener);
    }

    /**
     * For junit integration test: special internal version with explicit dependencies.
     * move (or copy) sourcefiles (with their xmp-sidecar-files) to destdirfolder.
     * Executes autoprocessing (#91: rename, add exif) if autoProccessData is not null
     * @param move false: copy
     * @param selectedFiles
     * @param destDirFolder where files are moved/copied to
     * @param autoProccessData null or data for auto rename/exif data
     * @param progessListener  */
    public int moveOrCopyFilesTo(boolean move, SelectedFiles selectedFiles, IFile destDirFolder,
                                 PhotoAutoprocessingDto autoProccessData, IProgessListener progessListener) {
        boolean doNotRenameIfSourceInDestFolder = false;
        IFileNameProcessor renameProcessor = null;
        PhotoPropertiesDiffCopy exifChanges = null;

        if ((autoProccessData != null) && (!autoProccessData.isEmpty())) {
            doNotRenameIfSourceInDestFolder = true;
            if (!autoProccessData.isRenameEmpty()) renameProcessor = autoProccessData.createFileNameProcessor();
            final IPhotoProperties mediaDefaults = autoProccessData.getMediaDefaults();
            if (mediaDefaults != null) exifChanges = new PhotoPropertiesDiffCopy(mediaDefaults, false);
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
                          PhotoPropertiesDiffCopy exifChanges, SelectedFiles selectedFiles, IFileNameProcessor renameProcessor,
                          IFile destDirFolder, IProgessListener progessListener) {
        int result = 0;
        if (canProcessFile(move ? OP_MOVE : OP_COPY)) {
            if (destDirFolder != null) destDirFolder.cacheStrategy(IFile.STRATEGY_OUTPUT);
            if (osCreateDirIfNeccessary(destDirFolder)) {
                IFile[] destFiles = createDestFiles(renameProcessor, destDirFolder, selectedFiles.getDatesPhotoTaken(), selectedFiles.getIFiles());

                result = moveOrCopyFiles(move, (move ? "mov" : "copy"), exifChanges, selectedFiles, destFiles, progessListener);

            } else {
                log("rem Target dir '", destDirFolder, "' cannot be created");
            }
        }
        return result;
    }

    /**
     * @return true if file was deleted or does not exist (any more)
     */
    protected boolean deleteFileWithSidecar(IFile file) {
        boolean result = false;

        if (file != null) {
            IFile sidecar = XmpFile.getSidecar(file, false);
            if (osFileExists(sidecar)) {
                osDeleteFile(sidecar); // dont care if delete was successfull
            }
            sidecar = XmpFile.getSidecar(file, true);
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
            log(MediaTransactionLogEntryType.DELETE.getCommand(file.getAbsolutePath(), ""));
        }
        return result;
    }

    @Deprecated
    protected int moveOrCopyFiles(final boolean move, String what, PhotoPropertiesDiffCopy exifChanges,
                                  SelectedFiles fotos, File[] destFiles,
                                  IProgessListener progessListener) {
        return moveOrCopyFiles(move, what, exifChanges, fotos,
                FileFacade.get("FileCommands.moveOrCopyFiles", destFiles), progessListener);
    }

    protected TransactionLoggerBase createTransactionLogger(long now) {
        return new TransactionLoggerBase(this, now);
    }

    private PhotoAutoprocessingDto getPhotoAutoprocessingDto(IFile destDirFolder) {
        PhotoAutoprocessingDto autoProccessData = null;
        try {
            autoProccessData = new PhotoAutoprocessingDto().load(destDirFolder);
        } catch (IOException e) {
            log("cannot load .apm file for '", destDirFolder, "'. ", e.getMessage());
            autoProccessData = null;
        }
        return autoProccessData;
    }

    public PhotoPropertiesBulkUpdateService createWorkflow(TransactionLoggerBase logger, String dbgContext) {
        return new PhotoPropertiesBulkUpdateService(logger);
    }

    private IFile[] createDestFiles(IFileNameProcessor renameProcessor, IFile destDirFolder, Date[] datesLastModified, IFile... sourceFiles) {
        IFile[] result = new IFile[sourceFiles.length];

        int pos = 0;
        IFile destFile;
        for (IFile srcFile : sourceFiles) {
            if (renameProcessor != null) {
                destFile = renameProcessor.getNextFile(srcFile, getRenameSourceFileDate(srcFile, datesLastModified, pos), -1);
            } else {
                destFile = destDirFolder.create(srcFile.getName());
            }
            destFile.cacheStrategy(IFile.STRATEGY_OUTPUT);
            result[pos++] = destFile;
        }

        return result;
    }

    private Date getRenameSourceFileDate(IFile srcFile, Date[] datesLastModified, int pos) {
        Date result = (datesLastModified != null && pos >= 0 && pos < datesLastModified.length)
                ? datesLastModified[pos] : null;
        if (result == null) result = new Date(srcFile.lastModified());
        return result;
    }

    /**
     * does the copying and/or apply exif changes. also used by unittesting
     */
    protected int moveOrCopyFiles(final boolean move, String what, PhotoPropertiesDiffCopy exifChanges,
                                  SelectedFiles fotos, IFile[] destFiles,
                                  IProgessListener progessListener) {
        long    startTimestamp = 0;
        if (LibGlobal.debugEnabledJpgMetaIo) {
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
                IFile[] sourceFiles = fotos.getIFiles();

                mModifiedSrcFiles = (move) ? new ArrayList<IFile>() : null;
                mModifiedDestFiles = new ArrayList<>();

                int itemsPerProgress = LibGlobal.itemsPerProgress;
                int itemcount = 0;
                int countdown = 0;
                int maxCount = fotos.size();

                openLogfile();
                // onPreProcess(what, sourceFiles, destFiles, opCode);
                onPreProcess(what, opCode, fotos, null, null);
                int pos = 0;
                long now = new Date().getTime();
                MediaTransactionLogEntryType moveOrCopyCommand = (move) ? MediaTransactionLogEntryType.MOVE : MediaTransactionLogEntryType.COPY;
                TransactionLoggerBase logger = (exifChanges == null) ? null : createTransactionLogger(now);
                boolean sameFile;
                //!!! TODO ??begin transaction??
                while (pos < fileCount) {
                    IFile sourceFile = sourceFiles[pos];
                    IFile destFile = destFiles[pos].getCanonicalFile();
                    Long id = ids[pos];

                    boolean deleteOriginalAfterFinish = move;
                    countdown--;
                    if (countdown <= 0) {
                        countdown = itemsPerProgress;
                        if (!onProgress(itemcount, maxCount, (sourceFile == null) ? null : sourceFile.toString())) break;
                    }

                    IFile destRenamed;
                    sameFile = (sourceFile != null) && sourceFile.equals(destFile);
                    if ((exifChanges != null) && sameFile) {
                        // copy/move with exif changes ==> exif changes only
                        destRenamed = destFile;
                    } else {
                        destRenamed = renameDuplicate(destFile);
                    }

                    final String sourcePath = sourceFile.getCanonicalPath();
                    final String destPath = destRenamed.getCanonicalPath();
                    if ((sourcePath != null) && (destPath != null)) {

                        if (exifChanges == null) {
                            // get info for potential xmp sidecare BEFORE jpg (sourceFile) is moved away
                            String jpgName = sourceFile.getName();//!!!
                            final IFile jpgParentFile = sourceFile.getParentFile();

                            // old style move/copy image with sidecarfile(s)
                            if (osFileMoveOrCopy(move, destRenamed, sourceFile)) itemCount++;

                            IFile sourceSidecar = XmpFile.getSidecar(jpgParentFile, jpgName, false);
                            if (osFileExists(sourceSidecar)) {
                                IFile destSidecar = XmpFile.getSidecar(destRenamed, false);
                                if (osFileMoveOrCopy(move, destSidecar, sourceSidecar)) itemCount++;
                            }

                            sourceSidecar = XmpFile.getSidecar(jpgParentFile, jpgName, true);
                            if (osFileExists(sourceSidecar)) {
                                IFile destSidecar = XmpFile.getSidecar(destRenamed, true);
                                if (osFileMoveOrCopy(move, destSidecar, sourceSidecar)) itemCount++;
                            }
                            addTransactionLog(id, sourceFile.getAbsolutePath(), now, moveOrCopyCommand, destFile.getAbsolutePath());
                        } else { // else move/copy with simultanious exif changes
                            PhotoPropertiesDiffCopy mediaDiffCopy = exifChanges;
                            // new style move/copy image with sidecarfile(s) with exif autoprocessing

                            // for the log the file has already been copied/moved
                            logger.set(id, sourceFile);

                            PhotoPropertiesUpdateHandler exifProcessor = createWorkflow(logger, what)
                                    .applyChanges(sourceFile, destRenamed, id, move, mediaDiffCopy);

                            if (exifProcessor == null) break; // error

                            itemCount++;
                            // apply exif; add changes to log and transactionlog
                            // should havebeen done by applyChanges
                            // exifProcessor.save("FileCommands-moveOrCopyFiles-with-exif");

                            String modifiedOutPath = exifProcessor.getAbsoluteJpgOutPath();
                            if (null != modifiedOutPath) {
                                // destFile might have renamed it-s extension for private images
                                destFile = FileFacade.convert(mDebugPrefix + "moveOrCopyFiles dest", modifiedOutPath);
                                sameFile = (sourceFile != null) && sourceFile.equals(destFile);
                                if (!sameFile && destFile != null) {
                                    destFile.cacheStrategy(IFile.STRATEGY_OUTPUT);
                                }
                            }

                            addProcessedFiles(move, destFile, sourceFile);
                        }
                    }
                    pos++;
                } // foreach selected file.
                // !!! TODO ??COMMIT TRANSACTION??
                int modifyCount = mModifiedDestFiles.size();

                IFile[] modifiedSourceFiles = ((mModifiedSrcFiles != null) && (mModifiedSrcFiles.size() > 0)) ? mModifiedSrcFiles.toArray(new IFile[modifyCount]) : null;

                IFile[] modifiedDestFiles = (modifyCount > 0) ? mModifiedDestFiles.toArray(new IFile[modifyCount]) : null;
                onPostProcess(what, opCode, fotos, itemCount, sourceFiles.length, modifiedSourceFiles, modifiedDestFiles);
                if (LibGlobal.debugEnabledJpgMetaIo) {
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

    /**
     * executes os specific move or copy operation and updates the list of modified files
     *
     * @param sourceFullPath the path of the file that shall be copied including the file name with ending
     * @param targetFullPath the path of the file  that shall be written to with filename
     */
    protected boolean osFileMoveOrCopy(boolean move, IFile targetFullPath, IFile sourceFullPath) {
        boolean result = false;
        long fileTime = sourceFullPath.lastModified();

        if (move) {
            result = osFileMove(targetFullPath, sourceFullPath);
        } else {
            result = osFileCopy(targetFullPath, sourceFullPath);
        }
        if (targetFullPath.lastModified() != fileTime) {
            targetFullPath.setLastModified(fileTime);
        }

        if (result) {
            addProcessedFiles(move, targetFullPath, sourceFullPath);
        }

        return result;
    }

    private void addProcessedFiles(boolean move, IFile dest, IFile source) {
        mModifiedDestFiles.add(dest);
        if (move) {
            mModifiedSrcFiles.add(source);
        }
    }

    /**
     * can be replaced by mock/stub in unittests
     *
     * @param sourceFullPath the path of the file that shall be copied including the file name with ending
     * @param targetFullPath the path of the file  that shall be written to with filename
     */
    protected boolean osFileMove(IFile targetFullPath, IFile sourceFullPath) {
        if (osRenameTo(targetFullPath, sourceFullPath)) {
            // move within same mountpoint
            if (LibGlobal.debugEnabledJpg) {
                logger.info("osFileMove(rename) '" + sourceFullPath
                        + "' => '" + targetFullPath + "'");
            }
            return true;
        }

        // #61 cannot move between different mountpoints/devices/partitions. do Copy+Delete instead
        if (osFileExists(sourceFullPath) && sourceFullPath.isFile() && sourceFullPath.canRead()
                && sourceFullPath.canWrite() // to delete after success
                && !osFileExists(targetFullPath)
                && osFileCopy(targetFullPath, sourceFullPath)) {
            if (osDeleteFile(sourceFullPath)) {
                if (LibGlobal.debugEnabledJpg) {
                    logger.info("osFileMove(copy+delete) '" + sourceFullPath
                            + "' => '" + targetFullPath + "'");
                }
                return true; // move: copy + delete(sourceFullPath) : success
            } else {
                // cannot delete souce: undo copy
                if (LibGlobal.debugEnabledJpg) {
                    logger.info("osFileMove failed for  '" + sourceFullPath
                            + "' => '" + targetFullPath + "'");
                }
                osDeleteFile(targetFullPath);
            }
        }
        return false;
    }

    @Deprecated
    protected boolean osRenameTo(File dest, File source) {
        return osRenameTo(
                FileFacade.convert(mDebugPrefix + "osRenameTo dest", dest),
                FileFacade.convert(mDebugPrefix + "osRenameTo src", source));
    }

    protected boolean osRenameTo(IFile dest, IFile source) {
        return source.renameTo(dest.getName());
    }

    /**
     * Copies a file from the sourceFullPath path to the target path.
     *
     * @param sourceFullPath the path of the file that shall be copied including the file name with ending
     * @param targetFullPath the path of the file  that shall be written to with filename
     */
    protected boolean osFileCopy(IFile targetFullPath, IFile sourceFullPath) {
        return _osFileCopy(targetFullPath, sourceFullPath, this);
    }

    protected boolean osDeleteFile(IFile file) {
        final boolean result = file.delete();
        if (LibGlobal.debugEnabledJpg) logger.info("osDeleteFile '" + file + "' success=" + result);
        return result;
    }

    /**
     * to be replaced by mock/stub in unittests
     */
    protected boolean osCreateDirIfNeccessary(IFile destDirFolder) {
        return destDirFolder.mkdirs() || destDirFolder.isDirectory();
    }

    /** called before copy/move/rename/delete */
    protected void onPreProcess(String what, int opCode, SelectedFiles selectedFiles, IFile[] oldPathNames, IFile[] newPathNames) {
        /* can be overwritten */
    }

    /** called for each modified/deleted file */
    protected void onPostProcess(String what, int opCode, SelectedFiles selectedFiles, int modifyCount, int itemCount, IFile[] oldPathNames, IFile[] newPathNames) {
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
        if (LibGlobal.debugEnabledJpg) {
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

package de.k3b.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by k3b on 03.08.2015.
 */
public class FileCommands implements  Cloneable {
    public static final int OP_COPY = 1;
    public static final int OP_MOVE = 2;
    public static final int OP_DELETE = 3;
    private static final String EXT_SIDECAR = ".xmp";

    private final String mLogFilePath;
    // private static final String LOG_FILE_ENCODING = "UTF-8";
    private PrintWriter mLogFile;
    private ArrayList<String> mModifiedFiles;

    public FileCommands(String logFilePath) {
        mLogFilePath = logFilePath;
        mLogFile = null;

        // openLogfile();
    }

    public int deleteFiles(String... paths) {
        int result = 0;
        openLogfile();
        for(String path : paths) {
            if (deleteFileWitSidecar(new File(path))) result++;
        }
        onPostProcess(paths, result, paths.length, OP_DELETE);
        closeLogFile();
        return result;
    }

    /**
     * @return true if file was deleted or does not exist (any more)
     */
    protected boolean deleteFileWitSidecar(File file) {
        boolean result = false;

        if (file != null) {
            File sidecar = getSidecar(file);
            if (osFileExists(sidecar)) {
                osDeleteFile(sidecar); // dont care if delete was successfull
                log("del \"", sidecar.getAbsolutePath(), "\"");
            }

            if (osFileExists(file)) {
                if (!osDeleteFile(file)) {
                    log("rem file exists. delete failed : ", file.getAbsolutePath());
                } else {
                    log("rem file exists");
                    result = true; // was deleted
                }
            } else {
                log("rem file ", file.getAbsolutePath(), "does not exist");
                result = true; // it is gone
            }
        }
        log("del \"", file.getAbsolutePath(), "\"");
        return result;
    }

    public int moveOrCopyFilesTo(boolean move, File destDirFolder, File... sourceFiles) {
        int opCode = (move) ? OP_MOVE : OP_COPY;
        int result = 0;
        if (osCreateDirIfNeccessary(destDirFolder)) {
            mModifiedFiles = new ArrayList<String>();

            File[] destFiles = createDestFiles(destDirFolder, sourceFiles);

            result = moveOrCopyFiles(move, destFiles, sourceFiles);

        } else {
            log("rem Target dir '", destDirFolder.getAbsolutePath(), "' cannot be created");
        }

        int modifyCount = mModifiedFiles.size();
        onPostProcess((modifyCount > 0) ? mModifiedFiles.toArray(new String[modifyCount]) : null, result, sourceFiles.length, opCode);
        return result;
    }

    /** does the copying. internal to allow unittesting */
    int moveOrCopyFiles(boolean move, File[] destFiles, File[] sourceFiles) {
        openLogfile();
        int itemCount = 0;
        int pos = 0;
        int fileCount = destFiles.length;

        while (pos < fileCount) {
            File sourceFile = sourceFiles[pos];
            File destFile = destFiles[pos];

            File destRenamed = renameDuplicate(destFile);
            if (osFileMoveOrCopy(move, destRenamed, sourceFile)) itemCount++;
            log(((move) ? "move \"" : "copy \""), sourceFile.getAbsolutePath(), "\" \"" , destRenamed.getAbsolutePath(), "\"");

            File sourceSidecar = getSidecar(sourceFile);
            if (osFileExists(sourceSidecar)) {
                File destSidecar = getSidecar(destRenamed);
                if (osFileMoveOrCopy(move, destSidecar, sourceSidecar)) itemCount++;
                log(((move) ? "move \"" : "copy \""), sourceSidecar.getAbsolutePath(), "\" \"" , destSidecar.getAbsolutePath(), "\"");
            }
            pos++;
        }
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

    public boolean isSidecar(File file) {
        if (file == null) return false;
        return isSidecar(file.getAbsolutePath());
    }

    public boolean isSidecar(String name) {
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

        if (suffix >= 0) absolutePath = absolutePath.substring(0, suffix);
        return new File(absolutePath + EXT_SIDECAR);
    }

    private List<File> getDuplicates(File[] files) {
        if (files != null) {
            ArrayList<File> result = new ArrayList<File>();
            for(File file : files) {
                if (osFileExists(file)) {
                    result.add(file);
                }
            }

            if (result.size() > 0) {
                return result;
            }
        }
        return null;
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
                log("rem renamed from '", file.toString(), "' to '", candidatePath, "'");
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
                mModifiedFiles.add(dest.getAbsolutePath());
            }
        } else {
            result = osFileCopy(dest, source);
            if (result) {
                mModifiedFiles.add(dest.getAbsolutePath());
            }
        }
        if (dest.lastModified() != fileTime) {
            dest.setLastModified(fileTime);
        }
        return result;
    }

    /** to be replaced by mock/stub in unittests */
    protected boolean osFileMove(File dest, File source) {
        return source.renameTo(dest);
    }

    /**
     *
     * @param sourceFullPath the path of the file that shall be copied including the file name with ending
     * @param targetFullPath the path of the file  that shall be written to without filename
     *
     * Copies a file from the sourceFullPath path to the target path.
     */
    private static boolean osFileCopy(File targetFullPath, File sourceFullPath) {
        FileChannel in = null, out = null;
        try {
            in = new FileInputStream(sourceFullPath).getChannel();
            out = new FileOutputStream(targetFullPath).getChannel();
            long size = in.size();
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0,	size);
            out.write(buf);
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null)
                try {
                    in.close();
                    if (out != null)
                        out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return false;
    }

    /** to be replaced by mock/stub in unittests */
    protected boolean osDeleteFile(File file) {
        return file.delete();
    }

    /** to be replaced by mock/stub in unittests */
    protected boolean osCreateDirIfNeccessary(File destDirFolder) {
        return destDirFolder.mkdirs() || destDirFolder.isDirectory();
    }

    /** to be replaced by mock/stub in unittests */
    protected boolean osFileExists(File file) {
        return file.exists();
    }

    /** called for each modified/deleted file */
    protected void onPostProcess(String[] paths, int modifyCount, int itemCount, int opCode) {
    }

    public void openLogfile() {
        closeLogFile();
        if (mLogFilePath != null) {
            OutputStream stream = null;
            try {
                File logFile = new File(mLogFilePath);
                if (osFileExists(logFile)) {
                    // open existing in append mode
                    stream = new FileOutputStream(logFile, true);
                    mLogFile = new PrintWriter(stream, true);
                } else {
                    // create new
                    mLogFile = new PrintWriter(logFile, "UTF-8");
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    }

    public void closeLogFile() {
        if (mLogFile != null) {
            mLogFile.close();
            mLogFile = null;
        }
    }

    public void log(Object... messages) {
        if (mLogFile != null) {
            for(Object message : messages) {
                mLogFile.print(message);
            }
            mLogFile.println();
            mLogFile.flush();
        }
    }
}

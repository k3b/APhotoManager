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

    private final String mLogFilePath;
    // private static final String LOG_FILE_ENCODING = "UTF-8";
    private PrintWriter mLogFile;
    private ArrayList<String> mModifiedFiles;

    public FileCommands(String logFilePath) {
        mLogFilePath = logFilePath;
        mLogFile = null;

        // openLogfile();
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

    public void deleteFile(String... paths) {
        openLogfile();
        for(String path : paths) {
            File file = new File(path);
            if (osFileExists(file)) {
                log("rem file osFileExists");
                if (!file.delete()) {
                    log("rem delete failed");
                }
            } else {
                log("rem file does not exist");
            }
            log("del \"" + path + "\"", "");
        }
        onPostProcess(paths);
        closeLogFile();
    }

    public void copyFilesTo(boolean move, File destDirFolder, File... sourceFiles) {
        if (createDirIfNeccessary(destDirFolder)) {
            mModifiedFiles = new ArrayList<String>();

            File[] destFiles = createDestFiles(destDirFolder, sourceFiles);

            copy(move, destFiles, sourceFiles);

        } else {
            log("rem Target dir '" + destDirFolder.getAbsolutePath() + "' cannot be created");
        }

        int modifyCount = mModifiedFiles.size();
        onPostProcess((modifyCount > 0) ? mModifiedFiles.toArray(new String[modifyCount]) : null);
    }

    boolean createDirIfNeccessary(File destDirFolder) {
        return destDirFolder.mkdirs() || destDirFolder.isDirectory();
    }

    /** does the copying. internal to allow unittesting */
    void copy(boolean move, File[] destFiles, File[] sourceFiles) {
        int pos = 0;
        int fileCount = destFiles.length;

        File[] renamedDestFiles = new File[fileCount];
        int[] ids = new int[1];
        while (pos < fileCount) {
            ids[0] = 0;
            File destFile = destFiles[pos];
            File renamed = renameDuplicate(destFile, ids);
            if (renamed != null) {
                if ((pos+1 < fileCount) && (destFiles[pos+1].getAbsolutePath().toLowerCase().endsWith(".xmp"))) {
                    // special handling if xmp sidecar file immediatly following current file
                    // jpg and xmp must have the same file suffix
                    File renamedXmp;
                    boolean xmpExists;
                    do {
                        String renamedFilePath = renamed.getAbsolutePath();
                        int suffix = renamedFilePath.lastIndexOf(".");

                        if (suffix >= 0) renamedFilePath = renamedFilePath.substring(0, suffix);
                        renamedXmp = new File(renamedFilePath + ".xmp");
                        xmpExists = osFileExists(renamedXmp);
                        if (xmpExists) {
                            renamed = renameDuplicate(destFile, ids);
                        }
                    } while (xmpExists);
                    renamedDestFiles[pos] = renamed;
                    osFileMoveOrCopy(move, renamedDestFiles[pos], sourceFiles[pos]);
                    pos++;
                    renamedDestFiles[pos] = renamedXmp;
                    osFileMoveOrCopy(move, renamedDestFiles[pos], sourceFiles[pos]);
                    pos++;
                } else {// if not xmp sidecar
                    renamedDestFiles[pos] = renamed;
                    osFileMoveOrCopy(move, renamedDestFiles[pos], sourceFiles[pos]);
                    pos++;
                }
            } else { // not renamed: use original
                renamedDestFiles[pos] = destFile;
                osFileMoveOrCopy(move, renamedDestFiles[pos], sourceFiles[pos]);
                pos++;
            }
        }
    }

    /** to be replaced by mock/stub in unittests */
    boolean osFileExists(File file) {
        return file.exists();
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
     * @param lastID lastID[0] is the last id tried.
     * @return null if rename is not neccessary else new name
     */
    private File renameDuplicate(File file, int[] lastID) {
        if (!osFileExists(file)) {
            // rename is not neccessary
            return null;
        }

        String filename = file.getAbsolutePath();
        String extension = ")";
        int extensionPosition = filename.lastIndexOf(".");
        if (extensionPosition >= 0) {
            extension = ")" + filename.substring(extensionPosition);
            filename = filename.substring(0, extensionPosition) + "(";
        }
        int id = lastID[0];
        while (true) {
            id++;
            String candidatePath = filename + id + extension;
            File candidate = new File(candidatePath);
            if (!osFileExists(candidate)) {
                log("renamed from '", filename, "' to '", candidatePath, "'");
                lastID[0] = id;
                return candidate;
            }

        }
    }

    protected void osFileMoveOrCopy(boolean move, File dest, File source) {
        if (move) {
            osFileMove(dest, source);
            mModifiedFiles.add(source.getAbsolutePath());
            mModifiedFiles.add(dest.getAbsolutePath());
        } else {
            osFileCopy(dest, source);
            mModifiedFiles.add(dest.getAbsolutePath());
        }
    }

    private void osFileMove(File dest, File source) {
        source.renameTo(dest);
    }

    /**
     *
     * @param sourceFullPath the path of the file that shall be copied including the file name with ending
     * @param targetDir the path of the file  that shall be written to without filename
     *
     * Copies a file from the sourceFullPath path to the target path
     */
    private static void osFileCopy(File targetDir, File sourceFullPath) {
        FileChannel in = null, out = null;
        try {
            File target = new File(targetDir, sourceFullPath.getName());
            in = new FileInputStream(sourceFullPath).getChannel();
            out = new FileOutputStream(target).getChannel();
            long size = in.size();
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0,	size);
            out.write(buf);
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
    }

    /** called for each modified/deleted file */
    protected void onPostProcess(String[] paths) {
    }

    public void log(String... messages) {
        if (mLogFile != null) {
            for(String message : messages) {
                mLogFile.println(message);
            }
            mLogFile.flush();
        }
    }

}

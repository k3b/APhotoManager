/*
 * Copyright (c) 2020 by k3b.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * {@link FileFacade} has the same methods as {@link File} so it may become a
 * replacement (aka man-in-the-middle-attack to
 * add support for Android DocumentFile
 */
public class FileFacade implements IFile {
    public static final String LOG_TAG = "k3b.FileFacade";
    private static final Logger logger = LoggerFactory.getLogger(LOG_TAG);
    public static boolean debugLogFacade = false;
    private static Converter<File, IFile> fileFacade = new Converter<File, IFile>() {
        @Override
        public IFile convert(String dbgContext, File file) {
            final IFile result = new FileFacade(file);
            if (debugLogFacade) {
                logger.info(dbgContext + " convert => " + result);
            }
            return result;
        }
    };
    private File file;

    @Override
    public boolean equals(Object o) {
        if (o instanceof File) return this.file.equals(o);
        if (o instanceof FileFacade) return this.file.equals(((FileFacade) o).file);
        if (o instanceof FileWrapper) return equals(((FileWrapper) o).child);
        return super.equals(o);
    }

    public FileFacade(File file) {
        this.file = file;
    }

    public FileFacade(String absolutPath) {
        this(new File(absolutPath));
    }

    public FileFacade(FileFacade parent, String newFolderName) {
        this(new File(parent.file, newFolderName));
    }

    public FileFacade(String parent, String newFolderName) {
        this(new File(parent, newFolderName));
    }

    public static IFile[] get(String dbgContext, File[] files) {
        IFile f[] = new FileFacade[files.length];
        for (int i = 0; i < files.length; i++) {
            f[i] = convert(dbgContext, files[i]);
        }

        return f;
    }

    /**
     * gets existing file from parent or create it if not found
     */
    public static IFile getOrCreateChild(String dbgContext, IFile parent, String name) {
        IFile result = parent.findExisting(name);
        if (result == null) {
            result = parent.create(name);
        }
        if (debugLogFacade) {
            logger.info(dbgContext + " getOrCreateChild(" + name + ") => " + result);
        }
        return result;
    }

    public static IFile convert(String dbgContext, File file) {
        if (file == null) return null;
        return fileFacade.convert(dbgContext, file);
    }

    public static IFile convert(String dbgContext, String filePath) {
        if (filePath == null) return null;
        return convert(dbgContext, new File(filePath));
    }

    public static void setFileFacade(Converter<File, IFile> fileFacade) {
        FileFacade.fileFacade = fileFacade;
    }

    @Deprecated
    @Override
    public boolean renameTo(IFile newName) {
        return renameImpl(((FileFacade) newName).file);
    }

    @Override
    public boolean renameTo(String newName) {
        File newFile = new File(this.file.getParentFile(), newName);
        final boolean result = renameImpl(newFile);
        return result;
    }

    private boolean renameImpl(File newFile) {
        final boolean success = this.file.renameTo(newFile);
        if (success) {
            this.file = newFile;
        }
        return success;
    }

    @Override
    public boolean delete() {
        return file.delete();
    }

    @Override
    public boolean exists() {
        return file != null && file.exists();
    }

    @Override
    public IFile findExisting(String name) {
        final File candidate = new File(this.file, name);
        if (candidate.exists()) {
            return convert("findExisting", candidate);
        }
        return null;
    }

    @Override
    public boolean canWrite() {
        return file.canWrite();
    }

    @Override
    public boolean canRead() {
        return file.canRead();
    }

    @Override
    public boolean isFile() {
        return file.isFile();
    }

    @Override
    public boolean isDirectory() {
        return file.isDirectory();
    }

    @Override
    public boolean isHidden() {
        return file.isHidden();
    }

    @Override
    public boolean isAbsolute() {
        return file.isAbsolute();
    }

    @Override
    public String getAbsolutePath() {
        return file.getAbsolutePath();
    }

    @Override
    public IFile getCanonicalFile() {
        return convert("getCanonicalFile", FileUtils.tryGetCanonicalFile(file));
    }

    @Override
    public String getCanonicalPath() {
        return FileUtils.tryGetCanonicalPath(file, null);
    }

    @Override
    public IFile getParentFile() {
        return convert("getParentFile", file.getParentFile());
    }

    @Override
    public String getParent() {
        return file.getParent();
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public long lastModified() {
        return file.lastModified();
    }

    @Override
    public void setLastModified(long fileTime) {
        file.setLastModified(fileTime);
    }

    @Override
    public boolean mkdirs() {
        return file.mkdirs();
    }

    @Override
    public IFile[] listFiles() {
        return get(null, file.listFiles());
    }

    @Override
    public boolean copy(IFile targetFullPath, boolean deleteSourceWhenSuccess) throws IOException {
        return copyImpl((FileFacade) targetFullPath, deleteSourceWhenSuccess);
    }

    private boolean copyImpl(FileFacade targetFullPath, boolean deleteSourceWhenSuccess) throws IOException {
        boolean success = true;
        FileChannel in = null;
        FileChannel out = null;
        try {
            in = new FileInputStream(file).getChannel();
            out = new FileOutputStream((targetFullPath).file).getChannel();
            long size = in.size();
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
            success = size == out.write(buf);
        } finally {
            FileUtils.close(in, "_osFileCopy-close");
            FileUtils.close(out, "_osFileCopy-close");
        }
        if (success && deleteSourceWhenSuccess) {
            this.delete();
        }
        return success;
    }

    @Override
    public OutputStream openOutputStream() throws FileNotFoundException {
        // create parent dirs if not exist
        file.getParentFile().mkdirs();

        // replace existing
        file.delete();

        return new FileOutputStream(file);
    }

    @Override
    public InputStream openInputStream() throws FileNotFoundException {
        return new FileInputStream(file);
    }

    /**
     * @return null if file already exist
     */
    @Override
    public IFile create(String name) {
        final File file = new File(this.file, name);
        if (!file.exists()) {
            return convert("create", file);
        }
        logger.error("create " + this + "/" + name + " failed already exists");
        return null;
    }

    @Override
    public String toString() {
        return String.format("%s: %s", this.getClass().getSimpleName(), file.getAbsoluteFile());
    }

    public File getFile() {
        if (debugLogFacade) {
            logger.info("getFile() " + file + " from " + this);
        }
        return file;
    }

    protected void setFile(File file) {
        this.file = file;
    }
}

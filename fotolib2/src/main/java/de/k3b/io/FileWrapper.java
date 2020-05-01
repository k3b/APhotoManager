package de.k3b.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileWrapper implements IFile {
    protected final IFile child;

    public FileWrapper(IFile child) {
        this.child = child;
    }

    @Override
    public boolean renameTo(IFile newName) {
        return child.renameTo(newName);
    }

    @Override
    public boolean renameTo(String newName) {
        return child.renameTo(newName);
    }

    @Override
    public boolean delete() {
        return child.delete();
    }

    @Override
    public boolean exists() {
        return child.exists();
    }

    @Override
    public boolean canWrite() {
        return child.canWrite();
    }

    @Override
    public boolean canRead() {
        return child.canRead();
    }

    @Override
    public boolean isFile() {
        return child.isFile();
    }

    @Override
    public boolean isDirectory() {
        return child.isDirectory();
    }

    @Override
    public boolean isHidden() {
        return child.isHidden();
    }

    @Override
    public boolean isAbsolute() {
        return child.isAbsolute();
    }

    @Override
    public String getAbsolutePath() {
        return child.getAbsolutePath();
    }

    @Override
    public IFile getCanonicalFile() {
        return child.getCanonicalFile();
    }

    @Override
    public String getCanonicalPath() {
        return child.getCanonicalPath();
    }

    @Override
    public IFile getParentFile() {
        return child.getParentFile();
    }

    @Override
    public String getParent() {
        return child.getParent();
    }

    @Override
    public String getName() {
        return child.getName();
    }

    @Override
    public void setLastModified(long fileTime) {
        child.setLastModified(fileTime);
    }

    @Override
    public long lastModified() {
        return child.lastModified();
    }

    @Override
    public boolean mkdirs() {
        return child.mkdirs();
    }

    @Override
    public IFile[] listFiles() {
        return child.listFiles();
    }

    @Override
    public boolean copy(IFile targetFullPath, boolean deleteSourceWhenSuccess) throws IOException {
        return child.copy(targetFullPath, deleteSourceWhenSuccess);
    }

    @Override
    public OutputStream openOutputStream() throws FileNotFoundException {
        return child.openOutputStream();
    }

    @Override
    public InputStream openInputStream() throws FileNotFoundException {
        return child.openInputStream();
    }

    @Override
    public IFile create(String name) {
        return child.create(name);
    }

    @Override
    public File getFile() {
        return child.getFile();
    }

    @Override
    public long length() {
        return child.length();
    }

    @Override
    public boolean equals(Object o) {
        return child.equals(o);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + child.toString();
    }
}

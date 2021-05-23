package de.k3b.androidx.documentfile;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.k3b.io.filefacade.FileFacade;
import de.k3b.io.filefacade.FileWrapper;
import de.k3b.io.filefacade.IFile;

/**
 * Inheritance layer to make DocumentFileFacade compatible with IFile
 */
public abstract class DocumentFileFacade extends DocumentFileOrininal implements IFile {
    protected File mFile = null;

    protected DocumentFileFacade(@Nullable DocumentFileEx parent) {
        super(parent);
        if (parent != null) {
            mFile = new File(parent.mFile, getName());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof File) return this.mFile.equals(o);
        if (o instanceof FileFacade) return this.mFile.equals(((FileFacade) o).getFile());
        if (o instanceof FileWrapper) return equals(((FileWrapper) o).getChild());
        return super.equals(o);
    }


    @Override
    public void set(IFile src) {
        if (src != null) {
            if (src instanceof FileWrapper) {
                set(((FileWrapper) src).getChild());
            }
            mFile = src.getFile();
        }
    }

    @Override
    public boolean isHidden() {
        String name = getName();
        return name == null || name.startsWith(".");
    }

    @Override
    public boolean isAbsolute() {
        return mFile.isAbsolute();
    }

    @Override
    public String getAbsolutePath() {
        return mFile.getAbsolutePath();
    }

    @Override
    public IFile getCanonicalFile() {
        return null;
    }

    @Override
    public String getCanonicalPath() {
        return null;
    }

    @Override
    public String getAsUriString() {
        return null;
    }

    @Override
    public void setReadUri(String readUri) {

    }

    @Override
    public String getParent() {
        return null;
    }

    @Override
    public void setLastModified(long fileTime) {

    }

    @Override
    public boolean mkdirs() {
        return false;
    }

    @Override
    public IFile[] listIFiles() {
        DocumentFileEx[] files = listFiles();
        IFile[] result = new IFile[files.length];
        for (int i = files.length - 1; i >= 0; i--) {
            result[i] = files[i];
        }
        return result;
    }

    @Override
    public IFile[] listIDirs() {
        return new IFile[0];
    }

    @Override
    public boolean copy(IFile targetFullPath, boolean deleteSourceWhenSuccess) throws IOException {
        return false;
    }

    @Override
    public OutputStream openOutputStream() throws FileNotFoundException {
        return null;
    }

    @Override
    public InputStream openInputStream() throws FileNotFoundException {
        return null;
    }

    @Override
    public IFile createFile(String name) {
        return null;
    }

    @Override
    public File getFile() {
        return null;
    }

    @Override
    public IFile invalidateParentDirCache() {
        return null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "uri='" + getUri() +
                ", file='" + mFile +
                "'}";
    }
}

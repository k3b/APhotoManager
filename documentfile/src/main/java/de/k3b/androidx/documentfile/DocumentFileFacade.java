package de.k3b.androidx.documentfile;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.k3b.io.filefacade.IFile;

public abstract class DocumentFileFacade extends DocumentFileOrininal implements IFile {
    protected DocumentFileFacade(@Nullable DocumentFileEx parent) {
        super(parent);
    }

    @Override
    public void set(IFile src) {

    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public boolean isAbsolute() {
        return false;
    }

    @Override
    public String getAbsolutePath() {
        return null;
    }

    @Override
    public IFile getCanonicalIFile() {
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
    public IFile createIFile(String name) {
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
}

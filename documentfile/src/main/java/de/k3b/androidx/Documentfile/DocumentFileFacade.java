package de.k3b.androidx.Documentfile;

import android.net.Uri;

import androidx.annotation.NonNull;
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

    @Nullable

    public DocumentFileEx createFile(@NonNull String mimeType, @NonNull String displayName) {
        return null;
    }

    @Nullable

    public DocumentFileEx createDirectory(@NonNull String displayName) {
        return null;
    }


    public Uri getUri() {
        return null;
    }

    @Nullable

    public String getName() {
        return null;
    }

    public void setLastModified(long fileTime) {

    }

    @Nullable

    public String getType() {
        return null;
    }


    public boolean isDirectory() {
        return false;
    }

    public boolean isHidden() {
        return false;
    }

    public boolean isAbsolute() {
        return false;
    }

    public String getAbsolutePath() {
        return null;
    }

    public IFile getCanonicalIFile() {
        return null;
    }


    public DocumentFileEx getParentFile() {
        return null; // super.getParentFile();
    }

    public String getCanonicalPath() {
        return null;
    }

    public String getAsUriString() {
        return null;
    }


    public void setReadUri(String readUri) {

    }


    public String getParent() {
        return null;
    }


    public boolean isFile() {
        return false;
    }


    public boolean isVirtual() {
        return false;
    }


    public long lastModified() {
        return 0;
    }


    public boolean mkdirs() {
        return false;
    }


    public long length() {
        return 0;
    }


    public IFile invalidateParentDirCache() {
        return null;
    }


    public boolean canRead() {
        return false;
    }


    public boolean canWrite() {
        return false;
    }


    public boolean delete() {
        return false;
    }


    public boolean exists() {
        return false;
    }


    public IFile[] listIFiles() {
        return new IFile[0];
    }


    public IFile[] listIDirs() {
        return new IFile[0];
    }


    public boolean copy(IFile targetFullPath, boolean deleteSourceWhenSuccess) throws IOException {
        return false;
    }


    public OutputStream openOutputStream() throws FileNotFoundException {
        return null;
    }


    public InputStream openInputStream() throws FileNotFoundException {
        return null;
    }


    public IFile createIFile(String name) {
        return null;
    }


    public File getFile() {
        return null;
    }


    public void set(IFile src) {

    }


    public boolean renameTo(@NonNull String displayName) {
        return false;
    }
}

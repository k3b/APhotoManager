package de.k3b.androidx.documentfile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import de.k3b.io.filefacade.DirectoryFilter;
import de.k3b.io.filefacade.FileFacade;
import de.k3b.io.filefacade.FileWrapper;
import de.k3b.io.filefacade.IFile;

/**
 * Inheritance layer that make DocumentFileFacade compatible with IFile
 */
public abstract class DocumentFileFacade extends DocumentFileOrininal implements IFile {
    /**
     * the j2se file equivalent to DocumentFileEx. Recalculated on Demand in getFile
     */
    protected File mFile = null;

    protected DocumentFileFacade(@Nullable DocumentFileEx parent) {
        super(parent);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof File) return o.equals(getFile());
        if (o instanceof FileFacade && getFile() != null)
            return getFile().equals(((FileFacade) o).getFile());
        if (o instanceof FileWrapper) return equals(((FileWrapper) o).getChild());
        return super.equals(o);
    }


    @Override
    public void set(IFile src) {
        if (src != null && src instanceof FileWrapper) {
            set(((FileWrapper) src).getChild());
        }
    }

    @Override
    public boolean isHidden() {
        String name = getName();
        return name == null || name.startsWith(".");
    }

    @Override
    public boolean isAbsolute() {
        File file = getFile();
        return (file != null) && file.isAbsolute();
    }

    @Override
    public String getAbsolutePath() {
        File file = getFile();
        return (file != null) ? file.getAbsolutePath() : null;
    }

    @Override
    public IFile getCanonicalFile() {
        return this;
    }

    @Override
    public String getCanonicalPath() {
        return getAbsolutePath();
    }

    @Override
    public String getAsUriString() {
        return getUri().toString();
    }

    @Override
    public void setReadUri(String readUri) {
    }

    @Override
    public String getParent() {
        File file = getFile();
        return (file != null) ? file.getParent() : null;
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
        return toIFiles(listFiles());
    }

    @NonNull
    protected IFile[] toIFiles(DocumentFileEx[] files) {
        IFile[] result = new IFile[files.length];
        for (int i = files.length - 1; i >= 0; i--) {
            result[i] = files[i];
        }
        return result;
    }

    @Override
    public IFile[] listIDirs() {
        List<IFile> found = new ArrayList<>();
        for (DocumentFileEx file : listFiles()) {
            if (file != null &&
                    (file.isDirectory() || DirectoryFilter.accept(file.getName().toLowerCase()))) {
                found.add(file);
            }
        }
        return found.toArray(new IFile[found.size()]);
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
        if (mFile == null) {
            // calculate on demand from parent file
            DocumentFileEx parentDoc = getParentFile();
            File parentFile = (parentDoc == null) ? null : parentDoc.getFile();
            if (parentFile != null) {
                mFile = new File(parentFile, getName());
            }
        }
        return mFile;
    }

    /**
     * @deprecated IFile.invalidateParentDirCache() will be removed
     */
    @Deprecated
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

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
package de.k3b.android.io;

import android.support.v4.provider.DocumentFile;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.k3b.io.FileFacade;
import de.k3b.io.FileUtils;
import de.k3b.io.IFile;

/**
 * de.k3b.android.io.AndroidFileFacade havs the same methods as java.io.File
 * but is implemented through Android specific {@link DocumentFile}
 */
public class AndroidFileFacade extends FileFacade {
    private static DocumentFileTranslator documentFileTranslator = null;
    private DocumentFile androidFile;

    private AndroidFileFacade(DocumentFile parentFile, File parentFile1) {
        super(parentFile1);
        androidFile = parentFile;
    }

    public AndroidFileFacade(File file) {
        this(documentFileTranslator.getDocumentFileOrDir(file, null), file);
    }

    public static void setContext(DocumentFileTranslator documentFileTranslator) {
        AndroidFileFacade.documentFileTranslator = documentFileTranslator;
    }

    @Override
    public boolean renameTo(IFile newName) {
        if (exists() && !newName.exists()) {
            if (getParentFile().equals(newName.getParentFile())) {
                // same directory
                return renameTo(newName.getName());
            }

            if (copyImpl((AndroidFileFacade) newName, true)) {
                setFile(newName.getFile());
                return true;
            }
        }
        Log.e(TAG, "renameTo " + this + " -> " + newName + " failed");
        return false;
    }

    private boolean copyImpl(AndroidFileFacade targetFullPath, boolean deleteSourceWhenSuccess) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = openInputStream();
            out = targetFullPath.openOutputStream();
            FileUtils.copy(in, out);
        } catch (IOException ex) {
            Log.e(TAG, "copyImpl " + this + " -> " + targetFullPath + " failed", ex);
            return false;
        } finally {
            FileUtils.close(in, this);
            FileUtils.close(out, targetFullPath);
        }
        if (deleteSourceWhenSuccess) {
            this.delete();
        }
        return true;
    }

    @Override
    public boolean renameTo(String newName) {
        if (exists() && androidFile.renameTo(newName)) {
            setFile(new File(getFile().getParentFile(), newName));
            return true;
        }

        Log.e(TAG, "renameTo " + this + " -> " + newName + " failed");
        return false;
    }

    @Override
    public boolean delete() {
        return exists() && androidFile.delete();
    }

    private void notImplemented() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public boolean exists() {
        return (androidFile != null) && (androidFile.exists());
    }

    @Override
    public IFile findExisting(String name) {
        DocumentFile doc = androidFile.findFile(name);
        if (doc != null) {
            return new AndroidFileFacade(doc, new File(getFile(), name));
        }
        return null;
    }

    @Override
    public boolean canWrite() {
        return (androidFile != null) && androidFile.canWrite();
    }

    @Override
    public boolean canRead() {
        return (androidFile != null) && androidFile.canRead();
    }

    @Override
    public boolean isFile() {
        return (androidFile != null) && androidFile.isFile();
    }

    @Override
    public boolean isDirectory() {
        return (androidFile != null) && androidFile.isDirectory();
    }

    @Override
    public boolean isHidden() {
        if ((androidFile != null)) {
            final String name = androidFile.getName();
            return (name == null) || name.startsWith(".");
        }
        return true;
    }

    @Override
    public IFile getCanonicalFile() {
        return this;
    }

    @Override
    public IFile getParentFile() {
        return new AndroidFileFacade(androidFile.getParentFile(), getFile().getParentFile());
    }

    @Override
    public String getName() {
        return androidFile.getName();
    }

    @Override
    public long lastModified() {
        return androidFile.lastModified();
    }

    @Override
    public boolean mkdirs() {
        return null != documentFileTranslator.getOrCreateDirectory(getFile());
    }

    @Override
    public IFile[] listFiles() {
        return get(androidFile.listFiles());
    }

    @Override
    public boolean copy(IFile targetFullPath, boolean deleteSourceWhenSuccess) throws IOException {
        return copyImpl((AndroidFileFacade) targetFullPath, deleteSourceWhenSuccess);
    }

    @Override
    public OutputStream openOutputStream() throws FileNotFoundException {
        return documentFileTranslator.createOutputStream(androidFile);
    }

    @Override
    public InputStream openInputStream() throws FileNotFoundException {
        return documentFileTranslator.openInputStream(androidFile);
    }

    @Override
    public String getMime() {
        notImplemented();
        return null;
    }

    /**
     * overwrite existing
     *
     * @param name
     * @param mime
     */
    @Override
    public IFile create(String name, String mime) {
        if (null == findExisting(name)) {
            return new AndroidFileFacade(androidFile.createFile(mime, name), new File(getFile(), name));
        }
        Log.e(TAG, "create " + this + "/" + name + " failed");
        return null;
    }

    private IFile[] get(DocumentFile[] docs) {
        AndroidFileFacade f[] = new AndroidFileFacade[docs.length];
        final File parent = getFile();
        for (int i = 0; i < docs.length; i++) {
            final DocumentFile doc = docs[i];
            f[i] = new AndroidFileFacade(doc, new File(parent, doc.getName()));
        }

        return f;
    }

}

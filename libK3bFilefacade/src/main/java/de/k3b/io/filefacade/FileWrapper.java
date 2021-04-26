/*
 * Copyright (c) 2020-2021 by k3b.
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
package de.k3b.io.filefacade;

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
    public void set(IFile src) {
        child.set(src);
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
    public String getAsUriString() {
        return child.getAsUriString();
    }

    @Override
    public void setReadUri(String readUri) {
        child.setReadUri(readUri);
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
    public IFile[] listDirs() {
        return child.listDirs();
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
    public void invalidateParentDirCache() {
        child.invalidateParentDirCache();
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

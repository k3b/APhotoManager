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
package de.k3b.io.filefacade;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Goal: to become an android independant replacement for java.io.File
 * that can be implemented by android independant de.k3b.io.File
 * and android specific de.k3b.android.io....
 */
public interface IFile {
    @Deprecated
    boolean renameTo(IFile newName);

    boolean renameTo(String newName);

    boolean delete();

    boolean exists();

    boolean canWrite();

    boolean canRead();

    boolean isFile();

    boolean isDirectory();

    boolean isHidden();

    boolean isAbsolute();

    String getAbsolutePath();

    IFile getCanonicalFile();

    String getCanonicalPath();

    IFile getParentFile();

    String getParent();

    String getName();

    void setLastModified(long fileTime);
    long lastModified();

    boolean mkdirs();

    IFile[] listFiles();

    boolean copy(IFile targetFullPath, boolean deleteSourceWhenSuccess) throws IOException;

    OutputStream openOutputStream() throws FileNotFoundException;

    InputStream openInputStream() throws FileNotFoundException;

    /**
     * @return null if file already exist
     */
    IFile create(String name);

    File getFile();

    long length();
}

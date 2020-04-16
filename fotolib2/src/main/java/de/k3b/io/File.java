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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/* de.k3b.io.File have the same methods as java.io.File so it may become a
replacement (aka man-in-the-middle-attack to
add support for Android DocumentFile
 */
public class File {
    public final java.io.File file;

    public File(java.io.File file) {
        this.file = file;
    }

    public File(String absolutPath) {
        this(new java.io.File(absolutPath));
    }

    public File(File parent, String newFolderName) {
        this(new java.io.File(parent.file, newFolderName));
    }

    public File(String parent, String newFolderName) {
        this(new java.io.File(parent, newFolderName));
    }

    public static File[] get(java.io.File[] files) {
        File f[] = new File[files.length];
        for (int i = 0; i < files.length; i++) {
            f[i] = new File(files[i]);
        }

        return f;
    }

    public boolean renameTo(File newName) {
        return file.renameTo(newName.file);
    }

    public boolean delete() {
        return file.delete();
    }

    public boolean exists() {
        return file.exists();
    }

    public boolean canWrite() {
        return file.canWrite();
    }

    public boolean canRead() {
        return canRead();
    }

    public boolean isFile() {
        return file.isFile();
    }

    public boolean isDirectory() {
        return file.isDirectory();
    }

    public boolean isHidden() {
        return file.isHidden();
    }

    public boolean isAbsolute() {
        return file.isAbsolute();
    }

    public String getAbsolutePath() {
        return file.getAbsolutePath();
    }

    public File getCanonicalFile() {
        return new File(FileUtils.tryGetCanonicalFile(file));
    }

    public String getCanonicalPath() {
        return FileUtils.tryGetCanonicalPath(file, null);
    }

    public File getParentFile() {
        return new File(file.getParentFile());
    }

    public String getParent() {
        return file.getParent();
    }

    public String getName() {
        return file.getName();
    }

    public long lastModified() {
        return file.lastModified();
    }

    public boolean mkdirs() {
        return file.mkdirs();
    }

    public File[] listFiles() {
        return get(file.listFiles());
    }

    public void copy(File targetFullPath, boolean deleteSourceWhenSuccess) throws IOException {
        FileChannel in = null;
        FileChannel out = null;
        try {
            in = new FileInputStream(file).getChannel();
            out = new FileOutputStream(targetFullPath.file).getChannel();
            long size = in.size();
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
            out.write(buf);
        } finally {
            FileUtils.close(in, "_osFileCopy-close");
            FileUtils.close(out, "_osFileCopy-close");
        }
        if (deleteSourceWhenSuccess) {
            this.delete();
        }
    }

    public OutputStream openOutputStream(boolean var2) throws FileNotFoundException {
        return new FileOutputStream(file, var2);
    }

    public InputStream openInputStream() throws FileNotFoundException {
        return new FileInputStream(file);
    }

}

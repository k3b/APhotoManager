/*
 * Copyright (c) 2020 by k3b.
 *
 * This file is part of AndroFotoFinder / #APhotoManager.
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.k3b.android.widget.FilePermissionActivity;
import de.k3b.io.IFile;
import de.k3b.media.ExifInterfaceEx;
import de.k3b.media.IPhotoProperties;

/**
 * Overwrite of {@link ExifInterfaceEx} that replaces {@link java.io.File} implementation
 * with android specific {@link android.support.v4.provider.DocumentFile}
 */
public class ExifInterfaceExAndroid extends ExifInterfaceEx {
    private static final String MIMNE = "image/jpeg";
    private static DocumentFileTranslator documentFileTranslator = null;

    private ExifInterfaceExAndroid(
            String absoluteJpgPath, InputStream in,
            IPhotoProperties xmpExtern, String dbg_context) throws IOException {
        super(absoluteJpgPath, in, xmpExtern, dbg_context);
    }

    public static void initFactory() {
        factory = new Factory() {
            public ExifInterfaceEx create(
                    String absoluteJpgPath, InputStream in, IPhotoProperties xmpExtern,
                    String dbg_context) throws IOException {
                return new ExifInterfaceExAndroid(absoluteJpgPath, in, xmpExtern, dbg_context);
            }
        };
    }

    public static void setContext(FilePermissionActivity activity) {
        documentFileTranslator = activity.getDocumentFileTranslator();
    }

    /**
     * @deprecated use {@link #saveAttributes(IFile, IFile, boolean)} instead
     */
    @Deprecated
    @Override
    public void saveAttributes(File inFile, File outFile, boolean deleteInFileOnFinish) throws IOException {
        super.saveAttributes(inFile, outFile, deleteInFileOnFinish);
    }

    public void saveAttributes(IFile inFile, IFile outFile, boolean deleteInFileOnFinish) throws IOException {
        throw new RuntimeException("not implemented yet");
        // super.saveAttributes(inFile, outFile, deleteInFileOnFinish);
    }

    /**
     * @deprecated use {@link #fixDateTakenIfNeccessary(IFile)} instead
     */
    @Deprecated
    @Override
    protected void fixDateTakenIfNeccessary(File inFile) {
        super.fixDateTakenIfNeccessary(inFile);
    }

    protected void fixDateTakenIfNeccessary(IFile inFile) {
        throw new RuntimeException("not implemented yet");
    }

    /**
     * @deprecated use {@link #setFilelastModified(IFile)} instead
     */
    @Deprecated
    @Override
    public void setFilelastModified(File file) {
        super.setFilelastModified(file);

    }

    public void setFilelastModified(IFile file) {
        throw new RuntimeException("not implemented yet");
    }

    //------------- File api to be overwritten for android specific DocumentFile implementation
    protected InputStream createInputStream(File exifFile) throws FileNotFoundException {
        return documentFileTranslator.openInputStream(exifFile);
    }

    protected OutputStream createOutputStream(File outFile) throws FileNotFoundException {
        return documentFileTranslator.createOutputStream(MIMNE, outFile);
    }

    protected boolean renameTo(File originalInFile, File renamedInFile) {
        return getDocumentFileOrDir(originalInFile, false).renameTo(renamedInFile.getName());
    }

    protected String getAbsolutePath(File inFile) {
        return inFile.getAbsolutePath();
    }

    protected boolean deleteFile(File file) {
        return getDocumentFileOrDir(file, false).delete();
    }

    // -----
    private DocumentFile getDocumentFileOrDir(File fileOrDir, boolean isDir) {
        return documentFileTranslator.getDocumentFileOrDir(fileOrDir, isDir);
    }
}

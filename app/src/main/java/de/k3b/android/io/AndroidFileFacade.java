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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.k3b.android.widget.FilePermissionActivity;
import de.k3b.io.Converter;
import de.k3b.io.FileFacade;
import de.k3b.io.FileUtils;
import de.k3b.io.IFile;

/**
 * de.k3b.android.io.AndroidFileFacade havs the same methods as java.io.File
 * but is implemented through Android specific {@link DocumentFile}
 */
public class AndroidFileFacade extends FileFacade {
    public static final String LOG_TAG = "k3b.AndFileFacade";

    private static DocumentFileTranslator documentFileTranslator = null;

    /**
     * androidFile=null && !androidFileMayExist means: DocumentFile does not exist (yet)
     */
    @Nullable
    private DocumentFile androidFile = null;

    /**
     * false means do not try to load androidFile again
     */
    private boolean androidFileMayExist = true;

    private static final Converter<File, IFile> androidFileFacadeImpl = new Converter<File, IFile>() {
        @Override
        public IFile convert(String dbgContext, File file) {
            final IFile result = new AndroidFileFacade(file);
            if (debugLogFacade) {
                Log.i(LOG_TAG, " convert => " + result);
            }
            return result;
        }
    };

    public static void initFactory(Context context) {
        setFileFacade(androidFileFacadeImpl);

        if (context instanceof FilePermissionActivity) {
            FilePermissionActivity activity = (FilePermissionActivity) context;
            documentFileTranslator = activity.getDocumentFileTranslator();
        }
    }

    private AndroidFileFacade(@Nullable DocumentFile parentFile, @NonNull File parentFile1) {
        super(parentFile1);
        androidFile = parentFile;
    }

    public AndroidFileFacade(@NonNull File file) {
        this(null, file);
    }

    public static DocumentFile getDocumentFileOrDirOrNull(@NonNull File file) {
        return documentFileTranslator.getDocumentFileOrDirOrNull(file, null);
    }

    @Override
    public boolean renameTo(@NonNull IFile newName) {
        if (exists() && !newName.exists()) {
            if (getParentFile().equals(newName.getParentFile())) {
                // same directory
                return renameTo(newName.getName());
            }

            if (copyImpl((AndroidFileFacade) newName, true)) {
                return true;
            }
        }
        Log.e(LOG_TAG, "renameTo " + this + " -> " + newName + " failed");
        return false;
    }

    private boolean copyImpl(@NonNull AndroidFileFacade targetFullPath, boolean deleteSourceWhenSuccess) {
        try {
            FileUtils.copy(openInputStream(), targetFullPath.openOutputStream());
        } catch (IOException ex) {
            Log.e(LOG_TAG, "copyImpl " + this + " -> " + targetFullPath + " failed", ex);
            return false;
        }
        if (deleteSourceWhenSuccess) {
            this.delete();
        }
        return true;
    }

    @Override
    public boolean renameTo(@NonNull String newName) {
        if (exists() && getAndroidFile().renameTo(newName)) {
            return true;
        }

        Log.e(LOG_TAG, "renameTo " + this + " -> " + newName + " failed");
        return false;
    }

    @Override
    public boolean delete() {
        return exists() && getAndroidFile().delete();
    }

    @Override
    public boolean exists() {
        DocumentFile androidFile = getAndroidFile();
        return (androidFile != null) && androidFile.exists();
    }

    @Override
    public IFile findExisting(String name) {
        final DocumentFile androidFile = getAndroidFile();
        if (androidFile != null) {
            DocumentFile doc = androidFile.findFile(name);
            if (doc != null) {
                return new AndroidFileFacade(doc, new File(getFile(), name));
            }
        }
        return null;
    }

    @Override
    public boolean canWrite() {
        final DocumentFile androidFile = getAndroidFile();
        return (androidFile != null) && androidFile.canWrite();
    }

    @Override
    public boolean canRead() {
        final DocumentFile androidFile = getAndroidFile();
        return (androidFile != null) && androidFile.canRead();
    }

    @Override
    public boolean isFile() {
        final DocumentFile androidFile = getAndroidFile();
        return (androidFile != null) && androidFile.isFile();
    }

    @Override
    public boolean isDirectory() {
        final DocumentFile androidFile = getAndroidFile();
        return (androidFile != null) && androidFile.isDirectory();
    }

    @Override
    public boolean isHidden() {
        final DocumentFile androidFile = getAndroidFile();
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
        final DocumentFile androidFile = getAndroidFile();
        if (androidFile != null) {
            return new AndroidFileFacade(androidFile.getParentFile(), getFile().getParentFile());
        } else {
            return super.getParentFile();
        }
    }

    @Override
    public String getName() {
        final DocumentFile androidFile = getAndroidFile();
        if (androidFile != null) {
            return androidFile.getName();
        }
        return super.getName();
    }

    @Override
    public long lastModified() {
        final DocumentFile androidFile = getAndroidFile();
        if (androidFile != null) {
            return androidFile.lastModified();
        }
        return 0;
    }

    @Override
    public boolean mkdirs() {
        this.androidFile = documentFileTranslator.getOrCreateDirectory(getFile());
        return null != this.androidFile;
    }

    @Override
    public IFile[] listFiles() {
        final DocumentFile androidFile = getAndroidFile();
        if (androidFile != null) {
            return get(androidFile.listFiles());
        }
        return new IFile[0];
    }

    @Override
    public long length() {
        final DocumentFile androidFile = getAndroidFile();
        if (androidFile != null) {
            return androidFile.length();
        }
        return 0;
    }

    @Override
    public boolean copy(@NonNull IFile targetFullPath, boolean deleteSourceWhenSuccess) throws IOException {
        return copyImpl((AndroidFileFacade) targetFullPath, deleteSourceWhenSuccess);
    }

    @Override
    public OutputStream openOutputStream() throws FileNotFoundException {
        DocumentFile androidFile = getAndroidFile();
        if (androidFile == null) {
            final DocumentFile documentFileParent = documentFileTranslator.getOrCreateDirectory(getFile().getParentFile());
            androidFile = this.androidFile = documentFileParent.createFile(null, getFile().getName());
            if (FileFacade.debugLogFacade) {
                Log.i(LOG_TAG, "created for openOutputStream " + this);
            }

        }
        return documentFileTranslator.createOutputStream(androidFile);
    }

    @Override
    public InputStream openInputStream() throws FileNotFoundException {
        return documentFileTranslator.openInputStream(getAndroidFile());
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

    /**
     * implements loads on demand.
     *
     * @return null means: DocumentFile does not exist (yet)
     */
    @Nullable
    private DocumentFile getAndroidFile() {
        if ((androidFile == null) && androidFileMayExist) {
            androidFile = getDocumentFileOrDirOrNull(getFile());
            androidFileMayExist = false;
        }
        return androidFile;
    }
}
/*
 * Copyright (c) 2020-2921 by k3b.
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
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.widget.FilePermissionActivity;
import de.k3b.io.Converter;
import de.k3b.io.FileUtils;
import de.k3b.io.filefacade.FileFacade;
import de.k3b.io.filefacade.IFile;

/**
 * de.k3b.android.io.AndroidFileFacade has the same methods as java.io.File
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
     * original android media content URI
     */
    private Uri readUri = null;

    @Override
    public String toString() {
        String result = super.toString();
        if (readUri != null) {
            result += "(" + readUri + ")";
        }
        return result;
    }

    /**
     * false means do not try to load androidFile again
     */
    private boolean androidFileMayExist = true;

    private static final Converter<File, IFile> androidFileFacadeImpl = new Converter<File, IFile>() {
        @Override
        public IFile convert(String dbgContext, File file) {
            final IFile result = new AndroidFileFacade(file);
            if (debugLogSAFFacade) {
                Log.i(LOG_TAG, dbgContext + " convert => " + result);
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

    @Override
    public void set(IFile src) {
        if (src != null) {
            if (src instanceof AndroidFileFacade) {
                AndroidFileFacade androidSrc = (AndroidFileFacade) src;
                this.androidFile = androidSrc.androidFile;
                this.androidFileMayExist = androidSrc.androidFileMayExist;
                this.readUri = androidSrc.readUri;
            }
        }
        super.set(src);
    }

    private DocumentFile getDocumentFileOrDirOrNull(String debugContext, @NonNull File file) {
        return documentFileTranslator.getDocumentFileOrDirOrNull(debugContext, file, null);
    }

    private boolean copyImpl(@NonNull AndroidFileFacade targetFullPath, boolean deleteSourceWhenSuccess) {
        final String dbgContext = "AndroidFileFacade.copyImpl " + this + " -> " + targetFullPath;
        try {
            FileUtils.copy(openInputStream(), targetFullPath.openOutputStream(), dbgContext);
        } catch (IOException ex) {
            Log.e(LOG_TAG, dbgContext + " failed", ex);
            return false;
        }
        if (deleteSourceWhenSuccess) {
            this.delete();
        }
        return true;
    }

    @Override
    public boolean renameTo(@NonNull String newName) {
        if (exists() && getAndroidFile("renameTo", false).renameTo(newName)) {
            invalidateParentDirCache();
            return true;
        }

        Log.e(LOG_TAG, "renameTo " + this + " -> " + newName + " failed");
        return false;
    }

    @Override
    public boolean delete() {
        boolean result = exists() && getAndroidFile("delete", false).delete();

        if (result) {
            invalidateParentDirCache();
            // File (and reference to it) does not exist any more
            androidFileMayExist = false;
            androidFile = null;
        }
        return result;
    }

    @Override
    public boolean exists() {
        DocumentFile androidFile = getAndroidFile("exists", false);
        return (androidFile != null) && androidFile.exists();
    }

    @Override
    public boolean canWrite() {
        final DocumentFile androidFile = getAndroidFile("canWrite", false);
        return (androidFile != null) && androidFile.canWrite();
    }

    @Override
    public boolean canRead() {
        final DocumentFile androidFile = getAndroidFile("canRead", false);
        return (androidFile != null) && androidFile.canRead();
    }

    @Override
    public boolean isFile() {
        final DocumentFile androidFile = getAndroidFile("isFile", false);
        return (androidFile != null) && androidFile.isFile();
    }

    @Override
    public boolean isDirectory() {
        final DocumentFile androidFile = getAndroidFile("isDirectory", false);
        return (androidFile != null) && androidFile.isDirectory();
    }

    @Override
    public boolean isHidden() {
        final DocumentFile androidFile = getAndroidFile("isHidden", false);
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
        final DocumentFile androidFile = getAndroidFile("getParentFile", false);
        if (androidFile != null) {
            return new AndroidFileFacade(androidFile.getParentFile(), getFile().getParentFile());
        } else {
            return super.getParentFile();
        }
    }

    @Override
    public String getAsUriString() {
        if (readUri != null) return readUri.toString();

        final DocumentFile androidFile = getAndroidFile("getAsUriString", false);
        final Uri uri = (androidFile != null) ? androidFile.getUri() : null;
        if (uri != null) {
            return uri.toString();
        }
        return null;
    }

    @Override
    public void setReadUri(String readUri) {
        this.readUri = (readUri != null) ? Uri.parse(readUri) : null;
    }

    @Override
    public String getName() {
        final DocumentFile androidFile = getAndroidFile("getName", false);
        if (androidFile != null) {
            return androidFile.getName();
        }
        return super.getName();
    }

    @Override
    public long lastModified() {
        final DocumentFile androidFile = getAndroidFile("lastModified", false);
        if (androidFile != null) {
            return androidFile.lastModified();
        }
        return 0;
    }

    @Override
    public boolean mkdirs() {
        this.androidFile = documentFileTranslator.getOrCreateDirectory("mkdirs", getFile());
        invalidateParentDirCache();
        return null != this.androidFile;
    }

    @Override
    public IFile[] listFiles() {
        String debugContext = "listFiles";
        enableCache(debugContext, true);
        final DocumentFile androidFile = getAndroidFile(debugContext, false);
        if (androidFile != null) {
            return get(androidFile.listFiles());
        }
        return new IFile[0];
    }

    private void enableCache(String dbgContext, boolean enable) {
        if (enable != Global.android_DocumentFile_find_cache) {
            if (FileFacade.debugLogSAFFacade || DocumentFileTranslator.debugLogSAFCache) {
                Log.i(FileFacade.LOG_TAG, this.getClass().getSimpleName()
                        + " " + dbgContext + ": enableCache(" + enable + ")");
            }

            Global.android_DocumentFile_find_cache = enable;

            if (enable) {
                invalidateParentDirCache();
            }
        }
    }

    public IFile[] listDirs() {
        String dbgContext = "listDirs";
        enableCache(dbgContext, true);
        List<IFile> found = new ArrayList<>();
        final DocumentFile androidFile = getAndroidFile(dbgContext, false);
        if (androidFile != null) {
            final File parent = getFile();
            for (DocumentFile file : androidFile.listFiles()) {
                if (file != null &&
                        (file.isDirectory() || accept(file.getName().toLowerCase()))) {
                    found.add(new AndroidFileFacade(
                            file, new File(parent, file.getName())));
                }
            }
        }
        return found.toArray(new IFile[found.size()]);
    }

    @Override
    public long length() {
        final DocumentFile androidFile = getAndroidFile("length", false);
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
        String debugContext = "openOutputStream";
        DocumentFile androidFile = getAndroidFile(debugContext, false);
        String context = "openOutputStream overwrite existing ";
        if (androidFile == null) {
            final DocumentFile documentFileParent = documentFileTranslator.getOrCreateDirectory(debugContext, getFile().getParentFile());
            androidFile = this.androidFile = documentFileParent.createFile(null, getFile().getName());
            context = "openOutputStream create new ";

        }
        if (FileFacade.debugLogSAFFacade) {
            Log.i(LOG_TAG, context + this);
        }
        OutputStream result = documentFileTranslator.createOutputStream(androidFile);
        enableCache(debugContext, false);
        return result;
    }

    @Override
    public InputStream openInputStream() throws FileNotFoundException {
        String debugContext = "openInputStream ";
        if ((readUri != null)) {
            if (debugLogSAFFacade) {
                Log.i(LOG_TAG, debugContext + this + " for uri " + readUri);
            }
            return documentFileTranslator.openInputStream(readUri);
        }
        final DocumentFile androidFile = getAndroidFile(debugContext, true);
        final InputStream resultInputStream = documentFileTranslator.openInputStream(androidFile);
        if (resultInputStream == null) {
            final String msg = debugContext + this + " for uri "
                    + ((androidFile != null) ? androidFile.getUri() : "null") + " returns null";
            Log.w(LOG_TAG, msg);
            getAndroidFile(debugContext, true); // allow debugger to step in
            throw new FileNotFoundException(msg);
        } else if (debugLogSAFFacade) {
            Log.i(LOG_TAG, debugContext + this + " for uri " + androidFile.getUri());
        }
        return resultInputStream;
    }

    //------- file cache support for android
    @Override
    public IFile invalidateParentDirCache() {
        if (documentFileTranslator != null) {
            documentFileTranslator.documentFileCache.invalidateParentDirCache();
        }
        return this;
    }


    private IFile[] get(DocumentFile[] docs) {
        AndroidFileFacade[] f = new AndroidFileFacade[docs.length];
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
    private DocumentFile getAndroidFile(String debugContext, boolean forOpenInputStream) {
        if ((androidFile == null) && (forOpenInputStream || androidFileMayExist)) {
            androidFile = getDocumentFileOrDirOrNull(debugContext, getFile());
            androidFileMayExist = false;
        }
        return androidFile;
    }
}

/*
 * Copyright 2018 The Android Open Source Project (Licensed under the Apache License, Version 2.0)
 * Copyright 2021 by k3b under (Licensed under the GPL v3 (the "License"))
 */

package de.k3b.androidx.documentfile;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.File;

/**
 * This class contains changes made to TreeDocumentFileOriginal for supporting a cache.
 */
@RequiresApi(21)
class CachedTreeDocumentFile extends TreeDocumentFileOriginal {
    /**
     * every TreeDocumentFile knows the same cache instance inherited from parent
     */
    private DocumentFileCache cache;

    CachedTreeDocumentFile(@Nullable DocumentFileEx parent, @NonNull Context context, Uri uri) {
        super(parent, context, uri);
        if (parent instanceof CachedTreeDocumentFile) {
            cache = ((CachedTreeDocumentFile) parent).cache;
        }
    }

    /**
     * may create a pending TreeDocumentFile that does not exist until {@link #mkdirs()} was called.
     */
    public static TreeDocumentFile getOrCreate(
            @NonNull File file, @NonNull Context context, @NonNull DocumentFileCache cache) {
        TreeDocumentFile result = cache.findDirectory(file);
        File parentFile;
        if (result == null && (parentFile = file.getParentFile()) != null) {
            TreeDocumentFile parent = cache.findDirectory(parentFile);
            result = new TreeDocumentFile(parent, context, null);
            result.mFile = file;
            ((CachedTreeDocumentFile) result).cache = cache;
        }
        return result;
    }

    /**
     * Translates from dirFile to TreeDocumentFile
     */
    public TreeDocumentFile findDir(File dirFile) {
        return cache.findDirectory(dirFile);
    }

    /**
     * Creates the directory named by this abstract pathname, including any
     * necessary but nonexistent parent directories.  Note that if this
     * operation fails it may have succeeded in creating some of the necessary
     * parent directories.
     *
     * @return <code>true</code> if and only if the directory was created,
     * along with all necessary parent directories; <code>false</code>
     * otherwise
     */
    @Override
    public boolean mkdirs() {
        File file;
        if ((getUri() == null || !exists()) && (file = getFile()) != null) {
            TreeDocumentFile parentDocument = (TreeDocumentFile) getParentFile();
            File parentFile;
            if (parentDocument == null && (parentFile = file.getParentFile()) != null) {
                parentDocument = getOrCreate(parentFile, mContext, cache);
                if (parentDocument != null) parentDocument.mkdirs();
            }

            if (parentDocument != null) {
                this.mUri = parentDocument.createDirectoryUri(file.getName());
                return true;
            }
        }
        return false;
    }
}

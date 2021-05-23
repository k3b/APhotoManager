/*
 * Copyright 2018 The Android Open Source Project (Licensed under the Apache License, Version 2.0)
 * Copyright 2021 by k3b under (Licensed under the GPL v3 (the "License"))
 */

package de.k3b.androidx.documentfile;

import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * This class contains changes made to TreeDocumentFileOriginal
 */
@RequiresApi(21)
class TreeDocumentFile extends CachedTreeDocumentFile {
    TreeDocumentFile(@Nullable DocumentFileEx parent, Context context, Uri uri) {
        super(parent, context, uri);
    }

    public TreeDocumentFile findDirByName(String displayName) {
        String query = DocumentsContract.Document.COLUMN_DISPLAY_NAME + "= ? and " +
                DocumentsContract.Document.COLUMN_MIME_TYPE + "= ?";

        DocumentFileEx[] files = listFiles(query, new String[]{displayName, DocumentsContract.Document.MIME_TYPE_DIR});
        if (files == null || files.length == 0) return null;
        return (TreeDocumentFile) files[0];
    }
}

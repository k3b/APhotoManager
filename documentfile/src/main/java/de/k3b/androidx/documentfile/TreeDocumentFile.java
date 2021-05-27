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

import java.util.ArrayList;
import java.util.List;

import de.k3b.io.filefacade.DirectoryFilter;
import de.k3b.io.filefacade.IFile;

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

    @Override
    public IFile[] listIDirs() {
        StringBuilder query = new StringBuilder().append(DocumentsContract.Document.COLUMN_MIME_TYPE).append("= ?");
        List<String> params = new ArrayList<>();
        params.add(DocumentsContract.Document.MIME_TYPE_DIR);
        List<String> allowedFileSuffixesLowercase = DirectoryFilter.getAllowedFileSuffixesLowercase();
        if (allowedFileSuffixesLowercase != null && allowedFileSuffixesLowercase.size() > 0) {
            query.append(" AND (");
            String delimiter = " ";
            for (String s : allowedFileSuffixesLowercase) {
                query.append(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                        .append(" like ?")
                        .append(delimiter);
                params.add("%" + s);
                delimiter = " OR ";
            }
            query.append(")");
        }
        return toIFiles(listFiles(query.toString(), params.toArray(new String[params.size()])));
    }

    private String name = null;

    @Nullable
    @Override
    public String getName() {
        if (name == null) {
            name = super.getName();
        }
        return name;
    }

}

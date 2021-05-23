/*
 * Copyright 2018 The Android Open Source Project (Licensed under the Apache License, Version 2.0)
 * Copyright 2021 by k3b under (Licensed under the GPL v3 (the "License"))
 */

package de.k3b.androidx.documentfile;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;

import de.k3b.io.filefacade.FileFacade;
import de.k3b.io.filefacade.IFile;

/**
 * This class contains changes made to SingleDocumentFileOriginal
 */
@RequiresApi(19)
class SingleDocumentFile extends SingleDocumentFileOriginal {
    public static final String LOG_TAG = TAG;

    public static boolean debugLogSAFFacade = false;

    /**
     * original android media content URI
     */
    private Uri readUri = null;

    SingleDocumentFile(@Nullable DocumentFileEx parent, Context context, Uri uri) {
        super(parent, context, uri);
    }

    @Override
    public void set(IFile src) {
        super.set(src);
        if (src instanceof SingleDocumentFile) {
            this.readUri = ((SingleDocumentFile) src).readUri;
        }
    }

    @Override
    public void setReadUri(String readUri) {
        this.readUri = (readUri != null) ? Uri.parse(readUri) : null;
    }

    @Override
    public String getAsUriString() {
        if (readUri != null) return readUri.toString();
        return super.getAsUriString();
    }

    @Override
    public InputStream openInputStream() throws FileNotFoundException {
        String debugContext = "openInputStream ";
        if ((readUri != null)) {
            if (debugLogSAFFacade) {
                Log.i(TAG, debugContext + this + " for uri " + readUri);
            }
            return mContext.getContentResolver().openInputStream(readUri);
        }
        return mContext.getContentResolver().openInputStream(getUri());
    }

    @Override
    public OutputStream openOutputStream() throws FileNotFoundException {
        String context = "openOutputStream overwrite existing ";
        if (FileFacade.debugLogSAFFacade) {
            Log.i(LOG_TAG, context + this);
        }
        OutputStream result = mContext.getContentResolver().openOutputStream(getUri());
        return result;
    }

    @Override
    public String toString() {
        return super.toString() + "{" +
                "readUri='" + readUri +
                "'}";
    }
}

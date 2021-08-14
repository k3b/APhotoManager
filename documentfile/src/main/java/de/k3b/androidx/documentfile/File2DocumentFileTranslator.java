/*
 * Copyright 2021 by k3b (Licensed under the GPL v3 (the "License"))
 */
package de.k3b.androidx.documentfile;

import static de.k3b.androidx.documentfile.SingleDocumentFile.LOG_TAG;
import static de.k3b.androidx.documentfile.SingleDocumentFile.debugLogSAFFacade;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.File;

import de.k3b.io.Converter;
import de.k3b.io.filefacade.FileFacade;
import de.k3b.io.filefacade.IFile;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class File2DocumentFileTranslator {
    private final DocumentFileCache roots;
    private final Converter<File, IFile> androidFileFacadeImpl = new Converter<File, IFile>() {
        @Override
        public IFile convert(String dbgContext, File file) {
            final IFile result = roots.find(file);
            if (debugLogSAFFacade) {
                Log.i(LOG_TAG, dbgContext + " convert => " + result);
            }
            return result;
        }
    };

    public File2DocumentFileTranslator(DocumentFileCache roots) {
        this.roots = roots;
        FileFacade.setFileFacade(androidFileFacadeImpl);
    }

    /**
     * Register an Android file system rootUri with a corresponding rootFile.
     */
    public @NonNull
    TreeDocumentFile register(@NonNull Context context, @NonNull Uri rootUri, @NonNull File rootFile) {
        return roots.register(context, rootUri, rootFile);
    }

}

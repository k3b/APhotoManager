/*
 * Copyright 2018 The Android Open Source Project (Licensed under the Apache License, Version 2.0)
 * Copyright 2021 by k3b under (Licensed under the GPL v3 (the "License"))
 */

package de.k3b.androidx.documentfile;

import androidx.annotation.Nullable;

import java.io.File;

/**
 * This class contains changes made to RawDocumentFileOriginal
 */
class RawDocumentFile extends RawDocumentFileOriginal {
    private File mFile;

    RawDocumentFile(@Nullable DocumentFileEx parent, File file) {
        super(parent, file);
    }
}

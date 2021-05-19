/*
 * Copyright 2018 The Android Open Source Project (Licensed under the Apache License, Version 2.0)
 * Copyright 2021 by k3b under (Licensed under the GPL v3 (the "License"))
 */

package de.k3b.androidx.Documentfile;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * This class contains changes made to SingleDocumentFileOriginal
 */
@RequiresApi(19)
class SingleDocumentFile extends SingleDocumentFileOriginal {
    SingleDocumentFile(@Nullable DocumentFileEx parent, Context context, Uri uri) {
        super(parent, context, uri);
    }
}

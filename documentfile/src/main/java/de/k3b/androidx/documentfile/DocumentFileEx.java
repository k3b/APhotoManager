package de.k3b.androidx.documentfile;

import androidx.annotation.Nullable;

public abstract class DocumentFileEx extends DocumentFileFacade {
    DocumentFileEx(@Nullable DocumentFileEx parent) {
        super(parent);
    }
}

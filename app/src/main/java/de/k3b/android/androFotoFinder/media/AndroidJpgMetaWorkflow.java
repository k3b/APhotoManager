/*
 * Copyright (c) 2017 by k3b.
 *
 * This file is part of AndroFotoFinder / #APhotoManager.
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

package de.k3b.android.androFotoFinder.media;

import android.content.Context;

import java.io.File;

import de.k3b.android.util.MediaScanner;
import de.k3b.media.IMetaApi;
import de.k3b.media.JpgMetaWorkflow;
import de.k3b.transactionlog.TransactionLoggerBase;

/**
 * Apply meta data changes to jpg and/or xmp file and log.
 *
 * Adds android specific "Update Media DB" implementation
 *
 * Created by k3b on 23.10.2017.
 */

public class AndroidJpgMetaWorkflow extends JpgMetaWorkflow {
    private final Context ctx;
    private final String dbgContext;

    public AndroidJpgMetaWorkflow(Context ctx, TransactionLoggerBase transactionLogger, String dbgContext) {
        super(transactionLogger);
        this.ctx = (ctx != null) ? ctx.getApplicationContext() : null;
        this.dbgContext = dbgContext;
    }

    /** todo overwrite in android class to implement update media db */
    protected long updateMediaDB(long id, IMetaApi newExif, String oldJpgAbsolutePath, File newJpgFile) {
        // let media scanner re-scan exif data
        Long affectedId = MediaScanner.getInstance(this.ctx).insertOrUpdateMediaDatabase(this.dbgContext, this.ctx, oldJpgAbsolutePath, newJpgFile, null);

        return  (affectedId != null) ? affectedId.longValue() : id;
    }

}


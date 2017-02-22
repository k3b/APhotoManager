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

package de.k3b.android.util;

import java.io.File;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

public class DatabaseContext extends ContextWrapper {
    private static final String DEBUG_CONTEXT = "DatabaseContext";

    public DatabaseContext(final Context base) {
        super(base);
    }

    @Override
    public File getDatabasePath(final String name) {
        final File sdcard = Environment.getExternalStorageDirectory();
        String dbfile = sdcard.getAbsolutePath() + File.separator + "databases"
                + File.separator + name;
        if (!dbfile.endsWith(".db")) {
            dbfile += ".db";
        }

        final File result = new File(dbfile);

        if (!result.getParentFile().exists()) {
            result.getParentFile().mkdirs();
        }

        if (Log.isLoggable(DatabaseContext.DEBUG_CONTEXT, Log.INFO)) {
            Log.i(DatabaseContext.DEBUG_CONTEXT, "getDatabasePath(" + name
                    + ") = " + result.getAbsolutePath());
        }

        return result;
    }

    /** this version is called for android devices < api-11 */
    @Override
    public SQLiteDatabase openOrCreateDatabase(final String name,
                                               final int mode, final SQLiteDatabase.CursorFactory factory) {
        final SQLiteDatabase result = SQLiteDatabase.openOrCreateDatabase(
                this.getDatabasePath(name), null);
        // SQLiteDatabase result = super.openOrCreateDatabase(name, mode,
        // factory);
        if (Log.isLoggable(DatabaseContext.DEBUG_CONTEXT, Log.INFO)) {
            Log.i(DatabaseContext.DEBUG_CONTEXT, "openOrCreateDatabase(" + name
                    + ",,) = " + result.getPath());
        }
        return result;
    }

    /** this version is called for android devices >= api-11 */
    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler errorHandler) {
        return openOrCreateDatabase(name,mode, factory);
    }
}

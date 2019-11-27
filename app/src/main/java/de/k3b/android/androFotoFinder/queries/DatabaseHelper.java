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

package de.k3b.android.androFotoFinder.queries;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import de.k3b.android.androFotoFinder.transactionlog.TransactionLogSql;
import de.k3b.android.util.DatabaseContext;

/**
 * Created by k3b on 22.02.2017.
 */

/**
 * Android specific Encapsulation of the Database create/open/close/upgrade
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION_1_TransactionLog = 1;
    public static final int DATABASE_VERSION_2_MEDIA_DB_COPY = 2;

    public static final int DATABASE_VERSION = DatabaseHelper.DATABASE_VERSION_2_MEDIA_DB_COPY;

    public DatabaseHelper(final Context context, final String databaseName) {
        super(context, databaseName, null, DatabaseHelper.DATABASE_VERSION);
    }

    public static SQLiteDatabase getWritableDatabase(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(new DatabaseContext(context), "APhotoManager");
        }
        return instance.getWritableDatabase();
    }

    /**
     * called if database doesn-t exist yet
     */
    @Override
    public void onCreate(final SQLiteDatabase db) {
        db.execSQL(TransactionLogSql.CREATE_TABLE);

        this.version2Upgrade_MediDbCopy(db);
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion,
                          final int newVersion) {
        Log.w(this.getClass().toString(), "Upgrading database from version "
                + oldVersion + " to " + newVersion + ". (Old data is kept.)");
        if (oldVersion < DatabaseHelper.DATABASE_VERSION_2_MEDIA_DB_COPY) {
            this.version2Upgrade_MediDbCopy(db);
        }
    }

    private static DatabaseHelper instance = null;

    private void version2Upgrade_MediDbCopy(final SQLiteDatabase db) {
        for (String sql : MediaImageDbReplacement.DDL) {
            db.execSQL(sql);
        }
    }
}

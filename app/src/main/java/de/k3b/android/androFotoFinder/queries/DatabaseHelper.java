/*
 * Copyright (c) 2017-2020 by k3b.
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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.transactionlog.TransactionLogSql;
import de.k3b.android.util.DatabaseContext;

import static de.k3b.android.androFotoFinder.queries.FotoSql.LOG_TAG;

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
    public static final String DATABASE_NAME = "APhotoManager";

    private static DatabaseHelper instance = null;
    private static DatabaseContext databaseContext = null;

    public DatabaseHelper(final Context context, final String databaseName) {
        super(context, databaseName, null, DatabaseHelper.DATABASE_VERSION);
    }

    public static SQLiteDatabase getWritableDatabase(Context context) {
        return getInstance(context).getWritableDatabase();
    }

    public static File getDatabasePath(Context context) {
        getInstance(context);
        return databaseContext.getDatabasePath(DATABASE_NAME);
    }

    private static DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            databaseContext = new DatabaseContext(context);
            instance = new DatabaseHelper(databaseContext, DATABASE_NAME);
        }
        return instance;
    }


    public static void version2Upgrade_ReCreateMediaDbTable(final SQLiteDatabase db) {
        execSql(db, "(Re)CreateMediaDbTable:", MediaDBRepository.Impl.DDL);
    }

    public static void createBackup(SQLiteDatabase db) {
        if (tableExists(db, MediaDBRepository.Impl.DATABASE_TABLE_NAME)) {
            // see https://www.techonthenet.com/sqlite/tables/create_table_as.php

            if (!tableExists(db, MediaDBRepository.Impl.DATABASE_TABLE_NAME_BACKUP)) {
                execSql(db, "create Backup:", MediaDBRepository.Impl.CREATE_BACKUP);
            } else {
                execSql(db, "update Backup:", MediaDBRepository.Impl.UPDATE_BACKUP);
            }
        }
    }

    public static void restoreFromBackup(SQLiteDatabase db) {
        if (tableExists(db, MediaDBRepository.Impl.DATABASE_TABLE_NAME_BACKUP)) {
            // see https://stackoverflow.com/questions/19270259/update-with-join-in-sqlite
            execSql(db, "restoreFromBackup:", MediaDBRepository.Impl.RESTORE_FROM_BACKUP);
        }
    }

    // from https://stackoverflow.com/questions/1601151/how-do-i-check-in-sqlite-whether-a-table-exists
    private static boolean tableExists(SQLiteDatabase db, String tableName) {
        if (tableName == null || db == null || !db.isOpen()) {
            return false;
        }
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = ? AND name = ?",
                new String[]{"table", tableName}
        );
        if (!cursor.moveToFirst()) {
            cursor.close();
            return false;
        }
        int count = cursor.getInt(0);
        cursor.close();
        return count > 0;
    }

    private static void execSql(SQLiteDatabase db, String dbgContext, String... ddlStatements) {

        for (String sql : ddlStatements) {
            if (Global.debugEnabledSql) {
                Log.i(LOG_TAG, "DatabaseHelper-" + dbgContext + sql);
            }
            db.execSQL(sql);
        }
    }

    /**
     * called if database doesn-t exist yet
     */
    @Override
    public void onCreate(final SQLiteDatabase db) {
        execSql(db, "First Create DB: ", TransactionLogSql.CREATE_TABLE);

        version2Upgrade_ReCreateMediaDbTable(db);
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion,
                          final int newVersion) {
        Log.w(this.getClass().toString(), "Upgrading database from version "
                + oldVersion + " to " + newVersion + ". (Old data is kept.)");
        if (oldVersion < DatabaseHelper.DATABASE_VERSION_2_MEDIA_DB_COPY) {
            version2Upgrade_ReCreateMediaDbTable(db);
        }
    }
}

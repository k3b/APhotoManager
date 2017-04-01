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

package de.k3b.android.androFotoFinder.transactionlog;

import android.content.ContentValues;

import de.k3b.transactionlog.MediaTransactionLogEntryType;

/**
 * Created by k3b on 22.02.2017.
 */

public class TransactionLogSql {
    public   static final String TABLE = "TransactionLog";
    private  static final String COL_PK = "_id";
    private  static final String COL_mediaID = "mediaID";
    private  static final String COL_modificationdate = "modificationDate";
    private  static final String COL_fullPath = "fullPath";
    private  static final String COL_command = "command";
    private  static final String COL_commandData = "commandData";

    public static final String CREATE_TABLE = "CREATE TABLE " + TABLE
            + "(" + COL_PK + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_mediaID + " INTEGER, "// " INTEGER REFERENCES " + TimeSliceCategorySql.TABLE + "(_id), "
            + COL_modificationdate + " DATE,"
            + COL_fullPath + " TEXT,"
            + COL_command + " TEXT,"
            + COL_commandData + " TEXT"
            + ")";

    public static ContentValues set(ContentValues valuesOrNull, long currentMediaID, String fileFullPath,
                           long modificationDate,
                           MediaTransactionLogEntryType mediaTransactionLogEntryType,
                           String commandData) {
        ContentValues values = valuesOrNull;
        if (values == null) {
            values = new ContentValues();
        } else {
            values.clear();
        }
        values.put(COL_mediaID,currentMediaID);
        values.put(COL_fullPath,fileFullPath);
        values.put(COL_modificationdate,modificationDate);
        values.put(COL_command,mediaTransactionLogEntryType.getId());
        values.put(COL_commandData, commandData);
        return values;
    }
}

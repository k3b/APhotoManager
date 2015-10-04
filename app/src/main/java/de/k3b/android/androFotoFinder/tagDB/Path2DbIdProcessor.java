/*
 * Copyright (c) 2015 by k3b.
 *
 * This file is part of AndroFotoFinder.
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

package de.k3b.android.androFotoFinder.tagDB;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import java.util.Iterator;
import java.util.NoSuchElementException;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.database.QueryParameter;

/**
 * This Processor translates Path to id from photo-media db.
 *
 * Created by k3b on 21.09.2015.
 */
abstract class Path2DbIdProcessor extends Path2IdProcessor {
    /** process all items in csvSorted by getting the id that corresponds to path  */
    protected void process(final Context context, Iterator<IPathID> csvSorted) {
        QueryParameter query = FotoSql.queryDetail;
        Cursor c = null;
        try {
            c = FotoSql.createCursorForQuery(context, query);
            process(csvSorted, new DbItemIterator(c));
        } catch (Exception ex) {
            Log.e(Global.LOG_CONTEXT, "FotoSql.execGetGeoRectangle(): error executing " + query, ex);
        } finally {
            if (c != null) c.close();
        }
    }

    class DbItem implements IPathID {
        private final Cursor mCursor;
        private final int mPathIndex;
        private final int mIdIndex;

        DbItem(Cursor cursor) {
            mCursor = cursor;
            mPathIndex = cursor.getColumnIndex(FotoSql.SQL_COL_PATH);
            mIdIndex = cursor.getColumnIndex(FotoSql.SQL_COL_PK);
        }

        @Override
        public String getPath() {
            return mCursor.getString(mPathIndex);
        }

        @Override
        public Integer getID() {
            return mCursor.getInt(mIdIndex);
        }
    }

    class DbItemIterator implements Iterator<IPathID> {
        private final Cursor mCursor;
        private int mPos = -1;
        private final int mLast;
        private final DbItem mItem;

        DbItemIterator(Cursor cursor) {
            mCursor = cursor;
            mLast = cursor.getCount() - 1;
            mItem = new DbItem(cursor);
        }

        @Override
        public boolean hasNext() {
            return mPos <= mLast;
        }

        @Override
        public IPathID next() {
            mPos++;
            if (!mCursor.moveToNext()) throw new NoSuchElementException();
            return mItem;
        }

        @Override
        public void remove() {
        }
    }
}

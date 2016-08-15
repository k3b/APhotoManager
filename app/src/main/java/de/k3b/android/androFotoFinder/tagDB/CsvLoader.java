/*
 * Copyright (c) 2015-2016 by k3b.
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
import android.support.annotation.Nullable;

import java.io.Reader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import de.k3b.csv2db.csv.CsvReader;

/**
 * Loads csv content into tags-db
 *
 * Created by k3b on 21.09.2015.
 */
public class CsvLoader extends Path2DbIdProcessor {
    public void load(final Context context, Reader reader) {
        CsvItemIterator csvReader = new CsvItemIterator(reader);
        this.process(context, csvReader);
    }

    @Override
    protected void process(IPathID _csv, Integer id) {
        CsvItem csv = (CsvItem) _csv;

        if (id == null) {
            // !!! TODO not found
        } else {
            // !!! TODO
        }
    }

    protected class CsvItem implements IPathID {
        private final int mPathIndex;
        private String[] mCurrentLineFields = null;

        CsvItem(List<String> header) {
            mPathIndex = header.indexOf("SourceFile");
        }

        @Override
        public String getPath() {
            return getString(mPathIndex);
        }

        @Nullable
        protected String getString(int columnNumber) {
            if ((columnNumber < 0) || (mCurrentLineFields == null) || (columnNumber >= mCurrentLineFields.length)) {
                return null;
            }
            return mCurrentLineFields[columnNumber];
        }

        @Override
        public Integer getID() {
            return null;
        }

        public void setData(String[] line) {
            mCurrentLineFields = line;
        }
    }

    protected class CsvItemIterator implements Iterator<IPathID> {
        private final CsvItem mItem;
        private final CsvReader mCsvReader;
        private boolean isEOF = false;

        CsvItemIterator(Reader reader) {
            mCsvReader = new CsvReader(reader);
            List<String> header = Arrays.asList(mCsvReader.readLine());
            mItem = new CsvItem(header);
        }

        @Override
        public boolean hasNext() {
            return ((mCsvReader != null) && (!isEOF));
        }

        @Override
        public IPathID next() {
            String[] line = mCsvReader.readLine();
            mItem.setData(line);

            if (line == null) {
                isEOF = true;
                return null;
            }
            return mItem;
        }

        @Override
        public void remove() {
            /* not used */
        }
    }

}

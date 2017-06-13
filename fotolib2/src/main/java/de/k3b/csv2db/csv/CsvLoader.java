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

package de.k3b.csv2db.csv;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Loads csv content into tags-db
 *
 * Created by k3b on 21.09.2015.
 */
abstract public class CsvLoader<T extends CsvItem> {
    private boolean mNotCanceled = true;
    public void load(Reader reader, T item) {
        CsvItemIterator<T> iter = new CsvItemIterator<T>(reader, item);
        while (iter.hasNext()) {
            onNextItem(iter.next(), iter.getLineNumner(), iter.getRecordNumber());
        }
    }

    abstract protected void onNextItem(T next, int lineNumber, int recordNumber);

    public void cancel() {
        mNotCanceled = false;
    }

    protected class CsvItemIterator<T extends CsvItem> implements Iterator<T>, Closeable {
        private final T mItem;
        private final CsvReader mCsvReader;
        private boolean isEOF = false;

        CsvItemIterator(Reader reader, T item) {
            mCsvReader = new CsvReader(reader);
            List<String> header = Arrays.asList(readLine());
            mItem = item;
            mItem.setHeader(header);
            mItem.setFieldDelimiter("" + mCsvReader.getFieldDelimiter());
        }

        public void close() throws IOException {
            if (mCsvReader != null) mCsvReader.close();
        }

        private String[] readLine() {
            return mCsvReader.readLine();
        }

        @Override
        public boolean hasNext() {
            return ((mNotCanceled) && (mCsvReader != null) && (!isEOF));
        }

        @Override
        public T next() {
            do {
                String[] line = readLine();
                mItem.setData(line);

                if (line == null) {
                    isEOF = true;
                    return null;
                }
            } while (mItem.isEmpty());
            return mItem;
        }

        @Override
        public void remove() {
            /* not used */
        }

        public int getLineNumner() {
            return (mCsvReader == null) ? -1 : mCsvReader.getLineNumner();
        }

        public int getRecordNumber() {
            return (mCsvReader == null) ? -1 : mCsvReader.getRecordNumber();
        }

    }

}

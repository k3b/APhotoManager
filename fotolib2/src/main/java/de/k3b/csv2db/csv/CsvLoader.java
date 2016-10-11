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

import java.io.Reader;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Loads csv content into tags-db
 *
 * Created by k3b on 21.09.2015.
 */
abstract public class CsvLoader<T extends CsvLoader.CsvItem> {
    public void load(Reader reader, T item) {
        CsvItemIterator<T> iter = new CsvItemIterator<T>(reader, item);
        while (iter.hasNext()) {
            onNextItem(iter.next());
        }
    }

    abstract protected void onNextItem(T next);

    abstract public static class CsvItem {
        private String[] mCurrentLineFields = null;

        protected String getString(int columnNumber) {
            if ((columnNumber < 0) || (mCurrentLineFields == null) || (columnNumber >= mCurrentLineFields.length)) {
                return null;
            }
            return mCurrentLineFields[columnNumber];
        }

        protected Double getDouble(int columnNumber) {
            String stringValue = getString(columnNumber);
            if ((stringValue != null) && (stringValue.length()> 0)) {
                try {
                    return Double.valueOf(stringValue);
                } catch (NumberFormatException ex) {

                }
            }
            return null;
        }

        protected Integer getInteger(int columnNumber) {
            String stringValue = getString(columnNumber);
            if ((stringValue != null) && (stringValue.length()> 0)) {
                try {
                    return Integer.valueOf(stringValue);
                } catch (NumberFormatException ex) {

                }
            }
            return null;
        }

        protected Date getDate(int columnNumber) {
            Integer stringValue = getInteger(columnNumber);
            if ((stringValue != null) && (stringValue.intValue() != 0)) {
                try {
                    Long.valueOf(stringValue);
                } catch (NumberFormatException ex) {

                }
            }
            return null;
        }

        public void setData(String[] line) {
            mCurrentLineFields = line;
        }
    }

    protected class CsvItemIterator<T extends CsvItem> implements Iterator<T> {
        private final T mItem;
        private final CsvReader mCsvReader;
        private boolean isEOF = false;

        CsvItemIterator(Reader reader, T item) {
            mCsvReader = new CsvReader(reader);
            List<String> header = Arrays.asList(mCsvReader.readLine());
            mItem = item;
        }

        @Override
        public boolean hasNext() {
            return ((mCsvReader != null) && (!isEOF));
        }

        @Override
        public T next() {
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

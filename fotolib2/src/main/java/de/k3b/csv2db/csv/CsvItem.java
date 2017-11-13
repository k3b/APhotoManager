/*
 * Copyright (c) 2015-2017 by k3b.
 *
 * This file is part of AndroFotoFinder / #APhotoManager
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

import java.util.Date;
import java.util.List;

import de.k3b.io.DateUtil;

/**
 * FieldVales from a/for a csv line (csv= comma seperated values)
 * Created by k3b on 17.10.2016.
 */
abstract public class CsvItem {
    public static final char DEFAULT_CHAR_LINE_DELIMITER = '\n';
    public static final String DEFAULT_CSV_FIELD_DELIMITER = ",";
    public static final char CHAR_FIELD_SURROUNDER = '\"';

    /** values of the current csv file line: header.get(i)=mCurrentLineFieldValues[i]  */
    private String[] mCurrentLineFieldValues = null;

    /** header.get(i)=mCurrentLineFieldValues[i] */
    protected List<String> header;

    private String mFieldDelimiter = DEFAULT_CSV_FIELD_DELIMITER;
    private String mCsvSpecialChars = null;

    public void setHeader(List<String> header) {
        this.header = header;
    }

    public void clear() {
        if (mCurrentLineFieldValues != null) {
            for (int i = 0; i < mCurrentLineFieldValues.length; i++) {
                mCurrentLineFieldValues[i] = null;
            }
        }
    }

    public boolean isEmpty() {
        return -1 == getLastNonEmptyIndex();
    }

    protected String getString(String debugContext, int columnNumber) {
        if (isInvalidIndex(columnNumber)) {
            return null;
        }
        return mCurrentLineFieldValues[columnNumber];
    }

    protected void setString(Object value, int columnNumber) {
        if (!isInvalidIndex(columnNumber)) {
            mCurrentLineFieldValues[columnNumber] = (value != null) ? value.toString() : null;
        }
    }

    private boolean isInvalidIndex(int columnNumber) {
        return (columnNumber < 0) || (mCurrentLineFieldValues == null) || (columnNumber >= mCurrentLineFieldValues.length);
    }

    protected Integer getInteger(String debugContext, int columnNumber) {
        String stringValue = getString(debugContext, columnNumber);
        if ((stringValue != null) && (stringValue.length() > 0)) {
            try {
                return Integer.valueOf(stringValue);
            } catch (NumberFormatException ex) {

            }
        }
        return null;
    }

    // last wins
    protected void setDate(Date value, int... columnNumbers) {
        for (int columnNumber : columnNumbers) {
            setString(DateUtil.toIsoDateTimeString(value), columnNumber);
        }
    }

    // first wins
    protected Date getDate(String debugContext, int... columnNumbers) {
        Date result = null;

        for (int columnNumber : columnNumbers) {
            if (columnNumber >= 0) {
                // if date as string
                String stringValue = getString(null, columnNumber);
                result = DateUtil.parseIsoDate(stringValue);

                if (result == null) {
                    // if date as integer
                    Integer intValue = getInteger(null, columnNumber);
                    if ((intValue != null) && (intValue.intValue() != 0)) {
                        try {
                            result = new Date(Long.valueOf(intValue));
                        } catch (NumberFormatException ex) {

                        }
                    }
                }
                if (result != null) return result;
            }
        }
        return result;
    }

    public void setData(String[] line) {
        mCurrentLineFieldValues = line;
    }

    @Override
    public String toString() {
        int last = getLastNonEmptyIndex();
        if (last < 0) return null;
        StringBuilder result = new StringBuilder();
        result.append(quouteIfNecessary(mCurrentLineFieldValues[0]));
        for (int i = 1; i <= last; i++) {
            result
                    .append(getFieldDelimiter())
                    .append(quouteIfNecessary(mCurrentLineFieldValues[i]));
        }
        return result.toString();
    }

    /** places quote around fieldValue if necessary */
    protected String quouteIfNecessary(String fieldValue) {
        if (fieldValue == null) return "";
        if (mustQuote(fieldValue)) {
            return CHAR_FIELD_SURROUNDER + fieldValue.replace(""+CHAR_FIELD_SURROUNDER, "'") + CHAR_FIELD_SURROUNDER;
        }
        return fieldValue;
    }

    /** return true if contentmust be quoted because it contains problematic chars */
    protected boolean mustQuote(String fieldValue) {
        if (fieldValue == null) return false;
        for (char forbidden : mCsvSpecialChars.toCharArray()) {
            if (fieldValue.indexOf(forbidden) >= 0) return true;
        }
        return false;
    }

    protected int getLastNonEmptyIndex() {
        if (mCurrentLineFieldValues != null) {
            for (int i = this.mCurrentLineFieldValues.length - 1; i >= 0; i--) {
                if (this.mCurrentLineFieldValues[i] != null) return i;
            }
        }
        return -1;
    }

    /** will become one of {@link CsvReader#POSSIBLE_DELIMITER_CHARS} */
    public String getFieldDelimiter() {
        return mFieldDelimiter;
    }

    public void setFieldDelimiter(String fieldDelimiter) {
        this.mFieldDelimiter = fieldDelimiter;
        this.mCsvSpecialChars = fieldDelimiter + "\r" + DEFAULT_CHAR_LINE_DELIMITER + CHAR_FIELD_SURROUNDER;
    }
}

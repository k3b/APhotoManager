package de.k3b.csv2db.csv;

import java.util.Date;
import java.util.List;

import de.k3b.io.DateUtil;

/**
 * Created by EVE on 17.10.2016.
 */
abstract public class CsvItem {
    public static final char DEFAULT_CHAR_LINE_DELIMITER = '\n';
    public static final String DEFAULT_CSV_FIELD_DELIMITER = ";";
    private String[] mCurrentLineFields = null;
    protected List<String> header;

    private String mFieldDelimiter = DEFAULT_CSV_FIELD_DELIMITER;

    public void setHeader(List<String> header) {
        this.header = header;
    }

    public void clear() {
        if (mCurrentLineFields != null) {
            for (int i = 0; i < mCurrentLineFields.length; i++) {
                mCurrentLineFields[i] = null;
            }
        }
    }

    public boolean isEmpty() {
        return -1 == getLastNonEmptyIndex();
    }

    protected String getString(int columnNumber) {
        if (isInvalidIndex(columnNumber)) {
            return null;
        }
        return mCurrentLineFields[columnNumber];
    }

    protected void setString(int columnNumber, Object value) {
        if (!isInvalidIndex(columnNumber)) {
            mCurrentLineFields[columnNumber] = (value != null) ? value.toString() : null;
        }
    }

    private boolean isInvalidIndex(int columnNumber) {
        return (columnNumber < 0) || (mCurrentLineFields == null) || (columnNumber >= mCurrentLineFields.length);
    }

    protected Double getDouble(int columnNumber) {
        String stringValue = getString(columnNumber);
        if ((stringValue != null) && (stringValue.length() > 0)) {
            try {
                return Double.valueOf(stringValue);
            } catch (NumberFormatException ex) {

            }
        }
        return null;
    }

    protected Integer getInteger(int columnNumber) {
        String stringValue = getString(columnNumber);
        if ((stringValue != null) && (stringValue.length() > 0)) {
            try {
                return Integer.valueOf(stringValue);
            } catch (NumberFormatException ex) {

            }
        }
        return null;
    }

    protected void setDate(int columnNumber, Date value) {
        setString(columnNumber, DateUtil.toIsoDateString(value));
    }

    protected Date getDate(int columnNumber) {
        Date result = null;

        // if date as string
        String stringValue = getString(columnNumber);
        result = DateUtil.parseIsoDate(stringValue);

        if (result == null) {
            // if date as integer
            Integer intValue = getInteger(columnNumber);
            if ((intValue != null) && (intValue.intValue() != 0)) {
                try {
                    result = new Date(Long.valueOf(intValue));
                } catch (NumberFormatException ex) {

                }
            }
        }
        return result;
    }

    public void setData(String[] line) {
        mCurrentLineFields = line;
    }

    @Override
    public String toString() {
        int last = getLastNonEmptyIndex();
        if (last < 0) return null;
        StringBuilder result = new StringBuilder();
        result.append(escape(mCurrentLineFields[0]));
        for (int i = 1; i <= last; i++) {
            result
                    .append(getFieldDelimiter())
                    .append(escape(mCurrentLineFields[i]));
        }
        return result.toString();
    }

    private String escape(String fieldValue) {
        return fieldValue;
    }

    protected int getLastNonEmptyIndex() {
        if (mCurrentLineFields != null) {
            for (int i = this.mCurrentLineFields.length - 1; i >= 0; i--) {
                if (this.mCurrentLineFields[i] != null) return i;
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
    }
}

/*
 * Copyright (c) 2015-2017 by k3b.
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

package de.k3b.csv2db.csv;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.Vector;

/**
 * Raw processing of csv reader
 * That gets lines of csv-columns.
 * Features: Infer column-Delimiter
 * Handle multiline columns if surrounded with ""
 * @author EVE
 *
 */
public class CsvReader implements Closeable {
	/** the first occurence in the first line of one othe these chars will become the {@link #fieldDelimiter} */
	private static final String POSSIBLE_DELIMITER_CHARS = ",;\t";

	public static final char FIELDLEN_DELIMITER = ':';
	private static final char CHAR_IGNORE = '\r';

	private char fieldDelimiter = 0;

	private Reader reader;

	// csv file source line number for error messages. (lineNumber >  recordNumber) if there is a record with multiline data.
	private int lineNumber = 0;

	// csv recordnumber
	private int recordNumber = 0;

	public CsvReader(Reader reader) {
		this.reader = reader;
	}

	public void close() throws IOException {
		if (reader != null) reader.close();
		reader = null;
	}
	public String[] readLine() {
		Vector<String> result = new Vector<String>();
		StringBuffer content = new StringBuffer();

		// != 0: look for matching -"- to allow multiline fields
		int fieldSurrounder = 0;
		
		try {
			// this.reader = new Reader(getClass().getResourceAsStream("/data.csv"));
			int ch;
			while ((ch=this.reader.read()) != -1) // ,0,cbuf.length) > 0)
			{
				if (ch== CsvItem.DEFAULT_CHAR_LINE_DELIMITER) this.lineNumber++;
				
				if (fieldSurrounder == 0) {
					if ((fieldDelimiter == 0) && POSSIBLE_DELIMITER_CHARS.indexOf(ch) >= 0) {
						// fieldDelimiter unknown: infer
 						fieldDelimiter = (char) ch;
					}
					if (ch == fieldDelimiter) {
						result.addElement(getStringWithoutDelimiters(content));
						content.setLength(0);
					} else if (ch== CsvItem.DEFAULT_CHAR_LINE_DELIMITER) {
						result.addElement(getStringWithoutDelimiters(content));
						this.recordNumber++;
						return toStringArray(result);
					} else if (ch != CHAR_IGNORE){
						content.append((char) ch);
					}
					
					if (ch == CsvItem.CHAR_FIELD_SURROUNDER)
						fieldSurrounder = (char) ch; // start -"- area
				} else {
					// waiting for end--"-
					if (ch != CHAR_IGNORE){
						content.append((char) ch);
						if (ch == fieldSurrounder)
							fieldSurrounder = 0;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (content.length() > 0) {
			result.addElement(getStringWithoutDelimiters(content));			
		}
		
		if (result.isEmpty()) {
			return null;
		} else {
			this.recordNumber++;
			return toStringArray(result);    	
		}
	}

	private String[] toStringArray(Vector<String> result) {
		int len = result.size();
		if (len == 0) {
			return null;
		}
		
		String[] array = new String[len];
		result.copyInto(array);
		return array;
	}

	/**
	 * @param content that may contain starting and ending -"-
	 * @return string without starting and ending -"-
	 */
	static private String getStringWithoutDelimiters(StringBuffer content)
    {
		if (content.length() > 0)
		{
			if (content.charAt(0) == CsvItem.CHAR_FIELD_SURROUNDER)
				content.deleteCharAt(0);
			if (content.charAt(content.length() -1 ) == CsvItem.CHAR_FIELD_SURROUNDER)
				content.deleteCharAt(content.length() -1);
			if (content.length() > 0)
				return content.toString();
		} 
		return null;
    }

	public int getLineNumner() {
		return this.lineNumber;
	}

	public int getRecordNumber() {
		return this.recordNumber;
	}

	public char getFieldDelimiter() {
		return fieldDelimiter;
	}

	public void setFieldDelimiter(char fieldDelimiter) {
		this.fieldDelimiter = fieldDelimiter;
	}
}

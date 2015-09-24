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

package de.k3b.csv2db.csv;

import java.io.Reader;

import de.k3b.csv2db.csv.CsvReader;

public class TestUtil {
	public static Reader createReader(String csvSrc) {
		return new java.io.StringReader(csvSrc);
		// return new InputStreamReader(new ByteArrayInputStream(csvSrc.getBytes("UTF-8")));
	}

	public static CsvReader createParser(String csvSrc) {
		return new CsvReader(TestUtil.createReader(csvSrc));
	}
}

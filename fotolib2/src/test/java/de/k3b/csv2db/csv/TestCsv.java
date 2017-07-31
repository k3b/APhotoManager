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

import static org.junit.Assert.*;

import java.io.Reader;

import org.junit.Assert;
import org.junit.Test;

import de.k3b.TestUtil;

public class TestCsv {
	@Test
	public void emptyReaderShouldReturnNoData() throws Throwable {
		Reader inputStream = TestUtil.createReader("");
		CsvReader parser = new CsvReader(inputStream);
		
		String[] header = parser.readLine();
		assertNull(header);
	}

	@Test
	public void shouldReturn2TabColums() throws Throwable {
		Reader inputStream = TestUtil.createReader("a\tb");
		CsvReader parser = new CsvReader(inputStream);
		
		String[] header = parser.readLine();
		assertEquals(2, header.length);
	}

	@Test
	public void shouldReturn2SemicolonColums() throws Throwable {
		Reader inputStream = TestUtil.createReader("a;\"b;something\nmulti;line\"");
		CsvReader parser = new CsvReader(inputStream);
		
		String[] header = parser.readLine();
		assertEquals(2, header.length);
	}

	@Test
	public void shouldReturnNullOn2ndLine() throws Throwable {
		Reader inputStream = TestUtil.createReader("a;b");
		CsvReader parser = new CsvReader(inputStream);
		
		parser.readLine();
		String[] header = parser.readLine();
		assertNull(header);
	}
	
	@Test
	public void shouldNotReturnNullOn2ndLine() throws Throwable {
		Reader inputStream = TestUtil.createReader("a;b\nc");
		CsvReader parser = new CsvReader(inputStream);
		
		parser.readLine();
		String[] header = parser.readLine();
		assertNotNull(header);
	}

	@Test
	public void shouldCountLines() throws Throwable {
		Reader inputStream = TestUtil.createReader("a\n\"b;something\nmulti;line\"\n");
		CsvReader parser = new CsvReader(inputStream);
		
		while (null!=parser.readLine()) {};
		
		assertEquals(3, parser.getLineNumner());
	}

	@Test
	public void shouldCountRecordNumber() throws Throwable {
		Reader inputStream = TestUtil.createReader("a\n\"b;something\nmulti;line\"\n");
		CsvReader parser = new CsvReader(inputStream);
		
		while (null!=parser.readLine()) {};
		
		assertEquals(2, parser.getRecordNumber());
	}


	@Test
	public void shouldQuote() {
		CsvItem sut = new CsvItem() {
		};
		sut.setFieldDelimiter(";");
		Assert.assertEquals(true, sut.mustQuote(";"));
		Assert.assertEquals(true, sut.mustQuote("\""));
		Assert.assertEquals(true, sut.mustQuote("\n"));
		Assert.assertEquals(true, sut.mustQuote("\r"));
		Assert.assertEquals(false, sut.mustQuote("hello'world"));

		Assert.assertEquals("hello'world", sut.quouteIfNecessary("hello'world"));
		Assert.assertEquals("\"hello';\n\r\tworld\"", sut.quouteIfNecessary("hello\";\n" +
				"\r" +
				"\tworld"));


	}
}

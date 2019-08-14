/*
 * Copyright (c) 2015-2018 by k3b.
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
 
package de.k3b.io;

import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

import de.k3b.LibGlobal;

/**
 * Created by k3b on 08.06.2015.
 */
public class DirectoryFormatterTests {
    @Test
    public void shoudFormatLatLonPathNoRound() {
        String result = DirectoryFormatter.getLatLonPath(8.3459, 54.1239);
        Assert.assertEquals("/ 0,50/8,54/8.3,54.1/8.34,54.12/", result);
    }

    @Test
    public void shoudFormatLatLonPathZero() {
        String result = DirectoryFormatter.getLatLonPath(8.0, 54.0);
        Assert.assertEquals("/ 0,50/8,54/8.0,54.0/8.00,54.00/", result);
    }

    @Test
    public void shoudGetLastPath() {
        String result = DirectoryFormatter.getLastPath("/ 0,50/8,54/8.0,54.0/8.00,54.00/8.000,54.000/");
        Assert.assertEquals("8.000,54.000", result);
    }

    @Test
    public void shouldParseLatLonFromRange() {
        IGeoRectangle result = DirectoryFormatter.parseLatLon("1.2,2.3;3.4,4.5");
        Assert.assertEquals("1.2,2.3;3.4,4.5", result.toString());
    }

    @Test
    public void shouldParseLatLon10() {
        IGeoRectangle result = DirectoryFormatter.parseLatLon(" 130,12");
        Assert.assertEquals("130.0,12.0;140.0,22.0", result.toString());
    }

    @Test
    public void shouldParseLatLon1() {
        IGeoRectangle result = DirectoryFormatter.parseLatLon("130,12");
        Assert.assertEquals("130.0,12.0;131.0,13.0", result.toString());
    }

    @Test
    public void shouldParseLatLon0() {
        IGeoRectangle result = DirectoryFormatter.parseLatLon("130.0,12.0");
        Assert.assertEquals("130.0,12.0;130.1,12.1", result.toString());
    }

    @Test
    public void shouldParseLatLon00() {
        IGeoRectangle result = DirectoryFormatter.parseLatLon("130.00,12.00");
        Assert.assertEquals("130.0,12.0;130.01,12.01", result.toString());
    }

    // DirectoryFormatter.formatLatLon
    @Test
    public void shoudFormatLatLon0() {
        Assert.assertEquals("123.000000", DirectoryFormatter.formatLatLon(123));
        Assert.assertEquals("0", DirectoryFormatter.formatLatLon(0));
    }

    @Test
    public void shoudFormatDatePath() {
        int no = 1;
        String context = "#";
        assertEquals(context + no++, "/2010*/2017/05/03", true, "2017-05-03", "2017-05-03");
        assertEquals(context + no++, "/2010*/2017/05/03", true, "2017-05-03", "2017-05-04");
        assertEquals(context + no++, "/2010*/2017/12/31", true, "2017-12-31", "2018-01-01");

        assertEquals(context + no++, "/2017/05/03", false, "2017-05-03", "2017-05-04");

        assertEquals(context + no++, "/2010*/2017/05", true, "2017-05-01", "2017-06-01");
        assertEquals(context + no++, "/2010*/2017", true, "2017-01-01", "2018-01-01");
        assertEquals(context + no++, "/2010*", true, "2010-01-01", "2020-01-01");
    }

    private void assertEquals(String message, String expected, boolean withDecade, String from, String to) {
        Date dateFrom = DateUtil.parseIsoDate(from);
        Date dateTo = DateUtil.parseIsoDate(to);

        Assert.assertEquals(message, expected, DirectoryFormatter.formatDatePath(withDecade, dateFrom.getTime(), dateTo.getTime()));
    }

    @Test
    public void parseDatesPath() {
        boolean old = LibGlobal.datePickerUseDecade;
        LibGlobal.datePickerUseDecade = false;
        assertParseDatesPath("2001-01-16", "2001-01-17", "/2001/01/16");
        assertParseDatesPath("2001-01-01", "2001-02-01", "/2001/01/");
        assertParseDatesPath("2001-01-01", "2002-01-01", "/2001/");

        LibGlobal.datePickerUseDecade = true;
        assertParseDatesPath("2001-01-01", "2002-01-01", "/2000*/2001/");
        assertParseDatesPath("2000-01-01", "2010-01-01", "/2000*/");

        LibGlobal.datePickerUseDecade = old;
    }
    private void assertParseDatesPath(String expextedFrom, String expectedTo, String datePath) {
        Date fromResult = new Date();
        Date toResult = new Date();
        DirectoryFormatter.parseDatesPath(datePath, fromResult, toResult);
        String context = datePath + " => " + expextedFrom + " ... " + expectedTo;
        Assert.assertEquals(context, expextedFrom, DateUtil.toIsoDateString(fromResult));
        Assert.assertEquals(context, expectedTo, DateUtil.toIsoDateString(toResult));
    }


    @Test
    public void formatDatePath() {
        boolean old = LibGlobal.datePickerUseDecade;
        LibGlobal.datePickerUseDecade = false;
        assertFormatDatePath("/2001/01/16", "2001-01-16", "2001-01-17");
        assertFormatDatePath("/2001/01", "2001-01-01", "2001-02-01");
        assertFormatDatePath("/2001", "2001-01-01", "2002-01-01");

        LibGlobal.datePickerUseDecade = true;
        assertFormatDatePath("/2000*/2001", "2001-01-01", "2002-01-01");
        assertFormatDatePath("/2000*", "2000-01-01", "2010-01-01");

        LibGlobal.datePickerUseDecade = old;
    }

    private void assertFormatDatePath(String expectedDatePath, String from, String to) {
        Date fromDate = DateUtil.parseIsoDate(from);
        Date toDate = DateUtil.parseIsoDate(to);
        String result = DirectoryFormatter.formatDatePath(LibGlobal.datePickerUseDecade,
                fromDate.getTime(), toDate.getTime());
        String context = from + " ... " + to + " => " + expectedDatePath;
        Assert.assertEquals(context, expectedDatePath, result);
    }

    @Test
    public void formatLatLon() {
        Assert.assertEquals("0.123456", 0.123456, 0.123456, 1e-7);
        boolean old = LibGlobal.datePickerUseDecade;
        LibGlobal.datePickerUseDecade = false;
        assertFormatDatePath("/2001/01/16", "2001-01-16", "2001-01-17");
        assertFormatDatePath("/2001/01", "2001-01-01", "2001-02-01");
        assertFormatDatePath("/2001", "2001-01-01", "2002-01-01");

        LibGlobal.datePickerUseDecade = true;
        assertFormatDatePath("/2000*/2001", "2001-01-01", "2002-01-01");
        assertFormatDatePath("/2000*", "2000-01-01", "2010-01-01");

        LibGlobal.datePickerUseDecade = old;
    }



}

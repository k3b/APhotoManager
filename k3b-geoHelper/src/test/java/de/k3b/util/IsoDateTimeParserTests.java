/*
 * Copyright (c) 2015 by k3b.
 *
 * This file is part of LocationMapViewer.
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

package de.k3b.util;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by k3b on 12.02.2015.
 */
public class IsoDateTimeParserTests {
    // SSS is millisecs and Z is timezone relative to gmt.
    private static final SimpleDateFormat ISO_FULL = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private static Date EXPECTED_WITH_MILLISECS    = IsoDateTimeParser.toDate(2001,12,24,12,34,56,789, TimeZone.getTimeZone("GMT"));
    private static Date EXPECTED_WITHOUT_MILLISECS = IsoDateTimeParser.toDate(2001,12,24,12,34,56,0, TimeZone.getTimeZone("GMT"));

    @BeforeClass
    public static void setup() {
        ISO_FULL.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    // @Test // ignore lost millisecs
    public void shoudParseWithTimeZoneAndMillisecs() throws Exception {
        // 13 hours in timezone +01:00 is 12 hours in timezone +00:00
        assertEquals(EXPECTED_WITH_MILLISECS, "2001-12-24T13:34:56.789+01:00");
    }

    @Test
    public void shoudParseWithTimeZone() throws Exception {
        // 13 hours in timezone +01:00 is 12 hours in timezone +00:00
        assertEquals(EXPECTED_WITHOUT_MILLISECS, "2001-12-24T13:34:56+01:00");
    }

    @Test
    public void shoudParseZulo() throws Exception {
        assertEquals(EXPECTED_WITHOUT_MILLISECS, "2001-12-24T12:34:56Z");
        assertEquals(EXPECTED_WITHOUT_MILLISECS, "2001-12-24T12:34:56+0000");
        assertEquals(EXPECTED_WITHOUT_MILLISECS, "2001-12-24T12:34:56+0000Z");

        assertEquals(EXPECTED_WITHOUT_MILLISECS, "2001-12-24T12:34:56-00:00");
        // used by MyTracks for Android           2015-02-10T08:04:45.000Z
        assertEquals(EXPECTED_WITHOUT_MILLISECS, "2001-12-24T12:34:56.000Z");
        // assertEquals(EXPECTED_WITHOUT_MILLISECS, "2015-02-10T08:04:45.000Z");
    }

    @Test
    public void shoudParseZuloWithMillisecs() throws Exception {
        assertEquals(EXPECTED_WITHOUT_MILLISECS, "2001-12-24T12:34:56.789Z");
        assertEquals(EXPECTED_WITHOUT_MILLISECS, "2001-12-24T12:34:56.789-0000");
        assertEquals(EXPECTED_WITHOUT_MILLISECS, "2001-12-24T12:34:56.789+00:00");
    }

    static public void assertEquals(Date expected,
                                    String actualString) {
        Date actual = IsoDateTimeParser.parse(actualString);
        Assert.assertNotNull(actualString, actual);
        if (actual.getTime() != expected.getTime()) {
            long diff = (actual.getTime() - expected.getTime()) / 1000;
            Assert.assertEquals("dif in secs: " + diff, ISO_FULL.format(expected) + " ("+ expected.getTime() + ")", actualString + "("+ actual.getTime() + ")");
        }
    }
}

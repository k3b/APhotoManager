/*
 * Copyright (c) 2016 by k3b.
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

import de.k3b.geo.api.IGeoPointInfo;

/**
 * Created by k3b on 19.10.2016.
 */

public class GeoUtilTests {
    @Test
    public void shoudFormat() {
        assertFormat("50",50);
        assertFormat("-50.5",-50.5);
        assertFormat("-0.1",-0.1);
        assertFormat("0.1",0.1);
        assertFormat("0.01",0.01);
        assertFormat("0.001",0.001);
        assertFormat("0.0001",0.0001);
        assertFormat("0.00001",0.00001);
        assertFormat("0.000001",0.000001);
        assertFormat("0"       ,0.0000001);
        assertFormat("0"       ,0);
        assertFormat("-0.000001",-0.000001);
        assertFormat("50.12345W",-50.12345,1,"EW");
        assertFormat("50.12345E",50.12345,1,"EW");
        assertFormat("-50,30",-50.5,2,null);
        assertFormat("50,7,24.42W",-50.12345,3,"EW");
    }

    @Test
    public void shoudFormatXmp() {
        Assert.assertEquals("53,5.28N", GeoUtil.toXmpStringLatNorth(53.088));
        Assert.assertEquals("8,48.81E", GeoUtil.toXmpStringLonEast(8.8135));
    }

    @Test
    public void shoudFormatCsv() {
        Assert.assertEquals("53.088", GeoUtil.toCsvStringLatLon(53.088));
        Assert.assertEquals("8.8135", GeoUtil.toCsvStringLatLon(8.8135));
    }

    private void assertFormat(String expected, double actual) {
        assertFormat(expected, actual, 1, null);
    }

    private void assertFormat(String expected, double actual, int numberOfGrroups, String plusMinus) {
        String result = GeoUtil.toString(actual, numberOfGrroups, ",",plusMinus);
        Assert.assertEquals("" +actual,  expected, result);
    }

    @Test
    public void shoudParse() {
        assertParse(50.5, "+50.5","NS");
        assertParse(-50.5, "-50.5","NS");
        assertParse(50.5, "50, 30N","NS");
        assertParse(-50.5, "50, 30S","NS");
        assertParse(-50.5, "S 50'30''0.00","NS");
        assertParse(GeoUtil.NO_LAT_LON, "0","NS");
        assertParse(null, "","NS");
        assertParse(null, null,"NS");
    }

    private void assertParse(Double expected, String actual, String plusMinusns) {
        if (expected != null) {
            Assert.assertEquals(actual, expected, GeoUtil.parse(actual, plusMinusns), 0.00001);
        } else {
            Assert.assertEquals(actual, expected, GeoUtil.parse(actual, plusMinusns));
        }
    }

    @Test
    public void shoudFormatParse() {
        assertFormatParse(53.288760, 2);
        assertFormatParse(8.734160, 2);
        assertFormatParse(-123.456789, 3);
        assertFormatParse(-50.12345,3);
    }

    private void assertFormatParse(double expected,int numberOfGrroups) {
        String result = GeoUtil.toString(expected, numberOfGrroups, ",","EW");
        double dResult = GeoUtil.parse(result, "EW");

        Assert.assertEquals(result, expected, dResult, 0.0001);
    }
}

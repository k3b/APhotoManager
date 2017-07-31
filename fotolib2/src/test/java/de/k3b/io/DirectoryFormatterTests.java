/*
 * Copyright (c) 2015-2017 by k3b.
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
    public void shoudGetLatLonFromRange() {
        IGeoRectangle result = DirectoryFormatter.parseLatLon("1.2,2.3;3.4,4.5");
        Assert.assertEquals("1.2,2.3;3.4,4.5", result.toString());
    }

    @Test
    public void shoudGetLatLon10() {
        IGeoRectangle result = DirectoryFormatter.parseLatLon(" 130,12");
        Assert.assertEquals("130.0,12.0;140.0,22.0", result.toString());
    }

    @Test
    public void shoudGetLatLon1() {
        IGeoRectangle result = DirectoryFormatter.parseLatLon("130,12");
        Assert.assertEquals("130.0,12.0;131.0,13.0", result.toString());
    }

    @Test
    public void shoudGetLatLon0() {
        IGeoRectangle result = DirectoryFormatter.parseLatLon("130.0,12.0");
        Assert.assertEquals("130.0,12.0;130.1,12.1", result.toString());
    }

    @Test
    public void shoudGetLatLon00() {
        IGeoRectangle result = DirectoryFormatter.parseLatLon("130.00,12.00");
        Assert.assertEquals("130.0,12.0;130.01,12.01", result.toString());
    }

    // DirectoryFormatter.formatLatLon
    @Test
    public void shoudFormatLatLon0() {
        Assert.assertEquals("123.000000", DirectoryFormatter.formatLatLon(123));
        Assert.assertEquals("0", DirectoryFormatter.formatLatLon(0));
    }
}

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

package de.k3b.io;

import org.junit.Assert;
import org.junit.Test;

import java.sql.Date;

import static org.junit.Assert.*;

/**
 * Created by k3b on 01.09.2015.
 */
public class GalleryFilterParameterTest {
    @Test
    public void toStringFullTest() {
        GalleryFilterParameter sut = new GalleryFilterParameter();
        sut.setLatitude(1.23,3.45).setLogitude(2.34, 4.56);
        sut.setDateMin(Date.valueOf("2001-02-03").getTime()).setDateMax(Date.valueOf("2005-12-31").getTime());
        sut.setPath("/some/path/");

        assertEquals("1.23,2.34;3.45,4.56;2001-02-03,2005-12-31;/some/path/;", sut.toString());

    }

    @Test
    public void toStringEmptyTest() {
        GalleryFilterParameter sut = new GalleryFilterParameter();
        assertEquals(";;;;", sut.toString());
    }

    @Test
    public void shouldParseFull() {
        GalleryFilterParameter sutParsed = GalleryFilterParameter.parse("1.23,2.34;3.45,4.56;2001-02-03,2005-12-31;/some/path/;", new GalleryFilterParameter());
        GalleryFilterParameter sut = new GalleryFilterParameter().get(sutParsed);
        assertEquals("1.23,2.34;3.45,4.56;2001-02-03,2005-12-31;/some/path/;", sut.toString());
    }

    @Test
    public void shouldParseFullNoGeo() {
        GalleryFilterParameter sutParsed = GalleryFilterParameter.parse("n;;2001-02-03,2005-12-31;/some/path/;", new GalleryFilterParameter());
        GalleryFilterParameter sut = new GalleryFilterParameter().get(sutParsed);
        assertEquals("noGeoInfo;;2001-02-03,2005-12-31;/some/path/;", sut.toString());
    }
}
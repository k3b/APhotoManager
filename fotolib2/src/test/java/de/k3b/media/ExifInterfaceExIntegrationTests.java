/*
 * Copyright (c) 2017 by k3b.
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

package de.k3b.media;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import de.k3b.FotoLibGlobal;
import de.k3b.TestUtil;
import de.k3b.io.DateUtil;
import de.k3b.io.ListUtils;
import de.k3b.io.VISIBILITY;

/**
 * Created by k3b on 05.04.2017.
 */

public class ExifInterfaceExIntegrationTests {
    private static final Logger logger = LoggerFactory.getLogger(ExifInterfaceExIntegrationTests.class);

    private IMetaApi sut = null;

    @BeforeClass
    public static void initDirectories() {
        FotoLibGlobal.appName = "JUnit";
        FotoLibGlobal.appVersion = "ExifInterfaceExIntegrationTests";
    }

    @Before
    public void setup() throws IOException {
        // ExifInterfaceEx.DEBUG = true;
        sut = getMeta("test-WitExtraData.jpg");
    }

    @Test
    public void shouldDump() throws IOException
    {
        // System.out.printf(sut.toString());
        logger.info("shouldDump " + sut.toString());
    }

    @Test
    public void shouldGetDescription() throws IOException
    {
        Assert.assertEquals("ImageDescription", sut.getDescription());
    }

    @Test
    public void shouldGetTitle() throws IOException
    {
        Assert.assertEquals("XPTitle", sut.getTitle());
    }

    @Test
    public void shouldGetDateTimeTaken() throws IOException
    {
        Assert.assertEquals("1962-11-07T09:38:46", DateUtil.toIsoDateTimeString(sut.getDateTimeTaken()));
    }

    @Test
    public void shouldGetLatitude() throws IOException
    {
        Assert.assertEquals(27.8186, sut.getLatitude(), 0.01);
    }

    @Test
    public void shouldGetLongitude() throws IOException
    {
        Assert.assertEquals(-15.764, sut.getLongitude(), 0.01);
    }

    @Test
    public void shouldGetTags() throws IOException
    {
        Assert.assertEquals("Marker1, Marker2", ListUtils.toString(", ", sut.getTags()));
    }

    @Test
    public void shouldGetRating() throws IOException
    {
        Assert.assertEquals(3, sut.getRating().intValue());
    }

    @Test
    public void shouldModifyInMemory() throws IOException
    {
        MediaDTO expected = TestUtil.createTestMediaDTO(2);
        expected.setVisibility(VISIBILITY.PUBLIC);
        MediaUtil.copy(sut, expected, true, true);
        MediaDTO actual = new MediaDTO();
        MediaUtil.copy(actual, sut, true, true);
        actual.setPath(expected.path);
        Assert.assertEquals(expected.toString(), actual.toString());

        logger.info("shouldModifyInMemory " + sut.toString());
    }


    @Test
    public void shouldPreservePrivate() throws IOException
    {
        MediaDTO expected = TestUtil.createTestMediaDTO(2);
        expected.setVisibility(VISIBILITY.PRIVATE);
        MediaUtil.copy(sut, expected, true, true);
        MediaDTO actual = new MediaDTO();
        MediaUtil.copy(actual, sut, true, true);
        actual.setPath(expected.path);
        Assert.assertEquals(VISIBILITY.PRIVATE, actual.getVisibility());

        logger.info("shouldModifyInMemory " + sut.toString());
    }

    @Test
    public void shouldClearInMemory() throws IOException
    {
        MediaDTO expected = new MediaDTO();
        expected.setVisibility(VISIBILITY.PUBLIC);
        MediaUtil.copy(sut, expected, true, true);
        MediaDTO actual = new MediaDTO();
        MediaUtil.copy(actual, sut, true, true);
        actual.setPath(expected.path);
        Assert.assertEquals(expected.toString(), actual.toString());

        logger.info("shouldClearInMemory " + sut.toString());
    }


    public static IMetaApi getMeta(String fileName) throws IOException {
        InputStream inputStream = TestUtil.getResourceInputStream(fileName);
        IMetaApi result = new ExifInterfaceEx(fileName, inputStream, null, "JUnit");
        return result;
    }

}

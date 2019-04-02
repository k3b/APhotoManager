/*
 * Copyright (c) 2017-2019 by k3b.
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
import java.util.TimeZone;

import de.k3b.LibGlobal;
import de.k3b.TestUtil;
import de.k3b.io.DateUtil;
import de.k3b.io.ListUtils;

/**
 * Created by k3b on 25.04.2017.
 */

public class PhotoPropertiesXmpSegmentIntegrationTests {
    private static final Logger logger = LoggerFactory.getLogger(PhotoPropertiesXmpSegmentIntegrationTests.class);

    private IPhotoProperties sut = null;
    @BeforeClass
    public static void initDirectories() {
        LibGlobal.appName = "JUnit";
        LibGlobal.appVersion = "PhotoPropertiesXmpSegmentIntegrationTests";
    }

    @Before
    public void setup() throws IOException {
        // PhotoPropertiesXmpSegment.DEBUG = true;
        sut = getMeta(TestUtil.TEST_FILE_XMP_WITH_EXIF);
        TimeZone.setDefault(DateUtil.UTC);
    }

    @Test
    public void shouldDump() {
        // System.out.printf(sut.toString());
        logger.info("shouldDump " + sut.toString());
    }

    @Test
    public void shouldGetDescription() {
        Assert.assertEquals("XPSubject", sut.getDescription());
    }

    @Test
    public void shouldGetTitle() {
        Assert.assertEquals("Headline", sut.getTitle());
    }

    @Test
    public void shouldGetDateTimeTaken() {
        Assert.assertEquals("1962-11-07T09:38:46", DateUtil.toIsoDateTimeString(sut.getDateTimeTaken()));
    }

    @Test
    public void shouldGetLatitude() {
        Assert.assertEquals(27.8186, sut.getLatitude(), 0.01);
    }

    @Test
    public void shouldGetLongitude() {
        Assert.assertEquals(-15.764, sut.getLongitude(), 0.01);
    }

    @Test
    public void shouldGetTags() {
        Assert.assertEquals("Marker1, Marker2", ListUtils.toString(", ", sut.getTags()));
    }

    @Test
    public void shouldGetRating() {
        Assert.assertEquals(3, sut.getRating().intValue());
    }

    @Test
    public void shouldModifyInMemory() {
        PhotoPropertiesDTO expected = TestUtil.createTestMediaDTO(2);
        PhotoPropertiesUtil.copy(sut, expected, true, true);
        PhotoPropertiesDTO actual = new PhotoPropertiesDTO();
        PhotoPropertiesUtil.copy(actual, sut, true, true);
        Assert.assertEquals(expected.toString(), actual.toString());

        logger.info("shouldModifyInMemory " + sut.toString());
    }


    @Test
    public void shouldClearInMemory() {
        PhotoPropertiesDTO expected = new PhotoPropertiesDTO();
        PhotoPropertiesUtil.copy(sut, expected, true, true);
        PhotoPropertiesDTO actual = new PhotoPropertiesDTO();
        PhotoPropertiesUtil.copy(actual, sut, true, true);
        actual.setPath(expected.path);
        Assert.assertEquals(expected.toString(), actual.toString());

        logger.info("shouldClearInMemory " + sut.toString());
    }

    private static IPhotoProperties getMeta(String fileName) {
        InputStream inputStream = TestUtil.getResourceInputStream(fileName);
        PhotoPropertiesXmpSegment xmpContent = new PhotoPropertiesXmpSegment();
        xmpContent.load(inputStream, "JUnit");
        return xmpContent;
    }

}

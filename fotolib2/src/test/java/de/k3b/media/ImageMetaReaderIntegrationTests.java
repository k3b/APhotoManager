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

import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.iptc.IptcDirectory;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import de.k3b.FotoLibGlobal;
import de.k3b.TestUtil;
import de.k3b.io.DateUtil;
import de.k3b.io.ListUtils;

/**
 * Created by k3b on 28.03.2017.
 */

public class ImageMetaReaderIntegrationTests {
    private static final Logger logger = LoggerFactory.getLogger(ImageMetaReaderIntegrationTests.class);

    private ImageMetaReader sut = null;
    @BeforeClass
    public static void initDirectories() {
        FotoLibGlobal.appName = "JUnit";
        FotoLibGlobal.appVersion = "ImageMetaReaderIntegrationTests";
    }

    @Before
    public void setup() throws IOException {
        ImageMetaReader.DEBUG = true;
        sut = getMeta("test-WitExtraData.jpg");
    }

    @Test
    public void shouldDump() throws IOException
    {
        // System.out.printf(sut.toString());
        logger.info(sut.toString());
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

    // low levelt implementaion detail test
    @Test
    public void shouldGetExifList()
    {
        List<String> expected = ListUtils.toStringList("Marker1","Marker2");
        sut.init();
        List<String> result = sut.getStringList("JUnit", sut.mExifDir, ExifDirectoryBase.TAG_WIN_KEYWORDS);
        assertEquals("", expected, result);
    }

    // low levelt implementaion detail test
    @Test
    public void shouldIptcList() throws IOException
    {
        List<String> expected = ListUtils.toStringList("Marker1","Marker2");
        sut.init();
        List<String> result = sut.getStringList("JUnit", sut.mIptcDir, IptcDirectory.TAG_KEYWORDS);
        assertEquals("", expected, result);
    }


    public static ImageMetaReader getMeta(String fileName) throws IOException {
        InputStream inputStream = TestUtil.getResourceInputStream(fileName);
        ImageMetaReader result = new ImageMetaReader().load(fileName, inputStream, null, "JUnit");
        return result;
    }

    protected void assertEquals(String msg, List<String> expected, List<String> actual) {
        String expectedString = (expected == null) ? null : ListUtils.toString("|", expected);
        String actualString = (actual == null) ? null : ListUtils.toString("|", actual);
        Assert.assertEquals(msg + ":" + expectedString + " != " + actualString, expectedString, actualString);
    }
}

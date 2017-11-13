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
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import de.k3b.FotoLibGlobal;
import de.k3b.TestUtil;
import de.k3b.io.FileUtils;

/**
 * Created by k3b on 06.04.2017.
 */


public class ExifInterfaceIntegrationTests {
    // Obtain a logger instance
    private static final Logger LOGGER = LoggerFactory.getLogger(ExifInterfaceIntegrationTests.class);
    private static final File OUTDIR = new File(TestUtil.OUTDIR_ROOT, "ExifInterfaceIntegrationTests").getAbsoluteFile();
    private static final HashMap<String, String> testItems = new HashMap<String, String>();

	private static final String specialChars = "\r\n日本人 (Japanese)\n" + 
												"العربية (Arabic)\n" +
												"简体中文 (simplified Chinese)\n" +
												"繁体中文 (traditional Chinese)\n" +
												"Français (French)\n" +
												"Русский (Russian)" ;
    @BeforeClass
    public static void initDirectories() {
        FotoLibGlobal.appName = "JUnit";
        FotoLibGlobal.appVersion = "ExifInterfaceIntegrationTests";

        FileUtils.delete(OUTDIR, null);
        OUTDIR.mkdirs();

        testItems.clear();

        testItems.put(ExifInterface.TAG_ARTIST					, "Artist");
        testItems.put(ExifInterface.TAG_COPYRIGHT				, "Copyright " + specialChars);
        testItems.put(ExifInterface.TAG_IMAGE_DESCRIPTION		, "ImageDescription " + specialChars);
        testItems.put(ExifInterface.TAG_DATETIME_DIGITIZED		, "1964:14:07 04:38:44");
        testItems.put(ExifInterface.TAG_DATETIME_ORIGINAL		, "1965:15:07 05:38:45");
        testItems.put(ExifInterface.TAG_FILE_SOURCE				, "FileSource");
        testItems.put(ExifInterface.TAG_IMAGE_UNIQUE_ID			, "ImageUniqueID");
        testItems.put(ExifInterface.TAG_USER_COMMENT			, "UserComment " + specialChars);

        testItems.put(ExifInterface.TAG_WIN_TITLE				, "XPTitle");
        testItems.put(ExifInterface.TAG_WIN_COMMENT				, "XPComment " + specialChars);
        testItems.put(ExifInterface.TAG_WIN_AUTHOR				, "XPAuthor");
        testItems.put(ExifInterface.TAG_WIN_KEYWORDS			, "XPKeywords");
        testItems.put(ExifInterface.TAG_WIN_SUBJECT				, "XPSubject");

        testItems.put(ExifInterface.TAG_GPS_LATITUDE_REF		, "S");
        testItems.put(ExifInterface.TAG_GPS_LONGITUDE_REF		, "E");
        testItems.put(ExifInterface.TAG_GPS_LATITUDE			, "53/1");
        testItems.put(ExifInterface.TAG_GPS_LONGITUDE			, "8/1");
        // Type is double.
        testItems.put(ExifInterface.TAG_APERTURE				, "5.6");
        // Type is int.
        testItems.put(ExifInterface.TAG_FLASH					, "1");

        testItems.put(ExifInterface.TAG_WIN_RATING				, "3");

        ExifInterface.DEBUG = true;
    }

    @Test
    public void shouldWriteExifToNonExif() throws IOException
    {
        String fileNameSrc = "NoExif.jpg";
        String fileNameDest = "shouldWriteExifToNonExif.jpg";

        ExifInterface sutRead = assertUpdateSameAsAfterWrite(fileNameDest, fileNameSrc, testItems);
        LOGGER.info(sutRead.toString());

    }

    @Test
    public void shouldUpdateExistingExif() throws IOException
    {
        String fileNameSrc = "test-WitExtraData.jpg";
        String fileNameDest = "shouldUpdateExistingExif.jpg";

        ExifInterface sutRead = assertUpdateSameAsAfterWrite(fileNameDest, fileNameSrc, testItems);

        LOGGER.info(sutRead.toString());

    }

    @Test
    @Ignore("Not implemented ExifInterface.UserComment=null for tif-com-segment is not implemented :-(")
    public void shouldClearUsercommentFromExistingExif() throws IOException
    {
        String fileNameSrc = "test-WitExtraData.jpg";
        String fileNameDest = "shouldClearUsercommentFromExistingExif.jpg";

        HashMap<String, String> testItems = new HashMap<String, String>();
        testItems.put(ExifInterface.TAG_USER_COMMENT			, null);

        ExifInterface sutRead = assertUpdateSameAsAfterWrite(fileNameDest, fileNameSrc, testItems);

        LOGGER.info(sutRead.toString());
    }

    @Test
    public void shouldHandlePrefixStringAscii() {
        String expected = "Hello ascii";

        byte[] encoded = ExifInterface.encodePrefixString(expected);

        String actual = ExifInterface.decodePrefixString(encoded.length , encoded, null);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void shouldHandlePrefixStringUtf() {
        String expected = "Hello world ÄÖÜ";

        byte[] encoded = ExifInterface.encodePrefixString(expected);

        String actual = ExifInterface.decodePrefixString(encoded.length , encoded, null);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void shouldHanldePrefixStringEmpty() {
        String expected = "";

        byte[] encoded = ExifInterface.encodePrefixString(expected);

        String actual = ExifInterface.decodePrefixString(encoded.length , encoded, null);
        Assert.assertEquals(expected, actual);
    }

    private ExifInterface assertUpdateSameAsAfterWrite(String fileNameDest, String fileNameSrc, HashMap<String, String> testItems) throws IOException {
        InputStream inputStream = ImageMetaReaderIntegrationTests.class.getResourceAsStream("images/" + fileNameSrc);

        final File sutFile = new File(OUTDIR, fileNameDest);
        OutputStream outputStream = new FileOutputStream(sutFile);

        ExifInterface sutWrite = new ExifInterface(sutFile.getAbsolutePath(), inputStream);
        for(String key : testItems.keySet()) {
            sutWrite.setAttribute(key, testItems.get(key));
        }

        String sutWriteText = sutWrite.toString();

        FileUtils.close(inputStream, fileNameSrc);

        inputStream = ImageMetaReaderIntegrationTests.class.getResourceAsStream("images/" + fileNameSrc);
        sutWrite.saveJpegAttributes(inputStream, outputStream, null);

        FileUtils.close(outputStream, sutFile);

        ExifInterface sutRead = new ExifInterface(sutFile.getAbsolutePath());

        Assert.assertEquals(sutWriteText, sutRead.toString());
        return sutRead;
    }


}

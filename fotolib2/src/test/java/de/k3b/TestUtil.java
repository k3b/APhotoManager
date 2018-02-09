/*
 * Copyright (c) 2015-2018 by k3b.
 *
 * This file is part of AndroFotoFinder / #APhotoManager.
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

package de.k3b;

import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import de.k3b.csv2db.csv.CsvReader;
import de.k3b.io.DateUtil;
import de.k3b.io.FileUtils;
import de.k3b.io.VISIBILITY;
import de.k3b.media.ImageMetaReaderIntegrationTests;
import de.k3b.media.MediaDTO;
import de.k3b.tagDB.TagConverter;

public class TestUtil {
    public static final File OUTDIR_ROOT = new File("./build/test-results/metafiles");

    public static Reader createReader(String csvSrc) {
		return new java.io.StringReader(csvSrc);
		// return new InputStreamReader(new ByteArrayInputStream(csvSrc.getBytes("UTF-8")));
	}

	public static CsvReader createParser(String csvSrc) {
		return new CsvReader(TestUtil.createReader(csvSrc));
	}

	public static MediaDTO createTestMediaDTO(int id) {
        MediaDTO result = new MediaDTO();

        result.setPath("Path" + id);
        result.setTitle("Title" + id);
        result.setDescription("Description" + id);
        String month = get2DigitString(id, 12);
        String day = get2DigitString(id, 30);
        String hour = get2DigitString(id, 24);
        String minute = get2DigitString(id, 60);
        result.setDateTimeTaken(DateUtil.parseIsoDate("" + (2000+ id) + "-" + month + "-" + day
                + "T" + hour + ":" + minute + ":" + minute));
        result.setLatitudeLongitude(50 + id + (0.01 * id), 10 + id + (0.01 * id));
        result.setTags(TagConverter.fromString("tagA" + id + TagConverter.TAG_DB_DELIMITER + "tagB" + id));
        result.setRating(Integer.valueOf(id % 6));

        result.setVisibility(VISIBILITY.PUBLIC);
        return result;
    }

    private static String get2DigitString(int value, int div) {
        return ("" + (((value -1) % div) +101)).substring(1);
    }

    public static InputStream getResourceInputStream(String fileName) {
        InputStream inputStream = ImageMetaReaderIntegrationTests.class.getResourceAsStream("images/" + fileName);
        Assert.assertNotNull("getResourceInputStream images/" + fileName, inputStream);
        return inputStream;
    }

    public static void saveTestResourceAs(String resourceName, File destination) throws IOException {
        InputStream sourceStream = getResourceInputStream(resourceName);

        FileUtils.copyReplace(sourceStream, destination);
        FileUtils.close(sourceStream,resourceName);
        destination.setLastModified(DateUtil.parseIsoDate("1972-03-04T05:06:07").getTime());
    }

}

/*
 * Copyright (c) 2015-2017 by k3b.
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import de.k3b.csv2db.csv.CsvReader;
import de.k3b.io.DateUtil;
import de.k3b.io.FileUtils;
import de.k3b.media.IMetaApi;
import de.k3b.media.ImageMetaReader;
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
        String month = ("" + (((id -1) % 12) +101)).substring(1);
        String day = ("" + (((id -1) % 30) +101)).substring(1);
        result.setDateTimeTaken(DateUtil.parseIsoDate("" + (2000+ id) + "-" + month + "-" + day));
        result.setLatitude(50 + id + (0.01 * id));
        result.setLongitude(10 + id + (0.01 * id));
        result.setTags(TagConverter.fromString("tagA" + id + TagConverter.TAG_DB_DELIMITER + "tagB" + id));
        result.setRating(id % 6);

        return result;
    }
    public static InputStream getResourceInputStream(String fileName) {
        InputStream inputStream = ImageMetaReaderIntegrationTests.class.getResourceAsStream("images/" + fileName);
        Assert.assertNotNull("getResourceInputStream images/" + fileName, inputStream);
        return inputStream;
    }

    public static void saveTestResourceAs(String resourceName, File destination) throws IOException {
        if (destination.exists()) destination.delete();
        FileOutputStream result = new FileOutputStream(destination);
        FileUtils.copy(getResourceInputStream(resourceName), result);
        result.flush();
        result.close();
    }
}

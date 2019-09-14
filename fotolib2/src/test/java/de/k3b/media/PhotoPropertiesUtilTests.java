/*
 * Copyright (c) 2016-2019 by k3b.
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

package de.k3b.media;

/**
 * Created by k3b on 10.10.2016.
 */

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import de.k3b.TestUtil;
import de.k3b.io.ListUtils;
import de.k3b.io.VISIBILITY;

public class PhotoPropertiesUtilTests {
    @Test
    public void shouldCopyAllFields() {
        PhotoPropertiesDTO expected = TestUtil.createTestMediaDTO(1);
        PhotoPropertiesDTO actual = new PhotoPropertiesDTO();
        PhotoPropertiesUtil.copy(actual, expected, true, true);
        Assert.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void shouldDeserializeAllFields() {
        PhotoPropertiesDTO expected = TestUtil.createTestMediaDTO(1);
        PhotoPropertiesAsString src = new PhotoPropertiesAsString().setData(expected);
        String serial = src.toString();
        PhotoPropertiesAsString dest = new PhotoPropertiesAsString().fromString(serial);

        PhotoPropertiesDTO actual = new PhotoPropertiesDTO(dest);
        Assert.assertEquals(expected.toString(), actual.toString());
    }


    @Test
    public void shouldCopyAllFieldsMetaApiWrapper() {
        PhotoPropertiesDTO expected = TestUtil.createTestMediaDTO(1);
        PhotoPropertiesDTO actual = new PhotoPropertiesDTO();
        PhotoPropertiesWrapper src = new PhotoPropertiesWrapper(expected, null);
        PhotoPropertiesWrapper dest = new PhotoPropertiesWrapper(null, actual);
        PhotoPropertiesUtil.copy(dest, src, true, true);
        Assert.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void shouldNotThrowMetaApiWrapperReadNull() {
        PhotoPropertiesDTO actual = new PhotoPropertiesDTO();
        PhotoPropertiesWrapper src = new PhotoPropertiesWrapper(null, null);
        PhotoPropertiesUtil.copy(actual, src, true, true);
    }

    @Test
    public void shouldNotThrowMetaApiWrapperWriteNull() {
        PhotoPropertiesDTO actual = new PhotoPropertiesDTO();
        PhotoPropertiesWrapper dest = new PhotoPropertiesWrapper(null, null);
        PhotoPropertiesUtil.copy(dest, actual, true, true);
    }


    @Test
    public void shouldCopyAllFieldsMetaApiChainReaderNull2() {
        PhotoPropertiesDTO expected = TestUtil.createTestMediaDTO(1);
        PhotoPropertiesDTO actual = new PhotoPropertiesDTO();
        PhotoPropertiesChainReader src = new PhotoPropertiesChainReader(null, expected);
        PhotoPropertiesUtil.copy(actual, src, true, true);
        Assert.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void shouldCopyAllFieldsMetaApiChainReaderEmpty2() {
        PhotoPropertiesDTO expected = TestUtil.createTestMediaDTO(1);
        PhotoPropertiesDTO actual = new PhotoPropertiesDTO();
        PhotoPropertiesChainReader src = new PhotoPropertiesChainReader(new PhotoPropertiesDTO(), expected);
        PhotoPropertiesUtil.copy(actual, src, true, true);
        Assert.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void shouldNotThrowMetaApiChainReaderNullNull() {
        PhotoPropertiesDTO actual = new PhotoPropertiesDTO();
        PhotoPropertiesChainReader src = new PhotoPropertiesChainReader(null, null);
        PhotoPropertiesUtil.copy(actual, src, true, true);
    }

    @Test
    public void shouldFindChanges() {
        IPhotoProperties item1 = TestUtil.createTestMediaDTO(3);
        IPhotoProperties item2 = TestUtil.createTestMediaDTO(3);

        // 3 changes
        item1.setTitle("other title");
        item1.setLatitudeLongitude(null,99.0);
        item2.setDateTimeTaken(null);

        // no change
        item1.setDescription(null);
        item2.setDescription(null);

        List<MediaFormatter.FieldID> result = PhotoPropertiesUtil.getChanges(item1, item2);
        Assert.assertEquals(ListUtils.toString(result), 4, result.size());
    }

    @Test
    public void shouldFindNonEmpty() {
        IPhotoProperties item1 = new PhotoPropertiesDTO();

        // 3 changes
        item1.setTitle("some title");
        item1.setLatitudeLongitude(99.0,99.0);

        List<MediaFormatter.FieldID> result = PhotoPropertiesUtil.getChanges(null, item1);
        Assert.assertEquals(ListUtils.toString(result), 3, result.size());
    }

    @Test
    public void shouldCopyAlways() {
        check("src", "dest", true, true, "src");
        check("src", null, true, true, "src");
        check(null, "dest", true, true, null);
    }

    @Test
    public void shouldCopyNotNull() {
        check("src", "dest", false, true, "src");
        check("src", null, false, true, "src");
        check(null, "dest", false, true, "dest");
    }

    @Test
    public void shouldCopyNoOverwrite() {
        check("src", "dest", true, false, "dest");
        check("src", null, true, false, "src");
        check(null, "dest", true, false, "dest");
    }

    private void check(String initalSrcValue, String initialDestValue,
                       boolean allowSetNull, boolean overwriteExisting, String expected) {
        IPhotoProperties src = new PhotoPropertiesDTO().setTitle(initalSrcValue);
        IPhotoProperties dest = new PhotoPropertiesDTO().setTitle(initialDestValue);

        PhotoPropertiesUtil.copy(dest, src, overwriteExisting, allowSetNull);
        Assert.assertEquals("(" + initalSrcValue +
                "," + initialDestValue +
                ")=>" +expected, expected, dest.getTitle());
    }

    @Test
    public void shouldCalculateModifiedPath() {
        check("/path/to/file.jpg-p", "/path/to/file.jpg", VISIBILITY.PRIVATE);
        check("/path/to/file.jpg", "/path/to/file.jpg-p", VISIBILITY.PUBLIC);
    }

    @Test
    public void shouldNotCalculateModifiedPath() {
        check(null, "/path/to/file.jpg", VISIBILITY.PUBLIC);
        check(null, "/path/to/file.jpg-p", VISIBILITY.PRIVATE);
        check(null, "/path/to/file.jpg", VISIBILITY.PRIVATE_PUBLIC);
        check(null, "/path/to/file.jpg", VISIBILITY.DEFAULT);
    }


    private void check(String expected, String actual, VISIBILITY visibility) {
        Assert.assertEquals(visibility +
                "(" + actual + ")=>" +expected, expected, PhotoPropertiesUtil.getModifiedPath(actual,visibility));
    }

    /***** Integrationtests via file system *********/
    private static final File INDIR = new File(TestUtil.OUTDIR_ROOT, "PhotoPropertiesUtilTests");

    @Test
    public void shouldInferAutoprocessingExifDefaultsFromExistingFiles() throws IOException {
        File[] jpgFilesToAnalyse = TestUtil.saveTestResourcesIn(INDIR,
                TestUtil.TEST_FILE_JPG_WITH_NO_EXIF,
                TestUtil.TEST_FILE_JPG_WITH_EXIF);
        PhotoPropertiesAsString result = PhotoPropertiesUtil.inferAutoprocessingExifDefaults(new PhotoPropertiesAsString(),
                jpgFilesToAnalyse);

        Assert.assertEquals(
                "regression: description, lat, lon, tags should be identical to " + TestUtil.TEST_FILE_JPG_WITH_EXIF,
                ",,ImageDescription,,27.818611,-15.764444,\"Marker1, Marker2\"", result.toString());
    }

}

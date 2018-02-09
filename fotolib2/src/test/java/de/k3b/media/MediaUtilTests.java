/*
 * Copyright (c) 2016-2018 by k3b.
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

import java.util.List;

import de.k3b.TestUtil;
import de.k3b.io.ListUtils;
import de.k3b.io.VISIBILITY;

public class MediaUtilTests {
    @Test
    public void shouldCopyAllFields() {
        MediaDTO expected = TestUtil.createTestMediaDTO(1);
        MediaDTO actual = new MediaDTO();
        MediaUtil.copy(actual, expected, true, true);
        Assert.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void shouldDeserializeAllFields() {
        MediaDTO expected = TestUtil.createTestMediaDTO(1);
        MediaAsString src = new MediaAsString().setData(expected);
        String serial = src.toString();
        MediaAsString dest = new MediaAsString().fromString(serial);

        MediaDTO actual = new MediaDTO(dest);
        Assert.assertEquals(expected.toString(), actual.toString());
    }


    @Test
    public void shouldCopyAllFieldsMetaApiWrapper() {
        MediaDTO expected = TestUtil.createTestMediaDTO(1);
        MediaDTO actual = new MediaDTO();
        MetaApiWrapper src = new MetaApiWrapper(expected, null);
        MetaApiWrapper dest = new MetaApiWrapper(null, actual);
        MediaUtil.copy(dest, src, true, true);
        Assert.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void shouldNotThrowMetaApiWrapperReadNull() {
        MediaDTO actual = new MediaDTO();
        MetaApiWrapper src = new MetaApiWrapper(null, null);
        MediaUtil.copy(actual, src, true, true);
    }

    @Test
    public void shouldNotThrowMetaApiWrapperWriteNull() {
        MediaDTO actual = new MediaDTO();
        MetaApiWrapper dest = new MetaApiWrapper(null, null);
        MediaUtil.copy(dest, actual, true, true);
    }


    @Test
    public void shouldCopyAllFieldsMetaApiChainReaderNull2() {
        MediaDTO expected = TestUtil.createTestMediaDTO(1);
        MediaDTO actual = new MediaDTO();
        MetaApiChainReader src = new MetaApiChainReader(null, expected);
        MediaUtil.copy(actual, src, true, true);
        Assert.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void shouldCopyAllFieldsMetaApiChainReaderEmpty2() {
        MediaDTO expected = TestUtil.createTestMediaDTO(1);
        MediaDTO actual = new MediaDTO();
        MetaApiChainReader src = new MetaApiChainReader(new MediaDTO(), expected);
        MediaUtil.copy(actual, src, true, true);
        Assert.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void shouldNotThrowMetaApiChainReaderNullNull() {
        MediaDTO actual = new MediaDTO();
        MetaApiChainReader src = new MetaApiChainReader(null, null);
        MediaUtil.copy(actual, src, true, true);
    }

    @Test
    public void shouldFindChanges() {
        IMetaApi item1 = TestUtil.createTestMediaDTO(3);
        IMetaApi item2 = TestUtil.createTestMediaDTO(3);

        // 3 changes
        item1.setTitle("other title");
        item1.setLatitudeLongitude(null,99.0);
        item2.setDateTimeTaken(null);

        // no change
        item1.setDescription(null);
        item2.setDescription(null);

        List<MediaUtil.FieldID> result = MediaUtil.getChanges(item1, item2);
        Assert.assertEquals(ListUtils.toString(result), 4, result.size());
    }

    @Test
    public void shouldFindNonEmpty() {
        IMetaApi item1 = new MediaDTO();

        // 3 changes
        item1.setTitle("some title");
        item1.setLatitudeLongitude(99.0,99.0);

        List<MediaUtil.FieldID> result = MediaUtil.getChanges(null, item1);
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
        IMetaApi src = new MediaDTO().setTitle(initalSrcValue);
        IMetaApi dest = new MediaDTO().setTitle(initialDestValue);

        MediaUtil.copy(dest, src, overwriteExisting, allowSetNull);
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
                "(" + actual + ")=>" +expected, expected, MediaUtil.getModifiedPath(actual,visibility));
    }

}

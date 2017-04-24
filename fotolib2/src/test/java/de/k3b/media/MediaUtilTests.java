/*
 * Copyright (c) 2016 by k3b.
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

package de.k3b.media;

/**
 * Created by k3b on 10.10.2016.
 */

import org.junit.Assert;
import org.junit.Test;

import de.k3b.TestUtil;

public class MediaUtilTests {
    @Test
    public void shouldCopyAllFields() {
        MediaDTO expected = TestUtil.createTestMediaDTO(1);
        MediaDTO actual = new MediaDTO();
        MediaUtil.copy(actual, expected, true, true);
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
    public void shouldCountChanges() {
        MediaDTO expected = TestUtil.createTestMediaDTO(1);
        Assert.assertEquals("all different", 7, MediaUtil.countChangedProperties(expected, TestUtil.createTestMediaDTO(2), false));
        Assert.assertEquals("all same", 0, MediaUtil.countChangedProperties(expected, TestUtil.createTestMediaDTO(1), false));
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

    private void check(String initalSrcValue, String initialDestValue, boolean allowSetNull, boolean overwriteExisting, String expected) {
        IMetaApi src = new MediaDTO().setTitle(initalSrcValue);
        IMetaApi dest = new MediaDTO().setTitle(initialDestValue);

        MediaUtil.copy(dest, src, allowSetNull, overwriteExisting);
        Assert.assertEquals("(" + initalSrcValue +
                "," + initialDestValue +
                ")=>" +expected, expected, dest.getTitle());
    }

}

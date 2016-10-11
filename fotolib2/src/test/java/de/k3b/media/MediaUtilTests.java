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
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

import de.k3b.tagDB.TagConverter;

public class MediaUtilTests {
    @Test
    public void shouldCopyAllFields() {
        MediaDTO expected = createTestItem(1);
        MediaDTO actual = new MediaDTO();
        MediaUtil.copy(actual, expected, true, true);
        Assert.assertEquals(expected.toString(), actual.toString());
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

    public static MediaDTO createTestItem(int id) {
        MediaDTO result = new MediaDTO();


        result.setPath("Path" + id);
        result.setTitle("Title" + id);
        result.setDescription("Description" + id);
        String month = ("" + (((id -1) % 12) +101)).substring(1);
        String day = ("" + (((id -1) % 30) +101)).substring(1);
        result.setDateTimeTaken(MediaUtil.parseIsoDate("" + (2000+ id) + "-" + month + "-" + day));
        result.setLatitude(50 + id + (0.01 * id));
        result.setLongitude(10 + id + (0.01 * id));
        result.setTags(TagConverter.fromString("tag" + id));

        return result;
    }
}

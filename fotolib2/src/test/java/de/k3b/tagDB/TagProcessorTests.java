/*
 * Copyright (c) 2017 by k3b.
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
package de.k3b.tagDB;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.k3b.io.ListUtils;

/**
 * Created by k3b on 09.01.2017.
 */

public class TagProcessorTests {
    private TagProcessor sut = null;

    @Before
    public void setup() {
        sut = new TagProcessor();
        sut.registerExistingTags(Arrays.asList("all", "single1"));
        sut.registerExistingTags(Arrays.asList("single2","all"));
    }

    @Test
    public void shouldCalculateAffected() throws Exception {
        Assert.assertEquals("getAffected "+ ListUtils.toString(sut.getAffected()), 3, sut.getAffected().size());
    }

    @Test
    public void shouldCalculateAll() throws Exception {
        Assert.assertEquals("getAllSet "+ ListUtils.toString(sut.getAllSet()), 1, sut.getAllSet().size());
    }

    @Test
    public void shouldGetUpdatedNone() throws Exception {
        List<String> updated = sut.getUpdated(Arrays.asList("all", "single1"), Arrays.asList("single1"), Arrays.asList("nonExistent"));
        Assert.assertEquals(null, updated);
    }

    @Test
    public void shouldGetUpdatedAdded() throws Exception {
        List<String> updated = sut.getUpdated(Arrays.asList("all", "single1"), Arrays.asList("single2"), Arrays.asList("nonExistent"));
        Assert.assertEquals(ListUtils.toString(updated), 3, updated.size());
    }

    @Test
    public void shouldGetUpdatedRemoved() throws Exception {
        List<String> updated = sut.getUpdated(Arrays.asList("all", "single1"), Arrays.asList("all"), Arrays.asList("single1"));
        Assert.assertEquals(ListUtils.toString(updated), 1, updated.size());
    }

    @Test
    public void shouldCalculateDiff() throws Exception {
        List<String> addedTags = new ArrayList<String>();
        List<String> removedTags = new ArrayList<String>();

        int changes = sut.getDiff(Arrays.asList("a", "b"), Arrays.asList("a","c"), addedTags, removedTags);

        Assert.assertEquals("removed", ListUtils.toString(Arrays.asList("b")), ListUtils.toString(removedTags) );
        Assert.assertEquals("added", ListUtils.toString(Arrays.asList("c")), ListUtils.toString(addedTags));
        Assert.assertEquals("# changes", 2, changes);
    }

    @Test
    public void shouldFormatTagsForBat() throws Exception {
        Assert.assertEquals("empty", null, TagConverter.asBatString());
        Assert.assertEquals("empty", "'a' 'b'".replaceAll("'","\""), TagConverter.asBatString("a","b"));
    }


}
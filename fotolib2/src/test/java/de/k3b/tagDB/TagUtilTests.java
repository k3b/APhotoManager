/*
 * Copyright (c) 2020 by k3b.
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
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Created by k3b on 02.05.2020.
 */

public class TagUtilTests {
    @Test
    public void shouldReversePath() {
        Tag c = createUnsavedRepo("a/b/c").findFirstByName("c");
        Assert.assertEquals("c <- b <- a", c.getReversePath());
    }

    @Test
    public void shouldGetPathElemensFromLastExpr() {
        String[] abc = TagExpression.getPathElemensFromLastExpr("x,y,a/b/c");
        Assert.assertEquals("size", 3, abc.length);
        Assert.assertEquals("[2]", "c", abc[2]);
    }

    @Test
    public void shouldPath() {
        Tag c = createUnsavedRepo("a/b/c").findFirstByName("c");
        Assert.assertEquals("/a/b/c", c.getPath());
    }

    @Test
    public void shouldEqualsString() {
        Tag c = createUnsavedRepo("a/b/c").findFirstByName("c");
        Assert.assertEquals(true, c.equals("c"));
    }

    @Test
    public void shouldCOMPARATOR_NAME_IGNORE_CASE() {
        Tag c = createUnsavedRepo("a/b/c").findFirstByName("c");
        Assert.assertEquals(0, Tag.COMPARATOR_NAME_IGNORE_CASE.compare(c, c));
    }

    @Test
    public void shouldAsDbStringWithWildcard() {
        List<String> abc = Arrays.asList(TagExpression.getPathElemens("a/b/c"));
        Assert.assertEquals("where tag like ", "%;a;%;b;%;c;%", TagConverter.asDbString("%", abc));
    }

    @Test
    public void shouldAsDbString() {
        List<String> abc = Arrays.asList(TagExpression.getPathElemens("a/b/c"));
        Assert.assertEquals("where tag in () ", "a, b, c", TagConverter.asDbString(null, abc));
    }

    @Test
    public void shouldAsBatchString() {
        List<String> abc = Arrays.asList(TagExpression.getPathElemens("a/b/c"));
        Assert.assertEquals("\"a\" \"b\" \"c\"", TagConverter.asBatString(abc));
    }

    @Test
    public void shouldGetPathElements() {
        String[] pathElemens = TagExpression.getPathElemens("/a");
        Assert.assertEquals(2, pathElemens.length);
        Assert.assertEquals("", pathElemens[0]);
    }

    private TagRepository createUnsavedRepo(String paths) {
        TagRepository result = new TagRepository(null);
        result.includePaths(null, paths);
        return result;
    }
}

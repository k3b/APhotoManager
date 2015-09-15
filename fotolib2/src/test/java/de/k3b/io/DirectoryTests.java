/*
 * Copyright (c) 2015 by k3b.
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
 
package de.k3b.io;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by EVE on 08.06.2015.
 */
public class DirectoryTests {
    @Test
    public void shoudGetAbsolute() {
        Directory root = new Directory("a", null, 0);
        IDirectory leave = new Directory("b", root, 0);
        Assert.assertEquals("/a/b", leave.getAbsolute());
    }

    @Test
    public void shoudCompress() {
        DirectoryBuilder builder = new DirectoryBuilder();
        builder.add("/a/b/c/", 0, 0);
        IDirectory result = builder.getRoot().getChildren().get(0);
        Assert.assertEquals("a/b/c", result.getRelPath());
    }

    @Test
    public void shouldCalculateStatistics() {
        DirectoryBuilder builder = new DirectoryBuilder();
        builder.add("/a/b", 1, 0);
        builder.add("/a/b/c",2, 0);
        builder.add("/a/b/c/d",4, 0);
        IDirectory root = builder.getRoot().getChildren().get(0);
        assertTree("a/b(1+1):(1+6)|c(1):(2+4)|d:(4)|", root);
    }

    @Test
    public void noAddShoudbeEmpty() {
        DirectoryBuilder builder = new DirectoryBuilder();
        IDirectory root = builder.getRoot();
        Assert.assertEquals(null, root);
    }
    @Test
    public void shoudBuild1() {
        DirectoryBuilder builder = new DirectoryBuilder();
        builder.add("/a/b/c/", 0, 0);
        IDirectory root = builder.getRoot().getChildren().get(0);
        Assert.assertEquals("/a/b/c", root.getAbsolute());
    }

    @Test
    public void shoudBuildDirPlus2Children() {
        DirectoryBuilder builder = new DirectoryBuilder();
        builder.add("/a/b", 0, 0);
        builder.add("/a/b/c1", 0, 0);
        builder.add("/a/b/c2", 0, 0);
        IDirectory root = builder.getRoot().getChildren().get(0);
        assertTree("a/b(2)|c1|c2|", root);
    }

    @Test
    public void siblingsShoudSplit() {
        DirectoryBuilder builder = new DirectoryBuilder();
        builder.add("/a/b/c1", 0, 0);
        builder.add("/a/b/c2", 0, 0);
        IDirectory root = builder.getRoot().getChildren().get(0);
        assertTree("a/b(2)|c1|c2|", root);
    }

    @Test
    public void shoudSetNonDirItemCount() {
        DirectoryBuilder builder = new DirectoryBuilder();
        builder.add("/a/b/c", 4, 0);
        Directory root = (Directory) builder.getRoot().getChildren().get(0);
        Assert.assertEquals(4, root.getNonDirItemCount());
    }

    @Test
    public void shoudAddNonDirItemCount() {
        DirectoryBuilder builder = new DirectoryBuilder();
        builder.add("/a/b/c", 4, 0);
        builder.add("/a/b/c", 3, 0);
        Directory root = (Directory) builder.getRoot().getChildren().get(0);
        Assert.assertEquals(7, root.getNonDirItemCount());
    }

    @Test
    public void shoudFormatTreeNoCount() {
        IDirectory root = new Directory("a", null, 0);
        assertTree("a|", root);
    }

    @Test
    public void shoudFormatTreeWithCount() {
        IDirectory root = new Directory("a", null,3).setNonDirSubItemCount(3+4).setDirCount(1).setSubDirCount(1+2);
        assertTree("a(1+2):(3+4)|", root);
    }

    @Test
    public void shoudFind() {
        Directory root = new Directory("", null, 0);
        Directory leave = root;
        leave = new Directory("a", leave, 0);
        leave = new Directory("b/c", leave, 0);
        IDirectory expected = new Directory("d", leave, 0);

        Assert.assertEquals(expected, root.find("/a/b/c/d/"));
    }

    protected void assertTree(String expected, IDirectory root) {
        Assert.assertEquals(expected, Directory.toTreeString(new StringBuilder(),(Directory) root, "|", Directory.OPT_ALL - Directory.OPT_AS_HTML).toString());
    }
}

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
        Directory leave = new Directory("b", root, 0);
        Assert.assertEquals("/a/b", leave.getAbsolute());
    }

    @Test
    public void shoudCompress() {
        DirectoryBuilder builder = new DirectoryBuilder();
        builder.add("/a/b/c/", 0);
        Directory result = builder.getRoot().getChildren().get(0);
        Assert.assertEquals("a/b/c", result.getRelPath());
    }

    @Test
    public void shouldCalculateStatistics() {
        DirectoryBuilder builder = new DirectoryBuilder();
        builder.add("/a/b", 1);
        builder.add("/a/b/c",2);
        builder.add("/a/b/c/d",4);
        Directory root = builder.getRoot().getChildren().get(0);
        assertTree("a/b(1+1):(1+6)|c(1):(2+4)|d:(4)|", root);
    }

    @Test
    public void noAddShoudbeEmpty() {
        DirectoryBuilder builder = new DirectoryBuilder();
        Directory root = builder.getRoot();
        Assert.assertEquals(null, root);
    }
    @Test
    public void shoudBuild1() {
        DirectoryBuilder builder = new DirectoryBuilder();
        builder.add("/a/b/c/", 0);
        Directory root = builder.getRoot().getChildren().get(0);
        Assert.assertEquals("/a/b/c", root.getAbsolute());
    }

    @Test
    public void shoudBuildDirPlus2Children() {
        DirectoryBuilder builder = new DirectoryBuilder();
        builder.add("/a/b", 0);
        builder.add("/a/b/c1", 0);
        builder.add("/a/b/c2", 0);
        Directory root = builder.getRoot().getChildren().get(0);
        assertTree("a/b(2)|c1|c2|", root);
    }

    @Test
    public void siblingsShoudSplit() {
        DirectoryBuilder builder = new DirectoryBuilder();
        builder.add("/a/b/c1", 0);
        builder.add("/a/b/c2", 0);
        Directory root = builder.getRoot().getChildren().get(0);
        assertTree("a/b(2)|c1|c2|", root);
    }

    @Test
    public void shoudSetNonDirItemCount() {
        DirectoryBuilder builder = new DirectoryBuilder();
        builder.add("/a/b/c", 4);
        Directory root = builder.getRoot().getChildren().get(0);
        Assert.assertEquals(4, root.getNonDirItemCount());
    }

    @Test
    public void shoudAddNonDirItemCount() {
        DirectoryBuilder builder = new DirectoryBuilder();
        builder.add("/a/b/c", 4);
        builder.add("/a/b/c", 3);
        Directory root = builder.getRoot().getChildren().get(0);
        Assert.assertEquals(7, root.getNonDirItemCount());
    }

    @Test
    public void shoudFormatTreeNoCount() {
        Directory root = new Directory("a", null, 0);
        assertTree("a|", root);
    }

    @Test
    public void shoudFormatTreeWithCount() {
        Directory root = new Directory("a", null,3).setNonDirSubItemCount(3+4).setDirCount(1).setSubDirCount(1+2);
        assertTree("a(1+2):(3+4)|", root);
    }

    protected void assertTree(String expected, Directory root) {
        Assert.assertEquals(expected, Directory.toTreeString(new StringBuilder(),root, "|").toString());
    }
}

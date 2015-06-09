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
    public void shouldAddDirPlusParent() {
        DirectoryBuilder builder = new DirectoryBuilder();
        builder.add("/a/b/c", 0);
        builder.add("/a/b", 1);
        Directory root = builder.getRoot();
        assertTree("|a/b|c|", root);
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
    public void shoudBuildDirPlusChild() {
        DirectoryBuilder builder = new DirectoryBuilder();
        builder.add("/a/b", 1);
        builder.add("/a/b/c", 0);
        Directory root = builder.getRoot();
        assertTree("|a/b|c|", root);
    }

    @Test
    public void shoudBuildDirPlus2Children() {
        DirectoryBuilder builder = new DirectoryBuilder();
        builder.add("/a/b", 0);
        builder.add("/a/b/c1", 0);
        builder.add("/a/b/c2", 0);
        Directory root = builder.getRoot();
        assertTree("|a/b|c1|c2|", root);
    }

    @Test
    public void siblingsShoudSplit() {
        DirectoryBuilder builder = new DirectoryBuilder();
        builder.add("/a/b/c1", 0);
        builder.add("/a/b/c2", 0);
        Directory root = builder.getRoot();
        assertTree("|a/b|c1|c2|", root);
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


    protected void assertTree(String expected, Directory root) {
        Assert.assertEquals(expected, Directory.toTreeString(new StringBuilder(),root, "|").toString());
    }
}

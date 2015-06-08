package de.k3b.io;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by EVE on 08.06.2015.
 */
public class DirectoryTests {
    @Test
    public void shoudGetFullPath() {
        Directory root = new Directory("/a/", null);
        Directory leave = new Directory("b/", root);
        Assert.assertEquals("/a/b/", leave.getFull());
    }
}

package de.k3b.io;

import org.junit.Assert;
import org.junit.Test;

public class FileNameUtilTest {

    @Test
    public void fixPath() {
        Assert.assertEquals("/path/to/dir/", FileNameUtil.fixPath("////path/to/dir/%"));
    }

    @Test
    public void createFileNameWitoutExtension() {
        Assert.assertEquals("file", FileNameUtil.createFileNameWitoutExtension("file.old"));
    }
}
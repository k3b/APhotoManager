package de.k3b.io;

import org.junit.Assert;
import org.junit.Test;

public class FileUtilsTest {

    @Test
    public void replaceExtension() {
        Assert.assertEquals("/path/to/file.new", FileUtils.replaceExtension("/path/to/file.old", ".new"));
        Assert.assertEquals("/path/to/file.new", FileUtils.replaceExtension("/path/to/file", ".new"));
        Assert.assertEquals("/path/to/file", FileUtils.replaceExtension("/path/to/file.old", ""));
        Assert.assertEquals("/path/to/file", FileUtils.replaceExtension("/path/to/file.", ""));
        Assert.assertEquals("file.new", FileUtils.replaceExtension("file.old", ".new"));
        Assert.assertEquals("file.new", FileUtils.replaceExtension("file.NEW", ".new"));
    }
}
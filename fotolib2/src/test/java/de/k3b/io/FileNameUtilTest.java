package de.k3b.io;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class FileNameUtilTest {

    @Test
    public void fixPath() {
        Assert.assertEquals("/path/to/dir/", FileNameUtil.fixPath("////path/to/dir/%"));
    }

    @Test
    public void createFileNameWitoutExtension() {
        Assert.assertEquals("file", FileNameUtil.createFileNameWitoutExtension("file.old"));
    }

    @Test
    public void getAnddroidRootDir() {
        String rootExt = "/storage/abcd-efgh";
        String rootint = "/storage/emulated/0";
        Assert.assertEquals(new File(rootExt), FileNameUtil.getAnddroidRootDir(new File(rootExt + "/path/to/dir")));
        Assert.assertEquals(new File(rootint), FileNameUtil.getAnddroidRootDir(new File(rootint + "/path/to/dir")));
        Assert.assertEquals(null, FileNameUtil.getAnddroidRootDir(new File("/path/to/dir")));
    }
}
package de.k3b.io;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;

import de.k3b.TestUtil;
import de.k3b.io.filefacade.IFile;

public class FileUtilsIntegrationTests {
    // Obtain a logger instance
    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtilsIntegrationTests.class);
    private static final IFile OUTDIR_ROOT = TestUtil.OUTDIR_ROOT.createFile("FileUtilsIntegrationTests");
    private static final IFile OUTDIR_NORMAL = TestUtil.OUTDIR_ROOT.createFile("normal/sub");

    @BeforeClass
    public static void initDirectories() {

        FileUtils.delete(OUTDIR_ROOT, null);

        OUTDIR_ROOT.mkdirs();
    }

    @Test
    public void shouldBeVisible() {
        IFile root = OUTDIR_ROOT.createFile("visbile/sub");
        root.mkdirs();
        IFile file = root.createFile("test.jpg");
        Assert.assertEquals("nomedia", false, FileUtils.isNoMedia(file, 5));
        Assert.assertEquals("HiddenFolder", false, FileUtils.isHiddenFolder(file));
    }

    @Test
    public void shouldBeHidden_HiddenFolder() {
        IFile root = OUTDIR_ROOT.createFile(".HiddenFolder/sub");
        root.mkdirs();
        IFile file = root.createFile("test.jpg");
        Assert.assertEquals("nomedia", true, FileUtils.isNoMedia(file, 5));
        Assert.assertEquals("HiddenFolder", true, FileUtils.isHiddenFolder(file));
    }

    @Test
    public void shouldBeHidden_Nomedia() throws IOException {
        IFile root = OUTDIR_ROOT.createFile("nomedia/sub");
        root.mkdirs();
        IFile marker = root.getParentFile().createFile(FileUtils.MEDIA_IGNORE_FILENAME);
        final OutputStream outputStream = marker.openOutputStream();
        outputStream.close();

        IFile file = root.createFile("test.jpg");
        Assert.assertEquals("nomedia", true, FileUtils.isNoMedia(file, 5));
        Assert.assertEquals("HiddenFolder", false, FileUtils.isHiddenFolder(file));
    }

}
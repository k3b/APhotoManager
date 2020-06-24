package de.k3b.io;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import de.k3b.LibGlobal;
import de.k3b.TestUtil;
import de.k3b.io.filefacade.IFile;
import de.k3b.media.ExifInterfaceIntegrationTests;

public class FileUtilsIntegrationTests {
    // Obtain a logger instance
    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtilsIntegrationTests.class);
    private static final IFile OUTDIR_ROOT = TestUtil.OUTDIR_ROOT.create("FileUtilsIntegrationTests");
    private static final IFile OUTDIR_NORMAL = TestUtil.OUTDIR_ROOT.create("normal/sub");

    @BeforeClass
    public static void initDirectories() {

        FileUtils.delete(OUTDIR_ROOT, null);

        OUTDIR_ROOT.mkdirs();
    }

    @Test
    public void shouldBeVisible() {
        IFile root = OUTDIR_ROOT.create("visbile/sub");
        root.mkdirs();
        IFile file = root.create("test.jpg");
        Assert.assertEquals("nomedia", false, FileUtils.isNoMedia(file, 5));
        Assert.assertEquals("HiddenFolder", false, FileUtils.isHiddenFolder(file));
    }

    @Test
    public void shouldBeHidden_HiddenFolder() {
        IFile root = OUTDIR_ROOT.create(".HiddenFolder/sub");
        root.mkdirs();
        IFile file = root.create("test.jpg");
        Assert.assertEquals("nomedia", true, FileUtils.isNoMedia(file, 5));
        Assert.assertEquals("HiddenFolder", true, FileUtils.isHiddenFolder(file));
    }

    @Test
    public void shouldBeHidden_Nomedia() throws IOException {
        IFile root = OUTDIR_ROOT.create("nomedia/sub");
        root.mkdirs();
        IFile marker = root.getParentFile().create(FileUtils.MEDIA_IGNORE_FILENAME);
        final OutputStream outputStream = marker.openOutputStream();
        outputStream.close();

        IFile file = root.create("test.jpg");
        Assert.assertEquals("nomedia", true, FileUtils.isNoMedia(file, 5));
        Assert.assertEquals("HiddenFolder", false, FileUtils.isHiddenFolder(file));
    }

}
/*
 * Copyright (c) 2017 by k3b.
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

package de.k3b.io;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import de.k3b.FotoLibGlobal;
import de.k3b.TestUtil;
import de.k3b.media.ExifInterface;
import de.k3b.media.MediaUtil;

/**
 * check autoprocessing workflow (#93:)
 * 
 * Created by k3b on 23.09.2017.
 */

public class FileCommandIntegrationTests {
    // Obtain a logger instance
    private static final Logger LOGGER = LoggerFactory.getLogger(FileCommandIntegrationTests.class);
    private static final File OUTDIR = new File(TestUtil.OUTDIR_ROOT, "FileCommandIntegrationTests/out").getAbsoluteFile();
    private static final File INJPG = new File(OUTDIR.getParentFile(), "in/myTestSource.jpg").getAbsoluteFile();

    @BeforeClass
    public static void setUpClass() throws IOException {
        FileUtils.delete(OUTDIR.getParentFile(), null);
        OUTDIR.mkdirs();

        TestUtil.saveTestResourceAs("test-WitExtraData.jpg", INJPG);
        TestUtil.saveTestResourceAs("test-WitExtraData.xmp", FileProcessor.getSidecar(INJPG, true));
        TestUtil.saveTestResourceAs("test-WitExtraData.xmp", FileProcessor.getSidecar(INJPG, false));
    }
    @Before
    public void setUp() throws IOException {
        FotoLibGlobal.appName = "JUnit";
        FotoLibGlobal.appVersion = "FileCommandIntegrationTests";

        ExifInterface.DEBUG = true;

    }

    @Test
    public void shouldCopyWithRenameProcessor() {
        FileCommands sut = new FileCommands();
        String outFileBaseName = "shouldCopyWithRenameProcessor";
        FileNameProcessor rename = new FileNameProcessor(null, outFileBaseName, null, OUTDIR);
        sut.moveOrCopyFilesTo(false, rename, null, OUTDIR, new Long[]{1l}, INJPG);
        assertFilesExist(true, outFileBaseName);
    }

    @Test
    public void shouldCopyWithRenameProcessor_noRename() {
        FileCommands sut = new FileCommands();
        String outFileBaseName = "Test";
        FileNameProcessor rename = new FileNameProcessor(null, outFileBaseName, null, OUTDIR);
        sut.moveOrCopyFilesTo(false, rename, null, OUTDIR, new Long[]{1l}, INJPG);
        assertFilesExist(true, "myTestSource"); // do not rename
    }

    private void assertFilesExist(boolean expected, String outFileBaseName) {
        assertFileExist(expected, outFileBaseName + ".jpg");
        assertFileExist(expected, outFileBaseName + ".jpg.xmp");
        assertFileExist(expected, outFileBaseName + ".xmp");
    }
    private void assertFileExist(boolean expected, String outFileName) {
        File f = new File(OUTDIR, outFileName);
        Assert.assertEquals(outFileName, expected, f.exists());
    }
}

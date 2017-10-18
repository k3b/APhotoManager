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
import de.k3b.io.collections.SelectedFiles;
import de.k3b.media.ExifInterface;
import de.k3b.media.IMetaApi;
import de.k3b.media.MediaDTO;

/**
 * check autoprocessing workflow (#93:)
 *
 * Created by k3b on 23.09.2017.
 */

public class FileCommandAutoIntegrationTests {
    // Obtain a logger instance
    private static final Logger LOGGER = LoggerFactory.getLogger(FileCommandAutoIntegrationTests.class);
    public static final String TEST_CLASS_NAME = FileCommandAutoIntegrationTests.class.getSimpleName();
    public static final Long[] FAKE_IDS = {1l};

    private static final File OUTDIR = new File(TestUtil.OUTDIR_ROOT, TEST_CLASS_NAME + "/out").getAbsoluteFile();
    private static final File INJPG = new File(OUTDIR.getParentFile(), "in/myTestSource.jpg").getAbsoluteFile();
    public static final String[] FAKE_INJPG = {INJPG.getAbsolutePath()};
    public static final SelectedFiles FAKE_SELECTED_FILES = new SelectedFiles(FAKE_INJPG, FAKE_IDS);

    private static final IMetaApi addExif = new MediaDTO().setTitle("title added by " + TEST_CLASS_NAME);

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
        FotoLibGlobal.appVersion = TEST_CLASS_NAME;

        ExifInterface.DEBUG = true;

    }

    @Test
    public void shouldCopy() {
        String outFileBaseName = "shouldCopy";
        FileCommands sut = createFileCommands(outFileBaseName);
        RuleFileNameProcessor rename = new RuleFileNameProcessor(null, outFileBaseName, null, OUTDIR);
        sut.moveOrCopyFilesTo(false, FAKE_SELECTED_FILES, rename, null, OUTDIR);
        assertFilesExist(true, outFileBaseName);
    }

    @Test
    public void shouldCopyExif() {
        String outFileBaseName = "shouldCopyExif";
        FileCommands sut = createFileCommands(outFileBaseName);
        RuleFileNameProcessor rename = new RuleFileNameProcessor(null, outFileBaseName, null, OUTDIR);
        sut.moveOrCopyFilesTo(false, FAKE_SELECTED_FILES, rename, addExif, OUTDIR);
        assertFilesExist(true, outFileBaseName);
    }

    public FileCommands createFileCommands(String outFileBaseName) {
        FileCommands result = new FileCommands();
        result.setLogFilePath(new File(OUTDIR, outFileBaseName + ".log").getAbsolutePath());
        return result;
    }

    @Test
    public void shouldCopyNoRename() {
        String outFileBaseName = "Test";
        FileCommands sut = createFileCommands(outFileBaseName);
        RuleFileNameProcessor rename = new RuleFileNameProcessor(null, outFileBaseName, null, OUTDIR);
        sut.moveOrCopyFilesTo(false, FAKE_SELECTED_FILES, rename, null, OUTDIR);
        assertFilesExist(true, "myTestSource"); // do not rename
    }

    private void assertFilesExist(boolean expected, String outFileBaseName) {
        assertFileExist(expected, outFileBaseName + ".jpg");
        assertFileExist(expected, outFileBaseName + ".jpg.xmp");
        assertFileExist(expected, outFileBaseName + ".xmp");
    }
    private void assertFileExist(boolean expected, String outFileName) {
        File f = new File(OUTDIR, outFileName);
        Assert.assertEquals(f.toString(), expected, f.exists());
    }
}

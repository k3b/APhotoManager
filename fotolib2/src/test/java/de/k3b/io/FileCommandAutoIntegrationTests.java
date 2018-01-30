/*
 * Copyright (c) 2017-2018 by k3b.
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
import java.util.Date;
import java.util.Properties;

import de.k3b.FotoLibGlobal;
import de.k3b.TestUtil;
import de.k3b.io.collections.SelectedFiles;
import de.k3b.media.ExifInterface;
import de.k3b.media.ExifInterfaceEx;
import de.k3b.media.IMetaApi;
import de.k3b.media.MediaDTO;
import de.k3b.media.MediaDiffCopy;
import de.k3b.media.MediaUtil;

/**
 * check autoprocessing workflow (#93:)
 *
 * Created by k3b on 23.09.2017.
 */

public class FileCommandAutoIntegrationTests {
    // Obtain a logger instance
    private static final Logger LOGGER = LoggerFactory.getLogger(FileCommandAutoIntegrationTests.class);
    public static final String TEST_CLASS_NAME = FileCommandAutoIntegrationTests.class.getSimpleName();

    private static final File OUTDIR = new File(TestUtil.OUTDIR_ROOT, TEST_CLASS_NAME + "/out").getAbsoluteFile();
    private static final File INJPG = new File(OUTDIR.getParentFile(), "in/myTestSource.jpg").getAbsoluteFile();
    public static final SelectedFiles FAKE_SELECTED_FILES = new SelectedFiles(INJPG.getAbsolutePath(), "1");

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

    /// TODO: move to seperate class FileCommandsIntegrationTests
    @Test
    public void shouldApplyExifChange() throws IOException {
        String outFileBaseName = "shouldApplyExifChange";
        FileCommands sut = createFileCommands(outFileBaseName);
        final File testJpg = new File(OUTDIR, outFileBaseName + ".jpg");
        TestUtil.saveTestResourceAs("NoExif.jpg", testJpg);

        MediaDiffCopy addExif = new MediaDiffCopy(new MediaDTO().setTitle("title added by " + TEST_CLASS_NAME), true);

        // false do not delete source file
        int changes = sut.applyExifChanges(false, addExif,new SelectedFiles(testJpg.toString(), "1"), null);

        Assert.assertEquals(1, changes);
    }

    @Test
    public void shouldCopy() {
        String outFileBaseName = "shouldCopy";
        FileCommands sut = createFileCommands(outFileBaseName);
        RuleFileNameProcessor rename = new RuleFileNameProcessor(null, outFileBaseName, null, OUTDIR);
        sut.moveOrCopyFilesTo(false, null, FAKE_SELECTED_FILES, rename, OUTDIR, null);
        assertFilesExist(true, outFileBaseName);
    }

    @Test
    public void shouldCopyExif() {
        String outFileBaseName = "shouldCopyExif";
        MediaDiffCopy addExif = new MediaDiffCopy(new MediaDTO().setTitle("title added by " + TEST_CLASS_NAME), true);

        FileCommands sut = createFileCommands(outFileBaseName);
        RuleFileNameProcessor rename = new RuleFileNameProcessor(null, outFileBaseName, null, OUTDIR);
        sut.moveOrCopyFilesTo(false, addExif, FAKE_SELECTED_FILES, rename, OUTDIR, null);
        assertFilesExist(true, outFileBaseName);
    }

    @Test
    public void shouldCopyNoRename() {
        String outFileBaseName = "shouldCopyNoRename";
        FileCommands sut = createFileCommands(outFileBaseName);
        RuleFileNameProcessor rename = new RuleFileNameProcessor(null, "Test", null, OUTDIR);
        sut.moveOrCopyFilesTo(false, null, FAKE_SELECTED_FILES, rename, OUTDIR, null);
        assertFilesExist(true, "myTestSource"); // has still old name. Not Renamed
    }

    @Test
    public void autoShouldAddTagSameFileNoRenameRule() throws IOException {
        final String outFileBaseName = "autoShouldAddTagSameFileNoRenameRule";
        final String tagAdded = outFileBaseName + "_" + (DateUtil.toIsoDateTimeString(new Date()).replace(":","_") );
        final File inFile = new File(OUTDIR, outFileBaseName + ".jpg");

        TestUtil.saveTestResourceAs("test-WitExtraData.jpg", inFile);

        FileCommands sut = createFileCommands(outFileBaseName);
        SelectedFiles selectedFiles = new SelectedFiles(inFile.getAbsolutePath(), "1");
        final IMetaApi exifChanges = new MediaDTO();
        exifChanges.setTags(ListUtils.fromString(tagAdded));

        PhotoWorkFlowDto autoProccessData = new PhotoWorkFlowDto(OUTDIR, new Properties())
                .setMediaDefaults(exifChanges);

        int changes = sut.moveOrCopyFilesTo(true, selectedFiles, OUTDIR,
                autoProccessData, null);

        ExifInterfaceEx result = new ExifInterfaceEx(inFile.getAbsolutePath(), null, null, "");

        Assert.assertEquals(tagAdded, true, result.getTags().contains(tagAdded));

    }

    @Test
    public void autoShouldAddTagSameFileRenameRuleMatching() throws IOException {
        final String outFileBaseName = "autoShouldAddTagSameFileRenameRuleMatching";
        final String tagAdded = outFileBaseName + "_" + (DateUtil.toIsoDateTimeString(new Date()).replace(":","_") );
        final File inFile = new File(OUTDIR, outFileBaseName + ".jpg");

        TestUtil.saveTestResourceAs("test-WitExtraData.jpg", inFile);

        FileCommands sut = createFileCommands(outFileBaseName);
        SelectedFiles selectedFiles = new SelectedFiles(inFile.getAbsolutePath(), "1");
        final IMetaApi exifChanges = new MediaDTO();
        exifChanges.setTags(ListUtils.fromString(tagAdded));

        PhotoWorkFlowDto autoProccessData = new PhotoWorkFlowDto(OUTDIR, new Properties())
                .setMediaDefaults(exifChanges).setName("ShouldAddTagSameFile");

        int changes = sut.moveOrCopyFilesTo(true, selectedFiles, OUTDIR,
                autoProccessData, null);

        ExifInterfaceEx result = new ExifInterfaceEx(inFile.getAbsolutePath(), null, null, "");

        Assert.assertEquals(tagAdded, true, result.getTags().contains(tagAdded));

    }

    @Test
    public void autoShouldAddTagWithRename() throws IOException {
        final String outFileBaseName = "autoShouldAddTagWithRename";
        final String tagAdded = outFileBaseName + "_" + (DateUtil.toIsoDateTimeString(new Date()).replace(":","_") );
        final File inFile = new File(OUTDIR, outFileBaseName + "-old.jpg");

        TestUtil.saveTestResourceAs("test-WitExtraData.jpg", inFile);

        FileCommands sut = createFileCommands(outFileBaseName);
        SelectedFiles selectedFiles = new SelectedFiles(inFile.getAbsolutePath(), "1");
        final IMetaApi exifChanges = new MediaDTO();
        exifChanges.setTags(ListUtils.fromString(tagAdded));

        PhotoWorkFlowDto autoProccessData = new PhotoWorkFlowDto(OUTDIR, new Properties())
                .setMediaDefaults(exifChanges).setName(outFileBaseName + "-new");

        int changes = sut.moveOrCopyFilesTo(true, selectedFiles, OUTDIR,
                autoProccessData, null);

        Assert.assertEquals(false, inFile.exists());

    }

    @Test
    public void shouldMoveRenameAutoSameDir() throws IOException {
        String outFileBaseName = "shouldMoveRename";
        final String originalName = outFileBaseName + "-old";
        File inFile = new File(OUTDIR, originalName + ".jpg");

        TestUtil.saveTestResourceAs("NoExif.jpg", inFile);

        FileCommands sut = createFileCommands(outFileBaseName);

        final String newName = outFileBaseName + "-new";
        SelectedFiles selectedFiles = new SelectedFiles(inFile.getAbsolutePath(), "1");

        // 0 avoid rounding of lat/lon; visibility not supported is only public
        final IMetaApi exifChanges = TestUtil.createTestMediaDTO(0).setVisibility(VISIBILITY.PUBLIC);
        PhotoWorkFlowDto autoProccessData = new PhotoWorkFlowDto(OUTDIR, new Properties())
                .setName(newName).setMediaDefaults(exifChanges);

        int changes = sut.moveOrCopyFilesTo(true, selectedFiles, OUTDIR,
                autoProccessData, null);

        assertFileExist(true, newName + ".jpg");
        assertFileExist(false, originalName + ".jpg"); // do not rename

        ExifInterfaceEx result = new ExifInterfaceEx(new File(OUTDIR, newName + ".jpg").getAbsolutePath(), null, null, "");

        String exprected = MediaUtil.toString(exifChanges, false, null, MediaUtil.FieldID.clasz, MediaUtil.FieldID.path);
        String current = MediaUtil.toString(result, false, null, MediaUtil.FieldID.clasz, MediaUtil.FieldID.path);
        Assert.assertEquals(exprected, current);

    }

    @Test
    public void shouldChangeFileNameOnVisibilityPrivate() throws IOException {
        String outFileBaseName = "shouldChangeFileNameOnVisibilityPrivate";
        File inFile = new File(OUTDIR, outFileBaseName + ".jpg");

        TestUtil.saveTestResourceAs("NoExif.jpg", inFile);

        FileCommands sut = createFileCommands(outFileBaseName);

        final String newName = outFileBaseName + "-new";
        SelectedFiles selectedFiles = new SelectedFiles(inFile.getAbsolutePath(), "1");

        //  final IMetaApi exifChanges = new MediaDTO().setVisibility(VISIBILITY.PUBLIC).setRating(3);
        final IMetaApi exifChanges = new MediaDTO().setVisibility(VISIBILITY.PRIVATE);

        PhotoWorkFlowDto autoProccessData = new PhotoWorkFlowDto(OUTDIR, new Properties())
                .setMediaDefaults(exifChanges);

        int changes = sut.moveOrCopyFilesTo(true, selectedFiles, OUTDIR,
                autoProccessData, null);

        assertFileExist(true, outFileBaseName + ".jpg-p");
        assertFileExist(false, outFileBaseName + ".jpg");
    }

    private FileCommands createFileCommands(String outFileBaseName) {
        FileCommands result = new FileCommands();
        result.setLogFilePath(new File(OUTDIR, outFileBaseName + ".log").getAbsolutePath());
        return result;
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

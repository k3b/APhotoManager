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
import de.k3b.media.JpgMetaWorkflow;
import de.k3b.media.MediaDTO;
import de.k3b.media.MediaDiffCopy;
import de.k3b.media.MediaUtil;
import de.k3b.transactionlog.TransactionLoggerBase;

/**
 * check autoprocessing workflow (#93:)
 *
 * Created by k3b on 23.09.2017.
 */

public class FileCommandAutoIntegrationTests {
    // Obtain a logger instance
    private static final Logger LOGGER = LoggerFactory.getLogger(FileCommandAutoIntegrationTests.class);
    public static final String TEST_CLASS_NAME = FileCommandAutoIntegrationTests.class.getSimpleName();

    private static final String FAKE_ID = "1";
    private static final String FAKE_DATE = "1223372036854775807";

    private static final File OUTDIR = new File(TestUtil.OUTDIR_ROOT, TEST_CLASS_NAME + "/out").getAbsoluteFile();
    private static final File INDIR = new File(TestUtil.OUTDIR_ROOT, TEST_CLASS_NAME + "/in").getAbsoluteFile();
    private static final File INJPG = new File(INDIR, "myTestSource.jpg").getAbsoluteFile();
    public static final SelectedFiles FAKE_SELECTED_FILES = SelectedFiles.create(INJPG.getAbsolutePath(), FAKE_ID, FAKE_DATE);

    @BeforeClass
    public static void setUpClass() throws IOException {
        FileUtils.delete(OUTDIR.getParentFile(), null);
        OUTDIR.mkdirs();

        TestUtil.saveTestResourceAs("test-WitExtraData.jpg", INJPG);
        TestUtil.saveTestResourceAs("test-WitExtraData.xmp", FileProcessor.getSidecar(INJPG, true));
        TestUtil.saveTestResourceAs("test-WitExtraData.xmp", FileProcessor.getSidecar(INJPG, false));

        LOGGER.info(" outdir:" + OUTDIR.getAbsolutePath());
    }
    @Before
    public void setUp() throws IOException {
        FotoLibGlobal.appName = "JUnit";
        FotoLibGlobal.appVersion = TEST_CLASS_NAME;

        ExifInterface.DEBUG = true;
        FotoLibGlobal.debugEnabledJpgMetaIo = true;
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
        int changes = sut.applyExifChanges(false, addExif,SelectedFiles.create(testJpg.toString(), FAKE_ID, FAKE_DATE), null);

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
        SelectedFiles selectedFiles = SelectedFiles.create(inFile.getAbsolutePath(), FAKE_ID, FAKE_DATE);
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
    public void shouldCopyAutoPrivate() throws IOException {
        checkMoveCopyWithAutoPrivate("shouldCopyAutoPrivate", false);
    }

    @Test
    public void shouldMoveAutoPrivate() throws IOException {
        checkMoveCopyWithAutoPrivate("shouldMoveAutoPrivate", true);
    }

    protected void checkMoveCopyWithAutoPrivate(final String outFileBaseName, boolean move) throws IOException {

        final File inFile = new File(INDIR, outFileBaseName + ".jpg");
        final File outFileExpexted = new File(OUTDIR, outFileBaseName + ".jpg-p");
        final File outFileNotExpexted = new File(OUTDIR, outFileBaseName + ".jpg");

        TestUtil.saveTestResourceAs("test-WitExtraData.jpg", inFile);

        FileCommands sut = createFileCommands(outFileBaseName);
        SelectedFiles selectedFiles = SelectedFiles.create(inFile.getAbsolutePath(), FAKE_ID, FAKE_DATE);
        final IMetaApi exifChanges = new MediaDTO();
        exifChanges.setVisibility(VISIBILITY.PRIVATE);

        PhotoWorkFlowDto autoProccessData = new PhotoWorkFlowDto(OUTDIR, new Properties())
                .setMediaDefaults(exifChanges);

        sut.moveOrCopyFilesTo(move, selectedFiles, OUTDIR,
                autoProccessData, null);

        Assert.assertEquals("Src Exists " + inFile.getName(), !move, inFile.exists());
        Assert.assertEquals("Dest Exists " + outFileExpexted.getName(), true, outFileExpexted.exists());
        Assert.assertEquals("Dest Not Exists " + outFileNotExpexted.getName(), false, outFileNotExpexted.exists());
    }


    @Test
    public void autoShouldAddTagSameFileRenameRuleMatching() throws IOException {
        final String outFileBaseName = "autoShouldAddTagSameFileRenameRuleMatching";
        final String tagAdded = outFileBaseName + "_" + (DateUtil.toIsoDateTimeString(new Date()).replace(":","_") );
        final File inFile = new File(OUTDIR, outFileBaseName + ".jpg");

        TestUtil.saveTestResourceAs("test-WitExtraData.jpg", inFile);

        FileCommands sut = createFileCommands(outFileBaseName);
        SelectedFiles selectedFiles = SelectedFiles.create(inFile.getAbsolutePath(), FAKE_ID, FAKE_DATE);
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
        SelectedFiles selectedFiles = SelectedFiles.create(inFile.getAbsolutePath(), FAKE_ID, FAKE_DATE);
        final IMetaApi exifChanges = new MediaDTO();
        exifChanges.setTags(ListUtils.fromString(tagAdded));

        PhotoWorkFlowDto autoProccessData = new PhotoWorkFlowDto(OUTDIR, new Properties())
                .setMediaDefaults(exifChanges).setName(outFileBaseName + "-new");

        int changes = sut.moveOrCopyFilesTo(true, selectedFiles, OUTDIR,
                autoProccessData, null);

        Assert.assertEquals(false, inFile.exists());

    }

    @Test
    public void shouldMoveRenameAutoExifSameDir() throws IOException {
        String outFileBaseName = "shouldMoveRenameAutoExifSameDir";
        final String originalName = outFileBaseName + "-old";
        File inFile = new File(OUTDIR, originalName + ".jpg");

        TestUtil.saveTestResourceAs("NoExif.jpg", inFile);

        FileCommands sut = createFileCommands(outFileBaseName);

        final String newName = outFileBaseName + "-new";
        SelectedFiles selectedFiles = SelectedFiles.create(inFile.getAbsolutePath(), FAKE_ID, FAKE_DATE);

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
        SelectedFiles selectedFiles = SelectedFiles.create(inFile.getAbsolutePath(), FAKE_ID, FAKE_DATE);

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
        FileCommands result = new FileCommands() {
            public JpgMetaWorkflow createWorkflow(TransactionLoggerBase logger, String dbgContext) {
                return new JpgMetaWorkflow(logger) {
                    protected long updateMediaDB(long id, String oldJpgAbsolutePath, File newJpgFile) {
                        // to verify that id has been updated
                        return id + 1;
                    }
                };
            }

        };
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

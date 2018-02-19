/*
 * Copyright (c) 2015-2017 by k3b.
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
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;

import de.k3b.io.collections.SelectedFiles;
import de.k3b.transactionlog.MediaTransactionLogEntryType;

import static org.mockito.Mockito.*;
/**
 * Created by k3b on 06.08.2015.
 */
public class FileCommandTests {
    private static final File X_FAKE_OUTPUT_DIR = new File("/fakeOutputDir").getAbsoluteFile();
    private static final File X_FAKE_INPUT_DIR = new File("/fakeInputDir").getAbsoluteFile();
    private FileCommands sut;

    MediaTransactionLogEntryType lastMediaTransactionLogEntryType;
    class FileCommandsWithFakeTransactionLog extends FileCommands {
        @Override public void addTransactionLog(
                long currentMediaID, String fileFullPath, long modificationDate,
                MediaTransactionLogEntryType mediaTransactionLogEntryType, String commandData) {
            lastMediaTransactionLogEntryType = mediaTransactionLogEntryType;
        }
    }
    @Before
    public void setUp() {
        sut = spy(new FileCommandsWithFakeTransactionLog());
        doReturn(true).when(sut).osCreateDirIfNeccessary(any(File.class));
        doReturn(true).when(sut).osFileMoveOrCopy(anyBoolean(), any(File.class), any(File.class));
        doReturn(true).when(sut).osDeleteFile(any(File.class));
        lastMediaTransactionLogEntryType = null;
    }

    @Test
    public void shouldCopy() {
        registerFakeFiles(sut);
        SelectedFiles selectedFiles = createTestSelectedFiles(X_FAKE_OUTPUT_DIR, "a.jpg");

        sut.moveOrCopyFilesTo(false, selectedFiles, X_FAKE_OUTPUT_DIR, null);

        Assert.assertEquals("MediaTransactionLogEntryType", MediaTransactionLogEntryType.COPY, lastMediaTransactionLogEntryType);
        verify(sut).osFileMoveOrCopy(false, new File(X_FAKE_OUTPUT_DIR, "a.jpg"), createTestFile(X_FAKE_OUTPUT_DIR, "a.jpg"));
    }

    @Test
    public void shouldCopyWitRenameExistingMultiple() {
        registerFakeFiles(sut, "a.jpg", "b.png", "b(1).png");
        SelectedFiles selectedFiles = createTestSelectedFiles(X_FAKE_INPUT_DIR, "a.jpg", "b.png");

        sut.moveOrCopyFilesTo(false, selectedFiles, X_FAKE_OUTPUT_DIR, null);

        Assert.assertEquals("MediaTransactionLogEntryType", MediaTransactionLogEntryType.COPY, lastMediaTransactionLogEntryType);
        verify(sut).osFileMoveOrCopy(false, new File(X_FAKE_OUTPUT_DIR, "a(1).jpg"), createTestFile(X_FAKE_INPUT_DIR, "a.jpg"));
        verify(sut).osFileMoveOrCopy(false, new File(X_FAKE_OUTPUT_DIR, "b(2).png"), createTestFile(X_FAKE_INPUT_DIR, "b.png"));
    }

    @Test
    public void shouldMoveExistingWithXmp() {
        registerFakeFiles(sut, "a.jpg", "a.xmp", "a.jpg.xmp"); // a(3) is next possible

        File destFile = new File(X_FAKE_OUTPUT_DIR, "a.jpg");

        // do not rename duplicate
        Mockito.doReturn(destFile).when(sut).renameDuplicate(destFile);

        SelectedFiles selectedFiles = createTestSelectedFiles(X_FAKE_INPUT_DIR, "a.jpg");

        sut.moveOrCopyFilesTo(true, selectedFiles, X_FAKE_OUTPUT_DIR, null);

        Assert.assertEquals("MediaTransactionLogEntryType", MediaTransactionLogEntryType.MOVE, lastMediaTransactionLogEntryType);
        verify(sut).osFileMoveOrCopy(true, new File(X_FAKE_OUTPUT_DIR, "a.jpg"), createTestFile(X_FAKE_INPUT_DIR, "a.jpg"));
        verify(sut).osFileMoveOrCopy(true, new File(X_FAKE_OUTPUT_DIR, "a.xmp"), createTestFile(X_FAKE_INPUT_DIR, "a.xmp"));
        verify(sut).osFileMoveOrCopy(true, new File(X_FAKE_OUTPUT_DIR, "a.jpg.xmp"), createTestFile(X_FAKE_INPUT_DIR, "a.jpg.xmp"));
    }

    @Test
    public void shouldDeleteExistingWithXmp() {
        registerFakeFiles(sut, "a.jpg", "a.xmp", "a.jpg.xmp");
        SelectedFiles selectedFiles = createTestSelectedFiles(X_FAKE_OUTPUT_DIR, "a.jpg");

        sut.deleteFiles(selectedFiles, null);

        Assert.assertEquals("MediaTransactionLogEntryType", MediaTransactionLogEntryType.DELETE, lastMediaTransactionLogEntryType);
        verify(sut).osDeleteFile(createTestFile(X_FAKE_OUTPUT_DIR, "a.jpg"));
        verify(sut).osDeleteFile(createTestFile(X_FAKE_OUTPUT_DIR, "a.xmp"));
        verify(sut).osDeleteFile(createTestFile(X_FAKE_OUTPUT_DIR, "a.jpg.xmp"));
    }

    /** these files exist in source-dir and in dest-dir */
    private static void registerFakeFiles(FileProcessor sut, String... filenames) {
        if (filenames.length == 0) {
            doReturn(false).when(sut).osFileExists(any(File.class));
        } else {
            for (String filename : filenames) {
                doReturn(true).when(sut).osFileExists(new File(X_FAKE_OUTPUT_DIR, filename));
                doReturn(true).when(sut).osFileExists(createTestFile(X_FAKE_OUTPUT_DIR, filename));
                doReturn(true).when(sut).osFileExists(new File(X_FAKE_INPUT_DIR, filename));
                doReturn(true).when(sut).osFileExists(createTestFile(X_FAKE_INPUT_DIR, filename));
            }
        }
    }

    private static File createTestFile(File destDir, String name) {
        return new File(destDir, name);
    }

    private SelectedFiles createTestSelectedFiles(File destDir, String... fileNames) {
        String[] paths = new String[fileNames.length];
        Long[] ids = new Long[fileNames.length];
        int pos = 0;
        for (String file : fileNames) {
            ids[pos] = Long.valueOf(pos+1);
            paths[pos++] = createTestFile(destDir, file).getAbsolutePath();
        }
        return new SelectedFiles(paths, ids, null);
    }


}

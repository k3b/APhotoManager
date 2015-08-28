/*
 * Copyright (c) 2015 by k3b.
 *
 * This file is part of AndroFotoFinder.
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

import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.mockito.Mockito.*;
/**
 * Created by k3b on 06.08.2015.
 */
public class FileCommandTests {
    private static final File X_FAKE_OUTPUT_DIR = new File("x:/fakeOutputDir");
    FileCommands sut;
    @Before
    public void setup() {
        sut = spy(new FileCommands());
        doReturn(true).when(sut).osCreateDirIfNeccessary(any(File.class));
        doReturn(true).when(sut).osFileMoveOrCopy(anyBoolean(), any(File.class), any(File.class));
        doReturn(true).when(sut).osDeleteFile(any(File.class));
    }

    @Test
    public void shouldCopy() {
        registerFakeFiles(sut);
        sut.moveOrCopyFilesTo(false, X_FAKE_OUTPUT_DIR, createTestFiles("a.jpg"));

        verify(sut).osFileMoveOrCopy(false, new File(X_FAKE_OUTPUT_DIR, "/a.jpg"), createTestFile("a.jpg"));

    }

    @Test
    public void shouldCopyWitRenameExistingMultible() {
        registerFakeFiles(sut, "a.jpg", "b.png", "b(1).png");
        sut.moveOrCopyFilesTo(false, X_FAKE_OUTPUT_DIR, createTestFiles("a.jpg", "b.png"));

        verify(sut).osFileMoveOrCopy(false, new File(X_FAKE_OUTPUT_DIR, "a(1).jpg"), createTestFile("a.jpg"));
        verify(sut).osFileMoveOrCopy(false, new File(X_FAKE_OUTPUT_DIR, "b(2).png"), createTestFile("b.png"));
    }

    @Test
    public void shouldCopyRenameExistingWithXmp() {
        registerFakeFiles(sut, "a.jpg", "a.xmp", "a(1).xmp", "a(2).jpg"); // a(3) is next possible

        sut.moveOrCopyFilesTo(false, X_FAKE_OUTPUT_DIR, createTestFiles("a.jpg"));

        verify(sut).osFileMoveOrCopy(false, new File(X_FAKE_OUTPUT_DIR, "a(3).jpg"), createTestFile("a.jpg"));
        verify(sut).osFileMoveOrCopy(false, new File(X_FAKE_OUTPUT_DIR, "a(3).xmp"), createTestFile("a.xmp"));
    }

    @Test
    public void shouldDeleteExistingWithXmp() {
        registerFakeFiles(sut, "a.jpg", "a.xmp");
        sut.deleteFiles(createTestFile("a.jpg").getAbsolutePath());

        verify(sut).osDeleteFile(createTestFile("a.jpg"));
        verify(sut).osDeleteFile(createTestFile("a.xmp"));
    }

    /** these files exist in source-dir and in dest-dir */
    private static void registerFakeFiles(FileCommands sut, String... filenames) {
        if (filenames.length == 0) {
            doReturn(false).when(sut).osFileExists(any(File.class));
        } else {
            for (String filename : filenames) {
                doReturn(true).when(sut).osFileExists(new File(X_FAKE_OUTPUT_DIR, filename));
                doReturn(true).when(sut).osFileExists(createTestFile(filename));
            }
        }
    }

    static File[] createTestFiles(String... files) {
        File[] result = new File[files.length];
        int pos = 0;
        for (String file : files) {
            result[pos++] = createTestFile(file);
        }
        return result;
    }

    private static File createTestFile(String name) {
        return new File(name).getAbsoluteFile();
    }
}

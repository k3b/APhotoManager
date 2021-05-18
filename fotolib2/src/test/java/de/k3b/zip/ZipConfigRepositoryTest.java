/*
 * Copyright (c) 2020 by k3b.
 *
 * This file is part of #toGoZip (https://github.com/k3b/ToGoZip/).
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
package de.k3b.zip;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;

import de.k3b.LibGlobal;
import de.k3b.TestUtil;
import de.k3b.io.filefacade.IFile;

public class ZipConfigRepositoryTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZipConfigRepositoryTest.class);

    private static final IFile OUTDIR = TestUtil.OUTDIR_ROOT.createIFile("ZipRepositoryTests");

    @BeforeClass
    public static void initDirectories() {
        OUTDIR.mkdirs();
        LibGlobal.zipFileDir = OUTDIR;
        LibZipGlobal.debugEnabled = false;
    }

    @Test
    public void isZipConfig() {
        assertIsZipConfigUri(false, "http://server/path/to/test.jpg");
        assertIsZipConfigUri(true, "http://server/path/to/test.zip.apm.config");
    }

    private void assertIsZipConfigUri(boolean expected, String uri) {
        Assert.assertEquals(expected, ZipConfigRepository.isZipConfig(uri));
    }

    @Test
    public void getZipConfigOrNull_notFound() {
        final String name = "getZipConfigOrNull_notFound.zip";
        ZipConfigRepository repository = createUnsavedZipConfigRepository(name);

        Assert.assertNull(ZipConfigRepository.getZipConfigOrNull(name));
    }

    @Test
    public void getZipConfigOrNull_found() {
        final String name = "getZipConfigOrNull_found.zip";
        createUnsavedZipConfigRepository(name).save();

        final IZipConfig found = ZipConfigRepository.getZipConfigOrNull(name);
        Assert.assertNotNull(found);
        Assert.assertEquals("getZipConfigOrNull_found", found.getZipName());
    }

    @Test
    public void saveWithException() throws IOException {
        final String name = "saveWithException.zip";

        final IFile fileUnwritable = Mockito.mock(IFile.class);
        Mockito.when(fileUnwritable.openOutputStream()).thenThrow(new FileNotFoundException("junit"));

        final ZipConfigRepository repository = Mockito.spy(createUnsavedZipConfigRepository(name));
        Mockito.when(repository.getZipConfigFile()).thenReturn(fileUnwritable);

        LibZipGlobal.debugEnabled = true;
        Assert.assertFalse(repository.save());
    }


    @Test
    public void loadWithError_null() throws IOException {
        final IFile fileUnwritable = Mockito.mock(IFile.class);
        Mockito.when(fileUnwritable.openInputStream()).thenThrow(new FileNotFoundException("junit"));

        LibZipGlobal.debugEnabled = true;
        Assert.assertNull(ZipConfigRepository.loadExisting(fileUnwritable));
    }

    @Test
    public void shouldFixIllegalName() {
        final String name = "saveWithIllegalName/../..\\../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../saveWithIllegalName/../illegal:file:test.ext";
        ZipConfigRepository repo = createUnsavedZipConfigRepository(name);
        Assert.assertEquals("saveWithIllegalName_saveWithIllegalName_illegal_file_test", repo.getZipName());
    }

    private ZipConfigRepository createUnsavedZipConfigRepository(String zipName) {
        IFile repositoryFile = OUTDIR.createIFile(zipName);
        repositoryFile.delete();
        ZipConfigDto dto = new ZipConfigDto(null);
        dto.setZipName(zipName);
        ZipConfigRepository repository = new ZipConfigRepository(dto);
        return repository;
    }
}
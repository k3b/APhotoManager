/*
 * Copyright (c) 2017-2020 by k3b.
 *
 * This file is part of AndroFotoFinder / #APhotoManager
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

package de.k3b.media;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.EnumSet;

import de.k3b.LibGlobal;
import de.k3b.TestUtil;
import de.k3b.io.FileUtils;
import de.k3b.io.filefacade.IFile;


/**
 * Created by k3b on 06.04.2017.
 */


public class PhotoPropertiesBulkUpdateServiceIntegratoinTests {
    // Obtain a logger instance
    private static final Logger LOGGER = LoggerFactory.getLogger(PhotoPropertiesBulkUpdateServiceIntegratoinTests.class);
    private static final String SUT_CLASS_NAME = PhotoPropertiesBulkUpdateServiceIntegratoinTests.class.getSimpleName();
    private static final IFile OUTDIR = TestUtil.OUTDIR_ROOT.createFile(SUT_CLASS_NAME);

    @BeforeClass
    public static void initDirectories() {
        LibGlobal.appName = "JUnit";
        LibGlobal.appVersion = SUT_CLASS_NAME;

        FileUtils.delete(OUTDIR, null);
        OUTDIR.mkdirs();


        ExifInterface.DEBUG = true;
        LibGlobal.debugEnabled = true;

        // so you can see that the file was modified
        LibGlobal.preserveJpgFileModificationDate = false;
    }

    private static IFile copy(String resourceName, String fileNameDest) throws IOException {
        final IFile sutFile = OUTDIR.createFile(fileNameDest);
        TestUtil.saveTestResourceAs(resourceName, sutFile);
        return sutFile;
    }

    @Test
    public void shouldUpdateExistingExifWithCreateXmp() throws IOException
    {
        LibGlobal.mediaUpdateStrategy = "JXC";
        String fileNameSrc = TestUtil.TEST_FILE_JPG_WITH_EXIF;
        String fileNameDest = "shouldUpdateExistingExifWithCreateXmp.jpg";

        IFile testJpg = copy(fileNameSrc, fileNameDest);

        PhotoPropertiesDTO testData = TestUtil.createTestMediaDTO(4);
        new PhotoPropertiesBulkUpdateService(null).applyChanges(testJpg,
                null, 0, false, new PhotoPropertiesDiffCopy(true, true).setDiff(testData, EnumSet.allOf(MediaFormatter.FieldID.class)));

        // LOGGER.info(sutRead.toString());

    }

}

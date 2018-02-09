package de.k3b.media;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;

import de.k3b.FotoLibGlobal;
import de.k3b.TestUtil;
import de.k3b.io.FileUtils;

/*
 * Copyright (c) 2017 by k3b.
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


/**
 * Created by k3b on 06.04.2017.
 */


public class JpgMetaWorkflowIntegratoinTests {
    // Obtain a logger instance
    private static final Logger LOGGER = LoggerFactory.getLogger(JpgMetaWorkflowIntegratoinTests.class);
    private static final String SUT_CLASS_NAME = JpgMetaWorkflowIntegratoinTests.class.getSimpleName();
    private static final File OUTDIR = new File(TestUtil.OUTDIR_ROOT, SUT_CLASS_NAME).getAbsoluteFile();

    @BeforeClass
    public static void initDirectories() {
        FotoLibGlobal.appName = "JUnit";
        FotoLibGlobal.appVersion = SUT_CLASS_NAME;

        FileUtils.delete(OUTDIR, null);
        OUTDIR.mkdirs();


        ExifInterface.DEBUG = true;
        FotoLibGlobal.debugEnabled = true;

        // so you can see that the file was modified
        FotoLibGlobal.preserveJpgFileModificationDate = false;
    }

    @Test
    public void shouldUpdateExistingExifWithCreateXmp() throws IOException
    {
        FotoLibGlobal.mediaUpdateStrategy = "JXC";
        String fileNameSrc = "test-WitExtraData.jpg";
        String fileNameDest = "shouldUpdateExistingExifWithCreateXmp.jpg";

        File testJpg = copy(fileNameSrc, fileNameDest);

        MediaDTO testData = TestUtil.createTestMediaDTO(4);
        new JpgMetaWorkflow(null).applyChanges(testJpg,
                null, 0, false, new MediaDiffCopy(true, true).setDiff(testData, EnumSet.allOf(MediaUtil.FieldID.class)));

        // LOGGER.info(sutRead.toString());

    }

    private static File copy(String resourceName, String fileNameDest) throws IOException {
        final File sutFile = new File(OUTDIR, fileNameDest);
        TestUtil.saveTestResourceAs(resourceName, sutFile);
        return sutFile;
    }

}

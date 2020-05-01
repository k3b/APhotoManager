/*
 * Copyright (c) 2017-2019 by k3b.
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

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import de.k3b.LibGlobal;
import de.k3b.TestUtil;
import de.k3b.io.FileUtils;
import de.k3b.io.IFile;
import de.k3b.io.VISIBILITY;
import de.k3b.io.XmpFile;

/**
 * Created by k3b on 24.04.2017.
 */

public class PhotoPropertiesUpdateHandlerIntegrationTests {
    private static final Logger logger = LoggerFactory.getLogger(PhotoPropertiesUpdateHandlerIntegrationTests.class);
    private static final IFile OUTDIR = TestUtil.OUTDIR_ROOT.create("PhotoPropertiesUpdateHandlerIntegrationTests");

    @BeforeClass
    public static void initDirectories() {
        LibGlobal.appName = "JUnit";
        LibGlobal.appVersion = "PhotoPropertiesUpdateHandlerIntegrationTests";

        OUTDIR.mkdirs();
    }

    @Test
    public void emptyWriteEmptyExifXmpCreate() throws IOException {
        ExifInterfaceEx.fixDateOnSave = false;

        IFile out = OUTDIR.create("emptyWriteEmptyExifXmpCreate.jpg");
        TestUtil.saveTestResourceAs(TestUtil.TEST_FILE_JPG_WITH_NO_EXIF, out);

        PhotoPropertiesUpdateHandler sut = PhotoPropertiesUpdateHandler.create(out, null, false, "JUnit"
                , true, true, true); //exif, xmp, create
        PhotoPropertiesDTO empty = new PhotoPropertiesDTO();
        empty.setVisibility(VISIBILITY.PUBLIC); // not complete empty since it is public visible

        PhotoPropertiesUtil.copy(sut, empty, true, true);

        // was overwritten by copy
        sut.setPath(out.getAbsolutePath());

        sut.save("JUnit");

        assertEqual(out, empty, empty, sut);
        // System.out.printf(sut.toString());
        // logger.info(sut.toString());

        ExifInterfaceEx.fixDateOnSave = true;
    }

    @Test
    public void existingWriteEmptyExifXmp() throws IOException {
        // workaround UserComment=null is not implemented
        ExifInterfaceEx.useUserComment = false;
        ExifInterfaceEx.fixDateOnSave = false;

        IFile out = OUTDIR.create("existingWriteEmptyExifXmp.jpg");
        TestUtil.saveTestResourceAs(TestUtil.TEST_FILE_JPG_WITH_EXIF, out);
        TestUtil.saveTestResourceAs(TestUtil.TEST_FILE_XMP_WITH_EXIF, XmpFile.getSidecar(out, false));

        PhotoPropertiesUpdateHandler sut = PhotoPropertiesUpdateHandler.create(out, null, false, "JUnit"
                , true, true, true); //exif, xmp, create
        PhotoPropertiesDTO empty = new PhotoPropertiesDTO();
        empty.setVisibility(VISIBILITY.PUBLIC); // not complete empty since it is public visible
        PhotoPropertiesUtil.copy(sut, empty, true, true);

//        System.out.printf("exif " + PhotoPropertiesUtil.toString(sut.getExif()));
        String xmpContent = "xmp " + PhotoPropertiesFormatter.format(sut.getXmp());
        System.out.printf(xmpContent);


        // was overwritten by copy
        sut.setPath(out.getAbsolutePath());

        sut.save("JUnit");
        assertEqual(out, empty, empty, sut);
        ExifInterfaceEx.useUserComment = true;
        ExifInterfaceEx.fixDateOnSave = true;
    }

    @Test
    public void existingWriteValueExifXmpCreate() throws IOException {
        IFile out = OUTDIR.create("existingWriteValueExifXmpCreate.jpg");
        TestUtil.saveTestResourceAs(TestUtil.TEST_FILE_JPG_WITH_EXIF, out);

        PhotoPropertiesUpdateHandler sut = PhotoPropertiesUpdateHandler.create(
                out, null,
                false, "JUnit",
                true, true, true); //exif, xmp, create
        PhotoPropertiesDTO value = createTestValue();
        PhotoPropertiesUtil.copy(sut, value, true, true);

//        System.out.printf("exif " + PhotoPropertiesUtil.toString(sut.getExif()));
        String xmpContent = "xmp " + PhotoPropertiesFormatter.format(sut.getXmp());
        System.out.printf(xmpContent);


        // was overwritten by copy
        sut.setPath(out.getAbsolutePath());

        sut.save("JUnit");
        assertEqual(out, value, value, sut);
    }

    private static PhotoPropertiesDTO createTestValue() {
        final PhotoPropertiesDTO testPhotoPropertiesDTO = TestUtil.createTestMediaDTO(2);
        testPhotoPropertiesDTO.setVisibility(VISIBILITY.PUBLIC);
        return testPhotoPropertiesDTO;
    }


    @Test
    public void emptyWriteValuesXmpCreate() throws IOException {
        IFile out = OUTDIR.create("emptyWriteValuesXmpCreate.jpg");
        TestUtil.saveTestResourceAs(TestUtil.TEST_FILE_JPG_WITH_NO_EXIF, out);

        PhotoPropertiesUpdateHandler sut = PhotoPropertiesUpdateHandler.create(out, null, false, "JUnit"
                , false, true, true); //exif, xmp, create
        PhotoPropertiesDTO values = createTestValue();
        PhotoPropertiesUtil.copy(sut, values, true, true);

        // was overwritten by copy
        sut.setPath(out.getAbsolutePath());

        sut.save("JUnit");

        assertEqual(out, null, values, sut);
        // System.out.printf(sut.toString());
        // logger.info(sut.toString());
    }

    @Test
    public void emptyWriteValuesExifOnly() throws IOException {
        IFile out = OUTDIR.create("emptyWriteValuesExifOnly.jpg");
        TestUtil.saveTestResourceAs(TestUtil.TEST_FILE_JPG_WITH_NO_EXIF, out);

        PhotoPropertiesUpdateHandler sut = PhotoPropertiesUpdateHandler.create(out, null, false, "JUnit"
                , true, false, false); //exif, xmp, create
        PhotoPropertiesDTO values = createTestValue();
        PhotoPropertiesUtil.copy(sut, values, true, true);

        // was overwritten by copy
        sut.setPath(out.getAbsolutePath());

        sut.save("JUnit");

        assertEqual(out, null, values, sut);
        // System.out.printf(sut.toString());
        // logger.info(sut.toString());
    }

    private void assertEqual(
            IFile file, PhotoPropertiesDTO expectedExif, PhotoPropertiesDTO expectedXmp,
            PhotoPropertiesUpdateHandler oldSut) throws IOException {
        PhotoPropertiesUpdateHandler sut = PhotoPropertiesUpdateHandler.create(file, null, false, "JUnit-check"
                , true, true, false); //exif, xmp, create

        if (oldSut != null) {
            Writer out = null;
            try {
                out = new FileWriter(new File(file.getAbsolutePath()+ ".log"), false);

                IPhotoProperties exif = oldSut.getExif();
                if (exif != null) {
                    out.write("old exif " + PhotoPropertiesFormatter.format(exif) + "\n");
                    out.write(exif.toString() + "\n");
                }

                exif = oldSut.getXmp();
                if (exif != null) {
                    out.write("old xmp " + PhotoPropertiesFormatter.format(exif) + "\n");
                    out.write(exif.toString() + "\n");
                }

                exif = sut.getExif();
                if (exif != null) {
                    out.write("new exif " + PhotoPropertiesFormatter.format(exif) + "\n");
                    out.write(exif.toString() + "\n");
                }

                exif = sut.getXmp();
                if (exif != null) {
                    out.write("new xmp " + PhotoPropertiesFormatter.format(exif) + "\n");
                    out.write(exif.toString() + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                FileUtils.close(out, "");
            }
        }

        if (expectedExif != null) Assert.assertEquals("exif "
                , getMediaString(expectedExif), getMediaString(sut.getExif()));
        if (expectedXmp != null) Assert.assertEquals("xmp ",
                getMediaString(expectedXmp), getMediaString(sut.getExif()));
    }

    private String getMediaString(IPhotoProperties media) {
        String result = PhotoPropertiesFormatter
                .format(media, false, null, MediaFormatter.FieldID.path, MediaFormatter.FieldID.clasz)
                .toString();
        // ignore runtime type and path
        return result; // .substring(result.indexOf("dateTimeTaken"));
    }
}


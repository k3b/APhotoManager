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

package de.k3b.media;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import de.k3b.TestUtil;
import de.k3b.io.FileUtils;

/**
 * Created by k3b on 24.04.2017.
 */

public class MetaWriterExifXmlIntegrationTests {
    private static final Logger logger = LoggerFactory.getLogger(MetaWriterExifXmlIntegrationTests.class);
    private static final File OUTDIR = new File(TestUtil.OUTDIR_ROOT, "MetaWriterExifXmlIntegrationTests");

    @BeforeClass
    public static void initDirectories() {
        OUTDIR.mkdirs();
    }

    @Before
    public void setup() throws IOException {
    }

    @Test
    public void emptyWriteEmptyExifXmpCreate() throws IOException
    {
        File out = new File(OUTDIR,"emptyWriteEmptyExifXmpCreate.jpg");
        TestUtil.saveTestResourceAs("NoExif.jpg", out);

        MetaWriterExifXml sut = MetaWriterExifXml.create(out.getAbsolutePath(),"JUnit"
                , true, true, true); //exif, xmp, create
        MediaDTO empty = new MediaDTO();
        MediaUtil.copy(sut, empty, true, true);

        // was overwritten by copy
        sut.setPath(out.getAbsolutePath());

        sut.save("JUnit");

        assertEqual(out, empty, empty, sut);
        // System.out.printf(sut.toString());
        // logger.info(sut.toString());
    }

    @Test
    public void existingWriteEmptyExifXmpCreate() throws IOException
    {
        // workaround UserComment=null is not implemented
        ExifInterfaceEx.useUserComment = false;

        File out = new File(OUTDIR,"existingWriteEmptyExifXmpCreate.jpg");
        TestUtil.saveTestResourceAs("test-WitExtraData.jpg", out);
        TestUtil.saveTestResourceAs("test-WitExtraData.xmp", FileUtils.getXmpFile(out.getAbsolutePath()));

        MetaWriterExifXml sut = MetaWriterExifXml.create(out.getAbsolutePath(),"JUnit"
                , true, true, true); //exif, xmp, create
        MediaDTO empty = new MediaDTO();
        MediaUtil.copy(sut, empty, true, true);

//        System.out.printf("exif " + MediaUtil.toString(sut.getExif()));
        String xmpContent = "xmp " + MediaUtil.toString(sut.getXmp());
        System.out.printf(xmpContent);


        // was overwritten by copy
        sut.setPath(out.getAbsolutePath());

        sut.save("JUnit");
        assertEqual(out, empty, empty, sut);
        ExifInterfaceEx.useUserComment = true;
    }

    private void assertEqual(File file, MediaDTO expectedExif, MediaDTO expectedXmp, MetaWriterExifXml oldSut) throws IOException {

        if (oldSut != null) {
            Writer out = null;
            try {
                out = new FileWriter(new File(file.getAbsolutePath()+ ".log"), false);
                IMetaApi exif = oldSut.getExif();
                if (exif != null) {
                    out.write("old exif " + MediaUtil.toString(exif) + "\n");
                    out.write(exif.toString() + "\n");
                }
                exif = oldSut.getXmp();
                if (exif != null) {
                    out.write("old xmp " + MediaUtil.toString(exif) + "\n");
                    out.write(exif.toString() + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                FileUtils.close(out, "");
            }
        }


        MetaWriterExifXml sut = MetaWriterExifXml.create(file.getAbsolutePath(),"JUnit-check"
                , true, true, true); //exif, xmp, create
        if (expectedExif != null) Assert.assertEquals("exif "
                , getMediaString(expectedExif), getMediaString(sut.getExif()));
        if (expectedXmp != null) Assert.assertEquals("xmp ",
                getMediaString(expectedXmp), getMediaString(sut.getExif()));
    }

    private String getMediaString(IMetaApi media) {
        String result = MediaUtil.toString(media);
        // ignore runtime type and path
        return result.substring(result.indexOf("dateTimeTaken"));
    }
}


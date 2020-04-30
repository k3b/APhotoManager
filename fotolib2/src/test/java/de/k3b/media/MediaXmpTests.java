/*
 * Copyright (c) 2016-2017 by k3b.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TimeZone;

import de.k3b.LibGlobal;
import de.k3b.TestUtil;
import de.k3b.io.IFile;

/**
 * Created by k3b on 24.10.2016.
 */

public class MediaXmpTests {
    // D:\prj\eve\android\prj\fotos-android.wrk\FotoGallery\FotoGallery\fotolib2\src\test\resources\testdata
    // test-WitExtraData.xmp
    private static final String RESOURCES_ROOT = "testdata/";
    private static final IFile OUTDIR = TestUtil.OUTDIR_ROOT.create("MediaXmpTests");

    @BeforeClass
    public static void initDirectories() {
        LibGlobal.appName = "JUnit";
        LibGlobal.appVersion = "MediaXmpTests";
    }

    @Test
    public void shouldReadExistingXmpFile() throws IOException {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        PhotoPropertiesXmpSegment sut = new PhotoPropertiesXmpSegment();
        InputStream fis = TestUtil.getResourceInputStream(TestUtil.TEST_FILE_XMP_WITH_EXIF);
        sut = new PhotoPropertiesXmpSegment();
        sut.load(fis, "JUnit");
        fis.close();

        PhotoPropertiesDTO actual = new PhotoPropertiesDTO(sut);

        Assert.assertEquals(sut.toString(), "PhotoPropertiesDTO: path null dateTimeTaken 1962-11-07T09:38:46 title Headline description XPSubject latitude_longitude 27.818611, -15.764444 rating 3 visibility null tags Marker1, Marker2", actual.toString());
    }

    private InputStream getStream(String _resourceName) {
        String currentResourceName = _resourceName;

        // this does not work with test-resources :-(
        // or i donot know how to do it with AndroidStudio-1.02/gradle-2.2
        InputStream result = this.getClass().getResourceAsStream(currentResourceName);

        if (result == null) {
            File prjRoot = new File(".").getAbsoluteFile();
            while (prjRoot.getName().compareToIgnoreCase("fotolib2") != 0) {
                prjRoot = prjRoot.getParentFile();
                if (prjRoot == null) return null;
            }

            // assuming this src folder structure:
            // .../LocationMapViewer/k3b-geoHelper/src/test/resources/....
            File resourceFile = new File(prjRoot, "build/resources/de/k3b/media/" + _resourceName);
// D:\prj\eve\android\prj\fotos-android.wrk\FotoGallery\FotoGallery\fotolib2\src\test\resources\de\k3b\media
            currentResourceName = resourceFile.getAbsolutePath(); // . new Path(resourceName).get;
            try {
                result = new FileInputStream(currentResourceName);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }
        return result;
    }

    @Test
    public void shouldCopyAllFields() {
        PhotoPropertiesXmpSegment sut = new PhotoPropertiesXmpSegment();
        PhotoPropertiesDTO expected = TestUtil.createTestMediaDTO(1);
        PhotoPropertiesUtil.copy(sut, expected, true, true);
        PhotoPropertiesDTO actual = new PhotoPropertiesDTO(sut);

        Assert.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void shouldSaveAndLoadXmp() throws IOException {
        PhotoPropertiesDTO content = TestUtil.createTestMediaDTO(1);
        content.setPath(null); // path is not copied to/from xmp file
        PhotoPropertiesXmpSegment sut = new PhotoPropertiesXmpSegment();
        PhotoPropertiesUtil.copy(sut, content, true, true);

        OUTDIR.mkdirs();
        IFile outFile = OUTDIR.create("shouldSaveAsXmp.xmp");
        OutputStream fos = outFile.openOutputStream();
        sut.save(fos, true, "JUnit");
        fos.close();

        InputStream fis = outFile.openInputStream();
        sut = new PhotoPropertiesXmpSegment();
        sut.load(fis, "JUnit");
        fis.close();

        PhotoPropertiesDTO actual = new PhotoPropertiesDTO(sut);

        Assert.assertEquals(content.toString(), actual.toString());
    }

}

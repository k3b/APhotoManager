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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.TimeZone;

import de.k3b.FotoLibGlobal;
import de.k3b.TestUtil;

/**
 * Created by k3b on 24.10.2016.
 */

public class MediaXmpTests {
    // D:\prj\eve\android\prj\fotos-android.wrk\FotoGallery\FotoGallery\fotolib2\src\test\resources\testdata
    // test-WitExtraData.xmp
    private static final String RESOURCES_ROOT = "testdata/";
    private static final File OUTDIR = new File(TestUtil.OUTDIR_ROOT, "MediaXmpTests");

    @BeforeClass
    public static void initDirectories() {
        FotoLibGlobal.appName = "JUnit";
        FotoLibGlobal.appVersion = "MediaXmpTests";
    }

    @Test
    public void shouldReadExistingXmpFile() throws IOException {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        MediaXmpSegment sut = new MediaXmpSegment();
        InputStream fis = getStream("images/test-WitExtraData.xmp");
        sut = new MediaXmpSegment();
        sut.load(fis, "JUnit");
        fis.close();

        MediaDTO actual = new MediaDTO(sut);

        Assert.assertEquals(sut.toString(), "MediaDTO: path null dateTimeTaken 1962-11-07T09:38:46 title Headline description XPSubject latitude_longitude 27.818611, -15.764444 rating 3 visibility null tags Marker1, Marker2", actual.toString());
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
        MediaXmpSegment sut = new MediaXmpSegment();
        MediaDTO expected = TestUtil.createTestMediaDTO(1);
        MediaUtil.copy(sut, expected, true, true);
        MediaDTO actual = new MediaDTO(sut);

        Assert.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void shouldSaveAndLoadXmp() throws IOException {
        MediaDTO content = TestUtil.createTestMediaDTO(1);
        content.setPath(null); // path is not copied to/from xmp file
        MediaXmpSegment sut = new MediaXmpSegment();
        MediaUtil.copy(sut, content, true, true);

        OUTDIR.mkdirs();
        File outFile = new File(OUTDIR, "shouldSaveAsXmp.xmp");
        FileOutputStream fos = new FileOutputStream(outFile);
        sut.save(fos, true, "JUnit");
        fos.close();

        FileInputStream fis = new FileInputStream(outFile);
        sut = new MediaXmpSegment();
        sut.load(fis, "JUnit");
        fis.close();

        MediaDTO actual = new MediaDTO(sut);

        Assert.assertEquals(content.toString(), actual.toString());
    }

}

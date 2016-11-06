/*
 * Copyright (c) 2016 by k3b.
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

package de.k3b.media;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import de.k3b.csv2db.csv.TestUtil;

/**
 * Created by k3b on 24.10.2016.
 */

public class MediaXmpTests {
    @Test
    public void shouldCopyAllFields() {
        MediaXmpSegment sut = new MediaXmpSegment();
        MediaDTO expected = TestUtil.createTestMediaDTO(1);
        MediaUtil.copy(sut, expected, true, true);
        MediaDTO actual = new MediaDTO(sut);

        Assert.assertEquals(expected.toString(), actual.toString());
    }

    private static final File OUTDIR = new File("./build/testresults/MediaXmpTests");

    @Test
    public void shouldSaveAndLoadXmp() throws IOException {
        MediaDTO content = TestUtil.createTestMediaDTO(1);
        content.setPath(null); // path is not copied to/from xmp file
        MediaXmpSegment sut = new MediaXmpSegment();
        MediaUtil.copy(sut, content, true, true);

        OUTDIR.mkdirs();
        File outFile = new File(OUTDIR, "shouldSaveAsXmp.xmp");
        FileOutputStream fos = new FileOutputStream(outFile);
        sut.save(fos, true);
        fos.close();

        FileInputStream fis = new FileInputStream(outFile);
        sut = new MediaXmpSegment();
        sut.load(fis);
        fis.close();

        MediaDTO actual = new MediaDTO(sut);

        Assert.assertEquals(content.toString(), actual.toString());
    }

}

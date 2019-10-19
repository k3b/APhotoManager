/*
 * Copyright (c) 2018-2019 by k3b.
 *
 * This file is part of #APhotoManager (https://github.com/k3b/APhotoManager/)
 *              and #toGoZip (https://github.com/k3b/ToGoZip/).
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *
 * for more details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>
 */
package de.k3b.io;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/**
 * Created by k3b on 17.02.2015.
 */
public class FileNameUtilTests {
    @Test
    public void shouldGenerateWithDefaultExtension() {
        // "content://com.mediatek.calendarimporter/1282"
        String result = FileNameUtil.createFileName("1282", "vcs");
        Assert.assertEquals("1282.vcs", result);
    }

    @Test
    public void shouldRemoveIllegalWithExistingExtension() {
        String result = FileNameUtil.createFileName("...hello:world.jpeg...", "jpg");
        Assert.assertEquals("hello_world.jpeg", result);
    }

    /**
     * Encapsulates call to sut calculateZipEntryName to Calculates the path within the zip file.
     *
     * @param zipRelPath if not empty paths are caclulated relative to this directory. Mus have trailing "/".
     * @param srcFile    full path to source file
     * @return
     */
    private static String sutExec_makePathRelative(String zipRelPath, File srcFile) {
        String result = FileNameUtil.makePathRelative(
                FileNameUtil.getCanonicalPath(new File(zipRelPath)).toLowerCase(), srcFile);

        // fix windows path seperator
        return fixPathDelimiter(result);
    }

    private static String fixPathDelimiter(String result) {
        if (result == null) return null;
        return result.replaceAll("\\\\", "/");
    }

    @Test
    public void shouldCalculateRelPath() {
        File srcFile = new File("/path/to/my/source/File.txt");

        Assert.assertEquals("happy case1 with trailing '/'", "source/File.txt",
                sutExec_makePathRelative("/path/to/my/", srcFile));
        Assert.assertEquals("happy case2 without trailing '/'", "source/File.txt",
                sutExec_makePathRelative("/path/to/my", srcFile));
        Assert.assertEquals("case doesn-t matter", "source/File.txt",
                sutExec_makePathRelative("/path/To/my/", srcFile));
        Assert.assertEquals("outside rel path", null,
                sutExec_makePathRelative("/path/to/other/", srcFile));
    }

}
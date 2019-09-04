/*
 * Copyright (c) 2018-2019 by k3b.
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
package de.k3b.io;

import org.junit.Assert;
import org.junit.Test;

public class AlbumFileTest {

    @Test
    public void shouldIdenitfyQueryFile() {
        assertIsQuery(true, "/path/to/myfile.album");
        assertIsQuery(true, "/path/to/myfile.album/%");

        assertIsQuery(false, "/path/to/myfile.album/dir");
        assertIsQuery(false, "/path/to/album");
    }

    @Test
    public void shouldFixFile() {
        Assert.assertEquals("/path/to/myfile.album", AlbumFile.fixPath("/path/to/myfile.album/%"));
        Assert.assertEquals("/path/to/myfile.album", AlbumFile.fixPath("/path/to/myfile.album"));
    }


    private void assertIsQuery(boolean expected, String uri) {
        Assert.assertEquals(uri, expected, AlbumFile.isQueryFile(uri));
    }
}

/*
 * Copyright (c) 2018 by k3b.
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

}
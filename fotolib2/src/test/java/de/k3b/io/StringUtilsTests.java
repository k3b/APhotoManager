/*
 * Copyright (c) 2017 by k3b.
 *
 * This file is part of AndroFotoFinder / #APhotoManager.
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

/**
 * Created by k3b on 29.05.2017.
 */

public class StringUtilsTests {
    @Test
    public void shoudCompare() {
        Assert.assertEquals(0, StringUtils.compare(null, null));
        Assert.assertEquals(-1, StringUtils.compare(null, ""));
        Assert.assertEquals(+1, StringUtils.compare("", null));
        Assert.assertEquals(-1, StringUtils.compare("a", "b"));
    }
}

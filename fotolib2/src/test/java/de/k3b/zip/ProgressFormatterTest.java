/*
 * Copyright (c) 2019 by k3b.
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
package de.k3b.zip;

import org.junit.Assert;
import org.junit.Test;

public class ProgressFormatterTest {

    @Test
    public void format() {
        long t3_15 = 3 * 60 + 15;

        Assert.assertEquals("1/60  00:03:15/03:15:00", ProgressFormatter.format(t3_15, 1, 60).toString());
        Assert.assertEquals("60/60  00:03:15/00:03:15", ProgressFormatter.format(t3_15, 60, 60).toString());
    }
}
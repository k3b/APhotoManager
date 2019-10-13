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

        int durationLoopingStart = 0;
        int countLoopingStart = 0;

        int first = 1;
        int last = 60;

        Assert.assertEquals("a", "1/60  00:03:15/03:15:00", ProgressFormatter.format(t3_15,
                first + countLoopingStart, last + countLoopingStart, durationLoopingStart, countLoopingStart).toString());

        Assert.assertEquals("b", "60/60  00:03:15/00:03:15", ProgressFormatter.format(t3_15,
                last + countLoopingStart, last + countLoopingStart, durationLoopingStart, countLoopingStart).toString());

        durationLoopingStart = 1;
        countLoopingStart = 100;

        Assert.assertEquals("c", "101/160  00:03:16/03:15:01", ProgressFormatter.format(t3_15,
                first + countLoopingStart, last + countLoopingStart, durationLoopingStart, countLoopingStart).toString());
        Assert.assertEquals("d", "160/160  00:03:16/00:03:16", ProgressFormatter.format(t3_15,
                last + countLoopingStart, last + countLoopingStart, durationLoopingStart, countLoopingStart).toString());

        durationLoopingStart = 3600;
        countLoopingStart = 100;

        Assert.assertEquals("e", "101/160  01:03:15/04:15:00", ProgressFormatter.format(t3_15,
                first + countLoopingStart, last + countLoopingStart, durationLoopingStart, countLoopingStart).toString());
        Assert.assertEquals("f", "160/160  01:03:15/01:03:15", ProgressFormatter.format(t3_15,
                last + countLoopingStart, last + countLoopingStart, durationLoopingStart, countLoopingStart).toString());

    }
}
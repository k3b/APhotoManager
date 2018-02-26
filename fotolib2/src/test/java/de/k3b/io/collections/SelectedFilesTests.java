/*
 * Copyright (c) 2016-2018 by k3b.
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
package de.k3b.io.collections;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by k3b on 06.10.2016.
 */

public class SelectedFilesTests {
    @Test
    public void shoudParseAndFormatString() {
        String names = "'b','z','c'";
        String ids = "71,2,3";
        String selectedDates = "2223372036854775807,1223372036854775807,3223372036854775807";
        SelectedFiles sut = SelectedFiles.create(names, ids, selectedDates);
        Assert.assertEquals("names", names, sut.toString());
        Assert.assertEquals("ids", ids, sut.toIdString());
        Assert.assertEquals("dates", selectedDates, sut.toDateString());
    }
}

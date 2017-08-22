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

/**
 * Created by k3b on 27.02.2017.
 */

import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

public class DateUtilsTests {
    /** my oldest photos are from 1954. make shure that this date is comuted correctly */
    @Test
    public void shoudParseFormatOldDate() {
        String dateString = "1954-12-14T19:32:56";
        Date sut = DateUtil.parseIsoDate(dateString);
        String sutAsString = DateUtil.toIsoDateTimeString(sut);
        Assert.assertEquals(dateString, sutAsString);
    }
}

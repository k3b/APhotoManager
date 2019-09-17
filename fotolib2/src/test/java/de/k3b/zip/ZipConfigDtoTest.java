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

import java.util.Date;

import de.k3b.io.DateUtil;

public class ZipConfigDtoTest {

    @Test
    public void getZipFileName() {
        Date backupStartDate = DateUtil.parseIsoDate("2017-12-06T23:59:59");
        Date backupEndDate = DateUtil.parseIsoDate("2019-12-24T15:32:59");
        ZipConfigDto config = new ZipConfigDto(null);
        config.setZipName("myResultFile");
        config.setZipDir("/path/to/");
        config.setDateModifiedFrom(backupStartDate);

        String result = ZipConfigDto.getZipFileName(config, backupEndDate);
        Assert.assertEquals("myResultFile.20171206-235959.20191224-153259.zip", result);
    }
}
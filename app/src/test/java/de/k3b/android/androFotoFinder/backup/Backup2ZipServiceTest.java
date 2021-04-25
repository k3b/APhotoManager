/*
 * Copyright (c) 2021 by k3b.
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
package de.k3b.android.androFotoFinder.backup;

import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.database.QueryParameter;
import de.k3b.io.DateUtil;
import de.k3b.zip.ZipConfigDto;

public class Backup2ZipServiceTest {
    @Test
    public void getEffectiveQueryParameter() {
        String expected = " SELECT _id, _data, datetaken, title, description, tags, latitude, longitude, bookmark, media_type  " +
                "WHERE (_data like ?) AND (date_modified >= ?)  \tPARAMETERS /path/to/dir%, 1512604799  ORDER BY _data";

        QueryParameter result = createEffectiveQueryParameter("/path/to/dir");

        Assert.assertEquals(expected, result.toString());
    }

    @Test
    public void getAlbumFilter_absolutePath() {
        String expected = " SELECT _id, _data, datetaken, title, description, tags, latitude, longitude, bookmark, media_type  " +
                "WHERE (_data like ?) AND ((_data like '%.album' OR _data like '%.query'))  \tPARAMETERS /path/to/dir%  ORDER BY _data";

        QueryParameter result = Backup2ZipService.getAlbumFilter(createEffectiveQueryParameter("/path/to/dir"));

        Assert.assertEquals(expected, result.toString());
    }

    @Test
    public void getAlbumFilter_pathContains() {
        String expected = " SELECT _id, _data, datetaken, title, description, tags, latitude, longitude, bookmark, media_type  " +
                "WHERE ((_data like '%.album' OR _data like '%.query'))";

        QueryParameter result = Backup2ZipService.getAlbumFilter(createEffectiveQueryParameter("%someExpression%"));

        Assert.assertEquals(expected, result.toString());
    }

    private QueryParameter createEffectiveQueryParameter(String path) {
        QueryParameter filter = FotoSql.addPathWhere(new QueryParameter(), path, 0);
        Date backupStartDate = DateUtil.parseIsoDate("2017-12-06T23:59:59");
        ZipConfigDto config = new ZipConfigDto(null);
        config.setDateModifiedFrom(backupStartDate);
        config.setFilter(filter.toReParseableString());

        return Backup2ZipService.getEffectiveQueryParameter(config);
    }
}
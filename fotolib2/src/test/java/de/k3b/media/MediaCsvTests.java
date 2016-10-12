/*
 * Copyright (c) 2016 by k3b.
 *
 * This file is part of AndroFotoFinder.
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

package de.k3b.media;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import de.k3b.csv2db.csv.CsvLoader;
import de.k3b.csv2db.csv.CsvReader;
import de.k3b.csv2db.csv.TestUtil;

/**
 * Created by k3b on 11.10.2016.
 */

public class MediaCsvTests {

    private static final String CSV_FIELD_DELIMITERC = ";";

    public final static String header = XmpFieldDefinition.PATH.getShortName() + CSV_FIELD_DELIMITERC +
            XmpFieldDefinition.TITLE.getShortName() + CSV_FIELD_DELIMITERC +
            XmpFieldDefinition.DESCRIPTION.getShortName() + CSV_FIELD_DELIMITERC +
            XmpFieldDefinition.DateTimeOriginal.getShortName() + CSV_FIELD_DELIMITERC +
            XmpFieldDefinition.GPSLatitude.getShortName() + CSV_FIELD_DELIMITERC +
            XmpFieldDefinition.GPSLongitude.getShortName() + CSV_FIELD_DELIMITERC +
            XmpFieldDefinition.TAGS.getShortName() + CSV_FIELD_DELIMITERC +
            CsvReader.CHAR_LINE_DELIMITER;

    public static String createTestCsv(int... ids) {
        StringBuilder result = new StringBuilder();
        result.append(header);

        for (int id : ids) {
            result.append("Path").append(id).append(CSV_FIELD_DELIMITERC);
            result.append("Title").append(id).append(CSV_FIELD_DELIMITERC);
            result.append("Description").append(id).append(CSV_FIELD_DELIMITERC);
            String month = ("" + (((id - 1) % 12) + 101)).substring(1);
            String day = ("" + (((id - 1) % 30) + 101)).substring(1);
            result.append(2000 + id).append("-").append(month).append("-").append(day).append(CSV_FIELD_DELIMITERC);
            result.append(50 + id + (0.01 * id)).append(CSV_FIELD_DELIMITERC);
            result.append(10 + id + (0.01 * id)).append(CSV_FIELD_DELIMITERC);
            result.append("tag").append(id).append(CsvReader.CHAR_LINE_DELIMITER);
        }
        return result.toString();
    }

    static class Sut extends CsvLoader<MediaCsvItem> {
        private ArrayList<IMetaApi> result = new ArrayList<IMetaApi>();

        @Override
        protected void onNextItem(MediaCsvItem next) {
            if (next != null) {
                result.add(new MediaDTO(next));
            }
        }

        List<IMetaApi>  load(int... ids) {
            result.clear();
            String data = createTestCsv(ids);
            super.load(TestUtil.createReader(data),new MediaCsvItem());
            return result;
        }
    }

    @Test
    public void shouldLoad1() {
        Sut sut = new Sut();
        List<IMetaApi> actual = sut.load(1);
        MediaDTO expected = TestUtil.createTestMediaDTO(1);
        Assert.assertEquals(expected.toString(), actual.get(0).toString());
    }

}

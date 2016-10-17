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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import de.k3b.csv2db.csv.CsvItem;
import de.k3b.csv2db.csv.CsvLoader;
import de.k3b.csv2db.csv.TestUtil;

/**
 * Created by k3b on 11.10.2016.
 */

public class MediaCsvTests {

    public static String createTestCsv(int... ids) {
        StringWriter result = new StringWriter();
        MediaCsvSaver saver = new MediaCsvSaver(new PrintWriter(result));
        for (int id : ids) {
            MediaDTO item = TestUtil.createTestMediaDTO(id);
            saver.save(item);
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
            String data = createTestCsv(ids);
            return load(data);
        }
		
        List<IMetaApi>  load(String data) {
            result.clear();
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

    @Test
    public void shouldLoadExtremas() {
		String csv = "a;" + XmpFieldDefinition.TITLE.getShortName() + ";c\n" 
				+ "normal;#1;regular\n"
				+ "short;#2\n"
				+ "long;#3;something;extra column\n"
                + "empty\n"
				+ "quoted;\"#5\";regular\n";
        Sut sut = new Sut();
        List<IMetaApi> actual = sut.load(csv);
        Assert.assertEquals("#", 5, actual.size());
        Assert.assertEquals("unquote", "#5", actual.get(4).getTitle());
    }

}

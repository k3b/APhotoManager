/*
 * Copyright (c) 2015 by k3b.
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
 
package de.k3b.database;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by k3b on 25.06.2015.
 */
public class QueryParameterTests {
    @Test
    public void shoudAllParts() {
        QueryParameter sut = new QueryParameter().addColumn("c").addFrom("f").addWhere("w=w").addOrderBy("o").addGroupBy("g").addHaving("h");
        Assert.assertEquals("select c from f where (w=w) group by g having (h) order by o",
                normalize(sut.toSqlString()));
    }

    @Test
    public void shoudAndroidWhereGroupOrder() {
        QueryParameter sut = new QueryParameter().addColumn("c").addFrom("f").addWhere("w=w").addOrderBy("o").addGroupBy("g");
        Assert.assertEquals("select c from f where ((w=w)) group by (g) order by o", normalize(sut.toSqlStringAndroid()));
    }

    @Test
    public void shoudAndroidWhere2Group2Order() {
        QueryParameter sut = new QueryParameter().addColumn("c").addFrom("f")
                .addWhere("w1").addWhere("w2")
                .addOrderBy("o")
                .addGroupBy("g1").addGroupBy("g2");
        Assert.assertEquals("select c from f where ((w1) and (w2)) group by (g1), (g2) order by o", normalize(sut.toSqlStringAndroid()));
    }

    @Test
    public void shoudCreateSerializable() {
        QueryParameter sut = new QueryParameter()
                .addFrom("f")
                .setID(4711)
                .addColumn("c1", "c2")
                .addWhere("w1=?", "w1Value")
                .addWhere("w2=?","w2Value")
                .addOrderBy("o")
                .addGroupBy("g")
                .addHaving("h1", "h1Value")
                .addHaving("h2", "h2Value");
        Assert.assertEquals("from f query-type-id 4711 select c1 c2 where w1=? w2=? where-parameters w1value w2value group-by g having h1 h2 having-parameters h1value h2value order-by o",
                normalize(sut.toReParseableString()));
    }

    @Test
    public void shoudParseSerializable() {
        QueryParameter original = new QueryParameter()
                .addFrom("f")
                .setID(4711)
                .addColumn("c1", "c2")
                .addWhere("w1=?", "w1Value")
                .addWhere("w2=?","w2Value")
                .addOrderBy("o")
                .addGroupBy("g")
                .addHaving("h1", "h1Value")
                .addHaving("h2", "h2Value");

        final String stringToBeParsed = original.toReParseableString();
        List<QueryParameter> sut = QueryParameter.parseMultiple(stringToBeParsed);
        Assert.assertEquals("size", 1, sut.size());
        Assert.assertEquals("from f query-type-id 4711 select c1 c2 where w1=? w2=? where-parameters w1value w2value group-by g having h1 h2 having-parameters h1value h2value order-by o",
                normalize(sut.get(0).toReParseableString()));
    }

    @Test
    public void shoudParseNoDefaults() {
        QueryParameter original = new QueryParameter()
                .addWhere("w1=?", "w1Value");

        final String stringToBeParsed = original.toReParseableString();
        List<QueryParameter> sut = QueryParameter.parseMultiple(stringToBeParsed);
        Assert.assertEquals("size", 1, sut.size());
        Assert.assertEquals("where w1=? where-parameters w1value",
                normalize(sut.get(0).toReParseableString()));
    }

    @Test
    public void shoudParseWithDefaults() {
        try {
            QueryParameter.sParserDefaultFrom = "f";
            QueryParameter.sParserDefaultQueryTypeId = 4711;
            QueryParameter.sParserDefaultSelect = new ArrayList<String>();
            QueryParameter.sParserDefaultSelect.add("c1");
            QueryParameter.sParserDefaultSelect.add("c2");


            QueryParameter original = new QueryParameter()
                    .addWhere("w1=?", "w1Value");

            final String stringToBeParsed = original.toReParseableString();
            List<QueryParameter> sut = QueryParameter.parseMultiple(stringToBeParsed);
            Assert.assertEquals("size", 1, sut.size());
            Assert.assertEquals("from f query-type-id 4711 select c1 c2 where w1=? where-parameters w1value",
                    normalize(sut.get(0).toReParseableString()));
        } finally {
            QueryParameter.sParserDefaultFrom = null;
            QueryParameter.sParserDefaultQueryTypeId = 0;
            QueryParameter.sParserDefaultSelect = null;
        }
    }

    @Test
    public void shoudParseExprParameter() {
        QueryParameter sut = new QueryParameter()
                .addWhere("w0")
                .addWhere("w1=?", "w1Value")
                .addWhere("w2 between ? and ?", "w21Value", "w22Value");

        Assert.assertNull(sut.getWhereParameter("doesNotExist", false));
        Assert.assertEquals(new String[0], sut.getWhereParameter("w0", false));
        Assert.assertEquals("w1Value", sut.getWhereParameter("w1=?", false)[0]);
        Assert.assertEquals("w22Value", sut.getWhereParameter("w2 between ? and ?", false)[1]);
    }

    @Test
    public void shoudRemoveParameter() {
        QueryParameter sut = new QueryParameter()
                .addWhere("w0")
                .addWhere("w1=?", "w1Value")
                .addWhere("w2 between ? and ?", "w21Value", "w22Value");

        // remove param
        sut.removeWhere("w1=?");
        Assert.assertEquals("where (w0) and (w2 between ? and ?) parameters w21value, w22value",
                normalize(sut.toSqlString()));
    }


    private String normalize(String unnormalized) {
        return unnormalized
                .replace("\t", " ").replace("\n", " ").replace("  ", " ").replace("  ", " ").toLowerCase().trim();
    }
}

/*
 * Copyright (c) 2015-2020 by k3b.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.k3b.TestUtil;
import de.k3b.io.filefacade.IFile;

/**
 * Created by k3b on 25.06.2015.
 */
public class QueryParameterTests {
    private static final IFile OUTDIR = TestUtil.OUTDIR_ROOT.createIFile("QueryParameterTests");
    private static final String sqlResultQueryCFWGHO =
            "select c1, c2 from f where (w1=?) and (w2=?) parameters w1param, w2param group by g1, gg2 having (h1) and (h2) parameters h1param, h2param order by o1, o2";

    private String normalize(String unnormalized) {
        return unnormalized
                .replace("\t", " ").replace("\n", " ").replace("  ", " ").replace("  ", " ").toLowerCase().trim();
    }

    protected QueryParameter createTestQueryCFWGHO() {
        return new QueryParameter()
                .addColumn("c1", "c2")
                .setID(4711)
                .addFrom("f")
                .addWhere("w1=?", "w1Param")
                .addWhere("w2=?", "w2Param")
                .addOrderBy("o1", "o2")
                .addGroupBy("g1", "gg2")
                .addHaving("h1", "h1Param")
                .addHaving("h2", "h2Param");
    }

    @Test
    public void shoudCopy() {
        QueryParameter sut = new QueryParameter(createTestQueryCFWGHO());
        Assert.assertEquals(sqlResultQueryCFWGHO,
                normalize(sut.toSqlString()));
    }

    @Test
    public void shoudAllParts() {
        QueryParameter sut = createTestQueryCFWGHO();
        Assert.assertEquals(sqlResultQueryCFWGHO,
                normalize(sut.toSqlString()));
    }

    @Test
    public void shoudWhereGroupHaving() {
        QueryParameter sut = createTestQueryCFWGHO().replaceOrderBy("o2");
        String result = "where " + sut.toWhere() + " group by " + sut.toGroupBy() + " having "
                + sut.toHaving() + " order by " + sut.toOrderBy();
        Assert.assertEquals("where w1=? and w2=? group by g1, gg2 having h1, h2 order by o2",
                normalize(result));
    }

    @Test
    public void shouldRemoveWhereParameters() {
        QueryParameter sut = new QueryParameter();
        sut.addWhere("c3 is not null");
        sut.addWhere("c2 between ? and ?", "p21", "p22");
        sut.addWhere("c1 = ?", "p1");
        sut.getWhereParameter("c2 between ? and ?", true);
        Assert.assertEquals("where (c3 is not null) and (c1 = ?) parameters p1", normalize(sut.toSqlString()));
    }

    @Test
    public void shouldGetWhereOrderByFrom() {
        QueryParameter original = createTestQueryCFWGHO();

        QueryParameter sut = new QueryParameter()
                .getWhereFrom(original, false)
                .getOrderByFrom(original, false);
        Assert.assertEquals("where (w1=?) and (w2=?) parameters w1param, w2param order by o1, o2",
                normalize(sut.toSqlString()));
    }

    @Test
    public void shouldRemoveColumnParameters2() {
        QueryParameter sut = new QueryParameter();
        sut.addColumn("c1", "count(c2)", "c3");
        sut.removeFirstColumnThatContains("c2");
        Assert.assertEquals("select c1, c3", normalize(sut.toString()));
    }

    @Test
    public void shoudSaveLoad() throws IOException {
        OUTDIR.mkdirs();
        IFile f = OUTDIR.createIFile("shoudSaveLoad");
        f.delete();
        QueryParameter original = new QueryParameter(createTestQueryCFWGHO());
        original.save(f.openOutputStream());

        QueryParameter sut = QueryParameter.load(f.openInputStream());

        Assert.assertEquals(sqlResultQueryCFWGHO,
                normalize(sut.toSqlString()));
    }

    @Test
    public void shoudClear() {
        QueryParameter sut = createTestQueryCFWGHO().clear();
        Assert.assertEquals(null, sut.toSqlString());
    }

    @Test
    public void shoudAndroidSql() {
        QueryParameter sut = createTestQueryCFWGHO();
        Assert.assertEquals(
                "select c1, c2 from f where ((w1=?) and (w2=?)) " +
                        "group by (g1), (gg2) " +
                        "having (h1), (h2) " +
                        "order by o1, o2 " +
                        "parameters w1param, w2param, h1param, h2param",
                normalize(sut.toSqlStringAndroid()));
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


}

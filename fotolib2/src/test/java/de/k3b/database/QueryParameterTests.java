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

    private String normalize(String unnormalized) {
        return unnormalized
                .replace("\n", "").replace("  ", " ").toLowerCase().trim();
    }
}

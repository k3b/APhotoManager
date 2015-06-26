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
        Assert.assertEquals("select c from f where ((w1) and (w2)) group by (g1, g2) order by o", normalize(sut.toSqlStringAndroid()));
    }

    private String normalize(String unnormalized) {
        return unnormalized
                .replace("\n", "").replace("  ", " ").toLowerCase().trim();
    }
}

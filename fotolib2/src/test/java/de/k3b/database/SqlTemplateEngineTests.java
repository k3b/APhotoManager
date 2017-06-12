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

package de.k3b.database;

import org.junit.Assert;
import org.junit.Test;

/**
 * Translates jsp like templates to sql concats.
 * Created by k3b on 12.06.2017.
 */

public class SqlTemplateEngineTests {
    @Test
    public void shoudExpandConstNoMakros() {

        String input = "SQL_COL_PK";
        String expected = "SQL_COL_PK";
        String actual = SqlTemplateEngine.toSql(input);

        Assert.assertEquals(input, expected, actual);
    }
    @Test
    public void shoudExpandExpression() {

        String input = "${SQL_COL_PK}";
        String expected = "ifnull(SQL_COL_PK,'')";
        String actual = SqlTemplateEngine.toSql(input);

        Assert.assertEquals(input, expected, actual);
    }
    @Test
    public void shoudExpandExpressionWithLinefeeds() {

        String input = "${SQL_COL_PK||}";
        String expected = "ifnull(SQL_COL_PK||'||','')";
        String actual = SqlTemplateEngine.toSql(input);

        Assert.assertEquals(input, expected, actual);
    }
    @Test
    public void shoudExpandConst_Expression() {

        String input = "#${SQL_COL_PK}";
        String expected = "'#'||ifnull(SQL_COL_PK,'')";
        String actual = SqlTemplateEngine.toSql(input);

        Assert.assertEquals(input, expected, actual);
    }
    @Test
    public void shoudExpandExpression_Const() {

        String input = "${SQL_COL_PK}#";
        String expected = "ifnull(SQL_COL_PK,'')||'#'";
        String actual = SqlTemplateEngine.toSql(input);

        Assert.assertEquals(input, expected, actual);
    }
    @Test
    public void shoudExpandHtml_Expression() {

        String input = "<font color='red'>${SQL_COL_PK}</font>";
        String expected = "'<font color=\"red\">'||ifnull(SQL_COL_PK,'')||'</font>'";
        String actual = SqlTemplateEngine.toSql(input);

        Assert.assertEquals(input, expected, actual);
    }

}

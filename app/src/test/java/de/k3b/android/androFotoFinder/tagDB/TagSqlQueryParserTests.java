/*
 * Copyright (c) 2017-2018 by k3b.
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

package de.k3b.android.androFotoFinder.tagDB;

import org.junit.Test;

import de.k3b.database.QueryParameter;
import de.k3b.io.GalleryFilterParameter;
import de.k3b.io.VISIBILITY;

import static org.junit.Assert.*;

/**
 * TagSql unittests with dependencies to android (android database field names),
 * which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class TagSqlQueryParserTests {
    @Test
    public void shouldParseFull() throws Exception {
        String FILTER_STRING = "1.23,2.34;3.45,4.56;2001-02-03,2005-12-31;/some/path/;q,^;%filter%;tag1,tag2,tag3;utag1,utag2,utag3;notags;3;4";
        assertFilterQueryFilter(FILTER_STRING);
    }

    @Test
    public void shouldEmpty() throws Exception {
        String FILTER_STRING = "";
        assertFilterQueryFilter(FILTER_STRING);
    }

    @Test
    public void shouldParseNoGeo() throws Exception {
        String FILTER_STRING = "noGeoInfo";
        assertFilterQueryFilter(FILTER_STRING);
    }

    @Test
    public void shouldParsePrivate() throws Exception {
        assertFilterQueryFilter(VISIBILITY.PRIVATE);
    }

    @Test
    public void shouldParsePrivatePublic() throws Exception {
        assertFilterQueryFilter(VISIBILITY.PRIVATE_PUBLIC);
    }

    @Test
    public void shouldParsePublic() throws Exception {
        assertFilterQueryFilter(VISIBILITY.PUBLIC);
    }

    // assert that input-string==output-string in  input-string -> filter -> query -> filter -> output-string
    private QueryParameter assertFilterQueryFilter(VISIBILITY visibility) {
        String FILTER_STRING = ";;;;;;;;;" + visibility.value;
        return assertFilterQueryFilter(FILTER_STRING);
    }

    // assert that input-string==output-string in  input-string -> filter -> query -> filter -> output-string
    private QueryParameter assertFilterQueryFilter(String filterString) {
        return assertFilterQueryFilter(filterString, null);
    }

    // assert that input-string==output-string in  input-string -> filter -> query -> filter -> output-string
    private QueryParameter assertFilterQueryFilter(String filterString, String printSql) {
        GalleryFilterParameter initialFilter = GalleryFilterParameter.parse(filterString, new GalleryFilterParameter());

        QueryParameter query = new QueryParameter();
        TagSql.filter2QueryEx(query, initialFilter, true);

        if (printSql != null) {
            String sql = query.toSqlString();
            int start = sql.indexOf("WHERE");
            System.out.println(printSql + ": " + sql.substring(start));
        }

        GalleryFilterParameter parsedFilter = (GalleryFilterParameter) TagSql.parseQueryEx(query, true);
        parsedFilter.setSort(initialFilter.getSortID(), initialFilter.isSortAscending());

        // compensate that query might automatically add visibility
        if (initialFilter.getVisibility() == VISIBILITY.DEFAULT) {
            parsedFilter.setVisibility(VISIBILITY.DEFAULT);
        }

        assertEquals(filterString, parsedFilter.toString());
        return query;
    }
    //################ tag filter support
    @Test
    public void shouldTagsNoneOnly() throws Exception {
        String FILTER_STRING = ";;;;;;;;notags";
        assertFilterQueryFilter(FILTER_STRING,"shouldTagsNoneOnly");
    }

    @Test
    public void shouldTagsIncludeExcludeWithWildcards() throws Exception {
        String FILTER_STRING = ";;;;;;%tag%;%utag%";
        assertFilterQueryFilter(FILTER_STRING,"shouldTagsIncludeExcludeWithWildcards");
    }

    @Test
    public void shouldTagsIncludeOrNone() throws Exception {
        String FILTER_STRING = ";;;;;;tag1,tag2,tag3;;notags";
        assertFilterQueryFilter(FILTER_STRING,"shouldTagsIncludeOrNone");
    }

    @Test
    public void shouldTagsIncludeWithoutNone() throws Exception {
        String FILTER_STRING = ";;;;;;tag1,tag2,tag3";
        assertFilterQueryFilter(FILTER_STRING,"shouldTagsIncludeWithoutNone");
    }

    @Test
    public void shouldTagsExcludeOrNone() throws Exception {
        String FILTER_STRING = ";;;;;;;tagexcl1,tagexcl2,tagexcl3;notags";
        assertFilterQueryFilter(FILTER_STRING,"shouldTagsExcludeOrNone");
    }

    @Test
    public void shouldTagsExcludeWithoutNone() throws Exception {
        String FILTER_STRING = ";;;;;;;tagexcl1,tagexcl2,tagexcl3";
        assertFilterQueryFilter(FILTER_STRING,"shouldTagsExcludeWithoutNone");
    }

    @Test
    public void shouldTagsIncludeExcludeOrNone() throws Exception {
        String FILTER_STRING = ";;;;;;tag1,tag2,tag3;tagexcl1,tagexcl2,tagexcl3;notags";
        assertFilterQueryFilter(FILTER_STRING,"shouldTagsIncludeExcludeOrNone");
    }

}
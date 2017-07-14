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

package de.k3b.android.androFotoFinder.tagDB;

import org.junit.Test;

import de.k3b.database.QueryParameter;
import de.k3b.io.GalleryFilterParameter;
import de.k3b.io.IGalleryFilter;

import static org.junit.Assert.*;

/**
 * TagSql unittests with dependencies to android, which will execute on the development machine (host).
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
    public void shouldTagsWithWildcards() throws Exception {
        String FILTER_STRING = ";;;;;;%tag%;%utag%;;3";
        assertFilterQueryFilter(FILTER_STRING);
    }


    @Test
    public void shouldParseNoGeo() throws Exception {
        String FILTER_STRING = "noGeoInfo;;;;;;;;;3";
        assertFilterQueryFilter(FILTER_STRING);
    }

    @Test
    public void shouldParsePrivate() throws Exception {
        assertFilterQueryFilter(IGalleryFilter.VISIBILITY_PRIVATE);
    }

    @Test
    public void shouldParsePrivatePublic() throws Exception {
        assertFilterQueryFilter(IGalleryFilter.VISIBILITY_PRIVATE_PUBLIC);
    }

    @Test
    public void shouldParsePublic() throws Exception {
        assertFilterQueryFilter(IGalleryFilter.VISIBILITY_PUBLIC);
    }

    @Test
    public void shouldEmptyBeDefault() throws Exception {
        String FILTER_STRING = "";
        GalleryFilterParameter parsedFilter = assertFilterQueryFilter(FILTER_STRING);
        assertEquals(IGalleryFilter.VISIBILITY_DEFAULT, parsedFilter.getVisibility());
    }

    // assert that input-string==output-string in  input-string -> filter -> query -> filter -> output-string
    private GalleryFilterParameter assertFilterQueryFilter(int visibility) {
        String FILTER_STRING = ";;;;;;;;;" + visibility;
        return assertFilterQueryFilter(FILTER_STRING);
    }

    // assert that input-string==output-string in  input-string -> filter -> query -> filter -> output-string
    private GalleryFilterParameter assertFilterQueryFilter(String filterString) {
        GalleryFilterParameter initialFilter = GalleryFilterParameter.parse(filterString, new GalleryFilterParameter());

        QueryParameter query = new QueryParameter();
        TagSql.filter2QueryEx(query, initialFilter, true);
        GalleryFilterParameter parsedFilter = (GalleryFilterParameter) TagSql.parseQueryEx(query, true);
        parsedFilter.setSort(initialFilter.getSortID(), initialFilter.isSortAscending());
        assertEquals(filterString, parsedFilter.toString());
        return parsedFilter;
    }
}
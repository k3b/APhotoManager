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
 * Created by k3b on 01.08.2015.
 */
public class SelectedItemsTests {
    @Test
    public void shoudParse() {
        SelectedItems sut = new SelectedItems().parse("1,2,3,1");
        Assert.assertEquals("size",3, sut.size());
        Assert.assertEquals("has 2",true, sut.contains(2l));
        Assert.assertEquals("has not 5",false, sut.contains(5l));
    }

    @Test
    public void shoudCreateString() {
        SelectedItems sut = new SelectedItems();
        sut.add(1l);
        sut.add(2l);
        sut.add(3l);
        sut.add(1l); // douplicate not included again
        Assert.assertEquals("1,2,3", sut.toString());
    }

}

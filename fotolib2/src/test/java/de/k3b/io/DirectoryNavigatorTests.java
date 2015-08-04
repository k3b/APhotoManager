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
 
package de.k3b.io;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by k3b on 12.06.2015.
 */
public class DirectoryNavigatorTests {
    // root[0..2][0..3][0..4]
    private final IDirectory root = createTestData();
    private final DirectoryNavigator sut = new DirectoryNavigator(root);

    @Before
    public void setup() {
        sut.setCurrentGrandFather(root);
    }
    @Test
    public void testGroupCount() {
        Assert.assertEquals(3, sut.getGroupCount());
    }

    @Test
    public void testChildCount() {
        Assert.assertEquals(4, sut.getChildrenCount(0));
    }

    @Test
    public void testGroup() {
        Assert.assertEquals("p_1", sut.getGroup(1).getRelPath());
    }

    @Test
    public void testChild() {
        Assert.assertEquals("p_1_2", sut.getChild(1, 2).getRelPath());
    }

    @Test
    public void testGroupIndexInvalid() {
        try {
            sut.getGroup(10);
            Assert.fail("should have thrown");
        } catch (IndexOutOfBoundsException e) {

        }
    }

    @Test
    public void testChildIndexInvalid() {
        try {
            sut.getChild(1, 10);
            Assert.fail("should have thrown");
        } catch (IndexOutOfBoundsException e) {

        }
    }

    @Test
    public void testNavigationGrandFather() {
        sut.setCurrentGrandFather(sut.getGroup(1));
        Assert.assertEquals("p_1_2_3", sut.getChild(2,3).getRelPath());
    }

    @Test
    public void testSubChildNotFound() {
        // valid testdata [0..2][0..3][0..4]
        Assert.assertEquals(null, sut.getSubChild(-1));
        Assert.assertEquals(null, sut.getSubChild(3));
        Assert.assertEquals(null, sut.getSubChild(1, 2, 5));
        Assert.assertEquals("only 3 levels exist", null, sut.getSubChild(1, 2, 3, 4));
    }

    @Test
    public void testSubChildFound() {
        Assert.assertEquals("minimum", "p_0_0_0", sut.getSubChild(0,0,0).getRelPath());
        Assert.assertEquals("in between", "p_1_2_3", sut.getSubChild(1, 2, 3).getRelPath());
        Assert.assertEquals("maximum", "p_2_3_4", sut.getSubChild(2,3,4).getRelPath());
    }

    @Test
    public void testNavigateTo() {
        IDirectory root = sut.getRoot();
        IDirectory subChild1 = sut.getSubChild(1);
        IDirectory subChild12 = sut.getSubChild(1,2);
        IDirectory subChild123 = sut.getSubChild(1,2,3);
        Assert.assertEquals("not found", root, sut.getNavigationGrandparent(null));
        Assert.assertEquals("first level", root, sut.getNavigationGrandparent(subChild1));
        Assert.assertEquals("sub level with children", subChild1, sut.getNavigationGrandparent(subChild12));
        Assert.assertEquals("sub level without children", subChild1, sut.getNavigationGrandparent(subChild123));
    }

    // root[0..2][0..3][0..4]
    private IDirectory createTestData() {
        Directory root = new Directory("", null, 0);

        for(int i=0; i < 3; i++) {
            Directory pi = new Directory("p_" + i, root, 0);
            for (int j = 0; j < 4; j++) {
                Directory pj = new Directory("p_" + i + "_" + j, pi, 0);
                for (int k = 0; k < 5; k++) {
                    IDirectory pk = new Directory("p_" + i + "_" + j + "_" + k, pj, 0);
                }
            }
        }
        return root;
    }

}

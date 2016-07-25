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

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by k3b on 04.08.2015.
 */
public class OSDirectoryTests {
    OSDirectory mRoot;

    @Before
    public void setup() {
        mRoot = createTestData("a","b","c","d");
    }

    @Test
    public void shoudFindExisting() {
        IDirectory found = OSDirectory.find(mRoot, new File("a/b/c"));
        assertNotNull(found);
        assertEquals(1, found.getChildren().size());
        assertEquals("d", found.getChildren().get(0).getRelPath());
    }

    @Test
    public void shoudFindExistingWithRoot() {
        mRoot = createTestData("/", "a","b","c","d");
        IDirectory found = OSDirectory.find(mRoot, new File("/a/b/c"));

        System.out.println(mRoot.toTreeString());
        assertNotNull(found);
        assertEquals(1, found.getChildren().size());
        assertEquals("d", found.getChildren().get(0).getRelPath());
    }

    @Test
    public void shoudFindNewWithRoot() {
        mRoot = createTestData("/", "a","b","c","d");
        IDirectory found = OSDirectory.find(mRoot, new File("/q"));

        System.out.println(mRoot.toTreeString());
        assertNotNull(found);
        assertEquals(2, mRoot.getChildren().size());
    }

    @Test
    public void shoudFindNew() {
        IDirectory found = OSDirectory.find(mRoot, new File("a/b/c/d2")).getParent();
        assertEquals(2, found.getChildren().size());
    }

    @Test
    public void shoudGetAbsolute() {
        String result = mRoot.getAbsolute();
        assertNotNull(result);
    }

    @Test
    public void shoudGetRelPath() {
        String result = mRoot.getRelPath();
        assertNotNull(result);
    }

    @Test
    public void shoudGetParent() {
        assertEquals(mRoot.getAbsolute(), mRoot.find(new File("a/b")).getParent().getAbsolute());
    }

    @Test
    public void shoudGetChildren() {
        assertEquals(true, mRoot.getChildren().size() > 0);
    }

    @Test
    public void shoudNotFind() {
        assertEquals(null, mRoot.find("DoesReallyNotExist"));
    }

    private OSDirectory createTestData(String... elements) {
        OSDirectory root = null;
        if ((elements != null) && (elements.length > 0)) {
            root = new OSDirectory(new File(elements[0]), null, new ArrayList<IDirectory>());

            OSDirectory current = root;
            for (int i = 1; i < elements.length; i++) {
                current = current.addChildFolder(elements[i], new ArrayList<IDirectory>());
            }
        }
        return root;
    }
}

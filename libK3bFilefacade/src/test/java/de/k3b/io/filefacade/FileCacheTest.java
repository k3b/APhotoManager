/*
 * Copyright (c) 2021 by k3b.
 *
 * This file is part of #APhotoManager (https://github.com/k3b/APhotoManager/)
 *              and #toGoZip (https://github.com/k3b/ToGoZip/).
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
package de.k3b.io.filefacade;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.k3b.io.FileUtilsBase;

public class FileCacheTest {
    @BeforeClass
    public static void init() {
        DirectoryFilter.init();
    }

    @Test
    public void get_putOneItem_found() {
        IFile file = new StringFileFacade().absolutePath("/Path/To/diR").dir();
        FileCache sut = new FileCacheImpl();

        sut.put(file);
        IFile found = sut.get("/path/to/Dir").getCurrent();
        Assert.assertEquals(file, found);
    }

    @Test
    public void getChildDirs_oneChildDir_ok() {
        StringFileFacade a = new StringFileFacade().absolutePath("/a").dir();
        StringFileFacade aa = new StringFileFacade().absolutePath("/a/a").parent(a).dir();

        FileCache sut = new FileCacheImpl();

        FileCacheItem root = sut.put(a);
        FileCacheItem[] children = sut.getChildDirs(root);

        Assert.assertEquals("len one child dir", 1, children.length);
        Assert.assertEquals("parent set", root, children[0].getParent());
        Assert.assertEquals("child-nomedia unknown yet", null, children[0].getNomedia());
        Assert.assertEquals("parent-nomedia without .nomedia", false, root.getNomedia());

        Assert.assertEquals("size dirs created", 2, sut.size());
    }

    @Test
    public void getChildDirs_oneChildNomediaFile_ok() {
        StringFileFacade a = new StringFileFacade().absolutePath("/a").dir();
        StringFileFacade aa = new StringFileFacade().absolutePath("/a/" + FileUtilsBase.MEDIA_IGNORE_FILENAME).parent(a);

        FileCache sut = new FileCacheImpl();

        FileCacheItem root = sut.put(a);
        FileCacheItem[] children = sut.getChildDirs(root);

        Assert.assertEquals("len no child dirs", 0, children.length);
        Assert.assertEquals("parent-nomedia", true, root.getNomedia());

        Assert.assertEquals("size only root dir without child dir", 1, sut.size());
    }

    @Test
    public void remove_rootWithOneChildDir_ok() {
        StringFileFacade a = new StringFileFacade().absolutePath("/a").dir();
        StringFileFacade aa = new StringFileFacade().absolutePath("/a/a").parent(a).dir();

        FileCache sut = new FileCacheImpl();

        FileCacheItem root = sut.put(a);
        FileCacheItem child = sut.getChildDirs(root)[0];

        Assert.assertEquals("size dirs created", 2, sut.size());

        sut.remove(a);
        Assert.assertEquals("size dirs removed", 0, sut.size());

        Assert.assertNull(root.getChildDirs(null));
        Assert.assertNull(child.getParent());
    }

    private static class FileCacheImpl extends FileCache<FileCacheItem> {
        @Override
        public FileCacheItem create(IFile file) {
            return new FileCacheItem(file);
        }

        @Override
        public FileCacheItem[] create(int size) {
            return new FileCacheItem[size];
        }
    }
}
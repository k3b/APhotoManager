/*
 * Copyright (c) 2016-2020 by k3b.
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

package de.k3b.tagDB;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import de.k3b.LibGlobal;
import de.k3b.TestUtil;
import de.k3b.io.ListUtils;
import de.k3b.io.filefacade.IFile;

/**
 * Created by k3b on 04.10.2016.
 */

public class TagRepositoryTests {
    // Obtain a logger instance
    private static final Logger LOGGER = LoggerFactory.getLogger(TagRepositoryTests.class);
    private static final IFile OUTDIR = TestUtil.OUTDIR_ROOT.createFile("TagRepositoryTests");
    private IFile repositoryFile = null;

    @BeforeClass
    public static void initDirectories() {
        OUTDIR.mkdirs();
        LibGlobal.debugEnabled = false;
    }

    private Tag createItem(int id) {
        Tag result = new Tag()
                .setName("Name" + id)
                ;
        return result;
    }

    private TagRepository createUnsavedRepo(String name, int numberOfItems) {
        return createUnsavedRepo(OUTDIR, name, numberOfItems);
    }

    private TagRepository createUnsavedRepo(IFile outdir, String name, int numberOfItems) {
        String fileName = name;
        if (fileName.indexOf(".") < 0) {
            fileName += "-repo.txt";
        }
        this.repositoryFile = outdir.createFile(fileName);
        repositoryFile.delete();

        TagRepository result = new TagRepository(this.repositoryFile);

        List<Tag> items = result.load();
        for (int i=1; i <= numberOfItems; i++) {
            items.add(createItem(i));
        }
        return result;
    }

    private TagRepository createUnsavedRepo(String name, String paths) {
        TagRepository result = createUnsavedRepo(name, 0);
        result.includePaths(null, paths);
        return result;
    }

    /* load() reload() createId() delete(T item)  save() */
    @Test
    public void shouldLoadNonExistentIsEmpty() {
        List items = createUnsavedRepo("shouldLoadNonExistentIsEmpty", 0).load();

        Assert.assertEquals(0, items.size());
    }

    @Test
    public void shouldSaveLoad() {
        createUnsavedRepo("shouldSaveLoad", 3).save();

        List items = new TagRepository(this.repositoryFile).load();

        Assert.assertEquals(3, items.size());
    }

    @Test
    public void saveWithError() throws FileNotFoundException {
        final IFile fileUnwritable = Mockito.mock(IFile.class);
        Mockito.when(fileUnwritable.openOutputStream()).thenThrow(new FileNotFoundException("junit"));
        Mockito.when(fileUnwritable.exists()).thenReturn(true);

        List<Tag> existingItems = new ArrayList<>();
        TagRepository.includePaths(existingItems, null, null, "a");

        TagRepository sut = new TagRepository(fileUnwritable, existingItems);

        LibGlobal.debugEnabled = true;
        Assert.assertNull(sut.save());
    }

    @Test
    public void shouldDeleteExistingItem() {
        List items = createUnsavedRepo("shouldDeleteExistingItem", 3)
                .save()
                .delete(createItem(2))
                .reload();
        Assert.assertEquals(2, items.size());
    }

    @Test
    public void shouldNotDeleteNonExistingItem() {
        List items = createUnsavedRepo("shouldNotDeleteNonExistingItem", 3)
                .save()
                .delete(createItem(7))
                .reload();
        Assert.assertEquals(3, items.size());
    }

    @Test
    public void shouldMoveRepo() {
        final String repoFileName = TagRepository.DB_NAME;
        IFile oldReproFile = createRepoDirWithoutRepo("shouldMoveRepoOld");
        final TagRepository oldRepo =
                createUnsavedRepo(oldReproFile, repoFileName, 2).save();
        TagRepository.setInstance(oldReproFile);

        IFile newReproFile = createRepoDirWithoutRepo("shouldMoveRepoNew");

        TagRepository.setInstance(newReproFile);

        Assert.assertEquals(2, TagRepository.getInstance().load().size());
    }

    private IFile createRepoDirWithoutRepo(String repoDirName) {
        IFile oldReproFile = OUTDIR.createFile(repoDirName);
        oldReproFile.mkdirs();
        oldReproFile.createFile(TagRepository.DB_NAME).delete();
        return oldReproFile;
    }

    @Test
    public void shouldMerge() {
        // 1,2,3
        TagRepository originalItems = createUnsavedRepo("shouldIncludeItemB12", "a/b1/c,a/b2");
        originalItems .save();

        // 1,2,7
        List<Tag> additionalItems = createUnsavedRepo("shouldIncludeItemB3", "a/b1/c,a/b3,c").load();

        // 7,1,2,3
        int changes = originalItems.merge(additionalItems);
        originalItems.save();
        List<Tag> items = originalItems.load();
        Assert.assertEquals("added 1", 2, changes);
        Assert.assertEquals(6, items.size());
    }

    @Test
    public void shouldAddExpression() throws Exception {
        // Name1
        TagRepository repo = createUnsavedRepo("shouldAddExpression", 1);
        List<Tag> items = repo.load();
        TagRepository.includePaths(items, items.get(0), null, "/a,b,c/c1/c11");
        StringWriter wr = new StringWriter();
        repo.save(items, wr, " ");

        String expected =
                "a\n" +
                "Name1\n" +
                " b\n" +
                " c\n" +
                "  c1\n" +
                "   c11\n";
        Assert.assertEquals(expected, wr.toString());
    }

    @Test
    public void shouldIncludePathsSimple() throws Exception {
        // Name1
        TagRepository repo = createUnsavedRepo("shouldIncludePathsSimple", 0);
        List<Tag> items = repo.load();
        TagRepository.includePaths(items, null, null, "/c/c1/c11");
        Tag c1 = repo.findFirstByName("c1");

        TagRepository.includePaths(items, c1.getParent(), c1, "c1New");

        StringWriter wr = new StringWriter();
        repo.save(items, wr, " ");

        String expected =
                        "c\n" +
                        " c1New\n" +
                        "  c11\n";
        Assert.assertEquals(expected, wr.toString());
    }

    @Test
    public void shouldIncludePathRoot() throws Exception {
        // Name1
        TagRepository repo = createUnsavedRepo("shouldIncludePathRoot", 0);
        List<Tag> items = repo.load();
        TagRepository.includePaths(items, null, null, "/c/c1/c11");
        Tag c1 = repo.findFirstByName("c1");

        TagRepository.includePaths(items, c1.getParent(), c1, "/c1AsRoot");

        StringWriter wr = new StringWriter();
        repo.save(items, wr, " ");

        String expected =
                "c\n" +
                "c1AsRoot\n" +
                " c11\n";
        Assert.assertEquals(expected, wr.toString());
    }

    @Test
    public void shouldIncludePathsWithInsert() throws Exception {
        // Name1
        TagRepository repo = createUnsavedRepo("shouldIncludePathsWithInsert", 0);
        List<Tag> items = repo.load();
        TagRepository.includePaths(items, null, null, "/c/c1/c11");
        Tag c1 = repo.findFirstByName("c1");

        TagRepository.includePaths(items, c1.getParent(), c1, "c1a/c1b");

        StringWriter wr = new StringWriter();
        repo.save(items, wr, " ");

        String expected =
                "c\n" +
                " c1a\n" +
                "  c1b\n" +
                "   c11\n";
        Assert.assertEquals(expected, wr.toString());
    }


    @Test
    public void shouldFindByString() {
        // 1,2,3
        TagRepository items = createUnsavedRepo("shouldFindByString", 3);

        Assert.assertEquals("Name2", true, items.contains("Name2"));
        Assert.assertEquals("Name5", false, items.contains("Name5"));
        Assert.assertEquals("naMe2 case in sentive", true, items.contains("naMe2"));
    }

    @Test
    public void shouldLoadHierarchy() throws Exception {
        String tagData =
                "__first\n" +
                "a\n" +
                " ab\n" +
                " ac\n" +
                "  aca\n" +
                "   doppelt\n" +
                "  acb\n" +
                "anderes\n" +
                "b\n" +
                " ba\n" +
                "  baa\n" +
                "  bab\n" +
                " bb\n" +
                "besonderes\n" +
                " doppelt\n" +
                "c\n" +
                "";

        ArrayList<Tag> items = new ArrayList<>();
        TagRepository sut = new TagRepository(null);
        sut.load(items, new StringReader(tagData));

        StringWriter wr = new StringWriter();
        sut.save(items, wr, " ");

        Assert.assertEquals(tagData, wr.toString());
    }

    @Test
    public void shouldGetChildren() throws Exception {
        String tagData =
                        "a\n" +
                        " ab\n" +
                        " ac\n" +
                        "   acz\n" +
                        "b\n" +
                        "";

        ArrayList<Tag> items = new ArrayList<>();
        TagRepository sut = new TagRepository(null);
        sut.load(items, new StringReader(tagData));

        Tag a = TagRepository.findFirstByName(items, "a");
        List<Tag> children = a.getChildren(items, true, false);
        Assert.assertEquals(3, children.size());
    }

    @Test
    public void shouldDelete() throws Exception {
        String tagData =
                "a\n" +
                " ab\n" +
                " ac\n" +
                "   acz\n" +
                "b\n" +
                "";

        ArrayList<Tag> items = new ArrayList<>();
        TagRepository sut = new TagRepository(null);
        sut.load(items, new StringReader(tagData));

        Tag a = TagRepository.findFirstByName(items, "a");
        int delCount = a.delete(items, false);
        Assert.assertEquals("delCount", 1, delCount);

        a = TagRepository.findFirstByName(items, "a");
        Assert.assertEquals("find a again", null, a);

        Tag z = TagRepository.findFirstByName(items, "acz");
        Assert.assertEquals("z after delete", "/ac/acz", z.getPath());

    }

    @Test
    public void shouldDeleteRecursive() throws Exception {
        String tagData =
                "a\n" +
                " ab\n" +
                " ac\n" +
                "   acz\n" +
                "b\n" +
                "";

        ArrayList<Tag> items = new ArrayList<>();
        TagRepository sut = new TagRepository(null);
        sut.load(items, new StringReader(tagData));

        Tag a = TagRepository.findFirstByName(items, "a");
        int delCount = a.delete(items, true);
        List<Tag> children = a.getChildren(items, false, false);
        Assert.assertEquals("delCount", 4, delCount);
    }

    @Test
    public void shouldInsertHierarchy() {
        TagRepository sut = createUnsavedRepo("shouldInsertHierarchy", 0);
        int changes = sut.includePaths(null,"a/b/c1,a/b/c2");
        sut.save();
        Assert.assertEquals(4, changes);
    }

    @Test
    public void shouldInsertIfNotFound() {
        TagRepository sut = createUnsavedRepo("shouldInsertIfNotFound", "a/b/c");
        int changes = sut.includeTagNamesIfNotFound((List<String>) ListUtils.fromString("c,b,q"));
        sut.save();
        Assert.assertEquals(1, changes);
    }

    @Test
    public void shouldRenameInHierachy() {
        TagRepository sut = createUnsavedRepo("shouldRenameInHierachy", "a/b/old/c,x/old/y");
        int changes = sut.renameTags("old","new");
        sut.save();
        Assert.assertEquals("/a/b/new/c", sut.findFirstByName("c").getPath());
        Assert.assertEquals("/x/new/y", sut.findFirstByName("y").getPath());
        Assert.assertEquals("num changes 2", 2, changes);
    }

    @Test
    public void shouldFindByPath() {
        List<Tag> sut = createUnsavedRepo("shouldFindByPath", "a/b/c,x/y/z").load();
        Tag root = Tag.findByPath(sut, null, "x/y");

        Tag found = Tag.findByPath(sut, null, "a/b/c");
        Assert.assertEquals("/a/b/c", found.getPath());

        found = Tag.findByPath(sut, root, "/a/b/c");
        Assert.assertEquals("/a/b/c", found.getPath());
    }

    @Test
    public void shouldNotFindByPath() {
        List<Tag> sut = createUnsavedRepo("shouldFindByPath", "a/b/c,x/y/z").load();
        Tag root = Tag.findByPath(sut, null, "x/y");

        Tag found = Tag.findByPath(sut, root, "a/q/c");
        Assert.assertEquals("no a/q", null, found);

        found = Tag.findByPath(sut, root, "a/b/c");
        Assert.assertEquals("wrong root", null, found);
    }
}

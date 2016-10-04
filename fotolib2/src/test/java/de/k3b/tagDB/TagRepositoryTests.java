package de.k3b.tagDB;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

/**
 * Created by k3b on 04.10.2016.
 */

public class TagRepositoryTests {
    // Obtain a logger instance
    private static final Logger LOGGER = LoggerFactory.getLogger(TagRepositoryTests.class);
    private static final File OUTDIR = new File("./build/testresults/TagRepositoryTests");
    private File repositoryFile = null;

    @BeforeClass
    public static void initDirectories() {
        OUTDIR.mkdirs();
    }

    private Tag createItem(int id) {
        Tag result = new Tag()
                .setName("Name" + id)
                ;
        return result;
    }

    private TagRepository createUnsavedRepo(String name, int numberOfItems) {
        this.repositoryFile = new File(OUTDIR, name + "-repo.txt");
        repositoryFile.delete();

        TagRepository result = new TagRepository(this.repositoryFile);

        List items = result.load();
        for (int i=1; i <= numberOfItems; i++) {
            items.add(createItem(i));
        }
        return result;
    }

    /* load() reload() createId() delete(T item)  save() */
    @Test
    public void shouldLoadNonExistentIsEmpty() throws Exception {
        List items = createUnsavedRepo("shouldLoadNonExistentIsEmpty", 0).load();

        Assert.assertEquals(0, items.size());
    }

    @Test
    public void shouldSaveLoad() throws Exception {
        createUnsavedRepo("shouldSaveLoad", 3).save();

        List items = new TagRepository(this.repositoryFile).load();

        Assert.assertEquals(3, items.size());
    }

    @Test
    public void shouldDeleteExistingItem() throws Exception {
        List items = createUnsavedRepo("shouldDeleteExistingItem", 3)
                .save()
                .delete(createItem(2))
                .reload();
        Assert.assertEquals(2, items.size());
    }

    @Test
    public void shouldNotDeleteNonExistingItem() throws Exception {
        List items = createUnsavedRepo("shouldNotDeleteNonExistingItem", 3)
                .save()
                .delete(createItem(7))
                .reload();
        Assert.assertEquals(3, items.size());
    }

    @Test
    public void shouldIncludeItem() throws Exception {
        // 1,2,3
        TagRepository originalItems = createUnsavedRepo("shouldIncludeItem", 3)
                .save();

        // 1,2,7
        List additionalItems = createUnsavedRepo("shouldIncludeItem", 2).load();
        Tag added = createItem(7);
        additionalItems.add(added);

        // 7,1,2,3
        int changes = originalItems.include(additionalItems);
        List<Tag> items = originalItems.load();
        Assert.assertEquals(added + "added 1", 1, changes);
        Assert.assertEquals(4, items.size());
    }

}

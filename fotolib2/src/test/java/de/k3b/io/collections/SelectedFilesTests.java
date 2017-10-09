package de.k3b.io.collections;

import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;

import de.k3b.io.collections.SelectedFiles;

/**
 * Created by EVE on 06.10.2016.
 */

public class SelectedFilesTests {
    @Test
    public void shoudParseAndFormatString() {
        String names = "'a','b','c'";
        String ids = "1,2,3";
        SelectedFiles sut = new SelectedFiles(names, ids);
        Assert.assertEquals("names", names, sut.toString());
        Assert.assertEquals("ids", ids, sut.toIdString());
    }

    @Test
    public void shoudIterate() {
        String names = "'a','b','c'";
        String ids = "1,2,3";
        SelectedFiles data = new SelectedFiles(names, ids);
        Iterator<IMediaFile> sut = new MediaFileIterator(data.iter(), null);

        sut.next();sut.next();
        IMediaFile media = sut.next();

        Assert.assertEquals("id", 3, media.getID());
        Assert.assertEquals("path", "c", media.getFullJpgSourcePath());
        Assert.assertEquals("has item#4", false, sut.hasNext());

    }

}

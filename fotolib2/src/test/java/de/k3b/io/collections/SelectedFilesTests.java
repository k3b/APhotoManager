package de.k3b.io.collections;

import org.junit.Assert;
import org.junit.Test;

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
}

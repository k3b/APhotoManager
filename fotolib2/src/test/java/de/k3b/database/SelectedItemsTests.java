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
        Assert.assertEquals("has 2",true, sut.contains(2));
        Assert.assertEquals("has not 5",false, sut.contains(5));
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

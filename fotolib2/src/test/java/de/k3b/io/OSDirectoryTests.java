package de.k3b.io;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * Created by k3b on 04.08.2015.
 */
public class OSDirectoryTests {
    private static final OSDirectory sRoot = new OSDirectory(OSDirectory.getCanonicalFile(".."));

    @Test
    public void shoudGetAbsolute() {
        String result = sRoot.getAbsolute();
        Assert.assertNotNull(result);
    }

    @Test
    public void shoudGetRelPath() {
        String result = sRoot.getRelPath();
        Assert.assertNotNull(result);
    }

    @Test
    public void shoudGetParent() {
        Assert.assertEquals(sRoot.getAbsolute(), new OSDirectory(OSDirectory.getCanonicalFile(".")).getParent().getAbsolute());
    }

    @Test
    public void shoudGetChildren() {
        Assert.assertEquals(true, sRoot.getChildren().size() > 0);
    }

    @Test
    public void shoudNotFind() {
        Assert.assertEquals(null, sRoot.find("DoesReallyNotExist"));
    }

}

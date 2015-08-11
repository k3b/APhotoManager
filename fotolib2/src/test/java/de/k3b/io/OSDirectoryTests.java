package de.k3b.io;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.any;

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
        File elementFile = null;
        OSDirectory root = null;
        OSDirectory parent = null;
        OSDirectory current = null;
        List<IDirectory> childen = null;
        for (String element : elements) {
            elementFile = (elementFile == null) ? new File(element) : new File(elementFile, element);
            current = new OSDirectory(elementFile, parent, new ArrayList<IDirectory>());
            if (parent != null) {
                parent.getChildren().add(current);
            }

            parent = current;

            if (root == null) {
                root = current;
            }
        }

        return root;
    }

}

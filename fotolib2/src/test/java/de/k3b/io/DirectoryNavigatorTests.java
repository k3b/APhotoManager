package de.k3b.io;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by k3b on 12.06.2015.
 */
public class DirectoryNavigatorTests {
    // root[0..2][0..3][0..4]
    private final Directory root = createTestData();
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
    public void testNavigation() {
        sut.setCurrentGrandFather(sut.getGroup(1));
        Assert.assertEquals("p_1_2_3", sut.getChild(2,3).getRelPath());
    }

    // root[3][4][5]
    private Directory createTestData() {
        Directory root = new Directory("", null, 0);

        for(int i=0; i < 3; i++) {
            Directory pi = new Directory("p_" + i, root, 0);
            for (int j = 0; j < 4; j++) {
                Directory pj = new Directory("p_" + i + "_" + j, pi, 0);
                for (int k = 0; k < 5; k++) {
                    Directory pk = new Directory("p_" + i + "_" + j + "_" + k, pj, 0);
                }
            }
        }
        return root;
    }

}

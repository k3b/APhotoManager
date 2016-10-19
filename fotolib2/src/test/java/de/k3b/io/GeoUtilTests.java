package de.k3b.io;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by k3b on 19.10.2016.
 */

public class GeoUtilTests {
    @Test
    public void shoudParse() {
        assertParse(50.5, "+50.5","NS");
        assertParse(-50.5, "-50.5","NS");
        assertParse(50.5, "50, 30N","NS");
        assertParse(-50.5, "50, 30S","NS");
        assertParse(-50.5, "S 50'30''0.00","NS");
    }

    private void assertParse(double expected, String actual, String plusMinusns) {
        Assert.assertEquals(actual, expected, GeoUtil.parse(actual, plusMinusns), 0.00001);
    }
}

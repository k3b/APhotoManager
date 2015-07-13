package de.k3b.io;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by k3b on 08.06.2015.
 */
public class DirectoryFormatterTests {
    @Test
    public void shoudFormatLatLonPathNoRound() {
        String result = DirectoryFormatter.getLatLonPath(8.3459, 54.1239);
        Assert.assertEquals("/ 0,50/8,54/8.3,54.1/8.34,54.12/8.345,54.123/", result);
    }

    @Test
    public void shoudFormatLatLonPathZero() {
        String result = DirectoryFormatter.getLatLonPath(8.0, 54.0);
        Assert.assertEquals("/ 0,50/8,54/8.0,54.0/8.00,54.00/8.000,54.000/", result);
    }

    @Test
    public void shoudGetLastPath() {
        String result = DirectoryFormatter.getLastPath("/ 0,50/8,54/8.0,54.0/8.00,54.00/8.000,54.000/");
        Assert.assertEquals("8.000,54.000", result);
    }

    @Test
    public void shoudGetLatLon10() {
        IGeoRectangle result = DirectoryFormatter.getLatLon(" 130,12");
        Assert.assertEquals("130.0,12.0..140.0,22.0", result.toString());
    }

    @Test
    public void shoudGetLatLon1() {
        IGeoRectangle result = DirectoryFormatter.getLatLon("130,12");
        Assert.assertEquals("130.0,12.0..131.0,13.0", result.toString());
    }

    @Test
    public void shoudGetLatLon0() {
        IGeoRectangle result = DirectoryFormatter.getLatLon("130.0,12.0");
        Assert.assertEquals("130.0,12.0..130.1,12.1", result.toString());
    }

    @Test
    public void shoudGetLatLon00() {
        IGeoRectangle result = DirectoryFormatter.getLatLon("130.00,12.00");
        Assert.assertEquals("130.0,12.0..130.01,12.01", result.toString());
    }
}

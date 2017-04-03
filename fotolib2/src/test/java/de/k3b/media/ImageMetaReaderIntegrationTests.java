package de.k3b.media;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import de.k3b.io.DateUtil;
import de.k3b.io.ListUtils;

/**
 * Created by k3b on 28.03.2017.
 */

public class ImageMetaReaderIntegrationTests {
    private static final Logger logger = LoggerFactory.getLogger(ImageMetaReaderIntegrationTests.class);

    private ImageMetaReader sut = null;
    @Before
    public void setup() throws IOException {
        ImageMetaReader.DEBUG = true;
        sut = getMeta("test-WitExtraData.jpg");
    }

    @Test
    public void shouldDump() throws IOException
    {
        // System.out.printf(sut.toString());
        logger.info(sut.toString());
    }

    @Test
    public void shouldGetDescription() throws IOException
    {
        Assert.assertEquals("ImageDescription", sut.getDescription());
    }

    @Test
    public void shouldGetTitle() throws IOException
    {
        Assert.assertEquals("XPTitle", sut.getTitle());
    }

    @Test
    public void shouldGetDateTimeTaken() throws IOException
    {
        Assert.assertEquals("1962-11-07T09:38:46", DateUtil.toIsoDateString(sut.getDateTimeTaken()));
    }

    @Test
    public void shouldGetLatitude() throws IOException
    {
        Assert.assertEquals(27.8186, sut.getLatitude(), 0.01);
    }
    @Test
    public void shouldGetLongitude() throws IOException
    {
        Assert.assertEquals(-15.764, sut.getLongitude(), 0.01);
    }

    @Test
    public void shouldGetTags() throws IOException
    {
        Assert.assertEquals("Marker1, Marker2", ListUtils.toString(sut.getTags(),", "));
    }

    @Test
    public void shouldGetRating() throws IOException
    {
        Assert.assertEquals(3, sut.getRating().intValue());
    }

    protected ImageMetaReader getMeta(String fileName) throws IOException {
        InputStream inputStream = ImageMetaReaderIntegrationTests.class.getResourceAsStream("images/" + fileName);
        Assert.assertNotNull("open images/" + fileName, inputStream);
        ImageMetaReader result = new ImageMetaReader().load(fileName, inputStream, null);
        return result;
    }
}

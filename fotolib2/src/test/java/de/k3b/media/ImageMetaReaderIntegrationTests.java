package de.k3b.media;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

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
        Assert.assertEquals("Comment", sut.getDescription());
    }

    @Test
    public void shouldGetTitle() throws IOException
    {
        Assert.assertEquals("XPTitle", sut.getTitle());
    }

    protected ImageMetaReader getMeta(String fileName) throws IOException {
        InputStream inputStream = ImageMetaReaderIntegrationTests.class.getResourceAsStream("images/" + fileName);
        Assert.assertNotNull("open images/" + fileName, inputStream);
        ImageMetaReader result = new ImageMetaReader().load(fileName, inputStream);
        return result;
    }
}

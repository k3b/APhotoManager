package de.k3b.media;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by k3b on 28.03.2017.
 */

public class ImageMetaReaderIntegrationTests {
    private static final Logger logger = LoggerFactory.getLogger(ImageMetaReaderIntegrationTests.class);

    @Test
    public void shouldDump() throws IOException
    {
        ImageMetaReader doCopy = getMeta("test-WitExtraData.jpg");
        System.out.printf(doCopy.toString());
        logger.info(doCopy.toString());
    }

    protected ImageMetaReader getMeta(String fileName) throws IOException {
        InputStream inputStream = ImageMetaReaderIntegrationTests.class.getResourceAsStream("images/" + fileName);
        Assert.assertNotNull("open images/" + fileName, inputStream);
        ImageMetaReader result = new ImageMetaReader().load(fileName, inputStream);
        return result;
    }

}

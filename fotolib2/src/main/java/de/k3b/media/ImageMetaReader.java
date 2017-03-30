/*
 * Copyright (c) 2017 by k3b.
 *
 * This file is part of AndroFotoFinder / #APhotoManager.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>
 */
package de.k3b.media;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import de.k3b.FotoLibGlobal;
import sun.awt.image.JPEGImageDecoder;

/**
 * com.drewnoakes:metadata-extractor based reader for image meta data files
 * Created by k3b on 27.03.2017.
 */

public class ImageMetaReader implements IMetaApi, Closeable {
    private static final Logger logger = LoggerFactory.getLogger(FotoLibGlobal.LOG_TAG);

    private String mFilename = null;
    private Metadata mMetadata = null;

    /**
     * Reads Meta data from the specified file.
     *
     * @param filename
     */
    public ImageMetaReader load(String filename, InputStream inputStream) throws IOException {
        mFilename = filename;

        Metadata metadata = null;
        File jpegFile = (inputStream == null) ? new File(filename) : null;
        try {
            if (inputStream != null) {
                // so proguard can eleminate support for gif, png and other image formats
                metadata = JpegMetadataReader.readMetadata(inputStream);
                // metadata = ImageMetadataReader.readMetadata(inputStream);
            } else {
                metadata = JpegMetadataReader.readMetadata(jpegFile);
                // metadata = ImageMetadataReader.readMetadata(jpegFile);
            }
            // IptcDirectory.TAG_ARM_VERSION
        } catch (ImageProcessingException e) {
            logger.error("ImageMetaReader: Error open  " + filename, e);

            metadata = null;
        }
        mMetadata = metadata;
        if (metadata == null) return null;
        return this;
    }

    private static final String NL = "\n";

    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Directory directory : mMetadata.getDirectories()) {
            String dirName = "";
            Directory parent = directory;
            while (parent != null) {
                dirName = parent.getName() + "." + dirName;
                parent = null; // directory.getParent(); requires newer version
            }

            /*
            builder.append(NL).append(dirName).append(":")
                    .append(directory.getClass().getSimpleName()).append(NL);
            */
            
            for (Tag tag : directory.getTags()) {
                String description = tag.getDescription();
                if (description == null)
                    description = "";

                builder.append(dirName).append(tag.getTagName())
                        .append("(").append(tag.getTagType()).append(")=").append(description).append(NL);
            }
        }
        return builder.toString();
    }

    @Override
    public String getPath() {
        return mFilename;
    }

    @Override
    public IMetaApi setPath(String filePath) {
        throw new UnsupportedOperationException ();
    }


    /**
     * When the photo was taken (not file create/modify date) in local time or utc
     */
    @Override
    public Date getDateTimeTaken() {
        init(); return null;
    }

    private void init() {
    }

    @Override
    public IMetaApi setDateTimeTaken(Date value) {
        throw new UnsupportedOperationException ();
    }

    /**
     * Latitude, in degrees north.
     *
     * @param latitude
     */
    @Override
    public IMetaApi setLatitude(Double latitude) {
        throw new UnsupportedOperationException ();
    }

    /**
     * Longitude, in degrees east.
     *
     * @param longitude
     */
    @Override
    public IMetaApi setLongitude(Double longitude) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public Double getLatitude() {
        init(); return null;
    }

    @Override
    public Double getLongitude() {
        return null;
    }

    /**
     * Title = Short Descrioption used as caption
     */
    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public IMetaApi setTitle(String title) {
        throw new UnsupportedOperationException ();
    }

    /**
     * Longer description = comment. may have more than one line
     */
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public IMetaApi setDescription(String description) {
        throw new UnsupportedOperationException ();
    }

    /**
     * Tags/Keywords/Categories/VirtualAlbum used to find images
     */
    @Override
    public List<String> getTags() {
        return null;
    }

    @Override
    public IMetaApi setTags(List<String> tags) {
        throw new UnsupportedOperationException ();
    }

    /**
     * 5=best .. 1=worst or 0/null unknown
     */
    @Override
    public Integer getRating() {
        return null;
    }

    @Override
    public IMetaApi setRating(Integer value) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public void close() throws IOException {

    }
}

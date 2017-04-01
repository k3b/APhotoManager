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

import com.drew.imaging.ImageProcessingException;
import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.iptc.IptcDirectory;
import com.drew.metadata.jpeg.JpegCommentDirectory;
import com.drew.metadata.xmp.XmpDirectory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

/**
 * com.drewnoakes:metadata-extractor based reader for image meta data files
 * Created by k3b on 27.03.2017.
 */

public class ImageMetaReader implements IMetaApi, Closeable {
    // public: used as log filter for crash report
    public  static final String LOG_TAG = "";

    // public: can be changed in settings dialog
    public  static boolean DEBUG = false;

    private static final Logger logger = LoggerFactory.getLogger(LOG_TAG);

    private String mFilename = null;
    private Metadata mMetadata = null;
    private Directory exifDir;
    private Directory iptcDir;
    // private Directory fileDir;
    private Directory commentDir;
    private Directory xmpDir;

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
        init(); return null;
    }

    /**
     * Title = Short Descrioption used as caption
     */
    @Override
    public String getTitle() {
        String debugContext = "getTitle";
        init();
        /*
        -XMP-dc:Title < EXIF:XPTitle
-XMP-dc:Title < iptc:Headline
*/

        String result = null;
        //!!! not implemented in  com.drewnoakes:metadata-extractor:2.8.1
        // if (result == null) result = getString(debugContext, xmpDir, XmpDirectory.TAG_TITLE);

        if (result == null) result = getString(debugContext, exifDir, ExifDirectoryBase.TAG_WIN_TITLE);
        //=> XPTitle

        if (result == null) result = getString(debugContext, iptcDir, IptcDirectory.TAG_HEADLINE);
        // => Headline

        return result;
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
        String debugContext = "getDescription";
        init();
        String result = null;

        if (result == null) result = getString(debugContext, exifDir, ExifDirectoryBase.TAG_WIN_COMMENT);
        // => Comment

        if (result == null) result = getString(debugContext, exifDir, ExifDirectoryBase.TAG_IMAGE_DESCRIPTION);
        // => ImageDescription

        //!!! not implemented in  com.drewnoakes:metadata-extractor:2.8.1
        //!!! if (result == null) result = getString(debugContext, xmpDir, XmpDirectory.TAG_DESCRIPTION);

        if (result == null) result = getString(debugContext, iptcDir, IptcDirectory.TAG_CAPTION);
        // => Caption-Abstract

        if (result == null) result = getString(debugContext, commentDir, JpegCommentDirectory.TAG_COMMENT);
        // => Comment

        //!!! not implemented in  com.drewnoakes:metadata-extractor:2.8.1
        // not supported -XMP-dc:Description < File:Comment
        return result;
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
        init(); return null;
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
        init(); return null;
    }

    @Override
    public IMetaApi setRating(Integer value) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public void close() throws IOException {

    }

    @Override
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
                int tagType = tag.getTagType();
                appendValue(builder, directory, dirName, tagType);
                builder.append(NL);
            }
        }
        return builder.toString();
    }

    //--------------- local helpers

    private StringBuilder appendValue(StringBuilder result, Directory directory, String dirName, int tagType) {
        String tagValue = (directory == null) ? null  : directory.getDescription(tagType);
        String tagName  = (directory == null) ? "???" : directory.getTagName(tagType);
        if (tagValue == null)
            tagValue = "";

        result.append(dirName).append(tagName);
        if (DEBUG) {
            result.append("(0x").append( Integer.toHexString(tagType)).append(")");
        }
        result.append("=").append(tagValue);
        return result;
    }

    private void init() {
        exifDir = this.mMetadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        iptcDir = this.mMetadata.getFirstDirectoryOfType(IptcDirectory.class);
        // fileDir = this.mMetadata.getFirstDirectoryOfType(FileDi.class);
        xmpDir = this.mMetadata.getFirstDirectoryOfType(XmpDirectory.class);
        commentDir = this.mMetadata.getFirstDirectoryOfType(JpegCommentDirectory.class);

    }

    private String getString(String debugContext, Directory directory, int tagType) {
        String result = null;
        if (directory != null) {
            result = directory.getDescription(tagType);
            if (DEBUG) {
                StringBuilder dbg = new StringBuilder();
                dbg.append(debugContext).append(":");
                appendValue(dbg, directory, directory.getName() + ".", tagType);
                logger.info(dbg.toString());
            }
        }
        if ((result != null) && (result.length() == 0)) return null;
        return result;
    }
}

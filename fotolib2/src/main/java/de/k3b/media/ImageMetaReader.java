/*
 * Copyright (c) 2017-2018 by k3b.
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
import com.drew.lang.GeoLocation;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
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

import de.k3b.FotoLibGlobal;
import de.k3b.io.DateUtil;
import de.k3b.io.ListUtils;
import de.k3b.io.StringUtils;
import de.k3b.io.VISIBILITY;

/**
 * com.drewnoakes:metadata-extractor based reader for image meta data files
 * Created by k3b on 27.03.2017.
 */

public class ImageMetaReader implements IMetaApi, Closeable {
    // public: used as log filter for crash report
    public  static final String LOG_TAG = "ImageMetaReader";

    private static final Logger logger = LoggerFactory.getLogger(LOG_TAG);

    // public: can be changed in settings dialog
    public  static boolean DEBUG = false;
    private static final boolean DEBUG_ALWAYS_NULL = false; // debug-time to enfaorce all meta readings

    private static final String checksum1 = "cmgol.nri.m.d.oied";
    private static final String checksum2 = "o.ogeadodgsasMblAs";
    private static final String checksum3 = "d.3.nri.tlDUis";
    private static final String checksum4 = "ekbadodui.Btl";

    private String mFilename = null;
    private IMetaApi mExternalXmpDir;
    private MediaXmpSegment mInternalXmpDir;
    private Metadata mMetadata = null;
    protected Directory mExifDir;
    protected Directory mExifSubDir;
    private GeoLocation mExifGpsDir;
    protected Directory mIptcDir;
    // private Directory fileDir;
    private Directory mCommentDir;
    private String dbg_context = "";
    private boolean mInitExecuted = false;

    static {
        FotoLibGlobal.itpcWriteSupport = hasItpcWriteSupport();
    }

    private static boolean hasItpcWriteSupport() {
        return !check(StringUtils.merge(checksum3, checksum4)) || check(StringUtils.merge(checksum1, checksum2));
    }

    private static boolean check(String checksum) {
        try {
            Class cls = Class.forName(checksum);
            if (cls != null) return true;
        } catch (ClassNotFoundException e) {

        }
        return false;
    }

    /**
     * Reads Meta data from the specified inputStream (if not null) or File(filename).
     */
    public ImageMetaReader load(String filename, InputStream inputStream, IMetaApi externalXmpContent, String _dbg_context) throws IOException {
        mInitExecuted = false;
        mFilename = filename;
        mExternalXmpDir = externalXmpContent;
        this.dbg_context = _dbg_context + "->ImageMetaReader(" + mFilename+ ") ";

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
            logger.error(dbg_context +" Error open file " + e.getMessage(), e);

            metadata = null;
        }
        mMetadata = metadata;

        if (metadata == null) {
            if (FotoLibGlobal.debugEnabledJpgMetaIo) {
                logger.debug(dbg_context +
                        "load: file not found ");
            }
            return null;
        }

        if (FotoLibGlobal.debugEnabledJpgMetaIo) {
            logger.debug(dbg_context +
                    "loaded: " + MediaUtil.toString(this, false, null, MediaUtil.FieldID.path, MediaUtil.FieldID.clasz));
        }

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
        String debugContext = "getDateTimeTaken";
        int i=0;

        init();

        Date result = null;
        if (isEmpty(result, ++i, debugContext, "ExifSubIFD.DATETIME_ORIGINAL") && (mExifSubDir != null)) result = mExifSubDir.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL, DateUtil.UTC);

        if (isEmpty(result, ++i, debugContext, "ExternalXmp.DateTimeTaken") && (mExternalXmpDir != null)) result = mExternalXmpDir.getDateTimeTaken();
        if (isEmpty(result, ++i, debugContext, "InternalXmp.DateTimeTaken") && (mInternalXmpDir != null)) result = mInternalXmpDir.getDateTimeTaken();
        isEmpty(result, ++i, null, null);
        return result;
    }

    @Override
    public IMetaApi setDateTimeTaken(Date value) {
        throw new UnsupportedOperationException ();
    }

    /** latitude, in degrees north. (-90 .. +90); longitude, in degrees east.  (-180 .. + 180)    */
    @Override public IMetaApi setLatitudeLongitude(Double latitude, Double longitude) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public Double getLatitude() {
        String debugContext = "getLatitude";
        int i=0;
        init();

        Double result = null;
        if (isEmpty(result, ++i, debugContext, "ExifGps.Latitude") && (mExifGpsDir != null)) result = mExifGpsDir.getLatitude();

        if (isEmpty(result, ++i, debugContext, "ExternalXmp.Latitude") && (mExternalXmpDir != null)) result = mExternalXmpDir.getLatitude();
        if (isEmpty(result, ++i, debugContext, "InternalXmp.Latitude") && (mInternalXmpDir != null)) result = mInternalXmpDir.getLatitude();
        isEmpty(result, ++i, null, null);
        return result;
    }

    @Override
    public Double getLongitude() {
        String debugContext = "getLongitude";
        int i=0;

        init();

        Double result = null;
        if (isEmpty(result, ++i, debugContext, "ExifGps.Longitude") && (mExifGpsDir != null)) result = mExifGpsDir.getLongitude();

        if (isEmpty(result, ++i, debugContext, "ExternalXmp.Longitude") && (mExternalXmpDir != null)) result = mExternalXmpDir.getLongitude();
        if (isEmpty(result, ++i, debugContext, "InternalXmp.Longitude") && (mInternalXmpDir != null)) result = mInternalXmpDir.getLongitude();
        isEmpty(result, ++i, null, null);
        return result;
    }

    /**
     * Title = Short Descrioption used as caption
     */
    @Override
    public String getTitle() {
        String debugContext = "getTitle";
        int i=0;

        init();

        String result = null;

        if (isEmpty(result, ++i, debugContext, "ExternalXmp.Title") && (mExternalXmpDir != null)) result = mExternalXmpDir.getTitle();

        if (isEmpty(result, ++i, debugContext, "Exif.XPTITLE")) result = getString(debugContext, mExifDir, ExifDirectoryBase.TAG_WIN_TITLE);
        //=> XPTitle

        if (isEmpty(result, ++i, debugContext, "IptcDirectory.TAG_HEADLINE")) result = getString(debugContext, mIptcDir, IptcDirectory.TAG_HEADLINE);
        // => Headline

        if ((isEmpty(result, ++i, debugContext, "InternalXmp.Title")) && (mInternalXmpDir != null)) result = mInternalXmpDir.getTitle();

        isEmpty(result, ++i, null, null);
        return result;
    }

    protected boolean isEmpty(Object result, int tryNumber, String debugContext, String debugFieldName) {
        if (DEBUG_ALWAYS_NULL) {
            logger.info(debugContext + "#" + tryNumber + ":" +result);
            return true;
        }
        return result == null;
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
        int i=0;

        init();
        String result = null;

        if (isEmpty(result, ++i, debugContext, "Exif.IMAGE_DESCRIPTION"))
            result = getString(debugContext, mExifDir, ExifDirectoryBase.TAG_IMAGE_DESCRIPTION);
        // => ImageDescription

        if ((isEmpty(result, ++i, debugContext, "ExternalXmp.Description")) && (mExternalXmpDir != null))
            result = mExternalXmpDir.getDescription();

        if (isEmpty(result, ++i, debugContext, "Exif.XPCOMMENT"))
            result = getString(debugContext, mExifDir, ExifDirectoryBase.TAG_WIN_COMMENT);
        // => Comment

        if (isEmpty(result, ++i, debugContext, "Exif.USER_COMMENT"))
            result = getString(debugContext, mExifDir, ExifDirectoryBase.TAG_USER_COMMENT);
        // => ImageDescription

        if (isEmpty(result, ++i, debugContext, "JpegComment.COMMENT"))
            result = getString(debugContext, mCommentDir, JpegCommentDirectory.TAG_COMMENT);
        // => Comment

        if ((isEmpty(result, ++i, debugContext, "InternalXmp.Description")) && (mInternalXmpDir != null))
            result = mInternalXmpDir.getDescription();

        //!!! not implemented in  com.drewnoakes:metadata-extractor:2.8.1
        //!!! if isEmpty(result) result = getString(debugContext, mInternalXmpDir, XmpDirectory.TAG_DESCRIPTION);

        if (isEmpty(result, ++i, debugContext, "Iptc.CAPTION"))
            result = getString(debugContext, mIptcDir, IptcDirectory.TAG_CAPTION);
        // => Caption-Abstract

        //!!! not implemented in  com.drewnoakes:metadata-extractor:2.8.1
        // not supported -XMP-dc:Description < File:Comment
        isEmpty(result, ++i, null, null);
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
        String debugContext = "getTags";
        int i=0;

        init();
        List<String> result = null;

        if ((isEmpty(result, ++i, debugContext, "ExternalXmp.Tags")) && (mExternalXmpDir != null))
            result = mExternalXmpDir.getTags();

        if (isEmpty(result, ++i, debugContext, "Exif.XPKEYWORDS") && (mExifDir != null)) {
            result = getStringList(debugContext, this.mExifDir, ExifDirectoryBase.TAG_WIN_KEYWORDS);
        }

        if (isEmpty(result, ++i, debugContext, "Iptc.KEYWORDS")) {
            result = getStringList(debugContext, this.mIptcDir, IptcDirectory.TAG_KEYWORDS);
        }

        if ((isEmpty(result, ++i, debugContext, "InternalXmp.Tags")) && (mInternalXmpDir != null)) result = mInternalXmpDir.getTags();

        isEmpty(result, ++i, null, null);
        return result;
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
        String debugContext = "getRating";
        int i=0;
        init();

        Integer result = null;
        if (isEmpty(result, ++i, debugContext, "ExternalXmp.Rating") && (mExternalXmpDir != null)) result = mExternalXmpDir.getRating();
        if (isEmpty(result, ++i, debugContext, "Exif.RATING") && (mExifDir != null)) result = mExifDir.getInteger(ExifDirectoryBase.TAG_RATING);
        if (isEmpty(result, ++i, debugContext, "InternalXmp.Rating") && (mInternalXmpDir != null)) result = mInternalXmpDir.getRating();

        isEmpty(result, ++i, null, null);
        return result;
    }

    @Override
    public IMetaApi setRating(Integer value) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public VISIBILITY getVisibility() {
        String debugContext = "getVisibility";
        int i=0;
        init();

        VISIBILITY result = null;
        if (isEmpty(result, ++i, debugContext, "Exif.XPKEYWORDS(PRIVATE)") && (mExifDir != null)) {
            List<String> list = getStringList(debugContext, this.mExifDir, ExifDirectoryBase.TAG_WIN_KEYWORDS);
            result = VISIBILITY.getVisibility(list);
        }
        if (isEmpty(result, ++i, debugContext, "ExternalXmp.apm.Visibility") && (mExternalXmpDir != null)) result = mExternalXmpDir.getVisibility();
        if (isEmpty(result, ++i, debugContext, "InternalXmp.apm.Visibility") && (mInternalXmpDir != null)) result = mInternalXmpDir.getVisibility();

        isEmpty(result, ++i, null, null);
        return result;
    }

    @Override
    public IMetaApi setVisibility(VISIBILITY value) {
        throw new UnsupportedOperationException ();
    }

    /** return the image orinentation as id (one of the ORIENTATION_ROTATE_XXX constants) */
    private Integer getOrientationId() {
        init();
        return (mExifDir == null) ? null : mExifDir.getInteger(ExifDirectoryBase.TAG_ORIENTATION);
    }

    private static final int ORIENTATION_ROTATE_180 = 3;
    private static final int ORIENTATION_ROTATE_90 = 6;  // rotate 90 cw to right it
    private static final int ORIENTATION_ROTATE_270 = 8;  // rotate 270 to right it

    /** return image orinentation in degrees (0, 90,180,270) or 0 if inknown */
    public int getOrientationInDegrees() {
        Integer orientation = getOrientationId();
        if (orientation != null) {
            // We only recognize a subset of orientation tag values.
            switch (orientation.intValue()) {
                case ORIENTATION_ROTATE_90:
                    return 90;
                case ORIENTATION_ROTATE_180:
                    return 180;
                case ORIENTATION_ROTATE_270:
                    return 270;
                default:
            }
        }
        return 0;
    }



    @Override
    public void close() throws IOException {

    }

    @Override
    public String toString() {
        init();
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

        if (mInternalXmpDir != null) {
            mInternalXmpDir.appendXmp("xmp.", builder);
        }

        return builder.toString();
    }

    public MediaXmpSegment getImternalXmp() {
        init();
        return mInternalXmpDir;
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

    protected void init() {
        if (!mInitExecuted && (mMetadata != null)) {
            mInitExecuted = true;
            mExifDir = this.mMetadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            mExifSubDir = this.mMetadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

            mIptcDir = this.mMetadata.getFirstDirectoryOfType(IptcDirectory.class);
            // fileDir = this.mMetadata.getFirstDirectoryOfType(FileDirec .class); // not implemented
            mCommentDir = this.mMetadata.getFirstDirectoryOfType(JpegCommentDirectory.class);



            mExifGpsDir = null;
            GpsDirectory gps = this.mMetadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gps != null) {
                mExifGpsDir = gps.getGeoLocation();
                if ((mExifGpsDir != null) && mExifGpsDir.isZero()) mExifGpsDir = null;
            }

            mInternalXmpDir = null;
            XmpDirectory xmp = this.mMetadata.getFirstDirectoryOfType(XmpDirectory.class);
            if (xmp != null) {
                mInternalXmpDir = new MediaXmpSegment();
                mInternalXmpDir.setXmpMeta(xmp.getXMPMeta(), dbg_context + " embedded xml ");
            }
        }
    }

    protected String getString(String debugContext, Directory directory, int tagType) {
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

    protected List<String> getStringList(String debugContext, Directory directory, int tag) {
        String value = getString(debugContext, directory, tag);
        if (value != null) {
            Object[] split = value.split(";");
            return ListUtils.toStringList(split);
        }
        return null;
    }

}

/*
 * Copyright (c) 2016-2021 by k3b.
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

// import android.media.ExifInterface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import de.k3b.LibGlobal;
import de.k3b.io.ListUtils;
import de.k3b.io.StringUtils;
import de.k3b.io.VISIBILITY;
import de.k3b.io.filefacade.FileFacade;
import de.k3b.io.filefacade.IFile;
import de.k3b.media.MediaFormatter.FieldID;

/**
 * Thin Wrapper around Android-s ExifInterface to read/write exif data as {@link IPhotoProperties}
 * from jpg {@link java.io.File} or {@link IFile}
 * <p>
 * Created by k3b on 08.10.2016.
 */
public class ExifInterfaceEx extends ExifInterface
        implements IPhotoProperties, IPhotoPropertyFileWriter, IPhotoPropertyFileReader {
    private static final Logger logger = LoggerFactory.getLogger(LOG_TAG);

    private static final SimpleDateFormat sExifDateTimeFormatter;
    private static final String LIST_DELIMITER = ";";

    static {
        sExifDateTimeFormatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        sExifDateTimeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }


    /**
     * when xmp sidecar file was last modified or 0
     */
    private long filelastModified = 0;

    // false for unittests because UserComment = null is not implemented for COM - Marker
    protected static boolean useUserComment = true;

    private static Factory factory = new Factory() {
        @Override
        public ExifInterfaceEx create() {
            return new ExifInterfaceEx();
        }
    };
    /**
     * if not null content of xmp sidecar file
     */
    private IPhotoProperties xmpExtern = null;
    private String mDbg_context = null;

    /**
     * Prefer using one of the {@link #create()} methods instead
     */
    public ExifInterfaceEx() {
        try {
            loadAttributes(null, null, null, null, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setFactory(Factory factory) {
        ExifInterfaceEx.factory = factory;
    }

    /**
     * All instances of {@link ExifInterfaceEx} should be created through this factory method.
     * <p>
     * Use {@link #setFactory(Factory)} if you want to use a derived class
     * of {@link ExifInterfaceEx} globally.
     */
    public static ExifInterfaceEx create() {
        return factory.create();
    }

    public static ExifInterfaceEx create(IFile jpgFile, InputStream _in, IPhotoProperties xmpExtern, String dbg_context) throws IOException {
        InputStream in = (_in != null) ? _in : jpgFile.openInputStream();
        final String absolutePath = (jpgFile != null) ? jpgFile.getAbsolutePath() : null;
        return create().loadAttributes(in, jpgFile, absolutePath, xmpExtern, dbg_context);
    }

    public static ExifInterfaceEx create(String absoluteJpgPath, InputStream in, IPhotoProperties xmpExtern, String dbg_context) throws IOException {
        return create().loadAttributes(in, null, absoluteJpgPath, xmpExtern, dbg_context);
    }

    @Override
    protected void reset() {
        super.reset();
        filelastModified = 0;
        mDbg_context = null;
        xmpExtern = null;
        mLatitude = null;
        mLongitude = null;
    }

    /**
     * Reads Exif tags from the specified source.
     *
     * @param xmpExtern   if not null content of xmp sidecar file
     * @param dbg_context
     */
    private ExifInterfaceEx loadAttributes(InputStream in, IFile jpgFile, String absoluteJpgPath, IPhotoProperties xmpExtern, String dbg_context) throws IOException {
        super.loadAttributes(in, jpgFile, absoluteJpgPath);
        setFilelastModified(mExifFile);

        this.xmpExtern = xmpExtern;
        this.mDbg_context = dbg_context + "->ExifInterfaceEx(" + absoluteJpgPath + ") ";
        if (LibGlobal.debugEnabledJpgMetaIo) {
            logger.debug(this.mDbg_context +
                    " load: " + PhotoPropertiesFormatter.format(this, false, null, FieldID.path, FieldID.clasz));
        }
        // Log.d(LOG_TAG, msg);
        return this;
    }

    @Override
    public IPhotoProperties load(IFile jpgFile, IPhotoProperties childProperties, String dbg_context) {
        try {
            return create(jpgFile, null, childProperties, dbg_context);
        } catch (IOException e) {
            if (LibGlobal.debugEnabledJpgMetaIo) {
                logger.info(StringUtils.appendMessage(
                        dbg_context, getClass().getSimpleName(), "load failed",
                        jpgFile, e.getMessage()).toString(), e);
            }
            return null;
        }
    }

    public static int getOrientationId(IFile fullPath) {
        try {
            return ExifInterfaceEx.create(fullPath, null, null, "getOrientationId").getOrientationId();
        } catch (IOException e) {
        }
        return 0;
    }

    @Override
    public void saveAttributes(IFile inFile, IFile outFile, boolean deleteInFileOnFinish, Boolean hasXmp) throws IOException {
        fixDateTakenIfNeccessary(inFile);
        super.saveAttributes(inFile, outFile, deleteInFileOnFinish, hasXmp);
        setFilelastModified(outFile);
    }

    // Stores a new JPEG image with EXIF attributes into a given output stream.
    @Override
    public void saveJpegAttributes(InputStream inputStream, OutputStream outputStream, byte[] thumbnail)
            throws IOException {
        if (LibGlobal.debugEnabledJpg || LibGlobal.debugEnabledJpgMetaIo) {
            logger.debug(mDbg_context + " saveJpegAttributes: " + getPath());
        }
        super.saveJpegAttributes(inputStream, outputStream, thumbnail);
    }

    @Override
    protected boolean deleteFile(IFile file) {
        boolean result = super.deleteFile(file);
        if (result && LibGlobal.debugEnabledJpg || LibGlobal.debugEnabledJpgMetaIo) {
            logger.debug(mDbg_context + " deleteFile: " + file);
        }
        return result;
    }

    /**
     * @deprecated use {@link #fixDateTakenIfNeccessary(IFile)} instead
     */
    @Deprecated
    protected void fixDateTakenIfNeccessary(File inFile) {
        fixDateTakenIfNeccessary(FileFacade.convert("ExifInterfaceEx.fixDateTakenIfNeccessary", inFile));
    }

    protected void fixDateTakenIfNeccessary(IFile inFile) {
        // donot fix in unittests
        if (ExifInterfaceEx.fixDateOnSave && (null == getDateTimeTaken()) && (inFile != null)) {
            long lastModified = inFile.lastModified();
            // #29 set data if not in exif: date, make model
            if (lastModified != 0) {
                setDateTimeTaken(new Date(lastModified));
            }
        }
    }

    @Override
    protected void fixAttributes() {
        fixDateTakenIfNeccessary(mExifFile);

        if ((LibGlobal.appName != null) && (null == getAttribute(ExifInterfaceEx.TAG_MAKE))) {
            setAttribute(ExifInterfaceEx.TAG_MAKE, LibGlobal.appName);
        }

        if ((LibGlobal.appVersion != null) && (null == getAttribute(ExifInterfaceEx.TAG_MODEL))) {
            setAttribute(ExifInterfaceEx.TAG_MODEL, LibGlobal.appVersion);
        }
        super.fixAttributes();
    }

    @Override
    public String getPath() {
        return (mExifFile != null) ? mExifFile.getAbsolutePath() : null;
    }

    @Override
    public IPhotoProperties setPath(String filePath) {
        mExifFile = (filePath != null) ? FileFacade.convert("ExifInterfaceEx.setPath", filePath) : null;
        if (xmpExtern != null) xmpExtern.setPath(filePath);
        return this;
    }

    @Override
    public Date getDateTimeTaken(){
        int i=0;String debugContext = "getDateTimeTaken";

        Date result = null;
        if (isEmpty(result, ++i, debugContext, "Exif.DATETIME_ORIGINAL")) result = getAttributeDate(ExifInterfaceEx.TAG_DATETIME_ORIGINAL);
        if (isEmpty(result, ++i, debugContext, "Exif.DATETIME")) result = getAttributeDate(ExifInterfaceEx.TAG_DATETIME);
        if ((isEmpty(result, ++i, debugContext, "xmp.DateTimeTaken")) && (xmpExtern != null)) result = xmpExtern.getDateTimeTaken();
        isEmpty(result, ++i, null, null);
        return result;
    }

    @Override
    public ExifInterfaceEx setDateTimeTaken(Date value) {
        String dateInExifFormat = toExifDateTimeString(value);
        setAttribute(ExifInterfaceEx.TAG_DATETIME, dateInExifFormat);
        setAttribute(ExifInterfaceEx.TAG_DATETIME_ORIGINAL, dateInExifFormat);
        if (xmpExtern != null) xmpExtern.setDateTimeTaken(value);
        return this;
    }

    /**
     * returns ref for latitude which is S or N.
     * @return S or N
     */
    private String latitudeRef(Double latitude) {
        if (latitude == null) return null;
        return latitude<0.0d?"S":"N";
    }

    /**
     * returns ref for longitude which is W or E.
     * @return W or E
     */
    private String longitudeRef(Double longitude) {
        if (longitude == null) return null;
        return longitude<0.0d?"W":"E";
    }

    /**
     * convert latitude_longitude into DMS (degree minute second) format. For instance<br/>
     * -79.948862 becomes<br/>
     * -79 degrees, 56 minutes, 55903 millisecs (equals 55.903 seconds)
     *  79/1,56/1,55903/1000<br/>
     * It works for latitude and longitude<br/>
     * @param _latitude could be longitude.
     */
    private final String convert(Double _latitude) {
        if (_latitude == null) return null;

        double latitude=Math.abs(_latitude);
        int degree = (int) latitude;
        latitude *= 60;
        latitude -= (degree * 60.0d);
        int minute = (int) latitude;
        latitude *= 60;
        latitude -= (minute * 60.0d);
        int milliSecond = (int) (latitude*1000.0d);

        String sb = String.valueOf(degree) +
                "/1," +
                minute +
                "/1," +
                milliSecond +
                "/1000,";
        return sb;
    }

    private Double mLatitude = null;
    private Double mLongitude = null;
    /** latitude, in degrees north. (-90 .. +90); longitude, in degrees east.  (-180 .. + 180)    */
    @Override public IPhotoProperties setLatitudeLongitude(Double latitude, Double longitude) {
        setAttribute(ExifInterfaceEx.TAG_GPS_LATITUDE, convert(latitude));
        setAttribute(ExifInterfaceEx.TAG_GPS_LATITUDE_REF, latitudeRef(latitude));
        mLatitude = latitude;

        setAttribute(ExifInterfaceEx.TAG_GPS_LONGITUDE, convert(longitude));
        setAttribute(ExifInterfaceEx.TAG_GPS_LONGITUDE_REF, longitudeRef(longitude));
        mLongitude = longitude;

        if (xmpExtern != null) xmpExtern.setLatitudeLongitude(latitude, longitude);

        return this;
    }

    @Override
    public Double getLatitude() {
        int i=0;String debugContext = "getLatitude";

        Double result = null;
        if (isEmpty(result, ++i, debugContext, "Exif.Latitude")) {
            loadLatLon();
            result = this.mLatitude;
        }

        if ((isEmpty(result, ++i, debugContext, "xmp.Latitude")) && (xmpExtern != null)) return xmpExtern.getLatitude();

        isEmpty(result, ++i, null, null);
        return result;
    }

    @Override
    public Double getLongitude() {
        int i=0;String debugContext = "getLongitude";
        Double result = null;
        if (isEmpty(result, ++i, debugContext, "Exif.Longitude")) {
            loadLatLon();
            result = this.mLongitude;
        }

        if ((isEmpty(result, ++i, debugContext, "xmp.Longitude")) && (xmpExtern != null)) return xmpExtern.getLongitude();

        isEmpty(result, ++i, null, null);
        return result;
    }

    @Override
    public String getTitle() {
        int i=0;String debugContext = "getTitle";

        String result = null;
        if ((isEmpty(result, ++i, debugContext, "xmp.Title")) && (xmpExtern != null)) result = xmpExtern.getTitle();
        if (isEmpty(result, ++i, debugContext, "Exif.XPTITLE")) result = getAttribute(TAG_WIN_TITLE);
        // iptc:Headline
        isEmpty(result, ++i, null, null);
        return result;
    }

    @Override
    public IPhotoProperties setTitle(String value) {
        setAttribute(TAG_WIN_TITLE, value);
        if (xmpExtern != null) xmpExtern.setTitle(value);
        return this;
    }

    /** not implemented in {@link ExifInterface} */
    @Override
    public String getDescription() {
        int i=0;String debugContext = "getDescription";

        String result = null;
        if (isEmpty(result, ++i, debugContext, "Exif.IMAGE_DESCRIPTION")) result = getAttribute(TAG_IMAGE_DESCRIPTION);

        // XMP-dc:Description
        if (isEmpty(result, ++i, debugContext, "xmp.Description") && (xmpExtern != null)) result = xmpExtern.getDescription();

        if (isEmpty(result, ++i, debugContext, "Exif.XPSUBJECT")) result = getAttribute(TAG_WIN_SUBJECT);

        if (isEmpty(result, ++i, debugContext, "Exif.XPCOMMENT")) result = getAttribute(TAG_WIN_COMMENT);

        // NOTE: write not fully supported for TAG_USER_COMMENT in tiff-com-segment
        if (useUserComment && isEmpty(result, ++i, debugContext, "Exif.USER_COMMENT")) result = getAttribute(TAG_USER_COMMENT);

        // iptc:Caption-Abstract

        isEmpty(result, ++i, null, null);
        return result;
    }

    protected boolean isEmpty(Object result, int tryNumber, String debugContext, String debugFieldName) {
        return (result == null); // || (result.length() == 0);
    }

    /** not implemented in {@link ExifInterface} */
    @Override
    public IPhotoProperties setDescription(String value) {
        setAttribute(TAG_IMAGE_DESCRIPTION, value);
        setAttribute(TAG_WIN_SUBJECT, value);

        if (xmpExtern != null) xmpExtern.setDescription(value);
        setAttribute(TAG_WIN_COMMENT, value);

        // NOTE: write not fully supported for TAG_USER_COMMENT in tiff-com-segment
        if (useUserComment) setAttribute(TAG_USER_COMMENT, value);
        return this;
    }

    /** not implemented in {@link ExifInterface} */
    @Override
    public List<String> getTags() {
        int i=0;String debugContext = "getTags";

        List<String> result = null;
        if (isEmpty(result, ++i, debugContext, "xmp.Tags") && (xmpExtern != null)) result = xmpExtern.getTags();
        if (isEmpty(result, ++i, debugContext, "Exif.XPKEYWORDS") || (result.size() == 0)) {
            result = getTagsInternal();
        }
        isEmpty(result, ++i, null, null);
        return result;
    }

    private List<String> getTagsInternal() {
        String s = getAttribute(TAG_WIN_KEYWORDS);
        if (s != null) return ListUtils.fromString(s, LIST_DELIMITER);
        return null;
    }

    /** not implemented in {@link ExifInterface} */
    @Override
    public IPhotoProperties setTags(List<String> value) {
        setAttribute(TAG_WIN_KEYWORDS, (value == null) ? null : ListUtils.toString(LIST_DELIMITER, value));
        if (xmpExtern != null) xmpExtern.setTags(value);
        return this;
    }

    /** not implemented in {@link ExifInterface} */
    @Override
    public Integer getRating() {
        int i=0;String debugContext = "getRating";
        Integer result = null;
        if (isEmpty(result, ++i, debugContext, "xmp.Rating") && (xmpExtern != null)) result = xmpExtern.getRating();
        if (isEmpty(result, ++i, debugContext, "Exif.XPRATING")) {
            int r = getAttributeInt(TAG_WIN_RATING, -1);
            if (r != -1) result = Integer.valueOf(r);
        }
        isEmpty(result, ++i, null, null);
        return result;
    }

    /**
     * not implemented in {@link ExifInterface}
     */
    @Override
    public IPhotoProperties setRating(Integer value) {
        setAttribute(TAG_WIN_RATING, (value != null) ? value.toString() : null);
        if (xmpExtern != null) xmpExtern.setRating(value);
        return this;
    }

    public interface Factory {
        public ExifInterfaceEx create();
    }

    /**
     * return the image orinentation as id (one of the ORIENTATION_ROTATE_XXX constants)
     */
    public int getOrientationId() {
        return getAttributeInt(
                ExifInterfaceEx.TAG_ORIENTATION, 0);
    }

    /** return image orinentation in degrees (0, 90,180,270) or 0 if inknown */
    public int getOrientationInDegrees() {
        int orientationId = getOrientationId();
        return PhotoPropertiesUtil.exifOrientationCode2RotationDegrees(orientationId, orientationId);
    }

    protected Date getAttributeDate(String tag) {
        String dateTimeString =  this.getAttribute(tag);

        if (dateTimeString == null) return null;

        ParsePosition pos = new ParsePosition(0);
        try {
            return sExifDateTimeFormatter.parse(dateTimeString, pos);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    protected static String toExifDateTimeString(Date value) {
        final String exifDate = (value != null) ? sExifDateTimeFormatter.format(value) : null;
        return exifDate;
    }

    private void loadLatLon() {
        if ((this.mLatitude == null) || (this.mLongitude == null)) {
            float[] latlng = new float[2];
            if (getLatLong(latlng)) {
                mLatitude = Double.valueOf(latlng[0]);
                mLongitude = Double.valueOf(latlng[1]);
            }
        }
    }

    /**
     * @deprecated use {@link #setFilelastModified(IFile)} instead
     */
    @Deprecated
    public void setFilelastModified(File file) {
        setFilelastModified(FileFacade.convert("ExifInterfaceEx.setFilelastModified", file));
    }

    /** when xmp sidecar file was last modified or 0 */
    public void setFilelastModified(IFile file) {
        if (file != null) this.filelastModified = file.lastModified();
    }

    /** when xmp sidecar file was last modified or 0 */
    public long getFilelastModified() {
        return filelastModified;
    }

    public VISIBILITY getVisibility() {
        int i=0;String debugContext = "getVisibility";
        VISIBILITY result = null;
        if (isEmpty(result, ++i, debugContext, "Exif.XPKEYWORDS(PRIVATE)")) result = VISIBILITY.getVisibility(getTagsInternal());
        if (isEmpty(result, ++i, debugContext, "xmp.apm.visibility") && (this.xmpExtern != null)) result = this.xmpExtern.getVisibility();
        return result;
    }

    public IPhotoProperties setVisibility(VISIBILITY visibility) {
        // exif does not support Visibility itseltf
        if (this.xmpExtern != null) this.xmpExtern.setVisibility(visibility);
        if (VISIBILITY.isChangingValue(visibility)) {
            List<String> tags = VISIBILITY.setPrivate(getTags(), visibility);
            if (tags != null) setTags(tags);
        }
        return this;
    }

    @Override
    public String toString() {
        return PhotoPropertiesFormatter.format(this).toString();
    }

}

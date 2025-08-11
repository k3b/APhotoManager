package de.k3b.android.androFotoFinder.media;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import de.k3b.io.StringUtils;
import de.k3b.io.filefacade.FileFacade;
import de.k3b.io.filefacade.IFile;
import de.k3b.media.ExifInterfaceFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import de.k3b.LibGlobal;
import de.k3b.io.ListUtils;
import de.k3b.io.VISIBILITY;
import de.k3b.media.ExifInterface;
import de.k3b.media.ExifInterfaceEx;
import de.k3b.media.ExifInterfaceExImpl;
import de.k3b.media.ExifInterfaceFactory;
import de.k3b.media.IPhotoProperties;
import de.k3b.media.MediaFormatter;
import de.k3b.media.PhotoPropertiesFormatter;
import de.k3b.media.PhotoPropertiesUtil;
import io.github.tommygeenexus.exifinterfaceextended.ExifInterfaceExtended;

public class ExifInterfaceExAndroidImpl extends ExifInterfaceExtended implements ExifInterfaceEx{
    // public to allow error filtering
    public static final String LOG_TAG = "ExifInterface";

    // false for unittests because UserComment = null is not implemented for COM - Marker
    protected static boolean fixDateOnSave = true;

    private static final Logger logger = LoggerFactory.getLogger(LOG_TAG);

    private static final SimpleDateFormat sExifDateTimeFormatter;
    private static final String LIST_DELIMITER = ";";

    /** factory to create an ExifInterface from file or stream */
    private static ExifInterfaceFactory factory = null;

    static {
        sExifDateTimeFormatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        sExifDateTimeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }


    // false for unittests because UserComment = null is not implemented for COM - Marker
    protected static boolean useUserComment = true;

    private final String mDbg_context;
    /** if not null content of xmp sidecar file */
    private final IPhotoProperties xmpExtern;

    // content of file.lastModified used if there is no exif-date
    private long initialLastModified = 0;

    File mExifFile = null;

    /**
     * Reads Exif info from the specified JPEG file or inputstream.
     * @param absoluteJpgPath null or full path of the file where exif comes from
     * @param in null or inputstream where exif comes from
     * @param xmpExtern if not null content of extern xmp sidecar file
     * @param dbgContext String added to the debug-output
     */
    public ExifInterfaceExAndroidImpl(String absoluteJpgPath, InputStream in, IPhotoProperties xmpExtern, String dbgContext) throws IOException {
        // super(absoluteJpgPath, in, xmpExtern, dbgContext);
        super(absoluteJpgPath, in);

        this.xmpExtern = xmpExtern;
        this.mDbg_context = dbgContext + "->ExifInterfaceEx(" + absoluteJpgPath+ ") ";
        if (absoluteJpgPath != null) {
            this.initialLastModified = new File(absoluteJpgPath).lastModified();
        }
        setPath(absoluteJpgPath);
        if (LibGlobal.debugEnabledJpgMetaIo) {
            logger.debug(this.mDbg_context +
                    " load: " + PhotoPropertiesFormatter.format(this, false, null, MediaFormatter.FieldID.path, MediaFormatter.FieldID.clasz));
        }
        // Log.d(LOG_TAG, msg);

    }

    /** factory to create an ExifInterface from file or stream */
    public static ExifInterfaceFactory factory() {
        if (factory == null) {
            factory = new ExifInterfaceFactory() {
                @Override
                public ExifInterfaceEx createExifInterface() {
                    return null;
                }

                /** {@inheritDoc} */
                // @Override
                public ExifInterfaceEx createExifInterface(String absoluteJpgPath, InputStream in, IPhotoProperties xmpExtern, String dbg_context) throws IOException {
                    return new ExifInterfaceExAndroidImpl(absoluteJpgPath, in, xmpExtern, dbg_context);
                }
            };
        }
        return factory;
    }

    @Override
    public void saveAttributes() throws IOException {
        fixDateTakenIfNeccessary();
        super.saveAttributes();
    }

    /**
     * {@inheritDoc}
     * Read the image without tag data from inFile and save image plus tag data into the outFile image file.
     * <p>
     * This method is supported for JPEG, PNG, and WebP formats.
     * <p class="note">
     * Note: after calling this method, any attempts to obtain range information
     * from {@link #getAttributeRange(String)} or {@link #getThumbnailRange()}
     * will throw {@link IllegalStateException}, since the offsets may have
     * changed in the newly written file.
     * <p>
     * For WebP format, the Exif data will be stored as an Extended File Format, and it may not be
     * supported for older readers.
     * <p>
     * For PNG format, the Exif data will be stored as an "eXIf" chunk as per
     * "Extensions to the PNG 1.2 Specification, Version 1.5.0".
     */
    @Override
    public void saveAttributes(IFile inFile, IFile outFile, boolean deleteInFileOnFinish, Boolean hasXmp) throws IOException {
        if (inFile == null || outFile == null || inFile.equals(outFile)) {
            throw new IOException(
                    "ExifInterface does not support saving attributes for the current input.");
        }
        try {
            fixDateTakenIfNeccessary();

            super.saveAttributes(inFile.openInputStream(), outFile.openOutputStream());
            if (deleteInFileOnFinish) {
                inFile.delete();
            }
        } catch (Exception ex) {
            outFile.delete();
        }
    }

    private void fixDateTakenIfNeccessary() {
        // donot fix in unittests
        if (fixDateOnSave && (null == getDateTimeTaken()) && (this.initialLastModified != 0)) {
            // #29 set data if not in exif: date, make model
            setDateTimeTaken(new Date(this.initialLastModified));
        }
    }

    protected void fixAttributes() {
        fixDateTakenIfNeccessary();

        if ((LibGlobal.appName != null) && (null == getAttribute(TAG_MAKE))) {
            setAttribute(TAG_MAKE, LibGlobal.appName);
        }

        if ((LibGlobal.appVersion != null) && (null == getAttribute(TAG_MODEL))) {
            setAttribute(TAG_MODEL, LibGlobal.appVersion);
        }
    }

    @Override
    public String getPath() {
        return ( mExifFile != null) ? mExifFile.getAbsolutePath() : null;
    }

    @Override
    public IPhotoProperties setPath(String filePath) {
        mExifFile = (filePath != null) ? new File(filePath) : null;
        if (xmpExtern != null) xmpExtern.setPath(filePath);
        return this;
    }

    @Override
    public Date getDateTimeTaken(){
        int i=0;String debugContext = "getDateTimeTaken";

        Date result = null;
        if (isEmpty(result, ++i, debugContext, "Exif.DATETIME_ORIGINAL")) result = getAttributeDate(TAG_DATETIME_ORIGINAL);
        if (isEmpty(result, ++i, debugContext, "Exif.DATETIME")) result = getAttributeDate(TAG_DATETIME);
        if ((isEmpty(result, ++i, debugContext, "xmp.DateTimeTaken")) && (xmpExtern != null)) result = xmpExtern.getDateTimeTaken();
        isEmpty(result, ++i, null, null);
        return result;
    }

    @Override
    public ExifInterfaceEx setDateTimeTaken(Date value) {
        String dateInExifFormat = toExifDateTimeString(value);
        setAttribute(TAG_DATETIME, dateInExifFormat);
        setAttribute(TAG_DATETIME_ORIGINAL, dateInExifFormat);
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
        setAttribute(TAG_GPS_LATITUDE, convert(latitude));
        setAttribute(TAG_GPS_LATITUDE_REF, latitudeRef(latitude));
        mLatitude = latitude;

        setAttribute(TAG_GPS_LONGITUDE, convert(longitude));
        setAttribute(TAG_GPS_LONGITUDE_REF, longitudeRef(longitude));
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

    /** not implemented in {@link ExifInterface} */
    @Override
    public IPhotoProperties setRating(Integer value) {
        setAttribute(TAG_WIN_RATING, (value != null) ? value.toString() : null);
        if (xmpExtern != null) xmpExtern.setRating(value);
        return this;
    }

    /**
     * {@inheritDoc}
     * @param inputStream
     * @param outputStream
     * @param thumbnail
     * @throws IOException
     */
    @Override
    public void saveJpegAttributes(InputStream inputStream, OutputStream outputStream, byte[] thumbnail) throws IOException {
        saveAttributes(inputStream,outputStream);
    }

    /** return the image orinentation as id (one of the ORIENTATION_ROTATE_XXX constants) */
    public int getOrientationId() {
        return getAttributeInt(
                TAG_ORIENTATION, 0);
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

    /** false means this is no valid jpg format */
    @Override
    public boolean isValidJpgExifFormat() {return this.hasAttributes(true);}

    @Override
    public String toString() {
        return PhotoPropertiesFormatter.format(this).toString();
    }


    /**
     * {@inheritDoc}
     * implements interface {link IPhotoPropertyFileReader}
     */
    @Override
    public IPhotoProperties load(IFile jpgFile, IPhotoProperties childProperties, String dbg_context) {
        try {
            final String absolutePath = (jpgFile != null) ? jpgFile.getAbsolutePath() : null;
            return loadAttributes(null, jpgFile, absolutePath, childProperties, dbg_context);
        } catch (IOException e) {
            if (LibGlobal.debugEnabledJpgMetaIo) {
                logger.info(StringUtils.appendMessage(
                        dbg_context, getClass().getSimpleName(), "load failed",
                        jpgFile, e.getMessage()).toString(), e);
            }
            return null;
        }
    }

    /**
     * Reads Exif tags from the specified source.
     *
     * @param in              if not null: input stream where data comes from
     * @param jpgFile         if not null: input IFile where data comes from
     * @param absoluteJpgPath
     * @param xmpExtern       if not null content of xmp sidecar file
     * @param dbg_context     info for debug log why attributes are loaded.
     * @return
     * @throws IOException
     */
    @Override
    public ExifInterfaceEx loadAttributes(InputStream in, IFile jpgFile, String absoluteJpgPath, IPhotoProperties xmpExtern, String dbg_context) throws IOException {
        IFile mExifFile = (jpgFile != null)
                ? jpgFile
                : (absoluteJpgPath != null)
                ? FileFacade.convert(dbg_context, absoluteJpgPath)
                : null;

        setPath(absoluteJpgPath);
        if (in != null) {
            loadAttributes(in);
            return this;
        } else if (mExifFile != null) {
            loadAttributes(mExifFile.openInputStream());
            return this;
        }
        return null;
    }


}

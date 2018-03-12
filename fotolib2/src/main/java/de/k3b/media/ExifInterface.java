// ExifInterface source code from android-6 - special version without jni
/*
 * Copyright (C) 2007 The Android Open Source Project under the Apache License, Version 2.0
 * Copyright (C) 2016-2018 by k3b under the GPL-v3+.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a class for reading and writing Exif tags in a JPEG file.
 * It is based on ExifInterface of android-6 version.
 *
 * Improvements:
 *  * with dependencies to android removed
 *  * added microsoft exiftags: TAG_WIN_xxxxx
 */
public class ExifInterface {
    // public to allow error filtering
    public static final String LOG_TAG = "ExifInterface";

    private static final Logger logger = LoggerFactory.getLogger(LOG_TAG);

    private static final boolean DEBUG_INTERNAL = false;

    // public to allow global settings to enable/disable
    public static boolean DEBUG = false;

    // false for unittests because UserComment = null is not implemented for COM - Marker
    protected static boolean fixDateOnSave = true;

    // The Exif tag names
    /** Type is String. */
    public static final String TAG_ARTIST = "Artist";
    /** Type is int. @hide */
    public static final String TAG_BITS_PER_SAMPLE = "BitsPerSample";
    /** Type is int. @hide */
    public static final String TAG_COMPRESSION = "Compression";
    /** Type is String.  */
    public static final String TAG_COPYRIGHT = "Copyright";
    /** Type is String. @hide */
    public static final String TAG_DATETIME = "DateTime";
    /** Type is String.  */
    public static final String TAG_IMAGE_DESCRIPTION = "ImageDescription";
    /** Type is int. */
    public static final String TAG_IMAGE_LENGTH = "ImageLength";
    /** Type is int. */
    public static final String TAG_IMAGE_WIDTH = "ImageWidth";
    /** Type is int. @hide */
    public static final String TAG_JPEG_INTERCHANGE_FORMAT = "JPEGInterchangeFormat";
    /** Type is int. @hide */
    public static final String TAG_JPEG_INTERCHANGE_FORMAT_LENGTH = "JPEGInterchangeFormatLength";
    /** Type is String. */
    public static final String TAG_MAKE = "Make";
    /** Type is String. */
    public static final String TAG_MODEL = "Model";
    /** Type is int. */
    public static final String TAG_ORIENTATION = "Orientation";
    /** Type is int. @hide */
    public static final String TAG_PHOTOMETRIC_INTERPRETATION = "PhotometricInterpretation";
    /** Type is int. @hide */
    public static final String TAG_PLANAR_CONFIGURATION = "PlanarConfiguration";
    /** Type is rational. @hide */
    public static final String TAG_PRIMARY_CHROMATICITIES = "PrimaryChromaticities";
    /** Type is rational. @hide */
    public static final String TAG_REFERENCE_BLACK_WHITE = "ReferenceBlackWhite";
    /** Type is int. @hide */
    public static final String TAG_RESOLUTION_UNIT = "ResolutionUnit";
    /** Type is int. @hide */
    public static final String TAG_ROWS_PER_STRIP = "RowsPerStrip";
    /** Type is int. @hide */
    public static final String TAG_SAMPLES_PER_PIXEL = "SamplesPerPixel";
    /** Type is String.  */
    public static final String TAG_SOFTWARE = "Software";
    /** Type is int. @hide */
    public static final String TAG_STRIP_BYTE_COUNTS = "StripByteCounts";
    /** Type is int. @hide */
    public static final String TAG_STRIP_OFFSETS = "StripOffsets";
    /** Type is int. @hide */
    public static final String TAG_TRANSFER_FUNCTION = "TransferFunction";
    /** Type is rational. @hide */
    public static final String TAG_WHITE_POINT = "WhitePoint";
    /** Type is rational. @hide */
    public static final String TAG_X_RESOLUTION = "XResolution";
    /** Type is rational. @hide */
    public static final String TAG_Y_CB_CR_COEFFICIENTS = "YCbCrCoefficients";
    /** Type is int. @hide */
    public static final String TAG_Y_CB_CR_POSITIONING = "YCbCrPositioning";
    /** Type is int. @hide */
    public static final String TAG_Y_CB_CR_SUB_SAMPLING = "YCbCrSubSampling";
    /** Type is rational. @hide */
    public static final String TAG_Y_RESOLUTION = "YResolution";
    /** Type is rational.  */
    public static final String TAG_APERTURE_VALUE = "ApertureValue";
    /** Type is rational. @hide */
    public static final String TAG_BRIGHTNESS_VALUE = "BrightnessValue";
    /** Type is String. @hide */
    public static final String TAG_CFA_PATTERN = "CFAPattern";
    /** Type is int. @hide */
    public static final String TAG_COLOR_SPACE = "ColorSpace";
    /** Type is String. @hide */
    public static final String TAG_COMPONENTS_CONFIGURATION = "ComponentsConfiguration";
    /** Type is rational. @hide */
    public static final String TAG_COMPRESSED_BITS_PER_PIXEL = "CompressedBitsPerPixel";
    /** Type is int. @hide */
    public static final String TAG_CONTRAST = "Contrast";
    /** Type is int. @hide */
    public static final String TAG_CUSTOM_RENDERED = "CustomRendered";
    /** Type is String. */
    public static final String TAG_DATETIME_DIGITIZED = "DateTimeDigitized";
    /** Type is String.  */
    public static final String TAG_DATETIME_ORIGINAL = "DateTimeOriginal";
    /** Type is String. @hide */
    public static final String TAG_DEVICE_SETTING_DESCRIPTION = "DeviceSettingDescription";
    /** Type is double. @hide */
    public static final String TAG_DIGITAL_ZOOM_RATIO = "DigitalZoomRatio";
    /** Type is String. @hide */
    public static final String TAG_EXIF_VERSION = "ExifVersion";
    /** Type is double. @hide */
    public static final String TAG_EXPOSURE_BIAS_VALUE = "ExposureBiasValue";
    /** Type is rational. @hide */
    public static final String TAG_EXPOSURE_INDEX = "ExposureIndex";
    /** Type is int. @hide */
    public static final String TAG_EXPOSURE_MODE = "ExposureMode";
    /** Type is int. @hide */
    public static final String TAG_EXPOSURE_PROGRAM = "ExposureProgram";
    /** Type is double. */
    public static final String TAG_EXPOSURE_TIME = "ExposureTime";
    /** Type is double. */
    public static final String TAG_APERTURE = "FNumber";
    /** Type is String.  @hide */
    public static final String TAG_FILE_SOURCE = "FileSource";
    /** Type is int. */
    public static final String TAG_FLASH = "Flash";
    /** Type is rational. @hide */
    public static final String TAG_FLASH_ENERGY = "FlashEnergy";
    /** Type is String. @hide */
    public static final String TAG_FLASHPIX_VERSION = "FlashpixVersion";
    /** Type is rational. */
    public static final String TAG_FOCAL_LENGTH = "FocalLength";
    /** Type is int. @hide */
    public static final String TAG_FOCAL_LENGTH_IN_35MM_FILM = "FocalLengthIn35mmFilm";
    /** Type is int. @hide */
    public static final String TAG_FOCAL_PLANE_RESOLUTION_UNIT = "FocalPlaneResolutionUnit";
    /** Type is rational. @hide */
    public static final String TAG_FOCAL_PLANE_X_RESOLUTION = "FocalPlaneXResolution";
    /** Type is rational. @hide */
    public static final String TAG_FOCAL_PLANE_Y_RESOLUTION = "FocalPlaneYResolution";
    /** Type is int. @hide */
    public static final String TAG_GAIN_CONTROL = "GainControl";
    /** Type is int. */
    public static final String TAG_ISO = "ISOSpeedRatings";
    /** Type is String.  */
    public static final String TAG_IMAGE_UNIQUE_ID = "ImageUniqueID";
    /** Type is int. @hide */
    public static final String TAG_LIGHT_SOURCE = "LightSource";
    /** Type is String. @hide */
    public static final String TAG_MAKER_NOTE = "MakerNote";
    /** Type is rational. @hide */
    public static final String TAG_MAX_APERTURE_VALUE = "MaxApertureValue";
    /** Type is int. @hide */
    public static final String TAG_METERING_MODE = "MeteringMode";
    /** Type is String. @hide */
    public static final String TAG_OECF = "OECF";
    /** Type is int. @hide */
    public static final String TAG_PIXEL_X_DIMENSION = "PixelXDimension";
    /** Type is int. @hide */
    public static final String TAG_PIXEL_Y_DIMENSION = "PixelYDimension";
    /** Type is String. @hide */
    public static final String TAG_RELATED_SOUND_FILE = "RelatedSoundFile";
    /** Type is int. @hide */
    public static final String TAG_SATURATION = "Saturation";
    /** Type is int. @hide */
    public static final String TAG_SCENE_CAPTURE_TYPE = "SceneCaptureType";
    /** Type is String. @hide */
    public static final String TAG_SCENE_TYPE = "SceneType";
    /** Type is int. @hide */
    public static final String TAG_SENSING_METHOD = "SensingMethod";
    /** Type is int. @hide */
    public static final String TAG_SHARPNESS = "Sharpness";
    /** Type is rational. @hide */
    public static final String TAG_SHUTTER_SPEED_VALUE = "ShutterSpeedValue";
    /** Type is String. @hide */
    public static final String TAG_SPATIAL_FREQUENCY_RESPONSE = "SpatialFrequencyResponse";
    /** Type is String. @hide */
    public static final String TAG_SPECTRAL_SENSITIVITY = "SpectralSensitivity";
    /** Type is String. */
    public static final String TAG_SUBSEC_TIME = "SubSecTime";
    /** Type is String. */
    public static final String TAG_SUBSEC_TIME_DIG = "SubSecTimeDigitized";
    /** Type is String. */
    public static final String TAG_SUBSEC_TIME_ORIG = "SubSecTimeOriginal";
    /** Type is int. @hide */
    public static final String TAG_SUBJECT_AREA = "SubjectArea";
    /** Type is double. @hide */
    public static final String TAG_SUBJECT_DISTANCE = "SubjectDistance";
    /** Type is int. @hide */
    public static final String TAG_SUBJECT_DISTANCE_RANGE = "SubjectDistanceRange";
    /** Type is int. @hide */
    public static final String TAG_SUBJECT_LOCATION = "SubjectLocation";
    /** Type is String. r/w mExifDir[TAG_USER_COMMENT] else ro from mCommentDir[TAG_COMMENT] */
    public static final String TAG_USER_COMMENT = "UserComment";
    /** Type is int. */
    public static final String TAG_WHITE_BALANCE = "WhiteBalance";
    /**
     * The altitude (in meters) based on the reference in TAG_GPS_ALTITUDE_REF.
     * Type is rational.
     */
    public static final String TAG_GPS_ALTITUDE = "GPSAltitude";
    /**
     * 0 if the altitude is above sea level. 1 if the altitude is below sea
     * level. Type is int.
     */
    public static final String TAG_GPS_ALTITUDE_REF = "GPSAltitudeRef";
    /** Type is String. @hide */
    public static final String TAG_GPS_AREA_INFORMATION = "GPSAreaInformation";
    /** Type is rational. @hide */
    public static final String TAG_GPS_DOP = "GPSDOP";
    /** Type is String. */
    public static final String TAG_GPS_DATESTAMP = "GPSDateStamp";
    /** Type is rational.  */
    public static final String TAG_GPS_DEST_BEARING = "GPSDestBearing";
    /** Type is String.  */
    public static final String TAG_GPS_DEST_BEARING_REF = "GPSDestBearingRef";
    /** Type is rational. @hide */
    public static final String TAG_GPS_DEST_DISTANCE = "GPSDestDistance";
    /** Type is String. @hide */
    public static final String TAG_GPS_DEST_DISTANCE_REF = "GPSDestDistanceRef";
    /** Type is rational. @hide */
    public static final String TAG_GPS_DEST_LATITUDE = "GPSDestLatitude";
    /** Type is String. @hide */
    public static final String TAG_GPS_DEST_LATITUDE_REF = "GPSDestLatitudeRef";
    /** Type is rational. @hide */
    public static final String TAG_GPS_DEST_LONGITUDE = "GPSDestLongitude";
    /** Type is String. @hide */
    public static final String TAG_GPS_DEST_LONGITUDE_REF = "GPSDestLongitudeRef";
    /** Type is int. @hide */
    public static final String TAG_GPS_DIFFERENTIAL = "GPSDifferential";
    /** Type is rational.  */
    public static final String TAG_GPS_IMG_DIRECTION = "GPSImgDirection";
    /** Type is String.  */
    public static final String TAG_GPS_IMG_DIRECTION_REF = "GPSImgDirectionRef";
    /** Type is rational. Format is "num1/denom1,num2/denom2,num3/denom3". */
    public static final String TAG_GPS_LATITUDE = "GPSLatitude";
    /** Type is String. */
    public static final String TAG_GPS_LATITUDE_REF = "GPSLatitudeRef";
    /** Type is rational. Format is "num1/denom1,num2/denom2,num3/denom3". */
    public static final String TAG_GPS_LONGITUDE = "GPSLongitude";
    /** Type is String. */
    public static final String TAG_GPS_LONGITUDE_REF = "GPSLongitudeRef";
    /** Type is String. @hide */
    public static final String TAG_GPS_MAP_DATUM = "GPSMapDatum";
    /** Type is String. @hide */
    public static final String TAG_GPS_MEASURE_MODE = "GPSMeasureMode";
    /** Type is String. Name of GPS processing method used for location finding. */
    public static final String TAG_GPS_PROCESSING_METHOD = "GPSProcessingMethod";
    /** Type is String. @hide */
    public static final String TAG_GPS_SATELLITES = "GPSSatellites";
    /** Type is rational. @hide */
    public static final String TAG_GPS_SPEED = "GPSSpeed";
    /** Type is String. @hide */
    public static final String TAG_GPS_SPEED_REF = "GPSSpeedRef";
    /** Type is String. @hide */
    public static final String TAG_GPS_STATUS = "GPSStatus";
    /** Type is String. Format is "hh:mm:ss". */
    public static final String TAG_GPS_TIMESTAMP = "GPSTimeStamp";
    /** Type is rational. @hide */
    public static final String TAG_GPS_TRACK = "GPSTrack";
    /** Type is String. @hide */
    public static final String TAG_GPS_TRACK_REF = "GPSTrackRef";
    /** Type is String. @hide */
    public static final String TAG_GPS_VERSION_ID = "GPSVersionID";
    /** Type is String. @hide */
    public static final String TAG_INTEROPERABILITY_INDEX = "InteroperabilityIndex";
    /** Type is int. @hide */
    public static final String TAG_THUMBNAIL_IMAGE_LENGTH = "ThumbnailImageLength";
    /** Type is int. @hide */
    public static final String TAG_THUMBNAIL_IMAGE_WIDTH = "ThumbnailImageWidth";
    // Private tags used for pointing the other IFD offset. The types of the following tags are int.
    private static final String TAG_EXIF_IFD_POINTER = "ExifIFDPointer";
    private static final String TAG_GPS_INFO_IFD_POINTER = "GPSInfoIFDPointer";
    private static final String TAG_INTEROPERABILITY_IFD_POINTER = "InteroperabilityIFDPointer";
    // Private tags used for thumbnail information.
    private static final String TAG_HAS_THUMBNAIL = "HasThumbnail";
    private static final String TAG_THUMBNAIL_OFFSET = "ThumbnailOffset";
    private static final String TAG_THUMBNAIL_LENGTH = "ThumbnailLength";
    private static final String TAG_THUMBNAIL_DATA = "ThumbnailData";

    // from http://www.exiv2.org/tags.html
    /** The image title, as used by Windows XP. */
    public static final String TAG_WIN_TITLE = "XPTitle";
    /** The image comment, as used by Windows XP. */
    public static final String TAG_WIN_COMMENT = "XPComment";
    /** The image author, as used by Windows XP (called Artist in the Windows shell). */
    public static final String TAG_WIN_AUTHOR = "XPAuthor";
    /** The image keywords, as used by Windows XP. */
    public static final String TAG_WIN_KEYWORDS = "XPKeywords";
    /** The image subject, as used by Windows XP. */
    public static final String TAG_WIN_SUBJECT = "XPSubject";
    /** The image subject, as used by Windows XP. */
    public static final String TAG_WIN_RATING = "Rating";

    // Constants used for the Orientation Exif tag.
    public static final int ORIENTATION_UNDEFINED = 0;
    public static final int ORIENTATION_NORMAL = 1;
    public static final int ORIENTATION_FLIP_HORIZONTAL = 2;  // left right reversed mirror
    public static final int ORIENTATION_ROTATE_180 = 3;
    public static final int ORIENTATION_FLIP_VERTICAL = 4;  // upside down mirror
    // flipped about top-left <--> bottom-right axis
    public static final int ORIENTATION_TRANSPOSE = 5;
    public static final int ORIENTATION_ROTATE_90 = 6;  // rotate 90 cw to right it
    // flipped about top-right <--> bottom-left axis
    public static final int ORIENTATION_TRANSVERSE = 7;
    public static final int ORIENTATION_ROTATE_270 = 8;  // rotate 270 to right it
    // Constants used for white balance
    public static final int WHITEBALANCE_AUTO = 0;
    public static final int WHITEBALANCE_MANUAL = 1;
    private static SimpleDateFormat sFormatter;
    // See Exchangeable image file format for digital still cameras: Exif version 2.2.
    // The following values are for parsing EXIF data area. There are tag groups in EXIF data area.
    // They are called "Image File Directory". They have multiple data formats to cover various
    // image metadata from GPS longitude to camera model name.
    // Types of Exif byte alignments (see JEITA CP-3451 page 10)
    private static final short BYTE_ALIGN_II = 0x4949;  // II: Intel order
    private static final short BYTE_ALIGN_MM = 0x4d4d;  // MM: Motorola order
    // Formats for the value in IFD entry (See TIFF 6.0 spec Types page 15).
    private static final int IFD_FORMAT_BYTE = 1;
    private static final int IFD_FORMAT_STRING = 2;
    private static final int IFD_FORMAT_USHORT = 3;
    private static final int IFD_FORMAT_ULONG = 4;
    private static final int IFD_FORMAT_URATIONAL = 5;
    private static final int IFD_FORMAT_SBYTE = 6;
    private static final int IFD_FORMAT_UNDEFINED = 7;
    private static final int IFD_FORMAT_SSHORT = 8;
    private static final int IFD_FORMAT_SLONG = 9;
    private static final int IFD_FORMAT_SRATIONAL = 10;
    private static final int IFD_FORMAT_SINGLE = 11;
    private static final int IFD_FORMAT_DOUBLE = 12;
    private static final int IFD_FORMAT_UCS2LE_STRING = 13; // user defined encoded as IFD_FORMAT_BYTE = +1
    private static final int IFD_FORMAT_PREFIX_STRING = 14; // user defined encoded as IFD_FORMAT_BYTE = +1
    // Names for the data formats for debugging purpose.
    private static final String[] IFD_FORMAT_NAMES = new String[] {
            "", "BYTE", "STRING", "USHORT", "ULONG", "URATIONAL", "SBYTE", "UNDEFINED", "SSHORT",
            "SLONG", "SRATIONAL", "SINGLE", "DOUBLE",
            "UCS2_STRING","PREFIX_STRING" // internal only
    };

    // Sizes-in-bytes of the components of each IFD value format
    private static final int[] IFD_FORMAT_BYTES_PER_FORMAT = new int[] {
            0, 1, 1, 2, 4, 8, 1, 1, 2, 4, 8, 4, 8,
            1,1, // internal only
    };

    //!!
    /** optional prefix: prepended to string to indicate format is ascii */
    private static final byte[] EXIF_ASCII_PREFIX = new byte[] {
            0x41, 0x53, 0x43, 0x49, 0x49, 0x0, 0x0, 0x0 // "ascii"
    };

    private static final byte[] EXIF_JIS_PREFIX = {
            0x4A, 0x49, 0x53, 0x00, 0x00, 0x00, 0x00, 0x00
    };
    private static final byte[] EXIF_UNICODE_PREFIX = {
            0x55, 0x4E, 0x49, 0x43, 0x4F, 0x44, 0x45, 0x00
    };

    //!!! default for TAG_GPS_VERSION_ID="GPSVersionID"
    private static final byte[] GPS_VERSION_DEFAULT = new byte[] {
            0x02, 0x02, 0x0, 0x0
    };

    // minimal replacement for import android.util.Pair;
    private static class IntPair { // } extends Pair<Integer, Integer>{
        public final int first;
        public final int second;

        public IntPair(Integer first, Integer second) {
            this.first = first;
            this.second = second;
        }
    }

    // A class for indicating EXIF rational type.
    private static class Rational {
        public final long numerator;
        public final long denominator;
        private Rational(long numerator, long denominator) {
            // Handle erroneous case
            if (denominator == 0) {
                this.numerator = 0;
                this.denominator = 1;
                return;
            }
            this.numerator = numerator;
            this.denominator = denominator;
        }
        @Override
        public String toString() {
            return numerator + "/" + denominator;
        }
        public double calculate() {
            return (double) numerator / denominator;
        }
    }
    // A class to store an EXIF attribute.
    private static class ExifAttribute implements Comparable {
        private final ExifTag exifTag;
        public final int format;
        public final int numberOfComponents;
        public final byte[] bytes;
        private ExifAttribute(ExifTag exifTag, int format, int numberOfComponents, byte[] bytes) {
            this.exifTag = exifTag;
            this.format = format;
            this.numberOfComponents = numberOfComponents;
            this.bytes = bytes;
        }
        public static ExifAttribute createUShort(ExifTag id, int[] values, ByteOrder byteOrder) {
            final ByteBuffer buffer = ByteBuffer.wrap(
                    new byte[IFD_FORMAT_BYTES_PER_FORMAT[IFD_FORMAT_USHORT] * values.length]);
            buffer.order(byteOrder);
            for (int value : values) {
                buffer.putShort((short) value);
            }
            return new ExifAttribute(id, IFD_FORMAT_USHORT, values.length, buffer.array());
        }
        public static ExifAttribute createUShort(ExifTag id, int value, ByteOrder byteOrder) {
            return createUShort(id, new int[] {value}, byteOrder);
        }
        public static ExifAttribute createULong(ExifTag id, long[] values, ByteOrder byteOrder) {
            final ByteBuffer buffer = ByteBuffer.wrap(
                    new byte[IFD_FORMAT_BYTES_PER_FORMAT[IFD_FORMAT_ULONG] * values.length]);
            buffer.order(byteOrder);
            for (long value : values) {
                buffer.putInt((int) value);
            }
            return new ExifAttribute(id, IFD_FORMAT_ULONG, values.length, buffer.array());
        }
        public static ExifAttribute createULong(ExifTag id, long value, ByteOrder byteOrder) {
            return createULong(id, new long[] {value}, byteOrder);
        }
        public static ExifAttribute createSLong(ExifTag id, int[] values, ByteOrder byteOrder) {
            final ByteBuffer buffer = ByteBuffer.wrap(
                    new byte[IFD_FORMAT_BYTES_PER_FORMAT[IFD_FORMAT_SLONG] * values.length]);
            buffer.order(byteOrder);
            for (int value : values) {
                buffer.putInt(value);
            }
            return new ExifAttribute(id, IFD_FORMAT_SLONG, values.length, buffer.array());
        }
        public static ExifAttribute createSLong(ExifTag id, int value, ByteOrder byteOrder) {
            return createSLong(id, new int[] {value}, byteOrder);
        }
        public static ExifAttribute createByte(ExifTag id, String value) {
            // Exception for GPSAltitudeRef tag
            if (value.length() == 1 && value.charAt(0) >= '0' && value.charAt(0) <= '1') {
                final byte[] bytes = new byte[] { (byte) (value.charAt(0) - '0') };
                return new ExifAttribute(id, IFD_FORMAT_BYTE, bytes.length, bytes);
            }
            final byte[] bytes = value.getBytes(ASCII);
            return new ExifAttribute(id, IFD_FORMAT_BYTE, bytes.length, bytes);
        }

        public static ExifAttribute createPrefixString(ExifTag id, String value) {
            final byte[] bytesWithPrefix = encodePrefixString(value);

            return new ExifAttribute(id, IFD_FORMAT_PREFIX_STRING, bytesWithPrefix.length, bytesWithPrefix);
        }

        public static ExifAttribute createUcs2String(ExifTag id, String value) {
            final byte[] bytes = (value + "\0").getBytes(UCS2);
            return new ExifAttribute(id, IFD_FORMAT_UCS2LE_STRING, bytes.length, bytes);
        }
        public static ExifAttribute createString(ExifTag id, String value) {
            final byte[] bytes = (value + '\0').getBytes(ASCII);
            return new ExifAttribute(id, IFD_FORMAT_STRING, bytes.length, bytes);
        }
        public static ExifAttribute createURational(ExifTag id, Rational[] values, ByteOrder byteOrder) {
            final ByteBuffer buffer = ByteBuffer.wrap(
                    new byte[IFD_FORMAT_BYTES_PER_FORMAT[IFD_FORMAT_URATIONAL] * values.length]);
            buffer.order(byteOrder);
            for (Rational value : values) {
                buffer.putInt((int) value.numerator);
                buffer.putInt((int) value.denominator);
            }
            return new ExifAttribute(id, IFD_FORMAT_URATIONAL, values.length, buffer.array());
        }
        public static ExifAttribute createURational(ExifTag id, Rational value, ByteOrder byteOrder) {
            return createURational(id, new Rational[] {value}, byteOrder);
        }
        public static ExifAttribute createSRational(ExifTag id, Rational[] values, ByteOrder byteOrder) {
            final ByteBuffer buffer = ByteBuffer.wrap(
                    new byte[IFD_FORMAT_BYTES_PER_FORMAT[IFD_FORMAT_SRATIONAL] * values.length]);
            buffer.order(byteOrder);
            for (Rational value : values) {
                buffer.putInt((int) value.numerator);
                buffer.putInt((int) value.denominator);
            }
            return new ExifAttribute(id, IFD_FORMAT_SRATIONAL, values.length, buffer.array());
        }
        public static ExifAttribute createSRational(ExifTag id, Rational value, ByteOrder byteOrder) {
            return createSRational(id, new Rational[] {value}, byteOrder);
        }
        public static ExifAttribute createDouble(ExifTag id, double[] values, ByteOrder byteOrder) {
            final ByteBuffer buffer = ByteBuffer.wrap(
                    new byte[IFD_FORMAT_BYTES_PER_FORMAT[IFD_FORMAT_DOUBLE] * values.length]);
            buffer.order(byteOrder);
            for (double value : values) {
                buffer.putDouble(value);
            }
            return new ExifAttribute(id, IFD_FORMAT_DOUBLE, values.length, buffer.array());
        }
        public static ExifAttribute createDouble(ExifTag id, double value, ByteOrder byteOrder) {
            return createDouble(id, new double[] {value}, byteOrder);
        }
        @Override
        public String toString() {
            return "(" + ExifAttribute.getFormatName(format) + ", data length:" + bytes.length + ")";
        }
        private Object getValue(ByteOrder byteOrder) {
            try {
                ByteOrderAwarenessDataInputStream inputStream =
                        new ByteOrderAwarenessDataInputStream(bytes);
                inputStream.setByteOrder(byteOrder);
                switch (format) {
                    case IFD_FORMAT_UCS2LE_STRING: {
                        return decodePrefixString(bytes.length, bytes, UCS2);
                    }
                    case IFD_FORMAT_BYTE:
                    case IFD_FORMAT_SBYTE: {
                        // Exception for GPSAltitudeRef tag
                        if (bytes.length == 1 && bytes[0] >= 0 && bytes[0] <= 1) {
                            return new String(new char[] { (char) (bytes[0] + '0') });
                        }
                        return decodePrefixString(bytes.length, bytes, ASCII);
                    }
                    case IFD_FORMAT_UNDEFINED:
                    case IFD_FORMAT_PREFIX_STRING:
                    case IFD_FORMAT_STRING: {
                        return decodePrefixString(numberOfComponents, bytes, ASCII);
                    }
                    case IFD_FORMAT_USHORT: {
                        final int[] values = new int[numberOfComponents];
                        for (int i = 0; i < numberOfComponents; ++i) {
                            values[i] = inputStream.readUnsignedShort();
                        }
                        return values;
                    }
                    case IFD_FORMAT_ULONG: {
                        final long[] values = new long[numberOfComponents];
                        for (int i = 0; i < numberOfComponents; ++i) {
                            values[i] = inputStream.readUnsignedInt();
                        }
                        return values;
                    }
                    case IFD_FORMAT_URATIONAL: {
                        final Rational[] values = new Rational[numberOfComponents];
                        for (int i = 0; i < numberOfComponents; ++i) {
                            final long numerator = inputStream.readUnsignedInt();
                            final long denominator = inputStream.readUnsignedInt();
                            values[i] = new Rational(numerator, denominator);
                        }
                        return values;
                    }
                    case IFD_FORMAT_SSHORT: {
                        final int[] values = new int[numberOfComponents];
                        for (int i = 0; i < numberOfComponents; ++i) {
                            values[i] = inputStream.readShort();
                        }
                        return values;
                    }
                    case IFD_FORMAT_SLONG: {
                        final int[] values = new int[numberOfComponents];
                        for (int i = 0; i < numberOfComponents; ++i) {
                            values[i] = inputStream.readInt();
                        }
                        return values;
                    }
                    case IFD_FORMAT_SRATIONAL: {
                        final Rational[] values = new Rational[numberOfComponents];
                        for (int i = 0; i < numberOfComponents; ++i) {
                            final long numerator = inputStream.readInt();
                            final long denominator = inputStream.readInt();
                            values[i] = new Rational(numerator, denominator);
                        }
                        return values;
                    }
                    case IFD_FORMAT_SINGLE: {
                        final double[] values = new double[numberOfComponents];
                        for (int i = 0; i < numberOfComponents; ++i) {
                            values[i] = inputStream.readFloat();
                        }
                        return values;
                    }
                    case IFD_FORMAT_DOUBLE: {
                        final double[] values = new double[numberOfComponents];
                        for (int i = 0; i < numberOfComponents; ++i) {
                            values[i] = inputStream.readDouble();
                        }
                        return values;
                    }
                    default:
                        return null;
                }
            } catch (IOException e) {
                logWarn( "IOException occurred during reading a value", e);
                return null;
            }
        }

        public double getDoubleValue(ByteOrder byteOrder) {
            Object value = getValue(byteOrder);
            if (value == null) {
                throw new NumberFormatException("NULL can't be converted to a double value");
            }
            if (value instanceof String) {
                return Double.parseDouble((String) value);
            }
            if (value instanceof long[]) {
                long[] array = (long[]) value;
                if (array.length == 1) {
                    return array[0];
                }
                throw new NumberFormatException("There are more than one component");
            }
            if (value instanceof int[]) {
                int[] array = (int[]) value;
                if (array.length == 1) {
                    return array[0];
                }
                throw new NumberFormatException("There are more than one component");
            }
            if (value instanceof double[]) {
                double[] array = (double[]) value;
                if (array.length == 1) {
                    return array[0];
                }
                throw new NumberFormatException("There are more than one component");
            }
            if (value instanceof Rational[]) {
                Rational[] array = (Rational[]) value;
                if (array.length == 1) {
                    return array[0].calculate();
                }
                throw new NumberFormatException("There are more than one component");
            }
            throw new NumberFormatException("Couldn't find a double value");
        }
        public int getIntValue(ByteOrder byteOrder) {
            Object value = getValue(byteOrder);
            if (value == null) {
                throw new NumberFormatException("NULL can't be converted to a integer value");
            }
            if (value instanceof String) {
                return Integer.parseInt((String) value);
            }
            if (value instanceof long[]) {
                long[] array = (long[]) value;
                if (array.length == 1) {
                    return (int) array[0];
                }
                throw new NumberFormatException("There are more than one component");
            }
            if (value instanceof int[]) {
                int[] array = (int[]) value;
                if (array.length == 1) {
                    return array[0];
                }
                throw new NumberFormatException("There are more than one component");
            }
            throw new NumberFormatException("Couldn't find a integer value");
        }
        public String getStringValue(ByteOrder byteOrder) {
            Object value = getValue(byteOrder);
            if (value == null) {
                return null;
            }
            if (value instanceof String) {
                return (String) value;
            }
            final StringBuilder stringBuilder = new StringBuilder();
            if (value instanceof long[]) {
                long[] array = (long[]) value;
                for (int i = 0; i < array.length; ++i) {
                    stringBuilder.append(array[i]);
                    if (i + 1 != array.length) {
                        stringBuilder.append(",");
                    }
                }
                return stringBuilder.toString();
            }
            if (value instanceof int[]) {
                int[] array = (int[]) value;
                for (int i = 0; i < array.length; ++i) {
                    stringBuilder.append(array[i]);
                    if (i + 1 != array.length) {
                        stringBuilder.append(",");
                    }
                }
                return stringBuilder.toString();
            }
            if (value instanceof double[]) {
                double[] array = (double[]) value;
                for (int i = 0; i < array.length; ++i) {
                    stringBuilder.append(array[i]);
                    if (i + 1 != array.length) {
                        stringBuilder.append(",");
                    }
                }
                return stringBuilder.toString();
            }
            if (value instanceof Rational[]) {
                Rational[] array = (Rational[]) value;
                for (int i = 0; i < array.length; ++i) {
                    stringBuilder.append(array[i].numerator);
                    stringBuilder.append('/');
                    stringBuilder.append(array[i].denominator);
                    if (i + 1 != array.length) {
                        stringBuilder.append(",");
                    }
                }
                return stringBuilder.toString();
            }
            return null;
        }
        public int size() {
            return IFD_FORMAT_BYTES_PER_FORMAT[format] * numberOfComponents;
        }

        public String getFormatName() {
            return getFormatName(format);
        }

        public static String getFormatName(int format) {
            return ((format >= 0) && (format < IFD_FORMAT_NAMES.length)) ? IFD_FORMAT_NAMES[format] : Integer.toHexString(format);
        }

        // in jpg file values must be sorted by exif id
        @Override
        public int compareTo(Object o) {
            ExifAttribute other = (ExifAttribute) o;
            return this.exifTag.id - other.exifTag.id;
        }
    }
    // A class for defining EXIF tag.
    private static class ExifTag {
        public final int id;
        public final String name;
        public final int primaryFormat;
        public final int secondaryFormat;
        private ExifTag(String name, int id, int format) {
            this.name = name;
            this.id = id;
            this.primaryFormat = format;
            this.secondaryFormat = -1;
        }
        private ExifTag(String name, int id, int primaryFormat, int secondaryFormat) {
            this.name = name;
            this.id = id;
            this.primaryFormat = primaryFormat;
            this.secondaryFormat = secondaryFormat;
        }
        @Override public String toString() {
            return getClass().getSimpleName()+ ":" + name
                    + "(" + id + "=" + Integer.toHexString(id) + ")";
        }
    }

    private static final ExifTag EXIF_TAG_IMAGE_LENGTH = new ExifTag(TAG_IMAGE_LENGTH, 257, IFD_FORMAT_USHORT, IFD_FORMAT_ULONG);
    private static final ExifTag EXIF_TAG_IMAGE_WIDTH = new ExifTag(TAG_IMAGE_WIDTH, 256, IFD_FORMAT_USHORT, IFD_FORMAT_ULONG);
    private static final ExifTag EXIF_TAG_ORIENTATION = new ExifTag(TAG_ORIENTATION, 274, IFD_FORMAT_USHORT);
    private static final ExifTag EXIF_TAG_DATETIME = new ExifTag(TAG_DATETIME, 306, IFD_FORMAT_STRING);

    // Primary image IFD TIFF tags (See JEITA CP-3451 Table 14. page 54).
    private static final ExifTag[] IFD_TIFF_TAGS = new ExifTag[] {
            EXIF_TAG_IMAGE_WIDTH,
            EXIF_TAG_IMAGE_LENGTH,
            new ExifTag(TAG_BITS_PER_SAMPLE, 258, IFD_FORMAT_USHORT),
            new ExifTag(TAG_COMPRESSION, 259, IFD_FORMAT_USHORT),
            new ExifTag(TAG_PHOTOMETRIC_INTERPRETATION, 262, IFD_FORMAT_USHORT),
            new ExifTag(TAG_IMAGE_DESCRIPTION, 270, IFD_FORMAT_STRING),
            new ExifTag(TAG_MAKE, 271, IFD_FORMAT_STRING),
            new ExifTag(TAG_MODEL, 272, IFD_FORMAT_STRING),
            new ExifTag(TAG_STRIP_OFFSETS, 273, IFD_FORMAT_USHORT, IFD_FORMAT_ULONG),
            EXIF_TAG_ORIENTATION,
            new ExifTag(TAG_SAMPLES_PER_PIXEL, 277, IFD_FORMAT_USHORT),
            new ExifTag(TAG_ROWS_PER_STRIP, 278, IFD_FORMAT_USHORT, IFD_FORMAT_ULONG),
            new ExifTag(TAG_STRIP_BYTE_COUNTS, 279, IFD_FORMAT_USHORT, IFD_FORMAT_ULONG),
            new ExifTag(TAG_X_RESOLUTION, 282, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_Y_RESOLUTION, 283, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_PLANAR_CONFIGURATION, 284, IFD_FORMAT_USHORT),
            new ExifTag(TAG_RESOLUTION_UNIT, 296, IFD_FORMAT_USHORT),
            new ExifTag(TAG_TRANSFER_FUNCTION, 301, IFD_FORMAT_USHORT),
            new ExifTag(TAG_SOFTWARE, 305, IFD_FORMAT_STRING),
            EXIF_TAG_DATETIME,
            new ExifTag(TAG_ARTIST, 315, IFD_FORMAT_STRING),
            new ExifTag(TAG_WHITE_POINT, 318, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_PRIMARY_CHROMATICITIES, 319, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_JPEG_INTERCHANGE_FORMAT, 513, IFD_FORMAT_ULONG),
            new ExifTag(TAG_JPEG_INTERCHANGE_FORMAT_LENGTH, 514, IFD_FORMAT_ULONG),
            new ExifTag(TAG_Y_CB_CR_COEFFICIENTS, 529, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_Y_CB_CR_SUB_SAMPLING, 530, IFD_FORMAT_USHORT),
            new ExifTag(TAG_Y_CB_CR_POSITIONING, 531, IFD_FORMAT_USHORT),
            new ExifTag(TAG_REFERENCE_BLACK_WHITE, 532, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_COPYRIGHT, 33432, IFD_FORMAT_STRING),
            new ExifTag(TAG_EXIF_IFD_POINTER, 34665, IFD_FORMAT_ULONG),
            new ExifTag(TAG_GPS_INFO_IFD_POINTER, 34853, IFD_FORMAT_ULONG),
            new ExifTag(TAG_GPS_INFO_IFD_POINTER, 34853, IFD_FORMAT_ULONG),

            // from http://www.exiv2.org/tags.html
            /** The image title, as used by Windows XP, encoded in UCS2 */
            new ExifTag(TAG_WIN_TITLE, 40091, IFD_FORMAT_UCS2LE_STRING, IFD_FORMAT_BYTE),
            /** The image comment, as used by Windows XP, encoded in UCS2 */
            new ExifTag(TAG_WIN_COMMENT, 40092, IFD_FORMAT_UCS2LE_STRING, IFD_FORMAT_BYTE),
            /** The image author, as used by Windows XP (called Artist in the Windows shell). */
            new ExifTag(TAG_WIN_AUTHOR, 40093, IFD_FORMAT_UCS2LE_STRING, IFD_FORMAT_BYTE),
            /** The image keywords, as used by Windows XP, encoded in UCS2 */
            new ExifTag(TAG_WIN_KEYWORDS, 40094, IFD_FORMAT_UCS2LE_STRING, IFD_FORMAT_BYTE),
            /** The image subject, as used by Windows XP, encoded in UCS2 */
            new ExifTag(TAG_WIN_SUBJECT, 40095, IFD_FORMAT_UCS2LE_STRING, IFD_FORMAT_BYTE),
            /** Rating, as used by Windows XP: 0..5. */
            new ExifTag(TAG_WIN_RATING, 18246, IFD_FORMAT_USHORT),

	};
    public static final ExifTag EXIF_TAG_USER_COMMENT = new ExifTag(TAG_USER_COMMENT, 37510, IFD_FORMAT_PREFIX_STRING, IFD_FORMAT_UNDEFINED);
    public static final ExifTag EXIF_TAG_LIGHT_SOURCE = new ExifTag(TAG_LIGHT_SOURCE, 37384, IFD_FORMAT_USHORT);
    // Primary image IFD Exif Private tags (See JEITA CP-3451 Table 15. page 55).
    private static final ExifTag[] IFD_EXIF_TAGS = new ExifTag[] {
            new ExifTag(TAG_EXPOSURE_TIME, 33434, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_APERTURE, 33437, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_EXPOSURE_PROGRAM, 34850, IFD_FORMAT_USHORT),
            new ExifTag(TAG_SPECTRAL_SENSITIVITY, 34852, IFD_FORMAT_STRING),
            new ExifTag(TAG_ISO, 34855, IFD_FORMAT_USHORT),
            new ExifTag(TAG_OECF, 34856, IFD_FORMAT_UNDEFINED),
            new ExifTag(TAG_EXIF_VERSION, 36864, IFD_FORMAT_STRING),
            new ExifTag(TAG_DATETIME_ORIGINAL, 36867, IFD_FORMAT_STRING),
            new ExifTag(TAG_DATETIME_DIGITIZED, 36868, IFD_FORMAT_STRING),
            new ExifTag(TAG_COMPONENTS_CONFIGURATION, 37121, IFD_FORMAT_UNDEFINED),
            new ExifTag(TAG_COMPRESSED_BITS_PER_PIXEL, 37122, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_SHUTTER_SPEED_VALUE, 37377, IFD_FORMAT_SRATIONAL),
            new ExifTag(TAG_APERTURE_VALUE, 37378, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_BRIGHTNESS_VALUE, 37379, IFD_FORMAT_SRATIONAL),
            new ExifTag(TAG_EXPOSURE_BIAS_VALUE, 37380, IFD_FORMAT_SRATIONAL),
            new ExifTag(TAG_MAX_APERTURE_VALUE, 37381, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_SUBJECT_DISTANCE, 37382, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_METERING_MODE, 37383, IFD_FORMAT_USHORT),
            EXIF_TAG_LIGHT_SOURCE,
            new ExifTag(TAG_FLASH, 37385, IFD_FORMAT_USHORT),
            new ExifTag(TAG_FOCAL_LENGTH, 37386, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_SUBJECT_AREA, 37396, IFD_FORMAT_USHORT),
            new ExifTag(TAG_MAKER_NOTE, 37500, IFD_FORMAT_UNDEFINED),
            EXIF_TAG_USER_COMMENT,
            new ExifTag(TAG_SUBSEC_TIME, 37520, IFD_FORMAT_STRING),
            new ExifTag(TAG_SUBSEC_TIME_ORIG, 37521, IFD_FORMAT_STRING),
            new ExifTag(TAG_SUBSEC_TIME_DIG, 37522, IFD_FORMAT_STRING),
            new ExifTag(TAG_FLASHPIX_VERSION, 40960, IFD_FORMAT_UNDEFINED),
            new ExifTag(TAG_COLOR_SPACE, 40961, IFD_FORMAT_USHORT),
            new ExifTag(TAG_PIXEL_X_DIMENSION, 40962, IFD_FORMAT_USHORT, IFD_FORMAT_ULONG),
            new ExifTag(TAG_PIXEL_Y_DIMENSION, 40963, IFD_FORMAT_USHORT, IFD_FORMAT_ULONG),
            new ExifTag(TAG_RELATED_SOUND_FILE, 40964, IFD_FORMAT_STRING),
            new ExifTag(TAG_INTEROPERABILITY_IFD_POINTER, 40965, IFD_FORMAT_ULONG),
            new ExifTag(TAG_FLASH_ENERGY, 41483, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_SPATIAL_FREQUENCY_RESPONSE, 41484, IFD_FORMAT_UNDEFINED),
            new ExifTag(TAG_FOCAL_PLANE_X_RESOLUTION, 41486, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_FOCAL_PLANE_Y_RESOLUTION, 41487, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_FOCAL_PLANE_RESOLUTION_UNIT, 41488, IFD_FORMAT_USHORT),
            new ExifTag(TAG_SUBJECT_LOCATION, 41492, IFD_FORMAT_USHORT),
            new ExifTag(TAG_EXPOSURE_INDEX, 41493, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_SENSING_METHOD, 41495, IFD_FORMAT_USHORT),
            new ExifTag(TAG_FILE_SOURCE, 41728, IFD_FORMAT_UNDEFINED),
            new ExifTag(TAG_SCENE_TYPE, 41729, IFD_FORMAT_UNDEFINED),
            new ExifTag(TAG_CFA_PATTERN, 41730, IFD_FORMAT_UNDEFINED),
            new ExifTag(TAG_CUSTOM_RENDERED, 41985, IFD_FORMAT_USHORT),
            new ExifTag(TAG_EXPOSURE_MODE, 41986, IFD_FORMAT_USHORT),
            new ExifTag(TAG_WHITE_BALANCE, 41987, IFD_FORMAT_USHORT),
            new ExifTag(TAG_DIGITAL_ZOOM_RATIO, 41988, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_FOCAL_LENGTH_IN_35MM_FILM, 41989, IFD_FORMAT_USHORT),
            new ExifTag(TAG_SCENE_CAPTURE_TYPE, 41990, IFD_FORMAT_USHORT),
            new ExifTag(TAG_GAIN_CONTROL, 41991, IFD_FORMAT_USHORT),
            new ExifTag(TAG_CONTRAST, 41992, IFD_FORMAT_USHORT),
            new ExifTag(TAG_SATURATION, 41993, IFD_FORMAT_USHORT),
            new ExifTag(TAG_SHARPNESS, 41994, IFD_FORMAT_USHORT),
            new ExifTag(TAG_DEVICE_SETTING_DESCRIPTION, 41995, IFD_FORMAT_UNDEFINED),
            new ExifTag(TAG_SUBJECT_DISTANCE_RANGE, 41996, IFD_FORMAT_USHORT),
            new ExifTag(TAG_IMAGE_UNIQUE_ID, 42016, IFD_FORMAT_STRING),

    };

    public static final ExifTag EXIF_TAG_GPS_VERSION_ID = new ExifTag(TAG_GPS_VERSION_ID, 0, IFD_FORMAT_BYTE);
    // Primary image IFD GPS Info tags (See JEITA CP-3451 Table 16. page 56).
    private static final ExifTag[] IFD_GPS_TAGS = new ExifTag[] {
            EXIF_TAG_GPS_VERSION_ID,
            new ExifTag(TAG_GPS_LATITUDE_REF, 1, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_LATITUDE, 2, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_GPS_LONGITUDE_REF, 3, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_LONGITUDE, 4, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_GPS_ALTITUDE_REF, 5, IFD_FORMAT_BYTE),
            new ExifTag(TAG_GPS_ALTITUDE, 6, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_GPS_TIMESTAMP, 7, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_GPS_SATELLITES, 8, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_STATUS, 9, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_MEASURE_MODE, 10, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_DOP, 11, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_GPS_SPEED_REF, 12, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_SPEED, 13, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_GPS_TRACK_REF, 14, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_TRACK, 15, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_GPS_IMG_DIRECTION_REF, 16, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_IMG_DIRECTION, 17, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_GPS_MAP_DATUM, 18, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_DEST_LATITUDE_REF, 19, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_DEST_LATITUDE, 20, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_GPS_DEST_LONGITUDE_REF, 21, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_DEST_LONGITUDE, 22, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_GPS_DEST_BEARING_REF, 23, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_DEST_BEARING, 24, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_GPS_DEST_DISTANCE_REF, 25, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_DEST_DISTANCE, 26, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_GPS_PROCESSING_METHOD, 27, IFD_FORMAT_UNDEFINED),
            new ExifTag(TAG_GPS_AREA_INFORMATION, 28, IFD_FORMAT_UNDEFINED),
            new ExifTag(TAG_GPS_DATESTAMP, 29, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_DIFFERENTIAL, 30, IFD_FORMAT_USHORT),
    };
    // Primary image IFD Interoperability tag (See JEITA CP-3451 Table 17. page 56).
    private static final ExifTag[] IFD_INTEROPERABILITY_TAGS = new ExifTag[] {
            new ExifTag(TAG_INTEROPERABILITY_INDEX, 1, IFD_FORMAT_STRING),
    };
    // IFD Thumbnail tags (See JEITA CP-3451 Table 18. page 57).
    private static final ExifTag[] IFD_THUMBNAIL_TAGS = new ExifTag[] {
            new ExifTag(TAG_THUMBNAIL_IMAGE_WIDTH, 256, IFD_FORMAT_USHORT, IFD_FORMAT_ULONG),
            new ExifTag(TAG_THUMBNAIL_IMAGE_LENGTH, 257, IFD_FORMAT_USHORT, IFD_FORMAT_ULONG),
            new ExifTag(TAG_BITS_PER_SAMPLE, 258, IFD_FORMAT_USHORT),
            new ExifTag(TAG_COMPRESSION, 259, IFD_FORMAT_USHORT),
            new ExifTag(TAG_PHOTOMETRIC_INTERPRETATION, 262, IFD_FORMAT_USHORT),
            new ExifTag(TAG_IMAGE_DESCRIPTION, 270, IFD_FORMAT_STRING),
            new ExifTag(TAG_MAKE, 271, IFD_FORMAT_STRING),
            new ExifTag(TAG_MODEL, 272, IFD_FORMAT_STRING),
            new ExifTag(TAG_STRIP_OFFSETS, IFD_FORMAT_USHORT, IFD_FORMAT_ULONG),
            EXIF_TAG_ORIENTATION,
            new ExifTag(TAG_SAMPLES_PER_PIXEL, 277, IFD_FORMAT_USHORT),
            new ExifTag(TAG_ROWS_PER_STRIP, 278, IFD_FORMAT_USHORT, IFD_FORMAT_ULONG),
            new ExifTag(TAG_STRIP_BYTE_COUNTS, 279, IFD_FORMAT_USHORT, IFD_FORMAT_ULONG),
            new ExifTag(TAG_X_RESOLUTION, 282, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_Y_RESOLUTION, 283, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_PLANAR_CONFIGURATION, 284, IFD_FORMAT_USHORT),
            new ExifTag(TAG_RESOLUTION_UNIT, 296, IFD_FORMAT_USHORT),
            new ExifTag(TAG_TRANSFER_FUNCTION, 301, IFD_FORMAT_USHORT),
            new ExifTag(TAG_SOFTWARE, 305, IFD_FORMAT_STRING),
            EXIF_TAG_DATETIME,
            new ExifTag(TAG_ARTIST, 315, IFD_FORMAT_STRING),
            new ExifTag(TAG_WHITE_POINT, 318, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_PRIMARY_CHROMATICITIES, 319, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_JPEG_INTERCHANGE_FORMAT, 513, IFD_FORMAT_ULONG),
            new ExifTag(TAG_JPEG_INTERCHANGE_FORMAT_LENGTH, 514, IFD_FORMAT_ULONG),
            new ExifTag(TAG_Y_CB_CR_COEFFICIENTS, 529, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_Y_CB_CR_SUB_SAMPLING, 530, IFD_FORMAT_USHORT),
            new ExifTag(TAG_Y_CB_CR_POSITIONING, 531, IFD_FORMAT_USHORT),
            new ExifTag(TAG_REFERENCE_BLACK_WHITE, 532, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_COPYRIGHT, 33432, IFD_FORMAT_STRING),
            new ExifTag(TAG_EXIF_IFD_POINTER, 34665, IFD_FORMAT_ULONG),
            new ExifTag(TAG_GPS_INFO_IFD_POINTER, 34853, IFD_FORMAT_ULONG),
    };
    // See JEITA CP-3451 Figure 5. page 9.
    // The following values are used for indicating pointers to the other Image File Directorys.
    // Indices of Exif Ifd tag groups
    private static final int IFD_TIFF_HINT = 0;
    private static final int IFD_EXIF_HINT = 1;
    private static final int IFD_GPS_HINT = 2;
    private static final int IFD_INTEROPERABILITY_HINT = 3;
    private static final int IFD_THUMBNAIL_HINT = 4;

    //!!!
    // List of Exif tag groups or subSegments. EXIF_TAGS and EXIF_TAG_NAMES must have the same order.
    private static final ExifTag[][] EXIF_TAGS = new ExifTag[][] {
            IFD_TIFF_TAGS, IFD_EXIF_TAGS, IFD_GPS_TAGS, IFD_INTEROPERABILITY_TAGS,
            IFD_THUMBNAIL_TAGS
    };

    // List of Exif tag groups or subSegments. EXIF_TAGS and EXIF_TAG_NAMES must have the same order.
    private static final String[] EXIF_TAG_NAMES = new String[]{
            "TIFF", "EXIF", "GPS", "INTEROP", "THUMBNAIL"
    };

    // List of tags for pointing to the other image file directory offset.
    private static final ExifTag[] IFD_POINTER_TAGS = new ExifTag[] {
            new ExifTag(TAG_EXIF_IFD_POINTER, 34665, IFD_FORMAT_ULONG),
            new ExifTag(TAG_GPS_INFO_IFD_POINTER, 34853, IFD_FORMAT_ULONG),
            new ExifTag(TAG_INTEROPERABILITY_IFD_POINTER, 40965, IFD_FORMAT_ULONG),
    };
    // List of indices of the indicated tag groups according to the IFD_POINTER_TAGS
    private static final int[] IFD_POINTER_TAG_HINTS = new int[] {
            IFD_EXIF_HINT, IFD_GPS_HINT, IFD_INTEROPERABILITY_HINT
    };
    // Tags for indicating the thumbnail offset and length
    private static final ExifTag JPEG_INTERCHANGE_FORMAT_TAG =
            new ExifTag(TAG_JPEG_INTERCHANGE_FORMAT, 513, IFD_FORMAT_ULONG);
    private static final ExifTag JPEG_INTERCHANGE_FORMAT_LENGTH_TAG =
            new ExifTag(TAG_JPEG_INTERCHANGE_FORMAT_LENGTH, 514, IFD_FORMAT_ULONG);

    // Mappings from tag number to tag name and each item represents one IFD tag group.
    private static final HashMap<Integer,ExifTag>[] sNumner2ExifTag = new HashMap[EXIF_TAGS.length];
    // Mappings from tag name to tag number and each item represents one IFD tag group.
    private static final HashMap<String,ExifTag>[] sName2ExifTag = new HashMap[EXIF_TAGS.length];

    // these have a special attribute string-formatting. must for all double attributes
    private static final HashSet<String> sTagSetForCompatibility = new HashSet<>(Arrays.asList(
            TAG_APERTURE, TAG_DIGITAL_ZOOM_RATIO, TAG_EXPOSURE_TIME, TAG_SUBJECT_DISTANCE,
            TAG_GPS_TIMESTAMP));

    // See JPEG File Interchange Format Version 1.02.
    // The following values are defined for handling JPEG streams. In this implementation, we are
    // not only getting information from EXIF but also from some JPEG special segments such as
    // MARKER_COM for user comment and MARKER_SOFx for image width and height.
    private static final Charset ASCII = Charset.forName ("UTF-8"); //("US-ASCII");
    private static final Charset UCS2 = Charset.forName("UTF-16LE");
    private static final Charset UTF16 = Charset.forName("UTF-16");

    // Identifier for EXIF APP1 segment in JPEG
    private static final byte[] IDENTIFIER_EXIF_APP1 = "Exif\0\0".getBytes(ASCII);
    // JPEG segment markers, that each marker consumes two bytes beginning with 0xff and ending with
    // the indicator. There is no SOF4, SOF8, SOF16 markers in JPEG and SOFx markers indicates start
    // of frame(baseline DCT) and the image size info exists in its beginning part.
    private static final byte MARKER = (byte) 0xff;
    private static final byte MARKER_SOI = (byte) 0xd8;
    private static final byte MARKER_SOF0 = (byte) 0xc0;
    private static final byte MARKER_SOF1 = (byte) 0xc1;
    private static final byte MARKER_SOF2 = (byte) 0xc2;
    private static final byte MARKER_SOF3 = (byte) 0xc3;
    private static final byte MARKER_SOF5 = (byte) 0xc5;
    private static final byte MARKER_SOF6 = (byte) 0xc6;
    private static final byte MARKER_SOF7 = (byte) 0xc7;
    private static final byte MARKER_SOF9 = (byte) 0xc9;
    private static final byte MARKER_SOF10 = (byte) 0xca;
    private static final byte MARKER_SOF11 = (byte) 0xcb;
    private static final byte MARKER_SOF13 = (byte) 0xcd;
    private static final byte MARKER_SOF14 = (byte) 0xce;
    private static final byte MARKER_SOF15 = (byte) 0xcf;
    private static final byte MARKER_SOS = (byte) 0xda;
    private static final byte MARKER_APP1 = (byte) 0xe1;
    private static final byte MARKER_COM = (byte) 0xfe;
    private static final byte MARKER_EOI = (byte) 0xd9;
    static {
        sFormatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        sFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        // Build up the hash tables to look up Exif tags for reading Exif tags.
        for (int hint = 0; hint < EXIF_TAGS.length; ++hint) {
            sNumner2ExifTag[hint] = new HashMap<>();
            sName2ExifTag[hint] = new HashMap<>();
            for (ExifTag tag : EXIF_TAGS[hint]) {
                sNumner2ExifTag[hint].put(tag.id, tag);
                sName2ExifTag[hint].put(tag.name, tag);
            }
        }
    }

    private boolean validJpgExifFormat = true;

    protected File mExifFile = null;

    //!!! tagname => tagvalue(with assoziated tagdefinition)
    protected final HashMap<String, ExifAttribute>[] mAttributes = new HashMap[EXIF_TAGS.length];
    private ByteOrder mExifByteOrder = ByteOrder.BIG_ENDIAN;
    private boolean mHasThumbnail;
    // The following values used for indicating a thumbnail position.
    private int mThumbnailOffset;
    private int mThumbnailLength;
    private byte[] mThumbnailBytes;
    // Pattern to check non zero timestamp
    private static final Pattern sNonZeroTimePattern = Pattern.compile(".*[1-9].*");
    // Pattern to check gps timestamp
    private static final Pattern sGpsTimestampPattern =
            Pattern.compile("^([0-9][0-9]):([0-9][0-9]):([0-9][0-9])$");
    /**
     * Reads Exif tags from the specified image file.
     */
    public ExifInterface(String filename) throws IOException {
        this(filename, null);
    }
    public ExifInterface(String filename, InputStream in) throws IOException {
        if (filename == null) {
            throw new IllegalArgumentException("filename cannot be null");
        }
        mExifFile = (filename != null) ? new File(filename) : null;
        if (in == null) {
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(mExifFile);
                loadAttributes(fileInputStream);
            } finally {
                closeQuietly(fileInputStream);
            }
        } else {
            loadAttributes(in);

        }
    }

    protected ExifInterface() {}

    /** false means this is no valid jpg format */
    public boolean isValidJpgExifFormat() {return validJpgExifFormat;}

    /**
         * Returns the EXIF attribute of the specified tagName or {@code null} if there is no such tagName in
         * the image file.
         *
         * @param tagName the name of the tagName.
         */
    private ExifAttribute getExifAttribute(String tagName) {
        if (mAttributes[0] != null) {
            // Retrieves all tagName groups. The value from primary image tagName group has a higher priority
            // than the value from the thumbnail tagName group if there are more than one candidates.
            for (int subSegment = 0; subSegment < EXIF_TAGS.length; ++subSegment) {
                ExifAttribute value = mAttributes[subSegment].get(tagName);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }
    /**
     * Returns the value of the specified tagName or {@code null} if there
     * is no such tagName in the image file.
     *
     * @param tagName the name of the tagName.
     */
    public String getAttribute(String tagName) {
        ExifAttribute attribute = getExifAttribute(tagName);
        if (attribute != null) {
            if (!sTagSetForCompatibility.contains(tagName)) {
                String result = attribute.getStringValue(mExifByteOrder);
                return result;
            }
            if (tagName.equals(TAG_GPS_TIMESTAMP)) {
                // Convert the rational values to the custom formats for backwards compatibility.
                if (attribute.format != IFD_FORMAT_URATIONAL
                        && attribute.format != IFD_FORMAT_SRATIONAL) {
                    return null;
                }
                Rational[] array = (Rational[]) attribute.getValue(mExifByteOrder);
                if (array.length != 3) {
                    return null;
                }
                return String.format("%02d:%02d:%02d",
                        (int) ((float) array[0].numerator / array[0].denominator),
                        (int) ((float) array[1].numerator / array[1].denominator),
                        (int) ((float) array[2].numerator / array[2].denominator));
            }
            try {
                return Double.toString(attribute.getDoubleValue(mExifByteOrder));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    /**
     * Returns the integer value of the specified tag. If there is no such tag
     * in the image file or the value cannot be parsed as integer, return
     * <var>defaultValue</var>.
     *
     * @param tag the name of the tag.
     * @param defaultValue the value to return if the tag is not available.
     */
    public int getAttributeInt(String tag, int defaultValue) {
        ExifAttribute exifAttribute = getExifAttribute(tag);
        if (exifAttribute == null) {
            return defaultValue;
        }
        try {
            return exifAttribute.getIntValue(mExifByteOrder);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    /**
     * Returns the double value of the tag that is specified as rational or contains a
     * double-formatted value. If there is no such tag in the image file or the value cannot be
     * parsed as double, return <var>defaultValue</var>.
     *
     * @param tag the name of the tag.
     * @param defaultValue the value to return if the tag is not available.
     */
    public double getAttributeDouble(String tag, double defaultValue) {
        ExifAttribute exifAttribute = getExifAttribute(tag);
        if (exifAttribute == null) {
            return defaultValue;
        }
        try {
            return exifAttribute.getDoubleValue(mExifByteOrder);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    /**
     * Set the value of the specified tagName.
     *
     * @param tagName the name of the tagName.
     * @param value the value of the tagName.
     */
    public void setAttribute(String tagName, String value) {
        // Convert the given value to rational values for backwards compatibility.
        if (value != null && sTagSetForCompatibility.contains(tagName)) {
            if (tagName.equals(TAG_GPS_TIMESTAMP)) {
                Matcher m = sGpsTimestampPattern.matcher(value);
                if (!m.find()) {
                    logWarn( "Invalid value for " + tagName + " : " + value);
                    return;
                }
                value = Integer.parseInt(m.group(1)) + "/1," + Integer.parseInt(m.group(2)) + "/1,"
                        + Integer.parseInt(m.group(3)) + "/1";
            } else {
                try {
                    double doubleValue = Double.parseDouble(value);
                    value = (long) (doubleValue * 10000L) + "/10000";
                } catch (NumberFormatException e) {
                    logWarn( "Invalid value for " + tagName + " : " + value);
                    return;
                }
            }
        }

        for (int subSegment = 0 ; subSegment < EXIF_TAGS.length; ++subSegment) {
            if (subSegment == IFD_THUMBNAIL_HINT && !mHasThumbnail) {
                continue;
            }
            final ExifTag exifTag = sName2ExifTag[subSegment].get(tagName);
            if (exifTag != null) {
                if (value == null) {
                    mAttributes[subSegment].remove(tagName);
                    continue;
                }
                IntPair guess = guessDataFormat(value);
                int dataFormat;
                if (exifTag.primaryFormat == IFD_FORMAT_UCS2LE_STRING) {
                    dataFormat = exifTag.primaryFormat;
                } else if (exifTag.primaryFormat == IFD_FORMAT_PREFIX_STRING) {
                    dataFormat = exifTag.primaryFormat;
                } else if (exifTag.primaryFormat == guess.first || exifTag.primaryFormat == guess.second) {
                    dataFormat = exifTag.primaryFormat;
                } else if (exifTag.secondaryFormat != -1 && (exifTag.secondaryFormat == guess.first
                        || exifTag.secondaryFormat == guess.second)) {
                    dataFormat = exifTag.secondaryFormat;
                } else if (exifTag.primaryFormat == IFD_FORMAT_BYTE
                        || exifTag.primaryFormat == IFD_FORMAT_UNDEFINED
                        || exifTag.primaryFormat == IFD_FORMAT_STRING) {
                    dataFormat = exifTag.primaryFormat;
                } else {
                    logWarn( "Given tagName (" + tagName + ") value didn't match with one of expected "
                            + "formats: " + ExifAttribute.getFormatName(exifTag.primaryFormat)
                            + (exifTag.secondaryFormat == -1 ? "" : ", "
                            + ExifAttribute.getFormatName(exifTag.secondaryFormat)) + " (guess: "
                            + ExifAttribute.getFormatName(guess.first) + (guess.second == -1 ? "" : ", "
                            + ExifAttribute.getFormatName(guess.second)) + ")");
                    continue;
                }
                switch (dataFormat) {

                    case IFD_FORMAT_UCS2LE_STRING: {
                        setAttribute(subSegment, tagName, ExifAttribute.createUcs2String(exifTag, value));
                        break;
                    }
                    case IFD_FORMAT_PREFIX_STRING: {
                        setAttribute(subSegment, tagName, ExifAttribute.createPrefixString(exifTag, value));
                        break;
                    }
                    case IFD_FORMAT_BYTE: {
                        setAttribute(subSegment, tagName, ExifAttribute.createByte(exifTag, value));
                        break;
                    }
                    case IFD_FORMAT_UNDEFINED:
                    case IFD_FORMAT_STRING: {
                        setAttribute(subSegment, tagName, ExifAttribute.createString(exifTag, value));
                        break;
                    }
                    case IFD_FORMAT_USHORT: {
                        final String[] values = value.split(",");
                        final int[] intArray = new int[values.length];
                        for (int j = 0; j < values.length; ++j) {
                            intArray[j] = Integer.parseInt(values[j]);
                        }
                        setAttribute(subSegment, tagName, ExifAttribute.createUShort(exifTag, intArray, mExifByteOrder));
                        break;
                    }
                    case IFD_FORMAT_SLONG: {
                        final String[] values = value.split(",");
                        final int[] intArray = new int[values.length];
                        for (int j = 0; j < values.length; ++j) {
                            intArray[j] = Integer.parseInt(values[j]);
                        }
                        setAttribute(subSegment, tagName, ExifAttribute.createSLong(exifTag, intArray, mExifByteOrder));
                        break;
                    }
                    case IFD_FORMAT_ULONG: {
                        final String[] values = value.split(",");
                        final long[] longArray = new long[values.length];
                        for (int j = 0; j < values.length; ++j) {
                            longArray[j] = Long.parseLong(values[j]);
                        }
                        setAttribute(subSegment, tagName, ExifAttribute.createULong(exifTag, longArray, mExifByteOrder));
                        break;
                    }
                    case IFD_FORMAT_URATIONAL: {
                        final String[] values = value.split(",");
                        final Rational[] rationalArray = new Rational[values.length];
                        for (int j = 0; j < values.length; ++j) {
                            final String[] numbers = values[j].split("/");
                            rationalArray[j] = new Rational(Long.parseLong(numbers[0]),
                                    Long.parseLong(numbers[1]));
                        }
                        setAttribute(subSegment, tagName, ExifAttribute.createURational(exifTag, rationalArray, mExifByteOrder));
                        break;
                    }
                    case IFD_FORMAT_SRATIONAL: {
                        final String[] values = value.split(",");
                        final Rational[] rationalArray = new Rational[values.length];
                        for (int j = 0; j < values.length; ++j) {
                            final String[] numbers = values[j].split("/");
                            rationalArray[j] = new Rational(Long.parseLong(numbers[0]),
                                    Long.parseLong(numbers[1]));
                        }
                        setAttribute(subSegment, tagName, ExifAttribute.createSRational(exifTag, rationalArray, mExifByteOrder));
                        break;
                    }
                    case IFD_FORMAT_DOUBLE: {
                        final String[] values = value.split(",");
                        final double[] doubleArray = new double[values.length];
                        for (int j = 0; j < values.length; ++j) {
                            doubleArray[j] = Double.parseDouble(values[j]);
                        }
                        setAttribute(subSegment, tagName, ExifAttribute.createDouble(exifTag, doubleArray, mExifByteOrder));
                        break;
                    }
                    default:
                        logWarn( "Data format isn't one of expected formats: " + dataFormat);
                        continue;
                }
            }
        }
    }

    private void setAttribute(int segment, String tag, ExifAttribute exifAttribute) {
        mAttributes[segment].put(tag, exifAttribute);
    }

    /**
     * Update the values of the tags in the tag groups if any value for the tag already was stored.
     *
     * @param tag the name of the tag.
     * @param value the value of the tag in a form of {@link ExifAttribute}.
     * @return Returns {@code true} if updating is placed.
     */
    private boolean updateAttribute(String tag, ExifAttribute value) {
        boolean updated = false;
        for (int i = 0 ; i < EXIF_TAGS.length; ++i) {
            if (mAttributes[i].containsKey(tag)) {
                mAttributes[i].put(tag, value);
                updated = true;
            }
        }
        return updated;
    }
    /**
     * Remove any values of the specified tag.
     *
     * @param tag the name of the tag.
     */
    public void removeAttribute(String tag) {
        for (int i = 0 ; i < EXIF_TAGS.length; ++i) {
            mAttributes[i].remove(tag);
        }
    }
    /**
     * This function decides which parser to read the image data according to the given input stream
     * type and the content of the input stream. In each case, it reads the first three bytes to
     * determine whether the image data format is JPEG or not.
     */
    private void loadAttributes(InputStream in) throws IOException {
        try {
            // Initialize mAttributes.
            for (int i = 0; i < EXIF_TAGS.length; ++i) {
                mAttributes[i] = new HashMap();
            }
            getJpegAttributes(in);
        } catch (IOException e) {
            // Ignore exceptions in order to keep the compatibility with the old versions of
            // ExifInterface.
            logWarn( "Invalid image.", e);
            validJpgExifFormat = false;
        } finally {
            if (DEBUG_INTERNAL) {
                logDebug(this.toString());
            }
        }
    }

    @Override
    public String toString() {
        return getDebugString("\n", TAG_DATETIME, TAG_GPS_VERSION_ID);
    }

    // Prints out attributes for debugging.
    public String getDebugString(String lineDelimiter, String... _keysToExclude) {
        StringBuilder sb = new StringBuilder();
        final List<String> keysToExclude = Arrays.asList(_keysToExclude);

        for (int i = 0; i < mAttributes.length; ++i) {
            HashMap<String, ExifAttribute> exifSegment = mAttributes[i];
            String[] keys = exifSegment.keySet().toArray(new String[exifSegment.size()]);
            Arrays.sort(keys);
            // for display exif tags are sorted by tagName
            for (String tagName : keys) {
                if (!keysToExclude.contains(tagName)) {
                    final ExifAttribute tagValue = exifSegment.get(tagName);
                    sb.append("EXIF.").append(EXIF_TAG_NAMES[i]).append(".").append(tagName);
                    if (DEBUG) {
                        ExifTag tag = tagValue.exifTag;
                        if (tag != null) {
                            sb.append("(").append(tag.id).append("=0x")
                                    .append(Integer.toHexString(tag.id)).append(")");
                        }
                    }
                    sb.append("='").append(tagValue.getStringValue(mExifByteOrder)).append("'");
                    if (DEBUG) {
                        sb.append(" : ")
                                .append(tagValue.getFormatName());
                    }
                    sb.append(lineDelimiter);
                }
            }
            sb.append(lineDelimiter);
        }
        return sb.toString();
    }

    /**
     * Save the tag data into the original image file. This is expensive because it involves
     * copying all the data from one file to another and deleting the old file and renaming the
     * other. It's best to use {@link #setAttribute(String,String)} to set all attributes to write
     * and make a single call rather than multiple calls for each attribute.
     */
    public void saveAttributes() throws IOException {
        saveAttributes(mExifFile, mExifFile, true);
    }

    public void saveAttributes(File inFile, File outFile, boolean deleteInFileOnFinish) throws IOException {
        fixAttributes();

        // Keep the thumbnail in memory
        mThumbnailBytes = getThumbnail(inFile);
        File renamedInFile = inFile;

        boolean overwriteOriginal = inFile.equals(outFile);

        if (overwriteOriginal) {
            // Move the original file to temporary file.
            renamedInFile = new File(inFile.getAbsolutePath() + ".tmp");
            File originalInFile = inFile;
            if (!originalInFile.renameTo(renamedInFile)) {
                throw new IOException("Could'nt rename sourcefile from " + inFile +
                        " to " + renamedInFile.getAbsolutePath());
            }
        }
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            // Save the new file.
            in = new FileInputStream(renamedInFile);
            out = new FileOutputStream(outFile);

            saveJpegAttributes(in, out, mThumbnailBytes);
        } finally {
            closeQuietly(in);
            closeQuietly(out);

            if (deleteInFileOnFinish || overwriteOriginal) {
                deleteFile(renamedInFile);
            }
        }
        // Discard the thumbnail in memory
        mThumbnailBytes = null;
    }

    protected boolean deleteFile(File renamedInFile) {
        return renamedInFile.delete();
    }

    /** repairs wrong/missing attributes */
    protected void fixAttributes() {
        if (ExifInterface.fixDateOnSave) {
            // The value of DATETIME tag has the same value of DATETIME_ORIGINAL tag.
            String valueOfDateTimeOriginal = getAttribute(TAG_DATETIME_ORIGINAL);
            if (valueOfDateTimeOriginal != null) {
                setAttribute(IFD_TIFF_HINT, TAG_DATETIME,
                        ExifAttribute.createString(EXIF_TAG_DATETIME, valueOfDateTimeOriginal));
            }
        }

        if (getExifAttribute(TAG_IMAGE_WIDTH) == null) {
            setAttribute(IFD_TIFF_HINT,TAG_IMAGE_WIDTH,
                    ExifAttribute.createULong(EXIF_TAG_IMAGE_WIDTH, 0, mExifByteOrder));
        }
        if (getExifAttribute(TAG_IMAGE_LENGTH) == null) {
            setAttribute(IFD_TIFF_HINT,TAG_IMAGE_LENGTH,
                    ExifAttribute.createULong(EXIF_TAG_IMAGE_LENGTH, 0, mExifByteOrder));
        }
        if (getExifAttribute(TAG_ORIENTATION) == null) {
            setAttribute(IFD_TIFF_HINT,TAG_ORIENTATION,
                    ExifAttribute.createULong(EXIF_TAG_ORIENTATION, 0, mExifByteOrder));
        }
        if (getExifAttribute(TAG_LIGHT_SOURCE) == null) {
            setAttribute(IFD_EXIF_HINT, TAG_LIGHT_SOURCE,
                    ExifAttribute.createULong(EXIF_TAG_LIGHT_SOURCE, 0, mExifByteOrder));
        }

        // add missing TAG_GPS_VERSION_ID if there is gps info included
        if ((getExifAttribute(TAG_GPS_LATITUDE) != null) && (getExifAttribute(TAG_GPS_VERSION_ID) == null)) {
            /* from http://www.exiv2.org/tags.html

                Indicates the version of <GPSInfoIFD>. The version is given as 2.0.0.0.
                This tag is mandatory when <GPSInfo> tag is present. (Note: The <GPSVersionID> tag
                is given in bytes, unlike the <ExifVersion> tag. When the version is 2.0.0.0,
                the tag value is 02000000.H).*/
            setAttribute(IFD_GPS_HINT,TAG_GPS_VERSION_ID,
                    new ExifAttribute(EXIF_TAG_GPS_VERSION_ID, IFD_FORMAT_BYTE, GPS_VERSION_DEFAULT.length, GPS_VERSION_DEFAULT));
        }
    }

    /**
     * Returns true if the image file has a thumbnail.
     */
    public boolean hasThumbnail() {
        return mHasThumbnail;
    }
    /**
     * Returns the thumbnail inside the image file, or {@code null} if there is no thumbnail.
     * The returned data is in JPEG format and can be decoded using
     * android.graphics.BitmapFactory#decodeByteArray(byte[],int,int)
     */
    public byte[] getThumbnail() {
        return getThumbnail(mExifFile);
    }

    public byte[] getThumbnail(File inFile) {
        if (!mHasThumbnail) {
            return null;
        }
        if (mThumbnailBytes != null) {
            return mThumbnailBytes;
        }
        // Read the thumbnail.
        FileInputStream in = null;
        try {
            in = new FileInputStream(inFile);
            return getThumbnail(in);
        } catch (IOException e) {
            // Couldn't get a thumbnail image.
        } finally {
            closeQuietly(in);
        }
        return null;
    }

    public byte[] getThumbnail(InputStream in) throws IOException {
        if (!mHasThumbnail) {
            return null;
        }
        if (mThumbnailBytes != null) {
            return mThumbnailBytes;
        }
        if (in.skip(mThumbnailOffset) != mThumbnailOffset) {
            throw new IOException("Corrupted image");
        }
        byte[] buffer = new byte[mThumbnailLength];
        if (in.read(buffer) != mThumbnailLength) {
            throw new IOException("Corrupted image");
        }
        mThumbnailBytes = buffer;
        return buffer;
    }

    private void closeQuietly(Closeable in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * Returns the offset and length of thumbnail inside the image file, or
     * {@code null} if there is no thumbnail.
     *
     * @return two-element array, the offset in the first value, and length in
     *         the second, or {@code null} if no thumbnail was found.
     * @hide
     */
    public long[] getThumbnailRange() {
        if (!mHasThumbnail) {
            return null;
        }
        long[] range = new long[2];
        range[0] = mThumbnailOffset;
        range[1] = mThumbnailLength;
        return range;
    }
    /**
     * Stores the latitude and longitude value in a float array. The first element is
     * the latitude, and the second element is the longitude. Returns false if the
     * Exif tags are not available.
     */
    public boolean getLatLong(float output[]) {
        String latValue = getAttribute(TAG_GPS_LATITUDE);
        String latRef = getAttribute(TAG_GPS_LATITUDE_REF);
        String lngValue = getAttribute(TAG_GPS_LONGITUDE);
        String lngRef = getAttribute(TAG_GPS_LONGITUDE_REF);
        if (latValue != null && latRef != null && lngValue != null && lngRef != null) {
            try {
                output[0] = convertRationalLatLonToFloat(latValue, latRef);
                output[1] = convertRationalLatLonToFloat(lngValue, lngRef);
                return true;
            } catch (IllegalArgumentException e) {
                // if values are not parseable
            }
        }
        return false;
    }
    /**
     * Return the altitude in meters. If the exif tag does not exist, return
     * <var>defaultValue</var>.
     *
     * @param defaultValue the value to return if the tag is not available.
     */
    public double getAltitude(double defaultValue) {
        double altitude = getAttributeDouble(TAG_GPS_ALTITUDE, -1);
        int ref = getAttributeInt(TAG_GPS_ALTITUDE_REF, -1);
        if (altitude >= 0 && ref >= 0) {
            return (altitude * ((ref == 1) ? -1 : 1));
        } else {
            return defaultValue;
        }
    }
    /**
     * Returns number of milliseconds since Jan. 1, 1970, midnight local time.
     * Returns -1 if the date time information if not available.
     *
     */
    public long getDateTime() {
        String dateTimeString = getAttribute(TAG_DATETIME);
        if (dateTimeString == null
                || !sNonZeroTimePattern.matcher(dateTimeString).matches()) return -1;
        ParsePosition pos = new ParsePosition(0);
        try {
            // The exif field is in local time. Parsing it as if it is UTC will yield time
            // since 1/1/1970 local time
            Date datetime = sFormatter.parse(dateTimeString, pos);
            if (datetime == null) return -1;
            long msecs = datetime.getTime();
            String subSecs = getAttribute(TAG_SUBSEC_TIME);
            if (subSecs != null) {
                try {
                    long sub = Long.valueOf(subSecs);
                    while (sub > 1000) {
                        sub /= 10;
                    }
                    msecs += sub;
                } catch (NumberFormatException e) {
                    // Ignored
                }
            }
            return msecs;
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }
    /**
     * Returns number of milliseconds since Jan. 1, 1970, midnight UTC.
     * Returns -1 if the date time information if not available.
     *
     */
    public long getGpsDateTime() {
        String date = getAttribute(TAG_GPS_DATESTAMP);
        String time = getAttribute(TAG_GPS_TIMESTAMP);
        if (date == null || time == null
                || (!sNonZeroTimePattern.matcher(date).matches()
                && !sNonZeroTimePattern.matcher(time).matches())) {
            return -1;
        }
        String dateTimeString = date + ' ' + time;
        ParsePosition pos = new ParsePosition(0);
        try {
            Date datetime = sFormatter.parse(dateTimeString, pos);
            if (datetime == null) return -1;
            return datetime.getTime();
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }
    private static float convertRationalLatLonToFloat(String rationalString, String ref) {
        try {
            String [] parts = rationalString.split(",");
            String [] pair;
            pair = parts[0].split("/");
            double degrees = Double.parseDouble(pair[0].trim())
                    / Double.parseDouble(pair[1].trim());
            pair = parts[1].split("/");
            double minutes = Double.parseDouble(pair[0].trim())
                    / Double.parseDouble(pair[1].trim());
            pair = parts[2].split("/");
            double seconds = Double.parseDouble(pair[0].trim())
                    / Double.parseDouble(pair[1].trim());
            double result = degrees + (minutes / 60.0) + (seconds / 3600.0);
            if ((ref.equals("S") || ref.equals("W"))) {
                return (float) -result;
            }
            return (float) result;
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            // Not valid
            throw new IllegalArgumentException();
        }
    }
    // Loads EXIF attributes from a JPEG input stream.
    private void getJpegAttributes(InputStream inputStream) throws IOException {
        // See JPEG File Interchange Format Specification page 5.
        if (DEBUG_INTERNAL) {
            logDebug( "getJpegAttributes starting with: " + inputStream);
        }
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        byte marker;
        int bytesRead = 0;
        if ((marker = dataInputStream.readByte()) != MARKER) {
            throw new IOException("Invalid marker: " + Integer.toHexString(marker & 0xff));
        }
        ++bytesRead;
        if (dataInputStream.readByte() != MARKER_SOI) {
            throw new IOException("Invalid marker: " + Integer.toHexString(marker & 0xff));
        }
        ++bytesRead;
        while (true) {
            marker = dataInputStream.readByte();
            if (marker != MARKER) {
                throw new IOException("Invalid marker:" + Integer.toHexString(marker & 0xff));
            }
            ++bytesRead;
            marker = dataInputStream.readByte();
            if (DEBUG_INTERNAL) {
                logDebug( "Found JPEG segment indicator: " + Integer.toHexString(marker & 0xff));
            }
            ++bytesRead;
            // EOI indicates the end of an image and in case of SOS, JPEG image stream starts and
            // the image data will terminate right after.
            if (marker == MARKER_EOI || marker == MARKER_SOS) {
                break;
            }
            int length = dataInputStream.readUnsignedShort() - 2;
            bytesRead += 2;
            if (DEBUG_INTERNAL) {
                logDebug( "JPEG segment: " + Integer.toHexString(marker & 0xff) + " (length: "
                        + (length + 2) + ")");
            }
            if (length < 0) {
                throw new IOException("Invalid length");
            }
            switch (marker) {
                case MARKER_APP1: {
                    if (DEBUG_INTERNAL) {
                        logDebug( "MARKER_APP1");
                    }
                    if (length < 6) {
                        // Skip if it's not an EXIF APP1 segment.
                        break;
                    }
                    byte[] identifier = new byte[6];
                    if (inputStream.read(identifier) != 6) {
                        throw new IOException("Invalid exif");
                    }
                    bytesRead += 6;
                    length -= 6;
                    if (!Arrays.equals(identifier, IDENTIFIER_EXIF_APP1)) {
                        // Skip if it's not an EXIF APP1 segment.
                        break;
                    }
                    if (length <= 0) {
                        throw new IOException("Invalid exif");
                    }
                    if (DEBUG_INTERNAL) {
                        logDebug( "readExifSegment with a byte array (length: " + length + ")");
                    }
                    byte[] bytes = new byte[length];
                    if (dataInputStream.read(bytes) != length) {
                        throw new IOException("Invalid exif");
                    }
                    readExifSegment(bytes, bytesRead);
                    bytesRead += length;
                    length = 0;
                    break;
                }
                case MARKER_COM: {
                    byte[] bytes = new byte[length];
                    if (dataInputStream.read(bytes) != length) {
                        throw new IOException("Invalid exif");
                    }
                    length = 0;
                    if (getAttribute(TAG_USER_COMMENT) == null) {
                        setAttribute(IFD_EXIF_HINT, TAG_USER_COMMENT,ExifAttribute.createString(
                                EXIF_TAG_USER_COMMENT,
                                decodePrefixString(bytes.length, bytes,ASCII)));
                    }
                    break;
                }
                case MARKER_SOF0:
                case MARKER_SOF1:
                case MARKER_SOF2:
                case MARKER_SOF3:
                case MARKER_SOF5:
                case MARKER_SOF6:
                case MARKER_SOF7:
                case MARKER_SOF9:
                case MARKER_SOF10:
                case MARKER_SOF11:
                case MARKER_SOF13:
                case MARKER_SOF14:
                case MARKER_SOF15: {
                    if (dataInputStream.skipBytes(1) != 1) {
                        throw new IOException("Invalid SOFx");
                    }
                    setAttribute(IFD_TIFF_HINT, TAG_IMAGE_LENGTH, ExifAttribute.createULong(EXIF_TAG_IMAGE_LENGTH,
                            dataInputStream.readUnsignedShort(), mExifByteOrder));
                    setAttribute(IFD_TIFF_HINT, TAG_IMAGE_WIDTH, ExifAttribute.createULong(EXIF_TAG_IMAGE_WIDTH,
                            dataInputStream.readUnsignedShort(), mExifByteOrder));
                    length -= 5;
                    break;
                }
                default: {
                    break;
                }
            }
            if (length < 0) {
                throw new IOException("Invalid length");
            }
            if (dataInputStream.skipBytes(length) != length) {
                throw new IOException("Invalid JPEG segment");
            }
            bytesRead += length;
        }
    }
    // Stores a new JPEG image with EXIF attributes into a given output stream.
    public void saveJpegAttributes(InputStream inputStream, OutputStream outputStream, byte[] thumbnail)
            throws IOException {
        // See JPEG File Interchange Format Specification page 5.
        if (DEBUG_INTERNAL) {
            logDebug( "saveJpegAttributes starting with (inputStream: " + inputStream
                    + ", outputStream: " + outputStream + ")");
        }
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        ByteOrderAwarenessDataOutputStream dataOutputStream =
                new ByteOrderAwarenessDataOutputStream(outputStream, ByteOrder.BIG_ENDIAN);
        if (dataInputStream.readByte() != MARKER) {
            throw new IOException("Invalid marker");
        }
        dataOutputStream.writeByte(MARKER);
        if (dataInputStream.readByte() != MARKER_SOI) {
            throw new IOException("Invalid marker");
        }
        dataOutputStream.writeByte(MARKER_SOI);
        // Write EXIF APP1 segment
        dataOutputStream.writeByte(MARKER);
        dataOutputStream.writeByte(MARKER_APP1);

        writeExifSegment(dataOutputStream, 6, thumbnail);
        byte[] bytes = new byte[4096];
        while (true) {
            byte marker = dataInputStream.readByte();
            if (marker != MARKER) {
                throw new IOException("Invalid marker");
            }
            marker = dataInputStream.readByte();
            switch (marker) {
                case MARKER_APP1: {
                    int length = dataInputStream.readUnsignedShort() - 2;
                    if (length < 0) {
                        throw new IOException("Invalid length");
                    }
                    byte[] identifier = new byte[6];
                    if (length >= 6) {
                        if (dataInputStream.read(identifier) != 6) {
                            throw new IOException("Invalid exif");
                        }
                        if (Arrays.equals(identifier, IDENTIFIER_EXIF_APP1)) {
                            // Skip the original EXIF APP1 segment.
                            if (dataInputStream.skip(length - 6) != length - 6) {
                                throw new IOException("Invalid length");
                            }
                            break;
                        }
                    }
                    // Copy non-EXIF APP1 segment.
                    dataOutputStream.writeByte(MARKER);
                    dataOutputStream.writeByte(marker);
                    dataOutputStream.writeUnsignedShort(length + 2);
                    if (length >= 6) {
                        length -= 6;
                        dataOutputStream.write(identifier);
                    }
                    int read;
                    while (length > 0 && (read = dataInputStream.read(
                            bytes, 0, Math.min(length, bytes.length))) >= 0) {
                        dataOutputStream.write(bytes, 0, read);
                        length -= read;
                    }
                    break;
                }
                case MARKER_EOI:
                case MARKER_SOS: {
                    dataOutputStream.writeByte(MARKER);
                    dataOutputStream.writeByte(marker);
                    // Copy all the remaining data
                    streamCopy(dataInputStream, dataOutputStream);
                    return;
                }
                default: {
                    // Copy JPEG segment
                    dataOutputStream.writeByte(MARKER);
                    dataOutputStream.writeByte(marker);
                    int length = dataInputStream.readUnsignedShort();
                    dataOutputStream.writeUnsignedShort(length);
                    length -= 2;
                    if (length < 0) {
                        throw new IOException("Invalid length");
                    }
                    int read;
                    while (length > 0 && (read = dataInputStream.read(
                            bytes, 0, Math.min(length, bytes.length))) >= 0) {
                        dataOutputStream.write(bytes, 0, read);
                        length -= read;
                    }
                    break;
                }
            }
        }
    }

    private void streamCopy(DataInputStream dataInputStream, ByteOrderAwarenessDataOutputStream dataOutputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int n = 0;
        while (-1 != (n = dataInputStream.read(buffer))) {
            dataOutputStream.write(buffer, 0, n);
        }
    }

    // Reads the given EXIF byte area and save its tag data into attributes.
    private void readExifSegment(byte[] exifBytes, int exifOffsetFromBeginning) throws IOException {
        // Parse TIFF Headers. See JEITA CP-3451C Table 1. page 10.
        ByteOrderAwarenessDataInputStream dataInputStream =
                new ByteOrderAwarenessDataInputStream(exifBytes);
        // Read byte align
        short byteOrder = dataInputStream.readShort();
        switch (byteOrder) {
            case BYTE_ALIGN_II:
                if (DEBUG_INTERNAL) {
                    logDebug( "readExifSegment: Byte Align II");
                }
                mExifByteOrder = ByteOrder.LITTLE_ENDIAN;
                break;
            case BYTE_ALIGN_MM:
                if (DEBUG_INTERNAL) {
                    logDebug( "readExifSegment: Byte Align MM");
                }
                mExifByteOrder = ByteOrder.BIG_ENDIAN;
                break;
            default:
                throw new IOException("Invalid byte order: " + Integer.toHexString(byteOrder));
        }
        // Set byte order.
        dataInputStream.setByteOrder(mExifByteOrder);
        int startCode = dataInputStream.readUnsignedShort();
        if (startCode != 0x2a) {
            throw new IOException("Invalid exif start: " + Integer.toHexString(startCode));
        }
        // Read first ifd offset
        long firstIfdOffset = dataInputStream.readUnsignedInt();
        if (firstIfdOffset < 8 || firstIfdOffset >= exifBytes.length) {
            throw new IOException("Invalid first Ifd offset: " + firstIfdOffset);
        }
        firstIfdOffset -= 8;
        if (firstIfdOffset > 0) {
            if (dataInputStream.skip(firstIfdOffset) != firstIfdOffset) {
                throw new IOException("Couldn't jump to first Ifd: " + firstIfdOffset);
            }
        }
        // Read primary image TIFF image file directory.
        readImageFileDirectory(dataInputStream, IFD_TIFF_HINT);
        // Process thumbnail.
        String jpegInterchangeFormatString = getAttribute(JPEG_INTERCHANGE_FORMAT_TAG.name);
        String jpegInterchangeFormatLengthString =
                getAttribute(JPEG_INTERCHANGE_FORMAT_LENGTH_TAG.name);
        if (jpegInterchangeFormatString != null && jpegInterchangeFormatLengthString != null) {
            try {
                int jpegInterchangeFormat = Integer.parseInt(jpegInterchangeFormatString);
                int jpegInterchangeFormatLength = Integer
                        .parseInt(jpegInterchangeFormatLengthString);
                // The following code limits the size of thumbnail size not to overflow EXIF data area.
                jpegInterchangeFormatLength = Math.min(jpegInterchangeFormat
                        + jpegInterchangeFormatLength, exifBytes.length) - jpegInterchangeFormat;
                if (jpegInterchangeFormat > 0 && jpegInterchangeFormatLength > 0) {
                    mHasThumbnail = true;
                    mThumbnailOffset = exifOffsetFromBeginning + jpegInterchangeFormat;
                    mThumbnailLength = jpegInterchangeFormatLength;
                }
            } catch (NumberFormatException e) {
                // Ignored the corrupted image.
            }
        }
    }

    // Reads image file directory, which is a tag group in EXIF.
    private void readImageFileDirectory(ByteOrderAwarenessDataInputStream dataInputStream, int hint)
            throws IOException {
        if (dataInputStream.peek() + 2 > dataInputStream.mLength) {
            // Return if there is no data from the offset.
            return;
        }
        // See JEITA CP-3451 Figure 5. page 9.
        short numberOfDirectoryEntry = dataInputStream.readShort();
        if (dataInputStream.peek() + 12 * numberOfDirectoryEntry > dataInputStream.mLength) {
            // Return if the size of entries is too big.
            return;
        }
        if (DEBUG_INTERNAL) {
            logDebug( "numberOfDirectoryEntry: " + numberOfDirectoryEntry);
        }
        for (short i = 0; i < numberOfDirectoryEntry; ++i) {
            int tagNumber = dataInputStream.readUnsignedShort();
            int dataFormat = dataInputStream.readUnsignedShort();
            int numberOfComponents = dataInputStream.readInt();
            long nextEntryOffset = dataInputStream.peek() + 4;  // next four bytes is for data
                                                                // offset or value.
            // Look up a corresponding tag from tag number
            final ExifTag tag = (ExifTag) sNumner2ExifTag[hint].get(tagNumber);
            if (DEBUG_INTERNAL) {
                logDebug(getContextDebugMessage(hint, tagNumber, dataFormat, numberOfComponents, tag));
            }
            if (tag == null || dataFormat <= 0 ||
                    dataFormat >= IFD_FORMAT_BYTES_PER_FORMAT.length) {

                // Skip if the parsed tag number is not defined or invalid data format.
                if (tag == null) {
                    logWarn( "Skip tag entry[undefined tag id " + tagNumber + "(0x" +
                            Integer.toHexString(tagNumber) +")]: "
                            + getContextDebugMessage(hint, tagNumber, dataFormat
                            , numberOfComponents, tag));
                } else {
                    logWarn( "Skip the tag entry[invalid format " + dataFormat + "(0x" +
                            Integer.toHexString(dataFormat) +")]: "
                            + getContextDebugMessage(hint, tagNumber, dataFormat
                            , numberOfComponents, tag));
                }
                dataInputStream.seek(nextEntryOffset);
                continue;
            }
            // Read a value from data field or seek to the value offset which is stored in data
            // field if the size of the entry value is bigger than 4.
            int byteCount = numberOfComponents * IFD_FORMAT_BYTES_PER_FORMAT[dataFormat];
            if (byteCount > 4) {
                long offset = dataInputStream.readUnsignedInt();
                if (DEBUG_INTERNAL) {
                    logDebug( "seek to data offset: " + offset);
                }
                if (offset + byteCount <= dataInputStream.mLength) {
                    dataInputStream.seek(offset);
                } else {
                     // Skip if invalid data offset.
                    logWarn( "Skip tag entry[offset invalid " + offset + "(0x" +
                            Long.toHexString(offset) +")]: "
                            + getContextDebugMessage(hint, tagNumber, dataFormat
                            , numberOfComponents, tag));
                    dataInputStream.seek(nextEntryOffset);
                    continue;
                }
            }
            // Recursively parse IFD when a IFD pointer tag appears.
            int innerIfdHint = getIfdHintFromTagNumber(tagNumber);
            if (DEBUG_INTERNAL) {
                logDebug( "innerIfdHint: " + innerIfdHint + " byteCount: " + byteCount);
            }
            if (innerIfdHint >= 0) {
                long offset = -1L;
                // Get offset from data field
                switch (dataFormat) {
                    case IFD_FORMAT_USHORT: {
                        offset = dataInputStream.readUnsignedShort();
                        break;
                    }
                    case IFD_FORMAT_SSHORT: {
                        offset = dataInputStream.readShort();
                        break;
                    }
                    case IFD_FORMAT_ULONG: {
                        offset = dataInputStream.readUnsignedInt();
                        break;
                    }
                    case IFD_FORMAT_SLONG: {
                        offset = dataInputStream.readInt();
                        break;
                    }
                    default: {
                        // Nothing to do
                        break;
                    }
                }
                if (DEBUG_INTERNAL) {
                    logDebug( String.format("Offset: %d, tagName: %s", offset, tag.name));
                }
                if (offset > 0L && offset < dataInputStream.mLength) {
                    dataInputStream.seek(offset);
                    readImageFileDirectory(dataInputStream, innerIfdHint);
                } else {
                    logWarn( "Skip jump into IFD [offset invalid " + offset + "(0x" +
                            Long.toHexString(offset) +")]: "
                            + getContextDebugMessage(hint, tagNumber, dataFormat
                            , numberOfComponents, tag));

                }
                dataInputStream.seek(nextEntryOffset);
                continue;
            }
            byte[] bytes = new byte[numberOfComponents * IFD_FORMAT_BYTES_PER_FORMAT[dataFormat]];
            dataInputStream.readFully(bytes);

            if ((tag.secondaryFormat == dataFormat) && (tag.primaryFormat == IFD_FORMAT_UCS2LE_STRING)) {
                dataFormat = IFD_FORMAT_UCS2LE_STRING;
            }
            if ((tag.secondaryFormat == dataFormat) && (tag.primaryFormat == IFD_FORMAT_PREFIX_STRING)) {
                dataFormat = IFD_FORMAT_PREFIX_STRING;
            }
            setAttribute(hint,
                    tag.name, new ExifAttribute(tag, dataFormat, numberOfComponents, bytes));
            if (dataInputStream.peek() != nextEntryOffset) {
                dataInputStream.seek(nextEntryOffset);
            }
        }
        if (dataInputStream.peek() + 4 <= dataInputStream.mLength) {
            long nextIfdOffset = dataInputStream.readUnsignedInt();
            if (DEBUG_INTERNAL) {
                logDebug( String.format("nextIfdOffset: %d", nextIfdOffset));
            }
            // The next IFD offset needs to be bigger than 8
            // since the first IFD offset is at least 8.
            if (nextIfdOffset > 8 && nextIfdOffset < dataInputStream.mLength) {
                dataInputStream.seek(nextIfdOffset);
                readImageFileDirectory(dataInputStream, IFD_THUMBNAIL_HINT);
            }
        }
    }

    private static String getContextDebugMessage(int hint, int tagNumber, int dataFormat, long numberOfComponents, ExifTag tag) {
        return String.format("hint: %d, tagNumber: %d(%05X), tagName: %s, dataFormat: %d(%s), " +
                        "numberOfComponents: %d", hint, tagNumber, tagNumber, tag != null ? tag.name : null,
                dataFormat,ExifAttribute.getFormatName(dataFormat), numberOfComponents);
    }

    // Gets the corresponding IFD group index of the given tag number for writing Exif Tags.
    private static int getIfdHintFromTagNumber(int tagNumber) {
        for (int i = 0; i < IFD_POINTER_TAG_HINTS.length; ++i) {
            if (IFD_POINTER_TAGS[i].id == tagNumber) {
                return IFD_POINTER_TAG_HINTS[i];
            }
        }
        return -1;
    }
    // Writes an Exif segment into the given output stream.
    private int writeExifSegment(ByteOrderAwarenessDataOutputStream dataOutputStream,
            int exifOffsetFromBeginning, byte[] thumbnail) throws IOException {
        // The following variables are for calculating each IFD tag group size in bytes.
        int[] ifdOffsets = new int[EXIF_TAGS.length];
        int[] ifdDataSizes = new int[EXIF_TAGS.length];
        // Remove IFD pointer tags (we'll re-add it later.)
        for (ExifTag tag : IFD_POINTER_TAGS) {
            removeAttribute(tag.name);
        }
        // Remove old thumbnail data
        removeAttribute(JPEG_INTERCHANGE_FORMAT_TAG.name);
        removeAttribute(JPEG_INTERCHANGE_FORMAT_LENGTH_TAG.name);
        // Remove null value tags.
        for (int hint = 0; hint < EXIF_TAGS.length; ++hint) {
            for (Object obj : mAttributes[hint].entrySet().toArray()) {
                final Map.Entry entry = (Map.Entry) obj;
                if (entry.getValue() == null) {
                    mAttributes[hint].remove(entry.getKey());
                }
            }
        }
        // Add IFD pointer tags. The next offset of primary image TIFF IFD will have thumbnail IFD
        // offset when there is one or more tags in the thumbnail IFD.
        if (!mAttributes[IFD_INTEROPERABILITY_HINT].isEmpty()) {
            setAttribute(IFD_EXIF_HINT,IFD_POINTER_TAGS[2].name,
                    ExifAttribute.createULong(IFD_POINTER_TAGS[2], 0, mExifByteOrder));
        }
        if (!mAttributes[IFD_EXIF_HINT].isEmpty()) {
            setAttribute(IFD_TIFF_HINT,IFD_POINTER_TAGS[0].name,
                    ExifAttribute.createULong(IFD_POINTER_TAGS[0], 0, mExifByteOrder));
        }
        if (!mAttributes[IFD_GPS_HINT].isEmpty()) {
            setAttribute(IFD_TIFF_HINT,IFD_POINTER_TAGS[1].name,
                    ExifAttribute.createULong(IFD_POINTER_TAGS[1], 0, mExifByteOrder));
        }
        if (mHasThumbnail) {
            setAttribute(IFD_TIFF_HINT,JPEG_INTERCHANGE_FORMAT_TAG.name,
                    ExifAttribute.createULong(JPEG_INTERCHANGE_FORMAT_TAG, 0, mExifByteOrder));
            setAttribute(IFD_TIFF_HINT,JPEG_INTERCHANGE_FORMAT_LENGTH_TAG.name,
                    ExifAttribute.createULong(JPEG_INTERCHANGE_FORMAT_LENGTH_TAG, mThumbnailLength, mExifByteOrder));
        }
        // Calculate IFD group data area sizes. IFD group data area is assigned to save the entry
        // value which has a bigger size than 4 bytes.
        for (int i = 0; i < EXIF_TAGS.length; ++i) {
            int sum = 0;
            for (Map.Entry entry : mAttributes[i].entrySet()) {
                final ExifAttribute exifAttribute = (ExifAttribute) entry.getValue();
                final int size = exifAttribute.size();
                if (size > 4) {
                    sum += size;
                }
            }
            ifdDataSizes[i] += sum;
        }
        // Calculate IFD offsets.
        int position = 8;
        for (int hint = 0; hint < EXIF_TAGS.length; ++hint) {
            if (!mAttributes[hint].isEmpty()) {
                ifdOffsets[hint] = position;
                position += 2 + mAttributes[hint].size() * 12 + 4 + ifdDataSizes[hint];
            }
        }
        if (mHasThumbnail) {
            int thumbnailOffset = position;
            setAttribute(IFD_TIFF_HINT,JPEG_INTERCHANGE_FORMAT_TAG.name,
                    ExifAttribute.createULong(JPEG_INTERCHANGE_FORMAT_TAG, thumbnailOffset, mExifByteOrder));
            mThumbnailOffset = exifOffsetFromBeginning + thumbnailOffset;
            position += mThumbnailLength;
        }
        // Calculate the total size
        int totalSize = position + 8;  // eight bytes is for header part.
        if (DEBUG_INTERNAL) {
            logDebug( "totalSize length: " + totalSize);
            for (int i = 0; i < EXIF_TAGS.length; ++i) {
                logDebug( String.format("index: %d, offsets: %d, tag count: %d, data sizes: %d",
                        i, ifdOffsets[i], mAttributes[i].size(), ifdDataSizes[i]));
            }
        }
        // Update IFD pointer tags with the calculated offsets.
        if (!mAttributes[IFD_EXIF_HINT].isEmpty()) {
            setAttribute(IFD_TIFF_HINT,IFD_POINTER_TAGS[0].name,
                    ExifAttribute.createULong(IFD_POINTER_TAGS[0], ifdOffsets[IFD_EXIF_HINT], mExifByteOrder));
        }
        if (!mAttributes[IFD_GPS_HINT].isEmpty()) {
            setAttribute(IFD_TIFF_HINT,IFD_POINTER_TAGS[1].name,
                    ExifAttribute.createULong(IFD_POINTER_TAGS[1], ifdOffsets[IFD_GPS_HINT], mExifByteOrder));
        }
        if (!mAttributes[IFD_INTEROPERABILITY_HINT].isEmpty()) {
            setAttribute(IFD_EXIF_HINT,IFD_POINTER_TAGS[2].name, ExifAttribute.createULong(
                    IFD_POINTER_TAGS[2], ifdOffsets[IFD_INTEROPERABILITY_HINT], mExifByteOrder));
        }
        // Write TIFF Headers. See JEITA CP-3451C Table 1. page 10.
        dataOutputStream.writeUnsignedShort(totalSize);
        dataOutputStream.write(IDENTIFIER_EXIF_APP1);
        dataOutputStream.writeShort(mExifByteOrder == ByteOrder.BIG_ENDIAN
                ? BYTE_ALIGN_MM : BYTE_ALIGN_II);
        dataOutputStream.setByteOrder(mExifByteOrder);
        dataOutputStream.writeUnsignedShort(0x2a);
        dataOutputStream.writeUnsignedInt(8);
        // Write IFD groups. See JEITA CP-3451C Figure 7. page 12.
        for (int hint = 0; hint < EXIF_TAGS.length; ++hint) {
            HashMap<String, ExifAttribute> mAttributeSegment = mAttributes[hint];
            int segmentSize = mAttributeSegment.size();
            if (segmentSize > 0) {
                // See JEITA CP-3451C 4.6.2 IFD structure. page 13.
                // Write entry count
                dataOutputStream.writeUnsignedShort(segmentSize);
                // Write entry info
                int dataOffset = ifdOffsets[hint] + 2 + segmentSize * 12 + 4;
                ExifAttribute[] values = mAttributeSegment.values().toArray(new ExifAttribute[segmentSize]);

                // in jpg file values must be sorted by exif id
                Arrays.sort(values);
                for (final ExifAttribute attribute : values) {
                    final ExifTag tag = attribute.exifTag;
                    final int tagNumber = tag.id;
                    final int size = attribute.size();
                    dataOutputStream.writeUnsignedShort(tagNumber);

                    // k3b: IFD_FORMAT_UCS2LE_STRING not supported by exif. use byte instead
                    int format = attribute.format;
                    if ((format == IFD_FORMAT_UCS2LE_STRING) || (format == IFD_FORMAT_PREFIX_STRING)) {
                        format = tag.secondaryFormat;
                    }

                    dataOutputStream.writeUnsignedShort(format);
                    dataOutputStream.writeInt(attribute.numberOfComponents);
                    if (size > 4) {
                        dataOutputStream.writeUnsignedInt(dataOffset);
                        dataOffset += size;
                    } else {
                        dataOutputStream.write(attribute.bytes);
                        // Fill zero up to 4 bytes
                        if (size < 4) {
                            for (int i = size; i < 4; ++i) {
                                dataOutputStream.writeByte(0);
                            }
                        }
                    }
                }
                // Write the next offset. It writes the offset of thumbnail IFD if there is one or
                // more tags in the thumbnail IFD when the current IFD is the primary image TIFF
                // IFD; Otherwise 0.
                if (hint == 0 && !mAttributes[IFD_THUMBNAIL_HINT].isEmpty()) {
                    dataOutputStream.writeUnsignedInt(ifdOffsets[IFD_THUMBNAIL_HINT]);
                } else {
                    dataOutputStream.writeUnsignedInt(0);
                }
                // Write values of data field exceeding 4 bytes after the next offset.
                for (final ExifAttribute attribute : values) {
                    if (attribute.bytes.length > 4) {
                        dataOutputStream.write(attribute.bytes, 0, attribute.bytes.length);
                    }
                }
            }
        }

        if (thumbnail != null) {
            dataOutputStream.write(thumbnail);
        }

        // Reset the byte order to big endian in order to write remaining parts of the JPEG file.
        dataOutputStream.setByteOrder(ByteOrder.BIG_ENDIAN);
        return totalSize;
    }
    /**
     * Determines the data format of EXIF entry value.
     *
     * @param entryValue The value to be determined.
     * @return Returns two data formats gussed as a pair in integer. If there is no two candidate
               data formats for the given entry value, returns {@code -1} in the second of the pair.
     */
    private static IntPair guessDataFormat(String entryValue) {
        // See TIFF 6.0 spec Types. page 15.
        // Take the first component if there are more than one component.
        if (entryValue.contains(",")) {
            String[] entryValues = entryValue.split(",");
            IntPair dataFormat = guessDataFormat(entryValues[0]);
            if (dataFormat.first == IFD_FORMAT_STRING) {
                return dataFormat;
            }
            for (int i = 1; i < entryValues.length; ++i) {
                final IntPair guessDataFormat = guessDataFormat(entryValues[i]);
                int first = -1, second = -1;
                if (guessDataFormat.first == dataFormat.first
                        || guessDataFormat.second == dataFormat.first) {
                    first = dataFormat.first;
                }
                if (dataFormat.second != -1 && (guessDataFormat.first == dataFormat.second
                        || guessDataFormat.second == dataFormat.second)) {
                    second = dataFormat.second;
                }
                if (first == -1 && second == -1) {
                    return new IntPair(IFD_FORMAT_STRING, -1);
                }
                if (first == -1) {
                    dataFormat = new IntPair(second, -1);
                    continue;
                }
                if (second == -1) {
                    dataFormat = new IntPair(first, -1);
                    continue;
                }
            }
            return dataFormat;
        }
        if (entryValue.contains("/")) {
            String[] rationalNumber = entryValue.split("/");
            if (rationalNumber.length == 2) {
                try {
                    long numerator = Long.parseLong(rationalNumber[0]);
                    long denominator = Long.parseLong(rationalNumber[1]);
                    if (numerator < 0L || denominator < 0L) {
                        return new IntPair(IFD_FORMAT_SRATIONAL, - 1);
                    }
                    if (numerator > Integer.MAX_VALUE || denominator > Integer.MAX_VALUE) {
                        return new IntPair(IFD_FORMAT_URATIONAL, -1);
                    }
                    return new IntPair(IFD_FORMAT_SRATIONAL, IFD_FORMAT_URATIONAL);
                } catch (NumberFormatException e)  {
                    // Ignored
                }
            }
            return new IntPair(IFD_FORMAT_STRING, -1);
        }
        try {
            Long longValue = Long.parseLong(entryValue);
            if (longValue >= 0 && longValue <= 65535) {
                return new IntPair(IFD_FORMAT_USHORT, IFD_FORMAT_ULONG);
            }
            if (longValue < 0) {
                return new IntPair(IFD_FORMAT_SLONG, -1);
            }
            return new IntPair(IFD_FORMAT_ULONG, -1);
        } catch (NumberFormatException e) {
            // Ignored
        }
        try {
            Double.parseDouble(entryValue);
            return new IntPair(IFD_FORMAT_DOUBLE, -1);
        } catch (NumberFormatException e) {
            // Ignored
        }
        return new IntPair(IFD_FORMAT_STRING, -1);
    }
    // An input stream to parse EXIF data area, which can be written in either little or big endian
    // order.
    private static class ByteOrderAwarenessDataInputStream extends ByteArrayInputStream {
        private static final ByteOrder LITTLE_ENDIAN = ByteOrder.LITTLE_ENDIAN;
        private static final ByteOrder BIG_ENDIAN = ByteOrder.BIG_ENDIAN;
        private ByteOrder mByteOrder = ByteOrder.BIG_ENDIAN;
        private final long mLength;
        private long mPosition;
        public ByteOrderAwarenessDataInputStream(byte[] bytes) {
            super(bytes);
            mLength = bytes.length;
            mPosition = 0L;
        }
        public void setByteOrder(ByteOrder byteOrder) {
            mByteOrder = byteOrder;
        }
        public void seek(long byteCount) throws IOException {
            mPosition = 0L;
            reset();
            if (skip(byteCount) != byteCount) {
                throw new IOException("Couldn't seek up to the byteCount");
            }
        }
        public long peek() {
            return mPosition;
        }
        public void readFully(byte[] buffer) throws IOException {
            mPosition += buffer.length;
            if (mPosition > mLength) {
                throw new EOFException();
            }
            if (super.read(buffer, 0, buffer.length) != buffer.length) {
                throw new IOException("Couldn't read up to the length of buffer");
            }
        }
        public byte readByte() throws IOException {
            ++mPosition;
            if (mPosition > mLength) {
                throw new EOFException();
            }
            int ch = super.read();
            if (ch < 0) {
                throw new EOFException();
            }
            return (byte) ch;
        }
        public short readShort() throws IOException {
            mPosition += 2;
            if (mPosition > mLength) {
                throw new EOFException();
            }
            int ch1 = super.read();
            int ch2 = super.read();
            if ((ch1 | ch2) < 0) {
                throw new EOFException();
            }
            if (mByteOrder == LITTLE_ENDIAN) {
                return (short) ((ch2 << 8) + (ch1));
            } else if (mByteOrder == BIG_ENDIAN) {
                return (short) ((ch1 << 8) + (ch2));
            }
            throw new IOException("Invalid byte order: " + mByteOrder);
        }
        public int readInt() throws IOException {
            mPosition += 4;
            if (mPosition > mLength) {
                throw new EOFException();
            }
            int ch1 = super.read();
            int ch2 = super.read();
            int ch3 = super.read();
            int ch4 = super.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) {
                throw new EOFException();
            }
            if (mByteOrder == LITTLE_ENDIAN) {
                return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + ch1);
            } else if (mByteOrder == BIG_ENDIAN) {
                return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + ch4);
            }
            throw new IOException("Invalid byte order: " + mByteOrder);
        }
        @Override
        public long skip(long byteCount) {
            long skipped = super.skip(Math.min(byteCount, mLength - mPosition));
            mPosition += skipped;
            return skipped;
        }
        public int readUnsignedShort() throws IOException {
            mPosition += 2;
            if (mPosition > mLength) {
                throw new EOFException();
            }
            int ch1 = super.read();
            int ch2 = super.read();
            if ((ch1 | ch2) < 0) {
                throw new EOFException();
            }
            if (mByteOrder == LITTLE_ENDIAN) {
                return ((ch2 << 8) + (ch1));
            } else if (mByteOrder == BIG_ENDIAN) {
                return ((ch1 << 8) + (ch2));
            }
            throw new IOException("Invalid byte order: " + mByteOrder);
        }
        public long readUnsignedInt() throws IOException {
            return readInt() & 0xffffffffL;
        }
        public long readLong() throws IOException {
            mPosition += 8;
            if (mPosition > mLength) {
                throw new EOFException();
            }
            int ch1 = super.read();
            int ch2 = super.read();
            int ch3 = super.read();
            int ch4 = super.read();
            int ch5 = super.read();
            int ch6 = super.read();
            int ch7 = super.read();
            int ch8 = super.read();
            if ((ch1 | ch2 | ch3 | ch4 | ch5 | ch6 | ch7 | ch8) < 0) {
                throw new EOFException();
            }
            if (mByteOrder == LITTLE_ENDIAN) {
                return (((long) ch8 << 56) + ((long) ch7 << 48) + ((long) ch6 << 40)
                        + ((long) ch5 << 32) + ((long) ch4 << 24) + ((long) ch3 << 16)
                        + ((long) ch2 << 8) + ch1);
            } else if (mByteOrder == BIG_ENDIAN) {
                return (((long) ch1 << 56) + ((long) ch2 << 48) + ((long) ch3 << 40)
                        + ((long) ch4 << 32) + ((long) ch5 << 24) + ((long) ch6 << 16)
                        + ((long) ch7 << 8) + ch8);
            }
            throw new IOException("Invalid byte order: " + mByteOrder);
        }
        public float readFloat() throws IOException {
            return Float.intBitsToFloat(readInt());
        }
        public double readDouble() throws IOException {
            return Double.longBitsToDouble(readLong());
        }
    }
    // An output stream to write EXIF data area, which can be written in either little or big endian
    // order.
    private static class ByteOrderAwarenessDataOutputStream extends FilterOutputStream {
        private final OutputStream mOutputStream;
        private ByteOrder mByteOrder;
        public ByteOrderAwarenessDataOutputStream(OutputStream out, ByteOrder byteOrder) {
            super(out);
            mOutputStream = out;
            mByteOrder = byteOrder;
        }
        public void setByteOrder(ByteOrder byteOrder) {
            mByteOrder = byteOrder;
        }
        public void write(byte[] bytes) throws IOException {
            mOutputStream.write(bytes);
        }
        public void write(byte[] bytes, int offset, int length) throws IOException {
            mOutputStream.write(bytes, offset, length);
        }
        public void writeByte(int val) throws IOException {
            mOutputStream.write(val);
        }
        public void writeShort(short val) throws IOException {
            if (mByteOrder == ByteOrder.LITTLE_ENDIAN) {
                mOutputStream.write((val >>> 0) & 0xFF);
                mOutputStream.write((val >>> 8) & 0xFF);
            } else if (mByteOrder == ByteOrder.BIG_ENDIAN) {
                mOutputStream.write((val >>> 8) & 0xFF);
                mOutputStream.write((val >>> 0) & 0xFF);
            }
        }
        public void writeInt(int val) throws IOException {
            if (mByteOrder == ByteOrder.LITTLE_ENDIAN) {
                mOutputStream.write((val >>> 0) & 0xFF);
                mOutputStream.write((val >>> 8) & 0xFF);
                mOutputStream.write((val >>> 16) & 0xFF);
                mOutputStream.write((val >>> 24) & 0xFF);
            } else if (mByteOrder == ByteOrder.BIG_ENDIAN) {
                mOutputStream.write((val >>> 24) & 0xFF);
                mOutputStream.write((val >>> 16) & 0xFF);
                mOutputStream.write((val >>> 8) & 0xFF);
                mOutputStream.write((val >>> 0) & 0xFF);
            }
        }
        public void writeUnsignedShort(int val) throws IOException {
            writeShort((short) val);
        }
        public void writeUnsignedInt(long val) throws IOException {
            writeInt((int) val);
        }
    }

    // import android.util.Log;

    private static void logWarn(String msg, Throwable e) {
        if (DEBUG) {
            logger.warn(msg, e);
            // Log.w(LOG_TAG, msg, e);
        }
    }
    private static void logWarn(String msg) {
        if (DEBUG) {
            logger.warn(msg);
            // Log.w(LOG_TAG, msg);
        }
    }
    private static void logDebug(String msg) {
        logger.debug(msg);
        // Log.d(LOG_TAG, msg);
    }

    protected static byte[] encodePrefixString(String value) {
        boolean allASCII = isAllASCII(value);
        final byte[] bytes = (value + "\0").getBytes(allASCII ? ASCII : UTF16);
        final byte[] bytesWithPrefix = new byte[EXIF_UNICODE_PREFIX.length + bytes.length];
        System.arraycopy(allASCII ? EXIF_ASCII_PREFIX : EXIF_UNICODE_PREFIX, 0, bytesWithPrefix, 0, EXIF_UNICODE_PREFIX.length);
        System.arraycopy(bytes, 0, bytesWithPrefix, EXIF_UNICODE_PREFIX.length, bytes.length);
        return bytesWithPrefix;
    }

    private static boolean isAllASCII(String input) {
        if (input == null) return true;
        for (int i = 0; i < input.length(); i++) {
            int c = input.charAt(i);
            if (c > 0x7F) {
                return false;
            }
        }
        return true;
    }

    protected static String decodePrefixString(int numberOfComponents, byte[] bytes, Charset defaultCharSet) {
        String result = decodePrefixStringInternal(numberOfComponents, bytes, defaultCharSet);
        if (result != null) {
            int last = result.length() - 1;
            while ((last >= 0) && (result.charAt(last) == 0)) {
                last--; // remove "\0" at end
            }
            return result.substring(0, last+1);
        }
        return null;
    }

    /** decodes exif usercomment and other strings*/
    private static String decodePrefixStringInternal(int numberOfComponents, byte[] bytes, Charset defaultCharSet) {
        try {
            if ((numberOfComponents >= EXIF_ASCII_PREFIX.length) && (bytes[EXIF_ASCII_PREFIX.length-1] == 0)) {
                if (startsWith(bytes, EXIF_ASCII_PREFIX)) {
                    return new String(bytes, 8, numberOfComponents - 8, ASCII);
                } else if (startsWith(bytes, EXIF_UNICODE_PREFIX)) {
                    return new String(bytes, 8, numberOfComponents - 8, UTF16);
                } else if (startsWith(bytes, EXIF_JIS_PREFIX)) {
                    return new String(bytes, 8, numberOfComponents - 8, "EUC-JP");
                }
            }
            return new String(bytes, 0, numberOfComponents, defaultCharSet);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "???";
    }

    private static boolean startsWith(byte[] content, byte[] prefix) {
        for (int i = 0; i < prefix.length; ++i) {
            if (content[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}

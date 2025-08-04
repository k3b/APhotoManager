/*
 * Copyright 2024 The Android Open Source Project
 * Copyright 2024 Tom Geiselmann <tomgapplicationsdevelopment@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.tommygeenexus.exifinterfaceextended;

import android.content.res.Resources;

import androidx.annotation.Nullable;
import androidx.annotation.RawRes;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;

import io.github.tommygeenexus.exifinterfaceextended.test.R;

import java.io.IOException;
import java.io.InputStreamReader;

/** Expected Exif attributes for test images in the res/raw/ directory. */
final class ExpectedAttributes {

    /** Expected attributes for {@link R.raw#jpeg_with_exif_byte_order_ii}. */
    public static final ExpectedAttributes JPEG_WITH_EXIF_BYTE_ORDER_II =
            new Builder()
                    .setThumbnailOffsetAndLength(3500, 6265)
                    .setThumbnailSize(512, 288)
                    .setIsThumbnailCompressed(true)
                    .setMake("SAMSUNG")
                    .setMakeOffset(160)
                    .setModel("SM-N900S")
                    .setAperture(2.2)
                    .setDateTimeOriginal("2016:01:29 18:32:27")
                    .setExposureTime(1.0 / 30)
                    .setFocalLength("413/100")
                    .setImageSize(640, 480)
                    .setIso("50")
                    .setOrientation(ExifInterfaceExtended.ORIENTATION_ROTATE_90)
                    .build();

    /**
     * Expected attributes for {@link R.raw#jpeg_with_exif_byte_order_ii} when only the Exif data is
     * read using {@link ExifInterfaceExtended#STREAM_TYPE_EXIF_DATA_ONLY}.
     */
    public static final ExpectedAttributes JPEG_WITH_EXIF_BYTE_ORDER_II_STANDALONE =
            JPEG_WITH_EXIF_BYTE_ORDER_II
                    .buildUpon()
                    .setThumbnailOffset(JPEG_WITH_EXIF_BYTE_ORDER_II.getThumbnailOffset() - 6)
                    .setMakeOffset(JPEG_WITH_EXIF_BYTE_ORDER_II.getMakeOffset() - 6)
                    .build();

    /** Expected attributes for {@link R.raw#jpeg_with_exif_byte_order_mm}. */
    public static final ExpectedAttributes JPEG_WITH_EXIF_BYTE_ORDER_MM =
            new Builder()
                    .setGpsLatitudeOffsetAndLength(584, 24)
                    .setComputedLatLong(0, 0)
                    .setComputedAltitude(0)
                    .setMake("LGE")
                    .setMakeOffset(414)
                    .setModel("Nexus 5")
                    .setAperture(2.4)
                    .setDateTimeOriginal("2016:01:29 15:44:58")
                    .setExposureTime(1.0 / 60)
                    .setFocalLength("3970/1000")
                    .setGpsAltitude("0/1000")
                    .setGpsAltitudeRef("0")
                    .setGpsDatestamp("1970:01:01")
                    .setGpsLatitude("0/1,0/1,0/10000")
                    .setGpsLatitudeRef("N")
                    .setGpsLongitude("0/1,0/1,0/10000")
                    .setGpsLongitudeRef("E")
                    .setGpsProcessingMethod("GPS")
                    .setGpsTimestamp("00:00:00")
                    .setImageSize(144, 176)
                    .setIso("146")
                    .build();

    /**
     * Expected attributes for {@link R.raw#jpeg_with_exif_byte_order_mm} when only the Exif data is
     * read using {@link ExifInterfaceExtended#STREAM_TYPE_EXIF_DATA_ONLY}.
     */
    public static final ExpectedAttributes JPEG_WITH_EXIF_BYTE_ORDER_MM_STANDALONE =
            JPEG_WITH_EXIF_BYTE_ORDER_MM
                    .buildUpon()
                    .setGpsLatitudeOffset(JPEG_WITH_EXIF_BYTE_ORDER_MM.getGpsLatitudeOffset() - 6)
                    .setMakeOffset(JPEG_WITH_EXIF_BYTE_ORDER_MM.getMakeOffset() - 6)
                    .setImageSize(0, 0)
                    .build();

    /** Expected attributes for {@link R.raw#jpeg_with_exif_invalid_offset}. */
    public static final ExpectedAttributes JPEG_WITH_EXIF_INVALID_OFFSET =
            JPEG_WITH_EXIF_BYTE_ORDER_MM
                    .buildUpon()
                    .setAperture(0)
                    .setDateTimeOriginal(null)
                    .setExposureTime(0)
                    .setFocalLength(null)
                    .setIso(null)
                    .build();

    /** Expected attributes for {@link R.raw#dng_with_exif_with_xmp}. */
    public static final ExpectedAttributes DNG_WITH_EXIF_WITH_XMP =
            new Builder()
                    .setThumbnailOffsetAndLength(12570, 15179)
                    .setThumbnailSize(256, 144)
                    .setIsThumbnailCompressed(true)
                    .setGpsLatitudeOffsetAndLength(12486, 24)
                    .setComputedLatLong(53.83450833333334, 10.69585)
                    .setComputedAltitude(0)
                    .setMake("LGE")
                    .setMakeOffset(102)
                    .setModel("LG-H815")
                    .setAperture(1.8)
                    .setDateTimeOriginal("2015:11:12 16:46:18")
                    .setExposureTime(0.0040)
                    .setFocalLength("442/100")
                    .setGpsDatestamp("1970:01:17")
                    .setGpsLatitude("53/1,50/1,423/100")
                    .setGpsLatitudeRef("N")
                    .setGpsLongitude("10/1,41/1,4506/100")
                    .setGpsLongitudeRef("E")
                    .setGpsTimestamp("18:08:10")
                    .setImageSize(600, 337)
                    .setIso("800")
                    .setXmpResourceId(R.raw.dng_xmp)
                    .setXmpOffsetAndLength(826, 10067)
                    .build();

    /** Expected attributes for {@link R.raw#jpeg_with_exif_with_xmp}. */
    public static final ExpectedAttributes JPEG_WITH_EXIF_WITH_XMP =
            DNG_WITH_EXIF_WITH_XMP
                    .buildUpon()
                    .clearThumbnail()
                    .setGpsLatitudeOffset(1692)
                    .setMakeOffset(84)
                    .setOrientation(ExifInterfaceExtended.ORIENTATION_NORMAL)
                    .setXmpResourceId(R.raw.jpeg_xmp)
                    .setXmpOffsetAndLength(1809, 13197)
                    .build();

    /** Expected attributes for {@link R.raw#png_with_exif_byte_order_ii}. */
    public static final ExpectedAttributes PNG_WITH_EXIF_BYTE_ORDER_II =
            JPEG_WITH_EXIF_BYTE_ORDER_II
                    .buildUpon()
                    .setThumbnailOffset(212271)
                    .setMakeOffset(211525)
                    .setFocalLength("41/10")
                    .setXmpResourceId(R.raw.png_xmp)
                    .setXmpOffsetAndLength(352, 1409)
                    .build();

    /** Expected attributes for {@link R.raw#webp_with_exif}. */
    public static final ExpectedAttributes WEBP_WITH_EXIF =
            JPEG_WITH_EXIF_BYTE_ORDER_II
                    .buildUpon()
                    .setThumbnailOffset(9646)
                    .setMakeOffset(6306)
                    .build();

    /** Expected attributes for {@link R.raw#invalid_webp_with_jpeg_app1_marker}. */
    public static final ExpectedAttributes INVALID_WEBP_WITH_JPEG_APP1_MARKER =
            new Builder()
                    .setOrientation(ExifInterfaceExtended.ORIENTATION_ROTATE_270)
                    .setHasIccProfile(true)
                    .build();

    /** Expected attributes for {@link R.raw#heic_with_exif} when read on a device below API 31. */
    public static final ExpectedAttributes HEIC_WITH_EXIF_BELOW_API_31 =
            new Builder()
                    .setMake("LGE")
                    .setMakeOffset(3519)
                    .setModel("Nexus 5")
                    .setImageSize(1920, 1080)
                    .setOrientation(ExifInterfaceExtended.ORIENTATION_NORMAL)
                    .build();

    /**
     * Expected attributes for {@link R.raw#heic_with_exif} when read on a device running API 31 or
     * above.
     */
    public static final ExpectedAttributes HEIC_WITH_EXIF_API_31_AND_ABOVE =
            HEIC_WITH_EXIF_BELOW_API_31
                    .buildUpon()
                    .setXmpResourceId(R.raw.heic_xmp)
                    .setXmpOffsetAndLength(3721, 3020)
                    .build();

    /** Expected attributes for {@link R.raw#jpeg_with_icc_with_exif_with_extended_xmp}. */
    public static final ExpectedAttributes JPEG_WITH_ICC_WITH_EXIF_WITH_EXTENDED_XMP =
            new Builder()
                    .setMake("Google")
                    .setMakeOffset(170)
                    .setModel("Pixel 3a")
                    .setAperture(1.8)
                    .setDateTimeOriginal("2021:01:07 11:38:23")
                    .setExposureTime(Double.parseDouble("5.11e-4"))
                    .setFocalLength("4440/1000")
                    .setImageSize(4032, 3024)
                    .setIso("63")
                    .setOrientation(ExifInterfaceExtended.ORIENTATION_NORMAL)
                    .setXmpResourceId(R.raw.jpeg_xmp_3)
                    .setXmpOffsetAndLength(955, 322)
                    .setHasExtendedXmp(true)
                    .setHasIccProfile(true)
                    .setHasPhotoshopImageResources(false)
                    .build();

    /** Expected attributes for {@link R.raw#jpeg_with_exif_with_photoshop_with_xmp}. */
    public static final ExpectedAttributes JPEG_WITH_EXIF_WITH_PHOTSHOP_WITH_XMP =
            new Builder()
                    .setThumbnailSize(127, 160)
                    .setThumbnailOffsetAndLength(464, 6473)
                    .setImageSize(300, 379)
                    .setOrientation(ExifInterfaceExtended.ORIENTATION_NORMAL)
                    .setXmpResourceId(R.raw.jpeg_xmp_2)
                    .setXmpOffsetAndLength(15680, 19087)
                    .setHasExtendedXmp(false)
                    .setHasIccProfile(true)
                    .setHasPhotoshopImageResources(true)
                    .build();

    /** Expected attributes for {@link R.raw#webp_with_icc_with_exif_with_xmp}. */
    public static final ExpectedAttributes WEBP_WITH_ICC_WITH_EXIF_WITH_XMP =
            new Builder()
                    .setMake("Google")
                    .setMakeOffset(3285288)
                    .setModel("Pixel 3a")
                    .setAperture(1.8)
                    .setDateTimeOriginal("2021:01:07 11:38:23")
                    .setExposureTime(Double.parseDouble("5.11e-4"))
                    .setFocalLength("4440/1000")
                    .setImageSize(4032, 3024)
                    .setIso("63")
                    .setOrientation(ExifInterfaceExtended.ORIENTATION_NORMAL)
                    .setXmpResourceId(R.raw.jpeg_xmp_3)
                    .setXmpOffsetAndLength(3286048, 322)
                    .setHasExtendedXmp(false)
                    .setHasIccProfile(true)
                    .setHasPhotoshopImageResources(false)
                    .build();

    /**
     * Expected attributes for {@link R.raw#avif_with_exif}.
     */
    public static final ExpectedAttributes AVIF_WITH_EXIF =
            HEIC_WITH_EXIF_API_31_AND_ABOVE
                    .buildUpon()
                    .setMakeOffset(451)
                    .setXmpOffsetAndLength(653, 3020)
                    .build();

    public static class Builder {
        // Thumbnail information.
        private boolean mHasThumbnail;
        private long mThumbnailOffset;
        private long mThumbnailLength;
        private int mThumbnailWidth;
        private int mThumbnailHeight;
        private boolean mIsThumbnailCompressed;

        // GPS information.
        private boolean mHasLatLong;
        private double mComputedLatitude;
        private double mComputedLongitude;
        private double mComputedAltitude;
        @Nullable private String mGpsAltitude;
        @Nullable private String mGpsAltitudeRef;
        @Nullable private String mGpsDatestamp;
        @Nullable private String mGpsLatitude;
        private long mGpsLatitudeOffset;
        private long mGpsLatitudeLength;
        @Nullable private String mGpsLatitudeRef;
        @Nullable private String mGpsLongitude;
        @Nullable private String mGpsLongitudeRef;
        @Nullable private String mGpsProcessingMethod;
        @Nullable private String mGpsTimestamp;

        // Make information
        private long mMakeOffset;
        private long mMakeLength;
        @Nullable private String mMake;

        // Values.
        @Nullable private String mModel;
        private double mAperture;
        @Nullable private String mDateTimeOriginal;
        private double mExposureTime;
        private double mFlash;
        @Nullable private String mFocalLength;
        private int mImageLength;
        private int mImageWidth;
        @Nullable private String mIso;
        private int mOrientation;
        private int mWhiteBalance;

        // XMP information.
        private boolean mHasXmp;
        @Nullable private String mXmp;
        @Nullable private Integer mXmpResourceId;
        private long mXmpOffset;
        private long mXmpLength;
        private boolean mHasExtendedXmp;
        private boolean mHasIccProfile;
        private boolean mHasPhotoshopImageResources;

        Builder() {}

        private Builder(ExpectedAttributes attributes) {
            mHasThumbnail = attributes.hasThumbnail();
            mThumbnailOffset = attributes.getThumbnailOffset();
            mThumbnailLength = attributes.getThumbnailLength();
            mThumbnailWidth = attributes.getThumbnailWidth();
            mThumbnailHeight = attributes.getThumbnailHeight();
            mIsThumbnailCompressed = attributes.isIsThumbnailCompressed();
            mHasLatLong = attributes.hasLatLong();
            mComputedLatitude = attributes.getComputedLatitude();
            mComputedLongitude = attributes.getComputedLongitude();
            mComputedAltitude = attributes.getComputedAltitude();
            mGpsAltitude = attributes.getGpsAltitude();
            mGpsAltitudeRef = attributes.getGpsAltitudeRef();
            mGpsDatestamp = attributes.getGpsDatestamp();
            mGpsLatitude = attributes.getGpsLatitude();
            mGpsLatitudeOffset = attributes.getGpsLatitudeOffset();
            mGpsLatitudeLength = attributes.getGpsLatitudeLength();
            mGpsLatitudeRef = attributes.getGpsLatitudeRef();
            mGpsLongitude = attributes.getGpsLongitude();
            mGpsLongitudeRef = attributes.getGpsLongitudeRef();
            mGpsProcessingMethod = attributes.getGpsProcessingMethod();
            mGpsTimestamp = attributes.getGpsTimestamp();
            mMakeOffset = attributes.getMakeOffset();
            mMakeLength = attributes.getMakeLength();
            mMake = attributes.getMake();
            mModel = attributes.getModel();
            mAperture = attributes.getAperture();
            mDateTimeOriginal = attributes.getDateTimeOriginal();
            mExposureTime = attributes.getExposureTime();
            mFocalLength = attributes.getFocalLength();
            mImageLength = attributes.getImageLength();
            mImageWidth = attributes.getImageWidth();
            mIso = attributes.getIso();
            mOrientation = attributes.getOrientation();
            mHasXmp = attributes.hasXmp();
            mXmp = attributes.getXmp();
            mXmpResourceId = attributes.getXmpResourceId();
            mXmpOffset = attributes.getXmpOffset();
            mXmpLength = attributes.getXmpLength();
            mHasExtendedXmp = attributes.hasExtendedXmp();
            mHasIccProfile = attributes.hasIccProfile();
            mHasPhotoshopImageResources = attributes.hasPhotoshopImageResources();
        }

        public Builder setThumbnailSize(int width, int height) {
            mHasThumbnail = true;
            mThumbnailWidth = width;
            mThumbnailHeight = height;
            return this;
        }

        public Builder setIsThumbnailCompressed(boolean isThumbnailCompressed) {
            mHasThumbnail = true;
            mIsThumbnailCompressed = isThumbnailCompressed;
            return this;
        }

        public Builder setThumbnailOffsetAndLength(long offset, long length) {
            mHasThumbnail = true;
            mThumbnailOffset = offset;
            mThumbnailLength = length;
            return this;
        }

        public Builder setThumbnailOffset(long offset) {
            if (!mHasThumbnail) {
                throw new IllegalStateException(
                        "Thumbnail position in the file must first be set with "
                                + "setThumbnailOffsetAndLength(...)");
            }
            mThumbnailOffset = offset;
            return this;
        }

        public Builder clearThumbnail() {
            mHasThumbnail = false;
            mThumbnailWidth = 0;
            mThumbnailHeight = 0;
            mThumbnailOffset = 0;
            mThumbnailLength = 0;
            mIsThumbnailCompressed = false;
            return this;
        }

        public Builder setComputedLatLong(double computedLatitude, double computedLongitude) {
            mHasLatLong = true;
            mComputedLatitude = computedLatitude;
            mComputedLongitude = computedLongitude;
            return this;
        }

        public Builder clearComputedLatLong() {
            mHasLatLong = false;
            mComputedLatitude = 0;
            mComputedLongitude = 0;
            return this;
        }

        public Builder setGpsAltitude(@Nullable String gpsAltitude) {
            mGpsAltitude = gpsAltitude;
            return this;
        }

        public Builder setGpsAltitudeRef(@Nullable String gpsAltitudeRef) {
            mGpsAltitudeRef = gpsAltitudeRef;
            return this;
        }

        public Builder setGpsDatestamp(@Nullable String gpsDatestamp) {
            mGpsDatestamp = gpsDatestamp;
            return this;
        }

        public Builder setGpsLatitude(@Nullable String gpsLatitude) {
            mGpsLatitude = gpsLatitude;
            return this;
        }

        public Builder setGpsLatitudeOffsetAndLength(long offset, long length) {
            mHasLatLong = true;
            mGpsLatitudeOffset = offset;
            mGpsLatitudeLength = length;
            return this;
        }

        public Builder setGpsLatitudeOffset(long offset) {
            if (!mHasLatLong) {
                throw new IllegalStateException(
                        "Latitude position in the file must first be "
                                + "set with setLatitudeOffsetAndLength(...)");
            }
            mGpsLatitudeOffset = offset;
            return this;
        }

        public Builder setGpsLatitudeRef(@Nullable String gpsLatitudeRef) {
            mGpsLatitudeRef = gpsLatitudeRef;
            return this;
        }

        public Builder setGpsLongitude(@Nullable String gpsLongitude) {
            mGpsLongitude = gpsLongitude;
            return this;
        }

        public Builder setGpsLongitudeRef(@Nullable String gpsLongitudeRef) {
            mGpsLongitudeRef = gpsLongitudeRef;
            return this;
        }

        public Builder setGpsProcessingMethod(@Nullable String gpsProcessingMethod) {
            mGpsProcessingMethod = gpsProcessingMethod;
            return this;
        }

        public Builder setGpsTimestamp(@Nullable String gpsTimestamp) {
            mGpsTimestamp = gpsTimestamp;
            return this;
        }

        public Builder setComputedAltitude(double computedAltitude) {
            mComputedAltitude = computedAltitude;
            return this;
        }

        public Builder setMake(@Nullable String make) {
            if (make == null) {
                mMakeOffset = 0;
                mMakeLength = 0;
            } else {
                mMake = make;
                mMakeLength = make.length() + 1;
            }
            return this;
        }

        public Builder setMakeOffset(long offset) {
            if (mMake == null) {
                throw new IllegalStateException("Make must first be set with setMake(...)");
            }
            mMakeOffset = offset;
            return this;
        }

        public Builder setModel(@Nullable String model) {
            mModel = model;
            return this;
        }

        public Builder setAperture(double aperture) {
            mAperture = aperture;
            return this;
        }

        public Builder setDateTimeOriginal(@Nullable String dateTimeOriginal) {
            mDateTimeOriginal = dateTimeOriginal;
            return this;
        }

        public Builder setExposureTime(double exposureTime) {
            mExposureTime = exposureTime;
            return this;
        }

        public Builder setFlash(double flash) {
            mFlash = flash;
            return this;
        }

        public Builder setFocalLength(@Nullable String focalLength) {
            mFocalLength = focalLength;
            return this;
        }

        public Builder setImageSize(int imageWidth, int imageLength) {
            mImageWidth = imageWidth;
            mImageLength = imageLength;
            return this;
        }

        public Builder setIso(@Nullable String iso) {
            mIso = iso;
            return this;
        }

        public Builder setOrientation(int orientation) {
            mOrientation = orientation;
            return this;
        }

        public Builder setWhiteBalance(int whiteBalance) {
            mWhiteBalance = whiteBalance;
            return this;
        }

        /**
         * Sets the expected XMP data.
         *
         * <p>Clears any value set by {@link #setXmpResourceId}.
         */
        public Builder setXmp(String xmp) {
            mHasXmp = true;
            mXmp = xmp;
            mXmpResourceId = null;
            return this;
        }

        /**
         * Sets the resource ID of the expected XMP data.
         *
         * <p>Clears any value set by {@link #setXmp}.
         */
        public Builder setXmpResourceId(@RawRes int xmpResourceId) {
            mHasXmp = true;
            mXmp = null;
            mXmpResourceId = xmpResourceId;
            return this;
        }

        public Builder setXmpOffsetAndLength(int offset, int length) {
            mHasXmp = true;
            mXmpOffset = offset;
            mXmpLength = length;
            return this;
        }

        public Builder setXmpOffset(int offset) {
            if (!mHasXmp) {
                throw new IllegalStateException(
                        "XMP position in the file must first be set with"
                                + " setXmpOffsetAndLength(...)");
            }
            mXmpOffset = offset;
            return this;
        }

        public Builder clearXmp() {
            mHasXmp = false;
            mXmp = null;
            mXmpResourceId = null;
            mXmpOffset = 0;
            mXmpLength = 0;
            return this;
        }

        public Builder setHasExtendedXmp(boolean value) {
            mHasExtendedXmp = value;
            return this;
        }

        public Builder setHasIccProfile(boolean value) {
            mHasIccProfile = value;
            return this;
        }

        public Builder setHasPhotoshopImageResources(boolean value) {
            mHasPhotoshopImageResources = value;
            return this;
        }

        ExpectedAttributes build() {
            return new ExpectedAttributes(this);
        }
    }

    // Thumbnail information.
    private final boolean mHasThumbnail;
    private final int mThumbnailWidth;
    private final int mThumbnailHeight;
    private final boolean mIsThumbnailCompressed;
    private final long mThumbnailOffset;
    private final long mThumbnailLength;

    // GPS information.
    private final boolean mHasLatLong;
    private final double mComputedLatitude;
    private final double mComputedLongitude;
    private final double mComputedAltitude;
    @Nullable private final String mGpsAltitude;
    @Nullable private final String mGpsAltitudeRef;
    @Nullable private final String mGpsDatestamp;
    @Nullable private final String mGpsLatitude;
    private final long mGpsLatitudeOffset;
    private final long mGpsLatitudeLength;
    @Nullable private final String mGpsLatitudeRef;
    @Nullable private final String mGpsLongitude;
    @Nullable private final String mGpsLongitudeRef;
    @Nullable private final String mGpsProcessingMethod;
    @Nullable private final String mGpsTimestamp;

    // Make information
    private final long mMakeOffset;
    private final long mMakeLength;
    @Nullable private final String mMake;

    // Values.
    @Nullable private final String mModel;
    private final double mAperture;
    @Nullable private final String mDateTimeOriginal;
    private final double mExposureTime;
    @Nullable private final String mFocalLength;
    private final int mImageLength;
    private final int mImageWidth;
    @Nullable private final String mIso;
    private final int mOrientation;

    // XMP information.
    private final boolean mHasXmp;
    @Nullable private final String mXmp;
    @Nullable private final Integer mXmpResourceId;
    @Nullable private String mMemoizedXmp;
    private final long mXmpOffset;
    private final long mXmpLength;

    private final boolean mHasExtendedXmp;
    private final boolean mHasIccProfile;
    private final boolean mHasPhotoshopImageResources;

    private ExpectedAttributes(Builder builder) {
        mHasThumbnail = builder.mHasThumbnail;
        mThumbnailWidth = builder.mThumbnailWidth;
        mThumbnailHeight = builder.mThumbnailHeight;
        mIsThumbnailCompressed = builder.mIsThumbnailCompressed;
        mThumbnailOffset = builder.mThumbnailOffset;
        mThumbnailLength = builder.mThumbnailLength;
        mHasLatLong = builder.mHasLatLong;
        mComputedLatitude = builder.mComputedLatitude;
        mComputedLongitude = builder.mComputedLongitude;
        mComputedAltitude = builder.mComputedAltitude;
        mGpsAltitude = builder.mGpsAltitude;
        mGpsAltitudeRef = builder.mGpsAltitudeRef;
        mGpsDatestamp = builder.mGpsDatestamp;
        mGpsLatitude = builder.mGpsLatitude;
        mGpsLatitudeOffset = builder.mGpsLatitudeOffset;
        mGpsLatitudeLength = builder.mGpsLatitudeLength;
        mGpsLatitudeRef = builder.mGpsLatitudeRef;
        mGpsLongitude = builder.mGpsLongitude;
        mGpsLongitudeRef = builder.mGpsLongitudeRef;
        mGpsProcessingMethod = builder.mGpsProcessingMethod;
        mGpsTimestamp = builder.mGpsTimestamp;
        mMakeOffset = builder.mMakeOffset;
        mMakeLength = builder.mMakeLength;
        mMake = builder.mMake;
        mModel = builder.mModel;
        mAperture = builder.mAperture;
        mDateTimeOriginal = builder.mDateTimeOriginal;
        mExposureTime = builder.mExposureTime;
        mFocalLength = builder.mFocalLength;
        mImageLength = builder.mImageLength;
        mImageWidth = builder.mImageWidth;
        mIso = builder.mIso;
        mOrientation = builder.mOrientation;
        mHasXmp = builder.mHasXmp;
        mXmp = builder.mXmp;
        mXmpResourceId = builder.mXmpResourceId;
        Preconditions.checkArgument(
                mXmp == null || mXmpResourceId == null,
                "At most one of mXmp or mXmpResourceId may be set");
        mMemoizedXmp = mXmp;
        mXmpOffset = builder.mXmpOffset;
        mXmpLength = builder.mXmpLength;
        mHasExtendedXmp = builder.mHasExtendedXmp;
        mHasIccProfile = builder.mHasIccProfile;
        mHasPhotoshopImageResources = builder.mHasPhotoshopImageResources;
    }

    public Builder buildUpon() {
        return new Builder(this);
    }

    public boolean hasThumbnail() {
        return mHasThumbnail;
    }

    public int getThumbnailWidth() {
        return mThumbnailWidth;
    }

    public int getThumbnailHeight() {
        return mThumbnailHeight;
    }

    public boolean isIsThumbnailCompressed() {
        return mIsThumbnailCompressed;
    }

    public long getThumbnailOffset() {
        return mThumbnailOffset;
    }

    public long getThumbnailLength() {
        return mThumbnailLength;
    }

    public boolean hasLatLong() {
        return mHasLatLong;
    }

    public double getComputedLatitude() {
        return mComputedLatitude;
    }

    public double getComputedLongitude() {
        return mComputedLongitude;
    }

    public double getComputedAltitude() {
        return mComputedAltitude;
    }

    public long getGpsLatitudeOffset() {
        return mGpsLatitudeOffset;
    }

    public long getGpsLatitudeLength() {
        return mGpsLatitudeLength;
    }

    public long getMakeOffset() {
        return mMakeOffset;
    }

    public long getMakeLength() {
        return mMakeLength;
    }

    public String getMake() {
        return mMake;
    }

    public String getModel() {
        return mModel;
    }

    public double getAperture() {
        return mAperture;
    }

    public String getDateTimeOriginal() {
        return mDateTimeOriginal;
    }

    public double getExposureTime() {
        return mExposureTime;
    }

    public String getFocalLength() {
        return mFocalLength;
    }

    public String getGpsAltitude() {
        return mGpsAltitude;
    }

    public String getGpsAltitudeRef() {
        return mGpsAltitudeRef;
    }

    public String getGpsDatestamp() {
        return mGpsDatestamp;
    }

    public String getGpsLatitude() {
        return mGpsLatitude;
    }

    public String getGpsLatitudeRef() {
        return mGpsLatitudeRef;
    }

    public String getGpsLongitude() {
        return mGpsLongitude;
    }

    public String getGpsLongitudeRef() {
        return mGpsLongitudeRef;
    }

    public String getGpsProcessingMethod() {
        return mGpsProcessingMethod;
    }

    public String getGpsTimestamp() {
        return mGpsTimestamp;
    }

    public int getImageLength() {
        return mImageLength;
    }

    public int getImageWidth() {
        return mImageWidth;
    }

    public String getIso() {
        return mIso;
    }

    public int getOrientation() {
        return mOrientation;
    }

    public boolean hasXmp() {
        return mHasXmp;
    }

    @Nullable
    public String getXmp() {
        return mXmp;
    }

    /**
     * Returns the expected XMP data read from {@code resources} using {@link
     * Builder#setXmpResourceId}.
     * Returns the expected XMP data set directly with {@link Builder#setXmp} or read from {@code
     * resources} using {@link Builder#setXmpResourceId}.
     *
     * <p>Returns null if no expected XMP data was set.
     */
    @Nullable
    public String getXmp(Resources resources) throws IOException {
        if (mMemoizedXmp == null && mXmpResourceId != null) {
            try (InputStreamReader inputStreamReader = new InputStreamReader(
                    resources.openRawResource(mXmpResourceId), Charsets.UTF_8)
            ) {
                mMemoizedXmp = CharStreams.toString(inputStreamReader);
            }
        }
        return mMemoizedXmp;
    }

    @Nullable
    public Integer getXmpResourceId() {
        return mXmpResourceId;
    }

    public long getXmpOffset() {
        return mXmpOffset;
    }

    public long getXmpLength() {
        return mXmpLength;
    }

    public boolean hasExtendedXmp() {
        return mHasExtendedXmp;
    }

    public boolean hasIccProfile() {
        return mHasIccProfile;
    }

    public boolean hasPhotoshopImageResources() {
        return mHasPhotoshopImageResources;
    }
}

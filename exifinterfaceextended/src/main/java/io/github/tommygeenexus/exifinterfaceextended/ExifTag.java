/*
 * Copyright 2018 The Android Open Source Project
 * Copyright 2020 Tom Geiselmann <tomgapplicationsdevelopment@gmail.com>
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

/**
 * A class for indicating EXIF tag.
 */
class ExifTag {

    private final int mNumber;
    private final String mName;
    private final int mPrimaryFormat;
    private final int mSecondaryFormat;

    ExifTag(String name, int number, int format) {
        mName = name;
        mNumber = number;
        mPrimaryFormat = format;
        mSecondaryFormat = -1;
    }

    ExifTag(String name, int number, int primaryFormat, int secondaryFormat) {
        mName = name;
        mNumber = number;
        mPrimaryFormat = primaryFormat;
        mSecondaryFormat = secondaryFormat;
    }

    public boolean isFormatCompatible(int format) {
        if (mPrimaryFormat == ExifInterfaceExtended.IFD_FORMAT_UNDEFINED || format == ExifInterfaceExtended.IFD_FORMAT_UNDEFINED) {
            return true;
        } else if (mPrimaryFormat == format || mSecondaryFormat == format) {
            return true;
        } else if ((mPrimaryFormat == ExifInterfaceExtended.IFD_FORMAT_ULONG || mSecondaryFormat == ExifInterfaceExtended.IFD_FORMAT_ULONG)
                && format == ExifInterfaceExtended.IFD_FORMAT_USHORT) {
            return true;
        } else if ((mPrimaryFormat == ExifInterfaceExtended.IFD_FORMAT_SLONG || mSecondaryFormat == ExifInterfaceExtended.IFD_FORMAT_SLONG)
                && format == ExifInterfaceExtended.IFD_FORMAT_SSHORT) {
            return true;
        } else {
            return (mPrimaryFormat == ExifInterfaceExtended.IFD_FORMAT_DOUBLE ||
                    mSecondaryFormat == ExifInterfaceExtended.IFD_FORMAT_DOUBLE) && format == ExifInterfaceExtended.IFD_FORMAT_SINGLE;
        }
    }

    public int getNumber() {
        return mNumber;
    }

    public String getName() {
        return mName;
    }

    public int getPrimaryFormat() {
        return mPrimaryFormat;
    }

    public int getSecondaryFormat() {
        return mSecondaryFormat;
    }
}

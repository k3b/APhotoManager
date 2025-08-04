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

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A class for indicating EXIF attribute.
 */
class ExifAttribute {

    private static final String TAG = "ExifAttribute";

    public static final long BYTES_OFFSET_UNKNOWN = -1;

    private final int mFormat;
    private final int mNumberOfComponents;
    private final long mBytesOffset;
    private final byte[] mBytes;

    ExifAttribute(int format, int numberOfComponents, byte[] bytes) {
        this(format, numberOfComponents, BYTES_OFFSET_UNKNOWN, bytes);
    }

    ExifAttribute(int format, int numberOfComponents, long bytesOffset, byte[] bytes) {
        this.mFormat = format;
        this.mNumberOfComponents = numberOfComponents;
        this.mBytesOffset = bytesOffset;
        this.mBytes = bytes;
    }

    public static ExifAttribute createUShort(int[] values, ByteOrder byteOrder) {
        final ByteBuffer buffer = ByteBuffer.wrap(
                new byte[ExifInterfaceExtended.IFD_FORMAT_BYTES_PER_FORMAT[ExifInterfaceExtended.IFD_FORMAT_USHORT] * values.length]);
        buffer.order(byteOrder);
        for (int value : values) {
            buffer.putShort((short) value);
        }
        return new ExifAttribute(ExifInterfaceExtended.IFD_FORMAT_USHORT, values.length, buffer.array());
    }

    public static ExifAttribute createUShort(int value, ByteOrder byteOrder) {
        return createUShort(new int[]{value}, byteOrder);
    }

    public static ExifAttribute createULong(long[] values, ByteOrder byteOrder) {
        final ByteBuffer buffer = ByteBuffer.wrap(
                new byte[ExifInterfaceExtended.IFD_FORMAT_BYTES_PER_FORMAT[ExifInterfaceExtended.IFD_FORMAT_ULONG] * values.length]);
        buffer.order(byteOrder);
        for (long value : values) {
            buffer.putInt((int) value);
        }
        return new ExifAttribute(ExifInterfaceExtended.IFD_FORMAT_ULONG, values.length, buffer.array());
    }

    public static ExifAttribute createULong(long value, ByteOrder byteOrder) {
        return createULong(new long[]{value}, byteOrder);
    }

    public static ExifAttribute createSLong(int[] values, ByteOrder byteOrder) {
        final ByteBuffer buffer = ByteBuffer.wrap(
                new byte[ExifInterfaceExtended.IFD_FORMAT_BYTES_PER_FORMAT[ExifInterfaceExtended.IFD_FORMAT_SLONG] * values.length]);
        buffer.order(byteOrder);
        for (int value : values) {
            buffer.putInt(value);
        }
        return new ExifAttribute(ExifInterfaceExtended.IFD_FORMAT_SLONG, values.length, buffer.array());
    }

    public static ExifAttribute createByte(String value) {
        // Exception for GPSAltitudeRef tag
        if (value.length() == 1 && value.charAt(0) >= '0' && value.charAt(0) <= '1') {
            final byte[] bytes = new byte[]{(byte) (value.charAt(0) - '0')};
            return new ExifAttribute(ExifInterfaceExtended.IFD_FORMAT_BYTE, bytes.length, bytes);
        }
        final byte[] ascii = value.getBytes(ExifInterfaceExtended.ASCII);
        return new ExifAttribute(ExifInterfaceExtended.IFD_FORMAT_BYTE, ascii.length, ascii);
    }

    public static ExifAttribute createString(String value) {
        final byte[] ascii = (value + '\0').getBytes(ExifInterfaceExtended.ASCII);
        return new ExifAttribute(ExifInterfaceExtended.IFD_FORMAT_STRING, ascii.length, ascii);
    }

    public static ExifAttribute createUcs2String(String value) {
        final byte[] bytes = (value + "\0").getBytes(ExifInterfaceExtended.UCS2);
        return new ExifAttribute(ExifInterfaceExtended.IFD_FORMAT_UCS2LE_STRING, bytes.length, bytes);
    }

    public static ExifAttribute createURational(Rational[] values, ByteOrder byteOrder) {
        final ByteBuffer buffer = ByteBuffer.wrap(
                new byte[ExifInterfaceExtended.IFD_FORMAT_BYTES_PER_FORMAT[ExifInterfaceExtended.IFD_FORMAT_URATIONAL] * values.length]);
        buffer.order(byteOrder);
        for (Rational value : values) {
            buffer.putInt((int) value.getNumerator());
            buffer.putInt((int) value.getDenominator());
        }
        return new ExifAttribute(ExifInterfaceExtended.IFD_FORMAT_URATIONAL, values.length, buffer.array());
    }

    public static ExifAttribute createURational(Rational value, ByteOrder byteOrder) {
        return createURational(new Rational[]{value}, byteOrder);
    }

    public static ExifAttribute createSRational(Rational[] values, ByteOrder byteOrder) {
        final ByteBuffer buffer = ByteBuffer.wrap(
                new byte[ExifInterfaceExtended.IFD_FORMAT_BYTES_PER_FORMAT[ExifInterfaceExtended.IFD_FORMAT_SRATIONAL] * values.length]);
        buffer.order(byteOrder);
        for (Rational value : values) {
            buffer.putInt((int) value.getNumerator());
            buffer.putInt((int) value.getDenominator());
        }
        return new ExifAttribute(ExifInterfaceExtended.IFD_FORMAT_SRATIONAL, values.length, buffer.array());
    }

    public static ExifAttribute createDouble(double[] values, ByteOrder byteOrder) {
        final ByteBuffer buffer = ByteBuffer.wrap(
                new byte[ExifInterfaceExtended.IFD_FORMAT_BYTES_PER_FORMAT[ExifInterfaceExtended.IFD_FORMAT_DOUBLE] * values.length]);
        buffer.order(byteOrder);
        for (double value : values) {
            buffer.putDouble(value);
        }
        return new ExifAttribute(ExifInterfaceExtended.IFD_FORMAT_DOUBLE, values.length, buffer.array());
    }

    @Override
    @NonNull
    public String toString() {
        return "(" + ExifInterfaceExtended.IFD_FORMAT_NAMES[mFormat] + ", data length:" + mBytes.length + ")";
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    Object getValue(ByteOrder byteOrder) {
        ByteOrderedDataInputStream inputStream = null;
        try {
            inputStream = new ByteOrderedDataInputStream(mBytes);
            inputStream.setByteOrder(byteOrder);
            switch (mFormat) {
                case ExifInterfaceExtended.IFD_FORMAT_BYTE:
                case ExifInterfaceExtended.IFD_FORMAT_SBYTE: {
                    // Exception for GPSAltitudeRef tag
                    if (mBytes.length == 1 && mBytes[0] >= 0 && mBytes[0] <= 1) {
                        return String.valueOf((char) (mBytes[0] + '0'));
                    }
                    return new String(mBytes, ExifInterfaceExtended.ASCII);
                }
                case ExifInterfaceExtended.IFD_FORMAT_UNDEFINED:
                case ExifInterfaceExtended.IFD_FORMAT_STRING: {
                    int index = 0;
                    if (mNumberOfComponents >= ExifInterfaceExtended.EXIF_ASCII_PREFIX.length) {
                        boolean same = true;
                        for (int i = 0; i < ExifInterfaceExtended.EXIF_ASCII_PREFIX.length; ++i) {
                            if (mBytes[i] != ExifInterfaceExtended.EXIF_ASCII_PREFIX[i]) {
                                same = false;
                                break;
                            }
                        }
                        if (same) {
                            index = ExifInterfaceExtended.EXIF_ASCII_PREFIX.length;
                        }
                    }

                    StringBuilder stringBuilder = new StringBuilder();
                    while (index < mNumberOfComponents) {
                        int ch = mBytes[index];
                        if (ch == 0) {
                            break;
                        }
                        if (ch >= 32) {
                            stringBuilder.append((char) ch);
                        } else {
                            stringBuilder.append('?');
                        }
                        ++index;
                    }
                    return stringBuilder.toString();
                }
                case ExifInterfaceExtended.IFD_FORMAT_USHORT: {
                    final int[] values = new int[mNumberOfComponents];
                    for (int i = 0; i < mNumberOfComponents; ++i) {
                        values[i] = inputStream.readUnsignedShort();
                    }
                    return values;
                }
                case ExifInterfaceExtended.IFD_FORMAT_ULONG: {
                    final long[] values = new long[mNumberOfComponents];
                    for (int i = 0; i < mNumberOfComponents; ++i) {
                        values[i] = inputStream.readUnsignedInt();
                    }
                    return values;
                }
                case ExifInterfaceExtended.IFD_FORMAT_URATIONAL: {
                    final Rational[] values = new Rational[mNumberOfComponents];
                    for (int i = 0; i < mNumberOfComponents; ++i) {
                        final long numerator = inputStream.readUnsignedInt();
                        final long denominator = inputStream.readUnsignedInt();
                        values[i] = new Rational(numerator, denominator);
                    }
                    return values;
                }
                case ExifInterfaceExtended.IFD_FORMAT_SSHORT: {
                    final int[] values = new int[mNumberOfComponents];
                    for (int i = 0; i < mNumberOfComponents; ++i) {
                        values[i] = inputStream.readShort();
                    }
                    return values;
                }
                case ExifInterfaceExtended.IFD_FORMAT_SLONG: {
                    final int[] values = new int[mNumberOfComponents];
                    for (int i = 0; i < mNumberOfComponents; ++i) {
                        values[i] = inputStream.readInt();
                    }
                    return values;
                }
                case ExifInterfaceExtended.IFD_FORMAT_SRATIONAL: {
                    final Rational[] values = new Rational[mNumberOfComponents];
                    for (int i = 0; i < mNumberOfComponents; ++i) {
                        final long numerator = inputStream.readInt();
                        final long denominator = inputStream.readInt();
                        values[i] = new Rational(numerator, denominator);
                    }
                    return values;
                }
                case ExifInterfaceExtended.IFD_FORMAT_SINGLE: {
                    final double[] values = new double[mNumberOfComponents];
                    for (int i = 0; i < mNumberOfComponents; ++i) {
                        values[i] = inputStream.readFloat();
                    }
                    return values;
                }
                case ExifInterfaceExtended.IFD_FORMAT_DOUBLE: {
                    final double[] values = new double[mNumberOfComponents];
                    for (int i = 0; i < mNumberOfComponents; ++i) {
                        values[i] = inputStream.readDouble();
                    }
                    return values;
                }
                default:
                    return null;
            }
        } catch (IOException e) {
            Log.w(TAG, "IOException occurred during reading a value", e);
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "IOException occurred while closing InputStream", e);
                }
            }
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
                stringBuilder.append(array[i].getNumerator());
                stringBuilder.append('/');
                stringBuilder.append(array[i].getDenominator());
                if (i + 1 != array.length) {
                    stringBuilder.append(",");
                }
            }
            return stringBuilder.toString();
        }
        return null;
    }

    public int size() {
        return ExifInterfaceExtended.IFD_FORMAT_BYTES_PER_FORMAT[mFormat] * mNumberOfComponents;
    }

    public byte[] getBytes() {
        return mBytes;
    }

    public int getFormat() {
        return mFormat;
    }

    public int getNumberOfComponents() {
        return mNumberOfComponents;
    }

    public long getBytesOffset() {
        return mBytesOffset;
    }
}

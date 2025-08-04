/*
 * Copyright 2018 The Android Open Source Project
 * Copyright 2020-2021 Tom Geiselmann <tomgapplicationsdevelopment@gmail.com>
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

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * An input stream to parse EXIF data area, which can be written in either little or big endian
 * order.
 */
class ByteOrderedDataInputStream extends InputStream implements DataInput {

    private static final ByteOrder LITTLE_ENDIAN = ByteOrder.LITTLE_ENDIAN;
    private static final ByteOrder BIG_ENDIAN = ByteOrder.BIG_ENDIAN;

    public static final int LENGTH_UNSET = -1;

    private final DataInputStream mDataInputStream;
    private ByteOrder mByteOrder;

    private int mPosition;
    private byte[] mSkipBuffer;
    private int mLength;

    ByteOrderedDataInputStream(byte[] bytes) {
        this(new ByteArrayInputStream(bytes), ByteOrder.BIG_ENDIAN);
        this.mLength = bytes.length;
    }

    ByteOrderedDataInputStream(InputStream in) {
        this(in, ByteOrder.BIG_ENDIAN);
    }

    ByteOrderedDataInputStream(InputStream in, ByteOrder byteOrder) {
        mDataInputStream = new DataInputStream(in);
        mDataInputStream.mark(0);
        mPosition = 0;
        mByteOrder = byteOrder;
        this.mLength = in instanceof ByteOrderedDataInputStream
                ? ((ByteOrderedDataInputStream) in).length()
                : LENGTH_UNSET;
    }

    @Override
    public int available() throws IOException {
        return mDataInputStream.available();
    }

    @Override
    public int read() throws IOException {
        ++mPosition;
        return mDataInputStream.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int bytesRead = mDataInputStream.read(b, off, len);
        mPosition += bytesRead;
        return bytesRead;
    }

    @Override
    public int readUnsignedByte() throws IOException {
        ++mPosition;
        return mDataInputStream.readUnsignedByte();
    }

    @Override
    public String readLine() {
        return null;
    }

    @Override
    public boolean readBoolean() throws IOException {
        ++mPosition;
        return mDataInputStream.readBoolean();
    }

    @Override
    public char readChar() throws IOException {
        mPosition += 2;
        return mDataInputStream.readChar();
    }

    @Override
    public String readUTF() throws IOException {
        mPosition += 2;
        return mDataInputStream.readUTF();
    }

    @Override
    public void readFully(byte[] buffer, int offset, int length) throws IOException {
        mPosition += length;
        mDataInputStream.readFully(buffer, offset, length);
    }

    @Override
    public void readFully(byte[] buffer) throws IOException {
        mPosition += buffer.length;
        mDataInputStream.readFully(buffer);
    }

    @Override
    public byte readByte() throws IOException {
        ++mPosition;
        int ch = mDataInputStream.read();
        if (ch < 0) {
            throw new EOFException();
        }
        return (byte) ch;
    }

    @Override
    public short readShort() throws IOException {
        mPosition += 2;
        int ch1 = mDataInputStream.read();
        int ch2 = mDataInputStream.read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        if (mByteOrder == LITTLE_ENDIAN) {
            return (short) ((ch2 << 8) + ch1);
        } else if (mByteOrder == BIG_ENDIAN) {
            return (short) ((ch1 << 8) + ch2);
        }
        throw new IOException("Invalid byte order: " + mByteOrder);
    }

    @Override
    public int readInt() throws IOException {
        mPosition += 4;
        int ch1 = mDataInputStream.read();
        int ch2 = mDataInputStream.read();
        int ch3 = mDataInputStream.read();
        int ch4 = mDataInputStream.read();
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

    @SuppressWarnings("RedundantThrows")
    @Override
    public int skipBytes(int n) throws IOException {
        throw new UnsupportedOperationException("skipBytes is currently unsupported");
    }

    /**
     * Discards n bytes of data from the input stream. This method will block until either
     * the full amount has been skipped or the end of the stream is reached, whichever happens
     * first.
     */
    public void skipFully(int n) throws IOException {
        int totalSkipped = 0;
        while (totalSkipped < n) {
            int skipped = (int) mDataInputStream.skip(n - totalSkipped);
            if (skipped == 0) {
                if (mSkipBuffer == null) {
                    mSkipBuffer = new byte[ExifInterfaceExtendedUtils.BUF_SIZE];
                }
                int bytesToSkip = Math.min(ExifInterfaceExtendedUtils.BUF_SIZE, n - totalSkipped);
                if ((skipped = mDataInputStream.read(mSkipBuffer, 0, bytesToSkip)) == -1) {
                    throw new EOFException("Reached EOF while skipping " + n + " bytes.");
                }
            }
            totalSkipped += skipped;
        }
        mPosition += totalSkipped;
    }

    @Override
    public int readUnsignedShort() throws IOException {
        mPosition += 2;
        int ch1 = mDataInputStream.read();
        int ch2 = mDataInputStream.read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        if (mByteOrder == LITTLE_ENDIAN) {
            return ((ch2 << 8) + ch1);
        } else if (mByteOrder == BIG_ENDIAN) {
            return ((ch1 << 8) + ch2);
        }
        throw new IOException("Invalid byte order: " + mByteOrder);
    }

    public long readUnsignedInt() throws IOException {
        return readInt() & 0xffffffffL;
    }

    @Override
    public long readLong() throws IOException {
        mPosition += 8;
        int ch1 = mDataInputStream.read();
        int ch2 = mDataInputStream.read();
        int ch3 = mDataInputStream.read();
        int ch4 = mDataInputStream.read();
        int ch5 = mDataInputStream.read();
        int ch6 = mDataInputStream.read();
        int ch7 = mDataInputStream.read();
        int ch8 = mDataInputStream.read();
        if ((ch1 | ch2 | ch3 | ch4 | ch5 | ch6 | ch7 | ch8) < 0) {
            throw new EOFException();
        }
        if (mByteOrder == LITTLE_ENDIAN) {
            return (((long) ch8 << 56) + ((long) ch7 << 48) + ((long) ch6 << 40)
                    + ((long) ch5 << 32) + ((long) ch4 << 24) + ((long) ch3 << 16)
                    + ((long) ch2 << 8) + (long) ch1);
        } else if (mByteOrder == BIG_ENDIAN) {
            return (((long) ch1 << 56) + ((long) ch2 << 48) + ((long) ch3 << 40)
                    + ((long) ch4 << 32) + ((long) ch5 << 24) + ((long) ch6 << 16)
                    + ((long) ch7 << 8) + (long) ch8);
        }
        throw new IOException("Invalid byte order: " + mByteOrder);
    }

    @Override
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    public void mark(int readlimit) {
        throw new UnsupportedOperationException("Mark is currently unsupported");
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException("Reset is currently unsupported");
    }

    /** Return the total length (in bytes) of the underlying stream if known, otherwise
     *  {@link #LENGTH_UNSET}. */
    public int length() {
        return mLength;
    }

    public DataInputStream getDataInputStream() {
        return mDataInputStream;
    }

    public void setByteOrder(ByteOrder byteOrder) {
        mByteOrder = byteOrder;
    }

    public int getPosition() {
        return mPosition;
    }

    public void setPosition(int position) {
        mPosition = position;
    }

    /** Reads all remaining data. */
    public byte[] readToEnd() throws IOException {
        byte[] data = new byte[1024];
        int bytesRead = 0;
        while (true) {
            if (bytesRead == data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
            int readResult = mDataInputStream.read(data, bytesRead, data.length - bytesRead);
            if (readResult != -1) {
                bytesRead += readResult;
                mPosition += readResult;
            } else {
                break;
            }
        }
        return Arrays.copyOf(data, bytesRead);
    }
}

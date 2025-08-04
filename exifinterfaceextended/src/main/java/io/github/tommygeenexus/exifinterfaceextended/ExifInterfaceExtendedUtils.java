/*
 * Copyright 2020 The Android Open Source Project
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

import android.media.MediaDataSource;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import androidx.annotation.DoNotInline;
import androidx.annotation.RequiresApi;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.zip.CRC32;

class ExifInterfaceExtendedUtils {

    private static final String TAG = "ExifInterfaceUtils";

    static final int BUF_SIZE = 8192;

    private ExifInterfaceExtendedUtils() {
        // Prevent instantiation
    }

    /**
     * Copies all of the bytes from {@code in} to {@code out}. Neither stream is closed.
     */
    static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[BUF_SIZE];
        int c;
        while ((c = in.read(buffer)) != -1) {
            out.write(buffer, 0, c);
        }
    }

    /**
     * Copies the given number of the bytes from {@code in} to {@code out}. Neither stream is
     * closed.
     */
    static void copy(InputStream in, OutputStream out, int numBytes) throws IOException {
        int remainder = numBytes;
        byte[] buffer = new byte[BUF_SIZE];
        while (remainder > 0) {
            int bytesToRead = Math.min(remainder, BUF_SIZE);
            int bytesRead = in.read(buffer, 0, bytesToRead);
            if (bytesRead != bytesToRead) {
                throw new IOException("Failed to copy the given amount of bytes from the input"
                        + "stream to the output stream.");
            }
            remainder -= bytesRead;
            out.write(buffer, 0, bytesRead);
        }
    }

    /**
     * Convert given int[] to long[]. If long[] is given, just return it.
     * Return null for other types of input.
     */
    static long[] convertToLongArray(Object inputObj) {
        if (inputObj instanceof int[]) {
            int[] input = (int[]) inputObj;
            long[] result = new long[input.length];
            for (int i = 0; i < input.length; i++) {
                result[i] = input[i];
            }
            return result;
        } else if (inputObj instanceof long[]) {
            return (long[]) inputObj;
        }
        return null;
    }

    static boolean startsWith(byte[] cur, byte[] val) {
        if (cur == null || val == null) {
            return false;
        }
        if (cur.length < val.length) {
            return false;
        }
        for (int i = 0; i < val.length; i++) {
            if (cur[i] != val[i]) {
                return false;
            }
        }
        return true;
    }

    static int calculateCrc32IntValue(byte[] type, byte[] data) {
        final CRC32 crc = new CRC32();
        crc.update(type);
        crc.update(data);
        return (int) crc.getValue();
    }

    static long parseSubSeconds(String subSec) {
        try {
            final int len = Math.min(subSec.length(), 3);
            long sub = Long.parseLong(subSec.substring(0, len));
            for (int i = len; i < 3; i++) {
                sub *= 10;
            }
            return sub;
        } catch (NumberFormatException e) {
            // Ignored
        }
        return 0L;
    }

    /**
     * Closes 'closeable', ignoring any checked exceptions. Does nothing if 'closeable' is null.
     */
    static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Closes a file descriptor that has been duplicated.
     */
    static void closeFileDescriptor(FileDescriptor fd) {
        // Os.dup and Os.close was introduced in API 21 so this method shouldn't be called
        // in API < 21.
        if (Build.VERSION.SDK_INT >= 21) {
            try {
                Api21Impl.close(fd);
                // Catching ErrnoException will raise error in API < 21
            } catch (Exception ex) {
                Log.e(TAG, "Error closing fd.");
            }
        } else {
            Log.e(TAG, "closeFileDescriptor is called in API < 21, which must be wrong.");
        }
    }

    /**
     * {@link Objects#requireNonNull(Object, String)} requires minSdk
     * {@link android.os.Build.VERSION_CODES#KITKAT}.
     */
    static <T> void requireNonNull(T obj, String message) {
        if (obj == null) {
            throw new NullPointerException(message);
        }
    }

    @RequiresApi(21)
    static class Api21Impl {
        private Api21Impl() {}

        @DoNotInline
        static FileDescriptor dup(FileDescriptor fileDescriptor) throws ErrnoException {
            return Os.dup(fileDescriptor);
        }

        @SuppressWarnings({"UnusedReturnValue", "SameParameterValue"})
        @DoNotInline
        static long lseek(FileDescriptor fd, long offset, int whence) throws ErrnoException {
            return Os.lseek(fd, offset, whence);
        }

        @DoNotInline
        static void close(FileDescriptor fd) throws ErrnoException {
            Os.close(fd);
        }
    }

    @RequiresApi(23)
    static class Api23Impl {
        private Api23Impl() {}

        @DoNotInline
        static void setDataSource(MediaMetadataRetriever retriever, MediaDataSource dataSource) {
            retriever.setDataSource(dataSource);
        }
    }
}

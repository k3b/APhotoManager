/*
 * Copyright 2018 The Android Open Source Project
 * Copyright 2021 Tom Geiselmann <tomgapplicationsdevelopment@gmail.com>
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

import java.io.IOException;
import java.io.InputStream;

/**
 * An input stream class that can parse both little and big endian order data and also
 * supports seeking to any position in the stream via mark/reset.
 */
class SeekableByteOrderedDataInputStream extends ByteOrderedDataInputStream {

    SeekableByteOrderedDataInputStream(byte[] bytes) {
        super(bytes);
        // No need to check if mark is supported here since ByteOrderedDataInputStream will
        // create a ByteArrayInputStream, which supports mark by default.
        getDataInputStream().mark(Integer.MAX_VALUE);
    }

    /**
     * Given input stream should support mark/reset, and should be set to the beginning of
     * the stream.
     */
    SeekableByteOrderedDataInputStream(InputStream in) {
        super(in);
        if (!in.markSupported()) {
            throw new IllegalArgumentException("Cannot create "
                    + "SeekableByteOrderedDataInputStream with stream that does not support "
                    + "mark/reset");
        }
        // Mark given InputStream to the maximum value (we can't know the length of the
        // stream for certain) so that InputStream.reset() may be called at any point in the
        // stream to reset the stream to an earlier position.
        getDataInputStream().mark(Integer.MAX_VALUE);
    }

    /**
     * Seek to the given absolute position in the stream (i.e. the number of bytes from the
     * beginning of the stream).
     */
    public void seek(long position) throws IOException {
        if (getPosition() > position) {
            setPosition(0);
            getDataInputStream().reset();
        } else {
            position -= getPosition();
        }
        skipFully((int) position);
    }
}

package com.google.common.io;

import java.io.ByteArrayOutputStream;

/**
 * A {@see ByteArrayOutputStream} with an additional method {@link FastByteArrayOutputStream#writeTo(byte[],
 * int)}
 *
 * @author Chris Povirk
 * @author Bernd Hopp
 * @since 20.0
 */
class FastByteArrayOutputStream
        extends ByteArrayOutputStream {
    public FastByteArrayOutputStream(int initByteSize) {
        super(initByteSize);
    }

    /**
     * Writes the contents of the internal buffer to the given array starting at the given offset.
     * Assumes the array has space to hold count bytes.
     */
    void writeTo(byte[] b, int off) {
        System.arraycopy(buf, 0, b, off, count);
    }
}


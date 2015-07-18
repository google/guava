package com.google.common.io;

import com.google.common.annotations.Beta;

import java.io.ByteArrayOutputStream;
import java.lang.ref.SoftReference;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Provides thread-local buffers in the form of either a byte-array of a {@link
 * FastByteArrayOutputStream}. To circumvent any memory-leak issues, the implementation uses {@link
 * SoftReference} internally, so the Garbage Collection may collect any memory allocated by this
 * class that is not referenced outside the class itself at the time. This class is package-local
 * for it is intended to be used by guava's io-classes, but can maybe eventually be made public.
 *
 * @author Bernd Hopp
 * @since 19.0
 */
@Beta
final class ThreadLocalBuffers {

    private static final int INIT_BYTE_SIZE = 2048;

    private static final ThreadLocal<SoftReference<FastByteArrayOutputStream>> BYTE_ARRAY_OUTPUTSTREAM_SOFTREFERENCE_THREADLOCAL =
            new ThreadLocal<SoftReference<FastByteArrayOutputStream>>() {
                @Override
                protected SoftReference<FastByteArrayOutputStream> initialValue() {
                    return new SoftReference<FastByteArrayOutputStream>(new FastByteArrayOutputStream(INIT_BYTE_SIZE));
                }
            };

    private static final ThreadLocal<SoftReference<byte[]>> BYTE_ARRAY_SOFTREFERENCE_THREADLOCAL =
            new ThreadLocal<SoftReference<byte[]>>() {
                @Override
                protected SoftReference<byte[]> initialValue() {
                    return new SoftReference<byte[]>(new byte[INIT_BYTE_SIZE]);

                }
            };

    private ThreadLocalBuffers() {
    }

    private static <T> T getOrCreateIfObjectWasCollected(ThreadLocal<SoftReference<T>> softReferenceThreadLocal) {
        /**since it cannot be predicted when GC will collect the T value from the SoftReference,
         * this loop will try as long as it takes to get a hard reference. If the softReference points to an already
         * gc'ed T, remove() is called on the threadlocal to force it to create a new soft-referenced T in the next iteration.
         */
        while (true) {
            SoftReference<T> softReference = softReferenceThreadLocal.get();

            T value = softReference.get();

            if (value != null) {
                return value;
            }

            softReferenceThreadLocal.remove();
        }
    }

    /**
     * Returns a thread-local byte-array of arbitrary data, that can be used for buffering. It is
     * guaranteed that the byte-array's length is a power of 2 greater than or equal 2048. If the
     * array must have a minimum size larger than 2048 or should not contain arbitrary data but just
     * zeros, see {@link ThreadLocalBuffers#getByteArray(int, boolean)}. Note that calls to this
     * method from the same thread will return the same byte-array.
     *
     * @return a byte-array for buffering.
     * @since 19.0
     */
    public static byte[] getByteArray() {
        return getByteArray(0, false);
    }

    /**
     * Returns a thread-local byte-array that can be used for buffering. It is guaranteed that the
     * byte-array's length is a power of 2 greater than or equal minSize.
     *
     * @param minSize the minimum size ot the returned byte-array, must not be negative.
     * @param zeroed  set to true if the byte-array should contain just zeros
     * @return a byte-array for buffering
     * @since 19.0
     */
    public static byte[] getByteArray(int minSize, boolean zeroed) {
        checkArgument(minSize >= 0, "minSize must not be negative");

        byte[] byteArray = getOrCreateIfObjectWasCollected(BYTE_ARRAY_SOFTREFERENCE_THREADLOCAL);

        if ( byteArray.length < minSize ) {

            int newLenght = byteArray.length;

            do {
                newLenght *= 2;
            } while ( newLenght < minSize );

            byteArray = new byte[newLenght];
            BYTE_ARRAY_SOFTREFERENCE_THREADLOCAL.set(new SoftReference<byte[]>(byteArray));
        } else if ( zeroed ) {
            Arrays.fill(byteArray, (byte) 0);
        }

        return byteArray;
    }

    /**
     * Returns a {@link FastByteArrayOutputStream}. It is guaranteed that the ByteArrayOutputStream will
     * not be accessed outside the current thread. Note that calls to this method from the same
     * thread will return the same ByteArrayOutputStream.
     *
     * @return a {@link FastByteArrayOutputStream} for buffering and stream-capturing.
     * @since 19.0
     */
    public static FastByteArrayOutputStream getByteArrayOutputStream() {
        FastByteArrayOutputStream byteArrayOutputStream = getOrCreateIfObjectWasCollected(
            BYTE_ARRAY_OUTPUTSTREAM_SOFTREFERENCE_THREADLOCAL
        );

        byteArrayOutputStream.reset();

        return byteArrayOutputStream;
    }
}

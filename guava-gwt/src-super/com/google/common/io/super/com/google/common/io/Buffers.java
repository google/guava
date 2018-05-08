package com.google.common.io;

import com.google.common.annotations.Beta;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author Bernd Hopp
 * @since 20.0
 */
@Beta
final class Buffers {

    /**
     * A Lease is a wrapper for some sort of buffer to guarantee that the buffer is not accessed
     * from outside the current thread and currently executed method. The buffer is therefore not
     * only thread-local but also it is guaranteed to not be used by the same thread on different
     * levels of the call-stack.
     */
    public interface Lease<T> extends Closeable {

        /**
         * @return a buffer that can be used for buffering. Never cache or reference
         * this buffer outside the scope of the current method.
         */
        T getBuffer();

        /**
         * returns the lease, call this method in a finally-block of the method where you use the buffer
         */
        void returnLease();
    }

    private static final Lease<byte[]> LEASE = new Lease<byte[]>() {
        @Override
        public byte[] getBuffer() {
            return buffer;
        }

        @Override
        public void returnLease() {

        }

        @Override
        public void close() throws IOException {

        }
    };

    //since javascript is effectively single threaded, one buffer is sufficient
    private static byte[] buffer = new byte[2048];

    /**
     * Returns a {@link Lease<byte[]>}, that can be used for buffering. It is
     * guaranteed that the byte-array's length is even
     *
     * @return a {@link Lease<byte[]>} for buffering.
     * @since 20.0
     */
    public static Lease<byte[]> leaseByteArray() {
        return leaseByteArray(0);
    }


    /**
     * Returns a {@link Lease<byte[]>}, that can be used for buffering. It is
     * guaranteed that the byte-array's length is even
     *
     * @param minSize the minimum size ot the returned byte-array, must not be negative.
     * @return a {@link Lease<byte[]>} for buffering.
     * @since 20.0
     */
     public static Lease<byte[]> leaseByteArray(int minSize) {
        checkArgument(minSize >= 0, "minSize must not be negative");

        if(buffer.length < minSize){
            if((minSize % 2) == 1){
                ++minSize;
            }

            buffer = new byte[minSize];
        } else {
            Arrays.fill(buffer, (byte)0);
        }

        return LEASE;
    }
}

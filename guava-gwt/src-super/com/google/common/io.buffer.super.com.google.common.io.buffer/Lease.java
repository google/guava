package com.google.common.io.buffer;

import com.google.common.annotations.GwtCompatible;

import java.io.Closeable;

/**
 * A Lease is a wrapper for some sort of buffer to guarantee that the buffer is not accessed
 * from outside the current thread and currently executed method. The buffer is therefore not
 * only thread-local but also it is guaranteed to not be used by the same thread on different
 * levels of the call-stack.
 */
@GwtCompatible
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

    /**
     * clears the buffer from previous data
     */
    void clearBuffer();

    /**
     * indicates whether the buffer is currently in use or not
     * @return true if the buffer is used, otherwise false
     */
    boolean isInUse();

    /**
     * the size of the underlying buffer in bytes.
     * @return the buffer's size or -1 if evaluation of the buffer's size is not possible
     */
    int bufferSize();
}

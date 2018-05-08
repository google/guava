package com.google.common.io.buffer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Math.min;

/**
 * A support class for {@link Buffers}, makes it easier to create re-usable
 * {@link BufferLeaseProvider}s. The implementation uses a {@link ThreadLocal}
 * and {@link Reference}s to reference the buffers. For the client-side GWT
 * implementation, no ThreadLocals of References are used because of the
 * single-thread model of javascript.
 *
 * Subclasses must implement {@link BufferLeaseProviderBase#createLease(int)}, for actually
 * creating the buffer. Subclasses can also overwrite {@link BufferLeaseProviderBase#reference(Lease)}
 * to use a custom strategy to create a {@link Reference} to wrap the Lease with, the default strategy
 * is to always use a SoftReference
 *
 * @param <T>
 */
public abstract class BufferLeaseProviderBase<T> implements BufferLeaseProvider<T> {

    private Lease<T> lease;

    private final int minBufferSize;

    protected BufferLeaseProviderBase(int minBufferSize) {
        this.minBufferSize = minBufferSize;
    }

    @Override
    public Lease<T> provide(int guaranteedSize) {
        checkArgument(guaranteedSize >= 0, "guaranteedSize must not be negative but was %s", guaranteedSize);

        if (lease == null) {
            return createAndCache(guaranteedSize);
        }

        if (lease.isInUse()) {
            //The lease is already in use, so we create a new one and cache that instance instead.
            //This can happen in two scenarios, either the lease is used in another method in the current
            //stacktrace, or the user forgot to close the lease at some point. Since the newly allocated buffer is
            // the cached instance after the following call, the old one can be gc'ed
            // and there will be no memory leak
            return createAndCache(guaranteedSize);
        }

        if (lease.bufferSize() < guaranteedSize) {
            //to small, allocate a bigger array and cache this instead
            return createAndCache(guaranteedSize);
        }

        //overwrite previous data
        lease.clearBuffer();

        return lease;
    }

    protected abstract Lease<T> createLease(int size);

    private Lease<T> createAndCache(int guaranteedSize) {

        final int size = min(minBufferSize, guaranteedSize);

        Lease<T> lease = createLease(size);

        return lease;
    }
}

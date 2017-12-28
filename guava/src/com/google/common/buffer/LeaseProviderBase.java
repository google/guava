package com.google.common.buffer;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Math.min;

/**
 * A support class for {@link Buffers}, makes it easier to create re-usable
 * {@link LeaseProvider}s. The implementation uses a {@link ThreadLocal}
 * and {@link Reference}s to reference the buffers. For the client-side GWT
 * implementation, no ThreadLocals of References are used because of the
 * single-thread model of javascript.
 *
 * Subclasses must implement {@link LeaseProviderBase#createLease(int)}, for actually
 * creating the buffer. Subclasses can also overwrite {@link LeaseProviderBase#reference(Lease)}
 * to use a custom strategy to create a {@link Reference} to wrap the Lease with, the default strategy
 * is to always use a SoftReference
 *
 * @param <T>
 */
@GwtCompatible(emulated = true)
public abstract class LeaseProviderBase<T> implements LeaseProvider<T> {

    @GwtIncompatible
    private final ThreadLocal<Reference<Lease<T>>> threadLocal = new ThreadLocal<>();

    private final int minBufferSize;

    protected LeaseProviderBase(int minBufferSize) {
        this.minBufferSize = minBufferSize;
    }

    @Override
    @GwtIncompatible
    public Lease<T> provide(int guaranteedSize) {
        checkArgument(guaranteedSize >= 0, "guaranteedSize must not be negative but was %s", guaranteedSize);

        Reference<Lease<T>> reference = threadLocal.get();

        if (reference == null) {
            //first call from this thread
            return createAndCache(guaranteedSize);
        }

        Lease<T> lease = reference.get();

        if (lease == null) {
            //buffer had been garbage collected
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
            //to small, allocate a bigger buffer and cache this instead
            return createAndCache(guaranteedSize);
        }

        //overwrite previous data
        lease.clearBuffer();

        return lease;
    }

    protected abstract Lease<T> createLease(int size);

    @GwtIncompatible
    /** create a {@link Reference} for the Lease. This method
     * can be overwritten to change the default behaviour of
     * creating a {@link WeakReference} for {@link Lease}'s with
     * {@link Lease#bufferSize()} larger than
     * {@link LeaseProviderBase#minBufferSize}
     */
    protected Reference<Lease<T>> reference(Lease<T> lease){
        return lease.bufferSize() > minBufferSize
                ? new WeakReference<>(lease)
                : new SoftReference<>(lease);
    }

    @GwtIncompatible
    private Lease<T> createAndCache(int guaranteedSize) {

        final int size = min(minBufferSize, guaranteedSize);

        Lease<T> lease = createLease(size);

        checkNotNull(lease, "method createLease() must not return null");

        Reference<Lease<T>> reference = reference(lease);

        threadLocal.set(reference);

        return lease;
    }
}

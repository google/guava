package com.google.common.buffer;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.min;

public abstract class ThreadLocalReferencedBufferLeaseProvider<T> implements BufferLeaseProvider<T> {

    private final ThreadLocal<Reference<Lease<T>>> threadLocal = new ThreadLocal<>();

    private final int initSize;

    public ThreadLocalReferencedBufferLeaseProvider(int initSize) {
        this.initSize = initSize;
    }

    @Override
    public Lease<T> provide(int minSize) {
        checkArgument(minSize >= 0, "minSize must not be negative but was %s", minSize);

        Reference<Lease<T>> reference = threadLocal.get();

        if (reference == null) {
            //first call from this thread
            return createAndCache(minSize);
        }

        Lease<T> lease = reference.get();

        if (lease == null) {
            //byte-array had been garbage collected
            return createAndCache(minSize);
        }

        if (lease.isInUse()) {
            //The lease is already in use, so we create a new one and cache that instance instead.
            //This can happen in two scenarios, either the lease is used in another method in the current
            //stacktrace, or the user forgot to close the lease at some point. Since the newly allocated buffer is
            // the cached instance after the following call, the old one (the cacheableBufferLease object ) can be gc'ed
            // and there will be no memory leak
            return createAndCache(minSize);
        }

        if (lease.bufferSize() < minSize) {
            //to small, allocate a bigger array and cache this instead
            return createAndCache(minSize);
        }

        //overwrite previous data
        lease.clearBuffer();

        return lease;
    }

    protected abstract Lease<T> createLease(int size);

    protected Reference<Lease<T>> reference(Lease<T> lease){
        return new SoftReference<>(lease);
    }

    private Lease<T> createAndCache(int minSize) {

        Lease<T> lease = createLease(min(initSize, minSize));

        Reference<Lease<T>> reference = reference(lease);

        threadLocal.set(reference);

        return lease;
    }
}

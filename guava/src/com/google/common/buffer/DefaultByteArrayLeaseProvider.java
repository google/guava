package com.google.common.buffer;

import com.google.common.annotations.GwtCompatible;

@GwtCompatible
class DefaultByteArrayLeaseProvider extends LeaseProviderBase<byte[]> {
    private static final int INIT_BYTE_SIZE = 4096;

    DefaultByteArrayLeaseProvider() {
        super(INIT_BYTE_SIZE);
    }

    @Override
    protected Lease<byte[]> createLease(int size) {
        return new DefaultByteArrayLease(size);
    }
}

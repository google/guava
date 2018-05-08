package com.google.common.buffer;

import com.google.common.annotations.GwtCompatible;

@GwtCompatible
class DefaultCharArrayLeaseProvider extends LeaseProviderBase<char[]> {
    private static final int INIT_CHAR_SIZE = 512;

    DefaultCharArrayLeaseProvider() {
        super(INIT_CHAR_SIZE);
    }

    @Override
    protected Lease<char[]> createLease(int size) {
        return new DefaultCharArrayLease(size);
    }
}

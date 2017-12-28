package com.google.common.buffer;

import com.google.common.annotations.GwtCompatible;

import java.io.ByteArrayOutputStream;

@GwtCompatible
class DefaultByteArrayOutputStreamLeaseProvider extends LeaseProviderBase<ByteArrayOutputStream> {

    private static final int INIT_BYTE_SIZE = 2048;

    DefaultByteArrayOutputStreamLeaseProvider() {
        super(INIT_BYTE_SIZE);
    }

    @Override
    protected Lease<ByteArrayOutputStream> createLease(int size) {
        return new DefaultByteArrayOutputStreamLease(size);
    }
}

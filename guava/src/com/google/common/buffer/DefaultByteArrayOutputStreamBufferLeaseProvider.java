package com.google.common.buffer;

import java.io.ByteArrayOutputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

class DefaultByteArrayOutputStreamBufferLeaseProvider extends ThreadLocalReferencedBufferLeaseProvider<ByteArrayOutputStream> {
    private static final int INIT_BYTE_SIZE = 4096;

    DefaultByteArrayOutputStreamBufferLeaseProvider() {
        super(INIT_BYTE_SIZE);
    }

    @Override
    protected Lease<ByteArrayOutputStream> createLease(int size) {
        //if size is odd, increase by one to make it even
        if(size % 2 == 1){
            ++size;
        }

        return new DefaultByteArrayOutputStreamLease(size);
    }

    @Override
    protected Reference<Lease<ByteArrayOutputStream>> reference(Lease<ByteArrayOutputStream> lease) {
        return new WeakReference<>(lease);
    }
}

package com.google.common.buffer;

import com.google.common.annotations.GwtCompatible;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@GwtCompatible
class DefaultByteArrayOutputStreamLease extends LeaseBase<ByteArrayOutputStream> {
    DefaultByteArrayOutputStreamLease(int size) {
        super(new ByteArrayOutputStream(size));
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void clearBuffer() {
        buffer.reset();
    }

    @Override
    public int bufferSize() {
        return buffer.size();
    }
}

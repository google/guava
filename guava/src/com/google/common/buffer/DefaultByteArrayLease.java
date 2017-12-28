package com.google.common.buffer;

import com.google.common.annotations.GwtCompatible;

import java.io.IOException;
import java.util.Arrays;

@GwtCompatible
class DefaultByteArrayLease extends LeaseBase<byte[]> {
    DefaultByteArrayLease(int size) {
        super(new byte[size]);
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void clearBuffer() {
        Arrays.fill(buffer, (byte)0);
    }

    @Override
    public int bufferSize() {
        return buffer.length;
    }
}

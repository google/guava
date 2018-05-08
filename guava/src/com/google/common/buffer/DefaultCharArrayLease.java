package com.google.common.buffer;

import com.google.common.annotations.GwtCompatible;

import java.io.IOException;
import java.util.Arrays;

@GwtCompatible
class DefaultCharArrayLease extends LeaseBase<char[]> {
    DefaultCharArrayLease(int size) {
        super(new char[size]);
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void clearBuffer() {
        Arrays.fill(buffer, (char)0);
    }

    @Override
    public int bufferSize() {
        return buffer.length;
    }
}

package com.google.common.buffer;

interface BufferLeaseProvider<T> {
    Lease<T> provide(int minSize);
}

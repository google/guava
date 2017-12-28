package com.google.common.buffer;

import com.google.common.annotations.GwtCompatible;

/**
 * A provider for a {@link Lease} of the type T.
 * Implement this interface to provide new types of buffers via {@link Buffers#lease(Class)}
 * and {@link Buffers#lease(Class, int)}.
 * @param <T> the type of buffer to be provided
 */
@GwtCompatible
interface LeaseProvider<T> {

    /**
     * provide a {@link Lease} of type T
     * @param minSize the minimum 'size' that a buffer needs to have. The actual 'size'-property
     *                depends on the type, it may also be ignored if no 'size' property is present
     *                for the buffer. Arrays, for example, have a 'length' property that is
     *                naturally the informal 'size'. So if the buffer type is an array and minSize
     *                is five, the array that is hold by the lease will have five or more elements.
     * @return a {@link Lease} holding the buffer
     */
    Lease<T> provide(int minSize);
}

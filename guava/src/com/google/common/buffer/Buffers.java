package com.google.common.buffer;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Allocates and provides buffers in the form of {@link Lease}s, this is
 * the main point of access for {@link Lease}s. It has built in {@link LeaseProvider}s
 * for {@link byte} and {@link char}
 *
 * @author Bernd Hopp
 * @since 21.0
 */
@Beta
@GwtCompatible
public final class Buffers {

    private static final Map<Class<?>, LeaseProvider<?>> providerMap = new ConcurrentHashMap<>();

    /**
     * this method will create an allocation for the buffer and
     * return a {@link Lease} object. The 'size' of the buffer depends
     * on the implementation of the {@link LeaseProvider}
     * @param type the class of the buffer to lease
     * @return a {@link Lease} of the buffer
     * @throws IllegalArgumentException if no {@link LeaseProvider} has been registered
     */
    public static <T> Lease<T> lease(Class<T> type){
        return lease(type, 0);
    }

    /**
     * this method will create an allocation for the buffer and
     * return a {@link Lease} object.
     * @param type the class of the buffer to lease
     * @param minSize the minimal size of the buffer. The actual meaning of 'size' is
     *                context-dependent, can be the number of bytes for byte-array and
     *                {@link ByteArrayOutputStream} or the number of chars for char-arrays
     *                and so forth.
     *                See {@link Lease#bufferSize()}.
     * @return a {@link Lease} of the buffer
     * @throws NullPointerException if type is null
     * @throws IllegalArgumentException if minSize is negative
     * @throws IllegalArgumentException if no {@link LeaseProvider} has been registered
     */
    public static <T> Lease<T> lease(Class<T> type, int minSize){
        checkNotNull(type);
        checkArgument(minSize >= 0);

        @SuppressWarnings("unchecked")
        final LeaseProvider<T> leaseProvider = (LeaseProvider<T>)providerMap.get(type);

        checkArgument(leaseProvider != null, "no LeaseProvider registered for type %s", type);

        return leaseProvider.provide(minSize);
    }

    /**
     * add's or overwrites the {@link LeaseProvider} for a certain type of buffer
     * @param type the class of the buffer
     * @param provider the provider for the buffer {@link Lease}s
     */
    public static <T> void setProvider(Class<T> type, LeaseProvider<T> provider){
        checkNotNull(type);
        checkNotNull(provider);
        providerMap.put(type, provider);
    }

    private Buffers() {
    }

    static {
        setProvider(byte[].class, new DefaultByteArrayLeaseProvider());
        setProvider(char[].class, new DefaultCharArrayLeaseProvider());
        setProvider(ByteArrayOutputStream.class, new DefaultByteArrayOutputStreamLeaseProvider());
    }
}

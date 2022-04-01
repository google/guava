/*
 * Copyright (C) 2015 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.hash;

import com.google.common.primitives.Longs;
import java.lang.reflect.Field;
import java.nio.ByteOrder;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import sun.misc.Unsafe;

/**
 * Utility functions for loading and storing values from a byte array.
 *
 * @author Kevin Damm
 * @author Kyle Maddison
 */
@ElementTypesAreNonnullByDefault
final class LittleEndianByteArray {

  /** The instance that actually does the work; delegates to Unsafe or a pure-Java fallback. */
  private static final LittleEndianBytes byteArray;

  /**
   * Load 8 bytes into long in a little endian manner, from the substring between position and
   * position + 8. The array must have at least 8 bytes from offset (inclusive).
   *
   * @param input the input bytes
   * @param offset the offset into the array at which to start
   * @return a long of a concatenated 8 bytes
   */
  static long load64(byte[] input, int offset) {
    // We don't want this in production code as this is the most critical part of the loop.
    assert input.length >= offset + 8;
    // Delegates to the fast (unsafe) version or the fallback.
    return byteArray.getLongLittleEndian(input, offset);
  }

  /**
   * Similar to load64, but allows offset + 8 > input.length, padding the result with zeroes. This
   * has to explicitly reverse the order of the bytes as it packs them into the result which makes
   * it slower than the native version.
   *
   * @param input the input bytes
   * @param offset the offset into the array at which to start reading
   * @param length the number of bytes from the input to read
   * @return a long of a concatenated 8 bytes
   */
  static long load64Safely(byte[] input, int offset, int length) {
    long result = 0;
    // Due to the way we shift, we can stop iterating once we've run out of data, the rest
    // of the result already being filled with zeros.

    // This loop is critical to performance, so please check HashBenchmark if altering it.
    int limit = Math.min(length, 8);
    for (int i = 0; i < limit; i++) {
      // Shift value left while iterating logically through the array.
      result |= (input[offset + i] & 0xFFL) << (i * 8);
    }
    return result;
  }

  /**
   * Store 8 bytes into the provided array at the indicated offset, using the value provided.
   *
   * @param sink the output byte array
   * @param offset the offset into the array at which to start writing
   * @param value the value to write
   */
  static void store64(byte[] sink, int offset, long value) {
    // We don't want to assert in production code.
    assert offset >= 0 && offset + 8 <= sink.length;
    // Delegates to the fast (unsafe)version or the fallback.
    byteArray.putLongLittleEndian(sink, offset, value);
  }

  /**
   * Load 4 bytes from the provided array at the indicated offset.
   *
   * @param source the input bytes
   * @param offset the offset into the array at which to start
   * @return the value found in the array in the form of a long
   */
  static int load32(byte[] source, int offset) {
    // TODO(user): Measure the benefit of delegating this to LittleEndianBytes also.
    return (source[offset] & 0xFF)
        | ((source[offset + 1] & 0xFF) << 8)
        | ((source[offset + 2] & 0xFF) << 16)
        | ((source[offset + 3] & 0xFF) << 24);
  }

  /**
   * Indicates that the loading of Unsafe was successful and the load and store operations will be
   * very efficient. May be useful for calling code to fall back on an alternative implementation
   * that is slower than Unsafe.get/store but faster than the pure-Java mask-and-shift.
   */
  static boolean usingUnsafe() {
    return (byteArray instanceof UnsafeByteArray);
  }

  /**
   * Common interface for retrieving a 64-bit long from a little-endian byte array.
   *
   * <p>This abstraction allows us to use single-instruction load and put when available, or fall
   * back on the slower approach of using Longs.fromBytes(byte...).
   */
  private interface LittleEndianBytes {
    long getLongLittleEndian(byte[] array, int offset);

    void putLongLittleEndian(byte[] array, int offset, long value);
  }

  /**
   * The only reference to Unsafe is in this nested class. We set things up so that if
   * Unsafe.theUnsafe is inaccessible, the attempt to load the nested class fails, and the outer
   * class's static initializer can fall back on a non-Unsafe version.
   */
  private enum UnsafeByteArray implements LittleEndianBytes {
    // Do *not* change the order of these constants!
    UNSAFE_LITTLE_ENDIAN {
      @Override
      public long getLongLittleEndian(byte[] array, int offset) {
        return theUnsafe.getLong(array, (long) offset + BYTE_ARRAY_BASE_OFFSET);
      }

      @Override
      public void putLongLittleEndian(byte[] array, int offset, long value) {
        theUnsafe.putLong(array, (long) offset + BYTE_ARRAY_BASE_OFFSET, value);
      }
    },
    UNSAFE_BIG_ENDIAN {
      @Override
      public long getLongLittleEndian(byte[] array, int offset) {
        long bigEndian = theUnsafe.getLong(array, (long) offset + BYTE_ARRAY_BASE_OFFSET);
        // The hardware is big-endian, so we need to reverse the order of the bytes.
        return Long.reverseBytes(bigEndian);
      }

      @Override
      public void putLongLittleEndian(byte[] array, int offset, long value) {
        // Reverse the order of the bytes before storing, since we're on big-endian hardware.
        long littleEndianValue = Long.reverseBytes(value);
        theUnsafe.putLong(array, (long) offset + BYTE_ARRAY_BASE_OFFSET, littleEndianValue);
      }
    };

    // Provides load and store operations that use native instructions to get better performance.
    private static final Unsafe theUnsafe;

    // The offset to the first element in a byte array.
    private static final int BYTE_ARRAY_BASE_OFFSET;

    /**
     * Returns an Unsafe. Suitable for use in a 3rd party package. Replace with a simple call to
     * Unsafe.getUnsafe when integrating into a JDK.
     *
     * @return an Unsafe instance if successful
     */
    private static Unsafe getUnsafe() {
      try {
        return Unsafe.getUnsafe();
      } catch (SecurityException tryReflectionInstead) {
        // We'll try reflection instead.
      }
      try {
        return AccessController.doPrivileged(
            (PrivilegedExceptionAction<Unsafe>)
                () -> {
                  Class<Unsafe> k = Unsafe.class;
                  for (Field f : k.getDeclaredFields()) {
                    f.setAccessible(true);
                    Object x = f.get(null);
                    if (k.isInstance(x)) {
                      return k.cast(x);
                    }
                  }
                  throw new NoSuchFieldError("the Unsafe");
                });
      } catch (PrivilegedActionException e) {
        throw new RuntimeException("Could not initialize intrinsics", e.getCause());
      }
    }

    static {
      theUnsafe = getUnsafe();
      BYTE_ARRAY_BASE_OFFSET = theUnsafe.arrayBaseOffset(byte[].class);

      // sanity check - this should never fail
      if (theUnsafe.arrayIndexScale(byte[].class) != 1) {
        throw new AssertionError();
      }
    }
  }

  /** Fallback implementation for when Unsafe is not available in our current environment. */
  private enum JavaLittleEndianBytes implements LittleEndianBytes {
    INSTANCE {
      @Override
      public long getLongLittleEndian(byte[] source, int offset) {
        return Longs.fromBytes(
            source[offset + 7],
            source[offset + 6],
            source[offset + 5],
            source[offset + 4],
            source[offset + 3],
            source[offset + 2],
            source[offset + 1],
            source[offset]);
      }

      @Override
      public void putLongLittleEndian(byte[] sink, int offset, long value) {
        long mask = 0xFFL;
        for (int i = 0; i < 8; mask <<= 8, i++) {
          sink[offset + i] = (byte) ((value & mask) >> (i * 8));
        }
      }
    }
  }

  static {
    LittleEndianBytes theGetter = JavaLittleEndianBytes.INSTANCE;
    try {
      /*
       * UnsafeByteArray uses Unsafe.getLong() in an unsupported way, which is known to cause
       * crashes on Android when running in 32-bit mode. For maximum safety, we shouldn't use
       * Unsafe.getLong() at all, but the performance benefit on x86_64 is too great to ignore, so
       * as a compromise, we enable the optimization only on platforms that we specifically know to
       * work.
       *
       * In the future, the use of Unsafe.getLong() should be replaced by ByteBuffer.getLong(),
       * which will have an efficient native implementation in JDK 9.
       *
       */
      String arch = System.getProperty("os.arch");
      if ("amd64".equals(arch)) {
        theGetter =
            ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)
                ? UnsafeByteArray.UNSAFE_LITTLE_ENDIAN
                : UnsafeByteArray.UNSAFE_BIG_ENDIAN;
      }
    } catch (Throwable t) {
      // ensure we really catch *everything*
    }
    byteArray = theGetter;
  }

  /** Deter instantiation of this class. */
  private LittleEndianByteArray() {}
}

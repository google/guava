/*
 * Copyright (C) 2009 The Guava Authors
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

package com.google.common.primitives;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndexes;
import static java.lang.Byte.toUnsignedInt;
import static java.security.AccessController.doPrivileged;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.InlineMe;
import com.google.j2objc.annotations.J2ObjCIncompatible;
import java.lang.reflect.Field;
import java.nio.ByteOrder;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import sun.misc.Unsafe;

/**
 * Static utility methods pertaining to {@code byte} primitives that interpret values as
 * <i>unsigned</i> (that is, any negative value {@code b} is treated as the positive value {@code
 * 256 + b}). The corresponding methods that treat the values as signed are found in {@link
 * SignedBytes}, and the methods for which signedness is not an issue are in {@link Bytes}.
 *
 * <p>See the Guava User Guide article on <a
 * href="https://github.com/google/guava/wiki/PrimitivesExplained">primitive utilities</a>.
 *
 * @author Kevin Bourrillion
 * @author Martin Buchholz
 * @author Hiroshi Yamauchi
 * @author Louis Wasserman
 * @since 1.0
 */
@J2ktIncompatible
@GwtIncompatible
public final class UnsignedBytes {
  private UnsignedBytes() {}

  /**
   * The largest power of two that can be represented as an unsigned {@code byte}.
   *
   * @since 10.0
   */
  public static final byte MAX_POWER_OF_TWO = (byte) 0x80;

  /**
   * The largest value that fits into an unsigned byte.
   *
   * @since 13.0
   */
  public static final byte MAX_VALUE = (byte) 0xFF;

  private static final int UNSIGNED_MASK = 0xFF;

  /**
   * Returns the value of the given byte as an integer, when treated as unsigned. That is, returns
   * {@code value + 256} if {@code value} is negative; {@code value} itself otherwise.
   *
   * <p>Prefer {@link Byte#toUnsignedInt(byte)} instead.
   *
   * @since 6.0
   */
  @InlineMe(replacement = "Byte.toUnsignedInt(value)")
  public static int toInt(byte value) {
    return Byte.toUnsignedInt(value);
  }

  /**
   * Returns the {@code byte} value that, when treated as unsigned, is equal to {@code value}, if
   * possible.
   *
   * @param value a value between 0 and 255 inclusive
   * @return the {@code byte} value that, when treated as unsigned, equals {@code value}
   * @throws IllegalArgumentException if {@code value} is negative or greater than 255
   */
  @CanIgnoreReturnValue
  public static byte checkedCast(long value) {
    checkArgument(value >> Byte.SIZE == 0, "out of range: %s", value);
    return (byte) value;
  }

  /**
   * Returns the {@code byte} value that, when treated as unsigned, is nearest in value to {@code
   * value}.
   *
   * @param value any {@code long} value
   * @return {@code (byte) 255} if {@code value >= 255}, {@code (byte) 0} if {@code value <= 0}, and
   *     {@code value} cast to {@code byte} otherwise
   */
  public static byte saturatedCast(long value) {
    if (value > toUnsignedInt(MAX_VALUE)) {
      return MAX_VALUE; // -1
    }
    if (value < 0) {
      return (byte) 0;
    }
    return (byte) value;
  }

  /**
   * Compares the two specified {@code byte} values, treating them as unsigned values between 0 and
   * 255 inclusive. For example, {@code (byte) -127} is considered greater than {@code (byte) 127}
   * because it is seen as having the value of positive {@code 129}.
   *
   * @param a the first {@code byte} to compare
   * @param b the second {@code byte} to compare
   * @return a negative value if {@code a} is less than {@code b}; a positive value if {@code a} is
   *     greater than {@code b}; or zero if they are equal
   */
  public static int compare(byte a, byte b) {
    return toUnsignedInt(a) - toUnsignedInt(b);
  }

  /**
   * Returns the least value present in {@code array}, treating values as unsigned.
   *
   * @param array a <i>nonempty</i> array of {@code byte} values
   * @return the value present in {@code array} that is less than or equal to every other value in
   *     the array according to {@link #compare}
   * @throws IllegalArgumentException if {@code array} is empty
   */
  public static byte min(byte... array) {
    checkArgument(array.length > 0);
    int min = toUnsignedInt(array[0]);
    for (int i = 1; i < array.length; i++) {
      int next = toUnsignedInt(array[i]);
      if (next < min) {
        min = next;
      }
    }
    return (byte) min;
  }

  /**
   * Returns the greatest value present in {@code array}, treating values as unsigned.
   *
   * @param array a <i>nonempty</i> array of {@code byte} values
   * @return the value present in {@code array} that is greater than or equal to every other value
   *     in the array according to {@link #compare}
   * @throws IllegalArgumentException if {@code array} is empty
   */
  public static byte max(byte... array) {
    checkArgument(array.length > 0);
    int max = toUnsignedInt(array[0]);
    for (int i = 1; i < array.length; i++) {
      int next = toUnsignedInt(array[i]);
      if (next > max) {
        max = next;
      }
    }
    return (byte) max;
  }

  /**
   * Returns a string representation of x, where x is treated as unsigned.
   *
   * @since 13.0
   */
  public static String toString(byte x) {
    return toString(x, 10);
  }

  /**
   * Returns a string representation of {@code x} for the given radix, where {@code x} is treated as
   * unsigned.
   *
   * @param x the value to convert to a string.
   * @param radix the radix to use while working with {@code x}
   * @throws IllegalArgumentException if {@code radix} is not between {@link Character#MIN_RADIX}
   *     and {@link Character#MAX_RADIX}.
   * @since 13.0
   */
  public static String toString(byte x, int radix) {
    checkArgument(
        radix >= Character.MIN_RADIX && radix <= Character.MAX_RADIX,
        "radix (%s) must be between Character.MIN_RADIX and Character.MAX_RADIX",
        radix);
    // Benchmarks indicate this is probably not worth optimizing.
    return Integer.toString(toUnsignedInt(x), radix);
  }

  /**
   * Returns the unsigned {@code byte} value represented by the given decimal string.
   *
   * @throws NumberFormatException if the string does not contain a valid unsigned {@code byte}
   *     value
   * @throws NullPointerException if {@code string} is null (in contrast to {@link
   *     Byte#parseByte(String)})
   * @since 13.0
   */
  @CanIgnoreReturnValue
  public static byte parseUnsignedByte(String string) {
    return parseUnsignedByte(string, 10);
  }

  /**
   * Returns the unsigned {@code byte} value represented by a string with the given radix.
   *
   * @param string the string containing the unsigned {@code byte} representation to be parsed.
   * @param radix the radix to use while parsing {@code string}
   * @throws NumberFormatException if the string does not contain a valid unsigned {@code byte} with
   *     the given radix, or if {@code radix} is not between {@link Character#MIN_RADIX} and {@link
   *     Character#MAX_RADIX}.
   * @throws NullPointerException if {@code string} is null (in contrast to {@link
   *     Byte#parseByte(String)})
   * @since 13.0
   */
  @CanIgnoreReturnValue
  public static byte parseUnsignedByte(String string, int radix) {
    int parse = Integer.parseInt(checkNotNull(string), radix);
    // We need to throw a NumberFormatException, so we have to duplicate checkedCast. =(
    if (parse >> Byte.SIZE == 0) {
      return (byte) parse;
    } else {
      throw new NumberFormatException("out of range: " + parse);
    }
  }

  /**
   * Returns a string containing the supplied {@code byte} values separated by {@code separator}.
   * For example, {@code join(":", (byte) 1, (byte) 2, (byte) 255)} returns the string {@code
   * "1:2:255"}.
   *
   * @param separator the text that should appear between consecutive values in the resulting string
   *     (but not at the start or end)
   * @param array an array of {@code byte} values, possibly empty
   */
  public static String join(String separator, byte... array) {
    checkNotNull(separator);
    if (array.length == 0) {
      return "";
    }

    // For pre-sizing a builder, just get the right order of magnitude
    StringBuilder builder = new StringBuilder(array.length * (3 + separator.length()));
    builder.append(toUnsignedInt(array[0]));
    for (int i = 1; i < array.length; i++) {
      builder.append(separator).append(toString(array[i]));
    }
    return builder.toString();
  }

  /**
   * Returns a comparator that compares two {@code byte} arrays <a
   * href="http://en.wikipedia.org/wiki/Lexicographical_order">lexicographically</a>. That is, it
   * compares, using {@link #compare(byte, byte)}), the first pair of values that follow any common
   * prefix, or when one array is a prefix of the other, treats the shorter array as the lesser. For
   * example, {@code [] < [0x01] < [0x01, 0x7F] < [0x01, 0x80] < [0x02]}. Values are treated as
   * unsigned.
   *
   * <p>The returned comparator is inconsistent with {@link Object#equals(Object)} (since arrays
   * support only identity equality), but it is consistent with {@link
   * java.util.Arrays#equals(byte[], byte[])}.
   *
   * <p><b>Java 9+ users:</b> Use {@link Arrays#compareUnsigned(byte[], byte[])
   * Arrays::compareUnsigned}.
   *
   * @since 2.0
   */
  public static Comparator<byte[]> lexicographicalComparator() {
    return LexicographicalComparatorHolder.BEST_COMPARATOR;
  }

  @VisibleForTesting
  static Comparator<byte[]> lexicographicalComparatorJavaImpl() {
    return LexicographicalComparatorHolder.PureJavaComparator.INSTANCE;
  }

  /** Provides a lexicographical comparator implementation selected based on the current runtime. */
  @VisibleForTesting
  static final class LexicographicalComparatorHolder {
    /**
     * Interface implemented by {@link UnsafeComparator}, extracted so that we can refer to it
     * without referring to {@link UnsafeComparator} itself.
     */
    interface LexicographicalComparator extends Comparator<byte[]> {
      /** Returns whether this implementation is available for use on the current platform. */
      boolean isFunctional();
    }

    static final String UNSAFE_COMPARATOR_NAME =
        LexicographicalComparatorHolder.class.getName() + "$UnsafeComparator";

    static final Comparator<byte[]> BEST_COMPARATOR = getBestComparator();

    @SuppressWarnings({
      // b/345822163
      "SunApi",
      "deprecation",
      // The `deprecation` suppression is for Unsafe APIs that aren't deprecated under some versions
      "UnnecessaryJavacSuppressWarnings"
    })
    @VisibleForTesting
    enum UnsafeComparator implements LexicographicalComparator {
      INSTANCE;

      static final boolean BIG_ENDIAN = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN);

      /*
       * The following static final fields exist for performance reasons.
       *
       * In UnsignedBytesBenchmark, accessing the following objects via static final fields is the
       * fastest (more than twice as fast as the Java implementation, vs ~1.5x with non-final static
       * fields, on x86_32) under the Hotspot server compiler. The reason is obviously that the
       * non-final fields need to be reloaded inside the loop.
       *
       * And, no, defining (final or not) local variables out of the loop still isn't as good
       * because the null check on the theUnsafe object remains inside the loop and
       * BYTE_ARRAY_BASE_OFFSET doesn't get constant-folded.
       *
       * The compiler can treat static final fields as compile-time constants and can constant-fold
       * them while (final or not) local variables are run time values.
       */

      /**
       * Value stored in {@link #BYTE_ARRAY_BASE_OFFSET} to indicate that the current runtime does
       * not support the {@link Unsafe} comparator.
       */
      static final int OFFSET_UNSAFE_APPROACH_IS_UNAVAILABLE = -1;

      static final @Nullable Unsafe theUnsafe = getUnsafe();

      /**
       * The offset to the first element in a byte array, or {@link
       * #OFFSET_UNSAFE_APPROACH_IS_UNAVAILABLE}.
       */
      static final int BYTE_ARRAY_BASE_OFFSET = getByteArrayBaseOffset();

      private static int getByteArrayBaseOffset() {
        if (theUnsafe == null) {
          return OFFSET_UNSAFE_APPROACH_IS_UNAVAILABLE;
        }

        try {
          int offset = theUnsafe.arrayBaseOffset(byte[].class);
          int scale = theUnsafe.arrayIndexScale(byte[].class);

          // Use Unsafe only if we're in a 64-bit JVM with an 8-byte aligned field offset.
          if (Objects.equals(System.getProperty("sun.arch.data.model"), "64")
              && (offset % 8) == 0
              // sanity check - this should never fail
              && scale == 1) {
            return offset;
          }

          return OFFSET_UNSAFE_APPROACH_IS_UNAVAILABLE;
        } catch (UnsupportedOperationException e) {
          return OFFSET_UNSAFE_APPROACH_IS_UNAVAILABLE;
        }
      }

      private static @Nullable Unsafe getUnsafe() {
        try {
          return Unsafe.getUnsafe();
        } catch (SecurityException e) {
          // that's okay; try reflection instead
        }
        try {
          return doPrivileged(
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
                    return null;
                  });
        } catch (PrivilegedActionException e) {
          return null;
        }
      }

      @Override
      public boolean isFunctional() {
        return BYTE_ARRAY_BASE_OFFSET != OFFSET_UNSAFE_APPROACH_IS_UNAVAILABLE;
      }

      @Override
      public int compare(byte[] left, byte[] right) {
        // If theUnsafe weren't available, we wouldn't have selected this Comparator implementation.
        Unsafe theUnsafe = requireNonNull(UnsafeComparator.theUnsafe);

        int stride = 8;
        int minLength = Math.min(left.length, right.length);
        int strideLimit = minLength & ~(stride - 1);
        int i;

        /*
         * Compare 8 bytes at a time. Benchmarking on x86 shows a stride of 8 bytes is no slower
         * than 4 bytes even on 32-bit. On the other hand, it is substantially faster on 64-bit.
         */
        for (i = 0; i < strideLimit; i += stride) {
          long lw = theUnsafe.getLong(left, BYTE_ARRAY_BASE_OFFSET + (long) i);
          long rw = theUnsafe.getLong(right, BYTE_ARRAY_BASE_OFFSET + (long) i);
          if (lw != rw) {
            if (BIG_ENDIAN) {
              return Long.compareUnsigned(lw, rw);
            }

            /*
             * We want to compare only the first index where left[index] != right[index]. This
             * corresponds to the least significant nonzero byte in lw ^ rw, since lw and rw are
             * little-endian. Long.numberOfTrailingZeros(diff) tells us the least significant
             * nonzero bit, and zeroing out the first three bits of L.nTZ gives us the shift to get
             * that least significant nonzero byte.
             */
            int n = Long.numberOfTrailingZeros(lw ^ rw) & ~0x7;
            return ((int) ((lw >>> n) & UNSIGNED_MASK)) - ((int) ((rw >>> n) & UNSIGNED_MASK));
          }
        }

        // The epilogue to cover the last (minLength % stride) elements.
        for (; i < minLength; i++) {
          int result = UnsignedBytes.compare(left[i], right[i]);
          if (result != 0) {
            return result;
          }
        }
        return left.length - right.length;
      }

      @Override
      public String toString() {
        return "UnsignedBytes.lexicographicalComparator() (sun.misc.Unsafe version)";
      }
    }

    enum PureJavaComparator implements Comparator<byte[]> {
      INSTANCE;

      @Override
      public int compare(byte[] left, byte[] right) {
        int minLength = Math.min(left.length, right.length);
        for (int i = 0; i < minLength; i++) {
          int result = UnsignedBytes.compare(left[i], right[i]);
          if (result != 0) {
            return result;
          }
        }
        return left.length - right.length;
      }

      @Override
      public String toString() {
        return "UnsignedBytes.lexicographicalComparator() (pure Java version)";
      }
    }

    /** Returns the best comparator supported by the current runtime. */
    static Comparator<byte[]> getBestComparator() {
      Comparator<byte[]> arraysCompareUnsignedComparator =
          ArraysCompareUnsignedComparatorMaker.INSTANCE.tryMakeArraysCompareUnsignedComparator();
      if (arraysCompareUnsignedComparator != null) {
        return arraysCompareUnsignedComparator;
      }

      try {
        Class<? extends LexicographicalComparator> unsafeImpl =
            Class.forName(UNSAFE_COMPARATOR_NAME).asSubclass(LexicographicalComparator.class);
        // requireNonNull is safe because the class is an enum.
        LexicographicalComparator unsafeComparator =
            requireNonNull(unsafeImpl.getEnumConstants())[0];
        return unsafeComparator.isFunctional()
            ? unsafeComparator
            : lexicographicalComparatorJavaImpl();
      } catch (Throwable t) { // ensure we really catch *everything*
        /*
         * Now that UnsafeComparator is implemented to initialize successfully even when we know we
         * can't use it, this `catch` block might now be necessary only:
         *
         * - in the Android flavor or anywhere else that users might be applying an optimizer that
         * might strip UnsafeComparator entirely. (TODO(cpovirk): Are we confident that optimizers
         * aren't stripping UnsafeComparator today? Should we have Proguard configuration for it?)
         *
         * - if Unsafe is removed entirely from JDKs (or already absent in some unusual environment
         * today). TODO: b/392974826 - Check for the existence of Unsafe and its methods
         * reflectively before attempting to access UnsafeComparator. Or, better yet, allow
         * UnsafeComparator to still initialize correctly even if Unsafe is unavailable. This would
         * protect against users that automatically preinitialize internal classes that they've seen
         * initialized in their apps in the past. To do that, we may need to move the references to
         * Unsafe to another class and then ensure that the preinitialization logic doesn't start
         * picking up the new class as part of loading UnsafeComparator!
         */
        return lexicographicalComparatorJavaImpl();
      }
    }

    private LexicographicalComparatorHolder() {}
  }

  private enum ArraysCompareUnsignedComparatorMaker {
    INSTANCE {
      /** Implementation used by non-J2ObjC environments. */
      // We use Arrays.compareUnsigned only after confirming that it's available at runtime.
      @SuppressWarnings("Java8ApiChecker")
      @IgnoreJRERequirement
      @Override
      @J2ObjCIncompatible
      @Nullable Comparator<byte[]> tryMakeArraysCompareUnsignedComparator() {
        try {
          // Compare AbstractFuture.VarHandleAtomicHelperMaker.
          Arrays.class.getMethod("compareUnsigned", byte[].class, byte[].class);
        } catch (NoSuchMethodException beforeJava9) {
          return null;
        }
        return ArraysCompareUnsignedComparator.INSTANCE;
      }

      // TODO(cpovirk): Implement toString, as the other implementations do?
    };

    /** Implementation used by J2ObjC environments, overridden for other environments. */
    @Nullable Comparator<byte[]> tryMakeArraysCompareUnsignedComparator() {
      return null;
    }
  }

  @J2ObjCIncompatible
  enum ArraysCompareUnsignedComparator implements Comparator<byte[]> {
    INSTANCE;

    @Override
    // We use the class only after confirming that Arrays.compareUnsigned is available at runtime.
    @SuppressWarnings("Java8ApiChecker")
    @IgnoreJRERequirement
    public int compare(byte[] left, byte[] right) {
      return Arrays.compareUnsigned(left, right);
    }
  }

  private static byte flip(byte b) {
    return (byte) (b ^ 0x80);
  }

  /**
   * Sorts the array, treating its elements as unsigned bytes.
   *
   * @since 23.1
   */
  public static void sort(byte[] array) {
    checkNotNull(array);
    sort(array, 0, array.length);
  }

  /**
   * Sorts the array between {@code fromIndex} inclusive and {@code toIndex} exclusive, treating its
   * elements as unsigned bytes.
   *
   * @since 23.1
   */
  public static void sort(byte[] array, int fromIndex, int toIndex) {
    checkNotNull(array);
    checkPositionIndexes(fromIndex, toIndex, array.length);
    for (int i = fromIndex; i < toIndex; i++) {
      array[i] = flip(array[i]);
    }
    Arrays.sort(array, fromIndex, toIndex);
    for (int i = fromIndex; i < toIndex; i++) {
      array[i] = flip(array[i]);
    }
  }

  /**
   * Sorts the elements of {@code array} in descending order, interpreting them as unsigned 8-bit
   * integers.
   *
   * @since 23.1
   */
  public static void sortDescending(byte[] array) {
    checkNotNull(array);
    sortDescending(array, 0, array.length);
  }

  /**
   * Sorts the elements of {@code array} between {@code fromIndex} inclusive and {@code toIndex}
   * exclusive in descending order, interpreting them as unsigned 8-bit integers.
   *
   * @since 23.1
   */
  public static void sortDescending(byte[] array, int fromIndex, int toIndex) {
    checkNotNull(array);
    checkPositionIndexes(fromIndex, toIndex, array.length);
    for (int i = fromIndex; i < toIndex; i++) {
      array[i] ^= Byte.MAX_VALUE;
    }
    Arrays.sort(array, fromIndex, toIndex);
    for (int i = fromIndex; i < toIndex; i++) {
      array[i] ^= Byte.MAX_VALUE;
    }
  }
}

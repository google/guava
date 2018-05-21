/*
 * Copyright (C) 2008 The Guava Authors
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
import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndexes;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Converter;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.RandomAccess;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Static utility methods pertaining to {@code short} primitives, that are not already found in
 * either {@link Short} or {@link Arrays}.
 *
 * <p>See the Guava User Guide article on <a
 * href="https://github.com/google/guava/wiki/PrimitivesExplained">primitive utilities</a>.
 *
 * @author Kevin Bourrillion
 * @since 1.0
 */
@GwtCompatible(emulated = true)
public final class Shorts {
  private Shorts() {}

  /**
   * The number of bytes required to represent a primitive {@code short} value.
   *
   * <p><b>Java 8 users:</b> use {@link Short#BYTES} instead.
   */
  public static final int BYTES = Short.SIZE / Byte.SIZE;

  /**
   * The largest power of two that can be represented as a {@code short}.
   *
   * @since 10.0
   */
  public static final short MAX_POWER_OF_TWO = 1 << (Short.SIZE - 2);

  /**
   * Returns a hash code for {@code value}; equal to the result of invoking {@code ((Short)
   * value).hashCode()}.
   *
   * <p><b>Java 8 users:</b> use {@link Short#hashCode(short)} instead.
   *
   * @param value a primitive {@code short} value
   * @return a hash code for the value
   */
  public static int hashCode(short value) {
    return value;
  }

  /**
   * Returns the {@code short} value that is equal to {@code value}, if possible.
   *
   * @param value any value in the range of the {@code short} type
   * @return the {@code short} value that equals {@code value}
   * @throws IllegalArgumentException if {@code value} is greater than {@link Short#MAX_VALUE} or
   *     less than {@link Short#MIN_VALUE}
   */
  public static short checkedCast(long value) {
    short result = (short) value;
    checkArgument(result == value, "Out of range: %s", value);
    return result;
  }

  /**
   * Returns the {@code short} nearest in value to {@code value}.
   *
   * @param value any {@code long} value
   * @return the same value cast to {@code short} if it is in the range of the {@code short} type,
   *     {@link Short#MAX_VALUE} if it is too large, or {@link Short#MIN_VALUE} if it is too small
   */
  public static short saturatedCast(long value) {
    if (value > Short.MAX_VALUE) {
      return Short.MAX_VALUE;
    }
    if (value < Short.MIN_VALUE) {
      return Short.MIN_VALUE;
    }
    return (short) value;
  }

  /**
   * Compares the two specified {@code short} values. The sign of the value returned is the same as
   * that of {@code ((Short) a).compareTo(b)}.
   *
   * <p><b>Note for Java 7 and later:</b> this method should be treated as deprecated; use the
   * equivalent {@link Short#compare} method instead.
   *
   * @param a the first {@code short} to compare
   * @param b the second {@code short} to compare
   * @return a negative value if {@code a} is less than {@code b}; a positive value if {@code a} is
   *     greater than {@code b}; or zero if they are equal
   */
  public static int compare(short a, short b) {
    return a - b; // safe due to restricted range
  }

  /**
   * Returns {@code true} if {@code target} is present as an element anywhere in {@code array}.
   *
   * @param array an array of {@code short} values, possibly empty
   * @param target a primitive {@code short} value
   * @return {@code true} if {@code array[i] == target} for some value of {@code i}
   */
  public static boolean contains(short[] array, short target) {
    for (short value : array) {
      if (value == target) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the index of the first appearance of the value {@code target} in {@code array}.
   *
   * @param array an array of {@code short} values, possibly empty
   * @param target a primitive {@code short} value
   * @return the least index {@code i} for which {@code array[i] == target}, or {@code -1} if no
   *     such index exists.
   */
  public static int indexOf(short[] array, short target) {
    return indexOf(array, target, 0, array.length);
  }

  // TODO(kevinb): consider making this public
  private static int indexOf(short[] array, short target, int start, int end) {
    for (int i = start; i < end; i++) {
      if (array[i] == target) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Returns the start position of the first occurrence of the specified {@code target} within
   * {@code array}, or {@code -1} if there is no such occurrence.
   *
   * <p>More formally, returns the lowest index {@code i} such that {@code Arrays.copyOfRange(array,
   * i, i + target.length)} contains exactly the same elements as {@code target}.
   *
   * @param array the array to search for the sequence {@code target}
   * @param target the array to search for as a sub-sequence of {@code array}
   */
  public static int indexOf(short[] array, short[] target) {
    checkNotNull(array, "array");
    checkNotNull(target, "target");
    if (target.length == 0) {
      return 0;
    }

    outer:
    for (int i = 0; i < array.length - target.length + 1; i++) {
      for (int j = 0; j < target.length; j++) {
        if (array[i + j] != target[j]) {
          continue outer;
        }
      }
      return i;
    }
    return -1;
  }

  /**
   * Returns the index of the last appearance of the value {@code target} in {@code array}.
   *
   * @param array an array of {@code short} values, possibly empty
   * @param target a primitive {@code short} value
   * @return the greatest index {@code i} for which {@code array[i] == target}, or {@code -1} if no
   *     such index exists.
   */
  public static int lastIndexOf(short[] array, short target) {
    return lastIndexOf(array, target, 0, array.length);
  }

  // TODO(kevinb): consider making this public
  private static int lastIndexOf(short[] array, short target, int start, int end) {
    for (int i = end - 1; i >= start; i--) {
      if (array[i] == target) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Returns the least value present in {@code array}.
   *
   * @param array a <i>nonempty</i> array of {@code short} values
   * @return the value present in {@code array} that is less than or equal to every other value in
   *     the array
   * @throws IllegalArgumentException if {@code array} is empty
   */
  public static short min(short... array) {
    checkArgument(array.length > 0);
    short min = array[0];
    for (int i = 1; i < array.length; i++) {
      if (array[i] < min) {
        min = array[i];
      }
    }
    return min;
  }

  /**
   * Returns the greatest value present in {@code array}.
   *
   * @param array a <i>nonempty</i> array of {@code short} values
   * @return the value present in {@code array} that is greater than or equal to every other value
   *     in the array
   * @throws IllegalArgumentException if {@code array} is empty
   */
  public static short max(short... array) {
    checkArgument(array.length > 0);
    short max = array[0];
    for (int i = 1; i < array.length; i++) {
      if (array[i] > max) {
        max = array[i];
      }
    }
    return max;
  }

  /**
   * Returns the value nearest to {@code value} which is within the closed range {@code [min..max]}.
   *
   * <p>If {@code value} is within the range {@code [min..max]}, {@code value} is returned
   * unchanged. If {@code value} is less than {@code min}, {@code min} is returned, and if {@code
   * value} is greater than {@code max}, {@code max} is returned.
   *
   * @param value the {@code short} value to constrain
   * @param min the lower bound (inclusive) of the range to constrain {@code value} to
   * @param max the upper bound (inclusive) of the range to constrain {@code value} to
   * @throws IllegalArgumentException if {@code min > max}
   * @since 21.0
   */
  @Beta
  public static short constrainToRange(short value, short min, short max) {
    checkArgument(min <= max, "min (%s) must be less than or equal to max (%s)", min, max);
    return value < min ? min : value < max ? value : max;
  }

  /**
   * Returns the values from each provided array combined into a single array. For example, {@code
   * concat(new short[] {a, b}, new short[] {}, new short[] {c}} returns the array {@code {a, b,
   * c}}.
   *
   * @param arrays zero or more {@code short} arrays
   * @return a single array containing all the values from the source arrays, in order
   */
  public static short[] concat(short[]... arrays) {
    int length = 0;
    for (short[] array : arrays) {
      length += array.length;
    }
    short[] result = new short[length];
    int pos = 0;
    for (short[] array : arrays) {
      System.arraycopy(array, 0, result, pos, array.length);
      pos += array.length;
    }
    return result;
  }

  /**
   * Returns a big-endian representation of {@code value} in a 2-element byte array; equivalent to
   * {@code ByteBuffer.allocate(2).putShort(value).array()}. For example, the input value {@code
   * (short) 0x1234} would yield the byte array {@code {0x12, 0x34}}.
   *
   * <p>If you need to convert and concatenate several values (possibly even of different types),
   * use a shared {@link java.nio.ByteBuffer} instance, or use {@link
   * com.google.common.io.ByteStreams#newDataOutput()} to get a growable buffer.
   */
  @GwtIncompatible // doesn't work
  public static byte[] toByteArray(short value) {
    return new byte[] {(byte) (value >> 8), (byte) value};
  }

  /**
   * Returns the {@code short} value whose big-endian representation is stored in the first 2 bytes
   * of {@code bytes}; equivalent to {@code ByteBuffer.wrap(bytes).getShort()}. For example, the
   * input byte array {@code {0x54, 0x32}} would yield the {@code short} value {@code 0x5432}.
   *
   * <p>Arguably, it's preferable to use {@link java.nio.ByteBuffer}; that library exposes much more
   * flexibility at little cost in readability.
   *
   * @throws IllegalArgumentException if {@code bytes} has fewer than 2 elements
   */
  @GwtIncompatible // doesn't work
  public static short fromByteArray(byte[] bytes) {
    checkArgument(bytes.length >= BYTES, "array too small: %s < %s", bytes.length, BYTES);
    return fromBytes(bytes[0], bytes[1]);
  }

  /**
   * Returns the {@code short} value whose byte representation is the given 2 bytes, in big-endian
   * order; equivalent to {@code Shorts.fromByteArray(new byte[] {b1, b2})}.
   *
   * @since 7.0
   */
  @GwtIncompatible // doesn't work
  public static short fromBytes(byte b1, byte b2) {
    return (short) ((b1 << 8) | (b2 & 0xFF));
  }

  private static final class ShortConverter extends Converter<String, Short>
      implements Serializable {
    static final ShortConverter INSTANCE = new ShortConverter();

    @Override
    protected Short doForward(String value) {
      return Short.decode(value);
    }

    @Override
    protected String doBackward(Short value) {
      return value.toString();
    }

    @Override
    public String toString() {
      return "Shorts.stringConverter()";
    }

    private Object readResolve() {
      return INSTANCE;
    }

    private static final long serialVersionUID = 1;
  }

  /**
   * Returns a serializable converter object that converts between strings and shorts using {@link
   * Short#decode} and {@link Short#toString()}. The returned converter throws {@link
   * NumberFormatException} if the input string is invalid.
   *
   * <p><b>Warning:</b> please see {@link Short#decode} to understand exactly how strings are
   * parsed. For example, the string {@code "0123"} is treated as <i>octal</i> and converted to the
   * value {@code 83}.
   *
   * @since 16.0
   */
  @Beta
  public static Converter<String, Short> stringConverter() {
    return ShortConverter.INSTANCE;
  }

  /**
   * Returns an array containing the same values as {@code array}, but guaranteed to be of a
   * specified minimum length. If {@code array} already has a length of at least {@code minLength},
   * it is returned directly. Otherwise, a new array of size {@code minLength + padding} is
   * returned, containing the values of {@code array}, and zeroes in the remaining places.
   *
   * @param array the source array
   * @param minLength the minimum length the returned array must guarantee
   * @param padding an extra amount to "grow" the array by if growth is necessary
   * @throws IllegalArgumentException if {@code minLength} or {@code padding} is negative
   * @return an array containing the values of {@code array}, with guaranteed minimum length {@code
   *     minLength}
   */
  public static short[] ensureCapacity(short[] array, int minLength, int padding) {
    checkArgument(minLength >= 0, "Invalid minLength: %s", minLength);
    checkArgument(padding >= 0, "Invalid padding: %s", padding);
    return (array.length < minLength) ? Arrays.copyOf(array, minLength + padding) : array;
  }

  /**
   * Returns a string containing the supplied {@code short} values separated by {@code separator}.
   * For example, {@code join("-", (short) 1, (short) 2, (short) 3)} returns the string {@code
   * "1-2-3"}.
   *
   * @param separator the text that should appear between consecutive values in the resulting string
   *     (but not at the start or end)
   * @param array an array of {@code short} values, possibly empty
   */
  public static String join(String separator, short... array) {
    checkNotNull(separator);
    if (array.length == 0) {
      return "";
    }

    // For pre-sizing a builder, just get the right order of magnitude
    StringBuilder builder = new StringBuilder(array.length * 6);
    builder.append(array[0]);
    for (int i = 1; i < array.length; i++) {
      builder.append(separator).append(array[i]);
    }
    return builder.toString();
  }

  /**
   * Returns a comparator that compares two {@code short} arrays <a
   * href="http://en.wikipedia.org/wiki/Lexicographical_order">lexicographically</a>. That is, it
   * compares, using {@link #compare(short, short)}), the first pair of values that follow any
   * common prefix, or when one array is a prefix of the other, treats the shorter array as the
   * lesser. For example, {@code [] < [(short) 1] < [(short) 1, (short) 2] < [(short) 2]}.
   *
   * <p>The returned comparator is inconsistent with {@link Object#equals(Object)} (since arrays
   * support only identity equality), but it is consistent with {@link Arrays#equals(short[],
   * short[])}.
   *
   * @since 2.0
   */
  public static Comparator<short[]> lexicographicalComparator() {
    return LexicographicalComparator.INSTANCE;
  }

  private enum LexicographicalComparator implements Comparator<short[]> {
    INSTANCE;

    @Override
    public int compare(short[] left, short[] right) {
      int minLength = Math.min(left.length, right.length);
      for (int i = 0; i < minLength; i++) {
        int result = Shorts.compare(left[i], right[i]);
        if (result != 0) {
          return result;
        }
      }
      return left.length - right.length;
    }

    @Override
    public String toString() {
      return "Shorts.lexicographicalComparator()";
    }
  }

  /**
   * Sorts the elements of {@code array} in descending order.
   *
   * @since 23.1
   */
  public static void sortDescending(short[] array) {
    checkNotNull(array);
    sortDescending(array, 0, array.length);
  }

  /**
   * Sorts the elements of {@code array} between {@code fromIndex} inclusive and {@code toIndex}
   * exclusive in descending order.
   *
   * @since 23.1
   */
  public static void sortDescending(short[] array, int fromIndex, int toIndex) {
    checkNotNull(array);
    checkPositionIndexes(fromIndex, toIndex, array.length);
    Arrays.sort(array, fromIndex, toIndex);
    reverse(array, fromIndex, toIndex);
  }

  /**
   * Reverses the elements of {@code array}. This is equivalent to {@code
   * Collections.reverse(Shorts.asList(array))}, but is likely to be more efficient.
   *
   * @since 23.1
   */
  public static void reverse(short[] array) {
    checkNotNull(array);
    reverse(array, 0, array.length);
  }

  /**
   * Reverses the elements of {@code array} between {@code fromIndex} inclusive and {@code toIndex}
   * exclusive. This is equivalent to {@code
   * Collections.reverse(Shorts.asList(array).subList(fromIndex, toIndex))}, but is likely to be
   * more efficient.
   *
   * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > array.length}, or
   *     {@code toIndex > fromIndex}
   * @since 23.1
   */
  public static void reverse(short[] array, int fromIndex, int toIndex) {
    checkNotNull(array);
    checkPositionIndexes(fromIndex, toIndex, array.length);
    for (int i = fromIndex, j = toIndex - 1; i < j; i++, j--) {
      short tmp = array[i];
      array[i] = array[j];
      array[j] = tmp;
    }
  }

  /**
   * Returns an array containing each value of {@code collection}, converted to a {@code short}
   * value in the manner of {@link Number#shortValue}.
   *
   * <p>Elements are copied from the argument collection as if by {@code collection.toArray()}.
   * Calling this method is as thread-safe as calling that method.
   *
   * @param collection a collection of {@code Number} instances
   * @return an array containing the same values as {@code collection}, in the same order, converted
   *     to primitives
   * @throws NullPointerException if {@code collection} or any of its elements is null
   * @since 1.0 (parameter was {@code Collection<Short>} before 12.0)
   */
  public static short[] toArray(Collection<? extends Number> collection) {
    if (collection instanceof ShortArrayAsList) {
      return ((ShortArrayAsList) collection).toShortArray();
    }

    Object[] boxedArray = collection.toArray();
    int len = boxedArray.length;
    short[] array = new short[len];
    for (int i = 0; i < len; i++) {
      // checkNotNull for GWT (do not optimize)
      array[i] = ((Number) checkNotNull(boxedArray[i])).shortValue();
    }
    return array;
  }

  /**
   * Returns a fixed-size list backed by the specified array, similar to {@link
   * Arrays#asList(Object[])}. The list supports {@link List#set(int, Object)}, but any attempt to
   * set a value to {@code null} will result in a {@link NullPointerException}.
   *
   * <p>The returned list maintains the values, but not the identities, of {@code Short} objects
   * written to or read from it. For example, whether {@code list.get(0) == list.get(0)} is true for
   * the returned list is unspecified.
   *
   * @param backingArray the array to back the list
   * @return a list view of the array
   */
  public static List<Short> asList(short... backingArray) {
    if (backingArray.length == 0) {
      return Collections.emptyList();
    }
    return new ShortArrayAsList(backingArray);
  }

  @GwtCompatible
  private static class ShortArrayAsList extends AbstractList<Short>
      implements RandomAccess, Serializable {
    final short[] array;
    final int start;
    final int end;

    ShortArrayAsList(short[] array) {
      this(array, 0, array.length);
    }

    ShortArrayAsList(short[] array, int start, int end) {
      this.array = array;
      this.start = start;
      this.end = end;
    }

    @Override
    public int size() {
      return end - start;
    }

    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    public Short get(int index) {
      checkElementIndex(index, size());
      return array[start + index];
    }

    @Override
    public boolean contains(@Nullable Object target) {
      // Overridden to prevent a ton of boxing
      return (target instanceof Short) && Shorts.indexOf(array, (Short) target, start, end) != -1;
    }

    @Override
    public int indexOf(@Nullable Object target) {
      // Overridden to prevent a ton of boxing
      if (target instanceof Short) {
        int i = Shorts.indexOf(array, (Short) target, start, end);
        if (i >= 0) {
          return i - start;
        }
      }
      return -1;
    }

    @Override
    public int lastIndexOf(@Nullable Object target) {
      // Overridden to prevent a ton of boxing
      if (target instanceof Short) {
        int i = Shorts.lastIndexOf(array, (Short) target, start, end);
        if (i >= 0) {
          return i - start;
        }
      }
      return -1;
    }

    @Override
    public Short set(int index, Short element) {
      checkElementIndex(index, size());
      short oldValue = array[start + index];
      // checkNotNull for GWT (do not optimize)
      array[start + index] = checkNotNull(element);
      return oldValue;
    }

    @Override
    public List<Short> subList(int fromIndex, int toIndex) {
      int size = size();
      checkPositionIndexes(fromIndex, toIndex, size);
      if (fromIndex == toIndex) {
        return Collections.emptyList();
      }
      return new ShortArrayAsList(array, start + fromIndex, start + toIndex);
    }

    @Override
    public boolean equals(@Nullable Object object) {
      if (object == this) {
        return true;
      }
      if (object instanceof ShortArrayAsList) {
        ShortArrayAsList that = (ShortArrayAsList) object;
        int size = size();
        if (that.size() != size) {
          return false;
        }
        for (int i = 0; i < size; i++) {
          if (array[start + i] != that.array[that.start + i]) {
            return false;
          }
        }
        return true;
      }
      return super.equals(object);
    }

    @Override
    public int hashCode() {
      int result = 1;
      for (int i = start; i < end; i++) {
        result = 31 * result + Shorts.hashCode(array[i]);
      }
      return result;
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder(size() * 6);
      builder.append('[').append(array[start]);
      for (int i = start + 1; i < end; i++) {
        builder.append(", ").append(array[i]);
      }
      return builder.append(']').toString();
    }

    short[] toShortArray() {
      return Arrays.copyOfRange(array, start, end);
    }

    private static final long serialVersionUID = 0;
  }
}

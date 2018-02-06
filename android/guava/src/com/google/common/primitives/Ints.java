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
import com.google.common.base.Converter;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.RandomAccess;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * Static utility methods pertaining to {@code int} primitives, that are not already found in either
 * {@link Integer} or {@link Arrays}.
 *
 * <p>See the Guava User Guide article on <a
 * href="https://github.com/google/guava/wiki/PrimitivesExplained">primitive utilities</a>.
 *
 * @author Kevin Bourrillion
 * @since 1.0
 */
@GwtCompatible
public final class Ints {
  private Ints() {}

  /**
   * The number of bytes required to represent a primitive {@code int} value.
   *
   * <p><b>Java 8 users:</b> use {@link Integer#BYTES} instead.
   */
  public static final int BYTES = Integer.SIZE / Byte.SIZE;

  /**
   * The largest power of two that can be represented as an {@code int}.
   *
   * @since 10.0
   */
  public static final int MAX_POWER_OF_TWO = 1 << (Integer.SIZE - 2);

  /**
   * Returns a hash code for {@code value}; equal to the result of invoking {@code ((Integer)
   * value).hashCode()}.
   *
   * <p><b>Java 8 users:</b> use {@link Integer#hashCode(int)} instead.
   *
   * @param value a primitive {@code int} value
   * @return a hash code for the value
   */
  public static int hashCode(int value) {
    return value;
  }

  /**
   * Returns the {@code int} value that is equal to {@code value}, if possible.
   *
   * @param value any value in the range of the {@code int} type
   * @return the {@code int} value that equals {@code value}
   * @throws IllegalArgumentException if {@code value} is greater than {@link Integer#MAX_VALUE} or
   *     less than {@link Integer#MIN_VALUE}
   */
  public static int checkedCast(long value) {
    int result = (int) value;
    checkArgument(result == value, "Out of range: %s", value);
    return result;
  }

  /**
   * Returns the {@code int} nearest in value to {@code value}.
   *
   * @param value any {@code long} value
   * @return the same value cast to {@code int} if it is in the range of the {@code int} type,
   *     {@link Integer#MAX_VALUE} if it is too large, or {@link Integer#MIN_VALUE} if it is too
   *     small
   */
  public static int saturatedCast(long value) {
    if (value > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    if (value < Integer.MIN_VALUE) {
      return Integer.MIN_VALUE;
    }
    return (int) value;
  }

  /**
   * Compares the two specified {@code int} values. The sign of the value returned is the same as
   * that of {@code ((Integer) a).compareTo(b)}.
   *
   * <p><b>Note for Java 7 and later:</b> this method should be treated as deprecated; use the
   * equivalent {@link Integer#compare} method instead.
   *
   * @param a the first {@code int} to compare
   * @param b the second {@code int} to compare
   * @return a negative value if {@code a} is less than {@code b}; a positive value if {@code a} is
   *     greater than {@code b}; or zero if they are equal
   */
  public static int compare(int a, int b) {
    return (a < b) ? -1 : ((a > b) ? 1 : 0);
  }

  /**
   * Returns {@code true} if {@code target} is present as an element anywhere in {@code array}.
   *
   * @param array an array of {@code int} values, possibly empty
   * @param target a primitive {@code int} value
   * @return {@code true} if {@code array[i] == target} for some value of {@code i}
   */
  public static boolean contains(int[] array, int target) {
    for (int value : array) {
      if (value == target) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the index of the first appearance of the value {@code target} in {@code array}.
   *
   * @param array an array of {@code int} values, possibly empty
   * @param target a primitive {@code int} value
   * @return the least index {@code i} for which {@code array[i] == target}, or {@code -1} if no
   *     such index exists.
   */
  public static int indexOf(int[] array, int target) {
    return indexOf(array, target, 0, array.length);
  }

  // TODO(kevinb): consider making this public
  private static int indexOf(int[] array, int target, int start, int end) {
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
  public static int indexOf(int[] array, int[] target) {
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
   * @param array an array of {@code int} values, possibly empty
   * @param target a primitive {@code int} value
   * @return the greatest index {@code i} for which {@code array[i] == target}, or {@code -1} if no
   *     such index exists.
   */
  public static int lastIndexOf(int[] array, int target) {
    return lastIndexOf(array, target, 0, array.length);
  }

  // TODO(kevinb): consider making this public
  private static int lastIndexOf(int[] array, int target, int start, int end) {
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
   * @param array a <i>nonempty</i> array of {@code int} values
   * @return the value present in {@code array} that is less than or equal to every other value in
   *     the array
   * @throws IllegalArgumentException if {@code array} is empty
   */
  public static int min(int... array) {
    checkArgument(array.length > 0);
    int min = array[0];
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
   * @param array a <i>nonempty</i> array of {@code int} values
   * @return the value present in {@code array} that is greater than or equal to every other value
   *     in the array
   * @throws IllegalArgumentException if {@code array} is empty
   */
  public static int max(int... array) {
    checkArgument(array.length > 0);
    int max = array[0];
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
   * @param value the {@code int} value to constrain
   * @param min the lower bound (inclusive) of the range to constrain {@code value} to
   * @param max the upper bound (inclusive) of the range to constrain {@code value} to
   * @throws IllegalArgumentException if {@code min > max}
   * @since 21.0
   */
  @Beta
  public static int constrainToRange(int value, int min, int max) {
    checkArgument(min <= max, "min (%s) must be less than or equal to max (%s)", min, max);
    return Math.min(Math.max(value, min), max);
  }

  /**
   * Returns the values from each provided array combined into a single array. For example, {@code
   * concat(new int[] {a, b}, new int[] {}, new int[] {c}} returns the array {@code {a, b, c}}.
   *
   * @param arrays zero or more {@code int} arrays
   * @return a single array containing all the values from the source arrays, in order
   */
  public static int[] concat(int[]... arrays) {
    int length = 0;
    for (int[] array : arrays) {
      length += array.length;
    }
    int[] result = new int[length];
    int pos = 0;
    for (int[] array : arrays) {
      System.arraycopy(array, 0, result, pos, array.length);
      pos += array.length;
    }
    return result;
  }

  /**
   * Returns a big-endian representation of {@code value} in a 4-element byte array; equivalent to
   * {@code ByteBuffer.allocate(4).putInt(value).array()}. For example, the input value {@code
   * 0x12131415} would yield the byte array {@code {0x12, 0x13, 0x14, 0x15}}.
   *
   * <p>If you need to convert and concatenate several values (possibly even of different types),
   * use a shared {@link java.nio.ByteBuffer} instance, or use {@link
   * com.google.common.io.ByteStreams#newDataOutput()} to get a growable buffer.
   */
  public static byte[] toByteArray(int value) {
    return new byte[] {
      (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value
    };
  }

  /**
   * Returns the {@code int} value whose big-endian representation is stored in the first 4 bytes of
   * {@code bytes}; equivalent to {@code ByteBuffer.wrap(bytes).getInt()}. For example, the input
   * byte array {@code {0x12, 0x13, 0x14, 0x15, 0x33}} would yield the {@code int} value {@code
   * 0x12131415}.
   *
   * <p>Arguably, it's preferable to use {@link java.nio.ByteBuffer}; that library exposes much more
   * flexibility at little cost in readability.
   *
   * @throws IllegalArgumentException if {@code bytes} has fewer than 4 elements
   */
  public static int fromByteArray(byte[] bytes) {
    checkArgument(bytes.length >= BYTES, "array too small: %s < %s", bytes.length, BYTES);
    return fromBytes(bytes[0], bytes[1], bytes[2], bytes[3]);
  }

  /**
   * Returns the {@code int} value whose byte representation is the given 4 bytes, in big-endian
   * order; equivalent to {@code Ints.fromByteArray(new byte[] {b1, b2, b3, b4})}.
   *
   * @since 7.0
   */
  public static int fromBytes(byte b1, byte b2, byte b3, byte b4) {
    return b1 << 24 | (b2 & 0xFF) << 16 | (b3 & 0xFF) << 8 | (b4 & 0xFF);
  }

  private static final class IntConverter extends Converter<String, Integer>
      implements Serializable {
    static final IntConverter INSTANCE = new IntConverter();

    @Override
    protected Integer doForward(String value) {
      return Integer.decode(value);
    }

    @Override
    protected String doBackward(Integer value) {
      return value.toString();
    }

    @Override
    public String toString() {
      return "Ints.stringConverter()";
    }

    private Object readResolve() {
      return INSTANCE;
    }

    private static final long serialVersionUID = 1;
  }

  /**
   * Returns a serializable converter object that converts between strings and integers using {@link
   * Integer#decode} and {@link Integer#toString()}. The returned converter throws {@link
   * NumberFormatException} if the input string is invalid.
   *
   * <p><b>Warning:</b> please see {@link Integer#decode} to understand exactly how strings are
   * parsed. For example, the string {@code "0123"} is treated as <i>octal</i> and converted to the
   * value {@code 83}.
   *
   * @since 16.0
   */
  @Beta
  public static Converter<String, Integer> stringConverter() {
    return IntConverter.INSTANCE;
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
  public static int[] ensureCapacity(int[] array, int minLength, int padding) {
    checkArgument(minLength >= 0, "Invalid minLength: %s", minLength);
    checkArgument(padding >= 0, "Invalid padding: %s", padding);
    return (array.length < minLength) ? Arrays.copyOf(array, minLength + padding) : array;
  }

  /**
   * Returns a string containing the supplied {@code int} values separated by {@code separator}. For
   * example, {@code join("-", 1, 2, 3)} returns the string {@code "1-2-3"}.
   *
   * @param separator the text that should appear between consecutive values in the resulting string
   *     (but not at the start or end)
   * @param array an array of {@code int} values, possibly empty
   */
  public static String join(String separator, int... array) {
    checkNotNull(separator);
    if (array.length == 0) {
      return "";
    }

    // For pre-sizing a builder, just get the right order of magnitude
    StringBuilder builder = new StringBuilder(array.length * 5);
    builder.append(array[0]);
    for (int i = 1; i < array.length; i++) {
      builder.append(separator).append(array[i]);
    }
    return builder.toString();
  }

  /**
   * Returns a comparator that compares two {@code int} arrays <a
   * href="http://en.wikipedia.org/wiki/Lexicographical_order">lexicographically</a>. That is, it
   * compares, using {@link #compare(int, int)}), the first pair of values that follow any common
   * prefix, or when one array is a prefix of the other, treats the shorter array as the lesser. For
   * example, {@code [] < [1] < [1, 2] < [2]}.
   *
   * <p>The returned comparator is inconsistent with {@link Object#equals(Object)} (since arrays
   * support only identity equality), but it is consistent with {@link Arrays#equals(int[], int[])}.
   *
   * @since 2.0
   */
  public static Comparator<int[]> lexicographicalComparator() {
    return LexicographicalComparator.INSTANCE;
  }

  private enum LexicographicalComparator implements Comparator<int[]> {
    INSTANCE;

    @Override
    public int compare(int[] left, int[] right) {
      int minLength = Math.min(left.length, right.length);
      for (int i = 0; i < minLength; i++) {
        int result = Ints.compare(left[i], right[i]);
        if (result != 0) {
          return result;
        }
      }
      return left.length - right.length;
    }

    @Override
    public String toString() {
      return "Ints.lexicographicalComparator()";
    }
  }

  /**
   * Sorts the elements of {@code array} in descending order.
   *
   * @since 23.1
   */
  public static void sortDescending(int[] array) {
    checkNotNull(array);
    sortDescending(array, 0, array.length);
  }

  /**
   * Sorts the elements of {@code array} between {@code fromIndex} inclusive and {@code toIndex}
   * exclusive in descending order.
   *
   * @since 23.1
   */
  public static void sortDescending(int[] array, int fromIndex, int toIndex) {
    checkNotNull(array);
    checkPositionIndexes(fromIndex, toIndex, array.length);
    Arrays.sort(array, fromIndex, toIndex);
    reverse(array, fromIndex, toIndex);
  }

  /**
   * Reverses the elements of {@code array}. This is equivalent to {@code
   * Collections.reverse(Ints.asList(array))}, but is likely to be more efficient.
   *
   * @since 23.1
   */
  public static void reverse(int[] array) {
    checkNotNull(array);
    reverse(array, 0, array.length);
  }

  /**
   * Reverses the elements of {@code array} between {@code fromIndex} inclusive and {@code toIndex}
   * exclusive. This is equivalent to {@code
   * Collections.reverse(Ints.asList(array).subList(fromIndex, toIndex))}, but is likely to be more
   * efficient.
   *
   * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > array.length}, or
   *     {@code toIndex > fromIndex}
   * @since 23.1
   */
  public static void reverse(int[] array, int fromIndex, int toIndex) {
    checkNotNull(array);
    checkPositionIndexes(fromIndex, toIndex, array.length);
    for (int i = fromIndex, j = toIndex - 1; i < j; i++, j--) {
      int tmp = array[i];
      array[i] = array[j];
      array[j] = tmp;
    }
  }

  /**
   * Returns an array containing each value of {@code collection}, converted to a {@code int} value
   * in the manner of {@link Number#intValue}.
   *
   * <p>Elements are copied from the argument collection as if by {@code collection.toArray()}.
   * Calling this method is as thread-safe as calling that method.
   *
   * @param collection a collection of {@code Number} instances
   * @return an array containing the same values as {@code collection}, in the same order, converted
   *     to primitives
   * @throws NullPointerException if {@code collection} or any of its elements is null
   * @since 1.0 (parameter was {@code Collection<Integer>} before 12.0)
   */
  public static int[] toArray(Collection<? extends Number> collection) {
    if (collection instanceof IntArrayAsList) {
      return ((IntArrayAsList) collection).toIntArray();
    }

    Object[] boxedArray = collection.toArray();
    int len = boxedArray.length;
    int[] array = new int[len];
    for (int i = 0; i < len; i++) {
      // checkNotNull for GWT (do not optimize)
      array[i] = ((Number) checkNotNull(boxedArray[i])).intValue();
    }
    return array;
  }

  /**
   * Returns a fixed-size list backed by the specified array, similar to {@link
   * Arrays#asList(Object[])}. The list supports {@link List#set(int, Object)}, but any attempt to
   * set a value to {@code null} will result in a {@link NullPointerException}.
   *
   * <p>The returned list maintains the values, but not the identities, of {@code Integer} objects
   * written to or read from it. For example, whether {@code list.get(0) == list.get(0)} is true for
   * the returned list is unspecified.
   *
   * <p><b>Note:</b> when possible, you should represent your data as an {@link ImmutableIntArray}
   * instead, which has an {@link ImmutableIntArray#asList asList} view.
   *
   * @param backingArray the array to back the list
   * @return a list view of the array
   */
  public static List<Integer> asList(int... backingArray) {
    if (backingArray.length == 0) {
      return Collections.emptyList();
    }
    return new IntArrayAsList(backingArray);
  }

  @GwtCompatible
  private static class IntArrayAsList extends AbstractList<Integer>
      implements RandomAccess, Serializable {
    final int[] array;
    final int start;
    final int end;

    IntArrayAsList(int[] array) {
      this(array, 0, array.length);
    }

    IntArrayAsList(int[] array, int start, int end) {
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
    public Integer get(int index) {
      checkElementIndex(index, size());
      return array[start + index];
    }

    @Override
    public boolean contains(Object target) {
      // Overridden to prevent a ton of boxing
      return (target instanceof Integer) && Ints.indexOf(array, (Integer) target, start, end) != -1;
    }

    @Override
    public int indexOf(Object target) {
      // Overridden to prevent a ton of boxing
      if (target instanceof Integer) {
        int i = Ints.indexOf(array, (Integer) target, start, end);
        if (i >= 0) {
          return i - start;
        }
      }
      return -1;
    }

    @Override
    public int lastIndexOf(Object target) {
      // Overridden to prevent a ton of boxing
      if (target instanceof Integer) {
        int i = Ints.lastIndexOf(array, (Integer) target, start, end);
        if (i >= 0) {
          return i - start;
        }
      }
      return -1;
    }

    @Override
    public Integer set(int index, Integer element) {
      checkElementIndex(index, size());
      int oldValue = array[start + index];
      // checkNotNull for GWT (do not optimize)
      array[start + index] = checkNotNull(element);
      return oldValue;
    }

    @Override
    public List<Integer> subList(int fromIndex, int toIndex) {
      int size = size();
      checkPositionIndexes(fromIndex, toIndex, size);
      if (fromIndex == toIndex) {
        return Collections.emptyList();
      }
      return new IntArrayAsList(array, start + fromIndex, start + toIndex);
    }

    @Override
    public boolean equals(@NullableDecl Object object) {
      if (object == this) {
        return true;
      }
      if (object instanceof IntArrayAsList) {
        IntArrayAsList that = (IntArrayAsList) object;
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
        result = 31 * result + Ints.hashCode(array[i]);
      }
      return result;
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder(size() * 5);
      builder.append('[').append(array[start]);
      for (int i = start + 1; i < end; i++) {
        builder.append(", ").append(array[i]);
      }
      return builder.append(']').toString();
    }

    int[] toIntArray() {
      return Arrays.copyOfRange(array, start, end);
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Parses the specified string as a signed decimal integer value. The ASCII character {@code '-'}
   * (<code>'&#92;u002D'</code>) is recognized as the minus sign.
   *
   * <p>Unlike {@link Integer#parseInt(String)}, this method returns {@code null} instead of
   * throwing an exception if parsing fails. Additionally, this method only accepts ASCII digits,
   * and returns {@code null} if non-ASCII digits are present in the string.
   *
   * <p>Note that strings prefixed with ASCII {@code '+'} are rejected, even under JDK 7, despite
   * the change to {@link Integer#parseInt(String)} for that version.
   *
   * @param string the string representation of an integer value
   * @return the integer value represented by {@code string}, or {@code null} if {@code string} has
   *     a length of zero or cannot be parsed as an integer value
   * @since 11.0
   */
  @Beta
  @NullableDecl
  public static Integer tryParse(String string) {
    return tryParse(string, 10);
  }

  /**
   * Parses the specified string as a signed integer value using the specified radix. The ASCII
   * character {@code '-'} (<code>'&#92;u002D'</code>) is recognized as the minus sign.
   *
   * <p>Unlike {@link Integer#parseInt(String, int)}, this method returns {@code null} instead of
   * throwing an exception if parsing fails. Additionally, this method only accepts ASCII digits,
   * and returns {@code null} if non-ASCII digits are present in the string.
   *
   * <p>Note that strings prefixed with ASCII {@code '+'} are rejected, even under JDK 7, despite
   * the change to {@link Integer#parseInt(String, int)} for that version.
   *
   * @param string the string representation of an integer value
   * @param radix the radix to use when parsing
   * @return the integer value represented by {@code string} using {@code radix}, or {@code null} if
   *     {@code string} has a length of zero or cannot be parsed as an integer value
   * @throws IllegalArgumentException if {@code radix < Character.MIN_RADIX} or {@code radix >
   *     Character.MAX_RADIX}
   * @since 19.0
   */
  @Beta
  @NullableDecl
  public static Integer tryParse(String string, int radix) {
    Long result = Longs.tryParse(string, radix);
    if (result == null || result.longValue() != result.intValue()) {
      return null;
    } else {
      return result.intValue();
    }
  }
}

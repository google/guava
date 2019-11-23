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
import static java.lang.Float.NEGATIVE_INFINITY;
import static java.lang.Float.POSITIVE_INFINITY;

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
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * Static utility methods pertaining to {@code float} primitives, that are not already found in
 * either {@link Float} or {@link Arrays}.
 *
 * <p>See the Guava User Guide article on <a
 * href="https://github.com/google/guava/wiki/PrimitivesExplained">primitive utilities</a>.
 *
 * @author Kevin Bourrillion
 * @since 1.0
 */
@GwtCompatible(emulated = true)
public final class Floats {
  private Floats() {}

  /**
   * The number of bytes required to represent a primitive {@code float} value.
   *
   * <p><b>Java 8 users:</b> use {@link Float#BYTES} instead.
   *
   * @since 10.0
   */
  public static final int BYTES = Float.SIZE / Byte.SIZE;

  /**
   * Returns a hash code for {@code value}; equal to the result of invoking {@code ((Float)
   * value).hashCode()}.
   *
   * <p><b>Java 8 users:</b> use {@link Float#hashCode(float)} instead.
   *
   * @param value a primitive {@code float} value
   * @return a hash code for the value
   */
  public static int hashCode(float value) {
    // TODO(kevinb): is there a better way, that's still gwt-safe?
    return ((Float) value).hashCode();
  }

  /**
   * Compares the two specified {@code float} values using {@link Float#compare(float, float)}. You
   * may prefer to invoke that method directly; this method exists only for consistency with the
   * other utilities in this package.
   *
   * <p><b>Note:</b> this method simply delegates to the JDK method {@link Float#compare}. It is
   * provided for consistency with the other primitive types, whose compare methods were not added
   * to the JDK until JDK 7.
   *
   * @param a the first {@code float} to compare
   * @param b the second {@code float} to compare
   * @return the result of invoking {@link Float#compare(float, float)}
   */
  public static int compare(float a, float b) {
    return Float.compare(a, b);
  }

  /**
   * Returns {@code true} if {@code value} represents a real number. This is equivalent to, but not
   * necessarily implemented as, {@code !(Float.isInfinite(value) || Float.isNaN(value))}.
   *
   * <p><b>Java 8 users:</b> use {@link Float#isFinite(float)} instead.
   *
   * @since 10.0
   */
  public static boolean isFinite(float value) {
    return NEGATIVE_INFINITY < value && value < POSITIVE_INFINITY;
  }

  /**
   * Returns {@code true} if {@code target} is present as an element anywhere in {@code array}. Note
   * that this always returns {@code false} when {@code target} is {@code NaN}.
   *
   * @param array an array of {@code float} values, possibly empty
   * @param target a primitive {@code float} value
   * @return {@code true} if {@code array[i] == target} for some value of {@code i}
   */
  public static boolean contains(float[] array, float target) {
    for (float value : array) {
      if (value == target) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the index of the first appearance of the value {@code target} in {@code array}. Note
   * that this always returns {@code -1} when {@code target} is {@code NaN}.
   *
   * @param array an array of {@code float} values, possibly empty
   * @param target a primitive {@code float} value
   * @return the least index {@code i} for which {@code array[i] == target}, or {@code -1} if no
   *     such index exists.
   */
  public static int indexOf(float[] array, float target) {
    return indexOf(array, target, 0, array.length);
  }

  // TODO(kevinb): consider making this public
  private static int indexOf(float[] array, float target, int start, int end) {
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
   * <p>Note that this always returns {@code -1} when {@code target} contains {@code NaN}.
   *
   * @param array the array to search for the sequence {@code target}
   * @param target the array to search for as a sub-sequence of {@code array}
   */
  public static int indexOf(float[] array, float[] target) {
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
   * Returns the index of the last appearance of the value {@code target} in {@code array}. Note
   * that this always returns {@code -1} when {@code target} is {@code NaN}.
   *
   * @param array an array of {@code float} values, possibly empty
   * @param target a primitive {@code float} value
   * @return the greatest index {@code i} for which {@code array[i] == target}, or {@code -1} if no
   *     such index exists.
   */
  public static int lastIndexOf(float[] array, float target) {
    return lastIndexOf(array, target, 0, array.length);
  }

  // TODO(kevinb): consider making this public
  private static int lastIndexOf(float[] array, float target, int start, int end) {
    for (int i = end - 1; i >= start; i--) {
      if (array[i] == target) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Returns the least value present in {@code array}, using the same rules of comparison as {@link
   * Math#min(float, float)}.
   *
   * @param array a <i>nonempty</i> array of {@code float} values
   * @return the value present in {@code array} that is less than or equal to every other value in
   *     the array
   * @throws IllegalArgumentException if {@code array} is empty
   */
  public static float min(float... array) {
    checkArgument(array.length > 0);
    float min = array[0];
    for (int i = 1; i < array.length; i++) {
      min = Math.min(min, array[i]);
    }
    return min;
  }

  /**
   * Returns the greatest value present in {@code array}, using the same rules of comparison as
   * {@link Math#max(float, float)}.
   *
   * @param array a <i>nonempty</i> array of {@code float} values
   * @return the value present in {@code array} that is greater than or equal to every other value
   *     in the array
   * @throws IllegalArgumentException if {@code array} is empty
   */
  public static float max(float... array) {
    checkArgument(array.length > 0);
    float max = array[0];
    for (int i = 1; i < array.length; i++) {
      max = Math.max(max, array[i]);
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
   * @param value the {@code float} value to constrain
   * @param min the lower bound (inclusive) of the range to constrain {@code value} to
   * @param max the upper bound (inclusive) of the range to constrain {@code value} to
   * @throws IllegalArgumentException if {@code min > max}
   * @since 21.0
   */
  @Beta
  public static float constrainToRange(float value, float min, float max) {
    checkArgument(min <= max, "min (%s) must be less than or equal to max (%s)", min, max);
    return Math.min(Math.max(value, min), max);
  }

  /**
   * Returns the values from each provided array combined into a single array. For example, {@code
   * concat(new float[] {a, b}, new float[] {}, new float[] {c}} returns the array {@code {a, b,
   * c}}.
   *
   * @param arrays zero or more {@code float} arrays
   * @return a single array containing all the values from the source arrays, in order
   */
  public static float[] concat(float[]... arrays) {
    int length = 0;
    for (float[] array : arrays) {
      length += array.length;
    }
    float[] result = new float[length];
    int pos = 0;
    for (float[] array : arrays) {
      System.arraycopy(array, 0, result, pos, array.length);
      pos += array.length;
    }
    return result;
  }

  private static final class FloatConverter extends Converter<String, Float>
      implements Serializable {
    static final FloatConverter INSTANCE = new FloatConverter();

    @Override
    protected Float doForward(String value) {
      return Float.valueOf(value);
    }

    @Override
    protected String doBackward(Float value) {
      return value.toString();
    }

    @Override
    public String toString() {
      return "Floats.stringConverter()";
    }

    private Object readResolve() {
      return INSTANCE;
    }

    private static final long serialVersionUID = 1;
  }

  /**
   * Returns a serializable converter object that converts between strings and floats using {@link
   * Float#valueOf} and {@link Float#toString()}.
   *
   * @since 16.0
   */
  @Beta
  public static Converter<String, Float> stringConverter() {
    return FloatConverter.INSTANCE;
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
  public static float[] ensureCapacity(float[] array, int minLength, int padding) {
    checkArgument(minLength >= 0, "Invalid minLength: %s", minLength);
    checkArgument(padding >= 0, "Invalid padding: %s", padding);
    return (array.length < minLength) ? Arrays.copyOf(array, minLength + padding) : array;
  }

  /**
   * Returns a string containing the supplied {@code float} values, converted to strings as
   * specified by {@link Float#toString(float)}, and separated by {@code separator}. For example,
   * {@code join("-", 1.0f, 2.0f, 3.0f)} returns the string {@code "1.0-2.0-3.0"}.
   *
   * <p>Note that {@link Float#toString(float)} formats {@code float} differently in GWT. In the
   * previous example, it returns the string {@code "1-2-3"}.
   *
   * @param separator the text that should appear between consecutive values in the resulting string
   *     (but not at the start or end)
   * @param array an array of {@code float} values, possibly empty
   */
  public static String join(String separator, float... array) {
    checkNotNull(separator);
    if (array.length == 0) {
      return "";
    }

    // For pre-sizing a builder, just get the right order of magnitude
    StringBuilder builder = new StringBuilder(array.length * 12);
    builder.append(array[0]);
    for (int i = 1; i < array.length; i++) {
      builder.append(separator).append(array[i]);
    }
    return builder.toString();
  }

  /**
   * Returns a comparator that compares two {@code float} arrays <a
   * href="http://en.wikipedia.org/wiki/Lexicographical_order">lexicographically</a>. That is, it
   * compares, using {@link #compare(float, float)}), the first pair of values that follow any
   * common prefix, or when one array is a prefix of the other, treats the shorter array as the
   * lesser. For example, {@code [] < [1.0f] < [1.0f, 2.0f] < [2.0f]}.
   *
   * <p>The returned comparator is inconsistent with {@link Object#equals(Object)} (since arrays
   * support only identity equality), but it is consistent with {@link Arrays#equals(float[],
   * float[])}.
   *
   * @since 2.0
   */
  public static Comparator<float[]> lexicographicalComparator() {
    return LexicographicalComparator.INSTANCE;
  }

  private enum LexicographicalComparator implements Comparator<float[]> {
    INSTANCE;

    @Override
    public int compare(float[] left, float[] right) {
      int minLength = Math.min(left.length, right.length);
      for (int i = 0; i < minLength; i++) {
        int result = Float.compare(left[i], right[i]);
        if (result != 0) {
          return result;
        }
      }
      return left.length - right.length;
    }

    @Override
    public String toString() {
      return "Floats.lexicographicalComparator()";
    }
  }

  /**
   * Sorts the elements of {@code array} in descending order.
   *
   * <p>Note that this method uses the total order imposed by {@link Float#compare}, which treats
   * all NaN values as equal and 0.0 as greater than -0.0.
   *
   * @since 23.1
   */
  public static void sortDescending(float[] array) {
    checkNotNull(array);
    sortDescending(array, 0, array.length);
  }

  /**
   * Sorts the elements of {@code array} between {@code fromIndex} inclusive and {@code toIndex}
   * exclusive in descending order.
   *
   * <p>Note that this method uses the total order imposed by {@link Float#compare}, which treats
   * all NaN values as equal and 0.0 as greater than -0.0.
   *
   * @since 23.1
   */
  public static void sortDescending(float[] array, int fromIndex, int toIndex) {
    checkNotNull(array);
    checkPositionIndexes(fromIndex, toIndex, array.length);
    Arrays.sort(array, fromIndex, toIndex);
    reverse(array, fromIndex, toIndex);
  }

  /**
   * Reverses the elements of {@code array}. This is equivalent to {@code
   * Collections.reverse(Floats.asList(array))}, but is likely to be more efficient.
   *
   * @since 23.1
   */
  public static void reverse(float[] array) {
    checkNotNull(array);
    reverse(array, 0, array.length);
  }

  /**
   * Reverses the elements of {@code array} between {@code fromIndex} inclusive and {@code toIndex}
   * exclusive. This is equivalent to {@code
   * Collections.reverse(Floats.asList(array).subList(fromIndex, toIndex))}, but is likely to be
   * more efficient.
   *
   * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > array.length}, or
   *     {@code toIndex > fromIndex}
   * @since 23.1
   */
  public static void reverse(float[] array, int fromIndex, int toIndex) {
    checkNotNull(array);
    checkPositionIndexes(fromIndex, toIndex, array.length);
    for (int i = fromIndex, j = toIndex - 1; i < j; i++, j--) {
      float tmp = array[i];
      array[i] = array[j];
      array[j] = tmp;
    }
  }

  /**
   * Returns an array containing each value of {@code collection}, converted to a {@code float}
   * value in the manner of {@link Number#floatValue}.
   *
   * <p>Elements are copied from the argument collection as if by {@code collection.toArray()}.
   * Calling this method is as thread-safe as calling that method.
   *
   * @param collection a collection of {@code Number} instances
   * @return an array containing the same values as {@code collection}, in the same order, converted
   *     to primitives
   * @throws NullPointerException if {@code collection} or any of its elements is null
   * @since 1.0 (parameter was {@code Collection<Float>} before 12.0)
   */
  public static float[] toArray(Collection<? extends Number> collection) {
    if (collection instanceof FloatArrayAsList) {
      return ((FloatArrayAsList) collection).toFloatArray();
    }

    Object[] boxedArray = collection.toArray();
    int len = boxedArray.length;
    float[] array = new float[len];
    for (int i = 0; i < len; i++) {
      // checkNotNull for GWT (do not optimize)
      array[i] = ((Number) checkNotNull(boxedArray[i])).floatValue();
    }
    return array;
  }

  /**
   * Returns a fixed-size list backed by the specified array, similar to {@link
   * Arrays#asList(Object[])}. The list supports {@link List#set(int, Object)}, but any attempt to
   * set a value to {@code null} will result in a {@link NullPointerException}.
   *
   * <p>The returned list maintains the values, but not the identities, of {@code Float} objects
   * written to or read from it. For example, whether {@code list.get(0) == list.get(0)} is true for
   * the returned list is unspecified.
   *
   * <p>The returned list may have unexpected behavior if it contains {@code NaN}, or if {@code NaN}
   * is used as a parameter to any of its methods.
   *
   * @param backingArray the array to back the list
   * @return a list view of the array
   */
  public static List<Float> asList(float... backingArray) {
    if (backingArray.length == 0) {
      return Collections.emptyList();
    }
    return new FloatArrayAsList(backingArray);
  }

  @GwtCompatible
  private static class FloatArrayAsList extends AbstractList<Float>
      implements RandomAccess, Serializable {
    final float[] array;
    final int start;
    final int end;

    FloatArrayAsList(float[] array) {
      this(array, 0, array.length);
    }

    FloatArrayAsList(float[] array, int start, int end) {
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
    public Float get(int index) {
      checkElementIndex(index, size());
      return array[start + index];
    }

    @Override
    public boolean contains(Object target) {
      // Overridden to prevent a ton of boxing
      return (target instanceof Float) && Floats.indexOf(array, (Float) target, start, end) != -1;
    }

    @Override
    public int indexOf(Object target) {
      // Overridden to prevent a ton of boxing
      if (target instanceof Float) {
        int i = Floats.indexOf(array, (Float) target, start, end);
        if (i >= 0) {
          return i - start;
        }
      }
      return -1;
    }

    @Override
    public int lastIndexOf(Object target) {
      // Overridden to prevent a ton of boxing
      if (target instanceof Float) {
        int i = Floats.lastIndexOf(array, (Float) target, start, end);
        if (i >= 0) {
          return i - start;
        }
      }
      return -1;
    }

    @Override
    public Float set(int index, Float element) {
      checkElementIndex(index, size());
      float oldValue = array[start + index];
      // checkNotNull for GWT (do not optimize)
      array[start + index] = checkNotNull(element);
      return oldValue;
    }

    @Override
    public List<Float> subList(int fromIndex, int toIndex) {
      int size = size();
      checkPositionIndexes(fromIndex, toIndex, size);
      if (fromIndex == toIndex) {
        return Collections.emptyList();
      }
      return new FloatArrayAsList(array, start + fromIndex, start + toIndex);
    }

    @Override
    public boolean equals(@NullableDecl Object object) {
      if (object == this) {
        return true;
      }
      if (object instanceof FloatArrayAsList) {
        FloatArrayAsList that = (FloatArrayAsList) object;
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
        result = 31 * result + Floats.hashCode(array[i]);
      }
      return result;
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder(size() * 12);
      builder.append('[').append(array[start]);
      for (int i = start + 1; i < end; i++) {
        builder.append(", ").append(array[i]);
      }
      return builder.append(']').toString();
    }

    float[] toFloatArray() {
      return Arrays.copyOfRange(array, start, end);
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Parses the specified string as a single-precision floating point value. The ASCII character
   * {@code '-'} (<code>'&#92;u002D'</code>) is recognized as the minus sign.
   *
   * <p>Unlike {@link Float#parseFloat(String)}, this method returns {@code null} instead of
   * throwing an exception if parsing fails. Valid inputs are exactly those accepted by {@link
   * Float#valueOf(String)}, except that leading and trailing whitespace is not permitted.
   *
   * <p>This implementation is likely to be faster than {@code Float.parseFloat} if many failures
   * are expected.
   *
   * @param string the string representation of a {@code float} value
   * @return the floating point value represented by {@code string}, or {@code null} if {@code
   *     string} has a length of zero or cannot be parsed as a {@code float} value
   * @throws NullPointerException if {@code string} is {@code null}
   * @since 14.0
   */
  @Beta
  @GwtIncompatible // regular expressions
  @NullableDecl
  public static Float tryParse(String string) {
    if (Doubles.FLOATING_POINT_PATTERN.matcher(string).matches()) {
      // TODO(lowasser): could be potentially optimized, but only with
      // extensive testing
      try {
        return Float.parseFloat(string);
      } catch (NumberFormatException e) {
        // Float.parseFloat has changed specs several times, so fall through
        // gracefully
      }
    }
    return null;
  }
}

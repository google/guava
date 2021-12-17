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
import static com.google.common.base.Strings.lenientFormat;
import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;

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
import java.util.Spliterator;
import java.util.Spliterators;
import javax.annotation.CheckForNull;

/**
 * Static utility methods pertaining to {@code double} primitives, that are not already found in
 * either {@link Double} or {@link Arrays}.
 *
 * <p>See the Guava User Guide article on <a
 * href="https://github.com/google/guava/wiki/PrimitivesExplained">primitive utilities</a>.
 *
 * @author Kevin Bourrillion
 * @since 1.0
 */
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
public final class Doubles extends DoublesMethodsForWeb {
  private Doubles() {}

  /**
   * The number of bytes required to represent a primitive {@code double} value.
   *
   * <p><b>Java 8 users:</b> use {@link Double#BYTES} instead.
   *
   * @since 10.0
   */
  public static final int BYTES = Double.SIZE / Byte.SIZE;

  /**
   * Returns a hash code for {@code value}; equal to the result of invoking {@code ((Double)
   * value).hashCode()}.
   *
   * <p><b>Java 8 users:</b> use {@link Double#hashCode(double)} instead.
   *
   * @param value a primitive {@code double} value
   * @return a hash code for the value
   */
  public static int hashCode(double value) {
    return ((Double) value).hashCode();
    // TODO(kevinb): do it this way when we can (GWT problem):
    // long bits = Double.doubleToLongBits(value);
    // return (int) (bits ^ (bits >>> 32));
  }

  /**
   * Compares the two specified {@code double} values. The sign of the value returned is the same as
   * that of <code>((Double) a).{@linkplain Double#compareTo compareTo}(b)</code>. As with that
   * method, {@code NaN} is treated as greater than all other values, and {@code 0.0 > -0.0}.
   *
   * <p><b>Note:</b> this method simply delegates to the JDK method {@link Double#compare}. It is
   * provided for consistency with the other primitive types, whose compare methods were not added
   * to the JDK until JDK 7.
   *
   * @param a the first {@code double} to compare
   * @param b the second {@code double} to compare
   * @return a negative value if {@code a} is less than {@code b}; a positive value if {@code a} is
   *     greater than {@code b}; or zero if they are equal
   */
  public static int compare(double a, double b) {
    return Double.compare(a, b);
  }

  /**
   * Returns {@code true} if {@code value} represents a real number. This is equivalent to, but not
   * necessarily implemented as, {@code !(Double.isInfinite(value) || Double.isNaN(value))}.
   *
   * <p><b>Java 8 users:</b> use {@link Double#isFinite(double)} instead.
   *
   * @since 10.0
   */
  public static boolean isFinite(double value) {
    return NEGATIVE_INFINITY < value && value < POSITIVE_INFINITY;
  }

  /**
   * Returns {@code true} if {@code target} is present as an element anywhere in {@code array}. Note
   * that this always returns {@code false} when {@code target} is {@code NaN}.
   *
   * @param array an array of {@code double} values, possibly empty
   * @param target a primitive {@code double} value
   * @return {@code true} if {@code array[i] == target} for some value of {@code i}
   */
  public static boolean contains(double[] array, double target) {
    for (double value : array) {
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
   * @param array an array of {@code double} values, possibly empty
   * @param target a primitive {@code double} value
   * @return the least index {@code i} for which {@code array[i] == target}, or {@code -1} if no
   *     such index exists.
   */
  public static int indexOf(double[] array, double target) {
    return indexOf(array, target, 0, array.length);
  }

  // TODO(kevinb): consider making this public
  private static int indexOf(double[] array, double target, int start, int end) {
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
  public static int indexOf(double[] array, double[] target) {
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
   * @param array an array of {@code double} values, possibly empty
   * @param target a primitive {@code double} value
   * @return the greatest index {@code i} for which {@code array[i] == target}, or {@code -1} if no
   *     such index exists.
   */
  public static int lastIndexOf(double[] array, double target) {
    return lastIndexOf(array, target, 0, array.length);
  }

  // TODO(kevinb): consider making this public
  private static int lastIndexOf(double[] array, double target, int start, int end) {
    for (int i = end - 1; i >= start; i--) {
      if (array[i] == target) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Returns the least value present in {@code array}, using the same rules of comparison as {@link
   * Math#min(double, double)}.
   *
   * @param array a <i>nonempty</i> array of {@code double} values
   * @return the value present in {@code array} that is less than or equal to every other value in
   *     the array
   * @throws IllegalArgumentException if {@code array} is empty
   */
  @GwtIncompatible(
      "Available in GWT! Annotation is to avoid conflict with GWT specialization of base class.")
  public static double min(double... array) {
    checkArgument(array.length > 0);
    double min = array[0];
    for (int i = 1; i < array.length; i++) {
      min = Math.min(min, array[i]);
    }
    return min;
  }

  /**
   * Returns the greatest value present in {@code array}, using the same rules of comparison as
   * {@link Math#max(double, double)}.
   *
   * @param array a <i>nonempty</i> array of {@code double} values
   * @return the value present in {@code array} that is greater than or equal to every other value
   *     in the array
   * @throws IllegalArgumentException if {@code array} is empty
   */
  @GwtIncompatible(
      "Available in GWT! Annotation is to avoid conflict with GWT specialization of base class.")
  public static double max(double... array) {
    checkArgument(array.length > 0);
    double max = array[0];
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
   * @param value the {@code double} value to constrain
   * @param min the lower bound (inclusive) of the range to constrain {@code value} to
   * @param max the upper bound (inclusive) of the range to constrain {@code value} to
   * @throws IllegalArgumentException if {@code min > max}
   * @since 21.0
   */
  @Beta
  public static double constrainToRange(double value, double min, double max) {
    // avoid auto-boxing by not using Preconditions.checkArgument(); see Guava issue 3984
    // Reject NaN by testing for the good case (min <= max) instead of the bad (min > max).
    if (min <= max) {
      return Math.min(Math.max(value, min), max);
    }
    throw new IllegalArgumentException(
        lenientFormat("min (%s) must be less than or equal to max (%s)", min, max));
  }

  /**
   * Returns the values from each provided array combined into a single array. For example, {@code
   * concat(new double[] {a, b}, new double[] {}, new double[] {c}} returns the array {@code {a, b,
   * c}}.
   *
   * @param arrays zero or more {@code double} arrays
   * @return a single array containing all the values from the source arrays, in order
   */
  public static double[] concat(double[]... arrays) {
    int length = 0;
    for (double[] array : arrays) {
      length += array.length;
    }
    double[] result = new double[length];
    int pos = 0;
    for (double[] array : arrays) {
      System.arraycopy(array, 0, result, pos, array.length);
      pos += array.length;
    }
    return result;
  }

  private static final class DoubleConverter extends Converter<String, Double>
      implements Serializable {
    static final DoubleConverter INSTANCE = new DoubleConverter();

    @Override
    protected Double doForward(String value) {
      return Double.valueOf(value);
    }

    @Override
    protected String doBackward(Double value) {
      return value.toString();
    }

    @Override
    public String toString() {
      return "Doubles.stringConverter()";
    }

    private Object readResolve() {
      return INSTANCE;
    }

    private static final long serialVersionUID = 1;
  }

  /**
   * Returns a serializable converter object that converts between strings and doubles using {@link
   * Double#valueOf} and {@link Double#toString()}.
   *
   * @since 16.0
   */
  @Beta
  public static Converter<String, Double> stringConverter() {
    return DoubleConverter.INSTANCE;
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
  public static double[] ensureCapacity(double[] array, int minLength, int padding) {
    checkArgument(minLength >= 0, "Invalid minLength: %s", minLength);
    checkArgument(padding >= 0, "Invalid padding: %s", padding);
    return (array.length < minLength) ? Arrays.copyOf(array, minLength + padding) : array;
  }

  /**
   * Returns a string containing the supplied {@code double} values, converted to strings as
   * specified by {@link Double#toString(double)}, and separated by {@code separator}. For example,
   * {@code join("-", 1.0, 2.0, 3.0)} returns the string {@code "1.0-2.0-3.0"}.
   *
   * <p>Note that {@link Double#toString(double)} formats {@code double} differently in GWT
   * sometimes. In the previous example, it returns the string {@code "1-2-3"}.
   *
   * @param separator the text that should appear between consecutive values in the resulting string
   *     (but not at the start or end)
   * @param array an array of {@code double} values, possibly empty
   */
  public static String join(String separator, double... array) {
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
   * Returns a comparator that compares two {@code double} arrays <a
   * href="http://en.wikipedia.org/wiki/Lexicographical_order">lexicographically</a>. That is, it
   * compares, using {@link #compare(double, double)}), the first pair of values that follow any
   * common prefix, or when one array is a prefix of the other, treats the shorter array as the
   * lesser. For example, {@code [] < [1.0] < [1.0, 2.0] < [2.0]}.
   *
   * <p>The returned comparator is inconsistent with {@link Object#equals(Object)} (since arrays
   * support only identity equality), but it is consistent with {@link Arrays#equals(double[],
   * double[])}.
   *
   * @since 2.0
   */
  public static Comparator<double[]> lexicographicalComparator() {
    return LexicographicalComparator.INSTANCE;
  }

  private enum LexicographicalComparator implements Comparator<double[]> {
    INSTANCE;

    @Override
    public int compare(double[] left, double[] right) {
      int minLength = Math.min(left.length, right.length);
      for (int i = 0; i < minLength; i++) {
        int result = Double.compare(left[i], right[i]);
        if (result != 0) {
          return result;
        }
      }
      return left.length - right.length;
    }

    @Override
    public String toString() {
      return "Doubles.lexicographicalComparator()";
    }
  }

  /**
   * Sorts the elements of {@code array} in descending order.
   *
   * <p>Note that this method uses the total order imposed by {@link Double#compare}, which treats
   * all NaN values as equal and 0.0 as greater than -0.0.
   *
   * @since 23.1
   */
  public static void sortDescending(double[] array) {
    checkNotNull(array);
    sortDescending(array, 0, array.length);
  }

  /**
   * Sorts the elements of {@code array} between {@code fromIndex} inclusive and {@code toIndex}
   * exclusive in descending order.
   *
   * <p>Note that this method uses the total order imposed by {@link Double#compare}, which treats
   * all NaN values as equal and 0.0 as greater than -0.0.
   *
   * @since 23.1
   */
  public static void sortDescending(double[] array, int fromIndex, int toIndex) {
    checkNotNull(array);
    checkPositionIndexes(fromIndex, toIndex, array.length);
    Arrays.sort(array, fromIndex, toIndex);
    reverse(array, fromIndex, toIndex);
  }

  /**
   * Reverses the elements of {@code array}. This is equivalent to {@code
   * Collections.reverse(Doubles.asList(array))}, but is likely to be more efficient.
   *
   * @since 23.1
   */
  public static void reverse(double[] array) {
    checkNotNull(array);
    reverse(array, 0, array.length);
  }

  /**
   * Reverses the elements of {@code array} between {@code fromIndex} inclusive and {@code toIndex}
   * exclusive. This is equivalent to {@code
   * Collections.reverse(Doubles.asList(array).subList(fromIndex, toIndex))}, but is likely to be
   * more efficient.
   *
   * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > array.length}, or
   *     {@code toIndex > fromIndex}
   * @since 23.1
   */
  public static void reverse(double[] array, int fromIndex, int toIndex) {
    checkNotNull(array);
    checkPositionIndexes(fromIndex, toIndex, array.length);
    for (int i = fromIndex, j = toIndex - 1; i < j; i++, j--) {
      double tmp = array[i];
      array[i] = array[j];
      array[j] = tmp;
    }
  }

  /**
   * Returns an array containing each value of {@code collection}, converted to a {@code double}
   * value in the manner of {@link Number#doubleValue}.
   *
   * <p>Elements are copied from the argument collection as if by {@code collection.toArray()}.
   * Calling this method is as thread-safe as calling that method.
   *
   * @param collection a collection of {@code Number} instances
   * @return an array containing the same values as {@code collection}, in the same order, converted
   *     to primitives
   * @throws NullPointerException if {@code collection} or any of its elements is null
   * @since 1.0 (parameter was {@code Collection<Double>} before 12.0)
   */
  public static double[] toArray(Collection<? extends Number> collection) {
    if (collection instanceof DoubleArrayAsList) {
      return ((DoubleArrayAsList) collection).toDoubleArray();
    }

    Object[] boxedArray = collection.toArray();
    int len = boxedArray.length;
    double[] array = new double[len];
    for (int i = 0; i < len; i++) {
      // checkNotNull for GWT (do not optimize)
      array[i] = ((Number) checkNotNull(boxedArray[i])).doubleValue();
    }
    return array;
  }

  /**
   * Returns a fixed-size list backed by the specified array, similar to {@link
   * Arrays#asList(Object[])}. The list supports {@link List#set(int, Object)}, but any attempt to
   * set a value to {@code null} will result in a {@link NullPointerException}.
   *
   * <p>The returned list maintains the values, but not the identities, of {@code Double} objects
   * written to or read from it. For example, whether {@code list.get(0) == list.get(0)} is true for
   * the returned list is unspecified.
   *
   * <p>The returned list may have unexpected behavior if it contains {@code NaN}, or if {@code NaN}
   * is used as a parameter to any of its methods.
   *
   * <p><b>Note:</b> when possible, you should represent your data as an {@link
   * ImmutableDoubleArray} instead, which has an {@link ImmutableDoubleArray#asList asList} view.
   *
   * @param backingArray the array to back the list
   * @return a list view of the array
   */
  public static List<Double> asList(double... backingArray) {
    if (backingArray.length == 0) {
      return Collections.emptyList();
    }
    return new DoubleArrayAsList(backingArray);
  }

  @GwtCompatible
  private static class DoubleArrayAsList extends AbstractList<Double>
      implements RandomAccess, Serializable {
    final double[] array;
    final int start;
    final int end;

    DoubleArrayAsList(double[] array) {
      this(array, 0, array.length);
    }

    DoubleArrayAsList(double[] array, int start, int end) {
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
    public Double get(int index) {
      checkElementIndex(index, size());
      return array[start + index];
    }

    @Override
    public Spliterator.OfDouble spliterator() {
      return Spliterators.spliterator(array, start, end, 0);
    }

    @Override
    public boolean contains(@CheckForNull Object target) {
      // Overridden to prevent a ton of boxing
      return (target instanceof Double)
          && Doubles.indexOf(array, (Double) target, start, end) != -1;
    }

    @Override
    public int indexOf(@CheckForNull Object target) {
      // Overridden to prevent a ton of boxing
      if (target instanceof Double) {
        int i = Doubles.indexOf(array, (Double) target, start, end);
        if (i >= 0) {
          return i - start;
        }
      }
      return -1;
    }

    @Override
    public int lastIndexOf(@CheckForNull Object target) {
      // Overridden to prevent a ton of boxing
      if (target instanceof Double) {
        int i = Doubles.lastIndexOf(array, (Double) target, start, end);
        if (i >= 0) {
          return i - start;
        }
      }
      return -1;
    }

    @Override
    public Double set(int index, Double element) {
      checkElementIndex(index, size());
      double oldValue = array[start + index];
      // checkNotNull for GWT (do not optimize)
      array[start + index] = checkNotNull(element);
      return oldValue;
    }

    @Override
    public List<Double> subList(int fromIndex, int toIndex) {
      int size = size();
      checkPositionIndexes(fromIndex, toIndex, size);
      if (fromIndex == toIndex) {
        return Collections.emptyList();
      }
      return new DoubleArrayAsList(array, start + fromIndex, start + toIndex);
    }

    @Override
    public boolean equals(@CheckForNull Object object) {
      if (object == this) {
        return true;
      }
      if (object instanceof DoubleArrayAsList) {
        DoubleArrayAsList that = (DoubleArrayAsList) object;
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
        result = 31 * result + Doubles.hashCode(array[i]);
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

    double[] toDoubleArray() {
      return Arrays.copyOfRange(array, start, end);
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * This is adapted from the regex suggested by {@link Double#valueOf(String)} for prevalidating
   * inputs. All valid inputs must pass this regex, but it's semantically fine if not all inputs
   * that pass this regex are valid -- only a performance hit is incurred, not a semantics bug.
   */
  @GwtIncompatible // regular expressions
  static final
  java.util.regex.Pattern
      FLOATING_POINT_PATTERN = fpPattern();

  @GwtIncompatible // regular expressions
  private static
  java.util.regex.Pattern
      fpPattern() {
    /*
     * We use # instead of * for possessive quantifiers. This lets us strip them out when building
     * the regex for RE2 (which doesn't support them) but leave them in when building it for
     * java.util.regex (where we want them in order to avoid catastrophic backtracking).
     */
    String decimal = "(?:\\d+#(?:\\.\\d*#)?|\\.\\d+#)";
    String completeDec = decimal + "(?:[eE][+-]?\\d+#)?[fFdD]?";
    String hex = "(?:[0-9a-fA-F]+#(?:\\.[0-9a-fA-F]*#)?|\\.[0-9a-fA-F]+#)";
    String completeHex = "0[xX]" + hex + "[pP][+-]?\\d+#[fFdD]?";
    String fpPattern = "[+-]?(?:NaN|Infinity|" + completeDec + "|" + completeHex + ")";
    fpPattern =
        fpPattern.replace(
            "#",
            "+"
            );
    return
    java.util.regex.Pattern
        .compile(fpPattern);
  }

  /**
   * Parses the specified string as a double-precision floating point value. The ASCII character
   * {@code '-'} (<code>'&#92;u002D'</code>) is recognized as the minus sign.
   *
   * <p>Unlike {@link Double#parseDouble(String)}, this method returns {@code null} instead of
   * throwing an exception if parsing fails. Valid inputs are exactly those accepted by {@link
   * Double#valueOf(String)}, except that leading and trailing whitespace is not permitted.
   *
   * <p>This implementation is likely to be faster than {@code Double.parseDouble} if many failures
   * are expected.
   *
   * @param string the string representation of a {@code double} value
   * @return the floating point value represented by {@code string}, or {@code null} if {@code
   *     string} has a length of zero or cannot be parsed as a {@code double} value
   * @throws NullPointerException if {@code string} is {@code null}
   * @since 14.0
   */
  @Beta
  @GwtIncompatible // regular expressions
  @CheckForNull
  public static Double tryParse(String string) {
    if (FLOATING_POINT_PATTERN.matcher(string).matches()) {
      // TODO(lowasser): could be potentially optimized, but only with
      // extensive testing
      try {
        return Double.parseDouble(string);
      } catch (NumberFormatException e) {
        // Double.parseDouble has changed specs several times, so fall through
        // gracefully
      }
    }
    return null;
  }
}

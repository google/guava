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

import com.google.common.annotations.GwtCompatible;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.RandomAccess;
import javax.annotation.CheckForNull;

/**
 * Static utility methods pertaining to {@code boolean} primitives, that are not already found in
 * either {@link Boolean} or {@link Arrays}.
 *
 * <p>See the Guava User Guide article on <a
 * href="https://github.com/google/guava/wiki/PrimitivesExplained">primitive utilities</a>.
 *
 * @author Kevin Bourrillion
 * @since 1.0
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
public final class Booleans {
  private Booleans() {}

  /** Comparators for {@code Boolean} values. */
  private enum BooleanComparator implements Comparator<Boolean> {
    TRUE_FIRST(1, "Booleans.trueFirst()"),
    FALSE_FIRST(-1, "Booleans.falseFirst()");

    private final int trueValue;
    private final String toString;

    BooleanComparator(int trueValue, String toString) {
      this.trueValue = trueValue;
      this.toString = toString;
    }

    @Override
    public int compare(Boolean a, Boolean b) {
      int aVal = a ? trueValue : 0;
      int bVal = b ? trueValue : 0;
      return bVal - aVal;
    }

    @Override
    public String toString() {
      return toString;
    }
  }

  /**
   * Returns a {@code Comparator<Boolean>} that sorts {@code true} before {@code false}.
   *
   * <p>This is particularly useful in Java 8+ in combination with {@code Comparator.comparing},
   * e.g. {@code Comparator.comparing(Foo::hasBar, trueFirst())}.
   *
   * @since 21.0
   */
  public static Comparator<Boolean> trueFirst() {
    return BooleanComparator.TRUE_FIRST;
  }

  /**
   * Returns a {@code Comparator<Boolean>} that sorts {@code false} before {@code true}.
   *
   * <p>This is particularly useful in Java 8+ in combination with {@code Comparator.comparing},
   * e.g. {@code Comparator.comparing(Foo::hasBar, falseFirst())}.
   *
   * @since 21.0
   */
  public static Comparator<Boolean> falseFirst() {
    return BooleanComparator.FALSE_FIRST;
  }

  /**
   * Returns a hash code for {@code value}; equal to the result of invoking {@code ((Boolean)
   * value).hashCode()}.
   *
   * <p><b>Java 8 users:</b> use {@link Boolean#hashCode(boolean)} instead.
   *
   * @param value a primitive {@code boolean} value
   * @return a hash code for the value
   */
  public static int hashCode(boolean value) {
    return value ? 1231 : 1237;
  }

  /**
   * Compares the two specified {@code boolean} values in the standard way ({@code false} is
   * considered less than {@code true}). The sign of the value returned is the same as that of
   * {@code ((Boolean) a).compareTo(b)}.
   *
   * <p><b>Note for Java 7 and later:</b> this method should be treated as deprecated; use the
   * equivalent {@link Boolean#compare} method instead.
   *
   * @param a the first {@code boolean} to compare
   * @param b the second {@code boolean} to compare
   * @return a positive number if only {@code a} is {@code true}, a negative number if only {@code
   *     b} is true, or zero if {@code a == b}
   */
  public static int compare(boolean a, boolean b) {
    return (a == b) ? 0 : (a ? 1 : -1);
  }

  /**
   * Returns {@code true} if {@code target} is present as an element anywhere in {@code array}.
   *
   * <p><b>Note:</b> consider representing the array as a {@link java.util.BitSet} instead,
   * replacing {@code Booleans.contains(array, true)} with {@code !bitSet.isEmpty()} and {@code
   * Booleans.contains(array, false)} with {@code bitSet.nextClearBit(0) == sizeOfBitSet}.
   *
   * @param array an array of {@code boolean} values, possibly empty
   * @param target a primitive {@code boolean} value
   * @return {@code true} if {@code array[i] == target} for some value of {@code i}
   */
  public static boolean contains(boolean[] array, boolean target) {
    for (boolean value : array) {
      if (value == target) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the index of the first appearance of the value {@code target} in {@code array}.
   *
   * <p><b>Note:</b> consider representing the array as a {@link java.util.BitSet} instead, and
   * using {@link java.util.BitSet#nextSetBit(int)} or {@link java.util.BitSet#nextClearBit(int)}.
   *
   * @param array an array of {@code boolean} values, possibly empty
   * @param target a primitive {@code boolean} value
   * @return the least index {@code i} for which {@code array[i] == target}, or {@code -1} if no
   *     such index exists.
   */
  public static int indexOf(boolean[] array, boolean target) {
    return indexOf(array, target, 0, array.length);
  }

  // TODO(kevinb): consider making this public
  private static int indexOf(boolean[] array, boolean target, int start, int end) {
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
  public static int indexOf(boolean[] array, boolean[] target) {
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
   * @param array an array of {@code boolean} values, possibly empty
   * @param target a primitive {@code boolean} value
   * @return the greatest index {@code i} for which {@code array[i] == target}, or {@code -1} if no
   *     such index exists.
   */
  public static int lastIndexOf(boolean[] array, boolean target) {
    return lastIndexOf(array, target, 0, array.length);
  }

  // TODO(kevinb): consider making this public
  private static int lastIndexOf(boolean[] array, boolean target, int start, int end) {
    for (int i = end - 1; i >= start; i--) {
      if (array[i] == target) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Returns the values from each provided array combined into a single array. For example, {@code
   * concat(new boolean[] {a, b}, new boolean[] {}, new boolean[] {c}} returns the array {@code {a,
   * b, c}}.
   *
   * @param arrays zero or more {@code boolean} arrays
   * @return a single array containing all the values from the source arrays, in order
   */
  public static boolean[] concat(boolean[]... arrays) {
    int length = 0;
    for (boolean[] array : arrays) {
      length += array.length;
    }
    boolean[] result = new boolean[length];
    int pos = 0;
    for (boolean[] array : arrays) {
      System.arraycopy(array, 0, result, pos, array.length);
      pos += array.length;
    }
    return result;
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
  public static boolean[] ensureCapacity(boolean[] array, int minLength, int padding) {
    checkArgument(minLength >= 0, "Invalid minLength: %s", minLength);
    checkArgument(padding >= 0, "Invalid padding: %s", padding);
    return (array.length < minLength) ? Arrays.copyOf(array, minLength + padding) : array;
  }

  /**
   * Returns a string containing the supplied {@code boolean} values separated by {@code separator}.
   * For example, {@code join("-", false, true, false)} returns the string {@code
   * "false-true-false"}.
   *
   * @param separator the text that should appear between consecutive values in the resulting string
   *     (but not at the start or end)
   * @param array an array of {@code boolean} values, possibly empty
   */
  public static String join(String separator, boolean... array) {
    checkNotNull(separator);
    if (array.length == 0) {
      return "";
    }

    // For pre-sizing a builder, just get the right order of magnitude
    StringBuilder builder = new StringBuilder(array.length * 7);
    builder.append(array[0]);
    for (int i = 1; i < array.length; i++) {
      builder.append(separator).append(array[i]);
    }
    return builder.toString();
  }

  /**
   * Returns a comparator that compares two {@code boolean} arrays <a
   * href="http://en.wikipedia.org/wiki/Lexicographical_order">lexicographically</a>. That is, it
   * compares, using {@link #compare(boolean, boolean)}), the first pair of values that follow any
   * common prefix, or when one array is a prefix of the other, treats the shorter array as the
   * lesser. For example, {@code [] < [false] < [false, true] < [true]}.
   *
   * <p>The returned comparator is inconsistent with {@link Object#equals(Object)} (since arrays
   * support only identity equality), but it is consistent with {@link Arrays#equals(boolean[],
   * boolean[])}.
   *
   * @since 2.0
   */
  public static Comparator<boolean[]> lexicographicalComparator() {
    return LexicographicalComparator.INSTANCE;
  }

  private enum LexicographicalComparator implements Comparator<boolean[]> {
    INSTANCE;

    @Override
    public int compare(boolean[] left, boolean[] right) {
      int minLength = Math.min(left.length, right.length);
      for (int i = 0; i < minLength; i++) {
        int result = Booleans.compare(left[i], right[i]);
        if (result != 0) {
          return result;
        }
      }
      return left.length - right.length;
    }

    @Override
    public String toString() {
      return "Booleans.lexicographicalComparator()";
    }
  }

  /**
   * Copies a collection of {@code Boolean} instances into a new array of primitive {@code boolean}
   * values.
   *
   * <p>Elements are copied from the argument collection as if by {@code collection.toArray()}.
   * Calling this method is as thread-safe as calling that method.
   *
   * <p><b>Note:</b> consider representing the collection as a {@link java.util.BitSet} instead.
   *
   * @param collection a collection of {@code Boolean} objects
   * @return an array containing the same values as {@code collection}, in the same order, converted
   *     to primitives
   * @throws NullPointerException if {@code collection} or any of its elements is null
   */
  public static boolean[] toArray(Collection<Boolean> collection) {
    if (collection instanceof BooleanArrayAsList) {
      return ((BooleanArrayAsList) collection).toBooleanArray();
    }

    Object[] boxedArray = collection.toArray();
    int len = boxedArray.length;
    boolean[] array = new boolean[len];
    for (int i = 0; i < len; i++) {
      // checkNotNull for GWT (do not optimize)
      array[i] = (Boolean) checkNotNull(boxedArray[i]);
    }
    return array;
  }

  /**
   * Returns a fixed-size list backed by the specified array, similar to {@link
   * Arrays#asList(Object[])}. The list supports {@link List#set(int, Object)}, but any attempt to
   * set a value to {@code null} will result in a {@link NullPointerException}.
   *
   * <p>There are at most two distinct objects in this list, {@code (Boolean) true} and {@code
   * (Boolean) false}. Java guarantees that those are always represented by the same objects.
   *
   * <p>The returned list is serializable.
   *
   * @param backingArray the array to back the list
   * @return a list view of the array
   */
  public static List<Boolean> asList(boolean... backingArray) {
    if (backingArray.length == 0) {
      return Collections.emptyList();
    }
    return new BooleanArrayAsList(backingArray);
  }

  @GwtCompatible
  private static class BooleanArrayAsList extends AbstractList<Boolean>
      implements RandomAccess, Serializable {
    final boolean[] array;
    final int start;
    final int end;

    BooleanArrayAsList(boolean[] array) {
      this(array, 0, array.length);
    }

    BooleanArrayAsList(boolean[] array, int start, int end) {
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
    public Boolean get(int index) {
      checkElementIndex(index, size());
      return array[start + index];
    }

    @Override
    public boolean contains(@CheckForNull Object target) {
      // Overridden to prevent a ton of boxing
      return (target instanceof Boolean)
          && Booleans.indexOf(array, (Boolean) target, start, end) != -1;
    }

    @Override
    public int indexOf(@CheckForNull Object target) {
      // Overridden to prevent a ton of boxing
      if (target instanceof Boolean) {
        int i = Booleans.indexOf(array, (Boolean) target, start, end);
        if (i >= 0) {
          return i - start;
        }
      }
      return -1;
    }

    @Override
    public int lastIndexOf(@CheckForNull Object target) {
      // Overridden to prevent a ton of boxing
      if (target instanceof Boolean) {
        int i = Booleans.lastIndexOf(array, (Boolean) target, start, end);
        if (i >= 0) {
          return i - start;
        }
      }
      return -1;
    }

    @Override
    public Boolean set(int index, Boolean element) {
      checkElementIndex(index, size());
      boolean oldValue = array[start + index];
      // checkNotNull for GWT (do not optimize)
      array[start + index] = checkNotNull(element);
      return oldValue;
    }

    @Override
    public List<Boolean> subList(int fromIndex, int toIndex) {
      int size = size();
      checkPositionIndexes(fromIndex, toIndex, size);
      if (fromIndex == toIndex) {
        return Collections.emptyList();
      }
      return new BooleanArrayAsList(array, start + fromIndex, start + toIndex);
    }

    @Override
    public boolean equals(@CheckForNull Object object) {
      if (object == this) {
        return true;
      }
      if (object instanceof BooleanArrayAsList) {
        BooleanArrayAsList that = (BooleanArrayAsList) object;
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
        result = 31 * result + Booleans.hashCode(array[i]);
      }
      return result;
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder(size() * 7);
      builder.append(array[start] ? "[true" : "[false");
      for (int i = start + 1; i < end; i++) {
        builder.append(array[i] ? ", true" : ", false");
      }
      return builder.append(']').toString();
    }

    boolean[] toBooleanArray() {
      return Arrays.copyOfRange(array, start, end);
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Returns the number of {@code values} that are {@code true}.
   *
   * @since 16.0
   */
  public static int countTrue(boolean... values) {
    int count = 0;
    for (boolean value : values) {
      if (value) {
        count++;
      }
    }
    return count;
  }

  /**
   * Reverses the elements of {@code array}. This is equivalent to {@code
   * Collections.reverse(Booleans.asList(array))}, but is likely to be more efficient.
   *
   * @since 23.1
   */
  public static void reverse(boolean[] array) {
    checkNotNull(array);
    reverse(array, 0, array.length);
  }

  /**
   * Reverses the elements of {@code array} between {@code fromIndex} inclusive and {@code toIndex}
   * exclusive. This is equivalent to {@code
   * Collections.reverse(Booleans.asList(array).subList(fromIndex, toIndex))}, but is likely to be
   * more efficient.
   *
   * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > array.length}, or
   *     {@code toIndex > fromIndex}
   * @since 23.1
   */
  public static void reverse(boolean[] array, int fromIndex, int toIndex) {
    checkNotNull(array);
    checkPositionIndexes(fromIndex, toIndex, array.length);
    for (int i = fromIndex, j = toIndex - 1; i < j; i++, j--) {
      boolean tmp = array[i];
      array[i] = array[j];
      array[j] = tmp;
    }
  }

  /**
   * Performs a right rotation of {@code array} of "distance" places, so that the first element is
   * moved to index "distance", and the element at index {@code i} ends up at index {@code (distance
   * + i) mod array.length}. This is equivalent to {@code Collections.rotate(Booleans.asList(array),
   * distance)}, but is somewhat faster.
   *
   * <p>The provided "distance" may be negative, which will rotate left.
   *
   * @since 32.0.0
   */
  public static void rotate(boolean[] array, int distance) {
    rotate(array, distance, 0, array.length);
  }

  /**
   * Performs a right rotation of {@code array} between {@code fromIndex} inclusive and {@code
   * toIndex} exclusive. This is equivalent to {@code
   * Collections.rotate(Booleans.asList(array).subList(fromIndex, toIndex), distance)}, but is
   * somewhat faster.
   *
   * <p>The provided "distance" may be negative, which will rotate left.
   *
   * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > array.length}, or
   *     {@code toIndex > fromIndex}
   * @since 32.0.0
   */
  public static void rotate(boolean[] array, int distance, int fromIndex, int toIndex) {
    // See Ints.rotate for more details about possible algorithms here.
    checkNotNull(array);
    checkPositionIndexes(fromIndex, toIndex, array.length);
    if (array.length <= 1) {
      return;
    }

    int length = toIndex - fromIndex;
    // Obtain m = (-distance mod length), a non-negative value less than "length". This is how many
    // places left to rotate.
    int m = -distance % length;
    m = (m < 0) ? m + length : m;
    // The current index of what will become the first element of the rotated section.
    int newFirstIndex = m + fromIndex;
    if (newFirstIndex == fromIndex) {
      return;
    }

    reverse(array, fromIndex, newFirstIndex);
    reverse(array, newFirstIndex, toIndex);
    reverse(array, fromIndex, toIndex);
  }
}

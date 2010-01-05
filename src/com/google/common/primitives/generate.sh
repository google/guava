#!/bin/sh
#
# Usage example: ./generate.sh int Int Integer"
# Args are: primitive type, capitalized primitive type, wrapper type
#
# To make changes to the .java files in this package,
# 1. run this script to generate the templates, move the .gen files
#    somewhere else
# 2. modify the template with your intended changes, then rerun the
#    script
# 3. use any three-way merge tool to edit the checked-in source files,
#    using the before-and-after generated files as the bases.
#

if [ "$#" -ne "3" ]
then
  echo "Usage example: ./generate.sh int Int Integer"
  exit 1
fi

# Note: using the strange strings 'primtyp' and 'WrapperCl' so that they match
# the maximum length of the real strings ('boolean' and 'Character').

perl -pe "s/primtyp/$1/g; s/PrimTyp/$2/g; s/WrapperCl/$3/g" << "--EOF--" > $2s.java.gen
/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.primitives;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndexes;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.RandomAccess;

/**
 * Static utility methods pertaining to {@code primtyp} primitives, that are not
 * already found in either {@link WrapperCl} or {@link Arrays}.
 *
 * @author Kevin Bourrillion
 * @since 2009.09.15 <b>tentative</b>
 */
@GwtCompatible
public final class PrimTyps {
  private PrimTyps() {}

  /**
   * The number of bytes required to represent a primitive {@code primtyp}
   * value.
   */
  public static final int BYTES = WrapperCl.SIZE / Byte.SIZE;

  /**
   * Returns a hash code for {@code value}; equal to the result of invoking
   * {@code ((WrapperCl) value).hashCode()}.
   *
   * @param value a primitive {@code primtyp} value
   * @return a hash code for the value
   */
  public static int hashCode(primtyp value) {
    return ??
  }

  /**
   * Returns the {@code primtyp} value that is equal to {@code value}, if possible.
   *
   * @param value any value in the range of the {@code primtyp} type
   * @return the {@code primtyp} value that equals {@code value}
   * @throws IllegalArgumentException if {@code value} is greater than {@link
   *     WrapperCl#MAX_VALUE} or less than {@link WrapperCl#MIN_VALUE}
   */
  public static primtyp checkedCast(long value) {
    primtyp result = (primtyp) value;
    checkArgument(result == value, "Out of range: %s", value);
    return result;
  }

  /**
   * Returns the {@code primtyp} nearest in value to {@code value}.
   *
   * @param value any {@code long} value
   * @return the same value cast to {@code primtyp} if it is in the range of the
   *     {@code primtyp} type, {@link WrapperCl#MAX_VALUE} if it is too large,
   *     or {@link WrapperCl#MIN_VALUE} if it is too small
   */
  public static primtyp saturatedCast(long value) {
    if (value > WrapperCl.MAX_VALUE) {
      return WrapperCl.MAX_VALUE;
    }
    if (value < WrapperCl.MIN_VALUE) {
      return WrapperCl.MIN_VALUE;
    }
    return (primtyp) value;
  }

  /**
   * Compares the two specified {@code primtyp} values. The sign of the value
   * returned is the same as that of {@code ((WrapperCl) a).compareTo(b)}.
   *
   * @param a the first {@code primtyp} to compare
   * @param b the second {@code primtyp} to compare
   * @return a negative value if {@code a} is less than {@code b}; a positive
   *     value if {@code a} is greater than {@code b}; or zero if they are equal
   */
  public static int compare(primtyp a, primtyp b) {
    return (a < b) ? -1 : ((a > b) ? 1 : 0);
  }

  /**
   * Returns {@code true} if {@code target} is present as an element anywhere in
   * {@code array}.
   *
   * @param array an array of {@code primtyp} values, possibly empty
   * @param target a primitive {@code primtyp} value
   * @return {@code true} if {@code array[i] == target} for some value of {@code
   *     i}
   */
  public static boolean contains(primtyp[] array, primtyp target) {
    for (primtyp value : array) {
      if (value == target) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the index of the first appearance of the value {@code target} in
   * {@code array}.
   *
   * @param array an array of {@code primtyp} values, possibly empty
   * @param target a primitive {@code primtyp} value
   * @return the least index {@code i} for which {@code array[i] == target}, or
   *     {@code -1} if no such index exists.
   */
  public static int indexOf(primtyp[] array, primtyp target) {
    return indexOf(array, target, 0, array.length);
  }

  // TODO: consider making this public
  private static int indexOf(
      primtyp[] array, primtyp target, int start, int end) {
    for (int i = start; i < end; i++) {
      if (array[i] == target) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Returns the start position of the first occurrence of the specified {@code
   * target} within {@code array}, or {@code -1} if there is no such occurrence.
   *
   * <p>More formally, returns the lowest index {@code i} such that {@code
   * java.util.Arrays.copyOfRange(array, i, i + target.length)} contains exactly
   * the same elements as {@code target}.
   *
   * @param array the array to search for the sequence {@code target}
   * @param target the array to search for as a sub-sequence of {@code array}
   */
  public static int indexOf(primtyp[] array, primtyp[] target) {
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
   * Returns the index of the last appearance of the value {@code target} in
   * {@code array}.
   *
   * @param array an array of {@code primtyp} values, possibly empty
   * @param target a primitive {@code primtyp} value
   * @return the greatest index {@code i} for which {@code array[i] == target},
   *     or {@code -1} if no such index exists.
   */
  public static int lastIndexOf(primtyp[] array, primtyp target) {
    return lastIndexOf(array, target, 0, array.length);
  }

  // TODO: consider making this public
  private static int lastIndexOf(
      primtyp[] array, primtyp target, int start, int end) {
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
   * @param array a <i>nonempty</i> array of {@code primtyp} values
   * @return the value present in {@code array} that is less than or equal to
   *     every other value in the array
   * @throws IllegalArgumentException if {@code array} is empty
   */
  public static primtyp min(primtyp... array) {
    checkArgument(array.length > 0);
    primtyp min = array[0];
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
   * @param array a <i>nonempty</i> array of {@code primtyp} values
   * @return the value present in {@code array} that is greater than or equal to
   *     every other value in the array
   * @throws IllegalArgumentException if {@code array} is empty
   */
  public static primtyp max(primtyp... array) {
    checkArgument(array.length > 0);
    primtyp max = array[0];
    for (int i = 1; i < array.length; i++) {
      if (array[i] > max) {
        max = array[i];
      }
    }
    return max;
  }

  /**
   * Returns the values from each provided array combined into a single array.
   * For example, {@code concat(new primtyp[] {a, b}, new primtyp[] {}, new
   * primtyp[] {c}} returns the array {@code {a, b, c}}.
   *
   * @param arrays zero or more {@code primtyp} arrays
   * @return a single array containing all the values from the source arrays, in
   *     order
   */
  public static primtyp[] concat(primtyp[]... arrays) {
    int length = 0;
    for (primtyp[] array : arrays) {
      length += array.length;
    }
    primtyp[] result = new primtyp[length];
    int pos = 0;
    for (primtyp[] array : arrays) {
      System.arraycopy(array, 0, result, pos, array.length);
      pos += array.length;
    }
    return result;
  }

  /**
   * Returns a big-endian representation of {@code value} in a ?-element byte
   * array; equivalent to {@code
   * ByteBuffer.allocate(?).putPrimTyp(value).array()}.  For example, the input
   * value {@code ?} would yield the byte array {@code {?}}.
   *
   * <p>If you need to convert and concatenate several values (possibly even of
   * different types), use a shared {@link java.nio.ByteBuffer} instance, or use
   * {@link com.google.common.io.ByteStreams#newDataOutput()} to get a growable
   * buffer.
   */
  @GwtIncompatible("doesn't work")
  public static byte[] toByteArray(primtyp value) {
    return new byte[] {
      ?
    };
  }

  /**
   * Returns the {@code primtyp} value whose big-endian representation is
   * stored in the first ? bytes of {@code bytes}; equivalent to {@code
   * ByteBuffer.wrap(bytes).getPrimTyp()}. For example, the input byte array
   * {@code {?}} would yield the {@code primtyp} value {@code ?}.
   *
   * <p>Arguably, it's preferable to use {@link java.nio.ByteBuffer}; that
   * library exposes much more flexibility at little cost in readability.
   *
   * @throws IllegalArgumentException if {@code bytes} has fewer than ?
   *     elements
   */
  @GwtIncompatible("doesn't work")
  public static primtyp fromByteArray(byte[] bytes) {
    checkArgument(bytes.length >= BYTES,
        "array too small: %s < %s", bytes.length, BYTES);
    return ?
  }

  /**
   * Returns an array containing the same values as {@code array}, but
   * guaranteed to be of a specified minimum length. If {@code array} already
   * has a length of at least {@code minLength}, it is returned directly.
   * Otherwise, a new array of size {@code minLength + padding} is returned,
   * containing the values of {@code array}, and zeroes in the remaining places.
   *
   * @param array the source array
   * @param minLength the minimum length the returned array must guarantee
   * @param padding an extra amount to "grow" the array by if growth is
   *     necessary
   * @throws IllegalArgumentException if {@code minLength} or {@code padding} is
   *     negative
   * @return an array containing the values of {@code array}, with guaranteed
   *     minimum length {@code minLength}
   */
  public static primtyp[] ensureCapacity(
      primtyp[] array, int minLength, int padding) {
    checkArgument(minLength >= 0, "Invalid minLength: %s", minLength);
    checkArgument(padding >= 0, "Invalid padding: %s", padding);
    return (array.length < minLength)
        ? copyOf(array, minLength + padding)
        : array;
  }

  // Arrays.copyOf() requires Java 6
  private static primtyp[] copyOf(primtyp[] original, int length) {
    primtyp[] copy = new primtyp[length];
    System.arraycopy(original, 0, copy, 0, Math.min(original.length, length));
    return copy;
  }

  /**
   * Returns a string containing the supplied {@code primtyp} values separated
   * by {@code separator}. For example, {@code join("-", 1?, 2?, 3?)} returns
   * the string {@code "1-2-3"}.
   *
   * @param separator the text that should appear between consecutive values in
   *     the resulting string (but not at the start or end)
   * @param array an array of {@code primtyp} values, possibly empty
   */
  public static String join(String separator, primtyp... array) {
    checkNotNull(separator);
    if (array.length == 0) {
      return "";
    }

    // For pre-sizing a builder, just get the right order of magnitude
    StringBuilder builder = new StringBuilder(array.length * ??);
    builder.append(array[0]);
    for (int i = 1; i < array.length; i++) {
      builder.append(separator).append(array[i]);
    }
    return builder.toString();
  }

  /**
   * Returns a comparator that compares two {@code primtyp} arrays
   * lexicographically. That is, it compares, using {@link
   * #compare(primtyp, primtyp)}), the first pair of values that follow any
   * common prefix, or when one array is a prefix of the other, treats the
   * shorter array as the lesser. For example, {@code [] < [1] < [1, 2] < [2]}.
   *
   * <p>The returned comparator is inconsistent with {@link
   * Object#equals(Object)} (since arrays support only identity equality), but
   * it is consistent with {@link Arrays#equals(primtyp[], primtyp[])}.
   *
   * @see <a href="http://en.wikipedia.org/wiki/Lexicographical_order">
   *     Lexicographical order</a> article at Wikipedia
   * @since 2010.01.04 <b>tentative</b>
   */
  public static Comparator<primtyp[]> lexicographicalComparator() {
    return LexicographicalComparator.INSTANCE;
  }

  private enum LexicographicalComparator implements Comparator<primtyp[]> {
    INSTANCE;

    public int compare(primtyp[] left, primtyp[] right) {
      int minLength = Math.min(left.length, right.length);
      for (int i = 0; i < minLength; i++) {
        int result = PrimTyps.compare(left[i], right[i]);
        if (result != 0) {
          return result;
        }
      }
      return left.length - right.length;
    }
  }

  /**
   * Copies a collection of {@code WrapperCl} instances into a new array of
   * primitive {@code primtyp} values.
   *
   * <p>Elements are copied from the argument collection as if by {@code
   * collection.toArray()}.  Calling this method is as thread-safe as calling
   * that method.
   *
   * @param collection a collection of {@code WrapperCl} objects
   * @return an array containing the same values as {@code collection}, in the
   *     same order, converted to primitives
   * @throws NullPointerException if {@code collection} or any of its elements
   *     is null
   */
  public static primtyp[] toArray(Collection<WrapperCl> collection) {
    if (collection instanceof PrimTypArrayAsList) {
      return ((PrimTypArrayAsList) collection).toPrimTypArray();
    }

    Object[] boxedArray = collection.toArray();
    int len = boxedArray.length;
    primtyp[] array = new primtyp[len];
    for (int i = 0; i < len; i++) {
      array[i] = (WrapperCl) boxedArray[i];
    }
    return array;
  }

  /**
   * Returns a fixed-size list backed by the specified array, similar to {@link
   * Arrays#asList(Object[])}. The list supports {@link List#set(int, Object)},
   * but any attempt to set a value to {@code null} will result in a {@link
   * NullPointerException}.
   *
   * <p>The returned list maintains the values, but not the identities, of
   * {@code WrapperCl} objects written to or read from it.  For example, whether
   * {@code list.get(0) == list.get(0)} is true for the returned list is
   * unspecified.
   *
   * @param backingArray the array to back the list
   * @return a list view of the array
   */
  public static List<WrapperCl> asList(primtyp... backingArray) {
    if (backingArray.length == 0) {
      return Collections.emptyList();
    }
    return new PrimTypArrayAsList(backingArray);
  }

  @GwtCompatible
  private static class PrimTypArrayAsList extends AbstractList<WrapperCl>
      implements RandomAccess, Serializable {
    final primtyp[] array;
    final int start;
    final int end;

    PrimTypArrayAsList(primtyp[] array) {
      this(array, 0, array.length);
    }

    PrimTypArrayAsList(primtyp[] array, int start, int end) {
      this.array = array;
      this.start = start;
      this.end = end;
    }

    @Override public int size() {
      return end - start;
    }

    @Override public boolean isEmpty() {
      return false;
    }

    @Override public WrapperCl get(int index) {
      checkElementIndex(index, size());
      return array[start + index];
    }

    @Override public boolean contains(Object target) {
      // Overridden to prevent a ton of boxing
      return (target instanceof WrapperCl)
          && PrimTyps.indexOf(array, (WrapperCl) target, start, end) != -1;
    }

    @Override public int indexOf(Object target) {
      // Overridden to prevent a ton of boxing
      if (target instanceof WrapperCl) {
        int i = PrimTyps.indexOf(array, (WrapperCl) target, start, end);
        if (i >= 0) {
          return i - start;
        }
      }
      return -1;
    }

    @Override public int lastIndexOf(Object target) {
      // Overridden to prevent a ton of boxing
      if (target instanceof WrapperCl) {
        int i = PrimTyps.lastIndexOf(array, (WrapperCl) target, start, end);
        if (i >= 0) {
          return i - start;
        }
      }
      return -1;
    }

    @Override public WrapperCl set(int index, WrapperCl element) {
      checkElementIndex(index, size());
      primtyp oldValue = array[start + index];
      array[start + index] = element;
      return oldValue;
    }

    /** In GWT, List and AbstractList do not have the subList method. */
    /*@Override*/ public List<WrapperCl> subList(int fromIndex, int toIndex) {
      int size = size();
      checkPositionIndexes(fromIndex, toIndex, size);
      if (fromIndex == toIndex) {
        return Collections.emptyList();
      }
      return new PrimTypArrayAsList(array, start + fromIndex, start + toIndex);
    }

    @Override public boolean equals(Object object) {
      if (object == this) {
        return true;
      }
      if (object instanceof PrimTypArrayAsList) {
        PrimTypArrayAsList that = (PrimTypArrayAsList) object;
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

    @Override public int hashCode() {
      int result = 1;
      for (int i = start; i < end; i++) {
        result = 31 * result + PrimTyps.hashCode(array[i]);
      }
      return result;
    }

    @Override public String toString() {
      StringBuilder builder = new StringBuilder(size() * ??);
      builder.append('[').append(array[start]);
      for (int i = start + 1; i < end; i++) {
        builder.append(", ").append(array[i]);
      }
      return builder.append(']').toString();
    }

    primtyp[] toPrimTypArray() {
      // Arrays.copyOfRange() requires Java 6
      int size = size();
      primtyp[] result = new primtyp[size];
      System.arraycopy(array, start, result, 0, size);
      return result;
    }

    private static final long serialVersionUID = 0;
  }
}
--EOF--


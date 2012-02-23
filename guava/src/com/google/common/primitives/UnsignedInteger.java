/*
 * Copyright (C) 2011 The Guava Authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.primitives;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.primitives.UnsignedInts.INT_MASK;
import static com.google.common.primitives.UnsignedInts.compare;
import static com.google.common.primitives.UnsignedInts.toLong;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;

import java.math.BigInteger;

import javax.annotation.Nullable;

/**
 * A wrapper class for unsigned {@code int} values, supporting arithmetic operations.
 * 
 * <p>In some cases, when speed is more important than code readability, it may be faster simply to
 * treat primitive {@code int} values as unsigned, using the methods from {@link UnsignedInts}.
 * 
 * <p>See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/PrimitivesExplained#Unsigned_support">
 * unsigned primitive utilities</a>.
 * 
 * @author Louis Wasserman
 * @since 11.0
 */
@Beta
@GwtCompatible(emulated = true)
public final class UnsignedInteger extends Number implements Comparable<UnsignedInteger> {
  public static final UnsignedInteger ZERO = asUnsigned(0);
  public static final UnsignedInteger ONE = asUnsigned(1);
  public static final UnsignedInteger MAX_VALUE = asUnsigned(-1);

  private final int value;

  private UnsignedInteger(int value) {
    this.value = value & 0xffffffff;
  }

  /**
   * Returns an {@code UnsignedInteger} that, when treated as signed, is
   * equal to {@code value}.
   */
  public static UnsignedInteger asUnsigned(int value) {
    return new UnsignedInteger(value);
  }

  /**
   * Returns an {@code UnsignedInteger} that is equal to {@code value},
   * if possible.  The inverse operation of {@link #longValue()}.
   */
  public static UnsignedInteger valueOf(long value) {
    checkArgument((value & INT_MASK) == value,
        "value (%s) is outside the range for an unsigned integer value", value);
    return asUnsigned((int) value);
  }

  /**
   * Returns a {@code UnsignedInteger} representing the same value as the specified
   * {@link BigInteger}. This is the inverse operation of {@link #bigIntegerValue()}.
   * 
   * @throws IllegalArgumentException if {@code value} is negative or {@code value >= 2^32}
   */
  public static UnsignedInteger valueOf(BigInteger value) {
    checkNotNull(value);
    checkArgument(value.signum() >= 0 && value.bitLength() <= Integer.SIZE,
        "value (%s) is outside the range for an unsigned integer value", value);
    return asUnsigned(value.intValue());
  }

  /**
   * Returns an {@code UnsignedInteger} holding the value of the specified {@code String}, parsed
   * as an unsigned {@code int} value.
   * 
   * @throws NumberFormatException if the string does not contain a parsable unsigned {@code int}
   *         value
   */
  public static UnsignedInteger valueOf(String string) {
    return valueOf(string, 10);
  }

  /**
   * Returns an {@code UnsignedInteger} holding the value of the specified {@code String}, parsed
   * as an unsigned {@code int} value in the specified radix.
   * 
   * @throws NumberFormatException if the string does not contain a parsable unsigned {@code int}
   *         value
   */
  public static UnsignedInteger valueOf(String string, int radix) {
    return asUnsigned(UnsignedInts.parseUnsignedInt(string, radix));
  }

  /**
   * Returns the result of adding this and {@code val}. If the result would have more than 32 bits,
   * returns the low 32 bits of the result.
   */
  public UnsignedInteger add(UnsignedInteger val) {
    checkNotNull(val);
    return asUnsigned(this.value + val.value);
  }

  /**
   * Returns the result of subtracting this and {@code val}. If the result would be negative,
   * returns the low 32 bits of the result.
   */
  public UnsignedInteger subtract(UnsignedInteger val) {
    checkNotNull(val);
    return asUnsigned(this.value - val.value);
  }

  /**
   * Returns the result of multiplying this and {@code val}. If the result would have more than 32
   * bits, returns the low 32 bits of the result.
   */
  @GwtIncompatible("Does not truncate correctly")
  public UnsignedInteger multiply(UnsignedInteger val) {
    checkNotNull(val);
    return asUnsigned(value * val.value);
  }

  /**
   * Returns the result of dividing this by {@code val}.
   */
  public UnsignedInteger divide(UnsignedInteger val) {
    checkNotNull(val);
    return asUnsigned(UnsignedInts.divide(value, val.value));
  }

  /**
   * Returns the remainder of dividing this by {@code val}.
   */
  public UnsignedInteger remainder(UnsignedInteger val) {
    checkNotNull(val);
    return asUnsigned(UnsignedInts.remainder(value, val.value));
  }

  /**
   * Returns the value of this {@code UnsignedInteger} as an {@code int}. This is an inverse
   * operation to {@link #asUnsigned}.
   * 
   * <p>Note that if this {@code UnsignedInteger} holds a value {@code >= 2^31}, the returned value
   * will be equal to {@code this - 2^32}.
   */
  @Override
  public int intValue() {
    return value;
  }

  /**
   * Returns the value of this {@code UnsignedInteger} as a {@code long}.
   */
  @Override
  public long longValue() {
    return toLong(value);
  }

  /**
   * Returns the value of this {@code UnsignedInteger} as a {@code float}, analogous to a widening
   * primitive conversion from {@code int} to {@code float}, and correctly rounded.
   */
  @Override
  public float floatValue() {
    return longValue();
  }

  /**
   * Returns the value of this {@code UnsignedInteger} as a {@code float}, analogous to a widening
   * primitive conversion from {@code int} to {@code double}, and correctly rounded.
   */
  @Override
  public double doubleValue() {
    return longValue();
  }

  /**
   * Returns the value of this {@code UnsignedInteger} as a {@link BigInteger}.
   */
  public BigInteger bigIntegerValue() {
    return BigInteger.valueOf(longValue());
  }

  /**
   * Compares this unsigned integer to another unsigned integer.
   * Returns {@code 0} if they are equal, a negative number if {@code this < other},
   * and a positive number if {@code this > other}.
   */
  @Override
  public int compareTo(UnsignedInteger other) {
    checkNotNull(other);
    return compare(value, other.value);
  }

  @Override
  public int hashCode() {
    return value;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj instanceof UnsignedInteger) {
      UnsignedInteger other = (UnsignedInteger) obj;
      return value == other.value;
    }
    return false;
  }

  /**
   * Returns a string representation of the {@code UnsignedInteger} value, in base 10.
   */
  @Override
  public String toString() {
    return toString(10);
  }

  /**
   * Returns a string representation of the {@code UnsignedInteger} value, in base {@code radix}.
   * If {@code radix < Character.MIN_RADIX} or {@code radix > Character.MAX_RADIX}, the radix
   * {@code 10} is used.
   */
  public String toString(int radix) {
    return UnsignedInts.toString(value, radix);
  }
}

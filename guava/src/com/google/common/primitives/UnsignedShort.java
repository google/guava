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
import static com.google.common.primitives.UnsignedShorts.SHORT_MASK;
import static com.google.common.primitives.UnsignedShorts.compare;

import java.math.BigInteger;

import javax.annotation.Nullable;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;

/**
 * A wrapper class for unsigned {@code int} values, supporting arithmetic operations.
 * 
 * <p>In some cases, when speed is more important than code readability, it may be faster simply to
 * treat primitive {@code short} values as unsigned, using the methods from {@link UnsignedShorts}.
 * 
 * @author Louis Wasserman
 * @author Johannes Schneider (<a href="mailto:js@cedarsoft.com">js@cedarsoft.com</a>)
 * @since 19.0
 */
@SuppressWarnings("serial")
@Beta
@GwtCompatible(emulated = true)
public final class UnsignedShort extends Number implements Comparable<UnsignedShort> {
  public static final UnsignedShort ZERO = asUnsigned((short) 0);
  public static final UnsignedShort ONE = asUnsigned((short) 1);
  public static final UnsignedShort MAX_VALUE = asUnsigned((short) -1);

  private final short value;

  private UnsignedShort(short value) {
    this.value = value;
  }

  /**
   * Returns an {@code UnsignedShort} that, when treated as signed, is equal to {@code value}. The
   * inverse operation is {@link #shortValue()}.
   *
   * <p>Put another way, if {@code value} is negative, the returned result will be equal to
   * {@code 2^16 + value}; otherwise, the returned result will be equal to {@code value}.
   */
  public static UnsignedShort asUnsigned(short value) {
    return new UnsignedShort(value);
  }

  /**
   * Returns a {@code UnsignedShort} representing the same value as the specified {@code long}.
   * This is the inverse operation of {@link #longValue()}.
   * 
   * @throws IllegalArgumentException
   *           if {@code value} is negative or {@code value >= 2^16}
   */
  public static UnsignedShort valueOf(long value) {
    checkArgument((value & SHORT_MASK) == value,
        "value (%s) is outside the range for an unsigned short value", value);
    return asUnsigned((short) value);
  }

  /**
   * Returns a {@code UnsignedShort} representing the same value as the specified
   * {@link BigInteger}. This is the inverse operation of {@link #bigIntegerValue()}.
   * 
   * @throws IllegalArgumentException
   *           if {@code value} is negative or {@code value >= 2^16}
   */
  public static UnsignedShort valueOf(BigInteger value) {
    checkNotNull(value);
    checkArgument(value.signum() >= 0 && value.bitLength() <= Short.SIZE,
        "value (%s) is outside the range for an unsigned short value", value);
    return asUnsigned(value.shortValue());
  }

  /**
   * Returns an {@code UnsignedShort} holding the value of the specified {@code String}, parsed as
   * an unsigned {@code short} value.
   * 
   * @throws NumberFormatException
   *           if the string does not contain a parsable unsigned {@code short} value
   */
  public static UnsignedShort valueOf(String string) {
    return valueOf(string, 10);
  }

  /**
   * Returns an {@code UnsignedShort} holding the value of the specified {@code String}, parsed as
   * an unsigned {@code short} value in the specified radix.
   * 
   * @throws NumberFormatException
   *           if the string does not contain a parsable unsigned {@code short} value
   */
  public static UnsignedShort valueOf(String string, int radix) {
    return asUnsigned(UnsignedShorts.parseUnsignedShort(string, radix));
  }

  /**
   * Returns the result of adding this and {@code val}. If the result would have more than 32 bits,
   * returns the low 16 bits of the result.
   */
  public UnsignedShort add(UnsignedShort val) {
    checkNotNull(val);
    return asUnsigned((short) (this.value + val.value));
  }

  /**
   * Returns the result of subtracting this and {@code val}. If the result would be negative,
   * returns the low 32 bits of the result.
   */
  public UnsignedShort subtract(UnsignedShort val) {
    checkNotNull(val);
    return asUnsigned((short) (this.value - val.value));
  }

  /**
   * Returns the result of multiplying this and {@code val}. If the result would have more than 32
   * bits, returns the low 32 bits of the result.
   */
  @GwtIncompatible("Does not truncate correctly")
  public UnsignedShort multiply(UnsignedShort val) {
    checkNotNull(val);
    return asUnsigned((short) (value * val.value));
  }

  /**
   * Returns the result of dividing this by {@code val}.
   */
  public UnsignedShort divide(UnsignedShort val) {
    checkNotNull(val);
    return asUnsigned(UnsignedShorts.divide(value, val.value));
  }

  /**
   * Returns the remainder of dividing this by {@code val}.
   */
  public UnsignedShort remainder(UnsignedShort val) {
    checkNotNull(val);
    return asUnsigned(UnsignedShorts.remainder(value, val.value));
  }

  /**
   * Returns the value of this {@code UnsignedShort} as a {@code short}.
   */
  @Override
  public short shortValue() {
    return value;
  }

  /**
   * Returns the value of this {@code UnsignedShort} as an {@code int}.
   */
  @Override
  public int intValue() {
    return value & SHORT_MASK;
  }

  /**
   * Returns the value of this {@code UnsignedShort} as a {@code long}.
   */
  @Override
  public long longValue() {
    return intValue();
  }

  /**
   * Returns the value of this {@code UnsignedShort} as a {@code float}, analogous to a widening
   * primitive conversion from {@code int} to {@code float}, and correctly rounded.
   */
  @Override
  public float floatValue() {
    return intValue();
  }

  /**
   * Returns the value of this {@code UnsignedShort} as a {@code float}, analogous to a widening
   * primitive conversion from {@code int} to {@code double}, and correctly rounded.
   */
  @Override
  public double doubleValue() {
    return intValue();
  }

  /**
   * Returns the value of this {@code UnsignedShort} as a {@link BigInteger}.
   */
  public BigInteger bigIntegerValue() {
    return BigInteger.valueOf(intValue());
  }

  @Override
  public int compareTo(UnsignedShort other) {
    checkNotNull(other);
    return compare(value, other.value);
  }

  @Override
  public int hashCode() {
    return value;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj instanceof UnsignedShort) {
      UnsignedShort other = (UnsignedShort) obj;
      return value == other.value;
    }
    return false;
  }

  /**
   * Returns a string representation of the {@code UnsignedShort} value, in base 10.
   */
  @Override
  public String toString() {
    return toString(10);
  }

  /**
   * Returns a string representation of the {@code UnsignedShort} value, in base {@code radix}. If
   * {@code radix < Character.MIN_RADIX} or {@code radix > Character.MAX_RADIX}, the radix
   * {@code 10} is used.
   */
  public String toString(int radix) {
    return UnsignedShorts.toString(value, radix);
  }
}

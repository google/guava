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

import java.io.Serializable;
import java.math.BigInteger;

import javax.annotation.Nullable;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

/**
 * A wrapper class for unsigned {@code long} values, supporting arithmetic operations.
 *
 * <p>In some cases, when speed is more important than code readability, it may be faster simply to
 * treat primitive {@code long} values as unsigned, using the methods from {@link UnsignedLongs}.
 *
 * <p><b>Please do not extend this class; it will be made final in the near future.</b>
 *
 * @author Louis Wasserman
 * @author Colin Evans
 * @since 11.0
 */
@Beta
@GwtCompatible(serializable = true)
public class UnsignedLong extends Number implements Comparable<UnsignedLong>, Serializable {
  // TODO(user): make final as soon as util.UnsignedLong is migrated over

  private static final long UNSIGNED_MASK = 0x7fffffffffffffffL;

  public static final UnsignedLong ZERO = new UnsignedLong(0);
  public static final UnsignedLong ONE = new UnsignedLong(1);
  public static final UnsignedLong MAX_VALUE = new UnsignedLong(-1L);

  private final long value;

  protected UnsignedLong(long value) {
    this.value = value;
  }

  /**
   * Returns an {@code UnsignedLong} that, when treated as signed, is equal to {@code value}. The
   * inverse operation is {@link #longValue()}.
   *
   * <p>Put another way, if {@code value} is negative, the returned result will be equal to
   * {@code 2^64 + value}; otherwise, the returned result will be equal to {@code value}.
   */
  public static UnsignedLong asUnsigned(long value) {
    return new UnsignedLong(value);
  }

  /**
   * Returns a {@code UnsignedLong} representing the same value as the specified {@code BigInteger}
   * . This is the inverse operation of {@link #bigIntegerValue()}.
   *
   * @throws IllegalArgumentException if {@code value} is negative or {@code value >= 2^64}
   */
  public static UnsignedLong valueOf(BigInteger value) {
    checkNotNull(value);
    checkArgument(value.signum() >= 0 && value.bitLength() <= Long.SIZE,
        "value (%s) is outside the range for an unsigned long value", value);
    return asUnsigned(value.longValue());
  }

  /**
   * Returns an {@code UnsignedLong} holding the value of the specified {@code String}, parsed as
   * an unsigned {@code long} value.
   *
   * @throws NumberFormatException if the string does not contain a parsable unsigned {@code long}
   *         value
   */
  public static UnsignedLong valueOf(String string) {
    return valueOf(string, 10);
  }

  /**
   * Returns an {@code UnsignedLong} holding the value of the specified {@code String}, parsed as
   * an unsigned {@code long} value in the specified radix.
   *
   * @throws NumberFormatException if the string does not contain a parsable unsigned {@code long}
   *         value, or {@code radix} is not between {@link Character#MIN_RADIX} and
   *         {@link Character#MAX_RADIX}
   */
  public static UnsignedLong valueOf(String string, int radix) {
    return asUnsigned(UnsignedLongs.parseUnsignedLong(string, radix));
  }

  /**
   * Returns the result of adding this and {@code val}. If the result would have more than 64 bits,
   * returns the low 64 bits of the result.
   */
  public UnsignedLong add(UnsignedLong val) {
    checkNotNull(val);
    return asUnsigned(this.value + val.value);
  }

  /**
   * Returns the result of subtracting this and {@code val}. If the result would be negative,
   * returns the low 64 bits of the result.
   */
  public UnsignedLong subtract(UnsignedLong val) {
    checkNotNull(val);
    return asUnsigned(this.value - val.value);
  }

  /**
   * Returns the result of multiplying this and {@code val}. If the result would have more than 64
   * bits, returns the low 64 bits of the result.
   */
  public UnsignedLong multiply(UnsignedLong val) {
    checkNotNull(val);
    return asUnsigned(value * val.value);
  }

  /**
   * Returns the result of dividing this by {@code val}.
   */
  public UnsignedLong divide(UnsignedLong val) {
    checkNotNull(val);
    return asUnsigned(UnsignedLongs.divide(value, val.value));
  }

  /**
   * Returns the remainder of dividing this by {@code val}.
   */
  public UnsignedLong remainder(UnsignedLong val) {
    checkNotNull(val);
    return asUnsigned(UnsignedLongs.remainder(value, val.value));
  }

  /**
   * Returns the value of this {@code UnsignedLong} as an {@code int}.
   */
  @Override
  public int intValue() {
    return (int) value;
  }

  /**
   * Returns the value of this {@code UnsignedLong} as a {@code long}. This is an inverse operation
   * to {@link #asUnsigned}.
   *
   * <p>Note that if this {@code UnsignedLong} holds a value {@code >= 2^63}, the returned value
   * will be equal to {@code this - 2^64}.
   */
  @Override
  public long longValue() {
    return value;
  }

  /**
   * Returns the value of this {@code UnsignedLong} as a {@code float}, analogous to a widening
   * primitive conversion from {@code long} to {@code float}, and correctly rounded.
   */
  @Override
  public float floatValue() {
    @SuppressWarnings("cast")
    float fValue = (float) (value & UNSIGNED_MASK);
    if (value < 0) {
      fValue += 0x1.0p63f;
    }
    return fValue;
  }

  /**
   * Returns the value of this {@code UnsignedLong} as a {@code double}, analogous to a widening
   * primitive conversion from {@code long} to {@code double}, and correctly rounded.
   */
  @Override
  public double doubleValue() {
    @SuppressWarnings("cast")
    double dValue = (double) (value & UNSIGNED_MASK);
    if (value < 0) {
      dValue += 0x1.0p63;
    }
    return dValue;
  }

  /**
   * Returns the value of this {@code UnsignedLong} as a {@link BigInteger}.
   */
  public BigInteger bigIntegerValue() {
    BigInteger bigInt = BigInteger.valueOf(value & UNSIGNED_MASK);
    if (value < 0) {
      bigInt = bigInt.setBit(Long.SIZE - 1);
    }
    return bigInt;
  }

  @Override
  public int compareTo(UnsignedLong o) {
    checkNotNull(o);
    return UnsignedLongs.compare(value, o.value);
  }

  @Override
  public int hashCode() {
    return Longs.hashCode(value);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj instanceof UnsignedLong) {
      UnsignedLong other = (UnsignedLong) obj;
      return value == other.value;
    }
    return false;
  }

  /**
   * Returns a string representation of the {@code UnsignedLong} value, in base 10.
   */
  @Override
  public String toString() {
    return UnsignedLongs.toString(value);
  }

  /**
   * Returns a string representation of the {@code UnsignedLong} value, in base {@code radix}. If
   * {@code radix < Character.MIN_RADIX} or {@code radix > Character.MAX_RADIX}, the radix
   * {@code 10} is used.
   */
  public String toString(int radix) {
    return UnsignedLongs.toString(value, radix);
  }
}

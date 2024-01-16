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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;
import java.math.BigInteger;
import junit.framework.TestCase;

/**
 * Tests for {@code UnsignedLong}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class UnsignedLongTest extends TestCase {
  private static final ImmutableSet<Long> TEST_LONGS;
  private static final ImmutableSet<BigInteger> TEST_BIG_INTEGERS;

  static {
    ImmutableSet.Builder<Long> testLongsBuilder = ImmutableSet.builder();
    ImmutableSet.Builder<BigInteger> testBigIntegersBuilder = ImmutableSet.builder();

    // The values here look like 111...11101...010 in binary, where the initial 111...1110 takes
    // up exactly as many bits as can be represented in the significand (24 for float, 53 for
    // double). That final 0 should be rounded up to 1 because the remaining bits make that number
    // slightly nearer.
    long floatConversionTest = 0xfffffe8000000002L;
    long doubleConversionTest = 0xfffffffffffff402L;

    for (long i = -3; i <= 3; i++) {
      testLongsBuilder
          .add(i)
          .add(Long.MAX_VALUE + i)
          .add(Long.MIN_VALUE + i)
          .add(Integer.MIN_VALUE + i)
          .add(Integer.MAX_VALUE + i)
          .add(floatConversionTest + i)
          .add(doubleConversionTest + i);
      BigInteger bigI = BigInteger.valueOf(i);
      testBigIntegersBuilder
          .add(bigI)
          .add(BigInteger.valueOf(Long.MAX_VALUE).add(bigI))
          .add(BigInteger.valueOf(Long.MIN_VALUE).add(bigI))
          .add(BigInteger.valueOf(Integer.MAX_VALUE).add(bigI))
          .add(BigInteger.valueOf(Integer.MIN_VALUE).add(bigI))
          .add(BigInteger.ONE.shiftLeft(63).add(bigI))
          .add(BigInteger.ONE.shiftLeft(64).add(bigI));
    }
    TEST_LONGS = testLongsBuilder.build();
    TEST_BIG_INTEGERS = testBigIntegersBuilder.build();
  }

  public void testAsUnsignedAndLongValueAreInverses() {
    for (long value : TEST_LONGS) {
      assertWithMessage(UnsignedLongs.toString(value))
          .that(UnsignedLong.fromLongBits(value).longValue())
          .isEqualTo(value);
    }
  }

  public void testAsUnsignedBigIntegerValue() {
    for (long value : TEST_LONGS) {
      BigInteger expected =
          (value >= 0)
              ? BigInteger.valueOf(value)
              : BigInteger.valueOf(value).add(BigInteger.ZERO.setBit(64));
      assertWithMessage(UnsignedLongs.toString(value))
          .that(UnsignedLong.fromLongBits(value).bigIntegerValue())
          .isEqualTo(expected);
    }
  }

  public void testValueOfLong() {
    for (long value : TEST_LONGS) {
      boolean expectSuccess = value >= 0;
      try {
        assertThat(UnsignedLong.valueOf(value).longValue()).isEqualTo(value);
        assertThat(expectSuccess).isTrue();
      } catch (IllegalArgumentException e) {
        assertThat(expectSuccess).isFalse();
      }
    }
  }

  @J2ktIncompatible // TODO(b/285562794): Wrong result for j2kt
  public void testValueOfBigInteger() {
    BigInteger min = BigInteger.ZERO;
    BigInteger max = UnsignedLong.MAX_VALUE.bigIntegerValue();
    for (BigInteger big : TEST_BIG_INTEGERS) {
      boolean expectSuccess = big.compareTo(min) >= 0 && big.compareTo(max) <= 0;
      try {
        assertThat(UnsignedLong.valueOf(big).bigIntegerValue()).isEqualTo(big);
        assertThat(expectSuccess).isTrue();
      } catch (IllegalArgumentException e) {
        assertThat(expectSuccess).isFalse();
      }
    }
  }

  public void testToString() {
    for (long value : TEST_LONGS) {
      UnsignedLong unsignedValue = UnsignedLong.fromLongBits(value);
      assertThat(unsignedValue.toString()).isEqualTo(unsignedValue.bigIntegerValue().toString());
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // too slow
  public void testToStringRadix() {
    for (int radix = Character.MIN_RADIX; radix <= Character.MAX_RADIX; radix++) {
      for (long l : TEST_LONGS) {
        UnsignedLong value = UnsignedLong.fromLongBits(l);
        assertThat(value.toString(radix)).isEqualTo(value.bigIntegerValue().toString(radix));
      }
    }
  }

  public void testToStringRadixQuick() {
    int[] radices = {2, 3, 5, 7, 10, 12, 16, 21, 31, 36};
    for (int radix : radices) {
      for (long l : TEST_LONGS) {
        UnsignedLong value = UnsignedLong.fromLongBits(l);
        assertThat(value.toString(radix)).isEqualTo(value.bigIntegerValue().toString(radix));
      }
    }
  }

  @AndroidIncompatible // b/28251030, re-enable when the fix is everywhere we run this test
  public void testFloatValue() {
    for (long value : TEST_LONGS) {
      UnsignedLong unsignedValue = UnsignedLong.fromLongBits(value);
      assertWithMessage("Float value of " + unsignedValue)
          .that(unsignedValue.floatValue())
          .isWithin(0.0f)
          .of(unsignedValue.bigIntegerValue().floatValue());
    }
  }

  public void testDoubleValue() {
    for (long value : TEST_LONGS) {
      UnsignedLong unsignedValue = UnsignedLong.fromLongBits(value);
      assertWithMessage("Double value of " + unsignedValue)
          .that(unsignedValue.doubleValue())
          .isWithin(0.0)
          .of(unsignedValue.bigIntegerValue().doubleValue());
    }
  }

  @J2ktIncompatible // TODO(b/285562794): Wrong result for j2kt
  public void testPlus() {
    for (long a : TEST_LONGS) {
      for (long b : TEST_LONGS) {
        UnsignedLong aUnsigned = UnsignedLong.fromLongBits(a);
        UnsignedLong bUnsigned = UnsignedLong.fromLongBits(b);
        long expected = aUnsigned.bigIntegerValue().add(bUnsigned.bigIntegerValue()).longValue();
        UnsignedLong unsignedSum = aUnsigned.plus(bUnsigned);
        assertThat(unsignedSum.longValue()).isEqualTo(expected);
      }
    }
  }

  @J2ktIncompatible // TODO(b/285562794): Wrong result for j2kt
  public void testMinus() {
    for (long a : TEST_LONGS) {
      for (long b : TEST_LONGS) {
        UnsignedLong aUnsigned = UnsignedLong.fromLongBits(a);
        UnsignedLong bUnsigned = UnsignedLong.fromLongBits(b);
        long expected =
            aUnsigned.bigIntegerValue().subtract(bUnsigned.bigIntegerValue()).longValue();
        UnsignedLong unsignedSub = aUnsigned.minus(bUnsigned);
        assertThat(unsignedSub.longValue()).isEqualTo(expected);
      }
    }
  }

  @J2ktIncompatible // TODO(b/285562794): Wrong result for j2kt
  public void testTimes() {
    for (long a : TEST_LONGS) {
      for (long b : TEST_LONGS) {
        UnsignedLong aUnsigned = UnsignedLong.fromLongBits(a);
        UnsignedLong bUnsigned = UnsignedLong.fromLongBits(b);
        long expected =
            aUnsigned.bigIntegerValue().multiply(bUnsigned.bigIntegerValue()).longValue();
        UnsignedLong unsignedMul = aUnsigned.times(bUnsigned);
        assertThat(unsignedMul.longValue()).isEqualTo(expected);
      }
    }
  }

  @J2ktIncompatible // TODO(b/285562794): Wrong result for j2kt
  public void testDividedBy() {
    for (long a : TEST_LONGS) {
      for (long b : TEST_LONGS) {
        if (b != 0) {
          UnsignedLong aUnsigned = UnsignedLong.fromLongBits(a);
          UnsignedLong bUnsigned = UnsignedLong.fromLongBits(b);
          long expected =
              aUnsigned.bigIntegerValue().divide(bUnsigned.bigIntegerValue()).longValue();
          UnsignedLong unsignedDiv = aUnsigned.dividedBy(bUnsigned);
          assertThat(unsignedDiv.longValue()).isEqualTo(expected);
        }
      }
    }
  }

  public void testDivideByZeroThrows() {
    for (long a : TEST_LONGS) {
      try {
        UnsignedLong.fromLongBits(a).dividedBy(UnsignedLong.ZERO);
        fail("Expected ArithmeticException");
      } catch (ArithmeticException expected) {
      }
    }
  }

  public void testMod() {
    for (long a : TEST_LONGS) {
      for (long b : TEST_LONGS) {
        if (b != 0) {
          UnsignedLong aUnsigned = UnsignedLong.fromLongBits(a);
          UnsignedLong bUnsigned = UnsignedLong.fromLongBits(b);
          long expected =
              aUnsigned.bigIntegerValue().remainder(bUnsigned.bigIntegerValue()).longValue();
          UnsignedLong unsignedRem = aUnsigned.mod(bUnsigned);
          assertThat(unsignedRem.longValue()).isEqualTo(expected);
        }
      }
    }
  }

  public void testModByZero() {
    for (long a : TEST_LONGS) {
      try {
        UnsignedLong.fromLongBits(a).mod(UnsignedLong.ZERO);
        fail("Expected ArithmeticException");
      } catch (ArithmeticException expected) {
      }
    }
  }

  public void testCompare() {
    for (long a : TEST_LONGS) {
      for (long b : TEST_LONGS) {
        UnsignedLong aUnsigned = UnsignedLong.fromLongBits(a);
        UnsignedLong bUnsigned = UnsignedLong.fromLongBits(b);
        assertThat(aUnsigned.compareTo(bUnsigned))
            .isEqualTo(aUnsigned.bigIntegerValue().compareTo(bUnsigned.bigIntegerValue()));
      }
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // too slow
  public void testEquals() {
    EqualsTester equalsTester = new EqualsTester();
    for (long a : TEST_LONGS) {
      BigInteger big =
          (a >= 0) ? BigInteger.valueOf(a) : BigInteger.valueOf(a).add(BigInteger.ZERO.setBit(64));
      equalsTester.addEqualityGroup(
          UnsignedLong.fromLongBits(a),
          UnsignedLong.valueOf(big),
          UnsignedLong.valueOf(big.toString()),
          UnsignedLong.valueOf(big.toString(16), 16));
    }
    equalsTester.testEquals();
  }

  public void testIntValue() {
    for (long a : TEST_LONGS) {
      UnsignedLong aUnsigned = UnsignedLong.fromLongBits(a);
      int intValue = aUnsigned.bigIntegerValue().intValue();
      assertThat(aUnsigned.intValue()).isEqualTo(intValue);
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // serialization
  public void testSerialization() {
    for (long a : TEST_LONGS) {
      SerializableTester.reserializeAndAssert(UnsignedLong.fromLongBits(a));
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testNulls() {
    new NullPointerTester().testAllPublicStaticMethods(UnsignedLong.class);
  }
}

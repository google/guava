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
 * Tests for {@code UnsignedInteger}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class UnsignedIntegerTest extends TestCase {
  private static final ImmutableSet<Integer> TEST_INTS;
  private static final ImmutableSet<Long> TEST_LONGS;

  private static int force32(int value) {
    // GWT doesn't consistently overflow values to make them 32-bit, so we need to force it.
    return value & 0xffffffff;
  }

  static {
    ImmutableSet.Builder<Integer> testIntsBuilder = ImmutableSet.builder();
    ImmutableSet.Builder<Long> testLongsBuilder = ImmutableSet.builder();
    for (int i = -3; i <= 3; i++) {
      testIntsBuilder
          .add(i)
          .add(force32(Integer.MIN_VALUE + i))
          .add(force32(Integer.MAX_VALUE + i));
      testLongsBuilder
          .add((long) i)
          .add((long) Integer.MIN_VALUE + i)
          .add((long) Integer.MAX_VALUE + i)
          .add((1L << 32) + i);
    }
    TEST_INTS = testIntsBuilder.build();
    TEST_LONGS = testLongsBuilder.build();
  }

  public void testFromIntBitsAndIntValueAreInverses() {
    for (int value : TEST_INTS) {
      assertWithMessage(UnsignedInts.toString(value))
          .that(UnsignedInteger.fromIntBits(value).intValue())
          .isEqualTo(value);
    }
  }

  public void testFromIntBitsLongValue() {
    for (int value : TEST_INTS) {
      long expected = value & 0xffffffffL;
      assertWithMessage(UnsignedInts.toString(value))
          .that(UnsignedInteger.fromIntBits(value).longValue())
          .isEqualTo(expected);
    }
  }

  public void testValueOfLong() {
    long min = 0;
    long max = (1L << 32) - 1;
    for (long value : TEST_LONGS) {
      boolean expectSuccess = value >= min && value <= max;
      try {
        assertThat(UnsignedInteger.valueOf(value).longValue()).isEqualTo(value);
        assertThat(expectSuccess).isTrue();
      } catch (IllegalArgumentException e) {
        assertThat(expectSuccess).isFalse();
      }
    }
  }

  public void testValueOfBigInteger() {
    long min = 0;
    long max = (1L << 32) - 1;
    for (long value : TEST_LONGS) {
      boolean expectSuccess = value >= min && value <= max;
      try {
        assertThat(UnsignedInteger.valueOf(BigInteger.valueOf(value)).longValue()).isEqualTo(value);
        assertThat(expectSuccess).isTrue();
      } catch (IllegalArgumentException e) {
        assertThat(expectSuccess).isFalse();
      }
    }
  }

  public void testToString() {
    for (int value : TEST_INTS) {
      UnsignedInteger unsignedValue = UnsignedInteger.fromIntBits(value);
      assertThat(unsignedValue.toString()).isEqualTo(unsignedValue.bigIntegerValue().toString());
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // too slow
  public void testToStringRadix() {
    for (int radix = Character.MIN_RADIX; radix <= Character.MAX_RADIX; radix++) {
      for (int l : TEST_INTS) {
        UnsignedInteger value = UnsignedInteger.fromIntBits(l);
        assertThat(value.toString(radix)).isEqualTo(value.bigIntegerValue().toString(radix));
      }
    }
  }

  public void testToStringRadixQuick() {
    int[] radices = {2, 3, 5, 7, 10, 12, 16, 21, 31, 36};
    for (int radix : radices) {
      for (int l : TEST_INTS) {
        UnsignedInteger value = UnsignedInteger.fromIntBits(l);
        assertThat(value.toString(radix)).isEqualTo(value.bigIntegerValue().toString(radix));
      }
    }
  }

  public void testFloatValue() {
    for (int value : TEST_INTS) {
      UnsignedInteger unsignedValue = UnsignedInteger.fromIntBits(value);
      assertThat(unsignedValue.floatValue())
          .isEqualTo(unsignedValue.bigIntegerValue().floatValue());
    }
  }

  public void testDoubleValue() {
    for (int value : TEST_INTS) {
      UnsignedInteger unsignedValue = UnsignedInteger.fromIntBits(value);
      assertThat(unsignedValue.doubleValue())
          .isEqualTo(unsignedValue.bigIntegerValue().doubleValue());
    }
  }

  public void testPlus() {
    for (int a : TEST_INTS) {
      for (int b : TEST_INTS) {
        UnsignedInteger aUnsigned = UnsignedInteger.fromIntBits(a);
        UnsignedInteger bUnsigned = UnsignedInteger.fromIntBits(b);
        int expected = aUnsigned.bigIntegerValue().add(bUnsigned.bigIntegerValue()).intValue();
        UnsignedInteger unsignedSum = aUnsigned.plus(bUnsigned);
        assertThat(unsignedSum.intValue()).isEqualTo(expected);
      }
    }
  }

  public void testMinus() {
    for (int a : TEST_INTS) {
      for (int b : TEST_INTS) {
        UnsignedInteger aUnsigned = UnsignedInteger.fromIntBits(a);
        UnsignedInteger bUnsigned = UnsignedInteger.fromIntBits(b);
        int expected =
            force32(aUnsigned.bigIntegerValue().subtract(bUnsigned.bigIntegerValue()).intValue());
        UnsignedInteger unsignedSub = aUnsigned.minus(bUnsigned);
        assertThat(unsignedSub.intValue()).isEqualTo(expected);
      }
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // multiply
  public void testTimes() {
    for (int a : TEST_INTS) {
      for (int b : TEST_INTS) {
        UnsignedInteger aUnsigned = UnsignedInteger.fromIntBits(a);
        UnsignedInteger bUnsigned = UnsignedInteger.fromIntBits(b);
        int expected =
            force32(aUnsigned.bigIntegerValue().multiply(bUnsigned.bigIntegerValue()).intValue());
        UnsignedInteger unsignedMul = aUnsigned.times(bUnsigned);
        assertWithMessage(aUnsigned + " * " + bUnsigned)
            .that(unsignedMul.intValue())
            .isEqualTo(expected);
      }
    }
  }

  public void testDividedBy() {
    for (int a : TEST_INTS) {
      for (int b : TEST_INTS) {
        if (b != 0) {
          UnsignedInteger aUnsigned = UnsignedInteger.fromIntBits(a);
          UnsignedInteger bUnsigned = UnsignedInteger.fromIntBits(b);
          int expected = aUnsigned.bigIntegerValue().divide(bUnsigned.bigIntegerValue()).intValue();
          UnsignedInteger unsignedDiv = aUnsigned.dividedBy(bUnsigned);
          assertThat(unsignedDiv.intValue()).isEqualTo(expected);
        }
      }
    }
  }

  public void testDivideByZeroThrows() {
    for (int a : TEST_INTS) {
      try {
        UnsignedInteger unused = UnsignedInteger.fromIntBits(a).dividedBy(UnsignedInteger.ZERO);
        fail("Expected ArithmeticException");
      } catch (ArithmeticException expected) {
      }
    }
  }

  public void testMod() {
    for (int a : TEST_INTS) {
      for (int b : TEST_INTS) {
        if (b != 0) {
          UnsignedInteger aUnsigned = UnsignedInteger.fromIntBits(a);
          UnsignedInteger bUnsigned = UnsignedInteger.fromIntBits(b);
          int expected = aUnsigned.bigIntegerValue().mod(bUnsigned.bigIntegerValue()).intValue();
          UnsignedInteger unsignedRem = aUnsigned.mod(bUnsigned);
          assertThat(unsignedRem.intValue()).isEqualTo(expected);
        }
      }
    }
  }

  public void testModByZero() {
    for (int a : TEST_INTS) {
      try {
        UnsignedInteger.fromIntBits(a).mod(UnsignedInteger.ZERO);
        fail("Expected ArithmeticException");
      } catch (ArithmeticException expected) {
      }
    }
  }

  public void testCompare() {
    for (int a : TEST_INTS) {
      for (int b : TEST_INTS) {
        UnsignedInteger aUnsigned = UnsignedInteger.fromIntBits(a);
        UnsignedInteger bUnsigned = UnsignedInteger.fromIntBits(b);
        assertThat(aUnsigned.compareTo(bUnsigned))
            .isEqualTo(aUnsigned.bigIntegerValue().compareTo(bUnsigned.bigIntegerValue()));
      }
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // too slow
  public void testEquals() {
    EqualsTester equalsTester = new EqualsTester();
    for (int a : TEST_INTS) {
      long value = a & 0xffffffffL;
      equalsTester.addEqualityGroup(
          UnsignedInteger.fromIntBits(a),
          UnsignedInteger.valueOf(value),
          UnsignedInteger.valueOf(Long.toString(value)),
          UnsignedInteger.valueOf(Long.toString(value, 16), 16));
    }
    equalsTester.testEquals();
  }

  public void testIntValue() {
    for (int a : TEST_INTS) {
      UnsignedInteger aUnsigned = UnsignedInteger.fromIntBits(a);
      int intValue = aUnsigned.bigIntegerValue().intValue();
      assertThat(aUnsigned.intValue()).isEqualTo(intValue);
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // serialization
  public void testSerialization() {
    for (int a : TEST_INTS) {
      SerializableTester.reserializeAndAssert(UnsignedInteger.fromIntBits(a));
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testNulls() {
    new NullPointerTester().testAllPublicStaticMethods(UnsignedInteger.class);
  }
}

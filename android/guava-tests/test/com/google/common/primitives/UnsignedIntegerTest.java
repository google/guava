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

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
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
      assertEquals(
          UnsignedInts.toString(value), value, UnsignedInteger.fromIntBits(value).intValue());
    }
  }

  public void testFromIntBitsLongValue() {
    for (int value : TEST_INTS) {
      long expected = value & 0xffffffffL;
      assertEquals(
          UnsignedInts.toString(value), expected, UnsignedInteger.fromIntBits(value).longValue());
    }
  }

  public void testValueOfLong() {
    long min = 0;
    long max = (1L << 32) - 1;
    for (long value : TEST_LONGS) {
      boolean expectSuccess = value >= min && value <= max;
      try {
        assertEquals(value, UnsignedInteger.valueOf(value).longValue());
        assertTrue(expectSuccess);
      } catch (IllegalArgumentException e) {
        assertFalse(expectSuccess);
      }
    }
  }

  public void testValueOfBigInteger() {
    long min = 0;
    long max = (1L << 32) - 1;
    for (long value : TEST_LONGS) {
      boolean expectSuccess = value >= min && value <= max;
      try {
        assertEquals(value, UnsignedInteger.valueOf(BigInteger.valueOf(value)).longValue());
        assertTrue(expectSuccess);
      } catch (IllegalArgumentException e) {
        assertFalse(expectSuccess);
      }
    }
  }

  public void testToString() {
    for (int value : TEST_INTS) {
      UnsignedInteger unsignedValue = UnsignedInteger.fromIntBits(value);
      assertEquals(unsignedValue.bigIntegerValue().toString(), unsignedValue.toString());
    }
  }

  @GwtIncompatible // too slow
  public void testToStringRadix() {
    for (int radix = Character.MIN_RADIX; radix <= Character.MAX_RADIX; radix++) {
      for (int l : TEST_INTS) {
        UnsignedInteger value = UnsignedInteger.fromIntBits(l);
        assertEquals(value.bigIntegerValue().toString(radix), value.toString(radix));
      }
    }
  }

  public void testToStringRadixQuick() {
    int[] radices = {2, 3, 5, 7, 10, 12, 16, 21, 31, 36};
    for (int radix : radices) {
      for (int l : TEST_INTS) {
        UnsignedInteger value = UnsignedInteger.fromIntBits(l);
        assertEquals(value.bigIntegerValue().toString(radix), value.toString(radix));
      }
    }
  }

  public void testFloatValue() {
    for (int value : TEST_INTS) {
      UnsignedInteger unsignedValue = UnsignedInteger.fromIntBits(value);
      assertEquals(unsignedValue.bigIntegerValue().floatValue(), unsignedValue.floatValue());
    }
  }

  public void testDoubleValue() {
    for (int value : TEST_INTS) {
      UnsignedInteger unsignedValue = UnsignedInteger.fromIntBits(value);
      assertEquals(unsignedValue.bigIntegerValue().doubleValue(), unsignedValue.doubleValue());
    }
  }

  public void testPlus() {
    for (int a : TEST_INTS) {
      for (int b : TEST_INTS) {
        UnsignedInteger aUnsigned = UnsignedInteger.fromIntBits(a);
        UnsignedInteger bUnsigned = UnsignedInteger.fromIntBits(b);
        int expected = aUnsigned.bigIntegerValue().add(bUnsigned.bigIntegerValue()).intValue();
        UnsignedInteger unsignedSum = aUnsigned.plus(bUnsigned);
        assertEquals(expected, unsignedSum.intValue());
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
        assertEquals(expected, unsignedSub.intValue());
      }
    }
  }

  @GwtIncompatible // multiply
  public void testTimes() {
    for (int a : TEST_INTS) {
      for (int b : TEST_INTS) {
        UnsignedInteger aUnsigned = UnsignedInteger.fromIntBits(a);
        UnsignedInteger bUnsigned = UnsignedInteger.fromIntBits(b);
        int expected =
            force32(aUnsigned.bigIntegerValue().multiply(bUnsigned.bigIntegerValue()).intValue());
        UnsignedInteger unsignedMul = aUnsigned.times(bUnsigned);
        assertEquals(aUnsigned + " * " + bUnsigned, expected, unsignedMul.intValue());
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
          assertEquals(expected, unsignedDiv.intValue());
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
          assertEquals(expected, unsignedRem.intValue());
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
        assertEquals(
            aUnsigned.bigIntegerValue().compareTo(bUnsigned.bigIntegerValue()),
            aUnsigned.compareTo(bUnsigned));
      }
    }
  }

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
      assertEquals(intValue, aUnsigned.intValue());
    }
  }

  @GwtIncompatible // serialization
  public void testSerialization() {
    for (int a : TEST_INTS) {
      SerializableTester.reserializeAndAssert(UnsignedInteger.fromIntBits(a));
    }
  }

  @GwtIncompatible // NullPointerTester
  public void testNulls() {
    new NullPointerTester().testAllPublicStaticMethods(UnsignedInteger.class);
  }
}

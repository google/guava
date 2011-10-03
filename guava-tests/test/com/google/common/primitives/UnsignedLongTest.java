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

import java.math.BigInteger;

import junit.framework.TestCase;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;

/**
 * Tests for {@code UnsignedLong}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class UnsignedLongTest extends TestCase {
  private static final ImmutableSet<Long> TEST_LONGS;

  static {
    ImmutableSet.Builder<Long> testLongsBuilder = ImmutableSet.builder();
    for (long i = -3; i <= 3; i++) {
      testLongsBuilder
          .add(i)
          .add(Long.MAX_VALUE + i)
          .add(Long.MIN_VALUE + i)
          .add(Integer.MIN_VALUE + i)
          .add(Integer.MAX_VALUE + i);
    }
    TEST_LONGS = testLongsBuilder.build();
  }

  public void testAsUnsignedAndLongValueAreInverses() {
    for (long value : TEST_LONGS) {
      assertEquals(
          UnsignedLongs.toString(value), value, UnsignedLong.asUnsigned(value).longValue());
    }
  }

  public void testAsUnsignedBigIntegerValue() {
    for (long value : TEST_LONGS) {
      BigInteger expected = (value >= 0)
          ? BigInteger.valueOf(value)
          : BigInteger.valueOf(value).add(BigInteger.ZERO.setBit(64));
      assertEquals(UnsignedLongs.toString(value), expected,
          UnsignedLong.asUnsigned(value).bigIntegerValue());
    }
  }

  public void testToString() {
    for (long value : TEST_LONGS) {
      UnsignedLong unsignedValue = UnsignedLong.asUnsigned(value);
      assertEquals(unsignedValue.bigIntegerValue().toString(), unsignedValue.toString());
    }
  }

  @GwtIncompatible("too slow")
  public void testToStringRadix() {
    for (int radix = Character.MIN_RADIX; radix <= Character.MAX_RADIX; radix++) {
      for (long l : TEST_LONGS) {
        UnsignedLong value = UnsignedLong.asUnsigned(l);
        assertEquals(value.bigIntegerValue().toString(radix), value.toString(radix));
      }
    }
  }

  public void testToStringRadixQuick() {
    int[] radices = {2, 3, 5, 7, 10, 12, 16, 21, 31, 36};
    for (int radix : radices) {
      for (long l : TEST_LONGS) {
        UnsignedLong value = UnsignedLong.asUnsigned(l);
        assertEquals(value.bigIntegerValue().toString(radix), value.toString(radix));
      }
    }
  }

  public void testFloatValue() {
    for (long value : TEST_LONGS) {
      UnsignedLong unsignedValue = UnsignedLong.asUnsigned(value);
      assertEquals(unsignedValue.bigIntegerValue().floatValue(), unsignedValue.floatValue());
    }
  }

  public void testDoubleValue() {
    for (long value : TEST_LONGS) {
      UnsignedLong unsignedValue = UnsignedLong.asUnsigned(value);
      assertEquals(unsignedValue.bigIntegerValue().doubleValue(), unsignedValue.doubleValue());
    }
  }

  public void testAdd() {
    for (long a : TEST_LONGS) {
      for (long b : TEST_LONGS) {
        UnsignedLong aUnsigned = UnsignedLong.asUnsigned(a);
        UnsignedLong bUnsigned = UnsignedLong.asUnsigned(b);
        long expected = aUnsigned
            .bigIntegerValue()
            .add(bUnsigned.bigIntegerValue())
            .longValue();
        UnsignedLong unsignedSum = aUnsigned.add(bUnsigned);
        assertEquals(expected, unsignedSum.longValue());
      }
    }
  }

  public void testSubtract() {
    for (long a : TEST_LONGS) {
      for (long b : TEST_LONGS) {
        UnsignedLong aUnsigned = UnsignedLong.asUnsigned(a);
        UnsignedLong bUnsigned = UnsignedLong.asUnsigned(b);
        long expected = aUnsigned
            .bigIntegerValue()
            .subtract(bUnsigned.bigIntegerValue())
            .longValue();
        UnsignedLong unsignedSub = aUnsigned.subtract(bUnsigned);
        assertEquals(expected, unsignedSub.longValue());
      }
    }
  }

  public void testMultiply() {
    for (long a : TEST_LONGS) {
      for (long b : TEST_LONGS) {
        UnsignedLong aUnsigned = UnsignedLong.asUnsigned(a);
        UnsignedLong bUnsigned = UnsignedLong.asUnsigned(b);
        long expected = aUnsigned
            .bigIntegerValue()
            .multiply(bUnsigned.bigIntegerValue())
            .longValue();
        UnsignedLong unsignedMul = aUnsigned.multiply(bUnsigned);
        assertEquals(expected, unsignedMul.longValue());
      }
    }
  }

  public void testDivide() {
    for (long a : TEST_LONGS) {
      for (long b : TEST_LONGS) {
        if (b != 0) {
          UnsignedLong aUnsigned = UnsignedLong.asUnsigned(a);
          UnsignedLong bUnsigned = UnsignedLong.asUnsigned(b);
          long expected = aUnsigned
              .bigIntegerValue()
              .divide(bUnsigned.bigIntegerValue())
              .longValue();
          UnsignedLong unsignedDiv = aUnsigned.divide(bUnsigned);
          assertEquals(expected, unsignedDiv.longValue());
        }
      }
    }
  }

  public void testDivideByZeroThrows() {
    for (long a : TEST_LONGS) {
      try {
        UnsignedLong.asUnsigned(a).divide(UnsignedLong.ZERO);
        fail("Expected ArithmeticException");
      } catch (ArithmeticException expected) {}
    }
  }

  public void testRemainder() {
    for (long a : TEST_LONGS) {
      for (long b : TEST_LONGS) {
        if (b != 0) {
          UnsignedLong aUnsigned = UnsignedLong.asUnsigned(a);
          UnsignedLong bUnsigned = UnsignedLong.asUnsigned(b);
          long expected = aUnsigned
              .bigIntegerValue()
              .remainder(bUnsigned.bigIntegerValue())
              .longValue();
          UnsignedLong unsignedRem = aUnsigned.remainder(bUnsigned);
          assertEquals(expected, unsignedRem.longValue());
        }
      }
    }
  }

  public void testRemainderByZero() {
    for (long a : TEST_LONGS) {
      try {
        UnsignedLong.asUnsigned(a).remainder(UnsignedLong.ZERO);
        fail("Expected ArithmeticException");
      } catch (ArithmeticException expected) {}
    }
  }

  public void testCompare() {
    for (long a : TEST_LONGS) {
      for (long b : TEST_LONGS) {
        UnsignedLong aUnsigned = UnsignedLong.asUnsigned(a);
        UnsignedLong bUnsigned = UnsignedLong.asUnsigned(b);
        assertEquals(aUnsigned.bigIntegerValue().compareTo(bUnsigned.bigIntegerValue()),
            aUnsigned.compareTo(bUnsigned));
      }
    }
  }

  @GwtIncompatible("too slow")
  public void testEqualsAndValueOf() {
    EqualsTester equalsTester = new EqualsTester();
    for (long a : TEST_LONGS) {
      BigInteger big =
          (a >= 0) ? BigInteger.valueOf(a) : BigInteger.valueOf(a).add(BigInteger.ZERO.setBit(64));
      equalsTester.addEqualityGroup(UnsignedLong.asUnsigned(a), UnsignedLong.valueOf(big),
          UnsignedLong.valueOf(big.toString()), UnsignedLong.valueOf(big.toString(16), 16));
    }
    equalsTester.testEquals();
  }

  public void testIntValue() {
    for (long a : TEST_LONGS) {
      UnsignedLong aUnsigned = UnsignedLong.asUnsigned(a);
      int intValue = aUnsigned.bigIntegerValue().intValue();
      assertEquals(intValue, aUnsigned.intValue());
    }
  }

  @GwtIncompatible("serialization")
  public void testSerialization() {
    for (long a : TEST_LONGS) {
      SerializableTester.reserializeAndAssert(UnsignedLong.asUnsigned(a));
    }
  }

  @GwtIncompatible("NullPointerTester")
  public void testNulls() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.setDefault(UnsignedLong.class, UnsignedLong.ONE);
    tester.testAllPublicStaticMethods(UnsignedLong.class);
  }
}

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

import junit.framework.TestCase;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;

/**
 * Tests for {@code UnsignedShort}.
 * 
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class UnsignedShortTest extends TestCase {
  private static final ImmutableSet<Short> TEST_SHORTS;

  private static short force16(short value) {
    // GWT doesn't overflow values to make them 32-bit, so we need to force it.
    return (short) (value & 0xffff);
  }

  static {
    ImmutableSet.Builder<Short> testShortsBuilder = ImmutableSet.builder();
    for (short i = -3; i <= 3; i++) {
      testShortsBuilder.add(i).add((short) -i).add(force16((short) (Short.MIN_VALUE + i)))
          .add(force16((short) (Short.MAX_VALUE + i)));
    }
    TEST_SHORTS = testShortsBuilder.build();
  }

  public void testAsUnsignedAndShortValueAreInverses() {
    for (short value : TEST_SHORTS) {
      assertEquals(UnsignedShorts.toString(value), value, UnsignedShort.asUnsigned(value)
          .shortValue());
    }
  }

  public void testAsUnsignedShortValue() {
    for (short value : TEST_SHORTS) {
      short expected = (short) (value & 0xffff);
      assertEquals(UnsignedShorts.toString(value), expected, UnsignedShort.asUnsigned(value)
          .shortValue());
    }
  }

  public void testToString() {
    for (short value : TEST_SHORTS) {
      UnsignedShort unsignedValue = UnsignedShort.asUnsigned(value);
      assertEquals(unsignedValue.bigIntegerValue().toString(), unsignedValue.toString());
    }
  }

  @GwtIncompatible("too slow")
  public void testToStringRadix() {
    for (int radix = Character.MIN_RADIX; radix <= Character.MAX_RADIX; radix++) {
      for (short l : TEST_SHORTS) {
        UnsignedShort value = UnsignedShort.asUnsigned(l);
        assertEquals(value.bigIntegerValue().toString(radix), value.toString(radix));
      }
    }
  }

  public void testToStringRadixQuick() {
    int[] radices = {2, 3, 5, 7, 10, 12, 16, 21, 31, 36};
    for (int radix : radices) {
      for (short l : TEST_SHORTS) {
        UnsignedShort value = UnsignedShort.asUnsigned(l);
        assertEquals(value.bigIntegerValue().toString(radix), value.toString(radix));
      }
    }
  }

  public void testFloatValue() {
    for (short value : TEST_SHORTS) {
      UnsignedShort unsignedValue = UnsignedShort.asUnsigned(value);
      assertEquals(unsignedValue.bigIntegerValue().floatValue(), unsignedValue.floatValue());
    }
  }

  public void testDoubleValue() {
    for (short value : TEST_SHORTS) {
      UnsignedShort unsignedValue = UnsignedShort.asUnsigned(value);
      assertEquals(unsignedValue.bigIntegerValue().doubleValue(), unsignedValue.doubleValue());
    }
  }

  public void testAdd() {
    for (short a : TEST_SHORTS) {
      for (short b : TEST_SHORTS) {
        UnsignedShort aUnsigned = UnsignedShort.asUnsigned(a);
        UnsignedShort bUnsigned = UnsignedShort.asUnsigned(b);
        short expected = aUnsigned.bigIntegerValue().add(bUnsigned.bigIntegerValue()).shortValue();
        UnsignedShort unsignedSum = aUnsigned.add(bUnsigned);
        assertEquals(expected, unsignedSum.shortValue());
      }
    }
  }

  public void testSubtract() {
    for (short a : TEST_SHORTS) {
      for (short b : TEST_SHORTS) {
        UnsignedShort aUnsigned = UnsignedShort.asUnsigned(a);
        UnsignedShort bUnsigned = UnsignedShort.asUnsigned(b);
        short expected =
            force16(aUnsigned.bigIntegerValue().subtract(bUnsigned.bigIntegerValue()).shortValue());
        UnsignedShort unsignedSub = aUnsigned.subtract(bUnsigned);
        assertEquals(expected, unsignedSub.shortValue());
      }
    }
  }

  @GwtIncompatible("multiply")
  public void testMultiply() {
    for (short a : TEST_SHORTS) {
      for (short b : TEST_SHORTS) {
        UnsignedShort aUnsigned = UnsignedShort.asUnsigned(a);
        UnsignedShort bUnsigned = UnsignedShort.asUnsigned(b);
        short expected =
            force16(aUnsigned.bigIntegerValue().multiply(bUnsigned.bigIntegerValue()).shortValue());
        UnsignedShort unsignedMul = aUnsigned.multiply(bUnsigned);
        assertEquals(aUnsigned + " * " + bUnsigned, expected, unsignedMul.shortValue());
      }
    }
  }

  public void testDivide() {
    for (short a : TEST_SHORTS) {
      for (short b : TEST_SHORTS) {
        if (b != 0) {
          UnsignedShort aUnsigned = UnsignedShort.asUnsigned(a);
          UnsignedShort bUnsigned = UnsignedShort.asUnsigned(b);
          short expected =
              aUnsigned.bigIntegerValue().divide(bUnsigned.bigIntegerValue()).shortValue();
          UnsignedShort unsignedDiv = aUnsigned.divide(bUnsigned);
          assertEquals(expected, unsignedDiv.shortValue());
        }
      }
    }
  }

  public void testDivideByZeroThrows() {
    for (short a : TEST_SHORTS) {
      try {
        UnsignedShort.asUnsigned(a).divide(UnsignedShort.ZERO);
        fail("Expected ArithmeticException");
      } catch (ArithmeticException expected) {}
    }
  }

  public void testRemainder() {
    for (short a : TEST_SHORTS) {
      for (short b : TEST_SHORTS) {
        if (b != 0) {
          UnsignedShort aUnsigned = UnsignedShort.asUnsigned(a);
          UnsignedShort bUnsigned = UnsignedShort.asUnsigned(b);
          short expected =
              aUnsigned.bigIntegerValue().remainder(bUnsigned.bigIntegerValue()).shortValue();
          UnsignedShort unsignedRem = aUnsigned.remainder(bUnsigned);
          assertEquals(expected, unsignedRem.shortValue());
        }
      }
    }
  }

  public void testRemainderByZero() {
    for (short a : TEST_SHORTS) {
      try {
        UnsignedShort.asUnsigned(a).remainder(UnsignedShort.ZERO);
        fail("Expected ArithmeticException");
      } catch (ArithmeticException expected) {}
    }
  }

  public void testCompare() {
    for (short a : TEST_SHORTS) {
      for (short b : TEST_SHORTS) {
        UnsignedShort aUnsigned = UnsignedShort.asUnsigned(a);
        UnsignedShort bUnsigned = UnsignedShort.asUnsigned(b);
        assertEquals(Integer.signum(aUnsigned.bigIntegerValue().compareTo(bUnsigned.bigIntegerValue())),
            Integer.signum(aUnsigned.compareTo(bUnsigned)));
      }
    }
  }

  @GwtIncompatible("too slow")
  public void testEqualsAndValueOf() {
    EqualsTester equalsTester = new EqualsTester();
    for (short a : TEST_SHORTS) {
      int value = a & 0xffff;
      equalsTester.addEqualityGroup(UnsignedShort.asUnsigned(a), UnsignedShort.valueOf(value),
          UnsignedShort.valueOf(Integer.toString(value)),
          UnsignedShort.valueOf(Integer.toString(value, 16), 16));
    }
    equalsTester.testEquals();
  }

  public void testIntValue() {
    for (short a : TEST_SHORTS) {
      UnsignedShort aUnsigned = UnsignedShort.asUnsigned(a);
      int intValue = aUnsigned.bigIntegerValue().intValue();
      assertEquals(intValue, aUnsigned.intValue());
    }
  }

  @GwtIncompatible("serialization")
  public void testSerialization() {
    for (short a : TEST_SHORTS) {
      SerializableTester.reserializeAndAssert(UnsignedShort.asUnsigned(a));
    }
  }

  @GwtIncompatible("NullPointerTester")
  public void testNulls() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.setDefault(UnsignedShort.class, UnsignedShort.ONE);
    tester.testAllPublicStaticMethods(UnsignedShort.class);
  }
}

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

import java.util.Random;

import junit.framework.TestCase;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.testing.NullPointerTester;

/**
 * Tests for UnsignedShorts
 * 
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class UnsignedShortsTest extends TestCase {
  private static final int[] UNSIGNED_SHORTS = { 0, 1, 2, 3, 0x1234, 0x5a43, 0x6cf7, 0xff1a,
      0xfffd, 0xfffe, 0xffff };

  public void testToLong() {
    for (int a : UNSIGNED_SHORTS) {
      assertEquals(a, UnsignedShorts.toInt((short) a));
    }
  }

  public void testCompare() {
    for (int a : UNSIGNED_SHORTS) {
      for (int b : UNSIGNED_SHORTS) {
        int cmpAsInts = Ints.compare(a, b);
        int cmpAsUInt = UnsignedShorts.compare((short) a, (short) b);
        assertEquals(Integer.signum(cmpAsInts), Integer.signum(cmpAsUInt));
      }
    }
  }

  public void testDivide() {
    for (int a : UNSIGNED_SHORTS) {
      for (int b : UNSIGNED_SHORTS) {
        try {
          assertEquals((short) (a / b), UnsignedShorts.divide((short) a, (short) b));
          assertFalse(b == 0);
        } catch (ArithmeticException e) {
          assertEquals(0, b);
        }
      }
    }
  }

  public void testRemainder() {
    for (int a : UNSIGNED_SHORTS) {
      for (int b : UNSIGNED_SHORTS) {
        try {
          assertEquals((short) (a % b), UnsignedShorts.remainder((short) a, (short) b));
          assertFalse(b == 0);
        } catch (ArithmeticException e) {
          assertEquals(0, b);
        }
      }
    }
  }

  @GwtIncompatible("Too slow in GWT (~3min fully optimized)")
  public void testDivideRemainderEuclideanProperty() {
    // Use a seed so that the test is deterministic:
    Random r = new Random(0L);
    for (int i = 0; i < 1000000; i++) {
      short dividend = (short) r.nextInt();
      short divisor = (short) r.nextInt();
      if (divisor == 0) {
        continue;
      }
      // Test that the Euclidean property is preserved:
      short result = (short) (divisor * UnsignedShorts.divide(dividend, divisor));
      result += UnsignedShorts.remainder(dividend, divisor);
      assertEquals(dividend, result);
    }
  }

  public void testParseInt() {
    try {
      for (int a : UNSIGNED_SHORTS) {
        assertEquals((short) a, UnsignedShorts.parseUnsignedShort(Integer.toString(a)));
      }
    } catch (NumberFormatException e) {
      fail(e.getMessage());
    }

    try {
      UnsignedShorts.parseUnsignedShort(Integer.toString(1 << 16));
      fail("Expected NumberFormatException");
    } catch (NumberFormatException expected) {
    }
  }

  public void testParseLongWithRadix() throws NumberFormatException {
    for (int a : UNSIGNED_SHORTS) {
      for (int radix = Character.MIN_RADIX; radix <= Character.MAX_RADIX; radix++) {
        assertEquals((short) a,
            UnsignedShorts.parseUnsignedShort(Integer.toString(a, radix), radix));
      }
    }

    // loops through all legal radix values.
    for (int radix = Character.MIN_RADIX; radix <= Character.MAX_RADIX; radix++) {
      // tests can successfully parse a number string with this radix.
      String maxAsString = Integer.toString((1 << 16) - 1, radix);
      assertEquals((short) -1, UnsignedShorts.parseUnsignedShort(maxAsString, radix));

      try {
        // tests that we get exception whre an overflow would occur.
        int overflow = 1 << 16;
        String overflowAsString = Integer.toString(overflow, radix);
        UnsignedShorts.parseUnsignedShort(overflowAsString, radix);
        fail();
      } catch (NumberFormatException expected) {
      }
    }
  }

  public void testParseLongThrowsExceptionForInvalidRadix() {
    // Valid radix values are Character.MIN_RADIX to Character.MAX_RADIX,
    // inclusive.
    try {
      UnsignedShorts.parseUnsignedShort("0", Character.MIN_RADIX - 1);
      fail();
    } catch (NumberFormatException expected) {
    }

    try {
      UnsignedShorts.parseUnsignedShort("0", Character.MAX_RADIX + 1);
      fail();
    } catch (NumberFormatException expected) {
    }

    // The radix is used as an array index, so try a negative value.
    try {
      UnsignedShorts.parseUnsignedShort("0", -1);
      fail();
    } catch (NumberFormatException expected) {
    }
  }

  public void testToString() {
    int[] bases = { 2, 5, 7, 8, 10, 16 };
    for (int a : UNSIGNED_SHORTS) {
      for (int base : bases) {
        assertEquals(UnsignedShorts.toString((short) a, base), Long.toString(a, base));
      }
    }
  }

  @GwtIncompatible("NullPointerTester")
  public void testNulls() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.setDefault(short[].class, new short[0]);
    tester.testAllPublicStaticMethods(UnsignedShorts.class);
  }
}

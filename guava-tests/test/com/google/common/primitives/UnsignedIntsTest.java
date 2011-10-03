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
 * Tests for UnsignedInts
 * 
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class UnsignedIntsTest extends TestCase {
  private static final long[] UNSIGNED_INTS = {
      0L,
      1L,
      2L,
      3L,
      0x12345678L,
      0x5a4316b8L,
      0x6cf78a4bL,
      0xff1a618bL,
      0xfffffffdL,
      0xfffffffeL,
      0xffffffffL};

  public void testToLong() {
    for (long a : UNSIGNED_INTS) {
      assertEquals(a, UnsignedInts.toLong((int) a));
    }
  }

  public void testCompare() {
    for (long a : UNSIGNED_INTS) {
      for (long b : UNSIGNED_INTS) {
        int cmpAsLongs = Longs.compare(a, b);
        int cmpAsUInt = UnsignedInts.compare((int) a, (int) b);
        assertEquals(Integer.signum(cmpAsLongs), Integer.signum(cmpAsUInt));
      }
    }
  }

  public void testDivide() {
    for (long a : UNSIGNED_INTS) {
      for (long b : UNSIGNED_INTS) {
        try {
          assertEquals((int) (a / b), UnsignedInts.divide((int) a, (int) b));
          assertFalse(b == 0);
        } catch (ArithmeticException e) {
          assertEquals(0, b);
        }
      }
    }
  }

  public void testRemainder() {
    for (long a : UNSIGNED_INTS) {
      for (long b : UNSIGNED_INTS) {
        try {
          assertEquals((int) (a % b), UnsignedInts.remainder((int) a, (int) b));
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
      int dividend = r.nextInt();
      int divisor = r.nextInt();
      // Test that the Euclidean property is preserved:
      assertTrue(dividend
          - (divisor * UnsignedInts.divide(dividend, divisor) + UnsignedInts.remainder(dividend,
              divisor)) == 0);
    }
  }

  public void testParseInt() {
    try {
      for (long a : UNSIGNED_INTS) {
        assertEquals((int) a, UnsignedInts.parseUnsignedInt(Long.toString(a)));
      }
    } catch (NumberFormatException e) {
      fail(e.getMessage());
    }

    try {
      UnsignedInts.parseUnsignedInt(Long.toString(1L << 32));
      fail("Expected NumberFormatException");
    } catch (NumberFormatException expected) {}
  }

  public void testParseLongWithRadix() throws NumberFormatException {
    for (long a : UNSIGNED_INTS) {
      for (int radix = Character.MIN_RADIX; radix <= Character.MAX_RADIX; radix++) {
        assertEquals((int) a, UnsignedInts.parseUnsignedInt(Long.toString(a, radix), radix));
      }
    }

    // loops through all legal radix values.
    for (int radix = Character.MIN_RADIX; radix <= Character.MAX_RADIX; radix++) {
      // tests can successfully parse a number string with this radix.
      String maxAsString = Long.toString((1L << 32) - 1, radix);
      assertEquals(-1, UnsignedInts.parseUnsignedInt(maxAsString, radix));

      try {
        // tests that we get exception whre an overflow would occur.
        long overflow = 1L << 32;
        String overflowAsString = Long.toString(overflow, radix);
        UnsignedInts.parseUnsignedInt(overflowAsString, radix);
        fail();
      } catch (NumberFormatException expected) {}
    }
  }

  public void testParseLongThrowsExceptionForInvalidRadix() {
    // Valid radix values are Character.MIN_RADIX to Character.MAX_RADIX,
    // inclusive.
    try {
      UnsignedInts.parseUnsignedInt("0", Character.MIN_RADIX - 1);
      fail();
    } catch (NumberFormatException expected) {}

    try {
      UnsignedInts.parseUnsignedInt("0", Character.MAX_RADIX + 1);
      fail();
    } catch (NumberFormatException expected) {}

    // The radix is used as an array index, so try a negative value.
    try {
      UnsignedInts.parseUnsignedInt("0", -1);
      fail();
    } catch (NumberFormatException expected) {}
  }

  public void testToString() {
    int[] bases = {2, 5, 7, 8, 10, 16};
    for (long a : UNSIGNED_INTS) {
      for (int base : bases) {
        assertEquals(UnsignedInts.toString((int) a, base), Long.toString(a, base));
      }
    }
  }

  @GwtIncompatible("NullPointerTester")
  public void testNulls() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.setDefault(int[].class, new int[0]);
    tester.testAllPublicStaticMethods(UnsignedInts.class);
  }
}

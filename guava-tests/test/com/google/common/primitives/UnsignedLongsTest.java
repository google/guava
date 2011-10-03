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

import static java.math.BigInteger.ONE;

import java.math.BigInteger;
import java.util.Random;

import junit.framework.TestCase;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.testing.NullPointerTester;

/**
 * Tests for UnsignedLongs
 * 
 * @author Brian Milch
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class UnsignedLongsTest extends TestCase {

  public void testCompare() {
    // max value
    assertTrue((UnsignedLongs.compare(0, 0xffffffffffffffffL) < 0));
    assertTrue((UnsignedLongs.compare(0xffffffffffffffffL, 0) > 0));

    // both with high bit set
    assertTrue((UnsignedLongs.compare(0xff1a618b7f65ea12L, 0xffffffffffffffffL) < 0));
    assertTrue((UnsignedLongs.compare(0xffffffffffffffffL, 0xff1a618b7f65ea12L) > 0));

    // one with high bit set
    assertTrue((UnsignedLongs.compare(0x5a4316b8c153ac4dL, 0xff1a618b7f65ea12L) < 0));
    assertTrue((UnsignedLongs.compare(0xff1a618b7f65ea12L, 0x5a4316b8c153ac4dL) > 0));

    // neither with high bit set
    assertTrue((UnsignedLongs.compare(0x5a4316b8c153ac4dL, 0x6cf78a4b139a4e2aL) < 0));
    assertTrue((UnsignedLongs.compare(0x6cf78a4b139a4e2aL, 0x5a4316b8c153ac4dL) > 0));

    // same value
    assertTrue((UnsignedLongs.compare(0xff1a618b7f65ea12L, 0xff1a618b7f65ea12L) == 0));
  }

  public void testDivide() {
    assertEquals(2, UnsignedLongs.divide(14, 5));
    assertEquals(0, UnsignedLongs.divide(0, 50));
    assertEquals(1, UnsignedLongs.divide(0xfffffffffffffffeL, 0xfffffffffffffffdL));
    assertEquals(0, UnsignedLongs.divide(0xfffffffffffffffdL, 0xfffffffffffffffeL));
    assertEquals(281479271743488L, UnsignedLongs.divide(0xfffffffffffffffeL, 65535));
    assertEquals(0x7fffffffffffffffL, UnsignedLongs.divide(0xfffffffffffffffeL, 2));
    assertEquals(3689348814741910322L, UnsignedLongs.divide(0xfffffffffffffffeL, 5));
  }

  public void testRemainder() {
    assertEquals(4, UnsignedLongs.remainder(14, 5));
    assertEquals(0, UnsignedLongs.remainder(0, 50));
    assertEquals(1, UnsignedLongs.remainder(0xfffffffffffffffeL, 0xfffffffffffffffdL));
    assertEquals(0xfffffffffffffffdL,
        UnsignedLongs.remainder(0xfffffffffffffffdL, 0xfffffffffffffffeL));
    assertEquals(65534L, UnsignedLongs.remainder(0xfffffffffffffffeL, 65535));
    assertEquals(0, UnsignedLongs.remainder(0xfffffffffffffffeL, 2));
    assertEquals(4, UnsignedLongs.remainder(0xfffffffffffffffeL, 5));
  }

  @GwtIncompatible("Too slow in GWT (~3min fully optimized)")
  public void testDivideRemainderEuclideanProperty() {
    // Use a seed so that the test is deterministic:
    Random r = new Random(0L);
    for (int i = 0; i < 1000000; i++) {
      long dividend = r.nextLong();
      long divisor = r.nextLong();
      // Test that the Euclidean property is preserved:
      assertTrue(dividend - (divisor * UnsignedLongs.divide(dividend, divisor) 
          + UnsignedLongs.remainder(dividend, divisor)) == 0);
    }
  }

  public void testParseLong() {
    try {
      assertEquals(0xffffffffffffffffL, UnsignedLongs.parseUnsignedLong("18446744073709551615"));
      assertEquals(0x7fffffffffffffffL, UnsignedLongs.parseUnsignedLong("9223372036854775807"));
      assertEquals(0xff1a618b7f65ea12L, UnsignedLongs.parseUnsignedLong("18382112080831834642"));
      assertEquals(0x5a4316b8c153ac4dL, UnsignedLongs.parseUnsignedLong("6504067269626408013"));
      assertEquals(0x6cf78a4b139a4e2aL, UnsignedLongs.parseUnsignedLong("7851896530399809066"));
    } catch (NumberFormatException e) {
      fail(e.getMessage());
    }

    boolean overflowCaught = false;
    try {
      // One more than maximum value
      UnsignedLongs.parseUnsignedLong("18446744073709551616");
    } catch (NumberFormatException e) {
      overflowCaught = true;
    }
    assertTrue(overflowCaught);
  }

  public void testParseLongWithRadix() throws NumberFormatException {
    assertEquals(0xffffffffffffffffL, UnsignedLongs.parseUnsignedLong("ffffffffffffffff", 16));
    assertEquals(0x1234567890abcdefL, UnsignedLongs.parseUnsignedLong("1234567890abcdef", 16));

    BigInteger max = BigInteger.ZERO.setBit(64).subtract(ONE);
    // loops through all legal radix values.
    for (int radix = Character.MIN_RADIX; radix <= Character.MAX_RADIX; radix++) {
      // tests can successfully parse a number string with this radix.
      String maxAsString = max.toString(radix);
      assertEquals(max.longValue(), UnsignedLongs.parseUnsignedLong(maxAsString, radix));

      try {
        // tests that we get exception whre an overflow would occur.
        BigInteger overflow = max.add(ONE);
        String overflowAsString = overflow.toString(radix);
        UnsignedLongs.parseUnsignedLong(overflowAsString, radix);
        fail();
      } catch (NumberFormatException nfe) {
        // expected
      }
    }
  }

  public void testParseLongThrowsExceptionForInvalidRadix() {
    // Valid radix values are Character.MIN_RADIX to Character.MAX_RADIX,
    // inclusive.
    try {
      UnsignedLongs.parseUnsignedLong("0", Character.MIN_RADIX - 1);
      fail();
    } catch (NumberFormatException nfe) {
      // expected
    }

    try {
      UnsignedLongs.parseUnsignedLong("0", Character.MAX_RADIX + 1);
      fail();
    } catch (NumberFormatException nfe) {
      // expected
    }

    // The radix is used as an array index, so try a negative value.
    try {
      UnsignedLongs.parseUnsignedLong("0", -1);
      fail();
    } catch (NumberFormatException nfe) {
      // expected
    }
  }

  public void testToString() {
    String[] tests = {
        "ffffffffffffffff",
        "7fffffffffffffff",
        "ff1a618b7f65ea12",
        "5a4316b8c153ac4d",
        "6cf78a4b139a4e2a"};
    int[] bases = {2, 5, 7, 8, 10, 16};
    for (int base : bases) {
      for (String x : tests) {
        BigInteger xValue = new BigInteger(x, 16);
        long xLong = xValue.longValue(); // signed
        assertEquals(xValue.toString(base), UnsignedLongs.toString(xLong, base));
      }
    }
  }

  @GwtIncompatible("NullPointerTester")
  public void testNulls() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.setDefault(long[].class, new long[0]);
    tester.setDefault(BigInteger.class, BigInteger.ZERO);
    tester.testAllPublicStaticMethods(UnsignedLongs.class);
  }
}

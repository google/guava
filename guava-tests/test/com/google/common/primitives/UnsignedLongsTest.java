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

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.Helpers;
import com.google.common.testing.NullPointerTester;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import junit.framework.TestCase;

/**
 * Tests for UnsignedLongs
 *
 * @author Brian Milch
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class UnsignedLongsTest extends TestCase {
  private static final long LEAST = 0L;
  private static final long GREATEST = 0xffffffffffffffffL;

  public void testCompare() {
    // max value
    assertTrue(UnsignedLongs.compare(0, 0xffffffffffffffffL) < 0);
    assertTrue(UnsignedLongs.compare(0xffffffffffffffffL, 0) > 0);

    // both with high bit set
    assertTrue(UnsignedLongs.compare(0xff1a618b7f65ea12L, 0xffffffffffffffffL) < 0);
    assertTrue(UnsignedLongs.compare(0xffffffffffffffffL, 0xff1a618b7f65ea12L) > 0);

    // one with high bit set
    assertTrue(UnsignedLongs.compare(0x5a4316b8c153ac4dL, 0xff1a618b7f65ea12L) < 0);
    assertTrue(UnsignedLongs.compare(0xff1a618b7f65ea12L, 0x5a4316b8c153ac4dL) > 0);

    // neither with high bit set
    assertTrue(UnsignedLongs.compare(0x5a4316b8c153ac4dL, 0x6cf78a4b139a4e2aL) < 0);
    assertTrue(UnsignedLongs.compare(0x6cf78a4b139a4e2aL, 0x5a4316b8c153ac4dL) > 0);

    // same value
    assertTrue(UnsignedLongs.compare(0xff1a618b7f65ea12L, 0xff1a618b7f65ea12L) == 0);
  }

  public void testMax_noArgs() {
    try {
      UnsignedLongs.max();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMax() {
    assertEquals(LEAST, UnsignedLongs.max(LEAST));
    assertEquals(GREATEST, UnsignedLongs.max(GREATEST));
    assertEquals(
        0xff1a618b7f65ea12L,
        UnsignedLongs.max(
            0x5a4316b8c153ac4dL, 8L, 100L, 0L, 0x6cf78a4b139a4e2aL, 0xff1a618b7f65ea12L));
  }

  public void testMin_noArgs() {
    try {
      UnsignedLongs.min();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMin() {
    assertEquals(LEAST, UnsignedLongs.min(LEAST));
    assertEquals(GREATEST, UnsignedLongs.min(GREATEST));
    assertEquals(
        0L,
        UnsignedLongs.min(
            0x5a4316b8c153ac4dL, 8L, 100L, 0L, 0x6cf78a4b139a4e2aL, 0xff1a618b7f65ea12L));
  }

  public void testLexicographicalComparator() {
    List<long[]> ordered =
        Arrays.asList(
            new long[] {},
            new long[] {LEAST},
            new long[] {LEAST, LEAST},
            new long[] {LEAST, (long) 1},
            new long[] {(long) 1},
            new long[] {(long) 1, LEAST},
            new long[] {GREATEST, GREATEST - (long) 1},
            new long[] {GREATEST, GREATEST},
            new long[] {GREATEST, GREATEST, GREATEST});

    Comparator<long[]> comparator = UnsignedLongs.lexicographicalComparator();
    Helpers.testComparator(comparator, ordered);
  }

  public void testSort() {
    testSort(new long[] {}, new long[] {});
    testSort(new long[] {2}, new long[] {2});
    testSort(new long[] {2, 1, 0}, new long[] {0, 1, 2});
    testSort(new long[] {2, GREATEST, 1, LEAST}, new long[] {LEAST, 1, 2, GREATEST});
  }

  static void testSort(long[] input, long[] expected) {
    input = Arrays.copyOf(input, input.length);
    UnsignedLongs.sort(input);
    assertTrue(Arrays.equals(expected, input));
  }

  static void testSort(long[] input, int from, int to, long[] expected) {
    input = Arrays.copyOf(input, input.length);
    UnsignedLongs.sort(input, from, to);
    assertTrue(Arrays.equals(expected, input));
  }

  public void testSortIndexed() {
    testSort(new long[] {}, 0, 0, new long[] {});
    testSort(new long[] {2}, 0, 1, new long[] {2});
    testSort(new long[] {2, 1, 0}, 0, 2, new long[] {1, 2, 0});
    testSort(new long[] {2, GREATEST, 1, LEAST}, 1, 4, new long[] {2, LEAST, 1, GREATEST});
  }

  public void testSortDescending() {
    testSortDescending(new long[] {}, new long[] {});
    testSortDescending(new long[] {1}, new long[] {1});
    testSortDescending(new long[] {1, 2}, new long[] {2, 1});
    testSortDescending(new long[] {1, 3, 1}, new long[] {3, 1, 1});
    testSortDescending(
        new long[] {GREATEST - 1, 1, GREATEST - 2, 2},
        new long[] {GREATEST - 1, GREATEST - 2, 2, 1});
  }

  private static void testSortDescending(long[] input, long[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    UnsignedLongs.sortDescending(input);
    assertTrue(Arrays.equals(expectedOutput, input));
  }

  private static void testSortDescending(
      long[] input, int fromIndex, int toIndex, long[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    UnsignedLongs.sortDescending(input, fromIndex, toIndex);
    assertTrue(Arrays.equals(expectedOutput, input));
  }

  public void testSortDescendingIndexed() {
    testSortDescending(new long[] {}, 0, 0, new long[] {});
    testSortDescending(new long[] {1}, 0, 1, new long[] {1});
    testSortDescending(new long[] {1, 2}, 0, 2, new long[] {2, 1});
    testSortDescending(new long[] {1, 3, 1}, 0, 2, new long[] {3, 1, 1});
    testSortDescending(new long[] {1, 3, 1}, 0, 1, new long[] {1, 3, 1});
    testSortDescending(
        new long[] {GREATEST - 1, 1, GREATEST - 2, 2},
        1,
        3,
        new long[] {GREATEST - 1, GREATEST - 2, 1, 2});
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
    assertEquals(
        0xfffffffffffffffdL, UnsignedLongs.remainder(0xfffffffffffffffdL, 0xfffffffffffffffeL));
    assertEquals(65534L, UnsignedLongs.remainder(0xfffffffffffffffeL, 65535));
    assertEquals(0, UnsignedLongs.remainder(0xfffffffffffffffeL, 2));
    assertEquals(4, UnsignedLongs.remainder(0xfffffffffffffffeL, 5));
  }

  @GwtIncompatible // Too slow in GWT (~3min fully optimized)
  public void testDivideRemainderEuclideanProperty() {
    // Use a seed so that the test is deterministic:
    Random r = new Random(0L);
    for (int i = 0; i < 1000000; i++) {
      long dividend = r.nextLong();
      long divisor = r.nextLong();
      // Test that the Euclidean property is preserved:
      assertEquals(
          0,
          dividend
              - (divisor * UnsignedLongs.divide(dividend, divisor)
                  + UnsignedLongs.remainder(dividend, divisor)));
    }
  }

  public void testParseLong() {
    assertEquals(0xffffffffffffffffL, UnsignedLongs.parseUnsignedLong("18446744073709551615"));
    assertEquals(0x7fffffffffffffffL, UnsignedLongs.parseUnsignedLong("9223372036854775807"));
    assertEquals(0xff1a618b7f65ea12L, UnsignedLongs.parseUnsignedLong("18382112080831834642"));
    assertEquals(0x5a4316b8c153ac4dL, UnsignedLongs.parseUnsignedLong("6504067269626408013"));
    assertEquals(0x6cf78a4b139a4e2aL, UnsignedLongs.parseUnsignedLong("7851896530399809066"));
  }

  public void testParseLongEmptyString() {
    try {
      UnsignedLongs.parseUnsignedLong("");
      fail("NumberFormatException should have been raised.");
    } catch (NumberFormatException expected) {
    }
  }

  public void testParseLongFails() {
    try {
      // One more than maximum value
      UnsignedLongs.parseUnsignedLong("18446744073709551616");
      fail();
    } catch (NumberFormatException expected) {
    }
  }

  public void testDecodeLong() {
    assertEquals(0xffffffffffffffffL, UnsignedLongs.decode("0xffffffffffffffff"));
    assertEquals(01234567, UnsignedLongs.decode("01234567")); // octal
    assertEquals(0x1234567890abcdefL, UnsignedLongs.decode("#1234567890abcdef"));
    assertEquals(987654321012345678L, UnsignedLongs.decode("987654321012345678"));
    assertEquals(0x135791357913579L, UnsignedLongs.decode("0x135791357913579"));
    assertEquals(0x135791357913579L, UnsignedLongs.decode("0X135791357913579"));
    assertEquals(0L, UnsignedLongs.decode("0"));
  }

  public void testDecodeLongFails() {
    try {
      // One more than maximum value
      UnsignedLongs.decode("0xfffffffffffffffff");
      fail();
    } catch (NumberFormatException expected) {
    }

    try {
      UnsignedLongs.decode("-5");
      fail();
    } catch (NumberFormatException expected) {
    }

    try {
      UnsignedLongs.decode("-0x5");
      fail();
    } catch (NumberFormatException expected) {
    }

    try {
      UnsignedLongs.decode("-05");
      fail();
    } catch (NumberFormatException expected) {
    }
  }

  public void testParseLongWithRadix() {
    assertEquals(0xffffffffffffffffL, UnsignedLongs.parseUnsignedLong("ffffffffffffffff", 16));
    assertEquals(0x1234567890abcdefL, UnsignedLongs.parseUnsignedLong("1234567890abcdef", 16));
  }

  public void testParseLongWithRadixLimits() {
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
      } catch (NumberFormatException expected) {
      }
    }

    try {
      UnsignedLongs.parseUnsignedLong("1234567890abcdef1", 16);
      fail();
    } catch (NumberFormatException expected) {
    }
  }

  public void testParseLongThrowsExceptionForInvalidRadix() {
    // Valid radix values are Character.MIN_RADIX to Character.MAX_RADIX, inclusive.
    try {
      UnsignedLongs.parseUnsignedLong("0", Character.MIN_RADIX - 1);
      fail();
    } catch (NumberFormatException expected) {
    }

    try {
      UnsignedLongs.parseUnsignedLong("0", Character.MAX_RADIX + 1);
      fail();
    } catch (NumberFormatException expected) {
    }

    // The radix is used as an array index, so try a negative value.
    try {
      UnsignedLongs.parseUnsignedLong("0", -1);
      fail();
    } catch (NumberFormatException expected) {
    }
  }

  public void testToString() {
    String[] tests = {
      "0",
      "ffffffffffffffff",
      "7fffffffffffffff",
      "ff1a618b7f65ea12",
      "5a4316b8c153ac4d",
      "6cf78a4b139a4e2a"
    };
    int[] bases = {2, 5, 7, 8, 10, 16};
    for (int base : bases) {
      for (String x : tests) {
        BigInteger xValue = new BigInteger(x, 16);
        long xLong = xValue.longValue(); // signed
        assertEquals(xValue.toString(base), UnsignedLongs.toString(xLong, base));
      }
    }
  }

  public void testJoin() {
    assertEquals("", UnsignedLongs.join(","));
    assertEquals("1", UnsignedLongs.join(",", 1));
    assertEquals("1,2", UnsignedLongs.join(",", 1, 2));
    assertEquals(
        "18446744073709551615,9223372036854775808", UnsignedLongs.join(",", -1, Long.MIN_VALUE));
    assertEquals("123", UnsignedLongs.join("", 1, 2, 3));
    assertEquals(
        "184467440737095516159223372036854775808", UnsignedLongs.join("", -1, Long.MIN_VALUE));
  }

  @GwtIncompatible // NullPointerTester
  public void testNulls() {
    new NullPointerTester().testAllPublicStaticMethods(UnsignedLongs.class);
  }
}

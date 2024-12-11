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

import static com.google.common.primitives.ReflectionFreeAssertThrows.assertThrows;
import static com.google.common.primitives.UnsignedLongs.max;
import static com.google.common.primitives.UnsignedLongs.min;
import static com.google.common.truth.Truth.assertThat;
import static java.math.BigInteger.ONE;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
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
    assertThat(UnsignedLongs.compare(0, 0xffffffffffffffffL)).isLessThan(0);
    assertThat(UnsignedLongs.compare(0xffffffffffffffffL, 0)).isGreaterThan(0);

    // both with high bit set
    assertThat(UnsignedLongs.compare(0xff1a618b7f65ea12L, 0xffffffffffffffffL)).isLessThan(0);
    assertThat(UnsignedLongs.compare(0xffffffffffffffffL, 0xff1a618b7f65ea12L)).isGreaterThan(0);

    // one with high bit set
    assertThat(UnsignedLongs.compare(0x5a4316b8c153ac4dL, 0xff1a618b7f65ea12L)).isLessThan(0);
    assertThat(UnsignedLongs.compare(0xff1a618b7f65ea12L, 0x5a4316b8c153ac4dL)).isGreaterThan(0);

    // neither with high bit set
    assertThat(UnsignedLongs.compare(0x5a4316b8c153ac4dL, 0x6cf78a4b139a4e2aL)).isLessThan(0);
    assertThat(UnsignedLongs.compare(0x6cf78a4b139a4e2aL, 0x5a4316b8c153ac4dL)).isGreaterThan(0);

    // same value
    assertThat(UnsignedLongs.compare(0xff1a618b7f65ea12L, 0xff1a618b7f65ea12L)).isEqualTo(0);
  }

  public void testMax_noArgs() {
    assertThrows(IllegalArgumentException.class, () -> max());
  }

  public void testMax() {
    assertThat(max(LEAST)).isEqualTo(LEAST);
    assertThat(max(GREATEST)).isEqualTo(GREATEST);
    assertThat(max(0x5a4316b8c153ac4dL, 8L, 100L, 0L, 0x6cf78a4b139a4e2aL, 0xff1a618b7f65ea12L))
        .isEqualTo(0xff1a618b7f65ea12L);
  }

  public void testMin_noArgs() {
    assertThrows(IllegalArgumentException.class, () -> min());
  }

  public void testMin() {
    assertThat(min(LEAST)).isEqualTo(LEAST);
    assertThat(min(GREATEST)).isEqualTo(GREATEST);
    assertThat(min(0x5a4316b8c153ac4dL, 8L, 100L, 0L, 0x6cf78a4b139a4e2aL, 0xff1a618b7f65ea12L))
        .isEqualTo(0L);
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
    assertThat(input).isEqualTo(expected);
  }

  static void testSort(long[] input, int from, int to, long[] expected) {
    input = Arrays.copyOf(input, input.length);
    UnsignedLongs.sort(input, from, to);
    assertThat(input).isEqualTo(expected);
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
    assertThat(input).isEqualTo(expectedOutput);
  }

  private static void testSortDescending(
      long[] input, int fromIndex, int toIndex, long[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    UnsignedLongs.sortDescending(input, fromIndex, toIndex);
    assertThat(input).isEqualTo(expectedOutput);
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
    assertThat(UnsignedLongs.divide(14, 5)).isEqualTo(2);
    assertThat(UnsignedLongs.divide(0, 50)).isEqualTo(0);
    assertThat(UnsignedLongs.divide(0xfffffffffffffffeL, 0xfffffffffffffffdL)).isEqualTo(1);
    assertThat(UnsignedLongs.divide(0xfffffffffffffffdL, 0xfffffffffffffffeL)).isEqualTo(0);
    assertThat(UnsignedLongs.divide(0xfffffffffffffffeL, 65535)).isEqualTo(281479271743488L);
    assertThat(UnsignedLongs.divide(0xfffffffffffffffeL, 2)).isEqualTo(0x7fffffffffffffffL);
    assertThat(UnsignedLongs.divide(0xfffffffffffffffeL, 5)).isEqualTo(3689348814741910322L);
  }

  public void testRemainder() {
    assertThat(UnsignedLongs.remainder(14, 5)).isEqualTo(4);
    assertThat(UnsignedLongs.remainder(0, 50)).isEqualTo(0);
    assertThat(UnsignedLongs.remainder(0xfffffffffffffffeL, 0xfffffffffffffffdL)).isEqualTo(1);
    assertThat(UnsignedLongs.remainder(0xfffffffffffffffdL, 0xfffffffffffffffeL))
        .isEqualTo(0xfffffffffffffffdL);
    assertThat(UnsignedLongs.remainder(0xfffffffffffffffeL, 65535)).isEqualTo(65534L);
    assertThat(UnsignedLongs.remainder(0xfffffffffffffffeL, 2)).isEqualTo(0);
    assertThat(UnsignedLongs.remainder(0xfffffffffffffffeL, 5)).isEqualTo(4);
  }

  @GwtIncompatible // Too slow in GWT (~3min fully optimized)
  public void testDivideRemainderEuclideanProperty() {
    // Use a seed so that the test is deterministic:
    Random r = new Random(0L);
    for (int i = 0; i < 1000000; i++) {
      long dividend = r.nextLong();
      long divisor = r.nextLong();
      // Test that the Euclidean property is preserved:
      assertThat(
              dividend
                  - (divisor * UnsignedLongs.divide(dividend, divisor)
                      + UnsignedLongs.remainder(dividend, divisor)))
          .isEqualTo(0);
    }
  }

  public void testParseLong() {
    assertThat(UnsignedLongs.parseUnsignedLong("18446744073709551615"))
        .isEqualTo(0xffffffffffffffffL);
    assertThat(UnsignedLongs.parseUnsignedLong("9223372036854775807"))
        .isEqualTo(0x7fffffffffffffffL);
    assertThat(UnsignedLongs.parseUnsignedLong("18382112080831834642"))
        .isEqualTo(0xff1a618b7f65ea12L);
    assertThat(UnsignedLongs.parseUnsignedLong("6504067269626408013"))
        .isEqualTo(0x5a4316b8c153ac4dL);
    assertThat(UnsignedLongs.parseUnsignedLong("7851896530399809066"))
        .isEqualTo(0x6cf78a4b139a4e2aL);
  }

  public void testParseLongEmptyString() {
    assertThrows(NumberFormatException.class, () -> UnsignedLongs.parseUnsignedLong(""));
  }

  public void testParseLongFails() {
    assertThrows(
        NumberFormatException.class, () -> UnsignedLongs.parseUnsignedLong("18446744073709551616"));
  }

  public void testDecodeLong() {
    assertThat(UnsignedLongs.decode("0xffffffffffffffff")).isEqualTo(0xffffffffffffffffL);
    assertThat(UnsignedLongs.decode("01234567")).isEqualTo(01234567); // octal
    assertThat(UnsignedLongs.decode("#1234567890abcdef")).isEqualTo(0x1234567890abcdefL);
    assertThat(UnsignedLongs.decode("987654321012345678")).isEqualTo(987654321012345678L);
    assertThat(UnsignedLongs.decode("0x135791357913579")).isEqualTo(0x135791357913579L);
    assertThat(UnsignedLongs.decode("0X135791357913579")).isEqualTo(0x135791357913579L);
    assertThat(UnsignedLongs.decode("0")).isEqualTo(0L);
  }

  public void testDecodeLongFails() {
    assertThrows(NumberFormatException.class, () -> UnsignedLongs.decode("0xfffffffffffffffff"));

    assertThrows(NumberFormatException.class, () -> UnsignedLongs.decode("-5"));

    assertThrows(NumberFormatException.class, () -> UnsignedLongs.decode("-0x5"));

    assertThrows(NumberFormatException.class, () -> UnsignedLongs.decode("-05"));
  }

  public void testParseLongWithRadix() {
    assertThat(UnsignedLongs.parseUnsignedLong("ffffffffffffffff", 16))
        .isEqualTo(0xffffffffffffffffL);
    assertThat(UnsignedLongs.parseUnsignedLong("1234567890abcdef", 16))
        .isEqualTo(0x1234567890abcdefL);
  }

  public void testParseLongWithRadixLimits() {
    BigInteger max = BigInteger.ZERO.setBit(64).subtract(ONE);
    // loops through all legal radix values.
    for (int r = Character.MIN_RADIX; r <= Character.MAX_RADIX; r++) {
      final int radix = r;
      // tests can successfully parse a number string with this radix.
      String maxAsString = max.toString(radix);
      assertThat(UnsignedLongs.parseUnsignedLong(maxAsString, radix)).isEqualTo(max.longValue());

      assertThrows(
          NumberFormatException.class,
          () -> {
            BigInteger overflow = max.add(ONE);
            String overflowAsString = overflow.toString(radix);
            UnsignedLongs.parseUnsignedLong(overflowAsString, radix);
          });
    }

    assertThrows(
        NumberFormatException.class,
        () -> UnsignedLongs.parseUnsignedLong("1234567890abcdef1", 16));
  }

  public void testParseLongThrowsExceptionForInvalidRadix() {
    // Valid radix values are Character.MIN_RADIX to Character.MAX_RADIX, inclusive.
    assertThrows(
        NumberFormatException.class,
        () -> UnsignedLongs.parseUnsignedLong("0", Character.MIN_RADIX - 1));

    assertThrows(
        NumberFormatException.class,
        () -> UnsignedLongs.parseUnsignedLong("0", Character.MAX_RADIX + 1));

    // The radix is used as an array index, so try a negative value.
    assertThrows(NumberFormatException.class, () -> UnsignedLongs.parseUnsignedLong("0", -1));
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
        assertThat(UnsignedLongs.toString(xLong, base)).isEqualTo(xValue.toString(base));
      }
    }
  }

  public void testJoin() {
    assertThat(UnsignedLongs.join(",")).isEmpty();
    assertThat(UnsignedLongs.join(",", 1)).isEqualTo("1");
    assertThat(UnsignedLongs.join(",", 1, 2)).isEqualTo("1,2");
    assertThat(UnsignedLongs.join(",", -1, Long.MIN_VALUE))
        .isEqualTo("18446744073709551615,9223372036854775808");
    assertThat(UnsignedLongs.join("", 1, 2, 3)).isEqualTo("123");
    assertThat(UnsignedLongs.join("", -1, Long.MIN_VALUE))
        .isEqualTo("184467440737095516159223372036854775808");
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testNulls() {
    new NullPointerTester().testAllPublicStaticMethods(UnsignedLongs.class);
  }
}

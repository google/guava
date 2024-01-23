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

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.testing.Helpers;
import com.google.common.testing.NullPointerTester;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import junit.framework.TestCase;

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
    0xffffffffL
  };

  private static final int LEAST = (int) 0L;
  private static final int GREATEST = (int) 0xffffffffL;

  public void testCheckedCast() {
    for (long value : UNSIGNED_INTS) {
      assertThat(UnsignedInts.toLong(UnsignedInts.checkedCast(value))).isEqualTo(value);
    }
    assertCastFails(1L << 32);
    assertCastFails(-1L);
    assertCastFails(Long.MAX_VALUE);
    assertCastFails(Long.MIN_VALUE);
  }

  private static void assertCastFails(long value) {
    try {
      UnsignedInts.checkedCast(value);
      fail("Cast to int should have failed: " + value);
    } catch (IllegalArgumentException ex) {
      assertThat(ex).hasMessageThat().contains(String.valueOf(value));
    }
  }

  public void testSaturatedCast() {
    for (long value : UNSIGNED_INTS) {
      assertThat(UnsignedInts.toLong(UnsignedInts.saturatedCast(value))).isEqualTo(value);
    }
    assertThat(UnsignedInts.saturatedCast(1L << 32)).isEqualTo(GREATEST);
    assertThat(UnsignedInts.saturatedCast(-1L)).isEqualTo(LEAST);
    assertThat(UnsignedInts.saturatedCast(Long.MAX_VALUE)).isEqualTo(GREATEST);
    assertThat(UnsignedInts.saturatedCast(Long.MIN_VALUE)).isEqualTo(LEAST);
  }

  public void testToLong() {
    for (long a : UNSIGNED_INTS) {
      assertThat(UnsignedInts.toLong((int) a)).isEqualTo(a);
    }
  }

  public void testCompare() {
    for (long a : UNSIGNED_INTS) {
      for (long b : UNSIGNED_INTS) {
        int cmpAsLongs = Longs.compare(a, b);
        int cmpAsUInt = UnsignedInts.compare((int) a, (int) b);
        assertThat(Integer.signum(cmpAsUInt)).isEqualTo(Integer.signum(cmpAsLongs));
      }
    }
  }

  public void testMax_noArgs() {
    try {
      UnsignedInts.max();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMax() {
    assertThat(UnsignedInts.max(LEAST)).isEqualTo(LEAST);
    assertThat(UnsignedInts.max(GREATEST)).isEqualTo(GREATEST);
    assertThat(
            UnsignedInts.max(
                (int) 8L,
                (int) 6L,
                (int) 7L,
                (int) 0x12345678L,
                (int) 0x5a4316b8L,
                (int) 0xff1a618bL,
                (int) 0L))
        .isEqualTo((int) 0xff1a618bL);
  }

  public void testMin_noArgs() {
    try {
      UnsignedInts.min();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMin() {
    assertThat(UnsignedInts.min(LEAST)).isEqualTo(LEAST);
    assertThat(UnsignedInts.min(GREATEST)).isEqualTo(GREATEST);
    assertThat(
            UnsignedInts.min(
                (int) 8L,
                (int) 6L,
                (int) 7L,
                (int) 0x12345678L,
                (int) 0x5a4316b8L,
                (int) 0xff1a618bL,
                (int) 0L))
        .isEqualTo((int) 0L);
  }

  public void testLexicographicalComparator() {
    List<int[]> ordered =
        Arrays.asList(
            new int[] {},
            new int[] {LEAST},
            new int[] {LEAST, LEAST},
            new int[] {LEAST, (int) 1L},
            new int[] {(int) 1L},
            new int[] {(int) 1L, LEAST},
            new int[] {GREATEST, (GREATEST - (int) 1L)},
            new int[] {GREATEST, GREATEST},
            new int[] {GREATEST, GREATEST, GREATEST});

    Comparator<int[]> comparator = UnsignedInts.lexicographicalComparator();
    Helpers.testComparator(comparator, ordered);
  }

  public void testSort() {
    testSort(new int[] {}, new int[] {});
    testSort(new int[] {2}, new int[] {2});
    testSort(new int[] {2, 1, 0}, new int[] {0, 1, 2});
    testSort(new int[] {2, GREATEST, 1, LEAST}, new int[] {LEAST, 1, 2, GREATEST});
  }

  static void testSort(int[] input, int[] expected) {
    input = Arrays.copyOf(input, input.length);
    UnsignedInts.sort(input);
    assertThat(input).isEqualTo(expected);
  }

  static void testSort(int[] input, int from, int to, int[] expected) {
    input = Arrays.copyOf(input, input.length);
    UnsignedInts.sort(input, from, to);
    assertThat(input).isEqualTo(expected);
  }

  public void testSortIndexed() {
    testSort(new int[] {}, 0, 0, new int[] {});
    testSort(new int[] {2}, 0, 1, new int[] {2});
    testSort(new int[] {2, 1, 0}, 0, 2, new int[] {1, 2, 0});
    testSort(new int[] {2, GREATEST, 1, LEAST}, 1, 4, new int[] {2, LEAST, 1, GREATEST});
  }

  public void testSortDescending() {
    testSortDescending(new int[] {}, new int[] {});
    testSortDescending(new int[] {1}, new int[] {1});
    testSortDescending(new int[] {1, 2}, new int[] {2, 1});
    testSortDescending(new int[] {1, 3, 1}, new int[] {3, 1, 1});
    testSortDescending(
        new int[] {GREATEST - 1, 1, GREATEST - 2, 2}, new int[] {GREATEST - 1, GREATEST - 2, 2, 1});
  }

  private static void testSortDescending(int[] input, int[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    UnsignedInts.sortDescending(input);
    assertThat(input).isEqualTo(expectedOutput);
  }

  private static void testSortDescending(
      int[] input, int fromIndex, int toIndex, int[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    UnsignedInts.sortDescending(input, fromIndex, toIndex);
    assertThat(input).isEqualTo(expectedOutput);
  }

  public void testSortDescendingIndexed() {
    testSortDescending(new int[] {}, 0, 0, new int[] {});
    testSortDescending(new int[] {1}, 0, 1, new int[] {1});
    testSortDescending(new int[] {1, 2}, 0, 2, new int[] {2, 1});
    testSortDescending(new int[] {1, 3, 1}, 0, 2, new int[] {3, 1, 1});
    testSortDescending(new int[] {1, 3, 1}, 0, 1, new int[] {1, 3, 1});
    testSortDescending(
        new int[] {GREATEST - 1, 1, GREATEST - 2, 2},
        1,
        3,
        new int[] {GREATEST - 1, GREATEST - 2, 1, 2});
  }

  public void testDivide() {
    for (long a : UNSIGNED_INTS) {
      for (long b : UNSIGNED_INTS) {
        try {
          assertThat(UnsignedInts.divide((int) a, (int) b)).isEqualTo((int) (a / b));
          assertThat(b).isNotEqualTo(0);
        } catch (ArithmeticException e) {
          assertThat(b).isEqualTo(0);
        }
      }
    }
  }

  public void testRemainder() {
    for (long a : UNSIGNED_INTS) {
      for (long b : UNSIGNED_INTS) {
        try {
          assertThat(UnsignedInts.remainder((int) a, (int) b)).isEqualTo((int) (a % b));
          assertThat(b).isNotEqualTo(0);
        } catch (ArithmeticException e) {
          assertThat(b).isEqualTo(0);
        }
      }
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // Too slow in GWT (~3min fully optimized)
  public void testDivideRemainderEuclideanProperty() {
    // Use a seed so that the test is deterministic:
    Random r = new Random(0L);
    for (int i = 0; i < 1000000; i++) {
      int dividend = r.nextInt();
      int divisor = r.nextInt();
      // Test that the Euclidean property is preserved:
      assertThat(
              dividend
                  - (divisor * UnsignedInts.divide(dividend, divisor)
                      + UnsignedInts.remainder(dividend, divisor)))
          .isEqualTo(0);
    }
  }

  public void testParseInt() {
    for (long a : UNSIGNED_INTS) {
      assertThat(UnsignedInts.parseUnsignedInt(Long.toString(a))).isEqualTo((int) a);
    }
  }

  public void testParseIntFail() {
    try {
      UnsignedInts.parseUnsignedInt(Long.toString(1L << 32));
      fail("Expected NumberFormatException");
    } catch (NumberFormatException expected) {
    }
  }

  public void testParseIntWithRadix() {
    for (long a : UNSIGNED_INTS) {
      for (int radix = Character.MIN_RADIX; radix <= Character.MAX_RADIX; radix++) {
        assertThat(UnsignedInts.parseUnsignedInt(Long.toString(a, radix), radix))
            .isEqualTo((int) a);
      }
    }
  }

  public void testParseIntWithRadixLimits() {
    // loops through all legal radix values.
    for (int radix = Character.MIN_RADIX; radix <= Character.MAX_RADIX; radix++) {
      // tests can successfully parse a number string with this radix.
      String maxAsString = Long.toString((1L << 32) - 1, radix);
      assertThat(UnsignedInts.parseUnsignedInt(maxAsString, radix)).isEqualTo(-1);

      try {
        // tests that we get exception where an overflow would occur.
        long overflow = 1L << 32;
        String overflowAsString = Long.toString(overflow, radix);
        UnsignedInts.parseUnsignedInt(overflowAsString, radix);
        fail();
      } catch (NumberFormatException expected) {
      }
    }
  }

  public void testParseIntThrowsExceptionForInvalidRadix() {
    // Valid radix values are Character.MIN_RADIX to Character.MAX_RADIX,
    // inclusive.
    //
    // Note: According to the spec, a NumberFormatException is thrown for a number that is not
    // parseable, but the spec doesn't seem to say which exception is thrown for an invalid radix.
    // In contrast to the JVM, Kotlin native throws an Illegal argument exception in this case
    // (which seems to make more sense).
    try {
      UnsignedInts.parseUnsignedInt("0", Character.MIN_RADIX - 1);
      fail();
    } catch (NumberFormatException expected) {
    } catch (IllegalArgumentException expected) {
      // Kotlin native, see above
    }

    try {
      UnsignedInts.parseUnsignedInt("0", Character.MAX_RADIX + 1);
      fail();
    } catch (NumberFormatException expected) {
    } catch (IllegalArgumentException expected) {
      // Kotlin native, see above
    }

    // The radix is used as an array index, so try a negative value.
    try {
      UnsignedInts.parseUnsignedInt("0", -1);
      fail();
    } catch (NumberFormatException expected) {
    } catch (IllegalArgumentException expected) {
      // Kotlin native, see above
    }
  }

  public void testDecodeInt() {
    assertThat(UnsignedInts.decode("0xffffffff")).isEqualTo(0xffffffff);
    assertThat(UnsignedInts.decode("01234567")).isEqualTo(01234567); // octal
    assertThat(UnsignedInts.decode("#12345678")).isEqualTo(0x12345678);
    assertThat(UnsignedInts.decode("76543210")).isEqualTo(76543210);
    assertThat(UnsignedInts.decode("0x13579135")).isEqualTo(0x13579135);
    assertThat(UnsignedInts.decode("0X13579135")).isEqualTo(0x13579135);
    assertThat(UnsignedInts.decode("0")).isEqualTo(0);
  }

  public void testDecodeIntFails() {
    try {
      // One more than maximum value
      UnsignedInts.decode("0xfffffffff");
      fail();
    } catch (NumberFormatException expected) {
    }

    try {
      UnsignedInts.decode("-5");
      fail();
    } catch (NumberFormatException expected) {
    }

    try {
      UnsignedInts.decode("-0x5");
      fail();
    } catch (NumberFormatException expected) {
    }

    try {
      UnsignedInts.decode("-05");
      fail();
    } catch (NumberFormatException expected) {
    }
  }

  public void testToString() {
    int[] bases = {2, 5, 7, 8, 10, 16};
    for (long a : UNSIGNED_INTS) {
      for (int base : bases) {
        assertThat(Long.toString(a, base)).isEqualTo(UnsignedInts.toString((int) a, base));
      }
    }
  }

  public void testJoin() {
    assertThat(join()).isEmpty();
    assertThat(join(1)).isEqualTo("1");
    assertThat(join(1, 2)).isEqualTo("1,2");
    assertThat(join(-1, Integer.MIN_VALUE)).isEqualTo("4294967295,2147483648");

    assertThat(UnsignedInts.join("", 1, 2, 3)).isEqualTo("123");
  }

  private static String join(int... values) {
    return UnsignedInts.join(",", values);
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testNulls() {
    new NullPointerTester().testAllPublicStaticMethods(UnsignedInts.class);
  }
}

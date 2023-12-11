/*
 * Copyright (C) 2008 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.primitives;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.testing.Helpers;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import junit.framework.TestCase;

/**
 * Unit test for {@link SignedBytes}.
 *
 * @author Kevin Bourrillion
 */
@ElementTypesAreNonnullByDefault
@GwtCompatible(emulated = true)
@SuppressWarnings("cast") // redundant casts are intentional and harmless
public class SignedBytesTest extends TestCase {
  private static final byte[] EMPTY = {};
  private static final byte[] ARRAY1 = {(byte) 1};

  private static final byte LEAST = Byte.MIN_VALUE;
  private static final byte GREATEST = Byte.MAX_VALUE;

  private static final byte[] VALUES = {LEAST, -1, 0, 1, GREATEST};

  public void testCheckedCast() {
    for (byte value : VALUES) {
      assertThat(SignedBytes.checkedCast((long) value)).isEqualTo(value);
    }
    assertCastFails(GREATEST + 1L);
    assertCastFails(LEAST - 1L);
    assertCastFails(Long.MAX_VALUE);
    assertCastFails(Long.MIN_VALUE);
  }

  public void testSaturatedCast() {
    for (byte value : VALUES) {
      assertThat(SignedBytes.saturatedCast((long) value)).isEqualTo(value);
    }
    assertThat(SignedBytes.saturatedCast(GREATEST + 1L)).isEqualTo(GREATEST);
    assertThat(SignedBytes.saturatedCast(LEAST - 1L)).isEqualTo(LEAST);
    assertThat(SignedBytes.saturatedCast(Long.MAX_VALUE)).isEqualTo(GREATEST);
    assertThat(SignedBytes.saturatedCast(Long.MIN_VALUE)).isEqualTo(LEAST);
  }

  private static void assertCastFails(long value) {
    try {
      SignedBytes.checkedCast(value);
      fail("Cast to byte should have failed: " + value);
    } catch (IllegalArgumentException ex) {
      assertWithMessage(value + " not found in exception text: " + ex.getMessage())
          .that(ex.getMessage().contains(String.valueOf(value)))
          .isTrue();
    }
  }

  public void testCompare() {
    for (byte x : VALUES) {
      for (byte y : VALUES) {
        // Only compare the sign of the result of compareTo().
        int expected = Byte.valueOf(x).compareTo(y);
        int actual = SignedBytes.compare(x, y);
        if (expected == 0) {
          assertWithMessage(x + ", " + y).that(actual).isEqualTo(expected);
        } else if (expected < 0) {
          assertWithMessage(x + ", " + y + " (expected: " + expected + ", actual" + actual + ")")
              .that(actual < 0)
              .isTrue();
        } else {
          assertWithMessage(x + ", " + y + " (expected: " + expected + ", actual" + actual + ")")
              .that(actual > 0)
              .isTrue();
        }
      }
    }
  }

  public void testMax_noArgs() {
    try {
      SignedBytes.max();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMax() {
    assertThat(SignedBytes.max(LEAST)).isEqualTo(LEAST);
    assertThat(SignedBytes.max(GREATEST)).isEqualTo(GREATEST);
    assertThat(SignedBytes.max((byte) 0, (byte) -128, (byte) -1, (byte) 127, (byte) 1))
        .isEqualTo((byte) 127);
  }

  public void testMin_noArgs() {
    try {
      SignedBytes.min();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMin() {
    assertThat(SignedBytes.min(LEAST)).isEqualTo(LEAST);
    assertThat(SignedBytes.min(GREATEST)).isEqualTo(GREATEST);
    assertThat(SignedBytes.min((byte) 0, (byte) -128, (byte) -1, (byte) 127, (byte) 1))
        .isEqualTo((byte) -128);
  }

  public void testJoin() {
    assertThat(SignedBytes.join(",", EMPTY)).isEmpty();
    assertThat(SignedBytes.join(",", ARRAY1)).isEqualTo("1");
    assertThat(SignedBytes.join(",", (byte) 1, (byte) 2)).isEqualTo("1,2");
    assertThat(SignedBytes.join("", (byte) 1, (byte) 2, (byte) 3)).isEqualTo("123");
    assertThat(SignedBytes.join(",", (byte) -128, (byte) -1)).isEqualTo("-128,-1");
  }

  @J2ktIncompatible // b/285319375
  public void testLexicographicalComparator() {
    List<byte[]> ordered =
        Arrays.asList(
            new byte[] {},
            new byte[] {LEAST},
            new byte[] {LEAST, LEAST},
            new byte[] {LEAST, (byte) 1},
            new byte[] {(byte) 1},
            new byte[] {(byte) 1, LEAST},
            new byte[] {GREATEST, GREATEST - (byte) 1},
            new byte[] {GREATEST, GREATEST},
            new byte[] {GREATEST, GREATEST, GREATEST});

    Comparator<byte[]> comparator = SignedBytes.lexicographicalComparator();
    Helpers.testComparator(comparator, ordered);
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testLexicographicalComparatorSerializable() {
    Comparator<byte[]> comparator = SignedBytes.lexicographicalComparator();
    assertThat(SerializableTester.reserialize(comparator)).isSameInstanceAs(comparator);
  }

  public void testSortDescending() {
    testSortDescending(new byte[] {}, new byte[] {});
    testSortDescending(new byte[] {1}, new byte[] {1});
    testSortDescending(new byte[] {1, 2}, new byte[] {2, 1});
    testSortDescending(new byte[] {1, 3, 1}, new byte[] {3, 1, 1});
    testSortDescending(new byte[] {-1, 1, -2, 2}, new byte[] {2, 1, -1, -2});
  }

  private static void testSortDescending(byte[] input, byte[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    SignedBytes.sortDescending(input);
    assertThat(input).isEqualTo(expectedOutput);
  }

  private static void testSortDescending(
      byte[] input, int fromIndex, int toIndex, byte[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    SignedBytes.sortDescending(input, fromIndex, toIndex);
    assertThat(input).isEqualTo(expectedOutput);
  }

  public void testSortDescendingIndexed() {
    testSortDescending(new byte[] {}, 0, 0, new byte[] {});
    testSortDescending(new byte[] {1}, 0, 1, new byte[] {1});
    testSortDescending(new byte[] {1, 2}, 0, 2, new byte[] {2, 1});
    testSortDescending(new byte[] {1, 3, 1}, 0, 2, new byte[] {3, 1, 1});
    testSortDescending(new byte[] {1, 3, 1}, 0, 1, new byte[] {1, 3, 1});
    testSortDescending(new byte[] {-1, -2, 1, 2}, 1, 3, new byte[] {-1, 1, -2, 2});
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testNulls() {
    new NullPointerTester().testAllPublicStaticMethods(SignedBytes.class);
  }
}

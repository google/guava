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

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
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
      assertEquals(value, SignedBytes.checkedCast((long) value));
    }
    assertCastFails(GREATEST + 1L);
    assertCastFails(LEAST - 1L);
    assertCastFails(Long.MAX_VALUE);
    assertCastFails(Long.MIN_VALUE);
  }

  public void testSaturatedCast() {
    for (byte value : VALUES) {
      assertEquals(value, SignedBytes.saturatedCast((long) value));
    }
    assertEquals(GREATEST, SignedBytes.saturatedCast(GREATEST + 1L));
    assertEquals(LEAST, SignedBytes.saturatedCast(LEAST - 1L));
    assertEquals(GREATEST, SignedBytes.saturatedCast(Long.MAX_VALUE));
    assertEquals(LEAST, SignedBytes.saturatedCast(Long.MIN_VALUE));
  }

  private static void assertCastFails(long value) {
    try {
      SignedBytes.checkedCast(value);
      fail("Cast to byte should have failed: " + value);
    } catch (IllegalArgumentException ex) {
      assertTrue(
          value + " not found in exception text: " + ex.getMessage(),
          ex.getMessage().contains(String.valueOf(value)));
    }
  }

  public void testCompare() {
    for (byte x : VALUES) {
      for (byte y : VALUES) {
        // Only compare the sign of the result of compareTo().
        int expected = Byte.valueOf(x).compareTo(y);
        int actual = SignedBytes.compare(x, y);
        if (expected == 0) {
          assertEquals(x + ", " + y, expected, actual);
        } else if (expected < 0) {
          assertTrue(
              x + ", " + y + " (expected: " + expected + ", actual" + actual + ")", actual < 0);
        } else {
          assertTrue(
              x + ", " + y + " (expected: " + expected + ", actual" + actual + ")", actual > 0);
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
    assertEquals(LEAST, SignedBytes.max(LEAST));
    assertEquals(GREATEST, SignedBytes.max(GREATEST));
    assertEquals(
        (byte) 127, SignedBytes.max((byte) 0, (byte) -128, (byte) -1, (byte) 127, (byte) 1));
  }

  public void testMin_noArgs() {
    try {
      SignedBytes.min();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMin() {
    assertEquals(LEAST, SignedBytes.min(LEAST));
    assertEquals(GREATEST, SignedBytes.min(GREATEST));
    assertEquals(
        (byte) -128, SignedBytes.min((byte) 0, (byte) -128, (byte) -1, (byte) 127, (byte) 1));
  }

  public void testJoin() {
    assertEquals("", SignedBytes.join(",", EMPTY));
    assertEquals("1", SignedBytes.join(",", ARRAY1));
    assertEquals("1,2", SignedBytes.join(",", (byte) 1, (byte) 2));
    assertEquals("123", SignedBytes.join("", (byte) 1, (byte) 2, (byte) 3));
    assertEquals("-128,-1", SignedBytes.join(",", (byte) -128, (byte) -1));
  }

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

  @GwtIncompatible // SerializableTester
  public void testLexicographicalComparatorSerializable() {
    Comparator<byte[]> comparator = SignedBytes.lexicographicalComparator();
    assertSame(comparator, SerializableTester.reserialize(comparator));
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
    assertTrue(Arrays.equals(expectedOutput, input));
  }

  private static void testSortDescending(
      byte[] input, int fromIndex, int toIndex, byte[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    SignedBytes.sortDescending(input, fromIndex, toIndex);
    assertTrue(Arrays.equals(expectedOutput, input));
  }

  public void testSortDescendingIndexed() {
    testSortDescending(new byte[] {}, 0, 0, new byte[] {});
    testSortDescending(new byte[] {1}, 0, 1, new byte[] {1});
    testSortDescending(new byte[] {1, 2}, 0, 2, new byte[] {2, 1});
    testSortDescending(new byte[] {1, 3, 1}, 0, 2, new byte[] {3, 1, 1});
    testSortDescending(new byte[] {1, 3, 1}, 0, 1, new byte[] {1, 3, 1});
    testSortDescending(new byte[] {-1, -2, 1, 2}, 1, 3, new byte[] {-1, 1, -2, 2});
  }

  @GwtIncompatible // NullPointerTester
  public void testNulls() {
    new NullPointerTester().testAllPublicStaticMethods(SignedBytes.class);
  }
}

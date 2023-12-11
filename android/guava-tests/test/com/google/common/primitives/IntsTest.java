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
import com.google.common.base.Converter;
import com.google.common.collect.testing.Helpers;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Unit test for {@link Ints}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
@SuppressWarnings("cast") // redundant casts are intentional and harmless
public class IntsTest extends TestCase {
  private static final int[] EMPTY = {};
  private static final int[] ARRAY1 = {(int) 1};
  private static final int[] ARRAY234 = {(int) 2, (int) 3, (int) 4};

  private static final int LEAST = Integer.MIN_VALUE;
  private static final int GREATEST = Integer.MAX_VALUE;

  private static final int[] VALUES = {LEAST, (int) -1, (int) 0, (int) 1, GREATEST};

  public void testHashCode() {
    for (int value : VALUES) {
      assertThat(Ints.hashCode(value)).isEqualTo(((Integer) value).hashCode());
    }
  }

  public void testCheckedCast() {
    for (int value : VALUES) {
      assertThat(Ints.checkedCast((long) value)).isEqualTo(value);
    }
    assertCastFails(GREATEST + 1L);
    assertCastFails(LEAST - 1L);
    assertCastFails(Long.MAX_VALUE);
    assertCastFails(Long.MIN_VALUE);
  }

  public void testSaturatedCast() {
    for (int value : VALUES) {
      assertThat(Ints.saturatedCast((long) value)).isEqualTo(value);
    }
    assertThat(Ints.saturatedCast(GREATEST + 1L)).isEqualTo(GREATEST);
    assertThat(Ints.saturatedCast(LEAST - 1L)).isEqualTo(LEAST);
    assertThat(Ints.saturatedCast(Long.MAX_VALUE)).isEqualTo(GREATEST);
    assertThat(Ints.saturatedCast(Long.MIN_VALUE)).isEqualTo(LEAST);
  }

  private static void assertCastFails(long value) {
    try {
      Ints.checkedCast(value);
      fail("Cast to int should have failed: " + value);
    } catch (IllegalArgumentException ex) {
      assertWithMessage(value + " not found in exception text: " + ex.getMessage())
          .that(ex.getMessage().contains(String.valueOf(value)))
          .isTrue();
    }
  }

  public void testCompare() {
    for (int x : VALUES) {
      for (int y : VALUES) {
        // note: spec requires only that the sign is the same
        assertWithMessage(x + ", " + y)
            .that(Ints.compare(x, y))
            .isEqualTo(Integer.valueOf(x).compareTo(y));
      }
    }
  }

  public void testContains() {
    assertThat(Ints.contains(EMPTY, (int) 1)).isFalse();
    assertThat(Ints.contains(ARRAY1, (int) 2)).isFalse();
    assertThat(Ints.contains(ARRAY234, (int) 1)).isFalse();
    assertThat(Ints.contains(new int[] {(int) -1}, (int) -1)).isTrue();
    assertThat(Ints.contains(ARRAY234, (int) 2)).isTrue();
    assertThat(Ints.contains(ARRAY234, (int) 3)).isTrue();
    assertThat(Ints.contains(ARRAY234, (int) 4)).isTrue();
  }

  public void testIndexOf() {
    assertThat(Ints.indexOf(EMPTY, (int) 1)).isEqualTo(-1);
    assertThat(Ints.indexOf(ARRAY1, (int) 2)).isEqualTo(-1);
    assertThat(Ints.indexOf(ARRAY234, (int) 1)).isEqualTo(-1);
    assertThat(Ints.indexOf(new int[] {(int) -1}, (int) -1)).isEqualTo(0);
    assertThat(Ints.indexOf(ARRAY234, (int) 2)).isEqualTo(0);
    assertThat(Ints.indexOf(ARRAY234, (int) 3)).isEqualTo(1);
    assertThat(Ints.indexOf(ARRAY234, (int) 4)).isEqualTo(2);
    assertThat(Ints.indexOf(new int[] {(int) 2, (int) 3, (int) 2, (int) 3}, (int) 3)).isEqualTo(1);
  }

  public void testIndexOf_arrayTarget() {
    assertThat(Ints.indexOf(EMPTY, EMPTY)).isEqualTo(0);
    assertThat(Ints.indexOf(ARRAY234, EMPTY)).isEqualTo(0);
    assertThat(Ints.indexOf(EMPTY, ARRAY234)).isEqualTo(-1);
    assertThat(Ints.indexOf(ARRAY234, ARRAY1)).isEqualTo(-1);
    assertThat(Ints.indexOf(ARRAY1, ARRAY234)).isEqualTo(-1);
    assertThat(Ints.indexOf(ARRAY1, ARRAY1)).isEqualTo(0);
    assertThat(Ints.indexOf(ARRAY234, ARRAY234)).isEqualTo(0);
    assertThat(Ints.indexOf(ARRAY234, new int[] {(int) 2, (int) 3})).isEqualTo(0);
    assertThat(Ints.indexOf(ARRAY234, new int[] {(int) 3, (int) 4})).isEqualTo(1);
    assertThat(Ints.indexOf(ARRAY234, new int[] {(int) 3})).isEqualTo(1);
    assertThat(Ints.indexOf(ARRAY234, new int[] {(int) 4})).isEqualTo(2);
    assertThat(
            Ints.indexOf(
                new int[] {(int) 2, (int) 3, (int) 3, (int) 3, (int) 3}, new int[] {(int) 3}))
        .isEqualTo(1);
    assertThat(
            Ints.indexOf(
                new int[] {(int) 2, (int) 3, (int) 2, (int) 3, (int) 4, (int) 2, (int) 3},
                new int[] {(int) 2, (int) 3, (int) 4}))
        .isEqualTo(2);
    assertThat(
            Ints.indexOf(
                new int[] {(int) 2, (int) 2, (int) 3, (int) 4, (int) 2, (int) 3, (int) 4},
                new int[] {(int) 2, (int) 3, (int) 4}))
        .isEqualTo(1);
    assertThat(
            Ints.indexOf(
                new int[] {(int) 4, (int) 3, (int) 2}, new int[] {(int) 2, (int) 3, (int) 4}))
        .isEqualTo(-1);
  }

  public void testLastIndexOf() {
    assertThat(Ints.lastIndexOf(EMPTY, (int) 1)).isEqualTo(-1);
    assertThat(Ints.lastIndexOf(ARRAY1, (int) 2)).isEqualTo(-1);
    assertThat(Ints.lastIndexOf(ARRAY234, (int) 1)).isEqualTo(-1);
    assertThat(Ints.lastIndexOf(new int[] {(int) -1}, (int) -1)).isEqualTo(0);
    assertThat(Ints.lastIndexOf(ARRAY234, (int) 2)).isEqualTo(0);
    assertThat(Ints.lastIndexOf(ARRAY234, (int) 3)).isEqualTo(1);
    assertThat(Ints.lastIndexOf(ARRAY234, (int) 4)).isEqualTo(2);
    assertThat(Ints.lastIndexOf(new int[] {(int) 2, (int) 3, (int) 2, (int) 3}, (int) 3))
        .isEqualTo(3);
  }

  @J2ktIncompatible
  @GwtIncompatible
  public void testMax_noArgs() {
    try {
      Ints.max();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMax() {
    assertThat(Ints.max(LEAST)).isEqualTo(LEAST);
    assertThat(Ints.max(GREATEST)).isEqualTo(GREATEST);
    assertThat(Ints.max((int) 8, (int) 6, (int) 7, (int) 5, (int) 3, (int) 0, (int) 9))
        .isEqualTo((int) 9);
  }

  @J2ktIncompatible
  @GwtIncompatible
  public void testMin_noArgs() {
    try {
      Ints.min();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMin() {
    assertThat(Ints.min(LEAST)).isEqualTo(LEAST);
    assertThat(Ints.min(GREATEST)).isEqualTo(GREATEST);
    assertThat(Ints.min((int) 8, (int) 6, (int) 7, (int) 5, (int) 3, (int) 0, (int) 9))
        .isEqualTo((int) 0);
  }

  public void testConstrainToRange() {
    assertThat(Ints.constrainToRange((int) 1, (int) 0, (int) 5)).isEqualTo((int) 1);
    assertThat(Ints.constrainToRange((int) 1, (int) 1, (int) 5)).isEqualTo((int) 1);
    assertThat(Ints.constrainToRange((int) 1, (int) 3, (int) 5)).isEqualTo((int) 3);
    assertThat(Ints.constrainToRange((int) 0, (int) -5, (int) -1)).isEqualTo((int) -1);
    assertThat(Ints.constrainToRange((int) 5, (int) 2, (int) 2)).isEqualTo((int) 2);
    try {
      Ints.constrainToRange((int) 1, (int) 3, (int) 2);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testConcat() {
    assertThat(Ints.concat()).isEqualTo(EMPTY);
    assertThat(Ints.concat(EMPTY)).isEqualTo(EMPTY);
    assertThat(Ints.concat(EMPTY, EMPTY, EMPTY)).isEqualTo(EMPTY);
    assertThat(Ints.concat(ARRAY1)).isEqualTo(ARRAY1);
    assertThat(Ints.concat(ARRAY1)).isNotSameInstanceAs(ARRAY1);
    assertThat(Ints.concat(EMPTY, ARRAY1, EMPTY)).isEqualTo(ARRAY1);
    assertThat(Ints.concat(ARRAY1, ARRAY1, ARRAY1))
        .isEqualTo(new int[] {(int) 1, (int) 1, (int) 1});
    assertThat(Ints.concat(ARRAY1, ARRAY234))
        .isEqualTo(new int[] {(int) 1, (int) 2, (int) 3, (int) 4});
  }

  public void testToByteArray() {
    assertThat(Ints.toByteArray(0x12131415)).isEqualTo(new byte[] {0x12, 0x13, 0x14, 0x15});
    assertThat(Ints.toByteArray(0xFFEEDDCC))
        .isEqualTo(new byte[] {(byte) 0xFF, (byte) 0xEE, (byte) 0xDD, (byte) 0xCC});
  }

  public void testFromByteArray() {
    assertThat(Ints.fromByteArray(new byte[] {0x12, 0x13, 0x14, 0x15, 0x33})).isEqualTo(0x12131415);
    assertThat(Ints.fromByteArray(new byte[] {(byte) 0xFF, (byte) 0xEE, (byte) 0xDD, (byte) 0xCC}))
        .isEqualTo(0xFFEEDDCC);
  }

  public void testFromByteArrayFails() {
    try {
      Ints.fromByteArray(new byte[Ints.BYTES - 1]);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testFromBytes() {
    assertThat(Ints.fromBytes((byte) 0x12, (byte) 0x13, (byte) 0x14, (byte) 0x15))
        .isEqualTo(0x12131415);
    assertThat(Ints.fromBytes((byte) 0xFF, (byte) 0xEE, (byte) 0xDD, (byte) 0xCC))
        .isEqualTo(0xFFEEDDCC);
  }

  public void testByteArrayRoundTrips() {
    Random r = new Random(5);
    byte[] b = new byte[Ints.BYTES];

    // total overkill, but, it takes 0.1 sec so why not...
    for (int i = 0; i < 10000; i++) {
      int num = r.nextInt();
      assertThat(Ints.fromByteArray(Ints.toByteArray(num))).isEqualTo(num);

      r.nextBytes(b);
      assertThat(Ints.toByteArray(Ints.fromByteArray(b))).isEqualTo(b);
    }
  }

  public void testEnsureCapacity() {
    assertThat(Ints.ensureCapacity(EMPTY, 0, 1)).isSameInstanceAs(EMPTY);
    assertThat(Ints.ensureCapacity(ARRAY1, 0, 1)).isSameInstanceAs(ARRAY1);
    assertThat(Ints.ensureCapacity(ARRAY1, 1, 1)).isSameInstanceAs(ARRAY1);
    assertThat(Ints.ensureCapacity(ARRAY1, 2, 1)).isEqualTo(new int[] {(int) 1, (int) 0, (int) 0});
  }

  public void testEnsureCapacity_fail() {
    try {
      Ints.ensureCapacity(ARRAY1, -1, 1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      // notice that this should even fail when no growth was needed
      Ints.ensureCapacity(ARRAY1, 1, -1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testJoin() {
    assertThat(Ints.join(",", EMPTY)).isEmpty();
    assertThat(Ints.join(",", ARRAY1)).isEqualTo("1");
    assertThat(Ints.join(",", (int) 1, (int) 2)).isEqualTo("1,2");
    assertThat(Ints.join("", (int) 1, (int) 2, (int) 3)).isEqualTo("123");
  }

  public void testLexicographicalComparator() {
    List<int[]> ordered =
        Arrays.asList(
            new int[] {},
            new int[] {LEAST},
            new int[] {LEAST, LEAST},
            new int[] {LEAST, (int) 1},
            new int[] {(int) 1},
            new int[] {(int) 1, LEAST},
            new int[] {GREATEST, GREATEST - (int) 1},
            new int[] {GREATEST, GREATEST},
            new int[] {GREATEST, GREATEST, GREATEST});

    Comparator<int[]> comparator = Ints.lexicographicalComparator();
    Helpers.testComparator(comparator, ordered);
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testLexicographicalComparatorSerializable() {
    Comparator<int[]> comparator = Ints.lexicographicalComparator();
    assertThat(SerializableTester.reserialize(comparator)).isSameInstanceAs(comparator);
  }

  public void testReverse() {
    testReverse(new int[] {}, new int[] {});
    testReverse(new int[] {1}, new int[] {1});
    testReverse(new int[] {1, 2}, new int[] {2, 1});
    testReverse(new int[] {3, 1, 1}, new int[] {1, 1, 3});
    testReverse(new int[] {-1, 1, -2, 2}, new int[] {2, -2, 1, -1});
  }

  private static void testReverse(int[] input, int[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Ints.reverse(input);
    assertThat(input).isEqualTo(expectedOutput);
  }

  private static void testReverse(int[] input, int fromIndex, int toIndex, int[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Ints.reverse(input, fromIndex, toIndex);
    assertThat(input).isEqualTo(expectedOutput);
  }

  public void testReverseIndexed() {
    testReverse(new int[] {}, 0, 0, new int[] {});
    testReverse(new int[] {1}, 0, 1, new int[] {1});
    testReverse(new int[] {1, 2}, 0, 2, new int[] {2, 1});
    testReverse(new int[] {3, 1, 1}, 0, 2, new int[] {1, 3, 1});
    testReverse(new int[] {3, 1, 1}, 0, 1, new int[] {3, 1, 1});
    testReverse(new int[] {-1, 1, -2, 2}, 1, 3, new int[] {-1, -2, 1, 2});
  }

  private static void testRotate(int[] input, int distance, int[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Ints.rotate(input, distance);
    assertThat(input).isEqualTo(expectedOutput);
  }

  private static void testRotate(
      int[] input, int distance, int fromIndex, int toIndex, int[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Ints.rotate(input, distance, fromIndex, toIndex);
    assertThat(input).isEqualTo(expectedOutput);
  }

  public void testRotate() {
    testRotate(new int[] {}, -1, new int[] {});
    testRotate(new int[] {}, 0, new int[] {});
    testRotate(new int[] {}, 1, new int[] {});

    testRotate(new int[] {1}, -2, new int[] {1});
    testRotate(new int[] {1}, -1, new int[] {1});
    testRotate(new int[] {1}, 0, new int[] {1});
    testRotate(new int[] {1}, 1, new int[] {1});
    testRotate(new int[] {1}, 2, new int[] {1});

    testRotate(new int[] {1, 2}, -3, new int[] {2, 1});
    testRotate(new int[] {1, 2}, -1, new int[] {2, 1});
    testRotate(new int[] {1, 2}, -2, new int[] {1, 2});
    testRotate(new int[] {1, 2}, 0, new int[] {1, 2});
    testRotate(new int[] {1, 2}, 1, new int[] {2, 1});
    testRotate(new int[] {1, 2}, 2, new int[] {1, 2});
    testRotate(new int[] {1, 2}, 3, new int[] {2, 1});

    testRotate(new int[] {1, 2, 3}, -5, new int[] {3, 1, 2});
    testRotate(new int[] {1, 2, 3}, -4, new int[] {2, 3, 1});
    testRotate(new int[] {1, 2, 3}, -3, new int[] {1, 2, 3});
    testRotate(new int[] {1, 2, 3}, -2, new int[] {3, 1, 2});
    testRotate(new int[] {1, 2, 3}, -1, new int[] {2, 3, 1});
    testRotate(new int[] {1, 2, 3}, 0, new int[] {1, 2, 3});
    testRotate(new int[] {1, 2, 3}, 1, new int[] {3, 1, 2});
    testRotate(new int[] {1, 2, 3}, 2, new int[] {2, 3, 1});
    testRotate(new int[] {1, 2, 3}, 3, new int[] {1, 2, 3});
    testRotate(new int[] {1, 2, 3}, 4, new int[] {3, 1, 2});
    testRotate(new int[] {1, 2, 3}, 5, new int[] {2, 3, 1});

    testRotate(new int[] {1, 2, 3, 4}, -9, new int[] {2, 3, 4, 1});
    testRotate(new int[] {1, 2, 3, 4}, -5, new int[] {2, 3, 4, 1});
    testRotate(new int[] {1, 2, 3, 4}, -1, new int[] {2, 3, 4, 1});
    testRotate(new int[] {1, 2, 3, 4}, 0, new int[] {1, 2, 3, 4});
    testRotate(new int[] {1, 2, 3, 4}, 1, new int[] {4, 1, 2, 3});
    testRotate(new int[] {1, 2, 3, 4}, 5, new int[] {4, 1, 2, 3});
    testRotate(new int[] {1, 2, 3, 4}, 9, new int[] {4, 1, 2, 3});

    testRotate(new int[] {1, 2, 3, 4, 5}, -6, new int[] {2, 3, 4, 5, 1});
    testRotate(new int[] {1, 2, 3, 4, 5}, -4, new int[] {5, 1, 2, 3, 4});
    testRotate(new int[] {1, 2, 3, 4, 5}, -3, new int[] {4, 5, 1, 2, 3});
    testRotate(new int[] {1, 2, 3, 4, 5}, -1, new int[] {2, 3, 4, 5, 1});
    testRotate(new int[] {1, 2, 3, 4, 5}, 0, new int[] {1, 2, 3, 4, 5});
    testRotate(new int[] {1, 2, 3, 4, 5}, 1, new int[] {5, 1, 2, 3, 4});
    testRotate(new int[] {1, 2, 3, 4, 5}, 3, new int[] {3, 4, 5, 1, 2});
    testRotate(new int[] {1, 2, 3, 4, 5}, 4, new int[] {2, 3, 4, 5, 1});
    testRotate(new int[] {1, 2, 3, 4, 5}, 6, new int[] {5, 1, 2, 3, 4});
  }

  public void testRotateIndexed() {
    testRotate(new int[] {}, 0, 0, 0, new int[] {});

    testRotate(new int[] {1}, 0, 0, 1, new int[] {1});
    testRotate(new int[] {1}, 1, 0, 1, new int[] {1});
    testRotate(new int[] {1}, 1, 1, 1, new int[] {1});

    // Rotate the central 5 elements, leaving the ends as-is
    testRotate(new int[] {0, 1, 2, 3, 4, 5, 6}, -6, 1, 6, new int[] {0, 2, 3, 4, 5, 1, 6});
    testRotate(new int[] {0, 1, 2, 3, 4, 5, 6}, -1, 1, 6, new int[] {0, 2, 3, 4, 5, 1, 6});
    testRotate(new int[] {0, 1, 2, 3, 4, 5, 6}, 0, 1, 6, new int[] {0, 1, 2, 3, 4, 5, 6});
    testRotate(new int[] {0, 1, 2, 3, 4, 5, 6}, 5, 1, 6, new int[] {0, 1, 2, 3, 4, 5, 6});
    testRotate(new int[] {0, 1, 2, 3, 4, 5, 6}, 14, 1, 6, new int[] {0, 2, 3, 4, 5, 1, 6});

    // Rotate the first three elements
    testRotate(new int[] {0, 1, 2, 3, 4, 5, 6}, -2, 0, 3, new int[] {2, 0, 1, 3, 4, 5, 6});
    testRotate(new int[] {0, 1, 2, 3, 4, 5, 6}, -1, 0, 3, new int[] {1, 2, 0, 3, 4, 5, 6});
    testRotate(new int[] {0, 1, 2, 3, 4, 5, 6}, 0, 0, 3, new int[] {0, 1, 2, 3, 4, 5, 6});
    testRotate(new int[] {0, 1, 2, 3, 4, 5, 6}, 1, 0, 3, new int[] {2, 0, 1, 3, 4, 5, 6});
    testRotate(new int[] {0, 1, 2, 3, 4, 5, 6}, 2, 0, 3, new int[] {1, 2, 0, 3, 4, 5, 6});

    // Rotate the last four elements
    testRotate(new int[] {0, 1, 2, 3, 4, 5, 6}, -6, 3, 7, new int[] {0, 1, 2, 5, 6, 3, 4});
    testRotate(new int[] {0, 1, 2, 3, 4, 5, 6}, -5, 3, 7, new int[] {0, 1, 2, 4, 5, 6, 3});
    testRotate(new int[] {0, 1, 2, 3, 4, 5, 6}, -4, 3, 7, new int[] {0, 1, 2, 3, 4, 5, 6});
    testRotate(new int[] {0, 1, 2, 3, 4, 5, 6}, -3, 3, 7, new int[] {0, 1, 2, 6, 3, 4, 5});
    testRotate(new int[] {0, 1, 2, 3, 4, 5, 6}, -2, 3, 7, new int[] {0, 1, 2, 5, 6, 3, 4});
    testRotate(new int[] {0, 1, 2, 3, 4, 5, 6}, -1, 3, 7, new int[] {0, 1, 2, 4, 5, 6, 3});
    testRotate(new int[] {0, 1, 2, 3, 4, 5, 6}, 0, 3, 7, new int[] {0, 1, 2, 3, 4, 5, 6});
    testRotate(new int[] {0, 1, 2, 3, 4, 5, 6}, 1, 3, 7, new int[] {0, 1, 2, 6, 3, 4, 5});
    testRotate(new int[] {0, 1, 2, 3, 4, 5, 6}, 2, 3, 7, new int[] {0, 1, 2, 5, 6, 3, 4});
    testRotate(new int[] {0, 1, 2, 3, 4, 5, 6}, 3, 3, 7, new int[] {0, 1, 2, 4, 5, 6, 3});
  }

  public void testSortDescending() {
    testSortDescending(new int[] {}, new int[] {});
    testSortDescending(new int[] {1}, new int[] {1});
    testSortDescending(new int[] {1, 2}, new int[] {2, 1});
    testSortDescending(new int[] {1, 3, 1}, new int[] {3, 1, 1});
    testSortDescending(new int[] {-1, 1, -2, 2}, new int[] {2, 1, -1, -2});
  }

  private static void testSortDescending(int[] input, int[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Ints.sortDescending(input);
    assertThat(input).isEqualTo(expectedOutput);
  }

  private static void testSortDescending(
      int[] input, int fromIndex, int toIndex, int[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Ints.sortDescending(input, fromIndex, toIndex);
    assertThat(input).isEqualTo(expectedOutput);
  }

  public void testSortDescendingIndexed() {
    testSortDescending(new int[] {}, 0, 0, new int[] {});
    testSortDescending(new int[] {1}, 0, 1, new int[] {1});
    testSortDescending(new int[] {1, 2}, 0, 2, new int[] {2, 1});
    testSortDescending(new int[] {1, 3, 1}, 0, 2, new int[] {3, 1, 1});
    testSortDescending(new int[] {1, 3, 1}, 0, 1, new int[] {1, 3, 1});
    testSortDescending(new int[] {-1, -2, 1, 2}, 1, 3, new int[] {-1, 1, -2, 2});
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testStringConverterSerialization() {
    SerializableTester.reserializeAndAssert(Ints.stringConverter());
  }

  public void testToArray() {
    // need explicit type parameter to avoid javac warning!?
    List<Integer> none = Arrays.<Integer>asList();
    assertThat(Ints.toArray(none)).isEqualTo(EMPTY);

    List<Integer> one = Arrays.asList((int) 1);
    assertThat(Ints.toArray(one)).isEqualTo(ARRAY1);

    int[] array = {(int) 0, (int) 1, (int) 0xdeadbeef};

    List<Integer> three = Arrays.asList((int) 0, (int) 1, (int) 0xdeadbeef);
    assertThat(Ints.toArray(three)).isEqualTo(array);

    assertThat(Ints.toArray(Ints.asList(array))).isEqualTo(array);
  }

  public void testToArray_threadSafe() {
    for (int delta : new int[] {+1, 0, -1}) {
      for (int i = 0; i < VALUES.length; i++) {
        List<Integer> list = Ints.asList(VALUES).subList(0, i);
        Collection<Integer> misleadingSize = Helpers.misleadingSizeCollection(delta);
        misleadingSize.addAll(list);
        int[] arr = Ints.toArray(misleadingSize);
        assertThat(arr).hasLength(i);
        for (int j = 0; j < i; j++) {
          assertThat(arr[j]).isEqualTo(VALUES[j]);
        }
      }
    }
  }

  public void testToArray_withNull() {
    List<@Nullable Integer> list = Arrays.asList((int) 0, (int) 1, null);
    try {
      Ints.toArray(list);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testToArray_withConversion() {
    int[] array = {0, 1, 2};

    List<Byte> bytes = Arrays.asList((byte) 0, (byte) 1, (byte) 2);
    List<Short> shorts = Arrays.asList((short) 0, (short) 1, (short) 2);
    List<Integer> ints = Arrays.asList(0, 1, 2);
    List<Float> floats = Arrays.asList((float) 0, (float) 1, (float) 2);
    List<Long> longs = Arrays.asList((long) 0, (long) 1, (long) 2);
    List<Double> doubles = Arrays.asList((double) 0, (double) 1, (double) 2);

    assertThat(Ints.toArray(bytes)).isEqualTo(array);
    assertThat(Ints.toArray(shorts)).isEqualTo(array);
    assertThat(Ints.toArray(ints)).isEqualTo(array);
    assertThat(Ints.toArray(floats)).isEqualTo(array);
    assertThat(Ints.toArray(longs)).isEqualTo(array);
    assertThat(Ints.toArray(doubles)).isEqualTo(array);
  }

  @J2ktIncompatible // b/285319375
  public void testAsList_isAView() {
    int[] array = {(int) 0, (int) 1};
    List<Integer> list = Ints.asList(array);
    list.set(0, (int) 2);
    assertThat(array).isEqualTo(new int[] {(int) 2, (int) 1});
    array[1] = (int) 3;
    assertThat(list).containsExactly((int) 2, (int) 3).inOrder();
  }

  public void testAsList_toArray_roundTrip() {
    int[] array = {(int) 0, (int) 1, (int) 2};
    List<Integer> list = Ints.asList(array);
    int[] newArray = Ints.toArray(list);

    // Make sure it returned a copy
    list.set(0, (int) 4);
    assertThat(newArray).isEqualTo(new int[] {(int) 0, (int) 1, (int) 2});
    newArray[1] = (int) 5;
    assertThat((int) list.get(1)).isEqualTo((int) 1);
  }

  // This test stems from a real bug found by andrewk
  public void testAsList_subList_toArray_roundTrip() {
    int[] array = {(int) 0, (int) 1, (int) 2, (int) 3};
    List<Integer> list = Ints.asList(array);
    assertThat(Ints.toArray(list.subList(1, 3))).isEqualTo(new int[] {(int) 1, (int) 2});
    assertThat(Ints.toArray(list.subList(2, 2))).isEqualTo(new int[] {});
  }

  public void testAsListEmpty() {
    assertThat(Ints.asList(EMPTY)).isSameInstanceAs(Collections.emptyList());
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testNulls() {
    new NullPointerTester().testAllPublicStaticMethods(Ints.class);
  }

  public void testStringConverter_convert() {
    Converter<String, Integer> converter = Ints.stringConverter();
    assertThat(converter.convert("1")).isEqualTo((Integer) 1);
    assertThat(converter.convert("0")).isEqualTo((Integer) 0);
    assertThat(converter.convert("-1")).isEqualTo((Integer) (-1));
    assertThat(converter.convert("0xff")).isEqualTo((Integer) 255);
    assertThat(converter.convert("0xFF")).isEqualTo((Integer) 255);
    assertThat(converter.convert("-0xFF")).isEqualTo((Integer) (-255));
    assertThat(converter.convert("#0000FF")).isEqualTo((Integer) 255);
    assertThat(converter.convert("0666")).isEqualTo((Integer) 438);
  }

  public void testStringConverter_convertError() {
    try {
      Ints.stringConverter().convert("notanumber");
      fail();
    } catch (NumberFormatException expected) {
    }
  }

  public void testStringConverter_nullConversions() {
    assertThat(Ints.stringConverter().convert(null)).isNull();
    assertThat(Ints.stringConverter().reverse().convert(null)).isNull();
  }

  public void testStringConverter_reverse() {
    Converter<String, Integer> converter = Ints.stringConverter();
    assertThat(converter.reverse().convert(1)).isEqualTo("1");
    assertThat(converter.reverse().convert(0)).isEqualTo("0");
    assertThat(converter.reverse().convert(-1)).isEqualTo("-1");
    assertThat(converter.reverse().convert(0xff)).isEqualTo("255");
    assertThat(converter.reverse().convert(0xFF)).isEqualTo("255");
    assertThat(converter.reverse().convert(-0xFF)).isEqualTo("-255");
    assertThat(converter.reverse().convert(0666)).isEqualTo("438");
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testStringConverter_nullPointerTester() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicInstanceMethods(Ints.stringConverter());
  }

  public void testTryParse() {
    tryParseAndAssertEquals(0, "0");
    tryParseAndAssertEquals(0, "-0");
    tryParseAndAssertEquals(1, "1");
    tryParseAndAssertEquals(-1, "-1");
    tryParseAndAssertEquals(8900, "8900");
    tryParseAndAssertEquals(-8900, "-8900");
    tryParseAndAssertEquals(GREATEST, Integer.toString(GREATEST));
    tryParseAndAssertEquals(LEAST, Integer.toString(LEAST));
    assertThat(Ints.tryParse("")).isNull();
    assertThat(Ints.tryParse("-")).isNull();
    assertThat(Ints.tryParse("+1")).isNull();
    assertThat(Ints.tryParse("9999999999999999")).isNull();
    assertWithMessage("Max integer + 1")
        .that(Ints.tryParse(Long.toString(((long) GREATEST) + 1)))
        .isNull();
    assertWithMessage("Max integer * 10")
        .that(Ints.tryParse(Long.toString(((long) GREATEST) * 10)))
        .isNull();
    assertWithMessage("Min integer - 1")
        .that(Ints.tryParse(Long.toString(((long) LEAST) - 1)))
        .isNull();
    assertWithMessage("Min integer * 10")
        .that(Ints.tryParse(Long.toString(((long) LEAST) * 10)))
        .isNull();
    assertWithMessage("Max long").that(Ints.tryParse(Long.toString(Long.MAX_VALUE))).isNull();
    assertWithMessage("Min long").that(Ints.tryParse(Long.toString(Long.MIN_VALUE))).isNull();
    assertThat(Ints.tryParse("\u0662\u06f3")).isNull();
  }

  /**
   * Applies {@link Ints#tryParse(String)} to the given string and asserts that the result is as
   * expected.
   */
  private static void tryParseAndAssertEquals(Integer expected, String value) {
    assertThat(Ints.tryParse(value)).isEqualTo(expected);
  }

  public void testTryParse_radix() {
    for (int radix = Character.MIN_RADIX; radix <= Character.MAX_RADIX; radix++) {
      radixEncodeParseAndAssertEquals(0, radix);
      radixEncodeParseAndAssertEquals(8000, radix);
      radixEncodeParseAndAssertEquals(-8000, radix);
      radixEncodeParseAndAssertEquals(GREATEST, radix);
      radixEncodeParseAndAssertEquals(LEAST, radix);
      assertWithMessage("Radix: " + radix).that(Ints.tryParse("9999999999999999", radix)).isNull();
      assertWithMessage("Radix: " + radix)
          .that(Ints.tryParse(Long.toString((long) GREATEST + 1, radix), radix))
          .isNull();
      assertWithMessage("Radix: " + radix)
          .that(Ints.tryParse(Long.toString((long) LEAST - 1, radix), radix))
          .isNull();
    }
    assertWithMessage("Hex string and dec parm").that(Ints.tryParse("FFFF", 10)).isNull();
    assertWithMessage("Mixed hex case").that((int) Ints.tryParse("ffFF", 16)).isEqualTo(65535);
  }

  /**
   * Encodes an integer as a string with given radix, then uses {@link Ints#tryParse(String, int)}
   * to parse the result. Asserts the result is the same as what we started with.
   */
  private static void radixEncodeParseAndAssertEquals(Integer value, int radix) {
    assertWithMessage("Radix: " + radix)
        .that(Ints.tryParse(Integer.toString(value, radix), radix))
        .isEqualTo(value);
  }

  public void testTryParse_radixTooBig() {
    try {
      Ints.tryParse("0", Character.MAX_RADIX + 1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testTryParse_radixTooSmall() {
    try {
      Ints.tryParse("0", Character.MIN_RADIX - 1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testTryParse_withNullGwt() {
    assertThat(Ints.tryParse("null")).isNull();
    try {
      Ints.tryParse(null);
      fail("Expected NPE");
    } catch (NullPointerException expected) {
    }
  }
}

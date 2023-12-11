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
import static java.lang.Long.MAX_VALUE;
import static java.lang.Long.MIN_VALUE;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.base.Converter;
import com.google.common.collect.testing.Helpers;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Unit test for {@link Longs}.
 *
 * @author Kevin Bourrillion
 */
@ElementTypesAreNonnullByDefault
@GwtCompatible(emulated = true)
@SuppressWarnings("cast") // redundant casts are intentional and harmless
public class LongsTest extends TestCase {
  private static final long[] EMPTY = {};
  private static final long[] ARRAY1 = {(long) 1};
  private static final long[] ARRAY234 = {(long) 2, (long) 3, (long) 4};

  private static final long[] VALUES = {MIN_VALUE, (long) -1, (long) 0, (long) 1, MAX_VALUE};

  @J2ktIncompatible
  @GwtIncompatible // Long.hashCode returns different values in GWT.
  public void testHashCode() {
    for (long value : VALUES) {
      assertWithMessage("hashCode for " + value)
          .that(Longs.hashCode(value))
          .isEqualTo(((Long) value).hashCode());
    }
  }

  public void testCompare() {
    for (long x : VALUES) {
      for (long y : VALUES) {
        // note: spec requires only that the sign is the same
        assertWithMessage(x + ", " + y)
            .that(Longs.compare(x, y))
            .isEqualTo(Long.valueOf(x).compareTo(y));
      }
    }
  }

  public void testContains() {
    assertThat(Longs.contains(EMPTY, (long) 1)).isFalse();
    assertThat(Longs.contains(ARRAY1, (long) 2)).isFalse();
    assertThat(Longs.contains(ARRAY234, (long) 1)).isFalse();
    assertThat(Longs.contains(new long[] {(long) -1}, (long) -1)).isTrue();
    assertThat(Longs.contains(ARRAY234, (long) 2)).isTrue();
    assertThat(Longs.contains(ARRAY234, (long) 3)).isTrue();
    assertThat(Longs.contains(ARRAY234, (long) 4)).isTrue();
  }

  public void testIndexOf() {
    assertThat(Longs.indexOf(EMPTY, (long) 1)).isEqualTo(-1);
    assertThat(Longs.indexOf(ARRAY1, (long) 2)).isEqualTo(-1);
    assertThat(Longs.indexOf(ARRAY234, (long) 1)).isEqualTo(-1);
    assertThat(Longs.indexOf(new long[] {(long) -1}, (long) -1)).isEqualTo(0);
    assertThat(Longs.indexOf(ARRAY234, (long) 2)).isEqualTo(0);
    assertThat(Longs.indexOf(ARRAY234, (long) 3)).isEqualTo(1);
    assertThat(Longs.indexOf(ARRAY234, (long) 4)).isEqualTo(2);
    assertThat(Longs.indexOf(new long[] {(long) 2, (long) 3, (long) 2, (long) 3}, (long) 3))
        .isEqualTo(1);
  }

  public void testIndexOf_arrayTarget() {
    assertThat(Longs.indexOf(EMPTY, EMPTY)).isEqualTo(0);
    assertThat(Longs.indexOf(ARRAY234, EMPTY)).isEqualTo(0);
    assertThat(Longs.indexOf(EMPTY, ARRAY234)).isEqualTo(-1);
    assertThat(Longs.indexOf(ARRAY234, ARRAY1)).isEqualTo(-1);
    assertThat(Longs.indexOf(ARRAY1, ARRAY234)).isEqualTo(-1);
    assertThat(Longs.indexOf(ARRAY1, ARRAY1)).isEqualTo(0);
    assertThat(Longs.indexOf(ARRAY234, ARRAY234)).isEqualTo(0);
    assertThat(Longs.indexOf(ARRAY234, new long[] {(long) 2, (long) 3})).isEqualTo(0);
    assertThat(Longs.indexOf(ARRAY234, new long[] {(long) 3, (long) 4})).isEqualTo(1);
    assertThat(Longs.indexOf(ARRAY234, new long[] {(long) 3})).isEqualTo(1);
    assertThat(Longs.indexOf(ARRAY234, new long[] {(long) 4})).isEqualTo(2);
    assertThat(
            Longs.indexOf(
                new long[] {(long) 2, (long) 3, (long) 3, (long) 3, (long) 3},
                new long[] {(long) 3}))
        .isEqualTo(1);
    assertThat(
            Longs.indexOf(
                new long[] {(long) 2, (long) 3, (long) 2, (long) 3, (long) 4, (long) 2, (long) 3},
                new long[] {(long) 2, (long) 3, (long) 4}))
        .isEqualTo(2);
    assertThat(
            Longs.indexOf(
                new long[] {(long) 2, (long) 2, (long) 3, (long) 4, (long) 2, (long) 3, (long) 4},
                new long[] {(long) 2, (long) 3, (long) 4}))
        .isEqualTo(1);
    assertThat(
            Longs.indexOf(
                new long[] {(long) 4, (long) 3, (long) 2},
                new long[] {(long) 2, (long) 3, (long) 4}))
        .isEqualTo(-1);
  }

  public void testLastIndexOf() {
    assertThat(Longs.lastIndexOf(EMPTY, (long) 1)).isEqualTo(-1);
    assertThat(Longs.lastIndexOf(ARRAY1, (long) 2)).isEqualTo(-1);
    assertThat(Longs.lastIndexOf(ARRAY234, (long) 1)).isEqualTo(-1);
    assertThat(Longs.lastIndexOf(new long[] {(long) -1}, (long) -1)).isEqualTo(0);
    assertThat(Longs.lastIndexOf(ARRAY234, (long) 2)).isEqualTo(0);
    assertThat(Longs.lastIndexOf(ARRAY234, (long) 3)).isEqualTo(1);
    assertThat(Longs.lastIndexOf(ARRAY234, (long) 4)).isEqualTo(2);
    assertThat(Longs.lastIndexOf(new long[] {(long) 2, (long) 3, (long) 2, (long) 3}, (long) 3))
        .isEqualTo(3);
  }

  public void testMax_noArgs() {
    try {
      Longs.max();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMax() {
    assertThat(Longs.max(MIN_VALUE)).isEqualTo(MIN_VALUE);
    assertThat(Longs.max(MAX_VALUE)).isEqualTo(MAX_VALUE);
    assertThat(Longs.max((long) 8, (long) 6, (long) 7, (long) 5, (long) 3, (long) 0, (long) 9))
        .isEqualTo((long) 9);
  }

  public void testMin_noArgs() {
    try {
      Longs.min();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMin() {
    assertThat(Longs.min(MIN_VALUE)).isEqualTo(MIN_VALUE);
    assertThat(Longs.min(MAX_VALUE)).isEqualTo(MAX_VALUE);
    assertThat(Longs.min((long) 8, (long) 6, (long) 7, (long) 5, (long) 3, (long) 0, (long) 9))
        .isEqualTo((long) 0);
  }

  public void testConstrainToRange() {
    assertThat(Longs.constrainToRange((long) 1, (long) 0, (long) 5)).isEqualTo((long) 1);
    assertThat(Longs.constrainToRange((long) 1, (long) 1, (long) 5)).isEqualTo((long) 1);
    assertThat(Longs.constrainToRange((long) 1, (long) 3, (long) 5)).isEqualTo((long) 3);
    assertThat(Longs.constrainToRange((long) 0, (long) -5, (long) -1)).isEqualTo((long) -1);
    assertThat(Longs.constrainToRange((long) 5, (long) 2, (long) 2)).isEqualTo((long) 2);
    try {
      Longs.constrainToRange((long) 1, (long) 3, (long) 2);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testConcat() {
    assertThat(Longs.concat()).isEqualTo(EMPTY);
    assertThat(Longs.concat(EMPTY)).isEqualTo(EMPTY);
    assertThat(Longs.concat(EMPTY, EMPTY, EMPTY)).isEqualTo(EMPTY);
    assertThat(Longs.concat(ARRAY1)).isEqualTo(ARRAY1);
    assertThat(Longs.concat(ARRAY1)).isNotSameInstanceAs(ARRAY1);
    assertThat(Longs.concat(EMPTY, ARRAY1, EMPTY)).isEqualTo(ARRAY1);
    assertThat(Longs.concat(ARRAY1, ARRAY1, ARRAY1))
        .isEqualTo(new long[] {(long) 1, (long) 1, (long) 1});
    assertThat(Longs.concat(ARRAY1, ARRAY234))
        .isEqualTo(new long[] {(long) 1, (long) 2, (long) 3, (long) 4});
  }

  @GwtIncompatible // different overflow behavior; could probably be made to work by using ~~
  public void testConcat_overflow_negative() {
    int dim1 = 1 << 16;
    int dim2 = 1 << 15;
    assertThat(dim1 * dim2).isLessThan(0);
    testConcat_overflow(dim1, dim2);
  }

  @GwtIncompatible // different overflow behavior; could probably be made to work by using ~~
  public void testConcat_overflow_nonNegative() {
    int dim1 = 1 << 16;
    int dim2 = 1 << 16;
    assertThat(dim1 * dim2).isAtLeast(0);
    testConcat_overflow(dim1, dim2);
  }

  private static void testConcat_overflow(int arraysDim1, int arraysDim2) {
    assertThat((long) arraysDim1 * arraysDim2).isNotEqualTo((long) (arraysDim1 * arraysDim2));

    long[][] arrays = new long[arraysDim1][];
    // it's shared to avoid using too much memory in tests
    long[] sharedArray = new long[arraysDim2];
    Arrays.fill(arrays, sharedArray);

    try {
      Longs.concat(arrays);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  private static void assertByteArrayEquals(byte[] expected, byte[] actual) {
    assertWithMessage(
            "Expected: " + Arrays.toString(expected) + ", but got: " + Arrays.toString(actual))
        .that(Arrays.equals(expected, actual))
        .isTrue();
  }

  public void testToByteArray() {
    assertByteArrayEquals(
        new byte[] {0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19},
        Longs.toByteArray(0x1213141516171819L));
    assertByteArrayEquals(
        new byte[] {
          (byte) 0xFF, (byte) 0xEE, (byte) 0xDD, (byte) 0xCC,
          (byte) 0xBB, (byte) 0xAA, (byte) 0x99, (byte) 0x88
        },
        Longs.toByteArray(0xFFEEDDCCBBAA9988L));
  }

  public void testFromByteArray() {
    assertThat(
            Longs.fromByteArray(new byte[] {0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x33}))
        .isEqualTo(0x1213141516171819L);
    assertThat(
            Longs.fromByteArray(
                new byte[] {
                  (byte) 0xFF, (byte) 0xEE, (byte) 0xDD, (byte) 0xCC,
                  (byte) 0xBB, (byte) 0xAA, (byte) 0x99, (byte) 0x88
                }))
        .isEqualTo(0xFFEEDDCCBBAA9988L);
  }

  public void testFromByteArrayFails() {
    try {
      Longs.fromByteArray(new byte[Longs.BYTES - 1]);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testFromBytes() {
    assertThat(
            Longs.fromBytes(
                (byte) 0x12,
                (byte) 0x13,
                (byte) 0x14,
                (byte) 0x15,
                (byte) 0x16,
                (byte) 0x17,
                (byte) 0x18,
                (byte) 0x19))
        .isEqualTo(0x1213141516171819L);
    assertThat(
            Longs.fromBytes(
                (byte) 0xFF,
                (byte) 0xEE,
                (byte) 0xDD,
                (byte) 0xCC,
                (byte) 0xBB,
                (byte) 0xAA,
                (byte) 0x99,
                (byte) 0x88))
        .isEqualTo(0xFFEEDDCCBBAA9988L);
  }

  public void testByteArrayRoundTrips() {
    Random r = new Random(5);
    byte[] b = new byte[Longs.BYTES];

    for (int i = 0; i < 1000; i++) {
      long num = r.nextLong();
      assertThat(Longs.fromByteArray(Longs.toByteArray(num))).isEqualTo(num);

      r.nextBytes(b);
      long value = Longs.fromByteArray(b);
      assertWithMessage("" + value).that(Arrays.equals(b, Longs.toByteArray(value))).isTrue();
    }
  }

  public void testEnsureCapacity() {
    assertThat(Longs.ensureCapacity(EMPTY, 0, 1)).isSameInstanceAs(EMPTY);
    assertThat(Longs.ensureCapacity(ARRAY1, 0, 1)).isSameInstanceAs(ARRAY1);
    assertThat(Longs.ensureCapacity(ARRAY1, 1, 1)).isSameInstanceAs(ARRAY1);
    assertThat(Longs.ensureCapacity(ARRAY1, 2, 1))
        .isEqualTo(new long[] {(long) 1, (long) 0, (long) 0});
  }

  public void testEnsureCapacity_fail() {
    try {
      Longs.ensureCapacity(ARRAY1, -1, 1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      // notice that this should even fail when no growth was needed
      Longs.ensureCapacity(ARRAY1, 1, -1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testJoin() {
    assertThat(Longs.join(",", EMPTY)).isEmpty();
    assertThat(Longs.join(",", ARRAY1)).isEqualTo("1");
    assertThat(Longs.join(",", (long) 1, (long) 2)).isEqualTo("1,2");
    assertThat(Longs.join("", (long) 1, (long) 2, (long) 3)).isEqualTo("123");
  }

  public void testLexicographicalComparator() {
    List<long[]> ordered =
        Arrays.asList(
            new long[] {},
            new long[] {MIN_VALUE},
            new long[] {MIN_VALUE, MIN_VALUE},
            new long[] {MIN_VALUE, (long) 1},
            new long[] {(long) 1},
            new long[] {(long) 1, MIN_VALUE},
            new long[] {MAX_VALUE, MAX_VALUE - (long) 1},
            new long[] {MAX_VALUE, MAX_VALUE},
            new long[] {MAX_VALUE, MAX_VALUE, MAX_VALUE});

    Comparator<long[]> comparator = Longs.lexicographicalComparator();
    Helpers.testComparator(comparator, ordered);
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testLexicographicalComparatorSerializable() {
    Comparator<long[]> comparator = Longs.lexicographicalComparator();
    assertThat(SerializableTester.reserialize(comparator)).isSameInstanceAs(comparator);
  }

  public void testReverse() {
    testReverse(new long[] {}, new long[] {});
    testReverse(new long[] {1}, new long[] {1});
    testReverse(new long[] {1, 2}, new long[] {2, 1});
    testReverse(new long[] {3, 1, 1}, new long[] {1, 1, 3});
    testReverse(new long[] {-1, 1, -2, 2}, new long[] {2, -2, 1, -1});
  }

  private static void testReverse(long[] input, long[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Longs.reverse(input);
    assertThat(input).isEqualTo(expectedOutput);
  }

  private static void testReverse(long[] input, int fromIndex, int toIndex, long[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Longs.reverse(input, fromIndex, toIndex);
    assertThat(input).isEqualTo(expectedOutput);
  }

  public void testReverseIndexed() {
    testReverse(new long[] {}, 0, 0, new long[] {});
    testReverse(new long[] {1}, 0, 1, new long[] {1});
    testReverse(new long[] {1, 2}, 0, 2, new long[] {2, 1});
    testReverse(new long[] {3, 1, 1}, 0, 2, new long[] {1, 3, 1});
    testReverse(new long[] {3, 1, 1}, 0, 1, new long[] {3, 1, 1});
    testReverse(new long[] {-1, 1, -2, 2}, 1, 3, new long[] {-1, -2, 1, 2});
  }

  private static void testRotate(long[] input, int distance, long[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Longs.rotate(input, distance);
    assertThat(input).isEqualTo(expectedOutput);
  }

  private static void testRotate(
      long[] input, int distance, int fromIndex, int toIndex, long[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Longs.rotate(input, distance, fromIndex, toIndex);
    assertThat(input).isEqualTo(expectedOutput);
  }

  public void testRotate() {
    testRotate(new long[] {}, -1, new long[] {});
    testRotate(new long[] {}, 0, new long[] {});
    testRotate(new long[] {}, 1, new long[] {});

    testRotate(new long[] {1}, -2, new long[] {1});
    testRotate(new long[] {1}, -1, new long[] {1});
    testRotate(new long[] {1}, 0, new long[] {1});
    testRotate(new long[] {1}, 1, new long[] {1});
    testRotate(new long[] {1}, 2, new long[] {1});

    testRotate(new long[] {1, 2}, -3, new long[] {2, 1});
    testRotate(new long[] {1, 2}, -1, new long[] {2, 1});
    testRotate(new long[] {1, 2}, -2, new long[] {1, 2});
    testRotate(new long[] {1, 2}, 0, new long[] {1, 2});
    testRotate(new long[] {1, 2}, 1, new long[] {2, 1});
    testRotate(new long[] {1, 2}, 2, new long[] {1, 2});
    testRotate(new long[] {1, 2}, 3, new long[] {2, 1});

    testRotate(new long[] {1, 2, 3}, -5, new long[] {3, 1, 2});
    testRotate(new long[] {1, 2, 3}, -4, new long[] {2, 3, 1});
    testRotate(new long[] {1, 2, 3}, -3, new long[] {1, 2, 3});
    testRotate(new long[] {1, 2, 3}, -2, new long[] {3, 1, 2});
    testRotate(new long[] {1, 2, 3}, -1, new long[] {2, 3, 1});
    testRotate(new long[] {1, 2, 3}, 0, new long[] {1, 2, 3});
    testRotate(new long[] {1, 2, 3}, 1, new long[] {3, 1, 2});
    testRotate(new long[] {1, 2, 3}, 2, new long[] {2, 3, 1});
    testRotate(new long[] {1, 2, 3}, 3, new long[] {1, 2, 3});
    testRotate(new long[] {1, 2, 3}, 4, new long[] {3, 1, 2});
    testRotate(new long[] {1, 2, 3}, 5, new long[] {2, 3, 1});

    testRotate(new long[] {1, 2, 3, 4}, -9, new long[] {2, 3, 4, 1});
    testRotate(new long[] {1, 2, 3, 4}, -5, new long[] {2, 3, 4, 1});
    testRotate(new long[] {1, 2, 3, 4}, -1, new long[] {2, 3, 4, 1});
    testRotate(new long[] {1, 2, 3, 4}, 0, new long[] {1, 2, 3, 4});
    testRotate(new long[] {1, 2, 3, 4}, 1, new long[] {4, 1, 2, 3});
    testRotate(new long[] {1, 2, 3, 4}, 5, new long[] {4, 1, 2, 3});
    testRotate(new long[] {1, 2, 3, 4}, 9, new long[] {4, 1, 2, 3});

    testRotate(new long[] {1, 2, 3, 4, 5}, -6, new long[] {2, 3, 4, 5, 1});
    testRotate(new long[] {1, 2, 3, 4, 5}, -4, new long[] {5, 1, 2, 3, 4});
    testRotate(new long[] {1, 2, 3, 4, 5}, -3, new long[] {4, 5, 1, 2, 3});
    testRotate(new long[] {1, 2, 3, 4, 5}, -1, new long[] {2, 3, 4, 5, 1});
    testRotate(new long[] {1, 2, 3, 4, 5}, 0, new long[] {1, 2, 3, 4, 5});
    testRotate(new long[] {1, 2, 3, 4, 5}, 1, new long[] {5, 1, 2, 3, 4});
    testRotate(new long[] {1, 2, 3, 4, 5}, 3, new long[] {3, 4, 5, 1, 2});
    testRotate(new long[] {1, 2, 3, 4, 5}, 4, new long[] {2, 3, 4, 5, 1});
    testRotate(new long[] {1, 2, 3, 4, 5}, 6, new long[] {5, 1, 2, 3, 4});
  }

  public void testRotateIndexed() {
    testRotate(new long[] {}, 0, 0, 0, new long[] {});

    testRotate(new long[] {1}, 0, 0, 1, new long[] {1});
    testRotate(new long[] {1}, 1, 0, 1, new long[] {1});
    testRotate(new long[] {1}, 1, 1, 1, new long[] {1});

    // Rotate the central 5 elements, leaving the ends as-is
    testRotate(new long[] {0, 1, 2, 3, 4, 5, 6}, -6, 1, 6, new long[] {0, 2, 3, 4, 5, 1, 6});
    testRotate(new long[] {0, 1, 2, 3, 4, 5, 6}, -1, 1, 6, new long[] {0, 2, 3, 4, 5, 1, 6});
    testRotate(new long[] {0, 1, 2, 3, 4, 5, 6}, 0, 1, 6, new long[] {0, 1, 2, 3, 4, 5, 6});
    testRotate(new long[] {0, 1, 2, 3, 4, 5, 6}, 5, 1, 6, new long[] {0, 1, 2, 3, 4, 5, 6});
    testRotate(new long[] {0, 1, 2, 3, 4, 5, 6}, 14, 1, 6, new long[] {0, 2, 3, 4, 5, 1, 6});

    // Rotate the first three elements
    testRotate(new long[] {0, 1, 2, 3, 4, 5, 6}, -2, 0, 3, new long[] {2, 0, 1, 3, 4, 5, 6});
    testRotate(new long[] {0, 1, 2, 3, 4, 5, 6}, -1, 0, 3, new long[] {1, 2, 0, 3, 4, 5, 6});
    testRotate(new long[] {0, 1, 2, 3, 4, 5, 6}, 0, 0, 3, new long[] {0, 1, 2, 3, 4, 5, 6});
    testRotate(new long[] {0, 1, 2, 3, 4, 5, 6}, 1, 0, 3, new long[] {2, 0, 1, 3, 4, 5, 6});
    testRotate(new long[] {0, 1, 2, 3, 4, 5, 6}, 2, 0, 3, new long[] {1, 2, 0, 3, 4, 5, 6});

    // Rotate the last four elements
    testRotate(new long[] {0, 1, 2, 3, 4, 5, 6}, -6, 3, 7, new long[] {0, 1, 2, 5, 6, 3, 4});
    testRotate(new long[] {0, 1, 2, 3, 4, 5, 6}, -5, 3, 7, new long[] {0, 1, 2, 4, 5, 6, 3});
    testRotate(new long[] {0, 1, 2, 3, 4, 5, 6}, -4, 3, 7, new long[] {0, 1, 2, 3, 4, 5, 6});
    testRotate(new long[] {0, 1, 2, 3, 4, 5, 6}, -3, 3, 7, new long[] {0, 1, 2, 6, 3, 4, 5});
    testRotate(new long[] {0, 1, 2, 3, 4, 5, 6}, -2, 3, 7, new long[] {0, 1, 2, 5, 6, 3, 4});
    testRotate(new long[] {0, 1, 2, 3, 4, 5, 6}, -1, 3, 7, new long[] {0, 1, 2, 4, 5, 6, 3});
    testRotate(new long[] {0, 1, 2, 3, 4, 5, 6}, 0, 3, 7, new long[] {0, 1, 2, 3, 4, 5, 6});
    testRotate(new long[] {0, 1, 2, 3, 4, 5, 6}, 1, 3, 7, new long[] {0, 1, 2, 6, 3, 4, 5});
    testRotate(new long[] {0, 1, 2, 3, 4, 5, 6}, 2, 3, 7, new long[] {0, 1, 2, 5, 6, 3, 4});
    testRotate(new long[] {0, 1, 2, 3, 4, 5, 6}, 3, 3, 7, new long[] {0, 1, 2, 4, 5, 6, 3});
  }

  public void testSortDescending() {
    testSortDescending(new long[] {}, new long[] {});
    testSortDescending(new long[] {1}, new long[] {1});
    testSortDescending(new long[] {1, 2}, new long[] {2, 1});
    testSortDescending(new long[] {1, 3, 1}, new long[] {3, 1, 1});
    testSortDescending(new long[] {-1, 1, -2, 2}, new long[] {2, 1, -1, -2});
  }

  private static void testSortDescending(long[] input, long[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Longs.sortDescending(input);
    assertThat(input).isEqualTo(expectedOutput);
  }

  private static void testSortDescending(
      long[] input, int fromIndex, int toIndex, long[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Longs.sortDescending(input, fromIndex, toIndex);
    assertThat(input).isEqualTo(expectedOutput);
  }

  public void testSortDescendingIndexed() {
    testSortDescending(new long[] {}, 0, 0, new long[] {});
    testSortDescending(new long[] {1}, 0, 1, new long[] {1});
    testSortDescending(new long[] {1, 2}, 0, 2, new long[] {2, 1});
    testSortDescending(new long[] {1, 3, 1}, 0, 2, new long[] {3, 1, 1});
    testSortDescending(new long[] {1, 3, 1}, 0, 1, new long[] {1, 3, 1});
    testSortDescending(new long[] {-1, -2, 1, 2}, 1, 3, new long[] {-1, 1, -2, 2});
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testStringConverterSerialization() {
    SerializableTester.reserializeAndAssert(Longs.stringConverter());
  }

  public void testToArray() {
    // need explicit type parameter to avoid javac warning!?
    List<Long> none = Arrays.<Long>asList();
    assertThat(Longs.toArray(none)).isEqualTo(EMPTY);

    List<Long> one = Arrays.asList((long) 1);
    assertThat(Longs.toArray(one)).isEqualTo(ARRAY1);

    long[] array = {(long) 0, (long) 1, 0x0FF1C1AL};

    List<Long> three = Arrays.asList((long) 0, (long) 1, 0x0FF1C1AL);
    assertThat(Longs.toArray(three)).isEqualTo(array);

    assertThat(Longs.toArray(Longs.asList(array))).isEqualTo(array);
  }

  public void testToArray_threadSafe() {
    for (int delta : new int[] {+1, 0, -1}) {
      for (int i = 0; i < VALUES.length; i++) {
        List<Long> list = Longs.asList(VALUES).subList(0, i);
        Collection<Long> misleadingSize = Helpers.misleadingSizeCollection(delta);
        misleadingSize.addAll(list);
        long[] arr = Longs.toArray(misleadingSize);
        assertThat(arr).hasLength(i);
        for (int j = 0; j < i; j++) {
          assertThat(arr[j]).isEqualTo(VALUES[j]);
        }
      }
    }
  }

  public void testToArray_withNull() {
    List<@Nullable Long> list = Arrays.asList((long) 0, (long) 1, null);
    try {
      Longs.toArray(list);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testToArray_withConversion() {
    long[] array = {(long) 0, (long) 1, (long) 2};

    List<Byte> bytes = Arrays.asList((byte) 0, (byte) 1, (byte) 2);
    List<Short> shorts = Arrays.asList((short) 0, (short) 1, (short) 2);
    List<Integer> ints = Arrays.asList(0, 1, 2);
    List<Float> floats = Arrays.asList((float) 0, (float) 1, (float) 2);
    List<Long> longs = Arrays.asList((long) 0, (long) 1, (long) 2);
    List<Double> doubles = Arrays.asList((double) 0, (double) 1, (double) 2);

    assertThat(Longs.toArray(bytes)).isEqualTo(array);
    assertThat(Longs.toArray(shorts)).isEqualTo(array);
    assertThat(Longs.toArray(ints)).isEqualTo(array);
    assertThat(Longs.toArray(floats)).isEqualTo(array);
    assertThat(Longs.toArray(longs)).isEqualTo(array);
    assertThat(Longs.toArray(doubles)).isEqualTo(array);
  }

  @J2ktIncompatible // b/285319375
  public void testAsList_isAView() {
    long[] array = {(long) 0, (long) 1};
    List<Long> list = Longs.asList(array);
    list.set(0, (long) 2);
    assertThat(array).isEqualTo(new long[] {(long) 2, (long) 1});
    array[1] = (long) 3;
    assertThat(list).containsExactly((long) 2, (long) 3).inOrder();
  }

  public void testAsList_toArray_roundTrip() {
    long[] array = {(long) 0, (long) 1, (long) 2};
    List<Long> list = Longs.asList(array);
    long[] newArray = Longs.toArray(list);

    // Make sure it returned a copy
    list.set(0, (long) 4);
    assertThat(newArray).isEqualTo(new long[] {(long) 0, (long) 1, (long) 2});
    newArray[1] = (long) 5;
    assertThat((long) list.get(1)).isEqualTo((long) 1);
  }

  // This test stems from a real bug found by andrewk
  public void testAsList_subList_toArray_roundTrip() {
    long[] array = {(long) 0, (long) 1, (long) 2, (long) 3};
    List<Long> list = Longs.asList(array);
    assertThat(Longs.toArray(list.subList(1, 3))).isEqualTo(new long[] {(long) 1, (long) 2});
    assertThat(Longs.toArray(list.subList(2, 2))).isEqualTo(new long[] {});
  }

  public void testAsListEmpty() {
    assertThat(Longs.asList(EMPTY)).isSameInstanceAs(Collections.emptyList());
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testNulls() {
    new NullPointerTester().testAllPublicStaticMethods(Longs.class);
  }

  public void testStringConverter_convert() {
    Converter<String, Long> converter = Longs.stringConverter();
    assertThat(converter.convert("1")).isEqualTo((Long) 1L);
    assertThat(converter.convert("0")).isEqualTo((Long) 0L);
    assertThat(converter.convert("-1")).isEqualTo((Long) (-1L));
    assertThat(converter.convert("0xff")).isEqualTo((Long) 255L);
    assertThat(converter.convert("0xFF")).isEqualTo((Long) 255L);
    assertThat(converter.convert("-0xFF")).isEqualTo((Long) (-255L));
    assertThat(converter.convert("#0000FF")).isEqualTo((Long) 255L);
    assertThat(converter.convert("0666")).isEqualTo((Long) 438L);
  }

  public void testStringConverter_convertError() {
    try {
      Longs.stringConverter().convert("notanumber");
      fail();
    } catch (NumberFormatException expected) {
    }
  }

  public void testStringConverter_nullConversions() {
    assertThat(Longs.stringConverter().convert(null)).isNull();
    assertThat(Longs.stringConverter().reverse().convert(null)).isNull();
  }

  public void testStringConverter_reverse() {
    Converter<String, Long> converter = Longs.stringConverter();
    assertThat(converter.reverse().convert(1L)).isEqualTo("1");
    assertThat(converter.reverse().convert(0L)).isEqualTo("0");
    assertThat(converter.reverse().convert(-1L)).isEqualTo("-1");
    assertThat(converter.reverse().convert(0xffL)).isEqualTo("255");
    assertThat(converter.reverse().convert(0xFFL)).isEqualTo("255");
    assertThat(converter.reverse().convert(-0xFFL)).isEqualTo("-255");
    assertThat(converter.reverse().convert(0666L)).isEqualTo("438");
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testStringConverter_nullPointerTester() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicInstanceMethods(Longs.stringConverter());
  }

  public void testTryParse() {
    tryParseAndAssertEquals(0L, "0");
    tryParseAndAssertEquals(0L, "-0");
    tryParseAndAssertEquals(1L, "1");
    tryParseAndAssertEquals(-1L, "-1");
    tryParseAndAssertEquals(8900L, "8900");
    tryParseAndAssertEquals(-8900L, "-8900");
    tryParseAndAssertEquals(MAX_VALUE, Long.toString(MAX_VALUE));
    tryParseAndAssertEquals(MIN_VALUE, Long.toString(MIN_VALUE));
    assertThat(Longs.tryParse("")).isNull();
    assertThat(Longs.tryParse("-")).isNull();
    assertThat(Longs.tryParse("+1")).isNull();
    assertThat(Longs.tryParse("999999999999999999999999")).isNull();
    assertWithMessage("Max long + 1")
        .that(Longs.tryParse(BigInteger.valueOf(MAX_VALUE).add(BigInteger.ONE).toString()))
        .isNull();
    assertWithMessage("Max long * 10")
        .that(Longs.tryParse(BigInteger.valueOf(MAX_VALUE).multiply(BigInteger.TEN).toString()))
        .isNull();
    assertWithMessage("Min long - 1")
        .that(Longs.tryParse(BigInteger.valueOf(MIN_VALUE).subtract(BigInteger.ONE).toString()))
        .isNull();
    assertWithMessage("Min long * 10")
        .that(Longs.tryParse(BigInteger.valueOf(MIN_VALUE).multiply(BigInteger.TEN).toString()))
        .isNull();
    assertThat(Longs.tryParse("\u0662\u06f3")).isNull();
  }

  /**
   * Applies {@link Longs#tryParse(String)} to the given string and asserts that the result is as
   * expected.
   */
  private static void tryParseAndAssertEquals(Long expected, String value) {
    assertThat(Longs.tryParse(value)).isEqualTo(expected);
  }

  public void testTryParse_radix() {
    for (int radix = Character.MIN_RADIX; radix <= Character.MAX_RADIX; radix++) {
      radixEncodeParseAndAssertEquals((long) 0, radix);
      radixEncodeParseAndAssertEquals((long) 8000, radix);
      radixEncodeParseAndAssertEquals((long) -8000, radix);
      radixEncodeParseAndAssertEquals(MAX_VALUE, radix);
      radixEncodeParseAndAssertEquals(MIN_VALUE, radix);
      assertWithMessage("Radix: " + radix)
          .that(Longs.tryParse("999999999999999999999999", radix))
          .isNull();
      assertWithMessage("Radix: " + radix)
          .that(Longs.tryParse(BigInteger.valueOf(MAX_VALUE).add(BigInteger.ONE).toString(), radix))
          .isNull();
      assertWithMessage("Radix: " + radix)
          .that(
              Longs.tryParse(
                  BigInteger.valueOf(MIN_VALUE).subtract(BigInteger.ONE).toString(), radix))
          .isNull();
    }
    assertWithMessage("Hex string and dec parm").that(Longs.tryParse("FFFF", 10)).isNull();
    assertWithMessage("Mixed hex case")
        .that(Longs.tryParse("ffFF", 16).longValue())
        .isEqualTo(65535);
  }

  /**
   * Encodes the long as a string with given radix, then uses {@link Longs#tryParse(String, int)} to
   * parse the result. Asserts the result is the same as what we started with.
   */
  private static void radixEncodeParseAndAssertEquals(Long value, int radix) {
    assertWithMessage("Radix: " + radix)
        .that(Longs.tryParse(Long.toString(value, radix), radix))
        .isEqualTo(value);
  }

  public void testTryParse_radixTooBig() {
    try {
      Longs.tryParse("0", Character.MAX_RADIX + 1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testTryParse_radixTooSmall() {
    try {
      Longs.tryParse("0", Character.MIN_RADIX - 1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testTryParse_withNullGwt() {
    assertThat(Longs.tryParse("null")).isNull();
    try {
      Longs.tryParse(null);
      fail("Expected NPE");
    } catch (NullPointerException expected) {
    }
  }
}

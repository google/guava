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
 * Unit test for {@link Shorts}.
 *
 * @author Kevin Bourrillion
 */
@ElementTypesAreNonnullByDefault
@GwtCompatible(emulated = true)
@SuppressWarnings("cast") // redundant casts are intentional and harmless
public class ShortsTest extends TestCase {
  private static final short[] EMPTY = {};
  private static final short[] ARRAY1 = {(short) 1};
  private static final short[] ARRAY234 = {(short) 2, (short) 3, (short) 4};

  private static final short LEAST = Short.MIN_VALUE;
  private static final short GREATEST = Short.MAX_VALUE;

  private static final short[] VALUES = {LEAST, (short) -1, (short) 0, (short) 1, GREATEST};

  public void testHashCode() {
    for (short value : VALUES) {
      assertThat(Shorts.hashCode(value)).isEqualTo(((Short) value).hashCode());
    }
  }

  public void testCheckedCast() {
    for (short value : VALUES) {
      assertThat(Shorts.checkedCast((long) value)).isEqualTo(value);
    }
    assertCastFails(GREATEST + 1L);
    assertCastFails(LEAST - 1L);
    assertCastFails(Long.MAX_VALUE);
    assertCastFails(Long.MIN_VALUE);
  }

  public void testSaturatedCast() {
    for (short value : VALUES) {
      assertThat(Shorts.saturatedCast((long) value)).isEqualTo(value);
    }
    assertThat(Shorts.saturatedCast(GREATEST + 1L)).isEqualTo(GREATEST);
    assertThat(Shorts.saturatedCast(LEAST - 1L)).isEqualTo(LEAST);
    assertThat(Shorts.saturatedCast(Long.MAX_VALUE)).isEqualTo(GREATEST);
    assertThat(Shorts.saturatedCast(Long.MIN_VALUE)).isEqualTo(LEAST);
  }

  private static void assertCastFails(long value) {
    try {
      Shorts.checkedCast(value);
      fail("Cast to short should have failed: " + value);
    } catch (IllegalArgumentException ex) {
      assertWithMessage(value + " not found in exception text: " + ex.getMessage())
          .that(ex.getMessage().contains(String.valueOf(value)))
          .isTrue();
    }
  }

  public void testCompare() {
    for (short x : VALUES) {
      for (short y : VALUES) {
        // Only compare the sign of the result of compareTo().
        int expected = Short.valueOf(x).compareTo(y);
        int actual = Shorts.compare(x, y);
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

  public void testContains() {
    assertThat(Shorts.contains(EMPTY, (short) 1)).isFalse();
    assertThat(Shorts.contains(ARRAY1, (short) 2)).isFalse();
    assertThat(Shorts.contains(ARRAY234, (short) 1)).isFalse();
    assertThat(Shorts.contains(new short[] {(short) -1}, (short) -1)).isTrue();
    assertThat(Shorts.contains(ARRAY234, (short) 2)).isTrue();
    assertThat(Shorts.contains(ARRAY234, (short) 3)).isTrue();
    assertThat(Shorts.contains(ARRAY234, (short) 4)).isTrue();
  }

  public void testIndexOf() {
    assertThat(Shorts.indexOf(EMPTY, (short) 1)).isEqualTo(-1);
    assertThat(Shorts.indexOf(ARRAY1, (short) 2)).isEqualTo(-1);
    assertThat(Shorts.indexOf(ARRAY234, (short) 1)).isEqualTo(-1);
    assertThat(Shorts.indexOf(new short[] {(short) -1}, (short) -1)).isEqualTo(0);
    assertThat(Shorts.indexOf(ARRAY234, (short) 2)).isEqualTo(0);
    assertThat(Shorts.indexOf(ARRAY234, (short) 3)).isEqualTo(1);
    assertThat(Shorts.indexOf(ARRAY234, (short) 4)).isEqualTo(2);
    assertThat(Shorts.indexOf(new short[] {(short) 2, (short) 3, (short) 2, (short) 3}, (short) 3))
        .isEqualTo(1);
  }

  public void testIndexOf_arrayTarget() {
    assertThat(Shorts.indexOf(EMPTY, EMPTY)).isEqualTo(0);
    assertThat(Shorts.indexOf(ARRAY234, EMPTY)).isEqualTo(0);
    assertThat(Shorts.indexOf(EMPTY, ARRAY234)).isEqualTo(-1);
    assertThat(Shorts.indexOf(ARRAY234, ARRAY1)).isEqualTo(-1);
    assertThat(Shorts.indexOf(ARRAY1, ARRAY234)).isEqualTo(-1);
    assertThat(Shorts.indexOf(ARRAY1, ARRAY1)).isEqualTo(0);
    assertThat(Shorts.indexOf(ARRAY234, ARRAY234)).isEqualTo(0);
    assertThat(Shorts.indexOf(ARRAY234, new short[] {(short) 2, (short) 3})).isEqualTo(0);
    assertThat(Shorts.indexOf(ARRAY234, new short[] {(short) 3, (short) 4})).isEqualTo(1);
    assertThat(Shorts.indexOf(ARRAY234, new short[] {(short) 3})).isEqualTo(1);
    assertThat(Shorts.indexOf(ARRAY234, new short[] {(short) 4})).isEqualTo(2);
    assertThat(
            Shorts.indexOf(
                new short[] {(short) 2, (short) 3, (short) 3, (short) 3, (short) 3},
                new short[] {(short) 3}))
        .isEqualTo(1);
    assertThat(
            Shorts.indexOf(
                new short[] {
                  (short) 2, (short) 3, (short) 2, (short) 3, (short) 4, (short) 2, (short) 3
                },
                new short[] {(short) 2, (short) 3, (short) 4}))
        .isEqualTo(2);
    assertThat(
            Shorts.indexOf(
                new short[] {
                  (short) 2, (short) 2, (short) 3, (short) 4, (short) 2, (short) 3, (short) 4
                },
                new short[] {(short) 2, (short) 3, (short) 4}))
        .isEqualTo(1);
    assertThat(
            Shorts.indexOf(
                new short[] {(short) 4, (short) 3, (short) 2},
                new short[] {(short) 2, (short) 3, (short) 4}))
        .isEqualTo(-1);
  }

  public void testLastIndexOf() {
    assertThat(Shorts.lastIndexOf(EMPTY, (short) 1)).isEqualTo(-1);
    assertThat(Shorts.lastIndexOf(ARRAY1, (short) 2)).isEqualTo(-1);
    assertThat(Shorts.lastIndexOf(ARRAY234, (short) 1)).isEqualTo(-1);
    assertThat(Shorts.lastIndexOf(new short[] {(short) -1}, (short) -1)).isEqualTo(0);
    assertThat(Shorts.lastIndexOf(ARRAY234, (short) 2)).isEqualTo(0);
    assertThat(Shorts.lastIndexOf(ARRAY234, (short) 3)).isEqualTo(1);
    assertThat(Shorts.lastIndexOf(ARRAY234, (short) 4)).isEqualTo(2);
    assertThat(
            Shorts.lastIndexOf(new short[] {(short) 2, (short) 3, (short) 2, (short) 3}, (short) 3))
        .isEqualTo(3);
  }

  @J2ktIncompatible
  @GwtIncompatible
  public void testMax_noArgs() {
    try {
      Shorts.max();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMax() {
    assertThat(Shorts.max(LEAST)).isEqualTo(LEAST);
    assertThat(Shorts.max(GREATEST)).isEqualTo(GREATEST);
    assertThat(
            Shorts.max((short) 8, (short) 6, (short) 7, (short) 5, (short) 3, (short) 0, (short) 9))
        .isEqualTo((short) 9);
  }

  @J2ktIncompatible
  @GwtIncompatible
  public void testMin_noArgs() {
    try {
      Shorts.min();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMin() {
    assertThat(Shorts.min(LEAST)).isEqualTo(LEAST);
    assertThat(Shorts.min(GREATEST)).isEqualTo(GREATEST);
    assertThat(
            Shorts.min((short) 8, (short) 6, (short) 7, (short) 5, (short) 3, (short) 0, (short) 9))
        .isEqualTo((short) 0);
  }

  public void testConstrainToRange() {
    assertThat(Shorts.constrainToRange((short) 1, (short) 0, (short) 5)).isEqualTo((short) 1);
    assertThat(Shorts.constrainToRange((short) 1, (short) 1, (short) 5)).isEqualTo((short) 1);
    assertThat(Shorts.constrainToRange((short) 1, (short) 3, (short) 5)).isEqualTo((short) 3);
    assertThat(Shorts.constrainToRange((short) 0, (short) -5, (short) -1)).isEqualTo((short) -1);
    assertThat(Shorts.constrainToRange((short) 5, (short) 2, (short) 2)).isEqualTo((short) 2);
    try {
      Shorts.constrainToRange((short) 1, (short) 3, (short) 2);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testConcat() {
    assertThat(Shorts.concat()).isEqualTo(EMPTY);
    assertThat(Shorts.concat(EMPTY)).isEqualTo(EMPTY);
    assertThat(Shorts.concat(EMPTY, EMPTY, EMPTY)).isEqualTo(EMPTY);
    assertThat(Shorts.concat(ARRAY1)).isEqualTo(ARRAY1);
    assertThat(Shorts.concat(ARRAY1)).isNotSameInstanceAs(ARRAY1);
    assertThat(Shorts.concat(EMPTY, ARRAY1, EMPTY)).isEqualTo(ARRAY1);
    assertThat(Shorts.concat(ARRAY1, ARRAY1, ARRAY1))
        .isEqualTo(new short[] {(short) 1, (short) 1, (short) 1});
    assertThat(Shorts.concat(ARRAY1, ARRAY234))
        .isEqualTo(new short[] {(short) 1, (short) 2, (short) 3, (short) 4});
  }

  @J2ktIncompatible
  @GwtIncompatible // Shorts.toByteArray
  public void testToByteArray() {
    assertThat(Shorts.toByteArray((short) 0x2345)).isEqualTo(new byte[] {0x23, 0x45});
    assertThat(Shorts.toByteArray((short) 0xFEDC)).isEqualTo(new byte[] {(byte) 0xFE, (byte) 0xDC});
  }

  @J2ktIncompatible
  @GwtIncompatible // Shorts.fromByteArray
  public void testFromByteArray() {
    assertThat(Shorts.fromByteArray(new byte[] {0x23, 0x45})).isEqualTo((short) 0x2345);
    assertThat(Shorts.fromByteArray(new byte[] {(byte) 0xFE, (byte) 0xDC}))
        .isEqualTo((short) 0xFEDC);
  }

  @J2ktIncompatible
  @GwtIncompatible // Shorts.fromByteArray
  public void testFromByteArrayFails() {
    try {
      Shorts.fromByteArray(new byte[] {0x01});
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // Shorts.fromBytes
  public void testFromBytes() {
    assertThat(Shorts.fromBytes((byte) 0x23, (byte) 0x45)).isEqualTo((short) 0x2345);
    assertThat(Shorts.fromBytes((byte) 0xFE, (byte) 0xDC)).isEqualTo((short) 0xFEDC);
  }

  @J2ktIncompatible
  @GwtIncompatible // Shorts.fromByteArray, Shorts.toByteArray
  public void testByteArrayRoundTrips() {
    Random r = new Random(5);
    byte[] b = new byte[Shorts.BYTES];

    // total overkill, but, it takes 0.1 sec so why not...
    for (int i = 0; i < 10000; i++) {
      short num = (short) r.nextInt();
      assertThat(Shorts.fromByteArray(Shorts.toByteArray(num))).isEqualTo(num);

      r.nextBytes(b);
      assertThat(Shorts.toByteArray(Shorts.fromByteArray(b))).isEqualTo(b);
    }
  }

  public void testEnsureCapacity() {
    assertThat(Shorts.ensureCapacity(EMPTY, 0, 1)).isSameInstanceAs(EMPTY);
    assertThat(Shorts.ensureCapacity(ARRAY1, 0, 1)).isSameInstanceAs(ARRAY1);
    assertThat(Shorts.ensureCapacity(ARRAY1, 1, 1)).isSameInstanceAs(ARRAY1);
    assertThat(Shorts.ensureCapacity(ARRAY1, 2, 1))
        .isEqualTo(new short[] {(short) 1, (short) 0, (short) 0});
  }

  public void testEnsureCapacity_fail() {
    try {
      Shorts.ensureCapacity(ARRAY1, -1, 1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      // notice that this should even fail when no growth was needed
      Shorts.ensureCapacity(ARRAY1, 1, -1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testJoin() {
    assertThat(Shorts.join(",", EMPTY)).isEmpty();
    assertThat(Shorts.join(",", ARRAY1)).isEqualTo("1");
    assertThat(Shorts.join(",", (short) 1, (short) 2)).isEqualTo("1,2");
    assertThat(Shorts.join("", (short) 1, (short) 2, (short) 3)).isEqualTo("123");
  }

  @J2ktIncompatible // TODO(b/285297472): Enable
  public void testLexicographicalComparator() {
    List<short[]> ordered =
        Arrays.asList(
            new short[] {},
            new short[] {LEAST},
            new short[] {LEAST, LEAST},
            new short[] {LEAST, (short) 1},
            new short[] {(short) 1},
            new short[] {(short) 1, LEAST},
            new short[] {GREATEST, GREATEST - (short) 1},
            new short[] {GREATEST, GREATEST},
            new short[] {GREATEST, GREATEST, GREATEST});

    Comparator<short[]> comparator = Shorts.lexicographicalComparator();
    Helpers.testComparator(comparator, ordered);
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testLexicographicalComparatorSerializable() {
    Comparator<short[]> comparator = Shorts.lexicographicalComparator();
    assertThat(SerializableTester.reserialize(comparator)).isSameInstanceAs(comparator);
  }

  public void testReverse() {
    testReverse(new short[] {}, new short[] {});
    testReverse(new short[] {1}, new short[] {1});
    testReverse(new short[] {1, 2}, new short[] {2, 1});
    testReverse(new short[] {3, 1, 1}, new short[] {1, 1, 3});
    testReverse(new short[] {-1, 1, -2, 2}, new short[] {2, -2, 1, -1});
  }

  private static void testReverse(short[] input, short[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Shorts.reverse(input);
    assertThat(input).isEqualTo(expectedOutput);
  }

  private static void testReverse(
      short[] input, int fromIndex, int toIndex, short[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Shorts.reverse(input, fromIndex, toIndex);
    assertThat(input).isEqualTo(expectedOutput);
  }

  public void testReverseIndexed() {
    testReverse(new short[] {}, 0, 0, new short[] {});
    testReverse(new short[] {1}, 0, 1, new short[] {1});
    testReverse(new short[] {1, 2}, 0, 2, new short[] {2, 1});
    testReverse(new short[] {3, 1, 1}, 0, 2, new short[] {1, 3, 1});
    testReverse(new short[] {3, 1, 1}, 0, 1, new short[] {3, 1, 1});
    testReverse(new short[] {-1, 1, -2, 2}, 1, 3, new short[] {-1, -2, 1, 2});
  }

  private static void testRotate(short[] input, int distance, short[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Shorts.rotate(input, distance);
    assertThat(input).isEqualTo(expectedOutput);
  }

  private static void testRotate(
      short[] input, int distance, int fromIndex, int toIndex, short[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Shorts.rotate(input, distance, fromIndex, toIndex);
    assertThat(input).isEqualTo(expectedOutput);
  }

  public void testRotate() {
    testRotate(new short[] {}, -1, new short[] {});
    testRotate(new short[] {}, 0, new short[] {});
    testRotate(new short[] {}, 1, new short[] {});

    testRotate(new short[] {1}, -2, new short[] {1});
    testRotate(new short[] {1}, -1, new short[] {1});
    testRotate(new short[] {1}, 0, new short[] {1});
    testRotate(new short[] {1}, 1, new short[] {1});
    testRotate(new short[] {1}, 2, new short[] {1});

    testRotate(new short[] {1, 2}, -3, new short[] {2, 1});
    testRotate(new short[] {1, 2}, -1, new short[] {2, 1});
    testRotate(new short[] {1, 2}, -2, new short[] {1, 2});
    testRotate(new short[] {1, 2}, 0, new short[] {1, 2});
    testRotate(new short[] {1, 2}, 1, new short[] {2, 1});
    testRotate(new short[] {1, 2}, 2, new short[] {1, 2});
    testRotate(new short[] {1, 2}, 3, new short[] {2, 1});

    testRotate(new short[] {1, 2, 3}, -5, new short[] {3, 1, 2});
    testRotate(new short[] {1, 2, 3}, -4, new short[] {2, 3, 1});
    testRotate(new short[] {1, 2, 3}, -3, new short[] {1, 2, 3});
    testRotate(new short[] {1, 2, 3}, -2, new short[] {3, 1, 2});
    testRotate(new short[] {1, 2, 3}, -1, new short[] {2, 3, 1});
    testRotate(new short[] {1, 2, 3}, 0, new short[] {1, 2, 3});
    testRotate(new short[] {1, 2, 3}, 1, new short[] {3, 1, 2});
    testRotate(new short[] {1, 2, 3}, 2, new short[] {2, 3, 1});
    testRotate(new short[] {1, 2, 3}, 3, new short[] {1, 2, 3});
    testRotate(new short[] {1, 2, 3}, 4, new short[] {3, 1, 2});
    testRotate(new short[] {1, 2, 3}, 5, new short[] {2, 3, 1});

    testRotate(new short[] {1, 2, 3, 4}, -9, new short[] {2, 3, 4, 1});
    testRotate(new short[] {1, 2, 3, 4}, -5, new short[] {2, 3, 4, 1});
    testRotate(new short[] {1, 2, 3, 4}, -1, new short[] {2, 3, 4, 1});
    testRotate(new short[] {1, 2, 3, 4}, 0, new short[] {1, 2, 3, 4});
    testRotate(new short[] {1, 2, 3, 4}, 1, new short[] {4, 1, 2, 3});
    testRotate(new short[] {1, 2, 3, 4}, 5, new short[] {4, 1, 2, 3});
    testRotate(new short[] {1, 2, 3, 4}, 9, new short[] {4, 1, 2, 3});

    testRotate(new short[] {1, 2, 3, 4, 5}, -6, new short[] {2, 3, 4, 5, 1});
    testRotate(new short[] {1, 2, 3, 4, 5}, -4, new short[] {5, 1, 2, 3, 4});
    testRotate(new short[] {1, 2, 3, 4, 5}, -3, new short[] {4, 5, 1, 2, 3});
    testRotate(new short[] {1, 2, 3, 4, 5}, -1, new short[] {2, 3, 4, 5, 1});
    testRotate(new short[] {1, 2, 3, 4, 5}, 0, new short[] {1, 2, 3, 4, 5});
    testRotate(new short[] {1, 2, 3, 4, 5}, 1, new short[] {5, 1, 2, 3, 4});
    testRotate(new short[] {1, 2, 3, 4, 5}, 3, new short[] {3, 4, 5, 1, 2});
    testRotate(new short[] {1, 2, 3, 4, 5}, 4, new short[] {2, 3, 4, 5, 1});
    testRotate(new short[] {1, 2, 3, 4, 5}, 6, new short[] {5, 1, 2, 3, 4});
  }

  public void testRotateIndexed() {
    testRotate(new short[] {}, 0, 0, 0, new short[] {});

    testRotate(new short[] {1}, 0, 0, 1, new short[] {1});
    testRotate(new short[] {1}, 1, 0, 1, new short[] {1});
    testRotate(new short[] {1}, 1, 1, 1, new short[] {1});

    // Rotate the central 5 elements, leaving the ends as-is
    testRotate(new short[] {0, 1, 2, 3, 4, 5, 6}, -6, 1, 6, new short[] {0, 2, 3, 4, 5, 1, 6});
    testRotate(new short[] {0, 1, 2, 3, 4, 5, 6}, -1, 1, 6, new short[] {0, 2, 3, 4, 5, 1, 6});
    testRotate(new short[] {0, 1, 2, 3, 4, 5, 6}, 0, 1, 6, new short[] {0, 1, 2, 3, 4, 5, 6});
    testRotate(new short[] {0, 1, 2, 3, 4, 5, 6}, 5, 1, 6, new short[] {0, 1, 2, 3, 4, 5, 6});
    testRotate(new short[] {0, 1, 2, 3, 4, 5, 6}, 14, 1, 6, new short[] {0, 2, 3, 4, 5, 1, 6});

    // Rotate the first three elements
    testRotate(new short[] {0, 1, 2, 3, 4, 5, 6}, -2, 0, 3, new short[] {2, 0, 1, 3, 4, 5, 6});
    testRotate(new short[] {0, 1, 2, 3, 4, 5, 6}, -1, 0, 3, new short[] {1, 2, 0, 3, 4, 5, 6});
    testRotate(new short[] {0, 1, 2, 3, 4, 5, 6}, 0, 0, 3, new short[] {0, 1, 2, 3, 4, 5, 6});
    testRotate(new short[] {0, 1, 2, 3, 4, 5, 6}, 1, 0, 3, new short[] {2, 0, 1, 3, 4, 5, 6});
    testRotate(new short[] {0, 1, 2, 3, 4, 5, 6}, 2, 0, 3, new short[] {1, 2, 0, 3, 4, 5, 6});

    // Rotate the last four elements
    testRotate(new short[] {0, 1, 2, 3, 4, 5, 6}, -6, 3, 7, new short[] {0, 1, 2, 5, 6, 3, 4});
    testRotate(new short[] {0, 1, 2, 3, 4, 5, 6}, -5, 3, 7, new short[] {0, 1, 2, 4, 5, 6, 3});
    testRotate(new short[] {0, 1, 2, 3, 4, 5, 6}, -4, 3, 7, new short[] {0, 1, 2, 3, 4, 5, 6});
    testRotate(new short[] {0, 1, 2, 3, 4, 5, 6}, -3, 3, 7, new short[] {0, 1, 2, 6, 3, 4, 5});
    testRotate(new short[] {0, 1, 2, 3, 4, 5, 6}, -2, 3, 7, new short[] {0, 1, 2, 5, 6, 3, 4});
    testRotate(new short[] {0, 1, 2, 3, 4, 5, 6}, -1, 3, 7, new short[] {0, 1, 2, 4, 5, 6, 3});
    testRotate(new short[] {0, 1, 2, 3, 4, 5, 6}, 0, 3, 7, new short[] {0, 1, 2, 3, 4, 5, 6});
    testRotate(new short[] {0, 1, 2, 3, 4, 5, 6}, 1, 3, 7, new short[] {0, 1, 2, 6, 3, 4, 5});
    testRotate(new short[] {0, 1, 2, 3, 4, 5, 6}, 2, 3, 7, new short[] {0, 1, 2, 5, 6, 3, 4});
    testRotate(new short[] {0, 1, 2, 3, 4, 5, 6}, 3, 3, 7, new short[] {0, 1, 2, 4, 5, 6, 3});
  }

  public void testSortDescending() {
    testSortDescending(new short[] {}, new short[] {});
    testSortDescending(new short[] {1}, new short[] {1});
    testSortDescending(new short[] {1, 2}, new short[] {2, 1});
    testSortDescending(new short[] {1, 3, 1}, new short[] {3, 1, 1});
    testSortDescending(new short[] {-1, 1, -2, 2}, new short[] {2, 1, -1, -2});
  }

  private static void testSortDescending(short[] input, short[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Shorts.sortDescending(input);
    assertThat(input).isEqualTo(expectedOutput);
  }

  private static void testSortDescending(
      short[] input, int fromIndex, int toIndex, short[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Shorts.sortDescending(input, fromIndex, toIndex);
    assertThat(input).isEqualTo(expectedOutput);
  }

  public void testSortDescendingIndexed() {
    testSortDescending(new short[] {}, 0, 0, new short[] {});
    testSortDescending(new short[] {1}, 0, 1, new short[] {1});
    testSortDescending(new short[] {1, 2}, 0, 2, new short[] {2, 1});
    testSortDescending(new short[] {1, 3, 1}, 0, 2, new short[] {3, 1, 1});
    testSortDescending(new short[] {1, 3, 1}, 0, 1, new short[] {1, 3, 1});
    testSortDescending(new short[] {-1, -2, 1, 2}, 1, 3, new short[] {-1, 1, -2, 2});
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testStringConverterSerialization() {
    SerializableTester.reserializeAndAssert(Shorts.stringConverter());
  }

  public void testToArray() {
    // need explicit type parameter to avoid javac warning!?
    List<Short> none = Arrays.<Short>asList();
    assertThat(Shorts.toArray(none)).isEqualTo(EMPTY);

    List<Short> one = Arrays.asList((short) 1);
    assertThat(Shorts.toArray(one)).isEqualTo(ARRAY1);

    short[] array = {(short) 0, (short) 1, (short) 3};

    List<Short> three = Arrays.asList((short) 0, (short) 1, (short) 3);
    assertThat(Shorts.toArray(three)).isEqualTo(array);

    assertThat(Shorts.toArray(Shorts.asList(array))).isEqualTo(array);
  }

  public void testToArray_threadSafe() {
    for (int delta : new int[] {+1, 0, -1}) {
      for (int i = 0; i < VALUES.length; i++) {
        List<Short> list = Shorts.asList(VALUES).subList(0, i);
        Collection<Short> misleadingSize = Helpers.misleadingSizeCollection(delta);
        misleadingSize.addAll(list);
        short[] arr = Shorts.toArray(misleadingSize);
        assertThat(arr).hasLength(i);
        for (int j = 0; j < i; j++) {
          assertThat(arr[j]).isEqualTo(VALUES[j]);
        }
      }
    }
  }

  public void testToArray_withNull() {
    List<@Nullable Short> list = Arrays.asList((short) 0, (short) 1, null);
    try {
      Shorts.toArray(list);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testToArray_withConversion() {
    short[] array = {(short) 0, (short) 1, (short) 2};

    List<Byte> bytes = Arrays.asList((byte) 0, (byte) 1, (byte) 2);
    List<Short> shorts = Arrays.asList((short) 0, (short) 1, (short) 2);
    List<Integer> ints = Arrays.asList(0, 1, 2);
    List<Float> floats = Arrays.asList((float) 0, (float) 1, (float) 2);
    List<Long> longs = Arrays.asList((long) 0, (long) 1, (long) 2);
    List<Double> doubles = Arrays.asList((double) 0, (double) 1, (double) 2);

    assertThat(Shorts.toArray(bytes)).isEqualTo(array);
    assertThat(Shorts.toArray(shorts)).isEqualTo(array);
    assertThat(Shorts.toArray(ints)).isEqualTo(array);
    assertThat(Shorts.toArray(floats)).isEqualTo(array);
    assertThat(Shorts.toArray(longs)).isEqualTo(array);
    assertThat(Shorts.toArray(doubles)).isEqualTo(array);
  }

  @J2ktIncompatible // b/285319375
  public void testAsList_isAView() {
    short[] array = {(short) 0, (short) 1};
    List<Short> list = Shorts.asList(array);
    list.set(0, (short) 2);
    assertThat(array).isEqualTo(new short[] {(short) 2, (short) 1});
    array[1] = (short) 3;
    assertThat(list).containsExactly((short) 2, (short) 3).inOrder();
  }

  public void testAsList_toArray_roundTrip() {
    short[] array = {(short) 0, (short) 1, (short) 2};
    List<Short> list = Shorts.asList(array);
    short[] newArray = Shorts.toArray(list);

    // Make sure it returned a copy
    list.set(0, (short) 4);
    assertThat(newArray).isEqualTo(new short[] {(short) 0, (short) 1, (short) 2});
    newArray[1] = (short) 5;
    assertThat((short) list.get(1)).isEqualTo((short) 1);
  }

  // This test stems from a real bug found by andrewk
  public void testAsList_subList_toArray_roundTrip() {
    short[] array = {(short) 0, (short) 1, (short) 2, (short) 3};
    List<Short> list = Shorts.asList(array);
    assertThat(Shorts.toArray(list.subList(1, 3))).isEqualTo(new short[] {(short) 1, (short) 2});
    assertThat(Shorts.toArray(list.subList(2, 2))).isEqualTo(new short[] {});
  }

  public void testAsListEmpty() {
    assertThat(Shorts.asList(EMPTY)).isSameInstanceAs(Collections.emptyList());
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testNulls() {
    new NullPointerTester().testAllPublicStaticMethods(Shorts.class);
  }

  public void testStringConverter_convert() {
    Converter<String, Short> converter = Shorts.stringConverter();
    assertThat(converter.convert("1")).isEqualTo((Short) (short) 1);
    assertThat(converter.convert("0")).isEqualTo((Short) (short) 0);
    assertThat(converter.convert("-1")).isEqualTo((Short) (short) (-1));
    assertThat(converter.convert("0xff")).isEqualTo((Short) (short) 255);
    assertThat(converter.convert("0xFF")).isEqualTo((Short) (short) 255);
    assertThat(converter.convert("-0xFF")).isEqualTo((Short) (short) (-255));
    assertThat(converter.convert("#0000FF")).isEqualTo((Short) (short) 255);
    assertThat(converter.convert("0666")).isEqualTo((Short) (short) 438);
  }

  public void testStringConverter_convertError() {
    try {
      Shorts.stringConverter().convert("notanumber");
      fail();
    } catch (NumberFormatException expected) {
    }
  }

  public void testStringConverter_nullConversions() {
    assertThat(Shorts.stringConverter().convert(null)).isNull();
    assertThat(Shorts.stringConverter().reverse().convert(null)).isNull();
  }

  public void testStringConverter_reverse() {
    Converter<String, Short> converter = Shorts.stringConverter();
    assertThat(converter.reverse().convert((short) 1)).isEqualTo("1");
    assertThat(converter.reverse().convert((short) 0)).isEqualTo("0");
    assertThat(converter.reverse().convert((short) -1)).isEqualTo("-1");
    assertThat(converter.reverse().convert((short) 0xff)).isEqualTo("255");
    assertThat(converter.reverse().convert((short) 0xFF)).isEqualTo("255");
    assertThat(converter.reverse().convert((short) -0xFF)).isEqualTo("-255");
    assertThat(converter.reverse().convert((short) 0666)).isEqualTo("438");
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testStringConverter_nullPointerTester() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicInstanceMethods(Shorts.stringConverter());
  }
}

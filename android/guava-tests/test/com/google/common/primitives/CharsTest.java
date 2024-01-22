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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Unit test for {@link Chars}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
@SuppressWarnings("cast") // redundant casts are intentional and harmless
public class CharsTest extends TestCase {
  private static final char[] EMPTY = {};
  private static final char[] ARRAY1 = {(char) 1};
  private static final char[] ARRAY234 = {(char) 2, (char) 3, (char) 4};

  private static final char LEAST = Character.MIN_VALUE;
  private static final char GREATEST = Character.MAX_VALUE;

  private static final char[] VALUES = {LEAST, 'a', '\u00e0', '\udcaa', GREATEST};

  public void testHashCode() {
    for (char value : VALUES) {
      assertThat(Chars.hashCode(value)).isEqualTo(((Character) value).hashCode());
    }
  }

  public void testCheckedCast() {
    for (char value : VALUES) {
      assertThat(Chars.checkedCast((long) value)).isEqualTo(value);
    }
    assertCastFails(GREATEST + 1L);
    assertCastFails(LEAST - 1L);
    assertCastFails(Long.MAX_VALUE);
    assertCastFails(Long.MIN_VALUE);
  }

  public void testSaturatedCast() {
    for (char value : VALUES) {
      assertThat(Chars.saturatedCast((long) value)).isEqualTo(value);
    }
    assertThat(Chars.saturatedCast(GREATEST + 1L)).isEqualTo(GREATEST);
    assertThat(Chars.saturatedCast(LEAST - 1L)).isEqualTo(LEAST);
    assertThat(Chars.saturatedCast(Long.MAX_VALUE)).isEqualTo(GREATEST);
    assertThat(Chars.saturatedCast(Long.MIN_VALUE)).isEqualTo(LEAST);
  }

  private void assertCastFails(long value) {
    try {
      Chars.checkedCast(value);
      fail("Cast to char should have failed: " + value);
    } catch (IllegalArgumentException ex) {
      assertWithMessage(value + " not found in exception text: " + ex.getMessage())
          .that(ex.getMessage().contains(String.valueOf(value)))
          .isTrue();
    }
  }

  public void testCompare() {
    for (char x : VALUES) {
      for (char y : VALUES) {
        assertWithMessage(x + ", " + y)
            .that(Math.signum(Chars.compare(x, y)))
            .isEqualTo(Math.signum(Character.valueOf(x).compareTo(y)));
      }
    }
  }

  public void testContains() {
    assertThat(Chars.contains(EMPTY, (char) 1)).isFalse();
    assertThat(Chars.contains(ARRAY1, (char) 2)).isFalse();
    assertThat(Chars.contains(ARRAY234, (char) 1)).isFalse();
    assertThat(Chars.contains(new char[] {(char) -1}, (char) -1)).isTrue();
    assertThat(Chars.contains(ARRAY234, (char) 2)).isTrue();
    assertThat(Chars.contains(ARRAY234, (char) 3)).isTrue();
    assertThat(Chars.contains(ARRAY234, (char) 4)).isTrue();
  }

  public void testIndexOf() {
    assertThat(Chars.indexOf(EMPTY, (char) 1)).isEqualTo(-1);
    assertThat(Chars.indexOf(ARRAY1, (char) 2)).isEqualTo(-1);
    assertThat(Chars.indexOf(ARRAY234, (char) 1)).isEqualTo(-1);
    assertThat(Chars.indexOf(new char[] {(char) -1}, (char) -1)).isEqualTo(0);
    assertThat(Chars.indexOf(ARRAY234, (char) 2)).isEqualTo(0);
    assertThat(Chars.indexOf(ARRAY234, (char) 3)).isEqualTo(1);
    assertThat(Chars.indexOf(ARRAY234, (char) 4)).isEqualTo(2);
    assertThat(Chars.indexOf(new char[] {(char) 2, (char) 3, (char) 2, (char) 3}, (char) 3))
        .isEqualTo(1);
  }

  public void testIndexOf_arrayTarget() {
    assertThat(Chars.indexOf(EMPTY, EMPTY)).isEqualTo(0);
    assertThat(Chars.indexOf(ARRAY234, EMPTY)).isEqualTo(0);
    assertThat(Chars.indexOf(EMPTY, ARRAY234)).isEqualTo(-1);
    assertThat(Chars.indexOf(ARRAY234, ARRAY1)).isEqualTo(-1);
    assertThat(Chars.indexOf(ARRAY1, ARRAY234)).isEqualTo(-1);
    assertThat(Chars.indexOf(ARRAY1, ARRAY1)).isEqualTo(0);
    assertThat(Chars.indexOf(ARRAY234, ARRAY234)).isEqualTo(0);
    assertThat(Chars.indexOf(ARRAY234, new char[] {(char) 2, (char) 3})).isEqualTo(0);
    assertThat(Chars.indexOf(ARRAY234, new char[] {(char) 3, (char) 4})).isEqualTo(1);
    assertThat(Chars.indexOf(ARRAY234, new char[] {(char) 3})).isEqualTo(1);
    assertThat(Chars.indexOf(ARRAY234, new char[] {(char) 4})).isEqualTo(2);
    assertThat(
            Chars.indexOf(
                new char[] {(char) 2, (char) 3, (char) 3, (char) 3, (char) 3},
                new char[] {(char) 3}))
        .isEqualTo(1);
    assertThat(
            Chars.indexOf(
                new char[] {(char) 2, (char) 3, (char) 2, (char) 3, (char) 4, (char) 2, (char) 3},
                new char[] {(char) 2, (char) 3, (char) 4}))
        .isEqualTo(2);
    assertThat(
            Chars.indexOf(
                new char[] {(char) 2, (char) 2, (char) 3, (char) 4, (char) 2, (char) 3, (char) 4},
                new char[] {(char) 2, (char) 3, (char) 4}))
        .isEqualTo(1);
    assertThat(
            Chars.indexOf(
                new char[] {(char) 4, (char) 3, (char) 2},
                new char[] {(char) 2, (char) 3, (char) 4}))
        .isEqualTo(-1);
  }

  public void testLastIndexOf() {
    assertThat(Chars.lastIndexOf(EMPTY, (char) 1)).isEqualTo(-1);
    assertThat(Chars.lastIndexOf(ARRAY1, (char) 2)).isEqualTo(-1);
    assertThat(Chars.lastIndexOf(ARRAY234, (char) 1)).isEqualTo(-1);
    assertThat(Chars.lastIndexOf(new char[] {(char) -1}, (char) -1)).isEqualTo(0);
    assertThat(Chars.lastIndexOf(ARRAY234, (char) 2)).isEqualTo(0);
    assertThat(Chars.lastIndexOf(ARRAY234, (char) 3)).isEqualTo(1);
    assertThat(Chars.lastIndexOf(ARRAY234, (char) 4)).isEqualTo(2);
    assertThat(Chars.lastIndexOf(new char[] {(char) 2, (char) 3, (char) 2, (char) 3}, (char) 3))
        .isEqualTo(3);
  }

  public void testMax_noArgs() {
    try {
      Chars.max();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMax() {
    assertThat(Chars.max(LEAST)).isEqualTo(LEAST);
    assertThat(Chars.max(GREATEST)).isEqualTo(GREATEST);
    assertThat(Chars.max((char) 8, (char) 6, (char) 7, (char) 5, (char) 3, (char) 0, (char) 9))
        .isEqualTo((char) 9);
  }

  public void testMin_noArgs() {
    try {
      Chars.min();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMin() {
    assertThat(Chars.min(LEAST)).isEqualTo(LEAST);
    assertThat(Chars.min(GREATEST)).isEqualTo(GREATEST);
    assertThat(Chars.min((char) 8, (char) 6, (char) 7, (char) 5, (char) 3, (char) 0, (char) 9))
        .isEqualTo((char) 0);
  }

  public void testConstrainToRange() {
    assertThat(Chars.constrainToRange((char) 1, (char) 0, (char) 5)).isEqualTo((char) 1);
    assertThat(Chars.constrainToRange((char) 1, (char) 1, (char) 5)).isEqualTo((char) 1);
    assertThat(Chars.constrainToRange((char) 1, (char) 3, (char) 5)).isEqualTo((char) 3);
    assertThat(Chars.constrainToRange((char) 255, (char) 250, (char) 254)).isEqualTo((char) 254);
    assertThat(Chars.constrainToRange((char) 5, (char) 2, (char) 2)).isEqualTo((char) 2);
    try {
      Chars.constrainToRange((char) 1, (char) 3, (char) 2);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testConcat() {
    assertThat(Chars.concat()).isEqualTo(EMPTY);
    assertThat(Chars.concat(EMPTY)).isEqualTo(EMPTY);
    assertThat(Chars.concat(EMPTY, EMPTY, EMPTY)).isEqualTo(EMPTY);
    assertThat(Chars.concat(ARRAY1)).isEqualTo(ARRAY1);
    assertThat(Chars.concat(ARRAY1)).isNotSameInstanceAs(ARRAY1);
    assertThat(Chars.concat(EMPTY, ARRAY1, EMPTY)).isEqualTo(ARRAY1);
    assertThat(Chars.concat(ARRAY1, ARRAY1, ARRAY1))
        .isEqualTo(new char[] {(char) 1, (char) 1, (char) 1});
    assertThat(Chars.concat(ARRAY1, ARRAY234))
        .isEqualTo(new char[] {(char) 1, (char) 2, (char) 3, (char) 4});
  }

  @J2ktIncompatible
  @GwtIncompatible // Chars.fromByteArray
  public void testFromByteArray() {
    assertThat(Chars.fromByteArray(new byte[] {0x23, 0x45, (byte) 0xDC})).isEqualTo('\u2345');
    assertThat(Chars.fromByteArray(new byte[] {(byte) 0xFE, (byte) 0xDC})).isEqualTo('\uFEDC');
  }

  @J2ktIncompatible
  @GwtIncompatible // Chars.fromByteArray
  public void testFromByteArrayFails() {
    try {
      Chars.fromByteArray(new byte[Chars.BYTES - 1]);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // Chars.fromBytes
  public void testFromBytes() {
    assertThat(Chars.fromBytes((byte) 0x23, (byte) 0x45)).isEqualTo('\u2345');
    assertThat(Chars.fromBytes((byte) 0xFE, (byte) 0xDC)).isEqualTo('\uFEDC');
  }

  @J2ktIncompatible
  @GwtIncompatible // Chars.fromByteArray, Chars.toByteArray
  public void testByteArrayRoundTrips() {
    char c = 0;
    for (int hi = 0; hi < 256; hi++) {
      for (int lo = 0; lo < 256; lo++) {
        char result = Chars.fromByteArray(new byte[] {(byte) hi, (byte) lo});
        assertWithMessage(
                String.format(
                    Locale.ROOT,
                    "hi=%s, lo=%s, expected=%s, result=%s",
                    hi,
                    lo,
                    (int) c,
                    (int) result))
            .that(result)
            .isEqualTo(c);

        byte[] bytes = Chars.toByteArray(c);
        assertThat(bytes[0]).isEqualTo((byte) hi);
        assertThat(bytes[1]).isEqualTo((byte) lo);

        c++;
      }
    }
    assertThat(c).isEqualTo((char) 0); // sanity check
  }

  @J2ktIncompatible
  @GwtIncompatible // Chars.fromByteArray, Chars.toByteArray
  public void testByteArrayRoundTripsFails() {
    try {
      Chars.fromByteArray(new byte[] {0x11});
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testEnsureCapacity() {
    assertThat(Chars.ensureCapacity(EMPTY, 0, 1)).isSameInstanceAs(EMPTY);
    assertThat(Chars.ensureCapacity(ARRAY1, 0, 1)).isSameInstanceAs(ARRAY1);
    assertThat(Chars.ensureCapacity(ARRAY1, 1, 1)).isSameInstanceAs(ARRAY1);
    assertThat(Chars.ensureCapacity(ARRAY1, 2, 1))
        .isEqualTo(new char[] {(char) 1, (char) 0, (char) 0});
  }

  public void testEnsureCapacity_fail() {
    try {
      Chars.ensureCapacity(ARRAY1, -1, 1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      // notice that this should even fail when no growth was needed
      Chars.ensureCapacity(ARRAY1, 1, -1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testJoin() {
    assertThat(Chars.join(",", EMPTY)).isEmpty();
    assertThat(Chars.join(",", '1')).isEqualTo("1");
    assertThat(Chars.join(",", '1', '2')).isEqualTo("1,2");
    assertThat(Chars.join("", '1', '2', '3')).isEqualTo("123");
  }

  public void testLexicographicalComparator() {
    List<char[]> ordered =
        Arrays.asList(
            new char[] {},
            new char[] {LEAST},
            new char[] {LEAST, LEAST},
            new char[] {LEAST, (char) 1},
            new char[] {(char) 1},
            new char[] {(char) 1, LEAST},
            new char[] {GREATEST, GREATEST - (char) 1},
            new char[] {GREATEST, GREATEST},
            new char[] {GREATEST, GREATEST, GREATEST});

    Comparator<char[]> comparator = Chars.lexicographicalComparator();
    Helpers.testComparator(comparator, ordered);
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testLexicographicalComparatorSerializable() {
    Comparator<char[]> comparator = Chars.lexicographicalComparator();
    assertThat(SerializableTester.reserialize(comparator)).isSameInstanceAs(comparator);
  }

  public void testReverse() {
    testReverse(new char[] {}, new char[] {});
    testReverse(new char[] {'1'}, new char[] {'1'});
    testReverse(new char[] {'1', '2'}, new char[] {'2', '1'});
    testReverse(new char[] {'3', '1', '1'}, new char[] {'1', '1', '3'});
    testReverse(new char[] {'A', '1', 'B', '2'}, new char[] {'2', 'B', '1', 'A'});
  }

  private static void testReverse(char[] input, char[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Chars.reverse(input);
    assertThat(input).isEqualTo(expectedOutput);
  }

  private static void testReverse(char[] input, int fromIndex, int toIndex, char[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Chars.reverse(input, fromIndex, toIndex);
    assertThat(input).isEqualTo(expectedOutput);
  }

  public void testReverseIndexed() {
    testReverse(new char[] {}, 0, 0, new char[] {});
    testReverse(new char[] {'1'}, 0, 1, new char[] {'1'});
    testReverse(new char[] {'1', '2'}, 0, 2, new char[] {'2', '1'});
    testReverse(new char[] {'3', '1', '1'}, 0, 2, new char[] {'1', '3', '1'});
    testReverse(new char[] {'3', '1', '1'}, 0, 1, new char[] {'3', '1', '1'});
    testReverse(new char[] {'A', '1', 'B', '2'}, 1, 3, new char[] {'A', 'B', '1', '2'});
  }

  private static void testRotate(char[] input, int distance, char[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Chars.rotate(input, distance);
    assertThat(input).isEqualTo(expectedOutput);
  }

  private static void testRotate(
      char[] input, int distance, int fromIndex, int toIndex, char[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Chars.rotate(input, distance, fromIndex, toIndex);
    assertThat(input).isEqualTo(expectedOutput);
  }

  public void testRotate() {
    testRotate(new char[] {}, -1, new char[] {});
    testRotate(new char[] {}, 0, new char[] {});
    testRotate(new char[] {}, 1, new char[] {});

    testRotate(new char[] {'1'}, -2, new char[] {'1'});
    testRotate(new char[] {'1'}, -1, new char[] {'1'});
    testRotate(new char[] {'1'}, 0, new char[] {'1'});
    testRotate(new char[] {'1'}, 1, new char[] {'1'});
    testRotate(new char[] {'1'}, 2, new char[] {'1'});

    testRotate(new char[] {'1', '2'}, -3, new char[] {'2', '1'});
    testRotate(new char[] {'1', '2'}, -1, new char[] {'2', '1'});
    testRotate(new char[] {'1', '2'}, -2, new char[] {'1', '2'});
    testRotate(new char[] {'1', '2'}, 0, new char[] {'1', '2'});
    testRotate(new char[] {'1', '2'}, 1, new char[] {'2', '1'});
    testRotate(new char[] {'1', '2'}, 2, new char[] {'1', '2'});
    testRotate(new char[] {'1', '2'}, 3, new char[] {'2', '1'});

    testRotate(new char[] {'1', '2', '3'}, -5, new char[] {'3', '1', '2'});
    testRotate(new char[] {'1', '2', '3'}, -4, new char[] {'2', '3', '1'});
    testRotate(new char[] {'1', '2', '3'}, -3, new char[] {'1', '2', '3'});
    testRotate(new char[] {'1', '2', '3'}, -2, new char[] {'3', '1', '2'});
    testRotate(new char[] {'1', '2', '3'}, -1, new char[] {'2', '3', '1'});
    testRotate(new char[] {'1', '2', '3'}, 0, new char[] {'1', '2', '3'});
    testRotate(new char[] {'1', '2', '3'}, 1, new char[] {'3', '1', '2'});
    testRotate(new char[] {'1', '2', '3'}, 2, new char[] {'2', '3', '1'});
    testRotate(new char[] {'1', '2', '3'}, 3, new char[] {'1', '2', '3'});
    testRotate(new char[] {'1', '2', '3'}, 4, new char[] {'3', '1', '2'});
    testRotate(new char[] {'1', '2', '3'}, 5, new char[] {'2', '3', '1'});

    testRotate(new char[] {'1', '2', '3', '4'}, -9, new char[] {'2', '3', '4', '1'});
    testRotate(new char[] {'1', '2', '3', '4'}, -5, new char[] {'2', '3', '4', '1'});
    testRotate(new char[] {'1', '2', '3', '4'}, -1, new char[] {'2', '3', '4', '1'});
    testRotate(new char[] {'1', '2', '3', '4'}, 0, new char[] {'1', '2', '3', '4'});
    testRotate(new char[] {'1', '2', '3', '4'}, 1, new char[] {'4', '1', '2', '3'});
    testRotate(new char[] {'1', '2', '3', '4'}, 5, new char[] {'4', '1', '2', '3'});
    testRotate(new char[] {'1', '2', '3', '4'}, 9, new char[] {'4', '1', '2', '3'});

    testRotate(new char[] {'1', '2', '3', '4', '5'}, -6, new char[] {'2', '3', '4', '5', '1'});
    testRotate(new char[] {'1', '2', '3', '4', '5'}, -4, new char[] {'5', '1', '2', '3', '4'});
    testRotate(new char[] {'1', '2', '3', '4', '5'}, -3, new char[] {'4', '5', '1', '2', '3'});
    testRotate(new char[] {'1', '2', '3', '4', '5'}, -1, new char[] {'2', '3', '4', '5', '1'});
    testRotate(new char[] {'1', '2', '3', '4', '5'}, 0, new char[] {'1', '2', '3', '4', '5'});
    testRotate(new char[] {'1', '2', '3', '4', '5'}, 1, new char[] {'5', '1', '2', '3', '4'});
    testRotate(new char[] {'1', '2', '3', '4', '5'}, 3, new char[] {'3', '4', '5', '1', '2'});
    testRotate(new char[] {'1', '2', '3', '4', '5'}, 4, new char[] {'2', '3', '4', '5', '1'});
    testRotate(new char[] {'1', '2', '3', '4', '5'}, 6, new char[] {'5', '1', '2', '3', '4'});
  }

  public void testRotateIndexed() {
    testRotate(new char[] {}, 0, 0, 0, new char[] {});

    testRotate(new char[] {'1'}, 0, 0, 1, new char[] {'1'});
    testRotate(new char[] {'1'}, 1, 0, 1, new char[] {'1'});
    testRotate(new char[] {'1'}, 1, 1, 1, new char[] {'1'});

    // Rotate the central 5 elements, leaving the ends as-is
    testRotate(
        new char[] {'0', '1', '2', '3', '4', '5', '6'},
        -6,
        1,
        6,
        new char[] {'0', '2', '3', '4', '5', '1', '6'});
    testRotate(
        new char[] {'0', '1', '2', '3', '4', '5', '6'},
        -1,
        1,
        6,
        new char[] {'0', '2', '3', '4', '5', '1', '6'});
    testRotate(
        new char[] {'0', '1', '2', '3', '4', '5', '6'},
        0,
        1,
        6,
        new char[] {'0', '1', '2', '3', '4', '5', '6'});
    testRotate(
        new char[] {'0', '1', '2', '3', '4', '5', '6'},
        5,
        1,
        6,
        new char[] {'0', '1', '2', '3', '4', '5', '6'});
    testRotate(
        new char[] {'0', '1', '2', '3', '4', '5', '6'},
        14,
        1,
        6,
        new char[] {'0', '2', '3', '4', '5', '1', '6'});

    // Rotate the first three elements
    testRotate(
        new char[] {'0', '1', '2', '3', '4', '5', '6'},
        -2,
        0,
        3,
        new char[] {'2', '0', '1', '3', '4', '5', '6'});
    testRotate(
        new char[] {'0', '1', '2', '3', '4', '5', '6'},
        -1,
        0,
        3,
        new char[] {'1', '2', '0', '3', '4', '5', '6'});
    testRotate(
        new char[] {'0', '1', '2', '3', '4', '5', '6'},
        0,
        0,
        3,
        new char[] {'0', '1', '2', '3', '4', '5', '6'});
    testRotate(
        new char[] {'0', '1', '2', '3', '4', '5', '6'},
        1,
        0,
        3,
        new char[] {'2', '0', '1', '3', '4', '5', '6'});
    testRotate(
        new char[] {'0', '1', '2', '3', '4', '5', '6'},
        2,
        0,
        3,
        new char[] {'1', '2', '0', '3', '4', '5', '6'});

    // Rotate the last four elements
    testRotate(
        new char[] {'0', '1', '2', '3', '4', '5', '6'},
        -6,
        3,
        7,
        new char[] {'0', '1', '2', '5', '6', '3', '4'});
    testRotate(
        new char[] {'0', '1', '2', '3', '4', '5', '6'},
        -5,
        3,
        7,
        new char[] {'0', '1', '2', '4', '5', '6', '3'});
    testRotate(
        new char[] {'0', '1', '2', '3', '4', '5', '6'},
        -4,
        3,
        7,
        new char[] {'0', '1', '2', '3', '4', '5', '6'});
    testRotate(
        new char[] {'0', '1', '2', '3', '4', '5', '6'},
        -3,
        3,
        7,
        new char[] {'0', '1', '2', '6', '3', '4', '5'});
    testRotate(
        new char[] {'0', '1', '2', '3', '4', '5', '6'},
        -2,
        3,
        7,
        new char[] {'0', '1', '2', '5', '6', '3', '4'});
    testRotate(
        new char[] {'0', '1', '2', '3', '4', '5', '6'},
        -1,
        3,
        7,
        new char[] {'0', '1', '2', '4', '5', '6', '3'});
    testRotate(
        new char[] {'0', '1', '2', '3', '4', '5', '6'},
        0,
        3,
        7,
        new char[] {'0', '1', '2', '3', '4', '5', '6'});
    testRotate(
        new char[] {'0', '1', '2', '3', '4', '5', '6'},
        1,
        3,
        7,
        new char[] {'0', '1', '2', '6', '3', '4', '5'});
    testRotate(
        new char[] {'0', '1', '2', '3', '4', '5', '6'},
        2,
        3,
        7,
        new char[] {'0', '1', '2', '5', '6', '3', '4'});
    testRotate(
        new char[] {'0', '1', '2', '3', '4', '5', '6'},
        3,
        3,
        7,
        new char[] {'0', '1', '2', '4', '5', '6', '3'});
  }

  public void testSortDescending() {
    testSortDescending(new char[] {}, new char[] {});
    testSortDescending(new char[] {'1'}, new char[] {'1'});
    testSortDescending(new char[] {'1', '2'}, new char[] {'2', '1'});
    testSortDescending(new char[] {'1', '3', '1'}, new char[] {'3', '1', '1'});
    testSortDescending(new char[] {'A', '1', 'B', '2'}, new char[] {'B', 'A', '2', '1'});
  }

  private static void testSortDescending(char[] input, char[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Chars.sortDescending(input);
    assertThat(input).isEqualTo(expectedOutput);
  }

  private static void testSortDescending(
      char[] input, int fromIndex, int toIndex, char[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Chars.sortDescending(input, fromIndex, toIndex);
    assertThat(input).isEqualTo(expectedOutput);
  }

  public void testSortDescendingIndexed() {
    testSortDescending(new char[] {}, 0, 0, new char[] {});
    testSortDescending(new char[] {'1'}, 0, 1, new char[] {'1'});
    testSortDescending(new char[] {'1', '2'}, 0, 2, new char[] {'2', '1'});
    testSortDescending(new char[] {'1', '3', '1'}, 0, 2, new char[] {'3', '1', '1'});
    testSortDescending(new char[] {'1', '3', '1'}, 0, 1, new char[] {'1', '3', '1'});
    testSortDescending(new char[] {'A', '1', 'B', '2'}, 1, 3, new char[] {'A', 'B', '1', '2'});
  }

  public void testToArray() {
    // need explicit type parameter to avoid javac warning!?
    List<Character> none = Arrays.<Character>asList();
    assertThat(Chars.toArray(none)).isEqualTo(EMPTY);

    List<Character> one = Arrays.asList((char) 1);
    assertThat(Chars.toArray(one)).isEqualTo(ARRAY1);

    char[] array = {(char) 0, (char) 1, 'A'};

    List<Character> three = Arrays.asList((char) 0, (char) 1, 'A');
    assertThat(Chars.toArray(three)).isEqualTo(array);

    assertThat(Chars.toArray(Chars.asList(array))).isEqualTo(array);
  }

  public void testToArray_threadSafe() {
    for (int delta : new int[] {+1, 0, -1}) {
      for (int i = 0; i < VALUES.length; i++) {
        List<Character> list = Chars.asList(VALUES).subList(0, i);
        Collection<Character> misleadingSize = Helpers.misleadingSizeCollection(delta);
        misleadingSize.addAll(list);
        char[] arr = Chars.toArray(misleadingSize);
        assertThat(arr).hasLength(i);
        for (int j = 0; j < i; j++) {
          assertThat(arr[j]).isEqualTo(VALUES[j]);
        }
      }
    }
  }

  public void testToArray_withNull() {
    List<@Nullable Character> list = Arrays.asList((char) 0, (char) 1, null);
    try {
      Chars.toArray(list);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  @J2ktIncompatible // b/285319375
  public void testAsList_isAView() {
    char[] array = {(char) 0, (char) 1};
    List<Character> list = Chars.asList(array);
    list.set(0, (char) 2);
    assertThat(array).isEqualTo(new char[] {(char) 2, (char) 1});
    array[1] = (char) 3;
    assertThat(list).containsExactly((char) 2, (char) 3).inOrder();
  }

  public void testAsList_toArray_roundTrip() {
    char[] array = {(char) 0, (char) 1, (char) 2};
    List<Character> list = Chars.asList(array);
    char[] newArray = Chars.toArray(list);

    // Make sure it returned a copy
    list.set(0, (char) 4);
    assertThat(newArray).isEqualTo(new char[] {(char) 0, (char) 1, (char) 2});
    newArray[1] = (char) 5;
    assertThat((char) list.get(1)).isEqualTo((char) 1);
  }

  // This test stems from a real bug found by andrewk
  public void testAsList_subList_toArray_roundTrip() {
    char[] array = {(char) 0, (char) 1, (char) 2, (char) 3};
    List<Character> list = Chars.asList(array);
    assertThat(Chars.toArray(list.subList(1, 3))).isEqualTo(new char[] {(char) 1, (char) 2});
    assertThat(Chars.toArray(list.subList(2, 2))).isEqualTo(new char[] {});
  }

  public void testAsListEmpty() {
    assertThat(Chars.asList(EMPTY)).isSameInstanceAs(Collections.emptyList());
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testNulls() {
    new NullPointerTester().testAllPublicStaticMethods(Chars.class);
  }
}

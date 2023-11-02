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
import static org.junit.Assert.assertThrows;

import com.google.common.collect.testing.Helpers;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import junit.framework.TestCase;

/**
 * Unit test for {@link UnsignedBytes}.
 *
 * @author Kevin Bourrillion
 * @author Louis Wasserman
 */
public class UnsignedBytesTest extends TestCase {
  private static final byte LEAST = 0;
  private static final byte GREATEST = (byte) 255;

  // Only in this class, VALUES must be strictly ascending
  private static final byte[] VALUES = {LEAST, 127, (byte) 128, (byte) 129, GREATEST};

  public void testToInt() {
    assertThat(UnsignedBytes.toInt((byte) 0)).isEqualTo(0);
    assertThat(UnsignedBytes.toInt((byte) 1)).isEqualTo(1);
    assertThat(UnsignedBytes.toInt((byte) 127)).isEqualTo(127);
    assertThat(UnsignedBytes.toInt((byte) -128)).isEqualTo(128);
    assertThat(UnsignedBytes.toInt((byte) -127)).isEqualTo(129);
    assertThat(UnsignedBytes.toInt((byte) -1)).isEqualTo(255);
  }

  public void testCheckedCast() {
    for (byte value : VALUES) {
      assertThat(UnsignedBytes.checkedCast(UnsignedBytes.toInt(value))).isEqualTo(value);
    }
    assertCastFails(256L);
    assertCastFails(-1L);
    assertCastFails(Long.MAX_VALUE);
    assertCastFails(Long.MIN_VALUE);
  }

  public void testSaturatedCast() {
    for (byte value : VALUES) {
      assertThat(UnsignedBytes.saturatedCast(UnsignedBytes.toInt(value))).isEqualTo(value);
    }
    assertThat(UnsignedBytes.saturatedCast(256L)).isEqualTo(GREATEST);
    assertThat(UnsignedBytes.saturatedCast(-1L)).isEqualTo(LEAST);
    assertThat(UnsignedBytes.saturatedCast(Long.MAX_VALUE)).isEqualTo(GREATEST);
    assertThat(UnsignedBytes.saturatedCast(Long.MIN_VALUE)).isEqualTo(LEAST);
  }

  private static void assertCastFails(long value) {
    try {
      UnsignedBytes.checkedCast(value);
      fail("Cast to byte should have failed: " + value);
    } catch (IllegalArgumentException ex) {
      assertWithMessage(value + " not found in exception text: " + ex.getMessage())
          .that(ex.getMessage().contains(String.valueOf(value)))
          .isTrue();
    }
  }

  public void testCompare() {
    // This is the only ordering for primitives that does not have a
    // corresponding Comparable wrapper in java.lang.
    for (int i = 0; i < VALUES.length; i++) {
      for (int j = 0; j < VALUES.length; j++) {
        byte x = VALUES[i];
        byte y = VALUES[j];
        // note: spec requires only that the sign is the same
        assertWithMessage(x + ", " + y)
            .that(Math.signum(Ints.compare(i, j)))
            .isEqualTo(Math.signum(UnsignedBytes.compare(x, y)));
      }
    }
  }

  public void testMax_noArgs() {
    assertThrows(IllegalArgumentException.class, () -> UnsignedBytes.max());
  }

  public void testMax() {
    assertThat(UnsignedBytes.max(LEAST)).isEqualTo(LEAST);
    assertThat(UnsignedBytes.max(GREATEST)).isEqualTo(GREATEST);
    assertThat(UnsignedBytes.max((byte) 0, (byte) -128, (byte) -1, (byte) 127, (byte) 1))
        .isEqualTo((byte) 255);
  }

  public void testMin_noArgs() {
    assertThrows(IllegalArgumentException.class, () -> UnsignedBytes.min());
  }

  public void testMin() {
    assertThat(UnsignedBytes.min(LEAST)).isEqualTo(LEAST);
    assertThat(UnsignedBytes.min(GREATEST)).isEqualTo(GREATEST);
    assertThat(UnsignedBytes.min((byte) 0, (byte) -128, (byte) -1, (byte) 127, (byte) 1))
        .isEqualTo((byte) 0);
    assertThat(UnsignedBytes.min((byte) -1, (byte) 127, (byte) 1, (byte) -128, (byte) 0))
        .isEqualTo((byte) 0);
  }

  private static void assertParseFails(String value) {
    try {
      UnsignedBytes.parseUnsignedByte(value);
      fail();
    } catch (NumberFormatException expected) {
    }
  }

  private static void assertParseFails(String value, int radix) {
    try {
      UnsignedBytes.parseUnsignedByte(value, radix);
      fail();
    } catch (NumberFormatException expected) {
    }
  }

  public void testParseUnsignedByte() {
    // We can easily afford to test this exhaustively.
    for (int i = 0; i <= 0xff; i++) {
      assertThat(UnsignedBytes.parseUnsignedByte(Integer.toString(i))).isEqualTo((byte) i);
    }
    assertParseFails("1000");
    assertParseFails("-1");
    assertParseFails("-128");
    assertParseFails("256");
  }

  public void testMaxValue() {
    assertThat(UnsignedBytes.compare(UnsignedBytes.MAX_VALUE, (byte) (UnsignedBytes.MAX_VALUE + 1)))
        .isGreaterThan(0);
  }

  public void testParseUnsignedByteWithRadix() throws NumberFormatException {
    // We can easily afford to test this exhaustively.
    for (int radix = Character.MIN_RADIX; radix <= Character.MAX_RADIX; radix++) {
      for (int i = 0; i <= 0xff; i++) {
        assertThat(UnsignedBytes.parseUnsignedByte(Integer.toString(i, radix), radix))
            .isEqualTo((byte) i);
      }
      assertParseFails(Integer.toString(1000, radix), radix);
      assertParseFails(Integer.toString(-1, radix), radix);
      assertParseFails(Integer.toString(-128, radix), radix);
      assertParseFails(Integer.toString(256, radix), radix);
    }
  }

  public void testParseUnsignedByteThrowsExceptionForInvalidRadix() {
    // Valid radix values are Character.MIN_RADIX to Character.MAX_RADIX,
    // inclusive.
    assertThrows(
        NumberFormatException.class,
        () -> UnsignedBytes.parseUnsignedByte("0", Character.MIN_RADIX - 1));

    assertThrows(
        NumberFormatException.class,
        () -> UnsignedBytes.parseUnsignedByte("0", Character.MAX_RADIX + 1));

    // The radix is used as an array index, so try a negative value.
    assertThrows(NumberFormatException.class, () -> UnsignedBytes.parseUnsignedByte("0", -1));
  }

  public void testToString() {
    // We can easily afford to test this exhaustively.
    for (int i = 0; i <= 0xff; i++) {
      assertThat(UnsignedBytes.toString((byte) i)).isEqualTo(Integer.toString(i));
    }
  }

  public void testToStringWithRadix() {
    // We can easily afford to test this exhaustively.
    for (int radix = Character.MIN_RADIX; radix <= Character.MAX_RADIX; radix++) {
      for (int i = 0; i <= 0xff; i++) {
        assertThat(UnsignedBytes.toString((byte) i, radix)).isEqualTo(Integer.toString(i, radix));
      }
    }
  }

  public void testJoin() {
    assertThat(UnsignedBytes.join(",", new byte[] {})).isEmpty();
    assertThat(UnsignedBytes.join(",", new byte[] {(byte) 1})).isEqualTo("1");
    assertThat(UnsignedBytes.join(",", (byte) 1, (byte) 2)).isEqualTo("1,2");
    assertThat(UnsignedBytes.join("", (byte) 1, (byte) 2, (byte) 3)).isEqualTo("123");
    assertThat(UnsignedBytes.join(",", (byte) 128, (byte) -1)).isEqualTo("128,255");
  }

  private static String unsafeComparatorClassName() {
    return UnsignedBytes.LexicographicalComparatorHolder.class.getName() + "$UnsafeComparator";
  }

  private static boolean unsafeComparatorAvailable() {
    // See Java Puzzler #44
    // Use reflection instead of catching NoClassDefFoundError
    try {
      Class.forName(unsafeComparatorClassName());
      return true;
    } catch (Error | ClassNotFoundException tolerable) {
      /*
       * We're probably running on Android.
       *
       * A note on exception types:
       *
       * Android API level 10 throws ExceptionInInitializerError the first time and
       * ClassNotFoundException thereafter.
       *
       * Android API level 26 and JVM8 both let our Error propagate directly the first time and
       * throw NoClassDefFoundError thereafter. This is the proper behavior according to the spec.
       * See https://docs.oracle.com/javase/specs/jls/se8/html/jls-12.html#jls-12.4.2 (steps #11 and
       * #5).
       *
       * Note that that "first time" might happen in a test other than
       * testLexicographicalComparatorChoice!
       */
      return false;
    }
  }

  public void testLexicographicalComparatorChoice() throws Exception {
    Comparator<byte[]> defaultComparator = UnsignedBytes.lexicographicalComparator();
    assertThat(defaultComparator).isNotNull();
    assertThat(UnsignedBytes.lexicographicalComparator()).isSameInstanceAs(defaultComparator);
    if (unsafeComparatorAvailable()) {
      assertThat(Class.forName(unsafeComparatorClassName()))
          .isSameInstanceAs(defaultComparator.getClass());
    } else {
      assertThat(UnsignedBytes.lexicographicalComparatorJavaImpl())
          .isSameInstanceAs(defaultComparator);
    }
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

    // The Unsafe implementation if it's available. Otherwise, the Java implementation.
    Comparator<byte[]> comparator = UnsignedBytes.lexicographicalComparator();
    Helpers.testComparator(comparator, ordered);
    assertThat(SerializableTester.reserialize(comparator)).isSameInstanceAs(comparator);

    // The Java implementation.
    Comparator<byte[]> javaImpl = UnsignedBytes.lexicographicalComparatorJavaImpl();
    Helpers.testComparator(javaImpl, ordered);
    assertThat(SerializableTester.reserialize(javaImpl)).isSameInstanceAs(javaImpl);
  }

  @SuppressWarnings("unchecked")
  public void testLexicographicalComparatorLongInputs() {
    Random rnd = new Random();
    for (Comparator<byte[]> comparator :
        Arrays.asList(
            UnsignedBytes.lexicographicalComparator(),
            UnsignedBytes.lexicographicalComparatorJavaImpl())) {
      for (int trials = 10; trials-- > 0; ) {
        byte[] left = new byte[1 + rnd.nextInt(32)];
        rnd.nextBytes(left);
        byte[] right = left.clone();
        assertThat(comparator.compare(left, right)).isEqualTo(0);
        int i = rnd.nextInt(left.length);
        left[i] ^= (byte) (1 + rnd.nextInt(255));
        assertThat(comparator.compare(left, right)).isNotEqualTo(0);
        assertThat(UnsignedBytes.compare(left[i], right[i]) > 0)
            .isEqualTo(comparator.compare(left, right) > 0);
      }
    }
  }

  public void testSort() {
    testSort(new byte[] {}, new byte[] {});
    testSort(new byte[] {2}, new byte[] {2});
    testSort(new byte[] {2, 1, 0}, new byte[] {0, 1, 2});
    testSort(new byte[] {2, GREATEST, 1, LEAST}, new byte[] {LEAST, 1, 2, GREATEST});
  }

  static void testSort(byte[] input, byte[] expected) {
    input = Arrays.copyOf(input, input.length);
    UnsignedBytes.sort(input);
    assertThat(input).isEqualTo(expected);
  }

  static void testSort(byte[] input, int from, int to, byte[] expected) {
    input = Arrays.copyOf(input, input.length);
    UnsignedBytes.sort(input, from, to);
    assertThat(input).isEqualTo(expected);
  }

  public void testSortIndexed() {
    testSort(new byte[] {}, 0, 0, new byte[] {});
    testSort(new byte[] {2}, 0, 1, new byte[] {2});
    testSort(new byte[] {2, 1, 0}, 0, 2, new byte[] {1, 2, 0});
    testSort(new byte[] {2, GREATEST, 1, LEAST}, 1, 4, new byte[] {2, LEAST, 1, GREATEST});
  }

  public void testSortDescending() {
    testSortDescending(new byte[] {}, new byte[] {});
    testSortDescending(new byte[] {1}, new byte[] {1});
    testSortDescending(new byte[] {1, 2}, new byte[] {2, 1});
    testSortDescending(new byte[] {1, 3, 1}, new byte[] {3, 1, 1});
    testSortDescending(
        new byte[] {GREATEST - 1, 1, GREATEST - 2, 2},
        new byte[] {GREATEST - 1, GREATEST - 2, 2, 1});
  }

  private static void testSortDescending(byte[] input, byte[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    UnsignedBytes.sortDescending(input);
    assertThat(input).isEqualTo(expectedOutput);
  }

  private static void testSortDescending(
      byte[] input, int fromIndex, int toIndex, byte[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    UnsignedBytes.sortDescending(input, fromIndex, toIndex);
    assertThat(input).isEqualTo(expectedOutput);
  }

  public void testSortDescendingIndexed() {
    testSortDescending(new byte[] {}, 0, 0, new byte[] {});
    testSortDescending(new byte[] {1}, 0, 1, new byte[] {1});
    testSortDescending(new byte[] {1, 2}, 0, 2, new byte[] {2, 1});
    testSortDescending(new byte[] {1, 3, 1}, 0, 2, new byte[] {3, 1, 1});
    testSortDescending(new byte[] {1, 3, 1}, 0, 1, new byte[] {1, 3, 1});
    testSortDescending(
        new byte[] {GREATEST - 1, 1, GREATEST - 2, 2},
        1,
        3,
        new byte[] {GREATEST - 1, GREATEST - 2, 1, 2});
  }

  public void testNulls() {
    new NullPointerTester().testAllPublicStaticMethods(UnsignedBytes.class);
  }
}

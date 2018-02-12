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

import static java.lang.Long.MAX_VALUE;
import static java.lang.Long.MIN_VALUE;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
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

/**
 * Unit test for {@link Longs}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
@SuppressWarnings("cast") // redundant casts are intentional and harmless
public class LongsTest extends TestCase {
  private static final long[] EMPTY = {};
  private static final long[] ARRAY1 = {(long) 1};
  private static final long[] ARRAY234 = {(long) 2, (long) 3, (long) 4};

  private static final long[] VALUES = {MIN_VALUE, (long) -1, (long) 0, (long) 1, MAX_VALUE};

  @GwtIncompatible // Long.hashCode returns different values in GWT.
  public void testHashCode() {
    for (long value : VALUES) {
      assertEquals("hashCode for " + value, ((Long) value).hashCode(), Longs.hashCode(value));
    }
  }

  public void testCompare() {
    for (long x : VALUES) {
      for (long y : VALUES) {
        // note: spec requires only that the sign is the same
        assertEquals(x + ", " + y, Long.valueOf(x).compareTo(y), Longs.compare(x, y));
      }
    }
  }

  public void testContains() {
    assertFalse(Longs.contains(EMPTY, (long) 1));
    assertFalse(Longs.contains(ARRAY1, (long) 2));
    assertFalse(Longs.contains(ARRAY234, (long) 1));
    assertTrue(Longs.contains(new long[] {(long) -1}, (long) -1));
    assertTrue(Longs.contains(ARRAY234, (long) 2));
    assertTrue(Longs.contains(ARRAY234, (long) 3));
    assertTrue(Longs.contains(ARRAY234, (long) 4));
  }

  public void testIndexOf() {
    assertEquals(-1, Longs.indexOf(EMPTY, (long) 1));
    assertEquals(-1, Longs.indexOf(ARRAY1, (long) 2));
    assertEquals(-1, Longs.indexOf(ARRAY234, (long) 1));
    assertEquals(0, Longs.indexOf(new long[] {(long) -1}, (long) -1));
    assertEquals(0, Longs.indexOf(ARRAY234, (long) 2));
    assertEquals(1, Longs.indexOf(ARRAY234, (long) 3));
    assertEquals(2, Longs.indexOf(ARRAY234, (long) 4));
    assertEquals(1, Longs.indexOf(new long[] {(long) 2, (long) 3, (long) 2, (long) 3}, (long) 3));
  }

  public void testIndexOf_arrayTarget() {
    assertEquals(0, Longs.indexOf(EMPTY, EMPTY));
    assertEquals(0, Longs.indexOf(ARRAY234, EMPTY));
    assertEquals(-1, Longs.indexOf(EMPTY, ARRAY234));
    assertEquals(-1, Longs.indexOf(ARRAY234, ARRAY1));
    assertEquals(-1, Longs.indexOf(ARRAY1, ARRAY234));
    assertEquals(0, Longs.indexOf(ARRAY1, ARRAY1));
    assertEquals(0, Longs.indexOf(ARRAY234, ARRAY234));
    assertEquals(0, Longs.indexOf(ARRAY234, new long[] {(long) 2, (long) 3}));
    assertEquals(1, Longs.indexOf(ARRAY234, new long[] {(long) 3, (long) 4}));
    assertEquals(1, Longs.indexOf(ARRAY234, new long[] {(long) 3}));
    assertEquals(2, Longs.indexOf(ARRAY234, new long[] {(long) 4}));
    assertEquals(
        1,
        Longs.indexOf(
            new long[] {(long) 2, (long) 3, (long) 3, (long) 3, (long) 3}, new long[] {(long) 3}));
    assertEquals(
        2,
        Longs.indexOf(
            new long[] {(long) 2, (long) 3, (long) 2, (long) 3, (long) 4, (long) 2, (long) 3},
            new long[] {(long) 2, (long) 3, (long) 4}));
    assertEquals(
        1,
        Longs.indexOf(
            new long[] {(long) 2, (long) 2, (long) 3, (long) 4, (long) 2, (long) 3, (long) 4},
            new long[] {(long) 2, (long) 3, (long) 4}));
    assertEquals(
        -1,
        Longs.indexOf(
            new long[] {(long) 4, (long) 3, (long) 2}, new long[] {(long) 2, (long) 3, (long) 4}));
  }

  public void testLastIndexOf() {
    assertEquals(-1, Longs.lastIndexOf(EMPTY, (long) 1));
    assertEquals(-1, Longs.lastIndexOf(ARRAY1, (long) 2));
    assertEquals(-1, Longs.lastIndexOf(ARRAY234, (long) 1));
    assertEquals(0, Longs.lastIndexOf(new long[] {(long) -1}, (long) -1));
    assertEquals(0, Longs.lastIndexOf(ARRAY234, (long) 2));
    assertEquals(1, Longs.lastIndexOf(ARRAY234, (long) 3));
    assertEquals(2, Longs.lastIndexOf(ARRAY234, (long) 4));
    assertEquals(
        3, Longs.lastIndexOf(new long[] {(long) 2, (long) 3, (long) 2, (long) 3}, (long) 3));
  }

  public void testMax_noArgs() {
    try {
      Longs.max();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMax() {
    assertEquals(MIN_VALUE, Longs.max(MIN_VALUE));
    assertEquals(MAX_VALUE, Longs.max(MAX_VALUE));
    assertEquals(
        (long) 9, Longs.max((long) 8, (long) 6, (long) 7, (long) 5, (long) 3, (long) 0, (long) 9));
  }

  public void testMin_noArgs() {
    try {
      Longs.min();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMin() {
    assertEquals(MIN_VALUE, Longs.min(MIN_VALUE));
    assertEquals(MAX_VALUE, Longs.min(MAX_VALUE));
    assertEquals(
        (long) 0, Longs.min((long) 8, (long) 6, (long) 7, (long) 5, (long) 3, (long) 0, (long) 9));
  }

  public void testConstrainToRange() {
    assertEquals((long) 1, Longs.constrainToRange((long) 1, (long) 0, (long) 5));
    assertEquals((long) 1, Longs.constrainToRange((long) 1, (long) 1, (long) 5));
    assertEquals((long) 3, Longs.constrainToRange((long) 1, (long) 3, (long) 5));
    assertEquals((long) -1, Longs.constrainToRange((long) 0, (long) -5, (long) -1));
    assertEquals((long) 2, Longs.constrainToRange((long) 5, (long) 2, (long) 2));
    try {
      Longs.constrainToRange((long) 1, (long) 3, (long) 2);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testConcat() {
    assertTrue(Arrays.equals(EMPTY, Longs.concat()));
    assertTrue(Arrays.equals(EMPTY, Longs.concat(EMPTY)));
    assertTrue(Arrays.equals(EMPTY, Longs.concat(EMPTY, EMPTY, EMPTY)));
    assertTrue(Arrays.equals(ARRAY1, Longs.concat(ARRAY1)));
    assertNotSame(ARRAY1, Longs.concat(ARRAY1));
    assertTrue(Arrays.equals(ARRAY1, Longs.concat(EMPTY, ARRAY1, EMPTY)));
    assertTrue(
        Arrays.equals(
            new long[] {(long) 1, (long) 1, (long) 1}, Longs.concat(ARRAY1, ARRAY1, ARRAY1)));
    assertTrue(
        Arrays.equals(
            new long[] {(long) 1, (long) 2, (long) 3, (long) 4}, Longs.concat(ARRAY1, ARRAY234)));
  }

  private static void assertByteArrayEquals(byte[] expected, byte[] actual) {
    assertTrue(
        "Expected: " + Arrays.toString(expected) + ", but got: " + Arrays.toString(actual),
        Arrays.equals(expected, actual));
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
    assertEquals(
        0x1213141516171819L,
        Longs.fromByteArray(new byte[] {0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x33}));
    assertEquals(
        0xFFEEDDCCBBAA9988L,
        Longs.fromByteArray(
            new byte[] {
              (byte) 0xFF, (byte) 0xEE, (byte) 0xDD, (byte) 0xCC,
              (byte) 0xBB, (byte) 0xAA, (byte) 0x99, (byte) 0x88
            }));
  }

  public void testFromByteArrayFails() {
    try {
      Longs.fromByteArray(new byte[Longs.BYTES - 1]);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testFromBytes() {
    assertEquals(
        0x1213141516171819L,
        Longs.fromBytes(
            (byte) 0x12,
            (byte) 0x13,
            (byte) 0x14,
            (byte) 0x15,
            (byte) 0x16,
            (byte) 0x17,
            (byte) 0x18,
            (byte) 0x19));
    assertEquals(
        0xFFEEDDCCBBAA9988L,
        Longs.fromBytes(
            (byte) 0xFF,
            (byte) 0xEE,
            (byte) 0xDD,
            (byte) 0xCC,
            (byte) 0xBB,
            (byte) 0xAA,
            (byte) 0x99,
            (byte) 0x88));
  }

  public void testByteArrayRoundTrips() {
    Random r = new Random(5);
    byte[] b = new byte[Longs.BYTES];

    for (int i = 0; i < 1000; i++) {
      long num = r.nextLong();
      assertEquals(num, Longs.fromByteArray(Longs.toByteArray(num)));

      r.nextBytes(b);
      long value = Longs.fromByteArray(b);
      assertTrue("" + value, Arrays.equals(b, Longs.toByteArray(value)));
    }
  }

  public void testEnsureCapacity() {
    assertSame(EMPTY, Longs.ensureCapacity(EMPTY, 0, 1));
    assertSame(ARRAY1, Longs.ensureCapacity(ARRAY1, 0, 1));
    assertSame(ARRAY1, Longs.ensureCapacity(ARRAY1, 1, 1));
    assertTrue(
        Arrays.equals(
            new long[] {(long) 1, (long) 0, (long) 0}, Longs.ensureCapacity(ARRAY1, 2, 1)));
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
    assertEquals("", Longs.join(",", EMPTY));
    assertEquals("1", Longs.join(",", ARRAY1));
    assertEquals("1,2", Longs.join(",", (long) 1, (long) 2));
    assertEquals("123", Longs.join("", (long) 1, (long) 2, (long) 3));
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

  @GwtIncompatible // SerializableTester
  public void testLexicographicalComparatorSerializable() {
    Comparator<long[]> comparator = Longs.lexicographicalComparator();
    assertSame(comparator, SerializableTester.reserialize(comparator));
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
    assertTrue(Arrays.equals(expectedOutput, input));
  }

  private static void testReverse(long[] input, int fromIndex, int toIndex, long[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Longs.reverse(input, fromIndex, toIndex);
    assertTrue(Arrays.equals(expectedOutput, input));
  }

  public void testReverseIndexed() {
    testReverse(new long[] {}, 0, 0, new long[] {});
    testReverse(new long[] {1}, 0, 1, new long[] {1});
    testReverse(new long[] {1, 2}, 0, 2, new long[] {2, 1});
    testReverse(new long[] {3, 1, 1}, 0, 2, new long[] {1, 3, 1});
    testReverse(new long[] {3, 1, 1}, 0, 1, new long[] {3, 1, 1});
    testReverse(new long[] {-1, 1, -2, 2}, 1, 3, new long[] {-1, -2, 1, 2});
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
    assertTrue(Arrays.equals(expectedOutput, input));
  }

  private static void testSortDescending(
      long[] input, int fromIndex, int toIndex, long[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Longs.sortDescending(input, fromIndex, toIndex);
    assertTrue(Arrays.equals(expectedOutput, input));
  }

  public void testSortDescendingIndexed() {
    testSortDescending(new long[] {}, 0, 0, new long[] {});
    testSortDescending(new long[] {1}, 0, 1, new long[] {1});
    testSortDescending(new long[] {1, 2}, 0, 2, new long[] {2, 1});
    testSortDescending(new long[] {1, 3, 1}, 0, 2, new long[] {3, 1, 1});
    testSortDescending(new long[] {1, 3, 1}, 0, 1, new long[] {1, 3, 1});
    testSortDescending(new long[] {-1, -2, 1, 2}, 1, 3, new long[] {-1, 1, -2, 2});
  }

  @GwtIncompatible // SerializableTester
  public void testStringConverterSerialization() {
    SerializableTester.reserializeAndAssert(Longs.stringConverter());
  }

  public void testToArray() {
    // need explicit type parameter to avoid javac warning!?
    List<Long> none = Arrays.<Long>asList();
    assertTrue(Arrays.equals(EMPTY, Longs.toArray(none)));

    List<Long> one = Arrays.asList((long) 1);
    assertTrue(Arrays.equals(ARRAY1, Longs.toArray(one)));

    long[] array = {(long) 0, (long) 1, 0x0FF1C1AL};

    List<Long> three = Arrays.asList((long) 0, (long) 1, 0x0FF1C1AL);
    assertTrue(Arrays.equals(array, Longs.toArray(three)));

    assertTrue(Arrays.equals(array, Longs.toArray(Longs.asList(array))));
  }

  public void testToArray_threadSafe() {
    for (int delta : new int[] {+1, 0, -1}) {
      for (int i = 0; i < VALUES.length; i++) {
        List<Long> list = Longs.asList(VALUES).subList(0, i);
        Collection<Long> misleadingSize = Helpers.misleadingSizeCollection(delta);
        misleadingSize.addAll(list);
        long[] arr = Longs.toArray(misleadingSize);
        assertEquals(i, arr.length);
        for (int j = 0; j < i; j++) {
          assertEquals(VALUES[j], arr[j]);
        }
      }
    }
  }

  public void testToArray_withNull() {
    List<Long> list = Arrays.asList((long) 0, (long) 1, null);
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

    assertTrue(Arrays.equals(array, Longs.toArray(bytes)));
    assertTrue(Arrays.equals(array, Longs.toArray(shorts)));
    assertTrue(Arrays.equals(array, Longs.toArray(ints)));
    assertTrue(Arrays.equals(array, Longs.toArray(floats)));
    assertTrue(Arrays.equals(array, Longs.toArray(longs)));
    assertTrue(Arrays.equals(array, Longs.toArray(doubles)));
  }

  public void testAsList_isAView() {
    long[] array = {(long) 0, (long) 1};
    List<Long> list = Longs.asList(array);
    list.set(0, (long) 2);
    assertTrue(Arrays.equals(new long[] {(long) 2, (long) 1}, array));
    array[1] = (long) 3;
    assertEquals(Arrays.asList((long) 2, (long) 3), list);
  }

  public void testAsList_toArray_roundTrip() {
    long[] array = {(long) 0, (long) 1, (long) 2};
    List<Long> list = Longs.asList(array);
    long[] newArray = Longs.toArray(list);

    // Make sure it returned a copy
    list.set(0, (long) 4);
    assertTrue(Arrays.equals(new long[] {(long) 0, (long) 1, (long) 2}, newArray));
    newArray[1] = (long) 5;
    assertEquals((long) 1, (long) list.get(1));
  }

  // This test stems from a real bug found by andrewk
  public void testAsList_subList_toArray_roundTrip() {
    long[] array = {(long) 0, (long) 1, (long) 2, (long) 3};
    List<Long> list = Longs.asList(array);
    assertTrue(Arrays.equals(new long[] {(long) 1, (long) 2}, Longs.toArray(list.subList(1, 3))));
    assertTrue(Arrays.equals(new long[] {}, Longs.toArray(list.subList(2, 2))));
  }

  public void testAsListEmpty() {
    assertSame(Collections.emptyList(), Longs.asList(EMPTY));
  }

  @GwtIncompatible // NullPointerTester
  public void testNulls() {
    new NullPointerTester().testAllPublicStaticMethods(Longs.class);
  }

  public void testStringConverter_convert() {
    Converter<String, Long> converter = Longs.stringConverter();
    assertEquals((Long) 1L, converter.convert("1"));
    assertEquals((Long) 0L, converter.convert("0"));
    assertEquals((Long) (-1L), converter.convert("-1"));
    assertEquals((Long) 255L, converter.convert("0xff"));
    assertEquals((Long) 255L, converter.convert("0xFF"));
    assertEquals((Long) (-255L), converter.convert("-0xFF"));
    assertEquals((Long) 255L, converter.convert("#0000FF"));
    assertEquals((Long) 438L, converter.convert("0666"));
  }

  public void testStringConverter_convertError() {
    try {
      Longs.stringConverter().convert("notanumber");
      fail();
    } catch (NumberFormatException expected) {
    }
  }

  public void testStringConverter_nullConversions() {
    assertNull(Longs.stringConverter().convert(null));
    assertNull(Longs.stringConverter().reverse().convert(null));
  }

  public void testStringConverter_reverse() {
    Converter<String, Long> converter = Longs.stringConverter();
    assertEquals("1", converter.reverse().convert(1L));
    assertEquals("0", converter.reverse().convert(0L));
    assertEquals("-1", converter.reverse().convert(-1L));
    assertEquals("255", converter.reverse().convert(0xffL));
    assertEquals("255", converter.reverse().convert(0xFFL));
    assertEquals("-255", converter.reverse().convert(-0xFFL));
    assertEquals("438", converter.reverse().convert(0666L));
  }

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
    assertNull(Longs.tryParse(""));
    assertNull(Longs.tryParse("-"));
    assertNull(Longs.tryParse("+1"));
    assertNull(Longs.tryParse("999999999999999999999999"));
    assertNull(
        "Max long + 1",
        Longs.tryParse(BigInteger.valueOf(MAX_VALUE).add(BigInteger.ONE).toString()));
    assertNull(
        "Max long * 10",
        Longs.tryParse(BigInteger.valueOf(MAX_VALUE).multiply(BigInteger.TEN).toString()));
    assertNull(
        "Min long - 1",
        Longs.tryParse(BigInteger.valueOf(MIN_VALUE).subtract(BigInteger.ONE).toString()));
    assertNull(
        "Min long * 10",
        Longs.tryParse(BigInteger.valueOf(MIN_VALUE).multiply(BigInteger.TEN).toString()));
    assertNull(Longs.tryParse("\u0662\u06f3"));
  }

  /**
   * Applies {@link Longs#tryParse(String)} to the given string and asserts that the result is as
   * expected.
   */
  private static void tryParseAndAssertEquals(Long expected, String value) {
    assertEquals(expected, Longs.tryParse(value));
  }

  public void testTryParse_radix() {
    for (int radix = Character.MIN_RADIX; radix <= Character.MAX_RADIX; radix++) {
      radixEncodeParseAndAssertEquals((long) 0, radix);
      radixEncodeParseAndAssertEquals((long) 8000, radix);
      radixEncodeParseAndAssertEquals((long) -8000, radix);
      radixEncodeParseAndAssertEquals(MAX_VALUE, radix);
      radixEncodeParseAndAssertEquals(MIN_VALUE, radix);
      assertNull("Radix: " + radix, Longs.tryParse("999999999999999999999999", radix));
      assertNull(
          "Radix: " + radix,
          Longs.tryParse(BigInteger.valueOf(MAX_VALUE).add(BigInteger.ONE).toString(), radix));
      assertNull(
          "Radix: " + radix,
          Longs.tryParse(BigInteger.valueOf(MIN_VALUE).subtract(BigInteger.ONE).toString(), radix));
    }
    assertNull("Hex string and dec parm", Longs.tryParse("FFFF", 10));
    assertEquals("Mixed hex case", 65535, Longs.tryParse("ffFF", 16).longValue());
  }

  /**
   * Encodes the long as a string with given radix, then uses {@link Longs#tryParse(String, int)} to
   * parse the result. Asserts the result is the same as what we started with.
   */
  private static void radixEncodeParseAndAssertEquals(Long value, int radix) {
    assertEquals("Radix: " + radix, value, Longs.tryParse(Long.toString(value, radix), radix));
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
    assertNull(Longs.tryParse("null"));
    try {
      Longs.tryParse(null);
      fail("Expected NPE");
    } catch (NullPointerException expected) {
    }
  }
}

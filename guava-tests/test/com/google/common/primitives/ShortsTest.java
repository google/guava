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

/**
 * Unit test for {@link Shorts}.
 *
 * @author Kevin Bourrillion
 */
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
      assertEquals(((Short) value).hashCode(), Shorts.hashCode(value));
    }
  }

  public void testCheckedCast() {
    for (short value : VALUES) {
      assertEquals(value, Shorts.checkedCast((long) value));
    }
    assertCastFails(GREATEST + 1L);
    assertCastFails(LEAST - 1L);
    assertCastFails(Long.MAX_VALUE);
    assertCastFails(Long.MIN_VALUE);
  }

  public void testSaturatedCast() {
    for (short value : VALUES) {
      assertEquals(value, Shorts.saturatedCast((long) value));
    }
    assertEquals(GREATEST, Shorts.saturatedCast(GREATEST + 1L));
    assertEquals(LEAST, Shorts.saturatedCast(LEAST - 1L));
    assertEquals(GREATEST, Shorts.saturatedCast(Long.MAX_VALUE));
    assertEquals(LEAST, Shorts.saturatedCast(Long.MIN_VALUE));
  }

  private static void assertCastFails(long value) {
    try {
      Shorts.checkedCast(value);
      fail("Cast to short should have failed: " + value);
    } catch (IllegalArgumentException ex) {
      assertTrue(
          value + " not found in exception text: " + ex.getMessage(),
          ex.getMessage().contains(String.valueOf(value)));
    }
  }

  public void testCompare() {
    for (short x : VALUES) {
      for (short y : VALUES) {
        // Only compare the sign of the result of compareTo().
        int expected = Short.valueOf(x).compareTo(y);
        int actual = Shorts.compare(x, y);
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

  public void testContains() {
    assertFalse(Shorts.contains(EMPTY, (short) 1));
    assertFalse(Shorts.contains(ARRAY1, (short) 2));
    assertFalse(Shorts.contains(ARRAY234, (short) 1));
    assertTrue(Shorts.contains(new short[] {(short) -1}, (short) -1));
    assertTrue(Shorts.contains(ARRAY234, (short) 2));
    assertTrue(Shorts.contains(ARRAY234, (short) 3));
    assertTrue(Shorts.contains(ARRAY234, (short) 4));
  }

  public void testIndexOf() {
    assertEquals(-1, Shorts.indexOf(EMPTY, (short) 1));
    assertEquals(-1, Shorts.indexOf(ARRAY1, (short) 2));
    assertEquals(-1, Shorts.indexOf(ARRAY234, (short) 1));
    assertEquals(0, Shorts.indexOf(new short[] {(short) -1}, (short) -1));
    assertEquals(0, Shorts.indexOf(ARRAY234, (short) 2));
    assertEquals(1, Shorts.indexOf(ARRAY234, (short) 3));
    assertEquals(2, Shorts.indexOf(ARRAY234, (short) 4));
    assertEquals(
        1, Shorts.indexOf(new short[] {(short) 2, (short) 3, (short) 2, (short) 3}, (short) 3));
  }

  public void testIndexOf_arrayTarget() {
    assertEquals(0, Shorts.indexOf(EMPTY, EMPTY));
    assertEquals(0, Shorts.indexOf(ARRAY234, EMPTY));
    assertEquals(-1, Shorts.indexOf(EMPTY, ARRAY234));
    assertEquals(-1, Shorts.indexOf(ARRAY234, ARRAY1));
    assertEquals(-1, Shorts.indexOf(ARRAY1, ARRAY234));
    assertEquals(0, Shorts.indexOf(ARRAY1, ARRAY1));
    assertEquals(0, Shorts.indexOf(ARRAY234, ARRAY234));
    assertEquals(0, Shorts.indexOf(ARRAY234, new short[] {(short) 2, (short) 3}));
    assertEquals(1, Shorts.indexOf(ARRAY234, new short[] {(short) 3, (short) 4}));
    assertEquals(1, Shorts.indexOf(ARRAY234, new short[] {(short) 3}));
    assertEquals(2, Shorts.indexOf(ARRAY234, new short[] {(short) 4}));
    assertEquals(
        1,
        Shorts.indexOf(
            new short[] {(short) 2, (short) 3, (short) 3, (short) 3, (short) 3},
            new short[] {(short) 3}));
    assertEquals(
        2,
        Shorts.indexOf(
            new short[] {
              (short) 2, (short) 3, (short) 2, (short) 3, (short) 4, (short) 2, (short) 3
            },
            new short[] {(short) 2, (short) 3, (short) 4}));
    assertEquals(
        1,
        Shorts.indexOf(
            new short[] {
              (short) 2, (short) 2, (short) 3, (short) 4, (short) 2, (short) 3, (short) 4
            },
            new short[] {(short) 2, (short) 3, (short) 4}));
    assertEquals(
        -1,
        Shorts.indexOf(
            new short[] {(short) 4, (short) 3, (short) 2},
            new short[] {(short) 2, (short) 3, (short) 4}));
  }

  public void testLastIndexOf() {
    assertEquals(-1, Shorts.lastIndexOf(EMPTY, (short) 1));
    assertEquals(-1, Shorts.lastIndexOf(ARRAY1, (short) 2));
    assertEquals(-1, Shorts.lastIndexOf(ARRAY234, (short) 1));
    assertEquals(0, Shorts.lastIndexOf(new short[] {(short) -1}, (short) -1));
    assertEquals(0, Shorts.lastIndexOf(ARRAY234, (short) 2));
    assertEquals(1, Shorts.lastIndexOf(ARRAY234, (short) 3));
    assertEquals(2, Shorts.lastIndexOf(ARRAY234, (short) 4));
    assertEquals(
        3, Shorts.lastIndexOf(new short[] {(short) 2, (short) 3, (short) 2, (short) 3}, (short) 3));
  }

  public void testMax_noArgs() {
    try {
      Shorts.max();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMax() {
    assertEquals(LEAST, Shorts.max(LEAST));
    assertEquals(GREATEST, Shorts.max(GREATEST));
    assertEquals(
        (short) 9,
        Shorts.max((short) 8, (short) 6, (short) 7, (short) 5, (short) 3, (short) 0, (short) 9));
  }

  public void testMin_noArgs() {
    try {
      Shorts.min();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMin() {
    assertEquals(LEAST, Shorts.min(LEAST));
    assertEquals(GREATEST, Shorts.min(GREATEST));
    assertEquals(
        (short) 0,
        Shorts.min((short) 8, (short) 6, (short) 7, (short) 5, (short) 3, (short) 0, (short) 9));
  }

  public void testConstrainToRange() {
    assertEquals((short) 1, Shorts.constrainToRange((short) 1, (short) 0, (short) 5));
    assertEquals((short) 1, Shorts.constrainToRange((short) 1, (short) 1, (short) 5));
    assertEquals((short) 3, Shorts.constrainToRange((short) 1, (short) 3, (short) 5));
    assertEquals((short) -1, Shorts.constrainToRange((short) 0, (short) -5, (short) -1));
    assertEquals((short) 2, Shorts.constrainToRange((short) 5, (short) 2, (short) 2));
    try {
      Shorts.constrainToRange((short) 1, (short) 3, (short) 2);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testConcat() {
    assertTrue(Arrays.equals(EMPTY, Shorts.concat()));
    assertTrue(Arrays.equals(EMPTY, Shorts.concat(EMPTY)));
    assertTrue(Arrays.equals(EMPTY, Shorts.concat(EMPTY, EMPTY, EMPTY)));
    assertTrue(Arrays.equals(ARRAY1, Shorts.concat(ARRAY1)));
    assertNotSame(ARRAY1, Shorts.concat(ARRAY1));
    assertTrue(Arrays.equals(ARRAY1, Shorts.concat(EMPTY, ARRAY1, EMPTY)));
    assertTrue(
        Arrays.equals(
            new short[] {(short) 1, (short) 1, (short) 1}, Shorts.concat(ARRAY1, ARRAY1, ARRAY1)));
    assertTrue(
        Arrays.equals(
            new short[] {(short) 1, (short) 2, (short) 3, (short) 4},
            Shorts.concat(ARRAY1, ARRAY234)));
  }

  @GwtIncompatible // Shorts.toByteArray
  public void testToByteArray() {
    assertTrue(Arrays.equals(new byte[] {0x23, 0x45}, Shorts.toByteArray((short) 0x2345)));
    assertTrue(
        Arrays.equals(new byte[] {(byte) 0xFE, (byte) 0xDC}, Shorts.toByteArray((short) 0xFEDC)));
  }

  @GwtIncompatible // Shorts.fromByteArray
  public void testFromByteArray() {
    assertEquals((short) 0x2345, Shorts.fromByteArray(new byte[] {0x23, 0x45}));
    assertEquals((short) 0xFEDC, Shorts.fromByteArray(new byte[] {(byte) 0xFE, (byte) 0xDC}));
  }

  @GwtIncompatible // Shorts.fromByteArray
  public void testFromByteArrayFails() {
    try {
      Shorts.fromByteArray(new byte[] {0x01});
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @GwtIncompatible // Shorts.fromBytes
  public void testFromBytes() {
    assertEquals((short) 0x2345, Shorts.fromBytes((byte) 0x23, (byte) 0x45));
    assertEquals((short) 0xFEDC, Shorts.fromBytes((byte) 0xFE, (byte) 0xDC));
  }

  @GwtIncompatible // Shorts.fromByteArray, Shorts.toByteArray
  public void testByteArrayRoundTrips() {
    Random r = new Random(5);
    byte[] b = new byte[Shorts.BYTES];

    // total overkill, but, it takes 0.1 sec so why not...
    for (int i = 0; i < 10000; i++) {
      short num = (short) r.nextInt();
      assertEquals(num, Shorts.fromByteArray(Shorts.toByteArray(num)));

      r.nextBytes(b);
      assertTrue(Arrays.equals(b, Shorts.toByteArray(Shorts.fromByteArray(b))));
    }
  }

  public void testEnsureCapacity() {
    assertSame(EMPTY, Shorts.ensureCapacity(EMPTY, 0, 1));
    assertSame(ARRAY1, Shorts.ensureCapacity(ARRAY1, 0, 1));
    assertSame(ARRAY1, Shorts.ensureCapacity(ARRAY1, 1, 1));
    assertTrue(
        Arrays.equals(
            new short[] {(short) 1, (short) 0, (short) 0}, Shorts.ensureCapacity(ARRAY1, 2, 1)));
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
    assertEquals("", Shorts.join(",", EMPTY));
    assertEquals("1", Shorts.join(",", ARRAY1));
    assertEquals("1,2", Shorts.join(",", (short) 1, (short) 2));
    assertEquals("123", Shorts.join("", (short) 1, (short) 2, (short) 3));
  }

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

  @GwtIncompatible // SerializableTester
  public void testLexicographicalComparatorSerializable() {
    Comparator<short[]> comparator = Shorts.lexicographicalComparator();
    assertSame(comparator, SerializableTester.reserialize(comparator));
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
    assertTrue(Arrays.equals(expectedOutput, input));
  }

  private static void testReverse(
      short[] input, int fromIndex, int toIndex, short[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Shorts.reverse(input, fromIndex, toIndex);
    assertTrue(Arrays.equals(expectedOutput, input));
  }

  public void testReverseIndexed() {
    testReverse(new short[] {}, 0, 0, new short[] {});
    testReverse(new short[] {1}, 0, 1, new short[] {1});
    testReverse(new short[] {1, 2}, 0, 2, new short[] {2, 1});
    testReverse(new short[] {3, 1, 1}, 0, 2, new short[] {1, 3, 1});
    testReverse(new short[] {3, 1, 1}, 0, 1, new short[] {3, 1, 1});
    testReverse(new short[] {-1, 1, -2, 2}, 1, 3, new short[] {-1, -2, 1, 2});
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
    assertTrue(Arrays.equals(expectedOutput, input));
  }

  private static void testSortDescending(
      short[] input, int fromIndex, int toIndex, short[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Shorts.sortDescending(input, fromIndex, toIndex);
    assertTrue(Arrays.equals(expectedOutput, input));
  }

  public void testSortDescendingIndexed() {
    testSortDescending(new short[] {}, 0, 0, new short[] {});
    testSortDescending(new short[] {1}, 0, 1, new short[] {1});
    testSortDescending(new short[] {1, 2}, 0, 2, new short[] {2, 1});
    testSortDescending(new short[] {1, 3, 1}, 0, 2, new short[] {3, 1, 1});
    testSortDescending(new short[] {1, 3, 1}, 0, 1, new short[] {1, 3, 1});
    testSortDescending(new short[] {-1, -2, 1, 2}, 1, 3, new short[] {-1, 1, -2, 2});
  }

  @GwtIncompatible // SerializableTester
  public void testStringConverterSerialization() {
    SerializableTester.reserializeAndAssert(Shorts.stringConverter());
  }

  public void testToArray() {
    // need explicit type parameter to avoid javac warning!?
    List<Short> none = Arrays.<Short>asList();
    assertTrue(Arrays.equals(EMPTY, Shorts.toArray(none)));

    List<Short> one = Arrays.asList((short) 1);
    assertTrue(Arrays.equals(ARRAY1, Shorts.toArray(one)));

    short[] array = {(short) 0, (short) 1, (short) 3};

    List<Short> three = Arrays.asList((short) 0, (short) 1, (short) 3);
    assertTrue(Arrays.equals(array, Shorts.toArray(three)));

    assertTrue(Arrays.equals(array, Shorts.toArray(Shorts.asList(array))));
  }

  public void testToArray_threadSafe() {
    for (int delta : new int[] {+1, 0, -1}) {
      for (int i = 0; i < VALUES.length; i++) {
        List<Short> list = Shorts.asList(VALUES).subList(0, i);
        Collection<Short> misleadingSize = Helpers.misleadingSizeCollection(delta);
        misleadingSize.addAll(list);
        short[] arr = Shorts.toArray(misleadingSize);
        assertEquals(i, arr.length);
        for (int j = 0; j < i; j++) {
          assertEquals(VALUES[j], arr[j]);
        }
      }
    }
  }

  public void testToArray_withNull() {
    List<Short> list = Arrays.asList((short) 0, (short) 1, null);
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

    assertTrue(Arrays.equals(array, Shorts.toArray(bytes)));
    assertTrue(Arrays.equals(array, Shorts.toArray(shorts)));
    assertTrue(Arrays.equals(array, Shorts.toArray(ints)));
    assertTrue(Arrays.equals(array, Shorts.toArray(floats)));
    assertTrue(Arrays.equals(array, Shorts.toArray(longs)));
    assertTrue(Arrays.equals(array, Shorts.toArray(doubles)));
  }

  public void testAsList_isAView() {
    short[] array = {(short) 0, (short) 1};
    List<Short> list = Shorts.asList(array);
    list.set(0, (short) 2);
    assertTrue(Arrays.equals(new short[] {(short) 2, (short) 1}, array));
    array[1] = (short) 3;
    assertEquals(Arrays.asList((short) 2, (short) 3), list);
  }

  public void testAsList_toArray_roundTrip() {
    short[] array = {(short) 0, (short) 1, (short) 2};
    List<Short> list = Shorts.asList(array);
    short[] newArray = Shorts.toArray(list);

    // Make sure it returned a copy
    list.set(0, (short) 4);
    assertTrue(Arrays.equals(new short[] {(short) 0, (short) 1, (short) 2}, newArray));
    newArray[1] = (short) 5;
    assertEquals((short) 1, (short) list.get(1));
  }

  // This test stems from a real bug found by andrewk
  public void testAsList_subList_toArray_roundTrip() {
    short[] array = {(short) 0, (short) 1, (short) 2, (short) 3};
    List<Short> list = Shorts.asList(array);
    assertTrue(
        Arrays.equals(new short[] {(short) 1, (short) 2}, Shorts.toArray(list.subList(1, 3))));
    assertTrue(Arrays.equals(new short[] {}, Shorts.toArray(list.subList(2, 2))));
  }

  public void testAsListEmpty() {
    assertSame(Collections.emptyList(), Shorts.asList(EMPTY));
  }

  @GwtIncompatible // NullPointerTester
  public void testNulls() {
    new NullPointerTester().testAllPublicStaticMethods(Shorts.class);
  }

  public void testStringConverter_convert() {
    Converter<String, Short> converter = Shorts.stringConverter();
    assertEquals((Short) (short) 1, converter.convert("1"));
    assertEquals((Short) (short) 0, converter.convert("0"));
    assertEquals((Short) (short) (-1), converter.convert("-1"));
    assertEquals((Short) (short) 255, converter.convert("0xff"));
    assertEquals((Short) (short) 255, converter.convert("0xFF"));
    assertEquals((Short) (short) (-255), converter.convert("-0xFF"));
    assertEquals((Short) (short) 255, converter.convert("#0000FF"));
    assertEquals((Short) (short) 438, converter.convert("0666"));
  }

  public void testStringConverter_convertError() {
    try {
      Shorts.stringConverter().convert("notanumber");
      fail();
    } catch (NumberFormatException expected) {
    }
  }

  public void testStringConverter_nullConversions() {
    assertNull(Shorts.stringConverter().convert(null));
    assertNull(Shorts.stringConverter().reverse().convert(null));
  }

  public void testStringConverter_reverse() {
    Converter<String, Short> converter = Shorts.stringConverter();
    assertEquals("1", converter.reverse().convert((short) 1));
    assertEquals("0", converter.reverse().convert((short) 0));
    assertEquals("-1", converter.reverse().convert((short) -1));
    assertEquals("255", converter.reverse().convert((short) 0xff));
    assertEquals("255", converter.reverse().convert((short) 0xFF));
    assertEquals("-255", converter.reverse().convert((short) -0xFF));
    assertEquals("438", converter.reverse().convert((short) 0666));
  }

  @GwtIncompatible // NullPointerTester
  public void testStringConverter_nullPointerTester() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicInstanceMethods(Shorts.stringConverter());
  }
}

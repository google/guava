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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import junit.framework.TestCase;

/**
 * Unit test for {@link Chars}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
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
      assertEquals(((Character) value).hashCode(), Chars.hashCode(value));
    }
  }

  public void testCheckedCast() {
    for (char value : VALUES) {
      assertEquals(value, Chars.checkedCast((long) value));
    }
    assertCastFails(GREATEST + 1L);
    assertCastFails(LEAST - 1L);
    assertCastFails(Long.MAX_VALUE);
    assertCastFails(Long.MIN_VALUE);
  }

  public void testSaturatedCast() {
    for (char value : VALUES) {
      assertEquals(value, Chars.saturatedCast((long) value));
    }
    assertEquals(GREATEST, Chars.saturatedCast(GREATEST + 1L));
    assertEquals(LEAST, Chars.saturatedCast(LEAST - 1L));
    assertEquals(GREATEST, Chars.saturatedCast(Long.MAX_VALUE));
    assertEquals(LEAST, Chars.saturatedCast(Long.MIN_VALUE));
  }

  private void assertCastFails(long value) {
    try {
      Chars.checkedCast(value);
      fail("Cast to char should have failed: " + value);
    } catch (IllegalArgumentException ex) {
      assertTrue(
          value + " not found in exception text: " + ex.getMessage(),
          ex.getMessage().contains(String.valueOf(value)));
    }
  }

  public void testCompare() {
    for (char x : VALUES) {
      for (char y : VALUES) {
        // note: spec requires only that the sign is the same
        assertEquals(x + ", " + y, Character.valueOf(x).compareTo(y), Chars.compare(x, y));
      }
    }
  }

  public void testContains() {
    assertFalse(Chars.contains(EMPTY, (char) 1));
    assertFalse(Chars.contains(ARRAY1, (char) 2));
    assertFalse(Chars.contains(ARRAY234, (char) 1));
    assertTrue(Chars.contains(new char[] {(char) -1}, (char) -1));
    assertTrue(Chars.contains(ARRAY234, (char) 2));
    assertTrue(Chars.contains(ARRAY234, (char) 3));
    assertTrue(Chars.contains(ARRAY234, (char) 4));
  }

  public void testIndexOf() {
    assertEquals(-1, Chars.indexOf(EMPTY, (char) 1));
    assertEquals(-1, Chars.indexOf(ARRAY1, (char) 2));
    assertEquals(-1, Chars.indexOf(ARRAY234, (char) 1));
    assertEquals(0, Chars.indexOf(new char[] {(char) -1}, (char) -1));
    assertEquals(0, Chars.indexOf(ARRAY234, (char) 2));
    assertEquals(1, Chars.indexOf(ARRAY234, (char) 3));
    assertEquals(2, Chars.indexOf(ARRAY234, (char) 4));
    assertEquals(1, Chars.indexOf(new char[] {(char) 2, (char) 3, (char) 2, (char) 3}, (char) 3));
  }

  public void testIndexOf_arrayTarget() {
    assertEquals(0, Chars.indexOf(EMPTY, EMPTY));
    assertEquals(0, Chars.indexOf(ARRAY234, EMPTY));
    assertEquals(-1, Chars.indexOf(EMPTY, ARRAY234));
    assertEquals(-1, Chars.indexOf(ARRAY234, ARRAY1));
    assertEquals(-1, Chars.indexOf(ARRAY1, ARRAY234));
    assertEquals(0, Chars.indexOf(ARRAY1, ARRAY1));
    assertEquals(0, Chars.indexOf(ARRAY234, ARRAY234));
    assertEquals(0, Chars.indexOf(ARRAY234, new char[] {(char) 2, (char) 3}));
    assertEquals(1, Chars.indexOf(ARRAY234, new char[] {(char) 3, (char) 4}));
    assertEquals(1, Chars.indexOf(ARRAY234, new char[] {(char) 3}));
    assertEquals(2, Chars.indexOf(ARRAY234, new char[] {(char) 4}));
    assertEquals(
        1,
        Chars.indexOf(
            new char[] {(char) 2, (char) 3, (char) 3, (char) 3, (char) 3}, new char[] {(char) 3}));
    assertEquals(
        2,
        Chars.indexOf(
            new char[] {(char) 2, (char) 3, (char) 2, (char) 3, (char) 4, (char) 2, (char) 3},
            new char[] {(char) 2, (char) 3, (char) 4}));
    assertEquals(
        1,
        Chars.indexOf(
            new char[] {(char) 2, (char) 2, (char) 3, (char) 4, (char) 2, (char) 3, (char) 4},
            new char[] {(char) 2, (char) 3, (char) 4}));
    assertEquals(
        -1,
        Chars.indexOf(
            new char[] {(char) 4, (char) 3, (char) 2}, new char[] {(char) 2, (char) 3, (char) 4}));
  }

  public void testLastIndexOf() {
    assertEquals(-1, Chars.lastIndexOf(EMPTY, (char) 1));
    assertEquals(-1, Chars.lastIndexOf(ARRAY1, (char) 2));
    assertEquals(-1, Chars.lastIndexOf(ARRAY234, (char) 1));
    assertEquals(0, Chars.lastIndexOf(new char[] {(char) -1}, (char) -1));
    assertEquals(0, Chars.lastIndexOf(ARRAY234, (char) 2));
    assertEquals(1, Chars.lastIndexOf(ARRAY234, (char) 3));
    assertEquals(2, Chars.lastIndexOf(ARRAY234, (char) 4));
    assertEquals(
        3, Chars.lastIndexOf(new char[] {(char) 2, (char) 3, (char) 2, (char) 3}, (char) 3));
  }

  public void testMax_noArgs() {
    try {
      Chars.max();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMax() {
    assertEquals(LEAST, Chars.max(LEAST));
    assertEquals(GREATEST, Chars.max(GREATEST));
    assertEquals(
        (char) 9, Chars.max((char) 8, (char) 6, (char) 7, (char) 5, (char) 3, (char) 0, (char) 9));
  }

  public void testMin_noArgs() {
    try {
      Chars.min();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMin() {
    assertEquals(LEAST, Chars.min(LEAST));
    assertEquals(GREATEST, Chars.min(GREATEST));
    assertEquals(
        (char) 0, Chars.min((char) 8, (char) 6, (char) 7, (char) 5, (char) 3, (char) 0, (char) 9));
  }

  public void testConstrainToRange() {
    assertEquals((char) 1, Chars.constrainToRange((char) 1, (char) 0, (char) 5));
    assertEquals((char) 1, Chars.constrainToRange((char) 1, (char) 1, (char) 5));
    assertEquals((char) 3, Chars.constrainToRange((char) 1, (char) 3, (char) 5));
    assertEquals((char) 254, Chars.constrainToRange((char) 255, (char) 250, (char) 254));
    assertEquals((char) 2, Chars.constrainToRange((char) 5, (char) 2, (char) 2));
    try {
      Chars.constrainToRange((char) 1, (char) 3, (char) 2);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testConcat() {
    assertTrue(Arrays.equals(EMPTY, Chars.concat()));
    assertTrue(Arrays.equals(EMPTY, Chars.concat(EMPTY)));
    assertTrue(Arrays.equals(EMPTY, Chars.concat(EMPTY, EMPTY, EMPTY)));
    assertTrue(Arrays.equals(ARRAY1, Chars.concat(ARRAY1)));
    assertNotSame(ARRAY1, Chars.concat(ARRAY1));
    assertTrue(Arrays.equals(ARRAY1, Chars.concat(EMPTY, ARRAY1, EMPTY)));
    assertTrue(
        Arrays.equals(
            new char[] {(char) 1, (char) 1, (char) 1}, Chars.concat(ARRAY1, ARRAY1, ARRAY1)));
    assertTrue(
        Arrays.equals(
            new char[] {(char) 1, (char) 2, (char) 3, (char) 4}, Chars.concat(ARRAY1, ARRAY234)));
  }

  @GwtIncompatible // Chars.fromByteArray
  public void testFromByteArray() {
    assertEquals('\u2345', Chars.fromByteArray(new byte[] {0x23, 0x45, (byte) 0xDC}));
    assertEquals('\uFEDC', Chars.fromByteArray(new byte[] {(byte) 0xFE, (byte) 0xDC}));
  }

  @GwtIncompatible // Chars.fromByteArray
  public void testFromByteArrayFails() {
    try {
      Chars.fromByteArray(new byte[Chars.BYTES - 1]);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @GwtIncompatible // Chars.fromBytes
  public void testFromBytes() {
    assertEquals('\u2345', Chars.fromBytes((byte) 0x23, (byte) 0x45));
    assertEquals('\uFEDC', Chars.fromBytes((byte) 0xFE, (byte) 0xDC));
  }

  @GwtIncompatible // Chars.fromByteArray, Chars.toByteArray
  public void testByteArrayRoundTrips() {
    char c = 0;
    for (int hi = 0; hi < 256; hi++) {
      for (int lo = 0; lo < 256; lo++) {
        char result = Chars.fromByteArray(new byte[] {(byte) hi, (byte) lo});
        assertEquals(
            String.format(
                Locale.ROOT, "hi=%s, lo=%s, expected=%s, result=%s", hi, lo, (int) c, (int) result),
            c,
            result);

        byte[] bytes = Chars.toByteArray(c);
        assertEquals((byte) hi, bytes[0]);
        assertEquals((byte) lo, bytes[1]);

        c++;
      }
    }
    assertEquals((char) 0, c); // sanity check
  }

  @GwtIncompatible // Chars.fromByteArray, Chars.toByteArray
  public void testByteArrayRoundTripsFails() {
    try {
      Chars.fromByteArray(new byte[] {0x11});
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testEnsureCapacity() {
    assertSame(EMPTY, Chars.ensureCapacity(EMPTY, 0, 1));
    assertSame(ARRAY1, Chars.ensureCapacity(ARRAY1, 0, 1));
    assertSame(ARRAY1, Chars.ensureCapacity(ARRAY1, 1, 1));
    assertTrue(
        Arrays.equals(
            new char[] {(char) 1, (char) 0, (char) 0}, Chars.ensureCapacity(ARRAY1, 2, 1)));
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
    assertEquals("", Chars.join(",", EMPTY));
    assertEquals("1", Chars.join(",", '1'));
    assertEquals("1,2", Chars.join(",", '1', '2'));
    assertEquals("123", Chars.join("", '1', '2', '3'));
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

  @GwtIncompatible // SerializableTester
  public void testLexicographicalComparatorSerializable() {
    Comparator<char[]> comparator = Chars.lexicographicalComparator();
    assertSame(comparator, SerializableTester.reserialize(comparator));
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
    assertTrue(Arrays.equals(expectedOutput, input));
  }

  private static void testReverse(char[] input, int fromIndex, int toIndex, char[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Chars.reverse(input, fromIndex, toIndex);
    assertTrue(Arrays.equals(expectedOutput, input));
  }

  public void testReverseIndexed() {
    testReverse(new char[] {}, 0, 0, new char[] {});
    testReverse(new char[] {'1'}, 0, 1, new char[] {'1'});
    testReverse(new char[] {'1', '2'}, 0, 2, new char[] {'2', '1'});
    testReverse(new char[] {'3', '1', '1'}, 0, 2, new char[] {'1', '3', '1'});
    testReverse(new char[] {'3', '1', '1'}, 0, 1, new char[] {'3', '1', '1'});
    testReverse(new char[] {'A', '1', 'B', '2'}, 1, 3, new char[] {'A', 'B', '1', '2'});
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
    assertTrue(Arrays.equals(expectedOutput, input));
  }

  private static void testSortDescending(
      char[] input, int fromIndex, int toIndex, char[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Chars.sortDescending(input, fromIndex, toIndex);
    assertTrue(Arrays.equals(expectedOutput, input));
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
    assertTrue(Arrays.equals(EMPTY, Chars.toArray(none)));

    List<Character> one = Arrays.asList((char) 1);
    assertTrue(Arrays.equals(ARRAY1, Chars.toArray(one)));

    char[] array = {(char) 0, (char) 1, 'A'};

    List<Character> three = Arrays.asList((char) 0, (char) 1, 'A');
    assertTrue(Arrays.equals(array, Chars.toArray(three)));

    assertTrue(Arrays.equals(array, Chars.toArray(Chars.asList(array))));
  }

  public void testToArray_threadSafe() {
    for (int delta : new int[] {+1, 0, -1}) {
      for (int i = 0; i < VALUES.length; i++) {
        List<Character> list = Chars.asList(VALUES).subList(0, i);
        Collection<Character> misleadingSize = Helpers.misleadingSizeCollection(delta);
        misleadingSize.addAll(list);
        char[] arr = Chars.toArray(misleadingSize);
        assertEquals(i, arr.length);
        for (int j = 0; j < i; j++) {
          assertEquals(VALUES[j], arr[j]);
        }
      }
    }
  }

  public void testToArray_withNull() {
    List<Character> list = Arrays.asList((char) 0, (char) 1, null);
    try {
      Chars.toArray(list);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testAsList_isAView() {
    char[] array = {(char) 0, (char) 1};
    List<Character> list = Chars.asList(array);
    list.set(0, (char) 2);
    assertTrue(Arrays.equals(new char[] {(char) 2, (char) 1}, array));
    array[1] = (char) 3;
    assertEquals(Arrays.asList((char) 2, (char) 3), list);
  }

  public void testAsList_toArray_roundTrip() {
    char[] array = {(char) 0, (char) 1, (char) 2};
    List<Character> list = Chars.asList(array);
    char[] newArray = Chars.toArray(list);

    // Make sure it returned a copy
    list.set(0, (char) 4);
    assertTrue(Arrays.equals(new char[] {(char) 0, (char) 1, (char) 2}, newArray));
    newArray[1] = (char) 5;
    assertEquals((char) 1, (char) list.get(1));
  }

  // This test stems from a real bug found by andrewk
  public void testAsList_subList_toArray_roundTrip() {
    char[] array = {(char) 0, (char) 1, (char) 2, (char) 3};
    List<Character> list = Chars.asList(array);
    assertTrue(Arrays.equals(new char[] {(char) 1, (char) 2}, Chars.toArray(list.subList(1, 3))));
    assertTrue(Arrays.equals(new char[] {}, Chars.toArray(list.subList(2, 2))));
  }

  public void testAsListEmpty() {
    assertSame(Collections.emptyList(), Chars.asList(EMPTY));
  }

  @GwtIncompatible // NullPointerTester
  public void testNulls() {
    new NullPointerTester().testAllPublicStaticMethods(Chars.class);
  }
}

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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import junit.framework.TestCase;

/**
 * Unit test for {@link Bytes}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
public class BytesTest extends TestCase {
  private static final byte[] EMPTY = {};
  private static final byte[] ARRAY1 = {(byte) 1};
  private static final byte[] ARRAY234 = {(byte) 2, (byte) 3, (byte) 4};

  private static final byte[] VALUES = {Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE};

  public void testHashCode() {
    for (byte value : VALUES) {
      assertEquals(((Byte) value).hashCode(), Bytes.hashCode(value));
    }
  }

  public void testContains() {
    assertFalse(Bytes.contains(EMPTY, (byte) 1));
    assertFalse(Bytes.contains(ARRAY1, (byte) 2));
    assertFalse(Bytes.contains(ARRAY234, (byte) 1));
    assertTrue(Bytes.contains(new byte[] {(byte) -1}, (byte) -1));
    assertTrue(Bytes.contains(ARRAY234, (byte) 2));
    assertTrue(Bytes.contains(ARRAY234, (byte) 3));
    assertTrue(Bytes.contains(ARRAY234, (byte) 4));
  }

  public void testIndexOf() {
    assertEquals(-1, Bytes.indexOf(EMPTY, (byte) 1));
    assertEquals(-1, Bytes.indexOf(ARRAY1, (byte) 2));
    assertEquals(-1, Bytes.indexOf(ARRAY234, (byte) 1));
    assertEquals(0, Bytes.indexOf(new byte[] {(byte) -1}, (byte) -1));
    assertEquals(0, Bytes.indexOf(ARRAY234, (byte) 2));
    assertEquals(1, Bytes.indexOf(ARRAY234, (byte) 3));
    assertEquals(2, Bytes.indexOf(ARRAY234, (byte) 4));
    assertEquals(1, Bytes.indexOf(new byte[] {(byte) 2, (byte) 3, (byte) 2, (byte) 3}, (byte) 3));
  }

  public void testIndexOf_arrayTarget() {
    assertEquals(0, Bytes.indexOf(EMPTY, EMPTY));
    assertEquals(0, Bytes.indexOf(ARRAY234, EMPTY));
    assertEquals(-1, Bytes.indexOf(EMPTY, ARRAY234));
    assertEquals(-1, Bytes.indexOf(ARRAY234, ARRAY1));
    assertEquals(-1, Bytes.indexOf(ARRAY1, ARRAY234));
    assertEquals(0, Bytes.indexOf(ARRAY1, ARRAY1));
    assertEquals(0, Bytes.indexOf(ARRAY234, ARRAY234));
    assertEquals(0, Bytes.indexOf(ARRAY234, new byte[] {(byte) 2, (byte) 3}));
    assertEquals(1, Bytes.indexOf(ARRAY234, new byte[] {(byte) 3, (byte) 4}));
    assertEquals(1, Bytes.indexOf(ARRAY234, new byte[] {(byte) 3}));
    assertEquals(2, Bytes.indexOf(ARRAY234, new byte[] {(byte) 4}));
    assertEquals(
        1,
        Bytes.indexOf(
            new byte[] {(byte) 2, (byte) 3, (byte) 3, (byte) 3, (byte) 3}, new byte[] {(byte) 3}));
    assertEquals(
        2,
        Bytes.indexOf(
            new byte[] {(byte) 2, (byte) 3, (byte) 2, (byte) 3, (byte) 4, (byte) 2, (byte) 3},
            new byte[] {(byte) 2, (byte) 3, (byte) 4}));
    assertEquals(
        1,
        Bytes.indexOf(
            new byte[] {(byte) 2, (byte) 2, (byte) 3, (byte) 4, (byte) 2, (byte) 3, (byte) 4},
            new byte[] {(byte) 2, (byte) 3, (byte) 4}));
    assertEquals(
        -1,
        Bytes.indexOf(
            new byte[] {(byte) 4, (byte) 3, (byte) 2}, new byte[] {(byte) 2, (byte) 3, (byte) 4}));
  }

  public void testLastIndexOf() {
    assertEquals(-1, Bytes.lastIndexOf(EMPTY, (byte) 1));
    assertEquals(-1, Bytes.lastIndexOf(ARRAY1, (byte) 2));
    assertEquals(-1, Bytes.lastIndexOf(ARRAY234, (byte) 1));
    assertEquals(0, Bytes.lastIndexOf(new byte[] {(byte) -1}, (byte) -1));
    assertEquals(0, Bytes.lastIndexOf(ARRAY234, (byte) 2));
    assertEquals(1, Bytes.lastIndexOf(ARRAY234, (byte) 3));
    assertEquals(2, Bytes.lastIndexOf(ARRAY234, (byte) 4));
    assertEquals(
        3, Bytes.lastIndexOf(new byte[] {(byte) 2, (byte) 3, (byte) 2, (byte) 3}, (byte) 3));
  }

  public void testConcat() {
    assertTrue(Arrays.equals(EMPTY, Bytes.concat()));
    assertTrue(Arrays.equals(EMPTY, Bytes.concat(EMPTY)));
    assertTrue(Arrays.equals(EMPTY, Bytes.concat(EMPTY, EMPTY, EMPTY)));
    assertTrue(Arrays.equals(ARRAY1, Bytes.concat(ARRAY1)));
    assertNotSame(ARRAY1, Bytes.concat(ARRAY1));
    assertTrue(Arrays.equals(ARRAY1, Bytes.concat(EMPTY, ARRAY1, EMPTY)));
    assertTrue(
        Arrays.equals(
            new byte[] {(byte) 1, (byte) 1, (byte) 1}, Bytes.concat(ARRAY1, ARRAY1, ARRAY1)));
    assertTrue(
        Arrays.equals(
            new byte[] {(byte) 1, (byte) 2, (byte) 3, (byte) 4}, Bytes.concat(ARRAY1, ARRAY234)));
  }

  public void testEnsureCapacity() {
    assertSame(EMPTY, Bytes.ensureCapacity(EMPTY, 0, 1));
    assertSame(ARRAY1, Bytes.ensureCapacity(ARRAY1, 0, 1));
    assertSame(ARRAY1, Bytes.ensureCapacity(ARRAY1, 1, 1));
    assertTrue(
        Arrays.equals(
            new byte[] {(byte) 1, (byte) 0, (byte) 0}, Bytes.ensureCapacity(ARRAY1, 2, 1)));
  }

  public void testEnsureCapacity_fail() {
    try {
      Bytes.ensureCapacity(ARRAY1, -1, 1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      // notice that this should even fail when no growth was needed
      Bytes.ensureCapacity(ARRAY1, 1, -1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testToArray() {
    // need explicit type parameter to avoid javac warning!?
    List<Byte> none = Arrays.<Byte>asList();
    assertTrue(Arrays.equals(EMPTY, Bytes.toArray(none)));

    List<Byte> one = Arrays.asList((byte) 1);
    assertTrue(Arrays.equals(ARRAY1, Bytes.toArray(one)));

    byte[] array = {(byte) 0, (byte) 1, (byte) 0x55};

    List<Byte> three = Arrays.asList((byte) 0, (byte) 1, (byte) 0x55);
    assertTrue(Arrays.equals(array, Bytes.toArray(three)));

    assertTrue(Arrays.equals(array, Bytes.toArray(Bytes.asList(array))));
  }

  public void testToArray_threadSafe() {
    for (int delta : new int[] {+1, 0, -1}) {
      for (int i = 0; i < VALUES.length; i++) {
        List<Byte> list = Bytes.asList(VALUES).subList(0, i);
        Collection<Byte> misleadingSize = Helpers.misleadingSizeCollection(delta);
        misleadingSize.addAll(list);
        byte[] arr = Bytes.toArray(misleadingSize);
        assertEquals(i, arr.length);
        for (int j = 0; j < i; j++) {
          assertEquals(VALUES[j], arr[j]);
        }
      }
    }
  }

  public void testToArray_withNull() {
    List<Byte> list = Arrays.asList((byte) 0, (byte) 1, null);
    try {
      Bytes.toArray(list);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testToArray_withConversion() {
    byte[] array = {(byte) 0, (byte) 1, (byte) 2};

    List<Byte> bytes = Arrays.asList((byte) 0, (byte) 1, (byte) 2);
    List<Short> shorts = Arrays.asList((short) 0, (short) 1, (short) 2);
    List<Integer> ints = Arrays.asList(0, 1, 2);
    List<Float> floats = Arrays.asList((float) 0, (float) 1, (float) 2);
    List<Long> longs = Arrays.asList((long) 0, (long) 1, (long) 2);
    List<Double> doubles = Arrays.asList((double) 0, (double) 1, (double) 2);

    assertTrue(Arrays.equals(array, Bytes.toArray(bytes)));
    assertTrue(Arrays.equals(array, Bytes.toArray(shorts)));
    assertTrue(Arrays.equals(array, Bytes.toArray(ints)));
    assertTrue(Arrays.equals(array, Bytes.toArray(floats)));
    assertTrue(Arrays.equals(array, Bytes.toArray(longs)));
    assertTrue(Arrays.equals(array, Bytes.toArray(doubles)));
  }

  public void testAsList_isAView() {
    byte[] array = {(byte) 0, (byte) 1};
    List<Byte> list = Bytes.asList(array);
    list.set(0, (byte) 2);
    assertTrue(Arrays.equals(new byte[] {(byte) 2, (byte) 1}, array));
    array[1] = (byte) 3;
    assertEquals(Arrays.asList((byte) 2, (byte) 3), list);
  }

  public void testAsList_toArray_roundTrip() {
    byte[] array = {(byte) 0, (byte) 1, (byte) 2};
    List<Byte> list = Bytes.asList(array);
    byte[] newArray = Bytes.toArray(list);

    // Make sure it returned a copy
    list.set(0, (byte) 4);
    assertTrue(Arrays.equals(new byte[] {(byte) 0, (byte) 1, (byte) 2}, newArray));
    newArray[1] = (byte) 5;
    assertEquals((byte) 1, (byte) list.get(1));
  }

  // This test stems from a real bug found by andrewk
  public void testAsList_subList_toArray_roundTrip() {
    byte[] array = {(byte) 0, (byte) 1, (byte) 2, (byte) 3};
    List<Byte> list = Bytes.asList(array);
    assertTrue(Arrays.equals(new byte[] {(byte) 1, (byte) 2}, Bytes.toArray(list.subList(1, 3))));
    assertTrue(Arrays.equals(new byte[] {}, Bytes.toArray(list.subList(2, 2))));
  }

  public void testAsListEmpty() {
    assertSame(Collections.emptyList(), Bytes.asList(EMPTY));
  }

  public void testReverse() {
    testReverse(new byte[] {}, new byte[] {});
    testReverse(new byte[] {1}, new byte[] {1});
    testReverse(new byte[] {1, 2}, new byte[] {2, 1});
    testReverse(new byte[] {3, 1, 1}, new byte[] {1, 1, 3});
    testReverse(new byte[] {-1, 1, -2, 2}, new byte[] {2, -2, 1, -1});
  }

  private static void testReverse(byte[] input, byte[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Bytes.reverse(input);
    assertTrue(Arrays.equals(expectedOutput, input));
  }

  private static void testReverse(byte[] input, int fromIndex, int toIndex, byte[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Bytes.reverse(input, fromIndex, toIndex);
    assertTrue(Arrays.equals(expectedOutput, input));
  }

  public void testReverseIndexed() {
    testReverse(new byte[] {}, 0, 0, new byte[] {});
    testReverse(new byte[] {1}, 0, 1, new byte[] {1});
    testReverse(new byte[] {1, 2}, 0, 2, new byte[] {2, 1});
    testReverse(new byte[] {3, 1, 1}, 0, 2, new byte[] {1, 3, 1});
    testReverse(new byte[] {3, 1, 1}, 0, 1, new byte[] {3, 1, 1});
    testReverse(new byte[] {-1, 1, -2, 2}, 1, 3, new byte[] {-1, -2, 1, 2});
  }

  @GwtIncompatible // NullPointerTester
  public void testNulls() {
    new NullPointerTester().testAllPublicStaticMethods(Bytes.class);
  }
}

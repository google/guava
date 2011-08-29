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

import static java.lang.Double.NaN;
import static org.junit.contrib.truth.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.Helpers;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Unit test for {@link Doubles}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
@SuppressWarnings("cast") // redundant casts are intentional and harmless
public class DoublesTest extends TestCase {
  private static final double[] EMPTY = {};
  private static final double[] ARRAY1 = {(double) 1};
  private static final double[] ARRAY234
      = {(double) 2, (double) 3, (double) 4};

  private static final double LEAST = Double.NEGATIVE_INFINITY;
  private static final double GREATEST = Double.POSITIVE_INFINITY;

  private static final double[] NUMBERS = new double[] {
      LEAST, -Double.MAX_VALUE, -1.0, -0.0, 0.0, 1.0, Double.MAX_VALUE, GREATEST,
      Double.MIN_NORMAL, -Double.MIN_NORMAL,  Double.MIN_VALUE, -Double.MIN_VALUE,
      Integer.MIN_VALUE, Integer.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE
  };

  private static final double[] VALUES
      = Doubles.concat(NUMBERS, new double[] {NaN});

  public void testHashCode() {
    for (double value : VALUES) {
      assertEquals(((Double) value).hashCode(), Doubles.hashCode(value));
    }
  }

  public void testIsFinite() {
    for (double value : NUMBERS) {
      assertEquals(!(Double.isNaN(value) || Double.isInfinite(value)), Doubles.isFinite(value));
    }
  }

  public void testCompare() {
    for (double x : VALUES) {
      for (double y : VALUES) {
        // note: spec requires only that the sign is the same
        assertEquals(x + ", " + y,
                     Double.valueOf(x).compareTo(y),
                     Doubles.compare(x, y));
      }
    }
  }

  public void testContains() {
    assertFalse(Doubles.contains(EMPTY, (double) 1));
    assertFalse(Doubles.contains(ARRAY1, (double) 2));
    assertFalse(Doubles.contains(ARRAY234, (double) 1));
    assertTrue(Doubles.contains(new double[] {(double) -1}, (double) -1));
    assertTrue(Doubles.contains(ARRAY234, (double) 2));
    assertTrue(Doubles.contains(ARRAY234, (double) 3));
    assertTrue(Doubles.contains(ARRAY234, (double) 4));

    for (double value : NUMBERS) {
      assertTrue("" + value,
          Doubles.contains(new double[] {5.0, value}, value));
    }
    assertFalse(Doubles.contains(new double[] {5.0, NaN}, NaN));
  }

  public void testIndexOf() {
    assertEquals(-1, Doubles.indexOf(EMPTY, (double) 1));
    assertEquals(-1, Doubles.indexOf(ARRAY1, (double) 2));
    assertEquals(-1, Doubles.indexOf(ARRAY234, (double) 1));
    assertEquals(0, Doubles.indexOf(
        new double[] {(double) -1}, (double) -1));
    assertEquals(0, Doubles.indexOf(ARRAY234, (double) 2));
    assertEquals(1, Doubles.indexOf(ARRAY234, (double) 3));
    assertEquals(2, Doubles.indexOf(ARRAY234, (double) 4));
    assertEquals(1, Doubles.indexOf(
        new double[] { (double) 2, (double) 3, (double) 2, (double) 3 },
        (double) 3));

    for (double value : NUMBERS) {
      assertEquals("" + value,
          1, Doubles.indexOf(new double[] {5.0, value}, value));
    }
    assertEquals(-1, Doubles.indexOf(new double[] {5.0, NaN}, NaN));
  }

  public void testIndexOf_arrayTarget() {
    assertEquals(0, Doubles.indexOf(EMPTY, EMPTY));
    assertEquals(0, Doubles.indexOf(ARRAY234, EMPTY));
    assertEquals(-1, Doubles.indexOf(EMPTY, ARRAY234));
    assertEquals(-1, Doubles.indexOf(ARRAY234, ARRAY1));
    assertEquals(-1, Doubles.indexOf(ARRAY1, ARRAY234));
    assertEquals(0, Doubles.indexOf(ARRAY1, ARRAY1));
    assertEquals(0, Doubles.indexOf(ARRAY234, ARRAY234));
    assertEquals(0, Doubles.indexOf(
        ARRAY234, new double[] { (double) 2, (double) 3 }));
    assertEquals(1, Doubles.indexOf(
        ARRAY234, new double[] { (double) 3, (double) 4 }));
    assertEquals(1, Doubles.indexOf(ARRAY234, new double[] { (double) 3 }));
    assertEquals(2, Doubles.indexOf(ARRAY234, new double[] { (double) 4 }));
    assertEquals(1, Doubles.indexOf(new double[] { (double) 2, (double) 3,
        (double) 3, (double) 3, (double) 3 },
        new double[] { (double) 3 }
    ));
    assertEquals(2, Doubles.indexOf(
        new double[] { (double) 2, (double) 3, (double) 2,
            (double) 3, (double) 4, (double) 2, (double) 3},
        new double[] { (double) 2, (double) 3, (double) 4}
    ));
    assertEquals(1, Doubles.indexOf(
        new double[] { (double) 2, (double) 2, (double) 3,
            (double) 4, (double) 2, (double) 3, (double) 4},
        new double[] { (double) 2, (double) 3, (double) 4}
    ));
    assertEquals(-1, Doubles.indexOf(
        new double[] { (double) 4, (double) 3, (double) 2},
        new double[] { (double) 2, (double) 3, (double) 4}
    ));

    for (double value : NUMBERS) {
      assertEquals("" + value, 1, Doubles.indexOf(
          new double[] {5.0, value, value, 5.0}, new double[] {value, value}));
    }
    assertEquals(-1, Doubles.indexOf(
        new double[] {5.0, NaN, NaN, 5.0}, new double[] {NaN, NaN}));
  }

  public void testLastIndexOf() {
    assertEquals(-1, Doubles.lastIndexOf(EMPTY, (double) 1));
    assertEquals(-1, Doubles.lastIndexOf(ARRAY1, (double) 2));
    assertEquals(-1, Doubles.lastIndexOf(ARRAY234, (double) 1));
    assertEquals(0, Doubles.lastIndexOf(
        new double[] {(double) -1}, (double) -1));
    assertEquals(0, Doubles.lastIndexOf(ARRAY234, (double) 2));
    assertEquals(1, Doubles.lastIndexOf(ARRAY234, (double) 3));
    assertEquals(2, Doubles.lastIndexOf(ARRAY234, (double) 4));
    assertEquals(3, Doubles.lastIndexOf(
        new double[] { (double) 2, (double) 3, (double) 2, (double) 3 },
        (double) 3));

    for (double value : NUMBERS) {
      assertEquals("" + value,
          0, Doubles.lastIndexOf(new double[] {value, 5.0}, value));
    }
    assertEquals(-1, Doubles.lastIndexOf(new double[] {NaN, 5.0}, NaN));
  }

  public void testMax_noArgs() {
    try {
      Doubles.max();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMax() {
    assertEquals(LEAST, Doubles.max(LEAST));
    assertEquals(GREATEST, Doubles.max(GREATEST));
    assertEquals((double) 9, Doubles.max(
        (double) 8, (double) 6, (double) 7,
        (double) 5, (double) 3, (double) 0, (double) 9));

    assertEquals(0.0, Doubles.max(-0.0, 0.0));
    assertEquals(0.0, Doubles.max(0.0, -0.0));
    assertEquals(GREATEST, Doubles.max(NUMBERS));
    assertTrue(Double.isNaN(Doubles.max(VALUES)));
  }

  public void testMin_noArgs() {
    try {
      Doubles.min();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMin() {
    assertEquals(LEAST, Doubles.min(LEAST));
    assertEquals(GREATEST, Doubles.min(GREATEST));
    assertEquals((double) 0, Doubles.min(
        (double) 8, (double) 6, (double) 7,
        (double) 5, (double) 3, (double) 0, (double) 9));

    assertEquals(-0.0, Doubles.min(-0.0, 0.0));
    assertEquals(-0.0, Doubles.min(0.0, -0.0));
    assertEquals(LEAST, Doubles.min(NUMBERS));
    assertTrue(Double.isNaN(Doubles.min(VALUES)));
  }

  public void testConcat() {
    assertTrue(Arrays.equals(EMPTY, Doubles.concat()));
    assertTrue(Arrays.equals(EMPTY, Doubles.concat(EMPTY)));
    assertTrue(Arrays.equals(EMPTY, Doubles.concat(EMPTY, EMPTY, EMPTY)));
    assertTrue(Arrays.equals(ARRAY1, Doubles.concat(ARRAY1)));
    assertNotSame(ARRAY1, Doubles.concat(ARRAY1));
    assertTrue(Arrays.equals(ARRAY1, Doubles.concat(EMPTY, ARRAY1, EMPTY)));
    assertTrue(Arrays.equals(
        new double[] {(double) 1, (double) 1, (double) 1},
        Doubles.concat(ARRAY1, ARRAY1, ARRAY1)));
    assertTrue(Arrays.equals(
        new double[] {(double) 1, (double) 2, (double) 3, (double) 4},
        Doubles.concat(ARRAY1, ARRAY234)));
  }

  public void testEnsureCapacity() {
    assertSame(EMPTY, Doubles.ensureCapacity(EMPTY, 0, 1));
    assertSame(ARRAY1, Doubles.ensureCapacity(ARRAY1, 0, 1));
    assertSame(ARRAY1, Doubles.ensureCapacity(ARRAY1, 1, 1));
    assertTrue(Arrays.equals(
        new double[] {(double) 1, (double) 0, (double) 0},
        Doubles.ensureCapacity(ARRAY1, 2, 1)));
  }

  public void testEnsureCapacity_fail() {
    try {
      Doubles.ensureCapacity(ARRAY1, -1, 1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      // notice that this should even fail when no growth was needed
      Doubles.ensureCapacity(ARRAY1, 1, -1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @GwtIncompatible("Double.toString returns different value in GWT.")
  public void testJoin() {
    assertEquals("", Doubles.join(",", EMPTY));
    assertEquals("1.0", Doubles.join(",", ARRAY1));
    assertEquals("1.0,2.0", Doubles.join(",", (double) 1, (double) 2));
    assertEquals("1.02.03.0",
        Doubles.join("", (double) 1, (double) 2, (double) 3));
  }

  public void testJoinNonTrivialDoubles() {
    assertEquals("", Doubles.join(",", EMPTY));
    assertEquals("1.2", Doubles.join(",", 1.2));
    assertEquals("1.3,2.4", Doubles.join(",", 1.3, 2.4));
    assertEquals("1.42.53.6", Doubles.join("", 1.4, 2.5, 3.6));
  }

  public void testLexicographicalComparator() {
    List<double[]> ordered = Arrays.asList(
        new double[] {},
        new double[] {LEAST},
        new double[] {LEAST, LEAST},
        new double[] {LEAST, (double) 1},
        new double[] {(double) 1},
        new double[] {(double) 1, LEAST},
        new double[] {GREATEST, Double.MAX_VALUE},
        new double[] {GREATEST, GREATEST},
        new double[] {GREATEST, GREATEST, GREATEST});

    Comparator<double[]> comparator = Doubles.lexicographicalComparator();
    Helpers.testComparator(comparator, ordered);
  }

  @GwtIncompatible("SerializableTester")
  public void testLexicographicalComparatorSerializable() {
    Comparator<double[]> comparator = Doubles.lexicographicalComparator();
    assertSame(comparator, SerializableTester.reserialize(comparator));
  }

  public void testToArray() {
    // need explicit type parameter to avoid javac warning!?
    List<Double> none = Arrays.<Double>asList();
    assertTrue(Arrays.equals(EMPTY, Doubles.toArray(none)));

    List<Double> one = Arrays.asList((double) 1);
    assertTrue(Arrays.equals(ARRAY1, Doubles.toArray(one)));

    double[] array = {(double) 0, (double) 1, Math.PI};

    List<Double> three = Arrays.asList((double) 0, (double) 1, Math.PI);
    assertTrue(Arrays.equals(array, Doubles.toArray(three)));

    assertTrue(Arrays.equals(array, Doubles.toArray(Doubles.asList(array))));
  }

  public void testToArray_threadSafe() {
    for (int delta : new int[] { +1, 0, -1 }) {
      for (int i = 0; i < VALUES.length; i++) {
        List<Double> list = Doubles.asList(VALUES).subList(0, i);
        Collection<Double> misleadingSize =
            Helpers.misleadingSizeCollection(delta);
        misleadingSize.addAll(list);
        double[] arr = Doubles.toArray(misleadingSize);
        assertEquals(i, arr.length);
        for (int j = 0; j < i; j++) {
          assertEquals(VALUES[j], arr[j]);
        }
      }
    }
  }

  public void testToArray_withNull() {
    List<Double> list = Arrays.asList((double) 0, (double) 1, null);
    try {
      Doubles.toArray(list);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testAsList_isAView() {
    double[] array = {(double) 0, (double) 1};
    List<Double> list = Doubles.asList(array);
    list.set(0, (double) 2);
    assertTrue(Arrays.equals(new double[] {(double) 2, (double) 1}, array));
    array[1] = (double) 3;
    ASSERT.that(list).hasContentsInOrder((double) 2, (double) 3);
  }

  public void testAsList_toArray_roundTrip() {
    double[] array = { (double) 0, (double) 1, (double) 2 };
    List<Double> list = Doubles.asList(array);
    double[] newArray = Doubles.toArray(list);

    // Make sure it returned a copy
    list.set(0, (double) 4);
    assertTrue(Arrays.equals(
        new double[] { (double) 0, (double) 1, (double) 2 }, newArray));
    newArray[1] = (double) 5;
    assertEquals((double) 1, (double) list.get(1));
  }

  // This test stems from a real bug found by andrewk
  public void testAsList_subList_toArray_roundTrip() {
    double[] array = { (double) 0, (double) 1, (double) 2, (double) 3 };
    List<Double> list = Doubles.asList(array);
    assertTrue(Arrays.equals(new double[] { (double) 1, (double) 2 },
        Doubles.toArray(list.subList(1, 3))));
    assertTrue(Arrays.equals(new double[] {},
        Doubles.toArray(list.subList(2, 2))));
  }

  public void testAsListEmpty() {
    assertSame(Collections.emptyList(), Doubles.asList(EMPTY));
  }

  @GwtIncompatible("NullPointerTester")
  public void testNulls() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.setDefault(double[].class, new double[0]);
    tester.testAllPublicStaticMethods(Doubles.class);
  }
}

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
import junit.framework.TestCase;

/**
 * Unit test for {@link Booleans}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
public class BooleansTest extends TestCase {
  private static final boolean[] EMPTY = {};
  private static final boolean[] ARRAY_FALSE = {false};
  private static final boolean[] ARRAY_TRUE = {true};
  private static final boolean[] ARRAY_FALSE_FALSE = {false, false};
  private static final boolean[] ARRAY_FALSE_TRUE = {false, true};

  private static final boolean[] VALUES = {false, true};

  public void testHashCode() {
    assertEquals(Boolean.TRUE.hashCode(), Booleans.hashCode(true));
    assertEquals(Boolean.FALSE.hashCode(), Booleans.hashCode(false));
  }

  public void testTrueFirst() {
    assertEquals(0, Booleans.trueFirst().compare(true, true));
    assertEquals(0, Booleans.trueFirst().compare(false, false));
    assertTrue(Booleans.trueFirst().compare(true, false) < 0);
    assertTrue(Booleans.trueFirst().compare(false, true) > 0);
  }

  public void testFalseFirst() {
    assertEquals(0, Booleans.falseFirst().compare(true, true));
    assertEquals(0, Booleans.falseFirst().compare(false, false));
    assertTrue(Booleans.falseFirst().compare(false, true) < 0);
    assertTrue(Booleans.falseFirst().compare(true, false) > 0);
  }

  public void testCompare() {
    for (boolean x : VALUES) {
      for (boolean y : VALUES) {
        // note: spec requires only that the sign is the same
        assertEquals(x + ", " + y, Boolean.valueOf(x).compareTo(y), Booleans.compare(x, y));
      }
    }
  }

  public void testContains() {
    assertFalse(Booleans.contains(EMPTY, false));
    assertFalse(Booleans.contains(ARRAY_FALSE, true));
    assertTrue(Booleans.contains(ARRAY_FALSE, false));
    assertTrue(Booleans.contains(ARRAY_FALSE_TRUE, false));
    assertTrue(Booleans.contains(ARRAY_FALSE_TRUE, true));
  }

  public void testIndexOf() {
    assertEquals(-1, Booleans.indexOf(EMPTY, ARRAY_FALSE));
    assertEquals(-1, Booleans.indexOf(ARRAY_FALSE, ARRAY_FALSE_TRUE));
    assertEquals(0, Booleans.indexOf(ARRAY_FALSE_FALSE, ARRAY_FALSE));
    assertEquals(0, Booleans.indexOf(ARRAY_FALSE, ARRAY_FALSE));
    assertEquals(0, Booleans.indexOf(ARRAY_FALSE_TRUE, ARRAY_FALSE));
    assertEquals(1, Booleans.indexOf(ARRAY_FALSE_TRUE, ARRAY_TRUE));
    assertEquals(0, Booleans.indexOf(ARRAY_TRUE, new boolean[0]));
  }

  public void testIndexOf_arrays() {
    assertEquals(-1, Booleans.indexOf(EMPTY, false));
    assertEquals(-1, Booleans.indexOf(ARRAY_FALSE, true));
    assertEquals(-1, Booleans.indexOf(ARRAY_FALSE_FALSE, true));
    assertEquals(0, Booleans.indexOf(ARRAY_FALSE, false));
    assertEquals(0, Booleans.indexOf(ARRAY_FALSE_TRUE, false));
    assertEquals(1, Booleans.indexOf(ARRAY_FALSE_TRUE, true));
    assertEquals(2, Booleans.indexOf(new boolean[] {false, false, true}, true));
  }

  public void testLastIndexOf() {
    assertEquals(-1, Booleans.lastIndexOf(EMPTY, false));
    assertEquals(-1, Booleans.lastIndexOf(ARRAY_FALSE, true));
    assertEquals(-1, Booleans.lastIndexOf(ARRAY_FALSE_FALSE, true));
    assertEquals(0, Booleans.lastIndexOf(ARRAY_FALSE, false));
    assertEquals(0, Booleans.lastIndexOf(ARRAY_FALSE_TRUE, false));
    assertEquals(1, Booleans.lastIndexOf(ARRAY_FALSE_TRUE, true));
    assertEquals(2, Booleans.lastIndexOf(new boolean[] {false, true, true}, true));
  }

  public void testConcat() {
    assertTrue(Arrays.equals(EMPTY, Booleans.concat()));
    assertTrue(Arrays.equals(EMPTY, Booleans.concat(EMPTY)));
    assertTrue(Arrays.equals(EMPTY, Booleans.concat(EMPTY, EMPTY, EMPTY)));
    assertTrue(Arrays.equals(ARRAY_FALSE, Booleans.concat(ARRAY_FALSE)));
    assertNotSame(ARRAY_FALSE, Booleans.concat(ARRAY_FALSE));
    assertTrue(Arrays.equals(ARRAY_FALSE, Booleans.concat(EMPTY, ARRAY_FALSE, EMPTY)));
    assertTrue(
        Arrays.equals(
            new boolean[] {false, false, false},
            Booleans.concat(ARRAY_FALSE, ARRAY_FALSE, ARRAY_FALSE)));
    assertTrue(
        Arrays.equals(
            new boolean[] {false, false, true}, Booleans.concat(ARRAY_FALSE, ARRAY_FALSE_TRUE)));
  }

  public void testEnsureCapacity() {
    assertSame(EMPTY, Booleans.ensureCapacity(EMPTY, 0, 1));
    assertSame(ARRAY_FALSE, Booleans.ensureCapacity(ARRAY_FALSE, 0, 1));
    assertSame(ARRAY_FALSE, Booleans.ensureCapacity(ARRAY_FALSE, 1, 1));
    assertTrue(
        Arrays.equals(
            new boolean[] {true, false, false},
            Booleans.ensureCapacity(new boolean[] {true}, 2, 1)));
  }

  public void testEnsureCapacity_fail() {
    try {
      Booleans.ensureCapacity(ARRAY_FALSE, -1, 1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      // notice that this should even fail when no growth was needed
      Booleans.ensureCapacity(ARRAY_FALSE, 1, -1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testJoin() {
    assertEquals("", Booleans.join(",", EMPTY));
    assertEquals("false", Booleans.join(",", ARRAY_FALSE));
    assertEquals("false,true", Booleans.join(",", false, true));
    assertEquals("falsetruefalse", Booleans.join("", false, true, false));
  }

  public void testLexicographicalComparator() {
    List<boolean[]> ordered =
        Arrays.asList(
            new boolean[] {},
            new boolean[] {false},
            new boolean[] {false, false},
            new boolean[] {false, true},
            new boolean[] {true},
            new boolean[] {true, false},
            new boolean[] {true, true},
            new boolean[] {true, true, true});

    Comparator<boolean[]> comparator = Booleans.lexicographicalComparator();
    Helpers.testComparator(comparator, ordered);
  }

  @GwtIncompatible // SerializableTester
  public void testLexicographicalComparatorSerializable() {
    Comparator<boolean[]> comparator = Booleans.lexicographicalComparator();
    assertSame(comparator, SerializableTester.reserialize(comparator));
  }

  public void testReverse() {
    testReverse(new boolean[] {}, new boolean[] {});
    testReverse(new boolean[] {true}, new boolean[] {true});
    testReverse(new boolean[] {false, true}, new boolean[] {true, false});
    testReverse(new boolean[] {true, false, false}, new boolean[] {false, false, true});
    testReverse(new boolean[] {true, true, false, false}, new boolean[] {false, false, true, true});
  }

  private static void testReverse(boolean[] input, boolean[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Booleans.reverse(input);
    assertTrue(Arrays.equals(expectedOutput, input));
  }

  private static void testReverse(
      boolean[] input, int fromIndex, int toIndex, boolean[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Booleans.reverse(input, fromIndex, toIndex);
    assertTrue(Arrays.equals(expectedOutput, input));
  }

  public void testReverseIndexed() {
    testReverse(new boolean[] {}, 0, 0, new boolean[] {});
    testReverse(new boolean[] {true}, 0, 1, new boolean[] {true});
    testReverse(new boolean[] {false, true}, 0, 2, new boolean[] {true, false});
    testReverse(new boolean[] {true, false, false}, 0, 2, new boolean[] {false, true, false});
    testReverse(new boolean[] {true, false, false}, 0, 1, new boolean[] {true, false, false});
    testReverse(
        new boolean[] {true, true, false, false}, 1, 3, new boolean[] {true, false, true, false});
  }

  public void testToArray() {
    // need explicit type parameter to avoid javac warning!?
    List<Boolean> none = Arrays.<Boolean>asList();
    assertTrue(Arrays.equals(EMPTY, Booleans.toArray(none)));

    List<Boolean> one = Arrays.asList(false);
    assertTrue(Arrays.equals(ARRAY_FALSE, Booleans.toArray(one)));

    boolean[] array = {false, false, true};

    List<Boolean> three = Arrays.asList(false, false, true);
    assertTrue(Arrays.equals(array, Booleans.toArray(three)));

    assertTrue(Arrays.equals(array, Booleans.toArray(Booleans.asList(array))));
  }

  public void testToArray_threadSafe() {
    // Only for booleans, we lengthen VALUES
    boolean[] VALUES = BooleansTest.VALUES;
    VALUES = Booleans.concat(VALUES, VALUES);

    for (int delta : new int[] {+1, 0, -1}) {
      for (int i = 0; i < VALUES.length; i++) {
        List<Boolean> list = Booleans.asList(VALUES).subList(0, i);
        Collection<Boolean> misleadingSize = Helpers.misleadingSizeCollection(delta);
        misleadingSize.addAll(list);
        boolean[] arr = Booleans.toArray(misleadingSize);
        assertEquals(i, arr.length);
        for (int j = 0; j < i; j++) {
          assertEquals(VALUES[j], arr[j]);
        }
      }
    }
  }

  public void testToArray_withNull() {
    List<Boolean> list = Arrays.asList(false, true, null);
    try {
      Booleans.toArray(list);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testAsListIsEmpty() {
    assertTrue(Booleans.asList(EMPTY).isEmpty());
    assertFalse(Booleans.asList(ARRAY_FALSE).isEmpty());
  }

  public void testAsListSize() {
    assertEquals(0, Booleans.asList(EMPTY).size());
    assertEquals(1, Booleans.asList(ARRAY_FALSE).size());
    assertEquals(2, Booleans.asList(ARRAY_FALSE_TRUE).size());
  }

  public void testAsListIndexOf() {
    assertEquals(-1, Booleans.asList(EMPTY).indexOf((Object) "wrong type"));
    assertEquals(-1, Booleans.asList(EMPTY).indexOf(true));
    assertEquals(-1, Booleans.asList(ARRAY_FALSE).indexOf(true));
    assertEquals(0, Booleans.asList(ARRAY_FALSE).indexOf(false));
    assertEquals(1, Booleans.asList(ARRAY_FALSE_TRUE).indexOf(true));
  }

  public void testAsListLastIndexOf() {
    assertEquals(-1, Booleans.asList(EMPTY).lastIndexOf((Object) "wrong type"));
    assertEquals(-1, Booleans.asList(EMPTY).lastIndexOf(true));
    assertEquals(-1, Booleans.asList(ARRAY_FALSE).lastIndexOf(true));
    assertEquals(1, Booleans.asList(ARRAY_FALSE_TRUE).lastIndexOf(true));
    assertEquals(1, Booleans.asList(ARRAY_FALSE_FALSE).lastIndexOf(false));
  }

  public void testAsListContains() {
    assertFalse(Booleans.asList(EMPTY).contains((Object) "wrong type"));
    assertFalse(Booleans.asList(EMPTY).contains(true));
    assertFalse(Booleans.asList(ARRAY_FALSE).contains(true));
    assertTrue(Booleans.asList(ARRAY_TRUE).contains(true));
    assertTrue(Booleans.asList(ARRAY_FALSE_TRUE).contains(false));
    assertTrue(Booleans.asList(ARRAY_FALSE_TRUE).contains(true));
  }

  public void testAsListEquals() {
    assertEquals(Booleans.asList(EMPTY), Collections.emptyList());
    assertEquals(Booleans.asList(ARRAY_FALSE), Booleans.asList(ARRAY_FALSE));
    assertFalse(Booleans.asList(ARRAY_FALSE).equals(ARRAY_FALSE));
    assertFalse(Booleans.asList(ARRAY_FALSE).equals(null));
    assertFalse(Booleans.asList(ARRAY_FALSE).equals(Booleans.asList(ARRAY_FALSE_TRUE)));
    assertFalse(Booleans.asList(ARRAY_FALSE_FALSE).equals(Booleans.asList(ARRAY_FALSE_TRUE)));
    assertEquals(1, Booleans.asList(ARRAY_FALSE_TRUE).lastIndexOf(true));
    List<Boolean> reference = Booleans.asList(ARRAY_FALSE);
    assertEquals(Booleans.asList(ARRAY_FALSE), reference);
    assertEquals(reference, reference);
  }

  public void testAsListHashcode() {
    assertEquals(1, Booleans.asList(EMPTY).hashCode());
    assertEquals(Booleans.asList(ARRAY_FALSE).hashCode(), Booleans.asList(ARRAY_FALSE).hashCode());
    List<Boolean> reference = Booleans.asList(ARRAY_FALSE);
    assertEquals(Booleans.asList(ARRAY_FALSE).hashCode(), reference.hashCode());
  }

  public void testAsListToString() {
    assertEquals("[false]", Booleans.asList(ARRAY_FALSE).toString());
    assertEquals("[false, true]", Booleans.asList(ARRAY_FALSE_TRUE).toString());
  }

  public void testAsListSet() {
    List<Boolean> list = Booleans.asList(ARRAY_FALSE);
    assertFalse(list.set(0, true));
    assertTrue(list.set(0, false));
    try {
      list.set(0, null);
      fail();
    } catch (NullPointerException expected) {
    }
    try {
      list.set(1, true);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  public void testCountTrue() {
    assertEquals(0, Booleans.countTrue());
    assertEquals(0, Booleans.countTrue(false));
    assertEquals(1, Booleans.countTrue(true));
    assertEquals(3, Booleans.countTrue(false, true, false, true, false, true));
    assertEquals(1, Booleans.countTrue(false, false, true, false, false));
  }

  @GwtIncompatible // NullPointerTester
  public void testNulls() {
    new NullPointerTester().testAllPublicStaticMethods(Booleans.class);
  }
}

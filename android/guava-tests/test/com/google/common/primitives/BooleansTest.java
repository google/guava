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
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Unit test for {@link Booleans}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
public class BooleansTest extends TestCase {
  private static final boolean[] EMPTY = {};
  private static final boolean[] ARRAY_FALSE = {false};
  private static final boolean[] ARRAY_TRUE = {true};
  private static final boolean[] ARRAY_FALSE_FALSE = {false, false};
  private static final boolean[] ARRAY_FALSE_TRUE = {false, true};

  private static final boolean[] VALUES = {false, true};

  public void testHashCode() {
    assertThat(Booleans.hashCode(true)).isEqualTo(Boolean.TRUE.hashCode());
    assertThat(Booleans.hashCode(false)).isEqualTo(Boolean.FALSE.hashCode());
  }

  public void testTrueFirst() {
    assertThat(Booleans.trueFirst().compare(true, true)).isEqualTo(0);
    assertThat(Booleans.trueFirst().compare(false, false)).isEqualTo(0);
    assertThat(Booleans.trueFirst().compare(true, false)).isLessThan(0);
    assertThat(Booleans.trueFirst().compare(false, true)).isGreaterThan(0);
  }

  public void testFalseFirst() {
    assertThat(Booleans.falseFirst().compare(true, true)).isEqualTo(0);
    assertThat(Booleans.falseFirst().compare(false, false)).isEqualTo(0);
    assertThat(Booleans.falseFirst().compare(false, true)).isLessThan(0);
    assertThat(Booleans.falseFirst().compare(true, false)).isGreaterThan(0);
  }

  public void testCompare() {
    for (boolean x : VALUES) {
      for (boolean y : VALUES) {
        // note: spec requires only that the sign is the same
        assertWithMessage(x + ", " + y)
            .that(Booleans.compare(x, y))
            .isEqualTo(Boolean.valueOf(x).compareTo(y));
      }
    }
  }

  public void testContains() {
    assertThat(Booleans.contains(EMPTY, false)).isFalse();
    assertThat(Booleans.contains(ARRAY_FALSE, true)).isFalse();
    assertThat(Booleans.contains(ARRAY_FALSE, false)).isTrue();
    assertThat(Booleans.contains(ARRAY_FALSE_TRUE, false)).isTrue();
    assertThat(Booleans.contains(ARRAY_FALSE_TRUE, true)).isTrue();
  }

  public void testIndexOf() {
    assertThat(Booleans.indexOf(EMPTY, ARRAY_FALSE)).isEqualTo(-1);
    assertThat(Booleans.indexOf(ARRAY_FALSE, ARRAY_FALSE_TRUE)).isEqualTo(-1);
    assertThat(Booleans.indexOf(ARRAY_FALSE_FALSE, ARRAY_FALSE)).isEqualTo(0);
    assertThat(Booleans.indexOf(ARRAY_FALSE, ARRAY_FALSE)).isEqualTo(0);
    assertThat(Booleans.indexOf(ARRAY_FALSE_TRUE, ARRAY_FALSE)).isEqualTo(0);
    assertThat(Booleans.indexOf(ARRAY_FALSE_TRUE, ARRAY_TRUE)).isEqualTo(1);
    assertThat(Booleans.indexOf(ARRAY_TRUE, new boolean[0])).isEqualTo(0);
  }

  public void testIndexOf_arrays() {
    assertThat(Booleans.indexOf(EMPTY, false)).isEqualTo(-1);
    assertThat(Booleans.indexOf(ARRAY_FALSE, true)).isEqualTo(-1);
    assertThat(Booleans.indexOf(ARRAY_FALSE_FALSE, true)).isEqualTo(-1);
    assertThat(Booleans.indexOf(ARRAY_FALSE, false)).isEqualTo(0);
    assertThat(Booleans.indexOf(ARRAY_FALSE_TRUE, false)).isEqualTo(0);
    assertThat(Booleans.indexOf(ARRAY_FALSE_TRUE, true)).isEqualTo(1);
    assertThat(Booleans.indexOf(new boolean[] {false, false, true}, true)).isEqualTo(2);
  }

  public void testLastIndexOf() {
    assertThat(Booleans.lastIndexOf(EMPTY, false)).isEqualTo(-1);
    assertThat(Booleans.lastIndexOf(ARRAY_FALSE, true)).isEqualTo(-1);
    assertThat(Booleans.lastIndexOf(ARRAY_FALSE_FALSE, true)).isEqualTo(-1);
    assertThat(Booleans.lastIndexOf(ARRAY_FALSE, false)).isEqualTo(0);
    assertThat(Booleans.lastIndexOf(ARRAY_FALSE_TRUE, false)).isEqualTo(0);
    assertThat(Booleans.lastIndexOf(ARRAY_FALSE_TRUE, true)).isEqualTo(1);
    assertThat(Booleans.lastIndexOf(new boolean[] {false, true, true}, true)).isEqualTo(2);
  }

  public void testConcat() {
    assertThat(Booleans.concat()).isEqualTo(EMPTY);
    assertThat(Booleans.concat(EMPTY)).isEqualTo(EMPTY);
    assertThat(Booleans.concat(EMPTY, EMPTY, EMPTY)).isEqualTo(EMPTY);
    assertThat(Booleans.concat(ARRAY_FALSE)).isEqualTo(ARRAY_FALSE);
    assertThat(Booleans.concat(ARRAY_FALSE)).isNotSameInstanceAs(ARRAY_FALSE);
    assertThat(Booleans.concat(EMPTY, ARRAY_FALSE, EMPTY)).isEqualTo(ARRAY_FALSE);
    assertThat(Booleans.concat(ARRAY_FALSE, ARRAY_FALSE, ARRAY_FALSE))
        .isEqualTo(new boolean[] {false, false, false});
    assertThat(Booleans.concat(ARRAY_FALSE, ARRAY_FALSE_TRUE))
        .isEqualTo(new boolean[] {false, false, true});
  }

  public void testEnsureCapacity() {
    assertThat(Booleans.ensureCapacity(EMPTY, 0, 1)).isSameInstanceAs(EMPTY);
    assertThat(Booleans.ensureCapacity(ARRAY_FALSE, 0, 1)).isSameInstanceAs(ARRAY_FALSE);
    assertThat(Booleans.ensureCapacity(ARRAY_FALSE, 1, 1)).isSameInstanceAs(ARRAY_FALSE);
    assertThat(Booleans.ensureCapacity(new boolean[] {true}, 2, 1))
        .isEqualTo(new boolean[] {true, false, false});
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
    assertThat(Booleans.join(",", EMPTY)).isEmpty();
    assertThat(Booleans.join(",", ARRAY_FALSE)).isEqualTo("false");
    assertThat(Booleans.join(",", false, true)).isEqualTo("false,true");
    assertThat(Booleans.join("", false, true, false)).isEqualTo("falsetruefalse");
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

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testLexicographicalComparatorSerializable() {
    Comparator<boolean[]> comparator = Booleans.lexicographicalComparator();
    assertThat(SerializableTester.reserialize(comparator)).isSameInstanceAs(comparator);
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
    assertThat(input).isEqualTo(expectedOutput);
  }

  private static void testReverse(
      boolean[] input, int fromIndex, int toIndex, boolean[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Booleans.reverse(input, fromIndex, toIndex);
    assertThat(input).isEqualTo(expectedOutput);
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

  private static void testRotate(boolean[] input, int distance, boolean[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Booleans.rotate(input, distance);
    assertThat(input).isEqualTo(expectedOutput);
  }

  private static void testRotate(
      boolean[] input, int distance, int fromIndex, int toIndex, boolean[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Booleans.rotate(input, distance, fromIndex, toIndex);
    assertThat(input).isEqualTo(expectedOutput);
  }

  public void testRotate() {
    testRotate(new boolean[] {}, -1, new boolean[] {});
    testRotate(new boolean[] {}, 0, new boolean[] {});
    testRotate(new boolean[] {}, 1, new boolean[] {});

    testRotate(new boolean[] {true}, -2, new boolean[] {true});
    testRotate(new boolean[] {true}, -1, new boolean[] {true});
    testRotate(new boolean[] {true}, 0, new boolean[] {true});
    testRotate(new boolean[] {true}, 1, new boolean[] {true});
    testRotate(new boolean[] {true}, 2, new boolean[] {true});

    testRotate(new boolean[] {true, false}, -3, new boolean[] {false, true});
    testRotate(new boolean[] {true, false}, -1, new boolean[] {false, true});
    testRotate(new boolean[] {true, false}, -2, new boolean[] {true, false});
    testRotate(new boolean[] {true, false}, 0, new boolean[] {true, false});
    testRotate(new boolean[] {true, false}, 1, new boolean[] {false, true});
    testRotate(new boolean[] {true, false}, 2, new boolean[] {true, false});
    testRotate(new boolean[] {true, false}, 3, new boolean[] {false, true});

    testRotate(new boolean[] {true, false, true}, -5, new boolean[] {true, true, false});
    testRotate(new boolean[] {true, false, true}, -4, new boolean[] {false, true, true});
    testRotate(new boolean[] {true, false, true}, -3, new boolean[] {true, false, true});
    testRotate(new boolean[] {true, false, true}, -2, new boolean[] {true, true, false});
    testRotate(new boolean[] {true, false, true}, -1, new boolean[] {false, true, true});
    testRotate(new boolean[] {true, false, true}, 0, new boolean[] {true, false, true});
    testRotate(new boolean[] {true, false, true}, 1, new boolean[] {true, true, false});
    testRotate(new boolean[] {true, false, true}, 2, new boolean[] {false, true, true});
    testRotate(new boolean[] {true, false, true}, 3, new boolean[] {true, false, true});
    testRotate(new boolean[] {true, false, true}, 4, new boolean[] {true, true, false});
    testRotate(new boolean[] {true, false, true}, 5, new boolean[] {false, true, true});

    testRotate(
        new boolean[] {true, false, true, false}, -9, new boolean[] {false, true, false, true});
    testRotate(
        new boolean[] {true, false, true, false}, -5, new boolean[] {false, true, false, true});
    testRotate(
        new boolean[] {true, false, true, false}, -1, new boolean[] {false, true, false, true});
    testRotate(
        new boolean[] {true, false, true, false}, 0, new boolean[] {true, false, true, false});
    testRotate(
        new boolean[] {true, false, true, false}, 1, new boolean[] {false, true, false, true});
    testRotate(
        new boolean[] {true, false, true, false}, 5, new boolean[] {false, true, false, true});
    testRotate(
        new boolean[] {true, false, true, false}, 9, new boolean[] {false, true, false, true});

    testRotate(
        new boolean[] {true, false, true, false, true},
        -6,
        new boolean[] {false, true, false, true, true});
    testRotate(
        new boolean[] {true, false, true, false, true},
        -4,
        new boolean[] {true, true, false, true, false});
    testRotate(
        new boolean[] {true, false, true, false, true},
        -3,
        new boolean[] {false, true, true, false, true});
    testRotate(
        new boolean[] {true, false, true, false, true},
        -1,
        new boolean[] {false, true, false, true, true});
    testRotate(
        new boolean[] {true, false, true, false, true},
        0,
        new boolean[] {true, false, true, false, true});
    testRotate(
        new boolean[] {true, false, true, false, true},
        1,
        new boolean[] {true, true, false, true, false});
    testRotate(
        new boolean[] {true, false, true, false, true},
        3,
        new boolean[] {true, false, true, true, false});
    testRotate(
        new boolean[] {true, false, true, false, true},
        4,
        new boolean[] {false, true, false, true, true});
    testRotate(
        new boolean[] {true, false, true, false, true},
        6,
        new boolean[] {true, true, false, true, false});
  }

  public void testRotateIndexed() {
    testRotate(new boolean[] {}, 0, 0, 0, new boolean[] {});

    testRotate(new boolean[] {true}, 0, 0, 1, new boolean[] {true});
    testRotate(new boolean[] {true}, 1, 0, 1, new boolean[] {true});
    testRotate(new boolean[] {true}, 1, 1, 1, new boolean[] {true});

    // Rotate the central 5 elements, leaving the ends as-is
    testRotate(
        new boolean[] {false, true, false, true, false, true, false},
        -6,
        1,
        6,
        new boolean[] {false, false, true, false, true, true, false});
    testRotate(
        new boolean[] {false, true, false, true, false, true, false},
        -1,
        1,
        6,
        new boolean[] {false, false, true, false, true, true, false});
    testRotate(
        new boolean[] {false, true, false, true, false, true, false},
        0,
        1,
        6,
        new boolean[] {false, true, false, true, false, true, false});
    testRotate(
        new boolean[] {false, true, false, true, false, true, false},
        5,
        1,
        6,
        new boolean[] {false, true, false, true, false, true, false});
    testRotate(
        new boolean[] {false, true, false, true, false, true, false},
        14,
        1,
        6,
        new boolean[] {false, false, true, false, true, true, false});

    // Rotate the first three elements
    testRotate(
        new boolean[] {false, true, false, true, false, true, false},
        -2,
        0,
        3,
        new boolean[] {false, false, true, true, false, true, false});
    testRotate(
        new boolean[] {false, true, false, true, false, true, false},
        -1,
        0,
        3,
        new boolean[] {true, false, false, true, false, true, false});
    testRotate(
        new boolean[] {false, true, false, true, false, true, false},
        0,
        0,
        3,
        new boolean[] {false, true, false, true, false, true, false});
    testRotate(
        new boolean[] {false, true, false, true, false, true, false},
        1,
        0,
        3,
        new boolean[] {false, false, true, true, false, true, false});
    testRotate(
        new boolean[] {false, true, false, true, false, true, false},
        2,
        0,
        3,
        new boolean[] {true, false, false, true, false, true, false});

    // Rotate the last four elements
    testRotate(
        new boolean[] {false, true, false, true, false, true, false},
        -6,
        3,
        7,
        new boolean[] {false, true, false, true, false, true, false});
    testRotate(
        new boolean[] {false, true, false, true, false, true, false},
        -5,
        3,
        7,
        new boolean[] {false, true, false, false, true, false, true});
    testRotate(
        new boolean[] {false, true, false, true, false, true, false},
        -4,
        3,
        7,
        new boolean[] {false, true, false, true, false, true, false});
    testRotate(
        new boolean[] {false, true, false, true, false, true, false},
        -3,
        3,
        7,
        new boolean[] {false, true, false, false, true, false, true});
    testRotate(
        new boolean[] {false, true, false, true, false, true, false},
        -2,
        3,
        7,
        new boolean[] {false, true, false, true, false, true, false});
    testRotate(
        new boolean[] {false, true, false, true, false, true, false},
        -1,
        3,
        7,
        new boolean[] {false, true, false, false, true, false, true});
    testRotate(
        new boolean[] {false, true, false, true, false, true, false},
        0,
        3,
        7,
        new boolean[] {false, true, false, true, false, true, false});
    testRotate(
        new boolean[] {false, true, false, true, false, true, false},
        1,
        3,
        7,
        new boolean[] {false, true, false, false, true, false, true});
    testRotate(
        new boolean[] {false, true, false, true, false, true, false},
        2,
        3,
        7,
        new boolean[] {false, true, false, true, false, true, false});
    testRotate(
        new boolean[] {false, true, false, true, false, true, false},
        3,
        3,
        7,
        new boolean[] {false, true, false, false, true, false, true});
  }

  public void testToArray() {
    // need explicit type parameter to avoid javac warning!?
    List<Boolean> none = Arrays.<Boolean>asList();
    assertThat(Booleans.toArray(none)).isEqualTo(EMPTY);

    List<Boolean> one = Arrays.asList(false);
    assertThat(Booleans.toArray(one)).isEqualTo(ARRAY_FALSE);

    boolean[] array = {false, false, true};

    List<Boolean> three = Arrays.asList(false, false, true);
    assertThat(Booleans.toArray(three)).isEqualTo(array);

    assertThat(Booleans.toArray(Booleans.asList(array))).isEqualTo(array);
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
        assertThat(arr).hasLength(i);
        for (int j = 0; j < i; j++) {
          assertThat(arr[j]).isEqualTo(VALUES[j]);
        }
      }
    }
  }

  public void testToArray_withNull() {
    List<@Nullable Boolean> list = Arrays.asList(false, true, null);
    try {
      Booleans.toArray(list);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  @SuppressWarnings({"CollectionIsEmptyTruth", "CollectionIsNotEmptyTruth"})
  public void testAsListIsEmpty() {
    assertThat(Booleans.asList(EMPTY).isEmpty()).isTrue();
    assertThat(Booleans.asList(ARRAY_FALSE).isEmpty()).isFalse();
  }

  @SuppressWarnings("CollectionSizeTruth")
  public void testAsListSize() {
    assertThat(Booleans.asList(EMPTY).size()).isEqualTo(0);
    assertThat(Booleans.asList(ARRAY_FALSE).size()).isEqualTo(1);
    assertThat(Booleans.asList(ARRAY_FALSE_TRUE).size()).isEqualTo(2);
  }

  @SuppressWarnings("BooleanArrayIndexOfBoolean")
  public void testAsListIndexOf() {
    assertThat(Booleans.asList(EMPTY).indexOf((Object) "wrong type")).isEqualTo(-1);
    assertThat(Booleans.asList(EMPTY).indexOf(true)).isEqualTo(-1);
    assertThat(Booleans.asList(ARRAY_FALSE).indexOf(true)).isEqualTo(-1);
    assertThat(Booleans.asList(ARRAY_FALSE).indexOf(false)).isEqualTo(0);
    assertThat(Booleans.asList(ARRAY_FALSE_TRUE).indexOf(true)).isEqualTo(1);
  }

  public void testAsListLastIndexOf() {
    assertThat(Booleans.asList(EMPTY).lastIndexOf((Object) "wrong type")).isEqualTo(-1);
    assertThat(Booleans.asList(EMPTY).lastIndexOf(true)).isEqualTo(-1);
    assertThat(Booleans.asList(ARRAY_FALSE).lastIndexOf(true)).isEqualTo(-1);
    assertThat(Booleans.asList(ARRAY_FALSE_TRUE).lastIndexOf(true)).isEqualTo(1);
    assertThat(Booleans.asList(ARRAY_FALSE_FALSE).lastIndexOf(false)).isEqualTo(1);
  }

  @SuppressWarnings({"BooleanArrayContainsBoolean", "CollectionDoesNotContainTruth"})
  public void testAsListContains() {
    assertThat(Booleans.asList(EMPTY).contains((Object) "wrong type")).isFalse();
    assertThat(Booleans.asList(EMPTY).contains(true)).isFalse();
    assertThat(Booleans.asList(ARRAY_FALSE).contains(true)).isFalse();
    assertThat(Booleans.asList(ARRAY_TRUE).contains(true)).isTrue();
    assertThat(Booleans.asList(ARRAY_FALSE_TRUE).contains(false)).isTrue();
    assertThat(Booleans.asList(ARRAY_FALSE_TRUE).contains(true)).isTrue();
  }

  public void testAsListEquals() {
    assertThat(Booleans.asList(EMPTY).equals(Collections.emptyList())).isTrue();
    assertThat(Booleans.asList(ARRAY_FALSE).equals(Booleans.asList(ARRAY_FALSE))).isTrue();
    @SuppressWarnings("EqualsIncompatibleType")
    boolean listEqualsArray = Booleans.asList(ARRAY_FALSE).equals(ARRAY_FALSE);
    assertThat(listEqualsArray).isFalse();
    assertThat(Booleans.asList(ARRAY_FALSE).equals(null)).isFalse();
    assertThat(Booleans.asList(ARRAY_FALSE).equals(Booleans.asList(ARRAY_FALSE_TRUE))).isFalse();
    assertThat(Booleans.asList(ARRAY_FALSE_FALSE).equals(Booleans.asList(ARRAY_FALSE_TRUE)))
        .isFalse();
    assertEquals(1, Booleans.asList(ARRAY_FALSE_TRUE).lastIndexOf(true));
    List<Boolean> reference = Booleans.asList(ARRAY_FALSE);
    assertEquals(Booleans.asList(ARRAY_FALSE), reference);
    // Explicitly call `equals`; `assertEquals` might return fast
    assertThat(reference.equals(reference)).isTrue();
  }

  public void testAsListHashcode() {
    assertThat(Booleans.asList(EMPTY).hashCode()).isEqualTo(1);
    assertThat(Booleans.asList(ARRAY_FALSE).hashCode())
        .isEqualTo(Booleans.asList(ARRAY_FALSE).hashCode());
    List<Boolean> reference = Booleans.asList(ARRAY_FALSE);
    assertThat(reference.hashCode()).isEqualTo(Booleans.asList(ARRAY_FALSE).hashCode());
  }

  public void testAsListToString() {
    assertThat(Booleans.asList(ARRAY_FALSE).toString()).isEqualTo("[false]");
    assertThat(Booleans.asList(ARRAY_FALSE_TRUE).toString()).isEqualTo("[false, true]");
  }

  public void testAsListSet() {
    List<Boolean> list = Booleans.asList(ARRAY_FALSE);
    assertThat(list.set(0, true)).isFalse();
    assertThat(list.set(0, false)).isTrue();
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

  public void testAsListCanonicalValues() {
    List<Boolean> list = Booleans.asList(true, false);
    assertThat(list.get(0)).isSameInstanceAs(true);
    assertThat(list.get(1)).isSameInstanceAs(false);
    @SuppressWarnings("deprecation")
    Boolean anotherTrue = new Boolean(true);
    @SuppressWarnings("deprecation")
    Boolean anotherFalse = new Boolean(false);
    list.set(0, anotherTrue);
    assertThat(list.get(0)).isSameInstanceAs(true);
    list.set(1, anotherFalse);
    assertThat(list.get(1)).isSameInstanceAs(false);
  }

  public void testCountTrue() {
    assertThat(Booleans.countTrue()).isEqualTo(0);
    assertThat(Booleans.countTrue(false)).isEqualTo(0);
    assertThat(Booleans.countTrue(true)).isEqualTo(1);
    assertThat(Booleans.countTrue(false, true, false, true, false, true)).isEqualTo(3);
    assertThat(Booleans.countTrue(false, false, true, false, false)).isEqualTo(1);
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testNulls() {
    new NullPointerTester().testAllPublicStaticMethods(Booleans.class);
  }
}

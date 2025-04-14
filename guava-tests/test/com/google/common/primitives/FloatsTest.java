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

import static com.google.common.primitives.Floats.max;
import static com.google.common.primitives.Floats.min;
import static com.google.common.primitives.ReflectionFreeAssertThrows.assertThrows;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static java.lang.Float.NaN;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.base.Converter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.testing.Helpers;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import junit.framework.TestCase;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Unit test for {@link Floats}.
 *
 * @author Kevin Bourrillion
 */
@NullMarked
@GwtCompatible(emulated = true)
public class FloatsTest extends TestCase {
  private static final float[] EMPTY = {};
  private static final float[] ARRAY1 = {1.0f};
  private static final float[] ARRAY234 = {2.0f, 3.0f, 4.0f};

  private static final float LEAST = Float.NEGATIVE_INFINITY;
  private static final float GREATEST = Float.POSITIVE_INFINITY;

  private static final float[] NUMBERS =
      new float[] {
        LEAST,
        -Float.MAX_VALUE,
        -1f,
        -0f,
        0f,
        1f,
        Float.MAX_VALUE,
        GREATEST,
        Float.MIN_NORMAL,
        -Float.MIN_NORMAL,
        Float.MIN_VALUE,
        -Float.MIN_VALUE,
        Integer.MIN_VALUE,
        Integer.MAX_VALUE,
        Long.MIN_VALUE,
        Long.MAX_VALUE
      };

  private static final float[] VALUES = Floats.concat(NUMBERS, new float[] {NaN});

  // We need to test that our method behaves like the JDK method.
  @SuppressWarnings("InlineMeInliner")
  public void testHashCode() {
    for (float value : VALUES) {
      assertThat(Floats.hashCode(value)).isEqualTo(Float.hashCode(value));
    }
  }

  @SuppressWarnings("InlineMeInliner") // We need to test our method.
  public void testIsFinite() {
    for (float value : NUMBERS) {
      assertThat(Floats.isFinite(value)).isEqualTo(Float.isFinite(value));
    }
  }

  // We need to test that our method behaves like the JDK method.
  @SuppressWarnings("InlineMeInliner")
  public void testCompare() {
    for (float x : VALUES) {
      for (float y : VALUES) {
        // note: spec requires only that the sign is the same
        assertWithMessage(x + ", " + y).that(Floats.compare(x, y)).isEqualTo(Float.compare(x, y));
      }
    }
  }

  public void testContains() {
    assertThat(Floats.contains(EMPTY, 1.0f)).isFalse();
    assertThat(Floats.contains(ARRAY1, 2.0f)).isFalse();
    assertThat(Floats.contains(ARRAY234, 1.0f)).isFalse();
    assertThat(Floats.contains(new float[] {-1.0f}, -1.0f)).isTrue();
    assertThat(Floats.contains(ARRAY234, 2.0f)).isTrue();
    assertThat(Floats.contains(ARRAY234, 3.0f)).isTrue();
    assertThat(Floats.contains(ARRAY234, 4.0f)).isTrue();

    for (float value : NUMBERS) {
      assertWithMessage("" + value).that(Floats.contains(new float[] {5f, value}, value)).isTrue();
    }
    assertThat(Floats.contains(new float[] {5f, NaN}, NaN)).isFalse();
  }

  public void testIndexOf() {
    assertThat(Floats.indexOf(EMPTY, 1.0f)).isEqualTo(-1);
    assertThat(Floats.indexOf(ARRAY1, 2.0f)).isEqualTo(-1);
    assertThat(Floats.indexOf(ARRAY234, 1.0f)).isEqualTo(-1);
    assertThat(Floats.indexOf(new float[] {-1.0f}, -1.0f)).isEqualTo(0);
    assertThat(Floats.indexOf(ARRAY234, 2.0f)).isEqualTo(0);
    assertThat(Floats.indexOf(ARRAY234, 3.0f)).isEqualTo(1);
    assertThat(Floats.indexOf(ARRAY234, 4.0f)).isEqualTo(2);
    assertThat(Floats.indexOf(new float[] {2.0f, 3.0f, 2.0f, 3.0f}, 3.0f)).isEqualTo(1);

    for (float value : NUMBERS) {
      assertWithMessage("" + value)
          .that(Floats.indexOf(new float[] {5f, value}, value))
          .isEqualTo(1);
    }
    assertThat(Floats.indexOf(new float[] {5f, NaN}, NaN)).isEqualTo(-1);
  }

  public void testIndexOf_arrayTarget() {
    assertThat(Floats.indexOf(EMPTY, EMPTY)).isEqualTo(0);
    assertThat(Floats.indexOf(ARRAY234, EMPTY)).isEqualTo(0);
    assertThat(Floats.indexOf(EMPTY, ARRAY234)).isEqualTo(-1);
    assertThat(Floats.indexOf(ARRAY234, ARRAY1)).isEqualTo(-1);
    assertThat(Floats.indexOf(ARRAY1, ARRAY234)).isEqualTo(-1);
    assertThat(Floats.indexOf(ARRAY1, ARRAY1)).isEqualTo(0);
    assertThat(Floats.indexOf(ARRAY234, ARRAY234)).isEqualTo(0);
    assertThat(Floats.indexOf(ARRAY234, new float[] {2.0f, 3.0f})).isEqualTo(0);
    assertThat(Floats.indexOf(ARRAY234, new float[] {3.0f, 4.0f})).isEqualTo(1);
    assertThat(Floats.indexOf(ARRAY234, new float[] {3.0f})).isEqualTo(1);
    assertThat(Floats.indexOf(ARRAY234, new float[] {4.0f})).isEqualTo(2);
    assertThat(Floats.indexOf(new float[] {2.0f, 3.0f, 3.0f, 3.0f, 3.0f}, new float[] {3.0f}))
        .isEqualTo(1);
    assertThat(
            Floats.indexOf(
                new float[] {2.0f, 3.0f, 2.0f, 3.0f, 4.0f, 2.0f, 3.0f},
                new float[] {2.0f, 3.0f, 4.0f}))
        .isEqualTo(2);
    assertThat(
            Floats.indexOf(
                new float[] {2.0f, 2.0f, 3.0f, 4.0f, 2.0f, 3.0f, 4.0f},
                new float[] {2.0f, 3.0f, 4.0f}))
        .isEqualTo(1);
    assertThat(Floats.indexOf(new float[] {4.0f, 3.0f, 2.0f}, new float[] {2.0f, 3.0f, 4.0f}))
        .isEqualTo(-1);

    for (float value : NUMBERS) {
      assertWithMessage("" + value)
          .that(Floats.indexOf(new float[] {5f, value, value, 5f}, new float[] {value, value}))
          .isEqualTo(1);
    }
    assertThat(Floats.indexOf(new float[] {5f, NaN, NaN, 5f}, new float[] {NaN, NaN}))
        .isEqualTo(-1);
  }

  public void testLastIndexOf() {
    assertThat(Floats.lastIndexOf(EMPTY, 1.0f)).isEqualTo(-1);
    assertThat(Floats.lastIndexOf(ARRAY1, 2.0f)).isEqualTo(-1);
    assertThat(Floats.lastIndexOf(ARRAY234, 1.0f)).isEqualTo(-1);
    assertThat(Floats.lastIndexOf(new float[] {-1.0f}, -1.0f)).isEqualTo(0);
    assertThat(Floats.lastIndexOf(ARRAY234, 2.0f)).isEqualTo(0);
    assertThat(Floats.lastIndexOf(ARRAY234, 3.0f)).isEqualTo(1);
    assertThat(Floats.lastIndexOf(ARRAY234, 4.0f)).isEqualTo(2);
    assertThat(Floats.lastIndexOf(new float[] {2.0f, 3.0f, 2.0f, 3.0f}, 3.0f)).isEqualTo(3);

    for (float value : NUMBERS) {
      assertWithMessage("" + value)
          .that(Floats.lastIndexOf(new float[] {value, 5f}, value))
          .isEqualTo(0);
    }
    assertThat(Floats.lastIndexOf(new float[] {NaN, 5f}, NaN)).isEqualTo(-1);
  }

  @GwtIncompatible
  public void testMax_noArgs() {
    assertThrows(IllegalArgumentException.class, () -> max());
  }

  public void testMax() {
    assertThat(max(GREATEST)).isEqualTo(GREATEST);
    assertThat(max(LEAST)).isEqualTo(LEAST);
    assertThat(max(8.0f, 6.0f, 7.0f, 5.0f, 3.0f, 0.0f, 9.0f)).isEqualTo(9.0f);

    assertThat(max(-0f, 0f)).isEqualTo(0f);
    assertThat(max(0f, -0f)).isEqualTo(0f);
    assertThat(max(NUMBERS)).isEqualTo(GREATEST);
    assertThat(Float.isNaN(max(VALUES))).isTrue();
  }

  @GwtIncompatible
  public void testMin_noArgs() {
    assertThrows(IllegalArgumentException.class, () -> min());
  }

  public void testMin() {
    assertThat(min(LEAST)).isEqualTo(LEAST);
    assertThat(min(GREATEST)).isEqualTo(GREATEST);
    assertThat(min(8.0f, 6.0f, 7.0f, 5.0f, 3.0f, 0.0f, 9.0f)).isEqualTo(0.0f);

    assertThat(min(-0f, 0f)).isEqualTo(-0f);
    assertThat(min(0f, -0f)).isEqualTo(-0f);
    assertThat(min(NUMBERS)).isEqualTo(LEAST);
    assertThat(Float.isNaN(min(VALUES))).isTrue();
  }

  public void testConstrainToRange() {
    assertThat(Floats.constrainToRange(1.0f, 0.0f, 5.0f)).isEqualTo(1.0f);
    assertThat(Floats.constrainToRange(1.0f, 1.0f, 5.0f)).isEqualTo(1.0f);
    assertThat(Floats.constrainToRange(1.0f, 3.0f, 5.0f)).isEqualTo(3.0f);
    assertThat(Floats.constrainToRange(0.0f, -5.0f, -1.0f)).isEqualTo(-1.0f);
    assertThat(Floats.constrainToRange(5.0f, 2.0f, 2.0f)).isEqualTo(2.0f);
    assertThrows(IllegalArgumentException.class, () -> Floats.constrainToRange(1.0f, 3.0f, 2.0f));
  }

  public void testConcat() {
    assertThat(Floats.concat()).isEqualTo(EMPTY);
    assertThat(Floats.concat(EMPTY)).isEqualTo(EMPTY);
    assertThat(Floats.concat(EMPTY, EMPTY, EMPTY)).isEqualTo(EMPTY);
    assertThat(Floats.concat(ARRAY1)).isEqualTo(ARRAY1);
    assertThat(Floats.concat(ARRAY1)).isNotSameInstanceAs(ARRAY1);
    assertThat(Floats.concat(EMPTY, ARRAY1, EMPTY)).isEqualTo(ARRAY1);
    assertThat(Floats.concat(ARRAY1, ARRAY1, ARRAY1)).isEqualTo(new float[] {1.0f, 1.0f, 1.0f});
    assertThat(Floats.concat(ARRAY1, ARRAY234)).isEqualTo(new float[] {1.0f, 2.0f, 3.0f, 4.0f});
  }

  @GwtIncompatible // different overflow behavior; could probably be made to work by using ~~
  public void testConcat_overflow_negative() {
    int dim1 = 1 << 16;
    int dim2 = 1 << 15;
    assertThat(dim1 * dim2).isLessThan(0);
    testConcatOverflow(dim1, dim2);
  }

  @GwtIncompatible // different overflow behavior; could probably be made to work by using ~~
  public void testConcat_overflow_nonNegative() {
    int dim1 = 1 << 16;
    int dim2 = 1 << 16;
    assertThat(dim1 * dim2).isAtLeast(0);
    testConcatOverflow(dim1, dim2);
  }

  private static void testConcatOverflow(int arraysDim1, int arraysDim2) {
    assertThat((long) arraysDim1 * arraysDim2).isNotEqualTo((long) (arraysDim1 * arraysDim2));

    float[][] arrays = new float[arraysDim1][];
    // it's shared to avoid using too much memory in tests
    float[] sharedArray = new float[arraysDim2];
    Arrays.fill(arrays, sharedArray);

    try {
      Floats.concat(arrays);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testEnsureCapacity() {
    assertThat(Floats.ensureCapacity(EMPTY, 0, 1)).isSameInstanceAs(EMPTY);
    assertThat(Floats.ensureCapacity(ARRAY1, 0, 1)).isSameInstanceAs(ARRAY1);
    assertThat(Floats.ensureCapacity(ARRAY1, 1, 1)).isSameInstanceAs(ARRAY1);
    assertThat(Arrays.equals(new float[] {1.0f, 0.0f, 0.0f}, Floats.ensureCapacity(ARRAY1, 2, 1)))
        .isTrue();
  }

  public void testEnsureCapacity_fail() {
    assertThrows(IllegalArgumentException.class, () -> Floats.ensureCapacity(ARRAY1, -1, 1));
    assertThrows(IllegalArgumentException.class, () -> Floats.ensureCapacity(ARRAY1, 1, -1));
  }

  @GwtIncompatible // Float.toString returns different value in GWT.
  public void testJoin() {
    assertThat(Floats.join(",", EMPTY)).isEmpty();
    assertThat(Floats.join(",", ARRAY1)).isEqualTo("1.0");
    assertThat(Floats.join(",", 1.0f, 2.0f)).isEqualTo("1.0,2.0");
    assertThat(Floats.join("", 1.0f, 2.0f, 3.0f)).isEqualTo("1.02.03.0");
  }

  public void testLexicographicalComparator() {
    List<float[]> ordered =
        Arrays.asList(
            new float[] {},
            new float[] {LEAST},
            new float[] {LEAST, LEAST},
            new float[] {LEAST, 1.0f},
            new float[] {1.0f},
            new float[] {1.0f, LEAST},
            new float[] {GREATEST, Float.MAX_VALUE},
            new float[] {GREATEST, GREATEST},
            new float[] {GREATEST, GREATEST, GREATEST});

    Comparator<float[]> comparator = Floats.lexicographicalComparator();
    Helpers.testComparator(comparator, ordered);
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testLexicographicalComparatorSerializable() {
    Comparator<float[]> comparator = Floats.lexicographicalComparator();
    assertThat(SerializableTester.reserialize(comparator)).isSameInstanceAs(comparator);
  }

  public void testReverse() {
    testReverse(new float[] {}, new float[] {});
    testReverse(new float[] {1}, new float[] {1});
    testReverse(new float[] {1, 2}, new float[] {2, 1});
    testReverse(new float[] {3, 1, 1}, new float[] {1, 1, 3});
    testReverse(new float[] {-1, 1, -2, 2}, new float[] {2, -2, 1, -1});
  }

  private static void testReverse(float[] input, float[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Floats.reverse(input);
    assertThat(input).isEqualTo(expectedOutput);
  }

  private static void testReverse(
      float[] input, int fromIndex, int toIndex, float[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Floats.reverse(input, fromIndex, toIndex);
    assertThat(input).isEqualTo(expectedOutput);
  }

  public void testReverseIndexed() {
    testReverse(new float[] {}, 0, 0, new float[] {});
    testReverse(new float[] {1}, 0, 1, new float[] {1});
    testReverse(new float[] {1, 2}, 0, 2, new float[] {2, 1});
    testReverse(new float[] {3, 1, 1}, 0, 2, new float[] {1, 3, 1});
    testReverse(new float[] {3, 1, 1}, 0, 1, new float[] {3, 1, 1});
    testReverse(new float[] {-1, 1, -2, 2}, 1, 3, new float[] {-1, -2, 1, 2});
  }

  private static void testRotate(float[] input, int distance, float[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Floats.rotate(input, distance);
    assertThat(input).isEqualTo(expectedOutput);
  }

  private static void testRotate(
      float[] input, int distance, int fromIndex, int toIndex, float[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Floats.rotate(input, distance, fromIndex, toIndex);
    assertThat(input).isEqualTo(expectedOutput);
  }

  public void testRotate() {
    testRotate(new float[] {}, -1, new float[] {});
    testRotate(new float[] {}, 0, new float[] {});
    testRotate(new float[] {}, 1, new float[] {});

    testRotate(new float[] {1}, -2, new float[] {1});
    testRotate(new float[] {1}, -1, new float[] {1});
    testRotate(new float[] {1}, 0, new float[] {1});
    testRotate(new float[] {1}, 1, new float[] {1});
    testRotate(new float[] {1}, 2, new float[] {1});

    testRotate(new float[] {1, 2}, -3, new float[] {2, 1});
    testRotate(new float[] {1, 2}, -1, new float[] {2, 1});
    testRotate(new float[] {1, 2}, -2, new float[] {1, 2});
    testRotate(new float[] {1, 2}, 0, new float[] {1, 2});
    testRotate(new float[] {1, 2}, 1, new float[] {2, 1});
    testRotate(new float[] {1, 2}, 2, new float[] {1, 2});
    testRotate(new float[] {1, 2}, 3, new float[] {2, 1});

    testRotate(new float[] {1, 2, 3}, -5, new float[] {3, 1, 2});
    testRotate(new float[] {1, 2, 3}, -4, new float[] {2, 3, 1});
    testRotate(new float[] {1, 2, 3}, -3, new float[] {1, 2, 3});
    testRotate(new float[] {1, 2, 3}, -2, new float[] {3, 1, 2});
    testRotate(new float[] {1, 2, 3}, -1, new float[] {2, 3, 1});
    testRotate(new float[] {1, 2, 3}, 0, new float[] {1, 2, 3});
    testRotate(new float[] {1, 2, 3}, 1, new float[] {3, 1, 2});
    testRotate(new float[] {1, 2, 3}, 2, new float[] {2, 3, 1});
    testRotate(new float[] {1, 2, 3}, 3, new float[] {1, 2, 3});
    testRotate(new float[] {1, 2, 3}, 4, new float[] {3, 1, 2});
    testRotate(new float[] {1, 2, 3}, 5, new float[] {2, 3, 1});

    testRotate(new float[] {1, 2, 3, 4}, -9, new float[] {2, 3, 4, 1});
    testRotate(new float[] {1, 2, 3, 4}, -5, new float[] {2, 3, 4, 1});
    testRotate(new float[] {1, 2, 3, 4}, -1, new float[] {2, 3, 4, 1});
    testRotate(new float[] {1, 2, 3, 4}, 0, new float[] {1, 2, 3, 4});
    testRotate(new float[] {1, 2, 3, 4}, 1, new float[] {4, 1, 2, 3});
    testRotate(new float[] {1, 2, 3, 4}, 5, new float[] {4, 1, 2, 3});
    testRotate(new float[] {1, 2, 3, 4}, 9, new float[] {4, 1, 2, 3});

    testRotate(new float[] {1, 2, 3, 4, 5}, -6, new float[] {2, 3, 4, 5, 1});
    testRotate(new float[] {1, 2, 3, 4, 5}, -4, new float[] {5, 1, 2, 3, 4});
    testRotate(new float[] {1, 2, 3, 4, 5}, -3, new float[] {4, 5, 1, 2, 3});
    testRotate(new float[] {1, 2, 3, 4, 5}, -1, new float[] {2, 3, 4, 5, 1});
    testRotate(new float[] {1, 2, 3, 4, 5}, 0, new float[] {1, 2, 3, 4, 5});
    testRotate(new float[] {1, 2, 3, 4, 5}, 1, new float[] {5, 1, 2, 3, 4});
    testRotate(new float[] {1, 2, 3, 4, 5}, 3, new float[] {3, 4, 5, 1, 2});
    testRotate(new float[] {1, 2, 3, 4, 5}, 4, new float[] {2, 3, 4, 5, 1});
    testRotate(new float[] {1, 2, 3, 4, 5}, 6, new float[] {5, 1, 2, 3, 4});
  }

  public void testRotateIndexed() {
    testRotate(new float[] {}, 0, 0, 0, new float[] {});

    testRotate(new float[] {1}, 0, 0, 1, new float[] {1});
    testRotate(new float[] {1}, 1, 0, 1, new float[] {1});
    testRotate(new float[] {1}, 1, 1, 1, new float[] {1});

    // Rotate the central 5 elements, leaving the ends as-is
    testRotate(new float[] {0, 1, 2, 3, 4, 5, 6}, -6, 1, 6, new float[] {0, 2, 3, 4, 5, 1, 6});
    testRotate(new float[] {0, 1, 2, 3, 4, 5, 6}, -1, 1, 6, new float[] {0, 2, 3, 4, 5, 1, 6});
    testRotate(new float[] {0, 1, 2, 3, 4, 5, 6}, 0, 1, 6, new float[] {0, 1, 2, 3, 4, 5, 6});
    testRotate(new float[] {0, 1, 2, 3, 4, 5, 6}, 5, 1, 6, new float[] {0, 1, 2, 3, 4, 5, 6});
    testRotate(new float[] {0, 1, 2, 3, 4, 5, 6}, 14, 1, 6, new float[] {0, 2, 3, 4, 5, 1, 6});

    // Rotate the first three elements
    testRotate(new float[] {0, 1, 2, 3, 4, 5, 6}, -2, 0, 3, new float[] {2, 0, 1, 3, 4, 5, 6});
    testRotate(new float[] {0, 1, 2, 3, 4, 5, 6}, -1, 0, 3, new float[] {1, 2, 0, 3, 4, 5, 6});
    testRotate(new float[] {0, 1, 2, 3, 4, 5, 6}, 0, 0, 3, new float[] {0, 1, 2, 3, 4, 5, 6});
    testRotate(new float[] {0, 1, 2, 3, 4, 5, 6}, 1, 0, 3, new float[] {2, 0, 1, 3, 4, 5, 6});
    testRotate(new float[] {0, 1, 2, 3, 4, 5, 6}, 2, 0, 3, new float[] {1, 2, 0, 3, 4, 5, 6});

    // Rotate the last four elements
    testRotate(new float[] {0, 1, 2, 3, 4, 5, 6}, -6, 3, 7, new float[] {0, 1, 2, 5, 6, 3, 4});
    testRotate(new float[] {0, 1, 2, 3, 4, 5, 6}, -5, 3, 7, new float[] {0, 1, 2, 4, 5, 6, 3});
    testRotate(new float[] {0, 1, 2, 3, 4, 5, 6}, -4, 3, 7, new float[] {0, 1, 2, 3, 4, 5, 6});
    testRotate(new float[] {0, 1, 2, 3, 4, 5, 6}, -3, 3, 7, new float[] {0, 1, 2, 6, 3, 4, 5});
    testRotate(new float[] {0, 1, 2, 3, 4, 5, 6}, -2, 3, 7, new float[] {0, 1, 2, 5, 6, 3, 4});
    testRotate(new float[] {0, 1, 2, 3, 4, 5, 6}, -1, 3, 7, new float[] {0, 1, 2, 4, 5, 6, 3});
    testRotate(new float[] {0, 1, 2, 3, 4, 5, 6}, 0, 3, 7, new float[] {0, 1, 2, 3, 4, 5, 6});
    testRotate(new float[] {0, 1, 2, 3, 4, 5, 6}, 1, 3, 7, new float[] {0, 1, 2, 6, 3, 4, 5});
    testRotate(new float[] {0, 1, 2, 3, 4, 5, 6}, 2, 3, 7, new float[] {0, 1, 2, 5, 6, 3, 4});
    testRotate(new float[] {0, 1, 2, 3, 4, 5, 6}, 3, 3, 7, new float[] {0, 1, 2, 4, 5, 6, 3});
  }

  public void testSortDescending() {
    testSortDescending(new float[] {}, new float[] {});
    testSortDescending(new float[] {1}, new float[] {1});
    testSortDescending(new float[] {1, 2}, new float[] {2, 1});
    testSortDescending(new float[] {1, 3, 1}, new float[] {3, 1, 1});
    testSortDescending(new float[] {-1, 1, -2, 2}, new float[] {2, 1, -1, -2});
    testSortDescending(
        new float[] {-1, 1, Float.NaN, -2, -0f, 0, 2},
        new float[] {Float.NaN, 2, 1, 0, -0f, -1, -2});
  }

  private static void testSortDescending(float[] input, float[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Floats.sortDescending(input);
    for (int i = 0; i < input.length; i++) {
      assertThat(input[i]).isEqualTo(expectedOutput[i]);
    }
  }

  private static void testSortDescending(
      float[] input, int fromIndex, int toIndex, float[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Floats.sortDescending(input, fromIndex, toIndex);
    for (int i = 0; i < input.length; i++) {
      assertThat(input[i]).isEqualTo(expectedOutput[i]);
    }
  }

  public void testSortDescendingIndexed() {
    testSortDescending(new float[] {}, 0, 0, new float[] {});
    testSortDescending(new float[] {1}, 0, 1, new float[] {1});
    testSortDescending(new float[] {1, 2}, 0, 2, new float[] {2, 1});
    testSortDescending(new float[] {1, 3, 1}, 0, 2, new float[] {3, 1, 1});
    testSortDescending(new float[] {1, 3, 1}, 0, 1, new float[] {1, 3, 1});
    testSortDescending(new float[] {-1, -2, 1, 2}, 1, 3, new float[] {-1, 1, -2, 2});
    testSortDescending(
        new float[] {-1, 1, Float.NaN, -2, 2}, 1, 4, new float[] {-1, Float.NaN, 1, -2, 2});
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testStringConverterSerialization() {
    SerializableTester.reserializeAndAssert(Floats.stringConverter());
  }

  public void testToArray() {
    // need explicit type parameter to avoid javac warning!?
    List<Float> none = Arrays.<Float>asList();
    assertThat(Floats.toArray(none)).isEqualTo(EMPTY);

    List<Float> one = Arrays.asList(1.0f);
    assertThat(Floats.toArray(one)).isEqualTo(ARRAY1);

    float[] array = {0.0f, 1.0f, 3.0f};

    List<Float> three = Arrays.asList(0.0f, 1.0f, 3.0f);
    assertThat(Floats.toArray(three)).isEqualTo(array);

    assertThat(Floats.toArray(Floats.asList(array))).isEqualTo(array);
  }

  public void testToArray_threadSafe() {
    for (int delta : new int[] {+1, 0, -1}) {
      for (int i = 0; i < VALUES.length; i++) {
        List<Float> list = Floats.asList(VALUES).subList(0, i);
        Collection<Float> misleadingSize = Helpers.misleadingSizeCollection(delta);
        misleadingSize.addAll(list);
        float[] arr = Floats.toArray(misleadingSize);
        assertThat(arr.length).isEqualTo(i);
        for (int j = 0; j < i; j++) {
          assertThat(arr[j]).isEqualTo(VALUES[j]);
        }
      }
    }
  }

  public void testToArray_withNull() {
    List<@Nullable Float> list = Arrays.asList(0.0f, 1.0f, null);
    assertThrows(NullPointerException.class, () -> Floats.toArray(list));
  }

  public void testToArray_withConversion() {
    float[] array = {0.0f, 1.0f, 2.0f};

    List<Byte> bytes = Arrays.asList((byte) 0, (byte) 1, (byte) 2);
    List<Short> shorts = Arrays.asList((short) 0, (short) 1, (short) 2);
    List<Integer> ints = Arrays.asList(0, 1, 2);
    List<Float> floats = Arrays.asList(0.0f, 1.0f, 2.0f);
    List<Long> longs = Arrays.asList(0L, 1L, 2L);
    List<Double> doubles = Arrays.asList(0.0, 1.0, 2.0);

    assertThat(Floats.toArray(bytes)).isEqualTo(array);
    assertThat(Floats.toArray(shorts)).isEqualTo(array);
    assertThat(Floats.toArray(ints)).isEqualTo(array);
    assertThat(Floats.toArray(floats)).isEqualTo(array);
    assertThat(Floats.toArray(longs)).isEqualTo(array);
    assertThat(Floats.toArray(doubles)).isEqualTo(array);
  }

  @J2ktIncompatible // b/239034072: Kotlin varargs copy parameter arrays.
  public void testAsList_isAView() {
    float[] array = {0.0f, 1.0f};
    List<Float> list = Floats.asList(array);
    list.set(0, 2.0f);
    assertThat(array).isEqualTo(new float[] {2.0f, 1.0f});
    array[1] = 3.0f;
    assertThat(list).containsExactly(2.0f, 3.0f).inOrder();
  }

  public void testAsList_toArray_roundTrip() {
    float[] array = {0.0f, 1.0f, 2.0f};
    List<Float> list = Floats.asList(array);
    float[] newArray = Floats.toArray(list);

    // Make sure it returned a copy
    list.set(0, 4.0f);
    assertThat(newArray).isEqualTo(new float[] {0.0f, 1.0f, 2.0f});
    newArray[1] = 5.0f;
    assertThat((float) list.get(1)).isEqualTo(1.0f);
  }

  // This test stems from a real bug found by andrewk
  public void testAsList_subList_toArray_roundTrip() {
    float[] array = {0.0f, 1.0f, 2.0f, 3.0f};
    List<Float> list = Floats.asList(array);
    assertThat(Floats.toArray(list.subList(1, 3))).isEqualTo(new float[] {1.0f, 2.0f});
    assertThat(Floats.toArray(list.subList(2, 2))).isEmpty();
  }

  // `primitives` can't depend on `collect`, so this is what the prod code has to return.
  @SuppressWarnings("EmptyList")
  public void testAsListEmpty() {
    assertThat(Floats.asList(EMPTY)).isSameInstanceAs(Collections.emptyList());
  }

  /**
   * A reference implementation for {@code tryParse} that just catches the exception from {@link
   * Float#valueOf}.
   */
  private static @Nullable Float referenceTryParse(String input) {
    if (input.trim().length() < input.length()) {
      return null;
    }
    try {
      return Float.valueOf(input);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  @GwtIncompatible // Floats.tryParse
  private static void checkTryParse(String input) {
    assertThat(Floats.tryParse(input)).isEqualTo(referenceTryParse(input));
  }

  @GwtIncompatible // Floats.tryParse
  private static void checkTryParse(float expected, String input) {
    assertThat(Floats.tryParse(input)).isEqualTo(Float.valueOf(expected));
  }

  @GwtIncompatible // Floats.tryParse
  public void testTryParseHex() {
    for (String signChar : ImmutableList.of("", "+", "-")) {
      for (String hexPrefix : ImmutableList.of("0x", "0X")) {
        for (String iPart : ImmutableList.of("", "0", "1", "F", "f", "c4", "CE")) {
          for (String fPart : ImmutableList.of("", ".", ".F", ".52", ".a")) {
            for (String expMarker : ImmutableList.of("p", "P")) {
              for (String exponent : ImmutableList.of("0", "-5", "+20", "52")) {
                for (String typePart : ImmutableList.of("", "D", "F", "d", "f")) {
                  checkTryParse(
                      signChar + hexPrefix + iPart + fPart + expMarker + exponent + typePart);
                }
              }
            }
          }
        }
      }
    }
  }

  @AndroidIncompatible // slow
  @GwtIncompatible // Floats.tryParse
  public void testTryParseAllCodePoints() {
    // Exercise non-ASCII digit test cases and the like.
    char[] tmp = new char[2];
    for (int i = Character.MIN_CODE_POINT; i < Character.MAX_CODE_POINT; i++) {
      Character.toChars(i, tmp, 0);
      checkTryParse(String.copyValueOf(tmp, 0, Character.charCount(i)));
    }
  }

  @GwtIncompatible // Floats.tryParse
  public void testTryParseOfToStringIsOriginal() {
    for (float f : NUMBERS) {
      checkTryParse(f, Float.toString(f));
    }
  }

  @J2ktIncompatible // hexadecimal floats
  @GwtIncompatible // Floats.tryParse
  public void testTryParseOfToHexStringIsOriginal() {
    for (float f : NUMBERS) {
      checkTryParse(f, Float.toHexString(f));
    }
  }

  @GwtIncompatible // Floats.tryParse
  public void testTryParseNaN() {
    checkTryParse("NaN");
    checkTryParse("+NaN");
    checkTryParse("-NaN");
  }

  @GwtIncompatible // Floats.tryParse
  public void testTryParseInfinity() {
    checkTryParse(Float.POSITIVE_INFINITY, "Infinity");
    checkTryParse(Float.POSITIVE_INFINITY, "+Infinity");
    checkTryParse(Float.NEGATIVE_INFINITY, "-Infinity");
  }

  private static final String[] BAD_TRY_PARSE_INPUTS = {
    "",
    "+-",
    "+-0",
    " 5",
    "32 ",
    " 55 ",
    "infinity",
    "POSITIVE_INFINITY",
    "0x9A",
    "0x9A.bE-5",
    ".",
    ".e5",
    "NaNd",
    "InfinityF"
  };

  @GwtIncompatible // Floats.tryParse
  public void testTryParseFailures() {
    for (String badInput : BAD_TRY_PARSE_INPUTS) {
      assertThat(Floats.tryParse(badInput)).isEqualTo(referenceTryParse(badInput));
      assertThat(Floats.tryParse(badInput)).isNull();
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testNulls() {
    new NullPointerTester().testAllPublicStaticMethods(Floats.class);
  }

  @GwtIncompatible // Float.toString returns different value in GWT.
  public void testStringConverter_convert() {
    Converter<String, Float> converter = Floats.stringConverter();
    assertThat(converter.convert("1.0")).isEqualTo(1.0f);
    assertThat(converter.convert("0.0")).isEqualTo(0.0f);
    assertThat(converter.convert("-1.0")).isEqualTo(-1.0f);
    assertThat(converter.convert("1")).isEqualTo(1.0f);
    assertThat(converter.convert("0")).isEqualTo(0.0f);
    assertThat(converter.convert("-1")).isEqualTo(-1.0f);
    assertThat(converter.convert("1e6")).isEqualTo(1e6f);
    assertThat(converter.convert("1e-6")).isEqualTo(1e-6f);
  }

  public void testStringConverter_convertError() {
    assertThrows(NumberFormatException.class, () -> Floats.stringConverter().convert("notanumber"));
  }

  public void testStringConverter_nullConversions() {
    assertThat(Floats.stringConverter().convert(null)).isNull();
    assertThat(Floats.stringConverter().reverse().convert(null)).isNull();
  }

  @J2ktIncompatible
  @GwtIncompatible // Float.toString returns different value in GWT.
  public void testStringConverter_reverse() {
    Converter<String, Float> converter = Floats.stringConverter();
    assertThat(converter.reverse().convert(1.0f)).isEqualTo("1.0");
    assertThat(converter.reverse().convert(0.0f)).isEqualTo("0.0");
    assertThat(converter.reverse().convert(-1.0f)).isEqualTo("-1.0");
    assertThat(converter.reverse().convert(1e6f)).isEqualTo("1000000.0");
    assertThat(converter.reverse().convert(1e-6f)).isEqualTo("1.0E-6");
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testStringConverter_nullPointerTester() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicInstanceMethods(Floats.stringConverter());
  }

  @GwtIncompatible
  public void testTryParse_withNullNoGwt() {
    assertThat(Floats.tryParse("null")).isNull();
    assertThrows(NullPointerException.class, () -> Floats.tryParse(null));
  }
}

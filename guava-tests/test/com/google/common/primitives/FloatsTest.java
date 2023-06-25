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
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Unit test for {@link Floats}.
 *
 * @author Kevin Bourrillion
 */
@ElementTypesAreNonnullByDefault
@GwtCompatible(emulated = true)
public class FloatsTest extends TestCase {
  private static final float[] EMPTY = {};
  private static final float[] ARRAY1 = {(float) 1};
  private static final float[] ARRAY234 = {(float) 2, (float) 3, (float) 4};

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

  public void testHashCode() {
    for (float value : VALUES) {
      assertThat(Floats.hashCode(value)).isEqualTo(((Float) value).hashCode());
    }
  }

  public void testIsFinite() {
    for (float value : NUMBERS) {
      assertThat(Floats.isFinite(value))
          .isEqualTo(!(Float.isInfinite(value) || Float.isNaN(value)));
    }
  }

  public void testCompare() {
    for (float x : VALUES) {
      for (float y : VALUES) {
        // note: spec requires only that the sign is the same
        assertWithMessage(x + ", " + y)
            .that(Floats.compare(x, y))
            .isEqualTo(Float.valueOf(x).compareTo(y));
      }
    }
  }

  public void testContains() {
    assertThat(Floats.contains(EMPTY, (float) 1)).isFalse();
    assertThat(Floats.contains(ARRAY1, (float) 2)).isFalse();
    assertThat(Floats.contains(ARRAY234, (float) 1)).isFalse();
    assertThat(Floats.contains(new float[] {(float) -1}, (float) -1)).isTrue();
    assertThat(Floats.contains(ARRAY234, (float) 2)).isTrue();
    assertThat(Floats.contains(ARRAY234, (float) 3)).isTrue();
    assertThat(Floats.contains(ARRAY234, (float) 4)).isTrue();

    for (float value : NUMBERS) {
      assertWithMessage("" + value).that(Floats.contains(new float[] {5f, value}, value)).isTrue();
    }
    assertThat(Floats.contains(new float[] {5f, NaN}, NaN)).isFalse();
  }

  public void testIndexOf() {
    assertThat(Floats.indexOf(EMPTY, (float) 1)).isEqualTo(-1);
    assertThat(Floats.indexOf(ARRAY1, (float) 2)).isEqualTo(-1);
    assertThat(Floats.indexOf(ARRAY234, (float) 1)).isEqualTo(-1);
    assertThat(Floats.indexOf(new float[] {(float) -1}, (float) -1)).isEqualTo(0);
    assertThat(Floats.indexOf(ARRAY234, (float) 2)).isEqualTo(0);
    assertThat(Floats.indexOf(ARRAY234, (float) 3)).isEqualTo(1);
    assertThat(Floats.indexOf(ARRAY234, (float) 4)).isEqualTo(2);
    assertThat(Floats.indexOf(new float[] {(float) 2, (float) 3, (float) 2, (float) 3}, (float) 3))
        .isEqualTo(1);

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
    assertThat(Floats.indexOf(ARRAY234, new float[] {(float) 2, (float) 3})).isEqualTo(0);
    assertThat(Floats.indexOf(ARRAY234, new float[] {(float) 3, (float) 4})).isEqualTo(1);
    assertThat(Floats.indexOf(ARRAY234, new float[] {(float) 3})).isEqualTo(1);
    assertThat(Floats.indexOf(ARRAY234, new float[] {(float) 4})).isEqualTo(2);
    assertThat(
            Floats.indexOf(
                new float[] {(float) 2, (float) 3, (float) 3, (float) 3, (float) 3},
                new float[] {(float) 3}))
        .isEqualTo(1);
    assertThat(
            Floats.indexOf(
                new float[] {
                  (float) 2, (float) 3, (float) 2, (float) 3, (float) 4, (float) 2, (float) 3
                },
                new float[] {(float) 2, (float) 3, (float) 4}))
        .isEqualTo(2);
    assertThat(
            Floats.indexOf(
                new float[] {
                  (float) 2, (float) 2, (float) 3, (float) 4, (float) 2, (float) 3, (float) 4
                },
                new float[] {(float) 2, (float) 3, (float) 4}))
        .isEqualTo(1);
    assertThat(
            Floats.indexOf(
                new float[] {(float) 4, (float) 3, (float) 2},
                new float[] {(float) 2, (float) 3, (float) 4}))
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
    assertThat(Floats.lastIndexOf(EMPTY, (float) 1)).isEqualTo(-1);
    assertThat(Floats.lastIndexOf(ARRAY1, (float) 2)).isEqualTo(-1);
    assertThat(Floats.lastIndexOf(ARRAY234, (float) 1)).isEqualTo(-1);
    assertThat(Floats.lastIndexOf(new float[] {(float) -1}, (float) -1)).isEqualTo(0);
    assertThat(Floats.lastIndexOf(ARRAY234, (float) 2)).isEqualTo(0);
    assertThat(Floats.lastIndexOf(ARRAY234, (float) 3)).isEqualTo(1);
    assertThat(Floats.lastIndexOf(ARRAY234, (float) 4)).isEqualTo(2);
    assertThat(
            Floats.lastIndexOf(new float[] {(float) 2, (float) 3, (float) 2, (float) 3}, (float) 3))
        .isEqualTo(3);

    for (float value : NUMBERS) {
      assertWithMessage("" + value)
          .that(Floats.lastIndexOf(new float[] {value, 5f}, value))
          .isEqualTo(0);
    }
    assertThat(Floats.lastIndexOf(new float[] {NaN, 5f}, NaN)).isEqualTo(-1);
  }

  @J2ktIncompatible
  @GwtIncompatible
  public void testMax_noArgs() {
    try {
      Floats.max();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMax() {
    assertThat(Floats.max(GREATEST)).isEqualTo(GREATEST);
    assertThat(Floats.max(LEAST)).isEqualTo(LEAST);
    assertThat(
            Floats.max((float) 8, (float) 6, (float) 7, (float) 5, (float) 3, (float) 0, (float) 9))
        .isEqualTo((float) 9);

    assertThat(Floats.max(-0f, 0f)).isEqualTo(0f);
    assertThat(Floats.max(0f, -0f)).isEqualTo(0f);
    assertThat(Floats.max(NUMBERS)).isEqualTo(GREATEST);
    assertThat(Float.isNaN(Floats.max(VALUES))).isTrue();
  }

  @J2ktIncompatible
  @GwtIncompatible
  public void testMin_noArgs() {
    try {
      Floats.min();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMin() {
    assertThat(Floats.min(LEAST)).isEqualTo(LEAST);
    assertThat(Floats.min(GREATEST)).isEqualTo(GREATEST);
    assertThat(
            Floats.min((float) 8, (float) 6, (float) 7, (float) 5, (float) 3, (float) 0, (float) 9))
        .isEqualTo((float) 0);

    assertThat(Floats.min(-0f, 0f)).isEqualTo(-0f);
    assertThat(Floats.min(0f, -0f)).isEqualTo(-0f);
    assertThat(Floats.min(NUMBERS)).isEqualTo(LEAST);
    assertThat(Float.isNaN(Floats.min(VALUES))).isTrue();
  }

  public void testConstrainToRange() {
    assertThat(Floats.constrainToRange((float) 1, (float) 0, (float) 5)).isEqualTo((float) 1);
    assertThat(Floats.constrainToRange((float) 1, (float) 1, (float) 5)).isEqualTo((float) 1);
    assertThat(Floats.constrainToRange((float) 1, (float) 3, (float) 5)).isEqualTo((float) 3);
    assertThat(Floats.constrainToRange((float) 0, (float) -5, (float) -1)).isEqualTo((float) -1);
    assertThat(Floats.constrainToRange((float) 5, (float) 2, (float) 2)).isEqualTo((float) 2);
    try {
      Floats.constrainToRange((float) 1, (float) 3, (float) 2);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testConcat() {
    assertThat(Floats.concat()).isEqualTo(EMPTY);
    assertThat(Floats.concat(EMPTY)).isEqualTo(EMPTY);
    assertThat(Floats.concat(EMPTY, EMPTY, EMPTY)).isEqualTo(EMPTY);
    assertThat(Floats.concat(ARRAY1)).isEqualTo(ARRAY1);
    assertThat(Floats.concat(ARRAY1)).isNotSameInstanceAs(ARRAY1);
    assertThat(Floats.concat(EMPTY, ARRAY1, EMPTY)).isEqualTo(ARRAY1);
    assertThat(Floats.concat(ARRAY1, ARRAY1, ARRAY1))
        .isEqualTo(new float[] {(float) 1, (float) 1, (float) 1});
    assertThat(Floats.concat(ARRAY1, ARRAY234))
        .isEqualTo(new float[] {(float) 1, (float) 2, (float) 3, (float) 4});
  }

  public void testEnsureCapacity() {
    assertThat(Floats.ensureCapacity(EMPTY, 0, 1)).isSameInstanceAs(EMPTY);
    assertThat(Floats.ensureCapacity(ARRAY1, 0, 1)).isSameInstanceAs(ARRAY1);
    assertThat(Floats.ensureCapacity(ARRAY1, 1, 1)).isSameInstanceAs(ARRAY1);
    assertThat(
            Arrays.equals(
                new float[] {(float) 1, (float) 0, (float) 0}, Floats.ensureCapacity(ARRAY1, 2, 1)))
        .isTrue();
  }

  public void testEnsureCapacity_fail() {
    try {
      Floats.ensureCapacity(ARRAY1, -1, 1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      // notice that this should even fail when no growth was needed
      Floats.ensureCapacity(ARRAY1, 1, -1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // Float.toString returns different value in GWT.
  public void testJoin() {
    assertThat(Floats.join(",", EMPTY)).isEmpty();
    assertThat(Floats.join(",", ARRAY1)).isEqualTo("1.0");
    assertThat(Floats.join(",", (float) 1, (float) 2)).isEqualTo("1.0,2.0");
    assertThat(Floats.join("", (float) 1, (float) 2, (float) 3)).isEqualTo("1.02.03.0");
  }

  public void testLexicographicalComparator() {
    List<float[]> ordered =
        Arrays.asList(
            new float[] {},
            new float[] {LEAST},
            new float[] {LEAST, LEAST},
            new float[] {LEAST, (float) 1},
            new float[] {(float) 1},
            new float[] {(float) 1, LEAST},
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

    List<Float> one = Arrays.asList((float) 1);
    assertThat(Floats.toArray(one)).isEqualTo(ARRAY1);

    float[] array = {(float) 0, (float) 1, (float) 3};

    List<Float> three = Arrays.asList((float) 0, (float) 1, (float) 3);
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
    List<@Nullable Float> list = Arrays.asList((float) 0, (float) 1, null);
    try {
      Floats.toArray(list);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testToArray_withConversion() {
    float[] array = {(float) 0, (float) 1, (float) 2};

    List<Byte> bytes = Arrays.asList((byte) 0, (byte) 1, (byte) 2);
    List<Short> shorts = Arrays.asList((short) 0, (short) 1, (short) 2);
    List<Integer> ints = Arrays.asList(0, 1, 2);
    List<Float> floats = Arrays.asList((float) 0, (float) 1, (float) 2);
    List<Long> longs = Arrays.asList((long) 0, (long) 1, (long) 2);
    List<Double> doubles = Arrays.asList((double) 0, (double) 1, (double) 2);

    assertThat(Floats.toArray(bytes)).isEqualTo(array);
    assertThat(Floats.toArray(shorts)).isEqualTo(array);
    assertThat(Floats.toArray(ints)).isEqualTo(array);
    assertThat(Floats.toArray(floats)).isEqualTo(array);
    assertThat(Floats.toArray(longs)).isEqualTo(array);
    assertThat(Floats.toArray(doubles)).isEqualTo(array);
  }

  @J2ktIncompatible // b/285319375
  public void testAsList_isAView() {
    float[] array = {(float) 0, (float) 1};
    List<Float> list = Floats.asList(array);
    list.set(0, (float) 2);
    assertThat(array).isEqualTo(new float[] {(float) 2, (float) 1});
    array[1] = (float) 3;
    assertThat(list).containsExactly((float) 2, (float) 3).inOrder();
  }

  public void testAsList_toArray_roundTrip() {
    float[] array = {(float) 0, (float) 1, (float) 2};
    List<Float> list = Floats.asList(array);
    float[] newArray = Floats.toArray(list);

    // Make sure it returned a copy
    list.set(0, (float) 4);
    assertThat(newArray).isEqualTo(new float[] {(float) 0, (float) 1, (float) 2});
    newArray[1] = (float) 5;
    assertThat((float) list.get(1)).isEqualTo((float) 1);
  }

  // This test stems from a real bug found by andrewk
  public void testAsList_subList_toArray_roundTrip() {
    float[] array = {(float) 0, (float) 1, (float) 2, (float) 3};
    List<Float> list = Floats.asList(array);
    assertThat(Floats.toArray(list.subList(1, 3))).isEqualTo(new float[] {(float) 1, (float) 2});
    assertThat(Floats.toArray(list.subList(2, 2))).isEmpty();
  }

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

  @J2ktIncompatible
  @GwtIncompatible // Floats.tryParse
  private static void checkTryParse(String input) {
    assertThat(Floats.tryParse(input)).isEqualTo(referenceTryParse(input));
  }

  @J2ktIncompatible
  @GwtIncompatible // Floats.tryParse
  private static void checkTryParse(float expected, String input) {
    assertThat(Floats.tryParse(input)).isEqualTo(Float.valueOf(expected));
  }

  @J2ktIncompatible
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
  @J2ktIncompatible
  @GwtIncompatible // Floats.tryParse
  public void testTryParseAllCodePoints() {
    // Exercise non-ASCII digit test cases and the like.
    char[] tmp = new char[2];
    for (int i = Character.MIN_CODE_POINT; i < Character.MAX_CODE_POINT; i++) {
      Character.toChars(i, tmp, 0);
      checkTryParse(String.copyValueOf(tmp, 0, Character.charCount(i)));
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // Floats.tryParse
  public void testTryParseOfToStringIsOriginal() {
    for (float f : NUMBERS) {
      checkTryParse(f, Float.toString(f));
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // Floats.tryParse
  public void testTryParseOfToHexStringIsOriginal() {
    for (float f : NUMBERS) {
      checkTryParse(f, Float.toHexString(f));
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // Floats.tryParse
  public void testTryParseNaN() {
    checkTryParse("NaN");
    checkTryParse("+NaN");
    checkTryParse("-NaN");
  }

  @J2ktIncompatible
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

  @J2ktIncompatible
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

  @J2ktIncompatible
  @GwtIncompatible // Float.toString returns different value in GWT.
  public void testStringConverter_convert() {
    Converter<String, Float> converter = Floats.stringConverter();
    assertThat(converter.convert("1.0")).isEqualTo((Float) 1.0f);
    assertThat(converter.convert("0.0")).isEqualTo((Float) 0.0f);
    assertThat(converter.convert("-1.0")).isEqualTo((Float) (-1.0f));
    assertThat(converter.convert("1")).isEqualTo((Float) 1.0f);
    assertThat(converter.convert("0")).isEqualTo((Float) 0.0f);
    assertThat(converter.convert("-1")).isEqualTo((Float) (-1.0f));
    assertThat(converter.convert("1e6")).isEqualTo((Float) 1e6f);
    assertThat(converter.convert("1e-6")).isEqualTo((Float) 1e-6f);
  }

  public void testStringConverter_convertError() {
    try {
      Floats.stringConverter().convert("notanumber");
      fail();
    } catch (NumberFormatException expected) {
    }
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

  @J2ktIncompatible
  @GwtIncompatible
  public void testTryParse_withNullNoGwt() {
    assertThat(Floats.tryParse("null")).isNull();
    try {
      Floats.tryParse(null);
      fail("Expected NPE");
    } catch (NullPointerException expected) {
    }
  }
}

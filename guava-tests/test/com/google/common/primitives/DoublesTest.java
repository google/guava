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
import static java.lang.Double.NaN;

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
import java.util.regex.Pattern;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Unit test for {@link Doubles}.
 *
 * @author Kevin Bourrillion
 */
@ElementTypesAreNonnullByDefault
@GwtCompatible(emulated = true)
public class DoublesTest extends TestCase {
  private static final double[] EMPTY = {};
  private static final double[] ARRAY1 = {(double) 1};
  private static final double[] ARRAY234 = {(double) 2, (double) 3, (double) 4};

  private static final double LEAST = Double.NEGATIVE_INFINITY;
  private static final double GREATEST = Double.POSITIVE_INFINITY;

  private static final double[] NUMBERS =
      new double[] {
        LEAST,
        -Double.MAX_VALUE,
        -1.0,
        -0.5,
        -0.1,
        -0.0,
        0.0,
        0.1,
        0.5,
        1.0,
        Double.MAX_VALUE,
        GREATEST,
        Double.MIN_NORMAL,
        -Double.MIN_NORMAL,
        Double.MIN_VALUE,
        -Double.MIN_VALUE,
        Integer.MIN_VALUE,
        Integer.MAX_VALUE,
        Long.MIN_VALUE,
        Long.MAX_VALUE
      };

  private static final double[] VALUES = Doubles.concat(NUMBERS, new double[] {NaN});

  public void testHashCode() {
    for (double value : VALUES) {
      assertThat(Doubles.hashCode(value)).isEqualTo(((Double) value).hashCode());
    }
  }

  public void testIsFinite() {
    for (double value : NUMBERS) {
      assertThat(Doubles.isFinite(value))
          .isEqualTo(!(Double.isNaN(value) || Double.isInfinite(value)));
    }
  }

  public void testCompare() {
    for (double x : VALUES) {
      for (double y : VALUES) {
        // note: spec requires only that the sign is the same
        assertWithMessage(x + ", " + y)
            .that(Doubles.compare(x, y))
            .isEqualTo(Double.valueOf(x).compareTo(y));
      }
    }
  }

  public void testContains() {
    assertThat(Doubles.contains(EMPTY, (double) 1)).isFalse();
    assertThat(Doubles.contains(ARRAY1, (double) 2)).isFalse();
    assertThat(Doubles.contains(ARRAY234, (double) 1)).isFalse();
    assertThat(Doubles.contains(new double[] {(double) -1}, (double) -1)).isTrue();
    assertThat(Doubles.contains(ARRAY234, (double) 2)).isTrue();
    assertThat(Doubles.contains(ARRAY234, (double) 3)).isTrue();
    assertThat(Doubles.contains(ARRAY234, (double) 4)).isTrue();

    for (double value : NUMBERS) {
      assertWithMessage("" + value)
          .that(Doubles.contains(new double[] {5.0, value}, value))
          .isTrue();
    }
    assertThat(Doubles.contains(new double[] {5.0, NaN}, NaN)).isFalse();
  }

  public void testIndexOf() {
    assertThat(Doubles.indexOf(EMPTY, (double) 1)).isEqualTo(-1);
    assertThat(Doubles.indexOf(ARRAY1, (double) 2)).isEqualTo(-1);
    assertThat(Doubles.indexOf(ARRAY234, (double) 1)).isEqualTo(-1);
    assertThat(Doubles.indexOf(new double[] {(double) -1}, (double) -1)).isEqualTo(0);
    assertThat(Doubles.indexOf(ARRAY234, (double) 2)).isEqualTo(0);
    assertThat(Doubles.indexOf(ARRAY234, (double) 3)).isEqualTo(1);
    assertThat(Doubles.indexOf(ARRAY234, (double) 4)).isEqualTo(2);
    assertThat(
            Doubles.indexOf(
                new double[] {(double) 2, (double) 3, (double) 2, (double) 3}, (double) 3))
        .isEqualTo(1);

    for (double value : NUMBERS) {
      assertWithMessage("" + value)
          .that(Doubles.indexOf(new double[] {5.0, value}, value))
          .isEqualTo(1);
    }
    assertThat(Doubles.indexOf(new double[] {5.0, NaN}, NaN)).isEqualTo(-1);
  }

  public void testIndexOf_arrayTarget() {
    assertThat(Doubles.indexOf(EMPTY, EMPTY)).isEqualTo(0);
    assertThat(Doubles.indexOf(ARRAY234, EMPTY)).isEqualTo(0);
    assertThat(Doubles.indexOf(EMPTY, ARRAY234)).isEqualTo(-1);
    assertThat(Doubles.indexOf(ARRAY234, ARRAY1)).isEqualTo(-1);
    assertThat(Doubles.indexOf(ARRAY1, ARRAY234)).isEqualTo(-1);
    assertThat(Doubles.indexOf(ARRAY1, ARRAY1)).isEqualTo(0);
    assertThat(Doubles.indexOf(ARRAY234, ARRAY234)).isEqualTo(0);
    assertThat(Doubles.indexOf(ARRAY234, new double[] {(double) 2, (double) 3})).isEqualTo(0);
    assertThat(Doubles.indexOf(ARRAY234, new double[] {(double) 3, (double) 4})).isEqualTo(1);
    assertThat(Doubles.indexOf(ARRAY234, new double[] {(double) 3})).isEqualTo(1);
    assertThat(Doubles.indexOf(ARRAY234, new double[] {(double) 4})).isEqualTo(2);
    assertThat(
            Doubles.indexOf(
                new double[] {(double) 2, (double) 3, (double) 3, (double) 3, (double) 3},
                new double[] {(double) 3}))
        .isEqualTo(1);
    assertThat(
            Doubles.indexOf(
                new double[] {
                  (double) 2, (double) 3, (double) 2, (double) 3, (double) 4, (double) 2, (double) 3
                },
                new double[] {(double) 2, (double) 3, (double) 4}))
        .isEqualTo(2);
    assertThat(
            Doubles.indexOf(
                new double[] {
                  (double) 2, (double) 2, (double) 3, (double) 4, (double) 2, (double) 3, (double) 4
                },
                new double[] {(double) 2, (double) 3, (double) 4}))
        .isEqualTo(1);
    assertThat(
            Doubles.indexOf(
                new double[] {(double) 4, (double) 3, (double) 2},
                new double[] {(double) 2, (double) 3, (double) 4}))
        .isEqualTo(-1);

    for (double value : NUMBERS) {
      assertWithMessage("" + value)
          .that(Doubles.indexOf(new double[] {5.0, value, value, 5.0}, new double[] {value, value}))
          .isEqualTo(1);
    }
    assertThat(Doubles.indexOf(new double[] {5.0, NaN, NaN, 5.0}, new double[] {NaN, NaN}))
        .isEqualTo(-1);
  }

  public void testLastIndexOf() {
    assertThat(Doubles.lastIndexOf(EMPTY, (double) 1)).isEqualTo(-1);
    assertThat(Doubles.lastIndexOf(ARRAY1, (double) 2)).isEqualTo(-1);
    assertThat(Doubles.lastIndexOf(ARRAY234, (double) 1)).isEqualTo(-1);
    assertThat(Doubles.lastIndexOf(new double[] {(double) -1}, (double) -1)).isEqualTo(0);
    assertThat(Doubles.lastIndexOf(ARRAY234, (double) 2)).isEqualTo(0);
    assertThat(Doubles.lastIndexOf(ARRAY234, (double) 3)).isEqualTo(1);
    assertThat(Doubles.lastIndexOf(ARRAY234, (double) 4)).isEqualTo(2);
    assertThat(
            Doubles.lastIndexOf(
                new double[] {(double) 2, (double) 3, (double) 2, (double) 3}, (double) 3))
        .isEqualTo(3);

    for (double value : NUMBERS) {
      assertWithMessage("" + value)
          .that(Doubles.lastIndexOf(new double[] {value, 5.0}, value))
          .isEqualTo(0);
    }
    assertThat(Doubles.lastIndexOf(new double[] {NaN, 5.0}, NaN)).isEqualTo(-1);
  }

  @J2ktIncompatible
  @GwtIncompatible
  public void testMax_noArgs() {
    try {
      Doubles.max();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMax() {
    assertThat(Doubles.max(LEAST)).isEqualTo(LEAST);
    assertThat(Doubles.max(GREATEST)).isEqualTo(GREATEST);
    assertThat(
            Doubles.max(
                (double) 8, (double) 6, (double) 7, (double) 5, (double) 3, (double) 0, (double) 9))
        .isEqualTo((double) 9);

    assertThat(Doubles.max(-0.0, 0.0)).isEqualTo(0.0);
    assertThat(Doubles.max(0.0, -0.0)).isEqualTo(0.0);
    assertThat(Doubles.max(NUMBERS)).isEqualTo(GREATEST);
    assertThat(Double.isNaN(Doubles.max(VALUES))).isTrue();
  }

  @J2ktIncompatible
  @GwtIncompatible
  public void testMin_noArgs() {
    try {
      Doubles.min();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMin() {
    assertThat(Doubles.min(LEAST)).isEqualTo(LEAST);
    assertThat(Doubles.min(GREATEST)).isEqualTo(GREATEST);
    assertThat(
            Doubles.min(
                (double) 8, (double) 6, (double) 7, (double) 5, (double) 3, (double) 0, (double) 9))
        .isEqualTo((double) 0);

    assertThat(Doubles.min(-0.0, 0.0)).isEqualTo(-0.0);
    assertThat(Doubles.min(0.0, -0.0)).isEqualTo(-0.0);
    assertThat(Doubles.min(NUMBERS)).isEqualTo(LEAST);
    assertThat(Double.isNaN(Doubles.min(VALUES))).isTrue();
  }

  public void testConstrainToRange() {
    assertThat(Doubles.constrainToRange((double) 1, (double) 0, (double) 5)).isEqualTo((double) 1);
    assertThat(Doubles.constrainToRange((double) 1, (double) 1, (double) 5)).isEqualTo((double) 1);
    assertThat(Doubles.constrainToRange((double) 1, (double) 3, (double) 5)).isEqualTo((double) 3);
    assertThat(Doubles.constrainToRange((double) 0, (double) -5, (double) -1))
        .isEqualTo((double) -1);
    assertThat(Doubles.constrainToRange((double) 5, (double) 2, (double) 2)).isEqualTo((double) 2);
    try {
      Doubles.constrainToRange((double) 1, (double) 3, (double) 2);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testConcat() {
    assertThat(Doubles.concat()).isEqualTo(EMPTY);
    assertThat(Doubles.concat(EMPTY)).isEqualTo(EMPTY);
    assertThat(Doubles.concat(EMPTY, EMPTY, EMPTY)).isEqualTo(EMPTY);
    assertThat(Doubles.concat(ARRAY1)).isEqualTo(ARRAY1);
    assertThat(Doubles.concat(ARRAY1)).isNotSameInstanceAs(ARRAY1);
    assertThat(Doubles.concat(EMPTY, ARRAY1, EMPTY)).isEqualTo(ARRAY1);
    assertThat(Doubles.concat(ARRAY1, ARRAY1, ARRAY1))
        .isEqualTo(new double[] {(double) 1, (double) 1, (double) 1});
    assertThat(Doubles.concat(ARRAY1, ARRAY234))
        .isEqualTo(new double[] {(double) 1, (double) 2, (double) 3, (double) 4});
  }

  public void testEnsureCapacity() {
    assertThat(Doubles.ensureCapacity(EMPTY, 0, 1)).isSameInstanceAs(EMPTY);
    assertThat(Doubles.ensureCapacity(ARRAY1, 0, 1)).isSameInstanceAs(ARRAY1);
    assertThat(Doubles.ensureCapacity(ARRAY1, 1, 1)).isSameInstanceAs(ARRAY1);
    assertThat(
            Arrays.equals(
                new double[] {(double) 1, (double) 0, (double) 0},
                Doubles.ensureCapacity(ARRAY1, 2, 1)))
        .isTrue();
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

  @J2ktIncompatible
  @GwtIncompatible // Double.toString returns different value in GWT.
  public void testJoin() {
    assertThat(Doubles.join(",", EMPTY)).isEmpty();
    assertThat(Doubles.join(",", ARRAY1)).isEqualTo("1.0");
    assertThat(Doubles.join(",", (double) 1, (double) 2)).isEqualTo("1.0,2.0");
    assertThat(Doubles.join("", (double) 1, (double) 2, (double) 3)).isEqualTo("1.02.03.0");
  }

  public void testJoinNonTrivialDoubles() {
    assertThat(Doubles.join(",", EMPTY)).isEmpty();
    assertThat(Doubles.join(",", 1.2)).isEqualTo("1.2");
    assertThat(Doubles.join(",", 1.3, 2.4)).isEqualTo("1.3,2.4");
    assertThat(Doubles.join("", 1.4, 2.5, 3.6)).isEqualTo("1.42.53.6");
  }

  public void testLexicographicalComparator() {
    List<double[]> ordered =
        Arrays.asList(
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

  public void testReverse() {
    testReverse(new double[] {}, new double[] {});
    testReverse(new double[] {1}, new double[] {1});
    testReverse(new double[] {1, 2}, new double[] {2, 1});
    testReverse(new double[] {3, 1, 1}, new double[] {1, 1, 3});
    testReverse(new double[] {-1, 1, -2, 2}, new double[] {2, -2, 1, -1});
  }

  private static void testReverse(double[] input, double[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Doubles.reverse(input);
    assertThat(input).isEqualTo(expectedOutput);
  }

  private static void testReverse(
      double[] input, int fromIndex, int toIndex, double[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Doubles.reverse(input, fromIndex, toIndex);
    assertThat(input).isEqualTo(expectedOutput);
  }

  public void testReverseIndexed() {
    testReverse(new double[] {}, 0, 0, new double[] {});
    testReverse(new double[] {1}, 0, 1, new double[] {1});
    testReverse(new double[] {1, 2}, 0, 2, new double[] {2, 1});
    testReverse(new double[] {3, 1, 1}, 0, 2, new double[] {1, 3, 1});
    testReverse(new double[] {3, 1, 1}, 0, 1, new double[] {3, 1, 1});
    testReverse(new double[] {-1, 1, -2, 2}, 1, 3, new double[] {-1, -2, 1, 2});
  }

  private static void testRotate(double[] input, int distance, double[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Doubles.rotate(input, distance);
    assertThat(input).isEqualTo(expectedOutput);
  }

  private static void testRotate(
      double[] input, int distance, int fromIndex, int toIndex, double[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Doubles.rotate(input, distance, fromIndex, toIndex);
    assertThat(input).isEqualTo(expectedOutput);
  }

  public void testRotate() {
    testRotate(new double[] {}, -1, new double[] {});
    testRotate(new double[] {}, 0, new double[] {});
    testRotate(new double[] {}, 1, new double[] {});

    testRotate(new double[] {1}, -2, new double[] {1});
    testRotate(new double[] {1}, -1, new double[] {1});
    testRotate(new double[] {1}, 0, new double[] {1});
    testRotate(new double[] {1}, 1, new double[] {1});
    testRotate(new double[] {1}, 2, new double[] {1});

    testRotate(new double[] {1, 2}, -3, new double[] {2, 1});
    testRotate(new double[] {1, 2}, -1, new double[] {2, 1});
    testRotate(new double[] {1, 2}, -2, new double[] {1, 2});
    testRotate(new double[] {1, 2}, 0, new double[] {1, 2});
    testRotate(new double[] {1, 2}, 1, new double[] {2, 1});
    testRotate(new double[] {1, 2}, 2, new double[] {1, 2});
    testRotate(new double[] {1, 2}, 3, new double[] {2, 1});

    testRotate(new double[] {1, 2, 3}, -5, new double[] {3, 1, 2});
    testRotate(new double[] {1, 2, 3}, -4, new double[] {2, 3, 1});
    testRotate(new double[] {1, 2, 3}, -3, new double[] {1, 2, 3});
    testRotate(new double[] {1, 2, 3}, -2, new double[] {3, 1, 2});
    testRotate(new double[] {1, 2, 3}, -1, new double[] {2, 3, 1});
    testRotate(new double[] {1, 2, 3}, 0, new double[] {1, 2, 3});
    testRotate(new double[] {1, 2, 3}, 1, new double[] {3, 1, 2});
    testRotate(new double[] {1, 2, 3}, 2, new double[] {2, 3, 1});
    testRotate(new double[] {1, 2, 3}, 3, new double[] {1, 2, 3});
    testRotate(new double[] {1, 2, 3}, 4, new double[] {3, 1, 2});
    testRotate(new double[] {1, 2, 3}, 5, new double[] {2, 3, 1});

    testRotate(new double[] {1, 2, 3, 4}, -9, new double[] {2, 3, 4, 1});
    testRotate(new double[] {1, 2, 3, 4}, -5, new double[] {2, 3, 4, 1});
    testRotate(new double[] {1, 2, 3, 4}, -1, new double[] {2, 3, 4, 1});
    testRotate(new double[] {1, 2, 3, 4}, 0, new double[] {1, 2, 3, 4});
    testRotate(new double[] {1, 2, 3, 4}, 1, new double[] {4, 1, 2, 3});
    testRotate(new double[] {1, 2, 3, 4}, 5, new double[] {4, 1, 2, 3});
    testRotate(new double[] {1, 2, 3, 4}, 9, new double[] {4, 1, 2, 3});

    testRotate(new double[] {1, 2, 3, 4, 5}, -6, new double[] {2, 3, 4, 5, 1});
    testRotate(new double[] {1, 2, 3, 4, 5}, -4, new double[] {5, 1, 2, 3, 4});
    testRotate(new double[] {1, 2, 3, 4, 5}, -3, new double[] {4, 5, 1, 2, 3});
    testRotate(new double[] {1, 2, 3, 4, 5}, -1, new double[] {2, 3, 4, 5, 1});
    testRotate(new double[] {1, 2, 3, 4, 5}, 0, new double[] {1, 2, 3, 4, 5});
    testRotate(new double[] {1, 2, 3, 4, 5}, 1, new double[] {5, 1, 2, 3, 4});
    testRotate(new double[] {1, 2, 3, 4, 5}, 3, new double[] {3, 4, 5, 1, 2});
    testRotate(new double[] {1, 2, 3, 4, 5}, 4, new double[] {2, 3, 4, 5, 1});
    testRotate(new double[] {1, 2, 3, 4, 5}, 6, new double[] {5, 1, 2, 3, 4});
  }

  public void testRotateIndexed() {
    testRotate(new double[] {}, 0, 0, 0, new double[] {});

    testRotate(new double[] {1}, 0, 0, 1, new double[] {1});
    testRotate(new double[] {1}, 1, 0, 1, new double[] {1});
    testRotate(new double[] {1}, 1, 1, 1, new double[] {1});

    // Rotate the central 5 elements, leaving the ends as-is
    testRotate(new double[] {0, 1, 2, 3, 4, 5, 6}, -6, 1, 6, new double[] {0, 2, 3, 4, 5, 1, 6});
    testRotate(new double[] {0, 1, 2, 3, 4, 5, 6}, -1, 1, 6, new double[] {0, 2, 3, 4, 5, 1, 6});
    testRotate(new double[] {0, 1, 2, 3, 4, 5, 6}, 0, 1, 6, new double[] {0, 1, 2, 3, 4, 5, 6});
    testRotate(new double[] {0, 1, 2, 3, 4, 5, 6}, 5, 1, 6, new double[] {0, 1, 2, 3, 4, 5, 6});
    testRotate(new double[] {0, 1, 2, 3, 4, 5, 6}, 14, 1, 6, new double[] {0, 2, 3, 4, 5, 1, 6});

    // Rotate the first three elements
    testRotate(new double[] {0, 1, 2, 3, 4, 5, 6}, -2, 0, 3, new double[] {2, 0, 1, 3, 4, 5, 6});
    testRotate(new double[] {0, 1, 2, 3, 4, 5, 6}, -1, 0, 3, new double[] {1, 2, 0, 3, 4, 5, 6});
    testRotate(new double[] {0, 1, 2, 3, 4, 5, 6}, 0, 0, 3, new double[] {0, 1, 2, 3, 4, 5, 6});
    testRotate(new double[] {0, 1, 2, 3, 4, 5, 6}, 1, 0, 3, new double[] {2, 0, 1, 3, 4, 5, 6});
    testRotate(new double[] {0, 1, 2, 3, 4, 5, 6}, 2, 0, 3, new double[] {1, 2, 0, 3, 4, 5, 6});

    // Rotate the last four elements
    testRotate(new double[] {0, 1, 2, 3, 4, 5, 6}, -6, 3, 7, new double[] {0, 1, 2, 5, 6, 3, 4});
    testRotate(new double[] {0, 1, 2, 3, 4, 5, 6}, -5, 3, 7, new double[] {0, 1, 2, 4, 5, 6, 3});
    testRotate(new double[] {0, 1, 2, 3, 4, 5, 6}, -4, 3, 7, new double[] {0, 1, 2, 3, 4, 5, 6});
    testRotate(new double[] {0, 1, 2, 3, 4, 5, 6}, -3, 3, 7, new double[] {0, 1, 2, 6, 3, 4, 5});
    testRotate(new double[] {0, 1, 2, 3, 4, 5, 6}, -2, 3, 7, new double[] {0, 1, 2, 5, 6, 3, 4});
    testRotate(new double[] {0, 1, 2, 3, 4, 5, 6}, -1, 3, 7, new double[] {0, 1, 2, 4, 5, 6, 3});
    testRotate(new double[] {0, 1, 2, 3, 4, 5, 6}, 0, 3, 7, new double[] {0, 1, 2, 3, 4, 5, 6});
    testRotate(new double[] {0, 1, 2, 3, 4, 5, 6}, 1, 3, 7, new double[] {0, 1, 2, 6, 3, 4, 5});
    testRotate(new double[] {0, 1, 2, 3, 4, 5, 6}, 2, 3, 7, new double[] {0, 1, 2, 5, 6, 3, 4});
    testRotate(new double[] {0, 1, 2, 3, 4, 5, 6}, 3, 3, 7, new double[] {0, 1, 2, 4, 5, 6, 3});
  }

  public void testSortDescending() {
    testSortDescending(new double[] {}, new double[] {});
    testSortDescending(new double[] {1}, new double[] {1});
    testSortDescending(new double[] {1, 2}, new double[] {2, 1});
    testSortDescending(new double[] {1, 3, 1}, new double[] {3, 1, 1});
    testSortDescending(new double[] {-1, 1, -2, 2}, new double[] {2, 1, -1, -2});
    testSortDescending(
        new double[] {-1, 1, Double.NaN, -2, -0.0, 0, 2},
        new double[] {Double.NaN, 2, 1, 0, -0.0, -1, -2});
  }

  private static void testSortDescending(double[] input, double[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Doubles.sortDescending(input);
    for (int i = 0; i < input.length; i++) {
      assertThat(input[i]).isEqualTo(expectedOutput[i]);
    }
  }

  private static void testSortDescending(
      double[] input, int fromIndex, int toIndex, double[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Doubles.sortDescending(input, fromIndex, toIndex);
    for (int i = 0; i < input.length; i++) {
      assertThat(input[i]).isEqualTo(expectedOutput[i]);
    }
  }

  public void testSortDescendingIndexed() {
    testSortDescending(new double[] {}, 0, 0, new double[] {});
    testSortDescending(new double[] {1}, 0, 1, new double[] {1});
    testSortDescending(new double[] {1, 2}, 0, 2, new double[] {2, 1});
    testSortDescending(new double[] {1, 3, 1}, 0, 2, new double[] {3, 1, 1});
    testSortDescending(new double[] {1, 3, 1}, 0, 1, new double[] {1, 3, 1});
    testSortDescending(new double[] {-1, -2, 1, 2}, 1, 3, new double[] {-1, 1, -2, 2});
    testSortDescending(
        new double[] {-1, 1, Double.NaN, -2, 2}, 1, 4, new double[] {-1, Double.NaN, 1, -2, 2});
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testLexicographicalComparatorSerializable() {
    Comparator<double[]> comparator = Doubles.lexicographicalComparator();
    assertThat(SerializableTester.reserialize(comparator)).isSameInstanceAs(comparator);
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testStringConverterSerialization() {
    SerializableTester.reserializeAndAssert(Doubles.stringConverter());
  }

  public void testToArray() {
    // need explicit type parameter to avoid javac warning!?
    List<Double> none = Arrays.<Double>asList();
    assertThat(Doubles.toArray(none)).isEqualTo(EMPTY);

    List<Double> one = Arrays.asList((double) 1);
    assertThat(Doubles.toArray(one)).isEqualTo(ARRAY1);

    double[] array = {(double) 0, (double) 1, Math.PI};

    List<Double> three = Arrays.asList((double) 0, (double) 1, Math.PI);
    assertThat(Doubles.toArray(three)).isEqualTo(array);

    assertThat(Doubles.toArray(Doubles.asList(array))).isEqualTo(array);
  }

  public void testToArray_threadSafe() {
    for (int delta : new int[] {+1, 0, -1}) {
      for (int i = 0; i < VALUES.length; i++) {
        List<Double> list = Doubles.asList(VALUES).subList(0, i);
        Collection<Double> misleadingSize = Helpers.misleadingSizeCollection(delta);
        misleadingSize.addAll(list);
        double[] arr = Doubles.toArray(misleadingSize);
        assertThat(arr.length).isEqualTo(i);
        for (int j = 0; j < i; j++) {
          assertThat(arr[j]).isEqualTo(VALUES[j]);
        }
      }
    }
  }

  public void testToArray_withNull() {
    List<@Nullable Double> list = Arrays.asList((double) 0, (double) 1, null);
    try {
      Doubles.toArray(list);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testToArray_withConversion() {
    double[] array = {(double) 0, (double) 1, (double) 2};

    List<Byte> bytes = Arrays.asList((byte) 0, (byte) 1, (byte) 2);
    List<Short> shorts = Arrays.asList((short) 0, (short) 1, (short) 2);
    List<Integer> ints = Arrays.asList(0, 1, 2);
    List<Float> floats = Arrays.asList((float) 0, (float) 1, (float) 2);
    List<Long> longs = Arrays.asList((long) 0, (long) 1, (long) 2);
    List<Double> doubles = Arrays.asList((double) 0, (double) 1, (double) 2);

    assertThat(Doubles.toArray(bytes)).isEqualTo(array);
    assertThat(Doubles.toArray(shorts)).isEqualTo(array);
    assertThat(Doubles.toArray(ints)).isEqualTo(array);
    assertThat(Doubles.toArray(floats)).isEqualTo(array);
    assertThat(Doubles.toArray(longs)).isEqualTo(array);
    assertThat(Doubles.toArray(doubles)).isEqualTo(array);
  }

  @J2ktIncompatible // b/285319375
  public void testAsList_isAView() {
    double[] array = {(double) 0, (double) 1};
    List<Double> list = Doubles.asList(array);
    list.set(0, (double) 2);
    assertThat(array).isEqualTo(new double[] {(double) 2, (double) 1});
    array[1] = (double) 3;
    assertThat(list).containsExactly((double) 2, (double) 3).inOrder();
  }

  public void testAsList_toArray_roundTrip() {
    double[] array = {(double) 0, (double) 1, (double) 2};
    List<Double> list = Doubles.asList(array);
    double[] newArray = Doubles.toArray(list);

    // Make sure it returned a copy
    list.set(0, (double) 4);
    assertThat(newArray).isEqualTo(new double[] {(double) 0, (double) 1, (double) 2});
    newArray[1] = (double) 5;
    assertThat((double) list.get(1)).isEqualTo((double) 1);
  }

  // This test stems from a real bug found by andrewk
  public void testAsList_subList_toArray_roundTrip() {
    double[] array = {(double) 0, (double) 1, (double) 2, (double) 3};
    List<Double> list = Doubles.asList(array);
    assertThat(Doubles.toArray(list.subList(1, 3)))
        .isEqualTo(new double[] {(double) 1, (double) 2});
    assertThat(Doubles.toArray(list.subList(2, 2))).isEmpty();
  }

  public void testAsListEmpty() {
    assertThat(Doubles.asList(EMPTY)).isSameInstanceAs(Collections.emptyList());
  }

  /**
   * A reference implementation for {@code tryParse} that just catches the exception from {@link
   * Double#valueOf}.
   */
  private static @Nullable Double referenceTryParse(String input) {
    if (input.trim().length() < input.length()) {
      return null;
    }
    try {
      return Double.valueOf(input);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // Doubles.tryParse
  private static void checkTryParse(String input) {
    Double expected = referenceTryParse(input);
    assertThat(Doubles.tryParse(input)).isEqualTo(expected);
    if (expected != null && !Doubles.FLOATING_POINT_PATTERN.matcher(input).matches()) {
      // TODO(cpovirk): Use SourceCodeEscapers if it is added to Guava.
      StringBuilder escapedInput = new StringBuilder();
      for (char c : input.toCharArray()) {
        if (c >= 0x20 && c <= 0x7E) {
          escapedInput.append(c);
        } else {
          escapedInput.append(String.format("\\u%04x", (int) c));
        }
      }
      fail("FLOATING_POINT_PATTERN should have matched valid input <" + escapedInput + ">");
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // Doubles.tryParse
  private static void checkTryParse(double expected, String input) {
    assertThat(Doubles.tryParse(input)).isEqualTo(Double.valueOf(expected));
    assertThat(input)
        .matches(
            Pattern.compile(
                Doubles.FLOATING_POINT_PATTERN.pattern(), Doubles.FLOATING_POINT_PATTERN.flags()));
  }

  @J2ktIncompatible
  @GwtIncompatible // Doubles.tryParse
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
  @GwtIncompatible // Doubles.tryParse
  public void testTryParseAllCodePoints() {
    // Exercise non-ASCII digit test cases and the like.
    char[] tmp = new char[2];
    for (int i = Character.MIN_CODE_POINT; i < Character.MAX_CODE_POINT; i++) {
      Character.toChars(i, tmp, 0);
      checkTryParse(String.copyValueOf(tmp, 0, Character.charCount(i)));
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // Doubles.tryParse
  public void testTryParseOfToStringIsOriginal() {
    for (double d : NUMBERS) {
      checkTryParse(d, Double.toString(d));
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // Doubles.tryParse
  public void testTryParseOfToHexStringIsOriginal() {
    for (double d : NUMBERS) {
      checkTryParse(d, Double.toHexString(d));
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // Doubles.tryParse
  public void testTryParseNaN() {
    checkTryParse("NaN");
    checkTryParse("+NaN");
    checkTryParse("-NaN");
  }

  @J2ktIncompatible
  @GwtIncompatible // Doubles.tryParse
  public void testTryParseInfinity() {
    checkTryParse(Double.POSITIVE_INFINITY, "Infinity");
    checkTryParse(Double.POSITIVE_INFINITY, "+Infinity");
    checkTryParse(Double.NEGATIVE_INFINITY, "-Infinity");
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
  @GwtIncompatible // Doubles.tryParse
  public void testTryParseFailures() {
    for (String badInput : BAD_TRY_PARSE_INPUTS) {
      assertThat(badInput)
          .doesNotMatch(
              Pattern.compile(
                  Doubles.FLOATING_POINT_PATTERN.pattern(),
                  Doubles.FLOATING_POINT_PATTERN.flags()));
      assertThat(Doubles.tryParse(badInput)).isEqualTo(referenceTryParse(badInput));
      assertThat(Doubles.tryParse(badInput)).isNull();
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testNulls() {
    new NullPointerTester().testAllPublicStaticMethods(Doubles.class);
  }

  public void testStringConverter_convert() {
    Converter<String, Double> converter = Doubles.stringConverter();
    assertThat(converter.convert("1.0")).isEqualTo((Double) 1.0);
    assertThat(converter.convert("0.0")).isEqualTo((Double) 0.0);
    assertThat(converter.convert("-1.0")).isEqualTo((Double) (-1.0));
    assertThat(converter.convert("1")).isEqualTo((Double) 1.0);
    assertThat(converter.convert("0")).isEqualTo((Double) 0.0);
    assertThat(converter.convert("-1")).isEqualTo((Double) (-1.0));
    assertThat(converter.convert("1e6")).isEqualTo((Double) 1e6);
    assertThat(converter.convert("1e-6")).isEqualTo((Double) 1e-6);
  }

  public void testStringConverter_convertError() {
    try {
      Doubles.stringConverter().convert("notanumber");
      fail();
    } catch (NumberFormatException expected) {
    }
  }

  public void testStringConverter_nullConversions() {
    assertThat(Doubles.stringConverter().convert(null)).isNull();
    assertThat(Doubles.stringConverter().reverse().convert(null)).isNull();
  }

  @J2ktIncompatible
  @GwtIncompatible // Double.toString returns different value in GWT.
  public void testStringConverter_reverse() {
    Converter<String, Double> converter = Doubles.stringConverter();
    assertThat(converter.reverse().convert(1.0)).isEqualTo("1.0");
    assertThat(converter.reverse().convert(0.0)).isEqualTo("0.0");
    assertThat(converter.reverse().convert(-1.0)).isEqualTo("-1.0");
    assertThat(converter.reverse().convert(1e6)).isEqualTo("1000000.0");
    assertThat(converter.reverse().convert(1e-6)).isEqualTo("1.0E-6");
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testStringConverter_nullPointerTester() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicInstanceMethods(Doubles.stringConverter());
  }

  @J2ktIncompatible
  @GwtIncompatible
  public void testTryParse_withNullNoGwt() {
    assertThat(Doubles.tryParse("null")).isNull();
    try {
      Doubles.tryParse(null);
      fail("Expected NPE");
    } catch (NullPointerException expected) {
    }
  }
}

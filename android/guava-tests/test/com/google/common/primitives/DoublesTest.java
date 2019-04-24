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
import static java.lang.Double.NaN;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
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
        assertEquals(x + ", " + y, Double.valueOf(x).compareTo(y), Doubles.compare(x, y));
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
      assertTrue("" + value, Doubles.contains(new double[] {5.0, value}, value));
    }
    assertFalse(Doubles.contains(new double[] {5.0, NaN}, NaN));
  }

  public void testIndexOf() {
    assertEquals(-1, Doubles.indexOf(EMPTY, (double) 1));
    assertEquals(-1, Doubles.indexOf(ARRAY1, (double) 2));
    assertEquals(-1, Doubles.indexOf(ARRAY234, (double) 1));
    assertEquals(0, Doubles.indexOf(new double[] {(double) -1}, (double) -1));
    assertEquals(0, Doubles.indexOf(ARRAY234, (double) 2));
    assertEquals(1, Doubles.indexOf(ARRAY234, (double) 3));
    assertEquals(2, Doubles.indexOf(ARRAY234, (double) 4));
    assertEquals(
        1,
        Doubles.indexOf(new double[] {(double) 2, (double) 3, (double) 2, (double) 3}, (double) 3));

    for (double value : NUMBERS) {
      assertEquals("" + value, 1, Doubles.indexOf(new double[] {5.0, value}, value));
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
    assertEquals(0, Doubles.indexOf(ARRAY234, new double[] {(double) 2, (double) 3}));
    assertEquals(1, Doubles.indexOf(ARRAY234, new double[] {(double) 3, (double) 4}));
    assertEquals(1, Doubles.indexOf(ARRAY234, new double[] {(double) 3}));
    assertEquals(2, Doubles.indexOf(ARRAY234, new double[] {(double) 4}));
    assertEquals(
        1,
        Doubles.indexOf(
            new double[] {(double) 2, (double) 3, (double) 3, (double) 3, (double) 3},
            new double[] {(double) 3}));
    assertEquals(
        2,
        Doubles.indexOf(
            new double[] {
              (double) 2, (double) 3, (double) 2, (double) 3, (double) 4, (double) 2, (double) 3
            },
            new double[] {(double) 2, (double) 3, (double) 4}));
    assertEquals(
        1,
        Doubles.indexOf(
            new double[] {
              (double) 2, (double) 2, (double) 3, (double) 4, (double) 2, (double) 3, (double) 4
            },
            new double[] {(double) 2, (double) 3, (double) 4}));
    assertEquals(
        -1,
        Doubles.indexOf(
            new double[] {(double) 4, (double) 3, (double) 2},
            new double[] {(double) 2, (double) 3, (double) 4}));

    for (double value : NUMBERS) {
      assertEquals(
          "" + value,
          1,
          Doubles.indexOf(new double[] {5.0, value, value, 5.0}, new double[] {value, value}));
    }
    assertEquals(-1, Doubles.indexOf(new double[] {5.0, NaN, NaN, 5.0}, new double[] {NaN, NaN}));
  }

  public void testLastIndexOf() {
    assertEquals(-1, Doubles.lastIndexOf(EMPTY, (double) 1));
    assertEquals(-1, Doubles.lastIndexOf(ARRAY1, (double) 2));
    assertEquals(-1, Doubles.lastIndexOf(ARRAY234, (double) 1));
    assertEquals(0, Doubles.lastIndexOf(new double[] {(double) -1}, (double) -1));
    assertEquals(0, Doubles.lastIndexOf(ARRAY234, (double) 2));
    assertEquals(1, Doubles.lastIndexOf(ARRAY234, (double) 3));
    assertEquals(2, Doubles.lastIndexOf(ARRAY234, (double) 4));
    assertEquals(
        3,
        Doubles.lastIndexOf(
            new double[] {(double) 2, (double) 3, (double) 2, (double) 3}, (double) 3));

    for (double value : NUMBERS) {
      assertEquals("" + value, 0, Doubles.lastIndexOf(new double[] {value, 5.0}, value));
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
    assertEquals(
        (double) 9,
        Doubles.max(
            (double) 8, (double) 6, (double) 7, (double) 5, (double) 3, (double) 0, (double) 9));

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
    assertEquals(
        (double) 0,
        Doubles.min(
            (double) 8, (double) 6, (double) 7, (double) 5, (double) 3, (double) 0, (double) 9));

    assertEquals(-0.0, Doubles.min(-0.0, 0.0));
    assertEquals(-0.0, Doubles.min(0.0, -0.0));
    assertEquals(LEAST, Doubles.min(NUMBERS));
    assertTrue(Double.isNaN(Doubles.min(VALUES)));
  }

  public void testConstrainToRange() {
    double tolerance = 1e-10;
    assertEquals(
        (double) 1, Doubles.constrainToRange((double) 1, (double) 0, (double) 5), tolerance);
    assertEquals(
        (double) 1, Doubles.constrainToRange((double) 1, (double) 1, (double) 5), tolerance);
    assertEquals(
        (double) 3, Doubles.constrainToRange((double) 1, (double) 3, (double) 5), tolerance);
    assertEquals(
        (double) -1, Doubles.constrainToRange((double) 0, (double) -5, (double) -1), tolerance);
    assertEquals(
        (double) 2, Doubles.constrainToRange((double) 5, (double) 2, (double) 2), tolerance);
    try {
      Doubles.constrainToRange((double) 1, (double) 3, (double) 2);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testConcat() {
    assertTrue(Arrays.equals(EMPTY, Doubles.concat()));
    assertTrue(Arrays.equals(EMPTY, Doubles.concat(EMPTY)));
    assertTrue(Arrays.equals(EMPTY, Doubles.concat(EMPTY, EMPTY, EMPTY)));
    assertTrue(Arrays.equals(ARRAY1, Doubles.concat(ARRAY1)));
    assertNotSame(ARRAY1, Doubles.concat(ARRAY1));
    assertTrue(Arrays.equals(ARRAY1, Doubles.concat(EMPTY, ARRAY1, EMPTY)));
    assertTrue(
        Arrays.equals(
            new double[] {(double) 1, (double) 1, (double) 1},
            Doubles.concat(ARRAY1, ARRAY1, ARRAY1)));
    assertTrue(
        Arrays.equals(
            new double[] {(double) 1, (double) 2, (double) 3, (double) 4},
            Doubles.concat(ARRAY1, ARRAY234)));
  }

  public void testEnsureCapacity() {
    assertSame(EMPTY, Doubles.ensureCapacity(EMPTY, 0, 1));
    assertSame(ARRAY1, Doubles.ensureCapacity(ARRAY1, 0, 1));
    assertSame(ARRAY1, Doubles.ensureCapacity(ARRAY1, 1, 1));
    assertTrue(
        Arrays.equals(
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

  @GwtIncompatible // Double.toString returns different value in GWT.
  public void testJoin() {
    assertEquals("", Doubles.join(",", EMPTY));
    assertEquals("1.0", Doubles.join(",", ARRAY1));
    assertEquals("1.0,2.0", Doubles.join(",", (double) 1, (double) 2));
    assertEquals("1.02.03.0", Doubles.join("", (double) 1, (double) 2, (double) 3));
  }

  public void testJoinNonTrivialDoubles() {
    assertEquals("", Doubles.join(",", EMPTY));
    assertEquals("1.2", Doubles.join(",", 1.2));
    assertEquals("1.3,2.4", Doubles.join(",", 1.3, 2.4));
    assertEquals("1.42.53.6", Doubles.join("", 1.4, 2.5, 3.6));
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
    assertTrue(Arrays.equals(expectedOutput, input));
  }

  private static void testReverse(
      double[] input, int fromIndex, int toIndex, double[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Doubles.reverse(input, fromIndex, toIndex);
    assertTrue(Arrays.equals(expectedOutput, input));
  }

  public void testReverseIndexed() {
    testReverse(new double[] {}, 0, 0, new double[] {});
    testReverse(new double[] {1}, 0, 1, new double[] {1});
    testReverse(new double[] {1, 2}, 0, 2, new double[] {2, 1});
    testReverse(new double[] {3, 1, 1}, 0, 2, new double[] {1, 3, 1});
    testReverse(new double[] {3, 1, 1}, 0, 1, new double[] {3, 1, 1});
    testReverse(new double[] {-1, 1, -2, 2}, 1, 3, new double[] {-1, -2, 1, 2});
  }

  public void testSortDescending() {
    testSortDescending(new double[] {}, new double[] {});
    testSortDescending(new double[] {1}, new double[] {1});
    testSortDescending(new double[] {1, 2}, new double[] {2, 1});
    testSortDescending(new double[] {1, 3, 1}, new double[] {3, 1, 1});
    testSortDescending(new double[] {-1, 1, -2, 2}, new double[] {2, 1, -1, -2});
    testSortDescending(
        new double[] {-1, 1, Double.NaN, -2, -0, 0, 2},
        new double[] {Double.NaN, 2, 1, 0, -0, -1, -2});
  }

  private static void testSortDescending(double[] input, double[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Doubles.sortDescending(input);
    // GWT's Arrays.equals doesn't appear to handle NaN correctly, so test each element individually
    for (int i = 0; i < input.length; i++) {
      assertEquals(0, Double.compare(expectedOutput[i], input[i]));
    }
  }

  private static void testSortDescending(
      double[] input, int fromIndex, int toIndex, double[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Doubles.sortDescending(input, fromIndex, toIndex);
    // GWT's Arrays.equals doesn't appear to handle NaN correctly, so test each element individually
    for (int i = 0; i < input.length; i++) {
      assertEquals(0, Double.compare(expectedOutput[i], input[i]));
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

  @GwtIncompatible // SerializableTester
  public void testLexicographicalComparatorSerializable() {
    Comparator<double[]> comparator = Doubles.lexicographicalComparator();
    assertSame(comparator, SerializableTester.reserialize(comparator));
  }

  @GwtIncompatible // SerializableTester
  public void testStringConverterSerialization() {
    SerializableTester.reserializeAndAssert(Doubles.stringConverter());
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
    for (int delta : new int[] {+1, 0, -1}) {
      for (int i = 0; i < VALUES.length; i++) {
        List<Double> list = Doubles.asList(VALUES).subList(0, i);
        Collection<Double> misleadingSize = Helpers.misleadingSizeCollection(delta);
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

  public void testToArray_withConversion() {
    double[] array = {(double) 0, (double) 1, (double) 2};

    List<Byte> bytes = Arrays.asList((byte) 0, (byte) 1, (byte) 2);
    List<Short> shorts = Arrays.asList((short) 0, (short) 1, (short) 2);
    List<Integer> ints = Arrays.asList(0, 1, 2);
    List<Float> floats = Arrays.asList((float) 0, (float) 1, (float) 2);
    List<Long> longs = Arrays.asList((long) 0, (long) 1, (long) 2);
    List<Double> doubles = Arrays.asList((double) 0, (double) 1, (double) 2);

    assertTrue(Arrays.equals(array, Doubles.toArray(bytes)));
    assertTrue(Arrays.equals(array, Doubles.toArray(shorts)));
    assertTrue(Arrays.equals(array, Doubles.toArray(ints)));
    assertTrue(Arrays.equals(array, Doubles.toArray(floats)));
    assertTrue(Arrays.equals(array, Doubles.toArray(longs)));
    assertTrue(Arrays.equals(array, Doubles.toArray(doubles)));
  }

  public void testAsList_isAView() {
    double[] array = {(double) 0, (double) 1};
    List<Double> list = Doubles.asList(array);
    list.set(0, (double) 2);
    assertTrue(Arrays.equals(new double[] {(double) 2, (double) 1}, array));
    array[1] = (double) 3;
    assertThat(list).containsExactly((double) 2, (double) 3).inOrder();
  }

  public void testAsList_toArray_roundTrip() {
    double[] array = {(double) 0, (double) 1, (double) 2};
    List<Double> list = Doubles.asList(array);
    double[] newArray = Doubles.toArray(list);

    // Make sure it returned a copy
    list.set(0, (double) 4);
    assertTrue(Arrays.equals(new double[] {(double) 0, (double) 1, (double) 2}, newArray));
    newArray[1] = (double) 5;
    assertEquals((double) 1, (double) list.get(1));
  }

  // This test stems from a real bug found by andrewk
  public void testAsList_subList_toArray_roundTrip() {
    double[] array = {(double) 0, (double) 1, (double) 2, (double) 3};
    List<Double> list = Doubles.asList(array);
    assertTrue(
        Arrays.equals(new double[] {(double) 1, (double) 2}, Doubles.toArray(list.subList(1, 3))));
    assertTrue(Arrays.equals(new double[] {}, Doubles.toArray(list.subList(2, 2))));
  }

  public void testAsListEmpty() {
    assertSame(Collections.emptyList(), Doubles.asList(EMPTY));
  }

  /**
   * A reference implementation for {@code tryParse} that just catches the exception from {@link
   * Double#valueOf}.
   */
  private static Double referenceTryParse(String input) {
    if (input.trim().length() < input.length()) {
      return null;
    }
    try {
      return Double.valueOf(input);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  @GwtIncompatible // Doubles.tryParse
  private static void checkTryParse(String input) {
    Double expected = referenceTryParse(input);
    assertEquals(expected, Doubles.tryParse(input));
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

  @GwtIncompatible // Doubles.tryParse
  private static void checkTryParse(double expected, String input) {
    assertEquals(Double.valueOf(expected), Doubles.tryParse(input));
    assertThat(input)
        .matches(
            Pattern.compile(
                Doubles.FLOATING_POINT_PATTERN.pattern(), Doubles.FLOATING_POINT_PATTERN.flags()));
  }

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
  @GwtIncompatible // Doubles.tryParse
  public void testTryParseAllCodePoints() {
    // Exercise non-ASCII digit test cases and the like.
    char[] tmp = new char[2];
    for (int i = Character.MIN_CODE_POINT; i < Character.MAX_CODE_POINT; i++) {
      Character.toChars(i, tmp, 0);
      checkTryParse(String.copyValueOf(tmp, 0, Character.charCount(i)));
    }
  }

  @GwtIncompatible // Doubles.tryParse
  public void testTryParseOfToStringIsOriginal() {
    for (double d : NUMBERS) {
      checkTryParse(d, Double.toString(d));
    }
  }

  @GwtIncompatible // Doubles.tryParse
  public void testTryParseOfToHexStringIsOriginal() {
    for (double d : NUMBERS) {
      checkTryParse(d, Double.toHexString(d));
    }
  }

  @GwtIncompatible // Doubles.tryParse
  public void testTryParseNaN() {
    checkTryParse("NaN");
    checkTryParse("+NaN");
    checkTryParse("-NaN");
  }

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

  @GwtIncompatible // Doubles.tryParse
  public void testTryParseFailures() {
    for (String badInput : BAD_TRY_PARSE_INPUTS) {
      assertThat(badInput)
          .doesNotMatch(
              Pattern.compile(
                  Doubles.FLOATING_POINT_PATTERN.pattern(),
                  Doubles.FLOATING_POINT_PATTERN.flags()));
      assertEquals(referenceTryParse(badInput), Doubles.tryParse(badInput));
      assertNull(Doubles.tryParse(badInput));
    }
  }

  @GwtIncompatible // NullPointerTester
  public void testNulls() {
    new NullPointerTester().testAllPublicStaticMethods(Doubles.class);
  }

  public void testStringConverter_convert() {
    Converter<String, Double> converter = Doubles.stringConverter();
    assertEquals((Double) 1.0, converter.convert("1.0"));
    assertEquals((Double) 0.0, converter.convert("0.0"));
    assertEquals((Double) (-1.0), converter.convert("-1.0"));
    assertEquals((Double) 1.0, converter.convert("1"));
    assertEquals((Double) 0.0, converter.convert("0"));
    assertEquals((Double) (-1.0), converter.convert("-1"));
    assertEquals((Double) 1e6, converter.convert("1e6"));
    assertEquals((Double) 1e-6, converter.convert("1e-6"));
  }

  public void testStringConverter_convertError() {
    try {
      Doubles.stringConverter().convert("notanumber");
      fail();
    } catch (NumberFormatException expected) {
    }
  }

  public void testStringConverter_nullConversions() {
    assertNull(Doubles.stringConverter().convert(null));
    assertNull(Doubles.stringConverter().reverse().convert(null));
  }

  @GwtIncompatible // Double.toString returns different value in GWT.
  public void testStringConverter_reverse() {
    Converter<String, Double> converter = Doubles.stringConverter();
    assertEquals("1.0", converter.reverse().convert(1.0));
    assertEquals("0.0", converter.reverse().convert(0.0));
    assertEquals("-1.0", converter.reverse().convert(-1.0));
    assertEquals("1000000.0", converter.reverse().convert(1e6));
    assertEquals("1.0E-6", converter.reverse().convert(1e-6));
  }

  @GwtIncompatible // NullPointerTester
  public void testStringConverter_nullPointerTester() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicInstanceMethods(Doubles.stringConverter());
  }

  @GwtIncompatible
  public void testTryParse_withNullNoGwt() {
    assertNull(Doubles.tryParse("null"));
    try {
      Doubles.tryParse(null);
      fail("Expected NPE");
    } catch (NullPointerException expected) {
    }
  }
}

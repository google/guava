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
import static java.lang.Float.NaN;

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
import junit.framework.TestCase;

/**
 * Unit test for {@link Floats}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
@SuppressWarnings("cast") // redundant casts are intentional and harmless
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
      assertEquals(((Float) value).hashCode(), Floats.hashCode(value));
    }
  }

  public void testIsFinite() {
    for (float value : NUMBERS) {
      assertEquals(!(Float.isInfinite(value) || Float.isNaN(value)), Floats.isFinite(value));
    }
  }

  public void testCompare() {
    for (float x : VALUES) {
      for (float y : VALUES) {
        // note: spec requires only that the sign is the same
        assertEquals(x + ", " + y, Float.valueOf(x).compareTo(y), Floats.compare(x, y));
      }
    }
  }

  public void testContains() {
    assertFalse(Floats.contains(EMPTY, (float) 1));
    assertFalse(Floats.contains(ARRAY1, (float) 2));
    assertFalse(Floats.contains(ARRAY234, (float) 1));
    assertTrue(Floats.contains(new float[] {(float) -1}, (float) -1));
    assertTrue(Floats.contains(ARRAY234, (float) 2));
    assertTrue(Floats.contains(ARRAY234, (float) 3));
    assertTrue(Floats.contains(ARRAY234, (float) 4));

    for (float value : NUMBERS) {
      assertTrue("" + value, Floats.contains(new float[] {5f, value}, value));
    }
    assertFalse(Floats.contains(new float[] {5f, NaN}, NaN));
  }

  public void testIndexOf() {
    assertEquals(-1, Floats.indexOf(EMPTY, (float) 1));
    assertEquals(-1, Floats.indexOf(ARRAY1, (float) 2));
    assertEquals(-1, Floats.indexOf(ARRAY234, (float) 1));
    assertEquals(0, Floats.indexOf(new float[] {(float) -1}, (float) -1));
    assertEquals(0, Floats.indexOf(ARRAY234, (float) 2));
    assertEquals(1, Floats.indexOf(ARRAY234, (float) 3));
    assertEquals(2, Floats.indexOf(ARRAY234, (float) 4));
    assertEquals(
        1, Floats.indexOf(new float[] {(float) 2, (float) 3, (float) 2, (float) 3}, (float) 3));

    for (float value : NUMBERS) {
      assertEquals("" + value, 1, Floats.indexOf(new float[] {5f, value}, value));
    }
    assertEquals(-1, Floats.indexOf(new float[] {5f, NaN}, NaN));
  }

  public void testIndexOf_arrayTarget() {
    assertEquals(0, Floats.indexOf(EMPTY, EMPTY));
    assertEquals(0, Floats.indexOf(ARRAY234, EMPTY));
    assertEquals(-1, Floats.indexOf(EMPTY, ARRAY234));
    assertEquals(-1, Floats.indexOf(ARRAY234, ARRAY1));
    assertEquals(-1, Floats.indexOf(ARRAY1, ARRAY234));
    assertEquals(0, Floats.indexOf(ARRAY1, ARRAY1));
    assertEquals(0, Floats.indexOf(ARRAY234, ARRAY234));
    assertEquals(0, Floats.indexOf(ARRAY234, new float[] {(float) 2, (float) 3}));
    assertEquals(1, Floats.indexOf(ARRAY234, new float[] {(float) 3, (float) 4}));
    assertEquals(1, Floats.indexOf(ARRAY234, new float[] {(float) 3}));
    assertEquals(2, Floats.indexOf(ARRAY234, new float[] {(float) 4}));
    assertEquals(
        1,
        Floats.indexOf(
            new float[] {(float) 2, (float) 3, (float) 3, (float) 3, (float) 3},
            new float[] {(float) 3}));
    assertEquals(
        2,
        Floats.indexOf(
            new float[] {
              (float) 2, (float) 3, (float) 2, (float) 3, (float) 4, (float) 2, (float) 3
            },
            new float[] {(float) 2, (float) 3, (float) 4}));
    assertEquals(
        1,
        Floats.indexOf(
            new float[] {
              (float) 2, (float) 2, (float) 3, (float) 4, (float) 2, (float) 3, (float) 4
            },
            new float[] {(float) 2, (float) 3, (float) 4}));
    assertEquals(
        -1,
        Floats.indexOf(
            new float[] {(float) 4, (float) 3, (float) 2},
            new float[] {(float) 2, (float) 3, (float) 4}));

    for (float value : NUMBERS) {
      assertEquals(
          "" + value,
          1,
          Floats.indexOf(new float[] {5f, value, value, 5f}, new float[] {value, value}));
    }
    assertEquals(-1, Floats.indexOf(new float[] {5f, NaN, NaN, 5f}, new float[] {NaN, NaN}));
  }

  public void testLastIndexOf() {
    assertEquals(-1, Floats.lastIndexOf(EMPTY, (float) 1));
    assertEquals(-1, Floats.lastIndexOf(ARRAY1, (float) 2));
    assertEquals(-1, Floats.lastIndexOf(ARRAY234, (float) 1));
    assertEquals(0, Floats.lastIndexOf(new float[] {(float) -1}, (float) -1));
    assertEquals(0, Floats.lastIndexOf(ARRAY234, (float) 2));
    assertEquals(1, Floats.lastIndexOf(ARRAY234, (float) 3));
    assertEquals(2, Floats.lastIndexOf(ARRAY234, (float) 4));
    assertEquals(
        3, Floats.lastIndexOf(new float[] {(float) 2, (float) 3, (float) 2, (float) 3}, (float) 3));

    for (float value : NUMBERS) {
      assertEquals("" + value, 0, Floats.lastIndexOf(new float[] {value, 5f}, value));
    }
    assertEquals(-1, Floats.lastIndexOf(new float[] {NaN, 5f}, NaN));
  }

  @GwtIncompatible
  public void testMax_noArgs() {
    try {
      Floats.max();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMax() {
    assertEquals(GREATEST, Floats.max(GREATEST));
    assertEquals(LEAST, Floats.max(LEAST));
    assertEquals(
        (float) 9,
        Floats.max((float) 8, (float) 6, (float) 7, (float) 5, (float) 3, (float) 0, (float) 9));

    assertEquals(0f, Floats.max(-0f, 0f));
    assertEquals(0f, Floats.max(0f, -0f));
    assertEquals(GREATEST, Floats.max(NUMBERS));
    assertTrue(Float.isNaN(Floats.max(VALUES)));
  }

  @GwtIncompatible
  public void testMin_noArgs() {
    try {
      Floats.min();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMin() {
    assertEquals(LEAST, Floats.min(LEAST));
    assertEquals(GREATEST, Floats.min(GREATEST));
    assertEquals(
        (float) 0,
        Floats.min((float) 8, (float) 6, (float) 7, (float) 5, (float) 3, (float) 0, (float) 9));

    assertEquals(-0f, Floats.min(-0f, 0f));
    assertEquals(-0f, Floats.min(0f, -0f));
    assertEquals(LEAST, Floats.min(NUMBERS));
    assertTrue(Float.isNaN(Floats.min(VALUES)));
  }

  public void testConstrainToRange() {
    float tolerance = 1e-10f;
    assertEquals((float) 1, Floats.constrainToRange((float) 1, (float) 0, (float) 5), tolerance);
    assertEquals((float) 1, Floats.constrainToRange((float) 1, (float) 1, (float) 5), tolerance);
    assertEquals((float) 3, Floats.constrainToRange((float) 1, (float) 3, (float) 5), tolerance);
    assertEquals((float) -1, Floats.constrainToRange((float) 0, (float) -5, (float) -1), tolerance);
    assertEquals((float) 2, Floats.constrainToRange((float) 5, (float) 2, (float) 2), tolerance);
    try {
      Floats.constrainToRange((float) 1, (float) 3, (float) 2);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testConcat() {
    assertTrue(Arrays.equals(EMPTY, Floats.concat()));
    assertTrue(Arrays.equals(EMPTY, Floats.concat(EMPTY)));
    assertTrue(Arrays.equals(EMPTY, Floats.concat(EMPTY, EMPTY, EMPTY)));
    assertTrue(Arrays.equals(ARRAY1, Floats.concat(ARRAY1)));
    assertNotSame(ARRAY1, Floats.concat(ARRAY1));
    assertTrue(Arrays.equals(ARRAY1, Floats.concat(EMPTY, ARRAY1, EMPTY)));
    assertTrue(
        Arrays.equals(
            new float[] {(float) 1, (float) 1, (float) 1}, Floats.concat(ARRAY1, ARRAY1, ARRAY1)));
    assertTrue(
        Arrays.equals(
            new float[] {(float) 1, (float) 2, (float) 3, (float) 4},
            Floats.concat(ARRAY1, ARRAY234)));
  }

  public void testEnsureCapacity() {
    assertSame(EMPTY, Floats.ensureCapacity(EMPTY, 0, 1));
    assertSame(ARRAY1, Floats.ensureCapacity(ARRAY1, 0, 1));
    assertSame(ARRAY1, Floats.ensureCapacity(ARRAY1, 1, 1));
    assertTrue(
        Arrays.equals(
            new float[] {(float) 1, (float) 0, (float) 0}, Floats.ensureCapacity(ARRAY1, 2, 1)));
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

  @GwtIncompatible // Float.toString returns different value in GWT.
  public void testJoin() {
    assertEquals("", Floats.join(",", EMPTY));
    assertEquals("1.0", Floats.join(",", ARRAY1));
    assertEquals("1.0,2.0", Floats.join(",", (float) 1, (float) 2));
    assertEquals("1.02.03.0", Floats.join("", (float) 1, (float) 2, (float) 3));
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

  @GwtIncompatible // SerializableTester
  public void testLexicographicalComparatorSerializable() {
    Comparator<float[]> comparator = Floats.lexicographicalComparator();
    assertSame(comparator, SerializableTester.reserialize(comparator));
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
    assertTrue(Arrays.equals(expectedOutput, input));
  }

  private static void testReverse(
      float[] input, int fromIndex, int toIndex, float[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Floats.reverse(input, fromIndex, toIndex);
    assertTrue(Arrays.equals(expectedOutput, input));
  }

  public void testReverseIndexed() {
    testReverse(new float[] {}, 0, 0, new float[] {});
    testReverse(new float[] {1}, 0, 1, new float[] {1});
    testReverse(new float[] {1, 2}, 0, 2, new float[] {2, 1});
    testReverse(new float[] {3, 1, 1}, 0, 2, new float[] {1, 3, 1});
    testReverse(new float[] {3, 1, 1}, 0, 1, new float[] {3, 1, 1});
    testReverse(new float[] {-1, 1, -2, 2}, 1, 3, new float[] {-1, -2, 1, 2});
  }

  public void testSortDescending() {
    testSortDescending(new float[] {}, new float[] {});
    testSortDescending(new float[] {1}, new float[] {1});
    testSortDescending(new float[] {1, 2}, new float[] {2, 1});
    testSortDescending(new float[] {1, 3, 1}, new float[] {3, 1, 1});
    testSortDescending(new float[] {-1, 1, -2, 2}, new float[] {2, 1, -1, -2});
    testSortDescending(
        new float[] {-1, 1, Float.NaN, -2, -0, 0, 2}, new float[] {Float.NaN, 2, 1, 0, -0, -1, -2});
  }

  private static void testSortDescending(float[] input, float[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Floats.sortDescending(input);
    // GWT's Arrays.equals doesn't appear to handle NaN correctly, so test each element individually
    for (int i = 0; i < input.length; i++) {
      assertEquals(0, Float.compare(expectedOutput[i], input[i]));
    }
  }

  private static void testSortDescending(
      float[] input, int fromIndex, int toIndex, float[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Floats.sortDescending(input, fromIndex, toIndex);
    // GWT's Arrays.equals doesn't appear to handle NaN correctly, so test each element individually
    for (int i = 0; i < input.length; i++) {
      assertEquals(0, Float.compare(expectedOutput[i], input[i]));
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

  @GwtIncompatible // SerializableTester
  public void testStringConverterSerialization() {
    SerializableTester.reserializeAndAssert(Floats.stringConverter());
  }

  public void testToArray() {
    // need explicit type parameter to avoid javac warning!?
    List<Float> none = Arrays.<Float>asList();
    assertTrue(Arrays.equals(EMPTY, Floats.toArray(none)));

    List<Float> one = Arrays.asList((float) 1);
    assertTrue(Arrays.equals(ARRAY1, Floats.toArray(one)));

    float[] array = {(float) 0, (float) 1, (float) 3};

    List<Float> three = Arrays.asList((float) 0, (float) 1, (float) 3);
    assertTrue(Arrays.equals(array, Floats.toArray(three)));

    assertTrue(Arrays.equals(array, Floats.toArray(Floats.asList(array))));
  }

  public void testToArray_threadSafe() {
    for (int delta : new int[] {+1, 0, -1}) {
      for (int i = 0; i < VALUES.length; i++) {
        List<Float> list = Floats.asList(VALUES).subList(0, i);
        Collection<Float> misleadingSize = Helpers.misleadingSizeCollection(delta);
        misleadingSize.addAll(list);
        float[] arr = Floats.toArray(misleadingSize);
        assertEquals(i, arr.length);
        for (int j = 0; j < i; j++) {
          assertEquals(VALUES[j], arr[j]);
        }
      }
    }
  }

  public void testToArray_withNull() {
    List<Float> list = Arrays.asList((float) 0, (float) 1, null);
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

    assertTrue(Arrays.equals(array, Floats.toArray(bytes)));
    assertTrue(Arrays.equals(array, Floats.toArray(shorts)));
    assertTrue(Arrays.equals(array, Floats.toArray(ints)));
    assertTrue(Arrays.equals(array, Floats.toArray(floats)));
    assertTrue(Arrays.equals(array, Floats.toArray(longs)));
    assertTrue(Arrays.equals(array, Floats.toArray(doubles)));
  }

  public void testAsList_isAView() {
    float[] array = {(float) 0, (float) 1};
    List<Float> list = Floats.asList(array);
    list.set(0, (float) 2);
    assertTrue(Arrays.equals(new float[] {(float) 2, (float) 1}, array));
    array[1] = (float) 3;
    assertThat(list).containsExactly((float) 2, (float) 3).inOrder();
  }

  public void testAsList_toArray_roundTrip() {
    float[] array = {(float) 0, (float) 1, (float) 2};
    List<Float> list = Floats.asList(array);
    float[] newArray = Floats.toArray(list);

    // Make sure it returned a copy
    list.set(0, (float) 4);
    assertTrue(Arrays.equals(new float[] {(float) 0, (float) 1, (float) 2}, newArray));
    newArray[1] = (float) 5;
    assertEquals((float) 1, (float) list.get(1));
  }

  // This test stems from a real bug found by andrewk
  public void testAsList_subList_toArray_roundTrip() {
    float[] array = {(float) 0, (float) 1, (float) 2, (float) 3};
    List<Float> list = Floats.asList(array);
    assertTrue(
        Arrays.equals(new float[] {(float) 1, (float) 2}, Floats.toArray(list.subList(1, 3))));
    assertTrue(Arrays.equals(new float[] {}, Floats.toArray(list.subList(2, 2))));
  }

  public void testAsListEmpty() {
    assertSame(Collections.emptyList(), Floats.asList(EMPTY));
  }

  /**
   * A reference implementation for {@code tryParse} that just catches the exception from {@link
   * Float#valueOf}.
   */
  private static Float referenceTryParse(String input) {
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
    assertEquals(referenceTryParse(input), Floats.tryParse(input));
  }

  @GwtIncompatible // Floats.tryParse
  private static void checkTryParse(float expected, String input) {
    assertEquals(Float.valueOf(expected), Floats.tryParse(input));
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
      assertEquals(referenceTryParse(badInput), Floats.tryParse(badInput));
      assertNull(Floats.tryParse(badInput));
    }
  }

  @GwtIncompatible // NullPointerTester
  public void testNulls() {
    new NullPointerTester().testAllPublicStaticMethods(Floats.class);
  }

  @GwtIncompatible // Float.toString returns different value in GWT.
  public void testStringConverter_convert() {
    Converter<String, Float> converter = Floats.stringConverter();
    assertEquals((Float) 1.0f, converter.convert("1.0"));
    assertEquals((Float) 0.0f, converter.convert("0.0"));
    assertEquals((Float) (-1.0f), converter.convert("-1.0"));
    assertEquals((Float) 1.0f, converter.convert("1"));
    assertEquals((Float) 0.0f, converter.convert("0"));
    assertEquals((Float) (-1.0f), converter.convert("-1"));
    assertEquals((Float) 1e6f, converter.convert("1e6"));
    assertEquals((Float) 1e-6f, converter.convert("1e-6"));
  }

  public void testStringConverter_convertError() {
    try {
      Floats.stringConverter().convert("notanumber");
      fail();
    } catch (NumberFormatException expected) {
    }
  }

  public void testStringConverter_nullConversions() {
    assertNull(Floats.stringConverter().convert(null));
    assertNull(Floats.stringConverter().reverse().convert(null));
  }

  @GwtIncompatible // Float.toString returns different value in GWT.
  public void testStringConverter_reverse() {
    Converter<String, Float> converter = Floats.stringConverter();
    assertEquals("1.0", converter.reverse().convert(1.0f));
    assertEquals("0.0", converter.reverse().convert(0.0f));
    assertEquals("-1.0", converter.reverse().convert(-1.0f));
    assertEquals("1000000.0", converter.reverse().convert(1e6f));
    assertEquals("1.0E-6", converter.reverse().convert(1e-6f));
  }

  @GwtIncompatible // NullPointerTester
  public void testStringConverter_nullPointerTester() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicInstanceMethods(Floats.stringConverter());
  }

  @GwtIncompatible
  public void testTryParse_withNullNoGwt() {
    assertNull(Floats.tryParse("null"));
    try {
      Floats.tryParse(null);
      fail("Expected NPE");
    } catch (NullPointerException expected) {
    }
  }
}

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
      assertThat(Bytes.hashCode(value)).isEqualTo(((Byte) value).hashCode());
    }
  }

  public void testContains() {
    assertThat(Bytes.contains(EMPTY, (byte) 1)).isFalse();
    assertThat(Bytes.contains(ARRAY1, (byte) 2)).isFalse();
    assertThat(Bytes.contains(ARRAY234, (byte) 1)).isFalse();
    assertThat(Bytes.contains(new byte[] {(byte) -1}, (byte) -1)).isTrue();
    assertThat(Bytes.contains(ARRAY234, (byte) 2)).isTrue();
    assertThat(Bytes.contains(ARRAY234, (byte) 3)).isTrue();
    assertThat(Bytes.contains(ARRAY234, (byte) 4)).isTrue();
  }

  public void testIndexOf() {
    assertThat(Bytes.indexOf(EMPTY, (byte) 1)).isEqualTo(-1);
    assertThat(Bytes.indexOf(ARRAY1, (byte) 2)).isEqualTo(-1);
    assertThat(Bytes.indexOf(ARRAY234, (byte) 1)).isEqualTo(-1);
    assertThat(Bytes.indexOf(new byte[] {(byte) -1}, (byte) -1)).isEqualTo(0);
    assertThat(Bytes.indexOf(ARRAY234, (byte) 2)).isEqualTo(0);
    assertThat(Bytes.indexOf(ARRAY234, (byte) 3)).isEqualTo(1);
    assertThat(Bytes.indexOf(ARRAY234, (byte) 4)).isEqualTo(2);
    assertThat(Bytes.indexOf(new byte[] {(byte) 2, (byte) 3, (byte) 2, (byte) 3}, (byte) 3))
        .isEqualTo(1);
  }

  public void testIndexOf_arrayTarget() {
    assertThat(Bytes.indexOf(EMPTY, EMPTY)).isEqualTo(0);
    assertThat(Bytes.indexOf(ARRAY234, EMPTY)).isEqualTo(0);
    assertThat(Bytes.indexOf(EMPTY, ARRAY234)).isEqualTo(-1);
    assertThat(Bytes.indexOf(ARRAY234, ARRAY1)).isEqualTo(-1);
    assertThat(Bytes.indexOf(ARRAY1, ARRAY234)).isEqualTo(-1);
    assertThat(Bytes.indexOf(ARRAY1, ARRAY1)).isEqualTo(0);
    assertThat(Bytes.indexOf(ARRAY234, ARRAY234)).isEqualTo(0);
    assertThat(Bytes.indexOf(ARRAY234, new byte[] {(byte) 2, (byte) 3})).isEqualTo(0);
    assertThat(Bytes.indexOf(ARRAY234, new byte[] {(byte) 3, (byte) 4})).isEqualTo(1);
    assertThat(Bytes.indexOf(ARRAY234, new byte[] {(byte) 3})).isEqualTo(1);
    assertThat(Bytes.indexOf(ARRAY234, new byte[] {(byte) 4})).isEqualTo(2);
    assertThat(
            Bytes.indexOf(
                new byte[] {(byte) 2, (byte) 3, (byte) 3, (byte) 3, (byte) 3},
                new byte[] {(byte) 3}))
        .isEqualTo(1);
    assertThat(
            Bytes.indexOf(
                new byte[] {(byte) 2, (byte) 3, (byte) 2, (byte) 3, (byte) 4, (byte) 2, (byte) 3},
                new byte[] {(byte) 2, (byte) 3, (byte) 4}))
        .isEqualTo(2);
    assertThat(
            Bytes.indexOf(
                new byte[] {(byte) 2, (byte) 2, (byte) 3, (byte) 4, (byte) 2, (byte) 3, (byte) 4},
                new byte[] {(byte) 2, (byte) 3, (byte) 4}))
        .isEqualTo(1);
    assertThat(
            Bytes.indexOf(
                new byte[] {(byte) 4, (byte) 3, (byte) 2},
                new byte[] {(byte) 2, (byte) 3, (byte) 4}))
        .isEqualTo(-1);
  }

  public void testLastIndexOf() {
    assertThat(Bytes.lastIndexOf(EMPTY, (byte) 1)).isEqualTo(-1);
    assertThat(Bytes.lastIndexOf(ARRAY1, (byte) 2)).isEqualTo(-1);
    assertThat(Bytes.lastIndexOf(ARRAY234, (byte) 1)).isEqualTo(-1);
    assertThat(Bytes.lastIndexOf(new byte[] {(byte) -1}, (byte) -1)).isEqualTo(0);
    assertThat(Bytes.lastIndexOf(ARRAY234, (byte) 2)).isEqualTo(0);
    assertThat(Bytes.lastIndexOf(ARRAY234, (byte) 3)).isEqualTo(1);
    assertThat(Bytes.lastIndexOf(ARRAY234, (byte) 4)).isEqualTo(2);
    assertThat(Bytes.lastIndexOf(new byte[] {(byte) 2, (byte) 3, (byte) 2, (byte) 3}, (byte) 3))
        .isEqualTo(3);
  }

  public void testConcat() {
    assertThat(Bytes.concat()).isEqualTo(EMPTY);
    assertThat(Bytes.concat(EMPTY)).isEqualTo(EMPTY);
    assertThat(Bytes.concat(EMPTY, EMPTY, EMPTY)).isEqualTo(EMPTY);
    assertThat(Bytes.concat(ARRAY1)).isEqualTo(ARRAY1);
    assertThat(Bytes.concat(ARRAY1)).isNotSameInstanceAs(ARRAY1);
    assertThat(Bytes.concat(EMPTY, ARRAY1, EMPTY)).isEqualTo(ARRAY1);
    assertThat(Bytes.concat(ARRAY1, ARRAY1, ARRAY1))
        .isEqualTo(new byte[] {(byte) 1, (byte) 1, (byte) 1});
    assertThat(Bytes.concat(ARRAY1, ARRAY234))
        .isEqualTo(new byte[] {(byte) 1, (byte) 2, (byte) 3, (byte) 4});
  }

  public void testEnsureCapacity() {
    assertThat(Bytes.ensureCapacity(EMPTY, 0, 1)).isSameInstanceAs(EMPTY);
    assertThat(Bytes.ensureCapacity(ARRAY1, 0, 1)).isSameInstanceAs(ARRAY1);
    assertThat(Bytes.ensureCapacity(ARRAY1, 1, 1)).isSameInstanceAs(ARRAY1);
    assertThat(Bytes.ensureCapacity(ARRAY1, 2, 1))
        .isEqualTo(new byte[] {(byte) 1, (byte) 0, (byte) 0});
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
    assertThat(Bytes.toArray(none)).isEqualTo(EMPTY);

    List<Byte> one = Arrays.asList((byte) 1);
    assertThat(Bytes.toArray(one)).isEqualTo(ARRAY1);

    byte[] array = {(byte) 0, (byte) 1, (byte) 0x55};

    List<Byte> three = Arrays.asList((byte) 0, (byte) 1, (byte) 0x55);
    assertThat(Bytes.toArray(three)).isEqualTo(array);

    assertThat(Bytes.toArray(Bytes.asList(array))).isEqualTo(array);
  }

  public void testToArray_threadSafe() {
    for (int delta : new int[] {+1, 0, -1}) {
      for (int i = 0; i < VALUES.length; i++) {
        List<Byte> list = Bytes.asList(VALUES).subList(0, i);
        Collection<Byte> misleadingSize = Helpers.misleadingSizeCollection(delta);
        misleadingSize.addAll(list);
        byte[] arr = Bytes.toArray(misleadingSize);
        assertThat(arr).hasLength(i);
        for (int j = 0; j < i; j++) {
          assertThat(arr[j]).isEqualTo(VALUES[j]);
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

    assertThat(Bytes.toArray(bytes)).isEqualTo(array);
    assertThat(Bytes.toArray(shorts)).isEqualTo(array);
    assertThat(Bytes.toArray(ints)).isEqualTo(array);
    assertThat(Bytes.toArray(floats)).isEqualTo(array);
    assertThat(Bytes.toArray(longs)).isEqualTo(array);
    assertThat(Bytes.toArray(doubles)).isEqualTo(array);
  }

  public void testAsList_isAView() {
    byte[] array = {(byte) 0, (byte) 1};
    List<Byte> list = Bytes.asList(array);
    list.set(0, (byte) 2);
    assertThat(array).isEqualTo(new byte[] {(byte) 2, (byte) 1});
    array[1] = (byte) 3;
    assertThat(list).containsExactly((byte) 2, (byte) 3).inOrder();
  }

  public void testAsList_toArray_roundTrip() {
    byte[] array = {(byte) 0, (byte) 1, (byte) 2};
    List<Byte> list = Bytes.asList(array);
    byte[] newArray = Bytes.toArray(list);

    // Make sure it returned a copy
    list.set(0, (byte) 4);
    assertThat(newArray).isEqualTo(new byte[] {(byte) 0, (byte) 1, (byte) 2});
    newArray[1] = (byte) 5;
    assertThat((byte) list.get(1)).isEqualTo((byte) 1);
  }

  // This test stems from a real bug found by andrewk
  public void testAsList_subList_toArray_roundTrip() {
    byte[] array = {(byte) 0, (byte) 1, (byte) 2, (byte) 3};
    List<Byte> list = Bytes.asList(array);
    assertThat(Bytes.toArray(list.subList(1, 3))).isEqualTo(new byte[] {(byte) 1, (byte) 2});
    assertThat(Bytes.toArray(list.subList(2, 2))).isEqualTo(new byte[] {});
  }

  public void testAsListEmpty() {
    assertThat(Bytes.asList(EMPTY)).isSameInstanceAs(Collections.emptyList());
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
    assertThat(input).isEqualTo(expectedOutput);
  }

  private static void testReverse(byte[] input, int fromIndex, int toIndex, byte[] expectedOutput) {
    input = Arrays.copyOf(input, input.length);
    Bytes.reverse(input, fromIndex, toIndex);
    assertThat(input).isEqualTo(expectedOutput);
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

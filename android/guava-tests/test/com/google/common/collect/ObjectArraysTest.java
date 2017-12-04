/*
 * Copyright (C) 2007 The Guava Authors
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

package com.google.common.collect;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.testing.NullPointerTester;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;

/**
 * Unit test for {@code ObjectArrays}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
public class ObjectArraysTest extends TestCase {

  @GwtIncompatible // NullPointerTester
  public void testNullPointerExceptions() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(ObjectArrays.class);
  }

  @GwtIncompatible // ObjectArrays.newArray(Class, int)
  public void testNewArray_fromClass_Empty() {
    String[] empty = ObjectArrays.newArray(String.class, 0);
    assertEquals(String[].class, empty.getClass());
    assertThat(empty).isEmpty();
  }

  @GwtIncompatible // ObjectArrays.newArray(Class, int)
  public void testNewArray_fromClass_Nonempty() {
    String[] array = ObjectArrays.newArray(String.class, 2);
    assertEquals(String[].class, array.getClass());
    assertThat(array).hasLength(2);
    assertNull(array[0]);
  }

  @GwtIncompatible // ObjectArrays.newArray(Class, int)
  public void testNewArray_fromClass_OfArray() {
    String[][] array = ObjectArrays.newArray(String[].class, 1);
    assertEquals(String[][].class, array.getClass());
    assertThat(array).hasLength(1);
    assertNull(array[0]);
  }

  public void testNewArray_fromArray_Empty() {
    String[] in = new String[0];
    String[] empty = ObjectArrays.newArray(in, 0);
    assertThat(empty).isEmpty();
  }

  public void testNewArray_fromArray_Nonempty() {
    String[] array = ObjectArrays.newArray(new String[0], 2);
    assertEquals(String[].class, array.getClass());
    assertThat(array).hasLength(2);
    assertNull(array[0]);
  }

  public void testNewArray_fromArray_OfArray() {
    String[][] array = ObjectArrays.newArray(new String[0][0], 1);
    assertEquals(String[][].class, array.getClass());
    assertThat(array).hasLength(1);
    assertNull(array[0]);
  }

  @GwtIncompatible // ObjectArrays.concat(Object[], Object[], Class)
  public void testConcatEmptyEmpty() {
    String[] result = ObjectArrays.concat(new String[0], new String[0], String.class);
    assertEquals(String[].class, result.getClass());
    assertThat(result).isEmpty();
  }

  @GwtIncompatible // ObjectArrays.concat(Object[], Object[], Class)
  public void testConcatEmptyNonempty() {
    String[] result = ObjectArrays.concat(new String[0], new String[] {"a", "b"}, String.class);
    assertEquals(String[].class, result.getClass());
    assertThat(result).asList().containsExactly("a", "b").inOrder();
  }

  @GwtIncompatible // ObjectArrays.concat(Object[], Object[], Class)
  public void testConcatNonemptyEmpty() {
    String[] result = ObjectArrays.concat(new String[] {"a", "b"}, new String[0], String.class);
    assertEquals(String[].class, result.getClass());
    assertThat(result).asList().containsExactly("a", "b").inOrder();
  }

  @GwtIncompatible // ObjectArrays.concat(Object[], Object[], Class)
  public void testConcatBasic() {
    String[] result =
        ObjectArrays.concat(new String[] {"a", "b"}, new String[] {"c", "d"}, String.class);
    assertEquals(String[].class, result.getClass());
    assertThat(result).asList().containsExactly("a", "b", "c", "d").inOrder();
  }

  @GwtIncompatible // ObjectArrays.concat(Object[], Object[], Class)
  public void testConcatWithMoreGeneralType() {
    Serializable[] result = ObjectArrays.concat(new String[0], new String[0], Serializable.class);
    assertEquals(Serializable[].class, result.getClass());
  }

  public void testToArrayImpl1() {
    doTestToArrayImpl1(Lists.<Integer>newArrayList());
    doTestToArrayImpl1(Lists.newArrayList(1));
    doTestToArrayImpl1(Lists.newArrayList(1, null, 3));
  }

  private void doTestToArrayImpl1(List<Integer> list) {
    Object[] reference = list.toArray();
    Object[] target = ObjectArrays.toArrayImpl(list);
    assertEquals(reference.getClass(), target.getClass());
    assertTrue(Arrays.equals(reference, target));
  }

  public void testToArrayImpl2() {
    doTestToArrayImpl2(Lists.<Integer>newArrayList(), new Integer[0], false);
    doTestToArrayImpl2(Lists.<Integer>newArrayList(), new Integer[1], true);

    doTestToArrayImpl2(Lists.newArrayList(1), new Integer[0], false);
    doTestToArrayImpl2(Lists.newArrayList(1), new Integer[1], true);
    doTestToArrayImpl2(Lists.newArrayList(1), new Integer[] {2, 3}, true);

    doTestToArrayImpl2(Lists.newArrayList(1, null, 3), new Integer[0], false);
    doTestToArrayImpl2(Lists.newArrayList(1, null, 3), new Integer[2], false);
    doTestToArrayImpl2(Lists.newArrayList(1, null, 3), new Integer[3], true);
  }

  private void doTestToArrayImpl2(List<Integer> list, Integer[] array1, boolean expectModify) {
    Integer[] starting = Arrays.copyOf(array1, array1.length);
    Integer[] array2 = Arrays.copyOf(array1, array1.length);
    Object[] reference = list.toArray(array1);

    Object[] target = ObjectArrays.toArrayImpl(list, array2);

    assertEquals(reference.getClass(), target.getClass());
    assertTrue(Arrays.equals(reference, target));
    assertTrue(Arrays.equals(reference, target));

    Object[] expectedArray1 = expectModify ? reference : starting;
    Object[] expectedArray2 = expectModify ? target : starting;
    assertTrue(Arrays.equals(expectedArray1, array1));
    assertTrue(Arrays.equals(expectedArray2, array2));
  }

  public void testPrependZeroElements() {
    String[] result = ObjectArrays.concat("foo", new String[] {});
    assertThat(result).asList().contains("foo");
  }

  public void testPrependOneElement() {
    String[] result = ObjectArrays.concat("foo", new String[] {"bar"});
    assertThat(result).asList().containsExactly("foo", "bar").inOrder();
  }

  public void testPrependTwoElements() {
    String[] result = ObjectArrays.concat("foo", new String[] {"bar", "baz"});
    assertThat(result).asList().containsExactly("foo", "bar", "baz").inOrder();
  }

  public void testAppendZeroElements() {
    String[] result = ObjectArrays.concat(new String[] {}, "foo");
    assertThat(result).asList().contains("foo");
  }

  public void testAppendOneElement() {
    String[] result = ObjectArrays.concat(new String[] {"foo"}, "bar");
    assertThat(result).asList().containsExactly("foo", "bar").inOrder();
  }

  public void testAppendTwoElements() {
    String[] result = ObjectArrays.concat(new String[] {"foo", "bar"}, "baz");
    assertThat(result).asList().containsExactly("foo", "bar", "baz").inOrder();
  }

  public void testEmptyArrayToEmpty() {
    doTestNewArrayEquals(new Object[0], 0);
  }

  public void testEmptyArrayToNonEmpty() {
    checkArrayEquals(new Long[5], ObjectArrays.newArray(new Long[0], 5));
  }

  public void testNonEmptyToShorter() {
    checkArrayEquals(new String[9], ObjectArrays.newArray(new String[10], 9));
  }

  public void testNonEmptyToSameLength() {
    doTestNewArrayEquals(new String[10], 10);
  }

  public void testNonEmptyToLonger() {
    checkArrayEquals(
        new String[10], ObjectArrays.newArray(new String[] {"a", "b", "c", "d", "e"}, 10));
  }

  private static void checkArrayEquals(Object[] expected, Object[] actual) {
    assertTrue(
        "expected("
            + expected.getClass()
            + "): "
            + Arrays.toString(expected)
            + " actual("
            + actual.getClass()
            + "): "
            + Arrays.toString(actual),
        arrayEquals(expected, actual));
  }

  private static boolean arrayEquals(Object[] array1, Object[] array2) {
    assertSame(array1.getClass(), array2.getClass());
    return Arrays.equals(array1, array2);
  }

  private static void doTestNewArrayEquals(Object[] expected, int length) {
    checkArrayEquals(expected, ObjectArrays.newArray(expected, length));
  }
}

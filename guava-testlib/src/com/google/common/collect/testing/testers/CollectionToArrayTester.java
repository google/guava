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

package com.google.common.collect.testing.testers;

import static com.google.common.collect.testing.features.CollectionFeature.KNOWN_ORDER;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.AbstractCollectionTester;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.WrongType;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@code toArray()} operations on a collection. Can't be invoked
 * directly; please see {@link com.google.common.collect.testing.CollectionTestSuiteBuilder}.
 *
 * @author Kevin Bourrillion
 * @author Chris Povirk
 */
@GwtCompatible(emulated = true)
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class CollectionToArrayTester<E> extends AbstractCollectionTester<E> {
  public void testToArray_noArgs() {
    Object[] array = collection.toArray();
    expectArrayContentsAnyOrder(createSamplesArray(), array);
  }

  /**
   * {@link Collection#toArray(Object[])} says: "Note that {@code toArray(new Object[0])} is
   * identical in function to {@code toArray()}."
   *
   * <p>For maximum effect, the collection under test should be created from an element array of a
   * type other than {@code Object[]}.
   */
  public void testToArray_isPlainObjectArray() {
    Object[] array = collection.toArray();
    assertEquals(Object[].class, array.getClass());
  }

  public void testToArray_emptyArray() {
    E[] empty = getSubjectGenerator().createArray(0);
    E[] array = collection.toArray(empty);
    assertEquals(
        "toArray(emptyT[]) should return an array of type T", empty.getClass(), array.getClass());
    assertEquals("toArray(emptyT[]).length:", getNumElements(), array.length);
    expectArrayContentsAnyOrder(createSamplesArray(), array);
  }

  @CollectionFeature.Require(KNOWN_ORDER)
  public void testToArray_emptyArray_ordered() {
    E[] empty = getSubjectGenerator().createArray(0);
    E[] array = collection.toArray(empty);
    assertEquals(
        "toArray(emptyT[]) should return an array of type T", empty.getClass(), array.getClass());
    assertEquals("toArray(emptyT[]).length:", getNumElements(), array.length);
    expectArrayContentsInOrder(getOrderedElements(), array);
  }

  public void testToArray_emptyArrayOfObject() {
    Object[] in = new Object[0];
    Object[] array = collection.toArray(in);
    assertEquals(
        "toArray(emptyObject[]) should return an array of type Object",
        Object[].class,
        array.getClass());
    assertEquals("toArray(emptyObject[]).length", getNumElements(), array.length);
    expectArrayContentsAnyOrder(createSamplesArray(), array);
  }

  public void testToArray_rightSizedArray() {
    E[] array = getSubjectGenerator().createArray(getNumElements());
    assertSame(
        "toArray(sameSizeE[]) should return the given array", array, collection.toArray(array));
    expectArrayContentsAnyOrder(createSamplesArray(), array);
  }

  @CollectionFeature.Require(KNOWN_ORDER)
  public void testToArray_rightSizedArray_ordered() {
    E[] array = getSubjectGenerator().createArray(getNumElements());
    assertSame(
        "toArray(sameSizeE[]) should return the given array", array, collection.toArray(array));
    expectArrayContentsInOrder(getOrderedElements(), array);
  }

  public void testToArray_rightSizedArrayOfObject() {
    Object[] array = new Object[getNumElements()];
    assertSame(
        "toArray(sameSizeObject[]) should return the given array",
        array,
        collection.toArray(array));
    expectArrayContentsAnyOrder(createSamplesArray(), array);
  }

  @CollectionFeature.Require(KNOWN_ORDER)
  public void testToArray_rightSizedArrayOfObject_ordered() {
    Object[] array = new Object[getNumElements()];
    assertSame(
        "toArray(sameSizeObject[]) should return the given array",
        array,
        collection.toArray(array));
    expectArrayContentsInOrder(getOrderedElements(), array);
  }

  public void testToArray_oversizedArray() {
    E[] array = getSubjectGenerator().createArray(getNumElements() + 2);
    array[getNumElements()] = e3();
    array[getNumElements() + 1] = e3();
    assertSame(
        "toArray(overSizedE[]) should return the given array", array, collection.toArray(array));

    List<E> subArray = Arrays.asList(array).subList(0, getNumElements());
    E[] expectedSubArray = createSamplesArray();
    for (int i = 0; i < getNumElements(); i++) {
      assertTrue(
          "toArray(overSizedE[]) should contain element " + expectedSubArray[i],
          subArray.contains(expectedSubArray[i]));
    }
    assertNull(
        "The array element immediately following the end of the collection should be nulled",
        array[getNumElements()]);
    // array[getNumElements() + 1] might or might not have been nulled
  }

  @CollectionFeature.Require(KNOWN_ORDER)
  public void testToArray_oversizedArray_ordered() {
    E[] array = getSubjectGenerator().createArray(getNumElements() + 2);
    array[getNumElements()] = e3();
    array[getNumElements() + 1] = e3();
    assertSame(
        "toArray(overSizedE[]) should return the given array", array, collection.toArray(array));

    List<E> expected = getOrderedElements();
    for (int i = 0; i < getNumElements(); i++) {
      assertEquals(expected.get(i), array[i]);
    }
    assertNull(
        "The array element immediately following the end of the collection should be nulled",
        array[getNumElements()]);
    // array[getNumElements() + 1] might or might not have been nulled
  }

  @CollectionSize.Require(absent = ZERO)
  public void testToArray_emptyArrayOfWrongTypeForNonEmptyCollection() {
    try {
      WrongType[] array = new WrongType[0];
      collection.toArray(array);
      fail("toArray(notAssignableTo[]) should throw");
    } catch (ArrayStoreException expected) {
    }
  }

  @CollectionSize.Require(ZERO)
  public void testToArray_emptyArrayOfWrongTypeForEmptyCollection() {
    WrongType[] array = new WrongType[0];
    assertSame(
        "toArray(sameSizeNotAssignableTo[]) should return the given array",
        array,
        collection.toArray(array));
  }

  private void expectArrayContentsAnyOrder(Object[] expected, Object[] actual) {
    Helpers.assertEqualIgnoringOrder(Arrays.asList(expected), Arrays.asList(actual));
  }

  private void expectArrayContentsInOrder(List<E> expected, Object[] actual) {
    assertEquals("toArray() ordered contents: ", expected, Arrays.asList(actual));
  }

  /**
   * Returns the {@link Method} instance for {@link #testToArray_isPlainObjectArray()} so that tests
   * of {@link Arrays#asList(Object[])} can suppress it with {@code
   * FeatureSpecificTestSuiteBuilder.suppressing()} until <a
   * href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6260652">Sun bug 6260652</a> is fixed.
   */
  @GwtIncompatible // reflection
  public static Method getToArrayIsPlainObjectArrayMethod() {
    return Helpers.getMethod(CollectionToArrayTester.class, "testToArray_isPlainObjectArray");
  }
}

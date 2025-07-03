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

import static com.google.common.collect.testing.Helpers.assertEqualIgnoringOrder;
import static com.google.common.collect.testing.Helpers.getMethod;
import static com.google.common.collect.testing.features.CollectionFeature.KNOWN_ORDER;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.testers.ReflectionFreeAssertThrows.assertThrows;
import static java.util.Arrays.asList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.testing.AbstractCollectionTester;
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
@Ignore("test runners must not instantiate and run this directly, only via suites we build")
// @Ignore affects the Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
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

    List<E> subArray = asList(array).subList(0, getNumElements());
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
    assertThrows(
        ArrayStoreException.class,
        () -> {
          WrongType[] array = new WrongType[0];
          collection.toArray(array);
        });
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
    assertEqualIgnoringOrder(asList(expected), asList(actual));
  }

  private void expectArrayContentsInOrder(List<E> expected, Object[] actual) {
    assertEquals("toArray() ordered contents: ", expected, asList(actual));
  }

  /**
   * Returns the {@link Method} instance for {@link #testToArray_isPlainObjectArray()} so that tests
   * of {@link Arrays#asList(Object[])} can suppress it with {@code
   * FeatureSpecificTestSuiteBuilder.suppressing()} until <a
   * href="https://bugs.openjdk.org/browse/JDK-6260652">JDK-6260652</a> is fixed.
   */
  @J2ktIncompatible
  @GwtIncompatible // reflection
  public static Method getToArrayIsPlainObjectArrayMethod() {
    return getMethod(CollectionToArrayTester.class, "testToArray_isPlainObjectArray");
  }
}

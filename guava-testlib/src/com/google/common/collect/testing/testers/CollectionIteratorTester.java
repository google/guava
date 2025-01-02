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

package com.google.common.collect.testing.testers;

import static com.google.common.collect.testing.Helpers.assertEqualIgnoringOrder;
import static com.google.common.collect.testing.Helpers.copyToList;
import static com.google.common.collect.testing.Helpers.mapEntry;
import static com.google.common.collect.testing.IteratorFeature.MODIFIABLE;
import static com.google.common.collect.testing.IteratorFeature.UNMODIFIABLE;
import static com.google.common.collect.testing.features.CollectionFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.CollectionFeature.KNOWN_ORDER;
import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_ITERATOR_REMOVE;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.testers.ReflectionFreeAssertThrows.assertThrows;
import static java.util.Arrays.asList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.AbstractCollectionTester;
import com.google.common.collect.testing.IteratorFeature;
import com.google.common.collect.testing.IteratorTester;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@code iterator} operations on a collection. Can't be invoked
 * directly; please see {@link com.google.common.collect.testing.CollectionTestSuiteBuilder}.
 *
 * @author Chris Povirk
 */
@GwtCompatible(emulated = true)
@Ignore("test runners must not instantiate and run this directly, only via suites we build")
// @Ignore affects the Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
@NullMarked
public class CollectionIteratorTester<E extends @Nullable Object>
    extends AbstractCollectionTester<E> {
  public void testIterator() {
    List<E> iteratorElements = new ArrayList<>();
    for (E element : collection) { // uses iterator()
      iteratorElements.add(element);
    }
    assertEqualIgnoringOrder(asList(createSamplesArray()), iteratorElements);
  }

  @CollectionFeature.Require(KNOWN_ORDER)
  public void testIterationOrdering() {
    List<E> iteratorElements = new ArrayList<>();
    for (E element : collection) { // uses iterator()
      iteratorElements.add(element);
    }
    List<E> expected = copyToList(getOrderedElements());
    assertEquals("Different ordered iteration", expected, iteratorElements);
  }

  @CollectionFeature.Require(ALLOWS_NULL_VALUES)
  @CollectionSize.Require(absent = ZERO)
  public void testIterator_nullElement() {
    initCollectionWithNullElement();
    List<E> iteratorElements = new ArrayList<>();
    for (E element : collection) { // uses iterator()
      iteratorElements.add(element);
    }
    assertEqualIgnoringOrder(asList(createArrayWithNullElement()), iteratorElements);
  }

  @CollectionFeature.Require(SUPPORTS_ITERATOR_REMOVE)
  @CollectionSize.Require(absent = ZERO)
  public void testIterator_removeAffectsBackingCollection() {
    int originalSize = collection.size();
    Iterator<E> iterator = collection.iterator();
    Object element = iterator.next();
    // If it's an Entry, it may become invalid once it's removed from the Map. Copy it.
    if (element instanceof Entry) {
      Entry<?, ?> entry = (Entry<?, ?>) element;
      element = mapEntry(entry.getKey(), entry.getValue());
    }
    assertTrue(collection.contains(element)); // sanity check
    iterator.remove();
    assertFalse(collection.contains(element));
    assertEquals(originalSize - 1, collection.size());
  }

  @CollectionFeature.Require({KNOWN_ORDER, SUPPORTS_ITERATOR_REMOVE})
  public void testIterator_knownOrderRemoveSupported() {
    runIteratorTest(MODIFIABLE, IteratorTester.KnownOrder.KNOWN_ORDER, getOrderedElements());
  }

  @CollectionFeature.Require(value = KNOWN_ORDER, absent = SUPPORTS_ITERATOR_REMOVE)
  public void testIterator_knownOrderRemoveUnsupported() {
    runIteratorTest(UNMODIFIABLE, IteratorTester.KnownOrder.KNOWN_ORDER, getOrderedElements());
  }

  @CollectionFeature.Require(absent = KNOWN_ORDER, value = SUPPORTS_ITERATOR_REMOVE)
  public void testIterator_unknownOrderRemoveSupported() {
    runIteratorTest(MODIFIABLE, IteratorTester.KnownOrder.UNKNOWN_ORDER, getSampleElements());
  }

  @CollectionFeature.Require(absent = {KNOWN_ORDER, SUPPORTS_ITERATOR_REMOVE})
  public void testIterator_unknownOrderRemoveUnsupported() {
    runIteratorTest(UNMODIFIABLE, IteratorTester.KnownOrder.UNKNOWN_ORDER, getSampleElements());
  }

  private void runIteratorTest(
      Set<IteratorFeature> features, IteratorTester.KnownOrder knownOrder, Iterable<E> elements) {
    new IteratorTester<E>(
        Platform.collectionIteratorTesterNumIterations(), features, elements, knownOrder) {
      @Override
      protected Iterator<E> newTargetIterator() {
        resetCollection();
        return collection.iterator();
      }

      @Override
      protected void verify(List<E> elements) {
        expectContents(elements);
      }
    }.test();
  }

  public void testIteratorNoSuchElementException() {
    Iterator<E> iterator = collection.iterator();
    while (iterator.hasNext()) {
      iterator.next();
    }

    assertThrows(NoSuchElementException.class, () -> iterator.next());
  }
}

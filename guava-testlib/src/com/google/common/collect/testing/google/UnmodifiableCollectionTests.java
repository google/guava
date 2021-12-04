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

package com.google.common.collect.testing.google;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterators;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A series of tests that support asserting that collections cannot be modified, either through
 * direct or indirect means.
 *
 * @author Robert Konigsberg
 */
@GwtCompatible
public class UnmodifiableCollectionTests {

  public static void assertMapEntryIsUnmodifiable(Entry<?, ?> entry) {
    try {
      entry.setValue(null);
      fail("setValue on unmodifiable Map.Entry succeeded");
    } catch (UnsupportedOperationException expected) {
    }
  }

  /**
   * Verifies that an Iterator is unmodifiable.
   *
   * <p>This test only works with iterators that iterate over a finite set.
   */
  public static void assertIteratorIsUnmodifiable(Iterator<?> iterator) {
    while (iterator.hasNext()) {
      iterator.next();
      try {
        iterator.remove();
        fail("Remove on unmodifiable iterator succeeded");
      } catch (UnsupportedOperationException expected) {
      }
    }
  }

  /**
   * Asserts that two iterators contain elements in tandem.
   *
   * <p>This test only works with iterators that iterate over a finite set.
   */
  public static void assertIteratorsInOrder(
      Iterator<?> expectedIterator, Iterator<?> actualIterator) {
    int i = 0;
    while (expectedIterator.hasNext()) {
      Object expected = expectedIterator.next();

      assertTrue(
          "index " + i + " expected <" + expected + "., actual is exhausted",
          actualIterator.hasNext());

      Object actual = actualIterator.next();
      assertEquals("index " + i, expected, actual);
      i++;
    }
    if (actualIterator.hasNext()) {
      fail("index " + i + ", expected is exhausted, actual <" + actualIterator.next() + ">");
    }
  }

  /**
   * Verifies that a collection is immutable.
   *
   * <p>A collection is considered immutable if:
   *
   * <ol>
   *   <li>All its mutation methods result in UnsupportedOperationException, and do not change the
   *       underlying contents.
   *   <li>All methods that return objects that can indirectly mutate the collection throw
   *       UnsupportedOperationException when those mutators are called.
   * </ol>
   *
   * @param collection the presumed-immutable collection
   * @param sampleElement an element of the same type as that contained by {@code collection}.
   *     {@code collection} may or may not have {@code sampleElement} as a member.
   */
  public static <E> void assertCollectionIsUnmodifiable(Collection<E> collection, E sampleElement) {
    Collection<E> siblingCollection = new ArrayList<>();
    siblingCollection.add(sampleElement);

    Collection<E> copy = new ArrayList<>();
    // Avoid copy.addAll(collection), which runs afoul of an Android bug in older versions:
    // http://b.android.com/72073 http://r.android.com/98929
    Iterators.addAll(copy, collection.iterator());

    try {
      collection.add(sampleElement);
      fail("add succeeded on unmodifiable collection");
    } catch (UnsupportedOperationException expected) {
    }

    assertCollectionsAreEquivalent(copy, collection);

    try {
      collection.addAll(siblingCollection);
      fail("addAll succeeded on unmodifiable collection");
    } catch (UnsupportedOperationException expected) {
    }
    assertCollectionsAreEquivalent(copy, collection);

    try {
      collection.clear();
      fail("clear succeeded on unmodifiable collection");
    } catch (UnsupportedOperationException expected) {
    }
    assertCollectionsAreEquivalent(copy, collection);

    assertIteratorIsUnmodifiable(collection.iterator());
    assertCollectionsAreEquivalent(copy, collection);

    try {
      collection.remove(sampleElement);
      fail("remove succeeded on unmodifiable collection");
    } catch (UnsupportedOperationException expected) {
    }
    assertCollectionsAreEquivalent(copy, collection);

    try {
      collection.removeAll(siblingCollection);
      fail("removeAll succeeded on unmodifiable collection");
    } catch (UnsupportedOperationException expected) {
    }
    assertCollectionsAreEquivalent(copy, collection);

    try {
      collection.retainAll(siblingCollection);
      fail("retainAll succeeded on unmodifiable collection");
    } catch (UnsupportedOperationException expected) {
    }
    assertCollectionsAreEquivalent(copy, collection);
  }

  /**
   * Verifies that a set is immutable.
   *
   * <p>A set is considered immutable if:
   *
   * <ol>
   *   <li>All its mutation methods result in UnsupportedOperationException, and do not change the
   *       underlying contents.
   *   <li>All methods that return objects that can indirectly mutate the set throw
   *       UnsupportedOperationException when those mutators are called.
   * </ol>
   *
   * @param set the presumed-immutable set
   * @param sampleElement an element of the same type as that contained by {@code set}. {@code set}
   *     may or may not have {@code sampleElement} as a member.
   */
  public static <E> void assertSetIsUnmodifiable(Set<E> set, E sampleElement) {
    assertCollectionIsUnmodifiable(set, sampleElement);
  }

  /**
   * Verifies that a multiset is immutable.
   *
   * <p>A multiset is considered immutable if:
   *
   * <ol>
   *   <li>All its mutation methods result in UnsupportedOperationException, and do not change the
   *       underlying contents.
   *   <li>All methods that return objects that can indirectly mutate the multiset throw
   *       UnsupportedOperationException when those mutators are called.
   * </ol>
   *
   * @param multiset the presumed-immutable multiset
   * @param sampleElement an element of the same type as that contained by {@code multiset}. {@code
   *     multiset} may or may not have {@code sampleElement} as a member.
   */
  public static <E> void assertMultisetIsUnmodifiable(Multiset<E> multiset, E sampleElement) {
    Multiset<E> copy = LinkedHashMultiset.create(multiset);
    assertCollectionsAreEquivalent(multiset, copy);

    // Multiset is a collection, so we can use all those tests.
    assertCollectionIsUnmodifiable(multiset, sampleElement);

    assertCollectionsAreEquivalent(multiset, copy);

    try {
      multiset.add(sampleElement, 2);
      fail("add(Object, int) succeeded on unmodifiable collection");
    } catch (UnsupportedOperationException expected) {
    }
    assertCollectionsAreEquivalent(multiset, copy);

    try {
      multiset.remove(sampleElement, 2);
      fail("remove(Object, int) succeeded on unmodifiable collection");
    } catch (UnsupportedOperationException expected) {
    }
    assertCollectionsAreEquivalent(multiset, copy);

    assertCollectionsAreEquivalent(multiset, copy);

    assertSetIsUnmodifiable(multiset.elementSet(), sampleElement);
    assertCollectionsAreEquivalent(multiset, copy);

    assertSetIsUnmodifiable(
        multiset.entrySet(),
        new Multiset.Entry<E>() {
          @Override
          public int getCount() {
            return 1;
          }

          @Override
          public E getElement() {
            return sampleElement;
          }
        });
    assertCollectionsAreEquivalent(multiset, copy);
  }

  /**
   * Verifies that a multimap is immutable.
   *
   * <p>A multimap is considered immutable if:
   *
   * <ol>
   *   <li>All its mutation methods result in UnsupportedOperationException, and do not change the
   *       underlying contents.
   *   <li>All methods that return objects that can indirectly mutate the multimap throw
   *       UnsupportedOperationException when those mutators
   * </ol>
   *
   * @param multimap the presumed-immutable multimap
   * @param sampleKey a key of the same type as that contained by {@code multimap}. {@code multimap}
   *     may or may not have {@code sampleKey} as a key.
   * @param sampleValue a key of the same type as that contained by {@code multimap}. {@code
   *     multimap} may or may not have {@code sampleValue} as a key.
   */
  public static <K, V> void assertMultimapIsUnmodifiable(
      Multimap<K, V> multimap, K sampleKey, V sampleValue) {
    List<Entry<K, V>> originalEntries =
        Collections.unmodifiableList(Lists.newArrayList(multimap.entries()));

    assertMultimapRemainsUnmodified(multimap, originalEntries);

    Collection<V> sampleValueAsCollection = Collections.singleton(sampleValue);

    // Test #clear()
    try {
      multimap.clear();
      fail("clear succeeded on unmodifiable multimap");
    } catch (UnsupportedOperationException expected) {
    }

    assertMultimapRemainsUnmodified(multimap, originalEntries);

    // Test asMap().entrySet()
    assertSetIsUnmodifiable(
        multimap.asMap().entrySet(), Maps.immutableEntry(sampleKey, sampleValueAsCollection));

    // Test #values()

    assertMultimapRemainsUnmodified(multimap, originalEntries);
    if (!multimap.isEmpty()) {
      Collection<V> values = multimap.asMap().entrySet().iterator().next().getValue();

      assertCollectionIsUnmodifiable(values, sampleValue);
    }

    // Test #entries()
    assertCollectionIsUnmodifiable(multimap.entries(), Maps.immutableEntry(sampleKey, sampleValue));
    assertMultimapRemainsUnmodified(multimap, originalEntries);

    // Iterate over every element in the entry set
    for (Entry<K, V> entry : multimap.entries()) {
      assertMapEntryIsUnmodifiable(entry);
    }
    assertMultimapRemainsUnmodified(multimap, originalEntries);

    // Test #keys()
    assertMultisetIsUnmodifiable(multimap.keys(), sampleKey);
    assertMultimapRemainsUnmodified(multimap, originalEntries);

    // Test #keySet()
    assertSetIsUnmodifiable(multimap.keySet(), sampleKey);
    assertMultimapRemainsUnmodified(multimap, originalEntries);

    // Test #get()
    if (!multimap.isEmpty()) {
      K key = multimap.keySet().iterator().next();
      assertCollectionIsUnmodifiable(multimap.get(key), sampleValue);
      assertMultimapRemainsUnmodified(multimap, originalEntries);
    }

    // Test #put()
    try {
      multimap.put(sampleKey, sampleValue);
      fail("put succeeded on unmodifiable multimap");
    } catch (UnsupportedOperationException expected) {
    }
    assertMultimapRemainsUnmodified(multimap, originalEntries);

    // Test #putAll(K, Collection<V>)
    try {
      multimap.putAll(sampleKey, sampleValueAsCollection);
      fail("putAll(K, Iterable) succeeded on unmodifiable multimap");
    } catch (UnsupportedOperationException expected) {
    }
    assertMultimapRemainsUnmodified(multimap, originalEntries);

    // Test #putAll(Multimap<K, V>)
    Multimap<K, V> multimap2 = ArrayListMultimap.create();
    multimap2.put(sampleKey, sampleValue);
    try {
      multimap.putAll(multimap2);
      fail("putAll(Multimap<K, V>) succeeded on unmodifiable multimap");
    } catch (UnsupportedOperationException expected) {
    }
    assertMultimapRemainsUnmodified(multimap, originalEntries);

    // Test #remove()
    try {
      multimap.remove(sampleKey, sampleValue);
      fail("remove succeeded on unmodifiable multimap");
    } catch (UnsupportedOperationException expected) {
    }
    assertMultimapRemainsUnmodified(multimap, originalEntries);

    // Test #removeAll()
    try {
      multimap.removeAll(sampleKey);
      fail("removeAll succeeded on unmodifiable multimap");
    } catch (UnsupportedOperationException expected) {
    }
    assertMultimapRemainsUnmodified(multimap, originalEntries);

    // Test #replaceValues()
    try {
      multimap.replaceValues(sampleKey, sampleValueAsCollection);
      fail("replaceValues succeeded on unmodifiable multimap");
    } catch (UnsupportedOperationException expected) {
    }
    assertMultimapRemainsUnmodified(multimap, originalEntries);

    // Test #asMap()
    try {
      multimap.asMap().remove(sampleKey);
      fail("asMap().remove() succeeded on unmodifiable multimap");
    } catch (UnsupportedOperationException expected) {
    }
    assertMultimapRemainsUnmodified(multimap, originalEntries);

    if (!multimap.isEmpty()) {
      K presentKey = multimap.keySet().iterator().next();
      try {
        multimap.asMap().get(presentKey).remove(sampleValue);
        fail("asMap().get().remove() succeeded on unmodifiable multimap");
      } catch (UnsupportedOperationException expected) {
      }
      assertMultimapRemainsUnmodified(multimap, originalEntries);

      try {
        multimap.asMap().values().iterator().next().remove(sampleValue);
        fail("asMap().values().iterator().next().remove() succeeded on unmodifiable multimap");
      } catch (UnsupportedOperationException expected) {
      }

      try {
        ((Collection<?>) multimap.asMap().values().toArray()[0]).clear();
        fail("asMap().values().toArray()[0].clear() succeeded on unmodifiable multimap");
      } catch (UnsupportedOperationException expected) {
      }
    }

    assertCollectionIsUnmodifiable(multimap.values(), sampleValue);
    assertMultimapRemainsUnmodified(multimap, originalEntries);
  }

  private static <E> void assertCollectionsAreEquivalent(
      Collection<E> expected, Collection<E> actual) {
    assertIteratorsInOrder(expected.iterator(), actual.iterator());
  }

  private static <K, V> void assertMultimapRemainsUnmodified(
      Multimap<K, V> expected, List<Entry<K, V>> actual) {
    assertIteratorsInOrder(expected.entries().iterator(), actual.iterator());
  }
}

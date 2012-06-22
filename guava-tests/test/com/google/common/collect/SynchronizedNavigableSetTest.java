/*
 * Copyright (C) 2010 The Guava Authors
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

import com.google.common.collect.Synchronized.SynchronizedNavigableSet;
import com.google.common.collect.Synchronized.SynchronizedSortedSet;
import com.google.common.collect.testing.NavigableSetTestSuiteBuilder;
import com.google.common.collect.testing.SafeTreeSet;
import com.google.common.collect.testing.TestStringSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.testing.SerializableTester;

import junit.framework.TestSuite;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Tests for {@link Sets#synchronizedNavigableSet(NavigableSet)}.
 *
 * @author Louis Wasserman
 */
public class SynchronizedNavigableSetTest extends SynchronizedSetTest {
  @SuppressWarnings("unchecked")
      @Override protected <E> NavigableSet<E> create() {
    TestSet<E> inner = new TestSet<E>(
        new TreeSet<E>((Comparator<E>) Ordering.natural().nullsFirst()), mutex);
    NavigableSet<E> outer =
        Synchronized.navigableSet(inner, mutex);
    return outer;
  }

  static class TestSet<E> extends SynchronizedSetTest.TestSet<E>
      implements NavigableSet<E> {

    TestSet(NavigableSet<E> delegate, Object mutex) {
      super(delegate, mutex);
    }

    @Override protected NavigableSet<E> delegate() {
      return (NavigableSet<E>) super.delegate();
    }

    @Override public E ceiling(E e) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().ceiling(e);
    }

    @Override public Iterator<E> descendingIterator() {
      return delegate().descendingIterator();
    }

    @Override public NavigableSet<E> descendingSet() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().descendingSet();
    }

    @Override public E floor(E e) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().floor(e);
    }

    @Override public NavigableSet<E> headSet(E toElement, boolean inclusive) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().headSet(toElement, inclusive);
    }

    @Override public SortedSet<E> headSet(E toElement) {
      return headSet(toElement, false);
    }

    @Override public E higher(E e) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().higher(e);
    }

    @Override public E lower(E e) {
      return delegate().lower(e);
    }

    @Override public E pollFirst() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().pollFirst();
    }

    @Override public E pollLast() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().pollLast();
    }

    @Override public NavigableSet<E> subSet(E fromElement,
        boolean fromInclusive, E toElement, boolean toInclusive) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().subSet(
          fromElement, fromInclusive, toElement, toInclusive);
    }

    @Override public SortedSet<E> subSet(E fromElement, E toElement) {
      return subSet(fromElement, true, toElement, false);
    }

    @Override public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().tailSet(fromElement, inclusive);
    }

    @Override public SortedSet<E> tailSet(E fromElement) {
      return tailSet(fromElement, true);
    }

    @Override public Comparator<? super E> comparator() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().comparator();
    }

    @Override public E first() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().first();
    }

    @Override public E last() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate().last();
    }

    private static final long serialVersionUID = 0;
  }

  public static TestSuite suite() {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(SynchronizedNavigableSetTest.class);
    suite.addTest(
        NavigableSetTestSuiteBuilder.using(new TestStringSetGenerator() {
          private final Object mutex = new Integer(1);

          @Override protected Set<String> create(String[] elements) {
            NavigableSet<String> innermost = new SafeTreeSet<String>();
            innermost.addAll(Arrays.asList(elements));
            TestSet<String> inner = new TestSet<String>(innermost, mutex);
            NavigableSet<String> outer =
                Synchronized.navigableSet(inner, mutex);
            return outer;
          }

          @Override public List<String> order(List<String> insertionOrder) {
            return Ordering.natural().sortedCopy(insertionOrder);
          }
        }).named("Sets.synchronizedNavigableSet[SafeTreeSet]")
            .withFeatures(CollectionSize.ANY, CollectionFeature.KNOWN_ORDER,
                CollectionFeature.GENERAL_PURPOSE).createTestSuite());

    return suite;
  }

  public void testComparator() {
    create().comparator();
  }

  public void testCeiling() {
    create().ceiling("a");
  }

  public void testFloor() {
    create().floor("a");
  }

  public void testHigher() {
    create().higher("a");
  }

  public void testLower() {
    create().lower("a");
  }

  public void testDescendingSet() {
    NavigableSet<String> map = create();
    NavigableSet<String> descendingSet = map.descendingSet();
    assertTrue(descendingSet instanceof SynchronizedNavigableSet);
    assertSame(mutex, ((SynchronizedNavigableSet<String>) descendingSet).mutex);
  }

  public void testFirst() {
    NavigableSet<String> set = create();
    set.add("a");
    set.first();
  }

  public void testPollFirst() {
    create().pollFirst();
  }

  public void testLast() {
    NavigableSet<String> set = create();
    set.add("a");
    set.last();
  }

  public void testPollLast() {
    create().pollLast();
  }

  public void testHeadSet_E() {
    NavigableSet<String> map = create();
    SortedSet<String> headSet = map.headSet("a");
    assertTrue(headSet instanceof SynchronizedSortedSet);
    assertSame(mutex, ((SynchronizedSortedSet<String>) headSet).mutex);
  }

  public void testHeadSet_E_B() {
    NavigableSet<String> map = create();
    NavigableSet<String> headSet = map.headSet("a", true);
    assertTrue(headSet instanceof SynchronizedNavigableSet);
    assertSame(mutex, ((SynchronizedNavigableSet<String>) headSet).mutex);
  }

  public void testSubSet_E_E() {
    NavigableSet<String> map = create();
    SortedSet<String> subSet = map.subSet("a", "b");
    assertTrue(subSet instanceof SynchronizedSortedSet);
    assertSame(mutex, ((SynchronizedSortedSet<String>) subSet).mutex);
  }

  public void testSubSet_E_B_E_B() {
    NavigableSet<String> map = create();
    NavigableSet<String> subSet = map.subSet("a", false, "b", true);
    assertTrue(subSet instanceof SynchronizedNavigableSet);
    assertSame(mutex, ((SynchronizedNavigableSet<String>) subSet).mutex);
  }

  public void testTailSet_E() {
    NavigableSet<String> map = create();
    SortedSet<String> tailSet = map.tailSet("a");
    assertTrue(tailSet instanceof SynchronizedSortedSet);
    assertSame(mutex, ((SynchronizedSortedSet<String>) tailSet).mutex);
  }

  public void testTailSet_E_B() {
    NavigableSet<String> map = create();
    NavigableSet<String> tailSet = map.tailSet("a", true);
    assertTrue(tailSet instanceof SynchronizedNavigableSet);
    assertSame(mutex, ((SynchronizedNavigableSet<String>) tailSet).mutex);
  }

  @Override public void testSerialization() {
    SerializableTester.reserializeAndAssert(create());
  }
}

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

import static com.google.common.collect.MapMakerInternalMap.Strength.STRONG;
import static com.google.common.collect.MapMakerInternalMap.Strength.WEAK;
import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Equivalence;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.google.MultisetTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringMultisetGenerator;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test case for {@link ConcurrentHashMultiset}.
 *
 * @author Cliff L. Biffle
 * @author mike nonemacher
 */
public class ConcurrentHashMultisetTest extends TestCase {

  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(
        MultisetTestSuiteBuilder.using(concurrentHashMultisetGenerator())
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.GENERAL_PURPOSE,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .named("ConcurrentHashMultiset")
            .createTestSuite());
    suite.addTest(
        MultisetTestSuiteBuilder.using(concurrentSkipListMultisetGenerator())
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.GENERAL_PURPOSE,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .named("ConcurrentSkipListMultiset")
            .createTestSuite());
    suite.addTestSuite(ConcurrentHashMultisetTest.class);
    return suite;
  }

  private static TestStringMultisetGenerator concurrentHashMultisetGenerator() {
    return new TestStringMultisetGenerator() {
      @Override
      protected Multiset<String> create(String[] elements) {
        return ConcurrentHashMultiset.create(asList(elements));
      }
    };
  }

  private static TestStringMultisetGenerator concurrentSkipListMultisetGenerator() {
    return new TestStringMultisetGenerator() {
      @Override
      protected Multiset<String> create(String[] elements) {
        Multiset<String> multiset =
            new ConcurrentHashMultiset<>(new ConcurrentSkipListMap<String, AtomicInteger>());
        Collections.addAll(multiset, elements);
        return multiset;
      }

      @Override
      public List<String> order(List<String> insertionOrder) {
        return Ordering.natural().sortedCopy(insertionOrder);
      }
    };
  }

  private static final String KEY = "puppies";

  ConcurrentMap<String, AtomicInteger> backingMap;
  ConcurrentHashMultiset<String> multiset;

  @SuppressWarnings("unchecked")
  @Override
  protected void setUp() {
    backingMap = mock(ConcurrentMap.class);
    when(backingMap.isEmpty()).thenReturn(true);

    multiset = new ConcurrentHashMultiset<>(backingMap);
  }

  public void testCount_elementPresent() {
    final int COUNT = 12;
    when(backingMap.get(KEY)).thenReturn(new AtomicInteger(COUNT));

    assertEquals(COUNT, multiset.count(KEY));
  }

  public void testCount_elementAbsent() {
    when(backingMap.get(KEY)).thenReturn(null);

    assertEquals(0, multiset.count(KEY));
  }

  public void testAdd_zero() {
    final int INITIAL_COUNT = 32;

    when(backingMap.get(KEY)).thenReturn(new AtomicInteger(INITIAL_COUNT));
    assertEquals(INITIAL_COUNT, multiset.add(KEY, 0));
  }

  public void testAdd_firstFewWithSuccess() {
    final int COUNT = 400;

    when(backingMap.get(KEY)).thenReturn(null);
    when(backingMap.putIfAbsent(eq(KEY), isA(AtomicInteger.class))).thenReturn(null);

    assertEquals(0, multiset.add(KEY, COUNT));
  }

  public void testAdd_laterFewWithSuccess() {
    int INITIAL_COUNT = 32;
    int COUNT_TO_ADD = 400;

    AtomicInteger initial = new AtomicInteger(INITIAL_COUNT);
    when(backingMap.get(KEY)).thenReturn(initial);

    assertEquals(INITIAL_COUNT, multiset.add(KEY, COUNT_TO_ADD));
    assertEquals(INITIAL_COUNT + COUNT_TO_ADD, initial.get());
  }

  public void testAdd_laterFewWithOverflow() {
    final int INITIAL_COUNT = 92384930;
    final int COUNT_TO_ADD = Integer.MAX_VALUE - INITIAL_COUNT + 1;

    when(backingMap.get(KEY)).thenReturn(new AtomicInteger(INITIAL_COUNT));

    try {
      multiset.add(KEY, COUNT_TO_ADD);
      fail("Must reject arguments that would cause counter overflow.");
    } catch (IllegalArgumentException expected) {
    }
  }

  /**
   * Simulate some of the races that can happen on add. We can't easily simulate the race that
   * happens when an {@link AtomicInteger#compareAndSet} fails, but we can simulate the case where
   * the putIfAbsent returns a non-null value, and the case where the replace() of an observed zero
   * fails.
   */
  public void testAdd_withFailures() {
    AtomicInteger existing = new AtomicInteger(12);
    AtomicInteger existingZero = new AtomicInteger(0);

    // initial map.get()
    when(backingMap.get(KEY)).thenReturn(null);
    // since get returned null, try a putIfAbsent; that fails due to a simulated race
    when(backingMap.putIfAbsent(eq(KEY), isA(AtomicInteger.class))).thenReturn(existingZero);
    // since the putIfAbsent returned a zero, we'll try to replace...
    when(backingMap.replace(eq(KEY), eq(existingZero), isA(AtomicInteger.class))).thenReturn(false);
    // ...and then putIfAbsent. Simulate failure on both
    when(backingMap.putIfAbsent(eq(KEY), isA(AtomicInteger.class))).thenReturn(existing);

    // next map.get()
    when(backingMap.get(KEY)).thenReturn(existingZero);
    // since get returned zero, try a replace; that fails due to a simulated race
    when(backingMap.replace(eq(KEY), eq(existingZero), isA(AtomicInteger.class))).thenReturn(false);
    when(backingMap.putIfAbsent(eq(KEY), isA(AtomicInteger.class))).thenReturn(existing);

    // another map.get()
    when(backingMap.get(KEY)).thenReturn(existing);
    // we shouldn't see any more map operations; CHM will now just update the AtomicInteger

    assertEquals(12, multiset.add(KEY, 3));
    assertEquals(15, existing.get());
  }

  public void testRemove_zeroFromSome() {
    final int INITIAL_COUNT = 14;
    when(backingMap.get(KEY)).thenReturn(new AtomicInteger(INITIAL_COUNT));

    assertEquals(INITIAL_COUNT, multiset.remove(KEY, 0));
  }

  public void testRemove_zeroFromNone() {
    when(backingMap.get(KEY)).thenReturn(null);

    assertEquals(0, multiset.remove(KEY, 0));
  }

  public void testRemove_nonePresent() {
    when(backingMap.get(KEY)).thenReturn(null);

    assertEquals(0, multiset.remove(KEY, 400));
  }

  public void testRemove_someRemaining() {
    int countToRemove = 30;
    int countRemaining = 1;
    AtomicInteger current = new AtomicInteger(countToRemove + countRemaining);

    when(backingMap.get(KEY)).thenReturn(current);

    assertEquals(countToRemove + countRemaining, multiset.remove(KEY, countToRemove));
    assertEquals(countRemaining, current.get());
  }

  public void testRemove_noneRemaining() {
    int countToRemove = 30;
    AtomicInteger current = new AtomicInteger(countToRemove);

    when(backingMap.get(KEY)).thenReturn(current);
    // it's ok if removal fails: another thread may have done the remove
    when(backingMap.remove(KEY, current)).thenReturn(false);

    assertEquals(countToRemove, multiset.remove(KEY, countToRemove));
    assertEquals(0, current.get());
  }

  public void testRemoveExactly() {
    ConcurrentHashMultiset<String> cms = ConcurrentHashMultiset.create();
    cms.add("a", 2);
    cms.add("b", 3);

    try {
      cms.removeExactly("a", -2);
      fail();
    } catch (IllegalArgumentException expected) {
    }

    assertTrue(cms.removeExactly("a", 0));
    assertEquals(2, cms.count("a"));
    assertTrue(cms.removeExactly("c", 0));
    assertEquals(0, cms.count("c"));

    assertFalse(cms.removeExactly("a", 4));
    assertEquals(2, cms.count("a"));
    assertTrue(cms.removeExactly("a", 2));
    assertEquals(0, cms.count("a"));
    assertTrue(cms.removeExactly("b", 2));
    assertEquals(1, cms.count("b"));
  }

  public void testIteratorRemove_actualMap() {
    // Override to avoid using mocks.
    multiset = ConcurrentHashMultiset.create();

    multiset.add(KEY);
    multiset.add(KEY + "_2");
    multiset.add(KEY);

    int mutations = 0;
    for (Iterator<String> it = multiset.iterator(); it.hasNext(); ) {
      it.next();
      it.remove();
      mutations++;
    }
    assertTrue(multiset.isEmpty());
    assertEquals(3, mutations);
  }

  public void testSetCount_basic() {
    int initialCount = 20;
    int countToSet = 40;
    AtomicInteger current = new AtomicInteger(initialCount);

    when(backingMap.get(KEY)).thenReturn(current);

    assertEquals(initialCount, multiset.setCount(KEY, countToSet));
    assertEquals(countToSet, current.get());
  }

  public void testSetCount_asRemove() {
    int countToRemove = 40;
    AtomicInteger current = new AtomicInteger(countToRemove);

    when(backingMap.get(KEY)).thenReturn(current);
    when(backingMap.remove(KEY, current)).thenReturn(true);

    assertEquals(countToRemove, multiset.setCount(KEY, 0));
    assertEquals(0, current.get());
  }

  public void testSetCount_0_nonePresent() {
    when(backingMap.get(KEY)).thenReturn(null);

    assertEquals(0, multiset.setCount(KEY, 0));
  }

  public void testCreate() {
    ConcurrentHashMultiset<Integer> multiset = ConcurrentHashMultiset.create();
    assertTrue(multiset.isEmpty());
    reserializeAndAssert(multiset);
  }

  public void testCreateFromIterable() {
    Iterable<Integer> iterable = asList(1, 2, 2, 3, 4);
    ConcurrentHashMultiset<Integer> multiset = ConcurrentHashMultiset.create(iterable);
    assertEquals(2, multiset.count(2));
    reserializeAndAssert(multiset);
  }

  public void testIdentityKeyEquality_strongKeys() {
    testIdentityKeyEquality(STRONG);
  }

  public void testIdentityKeyEquality_weakKeys() {
    testIdentityKeyEquality(WEAK);
  }

  private void testIdentityKeyEquality(MapMakerInternalMap.Strength keyStrength) {

    ConcurrentMap<String, AtomicInteger> map =
        new MapMaker().setKeyStrength(keyStrength).keyEquivalence(Equivalence.identity()).makeMap();

    ConcurrentHashMultiset<String> multiset = ConcurrentHashMultiset.create(map);

    String s1 = new String("a");
    String s2 = new String("a");
    assertEquals(s1, s2); // Stating the obvious.
    assertTrue(s1 != s2); // Stating the obvious.

    multiset.add(s1);
    assertTrue(multiset.contains(s1));
    assertFalse(multiset.contains(s2));
    assertEquals(1, multiset.count(s1));
    assertEquals(0, multiset.count(s2));

    multiset.add(s1);
    multiset.add(s2, 3);
    assertEquals(2, multiset.count(s1));
    assertEquals(3, multiset.count(s2));

    multiset.remove(s1);
    assertEquals(1, multiset.count(s1));
    assertEquals(3, multiset.count(s2));
  }

  public void testLogicalKeyEquality_strongKeys() {
    testLogicalKeyEquality(STRONG);
  }

  public void testLogicalKeyEquality_weakKeys() {
    testLogicalKeyEquality(WEAK);
  }

  private void testLogicalKeyEquality(MapMakerInternalMap.Strength keyStrength) {

    ConcurrentMap<String, AtomicInteger> map =
        new MapMaker().setKeyStrength(keyStrength).keyEquivalence(Equivalence.equals()).makeMap();

    ConcurrentHashMultiset<String> multiset = ConcurrentHashMultiset.create(map);

    String s1 = new String("a");
    String s2 = new String("a");
    assertEquals(s1, s2); // Stating the obvious.

    multiset.add(s1);
    assertTrue(multiset.contains(s1));
    assertTrue(multiset.contains(s2));
    assertEquals(1, multiset.count(s1));
    assertEquals(1, multiset.count(s2));

    multiset.add(s2, 3);
    assertEquals(4, multiset.count(s1));
    assertEquals(4, multiset.count(s2));

    multiset.remove(s1);
    assertEquals(3, multiset.count(s1));
    assertEquals(3, multiset.count(s2));
  }

  public void testSerializationWithMapMaker1() {
    ConcurrentMap<String, AtomicInteger> map = new MapMaker().makeMap();
    multiset = ConcurrentHashMultiset.create(map);
    reserializeAndAssert(multiset);
  }

  public void testSerializationWithMapMaker2() {
    ConcurrentMap<String, AtomicInteger> map = new MapMaker().makeMap();
    multiset = ConcurrentHashMultiset.create(map);
    multiset.addAll(ImmutableList.of("a", "a", "b", "c", "d", "b"));
    reserializeAndAssert(multiset);
  }

  public void testSerializationWithMapMaker3() {
    ConcurrentMap<String, AtomicInteger> map = new MapMaker().makeMap();
    multiset = ConcurrentHashMultiset.create(map);
    multiset.addAll(ImmutableList.of("a", "a", "b", "c", "d", "b"));
    reserializeAndAssert(multiset);
  }

  public void testSerializationWithMapMaker_preservesIdentityKeyEquivalence() {
    ConcurrentMap<String, AtomicInteger> map =
        new MapMaker().keyEquivalence(Equivalence.identity()).makeMap();

    ConcurrentHashMultiset<String> multiset = ConcurrentHashMultiset.create(map);
    multiset = reserializeAndAssert(multiset);

    String s1 = new String("a");
    String s2 = new String("a");
    assertEquals(s1, s2); // Stating the obvious.
    assertTrue(s1 != s2); // Stating the obvious.

    multiset.add(s1);
    assertTrue(multiset.contains(s1));
    assertFalse(multiset.contains(s2));
    assertEquals(1, multiset.count(s1));
    assertEquals(0, multiset.count(s2));
  }
}

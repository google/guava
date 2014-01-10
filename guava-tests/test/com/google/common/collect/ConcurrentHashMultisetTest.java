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
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import com.google.common.base.Equivalence;
import com.google.common.collect.MapMaker.RemovalListener;
import com.google.common.collect.MapMaker.RemovalNotification;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.google.MultisetTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringMultisetGenerator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.easymock.EasyMock;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test case for {@link ConcurrentHashMultiset}.
 *
 * @author Cliff L. Biffle
 * @author mike nonemacher
 */
public class ConcurrentHashMultisetTest extends TestCase {

  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(MultisetTestSuiteBuilder.using(concurrentHashMultisetGenerator())
        .withFeatures(CollectionSize.ANY,
            CollectionFeature.GENERAL_PURPOSE,
            CollectionFeature.SERIALIZABLE,
            CollectionFeature.ALLOWS_NULL_QUERIES)
        .named("ConcurrentHashMultiset")
        .createTestSuite());
    suite.addTestSuite(ConcurrentHashMultisetTest.class);
    return suite;
  }

  private static TestStringMultisetGenerator concurrentHashMultisetGenerator() {
    return new TestStringMultisetGenerator() {
      @Override protected Multiset<String> create(String[] elements) {
        return ConcurrentHashMultiset.create(asList(elements));
      }
    };
  }

  private static final String KEY = "puppies";

  ConcurrentMap<String, AtomicInteger> backingMap;
  ConcurrentHashMultiset<String> multiset;

  @SuppressWarnings("unchecked")
  @Override protected void setUp() {
    backingMap = EasyMock.createMock(ConcurrentMap.class);
    expect(backingMap.isEmpty()).andReturn(true);
    replay();

    multiset = new ConcurrentHashMultiset<String>(backingMap);
    verify();
    reset();
  }

  public void testCount_elementPresent() {
    final int COUNT = 12;
    expect(backingMap.get(KEY)).andReturn(new AtomicInteger(COUNT));
    replay();

    assertEquals(COUNT, multiset.count(KEY));
    verify();
  }

  public void testCount_elementAbsent() {
    expect(backingMap.get(KEY)).andReturn(null);
    replay();

    assertEquals(0, multiset.count(KEY));
    verify();
  }

  public void testAdd_zero() {
    final int INITIAL_COUNT = 32;

    expect(backingMap.get(KEY)).andReturn(new AtomicInteger(INITIAL_COUNT));
    replay();
    assertEquals(INITIAL_COUNT, multiset.add(KEY, 0));
    verify();
  }

  public void testAdd_firstFewWithSuccess() {
    final int COUNT = 400;

    expect(backingMap.get(KEY)).andReturn(null);
    expect(backingMap.putIfAbsent(eq(KEY), isA(AtomicInteger.class))).andReturn(null);
    replay();

    assertEquals(0, multiset.add(KEY, COUNT));
    verify();
  }

  public void testAdd_laterFewWithSuccess() {
    int INITIAL_COUNT = 32;
    int COUNT_TO_ADD = 400;

    AtomicInteger initial = new AtomicInteger(INITIAL_COUNT);
    expect(backingMap.get(KEY)).andReturn(initial);
    replay();

    assertEquals(INITIAL_COUNT, multiset.add(KEY, COUNT_TO_ADD));
    assertEquals(INITIAL_COUNT + COUNT_TO_ADD, initial.get());
    verify();
  }

  public void testAdd_laterFewWithOverflow() {
    final int INITIAL_COUNT = 92384930;
    final int COUNT_TO_ADD = Integer.MAX_VALUE - INITIAL_COUNT + 1;

    expect(backingMap.get(KEY)).andReturn(new AtomicInteger(INITIAL_COUNT));
    replay();

    try {
      multiset.add(KEY, COUNT_TO_ADD);
      fail("Must reject arguments that would cause counter overflow.");
    } catch (IllegalArgumentException e) {
      // Expected.
    }
    verify();
  }

  /**
   * Simulate some of the races that can happen on add. We can't easily simulate the race that
   * happens when an {@link AtomicInteger#compareAndSet} fails, but we can simulate the case where
   * the putIfAbsent returns a non-null value, and the case where the replace() of an observed
   * zero fails.
   */
  public void testAdd_withFailures() {
    AtomicInteger existing = new AtomicInteger(12);
    AtomicInteger existingZero = new AtomicInteger(0);

    // initial map.get()
    expect(backingMap.get(KEY)).andReturn(null);
    // since get returned null, try a putIfAbsent; that fails due to a simulated race
    expect(backingMap.putIfAbsent(eq(KEY), isA(AtomicInteger.class))).andReturn(existingZero);
    // since the putIfAbsent returned a zero, we'll try to replace...
    expect(backingMap.replace(eq(KEY), eq(existingZero), isA(AtomicInteger.class)))
        .andReturn(false);
    // ...and then putIfAbsent. Simulate failure on both
    expect(backingMap.putIfAbsent(eq(KEY), isA(AtomicInteger.class))).andReturn(existing);

    // next map.get()
    expect(backingMap.get(KEY)).andReturn(existingZero);
    // since get returned zero, try a replace; that fails due to a simulated race
    expect(backingMap.replace(eq(KEY), eq(existingZero), isA(AtomicInteger.class)))
        .andReturn(false);
    expect(backingMap.putIfAbsent(eq(KEY), isA(AtomicInteger.class))).andReturn(existing);

    // another map.get()
    expect(backingMap.get(KEY)).andReturn(existing);
    // we shouldn't see any more map operations; CHM will now just update the AtomicInteger

    replay();

    assertEquals(multiset.add(KEY, 3), 12);
    assertEquals(15, existing.get());

    verify();
  }

  public void testRemove_zeroFromSome() {
    final int INITIAL_COUNT = 14;
    expect(backingMap.get(KEY)).andReturn(new AtomicInteger(INITIAL_COUNT));
    replay();

    assertEquals(INITIAL_COUNT, multiset.remove(KEY, 0));
    verify();
  }

  public void testRemove_zeroFromNone() {
    expect(backingMap.get(KEY)).andReturn(null);
    replay();

    assertEquals(0, multiset.remove(KEY, 0));
    verify();
  }

  public void testRemove_nonePresent() {
    expect(backingMap.get(KEY)).andReturn(null);
    replay();

    assertEquals(0, multiset.remove(KEY, 400));
    verify();
  }

  public void testRemove_someRemaining() {
    int countToRemove = 30;
    int countRemaining = 1;
    AtomicInteger current = new AtomicInteger(countToRemove + countRemaining);

    expect(backingMap.get(KEY)).andReturn(current);
    replay();

    assertEquals(countToRemove + countRemaining, multiset.remove(KEY, countToRemove));
    assertEquals(countRemaining, current.get());
    verify();
  }

  public void testRemove_noneRemaining() {
    int countToRemove = 30;
    AtomicInteger current = new AtomicInteger(countToRemove);

    expect(backingMap.get(KEY)).andReturn(current);
    // it's ok if removal fails: another thread may have done the remove
    expect(backingMap.remove(KEY, current)).andReturn(false);
    replay();

    assertEquals(countToRemove, multiset.remove(KEY, countToRemove));
    assertEquals(0, current.get());
    verify();
  }

  public void testRemoveExactly() {
    ConcurrentHashMultiset<String> cms = ConcurrentHashMultiset.create();
    cms.add("a", 2);
    cms.add("b", 3);

    try {
      cms.removeExactly("a", -2);
    } catch (IllegalArgumentException expected) {}

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

    expect(backingMap.get(KEY)).andReturn(current);
    replay();

    assertEquals(initialCount, multiset.setCount(KEY, countToSet));
    assertEquals(countToSet, current.get());
    verify();
  }

  public void testSetCount_asRemove() {
    int countToRemove = 40;
    AtomicInteger current = new AtomicInteger(countToRemove);

    expect(backingMap.get(KEY)).andReturn(current);
    expect(backingMap.remove(KEY, current)).andReturn(true);
    replay();

    assertEquals(countToRemove, multiset.setCount(KEY, 0));
    assertEquals(0, current.get());
    verify();
  }

  public void testSetCount_0_nonePresent() {
    expect(backingMap.get(KEY)).andReturn(null);
    replay();

    assertEquals(0, multiset.setCount(KEY, 0));
    verify();
  }

  public void testCreate() {
    ConcurrentHashMultiset<Integer> multiset = ConcurrentHashMultiset.create();
    assertTrue(multiset.isEmpty());
    reserializeAndAssert(multiset);
  }

  public void testCreateFromIterable() {
    Iterable<Integer> iterable = asList(1, 2, 2, 3, 4);
    ConcurrentHashMultiset<Integer> multiset
        = ConcurrentHashMultiset.create(iterable);
    assertEquals(2, multiset.count(2));
    reserializeAndAssert(multiset);
  }

  public void testIdentityKeyEquality_strongKeys() {
    testIdentityKeyEquality(STRONG);
  }

  public void testIdentityKeyEquality_weakKeys() {
    testIdentityKeyEquality(WEAK);
  }

  private void testIdentityKeyEquality(
      MapMakerInternalMap.Strength keyStrength) {

    MapMaker mapMaker = new MapMaker()
        .setKeyStrength(keyStrength)
        .keyEquivalence(Equivalence.identity());

    ConcurrentHashMultiset<String> multiset =
        ConcurrentHashMultiset.create(mapMaker);

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

  private void testLogicalKeyEquality(
      MapMakerInternalMap.Strength keyStrength) {

    MapMaker mapMaker = new MapMaker()
        .setKeyStrength(keyStrength)
        .keyEquivalence(Equivalence.equals());

    ConcurrentHashMultiset<String> multiset =
        ConcurrentHashMultiset.create(mapMaker);

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
    MapMaker mapMaker = new MapMaker();
    multiset = ConcurrentHashMultiset.create(mapMaker);
    reserializeAndAssert(multiset);
  }

  public void testSerializationWithMapMaker2() {
    MapMaker mapMaker = new MapMaker();
    multiset = ConcurrentHashMultiset.create(mapMaker);
    multiset.addAll(ImmutableList.of("a", "a", "b", "c", "d", "b"));
    reserializeAndAssert(multiset);
  }

  public void testSerializationWithMapMaker3() {
    MapMaker mapMaker = new MapMaker().expireAfterWrite(1, TimeUnit.SECONDS);
    multiset = ConcurrentHashMultiset.create(mapMaker);
    multiset.addAll(ImmutableList.of("a", "a", "b", "c", "d", "b"));
    reserializeAndAssert(multiset);
  }

  public void testSerializationWithMapMaker_preservesIdentityKeyEquivalence() {
    MapMaker mapMaker = new MapMaker()
        .keyEquivalence(Equivalence.identity());

    ConcurrentHashMultiset<String> multiset =
        ConcurrentHashMultiset.create(mapMaker);
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

//  @Suppress(owner = "bmanes", detail = "Does not call the eviction listener")
//  public void testWithMapMakerEvictionListener_BROKEN1()
//      throws InterruptedException {
//    MapEvictionListener<String, Number> evictionListener =
//        mockEvictionListener();
//    evictionListener.onEviction("a", 5);
//    EasyMock.replay(evictionListener);
//
//    GenericMapMaker<String, Number> mapMaker = new MapMaker()
//        .expireAfterWrite(100, TimeUnit.MILLISECONDS)
//        .evictionListener(evictionListener);
//
//    ConcurrentHashMultiset<String> multiset =
//        ConcurrentHashMultiset.create(mapMaker);
//
//    multiset.add("a", 5);
//
//    assertTrue(multiset.contains("a"));
//    assertEquals(5, multiset.count("a"));
//
//    Thread.sleep(2000);
//
//    EasyMock.verify(evictionListener);
//  }

//  @Suppress(owner = "bmanes", detail = "Does not call the eviction listener")
//  public void testWithMapMakerEvictionListener_BROKEN2()
//      throws InterruptedException {
//    MapEvictionListener<String, Number> evictionListener =
//        mockEvictionListener();
//    evictionListener.onEviction("a", 5);
//    EasyMock.replay(evictionListener);
//
//    GenericMapMaker<String, Number> mapMaker = new MapMaker()
//        .expireAfterWrite(100, TimeUnit.MILLISECONDS)
//        .evictionListener(evictionListener);
//
//    ConcurrentHashMultiset<String> multiset =
//        ConcurrentHashMultiset.create(mapMaker);
//
//    multiset.add("a", 5);
//
//    assertTrue(multiset.contains("a"));
//    assertEquals(5, multiset.count("a"));
//
//    Thread.sleep(2000);
//
//    // This call should have the side-effect of calling the
//    // eviction listener, but it does not.
//    assertFalse(multiset.contains("a"));
//
//    EasyMock.verify(evictionListener);
//  }

  public void testWithMapMakerEvictionListener() {
    final List<RemovalNotification<String, Number>> notificationQueue = Lists.newArrayList();
    RemovalListener<String, Number> removalListener =
        new RemovalListener<String, Number>() {
          @Override public void onRemoval(RemovalNotification<String, Number> notification) {
            notificationQueue.add(notification);
          }
        };

    @SuppressWarnings("deprecation") // TODO(kevinb): what to do?
    MapMaker mapMaker = new MapMaker()
        .concurrencyLevel(1)
        .maximumSize(1);
    /*
     * Cleverly ignore the return type now that ConcurrentHashMultiset accepts only MapMaker and not
     * the deprecated GenericMapMaker. We know that a RemovalListener<String, Number> is a type that
     * will work with ConcurrentHashMultiset.
     */
    mapMaker.removalListener(removalListener);

    ConcurrentHashMultiset<String> multiset = ConcurrentHashMultiset.create(mapMaker);

    multiset.add("a", 5);
    assertTrue(multiset.contains("a"));
    assertEquals(5, multiset.count("a"));

    multiset.add("b", 3);

    assertFalse(multiset.contains("a"));
    assertTrue(multiset.contains("b"));
    assertEquals(3, multiset.count("b"));
    RemovalNotification<String, Number> notification = Iterables.getOnlyElement(notificationQueue);
    assertEquals("a", notification.getKey());
    // The map evicted this entry, so CHM didn't have a chance to zero it.
    assertEquals(5, notification.getValue().intValue());
  }

  private void replay() {
    EasyMock.replay(backingMap);
  }

  private void verify() {
    EasyMock.verify(backingMap);
  }

  private void reset() {
    EasyMock.reset(backingMap);
  }
}

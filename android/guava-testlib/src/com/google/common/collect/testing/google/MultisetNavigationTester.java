/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect.testing.google;

import static com.google.common.collect.BoundType.CLOSED;
import static com.google.common.collect.BoundType.OPEN;
import static com.google.common.collect.testing.Helpers.copyToList;
import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_ADD;
import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_REMOVE;
import static com.google.common.collect.testing.features.CollectionSize.ONE;
import static com.google.common.collect.testing.features.CollectionSize.SEVERAL;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.BoundType;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Multisets;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.Ignore;

/**
 * Tester for navigation of SortedMultisets.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MultisetNavigationTester<E> extends AbstractMultisetTester<E> {
  private SortedMultiset<E> sortedMultiset;
  private List<E> entries;
  private Entry<E> a;
  private Entry<E> b;
  private Entry<E> c;

  /** Used to avoid http://bugs.sun.com/view_bug.do?bug_id=6558557 */
  static <T> SortedMultiset<T> cast(Multiset<T> iterable) {
    return (SortedMultiset<T>) iterable;
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    sortedMultiset = cast(getMultiset());
    entries =
        copyToList(
            getSubjectGenerator()
                .getSampleElements(getSubjectGenerator().getCollectionSize().getNumElements()));
    Collections.sort(entries, sortedMultiset.comparator());

    // some tests assume SEVERAL == 3
    if (entries.size() >= 1) {
      a = Multisets.immutableEntry(entries.get(0), sortedMultiset.count(entries.get(0)));
      if (entries.size() >= 3) {
        b = Multisets.immutableEntry(entries.get(1), sortedMultiset.count(entries.get(1)));
        c = Multisets.immutableEntry(entries.get(2), sortedMultiset.count(entries.get(2)));
      }
    }
  }

  /** Resets the contents of sortedMultiset to have entries a, c, for the navigation tests. */
  @SuppressWarnings("unchecked")
  // Needed to stop Eclipse whining
  private void resetWithHole() {
    List<E> container = new ArrayList<E>();
    container.addAll(Collections.nCopies(a.getCount(), a.getElement()));
    container.addAll(Collections.nCopies(c.getCount(), c.getElement()));
    super.resetContainer(getSubjectGenerator().create(container.toArray()));
    sortedMultiset = (SortedMultiset<E>) getMultiset();
  }

  @CollectionSize.Require(ZERO)
  public void testEmptyMultisetFirst() {
    assertNull(sortedMultiset.firstEntry());
    try {
      sortedMultiset.elementSet().first();
      fail();
    } catch (NoSuchElementException e) {
    }
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(ZERO)
  public void testEmptyMultisetPollFirst() {
    assertNull(sortedMultiset.pollFirstEntry());
  }

  @CollectionSize.Require(ZERO)
  public void testEmptyMultisetNearby() {
    for (BoundType type : BoundType.values()) {
      assertNull(sortedMultiset.headMultiset(e0(), type).lastEntry());
      assertNull(sortedMultiset.tailMultiset(e0(), type).firstEntry());
    }
  }

  @CollectionSize.Require(ZERO)
  public void testEmptyMultisetLast() {
    assertNull(sortedMultiset.lastEntry());
    try {
      assertNull(sortedMultiset.elementSet().last());
      fail();
    } catch (NoSuchElementException e) {
    }
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(ZERO)
  public void testEmptyMultisetPollLast() {
    assertNull(sortedMultiset.pollLastEntry());
  }

  @CollectionSize.Require(ONE)
  public void testSingletonMultisetFirst() {
    assertEquals(a, sortedMultiset.firstEntry());
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(ONE)
  public void testSingletonMultisetPollFirst() {
    assertEquals(a, sortedMultiset.pollFirstEntry());
    assertTrue(sortedMultiset.isEmpty());
  }

  @CollectionSize.Require(ONE)
  public void testSingletonMultisetNearby() {
    assertNull(sortedMultiset.headMultiset(e0(), OPEN).lastEntry());
    assertNull(sortedMultiset.tailMultiset(e0(), OPEN).lastEntry());

    assertEquals(a, sortedMultiset.headMultiset(e0(), CLOSED).lastEntry());
    assertEquals(a, sortedMultiset.tailMultiset(e0(), CLOSED).firstEntry());
  }

  @CollectionSize.Require(ONE)
  public void testSingletonMultisetLast() {
    assertEquals(a, sortedMultiset.lastEntry());
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(ONE)
  public void testSingletonMultisetPollLast() {
    assertEquals(a, sortedMultiset.pollLastEntry());
    assertTrue(sortedMultiset.isEmpty());
  }

  @CollectionSize.Require(SEVERAL)
  public void testFirst() {
    assertEquals(a, sortedMultiset.firstEntry());
  }

  @SuppressWarnings("unchecked")
  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(SEVERAL)
  public void testPollFirst() {
    assertEquals(a, sortedMultiset.pollFirstEntry());
    assertEquals(Arrays.asList(b, c), copyToList(sortedMultiset.entrySet()));
  }

  @CollectionFeature.Require(absent = SUPPORTS_REMOVE)
  public void testPollFirstUnsupported() {
    try {
      sortedMultiset.pollFirstEntry();
      fail();
    } catch (UnsupportedOperationException e) {
    }
  }

  @CollectionSize.Require(SEVERAL)
  public void testLower() {
    resetWithHole();
    assertEquals(null, sortedMultiset.headMultiset(a.getElement(), OPEN).lastEntry());
    assertEquals(a, sortedMultiset.headMultiset(b.getElement(), OPEN).lastEntry());
    assertEquals(a, sortedMultiset.headMultiset(c.getElement(), OPEN).lastEntry());
  }

  @CollectionSize.Require(SEVERAL)
  public void testFloor() {
    resetWithHole();
    assertEquals(a, sortedMultiset.headMultiset(a.getElement(), CLOSED).lastEntry());
    assertEquals(a, sortedMultiset.headMultiset(b.getElement(), CLOSED).lastEntry());
    assertEquals(c, sortedMultiset.headMultiset(c.getElement(), CLOSED).lastEntry());
  }

  @CollectionSize.Require(SEVERAL)
  public void testCeiling() {
    resetWithHole();

    assertEquals(a, sortedMultiset.tailMultiset(a.getElement(), CLOSED).firstEntry());
    assertEquals(c, sortedMultiset.tailMultiset(b.getElement(), CLOSED).firstEntry());
    assertEquals(c, sortedMultiset.tailMultiset(c.getElement(), CLOSED).firstEntry());
  }

  @CollectionSize.Require(SEVERAL)
  public void testHigher() {
    resetWithHole();
    assertEquals(c, sortedMultiset.tailMultiset(a.getElement(), OPEN).firstEntry());
    assertEquals(c, sortedMultiset.tailMultiset(b.getElement(), OPEN).firstEntry());
    assertEquals(null, sortedMultiset.tailMultiset(c.getElement(), OPEN).firstEntry());
  }

  @CollectionSize.Require(SEVERAL)
  public void testLast() {
    assertEquals(c, sortedMultiset.lastEntry());
  }

  @SuppressWarnings("unchecked")
  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(SEVERAL)
  public void testPollLast() {
    assertEquals(c, sortedMultiset.pollLastEntry());
    assertEquals(Arrays.asList(a, b), copyToList(sortedMultiset.entrySet()));
  }

  @CollectionFeature.Require(absent = SUPPORTS_REMOVE)
  @CollectionSize.Require(SEVERAL)
  public void testPollLastUnsupported() {
    try {
      sortedMultiset.pollLastEntry();
      fail();
    } catch (UnsupportedOperationException e) {
    }
  }

  @CollectionSize.Require(SEVERAL)
  public void testDescendingNavigation() {
    List<Entry<E>> ascending = new ArrayList<>();
    Iterators.addAll(ascending, sortedMultiset.entrySet().iterator());
    List<Entry<E>> descending = new ArrayList<>();
    Iterators.addAll(descending, sortedMultiset.descendingMultiset().entrySet().iterator());
    Collections.reverse(descending);
    assertEquals(ascending, descending);
  }

  void expectAddFailure(SortedMultiset<E> multiset, Entry<E> entry) {
    try {
      multiset.add(entry.getElement(), entry.getCount());
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }

    try {
      multiset.add(entry.getElement());
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }

    try {
      multiset.addAll(Collections.singletonList(entry.getElement()));
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  void expectRemoveZero(SortedMultiset<E> multiset, Entry<E> entry) {
    assertEquals(0, multiset.remove(entry.getElement(), entry.getCount()));
    assertFalse(multiset.remove(entry.getElement()));
    assertFalse(multiset.elementSet().remove(entry.getElement()));
  }

  void expectSetCountFailure(SortedMultiset<E> multiset, Entry<E> entry) {
    try {
      multiset.setCount(entry.getElement(), multiset.count(entry.getElement()));
    } catch (IllegalArgumentException acceptable) {
    }
    try {
      multiset.setCount(entry.getElement(), multiset.count(entry.getElement()) + 1);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  @CollectionSize.Require(ONE)
  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testAddOutOfTailBoundsOne() {
    expectAddFailure(sortedMultiset.tailMultiset(a.getElement(), OPEN), a);
  }

  @CollectionSize.Require(SEVERAL)
  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testAddOutOfTailBoundsSeveral() {
    expectAddFailure(sortedMultiset.tailMultiset(a.getElement(), OPEN), a);
    expectAddFailure(sortedMultiset.tailMultiset(b.getElement(), CLOSED), a);
    expectAddFailure(sortedMultiset.tailMultiset(b.getElement(), OPEN), a);
    expectAddFailure(sortedMultiset.tailMultiset(b.getElement(), OPEN), b);
    expectAddFailure(sortedMultiset.tailMultiset(c.getElement(), CLOSED), a);
    expectAddFailure(sortedMultiset.tailMultiset(c.getElement(), CLOSED), b);
    expectAddFailure(sortedMultiset.tailMultiset(c.getElement(), OPEN), a);
    expectAddFailure(sortedMultiset.tailMultiset(c.getElement(), OPEN), b);
    expectAddFailure(sortedMultiset.tailMultiset(c.getElement(), OPEN), c);
  }

  @CollectionSize.Require(ONE)
  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testAddOutOfHeadBoundsOne() {
    expectAddFailure(sortedMultiset.headMultiset(a.getElement(), OPEN), a);
  }

  @CollectionSize.Require(SEVERAL)
  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testAddOutOfHeadBoundsSeveral() {
    expectAddFailure(sortedMultiset.headMultiset(c.getElement(), OPEN), c);
    expectAddFailure(sortedMultiset.headMultiset(b.getElement(), CLOSED), c);
    expectAddFailure(sortedMultiset.headMultiset(b.getElement(), OPEN), c);
    expectAddFailure(sortedMultiset.headMultiset(b.getElement(), OPEN), b);
    expectAddFailure(sortedMultiset.headMultiset(a.getElement(), CLOSED), c);
    expectAddFailure(sortedMultiset.headMultiset(a.getElement(), CLOSED), b);
    expectAddFailure(sortedMultiset.headMultiset(a.getElement(), OPEN), c);
    expectAddFailure(sortedMultiset.headMultiset(a.getElement(), OPEN), b);
    expectAddFailure(sortedMultiset.headMultiset(a.getElement(), OPEN), a);
  }

  @CollectionSize.Require(ONE)
  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testRemoveOutOfTailBoundsOne() {
    expectRemoveZero(sortedMultiset.tailMultiset(a.getElement(), OPEN), a);
  }

  @CollectionSize.Require(SEVERAL)
  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testRemoveOutOfTailBoundsSeveral() {
    expectRemoveZero(sortedMultiset.tailMultiset(a.getElement(), OPEN), a);
    expectRemoveZero(sortedMultiset.tailMultiset(b.getElement(), CLOSED), a);
    expectRemoveZero(sortedMultiset.tailMultiset(b.getElement(), OPEN), a);
    expectRemoveZero(sortedMultiset.tailMultiset(b.getElement(), OPEN), b);
    expectRemoveZero(sortedMultiset.tailMultiset(c.getElement(), CLOSED), a);
    expectRemoveZero(sortedMultiset.tailMultiset(c.getElement(), CLOSED), b);
    expectRemoveZero(sortedMultiset.tailMultiset(c.getElement(), OPEN), a);
    expectRemoveZero(sortedMultiset.tailMultiset(c.getElement(), OPEN), b);
    expectRemoveZero(sortedMultiset.tailMultiset(c.getElement(), OPEN), c);
  }

  @CollectionSize.Require(ONE)
  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testRemoveOutOfHeadBoundsOne() {
    expectRemoveZero(sortedMultiset.headMultiset(a.getElement(), OPEN), a);
  }

  @CollectionSize.Require(SEVERAL)
  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testRemoveOutOfHeadBoundsSeveral() {
    expectRemoveZero(sortedMultiset.headMultiset(c.getElement(), OPEN), c);
    expectRemoveZero(sortedMultiset.headMultiset(b.getElement(), CLOSED), c);
    expectRemoveZero(sortedMultiset.headMultiset(b.getElement(), OPEN), c);
    expectRemoveZero(sortedMultiset.headMultiset(b.getElement(), OPEN), b);
    expectRemoveZero(sortedMultiset.headMultiset(a.getElement(), CLOSED), c);
    expectRemoveZero(sortedMultiset.headMultiset(a.getElement(), CLOSED), b);
    expectRemoveZero(sortedMultiset.headMultiset(a.getElement(), OPEN), c);
    expectRemoveZero(sortedMultiset.headMultiset(a.getElement(), OPEN), b);
    expectRemoveZero(sortedMultiset.headMultiset(a.getElement(), OPEN), a);
  }

  @CollectionSize.Require(ONE)
  @CollectionFeature.Require({SUPPORTS_ADD, SUPPORTS_REMOVE})
  public void testSetCountOutOfTailBoundsOne() {
    expectSetCountFailure(sortedMultiset.tailMultiset(a.getElement(), OPEN), a);
  }

  @CollectionSize.Require(SEVERAL)
  @CollectionFeature.Require({SUPPORTS_ADD, SUPPORTS_REMOVE})
  public void testSetCountOutOfTailBoundsSeveral() {
    expectSetCountFailure(sortedMultiset.tailMultiset(a.getElement(), OPEN), a);
    expectSetCountFailure(sortedMultiset.tailMultiset(b.getElement(), CLOSED), a);
    expectSetCountFailure(sortedMultiset.tailMultiset(b.getElement(), OPEN), a);
    expectSetCountFailure(sortedMultiset.tailMultiset(b.getElement(), OPEN), b);
    expectSetCountFailure(sortedMultiset.tailMultiset(c.getElement(), CLOSED), a);
    expectSetCountFailure(sortedMultiset.tailMultiset(c.getElement(), CLOSED), b);
    expectSetCountFailure(sortedMultiset.tailMultiset(c.getElement(), OPEN), a);
    expectSetCountFailure(sortedMultiset.tailMultiset(c.getElement(), OPEN), b);
    expectSetCountFailure(sortedMultiset.tailMultiset(c.getElement(), OPEN), c);
  }

  @CollectionSize.Require(ONE)
  @CollectionFeature.Require({SUPPORTS_ADD, SUPPORTS_REMOVE})
  public void testSetCountOutOfHeadBoundsOne() {
    expectSetCountFailure(sortedMultiset.headMultiset(a.getElement(), OPEN), a);
  }

  @CollectionSize.Require(SEVERAL)
  @CollectionFeature.Require({SUPPORTS_ADD, SUPPORTS_REMOVE})
  public void testSetCountOutOfHeadBoundsSeveral() {
    expectSetCountFailure(sortedMultiset.headMultiset(c.getElement(), OPEN), c);
    expectSetCountFailure(sortedMultiset.headMultiset(b.getElement(), CLOSED), c);
    expectSetCountFailure(sortedMultiset.headMultiset(b.getElement(), OPEN), c);
    expectSetCountFailure(sortedMultiset.headMultiset(b.getElement(), OPEN), b);
    expectSetCountFailure(sortedMultiset.headMultiset(a.getElement(), CLOSED), c);
    expectSetCountFailure(sortedMultiset.headMultiset(a.getElement(), CLOSED), b);
    expectSetCountFailure(sortedMultiset.headMultiset(a.getElement(), OPEN), c);
    expectSetCountFailure(sortedMultiset.headMultiset(a.getElement(), OPEN), b);
    expectSetCountFailure(sortedMultiset.headMultiset(a.getElement(), OPEN), a);
  }

  @CollectionSize.Require(SEVERAL)
  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testAddWithConflictingBounds() {
    testEmptyRangeSubMultisetSupportingAdd(
        sortedMultiset.subMultiset(a.getElement(), CLOSED, a.getElement(), OPEN));
    testEmptyRangeSubMultisetSupportingAdd(
        sortedMultiset.subMultiset(a.getElement(), OPEN, a.getElement(), OPEN));
    testEmptyRangeSubMultisetSupportingAdd(
        sortedMultiset.subMultiset(a.getElement(), OPEN, a.getElement(), CLOSED));
    testEmptyRangeSubMultisetSupportingAdd(
        sortedMultiset.subMultiset(b.getElement(), CLOSED, a.getElement(), CLOSED));
    testEmptyRangeSubMultisetSupportingAdd(
        sortedMultiset.subMultiset(b.getElement(), CLOSED, a.getElement(), OPEN));
    testEmptyRangeSubMultisetSupportingAdd(
        sortedMultiset.subMultiset(b.getElement(), OPEN, a.getElement(), OPEN));
  }

  @CollectionSize.Require(SEVERAL)
  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testConflictingBounds() {
    testEmptyRangeSubMultiset(
        sortedMultiset.subMultiset(a.getElement(), CLOSED, a.getElement(), OPEN));
    testEmptyRangeSubMultiset(
        sortedMultiset.subMultiset(a.getElement(), OPEN, a.getElement(), OPEN));
    testEmptyRangeSubMultiset(
        sortedMultiset.subMultiset(a.getElement(), OPEN, a.getElement(), CLOSED));
    testEmptyRangeSubMultiset(
        sortedMultiset.subMultiset(b.getElement(), CLOSED, a.getElement(), CLOSED));
    testEmptyRangeSubMultiset(
        sortedMultiset.subMultiset(b.getElement(), CLOSED, a.getElement(), OPEN));
    testEmptyRangeSubMultiset(
        sortedMultiset.subMultiset(b.getElement(), OPEN, a.getElement(), OPEN));
  }

  public void testEmptyRangeSubMultiset(SortedMultiset<E> multiset) {
    assertTrue(multiset.isEmpty());
    assertEquals(0, multiset.size());
    assertEquals(0, multiset.toArray().length);
    assertTrue(multiset.entrySet().isEmpty());
    assertFalse(multiset.iterator().hasNext());
    assertEquals(0, multiset.entrySet().size());
    assertEquals(0, multiset.entrySet().toArray().length);
    assertFalse(multiset.entrySet().iterator().hasNext());
  }

  @SuppressWarnings("unchecked")
  public void testEmptyRangeSubMultisetSupportingAdd(SortedMultiset<E> multiset) {
    for (Entry<E> entry : Arrays.asList(a, b, c)) {
      expectAddFailure(multiset, entry);
    }
  }

  private static int totalSize(Iterable<? extends Entry<?>> entries) {
    int sum = 0;
    for (Entry<?> entry : entries) {
      sum += entry.getCount();
    }
    return sum;
  }

  private enum SubMultisetSpec {
    TAIL_CLOSED {
      @Override
      <E> List<Entry<E>> expectedEntries(int targetEntry, List<Entry<E>> entries) {
        return entries.subList(targetEntry, entries.size());
      }

      @Override
      <E> SortedMultiset<E> subMultiset(
          SortedMultiset<E> multiset, List<Entry<E>> entries, int targetEntry) {
        return multiset.tailMultiset(entries.get(targetEntry).getElement(), CLOSED);
      }
    },
    TAIL_OPEN {
      @Override
      <E> List<Entry<E>> expectedEntries(int targetEntry, List<Entry<E>> entries) {
        return entries.subList(targetEntry + 1, entries.size());
      }

      @Override
      <E> SortedMultiset<E> subMultiset(
          SortedMultiset<E> multiset, List<Entry<E>> entries, int targetEntry) {
        return multiset.tailMultiset(entries.get(targetEntry).getElement(), OPEN);
      }
    },
    HEAD_CLOSED {
      @Override
      <E> List<Entry<E>> expectedEntries(int targetEntry, List<Entry<E>> entries) {
        return entries.subList(0, targetEntry + 1);
      }

      @Override
      <E> SortedMultiset<E> subMultiset(
          SortedMultiset<E> multiset, List<Entry<E>> entries, int targetEntry) {
        return multiset.headMultiset(entries.get(targetEntry).getElement(), CLOSED);
      }
    },
    HEAD_OPEN {
      @Override
      <E> List<Entry<E>> expectedEntries(int targetEntry, List<Entry<E>> entries) {
        return entries.subList(0, targetEntry);
      }

      @Override
      <E> SortedMultiset<E> subMultiset(
          SortedMultiset<E> multiset, List<Entry<E>> entries, int targetEntry) {
        return multiset.headMultiset(entries.get(targetEntry).getElement(), OPEN);
      }
    };

    abstract <E> List<Entry<E>> expectedEntries(int targetEntry, List<Entry<E>> entries);

    abstract <E> SortedMultiset<E> subMultiset(
        SortedMultiset<E> multiset, List<Entry<E>> entries, int targetEntry);
  }

  private void testSubMultisetEntrySet(SubMultisetSpec spec) {
    List<Entry<E>> entries = copyToList(sortedMultiset.entrySet());
    for (int i = 0; i < entries.size(); i++) {
      List<Entry<E>> expected = spec.expectedEntries(i, entries);
      SortedMultiset<E> subMultiset = spec.subMultiset(sortedMultiset, entries, i);
      assertEquals(expected, copyToList(subMultiset.entrySet()));
    }
  }

  private void testSubMultisetSize(SubMultisetSpec spec) {
    List<Entry<E>> entries = copyToList(sortedMultiset.entrySet());
    for (int i = 0; i < entries.size(); i++) {
      List<Entry<E>> expected = spec.expectedEntries(i, entries);
      SortedMultiset<E> subMultiset = spec.subMultiset(sortedMultiset, entries, i);
      assertEquals(totalSize(expected), subMultiset.size());
    }
  }

  private void testSubMultisetDistinctElements(SubMultisetSpec spec) {
    List<Entry<E>> entries = copyToList(sortedMultiset.entrySet());
    for (int i = 0; i < entries.size(); i++) {
      List<Entry<E>> expected = spec.expectedEntries(i, entries);
      SortedMultiset<E> subMultiset = spec.subMultiset(sortedMultiset, entries, i);
      assertEquals(expected.size(), subMultiset.entrySet().size());
      assertEquals(expected.size(), subMultiset.elementSet().size());
    }
  }

  public void testTailClosedEntrySet() {
    testSubMultisetEntrySet(SubMultisetSpec.TAIL_CLOSED);
  }

  public void testTailClosedSize() {
    testSubMultisetSize(SubMultisetSpec.TAIL_CLOSED);
  }

  public void testTailClosedDistinctElements() {
    testSubMultisetDistinctElements(SubMultisetSpec.TAIL_CLOSED);
  }

  public void testTailOpenEntrySet() {
    testSubMultisetEntrySet(SubMultisetSpec.TAIL_OPEN);
  }

  public void testTailOpenSize() {
    testSubMultisetSize(SubMultisetSpec.TAIL_OPEN);
  }

  public void testTailOpenDistinctElements() {
    testSubMultisetDistinctElements(SubMultisetSpec.TAIL_OPEN);
  }

  public void testHeadClosedEntrySet() {
    testSubMultisetEntrySet(SubMultisetSpec.HEAD_CLOSED);
  }

  public void testHeadClosedSize() {
    testSubMultisetSize(SubMultisetSpec.HEAD_CLOSED);
  }

  public void testHeadClosedDistinctElements() {
    testSubMultisetDistinctElements(SubMultisetSpec.HEAD_CLOSED);
  }

  public void testHeadOpenEntrySet() {
    testSubMultisetEntrySet(SubMultisetSpec.HEAD_OPEN);
  }

  public void testHeadOpenSize() {
    testSubMultisetSize(SubMultisetSpec.HEAD_OPEN);
  }

  public void testHeadOpenDistinctElements() {
    testSubMultisetDistinctElements(SubMultisetSpec.HEAD_OPEN);
  }

  @CollectionSize.Require(SEVERAL)
  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testClearTailOpen() {
    List<Entry<E>> expected =
        copyToList(sortedMultiset.headMultiset(b.getElement(), CLOSED).entrySet());
    sortedMultiset.tailMultiset(b.getElement(), OPEN).clear();
    assertEquals(expected, copyToList(sortedMultiset.entrySet()));
  }

  @CollectionSize.Require(SEVERAL)
  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testClearTailOpenEntrySet() {
    List<Entry<E>> expected =
        copyToList(sortedMultiset.headMultiset(b.getElement(), CLOSED).entrySet());
    sortedMultiset.tailMultiset(b.getElement(), OPEN).entrySet().clear();
    assertEquals(expected, copyToList(sortedMultiset.entrySet()));
  }

  @CollectionSize.Require(SEVERAL)
  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testClearTailClosed() {
    List<Entry<E>> expected =
        copyToList(sortedMultiset.headMultiset(b.getElement(), OPEN).entrySet());
    sortedMultiset.tailMultiset(b.getElement(), CLOSED).clear();
    assertEquals(expected, copyToList(sortedMultiset.entrySet()));
  }

  @CollectionSize.Require(SEVERAL)
  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testClearTailClosedEntrySet() {
    List<Entry<E>> expected =
        copyToList(sortedMultiset.headMultiset(b.getElement(), OPEN).entrySet());
    sortedMultiset.tailMultiset(b.getElement(), CLOSED).entrySet().clear();
    assertEquals(expected, copyToList(sortedMultiset.entrySet()));
  }

  @CollectionSize.Require(SEVERAL)
  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testClearHeadOpen() {
    List<Entry<E>> expected =
        copyToList(sortedMultiset.tailMultiset(b.getElement(), CLOSED).entrySet());
    sortedMultiset.headMultiset(b.getElement(), OPEN).clear();
    assertEquals(expected, copyToList(sortedMultiset.entrySet()));
  }

  @CollectionSize.Require(SEVERAL)
  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testClearHeadOpenEntrySet() {
    List<Entry<E>> expected =
        copyToList(sortedMultiset.tailMultiset(b.getElement(), CLOSED).entrySet());
    sortedMultiset.headMultiset(b.getElement(), OPEN).entrySet().clear();
    assertEquals(expected, copyToList(sortedMultiset.entrySet()));
  }

  @CollectionSize.Require(SEVERAL)
  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testClearHeadClosed() {
    List<Entry<E>> expected =
        copyToList(sortedMultiset.tailMultiset(b.getElement(), OPEN).entrySet());
    sortedMultiset.headMultiset(b.getElement(), CLOSED).clear();
    assertEquals(expected, copyToList(sortedMultiset.entrySet()));
  }

  @CollectionSize.Require(SEVERAL)
  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testClearHeadClosedEntrySet() {
    List<Entry<E>> expected =
        copyToList(sortedMultiset.tailMultiset(b.getElement(), OPEN).entrySet());
    sortedMultiset.headMultiset(b.getElement(), CLOSED).entrySet().clear();
    assertEquals(expected, copyToList(sortedMultiset.entrySet()));
  }
}

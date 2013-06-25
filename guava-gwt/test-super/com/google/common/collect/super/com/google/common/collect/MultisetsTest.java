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

import static org.truth0.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.DerivedComparable;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collections;

/**
 * Tests for {@link Multisets}.
 *
 * @author Mike Bostock
 * @author Jared Levy
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class MultisetsTest extends TestCase {

  /* See MultisetsImmutableEntryTest for immutableEntry() tests. */

  public void testNewTreeMultisetDerived() {
    TreeMultiset<DerivedComparable> set = TreeMultiset.create();
    assertTrue(set.isEmpty());
    set.add(new DerivedComparable("foo"), 2);
    set.add(new DerivedComparable("bar"), 3);
    ASSERT.that(set).has().exactly(
        new DerivedComparable("bar"), new DerivedComparable("bar"), new DerivedComparable("bar"),
        new DerivedComparable("foo"), new DerivedComparable("foo")).inOrder();
  }

  public void testNewTreeMultisetNonGeneric() {
    TreeMultiset<LegacyComparable> set = TreeMultiset.create();
    assertTrue(set.isEmpty());
    set.add(new LegacyComparable("foo"), 2);
    set.add(new LegacyComparable("bar"), 3);
    ASSERT.that(set).has().exactly(new LegacyComparable("bar"),
        new LegacyComparable("bar"), new LegacyComparable("bar"),
        new LegacyComparable("foo"), new LegacyComparable("foo")).inOrder();
  }

  public void testNewTreeMultisetComparator() {
    TreeMultiset<String> multiset
        = TreeMultiset.create(Collections.reverseOrder());
    multiset.add("bar", 3);
    multiset.add("foo", 2);
    ASSERT.that(multiset).has().exactly("foo", "foo", "bar", "bar", "bar").inOrder();
  }

  public void testRetainOccurrencesEmpty() {
    Multiset<String> multiset = HashMultiset.create();
    Multiset<String> toRetain =
        HashMultiset.create(Arrays.asList("a", "b", "a"));
    assertFalse(Multisets.retainOccurrences(multiset, toRetain));
    ASSERT.that(multiset).isEmpty();
  }

  public void testRemoveOccurrencesEmpty() {
    Multiset<String> multiset = HashMultiset.create();
    Multiset<String> toRemove =
        HashMultiset.create(Arrays.asList("a", "b", "a"));
    assertFalse(Multisets.retainOccurrences(multiset, toRemove));
    assertTrue(multiset.isEmpty());
  }

  public void testUnion() {
    Multiset<String> ms1 = HashMultiset.create(Arrays.asList("a", "b", "a"));
    Multiset<String> ms2 = HashMultiset.create(
        Arrays.asList("a", "b", "b", "c"));
    ASSERT.that(Multisets.union(ms1, ms2)).has().exactly("a", "a", "b", "b", "c");
  }

  public void testUnionEqualMultisets() {
    Multiset<String> ms1 = HashMultiset.create(Arrays.asList("a", "b", "a"));
    Multiset<String> ms2 = HashMultiset.create(Arrays.asList("a", "b", "a"));
    assertEquals(ms1, Multisets.union(ms1, ms2));
  }

  public void testUnionEmptyNonempty() {
    Multiset<String> ms1 = HashMultiset.create();
    Multiset<String> ms2 = HashMultiset.create(Arrays.asList("a", "b", "a"));
    assertEquals(ms2, Multisets.union(ms1, ms2));
  }

  public void testUnionNonemptyEmpty() {
    Multiset<String> ms1 = HashMultiset.create(Arrays.asList("a", "b", "a"));
    Multiset<String> ms2 = HashMultiset.create();
    assertEquals(ms1, Multisets.union(ms1, ms2));
  }

  public void testIntersectEmptyNonempty() {
    Multiset<String> ms1 = HashMultiset.create();
    Multiset<String> ms2 = HashMultiset.create(Arrays.asList("a", "b", "a"));
    ASSERT.that(Multisets.intersection(ms1, ms2)).isEmpty();
  }

  public void testIntersectNonemptyEmpty() {
    Multiset<String> ms1 = HashMultiset.create(Arrays.asList("a", "b", "a"));
    Multiset<String> ms2 = HashMultiset.create();
    ASSERT.that(Multisets.intersection(ms1, ms2)).isEmpty();
  }

  public void testSum() {
    Multiset<String> ms1 = HashMultiset.create(Arrays.asList("a", "b", "a"));
    Multiset<String> ms2 = HashMultiset.create(Arrays.asList("b", "c"));
    ASSERT.that(Multisets.sum(ms1, ms2)).has().exactly("a", "a", "b", "b", "c");
  }

  public void testSumEmptyNonempty() {
    Multiset<String> ms1 = HashMultiset.create();
    Multiset<String> ms2 = HashMultiset.create(Arrays.asList("a", "b", "a"));
    ASSERT.that(Multisets.sum(ms1, ms2)).has().exactly("a", "b", "a");
  }

  public void testSumNonemptyEmpty() {
    Multiset<String> ms1 = HashMultiset.create(Arrays.asList("a", "b", "a"));
    Multiset<String> ms2 = HashMultiset.create();
    ASSERT.that(Multisets.sum(ms1, ms2)).has().exactly("a", "b", "a");
  }

  public void testDifferenceWithNoRemovedElements() {
    Multiset<String> ms1 = HashMultiset.create(Arrays.asList("a", "b", "a"));
    Multiset<String> ms2 = HashMultiset.create(Arrays.asList("a"));
    ASSERT.that(Multisets.difference(ms1, ms2)).has().exactly("a", "b");
  }

  public void testDifferenceWithRemovedElement() {
    Multiset<String> ms1 = HashMultiset.create(Arrays.asList("a", "b", "a"));
    Multiset<String> ms2 = HashMultiset.create(Arrays.asList("b"));
    ASSERT.that(Multisets.difference(ms1, ms2)).has().exactly("a", "a");
  }

  public void testDifferenceWithMoreElementsInSecondMultiset() {
    Multiset<String> ms1 = HashMultiset.create(Arrays.asList("a", "b", "a"));
    Multiset<String> ms2 = HashMultiset.create(Arrays.asList("a", "b", "b", "b"));
    Multiset<String> diff = Multisets.difference(ms1, ms2);
    ASSERT.that(diff).has().item("a");
    assertEquals(0, diff.count("b"));
    assertEquals(1, diff.count("a"));
    assertFalse(diff.contains("b"));
    assertTrue(diff.contains("a"));
  }

  public void testDifferenceEmptyNonempty() {
    Multiset<String> ms1 = HashMultiset.create();
    Multiset<String> ms2 = HashMultiset.create(Arrays.asList("a", "b", "a"));
    assertEquals(ms1, Multisets.difference(ms1, ms2));
  }

  public void testDifferenceNonemptyEmpty() {
    Multiset<String> ms1 = HashMultiset.create(Arrays.asList("a", "b", "a"));
    Multiset<String> ms2 = HashMultiset.create();
    assertEquals(ms1, Multisets.difference(ms1, ms2));
  }

  public void testContainsOccurrencesEmpty() {
    Multiset<String> superMultiset = HashMultiset.create(Arrays.asList("a", "b", "a"));
    Multiset<String> subMultiset = HashMultiset.create();
    assertTrue(Multisets.containsOccurrences(superMultiset, subMultiset));
    assertFalse(Multisets.containsOccurrences(subMultiset, superMultiset));
  }

  public void testContainsOccurrences() {
    Multiset<String> superMultiset = HashMultiset.create(Arrays.asList("a", "b", "a"));
    Multiset<String> subMultiset = HashMultiset.create(Arrays.asList("a", "b"));
    assertTrue(Multisets.containsOccurrences(superMultiset, subMultiset));
    assertFalse(Multisets.containsOccurrences(subMultiset, superMultiset));
    Multiset<String> diffMultiset = HashMultiset.create(Arrays.asList("a", "b", "c"));
    assertFalse(Multisets.containsOccurrences(superMultiset, diffMultiset));
    assertTrue(Multisets.containsOccurrences(diffMultiset, subMultiset));
  }

  public void testRetainEmptyOccurrences() {
    Multiset<String> multiset =
        HashMultiset.create(Arrays.asList("a", "b", "a"));
    Multiset<String> toRetain = HashMultiset.create();
    assertTrue(Multisets.retainOccurrences(multiset, toRetain));
    assertTrue(multiset.isEmpty());
  }

  public void testRetainOccurrences() {
    Multiset<String> multiset =
        TreeMultiset.create(Arrays.asList("a", "b", "a", "c"));
    Multiset<String> toRetain =
        HashMultiset.create(Arrays.asList("a", "b", "b"));
    assertTrue(Multisets.retainOccurrences(multiset, toRetain));
    ASSERT.that(multiset).has().exactly("a", "b").inOrder();
  }

  public void testRemoveEmptyOccurrences() {
    Multiset<String> multiset =
        TreeMultiset.create(Arrays.asList("a", "b", "a"));
    Multiset<String> toRemove = HashMultiset.create();
    assertFalse(Multisets.removeOccurrences(multiset, toRemove));
    ASSERT.that(multiset).has().exactly("a", "a", "b").inOrder();
  }

  public void testRemoveOccurrences() {
    Multiset<String> multiset =
        TreeMultiset.create(Arrays.asList("a", "b", "a", "c"));
    Multiset<String> toRemove =
        HashMultiset.create(Arrays.asList("a", "b", "b"));
    assertTrue(Multisets.removeOccurrences(multiset, toRemove));
    ASSERT.that(multiset).has().exactly("a", "c").inOrder();
  }

  @SuppressWarnings("deprecation")
  public void testUnmodifiableMultisetShortCircuit() {
    Multiset<String> mod = HashMultiset.create();
    Multiset<String> unmod = Multisets.unmodifiableMultiset(mod);
    assertNotSame(mod, unmod);
    assertSame(unmod, Multisets.unmodifiableMultiset(unmod));
    ImmutableMultiset<String> immutable = ImmutableMultiset.of("a", "a", "b", "a");
    assertSame(immutable, Multisets.unmodifiableMultiset(immutable));
    assertSame(immutable, Multisets.unmodifiableMultiset((Multiset<String>) immutable));
  }

  public void testHighestCountFirst() {
    Multiset<String> multiset = HashMultiset.create(
        Arrays.asList("a", "a", "a", "b", "c", "c"));
    ImmutableMultiset<String> sortedMultiset =
        Multisets.copyHighestCountFirst(multiset);

    ASSERT.that(sortedMultiset.entrySet()).has().exactly(
        Multisets.immutableEntry("a", 3), Multisets.immutableEntry("c", 2),
        Multisets.immutableEntry("b", 1)).inOrder();

    ASSERT.that(sortedMultiset).has().exactly(
        "a",
        "a",
        "a",
        "c",
        "c",
        "b").inOrder();

    ASSERT.that(Multisets.copyHighestCountFirst(ImmutableMultiset.of())).isEmpty();
  }
}


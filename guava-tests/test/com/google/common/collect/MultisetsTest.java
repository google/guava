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
import com.google.common.collect.testing.DerivedComparable;
import com.google.common.testing.CollectorTester;
import com.google.common.testing.NullPointerTester;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;
import junit.framework.TestCase;

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
    assertThat(set)
        .containsExactly(
            new DerivedComparable("bar"),
            new DerivedComparable("bar"),
            new DerivedComparable("bar"),
            new DerivedComparable("foo"),
            new DerivedComparable("foo"))
        .inOrder();
  }

  public void testNewTreeMultisetNonGeneric() {
    TreeMultiset<LegacyComparable> set = TreeMultiset.create();
    assertTrue(set.isEmpty());
    set.add(new LegacyComparable("foo"), 2);
    set.add(new LegacyComparable("bar"), 3);
    assertThat(set)
        .containsExactly(
            new LegacyComparable("bar"),
            new LegacyComparable("bar"),
            new LegacyComparable("bar"),
            new LegacyComparable("foo"),
            new LegacyComparable("foo"))
        .inOrder();
  }

  public void testNewTreeMultisetComparator() {
    TreeMultiset<String> multiset = TreeMultiset.create(Collections.reverseOrder());
    multiset.add("bar", 3);
    multiset.add("foo", 2);
    assertThat(multiset).containsExactly("foo", "foo", "bar", "bar", "bar").inOrder();
  }

  public void testRetainOccurrencesEmpty() {
    Multiset<String> multiset = HashMultiset.create();
    Multiset<String> toRetain = HashMultiset.create(Arrays.asList("a", "b", "a"));
    assertFalse(Multisets.retainOccurrences(multiset, toRetain));
    assertThat(multiset).isEmpty();
  }

  public void testRemoveOccurrencesIterableEmpty() {
    Multiset<String> multiset = HashMultiset.create();
    Iterable<String> toRemove = Arrays.asList("a", "b", "a");
    assertFalse(Multisets.removeOccurrences(multiset, toRemove));
    assertTrue(multiset.isEmpty());
  }

  public void testRemoveOccurrencesMultisetEmpty() {
    Multiset<String> multiset = HashMultiset.create();
    Multiset<String> toRemove = HashMultiset.create(Arrays.asList("a", "b", "a"));
    assertFalse(Multisets.removeOccurrences(multiset, toRemove));
    assertTrue(multiset.isEmpty());
  }

  public void testUnion() {
    Multiset<String> ms1 = HashMultiset.create(Arrays.asList("a", "b", "a"));
    Multiset<String> ms2 = HashMultiset.create(Arrays.asList("a", "b", "b", "c"));
    assertThat(Multisets.union(ms1, ms2)).containsExactly("a", "a", "b", "b", "c");
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
    assertThat(Multisets.intersection(ms1, ms2)).isEmpty();
  }

  public void testIntersectNonemptyEmpty() {
    Multiset<String> ms1 = HashMultiset.create(Arrays.asList("a", "b", "a"));
    Multiset<String> ms2 = HashMultiset.create();
    assertThat(Multisets.intersection(ms1, ms2)).isEmpty();
  }

  public void testSum() {
    Multiset<String> ms1 = HashMultiset.create(Arrays.asList("a", "b", "a"));
    Multiset<String> ms2 = HashMultiset.create(Arrays.asList("b", "c"));
    assertThat(Multisets.sum(ms1, ms2)).containsExactly("a", "a", "b", "b", "c");
  }

  public void testSumEmptyNonempty() {
    Multiset<String> ms1 = HashMultiset.create();
    Multiset<String> ms2 = HashMultiset.create(Arrays.asList("a", "b", "a"));
    assertThat(Multisets.sum(ms1, ms2)).containsExactly("a", "b", "a");
  }

  public void testSumNonemptyEmpty() {
    Multiset<String> ms1 = HashMultiset.create(Arrays.asList("a", "b", "a"));
    Multiset<String> ms2 = HashMultiset.create();
    assertThat(Multisets.sum(ms1, ms2)).containsExactly("a", "b", "a");
  }

  public void testDifferenceWithNoRemovedElements() {
    Multiset<String> ms1 = HashMultiset.create(Arrays.asList("a", "b", "a"));
    Multiset<String> ms2 = HashMultiset.create(Arrays.asList("a"));
    assertThat(Multisets.difference(ms1, ms2)).containsExactly("a", "b");
  }

  public void testDifferenceWithRemovedElement() {
    Multiset<String> ms1 = HashMultiset.create(Arrays.asList("a", "b", "a"));
    Multiset<String> ms2 = HashMultiset.create(Arrays.asList("b"));
    assertThat(Multisets.difference(ms1, ms2)).containsExactly("a", "a");
  }

  public void testDifferenceWithMoreElementsInSecondMultiset() {
    Multiset<String> ms1 = HashMultiset.create(Arrays.asList("a", "b", "a"));
    Multiset<String> ms2 = HashMultiset.create(Arrays.asList("a", "b", "b", "b"));
    Multiset<String> diff = Multisets.difference(ms1, ms2);
    assertThat(diff).contains("a");
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
    Multiset<String> multiset = HashMultiset.create(Arrays.asList("a", "b", "a"));
    Multiset<String> toRetain = HashMultiset.create();
    assertTrue(Multisets.retainOccurrences(multiset, toRetain));
    assertTrue(multiset.isEmpty());
  }

  public void testRetainOccurrences() {
    Multiset<String> multiset = TreeMultiset.create(Arrays.asList("a", "b", "a", "c"));
    Multiset<String> toRetain = HashMultiset.create(Arrays.asList("a", "b", "b"));
    assertTrue(Multisets.retainOccurrences(multiset, toRetain));
    assertThat(multiset).containsExactly("a", "b").inOrder();
  }

  public void testRemoveEmptyOccurrencesMultiset() {
    Multiset<String> multiset = TreeMultiset.create(Arrays.asList("a", "b", "a"));
    Multiset<String> toRemove = HashMultiset.create();
    assertFalse(Multisets.removeOccurrences(multiset, toRemove));
    assertThat(multiset).containsExactly("a", "a", "b").inOrder();
  }

  public void testRemoveOccurrencesMultiset() {
    Multiset<String> multiset = TreeMultiset.create(Arrays.asList("a", "b", "a", "c"));
    Multiset<String> toRemove = HashMultiset.create(Arrays.asList("a", "b", "b"));
    assertTrue(Multisets.removeOccurrences(multiset, toRemove));
    assertThat(multiset).containsExactly("a", "c").inOrder();
  }

  public void testRemoveEmptyOccurrencesIterable() {
    Multiset<String> multiset = TreeMultiset.create(Arrays.asList("a", "b", "a"));
    Iterable<String> toRemove = ImmutableList.of();
    assertFalse(Multisets.removeOccurrences(multiset, toRemove));
    assertThat(multiset).containsExactly("a", "a", "b").inOrder();
  }

  public void testRemoveOccurrencesMultisetIterable() {
    Multiset<String> multiset = TreeMultiset.create(Arrays.asList("a", "b", "a", "c"));
    List<String> toRemove = Arrays.asList("a", "b", "b");
    assertTrue(Multisets.removeOccurrences(multiset, toRemove));
    assertThat(multiset).containsExactly("a", "c").inOrder();
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
    Multiset<String> multiset = HashMultiset.create(Arrays.asList("a", "a", "a", "b", "c", "c"));
    ImmutableMultiset<String> sortedMultiset = Multisets.copyHighestCountFirst(multiset);

    assertThat(sortedMultiset.entrySet())
        .containsExactly(
            Multisets.immutableEntry("a", 3),
            Multisets.immutableEntry("c", 2),
            Multisets.immutableEntry("b", 1))
        .inOrder();

    assertThat(sortedMultiset).containsExactly("a", "a", "a", "c", "c", "b").inOrder();

    assertThat(Multisets.copyHighestCountFirst(ImmutableMultiset.of())).isEmpty();
  }

  public void testToMultisetCountFunction() {
    BiPredicate<Multiset<String>, Multiset<String>> equivalence =
        (ms1, ms2) ->
            ms1.equals(ms2)
                && ImmutableList.copyOf(ms1.entrySet())
                    .equals(ImmutableList.copyOf(ms2.entrySet()));
    CollectorTester.of(
            Multisets.<Multiset.Entry<String>, String, Multiset<String>>toMultiset(
                Multiset.Entry::getElement, Multiset.Entry::getCount, LinkedHashMultiset::create),
            equivalence)
        .expectCollects(ImmutableMultiset.<String>of())
        .expectCollects(
            ImmutableMultiset.of("a", "a", "b", "c", "c", "c"),
            Multisets.immutableEntry("a", 1),
            Multisets.immutableEntry("b", 1),
            Multisets.immutableEntry("a", 1),
            Multisets.immutableEntry("c", 3));
  }

  @GwtIncompatible // NullPointerTester
  public void testNullPointers() {
    new NullPointerTester().testAllPublicStaticMethods(Multisets.class);
  }
}

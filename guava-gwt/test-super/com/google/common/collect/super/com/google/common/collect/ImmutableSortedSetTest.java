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

package com.google.common.collect;

import static java.util.Arrays.asList;
import static org.truth0.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Unit tests for {@link ImmutableSortedSet}.
 *
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public class ImmutableSortedSetTest extends AbstractImmutableSetTest {

  // enum singleton pattern
  private enum StringLengthComparator implements Comparator<String> {
    INSTANCE;

    @Override
    public int compare(String a, String b) {
      return a.length() - b.length();
    }
  }

  private static final Comparator<String> STRING_LENGTH
      = StringLengthComparator.INSTANCE;

  @Override protected SortedSet<String> of() {
    return ImmutableSortedSet.of();
  }

  @Override protected SortedSet<String> of(String e) {
    return ImmutableSortedSet.of(e);
  }

  @Override protected SortedSet<String> of(String e1, String e2) {
    return ImmutableSortedSet.of(e1, e2);
  }

  @Override protected SortedSet<String> of(String e1, String e2, String e3) {
    return ImmutableSortedSet.of(e1, e2, e3);
  }

  @Override protected SortedSet<String> of(
      String e1, String e2, String e3, String e4) {
    return ImmutableSortedSet.of(e1, e2, e3, e4);
  }

  @Override protected SortedSet<String> of(
      String e1, String e2, String e3, String e4, String e5) {
    return ImmutableSortedSet.of(e1, e2, e3, e4, e5);
  }

  @Override protected SortedSet<String> of(String e1, String e2, String e3,
      String e4, String e5, String e6, String... rest) {
    return ImmutableSortedSet.of(e1, e2, e3, e4, e5, e6, rest);
  }

  @Override protected SortedSet<String> copyOf(String[] elements) {
    return ImmutableSortedSet.copyOf(elements);
  }

  @Override protected SortedSet<String> copyOf(Collection<String> elements) {
    return ImmutableSortedSet.copyOf(elements);
  }

  @Override protected SortedSet<String> copyOf(Iterable<String> elements) {
    return ImmutableSortedSet.copyOf(elements);
  }

  @Override protected SortedSet<String> copyOf(Iterator<String> elements) {
    return ImmutableSortedSet.copyOf(elements);
  }

  public void testEmpty_comparator() {
    SortedSet<String> set = of();
    assertSame(Ordering.natural(), set.comparator());
  }

  public void testEmpty_headSet() {
    SortedSet<String> set = of();
    assertSame(set, set.headSet("c"));
  }

  public void testEmpty_tailSet() {
    SortedSet<String> set = of();
    assertSame(set, set.tailSet("f"));
  }

  public void testEmpty_subSet() {
    SortedSet<String> set = of();
    assertSame(set, set.subSet("c", "f"));
  }

  public void testEmpty_first() {
    SortedSet<String> set = of();
    try {
      set.first();
      fail();
    } catch (NoSuchElementException expected) {
    }
  }

  public void testEmpty_last() {
    SortedSet<String> set = of();
    try {
      set.last();
      fail();
    } catch (NoSuchElementException expected) {
    }
  }

  public void testSingle_comparator() {
    SortedSet<String> set = of("e");
    assertSame(Ordering.natural(), set.comparator());
  }

  public void testSingle_headSet() {
    SortedSet<String> set = of("e");
    assertTrue(set.headSet("g") instanceof ImmutableSortedSet);
    ASSERT.that(set.headSet("g")).has().item("e");
    assertSame(of(), set.headSet("c"));
    assertSame(of(), set.headSet("e"));
  }

  public void testSingle_tailSet() {
    SortedSet<String> set = of("e");
    assertTrue(set.tailSet("c") instanceof ImmutableSortedSet);
    ASSERT.that(set.tailSet("c")).has().item("e");
    ASSERT.that(set.tailSet("e")).has().item("e");
    assertSame(of(), set.tailSet("g"));
  }

  public void testSingle_subSet() {
    SortedSet<String> set = of("e");
    assertTrue(set.subSet("c", "g") instanceof ImmutableSortedSet);
    ASSERT.that(set.subSet("c", "g")).has().item("e");
    ASSERT.that(set.subSet("e", "g")).has().item("e");
    assertSame(of(), set.subSet("f", "g"));
    assertSame(of(), set.subSet("c", "e"));
    assertSame(of(), set.subSet("c", "d"));
  }

  public void testSingle_first() {
    SortedSet<String> set = of("e");
    assertEquals("e", set.first());
  }

  public void testSingle_last() {
    SortedSet<String> set = of("e");
    assertEquals("e", set.last());
  }

  public void testOf_ordering() {
    SortedSet<String> set = of("e", "a", "f", "b", "d", "c");
    ASSERT.that(set).has().exactly("a", "b", "c", "d", "e", "f").inOrder();
  }

  /*
   * Tests that we workaround GWT bug #3621 (or that it is already fixed).
   *
   * A call to of() with a parameter that is not a plain Object[] (here,
   * Interface[]) creates a RegularImmutableSortedSet backed by an array of that
   * type. Later, RegularImmutableSortedSet.toArray() calls System.arraycopy()
   * to copy from that array to the destination array. This would be fine, but
   * GWT has a bug: It refuses to copy from an E[] to an Object[] when E is an
   * interface type.
   */
  // TODO: test other collections for this problem
  public void testOf_gwtArraycopyBug() {
    /*
     * The test requires:
     *
     * 1) An interface I extending Comparable<I> so that the created array is of
     * an interface type. 2) An instance of a class implementing that interface
     * so that we can pass non-null instances of the interface.
     *
     * (Currently it's safe to pass instances for which compareTo() always
     * returns 0, but if we had a SingletonImmutableSortedSet, this might no
     * longer be the case.)
     *
     * javax.naming.Name and java.util.concurrent.Delayed might work, but
     * they're fairly obscure, we've invented our own interface and class.
     */
    Interface a = new Impl();
    Interface b = new Impl();
    ImmutableSortedSet<Interface> set = ImmutableSortedSet.of(a, b);
    set.toArray();
    set.toArray(new Object[2]);
  }

  interface Interface extends Comparable<Interface> {
  }
  static class Impl implements Interface {
    static int nextId;
    Integer id = nextId++;

    @Override public int compareTo(Interface other) {
      return id.compareTo(((Impl) other).id);
    }
  }

  public void testOf_ordering_dupes() {
    SortedSet<String> set = of("e", "a", "e", "f", "b", "b", "d", "a", "c");
    ASSERT.that(set).has().exactly("a", "b", "c", "d", "e", "f").inOrder();
  }

  public void testOf_comparator() {
    SortedSet<String> set = of("e", "a", "f", "b", "d", "c");
    assertSame(Ordering.natural(), set.comparator());
  }

  public void testOf_headSet() {
    SortedSet<String> set = of("e", "f", "b", "d", "c");
    assertTrue(set.headSet("e") instanceof ImmutableSortedSet);
    ASSERT.that(set.headSet("e")).has().exactly("b", "c", "d").inOrder();
    ASSERT.that(set.headSet("g")).has().exactly("b", "c", "d", "e", "f").inOrder();
    assertSame(of(), set.headSet("a"));
    assertSame(of(), set.headSet("b"));
  }

  public void testOf_tailSet() {
    SortedSet<String> set = of("e", "f", "b", "d", "c");
    assertTrue(set.tailSet("e") instanceof ImmutableSortedSet);
    ASSERT.that(set.tailSet("e")).has().exactly("e", "f").inOrder();
    ASSERT.that(set.tailSet("a")).has().exactly("b", "c", "d", "e", "f").inOrder();
    assertSame(of(), set.tailSet("g"));
  }

  public void testOf_subSet() {
    SortedSet<String> set = of("e", "f", "b", "d", "c");
    assertTrue(set.subSet("c", "e") instanceof ImmutableSortedSet);
    ASSERT.that(set.subSet("c", "e")).has().exactly("c", "d").inOrder();
    ASSERT.that(set.subSet("a", "g")).has().exactly("b", "c", "d", "e", "f").inOrder();
    assertSame(of(), set.subSet("a", "b"));
    assertSame(of(), set.subSet("g", "h"));
    assertSame(of(), set.subSet("c", "c"));
    try {
      set.subSet("e", "c");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testOf_first() {
    SortedSet<String> set = of("e", "f", "b", "d", "c");
    assertEquals("b", set.first());
  }

  public void testOf_last() {
    SortedSet<String> set = of("e", "f", "b", "d", "c");
    assertEquals("f", set.last());
  }

  /* "Explicit" indicates an explicit comparator. */

  public void testExplicit_ordering() {
    SortedSet<String> set = ImmutableSortedSet.orderedBy(STRING_LENGTH).add(
        "in", "the", "quick", "jumped", "over", "a").build();
    ASSERT.that(set).has().exactly("a", "in", "the", "over", "quick", "jumped").inOrder();
  }

  public void testExplicit_ordering_dupes() {
    SortedSet<String> set = ImmutableSortedSet.orderedBy(STRING_LENGTH).add(
        "in", "the", "quick", "brown", "fox", "jumped",
        "over", "a", "lazy", "dog").build();
    ASSERT.that(set).has().exactly("a", "in", "the", "over", "quick", "jumped").inOrder();
  }

  public void testExplicit_contains() {
    SortedSet<String> set = ImmutableSortedSet.orderedBy(STRING_LENGTH).add(
        "in", "the", "quick", "jumped", "over", "a").build();
    assertTrue(set.contains("quick"));
    assertTrue(set.contains("google"));
    assertFalse(set.contains(""));
    assertFalse(set.contains("california"));
    assertFalse(set.contains(null));
  }

  public void testExplicit_containsMismatchedTypes() {
    SortedSet<String> set = ImmutableSortedSet.orderedBy(STRING_LENGTH).add(
        "in", "the", "quick", "jumped", "over", "a").build();
    assertFalse(set.contains(3.7));
  }

  public void testExplicit_comparator() {
    SortedSet<String> set = ImmutableSortedSet.orderedBy(STRING_LENGTH).add(
        "in", "the", "quick", "jumped", "over", "a").build();
    assertSame(STRING_LENGTH, set.comparator());
  }

  public void testExplicit_headSet() {
    SortedSet<String> set = ImmutableSortedSet.orderedBy(STRING_LENGTH).add(
        "in", "the", "quick", "jumped", "over", "a").build();
    assertTrue(set.headSet("a") instanceof ImmutableSortedSet);
    assertTrue(set.headSet("fish") instanceof ImmutableSortedSet);
    ASSERT.that(set.headSet("fish")).has().exactly("a", "in", "the").inOrder();
    ASSERT.that(set.headSet("california")).has()
        .exactly("a", "in", "the", "over", "quick", "jumped").inOrder();
    assertTrue(set.headSet("a").isEmpty());
    assertTrue(set.headSet("").isEmpty());
  }

  public void testExplicit_tailSet() {
    SortedSet<String> set = ImmutableSortedSet.orderedBy(STRING_LENGTH).add(
        "in", "the", "quick", "jumped", "over", "a").build();
    assertTrue(set.tailSet("california") instanceof ImmutableSortedSet);
    assertTrue(set.tailSet("fish") instanceof ImmutableSortedSet);
    ASSERT.that(set.tailSet("fish")).has().exactly("over", "quick", "jumped").inOrder();
    ASSERT.that(
        set.tailSet("a")).has().exactly("a", "in", "the", "over", "quick", "jumped").inOrder();
    assertTrue(set.tailSet("california").isEmpty());
  }

  public void testExplicit_subSet() {
    SortedSet<String> set = ImmutableSortedSet.orderedBy(STRING_LENGTH).add(
        "in", "the", "quick", "jumped", "over", "a").build();
    assertTrue(set.subSet("the", "quick") instanceof ImmutableSortedSet);
    assertTrue(set.subSet("", "b") instanceof ImmutableSortedSet);
    ASSERT.that(set.subSet("the", "quick")).has().exactly("the", "over").inOrder();
    ASSERT.that(set.subSet("a", "california"))
        .has().exactly("a", "in", "the", "over", "quick", "jumped").inOrder();
    assertTrue(set.subSet("", "b").isEmpty());
    assertTrue(set.subSet("vermont", "california").isEmpty());
    assertTrue(set.subSet("aaa", "zzz").isEmpty());
    try {
      set.subSet("quick", "the");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testExplicit_first() {
    SortedSet<String> set = ImmutableSortedSet.orderedBy(STRING_LENGTH).add(
        "in", "the", "quick", "jumped", "over", "a").build();
    assertEquals("a", set.first());
  }

  public void testExplicit_last() {
    SortedSet<String> set = ImmutableSortedSet.orderedBy(STRING_LENGTH).add(
        "in", "the", "quick", "jumped", "over", "a").build();
    assertEquals("jumped", set.last());
  }

  public void testCopyOf_ordering() {
    SortedSet<String> set =
        copyOf(asList("e", "a", "f", "b", "d", "c"));
    ASSERT.that(set).has().exactly("a", "b", "c", "d", "e", "f").inOrder();
  }

  public void testCopyOf_ordering_dupes() {
    SortedSet<String> set =
        copyOf(asList("e", "a", "e", "f", "b", "b", "d", "a", "c"));
    ASSERT.that(set).has().exactly("a", "b", "c", "d", "e", "f").inOrder();
  }

  public void testCopyOf_subSet() {
    SortedSet<String> set = of("e", "a", "f", "b", "d", "c");
    SortedSet<String> subset = set.subSet("c", "e");
    SortedSet<String> copy = copyOf(subset);
    assertEquals(subset, copy);
  }

  public void testCopyOf_headSet() {
    SortedSet<String> set = of("e", "a", "f", "b", "d", "c");
    SortedSet<String> headset = set.headSet("d");
    SortedSet<String> copy = copyOf(headset);
    assertEquals(headset, copy);
  }

  public void testCopyOf_tailSet() {
    SortedSet<String> set = of("e", "a", "f", "b", "d", "c");
    SortedSet<String> tailset = set.tailSet("d");
    SortedSet<String> copy = copyOf(tailset);
    assertEquals(tailset, copy);
  }

  public void testCopyOf_comparator() {
    SortedSet<String> set = copyOf(asList("e", "a", "f", "b", "d", "c"));
    assertSame(Ordering.natural(), set.comparator());
  }

  public void testCopyOf_iterator_ordering() {
    SortedSet<String> set = copyOf(asIterator("e", "a", "f", "b", "d", "c"));
    ASSERT.that(set).has().exactly("a", "b", "c", "d", "e", "f").inOrder();
  }

  public void testCopyOf_iterator_ordering_dupes() {
    SortedSet<String> set =
        copyOf(asIterator("e", "a", "e", "f", "b", "b", "d", "a", "c"));
    ASSERT.that(set).has().exactly("a", "b", "c", "d", "e", "f").inOrder();
  }

  public void testCopyOf_iterator_comparator() {
    SortedSet<String> set = copyOf(asIterator("e", "a", "f", "b", "d", "c"));
    assertSame(Ordering.natural(), set.comparator());
  }

  public void testCopyOf_sortedSet_ordering() {
    SortedSet<String> set =
        copyOf(Sets.newTreeSet(asList("e", "a", "f", "b", "d", "c")));
    ASSERT.that(set).has().exactly("a", "b", "c", "d", "e", "f").inOrder();
  }

  public void testCopyOf_sortedSet_comparator() {
    SortedSet<String> set = copyOf(Sets.<String>newTreeSet());
    assertSame(Ordering.natural(), set.comparator());
  }

  public void testCopyOfExplicit_ordering() {
    SortedSet<String> set =
        ImmutableSortedSet.copyOf(STRING_LENGTH, asList(
            "in", "the", "quick", "jumped", "over", "a"));
    ASSERT.that(set).has().exactly("a", "in", "the", "over", "quick", "jumped").inOrder();
  }

  public void testCopyOfExplicit_ordering_dupes() {
    SortedSet<String> set =
        ImmutableSortedSet.copyOf(STRING_LENGTH, asList(
            "in", "the", "quick", "brown", "fox", "jumped", "over", "a",
            "lazy", "dog"));
    ASSERT.that(set).has().exactly("a", "in", "the", "over", "quick", "jumped").inOrder();
  }

  public void testCopyOfExplicit_comparator() {
    SortedSet<String> set =
        ImmutableSortedSet.copyOf(STRING_LENGTH, asList(
            "in", "the", "quick", "jumped", "over", "a"));
    assertSame(STRING_LENGTH, set.comparator());
  }

  public void testCopyOfExplicit_iterator_ordering() {
    SortedSet<String> set =
        ImmutableSortedSet.copyOf(STRING_LENGTH, asIterator(
            "in", "the", "quick", "jumped", "over", "a"));
    ASSERT.that(set).has().exactly("a", "in", "the", "over", "quick", "jumped").inOrder();
  }

  public void testCopyOfExplicit_iterator_ordering_dupes() {
    SortedSet<String> set =
        ImmutableSortedSet.copyOf(STRING_LENGTH, asIterator(
            "in", "the", "quick", "brown", "fox", "jumped", "over", "a",
            "lazy", "dog"));
    ASSERT.that(set).has().exactly("a", "in", "the", "over", "quick", "jumped").inOrder();
  }

  public void testCopyOfExplicit_iterator_comparator() {
    SortedSet<String> set =
        ImmutableSortedSet.copyOf(STRING_LENGTH, asIterator(
            "in", "the", "quick", "jumped", "over", "a"));
    assertSame(STRING_LENGTH, set.comparator());
  }

  public void testCopyOf_sortedSetIterable() {
    SortedSet<String> input = Sets.newTreeSet(STRING_LENGTH);
    Collections.addAll(input, "in", "the", "quick", "jumped", "over", "a");
    SortedSet<String> set = copyOf(input);
    ASSERT.that(set).has().exactly("a", "in", "jumped", "over", "quick", "the").inOrder();
  }

  public void testCopyOfSorted_natural_ordering() {
    SortedSet<String> input = Sets.newTreeSet(
        asList("in", "the", "quick", "jumped", "over", "a"));
    SortedSet<String> set = ImmutableSortedSet.copyOfSorted(input);
    ASSERT.that(set).has().exactly("a", "in", "jumped", "over", "quick", "the").inOrder();
  }

  public void testCopyOfSorted_natural_comparator() {
    SortedSet<String> input =
        Sets.newTreeSet(asList("in", "the", "quick", "jumped", "over", "a"));
    SortedSet<String> set = ImmutableSortedSet.copyOfSorted(input);
    assertSame(Ordering.natural(), set.comparator());
  }

  public void testCopyOfSorted_explicit_ordering() {
    SortedSet<String> input = Sets.newTreeSet(STRING_LENGTH);
    Collections.addAll(input, "in", "the", "quick", "jumped", "over", "a");
    SortedSet<String> set = ImmutableSortedSet.copyOfSorted(input);
    ASSERT.that(set).has().exactly("a", "in", "the", "over", "quick", "jumped").inOrder();
    assertSame(STRING_LENGTH, set.comparator());
  }

  public void testEquals_bothDefaultOrdering() {
    SortedSet<String> set = of("a", "b", "c");
    assertEquals(set, Sets.newTreeSet(asList("a", "b", "c")));
    assertEquals(Sets.newTreeSet(asList("a", "b", "c")), set);
    assertFalse(set.equals(Sets.newTreeSet(asList("a", "b", "d"))));
    assertFalse(Sets.newTreeSet(asList("a", "b", "d")).equals(set));
    assertFalse(set.equals(Sets.newHashSet(4, 5, 6)));
    assertFalse(Sets.newHashSet(4, 5, 6).equals(set));
  }

  public void testEquals_bothExplicitOrdering() {
    SortedSet<String> set = of("in", "the", "a");
    assertEquals(Sets.newTreeSet(asList("in", "the", "a")), set);
    assertFalse(set.equals(Sets.newTreeSet(asList("in", "the", "house"))));
    assertFalse(Sets.newTreeSet(asList("in", "the", "house")).equals(set));
    assertFalse(set.equals(Sets.newHashSet(4, 5, 6)));
    assertFalse(Sets.newHashSet(4, 5, 6).equals(set));

    Set<String> complex = Sets.newTreeSet(STRING_LENGTH);
    Collections.addAll(complex, "in", "the", "a");
    assertEquals(set, complex);
  }

  public void testEquals_bothDefaultOrdering_StringVsInt() {
    SortedSet<String> set = of("a", "b", "c");
    assertFalse(set.equals(Sets.newTreeSet(asList(4, 5, 6))));
    assertNotEqualLenient(Sets.newTreeSet(asList(4, 5, 6)), set);
  }

  public void testEquals_bothExplicitOrdering_StringVsInt() {
    SortedSet<String> set = of("in", "the", "a");
    assertFalse(set.equals(Sets.newTreeSet(asList(4, 5, 6))));
    assertNotEqualLenient(Sets.newTreeSet(asList(4, 5, 6)), set);
  }

  public void testContainsAll_notSortedSet() {
    SortedSet<String> set = of("a", "b", "f");
    assertTrue(set.containsAll(Collections.emptyList()));
    assertTrue(set.containsAll(asList("b")));
    assertTrue(set.containsAll(asList("b", "b")));
    assertTrue(set.containsAll(asList("b", "f")));
    assertTrue(set.containsAll(asList("b", "f", "a")));
    assertFalse(set.containsAll(asList("d")));
    assertFalse(set.containsAll(asList("z")));
    assertFalse(set.containsAll(asList("b", "d")));
    assertFalse(set.containsAll(asList("f", "d", "a")));
  }

  public void testContainsAll_sameComparator() {
    SortedSet<String> set = of("a", "b", "f");
    assertTrue(set.containsAll(Sets.newTreeSet()));
    assertTrue(set.containsAll(Sets.newTreeSet(asList("b"))));
    assertTrue(set.containsAll(Sets.newTreeSet(asList("a", "f"))));
    assertTrue(set.containsAll(Sets.newTreeSet(asList("a", "b", "f"))));
    assertFalse(set.containsAll(Sets.newTreeSet(asList("d"))));
    assertFalse(set.containsAll(Sets.newTreeSet(asList("z"))));
    assertFalse(set.containsAll(Sets.newTreeSet(asList("b", "d"))));
    assertFalse(set.containsAll(Sets.newTreeSet(asList("f", "d", "a"))));
  }

  public void testContainsAll_sameComparator_StringVsInt() {
    SortedSet<String> set = of("a", "b", "f");
    SortedSet<Integer> unexpected = Sets.newTreeSet(Ordering.natural());
    unexpected.addAll(asList(1, 2, 3));
    assertFalse(set.containsAll(unexpected));
  }

  public void testContainsAll_differentComparator() {
    Comparator<Comparable<?>> comparator = Collections.reverseOrder();
    SortedSet<String> set = new ImmutableSortedSet.Builder<String>(comparator)
        .add("a", "b", "f").build();
    assertTrue(set.containsAll(Sets.newTreeSet()));
    assertTrue(set.containsAll(Sets.newTreeSet(asList("b"))));
    assertTrue(set.containsAll(Sets.newTreeSet(asList("a", "f"))));
    assertTrue(set.containsAll(Sets.newTreeSet(asList("a", "b", "f"))));
    assertFalse(set.containsAll(Sets.newTreeSet(asList("d"))));
    assertFalse(set.containsAll(Sets.newTreeSet(asList("z"))));
    assertFalse(set.containsAll(Sets.newTreeSet(asList("b", "d"))));
    assertFalse(set.containsAll(Sets.newTreeSet(asList("f", "d", "a"))));
  }

  public void testReverseOrder() {
    SortedSet<String> set = ImmutableSortedSet.<String>reverseOrder()
        .add("a", "b", "c").build();
    ASSERT.that(set).has().exactly("c", "b", "a").inOrder();
    assertEquals(Ordering.natural().reverse(), set.comparator());
  }

  private static final Comparator<Object> TO_STRING
      = new Comparator<Object>() {
          @Override
          public int compare(Object o1, Object o2) {
            return o1.toString().compareTo(o2.toString());
          }
        };

  public void testSupertypeComparator() {
    SortedSet<Integer> set = new ImmutableSortedSet.Builder<Integer>(TO_STRING)
        .add(3, 12, 101, 44).build();
    ASSERT.that(set).has().exactly(101, 12, 3, 44).inOrder();
  }

  public void testSupertypeComparatorSubtypeElements() {
    SortedSet<Number> set = new ImmutableSortedSet.Builder<Number>(TO_STRING)
        .add(3, 12, 101, 44).build();
    ASSERT.that(set).has().exactly(101, 12, 3, 44).inOrder();
  }

  @Override <E extends Comparable<E>> ImmutableSortedSet.Builder<E> builder() {
    return ImmutableSortedSet.naturalOrder();
  }

  @Override int getComplexBuilderSetLastElement() {
    return 0x00FFFFFF;
  }

  public void testLegacyComparable_of() {
    ImmutableSortedSet<LegacyComparable> set0 = ImmutableSortedSet.of();

    @SuppressWarnings("unchecked") // using a legacy comparable
    ImmutableSortedSet<LegacyComparable> set1 = ImmutableSortedSet.of(
        LegacyComparable.Z);

    @SuppressWarnings("unchecked") // using a legacy comparable
    ImmutableSortedSet<LegacyComparable> set2 = ImmutableSortedSet.of(
        LegacyComparable.Z, LegacyComparable.Y);
  }

  public void testLegacyComparable_copyOf_collection() {
    ImmutableSortedSet<LegacyComparable> set
        = ImmutableSortedSet.copyOf(LegacyComparable.VALUES_BACKWARD);
    assertTrue(Iterables.elementsEqual(LegacyComparable.VALUES_FORWARD, set));
  }

  public void testLegacyComparable_copyOf_iterator() {
    ImmutableSortedSet<LegacyComparable> set = ImmutableSortedSet.copyOf(
        LegacyComparable.VALUES_BACKWARD.iterator());
    assertTrue(Iterables.elementsEqual(LegacyComparable.VALUES_FORWARD, set));
  }

  public void testLegacyComparable_builder_natural() {
    @SuppressWarnings("unchecked")
    // Note: IntelliJ wrongly reports an error for this statement
    ImmutableSortedSet.Builder<LegacyComparable> builder
        = ImmutableSortedSet.<LegacyComparable>naturalOrder();

    builder.addAll(LegacyComparable.VALUES_BACKWARD);
    builder.add(LegacyComparable.X);
    builder.add(LegacyComparable.Y, LegacyComparable.Z);

    ImmutableSortedSet<LegacyComparable> set = builder.build();
    assertTrue(Iterables.elementsEqual(LegacyComparable.VALUES_FORWARD, set));
  }

  public void testLegacyComparable_builder_reverse() {
    @SuppressWarnings("unchecked")
    // Note: IntelliJ wrongly reports an error for this statement
    ImmutableSortedSet.Builder<LegacyComparable> builder
        = ImmutableSortedSet.<LegacyComparable>reverseOrder();

    builder.addAll(LegacyComparable.VALUES_FORWARD);
    builder.add(LegacyComparable.X);
    builder.add(LegacyComparable.Y, LegacyComparable.Z);

    ImmutableSortedSet<LegacyComparable> set = builder.build();
    assertTrue(Iterables.elementsEqual(LegacyComparable.VALUES_BACKWARD, set));
  }

  @SuppressWarnings({"deprecation", "static-access"})
  public void testBuilderMethod() {
    try {
      ImmutableSortedSet.builder();
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }

  public void testAsList() {
    ImmutableSet<String> set = ImmutableSortedSet.of("a", "e", "i", "o", "u");
    ImmutableList<String> list = set.asList();
    assertEquals(ImmutableList.of("a", "e", "i", "o", "u"), list);
    assertSame(list, ImmutableList.copyOf(set));
  }

  public void testSubsetAsList() {
    ImmutableSet<String> set
        = ImmutableSortedSet.of("a", "e", "i", "o", "u").subSet("c", "r");
    ImmutableList<String> list = set.asList();
    assertEquals(ImmutableList.of("e", "i", "o"), list);
    assertEquals(list, ImmutableList.copyOf(set));
  }

  public void testAsListInconsistentComprator() {
    ImmutableSet<String> set = ImmutableSortedSet.orderedBy(STRING_LENGTH).add(
        "in", "the", "quick", "jumped", "over", "a").build();
    ImmutableList<String> list = set.asList();
    assertTrue(list.contains("the"));
    assertEquals(2, list.indexOf("the"));
    assertEquals(2, list.lastIndexOf("the"));
    assertFalse(list.contains("dog"));
    assertEquals(-1, list.indexOf("dog"));
    assertEquals(-1, list.lastIndexOf("dog"));
    assertFalse(list.contains("chicken"));
    assertEquals(-1, list.indexOf("chicken"));
    assertEquals(-1, list.lastIndexOf("chicken"));
  }

  private static <E> Iterator<E> asIterator(E... elements) {
    return asList(elements).iterator();
  }

  // In GWT, java.util.TreeSet throws ClassCastException when the comparator
  // throws it, unlike JDK6.  Therefore, we accept ClassCastException as a
  // valid result thrown by java.util.TreeSet#equals.
  private static void assertNotEqualLenient(
      TreeSet<?> unexpected, SortedSet<?> actual) {
    try {
      ASSERT.that(actual).isNotEqualTo(unexpected);
    } catch (ClassCastException accepted) {
    }
  }

  public void testHeadSetInclusive() {
    String[] strings = NUMBER_NAMES.toArray(new String[0]);
    ImmutableSortedSet<String> set = ImmutableSortedSet.copyOf(strings);
    Arrays.sort(strings);
    for (int i = 0; i < strings.length; i++) {
      ASSERT.that(set.headSet(strings[i], true))
          .has().exactlyAs(sortedNumberNames(0, i + 1)).inOrder();
    }
  }

  public void testHeadSetExclusive() {
    String[] strings = NUMBER_NAMES.toArray(new String[0]);
    ImmutableSortedSet<String> set = ImmutableSortedSet.copyOf(strings);
    Arrays.sort(strings);
    for (int i = 0; i < strings.length; i++) {
      ASSERT.that(set.headSet(strings[i], false)).has().exactlyAs(
          sortedNumberNames(0, i)).inOrder();
    }
  }

  public void testTailSetInclusive() {
    String[] strings = NUMBER_NAMES.toArray(new String[0]);
    ImmutableSortedSet<String> set = ImmutableSortedSet.copyOf(strings);
    Arrays.sort(strings);
    for (int i = 0; i < strings.length; i++) {
      ASSERT.that(set.tailSet(strings[i], true)).has().exactlyAs(
          sortedNumberNames(i, strings.length)).inOrder();
    }
  }

  public void testTailSetExclusive() {
    String[] strings = NUMBER_NAMES.toArray(new String[0]);
    ImmutableSortedSet<String> set = ImmutableSortedSet.copyOf(strings);
    Arrays.sort(strings);
    for (int i = 0; i < strings.length; i++) {
      ASSERT.that(set.tailSet(strings[i], false)).has().exactlyAs(
          sortedNumberNames(i + 1, strings.length)).inOrder();
    }
  }

  public void testSubSetExclusiveExclusive() {
    String[] strings = NUMBER_NAMES.toArray(new String[0]);
    ImmutableSortedSet<String> set = ImmutableSortedSet.copyOf(strings);
    Arrays.sort(strings);
    for (int i = 0; i < strings.length; i++) {
      for (int j = i; j < strings.length; j++) {
        ASSERT.that(set.subSet(strings[i], false, strings[j], false))
            .has().exactlyAs(sortedNumberNames(Math.min(i + 1, j), j)).inOrder();
      }
    }
  }

  public void testSubSetInclusiveExclusive() {
    String[] strings = NUMBER_NAMES.toArray(new String[0]);
    ImmutableSortedSet<String> set = ImmutableSortedSet.copyOf(strings);
    Arrays.sort(strings);
    for (int i = 0; i < strings.length; i++) {
      for (int j = i; j < strings.length; j++) {
        ASSERT.that(set.subSet(strings[i], true, strings[j], false))
            .has().exactlyAs(sortedNumberNames(i, j)).inOrder();
      }
    }
  }

  public void testSubSetExclusiveInclusive() {
    String[] strings = NUMBER_NAMES.toArray(new String[0]);
    ImmutableSortedSet<String> set = ImmutableSortedSet.copyOf(strings);
    Arrays.sort(strings);
    for (int i = 0; i < strings.length; i++) {
      for (int j = i; j < strings.length; j++) {
        ASSERT.that(set.subSet(strings[i], false, strings[j], true))
            .has().exactlyAs(sortedNumberNames(i + 1, j + 1)).inOrder();
      }
    }
  }

  public void testSubSetInclusiveInclusive() {
    String[] strings = NUMBER_NAMES.toArray(new String[0]);
    ImmutableSortedSet<String> set = ImmutableSortedSet.copyOf(strings);
    Arrays.sort(strings);
    for (int i = 0; i < strings.length; i++) {
      for (int j = i; j < strings.length; j++) {
        ASSERT.that(set.subSet(strings[i], true, strings[j], true))
            .has().exactlyAs(sortedNumberNames(i, j + 1)).inOrder();
      }
    }
  }

  private static ImmutableList<String> sortedNumberNames(int i, int j) {
    return ImmutableList.copyOf(SORTED_NUMBER_NAMES.subList(i, j));
  }

  private static final ImmutableList<String> NUMBER_NAMES =
      ImmutableList.of("one", "two", "three", "four", "five", "six", "seven");

  private static final ImmutableList<String> SORTED_NUMBER_NAMES =
      Ordering.natural().immutableSortedCopy(NUMBER_NAMES);

  private static class SelfComparableExample implements Comparable<SelfComparableExample> {
    @Override
    public int compareTo(SelfComparableExample o) {
      return 0;
    }
  }

  public void testBuilderGenerics_SelfComparable() {
    ImmutableSortedSet.Builder<SelfComparableExample> natural = ImmutableSortedSet.naturalOrder();
    ImmutableSortedSet.Builder<SelfComparableExample> reverse = ImmutableSortedSet.reverseOrder();
  }

  private static class SuperComparableExample extends SelfComparableExample {}

  public void testBuilderGenerics_SuperComparable() {
    ImmutableSortedSet.Builder<SuperComparableExample> natural = ImmutableSortedSet.naturalOrder();
    ImmutableSortedSet.Builder<SuperComparableExample> reverse = ImmutableSortedSet.reverseOrder();
  }
}


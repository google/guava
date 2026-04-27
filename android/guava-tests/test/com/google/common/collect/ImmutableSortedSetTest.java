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

import static com.google.common.collect.Comparators.isInOrder;
import static com.google.common.collect.Iterables.elementsEqual;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.testing.SerializableTester.reserialize;
import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static com.google.common.truth.Truth.assertThat;
import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static java.util.Arrays.sort;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.testing.ListTestSuiteBuilder;
import com.google.common.collect.testing.NavigableSetTestSuiteBuilder;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSortedSetAsListGenerator;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSortedSetCopyOfGenerator;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSortedSetDescendingAsListGenerator;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSortedSetDescendingGenerator;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSortedSetExplicitComparator;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSortedSetExplicitSuperclassComparatorGenerator;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSortedSetReversedOrderGenerator;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSortedSetSubsetAsListGenerator;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSortedSetUnhashableGenerator;
import com.google.common.collect.testing.testers.SetHashCodeTester;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Unit tests for {@link ImmutableSortedSet}.
 *
 * @author Jared Levy
 */
@GwtCompatible
@NullMarked
public class ImmutableSortedSetTest extends AbstractImmutableSetTest {

  @J2ktIncompatible
  @GwtIncompatible // suite
  @AndroidIncompatible // test-suite builders
  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(
        NavigableSetTestSuiteBuilder.using(new ImmutableSortedSetCopyOfGenerator())
            .named(ImmutableSortedSetTest.class.getName())
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        NavigableSetTestSuiteBuilder.using(new ImmutableSortedSetExplicitComparator())
            .named(ImmutableSortedSetTest.class.getName() + ", explicit comparator, vararg")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        NavigableSetTestSuiteBuilder.using(
                new ImmutableSortedSetExplicitSuperclassComparatorGenerator())
            .named(
                ImmutableSortedSetTest.class.getName()
                    + ", explicit superclass comparator, iterable")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        NavigableSetTestSuiteBuilder.using(new ImmutableSortedSetReversedOrderGenerator())
            .named(ImmutableSortedSetTest.class.getName() + ", reverseOrder, iterator")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        NavigableSetTestSuiteBuilder.using(new ImmutableSortedSetUnhashableGenerator())
            .suppressing(SetHashCodeTester.getHashCodeMethods())
            .named(ImmutableSortedSetTest.class.getName() + ", unhashable")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        NavigableSetTestSuiteBuilder.using(new ImmutableSortedSetDescendingGenerator())
            .named(ImmutableSortedSetTest.class.getName() + ", descending")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        ListTestSuiteBuilder.using(new ImmutableSortedSetAsListGenerator())
            .named("ImmutableSortedSet.asList")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.REJECTS_DUPLICATES_AT_CREATION,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        ListTestSuiteBuilder.using(new ImmutableSortedSetSubsetAsListGenerator())
            .named("ImmutableSortedSet.subSet.asList")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.REJECTS_DUPLICATES_AT_CREATION,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        ListTestSuiteBuilder.using(new ImmutableSortedSetDescendingAsListGenerator())
            .named("ImmutableSortedSet.descendingSet.asList")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.REJECTS_DUPLICATES_AT_CREATION,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTestSuite(ImmutableSortedSetTest.class);

    return suite;
  }

  // enum singleton pattern
  private enum StringLengthComparator implements Comparator<String> {
    INSTANCE;

    @Override
    public int compare(String a, String b) {
      return a.length() - b.length();
    }
  }

  private static final Comparator<String> LENGTH_COMPARATOR = StringLengthComparator.INSTANCE;

  private static final Comparator<Integer> COMPLEMENT_COMPARATOR =
      (a, b) -> Integer.compare(~a, ~b);

  @Override
  protected <E extends Comparable<? super E>> SortedSet<E> of() {
    return ImmutableSortedSet.of();
  }

  @Override
  protected <E extends Comparable<? super E>> SortedSet<E> of(E e) {
    return ImmutableSortedSet.of(e);
  }

  @Override
  protected <E extends Comparable<? super E>> SortedSet<E> of(E e1, E e2) {
    return ImmutableSortedSet.of(e1, e2);
  }

  @Override
  protected <E extends Comparable<? super E>> SortedSet<E> of(E e1, E e2, E e3) {
    return ImmutableSortedSet.of(e1, e2, e3);
  }

  @Override
  protected <E extends Comparable<? super E>> SortedSet<E> of(E e1, E e2, E e3, E e4) {
    return ImmutableSortedSet.of(e1, e2, e3, e4);
  }

  @Override
  protected <E extends Comparable<? super E>> SortedSet<E> of(E e1, E e2, E e3, E e4, E e5) {
    return ImmutableSortedSet.of(e1, e2, e3, e4, e5);
  }

  @Override
  protected <E extends Comparable<? super E>> SortedSet<E> of(
      E e1, E e2, E e3, E e4, E e5, E e6, E... rest) {
    return ImmutableSortedSet.of(e1, e2, e3, e4, e5, e6, rest);
  }

  @Override
  protected <E extends Comparable<? super E>> SortedSet<E> copyOf(E[] elements) {
    return ImmutableSortedSet.copyOf(elements);
  }

  @Override
  protected <E extends Comparable<? super E>> SortedSet<E> copyOf(
      Collection<? extends E> elements) {
    return ImmutableSortedSet.copyOf(elements);
  }

  @Override
  protected <E extends Comparable<? super E>> SortedSet<E> copyOf(Iterable<? extends E> elements) {
    return ImmutableSortedSet.copyOf(elements);
  }

  @Override
  protected <E extends Comparable<? super E>> SortedSet<E> copyOf(Iterator<? extends E> elements) {
    return ImmutableSortedSet.copyOf(elements);
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testNullPointers() {
    new NullPointerTester().testAllPublicStaticMethods(ImmutableSortedSet.class);
  }

  public void testEmpty_comparator() {
    SortedSet<String> set = of();
    assertThat(set.comparator()).isEqualTo(Ordering.natural());
  }

  public void testEmpty_headSet() {
    SortedSet<String> set = of();
    assertThat(set.headSet("c")).isSameInstanceAs(set);
  }

  public void testEmpty_tailSet() {
    SortedSet<String> set = of();
    assertThat(set.tailSet("f")).isSameInstanceAs(set);
  }

  public void testEmpty_subSet() {
    SortedSet<String> set = of();
    assertThat(set.subSet("c", "f")).isSameInstanceAs(set);
  }

  public void testEmpty_first() {
    SortedSet<String> set = of();
    assertThrows(NoSuchElementException.class, () -> set.first());
  }

  public void testEmpty_last() {
    SortedSet<String> set = of();
    assertThrows(NoSuchElementException.class, () -> set.last());
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testEmpty_serialization() {
    SortedSet<String> set = of();
    SortedSet<String> copy = reserialize(set);
    assertThat(copy).isSameInstanceAs(set);
  }

  public void testSingle_comparator() {
    SortedSet<String> set = of("e");
    assertThat(set.comparator()).isEqualTo(Ordering.natural());
  }

  public void testSingle_headSet() {
    SortedSet<String> set = of("e");
    assertTrue(set.headSet("g") instanceof ImmutableSortedSet);
    assertThat(set.headSet("g")).contains("e");
    assertThat(set.headSet("c")).isSameInstanceAs(this.<String>of());
    assertThat(set.headSet("e")).isSameInstanceAs(this.<String>of());
  }

  public void testSingle_tailSet() {
    SortedSet<String> set = of("e");
    assertTrue(set.tailSet("c") instanceof ImmutableSortedSet);
    assertThat(set.tailSet("c")).contains("e");
    assertThat(set.tailSet("e")).contains("e");
    assertThat(set.tailSet("g")).isSameInstanceAs(this.<String>of());
  }

  public void testSingle_subSet() {
    SortedSet<String> set = of("e");
    assertTrue(set.subSet("c", "g") instanceof ImmutableSortedSet);
    assertThat(set.subSet("c", "g")).contains("e");
    assertThat(set.subSet("e", "g")).contains("e");
    assertThat(set.subSet("f", "g")).isSameInstanceAs(this.<String>of());
    assertThat(set.subSet("c", "e")).isSameInstanceAs(this.<String>of());
    assertThat(set.subSet("c", "d")).isSameInstanceAs(this.<String>of());
  }

  public void testSingle_first() {
    SortedSet<String> set = of("e");
    assertThat(set.first()).isEqualTo("e");
  }

  public void testSingle_last() {
    SortedSet<String> set = of("e");
    assertThat(set.last()).isEqualTo("e");
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testSingle_serialization() {
    SortedSet<String> set = of("e");
    SortedSet<String> copy = reserializeAndAssert(set);
    assertEquals(set.comparator(), copy.comparator());
  }

  public void testOf_ordering() {
    SortedSet<String> set = of("e", "a", "f", "b", "d", "c");
    assertThat(set).containsExactly("a", "b", "c", "d", "e", "f").inOrder();
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
    Object[] unused1 = set.toArray();
    Object[] unused2 = set.toArray(new Object[2]);
  }

  interface Interface extends Comparable<Interface> {}

  static class Impl implements Interface {
    static final AtomicInteger nextId = new AtomicInteger();

    int id = nextId.getAndIncrement();

    @Override
    public int compareTo(Interface other) {
      return Integer.compare(id, ((Impl) other).id);
    }
  }

  public void testOf_ordering_dupes() {
    SortedSet<String> set = of("e", "a", "e", "f", "b", "b", "d", "a", "c");
    assertThat(set).containsExactly("a", "b", "c", "d", "e", "f").inOrder();
  }

  public void testOf_comparator() {
    SortedSet<String> set = of("e", "a", "f", "b", "d", "c");
    assertThat(set.comparator()).isEqualTo(Ordering.natural());
  }

  public void testOf_headSet() {
    SortedSet<String> set = of("e", "f", "b", "d", "c");
    assertTrue(set.headSet("e") instanceof ImmutableSortedSet);
    assertThat(set.headSet("e")).containsExactly("b", "c", "d").inOrder();
    assertThat(set.headSet("g")).containsExactly("b", "c", "d", "e", "f").inOrder();
    assertThat(set.headSet("a")).isSameInstanceAs(this.<String>of());
    assertThat(set.headSet("b")).isSameInstanceAs(this.<String>of());
  }

  public void testOf_tailSet() {
    SortedSet<String> set = of("e", "f", "b", "d", "c");
    assertTrue(set.tailSet("e") instanceof ImmutableSortedSet);
    assertThat(set.tailSet("e")).containsExactly("e", "f").inOrder();
    assertThat(set.tailSet("a")).containsExactly("b", "c", "d", "e", "f").inOrder();
    assertThat(set.tailSet("g")).isSameInstanceAs(this.<String>of());
  }

  public void testOf_subSet() {
    SortedSet<String> set = of("e", "f", "b", "d", "c");
    assertTrue(set.subSet("c", "e") instanceof ImmutableSortedSet);
    assertThat(set.subSet("c", "e")).containsExactly("c", "d").inOrder();
    assertThat(set.subSet("a", "g")).containsExactly("b", "c", "d", "e", "f").inOrder();
    assertThat(set.subSet("a", "b")).isSameInstanceAs(this.<String>of());
    assertThat(set.subSet("g", "h")).isSameInstanceAs(this.<String>of());
    assertThat(set.subSet("c", "c")).isSameInstanceAs(this.<String>of());
    assertThrows(IllegalArgumentException.class, () -> set.subSet("e", "c"));
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testOf_subSetSerialization() {
    SortedSet<String> set = of("e", "f", "b", "d", "c");
    reserializeAndAssert(set.subSet("c", "e"));
  }

  public void testOf_first() {
    SortedSet<String> set = of("e", "f", "b", "d", "c");
    assertThat(set.first()).isEqualTo("b");
  }

  public void testOf_last() {
    SortedSet<String> set = of("e", "f", "b", "d", "c");
    assertThat(set.last()).isEqualTo("f");
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testOf_serialization() {
    SortedSet<String> set = of("e", "f", "b", "d", "c");
    SortedSet<String> copy = reserializeAndAssert(set);
    assertTrue(elementsEqual(set, copy));
    assertEquals(set.comparator(), copy.comparator());
  }

  /* "Explicit" indicates an explicit comparator. */

  public void testExplicit_ordering() {
    SortedSet<String> set =
        ImmutableSortedSet.orderedBy(LENGTH_COMPARATOR)
            .add("in", "the", "quick", "jumped", "over", "a")
            .build();
    assertThat(set).containsExactly("a", "in", "the", "over", "quick", "jumped").inOrder();
  }

  public void testExplicit_ordering_dupes() {
    SortedSet<String> set =
        ImmutableSortedSet.orderedBy(LENGTH_COMPARATOR)
            .add("in", "the", "quick", "brown", "fox", "jumped", "over", "a", "lazy", "dog")
            .build();
    assertThat(set).containsExactly("a", "in", "the", "over", "quick", "jumped").inOrder();
  }

  public void testExplicit_contains() {
    SortedSet<String> set =
        ImmutableSortedSet.orderedBy(LENGTH_COMPARATOR)
            .add("in", "the", "quick", "jumped", "over", "a")
            .build();
    assertTrue(set.contains("quick"));
    assertTrue(set.contains("google"));
    assertFalse(set.contains(""));
    assertFalse(set.contains("california"));
    assertFalse(set.contains(null));
  }

  @SuppressWarnings("CollectionIncompatibleType") // testing incompatible types
  public void testExplicit_containsMismatchedTypes() {
    SortedSet<String> set =
        ImmutableSortedSet.orderedBy(LENGTH_COMPARATOR)
            .add("in", "the", "quick", "jumped", "over", "a")
            .build();
    assertFalse(set.contains(3.7));
  }

  public void testExplicit_comparator() {
    SortedSet<String> set =
        ImmutableSortedSet.orderedBy(LENGTH_COMPARATOR)
            .add("in", "the", "quick", "jumped", "over", "a")
            .build();
    assertThat(set.comparator()).isEqualTo(LENGTH_COMPARATOR);
  }

  public void testExplicit_headSet() {
    SortedSet<String> set =
        ImmutableSortedSet.orderedBy(LENGTH_COMPARATOR)
            .add("in", "the", "quick", "jumped", "over", "a")
            .build();
    assertTrue(set.headSet("a") instanceof ImmutableSortedSet);
    assertTrue(set.headSet("fish") instanceof ImmutableSortedSet);
    assertThat(set.headSet("fish")).containsExactly("a", "in", "the").inOrder();
    assertThat(set.headSet("california"))
        .containsExactly("a", "in", "the", "over", "quick", "jumped")
        .inOrder();
    assertTrue(set.headSet("a").isEmpty());
    assertTrue(set.headSet("").isEmpty());
  }

  public void testExplicit_tailSet() {
    SortedSet<String> set =
        ImmutableSortedSet.orderedBy(LENGTH_COMPARATOR)
            .add("in", "the", "quick", "jumped", "over", "a")
            .build();
    assertTrue(set.tailSet("california") instanceof ImmutableSortedSet);
    assertTrue(set.tailSet("fish") instanceof ImmutableSortedSet);
    assertThat(set.tailSet("fish")).containsExactly("over", "quick", "jumped").inOrder();
    assertThat(set.tailSet("a"))
        .containsExactly("a", "in", "the", "over", "quick", "jumped")
        .inOrder();
    assertTrue(set.tailSet("california").isEmpty());
  }

  public void testExplicit_subSet() {
    SortedSet<String> set =
        ImmutableSortedSet.orderedBy(LENGTH_COMPARATOR)
            .add("in", "the", "quick", "jumped", "over", "a")
            .build();
    assertTrue(set.subSet("the", "quick") instanceof ImmutableSortedSet);
    assertTrue(set.subSet("", "b") instanceof ImmutableSortedSet);
    assertThat(set.subSet("the", "quick")).containsExactly("the", "over").inOrder();
    assertThat(set.subSet("a", "california"))
        .containsExactly("a", "in", "the", "over", "quick", "jumped")
        .inOrder();
    assertTrue(set.subSet("", "b").isEmpty());
    assertTrue(set.subSet("vermont", "california").isEmpty());
    assertTrue(set.subSet("aaa", "zzz").isEmpty());
    assertThrows(IllegalArgumentException.class, () -> set.subSet("quick", "the"));
  }

  public void testExplicit_first() {
    SortedSet<String> set =
        ImmutableSortedSet.orderedBy(LENGTH_COMPARATOR)
            .add("in", "the", "quick", "jumped", "over", "a")
            .build();
    assertThat(set.first()).isEqualTo("a");
  }

  public void testExplicit_last() {
    SortedSet<String> set =
        ImmutableSortedSet.orderedBy(LENGTH_COMPARATOR)
            .add("in", "the", "quick", "jumped", "over", "a")
            .build();
    assertThat(set.last()).isEqualTo("jumped");
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testExplicitEmpty_serialization() {
    SortedSet<String> set = ImmutableSortedSet.orderedBy(LENGTH_COMPARATOR).build();
    SortedSet<String> copy = reserializeAndAssert(set);
    assertTrue(set.isEmpty());
    assertTrue(copy.isEmpty());
    assertThat(copy.comparator()).isEqualTo(set.comparator());
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testExplicit_serialization() {
    SortedSet<String> set =
        ImmutableSortedSet.orderedBy(LENGTH_COMPARATOR)
            .add("in", "the", "quick", "jumped", "over", "a")
            .build();
    SortedSet<String> copy = reserializeAndAssert(set);
    assertTrue(elementsEqual(set, copy));
    assertThat(copy.comparator()).isEqualTo(set.comparator());
  }

  public void testCopyOf_ordering() {
    SortedSet<String> set = copyOf(asList("e", "a", "f", "b", "d", "c"));
    assertThat(set).containsExactly("a", "b", "c", "d", "e", "f").inOrder();
  }

  public void testCopyOf_ordering_dupes() {
    SortedSet<String> set = copyOf(asList("e", "a", "e", "f", "b", "b", "d", "a", "c"));
    assertThat(set).containsExactly("a", "b", "c", "d", "e", "f").inOrder();
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
    assertThat(set.comparator()).isEqualTo(Ordering.natural());
  }

  public void testCopyOf_iterator_ordering() {
    SortedSet<String> set = copyOf(asIterator("e", "a", "f", "b", "d", "c"));
    assertThat(set).containsExactly("a", "b", "c", "d", "e", "f").inOrder();
  }

  public void testCopyOf_iterator_ordering_dupes() {
    SortedSet<String> set = copyOf(asIterator("e", "a", "e", "f", "b", "b", "d", "a", "c"));
    assertThat(set).containsExactly("a", "b", "c", "d", "e", "f").inOrder();
  }

  public void testCopyOf_iterator_comparator() {
    SortedSet<String> set = copyOf(asIterator("e", "a", "f", "b", "d", "c"));
    assertThat(set.comparator()).isEqualTo(Ordering.natural());
  }

  public void testCopyOf_sortedSet_ordering() {
    SortedSet<String> set = copyOf(new TreeSet<>(asList("e", "a", "f", "b", "d", "c")));
    assertThat(set).containsExactly("a", "b", "c", "d", "e", "f").inOrder();
  }

  public void testCopyOf_sortedSet_comparator() {
    SortedSet<String> set = copyOf(new TreeSet<>());
    assertThat(set.comparator()).isEqualTo(Ordering.natural());
  }

  public void testCopyOfExplicit_ordering() {
    SortedSet<String> set =
        ImmutableSortedSet.copyOf(
            LENGTH_COMPARATOR, asList("in", "the", "quick", "jumped", "over", "a"));
    assertThat(set).containsExactly("a", "in", "the", "over", "quick", "jumped").inOrder();
  }

  public void testCopyOfExplicit_ordering_dupes() {
    SortedSet<String> set =
        ImmutableSortedSet.copyOf(
            LENGTH_COMPARATOR,
            asList("in", "the", "quick", "brown", "fox", "jumped", "over", "a", "lazy", "dog"));
    assertThat(set).containsExactly("a", "in", "the", "over", "quick", "jumped").inOrder();
  }

  public void testCopyOfExplicit_comparator() {
    SortedSet<String> set =
        ImmutableSortedSet.copyOf(
            LENGTH_COMPARATOR, asList("in", "the", "quick", "jumped", "over", "a"));
    assertThat(set.comparator()).isEqualTo(LENGTH_COMPARATOR);
  }

  public void testCopyOfExplicit_iterator_ordering() {
    SortedSet<String> set =
        ImmutableSortedSet.copyOf(
            LENGTH_COMPARATOR, asIterator("in", "the", "quick", "jumped", "over", "a"));
    assertThat(set).containsExactly("a", "in", "the", "over", "quick", "jumped").inOrder();
  }

  public void testCopyOfExplicit_iterator_ordering_dupes() {
    SortedSet<String> set =
        ImmutableSortedSet.copyOf(
            LENGTH_COMPARATOR,
            asIterator("in", "the", "quick", "brown", "fox", "jumped", "over", "a", "lazy", "dog"));
    assertThat(set).containsExactly("a", "in", "the", "over", "quick", "jumped").inOrder();
  }

  public void testCopyOfExplicit_iterator_comparator() {
    SortedSet<String> set =
        ImmutableSortedSet.copyOf(
            LENGTH_COMPARATOR, asIterator("in", "the", "quick", "jumped", "over", "a"));
    assertThat(set.comparator()).isEqualTo(LENGTH_COMPARATOR);
  }

  public void testCopyOf_sortedSetIterable() {
    SortedSet<String> input = new TreeSet<>(LENGTH_COMPARATOR);
    Collections.addAll(input, "in", "the", "quick", "jumped", "over", "a");
    SortedSet<String> set = copyOf(input);
    assertThat(set).containsExactly("a", "in", "jumped", "over", "quick", "the").inOrder();
  }

  public void testCopyOfSorted_natural_ordering() {
    SortedSet<String> input = new TreeSet<>(asList("in", "the", "quick", "jumped", "over", "a"));
    SortedSet<String> set = ImmutableSortedSet.copyOfSorted(input);
    assertThat(set).containsExactly("a", "in", "jumped", "over", "quick", "the").inOrder();
  }

  public void testCopyOfSorted_natural_comparator() {
    SortedSet<String> input = new TreeSet<>(asList("in", "the", "quick", "jumped", "over", "a"));
    SortedSet<String> set = ImmutableSortedSet.copyOfSorted(input);
    assertThat(set.comparator()).isEqualTo(Ordering.natural());
  }

  public void testCopyOfSorted_explicit_ordering() {
    SortedSet<String> input = new TreeSet<>(LENGTH_COMPARATOR);
    Collections.addAll(input, "in", "the", "quick", "jumped", "over", "a");
    SortedSet<String> set = ImmutableSortedSet.copyOfSorted(input);
    assertThat(set).containsExactly("a", "in", "the", "over", "quick", "jumped").inOrder();
    assertThat(set.comparator()).isEqualTo(LENGTH_COMPARATOR);
  }

  // TODO(b/172823566): Use mainline testToImmutableSortedSet once CollectorTester is usable.
  public void testToImmutableSortedSet_java7() {
    // Note that a Collector should generally enforce consistent comparator between builders
    ImmutableSortedSet.Builder<String> zis =
        ImmutableSortedSet.<String>naturalOrder().add("c", "b", "c");
    ImmutableSortedSet.Builder<String> zat =
        ImmutableSortedSet.<String>naturalOrder().add("a", "b", "d", "c");
    ImmutableSortedSet<String> sortedSet = zis.combine(zat).build();
    assertThat(sortedSet).containsExactly("a", "b", "c", "d").inOrder();
  }

  // TODO(b/172823566): Use mainline testToImmutableSortedSet_customComparator once CollectorTester
  //  is usable to java7.
  public void testToImmutableSortedSet_customComparator_java7() {
    // Note that a Collector should generally enforce consistent comparator between builders.
    // So no tests for non-matching comparator shenanigans.
    ImmutableSortedSet.Builder<String> zis =
        ImmutableSortedSet.<String>orderedBy(LENGTH_COMPARATOR).add("ccc", "bb", "ccc");
    ImmutableSortedSet.Builder<String> zat =
        ImmutableSortedSet.<String>orderedBy(LENGTH_COMPARATOR).add("a", "bb", "dddd", "ccc");
    ImmutableSortedSet<String> sortedSet = zis.combine(zat).build();
    assertThat(sortedSet).containsExactly("a", "bb", "ccc", "dddd").inOrder();
  }

  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(
            of("in", "the", "a"),
            newHashSet("in", "the", "a"),
            new TreeSet<>(asList("in", "the", "a")),
            newTreeSetWithComparator(LENGTH_COMPARATOR, "in", "the", "a"))
        .addEqualityGroup(
            newHashSet("in", "the", "house"), new TreeSet<>(asList("in", "the", "house")))
        .testEquals();
  }

  @SuppressWarnings("EqualsIncompatibleType") // test of a bogus call
  public void testEquals_bothNaturalComparator_stringVsInt() {
    SortedSet<String> set = of("a", "b", "c");
    SortedSet<Integer> other = new TreeSet<>(asList(4, 5, 6));
    assertThat(set.equals(other)).isFalse();
  }

  @SuppressWarnings("EqualsIncompatibleType") // test of a bogus call
  public void testEquals_otherUsesAlternativeComparator_stringVsInt() {
    SortedSet<String> set = of("in", "the", "a");
    SortedSet<Integer> other = newTreeSetWithComparator(COMPLEMENT_COMPARATOR, 4, 5, 6);
    assertThat(set.equals(other)).isFalse();
  }

  @SuppressWarnings("EqualsIncompatibleType") // test of a bogus call
  public void testEquals_otherNotSorted_stringVsInt() {
    SortedSet<String> set = of("in", "the", "a");
    Set<Integer> other = newHashSet(4, 5, 6);
    assertThat(set.equals(other)).isFalse();
  }

  public void testContainsAll_notSortedSet() {
    SortedSet<String> set = of("a", "b", "f");
    assertTrue(set.containsAll(emptyList()));
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
    assertTrue(set.containsAll(new TreeSet<>()));
    assertTrue(set.containsAll(new TreeSet<>(asList("b"))));
    assertTrue(set.containsAll(new TreeSet<>(asList("a", "f"))));
    assertTrue(set.containsAll(new TreeSet<>(asList("a", "b", "f"))));
    assertFalse(set.containsAll(new TreeSet<>(asList("d"))));
    assertFalse(set.containsAll(new TreeSet<>(asList("z"))));
    assertFalse(set.containsAll(new TreeSet<>(asList("b", "d"))));
    assertFalse(set.containsAll(new TreeSet<>(asList("f", "d", "a"))));
  }

  @SuppressWarnings("CollectionIncompatibleType") // testing incompatible types
  public void testContainsAll_sameComparator_stringVsInt() {
    SortedSet<String> set = of("a", "b", "f");
    SortedSet<Integer> unexpected = new TreeSet<>(Ordering.natural());
    unexpected.addAll(asList(1, 2, 3));
    assertFalse(set.containsAll(unexpected));
  }

  public void testContainsAll_differentComparator() {
    Comparator<Comparable<?>> comparator = Collections.reverseOrder();
    SortedSet<String> set =
        new ImmutableSortedSet.Builder<String>(comparator).add("a", "b", "f").build();
    assertTrue(set.containsAll(new TreeSet<>()));
    assertTrue(set.containsAll(new TreeSet<>(asList("b"))));
    assertTrue(set.containsAll(new TreeSet<>(asList("a", "f"))));
    assertTrue(set.containsAll(new TreeSet<>(asList("a", "b", "f"))));
    assertFalse(set.containsAll(new TreeSet<>(asList("d"))));
    assertFalse(set.containsAll(new TreeSet<>(asList("z"))));
    assertFalse(set.containsAll(new TreeSet<>(asList("b", "d"))));
    assertFalse(set.containsAll(new TreeSet<>(asList("f", "d", "a"))));
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testDifferentComparator_serialization() {
    // don't use Collections.reverseOrder(); it didn't reserialize to the same instance in JDK5
    Comparator<Comparable<?>> comparator = Ordering.natural().reverse();
    SortedSet<String> set =
        new ImmutableSortedSet.Builder<String>(comparator).add("a", "b", "c").build();
    SortedSet<String> copy = reserializeAndAssert(set);
    assertTrue(elementsEqual(set, copy));
    assertEquals(set.comparator(), copy.comparator());
  }

  public void testReverseOrder() {
    SortedSet<String> set = ImmutableSortedSet.<String>reverseOrder().add("a", "b", "c").build();
    assertThat(set).containsExactly("c", "b", "a").inOrder();
    assertTrue(isInOrder(asList("c", "b", "a"), set.comparator()));
  }

  private static final Comparator<Object> TO_STRING =
      new Comparator<Object>() {
        @Override
        public int compare(Object o1, Object o2) {
          return o1.toString().compareTo(o2.toString());
        }
      };

  public void testSupertypeComparator() {
    SortedSet<Integer> set =
        new ImmutableSortedSet.Builder<Integer>(TO_STRING).add(3, 12, 101, 44).build();
    assertThat(set).containsExactly(101, 12, 3, 44).inOrder();
  }

  public void testSupertypeComparatorSubtypeElements() {
    SortedSet<Number> set =
        new ImmutableSortedSet.Builder<Number>(TO_STRING).add(3, 12, 101, 44).build();
    assertThat(set).containsExactly(101, 12, 3, 44).inOrder();
  }

  @Override
  <E extends Comparable<E>> ImmutableSortedSet.Builder<E> builder() {
    return ImmutableSortedSet.naturalOrder();
  }

  @Override
  int getComplexBuilderSetLastElement() {
    return 0x00FFFFFF;
  }

  public void testLegacyComparable_of() {
    ImmutableSortedSet<LegacyComparable> set0 = ImmutableSortedSet.of();
    assertThat(set0).isEmpty();

    @SuppressWarnings("unchecked") // using a legacy comparable
    ImmutableSortedSet<LegacyComparable> set1 = ImmutableSortedSet.of(LegacyComparable.Z);
    assertThat(set1).containsExactly(LegacyComparable.Z);

    @SuppressWarnings("unchecked") // using a legacy comparable
    ImmutableSortedSet<LegacyComparable> set2 =
        ImmutableSortedSet.of(LegacyComparable.Z, LegacyComparable.Y);
    assertThat(set2).containsExactly(LegacyComparable.Y, LegacyComparable.Z);
  }

  public void testLegacyComparable_copyOf_collection() {
    ImmutableSortedSet<LegacyComparable> set =
        ImmutableSortedSet.copyOf(LegacyComparable.VALUES_BACKWARD);
    assertTrue(elementsEqual(LegacyComparable.VALUES_FORWARD, set));
  }

  public void testLegacyComparable_copyOf_iterator() {
    ImmutableSortedSet<LegacyComparable> set =
        ImmutableSortedSet.copyOf(LegacyComparable.VALUES_BACKWARD.iterator());
    assertTrue(elementsEqual(LegacyComparable.VALUES_FORWARD, set));
  }

  public void testLegacyComparable_builder_natural() {
    // Note: IntelliJ wrongly reports an error for this statement
    ImmutableSortedSet.Builder<LegacyComparable> builder = ImmutableSortedSet.naturalOrder();

    builder.addAll(LegacyComparable.VALUES_BACKWARD);
    builder.add(LegacyComparable.X);
    builder.add(LegacyComparable.Y, LegacyComparable.Z);

    ImmutableSortedSet<LegacyComparable> set = builder.build();
    assertTrue(elementsEqual(LegacyComparable.VALUES_FORWARD, set));
  }

  public void testLegacyComparable_builder_reverse() {
    // Note: IntelliJ wrongly reports an error for this statement
    ImmutableSortedSet.Builder<LegacyComparable> builder = ImmutableSortedSet.reverseOrder();

    builder.addAll(LegacyComparable.VALUES_FORWARD);
    builder.add(LegacyComparable.X);
    builder.add(LegacyComparable.Y, LegacyComparable.Z);

    ImmutableSortedSet<LegacyComparable> set = builder.build();
    assertTrue(elementsEqual(LegacyComparable.VALUES_BACKWARD, set));
  }

  @SuppressWarnings({"deprecation", "static-access", "DoNotCall"}) // test of a bogus call
  public void testBuilderMethod() {
    assertThrows(UnsupportedOperationException.class, () -> ImmutableSortedSet.builder());
  }

  public void testAsList() {
    ImmutableSet<String> set = ImmutableSortedSet.of("a", "e", "i", "o", "u");
    ImmutableList<String> list = set.asList();
    assertEquals(ImmutableList.of("a", "e", "i", "o", "u"), list);
    assertThat(ImmutableList.copyOf(set)).isSameInstanceAs(list);
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester, ImmutableSortedAsList
  public void testAsListReturnTypeAndSerialization() {
    ImmutableSet<String> set = ImmutableSortedSet.of("a", "e", "i", "o", "u");
    ImmutableList<String> list = set.asList();
    ImmutableList<String> copy = reserializeAndAssert(list);
    assertEquals(list, copy);
  }

  public void testSubsetAsList() {
    ImmutableSet<String> set = ImmutableSortedSet.of("a", "e", "i", "o", "u").subSet("c", "r");
    ImmutableList<String> list = set.asList();
    assertEquals(ImmutableList.of("e", "i", "o"), list);
    assertEquals(list, ImmutableList.copyOf(set));
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester, ImmutableSortedAsList
  public void testSubsetAsListReturnTypeAndSerialization() {
    ImmutableSet<String> set = ImmutableSortedSet.of("a", "e", "i", "o", "u").subSet("c", "r");
    ImmutableList<String> list = set.asList();
    ImmutableList<String> copy = reserialize(list);
    assertEquals(list, copy);
  }

  public void testAsListInconsistentComparator() {
    ImmutableSet<String> set =
        ImmutableSortedSet.orderedBy(LENGTH_COMPARATOR)
            .add("in", "the", "quick", "jumped", "over", "a")
            .build();
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

  public void testHeadSetInclusive() {
    String[] strings = NUMBER_NAMES.toArray(new String[0]);
    ImmutableSortedSet<String> set = ImmutableSortedSet.copyOf(strings);
    sort(strings);
    for (int i = 0; i < strings.length; i++) {
      assertThat(set.headSet(strings[i], true))
          .containsExactlyElementsIn(sortedNumberNames(0, i + 1))
          .inOrder();
    }
  }

  public void testHeadSetExclusive() {
    String[] strings = NUMBER_NAMES.toArray(new String[0]);
    ImmutableSortedSet<String> set = ImmutableSortedSet.copyOf(strings);
    sort(strings);
    for (int i = 0; i < strings.length; i++) {
      assertThat(set.headSet(strings[i], false))
          .containsExactlyElementsIn(sortedNumberNames(0, i))
          .inOrder();
    }
  }

  public void testTailSetInclusive() {
    String[] strings = NUMBER_NAMES.toArray(new String[0]);
    ImmutableSortedSet<String> set = ImmutableSortedSet.copyOf(strings);
    sort(strings);
    for (int i = 0; i < strings.length; i++) {
      assertThat(set.tailSet(strings[i], true))
          .containsExactlyElementsIn(sortedNumberNames(i, strings.length))
          .inOrder();
    }
  }

  public void testTailSetExclusive() {
    String[] strings = NUMBER_NAMES.toArray(new String[0]);
    ImmutableSortedSet<String> set = ImmutableSortedSet.copyOf(strings);
    sort(strings);
    for (int i = 0; i < strings.length; i++) {
      assertThat(set.tailSet(strings[i], false))
          .containsExactlyElementsIn(sortedNumberNames(i + 1, strings.length))
          .inOrder();
    }
  }

  public void testFloor_emptySet() {
    ImmutableSortedSet<String> set = ImmutableSortedSet.copyOf(new String[] {});
    assertThat(set.floor("f")).isNull();
  }

  public void testFloor_elementPresent() {
    ImmutableSortedSet<String> set =
        ImmutableSortedSet.copyOf(new String[] {"e", "a", "e", "f", "b", "i", "d", "a", "c", "k"});
    assertThat(set.floor("f")).isEqualTo("f");
    assertThat(set.floor("j")).isEqualTo("i");
    assertThat(set.floor("q")).isEqualTo("k");
  }

  public void testFloor_elementAbsent() {
    ImmutableSortedSet<String> set =
        ImmutableSortedSet.copyOf(new String[] {"e", "e", "f", "b", "i", "d", "c", "k"});
    assertThat(set.floor("a")).isNull();
  }

  public void testCeiling_emptySet() {
    ImmutableSortedSet<String> set = ImmutableSortedSet.copyOf(new String[] {});
    assertThat(set.ceiling("f")).isNull();
  }

  public void testCeiling_elementPresent() {
    ImmutableSortedSet<String> set =
        ImmutableSortedSet.copyOf(new String[] {"e", "e", "f", "f", "i", "d", "c", "k", "p", "c"});
    assertThat(set.ceiling("f")).isEqualTo("f");
    assertThat(set.ceiling("h")).isEqualTo("i");
    assertThat(set.ceiling("a")).isEqualTo("c");
  }

  public void testCeiling_elementAbsent() {
    ImmutableSortedSet<String> set =
        ImmutableSortedSet.copyOf(new String[] {"e", "a", "e", "f", "b", "i", "d", "a", "c", "k"});
    assertThat(set.ceiling("l")).isNull();
  }

  public void testSubSetExclusiveExclusive() {
    String[] strings = NUMBER_NAMES.toArray(new String[0]);
    ImmutableSortedSet<String> set = ImmutableSortedSet.copyOf(strings);
    sort(strings);
    for (int i = 0; i < strings.length; i++) {
      for (int j = i; j < strings.length; j++) {
        assertThat(set.subSet(strings[i], false, strings[j], false))
            .containsExactlyElementsIn(sortedNumberNames(min(i + 1, j), j))
            .inOrder();
      }
    }
  }

  public void testSubSetInclusiveExclusive() {
    String[] strings = NUMBER_NAMES.toArray(new String[0]);
    ImmutableSortedSet<String> set = ImmutableSortedSet.copyOf(strings);
    sort(strings);
    for (int i = 0; i < strings.length; i++) {
      for (int j = i; j < strings.length; j++) {
        assertThat(set.subSet(strings[i], true, strings[j], false))
            .containsExactlyElementsIn(sortedNumberNames(i, j))
            .inOrder();
      }
    }
  }

  public void testSubSetExclusiveInclusive() {
    String[] strings = NUMBER_NAMES.toArray(new String[0]);
    ImmutableSortedSet<String> set = ImmutableSortedSet.copyOf(strings);
    sort(strings);
    for (int i = 0; i < strings.length; i++) {
      for (int j = i; j < strings.length; j++) {
        assertThat(set.subSet(strings[i], false, strings[j], true))
            .containsExactlyElementsIn(sortedNumberNames(i + 1, j + 1))
            .inOrder();
      }
    }
  }

  public void testSubSetInclusiveInclusive() {
    String[] strings = NUMBER_NAMES.toArray(new String[0]);
    ImmutableSortedSet<String> set = ImmutableSortedSet.copyOf(strings);
    sort(strings);
    for (int i = 0; i < strings.length; i++) {
      for (int j = i; j < strings.length; j++) {
        assertThat(set.subSet(strings[i], true, strings[j], true))
            .containsExactlyElementsIn(sortedNumberNames(i, j + 1))
            .inOrder();
      }
    }
  }

  private static ImmutableList<String> sortedNumberNames(int i, int j) {
    return ImmutableList.copyOf(SORTED_NUMBER_NAMES.subList(i, j));
  }

  private static final ImmutableList<String> NUMBER_NAMES =
      ImmutableList.of("one", "two", "three", "four", "five", "six", "seven");

  private static final ImmutableList<String> SORTED_NUMBER_NAMES =
      Ordering.<String>natural().immutableSortedCopy(NUMBER_NAMES);

  private static class SelfComparableExample implements Comparable<SelfComparableExample> {
    @Override
    public int compareTo(SelfComparableExample o) {
      return 0;
    }
  }

  public void testBuilderGenerics_selfComparable() {
    // testing simple creation
    ImmutableSortedSet.Builder<SelfComparableExample> natural = ImmutableSortedSet.naturalOrder();
    assertThat(natural).isNotNull();
    ImmutableSortedSet.Builder<SelfComparableExample> reverse = ImmutableSortedSet.reverseOrder();
    assertThat(reverse).isNotNull();
  }

  private static class SuperComparableExample extends SelfComparableExample {}

  public void testBuilderGenerics_superComparable() {
    // testing simple creation
    ImmutableSortedSet.Builder<SuperComparableExample> natural = ImmutableSortedSet.naturalOrder();
    assertThat(natural).isNotNull();
    ImmutableSortedSet.Builder<SuperComparableExample> reverse = ImmutableSortedSet.reverseOrder();
    assertThat(reverse).isNotNull();
  }

  @GwtIncompatible // internals
  public void testControlsArraySize() {
    ImmutableSet.Builder<String> builder = ImmutableSortedSet.naturalOrder();
    for (int i = 0; i < 10; i++) {
      builder.add("foo");
    }
    builder.add("bar");
    RegularImmutableSortedSet<String> set = (RegularImmutableSortedSet<String>) builder.build();
    RegularImmutableList<String> list = (RegularImmutableList<String>) set.elements;
    assertTrue(list.array.length <= 2 * set.size());
  }

  @GwtIncompatible // internals
  public void testReusedBuilder() {
    ImmutableSortedSet.Builder<String> builder = ImmutableSortedSet.naturalOrder();
    for (int i = 0; i < 10; i++) {
      builder.add("foo");
    }
    builder.add("bar");
    RegularImmutableSortedSet<String> set = (RegularImmutableSortedSet<String>) builder.build();
    RegularImmutableList<String> list = (RegularImmutableList<String>) set.elements;
    builder.add("baz");
    assertTrue(list.array != builder.contents);
  }

  public void testBuilderAsymptotics() {
    int[] compares = {0};
    Comparator<Integer> countingComparator =
        (i, j) -> {
          compares[0]++;
          return i.compareTo(j);
        };
    ImmutableSortedSet.Builder<Integer> builder =
        new ImmutableSortedSet.Builder<Integer>(countingComparator, 10);
    for (int i = 0; i < 9; i++) {
      builder.add(i);
    }
    for (int j = 0; j < 1000; j++) {
      builder.add(9);
    }
    ImmutableSortedSet<Integer> unused = builder.build();
    assertThat(compares[0]).isAtMost(10000);
    // hopefully something quadratic would have more digits
  }

  private static <E extends @Nullable Object> SortedSet<E> newTreeSetWithComparator(
      Comparator<? super E> comparator, E... elements) {
    SortedSet<E> set = new TreeSet<>(comparator);
    Collections.addAll(set, elements);
    return set;
  }
}

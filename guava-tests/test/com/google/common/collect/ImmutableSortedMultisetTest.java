/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterators.emptyIterator;
import static com.google.common.collect.Iterators.singletonIterator;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.testing.ListTestSuiteBuilder;
import com.google.common.collect.testing.MinimalCollection;
import com.google.common.collect.testing.TestStringListGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.google.SortedMultisetTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringMultisetGenerator;
import com.google.common.collect.testing.google.UnmodifiableCollectionTests;
import com.google.common.testing.CollectorTester;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests for {@link ImmutableSortedMultiset}.
 *
 * @author Louis Wasserman
 */
public class ImmutableSortedMultisetTest extends TestCase {
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(ImmutableSortedMultisetTest.class);

    suite.addTest(
        SortedMultisetTestSuiteBuilder.using(
                new TestStringMultisetGenerator() {
                  @Override
                  protected Multiset<String> create(String[] elements) {
                    return ImmutableSortedMultiset.copyOf(elements);
                  }

                  @Override
                  public List<String> order(List<String> insertionOrder) {
                    return Ordering.natural().sortedCopy(insertionOrder);
                  }
                })
            .named("ImmutableSortedMultiset")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.SERIALIZABLE_INCLUDING_VIEWS,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        ListTestSuiteBuilder.using(
                new TestStringListGenerator() {
                  @Override
                  protected List<String> create(String[] elements) {
                    return ImmutableSortedMultiset.copyOf(elements).asList();
                  }

                  @Override
                  public List<String> order(List<String> insertionOrder) {
                    return Ordering.natural().sortedCopy(insertionOrder);
                  }
                })
            .named("ImmutableSortedMultiset.asList")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        ListTestSuiteBuilder.using(
                new TestStringListGenerator() {
                  @Override
                  protected List<String> create(String[] elements) {
                    Set<String> set = newHashSet();
                    ImmutableSortedMultiset.Builder<String> builder =
                        ImmutableSortedMultiset.naturalOrder();
                    for (String s : elements) {
                      checkArgument(set.add(s));
                      builder.addCopies(s, 2);
                    }
                    return builder.build().elementSet().asList();
                  }

                  @Override
                  public List<String> order(List<String> insertionOrder) {
                    return Ordering.natural().sortedCopy(insertionOrder);
                  }
                })
            .named("ImmutableSortedMultiset.elementSet.asList")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.REJECTS_DUPLICATES_AT_CREATION,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    return suite;
  }

  public void testCreation_noArgs() {
    Multiset<String> multiset = ImmutableSortedMultiset.of();
    assertTrue(multiset.isEmpty());
  }

  public void testCreation_oneElement() {
    Multiset<String> multiset = ImmutableSortedMultiset.of("a");
    assertEquals(HashMultiset.create(asList("a")), multiset);
  }

  public void testCreation_twoElements() {
    Multiset<String> multiset = ImmutableSortedMultiset.of("a", "b");
    assertEquals(HashMultiset.create(asList("a", "b")), multiset);
  }

  public void testCreation_threeElements() {
    Multiset<String> multiset = ImmutableSortedMultiset.of("a", "b", "c");
    assertEquals(HashMultiset.create(asList("a", "b", "c")), multiset);
  }

  public void testCreation_fourElements() {
    Multiset<String> multiset = ImmutableSortedMultiset.of("a", "b", "c", "d");
    assertEquals(HashMultiset.create(asList("a", "b", "c", "d")), multiset);
  }

  public void testCreation_fiveElements() {
    Multiset<String> multiset = ImmutableSortedMultiset.of("a", "b", "c", "d", "e");
    assertEquals(HashMultiset.create(asList("a", "b", "c", "d", "e")), multiset);
  }

  public void testCreation_sixElements() {
    Multiset<String> multiset = ImmutableSortedMultiset.of("a", "b", "c", "d", "e", "f");
    assertEquals(HashMultiset.create(asList("a", "b", "c", "d", "e", "f")), multiset);
  }

  public void testCreation_sevenElements() {
    Multiset<String> multiset = ImmutableSortedMultiset.of("a", "b", "c", "d", "e", "f", "g");
    assertEquals(HashMultiset.create(asList("a", "b", "c", "d", "e", "f", "g")), multiset);
  }

  public void testCreation_emptyArray() {
    String[] array = new String[0];
    Multiset<String> multiset = ImmutableSortedMultiset.copyOf(array);
    assertTrue(multiset.isEmpty());
  }

  public void testCreation_arrayOfOneElement() {
    String[] array = new String[] {"a"};
    Multiset<String> multiset = ImmutableSortedMultiset.copyOf(array);
    assertEquals(HashMultiset.create(asList("a")), multiset);
  }

  public void testCreation_arrayOfArray() {
    Comparator<String[]> comparator =
        Ordering.natural().lexicographical().onResultOf(Arrays::asList);
    String[] array = new String[] {"a"};
    Multiset<String[]> multiset = ImmutableSortedMultiset.orderedBy(comparator).add(array).build();
    Multiset<String[]> expected = HashMultiset.create();
    expected.add(array);
    assertEquals(expected, multiset);
  }

  public void testCreation_arrayContainingOnlyNull() {
    String[] array = new String[] {null};
    assertThrows(NullPointerException.class, () -> ImmutableSortedMultiset.copyOf(array));
  }

  public void testCopyOf_collection_empty() {
    // "<String>" is required to work around a javac 1.5 bug.
    Collection<String> c = MinimalCollection.<String>of();
    Multiset<String> multiset = ImmutableSortedMultiset.copyOf(c);
    assertTrue(multiset.isEmpty());
  }

  public void testCopyOf_collection_oneElement() {
    Collection<String> c = MinimalCollection.of("a");
    Multiset<String> multiset = ImmutableSortedMultiset.copyOf(c);
    assertEquals(HashMultiset.create(asList("a")), multiset);
  }

  public void testCopyOf_collection_general() {
    Collection<String> c = MinimalCollection.of("a", "b", "a");
    Multiset<String> multiset = ImmutableSortedMultiset.copyOf(c);
    assertEquals(HashMultiset.create(asList("a", "b", "a")), multiset);
  }

  public void testCopyOf_collectionContainingNull() {
    Collection<String> c = MinimalCollection.of("a", null, "b");
    assertThrows(NullPointerException.class, () -> ImmutableSortedMultiset.copyOf(c));
  }

  public void testCopyOf_multiset_empty() {
    Multiset<String> c = HashMultiset.create();
    Multiset<String> multiset = ImmutableSortedMultiset.copyOf(c);
    assertTrue(multiset.isEmpty());
  }

  public void testCopyOf_multiset_oneElement() {
    Multiset<String> c = HashMultiset.create(asList("a"));
    Multiset<String> multiset = ImmutableSortedMultiset.copyOf(c);
    assertEquals(HashMultiset.create(asList("a")), multiset);
  }

  public void testCopyOf_multiset_general() {
    Multiset<String> c = HashMultiset.create(asList("a", "b", "a"));
    Multiset<String> multiset = ImmutableSortedMultiset.copyOf(c);
    assertEquals(HashMultiset.create(asList("a", "b", "a")), multiset);
  }

  public void testCopyOf_multisetContainingNull() {
    Multiset<String> c = HashMultiset.create(asList("a", null, "b"));
    assertThrows(NullPointerException.class, () -> ImmutableSortedMultiset.copyOf(c));
  }

  public void testCopyOf_iterator_empty() {
    Iterator<String> iterator = emptyIterator();
    Multiset<String> multiset = ImmutableSortedMultiset.copyOf(iterator);
    assertTrue(multiset.isEmpty());
  }

  public void testCopyOf_iterator_oneElement() {
    Iterator<String> iterator = singletonIterator("a");
    Multiset<String> multiset = ImmutableSortedMultiset.copyOf(iterator);
    assertEquals(HashMultiset.create(asList("a")), multiset);
  }

  public void testCopyOf_iterator_general() {
    Iterator<String> iterator = asList("a", "b", "a").iterator();
    Multiset<String> multiset = ImmutableSortedMultiset.copyOf(iterator);
    assertEquals(HashMultiset.create(asList("a", "b", "a")), multiset);
  }

  public void testCopyOf_iteratorContainingNull() {
    Iterator<String> iterator = asList("a", null, "b").iterator();
    assertThrows(NullPointerException.class, () -> ImmutableSortedMultiset.copyOf(iterator));
  }

  private static class CountingIterable implements Iterable<String> {
    int count = 0;

    @Override
    public Iterator<String> iterator() {
      count++;
      return asList("a", "b", "a").iterator();
    }
  }

  public void testCopyOf_plainIterable() {
    CountingIterable iterable = new CountingIterable();
    Multiset<String> multiset = ImmutableSortedMultiset.copyOf(iterable);
    assertEquals(HashMultiset.create(asList("a", "b", "a")), multiset);
    assertEquals(1, iterable.count);
  }

  public void testCopyOf_shortcut_empty() {
    Collection<String> c = ImmutableSortedMultiset.of();
    assertSame(c, ImmutableSortedMultiset.copyOf(c));
  }

  public void testCopyOf_shortcut_singleton() {
    Collection<String> c = ImmutableSortedMultiset.of("a");
    assertSame(c, ImmutableSortedMultiset.copyOf(c));
  }

  public void testCopyOf_shortcut_immutableMultiset() {
    Collection<String> c = ImmutableSortedMultiset.of("a", "b", "c");
    assertSame(c, ImmutableSortedMultiset.copyOf(c));
  }

  public void testForEachEntry() {
    ImmutableSortedMultiset<String> multiset =
        ImmutableSortedMultiset.<String>naturalOrder().add("a").add("b").add("a").add("c").build();
    List<Multiset.Entry<String>> entries = new ArrayList<>();
    multiset.forEachEntry((e, c) -> entries.add(Multisets.immutableEntry(e, c)));
    assertThat(entries)
        .containsExactly(
            Multisets.immutableEntry("a", 2),
            Multisets.immutableEntry("b", 1),
            Multisets.immutableEntry("c", 1))
        .inOrder();
  }

  public void testBuilderAdd() {
    ImmutableSortedMultiset<String> multiset =
        ImmutableSortedMultiset.<String>naturalOrder().add("a").add("b").add("a").add("c").build();
    assertEquals(HashMultiset.create(asList("a", "b", "a", "c")), multiset);
  }

  public void testBuilderAddAll() {
    List<String> a = asList("a", "b");
    List<String> b = asList("c", "d");
    ImmutableSortedMultiset<String> multiset =
        ImmutableSortedMultiset.<String>naturalOrder().addAll(a).addAll(b).build();
    assertEquals(HashMultiset.create(asList("a", "b", "c", "d")), multiset);
  }

  public void testBuilderAddAllMultiset() {
    Multiset<String> a = HashMultiset.create(asList("a", "b", "b"));
    Multiset<String> b = HashMultiset.create(asList("c", "b"));
    ImmutableSortedMultiset<String> multiset =
        ImmutableSortedMultiset.<String>naturalOrder().addAll(a).addAll(b).build();
    assertEquals(HashMultiset.create(asList("a", "b", "b", "b", "c")), multiset);
  }

  public void testBuilderAddAllIterator() {
    Iterator<String> iterator = asList("a", "b", "a", "c").iterator();
    ImmutableSortedMultiset<String> multiset =
        ImmutableSortedMultiset.<String>naturalOrder().addAll(iterator).build();
    assertEquals(HashMultiset.create(asList("a", "b", "a", "c")), multiset);
  }

  public void testBuilderAddCopies() {
    ImmutableSortedMultiset<String> multiset =
        ImmutableSortedMultiset.<String>naturalOrder()
            .addCopies("a", 2)
            .addCopies("b", 3)
            .addCopies("c", 0)
            .build();
    assertEquals(HashMultiset.create(asList("a", "a", "b", "b", "b")), multiset);
  }

  public void testBuilderSetCount() {
    ImmutableSortedMultiset<String> multiset =
        ImmutableSortedMultiset.<String>naturalOrder()
            .add("a")
            .setCount("a", 2)
            .setCount("b", 3)
            .build();
    assertEquals(HashMultiset.create(asList("a", "a", "b", "b", "b")), multiset);
  }

  public void testBuilderAddHandlesNullsCorrectly() {
    ImmutableSortedMultiset.Builder<String> builder = ImmutableSortedMultiset.naturalOrder();
    assertThrows(NullPointerException.class, () -> builder.add((String) null));
  }

  public void testBuilderAddAllHandlesNullsCorrectly() {
    {
      ImmutableSortedMultiset.Builder<String> builder = ImmutableSortedMultiset.naturalOrder();
      assertThrows(NullPointerException.class, () -> builder.addAll((Collection<String>) null));
    }

    {
      ImmutableSortedMultiset.Builder<String> builder = ImmutableSortedMultiset.naturalOrder();
      List<String> listWithNulls = asList("a", null, "b");
      assertThrows(NullPointerException.class, () -> builder.addAll(listWithNulls));
    }

    {
      ImmutableSortedMultiset.Builder<String> builder = ImmutableSortedMultiset.naturalOrder();
      Multiset<String> multisetWithNull = LinkedHashMultiset.create(asList("a", null, "b"));
      assertThrows(NullPointerException.class, () -> builder.addAll(multisetWithNull));
    }
  }

  public void testBuilderAddCopiesHandlesNullsCorrectly() {
    ImmutableSortedMultiset.Builder<String> builder = ImmutableSortedMultiset.naturalOrder();
    assertThrows(NullPointerException.class, () -> builder.addCopies(null, 2));
  }

  public void testBuilderAddCopiesIllegal() {
    ImmutableSortedMultiset.Builder<String> builder = ImmutableSortedMultiset.naturalOrder();
    assertThrows(IllegalArgumentException.class, () -> builder.addCopies("a", -2));
  }

  public void testBuilderSetCountHandlesNullsCorrectly() {
    ImmutableSortedMultiset.Builder<String> builder =
        new ImmutableSortedMultiset.Builder<>(Ordering.natural().nullsFirst());
    assertThrows(NullPointerException.class, () -> builder.setCount(null, 2));
  }

  public void testBuilderSetCountIllegal() {
    ImmutableSortedMultiset.Builder<String> builder = ImmutableSortedMultiset.naturalOrder();
    assertThrows(IllegalArgumentException.class, () -> builder.setCount("a", -2));
  }

  public void testToImmutableSortedMultiset() {
    BiPredicate<ImmutableSortedMultiset<String>, ImmutableSortedMultiset<String>> equivalence =
        (ms1, ms2) ->
            ms1.equals(ms2)
                && ms1.entrySet().asList().equals(ms2.entrySet().asList())
                && ms1.comparator().equals(ms2.comparator());
    CollectorTester.of(
            ImmutableSortedMultiset.<String>toImmutableSortedMultiset(
                String.CASE_INSENSITIVE_ORDER),
            equivalence)
        .expectCollects(ImmutableSortedMultiset.emptyMultiset(String.CASE_INSENSITIVE_ORDER))
        .expectCollects(
            ImmutableSortedMultiset.orderedBy(String.CASE_INSENSITIVE_ORDER)
                .addCopies("a", 2)
                .addCopies("b", 1)
                .addCopies("c", 3)
                .build(),
            "a",
            "c",
            "b",
            "c",
            "A",
            "C");
  }

  public void testToImmutableSortedMultisetCountFunction() {
    BiPredicate<ImmutableSortedMultiset<String>, ImmutableSortedMultiset<String>> equivalence =
        (ms1, ms2) ->
            ms1.equals(ms2)
                && ms1.entrySet().asList().equals(ms2.entrySet().asList())
                && ms1.comparator().equals(ms2.comparator());
    CollectorTester.of(
            ImmutableSortedMultiset.<String, String>toImmutableSortedMultiset(
                String.CASE_INSENSITIVE_ORDER, e -> e, e -> 1),
            equivalence)
        .expectCollects(ImmutableSortedMultiset.emptyMultiset(String.CASE_INSENSITIVE_ORDER))
        .expectCollects(
            ImmutableSortedMultiset.orderedBy(String.CASE_INSENSITIVE_ORDER)
                .addCopies("a", 2)
                .addCopies("b", 1)
                .addCopies("c", 3)
                .build(),
            "a",
            "c",
            "b",
            "c",
            "A",
            "C");
  }

  public void testNullPointers() {
    new NullPointerTester().testAllPublicStaticMethods(ImmutableSortedMultiset.class);
  }

  public void testSerialization_empty() {
    Collection<String> c = ImmutableSortedMultiset.of();
    assertSame(c, SerializableTester.reserialize(c));
  }

  public void testSerialization_multiple() {
    Collection<String> c = ImmutableSortedMultiset.of("a", "b", "a");
    Collection<String> copy = SerializableTester.reserializeAndAssert(c);
    assertThat(copy).containsExactly("a", "a", "b").inOrder();
  }

  public void testSerialization_elementSet() {
    Multiset<String> c = ImmutableSortedMultiset.of("a", "b", "a");
    Collection<String> copy = SerializableTester.reserializeAndAssert(c.elementSet());
    assertThat(copy).containsExactly("a", "b").inOrder();
  }

  public void testSerialization_entrySet() {
    Multiset<String> c = ImmutableSortedMultiset.of("a", "b", "c");
    SerializableTester.reserializeAndAssert(c.entrySet());
  }

  public void testEquals_immutableMultiset() {
    Collection<String> c = ImmutableSortedMultiset.of("a", "b", "a");
    assertEquals(c, ImmutableSortedMultiset.of("a", "b", "a"));
    assertEquals(c, ImmutableSortedMultiset.of("a", "a", "b"));
    assertThat(c).isNotEqualTo(ImmutableSortedMultiset.of("a", "b"));
    assertThat(c).isNotEqualTo(ImmutableSortedMultiset.of("a", "b", "c", "d"));
  }

  public void testIterationOrder() {
    Collection<String> c = ImmutableSortedMultiset.of("a", "b", "a");
    assertThat(c).containsExactly("a", "a", "b").inOrder();
  }

  public void testMultisetWrites() {
    Multiset<String> multiset = ImmutableSortedMultiset.of("a", "b", "a");
    UnmodifiableCollectionTests.assertMultisetIsUnmodifiable(multiset, "test");
  }

  public void testAsList() {
    ImmutableSortedMultiset<String> multiset = ImmutableSortedMultiset.of("a", "a", "b", "b", "b");
    ImmutableList<String> list = multiset.asList();
    assertEquals(ImmutableList.of("a", "a", "b", "b", "b"), list);
    SerializableTester.reserializeAndAssert(list);
    assertEquals(2, list.indexOf("b"));
    assertEquals(4, list.lastIndexOf("b"));
  }

  public void testCopyOfDefensiveCopy() {
    // Depending on JDK version, either toArray() or toArray(T[]) may be called... use this class
    // rather than mocking to ensure that one of those methods is called.
    class TestArrayList<E> extends ArrayList<E> {
      boolean toArrayCalled = false;

      @Override
      public Object[] toArray() {
        toArrayCalled = true;
        return super.toArray();
      }

      @Override
      public <T> T[] toArray(T[] a) {
        toArrayCalled = true;
        return super.toArray(a);
      }
    }

    // Test that toArray() is used to make a defensive copy in copyOf(), so concurrently modified
    // synchronized collections can be safely copied.
    TestArrayList<String> toCopy = new TestArrayList<>();
    ImmutableSortedMultiset<String> unused =
        ImmutableSortedMultiset.copyOf(Ordering.natural(), toCopy);
    assertTrue(toCopy.toArrayCalled);
  }

  @SuppressWarnings("unchecked")
  public void testCopyOfSortedDefensiveCopy() {
    // Depending on JDK version, either toArray() or toArray(T[]) may be called... use this class
    // rather than mocking to ensure that one of those methods is called.
    class TestHashSet<E> extends HashSet<E> {
      boolean toArrayCalled = false;

      @Override
      public Object[] toArray() {
        toArrayCalled = true;
        return super.toArray();
      }

      @Override
      public <T> T[] toArray(T[] a) {
        toArrayCalled = true;
        return super.toArray(a);
      }
    }

    // Test that toArray() is used to make a defensive copy in copyOf(), so concurrently modified
    // synchronized collections can be safely copied.
    SortedMultiset<String> toCopy = mock(SortedMultiset.class);
    TestHashSet<Entry<String>> entrySet = new TestHashSet<>();
    when((Comparator<Comparable<String>>) toCopy.comparator())
        .thenReturn(Ordering.<Comparable<String>>natural());
    when(toCopy.entrySet()).thenReturn(entrySet);
    ImmutableSortedMultiset<String> unused = ImmutableSortedMultiset.copyOfSorted(toCopy);
    assertTrue(entrySet.toArrayCalled);
  }

  private static class IntegerDiv10 implements Comparable<IntegerDiv10> {
    final int value;

    IntegerDiv10(int value) {
      this.value = value;
    }

    @Override
    public int compareTo(IntegerDiv10 o) {
      return value / 10 - o.value / 10;
    }

    @Override
    public String toString() {
      return Integer.toString(value);
    }
  }

  public void testCopyOfDuplicateInconsistentWithEquals() {
    IntegerDiv10 three = new IntegerDiv10(3);
    IntegerDiv10 eleven = new IntegerDiv10(11);
    IntegerDiv10 twelve = new IntegerDiv10(12);
    IntegerDiv10 twenty = new IntegerDiv10(20);

    List<IntegerDiv10> original = ImmutableList.of(three, eleven, twelve, twenty);

    Multiset<IntegerDiv10> copy = ImmutableSortedMultiset.copyOf(original);
    assertTrue(copy.contains(eleven));
    assertTrue(copy.contains(twelve));
  }
}

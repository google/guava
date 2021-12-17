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
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSortedMultiset.Builder;
import com.google.common.collect.testing.ListTestSuiteBuilder;
import com.google.common.collect.testing.MinimalCollection;
import com.google.common.collect.testing.TestStringListGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.google.SortedMultisetTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringMultisetGenerator;
import com.google.common.collect.testing.google.UnmodifiableCollectionTests;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
                    Set<String> set = Sets.newHashSet();
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
        Ordering.natural()
            .lexicographical()
            .onResultOf(
                new Function<String[], Iterable<Comparable>>() {
                  @Override
                  public Iterable<Comparable> apply(String[] input) {
                    return Arrays.<Comparable>asList(input);
                  }
                });
    String[] array = new String[] {"a"};
    Multiset<String[]> multiset = ImmutableSortedMultiset.orderedBy(comparator).add(array).build();
    Multiset<String[]> expected = HashMultiset.create();
    expected.add(array);
    assertEquals(expected, multiset);
  }

  public void testCreation_arrayContainingOnlyNull() {
    String[] array = new String[] {null};
    try {
      ImmutableSortedMultiset.copyOf(array);
      fail();
    } catch (NullPointerException expected) {
    }
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
    try {
      ImmutableSortedMultiset.copyOf(c);
      fail();
    } catch (NullPointerException expected) {
    }
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
    try {
      ImmutableSortedMultiset.copyOf(c);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testCopyOf_iterator_empty() {
    Iterator<String> iterator = Iterators.emptyIterator();
    Multiset<String> multiset = ImmutableSortedMultiset.copyOf(iterator);
    assertTrue(multiset.isEmpty());
  }

  public void testCopyOf_iterator_oneElement() {
    Iterator<String> iterator = Iterators.singletonIterator("a");
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
    try {
      ImmutableSortedMultiset.copyOf(iterator);
      fail();
    } catch (NullPointerException expected) {
    }
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

  public void testBuilderAdd() {
    ImmutableSortedMultiset<String> multiset =
        ImmutableSortedMultiset.<String>naturalOrder().add("a").add("b").add("a").add("c").build();
    assertEquals(HashMultiset.create(asList("a", "b", "a", "c")), multiset);
  }

  public void testReuseBuilder() {
    Builder<String> builder =
        ImmutableSortedMultiset.<String>naturalOrder().add("a").add("b").add("a").add("c");
    ImmutableSortedMultiset<String> multiset1 = builder.build();
    assertEquals(HashMultiset.create(asList("a", "b", "a", "c")), multiset1);
    ImmutableSortedMultiset<String> multiset2 = builder.add("c").build();
    assertEquals(HashMultiset.create(asList("a", "b", "a", "c")), multiset1);
    assertEquals(HashMultiset.create(asList("a", "b", "a", "c", "c")), multiset2);
    assertTrue(
        ((RegularImmutableList<String>)
                    ((RegularImmutableSortedMultiset<String>) multiset1).elementSet.elements)
                .array
            != builder.elements);
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

  public void testBuilderSetCountZero() {
    ImmutableSortedMultiset<String> multiset =
        ImmutableSortedMultiset.<String>naturalOrder()
            .add("a")
            .setCount("a", 2)
            .setCount("b", 3)
            .setCount("a", 0)
            .build();
    assertEquals(HashMultiset.create(asList("b", "b", "b")), multiset);
  }

  public void testBuilderSetCountThenAdd() {
    ImmutableSortedMultiset<String> multiset =
        ImmutableSortedMultiset.<String>naturalOrder()
            .add("a")
            .setCount("a", 2)
            .setCount("b", 3)
            .setCount("a", 1)
            .add("a")
            .build();
    assertEquals(HashMultiset.create(asList("a", "a", "b", "b", "b")), multiset);
  }

  public void testBuilderAddHandlesNullsCorrectly() {
    ImmutableSortedMultiset.Builder<String> builder = ImmutableSortedMultiset.naturalOrder();
    try {
      builder.add((String) null);
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
    }
  }

  public void testBuilderAddAllHandlesNullsCorrectly() {
    ImmutableSortedMultiset.Builder<String> builder = ImmutableSortedMultiset.naturalOrder();
    try {
      builder.addAll((Collection<String>) null);
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
    }

    builder = ImmutableSortedMultiset.naturalOrder();
    List<String> listWithNulls = asList("a", null, "b");
    try {
      builder.addAll(listWithNulls);
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
    }

    builder = ImmutableSortedMultiset.naturalOrder();
    Multiset<String> multisetWithNull = LinkedHashMultiset.create(asList("a", null, "b"));
    try {
      builder.addAll(multisetWithNull);
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
    }
  }

  public void testBuilderAddCopiesHandlesNullsCorrectly() {
    ImmutableSortedMultiset.Builder<String> builder = ImmutableSortedMultiset.naturalOrder();
    try {
      builder.addCopies(null, 2);
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
    }
  }

  public void testBuilderAddCopiesIllegal() {
    ImmutableSortedMultiset.Builder<String> builder = ImmutableSortedMultiset.naturalOrder();
    try {
      builder.addCopies("a", -2);
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testBuilderSetCountHandlesNullsCorrectly() {
    ImmutableSortedMultiset.Builder<String> builder =
        new ImmutableSortedMultiset.Builder<>(Ordering.natural().nullsFirst());
    try {
      builder.setCount(null, 2);
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
    }
  }

  public void testBuilderSetCountIllegal() {
    ImmutableSortedMultiset.Builder<String> builder = ImmutableSortedMultiset.naturalOrder();
    try {
      builder.setCount("a", -2);
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
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

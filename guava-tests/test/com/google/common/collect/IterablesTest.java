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

import static com.google.common.collect.Iterables.skip;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static com.google.common.collect.testing.IteratorFeature.MODIFIABLE;
import static com.google.common.collect.testing.IteratorFeature.UNMODIFIABLE;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.testing.IteratorTester;
import com.google.common.testing.ClassSanityTester;
import com.google.common.testing.NullPointerTester;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedSet;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * Unit test for {@code Iterables}.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public class IterablesTest extends TestCase {

  public void testSize0() {
    Iterable<String> iterable = Collections.emptySet();
    assertEquals(0, Iterables.size(iterable));
  }

  public void testSize1Collection() {
    Iterable<String> iterable = Collections.singleton("a");
    assertEquals(1, Iterables.size(iterable));
  }

  public void testSize2NonCollection() {
    Iterable<Integer> iterable =
        new Iterable<Integer>() {
          @Override
          public Iterator<Integer> iterator() {
            return asList(0, 1).iterator();
          }
        };
    assertEquals(2, Iterables.size(iterable));
  }

  @SuppressWarnings("serial")
  public void testSize_collection_doesntIterate() {
    List<Integer> nums = asList(1, 2, 3, 4, 5);
    List<Integer> collection =
        new ArrayList<Integer>(nums) {
          @Override
          public Iterator<Integer> iterator() {
            throw new AssertionFailedError("Don't iterate me!");
          }
        };
    assertEquals(5, Iterables.size(collection));
  }

  private static Iterable<String> iterable(String... elements) {
    final List<String> list = asList(elements);
    return new Iterable<String>() {
      @Override
      public Iterator<String> iterator() {
        return list.iterator();
      }
    };
  }

  public void test_contains_null_set_yes() {
    Iterable<String> set = Sets.newHashSet("a", null, "b");
    assertTrue(Iterables.contains(set, null));
  }

  public void test_contains_null_set_no() {
    Iterable<String> set = Sets.newHashSet("a", "b");
    assertFalse(Iterables.contains(set, null));
  }

  public void test_contains_null_iterable_yes() {
    Iterable<String> set = iterable("a", null, "b");
    assertTrue(Iterables.contains(set, null));
  }

  public void test_contains_null_iterable_no() {
    Iterable<String> set = iterable("a", "b");
    assertFalse(Iterables.contains(set, null));
  }

  public void test_contains_nonnull_set_yes() {
    Iterable<String> set = Sets.newHashSet("a", null, "b");
    assertTrue(Iterables.contains(set, "b"));
  }

  public void test_contains_nonnull_set_no() {
    Iterable<String> set = Sets.newHashSet("a", "b");
    assertFalse(Iterables.contains(set, "c"));
  }

  public void test_contains_nonnull_iterable_yes() {
    Iterable<String> set = iterable("a", null, "b");
    assertTrue(Iterables.contains(set, "b"));
  }

  public void test_contains_nonnull_iterable_no() {
    Iterable<String> set = iterable("a", "b");
    assertFalse(Iterables.contains(set, "c"));
  }

  public void testGetOnlyElement_noDefault_valid() {
    Iterable<String> iterable = Collections.singletonList("foo");
    assertEquals("foo", Iterables.getOnlyElement(iterable));
  }

  public void testGetOnlyElement_noDefault_empty() {
    Iterable<String> iterable = Collections.emptyList();
    try {
      Iterables.getOnlyElement(iterable);
      fail();
    } catch (NoSuchElementException expected) {
    }
  }

  public void testGetOnlyElement_noDefault_multiple() {
    Iterable<String> iterable = asList("foo", "bar");
    try {
      Iterables.getOnlyElement(iterable);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testGetOnlyElement_withDefault_singleton() {
    Iterable<String> iterable = Collections.singletonList("foo");
    assertEquals("foo", Iterables.getOnlyElement(iterable, "bar"));
  }

  public void testGetOnlyElement_withDefault_empty() {
    Iterable<String> iterable = Collections.emptyList();
    assertEquals("bar", Iterables.getOnlyElement(iterable, "bar"));
  }

  public void testGetOnlyElement_withDefault_empty_null() {
    Iterable<String> iterable = Collections.emptyList();
    assertNull(Iterables.getOnlyElement(iterable, null));
  }

  public void testGetOnlyElement_withDefault_multiple() {
    Iterable<String> iterable = asList("foo", "bar");
    try {
      Iterables.getOnlyElement(iterable, "x");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @GwtIncompatible // Iterables.toArray(Iterable, Class)
  public void testToArrayEmpty() {
    Iterable<String> iterable = Collections.emptyList();
    String[] array = Iterables.toArray(iterable, String.class);
    assertTrue(Arrays.equals(new String[0], array));
  }

  @GwtIncompatible // Iterables.toArray(Iterable, Class)
  public void testToArraySingleton() {
    Iterable<String> iterable = Collections.singletonList("a");
    String[] array = Iterables.toArray(iterable, String.class);
    assertTrue(Arrays.equals(new String[] {"a"}, array));
  }

  @GwtIncompatible // Iterables.toArray(Iterable, Class)
  public void testToArray() {
    String[] sourceArray = new String[] {"a", "b", "c"};
    Iterable<String> iterable = asList(sourceArray);
    String[] newArray = Iterables.toArray(iterable, String.class);
    assertTrue(Arrays.equals(sourceArray, newArray));
  }

  public void testAny() {
    List<String> list = newArrayList();
    Predicate<String> predicate = Predicates.equalTo("pants");

    assertFalse(Iterables.any(list, predicate));
    list.add("cool");
    assertFalse(Iterables.any(list, predicate));
    list.add("pants");
    assertTrue(Iterables.any(list, predicate));
  }

  public void testAll() {
    List<String> list = newArrayList();
    Predicate<String> predicate = Predicates.equalTo("cool");

    assertTrue(Iterables.all(list, predicate));
    list.add("cool");
    assertTrue(Iterables.all(list, predicate));
    list.add("pants");
    assertFalse(Iterables.all(list, predicate));
  }

  public void testFind() {
    Iterable<String> list = newArrayList("cool", "pants");
    assertEquals("cool", Iterables.find(list, Predicates.equalTo("cool")));
    assertEquals("pants", Iterables.find(list, Predicates.equalTo("pants")));
    try {
      Iterables.find(list, Predicates.alwaysFalse());
      fail();
    } catch (NoSuchElementException e) {
    }
    assertEquals("cool", Iterables.find(list, Predicates.alwaysTrue()));
    assertCanIterateAgain(list);
  }

  public void testFind_withDefault() {
    Iterable<String> list = Lists.newArrayList("cool", "pants");
    assertEquals("cool", Iterables.find(list, Predicates.equalTo("cool"), "woot"));
    assertEquals("pants", Iterables.find(list, Predicates.equalTo("pants"), "woot"));
    assertEquals("woot", Iterables.find(list, Predicates.alwaysFalse(), "woot"));
    assertNull(Iterables.find(list, Predicates.alwaysFalse(), null));
    assertEquals("cool", Iterables.find(list, Predicates.alwaysTrue(), "woot"));
    assertCanIterateAgain(list);
  }

  public void testTryFind() {
    Iterable<String> list = newArrayList("cool", "pants");
    assertThat(Iterables.tryFind(list, Predicates.equalTo("cool"))).hasValue("cool");
    assertThat(Iterables.tryFind(list, Predicates.equalTo("pants"))).hasValue("pants");
    assertThat(Iterables.tryFind(list, Predicates.alwaysTrue())).hasValue("cool");
    assertThat(Iterables.tryFind(list, Predicates.alwaysFalse())).isAbsent();
    assertCanIterateAgain(list);
  }

  private static class TypeA {}

  private interface TypeB {}

  private static class HasBoth extends TypeA implements TypeB {}

  @GwtIncompatible // Iterables.filter(Iterable, Class)
  public void testFilterByType_iterator() throws Exception {
    HasBoth hasBoth = new HasBoth();
    Iterable<TypeA> alist = Lists.newArrayList(new TypeA(), new TypeA(), hasBoth, new TypeA());
    Iterable<TypeB> blist = Iterables.filter(alist, TypeB.class);
    assertThat(blist).containsExactly(hasBoth).inOrder();
  }

  @GwtIncompatible // Iterables.filter(Iterable, Class)
  public void testFilterByType_forEach() throws Exception {
    HasBoth hasBoth1 = new HasBoth();
    HasBoth hasBoth2 = new HasBoth();
    Iterable<TypeA> alist = Lists.newArrayList(hasBoth1, new TypeA(), hasBoth2, new TypeA());
    Iterable<TypeB> blist = Iterables.filter(alist, TypeB.class);

    Iterator<TypeB> expectedIterator = Arrays.<TypeB>asList(hasBoth1, hasBoth2).iterator();
    blist.forEach(b -> assertThat(b).isEqualTo(expectedIterator.next()));
    assertThat(expectedIterator.hasNext()).isFalse();
  }

  public void testTransform_iterator() {
    List<String> input = asList("1", "2", "3");
    Iterable<Integer> result =
        Iterables.transform(
            input,
            new Function<String, Integer>() {
              @Override
              public Integer apply(String from) {
                return Integer.valueOf(from);
              }
            });

    List<Integer> actual = newArrayList(result);
    List<Integer> expected = asList(1, 2, 3);
    assertEquals(expected, actual);
    assertCanIterateAgain(result);
    assertEquals("[1, 2, 3]", result.toString());
  }

  public void testTransform_forEach() {
    List<Integer> input = asList(1, 2, 3, 4);
    Iterable<String> result =
        Iterables.transform(
            input,
            new Function<Integer, String>() {
              @Override
              public String apply(Integer from) {
                return Integer.toBinaryString(from);
              }
            });

    Iterator<String> expectedIterator = asList("1", "10", "11", "100").iterator();
    result.forEach(s -> assertEquals(expectedIterator.next(), s));
    assertFalse(expectedIterator.hasNext());
  }

  public void testPoorlyBehavedTransform() {
    List<String> input = asList("1", null, "3");
    Iterable<Integer> result =
        Iterables.transform(
            input,
            new Function<String, Integer>() {
              @Override
              public Integer apply(String from) {
                return Integer.valueOf(from);
              }
            });

    Iterator<Integer> resultIterator = result.iterator();
    resultIterator.next();

    try {
      resultIterator.next();
      fail("Expected NFE");
    } catch (NumberFormatException expected) {
    }
  }

  public void testNullFriendlyTransform() {
    List<Integer> input = asList(1, 2, null, 3);
    Iterable<String> result =
        Iterables.transform(
            input,
            new Function<Integer, String>() {
              @Override
              public String apply(Integer from) {
                return String.valueOf(from);
              }
            });

    List<String> actual = newArrayList(result);
    List<String> expected = asList("1", "2", "null", "3");
    assertEquals(expected, actual);
  }

  // Far less exhaustive than the tests in IteratorsTest
  public void testCycle() {
    Iterable<String> cycle = Iterables.cycle("a", "b");

    int howManyChecked = 0;
    for (String string : cycle) {
      String expected = (howManyChecked % 2 == 0) ? "a" : "b";
      assertEquals(expected, string);
      if (howManyChecked++ == 5) {
        break;
      }
    }

    // We left the last iterator pointing to "b". But a new iterator should
    // always point to "a".
    for (String string : cycle) {
      assertEquals("a", string);
      break;
    }

    assertEquals("[a, b] (cycled)", cycle.toString());
  }

  // Again, the exhaustive tests are in IteratorsTest
  public void testConcatIterable() {
    List<Integer> list1 = newArrayList(1);
    List<Integer> list2 = newArrayList(4);

    @SuppressWarnings("unchecked")
    List<List<Integer>> input = newArrayList(list1, list2);

    Iterable<Integer> result = Iterables.concat(input);
    assertEquals(asList(1, 4), newArrayList(result));

    // Now change the inputs and see result dynamically change as well

    list1.add(2);
    List<Integer> list3 = newArrayList(3);
    input.add(1, list3);

    assertEquals(asList(1, 2, 3, 4), newArrayList(result));
    assertEquals("[1, 2, 3, 4]", result.toString());
  }

  public void testConcatVarargs() {
    List<Integer> list1 = newArrayList(1);
    List<Integer> list2 = newArrayList(4);
    List<Integer> list3 = newArrayList(7, 8);
    List<Integer> list4 = newArrayList(9);
    List<Integer> list5 = newArrayList(10);
    @SuppressWarnings("unchecked")
    Iterable<Integer> result = Iterables.concat(list1, list2, list3, list4, list5);
    assertEquals(asList(1, 4, 7, 8, 9, 10), newArrayList(result));
    assertEquals("[1, 4, 7, 8, 9, 10]", result.toString());
  }

  public void testConcatNullPointerException() {
    List<Integer> list1 = newArrayList(1);
    List<Integer> list2 = newArrayList(4);

    try {
      Iterables.concat(list1, null, list2);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testConcatPeformingFiniteCycle() {
    Iterable<Integer> iterable = asList(1, 2, 3);
    int n = 4;
    Iterable<Integer> repeated = Iterables.concat(Collections.nCopies(n, iterable));
    assertThat(repeated).containsExactly(1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3).inOrder();
  }

  public void testPartition_badSize() {
    Iterable<Integer> source = Collections.singleton(1);
    try {
      Iterables.partition(source, 0);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testPartition_empty() {
    Iterable<Integer> source = Collections.emptySet();
    Iterable<List<Integer>> partitions = Iterables.partition(source, 1);
    assertTrue(Iterables.isEmpty(partitions));
  }

  public void testPartition_singleton1() {
    Iterable<Integer> source = Collections.singleton(1);
    Iterable<List<Integer>> partitions = Iterables.partition(source, 1);
    assertEquals(1, Iterables.size(partitions));
    assertEquals(Collections.singletonList(1), partitions.iterator().next());
  }

  public void testPartition_view() {
    List<Integer> list = asList(1, 2);
    Iterable<List<Integer>> partitions = Iterables.partition(list, 2);

    // Changes before the partition is retrieved are reflected
    list.set(0, 3);

    Iterator<List<Integer>> iterator = partitions.iterator();

    // Changes before the partition is retrieved are reflected
    list.set(1, 4);

    List<Integer> first = iterator.next();

    // Changes after are not
    list.set(0, 5);

    assertEquals(ImmutableList.of(3, 4), first);
  }

  @GwtIncompatible // ?
  // TODO: Figure out why this is failing in GWT.
  public void testPartitionRandomAccessInput() {
    Iterable<Integer> source = asList(1, 2, 3);
    Iterable<List<Integer>> partitions = Iterables.partition(source, 2);
    Iterator<List<Integer>> iterator = partitions.iterator();
    assertTrue(iterator.next() instanceof RandomAccess);
    assertTrue(iterator.next() instanceof RandomAccess);
  }

  @GwtIncompatible // ?
  // TODO: Figure out why this is failing in GWT.
  public void testPartitionNonRandomAccessInput() {
    Iterable<Integer> source = Lists.newLinkedList(asList(1, 2, 3));
    Iterable<List<Integer>> partitions = Iterables.partition(source, 2);
    Iterator<List<Integer>> iterator = partitions.iterator();
    // Even though the input list doesn't implement RandomAccess, the output
    // lists do.
    assertTrue(iterator.next() instanceof RandomAccess);
    assertTrue(iterator.next() instanceof RandomAccess);
  }

  public void testPaddedPartition_basic() {
    List<Integer> list = asList(1, 2, 3, 4, 5);
    Iterable<List<Integer>> partitions = Iterables.paddedPartition(list, 2);
    assertEquals(3, Iterables.size(partitions));
    assertEquals(asList(5, null), Iterables.getLast(partitions));
  }

  public void testPaddedPartitionRandomAccessInput() {
    Iterable<Integer> source = asList(1, 2, 3);
    Iterable<List<Integer>> partitions = Iterables.paddedPartition(source, 2);
    Iterator<List<Integer>> iterator = partitions.iterator();
    assertTrue(iterator.next() instanceof RandomAccess);
    assertTrue(iterator.next() instanceof RandomAccess);
  }

  public void testPaddedPartitionNonRandomAccessInput() {
    Iterable<Integer> source = Lists.newLinkedList(asList(1, 2, 3));
    Iterable<List<Integer>> partitions = Iterables.paddedPartition(source, 2);
    Iterator<List<Integer>> iterator = partitions.iterator();
    // Even though the input list doesn't implement RandomAccess, the output
    // lists do.
    assertTrue(iterator.next() instanceof RandomAccess);
    assertTrue(iterator.next() instanceof RandomAccess);
  }

  // More tests in IteratorsTest
  public void testAddAllToList() {
    List<String> alreadyThere = newArrayList("already", "there");
    List<String> freshlyAdded = newArrayList("freshly", "added");

    boolean changed = Iterables.addAll(alreadyThere, freshlyAdded);
    assertThat(alreadyThere).containsExactly("already", "there", "freshly", "added").inOrder();
    assertTrue(changed);
  }

  private static void assertCanIterateAgain(Iterable<?> iterable) {
    for (@SuppressWarnings("unused") Object obj : iterable) {}
  }

  @GwtIncompatible // NullPointerTester
  public void testNullPointerExceptions() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(Iterables.class);
  }

  // More exhaustive tests are in IteratorsTest.
  public void testElementsEqual() throws Exception {
    Iterable<?> a;
    Iterable<?> b;

    // A few elements.
    a = asList(4, 8, 15, 16, 23, 42);
    b = asList(4, 8, 15, 16, 23, 42);
    assertTrue(Iterables.elementsEqual(a, b));

    // An element differs.
    a = asList(4, 8, 15, 12, 23, 42);
    b = asList(4, 8, 15, 16, 23, 42);
    assertFalse(Iterables.elementsEqual(a, b));

    // null versus non-null.
    a = asList(4, 8, 15, null, 23, 42);
    b = asList(4, 8, 15, 16, 23, 42);
    assertFalse(Iterables.elementsEqual(a, b));
    assertFalse(Iterables.elementsEqual(b, a));

    // Different lengths.
    a = asList(4, 8, 15, 16, 23);
    b = asList(4, 8, 15, 16, 23, 42);
    assertFalse(Iterables.elementsEqual(a, b));
    assertFalse(Iterables.elementsEqual(b, a));
  }

  public void testToString() {
    List<String> list = Collections.emptyList();
    assertEquals("[]", Iterables.toString(list));

    list = newArrayList("yam", "bam", "jam", "ham");
    assertEquals("[yam, bam, jam, ham]", Iterables.toString(list));
  }

  public void testLimit() {
    Iterable<String> iterable = newArrayList("foo", "bar", "baz");
    Iterable<String> limited = Iterables.limit(iterable, 2);

    List<String> expected = ImmutableList.of("foo", "bar");
    List<String> actual = newArrayList(limited);
    assertEquals(expected, actual);
    assertCanIterateAgain(limited);
    assertEquals("[foo, bar]", limited.toString());
  }

  public void testLimit_illegalArgument() {
    List<String> list = newArrayList("a", "b", "c");
    try {
      Iterables.limit(list, -1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testIsEmpty() {
    Iterable<String> emptyList = Collections.emptyList();
    assertTrue(Iterables.isEmpty(emptyList));

    Iterable<String> singletonList = Collections.singletonList("foo");
    assertFalse(Iterables.isEmpty(singletonList));
  }

  public void testSkip_simple() {
    Collection<String> set = ImmutableSet.of("a", "b", "c", "d", "e");
    assertEquals(newArrayList("c", "d", "e"), newArrayList(skip(set, 2)));
    assertEquals("[c, d, e]", skip(set, 2).toString());
  }

  public void testSkip_simpleList() {
    Collection<String> list = newArrayList("a", "b", "c", "d", "e");
    assertEquals(newArrayList("c", "d", "e"), newArrayList(skip(list, 2)));
    assertEquals("[c, d, e]", skip(list, 2).toString());
  }

  public void testSkip_pastEnd() {
    Collection<String> set = ImmutableSet.of("a", "b");
    assertEquals(emptyList(), newArrayList(skip(set, 20)));
  }

  public void testSkip_pastEndList() {
    Collection<String> list = newArrayList("a", "b");
    assertEquals(emptyList(), newArrayList(skip(list, 20)));
  }

  public void testSkip_skipNone() {
    Collection<String> set = ImmutableSet.of("a", "b");
    assertEquals(newArrayList("a", "b"), newArrayList(skip(set, 0)));
  }

  public void testSkip_skipNoneList() {
    Collection<String> list = newArrayList("a", "b");
    assertEquals(newArrayList("a", "b"), newArrayList(skip(list, 0)));
  }

  public void testSkip_removal() {
    Collection<String> set = Sets.newHashSet("a", "b");
    Iterator<String> iterator = skip(set, 2).iterator();
    try {
      iterator.next();
    } catch (NoSuchElementException suppressed) {
      // We want remove() to fail even after a failed call to next().
    }
    try {
      iterator.remove();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
  }

  public void testSkip_allOfMutableList_modifiable() {
    List<String> list = newArrayList("a", "b");
    Iterator<String> iterator = skip(list, 2).iterator();
    try {
      iterator.remove();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
  }

  public void testSkip_allOfImmutableList_modifiable() {
    List<String> list = ImmutableList.of("a", "b");
    Iterator<String> iterator = skip(list, 2).iterator();
    try {
      iterator.remove();
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }
  }

  @GwtIncompatible // slow (~35s)
  public void testSkip_iterator() {
    new IteratorTester<Integer>(
        5, MODIFIABLE, newArrayList(2, 3), IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override
      protected Iterator<Integer> newTargetIterator() {
        return skip(newLinkedHashSet(asList(1, 2, 3)), 1).iterator();
      }
    }.test();
  }

  @GwtIncompatible // slow (~35s)
  public void testSkip_iteratorList() {
    new IteratorTester<Integer>(
        5, MODIFIABLE, newArrayList(2, 3), IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override
      protected Iterator<Integer> newTargetIterator() {
        return skip(newArrayList(1, 2, 3), 1).iterator();
      }
    }.test();
  }

  public void testSkip_nonStructurallyModifiedList() throws Exception {
    List<String> list = newArrayList("a", "b", "c");
    Iterable<String> tail = skip(list, 1);
    Iterator<String> tailIterator = tail.iterator();
    list.set(2, "C");
    assertEquals("b", tailIterator.next());
    assertEquals("C", tailIterator.next());
    assertFalse(tailIterator.hasNext());
  }

  public void testSkip_structurallyModifiedSkipSome() throws Exception {
    Collection<String> set = newLinkedHashSet(asList("a", "b", "c"));
    Iterable<String> tail = skip(set, 1);
    set.remove("b");
    set.addAll(newArrayList("A", "B", "C"));
    assertThat(tail).containsExactly("c", "A", "B", "C").inOrder();
  }

  public void testSkip_structurallyModifiedSkipSomeList() throws Exception {
    List<String> list = newArrayList("a", "b", "c");
    Iterable<String> tail = skip(list, 1);
    list.subList(1, 3).clear();
    list.addAll(0, newArrayList("A", "B", "C"));
    assertThat(tail).containsExactly("B", "C", "a").inOrder();
  }

  public void testSkip_structurallyModifiedSkipAll() throws Exception {
    Collection<String> set = newLinkedHashSet(asList("a", "b", "c"));
    Iterable<String> tail = skip(set, 2);
    set.remove("a");
    set.remove("b");
    assertFalse(tail.iterator().hasNext());
  }

  public void testSkip_structurallyModifiedSkipAllList() throws Exception {
    List<String> list = newArrayList("a", "b", "c");
    Iterable<String> tail = skip(list, 2);
    list.subList(0, 2).clear();
    assertTrue(Iterables.isEmpty(tail));
  }

  public void testSkip_illegalArgument() {
    List<String> list = newArrayList("a", "b", "c");
    try {
      skip(list, -1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  private void testGetOnAbc(Iterable<String> iterable) {
    try {
      Iterables.get(iterable, -1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
    assertEquals("a", Iterables.get(iterable, 0));
    assertEquals("b", Iterables.get(iterable, 1));
    assertEquals("c", Iterables.get(iterable, 2));
    try {
      Iterables.get(iterable, 3);
      fail();
    } catch (IndexOutOfBoundsException nsee) {
    }
    try {
      Iterables.get(iterable, 4);
      fail();
    } catch (IndexOutOfBoundsException nsee) {
    }
  }

  private void testGetOnEmpty(Iterable<String> iterable) {
    try {
      Iterables.get(iterable, 0);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  public void testGet_list() {
    testGetOnAbc(newArrayList("a", "b", "c"));
  }

  public void testGet_emptyList() {
    testGetOnEmpty(Collections.<String>emptyList());
  }

  public void testGet_sortedSet() {
    testGetOnAbc(ImmutableSortedSet.of("b", "c", "a"));
  }

  public void testGet_emptySortedSet() {
    testGetOnEmpty(ImmutableSortedSet.<String>of());
  }

  public void testGet_iterable() {
    testGetOnAbc(ImmutableSet.of("a", "b", "c"));
  }

  public void testGet_emptyIterable() {
    testGetOnEmpty(Sets.<String>newHashSet());
  }

  public void testGet_withDefault_negativePosition() {
    try {
      Iterables.get(newArrayList("a", "b", "c"), -1, "d");
      fail();
    } catch (IndexOutOfBoundsException expected) {
      // pass
    }
  }

  public void testGet_withDefault_simple() {
    ArrayList<String> list = newArrayList("a", "b", "c");
    assertEquals("b", Iterables.get(list, 1, "d"));
  }

  public void testGet_withDefault_iterable() {
    Set<String> set = ImmutableSet.of("a", "b", "c");
    assertEquals("b", Iterables.get(set, 1, "d"));
  }

  public void testGet_withDefault_last() {
    ArrayList<String> list = newArrayList("a", "b", "c");
    assertEquals("c", Iterables.get(list, 2, "d"));
  }

  public void testGet_withDefault_lastPlusOne() {
    ArrayList<String> list = newArrayList("a", "b", "c");
    assertEquals("d", Iterables.get(list, 3, "d"));
  }

  public void testGet_withDefault_doesntIterate() {
    List<String> list = new DiesOnIteratorArrayList();
    list.add("a");
    assertEquals("a", Iterables.get(list, 0, "b"));
  }

  public void testGetFirst_withDefault_singleton() {
    Iterable<String> iterable = Collections.singletonList("foo");
    assertEquals("foo", Iterables.getFirst(iterable, "bar"));
  }

  public void testGetFirst_withDefault_empty() {
    Iterable<String> iterable = Collections.emptyList();
    assertEquals("bar", Iterables.getFirst(iterable, "bar"));
  }

  public void testGetFirst_withDefault_empty_null() {
    Iterable<String> iterable = Collections.emptyList();
    assertNull(Iterables.getFirst(iterable, null));
  }

  public void testGetFirst_withDefault_multiple() {
    Iterable<String> iterable = asList("foo", "bar");
    assertEquals("foo", Iterables.getFirst(iterable, "qux"));
  }

  public void testGetLast_list() {
    List<String> list = newArrayList("a", "b", "c");
    assertEquals("c", Iterables.getLast(list));
  }

  public void testGetLast_emptyList() {
    List<String> list = Collections.emptyList();
    try {
      Iterables.getLast(list);
      fail();
    } catch (NoSuchElementException e) {
    }
  }

  public void testGetLast_sortedSet() {
    SortedSet<String> sortedSet = ImmutableSortedSet.of("b", "c", "a");
    assertEquals("c", Iterables.getLast(sortedSet));
  }

  public void testGetLast_withDefault_singleton() {
    Iterable<String> iterable = Collections.singletonList("foo");
    assertEquals("foo", Iterables.getLast(iterable, "bar"));
  }

  public void testGetLast_withDefault_empty() {
    Iterable<String> iterable = Collections.emptyList();
    assertEquals("bar", Iterables.getLast(iterable, "bar"));
  }

  public void testGetLast_withDefault_empty_null() {
    Iterable<String> iterable = Collections.emptyList();
    assertNull(Iterables.getLast(iterable, null));
  }

  public void testGetLast_withDefault_multiple() {
    Iterable<String> iterable = asList("foo", "bar");
    assertEquals("bar", Iterables.getLast(iterable, "qux"));
  }

  /**
   * {@link ArrayList} extension that forbids the use of {@link Collection#iterator} for tests that
   * need to prove that it isn't called.
   */
  private static class DiesOnIteratorArrayList extends ArrayList<String> {
    /** @throws UnsupportedOperationException all the time */
    @Override
    public Iterator<String> iterator() {
      throw new UnsupportedOperationException();
    }
  }

  public void testGetLast_withDefault_not_empty_list() {
    // TODO: verify that this is the best testing strategy.
    List<String> diesOnIteratorList = new DiesOnIteratorArrayList();
    diesOnIteratorList.add("bar");

    assertEquals("bar", Iterables.getLast(diesOnIteratorList, "qux"));
  }

  public void testGetLast_emptySortedSet() {
    SortedSet<String> sortedSet = ImmutableSortedSet.of();
    try {
      Iterables.getLast(sortedSet);
      fail();
    } catch (NoSuchElementException e) {
    }
  }

  public void testGetLast_iterable() {
    Set<String> set = ImmutableSet.of("a", "b", "c");
    assertEquals("c", Iterables.getLast(set));
  }

  public void testGetLast_emptyIterable() {
    Set<String> set = Sets.newHashSet();
    try {
      Iterables.getLast(set);
      fail();
    } catch (NoSuchElementException e) {
    }
  }

  public void testUnmodifiableIterable() {
    List<String> list = newArrayList("a", "b", "c");
    Iterable<String> iterable = Iterables.unmodifiableIterable(list);
    Iterator<String> iterator = iterable.iterator();
    iterator.next();
    try {
      iterator.remove();
      fail();
    } catch (UnsupportedOperationException expected) {
    }
    assertEquals("[a, b, c]", iterable.toString());
  }

  public void testUnmodifiableIterable_forEach() {
    List<String> list = newArrayList("a", "b", "c", "d");
    Iterable<String> iterable = Iterables.unmodifiableIterable(list);
    Iterator<String> expectedIterator = list.iterator();
    iterable.forEach(s -> assertEquals(expectedIterator.next(), s));
    assertFalse(expectedIterator.hasNext());
  }

  @SuppressWarnings("deprecation") // test of deprecated method
  public void testUnmodifiableIterableShortCircuit() {
    List<String> list = newArrayList("a", "b", "c");
    Iterable<String> iterable = Iterables.unmodifiableIterable(list);
    Iterable<String> iterable2 = Iterables.unmodifiableIterable(iterable);
    assertSame(iterable, iterable2);
    ImmutableList<String> immutableList = ImmutableList.of("a", "b", "c");
    assertSame(immutableList, Iterables.unmodifiableIterable(immutableList));
    assertSame(immutableList, Iterables.unmodifiableIterable((List<String>) immutableList));
  }

  public void testFrequency_multiset() {
    Multiset<String> multiset = ImmutableMultiset.of("a", "b", "a", "c", "b", "a");
    assertEquals(3, Iterables.frequency(multiset, "a"));
    assertEquals(2, Iterables.frequency(multiset, "b"));
    assertEquals(1, Iterables.frequency(multiset, "c"));
    assertEquals(0, Iterables.frequency(multiset, "d"));
    assertEquals(0, Iterables.frequency(multiset, 4.2));
    assertEquals(0, Iterables.frequency(multiset, null));
  }

  public void testFrequency_set() {
    Set<String> set = Sets.newHashSet("a", "b", "c");
    assertEquals(1, Iterables.frequency(set, "a"));
    assertEquals(1, Iterables.frequency(set, "b"));
    assertEquals(1, Iterables.frequency(set, "c"));
    assertEquals(0, Iterables.frequency(set, "d"));
    assertEquals(0, Iterables.frequency(set, 4.2));
    assertEquals(0, Iterables.frequency(set, null));
  }

  public void testFrequency_list() {
    List<String> list = newArrayList("a", "b", "a", "c", "b", "a");
    assertEquals(3, Iterables.frequency(list, "a"));
    assertEquals(2, Iterables.frequency(list, "b"));
    assertEquals(1, Iterables.frequency(list, "c"));
    assertEquals(0, Iterables.frequency(list, "d"));
    assertEquals(0, Iterables.frequency(list, 4.2));
    assertEquals(0, Iterables.frequency(list, null));
  }

  public void testRemoveAll_collection() {
    List<String> list = newArrayList("a", "b", "c", "d", "e");
    assertTrue(Iterables.removeAll(list, newArrayList("b", "d", "f")));
    assertEquals(newArrayList("a", "c", "e"), list);
    assertFalse(Iterables.removeAll(list, newArrayList("x", "y", "z")));
    assertEquals(newArrayList("a", "c", "e"), list);
  }

  public void testRemoveAll_iterable() {
    final List<String> list = newArrayList("a", "b", "c", "d", "e");
    Iterable<String> iterable =
        new Iterable<String>() {
          @Override
          public Iterator<String> iterator() {
            return list.iterator();
          }
        };
    assertTrue(Iterables.removeAll(iterable, newArrayList("b", "d", "f")));
    assertEquals(newArrayList("a", "c", "e"), list);
    assertFalse(Iterables.removeAll(iterable, newArrayList("x", "y", "z")));
    assertEquals(newArrayList("a", "c", "e"), list);
  }

  public void testRetainAll_collection() {
    List<String> list = newArrayList("a", "b", "c", "d", "e");
    assertTrue(Iterables.retainAll(list, newArrayList("b", "d", "f")));
    assertEquals(newArrayList("b", "d"), list);
    assertFalse(Iterables.retainAll(list, newArrayList("b", "e", "d")));
    assertEquals(newArrayList("b", "d"), list);
  }

  public void testRetainAll_iterable() {
    final List<String> list = newArrayList("a", "b", "c", "d", "e");
    Iterable<String> iterable =
        new Iterable<String>() {
          @Override
          public Iterator<String> iterator() {
            return list.iterator();
          }
        };
    assertTrue(Iterables.retainAll(iterable, newArrayList("b", "d", "f")));
    assertEquals(newArrayList("b", "d"), list);
    assertFalse(Iterables.retainAll(iterable, newArrayList("b", "e", "d")));
    assertEquals(newArrayList("b", "d"), list);
  }

  public void testRemoveIf_randomAccess() {
    List<String> list = newArrayList("a", "b", "c", "d", "e");
    assertTrue(
        Iterables.removeIf(
            list,
            new Predicate<String>() {
              @Override
              public boolean apply(String s) {
                return s.equals("b") || s.equals("d") || s.equals("f");
              }
            }));
    assertEquals(newArrayList("a", "c", "e"), list);
    assertFalse(
        Iterables.removeIf(
            list,
            new Predicate<String>() {
              @Override
              public boolean apply(String s) {
                return s.equals("x") || s.equals("y") || s.equals("z");
              }
            }));
    assertEquals(newArrayList("a", "c", "e"), list);
  }

  public void testRemoveIf_randomAccess_notPermittingDuplicates() {
    // https://github.com/google/guava/issues/1596
    List<String> uniqueList = newArrayList("a", "b", "c", "d", "e");
    assertThat(uniqueList).containsNoDuplicates();

    assertTrue(uniqueList instanceof RandomAccess);
    assertTrue(
        Iterables.removeIf(
            uniqueList,
            new Predicate<String>() {
              @Override
              public boolean apply(String s) {
                return s.equals("b") || s.equals("d") || s.equals("f");
              }
            }));
    assertEquals(newArrayList("a", "c", "e"), uniqueList);
    assertFalse(
        Iterables.removeIf(
            uniqueList,
            new Predicate<String>() {
              @Override
              public boolean apply(String s) {
                return s.equals("x") || s.equals("y") || s.equals("z");
              }
            }));
    assertEquals(newArrayList("a", "c", "e"), uniqueList);
  }

  public void testRemoveIf_transformedList() {
    List<String> list = newArrayList("1", "2", "3", "4", "5");
    List<Integer> transformed =
        Lists.transform(
            list,
            new Function<String, Integer>() {
              @Override
              public Integer apply(String s) {
                return Integer.valueOf(s);
              }
            });
    assertTrue(
        Iterables.removeIf(
            transformed,
            new Predicate<Integer>() {
              @Override
              public boolean apply(Integer n) {
                return (n & 1) == 0; // isEven()
              }
            }));
    assertEquals(newArrayList("1", "3", "5"), list);
    assertFalse(
        Iterables.removeIf(
            transformed,
            new Predicate<Integer>() {
              @Override
              public boolean apply(Integer n) {
                return (n & 1) == 0; // isEven()
              }
            }));
    assertEquals(newArrayList("1", "3", "5"), list);
  }

  public void testRemoveIf_noRandomAccess() {
    List<String> list = Lists.newLinkedList(asList("a", "b", "c", "d", "e"));
    assertTrue(
        Iterables.removeIf(
            list,
            new Predicate<String>() {
              @Override
              public boolean apply(String s) {
                return s.equals("b") || s.equals("d") || s.equals("f");
              }
            }));
    assertEquals(newArrayList("a", "c", "e"), list);
    assertFalse(
        Iterables.removeIf(
            list,
            new Predicate<String>() {
              @Override
              public boolean apply(String s) {
                return s.equals("x") || s.equals("y") || s.equals("z");
              }
            }));
    assertEquals(newArrayList("a", "c", "e"), list);
  }

  public void testRemoveIf_iterable() {
    final List<String> list = Lists.newLinkedList(asList("a", "b", "c", "d", "e"));
    Iterable<String> iterable =
        new Iterable<String>() {
          @Override
          public Iterator<String> iterator() {
            return list.iterator();
          }
        };
    assertTrue(
        Iterables.removeIf(
            iterable,
            new Predicate<String>() {
              @Override
              public boolean apply(String s) {
                return s.equals("b") || s.equals("d") || s.equals("f");
              }
            }));
    assertEquals(newArrayList("a", "c", "e"), list);
    assertFalse(
        Iterables.removeIf(
            iterable,
            new Predicate<String>() {
              @Override
              public boolean apply(String s) {
                return s.equals("x") || s.equals("y") || s.equals("z");
              }
            }));
    assertEquals(newArrayList("a", "c", "e"), list);
  }

  // The Maps returned by Maps.filterEntries(), Maps.filterKeys(), and
  // Maps.filterValues() are not tested with removeIf() since Maps are not
  // Iterable.  Those returned by Iterators.filter() and Iterables.filter()
  // are not tested because they are unmodifiable.

  public void testIterableWithToString() {
    assertEquals("[]", create().toString());
    assertEquals("[a]", create("a").toString());
    assertEquals("[a, b, c]", create("a", "b", "c").toString());
    assertEquals("[c, a, a]", create("c", "a", "a").toString());
  }

  public void testIterableWithToStringNull() {
    assertEquals("[null]", create((String) null).toString());
    assertEquals("[null, null]", create(null, null).toString());
    assertEquals("[, null, a]", create("", null, "a").toString());
  }

  /** Returns a new iterable over the specified strings. */
  private static Iterable<String> create(String... strings) {
    final List<String> list = asList(strings);
    return new FluentIterable<String>() {
      @Override
      public Iterator<String> iterator() {
        return list.iterator();
      }
    };
  }

  public void testConsumingIterable() {
    // Test data
    List<String> list = Lists.newArrayList(asList("a", "b"));

    // Test & Verify
    Iterable<String> consumingIterable = Iterables.consumingIterable(list);
    assertEquals("Iterables.consumingIterable(...)", consumingIterable.toString());
    Iterator<String> consumingIterator = consumingIterable.iterator();

    assertThat(list).containsExactly("a", "b").inOrder();

    assertTrue(consumingIterator.hasNext());
    assertThat(list).containsExactly("a", "b").inOrder();
    assertEquals("a", consumingIterator.next());
    assertThat(list).contains("b");

    assertTrue(consumingIterator.hasNext());
    assertEquals("b", consumingIterator.next());
    assertThat(list).isEmpty();

    assertFalse(consumingIterator.hasNext());
  }

  @GwtIncompatible // ?
  // TODO: Figure out why this is failing in GWT.
  public void testConsumingIterable_duelingIterators() {
    // Test data
    List<String> list = Lists.newArrayList(asList("a", "b"));

    // Test & Verify
    Iterator<String> i1 = Iterables.consumingIterable(list).iterator();
    Iterator<String> i2 = Iterables.consumingIterable(list).iterator();

    i1.next();
    try {
      i2.next();
      fail("Concurrent modification should throw an exception.");
    } catch (ConcurrentModificationException cme) {
      // Pass
    }
  }

  public void testConsumingIterable_queue_iterator() {
    final List<Integer> items = ImmutableList.of(4, 8, 15, 16, 23, 42);
    new IteratorTester<Integer>(3, UNMODIFIABLE, items, IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override
      protected Iterator<Integer> newTargetIterator() {
        return Iterables.consumingIterable(Lists.newLinkedList(items)).iterator();
      }
    }.test();
  }

  public void testConsumingIterable_queue_removesFromQueue() {
    Queue<Integer> queue = Lists.newLinkedList(asList(5, 14));

    Iterator<Integer> consumingIterator = Iterables.consumingIterable(queue).iterator();

    assertEquals(5, queue.peek().intValue());
    assertEquals(5, consumingIterator.next().intValue());

    assertEquals(14, queue.peek().intValue());
    assertTrue(consumingIterator.hasNext());
    assertTrue(queue.isEmpty());
  }

  public void testConsumingIterable_noIteratorCall() {
    Queue<Integer> queue = new UnIterableQueue<>(Lists.newLinkedList(asList(5, 14)));

    Iterator<Integer> consumingIterator = Iterables.consumingIterable(queue).iterator();
    /*
     * Make sure that we can get an element off without calling
     * UnIterableQueue.iterator().
     */
    assertEquals(5, consumingIterator.next().intValue());
  }

  private static class UnIterableQueue<T> extends ForwardingQueue<T> {
    private final Queue<T> queue;

    UnIterableQueue(Queue<T> queue) {
      this.queue = queue;
    }

    @Override
    public Iterator<T> iterator() {
      throw new UnsupportedOperationException();
    }

    @Override
    protected Queue<T> delegate() {
      return queue;
    }
  }

  public void testIndexOf_empty() {
    List<String> list = new ArrayList<>();
    assertEquals(-1, Iterables.indexOf(list, Predicates.equalTo("")));
  }

  public void testIndexOf_oneElement() {
    List<String> list = Lists.newArrayList("bob");
    assertEquals(0, Iterables.indexOf(list, Predicates.equalTo("bob")));
    assertEquals(-1, Iterables.indexOf(list, Predicates.equalTo("jack")));
  }

  public void testIndexOf_twoElements() {
    List<String> list = Lists.newArrayList("mary", "bob");
    assertEquals(0, Iterables.indexOf(list, Predicates.equalTo("mary")));
    assertEquals(1, Iterables.indexOf(list, Predicates.equalTo("bob")));
    assertEquals(-1, Iterables.indexOf(list, Predicates.equalTo("jack")));
  }

  public void testIndexOf_withDuplicates() {
    List<String> list = Lists.newArrayList("mary", "bob", "bob", "bob", "sam");
    assertEquals(0, Iterables.indexOf(list, Predicates.equalTo("mary")));
    assertEquals(1, Iterables.indexOf(list, Predicates.equalTo("bob")));
    assertEquals(4, Iterables.indexOf(list, Predicates.equalTo("sam")));
    assertEquals(-1, Iterables.indexOf(list, Predicates.equalTo("jack")));
  }

  private static final Predicate<CharSequence> STARTSWITH_A =
      new Predicate<CharSequence>() {
        @Override
        public boolean apply(CharSequence input) {
          return (input.length() > 0) && (input.charAt(0) == 'a');
        }
      };

  public void testIndexOf_genericPredicate() {
    List<CharSequence> sequences = Lists.newArrayList();
    sequences.add("bob");
    sequences.add(new StringBuilder("charlie"));
    sequences.add(new StringBuffer("henry"));
    sequences.add(new StringBuilder("apple"));
    sequences.add("lemon");

    assertEquals(3, Iterables.indexOf(sequences, STARTSWITH_A));
  }

  public void testIndexOf_genericPredicate2() {
    List<String> sequences = Lists.newArrayList("bob", "charlie", "henry", "apple", "lemon");
    assertEquals(3, Iterables.indexOf(sequences, STARTSWITH_A));
  }

  public void testMergeSorted_empty() {
    // Setup
    Iterable<Iterable<Integer>> elements = ImmutableList.of();

    // Test
    Iterable<Integer> iterable = Iterables.mergeSorted(elements, Ordering.natural());

    // Verify
    Iterator<Integer> iterator = iterable.iterator();
    assertFalse(iterator.hasNext());
    try {
      iterator.next();
      fail("next() on empty iterator should throw NoSuchElementException");
    } catch (NoSuchElementException e) {
      // Huzzah!
    }
  }

  public void testMergeSorted_single_empty() {
    // Setup
    Iterable<Integer> iterable0 = ImmutableList.of();
    Iterable<Iterable<Integer>> iterables = ImmutableList.of(iterable0);

    // Test & Verify
    verifyMergeSorted(iterables, ImmutableList.<Integer>of());
  }

  public void testMergeSorted_single() {
    // Setup
    Iterable<Integer> iterable0 = ImmutableList.of(1, 2, 3);
    Iterable<Iterable<Integer>> iterables = ImmutableList.of(iterable0);

    // Test & Verify
    verifyMergeSorted(iterables, iterable0);
  }

  public void testMergeSorted_pyramid() {
    List<Iterable<Integer>> iterables = Lists.newLinkedList();
    List<Integer> allIntegers = Lists.newArrayList();

    // Creates iterators like: {{}, {0}, {0, 1}, {0, 1, 2}, ...}
    for (int i = 0; i < 10; i++) {
      List<Integer> list = Lists.newLinkedList();
      for (int j = 0; j < i; j++) {
        list.add(j);
        allIntegers.add(j);
      }
      iterables.add(Ordering.natural().sortedCopy(list));
    }

    verifyMergeSorted(iterables, allIntegers);
  }

  // Like the pyramid, but creates more unique values, along with repeated ones.
  public void testMergeSorted_skipping_pyramid() {
    List<Iterable<Integer>> iterables = Lists.newLinkedList();
    List<Integer> allIntegers = Lists.newArrayList();

    for (int i = 0; i < 20; i++) {
      List<Integer> list = Lists.newLinkedList();
      for (int j = 0; j < i; j++) {
        list.add(j * i);
        allIntegers.add(j * i);
      }
      iterables.add(Ordering.natural().sortedCopy(list));
    }

    verifyMergeSorted(iterables, allIntegers);
  }

  @GwtIncompatible // reflection
  @AndroidIncompatible // see ImmutableTableTest.testNullPointerInstance
  public void testIterables_nullCheck() throws Exception {
    new ClassSanityTester()
        .forAllPublicStaticMethods(Iterables.class)
        .thatReturn(Iterable.class)
        .testNulls();
  }

  private static void verifyMergeSorted(
      Iterable<Iterable<Integer>> iterables, Iterable<Integer> unsortedExpected) {
    Iterable<Integer> expected = Ordering.natural().sortedCopy(unsortedExpected);

    Iterable<Integer> mergedIterator = Iterables.mergeSorted(iterables, Ordering.natural());

    assertEquals(Lists.newLinkedList(expected), Lists.newLinkedList(mergedIterator));
  }
}

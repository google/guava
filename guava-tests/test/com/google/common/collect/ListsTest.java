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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.elementsEqual;
import static com.google.common.collect.Lists.cartesianProduct;
import static com.google.common.collect.Lists.charactersOf;
import static com.google.common.collect.Lists.computeArrayListCapacity;
import static com.google.common.collect.Lists.newArrayListWithExpectedSize;
import static com.google.common.collect.Lists.partition;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.ReflectionFreeAssertThrows.assertThrows;
import static com.google.common.collect.testing.IteratorFeature.UNMODIFIABLE;
import static com.google.common.truth.Truth.assertThat;
import static java.lang.System.arraycopy;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
import static java.util.Collections.singletonList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.testing.IteratorTester;
import com.google.common.collect.testing.ListTestSuiteBuilder;
import com.google.common.collect.testing.TestStringListGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.ListFeature;
import com.google.common.collect.testing.google.ListGenerators.CharactersOfCharSequenceGenerator;
import com.google.common.collect.testing.google.ListGenerators.CharactersOfStringGenerator;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.concurrent.CopyOnWriteArrayList;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for {@code Lists}.
 *
 * @author Kevin Bourrillion
 * @author Mike Bostock
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
public class ListsTest extends TestCase {

  private static final Collection<Integer> SOME_COLLECTION = asList(0, 1, 1);

  private static final Iterable<Integer> SOME_ITERABLE = new SomeIterable();

  private static final class RemoveFirstFunction implements Function<String, String>, Serializable {
    @Override
    public String apply(String from) {
      return (from.length() == 0) ? from : from.substring(1);
    }
  }

  private static class SomeIterable implements Iterable<Integer>, Serializable {
    @Override
    public Iterator<Integer> iterator() {
      return SOME_COLLECTION.iterator();
    }

    private static final long serialVersionUID = 0;
  }

  private static final List<Integer> SOME_LIST = Lists.newArrayList(1, 2, 3, 4);

  private static final List<Integer> SOME_SEQUENTIAL_LIST = Lists.newLinkedList(asList(1, 2, 3, 4));

  private static final List<String> SOME_STRING_LIST = asList("1", "2", "3", "4");

  private static final Function<Number, String> SOME_FUNCTION = new SomeFunction();

  private static class SomeFunction implements Function<Number, String>, Serializable {
    @Override
    public String apply(Number n) {
      return String.valueOf(n);
    }

    private static final long serialVersionUID = 0;
  }

  @J2ktIncompatible
  @GwtIncompatible // suite
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(ListsTest.class);

    suite.addTest(
        ListTestSuiteBuilder.using(
                new TestStringListGenerator() {
                  @Override
                  protected List<String> create(String[] elements) {
                    String[] rest = new String[elements.length - 1];
                    arraycopy(elements, 1, rest, 0, elements.length - 1);
                    return Lists.asList(elements[0], rest);
                  }
                })
            .named("Lists.asList, 2 parameter")
            .withFeatures(
                CollectionSize.SEVERAL,
                CollectionSize.ONE,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        ListTestSuiteBuilder.using(
                new TestStringListGenerator() {
                  @Override
                  protected List<String> create(String[] elements) {
                    String[] rest = new String[elements.length - 2];
                    arraycopy(elements, 2, rest, 0, elements.length - 2);
                    return Lists.asList(elements[0], elements[1], rest);
                  }
                })
            .named("Lists.asList, 3 parameter")
            .withFeatures(
                CollectionSize.SEVERAL,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    final Function<String, String> removeFirst = new RemoveFirstFunction();

    suite.addTest(
        ListTestSuiteBuilder.using(
                new TestStringListGenerator() {
                  @Override
                  protected List<String> create(String[] elements) {
                    List<String> fromList = Lists.newArrayList();
                    for (String element : elements) {
                      fromList.add("q" + checkNotNull(element));
                    }
                    return transform(fromList, removeFirst);
                  }
                })
            .named("Lists.transform, random access, no nulls")
            .withFeatures(
                CollectionSize.ANY,
                ListFeature.REMOVE_OPERATIONS,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        ListTestSuiteBuilder.using(
                new TestStringListGenerator() {
                  @Override
                  protected List<String> create(String[] elements) {
                    List<String> fromList = Lists.newLinkedList();
                    for (String element : elements) {
                      fromList.add("q" + checkNotNull(element));
                    }
                    return transform(fromList, removeFirst);
                  }
                })
            .named("Lists.transform, sequential access, no nulls")
            .withFeatures(
                CollectionSize.ANY,
                ListFeature.REMOVE_OPERATIONS,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        ListTestSuiteBuilder.using(
                new TestStringListGenerator() {
                  @Override
                  protected List<String> create(String[] elements) {
                    List<String> fromList = Lists.newArrayList(elements);
                    return transform(fromList, Functions.<String>identity());
                  }
                })
            .named("Lists.transform, random access, nulls")
            .withFeatures(
                CollectionSize.ANY,
                ListFeature.REMOVE_OPERATIONS,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        ListTestSuiteBuilder.using(
                new TestStringListGenerator() {
                  @Override
                  protected List<String> create(String[] elements) {
                    List<String> fromList = Lists.newLinkedList(asList(elements));
                    return transform(fromList, Functions.<String>identity());
                  }
                })
            .named("Lists.transform, sequential access, nulls")
            .withFeatures(
                CollectionSize.ANY,
                ListFeature.REMOVE_OPERATIONS,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        ListTestSuiteBuilder.using(
                new TestStringListGenerator() {
                  @Override
                  protected List<String> create(String[] elements) {
                    List<String> list = Lists.newArrayList();
                    for (int i = elements.length - 1; i >= 0; i--) {
                      list.add(elements[i]);
                    }
                    return Lists.reverse(list);
                  }
                })
            .named("Lists.reverse[ArrayList]")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.ALLOWS_NULL_VALUES,
                ListFeature.GENERAL_PURPOSE)
            .createTestSuite());

    suite.addTest(
        ListTestSuiteBuilder.using(
                new TestStringListGenerator() {
                  @Override
                  protected List<String> create(String[] elements) {
                    String[] reverseElements = new String[elements.length];
                    for (int i = elements.length - 1, j = 0; i >= 0; i--, j++) {
                      reverseElements[j] = elements[i];
                    }
                    return Lists.reverse(asList(reverseElements));
                  }
                })
            .named("Lists.reverse[Arrays.asList]")
            .withFeatures(
                CollectionSize.ANY, CollectionFeature.ALLOWS_NULL_VALUES, ListFeature.SUPPORTS_SET)
            .createTestSuite());

    suite.addTest(
        ListTestSuiteBuilder.using(
                new TestStringListGenerator() {
                  @Override
                  protected List<String> create(String[] elements) {
                    List<String> list = Lists.newLinkedList();
                    for (int i = elements.length - 1; i >= 0; i--) {
                      list.add(elements[i]);
                    }
                    return Lists.reverse(list);
                  }
                })
            .named("Lists.reverse[LinkedList]")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.ALLOWS_NULL_VALUES,
                ListFeature.GENERAL_PURPOSE)
            .createTestSuite());

    suite.addTest(
        ListTestSuiteBuilder.using(
                new TestStringListGenerator() {
                  @Override
                  protected List<String> create(String[] elements) {
                    ImmutableList.Builder<String> builder = ImmutableList.builder();
                    for (int i = elements.length - 1; i >= 0; i--) {
                      builder.add(elements[i]);
                    }
                    return Lists.reverse(builder.build());
                  }
                })
            .named("Lists.reverse[ImmutableList]")
            .withFeatures(CollectionSize.ANY, CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        ListTestSuiteBuilder.using(new CharactersOfStringGenerator())
            .named("Lists.charactersOf[String]")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        ListTestSuiteBuilder.using(new CharactersOfCharSequenceGenerator())
            .named("Lists.charactersOf[CharSequence]")
            .withFeatures(CollectionSize.ANY, CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    return suite;
  }

  public void testCharactersOfIsView() {
    StringBuilder builder = new StringBuilder("abc");
    List<Character> chars = charactersOf(builder);
    assertEquals(asList('a', 'b', 'c'), chars);
    builder.append("def");
    assertEquals(asList('a', 'b', 'c', 'd', 'e', 'f'), chars);
    builder.deleteCharAt(5);
    assertEquals(asList('a', 'b', 'c', 'd', 'e'), chars);
  }

  public void testNewArrayListEmpty() {
    ArrayList<Integer> list = Lists.newArrayList();
    assertEquals(emptyList(), list);
  }

  public void testNewArrayListWithCapacity() {
    ArrayList<Integer> list = Lists.newArrayListWithCapacity(0);
    assertEquals(emptyList(), list);

    ArrayList<Integer> bigger = Lists.newArrayListWithCapacity(256);
    assertEquals(emptyList(), bigger);
  }

  public void testNewArrayListWithCapacity_negative() {
    assertThrows(IllegalArgumentException.class, () -> Lists.newArrayListWithCapacity(-1));
  }

  public void testNewArrayListWithExpectedSize() {
    ArrayList<Integer> list = newArrayListWithExpectedSize(0);
    assertEquals(emptyList(), list);

    ArrayList<Integer> bigger = newArrayListWithExpectedSize(256);
    assertEquals(emptyList(), bigger);
  }

  public void testNewArrayListWithExpectedSize_negative() {
    assertThrows(IllegalArgumentException.class, () -> newArrayListWithExpectedSize(-1));
  }

  public void testNewArrayListVarArgs() {
    ArrayList<Integer> list = Lists.newArrayList(0, 1, 1);
    assertEquals(SOME_COLLECTION, list);
  }

  public void testComputeArrayListCapacity() {
    assertEquals(5, computeArrayListCapacity(0));
    assertEquals(13, computeArrayListCapacity(8));
    assertEquals(89, computeArrayListCapacity(77));
    assertEquals(22000005, computeArrayListCapacity(20000000));
    assertEquals(Integer.MAX_VALUE, computeArrayListCapacity(Integer.MAX_VALUE - 1000));
  }

  public void testNewArrayListFromCollection() {
    ArrayList<Integer> list = Lists.newArrayList(SOME_COLLECTION);
    assertEquals(SOME_COLLECTION, list);
  }

  public void testNewArrayListFromIterable() {
    ArrayList<Integer> list = Lists.newArrayList(SOME_ITERABLE);
    assertEquals(SOME_COLLECTION, list);
  }

  public void testNewArrayListFromIterator() {
    ArrayList<Integer> list = Lists.newArrayList(SOME_COLLECTION.iterator());
    assertEquals(SOME_COLLECTION, list);
  }

  public void testNewLinkedListEmpty() {
    LinkedList<Integer> list = Lists.newLinkedList();
    assertEquals(emptyList(), list);
  }

  public void testNewLinkedListFromCollection() {
    LinkedList<Integer> list = Lists.newLinkedList(SOME_COLLECTION);
    assertEquals(SOME_COLLECTION, list);
  }

  public void testNewLinkedListFromIterable() {
    LinkedList<Integer> list = Lists.newLinkedList(SOME_ITERABLE);
    assertEquals(SOME_COLLECTION, list);
  }

  @J2ktIncompatible
  @GwtIncompatible // CopyOnWriteArrayList
  public void testNewCOWALEmpty() {
    CopyOnWriteArrayList<Integer> list = Lists.newCopyOnWriteArrayList();
    assertEquals(emptyList(), list);
  }

  @J2ktIncompatible
  @GwtIncompatible // CopyOnWriteArrayList
  public void testNewCOWALFromIterable() {
    CopyOnWriteArrayList<Integer> list = Lists.newCopyOnWriteArrayList(SOME_ITERABLE);
    assertEquals(SOME_COLLECTION, list);
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testNullPointerExceptions() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(Lists.class);
  }

  /**
   * This is just here to illustrate how {@code Arrays#asList} differs from {@code
   * Lists#newArrayList}.
   */
  public void testArraysAsList() {
    List<String> ourWay = Lists.newArrayList("foo", "bar", "baz");
    List<String> otherWay = asList("foo", "bar", "baz");

    // They're logically equal
    assertEquals(ourWay, otherWay);

    // The result of Arrays.asList() is mutable
    otherWay.set(0, "FOO");
    assertEquals("FOO", otherWay.get(0));

    // But it can't grow
    assertThrows(UnsupportedOperationException.class, () -> otherWay.add("nope"));

    // And it can't shrink
    assertThrows(UnsupportedOperationException.class, () -> otherWay.remove(2));
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testAsList1() {
    List<String> list = Lists.asList("foo", new String[] {"bar", "baz"});
    checkFooBarBazList(list);
    SerializableTester.reserializeAndAssert(list);
    assertTrue(list instanceof RandomAccess);

    new IteratorTester<String>(
        5, UNMODIFIABLE, asList("foo", "bar", "baz"), IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override
      protected Iterator<String> newTargetIterator() {
        return Lists.asList("foo", new String[] {"bar", "baz"}).iterator();
      }
    }.test();
  }

  private void checkFooBarBazList(List<String> list) {
    assertThat(list).containsExactly("foo", "bar", "baz").inOrder();
    assertEquals(3, list.size());
    assertIndexIsOutOfBounds(list, -1);
    assertEquals("foo", list.get(0));
    assertEquals("bar", list.get(1));
    assertEquals("baz", list.get(2));
    assertIndexIsOutOfBounds(list, 3);
  }

  public void testAsList1Small() {
    List<String> list = Lists.asList("foo", new String[0]);
    assertThat(list).contains("foo");
    assertEquals(1, list.size());
    assertIndexIsOutOfBounds(list, -1);
    assertEquals("foo", list.get(0));
    assertIndexIsOutOfBounds(list, 1);
    assertTrue(list instanceof RandomAccess);

    new IteratorTester<String>(
        3, UNMODIFIABLE, singletonList("foo"), IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override
      protected Iterator<String> newTargetIterator() {
        return Lists.asList("foo", new String[0]).iterator();
      }
    }.test();
  }

  public void testAsList2() {
    List<String> list = Lists.asList("foo", "bar", new String[] {"baz"});
    checkFooBarBazList(list);
    assertTrue(list instanceof RandomAccess);

    new IteratorTester<String>(
        5, UNMODIFIABLE, asList("foo", "bar", "baz"), IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override
      protected Iterator<String> newTargetIterator() {
        return Lists.asList("foo", "bar", new String[] {"baz"}).iterator();
      }
    }.test();
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testAsList2Small() {
    List<String> list = Lists.asList("foo", "bar", new String[0]);
    assertThat(list).containsExactly("foo", "bar").inOrder();
    assertEquals(2, list.size());
    assertIndexIsOutOfBounds(list, -1);
    assertEquals("foo", list.get(0));
    assertEquals("bar", list.get(1));
    assertIndexIsOutOfBounds(list, 2);
    SerializableTester.reserializeAndAssert(list);
    assertTrue(list instanceof RandomAccess);

    new IteratorTester<String>(
        5, UNMODIFIABLE, asList("foo", "bar"), IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override
      protected Iterator<String> newTargetIterator() {
        return Lists.asList("foo", "bar", new String[0]).iterator();
      }
    }.test();
  }

  private static void assertIndexIsOutOfBounds(List<String> list, int index) {
    try {
      list.get(index);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  public void testReverseViewRandomAccess() {
    List<Integer> fromList = Lists.newArrayList(SOME_LIST);
    List<Integer> toList = Lists.reverse(fromList);
    assertReverseView(fromList, toList);
  }

  public void testReverseViewSequential() {
    List<Integer> fromList = Lists.newLinkedList(SOME_SEQUENTIAL_LIST);
    List<Integer> toList = Lists.reverse(fromList);
    assertReverseView(fromList, toList);
  }

  private static void assertReverseView(List<Integer> fromList, List<Integer> toList) {
    /* fromList modifications reflected in toList */
    fromList.set(0, 5);
    assertEquals(asList(4, 3, 2, 5), toList);
    fromList.add(6);
    assertEquals(asList(6, 4, 3, 2, 5), toList);
    fromList.add(2, 9);
    assertEquals(asList(6, 4, 3, 9, 2, 5), toList);
    fromList.remove(Integer.valueOf(2));
    assertEquals(asList(6, 4, 3, 9, 5), toList);
    fromList.remove(3);
    assertEquals(asList(6, 3, 9, 5), toList);

    /* toList modifications reflected in fromList */
    toList.remove(0);
    assertEquals(asList(5, 9, 3), fromList);
    toList.add(7);
    assertEquals(asList(7, 5, 9, 3), fromList);
    toList.add(5);
    assertEquals(asList(5, 7, 5, 9, 3), fromList);
    toList.remove(Integer.valueOf(5));
    assertEquals(asList(5, 7, 9, 3), fromList);
    toList.set(1, 8);
    assertEquals(asList(5, 7, 8, 3), fromList);
    toList.clear();
    assertEquals(emptyList(), fromList);
  }

  @SafeVarargs
  private static <E> List<E> list(E... elements) {
    return ImmutableList.copyOf(elements);
  }

  public void testCartesianProduct_binary1x1() {
    assertThat(cartesianProduct(list(1), list(2))).contains(list(1, 2));
  }

  public void testCartesianProduct_binary1x2() {
    assertThat(cartesianProduct(list(1), list(2, 3)))
        .containsExactly(list(1, 2), list(1, 3))
        .inOrder();
  }

  public void testCartesianProduct_binary2x2() {
    assertThat(cartesianProduct(list(1, 2), list(3, 4)))
        .containsExactly(list(1, 3), list(1, 4), list(2, 3), list(2, 4))
        .inOrder();
  }

  public void testCartesianProduct_2x2x2() {
    assertThat(cartesianProduct(list(0, 1), list(0, 1), list(0, 1)))
        .containsExactly(
            list(0, 0, 0),
            list(0, 0, 1),
            list(0, 1, 0),
            list(0, 1, 1),
            list(1, 0, 0),
            list(1, 0, 1),
            list(1, 1, 0),
            list(1, 1, 1))
        .inOrder();
  }

  public void testCartesianProduct_contains() {
    List<List<Integer>> actual = cartesianProduct(list(1, 2), list(3, 4));
    assertTrue(actual.contains(list(1, 3)));
    assertTrue(actual.contains(list(1, 4)));
    assertTrue(actual.contains(list(2, 3)));
    assertTrue(actual.contains(list(2, 4)));
    assertFalse(actual.contains(list(3, 1)));
  }

  public void testCartesianProduct_indexOf() {
    List<List<Integer>> actual = cartesianProduct(list(1, 2), list(3, 4));
    assertEquals(0, actual.indexOf(list(1, 3)));
    assertEquals(1, actual.indexOf(list(1, 4)));
    assertEquals(2, actual.indexOf(list(2, 3)));
    assertEquals(3, actual.indexOf(list(2, 4)));
    assertEquals(-1, actual.indexOf(list(3, 1)));

    assertEquals(-1, actual.indexOf(list(1)));
    assertEquals(-1, actual.indexOf(list(1, 1, 1)));
  }

  public void testCartesianProduct_lastIndexOf() {
    List<List<Integer>> actual = cartesianProduct(list(1, 1), list(2, 3));
    assertThat(actual.lastIndexOf(list(1, 2))).isEqualTo(2);
    assertThat(actual.lastIndexOf(list(1, 3))).isEqualTo(3);
    assertThat(actual.lastIndexOf(list(1, 1))).isEqualTo(-1);

    assertThat(actual.lastIndexOf(list(1))).isEqualTo(-1);
    assertThat(actual.lastIndexOf(list(1, 1, 1))).isEqualTo(-1);
  }

  public void testCartesianProduct_unrelatedTypes() {
    List<Integer> x = list(1, 2);
    List<String> y = list("3", "4");

    List<Object> exp1 = list((Object) 1, "3");
    List<Object> exp2 = list((Object) 1, "4");
    List<Object> exp3 = list((Object) 2, "3");
    List<Object> exp4 = list((Object) 2, "4");

    assertThat(Lists.<Object>cartesianProduct(x, y))
        .containsExactly(exp1, exp2, exp3, exp4)
        .inOrder();
  }

  public void testCartesianProductTooBig() {
    List<String> list = nCopies(10000, "foo");
    assertThrows(
        IllegalArgumentException.class, () -> cartesianProduct(list, list, list, list, list));
  }

  public void testTransformHashCodeRandomAccess() {
    List<String> list = transform(SOME_LIST, SOME_FUNCTION);
    assertEquals(SOME_STRING_LIST.hashCode(), list.hashCode());
  }

  public void testTransformHashCodeSequential() {
    List<String> list = transform(SOME_SEQUENTIAL_LIST, SOME_FUNCTION);
    assertEquals(SOME_STRING_LIST.hashCode(), list.hashCode());
  }

  public void testTransformModifiableRandomAccess() {
    List<Integer> fromList = Lists.newArrayList(SOME_LIST);
    List<String> list = transform(fromList, SOME_FUNCTION);
    assertTransformModifiable(list);
  }

  public void testTransformModifiableSequential() {
    List<Integer> fromList = Lists.newLinkedList(SOME_SEQUENTIAL_LIST);
    List<String> list = transform(fromList, SOME_FUNCTION);
    assertTransformModifiable(list);
  }

  private static void assertTransformModifiable(List<String> list) {
    try {
      list.add("5");
      fail("transformed list is addable");
    } catch (UnsupportedOperationException expected) {
    }
    list.remove(0);
    assertEquals(asList("2", "3", "4"), list);
    list.remove("3");
    assertEquals(asList("2", "4"), list);
    try {
      list.set(0, "5");
      fail("transformed list is setable");
    } catch (UnsupportedOperationException expected) {
    }
    list.clear();
    assertEquals(emptyList(), list);
  }

  public void testTransformViewRandomAccess() {
    List<Integer> fromList = Lists.newArrayList(SOME_LIST);
    List<String> toList = transform(fromList, SOME_FUNCTION);
    assertTransformView(fromList, toList);
  }

  public void testTransformViewSequential() {
    List<Integer> fromList = Lists.newLinkedList(SOME_SEQUENTIAL_LIST);
    List<String> toList = transform(fromList, SOME_FUNCTION);
    assertTransformView(fromList, toList);
  }

  private static void assertTransformView(List<Integer> fromList, List<String> toList) {
    /* fromList modifications reflected in toList */
    fromList.set(0, 5);
    assertEquals(asList("5", "2", "3", "4"), toList);
    fromList.add(6);
    assertEquals(asList("5", "2", "3", "4", "6"), toList);
    fromList.remove(Integer.valueOf(2));
    assertEquals(asList("5", "3", "4", "6"), toList);
    fromList.remove(2);
    assertEquals(asList("5", "3", "6"), toList);

    /* toList modifications reflected in fromList */
    toList.remove(2);
    assertEquals(asList(5, 3), fromList);
    toList.remove("5");
    assertEquals(asList(3), fromList);
    toList.clear();
    assertEquals(emptyList(), fromList);
  }

  public void testTransformRandomAccess() {
    List<String> list = transform(SOME_LIST, SOME_FUNCTION);
    assertTrue(list instanceof RandomAccess);
  }

  public void testTransformSequential() {
    List<String> list = transform(SOME_SEQUENTIAL_LIST, SOME_FUNCTION);
    assertFalse(list instanceof RandomAccess);
  }

  public void testTransformRandomAccessIsNotEmpty() {
    List<String> transformedList = transform(SOME_LIST, SOME_FUNCTION);
    assertFalse(transformedList.isEmpty());
  }

  public void testTransformSequentialIsNotEmpty() {
    List<String> transformedList = transform(SOME_SEQUENTIAL_LIST, SOME_FUNCTION);
    assertFalse(transformedList.isEmpty());
  }

  public void testTransformListIteratorRandomAccess() {
    List<Integer> fromList = Lists.newArrayList(SOME_LIST);
    List<String> list = transform(fromList, SOME_FUNCTION);
    assertTransformListIterator(list);
  }

  public void testTransformListIteratorSequential() {
    List<Integer> fromList = Lists.newLinkedList(SOME_SEQUENTIAL_LIST);
    List<String> list = transform(fromList, SOME_FUNCTION);
    assertTransformListIterator(list);
  }

  public void testTransformPreservesIOOBEsThrownByFunction() {
    assertThrows(
        IndexOutOfBoundsException.class,
        () ->
            transform(
                    ImmutableList.of("foo", "bar"),
                    new Function<String, String>() {
                      @Override
                      public String apply(String input) {
                        throw new IndexOutOfBoundsException();
                      }
                    })
                .toArray());
  }

  private static void assertTransformListIterator(List<String> list) {
    ListIterator<String> iterator = list.listIterator(1);
    assertEquals(1, iterator.nextIndex());
    assertEquals("2", iterator.next());
    assertEquals("3", iterator.next());
    assertEquals("4", iterator.next());
    assertEquals(4, iterator.nextIndex());
    try {
      iterator.next();
      fail("did not detect end of list");
    } catch (NoSuchElementException expected) {
    }
    assertEquals(3, iterator.previousIndex());
    assertEquals("4", iterator.previous());
    assertEquals("3", iterator.previous());
    assertEquals("2", iterator.previous());
    assertTrue(iterator.hasPrevious());
    assertEquals("1", iterator.previous());
    assertFalse(iterator.hasPrevious());
    assertEquals(-1, iterator.previousIndex());
    try {
      iterator.previous();
      fail("did not detect beginning of list");
    } catch (NoSuchElementException expected) {
    }
    iterator.remove();
    assertEquals(asList("2", "3", "4"), list);
    assertFalse(list.isEmpty());

    // An UnsupportedOperationException or IllegalStateException may occur.
    try {
      iterator.add("1");
      fail("transformed list iterator is addable");
    } catch (UnsupportedOperationException | IllegalStateException expected) {
    }
    try {
      iterator.set("1");
      fail("transformed list iterator is settable");
    } catch (UnsupportedOperationException | IllegalStateException expected) {
    }
  }

  public void testTransformIteratorRandomAccess() {
    List<Integer> fromList = Lists.newArrayList(SOME_LIST);
    List<String> list = transform(fromList, SOME_FUNCTION);
    assertTransformIterator(list);
  }

  public void testTransformIteratorSequential() {
    List<Integer> fromList = Lists.newLinkedList(SOME_SEQUENTIAL_LIST);
    List<String> list = transform(fromList, SOME_FUNCTION);
    assertTransformIterator(list);
  }

  /**
   * This test depends on the fact that {@code AbstractSequentialList.iterator} transforms the
   * {@code iterator()} call into a call on {@code listIterator(int)}. This is fine because the
   * behavior is clearly documented so it's not expected to change.
   */
  public void testTransformedSequentialIterationUsesBackingListIterationOnly() {
    List<Integer> randomAccessList = Lists.newArrayList(SOME_SEQUENTIAL_LIST);
    List<Integer> listIteratorOnlyList = new ListIterationOnlyList<>(randomAccessList);
    List<String> transform = transform(listIteratorOnlyList, SOME_FUNCTION);
    assertTrue(elementsEqual(transform, transform(randomAccessList, SOME_FUNCTION)));
  }

  private static class ListIterationOnlyList<E> extends ForwardingList<E> {
    private final List<E> realDelegate;

    private ListIterationOnlyList(List<E> realDelegate) {
      this.realDelegate = realDelegate;
    }

    @Override
    public int size() {
      return realDelegate.size();
    }

    @Override
    public ListIterator<E> listIterator(int index) {
      return realDelegate.listIterator(index);
    }

    @Override
    protected List<E> delegate() {
      throw new UnsupportedOperationException("This list only supports ListIterator");
    }
  }

  private static void assertTransformIterator(List<String> list) {
    Iterator<String> iterator = list.iterator();
    assertTrue(iterator.hasNext());
    assertEquals("1", iterator.next());
    assertTrue(iterator.hasNext());
    assertEquals("2", iterator.next());
    assertTrue(iterator.hasNext());
    assertEquals("3", iterator.next());
    assertTrue(iterator.hasNext());
    assertEquals("4", iterator.next());
    assertFalse(iterator.hasNext());
    try {
      iterator.next();
      fail("did not detect end of list");
    } catch (NoSuchElementException expected) {
    }
    iterator.remove();
    assertEquals(asList("1", "2", "3"), list);
    assertFalse(iterator.hasNext());
  }

  public void testPartition_badSize() {
    List<Integer> source = singletonList(1);
    assertThrows(IllegalArgumentException.class, () -> partition(source, 0));
  }

  public void testPartition_empty() {
    List<Integer> source = emptyList();
    List<List<Integer>> partitions = partition(source, 1);
    assertTrue(partitions.isEmpty());
    assertEquals(0, partitions.size());
  }

  public void testPartition_1_1() {
    List<Integer> source = singletonList(1);
    List<List<Integer>> partitions = partition(source, 1);
    assertEquals(1, partitions.size());
    assertEquals(singletonList(1), partitions.get(0));
  }

  public void testPartition_1_2() {
    List<Integer> source = singletonList(1);
    List<List<Integer>> partitions = partition(source, 2);
    assertEquals(1, partitions.size());
    assertEquals(singletonList(1), partitions.get(0));
  }

  public void testPartition_2_1() {
    List<Integer> source = asList(1, 2);
    List<List<Integer>> partitions = partition(source, 1);
    assertEquals(2, partitions.size());
    assertEquals(singletonList(1), partitions.get(0));
    assertEquals(singletonList(2), partitions.get(1));
  }

  public void testPartition_3_2() {
    List<Integer> source = asList(1, 2, 3);
    List<List<Integer>> partitions = partition(source, 2);
    assertEquals(2, partitions.size());
    assertEquals(asList(1, 2), partitions.get(0));
    assertEquals(asList(3), partitions.get(1));
  }

  @J2ktIncompatible // Arrays.asList(...).subList() doesn't implement RandomAccess in J2KT.
  @GwtIncompatible // ArrayList.subList doesn't implement RandomAccess in GWT.
  public void testPartitionRandomAccessTrue() {
    List<Integer> source = asList(1, 2, 3);
    List<List<Integer>> partitions = partition(source, 2);

    assertTrue(
        "partition should be RandomAccess, but not: " + partitions.getClass(),
        partitions instanceof RandomAccess);

    assertTrue(
        "partition[0] should be RandomAccess, but not: " + partitions.get(0).getClass(),
        partitions.get(0) instanceof RandomAccess);

    assertTrue(
        "partition[1] should be RandomAccess, but not: " + partitions.get(1).getClass(),
        partitions.get(1) instanceof RandomAccess);
  }

  public void testPartitionRandomAccessFalse() {
    List<Integer> source = Lists.newLinkedList(asList(1, 2, 3));
    List<List<Integer>> partitions = partition(source, 2);
    assertFalse(partitions instanceof RandomAccess);
    assertFalse(partitions.get(0) instanceof RandomAccess);
    assertFalse(partitions.get(1) instanceof RandomAccess);
  }

  // TODO: use the ListTestSuiteBuilder

  public void testPartition_view() {
    List<Integer> list = asList(1, 2, 3);
    List<List<Integer>> partitions = partition(list, 3);

    // Changes before the partition is retrieved are reflected
    list.set(0, 3);

    Iterator<List<Integer>> iterator = partitions.iterator();

    // Changes before the partition is retrieved are reflected
    list.set(1, 4);

    List<Integer> first = iterator.next();

    // Changes after are too (unlike Iterables.partition)
    list.set(2, 5);

    assertEquals(asList(3, 4, 5), first);

    // Changes to a sublist also write through to the original list
    first.set(1, 6);
    assertEquals(asList(3, 6, 5), list);
  }

  public void testPartitionSize_1() {
    List<Integer> list = asList(1, 2, 3);
    assertEquals(1, partition(list, Integer.MAX_VALUE).size());
    assertEquals(1, partition(list, Integer.MAX_VALUE - 1).size());
  }

  @GwtIncompatible // cannot do such a big explicit copy
  @J2ktIncompatible // too slow
  public void testPartitionSize_2() {
    assertEquals(2, partition(nCopies(0x40000001, 1), 0x40000000).size());
  }
}

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

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.collect.CollectPreconditions.checkRemove;
import static com.google.common.collect.Iterators.advance;
import static com.google.common.collect.Iterators.all;
import static com.google.common.collect.Iterators.any;
import static com.google.common.collect.Iterators.elementsEqual;
import static com.google.common.collect.Iterators.emptyIterator;
import static com.google.common.collect.Iterators.filter;
import static com.google.common.collect.Iterators.find;
import static com.google.common.collect.Iterators.frequency;
import static com.google.common.collect.Iterators.get;
import static com.google.common.collect.Iterators.getLast;
import static com.google.common.collect.Iterators.getOnlyElement;
import static com.google.common.collect.Iterators.singletonIterator;
import static com.google.common.collect.Iterators.tryFind;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.ReflectionFreeAssertThrows.assertThrows;
import static com.google.common.collect.testing.IteratorFeature.MODIFIABLE;
import static com.google.common.collect.testing.IteratorFeature.UNMODIFIABLE;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.testing.IteratorFeature;
import com.google.common.collect.testing.IteratorTester;
import com.google.common.collect.testing.ListTestSuiteBuilder;
import com.google.common.collect.testing.TestStringListGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.ListFeature;
import com.google.common.primitives.Ints;
import com.google.common.testing.NullPointerTester;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.Set;
import java.util.Vector;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Unit test for {@code Iterators}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
@NullMarked
public class IteratorsTest extends TestCase {

  @J2ktIncompatible
  @GwtIncompatible // suite
  @AndroidIncompatible // test-suite builders
  public static Test suite() {
    TestSuite suite = new TestSuite(IteratorsTest.class.getSimpleName());
    suite.addTest(testsForRemoveAllAndRetainAll());
    suite.addTestSuite(IteratorsTest.class);
    return suite;
  }

  @SuppressWarnings("DoNotCall")
  public void testEmptyIterator() {
    Iterator<String> iterator = emptyIterator();
    assertFalse(iterator.hasNext());
    assertThrows(NoSuchElementException.class, () -> iterator.next());
    assertThrows(UnsupportedOperationException.class, () -> iterator.remove());
  }

  @SuppressWarnings("DoNotCall")
  public void testEmptyListIterator() {
    ListIterator<String> iterator = Iterators.emptyListIterator();
    assertFalse(iterator.hasNext());
    assertFalse(iterator.hasPrevious());
    assertEquals(0, iterator.nextIndex());
    assertEquals(-1, iterator.previousIndex());
    assertThrows(NoSuchElementException.class, () -> iterator.next());
    assertThrows(NoSuchElementException.class, () -> iterator.previous());
    assertThrows(UnsupportedOperationException.class, () -> iterator.remove());
    assertThrows(UnsupportedOperationException.class, () -> iterator.set("a"));
    assertThrows(UnsupportedOperationException.class, () -> iterator.add("a"));
  }

  public void testEmptyModifiableIterator() {
    Iterator<String> iterator = Iterators.emptyModifiableIterator();
    assertFalse(iterator.hasNext());
    assertThrows(NoSuchElementException.class, () -> iterator.next());
    assertThrows(IllegalStateException.class, () -> iterator.remove());
  }

  public void testSize0() {
    Iterator<String> iterator = emptyIterator();
    assertEquals(0, Iterators.size(iterator));
  }

  public void testSize1() {
    Iterator<Integer> iterator = singleton(0).iterator();
    assertEquals(1, Iterators.size(iterator));
  }

  public void testSize_partiallyConsumed() {
    Iterator<Integer> iterator = asList(1, 2, 3, 4, 5).iterator();
    iterator.next();
    iterator.next();
    assertEquals(3, Iterators.size(iterator));
  }

  public void test_contains_nonnull_yes() {
    Iterator<@Nullable String> set = Arrays.<@Nullable String>asList("a", null, "b").iterator();
    assertTrue(Iterators.contains(set, "b"));
  }

  public void test_contains_nonnull_no() {
    Iterator<String> set = asList("a", "b").iterator();
    assertFalse(Iterators.contains(set, "c"));
  }

  public void test_contains_null_yes() {
    Iterator<@Nullable String> set = Arrays.<@Nullable String>asList("a", null, "b").iterator();
    assertTrue(Iterators.contains(set, null));
  }

  public void test_contains_null_no() {
    Iterator<String> set = asList("a", "b").iterator();
    assertFalse(Iterators.contains(set, null));
  }

  public void testGetOnlyElement_noDefault_valid() {
    Iterator<String> iterator = singletonList("foo").iterator();
    assertEquals("foo", getOnlyElement(iterator));
  }

  public void testGetOnlyElement_noDefault_empty() {
    Iterator<String> iterator = emptyIterator();
    assertThrows(NoSuchElementException.class, () -> getOnlyElement(iterator));
  }

  public void testGetOnlyElement_noDefault_moreThanOneLessThanFiveElements() {
    Iterator<String> iterator = asList("one", "two").iterator();
    IllegalArgumentException expected =
        assertThrows(IllegalArgumentException.class, () -> getOnlyElement(iterator));
    assertThat(expected).hasMessageThat().isEqualTo("expected one element but was: <one, two>");
  }

  public void testGetOnlyElement_noDefault_fiveElements() {
    Iterator<String> iterator = asList("one", "two", "three", "four", "five").iterator();
    IllegalArgumentException expected =
        assertThrows(IllegalArgumentException.class, () -> getOnlyElement(iterator));
    assertThat(expected)
        .hasMessageThat()
        .isEqualTo("expected one element but was: <one, two, three, four, five>");
  }

  public void testGetOnlyElement_noDefault_moreThanFiveElements() {
    Iterator<String> iterator = asList("one", "two", "three", "four", "five", "six").iterator();
    IllegalArgumentException expected =
        assertThrows(IllegalArgumentException.class, () -> getOnlyElement(iterator));
    assertThat(expected)
        .hasMessageThat()
        .isEqualTo("expected one element but was: <one, two, three, four, five, ...>");
  }

  public void testGetOnlyElement_withDefault_singleton() {
    Iterator<String> iterator = singletonList("foo").iterator();
    assertEquals("foo", getOnlyElement(iterator, "bar"));
  }

  public void testGetOnlyElement_withDefault_empty() {
    Iterator<String> iterator = emptyIterator();
    assertEquals("bar", getOnlyElement(iterator, "bar"));
  }

  public void testGetOnlyElement_withDefault_empty_null() {
    Iterator<String> iterator = emptyIterator();
    assertNull(Iterators.<@Nullable String>getOnlyElement(iterator, null));
  }

  public void testGetOnlyElement_withDefault_two() {
    Iterator<String> iterator = asList("foo", "bar").iterator();
    IllegalArgumentException expected =
        assertThrows(IllegalArgumentException.class, () -> getOnlyElement(iterator, "x"));
    assertThat(expected).hasMessageThat().isEqualTo("expected one element but was: <foo, bar>");
  }

  @GwtIncompatible // Iterators.toArray(Iterator, Class)
  public void testToArrayEmpty() {
    Iterator<String> iterator = Collections.<String>emptyList().iterator();
    String[] array = Iterators.toArray(iterator, String.class);
    assertTrue(Arrays.equals(new String[0], array));
  }

  @GwtIncompatible // Iterators.toArray(Iterator, Class)
  public void testToArraySingleton() {
    Iterator<String> iterator = singletonList("a").iterator();
    String[] array = Iterators.toArray(iterator, String.class);
    assertTrue(Arrays.equals(new String[] {"a"}, array));
  }

  @GwtIncompatible // Iterators.toArray(Iterator, Class)
  public void testToArray() {
    String[] sourceArray = new String[] {"a", "b", "c"};
    Iterator<String> iterator = asList(sourceArray).iterator();
    String[] newArray = Iterators.toArray(iterator, String.class);
    assertTrue(Arrays.equals(sourceArray, newArray));
  }

  public void testFilterSimple() {
    Iterator<String> unfiltered = Lists.newArrayList("foo", "bar").iterator();
    Iterator<String> filtered = filter(unfiltered, equalTo("foo"));
    List<String> expected = singletonList("foo");
    List<String> actual = Lists.newArrayList(filtered);
    assertEquals(expected, actual);
  }

  public void testFilterNoMatch() {
    Iterator<String> unfiltered = Lists.newArrayList("foo", "bar").iterator();
    Iterator<String> filtered = filter(unfiltered, Predicates.alwaysFalse());
    List<String> expected = emptyList();
    List<String> actual = Lists.newArrayList(filtered);
    assertEquals(expected, actual);
  }

  public void testFilterMatchAll() {
    Iterator<String> unfiltered = Lists.newArrayList("foo", "bar").iterator();
    Iterator<String> filtered = filter(unfiltered, Predicates.alwaysTrue());
    List<String> expected = Lists.newArrayList("foo", "bar");
    List<String> actual = Lists.newArrayList(filtered);
    assertEquals(expected, actual);
  }

  public void testFilterNothing() {
    Iterator<String> unfiltered = Collections.<String>emptyList().iterator();
    Iterator<String> filtered =
        filter(
            unfiltered,
            new Predicate<String>() {
              @Override
              public boolean apply(String s) {
                throw new AssertionFailedError("Should never be evaluated");
              }
            });

    List<String> expected = emptyList();
    List<String> actual = Lists.newArrayList(filtered);
    assertEquals(expected, actual);
  }

  @GwtIncompatible // unreasonably slow
  public void testFilterUsingIteratorTester() {
    List<Integer> list = asList(1, 2, 3, 4, 5);
    Predicate<Integer> isEven =
        new Predicate<Integer>() {
          @Override
          public boolean apply(Integer integer) {
            return integer % 2 == 0;
          }
        };
    new IteratorTester<Integer>(
        5, UNMODIFIABLE, asList(2, 4), IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override
      protected Iterator<Integer> newTargetIterator() {
        return filter(list.iterator(), isEven);
      }
    }.test();
  }

  public void testAny() {
    List<String> list = new ArrayList<>();
    Predicate<String> predicate = equalTo("pants");

    assertFalse(any(list.iterator(), predicate));
    list.add("cool");
    assertFalse(any(list.iterator(), predicate));
    list.add("pants");
    assertTrue(any(list.iterator(), predicate));
  }

  public void testAll() {
    List<String> list = new ArrayList<>();
    Predicate<String> predicate = equalTo("cool");

    assertTrue(all(list.iterator(), predicate));
    list.add("cool");
    assertTrue(all(list.iterator(), predicate));
    list.add("pants");
    assertFalse(all(list.iterator(), predicate));
  }

  public void testFind_firstElement() {
    Iterable<String> list = Lists.newArrayList("cool", "pants");
    Iterator<String> iterator = list.iterator();
    assertEquals("cool", find(iterator, equalTo("cool")));
    assertEquals("pants", iterator.next());
  }

  public void testFind_lastElement() {
    Iterable<String> list = Lists.newArrayList("cool", "pants");
    Iterator<String> iterator = list.iterator();
    assertEquals("pants", find(iterator, equalTo("pants")));
    assertFalse(iterator.hasNext());
  }

  public void testFind_notPresent() {
    Iterable<String> list = Lists.newArrayList("cool", "pants");
    Iterator<String> iterator = list.iterator();
    assertThrows(NoSuchElementException.class, () -> find(iterator, Predicates.alwaysFalse()));
    assertFalse(iterator.hasNext());
  }

  public void testFind_matchAlways() {
    Iterable<String> list = Lists.newArrayList("cool", "pants");
    Iterator<String> iterator = list.iterator();
    assertEquals("cool", find(iterator, Predicates.alwaysTrue()));
  }

  public void testFind_withDefault_first() {
    Iterable<String> list = Lists.newArrayList("cool", "pants");
    Iterator<String> iterator = list.iterator();
    assertEquals("cool", find(iterator, equalTo("cool"), "woot"));
    assertEquals("pants", iterator.next());
  }

  public void testFind_withDefault_last() {
    Iterable<String> list = Lists.newArrayList("cool", "pants");
    Iterator<String> iterator = list.iterator();
    assertEquals("pants", find(iterator, equalTo("pants"), "woot"));
    assertFalse(iterator.hasNext());
  }

  public void testFind_withDefault_notPresent() {
    Iterable<String> list = Lists.newArrayList("cool", "pants");
    Iterator<String> iterator = list.iterator();
    assertEquals("woot", find(iterator, Predicates.alwaysFalse(), "woot"));
    assertFalse(iterator.hasNext());
  }

  public void testFind_withDefault_notPresent_nullReturn() {
    Iterable<String> list = Lists.newArrayList("cool", "pants");
    Iterator<String> iterator = list.iterator();
    assertNull(find(iterator, Predicates.alwaysFalse(), null));
    assertFalse(iterator.hasNext());
  }

  public void testFind_withDefault_matchAlways() {
    Iterable<String> list = Lists.newArrayList("cool", "pants");
    Iterator<String> iterator = list.iterator();
    assertEquals("cool", find(iterator, Predicates.alwaysTrue(), "woot"));
    assertEquals("pants", iterator.next());
  }

  public void testTryFind_firstElement() {
    Iterable<String> list = Lists.newArrayList("cool", "pants");
    Iterator<String> iterator = list.iterator();
    assertThat(tryFind(iterator, equalTo("cool"))).hasValue("cool");
  }

  public void testTryFind_lastElement() {
    Iterable<String> list = Lists.newArrayList("cool", "pants");
    Iterator<String> iterator = list.iterator();
    assertThat(tryFind(iterator, equalTo("pants"))).hasValue("pants");
  }

  public void testTryFind_alwaysTrue() {
    Iterable<String> list = Lists.newArrayList("cool", "pants");
    Iterator<String> iterator = list.iterator();
    assertThat(tryFind(iterator, Predicates.alwaysTrue())).hasValue("cool");
  }

  public void testTryFind_alwaysFalse_orDefault() {
    Iterable<String> list = Lists.newArrayList("cool", "pants");
    Iterator<String> iterator = list.iterator();
    assertEquals("woot", tryFind(iterator, Predicates.alwaysFalse()).or("woot"));
    assertFalse(iterator.hasNext());
  }

  public void testTryFind_alwaysFalse_isPresent() {
    Iterable<String> list = Lists.newArrayList("cool", "pants");
    Iterator<String> iterator = list.iterator();
    assertThat(tryFind(iterator, Predicates.alwaysFalse())).isAbsent();
    assertFalse(iterator.hasNext());
  }

  public void testTransform() {
    Iterator<String> input = asList("1", "2", "3").iterator();
    Iterator<Integer> result =
        Iterators.transform(
            input,
            new Function<String, Integer>() {
              @Override
              public Integer apply(String from) {
                return Integer.valueOf(from);
              }
            });

    List<Integer> actual = Lists.newArrayList(result);
    List<Integer> expected = asList(1, 2, 3);
    assertEquals(expected, actual);
  }

  public void testTransformRemove() {
    List<String> list = Lists.newArrayList("1", "2", "3");
    Iterator<String> input = list.iterator();
    Iterator<Integer> iterator =
        Iterators.transform(
            input,
            new Function<String, Integer>() {
              @Override
              public Integer apply(String from) {
                return Integer.valueOf(from);
              }
            });

    assertEquals(Integer.valueOf(1), iterator.next());
    assertEquals(Integer.valueOf(2), iterator.next());
    iterator.remove();
    assertEquals(asList("1", "3"), list);
  }

  public void testPoorlyBehavedTransform() {
    Iterator<String> input = asList("1", "not a number", "3").iterator();
    Iterator<Integer> result =
        Iterators.transform(
            input,
            new Function<String, Integer>() {
              @Override
              public Integer apply(String from) {
                return Integer.valueOf(from);
              }
            });

    result.next();
    assertThrows(NumberFormatException.class, () -> result.next());
  }

  public void testNullFriendlyTransform() {
    Iterator<@Nullable Integer> input = Arrays.<@Nullable Integer>asList(1, 2, null, 3).iterator();
    Iterator<String> result =
        Iterators.transform(
            input,
            new Function<@Nullable Integer, String>() {
              @Override
              public String apply(@Nullable Integer from) {
                return String.valueOf(from);
              }
            });

    List<String> actual = Lists.newArrayList(result);
    List<String> expected = asList("1", "2", "null", "3");
    assertEquals(expected, actual);
  }

  public void testCycleOfEmpty() {
    // "<String>" for javac 1.5.
    Iterator<String> cycle = Iterators.<String>cycle();
    assertFalse(cycle.hasNext());
  }

  public void testCycleOfOne() {
    Iterator<String> cycle = Iterators.cycle("a");
    for (int i = 0; i < 3; i++) {
      assertTrue(cycle.hasNext());
      assertEquals("a", cycle.next());
    }
  }

  public void testCycleOfOneWithRemove() {
    Iterable<String> iterable = Lists.newArrayList("a");
    Iterator<String> cycle = Iterators.cycle(iterable);
    assertTrue(cycle.hasNext());
    assertEquals("a", cycle.next());
    cycle.remove();
    assertEquals(emptyList(), iterable);
    assertFalse(cycle.hasNext());
  }

  public void testCycleOfTwo() {
    Iterator<String> cycle = Iterators.cycle("a", "b");
    for (int i = 0; i < 3; i++) {
      assertTrue(cycle.hasNext());
      assertEquals("a", cycle.next());
      assertTrue(cycle.hasNext());
      assertEquals("b", cycle.next());
    }
  }

  public void testCycleOfTwoWithRemove() {
    Iterable<String> iterable = Lists.newArrayList("a", "b");
    Iterator<String> cycle = Iterators.cycle(iterable);
    assertTrue(cycle.hasNext());
    assertEquals("a", cycle.next());
    assertTrue(cycle.hasNext());
    assertEquals("b", cycle.next());
    assertTrue(cycle.hasNext());
    assertEquals("a", cycle.next());
    cycle.remove();
    assertEquals(singletonList("b"), iterable);
    assertTrue(cycle.hasNext());
    assertEquals("b", cycle.next());
    assertTrue(cycle.hasNext());
    assertEquals("b", cycle.next());
    cycle.remove();
    assertEquals(emptyList(), iterable);
    assertFalse(cycle.hasNext());
  }

  public void testCycleRemoveWithoutNext() {
    Iterator<String> cycle = Iterators.cycle("a", "b");
    assertTrue(cycle.hasNext());
    assertThrows(IllegalStateException.class, () -> cycle.remove());
  }

  public void testCycleRemoveSameElementTwice() {
    Iterator<String> cycle = Iterators.cycle("a", "b");
    cycle.next();
    cycle.remove();
    assertThrows(IllegalStateException.class, () -> cycle.remove());
  }

  public void testCycleWhenRemoveIsNotSupported() {
    Iterable<String> iterable = asList("a", "b");
    Iterator<String> cycle = Iterators.cycle(iterable);
    cycle.next();
    assertThrows(UnsupportedOperationException.class, () -> cycle.remove());
  }

  public void testCycleRemoveAfterHasNext() {
    Iterable<String> iterable = Lists.newArrayList("a");
    Iterator<String> cycle = Iterators.cycle(iterable);
    assertTrue(cycle.hasNext());
    assertEquals("a", cycle.next());
    assertTrue(cycle.hasNext());
    cycle.remove();
    assertEquals(emptyList(), iterable);
    assertFalse(cycle.hasNext());
  }

  /** An Iterable whose Iterator is rigorous in checking for concurrent modification. */
  private static final class PickyIterable<E> implements Iterable<E> {
    final List<E> elements;
    int modCount = 0;

    PickyIterable(E... elements) {
      this.elements = new ArrayList<E>(asList(elements));
    }

    @Override
    public Iterator<E> iterator() {
      return new PickyIterator();
    }

    final class PickyIterator implements Iterator<E> {
      int expectedModCount = modCount;
      int index = 0;
      boolean canRemove;

      @Override
      public boolean hasNext() {
        checkConcurrentModification();
        return index < elements.size();
      }

      @Override
      public E next() {
        checkConcurrentModification();
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        canRemove = true;
        return elements.get(index++);
      }

      @Override
      public void remove() {
        checkConcurrentModification();
        checkRemove(canRemove);
        elements.remove(--index);
        expectedModCount = ++modCount;
        canRemove = false;
      }

      void checkConcurrentModification() {
        if (expectedModCount != modCount) {
          throw new ConcurrentModificationException();
        }
      }
    }
  }

  public void testCycleRemoveAfterHasNextExtraPicky() {
    PickyIterable<String> iterable = new PickyIterable<>("a");
    Iterator<String> cycle = Iterators.cycle(iterable);
    assertTrue(cycle.hasNext());
    assertEquals("a", cycle.next());
    assertTrue(cycle.hasNext());
    cycle.remove();
    assertTrue(iterable.elements.isEmpty());
    assertFalse(cycle.hasNext());
  }

  public void testCycleNoSuchElementException() {
    Iterable<String> iterable = Lists.newArrayList("a");
    Iterator<String> cycle = Iterators.cycle(iterable);
    assertTrue(cycle.hasNext());
    assertEquals("a", cycle.next());
    cycle.remove();
    assertFalse(cycle.hasNext());
    assertThrows(NoSuchElementException.class, () -> cycle.next());
  }

  @GwtIncompatible // unreasonably slow
  public void testCycleUsingIteratorTester() {
    new IteratorTester<Integer>(
        5,
        UNMODIFIABLE,
        asList(1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2),
        IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override
      protected Iterator<Integer> newTargetIterator() {
        return Iterators.cycle(asList(1, 2));
      }
    }.test();
  }

  @GwtIncompatible // slow (~5s)
  public void testConcatNoIteratorsYieldsEmpty() {
    new EmptyIteratorTester() {
      @Override
      protected Iterator<Integer> newTargetIterator() {
        return Iterators.concat();
      }
    }.test();
  }

  @GwtIncompatible // slow (~5s)
  public void testConcatOneEmptyIteratorYieldsEmpty() {
    new EmptyIteratorTester() {
      @Override
      protected Iterator<Integer> newTargetIterator() {
        return Iterators.concat(iterateOver());
      }
    }.test();
  }

  @GwtIncompatible // slow (~5s)
  public void testConcatMultipleEmptyIteratorsYieldsEmpty() {
    new EmptyIteratorTester() {
      @Override
      protected Iterator<Integer> newTargetIterator() {
        return Iterators.concat(iterateOver(), iterateOver());
      }
    }.test();
  }

  @GwtIncompatible // slow (~3s)
  public void testConcatSingletonYieldsSingleton() {
    new SingletonIteratorTester() {
      @Override
      protected Iterator<Integer> newTargetIterator() {
        return Iterators.concat(iterateOver(1));
      }
    }.test();
  }

  @GwtIncompatible // slow (~5s)
  public void testConcatEmptyAndSingletonAndEmptyYieldsSingleton() {
    new SingletonIteratorTester() {
      @Override
      protected Iterator<Integer> newTargetIterator() {
        return Iterators.concat(iterateOver(), iterateOver(1), iterateOver());
      }
    }.test();
  }

  @GwtIncompatible // fairly slow (~40s)
  public void testConcatSingletonAndSingletonYieldsDoubleton() {
    new DoubletonIteratorTester() {
      @Override
      protected Iterator<Integer> newTargetIterator() {
        return Iterators.concat(iterateOver(1), iterateOver(2));
      }
    }.test();
  }

  @GwtIncompatible // fairly slow (~40s)
  public void testConcatSingletonAndSingletonWithEmptiesYieldsDoubleton() {
    new DoubletonIteratorTester() {
      @Override
      protected Iterator<Integer> newTargetIterator() {
        return Iterators.concat(iterateOver(1), iterateOver(), iterateOver(), iterateOver(2));
      }
    }.test();
  }

  @GwtIncompatible // fairly slow (~50s)
  public void testConcatUnmodifiable() {
    new IteratorTester<Integer>(
        5, UNMODIFIABLE, asList(1, 2), IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override
      protected Iterator<Integer> newTargetIterator() {
        return Iterators.concat(
            asList(1).iterator(), Arrays.<Integer>asList().iterator(), asList(2).iterator());
      }
    }.test();
  }

  public void testConcatPartiallyAdvancedSecond() {
    Iterator<String> itr1 = Iterators.concat(singletonIterator("a"), Iterators.forArray("b", "c"));
    assertEquals("a", itr1.next());
    assertEquals("b", itr1.next());
    Iterator<String> itr2 = Iterators.concat(singletonIterator("d"), itr1);
    assertEquals("d", itr2.next());
    assertEquals("c", itr2.next());
  }

  public void testConcatPartiallyAdvancedFirst() {
    Iterator<String> itr1 = Iterators.concat(singletonIterator("a"), Iterators.forArray("b", "c"));
    assertEquals("a", itr1.next());
    assertEquals("b", itr1.next());
    Iterator<String> itr2 = Iterators.concat(itr1, singletonIterator("d"));
    assertEquals("c", itr2.next());
    assertEquals("d", itr2.next());
  }

  /** Illustrates the somewhat bizarre behavior when a null is passed in. */
  public void testConcatContainingNull() {
    Iterator<Iterator<Integer>> input =
        (Iterator<Iterator<Integer>>)
            Arrays.<@Nullable Iterator<Integer>>asList(iterateOver(1, 2), null, iterateOver(3))
                .iterator();
    Iterator<Integer> result = Iterators.concat(input);
    assertEquals(1, (int) result.next());
    assertEquals(2, (int) result.next());
    assertThrows(NullPointerException.class, () -> result.hasNext());
    assertThrows(NullPointerException.class, () -> result.next());
    // There is no way to get "through" to the 3.  Buh-bye
  }

  public void testConcatVarArgsContainingNull() {
    assertThrows(
        NullPointerException.class,
        () ->
            Iterators.concat(
                iterateOver(1, 2), null, iterateOver(3), iterateOver(4), iterateOver(5)));
  }

  public void testConcatNested_appendToEnd() {
    int nestingDepth = 128;
    Iterator<Integer> iterator = iterateOver();
    for (int i = 0; i < nestingDepth; i++) {
      iterator = Iterators.concat(iterator, iterateOver(1));
    }
    assertEquals(nestingDepth, Iterators.size(iterator));
  }

  public void testConcatNested_appendToBeginning() {
    int nestingDepth = 128;
    Iterator<Integer> iterator = iterateOver();
    for (int i = 0; i < nestingDepth; i++) {
      iterator = Iterators.concat(iterateOver(1), iterator);
    }
    assertEquals(nestingDepth, Iterators.size(iterator));
  }

  public void testAddAllWithEmptyIterator() {
    List<String> alreadyThere = Lists.newArrayList("already", "there");

    boolean changed = Iterators.addAll(alreadyThere, Iterators.<String>emptyIterator());
    assertThat(alreadyThere).containsExactly("already", "there").inOrder();
    assertFalse(changed);
  }

  public void testAddAllToList() {
    List<String> alreadyThere = Lists.newArrayList("already", "there");
    List<String> freshlyAdded = Lists.newArrayList("freshly", "added");

    boolean changed = Iterators.addAll(alreadyThere, freshlyAdded.iterator());

    assertThat(alreadyThere).containsExactly("already", "there", "freshly", "added");
    assertTrue(changed);
  }

  public void testAddAllToSet() {
    Set<String> alreadyThere = new LinkedHashSet<>(asList("already", "there"));
    List<String> oneMore = Lists.newArrayList("there");

    boolean changed = Iterators.addAll(alreadyThere, oneMore.iterator());
    assertThat(alreadyThere).containsExactly("already", "there").inOrder();
    assertFalse(changed);
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testNullPointerExceptions() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(Iterators.class);
  }

  @GwtIncompatible // Only used by @GwtIncompatible code
  private abstract static class EmptyIteratorTester extends IteratorTester<Integer> {
    EmptyIteratorTester() {
      super(3, MODIFIABLE, Collections.<Integer>emptySet(), IteratorTester.KnownOrder.KNOWN_ORDER);
    }
  }

  @GwtIncompatible // Only used by @GwtIncompatible code
  private abstract static class SingletonIteratorTester extends IteratorTester<Integer> {
    SingletonIteratorTester() {
      super(3, MODIFIABLE, singleton(1), IteratorTester.KnownOrder.KNOWN_ORDER);
    }
  }

  @GwtIncompatible // Only used by @GwtIncompatible code
  private abstract static class DoubletonIteratorTester extends IteratorTester<Integer> {
    DoubletonIteratorTester() {
      super(5, MODIFIABLE, newArrayList(1, 2), IteratorTester.KnownOrder.KNOWN_ORDER);
    }
  }

  private static Iterator<Integer> iterateOver(int... values) {
    // Note: Ints.asList's iterator does not support remove which we need for testing.
    return new ArrayList<>(Ints.asList(values)).iterator();
  }

  public void testElementsEqual() {
    Iterable<?> a;
    Iterable<?> b;

    // Base case.
    a = new ArrayList<>();
    b = emptySet();
    assertTrue(elementsEqual(a.iterator(), b.iterator()));

    // A few elements.
    a = asList(4, 8, 15, 16, 23, 42);
    b = asList(4, 8, 15, 16, 23, 42);
    assertTrue(elementsEqual(a.iterator(), b.iterator()));

    // The same, but with nulls.
    a = Arrays.<@Nullable Integer>asList(4, 8, null, 16, 23, 42);
    b = Arrays.<@Nullable Integer>asList(4, 8, null, 16, 23, 42);
    assertTrue(elementsEqual(a.iterator(), b.iterator()));

    // Different Iterable types (still equal elements, though).
    a = ImmutableList.of(4, 8, 15, 16, 23, 42);
    b = asList(4, 8, 15, 16, 23, 42);
    assertTrue(elementsEqual(a.iterator(), b.iterator()));

    // An element differs.
    a = asList(4, 8, 15, 12, 23, 42);
    b = asList(4, 8, 15, 16, 23, 42);
    assertFalse(elementsEqual(a.iterator(), b.iterator()));

    // null versus non-null.
    a = Arrays.<@Nullable Integer>asList(4, 8, 15, null, 23, 42);
    b = asList(4, 8, 15, 16, 23, 42);
    assertFalse(elementsEqual(a.iterator(), b.iterator()));
    assertFalse(elementsEqual(b.iterator(), a.iterator()));

    // Different lengths.
    a = asList(4, 8, 15, 16, 23);
    b = asList(4, 8, 15, 16, 23, 42);
    assertFalse(elementsEqual(a.iterator(), b.iterator()));
    assertFalse(elementsEqual(b.iterator(), a.iterator()));

    // Different lengths, one is empty.
    a = emptySet();
    b = asList(4, 8, 15, 16, 23, 42);
    assertFalse(elementsEqual(a.iterator(), b.iterator()));
    assertFalse(elementsEqual(b.iterator(), a.iterator()));
  }

  public void testPartition_badSize() {
    Iterator<Integer> source = singletonIterator(1);
    assertThrows(IllegalArgumentException.class, () -> Iterators.partition(source, 0));
  }

  public void testPartition_empty() {
    Iterator<Integer> source = emptyIterator();
    Iterator<List<Integer>> partitions = Iterators.partition(source, 1);
    assertFalse(partitions.hasNext());
  }

  public void testPartition_singleton1() {
    Iterator<Integer> source = singletonIterator(1);
    Iterator<List<Integer>> partitions = Iterators.partition(source, 1);
    assertTrue(partitions.hasNext());
    assertTrue(partitions.hasNext());
    assertEquals(ImmutableList.of(1), partitions.next());
    assertFalse(partitions.hasNext());
  }

  public void testPartition_singleton2() {
    Iterator<Integer> source = singletonIterator(1);
    Iterator<List<Integer>> partitions = Iterators.partition(source, 2);
    assertTrue(partitions.hasNext());
    assertTrue(partitions.hasNext());
    assertEquals(ImmutableList.of(1), partitions.next());
    assertFalse(partitions.hasNext());
  }

  @GwtIncompatible // fairly slow (~50s)
  public void testPartition_general() {
    new IteratorTester<List<Integer>>(
        5,
        IteratorFeature.UNMODIFIABLE,
        ImmutableList.of(asList(1, 2, 3), asList(4, 5, 6), asList(7)),
        IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override
      protected Iterator<List<Integer>> newTargetIterator() {
        Iterator<Integer> source = Iterators.forArray(1, 2, 3, 4, 5, 6, 7);
        return Iterators.partition(source, 3);
      }
    }.test();
  }

  public void testPartition_view() {
    List<Integer> list = asList(1, 2);
    Iterator<List<Integer>> partitions = Iterators.partition(list.iterator(), 1);

    // Changes before the partition is retrieved are reflected
    list.set(0, 3);
    List<Integer> first = partitions.next();

    // Changes after are not
    list.set(0, 4);

    assertEquals(ImmutableList.of(3), first);
  }

  @J2ktIncompatible // Arrays.asList(...).subList() doesn't implement RandomAccess in J2KT.
  @GwtIncompatible // Arrays.asList(...).subList() doesn't implement RandomAccess in GWT
  public void testPartitionRandomAccess() {
    Iterator<Integer> source = asList(1, 2, 3).iterator();
    Iterator<List<Integer>> partitions = Iterators.partition(source, 2);
    assertTrue(partitions.next() instanceof RandomAccess);
    assertTrue(partitions.next() instanceof RandomAccess);
  }

  public void testPaddedPartition_badSize() {
    Iterator<Integer> source = singletonIterator(1);
    assertThrows(IllegalArgumentException.class, () -> Iterators.paddedPartition(source, 0));
  }

  public void testPaddedPartition_empty() {
    Iterator<Integer> source = emptyIterator();
    Iterator<List<Integer>> partitions = Iterators.paddedPartition(source, 1);
    assertFalse(partitions.hasNext());
  }

  public void testPaddedPartition_singleton1() {
    Iterator<Integer> source = singletonIterator(1);
    Iterator<List<Integer>> partitions = Iterators.paddedPartition(source, 1);
    assertTrue(partitions.hasNext());
    assertTrue(partitions.hasNext());
    assertEquals(ImmutableList.of(1), partitions.next());
    assertFalse(partitions.hasNext());
  }

  public void testPaddedPartition_singleton2() {
    Iterator<Integer> source = singletonIterator(1);
    Iterator<List<Integer>> partitions = Iterators.paddedPartition(source, 2);
    assertTrue(partitions.hasNext());
    assertTrue(partitions.hasNext());
    assertEquals(Arrays.<@Nullable Integer>asList(1, null), partitions.next());
    assertFalse(partitions.hasNext());
  }

  @GwtIncompatible // fairly slow (~50s)
  public void testPaddedPartition_general() {
    ImmutableList<List<@Nullable Integer>> expectedElements =
        ImmutableList.of(
            asList(1, 2, 3), asList(4, 5, 6), Arrays.<@Nullable Integer>asList(7, null, null));
    new IteratorTester<List<Integer>>(
        5, IteratorFeature.UNMODIFIABLE, expectedElements, IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override
      protected Iterator<List<Integer>> newTargetIterator() {
        Iterator<Integer> source = Iterators.forArray(1, 2, 3, 4, 5, 6, 7);
        return Iterators.paddedPartition(source, 3);
      }
    }.test();
  }

  public void testPaddedPartition_view() {
    List<Integer> list = asList(1, 2);
    Iterator<List<Integer>> partitions = Iterators.paddedPartition(list.iterator(), 1);

    // Changes before the PaddedPartition is retrieved are reflected
    list.set(0, 3);
    List<Integer> first = partitions.next();

    // Changes after are not
    list.set(0, 4);

    assertEquals(ImmutableList.of(3), first);
  }

  public void testPaddedPartitionRandomAccess() {
    Iterator<Integer> source = asList(1, 2, 3).iterator();
    Iterator<List<Integer>> partitions = Iterators.paddedPartition(source, 2);
    assertTrue(partitions.next() instanceof RandomAccess);
    assertTrue(partitions.next() instanceof RandomAccess);
  }

  public void testForArrayEmpty() {
    String[] array = new String[0];
    Iterator<String> iterator = Iterators.forArray(array);
    assertFalse(iterator.hasNext());
    assertThrows(NoSuchElementException.class, () -> iterator.next());
    assertThrows(IndexOutOfBoundsException.class, () -> Iterators.forArrayWithPosition(array, 1));
  }

  @SuppressWarnings("DoNotCall")
  public void testForArrayTypical() {
    String[] array = {"foo", "bar"};
    Iterator<String> iterator = Iterators.forArray(array);
    assertTrue(iterator.hasNext());
    assertEquals("foo", iterator.next());
    assertTrue(iterator.hasNext());
    assertThrows(UnsupportedOperationException.class, () -> iterator.remove());
    assertEquals("bar", iterator.next());
    assertFalse(iterator.hasNext());
    assertThrows(NoSuchElementException.class, () -> iterator.next());
  }

  public void testForArrayWithPosition() {
    String[] array = {"foo", "bar", "cat"};
    Iterator<String> iterator = Iterators.forArrayWithPosition(array, 1);
    assertTrue(iterator.hasNext());
    assertEquals("bar", iterator.next());
    assertTrue(iterator.hasNext());
    assertEquals("cat", iterator.next());
    assertFalse(iterator.hasNext());
  }

  public void testForArrayLengthWithPositionBoundaryCases() {
    String[] array = {"foo", "bar"};
    assertFalse(Iterators.forArrayWithPosition(array, 2).hasNext());
    assertThrows(IndexOutOfBoundsException.class, () -> Iterators.forArrayWithPosition(array, -1));
    assertThrows(IndexOutOfBoundsException.class, () -> Iterators.forArrayWithPosition(array, 3));
  }

  @GwtIncompatible // unreasonably slow
  public void testForArrayUsingTester() {
    new IteratorTester<Integer>(
        6, UNMODIFIABLE, asList(1, 2, 3), IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override
      protected Iterator<Integer> newTargetIterator() {
        return Iterators.forArray(1, 2, 3);
      }
    }.test();
  }

  /*
   * TODO(cpovirk): Test forArray with ListIteratorTester (not just IteratorTester), including with
   * a start position other than 0.
   */

  public void testForEnumerationEmpty() {
    Enumeration<Integer> enumer = enumerate();
    Iterator<Integer> iter = Iterators.forEnumeration(enumer);

    assertFalse(iter.hasNext());
    assertThrows(NoSuchElementException.class, () -> iter.next());
  }

  @SuppressWarnings("DoNotCall")
  public void testForEnumerationSingleton() {
    Enumeration<Integer> enumer = enumerate(1);
    Iterator<Integer> iter = Iterators.forEnumeration(enumer);

    assertTrue(iter.hasNext());
    assertTrue(iter.hasNext());
    assertEquals(1, (int) iter.next());
    assertThrows(UnsupportedOperationException.class, () -> iter.remove());
    assertFalse(iter.hasNext());
    assertThrows(NoSuchElementException.class, () -> iter.next());
  }

  public void testForEnumerationTypical() {
    Enumeration<Integer> enumer = enumerate(1, 2, 3);
    Iterator<Integer> iter = Iterators.forEnumeration(enumer);

    assertTrue(iter.hasNext());
    assertEquals(1, (int) iter.next());
    assertTrue(iter.hasNext());
    assertEquals(2, (int) iter.next());
    assertTrue(iter.hasNext());
    assertEquals(3, (int) iter.next());
    assertFalse(iter.hasNext());
  }

  public void testAsEnumerationEmpty() {
    Iterator<Integer> iter = emptyIterator();
    Enumeration<Integer> enumer = Iterators.asEnumeration(iter);

    assertFalse(enumer.hasMoreElements());
    assertThrows(NoSuchElementException.class, () -> enumer.nextElement());
  }

  public void testAsEnumerationSingleton() {
    Iterator<Integer> iter = ImmutableList.of(1).iterator();
    Enumeration<Integer> enumer = Iterators.asEnumeration(iter);

    assertTrue(enumer.hasMoreElements());
    assertTrue(enumer.hasMoreElements());
    assertEquals(1, (int) enumer.nextElement());
    assertFalse(enumer.hasMoreElements());
    assertThrows(NoSuchElementException.class, () -> enumer.nextElement());
  }

  public void testAsEnumerationTypical() {
    Iterator<Integer> iter = ImmutableList.of(1, 2, 3).iterator();
    Enumeration<Integer> enumer = Iterators.asEnumeration(iter);

    assertTrue(enumer.hasMoreElements());
    assertEquals(1, (int) enumer.nextElement());
    assertTrue(enumer.hasMoreElements());
    assertEquals(2, (int) enumer.nextElement());
    assertTrue(enumer.hasMoreElements());
    assertEquals(3, (int) enumer.nextElement());
    assertFalse(enumer.hasMoreElements());
  }

  // We're testing our asEnumeration method against a known-good implementation.
  @SuppressWarnings("JdkObsolete")
  private static Enumeration<Integer> enumerate(int... ints) {
    Vector<Integer> vector = new Vector<>(Ints.asList(ints));
    return vector.elements();
  }

  public void testToString() {
    Iterator<String> iterator = Lists.newArrayList("yam", "bam", "jam", "ham").iterator();
    assertEquals("[yam, bam, jam, ham]", Iterators.toString(iterator));
  }

  public void testToStringWithNull() {
    Iterator<@Nullable String> iterator =
        Lists.<@Nullable String>newArrayList("hello", null, "world").iterator();
    assertEquals("[hello, null, world]", Iterators.toString(iterator));
  }

  public void testToStringEmptyIterator() {
    Iterator<String> iterator = Collections.<String>emptyList().iterator();
    assertEquals("[]", Iterators.toString(iterator));
  }

  @SuppressWarnings("JUnitIncompatibleType") // Fails with j2kt.
  public void testLimit() {
    List<String> list = new ArrayList<>();
    assertThrows(IllegalArgumentException.class, () -> Iterators.limit(list.iterator(), -1));

    assertFalse(Iterators.limit(list.iterator(), 0).hasNext());
    assertFalse(Iterators.limit(list.iterator(), 1).hasNext());

    list.add("cool");
    assertFalse(Iterators.limit(list.iterator(), 0).hasNext());
    assertEquals(list, newArrayList(Iterators.limit(list.iterator(), 1)));
    assertEquals(list, newArrayList(Iterators.limit(list.iterator(), 2)));

    list.add("pants");
    assertFalse(Iterators.limit(list.iterator(), 0).hasNext());
    assertEquals(ImmutableList.of("cool"), newArrayList(Iterators.limit(list.iterator(), 1)));
    assertEquals(list, newArrayList(Iterators.limit(list.iterator(), 2)));
    assertEquals(list, newArrayList(Iterators.limit(list.iterator(), 3)));
  }

  public void testLimitRemove() {
    List<String> list = new ArrayList<>();
    list.add("cool");
    list.add("pants");
    Iterator<String> iterator = Iterators.limit(list.iterator(), 1);
    iterator.next();
    iterator.remove();
    assertFalse(iterator.hasNext());
    assertEquals(1, list.size());
    assertEquals("pants", list.get(0));
  }

  @GwtIncompatible // fairly slow (~30s)
  public void testLimitUsingIteratorTester() {
    List<Integer> list = Lists.newArrayList(1, 2, 3, 4, 5);
    new IteratorTester<Integer>(
        5, MODIFIABLE, newArrayList(1, 2, 3), IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override
      protected Iterator<Integer> newTargetIterator() {
        return Iterators.limit(new ArrayList<>(list).iterator(), 3);
      }
    }.test();
  }

  public void testGetNext_withDefault_singleton() {
    Iterator<String> iterator = singletonList("foo").iterator();
    assertEquals("foo", Iterators.getNext(iterator, "bar"));
  }

  public void testGetNext_withDefault_empty() {
    Iterator<String> iterator = emptyIterator();
    assertEquals("bar", Iterators.getNext(iterator, "bar"));
  }

  public void testGetNext_withDefault_empty_null() {
    Iterator<String> iterator = emptyIterator();
    assertNull(Iterators.<@Nullable String>getNext(iterator, null));
  }

  public void testGetNext_withDefault_two() {
    Iterator<String> iterator = asList("foo", "bar").iterator();
    assertEquals("foo", Iterators.getNext(iterator, "x"));
  }

  public void testGetLast_basic() {
    List<String> list = new ArrayList<>();
    list.add("a");
    list.add("b");
    assertEquals("b", getLast(list.iterator()));
  }

  public void testGetLast_exception() {
    List<String> list = new ArrayList<>();
    assertThrows(NoSuchElementException.class, () -> getLast(list.iterator()));
  }

  public void testGetLast_withDefault_singleton() {
    Iterator<String> iterator = singletonList("foo").iterator();
    assertEquals("foo", Iterators.getLast(iterator, "bar"));
  }

  public void testGetLast_withDefault_empty() {
    Iterator<String> iterator = emptyIterator();
    assertEquals("bar", Iterators.getLast(iterator, "bar"));
  }

  public void testGetLast_withDefault_empty_null() {
    Iterator<String> iterator = emptyIterator();
    assertNull(Iterators.<@Nullable String>getLast(iterator, null));
  }

  public void testGetLast_withDefault_two() {
    Iterator<String> iterator = asList("foo", "bar").iterator();
    assertEquals("bar", Iterators.getLast(iterator, "x"));
  }

  public void testGet_basic() {
    List<String> list = new ArrayList<>();
    list.add("a");
    list.add("b");
    Iterator<String> iterator = list.iterator();
    assertEquals("b", get(iterator, 1));
    assertFalse(iterator.hasNext());
  }

  public void testGet_atSize() {
    List<String> list = new ArrayList<>();
    list.add("a");
    list.add("b");
    Iterator<String> iterator = list.iterator();
    assertThrows(IndexOutOfBoundsException.class, () -> get(iterator, 2));
    assertFalse(iterator.hasNext());
  }

  public void testGet_pastEnd() {
    List<String> list = new ArrayList<>();
    list.add("a");
    list.add("b");
    Iterator<String> iterator = list.iterator();
    assertThrows(IndexOutOfBoundsException.class, () -> get(iterator, 5));
    assertFalse(iterator.hasNext());
  }

  public void testGet_empty() {
    List<String> list = new ArrayList<>();
    Iterator<String> iterator = list.iterator();
    assertThrows(IndexOutOfBoundsException.class, () -> get(iterator, 0));
    assertFalse(iterator.hasNext());
  }

  public void testGet_negativeIndex() {
    List<String> list = newArrayList("a", "b", "c");
    Iterator<String> iterator = list.iterator();
    assertThrows(IndexOutOfBoundsException.class, () -> get(iterator, -1));
  }

  public void testGet_withDefault_basic() {
    List<String> list = new ArrayList<>();
    list.add("a");
    list.add("b");
    Iterator<String> iterator = list.iterator();
    assertEquals("a", get(iterator, 0, "c"));
    assertTrue(iterator.hasNext());
  }

  public void testGet_withDefault_atSize() {
    List<String> list = new ArrayList<>();
    list.add("a");
    list.add("b");
    Iterator<String> iterator = list.iterator();
    assertEquals("c", get(iterator, 2, "c"));
    assertFalse(iterator.hasNext());
  }

  public void testGet_withDefault_pastEnd() {
    List<String> list = new ArrayList<>();
    list.add("a");
    list.add("b");
    Iterator<String> iterator = list.iterator();
    assertEquals("c", get(iterator, 3, "c"));
    assertFalse(iterator.hasNext());
  }

  public void testGet_withDefault_negativeIndex() {
    List<String> list = new ArrayList<>();
    list.add("a");
    list.add("b");
    Iterator<String> iterator = list.iterator();
    assertThrows(IndexOutOfBoundsException.class, () -> get(iterator, -1, "c"));
    assertTrue(iterator.hasNext());
  }

  public void testAdvance_basic() {
    List<String> list = new ArrayList<>();
    list.add("a");
    list.add("b");
    Iterator<String> iterator = list.iterator();
    advance(iterator, 1);
    assertEquals("b", iterator.next());
  }

  public void testAdvance_pastEnd() {
    List<String> list = new ArrayList<>();
    list.add("a");
    list.add("b");
    Iterator<String> iterator = list.iterator();
    advance(iterator, 5);
    assertFalse(iterator.hasNext());
  }

  public void testAdvance_illegalArgument() {
    List<String> list = newArrayList("a", "b", "c");
    Iterator<String> iterator = list.iterator();
    assertThrows(IllegalArgumentException.class, () -> advance(iterator, -1));
  }

  public void testFrequency() {
    List<@Nullable String> list = newArrayList("a", null, "b", null, "a", null);
    assertEquals(2, frequency(list.iterator(), "a"));
    assertEquals(1, frequency(list.iterator(), "b"));
    assertEquals(0, frequency(list.iterator(), "c"));
    assertEquals(0, frequency(list.iterator(), 4.2));
    assertEquals(3, frequency(list.iterator(), null));
  }

  @GwtIncompatible // slow (~4s)
  public void testSingletonIterator() {
    new IteratorTester<Integer>(
        3, UNMODIFIABLE, singleton(1), IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override
      protected Iterator<Integer> newTargetIterator() {
        return singletonIterator(1);
      }
    }.test();
  }

  public void testRemoveAll() {
    List<String> list = newArrayList("a", "b", "c", "d", "e");
    assertTrue(Iterators.removeAll(list.iterator(), newArrayList("b", "d", "f")));
    assertEquals(newArrayList("a", "c", "e"), list);
    assertFalse(Iterators.removeAll(list.iterator(), newArrayList("x", "y", "z")));
    assertEquals(newArrayList("a", "c", "e"), list);
  }

  public void testRemoveIf() {
    List<String> list = newArrayList("a", "b", "c", "d", "e");
    assertTrue(
        Iterators.removeIf(
            list.iterator(),
            new Predicate<String>() {
              @Override
              public boolean apply(String s) {
                return s.equals("b") || s.equals("d") || s.equals("f");
              }
            }));
    assertEquals(newArrayList("a", "c", "e"), list);
    assertFalse(
        Iterators.removeIf(
            list.iterator(),
            new Predicate<String>() {
              @Override
              public boolean apply(String s) {
                return s.equals("x") || s.equals("y") || s.equals("z");
              }
            }));
    assertEquals(newArrayList("a", "c", "e"), list);
  }

  public void testRetainAll() {
    List<String> list = newArrayList("a", "b", "c", "d", "e");
    assertTrue(Iterators.retainAll(list.iterator(), newArrayList("b", "d", "f")));
    assertEquals(newArrayList("b", "d"), list);
    assertFalse(Iterators.retainAll(list.iterator(), newArrayList("b", "e", "d")));
    assertEquals(newArrayList("b", "d"), list);
  }

  @J2ktIncompatible
  @GwtIncompatible // ListTestSuiteBuilder
  @AndroidIncompatible // test-suite builders
  private static Test testsForRemoveAllAndRetainAll() {
    return ListTestSuiteBuilder.using(
            new TestStringListGenerator() {
              @Override
              public List<String> create(String[] elements) {
                List<String> delegate = newArrayList(elements);
                return new ForwardingList<String>() {
                  @Override
                  protected List<String> delegate() {
                    return delegate;
                  }

                  @Override
                  public boolean removeAll(Collection<?> c) {
                    return Iterators.removeAll(iterator(), c);
                  }

                  @Override
                  public boolean retainAll(Collection<?> c) {
                    return Iterators.retainAll(iterator(), c);
                  }
                };
              }
            })
        .named("ArrayList with Iterators.removeAll and retainAll")
        .withFeatures(
            ListFeature.GENERAL_PURPOSE, CollectionFeature.ALLOWS_NULL_VALUES, CollectionSize.ANY)
        .createTestSuite();
  }

  public void testConsumingIterator() {
    // Test data
    List<String> list = Lists.newArrayList("a", "b");

    // Test & Verify
    Iterator<String> consumingIterator = Iterators.consumingIterator(list.iterator());

    assertEquals("Iterators.consumingIterator(...)", consumingIterator.toString());

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
  public void testConsumingIterator_duelingIterators() {
    // Test data
    List<String> list = Lists.newArrayList("a", "b");

    // Test & Verify
    Iterator<String> i1 = Iterators.consumingIterator(list.iterator());
    Iterator<String> i2 = Iterators.consumingIterator(list.iterator());

    i1.next();
    assertThrows(ConcurrentModificationException.class, () -> i2.next());
  }

  public void testIndexOf_consumedData() {
    Iterator<String> iterator = Lists.newArrayList("manny", "mo", "jack").iterator();
    assertEquals(1, Iterators.indexOf(iterator, equalTo("mo")));
    assertEquals("jack", iterator.next());
    assertFalse(iterator.hasNext());
  }

  public void testIndexOf_consumedDataWithDuplicates() {
    Iterator<String> iterator = Lists.newArrayList("manny", "mo", "mo", "jack").iterator();
    assertEquals(1, Iterators.indexOf(iterator, equalTo("mo")));
    assertEquals("mo", iterator.next());
    assertEquals("jack", iterator.next());
    assertFalse(iterator.hasNext());
  }

  public void testIndexOf_consumedDataNoMatch() {
    Iterator<String> iterator = Lists.newArrayList("manny", "mo", "mo", "jack").iterator();
    assertEquals(-1, Iterators.indexOf(iterator, equalTo("bob")));
    assertFalse(iterator.hasNext());
  }

  @SuppressWarnings({"deprecation", "InlineMeInliner"}) // test of a deprecated method
  public void testUnmodifiableIteratorShortCircuit() {
    Iterator<String> mod = Lists.newArrayList("a", "b", "c").iterator();
    UnmodifiableIterator<String> unmod = Iterators.unmodifiableIterator(mod);
    assertNotSame(mod, unmod);
    assertSame(unmod, Iterators.unmodifiableIterator(unmod));
    assertSame(unmod, Iterators.unmodifiableIterator((Iterator<String>) unmod));
  }

  @SuppressWarnings({"deprecation", "InlineMeInliner"}) // test of a deprecated method
  public void testPeekingIteratorShortCircuit() {
    Iterator<String> nonpeek = Lists.newArrayList("a", "b", "c").iterator();
    PeekingIterator<String> peek = Iterators.peekingIterator(nonpeek);
    assertNotSame(peek, nonpeek);
    assertSame(peek, Iterators.peekingIterator(peek));
    assertSame(peek, Iterators.peekingIterator((Iterator<String>) peek));
  }
}

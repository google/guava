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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Iterables.unmodifiableIterable;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.testing.features.CollectionFeature.ALLOWS_NULL_QUERIES;
import static com.google.common.collect.testing.features.CollectionFeature.SERIALIZABLE;
import static java.lang.reflect.Proxy.newProxyInstance;
import static java.util.Arrays.asList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.ListTestSuiteBuilder;
import com.google.common.collect.testing.MinimalCollection;
import com.google.common.collect.testing.MinimalIterable;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.google.ListGenerators.BuilderAddAllListGenerator;
import com.google.common.collect.testing.google.ListGenerators.BuilderReversedListGenerator;
import com.google.common.collect.testing.google.ListGenerators.ImmutableListHeadSubListGenerator;
import com.google.common.collect.testing.google.ListGenerators.ImmutableListMiddleSubListGenerator;
import com.google.common.collect.testing.google.ListGenerators.ImmutableListOfGenerator;
import com.google.common.collect.testing.google.ListGenerators.ImmutableListTailSubListGenerator;
import com.google.common.collect.testing.google.ListGenerators.UnhashableElementsImmutableListGenerator;
import com.google.common.collect.testing.testers.ListHashCodeTester;
import com.google.common.testing.CollectorTester;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for {@link ImmutableList}.
 *
 * @author Kevin Bourrillion
 * @author George van den Driessche
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public class ImmutableListTest extends TestCase {

  @GwtIncompatible // suite
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(
        ListTestSuiteBuilder.using(new ImmutableListOfGenerator())
            .named("ImmutableList")
            .withFeatures(CollectionSize.ANY, SERIALIZABLE, ALLOWS_NULL_QUERIES)
            .createTestSuite());
    suite.addTest(
        ListTestSuiteBuilder.using(new BuilderAddAllListGenerator())
            .named("ImmutableList, built with Builder.add")
            .withFeatures(CollectionSize.ANY, SERIALIZABLE, ALLOWS_NULL_QUERIES)
            .createTestSuite());
    suite.addTest(
        ListTestSuiteBuilder.using(new BuilderAddAllListGenerator())
            .named("ImmutableList, built with Builder.addAll")
            .withFeatures(CollectionSize.ANY, SERIALIZABLE, ALLOWS_NULL_QUERIES)
            .createTestSuite());
    suite.addTest(
        ListTestSuiteBuilder.using(new BuilderReversedListGenerator())
            .named("ImmutableList, reversed")
            .withFeatures(CollectionSize.ANY, SERIALIZABLE, ALLOWS_NULL_QUERIES)
            .createTestSuite());
    suite.addTest(
        ListTestSuiteBuilder.using(new ImmutableListHeadSubListGenerator())
            .named("ImmutableList, head subList")
            .withFeatures(CollectionSize.ANY, SERIALIZABLE, ALLOWS_NULL_QUERIES)
            .createTestSuite());
    suite.addTest(
        ListTestSuiteBuilder.using(new ImmutableListTailSubListGenerator())
            .named("ImmutableList, tail subList")
            .withFeatures(CollectionSize.ANY, SERIALIZABLE, ALLOWS_NULL_QUERIES)
            .createTestSuite());
    suite.addTest(
        ListTestSuiteBuilder.using(new ImmutableListMiddleSubListGenerator())
            .named("ImmutableList, middle subList")
            .withFeatures(CollectionSize.ANY, SERIALIZABLE, ALLOWS_NULL_QUERIES)
            .createTestSuite());
    suite.addTest(
        ListTestSuiteBuilder.using(new UnhashableElementsImmutableListGenerator())
            .suppressing(ListHashCodeTester.getHashCodeMethod())
            .named("ImmutableList, unhashable values")
            .withFeatures(CollectionSize.ANY, ALLOWS_NULL_QUERIES)
            .createTestSuite());
    return suite;
  }

  public static class CreationTests extends TestCase {
    public void testCreation_noArgs() {
      List<String> list = ImmutableList.of();
      assertEquals(Collections.emptyList(), list);
    }

    public void testCreation_oneElement() {
      List<String> list = ImmutableList.of("a");
      assertEquals(Collections.singletonList("a"), list);
    }

    public void testCreation_twoElements() {
      List<String> list = ImmutableList.of("a", "b");
      assertEquals(Lists.newArrayList("a", "b"), list);
    }

    public void testCreation_threeElements() {
      List<String> list = ImmutableList.of("a", "b", "c");
      assertEquals(Lists.newArrayList("a", "b", "c"), list);
    }

    public void testCreation_fourElements() {
      List<String> list = ImmutableList.of("a", "b", "c", "d");
      assertEquals(Lists.newArrayList("a", "b", "c", "d"), list);
    }

    public void testCreation_fiveElements() {
      List<String> list = ImmutableList.of("a", "b", "c", "d", "e");
      assertEquals(Lists.newArrayList("a", "b", "c", "d", "e"), list);
    }

    public void testCreation_sixElements() {
      List<String> list = ImmutableList.of("a", "b", "c", "d", "e", "f");
      assertEquals(Lists.newArrayList("a", "b", "c", "d", "e", "f"), list);
    }

    public void testCreation_sevenElements() {
      List<String> list = ImmutableList.of("a", "b", "c", "d", "e", "f", "g");
      assertEquals(Lists.newArrayList("a", "b", "c", "d", "e", "f", "g"), list);
    }

    public void testCreation_eightElements() {
      List<String> list = ImmutableList.of("a", "b", "c", "d", "e", "f", "g", "h");
      assertEquals(Lists.newArrayList("a", "b", "c", "d", "e", "f", "g", "h"), list);
    }

    public void testCreation_nineElements() {
      List<String> list = ImmutableList.of("a", "b", "c", "d", "e", "f", "g", "h", "i");
      assertEquals(Lists.newArrayList("a", "b", "c", "d", "e", "f", "g", "h", "i"), list);
    }

    public void testCreation_tenElements() {
      List<String> list = ImmutableList.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
      assertEquals(Lists.newArrayList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"), list);
    }

    public void testCreation_elevenElements() {
      List<String> list = ImmutableList.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k");
      assertEquals(Lists.newArrayList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k"), list);
    }

    // Varargs versions

    public void testCreation_twelveElements() {
      List<String> list =
          ImmutableList.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l");
      assertEquals(
          Lists.newArrayList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l"), list);
    }

    public void testCreation_thirteenElements() {
      List<String> list =
          ImmutableList.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m");
      assertEquals(
          Lists.newArrayList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m"),
          list);
    }

    public void testCreation_fourteenElements() {
      List<String> list =
          ImmutableList.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n");
      assertEquals(
          Lists.newArrayList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n"),
          list);
    }

    public void testCreation_singletonNull() {
      try {
        ImmutableList.of((String) null);
        fail();
      } catch (NullPointerException expected) {
      }
    }

    public void testCreation_withNull() {
      try {
        ImmutableList.of("a", null, "b");
        fail();
      } catch (NullPointerException expected) {
      }
    }

    public void testCreation_generic() {
      List<String> a = ImmutableList.of("a");
      // only verify that there is no compile warning
      ImmutableList<List<String>> unused = ImmutableList.of(a, a);
    }

    public void testCreation_arrayOfArray() {
      String[] array = new String[] {"a"};
      List<String[]> list = ImmutableList.<String[]>of(array);
      assertEquals(Collections.singletonList(array), list);
    }

    public void testCopyOf_emptyArray() {
      String[] array = new String[0];
      List<String> list = ImmutableList.copyOf(array);
      assertEquals(Collections.emptyList(), list);
    }

    public void testCopyOf_arrayOfOneElement() {
      String[] array = new String[] {"a"};
      List<String> list = ImmutableList.copyOf(array);
      assertEquals(Collections.singletonList("a"), list);
    }

    public void testCopyOf_nullArray() {
      try {
        ImmutableList.copyOf((String[]) null);
        fail();
      } catch (NullPointerException expected) {
      }
    }

    public void testCopyOf_arrayContainingOnlyNull() {
      String[] array = new String[] {null};
      try {
        ImmutableList.copyOf(array);
        fail();
      } catch (NullPointerException expected) {
      }
    }

    public void testCopyOf_collection_empty() {
      // "<String>" is required to work around a javac 1.5 bug.
      Collection<String> c = MinimalCollection.<String>of();
      List<String> list = ImmutableList.copyOf(c);
      assertEquals(Collections.emptyList(), list);
    }

    public void testCopyOf_collection_oneElement() {
      Collection<String> c = MinimalCollection.of("a");
      List<String> list = ImmutableList.copyOf(c);
      assertEquals(Collections.singletonList("a"), list);
    }

    public void testCopyOf_collection_general() {
      Collection<String> c = MinimalCollection.of("a", "b", "a");
      List<String> list = ImmutableList.copyOf(c);
      assertEquals(asList("a", "b", "a"), list);
      List<String> mutableList = asList("a", "b");
      list = ImmutableList.copyOf(mutableList);
      mutableList.set(0, "c");
      assertEquals(asList("a", "b"), list);
    }

    public void testCopyOf_collectionContainingNull() {
      Collection<String> c = MinimalCollection.of("a", null, "b");
      try {
        ImmutableList.copyOf(c);
        fail();
      } catch (NullPointerException expected) {
      }
    }

    public void testCopyOf_iterator_empty() {
      Iterator<String> iterator = Iterators.emptyIterator();
      List<String> list = ImmutableList.copyOf(iterator);
      assertEquals(Collections.emptyList(), list);
    }

    public void testCopyOf_iterator_oneElement() {
      Iterator<String> iterator = Iterators.singletonIterator("a");
      List<String> list = ImmutableList.copyOf(iterator);
      assertEquals(Collections.singletonList("a"), list);
    }

    public void testCopyOf_iterator_general() {
      Iterator<String> iterator = asList("a", "b", "a").iterator();
      List<String> list = ImmutableList.copyOf(iterator);
      assertEquals(asList("a", "b", "a"), list);
    }

    public void testCopyOf_iteratorContainingNull() {
      Iterator<String> iterator = asList("a", null, "b").iterator();
      try {
        ImmutableList.copyOf(iterator);
        fail();
      } catch (NullPointerException expected) {
      }
    }

    public void testCopyOf_iteratorNull() {
      try {
        ImmutableList.copyOf((Iterator<String>) null);
        fail();
      } catch (NullPointerException expected) {
      }
    }

    public void testCopyOf_concurrentlyMutating() {
      List<String> sample = Lists.newArrayList("a", "b", "c");
      for (int delta : new int[] {-1, 0, 1}) {
        for (int i = 0; i < sample.size(); i++) {
          Collection<String> misleading = Helpers.misleadingSizeCollection(delta);
          List<String> expected = sample.subList(0, i);
          misleading.addAll(expected);
          assertEquals(expected, ImmutableList.copyOf(misleading));
          assertEquals(expected, ImmutableList.copyOf((Iterable<String>) misleading));
        }
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
      List<String> list = ImmutableList.copyOf(iterable);
      assertEquals(asList("a", "b", "a"), list);
    }

    public void testCopyOf_plainIterable_iteratesOnce() {
      CountingIterable iterable = new CountingIterable();
      ImmutableList<String> unused = ImmutableList.copyOf(iterable);
      assertEquals(1, iterable.count);
    }

    public void testCopyOf_shortcut_empty() {
      Collection<String> c = ImmutableList.of();
      assertSame(c, ImmutableList.copyOf(c));
    }

    public void testCopyOf_shortcut_singleton() {
      Collection<String> c = ImmutableList.of("a");
      assertSame(c, ImmutableList.copyOf(c));
    }

    public void testCopyOf_shortcut_immutableList() {
      Collection<String> c = ImmutableList.of("a", "b", "c");
      assertSame(c, ImmutableList.copyOf(c));
    }

    public void testBuilderAddArrayHandlesNulls() {
      String[] elements = {"a", null, "b"};
      ImmutableList.Builder<String> builder = ImmutableList.builder();
      try {
        builder.add(elements);
        fail("Expected NullPointerException");
      } catch (NullPointerException expected) {
      }
      ImmutableList<String> result = builder.build();

      /*
       * Maybe it rejects all elements, or maybe it adds "a" before failing.
       * Either way is fine with us.
       */
      if (result.isEmpty()) {
        return;
      }
      assertTrue(ImmutableList.of("a").equals(result));
      assertEquals(1, result.size());
    }

    public void testBuilderAddCollectionHandlesNulls() {
      List<String> elements = Arrays.asList("a", null, "b");
      ImmutableList.Builder<String> builder = ImmutableList.builder();
      try {
        builder.addAll(elements);
        fail("Expected NullPointerException");
      } catch (NullPointerException expected) {
      }
      ImmutableList<String> result = builder.build();
      assertEquals(ImmutableList.of("a"), result);
      assertEquals(1, result.size());
    }

    public void testSortedCopyOf_natural() {
      Collection<Integer> c = MinimalCollection.of(4, 16, 10, -1, 5);
      ImmutableList<Integer> list = ImmutableList.sortedCopyOf(c);
      assertEquals(asList(-1, 4, 5, 10, 16), list);
    }

    public void testSortedCopyOf_natural_empty() {
      Collection<Integer> c = MinimalCollection.of();
      ImmutableList<Integer> list = ImmutableList.sortedCopyOf(c);
      assertEquals(asList(), list);
    }

    public void testSortedCopyOf_natural_singleton() {
      Collection<Integer> c = MinimalCollection.of(100);
      ImmutableList<Integer> list = ImmutableList.sortedCopyOf(c);
      assertEquals(asList(100), list);
    }

    public void testSortedCopyOf_natural_containsNull() {
      Collection<Integer> c = MinimalCollection.of(1, 3, null, 2);
      try {
        ImmutableList.sortedCopyOf(c);
        fail("Expected NPE");
      } catch (NullPointerException expected) {
      }
    }

    public void testSortedCopyOf() {
      Collection<String> c = MinimalCollection.of("a", "b", "A", "c");
      List<String> list = ImmutableList.sortedCopyOf(String.CASE_INSENSITIVE_ORDER, c);
      assertEquals(asList("a", "A", "b", "c"), list);
    }

    public void testSortedCopyOf_empty() {
      Collection<String> c = MinimalCollection.of();
      List<String> list = ImmutableList.sortedCopyOf(String.CASE_INSENSITIVE_ORDER, c);
      assertEquals(asList(), list);
    }

    public void testSortedCopyOf_singleton() {
      Collection<String> c = MinimalCollection.of("a");
      List<String> list = ImmutableList.sortedCopyOf(String.CASE_INSENSITIVE_ORDER, c);
      assertEquals(asList("a"), list);
    }

    public void testSortedCopyOf_containsNull() {
      Collection<String> c = MinimalCollection.of("a", "b", "A", null, "c");
      try {
        ImmutableList.sortedCopyOf(String.CASE_INSENSITIVE_ORDER, c);
        fail("Expected NPE");
      } catch (NullPointerException expected) {
      }
    }

    public void testToImmutableList() {
      CollectorTester.of(ImmutableList.<String>toImmutableList())
          .expectCollects(ImmutableList.of("a", "b", "c", "d"), "a", "b", "c", "d");
    }
  }

  @GwtIncompatible // reflection
  public static class ConcurrentTests extends TestCase {
    enum WrapWithIterable {
      WRAP,
      NO_WRAP
    }

    private static void runConcurrentlyMutatedTest(
        Collection<Integer> initialContents,
        Iterable<ListFrobber> actionsToPerformConcurrently,
        WrapWithIterable wrap) {
      ConcurrentlyMutatedList<Integer> concurrentlyMutatedList =
          newConcurrentlyMutatedList(initialContents, actionsToPerformConcurrently);

      Iterable<Integer> iterableToCopy =
          wrap == WrapWithIterable.WRAP
              ? unmodifiableIterable(concurrentlyMutatedList)
              : concurrentlyMutatedList;

      ImmutableList<Integer> copyOfIterable = ImmutableList.copyOf(iterableToCopy);

      assertTrue(concurrentlyMutatedList.getAllStates().contains(copyOfIterable));
    }

    private static void runConcurrentlyMutatedTest(WrapWithIterable wrap) {
      /*
       * TODO: Iterate over many array sizes and all possible operation lists,
       * performing adds and removes in different ways.
       */
      runConcurrentlyMutatedTest(elements(), ops(add(1), add(2)), wrap);

      runConcurrentlyMutatedTest(elements(), ops(add(1), nop()), wrap);

      runConcurrentlyMutatedTest(elements(), ops(add(1), remove()), wrap);

      runConcurrentlyMutatedTest(elements(), ops(nop(), add(1)), wrap);

      runConcurrentlyMutatedTest(elements(1), ops(remove(), nop()), wrap);

      runConcurrentlyMutatedTest(elements(1), ops(remove(), add(2)), wrap);

      runConcurrentlyMutatedTest(elements(1, 2), ops(remove(), remove()), wrap);

      runConcurrentlyMutatedTest(elements(1, 2), ops(remove(), nop()), wrap);

      runConcurrentlyMutatedTest(elements(1, 2), ops(remove(), add(3)), wrap);

      runConcurrentlyMutatedTest(elements(1, 2), ops(nop(), remove()), wrap);

      runConcurrentlyMutatedTest(elements(1, 2, 3), ops(remove(), remove()), wrap);
    }

    private static ImmutableList<Integer> elements(Integer... elements) {
      return ImmutableList.copyOf(elements);
    }

    private static ImmutableList<ListFrobber> ops(ListFrobber... elements) {
      return ImmutableList.copyOf(elements);
    }

    public void testCopyOf_concurrentlyMutatedList() {
      runConcurrentlyMutatedTest(WrapWithIterable.NO_WRAP);
    }

    public void testCopyOf_concurrentlyMutatedIterable() {
      runConcurrentlyMutatedTest(WrapWithIterable.WRAP);
    }

    /** An operation to perform on a list. */
    interface ListFrobber {
      void perform(List<Integer> list);
    }

    static ListFrobber add(final int element) {
      return new ListFrobber() {
        @Override
        public void perform(List<Integer> list) {
          list.add(0, element);
        }
      };
    }

    static ListFrobber remove() {
      return new ListFrobber() {
        @Override
        public void perform(List<Integer> list) {
          list.remove(0);
        }
      };
    }

    static ListFrobber nop() {
      return new ListFrobber() {
        @Override
        public void perform(List<Integer> list) {}
      };
    }

    /** A list that mutates itself after every call to each of its {@link List} methods. */
    interface ConcurrentlyMutatedList<E> extends List<E> {
      /**
       * The elements of a {@link ConcurrentlyMutatedList} are added and removed over time. This
       * method returns every state that the list has passed through at some point.
       */
      Set<List<E>> getAllStates();
    }

    /**
     * Returns a {@link ConcurrentlyMutatedList} that performs the given operations as its
     * concurrent modifications. The mutations occur in the same thread as the triggering method
     * call.
     */
    private static ConcurrentlyMutatedList<Integer> newConcurrentlyMutatedList(
        final Collection<Integer> initialContents,
        final Iterable<ListFrobber> actionsToPerformConcurrently) {
      InvocationHandler invocationHandler =
          new InvocationHandler() {
            final CopyOnWriteArrayList<Integer> delegate =
                new CopyOnWriteArrayList<>(initialContents);

            final Method getAllStatesMethod =
                getOnlyElement(asList(ConcurrentlyMutatedList.class.getDeclaredMethods()));

            final Iterator<ListFrobber> remainingActions = actionsToPerformConcurrently.iterator();

            final Set<List<Integer>> allStates = newHashSet();

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
              return method.equals(getAllStatesMethod)
                  ? getAllStates()
                  : invokeListMethod(method, args);
            }

            private Set<List<Integer>> getAllStates() {
              return allStates;
            }

            private Object invokeListMethod(Method method, Object[] args) throws Throwable {
              try {
                Object returnValue = method.invoke(delegate, args);
                mutateDelegate();
                return returnValue;
              } catch (InvocationTargetException e) {
                throw e.getCause();
              } catch (IllegalAccessException e) {
                throw new AssertionError(e);
              }
            }

            private void mutateDelegate() {
              allStates.add(ImmutableList.copyOf(delegate));
              remainingActions.next().perform(delegate);
              allStates.add(ImmutableList.copyOf(delegate));
            }
          };

      @SuppressWarnings("unchecked")
      ConcurrentlyMutatedList<Integer> list =
          (ConcurrentlyMutatedList<Integer>)
              newProxyInstance(
                  ImmutableListTest.CreationTests.class.getClassLoader(),
                  new Class[] {ConcurrentlyMutatedList.class},
                  invocationHandler);
      return list;
    }
  }

  public static class BasicTests extends TestCase {

    @GwtIncompatible // NullPointerTester
    public void testNullPointers() {
      NullPointerTester tester = new NullPointerTester();
      tester.testAllPublicStaticMethods(ImmutableList.class);
      tester.testAllPublicInstanceMethods(ImmutableList.of(1, 2, 3));
    }

    @GwtIncompatible // SerializableTester
    public void testSerialization_empty() {
      Collection<String> c = ImmutableList.of();
      assertSame(c, SerializableTester.reserialize(c));
    }

    @GwtIncompatible // SerializableTester
    public void testSerialization_singleton() {
      Collection<String> c = ImmutableList.of("a");
      SerializableTester.reserializeAndAssert(c);
    }

    @GwtIncompatible // SerializableTester
    public void testSerialization_multiple() {
      Collection<String> c = ImmutableList.of("a", "b", "c");
      SerializableTester.reserializeAndAssert(c);
    }

    public void testEquals_immutableList() {
      Collection<String> c = ImmutableList.of("a", "b", "c");
      assertTrue(c.equals(ImmutableList.of("a", "b", "c")));
      assertFalse(c.equals(ImmutableList.of("a", "c", "b")));
      assertFalse(c.equals(ImmutableList.of("a", "b")));
      assertFalse(c.equals(ImmutableList.of("a", "b", "c", "d")));
    }

    public void testBuilderAdd() {
      ImmutableList<String> list =
          new ImmutableList.Builder<String>().add("a").add("b").add("a").add("c").build();
      assertEquals(asList("a", "b", "a", "c"), list);
    }

    @GwtIncompatible("Builder impl")
    public void testBuilderForceCopy() {
      ImmutableList.Builder<Integer> builder = ImmutableList.builder();
      Object[] prevArray = null;
      for (int i = 0; i < 10; i++) {
        builder.add(i);
        assertNotSame(builder.contents, prevArray);
        prevArray = builder.contents;
        ImmutableList<Integer> unused = builder.build();
      }
    }

    @GwtIncompatible
    public void testBuilderExactlySizedReusesArray() {
      ImmutableList.Builder<Integer> builder = ImmutableList.builderWithExpectedSize(10);
      Object[] builderArray = builder.contents;
      for (int i = 0; i < 10; i++) {
        builder.add(i);
      }
      Object[] builderArrayAfterAdds = builder.contents;
      RegularImmutableList<Integer> list = (RegularImmutableList<Integer>) builder.build();
      Object[] listInternalArray = list.array;
      assertSame(builderArray, builderArrayAfterAdds);
      assertSame(builderArray, listInternalArray);
    }

    public void testBuilderAdd_varargs() {
      ImmutableList<String> list =
          new ImmutableList.Builder<String>().add("a", "b", "a", "c").build();
      assertEquals(asList("a", "b", "a", "c"), list);
    }

    public void testBuilderAddAll_iterable() {
      List<String> a = asList("a", "b");
      List<String> b = asList("c", "d");
      ImmutableList<String> list = new ImmutableList.Builder<String>().addAll(a).addAll(b).build();
      assertEquals(asList("a", "b", "c", "d"), list);
      b.set(0, "f");
      assertEquals(asList("a", "b", "c", "d"), list);
    }

    public void testBuilderAddAll_iterator() {
      List<String> a = asList("a", "b");
      List<String> b = asList("c", "d");
      ImmutableList<String> list =
          new ImmutableList.Builder<String>().addAll(a.iterator()).addAll(b.iterator()).build();
      assertEquals(asList("a", "b", "c", "d"), list);
      b.set(0, "f");
      assertEquals(asList("a", "b", "c", "d"), list);
    }

    public void testComplexBuilder() {
      List<Integer> colorElem = asList(0x00, 0x33, 0x66, 0x99, 0xCC, 0xFF);
      ImmutableList.Builder<Integer> webSafeColorsBuilder = ImmutableList.builder();
      for (Integer red : colorElem) {
        for (Integer green : colorElem) {
          for (Integer blue : colorElem) {
            webSafeColorsBuilder.add((red << 16) + (green << 8) + blue);
          }
        }
      }
      ImmutableList<Integer> webSafeColors = webSafeColorsBuilder.build();
      assertEquals(216, webSafeColors.size());
      Integer[] webSafeColorArray = webSafeColors.toArray(new Integer[webSafeColors.size()]);
      assertEquals(0x000000, (int) webSafeColorArray[0]);
      assertEquals(0x000033, (int) webSafeColorArray[1]);
      assertEquals(0x000066, (int) webSafeColorArray[2]);
      assertEquals(0x003300, (int) webSafeColorArray[6]);
      assertEquals(0x330000, (int) webSafeColorArray[36]);
      assertEquals(0x000066, (int) webSafeColors.get(2));
      assertEquals(0x003300, (int) webSafeColors.get(6));
      ImmutableList<Integer> addedColor = webSafeColorsBuilder.add(0x00BFFF).build();
      assertEquals(
          "Modifying the builder should not have changed any already" + " built sets",
          216,
          webSafeColors.size());
      assertEquals("the new array should be one bigger than webSafeColors", 217, addedColor.size());
      Integer[] appendColorArray = addedColor.toArray(new Integer[addedColor.size()]);
      assertEquals(0x00BFFF, (int) appendColorArray[216]);
    }

    public void testBuilderAddHandlesNullsCorrectly() {
      ImmutableList.Builder<String> builder = ImmutableList.builder();
      try {
        builder.add((String) null);
        fail("expected NullPointerException");
      } catch (NullPointerException expected) {
      }

      try {
        builder.add((String[]) null);
        fail("expected NullPointerException");
      } catch (NullPointerException expected) {
      }

      try {
        builder.add("a", null, "b");
        fail("expected NullPointerException");
      } catch (NullPointerException expected) {
      }
    }

    public void testBuilderAddAllHandlesNullsCorrectly() {
      ImmutableList.Builder<String> builder = ImmutableList.builder();
      try {
        builder.addAll((Iterable<String>) null);
        fail("expected NullPointerException");
      } catch (NullPointerException expected) {
      }

      try {
        builder.addAll((Iterator<String>) null);
        fail("expected NullPointerException");
      } catch (NullPointerException expected) {
      }

      builder = ImmutableList.builder();
      List<String> listWithNulls = asList("a", null, "b");
      try {
        builder.addAll(listWithNulls);
        fail("expected NullPointerException");
      } catch (NullPointerException expected) {
      }

      builder = ImmutableList.builder();
      Iterator<String> iteratorWithNulls = asList("a", null, "b").iterator();
      try {
        builder.addAll(iteratorWithNulls);
        fail("expected NullPointerException");
      } catch (NullPointerException expected) {
      }

      Iterable<String> iterableWithNulls = MinimalIterable.of("a", null, "b");
      try {
        builder.addAll(iterableWithNulls);
        fail("expected NullPointerException");
      } catch (NullPointerException expected) {
      }
    }

    public void testAsList() {
      ImmutableList<String> list = ImmutableList.of("a", "b");
      assertSame(list, list.asList());
    }
  }
}

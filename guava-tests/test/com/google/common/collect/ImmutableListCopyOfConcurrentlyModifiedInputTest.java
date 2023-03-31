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
import static java.lang.reflect.Proxy.newProxyInstance;
import static java.util.Arrays.asList;

import com.google.common.annotations.GwtIncompatible;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import junit.framework.TestCase;

@GwtIncompatible // reflection
public class ImmutableListCopyOfConcurrentlyModifiedInputTest extends TestCase {
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
   * Returns a {@link ConcurrentlyMutatedList} that performs the given operations as its concurrent
   * modifications. The mutations occur in the same thread as the triggering method call.
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
                ImmutableListCopyOfConcurrentlyModifiedInputTest.class.getClassLoader(),
                new Class[] {ConcurrentlyMutatedList.class},
                invocationHandler);
    return list;
  }
}

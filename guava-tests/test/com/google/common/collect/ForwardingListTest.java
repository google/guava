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

import com.google.common.base.Function;
import com.google.common.collect.testing.ListTestSuiteBuilder;
import com.google.common.collect.testing.TestStringListGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.ListFeature;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.ForwardingWrapperTester;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests for {@code ForwardingList}.
 *
 * @author Robert Konigsberg
 * @author Louis Wasserman
 */
public class ForwardingListTest extends TestCase {
  static final class StandardImplForwardingList<T> extends ForwardingList<T> {
    private final List<T> backingList;

    StandardImplForwardingList(List<T> backingList) {
      this.backingList = backingList;
    }

    @Override
    protected List<T> delegate() {
      return backingList;
    }

    @Override
    public boolean add(T element) {
      return standardAdd(element);
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
      return standardAddAll(collection);
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> elements) {
      return standardAddAll(index, elements);
    }

    @Override
    public void clear() {
      standardClear();
    }

    @Override
    public boolean contains(Object object) {
      return standardContains(object);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
      return standardContainsAll(collection);
    }

    @Override
    public boolean remove(Object object) {
      return standardRemove(object);
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
      return standardRemoveAll(collection);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
      return standardRetainAll(collection);
    }

    @Override
    public Object[] toArray() {
      return standardToArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
      return standardToArray(array);
    }

    @Override
    public String toString() {
      return standardToString();
    }

    @Override
    public boolean equals(Object object) {
      return standardEquals(object);
    }

    @Override
    public int hashCode() {
      return standardHashCode();
    }

    @Override
    public int indexOf(Object element) {
      return standardIndexOf(element);
    }

    @Override
    public int lastIndexOf(Object element) {
      return standardLastIndexOf(element);
    }

    @Override
    public Iterator<T> iterator() {
      return listIterator();
    }

    @Override
    public ListIterator<T> listIterator() {
      return listIterator(0);
    }

    @Override
    public ListIterator<T> listIterator(int index) {
      return standardListIterator(index);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
      return standardSubList(fromIndex, toIndex);
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTestSuite(ForwardingListTest.class);
    suite.addTest(
        ListTestSuiteBuilder.using(
                new TestStringListGenerator() {

                  @Override
                  protected List<String> create(String[] elements) {
                    return new StandardImplForwardingList<>(Lists.newArrayList(elements));
                  }
                })
            .named("ForwardingList[ArrayList] with standard implementations")
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
                    return new StandardImplForwardingList<>(ImmutableList.copyOf(elements));
                  }
                })
            .named("ForwardingList[ImmutableList] with standard implementations")
            .withFeatures(CollectionSize.ANY, CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    return suite;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public void testForwarding() {
    new ForwardingWrapperTester()
        .testForwarding(
            List.class,
            new Function<List, List>() {
              @Override
              public List apply(List delegate) {
                return wrap(delegate);
              }
            });
  }

  public void testEquals() {
    List<String> list1 = ImmutableList.of("one");
    List<String> list2 = ImmutableList.of("two");
    new EqualsTester()
        .addEqualityGroup(list1, wrap(list1), wrap(list1))
        .addEqualityGroup(list2, wrap(list2))
        .testEquals();
  }

  private static <T> List<T> wrap(final List<T> delegate) {
    return new ForwardingList<T>() {
      @Override
      protected List<T> delegate() {
        return delegate;
      }
    };
  }
}

/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import com.google.common.base.Function;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.google.SortedMultisetTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringMultisetGenerator;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.ForwardingWrapperTester;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * Tests for {@link ForwardingSortedMultiset}.
 *
 * @author Louis Wasserman
 */
public class ForwardingSortedMultisetTest extends TestCase {
  static class StandardImplForwardingSortedMultiset<E> extends ForwardingSortedMultiset<E> {
    private final SortedMultiset<E> backingMultiset;

    StandardImplForwardingSortedMultiset(SortedMultiset<E> backingMultiset) {
      this.backingMultiset = backingMultiset;
    }

    @Override
    protected SortedMultiset<E> delegate() {
      return backingMultiset;
    }

    @Override
    public SortedMultiset<E> descendingMultiset() {
      return new StandardDescendingMultiset() {

        @Override
        Iterator<Entry<E>> entryIterator() {
          return backingMultiset.descendingMultiset().entrySet().iterator();
        }
      };
    }

    @Override
    public NavigableSet<E> elementSet() {
      return new StandardElementSet();
    }

    @Override
    public Entry<E> firstEntry() {
      return standardFirstEntry();
    }

    @Override
    public Entry<E> lastEntry() {
      return standardLastEntry();
    }

    @Override
    public Entry<E> pollFirstEntry() {
      return standardPollFirstEntry();
    }

    @Override
    public Entry<E> pollLastEntry() {
      return standardPollLastEntry();
    }

    @Override
    public SortedMultiset<E> subMultiset(
        E lowerBound, BoundType lowerBoundType, E upperBound, BoundType upperBoundType) {
      return standardSubMultiset(lowerBound, lowerBoundType, upperBound, upperBoundType);
    }

    @Override
    public int count(@NullableDecl Object element) {
      return standardCount(element);
    }

    @Override
    public boolean equals(@NullableDecl Object object) {
      return standardEquals(object);
    }

    @Override
    public int hashCode() {
      return standardHashCode();
    }

    @Override
    public boolean add(E element) {
      return standardAdd(element);
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
      return standardAddAll(collection);
    }

    @Override
    public void clear() {
      standardClear();
    }

    @Override
    public boolean contains(@NullableDecl Object object) {
      return standardContains(object);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
      return standardContainsAll(collection);
    }

    @Override
    public boolean isEmpty() {
      return standardIsEmpty();
    }

    @Override
    public Iterator<E> iterator() {
      return standardIterator();
    }

    @Override
    public boolean remove(@NullableDecl Object object) {
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
    public int size() {
      return standardSize();
    }

    @Override
    public Object[] toArray() {
      return standardToArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
      return standardToArray(array);
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTestSuite(ForwardingSortedMultisetTest.class);
    suite.addTest(
        SortedMultisetTestSuiteBuilder.using(
                new TestStringMultisetGenerator() {
                  @Override
                  protected Multiset<String> create(String[] elements) {
                    return new StandardImplForwardingSortedMultiset<>(
                        TreeMultiset.create(Arrays.asList(elements)));
                  }

                  @Override
                  public List<String> order(List<String> insertionOrder) {
                    return Ordering.natural().sortedCopy(insertionOrder);
                  }
                })
            .named("ForwardingSortedMultiset with standard impls")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.GENERAL_PURPOSE,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    return suite;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public void testForwarding() {
    new ForwardingWrapperTester()
        .testForwarding(
            SortedMultiset.class,
            new Function<SortedMultiset, SortedMultiset>() {
              @Override
              public SortedMultiset apply(SortedMultiset delegate) {
                return wrap(delegate);
              }
            });
  }

  public void testEquals() {
    SortedMultiset<String> set1 = ImmutableSortedMultiset.of("one");
    SortedMultiset<String> set2 = ImmutableSortedMultiset.of("two");
    new EqualsTester()
        .addEqualityGroup(set1, wrap(set1), wrap(set1))
        .addEqualityGroup(set2, wrap(set2))
        .testEquals();
  }

  private static <T> SortedMultiset<T> wrap(final SortedMultiset<T> delegate) {
    return new ForwardingSortedMultiset<T>() {
      @Override
      protected SortedMultiset<T> delegate() {
        return delegate;
      }
    };
  }
}

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

import static com.google.common.collect.BoundType.CLOSED;
import static com.google.common.collect.BoundType.OPEN;

import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.google.SortedMultisetTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringMultisetGenerator;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

import javax.annotation.Nullable;

/**
 * Tests for {@link ForwardingSortedMultiset}.
 *
 * @author Louis Wasserman
 */
public class ForwardingSortedMultisetTest extends ForwardingMultisetTest {
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
          return backingMultiset
              .descendingMultiset()
              .entrySet()
              .iterator();
        }
      };
    }

    @Override
    public SortedSet<E> elementSet() {
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
    public int count(@Nullable Object element) {
      return standardCount(element);
    }

    @Override
    public boolean equals(@Nullable Object object) {
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
    public boolean contains(@Nullable Object object) {
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
    public boolean remove(@Nullable Object object) {
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
    suite.addTest(SortedMultisetTestSuiteBuilder
        .using(new TestStringMultisetGenerator() {
          @Override
          protected Multiset<String> create(String[] elements) {
            return new StandardImplForwardingSortedMultiset<String>(
                TreeMultiset.create(Arrays.asList(elements)));
          }

          @Override
          public List<String> order(List<String> insertionOrder) {
            return Ordering.natural().sortedCopy(insertionOrder);
          }
        })
        .named("ForwardingSortedMultiset with standard impls")
        .withFeatures(
            CollectionSize.ANY, CollectionFeature.KNOWN_ORDER, CollectionFeature.GENERAL_PURPOSE,
            CollectionFeature.ALLOWS_NULL_QUERIES)
        .createTestSuite());

    return suite;
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    /*
     * Class parameters must be raw, so we can't create a proxy with generic type arguments. The
     * created proxy only records calls and returns null, so the type is irrelevant at runtime.
     */
    @SuppressWarnings("unchecked")
    final SortedMultiset<String> sortedMultiset = createProxyInstance(SortedMultiset.class);
    forward = new ForwardingSortedMultiset<String>() {
      @Override
      protected SortedMultiset<String> delegate() {
        return sortedMultiset;
      }
    };
  }

  public void testComparator() {
    forward().comparator();
    assertEquals("[comparator]", getCalls());
  }

  public void testFirstEntry() {
    forward().firstEntry();
    assertEquals("[firstEntry]", getCalls());
  }

  public void testLastEntry() {
    forward().lastEntry();
    assertEquals("[lastEntry]", getCalls());
  }

  public void testPollFirstEntry() {
    forward().pollFirstEntry();
    assertEquals("[pollFirstEntry]", getCalls());
  }

  public void testPollLastEntry() {
    forward().pollLastEntry();
    assertEquals("[pollLastEntry]", getCalls());
  }

  public void testDescendingMultiset() {
    forward().descendingMultiset();
    assertEquals("[descendingMultiset]", getCalls());
  }

  public void testHeadMultiset() {
    forward().headMultiset("abcd", CLOSED);
    assertEquals("[headMultiset(Object,BoundType)]", getCalls());
  }

  public void testSubMultiset() {
    forward().subMultiset("abcd", CLOSED, "dcba", OPEN);
    assertEquals("[subMultiset(Object,BoundType,Object,BoundType)]", getCalls());
  }

  public void testTailMultiset() {
    forward().tailMultiset("last", OPEN);
    assertEquals("[tailMultiset(Object,BoundType)]", getCalls());
  }

  @Override
  protected SortedMultiset<String> forward() {
    return (SortedMultiset<String>) super.forward();
  }
}

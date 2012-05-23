/*
 * Copyright (C) 2010 The Guava Authors
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

import com.google.common.collect.testing.SafeTreeSet;
import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.TestStringSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

/**
 * Tests for {@code ForwardingSortedSet}.
 *
 * @author Louis Wasserman
 */
public class ForwardingSortedSetTest extends ForwardingSetTest {
  static class StandardImplForwardingSortedSet<T>
      extends ForwardingSortedSet<T> {
    private final SortedSet<T> backingSet;

    StandardImplForwardingSortedSet(SortedSet<T> backingSet) {
      this.backingSet = backingSet;
    }

    @Override protected SortedSet<T> delegate() {
      return backingSet;
    }

    @Override public boolean equals(Object object) {
      return standardEquals(object);
    }

    @Override public int hashCode() {
      return standardHashCode();
    }

    @Override public boolean addAll(Collection<? extends T> collection) {
      return standardAddAll(collection);
    }

    @Override public void clear() {
      standardClear();
    }

    @Override public boolean contains(Object object) {
      return standardContains(object);
    }

    @Override public boolean containsAll(Collection<?> collection) {
      return standardContainsAll(collection);
    }

    @Override public boolean remove(Object object) {
      return standardRemove(object);
    }

    @Override public boolean removeAll(Collection<?> collection) {
      return standardRemoveAll(collection);
    }

    @Override public boolean retainAll(Collection<?> collection) {
      return standardRetainAll(collection);
    }

    @Override public Object[] toArray() {
      return standardToArray();
    }

    @Override public <T> T[] toArray(T[] array) {
      return standardToArray(array);
    }

    @Override public String toString() {
      return standardToString();
    }

    @Override public SortedSet<T> subSet(T fromElement, T toElement) {
      return standardSubSet(fromElement, toElement);
    }
  }
  
  public static Test suite() {
    TestSuite suite = new TestSuite();
    
    suite.addTestSuite(ForwardingSortedSetTest.class);
    suite.addTest(
        SetTestSuiteBuilder.using(new TestStringSetGenerator() {
          @Override protected Set<String> create(String[] elements) {
            return new StandardImplForwardingSortedSet<String>(
                new SafeTreeSet<String>(Arrays.asList(elements)));
          }

          @Override public List<String> order(List<String> insertionOrder) {
            return Lists.newArrayList(Sets.newTreeSet(insertionOrder));
          }
        }).named(
            "ForwardingSortedSet[SafeTreeSet] with standard implementations")
            .withFeatures(CollectionSize.ANY, CollectionFeature.KNOWN_ORDER,
                CollectionFeature.GENERAL_PURPOSE).createTestSuite());
    
    return suite;
  }
  
  @Override public void setUp() throws Exception {
    super.setUp();
    /*
     * Class parameters must be raw, so we can't create a proxy with generic
     * type arguments. The created proxy only records calls and returns null, so
     * the type is irrelevant at runtime.
     */
    @SuppressWarnings("unchecked")
    final SortedSet<String> sortedSet
        = createProxyInstance(SortedSet.class);
    forward = new ForwardingSortedSet<String>() {
      @Override protected SortedSet<String> delegate() {
        return sortedSet;
      }
    };
  }

  public void testComparator() {
    forward().comparator();
    assertEquals("[comparator]", getCalls());
  }

  public void testFirst() {
    forward().first();
    assertEquals("[first]", getCalls());
  }

  public void testHeadSet_K() {
    forward().headSet("asdf");
    assertEquals("[headSet(Object)]", getCalls());
  }

  public void testLast() {
    forward().last();
    assertEquals("[last]", getCalls());
  }

  public void testSubSet_K_K() {
    forward().subSet("first", "last");
    assertEquals("[subSet(Object,Object)]", getCalls());
  }

  public void testTailSet_K() {
    forward().tailSet("last");
    assertEquals("[tailSet(Object)]", getCalls());
  }

  @Override SortedSet<String> forward() {
    return (SortedSet<String>) super.forward();
  }
}

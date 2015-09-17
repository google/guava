/*
 * Copyright (C) 2009 The Guava Authors
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

import static java.util.Arrays.asList;

import com.google.common.collect.testing.CollectionTestSuiteBuilder;
import com.google.common.collect.testing.MinimalCollection;
import com.google.common.collect.testing.TestStringCollectionGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.Collection;
import java.util.Collections;

/**
 * Tests for {@link ForwardingCollection}.
 *
 * @author Robert Konigsberg
 * @author Hayward Chan
 * @author Louis Wasserman
 */
public class ForwardingCollectionTest extends ForwardingTestCase {
  static final class StandardImplForwardingCollection<T>
      extends ForwardingCollection<T> {
    private final Collection<T> backingCollection;

    StandardImplForwardingCollection(Collection<T> backingCollection) {
      this.backingCollection = backingCollection;
    }

    @Override protected Collection<T> delegate() {
      return backingCollection;
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
  }

  private static final Collection<String> EMPTY_COLLECTION =
      Collections.emptyList();

  private Collection<String> forward;

  public static Test suite() {
    TestSuite suite = new TestSuite();
    
    suite.addTestSuite(ForwardingCollectionTest.class);
    suite.addTest(
        CollectionTestSuiteBuilder.using(new TestStringCollectionGenerator() {
          @Override protected Collection<String> create(String[] elements) {
            return new StandardImplForwardingCollection<String>(
                Lists.newLinkedList(asList(elements)));
          }
        }).named(
            "ForwardingCollection[LinkedList] with standard implementations")
            .withFeatures(CollectionSize.ANY,
                CollectionFeature.ALLOWS_NULL_VALUES,
                CollectionFeature.GENERAL_PURPOSE).createTestSuite());
    suite.addTest(
        CollectionTestSuiteBuilder.using(new TestStringCollectionGenerator() {
          @Override protected Collection<String> create(String[] elements) {
            return new StandardImplForwardingCollection<String>(
                MinimalCollection.of(elements));
          }
        }).named(
            "ForwardingCollection[MinimalCollection] with standard"
            + " implementations")
            .withFeatures(CollectionSize.ANY, 
                CollectionFeature.ALLOWS_NULL_VALUES).createTestSuite());
    
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
    final Collection<String> list = createProxyInstance(Collection.class);
    forward = new ForwardingCollection<String>() {
      @Override protected Collection<String> delegate() {
        return list;
      }
    };
  }

  public void testAdd_T() {
    forward.add("asdf");
    assertEquals("[add(Object)]", getCalls());
  }

  public void testAddAll_Collection() {
    forward.addAll(EMPTY_COLLECTION);
    assertEquals("[addAll(Collection)]", getCalls());
  }

  public void testClear() {
    forward.clear();
    assertEquals("[clear]", getCalls());
  }

  public void testContains_Object() {
    forward.contains(null);
    assertEquals("[contains(Object)]", getCalls());
  }

  public void testContainsAll_Collection() {
    forward.containsAll(EMPTY_COLLECTION);
    assertEquals("[containsAll(Collection)]", getCalls());
  }

  public void testIsEmpty() {
    forward.isEmpty();
    assertEquals("[isEmpty]", getCalls());
  }

  public void testIterator() {
    forward.iterator();
    assertEquals("[iterator]", getCalls());
  }

  public void testRemove_Object() {
    forward.remove(null);
    assertEquals("[remove(Object)]", getCalls());
  }

  public void testRemoveAll_Collection() {
    forward.removeAll(EMPTY_COLLECTION);
    assertEquals("[removeAll(Collection)]", getCalls());
  }

  public void testRetainAll_Collection() {
    forward.retainAll(EMPTY_COLLECTION);
    assertEquals("[retainAll(Collection)]", getCalls());
  }

  public void testSize() {
    forward.size();
    assertEquals("[size]", getCalls());
  }

  public void testToArray() {
    forward.toArray();
    assertEquals("[toArray]", getCalls());
  }

  public void testToArray_TArray() {
    forward.toArray(new String[0]);
    assertEquals("[toArray(Object[])]", getCalls());
  }

  public void testToString() {
    forward.toString();
    assertEquals("[toString]", getCalls());
  }

  public void testEquals_Object() {
    forward.equals("asdf");
    assertFalse("equals() should not be forwarded.", isCalled());
  }

  public void testHashCode() {
    forward.hashCode();
    assertFalse("hashCode() should not be forwarded.", isCalled());
  }
}

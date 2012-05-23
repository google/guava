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

import com.google.common.collect.testing.ListTestSuiteBuilder;
import com.google.common.collect.testing.TestStringListGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.ListFeature;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

/**
 * Tests for {@code ForwardingList}.
 *
 * @author Robert Konigsberg
 * @author Louis Wasserman
 */
public class ForwardingListTest extends ForwardingTestCase {
  static final class StandardImplForwardingList<T> extends ForwardingList<T> {
    private final List<T> backingList;

    StandardImplForwardingList(List<T> backingList) {
      this.backingList = backingList;
    }

    @Override protected List<T> delegate() {
      return backingList;
    }

    @Override public boolean add(T element) {
      return standardAdd(element);
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

    @Override public boolean addAll(
        int index, Collection<? extends T> elements) {
      return standardAddAll(index, elements);
    }

    @Override public boolean equals(Object object) {
      return standardEquals(object);
    }

    @Override public int hashCode() {
      return standardHashCode();
    }

    @Override public int indexOf(Object element) {
      return standardIndexOf(element);
    }

    @Override public int lastIndexOf(Object element) {
      return standardLastIndexOf(element);
    }

    @Override public Iterator<T> iterator() {
      return listIterator();
    }

    @Override public ListIterator<T> listIterator() {
      return listIterator(0);
    }

    @Override public ListIterator<T> listIterator(int index) {
      return standardListIterator(index);
    }

    @Override public List<T> subList(int fromIndex, int toIndex) {
      return standardSubList(fromIndex, toIndex);
    }
  }
  
  private static final List<String> EMPTY_LIST =
      Collections.<String>emptyList();

  private List<String> forward;

  public static Test suite() {
    TestSuite suite = new TestSuite();
    
    suite.addTestSuite(ForwardingListTest.class);
    suite.addTest(ListTestSuiteBuilder.using(new TestStringListGenerator() {

      @Override protected List<String> create(String[] elements) {
        return new StandardImplForwardingList<String>(
            Lists.newArrayList(elements));
      }
    }).named("ForwardingList[ArrayList] with standard implementations")
        .withFeatures(CollectionSize.ANY, CollectionFeature.ALLOWS_NULL_VALUES,
            ListFeature.GENERAL_PURPOSE).createTestSuite());
    suite.addTest(ListTestSuiteBuilder.using(new TestStringListGenerator() {

      @Override protected List<String> create(String[] elements) {
        return new StandardImplForwardingList<String>(
            ImmutableList.copyOf(elements));
      }
    }).named("ForwardingList[ImmutableList] with standard implementations")
        .withFeatures(CollectionSize.ANY, CollectionFeature.ALLOWS_NULL_QUERIES)
        .createTestSuite());
    
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
    final List<String> list = createProxyInstance(List.class);
    forward = new ForwardingList<String>() {
      @Override protected List<String> delegate() {
        return list;
      }
    };
  }

  public void testAdd_T() {
    forward.add("asdf");
    assertEquals("[add(Object)]", getCalls());
  }

  public void testAdd_int_T() {
    forward.add(0, "asdf");
    assertEquals("[add(int,Object)]", getCalls());
  }

  public void testAddAll_Collection() {
    forward.addAll(EMPTY_LIST);
    assertEquals("[addAll(Collection)]", getCalls());
  }

  public void testAddAll_int_Collection() {
    forward.addAll(0, Collections.singleton("asdf"));
    assertEquals("[addAll(int,Collection)]", getCalls());
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
    forward.containsAll(EMPTY_LIST);
    assertEquals("[containsAll(Collection)]", getCalls());
  }

  public void testGet_int() {
    forward.get(0);
    assertEquals("[get(int)]", getCalls());
  }

  public void testIndexOf_Object() {
    forward.indexOf(null);
    assertEquals("[indexOf(Object)]", getCalls());
  }

  public void testIsEmpty() {
    forward.isEmpty();
    assertEquals("[isEmpty]", getCalls());
  }

  public void testIterator() {
    forward.iterator();
    assertEquals("[iterator]", getCalls());
  }

  public void testLastIndexOf_Object() {
    forward.lastIndexOf("asdf");
    assertEquals("[lastIndexOf(Object)]", getCalls());
  }

  public void testListIterator() {
    forward.listIterator();
    assertEquals("[listIterator]", getCalls());
  }

  public void testListIterator_int() {
    forward.listIterator(0);
    assertEquals("[listIterator(int)]", getCalls());
  }

  public void testRemove_int() {
    forward.remove(0);
    assertEquals("[remove(int)]", getCalls());
  }

  public void testRemove_Object() {
    forward.remove(null);
    assertEquals("[remove(Object)]", getCalls());
  }

  public void testRemoveAll_Collection() {
    forward.removeAll(EMPTY_LIST);
    assertEquals("[removeAll(Collection)]", getCalls());
  }

  public void testRetainAll_Collection() {
    forward.retainAll(EMPTY_LIST);
    assertEquals("[retainAll(Collection)]", getCalls());
  }

  public void testSet_int_T() {
    forward.set(0, "asdf");
    assertEquals("[set(int,Object)]", getCalls());
  }

  public void testSize() {
    forward.size();
    assertEquals("[size]", getCalls());
  }

  public void testSubList_int_int() {
    forward.subList(0, 1);
    assertEquals("[subList(int,int)]", getCalls());
  }

  public void testToArray() {
    forward.toArray();
    assertEquals("[toArray]", getCalls());
  }

  public void testToArray_TArray() {
    forward.toArray(new String[0]);
    assertEquals("[toArray(Object[])]", getCalls());
  }

  public void testEquals_Object() {
    forward.equals("asdf");
    assertEquals("[equals(Object)]", getCalls());
  }

  public void testHashCode() {
    forward.hashCode();
    assertEquals("[hashCode]", getCalls());
  }

  public void testRandomAccess() {
    assertFalse(forward instanceof RandomAccess);
  }

  public void testToString() {
    forward.toString();
    assertEquals("[toString]", getCalls());
  }
}

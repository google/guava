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

import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.TestStringSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.google.MultisetTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringMultisetGenerator;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

/**
 * Tests for {@link ForwardingMultiset}.
 *
 * @author Hayward Chan
 * @author Louis Wasserman
 */
public class ForwardingMultisetTest extends ForwardingTestCase {

  static final class StandardImplForwardingMultiset<T>
      extends ForwardingMultiset<T> {
    private final Multiset<T> backingCollection;

    StandardImplForwardingMultiset(Multiset<T> backingMultiset) {
      this.backingCollection = backingMultiset;
    }

    @Override protected Multiset<T> delegate() {
      return backingCollection;
    }

    @Override public boolean addAll(Collection<? extends T> collection) {
      return standardAddAll(collection);
    }

    @Override public boolean add(T element) {
      return standardAdd(element);
    }

    @Override public void clear() {
      standardClear();
    }

    @Override public int count(Object element) {
      return standardCount(element);
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

    @Override public boolean equals(Object object) {
      return standardEquals(object);
    }

    @Override public int hashCode() {
      return standardHashCode();
    }

    @Override public boolean setCount(T element, int oldCount, int newCount) {
      return standardSetCount(element, oldCount, newCount);
    }

    @Override public int setCount(T element, int count) {
      return standardSetCount(element, count);
    }

    @Override public Set<T> elementSet() {
      return new StandardElementSet();
    }

    @Override public Iterator<T> iterator() {
      return standardIterator();
    }

    @Override public boolean isEmpty() {
      return standardIsEmpty();
    }

    @Override public int size() {
      return standardSize();
    }
  }

  private static final Collection<String> EMPTY_COLLECTION =
      Collections.emptyList();

  protected Multiset<String> forward;

  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTestSuite(ForwardingMultisetTest.class);
    suite.addTest(
        MultisetTestSuiteBuilder.using(new TestStringMultisetGenerator() {

          @Override protected Multiset<String> create(String[] elements) {
            return new StandardImplForwardingMultiset<String>(
                LinkedHashMultiset.create(Arrays.asList(elements)));
          }
        }).named("ForwardingMultiset[LinkedHashMultiset] with standard "
            + "implementations").withFeatures(CollectionSize.ANY,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.GENERAL_PURPOSE).createTestSuite());
    suite.addTest(
        MultisetTestSuiteBuilder.using(new TestStringMultisetGenerator() {

          @Override protected Multiset<String> create(String[] elements) {
            return new StandardImplForwardingMultiset<String>(
                ImmutableMultiset.copyOf(elements));
          }
        }).named("ForwardingMultiset[ImmutableMultiset] with standard "
            + "implementations")
            .withFeatures(CollectionSize.ANY,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());
    suite.addTest(SetTestSuiteBuilder.using(new TestStringSetGenerator() {

      /**
       * Returns a Multiset that throws an exception on any attempt to use a
       * method not specifically authorized by the elementSet() or hashCode()
       * docs.
       */
      @Override protected Set<String> create(String[] elements) {
        final Multiset<String> inner =
            LinkedHashMultiset.create(Arrays.asList(elements));
        return new ForwardingMultiset<String>() {
          @Override protected Multiset<String> delegate() {
            return inner;
          }

          @Override public Set<String> elementSet() {
            return new StandardElementSet();
          }

          @Override public int add(String element, int occurrences) {
            throw new UnsupportedOperationException();
          }
          @Override public Set<Entry<String>> entrySet() {
            final Set<Entry<String>> backingSet = super.entrySet();
            return new ForwardingSet<Entry<String>>() {
              @Override protected Set<Entry<String>> delegate() {
                return backingSet;
              }
              @Override public boolean add(Entry<String> element) {
                throw new UnsupportedOperationException();
              }
              @Override public boolean addAll(
                  Collection<? extends Entry<String>> collection) {
                throw new UnsupportedOperationException();
              }
              @Override public void clear() {
                throw new UnsupportedOperationException();
              }
              @Override public boolean contains(Object object) {
                throw new UnsupportedOperationException();
              }
              @Override public boolean containsAll(Collection<?> collection) {
                throw new UnsupportedOperationException();
              }
              @Override public boolean isEmpty() {
                throw new UnsupportedOperationException();
              }
              @Override public boolean remove(Object object) {
                throw new UnsupportedOperationException();
              }
              @Override public boolean removeAll(Collection<?> collection) {
                throw new UnsupportedOperationException();
              }
              @Override public boolean retainAll(Collection<?> collection) {
                throw new UnsupportedOperationException();
              }
            };
          }
          @Override public boolean equals(Object object) {
            throw new UnsupportedOperationException();
          }
          @Override public boolean remove(Object element) {
            throw new UnsupportedOperationException();
          }
          @Override public boolean setCount(
              String element, int oldCount, int newCount) {
            throw new UnsupportedOperationException();
          }
          @Override public int setCount(String element, int count) {
            throw new UnsupportedOperationException();
          }
          @Override public boolean add(String element) {
            throw new UnsupportedOperationException();
          }
          @Override public boolean addAll(
              Collection<? extends String> collection) {
            throw new UnsupportedOperationException();
          }
          @Override public Iterator<String> iterator() {
            throw new UnsupportedOperationException();
          }
          @Override public boolean removeAll(Collection<?> collection) {
            throw new UnsupportedOperationException();
          }
          @Override public boolean retainAll(Collection<?> collection) {
            throw new UnsupportedOperationException();
          }
          @Override public int size() {
            throw new UnsupportedOperationException();
          }
        }.elementSet();
      }
    }).named("standardElementSet tripwire").withFeatures(CollectionSize.ANY,
        CollectionFeature.ALLOWS_NULL_VALUES,
        CollectionFeature.REMOVE_OPERATIONS).createTestSuite());

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
    final Multiset<String> multiset = createProxyInstance(Multiset.class);
    forward = new ForwardingMultiset<String>() {
      @Override protected Multiset<String> delegate() {
        return multiset;
      }
    };
  }

  public void testAdd_T() {
    forward().add("asdf");
    assertEquals("[add(Object)]", getCalls());
  }

  public void testAddAll_Collection() {
    forward().addAll(EMPTY_COLLECTION);
    assertEquals("[addAll(Collection)]", getCalls());
  }

  public void testClear() {
    forward().clear();
    assertEquals("[clear]", getCalls());
  }

  public void testContains_Object() {
    forward().contains(null);
    assertEquals("[contains(Object)]", getCalls());
  }

  public void testContainsAll_Collection() {
    forward().containsAll(EMPTY_COLLECTION);
    assertEquals("[containsAll(Collection)]", getCalls());
  }

  public void testIsEmpty() {
    forward().isEmpty();
    assertEquals("[isEmpty]", getCalls());
  }

  public void testIterator() {
    forward().iterator();
    assertEquals("[iterator]", getCalls());
  }

  public void testRemove_Object() {
    forward().remove(null);
    assertEquals("[remove(Object)]", getCalls());
  }

  public void testRemoveAll_Collection() {
    forward().removeAll(EMPTY_COLLECTION);
    assertEquals("[removeAll(Collection)]", getCalls());
  }

  public void testRetainAll_Collection() {
    forward().retainAll(EMPTY_COLLECTION);
    assertEquals("[retainAll(Collection)]", getCalls());
  }

  public void testSize() {
    forward().size();
    assertEquals("[size]", getCalls());
  }

  public void testToArray() {
    forward().toArray();
    assertEquals("[toArray]", getCalls());
  }

  public void testToArray_TArray() {
    forward().toArray(new String[0]);
    assertEquals("[toArray(Object[])]", getCalls());
  }

  public void testToString() {
    forward().toString();
    assertEquals("[toString]", getCalls());
  }

  public void testEquals_Object() {
    forward().equals("asdf");
    assertEquals("[equals(Object)]", getCalls());
  }

  public void testHashCode() {
    forward().hashCode();
    assertEquals("[hashCode]", getCalls());
  }

  public void testCount_Object() {
    forward().count(null);
    assertEquals("[count(Object)]", getCalls());
  }

  public void testAdd_Object_int() {
    forward().add("asd", 23);
    assertEquals("[add(Object,int)]", getCalls());
  }

  public void testRemove_Object_int() {
    forward().remove("asd", 23);
    assertEquals("[remove(Object,int)]", getCalls());
  }

  public void testSetCount_Object_int() {
    forward().setCount("asdf", 233);
    assertEquals("[setCount(Object,int)]", getCalls());
  }

  public void testSetCount_Object_oldCount_newCount() {
    forward().setCount("asdf", 4552, 1233);
    assertEquals("[setCount(Object,int,int)]", getCalls());
  }

  public void testElementSet() {
    forward().elementSet();
    assertEquals("[elementSet]", getCalls());
  }

  public void testEntrySet() {
    forward().entrySet();
    assertEquals("[entrySet]", getCalls());
  }

  protected Multiset<String> forward() {
    return forward;
  }
}

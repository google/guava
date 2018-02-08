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

import com.google.common.base.Function;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.TestStringSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.google.MultisetTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringMultisetGenerator;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.ForwardingWrapperTester;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests for {@link ForwardingMultiset}.
 *
 * @author Hayward Chan
 * @author Louis Wasserman
 */
public class ForwardingMultisetTest extends TestCase {

  static final class StandardImplForwardingMultiset<T> extends ForwardingMultiset<T> {
    private final Multiset<T> backingCollection;

    StandardImplForwardingMultiset(Multiset<T> backingMultiset) {
      this.backingCollection = backingMultiset;
    }

    @Override
    protected Multiset<T> delegate() {
      return backingCollection;
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
      return standardAddAll(collection);
    }

    @Override
    public boolean add(T element) {
      return standardAdd(element);
    }

    @Override
    public void clear() {
      standardClear();
    }

    @Override
    public int count(Object element) {
      return standardCount(element);
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
    public boolean setCount(T element, int oldCount, int newCount) {
      return standardSetCount(element, oldCount, newCount);
    }

    @Override
    public int setCount(T element, int count) {
      return standardSetCount(element, count);
    }

    @Override
    public Set<T> elementSet() {
      return new StandardElementSet();
    }

    @Override
    public Iterator<T> iterator() {
      return standardIterator();
    }

    @Override
    public boolean isEmpty() {
      return standardIsEmpty();
    }

    @Override
    public int size() {
      return standardSize();
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTestSuite(ForwardingMultisetTest.class);
    suite.addTest(
        MultisetTestSuiteBuilder.using(
                new TestStringMultisetGenerator() {

                  @Override
                  protected Multiset<String> create(String[] elements) {
                    return new StandardImplForwardingMultiset<>(
                        LinkedHashMultiset.create(Arrays.asList(elements)));
                  }
                })
            .named("ForwardingMultiset[LinkedHashMultiset] with standard " + "implementations")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.ALLOWS_NULL_VALUES,
                CollectionFeature.GENERAL_PURPOSE)
            .createTestSuite());
    suite.addTest(
        MultisetTestSuiteBuilder.using(
                new TestStringMultisetGenerator() {

                  @Override
                  protected Multiset<String> create(String[] elements) {
                    return new StandardImplForwardingMultiset<>(ImmutableMultiset.copyOf(elements));
                  }
                })
            .named("ForwardingMultiset[ImmutableMultiset] with standard " + "implementations")
            .withFeatures(CollectionSize.ANY, CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());
    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {

                  /**
                   * Returns a Multiset that throws an exception on any attempt to use a method not
                   * specifically authorized by the elementSet() or hashCode() docs.
                   */
                  @Override
                  protected Set<String> create(String[] elements) {
                    final Multiset<String> inner =
                        LinkedHashMultiset.create(Arrays.asList(elements));
                    return new ForwardingMultiset<String>() {
                      @Override
                      protected Multiset<String> delegate() {
                        return inner;
                      }

                      @Override
                      public Set<String> elementSet() {
                        return new StandardElementSet();
                      }

                      @Override
                      public int add(String element, int occurrences) {
                        throw new UnsupportedOperationException();
                      }

                      @Override
                      public boolean add(String element) {
                        throw new UnsupportedOperationException();
                      }

                      @Override
                      public Set<Entry<String>> entrySet() {
                        final Set<Entry<String>> backingSet = super.entrySet();
                        return new ForwardingSet<Entry<String>>() {
                          @Override
                          protected Set<Entry<String>> delegate() {
                            return backingSet;
                          }

                          @Override
                          public boolean add(Entry<String> element) {
                            throw new UnsupportedOperationException();
                          }

                          @Override
                          public boolean addAll(Collection<? extends Entry<String>> collection) {
                            throw new UnsupportedOperationException();
                          }

                          @Override
                          public void clear() {
                            throw new UnsupportedOperationException();
                          }

                          @Override
                          public boolean contains(Object object) {
                            throw new UnsupportedOperationException();
                          }

                          @Override
                          public boolean containsAll(Collection<?> collection) {
                            throw new UnsupportedOperationException();
                          }

                          @Override
                          public boolean isEmpty() {
                            throw new UnsupportedOperationException();
                          }

                          @Override
                          public boolean remove(Object object) {
                            throw new UnsupportedOperationException();
                          }

                          @Override
                          public boolean removeAll(Collection<?> collection) {
                            throw new UnsupportedOperationException();
                          }

                          @Override
                          public boolean retainAll(Collection<?> collection) {
                            throw new UnsupportedOperationException();
                          }
                        };
                      }

                      @Override
                      public boolean equals(Object object) {
                        throw new UnsupportedOperationException();
                      }

                      @Override
                      public boolean remove(Object element) {
                        throw new UnsupportedOperationException();
                      }

                      @Override
                      public boolean setCount(String element, int oldCount, int newCount) {
                        throw new UnsupportedOperationException();
                      }

                      @Override
                      public int setCount(String element, int count) {
                        throw new UnsupportedOperationException();
                      }

                      @Override
                      public boolean addAll(Collection<? extends String> collection) {
                        throw new UnsupportedOperationException();
                      }

                      @Override
                      public Iterator<String> iterator() {
                        throw new UnsupportedOperationException();
                      }

                      @Override
                      public boolean removeAll(Collection<?> collection) {
                        throw new UnsupportedOperationException();
                      }

                      @Override
                      public boolean retainAll(Collection<?> collection) {
                        throw new UnsupportedOperationException();
                      }

                      @Override
                      public int size() {
                        throw new UnsupportedOperationException();
                      }
                    }.elementSet();
                  }
                })
            .named("standardElementSet tripwire")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.ALLOWS_NULL_VALUES,
                CollectionFeature.REMOVE_OPERATIONS)
            .createTestSuite());

    return suite;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public void testForwarding() {
    new ForwardingWrapperTester()
        .testForwarding(
            Multiset.class,
            new Function<Multiset, Multiset>() {
              @Override
              public Multiset apply(Multiset delegate) {
                return wrap(delegate);
              }
            });
  }

  public void testEquals() {
    Multiset<String> set1 = ImmutableMultiset.of("one");
    Multiset<String> set2 = ImmutableMultiset.of("two");
    new EqualsTester()
        .addEqualityGroup(set1, wrap(set1), wrap(set1))
        .addEqualityGroup(set2, wrap(set2))
        .testEquals();
  }

  private static <T> Multiset<T> wrap(final Multiset<T> delegate) {
    return new ForwardingMultiset<T>() {
      @Override
      protected Multiset<T> delegate() {
        return delegate;
      }
    };
  }
}

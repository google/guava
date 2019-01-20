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

import com.google.common.base.Function;
import com.google.common.collect.testing.CollectionTestSuiteBuilder;
import com.google.common.collect.testing.MinimalCollection;
import com.google.common.collect.testing.TestStringCollectionGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.testing.ForwardingWrapperTester;
import java.util.Collection;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests for {@link ForwardingCollection}.
 *
 * @author Robert Konigsberg
 * @author Hayward Chan
 * @author Louis Wasserman
 */
public class ForwardingCollectionTest extends TestCase {
  static final class StandardImplForwardingCollection<T> extends ForwardingCollection<T> {
    private final Collection<T> backingCollection;

    StandardImplForwardingCollection(Collection<T> backingCollection) {
      this.backingCollection = backingCollection;
    }

    @Override
    protected Collection<T> delegate() {
      return backingCollection;
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
      return standardAddAll(collection);
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
  }

  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTestSuite(ForwardingCollectionTest.class);
    suite.addTest(
        CollectionTestSuiteBuilder.using(
                new TestStringCollectionGenerator() {
                  @Override
                  protected Collection<String> create(String[] elements) {
                    return new StandardImplForwardingCollection<>(
                        Lists.newLinkedList(asList(elements)));
                  }
                })
            .named("ForwardingCollection[LinkedList] with standard implementations")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.ALLOWS_NULL_VALUES,
                CollectionFeature.GENERAL_PURPOSE)
            .createTestSuite());
    suite.addTest(
        CollectionTestSuiteBuilder.using(
                new TestStringCollectionGenerator() {
                  @Override
                  protected Collection<String> create(String[] elements) {
                    return new StandardImplForwardingCollection<>(MinimalCollection.of(elements));
                  }
                })
            .named("ForwardingCollection[MinimalCollection] with standard" + " implementations")
            .withFeatures(CollectionSize.ANY, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    return suite;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public void testForwarding() {
    new ForwardingWrapperTester()
        .testForwarding(
            Collection.class,
            new Function<Collection, Collection>() {
              @Override
              public Collection apply(Collection delegate) {
                return wrap(delegate);
              }
            });
  }

  private static <T> Collection<T> wrap(final Collection<T> delegate) {
    return new ForwardingCollection<T>() {
      @Override
      protected Collection<T> delegate() {
        return delegate;
      }
    };
  }
}

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

import static java.util.Arrays.asList;

import com.google.common.base.Function;
import com.google.common.collect.testing.MinimalSet;
import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.TestStringSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.ForwardingWrapperTester;
import java.util.Collection;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests for {@code ForwardingSet}.
 *
 * @author Robert Konigsberg
 * @author Louis Wasserman
 */
public class ForwardingSetTest extends TestCase {
  static class StandardImplForwardingSet<T> extends ForwardingSet<T> {
    private final Set<T> backingSet;

    StandardImplForwardingSet(Set<T> backingSet) {
      this.backingSet = backingSet;
    }

    @Override
    protected Set<T> delegate() {
      return backingSet;
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

    suite.addTestSuite(ForwardingSetTest.class);
    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return new StandardImplForwardingSet<>(Sets.newLinkedHashSet(asList(elements)));
                  }
                })
            .named("ForwardingSet[LinkedHashSet] with standard implementations")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.ALLOWS_NULL_VALUES,
                CollectionFeature.GENERAL_PURPOSE)
            .createTestSuite());
    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return new StandardImplForwardingSet<>(MinimalSet.of(elements));
                  }
                })
            .named("ForwardingSet[MinimalSet] with standard implementations")
            .withFeatures(CollectionSize.ANY, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    return suite;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public void testForwarding() {
    new ForwardingWrapperTester()
        .testForwarding(
            Set.class,
            new Function<Set, Set>() {
              @Override
              public Set apply(Set delegate) {
                return wrap(delegate);
              }
            });
  }

  public void testEquals() {
    Set<String> set1 = ImmutableSet.of("one");
    Set<String> set2 = ImmutableSet.of("two");
    new EqualsTester()
        .addEqualityGroup(set1, wrap(set1), wrap(set1))
        .addEqualityGroup(set2, wrap(set2))
        .testEquals();
  }

  private static <T> Set<T> wrap(final Set<T> delegate) {
    return new ForwardingSet<T>() {
      @Override
      protected Set<T> delegate() {
        return delegate;
      }
    };
  }
}

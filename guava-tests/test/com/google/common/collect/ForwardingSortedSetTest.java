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

import com.google.common.base.Function;
import com.google.common.collect.testing.SafeTreeSet;
import com.google.common.collect.testing.SortedSetTestSuiteBuilder;
import com.google.common.collect.testing.TestStringSortedSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.ForwardingWrapperTester;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Tests for {@code ForwardingSortedSet}.
 *
 * @author Louis Wasserman
 */
public class ForwardingSortedSetTest extends TestCase {
  static class StandardImplForwardingSortedSet<T> extends ForwardingSortedSet<T> {
    private final SortedSet<T> backingSortedSet;

    StandardImplForwardingSortedSet(SortedSet<T> backingSortedSet) {
      this.backingSortedSet = backingSortedSet;
    }

    @Override
    protected SortedSet<T> delegate() {
      return backingSortedSet;
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

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
      return standardSubSet(fromElement, toElement);
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTestSuite(ForwardingSortedSetTest.class);
    suite.addTest(
        SortedSetTestSuiteBuilder.using(
                new TestStringSortedSetGenerator() {
                  @Override
                  protected SortedSet<String> create(String[] elements) {
                    return new StandardImplForwardingSortedSet<>(
                        new SafeTreeSet<String>(Arrays.asList(elements)));
                  }

                  @Override
                  public List<String> order(List<String> insertionOrder) {
                    return Lists.newArrayList(Sets.newTreeSet(insertionOrder));
                  }
                })
            .named("ForwardingSortedSet[SafeTreeSet] with standard implementations")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.GENERAL_PURPOSE)
            .createTestSuite());

    return suite;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public void testForwarding() {
    new ForwardingWrapperTester()
        .testForwarding(
            SortedSet.class,
            new Function<SortedSet, SortedSet>() {
              @Override
              public SortedSet apply(SortedSet delegate) {
                return wrap(delegate);
              }
            });
  }

  public void testEquals() {
    SortedSet<String> set1 = ImmutableSortedSet.of("one");
    SortedSet<String> set2 = ImmutableSortedSet.of("two");
    new EqualsTester()
        .addEqualityGroup(set1, wrap(set1), wrap(set1))
        .addEqualityGroup(set2, wrap(set2))
        .testEquals();
  }

  private static <T> SortedSet<T> wrap(final SortedSet<T> delegate) {
    return new ForwardingSortedSet<T>() {
      @Override
      protected SortedSet<T> delegate() {
        return delegate;
      }
    };
  }
}

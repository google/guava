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

package com.google.common.collect.testing;

import static com.google.common.collect.testing.Helpers.castOrCopyToList;
import static java.util.Collections.reverse;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.DerivedCollectionGenerators.Bound;
import com.google.common.collect.testing.DerivedCollectionGenerators.ForwardingTestMapGenerator;
import com.google.common.collect.testing.DerivedCollectionGenerators.SortedMapSubmapTestMapGenerator;
import com.google.common.collect.testing.features.Feature;
import com.google.common.collect.testing.testers.NavigableMapNavigationTester;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.SortedMap;
import junit.framework.TestSuite;

/**
 * Creates, based on your criteria, a JUnit test suite that exhaustively tests a NavigableMap
 * implementation.
 */
@GwtIncompatible
public class NavigableMapTestSuiteBuilder<K, V> extends SortedMapTestSuiteBuilder<K, V> {
  public static <K, V> NavigableMapTestSuiteBuilder<K, V> using(
      TestSortedMapGenerator<K, V> generator) {
    NavigableMapTestSuiteBuilder<K, V> result = new NavigableMapTestSuiteBuilder<>();
    result.usingGenerator(generator);
    return result;
  }

  @Override
  protected List<Class<? extends AbstractTester>> getTesters() {
    List<Class<? extends AbstractTester>> testers = Helpers.copyToList(super.getTesters());
    testers.add(NavigableMapNavigationTester.class);
    return testers;
  }

  @Override
  protected List<TestSuite> createDerivedSuites(
      FeatureSpecificTestSuiteBuilder<
              ?, ? extends OneSizeTestContainerGenerator<Map<K, V>, Entry<K, V>>>
          parentBuilder) {
    List<TestSuite> derivedSuites = super.createDerivedSuites(parentBuilder);

    if (!parentBuilder.getFeatures().contains(NoRecurse.DESCENDING)) {
      derivedSuites.add(createDescendingSuite(parentBuilder));
    }

    if (!parentBuilder.getFeatures().contains(NoRecurse.SUBMAP)) {
      // Other combinations are inherited from SortedMapTestSuiteBuilder.
      derivedSuites.add(createSubmapSuite(parentBuilder, Bound.NO_BOUND, Bound.INCLUSIVE));
      derivedSuites.add(createSubmapSuite(parentBuilder, Bound.EXCLUSIVE, Bound.NO_BOUND));
      derivedSuites.add(createSubmapSuite(parentBuilder, Bound.EXCLUSIVE, Bound.EXCLUSIVE));
      derivedSuites.add(createSubmapSuite(parentBuilder, Bound.EXCLUSIVE, Bound.INCLUSIVE));
      derivedSuites.add(createSubmapSuite(parentBuilder, Bound.INCLUSIVE, Bound.INCLUSIVE));
    }

    return derivedSuites;
  }

  @Override
  protected NavigableSetTestSuiteBuilder<K> createDerivedKeySetSuite(
      TestSetGenerator<K> keySetGenerator) {
    return NavigableSetTestSuiteBuilder.using((TestSortedSetGenerator<K>) keySetGenerator);
  }

  public static final class NavigableMapSubmapTestMapGenerator<K, V>
      extends SortedMapSubmapTestMapGenerator<K, V> {
    public NavigableMapSubmapTestMapGenerator(
        TestSortedMapGenerator<K, V> delegate, Bound to, Bound from) {
      super(delegate, to, from);
    }

    @Override
    NavigableMap<K, V> createSubMap(SortedMap<K, V> sortedMap, K firstExclusive, K lastExclusive) {
      NavigableMap<K, V> map = (NavigableMap<K, V>) sortedMap;
      if (from == Bound.NO_BOUND && to == Bound.INCLUSIVE) {
        return map.headMap(lastInclusive, true);
      } else if (from == Bound.EXCLUSIVE && to == Bound.NO_BOUND) {
        return map.tailMap(firstExclusive, false);
      } else if (from == Bound.EXCLUSIVE && to == Bound.EXCLUSIVE) {
        return map.subMap(firstExclusive, false, lastExclusive, false);
      } else if (from == Bound.EXCLUSIVE && to == Bound.INCLUSIVE) {
        return map.subMap(firstExclusive, false, lastInclusive, true);
      } else if (from == Bound.INCLUSIVE && to == Bound.INCLUSIVE) {
        return map.subMap(firstInclusive, true, lastInclusive, true);
      } else {
        return (NavigableMap<K, V>) super.createSubMap(map, firstExclusive, lastExclusive);
      }
    }
  }

  @Override
  public NavigableMapTestSuiteBuilder<K, V> newBuilderUsing(
      TestSortedMapGenerator<K, V> delegate, Bound to, Bound from) {
    return subSuiteUsing(new NavigableMapSubmapTestMapGenerator<K, V>(delegate, to, from));
  }

  /** Create a suite whose maps are descending views of other maps. */
  private TestSuite createDescendingSuite(
      final FeatureSpecificTestSuiteBuilder<
              ?, ? extends OneSizeTestContainerGenerator<Map<K, V>, Entry<K, V>>>
          parentBuilder) {
    final TestSortedMapGenerator<K, V> delegate =
        (TestSortedMapGenerator<K, V>) parentBuilder.getSubjectGenerator().getInnerGenerator();

    List<Feature<?>> features = new ArrayList<>();
    features.add(NoRecurse.DESCENDING);
    features.addAll(parentBuilder.getFeatures());

    return subSuiteUsing(new DescendingTestMapGenerator<K, V>(delegate))
        .named(parentBuilder.getName() + " descending")
        .withFeatures(features)
        .suppressing(parentBuilder.getSuppressedTests())
        .createTestSuite();
  }

  NavigableMapTestSuiteBuilder<K, V> subSuiteUsing(TestSortedMapGenerator<K, V> generator) {
    return using(generator);
  }

  static class DescendingTestMapGenerator<K, V> extends ForwardingTestMapGenerator<K, V>
      implements TestSortedMapGenerator<K, V> {
    DescendingTestMapGenerator(TestSortedMapGenerator<K, V> delegate) {
      super(delegate);
    }

    @Override
    public NavigableMap<K, V> create(Object... entries) {
      NavigableMap<K, V> map = (NavigableMap<K, V>) delegate.create(entries);
      return map.descendingMap();
    }

    @Override
    public Iterable<Entry<K, V>> order(List<Entry<K, V>> insertionOrder) {
      insertionOrder = castOrCopyToList(delegate.order(insertionOrder));
      reverse(insertionOrder);
      return insertionOrder;
    }

    TestSortedMapGenerator<K, V> delegate() {
      return (TestSortedMapGenerator<K, V>) delegate;
    }

    @Override
    public Entry<K, V> belowSamplesLesser() {
      return delegate().aboveSamplesGreater();
    }

    @Override
    public Entry<K, V> belowSamplesGreater() {
      return delegate().aboveSamplesLesser();
    }

    @Override
    public Entry<K, V> aboveSamplesLesser() {
      return delegate().belowSamplesGreater();
    }

    @Override
    public Entry<K, V> aboveSamplesGreater() {
      return delegate().belowSamplesLesser();
    }
  }
}

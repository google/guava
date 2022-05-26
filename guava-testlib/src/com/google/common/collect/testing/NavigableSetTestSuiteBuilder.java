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

import static com.google.common.collect.testing.features.CollectionFeature.DESCENDING_VIEW;
import static com.google.common.collect.testing.features.CollectionFeature.SUBSET_VIEW;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.DerivedCollectionGenerators.Bound;
import com.google.common.collect.testing.DerivedCollectionGenerators.SortedSetSubsetTestSetGenerator;
import com.google.common.collect.testing.features.Feature;
import com.google.common.collect.testing.testers.NavigableSetNavigationTester;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import junit.framework.TestSuite;

/**
 * Creates, based on your criteria, a JUnit test suite that exhaustively tests a NavigableSet
 * implementation.
 */
@GwtIncompatible
public final class NavigableSetTestSuiteBuilder<E> extends SortedSetTestSuiteBuilder<E> {
  public static <E> NavigableSetTestSuiteBuilder<E> using(TestSortedSetGenerator<E> generator) {
    NavigableSetTestSuiteBuilder<E> builder = new NavigableSetTestSuiteBuilder<>();
    builder.usingGenerator(generator);
    return builder;
  }

  @Override
  protected List<TestSuite> createDerivedSuites(
      FeatureSpecificTestSuiteBuilder<?, ? extends OneSizeTestContainerGenerator<Collection<E>, E>>
          parentBuilder) {
    List<TestSuite> derivedSuites = new ArrayList<>(super.createDerivedSuites(parentBuilder));

    if (!parentBuilder.getFeatures().contains(SUBSET_VIEW)) {
      // Other combinations are inherited from SortedSetTestSuiteBuilder.
      derivedSuites.add(createSubsetSuite(parentBuilder, Bound.NO_BOUND, Bound.INCLUSIVE));
      derivedSuites.add(createSubsetSuite(parentBuilder, Bound.EXCLUSIVE, Bound.NO_BOUND));
      derivedSuites.add(createSubsetSuite(parentBuilder, Bound.EXCLUSIVE, Bound.EXCLUSIVE));
      derivedSuites.add(createSubsetSuite(parentBuilder, Bound.EXCLUSIVE, Bound.INCLUSIVE));
      derivedSuites.add(createSubsetSuite(parentBuilder, Bound.INCLUSIVE, Bound.INCLUSIVE));
    }
    if (!parentBuilder.getFeatures().contains(DESCENDING_VIEW)) {
      derivedSuites.add(createDescendingSuite(parentBuilder));
    }
    return derivedSuites;
  }

  public static final class NavigableSetSubsetTestSetGenerator<E>
      extends SortedSetSubsetTestSetGenerator<E> {
    public NavigableSetSubsetTestSetGenerator(
        TestSortedSetGenerator<E> delegate, Bound to, Bound from) {
      super(delegate, to, from);
    }

    @Override
    NavigableSet<E> createSubSet(SortedSet<E> sortedSet, E firstExclusive, E lastExclusive) {
      NavigableSet<E> set = (NavigableSet<E>) sortedSet;
      if (from == Bound.NO_BOUND && to == Bound.INCLUSIVE) {
        return set.headSet(lastInclusive, true);
      } else if (from == Bound.EXCLUSIVE && to == Bound.NO_BOUND) {
        return set.tailSet(firstExclusive, false);
      } else if (from == Bound.EXCLUSIVE && to == Bound.EXCLUSIVE) {
        return set.subSet(firstExclusive, false, lastExclusive, false);
      } else if (from == Bound.EXCLUSIVE && to == Bound.INCLUSIVE) {
        return set.subSet(firstExclusive, false, lastInclusive, true);
      } else if (from == Bound.INCLUSIVE && to == Bound.INCLUSIVE) {
        return set.subSet(firstInclusive, true, lastInclusive, true);
      } else {
        return (NavigableSet<E>) super.createSubSet(set, firstExclusive, lastExclusive);
      }
    }
  }

  @Override
  public NavigableSetTestSuiteBuilder<E> newBuilderUsing(
      TestSortedSetGenerator<E> delegate, Bound to, Bound from) {
    return using(new NavigableSetSubsetTestSetGenerator<E>(delegate, to, from));
  }

  /** Create a suite whose maps are descending views of other maps. */
  private TestSuite createDescendingSuite(
      FeatureSpecificTestSuiteBuilder<?, ? extends OneSizeTestContainerGenerator<Collection<E>, E>>
          parentBuilder) {
    TestSetGenerator<E> delegate =
        (TestSetGenerator<E>) parentBuilder.getSubjectGenerator().getInnerGenerator();

    List<Feature<?>> features = new ArrayList<>();
    features.add(DESCENDING_VIEW);
    features.addAll(parentBuilder.getFeatures());

    return NavigableSetTestSuiteBuilder.using(
            new TestSetGenerator<E>() {

              @Override
              public SampleElements<E> samples() {
                return delegate.samples();
              }

              @Override
              public E[] createArray(int length) {
                return delegate.createArray(length);
              }

              @Override
              public Iterable<E> order(List<E> insertionOrder) {
                List<E> list = new ArrayList<>();
                for (E e : delegate.order(insertionOrder)) {
                  list.add(e);
                }
                Collections.reverse(list);
                return list;
              }

              @Override
              public Set<E> create(Object... elements) {
                NavigableSet<E> navigableSet = (NavigableSet<E>) delegate.create(elements);
                return navigableSet.descendingSet();
              }
            })
        .named(parentBuilder.getName() + " descending")
        .withFeatures(features)
        .suppressing(parentBuilder.getSuppressedTests())
        .createTestSuite();
  }

  @Override
  protected List<Class<? extends AbstractTester>> getTesters() {
    List<Class<? extends AbstractTester>> testers = Helpers.copyToList(super.getTesters());
    testers.add(NavigableSetNavigationTester.class);
    return testers;
  }
}

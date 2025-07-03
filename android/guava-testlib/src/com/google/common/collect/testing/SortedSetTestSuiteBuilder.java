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

import static com.google.common.collect.testing.Helpers.copyToList;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.DerivedCollectionGenerators.Bound;
import com.google.common.collect.testing.DerivedCollectionGenerators.SortedSetSubsetTestSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.Feature;
import com.google.common.collect.testing.testers.CollectionAddAllTester;
import com.google.common.collect.testing.testers.CollectionAddTester;
import com.google.common.collect.testing.testers.SortedSetNavigationTester;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import junit.framework.TestSuite;

/**
 * Creates, based on your criteria, a JUnit test suite that exhaustively tests a SortedSet
 * implementation.
 */
@GwtIncompatible
public class SortedSetTestSuiteBuilder<E> extends SetTestSuiteBuilder<E> {
  public static <E> SortedSetTestSuiteBuilder<E> using(TestSortedSetGenerator<E> generator) {
    SortedSetTestSuiteBuilder<E> builder = new SortedSetTestSuiteBuilder<>();
    builder.usingGenerator(generator);
    return builder;
  }

  @SuppressWarnings("rawtypes") // class literals
  @Override
  protected List<Class<? extends AbstractTester>> getTesters() {
    List<Class<? extends AbstractTester>> testers = copyToList(super.getTesters());
    testers.add(SortedSetNavigationTester.class);
    return testers;
  }

  @Override
  public TestSuite createTestSuite() {
    if (!getFeatures().contains(CollectionFeature.KNOWN_ORDER)) {
      List<Feature<?>> features = copyToList(getFeatures());
      features.add(CollectionFeature.KNOWN_ORDER);
      withFeatures(features);
    }
    return super.createTestSuite();
  }

  @Override
  protected List<TestSuite> createDerivedSuites(
      FeatureSpecificTestSuiteBuilder<?, ? extends OneSizeTestContainerGenerator<Collection<E>, E>>
          parentBuilder) {
    List<TestSuite> derivedSuites = super.createDerivedSuites(parentBuilder);

    if (!parentBuilder.getFeatures().contains(CollectionFeature.SUBSET_VIEW)) {
      derivedSuites.add(createSubsetSuite(parentBuilder, Bound.NO_BOUND, Bound.EXCLUSIVE));
      derivedSuites.add(createSubsetSuite(parentBuilder, Bound.INCLUSIVE, Bound.NO_BOUND));
      derivedSuites.add(createSubsetSuite(parentBuilder, Bound.INCLUSIVE, Bound.EXCLUSIVE));
    }

    return derivedSuites;
  }

  /**
   * Creates a suite whose set has some elements filtered out of view.
   *
   * <p>Because the set may be ascending or descending, this test must derive the relative order of
   * these extreme values rather than relying on their regular sort ordering.
   */
  final TestSuite createSubsetSuite(
      FeatureSpecificTestSuiteBuilder<?, ? extends OneSizeTestContainerGenerator<Collection<E>, E>>
          parentBuilder,
      Bound from,
      Bound to) {
    TestSortedSetGenerator<E> delegate =
        (TestSortedSetGenerator<E>) parentBuilder.getSubjectGenerator().getInnerGenerator();

    List<Feature<?>> features = new ArrayList<>(parentBuilder.getFeatures());
    Set<Method> suppressing = new HashSet<>(parentBuilder.getSuppressedTests());
    features.add(CollectionFeature.SUBSET_VIEW);
    if (features.remove(CollectionFeature.ALLOWS_NULL_VALUES)) {
      // the null value might be out of bounds, so we can't always construct a subset with nulls
      features.add(CollectionFeature.ALLOWS_NULL_QUERIES);
      // but add null might still be supported if it happens to be within range of the subset
      suppressing.add(CollectionAddTester.getAddNullUnsupportedMethod());
      suppressing.add(CollectionAddAllTester.getAddAllNullUnsupportedMethod());
    }

    return newBuilderUsing(delegate, to, from)
        .named(parentBuilder.getName() + " subSet " + from + "-" + to)
        .withFeatures(features)
        .suppressing(suppressing)
        .withSetUp(parentBuilder.getSetUp())
        .withTearDown(parentBuilder.getTearDown())
        .createTestSuite();
  }

  /** Like using() but overrideable by NavigableSetTestSuiteBuilder. */
  SortedSetTestSuiteBuilder<E> newBuilderUsing(
      TestSortedSetGenerator<E> delegate, Bound to, Bound from) {
    return using(new SortedSetSubsetTestSetGenerator<E>(delegate, to, from));
  }
}

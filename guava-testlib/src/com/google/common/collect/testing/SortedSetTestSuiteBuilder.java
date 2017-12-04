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

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.DerivedCollectionGenerators.Bound;
import com.google.common.collect.testing.DerivedCollectionGenerators.SortedSetSubsetTestSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.Feature;
import com.google.common.collect.testing.testers.SortedSetNavigationTester;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import junit.framework.TestSuite;

/**
 * Creates, based on your criteria, a JUnit test suite that exhaustively tests a SortedSet
 * implementation.
 */
@GwtIncompatible
public class SortedSetTestSuiteBuilder<E> extends SetTestSuiteBuilder<E> {
  public static <E> SortedSetTestSuiteBuilder<E> using(TestSortedSetGenerator<E> generator) {
    SortedSetTestSuiteBuilder<E> builder = new SortedSetTestSuiteBuilder<E>();
    builder.usingGenerator(generator);
    return builder;
  }

  @Override
  protected List<Class<? extends AbstractTester>> getTesters() {
    List<Class<? extends AbstractTester>> testers = Helpers.copyToList(super.getTesters());
    testers.add(SortedSetNavigationTester.class);
    return testers;
  }

  @Override
  public TestSuite createTestSuite() {
    if (!getFeatures().contains(CollectionFeature.KNOWN_ORDER)) {
      List<Feature<?>> features = Helpers.copyToList(getFeatures());
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
      final FeatureSpecificTestSuiteBuilder<
              ?, ? extends OneSizeTestContainerGenerator<Collection<E>, E>>
          parentBuilder,
      final Bound from,
      final Bound to) {
    final TestSortedSetGenerator<E> delegate =
        (TestSortedSetGenerator<E>) parentBuilder.getSubjectGenerator().getInnerGenerator();

    List<Feature<?>> features = new ArrayList<>();
    features.addAll(parentBuilder.getFeatures());
    features.remove(CollectionFeature.ALLOWS_NULL_VALUES);
    features.add(CollectionFeature.SUBSET_VIEW);

    return newBuilderUsing(delegate, to, from)
        .named(parentBuilder.getName() + " subSet " + from + "-" + to)
        .withFeatures(features)
        .suppressing(parentBuilder.getSuppressedTests())
        .createTestSuite();
  }

  /** Like using() but overrideable by NavigableSetTestSuiteBuilder. */
  SortedSetTestSuiteBuilder<E> newBuilderUsing(
      TestSortedSetGenerator<E> delegate, Bound to, Bound from) {
    return using(new SortedSetSubsetTestSetGenerator<E>(delegate, to, from));
  }
}

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

import static com.google.common.collect.testing.features.CollectionFeature.KNOWN_ORDER;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.DerivedCollectionGenerators.Bound;
import com.google.common.collect.testing.DerivedCollectionGenerators.SortedMapSubmapTestMapGenerator;
import com.google.common.collect.testing.features.Feature;
import com.google.common.collect.testing.testers.SortedMapNavigationTester;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import junit.framework.TestSuite;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Creates, based on your criteria, a JUnit test suite that exhaustively tests a SortedMap
 * implementation.
 */
@GwtIncompatible
public class SortedMapTestSuiteBuilder<K, V> extends MapTestSuiteBuilder<K, V> {
  public static <K, V> SortedMapTestSuiteBuilder<K, V> using(
      TestSortedMapGenerator<K, V> generator) {
    SortedMapTestSuiteBuilder<K, V> result = new SortedMapTestSuiteBuilder<>();
    result.usingGenerator(generator);
    return result;
  }

  @Override
  protected List<Class<? extends AbstractTester>> getTesters() {
    List<Class<? extends AbstractTester>> testers = Helpers.copyToList(super.getTesters());
    testers.add(SortedMapNavigationTester.class);
    return testers;
  }

  @Override
  public TestSuite createTestSuite() {
    if (!getFeatures().contains(KNOWN_ORDER)) {
      List<Feature<?>> features = Helpers.copyToList(getFeatures());
      features.add(KNOWN_ORDER);
      withFeatures(features);
    }
    return super.createTestSuite();
  }

  @Override
  protected List<TestSuite> createDerivedSuites(
      FeatureSpecificTestSuiteBuilder<
              ?, ? extends OneSizeTestContainerGenerator<Map<K, V>, Entry<K, V>>>
          parentBuilder) {
    List<TestSuite> derivedSuites = super.createDerivedSuites(parentBuilder);

    if (!parentBuilder.getFeatures().contains(NoRecurse.SUBMAP)) {
      derivedSuites.add(createSubmapSuite(parentBuilder, Bound.NO_BOUND, Bound.EXCLUSIVE));
      derivedSuites.add(createSubmapSuite(parentBuilder, Bound.INCLUSIVE, Bound.NO_BOUND));
      derivedSuites.add(createSubmapSuite(parentBuilder, Bound.INCLUSIVE, Bound.EXCLUSIVE));
    }

    return derivedSuites;
  }

  @Override
  protected SetTestSuiteBuilder<K> createDerivedKeySetSuite(TestSetGenerator<K> keySetGenerator) {
    return keySetGenerator instanceof TestSortedSetGenerator
        ? SortedSetTestSuiteBuilder.using((TestSortedSetGenerator<K>) keySetGenerator)
        : SetTestSuiteBuilder.using(keySetGenerator);
  }

  /**
   * To avoid infinite recursion, test suites with these marker features won't have derived suites
   * created for them.
   */
  enum NoRecurse implements Feature<@Nullable Void> {
    SUBMAP,
    DESCENDING;

    @Override
    public Set<Feature<? super @Nullable Void>> getImpliedFeatures() {
      return Collections.emptySet();
    }
  }

  /**
   * Creates a suite whose map has some elements filtered out of view.
   *
   * <p>Because the map may be ascending or descending, this test must derive the relative order of
   * these extreme values rather than relying on their regular sort ordering.
   */
  final TestSuite createSubmapSuite(
      FeatureSpecificTestSuiteBuilder<
              ?, ? extends OneSizeTestContainerGenerator<Map<K, V>, Entry<K, V>>>
          parentBuilder,
      Bound from,
      Bound to) {
    TestSortedMapGenerator<K, V> delegate =
        (TestSortedMapGenerator<K, V>) parentBuilder.getSubjectGenerator().getInnerGenerator();

    List<Feature<?>> features = new ArrayList<>();
    features.add(NoRecurse.SUBMAP);
    features.addAll(parentBuilder.getFeatures());

    return newBuilderUsing(delegate, to, from)
        .named(parentBuilder.getName() + " subMap " + from + "-" + to)
        .withFeatures(features)
        .suppressing(parentBuilder.getSuppressedTests())
        .withSetUp(parentBuilder.getSetUp())
        .withTearDown(parentBuilder.getTearDown())
        .createTestSuite();
  }

  /** Like using() but overrideable by NavigableMapTestSuiteBuilder. */
  SortedMapTestSuiteBuilder<K, V> newBuilderUsing(
      TestSortedMapGenerator<K, V> delegate, Bound to, Bound from) {
    return using(new SortedMapSubmapTestMapGenerator<K, V>(delegate, to, from));
  }
}

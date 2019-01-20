/*
 * Copyright (C) 2012 The Guava Authors
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

package com.google.common.collect.testing.google;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.testing.AbstractTester;
import com.google.common.collect.testing.FeatureSpecificTestSuiteBuilder;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.ListTestSuiteBuilder;
import com.google.common.collect.testing.OneSizeTestContainerGenerator;
import com.google.common.collect.testing.TestListGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.Feature;
import com.google.common.collect.testing.features.ListFeature;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import junit.framework.TestSuite;

/**
 * Creates, based on your criteria, a JUnit test suite that exhaustively tests a {@code
 * ListMultimap} implementation.
 *
 * @author Louis Wasserman
 */
@GwtIncompatible
public class ListMultimapTestSuiteBuilder<K, V>
    extends MultimapTestSuiteBuilder<K, V, ListMultimap<K, V>> {

  public static <K, V> ListMultimapTestSuiteBuilder<K, V> using(
      TestListMultimapGenerator<K, V> generator) {
    ListMultimapTestSuiteBuilder<K, V> result = new ListMultimapTestSuiteBuilder<>();
    result.usingGenerator(generator);
    return result;
  }

  @Override
  protected List<Class<? extends AbstractTester>> getTesters() {
    List<Class<? extends AbstractTester>> testers = Helpers.copyToList(super.getTesters());
    testers.add(ListMultimapAsMapTester.class);
    testers.add(ListMultimapEqualsTester.class);
    testers.add(ListMultimapPutTester.class);
    testers.add(ListMultimapPutAllTester.class);
    testers.add(ListMultimapRemoveTester.class);
    testers.add(ListMultimapReplaceValuesTester.class);
    return testers;
  }

  @Override
  TestSuite computeMultimapGetTestSuite(
      FeatureSpecificTestSuiteBuilder<
              ?, ? extends OneSizeTestContainerGenerator<ListMultimap<K, V>, Entry<K, V>>>
          parentBuilder) {
    return ListTestSuiteBuilder.using(
            new MultimapGetGenerator<K, V>(parentBuilder.getSubjectGenerator()))
        .withFeatures(computeMultimapGetFeatures(parentBuilder.getFeatures()))
        .named(parentBuilder.getName() + ".get[key]")
        .suppressing(parentBuilder.getSuppressedTests())
        .createTestSuite();
  }

  @Override
  TestSuite computeMultimapAsMapGetTestSuite(
      FeatureSpecificTestSuiteBuilder<
              ?, ? extends OneSizeTestContainerGenerator<ListMultimap<K, V>, Entry<K, V>>>
          parentBuilder) {
    Set<Feature<?>> features = computeMultimapAsMapGetFeatures(parentBuilder.getFeatures());
    if (Collections.disjoint(features, EnumSet.allOf(CollectionSize.class))) {
      return new TestSuite();
    } else {
      return ListTestSuiteBuilder.using(
              new MultimapAsMapGetGenerator<K, V>(parentBuilder.getSubjectGenerator()))
          .withFeatures(features)
          .named(parentBuilder.getName() + ".asMap[].get[key]")
          .suppressing(parentBuilder.getSuppressedTests())
          .createTestSuite();
    }
  }

  @Override
  Set<Feature<?>> computeMultimapGetFeatures(Set<Feature<?>> multimapFeatures) {
    Set<Feature<?>> derivedFeatures = super.computeMultimapGetFeatures(multimapFeatures);
    if (derivedFeatures.contains(CollectionFeature.SUPPORTS_ADD)) {
      derivedFeatures.add(ListFeature.SUPPORTS_ADD_WITH_INDEX);
    }
    if (derivedFeatures.contains(CollectionFeature.GENERAL_PURPOSE)) {
      derivedFeatures.add(ListFeature.GENERAL_PURPOSE);
    }
    return derivedFeatures;
  }

  private static class MultimapGetGenerator<K, V>
      extends MultimapTestSuiteBuilder.MultimapGetGenerator<K, V, ListMultimap<K, V>>
      implements TestListGenerator<V> {
    public MultimapGetGenerator(
        OneSizeTestContainerGenerator<ListMultimap<K, V>, Entry<K, V>> multimapGenerator) {
      super(multimapGenerator);
    }

    @Override
    public List<V> create(Object... elements) {
      return (List<V>) super.create(elements);
    }
  }

  private static class MultimapAsMapGetGenerator<K, V>
      extends MultimapTestSuiteBuilder.MultimapAsMapGetGenerator<K, V, ListMultimap<K, V>>
      implements TestListGenerator<V> {
    public MultimapAsMapGetGenerator(
        OneSizeTestContainerGenerator<ListMultimap<K, V>, Entry<K, V>> multimapGenerator) {
      super(multimapGenerator);
    }

    @Override
    public List<V> create(Object... elements) {
      return (List<V>) super.create(elements);
    }
  }
}

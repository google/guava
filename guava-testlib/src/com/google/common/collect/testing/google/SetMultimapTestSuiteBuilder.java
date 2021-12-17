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
import com.google.common.collect.SetMultimap;
import com.google.common.collect.testing.AbstractTester;
import com.google.common.collect.testing.FeatureSpecificTestSuiteBuilder;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.OneSizeTestContainerGenerator;
import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.TestSetGenerator;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.Feature;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import junit.framework.TestSuite;

/**
 * Creates, based on your criteria, a JUnit test suite that exhaustively tests a {@code SetMultimap}
 * implementation.
 *
 * @author Louis Wasserman
 */
@GwtIncompatible
public class SetMultimapTestSuiteBuilder<K, V>
    extends MultimapTestSuiteBuilder<K, V, SetMultimap<K, V>> {

  public static <K, V> SetMultimapTestSuiteBuilder<K, V> using(
      TestSetMultimapGenerator<K, V> generator) {
    SetMultimapTestSuiteBuilder<K, V> result = new SetMultimapTestSuiteBuilder<>();
    result.usingGenerator(generator);
    return result;
  }

  @Override
  protected List<Class<? extends AbstractTester>> getTesters() {
    List<Class<? extends AbstractTester>> testers = Helpers.copyToList(super.getTesters());
    testers.add(SetMultimapAsMapTester.class);
    testers.add(SetMultimapEqualsTester.class);
    testers.add(SetMultimapPutTester.class);
    testers.add(SetMultimapPutAllTester.class);
    testers.add(SetMultimapReplaceValuesTester.class);
    return testers;
  }

  @Override
  TestSuite computeMultimapGetTestSuite(
      FeatureSpecificTestSuiteBuilder<
              ?, ? extends OneSizeTestContainerGenerator<SetMultimap<K, V>, Entry<K, V>>>
          parentBuilder) {
    return SetTestSuiteBuilder.using(
            new MultimapGetGenerator<K, V>(parentBuilder.getSubjectGenerator()))
        .withFeatures(computeMultimapGetFeatures(parentBuilder.getFeatures()))
        .named(parentBuilder.getName() + ".get[key]")
        .suppressing(parentBuilder.getSuppressedTests())
        .createTestSuite();
  }

  @Override
  TestSuite computeMultimapAsMapGetTestSuite(
      FeatureSpecificTestSuiteBuilder<
              ?, ? extends OneSizeTestContainerGenerator<SetMultimap<K, V>, Entry<K, V>>>
          parentBuilder) {
    Set<Feature<?>> features = computeMultimapAsMapGetFeatures(parentBuilder.getFeatures());
    if (Collections.disjoint(features, EnumSet.allOf(CollectionSize.class))) {
      return new TestSuite();
    } else {
      return SetTestSuiteBuilder.using(
              new MultimapAsMapGetGenerator<K, V>(parentBuilder.getSubjectGenerator()))
          .withFeatures(features)
          .named(parentBuilder.getName() + ".asMap[].get[key]")
          .suppressing(parentBuilder.getSuppressedTests())
          .createTestSuite();
    }
  }

  @Override
  TestSuite computeEntriesTestSuite(
      FeatureSpecificTestSuiteBuilder<
              ?, ? extends OneSizeTestContainerGenerator<SetMultimap<K, V>, Entry<K, V>>>
          parentBuilder) {
    return SetTestSuiteBuilder.using(
            new EntriesGenerator<K, V>(parentBuilder.getSubjectGenerator()))
        .withFeatures(computeEntriesFeatures(parentBuilder.getFeatures()))
        .named(parentBuilder.getName() + ".entries")
        .suppressing(parentBuilder.getSuppressedTests())
        .createTestSuite();
  }

  private static class EntriesGenerator<K, V>
      extends MultimapTestSuiteBuilder.EntriesGenerator<K, V, SetMultimap<K, V>>
      implements TestSetGenerator<Entry<K, V>> {

    public EntriesGenerator(
        OneSizeTestContainerGenerator<SetMultimap<K, V>, Entry<K, V>> multimapGenerator) {
      super(multimapGenerator);
    }

    @Override
    public Set<Entry<K, V>> create(Object... elements) {
      return (Set<Entry<K, V>>) super.create(elements);
    }
  }

  static class MultimapGetGenerator<K, V>
      extends MultimapTestSuiteBuilder.MultimapGetGenerator<K, V, SetMultimap<K, V>>
      implements TestSetGenerator<V> {
    public MultimapGetGenerator(
        OneSizeTestContainerGenerator<SetMultimap<K, V>, Entry<K, V>> multimapGenerator) {
      super(multimapGenerator);
    }

    @Override
    public Set<V> create(Object... elements) {
      return (Set<V>) super.create(elements);
    }
  }

  static class MultimapAsMapGetGenerator<K, V>
      extends MultimapTestSuiteBuilder.MultimapAsMapGetGenerator<K, V, SetMultimap<K, V>>
      implements TestSetGenerator<V> {
    public MultimapAsMapGetGenerator(
        OneSizeTestContainerGenerator<SetMultimap<K, V>, Entry<K, V>> multimapGenerator) {
      super(multimapGenerator);
    }

    @Override
    public Set<V> create(Object... elements) {
      return (Set<V>) super.create(elements);
    }
  }
}

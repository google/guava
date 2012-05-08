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

import com.google.common.collect.SetMultimap;
import com.google.common.collect.testing.FeatureSpecificTestSuiteBuilder;
import com.google.common.collect.testing.OneSizeTestContainerGenerator;
import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.TestSetGenerator;

import junit.framework.TestSuite;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Creates, based on your criteria, a JUnit test suite that exhaustively tests
 * a {@code SetMultimap} implementation.
 *
 * @author Louis Wasserman
 */
public class SetMultimapTestSuiteBuilder<K, V>
    extends MultimapTestSuiteBuilder<K, V, SetMultimap<K, V>> {

  public static <K, V> SetMultimapTestSuiteBuilder<K, V> using(
      TestSetMultimapGenerator<K, V> generator) {
    SetMultimapTestSuiteBuilder<K, V> result = new SetMultimapTestSuiteBuilder<K, V>();
    result.usingGenerator(generator);
    return result;
  }

  @Override
  TestSuite computeMultimapGetTestSuite(
      FeatureSpecificTestSuiteBuilder<?, ? extends
      OneSizeTestContainerGenerator<SetMultimap<K, V>, Entry<K, V>>> parentBuilder) {
    return SetTestSuiteBuilder.using(
        new MultimapGetGenerator<K, V>(parentBuilder.getSubjectGenerator()))
        .withFeatures(computeMultimapGetFeatures(parentBuilder.getFeatures()))
        .named(parentBuilder.getName() + ".get[key]")
        .suppressing(parentBuilder.getSuppressedTests())
        .createTestSuite();
  }

  @Override
  TestSuite computeEntriesTestSuite(
      FeatureSpecificTestSuiteBuilder<?, ?  extends
          OneSizeTestContainerGenerator<SetMultimap<K, V>, Map.Entry<K, V>>> parentBuilder) {
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

  private static class MultimapGetGenerator<K, V>
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
}

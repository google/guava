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
import com.google.common.collect.testing.SortedSetTestSuiteBuilder;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.Feature;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import junit.framework.TestSuite;

/**
 * Creates, based on your criteria, a JUnit test suite that exhaustively tests a {@code
 * SortedSetMultimap} implementation.
 *
 * @author Louis Wasserman
 */
@GwtIncompatible
public class SortedSetMultimapTestSuiteBuilder<K, V>
    extends MultimapTestSuiteBuilder<K, V, SetMultimap<K, V>> {

  public static <K, V> SortedSetMultimapTestSuiteBuilder<K, V> using(
      TestSetMultimapGenerator<K, V> generator) {
    SortedSetMultimapTestSuiteBuilder<K, V> result = new SortedSetMultimapTestSuiteBuilder<>();
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
    testers.add(SortedSetMultimapAsMapTester.class);
    testers.add(SortedSetMultimapGetTester.class);
    return testers;
  }

  @Override
  TestSuite computeMultimapGetTestSuite(
      FeatureSpecificTestSuiteBuilder<
              ?, ? extends OneSizeTestContainerGenerator<SetMultimap<K, V>, Entry<K, V>>>
          parentBuilder) {
    return SortedSetTestSuiteBuilder.using(
            new SetMultimapTestSuiteBuilder.MultimapGetGenerator<K, V>(
                parentBuilder.getSubjectGenerator()))
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
      return SortedSetTestSuiteBuilder.using(
              new SetMultimapTestSuiteBuilder.MultimapAsMapGetGenerator<K, V>(
                  parentBuilder.getSubjectGenerator()))
          .withFeatures(features)
          .named(parentBuilder.getName() + ".asMap[].get[key]")
          .suppressing(parentBuilder.getSuppressedTests())
          .createTestSuite();
    }
  }
}

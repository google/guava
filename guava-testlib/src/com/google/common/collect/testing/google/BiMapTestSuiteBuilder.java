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
import com.google.common.collect.BiMap;
import com.google.common.collect.testing.AbstractTester;
import com.google.common.collect.testing.FeatureSpecificTestSuiteBuilder;
import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.OneSizeTestContainerGenerator;
import com.google.common.collect.testing.PerCollectionSizeTestSuiteBuilder;
import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.Feature;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.google.DerivedGoogleCollectionGenerators.BiMapValueSetGenerator;
import com.google.common.collect.testing.google.DerivedGoogleCollectionGenerators.InverseBiMapGenerator;
import com.google.common.collect.testing.google.DerivedGoogleCollectionGenerators.MapGenerator;
import com.google.common.collect.testing.testers.SetCreationTester;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import junit.framework.TestSuite;

/**
 * Creates, based on your criteria, a JUnit test suite that exhaustively tests a {@code BiMap}
 * implementation.
 *
 * @author Louis Wasserman
 */
@GwtIncompatible
public class BiMapTestSuiteBuilder<K, V>
    extends PerCollectionSizeTestSuiteBuilder<
        BiMapTestSuiteBuilder<K, V>, TestBiMapGenerator<K, V>, BiMap<K, V>, Entry<K, V>> {
  public static <K, V> BiMapTestSuiteBuilder<K, V> using(TestBiMapGenerator<K, V> generator) {
    return new BiMapTestSuiteBuilder<K, V>().usingGenerator(generator);
  }

  @Override
  protected List<Class<? extends AbstractTester>> getTesters() {
    List<Class<? extends AbstractTester>> testers = new ArrayList<>();
    testers.add(BiMapEntrySetTester.class);
    testers.add(BiMapPutTester.class);
    testers.add(BiMapInverseTester.class);
    testers.add(BiMapRemoveTester.class);
    testers.add(BiMapClearTester.class);
    return testers;
  }

  enum NoRecurse implements Feature<Void> {
    INVERSE;

    @Override
    public Set<Feature<? super Void>> getImpliedFeatures() {
      return Collections.emptySet();
    }
  }

  @Override
  protected List<TestSuite> createDerivedSuites(
      FeatureSpecificTestSuiteBuilder<
              ?, ? extends OneSizeTestContainerGenerator<BiMap<K, V>, Entry<K, V>>>
          parentBuilder) {
    List<TestSuite> derived = super.createDerivedSuites(parentBuilder);
    // TODO(cpovirk): consider using this approach (derived suites instead of extension) in
    // ListTestSuiteBuilder, etc.?
    derived.add(
        MapTestSuiteBuilder.using(new MapGenerator<K, V>(parentBuilder.getSubjectGenerator()))
            .withFeatures(parentBuilder.getFeatures())
            .named(parentBuilder.getName() + " [Map]")
            .suppressing(parentBuilder.getSuppressedTests())
            .suppressing(SetCreationTester.class.getMethods())
            // BiMap.entrySet() duplicate-handling behavior is too confusing for SetCreationTester
            .createTestSuite());
    /*
     * TODO(cpovirk): the Map tests duplicate most of this effort by using a
     * CollectionTestSuiteBuilder on values(). It would be nice to avoid that
     */
    derived.add(
        SetTestSuiteBuilder.using(
                new BiMapValueSetGenerator<K, V>(parentBuilder.getSubjectGenerator()))
            .withFeatures(computeValuesSetFeatures(parentBuilder.getFeatures()))
            .named(parentBuilder.getName() + " values [Set]")
            .suppressing(parentBuilder.getSuppressedTests())
            .suppressing(SetCreationTester.class.getMethods())
            // BiMap.values() duplicate-handling behavior is too confusing for SetCreationTester
            .createTestSuite());
    if (!parentBuilder.getFeatures().contains(NoRecurse.INVERSE)) {
      derived.add(
          BiMapTestSuiteBuilder.using(
                  new InverseBiMapGenerator<K, V>(parentBuilder.getSubjectGenerator()))
              .withFeatures(computeInverseFeatures(parentBuilder.getFeatures()))
              .named(parentBuilder.getName() + " inverse")
              .suppressing(parentBuilder.getSuppressedTests())
              .createTestSuite());
    }

    return derived;
  }

  private static Set<Feature<?>> computeInverseFeatures(Set<Feature<?>> mapFeatures) {
    Set<Feature<?>> inverseFeatures = new HashSet<>(mapFeatures);

    boolean nullKeys = inverseFeatures.remove(MapFeature.ALLOWS_NULL_KEYS);
    boolean nullValues = inverseFeatures.remove(MapFeature.ALLOWS_NULL_VALUES);

    if (nullKeys) {
      inverseFeatures.add(MapFeature.ALLOWS_NULL_VALUES);
    }
    if (nullValues) {
      inverseFeatures.add(MapFeature.ALLOWS_NULL_KEYS);
    }

    inverseFeatures.add(NoRecurse.INVERSE);
    inverseFeatures.remove(CollectionFeature.KNOWN_ORDER);
    inverseFeatures.add(MapFeature.REJECTS_DUPLICATES_AT_CREATION);

    return inverseFeatures;
  }

  // TODO(lowasser): can we eliminate the duplication from MapTestSuiteBuilder here?

  private static Set<Feature<?>> computeValuesSetFeatures(Set<Feature<?>> mapFeatures) {
    Set<Feature<?>> valuesCollectionFeatures = computeCommonDerivedCollectionFeatures(mapFeatures);
    valuesCollectionFeatures.add(CollectionFeature.ALLOWS_NULL_QUERIES);

    if (mapFeatures.contains(MapFeature.ALLOWS_NULL_VALUES)) {
      valuesCollectionFeatures.add(CollectionFeature.ALLOWS_NULL_VALUES);
    }

    valuesCollectionFeatures.add(CollectionFeature.REJECTS_DUPLICATES_AT_CREATION);

    return valuesCollectionFeatures;
  }

  private static Set<Feature<?>> computeCommonDerivedCollectionFeatures(
      Set<Feature<?>> mapFeatures) {
    return MapTestSuiteBuilder.computeCommonDerivedCollectionFeatures(mapFeatures);
  }
}

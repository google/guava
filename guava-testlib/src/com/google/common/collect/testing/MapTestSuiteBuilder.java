/*
 * Copyright (C) 2008 The Guava Authors
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

import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.Feature;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.testers.MapClearTester;
import com.google.common.collect.testing.testers.MapContainsKeyTester;
import com.google.common.collect.testing.testers.MapContainsValueTester;
import com.google.common.collect.testing.testers.MapCreationTester;
import com.google.common.collect.testing.testers.MapEqualsTester;
import com.google.common.collect.testing.testers.MapGetTester;
import com.google.common.collect.testing.testers.MapHashCodeTester;
import com.google.common.collect.testing.testers.MapIsEmptyTester;
import com.google.common.collect.testing.testers.MapPutAllTester;
import com.google.common.collect.testing.testers.MapPutTester;
import com.google.common.collect.testing.testers.MapRemoveTester;
import com.google.common.collect.testing.testers.MapSizeTester;

import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Creates, based on your criteria, a JUnit test suite that exhaustively tests
 * a Map implementation.
 *
 * @author George van den Driessche
 */
public class MapTestSuiteBuilder<K, V>
    extends PerCollectionSizeTestSuiteBuilder<
        MapTestSuiteBuilder<K, V>,
        TestMapGenerator<K, V>, Map<K, V>, Map.Entry<K, V>> {
  public static <K, V> MapTestSuiteBuilder<K, V> using(
      TestMapGenerator<K, V> generator) {
    return new MapTestSuiteBuilder<K, V>().usingGenerator(generator);
  }

  @SuppressWarnings("unchecked") // Class parameters must be raw.
  @Override protected List<Class<? extends AbstractTester>> getTesters() {
    return Arrays.<Class<? extends AbstractTester>>asList(
        MapClearTester.class,
        MapContainsKeyTester.class,
        MapContainsValueTester.class,
        MapCreationTester.class,
        MapEqualsTester.class,
        MapGetTester.class,
        MapHashCodeTester.class,
        MapIsEmptyTester.class,
        MapPutTester.class,
        MapPutAllTester.class,
        MapRemoveTester.class,
        MapSizeTester.class
    );
  }

  @Override List<TestSuite> createDerivedSuites(
      FeatureSpecificTestSuiteBuilder<
          ?,
          ? extends OneSizeTestContainerGenerator<Map<K, V>, Map.Entry<K, V>>>
      parentBuilder) {
    // TODO: Once invariant support is added, supply invariants to each of the
    // derived suites, to check that mutations to the derived collections are
    // reflected in the underlying map.

    List<TestSuite> derivedSuites = super.createDerivedSuites(parentBuilder);

    derivedSuites.add(SetTestSuiteBuilder.using(
            new MapEntrySetGenerator<K, V>(parentBuilder.getSubjectGenerator()))
        .withFeatures(computeEntrySetFeatures(parentBuilder.getFeatures()))
        .named(parentBuilder.getName() + " entrySet")
        .suppressing(parentBuilder.getSuppressedTests())
        .createTestSuite());

    derivedSuites.add(createDerivedKeySetSuite(
            new MapKeySetGenerator<K, V>(parentBuilder.getSubjectGenerator()))
        .withFeatures(computeKeySetFeatures(parentBuilder.getFeatures()))
        .named(parentBuilder.getName() + " keys")
        .suppressing(parentBuilder.getSuppressedTests())
        .createTestSuite());

    derivedSuites.add(CollectionTestSuiteBuilder.using(
            new MapValueCollectionGenerator<K, V>(
                parentBuilder.getSubjectGenerator()))
        .named(parentBuilder.getName() + " values")
        .withFeatures(computeValuesCollectionFeatures(
            parentBuilder.getFeatures()))
        .suppressing(parentBuilder.getSuppressedTests())
        .createTestSuite());

    return derivedSuites;
  }

  protected SetTestSuiteBuilder<K> createDerivedKeySetSuite(TestSetGenerator<K> keySetGenerator) {
    return SetTestSuiteBuilder.using(keySetGenerator);
  }

  private static Set<Feature<?>> computeEntrySetFeatures(
      Set<Feature<?>> mapFeatures) {
    Set<Feature<?>> entrySetFeatures = 
        computeCommonDerivedCollectionFeatures(mapFeatures);
    entrySetFeatures.add(CollectionFeature.ALLOWS_NULL_QUERIES);
    return entrySetFeatures;
  }

  private static Set<Feature<?>> computeKeySetFeatures(
      Set<Feature<?>> mapFeatures) {
    Set<Feature<?>> keySetFeatures =
        computeCommonDerivedCollectionFeatures(mapFeatures);

    if (mapFeatures.contains(MapFeature.ALLOWS_NULL_KEYS)) {
      keySetFeatures.add(CollectionFeature.ALLOWS_NULL_VALUES);
    } else if (mapFeatures.contains(MapFeature.ALLOWS_NULL_QUERIES)) {
      keySetFeatures.add(CollectionFeature.ALLOWS_NULL_QUERIES);
    }

    return keySetFeatures;
  }

  private static Set<Feature<?>> computeValuesCollectionFeatures(
      Set<Feature<?>> mapFeatures) {
    Set<Feature<?>> valuesCollectionFeatures =
        computeCommonDerivedCollectionFeatures(mapFeatures);
    valuesCollectionFeatures.add(CollectionFeature.ALLOWS_NULL_QUERIES);

    if (mapFeatures.contains(MapFeature.ALLOWS_NULL_VALUES)) {
      valuesCollectionFeatures.add(CollectionFeature.ALLOWS_NULL_VALUES);
    }

    return valuesCollectionFeatures;
  }

  private static Set<Feature<?>> computeCommonDerivedCollectionFeatures(
      Set<Feature<?>> mapFeatures) {
    Set<Feature<?>> derivedFeatures = new HashSet<Feature<?>>();
    if (mapFeatures.contains(MapFeature.SUPPORTS_REMOVE)) {
      derivedFeatures.add(CollectionFeature.SUPPORTS_REMOVE);
      derivedFeatures.add(CollectionFeature.SUPPORTS_REMOVE_ALL);
      derivedFeatures.add(CollectionFeature.SUPPORTS_RETAIN_ALL);
    }
    if (mapFeatures.contains(MapFeature.SUPPORTS_CLEAR)) {
      derivedFeatures.add(CollectionFeature.SUPPORTS_CLEAR);
    }
    if (mapFeatures.contains(MapFeature.REJECTS_DUPLICATES_AT_CREATION)) {
      derivedFeatures.add(CollectionFeature.REJECTS_DUPLICATES_AT_CREATION);
    }
    // add the intersection of CollectionSize.values() and mapFeatures
    for (CollectionSize size : CollectionSize.values()) {
      if (mapFeatures.contains(size)) {
        derivedFeatures.add(size);
      }
    }
    return derivedFeatures;
  }

  private static class MapEntrySetGenerator<K, V>
      implements TestSetGenerator<Map.Entry<K, V>> {
    private final OneSizeTestContainerGenerator<Map<K, V>, Map.Entry<K, V>>
        mapGenerator;

    public MapEntrySetGenerator(
        OneSizeTestContainerGenerator<
            Map<K, V>, Map.Entry<K, V>> mapGenerator) {
      this.mapGenerator = mapGenerator;
    }

    @Override
    public SampleElements<Map.Entry<K, V>> samples() {
      return mapGenerator.samples();
    }

    @Override
    public Set<Map.Entry<K, V>> create(Object... elements) {
      return mapGenerator.create(elements).entrySet();
    }

    @Override
    public Map.Entry<K, V>[] createArray(int length) {
      return mapGenerator.createArray(length);
    }

    @Override
    public Iterable<Map.Entry<K, V>> order(
        List<Map.Entry<K, V>> insertionOrder) {
      return mapGenerator.order(insertionOrder);
    }
  }

  // TODO: investigate some API changes to SampleElements that would tidy up
  // parts of the following classes.

  private static class MapKeySetGenerator<K, V> implements TestSetGenerator<K> {
    private final OneSizeTestContainerGenerator<Map<K, V>, Map.Entry<K, V>>
        mapGenerator;
    private final SampleElements<K> samples;

    public MapKeySetGenerator(
        OneSizeTestContainerGenerator<Map<K, V>, Map.Entry<K, V>>
            mapGenerator) {
      this.mapGenerator = mapGenerator;
      final SampleElements<Map.Entry<K, V>> mapSamples =
          this.mapGenerator.samples();
      this.samples = new SampleElements<K>(
          mapSamples.e0.getKey(),
          mapSamples.e1.getKey(),
          mapSamples.e2.getKey(),
          mapSamples.e3.getKey(),
          mapSamples.e4.getKey());
    }

    @Override
    public SampleElements<K> samples() {
      return samples;
    }

    @Override
    public Set<K> create(Object... elements) {
      @SuppressWarnings("unchecked")
      K[] keysArray = (K[]) elements;

      // Start with a suitably shaped collection of entries
      Collection<Map.Entry<K, V>> originalEntries =
          mapGenerator.getSampleElements(elements.length);

      // Create a copy of that, with the desired value for each key
      Collection<Map.Entry<K, V>> entries =
          new ArrayList<Entry<K, V>>(elements.length);
      int i = 0;
      for (Map.Entry<K, V> entry : originalEntries) {
        entries.add(Helpers.mapEntry(keysArray[i++], entry.getValue()));
      }

      return mapGenerator.create(entries.toArray()).keySet();
    }

    @Override
    public K[] createArray(int length) {
      // TODO: with appropriate refactoring of OneSizeGenerator, we can perhaps
      // tidy this up and get rid of the casts here and in
      // MapValueCollectionGenerator.

      return ((TestMapGenerator<K, V>) mapGenerator.getInnerGenerator())
          .createKeyArray(length);
    }

    @Override
    public Iterable<K> order(List<K> insertionOrder) {
      return insertionOrder;
    }
  }

  private static class MapValueCollectionGenerator<K, V>
      implements TestCollectionGenerator<V> {
    private final OneSizeTestContainerGenerator<Map<K, V>, Map.Entry<K, V>>
        mapGenerator;
    private final SampleElements<V> samples;

    public MapValueCollectionGenerator(
        OneSizeTestContainerGenerator<
            Map<K, V>, Map.Entry<K, V>> mapGenerator) {
      this.mapGenerator = mapGenerator;
      final SampleElements<Map.Entry<K, V>> mapSamples =
          this.mapGenerator.samples();
      this.samples = new SampleElements<V>(
          mapSamples.e0.getValue(),
          mapSamples.e1.getValue(),
          mapSamples.e2.getValue(),
          mapSamples.e3.getValue(),
          mapSamples.e4.getValue());
    }

    @Override
    public SampleElements<V> samples() {
      return samples;
    }

    @Override
    public Collection<V> create(Object... elements) {
      @SuppressWarnings("unchecked")
      V[] valuesArray = (V[]) elements;

      // Start with a suitably shaped collection of entries
      Collection<Map.Entry<K, V>> originalEntries =
          mapGenerator.getSampleElements(elements.length);

      // Create a copy of that, with the desired value for each value
      Collection<Map.Entry<K, V>> entries =
          new ArrayList<Entry<K, V>>(elements.length);
      int i = 0;
      for (Map.Entry<K, V> entry : originalEntries) {
        entries.add(Helpers.mapEntry(entry.getKey(), valuesArray[i++]));
      }

      return mapGenerator.create(entries.toArray()).values();
    }

    @Override
    public V[] createArray(int length) {
      //noinspection UnnecessaryLocalVariable
      final V[] vs = ((TestMapGenerator<K, V>) mapGenerator.getInnerGenerator())
          .createValueArray(length);
      return vs;
    }

    @Override
    public Iterable<V> order(List<V> insertionOrder) {
      return insertionOrder;
    }
  }
}

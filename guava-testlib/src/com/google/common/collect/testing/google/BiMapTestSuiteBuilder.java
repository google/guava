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

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.BiMap;
import com.google.common.collect.testing.AbstractTester;
import com.google.common.collect.testing.FeatureSpecificTestSuiteBuilder;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.OneSizeTestContainerGenerator;
import com.google.common.collect.testing.PerCollectionSizeTestSuiteBuilder;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.TestMapGenerator;
import com.google.common.collect.testing.TestSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.Feature;
import com.google.common.collect.testing.features.MapFeature;

import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Creates, based on your criteria, a JUnit test suite that exhaustively tests a {@code BiMap}
 * implementation.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
public class BiMapTestSuiteBuilder<K, V>
    extends PerCollectionSizeTestSuiteBuilder<BiMapTestSuiteBuilder<K, V>,
            TestBiMapGenerator<K, V>, BiMap<K, V>, Map.Entry<K, V>> {
  public static <K, V> BiMapTestSuiteBuilder<K, V> using(TestBiMapGenerator<K, V> generator) {
    return new BiMapTestSuiteBuilder<K, V>().usingGenerator(generator);
  }

  @Override
  protected List<Class<? extends AbstractTester>> getTesters() {
    List<Class<? extends AbstractTester>> testers =
        new ArrayList<Class<? extends AbstractTester>>();
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
  protected
      List<TestSuite>
      createDerivedSuites(
          FeatureSpecificTestSuiteBuilder<?,
              ? extends OneSizeTestContainerGenerator<BiMap<K, V>, Entry<K, V>>> parentBuilder) {
    List<TestSuite> derived = super.createDerivedSuites(parentBuilder);
    // TODO(cpovirk): consider using this approach (derived suites instead of extension) in
    // ListTestSuiteBuilder, etc.?
    derived.add(MapTestSuiteBuilder
        .using(new MapGenerator<K, V>(parentBuilder.getSubjectGenerator()))
        .withFeatures(parentBuilder.getFeatures())
        .named(parentBuilder.getName() + " [Map]")
        .suppressing(parentBuilder.getSuppressedTests())
        .createTestSuite());
    /*
     * TODO(cpovirk): the Map tests duplicate most of this effort by using a
     * CollectionTestSuiteBuilder on values(). It would be nice to avoid that
     */
    derived.add(SetTestSuiteBuilder
        .using(new BiMapValueSetGenerator<K, V>(parentBuilder.getSubjectGenerator()))
        .withFeatures(computeValuesSetFeatures(parentBuilder.getFeatures()))
        .named(parentBuilder.getName() + " values [Set]")
        .suppressing(parentBuilder.getSuppressedTests())
        .createTestSuite());
    if (!parentBuilder.getFeatures().contains(NoRecurse.INVERSE)) {
      derived.add(BiMapTestSuiteBuilder
          .using(new InverseBiMapGenerator<K, V>(parentBuilder.getSubjectGenerator()))
          .withFeatures(computeInverseFeatures(parentBuilder.getFeatures()))
          .named(parentBuilder.getName() + " inverse")
          .suppressing(parentBuilder.getSuppressedTests())
          .createTestSuite());
    }

    return derived;
  }

  private static Set<Feature<?>> computeInverseFeatures(Set<Feature<?>> mapFeatures) {
    Set<Feature<?>> inverseFeatures = new HashSet<Feature<?>>(mapFeatures);

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

    return inverseFeatures;
  }

  // TODO(user): can we eliminate the duplication from MapTestSuiteBuilder here?

  private static Set<Feature<?>> computeValuesSetFeatures(
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

  private static class MapGenerator<K, V> implements TestMapGenerator<K, V> {

    private final OneSizeTestContainerGenerator<BiMap<K, V>, Entry<K, V>> generator;

    public MapGenerator(
        OneSizeTestContainerGenerator<BiMap<K, V>, Entry<K, V>> oneSizeTestContainerGenerator) {
      this.generator = oneSizeTestContainerGenerator;
    }

    @Override
    public SampleElements<Map.Entry<K, V>> samples() {
      return generator.samples();
    }

    @Override
    public Map<K, V> create(Object... elements) {
      return generator.create(elements);
    }

    @Override
    public Map.Entry<K, V>[] createArray(int length) {
      return generator.createArray(length);
    }

    @Override
    public Iterable<Map.Entry<K, V>> order(List<Map.Entry<K, V>> insertionOrder) {
      return generator.order(insertionOrder);
    }

    @SuppressWarnings("unchecked")
    @Override
    public K[] createKeyArray(int length) {
      return (K[]) new Object[length];
    }

    @SuppressWarnings("unchecked")
    @Override
    public V[] createValueArray(int length) {
      return (V[]) new Object[length];
    }
  }

  private static class InverseBiMapGenerator<K, V> implements TestBiMapGenerator<V, K> {

    private final OneSizeTestContainerGenerator<BiMap<K, V>, Entry<K, V>> generator;

    public InverseBiMapGenerator(
        OneSizeTestContainerGenerator<BiMap<K, V>, Entry<K, V>> oneSizeTestContainerGenerator) {
      this.generator = oneSizeTestContainerGenerator;
    }

    @Override
    public SampleElements<Map.Entry<V, K>> samples() {
      SampleElements<Entry<K, V>> samples = generator.samples();
      return new SampleElements<Map.Entry<V, K>>(reverse(samples.e0), reverse(samples.e1),
          reverse(samples.e2), reverse(samples.e3), reverse(samples.e4));
    }

    private Map.Entry<V, K> reverse(Map.Entry<K, V> entry) {
      return Helpers.mapEntry(entry.getValue(), entry.getKey());
    }

    @SuppressWarnings("unchecked")
    @Override
    public BiMap<V, K> create(Object... elements) {
      Entry[] entries = new Entry[elements.length];
      for (int i = 0; i < elements.length; i++) {
        entries[i] = reverse((Entry<K, V>) elements[i]);
      }
      return generator.create((Object[]) entries).inverse();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map.Entry<V, K>[] createArray(int length) {
      return new Entry[length];
    }

    @Override
    public Iterable<Entry<V, K>> order(List<Entry<V, K>> insertionOrder) {
      return insertionOrder;
    }

    @SuppressWarnings("unchecked")
    @Override
    public V[] createKeyArray(int length) {
      return (V[]) new Object[length];
    }

    @SuppressWarnings("unchecked")
    @Override
    public K[] createValueArray(int length) {
      return (K[]) new Object[length];
    }
  }

  private static class BiMapValueSetGenerator<K, V>
      implements TestSetGenerator<V> {
    private final OneSizeTestContainerGenerator<BiMap<K, V>, Map.Entry<K, V>>
        mapGenerator;
    private final SampleElements<V> samples;

    public BiMapValueSetGenerator(
        OneSizeTestContainerGenerator<BiMap<K, V>, Entry<K, V>> mapGenerator) {
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
    public Set<V> create(Object... elements) {
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
      final V[] vs = ((TestBiMapGenerator<K, V>) mapGenerator.getInnerGenerator())
          .createValueArray(length);
      return vs;
    }

    @Override
    public Iterable<V> order(List<V> insertionOrder) {
      return insertionOrder;
    }
  }
}

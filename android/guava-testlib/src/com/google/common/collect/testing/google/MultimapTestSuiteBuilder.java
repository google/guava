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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.testing.Helpers.mapEntry;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.testing.AbstractTester;
import com.google.common.collect.testing.CollectionTestSuiteBuilder;
import com.google.common.collect.testing.DerivedGenerator;
import com.google.common.collect.testing.FeatureSpecificTestSuiteBuilder;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.OneSizeTestContainerGenerator;
import com.google.common.collect.testing.PerCollectionSizeTestSuiteBuilder;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.TestCollectionGenerator;
import com.google.common.collect.testing.TestMapGenerator;
import com.google.common.collect.testing.TestSubjectGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.Feature;
import com.google.common.collect.testing.features.ListFeature;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.testing.SerializableTester;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import junit.framework.TestSuite;

/**
 * Creates, based on your criteria, a JUnit test suite that exhaustively tests a {@code Multimap}
 * implementation.
 *
 * @author Louis Wasserman
 */
@GwtIncompatible
public class MultimapTestSuiteBuilder<K, V, M extends Multimap<K, V>>
    extends PerCollectionSizeTestSuiteBuilder<
        MultimapTestSuiteBuilder<K, V, M>, TestMultimapGenerator<K, V, M>, M, Entry<K, V>> {

  public static <K, V, M extends Multimap<K, V>> MultimapTestSuiteBuilder<K, V, M> using(
      TestMultimapGenerator<K, V, M> generator) {
    return new MultimapTestSuiteBuilder<K, V, M>().usingGenerator(generator);
  }

  // Class parameters must be raw.
  @Override
  protected List<Class<? extends AbstractTester>> getTesters() {
    return ImmutableList.<Class<? extends AbstractTester>>of(
        MultimapAsMapGetTester.class,
        MultimapAsMapTester.class,
        MultimapSizeTester.class,
        MultimapClearTester.class,
        MultimapContainsKeyTester.class,
        MultimapContainsValueTester.class,
        MultimapContainsEntryTester.class,
        MultimapEntriesTester.class,
        MultimapEqualsTester.class,
        MultimapGetTester.class,
        MultimapKeySetTester.class,
        MultimapKeysTester.class,
        MultimapPutTester.class,
        MultimapPutAllMultimapTester.class,
        MultimapPutIterableTester.class,
        MultimapReplaceValuesTester.class,
        MultimapRemoveEntryTester.class,
        MultimapRemoveAllTester.class,
        MultimapToStringTester.class,
        MultimapValuesTester.class);
  }

  @Override
  protected List<TestSuite> createDerivedSuites(
      FeatureSpecificTestSuiteBuilder<?, ? extends OneSizeTestContainerGenerator<M, Entry<K, V>>>
          parentBuilder) {
    // TODO: Once invariant support is added, supply invariants to each of the
    // derived suites, to check that mutations to the derived collections are
    // reflected in the underlying map.

    List<TestSuite> derivedSuites = super.createDerivedSuites(parentBuilder);

    if (parentBuilder.getFeatures().contains(CollectionFeature.SERIALIZABLE)) {
      derivedSuites.add(
          MultimapTestSuiteBuilder.using(
                  new ReserializedMultimapGenerator<K, V, M>(parentBuilder.getSubjectGenerator()))
              .withFeatures(computeReserializedMultimapFeatures(parentBuilder.getFeatures()))
              .named(parentBuilder.getName() + " reserialized")
              .suppressing(parentBuilder.getSuppressedTests())
              .withSetUp(parentBuilder.getSetUp())
              .withTearDown(parentBuilder.getTearDown())
              .createTestSuite());
    }

    derivedSuites.add(
        MapTestSuiteBuilder.using(new AsMapGenerator<K, V, M>(parentBuilder.getSubjectGenerator()))
            .withFeatures(computeAsMapFeatures(parentBuilder.getFeatures()))
            .named(parentBuilder.getName() + ".asMap")
            .suppressing(parentBuilder.getSuppressedTests())
            .withSetUp(parentBuilder.getSetUp())
            .withTearDown(parentBuilder.getTearDown())
            .createTestSuite());

    derivedSuites.add(computeEntriesTestSuite(parentBuilder));
    derivedSuites.add(computeMultimapGetTestSuite(parentBuilder));
    derivedSuites.add(computeMultimapAsMapGetTestSuite(parentBuilder));
    derivedSuites.add(computeKeysTestSuite(parentBuilder));
    derivedSuites.add(computeValuesTestSuite(parentBuilder));

    return derivedSuites;
  }

  TestSuite computeValuesTestSuite(
      FeatureSpecificTestSuiteBuilder<?, ? extends OneSizeTestContainerGenerator<M, Entry<K, V>>>
          parentBuilder) {
    return CollectionTestSuiteBuilder.using(
            new ValuesGenerator<K, V, M>(parentBuilder.getSubjectGenerator()))
        .withFeatures(computeValuesFeatures(parentBuilder.getFeatures()))
        .named(parentBuilder.getName() + ".values")
        .suppressing(parentBuilder.getSuppressedTests())
        .createTestSuite();
  }

  TestSuite computeEntriesTestSuite(
      FeatureSpecificTestSuiteBuilder<?, ? extends OneSizeTestContainerGenerator<M, Entry<K, V>>>
          parentBuilder) {
    return CollectionTestSuiteBuilder.using(
            new EntriesGenerator<K, V, M>(parentBuilder.getSubjectGenerator()))
        .withFeatures(computeEntriesFeatures(parentBuilder.getFeatures()))
        .named(parentBuilder.getName() + ".entries")
        .suppressing(parentBuilder.getSuppressedTests())
        .withSetUp(parentBuilder.getSetUp())
        .withTearDown(parentBuilder.getTearDown())
        .createTestSuite();
  }

  TestSuite computeMultimapGetTestSuite(
      FeatureSpecificTestSuiteBuilder<?, ? extends OneSizeTestContainerGenerator<M, Entry<K, V>>>
          parentBuilder) {
    return CollectionTestSuiteBuilder.using(
            new MultimapGetGenerator<K, V, M>(parentBuilder.getSubjectGenerator()))
        .withFeatures(computeMultimapGetFeatures(parentBuilder.getFeatures()))
        .named(parentBuilder.getName() + ".get[key]")
        .suppressing(parentBuilder.getSuppressedTests())
        .withSetUp(parentBuilder.getSetUp())
        .withTearDown(parentBuilder.getTearDown())
        .createTestSuite();
  }

  TestSuite computeMultimapAsMapGetTestSuite(
      FeatureSpecificTestSuiteBuilder<?, ? extends OneSizeTestContainerGenerator<M, Entry<K, V>>>
          parentBuilder) {
    Set<Feature<?>> features = computeMultimapAsMapGetFeatures(parentBuilder.getFeatures());
    if (Collections.disjoint(features, EnumSet.allOf(CollectionSize.class))) {
      return new TestSuite();
    } else {
      return CollectionTestSuiteBuilder.using(
              new MultimapAsMapGetGenerator<K, V, M>(parentBuilder.getSubjectGenerator()))
          .withFeatures(features)
          .named(parentBuilder.getName() + ".asMap[].get[key]")
          .suppressing(parentBuilder.getSuppressedTests())
          .withSetUp(parentBuilder.getSetUp())
          .withTearDown(parentBuilder.getTearDown())
          .createTestSuite();
    }
  }

  TestSuite computeKeysTestSuite(
      FeatureSpecificTestSuiteBuilder<?, ? extends OneSizeTestContainerGenerator<M, Entry<K, V>>>
          parentBuilder) {
    return MultisetTestSuiteBuilder.using(
            new KeysGenerator<K, V, M>(parentBuilder.getSubjectGenerator()))
        .withFeatures(computeKeysFeatures(parentBuilder.getFeatures()))
        .named(parentBuilder.getName() + ".keys")
        .suppressing(parentBuilder.getSuppressedTests())
        .withSetUp(parentBuilder.getSetUp())
        .withTearDown(parentBuilder.getTearDown())
        .createTestSuite();
  }

  static Set<Feature<?>> computeDerivedCollectionFeatures(Set<Feature<?>> multimapFeatures) {
    Set<Feature<?>> derivedFeatures = Helpers.copyToSet(multimapFeatures);
    if (!derivedFeatures.remove(CollectionFeature.SERIALIZABLE_INCLUDING_VIEWS)) {
      derivedFeatures.remove(CollectionFeature.SERIALIZABLE);
    }
    if (derivedFeatures.remove(MapFeature.SUPPORTS_REMOVE)) {
      derivedFeatures.add(CollectionFeature.SUPPORTS_REMOVE);
    }
    return derivedFeatures;
  }

  static Set<Feature<?>> computeEntriesFeatures(Set<Feature<?>> multimapFeatures) {
    Set<Feature<?>> result = computeDerivedCollectionFeatures(multimapFeatures);
    if (multimapFeatures.contains(MapFeature.ALLOWS_NULL_ENTRY_QUERIES)) {
      result.add(CollectionFeature.ALLOWS_NULL_QUERIES);
    }
    return result;
  }

  static Set<Feature<?>> computeValuesFeatures(Set<Feature<?>> multimapFeatures) {
    Set<Feature<?>> result = computeDerivedCollectionFeatures(multimapFeatures);
    if (multimapFeatures.contains(MapFeature.ALLOWS_NULL_VALUES)) {
      result.add(CollectionFeature.ALLOWS_NULL_VALUES);
    }
    if (multimapFeatures.contains(MapFeature.ALLOWS_NULL_VALUE_QUERIES)) {
      result.add(CollectionFeature.ALLOWS_NULL_QUERIES);
    }
    return result;
  }

  static Set<Feature<?>> computeKeysFeatures(Set<Feature<?>> multimapFeatures) {
    Set<Feature<?>> result = computeDerivedCollectionFeatures(multimapFeatures);
    if (multimapFeatures.contains(MapFeature.ALLOWS_NULL_KEYS)) {
      result.add(CollectionFeature.ALLOWS_NULL_VALUES);
    }
    if (multimapFeatures.contains(MapFeature.ALLOWS_NULL_KEY_QUERIES)) {
      result.add(CollectionFeature.ALLOWS_NULL_QUERIES);
    }
    return result;
  }

  private static Set<Feature<?>> computeReserializedMultimapFeatures(
      Set<Feature<?>> multimapFeatures) {
    Set<Feature<?>> derivedFeatures = Helpers.copyToSet(multimapFeatures);
    derivedFeatures.remove(CollectionFeature.SERIALIZABLE);
    derivedFeatures.remove(CollectionFeature.SERIALIZABLE_INCLUDING_VIEWS);
    return derivedFeatures;
  }

  private static Set<Feature<?>> computeAsMapFeatures(Set<Feature<?>> multimapFeatures) {
    Set<Feature<?>> derivedFeatures = Helpers.copyToSet(multimapFeatures);
    derivedFeatures.remove(MapFeature.GENERAL_PURPOSE);
    derivedFeatures.remove(MapFeature.SUPPORTS_PUT);
    derivedFeatures.remove(MapFeature.ALLOWS_NULL_VALUES);
    derivedFeatures.add(MapFeature.ALLOWS_NULL_VALUE_QUERIES);
    derivedFeatures.add(MapFeature.REJECTS_DUPLICATES_AT_CREATION);
    if (!derivedFeatures.contains(CollectionFeature.SERIALIZABLE_INCLUDING_VIEWS)) {
      derivedFeatures.remove(CollectionFeature.SERIALIZABLE);
    }
    return derivedFeatures;
  }

  private static final ImmutableMultimap<Feature<?>, Feature<?>> GET_FEATURE_MAP =
      ImmutableMultimap.<Feature<?>, Feature<?>>builder()
          .put(
              MapFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
              CollectionFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION)
          .put(MapFeature.GENERAL_PURPOSE, ListFeature.SUPPORTS_ADD_WITH_INDEX)
          .put(MapFeature.GENERAL_PURPOSE, ListFeature.SUPPORTS_REMOVE_WITH_INDEX)
          .put(MapFeature.GENERAL_PURPOSE, ListFeature.SUPPORTS_SET)
          .put(MapFeature.ALLOWS_NULL_VALUE_QUERIES, CollectionFeature.ALLOWS_NULL_QUERIES)
          .put(MapFeature.ALLOWS_NULL_VALUES, CollectionFeature.ALLOWS_NULL_VALUES)
          .put(MapFeature.SUPPORTS_REMOVE, CollectionFeature.SUPPORTS_REMOVE)
          .put(MapFeature.SUPPORTS_PUT, CollectionFeature.SUPPORTS_ADD)
          .build();

  Set<Feature<?>> computeMultimapGetFeatures(Set<Feature<?>> multimapFeatures) {
    Set<Feature<?>> derivedFeatures = Helpers.copyToSet(multimapFeatures);
    for (Entry<Feature<?>, Feature<?>> entry : GET_FEATURE_MAP.entries()) {
      if (derivedFeatures.contains(entry.getKey())) {
        derivedFeatures.add(entry.getValue());
      }
    }
    if (derivedFeatures.remove(MultimapFeature.VALUE_COLLECTIONS_SUPPORT_ITERATOR_REMOVE)) {
      derivedFeatures.add(CollectionFeature.SUPPORTS_ITERATOR_REMOVE);
    }
    if (!derivedFeatures.contains(CollectionFeature.SERIALIZABLE_INCLUDING_VIEWS)) {
      derivedFeatures.remove(CollectionFeature.SERIALIZABLE);
    }
    derivedFeatures.removeAll(GET_FEATURE_MAP.keySet());
    return derivedFeatures;
  }

  Set<Feature<?>> computeMultimapAsMapGetFeatures(Set<Feature<?>> multimapFeatures) {
    Set<Feature<?>> derivedFeatures =
        Helpers.copyToSet(computeMultimapGetFeatures(multimapFeatures));
    if (derivedFeatures.remove(CollectionSize.ANY)) {
      derivedFeatures.addAll(CollectionSize.ANY.getImpliedFeatures());
    }
    derivedFeatures.remove(CollectionSize.ZERO);
    return derivedFeatures;
  }

  private static class AsMapGenerator<K, V, M extends Multimap<K, V>>
      implements TestMapGenerator<K, Collection<V>>, DerivedGenerator {
    private final OneSizeTestContainerGenerator<M, Entry<K, V>> multimapGenerator;

    public AsMapGenerator(OneSizeTestContainerGenerator<M, Entry<K, V>> multimapGenerator) {
      this.multimapGenerator = multimapGenerator;
    }

    @Override
    public TestSubjectGenerator<?> getInnerGenerator() {
      return multimapGenerator;
    }

    private Collection<V> createCollection(V v) {
      return ((TestMultimapGenerator<K, V, M>) multimapGenerator.getInnerGenerator())
          .createCollection(Collections.singleton(v));
    }

    @Override
    public SampleElements<Entry<K, Collection<V>>> samples() {
      SampleElements<K> sampleKeys =
          ((TestMultimapGenerator<K, V, M>) multimapGenerator.getInnerGenerator()).sampleKeys();
      SampleElements<V> sampleValues =
          ((TestMultimapGenerator<K, V, M>) multimapGenerator.getInnerGenerator()).sampleValues();
      return new SampleElements<>(
          mapEntry(sampleKeys.e0(), createCollection(sampleValues.e0())),
          mapEntry(sampleKeys.e1(), createCollection(sampleValues.e1())),
          mapEntry(sampleKeys.e2(), createCollection(sampleValues.e2())),
          mapEntry(sampleKeys.e3(), createCollection(sampleValues.e3())),
          mapEntry(sampleKeys.e4(), createCollection(sampleValues.e4())));
    }

    @Override
    public Map<K, Collection<V>> create(Object... elements) {
      Set<K> keySet = new HashSet<>();
      List<Entry<K, V>> builder = new ArrayList<>();
      for (Object o : elements) {
        Entry<K, Collection<V>> entry = (Entry<K, Collection<V>>) o;
        keySet.add(entry.getKey());
        for (V v : entry.getValue()) {
          builder.add(mapEntry(entry.getKey(), v));
        }
      }
      checkArgument(keySet.size() == elements.length, "Duplicate keys");
      return multimapGenerator.create(builder.toArray()).asMap();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Entry<K, Collection<V>>[] createArray(int length) {
      return new Entry[length];
    }

    @Override
    public Iterable<Entry<K, Collection<V>>> order(List<Entry<K, Collection<V>>> insertionOrder) {
      Map<K, Collection<V>> map = new HashMap<>();
      List<Entry<K, V>> builder = new ArrayList<>();
      for (Entry<K, Collection<V>> entry : insertionOrder) {
        for (V v : entry.getValue()) {
          builder.add(mapEntry(entry.getKey(), v));
        }
        map.put(entry.getKey(), entry.getValue());
      }
      Iterable<Entry<K, V>> ordered = multimapGenerator.order(builder);
      LinkedHashMap<K, Collection<V>> orderedMap = new LinkedHashMap<>();
      for (Entry<K, V> entry : ordered) {
        orderedMap.put(entry.getKey(), map.get(entry.getKey()));
      }
      return orderedMap.entrySet();
    }

    @Override
    public K[] createKeyArray(int length) {
      return ((TestMultimapGenerator<K, V, M>) multimapGenerator.getInnerGenerator())
          .createKeyArray(length);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<V>[] createValueArray(int length) {
      return new Collection[length];
    }
  }

  static class EntriesGenerator<K, V, M extends Multimap<K, V>>
      implements TestCollectionGenerator<Entry<K, V>>, DerivedGenerator {
    private final OneSizeTestContainerGenerator<M, Entry<K, V>> multimapGenerator;

    public EntriesGenerator(OneSizeTestContainerGenerator<M, Entry<K, V>> multimapGenerator) {
      this.multimapGenerator = multimapGenerator;
    }

    @Override
    public TestSubjectGenerator<?> getInnerGenerator() {
      return multimapGenerator;
    }

    @Override
    public SampleElements<Entry<K, V>> samples() {
      return multimapGenerator.samples();
    }

    @Override
    public Collection<Entry<K, V>> create(Object... elements) {
      return multimapGenerator.create(elements).entries();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Entry<K, V>[] createArray(int length) {
      return new Entry[length];
    }

    @Override
    public Iterable<Entry<K, V>> order(List<Entry<K, V>> insertionOrder) {
      return multimapGenerator.order(insertionOrder);
    }
  }

  static class ValuesGenerator<K, V, M extends Multimap<K, V>>
      implements TestCollectionGenerator<V> {
    private final OneSizeTestContainerGenerator<M, Entry<K, V>> multimapGenerator;

    public ValuesGenerator(OneSizeTestContainerGenerator<M, Entry<K, V>> multimapGenerator) {
      this.multimapGenerator = multimapGenerator;
    }

    @Override
    public SampleElements<V> samples() {
      return ((TestMultimapGenerator<K, V, M>) multimapGenerator.getInnerGenerator())
          .sampleValues();
    }

    @Override
    public Collection<V> create(Object... elements) {
      K k =
          ((TestMultimapGenerator<K, V, M>) multimapGenerator.getInnerGenerator())
              .sampleKeys()
              .e0();
      Entry<K, V>[] entries = new Entry[elements.length];
      for (int i = 0; i < elements.length; i++) {
        entries[i] = mapEntry(k, (V) elements[i]);
      }
      return multimapGenerator.create((Object[]) entries).values();
    }

    @SuppressWarnings("unchecked")
    @Override
    public V[] createArray(int length) {
      return ((TestMultimapGenerator<K, V, M>) multimapGenerator.getInnerGenerator())
          .createValueArray(length);
    }

    @Override
    public Iterable<V> order(List<V> insertionOrder) {
      K k =
          ((TestMultimapGenerator<K, V, M>) multimapGenerator.getInnerGenerator())
              .sampleKeys()
              .e0();
      List<Entry<K, V>> entries = new ArrayList<>();
      for (V v : insertionOrder) {
        entries.add(mapEntry(k, v));
      }
      Iterable<Entry<K, V>> ordered = multimapGenerator.order(entries);
      List<V> orderedValues = new ArrayList<>();
      for (Entry<K, V> entry : ordered) {
        orderedValues.add(entry.getValue());
      }
      return orderedValues;
    }
  }

  static class KeysGenerator<K, V, M extends Multimap<K, V>>
      implements TestMultisetGenerator<K>, DerivedGenerator {
    private final OneSizeTestContainerGenerator<M, Entry<K, V>> multimapGenerator;

    public KeysGenerator(OneSizeTestContainerGenerator<M, Entry<K, V>> multimapGenerator) {
      this.multimapGenerator = multimapGenerator;
    }

    @Override
    public TestSubjectGenerator<?> getInnerGenerator() {
      return multimapGenerator;
    }

    @Override
    public SampleElements<K> samples() {
      return ((TestMultimapGenerator<K, V, M>) multimapGenerator.getInnerGenerator()).sampleKeys();
    }

    @Override
    public Multiset<K> create(Object... elements) {
      /*
       * This is nasty and complicated, but it's the only way to make sure keys get mapped to enough
       * distinct values.
       */
      Entry[] entries = new Entry[elements.length];
      Map<K, Iterator<V>> valueIterators = new HashMap<>();
      for (int i = 0; i < elements.length; i++) {
        @SuppressWarnings("unchecked")
        K key = (K) elements[i];

        Iterator<V> valueItr = valueIterators.get(key);
        if (valueItr == null) {
          valueIterators.put(key, valueItr = sampleValuesIterator());
        }
        entries[i] = mapEntry((K) elements[i], valueItr.next());
      }
      return multimapGenerator.create((Object[]) entries).keys();
    }

    private Iterator<V> sampleValuesIterator() {
      return ((TestMultimapGenerator<K, V, M>) multimapGenerator.getInnerGenerator())
          .sampleValues()
          .iterator();
    }

    @SuppressWarnings("unchecked")
    @Override
    public K[] createArray(int length) {
      return ((TestMultimapGenerator<K, V, M>) multimapGenerator.getInnerGenerator())
          .createKeyArray(length);
    }

    @Override
    public Iterable<K> order(List<K> insertionOrder) {
      Iterator<V> valueIter = sampleValuesIterator();
      List<Entry<K, V>> entries = new ArrayList<>();
      for (K k : insertionOrder) {
        entries.add(mapEntry(k, valueIter.next()));
      }
      Iterable<Entry<K, V>> ordered = multimapGenerator.order(entries);
      List<K> orderedValues = new ArrayList<>();
      for (Entry<K, V> entry : ordered) {
        orderedValues.add(entry.getKey());
      }
      return orderedValues;
    }
  }

  static class MultimapGetGenerator<K, V, M extends Multimap<K, V>>
      implements TestCollectionGenerator<V> {
    final OneSizeTestContainerGenerator<M, Entry<K, V>> multimapGenerator;

    public MultimapGetGenerator(OneSizeTestContainerGenerator<M, Entry<K, V>> multimapGenerator) {
      this.multimapGenerator = multimapGenerator;
    }

    @Override
    public SampleElements<V> samples() {
      return ((TestMultimapGenerator<K, V, M>) multimapGenerator.getInnerGenerator())
          .sampleValues();
    }

    @Override
    public V[] createArray(int length) {
      return ((TestMultimapGenerator<K, V, M>) multimapGenerator.getInnerGenerator())
          .createValueArray(length);
    }

    @Override
    public Iterable<V> order(List<V> insertionOrder) {
      K k =
          ((TestMultimapGenerator<K, V, M>) multimapGenerator.getInnerGenerator())
              .sampleKeys()
              .e0();
      List<Entry<K, V>> entries = new ArrayList<>();
      for (V v : insertionOrder) {
        entries.add(mapEntry(k, v));
      }
      Iterable<Entry<K, V>> orderedEntries = multimapGenerator.order(entries);
      List<V> values = new ArrayList<>();
      for (Entry<K, V> entry : orderedEntries) {
        values.add(entry.getValue());
      }
      return values;
    }

    @Override
    public Collection<V> create(Object... elements) {
      Entry<K, V>[] array = multimapGenerator.createArray(elements.length);
      K k =
          ((TestMultimapGenerator<K, V, M>) multimapGenerator.getInnerGenerator())
              .sampleKeys()
              .e0();
      for (int i = 0; i < elements.length; i++) {
        array[i] = mapEntry(k, (V) elements[i]);
      }
      return multimapGenerator.create((Object[]) array).get(k);
    }
  }

  static class MultimapAsMapGetGenerator<K, V, M extends Multimap<K, V>>
      extends MultimapGetGenerator<K, V, M> {

    public MultimapAsMapGetGenerator(
        OneSizeTestContainerGenerator<M, Entry<K, V>> multimapGenerator) {
      super(multimapGenerator);
    }

    @Override
    public Collection<V> create(Object... elements) {
      Entry<K, V>[] array = multimapGenerator.createArray(elements.length);
      K k =
          ((TestMultimapGenerator<K, V, M>) multimapGenerator.getInnerGenerator())
              .sampleKeys()
              .e0();
      for (int i = 0; i < elements.length; i++) {
        array[i] = mapEntry(k, (V) elements[i]);
      }
      return multimapGenerator.create((Object[]) array).asMap().get(k);
    }
  }

  private static class ReserializedMultimapGenerator<K, V, M extends Multimap<K, V>>
      implements TestMultimapGenerator<K, V, M> {
    private final OneSizeTestContainerGenerator<M, Entry<K, V>> multimapGenerator;

    public ReserializedMultimapGenerator(
        OneSizeTestContainerGenerator<M, Entry<K, V>> multimapGenerator) {
      this.multimapGenerator = multimapGenerator;
    }

    @Override
    public SampleElements<Entry<K, V>> samples() {
      return multimapGenerator.samples();
    }

    @Override
    public Entry<K, V>[] createArray(int length) {
      return multimapGenerator.createArray(length);
    }

    @Override
    public Iterable<Entry<K, V>> order(List<Entry<K, V>> insertionOrder) {
      return multimapGenerator.order(insertionOrder);
    }

    @Override
    public M create(Object... elements) {
      return SerializableTester.reserialize(
          ((TestMultimapGenerator<K, V, M>) multimapGenerator.getInnerGenerator())
              .create(elements));
    }

    @Override
    public K[] createKeyArray(int length) {
      return ((TestMultimapGenerator<K, V, M>) multimapGenerator.getInnerGenerator())
          .createKeyArray(length);
    }

    @Override
    public V[] createValueArray(int length) {
      return ((TestMultimapGenerator<K, V, M>) multimapGenerator.getInnerGenerator())
          .createValueArray(length);
    }

    @Override
    public SampleElements<K> sampleKeys() {
      return ((TestMultimapGenerator<K, V, M>) multimapGenerator.getInnerGenerator()).sampleKeys();
    }

    @Override
    public SampleElements<V> sampleValues() {
      return ((TestMultimapGenerator<K, V, M>) multimapGenerator.getInnerGenerator())
          .sampleValues();
    }

    @Override
    public Collection<V> createCollection(Iterable<? extends V> values) {
      return ((TestMultimapGenerator<K, V, M>) multimapGenerator.getInnerGenerator())
          .createCollection(values);
    }
  }
}

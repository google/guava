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

import static com.google.common.collect.testing.Helpers.castOrCopyToList;
import static com.google.common.collect.testing.Helpers.equal;
import static com.google.common.collect.testing.Helpers.mapEntry;
import static java.util.Collections.sort;

import com.google.common.annotations.GwtCompatible;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Derived suite generators, split out of the suite builders so that they are available to GWT.
 *
 * @author George van den Driessche
 */
@GwtCompatible
public final class DerivedCollectionGenerators {
  public static class MapEntrySetGenerator<K, V>
      implements TestSetGenerator<Entry<K, V>>, DerivedGenerator {
    private final OneSizeTestContainerGenerator<Map<K, V>, Entry<K, V>> mapGenerator;

    public MapEntrySetGenerator(
        OneSizeTestContainerGenerator<Map<K, V>, Entry<K, V>> mapGenerator) {
      this.mapGenerator = mapGenerator;
    }

    @Override
    public SampleElements<Entry<K, V>> samples() {
      return mapGenerator.samples();
    }

    @Override
    public Set<Entry<K, V>> create(Object... elements) {
      return mapGenerator.create(elements).entrySet();
    }

    @Override
    public Entry<K, V>[] createArray(int length) {
      return mapGenerator.createArray(length);
    }

    @Override
    public Iterable<Entry<K, V>> order(List<Entry<K, V>> insertionOrder) {
      return mapGenerator.order(insertionOrder);
    }

    @Override
    public OneSizeTestContainerGenerator<Map<K, V>, Entry<K, V>> getInnerGenerator() {
      return mapGenerator;
    }
  }

  // TODO: investigate some API changes to SampleElements that would tidy up
  // parts of the following classes.

  static <K, V> TestSetGenerator<K> keySetGenerator(
      OneSizeTestContainerGenerator<Map<K, V>, Entry<K, V>> mapGenerator) {
    TestContainerGenerator<Map<K, V>, Entry<K, V>> generator = mapGenerator.getInnerGenerator();
    if (generator instanceof TestSortedMapGenerator
        && ((TestSortedMapGenerator<K, V>) generator).create().keySet() instanceof SortedSet) {
      return new MapSortedKeySetGenerator<>(mapGenerator);
    } else {
      return new MapKeySetGenerator<>(mapGenerator);
    }
  }

  public static class MapKeySetGenerator<K, V> implements TestSetGenerator<K>, DerivedGenerator {
    private final OneSizeTestContainerGenerator<Map<K, V>, Entry<K, V>> mapGenerator;
    private final SampleElements<K> samples;

    public MapKeySetGenerator(OneSizeTestContainerGenerator<Map<K, V>, Entry<K, V>> mapGenerator) {
      this.mapGenerator = mapGenerator;
      final SampleElements<Entry<K, V>> mapSamples = this.mapGenerator.samples();
      this.samples =
          new SampleElements<K>(
              mapSamples.e0().getKey(),
              mapSamples.e1().getKey(),
              mapSamples.e2().getKey(),
              mapSamples.e3().getKey(),
              mapSamples.e4().getKey());
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
      Collection<Entry<K, V>> originalEntries = mapGenerator.getSampleElements(elements.length);

      // Create a copy of that, with the desired value for each key
      Collection<Entry<K, V>> entries = new ArrayList<>(elements.length);
      int i = 0;
      for (Entry<K, V> entry : originalEntries) {
        entries.add(Helpers.mapEntry(keysArray[i++], entry.getValue()));
      }

      return mapGenerator.create(entries.toArray()).keySet();
    }

    @Override
    public K[] createArray(int length) {
      // TODO: with appropriate refactoring of OneSizeGenerator, we can perhaps
      // tidy this up and get rid of the casts here and in
      // MapValueCollectionGenerator.

      return ((TestMapGenerator<K, V>) mapGenerator.getInnerGenerator()).createKeyArray(length);
    }

    @Override
    public Iterable<K> order(List<K> insertionOrder) {
      V v = ((TestMapGenerator<K, V>) mapGenerator.getInnerGenerator()).samples().e0().getValue();
      List<Entry<K, V>> entries = new ArrayList<>();
      for (K element : insertionOrder) {
        entries.add(mapEntry(element, v));
      }

      List<K> keys = new ArrayList<>();
      for (Entry<K, V> entry : mapGenerator.order(entries)) {
        keys.add(entry.getKey());
      }
      return keys;
    }

    @Override
    public OneSizeTestContainerGenerator<Map<K, V>, Entry<K, V>> getInnerGenerator() {
      return mapGenerator;
    }
  }

  public static class MapSortedKeySetGenerator<K, V> extends MapKeySetGenerator<K, V>
      implements TestSortedSetGenerator<K>, DerivedGenerator {
    private final TestSortedMapGenerator<K, V> delegate;

    public MapSortedKeySetGenerator(
        OneSizeTestContainerGenerator<Map<K, V>, Entry<K, V>> mapGenerator) {
      super(mapGenerator);
      this.delegate = (TestSortedMapGenerator<K, V>) mapGenerator.getInnerGenerator();
    }

    @Override
    public SortedSet<K> create(Object... elements) {
      return (SortedSet<K>) super.create(elements);
    }

    @Override
    public K belowSamplesLesser() {
      return delegate.belowSamplesLesser().getKey();
    }

    @Override
    public K belowSamplesGreater() {
      return delegate.belowSamplesGreater().getKey();
    }

    @Override
    public K aboveSamplesLesser() {
      return delegate.aboveSamplesLesser().getKey();
    }

    @Override
    public K aboveSamplesGreater() {
      return delegate.aboveSamplesGreater().getKey();
    }
  }

  public static class MapValueCollectionGenerator<K, V>
      implements TestCollectionGenerator<V>, DerivedGenerator {
    private final OneSizeTestContainerGenerator<Map<K, V>, Entry<K, V>> mapGenerator;
    private final SampleElements<V> samples;

    public MapValueCollectionGenerator(
        OneSizeTestContainerGenerator<Map<K, V>, Entry<K, V>> mapGenerator) {
      this.mapGenerator = mapGenerator;
      final SampleElements<Entry<K, V>> mapSamples = this.mapGenerator.samples();
      this.samples =
          new SampleElements<V>(
              mapSamples.e0().getValue(),
              mapSamples.e1().getValue(),
              mapSamples.e2().getValue(),
              mapSamples.e3().getValue(),
              mapSamples.e4().getValue());
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
      Collection<Entry<K, V>> originalEntries = mapGenerator.getSampleElements(elements.length);

      // Create a copy of that, with the desired value for each value
      Collection<Entry<K, V>> entries = new ArrayList<>(elements.length);
      int i = 0;
      for (Entry<K, V> entry : originalEntries) {
        entries.add(Helpers.mapEntry(entry.getKey(), valuesArray[i++]));
      }

      return mapGenerator.create(entries.toArray()).values();
    }

    @Override
    public V[] createArray(int length) {
      // noinspection UnnecessaryLocalVariable
      final V[] vs =
          ((TestMapGenerator<K, V>) mapGenerator.getInnerGenerator()).createValueArray(length);
      return vs;
    }

    @Override
    public Iterable<V> order(List<V> insertionOrder) {
      final List<Entry<K, V>> orderedEntries =
          castOrCopyToList(mapGenerator.order(castOrCopyToList(mapGenerator.getSampleElements(5))));
      sort(
          insertionOrder,
          new Comparator<V>() {
            @Override
            public int compare(V left, V right) {
              // The indexes are small enough for the subtraction trick to be safe.
              return indexOfEntryWithValue(left) - indexOfEntryWithValue(right);
            }

            int indexOfEntryWithValue(V value) {
              for (int i = 0; i < orderedEntries.size(); i++) {
                if (equal(orderedEntries.get(i).getValue(), value)) {
                  return i;
                }
              }
              throw new IllegalArgumentException(
                  "Map.values generator can order only sample values");
            }
          });
      return insertionOrder;
    }

    @Override
    public OneSizeTestContainerGenerator<Map<K, V>, Entry<K, V>> getInnerGenerator() {
      return mapGenerator;
    }
  }

  // TODO(cpovirk): could something like this be used elsewhere, e.g., ReserializedListGenerator?
  static class ForwardingTestMapGenerator<K, V> implements TestMapGenerator<K, V> {
    TestMapGenerator<K, V> delegate;

    ForwardingTestMapGenerator(TestMapGenerator<K, V> delegate) {
      this.delegate = delegate;
    }

    @Override
    public Iterable<Entry<K, V>> order(List<Entry<K, V>> insertionOrder) {
      return delegate.order(insertionOrder);
    }

    @Override
    public K[] createKeyArray(int length) {
      return delegate.createKeyArray(length);
    }

    @Override
    public V[] createValueArray(int length) {
      return delegate.createValueArray(length);
    }

    @Override
    public SampleElements<Entry<K, V>> samples() {
      return delegate.samples();
    }

    @Override
    public Map<K, V> create(Object... elements) {
      return delegate.create(elements);
    }

    @Override
    public Entry<K, V>[] createArray(int length) {
      return delegate.createArray(length);
    }
  }

  /** Two bounds (from and to) define how to build a subMap. */
  public enum Bound {
    INCLUSIVE,
    EXCLUSIVE,
    NO_BOUND;
  }

  public static class SortedSetSubsetTestSetGenerator<E> implements TestSortedSetGenerator<E> {
    final Bound to;
    final Bound from;
    final E firstInclusive;
    final E lastInclusive;
    private final Comparator<? super E> comparator;
    private final TestSortedSetGenerator<E> delegate;

    public SortedSetSubsetTestSetGenerator(
        TestSortedSetGenerator<E> delegate, Bound to, Bound from) {
      this.to = to;
      this.from = from;
      this.delegate = delegate;

      SortedSet<E> emptySet = delegate.create();
      this.comparator = emptySet.comparator();

      SampleElements<E> samples = delegate.samples();
      List<E> samplesList = new ArrayList<>(samples.asList());
      Collections.sort(samplesList, comparator);
      this.firstInclusive = samplesList.get(0);
      this.lastInclusive = samplesList.get(samplesList.size() - 1);
    }

    public final TestSortedSetGenerator<E> getInnerGenerator() {
      return delegate;
    }

    public final Bound getTo() {
      return to;
    }

    public final Bound getFrom() {
      return from;
    }

    @Override
    public SampleElements<E> samples() {
      return delegate.samples();
    }

    @Override
    public E[] createArray(int length) {
      return delegate.createArray(length);
    }

    @Override
    public Iterable<E> order(List<E> insertionOrder) {
      return delegate.order(insertionOrder);
    }

    @Override
    public SortedSet<E> create(Object... elements) {
      @SuppressWarnings("unchecked") // set generators must pass SampleElements values
      List<E> normalValues = (List) Arrays.asList(elements);
      List<E> extremeValues = new ArrayList<>();

      // nulls are usually out of bounds for a subset, so ban them altogether
      for (Object o : elements) {
        if (o == null) {
          throw new NullPointerException();
        }
      }

      // prepare extreme values to be filtered out of view
      E firstExclusive = delegate.belowSamplesGreater();
      E lastExclusive = delegate.aboveSamplesLesser();
      if (from != Bound.NO_BOUND) {
        extremeValues.add(delegate.belowSamplesLesser());
        extremeValues.add(delegate.belowSamplesGreater());
      }
      if (to != Bound.NO_BOUND) {
        extremeValues.add(delegate.aboveSamplesLesser());
        extremeValues.add(delegate.aboveSamplesGreater());
      }

      // the regular values should be visible after filtering
      List<E> allEntries = new ArrayList<>();
      allEntries.addAll(extremeValues);
      allEntries.addAll(normalValues);
      SortedSet<E> map = delegate.create(allEntries.toArray());

      return createSubSet(map, firstExclusive, lastExclusive);
    }

    /** Calls the smallest subSet overload that filters out the extreme values. */
    SortedSet<E> createSubSet(SortedSet<E> set, E firstExclusive, E lastExclusive) {
      if (from == Bound.NO_BOUND && to == Bound.EXCLUSIVE) {
        return set.headSet(lastExclusive);
      } else if (from == Bound.INCLUSIVE && to == Bound.NO_BOUND) {
        return set.tailSet(firstInclusive);
      } else if (from == Bound.INCLUSIVE && to == Bound.EXCLUSIVE) {
        return set.subSet(firstInclusive, lastExclusive);
      } else {
        throw new IllegalArgumentException();
      }
    }

    @Override
    public E belowSamplesLesser() {
      throw new UnsupportedOperationException();
    }

    @Override
    public E belowSamplesGreater() {
      throw new UnsupportedOperationException();
    }

    @Override
    public E aboveSamplesLesser() {
      throw new UnsupportedOperationException();
    }

    @Override
    public E aboveSamplesGreater() {
      throw new UnsupportedOperationException();
    }
  }

  /*
   * TODO(cpovirk): surely we can find a less ugly solution than a class that accepts 3 parameters,
   * exposes as many getters, does work in the constructor, and has both a superclass and a subclass
   */
  public static class SortedMapSubmapTestMapGenerator<K, V> extends ForwardingTestMapGenerator<K, V>
      implements TestSortedMapGenerator<K, V> {
    final Bound to;
    final Bound from;
    final K firstInclusive;
    final K lastInclusive;
    private final Comparator<Entry<K, V>> entryComparator;

    public SortedMapSubmapTestMapGenerator(
        TestSortedMapGenerator<K, V> delegate, Bound to, Bound from) {
      super(delegate);
      this.to = to;
      this.from = from;

      SortedMap<K, V> emptyMap = delegate.create();
      this.entryComparator = Helpers.entryComparator(emptyMap.comparator());

      // derive values for inclusive filtering from the input samples
      SampleElements<Entry<K, V>> samples = delegate.samples();
      @SuppressWarnings("unchecked") // no elements are inserted into the array
      List<Entry<K, V>> samplesList =
          Arrays.asList(samples.e0(), samples.e1(), samples.e2(), samples.e3(), samples.e4());
      Collections.sort(samplesList, entryComparator);
      this.firstInclusive = samplesList.get(0).getKey();
      this.lastInclusive = samplesList.get(samplesList.size() - 1).getKey();
    }

    @Override
    public SortedMap<K, V> create(Object... entries) {
      @SuppressWarnings("unchecked") // map generators must past entry objects
      List<Entry<K, V>> normalValues = (List) Arrays.asList(entries);
      List<Entry<K, V>> extremeValues = new ArrayList<>();

      // prepare extreme values to be filtered out of view
      K firstExclusive = getInnerGenerator().belowSamplesGreater().getKey();
      K lastExclusive = getInnerGenerator().aboveSamplesLesser().getKey();
      if (from != Bound.NO_BOUND) {
        extremeValues.add(getInnerGenerator().belowSamplesLesser());
        extremeValues.add(getInnerGenerator().belowSamplesGreater());
      }
      if (to != Bound.NO_BOUND) {
        extremeValues.add(getInnerGenerator().aboveSamplesLesser());
        extremeValues.add(getInnerGenerator().aboveSamplesGreater());
      }

      // the regular values should be visible after filtering
      List<Entry<K, V>> allEntries = new ArrayList<>();
      allEntries.addAll(extremeValues);
      allEntries.addAll(normalValues);
      SortedMap<K, V> map =
          (SortedMap<K, V>)
              delegate.create((Object[]) allEntries.toArray(new Entry<?, ?>[allEntries.size()]));

      return createSubMap(map, firstExclusive, lastExclusive);
    }

    /**
     * Calls the smallest subMap overload that filters out the extreme values. This method is
     * overridden in NavigableMapTestSuiteBuilder.
     */
    SortedMap<K, V> createSubMap(SortedMap<K, V> map, K firstExclusive, K lastExclusive) {
      if (from == Bound.NO_BOUND && to == Bound.EXCLUSIVE) {
        return map.headMap(lastExclusive);
      } else if (from == Bound.INCLUSIVE && to == Bound.NO_BOUND) {
        return map.tailMap(firstInclusive);
      } else if (from == Bound.INCLUSIVE && to == Bound.EXCLUSIVE) {
        return map.subMap(firstInclusive, lastExclusive);
      } else {
        throw new IllegalArgumentException();
      }
    }

    public final Bound getTo() {
      return to;
    }

    public final Bound getFrom() {
      return from;
    }

    public final TestSortedMapGenerator<K, V> getInnerGenerator() {
      return (TestSortedMapGenerator<K, V>) delegate;
    }

    @Override
    public Entry<K, V> belowSamplesLesser() {
      // should never reach here!
      throw new UnsupportedOperationException();
    }

    @Override
    public Entry<K, V> belowSamplesGreater() {
      // should never reach here!
      throw new UnsupportedOperationException();
    }

    @Override
    public Entry<K, V> aboveSamplesLesser() {
      // should never reach here!
      throw new UnsupportedOperationException();
    }

    @Override
    public Entry<K, V> aboveSamplesGreater() {
      // should never reach here!
      throw new UnsupportedOperationException();
    }
  }

  private DerivedCollectionGenerators() {}
}

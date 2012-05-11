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
import com.google.common.collect.testing.DerivedCollectionGenerators.SortedMapSubmapTestMapGenerator;

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

/**
 * Derived suite generators, split out of the suite builders so that they are available to GWT.
 *
 * @author George van den Driessche
 */
@GwtCompatible
public final class DerivedCollectionGenerators {
  public static class MapEntrySetGenerator<K, V>
      implements TestSetGenerator<Map.Entry<K, V>>, DerivedGenerator {
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

    public OneSizeTestContainerGenerator<Map<K, V>, Map.Entry<K, V>> getInnerGenerator() {
      return mapGenerator;
    }
  }

  // TODO: investigate some API changes to SampleElements that would tidy up
  // parts of the following classes.

  public static class MapKeySetGenerator<K, V>
      implements TestSetGenerator<K>, DerivedGenerator {
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
      V v = ((TestMapGenerator<K, V>) mapGenerator.getInnerGenerator()).samples().e0.getValue();
      List<Entry<K, V>> entries = new ArrayList<Entry<K, V>>();
      for (K element : insertionOrder) {
        entries.add(mapEntry(element, v));
      }

      List<K> keys = new ArrayList<K>();
      for (Entry<K, V> entry : mapGenerator.order(entries)) {
        keys.add(entry.getKey());
      }
      return keys;
    }

    public OneSizeTestContainerGenerator<Map<K, V>, Map.Entry<K, V>> getInnerGenerator() {
      return mapGenerator;
    }
  }

  public static class MapValueCollectionGenerator<K, V>
      implements TestCollectionGenerator<V>, DerivedGenerator {
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
      final List<Entry<K, V>> orderedEntries =
          castOrCopyToList(mapGenerator.order(castOrCopyToList(mapGenerator.getSampleElements(5))));
      sort(insertionOrder, new Comparator<V>() {
        @Override public int compare(V left, V right) {
          // The indexes are small enough for the subtraction trick to be safe.
          return indexOfEntryWithValue(left) - indexOfEntryWithValue(right);
        }

        int indexOfEntryWithValue(V value) {
          for (int i = 0; i < orderedEntries.size(); i++) {
            if (equal(orderedEntries.get(i).getValue(), value)) {
              return i;
            }
          }
          throw new IllegalArgumentException("Map.values generator can order only sample values");
        }
      });
      return insertionOrder;
    }

    public OneSizeTestContainerGenerator<Map<K, V>, Map.Entry<K, V>> getInnerGenerator() {
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

  /**
   * Two bounds (from and to) define how to build a subMap.
   */
  public enum Bound {
    INCLUSIVE,
    EXCLUSIVE,
    NO_BOUND;
  }

  /*
   * TODO(cpovirk): surely we can find a less ugly solution than a class that accepts 3 parameters,
   * exposes as many getters, does work in the constructor, and has both a superclass and a subclass
   */
  public static class SortedMapSubmapTestMapGenerator<K, V>
      extends ForwardingTestMapGenerator<K, V> {
    final Bound to;
    final Bound from;
    final K firstInclusive;
    final K lastInclusive;
    private final Comparator<Entry<K, V>> entryComparator;

    public SortedMapSubmapTestMapGenerator(TestMapGenerator<K, V> delegate, Bound to, Bound from) {
      super(delegate);
      this.to = to;
      this.from = from;

      SortedMap<K, V> emptyMap = (SortedMap<K, V>) delegate.create();
      this.entryComparator = Helpers.entryComparator(emptyMap.comparator());

      // derive values for inclusive filtering from the input samples
      SampleElements<Entry<K, V>> samples = delegate.samples();
      @SuppressWarnings("unchecked") // no elements are inserted into the array
      List<Entry<K, V>> samplesList = Arrays.asList(
          samples.e0, samples.e1, samples.e2, samples.e3, samples.e4);
      Collections.sort(samplesList, entryComparator);
      this.firstInclusive = samplesList.get(0).getKey();
      this.lastInclusive = samplesList.get(samplesList.size() - 1).getKey();
    }

    @Override public Map<K, V> create(Object... entries) {
      @SuppressWarnings("unchecked") // we dangerously assume K and V are both strings
      List<Entry<K, V>> extremeValues = (List) getExtremeValues();
      @SuppressWarnings("unchecked") // map generators must past entry objects
      List<Entry<K, V>> normalValues = (List) Arrays.asList(entries);

      // prepare extreme values to be filtered out of view
      Collections.sort(extremeValues, entryComparator);
      K firstExclusive = extremeValues.get(1).getKey();
      K lastExclusive = extremeValues.get(2).getKey();
      if (from == Bound.NO_BOUND) {
        extremeValues.remove(0);
        extremeValues.remove(0);
      }
      if (to == Bound.NO_BOUND) {
        extremeValues.remove(extremeValues.size() - 1);
        extremeValues.remove(extremeValues.size() - 1);
      }

      // the regular values should be visible after filtering
      List<Entry<K, V>> allEntries = new ArrayList<Entry<K, V>>();
      allEntries.addAll(extremeValues);
      allEntries.addAll(normalValues);
      SortedMap<K, V> map = (SortedMap<K, V>)
          delegate.create((Object[])
              allEntries.toArray(new Entry[allEntries.size()]));

      return createSubMap(map, firstExclusive, lastExclusive);
    }

    /**
     * Calls the smallest subMap overload that filters out the extreme values. This method is
     * overridden in NavigableMapTestSuiteBuilder.
     */
    Map<K, V> createSubMap(SortedMap<K, V> map, K firstExclusive, K lastExclusive) {
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

    public final TestMapGenerator<K, V> getInnerGenerator() {
      return delegate;
    }
  }

  /**
   * Returns an array of four bogus elements that will always be too high or
   * too low for the display. This includes two values for each extreme.
   *
   * <p>This method (dangerously) assume that the strings {@code "!! a"} and
   * {@code "~~ z"} will work for this purpose, which may cause problems for
   * navigable maps with non-string or unicode generators.
   */
  private static List<Entry<String, String>> getExtremeValues() {
    List<Entry<String, String>> result = new ArrayList<Entry<String, String>>();
    result.add(Helpers.mapEntry("!! a", "below view"));
    result.add(Helpers.mapEntry("!! b", "below view"));
    result.add(Helpers.mapEntry("~~ y", "above view"));
    result.add(Helpers.mapEntry("~~ z", "above view"));
    return result;
  }

  private DerivedCollectionGenerators() {}
}

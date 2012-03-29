/*
 * Copyright (C) 2010 The Guava Authors
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
import com.google.common.collect.testing.features.Feature;
import com.google.common.collect.testing.testers.SortedMapNavigationTester;

import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Creates, based on your criteria, a JUnit test suite that exhaustively tests
 * a SortedMap implementation.
 */
public class SortedMapTestSuiteBuilder<K, V> extends MapTestSuiteBuilder<K, V> {
  public static <K, V> SortedMapTestSuiteBuilder<K, V> using(
      TestMapGenerator<K, V> generator) {
    SortedMapTestSuiteBuilder<K, V> result = new SortedMapTestSuiteBuilder<K, V>();
    result.usingGenerator(generator);
    return result;
  }

  @Override protected List<Class<? extends AbstractTester>> getTesters() {
    List<Class<? extends AbstractTester>> testers = Helpers.copyToList(super.getTesters());
    testers.add(SortedMapNavigationTester.class);
    return testers;
  }

  @Override public TestSuite createTestSuite() {
    if (!getFeatures().contains(CollectionFeature.KNOWN_ORDER)) {
      List<Feature<?>> features = Helpers.copyToList(getFeatures());
      features.add(CollectionFeature.KNOWN_ORDER);
      withFeatures(features);
    }
    return super.createTestSuite();
  }

  @Override
  protected List<TestSuite> createDerivedSuites(FeatureSpecificTestSuiteBuilder<?,
      ? extends OneSizeTestContainerGenerator<Map<K, V>, Entry<K, V>>> parentBuilder) {
    List<TestSuite> derivedSuites = super.createDerivedSuites(parentBuilder);

    if (!parentBuilder.getFeatures().contains(NoRecurse.SUBMAP)) {
      derivedSuites.add(createSubmapSuite(parentBuilder, Bound.NO_BOUND, Bound.EXCLUSIVE));
      derivedSuites.add(createSubmapSuite(parentBuilder, Bound.INCLUSIVE, Bound.NO_BOUND));
      derivedSuites.add(createSubmapSuite(parentBuilder, Bound.INCLUSIVE, Bound.EXCLUSIVE));
    }

    return derivedSuites;
  }

  @Override protected SetTestSuiteBuilder<K> createDerivedKeySetSuite(
      TestSetGenerator<K> keySetGenerator) {
    /*
     * TODO(cpovirk): Consider requiring a SortedSet by default and requiring tests of a given
     * implementation to opt out if they wish to return Set. This would encourage us to return
     * keySets that implement SortedSet
     */
    return (keySetGenerator.create() instanceof SortedSet)
        ? SortedSetTestSuiteBuilder.using(keySetGenerator)
        : SetTestSuiteBuilder.using(keySetGenerator);
  }

  /**
   * To avoid infinite recursion, test suites with these marker features won't
   * have derived suites created for them.
   */
  enum NoRecurse implements Feature<Void> {
    SUBMAP,
    DESCENDING;

    @Override
    public Set<Feature<? super Void>> getImpliedFeatures() {
      return Collections.emptySet();
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

  /**
   * Creates a suite whose map has some elements filtered out of view.
   *
   * <p>Because the map may be ascending or descending, this test must derive
   * the relative order of these extreme values rather than relying on their
   * regular sort ordering.
   */
  final TestSuite createSubmapSuite(final FeatureSpecificTestSuiteBuilder<?,
          ? extends OneSizeTestContainerGenerator<Map<K, V>, Entry<K, V>>>
          parentBuilder, final Bound from, final Bound to) {
    final TestMapGenerator<K, V> delegate
        = (TestMapGenerator<K, V>) parentBuilder.getSubjectGenerator().getInnerGenerator();

    List<Feature<?>> features = new ArrayList<Feature<?>>();
    features.add(NoRecurse.SUBMAP);
    features.addAll(parentBuilder.getFeatures());

    return newBuilderUsing(delegate, to, from)
        .named(parentBuilder.getName() + " subMap " + from + "-" + to)
        .withFeatures(features)
        .suppressing(parentBuilder.getSuppressedTests())
        .createTestSuite();
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

  /** Like using() but overrideable by NavigableMapTestSuiteBuilder. */
  SortedMapTestSuiteBuilder<K, V> newBuilderUsing(
      TestMapGenerator<K, V> delegate, Bound to, Bound from) {
    return using(new SortedMapSubmapTestMapGenerator<K, V>(delegate, to, from));
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
}

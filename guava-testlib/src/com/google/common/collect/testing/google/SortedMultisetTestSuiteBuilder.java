/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect.testing.google;

import static com.google.common.collect.testing.features.CollectionFeature.KNOWN_ORDER;
import static com.google.common.collect.testing.features.CollectionFeature.RESTRICTS_ELEMENTS;
import static com.google.common.collect.testing.features.CollectionFeature.SERIALIZABLE;
import static com.google.common.collect.testing.features.CollectionFeature.SERIALIZABLE_INCLUDING_VIEWS;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.BoundType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.testing.AbstractTester;
import com.google.common.collect.testing.FeatureSpecificTestSuiteBuilder;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.OneSizeTestContainerGenerator;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.features.Feature;
import com.google.common.testing.SerializableTester;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import junit.framework.TestSuite;

/**
 * Creates, based on your criteria, a JUnit test suite that exhaustively tests a {@code
 * SortedMultiset} implementation.
 *
 * <p><b>Warning:</b> expects that {@code E} is a String.
 *
 * @author Louis Wasserman
 */
@GwtIncompatible
public class SortedMultisetTestSuiteBuilder<E> extends MultisetTestSuiteBuilder<E> {
  public static <E> SortedMultisetTestSuiteBuilder<E> using(TestMultisetGenerator<E> generator) {
    SortedMultisetTestSuiteBuilder<E> result = new SortedMultisetTestSuiteBuilder<E>();
    result.usingGenerator(generator);
    return result;
  }

  @Override
  public TestSuite createTestSuite() {
    withFeatures(KNOWN_ORDER);
    TestSuite suite = super.createTestSuite();
    for (TestSuite subSuite : createDerivedSuites(this)) {
      suite.addTest(subSuite);
    }
    return suite;
  }

  @Override
  protected List<Class<? extends AbstractTester>> getTesters() {
    List<Class<? extends AbstractTester>> testers = Helpers.copyToList(super.getTesters());
    testers.add(MultisetNavigationTester.class);
    return testers;
  }

  @Override
  TestSuite createElementSetTestSuite(
      FeatureSpecificTestSuiteBuilder<?, ? extends OneSizeTestContainerGenerator<Collection<E>, E>>
          parentBuilder) {
    // TODO(lowasser): make a SortedElementSetGenerator
    return SetTestSuiteBuilder.using(
            new ElementSetGenerator<E>(parentBuilder.getSubjectGenerator()))
        .named(getName() + ".elementSet")
        .withFeatures(computeElementSetFeatures(parentBuilder.getFeatures()))
        .suppressing(parentBuilder.getSuppressedTests())
        .createTestSuite();
  }

  /**
   * To avoid infinite recursion, test suites with these marker features won't have derived suites
   * created for them.
   */
  enum NoRecurse implements Feature<Void> {
    SUBMULTISET,
    DESCENDING;

    @Override
    public Set<Feature<? super Void>> getImpliedFeatures() {
      return Collections.emptySet();
    }
  }

  /** Two bounds (from and to) define how to build a subMultiset. */
  enum Bound {
    INCLUSIVE,
    EXCLUSIVE,
    NO_BOUND;
  }

  List<TestSuite> createDerivedSuites(SortedMultisetTestSuiteBuilder<E> parentBuilder) {
    List<TestSuite> derivedSuites = Lists.newArrayList();

    if (!parentBuilder.getFeatures().contains(NoRecurse.DESCENDING)) {
      derivedSuites.add(createDescendingSuite(parentBuilder));
    }

    if (parentBuilder.getFeatures().contains(SERIALIZABLE)) {
      derivedSuites.add(createReserializedSuite(parentBuilder));
    }

    if (!parentBuilder.getFeatures().contains(NoRecurse.SUBMULTISET)) {
      derivedSuites.add(createSubMultisetSuite(parentBuilder, Bound.NO_BOUND, Bound.EXCLUSIVE));
      derivedSuites.add(createSubMultisetSuite(parentBuilder, Bound.NO_BOUND, Bound.INCLUSIVE));
      derivedSuites.add(createSubMultisetSuite(parentBuilder, Bound.EXCLUSIVE, Bound.NO_BOUND));
      derivedSuites.add(createSubMultisetSuite(parentBuilder, Bound.EXCLUSIVE, Bound.EXCLUSIVE));
      derivedSuites.add(createSubMultisetSuite(parentBuilder, Bound.EXCLUSIVE, Bound.INCLUSIVE));
      derivedSuites.add(createSubMultisetSuite(parentBuilder, Bound.INCLUSIVE, Bound.NO_BOUND));
      derivedSuites.add(createSubMultisetSuite(parentBuilder, Bound.INCLUSIVE, Bound.EXCLUSIVE));
      derivedSuites.add(createSubMultisetSuite(parentBuilder, Bound.INCLUSIVE, Bound.INCLUSIVE));
    }

    return derivedSuites;
  }

  private TestSuite createSubMultisetSuite(
      SortedMultisetTestSuiteBuilder<E> parentBuilder, final Bound from, final Bound to) {
    final TestMultisetGenerator<E> delegate =
        (TestMultisetGenerator<E>) parentBuilder.getSubjectGenerator();

    Set<Feature<?>> features = new HashSet<>();
    features.add(NoRecurse.SUBMULTISET);
    features.add(RESTRICTS_ELEMENTS);
    features.addAll(parentBuilder.getFeatures());

    if (!features.remove(SERIALIZABLE_INCLUDING_VIEWS)) {
      features.remove(SERIALIZABLE);
    }

    SortedMultiset<E> emptyMultiset = (SortedMultiset<E>) delegate.create();
    final Comparator<? super E> comparator = emptyMultiset.comparator();
    SampleElements<E> samples = delegate.samples();
    @SuppressWarnings("unchecked")
    List<E> samplesList =
        Arrays.asList(samples.e0(), samples.e1(), samples.e2(), samples.e3(), samples.e4());

    Collections.sort(samplesList, comparator);
    final E firstInclusive = samplesList.get(0);
    final E lastInclusive = samplesList.get(samplesList.size() - 1);

    return SortedMultisetTestSuiteBuilder.using(
            new ForwardingTestMultisetGenerator<E>(delegate) {
              @Override
              public SortedMultiset<E> create(Object... entries) {
                @SuppressWarnings("unchecked")
                // we dangerously assume E is a string
                List<E> extremeValues = (List<E>) getExtremeValues();
                @SuppressWarnings("unchecked")
                // map generators must past entry objects
                List<E> normalValues = (List<E>) Arrays.asList(entries);

                // prepare extreme values to be filtered out of view
                Collections.sort(extremeValues, comparator);
                E firstExclusive = extremeValues.get(1);
                E lastExclusive = extremeValues.get(2);
                if (from == Bound.NO_BOUND) {
                  extremeValues.remove(0);
                  extremeValues.remove(0);
                }
                if (to == Bound.NO_BOUND) {
                  extremeValues.remove(extremeValues.size() - 1);
                  extremeValues.remove(extremeValues.size() - 1);
                }

                // the regular values should be visible after filtering
                List<E> allEntries = new ArrayList<E>();
                allEntries.addAll(extremeValues);
                allEntries.addAll(normalValues);
                SortedMultiset<E> multiset =
                    (SortedMultiset<E>) delegate.create(allEntries.toArray());

                // call the smallest subMap overload that filters out the extreme
                // values
                if (from == Bound.INCLUSIVE) {
                  multiset = multiset.tailMultiset(firstInclusive, BoundType.CLOSED);
                } else if (from == Bound.EXCLUSIVE) {
                  multiset = multiset.tailMultiset(firstExclusive, BoundType.OPEN);
                }

                if (to == Bound.INCLUSIVE) {
                  multiset = multiset.headMultiset(lastInclusive, BoundType.CLOSED);
                } else if (to == Bound.EXCLUSIVE) {
                  multiset = multiset.headMultiset(lastExclusive, BoundType.OPEN);
                }

                return multiset;
              }
            })
        .named(parentBuilder.getName() + " subMultiset " + from + "-" + to)
        .withFeatures(features)
        .suppressing(parentBuilder.getSuppressedTests())
        .createTestSuite();
  }

  /**
   * Returns an array of four bogus elements that will always be too high or too low for the
   * display. This includes two values for each extreme.
   *
   * <p>This method (dangerously) assume that the strings {@code "!! a"} and {@code "~~ z"} will
   * work for this purpose, which may cause problems for navigable maps with non-string or unicode
   * generators.
   */
  private List<String> getExtremeValues() {
    List<String> result = new ArrayList<>();
    result.add("!! a");
    result.add("!! b");
    result.add("~~ y");
    result.add("~~ z");
    return result;
  }

  private TestSuite createDescendingSuite(SortedMultisetTestSuiteBuilder<E> parentBuilder) {
    final TestMultisetGenerator<E> delegate =
        (TestMultisetGenerator<E>) parentBuilder.getSubjectGenerator();

    Set<Feature<?>> features = new HashSet<>();
    features.add(NoRecurse.DESCENDING);
    features.addAll(parentBuilder.getFeatures());
    if (!features.remove(SERIALIZABLE_INCLUDING_VIEWS)) {
      features.remove(SERIALIZABLE);
    }

    return SortedMultisetTestSuiteBuilder.using(
            new ForwardingTestMultisetGenerator<E>(delegate) {
              @Override
              public SortedMultiset<E> create(Object... entries) {
                return ((SortedMultiset<E>) super.create(entries)).descendingMultiset();
              }

              @Override
              public Iterable<E> order(List<E> insertionOrder) {
                return ImmutableList.copyOf(super.order(insertionOrder)).reverse();
              }
            })
        .named(parentBuilder.getName() + " descending")
        .withFeatures(features)
        .suppressing(parentBuilder.getSuppressedTests())
        .createTestSuite();
  }

  private TestSuite createReserializedSuite(SortedMultisetTestSuiteBuilder<E> parentBuilder) {
    final TestMultisetGenerator<E> delegate =
        (TestMultisetGenerator<E>) parentBuilder.getSubjectGenerator();

    Set<Feature<?>> features = new HashSet<>();
    features.addAll(parentBuilder.getFeatures());
    features.remove(SERIALIZABLE);
    features.remove(SERIALIZABLE_INCLUDING_VIEWS);

    return SortedMultisetTestSuiteBuilder.using(
            new ForwardingTestMultisetGenerator<E>(delegate) {
              @Override
              public SortedMultiset<E> create(Object... entries) {
                return SerializableTester.reserialize(((SortedMultiset<E>) super.create(entries)));
              }
            })
        .named(parentBuilder.getName() + " reserialized")
        .withFeatures(features)
        .suppressing(parentBuilder.getSuppressedTests())
        .createTestSuite();
  }

  private static class ForwardingTestMultisetGenerator<E> implements TestMultisetGenerator<E> {
    private final TestMultisetGenerator<E> delegate;

    ForwardingTestMultisetGenerator(TestMultisetGenerator<E> delegate) {
      this.delegate = delegate;
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
    public Multiset<E> create(Object... elements) {
      return delegate.create(elements);
    }
  }
}

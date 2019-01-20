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

package com.google.common.collect.testing.google;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Multisets;
import com.google.common.collect.testing.AbstractCollectionTestSuiteBuilder;
import com.google.common.collect.testing.AbstractTester;
import com.google.common.collect.testing.FeatureSpecificTestSuiteBuilder;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.OneSizeTestContainerGenerator;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.TestSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.Feature;
import com.google.common.collect.testing.testers.CollectionSerializationEqualTester;
import com.google.common.testing.SerializableTester;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import junit.framework.TestSuite;

/**
 * Creates, based on your criteria, a JUnit test suite that exhaustively tests a {@code Multiset}
 * implementation.
 *
 * @author Jared Levy
 * @author Louis Wasserman
 */
@GwtIncompatible
public class MultisetTestSuiteBuilder<E>
    extends AbstractCollectionTestSuiteBuilder<MultisetTestSuiteBuilder<E>, E> {
  public static <E> MultisetTestSuiteBuilder<E> using(TestMultisetGenerator<E> generator) {
    return new MultisetTestSuiteBuilder<E>().usingGenerator(generator);
  }

  public enum NoRecurse implements Feature<Void> {
    NO_ENTRY_SET;

    @Override
    public Set<Feature<? super Void>> getImpliedFeatures() {
      return Collections.emptySet();
    }
  }

  @Override
  protected List<Class<? extends AbstractTester>> getTesters() {
    List<Class<? extends AbstractTester>> testers = Helpers.copyToList(super.getTesters());
    testers.add(CollectionSerializationEqualTester.class);
    testers.add(MultisetAddTester.class);
    testers.add(MultisetContainsTester.class);
    testers.add(MultisetCountTester.class);
    testers.add(MultisetElementSetTester.class);
    testers.add(MultisetEqualsTester.class);
    testers.add(MultisetForEachEntryTester.class);
    testers.add(MultisetReadsTester.class);
    testers.add(MultisetSetCountConditionallyTester.class);
    testers.add(MultisetSetCountUnconditionallyTester.class);
    testers.add(MultisetRemoveTester.class);
    testers.add(MultisetEntrySetTester.class);
    testers.add(MultisetIteratorTester.class);
    testers.add(MultisetSerializationTester.class);
    return testers;
  }

  private static Set<Feature<?>> computeEntrySetFeatures(Set<Feature<?>> features) {
    Set<Feature<?>> derivedFeatures = new HashSet<>();
    derivedFeatures.addAll(features);
    derivedFeatures.remove(CollectionFeature.GENERAL_PURPOSE);
    derivedFeatures.remove(CollectionFeature.SUPPORTS_ADD);
    derivedFeatures.remove(CollectionFeature.ALLOWS_NULL_VALUES);
    derivedFeatures.add(CollectionFeature.REJECTS_DUPLICATES_AT_CREATION);
    if (!derivedFeatures.remove(CollectionFeature.SERIALIZABLE_INCLUDING_VIEWS)) {
      derivedFeatures.remove(CollectionFeature.SERIALIZABLE);
    }
    return derivedFeatures;
  }

  static Set<Feature<?>> computeElementSetFeatures(Set<Feature<?>> features) {
    Set<Feature<?>> derivedFeatures = new HashSet<>();
    derivedFeatures.addAll(features);
    derivedFeatures.remove(CollectionFeature.GENERAL_PURPOSE);
    derivedFeatures.remove(CollectionFeature.SUPPORTS_ADD);
    if (!derivedFeatures.remove(CollectionFeature.SERIALIZABLE_INCLUDING_VIEWS)) {
      derivedFeatures.remove(CollectionFeature.SERIALIZABLE);
    }
    return derivedFeatures;
  }

  private static Set<Feature<?>> computeReserializedMultisetFeatures(Set<Feature<?>> features) {
    Set<Feature<?>> derivedFeatures = new HashSet<>();
    derivedFeatures.addAll(features);
    derivedFeatures.remove(CollectionFeature.SERIALIZABLE);
    derivedFeatures.remove(CollectionFeature.SERIALIZABLE_INCLUDING_VIEWS);
    return derivedFeatures;
  }

  @Override
  protected List<TestSuite> createDerivedSuites(
      FeatureSpecificTestSuiteBuilder<?, ? extends OneSizeTestContainerGenerator<Collection<E>, E>>
          parentBuilder) {
    List<TestSuite> derivedSuites = new ArrayList<>(super.createDerivedSuites(parentBuilder));

    derivedSuites.add(createElementSetTestSuite(parentBuilder));

    if (!parentBuilder.getFeatures().contains(NoRecurse.NO_ENTRY_SET)) {
      derivedSuites.add(
          SetTestSuiteBuilder.using(new EntrySetGenerator<E>(parentBuilder.getSubjectGenerator()))
              .named(getName() + ".entrySet")
              .withFeatures(computeEntrySetFeatures(parentBuilder.getFeatures()))
              .suppressing(parentBuilder.getSuppressedTests())
              .createTestSuite());
    }

    if (parentBuilder.getFeatures().contains(CollectionFeature.SERIALIZABLE)) {
      derivedSuites.add(
          MultisetTestSuiteBuilder.using(
                  new ReserializedMultisetGenerator<E>(parentBuilder.getSubjectGenerator()))
              .named(getName() + " reserialized")
              .withFeatures(computeReserializedMultisetFeatures(parentBuilder.getFeatures()))
              .suppressing(parentBuilder.getSuppressedTests())
              .createTestSuite());
    }
    return derivedSuites;
  }

  TestSuite createElementSetTestSuite(
      FeatureSpecificTestSuiteBuilder<?, ? extends OneSizeTestContainerGenerator<Collection<E>, E>>
          parentBuilder) {
    return SetTestSuiteBuilder.using(
            new ElementSetGenerator<E>(parentBuilder.getSubjectGenerator()))
        .named(getName() + ".elementSet")
        .withFeatures(computeElementSetFeatures(parentBuilder.getFeatures()))
        .suppressing(parentBuilder.getSuppressedTests())
        .createTestSuite();
  }

  static class ElementSetGenerator<E> implements TestSetGenerator<E> {
    final OneSizeTestContainerGenerator<Collection<E>, E> gen;

    ElementSetGenerator(OneSizeTestContainerGenerator<Collection<E>, E> gen) {
      this.gen = gen;
    }

    @Override
    public SampleElements<E> samples() {
      return gen.samples();
    }

    @Override
    public Set<E> create(Object... elements) {
      Object[] duplicated = new Object[elements.length * 2];
      for (int i = 0; i < elements.length; i++) {
        duplicated[i] = elements[i];
        duplicated[i + elements.length] = elements[i];
      }
      return ((Multiset<E>) gen.create(duplicated)).elementSet();
    }

    @Override
    public E[] createArray(int length) {
      return gen.createArray(length);
    }

    @Override
    public Iterable<E> order(List<E> insertionOrder) {
      return gen.order(new ArrayList<E>(new LinkedHashSet<E>(insertionOrder)));
    }
  }

  static class EntrySetGenerator<E> implements TestSetGenerator<Multiset.Entry<E>> {
    final OneSizeTestContainerGenerator<Collection<E>, E> gen;

    private EntrySetGenerator(OneSizeTestContainerGenerator<Collection<E>, E> gen) {
      this.gen = gen;
    }

    @Override
    public SampleElements<Multiset.Entry<E>> samples() {
      SampleElements<E> samples = gen.samples();
      return new SampleElements<>(
          Multisets.immutableEntry(samples.e0(), 3),
          Multisets.immutableEntry(samples.e1(), 4),
          Multisets.immutableEntry(samples.e2(), 1),
          Multisets.immutableEntry(samples.e3(), 5),
          Multisets.immutableEntry(samples.e4(), 2));
    }

    @Override
    public Set<Multiset.Entry<E>> create(Object... entries) {
      List<Object> contents = new ArrayList<>();
      Set<E> elements = new HashSet<>();
      for (Object o : entries) {
        @SuppressWarnings("unchecked")
        Multiset.Entry<E> entry = (Entry<E>) o;
        checkArgument(
            elements.add(entry.getElement()), "Duplicate keys not allowed in EntrySetGenerator");
        for (int i = 0; i < entry.getCount(); i++) {
          contents.add(entry.getElement());
        }
      }
      return ((Multiset<E>) gen.create(contents.toArray())).entrySet();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Multiset.Entry<E>[] createArray(int length) {
      return new Multiset.Entry[length];
    }

    @Override
    public Iterable<Entry<E>> order(List<Entry<E>> insertionOrder) {
      // We mimic the order from gen.
      Map<E, Entry<E>> map = new LinkedHashMap<>();
      for (Entry<E> entry : insertionOrder) {
        map.put(entry.getElement(), entry);
      }

      Set<E> seen = new HashSet<>();
      List<Entry<E>> order = new ArrayList<>();
      for (E e : gen.order(new ArrayList<E>(map.keySet()))) {
        if (seen.add(e)) {
          order.add(map.get(e));
        }
      }
      return order;
    }
  }

  static class ReserializedMultisetGenerator<E> implements TestMultisetGenerator<E> {
    final OneSizeTestContainerGenerator<Collection<E>, E> gen;

    private ReserializedMultisetGenerator(OneSizeTestContainerGenerator<Collection<E>, E> gen) {
      this.gen = gen;
    }

    @Override
    public SampleElements<E> samples() {
      return gen.samples();
    }

    @Override
    public Multiset<E> create(Object... elements) {
      return (Multiset<E>) SerializableTester.reserialize(gen.create(elements));
    }

    @Override
    public E[] createArray(int length) {
      return gen.createArray(length);
    }

    @Override
    public Iterable<E> order(List<E> insertionOrder) {
      return gen.order(insertionOrder);
    }
  }
}

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

import com.google.common.collect.Multiset;
import com.google.common.collect.testing.AbstractCollectionTestSuiteBuilder;
import com.google.common.collect.testing.AbstractTester;
import com.google.common.collect.testing.FeatureSpecificTestSuiteBuilder;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.OneSizeTestContainerGenerator;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.Feature;
import com.google.common.collect.testing.testers.CollectionSerializationEqualTester;
import com.google.common.testing.SerializableTester;

import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Creates, based on your criteria, a JUnit test suite that exhaustively tests
 * a {@code Multiset} implementation.
 *
 * @author Jared Levy
 * @author Louis Wasserman
 */
public class MultisetTestSuiteBuilder<E> extends
    AbstractCollectionTestSuiteBuilder<MultisetTestSuiteBuilder<E>, E> {
  public static <E> MultisetTestSuiteBuilder<E> using(
      TestMultisetGenerator<E> generator) {
    return new MultisetTestSuiteBuilder<E>().usingGenerator(generator);
  }

  @Override protected List<Class<? extends AbstractTester>> getTesters() {
    List<Class<? extends AbstractTester>> testers
        = Helpers.copyToList(super.getTesters());
    testers.add(CollectionSerializationEqualTester.class);
    testers.add(MultisetReadsTester.class);
    testers.add(MultisetSetCountConditionallyTester.class);
    testers.add(MultisetSetCountUnconditionallyTester.class);
    testers.add(MultisetWritesTester.class);
    testers.add(MultisetIteratorTester.class);
    testers.add(MultisetSerializationTester.class);
    return testers;
  }

  private static Set<Feature<?>> computeReserializedMultisetFeatures(
      Set<Feature<?>> features) {
    Set<Feature<?>> derivedFeatures = new HashSet<Feature<?>>();
    derivedFeatures.addAll(features);
    derivedFeatures.remove(CollectionFeature.SERIALIZABLE);
    derivedFeatures.remove(CollectionFeature.SERIALIZABLE_INCLUDING_VIEWS);
    return derivedFeatures;
  }

  @Override
  protected
      List<TestSuite>
      createDerivedSuites(
          FeatureSpecificTestSuiteBuilder<
              ?, ? extends OneSizeTestContainerGenerator<Collection<E>, E>> parentBuilder) {
    List<TestSuite> derivedSuites = new ArrayList<TestSuite>(
        super.createDerivedSuites(parentBuilder));

    if (parentBuilder.getFeatures().contains(CollectionFeature.SERIALIZABLE)) {
      derivedSuites.add(MultisetTestSuiteBuilder
          .using(new ReserializedMultisetGenerator<E>(parentBuilder.getSubjectGenerator()))
          .named(getName() + " reserialized")
          .withFeatures(computeReserializedMultisetFeatures(parentBuilder.getFeatures()))
          .suppressing(parentBuilder.getSuppressedTests())
          .createTestSuite());
    }
    return derivedSuites;
  }

  static class ReserializedMultisetGenerator<E> implements TestMultisetGenerator<E>{
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


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
import com.google.common.collect.testing.features.Feature;
import com.google.common.testing.SerializableTester;

import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Concrete instantiation of {@link AbstractCollectionTestSuiteBuilder} for
 * testing collections that do not have a more specific tester like
 * {@link ListTestSuiteBuilder} or {@link SetTestSuiteBuilder}.
 *
 * @author Chris Povirk
 * @author Louis Wasserman
 */
public class CollectionTestSuiteBuilder<E>
    extends AbstractCollectionTestSuiteBuilder<
        CollectionTestSuiteBuilder<E>, E> {
  public static <E> CollectionTestSuiteBuilder<E> using(
      TestCollectionGenerator<E> generator) {
    return new CollectionTestSuiteBuilder<E>().usingGenerator(generator);
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
      derivedSuites.add(CollectionTestSuiteBuilder
          .using(new ReserializedCollectionGenerator<E>(parentBuilder.getSubjectGenerator()))
          .named(getName() + " reserialized")
          .withFeatures(computeReserializedCollectionFeatures(parentBuilder.getFeatures()))
          .suppressing(parentBuilder.getSuppressedTests())
          .createTestSuite());
    }
    return derivedSuites;
  }

  static class ReserializedCollectionGenerator<E> implements TestCollectionGenerator<E> {
    final OneSizeTestContainerGenerator<Collection<E>, E> gen;

    private ReserializedCollectionGenerator(OneSizeTestContainerGenerator<Collection<E>, E> gen) {
      this.gen = gen;
    }

    @Override
    public SampleElements<E> samples() {
      return gen.samples();
    }

    @Override
    public Collection<E> create(Object... elements) {
      return SerializableTester.reserialize(gen.create(elements));
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

  private static Set<Feature<?>> computeReserializedCollectionFeatures(Set<Feature<?>> features) {
    Set<Feature<?>> derivedFeatures = new HashSet<Feature<?>>();
    derivedFeatures.addAll(features);
    derivedFeatures.remove(CollectionFeature.SERIALIZABLE);
    derivedFeatures.remove(CollectionFeature.SERIALIZABLE_INCLUDING_VIEWS);
    return derivedFeatures;
  }
}

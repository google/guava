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

import static com.google.common.collect.testing.features.CollectionFeature.SERIALIZABLE;
import static com.google.common.collect.testing.features.CollectionFeature.SERIALIZABLE_INCLUDING_VIEWS;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.features.Feature;
import com.google.common.collect.testing.testers.CollectionSerializationEqualTester;
import com.google.common.collect.testing.testers.SetAddAllTester;
import com.google.common.collect.testing.testers.SetAddTester;
import com.google.common.collect.testing.testers.SetCreationTester;
import com.google.common.collect.testing.testers.SetEqualsTester;
import com.google.common.collect.testing.testers.SetHashCodeTester;
import com.google.common.collect.testing.testers.SetRemoveTester;
import com.google.common.testing.SerializableTester;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import junit.framework.TestSuite;

/**
 * Creates, based on your criteria, a JUnit test suite that exhaustively tests a Set implementation.
 *
 * @author George van den Driessche
 */
@GwtIncompatible
public class SetTestSuiteBuilder<E>
    extends AbstractCollectionTestSuiteBuilder<SetTestSuiteBuilder<E>, E> {
  public static <E> SetTestSuiteBuilder<E> using(TestSetGenerator<E> generator) {
    return new SetTestSuiteBuilder<E>().usingGenerator(generator);
  }

  @Override
  protected List<Class<? extends AbstractTester>> getTesters() {
    List<Class<? extends AbstractTester>> testers = Helpers.copyToList(super.getTesters());

    testers.add(CollectionSerializationEqualTester.class);
    testers.add(SetAddAllTester.class);
    testers.add(SetAddTester.class);
    testers.add(SetCreationTester.class);
    testers.add(SetHashCodeTester.class);
    testers.add(SetEqualsTester.class);
    testers.add(SetRemoveTester.class);
    // SetRemoveAllTester doesn't exist because, Sets not permitting
    // duplicate elements, there are no tests for Set.removeAll() that aren't
    // covered by CollectionRemoveAllTester.
    return testers;
  }

  @Override
  protected List<TestSuite> createDerivedSuites(
      FeatureSpecificTestSuiteBuilder<?, ? extends OneSizeTestContainerGenerator<Collection<E>, E>>
          parentBuilder) {
    List<TestSuite> derivedSuites = new ArrayList<>(super.createDerivedSuites(parentBuilder));

    if (parentBuilder.getFeatures().contains(SERIALIZABLE)) {
      derivedSuites.add(
          SetTestSuiteBuilder.using(
                  new ReserializedSetGenerator<E>(parentBuilder.getSubjectGenerator()))
              .named(getName() + " reserialized")
              .withFeatures(computeReserializedCollectionFeatures(parentBuilder.getFeatures()))
              .suppressing(parentBuilder.getSuppressedTests())
              .createTestSuite());
    }
    return derivedSuites;
  }

  static class ReserializedSetGenerator<E> implements TestSetGenerator<E> {
    final OneSizeTestContainerGenerator<Collection<E>, E> gen;

    private ReserializedSetGenerator(OneSizeTestContainerGenerator<Collection<E>, E> gen) {
      this.gen = gen;
    }

    @Override
    public SampleElements<E> samples() {
      return gen.samples();
    }

    @Override
    public Set<E> create(Object... elements) {
      return (Set<E>) SerializableTester.reserialize(gen.create(elements));
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
    Set<Feature<?>> derivedFeatures = new HashSet<>();
    derivedFeatures.addAll(features);
    derivedFeatures.remove(SERIALIZABLE);
    derivedFeatures.remove(SERIALIZABLE_INCLUDING_VIEWS);
    return derivedFeatures;
  }
}

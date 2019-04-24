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

import static com.google.common.collect.testing.features.CollectionFeature.KNOWN_ORDER;
import static com.google.common.collect.testing.features.CollectionFeature.SERIALIZABLE;
import static com.google.common.collect.testing.features.CollectionFeature.SERIALIZABLE_INCLUDING_VIEWS;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.Feature;
import com.google.common.collect.testing.testers.CollectionSerializationEqualTester;
import com.google.common.collect.testing.testers.ListAddAllAtIndexTester;
import com.google.common.collect.testing.testers.ListAddAllTester;
import com.google.common.collect.testing.testers.ListAddAtIndexTester;
import com.google.common.collect.testing.testers.ListAddTester;
import com.google.common.collect.testing.testers.ListCreationTester;
import com.google.common.collect.testing.testers.ListEqualsTester;
import com.google.common.collect.testing.testers.ListGetTester;
import com.google.common.collect.testing.testers.ListHashCodeTester;
import com.google.common.collect.testing.testers.ListIndexOfTester;
import com.google.common.collect.testing.testers.ListLastIndexOfTester;
import com.google.common.collect.testing.testers.ListListIteratorTester;
import com.google.common.collect.testing.testers.ListRemoveAllTester;
import com.google.common.collect.testing.testers.ListRemoveAtIndexTester;
import com.google.common.collect.testing.testers.ListRemoveTester;
import com.google.common.collect.testing.testers.ListRetainAllTester;
import com.google.common.collect.testing.testers.ListSetTester;
import com.google.common.collect.testing.testers.ListSubListTester;
import com.google.common.collect.testing.testers.ListToArrayTester;
import com.google.common.testing.SerializableTester;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import junit.framework.TestSuite;

/**
 * Creates, based on your criteria, a JUnit test suite that exhaustively tests a List
 * implementation.
 *
 * @author George van den Driessche
 */
@GwtIncompatible
public final class ListTestSuiteBuilder<E>
    extends AbstractCollectionTestSuiteBuilder<ListTestSuiteBuilder<E>, E> {
  public static <E> ListTestSuiteBuilder<E> using(TestListGenerator<E> generator) {
    return new ListTestSuiteBuilder<E>().usingGenerator(generator);
  }

  @Override
  protected List<Class<? extends AbstractTester>> getTesters() {
    List<Class<? extends AbstractTester>> testers = Helpers.copyToList(super.getTesters());

    testers.add(CollectionSerializationEqualTester.class);
    testers.add(ListAddAllAtIndexTester.class);
    testers.add(ListAddAllTester.class);
    testers.add(ListAddAtIndexTester.class);
    testers.add(ListAddTester.class);
    testers.add(ListCreationTester.class);
    testers.add(ListEqualsTester.class);
    testers.add(ListGetTester.class);
    testers.add(ListHashCodeTester.class);
    testers.add(ListIndexOfTester.class);
    testers.add(ListLastIndexOfTester.class);
    testers.add(ListListIteratorTester.class);
    testers.add(ListRemoveAllTester.class);
    testers.add(ListRemoveAtIndexTester.class);
    testers.add(ListRemoveTester.class);
    testers.add(ListRetainAllTester.class);
    testers.add(ListSetTester.class);
    testers.add(ListSubListTester.class);
    testers.add(ListToArrayTester.class);
    return testers;
  }

  /**
   * Specifies {@link CollectionFeature#KNOWN_ORDER} for all list tests, since lists have an
   * iteration ordering corresponding to the insertion order.
   */
  @Override
  public TestSuite createTestSuite() {
    withFeatures(KNOWN_ORDER);
    return super.createTestSuite();
  }

  @Override
  protected List<TestSuite> createDerivedSuites(
      FeatureSpecificTestSuiteBuilder<?, ? extends OneSizeTestContainerGenerator<Collection<E>, E>>
          parentBuilder) {
    List<TestSuite> derivedSuites = new ArrayList<>(super.createDerivedSuites(parentBuilder));

    if (parentBuilder.getFeatures().contains(SERIALIZABLE)) {
      derivedSuites.add(
          ListTestSuiteBuilder.using(
                  new ReserializedListGenerator<E>(parentBuilder.getSubjectGenerator()))
              .named(getName() + " reserialized")
              .withFeatures(computeReserializedCollectionFeatures(parentBuilder.getFeatures()))
              .suppressing(parentBuilder.getSuppressedTests())
              .createTestSuite());
    }
    return derivedSuites;
  }

  static class ReserializedListGenerator<E> implements TestListGenerator<E> {
    final OneSizeTestContainerGenerator<Collection<E>, E> gen;

    private ReserializedListGenerator(OneSizeTestContainerGenerator<Collection<E>, E> gen) {
      this.gen = gen;
    }

    @Override
    public SampleElements<E> samples() {
      return gen.samples();
    }

    @Override
    public List<E> create(Object... elements) {
      return (List<E>) SerializableTester.reserialize(gen.create(elements));
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

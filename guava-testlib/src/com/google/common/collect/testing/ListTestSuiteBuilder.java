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

import junit.framework.TestSuite;

import java.util.List;

/**
 * Creates, based on your criteria, a JUnit test suite that exhaustively tests
 * a List implementation.
 *
 * @author George van den Driessche
 */
public final class ListTestSuiteBuilder<E> extends
    AbstractCollectionTestSuiteBuilder<ListTestSuiteBuilder<E>, E> {
  public static <E> ListTestSuiteBuilder<E> using(
      TestListGenerator<E> generator) {
    return new ListTestSuiteBuilder<E>().usingGenerator(generator);
  }

  @Override protected List<Class<? extends AbstractTester>> getTesters() {
    List<Class<? extends AbstractTester>> testers
        = Helpers.copyToList(super.getTesters());

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
   * Specifies {@link CollectionFeature#KNOWN_ORDER} for all list tests, since
   * lists have an iteration ordering corresponding to the insertion order.
   */
  @Override public TestSuite createTestSuite() {
    if (!getFeatures().contains(CollectionFeature.KNOWN_ORDER)) {
      List<Feature<?>> features = Helpers.copyToList(getFeatures());
      features.add(CollectionFeature.KNOWN_ORDER);
      withFeatures(features);
    }
    return super.createTestSuite();
  }
}

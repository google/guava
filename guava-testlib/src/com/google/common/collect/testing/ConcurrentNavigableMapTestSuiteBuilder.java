/*
 * Copyright (C) 2015 The Guava Authors
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

import com.google.common.annotations.GwtIncompatible;
import java.util.List;

/**
 * Creates, based on your criteria, a JUnit test suite that exhaustively tests a
 * ConcurrentNavigableMap implementation.
 *
 * @author Louis Wasserman
 */
@GwtIncompatible
public class ConcurrentNavigableMapTestSuiteBuilder<K, V>
    extends NavigableMapTestSuiteBuilder<K, V> {

  public static <K, V> ConcurrentNavigableMapTestSuiteBuilder<K, V> using(
      TestSortedMapGenerator<K, V> generator) {
    ConcurrentNavigableMapTestSuiteBuilder<K, V> result =
        new ConcurrentNavigableMapTestSuiteBuilder<>();
    result.usingGenerator(generator);
    return result;
  }

  @Override
  protected List<Class<? extends AbstractTester>> getTesters() {
    List<Class<? extends AbstractTester>> testers = Helpers.copyToList(super.getTesters());
    testers.addAll(ConcurrentMapTestSuiteBuilder.TESTERS);
    return testers;
  }

  @Override
  NavigableMapTestSuiteBuilder<K, V> subSuiteUsing(TestSortedMapGenerator<K, V> generator) {
    return using(generator);
  }
}

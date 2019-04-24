/*
 * Copyright (C) 2015 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect.testing;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.testers.ConcurrentMapPutIfAbsentTester;
import com.google.common.collect.testing.testers.ConcurrentMapRemoveTester;
import com.google.common.collect.testing.testers.ConcurrentMapReplaceEntryTester;
import com.google.common.collect.testing.testers.ConcurrentMapReplaceTester;
import java.util.Arrays;
import java.util.List;

/**
 * Creates, based on your criteria, a JUnit test suite that exhaustively tests a ConcurrentMap
 * implementation.
 *
 * @author Louis Wasserman
 */
@GwtIncompatible
public class ConcurrentMapTestSuiteBuilder<K, V> extends MapTestSuiteBuilder<K, V> {
  public static <K, V> ConcurrentMapTestSuiteBuilder<K, V> using(TestMapGenerator<K, V> generator) {
    ConcurrentMapTestSuiteBuilder<K, V> result = new ConcurrentMapTestSuiteBuilder<>();
    result.usingGenerator(generator);
    return result;
  }

  static final List<? extends Class<? extends AbstractTester>> TESTERS =
      Arrays.asList(
          ConcurrentMapPutIfAbsentTester.class,
          ConcurrentMapRemoveTester.class,
          ConcurrentMapReplaceTester.class,
          ConcurrentMapReplaceEntryTester.class);

  @Override
  protected List<Class<? extends AbstractTester>> getTesters() {
    List<Class<? extends AbstractTester>> testers = Helpers.copyToList(super.getTesters());
    testers.addAll(TESTERS);
    return testers;
  }
}

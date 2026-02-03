/*
 * Copyright (C) 2025 The Guava Authors
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

import static com.google.common.collect.testing.Helpers.copyToList;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.testers.ConcurrentCollectionSpliteratorTester;
import java.util.List;

/**
 * Creates, based on your criteria, a JUnit test suite that exhaustively tests a concurrent Set
 * implementation.
 *
 * <p>This builder adds {@link ConcurrentCollectionSpliteratorTester} to verify that the set's
 * spliterator has the {@link java.util.Spliterator#CONCURRENT} characteristic.
 *
 * @author Guava Authors
 */
@GwtIncompatible
public class ConcurrentSetTestSuiteBuilder<E> extends SetTestSuiteBuilder<E> {

  public static <E> ConcurrentSetTestSuiteBuilder<E> using(TestSetGenerator<E> generator) {
    ConcurrentSetTestSuiteBuilder<E> result = new ConcurrentSetTestSuiteBuilder<>();
    result.usingGenerator(generator);
    return result;
  }

  @SuppressWarnings("rawtypes") // class literals
  @Override
  protected List<Class<? extends AbstractTester>> getTesters() {
    List<Class<? extends AbstractTester>> testers = copyToList(super.getTesters());
    testers.add(ConcurrentCollectionSpliteratorTester.class);
    return testers;
  }
}

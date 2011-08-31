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

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.AbstractCollectionTestSuiteBuilder;
import com.google.common.collect.testing.AbstractTester;
import com.google.common.collect.testing.Helpers;

import java.util.List;

/**
 * Creates, based on your criteria, a JUnit test suite that exhaustively tests
 * a {@code Multiset} implementation.
 *
 * @author Jared Levy
 * @author Louis Wasserman
 */
@GwtCompatible
public class MultisetTestSuiteBuilder<E> extends
    AbstractCollectionTestSuiteBuilder<MultisetTestSuiteBuilder<E>, E> {
  public static <E> MultisetTestSuiteBuilder<E> using(
      TestMultisetGenerator<E> generator) {
    return new MultisetTestSuiteBuilder<E>().usingGenerator(generator);
  }

  @Override protected List<Class<? extends AbstractTester>> getTesters() {
    List<Class<? extends AbstractTester>> testers
        = Helpers.copyToList(super.getTesters());
    testers.add(MultisetReadsTester.class);
    testers.add(MultisetSetCountConditionallyTester.class);
    testers.add(MultisetSetCountUnconditionallyTester.class);
    testers.add(MultisetWritesTester.class);
    testers.add(MultisetIteratorTester.class);
    return testers;
  }
}


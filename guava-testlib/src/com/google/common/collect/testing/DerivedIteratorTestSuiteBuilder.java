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

import junit.framework.TestSuite;

import java.util.List;

/**
 * Given a test iterable generator, builds a test suite for the
 * iterable's iterator, by delegating to a {@link IteratorTestSuiteBuilder}.
 *
 * @author George van den Driessche
 */
public class DerivedIteratorTestSuiteBuilder<E>
    extends FeatureSpecificTestSuiteBuilder<
        DerivedIteratorTestSuiteBuilder<E>,
        TestSubjectGenerator<? extends Iterable<E>>> {
  /**
   * We rely entirely on the delegate builder for test creation, so this
   * just throws UnsupportedOperationException.
   *
   * @return never.
   */
  @Override protected List<Class<? extends AbstractTester>> getTesters() {
    throw new UnsupportedOperationException();
  }

  @Override public TestSuite createTestSuite() {
    checkCanCreate();
    return new IteratorTestSuiteBuilder<E>()
        .named(getName() + " iterator")
        .usingGenerator(new DerivedTestIteratorGenerator<E>(
            getSubjectGenerator()))
        .withFeatures(getFeatures())
        .createTestSuite();
  }
}

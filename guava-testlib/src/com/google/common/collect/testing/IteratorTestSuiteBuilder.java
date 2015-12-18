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

import java.util.Collections;
import java.util.List;

/**
 * Creates, based on your criteria, a JUnit test suite that exhaustively tests
 * an Iterator implementation.
 *
 * <p>At least, it will do when it's finished.
 *
 * @author George van den Driessche
 */
public class IteratorTestSuiteBuilder<E>
    extends FeatureSpecificTestSuiteBuilder<
        IteratorTestSuiteBuilder<E>, TestIteratorGenerator<?>> {

  @Override protected List<Class<? extends AbstractTester>> getTesters() {
    return Collections.<Class<? extends AbstractTester>>singletonList(
        ExampleIteratorTester.class);
  }
}

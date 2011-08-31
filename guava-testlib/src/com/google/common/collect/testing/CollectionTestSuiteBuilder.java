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

/**
 * Concrete instantiation of {@link AbstractCollectionTestSuiteBuilder} for
 * testing collections that do not have a more specific tester like
 * {@link ListTestSuiteBuilder} or {@link SetTestSuiteBuilder}.
 *
 * @author Chris Povirk
 */
public class CollectionTestSuiteBuilder<E>
    extends AbstractCollectionTestSuiteBuilder<
        CollectionTestSuiteBuilder<E>, E> {
  public static <E> CollectionTestSuiteBuilder<E> using(
      TestCollectionGenerator<E> generator) {
    return new CollectionTestSuiteBuilder<E>().usingGenerator(generator);
  }
}

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

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.features.CollectionSize;
import java.util.Collection;

/**
 * The subject-generator interface accepted by Collection testers, for testing a Collection at one
 * particular {@link CollectionSize}.
 *
 * <p>This interface should not be implemented outside this package; {@link
 * PerCollectionSizeTestSuiteBuilder} constructs instances of it from a more general {@link
 * TestCollectionGenerator}.
 *
 * @author George van den Driessche
 */
@GwtCompatible
public interface OneSizeTestContainerGenerator<T, E>
    extends TestSubjectGenerator<T>, TestContainerGenerator<T, E> {
  TestContainerGenerator<T, E> getInnerGenerator();

  Collection<E> getSampleElements(int howMany);

  CollectionSize getCollectionSize();
}

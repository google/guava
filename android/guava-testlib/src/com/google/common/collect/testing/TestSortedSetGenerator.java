/*
 * Copyright (C) 2013 The Guava Authors
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
import java.util.SortedSet;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Creates sorted sets, containing sample elements, to be tested.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
public interface TestSortedSetGenerator<E extends @Nullable Object> extends TestSetGenerator<E> {
  @Override
  SortedSet<E> create(Object... elements);

  /**
   * Returns an element less than the {@link #samples()} and less than {@link
   * #belowSamplesGreater()}.
   */
  E belowSamplesLesser();

  /**
   * Returns an element less than the {@link #samples()} but greater than {@link
   * #belowSamplesLesser()}.
   */
  E belowSamplesGreater();

  /**
   * Returns an element greater than the {@link #samples()} but less than {@link
   * #aboveSamplesGreater()}.
   */
  E aboveSamplesLesser();

  /**
   * Returns an element greater than the {@link #samples()} and greater than {@link
   * #aboveSamplesLesser()}.
   */
  E aboveSamplesGreater();
}

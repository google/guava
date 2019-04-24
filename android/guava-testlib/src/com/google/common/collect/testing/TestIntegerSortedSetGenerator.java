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
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;

/**
 * Create integer sets for testing collections that are sorted by natural ordering.
 *
 * @author Chris Povirk
 * @author Jared Levy
 */
@GwtCompatible
public abstract class TestIntegerSortedSetGenerator extends TestIntegerSetGenerator {
  @Override
  protected abstract SortedSet<Integer> create(Integer[] elements);

  /** Sorts the elements by their natural ordering. */
  @Override
  public List<Integer> order(List<Integer> insertionOrder) {
    Collections.sort(insertionOrder);
    return insertionOrder;
  }
}

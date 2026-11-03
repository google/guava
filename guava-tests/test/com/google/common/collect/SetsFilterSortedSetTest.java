/*
 * Copyright (C) 2012 The Guava Authors
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

package com.google.common.collect;

import static com.google.common.collect.Sets.newTreeSet;

import com.google.common.base.Predicate;
import com.google.common.collect.FilteredCollectionsTestUtil.AbstractFilteredSortedSetTest;
import com.google.common.collect.ForwardingSortedSet.SimpleForwardingSortedSet;
import java.util.SortedSet;
import org.jspecify.annotations.NullUnmarked;

@NullUnmarked
public final class SetsFilterSortedSetTest
    extends AbstractFilteredSortedSetTest<SortedSet<Integer>> {
  @Override
  SortedSet<Integer> createUnfiltered(Iterable<Integer> contents) {
    // we have to make the result not Navigable
    return new SimpleForwardingSortedSet<Integer>(newTreeSet(contents)) {};
  }

  @Override
  SortedSet<Integer> filter(SortedSet<Integer> elements, Predicate<? super Integer> predicate) {
    return Sets.filter(elements, predicate);
  }
}

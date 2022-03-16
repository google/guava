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

import com.google.common.base.Predicate;
import com.google.common.collect.FilteredCollectionsTestUtil.AbstractFilteredSortedSetTest;
import java.util.SortedSet;
import java.util.TreeSet;

public final class SetsFilterSortedSetTest
    extends AbstractFilteredSortedSetTest<SortedSet<Integer>> {
  @Override
  SortedSet<Integer> createUnfiltered(Iterable<Integer> contents) {
    final TreeSet<Integer> result = Sets.newTreeSet(contents);
    // we have to make the result not Navigable
    return new ForwardingSortedSet<Integer>() {
      @Override
      protected SortedSet<Integer> delegate() {
        return result;
      }
    };
  }

  @Override
  SortedSet<Integer> filter(SortedSet<Integer> elements, Predicate<? super Integer> predicate) {
    return Sets.filter(elements, predicate);
  }
}

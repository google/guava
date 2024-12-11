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

import static com.google.common.collect.Sets.newHashSet;

import com.google.common.base.Predicate;
import com.google.common.collect.FilteredCollectionsTestUtil.AbstractFilteredSetTest;
import java.util.Set;

public final class SetsFilterHashSetTest extends AbstractFilteredSetTest<Set<Integer>> {
  @Override
  Set<Integer> createUnfiltered(Iterable<Integer> contents) {
    return newHashSet(contents);
  }

  @Override
  Set<Integer> filter(Set<Integer> elements, Predicate<? super Integer> predicate) {
    return Sets.filter(elements, predicate);
  }
}

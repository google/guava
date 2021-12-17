/*
 * Copyright (C) 2009 The Guava Authors
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

import java.util.Comparator;
import java.util.SortedSet;

/**
 * GWT emulation of {@link RegularImmutableSortedSet}.
 *
 * @author Hayward Chan
 */
final class RegularImmutableSortedSet<E> extends ImmutableSortedSet<E> {

  /** true if this set is a subset of another immutable sorted set. */
  final boolean isSubset;

  private Comparator<E> unusedComparatorForSerialization;
  private E unusedElementForSerialization;

  RegularImmutableSortedSet(SortedSet<E> delegate, boolean isSubset) {
    super(delegate);
    this.isSubset = isSubset;
  }

  @Override
  ImmutableList<E> createAsList() {
    return new ImmutableSortedAsList<E>(this, ImmutableList.<E>asImmutableList(toArray()));
  }
}

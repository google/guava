/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import java.util.Comparator;
import java.util.SortedSet;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * GWT emulation of {@code SortedMultiset}, with {@code elementSet} reduced to returning a {@code
 * SortedSet} for GWT compatibility.
 *
 * @author Louis Wasserman
 * @since 11.0
 */
@ElementTypesAreNonnullByDefault
public interface SortedMultiset<E extends @Nullable Object> extends Multiset<E>, SortedIterable<E> {
  Comparator<? super E> comparator();

  @CheckForNull
  Entry<E> firstEntry();

  @CheckForNull
  Entry<E> lastEntry();

  @CheckForNull
  Entry<E> pollFirstEntry();

  @CheckForNull
  Entry<E> pollLastEntry();

  /**
   * Returns a {@link SortedSet} view of the distinct elements in this multiset. (Outside GWT, this
   * returns a {@code NavigableSet}.)
   */
  @Override
  SortedSet<E> elementSet();

  SortedMultiset<E> descendingMultiset();

  SortedMultiset<E> headMultiset(E upperBound, BoundType boundType);

  SortedMultiset<E> subMultiset(
      E lowerBound, BoundType lowerBoundType, E upperBound, BoundType upperBoundType);

  SortedMultiset<E> tailMultiset(E lowerBound, BoundType boundType);
}

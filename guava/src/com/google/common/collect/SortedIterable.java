/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;

import java.util.Comparator;
import java.util.Iterator;

/**
 * An {@code Iterable} whose elements are sorted relative to a {@code Comparator}, typically
 * provided at creation time.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
interface SortedIterable<T> extends Iterable<T> {
  /**
   * Returns the {@code Comparator} by which the elements of this iterable are ordered, or {@code
   * Ordering.natural()} if the elements are ordered by their natural ordering.
   */
  Comparator<? super T> comparator();

  /**
   * Returns an iterator over elements of type {@code T}. The elements are returned in
   * nondecreasing order according to the associated {@link #comparator}.
   */
  @Override
  Iterator<T> iterator();
}

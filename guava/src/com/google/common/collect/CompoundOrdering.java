/*
 * Copyright (C) 2007 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;

import java.io.Serializable;
import java.util.Comparator;

/** An ordering that tries several comparators in order. */
@GwtCompatible(serializable = true)
final class CompoundOrdering<T> extends Ordering<T> implements Serializable {
  final ImmutableList<Comparator<? super T>> comparators;

  CompoundOrdering(Comparator<? super T> primary,
      Comparator<? super T> secondary) {
    this.comparators
        = ImmutableList.<Comparator<? super T>>of(primary, secondary);
  }

  CompoundOrdering(Iterable<? extends Comparator<? super T>> comparators) {
    this.comparators = ImmutableList.copyOf(comparators);
  }

  @Override public int compare(T left, T right) {
    // Avoid using the Iterator to avoid generating garbage (issue 979).
    int size = comparators.size();
    for (int i = 0; i < size; i++) {
      int result = comparators.get(i).compare(left, right);
      if (result != 0) {
        return result;
      }
    }
    return 0;
  }

  @Override public boolean equals(Object object) {
    if (object == this) {
      return true;
    }
    if (object instanceof CompoundOrdering) {
      CompoundOrdering<?> that = (CompoundOrdering<?>) object;
      return this.comparators.equals(that.comparators);
    }
    return false;
  }

  @Override public int hashCode() {
    return comparators.hashCode();
  }

  @Override public String toString() {
    return "Ordering.compound(" + comparators + ")";
  }

  private static final long serialVersionUID = 0;
}

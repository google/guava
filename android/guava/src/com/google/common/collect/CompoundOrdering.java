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
import java.util.Arrays;
import java.util.Comparator;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/** An ordering that tries several comparators in order. */
@GwtCompatible(serializable = true)
@ElementTypesAreNonnullByDefault
final class CompoundOrdering<T extends @Nullable Object> extends Ordering<T>
    implements Serializable {
  final Comparator<? super T>[] comparators;

  CompoundOrdering(Comparator<? super T> primary, Comparator<? super T> secondary) {
    this.comparators = (Comparator<? super T>[]) new Comparator[] {primary, secondary};
  }

  CompoundOrdering(Iterable<? extends Comparator<? super T>> comparators) {
    this.comparators = Iterables.toArray(comparators, new Comparator[0]);
  }

  @Override
  public int compare(@ParametricNullness T left, @ParametricNullness T right) {
    for (int i = 0; i < comparators.length; i++) {
      int result = comparators[i].compare(left, right);
      if (result != 0) {
        return result;
      }
    }
    return 0;
  }

  @Override
  public boolean equals(@CheckForNull Object object) {
    if (object == this) {
      return true;
    }
    if (object instanceof CompoundOrdering) {
      CompoundOrdering<?> that = (CompoundOrdering<?>) object;
      return Arrays.equals(this.comparators, that.comparators);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(comparators);
  }

  @Override
  public String toString() {
    return "Ordering.compound(" + Arrays.toString(comparators) + ")";
  }

  private static final long serialVersionUID = 0;
}

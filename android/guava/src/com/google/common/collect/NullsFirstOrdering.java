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
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/** An ordering that treats {@code null} as less than all other values. */
@GwtCompatible(serializable = true)
@ElementTypesAreNonnullByDefault
final class NullsFirstOrdering<T extends @Nullable Object> extends Ordering<@Nullable T>
    implements Serializable {
  final Ordering<? super T> ordering;

  NullsFirstOrdering(Ordering<? super T> ordering) {
    this.ordering = ordering;
  }

  @Override
  public int compare(@CheckForNull T left, @CheckForNull T right) {
    if (left == right) {
      return 0;
    }
    if (left == null) {
      return RIGHT_IS_GREATER;
    }
    if (right == null) {
      return LEFT_IS_GREATER;
    }
    return ordering.compare(left, right);
  }

  @Override
  @SuppressWarnings("nullness") // should be safe, but not sure if we can avoid the warning
  public <S extends @Nullable T> Ordering<S> reverse() {
    // ordering.reverse() might be optimized, so let it do its thing
    return ordering.<T>reverse().<@NonNull S>nullsLast();
  }

  @SuppressWarnings("unchecked") // still need the right way to explain this
  @Override
  public <S extends @Nullable T> Ordering<@Nullable S> nullsFirst() {
    return (Ordering<@Nullable S>) this;
  }

  @Override
  public <S extends @Nullable T> Ordering<@Nullable S> nullsLast() {
    return ordering.<@NonNull S>nullsLast();
  }

  @Override
  public boolean equals(@CheckForNull Object object) {
    if (object == this) {
      return true;
    }
    if (object instanceof NullsFirstOrdering) {
      NullsFirstOrdering<?> that = (NullsFirstOrdering<?>) object;
      return this.ordering.equals(that.ordering);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return ordering.hashCode() ^ 957692532; // meaningless
  }

  @Override
  public String toString() {
    return ordering + ".nullsFirst()";
  }

  private static final long serialVersionUID = 0;
}

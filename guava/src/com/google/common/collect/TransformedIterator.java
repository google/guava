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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;
import java.util.Iterator;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An iterator that transforms a backing iterator; for internal use. This avoids the object overhead
 * of constructing a {@link com.google.common.base.Function Function} for internal methods.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
abstract class TransformedIterator<F extends @Nullable Object, T extends @Nullable Object>
    implements Iterator<T> {
  final Iterator<? extends F> backingIterator;

  TransformedIterator(Iterator<? extends F> backingIterator) {
    this.backingIterator = checkNotNull(backingIterator);
  }

  @ParametricNullness
  abstract T transform(@ParametricNullness F from);

  @Override
  public final boolean hasNext() {
    return backingIterator.hasNext();
  }

  @Override
  @ParametricNullness
  public final T next() {
    return transform(backingIterator.next());
  }

  @Override
  public final void remove() {
    backingIterator.remove();
  }
}

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

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;

import java.util.ListIterator;

/**
 * An iterator that transforms a backing list iterator; for internal use. This
 * avoids the object overhead of constructing a {@link Function} for internal
 * methods.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
abstract class TransformedListIterator<F, T> extends TransformedIterator<F, T>
    implements ListIterator<T> {
  TransformedListIterator(ListIterator<? extends F> backingIterator) {
    super(backingIterator);
  }

  private ListIterator<? extends F> backingIterator() {
    return Iterators.cast(backingIterator);
  }

  @Override
  public final boolean hasPrevious() {
    return backingIterator().hasPrevious();
  }

  @Override
  public final T previous() {
    return transform(backingIterator().previous());
  }

  @Override
  public final int nextIndex() {
    return backingIterator().nextIndex();
  }

  @Override
  public final int previousIndex() {
    return backingIterator().previousIndex();
  }

  @Override
  public void set(T element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(T element) {
    throw new UnsupportedOperationException();
  }
}

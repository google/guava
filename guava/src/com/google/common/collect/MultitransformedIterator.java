/*
 * Copyright (C) 2016 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.CollectPreconditions.checkRemove;

import com.google.common.annotations.GwtCompatible;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Similar to {@link TransformedIterator}, this iterator transforms a backing iterator.
 * However, rather than enforcing a one-to-one mapping, each element in the backing iterator
 * can be transformed into an arbitrary number of elements (i.e. a one-to-many mapping).
 *
 * @author James Sexton
 */
@GwtCompatible
abstract class MultitransformedIterator<F, T> implements Iterator<T> {
  final Iterator<? extends F> backingIterator;

  private Iterator<? extends T> current = Iterators.emptyIterator();
  private Iterator<? extends T> removeFrom;

  MultitransformedIterator(Iterator<? extends F> backingIterator) {
    this.backingIterator = checkNotNull(backingIterator);
  }

  abstract Iterator<? extends T> transform(F from);

  @Override
  public boolean hasNext() {
    checkNotNull(current); // eager for GWT
    if (current.hasNext()) {
      return true;
    }
    while (backingIterator.hasNext()) {
      // checkNotNull the assignment, so that current is null even if the exception is caught
      checkNotNull(current = transform(backingIterator.next()));
      if (current.hasNext()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public T next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    removeFrom = current;
    return current.next();
  }

  @Override
  public void remove() {
    checkRemove(removeFrom != null);
    removeFrom.remove();
    removeFrom = null;
  }
}

/*
 * Copyright (C) 2008 The Guava Authors
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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.DoNotMock;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator that supports a one-element lookahead while iterating.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/CollectionHelpersExplained#peekingiterator"> {@code
 * PeekingIterator}</a>.
 *
 * @author Mick Killianey
 * @since 2.0
 */
@DoNotMock("Use Iterators.peekingIterator")
@GwtCompatible
public interface PeekingIterator<E> extends Iterator<E> {
  /**
   * Returns the next element in the iteration, without advancing the iteration.
   *
   * <p>Calls to {@code peek()} should not change the state of the iteration, except that it
   * <i>may</i> prevent removal of the most recent element via {@link #remove()}.
   *
   * @throws NoSuchElementException if the iteration has no more elements according to {@link
   *     #hasNext()}
   */
  E peek();

  /**
   * {@inheritDoc}
   *
   * <p>The objects returned by consecutive calls to {@link #peek()} then {@link #next()} are
   * guaranteed to be equal to each other.
   */
  @CanIgnoreReturnValue
  @Override
  E next();

  /**
   * {@inheritDoc}
   *
   * <p>Implementations may or may not support removal when a call to {@link #peek()} has occurred
   * since the most recent call to {@link #next()}.
   *
   * @throws IllegalStateException if there has been a call to {@link #peek()} since the most recent
   *     call to {@link #next()} and this implementation does not support this sequence of calls
   *     (optional)
   */
  @Override
  void remove();
}

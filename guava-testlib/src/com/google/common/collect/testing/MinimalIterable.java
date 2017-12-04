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

package com.google.common.collect.testing;

import com.google.common.annotations.GwtCompatible;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * An implementation of {@code Iterable} which throws an exception on all invocations of the {@link
 * #iterator()} method after the first, and whose iterator is always unmodifiable.
 *
 * <p>The {@code Iterable} specification does not make it absolutely clear what should happen on a
 * second invocation, so implementors have made various choices, including:
 *
 * <ul>
 *   <li>returning the same iterator again
 *   <li>throwing an exception of some kind
 *   <li>or the usual, <i>robust</i> behavior, which all known {@link Collection} implementations
 *       have, of returning a new, independent iterator
 * </ul>
 *
 * <p>Because of this situation, any public method accepting an iterable should invoke the {@code
 * iterator} method only once, and should be tested using this class. Exceptions to this rule should
 * be clearly documented.
 *
 * <p>Note that although your APIs should be liberal in what they accept, your methods which
 * <i>return</i> iterables should make every attempt to return ones of the robust variety.
 *
 * <p>This testing utility is not thread-safe.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible
public final class MinimalIterable<E> implements Iterable<E> {
  /** Returns an iterable whose iterator returns the given elements in order. */
  public static <E> MinimalIterable<E> of(E... elements) {
    // Make sure to get an unmodifiable iterator
    return new MinimalIterable<E>(Arrays.asList(elements).iterator());
  }

  /**
   * Returns an iterable whose iterator returns the given elements in order. The elements are copied
   * out of the source collection at the time this method is called.
   */
  @SuppressWarnings("unchecked") // Es come in, Es go out
  public static <E> MinimalIterable<E> from(final Collection<E> elements) {
    return (MinimalIterable) of(elements.toArray());
  }

  private Iterator<E> iterator;

  private MinimalIterable(Iterator<E> iterator) {
    this.iterator = iterator;
  }

  @Override
  public Iterator<E> iterator() {
    if (iterator == null) {
      // TODO: throw something else? Do we worry that people's code and tests
      // might be relying on this particular type of exception?
      throw new IllegalStateException();
    }
    try {
      return iterator;
    } finally {
      iterator = null;
    }
  }
}

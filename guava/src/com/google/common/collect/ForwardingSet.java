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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;

import java.util.Collection;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * A set which forwards all its method calls to another set. Subclasses should
 * override one or more methods to modify the behavior of the backing set as
 * desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * <p><b>Warning:</b> The methods of {@code ForwardingSet} forward
 * <b>indiscriminately</b> to the methods of the delegate. For example,
 * overriding {@link #add} alone <b>will not</b> change the behavior of {@link
 * #addAll}, which can lead to unexpected behavior. In this case, you should
 * override {@code addAll} as well, either providing your own implementation, or
 * delegating to the provided {@code standardAddAll} method.
 *
 * <p>The {@code standard} methods are not guaranteed to be thread-safe, even
 * when all of the methods that they depend on are thread-safe.
 *
 * @author Kevin Bourrillion
 * @author Louis Wasserman
 * @since 2.0
 */
@GwtCompatible
public abstract class ForwardingSet<E> extends ForwardingCollection<E> implements Set<E> {
  // TODO(lowasser): identify places where thread safety is actually lost

  /** Constructor for use by subclasses. */
  protected ForwardingSet() {}

  @Override
  protected abstract Set<E> delegate();

  @Override
  public boolean equals(@Nullable Object object) {
    return object == this || delegate().equals(object);
  }

  @Override
  public int hashCode() {
    return delegate().hashCode();
  }

  /**
   * A sensible definition of {@link #removeAll} in terms of {@link #iterator}
   * and {@link #remove}.  If you override {@code iterator} or {@code remove},
   * you may wish to override {@link #removeAll} to forward to this
   * implementation.
   *
   * @since 7.0 (this version overrides the {@code ForwardingCollection} version as of 12.0)
   */
  @Override
  protected boolean standardRemoveAll(Collection<?> collection) {
    return Sets.removeAllImpl(this, checkNotNull(collection)); // for GWT
  }

  /**
   * A sensible definition of {@link #equals} in terms of {@link #size} and
   * {@link #containsAll}. If you override either of those methods, you may wish
   * to override {@link #equals} to forward to this implementation.
   *
   * @since 7.0
   */
  protected boolean standardEquals(@Nullable Object object) {
    return Sets.equalsImpl(this, object);
  }

  /**
   * A sensible definition of {@link #hashCode} in terms of {@link #iterator}.
   * If you override {@link #iterator}, you may wish to override {@link #equals}
   * to forward to this implementation.
   *
   * @since 7.0
   */
  protected int standardHashCode() {
    return Sets.hashCodeImpl(this);
  }
}

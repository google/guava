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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.concurrent.ConcurrentMap;
import org.jspecify.annotations.Nullable;

/**
 * A concurrent map which forwards all its method calls to another concurrent map. Subclasses should
 * override one or more methods to modify the behavior of the backing map as desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * <p><b>{@code default} method warning:</b> This class forwards calls to <i>only some</i> {@code
 * default} methods. Specifically, it forwards calls only for methods that existed <a
 * href="https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ConcurrentMap.html">before
 * {@code default} methods were introduced</a>. For newer methods, like {@code forEach}, it inherits
 * their default implementations. When those implementations invoke methods, they invoke methods on
 * the {@code ForwardingConcurrentMap}.
 *
 * @author Charles Fry
 * @since 2.0
 */
@GwtCompatible
public abstract class ForwardingConcurrentMap<K, V> extends ForwardingMap<K, V>
    implements ConcurrentMap<K, V> {

  /** Constructor for use by subclasses. */
  protected ForwardingConcurrentMap() {}

  @Override
  protected abstract ConcurrentMap<K, V> delegate();

  @CanIgnoreReturnValue
  @Override
  public @Nullable V putIfAbsent(K key, V value) {
    return delegate().putIfAbsent(key, value);
  }

  @CanIgnoreReturnValue
  @Override
  public boolean remove(@Nullable Object key, @Nullable Object value) {
    return delegate().remove(key, value);
  }

  @CanIgnoreReturnValue
  @Override
  public @Nullable V replace(K key, V value) {
    return delegate().replace(key, value);
  }

  @CanIgnoreReturnValue
  @Override
  @SuppressWarnings("nullness") // https://github.com/jspecify/jdk/issues/118
  public boolean replace(K key, V oldValue, V newValue) {
    return delegate().replace(key, oldValue, newValue);
  }

  /**
   * A simplified version of {@link ForwardingConcurrentMap} where subclasses can pass in an already
   * constructed {@link ConcurrentMap} as the delegate.
   *
   * <p>TODO(b/3417232): Consider making this public, but take into consideration whether it would
   * then appear as a user-visible supertype for any classes.
   */
  abstract static class SimpleForwardingConcurrentMap<K, V> extends ForwardingConcurrentMap<K, V> {
    private final ConcurrentMap<K, V> delegate;

    /** Constructor for use by subclasses. */
    SimpleForwardingConcurrentMap(ConcurrentMap<K, V> delegate) {
      this.delegate = checkNotNull(delegate);
    }

    @Override
    protected final ConcurrentMap<K, V> delegate() {
      return delegate;
    }
  }
}

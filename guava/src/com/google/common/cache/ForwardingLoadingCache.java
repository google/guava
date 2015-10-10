/*
 * Copyright (C) 2011 The Guava Authors
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

package com.google.common.cache;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.util.concurrent.ExecutionException;

/**
 * A cache which forwards all its method calls to another cache. Subclasses should override one or
 * more methods to modify the behavior of the backing cache as desired per the
 * <a href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * <p>Note that {@link #get}, {@link #getUnchecked}, and {@link #apply} all expose the same
 * underlying functionality, so should probably be overridden as a group.
 *
 * @author Charles Fry
 * @since 11.0
 */
public abstract class ForwardingLoadingCache<K, V>
    extends ForwardingCache<K, V> implements LoadingCache<K, V> {

  /** Constructor for use by subclasses. */
  protected ForwardingLoadingCache() {}

  @Override
  protected abstract LoadingCache<K, V> delegate();

  @Override
  public V get(K key) throws ExecutionException {
    return delegate().get(key);
  }

  @Override
  public V getUnchecked(K key) {
    return delegate().getUnchecked(key);
  }

  @Override
  public ImmutableMap<K, V> getAll(Iterable<? extends K> keys) throws ExecutionException {
    return delegate().getAll(keys);
  }

  @Override
  public V apply(K key) {
    return delegate().apply(key);
  }

  @Override
  public void refresh(K key) {
    delegate().refresh(key);
  }

  /**
   * A simplified version of {@link ForwardingLoadingCache} where subclasses can pass in an already
   * constructed {@link LoadingCache} as the delegate.
   *
   * @since 10.0
   */
  public abstract static class SimpleForwardingLoadingCache<K, V>
      extends ForwardingLoadingCache<K, V> {
    private final LoadingCache<K, V> delegate;

    protected SimpleForwardingLoadingCache(LoadingCache<K, V> delegate) {
      this.delegate = Preconditions.checkNotNull(delegate);
    }

    @Override
    protected final LoadingCache<K, V> delegate() {
      return delegate;
    }
  }
}

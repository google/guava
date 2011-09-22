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

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.collect.ForwardingObject;

import javax.annotation.Nullable;

import java.util.concurrent.ConcurrentMap;
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
 * @since 10.0
 */
@Beta
public abstract class ForwardingCache<K, V> extends ForwardingObject implements Cache<K, V> {

  /** Constructor for use by subclasses. */
  protected ForwardingCache() {}

  @Override
  protected abstract Cache<K, V> delegate();

  @Override
  @Nullable
  public V get(@Nullable K key) throws ExecutionException {
    return delegate().get(key);
  }

  @Override
  @Nullable
  public V getUnchecked(@Nullable K key) {
    return delegate().getUnchecked(key);
  }

  @Deprecated
  @Override
  @Nullable
  public V apply(@Nullable K key) {
    return delegate().apply(key);
  }

  @Override
  public void invalidate(@Nullable Object key) {
    delegate().invalidate(key);
  }

  @Override
  public void invalidateAll() {
    delegate().invalidateAll();
  }

  @Override
  public long size() {
    return delegate().size();
  }

  @Override
  public CacheStats stats() {
    return delegate().stats();
  }

  @Override
  public ConcurrentMap<K, V> asMap() {
    return delegate().asMap();
  }

  @Override
  public void cleanUp() {
    delegate().cleanUp();
  }

  /**
   * A simplified version of {@link ForwardingCache} where subclasses can pass in an already
   * constructed {@link Cache} as the delegete.
   *
   * @since 10.0
   */
  @Beta
  public abstract static class SimpleForwardingCache<K, V> extends ForwardingCache<K, V> {
    private final Cache<K, V> delegate;

    protected SimpleForwardingCache(Cache<K, V> delegate) {
      this.delegate = Preconditions.checkNotNull(delegate);
    }

    @Override
    protected final Cache<K, V> delegate() {
      return delegate;
    }
  }
}

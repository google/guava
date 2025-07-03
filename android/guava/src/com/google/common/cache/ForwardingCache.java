/*
 * Copyright (C) 2011 The Guava Authors
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

package com.google.common.cache;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Preconditions;
import com.google.common.collect.ForwardingObject;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import org.jspecify.annotations.Nullable;

/**
 * A cache which forwards all its method calls to another cache. Subclasses should override one or
 * more methods to modify the behavior of the backing cache as desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * @author Charles Fry
 * @since 10.0
 */
@GwtIncompatible
public abstract class ForwardingCache<K, V> extends ForwardingObject implements Cache<K, V> {

  /** Constructor for use by subclasses. */
  protected ForwardingCache() {}

  @Override
  protected abstract Cache<K, V> delegate();

  /**
   * @since 11.0
   */
  @Override
  public @Nullable V getIfPresent(Object key) {
    return delegate().getIfPresent(key);
  }

  /**
   * @since 11.0
   */
  @Override
  public V get(K key, Callable<? extends V> valueLoader) throws ExecutionException {
    return delegate().get(key, valueLoader);
  }

  /**
   * @since 11.0
   */
  @Override
  /*
   * <? extends Object> is mostly the same as <?> to plain Java. But to nullness checkers, they
   * differ: <? extends Object> means "non-null types," while <?> means "all types."
   */
  public ImmutableMap<K, V> getAllPresent(Iterable<? extends Object> keys) {
    return delegate().getAllPresent(keys);
  }

  /**
   * @since 11.0
   */
  @Override
  public void put(K key, V value) {
    delegate().put(key, value);
  }

  /**
   * @since 12.0
   */
  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    delegate().putAll(m);
  }

  @Override
  public void invalidate(Object key) {
    delegate().invalidate(key);
  }

  /**
   * @since 11.0
   */
  @Override
  // For discussion of <? extends Object>, see getAllPresent.
  public void invalidateAll(Iterable<? extends Object> keys) {
    delegate().invalidateAll(keys);
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
   * constructed {@link Cache} as the delegate.
   *
   * @since 10.0
   */
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

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

import com.google.common.base.Supplier;
import com.google.common.cache.CustomConcurrentHashMap.ReferenceEntry;
import com.google.common.cache.CustomConcurrentHashMap.Segment;
import com.google.common.collect.ForwardingConcurrentMap;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

/**
 * Exposes a {@link CustomConcurrentHashMap} as a {@code Cache}.
 *
 * @author Charles Fry
 */
class ComputingCache<K, V> extends AbstractCache<K, V> {
  final CustomConcurrentHashMap<K, V> map;

  ComputingCache(CacheBuilder<? super K, ? super V> builder,
      Supplier<? extends StatsCounter> statsCounterSupplier,
      CacheLoader<? super K, V> loader) {
    this.map = new CustomConcurrentHashMap<K, V>(builder, statsCounterSupplier, loader);
  }

  // Cache methods

  @Override
  public V get(K key) throws ExecutionException {
    V value = map.getOrCompute(key);
    if (value == null) {
      throw new NullPointerException(map.loader + " returned null for key " + key + ".");
    }
    return value;
  }

  @Override
  public void invalidate(@Nullable Object key) {
    map.remove(key);
  }

  @Override
  public void invalidateAll() {
    map.clear();
  }

  @Override
  public int size() {
    return map.size();
  }

  ConcurrentMap<K, V> asMap;

  @Override
  public ConcurrentMap<K, V> asMap() {
    ConcurrentMap<K, V> am = asMap;
    return (am != null) ? am : (asMap = new CacheAsMap<K, V>(map));
  }

  @Override
  public CacheStats stats() {
    SimpleStatsCounter aggregator = new SimpleStatsCounter();
    for (Segment<K, V> segment : map.segments) {
      aggregator.incrementBy(segment.statsCounter);
    }
    return aggregator.snapshot();
  }

  // TODO(user): activeEntries

  static final class CacheAsMap<K, V> extends ForwardingConcurrentMap<K, V> {
    private final CustomConcurrentHashMap<K, V> delegate;

    CacheAsMap(CustomConcurrentHashMap<K, V> delegate) {
      this.delegate = delegate;
    }

    @Override
    protected ConcurrentMap<K, V> delegate() {
      return delegate;
    }

    @Override
    public V get(@Nullable Object key) {
      // does not impact recency ordering
      ReferenceEntry<K, V> e = delegate.getEntry(key);
      return (e == null) ? null : e.getValueReference().get();
    }

    @Override
    public V put(K key, V value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
      throw new UnsupportedOperationException();
    }

    @Override
    public V putIfAbsent(K key, V value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public V replace(K key, V value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
      throw new UnsupportedOperationException();
    }
  }
}

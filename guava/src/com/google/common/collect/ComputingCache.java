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

package com.google.common.collect;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import com.google.common.collect.AbstractCache.StatsCounter;
import com.google.common.collect.ComputingConcurrentHashMap.AsynchronousExecutionException;
import com.google.common.collect.CustomConcurrentHashMap.Segment;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

/**
 * Exposes a {@link ComputingConcurrentHashMap} as a {@code Cache}.
 *
 * @author Charles Fry
 */
class ComputingCache<K, V> extends AbstractCache<K, V> {
  @VisibleForTesting
  final ComputingConcurrentHashMap<K, V> map;

  ComputingCache(MapMaker builder,
      Supplier<? extends StatsCounter> statsCounterSupplier,
      CacheLoader<? super K, V> loader) {
    this.map = new ComputingConcurrentHashMap<K, V>(builder, statsCounterSupplier, loader);
  }

  // Cache methods

  @Override
  public V get(K key) {
    try {
      return map.compute(key);
    } catch (AsynchronousExecutionException e) {
      // Wasting the existing exception instance may be costly, but we should not need to optimize
      // for the exception case, and this makes the code a lot cleaner. The other obvious
      // alternative would be to introduce computeChecked which mimicked the compute codepath
      // but threw different exceptions.
      throw new AsynchronousComputationException(e.getCause());
    } catch (ExecutionException e) {
      throw new ComputationException(e.getCause());
    }
  }

  @Override
  public V getChecked(K key) throws ExecutionException {
    return map.compute(key);
  }

  @Override
  public void invalidate(Object key) {
    map.remove(key);
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
    private final ConcurrentMap<K, V> delegate;

    CacheAsMap(ConcurrentMap<K, V> delegate) {
      this.delegate = delegate;
    }

    protected ConcurrentMap<K, V> delegate() {
      return delegate;
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

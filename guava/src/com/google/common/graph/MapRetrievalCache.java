/*
 * Copyright (C) 2016 The Guava Authors
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

package com.google.common.graph;

import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link MapIteratorCache} that adds additional caching. In addition to the caching provided by
 * {@link MapIteratorCache}, this structure caches values for the two most recently retrieved keys.
 *
 * @author James Sexton
 */
class MapRetrievalCache<K, V> extends MapIteratorCache<K, V> {
  // See the note about volatile in the superclass.
  private transient volatile @Nullable CacheEntry<K, V> cacheEntry1;
  private transient volatile @Nullable CacheEntry<K, V> cacheEntry2;

  MapRetrievalCache(Map<K, V> backingMap) {
    super(backingMap);
  }

  @SuppressWarnings("unchecked") // Safe because we only cast if key is found in map.
  @Override
  public V get(@Nullable Object key) {
    V value = getIfCached(key);
    if (value != null) {
      return value;
    }

    value = getWithoutCaching(key);
    if (value != null) {
      addToCache((K) key, value);
    }
    return value;
  }

  // Internal methods ('protected' is still package-visible, but treat as only subclass-visible)

  @Override
  protected V getIfCached(@Nullable Object key) {
    V value = super.getIfCached(key);
    if (value != null) {
      return value;
    }

    // Store a local reference to the cache entry. If the backing map is immutable, this,
    // in combination with immutable cache entries, will ensure a thread-safe cache.
    CacheEntry<K, V> entry;

    // Check cache. We use == on purpose because it's cheaper and a cache miss is ok.
    entry = cacheEntry1;
    if (entry != null && entry.key == key) {
      return entry.value;
    }
    entry = cacheEntry2;
    if (entry != null && entry.key == key) {
      // Promote second cache entry to first so the access pattern
      // [K1, K2, K1, K3, K1, K4...] still hits the cache half the time.
      addToCache(entry);
      return entry.value;
    }
    return null;
  }

  @Override
  protected void clearCache() {
    super.clearCache();
    cacheEntry1 = null;
    cacheEntry2 = null;
  }

  private void addToCache(K key, V value) {
    addToCache(new CacheEntry<K, V>(key, value));
  }

  private void addToCache(CacheEntry<K, V> entry) {
    // Slide new entry into first cache position. Drop previous entry in second cache position.
    cacheEntry2 = cacheEntry1;
    cacheEntry1 = entry;
  }

  private static final class CacheEntry<K, V> {
    final K key;
    final V value;

    CacheEntry(K key, V value) {
      this.key = key;
      this.value = value;
    }
  }
}

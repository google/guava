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

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Objects;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;

/**
 * GWT emulation of {@code HashBiMap} that just delegates to a LinkedHashMap and a HashMap.
 *
 * @author Mike Bostock
 */
public final class HashBiMap<K, V> extends AbstractBiMap<K, V> {

  /**
   * Returns a new, empty {@code HashBiMap} with the default initial capacity
   * (16).
   */
  public static <K, V> HashBiMap<K, V> create() {
    return new HashBiMap<K, V>();
  }

  /**
   * Constructs a new, empty bimap with the specified expected size.
   *
   * @param expectedSize the expected number of entries
   * @throws IllegalArgumentException if the specified expected size is
   *     negative
   */
  public static <K, V> HashBiMap<K, V> create(int expectedSize) {
    return new HashBiMap<K, V>(expectedSize);
  }

  /**
   * Constructs a new bimap containing initial values from {@code map}. The
   * bimap is created with an initial capacity sufficient to hold the mappings
   * in the specified map.
   */
  public static <K, V> HashBiMap<K, V> create(
      Map<? extends K, ? extends V> map) {
    HashBiMap<K, V> bimap = create(map.size());
    bimap.putAll(map);
    return bimap;
  }

  private HashBiMap() {
    // we only care about the forward-direction order, so only that direction needs to be an LHM
    super(new LinkedHashMap<K, V>(), new HashMap<V, K>());
  }

  private HashBiMap(int expectedSize) {
    // we only care about the forward-direction order, so only that direction needs to be an LHM
    super(
        Maps.<K, V>newLinkedHashMapWithExpectedSize(expectedSize),
        Maps.<V, K>newHashMapWithExpectedSize(expectedSize));
  }

  @Override
  AbstractBiMap<V, K> makeInverse(Map<V, K> backward) {
    return new Inverse<V, K>(backward, this) {
      @Override
      Iterator<Entry<V, K>> entrySetIterator() {
        return new TransformedIterator<Entry<K, V>, Entry<V, K>>(
            HashBiMap.this.delegate().entrySet().iterator()) {
          @Override
          public Entry<V, K> transform(final Entry<K, V> forwardEntry) {
            return new AbstractMapEntry<V, K>() {
              final V value = forwardEntry.getValue();

              @Override
              public V getKey() {
                return value;
              }

              @Override
              public K getValue() {
                return delegate().get(value);
              }

              @Override
              public K setValue(K newKey) {
                // Preconditions keep the map and inverse consistent.
                checkState(entrySet().contains(this), "entry no longer in map");
                K oldKey = getValue();
                if (Objects.equal(newKey, oldKey)) {
                  return newKey;
                }
                forcePut(value, newKey);
                return oldKey;
              }
            };
          }
        };
      }
    };
  }

  // Override these two methods to show that keys and values may be null

  @Override
  public V put(@Nullable K key, @Nullable V value) {
    return super.put(key, value);
  }

  @Override public V forcePut(@Nullable K key, @Nullable V value) {
    return super.forcePut(key, value);
  }
}

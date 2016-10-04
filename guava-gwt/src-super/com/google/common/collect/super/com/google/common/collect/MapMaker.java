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

package com.google.common.collect;

import com.google.common.base.Function;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * MapMaker emulation. Since Javascript is single-threaded and have no references, this reduces to
 * the creation of expiring and computing maps.
 *
 * @author Charles Fry
 */
public final class MapMaker {

  // TODO(fry,kak): ConcurrentHashMap never throws a CME when mutating the map during iteration, but
  // this implementation (based on a LHM) does. This will all be replaced soon anyways, so leaving
  // it as is for now.
  private static class ComputingMap<K, V> extends LinkedHashMap<K, V>
      implements ConcurrentMap<K, V> {
    private final Function<? super K, ? extends V> computer;

    ComputingMap(int initialCapacity) {
      this(null, initialCapacity);
    }

    ComputingMap(Function<? super K, ? extends V> computer, int initialCapacity) {
      super(initialCapacity, /* ignored loadFactor */ 0.75f, true);
      this.computer = computer;
    }

    @Override
    public V putIfAbsent(K key, V value) {
      if (!containsKey(key)) {
        return put(key, value);
      } else {
        return get(key);
      }
    }

    @Override
    public boolean remove(Object key, Object value) {
      if (containsKey(key) && get(key).equals(value)) {
        remove(key);
        return true;
      }
      return false;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
      if (containsKey(key) && get(key).equals(oldValue)) {
        put(key, newValue);
        return true;
      }
      return false;
    }

    @Override
    public V replace(K key, V value) {
      return containsKey(key) ? put(key, value) : null;
    }

    @Override
    public V get(Object k) {
      // from CustomConcurrentHashMap
      V result = super.get(k);
      if (result == null && computer != null) {
        /*
         * This cast isn't safe, but we can rely on the fact that K is almost always passed to
         * Map.get(), and tools like IDEs and Findbugs can catch situations where this isn't the
         * case.
         *
         * The alternative is to add an overloaded method, but the chances of a user calling get()
         * instead of the new API and the risks inherent in adding a new API outweigh this little
         * hole.
         */
        @SuppressWarnings("unchecked")
        K key = (K) k;
        result = compute(key);
      }
      return result;
    }

    private V compute(K key) {
      // from MapMaker
      V value;
      try {
        value = computer.apply(key);
      } catch (Throwable t) {
        throw new ComputationException(t);
      }

      if (value == null) {
        String message = computer + " returned null for key " + key + ".";
        throw new NullPointerException(message);
      }
      put(key, value);
      return value;
    }
  }

  private int initialCapacity = 16;
  private boolean useCustomMap;

  public MapMaker() {}

  public MapMaker initialCapacity(int initialCapacity) {
    if (initialCapacity < 0) {
      throw new IllegalArgumentException();
    }
    this.initialCapacity = initialCapacity;
    return this;
  }

  public MapMaker concurrencyLevel(int concurrencyLevel) {
    if (concurrencyLevel < 1) {
      throw new IllegalArgumentException("GWT only supports a concurrency level of 1");
    }
    // GWT technically only supports concurrencyLevel == 1, but we silently
    // ignore other positive values.
    return this;
  }

  public <K, V> ConcurrentMap<K, V> makeMap() {
    return useCustomMap
        ? new ComputingMap<K, V>(null, initialCapacity)
        : new ConcurrentHashMap<K, V>(initialCapacity);
  }

  public <K, V> ConcurrentMap<K, V> makeComputingMap(Function<? super K, ? extends V> computer) {
    return new ComputingMap<K, V>(computer, initialCapacity);
  }
}

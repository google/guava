/*
 * Copyright (C) 2009 Google Inc.
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
import com.google.gwt.user.client.Timer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * MapMaker emulation. Since Javascript is single-threaded and have no
 * references, this reduces to the creation of expiring and computing maps.
 *
 * @author Charles Fry
 */
public class MapMaker {

  private static class ExpiringComputingMap<K, V>
      extends ConcurrentHashMap<K, V> {
    private long expirationMillis;
    private final Function<? super K, ? extends V> computer;

    public ExpiringComputingMap(long expirationMillis) {
      this(expirationMillis, null);
    }

    public ExpiringComputingMap(long expirationMillis,
        Function<? super K, ? extends V> computer) {
      this.expirationMillis = expirationMillis;
      this.computer = computer;
    }

    @Override
    public V put(K key, V value) {
      V result = super.put(key, value);
      if (expirationMillis > 0) {
        scheduleRemoval(key, value);
      }
      return result;
    }

    private void scheduleRemoval(final K key, final V value) {
      // from MapMaker
      /*
       * TODO: Keep weak reference to map, too. Build a priority
       * queue out of the entries themselves instead of creating a
       * task per entry. Then, we could have one recurring task per
       * map (which would clean the entire map and then reschedule
       * itself depending upon when the next expiration comes). We
       * also want to avoid removing an entry prematurely if the
       * entry was set to the same value again.
       */
      Timer timer = new Timer() {
        public void run() {
          remove(key, value);
        }
      };
      timer.schedule((int) expirationMillis);
    }

    @Override
    public V get(Object k) {
      // from CustomConcurrentHashMap
      V result = super.get(k);
      if (result == null && computer != null) {
        /*
         * This cast isn't safe, but we can rely on the fact that K is almost
         * always passed to Map.get(), and tools like IDEs and Findbugs can
         * catch situations where this isn't the case.
         *
         * The alternative is to add an overloaded method, but the chances of
         * a user calling get() instead of the new API and the risks inherent
         * in adding a new API outweigh this little hole.
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
  private float loadFactor = 0.75f;
  private long expirationMillis = 0;
  private boolean useCustomMap;

  public MapMaker() {
  }

  public MapMaker initialCapacity(int initialCapacity) {
    if (initialCapacity < 0) {
      throw new IllegalArgumentException();
    }
    this.initialCapacity = initialCapacity;
    return this;
  }

  public MapMaker loadFactor(float loadFactor) {
    if (loadFactor <= 0) {
      throw new IllegalArgumentException();
    }
    this.loadFactor = loadFactor;
    return this;
  }

  public MapMaker expiration(long duration, TimeUnit unit) {
    if (expirationMillis != 0) {
      throw new IllegalStateException("expiration time of "
          + expirationMillis + " ns was already set");
    }
    if (duration <= 0) {
      throw new IllegalArgumentException("invalid duration: " + duration);
    }
    this.expirationMillis = unit.toMillis(duration);
    useCustomMap = true;
    return this;
  }

  public <K, V> ConcurrentMap<K, V> makeMap() {
    return useCustomMap
        ? new ExpiringComputingMap<K, V>(expirationMillis)
        : new ConcurrentHashMap<K, V>(initialCapacity, loadFactor);
  }

  public <K, V> ConcurrentMap<K, V> makeComputingMap(
      Function<? super K, ? extends V> computer) {
    return new ExpiringComputingMap<K, V>(expirationMillis, computer);
  }

}

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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Function;
import com.google.common.cache.CacheLoader.InvalidCacheLoadException;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.gwt.user.client.Timer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

/**
 * CacheBuilder emulation.
 *
 * @author Charles Fry
 */
// TODO(fry): eventually we should emmulate LocalCache instead of CacheBuilder
public class CacheBuilder<K, V> {
  private static final int UNSET_INT = -1;
  private static final int DEFAULT_INITIAL_CAPACITY = 16;
  private static final int DEFAULT_EXPIRATION_NANOS = 0;

  private int initialCapacity = -1;
  private int concurrencyLevel = -1;
  private long expirationMillis = -1;
  private int maximumSize = -1;

  CacheBuilder() {}

  public static CacheBuilder<Object, Object> newBuilder() {
    return new CacheBuilder<Object, Object>();
  }

  public CacheBuilder<K, V> initialCapacity(int initialCapacity) {
    checkState(this.initialCapacity == UNSET_INT, "initial capacity was already set to %s",
        this.initialCapacity);
    checkArgument(initialCapacity >= 0);
    this.initialCapacity = initialCapacity;
    return this;
  }

  private int getInitialCapacity() {
    return (initialCapacity == UNSET_INT) ? DEFAULT_INITIAL_CAPACITY : initialCapacity;
  }

  public CacheBuilder<K, V> concurrencyLevel(int concurrencyLevel) {
    checkState(this.concurrencyLevel == UNSET_INT, "concurrency level was already set to %s",
        this.concurrencyLevel);
    checkArgument(concurrencyLevel > 0);
    // GWT technically only supports concurrencyLevel == 1, but we silently
    // ignore other positive values.
    this.concurrencyLevel = concurrencyLevel;
    return this;
  }

  public CacheBuilder<K, V> expireAfterWrite(long duration, TimeUnit unit) {
    checkState(expirationMillis == UNSET_INT, "expireAfterWrite was already set to %s ms",
        expirationMillis);
    checkArgument(duration >= 0, "duration cannot be negative: %s %s", duration, unit);
    this.expirationMillis = unit.toMillis(duration);
    return this;
  }

  private long getExpirationMillis() {
    return (expirationMillis == UNSET_INT) ? DEFAULT_EXPIRATION_NANOS : expirationMillis;
  }

  public CacheBuilder<K, V> maximumSize(int maximumSize) {
    if (this.maximumSize != -1) {
      throw new IllegalStateException("maximum size of " + maximumSize + " was already set");
    }
    if (maximumSize < 0) {
      throw new IllegalArgumentException("invalid maximum size: " + maximumSize);
    }
    this.maximumSize = maximumSize;
    return this;
  }

  public <K1 extends K, V1 extends V> Cache<K1, V1> build() {
    return new LocalManualCache<K1, V1>(this);
  }

  public <K1 extends K, V1 extends V> LoadingCache<K1, V1> build(
      CacheLoader<? super K1, V1> loader) {
    return new LocalLoadingCache<K1, V1>(this, loader);
  }

  private static class LocalManualCache<K, V>
      extends AbstractCache<K, V> implements Function<K, V> {
    final LocalCache<K, V> localCache;

    LocalManualCache(CacheBuilder<? super K, ? super V> builder) {
      this(builder, null);
    }

    protected LocalManualCache(CacheBuilder<? super K, ? super V> builder,
        CacheLoader<? super K, V> loader) {
      this.localCache = new LocalCache<K, V>(builder, loader);
    }

    // Cache methods

    @Override
    public V get(K key) throws ExecutionException {
      return localCache.getOrLoad(key);
    }

    @Override
    public V getUnchecked(K key) {
      try {
        return get(key);
      } catch (ExecutionException e) {
        throw new UncheckedExecutionException(e.getCause());
      }
    }

    @Override
    public final V apply(K key) {
      return getUnchecked(key);
    }

    @Override
    @Nullable
    public V getIfPresent(K key) {
      return localCache.get(key);
    }

    @Override
    public void put(K key, V value) {
      localCache.put(key, value);
    }

    @Override
    public void invalidate(Object key) {
      checkNotNull(key);
      localCache.remove(key);
    }

    @Override
    public void invalidateAll() {
      localCache.clear();
    }

    @Override
    public long size() {
      return localCache.size();
    }

    @Override
    public ConcurrentMap<K, V> asMap() {
      return localCache;
    }
  }

  private static class LocalLoadingCache<K, V>
      extends LocalManualCache<K, V> implements LoadingCache<K, V> {

    LocalLoadingCache(CacheBuilder<? super K, ? super V> builder,
        CacheLoader<? super K, V> loader) {
      super(builder, checkNotNull(loader));
    }

    // Cache methods

    @Override
    public ImmutableMap<K, V> getAll(Iterable<? extends K> keys) throws ExecutionException {
      throw new UnsupportedOperationException();
    }

    @Override
    public void refresh(K key) {
      throw new UnsupportedOperationException();
    }
  }

  // TODO(fry,user): ConcurrentHashMap never throws a CME when mutating the map during iteration, but
  // this implementation (based on a LHM) does. This will all be replaced soon anyways, so leaving
  // it as is for now.
  private static class LocalCache<K, V> extends LinkedHashMap<K, V>
      implements ConcurrentMap<K, V> {
    private final CacheLoader<? super K, V> loader;
    private final long expirationMillis;
    private final int maximumSize;

    LocalCache(CacheBuilder<? super K, ? super V> builder, CacheLoader<? super K, V> loader) {
      super(builder.getInitialCapacity(), 0.75f, (builder.maximumSize != UNSET_INT));
      this.loader = loader;
      this.expirationMillis = builder.getExpirationMillis();
      this.maximumSize = builder.maximumSize;
    }

    @Override
    public V put(K key, V value) {
      V result = super.put(key, value);
      if (expirationMillis > 0) {
        scheduleRemoval(key, value);
      }
      return result;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> ignored) {
      return (maximumSize == -1) ? false : size() > maximumSize;
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

    private void scheduleRemoval(final K key, final V value) {
      /*
       * TODO: Keep weak reference to map, too. Build a priority queue out of the entries themselves
       * instead of creating a task per entry. Then, we could have one recurring task per map (which
       * would clean the entire map and then reschedule itself depending upon when the next
       * expiration comes). We also want to avoid removing an entry prematurely if the entry was set
       * to the same value again.
       */
      Timer timer = new Timer() {
        @Override
        public void run() {
          remove(key, value);
        }
      };
      timer.schedule((int) expirationMillis);
    }

    public V getOrLoad(Object k) throws ExecutionException {
      // from CustomConcurrentHashMap
      V result = super.get(k);
      if (result == null) {
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

    private V compute(K key) throws ExecutionException {
      V value;
      try {
        value = loader.load(key);
      } catch (RuntimeException e) {
        throw new UncheckedExecutionException(e);
      } catch (Exception e) {
        throw new ExecutionException(e);
      } catch (Error e) {
        throw new ExecutionError(e);
      }

      if (value == null) {
        String message = loader + " returned null for key " + key + ".";
        throw new InvalidCacheLoadException(message);
      }
      put(key, value);
      return value;
    }
  }

}

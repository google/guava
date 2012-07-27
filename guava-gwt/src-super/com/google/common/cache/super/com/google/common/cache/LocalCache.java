/*
 * Copyright (C) 2012 The Guava Authors
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

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Equivalence;
import com.google.common.base.Ticker;
import com.google.common.cache.AbstractCache.StatsCounter;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

/**
 * LocalCache emulation for GWT.
 *
 * @param <K> the base key type
 * @param <V> the base value type
 * @author Charles Fry
 * @author Jon Donovan
 */
public class LocalCache<K, V> implements ConcurrentMap<K, V> {
  private static final int UNSET_INT = CacheBuilder.UNSET_INT;
  
  private final LinkedHashMap<K, Timestamped<V>> cachingHashMap;
  private final CacheLoader<? super K, V> loader;
  private final RemovalListener removalListener;
  private final StatsCounter statsCounter;
  private final Ticker ticker;
  private final long expireAfterWrite;
  private final long expireAfterAccess;

  LocalCache(CacheBuilder<? super K, ? super V> builder, CacheLoader<? super K, V> loader) {
    this.loader = loader;   
    this.removalListener = builder.removalListener;
    this.expireAfterAccess = builder.expireAfterAccessNanos;
    this.expireAfterWrite = builder.expireAfterWriteNanos;
    this.statsCounter = builder.getStatsCounterSupplier().get();
    
    /* Implements size-capped LinkedHashMap */
    final long maximumSize = builder.maximumSize;
    this.cachingHashMap = new CapacityEnforcingLinkedHashMap<K, V>(
        builder.getInitialCapacity(),
        0.75f,
        (builder.maximumSize != UNSET_INT),
        builder.maximumSize,
        statsCounter,
        removalListener);

    this.ticker = firstNonNull(builder.ticker, Ticker.systemTicker());
  }
  
  @Override
  public int size() {
    return cachingHashMap.size();
  }

  @Override
  public boolean isEmpty() {
    return cachingHashMap.isEmpty();
  }
  
  @Override
  public V get(Object key) {
    key = checkNotNull(key);
    Timestamped<V> value = cachingHashMap.get(key);

    if (value == null) {
      statsCounter.recordMisses(1);
      return null;
    } else if (!isExpired(value)) {
      statsCounter.recordHits(1);
      value.updateTimestamp();
      return value.getValue();
    } else {
      statsCounter.recordEviction();
      statsCounter.recordMisses(1);
      alertListenerIfPresent(key, value.getValue(), RemovalCause.EXPIRED);
      cachingHashMap.remove(key);
      return null;
    }
  }

  @Override
  public V put(K key, V value) {
    key = checkNotNull(key);
    value = checkNotNull(value);
    Timestamped<V> oldValue = cachingHashMap.put(key, new Timestamped<V>(value, ticker));
    if (oldValue == null) {
      return null;
    }
    alertListenerIfPresent(key, oldValue.getValue(), RemovalCause.REPLACED);
    return oldValue.getValue();
  }

  @Override
  public V remove(Object key) {
    Timestamped<V> stamped = cachingHashMap.remove(key);
    V value = stamped.getValue();
    
    if (!isExpired(stamped)) {
      alertListenerIfPresent(key, value, RemovalCause.EXPLICIT);
      return value;
    }

    alertListenerIfPresent(key, value, RemovalCause.EXPIRED);
    return null;
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void clear() {
    if (removalListener != null) {
      for (Map.Entry<K, Timestamped<V>> entry : cachingHashMap.entrySet()) {
        alertListenerIfPresent(entry.getKey(), entry.getValue().getValue(), RemovalCause.EXPLICIT);
      }
    }
    cachingHashMap.clear();
  }

  @Override
  public V putIfAbsent(K key, V value) {
    V currentValue = get(key);
    if (currentValue != null) {
      return currentValue;
    }
    return put(key, value);
  }

  @Override
  public boolean remove(Object key, Object value) {
    if (value.equals(get(key))) {
      alertListenerIfPresent(key, value, RemovalCause.EXPLICIT);
      remove(key);
      return true;
    }
    return false;
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    if (oldValue.equals(get(key))) {
      alertListenerIfPresent(key, oldValue, RemovalCause.REPLACED);
      put(key, newValue);
      return true;
    }
    return false;
  }

  @Override
  public V replace(K key, V value) {
    V currentValue = get(key);
    if (currentValue != null) {
      alertListenerIfPresent(key, currentValue, RemovalCause.REPLACED);
      return put(key, value);
    }
    return null;
  }
  
  @Override
  public boolean containsKey(Object key) {
    return cachingHashMap.containsKey(key) && !isExpired(cachingHashMap.get(key));
  }
  
  @Override
  public boolean containsValue(Object value) {
    for (Map.Entry<K, Timestamped<V>> entry : cachingHashMap.entrySet()) {
      if (entry.getValue().equals(value)) {
        if (value.equals(getIfPresent(entry.getKey()))) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public Collection<V> values() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<K> keySet() {
    throw new UnsupportedOperationException();
  }

  private boolean isExpired(Timestamped<V> stamped) {
    if ((expireAfterAccess == UNSET_INT) && (expireAfterWrite == UNSET_INT)) {
      return false;
    }

    boolean expireWrite = (stamped.getWriteTimestamp() + expireAfterWrite <= currentTimeNanos());
    boolean expireAccess = (stamped.getAccessTimestamp() + expireAfterAccess <= currentTimeNanos());
    
    if (expireAfterAccess == UNSET_INT) {
      return expireWrite;
    }
    if (expireAfterWrite == UNSET_INT) {
      return expireAccess;
    }

    return expireWrite || expireAccess;
  }

  private long currentTimeNanos() {
    return ticker.read();
  }
  
  private void alertListenerIfPresent(Object key, Object value, RemovalCause cause) {
    if (removalListener != null) {
      removalListener.onRemoval(new RemovalNotification(key, value, cause));
    }
  }

  private V load(Object key) throws ExecutionException {
    long startTime = ticker.read();
    V calculatedValue;
    try {
      /*
       * This cast isn't safe, but we can rely on the fact that K is almost always passed to
       * Map.get(), and tools like IDEs and Findbugs can catch situations where this isn't the
       * case.
       *
       * The alternative is to add an overloaded method, but the chances of a user calling get()
       * instead of the new API and the risks inherent in adding a new API outweigh this little
       * hole.
       */
      K castKey = (K) key;
      calculatedValue = loader.load(castKey);
      put(castKey, calculatedValue);
    } catch (RuntimeException e) {
      statsCounter.recordLoadException(ticker.read() - startTime);
      throw new UncheckedExecutionException(e);
    } catch (Exception e) {
      statsCounter.recordLoadException(ticker.read() - startTime);
      throw new ExecutionException(e);
    } catch (Error e) {
      statsCounter.recordLoadException(ticker.read() - startTime);
      throw new ExecutionError(e);
    }

    if (calculatedValue == null) {
      String message = loader + " returned null for key " + key + ".";
      throw new CacheLoader.InvalidCacheLoadException(message);
    }
    statsCounter.recordLoadSuccess(ticker.read() - startTime);
    return calculatedValue;
  }
  
  private V getIfPresent(Object key) {
    key = checkNotNull(key);
    Timestamped<V> value = cachingHashMap.get(key);

    if (value == null) {
      return null;
    } else if (!isExpired(value)) {
      value.updateTimestamp();
      return value.getValue();
    } else {
      alertListenerIfPresent(key, value.getValue(), RemovalCause.EXPIRED);
      cachingHashMap.remove(key);
      return null;
    }    
  }

  private V getOrLoad(K key) throws ExecutionException{
    V value = get(key);
    if (value != null) {
      return value;
    }
    return load(key);
  }
  
  private static class Timestamped<V> {
    private final V value;
    private final Ticker ticker;
    private long writeTimestamp;
    private long accessTimestamp;
    
    public Timestamped(V value, Ticker ticker) {
      this.value = checkNotNull(value);
      this.ticker = checkNotNull(ticker);
      this.writeTimestamp = ticker.read();
      this.accessTimestamp = this.writeTimestamp;
    }
    
    public V getValue() {
      return value;
    }
    
    public void updateTimestamp() {
      accessTimestamp = ticker.read();
    }
  
    public long getAccessTimestamp() {
      return accessTimestamp;
    }
    
    public long getWriteTimestamp() {
      return writeTimestamp;
    }
    
    public boolean equals(Object o) {
      return value.equals(o);
    }
    
    public int hashCode() {
      return value.hashCode();
    }
  }

  /**
   * LocalManualCache is a wrapper around LocalCache for a cache without loading.
   *
   * @param <K> the base key type
   * @param <V> the base value type
   */
  public static class LocalManualCache<K, V> extends AbstractCache<K, V> {
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
    public V get(K key, Callable<? extends V> valueLoader) throws ExecutionException {
      V value = localCache.get(key);
      if (value != null) {
        return value;
      }
      
      try {
        V newValue = valueLoader.call();
        localCache.put(key, newValue);
        return newValue;
      } catch (Exception e) {
        throw new ExecutionException(e);
      }
    }
    
    @Override
    @Nullable
    public V getIfPresent(Object key) {
      return localCache.getIfPresent(key);
    }

    @Override
    public void put(K key, V value) {
      localCache.put(key, value);
    }

    @Override
    public void invalidate(Object key) {
      key = checkNotNull(key);
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

  /**
   * LocalLoadingCache is a wrapper around LocalCache for a cache with loading.
   *
   * @param <K> the base key type
   * @param <V> the base value type
   */
  public static class LocalLoadingCache<K, V>
      extends LocalManualCache<K, V> implements LoadingCache<K, V> {

    LocalLoadingCache(CacheBuilder<? super K, ? super V> builder,
        CacheLoader<? super K, V> loader) {
      super(builder, checkNotNull(loader));
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
    public ImmutableMap<K, V> getAll(Iterable<? extends K> keys) throws ExecutionException {
      Map<K, V> map = new HashMap<K, V>();
      for (K key : keys) {
        map.put(key, localCache.get(key));
      }
      return ImmutableMap.copyOf(map);
    }
    
    @Override
    public void refresh(K key) {
      throw new UnsupportedOperationException();
    }
  }
  
  /**
   * LinkedHashMap that enforces it's maximum size and logs events in a StatsCounter object
   * and an optional RemovalListener.
   * 
   * @param <K> the base key type
   * @param <V> the base value type
   */
  private class CapacityEnforcingLinkedHashMap<K, V> extends LinkedHashMap<K, Timestamped<V>> {
    
    private final StatsCounter statsCounter;
    private final RemovalListener removalListener;
    private final long maximumSize;
    
    public CapacityEnforcingLinkedHashMap(
        int initialCapacity,
        float loadFactor,
        boolean accessOrder,
        long maximumSize,
        StatsCounter statsCounter,
        @Nullable RemovalListener removalListener) {
      super(initialCapacity, loadFactor, accessOrder);
      this.maximumSize = maximumSize;
      this.statsCounter = statsCounter;
      this.removalListener = removalListener;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, Timestamped<V>> ignored) {
      boolean removal = (maximumSize == UNSET_INT) ? false : (size() > maximumSize);
      if ((removalListener != null) && removal) {
        removalListener.onRemoval(new RemovalNotification(
            ignored.getKey(), 
            ignored.getValue().getValue(),
            RemovalCause.SIZE));
      }
      statsCounter.recordEviction();
      return removal;
    }
  }
  
  /**
   * Any updates to LocalCache.Strength used in CacheBuilder need to be matched in this class for
   * compilation purposes.
   */
  enum Strength {
    /*
     * TODO(kevinb): If we strongly reference the value and aren't loading, we needn't wrap the
     * value. This could save ~8 bytes per entry.
     */

    STRONG {
      @Override
      Equivalence<Object> defaultEquivalence() {
        return Equivalence.equals();
      }
    },

    SOFT {
      @Override
      Equivalence<Object> defaultEquivalence() {
        return Equivalence.identity();
      }
    },

    WEAK {
      @Override
      Equivalence<Object> defaultEquivalence() {
        return Equivalence.identity();
      }
    };
    
    abstract Equivalence<Object> defaultEquivalence();
  }
}

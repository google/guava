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

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Equivalence;
import com.google.common.base.Ticker;
import com.google.common.cache.AbstractCache.StatsCounter;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import org.checkerframework.checker.nullness.qual.Nullable;

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
    this.cachingHashMap =
        new CapacityEnforcingLinkedHashMap<K, V>(
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
    checkNotNull(key);
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

  @CanIgnoreReturnValue
  @Override
  public V put(K key, V value) {
    checkNotNull(key);
    checkNotNull(value);
    Timestamped<V> oldValue = cachingHashMap.put(key, new Timestamped<V>(value, ticker));
    if (oldValue == null) {
      return null;
    }
    alertListenerIfPresent(key, oldValue.getValue(), RemovalCause.REPLACED);
    return oldValue.getValue();
  }

  @CanIgnoreReturnValue
  @Override
  public V remove(Object key) {
    Timestamped<V> stamped = cachingHashMap.remove(key);
    if (stamped != null) {
      V value = stamped.getValue();

      if (!isExpired(stamped)) {
        alertListenerIfPresent(key, value, RemovalCause.EXPLICIT);
        return value;
      }

      alertListenerIfPresent(key, value, RemovalCause.EXPIRED);
    }
    return null;
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void clear() {
    if (removalListener != null) {
      for (Entry<K, Timestamped<V>> entry : cachingHashMap.entrySet()) {
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

  @CanIgnoreReturnValue
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
    for (Timestamped<V> val : cachingHashMap.values()) {
      if (val.getValue().equals(value)) {
        if (!isExpired(val)) {
          return true;
        }
      }
    }
    return false;
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

  @SuppressWarnings("GoodTime")
  private long currentTimeNanos() {
    return ticker.read();
  }

  private void alertListenerIfPresent(Object key, Object value, RemovalCause cause) {
    if (removalListener != null) {
      removalListener.onRemoval(RemovalNotification.create(key, value, cause));
    }
  }

  @SuppressWarnings("GoodTime") // timestamps as numeric primitives
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
    checkNotNull(key);
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

  private V getOrLoad(K key) throws ExecutionException {
    V value = get(key);
    if (value != null) {
      return value;
    }
    return load(key);
  }

  @SuppressWarnings("GoodTime") // timestamps as numeric primitives
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

    protected LocalManualCache(
        CacheBuilder<? super K, ? super V> builder, CacheLoader<? super K, V> loader) {
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
    public @Nullable V getIfPresent(Object key) {
      return localCache.getIfPresent(key);
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

  /**
   * LocalLoadingCache is a wrapper around LocalCache for a cache with loading.
   *
   * @param <K> the base key type
   * @param <V> the base value type
   */
  public static class LocalLoadingCache<K, V> extends LocalManualCache<K, V>
      implements LoadingCache<K, V> {

    LocalLoadingCache(
        CacheBuilder<? super K, ? super V> builder, CacheLoader<? super K, V> loader) {
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
        map.put(key, localCache.getOrLoad(key));
      }
      return ImmutableMap.copyOf(map);
    }

    @Override
    public void refresh(K key) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * LinkedHashMap that enforces it's maximum size and logs events in a StatsCounter object and an
   * optional RemovalListener.
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
    protected boolean removeEldestEntry(Entry<K, Timestamped<V>> ignored) {
      boolean removal = (maximumSize == UNSET_INT) ? false : (size() > maximumSize);
      if ((removalListener != null) && removal) {
        removalListener.onRemoval(
            RemovalNotification.create(
                ignored.getKey(), ignored.getValue().getValue(), RemovalCause.SIZE));
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

  /**
   * Implementation for the EntryIterator, which is used to build Key and Value iterators.
   *
   * <p>Expiration is only checked on hasNext(), so as to ensure that a next() call never returns
   * null when hasNext() has already been called.
   */
  class EntryIterator implements Iterator<Entry<K, V>> {
    Iterator<Entry<K, Timestamped<V>>> iterator;
    Entry<K, Timestamped<V>> lastEntry;
    Entry<K, Timestamped<V>> nextEntry;

    EntryIterator() {
      this.iterator = LocalCache.this.cachingHashMap.entrySet().iterator();
    }

    @Override
    public Entry<K, V> next() {
      if (nextEntry == null) {
        boolean unused = hasNext();

        if (nextEntry == null) {
          throw new NoSuchElementException();
        }
      }

      lastEntry = nextEntry;
      nextEntry = null;
      return new WriteThroughEntry(lastEntry.getKey(), lastEntry.getValue().getValue());
    }

    @Override
    public boolean hasNext() {
      if (nextEntry == null) {
        while (iterator.hasNext()) {
          Entry<K, Timestamped<V>> next = iterator.next();
          if (!isExpired(next.getValue())) {
            nextEntry = next;
            return true;
          }
        }
        return false;
      }
      return true;
    }

    @Override
    public void remove() {
      checkState(lastEntry != null);
      LocalCache.this.remove(lastEntry.getKey(), lastEntry.getValue());
      lastEntry = null;
    }
  }

  /** KeyIterator build on top of EntryIterator. */
  final class KeyIterator implements Iterator<K> {
    private EntryIterator iterator;

    KeyIterator() {
      iterator = new EntryIterator();
    }

    @Override
    public boolean hasNext() {
      return iterator.hasNext();
    }

    @Override
    public K next() {
      return iterator.next().getKey();
    }

    @Override
    public void remove() {
      iterator.remove();
    }
  }

  /** ValueIterator build on top of EntryIterator. */
  final class ValueIterator implements Iterator<V> {
    private EntryIterator iterator;

    ValueIterator() {
      iterator = new EntryIterator();
    }

    @Override
    public boolean hasNext() {
      return iterator.hasNext();
    }

    @Override
    public V next() {
      return iterator.next().getValue();
    }

    @Override
    public void remove() {
      iterator.remove();
    }
  }

  Set<K> keySet;

  @Override
  public Set<K> keySet() {
    // does not impact recency ordering
    Set<K> ks = keySet;
    return (ks != null) ? ks : (keySet = new KeySet(this));
  }

  Collection<V> values;

  @Override
  public Collection<V> values() {
    // does not impact recency ordering
    Collection<V> vs = values;
    return (vs != null) ? vs : (values = new Values(this));
  }

  Set<Entry<K, V>> entrySet;

  @Override
  public Set<Entry<K, V>> entrySet() {
    // does not impact recency ordering
    Set<Entry<K, V>> es = entrySet;
    return (es != null) ? es : (entrySet = new EntrySet(this));
  }

  /**
   * Custom Entry class used by EntryIterator.next(), that relays setValue changes to the underlying
   * map.
   */
  private final class WriteThroughEntry implements Entry<K, V> {
    final K key;
    V value;

    WriteThroughEntry(K key, V value) {
      this.key = checkNotNull(key);
      this.value = checkNotNull(value);
    }

    @Override
    public K getKey() {
      return key;
    }

    @Override
    public V getValue() {
      return value;
    }

    @Override
    public boolean equals(@Nullable Object object) {
      // Cannot use key and value equivalence
      if (object instanceof Entry) {
        Entry<?, ?> that = (Entry<?, ?>) object;
        return key.equals(that.getKey()) && value.equals(that.getValue());
      }
      return false;
    }

    @Override
    public int hashCode() {
      // Cannot use key and value equivalence
      return key.hashCode() ^ value.hashCode();
    }

    @Override
    public V setValue(V newValue) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return getKey() + "=" + getValue();
    }
  }

  // TODO(fry): Separate logic for consistency between emul and nonemul implementation.
  // TODO(fry): Look into Maps.KeySet and Maps.Values, which can ideally be reused here but are
  // currently only package visible.
  abstract class AbstractCacheSet<T> extends AbstractSet<T> {
    final ConcurrentMap<?, ?> map;

    AbstractCacheSet(ConcurrentMap<?, ?> map) {
      this.map = map;
    }

    @Override
    public int size() {
      return map.size();
    }

    @Override
    public boolean isEmpty() {
      return map.isEmpty();
    }

    @Override
    public void clear() {
      map.clear();
    }
  }

  private final class KeySet extends AbstractCacheSet<K> {

    KeySet(ConcurrentMap<?, ?> map) {
      super(map);
    }

    @Override
    public Iterator<K> iterator() {
      return new KeyIterator();
    }

    @Override
    public boolean contains(Object o) {
      return map.containsKey(o);
    }

    @Override
    public boolean remove(Object o) {
      return map.remove(o) != null;
    }
  }

  private final class Values extends AbstractCollection<V> {
    final ConcurrentMap<?, ?> map;

    Values(ConcurrentMap<?, ?> map) {
      this.map = map;
    }

    @Override
    public Iterator<V> iterator() {
      return new ValueIterator();
    }

    @Override
    public boolean contains(Object o) {
      return map.containsValue(o);
    }

    @Override
    public int size() {
      return map.size();
    }

    @Override
    public boolean isEmpty() {
      return map.isEmpty();
    }

    @Override
    public void clear() {
      map.clear();
    }
  }

  private final class EntrySet extends AbstractCacheSet<Entry<K, V>> {

    EntrySet(ConcurrentMap<?, ?> map) {
      super(map);
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
      return new EntryIterator();
    }

    @Override
    public boolean contains(Object o) {
      if (!(o instanceof Entry)) {
        return false;
      }
      Entry<?, ?> e = (Entry<?, ?>) o;
      Object key = e.getKey();
      if (key == null) {
        return false;
      }
      V v = LocalCache.this.get(key);

      return (v != null) && e.getValue().equals(v);
    }

    @Override
    public boolean remove(Object o) {
      if (!(o instanceof Entry)) {
        return false;
      }
      Entry<?, ?> e = (Entry<?, ?>) o;
      Object key = e.getKey();
      return (key != null) && LocalCache.this.remove(key, e.getValue());
    }
  }
}

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

package com.google.common.cache;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.Beta;
import com.google.common.base.Ascii;
import com.google.common.base.Equivalence;
import com.google.common.base.Equivalences;
import com.google.common.base.Objects;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.base.Ticker;
import com.google.common.cache.AbstractCache.SimpleStatsCounter;
import com.google.common.cache.AbstractCache.StatsCounter;
import com.google.common.cache.CustomConcurrentHashMap.Strength;
import com.google.common.collect.ForwardingConcurrentMap;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

/**
 * <p>A builder of {@link Cache} instances having any combination of the following features:
 *
 * <ul>
 * <li>least-recently-used eviction when a maximum size is exceeded
 * <li>time-based expiration of entries, measured since last access or last write
 * <li>keys automatically wrapped in {@linkplain WeakReference weak} references
 * <li>values automatically wrapped in {@linkplain WeakReference weak} or
 *     {@linkplain SoftReference soft} references
 * <li>notification of evicted (or otherwise removed) entries
 * </ul>
 *
 * <p>Usage example: <pre>   {@code
 *
 *   Cache<Key, Graph> graphs = CacheBuilder.newBuilder()
 *       .concurrencyLevel(4)
 *       .weakKeys()
 *       .maximumSize(10000)
 *       .expireAfterWrite(10, TimeUnit.MINUTES)
 *       .build(
 *           new CacheLoader<Key, Graph>() {
 *             public Graph load(Key key) throws AnyException {
 *               return createExpensiveGraph(key);
 *             }
 *           });}</pre>
 *
 *
 * These features are all optional.
 *
 * <p>The returned cache is implemented as a hash table with similar performance characteristics to
 * {@link ConcurrentHashMap}. It implements the optional operations {@link Cache#invalidate},
 * {@link Cache#invalidateAll}, {@link Cache#size}, {@link Cache#stats}, and {@link Cache#asMap},
 * with the following qualifications:
 *
 * <ul>
 * <li>The {@code invalidateAll} method will invalidate all cached entries prior to returning, and
 *     removal notifications will be issued for all invalidated entries.
 * <li>The {@code asMap} view (and its collection views) have <i>weakly consistent iterators</i>.
 *     This means that they are safe for concurrent use, but if other threads modify the cache after
 *     the iterator is created, it is undefined which of these changes, if any, are reflected in
 *     that iterator. These iterators never throw {@link ConcurrentModificationException}.
 * </ul>
 *
 * <p><b>Note:</b> by default, the returned cache uses equality comparisons (the
 * {@link Object#equals equals} method) to determine equality for keys or values. However, if
 * {@link #weakKeys} was specified, the cache uses identity ({@code ==})
 * comparisons instead for keys. Likewise, if {@link #weakValues} or {@link #softValues} was
 * specified, the cache uses identity comparisons for values.
 *
 * <p>If soft or weak references were requested, it is possible for a key or value present in the
 * the cache to be reclaimed by the garbage collector. If this happens, the entry automatically
 * disappears from the cache. A partially-reclaimed entry is never exposed to the user.
 *
 * <p>Certain cache configurations will result in the accrual of periodic maintenance tasks which
 * will be performed during write operations, or during occasional read operations in the absense of
 * writes. The {@link Cache#cleanUp} method of the returned cache will also perform maintenance, but
 * calling it should not be necessary with a high throughput cache. Only caches built with
 * {@linkplain CacheBuilder#removalListener removalListener},
 * {@linkplain CacheBuilder#expireAfterWrite expireAfterWrite},
 * {@linkplain CacheBuilder#expireAfterAccess expireAfterAccess},
 * {@linkplain CacheBuilder#weakKeys weakKeys}, {@linkplain CacheBuilder#weakValues weakValues},
 * or {@linkplain CacheBuilder#softValues softValues} perform periodic maintenance.
 *
 * <p>The caches produced by {@code CacheBuilder} are serializable, and the deserialized caches
 * retain all the configuration properties of the original cache. Note that the serialized form does
 * <i>not</i> include cache contents, but only configuration.
 *
 * @param <K> the base key type for all caches created by this builder
 * @param <V> the base value type for all caches created by this builder
 * @author Charles Fry
 * @author Kevin Bourrillion
 * @since 10.0
 */
@Beta
public final class CacheBuilder<K, V> {
  private static final int DEFAULT_INITIAL_CAPACITY = 16;
  private static final int DEFAULT_CONCURRENCY_LEVEL = 4;
  private static final int DEFAULT_EXPIRATION_NANOS = 0;

  static final Supplier<? extends StatsCounter> DEFAULT_STATS_COUNTER = Suppliers.ofInstance(
      new StatsCounter() {
        @Override
        public void recordHit() {}

        @Override
        public void recordLoadSuccess(long loadTime) {}

        @Override
        public void recordLoadException(long loadTime) {}

        @Override
        public void recordConcurrentMiss() {}

        @Override
        public void recordEviction() {}

        @Override
        public CacheStats snapshot() {
          return EMPTY_STATS;
        }
      });
  static final CacheStats EMPTY_STATS = new CacheStats(0, 0, 0, 0, 0, 0);

  static final Supplier<SimpleStatsCounter> CACHE_STATS_COUNTER =
      new Supplier<SimpleStatsCounter>() {
    @Override
    public SimpleStatsCounter get() {
      return new SimpleStatsCounter();
    }
  };

  enum NullListener implements RemovalListener<Object, Object> {
    INSTANCE;

    @Override
    public void onRemoval(RemovalNotification<Object, Object> notification) {}
  }

  static final int UNSET_INT = -1;

  int initialCapacity = UNSET_INT;
  int concurrencyLevel = UNSET_INT;
  int maximumSize = UNSET_INT;

  Strength keyStrength;
  Strength valueStrength;

  long expireAfterWriteNanos = UNSET_INT;
  long expireAfterAccessNanos = UNSET_INT;

  RemovalCause nullRemovalCause;

  Equivalence<Object> keyEquivalence;
  Equivalence<Object> valueEquivalence;

  RemovalListener<? super K, ? super V> removalListener;

  Ticker ticker;

  // TODO(fry): make constructor private and update tests to use newBuilder
  CacheBuilder() {}

  /**
   * Constructs a new {@code CacheBuilder} instance with default settings, including strong keys,
   * strong values, and no automatic eviction of any kind.
   */
  public static CacheBuilder<Object, Object> newBuilder() {
    return new CacheBuilder<Object, Object>();
  }

  private boolean useNullCache() {
    return (nullRemovalCause == null);
  }

  /**
   * Sets a custom {@code Equivalence} strategy for comparing keys.
   *
   * <p>By default, the cache uses {@link Equivalences#identity} to determine key equality when
   * {@link #weakKeys} is specified, and {@link Equivalences#equals()} otherwise.
   */
  CacheBuilder<K, V> keyEquivalence(Equivalence<Object> equivalence) {
    checkState(keyEquivalence == null, "key equivalence was already set to %s", keyEquivalence);
    keyEquivalence = checkNotNull(equivalence);
    return this;
  }

  Equivalence<Object> getKeyEquivalence() {
    return firstNonNull(keyEquivalence, getKeyStrength().defaultEquivalence());
  }

  /**
   * Sets a custom {@code Equivalence} strategy for comparing values.
   *
   * <p>By default, the cache uses {@link Equivalences#identity} to determine value equality when
   * {@link #weakValues} or {@link #softValues} is specified, and {@link Equivalences#equals()}
   * otherwise.
   */
  CacheBuilder<K, V> valueEquivalence(Equivalence<Object> equivalence) {
    checkState(valueEquivalence == null,
        "value equivalence was already set to %s", valueEquivalence);
    this.valueEquivalence = checkNotNull(equivalence);
    return this;
  }

  Equivalence<Object> getValueEquivalence() {
    return firstNonNull(valueEquivalence, getValueStrength().defaultEquivalence());
  }

  /**
   * Sets the minimum total size for the internal hash tables. For example, if the initial capacity
   * is {@code 60}, and the concurrency level is {@code 8}, then eight segments are created, each
   * having a hash table of size eight. Providing a large enough estimate at construction time
   * avoids the need for expensive resizing operations later, but setting this value unnecessarily
   * high wastes memory.
   *
   * @throws IllegalArgumentException if {@code initialCapacity} is negative
   * @throws IllegalStateException if an initial capacity was already set
   */
  public CacheBuilder<K, V> initialCapacity(int initialCapacity) {
    checkState(this.initialCapacity == UNSET_INT, "initial capacity was already set to %s",
        this.initialCapacity);
    checkArgument(initialCapacity >= 0);
    this.initialCapacity = initialCapacity;
    return this;
  }

  int getInitialCapacity() {
    return (initialCapacity == UNSET_INT) ? DEFAULT_INITIAL_CAPACITY : initialCapacity;
  }

  /**
   * Guides the allowed concurrency among update operations. Used as a hint for internal sizing. The
   * table is internally partitioned to try to permit the indicated number of concurrent updates
   * without contention. Because assignment of entries to these partitions is not necessarily
   * uniform, the actual concurrency observed may vary. Ideally, you should choose a value to
   * accommodate as many threads as will ever concurrently modify the table. Using a significantly
   * higher value than you need can waste space and time, and a significantly lower value can lead
   * to thread contention. But overestimates and underestimates within an order of magnitude do not
   * usually have much noticeable impact. A value of one permits only one thread to modify the cache
   * at a time, but since read operations can proceed concurrently, this still yields higher
   * concurrency than full synchronization. Defaults to 4.
   *
   * <p><b>Note:</b>The default may change in the future. If you care about this value, you should
   * always choose it explicitly.
   *
   * @throws IllegalArgumentException if {@code concurrencyLevel} is nonpositive
   * @throws IllegalStateException if a concurrency level was already set
   */
  public CacheBuilder<K, V> concurrencyLevel(int concurrencyLevel) {
    checkState(this.concurrencyLevel == UNSET_INT, "concurrency level was already set to %s",
        this.concurrencyLevel);
    checkArgument(concurrencyLevel > 0);
    this.concurrencyLevel = concurrencyLevel;
    return this;
  }

  int getConcurrencyLevel() {
    return (concurrencyLevel == UNSET_INT) ? DEFAULT_CONCURRENCY_LEVEL : concurrencyLevel;
  }

  /**
   * Specifies the maximum number of entries the cache may contain. Note that the cache <b>may evict
   * an entry before this limit is exceeded</b>. As the cache size grows close to the maximum, the
   * cache evicts entries that are less likely to be used again. For example, the cache may evict an
   * entry because it hasn't been used recently or very often.
   *
   * <p>When {@code size} is zero, elements will be evicted immediately after being loaded into the
   * cache. This has the same effect as invoking {@link #expireAfterWrite
   * expireAfterWrite}{@code (0, unit)} or {@link #expireAfterAccess expireAfterAccess}{@code (0,
   * unit)}. It can be useful in testing, or to disable caching temporarily without a code change.
   *
   * @param size the maximum size of the cache
   * @throws IllegalArgumentException if {@code size} is negative
   * @throws IllegalStateException if a maximum size was already set
   */
  public CacheBuilder<K, V> maximumSize(int size) {
    checkState(this.maximumSize == UNSET_INT, "maximum size was already set to %s",
        this.maximumSize);
    checkArgument(size >= 0, "maximum size must not be negative");
    this.maximumSize = size;
    if (maximumSize == 0) {
      // SIZE trumps EXPIRED
      this.nullRemovalCause = RemovalCause.SIZE;
    }
    return this;
  }

  /**
   * Specifies that each key (not value) stored in the cache should be strongly referenced.
   *
   * @throws IllegalStateException if the key strength was already set
   */
  CacheBuilder<K, V> strongKeys() {
    return setKeyStrength(Strength.STRONG);
  }

  /**
   * Specifies that each key (not value) stored in the cache should be wrapped in a {@link
   * WeakReference} (by default, strong references are used).
   *
   * <p><b>Warning:</b> when this method is used, the resulting cache will use identity ({@code ==})
   * comparison to determine equality of keys.
   *
   * <p>Entries with keys that have been garbage collected may be counted by {@link Cache#size}, but
   * will never be visible to read or write operations. Entries with garbage collected keys are
   * cleaned up as part of the routine maintenance described in the class javadoc.
   *
   * @throws IllegalStateException if the key strength was already set
   */
  public CacheBuilder<K, V> weakKeys() {
    return setKeyStrength(Strength.WEAK);
  }

  CacheBuilder<K, V> setKeyStrength(Strength strength) {
    checkState(keyStrength == null, "Key strength was already set to %s", keyStrength);
    keyStrength = checkNotNull(strength);
    return this;
  }

  Strength getKeyStrength() {
    return firstNonNull(keyStrength, Strength.STRONG);
  }

  /**
   * Specifies that each value (not key) stored in the cache should be strongly referenced.
   *
   * @throws IllegalStateException if the value strength was already set
   */
  CacheBuilder<K, V> strongValues() {
    return setValueStrength(Strength.STRONG);
  }

  /**
   * Specifies that each value (not key) stored in the cache should be wrapped in a
   * {@link WeakReference} (by default, strong references are used).
   *
   * <p>Weak values will be garbage collected once they are weakly reachable. This makes them a poor
   * candidate for caching; consider {@link #softValues} instead.
   *
   * <p><b>Note:</b> when this method is used, the resulting cache will use identity ({@code ==})
   * comparison to determine equality of values.
   *
   * <p>Entries with values that have been garbage collected may be counted by {@link Cache#size},
   * but will never be visible to read or write operations. Entries with garbage collected keys are
   * cleaned up as part of the routine maintenance described in the class javadoc.
   *
   * @throws IllegalStateException if the value strength was already set
   */
  public CacheBuilder<K, V> weakValues() {
    return setValueStrength(Strength.WEAK);
  }

  /**
   * Specifies that each value (not key) stored in the cache should be wrapped in a
   * {@link SoftReference} (by default, strong references are used). Softly-referenced objects will
   * be garbage-collected in a <i>globally</i> least-recently-used manner, in response to memory
   * demand.
   *
   * <p><b>Warning:</b> in most circumstances it is better to set a per-cache {@linkplain
   * #maximumSize maximum size} instead of using soft references. You should only use this method if
   * you are well familiar with the practical consequences of soft references.
   *
   * <p><b>Note:</b> when this method is used, the resulting cache will use identity ({@code ==})
   * comparison to determine equality of values.
   *
   * <p>Entries with values that have been garbage collected may be counted by {@link Cache#size},
   * but will never be visible to read or write operations. Entries with garbage collected values
   * are cleaned up as part of the routine maintenance described in the class javadoc.
   *
   * @throws IllegalStateException if the value strength was already set
   */
  public CacheBuilder<K, V> softValues() {
    return setValueStrength(Strength.SOFT);
  }

  CacheBuilder<K, V> setValueStrength(Strength strength) {
    checkState(valueStrength == null, "Value strength was already set to %s", valueStrength);
    valueStrength = checkNotNull(strength);
    return this;
  }

  Strength getValueStrength() {
    return firstNonNull(valueStrength, Strength.STRONG);
  }

  /**
   * Specifies that each entry should be automatically removed from the cache once a fixed duration
   * has elapsed after the entry's creation, or the most recent replacement of its value.
   *
   * <p>When {@code duration} is zero, elements will be evicted immediately after being loaded into
   * the cache. This has the same effect as invoking {@link #maximumSize maximumSize}{@code (0)}. It
   * can be useful in testing, or to disable caching temporarily without a code change.
   *
   * <p>Expired entries may be counted by {@link Cache#size}, but will never be visible to read or
   * write operations. Expired entries are cleaned up as part of the routine maintenance described
   * in the class javadoc.
   *
   * @param duration the length of time after an entry is created that it should be automatically
   *     removed
   * @param unit the unit that {@code duration} is expressed in
   * @throws IllegalArgumentException if {@code duration} is negative
   * @throws IllegalStateException if the time to live or time to idle was already set
   */
  public CacheBuilder<K, V> expireAfterWrite(long duration, TimeUnit unit) {
    checkExpiration(duration, unit);
    this.expireAfterWriteNanos = unit.toNanos(duration);
    if (duration == 0 && this.nullRemovalCause == null) {
      // SIZE trumps EXPIRED
      this.nullRemovalCause = RemovalCause.EXPIRED;
    }
    return this;
  }

  private void checkExpiration(long duration, TimeUnit unit) {
    checkState(expireAfterWriteNanos == UNSET_INT, "expireAfterWrite was already set to %s ns",
        expireAfterWriteNanos);
    checkState(expireAfterAccessNanos == UNSET_INT, "expireAfterAccess was already set to %s ns",
        expireAfterAccessNanos);
    checkArgument(duration >= 0, "duration cannot be negative: %s %s", duration, unit);
  }

  long getExpireAfterWriteNanos() {
    return (expireAfterWriteNanos == UNSET_INT) ? DEFAULT_EXPIRATION_NANOS : expireAfterWriteNanos;
  }

  /**
   * Specifies that each entry should be automatically removed from the cache once a fixed duration
   * has elapsed after the entry's creation, or last access. Access time is reset by
   * {@link Cache#get} and {@link Cache#getUnchecked}, but not by operations on the view returned by
   * {@link Cache#asMap}.
   *
   * <p>When {@code duration} is zero, elements will be evicted immediately after being loaded into
   * the cache. This has the same effect as invoking {@link #maximumSize maximumSize}{@code (0)}. It
   * can be useful in testing, or to disable caching temporarily without a code change.
   *
   * <p>Expired entries may be counted by {@link Cache#size}, but will never be visible to read or
   * write operations. Expired entries are cleaned up as part of the routine maintenance described
   * in the class javadoc.
   *
   * @param duration the length of time after an entry is last accessed that it should be
   *     automatically removed
   * @param unit the unit that {@code duration} is expressed in
   * @throws IllegalArgumentException if {@code duration} is negative
   * @throws IllegalStateException if the time to idle or time to live was already set
   */
  public CacheBuilder<K, V> expireAfterAccess(long duration, TimeUnit unit) {
    checkExpiration(duration, unit);
    this.expireAfterAccessNanos = unit.toNanos(duration);
    if (duration == 0 && this.nullRemovalCause == null) {
      // SIZE trumps EXPIRED
      this.nullRemovalCause = RemovalCause.EXPIRED;
    }
    return this;
  }

  long getExpireAfterAccessNanos() {
    return (expireAfterAccessNanos == UNSET_INT)
        ? DEFAULT_EXPIRATION_NANOS : expireAfterAccessNanos;
  }

  /**
   * Specifies a nanosecond-precision time source for use in determining when entries should be
   * expired. By default, {@link System#nanoTime} is used.
   *
   * <p>The primary intent of this method is to facilitate testing of caches which have been
   * configured with {@link #expireAfterWrite} or {@link #expireAfterAccess}.
   *
   * @throws IllegalStateException if a ticker was already set
   */
  public CacheBuilder<K, V> ticker(Ticker ticker) {
    checkState(this.ticker == null);
    this.ticker = checkNotNull(ticker);
    return this;
  }

  Ticker getTicker() {
    return firstNonNull(ticker, Ticker.systemTicker());
  }

  /**
   * Specifies a listener instance, which all caches built using this {@code CacheBuilder} will
   * notify each time an entry is removed from the cache by any means.
   *
   * <p>Each cache built by this {@code CacheBuilder} after this method is called invokes the
   * supplied listener after removing an element for any reason (see removal causes in {@link
   * RemovalCause}). It will invoke the listener as part of the routine maintenance described
   * in the class javadoc.
   *
   * <p><b>Important note:</b> Instead of returning <em>this</em> as a {@code CacheBuilder}
   * instance, this method returns {@code CacheBuilder<K1, V1>}. From this point on, either the
   * original reference or the returned reference may be used to complete configuration and build
   * the cache, but only the "generic" one is type-safe. That is, it will properly prevent you from
   * building caches whose key or value types are incompatible with the types accepted by the
   * listener already provided; the {@code CacheBuilder} type cannot do this. For best results,
   * simply use the standard method-chaining idiom, as illustrated in the documentation at top,
   * configuring a {@code CacheBuilder} and building your {@link Cache} all in a single statement.
   *
   * <p><b>Warning:</b> if you ignore the above advice, and use this {@code CacheBuilder} to build
   * a cache whose key or value type is incompatible with the listener, you will likely experience
   * a {@link ClassCastException} at some <i>undefined</i> point in the future.
   *
   * @throws IllegalStateException if a removal listener was already set
   */
  @CheckReturnValue
  public <K1 extends K, V1 extends V> CacheBuilder<K1, V1> removalListener(
      RemovalListener<? super K1, ? super V1> listener) {
    checkState(this.removalListener == null);

    // safely limiting the kinds of caches this can produce
    @SuppressWarnings("unchecked")
    CacheBuilder<K1, V1> me = (CacheBuilder<K1, V1>) this;
    me.removalListener = checkNotNull(listener);
    return me;
  }

  // Make a safe contravariant cast now so we don't have to do it over and over.
  @SuppressWarnings("unchecked")
  <K1 extends K, V1 extends V> RemovalListener<K1, V1> getRemovalListener() {
    return (RemovalListener<K1, V1>) Objects.firstNonNull(removalListener, NullListener.INSTANCE);
  }

  /**
   * Builds a cache, which either returns an already-loaded value for a given key or atomically
   * computes or retrieves it using the supplied {@code CacheLoader}. If another thread is currently
   * loading the value for this key, simply waits for that thread to finish and returns its
   * loaded value. Note that multiple threads can concurrently load values for distinct keys.
   *
   * <p>This method does not alter the state of this {@code CacheBuilder} instance, so it can be
   * invoked again to create multiple independent caches.
   *
   * @param loader the cache loader used to obtain new values
   * @return a cache having the requested features
   */
  public <K1 extends K, V1 extends V> Cache<K1, V1> build(CacheLoader<? super K1, V1> loader) {
    return useNullCache()
        ? new ComputingCache<K1, V1>(this, CACHE_STATS_COUNTER, loader)
        : new NullCache<K1, V1>(this, CACHE_STATS_COUNTER, loader);
  }

  /**
   * Returns a string representation for this CacheBuilder instance. The exact form of the returned
   * string is not specificed.
   */
  @Override
  public String toString() {
    Objects.ToStringHelper s = Objects.toStringHelper(this);
    if (initialCapacity != UNSET_INT) {
      s.add("initialCapacity", initialCapacity);
    }
    if (concurrencyLevel != UNSET_INT) {
      s.add("concurrencyLevel", concurrencyLevel);
    }
    if (maximumSize != UNSET_INT) {
      s.add("maximumSize", maximumSize);
    }
    if (expireAfterWriteNanos != UNSET_INT) {
      s.add("expireAfterWrite", expireAfterWriteNanos + "ns");
    }
    if (expireAfterAccessNanos != UNSET_INT) {
      s.add("expireAfterAccess", expireAfterAccessNanos + "ns");
    }
    if (keyStrength != null) {
      s.add("keyStrength", Ascii.toLowerCase(keyStrength.toString()));
    }
    if (valueStrength != null) {
      s.add("valueStrength", Ascii.toLowerCase(valueStrength.toString()));
    }
    if (keyEquivalence != null) {
      s.addValue("keyEquivalence");
    }
    if (valueEquivalence != null) {
      s.addValue("valueEquivalence");
    }
    if (removalListener != null) {
      s.addValue("removalListener");
    }
    return s.toString();
  }

  /** A map that is always empty and evicts on insertion. */
  static class NullConcurrentMap<K, V> extends AbstractMap<K, V>
      implements ConcurrentMap<K, V>, Serializable {
    private static final long serialVersionUID = 0;

    private final RemovalListener<K, V> removalListener;
    private final RemovalCause removalCause;

    NullConcurrentMap(CacheBuilder<? super K, ? super V> builder) {
      removalListener = builder.getRemovalListener();
      removalCause = builder.nullRemovalCause;
    }

    // implements ConcurrentMap

    @Override
    public boolean containsKey(@Nullable Object key) {
      return false;
    }

    @Override
    public boolean containsValue(@Nullable Object value) {
      return false;
    }

    @Override
    public V get(@Nullable Object key) {
      return null;
    }

    void notifyRemoval(K key, V value) {
      RemovalNotification<K, V> notification =
          new RemovalNotification<K, V>(key, value, removalCause);
      removalListener.onRemoval(notification);
    }

    @Override
    public V put(K key, V value) {
      checkNotNull(key);
      checkNotNull(value);
      notifyRemoval(key, value);
      return null;
    }

    @Override
    public V putIfAbsent(K key, V value) {
      return put(key, value);
    }

    @Override
    public V remove(@Nullable Object key) {
      return null;
    }

    @Override
    public boolean remove(@Nullable Object key, @Nullable Object value) {
      return false;
    }

    @Override
    public V replace(K key, V value) {
      checkNotNull(key);
      checkNotNull(value);
      return null;
    }

    @Override
    public boolean replace(K key, @Nullable V oldValue, V newValue) {
      checkNotNull(key);
      checkNotNull(newValue);
      return false;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
      return Collections.emptySet();
    }
  }

  // TODO(fry): remove, as no code path can hit this
  /** Computes on retrieval and evicts the result. */
  static final class NullComputingConcurrentMap<K, V> extends NullConcurrentMap<K, V> {
    private static final long serialVersionUID = 0;

    final CacheLoader<? super K, ? extends V> loader;

    NullComputingConcurrentMap(CacheBuilder<? super K, ? super V> builder,
        CacheLoader<? super K, ? extends V> loader) {
      super(builder);
      this.loader = checkNotNull(loader);
    }

    @SuppressWarnings("unchecked") // unsafe, which is why Cache is preferred
    @Override
    public V get(Object k) {
      K key = (K) k;
      V value = compute(key);
      checkNotNull(value, loader + " returned null for key " + key + ".");
      notifyRemoval(key, value);
      return value;
    }

    private V compute(K key) {
      checkNotNull(key);
      try {
        return loader.load(key);
      } catch (Exception e) {
        throw new UncheckedExecutionException(e);
      } catch (Error e) {
        throw new ExecutionError(e);
      }
    }
  }

  /** Computes on retrieval and evicts the result. */
  static final class NullCache<K, V> extends AbstractCache<K, V> {
    final NullConcurrentMap<K, V> map;
    final CacheLoader<? super K, V> loader;

    final StatsCounter statsCounter;

    NullCache(CacheBuilder<? super K, ? super V> builder,
        Supplier<? extends StatsCounter> statsCounterSupplier,
        CacheLoader<? super K, V> loader) {
      this.map = new NullConcurrentMap<K, V>(builder);
      this.statsCounter = statsCounterSupplier.get();
      this.loader = checkNotNull(loader);
    }

    @Override
    public V get(K key) throws ExecutionException {
      V value = compute(key);
      map.notifyRemoval(key, value);
      return value;
    }

    private V compute(K key) throws ExecutionException {
      checkNotNull(key);
      long start = System.nanoTime();
      V value = null;
      try {
        value = loader.load(key);
      } catch (RuntimeException e) {
        throw new UncheckedExecutionException(e);
      } catch (Exception e) {
        throw new ExecutionException(e);
      } catch (Error e) {
        throw new ExecutionError(e);
      } finally {
        long elapsed = System.nanoTime() - start;
        if (value == null) {
          statsCounter.recordLoadException(elapsed);
        } else {
          statsCounter.recordLoadSuccess(elapsed);
        }
        statsCounter.recordEviction();
      }
      if (value == null) {
        throw new NullPointerException();
      } else {
        return value;
      }
    }

    @Override
    public long size() {
      return 0;
    }

    @Override
    public void invalidate(Object key) {
      // no-op
    }

    @Override public void invalidateAll() {
      // no-op
    }

    @Override
    public CacheStats stats() {
      return statsCounter.snapshot();
    }

    ConcurrentMap<K, V> asMap;

    @Override
    public ConcurrentMap<K, V> asMap() {
      ConcurrentMap<K, V> am = asMap;
      return (am != null) ? am : (asMap = new CacheAsMap<K, V>(map));
    }
  }

  static final class CacheAsMap<K, V> extends ForwardingConcurrentMap<K, V> {
    private final ConcurrentMap<K, V> delegate;

    CacheAsMap(ConcurrentMap<K, V> delegate) {
      this.delegate = delegate;
    }

    @Override
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

/*
 * Copyright (C) 2009 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Ascii;
import com.google.common.base.Equivalence;
import com.google.common.base.MoreObjects;
import com.google.common.collect.MapMakerInternalMap.Strength;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.lang.ref.WeakReference;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/**
 * A builder of {@link ConcurrentMap} instances that can have keys or values automatically wrapped
 * in {@linkplain WeakReference weak} references.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * ConcurrentMap<Request, Stopwatch> timers = new MapMaker()
 *     .concurrencyLevel(4)
 *     .weakKeys()
 *     .makeMap();
 * }</pre>
 *
 * <p>These features are all optional; {@code new MapMaker().makeMap()} returns a valid concurrent
 * map that behaves similarly to a {@link ConcurrentHashMap}.
 *
 * <p>The returned map is implemented as a hash table with similar performance characteristics to
 * {@link ConcurrentHashMap}. It supports all optional operations of the {@code ConcurrentMap}
 * interface. It does not permit null keys or values.
 *
 * <p><b>Note:</b> by default, the returned map uses equality comparisons (the {@link Object#equals
 * equals} method) to determine equality for keys or values. However, if {@link #weakKeys} was
 * specified, the map uses identity ({@code ==}) comparisons instead for keys. Likewise, if {@link
 * #weakValues} was specified, the map uses identity comparisons for values.
 *
 * <p>The view collections of the returned map have <i>weakly consistent iterators</i>. This means
 * that they are safe for concurrent use, but if other threads modify the map after the iterator is
 * created, it is undefined which of these changes, if any, are reflected in that iterator. These
 * iterators never throw {@link ConcurrentModificationException}.
 *
 * <p>If {@link #weakKeys} or {@link #weakValues} are requested, it is possible for a key or value
 * present in the map to be reclaimed by the garbage collector. Entries with reclaimed keys or
 * values may be removed from the map on each map modification or on occasional map accesses; such
 * entries may be counted by {@link Map#size}, but will never be visible to read or write
 * operations. A partially-reclaimed entry is never exposed to the user. Any {@link java.util.Entry}
 * instance retrieved from the map's {@linkplain Map#entrySet entry set} is a snapshot of that
 * entry's state at the time of retrieval; such entries do, however, support {@link
 * java.util.Entry#setValue}, which simply calls {@link Map#put} on the entry's key.
 *
 * <p>The maps produced by {@code MapMaker} are serializable, and the deserialized maps retain all
 * the configuration properties of the original map. During deserialization, if the original map had
 * used weak references, the entries are reconstructed as they were, but it's not unlikely they'll
 * be quickly garbage-collected before they are ever accessed.
 *
 * <p>{@code new MapMaker().weakKeys().makeMap()} is a recommended replacement for {@link
 * java.util.WeakHashMap}, but note that it compares keys using object identity whereas {@code
 * WeakHashMap} uses {@link Object#equals}.
 *
 * @author Bob Lee
 * @author Charles Fry
 * @author Kevin Bourrillion
 * @since 2.0
 */
@GwtCompatible(emulated = true)
public final class MapMaker {
  private static final int DEFAULT_INITIAL_CAPACITY = 16;
  private static final int DEFAULT_CONCURRENCY_LEVEL = 4;

  static final int UNSET_INT = -1;

  // TODO(kevinb): dispense with this after benchmarking
  boolean useCustomMap;

  int initialCapacity = UNSET_INT;
  int concurrencyLevel = UNSET_INT;

  @MonotonicNonNull Strength keyStrength;
  @MonotonicNonNull Strength valueStrength;

  @MonotonicNonNull Equivalence<Object> keyEquivalence;

  /**
   * Constructs a new {@code MapMaker} instance with default settings, including strong keys, strong
   * values, and no automatic eviction of any kind.
   */
  public MapMaker() {}

  /**
   * Sets a custom {@code Equivalence} strategy for comparing keys.
   *
   * <p>By default, the map uses {@link Equivalence#identity} to determine key equality when {@link
   * #weakKeys} is specified, and {@link Equivalence#equals()} otherwise. The only place this is
   * used is in {@link Interners.WeakInterner}.
   */
  @CanIgnoreReturnValue
  @GwtIncompatible // To be supported
  MapMaker keyEquivalence(Equivalence<Object> equivalence) {
    checkState(keyEquivalence == null, "key equivalence was already set to %s", keyEquivalence);
    keyEquivalence = checkNotNull(equivalence);
    this.useCustomMap = true;
    return this;
  }

  Equivalence<Object> getKeyEquivalence() {
    return MoreObjects.firstNonNull(keyEquivalence, getKeyStrength().defaultEquivalence());
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
  @CanIgnoreReturnValue
  public MapMaker initialCapacity(int initialCapacity) {
    checkState(
        this.initialCapacity == UNSET_INT,
        "initial capacity was already set to %s",
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
   * usually have much noticeable impact. A value of one permits only one thread to modify the map
   * at a time, but since read operations can proceed concurrently, this still yields higher
   * concurrency than full synchronization. Defaults to 4.
   *
   * <p><b>Note:</b> Prior to Guava release 9.0, the default was 16. It is possible the default will
   * change again in the future. If you care about this value, you should always choose it
   * explicitly.
   *
   * @throws IllegalArgumentException if {@code concurrencyLevel} is nonpositive
   * @throws IllegalStateException if a concurrency level was already set
   */
  @CanIgnoreReturnValue
  public MapMaker concurrencyLevel(int concurrencyLevel) {
    checkState(
        this.concurrencyLevel == UNSET_INT,
        "concurrency level was already set to %s",
        this.concurrencyLevel);
    checkArgument(concurrencyLevel > 0);
    this.concurrencyLevel = concurrencyLevel;
    return this;
  }

  int getConcurrencyLevel() {
    return (concurrencyLevel == UNSET_INT) ? DEFAULT_CONCURRENCY_LEVEL : concurrencyLevel;
  }

  /**
   * Specifies that each key (not value) stored in the map should be wrapped in a {@link
   * WeakReference} (by default, strong references are used).
   *
   * <p><b>Warning:</b> when this method is used, the resulting map will use identity ({@code ==})
   * comparison to determine equality of keys, which is a technical violation of the {@link Map}
   * specification, and may not be what you expect.
   *
   * @throws IllegalStateException if the key strength was already set
   * @see WeakReference
   */
  @CanIgnoreReturnValue
  @GwtIncompatible // java.lang.ref.WeakReference
  public MapMaker weakKeys() {
    return setKeyStrength(Strength.WEAK);
  }

  MapMaker setKeyStrength(Strength strength) {
    checkState(keyStrength == null, "Key strength was already set to %s", keyStrength);
    keyStrength = checkNotNull(strength);
    if (strength != Strength.STRONG) {
      // STRONG could be used during deserialization.
      useCustomMap = true;
    }
    return this;
  }

  Strength getKeyStrength() {
    return MoreObjects.firstNonNull(keyStrength, Strength.STRONG);
  }

  /**
   * Specifies that each value (not key) stored in the map should be wrapped in a {@link
   * WeakReference} (by default, strong references are used).
   *
   * <p>Weak values will be garbage collected once they are weakly reachable. This makes them a poor
   * candidate for caching.
   *
   * <p><b>Warning:</b> when this method is used, the resulting map will use identity ({@code ==})
   * comparison to determine equality of values. This technically violates the specifications of the
   * methods {@link Map#containsValue containsValue}, {@link ConcurrentMap#remove(Object, Object)
   * remove(Object, Object)} and {@link ConcurrentMap#replace(Object, Object, Object) replace(K, V,
   * V)}, and may not be what you expect.
   *
   * @throws IllegalStateException if the value strength was already set
   * @see WeakReference
   */
  @CanIgnoreReturnValue
  @GwtIncompatible // java.lang.ref.WeakReference
  public MapMaker weakValues() {
    return setValueStrength(Strength.WEAK);
  }

  /**
   * A dummy singleton value type used by {@link Interners}.
   *
   * <p>{@link MapMakerInternalMap} can optimize for memory usage in this case; see {@link
   * MapMakerInternalMap#createWithDummyValues}.
   */
  enum Dummy {
    VALUE
  }

  MapMaker setValueStrength(Strength strength) {
    checkState(valueStrength == null, "Value strength was already set to %s", valueStrength);
    valueStrength = checkNotNull(strength);
    if (strength != Strength.STRONG) {
      // STRONG could be used during deserialization.
      useCustomMap = true;
    }
    return this;
  }

  Strength getValueStrength() {
    return MoreObjects.firstNonNull(valueStrength, Strength.STRONG);
  }

  /**
   * Builds a thread-safe map. This method does not alter the state of this {@code MapMaker}
   * instance, so it can be invoked again to create multiple independent maps.
   *
   * <p>The bulk operations {@code putAll}, {@code equals}, and {@code clear} are not guaranteed to
   * be performed atomically on the returned map. Additionally, {@code size} and {@code
   * containsValue} are implemented as bulk read operations, and thus may fail to observe concurrent
   * writes.
   *
   * @return a serializable concurrent map having the requested features
   */
  public <K, V> ConcurrentMap<K, V> makeMap() {
    if (!useCustomMap) {
      return new ConcurrentHashMap<>(getInitialCapacity(), 0.75f, getConcurrencyLevel());
    }
    return MapMakerInternalMap.create(this);
  }

  /**
   * Returns a string representation for this MapMaker instance. The exact form of the returned
   * string is not specified.
   */
  @Override
  public String toString() {
    MoreObjects.ToStringHelper s = MoreObjects.toStringHelper(this);
    if (initialCapacity != UNSET_INT) {
      s.add("initialCapacity", initialCapacity);
    }
    if (concurrencyLevel != UNSET_INT) {
      s.add("concurrencyLevel", concurrencyLevel);
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
    return s.toString();
  }
}

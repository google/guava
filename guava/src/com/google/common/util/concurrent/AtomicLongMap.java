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

package com.google.common.util.concurrent;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/**
 * A map containing {@code long} values that can be atomically updated. While writes to a
 * traditional {@code Map} rely on {@code put(K, V)}, the typical mechanism for writing to this map
 * is {@code addAndGet(K, long)}, which adds a {@code long} to the value currently associated with
 * {@code K}. If a key has not yet been associated with a value, its implicit value is zero.
 *
 * <p>Most methods in this class treat absent values and zero values identically, as individually
 * documented. Exceptions to this are {@link #containsKey}, {@link #size}, {@link #isEmpty}, {@link
 * #asMap}, and {@link #toString}.
 *
 * <p>Instances of this class may be used by multiple threads concurrently. All operations are
 * atomic unless otherwise noted.
 *
 * <p><b>Note:</b> If your values are always positive and less than 2^31, you may wish to use a
 * {@link com.google.common.collect.Multiset} such as {@link
 * com.google.common.collect.ConcurrentHashMultiset} instead.
 *
 * <p><b>Warning:</b> Unlike {@code Multiset}, entries whose values are zero are not automatically
 * removed from the map. Instead they must be removed manually with {@link #removeAllZeros}.
 *
 * @author Charles Fry
 * @since 11.0
 */
@GwtCompatible
public final class AtomicLongMap<K> implements Serializable {
  private final ConcurrentHashMap<K, Long> map;

  private AtomicLongMap(ConcurrentHashMap<K, Long> map) {
    this.map = checkNotNull(map);
  }

  /** Creates an {@code AtomicLongMap}. */
  public static <K> AtomicLongMap<K> create() {
    return new AtomicLongMap<K>(new ConcurrentHashMap<>());
  }

  /** Creates an {@code AtomicLongMap} with the same mappings as the specified {@code Map}. */
  public static <K> AtomicLongMap<K> create(Map<? extends K, ? extends Long> m) {
    AtomicLongMap<K> result = create();
    result.putAll(m);
    return result;
  }

  /**
   * Returns the value associated with {@code key}, or zero if there is no value associated with
   * {@code key}.
   */
  public long get(K key) {
    return map.getOrDefault(key, 0L);
  }

  /**
   * Increments by one the value currently associated with {@code key}, and returns the new value.
   */
  @CanIgnoreReturnValue
  public long incrementAndGet(K key) {
    return addAndGet(key, 1);
  }

  /**
   * Decrements by one the value currently associated with {@code key}, and returns the new value.
   */
  @CanIgnoreReturnValue
  public long decrementAndGet(K key) {
    return addAndGet(key, -1);
  }

  /**
   * Adds {@code delta} to the value currently associated with {@code key}, and returns the new
   * value.
   */
  @CanIgnoreReturnValue
  public long addAndGet(K key, long delta) {
    return accumulateAndGet(key, delta, Long::sum);
  }

  /**
   * Increments by one the value currently associated with {@code key}, and returns the old value.
   */
  @CanIgnoreReturnValue
  public long getAndIncrement(K key) {
    return getAndAdd(key, 1);
  }

  /**
   * Decrements by one the value currently associated with {@code key}, and returns the old value.
   */
  @CanIgnoreReturnValue
  public long getAndDecrement(K key) {
    return getAndAdd(key, -1);
  }

  /**
   * Adds {@code delta} to the value currently associated with {@code key}, and returns the old
   * value.
   */
  @CanIgnoreReturnValue
  public long getAndAdd(K key, long delta) {
    return getAndAccumulate(key, delta, Long::sum);
  }

  /**
   * Updates the value currently associated with {@code key} with the specified function, and
   * returns the new value. If there is not currently a value associated with {@code key}, the
   * function is applied to {@code 0L}.
   *
   * @since 21.0
   */
  @CanIgnoreReturnValue
  public long updateAndGet(K key, LongUnaryOperator updaterFunction) {
    checkNotNull(updaterFunction);
    return map.compute(
        key, (k, value) -> updaterFunction.applyAsLong((value == null) ? 0L : value.longValue()));
  }

  /**
   * Updates the value currently associated with {@code key} with the specified function, and
   * returns the old value. If there is not currently a value associated with {@code key}, the
   * function is applied to {@code 0L}.
   *
   * @since 21.0
   */
  @CanIgnoreReturnValue
  public long getAndUpdate(K key, LongUnaryOperator updaterFunction) {
    checkNotNull(updaterFunction);
    AtomicLong holder = new AtomicLong();
    map.compute(
        key,
        (k, value) -> {
          long oldValue = (value == null) ? 0L : value.longValue();
          holder.set(oldValue);
          return updaterFunction.applyAsLong(oldValue);
        });
    return holder.get();
  }

  /**
   * Updates the value currently associated with {@code key} by combining it with {@code x} via the
   * specified accumulator function, returning the new value. The previous value associated with
   * {@code key} (or zero, if there is none) is passed as the first argument to {@code
   * accumulatorFunction}, and {@code x} is passed as the second argument.
   *
   * @since 21.0
   */
  @CanIgnoreReturnValue
  public long accumulateAndGet(K key, long x, LongBinaryOperator accumulatorFunction) {
    checkNotNull(accumulatorFunction);
    return updateAndGet(key, oldValue -> accumulatorFunction.applyAsLong(oldValue, x));
  }

  /**
   * Updates the value currently associated with {@code key} by combining it with {@code x} via the
   * specified accumulator function, returning the old value. The previous value associated with
   * {@code key} (or zero, if there is none) is passed as the first argument to {@code
   * accumulatorFunction}, and {@code x} is passed as the second argument.
   *
   * @since 21.0
   */
  @CanIgnoreReturnValue
  public long getAndAccumulate(K key, long x, LongBinaryOperator accumulatorFunction) {
    checkNotNull(accumulatorFunction);
    return getAndUpdate(key, oldValue -> accumulatorFunction.applyAsLong(oldValue, x));
  }

  /**
   * Associates {@code newValue} with {@code key} in this map, and returns the value previously
   * associated with {@code key}, or zero if there was no such value.
   */
  @CanIgnoreReturnValue
  public long put(K key, long newValue) {
    return getAndUpdate(key, x -> newValue);
  }

  /**
   * Copies all of the mappings from the specified map to this map. The effect of this call is
   * equivalent to that of calling {@code put(k, v)} on this map once for each mapping from key
   * {@code k} to value {@code v} in the specified map. The behavior of this operation is undefined
   * if the specified map is modified while the operation is in progress.
   */
  public void putAll(Map<? extends K, ? extends Long> m) {
    m.forEach(this::put);
  }

  /**
   * Removes and returns the value associated with {@code key}. If {@code key} is not in the map,
   * this method has no effect and returns zero.
   */
  @CanIgnoreReturnValue
  public long remove(K key) {
    Long result = map.remove(key);
    return (result == null) ? 0L : result.longValue();
  }

  /**
   * If {@code (key, value)} is currently in the map, this method removes it and returns true;
   * otherwise, this method returns false.
   */
  boolean remove(K key, long value) {
    return map.remove(key, value);
  }

  /**
   * Atomically remove {@code key} from the map iff its associated value is 0.
   *
   * @since 20.0
   */
  @Beta
  @CanIgnoreReturnValue
  public boolean removeIfZero(K key) {
    return remove(key, 0);
  }

  /**
   * Removes all mappings from this map whose values are zero.
   *
   * <p>This method is not atomic: the map may be visible in intermediate states, where some of the
   * zero values have been removed and others have not.
   */
  public void removeAllZeros() {
    map.values().removeIf(x -> x == 0);
  }

  /**
   * Returns the sum of all values in this map.
   *
   * <p>This method is not atomic: the sum may or may not include other concurrent operations.
   */
  public long sum() {
    return map.values().stream().mapToLong(Long::longValue).sum();
  }

  private transient @MonotonicNonNull Map<K, Long> asMap;

  /** Returns a live, read-only view of the map backing this {@code AtomicLongMap}. */
  public Map<K, Long> asMap() {
    Map<K, Long> result = asMap;
    return (result == null) ? asMap = createAsMap() : result;
  }

  private Map<K, Long> createAsMap() {
    return Collections.unmodifiableMap(map);
  }

  /** Returns true if this map contains a mapping for the specified key. */
  public boolean containsKey(Object key) {
    return map.containsKey(key);
  }

  /**
   * Returns the number of key-value mappings in this map. If the map contains more than {@code
   * Integer.MAX_VALUE} elements, returns {@code Integer.MAX_VALUE}.
   */
  public int size() {
    return map.size();
  }

  /** Returns {@code true} if this map contains no key-value mappings. */
  public boolean isEmpty() {
    return map.isEmpty();
  }

  /**
   * Removes all of the mappings from this map. The map will be empty after this call returns.
   *
   * <p>This method is not atomic: the map may not be empty after returning if there were concurrent
   * writes.
   */
  public void clear() {
    map.clear();
  }

  @Override
  public String toString() {
    return map.toString();
  }

  /**
   * If {@code key} is not already associated with a value or if {@code key} is associated with
   * zero, associate it with {@code newValue}. Returns the previous value associated with {@code
   * key}, or zero if there was no mapping for {@code key}.
   */
  long putIfAbsent(K key, long newValue) {
    AtomicBoolean noValue = new AtomicBoolean(false);
    Long result =
        map.compute(
            key,
            (k, oldValue) -> {
              if (oldValue == null || oldValue == 0) {
                noValue.set(true);
                return newValue;
              } else {
                return oldValue;
              }
            });
    return noValue.get() ? 0L : result.longValue();
  }

  /**
   * If {@code (key, expectedOldValue)} is currently in the map, this method replaces {@code
   * expectedOldValue} with {@code newValue} and returns true; otherwise, this method returns false.
   *
   * <p>If {@code expectedOldValue} is zero, this method will succeed if {@code (key, zero)} is
   * currently in the map, or if {@code key} is not in the map at all.
   */
  boolean replace(K key, long expectedOldValue, long newValue) {
    if (expectedOldValue == 0L) {
      return putIfAbsent(key, newValue) == 0L;
    } else {
      return map.replace(key, expectedOldValue, newValue);
    }
  }
}

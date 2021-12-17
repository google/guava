/*
 * Copyright (C) 2011 The Guava Authors
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

package com.google.common.util.concurrent;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.CheckForNull;

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
@ElementTypesAreNonnullByDefault
public final class AtomicLongMap<K> implements Serializable {
  private final ConcurrentHashMap<K, AtomicLong> map;

  private AtomicLongMap(ConcurrentHashMap<K, AtomicLong> map) {
    this.map = checkNotNull(map);
  }

  /** Creates an {@code AtomicLongMap}. */
  public static <K> AtomicLongMap<K> create() {
    return new AtomicLongMap<K>(new ConcurrentHashMap<K, AtomicLong>());
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
    AtomicLong atomic = map.get(key);
    return atomic == null ? 0L : atomic.get();
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
    outer:
    while (true) {
      AtomicLong atomic = map.get(key);
      if (atomic == null) {
        atomic = map.putIfAbsent(key, new AtomicLong(delta));
        if (atomic == null) {
          return delta;
        }
        // atomic is now non-null; fall through
      }

      while (true) {
        long oldValue = atomic.get();
        if (oldValue == 0L) {
          // don't compareAndSet a zero
          if (map.replace(key, atomic, new AtomicLong(delta))) {
            return delta;
          }
          // atomic replaced
          continue outer;
        }

        long newValue = oldValue + delta;
        if (atomic.compareAndSet(oldValue, newValue)) {
          return newValue;
        }
        // value changed
      }
    }
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
    outer:
    while (true) {
      AtomicLong atomic = map.get(key);
      if (atomic == null) {
        atomic = map.putIfAbsent(key, new AtomicLong(delta));
        if (atomic == null) {
          return 0L;
        }
        // atomic is now non-null; fall through
      }

      while (true) {
        long oldValue = atomic.get();
        if (oldValue == 0L) {
          // don't compareAndSet a zero
          if (map.replace(key, atomic, new AtomicLong(delta))) {
            return 0L;
          }
          // atomic replaced
          continue outer;
        }

        long newValue = oldValue + delta;
        if (atomic.compareAndSet(oldValue, newValue)) {
          return oldValue;
        }
        // value changed
      }
    }
  }

  /**
   * Associates {@code newValue} with {@code key} in this map, and returns the value previously
   * associated with {@code key}, or zero if there was no such value.
   */
  @CanIgnoreReturnValue
  public long put(K key, long newValue) {
    outer:
    while (true) {
      AtomicLong atomic = map.get(key);
      if (atomic == null) {
        atomic = map.putIfAbsent(key, new AtomicLong(newValue));
        if (atomic == null) {
          return 0L;
        }
        // atomic is now non-null; fall through
      }

      while (true) {
        long oldValue = atomic.get();
        if (oldValue == 0L) {
          // don't compareAndSet a zero
          if (map.replace(key, atomic, new AtomicLong(newValue))) {
            return 0L;
          }
          // atomic replaced
          continue outer;
        }

        if (atomic.compareAndSet(oldValue, newValue)) {
          return oldValue;
        }
        // value changed
      }
    }
  }

  /**
   * Copies all of the mappings from the specified map to this map. The effect of this call is
   * equivalent to that of calling {@code put(k, v)} on this map once for each mapping from key
   * {@code k} to value {@code v} in the specified map. The behavior of this operation is undefined
   * if the specified map is modified while the operation is in progress.
   */
  public void putAll(Map<? extends K, ? extends Long> m) {
    for (Entry<? extends K, ? extends Long> entry : m.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Removes and returns the value associated with {@code key}. If {@code key} is not in the map,
   * this method has no effect and returns zero.
   */
  @CanIgnoreReturnValue
  public long remove(K key) {
    AtomicLong atomic = map.get(key);
    if (atomic == null) {
      return 0L;
    }

    while (true) {
      long oldValue = atomic.get();
      if (oldValue == 0L || atomic.compareAndSet(oldValue, 0L)) {
        // only remove after setting to zero, to avoid concurrent updates
        map.remove(key, atomic);
        // succeed even if the remove fails, since the value was already adjusted
        return oldValue;
      }
    }
  }

  /**
   * If {@code (key, value)} is currently in the map, this method removes it and returns true;
   * otherwise, this method returns false.
   */
  boolean remove(K key, long value) {
    AtomicLong atomic = map.get(key);
    if (atomic == null) {
      return false;
    }

    long oldValue = atomic.get();
    if (oldValue != value) {
      return false;
    }

    if (oldValue == 0L || atomic.compareAndSet(oldValue, 0L)) {
      // only remove after setting to zero, to avoid concurrent updates
      map.remove(key, atomic);
      // succeed even if the remove fails, since the value was already adjusted
      return true;
    }

    // value changed
    return false;
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
    Iterator<Entry<K, AtomicLong>> entryIterator = map.entrySet().iterator();
    while (entryIterator.hasNext()) {
      Entry<K, AtomicLong> entry = entryIterator.next();
      AtomicLong atomic = entry.getValue();
      if (atomic != null && atomic.get() == 0L) {
        entryIterator.remove();
      }
    }
  }

  /**
   * Returns the sum of all values in this map.
   *
   * <p>This method is not atomic: the sum may or may not include other concurrent operations.
   */
  public long sum() {
    long sum = 0L;
    for (AtomicLong value : map.values()) {
      sum = sum + value.get();
    }
    return sum;
  }

  @CheckForNull private transient Map<K, Long> asMap;

  /** Returns a live, read-only view of the map backing this {@code AtomicLongMap}. */
  public Map<K, Long> asMap() {
    Map<K, Long> result = asMap;
    return (result == null) ? asMap = createAsMap() : result;
  }

  private Map<K, Long> createAsMap() {
    return Collections.unmodifiableMap(
        Maps.transformValues(
            map,
            new Function<AtomicLong, Long>() {
              @Override
              public Long apply(AtomicLong atomic) {
                return atomic.get();
              }
            }));
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

  /*
   * ConcurrentMap operations which we may eventually add.
   *
   * The problem with these is that remove(K, long) has to be done in two phases by definition ---
   * first decrementing to zero, and then removing. putIfAbsent or replace could observe the
   * intermediate zero-state. Ways we could deal with this are:
   *
   * - Don't define any of the ConcurrentMap operations. This is the current state of affairs.
   *
   * - Define putIfAbsent and replace as treating zero and absent identically (as currently
   *   implemented below). This is a bit surprising with putIfAbsent, which really becomes
   *   putIfZero.
   *
   * - Allow putIfAbsent and replace to distinguish between zero and absent, but don't implement
   *   remove(K, long). Without any two-phase operations it becomes feasible for all remaining
   *   operations to distinguish between zero and absent. If we do this, then perhaps we should add
   *   replace(key, long).
   *
   * - Introduce a special-value private static final AtomicLong that would have the meaning of
   *   removal-in-progress, and rework all operations to properly distinguish between zero and
   *   absent.
   */

  /**
   * If {@code key} is not already associated with a value or if {@code key} is associated with
   * zero, associate it with {@code newValue}. Returns the previous value associated with {@code
   * key}, or zero if there was no mapping for {@code key}.
   */
  long putIfAbsent(K key, long newValue) {
    while (true) {
      AtomicLong atomic = map.get(key);
      if (atomic == null) {
        atomic = map.putIfAbsent(key, new AtomicLong(newValue));
        if (atomic == null) {
          return 0L;
        }
        // atomic is now non-null; fall through
      }

      long oldValue = atomic.get();
      if (oldValue == 0L) {
        // don't compareAndSet a zero
        if (map.replace(key, atomic, new AtomicLong(newValue))) {
          return 0L;
        }
        // atomic replaced
        continue;
      }

      return oldValue;
    }
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
      AtomicLong atomic = map.get(key);
      return (atomic == null) ? false : atomic.compareAndSet(expectedOldValue, newValue);
    }
  }
}

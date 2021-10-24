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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newTreeMap;
import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableSortedMap;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * GWT emulated version of {@link com.google.common.collect.ImmutableSortedMap}. It's a thin wrapper
 * around a {@link java.util.TreeMap}.
 *
 * @author Hayward Chan
 */
public final class ImmutableSortedMap<K, V> extends ForwardingImmutableMap<K, V>
    implements SortedMap<K, V> {

  @SuppressWarnings("unchecked")
  static final Comparator NATURAL_ORDER = Ordering.natural();

  // This reference is only used by GWT compiler to infer the keys and values
  // of the map that needs to be serialized.
  private Comparator<? super K> unusedComparatorForSerialization;
  private K unusedKeyForSerialization;
  private V unusedValueForSerialization;

  private final transient SortedMap<K, V> sortedDelegate;

  // The comparator used by this map.  It's the same as that of sortedDelegate,
  // except that when sortedDelegate's comparator is null, it points to a
  // non-null instance of Ordering.natural().
  // (cpovirk: Is sortedDelegate's comparator really ever null?)
  // The comparator will likely also differ because of our nullAccepting hack.
  // See the bottom of the file for more information about it.
  private final transient Comparator<? super K> comparator;

  ImmutableSortedMap(SortedMap<K, V> delegate, Comparator<? super K> comparator) {
    super(delegate);
    this.comparator = comparator;
    this.sortedDelegate = delegate;
  }

  public static <T, K, V> Collector<T, ?, ImmutableSortedMap<K, V>> toImmutableSortedMap(
      Comparator<? super K> comparator,
      Function<? super T, ? extends K> keyFunction,
      Function<? super T, ? extends V> valueFunction) {
    return CollectCollectors.toImmutableSortedMap(comparator, keyFunction, valueFunction);
  }

  public static <T, K, V> Collector<T, ?, ImmutableSortedMap<K, V>> toImmutableSortedMap(
      Comparator<? super K> comparator,
      Function<? super T, ? extends K> keyFunction,
      Function<? super T, ? extends V> valueFunction,
      BinaryOperator<V> mergeFunction) {
    checkNotNull(comparator);
    checkNotNull(keyFunction);
    checkNotNull(valueFunction);
    checkNotNull(mergeFunction);
    return Collectors.collectingAndThen(
        Collectors.toMap(
            keyFunction, valueFunction, mergeFunction, () -> new TreeMap<K, V>(comparator)),
        ImmutableSortedMap::copyOfSorted);
  }

  // Casting to any type is safe because the set will never hold any elements.
  @SuppressWarnings("unchecked")
  public static <K, V> ImmutableSortedMap<K, V> of() {
    return new Builder<K, V>(NATURAL_ORDER).build();
  }

  public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(K k1, V v1) {
    return copyOf(singletonMap(k1, v1));
  }

  public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(
      K k1, V v1, K k2, V v2) {
    return new Builder<K, V>(Ordering.natural()).put(k1, v1).put(k2, v2).build();
  }

  public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3) {
    return new Builder<K, V>(Ordering.natural()).put(k1, v1).put(k2, v2).put(k3, v3).build();
  }

  public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
    return new Builder<K, V>(Ordering.natural())
        .put(k1, v1)
        .put(k2, v2)
        .put(k3, v3)
        .put(k4, v4)
        .build();
  }

  public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
    return new Builder<K, V>(Ordering.natural())
        .put(k1, v1)
        .put(k2, v2)
        .put(k3, v3)
        .put(k4, v4)
        .put(k5, v5)
        .build();
  }

  public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6) {
    return new Builder<K, V>(Ordering.natural())
        .put(k1, v1)
        .put(k2, v2)
        .put(k3, v3)
        .put(k4, v4)
        .put(k5, v5)
        .put(k6, v6)
        .build();
  }

  public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7) {
    return new Builder<K, V>(Ordering.natural())
        .put(k1, v1)
        .put(k2, v2)
        .put(k3, v3)
        .put(k4, v4)
        .put(k5, v5)
        .put(k6, v6)
        .put(k7, v7)
        .build();
  }

  public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(
      K k1,
      V v1,
      K k2,
      V v2,
      K k3,
      V v3,
      K k4,
      V v4,
      K k5,
      V v5,
      K k6,
      V v6,
      K k7,
      V v7,
      K k8,
      V v8) {
    return new Builder<K, V>(Ordering.natural())
        .put(k1, v1)
        .put(k2, v2)
        .put(k3, v3)
        .put(k4, v4)
        .put(k5, v5)
        .put(k6, v6)
        .put(k7, v7)
        .put(k8, v8)
        .build();
  }

  public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(
      K k1,
      V v1,
      K k2,
      V v2,
      K k3,
      V v3,
      K k4,
      V v4,
      K k5,
      V v5,
      K k6,
      V v6,
      K k7,
      V v7,
      K k8,
      V v8,
      K k9,
      V v9) {
    return new Builder<K, V>(Ordering.natural())
        .put(k1, v1)
        .put(k2, v2)
        .put(k3, v3)
        .put(k4, v4)
        .put(k5, v5)
        .put(k6, v6)
        .put(k7, v7)
        .put(k8, v8)
        .put(k9, v9)
        .build();
  }

  public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(
      K k1,
      V v1,
      K k2,
      V v2,
      K k3,
      V v3,
      K k4,
      V v4,
      K k5,
      V v5,
      K k6,
      V v6,
      K k7,
      V v7,
      K k8,
      V v8,
      K k9,
      V v9,
      K k10,
      V v10) {
    return new Builder<K, V>(Ordering.natural())
        .put(k1, v1)
        .put(k2, v2)
        .put(k3, v3)
        .put(k4, v4)
        .put(k5, v5)
        .put(k6, v6)
        .put(k7, v7)
        .put(k8, v8)
        .put(k9, v9)
        .put(k10, v10)
        .build();
  }

  public static <K, V> ImmutableSortedMap<K, V> copyOf(Map<? extends K, ? extends V> map) {
    return copyOfInternal((Map) map, (Ordering<K>) Ordering.natural());
  }

  public static <K, V> ImmutableSortedMap<K, V> copyOf(
      Map<? extends K, ? extends V> map, Comparator<? super K> comparator) {
    return copyOfInternal(map, checkNotNull(comparator));
  }

  public static <K, V> ImmutableSortedMap<K, V> copyOf(
      Iterable<? extends Entry<? extends K, ? extends V>> entries) {
    return new Builder<K, V>(NATURAL_ORDER).putAll(entries).build();
  }

  public static <K, V> ImmutableSortedMap<K, V> copyOf(
      Iterable<? extends Entry<? extends K, ? extends V>> entries,
      Comparator<? super K> comparator) {
    return new Builder<K, V>(comparator).putAll(entries).build();
  }

  public static <K, V> ImmutableSortedMap<K, V> copyOfSorted(SortedMap<K, ? extends V> map) {
    // If map has a null comparator, the keys should have a natural ordering,
    // even though K doesn't explicitly implement Comparable.
    @SuppressWarnings("unchecked")
    Comparator<? super K> comparator =
        (map.comparator() == null) ? NATURAL_ORDER : map.comparator();
    return copyOfInternal(map, comparator);
  }

  private static <K, V> ImmutableSortedMap<K, V> copyOfInternal(
      Map<? extends K, ? extends V> map, Comparator<? super K> comparator) {

    if (map instanceof ImmutableSortedMap) {
      // TODO: Prove that this cast is safe, even though
      // Collections.unmodifiableSortedMap requires the same key type.
      @SuppressWarnings("unchecked")
      ImmutableSortedMap<K, V> kvMap = (ImmutableSortedMap<K, V>) map;
      Comparator<?> comparator2 = kvMap.comparator();
      boolean sameComparator =
          (comparator2 == null) ? comparator == NATURAL_ORDER : comparator.equals(comparator2);
      if (sameComparator) {
        return kvMap;
      }
    }

    SortedMap<K, V> delegate = newModifiableDelegate(comparator);
    for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
      putEntryWithChecks(delegate, entry);
    }
    return newView(unmodifiableSortedMap(delegate), comparator);
  }

  private static <K, V> void putEntryWithChecks(
      SortedMap<K, V> map, Entry<? extends K, ? extends V> entry) {
    K key = checkNotNull(entry.getKey());
    V value = checkNotNull(entry.getValue());
    if (map.containsKey(key)) {
      // When a collision happens, the colliding entry is the first entry
      // of the tail map.
      Entry<K, V> previousEntry = map.tailMap(key).entrySet().iterator().next();
      throw new IllegalArgumentException(
          "Duplicate keys in mappings "
              + previousEntry.getKey()
              + "="
              + previousEntry.getValue()
              + " and "
              + key
              + "="
              + value);
    }
    map.put(key, value);
  }

  public static <K extends Comparable<?>, V> Builder<K, V> naturalOrder() {
    return new Builder<K, V>(Ordering.natural());
  }

  public static <K, V> Builder<K, V> orderedBy(Comparator<K> comparator) {
    return new Builder<K, V>(comparator);
  }

  public static <K extends Comparable<?>, V> Builder<K, V> reverseOrder() {
    return new Builder<K, V>(Ordering.natural().reverse());
  }

  public static final class Builder<K, V> extends ImmutableMap.Builder<K, V> {
    private final Comparator<? super K> comparator;

    public Builder(Comparator<? super K> comparator) {
      this.comparator = checkNotNull(comparator);
    }

    @Override
    public Builder<K, V> put(K key, V value) {
      entries.add(entryOf(key, value));
      return this;
    }

    @Override
    public Builder<K, V> put(Entry<? extends K, ? extends V> entry) {
      super.put(entry);
      return this;
    }

    @Override
    public Builder<K, V> putAll(Map<? extends K, ? extends V> map) {
      return putAll(map.entrySet());
    }

    @Override
    public Builder<K, V> putAll(Iterable<? extends Entry<? extends K, ? extends V>> entries) {
      for (Entry<? extends K, ? extends V> entry : entries) {
        put(entry);
      }
      return this;
    }

    Builder<K, V> combine(Builder<K, V> other) {
      super.combine(other);
      return this;
    }

    @Override
    public Builder<K, V> orderEntriesByValue(Comparator<? super V> valueComparator) {
      throw new UnsupportedOperationException("Not available on ImmutableSortedMap.Builder");
    }

    @Override
    public ImmutableSortedMap<K, V> build() {
      return buildOrThrow();
    }

    @Override
    public ImmutableSortedMap<K, V> buildOrThrow() {
      SortedMap<K, V> delegate = newModifiableDelegate(comparator);
      for (Entry<? extends K, ? extends V> entry : entries) {
        putEntryWithChecks(delegate, entry);
      }
      return newView(unmodifiableSortedMap(delegate), comparator);
    }
  }

  private transient ImmutableSortedSet<K> keySet;

  @Override
  public ImmutableSortedSet<K> keySet() {
    ImmutableSortedSet<K> ks = keySet;
    return (ks == null) ? (keySet = createKeySet()) : ks;
  }

  @Override
  ImmutableSortedSet<K> createKeySet() {
    // the keySet() of the delegate is only a Set and TreeMap.navigatableKeySet
    // is not available in GWT yet.  To keep the code simple and code size more,
    // we make a copy here, instead of creating a view of it.
    //
    // TODO: revisit if it's unbearably slow or when GWT supports
    // TreeMap.navigatbleKeySet().
    return ImmutableSortedSet.copyOf(comparator, sortedDelegate.keySet());
  }

  public Comparator<? super K> comparator() {
    return comparator;
  }

  public K firstKey() {
    return sortedDelegate.firstKey();
  }

  public K lastKey() {
    return sortedDelegate.lastKey();
  }

  K higher(K k) {
    Iterator<K> iterator = keySet().tailSet(k).iterator();
    while (iterator.hasNext()) {
      K tmp = iterator.next();
      if (comparator().compare(k, tmp) < 0) {
        return tmp;
      }
    }
    return null;
  }

  public ImmutableSortedMap<K, V> headMap(K toKey) {
    checkNotNull(toKey);
    return newView(sortedDelegate.headMap(toKey));
  }

  ImmutableSortedMap<K, V> headMap(K toKey, boolean inclusive) {
    checkNotNull(toKey);
    if (inclusive) {
      K tmp = higher(toKey);
      if (tmp == null) {
        return this;
      }
      toKey = tmp;
    }
    return headMap(toKey);
  }

  public ImmutableSortedMap<K, V> subMap(K fromKey, K toKey) {
    checkNotNull(fromKey);
    checkNotNull(toKey);
    checkArgument(comparator.compare(fromKey, toKey) <= 0);
    return newView(sortedDelegate.subMap(fromKey, toKey));
  }

  ImmutableSortedMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
    checkNotNull(fromKey);
    checkNotNull(toKey);
    checkArgument(comparator.compare(fromKey, toKey) <= 0);
    return tailMap(fromKey, fromInclusive).headMap(toKey, toInclusive);
  }

  public ImmutableSortedMap<K, V> tailMap(K fromKey) {
    checkNotNull(fromKey);
    return newView(sortedDelegate.tailMap(fromKey));
  }

  public ImmutableSortedMap<K, V> tailMap(K fromKey, boolean inclusive) {
    checkNotNull(fromKey);
    if (!inclusive) {
      fromKey = higher(fromKey);
      if (fromKey == null) {
        return new Builder<K, V>(this.comparator).build();
      }
    }
    return tailMap(fromKey);
  }

  private ImmutableSortedMap<K, V> newView(SortedMap<K, V> delegate) {
    return newView(delegate, comparator);
  }

  private static <K, V> ImmutableSortedMap<K, V> newView(
      SortedMap<K, V> delegate, Comparator<? super K> comparator) {
    return new ImmutableSortedMap<K, V>(delegate, comparator);
  }

  /*
   * We don't permit nulls, but we wrap every comparator with nullsFirst().
   * Why? We want for queries like containsKey(null) to return false, but the
   * GWT SortedMap implementation that we delegate to throws
   * NullPointerException if the comparator does. Since our construction
   * methods ensure that null is never present in the map, it's OK for the
   * comparator to look for it wherever it wants.
   *
   * Note that we do NOT touch the comparator returned by comparator(), which
   * should be identical to the one the user passed in. We touch only the
   * "secret" comparator used by the delegate implementation.
   */

  private static <K, V> SortedMap<K, V> newModifiableDelegate(Comparator<? super K> comparator) {
    return newTreeMap(nullAccepting(comparator));
  }

  private static <E> Comparator<E> nullAccepting(Comparator<E> comparator) {
    return Ordering.from(comparator).nullsFirst();
  }
}

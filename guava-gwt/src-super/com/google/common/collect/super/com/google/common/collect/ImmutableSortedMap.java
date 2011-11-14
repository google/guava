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

import com.google.common.collect.ImmutableSortedSet;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;

/**
 * GWT emulated version of {@link ImmutableSortedMap}.  It's a thin wrapper
 * around a {@link java.util.TreeMap}.
 *
 * @author Hayward Chan
 */
public class ImmutableSortedMap<K, V>
    extends ImmutableMap<K, V> implements SortedMap<K, V> {

  // TODO: Confirm that ImmutableSortedMap is faster to construct and uses less
  // memory than TreeMap; then say so in the class Javadoc.

  // TODO: Create separate subclasses for empty, single-entry, and
  // multiple-entry instances.

  @SuppressWarnings("unchecked")
  private static final Comparator NATURAL_ORDER = Ordering.natural();

  @SuppressWarnings("unchecked")
  private static final ImmutableSortedMap<Object, Object> NATURAL_EMPTY_MAP
      = create(NATURAL_ORDER);

  // This reference is only used by GWT compiler to infer the keys and values
  // of the map that needs to be serialized.
  private Comparator<K> unusedComparatorForSerialization;
  private K unusedKeyForSerialization;
  private V unusedValueForSerialization;

  private transient final SortedMap<K, V> sortedDelegate;

  // The comparator used by this map.  It's the same as that of sortedDelegate,
  // except that when sortedDelegate's comparator is null, it points to a
  // non-null instance of Ordering.natural().
  private transient final Comparator<K> comparator;

  // If map has a null comparator, the keys should have a natural ordering,
  // even though K doesn't explicitly implement Comparable.
  @SuppressWarnings("unchecked")
  ImmutableSortedMap(SortedMap<K, ? extends V> delegate) {
    super(delegate);
    this.comparator = (delegate.comparator() == null)
        ? NATURAL_ORDER : delegate.comparator();
    this.sortedDelegate = Collections.unmodifiableSortedMap(delegate);
  }

  private static <K, V> ImmutableSortedMap<K, V> create(
      Comparator<? super K> comparator,
      Entry<? extends K, ? extends V>... entries) {
    checkNotNull(comparator);
    SortedMap<K, V> delegate = Maps.newTreeMap(comparator);
    for (Entry<? extends K, ? extends V> entry : entries) {
      delegate.put(entry.getKey(), entry.getValue());
    }
    return new ImmutableSortedMap<K, V>(delegate);
  }

  // Casting to any type is safe because the set will never hold any elements.
  @SuppressWarnings("unchecked")
  public static <K, V> ImmutableSortedMap<K, V> of() {
    return (ImmutableSortedMap) NATURAL_EMPTY_MAP;
  }

  public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V>
      of(K k1, V v1) {
    return create(Ordering.natural(), entryOf(k1, v1));
  }

  public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V>
      of(K k1, V v1, K k2, V v2) {
    return new Builder<K, V>(Ordering.natural())
        .put(k1, v1).put(k2, v2).build();
  }

  public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V>
      of(K k1, V v1, K k2, V v2, K k3, V v3) {
    return new Builder<K, V>(Ordering.natural())
        .put(k1, v1).put(k2, v2).put(k3, v3).build();
  }

  public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V>
      of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
    return new Builder<K, V>(Ordering.natural())
        .put(k1, v1).put(k2, v2).put(k3, v3).put(k4, v4).build();
  }

  public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V>
      of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
    return new Builder<K, V>(Ordering.natural())
        .put(k1, v1).put(k2, v2).put(k3, v3).put(k4, v4).put(k5, v5).build();
  }

  public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V>
      copyOf(Map<? extends K, ? extends V> map) {
    return copyOfInternal(map, Ordering.natural());
  }

  public static <K, V> ImmutableSortedMap<K, V> copyOf(
      Map<? extends K, ? extends V> map, Comparator<? super K> comparator) {
    return copyOfInternal(map, checkNotNull(comparator));
  }

  public static <K, V> ImmutableSortedMap<K, V> copyOfSorted(
      SortedMap<K, ? extends V> map) {
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
      boolean sameComparator = (comparator2 == null)
          ? comparator == NATURAL_ORDER
          : comparator.equals(comparator2);
      if (sameComparator) {
        return kvMap;
      }
    }

    SortedMap<K, V> delegate = Maps.newTreeMap(comparator);
    for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
      putEntryWithChecks(delegate, entry);
    }
    return new ImmutableSortedMap<K, V>(delegate);
  }

  private static <K, V> void putEntryWithChecks(
      SortedMap<K, V> map, Entry<? extends K, ? extends V> entry) {
    K key = checkNotNull(entry.getKey());
    V value = checkNotNull(entry.getValue());
    if (map.containsKey(key)) {
      // When a collision happens, the colliding entry is the first entry
      // of the tail map.
      Entry<K, V> previousEntry
          = map.tailMap(key).entrySet().iterator().next();
      throw new IllegalArgumentException(
          "Duplicate keys in mappings " + previousEntry.getKey() +
          "=" + previousEntry.getValue() + " and " + key +
          "=" + value);
    }
    map.put(key, value);
  }

  public static <K extends Comparable<K>, V> Builder<K, V> naturalOrder() {
    return new Builder<K, V>(Ordering.natural());
  }

  public static <K, V> Builder<K, V> orderedBy(Comparator<K> comparator) {
    return new Builder<K, V>(comparator);
  }

  public static <K extends Comparable<K>, V> Builder<K, V> reverseOrder() {
    return new Builder<K, V>(Ordering.natural().reverse());
  }

  public static final class Builder<K, V> extends ImmutableMap.Builder<K, V> {
    private final Comparator<? super K> comparator;

    public Builder(Comparator<? super K> comparator) {
      this.comparator = checkNotNull(comparator);
    }

    @Override public Builder<K, V> put(K key, V value) {
      entries.add(entryOf(key, value));
      return this;
    }

    @Override public Builder<K, V> put(Entry<? extends K, ? extends V> entry) {
      super.put(entry);
      return this;
    }

    @Override public Builder<K, V> putAll(Map<? extends K, ? extends V> map) {
      for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
        put(entry.getKey(), entry.getValue());
      }
      return this;
    }

    @Override public ImmutableSortedMap<K, V> build() {
      SortedMap<K, V> delegate = Maps.newTreeMap(comparator);
      for (Entry<? extends K, ? extends V> entry : entries) {
        putEntryWithChecks(delegate, entry);
      }
      return new ImmutableSortedMap<K, V>(delegate);
    }
  }

  private transient ImmutableSortedSet<K> keySet;

  @Override public ImmutableSortedSet<K> keySet() {
    ImmutableSortedSet<K> ks = keySet;
    return (ks == null) ? (keySet = createKeySet()) : ks;
  }

  private ImmutableSortedSet<K> createKeySet() {
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
    return new ImmutableSortedMap<K, V>(sortedDelegate.headMap(toKey));
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
    return new ImmutableSortedMap<K, V>(sortedDelegate.subMap(fromKey, toKey));
  }

  ImmutableSortedMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive){
    checkNotNull(fromKey);
    checkNotNull(toKey);
    checkArgument(comparator.compare(fromKey, toKey) <= 0);
    return tailMap(fromKey, fromInclusive).headMap(toKey, toInclusive);
  }
  
  public ImmutableSortedMap<K, V> tailMap(K fromKey) {
    checkNotNull(fromKey);
    return new ImmutableSortedMap<K, V>(sortedDelegate.tailMap(fromKey));
  }

  public ImmutableSortedMap<K, V> tailMap(K fromKey, boolean inclusive) {
    checkNotNull(fromKey);
    if (!inclusive) {
      fromKey = higher(fromKey);
      if (fromKey == null) {
        return emptyMap(comparator());
      }
    }
    return tailMap(fromKey);
  }

  static <K, V> ImmutableSortedMap<K, V> emptyMap(Comparator<? super K> comparator) {
    if (comparator == NATURAL_ORDER) {
      return (ImmutableSortedMap) NATURAL_EMPTY_MAP;
    }
    return create(comparator);
  }
}

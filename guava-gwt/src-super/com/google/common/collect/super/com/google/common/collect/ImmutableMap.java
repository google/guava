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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.CollectPreconditions.checkEntryNotNull;
import static com.google.common.collect.Iterables.getOnlyElement;

import com.google.common.annotations.Beta;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Spliterator;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * GWT emulation of {@link com.google.common.collect.ImmutableMap}. For non sorted maps, it is a
 * thin wrapper around {@link java.util.Collections#emptyMap()}, {@link
 * Collections#singletonMap(Object, Object)} and {@link java.util.LinkedHashMap} for empty,
 * singleton and regular maps respectively. For sorted maps, it's a thin wrapper around {@link
 * java.util.TreeMap}.
 *
 * @see ImmutableSortedMap
 * @author Hayward Chan
 */
public abstract class ImmutableMap<K, V> implements Map<K, V>, Serializable {
  static final ImmutableMap<Object, Object> EMPTY = new RegularImmutableMap<Object, Object>();

  abstract static class IteratorBasedImmutableMap<K, V> extends ImmutableMap<K, V> {
    abstract UnmodifiableIterator<Entry<K, V>> entryIterator();

    @Override
    ImmutableSet<Entry<K, V>> createEntrySet() {
      return new ImmutableMapEntrySet<K, V>() {
        @Override
        ImmutableMap<K, V> map() {
          return IteratorBasedImmutableMap.this;
        }

        @Override
        public UnmodifiableIterator<Entry<K, V>> iterator() {
          return entryIterator();
        }
      };
    }
  }

  ImmutableMap() {}

  @Beta
  public static <T, K, V> Collector<T, ?, ImmutableMap<K, V>> toImmutableMap(
      Function<? super T, ? extends K> keyFunction,
      Function<? super T, ? extends V> valueFunction) {
    return CollectCollectors.toImmutableMap(keyFunction, valueFunction);
  }

  @Beta
  public static <T, K, V> Collector<T, ?, ImmutableMap<K, V>> toImmutableMap(
      Function<? super T, ? extends K> keyFunction,
      Function<? super T, ? extends V> valueFunction,
      BinaryOperator<V> mergeFunction) {
    checkNotNull(keyFunction);
    checkNotNull(valueFunction);
    checkNotNull(mergeFunction);
    return Collectors.collectingAndThen(
        Collectors.toMap(keyFunction, valueFunction, mergeFunction, LinkedHashMap::new),
        ImmutableMap::copyOf);
  }

  public static <K, V> ImmutableMap<K, V> of() {
    return (ImmutableMap<K, V>) EMPTY;
  }

  public static <K, V> ImmutableMap<K, V> of(K k1, V v1) {
    return ImmutableBiMap.of(k1, v1);
  }

  public static <K, V> ImmutableMap<K, V> of(K k1, V v1, K k2, V v2) {
    return new RegularImmutableMap<K, V>(entryOf(k1, v1), entryOf(k2, v2));
  }

  public static <K, V> ImmutableMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
    return new RegularImmutableMap<K, V>(entryOf(k1, v1), entryOf(k2, v2), entryOf(k3, v3));
  }

  public static <K, V> ImmutableMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
    return new RegularImmutableMap<K, V>(
        entryOf(k1, v1), entryOf(k2, v2), entryOf(k3, v3), entryOf(k4, v4));
  }

  public static <K, V> ImmutableMap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
    return new RegularImmutableMap<K, V>(
        entryOf(k1, v1), entryOf(k2, v2), entryOf(k3, v3), entryOf(k4, v4), entryOf(k5, v5));
  }

  // looking for of() with > 5 entries? Use the builder instead.

  public static <K, V> Builder<K, V> builder() {
    return new Builder<K, V>();
  }

  public static <K, V> Builder<K, V> builderWithExpectedSize(int expectedSize) {
    return new Builder<K, V>(expectedSize);
  }

  static <K, V> Entry<K, V> entryOf(K key, V value) {
    checkEntryNotNull(key, value);
    return Maps.immutableEntry(key, value);
  }

  public static class Builder<K, V> {
    final List<Entry<K, V>> entries;
    Comparator<? super V> valueComparator;

    public Builder() {
      this.entries = Lists.newArrayList();
    }

    Builder(int initCapacity) {
      this.entries = Lists.newArrayListWithCapacity(initCapacity);
    }

    public Builder<K, V> put(K key, V value) {
      entries.add(entryOf(key, value));
      return this;
    }

    public Builder<K, V> put(Entry<? extends K, ? extends V> entry) {
      if (entry instanceof ImmutableEntry) {
        checkNotNull(entry.getKey());
        checkNotNull(entry.getValue());
        @SuppressWarnings("unchecked") // all supported methods are covariant
        Entry<K, V> immutableEntry = (Entry<K, V>) entry;
        entries.add(immutableEntry);
      } else {
        entries.add(entryOf((K) entry.getKey(), (V) entry.getValue()));
      }
      return this;
    }

    public Builder<K, V> putAll(Map<? extends K, ? extends V> map) {
      return putAll(map.entrySet());
    }

    public Builder<K, V> putAll(Iterable<? extends Entry<? extends K, ? extends V>> entries) {
      for (Entry<? extends K, ? extends V> entry : entries) {
        put(entry);
      }
      return this;
    }

    public Builder<K, V> orderEntriesByValue(Comparator<? super V> valueComparator) {
      checkState(this.valueComparator == null, "valueComparator was already set");
      this.valueComparator = checkNotNull(valueComparator, "valueComparator");
      return this;
    }

    Builder<K, V> combine(Builder<K, V> other) {
      checkNotNull(other);
      entries.addAll(other.entries);
      return this;
    }

    public ImmutableMap<K, V> build() {
      if (valueComparator != null) {
        Collections.sort(
            entries, Ordering.from(valueComparator).onResultOf(Maps.<V>valueFunction()));
      }
      return fromEntryList(entries);
    }

    ImmutableMap<K, V> buildJdkBacked() {
      return build();
    }
  }

  static <K, V> ImmutableMap<K, V> fromEntryList(
      Collection<? extends Entry<? extends K, ? extends V>> entries) {
    int size = entries.size();
    switch (size) {
      case 0:
        return of();
      case 1:
        Entry<? extends K, ? extends V> entry = getOnlyElement(entries);
        return of((K) entry.getKey(), (V) entry.getValue());
      default:
        @SuppressWarnings("unchecked")
        Entry<K, V>[] entryArray = entries.toArray(new Entry[entries.size()]);
        return new RegularImmutableMap<K, V>(entryArray);
    }
  }

  public static <K, V> ImmutableMap<K, V> copyOf(Map<? extends K, ? extends V> map) {
    if ((map instanceof ImmutableMap) && !(map instanceof ImmutableSortedMap)) {
      @SuppressWarnings("unchecked") // safe since map is not writable
      ImmutableMap<K, V> kvMap = (ImmutableMap<K, V>) map;
      return kvMap;
    } else if (map instanceof EnumMap) {
      EnumMap<?, ?> enumMap = (EnumMap<?, ?>) map;
      for (Entry<?, ?> entry : enumMap.entrySet()) {
        checkNotNull(entry.getKey());
        checkNotNull(entry.getValue());
      }
      @SuppressWarnings("unchecked")
      // immutable collections are safe for covariant casts
      ImmutableMap<K, V> result = ImmutableEnumMap.asImmutable(new EnumMap(enumMap));
      return result;
    }

    int size = map.size();
    switch (size) {
      case 0:
        return of();
      case 1:
        Entry<? extends K, ? extends V> entry = getOnlyElement(map.entrySet());
        return ImmutableMap.<K, V>of(entry.getKey(), entry.getValue());
      default:
        Map<K, V> orderPreservingCopy = Maps.newLinkedHashMap();
        for (Entry<? extends K, ? extends V> e : map.entrySet()) {
          orderPreservingCopy.put(checkNotNull(e.getKey()), checkNotNull(e.getValue()));
        }
        return new RegularImmutableMap<K, V>(orderPreservingCopy);
    }
  }

  public static <K, V> ImmutableMap<K, V> copyOf(
      Iterable<? extends Entry<? extends K, ? extends V>> entries) {
    if (entries instanceof Collection) {
      return fromEntryList((Collection<? extends Entry<? extends K, ? extends V>>) entries);
    } else {
      return fromEntryList(Lists.newArrayList(entries.iterator()));
    }
  }

  abstract boolean isPartialView();

  public final V put(K k, V v) {
    throw new UnsupportedOperationException();
  }

  public final V remove(Object o) {
    throw new UnsupportedOperationException();
  }

  public final void putAll(Map<? extends K, ? extends V> map) {
    throw new UnsupportedOperationException();
  }

  public final void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public boolean containsKey(@Nullable Object key) {
    return get(key) != null;
  }

  @Override
  public boolean containsValue(@Nullable Object value) {
    return values().contains(value);
  }

  private transient ImmutableSet<Entry<K, V>> cachedEntrySet = null;

  public final ImmutableSet<Entry<K, V>> entrySet() {
    if (cachedEntrySet != null) {
      return cachedEntrySet;
    }
    return cachedEntrySet = createEntrySet();
  }

  abstract ImmutableSet<Entry<K, V>> createEntrySet();

  private transient ImmutableSet<K> cachedKeySet = null;

  public ImmutableSet<K> keySet() {
    if (cachedKeySet != null) {
      return cachedKeySet;
    }
    return cachedKeySet = createKeySet();
  }

  ImmutableSet<K> createKeySet() {
    return new ImmutableMapKeySet<K, V>(this);
  }

  UnmodifiableIterator<K> keyIterator() {
    final UnmodifiableIterator<Entry<K, V>> entryIterator = entrySet().iterator();
    return new UnmodifiableIterator<K>() {
      @Override
      public boolean hasNext() {
        return entryIterator.hasNext();
      }

      @Override
      public K next() {
        return entryIterator.next().getKey();
      }
    };
  }

  Spliterator<K> keySpliterator() {
    return CollectSpliterators.map(entrySet().spliterator(), Entry::getKey);
  }

  private transient ImmutableCollection<V> cachedValues = null;

  public ImmutableCollection<V> values() {
    if (cachedValues != null) {
      return cachedValues;
    }
    return cachedValues = createValues();
  }

  // cached so that this.multimapView().inverse() only computes inverse once
  private transient ImmutableSetMultimap<K, V> multimapView;

  public ImmutableSetMultimap<K, V> asMultimap() {
    ImmutableSetMultimap<K, V> result = multimapView;
    return (result == null)
        ? (multimapView =
            new ImmutableSetMultimap<K, V>(new MapViewOfValuesAsSingletonSets(), size(), null))
        : result;
  }

  final class MapViewOfValuesAsSingletonSets extends IteratorBasedImmutableMap<K, ImmutableSet<V>> {

    @Override
    public int size() {
      return ImmutableMap.this.size();
    }

    @Override
    public ImmutableSet<K> keySet() {
      return ImmutableMap.this.keySet();
    }

    @Override
    public boolean containsKey(@Nullable Object key) {
      return ImmutableMap.this.containsKey(key);
    }

    @Override
    public ImmutableSet<V> get(@Nullable Object key) {
      V outerValue = ImmutableMap.this.get(key);
      return (outerValue == null) ? null : ImmutableSet.of(outerValue);
    }

    @Override
    boolean isPartialView() {
      return ImmutableMap.this.isPartialView();
    }

    @Override
    public int hashCode() {
      // ImmutableSet.of(value).hashCode() == value.hashCode(), so the hashes are the same
      return ImmutableMap.this.hashCode();
    }

    @Override
    UnmodifiableIterator<Entry<K, ImmutableSet<V>>> entryIterator() {
      final Iterator<Entry<K, V>> backingIterator = ImmutableMap.this.entrySet().iterator();
      return new UnmodifiableIterator<Entry<K, ImmutableSet<V>>>() {
        @Override
        public boolean hasNext() {
          return backingIterator.hasNext();
        }

        @Override
        public Entry<K, ImmutableSet<V>> next() {
          final Entry<K, V> backingEntry = backingIterator.next();
          return new AbstractMapEntry<K, ImmutableSet<V>>() {
            @Override
            public K getKey() {
              return backingEntry.getKey();
            }

            @Override
            public ImmutableSet<V> getValue() {
              return ImmutableSet.of(backingEntry.getValue());
            }
          };
        }
      };
    }
  }

  ImmutableCollection<V> createValues() {
    return new ImmutableMapValues<K, V>(this);
  }

  @Override
  public boolean equals(@Nullable Object object) {
    return Maps.equalsImpl(this, object);
  }

  @Override
  public int hashCode() {
    // not caching hash code since it could change if map values are mutable
    // in a way that modifies their hash codes
    return entrySet().hashCode();
  }

  @Override
  public String toString() {
    return Maps.toStringImpl(this);
  }
}

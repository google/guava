/*
 * Copyright (C) 2008 The Guava Authors
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

import static com.google.common.collect.CollectPreconditions.checkEntryNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * A {@link Map} whose contents will never change, with many other important properties detailed at
 * {@link ImmutableCollection}.
 *
 * <p>See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/ImmutableCollectionsExplained">
 * immutable collections</a>.
 *
 * @author Jesse Wilson
 * @author Kevin Bourrillion
 * @since 2.0 (imported from Google Collections Library)
 */
@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // we're overriding default serialization
public abstract class ImmutableMap<K, V> implements Map<K, V>, Serializable {

  /**
   * Returns the empty map. This map behaves and performs comparably to
   * {@link Collections#emptyMap}, and is preferable mainly for consistency
   * and maintainability of your code.
   */
  public static <K, V> ImmutableMap<K, V> of() {
    return ImmutableBiMap.of();
  }

  /**
   * Returns an immutable map containing a single entry. This map behaves and
   * performs comparably to {@link Collections#singletonMap} but will not accept
   * a null key or value. It is preferable mainly for consistency and
   * maintainability of your code.
   */
  public static <K, V> ImmutableMap<K, V> of(K k1, V v1) {
    return ImmutableBiMap.of(k1, v1);
  }

  /**
   * Returns an immutable map containing the given entries, in order.
   *
   * @throws IllegalArgumentException if duplicate keys are provided
   */
  public static <K, V> ImmutableMap<K, V> of(K k1, V v1, K k2, V v2) {
    return new RegularImmutableMap<K, V>(entryOf(k1, v1), entryOf(k2, v2));
  }

  /**
   * Returns an immutable map containing the given entries, in order.
   *
   * @throws IllegalArgumentException if duplicate keys are provided
   */
  public static <K, V> ImmutableMap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3) {
    return new RegularImmutableMap<K, V>(
        entryOf(k1, v1), entryOf(k2, v2), entryOf(k3, v3));
  }

  /**
   * Returns an immutable map containing the given entries, in order.
   *
   * @throws IllegalArgumentException if duplicate keys are provided
   */
  public static <K, V> ImmutableMap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
    return new RegularImmutableMap<K, V>(
        entryOf(k1, v1), entryOf(k2, v2), entryOf(k3, v3), entryOf(k4, v4));
  }

  /**
   * Returns an immutable map containing the given entries, in order.
   *
   * @throws IllegalArgumentException if duplicate keys are provided
   */
  public static <K, V> ImmutableMap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
    return new RegularImmutableMap<K, V>(entryOf(k1, v1),
        entryOf(k2, v2), entryOf(k3, v3), entryOf(k4, v4), entryOf(k5, v5));
  }

  // looking for of() with > 5 entries? Use the builder instead.

  /**
   * Verifies that {@code key} and {@code value} are non-null, and returns a new
   * immutable entry with those values.
   *
   * <p>A call to {@link Map.Entry#setValue} on the returned entry will always
   * throw {@link UnsupportedOperationException}.
   */
  static <K, V> ImmutableMapEntry<K, V> entryOf(K key, V value) {
    return new ImmutableMapEntry<K, V>(key, value);
  }

  /**
   * Returns a new builder. The generated builder is equivalent to the builder
   * created by the {@link Builder} constructor.
   */
  public static <K, V> Builder<K, V> builder() {
    return new Builder<K, V>();
  }

  static void checkNoConflict(boolean safe, String conflictDescription,
      Entry<?, ?> entry1, Entry<?, ?> entry2) {
    if (!safe) {
      throw new IllegalArgumentException(
          "Multiple entries with same " + conflictDescription + ": " + entry1 + " and " + entry2);
    }
  }

  /**
   * A builder for creating immutable map instances, especially {@code public
   * static final} maps ("constant maps"). Example: <pre>   {@code
   *
   *   static final ImmutableMap<String, Integer> WORD_TO_INT =
   *       new ImmutableMap.Builder<String, Integer>()
   *           .put("one", 1)
   *           .put("two", 2)
   *           .put("three", 3)
   *           .build();}</pre>
   *
   * <p>For <i>small</i> immutable maps, the {@code ImmutableMap.of()} methods are
   * even more convenient.
   *
   * <p>Builder instances can be reused - it is safe to call {@link #build}
   * multiple times to build multiple maps in series. Each map is a superset of
   * the maps created before it.
   *
   * @since 2.0 (imported from Google Collections Library)
   */
  public static class Builder<K, V> {
    ImmutableMapEntry<K, V>[] entries;
    int size;

    /**
     * Creates a new builder. The returned builder is equivalent to the builder
     * generated by {@link ImmutableMap#builder}.
     */
    public Builder() {
      this(ImmutableCollection.Builder.DEFAULT_INITIAL_CAPACITY);
    }

    @SuppressWarnings("unchecked")
    Builder(int initialCapacity) {
      this.entries = new ImmutableMapEntry[initialCapacity];
      this.size = 0;
    }

    private void ensureCapacity(int minCapacity) {
      if (minCapacity > entries.length) {
        entries = ObjectArrays.arraysCopyOf(
            entries, ImmutableCollection.Builder.expandedCapacity(entries.length, minCapacity));
      }
    }

    /**
     * Associates {@code key} with {@code value} in the built map. Duplicate
     * keys are not allowed, and will cause {@link #build} to fail.
     */
    public Builder<K, V> put(K key, V value) {
      ensureCapacity(size + 1);
      ImmutableMapEntry<K, V> entry = entryOf(key, value);
      // don't inline this: we want to fail atomically if key or value is null
      entries[size++] = entry;
      return this;
    }

    /**
     * Adds the given {@code entry} to the map, making it immutable if
     * necessary. Duplicate keys are not allowed, and will cause {@link #build}
     * to fail.
     *
     * @since 11.0
     */
    public Builder<K, V> put(Entry<? extends K, ? extends V> entry) {
      return put(entry.getKey(), entry.getValue());
    }

    /**
     * Associates all of the given map's keys and values in the built map.
     * Duplicate keys are not allowed, and will cause {@link #build} to fail.
     *
     * @throws NullPointerException if any key or value in {@code map} is null
     */
    public Builder<K, V> putAll(Map<? extends K, ? extends V> map) {
      ensureCapacity(size + map.size());
      return putAll(map.entrySet());
    }

    /**
     * Adds all of the given entries to the built map.  Duplicate keys are not
     * allowed, and will cause {@link #build} to fail.
     *
     * @throws NullPointerException if any key, value, or entry is null
     * @since 19.0
     */
    @Beta
    public Builder<K, V> putAll(
        Iterable<? extends Entry<? extends K, ? extends V>> entries) {
      for (Entry<? extends K, ? extends V> entry : entries) {
        put(entry);
      }
      return this;
    }

    /*
     * TODO(kevinb): Should build() and the ImmutableBiMap & ImmutableSortedMap
     * versions throw an IllegalStateException instead?
     */

    /**
     * Returns a newly-created immutable map.
     *
     * @throws IllegalArgumentException if duplicate keys were added
     */
    public ImmutableMap<K, V> build() {
      switch (size) {
        case 0:
          return of();
        case 1:
          return of(entries[0].getKey(), entries[0].getValue());
        default:
          return new RegularImmutableMap<K, V>(size, entries);
      }
    }
  }

  /**
   * Returns an immutable map containing the same entries as {@code map}. If
   * {@code map} somehow contains entries with duplicate keys (for example, if
   * it is a {@code SortedMap} whose comparator is not <i>consistent with
   * equals</i>), the results of this method are undefined.
   *
   * <p>Despite the method name, this method attempts to avoid actually copying
   * the data when it is safe to do so. The exact circumstances under which a
   * copy will or will not be performed are undocumented and subject to change.
   *
   * @throws NullPointerException if any key or value in {@code map} is null
   */
  public static <K, V> ImmutableMap<K, V> copyOf(
      Map<? extends K, ? extends V> map) {
    if ((map instanceof ImmutableMap) && !(map instanceof ImmutableSortedMap)) {
      // TODO(user): Make ImmutableMap.copyOf(immutableBiMap) call copyOf()
      // on the ImmutableMap delegate(), rather than the bimap itself

      @SuppressWarnings("unchecked") // safe since map is not writable
      ImmutableMap<K, V> kvMap = (ImmutableMap<K, V>) map;
      if (!kvMap.isPartialView()) {
        return kvMap;
      }
    } else if (map instanceof EnumMap) {
      return copyOfEnumMapUnsafe(map);
    }
    return copyOf(map.entrySet());
  }

  /**
   * Returns an immutable map containing the specified entries.  The returned
   * map iterates over entries in the same order as the original iterable.
   *
   * @throws NullPointerException if any key, value, or entry is null
   * @throws IllegalArgumentException if two entries have the same key
   * @since 19.0
   */
  @Beta
  public static <K, V> ImmutableMap<K, V> copyOf(
      Iterable<? extends Entry<? extends K, ? extends V>> entries) {
    Entry<?, ?>[] entryArray = Iterables.toArray(entries, EMPTY_ENTRY_ARRAY);
    switch (entryArray.length) {
      case 0:
        return of();
      case 1:
        @SuppressWarnings("unchecked") // all entries will be Entry<K, V>'s
        Entry<K, V> onlyEntry = (Entry<K, V>) entryArray[0];
        return of(onlyEntry.getKey(), onlyEntry.getValue());
      default:
        return new RegularImmutableMap<K, V>(entryArray.length, entryArray);
    }
  }

  // If the map is an EnumMap, it must have key type K for some <K extends Enum<K>>.
  @SuppressWarnings({"unchecked", "rawtypes"})
  private static <K, V> ImmutableMap<K, V> copyOfEnumMapUnsafe(Map<? extends K, ? extends V> map) {
    return copyOfEnumMap((EnumMap) map);
  }

  private static <K extends Enum<K>, V> ImmutableMap<K, V> copyOfEnumMap(
      EnumMap<K, ? extends V> original) {
    EnumMap<K, V> copy = new EnumMap<K, V>(original);
    for (Map.Entry<?, ?> entry : copy.entrySet()) {
      checkEntryNotNull(entry.getKey(), entry.getValue());
    }
    return ImmutableEnumMap.asImmutable(copy);
  }

  static final Entry<?, ?>[] EMPTY_ENTRY_ARRAY = new Entry<?, ?>[0];

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

  /**
   * Guaranteed to throw an exception and leave the map unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  public final V put(K k, V v) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the map unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  public final V remove(Object o) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the map unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  public final void putAll(Map<? extends K, ? extends V> map) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the map unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
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

  // Overriding to mark it Nullable
  @Override
  public abstract V get(@Nullable Object key);

  private transient ImmutableSet<Entry<K, V>> entrySet;

  /**
   * Returns an immutable set of the mappings in this map. The entries are in
   * the same order as the parameters used to build this map.
   */
  @Override
  public ImmutableSet<Entry<K, V>> entrySet() {
    ImmutableSet<Entry<K, V>> result = entrySet;
    return (result == null) ? entrySet = createEntrySet() : result;
  }

  abstract ImmutableSet<Entry<K, V>> createEntrySet();

  private transient ImmutableSet<K> keySet;

  /**
   * Returns an immutable set of the keys in this map. These keys are in
   * the same order as the parameters used to build this map.
   */
  @Override
  public ImmutableSet<K> keySet() {
    ImmutableSet<K> result = keySet;
    return (result == null) ? keySet = createKeySet() : result;
  }

  ImmutableSet<K> createKeySet() {
    return new ImmutableMapKeySet<K, V>(this);
  }

  UnmodifiableIterator<K> keyIterator() {
    final UnmodifiableIterator<Entry<K, V>> entryIterator = entrySet().iterator();
    return new UnmodifiableIterator<K>() {
      @Override public boolean hasNext() {
        return entryIterator.hasNext();
      }

      @Override public K next() {
        return entryIterator.next().getKey();
      }
    };
  }

  private transient ImmutableCollection<V> values;

  /**
   * Returns an immutable collection of the values in this map. The values are
   * in the same order as the parameters used to build this map.
   */
  @Override
  public ImmutableCollection<V> values() {
    ImmutableCollection<V> result = values;
    return (result == null) ? values = new ImmutableMapValues<K, V>(this) : result;
  }

  // cached so that this.multimapView().inverse() only computes inverse once
  private transient ImmutableSetMultimap<K, V> multimapView;

  /**
   * Returns a multimap view of the map.
   *
   * @since 14.0
   */
  @Beta
  public ImmutableSetMultimap<K, V> asMultimap() {
    ImmutableSetMultimap<K, V> result = multimapView;
    return (result == null)
        ? (multimapView = new ImmutableSetMultimap<K, V>(
            new MapViewOfValuesAsSingletonSets(), size(), null))
        : result;
  }

  private final class MapViewOfValuesAsSingletonSets
      extends IteratorBasedImmutableMap<K, ImmutableSet<V>> {

    @Override public int size() {
      return ImmutableMap.this.size();
    }

    @Override public ImmutableSet<K> keySet() {
      return ImmutableMap.this.keySet();
    }

    @Override public boolean containsKey(@Nullable Object key) {
      return ImmutableMap.this.containsKey(key);
    }

    @Override public ImmutableSet<V> get(@Nullable Object key) {
      V outerValue = ImmutableMap.this.get(key);
      return (outerValue == null) ? null : ImmutableSet.of(outerValue);
    }

    @Override boolean isPartialView() {
      return ImmutableMap.this.isPartialView();
    }

    @Override public int hashCode() {
      // ImmutableSet.of(value).hashCode() == value.hashCode(), so the hashes are the same
      return ImmutableMap.this.hashCode();
    }

    @Override boolean isHashCodeFast() {
      return ImmutableMap.this.isHashCodeFast();
    }

    @Override
    UnmodifiableIterator<Entry<K, ImmutableSet<V>>> entryIterator() {
      final Iterator<Entry<K, V>> backingIterator = ImmutableMap.this.entrySet().iterator();
      return new UnmodifiableIterator<Entry<K, ImmutableSet<V>>>() {
        @Override public boolean hasNext() {
          return backingIterator.hasNext();
        }

        @Override public Entry<K, ImmutableSet<V>> next() {
          final Entry<K, V> backingEntry = backingIterator.next();
          return new AbstractMapEntry<K, ImmutableSet<V>>() {
            @Override public K getKey() {
              return backingEntry.getKey();
            }

            @Override public ImmutableSet<V> getValue() {
              return ImmutableSet.of(backingEntry.getValue());
            }
          };
        }
      };
    }
  }

  @Override public boolean equals(@Nullable Object object) {
    return Maps.equalsImpl(this, object);
  }

  abstract boolean isPartialView();

  @Override public int hashCode() {
    return Sets.hashCodeImpl(entrySet());
  }

  boolean isHashCodeFast() {
    return false;
  }

  @Override public String toString() {
    return Maps.toStringImpl(this);
  }

  /**
   * Serialized type for all ImmutableMap instances. It captures the logical
   * contents and they are reconstructed using public factory methods. This
   * ensures that the implementation types remain as implementation details.
   */
  static class SerializedForm implements Serializable {
    private final Object[] keys;
    private final Object[] values;
    SerializedForm(ImmutableMap<?, ?> map) {
      keys = new Object[map.size()];
      values = new Object[map.size()];
      int i = 0;
      for (Entry<?, ?> entry : map.entrySet()) {
        keys[i] = entry.getKey();
        values[i] = entry.getValue();
        i++;
      }
    }
    Object readResolve() {
      Builder<Object, Object> builder = new Builder<Object, Object>();
      return createMap(builder);
    }
    Object createMap(Builder<Object, Object> builder) {
      for (int i = 0; i < keys.length; i++) {
        builder.put(keys[i], values[i]);
      }
      return builder.build();
    }
    private static final long serialVersionUID = 0;
  }

  Object writeReplace() {
    return new SerializedForm(this);
  }
}

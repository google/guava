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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.CollectPreconditions.checkEntryNotNull;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.DoNotCall;
import com.google.errorprone.annotations.DoNotMock;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.j2objc.annotations.RetainedWith;
import com.google.j2objc.annotations.WeakOuter;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link Map} whose contents will never change, with many other important properties detailed at
 * {@link ImmutableCollection}.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/ImmutableCollectionsExplained"> immutable collections</a>.
 *
 * @author Jesse Wilson
 * @author Kevin Bourrillion
 * @since 2.0
 */
@DoNotMock("Use ImmutableMap.of or another implementation")
@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // we're overriding default serialization
@ElementTypesAreNonnullByDefault
public abstract class ImmutableMap<K, V> implements Map<K, V>, Serializable {

  /**
   * Returns the empty map. This map behaves and performs comparably to {@link
   * Collections#emptyMap}, and is preferable mainly for consistency and maintainability of your
   * code.
   *
   * <p><b>Performance note:</b> the instance returned is a singleton.
   */
  @SuppressWarnings("unchecked")
  public static <K, V> ImmutableMap<K, V> of() {
    return (ImmutableMap<K, V>) RegularImmutableMap.EMPTY;
  }

  /**
   * Returns an immutable map containing a single entry. This map behaves and performs comparably to
   * {@link Collections#singletonMap} but will not accept a null key or value. It is preferable
   * mainly for consistency and maintainability of your code.
   */
  public static <K, V> ImmutableMap<K, V> of(K k1, V v1) {
    checkEntryNotNull(k1, v1);
    return RegularImmutableMap.create(1, new Object[] {k1, v1});
  }

  /**
   * Returns an immutable map containing the given entries, in order.
   *
   * @throws IllegalArgumentException if duplicate keys are provided
   */
  public static <K, V> ImmutableMap<K, V> of(K k1, V v1, K k2, V v2) {
    checkEntryNotNull(k1, v1);
    checkEntryNotNull(k2, v2);
    return RegularImmutableMap.create(2, new Object[] {k1, v1, k2, v2});
  }

  /**
   * Returns an immutable map containing the given entries, in order.
   *
   * @throws IllegalArgumentException if duplicate keys are provided
   */
  public static <K, V> ImmutableMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
    checkEntryNotNull(k1, v1);
    checkEntryNotNull(k2, v2);
    checkEntryNotNull(k3, v3);
    return RegularImmutableMap.create(3, new Object[] {k1, v1, k2, v2, k3, v3});
  }

  /**
   * Returns an immutable map containing the given entries, in order.
   *
   * @throws IllegalArgumentException if duplicate keys are provided
   */
  public static <K, V> ImmutableMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
    checkEntryNotNull(k1, v1);
    checkEntryNotNull(k2, v2);
    checkEntryNotNull(k3, v3);
    checkEntryNotNull(k4, v4);
    return RegularImmutableMap.create(4, new Object[] {k1, v1, k2, v2, k3, v3, k4, v4});
  }

  /**
   * Returns an immutable map containing the given entries, in order.
   *
   * @throws IllegalArgumentException if duplicate keys are provided
   */
  public static <K, V> ImmutableMap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
    checkEntryNotNull(k1, v1);
    checkEntryNotNull(k2, v2);
    checkEntryNotNull(k3, v3);
    checkEntryNotNull(k4, v4);
    checkEntryNotNull(k5, v5);
    return RegularImmutableMap.create(5, new Object[] {k1, v1, k2, v2, k3, v3, k4, v4, k5, v5});
  }

  // looking for of() with > 5 entries? Use the builder instead.

  /**
   * Verifies that {@code key} and {@code value} are non-null, and returns a new immutable entry
   * with those values.
   *
   * <p>A call to {@link Entry#setValue} on the returned entry will always throw {@link
   * UnsupportedOperationException}.
   */
  static <K, V> Entry<K, V> entryOf(K key, V value) {
    checkEntryNotNull(key, value);
    return new AbstractMap.SimpleImmutableEntry<>(key, value);
  }

  /**
   * Returns a new builder. The generated builder is equivalent to the builder created by the {@link
   * Builder} constructor.
   */
  public static <K, V> Builder<K, V> builder() {
    return new Builder<>();
  }

  /**
   * Returns a new builder, expecting the specified number of entries to be added.
   *
   * <p>If {@code expectedSize} is exactly the number of entries added to the builder before {@link
   * Builder#build} is called, the builder is likely to perform better than an unsized {@link
   * #builder()} would have.
   *
   * <p>It is not specified if any performance benefits apply if {@code expectedSize} is close to,
   * but not exactly, the number of entries added to the builder.
   *
   * @since 23.1
   */
  @Beta
  public static <K, V> Builder<K, V> builderWithExpectedSize(int expectedSize) {
    checkNonnegative(expectedSize, "expectedSize");
    return new Builder<>(expectedSize);
  }

  static void checkNoConflict(
      boolean safe, String conflictDescription, Entry<?, ?> entry1, Entry<?, ?> entry2) {
    if (!safe) {
      throw conflictException(conflictDescription, entry1, entry2);
    }
  }

  static IllegalArgumentException conflictException(
      String conflictDescription, Object entry1, Object entry2) {
    return new IllegalArgumentException(
        "Multiple entries with same " + conflictDescription + ": " + entry1 + " and " + entry2);
  }

  /**
   * A builder for creating immutable map instances, especially {@code public static final} maps
   * ("constant maps"). Example:
   *
   * <pre>{@code
   * static final ImmutableMap<String, Integer> WORD_TO_INT =
   *     new ImmutableMap.Builder<String, Integer>()
   *         .put("one", 1)
   *         .put("two", 2)
   *         .put("three", 3)
   *         .build();
   * }</pre>
   *
   * <p>For <i>small</i> immutable maps, the {@code ImmutableMap.of()} methods are even more
   * convenient.
   *
   * <p>By default, a {@code Builder} will generate maps that iterate over entries in the order they
   * were inserted into the builder, equivalently to {@code LinkedHashMap}. For example, in the
   * above example, {@code WORD_TO_INT.entrySet()} is guaranteed to iterate over the entries in the
   * order {@code "one"=1, "two"=2, "three"=3}, and {@code keySet()} and {@code values()} respect
   * the same order. If you want a different order, consider using {@link ImmutableSortedMap} to
   * sort by keys, or call {@link #orderEntriesByValue(Comparator)}, which changes this builder to
   * sort entries by value.
   *
   * <p>Builder instances can be reused - it is safe to call {@link #build} multiple times to build
   * multiple maps in series. Each map is a superset of the maps created before it.
   *
   * @since 2.0
   */
  @DoNotMock
  public static class Builder<K, V> {
    @CheckForNull Comparator<? super V> valueComparator;
    @Nullable Object[] alternatingKeysAndValues;
    int size;
    boolean entriesUsed;

    /**
     * Creates a new builder. The returned builder is equivalent to the builder generated by {@link
     * ImmutableMap#builder}.
     */
    public Builder() {
      this(ImmutableCollection.Builder.DEFAULT_INITIAL_CAPACITY);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    Builder(int initialCapacity) {
      this.alternatingKeysAndValues = new @Nullable Object[2 * initialCapacity];
      this.size = 0;
      this.entriesUsed = false;
    }

    private void ensureCapacity(int minCapacity) {
      if (minCapacity * 2 > alternatingKeysAndValues.length) {
        alternatingKeysAndValues =
            Arrays.copyOf(
                alternatingKeysAndValues,
                ImmutableCollection.Builder.expandedCapacity(
                    alternatingKeysAndValues.length, minCapacity * 2));
        entriesUsed = false;
      }
    }

    /**
     * Associates {@code key} with {@code value} in the built map. Duplicate keys are not allowed,
     * and will cause {@link #build} to fail.
     */
    @CanIgnoreReturnValue
    public Builder<K, V> put(K key, V value) {
      ensureCapacity(size + 1);
      checkEntryNotNull(key, value);
      alternatingKeysAndValues[2 * size] = key;
      alternatingKeysAndValues[2 * size + 1] = value;
      size++;
      return this;
    }

    /**
     * Adds the given {@code entry} to the map, making it immutable if necessary. Duplicate keys are
     * not allowed, and will cause {@link #build} to fail.
     *
     * @since 11.0
     */
    @CanIgnoreReturnValue
    public Builder<K, V> put(Entry<? extends K, ? extends V> entry) {
      return put(entry.getKey(), entry.getValue());
    }

    /**
     * Associates all of the given map's keys and values in the built map. Duplicate keys are not
     * allowed, and will cause {@link #build} to fail.
     *
     * @throws NullPointerException if any key or value in {@code map} is null
     */
    @CanIgnoreReturnValue
    public Builder<K, V> putAll(Map<? extends K, ? extends V> map) {
      return putAll(map.entrySet());
    }

    /**
     * Adds all of the given entries to the built map. Duplicate keys are not allowed, and will
     * cause {@link #build} to fail.
     *
     * @throws NullPointerException if any key, value, or entry is null
     * @since 19.0
     */
    @CanIgnoreReturnValue
    @Beta
    public Builder<K, V> putAll(Iterable<? extends Entry<? extends K, ? extends V>> entries) {
      if (entries instanceof Collection) {
        ensureCapacity(size + ((Collection<?>) entries).size());
      }
      for (Entry<? extends K, ? extends V> entry : entries) {
        put(entry);
      }
      return this;
    }

    /**
     * Configures this {@code Builder} to order entries by value according to the specified
     * comparator.
     *
     * <p>The sort order is stable, that is, if two entries have values that compare as equivalent,
     * the entry that was inserted first will be first in the built map's iteration order.
     *
     * @throws IllegalStateException if this method was already called
     * @since 19.0
     */
    @CanIgnoreReturnValue
    @Beta
    public Builder<K, V> orderEntriesByValue(Comparator<? super V> valueComparator) {
      checkState(this.valueComparator == null, "valueComparator was already set");
      this.valueComparator = checkNotNull(valueComparator, "valueComparator");
      return this;
    }

    @CanIgnoreReturnValue
    Builder<K, V> combine(Builder<K, V> other) {
      checkNotNull(other);
      ensureCapacity(this.size + other.size);
      System.arraycopy(
          other.alternatingKeysAndValues,
          0,
          this.alternatingKeysAndValues,
          this.size * 2,
          other.size * 2);
      this.size += other.size;
      return this;
    }

    /*
     * TODO(kevinb): Should build() and the ImmutableBiMap & ImmutableSortedMap
     * versions throw an IllegalStateException instead?
     */

    /**
     * Returns a newly-created immutable map. The iteration order of the returned map is the order
     * in which entries were inserted into the builder, unless {@link #orderEntriesByValue} was
     * called, in which case entries are sorted by value.
     *
     * @throws IllegalArgumentException if duplicate keys were added
     */
    @SuppressWarnings("unchecked")
    public ImmutableMap<K, V> build() {
      /*
       * If entries is full, then this implementation may end up using the entries array
       * directly and writing over the entry objects with non-terminal entries, but this is
       * safe; if this Builder is used further, it will grow the entries array (so it can't
       * affect the original array), and future build() calls will always copy any entry
       * objects that cannot be safely reused.
       */
      sortEntries();
      entriesUsed = true;
      return RegularImmutableMap.create(size, alternatingKeysAndValues);
    }

    void sortEntries() {
      if (valueComparator != null) {
        if (entriesUsed) {
          alternatingKeysAndValues = Arrays.copyOf(alternatingKeysAndValues, 2 * size);
        }
        Entry<K, V>[] entries = new Entry[size];
        for (int i = 0; i < size; i++) {
          // requireNonNull is safe because the first `2*size` elements have been filled in.
          entries[i] =
              new AbstractMap.SimpleImmutableEntry<K, V>(
                  (K) requireNonNull(alternatingKeysAndValues[2 * i]),
                  (V) requireNonNull(alternatingKeysAndValues[2 * i + 1]));
        }
        Arrays.sort(
            entries, 0, size, Ordering.from(valueComparator).onResultOf(Maps.<V>valueFunction()));
        for (int i = 0; i < size; i++) {
          alternatingKeysAndValues[2 * i] = entries[i].getKey();
          alternatingKeysAndValues[2 * i + 1] = entries[i].getValue();
        }
      }
    }
  }

  /**
   * Returns an immutable map containing the same entries as {@code map}. The returned map iterates
   * over entries in the same order as the {@code entrySet} of the original map. If {@code map}
   * somehow contains entries with duplicate keys (for example, if it is a {@code SortedMap} whose
   * comparator is not <i>consistent with equals</i>), the results of this method are undefined.
   *
   * <p>Despite the method name, this method attempts to avoid actually copying the data when it is
   * safe to do so. The exact circumstances under which a copy will or will not be performed are
   * undocumented and subject to change.
   *
   * @throws NullPointerException if any key or value in {@code map} is null
   */
  public static <K, V> ImmutableMap<K, V> copyOf(Map<? extends K, ? extends V> map) {
    if ((map instanceof ImmutableMap) && !(map instanceof SortedMap)) {
      @SuppressWarnings("unchecked") // safe since map is not writable
      ImmutableMap<K, V> kvMap = (ImmutableMap<K, V>) map;
      if (!kvMap.isPartialView()) {
        return kvMap;
      }
    }
    return copyOf(map.entrySet());
  }

  /**
   * Returns an immutable map containing the specified entries. The returned map iterates over
   * entries in the same order as the original iterable.
   *
   * @throws NullPointerException if any key, value, or entry is null
   * @throws IllegalArgumentException if two entries have the same key
   * @since 19.0
   */
  @Beta
  public static <K, V> ImmutableMap<K, V> copyOf(
      Iterable<? extends Entry<? extends K, ? extends V>> entries) {
    int initialCapacity =
        (entries instanceof Collection)
            ? ((Collection<?>) entries).size()
            : ImmutableCollection.Builder.DEFAULT_INITIAL_CAPACITY;
    ImmutableMap.Builder<K, V> builder = new ImmutableMap.Builder<K, V>(initialCapacity);
    builder.putAll(entries);
    return builder.build();
  }

  static final Entry<?, ?>[] EMPTY_ENTRY_ARRAY = new Entry<?, ?>[0];

  abstract static class IteratorBasedImmutableMap<K, V> extends ImmutableMap<K, V> {
    abstract UnmodifiableIterator<Entry<K, V>> entryIterator();

    @Override
    ImmutableSet<K> createKeySet() {
      return new ImmutableMapKeySet<>(this);
    }

    @Override
    ImmutableSet<Entry<K, V>> createEntrySet() {
      class EntrySetImpl extends ImmutableMapEntrySet<K, V> {
        @Override
        ImmutableMap<K, V> map() {
          return IteratorBasedImmutableMap.this;
        }

        @Override
        public UnmodifiableIterator<Entry<K, V>> iterator() {
          return entryIterator();
        }
      }
      return new EntrySetImpl();
    }

    @Override
    ImmutableCollection<V> createValues() {
      return new ImmutableMapValues<>(this);
    }
  }

  ImmutableMap() {}

  /**
   * Guaranteed to throw an exception and leave the map unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @CanIgnoreReturnValue
  @Deprecated
  @Override
  @DoNotCall("Always throws UnsupportedOperationException")
  @CheckForNull
  public final V put(K k, V v) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the map unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @CanIgnoreReturnValue
  @Deprecated
  @Override
  @CheckForNull
  public final V remove(@CheckForNull Object o) {
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
  @DoNotCall("Always throws UnsupportedOperationException")
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
  @DoNotCall("Always throws UnsupportedOperationException")
  public final void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public boolean containsKey(@CheckForNull Object key) {
    return get(key) != null;
  }

  @Override
  public boolean containsValue(@CheckForNull Object value) {
    return values().contains(value);
  }

  // Overriding to mark it Nullable
  @Override
  @CheckForNull
  public abstract V get(@CheckForNull Object key);

  /**
   * {@inheritDoc}
   *
   * <p>See <a
   * href="https://developer.android.com/reference/java/util/Map.html#getOrDefault%28java.lang.Object,%20V%29">{@code
   * Map.getOrDefault}</a>.
   *
   * @since 23.5 (but since 21.0 in the JRE <a
   *     href="https://github.com/google/guava#guava-google-core-libraries-for-java">flavor</a>).
   *     Note that API Level 24 users can call this method with any version of Guava.
   */
  // @Override under Java 8 / API Level 24
  @CheckForNull
  public final V getOrDefault(@CheckForNull Object key, @CheckForNull V defaultValue) {
    V result = get(key);
    // TODO(b/192579700): Use a ternary once it no longer confuses our nullness checker.
    if (result != null) {
      return result;
    } else {
      return defaultValue;
    }
  }

  @LazyInit @RetainedWith @CheckForNull private transient ImmutableSet<Entry<K, V>> entrySet;

  /**
   * Returns an immutable set of the mappings in this map. The iteration order is specified by the
   * method used to create this map. Typically, this is insertion order.
   */
  @Override
  public ImmutableSet<Entry<K, V>> entrySet() {
    ImmutableSet<Entry<K, V>> result = entrySet;
    return (result == null) ? entrySet = createEntrySet() : result;
  }

  abstract ImmutableSet<Entry<K, V>> createEntrySet();

  @LazyInit @RetainedWith @CheckForNull private transient ImmutableSet<K> keySet;

  /**
   * Returns an immutable set of the keys in this map, in the same order that they appear in {@link
   * #entrySet}.
   */
  @Override
  public ImmutableSet<K> keySet() {
    ImmutableSet<K> result = keySet;
    return (result == null) ? keySet = createKeySet() : result;
  }

  /*
   * This could have a good default implementation of return new ImmutableKeySet<K, V>(this),
   * but ProGuard can't figure out how to eliminate that default when RegularImmutableMap
   * overrides it.
   */
  abstract ImmutableSet<K> createKeySet();

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

  @LazyInit @RetainedWith @CheckForNull private transient ImmutableCollection<V> values;

  /**
   * Returns an immutable collection of the values in this map, in the same order that they appear
   * in {@link #entrySet}.
   */
  @Override
  public ImmutableCollection<V> values() {
    ImmutableCollection<V> result = values;
    return (result == null) ? values = createValues() : result;
  }

  /*
   * This could have a good default implementation of {@code return new
   * ImmutableMapValues<K, V>(this)}, but ProGuard can't figure out how to eliminate that default
   * when RegularImmutableMap overrides it.
   */
  abstract ImmutableCollection<V> createValues();

  // cached so that this.multimapView().inverse() only computes inverse once
  @LazyInit @CheckForNull private transient ImmutableSetMultimap<K, V> multimapView;

  /**
   * Returns a multimap view of the map.
   *
   * @since 14.0
   */
  public ImmutableSetMultimap<K, V> asMultimap() {
    if (isEmpty()) {
      return ImmutableSetMultimap.of();
    }
    ImmutableSetMultimap<K, V> result = multimapView;
    return (result == null)
        ? (multimapView =
            new ImmutableSetMultimap<>(new MapViewOfValuesAsSingletonSets(), size(), null))
        : result;
  }

  @WeakOuter
  private final class MapViewOfValuesAsSingletonSets
      extends IteratorBasedImmutableMap<K, ImmutableSet<V>> {

    @Override
    public int size() {
      return ImmutableMap.this.size();
    }

    @Override
    ImmutableSet<K> createKeySet() {
      return ImmutableMap.this.keySet();
    }

    @Override
    public boolean containsKey(@CheckForNull Object key) {
      return ImmutableMap.this.containsKey(key);
    }

    @Override
    @CheckForNull
    public ImmutableSet<V> get(@CheckForNull Object key) {
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
    boolean isHashCodeFast() {
      return ImmutableMap.this.isHashCodeFast();
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

  @Override
  public boolean equals(@CheckForNull Object object) {
    return Maps.equalsImpl(this, object);
  }

  abstract boolean isPartialView();

  @Override
  public int hashCode() {
    return Sets.hashCodeImpl(entrySet());
  }

  boolean isHashCodeFast() {
    return false;
  }

  @Override
  public String toString() {
    return Maps.toStringImpl(this);
  }

  /**
   * Serialized type for all ImmutableMap instances. It captures the logical contents and they are
   * reconstructed using public factory methods. This ensures that the implementation types remain
   * as implementation details.
   */
  static class SerializedForm<K, V> implements Serializable {
    // This object retains references to collections returned by keySet() and value(). This saves
    // bytes when the both the map and its keySet or value collection are written to the same
    // instance of ObjectOutputStream.

    // TODO(b/160980469): remove support for the old serialization format after some time
    private static final boolean USE_LEGACY_SERIALIZATION = true;

    private final Object keys;
    private final Object values;

    SerializedForm(ImmutableMap<K, V> map) {
      if (USE_LEGACY_SERIALIZATION) {
        Object[] keys = new Object[map.size()];
        Object[] values = new Object[map.size()];
        int i = 0;
        // "extends Object" works around https://github.com/typetools/checker-framework/issues/3013
        for (Entry<? extends Object, ? extends Object> entry : map.entrySet()) {
          keys[i] = entry.getKey();
          values[i] = entry.getValue();
          i++;
        }
        this.keys = keys;
        this.values = values;
        return;
      }
      this.keys = map.keySet();
      this.values = map.values();
    }

    @SuppressWarnings("unchecked")
    final Object readResolve() {
      if (!(this.keys instanceof ImmutableSet)) {
        return legacyReadResolve();
      }

      ImmutableSet<K> keySet = (ImmutableSet<K>) this.keys;
      ImmutableCollection<V> values = (ImmutableCollection<V>) this.values;

      Builder<K, V> builder = makeBuilder(keySet.size());

      UnmodifiableIterator<K> keyIter = keySet.iterator();
      UnmodifiableIterator<V> valueIter = values.iterator();

      while (keyIter.hasNext()) {
        builder.put(keyIter.next(), valueIter.next());
      }

      return builder.build();
    }

    @SuppressWarnings("unchecked")
    final Object legacyReadResolve() {
      K[] keys = (K[]) this.keys;
      V[] values = (V[]) this.values;

      Builder<K, V> builder = makeBuilder(keys.length);

      for (int i = 0; i < keys.length; i++) {
        builder.put(keys[i], values[i]);
      }
      return builder.build();
    }

    /**
     * Returns a builder that builds the unserialized type. Subclasses should override this method.
     */
    Builder<K, V> makeBuilder(int size) {
      return new Builder<>(size);
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Returns a serializable form of this object. Non-public subclasses should not override this
   * method. Publicly-accessible subclasses must override this method and should return a subclass
   * of SerializedForm whose readResolve() method returns objects of the subclass type.
   */
  Object writeReplace() {
    return new SerializedForm<>(this);
  }
}

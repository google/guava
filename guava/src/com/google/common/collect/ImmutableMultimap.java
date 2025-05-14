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
import static com.google.common.collect.CollectPreconditions.checkEntryNotNull;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;
import static com.google.common.collect.Iterators.emptyIterator;
import static com.google.common.collect.Maps.immutableEntry;
import static java.lang.Math.max;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.DoNotCall;
import com.google.errorprone.annotations.DoNotMock;
import com.google.j2objc.annotations.Weak;
import com.google.j2objc.annotations.WeakOuter;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import org.jspecify.annotations.Nullable;

/**
 * A {@link Multimap} whose contents will never change, with many other important properties
 * detailed at {@link ImmutableCollection}.
 *
 * <p><b>Warning:</b> avoid <i>direct</i> usage of {@link ImmutableMultimap} as a type (as with
 * {@link Multimap} itself). Prefer subtypes such as {@link ImmutableSetMultimap} or {@link
 * ImmutableListMultimap}, which have well-defined {@link #equals} semantics, thus avoiding a common
 * source of bugs and confusion.
 *
 * <p><b>Note:</b> every {@link ImmutableMultimap} offers an {@link #inverse} view, so there is no
 * need for a distinct {@code ImmutableBiMultimap} type.
 *
 * <p><a id="iteration"></a>
 *
 * <p><b>Key-grouped iteration.</b> All view collections follow the same iteration order. In all
 * current implementations, the iteration order always keeps multiple entries with the same key
 * together. Any creation method that would customarily respect insertion order (such as {@link
 * #copyOf(Multimap)}) instead preserves key-grouped order by inserting entries for an existing key
 * immediately after the last entry having that key.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/ImmutableCollectionsExplained">immutable collections</a>.
 *
 * @author Jared Levy
 * @since 2.0
 */
@GwtCompatible(emulated = true)
public abstract class ImmutableMultimap<K, V> extends BaseImmutableMultimap<K, V>
    implements Serializable {

  /**
   * Returns an empty multimap.
   *
   * <p><b>Performance note:</b> the instance returned is a singleton.
   */
  public static <K, V> ImmutableMultimap<K, V> of() {
    return ImmutableListMultimap.of();
  }

  /** Returns an immutable multimap containing a single entry. */
  public static <K, V> ImmutableMultimap<K, V> of(K k1, V v1) {
    return ImmutableListMultimap.of(k1, v1);
  }

  /** Returns an immutable multimap containing the given entries, in order. */
  public static <K, V> ImmutableMultimap<K, V> of(K k1, V v1, K k2, V v2) {
    return ImmutableListMultimap.of(k1, v1, k2, v2);
  }

  /**
   * Returns an immutable multimap containing the given entries, in the "key-grouped" insertion
   * order described in the <a href="#iteration">class documentation</a>.
   */
  public static <K, V> ImmutableMultimap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
    return ImmutableListMultimap.of(k1, v1, k2, v2, k3, v3);
  }

  /**
   * Returns an immutable multimap containing the given entries, in the "key-grouped" insertion
   * order described in the <a href="#iteration">class documentation</a>.
   */
  public static <K, V> ImmutableMultimap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
    return ImmutableListMultimap.of(k1, v1, k2, v2, k3, v3, k4, v4);
  }

  /**
   * Returns an immutable multimap containing the given entries, in the "key-grouped" insertion
   * order described in the <a href="#iteration">class documentation</a>.
   */
  public static <K, V> ImmutableMultimap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
    return ImmutableListMultimap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
  }

  // looking for of() with > 5 entries? Use the builder instead.

  /**
   * Returns a new builder. The generated builder is equivalent to the builder created by the {@link
   * Builder} constructor.
   */
  public static <K, V> Builder<K, V> builder() {
    return new Builder<>();
  }

  /**
   * Returns a new builder with a hint for how many distinct keys are expected to be added. The
   * generated builder is equivalent to that returned by {@link #builder}, but may perform better if
   * {@code expectedKeys} is a good estimate.
   *
   * @throws IllegalArgumentException if {@code expectedKeys} is negative
   * @since 33.3.0
   */
  public static <K, V> Builder<K, V> builderWithExpectedKeys(int expectedKeys) {
    checkNonnegative(expectedKeys, "expectedKeys");
    return new Builder<>(expectedKeys);
  }

  /**
   * A builder for creating immutable multimap instances, especially {@code public static final}
   * multimaps ("constant multimaps"). Example:
   *
   * {@snippet :
   * static final Multimap<String, Integer> STRING_TO_INTEGER_MULTIMAP =
   *     new ImmutableMultimap.Builder<String, Integer>()
   *         .put("one", 1)
   *         .putAll("several", 1, 2, 3)
   *         .putAll("many", 1, 2, 3, 4, 5)
   *         .build();
   * }
   *
   * <p>Builder instances can be reused; it is safe to call {@link #build} multiple times to build
   * multiple multimaps in series. Each multimap contains the key-value mappings in the previously
   * created multimaps.
   *
   * @since 2.0
   */
  @DoNotMock
  public static class Builder<K, V> {
    @Nullable Map<K, ImmutableCollection.Builder<V>> builderMap;
    @Nullable Comparator<? super K> keyComparator;
    @Nullable Comparator<? super V> valueComparator;
    int expectedValuesPerKey = ImmutableCollection.Builder.DEFAULT_INITIAL_CAPACITY;

    /**
     * Creates a new builder. The returned builder is equivalent to the builder generated by {@link
     * ImmutableMultimap#builder}.
     */
    public Builder() {}

    /** Creates a new builder with a hint for the number of distinct keys. */
    Builder(int expectedKeys) {
      if (expectedKeys > 0) {
        builderMap = Platform.preservesInsertionOrderOnPutsMapWithExpectedSize(expectedKeys);
      }
      // otherwise, leave it null to be constructed lazily
    }

    Map<K, ImmutableCollection.Builder<V>> ensureBuilderMapNonNull() {
      Map<K, ImmutableCollection.Builder<V>> result = builderMap;
      if (result == null) {
        result = Platform.preservesInsertionOrderOnPutsMap();
        builderMap = result;
      }
      return result;
    }

    ImmutableCollection.Builder<V> newValueCollectionBuilderWithExpectedSize(int expectedSize) {
      return ImmutableList.builderWithExpectedSize(expectedSize);
    }

    /**
     * Provides a hint for how many values will be associated with each key newly added to the
     * builder after this call. This does not change semantics, but may improve performance if
     * {@code expectedValuesPerKey} is a good estimate.
     *
     * <p>This may be called more than once; each newly added key will use the most recent call to
     * {@link #expectedValuesPerKey} as its hint.
     *
     * @throws IllegalArgumentException if {@code expectedValuesPerKey} is negative
     * @since 33.3.0
     */
    @CanIgnoreReturnValue
    public Builder<K, V> expectedValuesPerKey(int expectedValuesPerKey) {
      checkNonnegative(expectedValuesPerKey, "expectedValuesPerKey");

      // Always presize to at least 1, since we only bother creating a value collection if there's
      // at least one element.
      this.expectedValuesPerKey = max(expectedValuesPerKey, 1);

      return this;
    }

    /**
     * By default, if we are handed a value collection bigger than expectedValuesPerKey, presize to
     * accept that many elements.
     *
     * <p>This gets overridden in ImmutableSetMultimap.Builder to only trust the size of {@code
     * values} if it is a Set and therefore probably already deduplicated.
     */
    int expectedValueCollectionSize(int defaultExpectedValues, Iterable<?> values) {
      if (values instanceof Collection<?>) {
        Collection<?> collection = (Collection<?>) values;
        return max(defaultExpectedValues, collection.size());
      } else {
        return defaultExpectedValues;
      }
    }

    /** Adds a key-value mapping to the built multimap. */
    @CanIgnoreReturnValue
    public Builder<K, V> put(K key, V value) {
      checkEntryNotNull(key, value);
      ImmutableCollection.Builder<V> valuesBuilder = ensureBuilderMapNonNull().get(key);
      if (valuesBuilder == null) {
        valuesBuilder = newValueCollectionBuilderWithExpectedSize(expectedValuesPerKey);
        ensureBuilderMapNonNull().put(key, valuesBuilder);
      }
      valuesBuilder.add(value);
      return this;
    }

    /**
     * Adds an entry to the built multimap.
     *
     * @since 11.0
     */
    @CanIgnoreReturnValue
    public Builder<K, V> put(Entry<? extends K, ? extends V> entry) {
      return put(entry.getKey(), entry.getValue());
    }

    /**
     * Adds entries to the built multimap.
     *
     * @since 19.0
     */
    @CanIgnoreReturnValue
    public Builder<K, V> putAll(Iterable<? extends Entry<? extends K, ? extends V>> entries) {
      for (Entry<? extends K, ? extends V> entry : entries) {
        put(entry);
      }
      return this;
    }

    /**
     * Stores a collection of values with the same key in the built multimap.
     *
     * @throws NullPointerException if {@code key}, {@code values}, or any element in {@code values}
     *     is null. The builder is left in an invalid state.
     */
    @CanIgnoreReturnValue
    public Builder<K, V> putAll(K key, Iterable<? extends V> values) {
      if (key == null) {
        throw new NullPointerException("null key in entry: null=" + Iterables.toString(values));
      }
      Iterator<? extends V> valuesItr = values.iterator();
      if (!valuesItr.hasNext()) {
        return this;
      }
      ImmutableCollection.Builder<V> valuesBuilder = ensureBuilderMapNonNull().get(key);
      if (valuesBuilder == null) {
        valuesBuilder =
            newValueCollectionBuilderWithExpectedSize(
                expectedValueCollectionSize(expectedValuesPerKey, values));
        ensureBuilderMapNonNull().put(key, valuesBuilder);
      }
      while (valuesItr.hasNext()) {
        V value = valuesItr.next();
        checkEntryNotNull(key, value);
        valuesBuilder.add(value);
      }
      return this;
    }

    /**
     * Stores an array of values with the same key in the built multimap.
     *
     * @throws NullPointerException if the key or any value is null. The builder is left in an
     *     invalid state.
     */
    @CanIgnoreReturnValue
    public Builder<K, V> putAll(K key, V... values) {
      return putAll(key, asList(values));
    }

    /**
     * Stores another multimap's entries in the built multimap. The generated multimap's key and
     * value orderings correspond to the iteration ordering of the {@code multimap.asMap()} view,
     * with new keys and values following any existing keys and values.
     *
     * @throws NullPointerException if any key or value in {@code multimap} is null. The builder is
     *     left in an invalid state.
     */
    @CanIgnoreReturnValue
    public Builder<K, V> putAll(Multimap<? extends K, ? extends V> multimap) {
      for (Entry<? extends K, ? extends Collection<? extends V>> entry :
          multimap.asMap().entrySet()) {
        putAll(entry.getKey(), entry.getValue());
      }
      return this;
    }

    /**
     * Specifies the ordering of the generated multimap's keys.
     *
     * @since 8.0
     */
    @CanIgnoreReturnValue
    public Builder<K, V> orderKeysBy(Comparator<? super K> keyComparator) {
      this.keyComparator = checkNotNull(keyComparator);
      return this;
    }

    /**
     * Specifies the ordering of the generated multimap's values for each key.
     *
     * @since 8.0
     */
    @CanIgnoreReturnValue
    public Builder<K, V> orderValuesBy(Comparator<? super V> valueComparator) {
      this.valueComparator = checkNotNull(valueComparator);
      return this;
    }

    @CanIgnoreReturnValue
    Builder<K, V> combine(Builder<K, V> other) {
      if (other.builderMap != null) {
        for (Map.Entry<K, ImmutableCollection.Builder<V>> entry : other.builderMap.entrySet()) {
          putAll(entry.getKey(), entry.getValue().build());
        }
      }
      return this;
    }

    /** Returns a newly-created immutable multimap. */
    public ImmutableMultimap<K, V> build() {
      if (builderMap == null) {
        return ImmutableListMultimap.of();
      }
      Collection<Map.Entry<K, ImmutableCollection.Builder<V>>> mapEntries = builderMap.entrySet();
      if (keyComparator != null) {
        mapEntries = Ordering.from(keyComparator).<K>onKeys().immutableSortedCopy(mapEntries);
      }
      return ImmutableListMultimap.fromMapBuilderEntries(mapEntries, valueComparator);
    }
  }

  /**
   * Returns an immutable multimap containing the same mappings as {@code multimap}, in the
   * "key-grouped" iteration order described in the class documentation.
   *
   * <p>Despite the method name, this method attempts to avoid actually copying the data when it is
   * safe to do so. The exact circumstances under which a copy will or will not be performed are
   * undocumented and subject to change.
   *
   * @throws NullPointerException if any key or value in {@code multimap} is null
   */
  public static <K, V> ImmutableMultimap<K, V> copyOf(Multimap<? extends K, ? extends V> multimap) {
    if (multimap instanceof ImmutableMultimap) {
      @SuppressWarnings("unchecked") // safe since multimap is not writable
      ImmutableMultimap<K, V> kvMultimap = (ImmutableMultimap<K, V>) multimap;
      if (!kvMultimap.isPartialView()) {
        return kvMultimap;
      }
    }
    return ImmutableListMultimap.copyOf(multimap);
  }

  /**
   * Returns an immutable multimap containing the specified entries. The returned multimap iterates
   * over keys in the order they were first encountered in the input, and the values for each key
   * are iterated in the order they were encountered.
   *
   * @throws NullPointerException if any key, value, or entry is null
   * @since 19.0
   */
  public static <K, V> ImmutableMultimap<K, V> copyOf(
      Iterable<? extends Entry<? extends K, ? extends V>> entries) {
    return ImmutableListMultimap.copyOf(entries);
  }

  final transient ImmutableMap<K, ? extends ImmutableCollection<V>> map;
  final transient int size;

  // These constants allow the deserialization code to set final fields. This
  // holder class makes sure they are not initialized unless an instance is
  // deserialized.
  @GwtIncompatible
  @J2ktIncompatible
  static class FieldSettersHolder {
    static final Serialization.FieldSetter<? super ImmutableMultimap<?, ?>> MAP_FIELD_SETTER =
        Serialization.getFieldSetter(ImmutableMultimap.class, "map");
    static final Serialization.FieldSetter<? super ImmutableMultimap<?, ?>> SIZE_FIELD_SETTER =
        Serialization.getFieldSetter(ImmutableMultimap.class, "size");
  }

  ImmutableMultimap(ImmutableMap<K, ? extends ImmutableCollection<V>> map, int size) {
    this.map = map;
    this.size = size;
  }

  // mutators (not supported)

  /**
   * Guaranteed to throw an exception and leave the multimap unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @CanIgnoreReturnValue
  @Deprecated
  @Override
  @DoNotCall("Always throws UnsupportedOperationException")
  // DoNotCall wants this to be final, but we want to override it to return more specific types.
  // Inheritance is closed, and all subtypes are @DoNotCall, so this is safe to suppress.
  @SuppressWarnings("DoNotCall")
  public ImmutableCollection<V> removeAll(@Nullable Object key) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the multimap unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @CanIgnoreReturnValue
  @Deprecated
  @Override
  @DoNotCall("Always throws UnsupportedOperationException")
  // DoNotCall wants this to be final, but we want to override it to return more specific types.
  // Inheritance is closed, and all subtypes are @DoNotCall, so this is safe to suppress.
  @SuppressWarnings("DoNotCall")
  public ImmutableCollection<V> replaceValues(K key, Iterable<? extends V> values) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the multimap unmodified.
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

  /**
   * Returns an immutable collection of the values for the given key. If no mappings in the multimap
   * have the provided key, an empty immutable collection is returned. The values are in the same
   * order as the parameters used to build this multimap.
   */
  @Override
  public abstract ImmutableCollection<V> get(K key);

  /**
   * Returns an immutable multimap which is the inverse of this one. For every key-value mapping in
   * the original, the result will have a mapping with key and value reversed.
   *
   * @since 11.0
   */
  public abstract ImmutableMultimap<V, K> inverse();

  /**
   * Guaranteed to throw an exception and leave the multimap unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @CanIgnoreReturnValue
  @Deprecated
  @Override
  @DoNotCall("Always throws UnsupportedOperationException")
  public final boolean put(K key, V value) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the multimap unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @CanIgnoreReturnValue
  @Deprecated
  @Override
  @DoNotCall("Always throws UnsupportedOperationException")
  public final boolean putAll(K key, Iterable<? extends V> values) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the multimap unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @CanIgnoreReturnValue
  @Deprecated
  @Override
  @DoNotCall("Always throws UnsupportedOperationException")
  public final boolean putAll(Multimap<? extends K, ? extends V> multimap) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the multimap unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @CanIgnoreReturnValue
  @Deprecated
  @Override
  @DoNotCall("Always throws UnsupportedOperationException")
  public final boolean remove(@Nullable Object key, @Nullable Object value) {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns {@code true} if this immutable multimap's implementation contains references to
   * user-created objects that aren't accessible via this multimap's methods. This is generally used
   * to determine whether {@code copyOf} implementations should make an explicit copy to avoid
   * memory leaks.
   */
  boolean isPartialView() {
    return map.isPartialView();
  }

  // accessors

  @Override
  public boolean containsKey(@Nullable Object key) {
    return map.containsKey(key);
  }

  @Override
  public boolean containsValue(@Nullable Object value) {
    return value != null && super.containsValue(value);
  }

  @Override
  public int size() {
    return size;
  }

  // views

  /**
   * Returns an immutable set of the distinct keys in this multimap, in the same order as they
   * appear in this multimap.
   */
  @Override
  public ImmutableSet<K> keySet() {
    return map.keySet();
  }

  @Override
  Set<K> createKeySet() {
    throw new AssertionError("unreachable");
  }

  /**
   * Returns an immutable map that associates each key with its corresponding values in the
   * multimap. Keys and values appear in the same order as in this multimap.
   */
  @Override
  @SuppressWarnings("unchecked") // a widening cast
  public ImmutableMap<K, Collection<V>> asMap() {
    return (ImmutableMap) map;
  }

  @Override
  Map<K, Collection<V>> createAsMap() {
    throw new AssertionError("should never be called");
  }

  /** Returns an immutable collection of all key-value pairs in the multimap. */
  @Override
  public ImmutableCollection<Entry<K, V>> entries() {
    return (ImmutableCollection<Entry<K, V>>) super.entries();
  }

  @Override
  ImmutableCollection<Entry<K, V>> createEntries() {
    return new EntryCollection<>(this);
  }

  private static class EntryCollection<K, V> extends ImmutableCollection<Entry<K, V>> {
    @Weak final ImmutableMultimap<K, V> multimap;

    EntryCollection(ImmutableMultimap<K, V> multimap) {
      this.multimap = multimap;
    }

    @Override
    public UnmodifiableIterator<Entry<K, V>> iterator() {
      return multimap.entryIterator();
    }

    @Override
    boolean isPartialView() {
      return multimap.isPartialView();
    }

    @Override
    public int size() {
      return multimap.size();
    }

    @Override
    public boolean contains(@Nullable Object object) {
      if (object instanceof Entry) {
        Entry<?, ?> entry = (Entry<?, ?>) object;
        return multimap.containsEntry(entry.getKey(), entry.getValue());
      }
      return false;
    }

    // redeclare to help optimizers with b/310253115
    @SuppressWarnings("RedundantOverride")
    @Override
    @J2ktIncompatible
    @GwtIncompatible
        Object writeReplace() {
      return super.writeReplace();
    }

    @GwtIncompatible @J2ktIncompatible private static final long serialVersionUID = 0;
  }

  @Override
  UnmodifiableIterator<Entry<K, V>> entryIterator() {
    return new UnmodifiableIterator<Entry<K, V>>() {
      final Iterator<? extends Entry<K, ? extends ImmutableCollection<V>>> asMapItr =
          map.entrySet().iterator();
      @Nullable K currentKey = null;
      Iterator<V> valueItr = emptyIterator();

      @Override
      public boolean hasNext() {
        return valueItr.hasNext() || asMapItr.hasNext();
      }

      @Override
      public Entry<K, V> next() {
        if (!valueItr.hasNext()) {
          Entry<K, ? extends ImmutableCollection<V>> entry = asMapItr.next();
          currentKey = entry.getKey();
          valueItr = entry.getValue().iterator();
        }
        /*
         * requireNonNull is safe: The first call to this method always enters the !hasNext() case
         * and populates currentKey, after which it's never cleared.
         */
        return immutableEntry(requireNonNull(currentKey), valueItr.next());
      }
    };
  }

  @Override
  Spliterator<Entry<K, V>> entrySpliterator() {
    return CollectSpliterators.flatMap(
        asMap().entrySet().spliterator(),
        keyToValueCollectionEntry -> {
          K key = keyToValueCollectionEntry.getKey();
          Collection<V> valueCollection = keyToValueCollectionEntry.getValue();
          return CollectSpliterators.map(
              valueCollection.spliterator(), (V value) -> immutableEntry(key, value));
        },
        Spliterator.SIZED | (this instanceof SetMultimap ? Spliterator.DISTINCT : 0),
        size());
  }

  @Override
  public void forEach(BiConsumer<? super K, ? super V> action) {
    checkNotNull(action);
    asMap()
        .forEach(
            (key, valueCollection) -> valueCollection.forEach(value -> action.accept(key, value)));
  }

  /**
   * Returns an immutable multiset containing all the keys in this multimap, in the same order and
   * with the same frequencies as they appear in this multimap; to get only a single occurrence of
   * each key, use {@link #keySet}.
   */
  @Override
  public ImmutableMultiset<K> keys() {
    return (ImmutableMultiset<K>) super.keys();
  }

  @Override
  ImmutableMultiset<K> createKeys() {
    return new Keys();
  }

  @SuppressWarnings("serial") // Uses writeReplace, not default serialization
  @WeakOuter
  class Keys extends ImmutableMultiset<K> {
    @Override
    public boolean contains(@Nullable Object object) {
      return containsKey(object);
    }

    @Override
    public int count(@Nullable Object element) {
      Collection<V> values = map.get(element);
      return (values == null) ? 0 : values.size();
    }

    @Override
    public ImmutableSet<K> elementSet() {
      return keySet();
    }

    @Override
    public int size() {
      return ImmutableMultimap.this.size();
    }

    @Override
    Multiset.Entry<K> getEntry(int index) {
      Map.Entry<K, ? extends Collection<V>> entry = map.entrySet().asList().get(index);
      return Multisets.immutableEntry(entry.getKey(), entry.getValue().size());
    }

    @Override
    boolean isPartialView() {
      return true;
    }

    @GwtIncompatible
    @J2ktIncompatible
    @Override
    Object writeReplace() {
      return new KeysSerializedForm(ImmutableMultimap.this);
    }

    @GwtIncompatible
    @J2ktIncompatible
    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
      throw new InvalidObjectException("Use KeysSerializedForm");
    }
  }

  @GwtIncompatible
  @J2ktIncompatible
  private static final class KeysSerializedForm implements Serializable {
    final ImmutableMultimap<?, ?> multimap;

    KeysSerializedForm(ImmutableMultimap<?, ?> multimap) {
      this.multimap = multimap;
    }

    Object readResolve() {
      return multimap.keys();
    }
  }

  /**
   * Returns an immutable collection of the values in this multimap. Its iterator traverses the
   * values for the first key, the values for the second key, and so on.
   */
  @Override
  public ImmutableCollection<V> values() {
    return (ImmutableCollection<V>) super.values();
  }

  @Override
  ImmutableCollection<V> createValues() {
    return new Values<>(this);
  }

  @Override
  UnmodifiableIterator<V> valueIterator() {
    return new UnmodifiableIterator<V>() {
      final Iterator<? extends ImmutableCollection<V>> valueCollectionItr = map.values().iterator();
      Iterator<V> valueItr = emptyIterator();

      @Override
      public boolean hasNext() {
        return valueItr.hasNext() || valueCollectionItr.hasNext();
      }

      @Override
      public V next() {
        if (!valueItr.hasNext()) {
          valueItr = valueCollectionItr.next().iterator();
        }
        return valueItr.next();
      }
    };
  }

  private static final class Values<K, V> extends ImmutableCollection<V> {
    @Weak private final transient ImmutableMultimap<K, V> multimap;

    Values(ImmutableMultimap<K, V> multimap) {
      this.multimap = multimap;
    }

    @Override
    public boolean contains(@Nullable Object object) {
      return multimap.containsValue(object);
    }

    @Override
    public UnmodifiableIterator<V> iterator() {
      return multimap.valueIterator();
    }

    @GwtIncompatible // not present in emulated superclass
    @Override
    int copyIntoArray(@Nullable Object[] dst, int offset) {
      for (ImmutableCollection<V> valueCollection : multimap.map.values()) {
        offset = valueCollection.copyIntoArray(dst, offset);
      }
      return offset;
    }

    @Override
    public int size() {
      return multimap.size();
    }

    @Override
    boolean isPartialView() {
      return true;
    }

    // redeclare to help optimizers with b/310253115
    @SuppressWarnings("RedundantOverride")
    @Override
    @J2ktIncompatible
    @GwtIncompatible
        Object writeReplace() {
      return super.writeReplace();
    }

    @GwtIncompatible @J2ktIncompatible private static final long serialVersionUID = 0;
  }

  @GwtIncompatible @J2ktIncompatible private static final long serialVersionUID = 0;
}

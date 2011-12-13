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

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.annotation.Nullable;

/**
 * An immutable {@link Multimap}. Does not permit null keys or values.
 *
 * <p>Unlike {@link Multimaps#unmodifiableMultimap(Multimap)}, which is
 * a <i>view</i> of a separate multimap which can still change, an instance of
 * {@code ImmutableMultimap} contains its own data and will <i>never</i>
 * change. {@code ImmutableMultimap} is convenient for
 * {@code public static final} multimaps ("constant multimaps") and also lets
 * you easily make a "defensive copy" of a multimap provided to your class by
 * a caller.
 *
 * <p><b>Note:</b> Although this class is not final, it cannot be subclassed as
 * it has no public or protected constructors. Thus, instances of this class
 * are guaranteed to be immutable.
 *
 * <p>In addition to methods defined by {@link Multimap}, an {@link #inverse}
 * method is also supported.
 *
 * @author Jared Levy
 * @since 2.0 (imported from Google Collections Library)
 */
@GwtCompatible(emulated = true)
// TODO(user): If BiMultimap graduates from labs, this class should implement it.
public abstract class ImmutableMultimap<K, V>
    implements Multimap<K, V>, Serializable {

  /** Returns an empty multimap. */
  public static <K, V> ImmutableMultimap<K, V> of() {
    return ImmutableListMultimap.of();
  }

  /**
   * Returns an immutable multimap containing a single entry.
   */
  public static <K, V> ImmutableMultimap<K, V> of(K k1, V v1) {
    return ImmutableListMultimap.of(k1, v1);
  }

  /**
   * Returns an immutable multimap containing the given entries, in order.
   */
  public static <K, V> ImmutableMultimap<K, V> of(K k1, V v1, K k2, V v2) {
    return ImmutableListMultimap.of(k1, v1, k2, v2);
  }

  /**
   * Returns an immutable multimap containing the given entries, in order.
   */
  public static <K, V> ImmutableMultimap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3) {
    return ImmutableListMultimap.of(k1, v1, k2, v2, k3, v3);
  }

  /**
   * Returns an immutable multimap containing the given entries, in order.
   */
  public static <K, V> ImmutableMultimap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
    return ImmutableListMultimap.of(k1, v1, k2, v2, k3, v3, k4, v4);
  }

  /**
   * Returns an immutable multimap containing the given entries, in order.
   */
  public static <K, V> ImmutableMultimap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
    return ImmutableListMultimap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
  }

  // looking for of() with > 5 entries? Use the builder instead.

  /**
   * Returns a new builder. The generated builder is equivalent to the builder
   * created by the {@link Builder} constructor.
   */
  public static <K, V> Builder<K, V> builder() {
    return new Builder<K, V>();
  }

  /**
   * Multimap for {@link ImmutableMultimap.Builder} that maintains key and
   * value orderings, allows duplicate values, and performs better than
   * {@link LinkedListMultimap}.
   */
  private static class BuilderMultimap<K, V> extends AbstractMultimap<K, V> {
    BuilderMultimap() {
      super(new LinkedHashMap<K, Collection<V>>());
    }
    @Override Collection<V> createCollection() {
      return Lists.newArrayList();
    }
    private static final long serialVersionUID = 0;
  }

  /**
   * Multimap for {@link ImmutableMultimap.Builder} that sorts key and allows
   * duplicate values,
   */
  private static class SortedKeyBuilderMultimap<K, V>
      extends AbstractMultimap<K, V> {
    SortedKeyBuilderMultimap(
        Comparator<? super K> keyComparator, Multimap<K, V> multimap) {
      super(new TreeMap<K, Collection<V>>(keyComparator));
      putAll(multimap);
    }
    @Override Collection<V> createCollection() {
      return Lists.newArrayList();
    }
    private static final long serialVersionUID = 0;
  }

  /**
   * A builder for creating immutable multimap instances, especially
   * {@code public static final} multimaps ("constant multimaps"). Example:
   * <pre>   {@code
   *
   *   static final Multimap<String, Integer> STRING_TO_INTEGER_MULTIMAP =
   *       new ImmutableMultimap.Builder<String, Integer>()
   *           .put("one", 1)
   *           .putAll("several", 1, 2, 3)
   *           .putAll("many", 1, 2, 3, 4, 5)
   *           .build();}</pre>
   *
   * Builder instances can be reused; it is safe to call {@link #build} multiple
   * times to build multiple multimaps in series. Each multimap contains the
   * key-value mappings in the previously created multimaps.
   *
   * @since 2.0 (imported from Google Collections Library)
   */
  public static class Builder<K, V> {
    Multimap<K, V> builderMultimap = new BuilderMultimap<K, V>();
    Comparator<? super V> valueComparator;

    /**
     * Creates a new builder. The returned builder is equivalent to the builder
     * generated by {@link ImmutableMultimap#builder}.
     */
    public Builder() {}

    /**
     * Adds a key-value mapping to the built multimap.
     */
    public Builder<K, V> put(K key, V value) {
      builderMultimap.put(checkNotNull(key), checkNotNull(value));
      return this;
    }

    /**
     * Adds an entry to the built multimap.
     *
     * @since 11.0
     */
    public Builder<K, V> put(Entry<? extends K, ? extends V> entry) {
      builderMultimap.put(
          checkNotNull(entry.getKey()), checkNotNull(entry.getValue()));
      return this;
    }

    /**
     * Stores a collection of values with the same key in the built multimap.
     *
     * @throws NullPointerException if {@code key}, {@code values}, or any
     *     element in {@code values} is null. The builder is left in an invalid
     *     state.
     */
    public Builder<K, V> putAll(K key, Iterable<? extends V> values) {
      Collection<V> valueList = builderMultimap.get(checkNotNull(key));
      for (V value : values) {
        valueList.add(checkNotNull(value));
      }
      return this;
    }

    /**
     * Stores an array of values with the same key in the built multimap.
     *
     * @throws NullPointerException if the key or any value is null. The builder
     *     is left in an invalid state.
     */
    public Builder<K, V> putAll(K key, V... values) {
      return putAll(key, Arrays.asList(values));
    }

    /**
     * Stores another multimap's entries in the built multimap. The generated
     * multimap's key and value orderings correspond to the iteration ordering
     * of the {@code multimap.asMap()} view, with new keys and values following
     * any existing keys and values.
     *
     * @throws NullPointerException if any key or value in {@code multimap} is
     *     null. The builder is left in an invalid state.
     */
    public Builder<K, V> putAll(Multimap<? extends K, ? extends V> multimap) {
      for (Entry<? extends K, ? extends Collection<? extends V>> entry
          : multimap.asMap().entrySet()) {
        putAll(entry.getKey(), entry.getValue());
      }
      return this;
    }

    /**
     * Specifies the ordering of the generated multimap's keys.
     *
     * @since 8.0
     */
    @Beta
    public Builder<K, V> orderKeysBy(Comparator<? super K> keyComparator) {
      builderMultimap = new SortedKeyBuilderMultimap<K, V>(
          checkNotNull(keyComparator), builderMultimap);
      return this;
    }

    /**
     * Specifies the ordering of the generated multimap's values for each key.
     *
     * @since 8.0
     */
    @Beta
    public Builder<K, V> orderValuesBy(Comparator<? super V> valueComparator) {
      this.valueComparator = checkNotNull(valueComparator);
      return this;
    }

    /**
     * Returns a newly-created immutable multimap.
     */
    public ImmutableMultimap<K, V> build() {
      if (valueComparator != null) {
        for (Collection<V> values : builderMultimap.asMap().values()) {
          List<V> list = (List <V>) values;
          Collections.sort(list, valueComparator);
        }
      }
      return copyOf(builderMultimap);
    }
  }

  /**
   * Returns an immutable multimap containing the same mappings as {@code
   * multimap}. The generated multimap's key and value orderings correspond to
   * the iteration ordering of the {@code multimap.asMap()} view.
   *
   * <p>Despite the method name, this method attempts to avoid actually copying
   * the data when it is safe to do so. The exact circumstances under which a
   * copy will or will not be performed are undocumented and subject to change.
   *
   * @throws NullPointerException if any key or value in {@code multimap} is
   *         null
   */
  public static <K, V> ImmutableMultimap<K, V> copyOf(
      Multimap<? extends K, ? extends V> multimap) {
    if (multimap instanceof ImmutableMultimap) {
      @SuppressWarnings("unchecked") // safe since multimap is not writable
      ImmutableMultimap<K, V> kvMultimap
          = (ImmutableMultimap<K, V>) multimap;
      if (!kvMultimap.isPartialView()) {
        return kvMultimap;
      }
    }
    return ImmutableListMultimap.copyOf(multimap);
  }

  final transient ImmutableMap<K, ? extends ImmutableCollection<V>> map;
  final transient int size;

  // These constants allow the deserialization code to set final fields. This
  // holder class makes sure they are not initialized unless an instance is
  // deserialized.
  @GwtIncompatible("java serialization is not supported")
  static class FieldSettersHolder {
    static final Serialization.FieldSetter<ImmutableMultimap>
        MAP_FIELD_SETTER = Serialization.getFieldSetter(
        ImmutableMultimap.class, "map");
    static final Serialization.FieldSetter<ImmutableMultimap>
        SIZE_FIELD_SETTER = Serialization.getFieldSetter(
        ImmutableMultimap.class, "size");
  }

  ImmutableMultimap(ImmutableMap<K, ? extends ImmutableCollection<V>> map,
      int size) {
    this.map = map;
    this.size = size;
  }

  // mutators (not supported)

  /**
   * Guaranteed to throw an exception and leave the multimap unmodified.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public ImmutableCollection<V> removeAll(Object key) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the multimap unmodified.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public ImmutableCollection<V> replaceValues(K key,
      Iterable<? extends V> values) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the multimap unmodified.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns an immutable collection of the values for the given key.  If no
   * mappings in the multimap have the provided key, an empty immutable
   * collection is returned. The values are in the same order as the parameters
   * used to build this multimap.
   */
  @Override
  public abstract ImmutableCollection<V> get(K key);

  /**
   * Returns an immutable multimap which is the inverse of this one. For every
   * key-value mapping in the original, the result will have a mapping with
   * key and value reversed.
   *
   * @since 11
   */
  @Beta
  public abstract ImmutableMultimap<V, K> inverse();

  /**
   * Guaranteed to throw an exception and leave the multimap unmodified.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public boolean put(K key, V value) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the multimap unmodified.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public boolean putAll(K key, Iterable<? extends V> values) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the multimap unmodified.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the multimap unmodified.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public boolean remove(Object key, Object value) {
    throw new UnsupportedOperationException();
  }

  boolean isPartialView(){
    return map.isPartialView();
  }

  // accessors

  @Override
  public boolean containsEntry(@Nullable Object key, @Nullable Object value) {
    Collection<V> values = map.get(key);
    return values != null && values.contains(value);
  }

  @Override
  public boolean containsKey(@Nullable Object key) {
    return map.containsKey(key);
  }

  @Override
  public boolean containsValue(@Nullable Object value) {
    for (Collection<V> valueCollection : map.values()) {
      if (valueCollection.contains(value)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public int size() {
    return size;
  }

  @Override public boolean equals(@Nullable Object object) {
    if (object instanceof Multimap) {
      Multimap<?, ?> that = (Multimap<?, ?>) object;
      return this.map.equals(that.asMap());
    }
    return false;
  }

  @Override public int hashCode() {
    return map.hashCode();
  }

  @Override public String toString() {
    return map.toString();
  }

  // views

  /**
   * Returns an immutable set of the distinct keys in this multimap. These keys
   * are ordered according to when they first appeared during the construction
   * of this multimap.
   */
  @Override
  public ImmutableSet<K> keySet() {
    return map.keySet();
  }

  /**
   * Returns an immutable map that associates each key with its corresponding
   * values in the multimap.
   */
  @Override
  @SuppressWarnings("unchecked") // a widening cast
  public ImmutableMap<K, Collection<V>> asMap() {
    return (ImmutableMap) map;
  }

  private transient ImmutableCollection<Entry<K, V>> entries;

  /**
   * Returns an immutable collection of all key-value pairs in the multimap. Its
   * iterator traverses the values for the first key, the values for the second
   * key, and so on.
   */
  @Override
  public ImmutableCollection<Entry<K, V>> entries() {
    ImmutableCollection<Entry<K, V>> result = entries;
    return (result == null)
        ? (entries = new EntryCollection<K, V>(this)) : result;
  }

  private static class EntryCollection<K, V>
      extends ImmutableCollection<Entry<K, V>> {
    final ImmutableMultimap<K, V> multimap;

    EntryCollection(ImmutableMultimap<K, V> multimap) {
      this.multimap = multimap;
    }

    @Override public UnmodifiableIterator<Entry<K, V>> iterator() {
      final Iterator<? extends Entry<K, ? extends ImmutableCollection<V>>>
          mapIterator = this.multimap.map.entrySet().iterator();

      return new UnmodifiableIterator<Entry<K, V>>() {
        K key;
        Iterator<V> valueIterator;

        @Override
        public boolean hasNext() {
          return (key != null && valueIterator.hasNext())
              || mapIterator.hasNext();
        }

        @Override
        public Entry<K, V> next() {
          if (key == null || !valueIterator.hasNext()) {
            Entry<K, ? extends ImmutableCollection<V>> entry
                = mapIterator.next();
            key = entry.getKey();
            valueIterator = entry.getValue().iterator();
          }
          return Maps.immutableEntry(key, valueIterator.next());
        }
      };
    }

    @Override boolean isPartialView() {
      return multimap.isPartialView();
    }

    @Override
    public int size() {
      return multimap.size();
    }

    @Override public boolean contains(Object object) {
      if (object instanceof Entry) {
        Entry<?, ?> entry = (Entry<?, ?>) object;
        return multimap.containsEntry(entry.getKey(), entry.getValue());
      }
      return false;
    }

    private static final long serialVersionUID = 0;
  }

  private transient ImmutableMultiset<K> keys;

  /**
   * Returns a collection, which may contain duplicates, of all keys. The number
   * of times a key appears in the returned multiset equals the number of
   * mappings the key has in the multimap. Duplicate keys appear consecutively
   * in the multiset's iteration order.
   */
  @Override
  public ImmutableMultiset<K> keys() {
    ImmutableMultiset<K> result = keys;
    return (result == null) ? (keys = createKeys()) : result;
  }

  private ImmutableMultiset<K> createKeys() {
    ImmutableMultiset.Builder<K> builder = ImmutableMultiset.builder();
    for (Entry<K, ? extends ImmutableCollection<V>> entry
        : map.entrySet()) {
      builder.addCopies(entry.getKey(), entry.getValue().size());
    }
    return builder.build();
  }

  private transient ImmutableCollection<V> values;

  /**
   * Returns an immutable collection of the values in this multimap. Its
   * iterator traverses the values for the first key, the values for the second
   * key, and so on.
   */
  @Override
  public ImmutableCollection<V> values() {
    ImmutableCollection<V> result = values;
    return (result == null) ? (values = new Values<V>(this)) : result;
  }

  private static class Values<V> extends ImmutableCollection<V> {
    final ImmutableMultimap<?, V> multimap;

    Values(ImmutableMultimap<?, V> multimap) {
      this.multimap = multimap;
    }

    @Override public UnmodifiableIterator<V> iterator() {
      final Iterator<? extends Entry<?, V>> entryIterator
          = multimap.entries().iterator();
      return new UnmodifiableIterator<V>() {
        @Override
        public boolean hasNext() {
          return entryIterator.hasNext();
        }
        @Override
        public V next() {
          return entryIterator.next().getValue();
        }
      };
    }

    @Override
    public int size() {
      return multimap.size();
    }

    @Override boolean isPartialView() {
      return true;
    }

    private static final long serialVersionUID = 0;
  }

  private static final long serialVersionUID = 0;
}

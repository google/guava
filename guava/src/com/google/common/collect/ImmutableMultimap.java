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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
 * <a name="iteration"></a>
 * <p><b>Key-grouped iteration.</b> All view collections follow the same
 * iteration order. In all current implementations, the iteration order always
 * keeps multiple entries with the same key together. Any creation method that
 * would customarily respect insertion order (such as {@link #copyOf(Multimap)})
 * instead preserves key-grouped order by inserting entries for an existing key
 * immediately after the last entry having that key.
 *
 * <p><b>Note:</b> Although this class is not final, it cannot be subclassed as
 * it has no public or protected constructors. Thus, instances of this class
 * are guaranteed to be immutable.
 *
 * <p>In addition to methods defined by {@link Multimap}, an {@link #inverse}
 * method is also supported.
 *
 * <p>See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/ImmutableCollectionsExplained">
 * immutable collections</a>.
 *
 * @author Jared Levy
 * @since 2.0 (imported from Google Collections Library)
 */
@GwtCompatible(emulated = true)
public abstract class ImmutableMultimap<K, V> extends AbstractMultimap<K, V>
    implements Serializable {

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
   * Returns an immutable multimap containing the given entries, in the
   * "key-grouped" insertion order described in the
   * <a href="#iteration">class documentation</a>.
   */
  public static <K, V> ImmutableMultimap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3) {
    return ImmutableListMultimap.of(k1, v1, k2, v2, k3, v3);
  }

  /**
   * Returns an immutable multimap containing the given entries, in the
   * "key-grouped" insertion order described in the
   * <a href="#iteration">class documentation</a>.
   */
  public static <K, V> ImmutableMultimap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
    return ImmutableListMultimap.of(k1, v1, k2, v2, k3, v3, k4, v4);
  }

  /**
   * Returns an immutable multimap containing the given entries, in the
   * "key-grouped" insertion order described in the
   * <a href="#iteration">class documentation</a>.
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
  private static class BuilderMultimap<K, V> extends AbstractMapBasedMultimap<K, V> {
    BuilderMultimap() {
      super(new LinkedHashMap<K, Collection<V>>());
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
   * <p>Builder instances can be reused; it is safe to call {@link #build} multiple
   * times to build multiple multimaps in series. Each multimap contains the
   * key-value mappings in the previously created multimaps.
   *
   * @since 2.0 (imported from Google Collections Library)
   */
  public static class Builder<K, V> {
    Multimap<K, V> builderMultimap = new BuilderMultimap<K, V>();
    Comparator<? super K> keyComparator;
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
      checkEntryNotNull(key, value);
      builderMultimap.put(key, value);
      return this;
    }

    /**
     * Adds an entry to the built multimap.
     *
     * @since 11.0
     */
    public Builder<K, V> put(Entry<? extends K, ? extends V> entry) {
      return put(entry.getKey(), entry.getValue());
    }
    
    /**
     * Adds entries to the built multimap.
     * 
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

    /**
     * Stores a collection of values with the same key in the built multimap.
     *
     * @throws NullPointerException if {@code key}, {@code values}, or any
     *     element in {@code values} is null. The builder is left in an invalid
     *     state.
     */
    public Builder<K, V> putAll(K key, Iterable<? extends V> values) {
      if (key == null) {
        throw new NullPointerException(
            "null key in entry: null=" + Iterables.toString(values));
      }
      Collection<V> valueList = builderMultimap.get(key);
      for (V value : values) {
        checkEntryNotNull(key, value);
        valueList.add(value);
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
    public Builder<K, V> orderKeysBy(Comparator<? super K> keyComparator) {
      this.keyComparator = checkNotNull(keyComparator);
      return this;
    }

    /**
     * Specifies the ordering of the generated multimap's values for each key.
     *
     * @since 8.0
     */
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
      if (keyComparator != null) {
        Multimap<K, V> sortedCopy = new BuilderMultimap<K, V>();
        List<Map.Entry<K, Collection<V>>> entries =
            Ordering.from(keyComparator).<K>onKeys().immutableSortedCopy(
                builderMultimap.asMap().entrySet());
        for (Map.Entry<K, Collection<V>> entry : entries) {
          sortedCopy.putAll(entry.getKey(), entry.getValue());
        }
        builderMultimap = sortedCopy;
      }
      return copyOf(builderMultimap);
    }
  }

  /**
   * Returns an immutable multimap containing the same mappings as {@code
   * multimap}, in the "key-grouped" iteration order described in the class
   * documentation.
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
  

  /**
   * Returns an immutable multimap containing the specified entries.  The
   * returned multimap iterates over keys in the order they were first
   * encountered in the input, and the values for each key are iterated in the
   * order they were encountered.
   *
   * @throws NullPointerException if any key, value, or entry is null
   * @since 19.0
   */
  @Beta
  public static <K, V> ImmutableMultimap<K, V> copyOf(
      Iterable<? extends Entry<? extends K, ? extends V>> entries) {
    return ImmutableListMultimap.copyOf(entries);
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
    static final Serialization.FieldSetter<ImmutableSetMultimap>
        EMPTY_SET_FIELD_SETTER = Serialization.getFieldSetter(
        ImmutableSetMultimap.class, "emptySet");
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
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  public ImmutableCollection<V> removeAll(Object key) {
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
  public ImmutableCollection<V> replaceValues(K key,
      Iterable<? extends V> values) {
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
   * @since 11.0
   */
  public abstract ImmutableMultimap<V, K> inverse();

  /**
   * Guaranteed to throw an exception and leave the multimap unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  public boolean put(K key, V value) {
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
  public boolean putAll(K key, Iterable<? extends V> values) {
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
  public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
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
  public boolean remove(Object key, Object value) {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns {@code true} if this immutable multimap's implementation contains references to
   * user-created objects that aren't accessible via this multimap's methods. This is generally
   * used to determine whether {@code copyOf} implementations should make an explicit copy to avoid
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
   * Returns an immutable set of the distinct keys in this multimap, in the same
   * order as they appear in this multimap.
   */
  @Override
  public ImmutableSet<K> keySet() {
    return map.keySet();
  }

  /**
   * Returns an immutable map that associates each key with its corresponding
   * values in the multimap. Keys and values appear in the same order as in this
   * multimap.
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

  /**
   * Returns an immutable collection of all key-value pairs in the multimap.
   */
  @Override
  public ImmutableCollection<Entry<K, V>> entries() {
    return (ImmutableCollection<Entry<K, V>>) super.entries();
  }
  
  @Override
  ImmutableCollection<Entry<K, V>> createEntries() {
    return new EntryCollection<K, V>(this);
  }

  private static class EntryCollection<K, V>
      extends ImmutableCollection<Entry<K, V>> {
    final ImmutableMultimap<K, V> multimap;

    EntryCollection(ImmutableMultimap<K, V> multimap) {
      this.multimap = multimap;
    }

    @Override public UnmodifiableIterator<Entry<K, V>> iterator() {
      return multimap.entryIterator();
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
  
  private abstract class Itr<T> extends UnmodifiableIterator<T> {
    final Iterator<Entry<K, Collection<V>>> mapIterator = asMap().entrySet().iterator();
    K key = null;
    Iterator<V> valueIterator = Iterators.emptyIterator();
    
    abstract T output(K key, V value);

    @Override
    public boolean hasNext() {
      return mapIterator.hasNext() || valueIterator.hasNext();
    }

    @Override
    public T next() {
      if (!valueIterator.hasNext()) {
        Entry<K, Collection<V>> mapEntry = mapIterator.next();
        key = mapEntry.getKey();
        valueIterator = mapEntry.getValue().iterator();
      }
      return output(key, valueIterator.next());
    }
  }
  
  @Override
  UnmodifiableIterator<Entry<K, V>> entryIterator() {
    return new Itr<Entry<K, V>>() {
      @Override
      Entry<K, V> output(K key, V value) {
        return Maps.immutableEntry(key, value);
      }
    };
  }

  /**
   * Returns an immutable multiset containing all the keys in this multimap, in
   * the same order and with the same frequencies as they appear in this
   * multimap; to get only a single occurrence of each key, use {@link #keySet}.
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
    public Set<K> elementSet() {
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
  }

  /**
   * Returns an immutable collection of the values in this multimap. Its
   * iterator traverses the values for the first key, the values for the second
   * key, and so on.
   */
  @Override
  public ImmutableCollection<V> values() {
    return (ImmutableCollection<V>) super.values();
  }
  
  @Override
  ImmutableCollection<V> createValues() {
    return new Values<K, V>(this);
  }

  @Override
  UnmodifiableIterator<V> valueIterator() {
    return new Itr<V>() {
      @Override
      V output(K key, V value) {
        return value;
      }
    };
  }

  private static final class Values<K, V> extends ImmutableCollection<V> {
    private transient final ImmutableMultimap<K, V> multimap;
    
    Values(ImmutableMultimap<K, V> multimap) {
      this.multimap = multimap;
    }

    @Override
    public boolean contains(@Nullable Object object) {
      return multimap.containsValue(object);
    }
    
    @Override public UnmodifiableIterator<V> iterator() {
      return multimap.valueIterator();
    }

    @GwtIncompatible("not present in emulated superclass")
    @Override
    int copyIntoArray(Object[] dst, int offset) {
      for (ImmutableCollection<V> valueCollection : multimap.map.values()) {
        offset = valueCollection.copyIntoArray(dst, offset);
      }
      return offset;
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

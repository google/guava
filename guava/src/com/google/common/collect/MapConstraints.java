/*
 * Copyright (C) 2007 The Guava Authors
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
import com.google.j2objc.annotations.WeakOuter;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;

import javax.annotation.Nullable;

/**
 * Factory and utilities pertaining to the {@code MapConstraint} interface.
 *
 * @see Constraints
 * @author Mike Bostock
 * @since 3.0
 * @deprecated Use {@link Preconditions} for basic checks. In place of
 *     constrained maps, we encourage you to check your preconditions
 *     explicitly instead of leaving that work to the map implementation.
 *     For the specific case of rejecting null, consider {@link ImmutableMap}.
 *     This class is scheduled for removal in Guava 20.0.
 */
@Beta
@GwtCompatible
@Deprecated
public final class MapConstraints {
  private MapConstraints() {}

  /**
   * Returns a constraint that verifies that neither the key nor the value is
   * null. If either is null, a {@link NullPointerException} is thrown.
   */
  public static MapConstraint<Object, Object> notNull() {
    return NotNullMapConstraint.INSTANCE;
  }

  // enum singleton pattern
  private enum NotNullMapConstraint implements MapConstraint<Object, Object> {
    INSTANCE;

    @Override
    public void checkKeyValue(Object key, Object value) {
      checkNotNull(key);
      checkNotNull(value);
    }

    @Override
    public String toString() {
      return "Not null";
    }
  }

  /**
   * Returns a constrained view of the specified map, using the specified
   * constraint. Any operations that add new mappings will call the provided
   * constraint. However, this method does not verify that existing mappings
   * satisfy the constraint.
   *
   * <p>The returned map is not serializable.
   *
   * @param map the map to constrain
   * @param constraint the constraint that validates added entries
   * @return a constrained view of the specified map
   */
  public static <K, V> Map<K, V> constrainedMap(
      Map<K, V> map, MapConstraint<? super K, ? super V> constraint) {
    return new ConstrainedMap<K, V>(map, constraint);
  }

  /**
   * Returns a constrained view of the specified multimap, using the specified
   * constraint. Any operations that add new mappings will call the provided
   * constraint. However, this method does not verify that existing mappings
   * satisfy the constraint.
   *
   * <p>Note that the generated multimap's {@link Multimap#removeAll} and
   * {@link Multimap#replaceValues} methods return collections that are not
   * constrained.
   *
   * <p>The returned multimap is not serializable.
   *
   * @param multimap the multimap to constrain
   * @param constraint the constraint that validates added entries
   * @return a constrained view of the multimap
   */
  public static <K, V> Multimap<K, V> constrainedMultimap(
      Multimap<K, V> multimap, MapConstraint<? super K, ? super V> constraint) {
    return new ConstrainedMultimap<K, V>(multimap, constraint);
  }

  /**
   * Returns a constrained view of the specified list multimap, using the
   * specified constraint. Any operations that add new mappings will call the
   * provided constraint. However, this method does not verify that existing
   * mappings satisfy the constraint.
   *
   * <p>Note that the generated multimap's {@link Multimap#removeAll} and
   * {@link Multimap#replaceValues} methods return collections that are not
   * constrained.
   *
   * <p>The returned multimap is not serializable.
   *
   * @param multimap the multimap to constrain
   * @param constraint the constraint that validates added entries
   * @return a constrained view of the specified multimap
   */
  public static <K, V> ListMultimap<K, V> constrainedListMultimap(
      ListMultimap<K, V> multimap, MapConstraint<? super K, ? super V> constraint) {
    return new ConstrainedListMultimap<K, V>(multimap, constraint);
  }

  /**
   * Returns a constrained view of the specified set multimap, using the
   * specified constraint. Any operations that add new mappings will call the
   * provided constraint. However, this method does not verify that existing
   * mappings satisfy the constraint.
   *
   * <p>Note that the generated multimap's {@link Multimap#removeAll} and
   * {@link Multimap#replaceValues} methods return collections that are not
   * constrained.
   * <p>The returned multimap is not serializable.
   *
   * @param multimap the multimap to constrain
   * @param constraint the constraint that validates added entries
   * @return a constrained view of the specified multimap
   */
  public static <K, V> SetMultimap<K, V> constrainedSetMultimap(
      SetMultimap<K, V> multimap, MapConstraint<? super K, ? super V> constraint) {
    return new ConstrainedSetMultimap<K, V>(multimap, constraint);
  }

  /**
   * Returns a constrained view of the specified sorted-set multimap, using the
   * specified constraint. Any operations that add new mappings will call the
   * provided constraint. However, this method does not verify that existing
   * mappings satisfy the constraint.
   *
   * <p>Note that the generated multimap's {@link Multimap#removeAll} and
   * {@link Multimap#replaceValues} methods return collections that are not
   * constrained.
   * <p>The returned multimap is not serializable.
   *
   * @param multimap the multimap to constrain
   * @param constraint the constraint that validates added entries
   * @return a constrained view of the specified multimap
   */
  public static <K, V> SortedSetMultimap<K, V> constrainedSortedSetMultimap(
      SortedSetMultimap<K, V> multimap, MapConstraint<? super K, ? super V> constraint) {
    return new ConstrainedSortedSetMultimap<K, V>(multimap, constraint);
  }

  /**
   * Returns a constrained view of the specified entry, using the specified
   * constraint. The {@link Entry#setValue} operation will be verified with the
   * constraint.
   *
   * @param entry the entry to constrain
   * @param constraint the constraint for the entry
   * @return a constrained view of the specified entry
   */
  private static <K, V> Entry<K, V> constrainedEntry(
      final Entry<K, V> entry, final MapConstraint<? super K, ? super V> constraint) {
    checkNotNull(entry);
    checkNotNull(constraint);
    return new ForwardingMapEntry<K, V>() {
      @Override
      protected Entry<K, V> delegate() {
        return entry;
      }

      @Override
      public V setValue(V value) {
        constraint.checkKeyValue(getKey(), value);
        return entry.setValue(value);
      }
    };
  }

  /**
   * Returns a constrained view of the specified {@code asMap} entry, using the
   * specified constraint. The {@link Entry#setValue} operation will be verified
   * with the constraint, and the collection returned by {@link Entry#getValue}
   * will be similarly constrained.
   *
   * @param entry the {@code asMap} entry to constrain
   * @param constraint the constraint for the entry
   * @return a constrained view of the specified entry
   */
  private static <K, V> Entry<K, Collection<V>> constrainedAsMapEntry(
      final Entry<K, Collection<V>> entry, final MapConstraint<? super K, ? super V> constraint) {
    checkNotNull(entry);
    checkNotNull(constraint);
    return new ForwardingMapEntry<K, Collection<V>>() {
      @Override
      protected Entry<K, Collection<V>> delegate() {
        return entry;
      }

      @Override
      public Collection<V> getValue() {
        return Constraints.constrainedTypePreservingCollection(
            entry.getValue(),
            new Constraint<V>() {
              @Override
              public V checkElement(V value) {
                constraint.checkKeyValue(getKey(), value);
                return value;
              }
            });
      }
    };
  }

  /**
   * Returns a constrained view of the specified set of {@code asMap} entries,
   * using the specified constraint. The {@link Entry#setValue} operation will
   * be verified with the constraint, and the collection returned by {@link
   * Entry#getValue} will be similarly constrained. The {@code add} and {@code
   * addAll} operations simply forward to the underlying set, which throws an
   * {@link UnsupportedOperationException} per the multimap specification.
   *
   * @param entries the entries to constrain
   * @param constraint the constraint for the entries
   * @return a constrained view of the entries
   */
  private static <K, V> Set<Entry<K, Collection<V>>> constrainedAsMapEntries(
      Set<Entry<K, Collection<V>>> entries, MapConstraint<? super K, ? super V> constraint) {
    return new ConstrainedAsMapEntries<K, V>(entries, constraint);
  }

  /**
   * Returns a constrained view of the specified collection (or set) of entries,
   * using the specified constraint. The {@link Entry#setValue} operation will
   * be verified with the constraint, along with add operations on the returned
   * collection. The {@code add} and {@code addAll} operations simply forward to
   * the underlying collection, which throws an {@link
   * UnsupportedOperationException} per the map and multimap specification.
   *
   * @param entries the entries to constrain
   * @param constraint the constraint for the entries
   * @return a constrained view of the specified entries
   */
  private static <K, V> Collection<Entry<K, V>> constrainedEntries(
      Collection<Entry<K, V>> entries, MapConstraint<? super K, ? super V> constraint) {
    if (entries instanceof Set) {
      return constrainedEntrySet((Set<Entry<K, V>>) entries, constraint);
    }
    return new ConstrainedEntries<K, V>(entries, constraint);
  }

  /**
   * Returns a constrained view of the specified set of entries, using the
   * specified constraint. The {@link Entry#setValue} operation will be verified
   * with the constraint, along with add operations on the returned set. The
   * {@code add} and {@code addAll} operations simply forward to the underlying
   * set, which throws an {@link UnsupportedOperationException} per the map and
   * multimap specification.
   *
   * <p>The returned multimap is not serializable.
   *
   * @param entries the entries to constrain
   * @param constraint the constraint for the entries
   * @return a constrained view of the specified entries
   */
  private static <K, V> Set<Entry<K, V>> constrainedEntrySet(
      Set<Entry<K, V>> entries, MapConstraint<? super K, ? super V> constraint) {
    return new ConstrainedEntrySet<K, V>(entries, constraint);
  }

  /** @see MapConstraints#constrainedMap */
  static class ConstrainedMap<K, V> extends ForwardingMap<K, V> {
    private final Map<K, V> delegate;
    final MapConstraint<? super K, ? super V> constraint;
    private transient Set<Entry<K, V>> entrySet;

    ConstrainedMap(Map<K, V> delegate, MapConstraint<? super K, ? super V> constraint) {
      this.delegate = checkNotNull(delegate);
      this.constraint = checkNotNull(constraint);
    }

    @Override
    protected Map<K, V> delegate() {
      return delegate;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
      Set<Entry<K, V>> result = entrySet;
      if (result == null) {
        entrySet = result = constrainedEntrySet(delegate.entrySet(), constraint);
      }
      return result;
    }

    @Override
    public V put(K key, V value) {
      constraint.checkKeyValue(key, value);
      return delegate.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
      delegate.putAll(checkMap(map, constraint));
    }
  }

  /**
   * Returns a constrained view of the specified bimap, using the specified
   * constraint. Any operations that modify the bimap will have the associated
   * keys and values verified with the constraint.
   *
   * <p>The returned bimap is not serializable.
   *
   * @param map the bimap to constrain
   * @param constraint the constraint that validates added entries
   * @return a constrained view of the specified bimap
   */
  public static <K, V> BiMap<K, V> constrainedBiMap(
      BiMap<K, V> map, MapConstraint<? super K, ? super V> constraint) {
    return new ConstrainedBiMap<K, V>(map, null, constraint);
  }

  /** @see MapConstraints#constrainedBiMap */
  private static class ConstrainedBiMap<K, V> extends ConstrainedMap<K, V> implements BiMap<K, V> {
    /*
     * We could switch to racy single-check lazy init and remove volatile, but
     * there's a downside. That's because this field is also written in the
     * constructor. Without volatile, the constructor's write of the existing
     * inverse BiMap could occur after inverse()'s read of the field's initial
     * null value, leading inverse() to overwrite the existing inverse with a
     * doubly indirect version. This wouldn't be catastrophic, but it's
     * something to keep in mind if we make the change.
     *
     * Note that UnmodifiableBiMap *does* use racy single-check lazy init.
     * TODO(cpovirk): pick one and standardize
     */
    volatile BiMap<V, K> inverse;

    ConstrainedBiMap(
        BiMap<K, V> delegate,
        @Nullable BiMap<V, K> inverse,
        MapConstraint<? super K, ? super V> constraint) {
      super(delegate, constraint);
      this.inverse = inverse;
    }

    @Override
    protected BiMap<K, V> delegate() {
      return (BiMap<K, V>) super.delegate();
    }

    @Override
    public V forcePut(K key, V value) {
      constraint.checkKeyValue(key, value);
      return delegate().forcePut(key, value);
    }

    @Override
    public BiMap<V, K> inverse() {
      if (inverse == null) {
        inverse = new ConstrainedBiMap<V, K>(
            delegate().inverse(), this, new InverseConstraint<V, K>(constraint));
      }
      return inverse;
    }

    @Override
    public Set<V> values() {
      return delegate().values();
    }
  }

  /** @see MapConstraints#constrainedBiMap */
  private static class InverseConstraint<K, V> implements MapConstraint<K, V> {
    final MapConstraint<? super V, ? super K> constraint;

    public InverseConstraint(MapConstraint<? super V, ? super K> constraint) {
      this.constraint = checkNotNull(constraint);
    }

    @Override
    public void checkKeyValue(K key, V value) {
      constraint.checkKeyValue(value, key);
    }
  }

  /** @see MapConstraints#constrainedMultimap */
  private static class ConstrainedMultimap<K, V> extends ForwardingMultimap<K, V>
      implements Serializable {
    final MapConstraint<? super K, ? super V> constraint;
    final Multimap<K, V> delegate;

    transient Collection<Entry<K, V>> entries;

    transient Map<K, Collection<V>> asMap;

    public ConstrainedMultimap(
        Multimap<K, V> delegate, MapConstraint<? super K, ? super V> constraint) {
      this.delegate = checkNotNull(delegate);
      this.constraint = checkNotNull(constraint);
    }

    @Override
    protected Multimap<K, V> delegate() {
      return delegate;
    }

    @Override
    public Map<K, Collection<V>> asMap() {
      Map<K, Collection<V>> result = asMap;
      if (result == null) {
        final Map<K, Collection<V>> asMapDelegate = delegate.asMap();

        @WeakOuter
        class AsMap extends ForwardingMap<K, Collection<V>> {
          Set<Entry<K, Collection<V>>> entrySet;
          Collection<Collection<V>> values;

          @Override
          protected Map<K, Collection<V>> delegate() {
            return asMapDelegate;
          }

          @Override
          public Set<Entry<K, Collection<V>>> entrySet() {
            Set<Entry<K, Collection<V>>> result = entrySet;
            if (result == null) {
              entrySet = result = constrainedAsMapEntries(asMapDelegate.entrySet(), constraint);
            }
            return result;
          }

          @SuppressWarnings("unchecked")
          @Override
          public Collection<V> get(Object key) {
            try {
              Collection<V> collection = ConstrainedMultimap.this.get((K) key);
              return collection.isEmpty() ? null : collection;
            } catch (ClassCastException e) {
              return null; // key wasn't a K
            }
          }

          @Override
          public Collection<Collection<V>> values() {
            Collection<Collection<V>> result = values;
            if (result == null) {
              values = result = new ConstrainedAsMapValues<K, V>(delegate().values(), entrySet());
            }
            return result;
          }

          @Override
          public boolean containsValue(Object o) {
            return values().contains(o);
          }
        }
        asMap = result = new AsMap();
      }
      return result;
    }

    @Override
    public Collection<Entry<K, V>> entries() {
      Collection<Entry<K, V>> result = entries;
      if (result == null) {
        entries = result = constrainedEntries(delegate.entries(), constraint);
      }
      return result;
    }

    @Override
    public Collection<V> get(final K key) {
      return Constraints.constrainedTypePreservingCollection(
          delegate.get(key),
          new Constraint<V>() {
            @Override
            public V checkElement(V value) {
              constraint.checkKeyValue(key, value);
              return value;
            }
          });
    }

    @Override
    public boolean put(K key, V value) {
      constraint.checkKeyValue(key, value);
      return delegate.put(key, value);
    }

    @Override
    public boolean putAll(K key, Iterable<? extends V> values) {
      return delegate.putAll(key, checkValues(key, values, constraint));
    }

    @Override
    public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
      boolean changed = false;
      for (Entry<? extends K, ? extends V> entry : multimap.entries()) {
        changed |= put(entry.getKey(), entry.getValue());
      }
      return changed;
    }

    @Override
    public Collection<V> replaceValues(K key, Iterable<? extends V> values) {
      return delegate.replaceValues(key, checkValues(key, values, constraint));
    }
  }

  /** @see ConstrainedMultimap#asMap */
  private static class ConstrainedAsMapValues<K, V> extends ForwardingCollection<Collection<V>> {
    final Collection<Collection<V>> delegate;
    final Set<Entry<K, Collection<V>>> entrySet;

    /**
     * @param entrySet map entries, linking each key with its corresponding
     *     values, that already enforce the constraint
     */
    ConstrainedAsMapValues(
        Collection<Collection<V>> delegate, Set<Entry<K, Collection<V>>> entrySet) {
      this.delegate = delegate;
      this.entrySet = entrySet;
    }

    @Override
    protected Collection<Collection<V>> delegate() {
      return delegate;
    }

    @Override
    public Iterator<Collection<V>> iterator() {
      final Iterator<Entry<K, Collection<V>>> iterator = entrySet.iterator();
      return new Iterator<Collection<V>>() {
        @Override
        public boolean hasNext() {
          return iterator.hasNext();
        }

        @Override
        public Collection<V> next() {
          return iterator.next().getValue();
        }

        @Override
        public void remove() {
          iterator.remove();
        }
      };
    }

    @Override
    public Object[] toArray() {
      return standardToArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
      return standardToArray(array);
    }

    @Override
    public boolean contains(Object o) {
      return standardContains(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      return standardContainsAll(c);
    }

    @Override
    public boolean remove(Object o) {
      return standardRemove(o);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      return standardRemoveAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      return standardRetainAll(c);
    }
  }

  /** @see MapConstraints#constrainedEntries */
  private static class ConstrainedEntries<K, V> extends ForwardingCollection<Entry<K, V>> {
    final MapConstraint<? super K, ? super V> constraint;
    final Collection<Entry<K, V>> entries;

    ConstrainedEntries(
        Collection<Entry<K, V>> entries, MapConstraint<? super K, ? super V> constraint) {
      this.entries = entries;
      this.constraint = constraint;
    }

    @Override
    protected Collection<Entry<K, V>> delegate() {
      return entries;
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
      return new TransformedIterator<Entry<K, V>, Entry<K, V>>(entries.iterator()) {
        @Override
        Entry<K, V> transform(Entry<K, V> from) {
          return constrainedEntry(from, constraint);
        }
      };
    }

    // See Collections.CheckedMap.CheckedEntrySet for details on attacks.

    @Override
    public Object[] toArray() {
      return standardToArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
      return standardToArray(array);
    }

    @Override
    public boolean contains(Object o) {
      return Maps.containsEntryImpl(delegate(), o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      return standardContainsAll(c);
    }

    @Override
    public boolean remove(Object o) {
      return Maps.removeEntryImpl(delegate(), o);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      return standardRemoveAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      return standardRetainAll(c);
    }
  }

  /** @see MapConstraints#constrainedEntrySet */
  static class ConstrainedEntrySet<K, V> extends ConstrainedEntries<K, V>
      implements Set<Entry<K, V>> {
    ConstrainedEntrySet(Set<Entry<K, V>> entries, MapConstraint<? super K, ? super V> constraint) {
      super(entries, constraint);
    }

    // See Collections.CheckedMap.CheckedEntrySet for details on attacks.

    @Override
    public boolean equals(@Nullable Object object) {
      return Sets.equalsImpl(this, object);
    }

    @Override
    public int hashCode() {
      return Sets.hashCodeImpl(this);
    }
  }

  /** @see MapConstraints#constrainedAsMapEntries */
  static class ConstrainedAsMapEntries<K, V> extends ForwardingSet<Entry<K, Collection<V>>> {
    private final MapConstraint<? super K, ? super V> constraint;
    private final Set<Entry<K, Collection<V>>> entries;

    ConstrainedAsMapEntries(
        Set<Entry<K, Collection<V>>> entries, MapConstraint<? super K, ? super V> constraint) {
      this.entries = entries;
      this.constraint = constraint;
    }

    @Override
    protected Set<Entry<K, Collection<V>>> delegate() {
      return entries;
    }

    @Override
    public Iterator<Entry<K, Collection<V>>> iterator() {
      return new TransformedIterator<Entry<K, Collection<V>>, Entry<K, Collection<V>>>(
          entries.iterator()) {
        @Override
        Entry<K, Collection<V>> transform(Entry<K, Collection<V>> from) {
          return constrainedAsMapEntry(from, constraint);
        }
      };
    }

    // See Collections.CheckedMap.CheckedEntrySet for details on attacks.

    @Override
    public Object[] toArray() {
      return standardToArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
      return standardToArray(array);
    }

    @Override
    public boolean contains(Object o) {
      return Maps.containsEntryImpl(delegate(), o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      return standardContainsAll(c);
    }

    @Override
    public boolean equals(@Nullable Object object) {
      return standardEquals(object);
    }

    @Override
    public int hashCode() {
      return standardHashCode();
    }

    @Override
    public boolean remove(Object o) {
      return Maps.removeEntryImpl(delegate(), o);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      return standardRemoveAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      return standardRetainAll(c);
    }
  }

  private static class ConstrainedListMultimap<K, V> extends ConstrainedMultimap<K, V>
      implements ListMultimap<K, V> {
    ConstrainedListMultimap(
        ListMultimap<K, V> delegate, MapConstraint<? super K, ? super V> constraint) {
      super(delegate, constraint);
    }

    @Override
    public List<V> get(K key) {
      return (List<V>) super.get(key);
    }

    @Override
    public List<V> removeAll(Object key) {
      return (List<V>) super.removeAll(key);
    }

    @Override
    public List<V> replaceValues(K key, Iterable<? extends V> values) {
      return (List<V>) super.replaceValues(key, values);
    }
  }

  private static class ConstrainedSetMultimap<K, V> extends ConstrainedMultimap<K, V>
      implements SetMultimap<K, V> {
    ConstrainedSetMultimap(
        SetMultimap<K, V> delegate, MapConstraint<? super K, ? super V> constraint) {
      super(delegate, constraint);
    }

    @Override
    public Set<V> get(K key) {
      return (Set<V>) super.get(key);
    }

    @Override
    public Set<Map.Entry<K, V>> entries() {
      return (Set<Map.Entry<K, V>>) super.entries();
    }

    @Override
    public Set<V> removeAll(Object key) {
      return (Set<V>) super.removeAll(key);
    }

    @Override
    public Set<V> replaceValues(K key, Iterable<? extends V> values) {
      return (Set<V>) super.replaceValues(key, values);
    }
  }

  private static class ConstrainedSortedSetMultimap<K, V> extends ConstrainedSetMultimap<K, V>
      implements SortedSetMultimap<K, V> {
    ConstrainedSortedSetMultimap(
        SortedSetMultimap<K, V> delegate, MapConstraint<? super K, ? super V> constraint) {
      super(delegate, constraint);
    }

    @Override
    public SortedSet<V> get(K key) {
      return (SortedSet<V>) super.get(key);
    }

    @Override
    public SortedSet<V> removeAll(Object key) {
      return (SortedSet<V>) super.removeAll(key);
    }

    @Override
    public SortedSet<V> replaceValues(K key, Iterable<? extends V> values) {
      return (SortedSet<V>) super.replaceValues(key, values);
    }

    @Override
    public Comparator<? super V> valueComparator() {
      return ((SortedSetMultimap<K, V>) delegate()).valueComparator();
    }
  }

  private static <K, V> Collection<V> checkValues(
      K key, Iterable<? extends V> values, MapConstraint<? super K, ? super V> constraint) {
    Collection<V> copy = Lists.newArrayList(values);
    for (V value : copy) {
      constraint.checkKeyValue(key, value);
    }
    return copy;
  }

  private static <K, V> Map<K, V> checkMap(
      Map<? extends K, ? extends V> map, MapConstraint<? super K, ? super V> constraint) {
    Map<K, V> copy = new LinkedHashMap<K, V>(map);
    for (Entry<K, V> entry : copy.entrySet()) {
      constraint.checkKeyValue(entry.getKey(), entry.getValue());
    }
    return copy;
  }
}

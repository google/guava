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
import static com.google.common.collect.SortedLists.KeyAbsentBehavior.INVERTED_INSERTION_INDEX;
import static com.google.common.collect.SortedLists.KeyAbsentBehavior.NEXT_HIGHER;
import static com.google.common.collect.SortedLists.KeyAbsentBehavior.NEXT_LOWER;
import static com.google.common.collect.SortedLists.KeyPresentBehavior.ANY_PRESENT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.SortedLists.KeyAbsentBehavior;
import com.google.common.collect.SortedLists.KeyPresentBehavior;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.annotation.Nullable;

/**
 * An immutable {@link SortedMap}. Does not permit null keys or values.
 *
 * <p>Unlike {@link Collections#unmodifiableSortedMap}, which is a <i>view</i>
 * of a separate map which can still change, an instance of {@code
 * ImmutableSortedMap} contains its own data and will <i>never</i> change.
 * {@code ImmutableSortedMap} is convenient for {@code public static final} maps
 * ("constant maps") and also lets you easily make a "defensive copy" of a map
 * provided to your class by a caller.
 *
 * <p><b>Note:</b> Although this class is not final, it cannot be subclassed as
 * it has no public or protected constructors. Thus, instances of this class are
 * guaranteed to be immutable.
 *
 * @author Jared Levy
 * @author Louis Wasserman
 * @since 2.0 (imported from Google Collections Library)
 */
@GwtCompatible(serializable = true, emulated = true)
public class ImmutableSortedMap<K, V>
    extends ImmutableSortedMapFauxverideShim<K, V> implements SortedMap<K, V> {
  /*
   * TODO(kevinb): Confirm that ImmutableSortedMap is faster to construct and
   * uses less memory than TreeMap; then say so in the class Javadoc.
   *
   * TODO(kevinb): Create separate subclasses for empty, single-entry, and
   * multiple-entry instances, if it's deemed beneficial.
   */

  private static final Comparator<Comparable> NATURAL_ORDER =
      Ordering.natural();

  private static final ImmutableSortedMap<Comparable, Object>
      NATURAL_EMPTY_MAP =
          new ImmutableSortedMap<Comparable, Object>(
              ImmutableList.<Entry<Comparable, Object>>of(), NATURAL_ORDER);

  /**
   * Returns the empty sorted map.
   */
  @SuppressWarnings("unchecked")
  // unsafe, comparator() returns a comparator on the specified type
  // TODO(kevinb): evaluate whether or not of().comparator() should return null
  public static <K, V> ImmutableSortedMap<K, V> of() {
    return (ImmutableSortedMap<K, V>) NATURAL_EMPTY_MAP;
  }

  @SuppressWarnings("unchecked")
  private static <K, V> ImmutableSortedMap<K, V> emptyMap(
      Comparator<? super K> comparator) {
    if (NATURAL_ORDER.equals(comparator)) {
      return (ImmutableSortedMap<K, V>) NATURAL_EMPTY_MAP;
    } else {
      return new ImmutableSortedMap<K, V>(
          ImmutableList.<Entry<K, V>>of(), comparator);
    }
  }

  /**
   * Returns an immutable map containing a single entry.
   */
  public static <K extends Comparable<? super K>, V>
      ImmutableSortedMap<K, V> of(K k1, V v1) {
    return new ImmutableSortedMap<K, V>(
        ImmutableList.of(entryOf(k1, v1)), Ordering.natural());
  }

  /**
   * Returns an immutable sorted map containing the given entries, sorted by the
   * natural ordering of their keys.
   *
   * @throws IllegalArgumentException if the two keys are equal according to
   *     their natural ordering
   */
  public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V>
      of(K k1, V v1, K k2, V v2) {
    return new Builder<K, V>(Ordering.natural())
        .put(k1, v1).put(k2, v2).build();
  }

  /**
   * Returns an immutable sorted map containing the given entries, sorted by the
   * natural ordering of their keys.
   *
   * @throws IllegalArgumentException if any two keys are equal according to
   *     their natural ordering
   */
  public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V>
      of(K k1, V v1, K k2, V v2, K k3, V v3) {
    return new Builder<K, V>(Ordering.natural())
        .put(k1, v1).put(k2, v2).put(k3, v3).build();
  }

  /**
   * Returns an immutable sorted map containing the given entries, sorted by the
   * natural ordering of their keys.
   *
   * @throws IllegalArgumentException if any two keys are equal according to
   *     their natural ordering
   */
  public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V>
      of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
    return new Builder<K, V>(Ordering.natural())
        .put(k1, v1).put(k2, v2).put(k3, v3).put(k4, v4).build();
  }

  /**
   * Returns an immutable sorted map containing the given entries, sorted by the
   * natural ordering of their keys.
   *
   * @throws IllegalArgumentException if any two keys are equal according to
   *     their natural ordering
   */
  public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V>
      of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
    return new Builder<K, V>(Ordering.natural())
        .put(k1, v1).put(k2, v2).put(k3, v3).put(k4, v4).put(k5, v5).build();
  }

  /**
   * Returns an immutable map containing the same entries as {@code map}, sorted
   * by the natural ordering of the keys.
   *
   * <p>Despite the method name, this method attempts to avoid actually copying
   * the data when it is safe to do so. The exact circumstances under which a
   * copy will or will not be performed are undocumented and subject to change.
   *
   * <p>This method is not type-safe, as it may be called on a map with keys
   * that are not mutually comparable.
   *
   * @throws ClassCastException if the keys in {@code map} are not mutually
   *         comparable
   * @throws NullPointerException if any key or value in {@code map} is null
   * @throws IllegalArgumentException if any two keys are equal according to
   *         their natural ordering
   */
  public static <K, V> ImmutableSortedMap<K, V> copyOf(
      Map<? extends K, ? extends V> map) {
    // Hack around K not being a subtype of Comparable.
    // Unsafe, see ImmutableSortedSetFauxverideShim.
    @SuppressWarnings("unchecked")
    Ordering<K> naturalOrder = (Ordering<K>) Ordering.<Comparable>natural();
    return copyOfInternal(map, naturalOrder);
  }

  /**
   * Returns an immutable map containing the same entries as {@code map}, with
   * keys sorted by the provided comparator.
   *
   * <p>Despite the method name, this method attempts to avoid actually copying
   * the data when it is safe to do so. The exact circumstances under which a
   * copy will or will not be performed are undocumented and subject to change.
   *
   * @throws NullPointerException if any key or value in {@code map} is null
   * @throws IllegalArgumentException if any two keys are equal according to the
   *         comparator
   */
  public static <K, V> ImmutableSortedMap<K, V> copyOf(
      Map<? extends K, ? extends V> map, Comparator<? super K> comparator) {
    return copyOfInternal(map, checkNotNull(comparator));
  }

  /**
   * Returns an immutable map containing the same entries as the provided sorted
   * map, with the same ordering.
   *
   * <p>Despite the method name, this method attempts to avoid actually copying
   * the data when it is safe to do so. The exact circumstances under which a
   * copy will or will not be performed are undocumented and subject to change.
   *
   * @throws NullPointerException if any key or value in {@code map} is null
   */
  @SuppressWarnings("unchecked")
  public static <K, V> ImmutableSortedMap<K, V> copyOfSorted(
      SortedMap<K, ? extends V> map) {
    Comparator<? super K> comparator = map.comparator();
    if (comparator == null) {
      // If map has a null comparator, the keys should have a natural ordering,
      // even though K doesn't explicitly implement Comparable.
      comparator = (Comparator<? super K>) NATURAL_ORDER;
    }
    return copyOfInternal(map, comparator);
  }

  private static <K, V> ImmutableSortedMap<K, V> copyOfInternal(
      Map<? extends K, ? extends V> map, Comparator<? super K> comparator) {
    boolean sameComparator = false;
    if (map instanceof SortedMap) {
      SortedMap<?, ?> sortedMap = (SortedMap<?, ?>) map;
      Comparator<?> comparator2 = sortedMap.comparator();
      sameComparator = (comparator2 == null)
          ? comparator == NATURAL_ORDER 
          : comparator.equals(comparator2);
    }

    if (sameComparator && (map instanceof ImmutableSortedMap)) {
      // TODO(kevinb): Prove that this cast is safe, even though
      // Collections.unmodifiableSortedMap requires the same key type.
      @SuppressWarnings("unchecked")
      ImmutableSortedMap<K, V> kvMap = (ImmutableSortedMap<K, V>) map;
      if (!kvMap.isPartialView()) {
        return kvMap;
      }
    }

    // "adding" type params to an array of a raw type should be safe as
    // long as no one can ever cast that same array instance back to a 
    // raw type.
    @SuppressWarnings("unchecked")
    Entry<K, V>[] entries = map.entrySet().toArray(new Entry[0]);

    for (int i = 0; i < entries.length; i++) {
      Entry<K, V> entry = entries[i];
      entries[i] = entryOf(entry.getKey(), entry.getValue());
    }

    List<Entry<K, V>> list = Arrays.asList(entries);

    if (!sameComparator) {
      sortEntries(list, comparator);
      validateEntries(list, comparator);
    }

    return new ImmutableSortedMap<K, V>(ImmutableList.copyOf(list), comparator);
  }
  
  private static <K, V> void sortEntries(
      List<Entry<K, V>> entries, final Comparator<? super K> comparator) {
    Comparator<Entry<K, V>> entryComparator = new Comparator<Entry<K, V>>() {

      @Override public int compare(Entry<K, V> entry1, Entry<K, V> entry2) {
        return comparator.compare(entry1.getKey(), entry2.getKey());
      }
    };
    
    Collections.sort(entries, entryComparator);
  }

  private static <K, V> void validateEntries(List<Entry<K, V>> entries,
      Comparator<? super K> comparator) {
    for (int i = 1; i < entries.size(); i++) {
      if (comparator.compare(
          entries.get(i - 1).getKey(), entries.get(i).getKey()) == 0) {
        throw new IllegalArgumentException(
            "Duplicate keys in mappings " + entries.get(i - 1) + " and "
                + entries.get(i));
      }
    }
  }

  /**
   * Returns a builder that creates immutable sorted maps whose keys are
   * ordered by their natural ordering. The sorted maps use {@link
   * Ordering#natural()} as the comparator.
   *
   * <p>Note: the type parameter {@code K} extends {@code Comparable<K>} rather
   * than {@code Comparable<? super K>} as a workaround for javac <a
   * href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6468354">bug
   * 6468354</a>.
   */
  public static <K extends Comparable<K>, V> Builder<K, V> naturalOrder() {
    return new Builder<K, V>(Ordering.natural());
  }

  /**
   * Returns a builder that creates immutable sorted maps with an explicit
   * comparator. If the comparator has a more general type than the map's keys,
   * such as creating a {@code SortedMap<Integer, String>} with a {@code
   * Comparator<Number>}, use the {@link Builder} constructor instead.
   *
   * @throws NullPointerException if {@code comparator} is null
   */
  public static <K, V> Builder<K, V> orderedBy(Comparator<K> comparator) {
    return new Builder<K, V>(comparator);
  }

  /**
   * Returns a builder that creates immutable sorted maps whose keys are
   * ordered by the reverse of their natural ordering.
   *
   * <p>Note: the type parameter {@code K} extends {@code Comparable<K>} rather
   * than {@code Comparable<? super K>} as a workaround for javac <a
   * href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6468354">bug
   * 6468354</a>.
   */
  public static <K extends Comparable<K>, V> Builder<K, V> reverseOrder() {
    return new Builder<K, V>(Ordering.natural().reverse());
  }

  /**
   * A builder for creating immutable sorted map instances, especially {@code
   * public static final} maps ("constant maps"). Example: <pre>   {@code
   *
   *   static final ImmutableSortedMap<Integer, String> INT_TO_WORD =
   *       new ImmutableSortedMap.Builder<Integer, String>(Ordering.natural())
   *           .put(1, "one")
   *           .put(2, "two")
   *           .put(3, "three")
   *           .build();}</pre>
   *
   * For <i>small</i> immutable sorted maps, the {@code ImmutableSortedMap.of()}
   * methods are even more convenient.
   *
   * <p>Builder instances can be reused - it is safe to call {@link #build}
   * multiple times to build multiple maps in series. Each map is a superset of
   * the maps created before it.
   *
   * @since 2.0 (imported from Google Collections Library)
   */
  public static class Builder<K, V> extends ImmutableMap.Builder<K, V> {
    private final Comparator<? super K> comparator;

    /**
     * Creates a new builder. The returned builder is equivalent to the builder
     * generated by {@link ImmutableSortedMap#orderedBy}.
     */
    public Builder(Comparator<? super K> comparator) {
      this.comparator = checkNotNull(comparator);
    }

    /**
     * Associates {@code key} with {@code value} in the built map. Duplicate
     * keys, according to the comparator (which might be the keys' natural
     * order), are not allowed, and will cause {@link #build} to fail.
     */
    @Override public Builder<K, V> put(K key, V value) {
      entries.add(entryOf(key, value));
      return this;
    }

    /**
     * Adds the given {@code entry} to the map, making it immutable if
     * necessary. Duplicate keys, according to the comparator (which might be
     * the keys' natural order), are not allowed, and will cause {@link #build}
     * to fail.
     *
     * @since 11.0
     */
    @Override public Builder<K, V> put(Entry<? extends K, ? extends V> entry) {
      super.put(entry);
      return this;
    }

    /**
     * Associates all of the given map's keys and values in the built map.
     * Duplicate keys, according to the comparator (which might be the keys'
     * natural order), are not allowed, and will cause {@link #build} to fail.
     *
     * @throws NullPointerException if any key or value in {@code map} is null
     */
    @Override public Builder<K, V> putAll(Map<? extends K, ? extends V> map) {
      for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
        put(entry.getKey(), entry.getValue());
      }
      return this;
    }

    /**
     * Returns a newly-created immutable sorted map.
     *
     * @throws IllegalArgumentException if any two keys are equal according to
     *     the comparator (which might be the keys' natural order)
     */
    @Override public ImmutableSortedMap<K, V> build() {
      sortEntries(entries, comparator);
      validateEntries(entries, comparator);
      return new ImmutableSortedMap<K, V>(
          ImmutableList.copyOf(entries), comparator);
    }
  }

  final transient ImmutableList<Entry<K, V>> entries;
  private final transient Comparator<? super K> comparator;

  ImmutableSortedMap(
      ImmutableList<Entry<K, V>> entries, Comparator<? super K> comparator) {
    this.entries = entries;
    this.comparator = comparator;
  }

  @Override
  public int size() {
    return entries.size();
  }
  
  // Pretend the comparator can compare anything. If it turns out it can't
  // compare two elements, it'll throw a CCE. Only methods that are specified to
  // throw CCE should call this.
  @SuppressWarnings("unchecked")
  Comparator<Object> unsafeComparator() {
    return (Comparator<Object>) comparator;
  }
  
  @Override public V get(@Nullable Object key) {
    if (key == null) {
      return null;
    }
    int i;
    try {
      i = index(key, ANY_PRESENT, INVERTED_INSERTION_INDEX);
    } catch (ClassCastException e) {
      return null;
    }
    return i >= 0 ? entries.get(i).getValue() : null;
  }

  @Override public boolean containsValue(@Nullable Object value) {
    if (value == null) {
      return false;
    }
    return Iterators.contains(valueIterator(), value);
  }

  @Override boolean isPartialView() {
    return entries.isPartialView();
  }

  private transient ImmutableSet<Entry<K, V>> entrySet;

  /**
   * Returns an immutable set of the mappings in this map, sorted by the key
   * ordering.
   */
  @Override public ImmutableSet<Entry<K, V>> entrySet() {
    ImmutableSet<Entry<K, V>> es = entrySet;
    return (es == null) ? (entrySet = createEntrySet()) : es;
  }

  private ImmutableSet<Entry<K, V>> createEntrySet() {
    return isEmpty() ? ImmutableSet.<Entry<K, V>>of()
        : new EntrySet<K, V>(this);
  }

  @SuppressWarnings("serial") // uses writeReplace(), not default serialization
  private static class EntrySet<K, V> extends ImmutableSet<Entry<K, V>> {
    final transient ImmutableSortedMap<K, V> map;

    EntrySet(ImmutableSortedMap<K, V> map) {
      this.map = map;
    }

    @Override boolean isPartialView() {
      return map.isPartialView();
    }

    @Override
    public int size() {
      return map.size();
    }

    @Override public UnmodifiableIterator<Entry<K, V>> iterator() {
      return map.entries.iterator();
    }

    @Override public boolean contains(Object target) {
      if (target instanceof Entry) {
        Entry<?, ?> entry = (Entry<?, ?>) target;
        V mappedValue = map.get(entry.getKey());
        return mappedValue != null && mappedValue.equals(entry.getValue());
      }
      return false;
    }

    @Override Object writeReplace() {
      return new EntrySetSerializedForm<K, V>(map);
    }
  }

  private static class EntrySetSerializedForm<K, V> implements Serializable {
    final ImmutableSortedMap<K, V> map;
    EntrySetSerializedForm(ImmutableSortedMap<K, V> map) {
      this.map = map;
    }
    Object readResolve() {
      return map.entrySet();
    }
    private static final long serialVersionUID = 0;
  }

  private transient ImmutableSortedSet<K> keySet;

  /**
   * Returns an immutable sorted set of the keys in this map.
   */
  @Override public ImmutableSortedSet<K> keySet() {
    ImmutableSortedSet<K> ks = keySet;
    return (ks == null) ? (keySet = createKeySet()) : ks;
  }

  @SuppressWarnings("serial") // does not use default serialization
  private ImmutableSortedSet<K> createKeySet() {
    if (isEmpty()) {
      return ImmutableSortedSet.emptySet(comparator);
    }

    return new RegularImmutableSortedSet<K>(
        new TransformedImmutableList<Entry<K, V>, K>(entries) {

          @Override K transform(Entry<K, V> entry) {
            return entry.getKey();
          }
        }, comparator);
  }
  
  private transient ImmutableCollection<V> values;

  /**
   * Returns an immutable collection of the values in this map, sorted by the
   * ordering of the corresponding keys.
   */
  @Override public ImmutableCollection<V> values() {
    ImmutableCollection<V> v = values;
    return (v == null) ? (values = new Values<V>(this)) : v;
  }
  
  UnmodifiableIterator<V> valueIterator(){
    final UnmodifiableIterator<Entry<K, V>> entryIterator = entries.iterator();
    return new UnmodifiableIterator<V>() {

      @Override public boolean hasNext() {
        return entryIterator.hasNext();
      }

      @Override public V next() {
        return entryIterator.next().getValue();
      }
    };
  }

  @SuppressWarnings("serial") // uses writeReplace(), not default serialization
  private static class Values<V> extends ImmutableCollection<V> {
    private final ImmutableSortedMap<?, V> map;

    Values(ImmutableSortedMap<?, V> map) {
      this.map = map;
    }

    @Override
    public int size() {
      return map.size();
    }

    @Override public UnmodifiableIterator<V> iterator() {
      return map.valueIterator();
    }

    @Override public boolean contains(Object target) {
      return map.containsValue(target);
    }

    @Override boolean isPartialView() {
      return true;
    }

    @Override Object writeReplace() {
      return new ValuesSerializedForm<V>(map);
    }
  }

  private static class ValuesSerializedForm<V> implements Serializable {
    final ImmutableSortedMap<?, V> map;
    ValuesSerializedForm(ImmutableSortedMap<?, V> map) {
      this.map = map;
    }
    Object readResolve() {
      return map.values();
    }
    private static final long serialVersionUID = 0;
  }

  /**
   * Returns the comparator that orders the keys, which is
   * {@link Ordering#natural()} when the natural ordering of the keys is used.
   * Note that its behavior is not consistent with {@link TreeMap#comparator()},
   * which returns {@code null} to indicate natural ordering.
   */
  @Override
  public Comparator<? super K> comparator() {
    return comparator;
  }

  @Override
  public K firstKey() {
    if (isEmpty()) {
      throw new NoSuchElementException();
    }
    return entries.get(0).getKey();
  }

  @Override
  public K lastKey() {
    if (isEmpty()) {
      throw new NoSuchElementException();
    }
    return entries.get(size() - 1).getKey();
  }

  /**
   * This method returns a {@code ImmutableSortedMap}, consisting of the entries
   * whose keys are less than {@code toKey}.
   *
   * <p>The {@link SortedMap#headMap} documentation states that a submap of a
   * submap throws an {@link IllegalArgumentException} if passed a {@code toKey}
   * greater than an earlier {@code toKey}. However, this method doesn't throw
   * an exception in that situation, but instead keeps the original {@code
   * toKey}.
   */
  @Override
  public ImmutableSortedMap<K, V> headMap(K toKey) {
    return headMap(toKey, false);
  }

  ImmutableSortedMap<K, V> headMap(K toKey, boolean inclusive){
    int index;
    if (inclusive) {
      index = index(toKey, ANY_PRESENT, NEXT_LOWER) + 1;
    } else {
      index = index(toKey, ANY_PRESENT, NEXT_HIGHER);
    }
    return createSubmap(0, index);
  }

  /**
   * This method returns a {@code ImmutableSortedMap}, consisting of the entries
   * whose keys ranges from {@code fromKey}, inclusive, to {@code toKey},
   * exclusive.
   *
   * <p>The {@link SortedMap#subMap} documentation states that a submap of a
   * submap throws an {@link IllegalArgumentException} if passed a {@code
   * fromKey} less than an earlier {@code fromKey}. However, this method doesn't
   * throw an exception in that situation, but instead keeps the original {@code
   * fromKey}. Similarly, this method keeps the original {@code toKey}, instead
   * of throwing an exception, if passed a {@code toKey} greater than an earlier
   * {@code toKey}.
   */
  @Override
  public ImmutableSortedMap<K, V> subMap(K fromKey, K toKey) {
    return subMap(fromKey, true, toKey, false);
  }

  ImmutableSortedMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey,
      boolean toInclusive) {
    checkNotNull(fromKey);
    checkNotNull(toKey);
    checkArgument(comparator.compare(fromKey, toKey) <= 0);
    return tailMap(fromKey, fromInclusive).headMap(toKey, toInclusive);
  }

  /**
   * This method returns a {@code ImmutableSortedMap}, consisting of the entries
   * whose keys are greater than or equals to {@code fromKey}.
   *
   * <p>The {@link SortedMap#tailMap} documentation states that a submap of a
   * submap throws an {@link IllegalArgumentException} if passed a {@code
   * fromKey} less than an earlier {@code fromKey}. However, this method doesn't
   * throw an exception in that situation, but instead keeps the original {@code
   * fromKey}.
   */
  @Override
  public ImmutableSortedMap<K, V> tailMap(K fromKey) {
    return tailMap(fromKey, true);
  }

  ImmutableSortedMap<K, V> tailMap(K fromKey, boolean inclusive) {
    int index;
    if (inclusive) {
      index = index(fromKey, ANY_PRESENT, NEXT_HIGHER);
    } else {
      index = index(fromKey, ANY_PRESENT, NEXT_LOWER) + 1;
    }
    return createSubmap(index, size());
  }

  private ImmutableList<K> keyList() {
    return new TransformedImmutableList<Entry<K, V>, K>(entries) {
      @Override
      K transform(Entry<K, V> entry) {
        return entry.getKey();
      }
    };
  }

  private int index(
      Object key, KeyPresentBehavior presentBehavior, KeyAbsentBehavior absentBehavior) {
    return SortedLists.binarySearch(
        keyList(), checkNotNull(key), unsafeComparator(), presentBehavior, absentBehavior);
  }

  private ImmutableSortedMap<K, V> createSubmap(
      int newFromIndex, int newToIndex) {
    if (newFromIndex < newToIndex) {
      return new ImmutableSortedMap<K, V>(
          entries.subList(newFromIndex, newToIndex), comparator);
    } else {
      return emptyMap(comparator);
    }
  }

  /**
   * Serialized type for all ImmutableSortedMap instances. It captures the
   * logical contents and they are reconstructed using public factory methods.
   * This ensures that the implementation types remain as implementation
   * details.
   */
  private static class SerializedForm extends ImmutableMap.SerializedForm {
    private final Comparator<Object> comparator;
    @SuppressWarnings("unchecked")
    SerializedForm(ImmutableSortedMap<?, ?> sortedMap) {
      super(sortedMap);
      comparator = (Comparator<Object>) sortedMap.comparator();
    }
    @Override Object readResolve() {
      Builder<Object, Object> builder = new Builder<Object, Object>(comparator);
      return createMap(builder);
    }
    private static final long serialVersionUID = 0;
  }

  @Override Object writeReplace() {
    return new SerializedForm(this);
  }

  // This class is never actually serialized directly, but we have to make the
  // warning go away (and suppressing would suppress for all nested classes too)
  private static final long serialVersionUID = 0;
}

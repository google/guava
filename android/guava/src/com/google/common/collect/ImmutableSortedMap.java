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
import static com.google.common.collect.CollectPreconditions.checkEntryNotNull;
import static com.google.common.collect.Maps.keyOrNull;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.DoNotCall;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link NavigableMap} whose contents will never change, with many other important properties
 * detailed at {@link ImmutableCollection}.
 *
 * <p><b>Warning:</b> as with any sorted collection, you are strongly advised not to use a {@link
 * Comparator} or {@link Comparable} type whose comparison behavior is <i>inconsistent with
 * equals</i>. That is, {@code a.compareTo(b)} or {@code comparator.compare(a, b)} should equal zero
 * <i>if and only if</i> {@code a.equals(b)}. If this advice is not followed, the resulting map will
 * not correctly obey its specification.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/ImmutableCollectionsExplained">immutable collections</a>.
 *
 * @author Jared Levy
 * @author Louis Wasserman
 * @since 2.0 (implements {@code NavigableMap} since 12.0)
 */
@GwtCompatible(serializable = true, emulated = true)
@ElementTypesAreNonnullByDefault
public final class ImmutableSortedMap<K, V> extends ImmutableMap<K, V>
    implements NavigableMap<K, V> {
  /**
   * Returns a {@link Collector} that accumulates elements into an {@code ImmutableSortedMap} whose
   * keys and values are the result of applying the provided mapping functions to the input
   * elements. The generated map is sorted by the specified comparator.
   *
   * <p>If the mapped keys contain duplicates (according to the specified comparator), an {@code
   * IllegalArgumentException} is thrown when the collection operation is performed. (This differs
   * from the {@code Collector} returned by {@link Collectors#toMap(Function, Function)}, which
   * throws an {@code IllegalStateException}.)
   */
  @SuppressWarnings({"AndroidJdkLibsChecker", "Java7ApiChecker"})
  @IgnoreJRERequirement // Users will use this only if they're already using streams.
  static <T extends @Nullable Object, K, V>
      Collector<T, ?, ImmutableSortedMap<K, V>> toImmutableSortedMap(
          Comparator<? super K> comparator,
          Function<? super T, ? extends K> keyFunction,
          Function<? super T, ? extends V> valueFunction) {
    return CollectCollectors.toImmutableSortedMap(comparator, keyFunction, valueFunction);
  }

  /**
   * Returns a {@link Collector} that accumulates elements into an {@code ImmutableSortedMap} whose
   * keys and values are the result of applying the provided mapping functions to the input
   * elements.
   *
   * <p>If the mapped keys contain duplicates (according to the comparator), the values are merged
   * using the specified merging function. Entries will appear in the encounter order of the first
   * occurrence of the key.
   */
  @SuppressWarnings({"AndroidJdkLibsChecker", "Java7ApiChecker"})
  @IgnoreJRERequirement // Users will use this only if they're already using streams.
  static <T extends @Nullable Object, K, V>
      Collector<T, ?, ImmutableSortedMap<K, V>> toImmutableSortedMap(
          Comparator<? super K> comparator,
          Function<? super T, ? extends K> keyFunction,
          Function<? super T, ? extends V> valueFunction,
          BinaryOperator<V> mergeFunction) {
    return CollectCollectors.toImmutableSortedMap(
        comparator, keyFunction, valueFunction, mergeFunction);
  }

  /*
   * TODO(kevinb): Confirm that ImmutableSortedMap is faster to construct and
   * uses less memory than TreeMap; then say so in the class Javadoc.
   */
  private static final Comparator<Comparable> NATURAL_ORDER = Ordering.natural();

  private static final ImmutableSortedMap<Comparable, Object> NATURAL_EMPTY_MAP =
      new ImmutableSortedMap<>(
          ImmutableSortedSet.emptySet(Ordering.natural()), ImmutableList.<Object>of());

  static <K, V> ImmutableSortedMap<K, V> emptyMap(Comparator<? super K> comparator) {
    if (Ordering.natural().equals(comparator)) {
      return of();
    } else {
      return new ImmutableSortedMap<>(
          ImmutableSortedSet.emptySet(comparator), ImmutableList.<V>of());
    }
  }

  /**
   * Returns the empty sorted map.
   *
   * <p><b>Performance note:</b> the instance returned is a singleton.
   */
  @SuppressWarnings("unchecked")
  // unsafe, comparator() returns a comparator on the specified type
  // TODO(kevinb): evaluate whether or not of().comparator() should return null
  public static <K, V> ImmutableSortedMap<K, V> of() {
    return (ImmutableSortedMap<K, V>) NATURAL_EMPTY_MAP;
  }

  /** Returns an immutable map containing a single entry. */
  public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(K k1, V v1) {
    return of(Ordering.natural(), k1, v1);
  }

  /** Returns an immutable map containing a single entry. */
  private static <K, V> ImmutableSortedMap<K, V> of(Comparator<? super K> comparator, K k1, V v1) {
    return new ImmutableSortedMap<>(
        new RegularImmutableSortedSet<K>(ImmutableList.of(k1), checkNotNull(comparator)),
        ImmutableList.of(v1));
  }

  /**
   * Returns an immutable sorted map containing the given entries, sorted by the natural ordering of
   * their keys.
   *
   * @throws IllegalArgumentException if the two keys are equal according to their natural ordering
   */
  @SuppressWarnings("unchecked")
  public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(
      K k1, V v1, K k2, V v2) {
    return fromEntries(entryOf(k1, v1), entryOf(k2, v2));
  }

  /**
   * Returns an immutable sorted map containing the given entries, sorted by the natural ordering of
   * their keys.
   *
   * @throws IllegalArgumentException if any two keys are equal according to their natural ordering
   */
  @SuppressWarnings("unchecked")
  public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3) {
    return fromEntries(entryOf(k1, v1), entryOf(k2, v2), entryOf(k3, v3));
  }

  /**
   * Returns an immutable sorted map containing the given entries, sorted by the natural ordering of
   * their keys.
   *
   * @throws IllegalArgumentException if any two keys are equal according to their natural ordering
   */
  @SuppressWarnings("unchecked")
  public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
    return fromEntries(entryOf(k1, v1), entryOf(k2, v2), entryOf(k3, v3), entryOf(k4, v4));
  }

  /**
   * Returns an immutable sorted map containing the given entries, sorted by the natural ordering of
   * their keys.
   *
   * @throws IllegalArgumentException if any two keys are equal according to their natural ordering
   */
  @SuppressWarnings("unchecked")
  public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
    return fromEntries(
        entryOf(k1, v1), entryOf(k2, v2), entryOf(k3, v3), entryOf(k4, v4), entryOf(k5, v5));
  }

  /**
   * Returns an immutable sorted map containing the given entries, sorted by the natural ordering of
   * their keys.
   *
   * @throws IllegalArgumentException if any two keys are equal according to their natural ordering
   * @since 31.0
   */
  @SuppressWarnings("unchecked")
  public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6) {
    return fromEntries(
        entryOf(k1, v1),
        entryOf(k2, v2),
        entryOf(k3, v3),
        entryOf(k4, v4),
        entryOf(k5, v5),
        entryOf(k6, v6));
  }

  /**
   * Returns an immutable sorted map containing the given entries, sorted by the natural ordering of
   * their keys.
   *
   * @throws IllegalArgumentException if any two keys are equal according to their natural ordering
   * @since 31.0
   */
  @SuppressWarnings("unchecked")
  public static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7) {
    return fromEntries(
        entryOf(k1, v1),
        entryOf(k2, v2),
        entryOf(k3, v3),
        entryOf(k4, v4),
        entryOf(k5, v5),
        entryOf(k6, v6),
        entryOf(k7, v7));
  }

  /**
   * Returns an immutable sorted map containing the given entries, sorted by the natural ordering of
   * their keys.
   *
   * @throws IllegalArgumentException if any two keys are equal according to their natural ordering
   * @since 31.0
   */
  @SuppressWarnings("unchecked")
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
    return fromEntries(
        entryOf(k1, v1),
        entryOf(k2, v2),
        entryOf(k3, v3),
        entryOf(k4, v4),
        entryOf(k5, v5),
        entryOf(k6, v6),
        entryOf(k7, v7),
        entryOf(k8, v8));
  }

  /**
   * Returns an immutable sorted map containing the given entries, sorted by the natural ordering of
   * their keys.
   *
   * @throws IllegalArgumentException if any two keys are equal according to their natural ordering
   * @since 31.0
   */
  @SuppressWarnings("unchecked")
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
    return fromEntries(
        entryOf(k1, v1),
        entryOf(k2, v2),
        entryOf(k3, v3),
        entryOf(k4, v4),
        entryOf(k5, v5),
        entryOf(k6, v6),
        entryOf(k7, v7),
        entryOf(k8, v8),
        entryOf(k9, v9));
  }

  /**
   * Returns an immutable sorted map containing the given entries, sorted by the natural ordering of
   * their keys.
   *
   * @throws IllegalArgumentException if any two keys are equal according to their natural ordering
   * @since 31.0
   */
  @SuppressWarnings("unchecked")
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
    return fromEntries(
        entryOf(k1, v1),
        entryOf(k2, v2),
        entryOf(k3, v3),
        entryOf(k4, v4),
        entryOf(k5, v5),
        entryOf(k6, v6),
        entryOf(k7, v7),
        entryOf(k8, v8),
        entryOf(k9, v9),
        entryOf(k10, v10));
  }

  /**
   * Returns an immutable map containing the same entries as {@code map}, sorted by the natural
   * ordering of the keys.
   *
   * <p>Despite the method name, this method attempts to avoid actually copying the data when it is
   * safe to do so. The exact circumstances under which a copy will or will not be performed are
   * undocumented and subject to change.
   *
   * <p>This method is not type-safe, as it may be called on a map with keys that are not mutually
   * comparable.
   *
   * @throws ClassCastException if the keys in {@code map} are not mutually comparable
   * @throws NullPointerException if any key or value in {@code map} is null
   * @throws IllegalArgumentException if any two keys are equal according to their natural ordering
   */
  public static <K, V> ImmutableSortedMap<K, V> copyOf(Map<? extends K, ? extends V> map) {
    // Hack around K not being a subtype of Comparable.
    // Unsafe, see ImmutableSortedSetFauxverideShim.
    @SuppressWarnings("unchecked")
    Ordering<K> naturalOrder = (Ordering<K>) NATURAL_ORDER;
    return copyOfInternal(map, naturalOrder);
  }

  /**
   * Returns an immutable map containing the same entries as {@code map}, with keys sorted by the
   * provided comparator.
   *
   * <p>Despite the method name, this method attempts to avoid actually copying the data when it is
   * safe to do so. The exact circumstances under which a copy will or will not be performed are
   * undocumented and subject to change.
   *
   * @throws NullPointerException if any key or value in {@code map} is null
   * @throws IllegalArgumentException if any two keys are equal according to the comparator
   */
  public static <K, V> ImmutableSortedMap<K, V> copyOf(
      Map<? extends K, ? extends V> map, Comparator<? super K> comparator) {
    return copyOfInternal(map, checkNotNull(comparator));
  }

  /**
   * Returns an immutable map containing the given entries, with keys sorted by their natural
   * ordering.
   *
   * <p>This method is not type-safe, as it may be called on a map with keys that are not mutually
   * comparable.
   *
   * @throws NullPointerException if any key or value in {@code map} is null
   * @throws IllegalArgumentException if any two keys are equal according to the comparator
   * @since 19.0
   */
  public static <K, V> ImmutableSortedMap<K, V> copyOf(
      Iterable<? extends Entry<? extends K, ? extends V>> entries) {
    // Hack around K not being a subtype of Comparable.
    // Unsafe, see ImmutableSortedSetFauxverideShim.
    @SuppressWarnings("unchecked")
    Ordering<K> naturalOrder = (Ordering<K>) NATURAL_ORDER;
    return copyOf(entries, naturalOrder);
  }

  /**
   * Returns an immutable map containing the given entries, with keys sorted by the provided
   * comparator.
   *
   * @throws NullPointerException if any key or value in {@code map} is null
   * @throws IllegalArgumentException if any two keys are equal according to the comparator
   * @since 19.0
   */
  public static <K, V> ImmutableSortedMap<K, V> copyOf(
      Iterable<? extends Entry<? extends K, ? extends V>> entries,
      Comparator<? super K> comparator) {
    return fromEntries(checkNotNull(comparator), false, entries);
  }

  /**
   * Returns an immutable map containing the same entries as the provided sorted map, with the same
   * ordering.
   *
   * <p>Despite the method name, this method attempts to avoid actually copying the data when it is
   * safe to do so. The exact circumstances under which a copy will or will not be performed are
   * undocumented and subject to change.
   *
   * @throws NullPointerException if any key or value in {@code map} is null
   */
  @SuppressWarnings("unchecked")
  public static <K, V> ImmutableSortedMap<K, V> copyOfSorted(SortedMap<K, ? extends V> map) {
    Comparator<? super K> comparator = map.comparator();
    if (comparator == null) {
      // If map has a null comparator, the keys should have a natural ordering,
      // even though K doesn't explicitly implement Comparable.
      comparator = (Comparator<? super K>) NATURAL_ORDER;
    }
    if (map instanceof ImmutableSortedMap) {
      // TODO(kevinb): Prove that this cast is safe, even though
      // Collections.unmodifiableSortedMap requires the same key type.
      @SuppressWarnings("unchecked")
      ImmutableSortedMap<K, V> kvMap = (ImmutableSortedMap<K, V>) map;
      if (!kvMap.isPartialView()) {
        return kvMap;
      }
    }
    return fromEntries(comparator, true, map.entrySet());
  }

  private static <K, V> ImmutableSortedMap<K, V> copyOfInternal(
      Map<? extends K, ? extends V> map, Comparator<? super K> comparator) {
    boolean sameComparator = false;
    if (map instanceof SortedMap) {
      SortedMap<?, ?> sortedMap = (SortedMap<?, ?>) map;
      Comparator<?> comparator2 = sortedMap.comparator();
      sameComparator =
          (comparator2 == null) ? comparator == NATURAL_ORDER : comparator.equals(comparator2);
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
    return fromEntries(comparator, sameComparator, map.entrySet());
  }

  private static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> fromEntries(
      Entry<K, V>... entries) {
    return fromEntries(Ordering.natural(), false, entries, entries.length);
  }

  /**
   * Accepts a collection of possibly-null entries. If {@code sameComparator}, then it is assumed
   * that they do not need to be sorted or checked for dupes.
   */
  private static <K, V> ImmutableSortedMap<K, V> fromEntries(
      Comparator<? super K> comparator,
      boolean sameComparator,
      Iterable<? extends Entry<? extends K, ? extends V>> entries) {
    // "adding" type params to an array of a raw type should be safe as
    // long as no one can ever cast that same array instance back to a
    // raw type.
    @SuppressWarnings("unchecked")
    Entry<K, V>[] entryArray = (Entry[]) Iterables.toArray(entries, EMPTY_ENTRY_ARRAY);
    return fromEntries(comparator, sameComparator, entryArray, entryArray.length);
  }

  private static <K, V> ImmutableSortedMap<K, V> fromEntries(
      final Comparator<? super K> comparator,
      boolean sameComparator,
      @Nullable Entry<K, V>[] entryArray,
      int size) {
    switch (size) {
      case 0:
        return emptyMap(comparator);
      case 1:
        // requireNonNull is safe because the first `size` elements have been filled in.
        Entry<K, V> onlyEntry = requireNonNull(entryArray[0]);
        return of(comparator, onlyEntry.getKey(), onlyEntry.getValue());
      default:
        Object[] keys = new Object[size];
        Object[] values = new Object[size];
        if (sameComparator) {
          // Need to check for nulls, but don't need to sort or validate.
          for (int i = 0; i < size; i++) {
            // requireNonNull is safe because the first `size` elements have been filled in.
            Entry<K, V> entry = requireNonNull(entryArray[i]);
            Object key = entry.getKey();
            Object value = entry.getValue();
            checkEntryNotNull(key, value);
            keys[i] = key;
            values[i] = value;
          }
        } else {
          // Need to sort and check for nulls and dupes.
          // Inline the Comparator implementation rather than transforming with a Function
          // to save code size.
          Arrays.sort(
              entryArray,
              0,
              size,
              (e1, e2) -> {
                // requireNonNull is safe because the first `size` elements have been filled in.
                requireNonNull(e1);
                requireNonNull(e2);
                return comparator.compare(e1.getKey(), e2.getKey());
              });
          // requireNonNull is safe because the first `size` elements have been filled in.
          Entry<K, V> firstEntry = requireNonNull(entryArray[0]);
          K prevKey = firstEntry.getKey();
          keys[0] = prevKey;
          values[0] = firstEntry.getValue();
          checkEntryNotNull(keys[0], values[0]);
          for (int i = 1; i < size; i++) {
            // requireNonNull is safe because the first `size` elements have been filled in.
            Entry<K, V> prevEntry = requireNonNull(entryArray[i - 1]);
            Entry<K, V> entry = requireNonNull(entryArray[i]);
            K key = entry.getKey();
            V value = entry.getValue();
            checkEntryNotNull(key, value);
            keys[i] = key;
            values[i] = value;
            checkNoConflict(comparator.compare(prevKey, key) != 0, "key", prevEntry, entry);
            prevKey = key;
          }
        }
        return new ImmutableSortedMap<>(
            new RegularImmutableSortedSet<K>(ImmutableList.<K>asImmutableList(keys), comparator),
            ImmutableList.<V>asImmutableList(values));
    }
  }

  /**
   * Returns a builder that creates immutable sorted maps whose keys are ordered by their natural
   * ordering. The sorted maps use {@link Ordering#natural()} as the comparator.
   */
  public static <K extends Comparable<?>, V> Builder<K, V> naturalOrder() {
    return new Builder<>(Ordering.natural());
  }

  /**
   * Returns a builder that creates immutable sorted maps with an explicit comparator. If the
   * comparator has a more general type than the map's keys, such as creating a {@code
   * SortedMap<Integer, String>} with a {@code Comparator<Number>}, use the {@link Builder}
   * constructor instead.
   *
   * @throws NullPointerException if {@code comparator} is null
   */
  public static <K, V> Builder<K, V> orderedBy(Comparator<K> comparator) {
    return new Builder<>(comparator);
  }

  /**
   * Returns a builder that creates immutable sorted maps whose keys are ordered by the reverse of
   * their natural ordering.
   */
  public static <K extends Comparable<?>, V> Builder<K, V> reverseOrder() {
    return new Builder<>(Ordering.<K>natural().reverse());
  }

  /**
   * A builder for creating immutable sorted map instances, especially {@code public static final}
   * maps ("constant maps"). Example:
   *
   * <pre>{@code
   * static final ImmutableSortedMap<Integer, String> INT_TO_WORD =
   *     new ImmutableSortedMap.Builder<Integer, String>(Ordering.natural())
   *         .put(1, "one")
   *         .put(2, "two")
   *         .put(3, "three")
   *         .buildOrThrow();
   * }</pre>
   *
   * <p>For <i>small</i> immutable sorted maps, the {@code ImmutableSortedMap.of()} methods are even
   * more convenient.
   *
   * <p>Builder instances can be reused - it is safe to call {@link #buildOrThrow} multiple times to
   * build multiple maps in series. Each map is a superset of the maps created before it.
   *
   * @since 2.0
   */
  public static class Builder<K, V> extends ImmutableMap.Builder<K, V> {
    private transient @Nullable Object[] keys;
    private transient @Nullable Object[] values;
    private final Comparator<? super K> comparator;

    /**
     * Creates a new builder. The returned builder is equivalent to the builder generated by {@link
     * ImmutableSortedMap#orderedBy}.
     */
    @SuppressWarnings("unchecked")
    public Builder(Comparator<? super K> comparator) {
      this(comparator, ImmutableCollection.Builder.DEFAULT_INITIAL_CAPACITY);
    }

    private Builder(Comparator<? super K> comparator, int initialCapacity) {
      this.comparator = checkNotNull(comparator);
      this.keys = new @Nullable Object[initialCapacity];
      this.values = new @Nullable Object[initialCapacity];
    }

    private void ensureCapacity(int minCapacity) {
      if (minCapacity > keys.length) {
        int newCapacity = ImmutableCollection.Builder.expandedCapacity(keys.length, minCapacity);
        this.keys = Arrays.copyOf(keys, newCapacity);
        this.values = Arrays.copyOf(values, newCapacity);
      }
    }

    /**
     * Associates {@code key} with {@code value} in the built map. Duplicate keys, according to the
     * comparator (which might be the keys' natural order), are not allowed, and will cause {@link
     * #build} to fail.
     */
    @CanIgnoreReturnValue
    @Override
    public Builder<K, V> put(K key, V value) {
      ensureCapacity(size + 1);
      checkEntryNotNull(key, value);
      keys[size] = key;
      values[size] = value;
      size++;
      return this;
    }

    /**
     * Adds the given {@code entry} to the map, making it immutable if necessary. Duplicate keys,
     * according to the comparator (which might be the keys' natural order), are not allowed, and
     * will cause {@link #build} to fail.
     *
     * @since 11.0
     */
    @CanIgnoreReturnValue
    @Override
    public Builder<K, V> put(Entry<? extends K, ? extends V> entry) {
      super.put(entry);
      return this;
    }

    /**
     * Associates all of the given map's keys and values in the built map. Duplicate keys, according
     * to the comparator (which might be the keys' natural order), are not allowed, and will cause
     * {@link #build} to fail.
     *
     * @throws NullPointerException if any key or value in {@code map} is null
     */
    @CanIgnoreReturnValue
    @Override
    public Builder<K, V> putAll(Map<? extends K, ? extends V> map) {
      super.putAll(map);
      return this;
    }

    /**
     * Adds all the given entries to the built map. Duplicate keys, according to the comparator
     * (which might be the keys' natural order), are not allowed, and will cause {@link #build} to
     * fail.
     *
     * @throws NullPointerException if any key, value, or entry is null
     * @since 19.0
     */
    @CanIgnoreReturnValue
    @Override
    public Builder<K, V> putAll(Iterable<? extends Entry<? extends K, ? extends V>> entries) {
      super.putAll(entries);
      return this;
    }

    /**
     * Throws an {@code UnsupportedOperationException}.
     *
     * @since 19.0
     * @deprecated Unsupported by ImmutableSortedMap.Builder.
     */
    @CanIgnoreReturnValue
    @Override
    @Deprecated
    @DoNotCall("Always throws UnsupportedOperationException")
    public final Builder<K, V> orderEntriesByValue(Comparator<? super V> valueComparator) {
      throw new UnsupportedOperationException("Not available on ImmutableSortedMap.Builder");
    }

    @CanIgnoreReturnValue
    Builder<K, V> combine(ImmutableSortedMap.Builder<K, V> other) {
      ensureCapacity(size + other.size);
      System.arraycopy(other.keys, 0, this.keys, this.size, other.size);
      System.arraycopy(other.values, 0, this.values, this.size, other.size);
      size += other.size;
      return this;
    }

    /**
     * Returns a newly-created immutable sorted map.
     *
     * <p>Prefer the equivalent method {@link #buildOrThrow()} to make it explicit that the method
     * will throw an exception if there are duplicate keys. The {@code build()} method will soon be
     * deprecated.
     *
     * @throws IllegalArgumentException if any two keys are equal according to the comparator (which
     *     might be the keys' natural order)
     */
    @Override
    public ImmutableSortedMap<K, V> build() {
      return buildOrThrow();
    }

    /**
     * Returns a newly-created immutable sorted map, or throws an exception if any two keys are
     * equal.
     *
     * @throws IllegalArgumentException if any two keys are equal according to the comparator (which
     *     might be the keys' natural order)
     * @since 31.0
     */
    @Override
    public ImmutableSortedMap<K, V> buildOrThrow() {
      switch (size) {
        case 0:
          return emptyMap(comparator);
        case 1:
          // requireNonNull is safe because the first `size` elements have been filled in.
          return of(comparator, (K) requireNonNull(keys[0]), (V) requireNonNull(values[0]));
        default:
          Object[] sortedKeys = Arrays.copyOf(keys, size);
          Arrays.sort((K[]) sortedKeys, comparator);
          Object[] sortedValues = new Object[size];

          // We might, somehow, be able to reorder values in-place.  But it doesn't seem like
          // there's a way around creating the separate sortedKeys array, and if we're allocating
          // one array of size n, we might as well allocate two -- to say nothing of the allocation
          // done in Arrays.sort.
          for (int i = 0; i < size; i++) {
            if (i > 0 && comparator.compare((K) sortedKeys[i - 1], (K) sortedKeys[i]) == 0) {
              throw new IllegalArgumentException(
                  "keys required to be distinct but compared as equal: "
                      + sortedKeys[i - 1]
                      + " and "
                      + sortedKeys[i]);
            }
            // requireNonNull is safe because the first `size` elements have been filled in.
            int index =
                Arrays.binarySearch((K[]) sortedKeys, (K) requireNonNull(keys[i]), comparator);
            sortedValues[index] = requireNonNull(values[i]);
          }
          return new ImmutableSortedMap<K, V>(
              new RegularImmutableSortedSet<K>(
                  ImmutableList.<K>asImmutableList(sortedKeys), comparator),
              ImmutableList.<V>asImmutableList(sortedValues));
      }
    }

    /**
     * Throws UnsupportedOperationException. A future version may support this operation. Then the
     * value for any given key will be the one that was last supplied in a {@code put} operation for
     * that key.
     *
     * @throws UnsupportedOperationException always
     * @since 31.1
     * @deprecated This method is not currently implemented, and may never be.
     */
    @DoNotCall
    @Deprecated
    @Override
    public final ImmutableSortedMap<K, V> buildKeepingLast() {
      // TODO(emcmanus): implement
      throw new UnsupportedOperationException(
          "ImmutableSortedMap.Builder does not yet implement buildKeepingLast()");
    }
  }

  private final transient RegularImmutableSortedSet<K> keySet;
  private final transient ImmutableList<V> valueList;
  @CheckForNull private transient ImmutableSortedMap<K, V> descendingMap;

  ImmutableSortedMap(RegularImmutableSortedSet<K> keySet, ImmutableList<V> valueList) {
    this(keySet, valueList, null);
  }

  ImmutableSortedMap(
      RegularImmutableSortedSet<K> keySet,
      ImmutableList<V> valueList,
      @CheckForNull ImmutableSortedMap<K, V> descendingMap) {
    this.keySet = keySet;
    this.valueList = valueList;
    this.descendingMap = descendingMap;
  }

  @Override
  public int size() {
    return valueList.size();
  }

  @Override
  @CheckForNull
  public V get(@CheckForNull Object key) {
    int index = keySet.indexOf(key);
    return (index == -1) ? null : valueList.get(index);
  }

  @Override
  boolean isPartialView() {
    return keySet.isPartialView() || valueList.isPartialView();
  }

  /** Returns an immutable set of the mappings in this map, sorted by the key ordering. */
  @Override
  public ImmutableSet<Entry<K, V>> entrySet() {
    return super.entrySet();
  }

  @Override
  ImmutableSet<Entry<K, V>> createEntrySet() {
    class EntrySet extends ImmutableMapEntrySet<K, V> {
      @Override
      public UnmodifiableIterator<Entry<K, V>> iterator() {
        return asList().iterator();
      }

      @Override
      ImmutableList<Entry<K, V>> createAsList() {
        return new ImmutableList<Entry<K, V>>() {
          @Override
          public Entry<K, V> get(int index) {
            return new AbstractMap.SimpleImmutableEntry<>(
                keySet.asList().get(index), valueList.get(index));
          }

          @Override
          boolean isPartialView() {
            return true;
          }

          @Override
          public int size() {
            return ImmutableSortedMap.this.size();
          }

          // redeclare to help optimizers with b/310253115
          @SuppressWarnings("RedundantOverride")
          @Override
          @J2ktIncompatible // serialization
          @GwtIncompatible // serialization
          Object writeReplace() {
            return super.writeReplace();
          }
        };
      }

      @Override
      ImmutableMap<K, V> map() {
        return ImmutableSortedMap.this;
      }

      // redeclare to help optimizers with b/310253115
      @SuppressWarnings("RedundantOverride")
      @Override
      @J2ktIncompatible // serialization
      @GwtIncompatible // serialization
      Object writeReplace() {
        return super.writeReplace();
      }
    }
    return isEmpty() ? ImmutableSet.<Entry<K, V>>of() : new EntrySet();
  }

  /** Returns an immutable sorted set of the keys in this map. */
  @Override
  public ImmutableSortedSet<K> keySet() {
    return keySet;
  }

  @Override
  ImmutableSet<K> createKeySet() {
    throw new AssertionError("should never be called");
  }

  /**
   * Returns an immutable collection of the values in this map, sorted by the ordering of the
   * corresponding keys.
   */
  @Override
  public ImmutableCollection<V> values() {
    return valueList;
  }

  @Override
  ImmutableCollection<V> createValues() {
    throw new AssertionError("should never be called");
  }

  /**
   * Returns the comparator that orders the keys, which is {@link Ordering#natural()} when the
   * natural ordering of the keys is used. Note that its behavior is not consistent with {@link
   * TreeMap#comparator()}, which returns {@code null} to indicate natural ordering.
   */
  @Override
  public Comparator<? super K> comparator() {
    return keySet().comparator();
  }

  @Override
  public K firstKey() {
    return keySet().first();
  }

  @Override
  public K lastKey() {
    return keySet().last();
  }

  private ImmutableSortedMap<K, V> getSubMap(int fromIndex, int toIndex) {
    if (fromIndex == 0 && toIndex == size()) {
      return this;
    } else if (fromIndex == toIndex) {
      return emptyMap(comparator());
    } else {
      return new ImmutableSortedMap<>(
          keySet.getSubSet(fromIndex, toIndex), valueList.subList(fromIndex, toIndex));
    }
  }

  /**
   * This method returns a {@code ImmutableSortedMap}, consisting of the entries whose keys are less
   * than {@code toKey}.
   *
   * <p>The {@link SortedMap#headMap} documentation states that a submap of a submap throws an
   * {@link IllegalArgumentException} if passed a {@code toKey} greater than an earlier {@code
   * toKey}. However, this method doesn't throw an exception in that situation, but instead keeps
   * the original {@code toKey}.
   */
  @Override
  public ImmutableSortedMap<K, V> headMap(K toKey) {
    return headMap(toKey, false);
  }

  /**
   * This method returns a {@code ImmutableSortedMap}, consisting of the entries whose keys are less
   * than (or equal to, if {@code inclusive}) {@code toKey}.
   *
   * <p>The {@link SortedMap#headMap} documentation states that a submap of a submap throws an
   * {@link IllegalArgumentException} if passed a {@code toKey} greater than an earlier {@code
   * toKey}. However, this method doesn't throw an exception in that situation, but instead keeps
   * the original {@code toKey}.
   *
   * @since 12.0
   */
  @Override
  public ImmutableSortedMap<K, V> headMap(K toKey, boolean inclusive) {
    return getSubMap(0, keySet.headIndex(checkNotNull(toKey), inclusive));
  }

  /**
   * This method returns a {@code ImmutableSortedMap}, consisting of the entries whose keys ranges
   * from {@code fromKey}, inclusive, to {@code toKey}, exclusive.
   *
   * <p>The {@link SortedMap#subMap} documentation states that a submap of a submap throws an {@link
   * IllegalArgumentException} if passed a {@code fromKey} less than an earlier {@code fromKey}.
   * However, this method doesn't throw an exception in that situation, but instead keeps the
   * original {@code fromKey}. Similarly, this method keeps the original {@code toKey}, instead of
   * throwing an exception, if passed a {@code toKey} greater than an earlier {@code toKey}.
   */
  @Override
  public ImmutableSortedMap<K, V> subMap(K fromKey, K toKey) {
    return subMap(fromKey, true, toKey, false);
  }

  /**
   * This method returns a {@code ImmutableSortedMap}, consisting of the entries whose keys ranges
   * from {@code fromKey} to {@code toKey}, inclusive or exclusive as indicated by the boolean
   * flags.
   *
   * <p>The {@link SortedMap#subMap} documentation states that a submap of a submap throws an {@link
   * IllegalArgumentException} if passed a {@code fromKey} less than an earlier {@code fromKey}.
   * However, this method doesn't throw an exception in that situation, but instead keeps the
   * original {@code fromKey}. Similarly, this method keeps the original {@code toKey}, instead of
   * throwing an exception, if passed a {@code toKey} greater than an earlier {@code toKey}.
   *
   * @since 12.0
   */
  @Override
  public ImmutableSortedMap<K, V> subMap(
      K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
    checkNotNull(fromKey);
    checkNotNull(toKey);
    checkArgument(
        comparator().compare(fromKey, toKey) <= 0,
        "expected fromKey <= toKey but %s > %s",
        fromKey,
        toKey);
    return headMap(toKey, toInclusive).tailMap(fromKey, fromInclusive);
  }

  /**
   * This method returns a {@code ImmutableSortedMap}, consisting of the entries whose keys are
   * greater than or equals to {@code fromKey}.
   *
   * <p>The {@link SortedMap#tailMap} documentation states that a submap of a submap throws an
   * {@link IllegalArgumentException} if passed a {@code fromKey} less than an earlier {@code
   * fromKey}. However, this method doesn't throw an exception in that situation, but instead keeps
   * the original {@code fromKey}.
   */
  @Override
  public ImmutableSortedMap<K, V> tailMap(K fromKey) {
    return tailMap(fromKey, true);
  }

  /**
   * This method returns a {@code ImmutableSortedMap}, consisting of the entries whose keys are
   * greater than (or equal to, if {@code inclusive}) {@code fromKey}.
   *
   * <p>The {@link SortedMap#tailMap} documentation states that a submap of a submap throws an
   * {@link IllegalArgumentException} if passed a {@code fromKey} less than an earlier {@code
   * fromKey}. However, this method doesn't throw an exception in that situation, but instead keeps
   * the original {@code fromKey}.
   *
   * @since 12.0
   */
  @Override
  public ImmutableSortedMap<K, V> tailMap(K fromKey, boolean inclusive) {
    return getSubMap(keySet.tailIndex(checkNotNull(fromKey), inclusive), size());
  }

  @Override
  @CheckForNull
  public Entry<K, V> lowerEntry(K key) {
    return headMap(key, false).lastEntry();
  }

  @Override
  @CheckForNull
  public K lowerKey(K key) {
    return keyOrNull(lowerEntry(key));
  }

  @Override
  @CheckForNull
  public Entry<K, V> floorEntry(K key) {
    return headMap(key, true).lastEntry();
  }

  @Override
  @CheckForNull
  public K floorKey(K key) {
    return keyOrNull(floorEntry(key));
  }

  @Override
  @CheckForNull
  public Entry<K, V> ceilingEntry(K key) {
    return tailMap(key, true).firstEntry();
  }

  @Override
  @CheckForNull
  public K ceilingKey(K key) {
    return keyOrNull(ceilingEntry(key));
  }

  @Override
  @CheckForNull
  public Entry<K, V> higherEntry(K key) {
    return tailMap(key, false).firstEntry();
  }

  @Override
  @CheckForNull
  public K higherKey(K key) {
    return keyOrNull(higherEntry(key));
  }

  @Override
  @CheckForNull
  public Entry<K, V> firstEntry() {
    return isEmpty() ? null : entrySet().asList().get(0);
  }

  @Override
  @CheckForNull
  public Entry<K, V> lastEntry() {
    return isEmpty() ? null : entrySet().asList().get(size() - 1);
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
  @DoNotCall("Always throws UnsupportedOperationException")
  @CheckForNull
  public final Entry<K, V> pollFirstEntry() {
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
  @DoNotCall("Always throws UnsupportedOperationException")
  @CheckForNull
  public final Entry<K, V> pollLastEntry() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ImmutableSortedMap<K, V> descendingMap() {
    // TODO(kevinb): The descendingMap is never actually cached at all. Either:
    //
    // - Cache it, and annotate the field with @LazyInit.
    // - Simplify the code below, and consider eliminating the field (b/287198172), which is also
    //   set by one of the constructors.
    ImmutableSortedMap<K, V> result = descendingMap;
    if (result == null) {
      if (isEmpty()) {
        return emptyMap(Ordering.from(comparator()).reverse());
      } else {
        return new ImmutableSortedMap<>(
            (RegularImmutableSortedSet<K>) keySet.descendingSet(), valueList.reverse(), this);
      }
    }
    return result;
  }

  @Override
  public ImmutableSortedSet<K> navigableKeySet() {
    return keySet;
  }

  @Override
  public ImmutableSortedSet<K> descendingKeySet() {
    return keySet.descendingSet();
  }

  /**
   * Serialized type for all ImmutableSortedMap instances. It captures the logical contents and they
   * are reconstructed using public factory methods. This ensures that the implementation types
   * remain as implementation details.
   */
  @J2ktIncompatible // serialization
  private static class SerializedForm<K, V> extends ImmutableMap.SerializedForm<K, V> {
    private final Comparator<? super K> comparator;

    SerializedForm(ImmutableSortedMap<K, V> sortedMap) {
      super(sortedMap);
      comparator = sortedMap.comparator();
    }

    @Override
    Builder<K, V> makeBuilder(int size) {
      return new Builder<>(comparator);
    }

    private static final long serialVersionUID = 0;
  }

  @Override
  @J2ktIncompatible // serialization
  Object writeReplace() {
    return new SerializedForm<>(this);
  }

  @J2ktIncompatible // java.io.ObjectInputStream
  private void readObject(ObjectInputStream stream) throws InvalidObjectException {
    throw new InvalidObjectException("Use SerializedForm");
  }

  // This class is never actually serialized directly, but we have to make the
  // warning go away (and suppressing would suppress for all nested classes too)
  private static final long serialVersionUID = 0;

  /**
   * Not supported. Use {@link #toImmutableSortedMap}, which offers better type-safety, instead.
   * This method exists only to hide {@link ImmutableMap#toImmutableMap} from consumers of {@code
   * ImmutableSortedMap}.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Use {@link ImmutableSortedMap#toImmutableSortedMap}.
   */
  @DoNotCall("Use toImmutableSortedMap")
  @Deprecated
  @SuppressWarnings({"AndroidJdkLibsChecker", "Java7ApiChecker"})
  @IgnoreJRERequirement // Users will use this only if they're already using streams.
  static <T extends @Nullable Object, K, V> Collector<T, ?, ImmutableMap<K, V>> toImmutableMap(
      Function<? super T, ? extends K> keyFunction,
      Function<? super T, ? extends V> valueFunction) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. Use {@link #toImmutableSortedMap}, which offers better type-safety, instead.
   * This method exists only to hide {@link ImmutableMap#toImmutableMap} from consumers of {@code
   * ImmutableSortedMap}.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Use {@link ImmutableSortedMap#toImmutableSortedMap}.
   */
  @DoNotCall("Use toImmutableSortedMap")
  @Deprecated
  @SuppressWarnings({"AndroidJdkLibsChecker", "Java7ApiChecker"})
  @IgnoreJRERequirement // Users will use this only if they're already using streams.
  static <T extends @Nullable Object, K, V> Collector<T, ?, ImmutableMap<K, V>> toImmutableMap(
      Function<? super T, ? extends K> keyFunction,
      Function<? super T, ? extends V> valueFunction,
      BinaryOperator<V> mergeFunction) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. Use {@link #naturalOrder}, which offers better type-safety, instead. This method
   * exists only to hide {@link ImmutableMap#builder} from consumers of {@code ImmutableSortedMap}.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Use {@link ImmutableSortedMap#naturalOrder}, which offers better type-safety.
   */
  @DoNotCall("Use naturalOrder")
  @Deprecated
  public static <K, V> ImmutableSortedMap.Builder<K, V> builder() {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported for ImmutableSortedMap.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Not supported for ImmutableSortedMap.
   */
  @DoNotCall("Use naturalOrder (which does not accept an expected size)")
  @Deprecated
  public static <K, V> ImmutableSortedMap.Builder<K, V> builderWithExpectedSize(int expectedSize) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. <b>You are attempting to create a map that may contain a non-{@code Comparable}
   * key.</b> Proper calls will resolve to the version in {@code ImmutableSortedMap}, not this dummy
   * version.
   *
   * @throws UnsupportedOperationException always
   * @deprecated <b>Pass a key of type {@code Comparable} to use {@link
   *     ImmutableSortedMap#of(Comparable, Object)}.</b>
   */
  @DoNotCall("Pass a key of type Comparable")
  @Deprecated
  public static <K, V> ImmutableSortedMap<K, V> of(K k1, V v1) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. <b>You are attempting to create a map that may contain non-{@code Comparable}
   * keys.</b> Proper calls will resolve to the version in {@code ImmutableSortedMap}, not this
   * dummy version.
   *
   * @throws UnsupportedOperationException always
   * @deprecated <b>Pass keys of type {@code Comparable} to use {@link
   *     ImmutableSortedMap#of(Comparable, Object, Comparable, Object)}.</b>
   */
  @DoNotCall("Pass keys of type Comparable")
  @Deprecated
  public static <K, V> ImmutableSortedMap<K, V> of(K k1, V v1, K k2, V v2) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. <b>You are attempting to create a map that may contain non-{@code Comparable}
   * keys.</b> Proper calls to will resolve to the version in {@code ImmutableSortedMap}, not this
   * dummy version.
   *
   * @throws UnsupportedOperationException always
   * @deprecated <b>Pass keys of type {@code Comparable} to use {@link
   *     ImmutableSortedMap#of(Comparable, Object, Comparable, Object, Comparable, Object)}.</b>
   */
  @DoNotCall("Pass keys of type Comparable")
  @Deprecated
  public static <K, V> ImmutableSortedMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. <b>You are attempting to create a map that may contain non-{@code Comparable}
   * keys.</b> Proper calls will resolve to the version in {@code ImmutableSortedMap}, not this
   * dummy version.
   *
   * @throws UnsupportedOperationException always
   * @deprecated <b>Pass keys of type {@code Comparable} to use {@link
   *     ImmutableSortedMap#of(Comparable, Object, Comparable, Object, Comparable, Object,
   *     Comparable, Object)}.</b>
   */
  @DoNotCall("Pass keys of type Comparable")
  @Deprecated
  public static <K, V> ImmutableSortedMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. <b>You are attempting to create a map that may contain non-{@code Comparable}
   * keys.</b> Proper calls will resolve to the version in {@code ImmutableSortedMap}, not this
   * dummy version.
   *
   * @throws UnsupportedOperationException always
   * @deprecated <b>Pass keys of type {@code Comparable} to use {@link
   *     ImmutableSortedMap#of(Comparable, Object, Comparable, Object, Comparable, Object,
   *     Comparable, Object, Comparable, Object)}.</b>
   */
  @DoNotCall("Pass keys of type Comparable")
  @Deprecated
  public static <K, V> ImmutableSortedMap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. <b>You are attempting to create a map that may contain non-{@code Comparable}
   * keys.</b> Proper calls will resolve to the version in {@code ImmutableSortedMap}, not this
   * dummy version.
   *
   * @throws UnsupportedOperationException always
   * @deprecated <b>Pass keys of type {@code Comparable} to use {@link
   *     ImmutableSortedMap#of(Comparable, Object, Comparable, Object, Comparable, Object,
   *     Comparable, Object, Comparable, Object)}.</b>
   */
  @DoNotCall("Pass keys of type Comparable")
  @Deprecated
  public static <K, V> ImmutableSortedMap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. <b>You are attempting to create a map that may contain non-{@code Comparable}
   * keys.</b> Proper calls will resolve to the version in {@code ImmutableSortedMap}, not this
   * dummy version.
   *
   * @throws UnsupportedOperationException always
   * @deprecated <b>Pass keys of type {@code Comparable} to use {@link
   *     ImmutableSortedMap#of(Comparable, Object, Comparable, Object, Comparable, Object,
   *     Comparable, Object, Comparable, Object)}.</b>
   */
  @DoNotCall("Pass keys of type Comparable")
  @Deprecated
  public static <K, V> ImmutableSortedMap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. <b>You are attempting to create a map that may contain non-{@code Comparable}
   * keys.</b> Proper calls will resolve to the version in {@code ImmutableSortedMap}, not this
   * dummy version.
   *
   * @throws UnsupportedOperationException always
   * @deprecated <b>Pass keys of type {@code Comparable} to use {@link
   *     ImmutableSortedMap#of(Comparable, Object, Comparable, Object, Comparable, Object,
   *     Comparable, Object, Comparable, Object)}.</b>
   */
  @DoNotCall("Pass keys of type Comparable")
  @Deprecated
  public static <K, V> ImmutableSortedMap<K, V> of(
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
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. <b>You are attempting to create a map that may contain non-{@code Comparable}
   * keys.</b> Proper calls will resolve to the version in {@code ImmutableSortedMap}, not this
   * dummy version.
   *
   * @throws UnsupportedOperationException always
   * @deprecated <b>Pass keys of type {@code Comparable} to use {@link
   *     ImmutableSortedMap#of(Comparable, Object, Comparable, Object, Comparable, Object,
   *     Comparable, Object, Comparable, Object)}.</b>
   */
  @DoNotCall("Pass keys of type Comparable")
  @Deprecated
  public static <K, V> ImmutableSortedMap<K, V> of(
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
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. <b>You are attempting to create a map that may contain non-{@code Comparable}
   * keys.</b> Proper calls will resolve to the version in {@code ImmutableSortedMap}, not this
   * dummy version.
   *
   * @throws UnsupportedOperationException always
   * @deprecated <b>Pass keys of type {@code Comparable} to use {@link
   *     ImmutableSortedMap#of(Comparable, Object, Comparable, Object, Comparable, Object,
   *     Comparable, Object, Comparable, Object)}.</b>
   */
  @DoNotCall("Pass keys of type Comparable")
  @Deprecated
  public static <K, V> ImmutableSortedMap<K, V> of(
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
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. Use {@code ImmutableSortedMap.copyOf(ImmutableMap.ofEntries(...))}.
   *
   * @deprecated Use {@code ImmutableSortedMap.copyOf(ImmutableMap.ofEntries(...))}.
   */
  @DoNotCall("ImmutableSortedMap.ofEntries not currently available; use ImmutableSortedMap.copyOf")
  @Deprecated
  public static <K, V> ImmutableSortedMap<K, V> ofEntries(
      Entry<? extends K, ? extends V>... entries) {
    throw new UnsupportedOperationException();
  }
}

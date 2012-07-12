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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.base.Joiner.MapJoiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.MapDifference.ValueDifference;
import com.google.common.primitives.Ints;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nullable;

/**
 * Static utility methods pertaining to {@link Map} instances (including instances of
 * {@link SortedMap}, {@link BiMap}, etc.). Also see this class's counterparts
 * {@link Lists}, {@link Sets} and {@link Queues}.
 *
 * <p>See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/CollectionUtilitiesExplained#Maps">
 * {@code Maps}</a>.
 *
 * @author Kevin Bourrillion
 * @author Mike Bostock
 * @author Isaac Shum
 * @author Louis Wasserman
 * @since 2.0 (imported from Google Collections Library)
 */
@GwtCompatible(emulated = true)
public final class Maps {
  private Maps() {}

  /**
   * Creates a <i>mutable</i>, empty {@code HashMap} instance.
   *
   * <p><b>Note:</b> if mutability is not required, use {@link
   * ImmutableMap#of()} instead.
   *
   * <p><b>Note:</b> if {@code K} is an {@code enum} type, use {@link
   * #newEnumMap} instead.
   *
   * @return a new, empty {@code HashMap}
   */
  public static <K, V> HashMap<K, V> newHashMap() {
    return new HashMap<K, V>();
  }

  /**
   * Creates a {@code HashMap} instance, with a high enough "initial capacity"
   * that it <i>should</i> hold {@code expectedSize} elements without growth.
   * This behavior cannot be broadly guaranteed, but it is observed to be true
   * for OpenJDK 1.6. It also can't be guaranteed that the method isn't
   * inadvertently <i>oversizing</i> the returned map.
   *
   * @param expectedSize the number of elements you expect to add to the
   *        returned map
   * @return a new, empty {@code HashMap} with enough capacity to hold {@code
   *         expectedSize} elements without resizing
   * @throws IllegalArgumentException if {@code expectedSize} is negative
   */
  public static <K, V> HashMap<K, V> newHashMapWithExpectedSize(
      int expectedSize) {
    return new HashMap<K, V>(capacity(expectedSize));
  }

  /**
   * Returns a capacity that is sufficient to keep the map from being resized as
   * long as it grows no larger than expectedSize and the load factor is >= its
   * default (0.75).
   */
  static int capacity(int expectedSize) {
    if (expectedSize < 3) {
      checkArgument(expectedSize >= 0);
      return expectedSize + 1;
    }
    if (expectedSize < Ints.MAX_POWER_OF_TWO) {
      return expectedSize + expectedSize / 3;
    }
    return Integer.MAX_VALUE; // any large value
  }

  /**
   * Creates a <i>mutable</i> {@code HashMap} instance with the same mappings as
   * the specified map.
   *
   * <p><b>Note:</b> if mutability is not required, use {@link
   * ImmutableMap#copyOf(Map)} instead.
   *
   * <p><b>Note:</b> if {@code K} is an {@link Enum} type, use {@link
   * #newEnumMap} instead.
   *
   * @param map the mappings to be placed in the new map
   * @return a new {@code HashMap} initialized with the mappings from {@code
   *         map}
   */
  public static <K, V> HashMap<K, V> newHashMap(
      Map<? extends K, ? extends V> map) {
    return new HashMap<K, V>(map);
  }

  /**
   * Creates a <i>mutable</i>, empty, insertion-ordered {@code LinkedHashMap}
   * instance.
   *
   * <p><b>Note:</b> if mutability is not required, use {@link
   * ImmutableMap#of()} instead.
   *
   * @return a new, empty {@code LinkedHashMap}
   */
  public static <K, V> LinkedHashMap<K, V> newLinkedHashMap() {
    return new LinkedHashMap<K, V>();
  }

  /**
   * Creates a <i>mutable</i>, insertion-ordered {@code LinkedHashMap} instance
   * with the same mappings as the specified map.
   *
   * <p><b>Note:</b> if mutability is not required, use {@link
   * ImmutableMap#copyOf(Map)} instead.
   *
   * @param map the mappings to be placed in the new map
   * @return a new, {@code LinkedHashMap} initialized with the mappings from
   *         {@code map}
   */
  public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(
      Map<? extends K, ? extends V> map) {
    return new LinkedHashMap<K, V>(map);
  }

  /**
   * Returns a general-purpose instance of {@code ConcurrentMap}, which supports
   * all optional operations of the ConcurrentMap interface. It does not permit
   * null keys or values. It is serializable.
   *
   * <p>This is currently accomplished by calling {@link MapMaker#makeMap()}.
   *
   * <p>It is preferable to use {@code MapMaker} directly (rather than through
   * this method), as it presents numerous useful configuration options,
   * such as the concurrency level, load factor, key/value reference types,
   * and value computation.
   *
   * @return a new, empty {@code ConcurrentMap}
   * @since 3.0
   */
  public static <K, V> ConcurrentMap<K, V> newConcurrentMap() {
    return new MapMaker().<K, V>makeMap();
  }

  /**
   * Creates a <i>mutable</i>, empty {@code TreeMap} instance using the natural
   * ordering of its elements.
   *
   * <p><b>Note:</b> if mutability is not required, use {@link
   * ImmutableSortedMap#of()} instead.
   *
   * @return a new, empty {@code TreeMap}
   */
  public static <K extends Comparable, V> TreeMap<K, V> newTreeMap() {
    return new TreeMap<K, V>();
  }

  /**
   * Creates a <i>mutable</i> {@code TreeMap} instance with the same mappings as
   * the specified map and using the same ordering as the specified map.
   *
   * <p><b>Note:</b> if mutability is not required, use {@link
   * ImmutableSortedMap#copyOfSorted(SortedMap)} instead.
   *
   * @param map the sorted map whose mappings are to be placed in the new map
   *        and whose comparator is to be used to sort the new map
   * @return a new {@code TreeMap} initialized with the mappings from {@code
   *         map} and using the comparator of {@code map}
   */
  public static <K, V> TreeMap<K, V> newTreeMap(SortedMap<K, ? extends V> map) {
    return new TreeMap<K, V>(map);
  }

  /**
   * Creates a <i>mutable</i>, empty {@code TreeMap} instance using the given
   * comparator.
   *
   * <p><b>Note:</b> if mutability is not required, use {@code
   * ImmutableSortedMap.orderedBy(comparator).build()} instead.
   *
   * @param comparator the comparator to sort the keys with
   * @return a new, empty {@code TreeMap}
   */
  public static <C, K extends C, V> TreeMap<K, V> newTreeMap(
      @Nullable Comparator<C> comparator) {
    // Ideally, the extra type parameter "C" shouldn't be necessary. It is a
    // work-around of a compiler type inference quirk that prevents the
    // following code from being compiled:
    // Comparator<Class<?>> comparator = null;
    // Map<Class<? extends Throwable>, String> map = newTreeMap(comparator);
    return new TreeMap<K, V>(comparator);
  }

  /**
   * Creates an {@code EnumMap} instance.
   *
   * @param type the key type for this map
   * @return a new, empty {@code EnumMap}
   */
  public static <K extends Enum<K>, V> EnumMap<K, V> newEnumMap(Class<K> type) {
    return new EnumMap<K, V>(checkNotNull(type));
  }

  /**
   * Creates an {@code EnumMap} with the same mappings as the specified map.
   *
   * @param map the map from which to initialize this {@code EnumMap}
   * @return a new {@code EnumMap} initialized with the mappings from {@code
   *         map}
   * @throws IllegalArgumentException if {@code m} is not an {@code EnumMap}
   *         instance and contains no mappings
   */
  public static <K extends Enum<K>, V> EnumMap<K, V> newEnumMap(
      Map<K, ? extends V> map) {
    return new EnumMap<K, V>(map);
  }

  /**
   * Creates an {@code IdentityHashMap} instance.
   *
   * @return a new, empty {@code IdentityHashMap}
   */
  public static <K, V> IdentityHashMap<K, V> newIdentityHashMap() {
    return new IdentityHashMap<K, V>();
  }

  /**
   * Computes the difference between two maps. This difference is an immutable
   * snapshot of the state of the maps at the time this method is called. It
   * will never change, even if the maps change at a later time.
   *
   * <p>Since this method uses {@code HashMap} instances internally, the keys of
   * the supplied maps must be well-behaved with respect to
   * {@link Object#equals} and {@link Object#hashCode}.
   *
   * <p><b>Note:</b>If you only need to know whether two maps have the same
   * mappings, call {@code left.equals(right)} instead of this method.
   *
   * @param left the map to treat as the "left" map for purposes of comparison
   * @param right the map to treat as the "right" map for purposes of comparison
   * @return the difference between the two maps
   */
  @SuppressWarnings("unchecked")
  public static <K, V> MapDifference<K, V> difference(
      Map<? extends K, ? extends V> left, Map<? extends K, ? extends V> right) {
    if (left instanceof SortedMap) {
      SortedMap<K, ? extends V> sortedLeft = (SortedMap<K, ? extends V>) left;
      SortedMapDifference<K, V> result = difference(sortedLeft, right);
      return result;
    }
    return difference(left, right, Equivalence.equals());
  }

  /**
   * Computes the difference between two maps. This difference is an immutable
   * snapshot of the state of the maps at the time this method is called. It
   * will never change, even if the maps change at a later time.
   *
   * <p>Values are compared using a provided equivalence, in the case of
   * equality, the value on the 'left' is returned in the difference.
   *
   * <p>Since this method uses {@code HashMap} instances internally, the keys of
   * the supplied maps must be well-behaved with respect to
   * {@link Object#equals} and {@link Object#hashCode}.
   *
   * @param left the map to treat as the "left" map for purposes of comparison
   * @param right the map to treat as the "right" map for purposes of comparison
   * @param valueEquivalence the equivalence relationship to use to compare
   *    values
   * @return the difference between the two maps
   * @since 10.0
   */
  @Beta
  public static <K, V> MapDifference<K, V> difference(
      Map<? extends K, ? extends V> left, Map<? extends K, ? extends V> right,
      Equivalence<? super V> valueEquivalence) {
    Preconditions.checkNotNull(valueEquivalence);

    Map<K, V> onlyOnLeft = newHashMap();
    Map<K, V> onlyOnRight = new HashMap<K, V>(right); // will whittle it down
    Map<K, V> onBoth = newHashMap();
    Map<K, MapDifference.ValueDifference<V>> differences = newHashMap();
    boolean eq = true;

    for (Entry<? extends K, ? extends V> entry : left.entrySet()) {
      K leftKey = entry.getKey();
      V leftValue = entry.getValue();
      if (right.containsKey(leftKey)) {
        V rightValue = onlyOnRight.remove(leftKey);
        if (valueEquivalence.equivalent(leftValue, rightValue)) {
          onBoth.put(leftKey, leftValue);
        } else {
          eq = false;
          differences.put(
              leftKey, ValueDifferenceImpl.create(leftValue, rightValue));
        }
      } else {
        eq = false;
        onlyOnLeft.put(leftKey, leftValue);
      }
    }

    boolean areEqual = eq && onlyOnRight.isEmpty();
    return mapDifference(
        areEqual, onlyOnLeft, onlyOnRight, onBoth, differences);
  }

  private static <K, V> MapDifference<K, V> mapDifference(boolean areEqual,
      Map<K, V> onlyOnLeft, Map<K, V> onlyOnRight, Map<K, V> onBoth,
      Map<K, ValueDifference<V>> differences) {
    return new MapDifferenceImpl<K, V>(areEqual,
        Collections.unmodifiableMap(onlyOnLeft),
        Collections.unmodifiableMap(onlyOnRight),
        Collections.unmodifiableMap(onBoth),
        Collections.unmodifiableMap(differences));
  }

  static class MapDifferenceImpl<K, V> implements MapDifference<K, V> {
    final boolean areEqual;
    final Map<K, V> onlyOnLeft;
    final Map<K, V> onlyOnRight;
    final Map<K, V> onBoth;
    final Map<K, ValueDifference<V>> differences;

    MapDifferenceImpl(boolean areEqual, Map<K, V> onlyOnLeft,
        Map<K, V> onlyOnRight, Map<K, V> onBoth,
        Map<K, ValueDifference<V>> differences) {
      this.areEqual = areEqual;
      this.onlyOnLeft = onlyOnLeft;
      this.onlyOnRight = onlyOnRight;
      this.onBoth = onBoth;
      this.differences = differences;
    }

    @Override
    public boolean areEqual() {
      return areEqual;
    }

    @Override
    public Map<K, V> entriesOnlyOnLeft() {
      return onlyOnLeft;
    }

    @Override
    public Map<K, V> entriesOnlyOnRight() {
      return onlyOnRight;
    }

    @Override
    public Map<K, V> entriesInCommon() {
      return onBoth;
    }

    @Override
    public Map<K, ValueDifference<V>> entriesDiffering() {
      return differences;
    }

    @Override public boolean equals(Object object) {
      if (object == this) {
        return true;
      }
      if (object instanceof MapDifference) {
        MapDifference<?, ?> other = (MapDifference<?, ?>) object;
        return entriesOnlyOnLeft().equals(other.entriesOnlyOnLeft())
            && entriesOnlyOnRight().equals(other.entriesOnlyOnRight())
            && entriesInCommon().equals(other.entriesInCommon())
            && entriesDiffering().equals(other.entriesDiffering());
      }
      return false;
    }

    @Override public int hashCode() {
      return Objects.hashCode(entriesOnlyOnLeft(), entriesOnlyOnRight(),
          entriesInCommon(), entriesDiffering());
    }

    @Override public String toString() {
      if (areEqual) {
        return "equal";
      }

      StringBuilder result = new StringBuilder("not equal");
      if (!onlyOnLeft.isEmpty()) {
        result.append(": only on left=").append(onlyOnLeft);
      }
      if (!onlyOnRight.isEmpty()) {
        result.append(": only on right=").append(onlyOnRight);
      }
      if (!differences.isEmpty()) {
        result.append(": value differences=").append(differences);
      }
      return result.toString();
    }
  }

  static class ValueDifferenceImpl<V>
      implements MapDifference.ValueDifference<V> {
    private final V left;
    private final V right;

    static <V> ValueDifference<V> create(@Nullable V left, @Nullable V right) {
      return new ValueDifferenceImpl<V>(left, right);
    }

    private ValueDifferenceImpl(@Nullable V left, @Nullable V right) {
      this.left = left;
      this.right = right;
    }

    @Override
    public V leftValue() {
      return left;
    }

    @Override
    public V rightValue() {
      return right;
    }

    @Override public boolean equals(@Nullable Object object) {
      if (object instanceof MapDifference.ValueDifference) {
        MapDifference.ValueDifference<?> that =
            (MapDifference.ValueDifference<?>) object;
        return Objects.equal(this.left, that.leftValue())
            && Objects.equal(this.right, that.rightValue());
      }
      return false;
    }

    @Override public int hashCode() {
      return Objects.hashCode(left, right);
    }

    @Override public String toString() {
      return "(" + left + ", " + right + ")";
    }
  }

  /**
   * Computes the difference between two sorted maps, using the comparator of
   * the left map, or {@code Ordering.natural()} if the left map uses the
   * natural ordering of its elements. This difference is an immutable snapshot
   * of the state of the maps at the time this method is called. It will never
   * change, even if the maps change at a later time.
   *
   * <p>Since this method uses {@code TreeMap} instances internally, the keys of
   * the right map must all compare as distinct according to the comparator
   * of the left map.
   *
   * <p><b>Note:</b>If you only need to know whether two sorted maps have the
   * same mappings, call {@code left.equals(right)} instead of this method.
   *
   * @param left the map to treat as the "left" map for purposes of comparison
   * @param right the map to treat as the "right" map for purposes of comparison
   * @return the difference between the two maps
   * @since 11.0
   */
  public static <K, V> SortedMapDifference<K, V> difference(
      SortedMap<K, ? extends V> left, Map<? extends K, ? extends V> right) {
    checkNotNull(left);
    checkNotNull(right);
    Comparator<? super K> comparator = orNaturalOrder(left.comparator());
    SortedMap<K, V> onlyOnLeft = Maps.newTreeMap(comparator);
    SortedMap<K, V> onlyOnRight = Maps.newTreeMap(comparator);
    onlyOnRight.putAll(right); // will whittle it down
    SortedMap<K, V> onBoth = Maps.newTreeMap(comparator);
    SortedMap<K, MapDifference.ValueDifference<V>> differences =
        Maps.newTreeMap(comparator);
    boolean eq = true;

    for (Entry<? extends K, ? extends V> entry : left.entrySet()) {
      K leftKey = entry.getKey();
      V leftValue = entry.getValue();
      if (right.containsKey(leftKey)) {
        V rightValue = onlyOnRight.remove(leftKey);
        if (Objects.equal(leftValue, rightValue)) {
          onBoth.put(leftKey, leftValue);
        } else {
          eq = false;
          differences.put(
              leftKey, ValueDifferenceImpl.create(leftValue, rightValue));
        }
      } else {
        eq = false;
        onlyOnLeft.put(leftKey, leftValue);
      }
    }

    boolean areEqual = eq && onlyOnRight.isEmpty();
    return sortedMapDifference(
        areEqual, onlyOnLeft, onlyOnRight, onBoth, differences);
  }

  private static <K, V> SortedMapDifference<K, V> sortedMapDifference(
      boolean areEqual, SortedMap<K, V> onlyOnLeft, SortedMap<K, V> onlyOnRight,
      SortedMap<K, V> onBoth, SortedMap<K, ValueDifference<V>> differences) {
    return new SortedMapDifferenceImpl<K, V>(areEqual,
        Collections.unmodifiableSortedMap(onlyOnLeft),
        Collections.unmodifiableSortedMap(onlyOnRight),
        Collections.unmodifiableSortedMap(onBoth),
        Collections.unmodifiableSortedMap(differences));
  }

  static class SortedMapDifferenceImpl<K, V> extends MapDifferenceImpl<K, V>
      implements SortedMapDifference<K, V> {
    SortedMapDifferenceImpl(boolean areEqual, SortedMap<K, V> onlyOnLeft,
        SortedMap<K, V> onlyOnRight, SortedMap<K, V> onBoth,
        SortedMap<K, ValueDifference<V>> differences) {
      super(areEqual, onlyOnLeft, onlyOnRight, onBoth, differences);
    }

    @Override public SortedMap<K, ValueDifference<V>> entriesDiffering() {
      return (SortedMap<K, ValueDifference<V>>) super.entriesDiffering();
    }

    @Override public SortedMap<K, V> entriesInCommon() {
      return (SortedMap<K, V>) super.entriesInCommon();
    }

    @Override public SortedMap<K, V> entriesOnlyOnLeft() {
      return (SortedMap<K, V>) super.entriesOnlyOnLeft();
    }

    @Override public SortedMap<K, V> entriesOnlyOnRight() {
      return (SortedMap<K, V>) super.entriesOnlyOnRight();
    }
  }

  /**
   * Returns the specified comparator if not null; otherwise returns {@code
   * Ordering.natural()}. This method is an abomination of generics; the only
   * purpose of this method is to contain the ugly type-casting in one place.
   */
  @SuppressWarnings("unchecked")
  static <E> Comparator<? super E> orNaturalOrder(
      @Nullable Comparator<? super E> comparator) {
    if (comparator != null) { // can't use ? : because of javac bug 5080917
      return comparator;
    }
    return (Comparator<E>) Ordering.natural();
  }

  /**
   * Returns a view of the set as a map, mapping keys from the set according to
   * the specified function.
   *
   * <p>Specifically, for each {@code k} in the backing set, the returned map
   * has an entry mapping {@code k} to {@code function.apply(k)}. The {@code
   * keySet}, {@code values}, and {@code entrySet} views of the returned map
   * iterate in the same order as the backing set.
   *
   * <p>Modifications to the backing set are read through to the returned map.
   * The returned map supports removal operations if the backing set does.
   * Removal operations write through to the backing set.  The returned map
   * does not support put operations.
   *
   * <p><b>Warning</b>: If the function rejects {@code null}, caution is
   * required to make sure the set does not contain {@code null}, because the
   * view cannot stop {@code null} from being added to the set.
   *
   * <p><b>Warning:</b> This method assumes that for any instance {@code k} of
   * key type {@code K}, {@code k.equals(k2)} implies that {@code k2} is also
   * of type {@code K}. Using a key type for which this may not hold, such as
   * {@code ArrayList}, may risk a {@code ClassCastException} when calling
   * methods on the resulting map view.
   */
  @Beta
  static <K, V> Map<K, V> asMap(
      Set<K> set, Function<? super K, V> function) {
    if (set instanceof SortedSet) {
      return asMap((SortedSet<K>) set, function);
    } else {
      return new AsMapView<K, V>(set, function);
    }
  }

  /**
   * Returns a view of the sorted set as a map, mapping keys from the set
   * according to the specified function.
   *
   * <p>Specifically, for each {@code k} in the backing set, the returned map
   * has an entry mapping {@code k} to {@code function.apply(k)}. The {@code
   * keySet}, {@code values}, and {@code entrySet} views of the returned map
   * iterate in the same order as the backing set.
   *
   * <p>Modifications to the backing set are read through to the returned map.
   * The returned map supports removal operations if the backing set does.
   * Removal operations write through to the backing set.  The returned map does
   * not support put operations.
   *
   * <p><b>Warning</b>: If the function rejects {@code null}, caution is
   * required to make sure the set does not contain {@code null}, because the
   * view cannot stop {@code null} from being added to the set.
   *
   * <p><b>Warning:</b> This method assumes that for any instance {@code k} of
   * key type {@code K}, {@code k.equals(k2)} implies that {@code k2} is also of
   * type {@code K}. Using a key type for which this may not hold, such as
   * {@code ArrayList}, may risk a {@code ClassCastException} when calling
   * methods on the resulting map view.
   */
  @Beta
  static <K, V> SortedMap<K, V> asMap(
      SortedSet<K> set, Function<? super K, V> function) {
    // TODO: NavigableSet overloads
    return new SortedAsMapView<K, V>(set, function);
  }

  private static class AsMapView<K, V> extends ImprovedAbstractMap<K, V> {

    private final Set<K> set;
    final Function<? super K, V> function;

    Set<K> backingSet() {
      return set;
    }

    AsMapView(Set<K> set, Function<? super K, V> function) {
      this.set = checkNotNull(set);
      this.function = checkNotNull(function);
    }

    @Override
    public Set<K> keySet() {
      // probably not worth caching
      return new ForwardingSet<K>() {
        @Override
        protected Set<K> delegate() {
          return set;
        }

        @Override
        public boolean add(K element) {
          throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends K> collection) {
          throw new UnsupportedOperationException();
        }
      };
    }

    @Override
    public Collection<V> values() {
      // probably not worth caching
      return Collections2.transform(set, function);
    }

    @Override
    public int size() {
      return set.size();
    }

    @Override
    public boolean containsKey(@Nullable Object key) {
      return set.contains(key);
    }

    @Override
    public V get(@Nullable Object key) {
      if (set.contains(key)) {
        @SuppressWarnings("unchecked") // unsafe, but Javadoc warns about it
        K k = (K) key;
        return function.apply(k);
      } else {
        return null;
      }
    }

    @Override
    public V remove(@Nullable Object key) {
      if (set.remove(key)) {
        @SuppressWarnings("unchecked") // unsafe, but Javadoc warns about it
        K k = (K) key;
        return function.apply(k);
      } else {
        return null;
      }
    }

    @Override
    public void clear() {
      set.clear();
    }

    @Override
    protected Set<Entry<K, V>> createEntrySet() {
      return new EntrySet<K, V>() {
        @Override
        Map<K, V> map() {
          return AsMapView.this;
        }

        @Override
        public Iterator<Entry<K, V>> iterator() {
          final Iterator<K> backingIterator = set.iterator();
          return new Iterator<Entry<K, V>>() {
            @Override
            public boolean hasNext() {
              return backingIterator.hasNext();
            }

            @Override
            public Entry<K, V> next() {
              K k = backingIterator.next();
              return Maps.immutableEntry(k, function.apply(k));
            }

            @Override
            public void remove() {
              backingIterator.remove();
            }
          };
        }
      };
    }
  }

  private static final class SortedAsMapView<K, V> extends AsMapView<K, V>
      implements SortedMap<K, V> {

    SortedAsMapView(SortedSet<K> set, Function<? super K, V> function) {
      super(set, function);
    }

    @Override
    public Comparator<? super K> comparator() {
      return backingSet().comparator();
    }

    @Override
    public SortedMap<K, V> subMap(K fromKey, K toKey) {
      return asMap(backingSet().subSet(fromKey, toKey), function);
    }

    @Override
    public SortedMap<K, V> headMap(K toKey) {
      return asMap(backingSet().headSet(toKey), function);
    }

    @Override
    public SortedMap<K, V> tailMap(K fromKey) {
      return asMap(backingSet().tailSet(fromKey), function);
    }

    @Override
    public K firstKey() {
      return backingSet().first();
    }

    @Override
    public K lastKey() {
      return backingSet().last();
    }

    @Override
    SortedSet<K> backingSet() {
      return (SortedSet<K>) super.backingSet();
    }
  }

  /**
   * Returns an immutable map for which the {@link Map#values} are the given
   * elements in the given order, and each key is the product of invoking a
   * supplied function on its corresponding value.
   *
   * @param values the values to use when constructing the {@code Map}
   * @param keyFunction the function used to produce the key for each value
   * @return a map mapping the result of evaluating the function {@code
   *         keyFunction} on each value in the input collection to that value
   * @throws IllegalArgumentException if {@code keyFunction} produces the same
   *         key for more than one value in the input collection
   * @throws NullPointerException if any elements of {@code values} is null, or
   *         if {@code keyFunction} produces {@code null} for any value
   */
  public static <K, V> ImmutableMap<K, V> uniqueIndex(
      Iterable<V> values, Function<? super V, K> keyFunction) {
    return uniqueIndex(values.iterator(), keyFunction);
  }

  /**
   * Returns an immutable map for which the {@link Map#values} are the given
   * elements in the given order, and each key is the product of invoking a
   * supplied function on its corresponding value.
   *
   * @param values the values to use when constructing the {@code Map}
   * @param keyFunction the function used to produce the key for each value
   * @return a map mapping the result of evaluating the function {@code
   *         keyFunction} on each value in the input collection to that value
   * @throws IllegalArgumentException if {@code keyFunction} produces the same
   *         key for more than one value in the input collection
   * @throws NullPointerException if any elements of {@code values} is null, or
   *         if {@code keyFunction} produces {@code null} for any value
   * @since 10.0
   */
  public static <K, V> ImmutableMap<K, V> uniqueIndex(
      Iterator<V> values, Function<? super V, K> keyFunction) {
    checkNotNull(keyFunction);
    ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
    while (values.hasNext()) {
      V value = values.next();
      builder.put(keyFunction.apply(value), value);
    }
    return builder.build();
  }

  /**
   * Creates an {@code ImmutableMap<String, String>} from a {@code Properties}
   * instance. Properties normally derive from {@code Map<Object, Object>}, but
   * they typically contain strings, which is awkward. This method lets you get
   * a plain-old-{@code Map} out of a {@code Properties}.
   *
   * @param properties a {@code Properties} object to be converted
   * @return an immutable map containing all the entries in {@code properties}
   * @throws ClassCastException if any key in {@code Properties} is not a {@code
   *         String}
   * @throws NullPointerException if any key or value in {@code Properties} is
   *         null
   */
  @GwtIncompatible("java.util.Properties")
  public static ImmutableMap<String, String> fromProperties(
      Properties properties) {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

    for (Enumeration<?> e = properties.propertyNames(); e.hasMoreElements();) {
      String key = (String) e.nextElement();
      builder.put(key, properties.getProperty(key));
    }

    return builder.build();
  }

  /**
   * Returns an immutable map entry with the specified key and value. The {@link
   * Entry#setValue} operation throws an {@link UnsupportedOperationException}.
   *
   * <p>The returned entry is serializable.
   *
   * @param key the key to be associated with the returned entry
   * @param value the value to be associated with the returned entry
   */
  @GwtCompatible(serializable = true)
  public static <K, V> Entry<K, V> immutableEntry(
      @Nullable K key, @Nullable V value) {
    return new ImmutableEntry<K, V>(key, value);
  }

  /**
   * Returns an unmodifiable view of the specified set of entries. The {@link
   * Entry#setValue} operation throws an {@link UnsupportedOperationException},
   * as do any operations that would modify the returned set.
   *
   * @param entrySet the entries for which to return an unmodifiable view
   * @return an unmodifiable view of the entries
   */
  static <K, V> Set<Entry<K, V>> unmodifiableEntrySet(
      Set<Entry<K, V>> entrySet) {
    return new UnmodifiableEntrySet<K, V>(
        Collections.unmodifiableSet(entrySet));
  }

  /**
   * Returns an unmodifiable view of the specified map entry. The {@link
   * Entry#setValue} operation throws an {@link UnsupportedOperationException}.
   * This also has the side-effect of redefining {@code equals} to comply with
   * the Entry contract, to avoid a possible nefarious implementation of equals.
   *
   * @param entry the entry for which to return an unmodifiable view
   * @return an unmodifiable view of the entry
   */
  static <K, V> Entry<K, V> unmodifiableEntry(final Entry<K, V> entry) {
    checkNotNull(entry);
    return new AbstractMapEntry<K, V>() {
      @Override public K getKey() {
        return entry.getKey();
      }

      @Override public V getValue() {
        return entry.getValue();
      }
    };
  }

  /** @see Multimaps#unmodifiableEntries */
  static class UnmodifiableEntries<K, V>
      extends ForwardingCollection<Entry<K, V>> {
    private final Collection<Entry<K, V>> entries;

    UnmodifiableEntries(Collection<Entry<K, V>> entries) {
      this.entries = entries;
    }

    @Override protected Collection<Entry<K, V>> delegate() {
      return entries;
    }

    @Override public Iterator<Entry<K, V>> iterator() {
      final Iterator<Entry<K, V>> delegate = super.iterator();
      return new ForwardingIterator<Entry<K, V>>() {
        @Override public Entry<K, V> next() {
          return unmodifiableEntry(super.next());
        }

        @Override public void remove() {
          throw new UnsupportedOperationException();
        }

        @Override protected Iterator<Entry<K, V>> delegate() {
          return delegate;
        }
      };
    }

    // See java.util.Collections.UnmodifiableEntrySet for details on attacks.

    @Override public boolean add(Entry<K, V> element) {
      throw new UnsupportedOperationException();
    }

    @Override public boolean addAll(
        Collection<? extends Entry<K, V>> collection) {
      throw new UnsupportedOperationException();
    }

    @Override public void clear() {
      throw new UnsupportedOperationException();
    }

    @Override public boolean remove(Object object) {
      throw new UnsupportedOperationException();
    }

    @Override public boolean removeAll(Collection<?> collection) {
      throw new UnsupportedOperationException();
    }

    @Override public boolean retainAll(Collection<?> collection) {
      throw new UnsupportedOperationException();
    }

    @Override public Object[] toArray() {
      return standardToArray();
    }

    @Override public <T> T[] toArray(T[] array) {
      return standardToArray(array);
    }
  }

  /** @see Maps#unmodifiableEntrySet(Set) */
  static class UnmodifiableEntrySet<K, V>
      extends UnmodifiableEntries<K, V> implements Set<Entry<K, V>> {
    UnmodifiableEntrySet(Set<Entry<K, V>> entries) {
      super(entries);
    }

    // See java.util.Collections.UnmodifiableEntrySet for details on attacks.

    @Override public boolean equals(@Nullable Object object) {
      return Sets.equalsImpl(this, object);
    }

    @Override public int hashCode() {
      return Sets.hashCodeImpl(this);
    }
  }

  /**
   * Returns a synchronized (thread-safe) bimap backed by the specified bimap.
   * In order to guarantee serial access, it is critical that <b>all</b> access
   * to the backing bimap is accomplished through the returned bimap.
   *
   * <p>It is imperative that the user manually synchronize on the returned map
   * when accessing any of its collection views: <pre>   {@code
   *
   *   BiMap<Long, String> map = Maps.synchronizedBiMap(
   *       HashBiMap.<Long, String>create());
   *   ...
   *   Set<Long> set = map.keySet();  // Needn't be in synchronized block
   *   ...
   *   synchronized (map) {  // Synchronizing on map, not set!
   *     Iterator<Long> it = set.iterator(); // Must be in synchronized block
   *     while (it.hasNext()) {
   *       foo(it.next());
   *     }
   *   }}</pre>
   *
   * Failure to follow this advice may result in non-deterministic behavior.
   *
   * <p>The returned bimap will be serializable if the specified bimap is
   * serializable.
   *
   * @param bimap the bimap to be wrapped in a synchronized view
   * @return a sychronized view of the specified bimap
   */
  public static <K, V> BiMap<K, V> synchronizedBiMap(BiMap<K, V> bimap) {
    return Synchronized.biMap(bimap, null);
  }

  /**
   * Returns an unmodifiable view of the specified bimap. This method allows
   * modules to provide users with "read-only" access to internal bimaps. Query
   * operations on the returned bimap "read through" to the specified bimap, and
   * attempts to modify the returned map, whether direct or via its collection
   * views, result in an {@code UnsupportedOperationException}.
   *
   * <p>The returned bimap will be serializable if the specified bimap is
   * serializable.
   *
   * @param bimap the bimap for which an unmodifiable view is to be returned
   * @return an unmodifiable view of the specified bimap
   */
  public static <K, V> BiMap<K, V> unmodifiableBiMap(
      BiMap<? extends K, ? extends V> bimap) {
    return new UnmodifiableBiMap<K, V>(bimap, null);
  }

  /** @see Maps#unmodifiableBiMap(BiMap) */
  private static class UnmodifiableBiMap<K, V>
      extends ForwardingMap<K, V> implements BiMap<K, V>, Serializable {
    final Map<K, V> unmodifiableMap;
    final BiMap<? extends K, ? extends V> delegate;
    BiMap<V, K> inverse;
    transient Set<V> values;

    UnmodifiableBiMap(BiMap<? extends K, ? extends V> delegate,
        @Nullable BiMap<V, K> inverse) {
      unmodifiableMap = Collections.unmodifiableMap(delegate);
      this.delegate = delegate;
      this.inverse = inverse;
    }

    @Override protected Map<K, V> delegate() {
      return unmodifiableMap;
    }

    @Override
    public V forcePut(K key, V value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public BiMap<V, K> inverse() {
      BiMap<V, K> result = inverse;
      return (result == null)
          ? inverse = new UnmodifiableBiMap<V, K>(delegate.inverse(), this)
          : result;
    }

    @Override public Set<V> values() {
      Set<V> result = values;
      return (result == null)
          ? values = Collections.unmodifiableSet(delegate.values())
          : result;
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Returns a view of a map where each value is transformed by a function. All
   * other properties of the map, such as iteration order, are left intact. For
   * example, the code: <pre>   {@code
   *
   *   Map<String, Integer> map = ImmutableMap.of("a", 4, "b", 9);
   *   Function<Integer, Double> sqrt =
   *       new Function<Integer, Double>() {
   *         public Double apply(Integer in) {
   *           return Math.sqrt((int) in);
   *         }
   *       };
   *   Map<String, Double> transformed = Maps.transformValues(map, sqrt);
   *   System.out.println(transformed);}</pre>
   *
   * ... prints {@code {a=2.0, b=3.0}}.
   *
   * <p>Changes in the underlying map are reflected in this view. Conversely,
   * this view supports removal operations, and these are reflected in the
   * underlying map.
   *
   * <p>It's acceptable for the underlying map to contain null keys, and even
   * null values provided that the function is capable of accepting null input.
   * The transformed map might contain null values, if the function sometimes
   * gives a null result.
   *
   * <p>The returned map is not thread-safe or serializable, even if the
   * underlying map is.
   *
   * <p>The function is applied lazily, invoked when needed. This is necessary
   * for the returned map to be a view, but it means that the function will be
   * applied many times for bulk operations like {@link Map#containsValue} and
   * {@code Map.toString()}. For this to perform well, {@code function} should
   * be fast. To avoid lazy evaluation when the returned map doesn't need to be
   * a view, copy the returned map into a new map of your choosing.
   */
  public static <K, V1, V2> Map<K, V2> transformValues(
      Map<K, V1> fromMap, Function<? super V1, V2> function) {
    return transformEntries(fromMap, asEntryTransformer(function));
  }

  /**
   * Returns a view of a sorted map where each value is transformed by a
   * function. All other properties of the map, such as iteration order, are
   * left intact. For example, the code: <pre>   {@code
   *
   *   SortedMap<String, Integer> map = ImmutableSortedMap.of("a", 4, "b", 9);
   *   Function<Integer, Double> sqrt =
   *       new Function<Integer, Double>() {
   *         public Double apply(Integer in) {
   *           return Math.sqrt((int) in);
   *         }
   *       };
   *   SortedMap<String, Double> transformed =
   *        Maps.transformSortedValues(map, sqrt);
   *   System.out.println(transformed);}</pre>
   *
   * ... prints {@code {a=2.0, b=3.0}}.
   *
   * <p>Changes in the underlying map are reflected in this view. Conversely,
   * this view supports removal operations, and these are reflected in the
   * underlying map.
   *
   * <p>It's acceptable for the underlying map to contain null keys, and even
   * null values provided that the function is capable of accepting null input.
   * The transformed map might contain null values, if the function sometimes
   * gives a null result.
   *
   * <p>The returned map is not thread-safe or serializable, even if the
   * underlying map is.
   *
   * <p>The function is applied lazily, invoked when needed. This is necessary
   * for the returned map to be a view, but it means that the function will be
   * applied many times for bulk operations like {@link Map#containsValue} and
   * {@code Map.toString()}. For this to perform well, {@code function} should
   * be fast. To avoid lazy evaluation when the returned map doesn't need to be
   * a view, copy the returned map into a new map of your choosing.
   *
   * @since 11.0
   */
  @Beta
  public static <K, V1, V2> SortedMap<K, V2> transformValues(
      SortedMap<K, V1> fromMap, Function<? super V1, V2> function) {
    return transformEntries(fromMap, asEntryTransformer(function));
  }

  /**
   * Returns a view of a navigable map where each value is transformed by a
   * function. All other properties of the map, such as iteration order, are
   * left intact.  For example, the code: <pre>   {@code
   *
   *   NavigableMap<String, Integer> map = Maps.newTreeMap();
   *   map.put("a", 4);
   *   map.put("b", 9);
   *   Function<Integer, Double> sqrt =
   *       new Function<Integer, Double>() {
   *         public Double apply(Integer in) {
   *           return Math.sqrt((int) in);
   *         }
   *       };
   *   NavigableMap<String, Double> transformed =
   *        Maps.transformNavigableValues(map, sqrt);
   *   System.out.println(transformed);}</pre>
   *
   * ... prints {@code {a=2.0, b=3.0}}.
   *
   * Changes in the underlying map are reflected in this view.
   * Conversely, this view supports removal operations, and these are reflected
   * in the underlying map.
   *
   * <p>It's acceptable for the underlying map to contain null keys, and even
   * null values provided that the function is capable of accepting null input.
   * The transformed map might contain null values, if the function sometimes
   * gives a null result.
   *
   * <p>The returned map is not thread-safe or serializable, even if the
   * underlying map is.
   *
   * <p>The function is applied lazily, invoked when needed. This is necessary
   * for the returned map to be a view, but it means that the function will be
   * applied many times for bulk operations like {@link Map#containsValue} and
   * {@code Map.toString()}. For this to perform well, {@code function} should
   * be fast. To avoid lazy evaluation when the returned map doesn't need to be
   * a view, copy the returned map into a new map of your choosing.
   *
   * @since 13.0
   */
  @Beta
  @GwtIncompatible("NavigableMap")
  public static <K, V1, V2> NavigableMap<K, V2> transformValues(
      NavigableMap<K, V1> fromMap, Function<? super V1, V2> function) {
    return transformEntries(fromMap, asEntryTransformer(function));
  }

  private static <K, V1, V2> EntryTransformer<K, V1, V2>
      asEntryTransformer(final Function<? super V1, V2> function) {
    checkNotNull(function);
    return new EntryTransformer<K, V1, V2>() {
      @Override
      public V2 transformEntry(K key, V1 value) {
        return function.apply(value);
      }
    };
  }

  /**
   * Returns a view of a map whose values are derived from the original map's
   * entries. In contrast to {@link #transformValues}, this method's
   * entry-transformation logic may depend on the key as well as the value.
   *
   * <p>All other properties of the transformed map, such as iteration order,
   * are left intact. For example, the code: <pre>   {@code
   *
   *   Map<String, Boolean> options =
   *       ImmutableMap.of("verbose", true, "sort", false);
   *   EntryTransformer<String, Boolean, String> flagPrefixer =
   *       new EntryTransformer<String, Boolean, String>() {
   *         public String transformEntry(String key, Boolean value) {
   *           return value ? key : "no" + key;
   *         }
   *       };
   *   Map<String, String> transformed =
   *       Maps.transformEntries(options, flagPrefixer);
   *   System.out.println(transformed);}</pre>
   *
   * ... prints {@code {verbose=verbose, sort=nosort}}.
   *
   * <p>Changes in the underlying map are reflected in this view. Conversely,
   * this view supports removal operations, and these are reflected in the
   * underlying map.
   *
   * <p>It's acceptable for the underlying map to contain null keys and null
   * values provided that the transformer is capable of accepting null inputs.
   * The transformed map might contain null values if the transformer sometimes
   * gives a null result.
   *
   * <p>The returned map is not thread-safe or serializable, even if the
   * underlying map is.
   *
   * <p>The transformer is applied lazily, invoked when needed. This is
   * necessary for the returned map to be a view, but it means that the
   * transformer will be applied many times for bulk operations like {@link
   * Map#containsValue} and {@link Object#toString}. For this to perform well,
   * {@code transformer} should be fast. To avoid lazy evaluation when the
   * returned map doesn't need to be a view, copy the returned map into a new
   * map of your choosing.
   *
   * <p><b>Warning:</b> This method assumes that for any instance {@code k} of
   * {@code EntryTransformer} key type {@code K}, {@code k.equals(k2)} implies
   * that {@code k2} is also of type {@code K}. Using an {@code
   * EntryTransformer} key type for which this may not hold, such as {@code
   * ArrayList}, may risk a {@code ClassCastException} when calling methods on
   * the transformed map.
   *
   * @since 7.0
   */
  public static <K, V1, V2> Map<K, V2> transformEntries(
      Map<K, V1> fromMap,
      EntryTransformer<? super K, ? super V1, V2> transformer) {
    if (fromMap instanceof SortedMap) {
      return transformEntries((SortedMap<K, V1>) fromMap, transformer);
    }
    return new TransformedEntriesMap<K, V1, V2>(fromMap, transformer);
  }

  /**
   * Returns a view of a sorted map whose values are derived from the original
   * sorted map's entries. In contrast to {@link #transformValues}, this
   * method's entry-transformation logic may depend on the key as well as the
   * value.
   *
   * <p>All other properties of the transformed map, such as iteration order,
   * are left intact. For example, the code: <pre>   {@code
   *
   *   Map<String, Boolean> options =
   *       ImmutableSortedMap.of("verbose", true, "sort", false);
   *   EntryTransformer<String, Boolean, String> flagPrefixer =
   *       new EntryTransformer<String, Boolean, String>() {
   *         public String transformEntry(String key, Boolean value) {
   *           return value ? key : "yes" + key;
   *         }
   *       };
   *   SortedMap<String, String> transformed =
   *       LabsMaps.transformSortedEntries(options, flagPrefixer);
   *   System.out.println(transformed);}</pre>
   *
   * ... prints {@code {sort=yessort, verbose=verbose}}.
   *
   * <p>Changes in the underlying map are reflected in this view. Conversely,
   * this view supports removal operations, and these are reflected in the
   * underlying map.
   *
   * <p>It's acceptable for the underlying map to contain null keys and null
   * values provided that the transformer is capable of accepting null inputs.
   * The transformed map might contain null values if the transformer sometimes
   * gives a null result.
   *
   * <p>The returned map is not thread-safe or serializable, even if the
   * underlying map is.
   *
   * <p>The transformer is applied lazily, invoked when needed. This is
   * necessary for the returned map to be a view, but it means that the
   * transformer will be applied many times for bulk operations like {@link
   * Map#containsValue} and {@link Object#toString}. For this to perform well,
   * {@code transformer} should be fast. To avoid lazy evaluation when the
   * returned map doesn't need to be a view, copy the returned map into a new
   * map of your choosing.
   *
   * <p><b>Warning:</b> This method assumes that for any instance {@code k} of
   * {@code EntryTransformer} key type {@code K}, {@code k.equals(k2)} implies
   * that {@code k2} is also of type {@code K}. Using an {@code
   * EntryTransformer} key type for which this may not hold, such as {@code
   * ArrayList}, may risk a {@code ClassCastException} when calling methods on
   * the transformed map.
   *
   * @since 11.0
   */
  @Beta
  public static <K, V1, V2> SortedMap<K, V2> transformEntries(
      SortedMap<K, V1> fromMap,
      EntryTransformer<? super K, ? super V1, V2> transformer) {
    return Platform.mapsTransformEntriesSortedMap(fromMap, transformer);
  }

  /**
   * Returns a view of a navigable map whose values are derived from the
   * original navigable map's entries. In contrast to {@link
   * #transformValues}, this method's entry-transformation logic may
   * depend on the key as well as the value.
   *
   * <p>All other properties of the transformed map, such as iteration order,
   * are left intact. For example, the code: <pre>   {@code
   *
   *   NavigableMap<String, Boolean> options = Maps.newTreeMap();
   *   options.put("verbose", false);
   *   options.put("sort", true);
   *   EntryTransformer<String, Boolean, String> flagPrefixer =
   *       new EntryTransformer<String, Boolean, String>() {
   *         public String transformEntry(String key, Boolean value) {
   *           return value ? key : ("yes" + key);
   *         }
   *       };
   *   NavigableMap<String, String> transformed =
   *       LabsMaps.transformNavigableEntries(options, flagPrefixer);
   *   System.out.println(transformed);}</pre>
   *
   * ... prints {@code {sort=yessort, verbose=verbose}}.
   *
   * <p>Changes in the underlying map are reflected in this view.
   * Conversely, this view supports removal operations, and these are reflected
   * in the underlying map.
   *
   * <p>It's acceptable for the underlying map to contain null keys and null
   * values provided that the transformer is capable of accepting null inputs.
   * The transformed map might contain null values if the transformer sometimes
   * gives a null result.
   *
   * <p>The returned map is not thread-safe or serializable, even if the
   * underlying map is.
   *
   * <p>The transformer is applied lazily, invoked when needed. This is
   * necessary for the returned map to be a view, but it means that the
   * transformer will be applied many times for bulk operations like {@link
   * Map#containsValue} and {@link Object#toString}. For this to perform well,
   * {@code transformer} should be fast. To avoid lazy evaluation when the
   * returned map doesn't need to be a view, copy the returned map into a new
   * map of your choosing.
   *
   * <p><b>Warning:</b> This method assumes that for any instance {@code k} of
   * {@code EntryTransformer} key type {@code K}, {@code k.equals(k2)} implies
   * that {@code k2} is also of type {@code K}. Using an {@code
   * EntryTransformer} key type for which this may not hold, such as {@code
   * ArrayList}, may risk a {@code ClassCastException} when calling methods on
   * the transformed map.
   *
   * @since 13.0
   */
  @Beta
  @GwtIncompatible("NavigableMap")
  public static <K, V1, V2> NavigableMap<K, V2> transformEntries(
      final NavigableMap<K, V1> fromMap,
      EntryTransformer<? super K, ? super V1, V2> transformer) {
    return new TransformedEntriesNavigableMap<K, V1, V2>(fromMap, transformer);
  }

  static <K, V1, V2> SortedMap<K, V2> transformEntriesIgnoreNavigable(
      SortedMap<K, V1> fromMap,
      EntryTransformer<? super K, ? super V1, V2> transformer) {
    return new TransformedEntriesSortedMap<K, V1, V2>(fromMap, transformer);
  }

  /**
   * A transformation of the value of a key-value pair, using both key and value
   * as inputs. To apply the transformation to a map, use
   * {@link Maps#transformEntries(Map, EntryTransformer)}.
   *
   * @param <K> the key type of the input and output entries
   * @param <V1> the value type of the input entry
   * @param <V2> the value type of the output entry
   * @since 7.0
   */
  public interface EntryTransformer<K, V1, V2> {
    /**
     * Determines an output value based on a key-value pair. This method is
     * <i>generally expected</i>, but not absolutely required, to have the
     * following properties:
     *
     * <ul>
     * <li>Its execution does not cause any observable side effects.
     * <li>The computation is <i>consistent with equals</i>; that is,
     *     {@link Objects#equal Objects.equal}{@code (k1, k2) &&}
     *     {@link Objects#equal}{@code (v1, v2)} implies that {@code
     *     Objects.equal(transformer.transform(k1, v1),
     *     transformer.transform(k2, v2))}.
     * </ul>
     *
     * @throws NullPointerException if the key or value is null and this
     *     transformer does not accept null arguments
     */
    V2 transformEntry(@Nullable K key, @Nullable V1 value);
  }

  static class TransformedEntriesMap<K, V1, V2>
      extends AbstractMap<K, V2> {
    final Map<K, V1> fromMap;
    final EntryTransformer<? super K, ? super V1, V2> transformer;

    TransformedEntriesMap(
        Map<K, V1> fromMap,
        EntryTransformer<? super K, ? super V1, V2> transformer) {
      this.fromMap = checkNotNull(fromMap);
      this.transformer = checkNotNull(transformer);
    }

    @Override public int size() {
      return fromMap.size();
    }

    @Override public boolean containsKey(Object key) {
      return fromMap.containsKey(key);
    }

    // safe as long as the user followed the <b>Warning</b> in the javadoc
    @SuppressWarnings("unchecked")
    @Override public V2 get(Object key) {
      V1 value = fromMap.get(key);
      return (value != null || fromMap.containsKey(key))
          ? transformer.transformEntry((K) key, value)
          : null;
    }

    // safe as long as the user followed the <b>Warning</b> in the javadoc
    @SuppressWarnings("unchecked")
    @Override public V2 remove(Object key) {
      return fromMap.containsKey(key)
          ? transformer.transformEntry((K) key, fromMap.remove(key))
          : null;
    }

    @Override public void clear() {
      fromMap.clear();
    }

    @Override public Set<K> keySet() {
      return fromMap.keySet();
    }

    Set<Entry<K, V2>> entrySet;

    @Override public Set<Entry<K, V2>> entrySet() {
      Set<Entry<K, V2>> result = entrySet;
      if (result == null) {
        entrySet = result = new EntrySet<K, V2>() {
          @Override Map<K, V2> map() {
            return TransformedEntriesMap.this;
          }

          @Override public Iterator<Entry<K, V2>> iterator() {
            return new TransformedIterator<Entry<K, V1>, Entry<K, V2>>(
                fromMap.entrySet().iterator()) {
              @Override
              Entry<K, V2> transform(final Entry<K, V1> entry) {
                return new AbstractMapEntry<K, V2>() {
                  @Override
                  public K getKey() {
                    return entry.getKey();
                  }

                  @Override
                  public V2 getValue() {
                    return transformer.transformEntry(entry.getKey(), entry.getValue());
                  }
                };
              }
            };
          }
        };
      }
      return result;
    }

    Collection<V2> values;

    @Override public Collection<V2> values() {
      Collection<V2> result = values;
      if (result == null) {
        return values = new Values<K, V2>() {
          @Override Map<K, V2> map() {
            return TransformedEntriesMap.this;
          }
        };
      }
      return result;
    }
  }

  static class TransformedEntriesSortedMap<K, V1, V2>
      extends TransformedEntriesMap<K, V1, V2> implements SortedMap<K, V2> {

    protected SortedMap<K, V1> fromMap() {
      return (SortedMap<K, V1>) fromMap;
    }

    TransformedEntriesSortedMap(SortedMap<K, V1> fromMap,
        EntryTransformer<? super K, ? super V1, V2> transformer) {
      super(fromMap, transformer);
    }

    @Override public Comparator<? super K> comparator() {
      return fromMap().comparator();
    }

    @Override public K firstKey() {
      return fromMap().firstKey();
    }

    @Override public SortedMap<K, V2> headMap(K toKey) {
      return transformEntries(fromMap().headMap(toKey), transformer);
    }

    @Override public K lastKey() {
      return fromMap().lastKey();
    }

    @Override public SortedMap<K, V2> subMap(K fromKey, K toKey) {
      return transformEntries(
          fromMap().subMap(fromKey, toKey), transformer);
    }

    @Override public SortedMap<K, V2> tailMap(K fromKey) {
      return transformEntries(fromMap().tailMap(fromKey), transformer);
    }
  }

  @GwtIncompatible("NavigableMap")
  private static class TransformedEntriesNavigableMap<K, V1, V2>
      extends TransformedEntriesSortedMap<K, V1, V2>
      implements NavigableMap<K, V2> {

    TransformedEntriesNavigableMap(NavigableMap<K, V1> fromMap,
        EntryTransformer<? super K, ? super V1, V2> transformer) {
      super(fromMap, transformer);
    }

    @Override public Entry<K, V2> ceilingEntry(K key) {
      return transformEntry(fromMap().ceilingEntry(key));
    }

    @Override public K ceilingKey(K key) {
      return fromMap().ceilingKey(key);
    }

    @Override public NavigableSet<K> descendingKeySet() {
      return fromMap().descendingKeySet();
    }

    @Override public NavigableMap<K, V2> descendingMap() {
      return transformEntries(fromMap().descendingMap(), transformer);
    }

    @Override public Entry<K, V2> firstEntry() {
      return transformEntry(fromMap().firstEntry());
    }
    @Override public Entry<K, V2> floorEntry(K key) {
      return transformEntry(fromMap().floorEntry(key));
    }

    @Override public K floorKey(K key) {
      return fromMap().floorKey(key);
    }

    @Override public NavigableMap<K, V2> headMap(K toKey) {
      return headMap(toKey, false);
    }

    @Override public NavigableMap<K, V2> headMap(K toKey, boolean inclusive) {
      return transformEntries(
          fromMap().headMap(toKey, inclusive), transformer);
    }

    @Override public Entry<K, V2> higherEntry(K key) {
      return transformEntry(fromMap().higherEntry(key));
    }

    @Override public K higherKey(K key) {
      return fromMap().higherKey(key);
    }

    @Override public Entry<K, V2> lastEntry() {
      return transformEntry(fromMap().lastEntry());
    }

    @Override public Entry<K, V2> lowerEntry(K key) {
      return transformEntry(fromMap().lowerEntry(key));
    }

    @Override public K lowerKey(K key) {
      return fromMap().lowerKey(key);
    }

    @Override public NavigableSet<K> navigableKeySet() {
      return fromMap().navigableKeySet();
    }

    @Override public Entry<K, V2> pollFirstEntry() {
      return transformEntry(fromMap().pollFirstEntry());
    }

    @Override public Entry<K, V2> pollLastEntry() {
      return transformEntry(fromMap().pollLastEntry());
    }

    @Override public NavigableMap<K, V2> subMap(
        K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
      return transformEntries(
          fromMap().subMap(fromKey, fromInclusive, toKey, toInclusive),
          transformer);
    }

    @Override public NavigableMap<K, V2> subMap(K fromKey, K toKey) {
      return subMap(fromKey, true, toKey, false);
    }

    @Override public NavigableMap<K, V2> tailMap(K fromKey) {
      return tailMap(fromKey, true);
    }

    @Override public NavigableMap<K, V2> tailMap(K fromKey, boolean inclusive) {
      return transformEntries(
          fromMap().tailMap(fromKey, inclusive), transformer);
    }

    private Entry<K, V2> transformEntry(Entry<K, V1> entry) {
      if (entry == null) {
        return null;
      }
      K key = entry.getKey();
      V2 v2 = transformer.transformEntry(key, entry.getValue());
      return Maps.immutableEntry(key, v2);
    }

    @Override protected NavigableMap<K, V1> fromMap() {
      return (NavigableMap<K, V1>) super.fromMap();
    }
  }

  /**
   * Returns a map containing the mappings in {@code unfiltered} whose keys
   * satisfy a predicate. The returned map is a live view of {@code unfiltered};
   * changes to one affect the other.
   *
   * <p>The resulting map's {@code keySet()}, {@code entrySet()}, and {@code
   * values()} views have iterators that don't support {@code remove()}, but all
   * other methods are supported by the map and its views. When given a key that
   * doesn't satisfy the predicate, the map's {@code put()} and {@code putAll()}
   * methods throw an {@link IllegalArgumentException}.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called
   * on the filtered map or its views, only mappings whose keys satisfy the
   * filter will be removed from the underlying map.
   *
   * <p>The returned map isn't threadsafe or serializable, even if {@code
   * unfiltered} is.
   *
   * <p>Many of the filtered map's methods, such as {@code size()},
   * iterate across every key/value mapping in the underlying map and determine
   * which satisfy the filter. When a live view is <i>not</i> needed, it may be
   * faster to copy the filtered map and use the copy.
   *
   * <p><b>Warning:</b> {@code keyPredicate} must be <i>consistent with
   * equals</i>, as documented at {@link Predicate#apply}. Do not provide a
   * predicate such as {@code Predicates.instanceOf(ArrayList.class)}, which is
   * inconsistent with equals.
   */
  public static <K, V> Map<K, V> filterKeys(
      Map<K, V> unfiltered, final Predicate<? super K> keyPredicate) {
    if (unfiltered instanceof SortedMap) {
      return filterKeys((SortedMap<K, V>) unfiltered, keyPredicate);
    }
    checkNotNull(keyPredicate);
    Predicate<Entry<K, V>> entryPredicate =
        new Predicate<Entry<K, V>>() {
          @Override
          public boolean apply(Entry<K, V> input) {
            return keyPredicate.apply(input.getKey());
          }
        };
    return (unfiltered instanceof AbstractFilteredMap)
        ? filterFiltered((AbstractFilteredMap<K, V>) unfiltered, entryPredicate)
        : new FilteredKeyMap<K, V>(
            checkNotNull(unfiltered), keyPredicate, entryPredicate);
  }

  /**
   * Returns a sorted map containing the mappings in {@code unfiltered} whose
   * keys satisfy a predicate. The returned map is a live view of {@code
   * unfiltered}; changes to one affect the other.
   *
   * <p>The resulting map's {@code keySet()}, {@code entrySet()}, and {@code
   * values()} views have iterators that don't support {@code remove()}, but all
   * other methods are supported by the map and its views. When given a key that
   * doesn't satisfy the predicate, the map's {@code put()} and {@code putAll()}
   * methods throw an {@link IllegalArgumentException}.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called
   * on the filtered map or its views, only mappings whose keys satisfy the
   * filter will be removed from the underlying map.
   *
   * <p>The returned map isn't threadsafe or serializable, even if {@code
   * unfiltered} is.
   *
   * <p>Many of the filtered map's methods, such as {@code size()},
   * iterate across every key/value mapping in the underlying map and determine
   * which satisfy the filter. When a live view is <i>not</i> needed, it may be
   * faster to copy the filtered map and use the copy.
   *
   * <p><b>Warning:</b> {@code keyPredicate} must be <i>consistent with
   * equals</i>, as documented at {@link Predicate#apply}. Do not provide a
   * predicate such as {@code Predicates.instanceOf(ArrayList.class)}, which is
   * inconsistent with equals.
   *
   * @since 11.0
   */
  public static <K, V> SortedMap<K, V> filterKeys(
      SortedMap<K, V> unfiltered, final Predicate<? super K> keyPredicate) {
    // TODO: Return a subclass of Maps.FilteredKeyMap for slightly better
    // performance.
    checkNotNull(keyPredicate);
    Predicate<Entry<K, V>> entryPredicate = new Predicate<Entry<K, V>>() {
      @Override
      public boolean apply(Entry<K, V> input) {
        return keyPredicate.apply(input.getKey());
      }
    };
    return filterEntries(unfiltered, entryPredicate);
  }

  /**
   * Returns a map containing the mappings in {@code unfiltered} whose values
   * satisfy a predicate. The returned map is a live view of {@code unfiltered};
   * changes to one affect the other.
   *
   * <p>The resulting map's {@code keySet()}, {@code entrySet()}, and {@code
   * values()} views have iterators that don't support {@code remove()}, but all
   * other methods are supported by the map and its views. When given a value
   * that doesn't satisfy the predicate, the map's {@code put()}, {@code
   * putAll()}, and {@link Entry#setValue} methods throw an {@link
   * IllegalArgumentException}.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called
   * on the filtered map or its views, only mappings whose values satisfy the
   * filter will be removed from the underlying map.
   *
   * <p>The returned map isn't threadsafe or serializable, even if {@code
   * unfiltered} is.
   *
   * <p>Many of the filtered map's methods, such as {@code size()},
   * iterate across every key/value mapping in the underlying map and determine
   * which satisfy the filter. When a live view is <i>not</i> needed, it may be
   * faster to copy the filtered map and use the copy.
   *
   * <p><b>Warning:</b> {@code valuePredicate} must be <i>consistent with
   * equals</i>, as documented at {@link Predicate#apply}. Do not provide a
   * predicate such as {@code Predicates.instanceOf(ArrayList.class)}, which is
   * inconsistent with equals.
   */
  public static <K, V> Map<K, V> filterValues(
      Map<K, V> unfiltered, final Predicate<? super V> valuePredicate) {
    if (unfiltered instanceof SortedMap) {
      return filterValues((SortedMap<K, V>) unfiltered, valuePredicate);
    }
    checkNotNull(valuePredicate);
    Predicate<Entry<K, V>> entryPredicate =
        new Predicate<Entry<K, V>>() {
          @Override
          public boolean apply(Entry<K, V> input) {
            return valuePredicate.apply(input.getValue());
          }
        };
    return filterEntries(unfiltered, entryPredicate);
  }

  /**
   * Returns a sorted map containing the mappings in {@code unfiltered} whose
   * values satisfy a predicate. The returned map is a live view of {@code
   * unfiltered}; changes to one affect the other.
   *
   * <p>The resulting map's {@code keySet()}, {@code entrySet()}, and {@code
   * values()} views have iterators that don't support {@code remove()}, but all
   * other methods are supported by the map and its views. When given a value
   * that doesn't satisfy the predicate, the map's {@code put()}, {@code
   * putAll()}, and {@link Entry#setValue} methods throw an {@link
   * IllegalArgumentException}.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called
   * on the filtered map or its views, only mappings whose values satisfy the
   * filter will be removed from the underlying map.
   *
   * <p>The returned map isn't threadsafe or serializable, even if {@code
   * unfiltered} is.
   *
   * <p>Many of the filtered map's methods, such as {@code size()},
   * iterate across every key/value mapping in the underlying map and determine
   * which satisfy the filter. When a live view is <i>not</i> needed, it may be
   * faster to copy the filtered map and use the copy.
   *
   * <p><b>Warning:</b> {@code valuePredicate} must be <i>consistent with
   * equals</i>, as documented at {@link Predicate#apply}. Do not provide a
   * predicate such as {@code Predicates.instanceOf(ArrayList.class)}, which is
   * inconsistent with equals.
   *
   * @since 11.0
   */
  public static <K, V> SortedMap<K, V> filterValues(
      SortedMap<K, V> unfiltered, final Predicate<? super V> valuePredicate) {
    checkNotNull(valuePredicate);
    Predicate<Entry<K, V>> entryPredicate =
        new Predicate<Entry<K, V>>() {
          @Override
          public boolean apply(Entry<K, V> input) {
            return valuePredicate.apply(input.getValue());
          }
        };
    return filterEntries(unfiltered, entryPredicate);
  }

  /**
   * Returns a map containing the mappings in {@code unfiltered} that satisfy a
   * predicate. The returned map is a live view of {@code unfiltered}; changes
   * to one affect the other.
   *
   * <p>The resulting map's {@code keySet()}, {@code entrySet()}, and {@code
   * values()} views have iterators that don't support {@code remove()}, but all
   * other methods are supported by the map and its views. When given a
   * key/value pair that doesn't satisfy the predicate, the map's {@code put()}
   * and {@code putAll()} methods throw an {@link IllegalArgumentException}.
   * Similarly, the map's entries have a {@link Entry#setValue} method that
   * throws an {@link IllegalArgumentException} when the existing key and the
   * provided value don't satisfy the predicate.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called
   * on the filtered map or its views, only mappings that satisfy the filter
   * will be removed from the underlying map.
   *
   * <p>The returned map isn't threadsafe or serializable, even if {@code
   * unfiltered} is.
   *
   * <p>Many of the filtered map's methods, such as {@code size()},
   * iterate across every key/value mapping in the underlying map and determine
   * which satisfy the filter. When a live view is <i>not</i> needed, it may be
   * faster to copy the filtered map and use the copy.
   *
   * <p><b>Warning:</b> {@code entryPredicate} must be <i>consistent with
   * equals</i>, as documented at {@link Predicate#apply}.
   */
  public static <K, V> Map<K, V> filterEntries(
      Map<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate) {
    if (unfiltered instanceof SortedMap) {
      return filterEntries((SortedMap<K, V>) unfiltered, entryPredicate);
    }
    checkNotNull(entryPredicate);
    return (unfiltered instanceof AbstractFilteredMap)
        ? filterFiltered((AbstractFilteredMap<K, V>) unfiltered, entryPredicate)
        : new FilteredEntryMap<K, V>(checkNotNull(unfiltered), entryPredicate);
  }

  /**
   * Returns a sorted map containing the mappings in {@code unfiltered} that
   * satisfy a predicate. The returned map is a live view of {@code unfiltered};
   * changes to one affect the other.
   *
   * <p>The resulting map's {@code keySet()}, {@code entrySet()}, and {@code
   * values()} views have iterators that don't support {@code remove()}, but all
   * other methods are supported by the map and its views. When given a
   * key/value pair that doesn't satisfy the predicate, the map's {@code put()}
   * and {@code putAll()} methods throw an {@link IllegalArgumentException}.
   * Similarly, the map's entries have a {@link Entry#setValue} method that
   * throws an {@link IllegalArgumentException} when the existing key and the
   * provided value don't satisfy the predicate.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called
   * on the filtered map or its views, only mappings that satisfy the filter
   * will be removed from the underlying map.
   *
   * <p>The returned map isn't threadsafe or serializable, even if {@code
   * unfiltered} is.
   *
   * <p>Many of the filtered map's methods, such as {@code size()},
   * iterate across every key/value mapping in the underlying map and determine
   * which satisfy the filter. When a live view is <i>not</i> needed, it may be
   * faster to copy the filtered map and use the copy.
   *
   * <p><b>Warning:</b> {@code entryPredicate} must be <i>consistent with
   * equals</i>, as documented at {@link Predicate#apply}.
   *
   * @since 11.0
   */
  public static <K, V> SortedMap<K, V> filterEntries(
      SortedMap<K, V> unfiltered,
      Predicate<? super Entry<K, V>> entryPredicate) {
    checkNotNull(entryPredicate);
    return (unfiltered instanceof FilteredEntrySortedMap)
        ? filterFiltered((FilteredEntrySortedMap<K, V>) unfiltered, entryPredicate)
        : new FilteredEntrySortedMap<K, V>(checkNotNull(unfiltered), entryPredicate);
  }

  /**
   * Support {@code clear()}, {@code removeAll()}, and {@code retainAll()} when
   * filtering a filtered map.
   */
  private static <K, V> Map<K, V> filterFiltered(AbstractFilteredMap<K, V> map,
      Predicate<? super Entry<K, V>> entryPredicate) {
    Predicate<Entry<K, V>> predicate =
        Predicates.and(map.predicate, entryPredicate);
    return new FilteredEntryMap<K, V>(map.unfiltered, predicate);
  }

  private abstract static class AbstractFilteredMap<K, V>
      extends AbstractMap<K, V> {
    final Map<K, V> unfiltered;
    final Predicate<? super Entry<K, V>> predicate;

    AbstractFilteredMap(
        Map<K, V> unfiltered, Predicate<? super Entry<K, V>> predicate) {
      this.unfiltered = unfiltered;
      this.predicate = predicate;
    }

    boolean apply(Object key, V value) {
      // This method is called only when the key is in the map, implying that
      // key is a K.
      @SuppressWarnings("unchecked")
      K k = (K) key;
      return predicate.apply(Maps.immutableEntry(k, value));
    }

    @Override public V put(K key, V value) {
      checkArgument(apply(key, value));
      return unfiltered.put(key, value);
    }

    @Override public void putAll(Map<? extends K, ? extends V> map) {
      for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
        checkArgument(apply(entry.getKey(), entry.getValue()));
      }
      unfiltered.putAll(map);
    }

    @Override public boolean containsKey(Object key) {
      return unfiltered.containsKey(key) && apply(key, unfiltered.get(key));
    }

    @Override public V get(Object key) {
      V value = unfiltered.get(key);
      return ((value != null) && apply(key, value)) ? value : null;
    }

    @Override public boolean isEmpty() {
      return entrySet().isEmpty();
    }

    @Override public V remove(Object key) {
      return containsKey(key) ? unfiltered.remove(key) : null;
    }

    Collection<V> values;

    @Override public Collection<V> values() {
      Collection<V> result = values;
      return (result == null) ? values = new Values() : result;
    }

    class Values extends AbstractCollection<V> {
      @Override public Iterator<V> iterator() {
        final Iterator<Entry<K, V>> entryIterator = entrySet().iterator();
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

      @Override public int size() {
        return entrySet().size();
      }

      @Override public void clear() {
        entrySet().clear();
      }

      @Override public boolean isEmpty() {
        return entrySet().isEmpty();
      }

      @Override public boolean remove(Object o) {
        Iterator<Entry<K, V>> iterator = unfiltered.entrySet().iterator();
        while (iterator.hasNext()) {
          Entry<K, V> entry = iterator.next();
          if (Objects.equal(o, entry.getValue()) && predicate.apply(entry)) {
            iterator.remove();
            return true;
          }
        }
        return false;
      }

      @Override public boolean removeAll(Collection<?> collection) {
        checkNotNull(collection);
        boolean changed = false;
        Iterator<Entry<K, V>> iterator = unfiltered.entrySet().iterator();
        while (iterator.hasNext()) {
          Entry<K, V> entry = iterator.next();
          if (collection.contains(entry.getValue()) && predicate.apply(entry)) {
            iterator.remove();
            changed = true;
          }
        }
        return changed;
      }

      @Override public boolean retainAll(Collection<?> collection) {
        checkNotNull(collection);
        boolean changed = false;
        Iterator<Entry<K, V>> iterator = unfiltered.entrySet().iterator();
        while (iterator.hasNext()) {
          Entry<K, V> entry = iterator.next();
          if (!collection.contains(entry.getValue())
              && predicate.apply(entry)) {
            iterator.remove();
            changed = true;
          }
        }
        return changed;
      }

      @Override public Object[] toArray() {
        // creating an ArrayList so filtering happens once
        return Lists.newArrayList(iterator()).toArray();
      }

      @Override public <T> T[] toArray(T[] array) {
        return Lists.newArrayList(iterator()).toArray(array);
      }
    }
  }
  /**
   * Support {@code clear()}, {@code removeAll()}, and {@code retainAll()} when
   * filtering a filtered sorted map.
   */
  private static <K, V> SortedMap<K, V> filterFiltered(
      FilteredEntrySortedMap<K, V> map,
      Predicate<? super Entry<K, V>> entryPredicate) {
    Predicate<Entry<K, V>> predicate
        = Predicates.and(map.predicate, entryPredicate);
    return new FilteredEntrySortedMap<K, V>(map.sortedMap(), predicate);
  }

  private static class FilteredEntrySortedMap<K, V>
      extends FilteredEntryMap<K, V> implements SortedMap<K, V> {

    FilteredEntrySortedMap(SortedMap<K, V> unfiltered,
        Predicate<? super Entry<K, V>> entryPredicate) {
      super(unfiltered, entryPredicate);
    }

    SortedMap<K, V> sortedMap() {
      return (SortedMap<K, V>) unfiltered;
    }

    @Override public Comparator<? super K> comparator() {
      return sortedMap().comparator();
    }

    @Override public K firstKey() {
      // correctly throws NoSuchElementException when filtered map is empty.
      return keySet().iterator().next();
    }

    @Override public K lastKey() {
      SortedMap<K, V> headMap = sortedMap();
      while (true) {
        // correctly throws NoSuchElementException when filtered map is empty.
        K key = headMap.lastKey();
        if (apply(key, unfiltered.get(key))) {
          return key;
        }
        headMap = sortedMap().headMap(key);
      }
    }

    @Override public SortedMap<K, V> headMap(K toKey) {
      return new FilteredEntrySortedMap<K, V>(sortedMap().headMap(toKey), predicate);
    }

    @Override public SortedMap<K, V> subMap(K fromKey, K toKey) {
      return new FilteredEntrySortedMap<K, V>(
          sortedMap().subMap(fromKey, toKey), predicate);
    }

    @Override public SortedMap<K, V> tailMap(K fromKey) {
      return new FilteredEntrySortedMap<K, V>(
          sortedMap().tailMap(fromKey), predicate);
    }
  }

  private static class FilteredKeyMap<K, V> extends AbstractFilteredMap<K, V> {
    Predicate<? super K> keyPredicate;

    FilteredKeyMap(Map<K, V> unfiltered, Predicate<? super K> keyPredicate,
        Predicate<Entry<K, V>> entryPredicate) {
      super(unfiltered, entryPredicate);
      this.keyPredicate = keyPredicate;
    }

    Set<Entry<K, V>> entrySet;

    @Override public Set<Entry<K, V>> entrySet() {
      Set<Entry<K, V>> result = entrySet;
      return (result == null)
          ? entrySet = Sets.filter(unfiltered.entrySet(), predicate)
          : result;
    }

    Set<K> keySet;

    @Override public Set<K> keySet() {
      Set<K> result = keySet;
      return (result == null)
          ? keySet = Sets.filter(unfiltered.keySet(), keyPredicate)
          : result;
    }

    // The cast is called only when the key is in the unfiltered map, implying
    // that key is a K.
    @Override
    @SuppressWarnings("unchecked")
    public boolean containsKey(Object key) {
      return unfiltered.containsKey(key) && keyPredicate.apply((K) key);
    }
  }

  static class FilteredEntryMap<K, V> extends AbstractFilteredMap<K, V> {
    /**
     * Entries in this set satisfy the predicate, but they don't validate the
     * input to {@code Entry.setValue()}.
     */
    final Set<Entry<K, V>> filteredEntrySet;

    FilteredEntryMap(
        Map<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate) {
      super(unfiltered, entryPredicate);
      filteredEntrySet = Sets.filter(unfiltered.entrySet(), predicate);
    }

    Set<Entry<K, V>> entrySet;

    @Override public Set<Entry<K, V>> entrySet() {
      Set<Entry<K, V>> result = entrySet;
      return (result == null) ? entrySet = new EntrySet() : result;
    }

    private class EntrySet extends ForwardingSet<Entry<K, V>> {
      @Override protected Set<Entry<K, V>> delegate() {
        return filteredEntrySet;
      }

      @Override public Iterator<Entry<K, V>> iterator() {
        final Iterator<Entry<K, V>> iterator = filteredEntrySet.iterator();
        return new UnmodifiableIterator<Entry<K, V>>() {
          @Override
          public boolean hasNext() {
            return iterator.hasNext();
          }

          @Override
          public Entry<K, V> next() {
            final Entry<K, V> entry = iterator.next();
            return new ForwardingMapEntry<K, V>() {
              @Override protected Entry<K, V> delegate() {
                return entry;
              }

              @Override public V setValue(V value) {
                checkArgument(apply(entry.getKey(), value));
                return super.setValue(value);
              }
            };
          }
        };
      }
    }

    Set<K> keySet;

    @Override public Set<K> keySet() {
      Set<K> result = keySet;
      return (result == null) ? keySet = new KeySet() : result;
    }

    private class KeySet extends Sets.ImprovedAbstractSet<K> {
      @Override public Iterator<K> iterator() {
        final Iterator<Entry<K, V>> iterator = filteredEntrySet.iterator();
        return new UnmodifiableIterator<K>() {
          @Override
          public boolean hasNext() {
            return iterator.hasNext();
          }

          @Override
          public K next() {
            return iterator.next().getKey();
          }
        };
      }

      @Override public int size() {
        return filteredEntrySet.size();
      }

      @Override public void clear() {
        filteredEntrySet.clear();
      }

      @Override public boolean contains(Object o) {
        return containsKey(o);
      }

      @Override public boolean remove(Object o) {
        if (containsKey(o)) {
          unfiltered.remove(o);
          return true;
        }
        return false;
      }

      @Override public boolean retainAll(Collection<?> collection) {
        checkNotNull(collection); // for GWT
        boolean changed = false;
        Iterator<Entry<K, V>> iterator = unfiltered.entrySet().iterator();
        while (iterator.hasNext()) {
          Entry<K, V> entry = iterator.next();
          if (predicate.apply(entry) && !collection.contains(entry.getKey())) {
            iterator.remove();
            changed = true;
          }
        }
        return changed;
      }

      @Override public Object[] toArray() {
        // creating an ArrayList so filtering happens once
        return Lists.newArrayList(iterator()).toArray();
      }

      @Override public <T> T[] toArray(T[] array) {
        return Lists.newArrayList(iterator()).toArray(array);
      }
    }
  }

  /**
   * Returns an unmodifiable view of the specified navigable map. Query operations on the returned
   * map read through to the specified map, and attempts to modify the returned map, whether direct
   * or via its views, result in an {@code UnsupportedOperationException}.
   *
   * <p>The returned navigable map will be serializable if the specified navigable map is
   * serializable.
   *
   * @param map the navigable map for which an unmodifiable view is to be returned
   * @return an unmodifiable view of the specified navigable map
   * @since 12.0
   */
  @GwtIncompatible("NavigableMap")
  public static <K, V> NavigableMap<K, V> unmodifiableNavigableMap(NavigableMap<K, V> map) {
    checkNotNull(map);
    if (map instanceof UnmodifiableNavigableMap) {
      return map;
    } else {
      return new UnmodifiableNavigableMap<K, V>(map);
    }
  }

  @Nullable private static <K, V> Entry<K, V> unmodifiableOrNull(@Nullable Entry<K, V> entry) {
    return (entry == null) ? null : Maps.unmodifiableEntry(entry);
  }

  @GwtIncompatible("NavigableMap")
  static class UnmodifiableNavigableMap<K, V>
      extends ForwardingSortedMap<K, V> implements NavigableMap<K, V>, Serializable {
    private final NavigableMap<K, V> delegate;

    UnmodifiableNavigableMap(NavigableMap<K, V> delegate) {
      this.delegate = delegate;
    }

    @Override
    protected SortedMap<K, V> delegate() {
      return Collections.unmodifiableSortedMap(delegate);
    }

    @Override
    public Entry<K, V> lowerEntry(K key) {
      return unmodifiableOrNull(delegate.lowerEntry(key));
    }

    @Override
    public K lowerKey(K key) {
      return delegate.lowerKey(key);
    }

    @Override
    public Entry<K, V> floorEntry(K key) {
      return unmodifiableOrNull(delegate.floorEntry(key));
    }

    @Override
    public K floorKey(K key) {
      return delegate.floorKey(key);
    }

    @Override
    public Entry<K, V> ceilingEntry(K key) {
      return unmodifiableOrNull(delegate.ceilingEntry(key));
    }

    @Override
    public K ceilingKey(K key) {
      return delegate.ceilingKey(key);
    }

    @Override
    public Entry<K, V> higherEntry(K key) {
      return unmodifiableOrNull(delegate.higherEntry(key));
    }

    @Override
    public K higherKey(K key) {
      return delegate.higherKey(key);
    }

    @Override
    public Entry<K, V> firstEntry() {
      return unmodifiableOrNull(delegate.firstEntry());
    }

    @Override
    public Entry<K, V> lastEntry() {
      return unmodifiableOrNull(delegate.lastEntry());
    }

    @Override
    public final Entry<K, V> pollFirstEntry() {
      throw new UnsupportedOperationException();
    }

    @Override
    public final Entry<K, V> pollLastEntry() {
      throw new UnsupportedOperationException();
    }

    private transient UnmodifiableNavigableMap<K, V> descendingMap;

    @Override
    public NavigableMap<K, V> descendingMap() {
      UnmodifiableNavigableMap<K, V> result = descendingMap;
      if (result == null) {
        descendingMap = result = new UnmodifiableNavigableMap<K, V>(delegate.descendingMap());
        result.descendingMap = this;
      }
      return result;
    }

    @Override
    public Set<K> keySet() {
      return navigableKeySet();
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
      return Sets.unmodifiableNavigableSet(delegate.navigableKeySet());
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
      return Sets.unmodifiableNavigableSet(delegate.descendingKeySet());
    }

    @Override
    public SortedMap<K, V> subMap(K fromKey, K toKey) {
      return subMap(fromKey, true, toKey, false);
    }

    @Override
    public SortedMap<K, V> headMap(K toKey) {
      return headMap(toKey, false);
    }

    @Override
    public SortedMap<K, V> tailMap(K fromKey) {
      return tailMap(fromKey, true);
    }

    @Override
    public
        NavigableMap<K, V>
        subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
      return Maps.unmodifiableNavigableMap(delegate.subMap(
          fromKey,
          fromInclusive,
          toKey,
          toInclusive));
    }

    @Override
    public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
      return Maps.unmodifiableNavigableMap(delegate.headMap(toKey, inclusive));
    }

    @Override
    public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
      return Maps.unmodifiableNavigableMap(delegate.tailMap(fromKey, inclusive));
    }
  }

  /**
   * Returns a synchronized (thread-safe) navigable map backed by the specified
   * navigable map.  In order to guarantee serial access, it is critical that
   * <b>all</b> access to the backing navigable map is accomplished
   * through the returned navigable map (or its views).
   *
   * <p>It is imperative that the user manually synchronize on the returned
   * navigable map when iterating over any of its collection views, or the
   * collections views of any of its {@code descendingMap}, {@code subMap},
   * {@code headMap} or {@code tailMap} views. <pre>   {@code
   *
   *   NavigableMap<K, V> map = synchronizedNavigableMap(new TreeMap<K, V>());
   *
   *   // Needn't be in synchronized block
   *   NavigableSet<K> set = map.navigableKeySet();
   *
   *   synchronized (map) { // Synchronizing on map, not set!
   *     Iterator<K> it = set.iterator(); // Must be in synchronized block
   *     while (it.hasNext()){
   *       foo(it.next());
   *     }
   *   }}</pre>
   *
   * or: <pre>   {@code
   *
   *   NavigableMap<K, V> map = synchronizedNavigableMap(new TreeMap<K, V>());
   *   NavigableMap<K, V> map2 = map.subMap(foo, false, bar, true);
   *
   *   // Needn't be in synchronized block
   *   NavigableSet<K> set2 = map2.descendingKeySet();
   *
   *   synchronized (map) { // Synchronizing on map, not map2 or set2!
   *     Iterator<K> it = set2.iterator(); // Must be in synchronized block
   *     while (it.hasNext()){
   *       foo(it.next());
   *     }
   *   }}</pre>
   *
   * Failure to follow this advice may result in non-deterministic behavior.
   *
   * <p>The returned navigable map will be serializable if the specified
   * navigable map is serializable.
   *
   * @param navigableMap the navigable map to be "wrapped" in a synchronized
   *    navigable map.
   * @return a synchronized view of the specified navigable map.
   * @since 13.0
   */
  @Beta
  @GwtIncompatible("NavigableMap")
  public static <K, V> NavigableMap<K, V> synchronizedNavigableMap(
      NavigableMap<K, V> navigableMap) {
    return Synchronized.navigableMap(navigableMap);
  }

  /**
   * {@code AbstractMap} extension that implements {@link #isEmpty()} as {@code
   * entrySet().isEmpty()} instead of {@code size() == 0} to speed up
   * implementations where {@code size()} is O(n), and it delegates the {@code
   * isEmpty()} methods of its key set and value collection to this
   * implementation.
   */
  @GwtCompatible
  abstract static class ImprovedAbstractMap<K, V> extends AbstractMap<K, V> {
    /**
     * Creates the entry set to be returned by {@link #entrySet()}. This method
     * is invoked at most once on a given map, at the time when {@code entrySet}
     * is first called.
     */
    protected abstract Set<Entry<K, V>> createEntrySet();

    private Set<Entry<K, V>> entrySet;

    @Override public Set<Entry<K, V>> entrySet() {
      Set<Entry<K, V>> result = entrySet;
      if (result == null) {
        entrySet = result = createEntrySet();
      }
      return result;
    }

    private Set<K> keySet;

    @Override public Set<K> keySet() {
      Set<K> result = keySet;
      if (result == null) {
        return keySet = new KeySet<K, V>() {
          @Override Map<K, V> map() {
            return ImprovedAbstractMap.this;
          }
        };
      }
      return result;
    }

    private Collection<V> values;

    @Override public Collection<V> values() {
      Collection<V> result = values;
      if (result == null) {
        return values = new Values<K, V>() {
          @Override Map<K, V> map() {
            return ImprovedAbstractMap.this;
          }
        };
      }
      return result;
    }
  }

  static final MapJoiner STANDARD_JOINER =
      Collections2.STANDARD_JOINER.withKeyValueSeparator("=");

  /**
   * Delegates to {@link Map#get}. Returns {@code null} on {@code
   * ClassCastException}.
   */
  static <V> V safeGet(Map<?, V> map, Object key) {
    try {
      return map.get(key);
    } catch (ClassCastException e) {
      return null;
    }
  }

  /**
   * Delegates to {@link Map#containsKey}. Returns {@code false} on {@code
   * ClassCastException}
   */
  static boolean safeContainsKey(Map<?, ?> map, Object key) {
    try {
      return map.containsKey(key);
    } catch (ClassCastException e) {
      return false;
    }
  }

  /**
   * Implements {@code Collection.contains} safely for forwarding collections of
   * map entries. If {@code o} is an instance of {@code Map.Entry}, it is
   * wrapped using {@link #unmodifiableEntry} to protect against a possible
   * nefarious equals method.
   *
   * <p>Note that {@code c} is the backing (delegate) collection, rather than
   * the forwarding collection.
   *
   * @param c the delegate (unwrapped) collection of map entries
   * @param o the object that might be contained in {@code c}
   * @return {@code true} if {@code c} contains {@code o}
   */
  static <K, V> boolean containsEntryImpl(Collection<Entry<K, V>> c, Object o) {
    if (!(o instanceof Entry)) {
      return false;
    }
    return c.contains(unmodifiableEntry((Entry<?, ?>) o));
  }

  /**
   * Implements {@code Collection.remove} safely for forwarding collections of
   * map entries. If {@code o} is an instance of {@code Map.Entry}, it is
   * wrapped using {@link #unmodifiableEntry} to protect against a possible
   * nefarious equals method.
   *
   * <p>Note that {@code c} is backing (delegate) collection, rather than the
   * forwarding collection.
   *
   * @param c the delegate (unwrapped) collection of map entries
   * @param o the object to remove from {@code c}
   * @return {@code true} if {@code c} was changed
   */
  static <K, V> boolean removeEntryImpl(Collection<Entry<K, V>> c, Object o) {
    if (!(o instanceof Entry)) {
      return false;
    }
    return c.remove(unmodifiableEntry((Entry<?, ?>) o));
  }

  /**
   * An implementation of {@link Map#equals}.
   */
  static boolean equalsImpl(Map<?, ?> map, Object object) {
    if (map == object) {
      return true;
    }
    if (object instanceof Map) {
      Map<?, ?> o = (Map<?, ?>) object;
      return map.entrySet().equals(o.entrySet());
    }
    return false;
  }

  /**
   * An implementation of {@link Map#hashCode}.
   */
  static int hashCodeImpl(Map<?, ?> map) {
    return Sets.hashCodeImpl(map.entrySet());
  }

  /**
   * An implementation of {@link Map#toString}.
   */
  static String toStringImpl(Map<?, ?> map) {
    StringBuilder sb
        = Collections2.newStringBuilderForCollection(map.size()).append('{');
    STANDARD_JOINER.appendTo(sb, map);
    return sb.append('}').toString();
  }

  /**
   * An implementation of {@link Map#putAll}.
   */
  static <K, V> void putAllImpl(
      Map<K, V> self, Map<? extends K, ? extends V> map) {
    for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
      self.put(entry.getKey(), entry.getValue());
    }
  }

  /**
   * An admittedly inefficient implementation of {@link Map#containsKey}.
   */
  static boolean containsKeyImpl(Map<?, ?> map, @Nullable Object key) {
    for (Entry<?, ?> entry : map.entrySet()) {
      if (Objects.equal(entry.getKey(), key)) {
        return true;
      }
    }
    return false;
  }

  /**
   * An implementation of {@link Map#containsValue}.
   */
  static boolean containsValueImpl(Map<?, ?> map, @Nullable Object value) {
    for (Entry<?, ?> entry : map.entrySet()) {
      if (Objects.equal(entry.getValue(), value)) {
        return true;
      }
    }
    return false;
  }

  static <K, V> Iterator<K> keyIterator(Iterator<Entry<K, V>> entryIterator) {
    return new TransformedIterator<Entry<K, V>, K>(entryIterator) {
      @Override
      K transform(Entry<K, V> entry) {
        return entry.getKey();
      }
    };
  }

  abstract static class KeySet<K, V> extends Sets.ImprovedAbstractSet<K> {
    abstract Map<K, V> map();

    @Override public Iterator<K> iterator() {
      return keyIterator(map().entrySet().iterator());
    }

    @Override public int size() {
      return map().size();
    }

    @Override public boolean isEmpty() {
      return map().isEmpty();
    }

    @Override public boolean contains(Object o) {
      return map().containsKey(o);
    }

    @Override public boolean remove(Object o) {
      if (contains(o)) {
        map().remove(o);
        return true;
      }
      return false;
    }

    @Override public void clear() {
      map().clear();
    }
  }

  @Nullable
  static <K> K keyOrNull(@Nullable Entry<K, ?> entry) {
    return (entry == null) ? null : entry.getKey();
  }

  @GwtIncompatible("NavigableMap")
  abstract static class NavigableKeySet<K, V> extends KeySet<K, V> implements NavigableSet<K> {
    @Override
    abstract NavigableMap<K, V> map();

    @Override
    public Comparator<? super K> comparator() {
      return map().comparator();
    }

    @Override
    public K first() {
      return map().firstKey();
    }

    @Override
    public K last() {
      return map().lastKey();
    }

    @Override
    public K lower(K e) {
      return map().lowerKey(e);
    }

    @Override
    public K floor(K e) {
      return map().floorKey(e);
    }

    @Override
    public K ceiling(K e) {
      return map().ceilingKey(e);
    }

    @Override
    public K higher(K e) {
      return map().higherKey(e);
    }

    @Override
    public K pollFirst() {
      return keyOrNull(map().pollFirstEntry());
    }

    @Override
    public K pollLast() {
      return keyOrNull(map().pollLastEntry());
    }

    @Override
    public NavigableSet<K> descendingSet() {
      return map().descendingKeySet();
    }

    @Override
    public Iterator<K> descendingIterator() {
      return descendingSet().iterator();
    }

    @Override
    public NavigableSet<K> subSet(
        K fromElement,
        boolean fromInclusive,
        K toElement,
        boolean toInclusive) {
      return map().subMap(fromElement, fromInclusive, toElement, toInclusive).navigableKeySet();
    }

    @Override
    public NavigableSet<K> headSet(K toElement, boolean inclusive) {
      return map().headMap(toElement, inclusive).navigableKeySet();
    }

    @Override
    public NavigableSet<K> tailSet(K fromElement, boolean inclusive) {
      return map().tailMap(fromElement, inclusive).navigableKeySet();
    }

    @Override
    public SortedSet<K> subSet(K fromElement, K toElement) {
      return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<K> headSet(K toElement) {
      return headSet(toElement, false);
    }

    @Override
    public SortedSet<K> tailSet(K fromElement) {
      return tailSet(fromElement, true);
    }
  }

  static <K, V> Iterator<V> valueIterator(Iterator<Entry<K, V>> entryIterator) {
    return new TransformedIterator<Entry<K, V>, V>(entryIterator) {
      @Override
      V transform(Entry<K, V> entry) {
        return entry.getValue();
      }
    };
  }

  static <K, V> UnmodifiableIterator<V> valueIterator(
      final UnmodifiableIterator<Entry<K, V>> entryIterator) {
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

  abstract static class Values<K, V> extends AbstractCollection<V> {
    abstract Map<K, V> map();

    @Override public Iterator<V> iterator() {
      return valueIterator(map().entrySet().iterator());
    }

    @Override public boolean remove(Object o) {
      try {
        return super.remove(o);
      } catch (UnsupportedOperationException e) {
        for (Entry<K, V> entry : map().entrySet()) {
          if (Objects.equal(o, entry.getValue())) {
            map().remove(entry.getKey());
            return true;
          }
        }
        return false;
      }
    }

    @Override public boolean removeAll(Collection<?> c) {
      try {
        return super.removeAll(checkNotNull(c));
      } catch (UnsupportedOperationException e) {
        Set<K> toRemove = Sets.newHashSet();
        for (Entry<K, V> entry : map().entrySet()) {
          if (c.contains(entry.getValue())) {
            toRemove.add(entry.getKey());
          }
        }
        return map().keySet().removeAll(toRemove);
      }
    }

    @Override public boolean retainAll(Collection<?> c) {
      try {
        return super.retainAll(checkNotNull(c));
      } catch (UnsupportedOperationException e) {
        Set<K> toRetain = Sets.newHashSet();
        for (Entry<K, V> entry : map().entrySet()) {
          if (c.contains(entry.getValue())) {
            toRetain.add(entry.getKey());
          }
        }
        return map().keySet().retainAll(toRetain);
      }
    }

    @Override public int size() {
      return map().size();
    }

    @Override public boolean isEmpty() {
      return map().isEmpty();
    }

    @Override public boolean contains(@Nullable Object o) {
      return map().containsValue(o);
    }

    @Override public void clear() {
      map().clear();
    }
  }

  abstract static class EntrySet<K, V>
      extends Sets.ImprovedAbstractSet<Entry<K, V>> {
    abstract Map<K, V> map();

    @Override public int size() {
      return map().size();
    }

    @Override public void clear() {
      map().clear();
    }

    @Override public boolean contains(Object o) {
      if (o instanceof Entry) {
        Entry<?, ?> entry = (Entry<?, ?>) o;
        Object key = entry.getKey();
        V value = map().get(key);
        return Objects.equal(value, entry.getValue())
            && (value != null || map().containsKey(key));
      }
      return false;
    }

    @Override public boolean isEmpty() {
      return map().isEmpty();
    }

    @Override public boolean remove(Object o) {
      if (contains(o)) {
        Entry<?, ?> entry = (Entry<?, ?>) o;
        return map().keySet().remove(entry.getKey());
      }
      return false;
    }

    @Override public boolean removeAll(Collection<?> c) {
      try {
        return super.removeAll(checkNotNull(c));
      } catch (UnsupportedOperationException e) {
        // if the iterators don't support remove
        boolean changed = true;
        for (Object o : c) {
          changed |= remove(o);
        }
        return changed;
      }
    }

    @Override public boolean retainAll(Collection<?> c) {
      try {
        return super.retainAll(checkNotNull(c));
      } catch (UnsupportedOperationException e) {
        // if the iterators don't support remove
        Set<Object> keys = Sets.newHashSetWithExpectedSize(c.size());
        for (Object o : c) {
          if (contains(o)) {
            Entry<?, ?> entry = (Entry<?, ?>) o;
            keys.add(entry.getKey());
          }
        }
        return map().keySet().retainAll(keys);
      }
    }
  }

  @GwtIncompatible("NavigableMap")
  abstract static class DescendingMap<K, V> extends ForwardingMap<K, V>
      implements NavigableMap<K, V> {

    abstract NavigableMap<K, V> forward();

    @Override
    protected final Map<K, V> delegate() {
      return forward();
    }

    private transient Comparator<? super K> comparator;

    @SuppressWarnings("unchecked")
    @Override
    public Comparator<? super K> comparator() {
      Comparator<? super K> result = comparator;
      if (result == null) {
        Comparator<? super K> forwardCmp = forward().comparator();
        if (forwardCmp == null) {
          forwardCmp = (Comparator) Ordering.natural();
        }
        result = comparator = reverse(forwardCmp);
      }
      return result;
    }

    // If we inline this, we get a javac error.
    private static <T> Ordering<T> reverse(Comparator<T> forward) {
      return Ordering.from(forward).reverse();
    }

    @Override
    public K firstKey() {
      return forward().lastKey();
    }

    @Override
    public K lastKey() {
      return forward().firstKey();
    }

    @Override
    public Entry<K, V> lowerEntry(K key) {
      return forward().higherEntry(key);
    }

    @Override
    public K lowerKey(K key) {
      return forward().higherKey(key);
    }

    @Override
    public Entry<K, V> floorEntry(K key) {
      return forward().ceilingEntry(key);
    }

    @Override
    public K floorKey(K key) {
      return forward().ceilingKey(key);
    }

    @Override
    public Entry<K, V> ceilingEntry(K key) {
      return forward().floorEntry(key);
    }

    @Override
    public K ceilingKey(K key) {
      return forward().floorKey(key);
    }

    @Override
    public Entry<K, V> higherEntry(K key) {
      return forward().lowerEntry(key);
    }

    @Override
    public K higherKey(K key) {
      return forward().lowerKey(key);
    }

    @Override
    public Entry<K, V> firstEntry() {
      return forward().lastEntry();
    }

    @Override
    public Entry<K, V> lastEntry() {
      return forward().firstEntry();
    }

    @Override
    public Entry<K, V> pollFirstEntry() {
      return forward().pollLastEntry();
    }

    @Override
    public Entry<K, V> pollLastEntry() {
      return forward().pollFirstEntry();
    }

    @Override
    public NavigableMap<K, V> descendingMap() {
      return forward();
    }

    private transient Set<Entry<K, V>> entrySet;

    @Override
    public Set<Entry<K, V>> entrySet() {
      Set<Entry<K, V>> result = entrySet;
      return (result == null) ? entrySet = createEntrySet() : result;
    }

    abstract Iterator<Entry<K, V>> entryIterator();

    Set<Entry<K, V>> createEntrySet() {
      return new EntrySet<K, V>() {

        @Override
        Map<K, V> map() {
          return DescendingMap.this;
        }

        @Override
        public Iterator<Entry<K, V>> iterator() {
          return entryIterator();
        }
      };
    }

    @Override
    public Set<K> keySet() {
      return navigableKeySet();
    }

    private transient NavigableSet<K> navigableKeySet;

    @Override
    public NavigableSet<K> navigableKeySet() {
      NavigableSet<K> result = navigableKeySet;
      if (result == null) {
        result = navigableKeySet = new NavigableKeySet<K, V>() {
          @Override
          NavigableMap<K, V> map() {
            return DescendingMap.this;
          }
        };
      }
      return result;
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
      return forward().navigableKeySet();
    }

    @Override
    public
        NavigableMap<K, V>
        subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
      return forward().subMap(toKey, toInclusive, fromKey, fromInclusive).descendingMap();
    }

    @Override
    public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
      return forward().tailMap(toKey, inclusive).descendingMap();
    }

    @Override
    public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
      return forward().headMap(fromKey, inclusive).descendingMap();
    }

    @Override
    public SortedMap<K, V> subMap(K fromKey, K toKey) {
      return subMap(fromKey, true, toKey, false);
    }

    @Override
    public SortedMap<K, V> headMap(K toKey) {
      return headMap(toKey, false);
    }

    @Override
    public SortedMap<K, V> tailMap(K fromKey) {
      return tailMap(fromKey, true);
    }

    @Override
    public Collection<V> values() {
      return new Values<K, V>() {
        @Override
        Map<K, V> map() {
          return DescendingMap.this;
        }
      };
    }
  }
}

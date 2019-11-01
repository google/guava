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
import static com.google.common.base.Predicates.compose;
import static com.google.common.collect.CollectPreconditions.checkEntryNotNull;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Converter;
import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.MapDifference.ValueDifference;
import com.google.common.primitives.Ints;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2objc.annotations.RetainedWith;
import com.google.j2objc.annotations.Weak;
import com.google.j2objc.annotations.WeakOuter;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.checkerframework.checker.nullness.compatqual.MonotonicNonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * Static utility methods pertaining to {@link Map} instances (including instances of {@link
 * SortedMap}, {@link BiMap}, etc.). Also see this class's counterparts {@link Lists}, {@link Sets}
 * and {@link Queues}.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/CollectionUtilitiesExplained#maps"> {@code Maps}</a>.
 *
 * @author Kevin Bourrillion
 * @author Mike Bostock
 * @author Isaac Shum
 * @author Louis Wasserman
 * @since 2.0
 */
@GwtCompatible(emulated = true)
public final class Maps {
  private Maps() {}

  private enum EntryFunction implements Function<Entry<?, ?>, Object> {
    KEY {
      @Override
      @NullableDecl
      public Object apply(Entry<?, ?> entry) {
        return entry.getKey();
      }
    },
    VALUE {
      @Override
      @NullableDecl
      public Object apply(Entry<?, ?> entry) {
        return entry.getValue();
      }
    };
  }

  @SuppressWarnings("unchecked")
  static <K> Function<Entry<K, ?>, K> keyFunction() {
    return (Function) EntryFunction.KEY;
  }

  @SuppressWarnings("unchecked")
  static <V> Function<Entry<?, V>, V> valueFunction() {
    return (Function) EntryFunction.VALUE;
  }

  static <K, V> Iterator<K> keyIterator(Iterator<Entry<K, V>> entryIterator) {
    return new TransformedIterator<Entry<K, V>, K>(entryIterator) {
      @Override
      K transform(Entry<K, V> entry) {
        return entry.getKey();
      }
    };
  }

  static <K, V> Iterator<V> valueIterator(Iterator<Entry<K, V>> entryIterator) {
    return new TransformedIterator<Entry<K, V>, V>(entryIterator) {
      @Override
      V transform(Entry<K, V> entry) {
        return entry.getValue();
      }
    };
  }

  /**
   * Returns an immutable map instance containing the given entries. Internally, the returned map
   * will be backed by an {@link EnumMap}.
   *
   * <p>The iteration order of the returned map follows the enum's iteration order, not the order in
   * which the elements appear in the given map.
   *
   * @param map the map to make an immutable copy of
   * @return an immutable map containing those entries
   * @since 14.0
   */
  @GwtCompatible(serializable = true)
  public static <K extends Enum<K>, V> ImmutableMap<K, V> immutableEnumMap(
      Map<K, ? extends V> map) {
    if (map instanceof ImmutableEnumMap) {
      @SuppressWarnings("unchecked") // safe covariant cast
      ImmutableEnumMap<K, V> result = (ImmutableEnumMap<K, V>) map;
      return result;
    }
    Iterator<? extends Entry<K, ? extends V>> entryItr = map.entrySet().iterator();
    if (!entryItr.hasNext()) {
      return ImmutableMap.of();
    }
    Entry<K, ? extends V> entry1 = entryItr.next();
    K key1 = entry1.getKey();
    V value1 = entry1.getValue();
    checkEntryNotNull(key1, value1);
    Class<K> clazz = key1.getDeclaringClass();
    EnumMap<K, V> enumMap = new EnumMap<>(clazz);
    enumMap.put(key1, value1);
    while (entryItr.hasNext()) {
      Entry<K, ? extends V> entry = entryItr.next();
      K key = entry.getKey();
      V value = entry.getValue();
      checkEntryNotNull(key, value);
      enumMap.put(key, value);
    }
    return ImmutableEnumMap.asImmutable(enumMap);
  }

  /**
   * Creates a <i>mutable</i>, empty {@code HashMap} instance.
   *
   * <p><b>Note:</b> if mutability is not required, use {@link ImmutableMap#of()} instead.
   *
   * <p><b>Note:</b> if {@code K} is an {@code enum} type, use {@link #newEnumMap} instead.
   *
   * <p><b>Note for Java 7 and later:</b> this method is now unnecessary and should be treated as
   * deprecated. Instead, use the {@code HashMap} constructor directly, taking advantage of the new
   * <a href="http://goo.gl/iz2Wi">"diamond" syntax</a>.
   *
   * @return a new, empty {@code HashMap}
   */
  public static <K, V> HashMap<K, V> newHashMap() {
    return new HashMap<>();
  }

  /**
   * Creates a <i>mutable</i> {@code HashMap} instance with the same mappings as the specified map.
   *
   * <p><b>Note:</b> if mutability is not required, use {@link ImmutableMap#copyOf(Map)} instead.
   *
   * <p><b>Note:</b> if {@code K} is an {@link Enum} type, use {@link #newEnumMap} instead.
   *
   * <p><b>Note for Java 7 and later:</b> this method is now unnecessary and should be treated as
   * deprecated. Instead, use the {@code HashMap} constructor directly, taking advantage of the new
   * <a href="http://goo.gl/iz2Wi">"diamond" syntax</a>.
   *
   * @param map the mappings to be placed in the new map
   * @return a new {@code HashMap} initialized with the mappings from {@code map}
   */
  public static <K, V> HashMap<K, V> newHashMap(Map<? extends K, ? extends V> map) {
    return new HashMap<>(map);
  }

  /**
   * Creates a {@code HashMap} instance, with a high enough "initial capacity" that it <i>should</i>
   * hold {@code expectedSize} elements without growth. This behavior cannot be broadly guaranteed,
   * but it is observed to be true for OpenJDK 1.7. It also can't be guaranteed that the method
   * isn't inadvertently <i>oversizing</i> the returned map.
   *
   * @param expectedSize the number of entries you expect to add to the returned map
   * @return a new, empty {@code HashMap} with enough capacity to hold {@code expectedSize} entries
   *     without resizing
   * @throws IllegalArgumentException if {@code expectedSize} is negative
   */
  public static <K, V> HashMap<K, V> newHashMapWithExpectedSize(int expectedSize) {
    return new HashMap<>(capacity(expectedSize));
  }

  /**
   * Returns a capacity that is sufficient to keep the map from being resized as long as it grows no
   * larger than expectedSize and the load factor is ≥ its default (0.75).
   */
  static int capacity(int expectedSize) {
    if (expectedSize < 3) {
      checkNonnegative(expectedSize, "expectedSize");
      return expectedSize + 1;
    }
    if (expectedSize < Ints.MAX_POWER_OF_TWO) {
      // This is the calculation used in JDK8 to resize when a putAll
      // happens; it seems to be the most conservative calculation we
      // can make.  0.75 is the default load factor.
      return (int) ((float) expectedSize / 0.75F + 1.0F);
    }
    return Integer.MAX_VALUE; // any large value
  }

  /**
   * Creates a <i>mutable</i>, empty, insertion-ordered {@code LinkedHashMap} instance.
   *
   * <p><b>Note:</b> if mutability is not required, use {@link ImmutableMap#of()} instead.
   *
   * <p><b>Note for Java 7 and later:</b> this method is now unnecessary and should be treated as
   * deprecated. Instead, use the {@code LinkedHashMap} constructor directly, taking advantage of
   * the new <a href="http://goo.gl/iz2Wi">"diamond" syntax</a>.
   *
   * @return a new, empty {@code LinkedHashMap}
   */
  public static <K, V> LinkedHashMap<K, V> newLinkedHashMap() {
    return new LinkedHashMap<>();
  }

  /**
   * Creates a <i>mutable</i>, insertion-ordered {@code LinkedHashMap} instance with the same
   * mappings as the specified map.
   *
   * <p><b>Note:</b> if mutability is not required, use {@link ImmutableMap#copyOf(Map)} instead.
   *
   * <p><b>Note for Java 7 and later:</b> this method is now unnecessary and should be treated as
   * deprecated. Instead, use the {@code LinkedHashMap} constructor directly, taking advantage of
   * the new <a href="http://goo.gl/iz2Wi">"diamond" syntax</a>.
   *
   * @param map the mappings to be placed in the new map
   * @return a new, {@code LinkedHashMap} initialized with the mappings from {@code map}
   */
  public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(Map<? extends K, ? extends V> map) {
    return new LinkedHashMap<>(map);
  }

  /**
   * Creates a {@code LinkedHashMap} instance, with a high enough "initial capacity" that it
   * <i>should</i> hold {@code expectedSize} elements without growth. This behavior cannot be
   * broadly guaranteed, but it is observed to be true for OpenJDK 1.7. It also can't be guaranteed
   * that the method isn't inadvertently <i>oversizing</i> the returned map.
   *
   * @param expectedSize the number of entries you expect to add to the returned map
   * @return a new, empty {@code LinkedHashMap} with enough capacity to hold {@code expectedSize}
   *     entries without resizing
   * @throws IllegalArgumentException if {@code expectedSize} is negative
   * @since 19.0
   */
  public static <K, V> LinkedHashMap<K, V> newLinkedHashMapWithExpectedSize(int expectedSize) {
    return new LinkedHashMap<>(capacity(expectedSize));
  }

  /**
   * Creates a new empty {@link ConcurrentHashMap} instance.
   *
   * @since 3.0
   */
  public static <K, V> ConcurrentMap<K, V> newConcurrentMap() {
    return new ConcurrentHashMap<>();
  }

  /**
   * Creates a <i>mutable</i>, empty {@code TreeMap} instance using the natural ordering of its
   * elements.
   *
   * <p><b>Note:</b> if mutability is not required, use {@link ImmutableSortedMap#of()} instead.
   *
   * <p><b>Note for Java 7 and later:</b> this method is now unnecessary and should be treated as
   * deprecated. Instead, use the {@code TreeMap} constructor directly, taking advantage of the new
   * <a href="http://goo.gl/iz2Wi">"diamond" syntax</a>.
   *
   * @return a new, empty {@code TreeMap}
   */
  public static <K extends Comparable, V> TreeMap<K, V> newTreeMap() {
    return new TreeMap<>();
  }

  /**
   * Creates a <i>mutable</i> {@code TreeMap} instance with the same mappings as the specified map
   * and using the same ordering as the specified map.
   *
   * <p><b>Note:</b> if mutability is not required, use {@link
   * ImmutableSortedMap#copyOfSorted(SortedMap)} instead.
   *
   * <p><b>Note for Java 7 and later:</b> this method is now unnecessary and should be treated as
   * deprecated. Instead, use the {@code TreeMap} constructor directly, taking advantage of the new
   * <a href="http://goo.gl/iz2Wi">"diamond" syntax</a>.
   *
   * @param map the sorted map whose mappings are to be placed in the new map and whose comparator
   *     is to be used to sort the new map
   * @return a new {@code TreeMap} initialized with the mappings from {@code map} and using the
   *     comparator of {@code map}
   */
  public static <K, V> TreeMap<K, V> newTreeMap(SortedMap<K, ? extends V> map) {
    return new TreeMap<>(map);
  }

  /**
   * Creates a <i>mutable</i>, empty {@code TreeMap} instance using the given comparator.
   *
   * <p><b>Note:</b> if mutability is not required, use {@code
   * ImmutableSortedMap.orderedBy(comparator).build()} instead.
   *
   * <p><b>Note for Java 7 and later:</b> this method is now unnecessary and should be treated as
   * deprecated. Instead, use the {@code TreeMap} constructor directly, taking advantage of the new
   * <a href="http://goo.gl/iz2Wi">"diamond" syntax</a>.
   *
   * @param comparator the comparator to sort the keys with
   * @return a new, empty {@code TreeMap}
   */
  public static <C, K extends C, V> TreeMap<K, V> newTreeMap(
      @NullableDecl Comparator<C> comparator) {
    // Ideally, the extra type parameter "C" shouldn't be necessary. It is a
    // work-around of a compiler type inference quirk that prevents the
    // following code from being compiled:
    // Comparator<Class<?>> comparator = null;
    // Map<Class<? extends Throwable>, String> map = newTreeMap(comparator);
    return new TreeMap<>(comparator);
  }

  /**
   * Creates an {@code EnumMap} instance.
   *
   * @param type the key type for this map
   * @return a new, empty {@code EnumMap}
   */
  public static <K extends Enum<K>, V> EnumMap<K, V> newEnumMap(Class<K> type) {
    return new EnumMap<>(checkNotNull(type));
  }

  /**
   * Creates an {@code EnumMap} with the same mappings as the specified map.
   *
   * <p><b>Note for Java 7 and later:</b> this method is now unnecessary and should be treated as
   * deprecated. Instead, use the {@code EnumMap} constructor directly, taking advantage of the new
   * <a href="http://goo.gl/iz2Wi">"diamond" syntax</a>.
   *
   * @param map the map from which to initialize this {@code EnumMap}
   * @return a new {@code EnumMap} initialized with the mappings from {@code map}
   * @throws IllegalArgumentException if {@code m} is not an {@code EnumMap} instance and contains
   *     no mappings
   */
  public static <K extends Enum<K>, V> EnumMap<K, V> newEnumMap(Map<K, ? extends V> map) {
    return new EnumMap<>(map);
  }

  /**
   * Creates an {@code IdentityHashMap} instance.
   *
   * <p><b>Note for Java 7 and later:</b> this method is now unnecessary and should be treated as
   * deprecated. Instead, use the {@code IdentityHashMap} constructor directly, taking advantage of
   * the new <a href="http://goo.gl/iz2Wi">"diamond" syntax</a>.
   *
   * @return a new, empty {@code IdentityHashMap}
   */
  public static <K, V> IdentityHashMap<K, V> newIdentityHashMap() {
    return new IdentityHashMap<>();
  }

  /**
   * Computes the difference between two maps. This difference is an immutable snapshot of the state
   * of the maps at the time this method is called. It will never change, even if the maps change at
   * a later time.
   *
   * <p>Since this method uses {@code HashMap} instances internally, the keys of the supplied maps
   * must be well-behaved with respect to {@link Object#equals} and {@link Object#hashCode}.
   *
   * <p><b>Note:</b>If you only need to know whether two maps have the same mappings, call {@code
   * left.equals(right)} instead of this method.
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
      return difference(sortedLeft, right);
    }
    return difference(left, right, Equivalence.equals());
  }

  /**
   * Computes the difference between two maps. This difference is an immutable snapshot of the state
   * of the maps at the time this method is called. It will never change, even if the maps change at
   * a later time.
   *
   * <p>Since this method uses {@code HashMap} instances internally, the keys of the supplied maps
   * must be well-behaved with respect to {@link Object#equals} and {@link Object#hashCode}.
   *
   * @param left the map to treat as the "left" map for purposes of comparison
   * @param right the map to treat as the "right" map for purposes of comparison
   * @param valueEquivalence the equivalence relationship to use to compare values
   * @return the difference between the two maps
   * @since 10.0
   */
  public static <K, V> MapDifference<K, V> difference(
      Map<? extends K, ? extends V> left,
      Map<? extends K, ? extends V> right,
      Equivalence<? super V> valueEquivalence) {
    Preconditions.checkNotNull(valueEquivalence);

    Map<K, V> onlyOnLeft = newLinkedHashMap();
    Map<K, V> onlyOnRight = new LinkedHashMap<>(right); // will whittle it down
    Map<K, V> onBoth = newLinkedHashMap();
    Map<K, MapDifference.ValueDifference<V>> differences = newLinkedHashMap();
    doDifference(left, right, valueEquivalence, onlyOnLeft, onlyOnRight, onBoth, differences);
    return new MapDifferenceImpl<>(onlyOnLeft, onlyOnRight, onBoth, differences);
  }

  /**
   * Computes the difference between two sorted maps, using the comparator of the left map, or
   * {@code Ordering.natural()} if the left map uses the natural ordering of its elements. This
   * difference is an immutable snapshot of the state of the maps at the time this method is called.
   * It will never change, even if the maps change at a later time.
   *
   * <p>Since this method uses {@code TreeMap} instances internally, the keys of the right map must
   * all compare as distinct according to the comparator of the left map.
   *
   * <p><b>Note:</b>If you only need to know whether two sorted maps have the same mappings, call
   * {@code left.equals(right)} instead of this method.
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
    SortedMap<K, MapDifference.ValueDifference<V>> differences = Maps.newTreeMap(comparator);
    doDifference(left, right, Equivalence.equals(), onlyOnLeft, onlyOnRight, onBoth, differences);
    return new SortedMapDifferenceImpl<>(onlyOnLeft, onlyOnRight, onBoth, differences);
  }

  private static <K, V> void doDifference(
      Map<? extends K, ? extends V> left,
      Map<? extends K, ? extends V> right,
      Equivalence<? super V> valueEquivalence,
      Map<K, V> onlyOnLeft,
      Map<K, V> onlyOnRight,
      Map<K, V> onBoth,
      Map<K, MapDifference.ValueDifference<V>> differences) {
    for (Entry<? extends K, ? extends V> entry : left.entrySet()) {
      K leftKey = entry.getKey();
      V leftValue = entry.getValue();
      if (right.containsKey(leftKey)) {
        V rightValue = onlyOnRight.remove(leftKey);
        if (valueEquivalence.equivalent(leftValue, rightValue)) {
          onBoth.put(leftKey, leftValue);
        } else {
          differences.put(leftKey, ValueDifferenceImpl.create(leftValue, rightValue));
        }
      } else {
        onlyOnLeft.put(leftKey, leftValue);
      }
    }
  }

  private static <K, V> Map<K, V> unmodifiableMap(Map<K, ? extends V> map) {
    if (map instanceof SortedMap) {
      return Collections.unmodifiableSortedMap((SortedMap<K, ? extends V>) map);
    } else {
      return Collections.unmodifiableMap(map);
    }
  }

  static class MapDifferenceImpl<K, V> implements MapDifference<K, V> {
    final Map<K, V> onlyOnLeft;
    final Map<K, V> onlyOnRight;
    final Map<K, V> onBoth;
    final Map<K, ValueDifference<V>> differences;

    MapDifferenceImpl(
        Map<K, V> onlyOnLeft,
        Map<K, V> onlyOnRight,
        Map<K, V> onBoth,
        Map<K, ValueDifference<V>> differences) {
      this.onlyOnLeft = unmodifiableMap(onlyOnLeft);
      this.onlyOnRight = unmodifiableMap(onlyOnRight);
      this.onBoth = unmodifiableMap(onBoth);
      this.differences = unmodifiableMap(differences);
    }

    @Override
    public boolean areEqual() {
      return onlyOnLeft.isEmpty() && onlyOnRight.isEmpty() && differences.isEmpty();
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

    @Override
    public boolean equals(Object object) {
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

    @Override
    public int hashCode() {
      return Objects.hashCode(
          entriesOnlyOnLeft(), entriesOnlyOnRight(), entriesInCommon(), entriesDiffering());
    }

    @Override
    public String toString() {
      if (areEqual()) {
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

  static class ValueDifferenceImpl<V> implements MapDifference.ValueDifference<V> {
    @NullableDecl private final V left;
    @NullableDecl private final V right;

    static <V> ValueDifference<V> create(@NullableDecl V left, @NullableDecl V right) {
      return new ValueDifferenceImpl<V>(left, right);
    }

    private ValueDifferenceImpl(@NullableDecl V left, @NullableDecl V right) {
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

    @Override
    public boolean equals(@NullableDecl Object object) {
      if (object instanceof MapDifference.ValueDifference) {
        MapDifference.ValueDifference<?> that = (MapDifference.ValueDifference<?>) object;
        return Objects.equal(this.left, that.leftValue())
            && Objects.equal(this.right, that.rightValue());
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(left, right);
    }

    @Override
    public String toString() {
      return "(" + left + ", " + right + ")";
    }
  }

  static class SortedMapDifferenceImpl<K, V> extends MapDifferenceImpl<K, V>
      implements SortedMapDifference<K, V> {
    SortedMapDifferenceImpl(
        SortedMap<K, V> onlyOnLeft,
        SortedMap<K, V> onlyOnRight,
        SortedMap<K, V> onBoth,
        SortedMap<K, ValueDifference<V>> differences) {
      super(onlyOnLeft, onlyOnRight, onBoth, differences);
    }

    @Override
    public SortedMap<K, ValueDifference<V>> entriesDiffering() {
      return (SortedMap<K, ValueDifference<V>>) super.entriesDiffering();
    }

    @Override
    public SortedMap<K, V> entriesInCommon() {
      return (SortedMap<K, V>) super.entriesInCommon();
    }

    @Override
    public SortedMap<K, V> entriesOnlyOnLeft() {
      return (SortedMap<K, V>) super.entriesOnlyOnLeft();
    }

    @Override
    public SortedMap<K, V> entriesOnlyOnRight() {
      return (SortedMap<K, V>) super.entriesOnlyOnRight();
    }
  }

  /**
   * Returns the specified comparator if not null; otherwise returns {@code Ordering.natural()}.
   * This method is an abomination of generics; the only purpose of this method is to contain the
   * ugly type-casting in one place.
   */
  @SuppressWarnings("unchecked")
  static <E> Comparator<? super E> orNaturalOrder(@NullableDecl Comparator<? super E> comparator) {
    if (comparator != null) { // can't use ? : because of javac bug 5080917
      return comparator;
    }
    return (Comparator<E>) Ordering.natural();
  }

  /**
   * Returns a live {@link Map} view whose keys are the contents of {@code set} and whose values are
   * computed on demand using {@code function}. To get an immutable <i>copy</i> instead, use {@link
   * #toMap(Iterable, Function)}.
   *
   * <p>Specifically, for each {@code k} in the backing set, the returned map has an entry mapping
   * {@code k} to {@code function.apply(k)}. The {@code keySet}, {@code values}, and {@code
   * entrySet} views of the returned map iterate in the same order as the backing set.
   *
   * <p>Modifications to the backing set are read through to the returned map. The returned map
   * supports removal operations if the backing set does. Removal operations write through to the
   * backing set. The returned map does not support put operations.
   *
   * <p><b>Warning:</b> If the function rejects {@code null}, caution is required to make sure the
   * set does not contain {@code null}, because the view cannot stop {@code null} from being added
   * to the set.
   *
   * <p><b>Warning:</b> This method assumes that for any instance {@code k} of key type {@code K},
   * {@code k.equals(k2)} implies that {@code k2} is also of type {@code K}. Using a key type for
   * which this may not hold, such as {@code ArrayList}, may risk a {@code ClassCastException} when
   * calling methods on the resulting map view.
   *
   * @since 14.0
   */
  public static <K, V> Map<K, V> asMap(Set<K> set, Function<? super K, V> function) {
    return new AsMapView<>(set, function);
  }

  /**
   * Returns a view of the sorted set as a map, mapping keys from the set according to the specified
   * function.
   *
   * <p>Specifically, for each {@code k} in the backing set, the returned map has an entry mapping
   * {@code k} to {@code function.apply(k)}. The {@code keySet}, {@code values}, and {@code
   * entrySet} views of the returned map iterate in the same order as the backing set.
   *
   * <p>Modifications to the backing set are read through to the returned map. The returned map
   * supports removal operations if the backing set does. Removal operations write through to the
   * backing set. The returned map does not support put operations.
   *
   * <p><b>Warning:</b> If the function rejects {@code null}, caution is required to make sure the
   * set does not contain {@code null}, because the view cannot stop {@code null} from being added
   * to the set.
   *
   * <p><b>Warning:</b> This method assumes that for any instance {@code k} of key type {@code K},
   * {@code k.equals(k2)} implies that {@code k2} is also of type {@code K}. Using a key type for
   * which this may not hold, such as {@code ArrayList}, may risk a {@code ClassCastException} when
   * calling methods on the resulting map view.
   *
   * @since 14.0
   */
  public static <K, V> SortedMap<K, V> asMap(SortedSet<K> set, Function<? super K, V> function) {
    return new SortedAsMapView<>(set, function);
  }

  /**
   * Returns a view of the navigable set as a map, mapping keys from the set according to the
   * specified function.
   *
   * <p>Specifically, for each {@code k} in the backing set, the returned map has an entry mapping
   * {@code k} to {@code function.apply(k)}. The {@code keySet}, {@code values}, and {@code
   * entrySet} views of the returned map iterate in the same order as the backing set.
   *
   * <p>Modifications to the backing set are read through to the returned map. The returned map
   * supports removal operations if the backing set does. Removal operations write through to the
   * backing set. The returned map does not support put operations.
   *
   * <p><b>Warning:</b> If the function rejects {@code null}, caution is required to make sure the
   * set does not contain {@code null}, because the view cannot stop {@code null} from being added
   * to the set.
   *
   * <p><b>Warning:</b> This method assumes that for any instance {@code k} of key type {@code K},
   * {@code k.equals(k2)} implies that {@code k2} is also of type {@code K}. Using a key type for
   * which this may not hold, such as {@code ArrayList}, may risk a {@code ClassCastException} when
   * calling methods on the resulting map view.
   *
   * @since 14.0
   */
  @GwtIncompatible // NavigableMap
  public static <K, V> NavigableMap<K, V> asMap(
      NavigableSet<K> set, Function<? super K, V> function) {
    return new NavigableAsMapView<>(set, function);
  }

  private static class AsMapView<K, V> extends ViewCachingAbstractMap<K, V> {

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
    public Set<K> createKeySet() {
      return removeOnlySet(backingSet());
    }

    @Override
    Collection<V> createValues() {
      return Collections2.transform(set, function);
    }

    @Override
    public int size() {
      return backingSet().size();
    }

    @Override
    public boolean containsKey(@NullableDecl Object key) {
      return backingSet().contains(key);
    }

    @Override
    public V get(@NullableDecl Object key) {
      if (Collections2.safeContains(backingSet(), key)) {
        @SuppressWarnings("unchecked") // unsafe, but Javadoc warns about it
        K k = (K) key;
        return function.apply(k);
      } else {
        return null;
      }
    }

    @Override
    public V remove(@NullableDecl Object key) {
      if (backingSet().remove(key)) {
        @SuppressWarnings("unchecked") // unsafe, but Javadoc warns about it
        K k = (K) key;
        return function.apply(k);
      } else {
        return null;
      }
    }

    @Override
    public void clear() {
      backingSet().clear();
    }

    @Override
    protected Set<Entry<K, V>> createEntrySet() {
      @WeakOuter
      class EntrySetImpl extends EntrySet<K, V> {
        @Override
        Map<K, V> map() {
          return AsMapView.this;
        }

        @Override
        public Iterator<Entry<K, V>> iterator() {
          return asMapEntryIterator(backingSet(), function);
        }
      }
      return new EntrySetImpl();
    }
  }

  static <K, V> Iterator<Entry<K, V>> asMapEntryIterator(
      Set<K> set, final Function<? super K, V> function) {
    return new TransformedIterator<K, Entry<K, V>>(set.iterator()) {
      @Override
      Entry<K, V> transform(final K key) {
        return immutableEntry(key, function.apply(key));
      }
    };
  }

  private static class SortedAsMapView<K, V> extends AsMapView<K, V> implements SortedMap<K, V> {

    SortedAsMapView(SortedSet<K> set, Function<? super K, V> function) {
      super(set, function);
    }

    @Override
    SortedSet<K> backingSet() {
      return (SortedSet<K>) super.backingSet();
    }

    @Override
    public Comparator<? super K> comparator() {
      return backingSet().comparator();
    }

    @Override
    public Set<K> keySet() {
      return removeOnlySortedSet(backingSet());
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
  }

  @GwtIncompatible // NavigableMap
  private static final class NavigableAsMapView<K, V> extends AbstractNavigableMap<K, V> {
    /*
     * Using AbstractNavigableMap is simpler than extending SortedAsMapView and rewriting all the
     * NavigableMap methods.
     */

    private final NavigableSet<K> set;
    private final Function<? super K, V> function;

    NavigableAsMapView(NavigableSet<K> ks, Function<? super K, V> vFunction) {
      this.set = checkNotNull(ks);
      this.function = checkNotNull(vFunction);
    }

    @Override
    public NavigableMap<K, V> subMap(
        K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
      return asMap(set.subSet(fromKey, fromInclusive, toKey, toInclusive), function);
    }

    @Override
    public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
      return asMap(set.headSet(toKey, inclusive), function);
    }

    @Override
    public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
      return asMap(set.tailSet(fromKey, inclusive), function);
    }

    @Override
    public Comparator<? super K> comparator() {
      return set.comparator();
    }

    @Override
    @NullableDecl
    public V get(@NullableDecl Object key) {
      if (Collections2.safeContains(set, key)) {
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
    Iterator<Entry<K, V>> entryIterator() {
      return asMapEntryIterator(set, function);
    }

    @Override
    Iterator<Entry<K, V>> descendingEntryIterator() {
      return descendingMap().entrySet().iterator();
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
      return removeOnlyNavigableSet(set);
    }

    @Override
    public int size() {
      return set.size();
    }

    @Override
    public NavigableMap<K, V> descendingMap() {
      return asMap(set.descendingSet(), function);
    }
  }

  private static <E> Set<E> removeOnlySet(final Set<E> set) {
    return new ForwardingSet<E>() {
      @Override
      protected Set<E> delegate() {
        return set;
      }

      @Override
      public boolean add(E element) {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean addAll(Collection<? extends E> es) {
        throw new UnsupportedOperationException();
      }
    };
  }

  private static <E> SortedSet<E> removeOnlySortedSet(final SortedSet<E> set) {
    return new ForwardingSortedSet<E>() {
      @Override
      protected SortedSet<E> delegate() {
        return set;
      }

      @Override
      public boolean add(E element) {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean addAll(Collection<? extends E> es) {
        throw new UnsupportedOperationException();
      }

      @Override
      public SortedSet<E> headSet(E toElement) {
        return removeOnlySortedSet(super.headSet(toElement));
      }

      @Override
      public SortedSet<E> subSet(E fromElement, E toElement) {
        return removeOnlySortedSet(super.subSet(fromElement, toElement));
      }

      @Override
      public SortedSet<E> tailSet(E fromElement) {
        return removeOnlySortedSet(super.tailSet(fromElement));
      }
    };
  }

  @GwtIncompatible // NavigableSet
  private static <E> NavigableSet<E> removeOnlyNavigableSet(final NavigableSet<E> set) {
    return new ForwardingNavigableSet<E>() {
      @Override
      protected NavigableSet<E> delegate() {
        return set;
      }

      @Override
      public boolean add(E element) {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean addAll(Collection<? extends E> es) {
        throw new UnsupportedOperationException();
      }

      @Override
      public SortedSet<E> headSet(E toElement) {
        return removeOnlySortedSet(super.headSet(toElement));
      }

      @Override
      public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return removeOnlyNavigableSet(super.headSet(toElement, inclusive));
      }

      @Override
      public SortedSet<E> subSet(E fromElement, E toElement) {
        return removeOnlySortedSet(super.subSet(fromElement, toElement));
      }

      @Override
      public NavigableSet<E> subSet(
          E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        return removeOnlyNavigableSet(
            super.subSet(fromElement, fromInclusive, toElement, toInclusive));
      }

      @Override
      public SortedSet<E> tailSet(E fromElement) {
        return removeOnlySortedSet(super.tailSet(fromElement));
      }

      @Override
      public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return removeOnlyNavigableSet(super.tailSet(fromElement, inclusive));
      }

      @Override
      public NavigableSet<E> descendingSet() {
        return removeOnlyNavigableSet(super.descendingSet());
      }
    };
  }

  /**
   * Returns an immutable map whose keys are the distinct elements of {@code keys} and whose value
   * for each key was computed by {@code valueFunction}. The map's iteration order is the order of
   * the first appearance of each key in {@code keys}.
   *
   * <p>When there are multiple instances of a key in {@code keys}, it is unspecified whether {@code
   * valueFunction} will be applied to more than one instance of that key and, if it is, which
   * result will be mapped to that key in the returned map.
   *
   * <p>If {@code keys} is a {@link Set}, a live view can be obtained instead of a copy using {@link
   * Maps#asMap(Set, Function)}.
   *
   * @throws NullPointerException if any element of {@code keys} is {@code null}, or if {@code
   *     valueFunction} produces {@code null} for any key
   * @since 14.0
   */
  public static <K, V> ImmutableMap<K, V> toMap(
      Iterable<K> keys, Function<? super K, V> valueFunction) {
    return toMap(keys.iterator(), valueFunction);
  }

  /**
   * Returns an immutable map whose keys are the distinct elements of {@code keys} and whose value
   * for each key was computed by {@code valueFunction}. The map's iteration order is the order of
   * the first appearance of each key in {@code keys}.
   *
   * <p>When there are multiple instances of a key in {@code keys}, it is unspecified whether {@code
   * valueFunction} will be applied to more than one instance of that key and, if it is, which
   * result will be mapped to that key in the returned map.
   *
   * @throws NullPointerException if any element of {@code keys} is {@code null}, or if {@code
   *     valueFunction} produces {@code null} for any key
   * @since 14.0
   */
  public static <K, V> ImmutableMap<K, V> toMap(
      Iterator<K> keys, Function<? super K, V> valueFunction) {
    checkNotNull(valueFunction);
    // Using LHM instead of a builder so as not to fail on duplicate keys
    Map<K, V> builder = newLinkedHashMap();
    while (keys.hasNext()) {
      K key = keys.next();
      builder.put(key, valueFunction.apply(key));
    }
    return ImmutableMap.copyOf(builder);
  }

  /**
   * Returns a map with the given {@code values}, indexed by keys derived from those values. In
   * other words, each input value produces an entry in the map whose key is the result of applying
   * {@code keyFunction} to that value. These entries appear in the same order as the input values.
   * Example usage:
   *
   * <pre>{@code
   * Color red = new Color("red", 255, 0, 0);
   * ...
   * ImmutableSet<Color> allColors = ImmutableSet.of(red, green, blue);
   *
   * Map<String, Color> colorForName =
   *     uniqueIndex(allColors, toStringFunction());
   * assertThat(colorForName).containsEntry("red", red);
   * }</pre>
   *
   * <p>If your index may associate multiple values with each key, use {@link
   * Multimaps#index(Iterable, Function) Multimaps.index}.
   *
   * @param values the values to use when constructing the {@code Map}
   * @param keyFunction the function used to produce the key for each value
   * @return a map mapping the result of evaluating the function {@code keyFunction} on each value
   *     in the input collection to that value
   * @throws IllegalArgumentException if {@code keyFunction} produces the same key for more than one
   *     value in the input collection
   * @throws NullPointerException if any element of {@code values} is {@code null}, or if {@code
   *     keyFunction} produces {@code null} for any value
   */
  @CanIgnoreReturnValue
  public static <K, V> ImmutableMap<K, V> uniqueIndex(
      Iterable<V> values, Function<? super V, K> keyFunction) {
    // TODO(lowasser): consider presizing the builder if values is a Collection
    return uniqueIndex(values.iterator(), keyFunction);
  }

  /**
   * Returns a map with the given {@code values}, indexed by keys derived from those values. In
   * other words, each input value produces an entry in the map whose key is the result of applying
   * {@code keyFunction} to that value. These entries appear in the same order as the input values.
   * Example usage:
   *
   * <pre>{@code
   * Color red = new Color("red", 255, 0, 0);
   * ...
   * Iterator<Color> allColors = ImmutableSet.of(red, green, blue).iterator();
   *
   * Map<String, Color> colorForName =
   *     uniqueIndex(allColors, toStringFunction());
   * assertThat(colorForName).containsEntry("red", red);
   * }</pre>
   *
   * <p>If your index may associate multiple values with each key, use {@link
   * Multimaps#index(Iterator, Function) Multimaps.index}.
   *
   * @param values the values to use when constructing the {@code Map}
   * @param keyFunction the function used to produce the key for each value
   * @return a map mapping the result of evaluating the function {@code keyFunction} on each value
   *     in the input collection to that value
   * @throws IllegalArgumentException if {@code keyFunction} produces the same key for more than one
   *     value in the input collection
   * @throws NullPointerException if any element of {@code values} is {@code null}, or if {@code
   *     keyFunction} produces {@code null} for any value
   * @since 10.0
   */
  @CanIgnoreReturnValue
  public static <K, V> ImmutableMap<K, V> uniqueIndex(
      Iterator<V> values, Function<? super V, K> keyFunction) {
    checkNotNull(keyFunction);
    ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
    while (values.hasNext()) {
      V value = values.next();
      builder.put(keyFunction.apply(value), value);
    }
    try {
      return builder.build();
    } catch (IllegalArgumentException duplicateKeys) {
      throw new IllegalArgumentException(
          duplicateKeys.getMessage()
              + ". To index multiple values under a key, use Multimaps.index.");
    }
  }

  /**
   * Creates an {@code ImmutableMap<String, String>} from a {@code Properties} instance. Properties
   * normally derive from {@code Map<Object, Object>}, but they typically contain strings, which is
   * awkward. This method lets you get a plain-old-{@code Map} out of a {@code Properties}.
   *
   * @param properties a {@code Properties} object to be converted
   * @return an immutable map containing all the entries in {@code properties}
   * @throws ClassCastException if any key in {@code Properties} is not a {@code String}
   * @throws NullPointerException if any key or value in {@code Properties} is null
   */
  @GwtIncompatible // java.util.Properties
  public static ImmutableMap<String, String> fromProperties(Properties properties) {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

    for (Enumeration<?> e = properties.propertyNames(); e.hasMoreElements(); ) {
      String key = (String) e.nextElement();
      builder.put(key, properties.getProperty(key));
    }

    return builder.build();
  }

  /**
   * Returns an immutable map entry with the specified key and value. The {@link Entry#setValue}
   * operation throws an {@link UnsupportedOperationException}.
   *
   * <p>The returned entry is serializable.
   *
   * <p><b>Java 9 users:</b> consider using {@code java.util.Map.entry(key, value)} if the key and
   * value are non-null and the entry does not need to be serializable.
   *
   * @param key the key to be associated with the returned entry
   * @param value the value to be associated with the returned entry
   */
  @GwtCompatible(serializable = true)
  public static <K, V> Entry<K, V> immutableEntry(@NullableDecl K key, @NullableDecl V value) {
    return new ImmutableEntry<>(key, value);
  }

  /**
   * Returns an unmodifiable view of the specified set of entries. The {@link Entry#setValue}
   * operation throws an {@link UnsupportedOperationException}, as do any operations that would
   * modify the returned set.
   *
   * @param entrySet the entries for which to return an unmodifiable view
   * @return an unmodifiable view of the entries
   */
  static <K, V> Set<Entry<K, V>> unmodifiableEntrySet(Set<Entry<K, V>> entrySet) {
    return new UnmodifiableEntrySet<>(Collections.unmodifiableSet(entrySet));
  }

  /**
   * Returns an unmodifiable view of the specified map entry. The {@link Entry#setValue} operation
   * throws an {@link UnsupportedOperationException}. This also has the side-effect of redefining
   * {@code equals} to comply with the Entry contract, to avoid a possible nefarious implementation
   * of equals.
   *
   * @param entry the entry for which to return an unmodifiable view
   * @return an unmodifiable view of the entry
   */
  static <K, V> Entry<K, V> unmodifiableEntry(final Entry<? extends K, ? extends V> entry) {
    checkNotNull(entry);
    return new AbstractMapEntry<K, V>() {
      @Override
      public K getKey() {
        return entry.getKey();
      }

      @Override
      public V getValue() {
        return entry.getValue();
      }
    };
  }

  static <K, V> UnmodifiableIterator<Entry<K, V>> unmodifiableEntryIterator(
      final Iterator<Entry<K, V>> entryIterator) {
    return new UnmodifiableIterator<Entry<K, V>>() {
      @Override
      public boolean hasNext() {
        return entryIterator.hasNext();
      }

      @Override
      public Entry<K, V> next() {
        return unmodifiableEntry(entryIterator.next());
      }
    };
  }

  /** @see Multimaps#unmodifiableEntries */
  static class UnmodifiableEntries<K, V> extends ForwardingCollection<Entry<K, V>> {
    private final Collection<Entry<K, V>> entries;

    UnmodifiableEntries(Collection<Entry<K, V>> entries) {
      this.entries = entries;
    }

    @Override
    protected Collection<Entry<K, V>> delegate() {
      return entries;
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
      return unmodifiableEntryIterator(entries.iterator());
    }

    // See java.util.Collections.UnmodifiableEntrySet for details on attacks.

    @Override
    public Object[] toArray() {
      return standardToArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
      return standardToArray(array);
    }
  }

  /** @see Maps#unmodifiableEntrySet(Set) */
  static class UnmodifiableEntrySet<K, V> extends UnmodifiableEntries<K, V>
      implements Set<Entry<K, V>> {
    UnmodifiableEntrySet(Set<Entry<K, V>> entries) {
      super(entries);
    }

    // See java.util.Collections.UnmodifiableEntrySet for details on attacks.

    @Override
    public boolean equals(@NullableDecl Object object) {
      return Sets.equalsImpl(this, object);
    }

    @Override
    public int hashCode() {
      return Sets.hashCodeImpl(this);
    }
  }

  /**
   * Returns a {@link Converter} that converts values using {@link BiMap#get bimap.get()}, and whose
   * inverse view converts values using {@link BiMap#inverse bimap.inverse()}{@code .get()}.
   *
   * <p>To use a plain {@link Map} as a {@link Function}, see {@link
   * com.google.common.base.Functions#forMap(Map)} or {@link
   * com.google.common.base.Functions#forMap(Map, Object)}.
   *
   * @since 16.0
   */
  public static <A, B> Converter<A, B> asConverter(final BiMap<A, B> bimap) {
    return new BiMapConverter<>(bimap);
  }

  private static final class BiMapConverter<A, B> extends Converter<A, B> implements Serializable {
    private final BiMap<A, B> bimap;

    BiMapConverter(BiMap<A, B> bimap) {
      this.bimap = checkNotNull(bimap);
    }

    @Override
    protected B doForward(A a) {
      return convert(bimap, a);
    }

    @Override
    protected A doBackward(B b) {
      return convert(bimap.inverse(), b);
    }

    private static <X, Y> Y convert(BiMap<X, Y> bimap, X input) {
      Y output = bimap.get(input);
      checkArgument(output != null, "No non-null mapping present for input: %s", input);
      return output;
    }

    @Override
    public boolean equals(@NullableDecl Object object) {
      if (object instanceof BiMapConverter) {
        BiMapConverter<?, ?> that = (BiMapConverter<?, ?>) object;
        return this.bimap.equals(that.bimap);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return bimap.hashCode();
    }

    // There's really no good way to implement toString() without printing the entire BiMap, right?
    @Override
    public String toString() {
      return "Maps.asConverter(" + bimap + ")";
    }

    private static final long serialVersionUID = 0L;
  }

  /**
   * Returns a synchronized (thread-safe) bimap backed by the specified bimap. In order to guarantee
   * serial access, it is critical that <b>all</b> access to the backing bimap is accomplished
   * through the returned bimap.
   *
   * <p>It is imperative that the user manually synchronize on the returned map when accessing any
   * of its collection views:
   *
   * <pre>{@code
   * BiMap<Long, String> map = Maps.synchronizedBiMap(
   *     HashBiMap.<Long, String>create());
   * ...
   * Set<Long> set = map.keySet();  // Needn't be in synchronized block
   * ...
   * synchronized (map) {  // Synchronizing on map, not set!
   *   Iterator<Long> it = set.iterator(); // Must be in synchronized block
   *   while (it.hasNext()) {
   *     foo(it.next());
   *   }
   * }
   * }</pre>
   *
   * <p>Failure to follow this advice may result in non-deterministic behavior.
   *
   * <p>The returned bimap will be serializable if the specified bimap is serializable.
   *
   * @param bimap the bimap to be wrapped in a synchronized view
   * @return a synchronized view of the specified bimap
   */
  public static <K, V> BiMap<K, V> synchronizedBiMap(BiMap<K, V> bimap) {
    return Synchronized.biMap(bimap, null);
  }

  /**
   * Returns an unmodifiable view of the specified bimap. This method allows modules to provide
   * users with "read-only" access to internal bimaps. Query operations on the returned bimap "read
   * through" to the specified bimap, and attempts to modify the returned map, whether direct or via
   * its collection views, result in an {@code UnsupportedOperationException}.
   *
   * <p>The returned bimap will be serializable if the specified bimap is serializable.
   *
   * @param bimap the bimap for which an unmodifiable view is to be returned
   * @return an unmodifiable view of the specified bimap
   */
  public static <K, V> BiMap<K, V> unmodifiableBiMap(BiMap<? extends K, ? extends V> bimap) {
    return new UnmodifiableBiMap<>(bimap, null);
  }

  /** @see Maps#unmodifiableBiMap(BiMap) */
  private static class UnmodifiableBiMap<K, V> extends ForwardingMap<K, V>
      implements BiMap<K, V>, Serializable {
    final Map<K, V> unmodifiableMap;
    final BiMap<? extends K, ? extends V> delegate;
    @MonotonicNonNullDecl @RetainedWith BiMap<V, K> inverse;
    @MonotonicNonNullDecl transient Set<V> values;

    UnmodifiableBiMap(BiMap<? extends K, ? extends V> delegate, @NullableDecl BiMap<V, K> inverse) {
      unmodifiableMap = Collections.unmodifiableMap(delegate);
      this.delegate = delegate;
      this.inverse = inverse;
    }

    @Override
    protected Map<K, V> delegate() {
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
          ? inverse = new UnmodifiableBiMap<>(delegate.inverse(), this)
          : result;
    }

    @Override
    public Set<V> values() {
      Set<V> result = values;
      return (result == null) ? values = Collections.unmodifiableSet(delegate.values()) : result;
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Returns a view of a map where each value is transformed by a function. All other properties of
   * the map, such as iteration order, are left intact. For example, the code:
   *
   * <pre>{@code
   * Map<String, Integer> map = ImmutableMap.of("a", 4, "b", 9);
   * Function<Integer, Double> sqrt =
   *     new Function<Integer, Double>() {
   *       public Double apply(Integer in) {
   *         return Math.sqrt((int) in);
   *       }
   *     };
   * Map<String, Double> transformed = Maps.transformValues(map, sqrt);
   * System.out.println(transformed);
   * }</pre>
   *
   * ... prints {@code {a=2.0, b=3.0}}.
   *
   * <p>Changes in the underlying map are reflected in this view. Conversely, this view supports
   * removal operations, and these are reflected in the underlying map.
   *
   * <p>It's acceptable for the underlying map to contain null keys, and even null values provided
   * that the function is capable of accepting null input. The transformed map might contain null
   * values, if the function sometimes gives a null result.
   *
   * <p>The returned map is not thread-safe or serializable, even if the underlying map is.
   *
   * <p>The function is applied lazily, invoked when needed. This is necessary for the returned map
   * to be a view, but it means that the function will be applied many times for bulk operations
   * like {@link Map#containsValue} and {@code Map.toString()}. For this to perform well, {@code
   * function} should be fast. To avoid lazy evaluation when the returned map doesn't need to be a
   * view, copy the returned map into a new map of your choosing.
   */
  public static <K, V1, V2> Map<K, V2> transformValues(
      Map<K, V1> fromMap, Function<? super V1, V2> function) {
    return transformEntries(fromMap, asEntryTransformer(function));
  }

  /**
   * Returns a view of a sorted map where each value is transformed by a function. All other
   * properties of the map, such as iteration order, are left intact. For example, the code:
   *
   * <pre>{@code
   * SortedMap<String, Integer> map = ImmutableSortedMap.of("a", 4, "b", 9);
   * Function<Integer, Double> sqrt =
   *     new Function<Integer, Double>() {
   *       public Double apply(Integer in) {
   *         return Math.sqrt((int) in);
   *       }
   *     };
   * SortedMap<String, Double> transformed =
   *      Maps.transformValues(map, sqrt);
   * System.out.println(transformed);
   * }</pre>
   *
   * ... prints {@code {a=2.0, b=3.0}}.
   *
   * <p>Changes in the underlying map are reflected in this view. Conversely, this view supports
   * removal operations, and these are reflected in the underlying map.
   *
   * <p>It's acceptable for the underlying map to contain null keys, and even null values provided
   * that the function is capable of accepting null input. The transformed map might contain null
   * values, if the function sometimes gives a null result.
   *
   * <p>The returned map is not thread-safe or serializable, even if the underlying map is.
   *
   * <p>The function is applied lazily, invoked when needed. This is necessary for the returned map
   * to be a view, but it means that the function will be applied many times for bulk operations
   * like {@link Map#containsValue} and {@code Map.toString()}. For this to perform well, {@code
   * function} should be fast. To avoid lazy evaluation when the returned map doesn't need to be a
   * view, copy the returned map into a new map of your choosing.
   *
   * @since 11.0
   */
  public static <K, V1, V2> SortedMap<K, V2> transformValues(
      SortedMap<K, V1> fromMap, Function<? super V1, V2> function) {
    return transformEntries(fromMap, asEntryTransformer(function));
  }

  /**
   * Returns a view of a navigable map where each value is transformed by a function. All other
   * properties of the map, such as iteration order, are left intact. For example, the code:
   *
   * <pre>{@code
   * NavigableMap<String, Integer> map = Maps.newTreeMap();
   * map.put("a", 4);
   * map.put("b", 9);
   * Function<Integer, Double> sqrt =
   *     new Function<Integer, Double>() {
   *       public Double apply(Integer in) {
   *         return Math.sqrt((int) in);
   *       }
   *     };
   * NavigableMap<String, Double> transformed =
   *      Maps.transformNavigableValues(map, sqrt);
   * System.out.println(transformed);
   * }</pre>
   *
   * ... prints {@code {a=2.0, b=3.0}}.
   *
   * <p>Changes in the underlying map are reflected in this view. Conversely, this view supports
   * removal operations, and these are reflected in the underlying map.
   *
   * <p>It's acceptable for the underlying map to contain null keys, and even null values provided
   * that the function is capable of accepting null input. The transformed map might contain null
   * values, if the function sometimes gives a null result.
   *
   * <p>The returned map is not thread-safe or serializable, even if the underlying map is.
   *
   * <p>The function is applied lazily, invoked when needed. This is necessary for the returned map
   * to be a view, but it means that the function will be applied many times for bulk operations
   * like {@link Map#containsValue} and {@code Map.toString()}. For this to perform well, {@code
   * function} should be fast. To avoid lazy evaluation when the returned map doesn't need to be a
   * view, copy the returned map into a new map of your choosing.
   *
   * @since 13.0
   */
  @GwtIncompatible // NavigableMap
  public static <K, V1, V2> NavigableMap<K, V2> transformValues(
      NavigableMap<K, V1> fromMap, Function<? super V1, V2> function) {
    return transformEntries(fromMap, asEntryTransformer(function));
  }

  /**
   * Returns a view of a map whose values are derived from the original map's entries. In contrast
   * to {@link #transformValues}, this method's entry-transformation logic may depend on the key as
   * well as the value.
   *
   * <p>All other properties of the transformed map, such as iteration order, are left intact. For
   * example, the code:
   *
   * <pre>{@code
   * Map<String, Boolean> options =
   *     ImmutableMap.of("verbose", true, "sort", false);
   * EntryTransformer<String, Boolean, String> flagPrefixer =
   *     new EntryTransformer<String, Boolean, String>() {
   *       public String transformEntry(String key, Boolean value) {
   *         return value ? key : "no" + key;
   *       }
   *     };
   * Map<String, String> transformed =
   *     Maps.transformEntries(options, flagPrefixer);
   * System.out.println(transformed);
   * }</pre>
   *
   * ... prints {@code {verbose=verbose, sort=nosort}}.
   *
   * <p>Changes in the underlying map are reflected in this view. Conversely, this view supports
   * removal operations, and these are reflected in the underlying map.
   *
   * <p>It's acceptable for the underlying map to contain null keys and null values provided that
   * the transformer is capable of accepting null inputs. The transformed map might contain null
   * values if the transformer sometimes gives a null result.
   *
   * <p>The returned map is not thread-safe or serializable, even if the underlying map is.
   *
   * <p>The transformer is applied lazily, invoked when needed. This is necessary for the returned
   * map to be a view, but it means that the transformer will be applied many times for bulk
   * operations like {@link Map#containsValue} and {@link Object#toString}. For this to perform
   * well, {@code transformer} should be fast. To avoid lazy evaluation when the returned map
   * doesn't need to be a view, copy the returned map into a new map of your choosing.
   *
   * <p><b>Warning:</b> This method assumes that for any instance {@code k} of {@code
   * EntryTransformer} key type {@code K}, {@code k.equals(k2)} implies that {@code k2} is also of
   * type {@code K}. Using an {@code EntryTransformer} key type for which this may not hold, such as
   * {@code ArrayList}, may risk a {@code ClassCastException} when calling methods on the
   * transformed map.
   *
   * @since 7.0
   */
  public static <K, V1, V2> Map<K, V2> transformEntries(
      Map<K, V1> fromMap, EntryTransformer<? super K, ? super V1, V2> transformer) {
    return new TransformedEntriesMap<>(fromMap, transformer);
  }

  /**
   * Returns a view of a sorted map whose values are derived from the original sorted map's entries.
   * In contrast to {@link #transformValues}, this method's entry-transformation logic may depend on
   * the key as well as the value.
   *
   * <p>All other properties of the transformed map, such as iteration order, are left intact. For
   * example, the code:
   *
   * <pre>{@code
   * Map<String, Boolean> options =
   *     ImmutableSortedMap.of("verbose", true, "sort", false);
   * EntryTransformer<String, Boolean, String> flagPrefixer =
   *     new EntryTransformer<String, Boolean, String>() {
   *       public String transformEntry(String key, Boolean value) {
   *         return value ? key : "yes" + key;
   *       }
   *     };
   * SortedMap<String, String> transformed =
   *     Maps.transformEntries(options, flagPrefixer);
   * System.out.println(transformed);
   * }</pre>
   *
   * ... prints {@code {sort=yessort, verbose=verbose}}.
   *
   * <p>Changes in the underlying map are reflected in this view. Conversely, this view supports
   * removal operations, and these are reflected in the underlying map.
   *
   * <p>It's acceptable for the underlying map to contain null keys and null values provided that
   * the transformer is capable of accepting null inputs. The transformed map might contain null
   * values if the transformer sometimes gives a null result.
   *
   * <p>The returned map is not thread-safe or serializable, even if the underlying map is.
   *
   * <p>The transformer is applied lazily, invoked when needed. This is necessary for the returned
   * map to be a view, but it means that the transformer will be applied many times for bulk
   * operations like {@link Map#containsValue} and {@link Object#toString}. For this to perform
   * well, {@code transformer} should be fast. To avoid lazy evaluation when the returned map
   * doesn't need to be a view, copy the returned map into a new map of your choosing.
   *
   * <p><b>Warning:</b> This method assumes that for any instance {@code k} of {@code
   * EntryTransformer} key type {@code K}, {@code k.equals(k2)} implies that {@code k2} is also of
   * type {@code K}. Using an {@code EntryTransformer} key type for which this may not hold, such as
   * {@code ArrayList}, may risk a {@code ClassCastException} when calling methods on the
   * transformed map.
   *
   * @since 11.0
   */
  public static <K, V1, V2> SortedMap<K, V2> transformEntries(
      SortedMap<K, V1> fromMap, EntryTransformer<? super K, ? super V1, V2> transformer) {
    return new TransformedEntriesSortedMap<>(fromMap, transformer);
  }

  /**
   * Returns a view of a navigable map whose values are derived from the original navigable map's
   * entries. In contrast to {@link #transformValues}, this method's entry-transformation logic may
   * depend on the key as well as the value.
   *
   * <p>All other properties of the transformed map, such as iteration order, are left intact. For
   * example, the code:
   *
   * <pre>{@code
   * NavigableMap<String, Boolean> options = Maps.newTreeMap();
   * options.put("verbose", false);
   * options.put("sort", true);
   * EntryTransformer<String, Boolean, String> flagPrefixer =
   *     new EntryTransformer<String, Boolean, String>() {
   *       public String transformEntry(String key, Boolean value) {
   *         return value ? key : ("yes" + key);
   *       }
   *     };
   * NavigableMap<String, String> transformed =
   *     LabsMaps.transformNavigableEntries(options, flagPrefixer);
   * System.out.println(transformed);
   * }</pre>
   *
   * ... prints {@code {sort=yessort, verbose=verbose}}.
   *
   * <p>Changes in the underlying map are reflected in this view. Conversely, this view supports
   * removal operations, and these are reflected in the underlying map.
   *
   * <p>It's acceptable for the underlying map to contain null keys and null values provided that
   * the transformer is capable of accepting null inputs. The transformed map might contain null
   * values if the transformer sometimes gives a null result.
   *
   * <p>The returned map is not thread-safe or serializable, even if the underlying map is.
   *
   * <p>The transformer is applied lazily, invoked when needed. This is necessary for the returned
   * map to be a view, but it means that the transformer will be applied many times for bulk
   * operations like {@link Map#containsValue} and {@link Object#toString}. For this to perform
   * well, {@code transformer} should be fast. To avoid lazy evaluation when the returned map
   * doesn't need to be a view, copy the returned map into a new map of your choosing.
   *
   * <p><b>Warning:</b> This method assumes that for any instance {@code k} of {@code
   * EntryTransformer} key type {@code K}, {@code k.equals(k2)} implies that {@code k2} is also of
   * type {@code K}. Using an {@code EntryTransformer} key type for which this may not hold, such as
   * {@code ArrayList}, may risk a {@code ClassCastException} when calling methods on the
   * transformed map.
   *
   * @since 13.0
   */
  @GwtIncompatible // NavigableMap
  public static <K, V1, V2> NavigableMap<K, V2> transformEntries(
      NavigableMap<K, V1> fromMap, EntryTransformer<? super K, ? super V1, V2> transformer) {
    return new TransformedEntriesNavigableMap<>(fromMap, transformer);
  }

  /**
   * A transformation of the value of a key-value pair, using both key and value as inputs. To apply
   * the transformation to a map, use {@link Maps#transformEntries(Map, EntryTransformer)}.
   *
   * @param <K> the key type of the input and output entries
   * @param <V1> the value type of the input entry
   * @param <V2> the value type of the output entry
   * @since 7.0
   */
  public interface EntryTransformer<K, V1, V2> {
    /**
     * Determines an output value based on a key-value pair. This method is <i>generally
     * expected</i>, but not absolutely required, to have the following properties:
     *
     * <ul>
     *   <li>Its execution does not cause any observable side effects.
     *   <li>The computation is <i>consistent with equals</i>; that is, {@link Objects#equal
     *       Objects.equal}{@code (k1, k2) &&} {@link Objects#equal}{@code (v1, v2)} implies that
     *       {@code Objects.equal(transformer.transform(k1, v1), transformer.transform(k2, v2))}.
     * </ul>
     *
     * @throws NullPointerException if the key or value is null and this transformer does not accept
     *     null arguments
     */
    V2 transformEntry(@NullableDecl K key, @NullableDecl V1 value);
  }

  /** Views a function as an entry transformer that ignores the entry key. */
  static <K, V1, V2> EntryTransformer<K, V1, V2> asEntryTransformer(
      final Function<? super V1, V2> function) {
    checkNotNull(function);
    return new EntryTransformer<K, V1, V2>() {
      @Override
      public V2 transformEntry(K key, V1 value) {
        return function.apply(value);
      }
    };
  }

  static <K, V1, V2> Function<V1, V2> asValueToValueFunction(
      final EntryTransformer<? super K, V1, V2> transformer, final K key) {
    checkNotNull(transformer);
    return new Function<V1, V2>() {
      @Override
      public V2 apply(@NullableDecl V1 v1) {
        return transformer.transformEntry(key, v1);
      }
    };
  }

  /** Views an entry transformer as a function from {@code Entry} to values. */
  static <K, V1, V2> Function<Entry<K, V1>, V2> asEntryToValueFunction(
      final EntryTransformer<? super K, ? super V1, V2> transformer) {
    checkNotNull(transformer);
    return new Function<Entry<K, V1>, V2>() {
      @Override
      public V2 apply(Entry<K, V1> entry) {
        return transformer.transformEntry(entry.getKey(), entry.getValue());
      }
    };
  }

  /** Returns a view of an entry transformed by the specified transformer. */
  static <V2, K, V1> Entry<K, V2> transformEntry(
      final EntryTransformer<? super K, ? super V1, V2> transformer, final Entry<K, V1> entry) {
    checkNotNull(transformer);
    checkNotNull(entry);
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

  /** Views an entry transformer as a function from entries to entries. */
  static <K, V1, V2> Function<Entry<K, V1>, Entry<K, V2>> asEntryToEntryFunction(
      final EntryTransformer<? super K, ? super V1, V2> transformer) {
    checkNotNull(transformer);
    return new Function<Entry<K, V1>, Entry<K, V2>>() {
      @Override
      public Entry<K, V2> apply(final Entry<K, V1> entry) {
        return transformEntry(transformer, entry);
      }
    };
  }

  static class TransformedEntriesMap<K, V1, V2> extends IteratorBasedAbstractMap<K, V2> {
    final Map<K, V1> fromMap;
    final EntryTransformer<? super K, ? super V1, V2> transformer;

    TransformedEntriesMap(
        Map<K, V1> fromMap, EntryTransformer<? super K, ? super V1, V2> transformer) {
      this.fromMap = checkNotNull(fromMap);
      this.transformer = checkNotNull(transformer);
    }

    @Override
    public int size() {
      return fromMap.size();
    }

    @Override
    public boolean containsKey(Object key) {
      return fromMap.containsKey(key);
    }

    // safe as long as the user followed the <b>Warning</b> in the javadoc
    @SuppressWarnings("unchecked")
    @Override
    public V2 get(Object key) {
      V1 value = fromMap.get(key);
      return (value != null || fromMap.containsKey(key))
          ? transformer.transformEntry((K) key, value)
          : null;
    }

    // safe as long as the user followed the <b>Warning</b> in the javadoc
    @SuppressWarnings("unchecked")
    @Override
    public V2 remove(Object key) {
      return fromMap.containsKey(key)
          ? transformer.transformEntry((K) key, fromMap.remove(key))
          : null;
    }

    @Override
    public void clear() {
      fromMap.clear();
    }

    @Override
    public Set<K> keySet() {
      return fromMap.keySet();
    }

    @Override
    Iterator<Entry<K, V2>> entryIterator() {
      return Iterators.transform(
          fromMap.entrySet().iterator(), Maps.<K, V1, V2>asEntryToEntryFunction(transformer));
    }

    @Override
    public Collection<V2> values() {
      return new Values<>(this);
    }
  }

  static class TransformedEntriesSortedMap<K, V1, V2> extends TransformedEntriesMap<K, V1, V2>
      implements SortedMap<K, V2> {

    protected SortedMap<K, V1> fromMap() {
      return (SortedMap<K, V1>) fromMap;
    }

    TransformedEntriesSortedMap(
        SortedMap<K, V1> fromMap, EntryTransformer<? super K, ? super V1, V2> transformer) {
      super(fromMap, transformer);
    }

    @Override
    public Comparator<? super K> comparator() {
      return fromMap().comparator();
    }

    @Override
    public K firstKey() {
      return fromMap().firstKey();
    }

    @Override
    public SortedMap<K, V2> headMap(K toKey) {
      return transformEntries(fromMap().headMap(toKey), transformer);
    }

    @Override
    public K lastKey() {
      return fromMap().lastKey();
    }

    @Override
    public SortedMap<K, V2> subMap(K fromKey, K toKey) {
      return transformEntries(fromMap().subMap(fromKey, toKey), transformer);
    }

    @Override
    public SortedMap<K, V2> tailMap(K fromKey) {
      return transformEntries(fromMap().tailMap(fromKey), transformer);
    }
  }

  @GwtIncompatible // NavigableMap
  private static class TransformedEntriesNavigableMap<K, V1, V2>
      extends TransformedEntriesSortedMap<K, V1, V2> implements NavigableMap<K, V2> {

    TransformedEntriesNavigableMap(
        NavigableMap<K, V1> fromMap, EntryTransformer<? super K, ? super V1, V2> transformer) {
      super(fromMap, transformer);
    }

    @Override
    public Entry<K, V2> ceilingEntry(K key) {
      return transformEntry(fromMap().ceilingEntry(key));
    }

    @Override
    public K ceilingKey(K key) {
      return fromMap().ceilingKey(key);
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
      return fromMap().descendingKeySet();
    }

    @Override
    public NavigableMap<K, V2> descendingMap() {
      return transformEntries(fromMap().descendingMap(), transformer);
    }

    @Override
    public Entry<K, V2> firstEntry() {
      return transformEntry(fromMap().firstEntry());
    }

    @Override
    public Entry<K, V2> floorEntry(K key) {
      return transformEntry(fromMap().floorEntry(key));
    }

    @Override
    public K floorKey(K key) {
      return fromMap().floorKey(key);
    }

    @Override
    public NavigableMap<K, V2> headMap(K toKey) {
      return headMap(toKey, false);
    }

    @Override
    public NavigableMap<K, V2> headMap(K toKey, boolean inclusive) {
      return transformEntries(fromMap().headMap(toKey, inclusive), transformer);
    }

    @Override
    public Entry<K, V2> higherEntry(K key) {
      return transformEntry(fromMap().higherEntry(key));
    }

    @Override
    public K higherKey(K key) {
      return fromMap().higherKey(key);
    }

    @Override
    public Entry<K, V2> lastEntry() {
      return transformEntry(fromMap().lastEntry());
    }

    @Override
    public Entry<K, V2> lowerEntry(K key) {
      return transformEntry(fromMap().lowerEntry(key));
    }

    @Override
    public K lowerKey(K key) {
      return fromMap().lowerKey(key);
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
      return fromMap().navigableKeySet();
    }

    @Override
    public Entry<K, V2> pollFirstEntry() {
      return transformEntry(fromMap().pollFirstEntry());
    }

    @Override
    public Entry<K, V2> pollLastEntry() {
      return transformEntry(fromMap().pollLastEntry());
    }

    @Override
    public NavigableMap<K, V2> subMap(
        K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
      return transformEntries(
          fromMap().subMap(fromKey, fromInclusive, toKey, toInclusive), transformer);
    }

    @Override
    public NavigableMap<K, V2> subMap(K fromKey, K toKey) {
      return subMap(fromKey, true, toKey, false);
    }

    @Override
    public NavigableMap<K, V2> tailMap(K fromKey) {
      return tailMap(fromKey, true);
    }

    @Override
    public NavigableMap<K, V2> tailMap(K fromKey, boolean inclusive) {
      return transformEntries(fromMap().tailMap(fromKey, inclusive), transformer);
    }

    @NullableDecl
    private Entry<K, V2> transformEntry(@NullableDecl Entry<K, V1> entry) {
      return (entry == null) ? null : Maps.transformEntry(transformer, entry);
    }

    @Override
    protected NavigableMap<K, V1> fromMap() {
      return (NavigableMap<K, V1>) super.fromMap();
    }
  }

  static <K> Predicate<Entry<K, ?>> keyPredicateOnEntries(Predicate<? super K> keyPredicate) {
    return compose(keyPredicate, Maps.<K>keyFunction());
  }

  static <V> Predicate<Entry<?, V>> valuePredicateOnEntries(Predicate<? super V> valuePredicate) {
    return compose(valuePredicate, Maps.<V>valueFunction());
  }

  /**
   * Returns a map containing the mappings in {@code unfiltered} whose keys satisfy a predicate. The
   * returned map is a live view of {@code unfiltered}; changes to one affect the other.
   *
   * <p>The resulting map's {@code keySet()}, {@code entrySet()}, and {@code values()} views have
   * iterators that don't support {@code remove()}, but all other methods are supported by the map
   * and its views. When given a key that doesn't satisfy the predicate, the map's {@code put()} and
   * {@code putAll()} methods throw an {@link IllegalArgumentException}.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called on the filtered map
   * or its views, only mappings whose keys satisfy the filter will be removed from the underlying
   * map.
   *
   * <p>The returned map isn't threadsafe or serializable, even if {@code unfiltered} is.
   *
   * <p>Many of the filtered map's methods, such as {@code size()}, iterate across every key/value
   * mapping in the underlying map and determine which satisfy the filter. When a live view is
   * <i>not</i> needed, it may be faster to copy the filtered map and use the copy.
   *
   * <p><b>Warning:</b> {@code keyPredicate} must be <i>consistent with equals</i>, as documented at
   * {@link Predicate#apply}. Do not provide a predicate such as {@code
   * Predicates.instanceOf(ArrayList.class)}, which is inconsistent with equals.
   */
  public static <K, V> Map<K, V> filterKeys(
      Map<K, V> unfiltered, final Predicate<? super K> keyPredicate) {
    checkNotNull(keyPredicate);
    Predicate<Entry<K, ?>> entryPredicate = keyPredicateOnEntries(keyPredicate);
    return (unfiltered instanceof AbstractFilteredMap)
        ? filterFiltered((AbstractFilteredMap<K, V>) unfiltered, entryPredicate)
        : new FilteredKeyMap<K, V>(checkNotNull(unfiltered), keyPredicate, entryPredicate);
  }

  /**
   * Returns a sorted map containing the mappings in {@code unfiltered} whose keys satisfy a
   * predicate. The returned map is a live view of {@code unfiltered}; changes to one affect the
   * other.
   *
   * <p>The resulting map's {@code keySet()}, {@code entrySet()}, and {@code values()} views have
   * iterators that don't support {@code remove()}, but all other methods are supported by the map
   * and its views. When given a key that doesn't satisfy the predicate, the map's {@code put()} and
   * {@code putAll()} methods throw an {@link IllegalArgumentException}.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called on the filtered map
   * or its views, only mappings whose keys satisfy the filter will be removed from the underlying
   * map.
   *
   * <p>The returned map isn't threadsafe or serializable, even if {@code unfiltered} is.
   *
   * <p>Many of the filtered map's methods, such as {@code size()}, iterate across every key/value
   * mapping in the underlying map and determine which satisfy the filter. When a live view is
   * <i>not</i> needed, it may be faster to copy the filtered map and use the copy.
   *
   * <p><b>Warning:</b> {@code keyPredicate} must be <i>consistent with equals</i>, as documented at
   * {@link Predicate#apply}. Do not provide a predicate such as {@code
   * Predicates.instanceOf(ArrayList.class)}, which is inconsistent with equals.
   *
   * @since 11.0
   */
  public static <K, V> SortedMap<K, V> filterKeys(
      SortedMap<K, V> unfiltered, final Predicate<? super K> keyPredicate) {
    // TODO(lowasser): Return a subclass of Maps.FilteredKeyMap for slightly better
    // performance.
    return filterEntries(unfiltered, Maps.<K>keyPredicateOnEntries(keyPredicate));
  }

  /**
   * Returns a navigable map containing the mappings in {@code unfiltered} whose keys satisfy a
   * predicate. The returned map is a live view of {@code unfiltered}; changes to one affect the
   * other.
   *
   * <p>The resulting map's {@code keySet()}, {@code entrySet()}, and {@code values()} views have
   * iterators that don't support {@code remove()}, but all other methods are supported by the map
   * and its views. When given a key that doesn't satisfy the predicate, the map's {@code put()} and
   * {@code putAll()} methods throw an {@link IllegalArgumentException}.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called on the filtered map
   * or its views, only mappings whose keys satisfy the filter will be removed from the underlying
   * map.
   *
   * <p>The returned map isn't threadsafe or serializable, even if {@code unfiltered} is.
   *
   * <p>Many of the filtered map's methods, such as {@code size()}, iterate across every key/value
   * mapping in the underlying map and determine which satisfy the filter. When a live view is
   * <i>not</i> needed, it may be faster to copy the filtered map and use the copy.
   *
   * <p><b>Warning:</b> {@code keyPredicate} must be <i>consistent with equals</i>, as documented at
   * {@link Predicate#apply}. Do not provide a predicate such as {@code
   * Predicates.instanceOf(ArrayList.class)}, which is inconsistent with equals.
   *
   * @since 14.0
   */
  @GwtIncompatible // NavigableMap
  public static <K, V> NavigableMap<K, V> filterKeys(
      NavigableMap<K, V> unfiltered, final Predicate<? super K> keyPredicate) {
    // TODO(lowasser): Return a subclass of Maps.FilteredKeyMap for slightly better
    // performance.
    return filterEntries(unfiltered, Maps.<K>keyPredicateOnEntries(keyPredicate));
  }

  /**
   * Returns a bimap containing the mappings in {@code unfiltered} whose keys satisfy a predicate.
   * The returned bimap is a live view of {@code unfiltered}; changes to one affect the other.
   *
   * <p>The resulting bimap's {@code keySet()}, {@code entrySet()}, and {@code values()} views have
   * iterators that don't support {@code remove()}, but all other methods are supported by the bimap
   * and its views. When given a key that doesn't satisfy the predicate, the bimap's {@code put()},
   * {@code forcePut()} and {@code putAll()} methods throw an {@link IllegalArgumentException}.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called on the filtered
   * bimap or its views, only mappings that satisfy the filter will be removed from the underlying
   * bimap.
   *
   * <p>The returned bimap isn't threadsafe or serializable, even if {@code unfiltered} is.
   *
   * <p>Many of the filtered bimap's methods, such as {@code size()}, iterate across every key in
   * the underlying bimap and determine which satisfy the filter. When a live view is <i>not</i>
   * needed, it may be faster to copy the filtered bimap and use the copy.
   *
   * <p><b>Warning:</b> {@code entryPredicate} must be <i>consistent with equals </i>, as documented
   * at {@link Predicate#apply}.
   *
   * @since 14.0
   */
  public static <K, V> BiMap<K, V> filterKeys(
      BiMap<K, V> unfiltered, final Predicate<? super K> keyPredicate) {
    checkNotNull(keyPredicate);
    return filterEntries(unfiltered, Maps.<K>keyPredicateOnEntries(keyPredicate));
  }

  /**
   * Returns a map containing the mappings in {@code unfiltered} whose values satisfy a predicate.
   * The returned map is a live view of {@code unfiltered}; changes to one affect the other.
   *
   * <p>The resulting map's {@code keySet()}, {@code entrySet()}, and {@code values()} views have
   * iterators that don't support {@code remove()}, but all other methods are supported by the map
   * and its views. When given a value that doesn't satisfy the predicate, the map's {@code put()},
   * {@code putAll()}, and {@link Entry#setValue} methods throw an {@link IllegalArgumentException}.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called on the filtered map
   * or its views, only mappings whose values satisfy the filter will be removed from the underlying
   * map.
   *
   * <p>The returned map isn't threadsafe or serializable, even if {@code unfiltered} is.
   *
   * <p>Many of the filtered map's methods, such as {@code size()}, iterate across every key/value
   * mapping in the underlying map and determine which satisfy the filter. When a live view is
   * <i>not</i> needed, it may be faster to copy the filtered map and use the copy.
   *
   * <p><b>Warning:</b> {@code valuePredicate} must be <i>consistent with equals</i>, as documented
   * at {@link Predicate#apply}. Do not provide a predicate such as {@code
   * Predicates.instanceOf(ArrayList.class)}, which is inconsistent with equals.
   */
  public static <K, V> Map<K, V> filterValues(
      Map<K, V> unfiltered, final Predicate<? super V> valuePredicate) {
    return filterEntries(unfiltered, Maps.<V>valuePredicateOnEntries(valuePredicate));
  }

  /**
   * Returns a sorted map containing the mappings in {@code unfiltered} whose values satisfy a
   * predicate. The returned map is a live view of {@code unfiltered}; changes to one affect the
   * other.
   *
   * <p>The resulting map's {@code keySet()}, {@code entrySet()}, and {@code values()} views have
   * iterators that don't support {@code remove()}, but all other methods are supported by the map
   * and its views. When given a value that doesn't satisfy the predicate, the map's {@code put()},
   * {@code putAll()}, and {@link Entry#setValue} methods throw an {@link IllegalArgumentException}.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called on the filtered map
   * or its views, only mappings whose values satisfy the filter will be removed from the underlying
   * map.
   *
   * <p>The returned map isn't threadsafe or serializable, even if {@code unfiltered} is.
   *
   * <p>Many of the filtered map's methods, such as {@code size()}, iterate across every key/value
   * mapping in the underlying map and determine which satisfy the filter. When a live view is
   * <i>not</i> needed, it may be faster to copy the filtered map and use the copy.
   *
   * <p><b>Warning:</b> {@code valuePredicate} must be <i>consistent with equals</i>, as documented
   * at {@link Predicate#apply}. Do not provide a predicate such as {@code
   * Predicates.instanceOf(ArrayList.class)}, which is inconsistent with equals.
   *
   * @since 11.0
   */
  public static <K, V> SortedMap<K, V> filterValues(
      SortedMap<K, V> unfiltered, final Predicate<? super V> valuePredicate) {
    return filterEntries(unfiltered, Maps.<V>valuePredicateOnEntries(valuePredicate));
  }

  /**
   * Returns a navigable map containing the mappings in {@code unfiltered} whose values satisfy a
   * predicate. The returned map is a live view of {@code unfiltered}; changes to one affect the
   * other.
   *
   * <p>The resulting map's {@code keySet()}, {@code entrySet()}, and {@code values()} views have
   * iterators that don't support {@code remove()}, but all other methods are supported by the map
   * and its views. When given a value that doesn't satisfy the predicate, the map's {@code put()},
   * {@code putAll()}, and {@link Entry#setValue} methods throw an {@link IllegalArgumentException}.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called on the filtered map
   * or its views, only mappings whose values satisfy the filter will be removed from the underlying
   * map.
   *
   * <p>The returned map isn't threadsafe or serializable, even if {@code unfiltered} is.
   *
   * <p>Many of the filtered map's methods, such as {@code size()}, iterate across every key/value
   * mapping in the underlying map and determine which satisfy the filter. When a live view is
   * <i>not</i> needed, it may be faster to copy the filtered map and use the copy.
   *
   * <p><b>Warning:</b> {@code valuePredicate} must be <i>consistent with equals</i>, as documented
   * at {@link Predicate#apply}. Do not provide a predicate such as {@code
   * Predicates.instanceOf(ArrayList.class)}, which is inconsistent with equals.
   *
   * @since 14.0
   */
  @GwtIncompatible // NavigableMap
  public static <K, V> NavigableMap<K, V> filterValues(
      NavigableMap<K, V> unfiltered, final Predicate<? super V> valuePredicate) {
    return filterEntries(unfiltered, Maps.<V>valuePredicateOnEntries(valuePredicate));
  }

  /**
   * Returns a bimap containing the mappings in {@code unfiltered} whose values satisfy a predicate.
   * The returned bimap is a live view of {@code unfiltered}; changes to one affect the other.
   *
   * <p>The resulting bimap's {@code keySet()}, {@code entrySet()}, and {@code values()} views have
   * iterators that don't support {@code remove()}, but all other methods are supported by the bimap
   * and its views. When given a value that doesn't satisfy the predicate, the bimap's {@code
   * put()}, {@code forcePut()} and {@code putAll()} methods throw an {@link
   * IllegalArgumentException}. Similarly, the map's entries have a {@link Entry#setValue} method
   * that throws an {@link IllegalArgumentException} when the provided value doesn't satisfy the
   * predicate.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called on the filtered
   * bimap or its views, only mappings that satisfy the filter will be removed from the underlying
   * bimap.
   *
   * <p>The returned bimap isn't threadsafe or serializable, even if {@code unfiltered} is.
   *
   * <p>Many of the filtered bimap's methods, such as {@code size()}, iterate across every value in
   * the underlying bimap and determine which satisfy the filter. When a live view is <i>not</i>
   * needed, it may be faster to copy the filtered bimap and use the copy.
   *
   * <p><b>Warning:</b> {@code entryPredicate} must be <i>consistent with equals </i>, as documented
   * at {@link Predicate#apply}.
   *
   * @since 14.0
   */
  public static <K, V> BiMap<K, V> filterValues(
      BiMap<K, V> unfiltered, final Predicate<? super V> valuePredicate) {
    return filterEntries(unfiltered, Maps.<V>valuePredicateOnEntries(valuePredicate));
  }

  /**
   * Returns a map containing the mappings in {@code unfiltered} that satisfy a predicate. The
   * returned map is a live view of {@code unfiltered}; changes to one affect the other.
   *
   * <p>The resulting map's {@code keySet()}, {@code entrySet()}, and {@code values()} views have
   * iterators that don't support {@code remove()}, but all other methods are supported by the map
   * and its views. When given a key/value pair that doesn't satisfy the predicate, the map's {@code
   * put()} and {@code putAll()} methods throw an {@link IllegalArgumentException}. Similarly, the
   * map's entries have a {@link Entry#setValue} method that throws an {@link
   * IllegalArgumentException} when the existing key and the provided value don't satisfy the
   * predicate.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called on the filtered map
   * or its views, only mappings that satisfy the filter will be removed from the underlying map.
   *
   * <p>The returned map isn't threadsafe or serializable, even if {@code unfiltered} is.
   *
   * <p>Many of the filtered map's methods, such as {@code size()}, iterate across every key/value
   * mapping in the underlying map and determine which satisfy the filter. When a live view is
   * <i>not</i> needed, it may be faster to copy the filtered map and use the copy.
   *
   * <p><b>Warning:</b> {@code entryPredicate} must be <i>consistent with equals</i>, as documented
   * at {@link Predicate#apply}.
   */
  public static <K, V> Map<K, V> filterEntries(
      Map<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate) {
    checkNotNull(entryPredicate);
    return (unfiltered instanceof AbstractFilteredMap)
        ? filterFiltered((AbstractFilteredMap<K, V>) unfiltered, entryPredicate)
        : new FilteredEntryMap<K, V>(checkNotNull(unfiltered), entryPredicate);
  }

  /**
   * Returns a sorted map containing the mappings in {@code unfiltered} that satisfy a predicate.
   * The returned map is a live view of {@code unfiltered}; changes to one affect the other.
   *
   * <p>The resulting map's {@code keySet()}, {@code entrySet()}, and {@code values()} views have
   * iterators that don't support {@code remove()}, but all other methods are supported by the map
   * and its views. When given a key/value pair that doesn't satisfy the predicate, the map's {@code
   * put()} and {@code putAll()} methods throw an {@link IllegalArgumentException}. Similarly, the
   * map's entries have a {@link Entry#setValue} method that throws an {@link
   * IllegalArgumentException} when the existing key and the provided value don't satisfy the
   * predicate.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called on the filtered map
   * or its views, only mappings that satisfy the filter will be removed from the underlying map.
   *
   * <p>The returned map isn't threadsafe or serializable, even if {@code unfiltered} is.
   *
   * <p>Many of the filtered map's methods, such as {@code size()}, iterate across every key/value
   * mapping in the underlying map and determine which satisfy the filter. When a live view is
   * <i>not</i> needed, it may be faster to copy the filtered map and use the copy.
   *
   * <p><b>Warning:</b> {@code entryPredicate} must be <i>consistent with equals</i>, as documented
   * at {@link Predicate#apply}.
   *
   * @since 11.0
   */
  public static <K, V> SortedMap<K, V> filterEntries(
      SortedMap<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate) {
    checkNotNull(entryPredicate);
    return (unfiltered instanceof FilteredEntrySortedMap)
        ? filterFiltered((FilteredEntrySortedMap<K, V>) unfiltered, entryPredicate)
        : new FilteredEntrySortedMap<K, V>(checkNotNull(unfiltered), entryPredicate);
  }

  /**
   * Returns a sorted map containing the mappings in {@code unfiltered} that satisfy a predicate.
   * The returned map is a live view of {@code unfiltered}; changes to one affect the other.
   *
   * <p>The resulting map's {@code keySet()}, {@code entrySet()}, and {@code values()} views have
   * iterators that don't support {@code remove()}, but all other methods are supported by the map
   * and its views. When given a key/value pair that doesn't satisfy the predicate, the map's {@code
   * put()} and {@code putAll()} methods throw an {@link IllegalArgumentException}. Similarly, the
   * map's entries have a {@link Entry#setValue} method that throws an {@link
   * IllegalArgumentException} when the existing key and the provided value don't satisfy the
   * predicate.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called on the filtered map
   * or its views, only mappings that satisfy the filter will be removed from the underlying map.
   *
   * <p>The returned map isn't threadsafe or serializable, even if {@code unfiltered} is.
   *
   * <p>Many of the filtered map's methods, such as {@code size()}, iterate across every key/value
   * mapping in the underlying map and determine which satisfy the filter. When a live view is
   * <i>not</i> needed, it may be faster to copy the filtered map and use the copy.
   *
   * <p><b>Warning:</b> {@code entryPredicate} must be <i>consistent with equals</i>, as documented
   * at {@link Predicate#apply}.
   *
   * @since 14.0
   */
  @GwtIncompatible // NavigableMap
  public static <K, V> NavigableMap<K, V> filterEntries(
      NavigableMap<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate) {
    checkNotNull(entryPredicate);
    return (unfiltered instanceof FilteredEntryNavigableMap)
        ? filterFiltered((FilteredEntryNavigableMap<K, V>) unfiltered, entryPredicate)
        : new FilteredEntryNavigableMap<K, V>(checkNotNull(unfiltered), entryPredicate);
  }

  /**
   * Returns a bimap containing the mappings in {@code unfiltered} that satisfy a predicate. The
   * returned bimap is a live view of {@code unfiltered}; changes to one affect the other.
   *
   * <p>The resulting bimap's {@code keySet()}, {@code entrySet()}, and {@code values()} views have
   * iterators that don't support {@code remove()}, but all other methods are supported by the bimap
   * and its views. When given a key/value pair that doesn't satisfy the predicate, the bimap's
   * {@code put()}, {@code forcePut()} and {@code putAll()} methods throw an {@link
   * IllegalArgumentException}. Similarly, the map's entries have an {@link Entry#setValue} method
   * that throws an {@link IllegalArgumentException} when the existing key and the provided value
   * don't satisfy the predicate.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called on the filtered
   * bimap or its views, only mappings that satisfy the filter will be removed from the underlying
   * bimap.
   *
   * <p>The returned bimap isn't threadsafe or serializable, even if {@code unfiltered} is.
   *
   * <p>Many of the filtered bimap's methods, such as {@code size()}, iterate across every key/value
   * mapping in the underlying bimap and determine which satisfy the filter. When a live view is
   * <i>not</i> needed, it may be faster to copy the filtered bimap and use the copy.
   *
   * <p><b>Warning:</b> {@code entryPredicate} must be <i>consistent with equals </i>, as documented
   * at {@link Predicate#apply}.
   *
   * @since 14.0
   */
  public static <K, V> BiMap<K, V> filterEntries(
      BiMap<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate) {
    checkNotNull(unfiltered);
    checkNotNull(entryPredicate);
    return (unfiltered instanceof FilteredEntryBiMap)
        ? filterFiltered((FilteredEntryBiMap<K, V>) unfiltered, entryPredicate)
        : new FilteredEntryBiMap<K, V>(unfiltered, entryPredicate);
  }

  /**
   * Support {@code clear()}, {@code removeAll()}, and {@code retainAll()} when filtering a filtered
   * map.
   */
  private static <K, V> Map<K, V> filterFiltered(
      AbstractFilteredMap<K, V> map, Predicate<? super Entry<K, V>> entryPredicate) {
    return new FilteredEntryMap<>(
        map.unfiltered, Predicates.<Entry<K, V>>and(map.predicate, entryPredicate));
  }

  /**
   * Support {@code clear()}, {@code removeAll()}, and {@code retainAll()} when filtering a filtered
   * sorted map.
   */
  private static <K, V> SortedMap<K, V> filterFiltered(
      FilteredEntrySortedMap<K, V> map, Predicate<? super Entry<K, V>> entryPredicate) {
    Predicate<Entry<K, V>> predicate = Predicates.<Entry<K, V>>and(map.predicate, entryPredicate);
    return new FilteredEntrySortedMap<>(map.sortedMap(), predicate);
  }

  /**
   * Support {@code clear()}, {@code removeAll()}, and {@code retainAll()} when filtering a filtered
   * navigable map.
   */
  @GwtIncompatible // NavigableMap
  private static <K, V> NavigableMap<K, V> filterFiltered(
      FilteredEntryNavigableMap<K, V> map, Predicate<? super Entry<K, V>> entryPredicate) {
    Predicate<Entry<K, V>> predicate =
        Predicates.<Entry<K, V>>and(map.entryPredicate, entryPredicate);
    return new FilteredEntryNavigableMap<>(map.unfiltered, predicate);
  }

  /**
   * Support {@code clear()}, {@code removeAll()}, and {@code retainAll()} when filtering a filtered
   * map.
   */
  private static <K, V> BiMap<K, V> filterFiltered(
      FilteredEntryBiMap<K, V> map, Predicate<? super Entry<K, V>> entryPredicate) {
    Predicate<Entry<K, V>> predicate = Predicates.<Entry<K, V>>and(map.predicate, entryPredicate);
    return new FilteredEntryBiMap<>(map.unfiltered(), predicate);
  }

  private abstract static class AbstractFilteredMap<K, V> extends ViewCachingAbstractMap<K, V> {
    final Map<K, V> unfiltered;
    final Predicate<? super Entry<K, V>> predicate;

    AbstractFilteredMap(Map<K, V> unfiltered, Predicate<? super Entry<K, V>> predicate) {
      this.unfiltered = unfiltered;
      this.predicate = predicate;
    }

    boolean apply(@NullableDecl Object key, @NullableDecl V value) {
      // This method is called only when the key is in the map, implying that
      // key is a K.
      @SuppressWarnings("unchecked")
      K k = (K) key;
      return predicate.apply(Maps.immutableEntry(k, value));
    }

    @Override
    public V put(K key, V value) {
      checkArgument(apply(key, value));
      return unfiltered.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
      for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
        checkArgument(apply(entry.getKey(), entry.getValue()));
      }
      unfiltered.putAll(map);
    }

    @Override
    public boolean containsKey(Object key) {
      return unfiltered.containsKey(key) && apply(key, unfiltered.get(key));
    }

    @Override
    public V get(Object key) {
      V value = unfiltered.get(key);
      return ((value != null) && apply(key, value)) ? value : null;
    }

    @Override
    public boolean isEmpty() {
      return entrySet().isEmpty();
    }

    @Override
    public V remove(Object key) {
      return containsKey(key) ? unfiltered.remove(key) : null;
    }

    @Override
    Collection<V> createValues() {
      return new FilteredMapValues<>(this, unfiltered, predicate);
    }
  }

  private static final class FilteredMapValues<K, V> extends Maps.Values<K, V> {
    final Map<K, V> unfiltered;
    final Predicate<? super Entry<K, V>> predicate;

    FilteredMapValues(
        Map<K, V> filteredMap, Map<K, V> unfiltered, Predicate<? super Entry<K, V>> predicate) {
      super(filteredMap);
      this.unfiltered = unfiltered;
      this.predicate = predicate;
    }

    @Override
    public boolean remove(Object o) {
      Iterator<Entry<K, V>> entryItr = unfiltered.entrySet().iterator();
      while (entryItr.hasNext()) {
        Entry<K, V> entry = entryItr.next();
        if (predicate.apply(entry) && Objects.equal(entry.getValue(), o)) {
          entryItr.remove();
          return true;
        }
      }
      return false;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
      Iterator<Entry<K, V>> entryItr = unfiltered.entrySet().iterator();
      boolean result = false;
      while (entryItr.hasNext()) {
        Entry<K, V> entry = entryItr.next();
        if (predicate.apply(entry) && collection.contains(entry.getValue())) {
          entryItr.remove();
          result = true;
        }
      }
      return result;
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
      Iterator<Entry<K, V>> entryItr = unfiltered.entrySet().iterator();
      boolean result = false;
      while (entryItr.hasNext()) {
        Entry<K, V> entry = entryItr.next();
        if (predicate.apply(entry) && !collection.contains(entry.getValue())) {
          entryItr.remove();
          result = true;
        }
      }
      return result;
    }

    @Override
    public Object[] toArray() {
      // creating an ArrayList so filtering happens once
      return Lists.newArrayList(iterator()).toArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
      return Lists.newArrayList(iterator()).toArray(array);
    }
  }

  private static class FilteredKeyMap<K, V> extends AbstractFilteredMap<K, V> {
    final Predicate<? super K> keyPredicate;

    FilteredKeyMap(
        Map<K, V> unfiltered,
        Predicate<? super K> keyPredicate,
        Predicate<? super Entry<K, V>> entryPredicate) {
      super(unfiltered, entryPredicate);
      this.keyPredicate = keyPredicate;
    }

    @Override
    protected Set<Entry<K, V>> createEntrySet() {
      return Sets.filter(unfiltered.entrySet(), predicate);
    }

    @Override
    Set<K> createKeySet() {
      return Sets.filter(unfiltered.keySet(), keyPredicate);
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
     * Entries in this set satisfy the predicate, but they don't validate the input to {@code
     * Entry.setValue()}.
     */
    final Set<Entry<K, V>> filteredEntrySet;

    FilteredEntryMap(Map<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate) {
      super(unfiltered, entryPredicate);
      filteredEntrySet = Sets.filter(unfiltered.entrySet(), predicate);
    }

    @Override
    protected Set<Entry<K, V>> createEntrySet() {
      return new EntrySet();
    }

    @WeakOuter
    private class EntrySet extends ForwardingSet<Entry<K, V>> {
      @Override
      protected Set<Entry<K, V>> delegate() {
        return filteredEntrySet;
      }

      @Override
      public Iterator<Entry<K, V>> iterator() {
        return new TransformedIterator<Entry<K, V>, Entry<K, V>>(filteredEntrySet.iterator()) {
          @Override
          Entry<K, V> transform(final Entry<K, V> entry) {
            return new ForwardingMapEntry<K, V>() {
              @Override
              protected Entry<K, V> delegate() {
                return entry;
              }

              @Override
              public V setValue(V newValue) {
                checkArgument(apply(getKey(), newValue));
                return super.setValue(newValue);
              }
            };
          }
        };
      }
    }

    @Override
    Set<K> createKeySet() {
      return new KeySet();
    }

    static <K, V> boolean removeAllKeys(
        Map<K, V> map, Predicate<? super Entry<K, V>> entryPredicate, Collection<?> keyCollection) {
      Iterator<Entry<K, V>> entryItr = map.entrySet().iterator();
      boolean result = false;
      while (entryItr.hasNext()) {
        Entry<K, V> entry = entryItr.next();
        if (entryPredicate.apply(entry) && keyCollection.contains(entry.getKey())) {
          entryItr.remove();
          result = true;
        }
      }
      return result;
    }

    static <K, V> boolean retainAllKeys(
        Map<K, V> map, Predicate<? super Entry<K, V>> entryPredicate, Collection<?> keyCollection) {
      Iterator<Entry<K, V>> entryItr = map.entrySet().iterator();
      boolean result = false;
      while (entryItr.hasNext()) {
        Entry<K, V> entry = entryItr.next();
        if (entryPredicate.apply(entry) && !keyCollection.contains(entry.getKey())) {
          entryItr.remove();
          result = true;
        }
      }
      return result;
    }

    @WeakOuter
    class KeySet extends Maps.KeySet<K, V> {
      KeySet() {
        super(FilteredEntryMap.this);
      }

      @Override
      public boolean remove(Object o) {
        if (containsKey(o)) {
          unfiltered.remove(o);
          return true;
        }
        return false;
      }

      @Override
      public boolean removeAll(Collection<?> collection) {
        return removeAllKeys(unfiltered, predicate, collection);
      }

      @Override
      public boolean retainAll(Collection<?> collection) {
        return retainAllKeys(unfiltered, predicate, collection);
      }

      @Override
      public Object[] toArray() {
        // creating an ArrayList so filtering happens once
        return Lists.newArrayList(iterator()).toArray();
      }

      @Override
      public <T> T[] toArray(T[] array) {
        return Lists.newArrayList(iterator()).toArray(array);
      }
    }
  }

  private static class FilteredEntrySortedMap<K, V> extends FilteredEntryMap<K, V>
      implements SortedMap<K, V> {

    FilteredEntrySortedMap(
        SortedMap<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate) {
      super(unfiltered, entryPredicate);
    }

    SortedMap<K, V> sortedMap() {
      return (SortedMap<K, V>) unfiltered;
    }

    @Override
    public SortedSet<K> keySet() {
      return (SortedSet<K>) super.keySet();
    }

    @Override
    SortedSet<K> createKeySet() {
      return new SortedKeySet();
    }

    @WeakOuter
    class SortedKeySet extends KeySet implements SortedSet<K> {
      @Override
      public Comparator<? super K> comparator() {
        return sortedMap().comparator();
      }

      @Override
      public SortedSet<K> subSet(K fromElement, K toElement) {
        return (SortedSet<K>) subMap(fromElement, toElement).keySet();
      }

      @Override
      public SortedSet<K> headSet(K toElement) {
        return (SortedSet<K>) headMap(toElement).keySet();
      }

      @Override
      public SortedSet<K> tailSet(K fromElement) {
        return (SortedSet<K>) tailMap(fromElement).keySet();
      }

      @Override
      public K first() {
        return firstKey();
      }

      @Override
      public K last() {
        return lastKey();
      }
    }

    @Override
    public Comparator<? super K> comparator() {
      return sortedMap().comparator();
    }

    @Override
    public K firstKey() {
      // correctly throws NoSuchElementException when filtered map is empty.
      return keySet().iterator().next();
    }

    @Override
    public K lastKey() {
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

    @Override
    public SortedMap<K, V> headMap(K toKey) {
      return new FilteredEntrySortedMap<>(sortedMap().headMap(toKey), predicate);
    }

    @Override
    public SortedMap<K, V> subMap(K fromKey, K toKey) {
      return new FilteredEntrySortedMap<>(sortedMap().subMap(fromKey, toKey), predicate);
    }

    @Override
    public SortedMap<K, V> tailMap(K fromKey) {
      return new FilteredEntrySortedMap<>(sortedMap().tailMap(fromKey), predicate);
    }
  }

  @GwtIncompatible // NavigableMap
  private static class FilteredEntryNavigableMap<K, V> extends AbstractNavigableMap<K, V> {
    /*
     * It's less code to extend AbstractNavigableMap and forward the filtering logic to
     * FilteredEntryMap than to extend FilteredEntrySortedMap and reimplement all the NavigableMap
     * methods.
     */

    private final NavigableMap<K, V> unfiltered;
    private final Predicate<? super Entry<K, V>> entryPredicate;
    private final Map<K, V> filteredDelegate;

    FilteredEntryNavigableMap(
        NavigableMap<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate) {
      this.unfiltered = checkNotNull(unfiltered);
      this.entryPredicate = entryPredicate;
      this.filteredDelegate = new FilteredEntryMap<>(unfiltered, entryPredicate);
    }

    @Override
    public Comparator<? super K> comparator() {
      return unfiltered.comparator();
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
      return new Maps.NavigableKeySet<K, V>(this) {
        @Override
        public boolean removeAll(Collection<?> collection) {
          return FilteredEntryMap.removeAllKeys(unfiltered, entryPredicate, collection);
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
          return FilteredEntryMap.retainAllKeys(unfiltered, entryPredicate, collection);
        }
      };
    }

    @Override
    public Collection<V> values() {
      return new FilteredMapValues<>(this, unfiltered, entryPredicate);
    }

    @Override
    Iterator<Entry<K, V>> entryIterator() {
      return Iterators.filter(unfiltered.entrySet().iterator(), entryPredicate);
    }

    @Override
    Iterator<Entry<K, V>> descendingEntryIterator() {
      return Iterators.filter(unfiltered.descendingMap().entrySet().iterator(), entryPredicate);
    }

    @Override
    public int size() {
      return filteredDelegate.size();
    }

    @Override
    public boolean isEmpty() {
      return !Iterables.any(unfiltered.entrySet(), entryPredicate);
    }

    @Override
    @NullableDecl
    public V get(@NullableDecl Object key) {
      return filteredDelegate.get(key);
    }

    @Override
    public boolean containsKey(@NullableDecl Object key) {
      return filteredDelegate.containsKey(key);
    }

    @Override
    public V put(K key, V value) {
      return filteredDelegate.put(key, value);
    }

    @Override
    public V remove(@NullableDecl Object key) {
      return filteredDelegate.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
      filteredDelegate.putAll(m);
    }

    @Override
    public void clear() {
      filteredDelegate.clear();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
      return filteredDelegate.entrySet();
    }

    @Override
    public Entry<K, V> pollFirstEntry() {
      return Iterables.removeFirstMatching(unfiltered.entrySet(), entryPredicate);
    }

    @Override
    public Entry<K, V> pollLastEntry() {
      return Iterables.removeFirstMatching(unfiltered.descendingMap().entrySet(), entryPredicate);
    }

    @Override
    public NavigableMap<K, V> descendingMap() {
      return filterEntries(unfiltered.descendingMap(), entryPredicate);
    }

    @Override
    public NavigableMap<K, V> subMap(
        K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
      return filterEntries(
          unfiltered.subMap(fromKey, fromInclusive, toKey, toInclusive), entryPredicate);
    }

    @Override
    public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
      return filterEntries(unfiltered.headMap(toKey, inclusive), entryPredicate);
    }

    @Override
    public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
      return filterEntries(unfiltered.tailMap(fromKey, inclusive), entryPredicate);
    }
  }

  static final class FilteredEntryBiMap<K, V> extends FilteredEntryMap<K, V>
      implements BiMap<K, V> {
    @RetainedWith private final BiMap<V, K> inverse;

    private static <K, V> Predicate<Entry<V, K>> inversePredicate(
        final Predicate<? super Entry<K, V>> forwardPredicate) {
      return new Predicate<Entry<V, K>>() {
        @Override
        public boolean apply(Entry<V, K> input) {
          return forwardPredicate.apply(Maps.immutableEntry(input.getValue(), input.getKey()));
        }
      };
    }

    FilteredEntryBiMap(BiMap<K, V> delegate, Predicate<? super Entry<K, V>> predicate) {
      super(delegate, predicate);
      this.inverse =
          new FilteredEntryBiMap<>(delegate.inverse(), inversePredicate(predicate), this);
    }

    private FilteredEntryBiMap(
        BiMap<K, V> delegate, Predicate<? super Entry<K, V>> predicate, BiMap<V, K> inverse) {
      super(delegate, predicate);
      this.inverse = inverse;
    }

    BiMap<K, V> unfiltered() {
      return (BiMap<K, V>) unfiltered;
    }

    @Override
    public V forcePut(@NullableDecl K key, @NullableDecl V value) {
      checkArgument(apply(key, value));
      return unfiltered().forcePut(key, value);
    }

    @Override
    public BiMap<V, K> inverse() {
      return inverse;
    }

    @Override
    public Set<V> values() {
      return inverse.keySet();
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
   * <p>This method's signature will not permit you to convert a {@code NavigableMap<? extends K,
   * V>} to a {@code NavigableMap<K, V>}. If it permitted this, the returned map's {@code
   * comparator()} method might return a {@code Comparator<? extends K>}, which works only on a
   * particular subtype of {@code K}, but promise that it's a {@code Comparator<? super K>}, which
   * must work on any type of {@code K}.
   *
   * @param map the navigable map for which an unmodifiable view is to be returned
   * @return an unmodifiable view of the specified navigable map
   * @since 12.0
   */
  @GwtIncompatible // NavigableMap
  public static <K, V> NavigableMap<K, V> unmodifiableNavigableMap(
      NavigableMap<K, ? extends V> map) {
    checkNotNull(map);
    if (map instanceof UnmodifiableNavigableMap) {
      @SuppressWarnings("unchecked") // covariant
      NavigableMap<K, V> result = (NavigableMap<K, V>) map;
      return result;
    } else {
      return new UnmodifiableNavigableMap<>(map);
    }
  }

  @NullableDecl
  private static <K, V> Entry<K, V> unmodifiableOrNull(@NullableDecl Entry<K, ? extends V> entry) {
    return (entry == null) ? null : Maps.unmodifiableEntry(entry);
  }

  @GwtIncompatible // NavigableMap
  static class UnmodifiableNavigableMap<K, V> extends ForwardingSortedMap<K, V>
      implements NavigableMap<K, V>, Serializable {
    private final NavigableMap<K, ? extends V> delegate;

    UnmodifiableNavigableMap(NavigableMap<K, ? extends V> delegate) {
      this.delegate = delegate;
    }

    UnmodifiableNavigableMap(
        NavigableMap<K, ? extends V> delegate, UnmodifiableNavigableMap<K, V> descendingMap) {
      this.delegate = delegate;
      this.descendingMap = descendingMap;
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

    @MonotonicNonNullDecl private transient UnmodifiableNavigableMap<K, V> descendingMap;

    @Override
    public NavigableMap<K, V> descendingMap() {
      UnmodifiableNavigableMap<K, V> result = descendingMap;
      return (result == null)
          ? descendingMap = new UnmodifiableNavigableMap<>(delegate.descendingMap(), this)
          : result;
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
    public NavigableMap<K, V> subMap(
        K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
      return Maps.unmodifiableNavigableMap(
          delegate.subMap(fromKey, fromInclusive, toKey, toInclusive));
    }

    @Override
    public SortedMap<K, V> headMap(K toKey) {
      return headMap(toKey, false);
    }

    @Override
    public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
      return Maps.unmodifiableNavigableMap(delegate.headMap(toKey, inclusive));
    }

    @Override
    public SortedMap<K, V> tailMap(K fromKey) {
      return tailMap(fromKey, true);
    }

    @Override
    public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
      return Maps.unmodifiableNavigableMap(delegate.tailMap(fromKey, inclusive));
    }
  }

  /**
   * Returns a synchronized (thread-safe) navigable map backed by the specified navigable map. In
   * order to guarantee serial access, it is critical that <b>all</b> access to the backing
   * navigable map is accomplished through the returned navigable map (or its views).
   *
   * <p>It is imperative that the user manually synchronize on the returned navigable map when
   * iterating over any of its collection views, or the collections views of any of its {@code
   * descendingMap}, {@code subMap}, {@code headMap} or {@code tailMap} views.
   *
   * <pre>{@code
   * NavigableMap<K, V> map = synchronizedNavigableMap(new TreeMap<K, V>());
   *
   * // Needn't be in synchronized block
   * NavigableSet<K> set = map.navigableKeySet();
   *
   * synchronized (map) { // Synchronizing on map, not set!
   *   Iterator<K> it = set.iterator(); // Must be in synchronized block
   *   while (it.hasNext()) {
   *     foo(it.next());
   *   }
   * }
   * }</pre>
   *
   * <p>or:
   *
   * <pre>{@code
   * NavigableMap<K, V> map = synchronizedNavigableMap(new TreeMap<K, V>());
   * NavigableMap<K, V> map2 = map.subMap(foo, false, bar, true);
   *
   * // Needn't be in synchronized block
   * NavigableSet<K> set2 = map2.descendingKeySet();
   *
   * synchronized (map) { // Synchronizing on map, not map2 or set2!
   *   Iterator<K> it = set2.iterator(); // Must be in synchronized block
   *   while (it.hasNext()) {
   *     foo(it.next());
   *   }
   * }
   * }</pre>
   *
   * <p>Failure to follow this advice may result in non-deterministic behavior.
   *
   * <p>The returned navigable map will be serializable if the specified navigable map is
   * serializable.
   *
   * @param navigableMap the navigable map to be "wrapped" in a synchronized navigable map.
   * @return a synchronized view of the specified navigable map.
   * @since 13.0
   */
  @GwtIncompatible // NavigableMap
  public static <K, V> NavigableMap<K, V> synchronizedNavigableMap(
      NavigableMap<K, V> navigableMap) {
    return Synchronized.navigableMap(navigableMap);
  }

  /**
   * {@code AbstractMap} extension that makes it easy to cache customized keySet, values, and
   * entrySet views.
   */
  @GwtCompatible
  abstract static class ViewCachingAbstractMap<K, V> extends AbstractMap<K, V> {
    /**
     * Creates the entry set to be returned by {@link #entrySet()}. This method is invoked at most
     * once on a given map, at the time when {@code entrySet} is first called.
     */
    abstract Set<Entry<K, V>> createEntrySet();

    @MonotonicNonNullDecl private transient Set<Entry<K, V>> entrySet;

    @Override
    public Set<Entry<K, V>> entrySet() {
      Set<Entry<K, V>> result = entrySet;
      return (result == null) ? entrySet = createEntrySet() : result;
    }

    @MonotonicNonNullDecl private transient Set<K> keySet;

    @Override
    public Set<K> keySet() {
      Set<K> result = keySet;
      return (result == null) ? keySet = createKeySet() : result;
    }

    Set<K> createKeySet() {
      return new KeySet<>(this);
    }

    @MonotonicNonNullDecl private transient Collection<V> values;

    @Override
    public Collection<V> values() {
      Collection<V> result = values;
      return (result == null) ? values = createValues() : result;
    }

    Collection<V> createValues() {
      return new Values<>(this);
    }
  }

  abstract static class IteratorBasedAbstractMap<K, V> extends AbstractMap<K, V> {
    @Override
    public abstract int size();

    abstract Iterator<Entry<K, V>> entryIterator();

    @Override
    public Set<Entry<K, V>> entrySet() {
      return new EntrySet<K, V>() {
        @Override
        Map<K, V> map() {
          return IteratorBasedAbstractMap.this;
        }

        @Override
        public Iterator<Entry<K, V>> iterator() {
          return entryIterator();
        }
      };
    }

    @Override
    public void clear() {
      Iterators.clear(entryIterator());
    }
  }

  /**
   * Delegates to {@link Map#get}. Returns {@code null} on {@code ClassCastException} and {@code
   * NullPointerException}.
   */
  static <V> V safeGet(Map<?, V> map, @NullableDecl Object key) {
    checkNotNull(map);
    try {
      return map.get(key);
    } catch (ClassCastException | NullPointerException e) {
      return null;
    }
  }

  /**
   * Delegates to {@link Map#containsKey}. Returns {@code false} on {@code ClassCastException} and
   * {@code NullPointerException}.
   */
  static boolean safeContainsKey(Map<?, ?> map, Object key) {
    checkNotNull(map);
    try {
      return map.containsKey(key);
    } catch (ClassCastException | NullPointerException e) {
      return false;
    }
  }

  /**
   * Delegates to {@link Map#remove}. Returns {@code null} on {@code ClassCastException} and {@code
   * NullPointerException}.
   */
  static <V> V safeRemove(Map<?, V> map, Object key) {
    checkNotNull(map);
    try {
      return map.remove(key);
    } catch (ClassCastException | NullPointerException e) {
      return null;
    }
  }

  /** An admittedly inefficient implementation of {@link Map#containsKey}. */
  static boolean containsKeyImpl(Map<?, ?> map, @NullableDecl Object key) {
    return Iterators.contains(keyIterator(map.entrySet().iterator()), key);
  }

  /** An implementation of {@link Map#containsValue}. */
  static boolean containsValueImpl(Map<?, ?> map, @NullableDecl Object value) {
    return Iterators.contains(valueIterator(map.entrySet().iterator()), value);
  }

  /**
   * Implements {@code Collection.contains} safely for forwarding collections of map entries. If
   * {@code o} is an instance of {@code Entry}, it is wrapped using {@link #unmodifiableEntry} to
   * protect against a possible nefarious equals method.
   *
   * <p>Note that {@code c} is the backing (delegate) collection, rather than the forwarding
   * collection.
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
   * Implements {@code Collection.remove} safely for forwarding collections of map entries. If
   * {@code o} is an instance of {@code Entry}, it is wrapped using {@link #unmodifiableEntry} to
   * protect against a possible nefarious equals method.
   *
   * <p>Note that {@code c} is backing (delegate) collection, rather than the forwarding collection.
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

  /** An implementation of {@link Map#equals}. */
  static boolean equalsImpl(Map<?, ?> map, Object object) {
    if (map == object) {
      return true;
    } else if (object instanceof Map) {
      Map<?, ?> o = (Map<?, ?>) object;
      return map.entrySet().equals(o.entrySet());
    }
    return false;
  }

  /** An implementation of {@link Map#toString}. */
  static String toStringImpl(Map<?, ?> map) {
    StringBuilder sb = Collections2.newStringBuilderForCollection(map.size()).append('{');
    boolean first = true;
    for (Entry<?, ?> entry : map.entrySet()) {
      if (!first) {
        sb.append(", ");
      }
      first = false;
      sb.append(entry.getKey()).append('=').append(entry.getValue());
    }
    return sb.append('}').toString();
  }

  /** An implementation of {@link Map#putAll}. */
  static <K, V> void putAllImpl(Map<K, V> self, Map<? extends K, ? extends V> map) {
    for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
      self.put(entry.getKey(), entry.getValue());
    }
  }

  static class KeySet<K, V> extends Sets.ImprovedAbstractSet<K> {
    @Weak final Map<K, V> map;

    KeySet(Map<K, V> map) {
      this.map = checkNotNull(map);
    }

    Map<K, V> map() {
      return map;
    }

    @Override
    public Iterator<K> iterator() {
      return keyIterator(map().entrySet().iterator());
    }

    @Override
    public int size() {
      return map().size();
    }

    @Override
    public boolean isEmpty() {
      return map().isEmpty();
    }

    @Override
    public boolean contains(Object o) {
      return map().containsKey(o);
    }

    @Override
    public boolean remove(Object o) {
      if (contains(o)) {
        map().remove(o);
        return true;
      }
      return false;
    }

    @Override
    public void clear() {
      map().clear();
    }
  }

  @NullableDecl
  static <K> K keyOrNull(@NullableDecl Entry<K, ?> entry) {
    return (entry == null) ? null : entry.getKey();
  }

  @NullableDecl
  static <V> V valueOrNull(@NullableDecl Entry<?, V> entry) {
    return (entry == null) ? null : entry.getValue();
  }

  static class SortedKeySet<K, V> extends KeySet<K, V> implements SortedSet<K> {
    SortedKeySet(SortedMap<K, V> map) {
      super(map);
    }

    @Override
    SortedMap<K, V> map() {
      return (SortedMap<K, V>) super.map();
    }

    @Override
    public Comparator<? super K> comparator() {
      return map().comparator();
    }

    @Override
    public SortedSet<K> subSet(K fromElement, K toElement) {
      return new SortedKeySet<>(map().subMap(fromElement, toElement));
    }

    @Override
    public SortedSet<K> headSet(K toElement) {
      return new SortedKeySet<>(map().headMap(toElement));
    }

    @Override
    public SortedSet<K> tailSet(K fromElement) {
      return new SortedKeySet<>(map().tailMap(fromElement));
    }

    @Override
    public K first() {
      return map().firstKey();
    }

    @Override
    public K last() {
      return map().lastKey();
    }
  }

  @GwtIncompatible // NavigableMap
  static class NavigableKeySet<K, V> extends SortedKeySet<K, V> implements NavigableSet<K> {
    NavigableKeySet(NavigableMap<K, V> map) {
      super(map);
    }

    @Override
    NavigableMap<K, V> map() {
      return (NavigableMap<K, V>) map;
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
        K fromElement, boolean fromInclusive, K toElement, boolean toInclusive) {
      return map().subMap(fromElement, fromInclusive, toElement, toInclusive).navigableKeySet();
    }

    @Override
    public SortedSet<K> subSet(K fromElement, K toElement) {
      return subSet(fromElement, true, toElement, false);
    }

    @Override
    public NavigableSet<K> headSet(K toElement, boolean inclusive) {
      return map().headMap(toElement, inclusive).navigableKeySet();
    }

    @Override
    public SortedSet<K> headSet(K toElement) {
      return headSet(toElement, false);
    }

    @Override
    public NavigableSet<K> tailSet(K fromElement, boolean inclusive) {
      return map().tailMap(fromElement, inclusive).navigableKeySet();
    }

    @Override
    public SortedSet<K> tailSet(K fromElement) {
      return tailSet(fromElement, true);
    }
  }

  static class Values<K, V> extends AbstractCollection<V> {
    @Weak final Map<K, V> map;

    Values(Map<K, V> map) {
      this.map = checkNotNull(map);
    }

    final Map<K, V> map() {
      return map;
    }

    @Override
    public Iterator<V> iterator() {
      return valueIterator(map().entrySet().iterator());
    }

    @Override
    public boolean remove(Object o) {
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

    @Override
    public boolean removeAll(Collection<?> c) {
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

    @Override
    public boolean retainAll(Collection<?> c) {
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

    @Override
    public int size() {
      return map().size();
    }

    @Override
    public boolean isEmpty() {
      return map().isEmpty();
    }

    @Override
    public boolean contains(@NullableDecl Object o) {
      return map().containsValue(o);
    }

    @Override
    public void clear() {
      map().clear();
    }
  }

  abstract static class EntrySet<K, V> extends Sets.ImprovedAbstractSet<Entry<K, V>> {
    abstract Map<K, V> map();

    @Override
    public int size() {
      return map().size();
    }

    @Override
    public void clear() {
      map().clear();
    }

    @Override
    public boolean contains(Object o) {
      if (o instanceof Entry) {
        Entry<?, ?> entry = (Entry<?, ?>) o;
        Object key = entry.getKey();
        V value = Maps.safeGet(map(), key);
        return Objects.equal(value, entry.getValue()) && (value != null || map().containsKey(key));
      }
      return false;
    }

    @Override
    public boolean isEmpty() {
      return map().isEmpty();
    }

    @Override
    public boolean remove(Object o) {
      if (contains(o)) {
        Entry<?, ?> entry = (Entry<?, ?>) o;
        return map().keySet().remove(entry.getKey());
      }
      return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      try {
        return super.removeAll(checkNotNull(c));
      } catch (UnsupportedOperationException e) {
        // if the iterators don't support remove
        return Sets.removeAllImpl(this, c.iterator());
      }
    }

    @Override
    public boolean retainAll(Collection<?> c) {
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

  @GwtIncompatible // NavigableMap
  abstract static class DescendingMap<K, V> extends ForwardingMap<K, V>
      implements NavigableMap<K, V> {

    abstract NavigableMap<K, V> forward();

    @Override
    protected final Map<K, V> delegate() {
      return forward();
    }

    @MonotonicNonNullDecl private transient Comparator<? super K> comparator;

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

    @MonotonicNonNullDecl private transient Set<Entry<K, V>> entrySet;

    @Override
    public Set<Entry<K, V>> entrySet() {
      Set<Entry<K, V>> result = entrySet;
      return (result == null) ? entrySet = createEntrySet() : result;
    }

    abstract Iterator<Entry<K, V>> entryIterator();

    Set<Entry<K, V>> createEntrySet() {
      @WeakOuter
      class EntrySetImpl extends EntrySet<K, V> {
        @Override
        Map<K, V> map() {
          return DescendingMap.this;
        }

        @Override
        public Iterator<Entry<K, V>> iterator() {
          return entryIterator();
        }
      }
      return new EntrySetImpl();
    }

    @Override
    public Set<K> keySet() {
      return navigableKeySet();
    }

    @MonotonicNonNullDecl private transient NavigableSet<K> navigableKeySet;

    @Override
    public NavigableSet<K> navigableKeySet() {
      NavigableSet<K> result = navigableKeySet;
      return (result == null) ? navigableKeySet = new NavigableKeySet<>(this) : result;
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
      return forward().navigableKeySet();
    }

    @Override
    public NavigableMap<K, V> subMap(
        K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
      return forward().subMap(toKey, toInclusive, fromKey, fromInclusive).descendingMap();
    }

    @Override
    public SortedMap<K, V> subMap(K fromKey, K toKey) {
      return subMap(fromKey, true, toKey, false);
    }

    @Override
    public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
      return forward().tailMap(toKey, inclusive).descendingMap();
    }

    @Override
    public SortedMap<K, V> headMap(K toKey) {
      return headMap(toKey, false);
    }

    @Override
    public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
      return forward().headMap(fromKey, inclusive).descendingMap();
    }

    @Override
    public SortedMap<K, V> tailMap(K fromKey) {
      return tailMap(fromKey, true);
    }

    @Override
    public Collection<V> values() {
      return new Values<>(this);
    }

    @Override
    public String toString() {
      return standardToString();
    }
  }

  /** Returns a map from the ith element of list to i. */
  static <E> ImmutableMap<E, Integer> indexMap(Collection<E> list) {
    ImmutableMap.Builder<E, Integer> builder = new ImmutableMap.Builder<>(list.size());
    int i = 0;
    for (E e : list) {
      builder.put(e, i++);
    }
    return builder.build();
  }

  /**
   * Returns a view of the portion of {@code map} whose keys are contained by {@code range}.
   *
   * <p>This method delegates to the appropriate methods of {@link NavigableMap} (namely {@link
   * NavigableMap#subMap(Object, boolean, Object, boolean) subMap()}, {@link
   * NavigableMap#tailMap(Object, boolean) tailMap()}, and {@link NavigableMap#headMap(Object,
   * boolean) headMap()}) to actually construct the view. Consult these methods for a full
   * description of the returned view's behavior.
   *
   * <p><b>Warning:</b> {@code Range}s always represent a range of values using the values' natural
   * ordering. {@code NavigableMap} on the other hand can specify a custom ordering via a {@link
   * Comparator}, which can violate the natural ordering. Using this method (or in general using
   * {@code Range}) with unnaturally-ordered maps can lead to unexpected and undefined behavior.
   *
   * @since 20.0
   */
  @Beta
  @GwtIncompatible // NavigableMap
  public static <K extends Comparable<? super K>, V> NavigableMap<K, V> subMap(
      NavigableMap<K, V> map, Range<K> range) {
    if (map.comparator() != null
        && map.comparator() != Ordering.natural()
        && range.hasLowerBound()
        && range.hasUpperBound()) {
      checkArgument(
          map.comparator().compare(range.lowerEndpoint(), range.upperEndpoint()) <= 0,
          "map is using a custom comparator which is inconsistent with the natural ordering.");
    }
    if (range.hasLowerBound() && range.hasUpperBound()) {
      return map.subMap(
          range.lowerEndpoint(),
          range.lowerBoundType() == BoundType.CLOSED,
          range.upperEndpoint(),
          range.upperBoundType() == BoundType.CLOSED);
    } else if (range.hasLowerBound()) {
      return map.tailMap(range.lowerEndpoint(), range.lowerBoundType() == BoundType.CLOSED);
    } else if (range.hasUpperBound()) {
      return map.headMap(range.upperEndpoint(), range.upperBoundType() == BoundType.CLOSED);
    }
    return checkNotNull(map);
  }
}

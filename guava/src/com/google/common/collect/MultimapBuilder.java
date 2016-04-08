/*
 * Copyright (C) 2013 The Guava Authors
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
import static com.google.common.collect.CollectPreconditions.checkNonnegative;
import static com.google.common.collect.Maps.newLinkedHashMapWithExpectedSize;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Supplier;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A builder for a multimap implementation that allows customization of the backing map and value
 * collection implementations used in a particular multimap.
 *
 * <p>This can be used to easily configure multimap data structure implementations not provided
 * explicitly in {@code com.google.common.collect}, for example:
 *
 * <pre>   {@code
 *   ListMultimap<String, Integer> treeListMultimap =
 *       MultimapBuilder.treeKeys().arrayListValues().build();
 *   SetMultimap<Integer, MyEnum> hashEnumMultimap =
 *       MultimapBuilder.hashKeys().enumSetValues(MyEnum.class).build();}</pre>
 *
 * <p>{@code MultimapBuilder} instances are immutable.  Invoking a configuration method has no
 * effect on the receiving instance; you must store and use the new builder instance it returns
 * instead.
 *
 * <p>The generated multimaps are serializable if the key and value types are serializable,
 * unless stated otherwise in one of the configuration methods.
 *
 * @author Louis Wasserman
 * @param <K0> An upper bound on the key type of the generated multimap.
 * @param <V0> An upper bound on the value type of the generated multimap.
 * @since 16.0
 */
@Beta
@GwtCompatible
public abstract class MultimapBuilder<K0, V0> {
  /*
   * Leaving K and V as upper bounds rather than the actual key and value types allows type
   * parameters to be left implicit more often. CacheBuilder uses the same technique.
   */

  private MultimapBuilder() {}

  private static final int DEFAULT_EXPECTED_KEYS = 8;

  /**
   * Uses a {@link HashMap} to map keys to value collections.
   */
  public static MultimapBuilderWithKeys<Object> hashKeys() {
    return hashKeys(DEFAULT_EXPECTED_KEYS);
  }

  /**
   * Uses a {@link HashMap} to map keys to value collections, initialized to expect the specified
   * number of keys.
   *
   * @throws IllegalArgumentException if {@code expectedKeys < 0}
   */
  public static MultimapBuilderWithKeys<Object> hashKeys(final int expectedKeys) {
    checkNonnegative(expectedKeys, "expectedKeys");
    return new MultimapBuilderWithKeys<Object>() {
      @Override
      <K, V> Map<K, Collection<V>> createMap() {
        return Maps.newHashMapWithExpectedSize(expectedKeys);
      }
    };
  }

  /**
   * Uses a {@link LinkedHashMap} to map keys to value collections.
   *
   * <p>The collections returned by {@link Multimap#keySet()}, {@link Multimap#keys()}, and
   * {@link Multimap#asMap()} will iterate through the keys in the order that they were first added
   * to the multimap, save that if all values associated with a key are removed and then the key is
   * added back into the multimap, that key will come last in the key iteration order.
   */
  public static MultimapBuilderWithKeys<Object> linkedHashKeys() {
    return linkedHashKeys(DEFAULT_EXPECTED_KEYS);
  }

  /**
   * Uses a {@link LinkedHashMap} to map keys to value collections, initialized to expect the
   * specified number of keys.
   *
   * <p>The collections returned by {@link Multimap#keySet()}, {@link Multimap#keys()}, and
   * {@link Multimap#asMap()} will iterate through the keys in the order that they were first added
   * to the multimap, save that if all values associated with a key are removed and then the key is
   * added back into the multimap, that key will come last in the key iteration order.
   */
  public static MultimapBuilderWithKeys<Object> linkedHashKeys(final int expectedKeys) {
    checkNonnegative(expectedKeys, "expectedKeys");
    return new MultimapBuilderWithKeys<Object>() {
      @Override
      <K, V> Map<K, Collection<V>> createMap() {
        return newLinkedHashMapWithExpectedSize(expectedKeys);
      }
    };
  }

  /**
   * Uses a naturally-ordered {@link TreeMap} to map keys to value collections.
   *
   * <p>The collections returned by {@link Multimap#keySet()}, {@link Multimap#keys()}, and
   * {@link Multimap#asMap()} will iterate through the keys in sorted order.
   *
   * <p>For all multimaps generated by the resulting builder, the {@link Multimap#keySet()} can be
   * safely cast to a {@link java.util.SortedSet}, and the {@link Multimap#asMap()} can safely be
   * cast to a {@link java.util.SortedMap}.
   */
  @SuppressWarnings("rawtypes")
  public static MultimapBuilderWithKeys<Comparable> treeKeys() {
    return treeKeys(Ordering.natural());
  }

  /**
   * Uses a {@link TreeMap} sorted by the specified comparator to map keys to value collections.
   *
   * <p>The collections returned by {@link Multimap#keySet()}, {@link Multimap#keys()}, and
   * {@link Multimap#asMap()} will iterate through the keys in sorted order.
   *
   * <p>For all multimaps generated by the resulting builder, the {@link Multimap#keySet()} can be
   * safely cast to a {@link java.util.SortedSet}, and the {@link Multimap#asMap()} can safely be
   * cast to a {@link java.util.SortedMap}.
   *
   * <p>Multimaps generated by the resulting builder will not be serializable if {@code comparator}
   * is not serializable.
   */
  public static <K0> MultimapBuilderWithKeys<K0> treeKeys(final Comparator<K0> comparator) {
    checkNotNull(comparator);
    return new MultimapBuilderWithKeys<K0>() {
      @Override
      <K extends K0, V> Map<K, Collection<V>> createMap() {
        return new TreeMap<K, Collection<V>>(comparator);
      }
    };
  }

  /**
   * Uses an {@link EnumMap} to map keys to value collections.
   */
  public static <K0 extends Enum<K0>> MultimapBuilderWithKeys<K0> enumKeys(
      final Class<K0> keyClass) {
    checkNotNull(keyClass);
    return new MultimapBuilderWithKeys<K0>() {
      @SuppressWarnings("unchecked")
      @Override
      <K extends K0, V> Map<K, Collection<V>> createMap() {
        // K must actually be K0, since enums are effectively final
        // (their subclasses are inaccessible)
        return (Map<K, Collection<V>>) new EnumMap<K0, Collection<V>>(keyClass);
      }
    };
  }

  private static final class ArrayListSupplier<V> implements Supplier<List<V>>, Serializable {
    private final int expectedValuesPerKey;

    ArrayListSupplier(int expectedValuesPerKey) {
      this.expectedValuesPerKey = checkNonnegative(expectedValuesPerKey, "expectedValuesPerKey");
    }

    @Override
    public List<V> get() {
      return new ArrayList<V>(expectedValuesPerKey);
    }
  }

  private enum LinkedListSupplier implements Supplier<List<Object>> {
    INSTANCE;

    public static <V> Supplier<List<V>> instance() {
      // Each call generates a fresh LinkedList, which can serve as a List<V> for any V.
      @SuppressWarnings({"rawtypes", "unchecked"})
      Supplier<List<V>> result = (Supplier) INSTANCE;
      return result;
    }

    @Override
    public List<Object> get() {
      return new LinkedList<Object>();
    }
  }

  private static final class HashSetSupplier<V> implements Supplier<Set<V>>, Serializable {
    private final int expectedValuesPerKey;

    HashSetSupplier(int expectedValuesPerKey) {
      this.expectedValuesPerKey = checkNonnegative(expectedValuesPerKey, "expectedValuesPerKey");
    }

    @Override
    public Set<V> get() {
      return Sets.newHashSetWithExpectedSize(expectedValuesPerKey);
    }
  }

  private static final class LinkedHashSetSupplier<V> implements Supplier<Set<V>>, Serializable {
    private final int expectedValuesPerKey;

    LinkedHashSetSupplier(int expectedValuesPerKey) {
      this.expectedValuesPerKey = checkNonnegative(expectedValuesPerKey, "expectedValuesPerKey");
    }

    @Override
    public Set<V> get() {
      return Sets.newLinkedHashSetWithExpectedSize(expectedValuesPerKey);
    }
  }

  private static final class TreeSetSupplier<V> implements Supplier<SortedSet<V>>, Serializable {
    private final Comparator<? super V> comparator;

    TreeSetSupplier(Comparator<? super V> comparator) {
      this.comparator = checkNotNull(comparator);
    }

    @Override
    public SortedSet<V> get() {
      return new TreeSet<V>(comparator);
    }
  }

  private static final class EnumSetSupplier<V extends Enum<V>>
      implements Supplier<Set<V>>, Serializable {
    private final Class<V> clazz;

    EnumSetSupplier(Class<V> clazz) {
      this.clazz = checkNotNull(clazz);
    }

    @Override
    public Set<V> get() {
      return EnumSet.noneOf(clazz);
    }
  }

  /**
   * An intermediate stage in a {@link MultimapBuilder} in which the key-value collection map
   * implementation has been specified, but the value collection implementation has not.
   *
   * @param <K0> The upper bound on the key type of the generated multimap.
   */
  public abstract static class MultimapBuilderWithKeys<K0> {

    private static final int DEFAULT_EXPECTED_VALUES_PER_KEY = 2;

    MultimapBuilderWithKeys() {}

    abstract <K extends K0, V> Map<K, Collection<V>> createMap();

    /**
     * Uses an {@link ArrayList} to store value collections.
     */
    public ListMultimapBuilder<K0, Object> arrayListValues() {
      return arrayListValues(DEFAULT_EXPECTED_VALUES_PER_KEY);
    }

    /**
     * Uses an {@link ArrayList} to store value collections, initialized to expect the specified
     * number of values per key.
     *
     * @throws IllegalArgumentException if {@code expectedValuesPerKey < 0}
     */
    public ListMultimapBuilder<K0, Object> arrayListValues(final int expectedValuesPerKey) {
      checkNonnegative(expectedValuesPerKey, "expectedValuesPerKey");
      return new ListMultimapBuilder<K0, Object>() {
        @Override
        public <K extends K0, V> ListMultimap<K, V> build() {
          return Multimaps.newListMultimap(
              MultimapBuilderWithKeys.this.<K, V>createMap(),
              new ArrayListSupplier<V>(expectedValuesPerKey));
        }
      };
    }

    /**
     * Uses a {@link LinkedList} to store value collections.
     */
    public ListMultimapBuilder<K0, Object> linkedListValues() {
      return new ListMultimapBuilder<K0, Object>() {
        @Override
        public <K extends K0, V> ListMultimap<K, V> build() {
          return Multimaps.newListMultimap(
              MultimapBuilderWithKeys.this.<K, V>createMap(), LinkedListSupplier.<V>instance());
        }
      };
    }

    /**
     * Uses a {@link HashSet} to store value collections.
     */
    public SetMultimapBuilder<K0, Object> hashSetValues() {
      return hashSetValues(DEFAULT_EXPECTED_VALUES_PER_KEY);
    }

    /**
     * Uses a {@link HashSet} to store value collections, initialized to expect the specified number
     * of values per key.
     *
     * @throws IllegalArgumentException if {@code expectedValuesPerKey < 0}
     */
    public SetMultimapBuilder<K0, Object> hashSetValues(final int expectedValuesPerKey) {
      checkNonnegative(expectedValuesPerKey, "expectedValuesPerKey");
      return new SetMultimapBuilder<K0, Object>() {
        @Override
        public <K extends K0, V> SetMultimap<K, V> build() {
          return Multimaps.newSetMultimap(
              MultimapBuilderWithKeys.this.<K, V>createMap(),
              new HashSetSupplier<V>(expectedValuesPerKey));
        }
      };
    }

    /**
     * Uses a {@link LinkedHashSet} to store value collections.
     */
    public SetMultimapBuilder<K0, Object> linkedHashSetValues() {
      return linkedHashSetValues(DEFAULT_EXPECTED_VALUES_PER_KEY);
    }

    /**
     * Uses a {@link LinkedHashSet} to store value collections, initialized to expect the specified
     * number of values per key.
     *
     * @throws IllegalArgumentException if {@code expectedValuesPerKey < 0}
     */
    public SetMultimapBuilder<K0, Object> linkedHashSetValues(final int expectedValuesPerKey) {
      checkNonnegative(expectedValuesPerKey, "expectedValuesPerKey");
      return new SetMultimapBuilder<K0, Object>() {
        @Override
        public <K extends K0, V> SetMultimap<K, V> build() {
          return Multimaps.newSetMultimap(
              MultimapBuilderWithKeys.this.<K, V>createMap(),
              new LinkedHashSetSupplier<V>(expectedValuesPerKey));
        }
      };
    }

    /**
     * Uses a naturally-ordered {@link TreeSet} to store value collections.
     */
    @SuppressWarnings("rawtypes")
    public SortedSetMultimapBuilder<K0, Comparable> treeSetValues() {
      return treeSetValues(Ordering.natural());
    }

    /**
     * Uses a {@link TreeSet} ordered by the specified comparator to store value collections.
     *
     * <p>Multimaps generated by the resulting builder will not be serializable if
     * {@code comparator} is not serializable.
     */
    public <V0> SortedSetMultimapBuilder<K0, V0> treeSetValues(final Comparator<V0> comparator) {
      checkNotNull(comparator, "comparator");
      return new SortedSetMultimapBuilder<K0, V0>() {
        @Override
        public <K extends K0, V extends V0> SortedSetMultimap<K, V> build() {
          return Multimaps.newSortedSetMultimap(
              MultimapBuilderWithKeys.this.<K, V>createMap(), new TreeSetSupplier<V>(comparator));
        }
      };
    }

    /**
     * Uses an {@link EnumSet} to store value collections.
     */
    public <V0 extends Enum<V0>> SetMultimapBuilder<K0, V0> enumSetValues(
        final Class<V0> valueClass) {
      checkNotNull(valueClass, "valueClass");
      return new SetMultimapBuilder<K0, V0>() {
        @Override
        public <K extends K0, V extends V0> SetMultimap<K, V> build() {
          // V must actually be V0, since enums are effectively final
          // (their subclasses are inaccessible)
          @SuppressWarnings({"unchecked", "rawtypes"})
          Supplier<Set<V>> factory = (Supplier) new EnumSetSupplier<V0>(valueClass);
          return Multimaps.newSetMultimap(MultimapBuilderWithKeys.this.<K, V>createMap(), factory);
        }
      };
    }
  }

  /**
   * Returns a new, empty {@code Multimap} with the specified implementation.
   */
  public abstract <K extends K0, V extends V0> Multimap<K, V> build();

  /**
   * Returns a {@code Multimap} with the specified implementation, initialized with the entries of
   * {@code multimap}.
   */
  public <K extends K0, V extends V0> Multimap<K, V> build(
      Multimap<? extends K, ? extends V> multimap) {
    Multimap<K, V> result = build();
    result.putAll(multimap);
    return result;
  }

  /**
   * A specialization of {@link MultimapBuilder} that generates {@link ListMultimap} instances.
   */
  public abstract static class ListMultimapBuilder<K0, V0> extends MultimapBuilder<K0, V0> {
    ListMultimapBuilder() {}

    @Override
    public abstract <K extends K0, V extends V0> ListMultimap<K, V> build();

    @Override
    public <K extends K0, V extends V0> ListMultimap<K, V> build(
        Multimap<? extends K, ? extends V> multimap) {
      return (ListMultimap<K, V>) super.build(multimap);
    }
  }

  /**
   * A specialization of {@link MultimapBuilder} that generates {@link SetMultimap} instances.
   */
  public abstract static class SetMultimapBuilder<K0, V0> extends MultimapBuilder<K0, V0> {
    SetMultimapBuilder() {}

    @Override
    public abstract <K extends K0, V extends V0> SetMultimap<K, V> build();

    @Override
    public <K extends K0, V extends V0> SetMultimap<K, V> build(
        Multimap<? extends K, ? extends V> multimap) {
      return (SetMultimap<K, V>) super.build(multimap);
    }
  }

  /**
   * A specialization of {@link MultimapBuilder} that generates {@link SortedSetMultimap} instances.
   */
  public abstract static class SortedSetMultimapBuilder<K0, V0> extends SetMultimapBuilder<K0, V0> {
    SortedSetMultimapBuilder() {}

    @Override
    public abstract <K extends K0, V extends V0> SortedSetMultimap<K, V> build();

    @Override
    public <K extends K0, V extends V0> SortedSetMultimap<K, V> build(
        Multimap<? extends K, ? extends V> multimap) {
      return (SortedSetMultimap<K, V>) super.build(multimap);
    }
  }
}

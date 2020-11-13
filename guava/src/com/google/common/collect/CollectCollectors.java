/*
 * Copyright (C) 2016 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/** Collectors utilities for {@code common.collect} internals. */
@GwtCompatible
final class CollectCollectors {

  private static final Collector<Object, ?, ImmutableList<Object>> TO_IMMUTABLE_LIST =
      Collector.of(
          ImmutableList::builder,
          ImmutableList.Builder::add,
          ImmutableList.Builder::combine,
          ImmutableList.Builder::build);

  private static final Collector<Object, ?, ImmutableSet<Object>> TO_IMMUTABLE_SET =
      Collector.of(
          ImmutableSet::builder,
          ImmutableSet.Builder::add,
          ImmutableSet.Builder::combine,
          ImmutableSet.Builder::build);

  @GwtIncompatible
  private static final Collector<Range<Comparable<?>>, ?, ImmutableRangeSet<Comparable<?>>>
      TO_IMMUTABLE_RANGE_SET =
          Collector.of(
              ImmutableRangeSet::builder,
              ImmutableRangeSet.Builder::add,
              ImmutableRangeSet.Builder::combine,
              ImmutableRangeSet.Builder::build);

  // Lists

  @SuppressWarnings({"rawtypes", "unchecked"})
  static <E> Collector<E, ?, ImmutableList<E>> toImmutableList() {
    return (Collector) TO_IMMUTABLE_LIST;
  }

  // Sets

  @SuppressWarnings({"rawtypes", "unchecked"})
  static <E> Collector<E, ?, ImmutableSet<E>> toImmutableSet() {
    return (Collector) TO_IMMUTABLE_SET;
  }

  static <E> Collector<E, ?, ImmutableSortedSet<E>> toImmutableSortedSet(
      Comparator<? super E> comparator) {
    checkNotNull(comparator);
    return Collector.of(
        () -> new ImmutableSortedSet.Builder<E>(comparator),
        ImmutableSortedSet.Builder::add,
        ImmutableSortedSet.Builder::combine,
        ImmutableSortedSet.Builder::build);
  }

  @GwtIncompatible
  @SuppressWarnings({"rawtypes", "unchecked"})
  static <E extends Comparable<? super E>>
      Collector<Range<E>, ?, ImmutableRangeSet<E>> toImmutableRangeSet() {
    return (Collector) TO_IMMUTABLE_RANGE_SET;
  }

  // Multisets

  static <T, E> Collector<T, ?, ImmutableMultiset<E>> toImmutableMultiset(
      Function<? super T, ? extends E> elementFunction, ToIntFunction<? super T> countFunction) {
    checkNotNull(elementFunction);
    checkNotNull(countFunction);
    return Collector.of(
        LinkedHashMultiset::create,
        (multiset, t) ->
            multiset.add(checkNotNull(elementFunction.apply(t)), countFunction.applyAsInt(t)),
        (multiset1, multiset2) -> {
          multiset1.addAll(multiset2);
          return multiset1;
        },
        (Multiset<E> multiset) -> ImmutableMultiset.copyFromEntries(multiset.entrySet()));
  }

  static <T, E, M extends Multiset<E>> Collector<T, ?, M> toMultiset(
      Function<? super T, E> elementFunction,
      ToIntFunction<? super T> countFunction,
      Supplier<M> multisetSupplier) {
    checkNotNull(elementFunction);
    checkNotNull(countFunction);
    checkNotNull(multisetSupplier);
    return Collector.of(
        multisetSupplier,
        (ms, t) -> ms.add(elementFunction.apply(t), countFunction.applyAsInt(t)),
        (ms1, ms2) -> {
          ms1.addAll(ms2);
          return ms1;
        });
  }

  // Maps

  static <T, K, V> Collector<T, ?, ImmutableMap<K, V>> toImmutableMap(
      Function<? super T, ? extends K> keyFunction,
      Function<? super T, ? extends V> valueFunction) {
    checkNotNull(keyFunction);
    checkNotNull(valueFunction);
    return Collector.of(
        ImmutableMap.Builder<K, V>::new,
        (builder, input) -> builder.put(keyFunction.apply(input), valueFunction.apply(input)),
        ImmutableMap.Builder::combine,
        ImmutableMap.Builder::build);
  }

  public static <T, K, V> Collector<T, ?, ImmutableMap<K, V>> toImmutableMap(
      Function<? super T, ? extends K> keyFunction,
      Function<? super T, ? extends V> valueFunction,
      BinaryOperator<V> mergeFunction) {
    checkNotNull(keyFunction);
    checkNotNull(valueFunction);
    checkNotNull(mergeFunction);
    return Collectors.collectingAndThen(
        Collectors.toMap(keyFunction, valueFunction, mergeFunction, LinkedHashMap::new),
        ImmutableMap::copyOf);
  }

  static <T, K, V> Collector<T, ?, ImmutableSortedMap<K, V>> toImmutableSortedMap(
      Comparator<? super K> comparator,
      Function<? super T, ? extends K> keyFunction,
      Function<? super T, ? extends V> valueFunction) {
    checkNotNull(comparator);
    checkNotNull(keyFunction);
    checkNotNull(valueFunction);
    /*
     * We will always fail if there are duplicate keys, and the keys are always sorted by
     * the Comparator, so the entries can come in in arbitrary order -- so we report UNORDERED.
     */
    return Collector.of(
        () -> new ImmutableSortedMap.Builder<K, V>(comparator),
        (builder, input) -> builder.put(keyFunction.apply(input), valueFunction.apply(input)),
        ImmutableSortedMap.Builder::combine,
        ImmutableSortedMap.Builder::build,
        Collector.Characteristics.UNORDERED);
  }

  static <T, K, V> Collector<T, ?, ImmutableSortedMap<K, V>> toImmutableSortedMap(
      Comparator<? super K> comparator,
      Function<? super T, ? extends K> keyFunction,
      Function<? super T, ? extends V> valueFunction,
      BinaryOperator<V> mergeFunction) {
    checkNotNull(comparator);
    checkNotNull(keyFunction);
    checkNotNull(valueFunction);
    checkNotNull(mergeFunction);
    return Collectors.collectingAndThen(
        Collectors.toMap(
            keyFunction, valueFunction, mergeFunction, () -> new TreeMap<K, V>(comparator)),
        ImmutableSortedMap::copyOfSorted);
  }

  static <T, K, V> Collector<T, ?, ImmutableBiMap<K, V>> toImmutableBiMap(
      Function<? super T, ? extends K> keyFunction,
      Function<? super T, ? extends V> valueFunction) {
    checkNotNull(keyFunction);
    checkNotNull(valueFunction);
    return Collector.of(
        ImmutableBiMap.Builder<K, V>::new,
        (builder, input) -> builder.put(keyFunction.apply(input), valueFunction.apply(input)),
        ImmutableBiMap.Builder::combine,
        ImmutableBiMap.Builder::build,
        new Collector.Characteristics[0]);
  }

  @GwtIncompatible
  static <T, K extends Comparable<? super K>, V>
      Collector<T, ?, ImmutableRangeMap<K, V>> toImmutableRangeMap(
          Function<? super T, Range<K>> keyFunction,
          Function<? super T, ? extends V> valueFunction) {
    checkNotNull(keyFunction);
    checkNotNull(valueFunction);
    return Collector.of(
        ImmutableRangeMap::<K, V>builder,
        (builder, input) -> builder.put(keyFunction.apply(input), valueFunction.apply(input)),
        ImmutableRangeMap.Builder::combine,
        ImmutableRangeMap.Builder::build);
  }
}

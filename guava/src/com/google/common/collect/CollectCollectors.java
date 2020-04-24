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
import java.util.function.Function;
import java.util.stream.Collector;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Collectors utilities for {@code common.collect} internals. */
@GwtCompatible
final class CollectCollectors {
  static <T extends @Nullable Object, K extends @NonNull Object, V extends @NonNull Object>
      Collector<T, ?, ImmutableBiMap<K, V>> toImmutableBiMap(
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

  private static final Collector<?, ?, ?> TO_IMMUTABLE_LIST =
      Collector.of(
          ImmutableList::<Object>builder,
          ImmutableList.Builder::add,
          ImmutableList.Builder::combine,
          ImmutableList.Builder::build);

  @SuppressWarnings({"unchecked", "nullness"})
  static <E extends @NonNull Object> Collector<E, ?, ImmutableList<E>> toImmutableList() {
    return (Collector<E, ?, ImmutableList<E>>) TO_IMMUTABLE_LIST;
  }

  static <T extends @Nullable Object, K extends @NonNull Object, V extends @NonNull Object>
      Collector<T, ?, ImmutableMap<K, V>> toImmutableMap(
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

  private static final Collector<?, ?, ?> TO_IMMUTABLE_SET =
      Collector.of(
          ImmutableSet::<Object>builder,
          ImmutableSet.Builder::add,
          ImmutableSet.Builder::combine,
          ImmutableSet.Builder::build);

  @SuppressWarnings({"unchecked", "nullness"})
  static <E extends @NonNull Object> Collector<E, ?, ImmutableSet<E>> toImmutableSet() {
    return (Collector<E, ?, ImmutableSet<E>>) TO_IMMUTABLE_SET;
  }

  static <T extends @Nullable Object, K extends @NonNull Object, V extends @NonNull Object>
      Collector<T, ?, ImmutableSortedMap<K, V>> toImmutableSortedMap(
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

  static <E extends @NonNull Object> Collector<E, ?, ImmutableSortedSet<E>> toImmutableSortedSet(
      Comparator<? super E> comparator) {
    checkNotNull(comparator);
    return Collector.of(
        () -> new ImmutableSortedSet.Builder<E>(comparator),
        ImmutableSortedSet.Builder::add,
        ImmutableSortedSet.Builder::combine,
        ImmutableSortedSet.Builder::build);
  }

  @GwtIncompatible
  private static final Collector<Range<Comparable>, ?, ImmutableRangeSet<Comparable>>
      TO_IMMUTABLE_RANGE_SET =
          Collector.of(
              ImmutableRangeSet::<Comparable>builder,
              ImmutableRangeSet.Builder::add,
              ImmutableRangeSet.Builder::combine,
              ImmutableRangeSet.Builder::build);

  @GwtIncompatible
  static <E extends Comparable<? super E>>
      Collector<Range<E>, ?, ImmutableRangeSet<E>> toImmutableRangeSet() {
    return (Collector) TO_IMMUTABLE_RANGE_SET;
  }

  @GwtIncompatible
  static <T extends @Nullable Object, K extends Comparable<? super K>, V extends @NonNull Object>
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

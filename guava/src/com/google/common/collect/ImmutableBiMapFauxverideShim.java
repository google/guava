/*
 * Copyright (C) 2015 The Guava Authors
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

import com.google.common.annotations.GwtIncompatible;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * "Overrides" the {@link ImmutableMap} static methods that lack {@link ImmutableBiMap} equivalents
 * with deprecated, exception-throwing versions. See {@link ImmutableSortedSetFauxverideShim} for
 * details.
 *
 * @author Louis Wasserman
 */
@GwtIncompatible
abstract class ImmutableBiMapFauxverideShim<K extends @NonNull Object, V extends @NonNull Object>
    extends ImmutableMap<K, V> {
  /**
   * Not supported. Use {@link ImmutableBiMap#toImmutableBiMap} instead. This method exists only to
   * hide {@link ImmutableMap#toImmutableMap(Function, Function)} from consumers of {@code
   * ImmutableBiMap}.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Use {@link ImmutableBiMap#toImmutableBiMap}.
   */
  @Deprecated
  public static <T, K extends @NonNull Object, V extends @NonNull Object>
      Collector<T, ?, ImmutableMap<K, V>> toImmutableMap(
          Function<? super T, ? extends K> keyFunction,
          Function<? super T, ? extends V> valueFunction) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. This method does not make sense for {@code BiMap}. This method exists only to
   * hide {@link ImmutableMap#toImmutableMap(Function, Function, BinaryOperator)} from consumers of
   * {@code ImmutableBiMap}.
   *
   * @throws UnsupportedOperationException always
   * @deprecated
   */
  @Deprecated
  public static <T, K extends @NonNull Object, V extends @NonNull Object>
      Collector<T, ?, ImmutableMap<K, V>> toImmutableMap(
          Function<? super T, ? extends K> keyFunction,
          Function<? super T, ? extends V> valueFunction,
          BinaryOperator<V> mergeFunction) {
    throw new UnsupportedOperationException();
  }
}

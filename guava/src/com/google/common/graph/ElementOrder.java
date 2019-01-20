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

package com.google.common.graph;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.errorprone.annotations.Immutable;
import java.util.Comparator;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Used to represent the order of elements in a data structure that supports different options for
 * iteration order guarantees.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * MutableGraph<Integer> graph =
 *     GraphBuilder.directed().nodeOrder(ElementOrder.<Integer>natural()).build();
 * }</pre>
 *
 * @author Joshua O'Madadhain
 * @since 20.0
 */
@Beta
@Immutable
public final class ElementOrder<T> {
  private final Type type;

  @SuppressWarnings("Immutable") // Hopefully the comparator provided is immutable!
  private final @Nullable Comparator<T> comparator;

  /**
   * The type of ordering that this object specifies.
   *
   * <ul>
   *   <li>UNORDERED: no order is guaranteed.
   *   <li>INSERTION: insertion ordering is guaranteed.
   *   <li>SORTED: ordering according to a supplied comparator is guaranteed.
   * </ul>
   */
  public enum Type {
    UNORDERED,
    INSERTION,
    SORTED
  }

  private ElementOrder(Type type, @Nullable Comparator<T> comparator) {
    this.type = checkNotNull(type);
    this.comparator = comparator;
    checkState((type == Type.SORTED) == (comparator != null));
  }

  /** Returns an instance which specifies that no ordering is guaranteed. */
  public static <S> ElementOrder<S> unordered() {
    return new ElementOrder<S>(Type.UNORDERED, null);
  }

  /** Returns an instance which specifies that insertion ordering is guaranteed. */
  public static <S> ElementOrder<S> insertion() {
    return new ElementOrder<S>(Type.INSERTION, null);
  }

  /**
   * Returns an instance which specifies that the natural ordering of the elements is guaranteed.
   */
  public static <S extends Comparable<? super S>> ElementOrder<S> natural() {
    return new ElementOrder<S>(Type.SORTED, Ordering.<S>natural());
  }

  /**
   * Returns an instance which specifies that the ordering of the elements is guaranteed to be
   * determined by {@code comparator}.
   */
  public static <S> ElementOrder<S> sorted(Comparator<S> comparator) {
    return new ElementOrder<S>(Type.SORTED, comparator);
  }

  /** Returns the type of ordering used. */
  public Type type() {
    return type;
  }

  /**
   * Returns the {@link Comparator} used.
   *
   * @throws UnsupportedOperationException if comparator is not defined
   */
  public Comparator<T> comparator() {
    if (comparator != null) {
      return comparator;
    }
    throw new UnsupportedOperationException("This ordering does not define a comparator.");
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof ElementOrder)) {
      return false;
    }

    ElementOrder<?> other = (ElementOrder<?>) obj;
    return (type == other.type) && Objects.equal(comparator, other.comparator);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(type, comparator);
  }

  @Override
  public String toString() {
    ToStringHelper helper = MoreObjects.toStringHelper(this).add("type", type);
    if (comparator != null) {
      helper.add("comparator", comparator);
    }
    return helper.toString();
  }

  /** Returns an empty mutable map whose keys will respect this {@link ElementOrder}. */
  <K extends T, V> Map<K, V> createMap(int expectedSize) {
    switch (type) {
      case UNORDERED:
        return Maps.newHashMapWithExpectedSize(expectedSize);
      case INSERTION:
        return Maps.newLinkedHashMapWithExpectedSize(expectedSize);
      case SORTED:
        return Maps.newTreeMap(comparator());
      default:
        throw new AssertionError();
    }
  }

  @SuppressWarnings("unchecked")
  <T1 extends T> ElementOrder<T1> cast() {
    return (ElementOrder<T1>) this;
  }
}

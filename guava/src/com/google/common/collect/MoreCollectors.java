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
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collector;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Collectors not present in {@code java.util.stream.Collectors} that are not otherwise associated
 * with a {@code com.google.common} type.
 *
 * @author Louis Wasserman
 * @since 21.0
 */
@GwtCompatible
public final class MoreCollectors {

  /*
   * TODO(lowasser): figure out if we can convert this to a concurrent AtomicReference-based
   * collector without breaking j2cl?
   */
  private static final Collector<Object, ?, Optional<Object>> TO_OPTIONAL =
      Collector.of(
          ToOptionalState::new,
          ToOptionalState::add,
          ToOptionalState::combine,
          ToOptionalState::getOptional,
          Collector.Characteristics.UNORDERED);

  /**
   * A collector that converts a stream of zero or one elements to an {@code Optional}. The returned
   * collector throws an {@code IllegalArgumentException} if the stream consists of two or more
   * elements, and a {@code NullPointerException} if the stream consists of exactly one element,
   * which is null.
   */
  @SuppressWarnings("unchecked")
  public static <T> Collector<T, ?, Optional<T>> toOptional() {
    return (Collector) TO_OPTIONAL;
  }

  private static final Object NULL_PLACEHOLDER = new Object();

  private static final Collector<Object, ?, Object> ONLY_ELEMENT =
      Collector.of(
          ToOptionalState::new,
          (state, o) -> state.add((o == null) ? NULL_PLACEHOLDER : o),
          ToOptionalState::combine,
          state -> {
            Object result = state.getElement();
            return (result == NULL_PLACEHOLDER) ? null : result;
          },
          Collector.Characteristics.UNORDERED);

  /**
   * A collector that takes a stream containing exactly one element and returns that element. The
   * returned collector throws an {@code IllegalArgumentException} if the stream consists of two or
   * more elements, and a {@code NoSuchElementException} if the stream is empty.
   */
  @SuppressWarnings("unchecked")
  public static <T> Collector<T, ?, T> onlyElement() {
    return (Collector) ONLY_ELEMENT;
  }

  /**
   * This atrocity is here to let us report several of the elements in the stream if there were more
   * than one, not just two.
   */
  private static final class ToOptionalState {
    static final int MAX_EXTRAS = 4;

    @Nullable Object element;
    @Nullable List<Object> extras;

    ToOptionalState() {
      element = null;
      extras = null;
    }

    IllegalArgumentException multiples(boolean overflow) {
      StringBuilder sb =
          new StringBuilder().append("expected one element but was: <").append(element);
      for (Object o : extras) {
        sb.append(", ").append(o);
      }
      if (overflow) {
        sb.append(", ...");
      }
      sb.append('>');
      throw new IllegalArgumentException(sb.toString());
    }

    void add(Object o) {
      checkNotNull(o);
      if (element == null) {
        this.element = o;
      } else if (extras == null) {
        extras = new ArrayList<>(MAX_EXTRAS);
        extras.add(o);
      } else if (extras.size() < MAX_EXTRAS) {
        extras.add(o);
      } else {
        throw multiples(true);
      }
    }

    ToOptionalState combine(ToOptionalState other) {
      if (element == null) {
        return other;
      } else if (other.element == null) {
        return this;
      } else {
        if (extras == null) {
          extras = new ArrayList<>();
        }
        extras.add(other.element);
        if (other.extras != null) {
          this.extras.addAll(other.extras);
        }
        if (extras.size() > MAX_EXTRAS) {
          extras.subList(MAX_EXTRAS, extras.size()).clear();
          throw multiples(true);
        }
        return this;
      }
    }

    Optional<Object> getOptional() {
      if (extras == null) {
        return Optional.ofNullable(element);
      } else {
        throw multiples(false);
      }
    }

    Object getElement() {
      if (element == null) {
        throw new NoSuchElementException();
      } else if (extras == null) {
        return element;
      } else {
        throw multiples(false);
      }
    }
  }

  private MoreCollectors() {}
}

/*
 * Copyright (C) 2009 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS-IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.base;

import com.google.common.annotations.GwtCompatible;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Contains dummy collection implementations to convince GWT that part of serializing a collection
 * is serializing its elements.
 *
 * <p>See {@linkplain com.google.common.collect.GwtSerializationDependencies the
 * com.google.common.collect version} for more details.
 *
 * @author Chris Povirk
 */
@GwtCompatible
final class GwtSerializationDependencies {
  private GwtSerializationDependencies() {}

  static final class OptionalDependencies<T> extends Optional<T> {
    @Nullable T value;

    OptionalDependencies() {
      super();
    }

    @Override
    public boolean isPresent() {
      throw new AssertionError();
    }

    @Override
    public T get() {
      throw new AssertionError();
    }

    @Override
    public T or(T defaultValue) {
      throw new AssertionError();
    }

    @Override
    public Optional<T> or(Optional<? extends T> secondChoice) {
      throw new AssertionError();
    }

    @Override
    public T or(Supplier<? extends T> supplier) {
      throw new AssertionError();
    }

    @Override
    public T orNull() {
      throw new AssertionError();
    }

    @Override
    public Set<T> asSet() {
      throw new AssertionError();
    }

    @Override
    public <V> Optional<V> transform(Function<? super T, V> function) {
      throw new AssertionError();
    }

    @Override
    public boolean equals(@Nullable Object object) {
      throw new AssertionError();
    }

    @Override
    public int hashCode() {
      throw new AssertionError();
    }

    @Override
    public String toString() {
      throw new AssertionError();
    }
  }
}

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

import static com.google.common.collect.Maps.immutableEntry;

import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.TestMapGenerator;
import java.io.Serializable;
import java.util.List;
import java.util.Map.Entry;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

/**
 * Unit test for {@link ImmutableClassToInstanceMap}.
 *
 * @author Kevin Bourrillion
 */
@NullUnmarked
final class ClassToInstanceMapTesting {
  abstract static class TestClassToInstanceMapGenerator
      implements TestMapGenerator<Class<?>, Impl> {

    @Override
    public Class<?>[] createKeyArray(int length) {
      return new Class<?>[length];
    }

    @Override
    public Impl[] createValueArray(int length) {
      return new Impl[length];
    }

    @Override
    public SampleElements<Entry<Class<?>, Impl>> samples() {
      return new SampleElements<>(
          immutableEntry(One.class, new Impl(1)),
          immutableEntry(Two.class, new Impl(2)),
          immutableEntry(Three.class, new Impl(3)),
          immutableEntry(Four.class, new Impl(4)),
          immutableEntry(Five.class, new Impl(5)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Entry<Class<?>, Impl>[] createArray(int length) {
      return (Entry<Class<?>, Impl>[]) new Entry<?, ?>[length];
    }

    @Override
    public Iterable<Entry<Class<?>, Impl>> order(List<Entry<Class<?>, Impl>> insertionOrder) {
      return insertionOrder;
    }
  }

  private interface One {}

  private interface Two {}

  private interface Three {}

  private interface Four {}

  private interface Five {}

  static final class Impl implements One, Two, Three, Four, Five, Serializable {
    final int value;

    Impl(int value) {
      this.value = value;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      return obj instanceof Impl && value == ((Impl) obj).value;
    }

    @Override
    public int hashCode() {
      return value;
    }

    @Override
    public String toString() {
      return Integer.toString(value);
    }
  }

  private ClassToInstanceMapTesting() {}
}

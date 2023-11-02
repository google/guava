/*
 * Copyright (C) 2008 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;
import java.io.Serializable;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An immutable {@code Map.Entry}, used both by {@link
 * com.google.common.collect.Maps#immutableEntry(Object, Object)} and by other parts of {@code
 * common.collect} as a superclass.
 */
@GwtCompatible(serializable = true)
@ElementTypesAreNonnullByDefault
class ImmutableEntry<K extends @Nullable Object, V extends @Nullable Object>
    extends AbstractMapEntry<K, V> implements Serializable {
  @ParametricNullness final K key;
  @ParametricNullness final V value;

  ImmutableEntry(@ParametricNullness K key, @ParametricNullness V value) {
    this.key = key;
    this.value = value;
  }

  @Override
  @ParametricNullness
  public final K getKey() {
    return key;
  }

  @Override
  @ParametricNullness
  public final V getValue() {
    return value;
  }

  @Override
  @ParametricNullness
  public final V setValue(@ParametricNullness V value) {
    throw new UnsupportedOperationException();
  }

  private static final long serialVersionUID = 0;
}

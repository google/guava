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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * GWT emulation of {@link SingletonImmutableBiMap}.
 *
 * @author Hayward Chan
 */
@ElementTypesAreNonnullByDefault
final class SingletonImmutableBiMap<K, V> extends ImmutableBiMap<K, V> {

  // These references are used both by the custom field serializer, and by the
  // GWT compiler to infer the keys and values of the map that needs to be
  // serialized.
  //
  // Although they are non-final, they are package private and the reference is
  // never modified after a map is constructed.
  K singleKey;
  V singleValue;

  @Nullable transient SingletonImmutableBiMap<V, K> inverse;

  SingletonImmutableBiMap(K key, V value) {
    super(Collections.singletonMap(checkNotNull(key), checkNotNull(value)));
    this.singleKey = key;
    this.singleValue = value;
  }

  private SingletonImmutableBiMap(K key, V value, SingletonImmutableBiMap<V, K> inverse) {
    super(Collections.singletonMap(checkNotNull(key), checkNotNull(value)));
    this.singleKey = key;
    this.singleValue = value;
    this.inverse = inverse;
  }

  @Override
  public ImmutableBiMap<V, K> inverse() {
    ImmutableBiMap<V, K> result = inverse;
    if (result == null) {
      return inverse = new SingletonImmutableBiMap<V, K>(singleValue, singleKey, this);
    } else {
      return result;
    }
  }

  @Override
  public ImmutableSet<V> values() {
    return ImmutableSet.of(singleValue);
  }
}

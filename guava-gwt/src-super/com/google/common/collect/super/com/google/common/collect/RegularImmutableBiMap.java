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

import java.util.HashMap;

/**
 * GWT emulation of {@link RegularImmutableBiMap}.
 *
 * @author Jared Levy
 * @author Hayward Chan
 */
@ElementTypesAreNonnullByDefault
@SuppressWarnings("serial")
class RegularImmutableBiMap<K, V> extends ImmutableBiMap<K, V> {
  static final RegularImmutableBiMap<Object, Object> EMPTY =
      new RegularImmutableBiMap<Object, Object>();

  // This reference is used both by the GWT compiler to infer the elements
  // of the lists that needs to be serialized.
  private ImmutableBiMap<V, K> inverse;

  RegularImmutableBiMap() {
    super(new RegularImmutableMap<K, V>(new HashMap<K, V>()));
    this.inverse = (ImmutableBiMap<V, K>) this;
  }

  RegularImmutableBiMap(ImmutableMap<K, V> delegate) {
    super(delegate);

    ImmutableMap.Builder<V, K> builder = ImmutableMap.builder();
    for (Entry<K, V> entry : delegate.entrySet()) {
      builder.put(entry.getValue(), entry.getKey());
    }
    ImmutableMap<V, K> backwardMap = builder.build();
    this.inverse = new RegularImmutableBiMap<V, K>(backwardMap, this);
  }

  RegularImmutableBiMap(ImmutableMap<K, V> delegate, ImmutableBiMap<V, K> inverse) {
    super(delegate);
    this.inverse = inverse;
  }

  @Override
  public ImmutableBiMap<V, K> inverse() {
    return inverse;
  }
}

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

import java.util.Map;
import java.util.Map.Entry;

/**
 * GWT emulation of {@link ImmutableEnumMap}. The type parameter is not bounded by {@code Enum<E>}
 * to avoid code-size bloat.
 *
 * @author Hayward Chan
 */
@ElementTypesAreNonnullByDefault
final class ImmutableEnumMap<K, V> extends ForwardingImmutableMap<K, V> {
  static <K, V> ImmutableMap<K, V> asImmutable(Map<K, V> map) {
    for (Entry<K, V> entry : checkNotNull(map).entrySet()) {
      checkNotNull(entry.getKey());
      checkNotNull(entry.getValue());
    }
    return new ImmutableEnumMap<K, V>(map);
  }

  private ImmutableEnumMap(Map<? extends K, ? extends V> delegate) {
    super(delegate);
  }
}

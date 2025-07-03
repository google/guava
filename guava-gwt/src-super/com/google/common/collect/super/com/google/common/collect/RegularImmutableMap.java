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

import java.util.Map;

/**
 * GWt emulation of {@link RegularImmutableMap}.
 *
 * @author Hayward Chan
 */
final class RegularImmutableMap<K, V> extends ForwardingImmutableMap<K, V> {

  static final ImmutableMap<Object, Object> EMPTY = new RegularImmutableMap<Object, Object>();

  RegularImmutableMap(Map<? extends K, ? extends V> delegate) {
    super(delegate);
  }

  RegularImmutableMap(Entry<? extends K, ? extends V>... entries) {
    this(/* throwIfDuplicateKeys= */ true, entries);
  }

  RegularImmutableMap(boolean throwIfDuplicateKeys, Entry<? extends K, ? extends V>[] entries) {
    super(throwIfDuplicateKeys, entries);
  }
}

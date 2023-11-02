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
import com.google.common.base.Joiner;
import com.google.common.collect.testing.MapInterfaceTest;
import com.google.common.collect.testing.MinimalSet;
import java.util.Map;
import java.util.Map.Entry;

@GwtCompatible
abstract class AbstractImmutableMapMapInterfaceTest<K, V> extends MapInterfaceTest<K, V> {
  AbstractImmutableMapMapInterfaceTest() {
    super(false, false, false, false, false);
  }

  @Override
  protected Map<K, V> makeEmptyMap() {
    throw new UnsupportedOperationException();
  }

  private static final Joiner JOINER = Joiner.on(", ");

  @Override
  protected final void assertMoreInvariants(Map<K, V> map) {
    // TODO: can these be moved to MapInterfaceTest?
    for (Entry<K, V> entry : map.entrySet()) {
      assertEquals(entry.getKey() + "=" + entry.getValue(), entry.toString());
    }

    assertEquals("{" + JOINER.join(map.entrySet()) + "}", map.toString());
    assertEquals("[" + JOINER.join(map.entrySet()) + "]", map.entrySet().toString());
    assertEquals("[" + JOINER.join(map.keySet()) + "]", map.keySet().toString());
    assertEquals("[" + JOINER.join(map.values()) + "]", map.values().toString());

    assertEquals(MinimalSet.from(map.entrySet()), map.entrySet());
    assertEquals(Sets.newHashSet(map.keySet()), map.keySet());
  }
}

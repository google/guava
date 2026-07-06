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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Joiner;
import com.google.common.collect.testing.SortedMapInterfaceTest;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import org.jspecify.annotations.NullUnmarked;

@GwtCompatible
@NullUnmarked
public abstract class AbstractImmutableSortedMapMapInterfaceTest<K, V>
    extends SortedMapInterfaceTest<K, V> {
  protected AbstractImmutableSortedMapMapInterfaceTest() {
    super(false, false, false, false, false);
  }

  @Override
  protected SortedMap<K, V> makeEmptyMap() {
    throw new UnsupportedOperationException();
  }

  private static final Joiner JOINER = Joiner.on(", ");

  @Override
  protected void assertMoreInvariants(Map<K, V> map) {
    // TODO: can these be moved to MapInterfaceTest?
    for (Entry<K, V> entry : map.entrySet()) {
      assertThat(entry.toString()).isEqualTo(entry.getKey() + "=" + entry.getValue());
    }

    assertThat(map.toString()).isEqualTo("{" + JOINER.join(map.entrySet()) + "}");
    assertThat(map.entrySet().toString()).isEqualTo("[" + JOINER.join(map.entrySet()) + "]");
    assertThat(map.keySet().toString()).isEqualTo("[" + JOINER.join(map.keySet()) + "]");
    assertThat(map.values().toString()).isEqualTo("[" + JOINER.join(map.values()) + "]");

    assertEquals(new HashSet<>(map.entrySet()), map.entrySet());
    assertEquals(new HashSet<>(map.keySet()), map.keySet());
  }
}

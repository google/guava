/*
 * Copyright (C) 2026 The Guava Authors
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

import static java.lang.Math.log;

import com.google.common.annotations.GwtIncompatible;
import org.jspecify.annotations.NullUnmarked;

@GwtIncompatible
@NullUnmarked
public class LinkedHashMultimapFloodingTest
    extends AbstractHashFloodingTest<SetMultimap<Object, Object>> {
  public LinkedHashMultimapFloodingTest() {
    super(
        ImmutableList.of(
            keys -> {
              SetMultimap<Object, Object> multimap = LinkedHashMultimap.create();
              for (Object key : keys) {
                multimap.put(key, new Object());
              }
              return multimap;
            },
            values -> {
              SetMultimap<Object, Object> multimap = LinkedHashMultimap.create();
              for (Object value : values) {
                multimap.put("key", value);
              }
              return multimap;
            }),
        n -> n * log(n),
        ImmutableList.of(
            QueryOp.create(
                "SetMultimap.containsKey",
                (multimap, key) -> {
                  boolean unused = multimap.containsKey(key);
                },
                Math::log),
            QueryOp.create(
                "SetMultimap.get.contains",
                (multimap, value) -> {
                  boolean unused = multimap.get("key").contains(value);
                },
                Math::log)));
  }
}

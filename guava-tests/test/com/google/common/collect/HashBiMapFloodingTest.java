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
import java.util.Iterator;
import java.util.Map.Entry;
import org.jspecify.annotations.NullUnmarked;

@GwtIncompatible
@NullUnmarked
public class HashBiMapFloodingTest extends AbstractHashFloodingTest<BiMap<Object, Object>> {
  public HashBiMapFloodingTest() {
    super(
        ImmutableList.of(
            keys -> {
              BiMap<Object, Object> biMap = HashBiMap.create();
              for (Object key : keys) {
                biMap.put(key, new Object());
              }
              return biMap;
            },
            values -> {
              BiMap<Object, Object> biMap = HashBiMap.create();
              for (Object value : values) {
                biMap.put(new Object(), value);
              }
              return biMap;
            },
            keysAndValues -> {
              BiMap<Object, Object> biMap = HashBiMap.create();
              for (Object keyAndValue : keysAndValues) {
                biMap.put(keyAndValue, keyAndValue);
              }
              return biMap;
            },
            values -> {
              BiMap<Object, Object> biMap = HashBiMap.create();
              for (int i = 0; i < values.size(); i++) {
                biMap.put(new Object(), new Object());
              }
              Iterator<?> valuesIterator = values.iterator();
              biMap.replaceAll((k, v) -> valuesIterator.next());
              return biMap;
            },
            keys -> {
              BiMap<Object, Object> biMap = HashBiMap.create();
              for (int i = 0; i < keys.size(); i++) {
                biMap.put(new Object(), new Object());
              }
              Iterator<?> keysIterator = keys.iterator();
              biMap.inverse().replaceAll((v, k) -> keysIterator.next());
              return biMap;
            },
            values -> {
              BiMap<Object, Object> biMap = HashBiMap.create();
              for (int i = 0; i < values.size(); i++) {
                biMap.put(new Object(), new Object());
              }
              Iterator<?> valuesIterator = values.iterator();
              for (Entry<Object, Object> entry : biMap.entrySet()) {
                entry.setValue(valuesIterator.next());
              }
              return biMap;
            },
            keys -> {
              BiMap<Object, Object> biMap = HashBiMap.create();
              for (int i = 0; i < keys.size(); i++) {
                biMap.put(new Object(), new Object());
              }
              Iterator<?> keysIterator = keys.iterator();
              for (Entry<Object, Object> entry : biMap.inverse().entrySet()) {
                entry.setValue(keysIterator.next());
              }
              return biMap;
            }),
        n -> n * log(n),
        ImmutableList.of(
            QueryOp.create(
                "BiMap.get",
                (biMap, key) -> {
                  Object unused = biMap.get(key);
                },
                Math::log),
            QueryOp.create(
                "BiMap.inverse.get",
                (biMap, value) -> {
                  Object unused = biMap.inverse().get(value);
                },
                Math::log)));
  }
}

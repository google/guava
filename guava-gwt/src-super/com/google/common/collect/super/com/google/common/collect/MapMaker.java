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

import static com.google.common.base.Preconditions.checkArgument;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * MapMaker emulation.
 *
 * @author Charles Fry
 */
public final class MapMaker {
  private int initialCapacity = 16;

  public MapMaker() {}

  public MapMaker initialCapacity(int initialCapacity) {
    if (initialCapacity < 0) {
      throw new IllegalArgumentException();
    }
    this.initialCapacity = initialCapacity;
    return this;
  }

  public MapMaker concurrencyLevel(int concurrencyLevel) {
    checkArgument(
        concurrencyLevel >= 1, "concurrency level (%s) must be at least 1", concurrencyLevel);
    // GWT technically only supports concurrencyLevel == 1, but we silently
    // ignore other positive values.
    return this;
  }

  public <K, V> ConcurrentMap<K, V> makeMap() {
    return new ConcurrentHashMap<K, V>(initialCapacity);
  }
}

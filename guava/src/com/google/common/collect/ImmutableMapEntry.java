/*
 * Copyright (C) 2013 The Guava Authors
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

import static com.google.common.collect.CollectPreconditions.checkEntryNotNull;

import com.google.common.annotations.GwtIncompatible;

import javax.annotation.Nullable;

/**
 * Implementation of {@code Map.Entry} for {@link ImmutableMap} that adds extra methods to traverse
 * hash buckets for the key and the value. This allows reuse in {@link RegularImmutableMap} and
 * {@link RegularImmutableBiMap}, which don't have to recopy the entries created by their
 * {@code Builder} implementations.
 *
 * @author Louis Wasserman
 */
@GwtIncompatible("unnecessary")
abstract class ImmutableMapEntry<K, V> extends ImmutableEntry<K, V> {
  ImmutableMapEntry(K key, V value) {
    super(key, value);
    checkEntryNotNull(key, value);
  }

  ImmutableMapEntry(ImmutableMapEntry<K, V> contents) {
    super(contents.getKey(), contents.getValue());
    // null check would be redundant
  }

  @Nullable
  abstract ImmutableMapEntry<K, V> getNextInKeyBucket();

  @Nullable
  abstract ImmutableMapEntry<K, V> getNextInValueBucket();

  static final class TerminalEntry<K, V> extends ImmutableMapEntry<K, V> {
    TerminalEntry(ImmutableMapEntry<K, V> contents) {
      super(contents);
    }

    TerminalEntry(K key, V value) {
      super(key, value);
    }

    @Override
    @Nullable
    ImmutableMapEntry<K, V> getNextInKeyBucket() {
      return null;
    }

    @Override
    @Nullable
    ImmutableMapEntry<K, V> getNextInValueBucket() {
      return null;
    }
  }
}

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

import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/**
 * Key-set view reading even ({@code offset == 0}) or odd ({@code offset == 1}) indices of a flat
 * {@code alternatingKeysAndValues} array.
 */
@GwtCompatible
final class FlatKeySet<K> extends IndexedImmutableSet<K> {
  private final transient ImmutableMap<K, ?> map;
  private final transient Object[] alternatingKeysAndValues;
  private final transient int offset;

  FlatKeySet(ImmutableMap<K, ?> map, Object[] alternatingKeysAndValues, int offset) {
    this.map = map;
    this.alternatingKeysAndValues = alternatingKeysAndValues;
    this.offset = offset;
  }

  @Override
  @SuppressWarnings("unchecked") // alternatingKeysAndValues stores K at (2 * i + offset)
  K get(int index) {
    // Bounds check before shifting to prevent index << 1 wraparound (e.g. 1 << 30).
    checkElementIndex(index, size());
    return (K) alternatingKeysAndValues[(index << 1) + offset];
  }

  @Override
  @SuppressWarnings("unchecked") // alternatingKeysAndValues stores K at (2 * i + offset)
  public void forEach(Consumer<? super K> action) {
    checkNotNull(action);
    Object[] kv = this.alternatingKeysAndValues;
    for (int i = offset; i < kv.length; i += 2) {
      action.accept((K) kv[i]);
    }
  }

  @Override
  @GwtIncompatible // not used in GWT
  int copyIntoArray(@Nullable Object[] dst, int dstOffset) {
    Object[] kv = this.alternatingKeysAndValues;
    for (int i = offset; i < kv.length; i += 2) {
      dst[dstOffset++] = kv[i];
    }
    return dstOffset;
  }

  @Override
  public boolean contains(@Nullable Object object) {
    return map.containsKey(object);
  }

  @Override
  boolean isPartialView() {
    return true;
  }

  @Override
  public int size() {
    return alternatingKeysAndValues.length >>> 1;
  }

  // redeclare to help optimizers with b/310253115
  @SuppressWarnings("RedundantOverride")
  @Override
  @J2ktIncompatible
  @GwtIncompatible
    Object writeReplace() {
    return super.writeReplace();
  }
}

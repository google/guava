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
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/**
 * Entry-set view backed directly by a flat {@code alternatingKeysAndValues} array ({@code keyOffset
 * == 0} for forward maps, {@code keyOffset == 1} for inverse bimaps).
 *
 * <p>Each {@link Entry} is materialized on demand as a {@link SimpleImmutableEntry}; in typical
 * non-escaping loops HotSpot C2 scalar-replaces these short-lived wrappers.
 */
@GwtCompatible
final class FlatEntrySet<K, V> extends ImmutableMapEntrySet<K, V> {
  private static final int SPLITERATOR_CHARACTERISTICS =
      Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.NONNULL | Spliterator.IMMUTABLE;

  private final transient ImmutableMap<K, V> map;
  private final transient Object[] alternatingKeysAndValues;
  private final transient int keyOffset;

  FlatEntrySet(ImmutableMap<K, V> map, Object[] alternatingKeysAndValues, int keyOffset) {
    this.map = map;
    this.alternatingKeysAndValues = alternatingKeysAndValues;
    this.keyOffset = keyOffset;
  }

  @Override
  ImmutableMap<K, V> map() {
    return map;
  }

  @SuppressWarnings(
      "unchecked") // kv stores K at (2 * i + keyOffset) and V at ((2 * i + keyOffset) ^ 1)
  private Entry<K, V> entryAt(int kvIndex) {
    int pos = kvIndex + keyOffset;
    return new SimpleImmutableEntry<>(
        (K) alternatingKeysAndValues[pos], (V) alternatingKeysAndValues[pos ^ 1]);
  }

  @Override
  public UnmodifiableIterator<Entry<K, V>> iterator() {
    Object[] kv = this.alternatingKeysAndValues;
    int offset = this.keyOffset;
    return new UnmodifiableIterator<Entry<K, V>>() {
      private int index = 0;

      @Override
      public boolean hasNext() {
        return index < kv.length;
      }

      @Override
      @SuppressWarnings("unchecked")
      public Entry<K, V> next() {
        int i = index;
        if (i >= kv.length) {
          throw new NoSuchElementException();
        }
        index = i + 2;
        int pos = i + offset;
        return new SimpleImmutableEntry<>((K) kv[pos], (V) kv[pos ^ 1]);
      }
    };
  }

  @Override
  @GwtIncompatible // Spliterator
  public Spliterator<Entry<K, V>> spliterator() {
    return CollectSpliterators.indexed(
        alternatingKeysAndValues.length >>> 1, SPLITERATOR_CHARACTERISTICS, i -> entryAt(i << 1));
  }

  @Override
  @SuppressWarnings("unchecked")
  public void forEach(Consumer<? super Entry<K, V>> action) {
    checkNotNull(action);
    Object[] kv = this.alternatingKeysAndValues;
    int offset = this.keyOffset;
    for (int i = 0; i < kv.length; i += 2) {
      int pos = i + offset;
      action.accept(new SimpleImmutableEntry<>((K) kv[pos], (V) kv[pos ^ 1]));
    }
  }

  @Override
  @GwtIncompatible // not used in GWT
  @SuppressWarnings("unchecked")
  int copyIntoArray(@Nullable Object[] dst, int dstOffset) {
    Object[] kv = this.alternatingKeysAndValues;
    int offset = this.keyOffset;
    for (int i = 0; i < kv.length; i += 2) {
      int pos = i + offset;
      dst[dstOffset++] = new SimpleImmutableEntry<>((K) kv[pos], (V) kv[pos ^ 1]);
    }
    return dstOffset;
  }

  @Override
  ImmutableList<Entry<K, V>> createAsList() {
    return new ImmutableAsList<Entry<K, V>>() {
      @Override
      public Entry<K, V> get(int index) {
        // Bounds check before shifting to prevent index << 1 wraparound (e.g. 1 << 30).
        checkElementIndex(index, alternatingKeysAndValues.length >>> 1);
        return entryAt(index << 1);
      }

      @Override
      ImmutableCollection<Entry<K, V>> delegateCollection() {
        return FlatEntrySet.this;
      }

      // redeclare to help optimizers with b/310253115
      @SuppressWarnings("RedundantOverride")
      @Override
      @J2ktIncompatible
      @GwtIncompatible
            Object writeReplace() {
        return super.writeReplace();
      }
    };
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

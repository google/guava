/*
 * Copyright (C) 2018 The Guava Authors
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

import static com.google.common.collect.Maps.immutableEntry;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.j2objc.annotations.RetainedWith;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Implementation of ImmutableBiMap backed by a pair of JDK HashMaps, which have smartness
 * protecting against hash flooding.
 */
@GwtIncompatible
final class JdkBackedImmutableBiMap<K, V> extends ImmutableBiMap<K, V> {
  static <K, V> ImmutableBiMap<K, V> create(int n, @Nullable Entry<K, V>[] entryArray) {
    Map<K, V> forwardDelegate = Maps.newHashMapWithExpectedSize(n);
    Map<V, K> backwardDelegate = Maps.newHashMapWithExpectedSize(n);
    for (int i = 0; i < n; i++) {
      // requireNonNull is safe because the first `n` elements have been filled in.
      Entry<K, V> e = RegularImmutableMap.makeImmutable(requireNonNull(entryArray[i]));
      entryArray[i] = e;
      V oldValue = forwardDelegate.putIfAbsent(e.getKey(), e.getValue());
      if (oldValue != null) {
        throw conflictException("key", e.getKey() + "=" + oldValue, entryArray[i]);
      }
      K oldKey = backwardDelegate.putIfAbsent(e.getValue(), e.getKey());
      if (oldKey != null) {
        throw conflictException("value", oldKey + "=" + e.getValue(), entryArray[i]);
      }
    }
    ImmutableList<Entry<K, V>> entryList = ImmutableList.asImmutableList(entryArray, n);
    return new JdkBackedImmutableBiMap<>(
        entryList, forwardDelegate, backwardDelegate, /* inverse= */ null);
  }

  private final transient ImmutableList<Entry<K, V>> entries;
  private final Map<K, V> forwardDelegate;
  private final Map<V, K> backwardDelegate;
  private final @Nullable JdkBackedImmutableBiMap<V, K> inverse;

  private JdkBackedImmutableBiMap(
      ImmutableList<Entry<K, V>> entries,
      Map<K, V> forwardDelegate,
      Map<V, K> backwardDelegate,
      @Nullable JdkBackedImmutableBiMap<V, K> inverse) {
    this.entries = entries;
    this.forwardDelegate = forwardDelegate;
    this.backwardDelegate = backwardDelegate;
    this.inverse = inverse;
  }

  @Override
  public int size() {
    return entries.size();
  }

  @Override
  public ImmutableBiMap<V, K> inverse() {
    return inverse != null ? inverse : lazyInverse();
  }

  @LazyInit @RetainedWith private transient @Nullable JdkBackedImmutableBiMap<V, K> lazyInverse;

  private ImmutableBiMap<V, K> lazyInverse() {
    JdkBackedImmutableBiMap<V, K> result = lazyInverse;
    return result == null
        ? lazyInverse =
            new JdkBackedImmutableBiMap<>(
                new InverseEntries<>(entries),
                backwardDelegate,
                forwardDelegate,
                /* inverse= */ this)
        : result;
  }

  private static final class InverseEntries<K extends @Nullable Object, V extends @Nullable Object>
      extends ImmutableList<Entry<V, K>> {
    private final ImmutableList<Entry<K, V>> entries;

    InverseEntries(ImmutableList<Entry<K, V>> entries) {
      this.entries = entries;
    }

    @Override
    public Entry<V, K> get(int index) {
      Entry<K, V> entry = entries.get(index);
      return immutableEntry(entry.getValue(), entry.getKey());
    }

    @Override
    boolean isPartialView() {
      return false;
    }

    @Override
    public int size() {
      return entries.size();
    }

    // redeclare to help optimizers with b/310253115
    @SuppressWarnings("RedundantOverride")
    @Override
    @J2ktIncompatible
        Object writeReplace() {
      return super.writeReplace();
    }
  }

  @Override
  public @Nullable V get(@Nullable Object key) {
    return forwardDelegate.get(key);
  }

  @Override
  ImmutableSet<Entry<K, V>> createEntrySet() {
    return new ImmutableMapEntrySet.RegularEntrySet<>(this, entries);
  }

  @Override
  ImmutableSet<K> createKeySet() {
    return new ImmutableMapKeySet<>(this);
  }

  @Override
  boolean isPartialView() {
    return false;
  }

  // redeclare to help optimizers with b/310253115
  @SuppressWarnings("RedundantOverride")
  @Override
  @J2ktIncompatible
    Object writeReplace() {
    return super.writeReplace();
  }
}

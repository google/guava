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

import static com.google.common.base.Preconditions.checkPositionIndex;
import static com.google.common.collect.CollectPreconditions.checkEntryNotNull;
import static com.google.common.collect.ImmutableMapEntry.createEntryArray;
import static com.google.common.collect.RegularImmutableMap.checkNoConflictInKeyBucket;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableMapEntry.NonTerminalImmutableBiMapEntry;
import com.google.j2objc.annotations.WeakOuter;

import java.io.Serializable;

import javax.annotation.Nullable;

/**
 * Bimap with zero or more mappings.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // uses writeReplace(), not default serialization
class RegularImmutableBiMap<K, V> extends ImmutableBiMap<K, V> {
  static final RegularImmutableBiMap<Object, Object> EMPTY =
      new RegularImmutableBiMap<Object, Object>(
          null, null, (Entry<Object, Object>[]) ImmutableMap.EMPTY_ENTRY_ARRAY, 0, 0);

  static final double MAX_LOAD_FACTOR = 1.2;

  private final transient ImmutableMapEntry<K, V>[] keyTable;
  private final transient ImmutableMapEntry<K, V>[] valueTable;
  private final transient Entry<K, V>[] entries;
  private final transient int mask;
  private final transient int hashCode;

  static <K, V> RegularImmutableBiMap<K, V> fromEntries(Entry<K, V>... entries) {
    return fromEntryArray(entries.length, entries);
  }

  static <K, V> RegularImmutableBiMap<K, V> fromEntryArray(int n, Entry<K, V>[] entryArray) {
    checkPositionIndex(n, entryArray.length);
    int tableSize = Hashing.closedTableSize(n, MAX_LOAD_FACTOR);
    int mask = tableSize - 1;
    ImmutableMapEntry<K, V>[] keyTable = createEntryArray(tableSize);
    ImmutableMapEntry<K, V>[] valueTable = createEntryArray(tableSize);
    Entry<K, V>[] entries;
    if (n == entryArray.length) {
      entries = entryArray;
    } else {
      entries = createEntryArray(n);
    }
    int hashCode = 0;

    for (int i = 0; i < n; i++) {
      @SuppressWarnings("unchecked")
      Entry<K, V> entry = entryArray[i];
      K key = entry.getKey();
      V value = entry.getValue();
      checkEntryNotNull(key, value);
      int keyHash = key.hashCode();
      int valueHash = value.hashCode();
      int keyBucket = Hashing.smear(keyHash) & mask;
      int valueBucket = Hashing.smear(valueHash) & mask;

      ImmutableMapEntry<K, V> nextInKeyBucket = keyTable[keyBucket];
      checkNoConflictInKeyBucket(key, entry, nextInKeyBucket);
      ImmutableMapEntry<K, V> nextInValueBucket = valueTable[valueBucket];
      checkNoConflictInValueBucket(value, entry, nextInValueBucket);
      ImmutableMapEntry<K, V> newEntry;
      if (nextInValueBucket == null && nextInKeyBucket == null) {
        /*
         * TODO(lowasser): consider using a NonTerminalImmutableMapEntry when nextInKeyBucket is
         * nonnull but nextInValueBucket is null.  This may save a few bytes on some platforms, but
         * 2-morphic call sites are often optimized much better than 3-morphic, so it'd require
         * benchmarking.
         */
        boolean reusable =
            entry instanceof ImmutableMapEntry && ((ImmutableMapEntry<K, V>) entry).isReusable();
        newEntry =
            reusable ? (ImmutableMapEntry<K, V>) entry : new ImmutableMapEntry<K, V>(key, value);
      } else {
        newEntry =
            new NonTerminalImmutableBiMapEntry<K, V>(
                key, value, nextInKeyBucket, nextInValueBucket);
      }
      keyTable[keyBucket] = newEntry;
      valueTable[valueBucket] = newEntry;
      entries[i] = newEntry;
      hashCode += keyHash ^ valueHash;
    }
    return new RegularImmutableBiMap<K, V>(keyTable, valueTable, entries, mask, hashCode);
  }

  private RegularImmutableBiMap(
      ImmutableMapEntry<K, V>[] keyTable,
      ImmutableMapEntry<K, V>[] valueTable,
      Entry<K, V>[] entries,
      int mask,
      int hashCode) {
    this.keyTable = keyTable;
    this.valueTable = valueTable;
    this.entries = entries;
    this.mask = mask;
    this.hashCode = hashCode;
  }

  // checkNoConflictInKeyBucket is static imported from RegularImmutableMap

  private static void checkNoConflictInValueBucket(
      Object value, Entry<?, ?> entry, @Nullable ImmutableMapEntry<?, ?> valueBucketHead) {
    for (; valueBucketHead != null; valueBucketHead = valueBucketHead.getNextInValueBucket()) {
      checkNoConflict(!value.equals(valueBucketHead.getValue()), "value", entry, valueBucketHead);
    }
  }

  @Override
  @Nullable
  public V get(@Nullable Object key) {
    return (keyTable == null) ? null : RegularImmutableMap.get(key, keyTable, mask);
  }

  @Override
  ImmutableSet<Entry<K, V>> createEntrySet() {
    return isEmpty()
        ? ImmutableSet.<Entry<K, V>>of()
        : new ImmutableMapEntrySet.RegularEntrySet<K, V>(this, entries);
  }

  @Override
  boolean isHashCodeFast() {
    return true;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  boolean isPartialView() {
    return false;
  }

  @Override
  public int size() {
    return entries.length;
  }

  private transient ImmutableBiMap<V, K> inverse;

  @Override
  public ImmutableBiMap<V, K> inverse() {
    if (isEmpty()) {
      return ImmutableBiMap.of();
    }
    ImmutableBiMap<V, K> result = inverse;
    return (result == null) ? inverse = new Inverse() : result;
  }

  private final class Inverse extends ImmutableBiMap<V, K> {

    @Override
    public int size() {
      return inverse().size();
    }

    @Override
    public ImmutableBiMap<K, V> inverse() {
      return RegularImmutableBiMap.this;
    }

    @Override
    public K get(@Nullable Object value) {
      if (value == null || valueTable == null) {
        return null;
      }
      int bucket = Hashing.smear(value.hashCode()) & mask;
      for (ImmutableMapEntry<K, V> entry = valueTable[bucket];
          entry != null;
          entry = entry.getNextInValueBucket()) {
        if (value.equals(entry.getValue())) {
          return entry.getKey();
        }
      }
      return null;
    }

    @Override
    ImmutableSet<Entry<V, K>> createEntrySet() {
      return new InverseEntrySet();
    }

    @WeakOuter
    final class InverseEntrySet extends ImmutableMapEntrySet<V, K> {
      @Override
      ImmutableMap<V, K> map() {
        return Inverse.this;
      }

      @Override
      boolean isHashCodeFast() {
        return true;
      }

      @Override
      public int hashCode() {
        return hashCode;
      }

      @Override
      public UnmodifiableIterator<Entry<V, K>> iterator() {
        return asList().iterator();
      }

      @Override
      ImmutableList<Entry<V, K>> createAsList() {
        return new ImmutableAsList<Entry<V, K>>() {
          @Override
          public Entry<V, K> get(int index) {
            Entry<K, V> entry = entries[index];
            return Maps.immutableEntry(entry.getValue(), entry.getKey());
          }

          @Override
          ImmutableCollection<Entry<V, K>> delegateCollection() {
            return InverseEntrySet.this;
          }
        };
      }
    }

    @Override
    boolean isPartialView() {
      return false;
    }

    @Override
    Object writeReplace() {
      return new InverseSerializedForm<K, V>(RegularImmutableBiMap.this);
    }
  }

  private static class InverseSerializedForm<K, V> implements Serializable {
    private final ImmutableBiMap<K, V> forward;

    InverseSerializedForm(ImmutableBiMap<K, V> forward) {
      this.forward = forward;
    }

    Object readResolve() {
      return forward.inverse();
    }

    private static final long serialVersionUID = 1;
  }
}

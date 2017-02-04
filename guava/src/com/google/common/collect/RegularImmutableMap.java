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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndex;
import static com.google.common.collect.CollectPreconditions.checkEntryNotNull;
import static com.google.common.collect.ImmutableMapEntry.createEntryArray;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.ImmutableMapEntry.NonTerminalImmutableMapEntry;
import com.google.j2objc.annotations.Weak;
import java.io.Serializable;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;

/**
 * Implementation of {@link ImmutableMap} with two or more entries.
 *
 * @author Jesse Wilson
 * @author Kevin Bourrillion
 * @author Gregory Kick
 */
@GwtCompatible(serializable = true, emulated = true)
final class RegularImmutableMap<K, V> extends ImmutableMap<K, V> {
  @SuppressWarnings("unchecked")
  static final ImmutableMap<Object, Object> EMPTY =
      new RegularImmutableMap<Object, Object>(
          (Entry<Object, Object>[]) ImmutableMap.EMPTY_ENTRY_ARRAY, null, 0);
  
  // entries in insertion order
  private final transient Entry<K, V>[] entries;
  // array of linked lists of entries
  private final transient ImmutableMapEntry<K, V>[] table;
  // 'and' with an int to get a table index
  private final transient int mask;

  static <K, V> RegularImmutableMap<K, V> fromEntries(Entry<K, V>... entries) {
    return fromEntryArray(entries.length, entries);
  }

  /**
   * Creates a RegularImmutableMap from the first n entries in entryArray.  This implementation
   * may replace the entries in entryArray with its own entry objects (though they will have the
   * same key/value contents), and may take ownership of entryArray.
   */
  static <K, V> RegularImmutableMap<K, V> fromEntryArray(int n, Entry<K, V>[] entryArray) {
    checkPositionIndex(n, entryArray.length);
    if (n == 0) {
      return (RegularImmutableMap<K, V>) EMPTY;
    }
    Entry<K, V>[] entries;
    if (n == entryArray.length) {
      entries = entryArray;
    } else {
      entries = createEntryArray(n);
    }
    int tableSize = Hashing.closedTableSize(n, MAX_LOAD_FACTOR);
    ImmutableMapEntry<K, V>[] table = createEntryArray(tableSize);
    int mask = tableSize - 1;
    for (int entryIndex = 0; entryIndex < n; entryIndex++) {
      Entry<K, V> entry = entryArray[entryIndex];
      K key = entry.getKey();
      V value = entry.getValue();
      checkEntryNotNull(key, value);
      int tableIndex = Hashing.smear(key.hashCode()) & mask;
      @Nullable ImmutableMapEntry<K, V> existing = table[tableIndex];
      // prepend, not append, so the entries can be immutable
      ImmutableMapEntry<K, V> newEntry;
      if (existing == null) {
        boolean reusable =
            entry instanceof ImmutableMapEntry && ((ImmutableMapEntry<K, V>) entry).isReusable();
        newEntry =
            reusable ? (ImmutableMapEntry<K, V>) entry : new ImmutableMapEntry<K, V>(key, value);
      } else {
        newEntry = new NonTerminalImmutableMapEntry<K, V>(key, value, existing);
      }
      table[tableIndex] = newEntry;
      entries[entryIndex] = newEntry;
      checkNoConflictInKeyBucket(key, newEntry, existing);
    }
    return new RegularImmutableMap<K, V>(entries, table, mask);
  }

  private RegularImmutableMap(Entry<K, V>[] entries, ImmutableMapEntry<K, V>[] table, int mask) {
    this.entries = entries;
    this.table = table;
    this.mask = mask;
  }

  static void checkNoConflictInKeyBucket(
      Object key, Entry<?, ?> entry, @Nullable ImmutableMapEntry<?, ?> keyBucketHead) {
    for (; keyBucketHead != null; keyBucketHead = keyBucketHead.getNextInKeyBucket()) {
      checkNoConflict(!key.equals(keyBucketHead.getKey()), "key", entry, keyBucketHead);
    }
  }

  /**
   * Closed addressing tends to perform well even with high load factors.
   * Being conservative here ensures that the table is still likely to be
   * relatively sparse (hence it misses fast) while saving space.
   */
  private static final double MAX_LOAD_FACTOR = 1.2;

  @Override
  public V get(@Nullable Object key) {
    return get(key, table, mask);
  }

  @Nullable
  static <V> V get(@Nullable Object key, @Nullable ImmutableMapEntry<?, V>[] keyTable, int mask) {
    if (key == null || keyTable == null) {
      return null;
    }
    int index = Hashing.smear(key.hashCode()) & mask;
    for (ImmutableMapEntry<?, V> entry = keyTable[index];
        entry != null;
        entry = entry.getNextInKeyBucket()) {
      Object candidateKey = entry.getKey();

      /*
       * Assume that equals uses the == optimization when appropriate, and that
       * it would check hash codes as an optimization when appropriate. If we
       * did these things, it would just make things worse for the most
       * performance-conscious users.
       */
      if (key.equals(candidateKey)) {
        return entry.getValue();
      }
    }
    return null;
  }

  @Override
  public void forEach(BiConsumer<? super K, ? super V> action) {
    checkNotNull(action);
    for (Entry<K, V> entry : entries) {
      action.accept(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public int size() {
    return entries.length;
  }

  @Override
  boolean isPartialView() {
    return false;
  }

  @Override
  ImmutableSet<Entry<K, V>> createEntrySet() {
    return new ImmutableMapEntrySet.RegularEntrySet<K, V>(this, entries);
  }

  @Override
  ImmutableSet<K> createKeySet() {
    return new KeySet<K, V>(this);
  }

  @GwtCompatible(emulated = true)
  private static final class KeySet<K, V> extends ImmutableSet.Indexed<K> {
    @Weak private final RegularImmutableMap<K, V> map;

    KeySet(RegularImmutableMap<K, V> map) {
      this.map = map;
    }

    @Override
    K get(int index) {
      return map.entries[index].getKey();
    }

    @Override
    public boolean contains(Object object) {
      return map.containsKey(object);
    }

    @Override
    boolean isPartialView() {
      return true;
    }

    @Override
    public int size() {
      return map.size();
    }

    @GwtIncompatible // serialization
    @Override
    Object writeReplace() {
      return new SerializedForm<K>(map);
    }

    @GwtIncompatible // serialization
    private static class SerializedForm<K> implements Serializable {
      final ImmutableMap<K, ?> map;

      SerializedForm(ImmutableMap<K, ?> map) {
        this.map = map;
      }

      Object readResolve() {
        return map.keySet();
      }

      private static final long serialVersionUID = 0;
    }
  }

  @Override
  ImmutableCollection<V> createValues() {
    return new Values<K, V>(this);
  }

  @GwtCompatible(emulated = true)
  private static final class Values<K, V> extends ImmutableList<V> {
    @Weak final RegularImmutableMap<K, V> map;

    Values(RegularImmutableMap<K, V> map) {
      this.map = map;
    }

    @Override
    public V get(int index) {
      return map.entries[index].getValue();
    }

    @Override
    public int size() {
      return map.size();
    }

    @Override
    boolean isPartialView() {
      return true;
    }

    @GwtIncompatible // serialization
    @Override
    Object writeReplace() {
      return new SerializedForm<V>(map);
    }

    @GwtIncompatible // serialization
    private static class SerializedForm<V> implements Serializable {
      final ImmutableMap<?, V> map;

      SerializedForm(ImmutableMap<?, V> map) {
        this.map = map;
      }

      Object readResolve() {
        return map.values();
      }

      private static final long serialVersionUID = 0;
    }
  }

  // This class is never actually serialized directly, but we have to make the
  // warning go away (and suppressing would suppress for all nested classes too)
  private static final long serialVersionUID = 0;
}

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
import static com.google.common.collect.RegularImmutableMap.MAX_HASH_BUCKET_LENGTH;
import static com.google.common.collect.RegularImmutableMap.checkNoConflictInKeyBucket;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMapEntry.NonTerminalImmutableBiMapEntry;
import com.google.common.collect.RegularImmutableMap.BucketOverflowException;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.j2objc.annotations.RetainedWith;
import java.io.Serializable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Bimap with zero or more mappings.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // uses writeReplace(), not default serialization
@ElementTypesAreNonnullByDefault
class RegularImmutableBiMap<K, V> extends ImmutableBiMap<K, V> {
  static final RegularImmutableBiMap<Object, Object> EMPTY =
      new RegularImmutableBiMap<>(
          null, null, (Entry<Object, Object>[]) ImmutableMap.EMPTY_ENTRY_ARRAY, 0, 0);

  static final double MAX_LOAD_FACTOR = 1.2;

  @CheckForNull private final transient @Nullable ImmutableMapEntry<K, V>[] keyTable;
  @CheckForNull private final transient @Nullable ImmutableMapEntry<K, V>[] valueTable;
  @VisibleForTesting final transient Entry<K, V>[] entries;
  private final transient int mask;
  private final transient int hashCode;

  static <K, V> ImmutableBiMap<K, V> fromEntries(Entry<K, V>... entries) {
    return fromEntryArray(entries.length, entries);
  }

  static <K, V> ImmutableBiMap<K, V> fromEntryArray(int n, @Nullable Entry<K, V>[] entryArray) {
    checkPositionIndex(n, entryArray.length);
    int tableSize = Hashing.closedTableSize(n, MAX_LOAD_FACTOR);
    int mask = tableSize - 1;
    @Nullable ImmutableMapEntry<K, V>[] keyTable = createEntryArray(tableSize);
    @Nullable ImmutableMapEntry<K, V>[] valueTable = createEntryArray(tableSize);
    /*
     * The cast is safe: n==entryArray.length means that we have filled the whole array with Entry
     * instances, in which case it is safe to cast it from an array of nullable entries to an array
     * of non-null entries.
     */
    @SuppressWarnings("nullness")
    Entry<K, V>[] entries =
        (n == entryArray.length) ? (Entry<K, V>[]) entryArray : createEntryArray(n);
    int hashCode = 0;

    for (int i = 0; i < n; i++) {
      // requireNonNull is safe because the first `n` elements have been filled in.
      Entry<K, V> entry = requireNonNull(entryArray[i]);
      K key = entry.getKey();
      V value = entry.getValue();
      checkEntryNotNull(key, value);
      int keyHash = key.hashCode();
      int valueHash = value.hashCode();
      int keyBucket = Hashing.smear(keyHash) & mask;
      int valueBucket = Hashing.smear(valueHash) & mask;

      ImmutableMapEntry<K, V> nextInKeyBucket = keyTable[keyBucket];
      ImmutableMapEntry<K, V> nextInValueBucket = valueTable[valueBucket];
      try {
        checkNoConflictInKeyBucket(key, value, nextInKeyBucket, /* throwIfDuplicateKeys= */ true);
        checkNoConflictInValueBucket(value, entry, nextInValueBucket);
      } catch (BucketOverflowException e) {
        return JdkBackedImmutableBiMap.create(n, entryArray);
      }
      ImmutableMapEntry<K, V> newEntry =
          (nextInValueBucket == null && nextInKeyBucket == null)
              ? RegularImmutableMap.makeImmutable(entry, key, value)
              : new NonTerminalImmutableBiMapEntry<>(
                  key, value, nextInKeyBucket, nextInValueBucket);
      keyTable[keyBucket] = newEntry;
      valueTable[valueBucket] = newEntry;
      entries[i] = newEntry;
      hashCode += keyHash ^ valueHash;
    }
    return new RegularImmutableBiMap<>(keyTable, valueTable, entries, mask, hashCode);
  }

  private RegularImmutableBiMap(
      @CheckForNull @Nullable ImmutableMapEntry<K, V>[] keyTable,
      @CheckForNull @Nullable ImmutableMapEntry<K, V>[] valueTable,
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

  /**
   * @throws IllegalArgumentException if another entry in the bucket has the same key
   * @throws BucketOverflowException if this bucket has too many entries, which may indicate a hash
   *     flooding attack
   */
  private static void checkNoConflictInValueBucket(
      Object value, Entry<?, ?> entry, @CheckForNull ImmutableMapEntry<?, ?> valueBucketHead)
      throws BucketOverflowException {
    int bucketSize = 0;
    for (; valueBucketHead != null; valueBucketHead = valueBucketHead.getNextInValueBucket()) {
      checkNoConflict(!value.equals(valueBucketHead.getValue()), "value", entry, valueBucketHead);
      if (++bucketSize > MAX_HASH_BUCKET_LENGTH) {
        throw new BucketOverflowException();
      }
    }
  }

  @Override
  @CheckForNull
  public V get(@CheckForNull Object key) {
    return RegularImmutableMap.get(key, keyTable, mask);
  }

  @Override
  ImmutableSet<Entry<K, V>> createEntrySet() {
    return isEmpty()
        ? ImmutableSet.<Entry<K, V>>of()
        : new ImmutableMapEntrySet.RegularEntrySet<K, V>(this, entries);
  }

  @Override
  ImmutableSet<K> createKeySet() {
    return new ImmutableMapKeySet<>(this);
  }

  @Override
  public void forEach(BiConsumer<? super K, ? super V> action) {
    checkNotNull(action);
    for (Entry<K, V> entry : entries) {
      action.accept(entry.getKey(), entry.getValue());
    }
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

  @LazyInit @RetainedWith @CheckForNull private transient ImmutableBiMap<V, K> inverse;

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
    public void forEach(BiConsumer<? super V, ? super K> action) {
      checkNotNull(action);
      RegularImmutableBiMap.this.forEach((k, v) -> action.accept(v, k));
    }

    @Override
    @CheckForNull
    public K get(@CheckForNull Object value) {
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
    ImmutableSet<V> createKeySet() {
      return new ImmutableMapKeySet<>(this);
    }

    @Override
    ImmutableSet<Entry<V, K>> createEntrySet() {
      return new InverseEntrySet();
    }

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
      public void forEach(Consumer<? super Entry<V, K>> action) {
        asList().forEach(action);
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
      return new InverseSerializedForm<>(RegularImmutableBiMap.this);
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

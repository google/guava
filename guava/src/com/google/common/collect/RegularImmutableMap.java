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
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMapEntry.NonTerminalImmutableMapEntry;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.Serializable;
import java.util.IdentityHashMap;
import java.util.function.BiConsumer;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Implementation of {@link ImmutableMap} with two or more entries.
 *
 * @author Jesse Wilson
 * @author Kevin Bourrillion
 * @author Gregory Kick
 */
@GwtCompatible(serializable = true, emulated = true)
@ElementTypesAreNonnullByDefault
final class RegularImmutableMap<K, V> extends ImmutableMap<K, V> {
  @SuppressWarnings("unchecked")
  static final ImmutableMap<Object, Object> EMPTY =
      new RegularImmutableMap<>((Entry<Object, Object>[]) ImmutableMap.EMPTY_ENTRY_ARRAY, null, 0);

  /**
   * Closed addressing tends to perform well even with high load factors. Being conservative here
   * ensures that the table is still likely to be relatively sparse (hence it misses fast) while
   * saving space.
   */
  @VisibleForTesting static final double MAX_LOAD_FACTOR = 1.2;

  /**
   * Maximum allowed false positive probability of detecting a hash flooding attack given random
   * input.
   */
  @VisibleForTesting static final double HASH_FLOODING_FPP = 0.001;

  /**
   * Maximum allowed length of a hash table bucket before falling back to a j.u.HashMap based
   * implementation. Experimentally determined.
   */
  @VisibleForTesting static final int MAX_HASH_BUCKET_LENGTH = 8;

  // entries in insertion order
  @VisibleForTesting final transient Entry<K, V>[] entries;
  // array of linked lists of entries
  @CheckForNull private final transient @Nullable ImmutableMapEntry<K, V>[] table;
  // 'and' with an int to get a table index
  private final transient int mask;

  static <K, V> ImmutableMap<K, V> fromEntries(Entry<K, V>... entries) {
    return fromEntryArray(entries.length, entries, /* throwIfDuplicateKeys= */ true);
  }

  /**
   * Creates an ImmutableMap from the first n entries in entryArray. This implementation may replace
   * the entries in entryArray with its own entry objects (though they will have the same key/value
   * contents), and may take ownership of entryArray.
   */
  static <K, V> ImmutableMap<K, V> fromEntryArray(
      int n, @Nullable Entry<K, V>[] entryArray, boolean throwIfDuplicateKeys) {
    checkPositionIndex(n, entryArray.length);
    if (n == 0) {
      @SuppressWarnings("unchecked") // it has no entries so the type variables don't matter
      ImmutableMap<K, V> empty = (ImmutableMap<K, V>) EMPTY;
      return empty;
    }
    try {
      return fromEntryArrayCheckingBucketOverflow(n, entryArray, throwIfDuplicateKeys);
    } catch (BucketOverflowException e) {
      // probable hash flooding attack, fall back to j.u.HM based implementation and use its
      // implementation of hash flooding protection
      return JdkBackedImmutableMap.create(n, entryArray, throwIfDuplicateKeys);
    }
  }

  private static <K, V> ImmutableMap<K, V> fromEntryArrayCheckingBucketOverflow(
      int n, @Nullable Entry<K, V>[] entryArray, boolean throwIfDuplicateKeys)
      throws BucketOverflowException {
    /*
     * The cast is safe: n==entryArray.length means that we have filled the whole array with Entry
     * instances, in which case it is safe to cast it from an array of nullable entries to an array
     * of non-null entries.
     */
    @SuppressWarnings("nullness")
    Entry<K, V>[] entries =
        (n == entryArray.length) ? (Entry<K, V>[]) entryArray : createEntryArray(n);
    int tableSize = Hashing.closedTableSize(n, MAX_LOAD_FACTOR);
    @Nullable ImmutableMapEntry<K, V>[] table = createEntryArray(tableSize);
    int mask = tableSize - 1;
    // If duplicates are allowed, this IdentityHashMap will record the final Entry for each
    // duplicated key. We will use this final Entry to overwrite earlier slots in the entries array
    // that have the same key. Then a second pass will remove all but the first of the slots that
    // have this Entry. The value in the map becomes false when this first entry has been copied, so
    // we know not to copy the remaining ones.
    IdentityHashMap<Entry<K, V>, Boolean> duplicates = null;
    int dupCount = 0;
    for (int entryIndex = n - 1; entryIndex >= 0; entryIndex--) {
      // requireNonNull is safe because the first `n` elements have been filled in.
      Entry<K, V> entry = requireNonNull(entryArray[entryIndex]);
      K key = entry.getKey();
      V value = entry.getValue();
      checkEntryNotNull(key, value);
      int tableIndex = Hashing.smear(key.hashCode()) & mask;
      ImmutableMapEntry<K, V> keyBucketHead = table[tableIndex];
      ImmutableMapEntry<K, V> effectiveEntry =
          checkNoConflictInKeyBucket(key, value, keyBucketHead, throwIfDuplicateKeys);
      if (effectiveEntry == null) {
        // prepend, not append, so the entries can be immutable
        effectiveEntry =
            (keyBucketHead == null)
                ? makeImmutable(entry, key, value)
                : new NonTerminalImmutableMapEntry<K, V>(key, value, keyBucketHead);
        table[tableIndex] = effectiveEntry;
      } else {
        // We already saw this key, and the first value we saw (going backwards) is the one we are
        // keeping. So we won't touch table[], but we do still want to add the existing entry that
        // we found to entries[] so that we will see this key in the right place when iterating.
        if (duplicates == null) {
          duplicates = new IdentityHashMap<>();
        }
        duplicates.put(effectiveEntry, true);
        dupCount++;
        // Make sure we are not overwriting the original entries array, in case we later do
        // buildOrThrow(). We would want an exception to include two values for the duplicate key.
        if (entries == entryArray) {
          // Temporary variable is necessary to defeat bad smartcast (entries adopting the type of
          // entryArray) in the Kotlin translation.
          Entry<K, V>[] originalEntries = entries;
          entries = originalEntries.clone();
        }
      }
      entries[entryIndex] = effectiveEntry;
    }
    if (duplicates != null) {
      // Explicit type parameters needed here to avoid a problem with nullness inference.
      entries = RegularImmutableMap.<K, V>removeDuplicates(entries, n, n - dupCount, duplicates);
      int newTableSize = Hashing.closedTableSize(entries.length, MAX_LOAD_FACTOR);
      if (newTableSize != tableSize) {
        return fromEntryArrayCheckingBucketOverflow(
            entries.length, entries, /* throwIfDuplicateKeys= */ true);
      }
    }
    return new RegularImmutableMap<>(entries, table, mask);
  }

  /**
   * Constructs a new entry array where each duplicated key from the original appears only once, at
   * its first position but with its final value. The {@code duplicates} map is modified.
   *
   * @param entries the original array of entries including duplicates
   * @param n the number of valid entries in {@code entries}
   * @param newN the expected number of entries once duplicates are removed
   * @param duplicates a map of canonical {@link Entry} objects for each duplicate key. This map
   *     will be updated by the method, setting each value to false as soon as the {@link Entry} has
   *     been included in the new entry array.
   * @return an array of {@code newN} entries where no key appears more than once.
   */
  static <K, V> Entry<K, V>[] removeDuplicates(
      Entry<K, V>[] entries, int n, int newN, IdentityHashMap<Entry<K, V>, Boolean> duplicates) {
    Entry<K, V>[] newEntries = createEntryArray(newN);
    for (int in = 0, out = 0; in < n; in++) {
      Entry<K, V> entry = entries[in];
      Boolean status = duplicates.get(entry);
      // null=>not dup'd; true=>dup'd, first; false=>dup'd, not first
      if (status != null) {
        if (status) {
          duplicates.put(entry, false);
        } else {
          continue; // delete this entry; we already copied an earlier one for the same key
        }
      }
      newEntries[out++] = entry;
    }
    return newEntries;
  }

  /** Makes an entry usable internally by a new ImmutableMap without rereading its contents. */
  static <K, V> ImmutableMapEntry<K, V> makeImmutable(Entry<K, V> entry, K key, V value) {
    boolean reusable =
        entry instanceof ImmutableMapEntry && ((ImmutableMapEntry<K, V>) entry).isReusable();
    return reusable ? (ImmutableMapEntry<K, V>) entry : new ImmutableMapEntry<K, V>(key, value);
  }

  /** Makes an entry usable internally by a new ImmutableMap. */
  static <K, V> ImmutableMapEntry<K, V> makeImmutable(Entry<K, V> entry) {
    return makeImmutable(entry, entry.getKey(), entry.getValue());
  }

  private RegularImmutableMap(
      Entry<K, V>[] entries, @CheckForNull @Nullable ImmutableMapEntry<K, V>[] table, int mask) {
    this.entries = entries;
    this.table = table;
    this.mask = mask;
  }

  /**
   * Checks if the given key already appears in the hash chain starting at {@code keyBucketHead}. If
   * it does not, then null is returned. If it does, then if {@code throwIfDuplicateKeys} is true an
   * {@code IllegalArgumentException} is thrown, and otherwise the existing {@link Entry} is
   * returned.
   *
   * @throws IllegalArgumentException if another entry in the bucket has the same key and {@code
   *     throwIfDuplicateKeys} is true
   * @throws BucketOverflowException if this bucket has too many entries, which may indicate a hash
   *     flooding attack
   */
  @CanIgnoreReturnValue
  @CheckForNull
  static <K, V> ImmutableMapEntry<K, V> checkNoConflictInKeyBucket(
      Object key,
      Object newValue,
      @CheckForNull ImmutableMapEntry<K, V> keyBucketHead,
      boolean throwIfDuplicateKeys)
      throws BucketOverflowException {
    int bucketSize = 0;
    for (; keyBucketHead != null; keyBucketHead = keyBucketHead.getNextInKeyBucket()) {
      if (keyBucketHead.getKey().equals(key)) {
        if (throwIfDuplicateKeys) {
          checkNoConflict(/* safe= */ false, "key", keyBucketHead, key + "=" + newValue);
        } else {
          return keyBucketHead;
        }
      }
      if (++bucketSize > MAX_HASH_BUCKET_LENGTH) {
        throw new BucketOverflowException();
      }
    }
    return null;
  }

  static class BucketOverflowException extends Exception {}

  @Override
  @CheckForNull
  public V get(@CheckForNull Object key) {
    return get(key, table, mask);
  }

  @CheckForNull
  static <V> V get(
      @CheckForNull Object key,
      @CheckForNull @Nullable ImmutableMapEntry<?, V>[] keyTable,
      int mask) {
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
    return new ImmutableMapEntrySet.RegularEntrySet<>(this, entries);
  }

  @Override
  ImmutableSet<K> createKeySet() {
    return new KeySet<>(this);
  }

  @GwtCompatible(emulated = true)
  private static final class KeySet<K> extends IndexedImmutableSet<K> {
    private final RegularImmutableMap<K, ?> map;

    KeySet(RegularImmutableMap<K, ?> map) {
      this.map = map;
    }

    @Override
    K get(int index) {
      return map.entries[index].getKey();
    }

    @Override
    public boolean contains(@CheckForNull Object object) {
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

    // redeclare to help optimizers with b/310253115
    @SuppressWarnings("RedundantOverride")
    @Override
    @J2ktIncompatible // serialization
    @GwtIncompatible // serialization
    Object writeReplace() {
      return super.writeReplace();
    }

    // No longer used for new writes, but kept so that old data can still be read.
    @GwtIncompatible // serialization
    @J2ktIncompatible
    @SuppressWarnings("unused")
    private static class SerializedForm<K> implements Serializable {
      final ImmutableMap<K, ?> map;

      SerializedForm(ImmutableMap<K, ?> map) {
        this.map = map;
      }

      Object readResolve() {
        return map.keySet();
      }

      @J2ktIncompatible // serialization
      private static final long serialVersionUID = 0;
    }
  }

  @Override
  ImmutableCollection<V> createValues() {
    return new Values<>(this);
  }

  @GwtCompatible(emulated = true)
  private static final class Values<K, V> extends ImmutableList<V> {
    final RegularImmutableMap<K, V> map;

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

    // redeclare to help optimizers with b/310253115
    @SuppressWarnings("RedundantOverride")
    @Override
    @J2ktIncompatible // serialization
    @GwtIncompatible // serialization
    Object writeReplace() {
      return super.writeReplace();
    }

    // No longer used for new writes, but kept so that old data can still be read.
    @GwtIncompatible // serialization
    @J2ktIncompatible
    @SuppressWarnings("unused")
    private static class SerializedForm<V> implements Serializable {
      final ImmutableMap<?, V> map;

      SerializedForm(ImmutableMap<?, V> map) {
        this.map = map;
      }

      Object readResolve() {
        return map.values();
      }

      @J2ktIncompatible // serialization
      private static final long serialVersionUID = 0;
    }
  }

  // redeclare to help optimizers with b/310253115
  @SuppressWarnings("RedundantOverride")
  @Override
  @J2ktIncompatible // serialization
  @GwtIncompatible // serialization
  Object writeReplace() {
    return super.writeReplace();
  }

  // This class is never actually serialized directly, but we have to make the
  // warning go away (and suppressing would suppress for all nested classes too)
  @J2ktIncompatible // serialization
  private static final long serialVersionUID = 0;
}

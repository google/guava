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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.GwtCompatible;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Implementation of {@link ImmutableMap} with two or more entries.
 *
 * @author Jesse Wilson
 * @author Kevin Bourrillion
 * @author Gregory Kick
 */
@GwtCompatible(serializable = true, emulated = true)
final class RegularImmutableMap<K, V> extends ImmutableMap<K, V> {

  // entries in insertion order
  private final transient LinkedEntry<K, V>[] entries;
  // array of linked lists of entries
  private final transient LinkedEntry<K, V>[] table;
  // 'and' with an int to get a table index
  private final transient int mask;
  private final transient int keySetHashCode;

  // TODO(gak): investigate avoiding the creation of ImmutableEntries since we
  // re-copy them anyway.
  RegularImmutableMap(Entry<?, ?>... immutableEntries) {
    int size = immutableEntries.length;
    entries = createEntryArray(size);

    int tableSize = chooseTableSize(size);
    table = createEntryArray(tableSize);
    mask = tableSize - 1;

    int keySetHashCodeMutable = 0;
    for (int entryIndex = 0; entryIndex < size; entryIndex++) {
      // each of our 6 callers carefully put only Entry<K, V>s into the array!
      @SuppressWarnings("unchecked")
      Entry<K, V> entry = (Entry<K, V>) immutableEntries[entryIndex];
      K key = entry.getKey();
      int keyHashCode = key.hashCode();
      keySetHashCodeMutable += keyHashCode;
      int tableIndex = Hashing.smear(keyHashCode) & mask;
      @Nullable LinkedEntry<K, V> existing = table[tableIndex];
      // prepend, not append, so the entries can be immutable
      LinkedEntry<K, V> linkedEntry =
          newLinkedEntry(key, entry.getValue(), existing);
      table[tableIndex] = linkedEntry;
      entries[entryIndex] = linkedEntry;
      while (existing != null) {
        checkArgument(!key.equals(existing.getKey()), "duplicate key: %s", key);
        existing = existing.next();
      }
    }
    keySetHashCode = keySetHashCodeMutable;
  }

  /**
   * Closed addressing tends to perform well even with high load factors.
   * Being conservative here ensures that the table is still likely to be
   * relatively sparse (hence it misses fast) while saving space.
   */
  private static final double MAX_LOAD_FACTOR = 1.2;

  /**
   * Give a good hash table size for the given number of keys.
   *
   * @param size The number of keys to be inserted. Must be greater than or equal to 2.
   */
  private static int chooseTableSize(int size) {
    // Get the recommended table size.
    // Round down to the nearest power of 2.
    int tableSize = Integer.highestOneBit(size);
    // Check to make sure that we will not exceed the maximum load factor.
    if ((double) size / tableSize > MAX_LOAD_FACTOR) {
      tableSize <<= 1;
      checkArgument(tableSize > 0, "table too large: %s", size);
    }
    return tableSize;
  }

  /**
   * Creates a {@link LinkedEntry} array to hold parameterized entries. The
   * result must never be upcast back to LinkedEntry[] (or Object[], etc.), or
   * allowed to escape the class.
   */
  @SuppressWarnings("unchecked") // Safe as long as the javadocs are followed
  private LinkedEntry<K, V>[] createEntryArray(int size) {
    return new LinkedEntry[size];
  }

  private static <K, V> LinkedEntry<K, V> newLinkedEntry(K key, V value,
      @Nullable LinkedEntry<K, V> next) {
    return (next == null)
        ? new TerminalEntry<K, V>(key, value)
        : new NonTerminalEntry<K, V>(key, value, next);
  }

  private interface LinkedEntry<K, V> extends Entry<K, V> {
    /** Returns the next entry in the list or {@code null} if none exists. */
    @Nullable LinkedEntry<K, V> next();
  }

  /** {@code LinkedEntry} implementation that has a next value. */
  @Immutable
  @SuppressWarnings("serial") // this class is never serialized
  private static final class NonTerminalEntry<K, V>
      extends ImmutableEntry<K, V> implements LinkedEntry<K, V> {
    final LinkedEntry<K, V> next;

    NonTerminalEntry(K key, V value, LinkedEntry<K, V> next) {
      super(key, value);
      this.next = next;
    }

    @Override public LinkedEntry<K, V> next() {
      return next;
    }
  }

  /**
   * {@code LinkedEntry} implementation that serves as the last entry in the
   * list.  I.e. no next entry
   */
  @Immutable
  @SuppressWarnings("serial") // this class is never serialized
  private static final class TerminalEntry<K, V> extends ImmutableEntry<K, V>
      implements LinkedEntry<K, V> {
    TerminalEntry(K key, V value) {
      super(key, value);
    }

    @Nullable @Override public LinkedEntry<K, V> next() {
      return null;
    }
  }

  @Override public V get(@Nullable Object key) {
    if (key == null) {
      return null;
    }
    int index = Hashing.smear(key.hashCode()) & mask;
    for (LinkedEntry<K, V> entry = table[index];
        entry != null;
        entry = entry.next()) {
      K candidateKey = entry.getKey();

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
  public int size() {
    return entries.length;
  }

  @Override public boolean isEmpty() {
    return false;
  }

  @Override public boolean containsValue(@Nullable Object value) {
    if (value == null) {
      return false;
    }
    for (Entry<K, V> entry : entries) {
      if (entry.getValue().equals(value)) {
        return true;
      }
    }
    return false;
  }

  @Override boolean isPartialView() {
    return false;
  }

  @Override
  ImmutableSet<Entry<K, V>> createEntrySet() {
    return new EntrySet();
  }

  @SuppressWarnings("serial") // uses writeReplace(), not default serialization
  private class EntrySet extends ImmutableMapEntrySet<K, V> {
    @Override ImmutableMap<K, V> map() {
      return RegularImmutableMap.this;
    }

    @Override
    public UnmodifiableIterator<Entry<K, V>> iterator() {
      return asList().iterator();
    }

    @Override
    ImmutableList<Entry<K, V>> createAsList() {
      return new RegularImmutableAsList<Entry<K, V>>(this, entries);
    }
  }

  @Override
  ImmutableSet<K> createKeySet() {
    return new ImmutableMapKeySet<K, V>(entrySet(), keySetHashCode) {
      @Override ImmutableMap<K, V> map() {
        return RegularImmutableMap.this;
      }
    };
  }

  @Override public String toString() {
    StringBuilder result
        = Collections2.newStringBuilderForCollection(size()).append('{');
    Collections2.STANDARD_JOINER.appendTo(result, entries);
    return result.append('}').toString();
  }

  // This class is never actually serialized directly, but we have to make the
  // warning go away (and suppressing would suppress for all nested classes too)
  private static final long serialVersionUID = 0;
}

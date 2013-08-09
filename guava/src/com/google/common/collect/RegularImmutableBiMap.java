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

import static com.google.common.collect.CollectPreconditions.checkEntryNotNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableMapEntry.TerminalEntry;

import java.io.Serializable;

import javax.annotation.Nullable;

/**
 * Bimap with two or more mappings.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // uses writeReplace(), not default serialization
class RegularImmutableBiMap<K, V> extends ImmutableBiMap<K, V> {
  
  static final double MAX_LOAD_FACTOR = 1.2;
  
  private final transient ImmutableMapEntry<K, V>[] keyTable;
  private final transient ImmutableMapEntry<K, V>[] valueTable;
  private final transient ImmutableMapEntry<K, V>[] entries;
  private final transient int mask;
  private final transient int hashCode;
  
  RegularImmutableBiMap(TerminalEntry<?, ?>... entriesToAdd) {
    this(entriesToAdd.length, entriesToAdd);
  }
  
  /**
   * Constructor for RegularImmutableBiMap that takes as input an array of {@code TerminalEntry}
   * entries.  Assumes that these entries have already been checked for null.
   * 
   * <p>This allows reuse of the entry objects from the array in the actual implementation.
   */
  RegularImmutableBiMap(int n, TerminalEntry<?, ?>[] entriesToAdd) {
    int tableSize = Hashing.closedTableSize(n, MAX_LOAD_FACTOR);
    this.mask = tableSize - 1;
    ImmutableMapEntry<K, V>[] keyTable = createEntryArray(tableSize);
    ImmutableMapEntry<K, V>[] valueTable = createEntryArray(tableSize);
    ImmutableMapEntry<K, V>[] entries = createEntryArray(n);
    int hashCode = 0;
    
    for (int i = 0; i < n; i++) {
      @SuppressWarnings("unchecked")
      TerminalEntry<K, V> entry = (TerminalEntry<K, V>) entriesToAdd[i];
      K key = entry.getKey();
      V value = entry.getValue();
      
      int keyHash = key.hashCode();
      int valueHash = value.hashCode();
      int keyBucket = Hashing.smear(keyHash) & mask;
      int valueBucket = Hashing.smear(valueHash) & mask;
      
      ImmutableMapEntry<K, V> nextInKeyBucket = keyTable[keyBucket];
      for (ImmutableMapEntry<K, V> keyEntry = nextInKeyBucket; keyEntry != null;
           keyEntry = keyEntry.getNextInKeyBucket()) {
        checkNoConflict(!key.equals(keyEntry.getKey()), "key", entry, keyEntry);
      }
      ImmutableMapEntry<K, V> nextInValueBucket = valueTable[valueBucket];
      for (ImmutableMapEntry<K, V> valueEntry = nextInValueBucket; valueEntry != null;
           valueEntry = valueEntry.getNextInValueBucket()) {
        checkNoConflict(!value.equals(valueEntry.getValue()), "value", entry, valueEntry);
      }
      ImmutableMapEntry<K, V> newEntry =
          (nextInKeyBucket == null && nextInValueBucket == null)
          ? entry
          : new NonTerminalBiMapEntry<K, V>(entry, nextInKeyBucket, nextInValueBucket);
      keyTable[keyBucket] = newEntry;
      valueTable[valueBucket] = newEntry;
      entries[i] = newEntry;
      hashCode += keyHash ^ valueHash;
    }
    
    this.keyTable = keyTable;
    this.valueTable = valueTable;
    this.entries = entries;
    this.hashCode = hashCode;
  }
  
  /**
   * Constructor for RegularImmutableBiMap that makes no assumptions about the input entries.
   */
  RegularImmutableBiMap(Entry<?, ?>[] entriesToAdd) {
    int n = entriesToAdd.length;
    int tableSize = Hashing.closedTableSize(n, MAX_LOAD_FACTOR);
    this.mask = tableSize - 1;
    ImmutableMapEntry<K, V>[] keyTable = createEntryArray(tableSize);
    ImmutableMapEntry<K, V>[] valueTable = createEntryArray(tableSize);
    ImmutableMapEntry<K, V>[] entries = createEntryArray(n);
    int hashCode = 0;
    
    for (int i = 0; i < n; i++) {
      @SuppressWarnings("unchecked")
      Entry<K, V> entry = (Entry<K, V>) entriesToAdd[i];
      K key = entry.getKey();
      V value = entry.getValue();
      checkEntryNotNull(key, value);
      int keyHash = key.hashCode();
      int valueHash = value.hashCode();
      int keyBucket = Hashing.smear(keyHash) & mask;
      int valueBucket = Hashing.smear(valueHash) & mask;
      
      ImmutableMapEntry<K, V> nextInKeyBucket = keyTable[keyBucket];
      for (ImmutableMapEntry<K, V> keyEntry = nextInKeyBucket; keyEntry != null;
           keyEntry = keyEntry.getNextInKeyBucket()) {
        checkNoConflict(!key.equals(keyEntry.getKey()), "key", entry, keyEntry);
      }
      ImmutableMapEntry<K, V> nextInValueBucket = valueTable[valueBucket];
      for (ImmutableMapEntry<K, V> valueEntry = nextInValueBucket; valueEntry != null;
           valueEntry = valueEntry.getNextInValueBucket()) {
        checkNoConflict(!value.equals(valueEntry.getValue()), "value", entry, valueEntry);
      }
      ImmutableMapEntry<K, V> newEntry =
          (nextInKeyBucket == null && nextInValueBucket == null)
          ? new TerminalEntry<K, V>(key, value)
          : new NonTerminalBiMapEntry<K, V>(key, value, nextInKeyBucket, nextInValueBucket);
      keyTable[keyBucket] = newEntry;
      valueTable[valueBucket] = newEntry;
      entries[i] = newEntry;
      hashCode += keyHash ^ valueHash;
    }
    
    this.keyTable = keyTable;
    this.valueTable = valueTable;
    this.entries = entries;
    this.hashCode = hashCode;
  }
  
  private static final class NonTerminalBiMapEntry<K, V> extends ImmutableMapEntry<K, V> {
    @Nullable private final ImmutableMapEntry<K, V> nextInKeyBucket;
    @Nullable private final ImmutableMapEntry<K, V> nextInValueBucket;
    
    NonTerminalBiMapEntry(K key, V value, @Nullable ImmutableMapEntry<K, V> nextInKeyBucket,
        @Nullable ImmutableMapEntry<K, V> nextInValueBucket) {
      super(key, value);
      this.nextInKeyBucket = nextInKeyBucket;
      this.nextInValueBucket = nextInValueBucket;
    }

    NonTerminalBiMapEntry(ImmutableMapEntry<K, V> contents,
        @Nullable ImmutableMapEntry<K, V> nextInKeyBucket,
        @Nullable ImmutableMapEntry<K, V> nextInValueBucket) {
      super(contents);
      this.nextInKeyBucket = nextInKeyBucket;
      this.nextInValueBucket = nextInValueBucket;
    }

    @Override
    @Nullable
    ImmutableMapEntry<K, V> getNextInKeyBucket() {
      return nextInKeyBucket;
    }

    @Override
    @Nullable
    ImmutableMapEntry<K, V> getNextInValueBucket() {
      return nextInValueBucket;
    }
  }
  
  @SuppressWarnings("unchecked")
  private static <K, V> ImmutableMapEntry<K, V>[] createEntryArray(int length) {
    return new ImmutableMapEntry[length];
  }

  @Override
  @Nullable
  public V get(@Nullable Object key) {
    if (key == null) {
      return null;
    }
    int bucket = Hashing.smear(key.hashCode()) & mask;
    for (ImmutableMapEntry<K, V> entry = keyTable[bucket]; entry != null;
         entry = entry.getNextInKeyBucket()) {
      if (key.equals(entry.getKey())) {
        return entry.getValue();
      }
    }
    return null;
  }

  @Override
  ImmutableSet<Entry<K, V>> createEntrySet() {
    return new ImmutableMapEntrySet<K, V>() {
      @Override
      ImmutableMap<K, V> map() {
        return RegularImmutableBiMap.this;
      }

      @Override
      public UnmodifiableIterator<Entry<K, V>> iterator() {
        return asList().iterator();
      }

      @Override
      ImmutableList<Entry<K, V>> createAsList() {
        return new RegularImmutableAsList<Entry<K, V>>(this, entries);
      }

      @Override
      boolean isHashCodeFast() {
        return true;
      }

      @Override
      public int hashCode() {
        return hashCode;
      }
    };
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
      if (value == null) {
        return null;
      }
      int bucket = Hashing.smear(value.hashCode()) & mask;
      for (ImmutableMapEntry<K, V> entry = valueTable[bucket]; entry != null;
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

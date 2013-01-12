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

import com.google.common.annotations.GwtCompatible;

import java.io.Serializable;
import java.util.Collection;

import javax.annotation.Nullable;

/**
 * Bimap with two or more mappings.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // uses writeReplace(), not default serialization
class RegularImmutableBiMap<K, V> extends ImmutableBiMap<K, V> {
  private static class BiMapEntry<K, V> extends ImmutableEntry<K, V> {
    BiMapEntry(K key, V value) {
      super(key, value);
    }

    @Nullable
    BiMapEntry<K, V> getNextInKToVBucket() {
      return null;
    }

    @Nullable
    BiMapEntry<K, V> getNextInVToKBucket() {
      return null;
    }
  }
  
  private static class NonTerminalBiMapEntry<K, V> extends BiMapEntry<K, V> {
    @Nullable private final BiMapEntry<K, V> nextInKToVBucket;
    @Nullable private final BiMapEntry<K, V> nextInVToKBucket;

    NonTerminalBiMapEntry(K key, V value, @Nullable BiMapEntry<K, V> nextInKToVBucket,
        @Nullable BiMapEntry<K, V> nextInVToKBucket) {
      super(key, value);
      this.nextInKToVBucket = nextInKToVBucket;
      this.nextInVToKBucket = nextInVToKBucket;
    }

    @Override
    @Nullable
    BiMapEntry<K, V> getNextInKToVBucket() {
      return nextInKToVBucket;
    }
    
    @Override
    @Nullable
    BiMapEntry<K, V> getNextInVToKBucket() {
      return nextInVToKBucket;
    }
  }
  
  static final double MAX_LOAD_FACTOR = 1.2;
  
  private transient final BiMapEntry<K, V>[] kToVTable;
  private transient final BiMapEntry<K, V>[] vToKTable;
  private transient final BiMapEntry<K, V>[] entries;
  private transient final int mask;
  private transient final int hashCode;
  
  RegularImmutableBiMap(Collection<? extends Entry<? extends K, ? extends V>> entriesToAdd) {
    int n = entriesToAdd.size();
    int tableSize = Hashing.closedTableSize(n, MAX_LOAD_FACTOR);
    this.mask = tableSize - 1;
    BiMapEntry<K, V>[] kToVTable = createEntryArray(tableSize);
    BiMapEntry<K, V>[] vToKTable = createEntryArray(tableSize);
    BiMapEntry<K, V>[] entries = createEntryArray(n);
    int i = 0;
    int hashCode = 0;
    
    for (Entry<? extends K, ? extends V> entry : entriesToAdd) {
      K key = checkNotNull(entry.getKey());
      V value = checkNotNull(entry.getValue());
      
      int keyHash = key.hashCode();
      int valueHash = value.hashCode();
      int keyBucket = Hashing.smear(keyHash) & mask;
      int valueBucket = Hashing.smear(valueHash) & mask;
      
      BiMapEntry<K, V> nextInKToVBucket = kToVTable[keyBucket];
      for (BiMapEntry<K, V> kToVEntry = nextInKToVBucket; kToVEntry != null;
           kToVEntry = kToVEntry.getNextInKToVBucket()) {
        if (key.equals(kToVEntry.getKey())) {
          throw new IllegalArgumentException("Multiple entries with same key: " +
              entry + " and " + kToVEntry);
        }
      }
      BiMapEntry<K, V> nextInVToKBucket = vToKTable[valueBucket];
      for (BiMapEntry<K, V> vToKEntry = nextInVToKBucket; vToKEntry != null;
           vToKEntry = vToKEntry.getNextInVToKBucket()) {
        if (value.equals(vToKEntry.getValue())) {
          throw new IllegalArgumentException("Multiple entries with same value: "
              + entry + " and " + vToKEntry);
        }
      }
      BiMapEntry<K, V> newEntry =
          (nextInKToVBucket == null && nextInVToKBucket == null)
          ? new BiMapEntry<K, V>(key, value)
          : new NonTerminalBiMapEntry<K, V>(key, value, nextInKToVBucket, nextInVToKBucket);
      kToVTable[keyBucket] = newEntry;
      vToKTable[valueBucket] = newEntry;
      entries[i++] = newEntry;
      hashCode += keyHash ^ valueHash;
    }
    
    this.kToVTable = kToVTable;
    this.vToKTable = vToKTable;
    this.entries = entries;
    this.hashCode = hashCode;
  }
  
  @SuppressWarnings("unchecked")
  private static <K, V> BiMapEntry<K, V>[] createEntryArray(int length) {
    return new BiMapEntry[length];
  }

  @Override
  @Nullable
  public V get(@Nullable Object key) {
    if (key == null) {
      return null;
    }
    int bucket = Hashing.smear(key.hashCode()) & mask;
    for (BiMapEntry<K, V> entry = kToVTable[bucket]; entry != null;
         entry = entry.getNextInKToVBucket()) {
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
      for (BiMapEntry<K, V> entry = vToKTable[bucket]; entry != null;
           entry = entry.getNextInVToKBucket()) {
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

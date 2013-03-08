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
import com.google.common.annotations.GwtIncompatible;

import java.io.Serializable;
import java.util.Map.Entry;

import javax.annotation.Nullable;

/**
 * {@code entrySet()} implementation for {@link ImmutableMap}.
 *
 * @author Jesse Wilson
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
abstract class ImmutableMapEntrySet<K, V> extends ImmutableSet<Entry<K, V>> {
  final ImmutableMap<K, V> map;

  ImmutableMapEntrySet(ImmutableMap<K, V> map) {
    this.map = checkNotNull(map);
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean contains(@Nullable Object object) {
    if (object instanceof Entry) {
      Entry<?, ?> entry = (Entry<?, ?>) object;
      V value = map.get(entry.getKey());
      return value != null && value.equals(entry.getValue());
    }
    return false;
  }

  @Override
  boolean isPartialView() {
    return map.isPartialView();
  }
  
  abstract static class IndexedEntrySet<K, V> extends ImmutableMapEntrySet<K, V> {
    IndexedEntrySet(ImmutableMap<K, V> map) {
      super(map);
    }

    abstract Entry<K, V> getEntry(int index);
    
    @Override public int hashCode() {
      return map.hashCode();
    }

    @Override
    public UnmodifiableIterator<Entry<K, V>> iterator() {
      return asList().iterator();
    }

    @GwtIncompatible("no such method in GWT")
    @Override
    int copyIntoArray(Object[] dst, int offset) {
      return asList().copyIntoArray(dst, offset);
    }

    @Override
    ImmutableList<Entry<K, V>> createAsList() {
      return new ImmutableAsList<Entry<K, V>>() {
        @Override
        public Entry<K, V> get(int index) {
          return getEntry(index);
        }

        @Override
        ImmutableCollection<Entry<K, V>> delegateCollection() {
          return IndexedEntrySet.this;
        }
      };
    }
  }
  
  static final class ArrayEntrySet<K, V> extends IndexedEntrySet<K, V> {
    private final Entry<K, V>[] entries;

    ArrayEntrySet(ImmutableMap<K, V> map, Entry<K, V>[] entries) {
      super(map);
      this.entries = entries;
    }

    @Override
    Entry<K, V> getEntry(int index) {
      return entries[index];
    }

    @Override
    ImmutableList<Entry<K, V>> createAsList() {
      return new RegularImmutableAsList<Entry<K, V>>(this, entries);
    }
  }

  @GwtIncompatible("serialization")
  @Override
  Object writeReplace() {
    return new EntrySetSerializedForm<K, V>(map);
  }

  @GwtIncompatible("serialization")
  private static class EntrySetSerializedForm<K, V> implements Serializable {
    final ImmutableMap<K, V> map;
    EntrySetSerializedForm(ImmutableMap<K, V> map) {
      this.map = map;
    }
    Object readResolve() {
      return map.entrySet();
    }
    private static final long serialVersionUID = 0;
  }
}

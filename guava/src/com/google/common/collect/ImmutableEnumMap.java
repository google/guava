/*
 * Copyright (C) 2012 The Guava Authors
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

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Iterator;

import javax.annotation.Nullable;

/**
 * Implementation of {@link ImmutableMap} backed by a non-empty {@link
 * java.util.EnumMap}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // we're overriding default serialization
final class ImmutableEnumMap<K extends Enum<K>, V> extends ImmutableMap<K, V> {
  static <K extends Enum<K>, V> ImmutableMap<K, V> asImmutable(EnumMap<K, V> map) {
    switch (map.size()) {
      case 0:
        return ImmutableMap.of();
      case 1: {
        Entry<K, V> entry = Iterables.getOnlyElement(map.entrySet());
        return ImmutableMap.of(entry.getKey(), entry.getValue());
      }
      default:
        return new ImmutableEnumMap<K, V>(map);
    }
  }

  private transient final EnumMap<K, V> delegate;

  private ImmutableEnumMap(EnumMap<K, V> delegate) {
    this.delegate = delegate;
    checkArgument(!delegate.isEmpty());
  }

  @Override
  ImmutableSet<K> createKeySet() {
    return new ImmutableSet<K>() {

      @Override
      public boolean contains(Object object) {
        return delegate.containsKey(object);
      }

      @Override
      public int size() {
        return ImmutableEnumMap.this.size();
      }

      @Override
      public UnmodifiableIterator<K> iterator() {
        return Iterators.unmodifiableIterator(delegate.keySet().iterator());
      }

      @Override
      boolean isPartialView() {
        return true;
      }
    };
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public boolean containsKey(@Nullable Object key) {
    return delegate.containsKey(key);
  }

  @Override
  public V get(Object key) {
    return delegate.get(key);
  }

  @Override
  ImmutableSet<Entry<K, V>> createEntrySet() {
    return new ImmutableMapEntrySet<K, V>() {

      @Override
      ImmutableMap<K, V> map() {
        return ImmutableEnumMap.this;
      }

      @Override
      public UnmodifiableIterator<Entry<K, V>> iterator() {
        return new UnmodifiableIterator<Entry<K, V>>() {
          private final Iterator<Entry<K, V>> backingIterator = delegate.entrySet().iterator();

          @Override
          public boolean hasNext() {
            return backingIterator.hasNext();
          }

          @Override
          public Entry<K, V> next() {
            Entry<K, V> entry = backingIterator.next();
            return Maps.immutableEntry(entry.getKey(), entry.getValue());
          }
        };
      }
    };
  }

  @Override
  boolean isPartialView() {
    return false;
  }

  // All callers of the constructor are restricted to <K extends Enum<K>>.
  @Override Object writeReplace() {
    return new EnumSerializedForm<K, V>(delegate);
  }

  /*
   * This class is used to serialize ImmutableEnumSet instances.
   */
  private static class EnumSerializedForm<K extends Enum<K>, V>
      implements Serializable {
    final EnumMap<K, V> delegate;
    EnumSerializedForm(EnumMap<K, V> delegate) {
      this.delegate = delegate;
    }
    Object readResolve() {
      return new ImmutableEnumMap<K, V>(delegate);
    }
    private static final long serialVersionUID = 0;
  }
}

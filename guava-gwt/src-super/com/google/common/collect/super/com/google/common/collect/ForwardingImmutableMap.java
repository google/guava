/*
 * Copyright (C) 2009 The Guava Authors
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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * GWT implementation of {@link ImmutableMap} that forwards to another map.
 *
 * @author Hayward Chan
 */
public abstract class ForwardingImmutableMap<K, V> extends ImmutableMap<K, V> {

  final transient Map<K, V> delegate;

  ForwardingImmutableMap(Map<? extends K, ? extends V> delegate) {
    this.delegate = Collections.unmodifiableMap(delegate);
  }

  @SuppressWarnings("unchecked")
  ForwardingImmutableMap(boolean throwIfDuplicateKeys, Entry<? extends K, ? extends V>... entries) {
    Map<K, V> delegate = Maps.newLinkedHashMap();
    for (Entry<? extends K, ? extends V> entry : entries) {
      K key = checkNotNull(entry.getKey());
      V previous = delegate.put(key, checkNotNull(entry.getValue()));
      if (throwIfDuplicateKeys && previous != null) {
        throw new IllegalArgumentException("duplicate key: " + key);
      }
    }
    this.delegate = Collections.unmodifiableMap(delegate);
  }

  boolean isPartialView() {
    return false;
  }

  public final boolean isEmpty() {
    return delegate.isEmpty();
  }

  public final boolean containsKey(@Nullable Object key) {
    return Maps.safeContainsKey(delegate, key);
  }

  public final boolean containsValue(@Nullable Object value) {
    return delegate.containsValue(value);
  }

  public V get(@Nullable Object key) {
    return (key == null) ? null : Maps.safeGet(delegate, key);
  }

  @Override
  ImmutableSet<Entry<K, V>> createEntrySet() {
    return ImmutableSet.unsafeDelegate(
        new ForwardingSet<Entry<K, V>>() {
          @Override
          protected Set<Entry<K, V>> delegate() {
            return delegate.entrySet();
          }

          @Override
          public boolean contains(Object object) {
            if (object instanceof Entry<?, ?> && ((Entry<?, ?>) object).getKey() == null) {
              return false;
            }
            try {
              return super.contains(object);
            } catch (ClassCastException e) {
              return false;
            }
          }

          @Override
          public <T> T[] toArray(T[] array) {
            T[] result = super.toArray(array);
            if (size() < result.length) {
              // It works around a GWT bug where elements after last is not
              // properly null'ed.
              result[size()] = null;
            }
            return result;
          }
        });
  }

  @Override
  ImmutableSet<K> createKeySet() {
    return ImmutableSet.unsafeDelegate(delegate.keySet());
  }

  @Override
  ImmutableCollection<V> createValues() {
    return ImmutableCollection.unsafeDelegate(delegate.values());
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public boolean equals(@Nullable Object object) {
    return delegate.equals(object);
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}

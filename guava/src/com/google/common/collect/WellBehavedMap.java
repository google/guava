/*
 * Copyright (C) 2011 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;

import java.util.Map;
import java.util.Set;

/**
 * Workaround for 
 * <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6312706">
 * EnumMap bug</a>. If you want to pass an {@code EnumMap}, with the
 * intention of using its {@code entrySet()} method, you should
 * wrap the {@code EnumMap} in this class instead. 
 * 
 * @author Dimitris Andreou
 */
@GwtCompatible
final class WellBehavedMap<K, V> extends ForwardingMap<K, V> {
  private final Map<K, V> delegate;
  private Set<Entry<K, V>> entrySet;
  
  private WellBehavedMap(Map<K, V> delegate) {
    this.delegate = delegate;
  }
  
  /**
   * Wraps the given map into a {@code WellBehavedEntriesMap}, which
   * intercepts its {@code entrySet()} method by taking the 
   * {@code Set<K> keySet()} and transforming it to
   * {@code Set<Entry<K, V>>}. All other invocations are delegated as-is.  
   */
  static <K, V> WellBehavedMap<K, V> wrap(Map<K, V> delegate) {
    return new WellBehavedMap<K, V>(delegate);
  }

  @Override protected Map<K, V> delegate() {
    return delegate;
  }

  @Override public Set<Entry<K, V>> entrySet() {
    Set<Entry<K, V>> es = entrySet;
    if (es != null) {
      return es;
    }
    return entrySet = Sets.transform(
        delegate.keySet(), new KeyToEntryConverter<K, V>(this));
  }
  
  private static class KeyToEntryConverter<K, V> 
      extends Sets.InvertibleFunction<K, Map.Entry<K, V>> {
    final Map<K, V> map;
    
    KeyToEntryConverter(Map<K, V> map) {
      this.map = map;
    }

    @Override public Map.Entry<K, V> apply(final K key) {
      return new AbstractMapEntry<K, V>() {
        @Override public K getKey() {
          return key;
        }
        @Override public V getValue() {
          return map.get(key);
        }
        @Override public V setValue(V value) {
          return map.put(key, value);
        }
      };
    }

    @Override public K invert(Map.Entry<K, V> entry) {
      return entry.getKey();
    }
  }
}

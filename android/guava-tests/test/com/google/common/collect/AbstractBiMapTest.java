/*
 * Copyright (C) 2012 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterators.transform;

import com.google.common.collect.Maps.IteratorBasedAbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import junit.framework.TestCase;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Tests for {@code AbstractBiMap}.
 *
 * @author Mike Bostock
 */
@NullMarked
public class AbstractBiMapTest extends TestCase {

  public void testEntryNotAccessedAfterKeySetIteratorRemove() {
    BiMap<Integer, String> bimap =
        new AbstractBiMap<Integer, String>(
            new InvalidatingEntryMap<>(), new InvalidatingEntryMap<>()) {};
    bimap.put(1, "one");
    bimap.put(2, "two");
    bimap.put(3, "three");
    Iterator<Integer> iterator = bimap.keySet().iterator();
    iterator.next();
    iterator.next();
    iterator.remove();
    iterator.next();
    iterator.remove();
    assertEquals(1, bimap.size());
    assertEquals(1, bimap.inverse().size());
  }

  public void testEntryNotAccessedAfterEntrySetIteratorRemove() {
    BiMap<Integer, String> bimap =
        new AbstractBiMap<Integer, String>(
            new InvalidatingEntryMap<>(), new InvalidatingEntryMap<>()) {};
    bimap.put(1, "one");
    bimap.put(2, "two");
    bimap.put(3, "three");
    Iterator<Entry<Integer, String>> iterator = bimap.entrySet().iterator();
    iterator.next();
    iterator.next();
    iterator.remove();
    iterator.next();
    iterator.remove();
    assertEquals(1, bimap.size());
    assertEquals(1, bimap.inverse().size());
  }

  private static final class InvalidatingEntryMap<K, V> extends IteratorBasedAbstractMap<K, V> {
    final Map<K, V> delegate = new HashMap<>();

    @Override
    public int size() {
      return delegate.size();
    }

    @Override
    public @Nullable V put(K key, V value) {
      return delegate.put(key, value);
    }

    @Override
    Iterator<Entry<K, V>> entryIterator() {
      return transform(delegate.entrySet().iterator(), InvalidatingEntry::new);
    }

    private final class InvalidatingEntry extends SimpleImmutableEntry<K, V> {
      InvalidatingEntry(Entry<? extends K, ? extends V> entry) {
        super(entry);
      }

      @Override
      public K getKey() {
        checkState(delegate.containsKey(super.getKey()));
        return super.getKey();
      }

      @Override
      public V getValue() {
        checkState(delegate.containsKey(super.getKey()));
        return super.getValue();
      }
    }
  }
}

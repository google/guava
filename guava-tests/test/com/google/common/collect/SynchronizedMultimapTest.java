/*
 * Copyright (C) 2007 The Guava Authors
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

import static org.junit.contrib.truth.Truth.ASSERT;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Tests for {@code Synchronized#multimap}.
 *
 * @author Mike Bostock
 */
public class SynchronizedMultimapTest extends AbstractSetMultimapTest {

  @Override protected Multimap<String, Integer> create() {
    TestMultimap<String, Integer> inner = new TestMultimap<String, Integer>();
    Multimap<String, Integer> outer = Synchronized.multimap(inner, inner.mutex);
    return outer;
  }

  private static final class TestMultimap<K, V> extends ForwardingMultimap<K, V>
      implements Serializable {
    final Multimap<K, V> delegate = HashMultimap.create();
    public final Object mutex = new Integer(1); // something Serializable

    @Override protected Multimap<K, V> delegate() {
      return delegate;
    }

    @Override public String toString() {
      assertTrue(Thread.holdsLock(mutex));
      return super.toString();
    }

    @Override public boolean equals(@Nullable Object o) {
      assertTrue(Thread.holdsLock(mutex));
      return super.equals(o);
    }

    @Override public int hashCode() {
      assertTrue(Thread.holdsLock(mutex));
      return super.hashCode();
    }

    @Override public int size() {
      assertTrue(Thread.holdsLock(mutex));
      return super.size();
    }

    @Override public boolean isEmpty() {
      assertTrue(Thread.holdsLock(mutex));
      return super.isEmpty();
    }

    @Override public boolean containsKey(@Nullable Object key) {
      assertTrue(Thread.holdsLock(mutex));
      return super.containsKey(key);
    }

    @Override public boolean containsValue(@Nullable Object value) {
      assertTrue(Thread.holdsLock(mutex));
      return super.containsValue(value);
    }

    @Override public boolean containsEntry(@Nullable Object key,
        @Nullable Object value) {
      assertTrue(Thread.holdsLock(mutex));
      return super.containsEntry(key, value);
    }

    @Override public Collection<V> get(@Nullable K key) {
      assertTrue(Thread.holdsLock(mutex));
      /* TODO: verify that the Collection is also synchronized? */
      return super.get(key);
    }

    @Override public boolean put(K key, V value) {
      assertTrue(Thread.holdsLock(mutex));
      return super.put(key, value);
    }

    @Override public boolean putAll(@Nullable K key,
        Iterable<? extends V> values) {
      assertTrue(Thread.holdsLock(mutex));
      return super.putAll(key, values);
    }

    @Override public boolean putAll(Multimap<? extends K, ? extends V> map) {
      assertTrue(Thread.holdsLock(mutex));
      return super.putAll(map);
    }

    @Override public Collection<V> replaceValues(@Nullable K key,
        Iterable<? extends V> values) {
      assertTrue(Thread.holdsLock(mutex));
      return super.replaceValues(key, values);
    }

    @Override public boolean remove(@Nullable Object key,
        @Nullable Object value) {
      assertTrue(Thread.holdsLock(mutex));
      return super.remove(key, value);
    }

    @Override public Collection<V> removeAll(@Nullable Object key) {
      assertTrue(Thread.holdsLock(mutex));
      return super.removeAll(key);
    }

    @Override public void clear() {
      assertTrue(Thread.holdsLock(mutex));
      super.clear();
    }

    @Override public Set<K> keySet() {
      assertTrue(Thread.holdsLock(mutex));
      /* TODO: verify that the Set is also synchronized? */
      return super.keySet();
    }

    @Override public Multiset<K> keys() {
      assertTrue(Thread.holdsLock(mutex));
      /* TODO: verify that the Set is also synchronized? */
      return super.keys();
    }

    @Override public Collection<V> values() {
      assertTrue(Thread.holdsLock(mutex));
      /* TODO: verify that the Collection is also synchronized? */
      return super.values();
    }

    @Override public Collection<Map.Entry<K, V>> entries() {
      assertTrue(Thread.holdsLock(mutex));
      /* TODO: verify that the Collection is also synchronized? */
      return super.entries();
    }

    @Override public Map<K, Collection<V>> asMap() {
      assertTrue(Thread.holdsLock(mutex));
      /* TODO: verify that the Map is also synchronized? */
      return super.asMap();
    }

    private static final long serialVersionUID = 0;
  }

  public void testSynchronizedListMultimap() {
    ListMultimap<String, Integer> multimap
        = Multimaps.synchronizedListMultimap(
            ArrayListMultimap.<String, Integer>create());
    multimap.putAll("foo", Arrays.asList(3, -1, 2, 4, 1));
    multimap.putAll("bar", Arrays.asList(1, 2, 3, 1));
    ASSERT.that(multimap.removeAll("foo")).hasContentsInOrder(3, -1, 2, 4, 1);
    assertFalse(multimap.containsKey("foo"));
    ASSERT.that(multimap.replaceValues("bar", Arrays.asList(6, 5)))
        .hasContentsInOrder(1, 2, 3, 1);
    ASSERT.that(multimap.get("bar")).hasContentsInOrder(6, 5);
  }

  public void testSynchronizedSortedSetMultimap() {
    SortedSetMultimap<String, Integer> multimap
        = Multimaps.synchronizedSortedSetMultimap(
            TreeMultimap.<String, Integer>create());
    multimap.putAll("foo", Arrays.asList(3, -1, 2, 4, 1));
    multimap.putAll("bar", Arrays.asList(1, 2, 3, 1));
    ASSERT.that(multimap.removeAll("foo")).hasContentsInOrder(-1, 1, 2, 3, 4);
    assertFalse(multimap.containsKey("foo"));
    ASSERT.that(multimap.replaceValues("bar", Arrays.asList(6, 5)))
        .hasContentsInOrder(1, 2, 3);
    ASSERT.that(multimap.get("bar")).hasContentsInOrder(5, 6);
  }

  public void testSynchronizedArrayListMultimapRandomAccess() {
    ListMultimap<String, Integer> delegate = ArrayListMultimap.create();
    delegate.put("foo", 1);
    delegate.put("foo", 3);
    ListMultimap<String, Integer> multimap
        = Multimaps.synchronizedListMultimap(delegate);
    assertTrue(multimap.get("foo") instanceof RandomAccess);
    assertTrue(multimap.get("bar") instanceof RandomAccess);
  }

  public void testSynchronizedLinkedListMultimapRandomAccess() {
    ListMultimap<String, Integer> delegate = LinkedListMultimap.create();
    delegate.put("foo", 1);
    delegate.put("foo", 3);
    ListMultimap<String, Integer> multimap
        = Multimaps.synchronizedListMultimap(delegate);
    assertFalse(multimap.get("foo") instanceof RandomAccess);
    assertFalse(multimap.get("bar") instanceof RandomAccess);
  }
}

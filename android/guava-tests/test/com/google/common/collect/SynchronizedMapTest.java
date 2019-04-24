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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Synchronized.SynchronizedCollection;
import com.google.common.collect.Synchronized.SynchronizedSet;
import com.google.common.testing.SerializableTester;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import junit.framework.TestCase;

/**
 * Tests for {@code Synchronized#map}.
 *
 * @author Mike Bostock
 */
public class SynchronizedMapTest extends TestCase {
  public final Object mutex = new Integer(1); // something Serializable

  protected <K, V> Map<K, V> create() {
    TestMap<K, V> inner = new TestMap<>(new HashMap<K, V>(), mutex);
    Map<K, V> outer = Synchronized.map(inner, mutex);
    return outer;
  }

  static class TestMap<K, V> extends ForwardingMap<K, V> implements Serializable {
    public final Object mutex;
    private Map<K, V> delegate;

    public TestMap(Map<K, V> delegate, Object mutex) {
      checkNotNull(mutex);
      this.delegate = delegate;
      this.mutex = mutex;
    }

    @Override
    protected Map<K, V> delegate() {
      return delegate;
    }

    @Override
    public int size() {
      assertTrue(Thread.holdsLock(mutex));
      return super.size();
    }

    @Override
    public boolean isEmpty() {
      assertTrue(Thread.holdsLock(mutex));
      return super.isEmpty();
    }

    @Override
    public V remove(Object object) {
      assertTrue(Thread.holdsLock(mutex));
      return super.remove(object);
    }

    @Override
    public void clear() {
      assertTrue(Thread.holdsLock(mutex));
      super.clear();
    }

    @Override
    public boolean containsKey(Object key) {
      assertTrue(Thread.holdsLock(mutex));
      return super.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
      assertTrue(Thread.holdsLock(mutex));
      return super.containsValue(value);
    }

    @Override
    public V get(Object key) {
      assertTrue(Thread.holdsLock(mutex));
      return super.get(key);
    }

    @Override
    public V put(K key, V value) {
      assertTrue(Thread.holdsLock(mutex));
      return super.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
      assertTrue(Thread.holdsLock(mutex));
      super.putAll(map);
    }

    @Override
    public Set<K> keySet() {
      assertTrue(Thread.holdsLock(mutex));
      return super.keySet();
    }

    @Override
    public Collection<V> values() {
      assertTrue(Thread.holdsLock(mutex));
      return super.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
      assertTrue(Thread.holdsLock(mutex));
      return super.entrySet();
    }

    @Override
    public boolean equals(Object obj) {
      assertTrue(Thread.holdsLock(mutex));
      return super.equals(obj);
    }

    @Override
    public int hashCode() {
      assertTrue(Thread.holdsLock(mutex));
      return super.hashCode();
    }

    @Override
    public String toString() {
      assertTrue(Thread.holdsLock(mutex));
      return super.toString();
    }

    private static final long serialVersionUID = 0;
  }

  /*
   * This is somewhat of a weak test; we verify that all of the methods are
   * correct, but not that they're actually forwarding correctly. We also rely
   * on the other tests (e.g., SynchronizedSetTest) to verify that the
   * collection views are synchronized correctly.
   */

  public void testSize() {
    create().size();
  }

  public void testIsEmpty() {
    create().isEmpty();
  }

  public void testRemove() {
    create().remove(null);
  }

  public void testClear() {
    create().clear();
  }

  public void testContainsKey() {
    create().containsKey(null);
  }

  public void testContainsValue() {
    create().containsValue(null);
  }

  public void testGet() {
    create().get(null);
  }

  public void testPut() {
    create().put(null, null);
  }

  public void testPutAll() {
    create().putAll(new HashMap<String, Integer>());
  }

  public void testKeySet() {
    Map<String, Integer> map = create();
    Set<String> keySet = map.keySet();
    assertTrue(keySet instanceof SynchronizedSet);
    assertSame(mutex, ((SynchronizedSet<?>) keySet).mutex);
  }

  public void testValues() {
    Map<String, Integer> map = create();
    Collection<Integer> values = map.values();
    assertTrue(values instanceof SynchronizedCollection);
    assertSame(mutex, ((SynchronizedCollection<?>) values).mutex);
  }

  public void testEntrySet() {
    Map<String, Integer> map = create();
    Set<Entry<String, Integer>> entrySet = map.entrySet();
    assertTrue(entrySet instanceof SynchronizedSet);
    assertSame(mutex, ((SynchronizedSet<?>) entrySet).mutex);
  }

  public void testEquals() {
    create().equals(new HashMap<String, Integer>());
  }

  public void testHashCode() {
    create().hashCode();
  }

  public void testToString() {
    create().toString();
  }

  public void testSerialization() {
    SerializableTester.reserializeAndAssert(create());
  }
}

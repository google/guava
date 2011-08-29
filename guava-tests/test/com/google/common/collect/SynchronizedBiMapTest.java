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

import com.google.common.collect.Synchronized.SynchronizedBiMap;
import com.google.common.collect.Synchronized.SynchronizedSet;

import junit.framework.TestSuite;

import java.util.Set;

/**
 * Tests for {@code Synchronized#biMap}.
 *
 * @author Mike Bostock
 */
public class SynchronizedBiMapTest extends SynchronizedMapTest {

  public static TestSuite suite() {
    TestSuite suite = new TestSuite(SynchronizedBiMapTest.class);
    suite.addTestSuite(AbstractBiMapTests.class);
    return suite;
  }

  @Override protected <K, V> BiMap<K, V> create() {
    TestBiMap<K, V> inner =
        new TestBiMap<K, V>(HashBiMap.<K, V>create(), mutex);
    BiMap<K, V> outer = Synchronized.biMap(inner, mutex);
    return outer;
  }

  static class TestBiMap<K, V> extends TestMap<K, V> implements BiMap<K, V> {
    private final BiMap<K, V> delegate;

    public TestBiMap(BiMap<K, V> delegate, Object mutex) {
      super(delegate, mutex);
      this.delegate = delegate;
    }

    @Override
    public V forcePut(K key, V value) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.forcePut(key, value);
    }

    @Override
    public BiMap<V, K> inverse() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.inverse();
    }

    @Override public Set<V> values() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.values();
    }

    private static final long serialVersionUID = 0;
  }

  public void testForcePut() {
    create().forcePut(null, null);
  }

  public void testInverse() {
    BiMap<String, Integer> bimap = create();
    BiMap<Integer, String> inverse = bimap.inverse();
    assertSame(bimap, inverse.inverse());
    assertTrue(inverse instanceof SynchronizedBiMap);
    assertSame(mutex, ((SynchronizedBiMap<?, ?>) inverse).mutex);
  }

  @Override public void testValues() {
    BiMap<String, Integer> map = create();
    Set<Integer> values = map.values();
    assertTrue(values instanceof SynchronizedSet);
    assertSame(mutex, ((SynchronizedSet<?>) values).mutex);
  }

  public static class AbstractBiMapTests extends AbstractBiMapTest {
    public final Object mutex = new Integer(1); // something Serializable

    @Override protected BiMap<Integer, String> create() {
      TestBiMap<Integer, String> inner = new TestBiMap<Integer, String>(
          HashBiMap.<Integer, String>create(), mutex);
      BiMap<Integer, String> outer = Synchronized.biMap(inner, mutex);
      return outer;
    }

    /**
     * If you serialize a synchronized bimap and its inverse together, the
     * reserialized bimaps will have backing maps that stay in sync, as shown
     * by the {@code testSerializationWithInverseEqual()} test. However, the
     * inverse of one won't be the same as the other.
     *
     * To make them the same, the inverse synchronized bimap would need a custom
     * serialized form, similar to what {@code AbstractBiMap.Inverse} does.
     */
    @Override public void testSerializationWithInverseSame() {}
  }
}

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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.google.SetMultimapTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringSetMultimapGenerator;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.RandomAccess;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * Tests for {@code Synchronized#multimap}.
 *
 * @author Mike Bostock
 */
public class SynchronizedMultimapTest extends TestCase {

  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(SynchronizedMultimapTest.class);
    suite.addTest(
        SetMultimapTestSuiteBuilder.using(
                new TestStringSetMultimapGenerator() {
                  @Override
                  protected SetMultimap<String, String> create(Entry<String, String>[] entries) {
                    TestMultimap<String, String> inner = new TestMultimap<>();
                    SetMultimap<String, String> outer =
                        Synchronized.setMultimap(inner, inner.mutex);
                    for (Entry<String, String> entry : entries) {
                      outer.put(entry.getKey(), entry.getValue());
                    }
                    return outer;
                  }
                })
            .named("Synchronized.setMultimap")
            .withFeatures(
                MapFeature.GENERAL_PURPOSE,
                CollectionSize.ANY,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_ANY_NULL_QUERIES)
            .createTestSuite());
    return suite;
  }

  private static final class TestMultimap<K, V> extends ForwardingSetMultimap<K, V>
      implements Serializable {
    final SetMultimap<K, V> delegate = HashMultimap.create();
    public final Object mutex = new Integer(1); // something Serializable

    @Override
    protected SetMultimap<K, V> delegate() {
      return delegate;
    }

    @Override
    public String toString() {
      assertTrue(Thread.holdsLock(mutex));
      return super.toString();
    }

    @Override
    public boolean equals(@NullableDecl Object o) {
      assertTrue(Thread.holdsLock(mutex));
      return super.equals(o);
    }

    @Override
    public int hashCode() {
      assertTrue(Thread.holdsLock(mutex));
      return super.hashCode();
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
    public boolean containsKey(@NullableDecl Object key) {
      assertTrue(Thread.holdsLock(mutex));
      return super.containsKey(key);
    }

    @Override
    public boolean containsValue(@NullableDecl Object value) {
      assertTrue(Thread.holdsLock(mutex));
      return super.containsValue(value);
    }

    @Override
    public boolean containsEntry(@NullableDecl Object key, @NullableDecl Object value) {
      assertTrue(Thread.holdsLock(mutex));
      return super.containsEntry(key, value);
    }

    @Override
    public Set<V> get(@NullableDecl K key) {
      assertTrue(Thread.holdsLock(mutex));
      /* TODO: verify that the Collection is also synchronized? */
      return super.get(key);
    }

    @Override
    public boolean put(K key, V value) {
      assertTrue(Thread.holdsLock(mutex));
      return super.put(key, value);
    }

    @Override
    public boolean putAll(@NullableDecl K key, Iterable<? extends V> values) {
      assertTrue(Thread.holdsLock(mutex));
      return super.putAll(key, values);
    }

    @Override
    public boolean putAll(Multimap<? extends K, ? extends V> map) {
      assertTrue(Thread.holdsLock(mutex));
      return super.putAll(map);
    }

    @Override
    public Set<V> replaceValues(@NullableDecl K key, Iterable<? extends V> values) {
      assertTrue(Thread.holdsLock(mutex));
      return super.replaceValues(key, values);
    }

    @Override
    public boolean remove(@NullableDecl Object key, @NullableDecl Object value) {
      assertTrue(Thread.holdsLock(mutex));
      return super.remove(key, value);
    }

    @Override
    public Set<V> removeAll(@NullableDecl Object key) {
      assertTrue(Thread.holdsLock(mutex));
      return super.removeAll(key);
    }

    @Override
    public void clear() {
      assertTrue(Thread.holdsLock(mutex));
      super.clear();
    }

    @Override
    public Set<K> keySet() {
      assertTrue(Thread.holdsLock(mutex));
      /* TODO: verify that the Set is also synchronized? */
      return super.keySet();
    }

    @Override
    public Multiset<K> keys() {
      assertTrue(Thread.holdsLock(mutex));
      /* TODO: verify that the Set is also synchronized? */
      return super.keys();
    }

    @Override
    public Collection<V> values() {
      assertTrue(Thread.holdsLock(mutex));
      /* TODO: verify that the Collection is also synchronized? */
      return super.values();
    }

    @Override
    public Set<Entry<K, V>> entries() {
      assertTrue(Thread.holdsLock(mutex));
      /* TODO: verify that the Collection is also synchronized? */
      return super.entries();
    }

    @Override
    public Map<K, Collection<V>> asMap() {
      assertTrue(Thread.holdsLock(mutex));
      /* TODO: verify that the Map is also synchronized? */
      return super.asMap();
    }

    private static final long serialVersionUID = 0;
  }

  public void testSynchronizedListMultimap() {
    ListMultimap<String, Integer> multimap =
        Multimaps.synchronizedListMultimap(ArrayListMultimap.<String, Integer>create());
    multimap.putAll("foo", Arrays.asList(3, -1, 2, 4, 1));
    multimap.putAll("bar", Arrays.asList(1, 2, 3, 1));
    assertThat(multimap.removeAll("foo")).containsExactly(3, -1, 2, 4, 1).inOrder();
    assertFalse(multimap.containsKey("foo"));
    assertThat(multimap.replaceValues("bar", Arrays.asList(6, 5)))
        .containsExactly(1, 2, 3, 1)
        .inOrder();
    assertThat(multimap.get("bar")).containsExactly(6, 5).inOrder();
  }

  public void testSynchronizedSortedSetMultimap() {
    SortedSetMultimap<String, Integer> multimap =
        Multimaps.synchronizedSortedSetMultimap(TreeMultimap.<String, Integer>create());
    multimap.putAll("foo", Arrays.asList(3, -1, 2, 4, 1));
    multimap.putAll("bar", Arrays.asList(1, 2, 3, 1));
    assertThat(multimap.removeAll("foo")).containsExactly(-1, 1, 2, 3, 4).inOrder();
    assertFalse(multimap.containsKey("foo"));
    assertThat(multimap.replaceValues("bar", Arrays.asList(6, 5)))
        .containsExactly(1, 2, 3)
        .inOrder();
    assertThat(multimap.get("bar")).containsExactly(5, 6).inOrder();
  }

  public void testSynchronizedArrayListMultimapRandomAccess() {
    ListMultimap<String, Integer> delegate = ArrayListMultimap.create();
    delegate.put("foo", 1);
    delegate.put("foo", 3);
    ListMultimap<String, Integer> multimap = Multimaps.synchronizedListMultimap(delegate);
    assertTrue(multimap.get("foo") instanceof RandomAccess);
    assertTrue(multimap.get("bar") instanceof RandomAccess);
  }

  public void testSynchronizedLinkedListMultimapRandomAccess() {
    ListMultimap<String, Integer> delegate = LinkedListMultimap.create();
    delegate.put("foo", 1);
    delegate.put("foo", 3);
    ListMultimap<String, Integer> multimap = Multimaps.synchronizedListMultimap(delegate);
    assertFalse(multimap.get("foo") instanceof RandomAccess);
    assertFalse(multimap.get("bar") instanceof RandomAccess);
  }
}

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
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.TestStringMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Unit test for {@link ForwardingMap}.
 *
 * @author Hayward Chan
 * @author Louis Wasserman
 */
public class ForwardingMapTest extends ForwardingTestCase {
  static class StandardImplForwardingMap<K, V> extends ForwardingMap<K, V> {
    private final Map<K, V> backingMap;

    StandardImplForwardingMap(Map<K, V> backingMap) {
      this.backingMap = backingMap;
    }

    @Override protected Map<K, V> delegate() {
      return backingMap;
    }

    @Override public boolean containsKey(Object key) {
      return standardContainsKey(key);
    }

    @Override public boolean containsValue(Object value) {
      return standardContainsValue(value);
    }

    @Override public void putAll(Map<? extends K, ? extends V> map) {
      standardPutAll(map);
    }

    @Override public V remove(Object object) {
      return standardRemove(object);
    }

    @Override public boolean equals(Object object) {
      return standardEquals(object);
    }

    @Override public int hashCode() {
      return standardHashCode();
    }

    @Override public Set<K> keySet() {
      return new StandardKeySet();
    }

    @Override public Collection<V> values() {
      return new StandardValues();
    }

    @Override public String toString() {
      return standardToString();
    }

    @Override public Set<Entry<K, V>> entrySet() {
      return new StandardEntrySet() {
        @Override
        public Iterator<Entry<K, V>> iterator() {
          return delegate()
              .entrySet()
              .iterator();
        }
      };
    }

    @Override public void clear() {
      standardClear();
    }

    @Override public boolean isEmpty() {
      return standardIsEmpty();
    }
  }

  Map<String, Boolean> forward;

  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTestSuite(ForwardingMapTest.class);
    suite.addTest(MapTestSuiteBuilder.using(new TestStringMapGenerator() {

      @Override protected Map<String, String> create(
          Entry<String, String>[] entries) {
        Map<String, String> map = Maps.newLinkedHashMap();
        for (Entry<String, String> entry : entries) {
          map.put(entry.getKey(), entry.getValue());
        }
        return new StandardImplForwardingMap<String, String>(map);
      }

    }).named("ForwardingMap[LinkedHashMap] with standard implementations")
        .withFeatures(CollectionSize.ANY, MapFeature.ALLOWS_NULL_VALUES,
            MapFeature.ALLOWS_NULL_KEYS, MapFeature.ALLOWS_ANY_NULL_QUERIES,
            MapFeature.GENERAL_PURPOSE,
            CollectionFeature.SUPPORTS_ITERATOR_REMOVE, CollectionFeature.KNOWN_ORDER)
        .createTestSuite());
    suite.addTest(MapTestSuiteBuilder.using(new TestStringMapGenerator() {

      @Override protected Map<String, String> create(
          Entry<String, String>[] entries) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        for (Entry<String, String> entry : entries) {
          builder.put(entry.getKey(), entry.getValue());
        }
        return new StandardImplForwardingMap<String, String>(builder.build());
      }

    }).named("ForwardingMap[ImmutableMap] with standard implementations")
        .withFeatures(
            CollectionSize.ANY, MapFeature.REJECTS_DUPLICATES_AT_CREATION,
            MapFeature.ALLOWS_ANY_NULL_QUERIES,
            CollectionFeature.KNOWN_ORDER)
        .createTestSuite());

    return suite;
  }

  @Override public void setUp() throws Exception {
    super.setUp();
    /*
     * Class parameters must be raw, so we can't create a proxy with generic
     * type arguments. The created proxy only records calls and returns null, so
     * the type is irrelevant at runtime.
     */
    @SuppressWarnings("unchecked")
    final Map<String, Boolean> map = createProxyInstance(Map.class);
    forward = new ForwardingMap<String, Boolean>() {
      @Override protected Map<String, Boolean> delegate() {
        return map;
      }
    };
  }

  public void testSize() {
    forward().size();
    assertEquals("[size]", getCalls());
  }

  public void testIsEmpty() {
    forward().isEmpty();
    assertEquals("[isEmpty]", getCalls());
  }

  public void testRemove() {
    forward().remove(null);
    assertEquals("[remove(Object)]", getCalls());
  }

  public void testClear() {
    forward().clear();
    assertEquals("[clear]", getCalls());
  }

  public void testContainsKey() {
    forward().containsKey("asdf");
    assertEquals("[containsKey(Object)]", getCalls());
  }

  public void testContainsValue() {
    forward().containsValue(false);
    assertEquals("[containsValue(Object)]", getCalls());
  }

  public void testGet_Object() {
    forward().get("asdf");
    assertEquals("[get(Object)]", getCalls());
  }

  public void testPut_Key_Value() {
    forward().put("key", false);
    assertEquals("[put(Object,Object)]", getCalls());
  }

  public void testPutAll_Map() {
    forward().putAll(new HashMap<String, Boolean>());
    assertEquals("[putAll(Map)]", getCalls());
  }

  public void testKeySet() {
    forward().keySet();
    assertEquals("[keySet]", getCalls());
  }

  public void testValues() {
    forward().values();
    assertEquals("[values]", getCalls());
  }

  public void testEntrySet() {
    forward().entrySet();
    assertEquals("[entrySet]", getCalls());
  }

  public void testToString() {
    forward().toString();
    assertEquals("[toString]", getCalls());
  }

  public void testEquals_Object() {
    forward().equals("asdf");
    assertEquals("[equals(Object)]", getCalls());
  }

  public void testHashCode() {
    forward().hashCode();
    assertEquals("[hashCode]", getCalls());
  }

  public void testStandardEntrySet() throws InvocationTargetException {
    @SuppressWarnings("unchecked")
    final Map<String, Boolean> map = createMock(Map.class);
    @SuppressWarnings("unchecked")
    final Set<Map.Entry<String, Boolean>> entrySet = createMock(Set.class);
    expect(map.containsKey(anyObject())).andReturn(false).anyTimes();
    expect(map.get(anyObject())).andReturn(null).anyTimes();
    expect(map.isEmpty()).andReturn(true).anyTimes();
    expect(map.remove(anyObject())).andReturn(null).anyTimes();
    expect(map.size()).andReturn(0).anyTimes();
    expect(entrySet.iterator())
        .andReturn(Iterators.<Entry<String, Boolean>>emptyIterator())
        .anyTimes();
    map.clear();
    expectLastCall().anyTimes();

    replay(map, entrySet);

    Map<String, Boolean> forward = new ForwardingMap<String, Boolean>() {
      @Override protected Map<String, Boolean> delegate() {
        return map;
      }

      @Override public Set<Entry<String, Boolean>> entrySet() {
        return new StandardEntrySet() {
          @Override
          public Iterator<Entry<String, Boolean>> iterator() {
            return entrySet.iterator();
          }
        };
      }
    };
    callAllPublicMethods(Set.class, forward.entrySet());

    verify(map, entrySet);
  }

  public void testStandardKeySet() throws InvocationTargetException {
    @SuppressWarnings("unchecked")
    Set<Entry<String, Boolean>> entrySet = createMock(Set.class);
    expect(entrySet.iterator()).andReturn(
        Iterators.<Entry<String, Boolean>>emptyIterator()).anyTimes();

    @SuppressWarnings("unchecked")
    final Map<String, Boolean> map = createMock(Map.class);
    expect(map.containsKey(anyObject())).andReturn(false).anyTimes();
    expect(map.isEmpty()).andReturn(true).anyTimes();
    expect(map.remove(anyObject())).andReturn(null).anyTimes();
    expect(map.size()).andReturn(0).anyTimes();
    expect(map.entrySet()).andReturn(entrySet).anyTimes();
    map.clear();
    expectLastCall().anyTimes();

    replay(entrySet, map);

    Map<String, Boolean> forward = new ForwardingMap<String, Boolean>() {
      @Override protected Map<String, Boolean> delegate() {
        return map;
      }

      @Override public Set<String> keySet() {
        return new StandardKeySet();
      }
    };
    callAllPublicMethods(Set.class, forward.keySet());

    verify(entrySet, map);
  }

  public void testStandardValues() throws InvocationTargetException {
    @SuppressWarnings("unchecked")
    Set<Entry<String, Boolean>> entrySet = createMock(Set.class);
    expect(entrySet.iterator()).andReturn(
        Iterators.<Entry<String, Boolean>>emptyIterator()).anyTimes();

    @SuppressWarnings("unchecked")
    final Map<String, Boolean> map = createMock(Map.class);
    expect(map.containsValue(anyObject())).andReturn(false).anyTimes();
    expect(map.isEmpty()).andReturn(true).anyTimes();
    expect(map.size()).andReturn(0).anyTimes();
    expect(map.entrySet()).andReturn(entrySet).anyTimes();
    map.clear();
    expectLastCall().anyTimes();

    replay(entrySet, map);

    Map<String, Boolean> forward = new ForwardingMap<String, Boolean>() {
      @Override protected Map<String, Boolean> delegate() {
        return map;
      }

      @Override public Collection<Boolean> values() {
        return new StandardValues();
      }
    };
    callAllPublicMethods(Collection.class, forward.values());

    verify(entrySet, map);
  }

  public void testToStringWithNullKeys() throws Exception {
    Map<String, String> hashmap = Maps.newHashMap();
    hashmap.put("foo", "bar");
    hashmap.put(null, "baz");

    StandardImplForwardingMap<String, String> forwardingMap =
        new StandardImplForwardingMap<String, String>(
            Maps.<String, String>newHashMap());
    forwardingMap.put("foo", "bar");
    forwardingMap.put(null, "baz");

    assertEquals(hashmap.toString(), forwardingMap.toString());
  }

  public void testToStringWithNullValues() throws Exception {
    Map<String, String> hashmap = Maps.newHashMap();
    hashmap.put("foo", "bar");
    hashmap.put("baz", null);

    StandardImplForwardingMap<String, String> forwardingMap =
        new StandardImplForwardingMap<String, String>(
            Maps.<String, String>newHashMap());
    forwardingMap.put("foo", "bar");
    forwardingMap.put("baz", null);

    assertEquals(hashmap.toString(), forwardingMap.toString());
  }

  Map<String, Boolean> forward() {
    return forward;
  }
}

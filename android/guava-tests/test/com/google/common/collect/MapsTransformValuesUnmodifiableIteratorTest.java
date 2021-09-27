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
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.testing.MapInterfaceTest;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.CheckForNull;

/**
 * Tests for {@link Maps#transformValues} when the backing map's views have iterators that don't
 * support {@code remove()}.
 *
 * @author Jared Levy
 */
@GwtCompatible
public class MapsTransformValuesUnmodifiableIteratorTest extends MapInterfaceTest<String, String> {
  // TODO(jlevy): Move shared code of this class and MapsTransformValuesTest
  // to a superclass.

  public MapsTransformValuesUnmodifiableIteratorTest() {
    super(true, true, false /*supportsPut*/, true, true, false);
  }

  private static class UnmodifiableIteratorMap<K, V> extends ForwardingMap<K, V> {
    final Map<K, V> delegate;

    UnmodifiableIteratorMap(Map<K, V> delegate) {
      this.delegate = delegate;
    }

    @Override
    protected Map<K, V> delegate() {
      return delegate;
    }

    @Override
    public Set<K> keySet() {
      return new ForwardingSet<K>() {
        @Override
        protected Set<K> delegate() {
          return delegate.keySet();
        }

        @Override
        public Iterator<K> iterator() {
          return Iterators.unmodifiableIterator(delegate.keySet().iterator());
        }

        @Override
        public boolean removeAll(Collection<?> c) {
          return delegate.keySet().removeAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
          return delegate.keySet().retainAll(c);
        }
      };
    }

    @Override
    public Collection<V> values() {
      return new ForwardingCollection<V>() {
        @Override
        protected Collection<V> delegate() {
          return delegate.values();
        }

        @Override
        public Iterator<V> iterator() {
          return Iterators.unmodifiableIterator(delegate.values().iterator());
        }

        @Override
        public boolean removeAll(Collection<?> c) {
          return delegate.values().removeAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
          return delegate.values().retainAll(c);
        }
      };
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
      return new ForwardingSet<Entry<K, V>>() {
        @Override
        protected Set<Entry<K, V>> delegate() {
          return delegate.entrySet();
        }

        @Override
        public Iterator<Entry<K, V>> iterator() {
          return Iterators.unmodifiableIterator(delegate.entrySet().iterator());
        }

        @Override
        public boolean removeAll(Collection<?> c) {
          return delegate.entrySet().removeAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
          return delegate.entrySet().retainAll(c);
        }
      };
    }
  }

  @Override
  protected Map<String, String> makeEmptyMap() {
    Map<String, Integer> underlying = Maps.newHashMap();
    return Maps.transformValues(
        new UnmodifiableIteratorMap<String, Integer>(underlying), Functions.toStringFunction());
  }

  @Override
  protected Map<String, String> makePopulatedMap() {
    Map<String, Integer> underlying = Maps.newHashMap();
    underlying.put("a", 1);
    underlying.put("b", 2);
    underlying.put("c", 3);
    return Maps.transformValues(
        new UnmodifiableIteratorMap<String, Integer>(underlying), Functions.toStringFunction());
  }

  @Override
  protected String getKeyNotInPopulatedMap() throws UnsupportedOperationException {
    return "z";
  }

  @Override
  protected String getValueNotInPopulatedMap() throws UnsupportedOperationException {
    return "26";
  }

  /** Helper assertion comparing two maps */
  private void assertMapsEqual(Map<?, ?> expected, Map<?, ?> map) {
    assertEquals(expected, map);
    assertEquals(expected.hashCode(), map.hashCode());
    assertEquals(expected.entrySet(), map.entrySet());

    // Assert that expectedValues > mapValues and that
    // mapValues > expectedValues; i.e. that expectedValues == mapValues.
    Collection<?> expectedValues = expected.values();
    Collection<?> mapValues = map.values();
    assertEquals(expectedValues.size(), mapValues.size());
    assertTrue(expectedValues.containsAll(mapValues));
    assertTrue(mapValues.containsAll(expectedValues));
  }

  public void testTransformEmptyMapEquality() {
    Map<String, String> map =
        Maps.transformValues(ImmutableMap.<String, Integer>of(), Functions.toStringFunction());
    assertMapsEqual(Maps.newHashMap(), map);
  }

  public void testTransformSingletonMapEquality() {
    Map<String, String> map =
        Maps.transformValues(ImmutableMap.of("a", 1), Functions.toStringFunction());
    Map<String, String> expected = ImmutableMap.of("a", "1");
    assertMapsEqual(expected, map);
    assertEquals(expected.get("a"), map.get("a"));
  }

  public void testTransformIdentityFunctionEquality() {
    Map<String, Integer> underlying = ImmutableMap.of("a", 1);
    Map<String, Integer> map = Maps.transformValues(underlying, Functions.<Integer>identity());
    assertMapsEqual(underlying, map);
  }

  public void testTransformPutEntryIsUnsupported() {
    Map<String, String> map =
        Maps.transformValues(ImmutableMap.of("a", 1), Functions.toStringFunction());
    try {
      map.put("b", "2");
      fail();
    } catch (UnsupportedOperationException expected) {
    }

    try {
      map.putAll(ImmutableMap.of("b", "2"));
      fail();
    } catch (UnsupportedOperationException expected) {
    }

    try {
      map.entrySet().iterator().next().setValue("one");
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }

  public void testTransformRemoveEntry() {
    Map<String, Integer> underlying = Maps.newHashMap();
    underlying.put("a", 1);
    Map<String, String> map = Maps.transformValues(underlying, Functions.toStringFunction());
    assertEquals("1", map.remove("a"));
    assertNull(map.remove("b"));
  }

  public void testTransformEqualityOfMapsWithNullValues() {
    Map<String, String> underlying = Maps.newHashMap();
    underlying.put("a", null);
    underlying.put("b", "");

    Map<String, Boolean> map =
        Maps.transformValues(
            underlying,
            new Function<String, Boolean>() {
              @Override
              public Boolean apply(@CheckForNull String from) {
                return from == null;
              }
            });
    Map<String, Boolean> expected = ImmutableMap.of("a", true, "b", false);
    assertMapsEqual(expected, map);
    assertEquals(expected.get("a"), map.get("a"));
    assertEquals(expected.containsKey("a"), map.containsKey("a"));
    assertEquals(expected.get("b"), map.get("b"));
    assertEquals(expected.containsKey("b"), map.containsKey("b"));
    assertEquals(expected.get("c"), map.get("c"));
    assertEquals(expected.containsKey("c"), map.containsKey("c"));
  }

  public void testTransformReflectsUnderlyingMap() {
    Map<String, Integer> underlying = Maps.newHashMap();
    underlying.put("a", 1);
    underlying.put("b", 2);
    underlying.put("c", 3);
    Map<String, String> map = Maps.transformValues(underlying, Functions.toStringFunction());
    assertEquals(underlying.size(), map.size());

    underlying.put("d", 4);
    assertEquals(underlying.size(), map.size());
    assertEquals("4", map.get("d"));

    underlying.remove("c");
    assertEquals(underlying.size(), map.size());
    assertFalse(map.containsKey("c"));

    underlying.clear();
    assertEquals(underlying.size(), map.size());
  }

  public void testTransformChangesAreReflectedInUnderlyingMap() {
    Map<String, Integer> underlying = Maps.newLinkedHashMap();
    underlying.put("a", 1);
    underlying.put("b", 2);
    underlying.put("c", 3);
    underlying.put("d", 4);
    underlying.put("e", 5);
    underlying.put("f", 6);
    underlying.put("g", 7);
    Map<String, String> map = Maps.transformValues(underlying, Functions.toStringFunction());

    map.remove("a");
    assertFalse(underlying.containsKey("a"));

    Set<String> keys = map.keySet();
    keys.remove("b");
    assertFalse(underlying.containsKey("b"));

    Iterator<String> keyIterator = keys.iterator();
    keyIterator.next();
    keyIterator.remove();
    assertFalse(underlying.containsKey("c"));

    Collection<String> values = map.values();
    values.remove("4");
    assertFalse(underlying.containsKey("d"));

    Iterator<String> valueIterator = values.iterator();
    valueIterator.next();
    valueIterator.remove();
    assertFalse(underlying.containsKey("e"));

    Set<Entry<String, String>> entries = map.entrySet();
    Entry<String, String> firstEntry = entries.iterator().next();
    entries.remove(firstEntry);
    assertFalse(underlying.containsKey("f"));

    Iterator<Entry<String, String>> entryIterator = entries.iterator();
    entryIterator.next();
    entryIterator.remove();
    assertFalse(underlying.containsKey("g"));

    assertTrue(underlying.isEmpty());
    assertTrue(map.isEmpty());
    assertTrue(keys.isEmpty());
    assertTrue(values.isEmpty());
    assertTrue(entries.isEmpty());
  }

  public void testTransformEquals() {
    Map<String, Integer> underlying = ImmutableMap.of("a", 0, "b", 1, "c", 2);
    Map<String, Integer> expected = Maps.transformValues(underlying, Functions.<Integer>identity());

    assertMapsEqual(expected, expected);

    Map<String, Integer> equalToUnderlying = Maps.newTreeMap();
    equalToUnderlying.putAll(underlying);
    Map<String, Integer> map =
        Maps.transformValues(equalToUnderlying, Functions.<Integer>identity());
    assertMapsEqual(expected, map);

    map =
        Maps.transformValues(
            ImmutableMap.of("a", 1, "b", 2, "c", 3),
            new Function<Integer, Integer>() {
              @Override
              public Integer apply(Integer from) {
                return from - 1;
              }
            });
    assertMapsEqual(expected, map);
  }

  public void testTransformEntrySetContains() {
    Map<String, Boolean> underlying = Maps.newHashMap();
    underlying.put("a", null);
    underlying.put("b", true);
    underlying.put(null, true);

    Map<String, Boolean> map =
        Maps.transformValues(
            underlying,
            new Function<Boolean, Boolean>() {
              @Override
              public Boolean apply(@CheckForNull Boolean from) {
                return (from == null) ? true : null;
              }
            });

    Set<Entry<String, Boolean>> entries = map.entrySet();
    assertTrue(entries.contains(Maps.immutableEntry("a", true)));
    assertTrue(entries.contains(Maps.immutableEntry("b", (Boolean) null)));
    assertTrue(entries.contains(Maps.immutableEntry((String) null, (Boolean) null)));

    assertFalse(entries.contains(Maps.immutableEntry("c", (Boolean) null)));
    assertFalse(entries.contains(Maps.immutableEntry((String) null, true)));
  }

  @Override
  public void testKeySetRemoveAllNullFromEmpty() {
    try {
      super.testKeySetRemoveAllNullFromEmpty();
    } catch (RuntimeException tolerated) {
      // GWT's HashMap.keySet().removeAll(null) doesn't throws NPE.
    }
  }

  @Override
  public void testEntrySetRemoveAllNullFromEmpty() {
    try {
      super.testEntrySetRemoveAllNullFromEmpty();
    } catch (RuntimeException tolerated) {
      // GWT's HashMap.entrySet().removeAll(null) doesn't throws NPE.
    }
  }
}

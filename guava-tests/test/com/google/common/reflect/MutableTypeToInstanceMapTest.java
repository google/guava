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

package com.google.common.reflect;

import static com.google.common.collect.Maps.immutableEntry;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.reflect.ImmutableTypeToInstanceMapTest.TestTypeToInstanceMapGenerator;
import java.util.Map;
import java.util.Map.Entry;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test of {@link MutableTypeToInstanceMap}.
 *
 * @author Ben Yu
 */
public class MutableTypeToInstanceMapTest extends TestCase {

  @AndroidIncompatible // problem with suite builders on Android
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(MutableTypeToInstanceMapTest.class);

    suite.addTest(
        MapTestSuiteBuilder.using(
                new TestTypeToInstanceMapGenerator() {
                  // Other tests will verify what real, warning-free usage looks like
                  // but here we have to do some serious fudging
                  @Override
                  @SuppressWarnings({"unchecked", "rawtypes"})
                  public Map<TypeToken, Object> create(Object... elements) {
                    MutableTypeToInstanceMap<Object> map = new MutableTypeToInstanceMap<>();
                    for (Object object : elements) {
                      Entry<TypeToken, Object> entry = (Entry<TypeToken, Object>) object;
                      map.putInstance(entry.getKey(), entry.getValue());
                    }
                    return (Map) map;
                  }
                })
            .named("MutableTypeToInstanceMap")
            .withFeatures(
                MapFeature.SUPPORTS_REMOVE,
                MapFeature.RESTRICTS_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                CollectionSize.ANY,
                MapFeature.ALLOWS_ANY_NULL_QUERIES)
            .createTestSuite());

    return suite;
  }

  private TypeToInstanceMap<Object> map;

  @Override
  protected void setUp() throws Exception {
    map = new MutableTypeToInstanceMap<>();
  }

  public void testPutThrows() {
    assertThrows(
        UnsupportedOperationException.class,
        () -> map.put(TypeToken.of(Integer.class), Integer.valueOf(5)));
  }

  public void testPutAllThrows() {
    assertThrows(
        UnsupportedOperationException.class,
        () -> map.putAll(ImmutableMap.of(TypeToken.of(Integer.class), Integer.valueOf(5))));
  }

  public void testEntrySetMutationThrows() {
    map.putInstance(String.class, "test");
    assertEquals(TypeToken.of(String.class), map.entrySet().iterator().next().getKey());
    assertEquals("test", map.entrySet().iterator().next().getValue());
    assertThrows(
        UnsupportedOperationException.class, () -> map.entrySet().iterator().next().setValue(1));
  }

  public void testEntrySetToArrayMutationThrows() {
    map.putInstance(String.class, "test");
    @SuppressWarnings("unchecked") // Should get a CCE later if cast is wrong
    Entry<?, Object> entry = (Entry<?, Object>) map.entrySet().toArray()[0];
    assertEquals(TypeToken.of(String.class), entry.getKey());
    assertEquals("test", entry.getValue());
    assertThrows(UnsupportedOperationException.class, () -> entry.setValue(1));
  }

  public void testEntrySetToTypedArrayMutationThrows() {
    map.putInstance(String.class, "test");
    @SuppressWarnings("unchecked") // Should get a CCE later if cast is wrong
    Entry<?, Object> entry = (Entry<?, Object>) map.entrySet().toArray(new Entry<?, ?>[0])[0];
    assertEquals(TypeToken.of(String.class), entry.getKey());
    assertEquals("test", entry.getValue());
    assertThrows(UnsupportedOperationException.class, () -> entry.setValue(1));
  }

  public void testPutAndGetInstance() {
    assertNull(map.putInstance(Integer.class, Integer.valueOf(5)));

    Integer oldValue = map.putInstance(Integer.class, Integer.valueOf(7));
    assertEquals(5, (int) oldValue);

    Integer newValue = map.getInstance(Integer.class);
    assertEquals(7, (int) newValue);
    assertEquals(7, (int) map.getInstance(TypeToken.of(Integer.class)));

    // Won't compile: map.putInstance(Double.class, new Long(42));
  }

  public void testNull() {
    assertThrows(
        NullPointerException.class,
        () -> map.putInstance((TypeToken<Integer>) null, Integer.valueOf(1)));
    map.putInstance(Integer.class, null);
    assertTrue(map.containsKey(TypeToken.of(Integer.class)));
    assertTrue(map.entrySet().contains(immutableEntry(TypeToken.of(Integer.class), null)));
    assertNull(map.get(TypeToken.of(Integer.class)));
    assertNull(map.getInstance(Integer.class));

    map.putInstance(Long.class, null);
    assertTrue(map.containsKey(TypeToken.of(Long.class)));
    assertTrue(map.entrySet().contains(immutableEntry(TypeToken.of(Long.class), null)));
    assertNull(map.get(TypeToken.of(Long.class)));
    assertNull(map.getInstance(Long.class));
  }

  public void testPrimitiveAndWrapper() {
    assertNull(map.getInstance(int.class));
    assertNull(map.getInstance(Integer.class));

    assertNull(map.putInstance(int.class, 0));
    assertNull(map.putInstance(Integer.class, 1));
    assertEquals(2, map.size());

    assertEquals(0, (int) map.getInstance(int.class));
    assertEquals(1, (int) map.getInstance(Integer.class));

    assertEquals(0, (int) map.putInstance(int.class, null));
    assertEquals(1, (int) map.putInstance(Integer.class, null));

    assertNull(map.getInstance(int.class));
    assertNull(map.getInstance(Integer.class));
    assertEquals(2, map.size());
  }

  public void testParameterizedType() {
    TypeToken<ImmutableList<Integer>> type = new TypeToken<ImmutableList<Integer>>() {};
    map.putInstance(type, ImmutableList.of(1));
    assertEquals(1, map.size());
    assertEquals(ImmutableList.of(1), map.getInstance(type));
  }

  public void testGenericArrayType() {
    @SuppressWarnings("unchecked") // Trying to test generic array
    ImmutableList<Integer>[] array =
        (ImmutableList<Integer>[]) new ImmutableList<?>[] {ImmutableList.of(1)};
    TypeToken<ImmutableList<Integer>[]> type = new TypeToken<ImmutableList<Integer>[]>() {};
    map.putInstance(type, array);
    assertEquals(1, map.size());
    assertThat(map.getInstance(type)).asList().containsExactly(array[0]);
  }

  public void testWildcardType() {
    TypeToken<ImmutableList<?>> type = new TypeToken<ImmutableList<?>>() {};
    map.putInstance(type, ImmutableList.of(1));
    assertEquals(1, map.size());
    assertEquals(ImmutableList.of(1), map.getInstance(type));
  }

  public void testGetInstance_withTypeVariable() {
    assertThrows(
        IllegalArgumentException.class, () -> map.getInstance(this.<Number>anyIterableType()));
  }

  public void testPutInstance_withTypeVariable() {
    assertThrows(
        IllegalArgumentException.class,
        () -> map.putInstance(this.<Integer>anyIterableType(), ImmutableList.of(1)));
  }

  private <T> TypeToken<Iterable<T>> anyIterableType() {
    return new TypeToken<Iterable<T>>() {};
  }
}

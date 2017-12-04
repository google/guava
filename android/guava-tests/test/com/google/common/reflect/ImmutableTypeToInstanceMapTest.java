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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.TestMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for {@link ImmutableTypeToInstanceMap}.
 *
 * @author Ben Yu
 */
public class ImmutableTypeToInstanceMapTest extends TestCase {

  @AndroidIncompatible // problem with suite builders on Android
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(ImmutableTypeToInstanceMapTest.class);

    suite.addTest(
        MapTestSuiteBuilder.using(
                new TestTypeToInstanceMapGenerator() {
                  // Other tests will verify what real, warning-free usage looks like
                  // but here we have to do some serious fudging
                  @Override
                  @SuppressWarnings("unchecked")
                  public Map<TypeToken, Object> create(Object... elements) {
                    ImmutableTypeToInstanceMap.Builder<Object> builder =
                        ImmutableTypeToInstanceMap.builder();
                    for (Object object : elements) {
                      Entry<TypeToken, Object> entry = (Entry<TypeToken, Object>) object;
                      builder.put(entry.getKey(), entry.getValue());
                    }
                    return (Map) builder.build();
                  }
                })
            .named("ImmutableTypeToInstanceMap")
            .withFeatures(
                MapFeature.REJECTS_DUPLICATES_AT_CREATION,
                MapFeature.RESTRICTS_KEYS,
                CollectionFeature.KNOWN_ORDER,
                CollectionSize.ANY,
                MapFeature.ALLOWS_ANY_NULL_QUERIES)
            .createTestSuite());

    return suite;
  }

  public void testEmpty() {
    assertEquals(0, ImmutableTypeToInstanceMap.of().size());
  }

  public void testPrimitiveAndWrapper() {
    ImmutableTypeToInstanceMap<Number> map =
        ImmutableTypeToInstanceMap.<Number>builder()
            .put(Integer.class, 0)
            .put(int.class, 1)
            .build();
    assertEquals(2, map.size());

    assertEquals(0, (int) map.getInstance(Integer.class));
    assertEquals(0, (int) map.getInstance(TypeToken.of(Integer.class)));
    assertEquals(1, (int) map.getInstance(int.class));
    assertEquals(1, (int) map.getInstance(TypeToken.of(int.class)));
  }

  public void testParameterizedType() {
    TypeToken<ImmutableList<Integer>> type = new TypeToken<ImmutableList<Integer>>() {};
    ImmutableTypeToInstanceMap<Iterable<?>> map =
        ImmutableTypeToInstanceMap.<Iterable<?>>builder().put(type, ImmutableList.of(1)).build();
    assertEquals(1, map.size());
    assertEquals(ImmutableList.of(1), map.getInstance(type));
  }

  public void testGenericArrayType() {
    @SuppressWarnings("unchecked") // Trying to test generic array
    ImmutableList<Integer>[] array = new ImmutableList[] {ImmutableList.of(1)};
    TypeToken<ImmutableList<Integer>[]> type = new TypeToken<ImmutableList<Integer>[]>() {};
    ImmutableTypeToInstanceMap<Iterable<?>[]> map =
        ImmutableTypeToInstanceMap.<Iterable<?>[]>builder().put(type, array).build();
    assertEquals(1, map.size());
    // Redundant cast works around a javac bug.
    assertThat((Iterable<?>[]) map.getInstance(type)).asList().containsExactly(array[0]);
  }

  public void testWildcardType() {
    TypeToken<ImmutableList<?>> type = new TypeToken<ImmutableList<?>>() {};
    ImmutableTypeToInstanceMap<Iterable<?>> map =
        ImmutableTypeToInstanceMap.<Iterable<?>>builder().put(type, ImmutableList.of(1)).build();
    assertEquals(1, map.size());
    assertEquals(ImmutableList.of(1), map.getInstance(type));
  }

  public void testGetInstance_containsTypeVariable() {
    ImmutableTypeToInstanceMap<Iterable<Number>> map = ImmutableTypeToInstanceMap.of();
    try {
      map.getInstance(this.<Number>anyIterableType());
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testPut_containsTypeVariable() {
    ImmutableTypeToInstanceMap.Builder<Iterable<Integer>> builder =
        ImmutableTypeToInstanceMap.builder();
    try {
      builder.put(this.<Integer>anyIterableType(), ImmutableList.of(1));
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  private <T> TypeToken<Iterable<T>> anyIterableType() {
    return new TypeToken<Iterable<T>>() {};
  }

  abstract static class TestTypeToInstanceMapGenerator
      implements TestMapGenerator<TypeToken, Object> {

    @Override
    public TypeToken[] createKeyArray(int length) {
      return new TypeToken[length];
    }

    @Override
    public Object[] createValueArray(int length) {
      return new Object[length];
    }

    @Override
    public SampleElements<Entry<TypeToken, Object>> samples() {
      return new SampleElements<>(
          entry(TypeToken.of(Integer.class), 0),
          entry(TypeToken.of(Number.class), 1),
          entry(new TypeToken<ImmutableList<Integer>>() {}, ImmutableList.of(2)),
          entry(new TypeToken<int[]>() {}, new int[] {3}),
          entry(new TypeToken<Iterable<?>>() {}, ImmutableList.of("4")));
    }

    private static Entry<TypeToken, Object> entry(TypeToken<?> k, Object v) {
      return immutableEntry((TypeToken) k, v);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Entry<TypeToken, Object>[] createArray(int length) {
      return new Entry[length];
    }

    @Override
    public Iterable<Entry<TypeToken, Object>> order(List<Entry<TypeToken, Object>> insertionOrder) {
      return insertionOrder;
    }
  }
}

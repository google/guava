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

import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.TestMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Unit test for {@link ImmutableClassToInstanceMap}.
 *
 * @author Kevin Bourrillion
 */
public class ImmutableClassToInstanceMapTest extends TestCase {
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(ImmutableClassToInstanceMapTest.class);

    suite.addTest(MapTestSuiteBuilder
        .using(new TestClassToInstanceMapGenerator() {
          // Other tests will verify what real, warning-free usage looks like
          // but here we have to do some serious fudging
          @Override
          @SuppressWarnings("unchecked")
          public Map<Class, Number> create(Object... elements) {
            ImmutableClassToInstanceMap.Builder<Number> builder
                = ImmutableClassToInstanceMap.builder();
            for (Object object : elements) {
              Entry<Class, Number> entry = (Entry<Class, Number>) object;
              builder.put(entry.getKey(), entry.getValue());
            }
            return (Map) builder.build();
          }
        })
        .named("ImmutableClassToInstanceMap")
        .withFeatures(
            MapFeature.REJECTS_DUPLICATES_AT_CREATION,
            MapFeature.RESTRICTS_KEYS,
            CollectionFeature.KNOWN_ORDER,
            CollectionSize.ANY,
            MapFeature.ALLOWS_ANY_NULL_QUERIES,
            CollectionFeature.SERIALIZABLE)
        .createTestSuite());

    return suite;
  }

  public void testCopyOf_map_empty() {
    Map<Class<?>, Object> in = Collections.emptyMap();
    ClassToInstanceMap<Object> map = ImmutableClassToInstanceMap.copyOf(in);
    assertTrue(map.isEmpty());

    assertSame(map, ImmutableClassToInstanceMap.copyOf(map));
  }

  public void testCopyOf_map_valid() {
    Map<Class<? extends Number>, Number> in = Maps.newHashMap();
    in.put(Number.class, 0);
    in.put(Double.class, Math.PI);
    ClassToInstanceMap<Number> map = ImmutableClassToInstanceMap.copyOf(in);
    assertEquals(2, map.size());

    Number zero = map.getInstance(Number.class);
    assertEquals(0, zero);

    Double pi = map.getInstance(Double.class);
    assertEquals(Math.PI, pi, 0.0);

    assertSame(map, ImmutableClassToInstanceMap.copyOf(map));
  }

  public void testCopyOf_map_nulls() {
    Map<Class<? extends Number>, Number> nullKey = Collections.singletonMap(
        null, (Number) 1.0);
    try {
      ImmutableClassToInstanceMap.copyOf(nullKey);
      fail();
    } catch (NullPointerException expected) {
    }

    Map<? extends Class<? extends Number>, Number> nullValue
        = Collections.singletonMap(Number.class, null);
    try {
      ImmutableClassToInstanceMap.copyOf(nullValue);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testCopyOf_imap_empty() {
    Map<Class<?>, Object> in = Collections.emptyMap();
    ClassToInstanceMap<Object> map = ImmutableClassToInstanceMap.copyOf(in);
    assertTrue(map.isEmpty());
  }

  public void testCopyOf_imap_valid() {
    ImmutableMap<Class<? extends Number>, ? extends Number> in
        = ImmutableMap.of(Number.class, 0, Double.class, Math.PI);
    ClassToInstanceMap<Number> map = ImmutableClassToInstanceMap.copyOf(in);
    assertEquals(2, map.size());

    Number zero = map.getInstance(Number.class);
    assertEquals(0, zero);

    Double pi = map.getInstance(Double.class);
    assertEquals(Math.PI, pi, 0.0);
  }

  public void testPrimitiveAndWrapper() {
    ImmutableClassToInstanceMap<Number> ictim
        = new ImmutableClassToInstanceMap.Builder<Number>()
            .put(Integer.class, 0)
            .put(int.class, 1)
            .build();
    assertEquals(2, ictim.size());

    assertEquals(0, (int) ictim.getInstance(Integer.class));
    assertEquals(1, (int) ictim.getInstance(int.class));
  }

  abstract static class TestClassToInstanceMapGenerator
      implements TestMapGenerator<Class, Number> {

    @Override
    public Class[] createKeyArray(int length) {
      return new Class[length];
    }

    @Override
    public Number[] createValueArray(int length) {
      return new Number[length];
    }

    @Override
    public SampleElements<Entry<Class, Number>> samples() {
      Entry<Class, Number> entry1 =
          Maps.immutableEntry((Class) Integer.class, (Number) 0);
      Entry<Class, Number> entry2 =
          Maps.immutableEntry((Class) Number.class, (Number) 1);
      Entry<Class, Number> entry3 =
          Maps.immutableEntry((Class) Double.class, (Number) 2.0);
      Entry<Class, Number> entry4 =
          Maps.immutableEntry((Class) Byte.class, (Number) (byte) 0x03);
      Entry<Class, Number> entry5 =
          Maps.immutableEntry((Class) Long.class, (Number) 0x0FF1C1AL);
      return new SampleElements<Entry<Class, Number>>(
          entry1, entry2, entry3, entry4, entry5
      );
    }

    @Override
    @SuppressWarnings("unchecked")
    public Entry<Class, Number>[] createArray(int length) {
      return new Entry[length];
    }

    @Override
    public Iterable<Entry<Class, Number>> order(
        List<Entry<Class, Number>> insertionOrder) {
      return insertionOrder;
    }
  }
}

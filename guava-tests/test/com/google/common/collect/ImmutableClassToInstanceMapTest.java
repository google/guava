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

import static com.google.common.truth.Truth.assertThat;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ClassToInstanceMapTesting.Impl;
import com.google.common.collect.ClassToInstanceMapTesting.TestClassToInstanceMapGenerator;
import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.testing.SerializableTester;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jspecify.annotations.NullUnmarked;

/**
 * Unit test for {@link ImmutableClassToInstanceMap}.
 *
 * @author Kevin Bourrillion
 */
@NullUnmarked
public class ImmutableClassToInstanceMapTest extends TestCase {
  @AndroidIncompatible // test-suite builders
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(ImmutableClassToInstanceMapTest.class);

    suite.addTest(
        MapTestSuiteBuilder.using(
                new TestClassToInstanceMapGenerator() {
                  // Other tests will verify what real, warning-free usage looks like
                  // but here we have to do some serious fudging
                  @Override
                  @SuppressWarnings({"unchecked", "rawtypes"})
                  public Map<Class<?>, Impl> create(Object... elements) {
                    ImmutableClassToInstanceMap.Builder<Impl> builder =
                        ImmutableClassToInstanceMap.builder();
                    for (Object object : elements) {
                      Entry<?, ?> entry = (Entry<?, ?>) object;
                      builder.put((Class) entry.getKey(), (Impl) entry.getValue());
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

  public void testSerialization_empty() {
    assertThat(SerializableTester.reserialize(ImmutableClassToInstanceMap.of()))
        .isSameInstanceAs(ImmutableClassToInstanceMap.of());
  }

  public void testCopyOf_map_empty() {
    Map<Class<?>, Object> in = emptyMap();
    ClassToInstanceMap<Object> map = ImmutableClassToInstanceMap.copyOf(in);
    assertTrue(map.isEmpty());
    assertThat(map).isSameInstanceAs(ImmutableClassToInstanceMap.of());
    assertThat(ImmutableClassToInstanceMap.copyOf(map)).isSameInstanceAs(map);
  }

  public void testOf_zero() {
    assertTrue(ImmutableClassToInstanceMap.of().isEmpty());
  }

  public void testOf_one() {
    ImmutableClassToInstanceMap<Number> map = ImmutableClassToInstanceMap.of(int.class, 1);
    assertEquals(1, map.size());
  }

  public void testCopyOf_map_valid() {
    Map<Class<? extends Number>, Number> in = new HashMap<>();
    in.put(Number.class, 0);
    in.put(Double.class, Math.PI);
    ClassToInstanceMap<Number> map = ImmutableClassToInstanceMap.copyOf(in);
    assertEquals(2, map.size());

    Number zero = map.getInstance(Number.class);
    assertEquals(0, zero);

    Double pi = map.getInstance(Double.class);
    assertThat(pi).isEqualTo(Math.PI);

    assertThat(ImmutableClassToInstanceMap.copyOf(map)).isSameInstanceAs(map);
  }

  public void testCopyOf_map_nulls() {
    Map<Class<? extends Number>, Number> nullKey = singletonMap(null, (Number) 1.0);
    assertThrows(NullPointerException.class, () -> ImmutableClassToInstanceMap.copyOf(nullKey));

    Map<? extends Class<? extends Number>, Number> nullValue = singletonMap(Number.class, null);
    assertThrows(NullPointerException.class, () -> ImmutableClassToInstanceMap.copyOf(nullValue));
  }

  public void testCopyOf_imap_empty() {
    Map<Class<?>, Object> in = emptyMap();
    ClassToInstanceMap<Object> map = ImmutableClassToInstanceMap.copyOf(in);
    assertTrue(map.isEmpty());
  }

  public void testCopyOf_imap_valid() {
    ImmutableMap<Class<? extends Number>, ? extends Number> in =
        ImmutableMap.of(Number.class, 0, Double.class, Math.PI);
    ClassToInstanceMap<Number> map = ImmutableClassToInstanceMap.copyOf(in);
    assertEquals(2, map.size());

    Number zero = map.getInstance(Number.class);
    assertEquals(0, zero);

    Double pi = map.getInstance(Double.class);
    assertThat(pi).isEqualTo(Math.PI);
  }

  public void testPrimitiveAndWrapper() {
    ImmutableClassToInstanceMap<Number> ictim =
        new ImmutableClassToInstanceMap.Builder<Number>()
            .put(Integer.class, 0)
            .put(int.class, 1)
            .build();
    assertEquals(2, ictim.size());

    assertEquals(0, (int) ictim.getInstance(Integer.class));
    assertEquals(1, (int) ictim.getInstance(int.class));
  }
}

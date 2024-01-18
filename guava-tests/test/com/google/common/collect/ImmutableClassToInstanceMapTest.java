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

import static com.google.common.collect.Maps.immutableEntry;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.TestMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.testing.SerializableTester;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Unit test for {@link ImmutableClassToInstanceMap}.
 *
 * @author Kevin Bourrillion
 */
public class ImmutableClassToInstanceMapTest extends TestCase {
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(ImmutableClassToInstanceMapTest.class);

    suite.addTest(
        MapTestSuiteBuilder.using(
                new TestClassToInstanceMapGenerator() {
                  // Other tests will verify what real, warning-free usage looks like
                  // but here we have to do some serious fudging
                  @Override
                  @SuppressWarnings("unchecked")
                  public Map<Class, Impl> create(Object... elements) {
                    ImmutableClassToInstanceMap.Builder<Impl> builder =
                        ImmutableClassToInstanceMap.builder();
                    for (Object object : elements) {
                      Entry<Class, Impl> entry = (Entry<Class, Impl>) object;
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

  public void testSerialization_empty() {
    assertSame(
        ImmutableClassToInstanceMap.of(),
        SerializableTester.reserialize(ImmutableClassToInstanceMap.of()));
  }

  public void testCopyOf_map_empty() {
    Map<Class<?>, Object> in = Collections.emptyMap();
    ClassToInstanceMap<Object> map = ImmutableClassToInstanceMap.copyOf(in);
    assertTrue(map.isEmpty());
    assertSame(map, ImmutableClassToInstanceMap.of());
    assertSame(map, ImmutableClassToInstanceMap.copyOf(map));
  }

  public void testOf_zero() {
    assertTrue(ImmutableClassToInstanceMap.of().isEmpty());
  }

  public void testOf_one() {
    ImmutableClassToInstanceMap<Number> map = ImmutableClassToInstanceMap.of(int.class, 1);
    assertEquals(1, map.size());
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
    Map<Class<? extends Number>, Number> nullKey = Collections.singletonMap(null, (Number) 1.0);
    assertThrows(NullPointerException.class, () -> ImmutableClassToInstanceMap.copyOf(nullKey));

    Map<? extends Class<? extends Number>, Number> nullValue =
        Collections.singletonMap(Number.class, null);
    assertThrows(NullPointerException.class, () -> ImmutableClassToInstanceMap.copyOf(nullValue));
  }

  public void testCopyOf_imap_empty() {
    Map<Class<?>, Object> in = Collections.emptyMap();
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
    assertEquals(Math.PI, pi, 0.0);
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

  abstract static class TestClassToInstanceMapGenerator implements TestMapGenerator<Class, Impl> {

    @Override
    public Class[] createKeyArray(int length) {
      return new Class[length];
    }

    @Override
    public Impl[] createValueArray(int length) {
      return new Impl[length];
    }

    @Override
    public SampleElements<Entry<Class, Impl>> samples() {
      return new SampleElements<>(
          immutableEntry((Class) One.class, new Impl(1)),
          immutableEntry((Class) Two.class, new Impl(2)),
          immutableEntry((Class) Three.class, new Impl(3)),
          immutableEntry((Class) Four.class, new Impl(4)),
          immutableEntry((Class) Five.class, new Impl(5)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Entry<Class, Impl>[] createArray(int length) {
      return new Entry[length];
    }

    @Override
    public Iterable<Entry<Class, Impl>> order(List<Entry<Class, Impl>> insertionOrder) {
      return insertionOrder;
    }
  }

  private interface One {}

  private interface Two {}

  private interface Three {}

  private interface Four {}

  private interface Five {}

  static final class Impl implements One, Two, Three, Four, Five, Serializable {
    final int value;

    Impl(int value) {
      this.value = value;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      return obj instanceof Impl && value == ((Impl) obj).value;
    }

    @Override
    public int hashCode() {
      return value;
    }

    @Override
    public String toString() {
      return Integer.toString(value);
    }
  }
}

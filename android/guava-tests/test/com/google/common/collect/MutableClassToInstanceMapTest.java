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

import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableClassToInstanceMapTest.Impl;
import com.google.common.collect.ImmutableClassToInstanceMapTest.TestClassToInstanceMapGenerator;
import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.Map;
import java.util.Map.Entry;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test of {@link MutableClassToInstanceMap}.
 *
 * @author Kevin Bourrillion
 */
public class MutableClassToInstanceMapTest extends TestCase {
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(MutableClassToInstanceMapTest.class);

    suite.addTest(
        MapTestSuiteBuilder.using(
                new TestClassToInstanceMapGenerator() {
                  // Other tests will verify what real, warning-free usage looks like
                  // but here we have to do some serious fudging
                  @Override
                  @SuppressWarnings("unchecked")
                  public Map<Class, Impl> create(Object... elements) {
                    MutableClassToInstanceMap<Impl> map = MutableClassToInstanceMap.create();
                    for (Object object : elements) {
                      Entry<Class, Impl> entry = (Entry<Class, Impl>) object;
                      map.putInstance(entry.getKey(), entry.getValue());
                    }
                    return (Map) map;
                  }
                })
            .named("MutableClassToInstanceMap")
            .withFeatures(
                MapFeature.GENERAL_PURPOSE,
                MapFeature.RESTRICTS_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                CollectionSize.ANY,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                MapFeature.ALLOWS_ANY_NULL_QUERIES)
            .createTestSuite());

    return suite;
  }

  private ClassToInstanceMap<Number> map;

  @Override
  protected void setUp() throws Exception {
    map = MutableClassToInstanceMap.create();
  }

  public void testConstraint() {

    /**
     * We'll give ourselves a pass on testing all the possible ways of breaking the constraint,
     * because we know that newClassMap() is implemented using ConstrainedMap which is itself
     * well-tested. A purist would object to this, but what can I say, we're dirty cheaters.
     */
    map.put(Integer.class, new Integer(5));
    assertThrows(ClassCastException.class, () -> map.put(Double.class, new Long(42)));
    // Won't compile: map.put(String.class, "x");
  }

  public void testPutAndGetInstance() {
    assertNull(map.putInstance(Integer.class, new Integer(5)));

    Integer oldValue = map.putInstance(Integer.class, new Integer(7));
    assertEquals(5, (int) oldValue);

    Integer newValue = map.getInstance(Integer.class);
    assertEquals(7, (int) newValue);

    // Won't compile: map.putInstance(Double.class, new Long(42));
  }

  public void testNull() {
    assertThrows(NullPointerException.class, () -> map.put(null, new Integer(1)));
    map.putInstance(Integer.class, null);
    assertNull(map.get(Integer.class));
    assertNull(map.getInstance(Integer.class));

    map.put(Long.class, null);
    assertNull(map.get(Long.class));
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
}

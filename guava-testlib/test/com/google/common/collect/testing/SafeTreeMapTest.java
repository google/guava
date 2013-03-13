/*
 * Copyright (C) 2010 The Guava Authors
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

package com.google.common.collect.testing;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import com.google.common.testing.SerializableTester;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Map;
import java.util.SortedMap;

/**
 * Tests for SafeTreeMap.
 *
 * @author Louis Wasserman
 */
public class SafeTreeMapTest extends TestCase {
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(SafeTreeMapTest.class);
    return suite;
  }

  @GwtIncompatible("SerializableTester")
  public void testViewSerialization() {
    Map<String, Integer> map =
        ImmutableSortedMap.of("one", 1, "two", 2, "three", 3);
    SerializableTester.reserializeAndAssert(map.entrySet());
    SerializableTester.reserializeAndAssert(map.keySet());
    assertEquals(Lists.newArrayList(map.values()),
        Lists.newArrayList(SerializableTester.reserialize(map.values())));
  }

  @GwtIncompatible("SerializableTester")
  public static class ReserializedMapTests
      extends SortedMapInterfaceTest<String, Integer> {
    public ReserializedMapTests() {
      super(false, true, true, true, true);
    }

    @Override protected SortedMap<String, Integer> makePopulatedMap() {
      SortedMap<String, Integer> map = new SafeTreeMap<String, Integer>();
      map.put("one", 1);
      map.put("two", 2);
      map.put("three", 3);
      return SerializableTester.reserialize(map);
    }

    @Override protected SortedMap<String, Integer> makeEmptyMap()
        throws UnsupportedOperationException {
      SortedMap<String, Integer> map = new SafeTreeMap<String, Integer>();
      return SerializableTester.reserialize(map);
    }

    @Override protected String getKeyNotInPopulatedMap() {
      return "minus one";
    }

    @Override protected Integer getValueNotInPopulatedMap() {
      return -1;
    }
  }
}

/*
 * Copyright (C) 2008 The Guava Authors
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
import com.google.common.collect.testing.MapInterfaceTest;

import junit.framework.TestCase;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Map interface tests for bimaps.
 *
 * @author Jared Levy
 */
@GwtCompatible
public class BiMapMapInterfaceTest extends TestCase {

  private abstract static class AbstractMapInterfaceTest
      extends MapInterfaceTest<String, Integer> {

    protected AbstractMapInterfaceTest(boolean modifiable) {
      super(true, true, modifiable, modifiable, modifiable);
    }

    @Override protected String getKeyNotInPopulatedMap() {
      return "cat";
    }

    @Override protected Integer getValueNotInPopulatedMap() {
      return 3;
    }

    @Override protected Map<String, Integer> makeEmptyMap() {
      return HashBiMap.create();
    }

    @Override protected Map<String, Integer> makePopulatedMap() {
      Map<String, Integer> map = makeEmptyMap();
      map.put("foo", 1);
      map.put("bar", 2);
      return map;
    }

    @Override protected void assertMoreInvariants(Map<String, Integer> map) {
      BiMap<String, Integer> bimap = (BiMap<String, Integer>) map;
      BiMap<Integer, String> inverse = bimap.inverse();
      assertEquals(bimap.size(), inverse.size());
      for (Entry<String, Integer> entry : bimap.entrySet()) {
        assertEquals(entry.getKey(), inverse.get(entry.getValue()));
      }
      for (Entry<Integer, String> entry : inverse.entrySet()) {
        assertEquals(entry.getKey(), bimap.get(entry.getValue()));
      }
    }
  }

  public static class HashBiMapInterfaceTest extends AbstractMapInterfaceTest {
    public HashBiMapInterfaceTest() {
      super(true);
    }
    @Override protected Map<String, Integer> makeEmptyMap() {
      return HashBiMap.create();
    }
  }

  public static class InverseBiMapInterfaceTest
      extends AbstractMapInterfaceTest {
    public InverseBiMapInterfaceTest() {
      super(true);
    }
    @Override protected Map<String, Integer> makeEmptyMap() {
      return HashBiMap.<Integer, String>create().inverse();
    }
  }

  public static class UnmodifiableBiMapInterfaceTest
      extends AbstractMapInterfaceTest {
    public UnmodifiableBiMapInterfaceTest() {
      super(false);
    }
    @Override protected Map<String, Integer> makeEmptyMap() {
      return Maps.unmodifiableBiMap(HashBiMap.<String, Integer>create());
    }
    @Override protected Map<String, Integer> makePopulatedMap() {
      BiMap<String, Integer> bimap = HashBiMap.create();
      bimap.put("foo", 1);
      bimap.put("bar", 2);
      return Maps.unmodifiableBiMap(bimap);
    }
  }

  public static class SynchronizedBiMapInterfaceTest
      extends AbstractMapInterfaceTest {
    public SynchronizedBiMapInterfaceTest() {
      super(true);
    }
    @Override protected Map<String, Integer> makeEmptyMap() {
      return Maps.synchronizedBiMap(HashBiMap.<String, Integer>create());
    }
  }

  public void testNothing() {
    /*
     * It's a warning if a TestCase subclass contains no tests, so we add one.
     * Alternatively, we could stop extending TestCase, but I worry that someone
     * will add a test in the future and not realize that it's being ignored.
     */
  }
}

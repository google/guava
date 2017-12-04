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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import junit.framework.TestCase;

/**
 * Tests for {@link ForwardingConcurrentMap}.
 *
 * @author Jared Levy
 */
public class ForwardingConcurrentMapTest extends TestCase {

  private static class TestMap extends ForwardingConcurrentMap<String, Integer> {
    final ConcurrentMap<String, Integer> delegate = new ConcurrentHashMap<>();

    @Override
    protected ConcurrentMap<String, Integer> delegate() {
      return delegate;
    }
  }

  public void testPutIfAbsent() {
    TestMap map = new TestMap();
    map.put("foo", 1);
    assertEquals(Integer.valueOf(1), map.putIfAbsent("foo", 2));
    assertEquals(Integer.valueOf(1), map.get("foo"));
    assertNull(map.putIfAbsent("bar", 3));
    assertEquals(Integer.valueOf(3), map.get("bar"));
  }

  public void testRemove() {
    TestMap map = new TestMap();
    map.put("foo", 1);
    assertFalse(map.remove("foo", 2));
    assertFalse(map.remove("bar", 1));
    assertEquals(Integer.valueOf(1), map.get("foo"));
    assertTrue(map.remove("foo", 1));
    assertTrue(map.isEmpty());
  }

  public void testReplace() {
    TestMap map = new TestMap();
    map.put("foo", 1);
    assertEquals(Integer.valueOf(1), map.replace("foo", 2));
    assertNull(map.replace("bar", 3));
    assertEquals(Integer.valueOf(2), map.get("foo"));
    assertFalse(map.containsKey("bar"));
  }

  public void testReplaceConditional() {
    TestMap map = new TestMap();
    map.put("foo", 1);
    assertFalse(map.replace("foo", 2, 3));
    assertFalse(map.replace("bar", 1, 2));
    assertEquals(Integer.valueOf(1), map.get("foo"));
    assertFalse(map.containsKey("bar"));
    assertTrue(map.replace("foo", 1, 4));
    assertEquals(Integer.valueOf(4), map.get("foo"));
  }
}

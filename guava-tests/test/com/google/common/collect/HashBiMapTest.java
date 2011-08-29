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

import com.google.common.annotations.GwtCompatible;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Tests for {@link HashBiMap}.
 *
 * @author Mike Bostock
 */
@GwtCompatible
public class HashBiMapTest extends AbstractBiMapTest {

  @Override protected BiMap<Integer, String> create() {
    return HashBiMap.create();
  }

  public void testCreate() {
    BiMap<String, String> bimap = HashBiMap.create();
    assertEquals(0, bimap.size());
    bimap.put("canada", "dollar");
    assertEquals("dollar", bimap.get("canada"));
    assertEquals("canada", bimap.inverse().get("dollar"));
  }

  public void testMapConstructor() {
    /* Test with non-empty Map. */
    Map<String, String> map = ImmutableMap.of(
        "canada", "dollar",
        "chile", "peso",
        "switzerland", "franc");
    HashBiMap<String, String> bimap = HashBiMap.create(map);
    assertEquals("dollar", bimap.get("canada"));
    assertEquals("canada", bimap.inverse().get("dollar"));
  }

  private static final int N = 1000;

  public void testBashIt() throws Exception {
    BiMap<Integer, Integer> bimap = HashBiMap.create(N);
    BiMap<Integer, Integer> inverse = bimap.inverse();

    for (int i = 0; i < N; i++) {
      assertNull(bimap.put(2 * i, 2 * i + 1));
    }
    for (int i = 0; i < N; i++) {
      assertEquals(2 * i + 1, (int) bimap.get(2 * i));
    }
    for (int i = 0; i < N; i++) {
      assertEquals(2 * i, (int) inverse.get(2 * i + 1));
    }
    for (int i = 0; i < N; i++) {
      int oldValue = bimap.get(2 * i);
      assertEquals(2 * i + 1, (int) bimap.put(2 * i, oldValue - 2));
    }
    for (int i = 0; i < N; i++) {
      assertEquals(2 * i - 1, (int) bimap.get(2 * i));
    }
    for (int i = 0; i < N; i++) {
      assertEquals(2 * i, (int) inverse.get(2 * i - 1));
    }
    Set<Entry<Integer, Integer>> entries = bimap.entrySet();
    for (Entry<Integer, Integer> entry : entries) {
      entry.setValue(entry.getValue() + 2 * N);
    }
    for (int i = 0; i < N; i++) {
      assertEquals(2 * N + 2 * i - 1, (int) bimap.get(2 * i));
    }
  }

  // The next two tests verify that map entries are not accessed after they're
  // removed, since IdentityHashMap throws an exception when that occurs.
  public void testIdentityKeySetIteratorRemove() {
    bimap = new AbstractBiMap<Integer, String>(
        new IdentityHashMap<Integer, String>(),
        new IdentityHashMap<String, Integer>()) {};
    putOneTwoThree();
    Iterator<Integer> iterator = bimap.keySet().iterator();
    iterator.next();
    iterator.next();
    iterator.remove();
    iterator.next();
    iterator.remove();
    assertEquals(1, bimap.size());
    assertEquals(1, bimap.inverse().size());
  }

  public void testIdentityEntrySetIteratorRemove() {
    bimap = new AbstractBiMap<Integer, String>(
        new IdentityHashMap<Integer, String>(),
        new IdentityHashMap<String, Integer>()) {};
    putOneTwoThree();
    Iterator<Entry<Integer, String>> iterator = bimap.entrySet().iterator();
    iterator.next();
    iterator.next();
    iterator.remove();
    iterator.next();
    iterator.remove();
    assertEquals(1, bimap.size());
    assertEquals(1, bimap.inverse().size());
  }
}

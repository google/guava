/*
 * Copyright (C) 2012 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import junit.framework.TestCase;

/**
 * Tests for {@code AbstractBiMap}.
 *
 * @author Mike Bostock
 */
public class AbstractBiMapTest extends TestCase {

  // The next two tests verify that map entries are not accessed after they're
  // removed, since IdentityHashMap throws an exception when that occurs.
  public void testIdentityKeySetIteratorRemove() {
    BiMap<Integer, String> bimap =
        new AbstractBiMap<Integer, String>(
            new IdentityHashMap<Integer, String>(), new IdentityHashMap<String, Integer>()) {};
    bimap.put(1, "one");
    bimap.put(2, "two");
    bimap.put(3, "three");
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
    BiMap<Integer, String> bimap =
        new AbstractBiMap<Integer, String>(
            new IdentityHashMap<Integer, String>(), new IdentityHashMap<String, Integer>()) {};
    bimap.put(1, "one");
    bimap.put(2, "two");
    bimap.put(3, "three");
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

/*
 * Copyright (C) 2011 The Guava Authors
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
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import junit.framework.TestCase;

@GwtCompatible
public class WellBehavedMapTest extends TestCase {
  enum Foo {
    X, Y, Z, T
  }

  public void testEntrySet_contain() {
    WellBehavedMap<Foo, Integer> map = WellBehavedMap.wrap(
        new EnumMap<Foo, Integer>(Foo.class));
    map.putAll(ImmutableMap.of(Foo.X, 1, Foo.Y, 2, Foo.Z, 3));

    // testing with the exact entry
    assertTrue(map.entrySet().contains(Maps.immutableEntry(Foo.X, 1)));
    assertTrue(map.entrySet().contains(Maps.immutableEntry(Foo.Y, new Integer(2))));

    // testing an entry with a contained key, but not the same value
    assertFalse(map.entrySet().contains(Maps.immutableEntry(Foo.X, 5)));

    // testing a non-existent key
    assertFalse(map.entrySet().contains(Maps.immutableEntry(Foo.T, 0)));
  }

  public void testEntry_setValue() {
    WellBehavedMap<Foo, Integer> map = WellBehavedMap.wrap(
        new EnumMap<Foo, Integer>(Foo.class));
    map.putAll(ImmutableMap.of(Foo.X, 1, Foo.Y, 2, Foo.Z, 3));

    for (Map.Entry<Foo, Integer> entry : map.entrySet()) {
      entry.setValue(entry.getValue() + 5);
    }

    assertEquals(ImmutableMap.of(Foo.X, 6, Foo.Y, 7, Foo.Z, 8), map);
  }

  public void testEntriesAreMutableAndConsistent() {
    WellBehavedMap<Foo, Integer> map = WellBehavedMap.wrap(
        new EnumMap<Foo, Integer>(Foo.class));
    map.putAll(ImmutableMap.of(Foo.X, 1));

    Map.Entry<Foo, Integer> entry1 = Iterables.getOnlyElement(map.entrySet());
    Map.Entry<Foo, Integer> entry2 = Iterables.getOnlyElement(map.entrySet());

    // the entries are constructed and forgotten, thus different
    assertNotSame(entry1, entry2);

    Set<Map.Entry<Foo, Integer>> entrySet = map.entrySet();

    assertTrue(entrySet.contains(entry1));
    assertTrue(entrySet.contains(entry2));

    // mutating entry
    entry1.setValue(2);

    // entry2 is also modified
    assertEquals(entry1.getValue(), entry2.getValue());

    // and both are still contained in the set
    assertTrue(entrySet.contains(entry1));
    assertTrue(entrySet.contains(entry2));
  }

  public void testEntrySet_remove() {
    WellBehavedMap<Foo, Integer> map = WellBehavedMap.wrap(
        new EnumMap<Foo, Integer>(Foo.class));
    map.putAll(ImmutableMap.of(Foo.X, 1, Foo.Y, 2, Foo.Z, 3));
    Set<Map.Entry<Foo, Integer>> entrySet = map.entrySet();

    // removing an existing entry, verifying consistency
    Map.Entry<Foo, Integer> entry = Maps.immutableEntry(Foo.Y, 2);
    assertTrue(entrySet.remove(entry));
    assertFalse(map.containsKey(Foo.Y));
    assertNull(map.get(Foo.Y));
    assertFalse(entrySet.contains(entry));

    // we didn't have that entry, not removed
    assertFalse(entrySet.remove(Maps.immutableEntry(Foo.T, 4)));

    // we didn't have that entry, only <Z, 3>, must not remove
    assertFalse(entrySet.remove(Maps.immutableEntry(Foo.Z, 5)));
  }
}

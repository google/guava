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

import static java.util.Arrays.asList;

import com.google.common.annotations.GwtCompatible;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Tests for {@code SetMultimap} implementations.
 *
 * @author Jared Levy
 */
@GwtCompatible
public abstract class AbstractSetMultimapTest extends AbstractMultimapTest {

  public void testAsMapEquals() {
    Multimap<String, Integer> multimap = getMultimap();
    multimap.put("foo", 1);
    multimap.put("foo", nullValue());
    multimap.put(nullKey(), 3);
    Map<String, Collection<Integer>> map = multimap.asMap();

    Map<String, Collection<Integer>> equalMap = Maps.newHashMap();
    equalMap.put("foo", Sets.newHashSet(1, nullValue()));
    equalMap.put(nullKey(), Sets.newHashSet(3));
    assertEquals(map, equalMap);
    assertEquals(equalMap, map);
    assertEquals(equalMap.hashCode(), multimap.hashCode());

    Map<String, Collection<Integer>> unequalMap = Maps.newHashMap();
    equalMap.put("foo", Sets.newHashSet(3, nullValue()));
    equalMap.put(nullKey(), Sets.newHashSet(1));
    assertFalse(map.equals(unequalMap));
    assertFalse(unequalMap.equals(map));
  }

  public void testAsMapEntriesEquals() {
    Multimap<String, Integer> multimap = getMultimap();
    multimap.put("foo", 1);
    multimap.put("foo", 3);
    Set<Map.Entry<String, Collection<Integer>>> set
        = multimap.asMap().entrySet();

    Iterator<Map.Entry<String, Collection<Integer>>> i = set.iterator();
    Map.Entry<String, Collection<Integer>> expected =
        Maps.immutableEntry(
            "foo", (Collection<Integer>) ImmutableSet.of(1, 3));
    Map.Entry<String, Collection<Integer>> entry = i.next();
    assertEquals(expected, entry);
    assertFalse(i.hasNext());

    assertTrue(Collections.singleton(expected).equals(set));
    assertTrue(set.equals(Collections.singleton(expected)));

    Map.Entry<?, ?>[] array = new Map.Entry<?, ?>[3];
    array[1] = Maps.immutableEntry("another", "entry");
    assertSame(array, set.toArray(array));
    assertEquals(entry, array[0]);
    assertNull(array[1]);
  }

  @SuppressWarnings("unchecked")
  public void testAsMapValues() {
    Multimap<String, Integer> multimap = create();
    Collection<Collection<Integer>> asMapValues = multimap.asMap().values();
    multimap.put("foo", 1);
    multimap.put("foo", 3);

    Collection<?>[] array = new Collection<?>[3];
    array[1] = Collections.emptyList();
    assertSame(array, asMapValues.toArray(array));
    assertEquals(Sets.newHashSet(1, 3), array[0]);
    assertNull(array[1]);

    multimap.put("bar", 3);
    assertTrue(asMapValues.containsAll(
        asList(Sets.newHashSet(1, 3), Sets.newHashSet(3))));
    assertFalse(asMapValues.containsAll(
        asList(Sets.newHashSet(1, 3), Sets.newHashSet(1))));
    assertFalse(asMapValues.remove(ImmutableSet.of(1, 2)));
    assertEquals(3, multimap.size());
    assertTrue(asMapValues.remove(ImmutableSet.of(1, 3)));
    assertEquals(1, multimap.size());
  }
}

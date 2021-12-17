/*
 * Copyright (C) 2011 The Guava Authors
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

package com.google.common.cache;

import static java.util.Arrays.asList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import junit.framework.TestCase;

/** @author Charles Fry */
public class CacheManualTest extends TestCase {

  public void testGetIfPresent() {
    Cache<Object, Object> cache = CacheBuilder.newBuilder().recordStats().build();
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    Object one = new Object();
    Object two = new Object();

    assertNull(cache.getIfPresent(one));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());
    assertNull(cache.asMap().get(one));
    assertFalse(cache.asMap().containsKey(one));
    assertFalse(cache.asMap().containsValue(two));

    assertNull(cache.getIfPresent(two));
    stats = cache.stats();
    assertEquals(2, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());
    assertNull(cache.asMap().get(two));
    assertFalse(cache.asMap().containsKey(two));
    assertFalse(cache.asMap().containsValue(one));

    cache.put(one, two);

    assertSame(two, cache.getIfPresent(one));
    stats = cache.stats();
    assertEquals(2, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(1, stats.hitCount());
    assertSame(two, cache.asMap().get(one));
    assertTrue(cache.asMap().containsKey(one));
    assertTrue(cache.asMap().containsValue(two));

    assertNull(cache.getIfPresent(two));
    stats = cache.stats();
    assertEquals(3, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(1, stats.hitCount());
    assertNull(cache.asMap().get(two));
    assertFalse(cache.asMap().containsKey(two));
    assertFalse(cache.asMap().containsValue(one));

    cache.put(two, one);

    assertSame(two, cache.getIfPresent(one));
    stats = cache.stats();
    assertEquals(3, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(2, stats.hitCount());
    assertSame(two, cache.asMap().get(one));
    assertTrue(cache.asMap().containsKey(one));
    assertTrue(cache.asMap().containsValue(two));

    assertSame(one, cache.getIfPresent(two));
    stats = cache.stats();
    assertEquals(3, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(3, stats.hitCount());
    assertSame(one, cache.asMap().get(two));
    assertTrue(cache.asMap().containsKey(two));
    assertTrue(cache.asMap().containsValue(one));
  }

  public void testGetAllPresent() {
    Cache<Integer, Integer> cache = CacheBuilder.newBuilder().recordStats().build();
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertEquals(ImmutableMap.of(), cache.getAllPresent(ImmutableList.<Integer>of()));
    stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertEquals(ImmutableMap.of(), cache.getAllPresent(asList(1, 2, 3)));
    stats = cache.stats();
    assertEquals(3, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    cache.put(2, 22);

    assertEquals(ImmutableMap.of(2, 22), cache.getAllPresent(asList(1, 2, 3)));
    stats = cache.stats();
    assertEquals(5, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(1, stats.hitCount());

    cache.put(3, 33);

    assertEquals(ImmutableMap.of(2, 22, 3, 33), cache.getAllPresent(asList(1, 2, 3)));
    stats = cache.stats();
    assertEquals(6, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(3, stats.hitCount());

    cache.put(1, 11);

    assertEquals(ImmutableMap.of(1, 11, 2, 22, 3, 33), cache.getAllPresent(asList(1, 2, 3)));
    stats = cache.stats();
    assertEquals(6, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(6, stats.hitCount());
  }
}

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

import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * @author Charles Fry
 */
@NullUnmarked
public class CacheManualTest extends TestCase {

  public void testGetIfPresent() {
    Cache<Object, Object> cache = CacheBuilder.newBuilder().recordStats().build();
    CacheStats stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    Object one = new Object();
    Object two = new Object();

    assertThat(cache.getIfPresent(one)).isNull();
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);
    assertThat(cache.asMap().get(one)).isNull();
    assertThat(cache.asMap().containsKey(one)).isFalse();
    assertThat(cache.asMap().containsValue(two)).isFalse();

    assertThat(cache.getIfPresent(two)).isNull();
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(2);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);
    assertThat(cache.asMap().get(two)).isNull();
    assertThat(cache.asMap().containsKey(two)).isFalse();
    assertThat(cache.asMap().containsValue(one)).isFalse();

    cache.put(one, two);

    assertThat(cache.getIfPresent(one)).isSameInstanceAs(two);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(2);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(1);
    assertThat(cache.asMap().get(one)).isSameInstanceAs(two);
    assertThat(cache.asMap().containsKey(one)).isTrue();
    assertThat(cache.asMap().containsValue(two)).isTrue();

    assertThat(cache.getIfPresent(two)).isNull();
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(3);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(1);
    assertThat(cache.asMap().get(two)).isNull();
    assertThat(cache.asMap().containsKey(two)).isFalse();
    assertThat(cache.asMap().containsValue(one)).isFalse();

    cache.put(two, one);

    assertThat(cache.getIfPresent(one)).isSameInstanceAs(two);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(3);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(2);
    assertThat(cache.asMap().get(one)).isSameInstanceAs(two);
    assertThat(cache.asMap().containsKey(one)).isTrue();
    assertThat(cache.asMap().containsValue(two)).isTrue();

    assertThat(cache.getIfPresent(two)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(3);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(3);
    assertThat(cache.asMap().get(two)).isSameInstanceAs(one);
    assertThat(cache.asMap().containsKey(two)).isTrue();
    assertThat(cache.asMap().containsValue(one)).isTrue();
  }

  public void testGetAllPresent() {
    Cache<Integer, Integer> cache = CacheBuilder.newBuilder().recordStats().build();
    CacheStats stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getAllPresent(ImmutableList.<Integer>of())).isEmpty();
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getAllPresent(asList(1, 2, 3))).isEqualTo(ImmutableMap.of());
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(3);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    cache.put(2, 22);

    assertThat(cache.getAllPresent(asList(1, 2, 3))).containsExactly(2, 22);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(5);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(1);

    cache.put(3, 33);

    assertThat(cache.getAllPresent(asList(1, 2, 3))).containsExactly(2, 22, 3, 33);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(6);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(3);

    cache.put(1, 11);

    assertThat(cache.getAllPresent(asList(1, 2, 3)))
        .isEqualTo(ImmutableMap.of(1, 11, 2, 22, 3, 33));
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(6);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(6);
  }
}

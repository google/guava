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

import static com.google.common.cache.CacheTesting.checkEmpty;
import static com.google.common.cache.CacheTesting.checkValidState;
import static com.google.common.cache.TestingCacheLoaders.identityLoader;
import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertThrows;

import com.google.common.base.Function;
import com.google.common.cache.CacheBuilderFactory.DurationSpec;
import com.google.common.cache.LocalCache.Strength;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.testing.EqualsTester;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * {@link LoadingCache} tests that deal with caches that actually contain some key-value mappings.
 *
 * @author mike nonemacher
 */

@NullUnmarked
public class PopulatedCachesTest extends TestCase {
  // we use integers as keys; make sure the range covers some values that ARE cached by
  // Integer.valueOf(int), and some that are not cached. (127 is the highest cached value.)
  static final int WARMUP_MIN = 120;
  static final int WARMUP_MAX = 135;
  static final int WARMUP_SIZE = WARMUP_MAX - WARMUP_MIN;

  public void testSize_populated() {
    for (LoadingCache<Object, Object> cache : caches()) {
      // don't let the entries get GCed
      List<Entry<Object, Object>> unused = warmUp(cache);
      assertThat(cache.size()).isEqualTo(WARMUP_SIZE);
      assertMapSize(cache.asMap(), WARMUP_SIZE);
      checkValidState(cache);
    }
  }

  public void testContainsKey_found() {
    for (LoadingCache<Object, Object> cache : caches()) {
      // don't let the entries get GCed
      List<Entry<Object, Object>> warmed = warmUp(cache);
      for (int i = WARMUP_MIN; i < WARMUP_MAX; i++) {
        Entry<Object, Object> entry = warmed.get(i - WARMUP_MIN);
        assertThat(cache.asMap().containsKey(entry.getKey())).isTrue();
        assertThat(cache.asMap().containsValue(entry.getValue())).isTrue();
        // this getUnchecked() call shouldn't be a cache miss; verified below
        assertThat(cache.getUnchecked(entry.getKey())).isEqualTo(entry.getValue());
      }
      assertThat(cache.stats().missCount()).isEqualTo(WARMUP_SIZE);
      checkValidState(cache);
    }
  }

  public void testPut_populated() {
    for (LoadingCache<Object, Object> cache : caches()) {
      // don't let the entries get GCed
      List<Entry<Object, Object>> warmed = warmUp(cache);
      for (int i = WARMUP_MIN; i < WARMUP_MAX; i++) {
        Entry<Object, Object> entry = warmed.get(i - WARMUP_MIN);
        Object newValue = new Object();
        assertThat(cache.asMap().put(entry.getKey(), newValue)).isSameInstanceAs(entry.getValue());
        // don't let the new entry get GCed
        warmed.add(entryOf(entry.getKey(), newValue));
        Object newKey = new Object();
        assertThat(cache.asMap().put(newKey, entry.getValue())).isNull();
        // this getUnchecked() call shouldn't be a cache miss; verified below
        assertThat(cache.getUnchecked(entry.getKey())).isEqualTo(newValue);
        assertThat(cache.getUnchecked(newKey)).isEqualTo(entry.getValue());
        // don't let the new entry get GCed
        warmed.add(entryOf(newKey, entry.getValue()));
      }
      assertThat(cache.stats().missCount()).isEqualTo(WARMUP_SIZE);
      checkValidState(cache);
    }
  }

  public void testPutIfAbsent_populated() {
    for (LoadingCache<Object, Object> cache : caches()) {
      // don't let the entries get GCed
      List<Entry<Object, Object>> warmed = warmUp(cache);
      for (int i = WARMUP_MIN; i < WARMUP_MAX; i++) {
        Entry<Object, Object> entry = warmed.get(i - WARMUP_MIN);
        Object newValue = new Object();
        assertThat(cache.asMap().putIfAbsent(entry.getKey(), newValue))
            .isSameInstanceAs(entry.getValue());
        Object newKey = new Object();
        assertThat(cache.asMap().putIfAbsent(newKey, entry.getValue())).isNull();
        // this getUnchecked() call shouldn't be a cache miss; verified below
        assertThat(cache.getUnchecked(entry.getKey())).isEqualTo(entry.getValue());
        assertThat(cache.getUnchecked(newKey)).isEqualTo(entry.getValue());
        // don't let the new entry get GCed
        warmed.add(entryOf(newKey, entry.getValue()));
      }
      assertThat(cache.stats().missCount()).isEqualTo(WARMUP_SIZE);
      checkValidState(cache);
    }
  }

  public void testPutAll_populated() {
    for (LoadingCache<Object, Object> cache : caches()) {
      // don't let the entries get GCed
      List<Entry<Object, Object>> unused = warmUp(cache);
      Object newKey = new Object();
      Object newValue = new Object();
      cache.asMap().putAll(ImmutableMap.of(newKey, newValue));
      // this getUnchecked() call shouldn't be a cache miss; verified below
      assertThat(cache.getUnchecked(newKey)).isEqualTo(newValue);
      assertThat(cache.stats().missCount()).isEqualTo(WARMUP_SIZE);
      checkValidState(cache);
    }
  }

  public void testReplace_populated() {
    for (LoadingCache<Object, Object> cache : caches()) {
      // don't let the entries get GCed
      List<Entry<Object, Object>> warmed = warmUp(cache);
      for (int i = WARMUP_MIN; i < WARMUP_MAX; i++) {
        Entry<Object, Object> entry = warmed.get(i - WARMUP_MIN);
        Object newValue = new Object();
        assertThat(cache.asMap().replace(entry.getKey(), newValue))
            .isSameInstanceAs(entry.getValue());
        assertThat(cache.asMap().replace(entry.getKey(), newValue, entry.getValue())).isTrue();
        Object newKey = new Object();
        assertThat(cache.asMap().replace(newKey, entry.getValue())).isNull();
        assertThat(cache.asMap().replace(newKey, entry.getValue(), newValue)).isFalse();
        // this getUnchecked() call shouldn't be a cache miss; verified below
        assertThat(cache.getUnchecked(entry.getKey())).isEqualTo(entry.getValue());
        assertThat(cache.asMap().containsKey(newKey)).isFalse();
      }
      assertThat(cache.stats().missCount()).isEqualTo(WARMUP_SIZE);
      checkValidState(cache);
    }
  }

  public void testRemove_byKey() {
    for (LoadingCache<Object, Object> cache : caches()) {
      // don't let the entries get GCed
      List<Entry<Object, Object>> warmed = warmUp(cache);
      for (int i = WARMUP_MIN; i < WARMUP_MAX; i++) {
        Entry<Object, Object> entry = warmed.get(i - WARMUP_MIN);
        Object key = entry.getKey();
        assertThat(cache.asMap().remove(key)).isEqualTo(entry.getValue());
        assertThat(cache.asMap().remove(key)).isNull();
        assertThat(cache.asMap().containsKey(key)).isFalse();
      }
      checkEmpty(cache);
    }
  }

  public void testRemove_byKeyAndValue() {
    for (LoadingCache<Object, Object> cache : caches()) {
      // don't let the entries get GCed
      List<Entry<Object, Object>> warmed = warmUp(cache);
      for (int i = WARMUP_MIN; i < WARMUP_MAX; i++) {
        Object key = warmed.get(i - WARMUP_MIN).getKey();
        Object value = warmed.get(i - WARMUP_MIN).getValue();
        assertThat(cache.asMap().remove(key, -1)).isFalse();
        assertThat(cache.asMap().remove(key, value)).isTrue();
        assertThat(cache.asMap().remove(key, -1)).isFalse();
        assertThat(cache.asMap().containsKey(key)).isFalse();
      }
      checkEmpty(cache);
    }
  }


  public void testKeySet_populated() {
    for (LoadingCache<Object, Object> cache : caches()) {
      Set<Object> keys = cache.asMap().keySet();
      List<Entry<Object, Object>> warmed = warmUp(cache);

      Set<Object> expected = new HashMap<>(cache.asMap()).keySet();
      assertThat(keys).containsExactlyElementsIn(expected);
      assertThat(keys.toArray()).asList().containsExactlyElementsIn(expected);
      assertThat(keys.toArray(new Object[0])).asList().containsExactlyElementsIn(expected);

      new EqualsTester()
          .addEqualityGroup(cache.asMap().keySet(), keys)
          .addEqualityGroup(ImmutableSet.of())
          .testEquals();
      assertThat(keys).hasSize(WARMUP_SIZE);
      for (int i = WARMUP_MIN; i < WARMUP_MAX; i++) {
        Object key = warmed.get(i - WARMUP_MIN).getKey();
        assertThat(keys.contains(key)).isTrue();
        assertThat(keys.remove(key)).isTrue();
        assertThat(keys.remove(key)).isFalse();
        assertThat(keys.contains(key)).isFalse();
      }
      checkEmpty(keys);
      checkEmpty(cache);
    }
  }

  public void testValues_populated() {
    for (LoadingCache<Object, Object> cache : caches()) {
      Collection<Object> values = cache.asMap().values();
      List<Entry<Object, Object>> warmed = warmUp(cache);

      Collection<Object> expected = new HashMap<>(cache.asMap()).values();
      assertThat(values).containsExactlyElementsIn(expected);
      assertThat(values.toArray()).asList().containsExactlyElementsIn(expected);
      assertThat(values.toArray(new Object[0])).asList().containsExactlyElementsIn(expected);

      assertThat(values).hasSize(WARMUP_SIZE);
      for (int i = WARMUP_MIN; i < WARMUP_MAX; i++) {
        Object value = warmed.get(i - WARMUP_MIN).getValue();
        assertThat(values.contains(value)).isTrue();
        assertThat(values.remove(value)).isTrue();
        assertThat(values.remove(value)).isFalse();
        assertThat(values.contains(value)).isFalse();
      }
      checkEmpty(values);
      checkEmpty(cache);
    }
  }


  public void testEntrySet_populated() {
    for (LoadingCache<Object, Object> cache : caches()) {
      Set<Entry<Object, Object>> entries = cache.asMap().entrySet();
      List<Entry<Object, Object>> warmed = warmUp(cache, WARMUP_MIN, WARMUP_MAX);

      Set<?> expected = new HashMap<>(cache.asMap()).entrySet();
      assertThat(entries).containsExactlyElementsIn(expected);
      assertThat(entries.toArray()).asList().containsExactlyElementsIn(expected);
      assertThat(entries.toArray(new Object[0])).asList().containsExactlyElementsIn(expected);

      new EqualsTester()
          .addEqualityGroup(cache.asMap().entrySet(), entries)
          .addEqualityGroup(ImmutableSet.of())
          .testEquals();
      assertThat(entries).hasSize(WARMUP_SIZE);
      for (int i = WARMUP_MIN; i < WARMUP_MAX; i++) {
        Entry<Object, Object> newEntry = warmed.get(i - WARMUP_MIN);
        assertThat(entries.contains(newEntry)).isTrue();
        assertThat(entries.remove(newEntry)).isTrue();
        assertThat(entries.remove(newEntry)).isFalse();
        assertThat(entries.contains(newEntry)).isFalse();
      }
      checkEmpty(entries);
      checkEmpty(cache);
    }
  }

  public void testWriteThroughEntry() {
    for (LoadingCache<Object, Object> cache : caches()) {
      cache.getUnchecked(1);
      Entry<Object, Object> entry = Iterables.getOnlyElement(cache.asMap().entrySet());

      cache.invalidate(1);
      assertThat(cache.size()).isEqualTo(0);

      entry.setValue(3);
      assertThat(cache.size()).isEqualTo(1);
      assertThat(cache.getIfPresent(1)).isEqualTo(3);
      checkValidState(cache);

      assertThrows(NullPointerException.class, () -> entry.setValue(null));
      checkValidState(cache);
    }
  }

  /* ---------------- Local utilities -------------- */

  /** Most of the tests in this class run against every one of these caches. */
  private Iterable<LoadingCache<Object, Object>> caches() {
    // lots of different ways to configure a LoadingCache
    CacheBuilderFactory factory = cacheFactory();
    return Iterables.transform(
        factory.buildAllPermutations(),
        new Function<CacheBuilder<Object, Object>, LoadingCache<Object, Object>>() {
          @Override
          public LoadingCache<Object, Object> apply(CacheBuilder<Object, Object> builder) {
            return builder.recordStats().build(identityLoader());
          }
        });
  }

  private CacheBuilderFactory cacheFactory() {
    // This is trickier than expected. We plan to put 15 values in each of these (WARMUP_MIN to
    // WARMUP_MAX), but the tests assume no values get evicted. Even with a maximumSize of 100, one
    // of the values gets evicted. With weak keys, we use identity equality, which means using
    // System.identityHashCode, which means the assignment of keys to segments is nondeterministic,
    // so more than (maximumSize / #segments) keys could get assigned to the same segment, which
    // would cause one to be evicted.
    return new CacheBuilderFactory()
        .withKeyStrengths(ImmutableSet.of(Strength.STRONG, Strength.WEAK))
        .withValueStrengths(ImmutableSet.copyOf(Strength.values()))
        .withConcurrencyLevels(ImmutableSet.of(1, 4, 16, 64))
        .withMaximumSizes(ImmutableSet.of(400, 1000))
        .withInitialCapacities(ImmutableSet.of(0, 1, 10, 100, 1000))
        .withExpireAfterWrites(
            ImmutableSet.of(
                // DurationSpec.of(500, MILLISECONDS),
                DurationSpec.of(1, SECONDS), DurationSpec.of(1, DAYS)))
        .withExpireAfterAccesses(
            ImmutableSet.of(
                // DurationSpec.of(500, MILLISECONDS),
                DurationSpec.of(1, SECONDS), DurationSpec.of(1, DAYS)))
        .withRefreshes(
            ImmutableSet.of(
                // DurationSpec.of(500, MILLISECONDS),
                DurationSpec.of(1, SECONDS), DurationSpec.of(1, DAYS)));
  }

  private List<Entry<Object, Object>> warmUp(LoadingCache<Object, Object> cache) {
    return warmUp(cache, WARMUP_MIN, WARMUP_MAX);
  }

  /**
   * Returns the entries that were added to the map, so they won't fall out of a map with weak or
   * soft references until the caller drops the reference to the returned entries.
   */
  private List<Entry<Object, Object>> warmUp(
      LoadingCache<Object, Object> cache, int minimum, int maximum) {

    List<Entry<Object, Object>> entries = new ArrayList<>();
    for (int i = minimum; i < maximum; i++) {
      Object key = i;
      Object value = cache.getUnchecked(key);
      entries.add(entryOf(key, value));
    }
    return entries;
  }

  private Entry<Object, Object> entryOf(Object key, Object value) {
    return Maps.immutableEntry(key, value);
  }

  private void assertMapSize(Map<?, ?> map, int size) {
    assertThat(map).hasSize(size);
    if (size > 0) {
      assertThat(map.isEmpty()).isFalse();
    } else {
      assertThat(map.isEmpty()).isTrue();
    }
    assertCollectionSize(map.keySet(), size);
    assertCollectionSize(map.entrySet(), size);
    assertCollectionSize(map.values(), size);
  }

  @SuppressWarnings("IterablesSizeOfCollection") // we are testing our iterator implementation
  private void assertCollectionSize(Collection<?> collection, int size) {
    assertThat(collection.size()).isEqualTo(size);
    if (size > 0) {
      assertThat(collection.isEmpty()).isFalse();
    } else {
      assertThat(collection.isEmpty()).isTrue();
    }
    assertThat(Iterables.size(collection)).isEqualTo(size);
    assertThat(Iterators.size(collection.iterator())).isEqualTo(size);
  }
}

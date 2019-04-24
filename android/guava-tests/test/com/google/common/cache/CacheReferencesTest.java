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

import static com.google.common.cache.LocalCache.Strength.STRONG;
import static com.google.common.collect.Maps.immutableEntry;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Function;
import com.google.common.cache.LocalCache.Strength;
import com.google.common.cache.TestingRemovalListeners.CountingRemovalListener;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.lang.ref.WeakReference;
import junit.framework.TestCase;

/**
 * Tests of basic {@link LoadingCache} operations with all possible combinations of key & value
 * strengths.
 *
 * @author mike nonemacher
 */
public class CacheReferencesTest extends TestCase {

  private static final CacheLoader<Key, String> KEY_TO_STRING_LOADER =
      new CacheLoader<Key, String>() {
        @Override
        public String load(Key key) {
          return key.toString();
        }
      };

  private CacheBuilderFactory factoryWithAllKeyStrengths() {
    return new CacheBuilderFactory()
        .withKeyStrengths(ImmutableSet.of(STRONG, Strength.WEAK))
        .withValueStrengths(ImmutableSet.of(STRONG, Strength.WEAK, Strength.SOFT));
  }

  private Iterable<LoadingCache<Key, String>> caches() {
    CacheBuilderFactory factory = factoryWithAllKeyStrengths();
    return Iterables.transform(
        factory.buildAllPermutations(),
        new Function<CacheBuilder<Object, Object>, LoadingCache<Key, String>>() {
          @Override
          public LoadingCache<Key, String> apply(CacheBuilder<Object, Object> builder) {
            return builder.build(KEY_TO_STRING_LOADER);
          }
        });
  }

  public void testContainsKeyAndValue() {
    for (LoadingCache<Key, String> cache : caches()) {
      // maintain strong refs so these won't be collected, regardless of cache's key/value strength
      Key key = new Key(1);
      String value = key.toString();
      assertSame(value, cache.getUnchecked(key));
      assertTrue(cache.asMap().containsKey(key));
      assertTrue(cache.asMap().containsValue(value));
      assertEquals(1, cache.size());
    }
  }

  public void testClear() {
    for (LoadingCache<Key, String> cache : caches()) {
      Key key = new Key(1);
      String value = key.toString();
      assertSame(value, cache.getUnchecked(key));
      assertFalse(cache.asMap().isEmpty());
      cache.invalidateAll();
      assertEquals(0, cache.size());
      assertTrue(cache.asMap().isEmpty());
      assertFalse(cache.asMap().containsKey(key));
      assertFalse(cache.asMap().containsValue(value));
    }
  }

  public void testKeySetEntrySetValues() {
    for (LoadingCache<Key, String> cache : caches()) {
      Key key1 = new Key(1);
      String value1 = key1.toString();
      Key key2 = new Key(2);
      String value2 = key2.toString();
      assertSame(value1, cache.getUnchecked(key1));
      assertSame(value2, cache.getUnchecked(key2));
      assertEquals(ImmutableSet.of(key1, key2), cache.asMap().keySet());
      assertThat(cache.asMap().values()).containsExactly(value1, value2);
      assertEquals(
          ImmutableSet.of(immutableEntry(key1, value1), immutableEntry(key2, value2)),
          cache.asMap().entrySet());
    }
  }

  public void testInvalidate() {
    for (LoadingCache<Key, String> cache : caches()) {
      Key key1 = new Key(1);
      String value1 = key1.toString();
      Key key2 = new Key(2);
      String value2 = key2.toString();
      assertSame(value1, cache.getUnchecked(key1));
      assertSame(value2, cache.getUnchecked(key2));
      cache.invalidate(key1);
      assertFalse(cache.asMap().containsKey(key1));
      assertTrue(cache.asMap().containsKey(key2));
      assertEquals(1, cache.size());
      assertEquals(ImmutableSet.of(key2), cache.asMap().keySet());
      assertThat(cache.asMap().values()).contains(value2);
      assertEquals(ImmutableSet.of(immutableEntry(key2, value2)), cache.asMap().entrySet());
    }
  }

  // fails in Maven with 64-bit JDK: http://code.google.com/p/guava-libraries/issues/detail?id=1568

  private void assertCleanup(
      LoadingCache<Integer, String> cache,
      CountingRemovalListener<Integer, String> removalListener) {

    // initialSize will most likely be 2, but it's possible for the GC to have already run, so we'll
    // observe a size of 1
    long initialSize = cache.size();
    assertTrue(initialSize == 1 || initialSize == 2);

    // wait up to 5s
    byte[] filler = new byte[1024];
    for (int i = 0; i < 500; i++) {
      System.gc();

      CacheTesting.drainReferenceQueues(cache);
      if (cache.size() == 1) {
        break;
      }
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        /* ignore */
      }
      try {
        // Fill up heap so soft references get cleared.
        filler = new byte[Math.max(filler.length, filler.length * 2)];
      } catch (OutOfMemoryError e) {
      }
    }

    CacheTesting.processPendingNotifications(cache);
    assertEquals(1, cache.size());
    assertEquals(1, removalListener.getCount());
  }

  // A simple type whose .toString() will return the same value each time, but without maintaining
  // a strong reference to that value.
  static class Key {
    private final int value;
    private WeakReference<String> toString;

    Key(int value) {
      this.value = value;
    }

    @Override
    public synchronized String toString() {
      String s;
      if (toString != null) {
        s = toString.get();
        if (s != null) {
          return s;
        }
      }
      s = Integer.toString(value);
      toString = new WeakReference<>(s);
      return s;
    }
  }
}

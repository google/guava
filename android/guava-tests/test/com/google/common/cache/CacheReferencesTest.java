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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.lang.ref.WeakReference;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Tests of basic {@link LoadingCache} operations with all possible combinations of key & value
 * strengths.
 *
 * @author mike nonemacher
 */
@NullUnmarked
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
      assertThat(cache.getUnchecked(key)).isSameInstanceAs(value);
      assertThat(cache.asMap().containsKey(key)).isTrue();
      assertThat(cache.asMap().containsValue(value)).isTrue();
      assertThat(cache.size()).isEqualTo(1);
    }
  }

  public void testClear() {
    for (LoadingCache<Key, String> cache : caches()) {
      Key key = new Key(1);
      String value = key.toString();
      assertThat(cache.getUnchecked(key)).isSameInstanceAs(value);
      assertThat(cache.asMap().isEmpty()).isFalse();
      cache.invalidateAll();
      assertThat(cache.size()).isEqualTo(0);
      assertThat(cache.asMap().isEmpty()).isTrue();
      assertThat(cache.asMap().containsKey(key)).isFalse();
      assertThat(cache.asMap().containsValue(value)).isFalse();
    }
  }

  public void testKeySetEntrySetValues() {
    for (LoadingCache<Key, String> cache : caches()) {
      Key key1 = new Key(1);
      String value1 = key1.toString();
      Key key2 = new Key(2);
      String value2 = key2.toString();
      assertThat(cache.getUnchecked(key1)).isSameInstanceAs(value1);
      assertThat(cache.getUnchecked(key2)).isSameInstanceAs(value2);
      assertThat(cache.asMap().keySet()).isEqualTo(ImmutableSet.of(key1, key2));
      assertThat(cache.asMap().values()).containsExactly(value1, value2);
      assertThat(cache.asMap().entrySet())
          .containsExactly(immutableEntry(key1, value1), immutableEntry(key2, value2));
    }
  }

  public void testInvalidate() {
    for (LoadingCache<Key, String> cache : caches()) {
      Key key1 = new Key(1);
      String value1 = key1.toString();
      Key key2 = new Key(2);
      String value2 = key2.toString();
      assertThat(cache.getUnchecked(key1)).isSameInstanceAs(value1);
      assertThat(cache.getUnchecked(key2)).isSameInstanceAs(value2);
      cache.invalidate(key1);
      assertThat(cache.asMap().containsKey(key1)).isFalse();
      assertThat(cache.asMap().containsKey(key2)).isTrue();
      assertThat(cache.size()).isEqualTo(1);
      assertThat(cache.asMap().keySet()).isEqualTo(ImmutableSet.of(key2));
      assertThat(cache.asMap().values()).contains(value2);
      assertThat(cache.asMap().entrySet()).containsExactly(immutableEntry(key2, value2));
    }
  }

  // fails in Maven with 64-bit JDK: https://github.com/google/guava/issues/1568

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

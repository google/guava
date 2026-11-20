/*
 * Copyright (C) 2026 The Guava Authors
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

package com.google.common.cache;

import static com.google.common.truth.Truth.assertThat;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.testing.FakeTicker;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/** Tests that failed computations preserve cache expiration and eviction metadata. */
@GwtIncompatible
@J2ktIncompatible
@NullUnmarked
public class LocalCacheMapComputeFailureTest extends TestCase {

  public void testComputeException_preservesWriteExpiration() {
    IllegalStateException failure = new IllegalStateException();
    assertFailurePreservesWriteExpiration(
        map ->
            assertThat(
                    assertThrows(
                        IllegalStateException.class,
                        () ->
                            map.compute(
                                "key",
                                (k, v) -> {
                                  throw failure;
                                })))
                .isSameInstanceAs(failure));
  }

  public void testComputeIfPresentException_preservesWriteExpiration() {
    IllegalStateException failure = new IllegalStateException();
    assertFailurePreservesWriteExpiration(
        map ->
            assertThat(
                    assertThrows(
                        IllegalStateException.class,
                        () ->
                            map.computeIfPresent(
                                "key",
                                (k, v) -> {
                                  throw failure;
                                })))
                .isSameInstanceAs(failure));
  }

  public void testMergeException_preservesWriteExpiration() {
    IllegalStateException failure = new IllegalStateException();
    assertFailurePreservesWriteExpiration(
        map ->
            assertThat(
                    assertThrows(
                        IllegalStateException.class,
                        () ->
                            map.merge(
                                "key",
                                "replacement",
                                (oldValue, newValue) -> {
                                  throw failure;
                                })))
                .isSameInstanceAs(failure));
  }

  public void testComputeError_preservesWriteExpiration() {
    AssertionError failure = new AssertionError();
    assertFailurePreservesWriteExpiration(
        map ->
            assertThat(
                    assertThrows(
                        AssertionError.class,
                        () ->
                            map.compute(
                                "key",
                                (k, v) -> {
                                  throw failure;
                                })))
                .isSameInstanceAs(failure));
  }

  private static void assertFailurePreservesWriteExpiration(
      Consumer<ConcurrentMap<String, String>> remapping) {
    FakeTicker ticker = new FakeTicker();
    Cache<String, String> cache =
        CacheBuilder.newBuilder()
            .concurrencyLevel(1)
            .expireAfterWrite(ofSeconds(5))
            .ticker(ticker)
            .build();
    cache.put("key", "original");
    ticker.advance(4, SECONDS);

    remapping.accept(cache.asMap());

    assertThat(cache.getIfPresent("key")).isEqualTo("original");
    ticker.advance(2, SECONDS);
    cache.cleanUp();
    AtomicInteger calls = new AtomicInteger();
    assertThat(
            cache
                .asMap()
                .computeIfAbsent(
                    "key",
                    k -> {
                      calls.incrementAndGet();
                      return "fresh";
                    }))
        .isEqualTo("fresh");
    assertThat(calls.get()).isEqualTo(1);
    assertThat(cache.getIfPresent("key")).isEqualTo("fresh");
  }

  public void testComputeException_preservesAccessExpiration() {
    FakeTicker ticker = new FakeTicker();
    Cache<String, String> cache =
        CacheBuilder.newBuilder()
            .concurrencyLevel(1)
            .expireAfterAccess(ofSeconds(5))
            .ticker(ticker)
            .build();
    cache.put("key", "original");
    ticker.advance(4, SECONDS);
    failComputation(cache, "key");
    ticker.advance(2, SECONDS);
    cache.cleanUp();

    assertThat(cache.asMap().computeIfAbsent("key", k -> "fresh")).isEqualTo("fresh");
  }

  public void testComputeException_preservesEvictionOrder() {
    Cache<String, String> cache =
        CacheBuilder.newBuilder().concurrencyLevel(1).maximumSize(2).build();
    cache.put("first", "first-value");
    cache.put("second", "second-value");

    failComputation(cache, "first");
    cache.put("third", "third-value");

    assertThat(cache.asMap()).containsExactly("second", "second-value", "third", "third-value");
  }

  public void testComputeException_thenExpiredComputation_preservesMaximumSize() {
    FakeTicker ticker = new FakeTicker();
    Cache<String, String> cache =
        CacheBuilder.newBuilder()
            .concurrencyLevel(1)
            .maximumSize(1)
            .expireAfterWrite(ofSeconds(5))
            .ticker(ticker)
            .build();
    cache.put("first", "original");
    failComputation(cache, "first");
    ticker.advance(6, SECONDS);
    cache.asMap().computeIfAbsent("first", k -> "fresh");
    cache.put("second", "second-value");

    assertThat(cache.asMap()).containsExactly("second", "second-value");
    assertThat(cache.size()).isEqualTo(1);
  }

  public void testComputeException_absentKey_remainsAbsent() {
    Cache<String, String> cache = CacheBuilder.newBuilder().maximumSize(1).build();

    failComputation(cache, "key");

    CacheTesting.checkEmpty(cache);
    assertThat(cache.asMap().computeIfAbsent("key", k -> "fresh")).isEqualTo("fresh");
    assertThat(cache.asMap()).containsExactly("key", "fresh");
  }

  public void testCompute_successfulReplacement_updatesWriteExpiration() {
    FakeTicker ticker = new FakeTicker();
    Cache<String, String> cache =
        CacheBuilder.newBuilder().expireAfterWrite(ofSeconds(5)).ticker(ticker).build();
    cache.put("key", "original");
    ticker.advance(4, SECONDS);

    assertThat(cache.asMap().compute("key", (k, v) -> "replacement")).isEqualTo("replacement");

    ticker.advance(2, SECONDS);
    assertThat(cache.getIfPresent("key")).isEqualTo("replacement");
    ticker.advance(4, SECONDS);
    assertThat(cache.getIfPresent("key")).isNull();
  }

  public void testCompute_successfulRemoval_leavesNoEntry() {
    Cache<String, String> cache = CacheBuilder.newBuilder().maximumSize(1).build();
    cache.put("key", "original");

    assertThat(cache.asMap().compute("key", (k, v) -> null)).isNull();

    CacheTesting.checkEmpty(cache);
  }

  public void testCompute_callbackReadExpiresEntry_leavesValidState() {
    FakeTicker ticker = new FakeTicker();
    Cache<String, String> cache =
        CacheBuilder.newBuilder()
            .concurrencyLevel(1)
            .expireAfterWrite(ofSeconds(5))
            .ticker(ticker)
            .build();
    cache.put("key", "original");

    assertThat(
            cache
                .asMap()
                .compute(
                    "key",
                    (k, v) -> {
                      ticker.advance(6, SECONDS);
                      assertThat(cache.getIfPresent(k)).isNull();
                      return v;
                    }))
        .isEqualTo("original");

    cache.put("other", "other-value");
    assertThat(cache.getIfPresent("other")).isEqualTo("other-value");
    CacheTesting.checkValidState(cache);
  }

  private static void failComputation(Cache<String, String> cache, String key) {
    IllegalStateException failure = new IllegalStateException();
    assertThat(
            assertThrows(
                IllegalStateException.class,
                () ->
                    cache
                        .asMap()
                        .compute(
                            key,
                            (k, v) -> {
                              throw failure;
                            })))
        .isSameInstanceAs(failure);
  }
}

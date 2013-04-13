/*
 * Copyright (C) 2009 The Guava Authors
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

import static com.google.common.cache.TestingCacheLoaders.constantLoader;
import static com.google.common.cache.TestingCacheLoaders.identityLoader;
import static com.google.common.cache.TestingRemovalListeners.countingRemovalListener;
import static com.google.common.cache.TestingRemovalListeners.nullRemovalListener;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Ticker;

import junit.framework.TestCase;

/**
 * Unit tests for CacheBuilder.
 */
@GwtCompatible(emulated = true)
public class CacheBuilderTest extends TestCase {

  public void testNewBuilder() {
    CacheLoader<Object, Integer> loader = constantLoader(1);

    LoadingCache<String, Integer> cache = CacheBuilder.newBuilder()
        .removalListener(countingRemovalListener())
        .build(loader);

    assertEquals(Integer.valueOf(1), cache.getUnchecked("one"));
    assertEquals(1, cache.size());
  }

  public void testInitialCapacity_negative() {
    CacheBuilder<Object, Object> builder = new CacheBuilder<Object, Object>();
    try {
      builder.initialCapacity(-1);
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testInitialCapacity_setTwice() {
    CacheBuilder<Object, Object> builder = new CacheBuilder<Object, Object>().initialCapacity(16);
    try {
      // even to the same value is not allowed
      builder.initialCapacity(16);
      fail();
    } catch (IllegalStateException expected) {}
  }

  public void testInitialCapacity_large() {
    CacheBuilder.newBuilder().initialCapacity(Integer.MAX_VALUE);
    // that the builder didn't blow up is enough;
    // don't actually create this monster!
  }

  public void testConcurrencyLevel_zero() {
    CacheBuilder<Object, Object> builder = new CacheBuilder<Object, Object>();
    try {
      builder.concurrencyLevel(0);
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testConcurrencyLevel_setTwice() {
    CacheBuilder<Object, Object> builder = new CacheBuilder<Object, Object>().concurrencyLevel(16);
    try {
      // even to the same value is not allowed
      builder.concurrencyLevel(16);
      fail();
    } catch (IllegalStateException expected) {}
  }

  public void testConcurrencyLevel_large() {
    CacheBuilder.newBuilder().concurrencyLevel(Integer.MAX_VALUE);
    // don't actually build this beast
  }

  public void testMaximumSize_negative() {
    CacheBuilder<Object, Object> builder = new CacheBuilder<Object, Object>();
    try {
      builder.maximumSize(-1);
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testMaximumSize_setTwice() {
    CacheBuilder<Object, Object> builder = new CacheBuilder<Object, Object>().maximumSize(16);
    try {
      // even to the same value is not allowed
      builder.maximumSize(16);
      fail();
    } catch (IllegalStateException expected) {}
  }

  public void testTimeToLive_negative() {
    CacheBuilder<Object, Object> builder = new CacheBuilder<Object, Object>();
    try {
      builder.expireAfterWrite(-1, SECONDS);
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testTimeToLive_small() {
    CacheBuilder.newBuilder()
        .expireAfterWrite(1, NANOSECONDS)
        .build(identityLoader());
    // well, it didn't blow up.
  }

  public void testTimeToLive_setTwice() {
    CacheBuilder<Object, Object> builder =
        new CacheBuilder<Object, Object>().expireAfterWrite(3600, SECONDS);
    try {
      // even to the same value is not allowed
      builder.expireAfterWrite(3600, SECONDS);
      fail();
    } catch (IllegalStateException expected) {}
  }

  public void testTimeToIdle_negative() {
    CacheBuilder<Object, Object> builder = new CacheBuilder<Object, Object>();
    try {
      builder.expireAfterAccess(-1, SECONDS);
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testTimeToIdle_small() {
    CacheBuilder.newBuilder()
        .expireAfterAccess(1, NANOSECONDS)
        .build(identityLoader());
    // well, it didn't blow up.
  }

  public void testTimeToIdle_setTwice() {
    CacheBuilder<Object, Object> builder =
        new CacheBuilder<Object, Object>().expireAfterAccess(3600, SECONDS);
    try {
      // even to the same value is not allowed
      builder.expireAfterAccess(3600, SECONDS);
      fail();
    } catch (IllegalStateException expected) {}
  }

  public void testTimeToIdleAndToLive() {
    CacheBuilder.newBuilder()
        .expireAfterWrite(1, NANOSECONDS)
        .expireAfterAccess(1, NANOSECONDS)
        .build(identityLoader());
    // well, it didn't blow up.
  }

  public void testTicker_setTwice() {
    Ticker testTicker = Ticker.systemTicker();
    CacheBuilder<Object, Object> builder =
        new CacheBuilder<Object, Object>().ticker(testTicker);
    try {
      // even to the same instance is not allowed
      builder.ticker(testTicker);
      fail();
    } catch (IllegalStateException expected) {}
  }

  public void testRemovalListener_setTwice() {
    RemovalListener<Object, Object> testListener = nullRemovalListener();
    CacheBuilder<Object, Object> builder =
        new CacheBuilder<Object, Object>().removalListener(testListener);
    try {
      // even to the same instance is not allowed
      builder = builder.removalListener(testListener);
      fail();
    } catch (IllegalStateException expected) {}
  }

  // "Basher tests", where we throw a bunch of stuff at a LoadingCache and check basic invariants.
}


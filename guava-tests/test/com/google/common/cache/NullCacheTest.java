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
import static com.google.common.cache.TestingCacheLoaders.constantLoader;
import static com.google.common.cache.TestingCacheLoaders.exceptionLoader;
import static com.google.common.cache.TestingRemovalListeners.countingRemovalListener;

import com.google.common.cache.TestingRemovalListeners.CountingRemovalListener;
import com.google.common.util.concurrent.UncheckedExecutionException;

import junit.framework.TestCase;

/**
 * {@link Cache} tests for caches with a maximum size of zero.
 *
 * @author mike nonemacher
 */
public class NullCacheTest extends TestCase {
  CountingRemovalListener<Object, Object> listener;

  @Override
  protected void setUp() {
    listener = countingRemovalListener();
  }

  public void testGet() {
    Object computed = new Object();
    Cache<Object, Object> cache = CacheBuilder.newBuilder()
        .maximumSize(0)
        .removalListener(listener)
        .build(constantLoader(computed));

    assertSame(computed, cache.getUnchecked(new Object()));
    assertEquals(1, listener.getCount());
    checkEmpty(cache);
  }

  public void testGet_computeNull() {
    Cache<Object, Object> cache = CacheBuilder.newBuilder()
        .maximumSize(0)
        .removalListener(listener)
        .build(constantLoader(null));

    try {
      cache.getUnchecked(new Object());
      fail();
    } catch (NullPointerException e) { /* expected */}

    assertEquals(0, listener.getCount());
    checkEmpty(cache);
  }

  public void testGet_runtimeException() {
    final RuntimeException e = new RuntimeException();
    Cache<Object, Object> map = CacheBuilder.newBuilder()
        .maximumSize(0)
        .removalListener(listener)
        .build(exceptionLoader(e));

    try {
      map.getUnchecked(new Object());
      fail();
    } catch (UncheckedExecutionException uee) {
      assertSame(e, uee.getCause());
    }
    checkEmpty(map);
  }
}

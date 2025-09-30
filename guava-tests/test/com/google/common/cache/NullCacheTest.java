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
import static com.google.common.cache.TestingRemovalListeners.queuingRemovalListener;
import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertThrows;

import com.google.common.cache.CacheLoader.InvalidCacheLoadException;
import com.google.common.cache.TestingRemovalListeners.QueuingRemovalListener;
import com.google.common.util.concurrent.UncheckedExecutionException;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * {@link LoadingCache} tests for caches with a maximum size of zero.
 *
 * @author mike nonemacher
 */
@NullUnmarked
public class NullCacheTest extends TestCase {
  QueuingRemovalListener<Object, Object> listener;

  @Override
  protected void setUp() {
    listener = queuingRemovalListener();
  }

  public void testGet() {
    Object computed = new Object();
    LoadingCache<Object, Object> cache =
        CacheBuilder.newBuilder()
            .maximumSize(0)
            .removalListener(listener)
            .build(constantLoader(computed));

    Object key = new Object();
    assertThat(cache.getUnchecked(key)).isSameInstanceAs(computed);
    RemovalNotification<Object, Object> notification = listener.remove();
    assertThat(notification.getKey()).isSameInstanceAs(key);
    assertThat(notification.getValue()).isSameInstanceAs(computed);
    assertThat(notification.getCause()).isSameInstanceAs(RemovalCause.SIZE);
    assertThat(listener.isEmpty()).isTrue();
    checkEmpty(cache);
  }

  public void testGet_expireAfterWrite() {
    Object computed = new Object();
    LoadingCache<Object, Object> cache =
        CacheBuilder.newBuilder()
            .expireAfterWrite(0, SECONDS)
            .removalListener(listener)
            .build(constantLoader(computed));

    Object key = new Object();
    assertThat(cache.getUnchecked(key)).isSameInstanceAs(computed);
    RemovalNotification<Object, Object> notification = listener.remove();
    assertThat(notification.getKey()).isSameInstanceAs(key);
    assertThat(notification.getValue()).isSameInstanceAs(computed);
    assertThat(notification.getCause()).isSameInstanceAs(RemovalCause.SIZE);
    assertThat(listener.isEmpty()).isTrue();
    checkEmpty(cache);
  }

  public void testGet_expireAfterAccess() {
    Object computed = new Object();
    LoadingCache<Object, Object> cache =
        CacheBuilder.newBuilder()
            .expireAfterAccess(0, SECONDS)
            .removalListener(listener)
            .build(constantLoader(computed));

    Object key = new Object();
    assertThat(cache.getUnchecked(key)).isSameInstanceAs(computed);
    RemovalNotification<Object, Object> notification = listener.remove();
    assertThat(notification.getKey()).isSameInstanceAs(key);
    assertThat(notification.getValue()).isSameInstanceAs(computed);
    assertThat(notification.getCause()).isSameInstanceAs(RemovalCause.SIZE);
    assertThat(listener.isEmpty()).isTrue();
    checkEmpty(cache);
  }

  public void testGet_computeNull() {
    LoadingCache<Object, Object> cache =
        CacheBuilder.newBuilder()
            .maximumSize(0)
            .removalListener(listener)
            .build(constantLoader(null));

    assertThrows(InvalidCacheLoadException.class, () -> cache.getUnchecked(new Object()));

    assertThat(listener.isEmpty()).isTrue();
    checkEmpty(cache);
  }

  public void testGet_runtimeException() {
    RuntimeException e = new RuntimeException();
    LoadingCache<Object, Object> map =
        CacheBuilder.newBuilder()
            .maximumSize(0)
            .removalListener(listener)
            .build(exceptionLoader(e));

    UncheckedExecutionException uee =
        assertThrows(UncheckedExecutionException.class, () -> map.getUnchecked(new Object()));
    assertThat(uee).hasCauseThat().isSameInstanceAs(e);
    assertThat(listener.isEmpty()).isTrue();
    checkEmpty(map);
  }
}
